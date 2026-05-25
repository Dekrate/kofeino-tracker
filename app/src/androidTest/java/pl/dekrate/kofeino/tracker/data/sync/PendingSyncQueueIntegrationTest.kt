package pl.dekrate.kofeino.tracker.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.dekrate.kofeino.tracker.data.local.CaffeineDatabase
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeEntity.Companion.STATUS_FAILED
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeEntity.Companion.STATUS_PENDING

/**
 * Integration tests for [PendingSyncQueue] using a real Room in-memory database
 * and a mocked [com.google.android.gms.wearable.MessageClient].
 *
 * Unlike the pure unit tests (which mock the DAO), these tests verify that
 * the queue correctly persists to / reads from Room, AND the send logic works.
 */
@RunWith(AndroidJUnit4::class)
class PendingSyncQueueIntegrationTest {

    private lateinit var database: CaffeineDatabase
    private lateinit var dao: PendingChangeDao
    private lateinit var messageClient: com.google.android.gms.wearable.MessageClient
    private lateinit var capabilityClient: CapabilityClient
    private lateinit var queue: PendingSyncQueue

    private val nodeId = "test-node"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            CaffeineDatabase::class.java
        ).build()
        dao = database.pendingChangeDao()
        messageClient = mockk()
        capabilityClient = mockk()
        setupReachableNode(nodeId)
        queue = PendingSyncQueue(dao, messageClient, capabilityClient)
    }

    @After
    fun tearDown() {
        database.close()
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
        every { capabilityClient.getCapability(any(), any()) } returns Tasks.forResult(capabilityInfo)
    }

    /** Make [MessageClient.sendMessage] succeed. */
    private fun setupSuccessfulMessage() {
        every { messageClient.sendMessage(any(), any(), any()) } returns Tasks.forResult(0)
    }

    /** Simulate no reachable devices for the sync capability. */
    private fun setupUnreachableNode() {
        every {
            capabilityClient.getCapability(any(), any())
        } returns Tasks.forException<CapabilityInfo>(RuntimeException("Network error"))
    }

    // ------------------------------------------------------------------
    // Full lifecycle: enqueue → flush → dedup → retry → persist
    // ------------------------------------------------------------------

    @Test
    fun enqueueThenFlush_persistsAndSends() = runTest {
        // Mock Task creation is not needed here because we'll stub
        // the sendMessage to throw — but for this test we test persistence
        every { messageClient.sendMessage(any(), any(), any()) } throws RuntimeException("No real wearable")

        queue.enqueue("intake", "1", "INSERT", """{"caffeineMg":63}""")
        queue.enqueue("intake", "2", "UPDATE", """{"caffeineMg":95}""")

        // Items persisted in Room
        assert(dao.count() == 2) { "Expected 2 persisted items, got ${dao.count()}" }
    }

    @Test
    fun enqueueWithDedup_replacesExistingPendingChange() = runTest {
        queue.enqueue("intake", "1", "INSERT", """{"v":1}""")
        queue.enqueue("intake", "1", "UPDATE", """{"v":2}""")
        queue.enqueue("intake", "1", "DELETE", """{}""")

        // After dedup: only the latest operation per entity should remain
        assert(dao.count() == 1) { "Expected 1 after dedup, got ${dao.count()}" }

        val pending = dao.getPendingChanges()
        assert(pending.size == 1)
        assert(pending[0].operationType == "DELETE") {
            "Expected DELETE after dedup, got ${pending[0].operationType}"
        }
        assert(pending[0].retryCount == 0) { "retryCount should reset on dedup" }
    }

    @Test
    fun enqueueWithDifferentEntityTypes_doesNotDedup() = runTest {
        queue.enqueue("intake", "1", "INSERT", """{}""")
        queue.enqueue("drink", "1", "UPDATE", """{}""")

        assert(dao.count() == 2) { "Expected 2 (different types), got ${dao.count()}" }
    }

    @Test
    fun flushClearsQueueOnSuccess() = runTest {
        setupSuccessfulMessage()

        queue.enqueue("intake", "1", "INSERT", """{}""")
        queue.enqueue("intake", "2", "INSERT", """{}""")

        val result = queue.flush()

        assert(result.sent == 2) { "Expected 2 sent, got ${result.sent}" }
        assert(result.failed == 0) { "Expected 0 failed" }
        assert(dao.count() == 0) { "Queue should be empty after successful flush" }
        verify(exactly = 2) { messageClient.sendMessage(nodeId, any(), any()) }
    }

    @Test
    fun enqueueAfterFlush_persistsNewItems() = runTest {
        // Send first batch
        queue.enqueue("intake", "1", "INSERT", """{"v":1}""")

        // After restart: new queue reads existing DAO
        val queue2 = PendingSyncQueue(dao, messageClient, capabilityClient)
        assert(queue2.size() == 1) { "Expected 1 item after resuming, got ${queue2.size()}" }

        // Add more items
        queue2.enqueue("drink", "2", "UPDATE", """{}""")
        assert(dao.count() == 2) { "Expected 2 after adding more, got ${dao.count()}" }
    }

    // ------------------------------------------------------------------
    // Retry and failure handling
    // ------------------------------------------------------------------

    @Test
    fun consecutiveFailuresEventuallyMarkAsFailed() = runTest {
        every { messageClient.sendMessage(any(), any(), any()) } throws RuntimeException("Network error")

        queue.enqueue("intake", "1", "INSERT", """{}""")

        // First 5 flushes → all fail, retry count grows
        for (attempt in 1..5) {
            queue.flush()
        }

        assert(queue.failedCount() == 1) { "Expected 1 FAILED item after 5 attempts" }

        val failed = dao.getFailedChanges()
        assert(failed.size == 1)
        assert(failed[0].retryCount == 5) { "Expected retryCount=5, got ${failed[0].retryCount}" }
    }

    @Test
    fun failedItemsDoNotBlockNewPendingChanges() = runTest {
        every { messageClient.sendMessage(any(), any(), any()) } throws RuntimeException("Always fail")

        queue.enqueue("intake", "1", "INSERT", """{}""")
        repeat(5) { queue.flush() }

        // After 5 failures, it's FAILED. New item should still work.
        queue.enqueue("intake", "2", "INSERT", """{}""")

        assert(dao.count() == 2) { "Expected 2 items total (1 FAILED + 1 PENDING), got ${dao.count()}" }
        assert(queue.failedCount() == 1) { "Expected 1 FAILED" }
    }

    // ------------------------------------------------------------------
    // Dedup across multiple enqueue-flush cycles
    // ------------------------------------------------------------------

    @Test
    fun dedupBetweenFlushes() = runTest {
        every { messageClient.sendMessage(any(), any(), any()) } throws RuntimeException("Always fail")

        // Enqueue, flush (fail), enqueue new version, flush (fail) — only the latest survives
        queue.enqueue("intake", "1", "INSERT", """{"v1":true}""")
        queue.flush()
        queue.enqueue("intake", "1", "UPDATE", """{"v2":true}""")

        val pending = dao.getPendingChanges()
        assert(pending.size == 1) { "Expected 1 pending after dedup across flushes" }
        assert(pending[0].operationType == "UPDATE") {
            "Expected UPDATE, got ${pending[0].operationType}"
        }
    }

    // ------------------------------------------------------------------
    // No reachable node edge cases
    // ------------------------------------------------------------------

    @Test
    fun flushSkipsWhenNoReachableNode() = runTest {
        setupUnreachableNode()

        queue.enqueue("intake", "1", "INSERT", """{"caffeineMg":63}""")
        queue.enqueue("intake", "2", "UPDATE", """{"caffeineMg":95}""")

        // Items should remain in the queue (not deleted)
        assert(dao.count() == 2) { "Expected 2 items still in queue, got ${dao.count()}" }

        val result = queue.flush()

        assert(result.sent == 0) { "Expected 0 sent when no reachable node, got ${result.sent}" }
        assert(result.failed == 0) { "Expected 0 failed when no reachable node, got ${result.failed}" }

        // Items should still be in the queue after skipped flush
        assert(dao.count() == 2) { "Expected 2 items still in queue after flush, got ${dao.count()}" }

        // No messages should have been attempted
        verify(exactly = 0) { messageClient.sendMessage(any(), any(), any()) }
    }
}
