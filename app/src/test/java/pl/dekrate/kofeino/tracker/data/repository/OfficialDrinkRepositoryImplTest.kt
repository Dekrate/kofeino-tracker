package pl.dekrate.kofeino.tracker.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import pl.dekrate.kofeino.tracker.data.local.OfficialDrinkCacheDao
import pl.dekrate.kofeino.tracker.data.local.OfficialDrinkCacheEntity
import pl.dekrate.kofeino.tracker.data.remote.ConnectivityObserver
import pl.dekrate.kofeino.tracker.data.remote.OpenFoodFactsApi

class OfficialDrinkRepositoryImplTest {

    private val api: OpenFoodFactsApi = mockk()
    private val cacheDao: OfficialDrinkCacheDao = mockk()
    private val connectivityObserver: ConnectivityObserver = mockk()
    private lateinit var repository: OfficialDrinkRepositoryImpl

    private val sampleCached = OfficialDrinkCacheEntity(
        barcode = "5901234567890",
        name = "Test Energy Drink",
        brand = "TestBrand",
        caffeineMgPer100ml = 32.0,
        energyKcalPer100ml = 45.0,
        quantity = "250ml",
        fetchedAtMillis = System.currentTimeMillis()
    )

    @Before
    fun setUp() {
        // By default, API is unavailable — tests exercise cache fallback
        coEvery { api.searchProducts(any(), any(), any(), any(), any()) } throws RuntimeException("API unavailable")
        // Default: online
        every { connectivityObserver.isOnline } returns true
        repository = OfficialDrinkRepositoryImpl(api, cacheDao, connectivityObserver)
    }

    // ===== Fresh cache tests =====

    @Test
    fun `getOfficialDrinks returns cached data when cache is fresh`() = runTest {
        coEvery { cacheDao.getAllCached() } returns listOf(sampleCached)

        val result = repository.getOfficialDrinks()

        assert(result.isSuccess) { "Expected success, got $result" }
        val drinks = result.getOrThrow()
        assert(drinks.size == 1) { "Expected 1 drink, got ${drinks.size}" }
        assert(drinks[0].barcode == "5901234567890")
        assert(drinks[0].name == "Test Energy Drink")
    }

    @Test
    fun `getOfficialDrinks returns failure when cache is empty and API fails`() = runTest {
        coEvery { cacheDao.getAllCached() } returns emptyList()

        val result = repository.getOfficialDrinks()

        assert(result.isFailure) { "Expected failure, got $result" }
    }

    // ===== ConnectivityObserver tests =====

