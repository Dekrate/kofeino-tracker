package pl.dekrate.kofeino.tracker.di

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [WearableModule] in the phone (:app) module.
 *
 * Uses mockkStatic to intercept the Play Services Wearable API static methods
 * so no real wearable hardware or emulator is required.
 */
class WearableModuleTest {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var mockMessageClient: MessageClient

    @MockK
    private lateinit var mockDataClient: DataClient

    @MockK
    private lateinit var mockCapabilityClient: CapabilityClient

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        // Intercept Play Services static methods
        mockkStatic(Wearable::class)
        every { Wearable.getMessageClient(context) } returns mockMessageClient
        every { Wearable.getDataClient(context) } returns mockDataClient
        every { Wearable.getCapabilityClient(context) } returns mockCapabilityClient
    }

    @After
    fun tearDown() {
        unmockkStatic(Wearable::class)
    }

    // --- MessageClient ---

    @Test
    fun `provideMessageClient delegates to Wearable API and returns instance`() {
        val client = WearableModule.provideMessageClient(context)

        assert(client === mockMessageClient) { "Must return the instance from Wearable API" }
        verify(exactly = 1) { Wearable.getMessageClient(context) }
    }

    @Test
    fun `provideMessageClient passes correct context to Wearable API`() {
        WearableModule.provideMessageClient(context)

        verify(exactly = 1) { Wearable.getMessageClient(context) }
    }

    // --- DataClient ---

    @Test
    fun `provideDataClient delegates to Wearable API and returns instance`() {
        val client = WearableModule.provideDataClient(context)

        assert(client === mockDataClient) { "Must return the instance from Wearable API" }
        verify(exactly = 1) { Wearable.getDataClient(context) }
    }

    @Test
    fun `provideDataClient passes correct context to Wearable API`() {
        WearableModule.provideDataClient(context)

        verify(exactly = 1) { Wearable.getDataClient(context) }
    }

    // --- CapabilityClient ---

    @Test
    fun `provideCapabilityClient delegates to Wearable API and returns instance`() {
        val client = WearableModule.provideCapabilityClient(context)

        assert(client === mockCapabilityClient) { "Must return the instance from Wearable API" }
        verify(exactly = 1) { Wearable.getCapabilityClient(context) }
    }

    @Test
    fun `provideCapabilityClient passes correct context to Wearable API`() {
        WearableModule.provideCapabilityClient(context)

        verify(exactly = 1) { Wearable.getCapabilityClient(context) }
    }

    // --- Edge cases & error handling ---

    @Test
    fun `successive direct calls return same mock from Wearable API`() {
        val first = WearableModule.provideMessageClient(context)
        val second = WearableModule.provideMessageClient(context)

        assert(first === mockMessageClient) { "First call must return the instance from Wearable API" }
        assert(second === mockMessageClient) { "Second call must return the instance from Wearable API" }
        verify(exactly = 2) { Wearable.getMessageClient(context) }
    }
}
