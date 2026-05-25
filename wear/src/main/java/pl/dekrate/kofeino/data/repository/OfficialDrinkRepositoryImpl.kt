package pl.dekrate.kofeino.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.di.ApplicationScope
import pl.dekrate.kofeino.common.domain.repository.OfficialDrinkRepository
import pl.dekrate.kofeino.common.domain.model.OfficialDrink as CommonOfficialDrink
import pl.dekrate.kofeino.data.local.OfficialDrinkCacheDao
import pl.dekrate.kofeino.data.remote.CaffeineApiService
import pl.dekrate.kofeino.data.remote.ConnectivityObserver
import pl.dekrate.kofeino.common.util.OpenFoodFactsConfig
import pl.dekrate.kofeino.data.repository.OfficialDrinkMapper.hasValidCaffeineData
import pl.dekrate.kofeino.data.repository.OfficialDrinkMapper.toCacheEntity
import pl.dekrate.kofeino.data.repository.OfficialDrinkMapper.toOfficialDrink
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of OfficialDrinkRepository with rate-limit-aware resilience.
 *
 * ## Strategy
 *
 * ### Stale-while-revalidate (getOfficialDrinks)
 * - Fresh cache exists → serve from cache, initiate background refresh
 * - No fresh cache → fetch from API
 *
 * ### Rate limit resilience
 * - HTTP 429 (Too Many Requests) and 503 (Service Unavailable) are caught
 * - Immediate fallback to cache (even stale)
 * - Rate limit events are tracked for circuit-breaker pattern
 *
 * ### Offline-first (searchOfficialDrinks)
 * - Always attempts API first
 * - On HTTP 429/503 → searches local cache immediately
 */
