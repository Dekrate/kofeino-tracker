package pl.dekrate.kofeino.common.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedModelsTest {

    // CaffeineIntake tests
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
        assertEquals("Coffee", modified.drinkName) // unchanged
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

    // DrinkEntity tests
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
        assertTrue(decaf.isDefault) // unchanged
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

    // OfficialDrink tests
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
        assertEquals("Brand", changed.brand) // unchanged
    }

    // Cross-model tests
    @Test
    fun `shared models should be serializable via toString`() {
        val intake = CaffeineIntake(
            drinkName = "Tea",
            caffeineMg = 30,
            volumeMl = 200,
            timestamp = System.currentTimeMillis(),
        )
        val str = intake.toString()
        assertTrue(str.contains("Tea"))
        assertTrue(str.contains("caffeineMg=30"))
    }

    @Test
    fun `data class equals should work correctly`() {
        val drink1 = DrinkEntity(
            name = "Coffee",
            caffeineMg = 100,
            volumeMl = 250,
        )
        val drink2 = DrinkEntity(
            name = "Coffee",
            caffeineMg = 100,
            volumeMl = 250,
        )
        val drink3 = drink1.copy(caffeineMg = 200)
        assertEquals(drink1, drink2)
        assertTrue(drink1 != drink3)
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
}
