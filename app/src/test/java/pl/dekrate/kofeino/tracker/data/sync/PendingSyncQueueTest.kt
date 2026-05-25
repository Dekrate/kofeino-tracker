package pl.dekrate.kofeino.tracker.data.sync

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeEntity.Companion.STATUS_FAILED
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeEntity.Companion.STATUS_PENDING
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeEntity.Companion.STATUS_SENDING
import pl.dekrate.kofeino.tracker.data.sync.PendingSyncQueue.Companion.SYNC_PATH_FORMAT
import java.util.concurrent.ExecutionException

/**
 * Unit tests for [PendingSyncQueue].
 *
 * Tests cover the full lifecycle:
 * enqueue → flush → dedup → retry → persist.
 *
 * Uses mockk for [PendingChangeDao] and [MessageClient].
 * [Tasks.await] is mocked via [mockkStatic] because the real implementation
 * requires an Android Looper (not available in JVM unit tests).
 *
 * The custom [Task.await] extension (defined in [SyncTaskExtensionsKt]) calls
 * [Tasks.await] inside [kotlinx.coroutines.Dispatchers.IO], so we mock
 * [Tasks.await] statically to avoid Looper crashes and return synthetic results.
 */
class PendingSyncQueueTest {

    private val dao: PendingChangeDao = mockk()
    private val messageClient: MessageClient = mockk()
    private val capabilityClient: CapabilityClient = mockk()
    private lateinit var queue: PendingSyncQueue

    private val nodeId = "test-node"

