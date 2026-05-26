package pl.dekrate.kofeino.data.sync

import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import pl.dekrate.kofeino.data.sync.IncomingSyncProcessor
import pl.dekrate.kofeino.data.sync.WearableDataLayerManager.Companion.SYNC_CAPABILITY_NAME

/**
 * Unit tests for wear-module [WearableDataLayerManager].
 *
 * Mirrors the phone module test pattern. Tests verify:
 * - Listeners are registered on [register] and unregistered on [unregister]
 * - Graceful failure when Wear OS is unavailable
 * - Standalone mode — no crash when no paired device
 * - Event handlers do not crash on incoming data/messages/capabilities
 */
class WearableDataLayerManagerTest {

    @MockK
    private lateinit var dataClient: DataClient

    @MockK
    private lateinit var messageClient: MessageClient

    @MockK
    private lateinit var capabilityClient: CapabilityClient

    @MockK
    private lateinit var incomingSyncProcessor: IncomingSyncProcessor

    @MockK
    private lateinit var syncStatusTracker: SyncStatusTracker

    @MockK
    private lateinit var fullSyncManager: FullSyncManager

    private lateinit var manager: WearableDataLayerManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        every { dataClient.addListener(any<DataClient.OnDataChangedListener>()) } returns mockk()
        every { dataClient.removeListener(any<DataClient.OnDataChangedListener>()) } returns mockk()
        every { messageClient.addListener(any<MessageClient.OnMessageReceivedListener>()) } returns mockk()
        every { messageClient.removeListener(any<MessageClient.OnMessageReceivedListener>()) } returns mockk()
        every { capabilityClient.addListener(any<CapabilityClient.OnCapabilityChangedListener>(), any<String>()) } returns mockk()
        every { capabilityClient.removeListener(any<CapabilityClient.OnCapabilityChangedListener>()) } returns mockk()

