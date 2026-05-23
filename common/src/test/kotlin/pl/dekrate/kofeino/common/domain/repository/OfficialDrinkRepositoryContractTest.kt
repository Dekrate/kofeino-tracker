package pl.dekrate.kofeino.common.domain.repository

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.dekrate.kofeino.common.domain.model.OfficialDrink

/**
 * Contract tests for [OfficialDrinkRepository].
 *
 * These tests verify the expected behavior contract of the interface using
 * an in-memory fake implementation. Any real implementation should pass
 * these same tests to guarantee contract conformance.
 */
class OfficialDrinkRepositoryContractTest {

    private lateinit var repository: InMemoryOfficialDrinkRepository

    @Before
    fun setUp() {
        repository = InMemoryOfficialDrinkRepository()
    }

    @After
    fun tearDown() {
        repository.clear()
    }

    // ── Basic operations ────────────────────────────────────────────────────

    @Test
    fun `getOfficialDrinks returns empty list for empty repository`() = runTest {
        val result = repository.getOfficialDrinks()
        assertTrue("Result should be success", result.isSuccess)
        val drinks = result.getOrThrow()
        assertTrue("Empty repository should return empty list", drinks.isEmpty())
    }

    @Test
    fun `getOfficialDrinks succeeds with Result isSuccess`() = runTest {
        repository.seedDrinks(listOf(anOfficialDrink()))
        val result = repository.getOfficialDrinks()
        assertTrue("Result should be isSuccess", result.isSuccess)
    }

    @Test
    fun `getOfficialDrinks returns all seed drinks`() = runTest {
        repository.seedDrinks(
            listOf(
                anOfficialDrink(barcode = "1", name = "Drink A"),
                anOfficialDrink(barcode = "2", name = "Drink B"),
                anOfficialDrink(barcode = "3", name = "Drink C"),
            )
        )
        val result = repository.getOfficialDrinks()
        val drinks = result.getOrThrow()
        assertEquals("Should return 3 drinks", 3, drinks.size)
    }

    @Test
    fun `getOfficialDrinks returns a defensive copy`() = runTest {
        repository.seedDrinks(listOf(anOfficialDrink(barcode = "1", name = "Original")))
        val drinks = repository.getOfficialDrinks().getOrThrow()
        // Modifying the returned list should not affect the repository
        val mutableList = drinks.toMutableList()
        mutableList.add(anOfficialDrink(barcode = "999", name = "Injected"))

        val drinksAgain = repository.getOfficialDrinks().getOrThrow()
        assertEquals("Repository size must not change", 1, drinksAgain.size)
    }

    // ── Search operations ───────────────────────────────────────────────────

    @Test
    fun `searchOfficialDrinks with blank query returns all drinks`() = runTest {
        repository.seedDrinks(
            listOf(
                anOfficialDrink(barcode = "1", name = "Cola"),
                anOfficialDrink(barcode = "2", name = "Tea"),
            )
        )
        val result = repository.searchOfficialDrinks("")
        val drinks = result.getOrThrow()
        assertEquals("Blank query should return all drinks", 2, drinks.size)
    }

    @Test
    fun `searchOfficialDrinks with full name match returns matching drinks`() = runTest {
        repository.seedDrinks(
            listOf(
                anOfficialDrink(barcode = "1", name = "Coca-Cola"),
                anOfficialDrink(barcode = "2", name = "Pepsi"),
            )
        )
        val result = repository.searchOfficialDrinks("Coca-Cola")
        val drinks = result.getOrThrow()
        assertEquals("Should find exactly 1 drink", 1, drinks.size)
        assertEquals("Coca-Cola", drinks[0].name)
    }

    @Test
    fun `searchOfficialDrinks with partial name match returns matching drinks`() = runTest {
        repository.seedDrinks(
            listOf(
                anOfficialDrink(barcode = "1", name = "Coca-Cola Zero"),
                anOfficialDrink(barcode = "2", name = "Coca-Cola Classic"),
                anOfficialDrink(barcode = "3", name = "Sprite"),
            )
        )
        val result = repository.searchOfficialDrinks("Coca-Cola")
        val drinks = result.getOrThrow()
        assertEquals("Should find 2 drinks matching 'Coca-Cola'", 2, drinks.size)
    }

    @Test
    fun `searchOfficialDrinks matches by brand name`() = runTest {
        repository.seedDrinks(
            listOf(
                anOfficialDrink(barcode = "1", name = "Cola Zero", brand = "Coca-Cola"),
                anOfficialDrink(barcode = "2", name = "Fruit Tea", brand = "Lipton"),
                anOfficialDrink(barcode = "3", name = "Lemonade", brand = "Minute Maid"),
            )
        )
        val result = repository.searchOfficialDrinks("Lipton")
        val drinks = result.getOrThrow()
        assertEquals("Should find 1 drink from Lipton", 1, drinks.size)
        assertEquals("Fruit Tea", drinks[0].name)
    }