    @Before
    fun setUp() {
        queue = PendingSyncQueue(dao, messageClient, capabilityClient)
        mockkStatic(Tasks::class)
        // Stub Tasks.await for ALL Task types in one generic handler.
        // The real implementation requires a Looper, which is unavailable in JVM tests.
        // This stub reads isSuccessful / result / exception directly from the mock Task,
        // matching the semantics Tasks.await would provide.
        @Suppress("UNCHECKED_CAST")
        every { Tasks.await(any<Task<*>>()) } answers {
            val task = firstArg<Task<*>>()
            if (task.isSuccessful) {
                task.result
            } else {
                // Tasks.await wraps the task's exception in an ExecutionException.
                throw ExecutionException(
                    "Task failed",
                    task.exception ?: RuntimeException("Unknown error")
                )
            }
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(Tasks::class)
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Simulate a reachable paired device with the given [nodeId]. */
    private fun setupReachableNode(nodeId: String) {
        val node = mockk<Node> {
            every { id } returns nodeId
            every { isNearby } returns true
        }
        val capabilityInfo = mockk<CapabilityInfo> {
            every { nodes } returns setOf(node)
        }
        val task = mockk<Task<CapabilityInfo>>()
        every { task.isComplete } returns true
        every { task.isSuccessful } returns true
        every { task.result } returns capabilityInfo
        every { capabilityClient.getCapability(any(), any()) } returns task
    }

    /** Simulate failure during node resolution (no reachable device). */
    private fun setupUnreachableNode() {
        val task = mockk<Task<CapabilityInfo>>()
        every { task.isComplete } returns true
        every { task.isSuccessful } returns false
        every { task.exception } returns RuntimeException("Network error")
        every { capabilityClient.getCapability(any(), any()) } returns task
    }

    /** Make [MessageClient.sendMessage] succeed (return 0). */
    private fun setupSuccessfulMessage() {
        val task = mockk<Task<Int>>()
        every { task.isComplete } returns true
        every { task.isSuccessful } returns true
        every { task.result } returns 0
        every { messageClient.sendMessage(any(), any(), any()) } returns task
    }

    /** Make [MessageClient.sendMessage] throw on every call. */
    private fun setupFailingMessage() {
        val task = mockk<Task<Int>>()
        every { task.isComplete } returns true
        every { task.isSuccessful } returns false
        every { task.exception } returns RuntimeException("Send failed")
        every { messageClient.sendMessage(any(), any(), any()) } returns task
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
        coEvery { dao.getPendingChanges(any()) } returns listOf(change)
        coEvery { dao.update(any()) } returns Unit
        coEvery { dao.delete(any()) } returns Unit
        setupReachableNode(nodeId)
        setupSuccessfulMessage()

        val result = queue.flush()

        assert(result.sent == 1) { "Expected 1 sent, got ${result.sent}" }
        assert(result.failed == 0) { "Expected 0 failed, got ${result.failed}" }

        // Verify correct path
        coVerify { messageClient.sendMessage(nodeId, "/sync/intake/update", any()) }

        // Verify status transition to SENDING then delete
        coVerify {
            dao.update(match { it.status == STATUS_SENDING })
            dao.delete(change)
        }
    }

    @Test
    fun `flush with empty queue returns zero counts`() = runTest {
        coEvery { dao.getPendingChanges(any()) } returns emptyList()
        setupReachableNode(nodeId)

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
        coEvery { dao.getPendingChanges(any()) } returns listOf(change1, change2)
        coEvery { dao.update(any()) } returns Unit
        coEvery { dao.delete(any()) } returns Unit
        setupReachableNode(nodeId)
        setupSuccessfulMessage()

        val result = queue.flush()

        assert(result.sent == 2) { "Expected 2 sent, got ${result.sent}" }
        coVerify(exactly = 2) { dao.delete(any()) }
    }

    @Test
    fun `flush sends DELETE operation on correct path`() = runTest {
        val change = PendingChangeEntity(id = 1, entityType = "intake", entityId = "99",
            operationType = "DELETE", payload = """{}""", timestamp = 1L)
        coEvery { dao.getPendingChanges(any()) } returns listOf(change)
        coEvery { dao.update(any()) } returns Unit
        coEvery { dao.delete(any()) } returns Unit
        setupReachableNode(nodeId)
        setupSuccessfulMessage()

        queue.flush()

        coVerify { messageClient.sendMessage(nodeId, "/sync/intake/delete", any()) }
    }

    // ------------------------------------------------------------------
    // 4. Retry with exponential backoff concept
    // ------------------------------------------------------------------

    @Test
    fun `flush increments retryCount on failure and keeps PENDING for retry`() = runTest {
        val change = PendingChangeEntity(id = 1, entityType = "intake", entityId = "42",
            operationType = "UPDATE", payload = """{}""", timestamp = 1L,
            retryCount = 0)
        coEvery { dao.getPendingChanges(any()) } returns listOf(change)
        coEvery { dao.update(any()) } returns Unit
        setupReachableNode(nodeId)
        setupFailingMessage()

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
        coEvery { dao.getPendingChanges(any()) } returns listOf(change)
        coEvery { dao.update(any()) } returns Unit
        setupReachableNode(nodeId)
        setupFailingMessage()

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
        setupReachableNode(nodeId)
        setupFailingMessage()

        // Test all boundary retry counts
        for (initialRetry in 0..4) {
            val change = PendingChangeEntity(id = 1, entityType = "intake", entityId = "x",
                operationType = "INSERT", payload = """{}""", timestamp = 1L,
                retryCount = initialRetry)
            coEvery { dao.getPendingChanges(any()) } returns listOf(change)
            coEvery { dao.update(any()) } returns Unit

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
        coEvery { dao.getPendingChanges(any()) } returns persisted
        coEvery { dao.update(any()) } returns Unit
        coEvery { dao.delete(any()) } returns Unit
        setupReachableNode(nodeId)
        setupSuccessfulMessage()

        val restartedQueue = PendingSyncQueue(dao, messageClient, capabilityClient)
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
        coEvery { dao.getPendingChanges(any()) } returns listOf(good, bad)
        coEvery { dao.update(any()) } returns Unit
        coEvery { dao.delete(any()) } returns Unit
        setupReachableNode(nodeId)

        // First send succeeds, second fails
        val goodTask = mockk<Task<Int>>()
        every { goodTask.isComplete } returns true
        every { goodTask.isSuccessful } returns true
        every { goodTask.result } returns 1
        every { messageClient.sendMessage(nodeId, "/sync/intake/insert", any()) } returns goodTask

        val badTask = mockk<Task<Int>>()
        every { badTask.isComplete } returns true
        every { badTask.isSuccessful } returns false
        every { badTask.exception } returns RuntimeException("Fail")
        every { messageClient.sendMessage(nodeId, "/sync/intake/update", any()) } returns badTask

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
        coEvery { dao.getPendingChanges(any()) } returns listOf(change)
        coEvery { dao.update(any()) } returns Unit
        coEvery { dao.delete(any()) } returns Unit
        setupReachableNode(nodeId)
        setupSuccessfulMessage()

        val payloadSlot = slot<ByteArray>()
        every { messageClient.sendMessage(nodeId, any(), capture(payloadSlot)) } returns mockk()

        queue.flush()

        val sentPayload = payloadSlot.captured.toString(Charsets.UTF_8)
        assert(sentPayload == payload) {
            "Expected payload '$payload', got '$sentPayload'"
        }
    }

    // ------------------------------------------------------------------
    // 9. Exponential backoff
    // ------------------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `flush applies exponential backoff before retry attempt`() = runTest {
        val change = PendingChangeEntity(id = 1, entityType = "intake", entityId = "42",
            operationType = "UPDATE", payload = """{}""", timestamp = 1L,
            retryCount = 1) // already failed once → backoff = 2s
        coEvery { dao.getPendingChanges(any()) } returns listOf(change)
        coEvery { dao.update(any()) } returns Unit
        coEvery { dao.delete(any()) } returns Unit
        setupReachableNode(nodeId)
        setupSuccessfulMessage()

        val start = currentTime
        val result = queue.flush()
        val elapsed = currentTime - start

        // retryCount=1 → backoff = 1s × 2^1 = 2000ms
        assert(elapsed >= 2000) { "Expected ≥2000ms backoff, got ${elapsed}ms" }
        assert(result.sent == 1)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `flush does NOT backoff on first attempt`() = runTest {
        val change = PendingChangeEntity(id = 1, entityType = "intake", entityId = "42",
            operationType = "INSERT", payload = """{}""", timestamp = 1L,
            retryCount = 0)
        coEvery { dao.getPendingChanges(any()) } returns listOf(change)
        coEvery { dao.update(any()) } returns Unit
        coEvery { dao.delete(any()) } returns Unit
        setupReachableNode(nodeId)
        setupSuccessfulMessage()

        val start = currentTime
        queue.flush()
        val elapsed = currentTime - start

        assert(elapsed < 100) { "Expected no significant backoff, elapsed=${elapsed}ms" }
    }

    // ------------------------------------------------------------------
    // 10. flushAllPending — retry FAILED items
    // ------------------------------------------------------------------

    @Test
    fun `flushAllPending calls flush then retries failed items with budget`() = runTest {
        val pending = PendingChangeEntity(id = 1, entityType = "intake", entityId = "1",
            operationType = "INSERT", payload = """{}""", timestamp = 1L)
        val retryable = listOf(
            PendingChangeEntity(id = 2, entityType = "drink", entityId = "2",
                operationType = "UPDATE", payload = """{}""", timestamp = 2L,
                retryCount = 3, status = STATUS_FAILED)
        )
        coEvery { dao.getPendingChanges(any()) } returns listOf(pending)
        coEvery { dao.getRetryableFailed() } returns retryable
        coEvery { dao.update(any()) } returns Unit
        coEvery { dao.delete(any()) } returns Unit
        setupReachableNode(nodeId)
        setupSuccessfulMessage()

        val result = queue.flushAllPending()

        assert(result.sent == 2) { "Expected 2 sent (1 pending + 1 recovered), got ${result.sent}" }
        coVerify { dao.getRetryableFailed() }
    }

    @Test
    fun `flushAllPending does not fail when no retryable items exist`() = runTest {
        coEvery { dao.getPendingChanges(any()) } returns emptyList()
        coEvery { dao.getRetryableFailed() } returns emptyList()
        setupReachableNode(nodeId)

        val result = queue.flushAllPending()

        assert(result.sent == 0)
        assert(result.failed == 0)
    }

    @Test
    fun `flushAllPending maintains correct counts when failed item persists`() = runTest {
        val pending = PendingChangeEntity(id = 1, entityType = "intake", entityId = "1",
            operationType = "INSERT", payload = """{}""", timestamp = 1L)
        val retryable = listOf(
            PendingChangeEntity(id = 2, entityType = "intake", entityId = "2",
                operationType = "UPDATE", payload = """{}""", timestamp = 2L,
                retryCount = 2, status = STATUS_FAILED)
        )
        coEvery { dao.getPendingChanges(any()) } returns listOf(pending)
        coEvery { dao.getRetryableFailed() } returns retryable
        coEvery { dao.update(any()) } returns Unit
        coEvery { dao.delete(any()) } returns Unit
        setupReachableNode(nodeId)

        // Pending succeeds, retryable fails again
        val goodTask = mockk<Task<Int>>()
        every { goodTask.isComplete } returns true
        every { goodTask.isSuccessful } returns true
        every { goodTask.result } returns 1
        every { messageClient.sendMessage(nodeId, "/sync/intake/insert", any()) } returns goodTask

        val badTask = mockk<Task<Int>>()
        every { badTask.isComplete } returns true
        every { badTask.isSuccessful } returns false
        every { badTask.exception } returns RuntimeException("Fail again")
        every { messageClient.sendMessage(nodeId, "/sync/intake/update", any()) } returns badTask

        val result = queue.flushAllPending()

        assert(result.sent == 1)
        assert(result.failed == 1)
    }

    // ------------------------------------------------------------------
    // 11. No reachable node edge cases
    // ------------------------------------------------------------------

    @Test
    fun `flush returns zero when no reachable node`() = runTest {
        coEvery { dao.getPendingChanges(any()) } returns listOf(
            PendingChangeEntity(id = 1, entityType = "intake", entityId = "1",
                operationType = "INSERT", payload = """{}""", timestamp = 1L)
        )
        setupUnreachableNode()

        val result = queue.flush()

        assert(result.sent == 0) { "Expected 0 sent" }
        assert(result.failed == 0) { "Expected 0 failed" }
        coVerify(exactly = 0) { messageClient.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `flushAllPending returns correct counts when no reachable node`() = runTest {
        coEvery { dao.getPendingChanges(any()) } returns listOf(
            PendingChangeEntity(id = 1, entityType = "intake", entityId = "1",
                operationType = "INSERT", payload = """{}""", timestamp = 1L)
        )
        coEvery { dao.getRetryableFailed() } returns listOf(
            PendingChangeEntity(id = 2, entityType = "intake", entityId = "2",
                operationType = "UPDATE", payload = """{}""", timestamp = 2L,
                retryCount = 3, status = STATUS_FAILED)
        )
        setupUnreachableNode()

        val result = queue.flushAllPending()

        assert(result.sent == 0)
        assert(result.failed == 1) // retryable items count as failed when no node
    }

    // ------------------------------------------------------------------
    // 12. Dedup also matches SENDING status
    // ------------------------------------------------------------------

    @Test
    fun `enqueue dedup replaces existing SENDING change for same entity`() = runTest {
        val existing = PendingChangeEntity(id = 5, entityType = "intake", entityId = "77",
            operationType = "INSERT", payload = """{"old":1}""", timestamp = 100L,
            retryCount = 1, status = STATUS_SENDING) // in-flight
        coEvery { dao.getPendingByEntity("intake", "77") } returns existing
        coEvery { dao.update(any()) } returns Unit

        queue.enqueue("intake", "77", "UPDATE", """{"new":2}""")

        coVerify {
            dao.update(match {
                it.id == 5L &&
                    it.operationType == "UPDATE" &&
                    it.status == STATUS_PENDING
            })
        }
    }
}
