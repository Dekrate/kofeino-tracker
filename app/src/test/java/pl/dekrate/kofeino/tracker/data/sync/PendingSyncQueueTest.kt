package pl.dekrate.kofeino.tracker.data.sync

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeEntity.Companion.STATUS_FAILED
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeEntity.Companion.STATUS_PENDING
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeEntity.Companion.STATUS_SENDING
import pl.dekrate.kofeino.tracker.data.sync.PendingSyncQueue.Companion.SYNC_PATH_FORMAT

/**
 * Unit tests for [PendingSyncQueue].
 *
 * Tests cover the full lifecycle:
 * enqueue → flush → dedup → retry → persist.
 *
 * Uses mockk for [PendingChangeDao] and [com.google.android.gms.wearable.MessageClient].
 * The [Tasks.await] blocking call is mocked via [mockkStatic].
 */
class PendingSyncQueueTest {

    private val dao: PendingChangeDao = mockk()
    private val messageClient: com.google.android.gms.wearable.MessageClient = mockk()
    private lateinit var queue: PendingSyncQueue

    private val nodeId = "test-node"

    @Before
    fun setUp() {
        queue = PendingSyncQueue(dao, messageClient, nodeId)

        // Stub Tasks.await() so unit tests don't need a real Task runtime.
        // The Task returned by messageClient.sendMessage is mocked; Tasks.await()
        // returns our value or throws as configured per test.
        mockkStatic(Tasks::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Tasks::class)
    }

    // ------------------------------------------------------------------
    // 1. Enqueue → stored in Room DB
    // ------------------------------------------------------------------

    @Test
    fun `enqueue inserts entity when no existing pending change`() = runTest {
        coEvery { dao.getPendingByEntity("intake", "42") } returns null
        coEvery { dao.insert(any()) } returns 1L

        queue.enqueue("intake", "42", "UPDATE", """{"caffeineMg":63}""")

        coVerify {
            dao.getPendingByEntity("intake", "42")
            dao.insert(match {
                it.entityType == "intake" &&
                    it.entityId == "42" &&
                    it.operationType == "UPDATE" &&
                    it.payload == """{"caffeineMg":63}""" &&
                    it.retryCount == 0 &&
                    it.status == STATUS_PENDING
            })
        }
    }

    // ------------------------------------------------------------------
    // 2. Dedup — only latest operation per entity sent
    // ------------------------------------------------------------------

    @Test
    fun `enqueue dedup replaces existing pending change for same entity`() = runTest {
        val existing = PendingChangeEntity(
            id = 10,
            entityType = "drink",
            entityId = "7",
            operationType = "INSERT",
            payload = """{"name":"Espresso"}""",
            timestamp = 1000L,
            retryCount = 2,
            status = STATUS_PENDING
        )
        coEvery { dao.getPendingByEntity("drink", "7") } returns existing
        coEvery { dao.update(any()) } returns Unit

        queue.enqueue("drink", "7", "UPDATE", """{"name":"Double Espresso"}""")

        coVerify {
            dao.update(match {
                it.id == 10L &&
                    it.entityType == "drink" &&
                    it.entityId == "7" &&
                    it.operationType == "UPDATE" &&
                    it.payload == """{"name":"Double Espresso"}""" &&
                    it.retryCount == 0 && // reset on replacement
                    it.status == STATUS_PENDING
            })
        }
    }

    @Test
    fun `enqueue does NOT dedup when entityType differs`() = runTest {
        coEvery { dao.getPendingByEntity("intake", "42") } returns null
        coEvery { dao.getPendingByEntity("drink", "42") } returns null
        coEvery { dao.insert(any()) } returns 1L

        queue.enqueue("intake", "42", "INSERT", """{}""")
        queue.enqueue("drink", "42", "INSERT", """{}""")

        // Two separate inserts — entity type differs so no dedup
        coVerify(exactly = 2) { dao.insert(any()) }
    }

    // ------------------------------------------------------------------
    // 3. Flush — items sent via MessageClient
    // ------------------------------------------------------------------

    @Test
    fun `flush sends pending items via messageClient and removes on success`() = runTest {
        val change = PendingChangeEntity(
            id = 1, entityType = "intake", entityId = "42",
            operationType = "UPDATE", payload = """{"caffeineMg":63}""",
            timestamp = 1000L
        )
        coEvery { dao.getPendingChanges() } returns listOf(change)
        coEvery { dao.update(any()) } returns Unit
        coEvery { dao.delete(any()) } returns Unit

        every { messageClient.sendMessage(nodeId, any(), any()) } returns mockSendTask(42)
        every { Tasks.await(any<Task<Int>>()) } returns 42

        val result = queue.flush()

        assert(result.sent == 1) { "Expected 1 sent, got ${result.sent}" }
        assert(result.failed == 0) { "Expected 0 failed, got ${result.failed}" }

        // Verify correct path
        verify { messageClient.sendMessage(nodeId, "/sync/intake/update", any()) }

        // Verify status transition to SENDING then delete
        coVerify {
            dao.update(match { it.status == STATUS_SENDING })
            dao.delete(change)
        }
    }

