package pl.dekrate.kofeino.tracker.data.sync

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ExecutionException

/**
 * Unit tests for [RealTimeSyncService].
 *
 * Uses [mockkStatic] on [Tasks] with a single [any] matcher for
 * [Tasks.await] to avoid generic type-erasure collisions when stubbing
 * for both [CapabilityInfo] and [Int] results. [answers] provides
 * sequenced return values matching the order of calls in production.
 */
@Suppress("SwallowedException")
class RealTimeSyncServiceTest {

    private val pendingSyncQueue: PendingSyncQueue = mockk()
    private val messageClient: MessageClient = mockk()
    private val capabilityClient: CapabilityClient = mockk()
    private lateinit var service: RealTimeSyncService

    @Before
    fun setUp() {
        service = RealTimeSyncService(pendingSyncQueue, messageClient, capabilityClient)
        mockkStatic(Tasks::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Tasks::class)
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private fun capabilityInfoWithNodes(vararg nodeIds: String): CapabilityInfo {
        val nodeList = nodeIds.map { id ->
            object : Node {
                override fun getId(): String = id
                override fun getDisplayName(): String = id
                override fun isNearby(): Boolean = true
            }
        }
        return object : CapabilityInfo {
            override fun getName(): String = RealTimeSyncService.SYNC_CAPABILITY_NAME
            override fun getNodes(): Set<Node> = nodeList.toSet()
        }
    }

    /** Create a mock [Task] that [Tasks.await] can work with. */
    private fun <T> completedTask(result: T): Task<T> {
        val task = mockk<Task<T>>()
        every { task.isComplete } returns true
        every { task.isSuccessful } returns true
        every { task.result } returns result
        return task
    }

    private fun <T> failedTask(exception: Exception): Task<T> {
        val task = mockk<Task<T>>()
        every { task.isComplete } returns true
        every { task.isSuccessful } returns false
        every { task.exception } returns exception
        return task
    }

    // ------------------------------------------------------------------
    // Helpers: sequence Tasks.await answers
    //
    // Tasks.await() is called twice in propagateChange:
    //   1. For CapabilityClient.getCapability().await()
    //   2. For MessageClient.sendMessage().await()
    //
    // We use a single `answers { ... }` block that returns pre-queued results.
    // ------------------------------------------------------------------

    /**
     * Configure a sequenced answer for [Tasks.await] — returns values in
     * call order.
     */
    private fun <T> queueTaskResults(vararg results: T) {
        val iterator = results.iterator()
        every { Tasks.await<Any>(any()) } answers {
            if (iterator.hasNext()) iterator.next()
            else error("Tasks.await called more times than results queued")
        }
    }

    /**
     * Configure [Tasks.await] to throw on the first call.
     *
     * Wraps the exception in [ExecutionException] to match real [Tasks.await] behavior.
     */
    private fun queueTaskThrow(exception: Exception) {
        every { Tasks.await<Any>(any()) } throws ExecutionException(exception)
    }

    // ------------------------------------------------------------------
    // 1. Enqueue is always called (before send)
    // ------------------------------------------------------------------

    @Test
    fun `propagateChange enqueues change before attempting send`() = runTest {
        coEvery { pendingSyncQueue.enqueue(any(), any(), any(), any()) } just Runs
        every { capabilityClient.getCapability(any(), any()) } returns completedTask(capabilityInfoWithNodes())
        queueTaskResults(capabilityInfoWithNodes())

        service.propagateChange("intake", "42", "INSERT", """{"caffeineMg":63}""")

        coVerify(exactly = 1) { pendingSyncQueue.enqueue("intake", "42", "INSERT", """{"caffeineMg":63}""") }
    }

    @Test
    fun `propagateChange enqueues even when send fails`() = runTest {
        coEvery { pendingSyncQueue.enqueue(any(), any(), any(), any()) } just Runs
        every { capabilityClient.getCapability(any(), any()) } returns completedTask(capabilityInfoWithNodes("node-1"))
        every { messageClient.sendMessage(any(), any(), any()) } returns completedTask(1)
        val iterator = mutableListOf<Any>(capabilityInfoWithNodes("node-1")).iterator()
        every { Tasks.await<Any>(any()) } answers {
            if (iterator.hasNext()) iterator.next()
            else throw ExecutionException(RuntimeException("Send failed"))
        }

        service.propagateChange("intake", "42", "UPDATE", """{}""")

        coVerify(exactly = 1) { pendingSyncQueue.enqueue("intake", "42", "UPDATE", """{}""") }
    }

    // ------------------------------------------------------------------
    // 2. Send via MessageClient when node reachable
    // ------------------------------------------------------------------

    @Test
    fun `propagateChange sends message when node is reachable`() = runTest {
        coEvery { pendingSyncQueue.enqueue(any(), any(), any(), any()) } just Runs
        every { capabilityClient.getCapability(any(), any()) } returns completedTask(capabilityInfoWithNodes("node-1"))
        queueTaskResults(capabilityInfoWithNodes("node-1"), 1)
        val payloadSlot = slot<ByteArray>()
        every { messageClient.sendMessage("node-1", "/sync/intake/insert", capture(payloadSlot)) } returns completedTask(1)

        service.propagateChange("intake", "42", "INSERT", """{"caffeineMg":63}""")

        verify(exactly = 1) { messageClient.sendMessage("node-1", "/sync/intake/insert", any()) }
        val sentPayload = payloadSlot.captured.toString(Charsets.UTF_8)
        assert(sentPayload == """{"caffeineMg":63}""") {
            "Expected payload to match, got: $sentPayload"
        }
    }

    @Test
    fun `propagateChange sends drink entity with correct path`() = runTest {
        coEvery { pendingSyncQueue.enqueue(any(), any(), any(), any()) } just Runs
        every { capabilityClient.getCapability(any(), any()) } returns completedTask(capabilityInfoWithNodes("watch-1"))
        queueTaskResults(capabilityInfoWithNodes("watch-1"), 1)
        every { messageClient.sendMessage("watch-1", "/sync/drink/update", any()) } returns completedTask(1)

        service.propagateChange("drink", "7", "UPDATE", """{"name":"Latte"}""")

        verify { messageClient.sendMessage("watch-1", "/sync/drink/update", any()) }
    }

    @Test
    fun `propagateChange sends DELETE operation on correct path`() = runTest {
        coEvery { pendingSyncQueue.enqueue(any(), any(), any(), any()) } just Runs
        every { capabilityClient.getCapability(any(), any()) } returns completedTask(capabilityInfoWithNodes("node-1"))
        queueTaskResults(capabilityInfoWithNodes("node-1"), 1)
        every { messageClient.sendMessage("node-1", "/sync/intake/delete", any()) } returns completedTask(1)

        service.propagateChange("intake", "99", "DELETE", """{}""")

        verify { messageClient.sendMessage("node-1", "/sync/intake/delete", any()) }
    }

    // ------------------------------------------------------------------
    // 3. No reachable node → silent skip
    // ------------------------------------------------------------------

    @Test
    fun `propagateChange skips send when no reachable node`() = runTest {
        coEvery { pendingSyncQueue.enqueue(any(), any(), any(), any()) } just Runs
        every { capabilityClient.getCapability(any(), any()) } returns completedTask(capabilityInfoWithNodes())
        queueTaskResults(capabilityInfoWithNodes())

        service.propagateChange("intake", "42", "INSERT", """{}""")

        verify(exactly = 0) { messageClient.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `propagateChange skips send when getCapability fails`() = runTest {
        coEvery { pendingSyncQueue.enqueue(any(), any(), any(), any()) } just Runs
        every { capabilityClient.getCapability(any(), any()) } returns completedTask(capabilityInfoWithNodes("node-1"))
        queueTaskThrow(RuntimeException("Capability check failed"))

        service.propagateChange("intake", "42", "DELETE", """{}""")

        verify(exactly = 0) { messageClient.sendMessage(any(), any(), any()) }
    }

    // ------------------------------------------------------------------
    // 4. Node resolution — first reachable is used
    // ------------------------------------------------------------------

    @Test
    fun `propagateChange uses first reachable node when multiple available`() = runTest {
        coEvery { pendingSyncQueue.enqueue(any(), any(), any(), any()) } just Runs
        every { capabilityClient.getCapability(any(), any()) } returns completedTask(capabilityInfoWithNodes("node-alpha", "node-beta"))
        queueTaskResults(capabilityInfoWithNodes("node-alpha", "node-beta"), 1)
        every { messageClient.sendMessage("node-alpha", any(), any()) } returns completedTask(1)

        service.propagateChange("intake", "1", "INSERT", """{}""")

        verify { messageClient.sendMessage("node-alpha", any(), any()) }
        verify(exactly = 0) { messageClient.sendMessage("node-beta", any(), any()) }
    }

    // ------------------------------------------------------------------
    // 5. Payload bytes
    // ------------------------------------------------------------------

    @Test
    fun `propagateChange passes UTF-8 bytes as message data`() = runTest {
        val payload = """{"drinkName":"Kawa z mlekiem","caffeineMg":95}"""
        coEvery { pendingSyncQueue.enqueue(any(), any(), any(), any()) } just Runs
        every { capabilityClient.getCapability(any(), any()) } returns completedTask(capabilityInfoWithNodes("node-1"))
        queueTaskResults(capabilityInfoWithNodes("node-1"), 1)
        val payloadSlot = slot<ByteArray>()
        every { messageClient.sendMessage("node-1", any(), capture(payloadSlot)) } returns completedTask(1)

        service.propagateChange("intake", "42", "UPDATE", payload)

        verify(exactly = 1) { messageClient.sendMessage("node-1", any(), capture(payloadSlot)) }
        val sentPayload = payloadSlot.captured.toString(Charsets.UTF_8)
        assert(sentPayload == payload) {
            "Expected payload '$payload', got '$sentPayload'"
        }
    }

    // ------------------------------------------------------------------
    // 6. Graceful degradation
    // ------------------------------------------------------------------

    @Test
    fun `propagateChange does not throw when capability client throws`() = runTest {
        coEvery { pendingSyncQueue.enqueue(any(), any(), any(), any()) } just Runs
        every { capabilityClient.getCapability(any(), any()) } returns completedTask(capabilityInfoWithNodes("node-1"))
        queueTaskThrow(RuntimeException("Transient fault"))

        // Should not throw
        service.propagateChange("intake", "1", "INSERT", """{}""")
    }
}
