package pl.dekrate.kofeino.common.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncStatusTest {

    // ─── Equality tests ───────────────────────────────────────────────────

    @Test
    fun `Synced is equal to itself`() {
        assertEquals(SyncStatus.Synced, SyncStatus.Synced)
    }

    @Test
    fun `AwaitingDevice is equal to itself`() {
        assertEquals(SyncStatus.AwaitingDevice, SyncStatus.AwaitingDevice)
    }

    @Test
    fun `Syncing is equal to itself`() {
        assertEquals(SyncStatus.Syncing, SyncStatus.Syncing)
    }

    @Test
    fun `Error with same message is equal`() {
        assertEquals(SyncStatus.Error("Network unavailable"), SyncStatus.Error("Network unavailable"))
    }

    @Test
    fun `Error with different messages is not equal`() {
        assertNotEquals(SyncStatus.Error("Timeout"), SyncStatus.Error("Network error"))
    }

    // ─── Property tests ───────────────────────────────────────────────────

    @Test
    fun `Error preserves message`() {
        val error = SyncStatus.Error("Something went wrong")
        assertEquals("Something went wrong", error.message)
    }

    @Test
    fun `all states are distinct from each other`() {
        val states = setOf<SyncStatus>(
            SyncStatus.Synced,
            SyncStatus.AwaitingDevice,
            SyncStatus.Syncing,
            SyncStatus.Error("x"),
        )
        assertEquals(4, states.size)
    }

    @Test
    fun `initial status is AwaitingDevice`() {
        assertEquals(SyncStatus.AwaitingDevice, SyncStatus.initial)
    }

    // ─── Exhaustiveness test ──────────────────────────────────────────────

    @Test
    fun `exhaustive when covers all states`() {
        val status: SyncStatus = SyncStatus.Synced
        val description: String = when (status) {
            SyncStatus.Synced -> "synced"
            SyncStatus.AwaitingDevice -> "awaiting"
            SyncStatus.Syncing -> "syncing"
            is SyncStatus.Error -> "error: ${status.message}"
        }
        assertTrue(description.isNotEmpty())
    }

    // ─── Consumer behaviour test ──────────────────────────────────────────

    @Test
    fun `initial is AwaitingDevice when no connection established`() {
        val tracker = FakeSyncStatusConsumer()
        assertEquals(SyncStatus.AwaitingDevice, tracker.currentStatus)
    }

    /**
     * Minimal fake that simulates how a consumer would hold SyncStatus.
     */
    private class FakeSyncStatusConsumer {
        var currentStatus: SyncStatus = SyncStatus.initial
    }
}
