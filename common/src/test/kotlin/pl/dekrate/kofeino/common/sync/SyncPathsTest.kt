package pl.dekrate.kofeino.common.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPathsTest {

    // ─── Prefix tests ────────────────────────────────────────────────────

    @Test
    fun `SYNC_PATH_PREFIX should be slash sync`() {
        assertEquals("/sync", SyncPaths.SYNC_PATH_PREFIX)
    }

    @Test
    fun `MESSAGE_PATH_PREFIX should be slash message`() {
        assertEquals("/message", SyncPaths.MESSAGE_PATH_PREFIX)
    }

    @Test
    fun `prefixes should be different`() {
        assertTrue(SyncPaths.SYNC_PATH_PREFIX != SyncPaths.MESSAGE_PATH_PREFIX)
    }

    @Test
    fun `SYNC_PATH_PREFIX should start with slash`() {
        assertTrue(SyncPaths.SYNC_PATH_PREFIX.startsWith("/"))
    }

    @Test
    fun `MESSAGE_PATH_PREFIX should start with slash`() {
        assertTrue(SyncPaths.MESSAGE_PATH_PREFIX.startsWith("/"))
    }

    // ─── Entity path tests ───────────────────────────────────────────────

    @Test
    fun `INTAKES_PATH should start with sync prefix`() {
        assertTrue(SyncPaths.INTAKES_PATH.startsWith(SyncPaths.SYNC_PATH_PREFIX))
    }

    @Test
    fun `DRINKS_PATH should start with sync prefix`() {
        assertTrue(SyncPaths.DRINKS_PATH.startsWith(SyncPaths.SYNC_PATH_PREFIX))
    }

    @Test
    fun `SETTINGS_PATH should start with sync prefix`() {
        assertTrue(SyncPaths.SETTINGS_PATH.startsWith(SyncPaths.SYNC_PATH_PREFIX))
    }

    @Test
    fun `entity paths should be unique`() {
        val paths = setOf(SyncPaths.INTAKES_PATH, SyncPaths.DRINKS_PATH, SyncPaths.SETTINGS_PATH)
        assertEquals(3, paths.size)
    }

    @Test
    fun `entity paths should contain entity type name`() {
        assertTrue(SyncPaths.INTAKES_PATH.contains("intakes"))
        assertTrue(SyncPaths.DRINKS_PATH.contains("drinks"))
        assertTrue(SyncPaths.SETTINGS_PATH.contains("settings"))
    }

    // ─── Message path tests ──────────────────────────────────────────────

    @Test
    fun `MESSAGE_INTAKE_ADDED should start with message prefix`() {
        assertTrue(SyncPaths.MESSAGE_INTAKE_ADDED.startsWith(SyncPaths.MESSAGE_PATH_PREFIX))
    }

    @Test
    fun `MESSAGE_INTAKE_DELETED should start with message prefix`() {
        assertTrue(SyncPaths.MESSAGE_INTAKE_DELETED.startsWith(SyncPaths.MESSAGE_PATH_PREFIX))
    }

    @Test
    fun `MESSAGE_INTAKE_UPDATED should start with message prefix`() {
        assertTrue(SyncPaths.MESSAGE_INTAKE_UPDATED.startsWith(SyncPaths.MESSAGE_PATH_PREFIX))
    }

    @Test
    fun `MESSAGE_DRINK_CREATED should start with message prefix`() {
        assertTrue(SyncPaths.MESSAGE_DRINK_CREATED.startsWith(SyncPaths.MESSAGE_PATH_PREFIX))
    }

    @Test
    fun `MESSAGE_DRINK_UPDATED should start with message prefix`() {
        assertTrue(SyncPaths.MESSAGE_DRINK_UPDATED.startsWith(SyncPaths.MESSAGE_PATH_PREFIX))
    }

    @Test
    fun `MESSAGE_DRINK_DELETED should start with message prefix`() {
        assertTrue(SyncPaths.MESSAGE_DRINK_DELETED.startsWith(SyncPaths.MESSAGE_PATH_PREFIX))
    }

    @Test
    fun `MESSAGE_SETTINGS_CHANGED should start with message prefix`() {
        assertTrue(SyncPaths.MESSAGE_SETTINGS_CHANGED.startsWith(SyncPaths.MESSAGE_PATH_PREFIX))
    }

    @Test
    fun `MESSAGE_FULL_SYNC_REQUEST should start with message prefix`() {
        assertTrue(SyncPaths.MESSAGE_FULL_SYNC_REQUEST.startsWith(SyncPaths.MESSAGE_PATH_PREFIX))
    }

    @Test
    fun `MESSAGE_FULL_SYNC_RESPONSE should start with message prefix`() {
        assertTrue(SyncPaths.MESSAGE_FULL_SYNC_RESPONSE.startsWith(SyncPaths.MESSAGE_PATH_PREFIX))
    }

    @Test
    fun `MESSAGE_SYNC_ACK should start with message prefix`() {
        assertTrue(SyncPaths.MESSAGE_SYNC_ACK.startsWith(SyncPaths.MESSAGE_PATH_PREFIX))
    }

    @Test
    fun `all message paths should be unique`() {
        val paths = setOf(
            SyncPaths.MESSAGE_INTAKE_ADDED,
            SyncPaths.MESSAGE_INTAKE_DELETED,
            SyncPaths.MESSAGE_INTAKE_UPDATED,
            SyncPaths.MESSAGE_DRINK_CREATED,
            SyncPaths.MESSAGE_DRINK_UPDATED,
            SyncPaths.MESSAGE_DRINK_DELETED,
            SyncPaths.MESSAGE_SETTINGS_CHANGED,
            SyncPaths.MESSAGE_FULL_SYNC_REQUEST,
            SyncPaths.MESSAGE_FULL_SYNC_RESPONSE,
            SyncPaths.MESSAGE_SYNC_ACK,
        )
        assertEquals(10, paths.size)
    }

    @Test
    fun `intake paths should contain intake entity name`() {
        assertTrue(SyncPaths.MESSAGE_INTAKE_ADDED.contains("intake"))
        assertTrue(SyncPaths.MESSAGE_INTAKE_DELETED.contains("intake"))
        assertTrue(SyncPaths.MESSAGE_INTAKE_UPDATED.contains("intake"))
    }

    @Test
    fun `drink paths should contain drink entity name`() {
        assertTrue(SyncPaths.MESSAGE_DRINK_CREATED.contains("drink"))
        assertTrue(SyncPaths.MESSAGE_DRINK_UPDATED.contains("drink"))
        assertTrue(SyncPaths.MESSAGE_DRINK_DELETED.contains("drink"))
    }

    // ─── Builder function tests ──────────────────────────────────────────

    @Test
    fun `entityPath with SyncEntityType should build correct path`() {
        assertEquals("/sync/intake", SyncPaths.entityPath(SyncEntityType.INTAKE))
        assertEquals("/sync/drink", SyncPaths.entityPath(SyncEntityType.DRINK))
        assertEquals("/sync/settings", SyncPaths.entityPath(SyncEntityType.SETTINGS))
    }

    @Test
    fun `messagePath with no segments should return prefix only`() {
        assertEquals("/message/", SyncPaths.messagePath())
    }

    @Test
    fun `messagePath with single segment`() {
        assertEquals("/message/test", SyncPaths.messagePath("test"))
    }

    @Test
    fun `messagePath with multiple segments`() {
        assertEquals("/message/intake/added", SyncPaths.messagePath("intake", "added"))
    }

    @Test
    fun `syncPath with no segments should return prefix only`() {
        assertEquals("/sync/", SyncPaths.syncPath())
    }

    @Test
    fun `syncPath with single segment`() {
        assertEquals("/sync/test", SyncPaths.syncPath("test"))
    }

    @Test
    fun `syncPath with multiple segments`() {
        assertEquals("/sync/intake/batch", SyncPaths.syncPath("intake", "batch"))
    }

    // ─── Cross-cutting tests ────────────────────────────────────────────

    @Test
    fun `builders should match manually defined constants`() {
        assertEquals(
            SyncPaths.INTAKES_PATH,
            SyncPaths.syncPath("intakes")
        )
        assertEquals(
            SyncPaths.MESSAGE_INTAKE_ADDED,
            SyncPaths.messagePath("intake-added")
        )
    }

    @Test
    fun `sync paths should not overlap with message paths`() {
        val syncSet = setOf(SyncPaths.INTAKES_PATH, SyncPaths.DRINKS_PATH, SyncPaths.SETTINGS_PATH)
        val messageSet = setOf(
            SyncPaths.MESSAGE_INTAKE_ADDED, SyncPaths.MESSAGE_INTAKE_DELETED,
            SyncPaths.MESSAGE_INTAKE_UPDATED, SyncPaths.MESSAGE_DRINK_CREATED,
            SyncPaths.MESSAGE_DRINK_UPDATED, SyncPaths.MESSAGE_DRINK_DELETED,
            SyncPaths.MESSAGE_SETTINGS_CHANGED,
            SyncPaths.MESSAGE_FULL_SYNC_REQUEST, SyncPaths.MESSAGE_FULL_SYNC_RESPONSE,
            SyncPaths.MESSAGE_SYNC_ACK,
        )
        syncSet.forEach { syncPath ->
            messageSet.forEach { messagePath ->
                assertFalse("Sync path '$syncPath' overlaps with message path '$messagePath'",
                    syncPath == messagePath)
            }
        }
    }

    @Test
    fun `all constants should be non-blank`() {
        assertTrue(SyncPaths.SYNC_PATH_PREFIX.isNotBlank())
        assertTrue(SyncPaths.MESSAGE_PATH_PREFIX.isNotBlank())
        assertTrue(SyncPaths.INTAKES_PATH.isNotBlank())
        assertTrue(SyncPaths.DRINKS_PATH.isNotBlank())
        assertTrue(SyncPaths.SETTINGS_PATH.isNotBlank())
        assertTrue(SyncPaths.MESSAGE_INTAKE_ADDED.isNotBlank())
        assertTrue(SyncPaths.MESSAGE_INTAKE_DELETED.isNotBlank())
        assertTrue(SyncPaths.MESSAGE_INTAKE_UPDATED.isNotBlank())
        assertTrue(SyncPaths.MESSAGE_DRINK_CREATED.isNotBlank())
        assertTrue(SyncPaths.MESSAGE_DRINK_UPDATED.isNotBlank())
        assertTrue(SyncPaths.MESSAGE_DRINK_DELETED.isNotBlank())
        assertTrue(SyncPaths.MESSAGE_SETTINGS_CHANGED.isNotBlank())
        assertTrue(SyncPaths.MESSAGE_FULL_SYNC_REQUEST.isNotBlank())
        assertTrue(SyncPaths.MESSAGE_FULL_SYNC_RESPONSE.isNotBlank())
        assertTrue(SyncPaths.MESSAGE_SYNC_ACK.isNotBlank())
    }
}
