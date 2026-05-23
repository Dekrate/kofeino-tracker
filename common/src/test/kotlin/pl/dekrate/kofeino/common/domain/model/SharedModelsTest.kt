package pl.dekrate.kofeino.common.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedModelsTest {

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
    fun `three models should never equal each other across types`() {
        val intake = CaffeineIntake(drinkName = "Coffee", caffeineMg = 100, volumeMl = 250, timestamp = 0L)
        val drink = DrinkEntity(name = "Coffee", caffeineMg = 100, volumeMl = 250)
        val official = OfficialDrink(barcode = "Coffee", name = "Coffee", caffeineMgPer100ml = 40.0)
        assertNotEquals(intake, drink)
        assertNotEquals(intake, official)
        assertNotEquals(drink, official)
    }

    @Test
    fun `all models should have non-null toString`() {
        assertNotNull(CaffeineIntake(drinkName = "A", caffeineMg = 1, volumeMl = 1, timestamp = 0L).toString())
        assertNotNull(DrinkEntity(name = "A", caffeineMg = 1, volumeMl = 1).toString())
        assertNotNull(OfficialDrink(barcode = "A", name = "A", caffeineMgPer100ml = 1.0).toString())
    }

    @Test
    fun `all models should support destructuring`() {
        val intake = CaffeineIntake(drinkName = "N", caffeineMg = 1, volumeMl = 1, timestamp = 0L)
        assertEquals("N", intake.component3())

        val drink = DrinkEntity(name = "D", caffeineMg = 2, volumeMl = 3)
        assertEquals("D", drink.component2())

        val official = OfficialDrink(barcode = "B", name = "O", caffeineMgPer100ml = 5.0)
        assertEquals("B", official.component1())
        assertEquals("O", official.component2())
    }
}
