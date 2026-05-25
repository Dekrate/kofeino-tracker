package pl.dekrate.kofeino.data.sync

import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [PendingSyncQueue].
 *
 * Verifies:
 * - Enqueue with dedup logic (insert vs update)
 * - Flush sends pending changes and returns correct counts
 * - Empty queue / no-reachable-node edge cases
 * - Exponential back-off and max-retries → FAILED status
 * - flushAllPending retries recoverable failures
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
@OptIn(ExperimentalCoroutinesApi::class)
class PendingSyncQueueTest {

    @MockK
    private lateinit var dao: PendingChangeDao

    @MockK
    private lateinit var messageClient: MessageClient

    @MockK
    private lateinit var capabilityClient: CapabilityClient

    private lateinit var queue: PendingSyncQueue

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)
        queue = PendingSyncQueue(dao, messageClient, capabilityClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Simulate a reachable paired device with the given [nodeId].
     *
     * Uses [Tasks.forResult] to create a real, already-completed Task so that
     * the `.await()` extension function works without needing to mock it.
     */
    private fun setupReachableNode(nodeId: String) {
        val node = mockk<Node> {
            every { id } returns nodeId
            every { isNearby } returns true
        }
        val capabilityInfo = mockk<CapabilityInfo> {
            every { nodes } returns setOf(node)
        }
        every { capabilityClient.getCapability(any(), any()) } returns Tasks.forResult(capabilityInfo)
    }

    /**
     * Simulate failure during node resolution (no reachable device).
     *
     * Uses [Tasks.forException] so that `.await()` throws [java.util.concurrent.ExecutionException],
     * which is what the production code catches in [PendingSyncQueue.resolveNodeId].
     */
    private fun setupUnreachableNode() {
        every {
            capabilityClient.getCapability(any(), any())
        } returns Tasks.forException<CapabilityInfo>(RuntimeException("Network error"))
    }

    /** Make [MessageClient.sendMessage] succeed (return 0). */
    private fun setupSuccessfulMessage() {
        every { messageClient.sendMessage(any(), any(), any()) } returns Tasks.forResult(0)
    }

    /** Make [MessageClient.sendMessage] throw on every call. */
    private fun setupFailingMessage() {
        every {
            messageClient.sendMessage(any(), any(), any())
        } returns Tasks.forException<Int>(RuntimeException("Send failed"))
    }

    /** Factory helper reducing boilerplate when building [PendingChangeEntity] instances. */
    private fun entity(
        id: Long = 0L,
        entityType: String = "intake",
        entityId: String = "1",
        operationType: String = "INSERT",
        payload: String = "{}",
        timestamp: Long = 1000L,
        retryCount: Int = 0,
        status: String = PendingChangeEntity.STATUS_PENDING
    ): PendingChangeEntity = PendingChangeEntity(
        id = id,
        entityType = entityType,
        entityId = entityId,
        operationType = operationType,
        payload = payload,
        timestamp = timestamp,
        retryCount = retryCount,
        status = status
    )

    // ------------------------------------------------------------------
    // enqueue()
    // ------------------------------------------------------------------

    @Test
    fun `enqueue_insertsNewChange`() = runTest(testDispatcher) {
        // Explicit mock to guarantee null is returned (works around Java 25 ByteBuddy limitations)
        coEvery { dao.getPendingByEntity(any(), any()) } returns null

        queue.enqueue("intake", "1", "INSERT", """{"caffeineMg":50}""")

        coVerify(exactly = 1) {
            dao.insert(match {
                it.entityType == "intake" &&
                    it.entityId == "1" &&
                    it.operationType == "INSERT" &&
                    it.payload == """{"caffeineMg":50}""" &&
                    it.status == PendingChangeEntity.STATUS_PENDING &&
                    it.retryCount == 0
            })
        }
        coVerify(exactly = 0) { dao.update(any()) }
    }

    @Test
    fun `enqueue_dedupReplacesExistingChange`() = runTest(testDispatcher) {
        val existing = entity(
            id = 42L,
            entityType = "intake",
            entityId = "1",
            operationType = "INSERT",
            payload = """{"caffeineMg":50}""",
            timestamp = 1000L,
            retryCount = 2,
            status = PendingChangeEntity.STATUS_PENDING
        )
        coEvery { dao.getPendingByEntity("intake", "1") } returns existing

        queue.enqueue("intake", "1", "UPDATE", """{"caffeineMg":100}""")

        coVerify(exactly = 1) {
            dao.update(match {
                it.id == 42L &&
                    it.entityType == "intake" &&
                    it.entityId == "1" &&
                    it.operationType == "UPDATE" &&
                    it.payload == """{"caffeineMg":100}""" &&
                    it.retryCount == 0 &&
                    it.status == PendingChangeEntity.STATUS_PENDING
            })
        }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    // ------------------------------------------------------------------
    // flush()
    // ------------------------------------------------------------------

    @Test
    fun `flush_sendsPendingChanges`() = runTest(testDispatcher) {
        val items = listOf(
            entity(id = 1L, entityId = "1", operationType = "INSERT", timestamp = 1000L),
            entity(id = 2L, entityId = "2", operationType = "UPDATE", timestamp = 2000L)
        )
        coEvery { dao.getPendingChanges(any()) } returns items
        setupReachableNode("node-1")
        setupSuccessfulMessage()

        val result = queue.flush()

        assertEquals(PendingSyncQueue.FlushResult(sent = 2, failed = 0), result)
        coVerify(exactly = 2) { messageClient.sendMessage(any(), any(), any()) }
        coVerify(exactly = 2) { dao.delete(any()) }
    }

    @Test
    fun `flush_emptyQueueReturnsZero`() = runTest(testDispatcher) {
        coEvery { dao.getPendingChanges(any()) } returns emptyList()
        setupReachableNode("node-1")

        val result = queue.flush()

        assertEquals(PendingSyncQueue.FlushResult(sent = 0, failed = 0), result)
        coVerify(exactly = 0) { messageClient.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `flush_noReachableNode_returnsZero`() = runTest(testDispatcher) {
        setupUnreachableNode()

        val result = queue.flush()

        assertEquals(PendingSyncQueue.FlushResult(sent = 0, failed = 0), result)
        coVerify(exactly = 0) { messageClient.sendMessage(any(), any(), any()) }
        coVerify(exactly = 0) { dao.getPendingChanges(any()) }
    }

    @Test
    fun `flush_marksFailedAfterMaxRetries`() = runTest(testDispatcher) {
        // One more failure will push retryCount to MAX_RETRIES (5)
        val item = entity(
            id = 1L,
            entityType = "intake",
            entityId = "1",
            retryCount = 4,
            status = PendingChangeEntity.STATUS_PENDING
        )
        coEvery { dao.getPendingChanges(any()) } returns listOf(item)
        setupReachableNode("node-1")
        setupFailingMessage()

        val result = queue.flush()

        assertEquals(PendingSyncQueue.FlushResult(sent = 0, failed = 1), result)

        // 1st update: marks as SENDING
        coVerify(exactly = 1) {
            dao.update(match {
                it.id == 1L && it.status == PendingChangeEntity.STATUS_SENDING
            })
        }
        // 2nd update (handleFailure): bumps to retryCount=5, marks FAILED
        coVerify(exactly = 1) {
            dao.update(match {
                it.id == 1L &&
                    it.retryCount == 5 &&
                    it.status == PendingChangeEntity.STATUS_FAILED
            })
        }
        // Exactly two updates total (both tracked above)
        coVerify(exactly = 2) { dao.update(any()) }
    }

    // ------------------------------------------------------------------
    // flushAllPending()
    // ------------------------------------------------------------------

    @Test
    fun `flushAllPending_retriesFailedItems`() = runTest(testDispatcher) {
        // First flush returns empty pending
        coEvery { dao.getPendingChanges(any()) } returns emptyList()
        // There are retryable failures
        val retryItem = entity(
            id = 10L,
            entityType = "intake",
            entityId = "1",
            retryCount = 3,
            status = PendingChangeEntity.STATUS_FAILED
        )
        coEvery { dao.getRetryableFailed() } returns listOf(retryItem)
        setupReachableNode("node-1")
        setupSuccessfulMessage()

        val result = queue.flushAllPending()

        assertEquals(PendingSyncQueue.FlushResult(sent = 1, failed = 0), result)
        // 1 send for the retried item
        coVerify(exactly = 1) { messageClient.sendMessage(any(), any(), any()) }
        // Retried item deleted on success
        coVerify(exactly = 1) { dao.delete(match { it.id == 10L }) }
    }

    @Test
    fun `flushAllPending_emptyRetryables_returnsPendingResult`() = runTest(testDispatcher) {
        val pendingItem = entity(id = 1L, entityId = "1")
        coEvery { dao.getPendingChanges(any()) } returns listOf(pendingItem)
        coEvery { dao.getRetryableFailed() } returns emptyList()
        setupReachableNode("node-1")
        setupSuccessfulMessage()

        val result = queue.flushAllPending()

        // Result comes from the flush() call only — no retries happened
        assertEquals(PendingSyncQueue.FlushResult(sent = 1, failed = 0), result)
        coVerify(exactly = 1) { messageClient.sendMessage(any(), any(), any()) }
        coVerify(exactly = 1) { dao.getRetryableFailed() }
    }
}
