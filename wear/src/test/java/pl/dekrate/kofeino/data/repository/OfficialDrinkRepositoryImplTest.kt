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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class OfficialDrinkRepositoryImplTest {

    private lateinit var database: CaffeineDatabase
    private lateinit var cacheDao: OfficialDrinkCacheDao
    private lateinit var apiService: CaffeineApiService
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var repository: OfficialDrinkRepositoryImpl

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
        repository = OfficialDrinkRepositoryImpl(apiService, cacheDao, connectivityObserver)
    }

    @After
    fun teardown() {
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
}
