package pl.dekrate.kofeino.tracker.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented integration test for phone-module [WearableDataLayerManager].
 *
 * Uses Hilt's [BindValue] to supply mocked Wearable clients so that the
 * test can verify the manager is correctly wired to the DI graph.
 *
 * The Wearable API static methods are mocked via [mockkStatic] since
 * no real Wear OS hardware is available on the test device/emulator.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class WearableDataLayerManagerInstrumentedTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    val dataClient: DataClient = mockk()

    @BindValue
    val messageClient: MessageClient = mockk()

    @BindValue
    val capabilityClient: CapabilityClient = mockk()

    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()

        // Stub all listener registration methods
        every { dataClient.addListener(any<DataClient.OnDataChangedListener>()) } returns mockk()
        every { dataClient.removeListener(any<DataClient.OnDataChangedListener>()) } returns mockk()
        every { messageClient.addListener(any<MessageClient.OnMessageReceivedListener>()) } returns mockk()
        every { messageClient.removeListener(any<MessageClient.OnMessageReceivedListener>()) } returns mockk()
        every { capabilityClient.addListener(any<CapabilityClient.OnCapabilityChangedListener>(), any<String>()) } returns mockk()
        every { capabilityClient.removeListener(any<CapabilityClient.OnCapabilityChangedListener>()) } returns mockk()

        // Intercept Wearable static methods so DI module resolves to our mocks
        mockkStatic(Wearable::class)
        every { Wearable.getMessageClient(any<Context>()) } returns messageClient
        every { Wearable.getDataClient(any<Context>()) } returns dataClient
        every { Wearable.getCapabilityClient(any<Context>()) } returns capabilityClient
    }

    @After
    fun tearDown() {
        unmockkStatic(Wearable::class)
    }

    @Test
    fun register_adds_all_three_listeners() {
        val manager = WearableDataLayerManager(dataClient, messageClient, capabilityClient)
        manager.register()

        verify(exactly = 1) { dataClient.addListener(any<DataClient.OnDataChangedListener>()) }
        verify(exactly = 1) { messageClient.addListener(any<MessageClient.OnMessageReceivedListener>()) }
        verify(exactly = 1) {
            capabilityClient.addListener(
                any<CapabilityClient.OnCapabilityChangedListener>(),
                WearableDataLayerManager.SYNC_CAPABILITY_NAME
            )
        }

        manager.unregister()
    }

    @Test
    fun unregister_removes_all_three_listeners() {
        val manager = WearableDataLayerManager(dataClient, messageClient, capabilityClient)
        manager.register()
        manager.unregister()

        verify(exactly = 1) { dataClient.removeListener(any<DataClient.OnDataChangedListener>()) }
        verify(exactly = 1) { messageClient.removeListener(any<MessageClient.OnMessageReceivedListener>()) }
        verify(exactly = 1) { capabilityClient.removeListener(any<CapabilityClient.OnCapabilityChangedListener>()) }
    }

    @Test
    fun register_handles_failure_gracefully() {
        every { dataClient.addListener(any()) } throws RuntimeException("Wear OS not available")
        every { messageClient.addListener(any()) } throws RuntimeException("Wear OS not available")

        val manager = WearableDataLayerManager(dataClient, messageClient, capabilityClient)
        manager.register() // Must not throw

        // CapabilityClient should still succeed
        verify(exactly = 1) { capabilityClient.addListener(any(), any()) }
        manager.unregister()
    }

    @Test
    fun hilt_injectable_wearable_clients_are_non_null() {
        val msgClient = Wearable.getMessageClient(context)
        val dClient = Wearable.getDataClient(context)
        val capClient = Wearable.getCapabilityClient(context)

        assert(msgClient != null) { "MessageClient must not be null" }
        assert(dClient != null) { "DataClient must not be null" }
        assert(capClient != null) { "CapabilityClient must not be null" }

        // Verify our mocks are returned
        assert(msgClient === messageClient) { "Must be the mock" }
        assert(dClient === dataClient) { "Must be the mock" }
        assert(capClient === capabilityClient) { "Must be the mock" }
    }
}