        manager = WearableDataLayerManager(dataClient, messageClient, capabilityClient, incomingSyncProcessor, syncStatusTracker, fullSyncManager)
    }

    @After
    fun tearDown() {
        manager.unregister()
    }

    // ------------------------------------------------------------------
    // 1. Registration — all three listeners registered
    // ------------------------------------------------------------------

    @Test
    fun `register adds DataClient listener`() {
        manager.register()
        verify(exactly = 1) { dataClient.addListener(any<DataClient.OnDataChangedListener>()) }
    }

    @Test
    fun `register adds MessageClient listener`() {
        manager.register()
        verify(exactly = 1) { messageClient.addListener(any<MessageClient.OnMessageReceivedListener>()) }
    }

    @Test
    fun `register adds CapabilityClient listener with correct capability name`() {
        manager.register()
        verify(exactly = 1) {
            capabilityClient.addListener(
                any<CapabilityClient.OnCapabilityChangedListener>(),
                SYNC_CAPABILITY_NAME
            )
        }
    }

    @Test
    fun `register is idempotent when called multiple times`() {
        manager.register()
        manager.register()
        manager.register()
        verify(exactly = 1) { dataClient.addListener(any()) }
        verify(exactly = 1) { messageClient.addListener(any()) }
        verify(exactly = 1) { capabilityClient.addListener(any(), any()) }
    }

    // ------------------------------------------------------------------
    // 2. Unregistration — all three listeners removed
    // ------------------------------------------------------------------

    @Test
    fun `unregister removes DataClient listener`() {
        manager.register()
        manager.unregister()
        verify(exactly = 1) { dataClient.removeListener(any<DataClient.OnDataChangedListener>()) }
    }

    @Test
    fun `unregister removes MessageClient listener`() {
        manager.register()
        manager.unregister()
        verify(exactly = 1) { messageClient.removeListener(any<MessageClient.OnMessageReceivedListener>()) }
    }

    @Test
    fun `unregister removes CapabilityClient listener`() {
        manager.register()
        manager.unregister()
        verify(exactly = 1) { capabilityClient.removeListener(any<CapabilityClient.OnCapabilityChangedListener>()) }
    }

    @Test
    fun `unregister without prior register does not throw`() {
        manager.unregister()
    }

    // ------------------------------------------------------------------
    // 3. Graceful degradation — Wear OS unavailable
    // ------------------------------------------------------------------

    @Test
    fun `register does not crash when DataClient throws`() {
        every { dataClient.addListener(any<DataClient.OnDataChangedListener>()) } throws RuntimeException("Wear not available")
        manager.register()
        verify(exactly = 1) { messageClient.addListener(any()) }
        verify(exactly = 1) { capabilityClient.addListener(any(), any()) }
    }

    @Test
    fun `register does not crash when all clients throw`() {
        every { dataClient.addListener(any()) } throws RuntimeException("Fail")
        every { messageClient.addListener(any()) } throws RuntimeException("Fail")
        every { capabilityClient.addListener(any(), any()) } throws RuntimeException("Fail")
        manager.register()
    }

    @Test
    fun `register does not crash when MessageClient throws`() {
        every { messageClient.addListener(any<MessageClient.OnMessageReceivedListener>()) } throws RuntimeException("Fail")
        manager.register()
        verify(exactly = 1) { dataClient.addListener(any()) }
        verify(exactly = 1) { capabilityClient.addListener(any(), any()) }
    }

    @Test
    fun `unregister does not crash when clients throw`() {
        every { dataClient.removeListener(any()) } throws RuntimeException("Fail")
        every { messageClient.removeListener(any()) } throws RuntimeException("Fail")
        every { capabilityClient.removeListener(any()) } throws RuntimeException("Fail")
        manager.register()
        manager.unregister()
    }

    // ------------------------------------------------------------------
    // 4. Incoming events — handlers do not crash
    // ------------------------------------------------------------------

    @Test
    fun `message listener handles sync message without crash`() {
        val messageSlot = slot<MessageClient.OnMessageReceivedListener>()
        every { messageClient.addListener(capture(messageSlot)) } returns mockk()
        manager.register()

        val mockEvent = mockk<MessageEvent> {
            every { path } returns "/sync/intake/update"
            every { sourceNodeId } returns "watch-node-1"
            every { data } returns """{"caffeineMg":63}""".toByteArray()
        }
        messageSlot.captured.onMessageReceived(mockEvent)
    }

    @Test
    fun `data listener handles changed event without crash`() {
        val dataSlot = slot<DataClient.OnDataChangedListener>()
        every { dataClient.addListener(capture(dataSlot)) } returns mockk()
        manager.register()

        val mockDataItem = mockk<DataItem>(relaxed = true) {
            every { uri } returns mockk(relaxed = true)
        }
        val mockEvent = mockk<DataEvent> {
            every { type } returns DataEvent.TYPE_CHANGED
            every { dataItem } returns mockDataItem
        }
        val buffer = mockk<DataEventBuffer> {
            every { iterator() } returns mutableListOf(mockEvent).listIterator()
            every { release() } just runs
        }
        dataSlot.captured.onDataChanged(buffer)
    }

    @Test
    fun `data listener handles deleted event without crash`() {
        val dataSlot = slot<DataClient.OnDataChangedListener>()
        every { dataClient.addListener(capture(dataSlot)) } returns mockk()
        manager.register()

        val mockDataItem = mockk<DataItem>(relaxed = true) {
            every { uri } returns mockk(relaxed = true)
        }
        val mockEvent = mockk<DataEvent> {
            every { type } returns DataEvent.TYPE_DELETED
            every { dataItem } returns mockDataItem
        }
        val buffer = mockk<DataEventBuffer> {
            every { iterator() } returns mutableListOf(mockEvent).listIterator()
            every { release() } just runs
        }
        dataSlot.captured.onDataChanged(buffer)
    }

    @Test
    fun `capability listener handles nodes without crash`() {
        val capabilitySlot = slot<CapabilityClient.OnCapabilityChangedListener>()
        every { capabilityClient.addListener(capture(capabilitySlot), any()) } returns mockk()
        manager.register()

        val mockNode = mockk<Node> {
            every { id } returns "watch-node-1"
            every { displayName } returns "Smartwatch"
            every { isNearby } returns true
        }
        val capabilityInfo = mockk<CapabilityInfo>(relaxed = true)
        every { capabilityInfo.name } returns SYNC_CAPABILITY_NAME
        every { capabilityInfo.nodes } returns setOf(mockNode)
        capabilitySlot.captured.onCapabilityChanged(capabilityInfo)
    }

    @Test
    fun `capability listener handles no nodes without crash`() {
        val capabilitySlot = slot<CapabilityClient.OnCapabilityChangedListener>()
        every { capabilityClient.addListener(capture(capabilitySlot), any()) } returns mockk()
        manager.register()

        val capabilityInfo = mockk<CapabilityInfo>(relaxed = true)
        every { capabilityInfo.name } returns SYNC_CAPABILITY_NAME
        every { capabilityInfo.nodes } returns emptySet()
        capabilitySlot.captured.onCapabilityChanged(capabilityInfo)
    }

    // ------------------------------------------------------------------
    // 5. Full lifecycle: register → events → unregister
    // ------------------------------------------------------------------

    @Test
    fun `full lifecycle does not leak listeners`() {
        manager.register()
        manager.unregister()
        verify(exactly = 1) { dataClient.removeListener(any()) }
        verify(exactly = 1) { messageClient.removeListener(any()) }
        verify(exactly = 1) { capabilityClient.removeListener(any()) }

        // Re-register should work (app restart scenario)
        manager.register()
        verify(exactly = 2) { dataClient.addListener(any()) }
    }
}