    @Test
    fun `getOfficialDrinks skips API call when offline and cache is fresh`() = runTest {
        every { connectivityObserver.isOnline } returns false
        coEvery { cacheDao.getAllCached() } returns listOf(sampleCached)

        val result = repository.getOfficialDrinks()

        assert(result.isSuccess) { "Expected success, got $result" }
        assert(result.getOrThrow().size == 1)
        // API should NOT have been called
        coVerify(exactly = 0) { api.searchProducts(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `getOfficialDrinks returns stale cache when offline and no fresh cache`() = runTest {
        every { connectivityObserver.isOnline } returns false
        val expired = sampleCached.copy(fetchedAtMillis = 0L)
        coEvery { cacheDao.getAllCached() } returns listOf(expired)

        val result = repository.getOfficialDrinks()

        assert(result.isSuccess) { "Expected stale cache fallback when offline, got $result" }
        assert(result.getOrThrow().size == 1)
        coVerify(exactly = 0) { api.searchProducts(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `searchOfficialDrinks searches cache when offline`() = runTest {
        every { connectivityObserver.isOnline } returns false
        val drinks = listOf(
            sampleCached,
            sampleCached.copy(barcode = "2", name = "Cola"),
            sampleCached.copy(barcode = "3", name = "Red Bull")
        )
        coEvery { cacheDao.getAllCached() } returns drinks

        val result = repository.searchOfficialDrinks("Energy")

        assert(result.isSuccess)
        assert(result.getOrThrow().size == 1)
        coVerify(exactly = 0) { api.searchProducts(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `searchOfficialDrinks returns empty when offline and no fresh cache`() = runTest {
        every { connectivityObserver.isOnline } returns false
        val expired = sampleCached.copy(fetchedAtMillis = 0L)
        coEvery { cacheDao.getAllCached() } returns listOf(expired)

        val result = repository.searchOfficialDrinks("Energy")

        assert(result.isSuccess)
        assert(result.getOrThrow().isEmpty())
        coVerify(exactly = 0) { api.searchProducts(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `getOfficialDrinks tries API when online and cache is empty`() = runTest {
        every { connectivityObserver.isOnline } returns true
        coEvery { cacheDao.getAllCached() } returns emptyList()

        val result = repository.getOfficialDrinks()

        // API throws by default, so expects failure
        assert(result.isFailure)
        coVerify(atLeast = 1) { api.searchProducts(any(), any(), any(), any(), any()) }
    }

    // ===== Search tests =====

    @Test
    fun `searchOfficialDrinks filters by name`() = runTest {
        val drinks = listOf(
            sampleCached,
            sampleCached.copy(barcode = "2", name = "Cola"),
            sampleCached.copy(barcode = "3", name = "Red Bull")
        )
        coEvery { cacheDao.getAllCached() } returns drinks

        val result = repository.searchOfficialDrinks("Energy")

        assert(result.isSuccess) { "Expected success, got $result" }
        val filtered = result.getOrThrow()
        assert(filtered.size == 1) { "Expected 1 result, got ${filtered.size}" }
        assert(filtered[0].barcode == "5901234567890")
    }

    @Test
    fun `searchOfficialDrinks filters by brand`() = runTest {
        val drinks = listOf(
            sampleCached.copy(barcode = "1", brand = "Monster"),
            sampleCached.copy(barcode = "2", brand = "Red Bull", name = "Regular Cola")
        )
        coEvery { cacheDao.getAllCached() } returns drinks

        val result = repository.searchOfficialDrinks("monster")

        assert(result.isSuccess) { "Expected success, got $result" }
        val filtered = result.getOrThrow()
        assert(filtered.size == 1) { "Expected 1 result, got ${filtered.size}" }
        assert(filtered[0].barcode == "1")
    }

    @Test
    fun `searchOfficialDrinks returns empty list when no match`() = runTest {
        coEvery { cacheDao.getAllCached() } returns listOf(sampleCached)

        val result = repository.searchOfficialDrinks("nonexistent")

        assert(result.isSuccess) { "Expected success, got $result" }
        assert(result.getOrThrow().isEmpty()) { "Expected empty list" }
    }

    // ===== Cache freshness tests =====

    @Test
    fun `hasFreshCache returns true when cached entries are within TTL`() = runTest {
        coEvery { cacheDao.getAllCached() } returns listOf(sampleCached)

        val result = repository.hasFreshCache()

        assert(result) { "Expected fresh cache" }
    }

    @Test
    fun `hasFreshCache returns false when cache is empty`() = runTest {
        coEvery { cacheDao.getAllCached() } returns emptyList()

        val result = repository.hasFreshCache()

        assert(!result) { "Expected stale cache" }
    }

    @Test
    fun `hasFreshCache returns false when cache is expired`() = runTest {
        val expired = sampleCached.copy(fetchedAtMillis = 0L) // 1970
        coEvery { cacheDao.getAllCached() } returns listOf(expired)

        val result = repository.hasFreshCache()

        assert(!result) { "Expected stale cache" }
    }

    @Test
    fun `getOfficialDrinks returns stale cache when API fails and fresh cache is empty`() = runTest {
        val expired = sampleCached.copy(fetchedAtMillis = 0L) // 1970-01-01
        coEvery { cacheDao.getAllCached() } returns listOf(expired)

        val result = repository.getOfficialDrinks()

        assert(result.isSuccess) { "Expected stale cache fallback, got $result" }
        val drinks = result.getOrThrow()
        assert(drinks.size == 1) { "Expected 1 stale drink, got ${drinks.size}" }
        assert(drinks[0].barcode == "5901234567890")
    }

    @Test
    fun `searchOfficialDrinks returns empty when cache only has expired entries`() = runTest {
        val expired = sampleCached.copy(fetchedAtMillis = 0L)
        coEvery { cacheDao.getAllCached() } returns listOf(expired)

        val result = repository.searchOfficialDrinks("Energy")

        assert(result.isSuccess) { "Expected success for search even with expired cache" }
        assert(result.getOrThrow().isEmpty()) { "Expected empty results from expired cache" }
    }

    @Test
    fun `hasFreshCache returns false when one entry is expired in batch`() = runTest {
        val fresh = sampleCached.copy(barcode = "1", fetchedAtMillis = System.currentTimeMillis())
        val expired = sampleCached.copy(barcode = "2", fetchedAtMillis = 0L)
        coEvery { cacheDao.getAllCached() } returns listOf(fresh, expired)

        val result = repository.hasFreshCache()

        assert(!result) { "Expected stale cache when any entry is expired (batch semantics)" }
    }

    @Test
    fun `clearCache delegates to dao`() = runTest {
        coEvery { cacheDao.clearAll() } returns Unit

        repository.clearCache()

        coVerify { cacheDao.clearAll() }
    }

    // ===== Online/Offline edge cases =====

    @Test
    fun `multiple searches when offline uses cache for each call`() = runTest {
        every { connectivityObserver.isOnline } returns false
        val drinks = listOf(
            sampleCached,
            sampleCached.copy(barcode = "2", name = "Cola")
        )
        coEvery { cacheDao.getAllCached() } returns drinks

        val r1 = repository.searchOfficialDrinks("Energy")
        val r2 = repository.searchOfficialDrinks("Cola")

        assert(r1.getOrThrow().size == 1)
        assert(r2.getOrThrow().size == 1)
        coVerify(exactly = 0) { api.searchProducts(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `offline then online uses API for fresh data`() = runTest {
        // First call: offline
        every { connectivityObserver.isOnline } returns false
        coEvery { cacheDao.getAllCached() } returns listOf(sampleCached)
        repository.getOfficialDrinks()

        // Second call: online, empty cache → API called
        every { connectivityObserver.isOnline } returns true
        coEvery { cacheDao.getAllCached() } returns emptyList()

        val result = repository.getOfficialDrinks()
        assert(result.isFailure) // API throws by default
        coVerify(atLeast = 1) { api.searchProducts(any(), any(), any(), any(), any()) }
    }
}
