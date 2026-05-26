package pl.dekrate.kofeino.tracker.data.sync

import app.cash.turbine.expectNoEvents
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncDeviceStateManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var manager: SyncDeviceStateManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        manager = SyncDeviceStateManager()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Initial State ────────────────────────────────────────

    @Test
    fun `initial device states is empty`() = runTest {
        assertTrue(manager.deviceStates.value.isEmpty())
    }

    @Test
    fun `initial hasActiveSync is false`() {
        assertFalse(manager.hasActiveSync())
    }

    @Test
    fun `getState for unknown device returns IDLE`() {
        val state = manager.getState("unknown_node")
        assertEquals(SyncDeviceState.State.IDLE, state.state)
        assertEquals("unknown_node", state.deviceId)
    }

    // ── onSyncStarted ────────────────────────────────────────

    @Test
    fun `onSyncStarted transitions to SYNCING`() = runTest {
        manager.onSyncStarted("node_1")

        val state = manager.getState("node_1")
        assertEquals(SyncDeviceState.State.SYNCING, state.state)
        assertNull(state.lastError)
    }

    @Test
    fun `onSyncStarted emits via deviceStates flow`() = runTest {
        manager.deviceStates.test {
            assertTrue(awaitItem().isEmpty()) // initial empty

            manager.onSyncStarted("node_1")
            val states = awaitItem()
            assertEquals(SyncDeviceState.State.SYNCING, states["node_1"]?.state)
        }
    }

    @Test
    fun `onSyncStarted is idempotent when already SYNCING`() = runTest {
        manager.onSyncStarted("node_1")
        manager.onSyncStarted("node_1") // second call

        // Should only emit once (from first call)
        val state = manager.getState("node_1")
        assertEquals(SyncDeviceState.State.SYNCING, state.state)
    }

    @Test
    fun `onSyncStarted for second device tracks independently`() = runTest {
        manager.onSyncStarted("node_1")
        manager.onSyncStarted("node_2")

        assertEquals(
            SyncDeviceState.State.SYNCING,
            manager.getState("node_1").state
        )
        assertEquals(
            SyncDeviceState.State.SYNCING,
            manager.getState("node_2").state
        )
        assertTrue(manager.hasActiveSync())
    }

    // ── onSyncCompleted ──────────────────────────────────────

    @Test
    fun `onSyncCompleted transitions to IDLE with timestamp`() = runTest {
        manager.onSyncStarted("node_1")
        manager.onSyncCompleted("node_1")

        val state = manager.getState("node_1")
        assertEquals(SyncDeviceState.State.IDLE, state.state)
        assertNull(state.lastError)
        assertFalse(state.needsFullResync)
        assertTrue(state.lastSyncTimestamp > 0)
    }

    @Test
    fun `onSyncCompleted without prior start is safe`() = runTest {
        manager.onSyncCompleted("node_1")

        val state = manager.getState("node_1")
        assertEquals(SyncDeviceState.State.IDLE, state.state)
    }

    @Test
    fun `onSyncCompleted clears needsFullResync`() = runTest {
        manager.onSyncStarted("node_1")
        manager.onSyncFailed("node_1", "Error")
        manager.onSyncStarted("node_1")
        manager.onSyncCompleted("node_1")

        assertFalse(manager.getState("node_1").needsFullResync)
    }

    // ── onSyncFailed ─────────────────────────────────────────

    @Test
    fun `onSyncFailed transitions to FAILED with error`() = runTest {
        manager.onSyncFailed("node_1", "Network timeout")

        val state = manager.getState("node_1")
        assertEquals(SyncDeviceState.State.FAILED, state.state)
        assertEquals("Network timeout", state.lastError)
    }

    @Test
    fun `onSyncFailed mid-sync sets needsFullResync`() = runTest {
        manager.onSyncStarted("node_1")
        manager.onSyncFailed("node_1", "Send failure")

        assertTrue(manager.getState("node_1").needsFullResync)
    }

    @Test
    fun `onSyncFailed while IDLE does not set needsFullResync`() = runTest {
        manager.onSyncFailed("node_1", "Offline")

        assertFalse(manager.getState("node_1").needsFullResync)
    }

    // ── hasActiveSync ────────────────────────────────────────

    @Test
    fun `hasActiveSync is true when device is SYNCING`() = runTest {
        manager.onSyncStarted("node_1")
        assertTrue(manager.hasActiveSync())
    }

    @Test
    fun `hasActiveSync is false when all devices IDLE`() = runTest {
        manager.onSyncStarted("node_1")
        manager.onSyncCompleted("node_1")
        assertFalse(manager.hasActiveSync())
    }

    @Test
    fun `hasActiveSync is false when all devices FAILED`() = runTest {
        manager.onSyncStarted("node_1")
        manager.onSyncFailed("node_1", "Error")
        assertFalse(manager.hasActiveSync())
    }

    // ── onDeviceDisconnected ─────────────────────────────────

    @Test
    fun `onDeviceDisconnected mid-sync marks FAILED and needsFullResync`() = runTest {
        manager.onSyncStarted("node_1")
        manager.onDeviceDisconnected("node_1")

        val state = manager.getState("node_1")
        assertEquals(SyncDeviceState.State.FAILED, state.state)
        assertEquals("Device disconnected mid-sync", state.lastError)
        assertTrue(state.needsFullResync)
    }

    @Test
    fun `onDeviceDisconnected while IDLE stays IDLE`() = runTest {
        manager.onDeviceDisconnected("node_1")

        val state = manager.getState("node_1")
        assertEquals(SyncDeviceState.State.IDLE, state.state)
        assertNull(state.lastError)
    }

    @Test
    fun `onDeviceDisconnected while FAILED preserves FAILED state`() = runTest {
        manager.onSyncFailed("node_1", "Previous error")
        manager.onDeviceDisconnected("node_1")

        val state = manager.getState("node_1")
        assertEquals(SyncDeviceState.State.FAILED, state.state)
    }

    // ── onDeviceConnected ────────────────────────────────────

    @Test
    fun `onDeviceConnected returns false when no prior state`() = runTest {
        val needsResync = manager.onDeviceConnected("new_node")
        assertFalse(needsResync)
    }

    @Test
    fun `onDeviceConnected returns false when sync completed normally`() = runTest {
        manager.onSyncStarted("node_1")
        manager.onSyncCompleted("node_1")

        val needsResync = manager.onDeviceConnected("node_1")
        assertFalse(needsResync)
    }

    @Test
    fun `onDeviceConnected returns true when needsFullResync was set`() = runTest {
        manager.onSyncStarted("node_1")
        manager.onSyncFailed("node_1", "Mid-sync failure")

        val needsResync = manager.onDeviceConnected("node_1")
        assertTrue(needsResync)
    }

    @Test
    fun `onDeviceConnected resets needsFullResync after returning true`() = runTest {
        manager.onSyncStarted("node_1")
        manager.onDeviceDisconnected("node_1")
        manager.onDeviceConnected("node_1") // returns true, resets

        assertFalse(manager.getState("node_1").needsFullResync)
    }

    @Test
    fun `onDeviceConnected after disconnect resets to IDLE`() = runTest {
        manager.onSyncStarted("node_1")
        manager.onDeviceDisconnected("node_1")
        manager.onDeviceConnected("node_1")

        assertEquals(SyncDeviceState.State.IDLE, manager.getState("node_1").state)
    }

    // ── Multiple Devices ─────────────────────────────────────

    @Test
    fun `multiple devices tracked independently`() = runTest {
        manager.onSyncStarted("watch_1")
        manager.onSyncStarted("watch_2")

        assertTrue(manager.hasActiveSync())

        manager.onSyncCompleted("watch_1")
        assertTrue(manager.hasActiveSync()) // watch_2 still SYNCING

        manager.onSyncCompleted("watch_2")
        assertFalse(manager.hasActiveSync())
    }

    @Test
    fun `one device failure does not affect another`() = runTest {
        manager.onSyncStarted("node_a")
        manager.onSyncStarted("node_b")
        manager.onSyncFailed("node_a", "Error A")

        assertEquals(SyncDeviceState.State.FAILED, manager.getState("node_a").state)
        assertEquals(SyncDeviceState.State.SYNCING, manager.getState("node_b").state)
    }

    // ── reset ────────────────────────────────────────────────

    @Test
    fun `reset clears all state`() = runTest {
        manager.onSyncStarted("node_1")
        manager.onSyncStarted("node_2")
        manager.reset()

        assertTrue(manager.deviceStates.value.isEmpty())
        assertFalse(manager.hasActiveSync())
    }

    // ── Edge Cases ───────────────────────────────────────────

    @Test
    fun `SYNCING to SYNCING does not emit duplicate`() = runTest {
        manager.deviceStates.test {
            awaitItem() // initial empty
            manager.onSyncStarted("node_1")
            awaitItem() // SYNCING
            manager.onSyncStarted("node_1") // already SYNCING
            expectNoEvents() // Verify no emission occurs
        }
    }

    @Test
    fun `FAILED to SYNCING then COMPLETED preserves lastSyncTimestamp`() = runTest {
        manager.onSyncFailed("node_1", "Error")
        val failedState = manager.getState("node_1")
        val timestampBefore = failedState.lastSyncTimestamp

        manager.onSyncStarted("node_1")
        manager.onSyncCompleted("node_1")

        val completedState = manager.getState("node_1")
        assertTrue(completedState.lastSyncTimestamp > timestampBefore)
    }

    @Test
    fun `FAILED device disconnect preserves needsFullResync`() = runTest {
        manager.onSyncFailed("node_1", "Error")
        manager.onDeviceDisconnected("node_1")

        // FAILED with no prior SYNCING → needsFullResync stays false
        assertFalse(manager.getState("node_1").needsFullResync)
    }
}
