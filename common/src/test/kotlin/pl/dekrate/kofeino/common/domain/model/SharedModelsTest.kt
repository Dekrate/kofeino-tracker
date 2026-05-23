package pl.dekrate.kofeino.common.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SharedModelsTest {

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

    @Suppress("DestructuringDeclarationWithTooManyEntries")
    @Test
    fun `all models should support destructuring`() {
        // CaffeineIntake has 8 components:
        // id, drinkId, drinkName, caffeineMg, volumeMl, timestamp, lastModifiedTimestamp, sourceDeviceId
        val (ciId, ciDrinkId, ciName, ciCaffeine, ciVolume, ciTimestamp, ciLastMod, ciSource) =
            CaffeineIntake(
                id = 1L,
                drinkId = 2L,
                drinkName = "Latte",
                caffeineMg = 150,
                volumeMl = 300,
                timestamp = 1000L,
                lastModifiedTimestamp = 2000L,
                sourceDeviceId = "watch-1",
            )
        assertEquals(1L, ciId)
        assertEquals(2L, ciDrinkId)
        assertEquals("Latte", ciName)
        assertEquals(150, ciCaffeine)
        assertEquals(300, ciVolume)
        assertEquals(1000L, ciTimestamp)
        assertEquals(2000L, ciLastMod)
        assertEquals("watch-1", ciSource)

        // DrinkEntity has 7 components:
        // id, name, caffeineMg, volumeMl, isDefault, lastModifiedTimestamp, sourceDeviceId
        val (dId, dName, dCaffeine, dVolume, dIsDefault, dLastMod, dSource) =
            DrinkEntity(
                id = 3L,
                name = "Americano",
                caffeineMg = 150,
                volumeMl = 300,
                isDefault = true,
                lastModifiedTimestamp = 5000L,
                sourceDeviceId = "phone-2",
            )
        assertEquals(3L, dId)
        assertEquals("Americano", dName)
        assertEquals(150, dCaffeine)
        assertEquals(300, dVolume)
        assertEquals(true, dIsDefault)
        assertEquals(5000L, dLastMod)
        assertEquals("phone-2", dSource)

        // OfficialDrink has 7 components:
        // barcode, name, brand, caffeineMgPer100ml, energyKcalPer100ml, quantity, source
        val (barcode, oName, brand, caffeinePer100, energy, qty, src) =
            OfficialDrink(
                barcode = "EAN123",
                name = "Cola",
                brand = "BrandX",
                caffeineMgPer100ml = 10.0,
                energyKcalPer100ml = 42.0,
                quantity = "330ml",
                source = "OFF",
            )
        assertEquals("EAN123", barcode)
        assertEquals("Cola", oName)
        assertEquals("BrandX", brand)
        assertEquals(10.0, caffeinePer100, 0.0)
        assertEquals(42.0, energy!!, 0.0)
        assertEquals("330ml", qty!!)
        assertEquals("OFF", src)
    }
}