    @Test
    fun `searchOfficialDrinks with case-insensitive query works`() = runTest {
        repository.seedDrinks(
            listOf(
                anOfficialDrink(barcode = "1", name = "Energy Drink"),
                anOfficialDrink(barcode = "2", name = "Green Tea"),
            )
        )
        val result = repository.searchOfficialDrinks("energy")
        val drinks = result.getOrThrow()
        assertEquals("Case-insensitive search should find 1 drink", 1, drinks.size)
        assertEquals("Energy Drink", drinks[0].name)
    }

    @Test
    fun `searchOfficialDrinks with query matching multiple drinks`() = runTest {
        repository.seedDrinks(
            listOf(
                anOfficialDrink(barcode = "1", name = "Black Coffee"),
                anOfficialDrink(barcode = "2", name = "Coffee Latte"),
                anOfficialDrink(barcode = "3", name = "Iced Coffee"),
                anOfficialDrink(barcode = "4", name = "Orange Juice"),
            )
        )
        val result = repository.searchOfficialDrinks("coffee")
        val drinks = result.getOrThrow()
        assertEquals("Should find 3 coffee drinks", 3, drinks.size)
    }

    @Test
    fun `searchOfficialDrinks with no match returns empty list`() = runTest {
        repository.seedDrinks(
            listOf(
                anOfficialDrink(barcode = "1", name = "Cola"),
                anOfficialDrink(barcode = "2", name = "Tea"),
            )
        )
        val result = repository.searchOfficialDrinks("NonExistentXYZ")
        val drinks = result.getOrThrow()
        assertTrue("No match should return empty list", drinks.isEmpty())
    }

    @Test
    fun `searchOfficialDrinks with null brand should not crash`() = runTest {
        repository.seedDrinks(
            listOf(
                anOfficialDrink(barcode = "1", name = "Cola", brand = "Coca-Cola"),
                anOfficialDrink(barcode = "2", name = "Generic Cola", brand = null),
            )
        )
        // Searching by a brand name that doesn't exist should still work
        val result = repository.searchOfficialDrinks("NonExistentBrand")
        val drinks = result.getOrThrow()
        assertTrue("No match should return empty list", drinks.isEmpty())

        // Searching for the null-brand drink by name should work
        val byName = repository.searchOfficialDrinks("Generic Cola")
        assertEquals(1, byName.getOrThrow().size)

        // Searching for the brand drink should return exactly that drink
        val byBrand = repository.searchOfficialDrinks("Coca-Cola")
        assertEquals(1, byBrand.getOrThrow().size)
        assertEquals("Cola", byBrand.getOrThrow()[0].name)
    }

    // ── Cache operations ────────────────────────────────────────────────────

    @Test
    fun `hasFreshCache returns true initially`() = runTest {
        // Freshly created repository has no cache, but by default it's fresh
        assertTrue("Fresh repository should report fresh cache", repository.hasFreshCache())
    }

    @Test
    fun `clearCache sets cache to not fresh`() = runTest {
        repository.clearCache()
        assertFalse("After clearCache, cache should not be fresh", repository.hasFreshCache())
    }

    // ── Helper factory methods ──────────────────────────────────────────────

    private fun anOfficialDrink(
        barcode: String = "5901234567890",
        name: String = "Test Drink",
        brand: String? = "Test Brand",
        caffeineMgPer100ml: Double = 32.0,
        energyKcalPer100ml: Double? = 0.5,
        quantity: String? = "250ml",
        source: String = "Open Food Facts",
    ): OfficialDrink = OfficialDrink(
        barcode = barcode,
        name = name,
        brand = brand,
        caffeineMgPer100ml = caffeineMgPer100ml,
        energyKcalPer100ml = energyKcalPer100ml,
        quantity = quantity,
        source = source,
    )
}

/**
 * In-memory fake implementation of [OfficialDrinkRepository] for contract testing.
 *
 * Stores a list of [OfficialDrink] and supports seeding, searching,
 * and cache state management.
 */
class InMemoryOfficialDrinkRepository : OfficialDrinkRepository {

    private val drinks = mutableListOf<OfficialDrink>()
    private var cacheFresh = true

    fun clear() {
        drinks.clear()
        cacheFresh = true
    }

    fun seedDrinks(seed: List<OfficialDrink>) {
        drinks.clear()
        drinks.addAll(seed)
        cacheFresh = true
    }

    override suspend fun getOfficialDrinks(): Result<List<OfficialDrink>> {
        return Result.success(drinks.toList())
    }

    override suspend fun searchOfficialDrinks(query: String): Result<List<OfficialDrink>> {
        if (query.isBlank()) {
            return Result.success(drinks.toList())
        }
        val lowerQuery = query.lowercase()
        return Result.success(
            drinks.filter { drink ->
                drink.name.lowercase().contains(lowerQuery) ||
                    drink.brand?.lowercase()?.contains(lowerQuery) == true
            }
        )
    }

    override suspend fun hasFreshCache(): Boolean {
        return cacheFresh
    }

    override suspend fun clearCache() {
        cacheFresh = false
    }
}