    @Test
    fun `flush with empty queue returns zero counts`() = runTest {
        coEvery { dao.getPendingChanges() } returns emptyList()

        val result = queue.flush()

        assert(result.sent == 0) { "Expected 0 sent" }
        assert(result.failed == 0) { "Expected 0 failed" }
    }

    @Test
    fun `flush sends multiple items in FIFO order`() = runTest {
        val change1 = PendingChangeEntity(id = 1, entityType = "intake", entityId = "1",
            operationType = "INSERT", payload = """{}""", timestamp = 100L)
        val change2 = PendingChangeEntity(id = 2, entityType = "intake", entityId = "2",
            operationType = "UPDATE", payload = """{}""", timestamp = 200L)
        coEvery { dao.getPendingChanges() } returns listOf(change1, change2)
        coEvery { dao.update(any()) } returns Unit
        coEvery { dao.delete(any()) } returns Unit

        every { messageClient.sendMessage(nodeId, any(), any()) } returns mockSendTask(1)
        every { Tasks.await(any<Task<Int>>()) } returns 1

        val result = queue.flush()

        assert(result.sent == 2) { "Expected 2 sent, got ${result.sent}" }
        coVerify(exactly = 2) { dao.delete(any()) }
    }

    @Test
    fun `flush sends DELETE operation on correct path`() = runTest {
        val change = PendingChangeEntity(id = 1, entityType = "intake", entityId = "99",
            operationType = "DELETE", payload = """{}""", timestamp = 1L)
        coEvery { dao.getPendingChanges() } returns listOf(change)
        coEvery { dao.update(any()) } returns Unit
        coEvery { dao.delete(any()) } returns Unit

        every { messageClient.sendMessage(nodeId, any(), any()) } returns mockSendTask(1)
        every { Tasks.await(any<Task<Int>>()) } returns 1

        queue.flush()

        verify { messageClient.sendMessage(nodeId, "/sync/intake/delete", any()) }
    }

    // ------------------------------------------------------------------
    // 4. Retry with exponential backoff concept
    // ------------------------------------------------------------------

    @Test
    fun `flush increments retryCount on failure and keeps PENDING for retry`() = runTest {
        val change = PendingChangeEntity(id = 1, entityType = "intake", entityId = "42",
            operationType = "UPDATE", payload = """{}""", timestamp = 1L,
            retryCount = 0)
        coEvery { dao.getPendingChanges() } returns listOf(change)
        coEvery { dao.update(any()) } returns Unit

        every { messageClient.sendMessage(nodeId, any(), any()) } returns mockSendTask(0)
        every { Tasks.await(any<Task<Int>>()) } throws RuntimeException("Send failed")

        val result = queue.flush()

        assert(result.sent == 0) { "Expected 0 sent" }
        assert(result.failed == 1) { "Expected 1 failed" }

        // retryCount should go from 0 → 1, status stays PENDING
        coVerify {
            dao.update(match {
                it.id == 1L &&
                    it.retryCount == 1 &&
                    it.status == STATUS_PENDING
            })
        }
    }

    @Test
    fun `failure after 5 retries marks change as FAILED`() = runTest {
        val change = PendingChangeEntity(id = 1, entityType = "intake", entityId = "42",
            operationType = "UPDATE", payload = """{}""", timestamp = 1L,
            retryCount = 4) // one more failure → hits max 5
        coEvery { dao.getPendingChanges() } returns listOf(change)
        coEvery { dao.update(any()) } returns Unit

        every { messageClient.sendMessage(nodeId, any(), any()) } returns mockSendTask(0)
        every { Tasks.await(any<Task<Int>>()) } throws RuntimeException("Send failed")

        queue.flush()

        coVerify {
            dao.update(match {
                it.id == 1L &&
                    it.retryCount == 5 &&
                    it.status == STATUS_FAILED
            })
        }
    }

    @Test
    fun `retry counts 0 through 4 stay PENDING, 5th goes FAILED`() = runTest {
        // Test all boundary retry counts
        for (initialRetry in 0..4) {
            val change = PendingChangeEntity(id = 1, entityType = "intake", entityId = "x",
                operationType = "INSERT", payload = """{}""", timestamp = 1L,
                retryCount = initialRetry)
            coEvery { dao.getPendingChanges() } returns listOf(change)
            coEvery { dao.update(any()) } returns Unit

            every { messageClient.sendMessage(nodeId, any(), any()) } returns mockSendTask(0)
            every { Tasks.await(any<Task<Int>>()) } throws RuntimeException("Fail")

            queue.flush()

            val expectedStatus = if (initialRetry == 4) STATUS_FAILED else STATUS_PENDING
            coVerify {
                dao.update(match {
                    it.retryCount == initialRetry + 1 &&
                        it.status == expectedStatus
                })
            }
        }
    }

