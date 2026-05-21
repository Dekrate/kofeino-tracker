package pl.dekrate.kofeino.di

import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Assert.assertNotNull
import org.junit.Test
import javax.inject.Inject

/**
 * Hilt integration test for [WearableModule] in the wear (:wear) module.
 *
 * Verifies that the Hilt dependency injection graph correctly provides
 * MessageClient, DataClient, and CapabilityClient on a real device/emulator
 * that has Play Services installed.
 *
 * **Note:** These tests require an emulator with Google Play Services
 * (e.g. Wear Google APIs image). Without Play Services these tests
 * will fail at runtime.
 */
@HiltAndroidTest
class WearableModuleIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var messageClient: MessageClient

    @Inject
    lateinit var dataClient: DataClient

    @Inject
    lateinit var capabilityClient: CapabilityClient

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun messageClient_is_injectable() {
        assertNotNull(messageClient, "MessageClient must be injectable via Hilt")
    }

    @Test
    fun dataClient_is_injectable() {
        assertNotNull(dataClient, "DataClient must be injectable via Hilt")
    }

    @Test
    fun capabilityClient_is_injectable() {
        assertNotNull(capabilityClient, "CapabilityClient must be injectable via Hilt")
    }

    @Test
    fun allWearableClients_are_injectable() {
        assertNotNull(messageClient, "MessageClient")
        assertNotNull(dataClient, "DataClient")
        assertNotNull(capabilityClient, "CapabilityClient")
    }
}
