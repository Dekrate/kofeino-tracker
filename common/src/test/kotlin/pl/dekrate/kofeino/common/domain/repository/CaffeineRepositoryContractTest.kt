package pl.dekrate.kofeino.common.domain.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity

/**
 * Contract test for [CaffeineRepository].
 *
 * This test validates the behaviour of any [CaffeineRepository] implementation
 * by exercising every method in the interface.  It is written against
 * [InMemoryCaffeineRepository] so that no database or Android dependency is
 * required, but the same contract applies to all implementations.
 */
class CaffeineRepositoryContractTest {

    // ------------------------------------------------------------------
    // Deterministic timestamp helpers
    // ------------------------------------------------------------------

    private val date1 = LocalDate(2026, 5, 23)
    private val date2 = LocalDate(2026, 5, 24)

    private fun timestampForDate(date: LocalDate, hour: Int = 12): Long {
        return date.atStartOfDayIn(TimeZone.UTC)
            .plus(hour.toLong(), DateTimeUnit.HOUR)
            .toEpochMilliseconds()
    }

    private val noon23 = timestampForDate(date1)
    private val noon24 = timestampForDate(date2)

    // ------------------------------------------------------------------
    // Factory helpers
    // ------------------------------------------------------------------

    private fun createIntake(
        drinkName: String = "Coffee",
        caffeineMg: Int = 100,
        volumeMl: Int = 250,
        timestamp: Long = noon23,
        drinkId: Long? = null,
    ): CaffeineIntake = CaffeineIntake(
        drinkName = drinkName,
        caffeineMg = caffeineMg,
        volumeMl = volumeMl,
        timestamp = timestamp,
        drinkId = drinkId,
    )

    private fun createDrink(
        name: String = "Black Coffee",
        caffeineMg: Int = 95,
        volumeMl: Int = 200,
    ): DrinkEntity = DrinkEntity(
        name = name,
        caffeineMg = caffeineMg,
        volumeMl = volumeMl,
    )

    // ------------------------------------------------------------------
    // Intake operations
    // ------------------------------------------------------------------

    @Test
    fun `addIntake returns positive ID and intake can be retrieved`() = runTest {
        val repo = InMemoryCaffeineRepository()
        val intake = createIntake()

        val id = repo.addIntake(intake)
        assertTrue("generated ID must be positive", id > 0L)

        val retrieved = repo.getIntakeById(id)
        assertNotNull("intake must exist after add", retrieved)
        assertEquals(id, retrieved!!.id)
        assertEquals(intake.drinkName, retrieved.drinkName)
        assertEquals(intake.caffeineMg, retrieved.caffeineMg)
        assertEquals(intake.volumeMl, retrieved.volumeMl)
        assertEquals(intake.timestamp, retrieved.timestamp)
    }

    @Test
    fun `getIntakeById returns null for non-existent ID`() = runTest {
        val repo = InMemoryCaffeineRepository()
        val retrieved = repo.getIntakeById(99999L)
        assertNull("non-existent intake must be null", retrieved)
    }

    @Test
    fun `addIntake auto-increments IDs`() = runTest {
        val repo = InMemoryCaffeineRepository()
        val intake1 = createIntake()
        val intake2 = createIntake(drinkName = "Tea")

        val id1 = repo.addIntake(intake1)
        val id2 = repo.addIntake(intake2)

        assertTrue("first ID must be positive", id1 > 0L)
        assertTrue("second ID must be larger than first", id2 > id1)
    }

    @Test
    fun `getIntakesForDate returns intakes for the correct date`() = runTest {
        val repo = InMemoryCaffeineRepository()

        repo.addIntake(createIntake(drinkName = "Coffee", timestamp = noon23))
        repo.addIntake(createIntake(drinkName = "Tea", timestamp = noon24))
        repo.addIntake(createIntake(drinkName = "Espresso", timestamp = noon23))

        val date1Intakes = repo.getIntakesForDate(date1).first()
        assertEquals("must return 2 intakes for date1", 2, date1Intakes.size)
        assertTrue(date1Intakes.all { it.drinkName != "Tea" })
    }

    @Test
    fun `getIntakesForDate returns empty list for date with no intakes`() = runTest {
        val repo = InMemoryCaffeineRepository()
        repo.addIntake(createIntake(timestamp = noon23))

        val emptyDate = LocalDate(2026, 6, 1)
        val intakes = repo.getIntakesForDate(emptyDate).first()
        assertTrue("must be empty for date with no intakes", intakes.isEmpty())
    }

