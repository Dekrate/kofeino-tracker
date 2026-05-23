package pl.dekrate.kofeino.common.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaffeineIntakeTest {

    @Test
    fun `CaffeineIntake should have sensible defaults`() {
        val intake = CaffeineIntake(
            drinkName = "Coffee",
            caffeineMg = 100,
            volumeMl = 250,
            timestamp = 12345L,
        )
        assertEquals(0L, intake.id)
        assertEquals("Coffee", intake.drinkName)
        assertEquals(100, intake.caffeineMg)
        assertEquals(250, intake.volumeMl)
        assertEquals(12345L, intake.timestamp)
        assertEquals(0L, intake.lastModifiedTimestamp)
        assertEquals("", intake.sourceDeviceId)
        assertNull(intake.drinkId)
    }

    @Test
    fun `CaffeineIntake should support copy with modifications`() {
        val intake = CaffeineIntake(
            drinkName = "Coffee",
            caffeineMg = 100,
            volumeMl = 250,
            timestamp = 12345L,
        )
        val modified = intake.copy(caffeineMg = 150, volumeMl = 300)
        assertEquals(150, modified.caffeineMg)
        assertEquals(300, modified.volumeMl)
        assertEquals("Coffee", modified.drinkName)
    }

    @Test
    fun `CaffeineIntake should respect all constructor parameters`() {
        val intake = CaffeineIntake(
            drinkId = 1L,
            drinkName = "Espresso",
            caffeineMg = 63,
            volumeMl = 30,
            timestamp = 1000L,
            lastModifiedTimestamp = 2000L,
            sourceDeviceId = "watch-1",
        )
        assertEquals(1L, intake.drinkId)
        assertEquals("watch-1", intake.sourceDeviceId)
        assertEquals(2000L, intake.lastModifiedTimestamp)
    }

    @Test
    fun `CaffeineIntake should accept zero caffeine`() {
        val intake = CaffeineIntake(
            drinkName = "Decaf",
            caffeineMg = 0,
            volumeMl = 200,
            timestamp = 1000L,
        )
        assertEquals(0, intake.caffeineMg)
    }

    @Test
    fun `CaffeineIntake should accept large values`() {
        val intake = CaffeineIntake(
            drinkName = "Mega Coffee",
            caffeineMg = Int.MAX_VALUE,
            volumeMl = Int.MAX_VALUE,
            timestamp = Long.MAX_VALUE,
            id = Long.MAX_VALUE,
            drinkId = Long.MAX_VALUE,
            lastModifiedTimestamp = Long.MAX_VALUE,
        )
        assertEquals(Int.MAX_VALUE, intake.caffeineMg)
        assertEquals(Int.MAX_VALUE, intake.volumeMl)
        assertEquals(Long.MAX_VALUE, intake.timestamp)
        assertEquals(Long.MAX_VALUE, intake.id)
        assertEquals(Long.MAX_VALUE, intake.drinkId)
        assertEquals(Long.MAX_VALUE, intake.lastModifiedTimestamp)
    }

    @Test
    fun `CaffeineIntake should accept negative values for numeric fields`() {
        val intake = CaffeineIntake(
            drinkName = "Test",
            caffeineMg = -1,
            volumeMl = -1,
            timestamp = -1L,
        )
        assertEquals(-1, intake.caffeineMg)
        assertEquals(-1, intake.volumeMl)
        assertEquals(-1L, intake.timestamp)
    }

    @Test
    fun `CaffeineIntake should accept empty drink name`() {
        val intake = CaffeineIntake(
            drinkName = "",
            caffeineMg = 50,
            volumeMl = 100,
            timestamp = 0L,
        )
        assertEquals("", intake.drinkName)
    }

    @Test
    fun `CaffeineIntake hashCode should be consistent across multiple calls`() {
        val intake = CaffeineIntake(
            drinkName = "Tea",
            caffeineMg = 30,
            volumeMl = 200,
            timestamp = 5000L,
        )
        val hash1 = intake.hashCode()
        val hash2 = intake.hashCode()
        val hash3 = intake.hashCode()
        assertEquals(hash1, hash2)
        assertEquals(hash2, hash3)
    }

    @Test
    fun `CaffeineIntake equals should be reflexive`() {
        val intake = CaffeineIntake(
            drinkName = "Coffee",
            caffeineMg = 100,
            volumeMl = 250,
            timestamp = 1000L,
        )
        assertEquals(intake, intake)
    }

    @Test
    fun `CaffeineIntake equals should be symmetric`() {
        val a = CaffeineIntake(
            drinkName = "Coffee",
            caffeineMg = 100,
            volumeMl = 250,
            timestamp = 1000L,
        )
        val b = CaffeineIntake(
            drinkName = "Coffee",
            caffeineMg = 100,
            volumeMl = 250,
            timestamp = 1000L,
        )
        assertEquals(a, b)
        assertEquals(b, a)
    }

    @Test
    fun `CaffeineIntake equals should detect different drinkName`() {
        val a = CaffeineIntake(
            drinkName = "Coffee",
            caffeineMg = 100,
            volumeMl = 250,
            timestamp = 1000L,
        )
        val b = a.copy(drinkName = "Tea")
        assertNotEquals(a, b)
    }

    @Test
    fun `CaffeineIntake toString should include class name and key fields`() {
        val intake = CaffeineIntake(
            drinkName = "Tea",
            caffeineMg = 30,
            volumeMl = 200,
            timestamp = System.currentTimeMillis(),
        )
        val str = intake.toString()
        assertTrue(str.contains("Tea"))
        assertTrue(str.contains("caffeineMg=30"))
        assertTrue(str.contains("CaffeineIntake"))
    }

    @Test
    fun `CaffeineIntake component functions should return correct values`() {
        val intake = CaffeineIntake(
            id = 5L,
            drinkId = 10L,
            drinkName = "Coffee",
            caffeineMg = 100,
            volumeMl = 250,
            timestamp = 1000L,
            lastModifiedTimestamp = 2000L,
            sourceDeviceId = "phone-1",
        )
        assertEquals(5L, intake.component1())
        assertEquals(10L, intake.component2())
        assertEquals("Coffee", intake.component3())
        assertEquals(100, intake.component4())
        assertEquals(250, intake.component5())
        assertEquals(1000L, intake.component6())
        assertEquals(2000L, intake.component7())
        assertEquals("phone-1", intake.component8())
    }

    @Test
    fun `CaffeineIntake copy without arguments should preserve all fields`() {
        val intake = CaffeineIntake(
            id = 1L,
            drinkId = 2L,
            drinkName = "Latte",
            caffeineMg = 150,
            volumeMl = 300,
            timestamp = 1000L,
            lastModifiedTimestamp = 2000L,
            sourceDeviceId = "watch-1",
        )
        assertEquals(intake, intake.copy())
    }

    @Test
    fun `CaffeineIntake with null drinkId should have no drinkId`() {
        val intake = CaffeineIntake(
            drinkId = null,
            drinkName = "Generic",
            caffeineMg = 50,
            volumeMl = 100,
            timestamp = 0L,
        )
        assertNull(intake.drinkId)
    }

    @Test
    fun `CaffeineIntake with same values should be equal and have same hash`() {
        val a = CaffeineIntake(
            drinkId = 7L,
            drinkName = "Energy Drink",
            caffeineMg = 80,
            volumeMl = 250,
            timestamp = 1111L,
            lastModifiedTimestamp = 2222L,
            sourceDeviceId = "phone-1",
        )
        val b = CaffeineIntake(
            drinkId = 7L,
            drinkName = "Energy Drink",
            caffeineMg = 80,
            volumeMl = 250,
            timestamp = 1111L,
            lastModifiedTimestamp = 2222L,
            sourceDeviceId = "phone-1",
        )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