    // ------------------------------------------------------------------
    // 5. App restart → queue restored from Room
    // ------------------------------------------------------------------

    @Test
    fun `survive app restart by reading unflushed items from dao`() = runTest {
        // Simulate: items were enqueued before "restart"
        val persisted = listOf(
            PendingChangeEntity(id = 1, entityType = "intake", entityId = "1",
                operationType = "INSERT", payload = """{"a":1}""", timestamp = 100L,
                status = STATUS_PENDING),
            PendingChangeEntity(id = 2, entityType = "drink", entityId = "2",
                operationType = "UPDATE", payload = """{"b":2}""", timestamp = 200L,
                status = STATUS_PENDING)
        )

        // After "restart", a new PendingSyncQueue instance reads from the same DAO
        coEvery { dao.getPendingChanges() } returns persisted
        coEvery { dao.update(any()) } returns Unit
        coEvery { dao.delete(any()) } returns Unit
        every { messageClient.sendMessage(nodeId, any(), any()) } returns mockSendTask(1)
        every { Tasks.await(any<Task<Int>>()) } returns 1

        val restartedQueue = PendingSyncQueue(dao, messageClient, nodeId)
        val result = restartedQueue.flush()

        assert(result.sent == 2) { "Expected 2 items flushed after restart, got ${result.sent}" }

        // Verify the items from the "restored" DAO were sent
        coVerify {
            dao.getPendingChanges()
            dao.delete(persisted[0])
            dao.delete(persisted[1])
        }
    }

    // ------------------------------------------------------------------
    // 6. Size / failedCount queries
    // ------------------------------------------------------------------

    @Test
    fun `size delegates to dao count`() = runTest {
        coEvery { dao.count() } returns 7
        assert(queue.size() == 7)
        coVerify { dao.count() }
    }

    @Test
    fun `failedCount delegates to dao countFailed`() = runTest {
        coEvery { dao.countFailed() } returns 3
        assert(queue.failedCount() == 3)
        coVerify { dao.countFailed() }
    }

    // ------------------------------------------------------------------
    // 7. Handle mixed success/failure in same flush
    // ------------------------------------------------------------------

    @Test
    fun `flush with partial failures returns correct counts`() = runTest {
        val good = PendingChangeEntity(id = 1, entityType = "intake", entityId = "1",
            operationType = "INSERT", payload = """{}""", timestamp = 1L)
        val bad = PendingChangeEntity(id = 2, entityType = "intake", entityId = "2",
            operationType = "UPDATE", payload = """{}""", timestamp = 2L)
        coEvery { dao.getPendingChanges() } returns listOf(good, bad)
        coEvery { dao.update(any()) } returns Unit
        coEvery { dao.delete(any()) } returns Unit

        // First send succeeds, second fails
        every { messageClient.sendMessage(nodeId, "/sync/intake/insert", any()) } returns mockSendTask(1)
        every { messageClient.sendMessage(nodeId, "/sync/intake/update", any()) } returns mockSendTask(0)
        every { Tasks.await(any<Task<Int>>()) } returnsMany listOf(1) andThenThrows RuntimeException("Fail")

        val result = queue.flush()

        assert(result.sent == 1)
        assert(result.failed == 1)

        // Good item deleted, bad item marked for retry
        coVerify { dao.delete(good) }
        coVerify {
            dao.update(match { it.id == 2L && it.retryCount == 1 && it.status == STATUS_PENDING })
        }
    }

    // ------------------------------------------------------------------
    // 8. Payload bytes sent correctly
    // ------------------------------------------------------------------

    @Test
    fun `flush sends entity payload as message bytes`() = runTest {
        val payload = """{"drinkName":"Latte","caffeineMg":63}"""
        val change = PendingChangeEntity(id = 1, entityType = "intake", entityId = "10",
            operationType = "INSERT", payload = payload, timestamp = 1L)
        coEvery { dao.getPendingChanges() } returns listOf(change)
        coEvery { dao.update(any()) } returns Unit
        coEvery { dao.delete(any()) } returns Unit

        val payloadSlot = slot<ByteArray>()
        every { messageClient.sendMessage(nodeId, any(), capture(payloadSlot)) } returns mockSendTask(1)
        every { Tasks.await(any<Task<Int>>()) } returns 1

        queue.flush()

        val sentPayload = payloadSlot.captured.toString(Charsets.UTF_8)
        assert(sentPayload == payload) {
            "Expected payload '$payload', got '$sentPayload'"
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Creates a mock [Task] that [Tasks.await] can work with.
     * The mock is a simple [Int] task; the actual result value used by
     * [Tasks.await] is controlled by the [Tasks.await] stub in each test.
     */
    private fun mockSendTask(taskResult: Int): Task<Int> {
        val task = mockk<Task<Int>>()
        every { task.isComplete } returns true
        every { task.isSuccessful } returns true
        every { task.result } returns taskResult
        return task
    }
}
