package pl.dekrate.kofeino.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import pl.dekrate.kofeino.data.local.CaffeineDatabase
import pl.dekrate.kofeino.data.local.OfficialDrinkCacheDao
import pl.dekrate.kofeino.data.local.OfficialDrinkCacheEntity
import pl.dekrate.kofeino.data.remote.CaffeineApiService
import pl.dekrate.kofeino.data.remote.ConnectivityObserver
import pl.dekrate.kofeino.data.remote.dto.OpenFoodFactsNutrimentsDto
import pl.dekrate.kofeino.data.remote.dto.OpenFoodFactsProductDto
import pl.dekrate.kofeino.data.remote.dto.OpenFoodFactsSearchResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.mockk.coVerify
import io.mockk.just
import io.mockk.runs

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class OfficialDrinkRepositoryImplTest {

    private lateinit var database: CaffeineDatabase
    private lateinit var cacheDao: OfficialDrinkCacheDao
    private lateinit var apiService: CaffeineApiService
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var repository: OfficialDrinkRepositoryImpl
    private lateinit var testScope: CoroutineScope

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, CaffeineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cacheDao = database.officialDrinkCacheDao()
        apiService = mockk(relaxed = true)
        connectivityObserver = mockk(relaxed = true)
        every { connectivityObserver.isOnline } returns true
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        repository = OfficialDrinkRepositoryImpl(apiService, cacheDao, connectivityObserver, testScope)
    }

    @After
    fun teardown() {
        testScope.cancel()
        database.close()
    }

    @Test
    fun `when online and API succeeds should return drinks from API`() = runTest {
        val products = listOf(
            OpenFoodFactsProductDto(
                code = "123", productName = "Kawa", brands = "Brand",
                nutriments = OpenFoodFactsNutrimentsDto(caffeine100g = 0.063)
            ),
            OpenFoodFactsProductDto(
                code = "456", productName = "Herbata", brands = "TeaCo",
                nutriments = OpenFoodFactsNutrimentsDto(caffeine100g = 0.028)
            )
        )
        coEvery { apiService.searchBeveragesWithCaffeine(pageSize = 30) } returns
            OpenFoodFactsSearchResponse(count = 2, products = products)

        val result = repository.getOfficialDrinks()

        assertTrue("Expected success", result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(2, drinks.size)
        assertEquals("Kawa", drinks[0].name)
        assertEquals(63.0, drinks[0].caffeineMgPer100ml, 0.01)
        assertEquals(28.0, drinks[1].caffeineMgPer100ml, 0.01)
        assertEquals(2, cacheDao.count())
    }

    @Test
    fun `when offline and cache exists should return cached drinks`() = runTest {
        cacheDao.insertAll(
            listOf(
                OfficialDrinkCacheEntity(
                    barcode = "789", name = "Cola", brand = "Coca-Cola",
                    caffeineMgPer100ml = 10.0, energyKcalPer100ml = 42.0, quantity = "330ml"
                )
            )
        )
        every { connectivityObserver.isOnline } returns false

        val result = repository.getOfficialDrinks()

        assertTrue("Expected success", result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(1, drinks.size)
        assertEquals("Cola", drinks[0].name)
        assertEquals(10.0, drinks[0].caffeineMgPer100ml, 0.01)
    }

    @Test
    fun `when offline and cache empty should return failure`() = runTest {
        every { connectivityObserver.isOnline } returns false

        val result = repository.getOfficialDrinks()

        assertTrue("Expected failure", result.isFailure)
    }

    @Test
    fun `when online and API fails should fallback to cache`() = runTest {
        cacheDao.insertAll(
            listOf(
                OfficialDrinkCacheEntity(
                    barcode = "001", name = "Energy", brand = "Monster",
                    caffeineMgPer100ml = 32.0, energyKcalPer100ml = null, quantity = "250ml"
                )
            )
        )
        coEvery { apiService.searchBeveragesWithCaffeine(pageSize = 30) } throws
            RuntimeException("Network error")

        val result = repository.getOfficialDrinks()

        assertTrue("Expected success from cache fallback", result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(1, drinks.size)
        assertEquals("Energy", drinks[0].name)
    }

    @Test
    fun `hasFreshCache should return true when cache exists`() = runTest {
        cacheDao.insertAll(
            listOf(
                OfficialDrinkCacheEntity(
                    barcode = "x1", name = "Test", brand = null,
                    caffeineMgPer100ml = 10.0, energyKcalPer100ml = null, quantity = null
                )
            )
        )
        assertTrue(repository.hasFreshCache())
    }

    // ===== Search tests =====

    @Test
    fun `searchOfficialDrinks when online should query API v1`() = runTest {
        val products = listOf(
            OpenFoodFactsProductDto(
                code = "111", productName = "Red Bull", brands = "Red Bull",
                nutriments = OpenFoodFactsNutrimentsDto(caffeine100g = 0.032)
            ),
            OpenFoodFactsProductDto(
                code = "222", productName = "Monster Energy", brands = "Monster",
                nutriments = OpenFoodFactsNutrimentsDto(caffeine100g = 0.030)
            )
        )
        coEvery { apiService.searchProducts(query = "red bull", locale = "pl", country = "PL") } returns
            OpenFoodFactsSearchResponse(count = 2, products = products)

        val result = repository.searchOfficialDrinks("red bull")

        assertTrue(result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(2, drinks.size)
        assertEquals("Red Bull", drinks[0].name)
        assertEquals(32.0, drinks[0].caffeineMgPer100ml, 0.01)
    }

    @Test
    fun `searchOfficialDrinks when API fails should search cache`() = runTest {
        cacheDao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("001", "Red Bull Classic", "Red Bull", 32.0, null, "250ml"),
                OfficialDrinkCacheEntity("002", "Coca-Cola Zero", "Coca-Cola", 10.0, 0.5, "330ml"),
                OfficialDrinkCacheEntity("003", "Nescafe Gold", "Nestle", 40.0, 2.0, "200ml")
            )
        )
        // API rzuca błędem
        coEvery { apiService.searchProducts(query = any(), locale = any(), country = any()) } throws
            RuntimeException("API error")

        val result = repository.searchOfficialDrinks("red bull")

        assertTrue(result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(1, drinks.size)
        assertEquals("Red Bull Classic", drinks[0].name)
    }

    @Test
    fun `searchOfficialDrinks when offline and no cache match should return empty list`() = runTest {
        every { connectivityObserver.isOnline } returns false
        cacheDao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("001", "Red Bull", "Red Bull", 32.0, null, "250ml")
            )
        )

        val result = repository.searchOfficialDrinks("coca cola")

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().size)
    }

    @Test
    fun `searchOfficialDrinks should filter products without caffeine data`() = runTest {
        val products = listOf(
            OpenFoodFactsProductDto(
                code = "111", productName = "Red Bull", brands = "Red Bull",
                nutriments = OpenFoodFactsNutrimentsDto(caffeine100g = 0.032)
            ),
            OpenFoodFactsProductDto(
                code = "222", productName = "Water", brands = "Aqua",
                nutriments = OpenFoodFactsNutrimentsDto(caffeine100g = 0.0) // no caffeine
            ),
            OpenFoodFactsProductDto(
                code = "333", productName = "Juice", brands = "Tymbark",
                nutriments = null // no nutriments at all
            )
        )
        coEvery { apiService.searchProducts(query = "drink", locale = any(), country = any()) } returns
            OpenFoodFactsSearchResponse(count = 3, products = products)

        val result = repository.searchOfficialDrinks("drink")

        assertTrue(result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(1, drinks.size) // only Red Bull has caffeine > 0
        assertEquals("Red Bull", drinks[0].name)
    }

    // ===== hasFreshCache / clearCache =====

    @Test
    fun `clearCache should remove all cache entries`() = runTest {
        cacheDao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("a", "A", null, 1.0, null, null),
                OfficialDrinkCacheEntity("b", "B", null, 2.0, null, null)
            )
        )
        val beforeCount = cacheDao.count()
        assertEquals(2, beforeCount)
        repository.clearCache()
        val afterCount = cacheDao.count()
        assertEquals(0, afterCount)
    }

    // ===== Edge case tests =====

    @Test
    fun `when API returns empty products should fallback to cache`() = runTest {
        cacheDao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("001", "Cached", null, 10.0, null, null)
            )
        )
        coEvery { apiService.searchBeveragesWithCaffeine(pageSize = 30) } returns
            OpenFoodFactsSearchResponse(count = 0, products = emptyList())

        val result = repository.getOfficialDrinks()

        assertTrue(result.isSuccess)
        val drinks = result.getOrThrow()
        // Should return cached drinks
        assertEquals(1, drinks.size)
        assertEquals("Cached", drinks[0].name)
    }

    @Test
    fun `when API returns only invalid caffeine products should return empty`() = runTest {
        val products = listOf(
            OpenFoodFactsProductDto(
                code = "001", productName = "Water", brands = "Aqua",
                nutriments = OpenFoodFactsNutrimentsDto(caffeine100g = 0.0)
            ),
            OpenFoodFactsProductDto(
                code = "002", productName = "Juice", brands = "Tymbark",
                nutriments = null
            )
        )
        coEvery { apiService.searchBeveragesWithCaffeine(pageSize = 30) } returns
            OpenFoodFactsSearchResponse(count = 2, products = products)

        val result = repository.getOfficialDrinks()

        assertTrue(result.isSuccess)
        val drinks = result.getOrThrow()
        // All products filtered out, empty result with no cache
        assertEquals(0, drinks.size)
    }

    @Test
    fun `when API fails and cache is empty returns failure`() = runTest {
        coEvery { apiService.searchBeveragesWithCaffeine(pageSize = 30) } throws
            RuntimeException("Network error")

        val result = repository.getOfficialDrinks()

        assertTrue("Expected failure when API fails and no cache", result.isFailure)
    }

    @Test
    fun `searchOfficialDrinks with empty query should return cached drinks`() = runTest {
        cacheDao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("001", "Red Bull", null, 32.0, null, "250ml"),
                OfficialDrinkCacheEntity("002", "Coca-Cola", null, 10.0, null, "330ml")
            )
        )
        // API fails
        coEvery { apiService.searchProducts(query = any(), locale = any(), country = any()) } throws
            RuntimeException("API error")

        val result = repository.searchOfficialDrinks("")

        assertTrue(result.isSuccess)
        val drinks = result.getOrThrow()
        // Empty string matches everything in cache fallback
        assertEquals(2, drinks.size)
    }

    @Test
    fun `searchOfficialDrinks with very long query should not crash`() = runTest {
        val longQuery = "a".repeat(1000)
        coEvery { apiService.searchProducts(query = any(), locale = any(), country = any()) } returns
            OpenFoodFactsSearchResponse(count = 0, products = emptyList())

        val result = repository.searchOfficialDrinks(longQuery)

        assertTrue(result.isSuccess)
        // Should not throw or crash
    }

    @Test
    fun `hasFreshCache with out-of-date cache returns false`() = runTest {
        val oneHourAgo = System.currentTimeMillis() - 61 * 60 * 1000L
        val entity = OfficialDrinkCacheEntity(
            barcode = "001", name = "Stale", brand = null,
            caffeineMgPer100ml = 10.0, energyKcalPer100ml = null, quantity = null,
            fetchedAtMillis = oneHourAgo
        )
        cacheDao.insertAll(listOf(entity))

        // OfficialDrinkRepositoryImpl uses System.currentTimeMillis() - CACHE_TTL_MILLIS,
        // and our cache is 61 minutes old, which is greater than the 60 minute TTL.
        val fresh = repository.hasFreshCache()
        assertEquals(false, fresh)
    }

    // ===== Rate limit resilience tests =======================

    @Test
    fun `when API returns 429 should fallback to cache`() = runTest {
        cacheDao.insertAll(
            listOf(
                OfficialDrinkCacheEntity(
                    barcode = "001", name = "Cached Drink", brand = "Brand",
                    caffeineMgPer100ml = 32.0, energyKcalPer100ml = null, quantity = "250ml"
                )
            )
        )

        // Mock HttpException with status code 429
        val httpException = mockk<retrofit2.HttpException>(relaxed = true)
        every { httpException.code() } returns 429
        coEvery { apiService.searchBeveragesWithCaffeine(pageSize = 30) } throws httpException

        val result = repository.getOfficialDrinks()

        assertTrue("Expected success from cache fallback on 429", result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(1, drinks.size)
        assertEquals("Cached Drink", drinks[0].name)
    }

    @Test
    fun `when API returns 503 should fallback to cache`() = runTest {
        cacheDao.insertAll(
            listOf(
                OfficialDrinkCacheEntity(
                    barcode = "002", name = "Backup Drink", brand = "Backup",
                    caffeineMgPer100ml = 10.0, energyKcalPer100ml = null, quantity = "330ml"
                )
            )
        )

        val httpException = mockk<retrofit2.HttpException>(relaxed = true)
        every { httpException.code() } returns 503
        coEvery { apiService.searchBeveragesWithCaffeine(pageSize = 30) } throws httpException

        val result = repository.getOfficialDrinks()

        assertTrue("Expected success from cache fallback on 503", result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(1, drinks.size)
        assertEquals("Backup Drink", drinks[0].name)
    }

    @Test
    fun `when API 429 and cache is empty should return failure`() = runTest {
        val httpException = mockk<retrofit2.HttpException>(relaxed = true)
        every { httpException.code() } returns 429
        coEvery { apiService.searchBeveragesWithCaffeine(pageSize = 30) } throws httpException

        val result = repository.getOfficialDrinks()

        assertTrue("Expected failure when rate limited and no cache", result.isFailure)
    }

    @Test
    fun `when search API returns 429 should search cache locally`() = runTest {
        cacheDao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("001", "Red Bull Classic", "Red Bull", 32.0, null, "250ml"),
                OfficialDrinkCacheEntity("002", "Monster Energy", "Monster", 30.0, null, "500ml")
            )
        )

        val httpException = mockk<retrofit2.HttpException>(relaxed = true)
        every { httpException.code() } returns 429
        coEvery {
            apiService.searchProducts(query = any(), json = any(), pageSize = any(), fields = any(), locale = any(), country = any())
        } throws httpException

        val result = repository.searchOfficialDrinks("monster")

        assertTrue(result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(1, drinks.size)
        assertEquals("Monster Energy", drinks[0].name)
    }

    @Test
    fun `when fresh cache exists should serve from cache`() = runTest {
        cacheDao.insertAll(
            listOf(
                OfficialDrinkCacheEntity(
                    barcode = "001", name = "Fresh Cola", brand = "Coca-Cola",
                    caffeineMgPer100ml = 10.0, energyKcalPer100ml = 42.0, quantity = "330ml",
                    fetchedAtMillis = System.currentTimeMillis()
                )
            )
        )

        val result = repository.getOfficialDrinks()

        assertTrue("Expected success from fresh cache", result.isSuccess)
        val drinks = result.getOrThrow()
        assertEquals(1, drinks.size)
        assertEquals("Fresh Cola", drinks[0].name)
    }
}