    @Test
    fun `updateIntake modifies existing intake`() = runTest {
        val repo = InMemoryCaffeineRepository()
        val id = repo.addIntake(createIntake(caffeineMg = 100))

        repo.updateIntake(CaffeineIntake(
            id = id,
            drinkName = "Coffee",
            caffeineMg = 150,
            volumeMl = 250,
            timestamp = noon23,
        ))

        val updated = repo.getIntakeById(id)
        assertNotNull(updated)
        assertEquals(150, updated!!.caffeineMg)
    }

    @Test
    fun `updateIntake with non-existent ID does nothing`() = runTest {
        val repo = InMemoryCaffeineRepository()
        // Must not throw
        repo.updateIntake(CaffeineIntake(
            id = 999L,
            drinkName = "Ghost",
            caffeineMg = 999,
            volumeMl = 999,
            timestamp = noon23,
        ))
        // No intakes should exist
        val all = repo.getIntakesForDate(date1).first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun `deleteIntake removes the intake`() = runTest {
        val repo = InMemoryCaffeineRepository()
        val id = repo.addIntake(createIntake())

        repo.deleteIntake(CaffeineIntake(
            id = id,
            drinkName = "",
            caffeineMg = 0,
            volumeMl = 0,
            timestamp = noon23,
        ))

        val retrieved = repo.getIntakeById(id)
        assertNull("intake must be null after delete", retrieved)
    }

    @Test
    fun `deleteIntake with non-existent ID does nothing`() = runTest {
        val repo = InMemoryCaffeineRepository()
        // Must not throw
        repo.deleteIntake(CaffeineIntake(
            id = 999L,
            drinkName = "",
            caffeineMg = 0,
            volumeMl = 0,
            timestamp = noon23,
        ))
    }

    @Test
    fun `clearAll removes all intakes`() = runTest {
        val repo = InMemoryCaffeineRepository()
        repo.addIntake(createIntake(timestamp = noon23))
        repo.addIntake(createIntake(timestamp = noon24))

        repo.clearAll()

        val date1Intakes = repo.getIntakesForDate(date1).first()
        val date2Intakes = repo.getIntakesForDate(date2).first()
        assertTrue(date1Intakes.isEmpty())
        assertTrue(date2Intakes.isEmpty())
    }

    @Test
    fun `clearAll removes all drinks`() = runTest {
        val repo = InMemoryCaffeineRepository()
        repo.addDrink(createDrink())
        repo.addDrink(createDrink(name = "Green Tea"))

        repo.clearAll()

        val drinks = repo.getAllDrinks().first()
        assertTrue("all drinks must be removed after clearAll", drinks.isEmpty())
    }

    @Test
    fun `getTotalCaffeineForDate returns correct sum`() = runTest {
        val repo = InMemoryCaffeineRepository()
        repo.addIntake(createIntake(drinkName = "Coffee", caffeineMg = 100, timestamp = noon23))
        repo.addIntake(createIntake(drinkName = "Espresso", caffeineMg = 63, timestamp = noon23))
        repo.addIntake(createIntake(drinkName = "Tea", caffeineMg = 30, timestamp = noon23))

        val total = repo.getTotalCaffeineForDate(date1).first()
        assertEquals("total must equal sum of all intakes for the date", 193, total)
    }

    @Test
    fun `getTotalCaffeineForDate returns 0 for date with no intakes`() = runTest {
        val repo = InMemoryCaffeineRepository()
        repo.addIntake(createIntake(timestamp = noon23))

        val total = repo.getTotalCaffeineForDate(date2).first()
        assertEquals("must be 0 for date without intakes", 0, total)
    }

    // ------------------------------------------------------------------
    // Drink operations
    // ------------------------------------------------------------------

    @Test
    fun `addDrink returns positive ID and drink can be retrieved`() = runTest {
        val repo = InMemoryCaffeineRepository()
        val drink = createDrink()

        val id = repo.addDrink(drink)
        assertTrue("generated drink ID must be positive", id > 0L)

        val retrieved = repo.getDrinkById(id)
        assertNotNull("drink must exist after add", retrieved)
        assertEquals(id, retrieved!!.id)
        assertEquals(drink.name, retrieved.name)
        assertEquals(drink.caffeineMg, retrieved.caffeineMg)
        assertEquals(drink.volumeMl, retrieved.volumeMl)
    }

    @Test
    fun `getDrinkById returns null for non-existent ID`() = runTest {
        val repo = InMemoryCaffeineRepository()
        val retrieved = repo.getDrinkById(99999L)
        assertNull("non-existent drink must be null", retrieved)
    }

    @Test
    fun `addDrink auto-increments drink IDs`() = runTest {
        val repo = InMemoryCaffeineRepository()
        val drink1 = createDrink()
        val drink2 = createDrink(name = "Latte")

        val id1 = repo.addDrink(drink1)
        val id2 = repo.addDrink(drink2)

        assertTrue("first drink ID must be positive", id1 > 0L)
        assertTrue("second drink ID must be larger than first", id2 > id1)
    }

    @Test
    fun `getAllDrinks returns all drinks`() = runTest {
        val repo = InMemoryCaffeineRepository()
        repo.addDrink(createDrink(name = "Black Coffee"))
        repo.addDrink(createDrink(name = "Green Tea", caffeineMg = 30, volumeMl = 200))
        repo.addDrink(createDrink(name = "Espresso", caffeineMg = 63, volumeMl = 30))

        val drinks = repo.getAllDrinks().first()
        assertEquals(3, drinks.size)
        val names = drinks.map { it.name }.toSet()
        assertTrue(names.contains("Black Coffee"))
        assertTrue(names.contains("Green Tea"))
        assertTrue(names.contains("Espresso"))
    }

    @Test
    fun `getAllDrinks returns empty list initially`() = runTest {
        val repo = InMemoryCaffeineRepository()
        val drinks = repo.getAllDrinks().first()
        assertTrue("new repo must have no drinks", drinks.isEmpty())
    }

    @Test
    fun `updateDrink modifies existing drink`() = runTest {
        val repo = InMemoryCaffeineRepository()
        val id = repo.addDrink(createDrink(name = "Coffee", caffeineMg = 95))

        repo.updateDrink(DrinkEntity(
            id = id,
            name = "Strong Coffee",
            caffeineMg = 120,
            volumeMl = 200,
        ))

        val updated = repo.getDrinkById(id)
        assertNotNull(updated)
        assertEquals("Strong Coffee", updated!!.name)
        assertEquals(120, updated.caffeineMg)
    }

    @Test
    fun `deleteDrink removes the drink`() = runTest {
        val repo = InMemoryCaffeineRepository()
        val id = repo.addDrink(createDrink())

        repo.deleteDrink(DrinkEntity(
            id = id,
            name = "",
            caffeineMg = 0,
            volumeMl = 0,
        ))

        val retrieved = repo.getDrinkById(id)
        assertNull("drink must be null after delete", retrieved)
    }

    // ------------------------------------------------------------------
    // Edge cases
    // ------------------------------------------------------------------

    @Test
    fun `intake with explicit drinkId is preserved`() = runTest {
        val repo = InMemoryCaffeineRepository()
        val id = repo.addIntake(createIntake(drinkId = 42L))

        val retrieved = repo.getIntakeById(id)
        assertNotNull(retrieved)
        assertEquals(42L, retrieved!!.drinkId)
    }

    @Test
    fun `multiple intakes same date returns correct count`() = runTest {
        val repo = InMemoryCaffeineRepository()
        repo.addIntake(createIntake(drinkName = "Coffee", timestamp = noon23))
        repo.addIntake(createIntake(drinkName = "Espresso", timestamp = noon23))
        repo.addIntake(createIntake(drinkName = "Latte", timestamp = noon23))

        val intakes = repo.getIntakesForDate(date1).first()
        assertEquals(3, intakes.size)
    }

    @Test
    fun `deleteDrink with non-existent ID does nothing`() = runTest {
        val repo = InMemoryCaffeineRepository()
        // Must not throw
        repo.deleteDrink(DrinkEntity(
            id = 999L,
            name = "",
            caffeineMg = 0,
            volumeMl = 0,
        ))
    }

    @Test
    fun `updateDrink with non-existent ID does nothing`() = runTest {
        val repo = InMemoryCaffeineRepository()
        // Must not throw
        repo.updateDrink(DrinkEntity(
            id = 999L,
            name = "Ghost",
            caffeineMg = 0,
            volumeMl = 0,
        ))
        // No drinks should exist
        val all = repo.getAllDrinks().first()
        assertTrue(all.isEmpty())
    }
}
