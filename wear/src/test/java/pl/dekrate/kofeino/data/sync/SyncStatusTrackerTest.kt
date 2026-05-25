package pl.dekrate.kofeino.data.sync

import app.cash.turbine.test
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
import pl.dekrate.kofeino.common.sync.SyncStatus

@OptIn(ExperimentalCoroutinesApi::class)
class SyncStatusTrackerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var tracker: SyncStatusTracker

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tracker = SyncStatusTracker()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial status is AwaitingDevice`() = runTest {
        assertEquals(SyncStatus.AwaitingDevice, tracker.status.value)
    }

    @Test
    fun `onDeviceConnectionChanged with true transitions to Synced`() = runTest {
        tracker.status.test {
            awaitItem() // initial AwaitingDevice
            tracker.onDeviceConnectionChanged(true)
            assertEquals(SyncStatus.Synced, awaitItem())
        }
    }

    @Test
    fun `onDeviceConnectionChanged with false transitions to AwaitingDevice`() = runTest {
        tracker.status.test {
            awaitItem() // initial AwaitingDevice
            tracker.onDeviceConnectionChanged(true) // first move away from AwaitingDevice
            assertEquals(SyncStatus.Synced, awaitItem())
            tracker.onDeviceConnectionChanged(false)
            assertEquals(SyncStatus.AwaitingDevice, awaitItem())
        }
    }

    @Test
    fun `onDeviceConnectionChanged true then false transitions back to AwaitingDevice`() = runTest {
        tracker.status.test {
            awaitItem() // initial AwaitingDevice
            tracker.onDeviceConnectionChanged(true)
            assertEquals(SyncStatus.Synced, awaitItem())
            tracker.onDeviceConnectionChanged(false)
            assertEquals(SyncStatus.AwaitingDevice, awaitItem())
        }
    }

    @Test
    fun `onSyncStarted transitions to Syncing`() = runTest {
        tracker.status.test {
            awaitItem() // initial AwaitingDevice
            tracker.onDeviceConnectionChanged(true)
            assertEquals(SyncStatus.Synced, awaitItem())
            tracker.onSyncStarted()
            assertEquals(SyncStatus.Syncing, awaitItem())
        }
    }

    @Test
    fun `onSyncCompleted transitions to Synced`() = runTest {
        tracker.status.test {
            awaitItem() // initial AwaitingDevice
            tracker.onSyncStarted()
            assertEquals(SyncStatus.Syncing, awaitItem())
            tracker.onSyncCompleted()
            assertEquals(SyncStatus.Synced, awaitItem())
        }
    }

    @Test
    fun `onSyncFailed transitions to Error with message`() = runTest {
        tracker.status.test {
            awaitItem() // initial AwaitingDevice
            tracker.onSyncFailed("Network timeout")
            val item = awaitItem()
            assert(item is SyncStatus.Error)
            assertEquals("Network timeout", (item as SyncStatus.Error).message)
        }
    }

    @Test
    fun `onSyncFailed then onDeviceConnectionChanged true recovers to Synced`() = runTest {
        tracker.status.test {
            awaitItem() // initial AwaitingDevice
            tracker.onSyncFailed("Error")
            assert(awaitItem() is SyncStatus.Error)
            tracker.onDeviceConnectionChanged(true)
            assertEquals(SyncStatus.Synced, awaitItem())
        }
    }

    @Test
    fun `Syncing then onSyncFailed shows Error`() = runTest {
        tracker.status.test {
            awaitItem() // initial AwaitingDevice
            tracker.onDeviceConnectionChanged(true)
            assertEquals(SyncStatus.Synced, awaitItem())
            tracker.onSyncStarted()
            assertEquals(SyncStatus.Syncing, awaitItem())
            tracker.onSyncFailed("Send failure")
            val item = awaitItem()
            assert(item is SyncStatus.Error)
            assertEquals("Send failure", (item as SyncStatus.Error).message)
        }
    }
}
