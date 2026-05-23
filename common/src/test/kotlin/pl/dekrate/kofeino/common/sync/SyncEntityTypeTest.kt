package pl.dekrate.kofeino.common.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncEntityTypeTest {

    @Test
    fun `INTAKE should have pathSegment intake`() {
        assertEquals("intake", SyncEntityType.INTAKE.pathSegment)
    }

    @Test
    fun `DRINK should have pathSegment drink`() {
        assertEquals("drink", SyncEntityType.DRINK.pathSegment)
    }

    @Test
    fun `SETTINGS should have pathSegment settings`() {
        assertEquals("settings", SyncEntityType.SETTINGS.pathSegment)
    }

    @Test
    fun `fromPath should resolve intake correctly`() {
        assertEquals(SyncEntityType.INTAKE, SyncEntityType.fromPath("intake"))
    }

    @Test
    fun `fromPath should resolve drink correctly`() {
        assertEquals(SyncEntityType.DRINK, SyncEntityType.fromPath("drink"))
    }

    @Test
    fun `fromPath should resolve settings correctly`() {
        assertEquals(SyncEntityType.SETTINGS, SyncEntityType.fromPath("settings"))
    }

    @Test
    fun `fromPath should be case-insensitive`() {
        assertEquals(SyncEntityType.INTAKE, SyncEntityType.fromPath("INTAKE"))
        assertEquals(SyncEntityType.DRINK, SyncEntityType.fromPath("Drink"))
        assertEquals(SyncEntityType.SETTINGS, SyncEntityType.fromPath("Settings"))
    }

    @Test
    fun `fromPath should return null for unknown value`() {
        assertNull(SyncEntityType.fromPath("unknown"))
    }

    @Test
    fun `fromPath should return null for blank string`() {
        assertNull(SyncEntityType.fromPath(""))
    }

    @Test
    fun `fromPath should return null for null-like string`() {
        assertNull(SyncEntityType.fromPath("null"))
    }

    @Test
    fun `allPathSegments should contain all segments`() {
        assertEquals(3, SyncEntityType.allPathSegments.size)
        assertTrue(SyncEntityType.allPathSegments.contains("intake"))
        assertTrue(SyncEntityType.allPathSegments.contains("drink"))
        assertTrue(SyncEntityType.allPathSegments.contains("settings"))
    }

    @Test
    fun `fromPath should not match substrings`() {
        assertNull(SyncEntityType.fromPath("intakes"))
        assertNull(SyncEntityType.fromPath("drinks"))
    }

    @Test
    fun `all entries should have non-blank path segments`() {
        SyncEntityType.entries.forEach { entity ->
            assertTrue("Entity ${entity.name} has blank pathSegment",
                entity.pathSegment.isNotBlank())
        }
    }

    @Test
    fun `there should be exactly 3 entity types`() {
        assertEquals(3, SyncEntityType.entries.size)
    }

    @Test
    fun `entityPath builder should match fromPath roundtrip`() {
        SyncEntityType.entries.forEach { entity ->
            val path = SyncPaths.entityPath(entity)
            assertTrue("Path '$path' should contain segment '${entity.pathSegment}'",
                path.contains(entity.pathSegment))
        }
    }
}
