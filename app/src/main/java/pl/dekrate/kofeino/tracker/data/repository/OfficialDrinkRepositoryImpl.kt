package pl.dekrate.kofeino.tracker.data.repository

import pl.dekrate.kofeino.tracker.data.local.OfficialDrinkCacheDao
import pl.dekrate.kofeino.tracker.data.local.OfficialDrinkCacheEntity
import pl.dekrate.kofeino.tracker.data.remote.OpenFoodFactsApi
import pl.dekrate.kofeino.tracker.data.remote.OpenFoodFactsProduct
import pl.dekrate.kofeino.tracker.domain.model.OfficialDrink
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [OfficialDrinkRepository] backed by Open Food Facts API
 * with local cache for offline-first behavior.
 *
 * Strategy:
 * 1. Try API first (search or load popular)
 * 2. On success: cache results, return from API
 * 3. On failure: fall back to local cache (if fresh)
 *
 * Cache TTL: 1 hour — after that, API is tried again on next request.
 */
@Singleton
class OfficialDrinkRepositoryImpl @Inject constructor(
    private val api: OpenFoodFactsApi,
    private val cacheDao: OfficialDrinkCacheDao
) : OfficialDrinkRepository {

    companion object {
        private const val CACHE_TTL_MILLIS = 60 * 60 * 1000L
        private const val SOURCE_API = "Open Food Facts"
        private const val SOURCE_CACHE = "Local Cache"
    }

    override suspend fun getOfficialDrinks(): Result<List<OfficialDrink>> {
        // Try API first for a broad "coffee" search to populate the list
        return try {
            val response = api.searchProducts(
                query = "coffee",
                pageSize = 25
            )
            // Show all API products — caffeine value is 0 if not available
            val drinks = response.products.map { it.toOfficialDrink() }

            if (drinks.isNotEmpty()) {
                cacheDao.insertAll(drinks.map { it.toCacheEntity() })
                Timber.d("Loaded ${drinks.size} drinks from API")
                return Result.success(drinks)
            }
            // API returned empty — fall back to cache
            loadFreshFromCache()
        } catch (e: Exception) {
            Timber.w(e, "API load failed, falling back to cache")
            loadFreshFromCache()
        }
    }

    override suspend fun searchOfficialDrinks(query: String): Result<List<OfficialDrink>> {
        // Try API search first
        return try {
            val response = api.searchProducts(
                query = query,
                pageSize = 25
            )
            // Show all API products — caffeine value is 0 if not available
            val drinks = response.products.map { it.toOfficialDrink() }

            // Cache all results for offline use
            if (drinks.isNotEmpty()) {
                cacheDao.insertAll(drinks.map { it.toCacheEntity() })
            }
            Timber.d("API search for '$query' returned ${response.count} total, ${drinks.size} mapped")
            Result.success(drinks)
        } catch (e: Exception) {
            Timber.w(e, "API search failed for '$query', searching local cache")
            searchFreshCacheLocally(query)
        }
    }

    override suspend fun hasFreshCache(): Boolean {
        val cached = cacheDao.getAllCached()
        if (cached.isEmpty()) return false
        val now = System.currentTimeMillis()
        return cached.all { now - it.fetchedAtMillis < CACHE_TTL_MILLIS }
    }

    override suspend fun clearCache() {
        cacheDao.clearAll()
        Timber.d("Official drink cache cleared")
    }

    // --- Cache helpers ---

    private suspend fun loadFreshFromCache(): Result<List<OfficialDrink>> {
        val now = System.currentTimeMillis()
        val fresh = cacheDao.getAllCached()
            .filter { now - it.fetchedAtMillis < CACHE_TTL_MILLIS }
        if (fresh.isEmpty()) {
            return Result.failure(Exception("No fresh cached data available"))
        }
        Timber.d("Loaded ${fresh.size} fresh drinks from local cache")
        return Result.success(fresh.map { it.toOfficialDrink() })
    }

    private suspend fun searchFreshCacheLocally(query: String): Result<List<OfficialDrink>> {
        val now = System.currentTimeMillis()
        val fresh = cacheDao.getAllCached()
            .filter { now - it.fetchedAtMillis < CACHE_TTL_MILLIS }
        if (fresh.isEmpty()) {
            return Result.success(emptyList())
        }
        val lower = query.lowercase()
        val filtered = fresh.filter {
            it.name.lowercase().contains(lower) ||
                (it.brand?.lowercase()?.contains(lower) == true)
        }
        return Result.success(filtered.map { it.toOfficialDrink() })
    }

    // --- Mapping ---

    private fun OpenFoodFactsProduct.toOfficialDrink(): OfficialDrink {
        return OfficialDrink(
            barcode = code,
            name = productName,
            brand = brands,
            caffeineMgPer100ml = nutriments?.caffeineValue100g ?: 0.0,
            energyKcalPer100ml = nutriments?.energyKcalValue100g,
            quantity = productQuantity,
            source = SOURCE_API
        )
    }

    private fun OfficialDrink.toCacheEntity(): OfficialDrinkCacheEntity {
        return OfficialDrinkCacheEntity(
            barcode = barcode,
            name = name,
            brand = brand,
            caffeineMgPer100ml = caffeineMgPer100ml,
            energyKcalPer100ml = energyKcalPer100ml,
            quantity = quantity,
            fetchedAtMillis = System.currentTimeMillis()
        )
    }

    private fun OfficialDrinkCacheEntity.toOfficialDrink(): OfficialDrink {
        return OfficialDrink(
            barcode = barcode,
            name = name,
            brand = brand,
            caffeineMgPer100ml = caffeineMgPer100ml,
            energyKcalPer100ml = energyKcalPer100ml,
            quantity = quantity,
            source = SOURCE_CACHE
        )
    }
}
