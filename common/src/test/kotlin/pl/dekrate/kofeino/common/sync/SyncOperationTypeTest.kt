package pl.dekrate.kofeino.common.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncOperationTypeTest {

    @Test
    fun `INSERT should have wireValue insert`() {
        assertEquals("insert", SyncOperationType.INSERT.wireValue)
    }

    @Test
    fun `UPDATE should have wireValue update`() {
        assertEquals("update", SyncOperationType.UPDATE.wireValue)
    }

    @Test
    fun `DELETE should have wireValue delete`() {
        assertEquals("delete", SyncOperationType.DELETE.wireValue)
    }

    @Test
    fun `fromWire should resolve insert correctly`() {
        assertEquals(SyncOperationType.INSERT, SyncOperationType.fromWire("insert"))
    }

    @Test
    fun `fromWire should resolve update correctly`() {
        assertEquals(SyncOperationType.UPDATE, SyncOperationType.fromWire("update"))
    }

    @Test
    fun `fromWire should resolve delete correctly`() {
        assertEquals(SyncOperationType.DELETE, SyncOperationType.fromWire("delete"))
    }

    @Test
    fun `fromWire should be case-insensitive`() {
        assertEquals(SyncOperationType.INSERT, SyncOperationType.fromWire("INSERT"))
        assertEquals(SyncOperationType.UPDATE, SyncOperationType.fromWire("Update"))
        assertEquals(SyncOperationType.DELETE, SyncOperationType.fromWire("Delete"))
    }

    @Test
    fun `fromWire should return null for unknown value`() {
        assertNull(SyncOperationType.fromWire("unknown"))
    }

    @Test
    fun `fromWire should return null for blank string`() {
        assertNull(SyncOperationType.fromWire(""))
    }

    @Test
    fun `fromWire should return null for whitespace`() {
        assertNull(SyncOperationType.fromWire("   "))
    }

    @Test
    fun `allWireValues should contain all values`() {
        assertEquals(3, SyncOperationType.allWireValues.size)
        assertTrue(SyncOperationType.allWireValues.contains("insert"))
        assertTrue(SyncOperationType.allWireValues.contains("update"))
        assertTrue(SyncOperationType.allWireValues.contains("delete"))
    }

    @Test
    fun `all entries should have non-blank wire values`() {
        SyncOperationType.entries.forEach { op ->
            assertTrue("Operation ${op.name} has blank wireValue",
                op.wireValue.isNotBlank())
        }
    }

    @Test
    fun `there should be exactly 3 operation types`() {
        assertEquals(3, SyncOperationType.entries.size)
    }

    @Test
    fun `wire values should be lowercase`() {
        SyncOperationType.entries.forEach { op ->
            assertEquals(op.wireValue, op.wireValue.lowercase())
        }
    }

    @Test
    fun `fromWire roundtrip should work for all entries`() {
        SyncOperationType.entries.forEach { op ->
            val resolved = SyncOperationType.fromWire(op.wireValue)
            assertEquals(op, resolved)
        }
    }
}
