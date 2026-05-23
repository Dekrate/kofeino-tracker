package pl.dekrate.kofeino.common.domain.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dekrate.kofeino.common.domain.model.OfficialDrink

class OfficialDrinkRepositoryContractTest {

    // ------------------------------------------------------------------
    // Seed data
    // ------------------------------------------------------------------

    private val energyDrink = OfficialDrink(
        barcode = "5449000000996",
        name = "Monster Energy Original",
        brand = "Monster",
        caffeineMgPer100ml = 32.0,
        energyKcalPer100ml = 47.0,
        quantity = "500ml",
    )

    private val coffeeDrink = OfficialDrink(
        barcode = "8711001234567",
        name = "Caffè Nero Espresso",
        brand = "Nestlé",
        caffeineMgPer100ml = 60.0,
        energyKcalPer100ml = 2.0,
        quantity = "30ml",
    )

    private val teaDrink = OfficialDrink(
        barcode = "4901234567890",
        name = "Green Tea Sencha",
        brand = "Lipton",
        caffeineMgPer100ml = 15.0,
        energyKcalPer100ml = 1.0,
    )

    private val allDrinks: List<OfficialDrink> = listOf(energyDrink, coffeeDrink, teaDrink)

    // ------------------------------------------------------------------
    // getOfficialDrinks
    // ------------------------------------------------------------------

    @Test
    fun `getOfficialDrinks returns all seed drinks`() = runTest {
        val repository = InMemoryOfficialDrinkRepository(drinks = allDrinks)
        val result = repository.getOfficialDrinks()

        assertTrue(result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(3, drinks.size)
        assertTrue(drinks.contains(energyDrink))
        assertTrue(drinks.contains(coffeeDrink))
        assertTrue(drinks.contains(teaDrink))
    }

    @Test
    fun `getOfficialDrinks returns empty list for empty repository`() = runTest {
        val repository = InMemoryOfficialDrinkRepository()
        val result = repository.getOfficialDrinks()

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().size)
    }

    @Test
    fun `getOfficialDrinks returns a defensive copy`() = runTest {
        val repository = InMemoryOfficialDrinkRepository(drinks = allDrinks)
        val firstCall = repository.getOfficialDrinks().getOrThrow()
        val secondCall = repository.getOfficialDrinks().getOrThrow()

        // Both calls return the same content …
        assertEquals(firstCall, secondCall)
        // … but separate list instances, so external mutation cannot
        // corrupt the repository's internal state.
        assertTrue(firstCall !== secondCall)
    }

    @Test
    fun `getOfficialDrinks succeeds with Result isSuccess`() = runTest {
        val repository = InMemoryOfficialDrinkRepository(drinks = allDrinks)
        val result = repository.getOfficialDrinks()

        assertTrue(result.isSuccess)
    }

    // ------------------------------------------------------------------
    // searchOfficialDrinks
    // ------------------------------------------------------------------

    @Test
    fun `searchOfficialDrinks with full name match returns matching drinks`() = runTest {
        val repository = InMemoryOfficialDrinkRepository(drinks = allDrinks)
        val result = repository.searchOfficialDrinks("Monster Energy Original")

        assertTrue(result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(1, drinks.size)
        assertEquals(energyDrink, drinks[0])
    }

    @Test
    fun `searchOfficialDrinks with partial name match returns matching drinks`() = runTest {
        val repository = InMemoryOfficialDrinkRepository(drinks = allDrinks)
        val result = repository.searchOfficialDrinks("Monster")

        assertTrue(result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(1, drinks.size)
        assertEquals(energyDrink, drinks[0])
    }

    @Test
    fun `searchOfficialDrinks with case-insensitive query works`() = runTest {
        val repository = InMemoryOfficialDrinkRepository(drinks = allDrinks)
        val result = repository.searchOfficialDrinks("monster")

        assertTrue(result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(1, drinks.size)
        assertEquals(energyDrink, drinks[0])
    }

    @Test
    fun `searchOfficialDrinks with no match returns empty list`() = runTest {
        val repository = InMemoryOfficialDrinkRepository(drinks = allDrinks)
        val result = repository.searchOfficialDrinks("NonExistentDrink")

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().size)
    }

    @Test
    fun `searchOfficialDrinks with blank query returns all drinks`() = runTest {
        val repository = InMemoryOfficialDrinkRepository(drinks = allDrinks)
        val result = repository.searchOfficialDrinks("   ")

        assertTrue(result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(3, drinks.size)
    }

    @Test
    fun `searchOfficialDrinks matches by brand name`() = runTest {
        val repository = InMemoryOfficialDrinkRepository(drinks = allDrinks)
        val result = repository.searchOfficialDrinks("Nestlé")

        assertTrue(result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(1, drinks.size)
        assertEquals(coffeeDrink, drinks[0])
    }

    @Test
    fun `searchOfficialDrinks with query matching multiple drinks`() = runTest {
        val repository = InMemoryOfficialDrinkRepository(drinks = allDrinks)
        // The letter "e" appears in all three drink names:
        // "Monster Energy Original", "Caffè Nero Espresso", and "Green Tea Sencha"
        val result = repository.searchOfficialDrinks("e")

        assertTrue(result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(3, drinks.size)
    }

    // ------------------------------------------------------------------
    // Cache operations
    // ------------------------------------------------------------------

    @Test
    fun `hasFreshCache returns true initially`() = runTest {
        val repository = InMemoryOfficialDrinkRepository(drinks = allDrinks)

        assertTrue(repository.hasFreshCache())
    }

    @Test
    fun `clearCache sets cache to not fresh`() = runTest {
        val repository = InMemoryOfficialDrinkRepository(drinks = allDrinks)

        assertTrue(repository.hasFreshCache())
        repository.clearCache()
        assertFalse(repository.hasFreshCache())
    }
}
