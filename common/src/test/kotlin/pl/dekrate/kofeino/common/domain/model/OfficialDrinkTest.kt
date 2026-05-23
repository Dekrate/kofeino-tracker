package pl.dekrate.kofeino.common.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialDrinkTest {

    @Test
    fun `OfficialDrink should have sensible defaults`() {
        val drink = OfficialDrink(
            barcode = "5901234567890",
            name = "Test Drink",
            brand = "Test Brand",
            caffeineMgPer100ml = 32.0,
            energyKcalPer100ml = 0.5,
            quantity = "250ml",
        )
        assertEquals("5901234567890", drink.barcode)
        assertEquals("Open Food Facts", drink.source)
        assertEquals(32.0, drink.caffeineMgPer100ml, 0.0)
        assertEquals(0.5, drink.energyKcalPer100ml!!, 0.0)
    }

    @Test
    fun `OfficialDrink should support null fields`() {
        val drink = OfficialDrink(
            barcode = "123456",
            name = "Minimal",
            caffeineMgPer100ml = 10.0,
        )
        assertNull(drink.brand)
        assertNull(drink.energyKcalPer100ml)
        assertNull(drink.quantity)
        assertEquals("Open Food Facts", drink.source)
    }

    @Test
    fun `OfficialDrink should support copy`() {
        val drink = OfficialDrink(
            barcode = "ABC",
            name = "Original",
            brand = "Brand",
            caffeineMgPer100ml = 15.0,
            energyKcalPer100ml = 1.0,
            quantity = "330ml",
        )
        val changed = drink.copy(
            barcode = "DEF",
            name = "Changed",
            caffeineMgPer100ml = 20.0,
        )
        assertEquals("DEF", changed.barcode)
        assertEquals("Changed", changed.name)
        assertEquals(20.0, changed.caffeineMgPer100ml, 0.0)
        assertEquals("Brand", changed.brand)
    }

    @Test
    fun `OfficialDrink should accept zero caffeine per 100ml`() {
        val drink = OfficialDrink(
            barcode = "000",
            name = "Zero",
            caffeineMgPer100ml = 0.0,
        )
        assertEquals(0.0, drink.caffeineMgPer100ml, 0.0)
    }

    @Test
    fun `OfficialDrink should accept very large caffeine value`() {
        val drink = OfficialDrink(
            barcode = "999",
            name = "Super Strong",
            caffeineMgPer100ml = Double.MAX_VALUE,
        )
        assertEquals(Double.MAX_VALUE, drink.caffeineMgPer100ml, 0.0)
    }

    @Test
    fun `OfficialDrink should accept empty barcode`() {
        val drink = OfficialDrink(
            barcode = "",
            name = "No Barcode",
            caffeineMgPer100ml = 10.0,
        )
        assertEquals("", drink.barcode)
    }

    @Test
    fun `OfficialDrink should accept empty name`() {
        val drink = OfficialDrink(
            barcode = "123",
            name = "",
            caffeineMgPer100ml = 10.0,
        )
        assertEquals("", drink.name)
    }

    @Test
    fun `OfficialDrink should accept custom source`() {
        val drink = OfficialDrink(
            barcode = "123",
            name = "Test",
            caffeineMgPer100ml = 10.0,
            source = "Custom Database",
        )
        assertEquals("Custom Database", drink.source)
    }

    @Test
    fun `OfficialDrink hashCode should be consistent across multiple calls`() {
        val drink = OfficialDrink(
            barcode = "ABC",
            name = "Test",
            caffeineMgPer100ml = 15.0,
        )
        val hash1 = drink.hashCode()
        val hash2 = drink.hashCode()
        assertEquals(hash1, hash2)
    }

    @Test
    fun `OfficialDrink equals should be reflexive`() {
        val drink = OfficialDrink(
            barcode = "XYZ",
            name = "Test",
            caffeineMgPer100ml = 20.0,
        )
        assertEquals(drink, drink)
    }

    @Test
    fun `OfficialDrink equals should detect different barcode`() {
        val a = OfficialDrink(
            barcode = "111",
            name = "Same",
            caffeineMgPer100ml = 10.0,
        )
        val b = a.copy(barcode = "222")
        assertNotEquals(a, b)
    }

    @Test
    fun `OfficialDrink equals should detect different caffeineMgPer100ml`() {
        val a = OfficialDrink(
            barcode = "111",
            name = "Same",
            caffeineMgPer100ml = 10.0,
        )
        val b = a.copy(caffeineMgPer100ml = 20.0)
        assertNotEquals(a, b)
    }

    @Test
    fun `OfficialDrink toString should include barcode and name`() {
        val drink = OfficialDrink(
            barcode = "5901234567890",
            name = "Test Drink",
            caffeineMgPer100ml = 32.0,
        )
        val str = drink.toString()
        assertTrue(str.contains("5901234567890"))
        assertTrue(str.contains("Test Drink"))
        assertTrue(str.contains("OfficialDrink"))
    }

    @Test
    fun `OfficialDrink component functions should return correct values`() {
        val drink = OfficialDrink(
            barcode = "EAN123",
            name = "Cola",
            brand = "BrandX",
            caffeineMgPer100ml = 10.0,
            energyKcalPer100ml = 42.0,
            quantity = "330ml",
            source = "OFF",
        )
        assertEquals("EAN123", drink.component1())
        assertEquals("Cola", drink.component2())
        assertEquals("BrandX", drink.component3())
        assertEquals(10.0, drink.component4(), 0.0)
        assertEquals(42.0, drink.component5())
        assertEquals("330ml", drink.component6())
        assertEquals("OFF", drink.component7())
    }

    @Test
    fun `OfficialDrink copy without arguments should preserve all fields`() {
        val drink = OfficialDrink(
            barcode = "123",
            name = "Test",
            brand = "Brand",
            caffeineMgPer100ml = 5.0,
            energyKcalPer100ml = 1.0,
            quantity = "500ml",
            source = "Source",
        )
        assertEquals(drink, drink.copy())
    }

    @Test
    fun `OfficialDrink with null fields should be equal`() {
        val drink1 = OfficialDrink(
            barcode = "123456",
            name = "Minimal",
            caffeineMgPer100ml = 10.0,
        )
        val drink2 = OfficialDrink(
            barcode = "123456",
            name = "Minimal",
            caffeineMgPer100ml = 10.0,
        )
        assertEquals(drink1, drink2)
        assertEquals(drink1.hashCode(), drink2.hashCode())
    }

    @Test
    fun `OfficialDrink with null energyKcalPer100ml should not equal non-null`() {
        val a = OfficialDrink(
            barcode = "1",
            name = "A",
            caffeineMgPer100ml = 1.0,
            energyKcalPer100ml = null,
        )
        val b = OfficialDrink(
            barcode = "1",
            name = "A",
            caffeineMgPer100ml = 1.0,
            energyKcalPer100ml = 2.5,
        )
        assertNotEquals(a, b)
    }
}