@Singleton
class OfficialDrinkRepositoryImpl @Inject constructor(
    private val apiService: CaffeineApiService,
    private val cacheDao: OfficialDrinkCacheDao,
    private val connectivityObserver: ConnectivityObserver,
    @ApplicationScope private val applicationScope: CoroutineScope
) : OfficialDrinkRepository {

    companion object {
        private const val MAX_RESULTS = 50
        private const val API_PAGE_SIZE = 30

        /** Consecutive rate-limit counter (circuit breaker state). */
        private val consecutiveRateLimits = AtomicInteger(0)
    }

    /** Tracks in-flight background refresh to prevent concurrent accumulation. */
    private var refreshJob: Job? = null

    override suspend fun getOfficialDrinks(): Result<List<CommonOfficialDrink>> {
        val result: Result<List<CommonOfficialDrink>> = if (connectivityObserver.isOnline && !isCircuitBroken()) {
            if (hasFreshCache()) {
                Timber.d("Serving fresh cache, initiating background refresh")
                launchBackgroundRefresh()
                loadFromCache()
            } else {
                fetchFromApi()
            }
        } else {
            loadFromCache()
        }
        return result
    }

    override suspend fun searchOfficialDrinks(query: String): Result<List<CommonOfficialDrink>> {
        val result: Result<List<CommonOfficialDrink>> = if (connectivityObserver.isOnline && !isCircuitBroken()) {
            searchFromApi(query)
        } else {
            searchCacheLocally(query)
        }
        return result
    }

    override suspend fun hasFreshCache(): Boolean {
        val cached = cacheDao.getAllCached()
        if (cached.isEmpty()) return false
        val now = System.currentTimeMillis()
        return cached.any { now - it.fetchedAtMillis < OpenFoodFactsConfig.CACHE_TTL_MILLIS }
    }

    override suspend fun clearCache() {
        cacheDao.clearAll()
        consecutiveRateLimits.set(0)
        Timber.d("Official drink cache cleared")
    }

    // ── Circuit Breaker ─────────────────────────────────────

    private fun isCircuitBroken(): Boolean {
        if (consecutiveRateLimits.get() >= OpenFoodFactsConfig.RATE_LIMIT_CIRCUIT_BREAKER_THRESHOLD) {
            Timber.w("Circuit breaker open (%d consecutive rate limits)", consecutiveRateLimits.get())
            consecutiveRateLimits.set(0)
            return true
        }
        return false
    }

    private fun registerRateLimit(httpCode: Int) {
        val count = consecutiveRateLimits.incrementAndGet()
        Timber.w(
            "Rate limit hit (HTTP %d) — consecutive: %d/%d",
            httpCode,
            count,
            OpenFoodFactsConfig.RATE_LIMIT_CIRCUIT_BREAKER_THRESHOLD
        )
    }

    private fun resetRateLimitCounter() {
        val previous = consecutiveRateLimits.getAndSet(0)
        if (previous > 0) {
            Timber.d("Rate limit counter reset (successful API call)")
        }
    }

    // ── Background Refresh ──────────────────────────────────

    private fun launchBackgroundRefresh() {
        refreshJob?.cancel()
        refreshJob = applicationScope.launch {
            try {
                Timber.d("Background refresh starting")
                val response = apiService.searchBeveragesWithCaffeine(
                    pageSize = API_PAGE_SIZE
                )
                if (response.products.isNotEmpty()) {
                    val drinks = response.products
                        .filter { it.hasValidCaffeineData() }
                        .map { it.toOfficialDrink() }
                        .distinctBy { it.barcode }
                        .take(MAX_RESULTS)

                    val cacheEntities = drinks.map { it.toCacheEntity() }
                    cacheDao.clearAll()
                    cacheDao.insertAll(cacheEntities)
                    resetRateLimitCounter()
                    Timber.d("Background refresh completed: ${drinks.size} drinks")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (isRateLimitHttpError(e.code())) {
                    registerRateLimit(e.code())
                } else {
                    Timber.e(e, "Background refresh failed (HTTP %d)", e.code())
                }
            } catch (e: Exception) {
                Timber.e(e, "Background refresh failed")
            }
        }
    }

    // ── API Fetch ───────────────────────────────────────────

    private suspend fun fetchFromApi(): Result<List<CommonOfficialDrink>> {
        return try {
            Timber.d("Fetching official drinks from Open Food Facts API")
            val response = apiService.searchBeveragesWithCaffeine(
                pageSize = API_PAGE_SIZE
            )

            resetRateLimitCounter()

            if (response.products.isEmpty()) {
                Timber.w("API returned no products, falling back to cache")
                return loadFromCache().map { cached ->
                    if (cached.isNotEmpty()) cached else emptyList()
                }
            }

            val drinks = response.products
                .filter { it.hasValidCaffeineData() }
                .map { it.toOfficialDrink() }
                .distinctBy { it.barcode }
                .take(MAX_RESULTS)

            val cacheEntities = drinks.map { it.toCacheEntity() }
            cacheDao.clearAll()
            cacheDao.insertAll(cacheEntities)

            Timber.d("Fetched ${drinks.size} official drinks from API")
            Result.success(drinks)

        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            when {
                isRateLimitHttpError(e.code()) -> {
                    registerRateLimit(e.code())
                    Timber.w("API rate limited (HTTP %d) → cache fallback", e.code())
                    loadFromCache()
                }
                else -> {
                    Timber.e(e, "API HTTP error %d", e.code())
                    loadFromCache()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch official drinks from API")
            loadFromCache()
        }
    }

    private suspend fun searchFromApi(query: String): Result<List<CommonOfficialDrink>> {
        return try {
            Timber.d("Searching API for: $query")
            val response = apiService.searchProducts(query = query)

            resetRateLimitCounter()

            val drinks = response.products
                .filter { it.hasValidCaffeineData() }
                .map { it.toOfficialDrink() }
                .distinctBy { it.barcode }
                .take(MAX_RESULTS)

            Timber.d("Search returned ${drinks.size} drinks for: $query")
            Result.success(drinks)

        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            when {
                isRateLimitHttpError(e.code()) -> {
                    registerRateLimit(e.code())
                    Timber.w("Search rate limited (HTTP %d) → cache search", e.code())
                    searchCacheLocally(query)
                }
                else -> {
                    Timber.e(e, "Search HTTP error %d for: $query", e.code())
                    searchCacheLocally(query)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Search failed for: $query")
            searchCacheLocally(query)
        }
    }

    // ── Cache Operations ────────────────────────────────────

    private suspend fun searchCacheLocally(query: String): Result<List<CommonOfficialDrink>> {
        val all = cacheDao.getAllCached()
        val lower = query.lowercase()
        val filtered = all.filter {
            it.name.lowercase().contains(lower) ||
                (it.brand?.lowercase()?.contains(lower) == true)
        }
        if (filtered.isEmpty()) {
            return Result.success(emptyList())
        }
        return Result.success(filtered.map { it.toOfficialDrink() })
    }

    private suspend fun loadFromCache(): Result<List<CommonOfficialDrink>> {
        val cached = cacheDao.getAllCached()
        if (cached.isEmpty()) {
            return Result.failure(Exception("No cached data and no connection"))
        }
        Timber.d("Loaded ${cached.size} drinks from local cache")
        return Result.success(cached.map { it.toOfficialDrink() })
    }

    // ── HTTP Helpers ────────────────────────────────────────

    private fun isRateLimitHttpError(code: Int): Boolean {
        return code == OpenFoodFactsConfig.HTTP_TOO_MANY_REQUESTS ||
            code == OpenFoodFactsConfig.HTTP_SERVICE_UNAVAILABLE
    }
}
