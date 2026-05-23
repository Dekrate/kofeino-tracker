package pl.dekrate.kofeino.common.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrinkEntityTest {

    @Test
    fun `DrinkEntity should have sensible defaults`() {
        val drink = DrinkEntity(
            name = "Black Coffee",
            caffeineMg = 95,
            volumeMl = 200,
        )
        assertEquals(0L, drink.id)
        assertEquals("Black Coffee", drink.name)
        assertEquals(95, drink.caffeineMg)
        assertEquals(200, drink.volumeMl)
        assertEquals(false, drink.isDefault)
        assertEquals(0L, drink.lastModifiedTimestamp)
        assertEquals("", drink.sourceDeviceId)
    }

    @Test
    fun `DrinkEntity should support copy with modifications`() {
        val drink = DrinkEntity(
            name = "Latte",
            caffeineMg = 150,
            volumeMl = 300,
            isDefault = true,
        )
        val decaf = drink.copy(caffeineMg = 5, name = "Decaf Latte")
        assertEquals("Decaf Latte", decaf.name)
        assertEquals(5, decaf.caffeineMg)
        assertTrue(decaf.isDefault)
    }

    @Test
    fun `DrinkEntity should respect all constructor parameters`() {
        val drink = DrinkEntity(
            id = 42L,
            name = "Custom Drink",
            caffeineMg = 200,
            volumeMl = 500,
            isDefault = true,
            lastModifiedTimestamp = 9999L,
            sourceDeviceId = "phone-1",
        )
        assertEquals(42L, drink.id)
        assertTrue(drink.isDefault)
        assertEquals(9999L, drink.lastModifiedTimestamp)
        assertEquals("phone-1", drink.sourceDeviceId)
    }

    @Test
    fun `DrinkEntity should accept zero caffeine`() {
        val drink = DrinkEntity(
            name = "Water",
            caffeineMg = 0,
            volumeMl = 250,
        )
        assertEquals(0, drink.caffeineMg)
    }

    @Test
    fun `DrinkEntity should accept zero volume`() {
        val drink = DrinkEntity(
            name = "Drop",
            caffeineMg = 50,
            volumeMl = 0,
        )
        assertEquals(0, drink.volumeMl)
    }

    @Test
    fun `DrinkEntity should accept empty name`() {
        val drink = DrinkEntity(
            name = "",
            caffeineMg = 50,
            volumeMl = 200,
        )
        assertEquals("", drink.name)
    }

    @Test
    fun `DrinkEntity should accept large values`() {
        val drink = DrinkEntity(
            id = Long.MAX_VALUE,
            name = "Max",
            caffeineMg = Int.MAX_VALUE,
            volumeMl = Int.MAX_VALUE,
            isDefault = true,
            lastModifiedTimestamp = Long.MAX_VALUE,
            sourceDeviceId = "test",
        )
        assertEquals(Long.MAX_VALUE, drink.id)
        assertEquals(Int.MAX_VALUE, drink.caffeineMg)
        assertEquals(Long.MAX_VALUE, drink.lastModifiedTimestamp)
    }

    @Test
    fun `DrinkEntity hashCode should be consistent across multiple calls`() {
        val drink = DrinkEntity(
            name = "Coffee",
            caffeineMg = 100,
            volumeMl = 250,
        )
        val hash1 = drink.hashCode()
        val hash2 = drink.hashCode()
        assertEquals(hash1, hash2)
    }

    @Test
    fun `DrinkEntity equals should be reflexive`() {
        val drink = DrinkEntity(
            name = "Coffee",
            caffeineMg = 100,
            volumeMl = 250,
        )
        assertEquals(drink, drink)
    }

    @Test
    fun `DrinkEntity equals should detect different isDefault`() {
        val a = DrinkEntity(
            name = "Coffee",
            caffeineMg = 100,
            volumeMl = 250,
            isDefault = false,
        )
        val b = a.copy(isDefault = true)
        assertNotEquals(a, b)
    }

    @Test
    fun `DrinkEntity toString should include name and caffeineMg`() {
        val drink = DrinkEntity(
            name = "Espresso",
            caffeineMg = 63,
            volumeMl = 30,
        )
        val str = drink.toString()
        assertTrue(str.contains("Espresso"))
        assertTrue(str.contains("caffeineMg=63"))
        assertTrue(str.contains("DrinkEntity"))
    }

    @Test
    fun `DrinkEntity component functions should return correct values`() {
        val drink = DrinkEntity(
            id = 3L,
            name = "Americano",
            caffeineMg = 150,
            volumeMl = 300,
            isDefault = true,
            lastModifiedTimestamp = 5000L,
            sourceDeviceId = "phone-2",
        )
        assertEquals(3L, drink.component1())
        assertEquals("Americano", drink.component2())
        assertEquals(150, drink.component3())
        assertEquals(300, drink.component4())
        assertEquals(true, drink.component5())
        assertEquals(5000L, drink.component6())
        assertEquals("phone-2", drink.component7())
    }

    @Test
    fun `DrinkEntity copy without arguments should preserve all fields`() {
        val drink = DrinkEntity(
            id = 7L,
            name = "Mocha",
            caffeineMg = 180,
            volumeMl = 350,
            isDefault = false,
            lastModifiedTimestamp = 1000L,
            sourceDeviceId = "test",
        )
        assertEquals(drink, drink.copy())
    }
}
