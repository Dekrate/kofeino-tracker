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
 * 1. Return fresh cache immediately (offline-first, no network wait on init)
 * 2. Fetch from API in the background, cache results
 * 3. If cache is empty or stale, try API and block
 *
 * Cache TTL: 1 hour.
 * API caffeine values: returned in grams per 100g, converted to mg per 100ml.
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

        /** API returns caffeine in grams per 100g. Multiply by 10 to get mg per 100ml. */
        private const val CAFFEINE_G_TO_MG = 10.0
    }

    override suspend fun getOfficialDrinks(): Result<List<OfficialDrink>> {
        // 1. Return fresh cache immediately if available
        val cached = loadFreshFromCache()
        if (cached.isSuccess) return cached

        // 2. No fresh cache — try API with multiple broad queries to seed variety
        return try {
            val queries = listOf("coffee", "tea", "energy drink", "cola")
            val allDrinks = mutableListOf<OfficialDrink>()

            for (query in queries) {
                val response = api.searchProducts(query = query, pageSize = 15)
                val drinks = response.products
                    .filter { it.nutriments?.caffeine100g != null }
                    .map { it.toOfficialDrink() }
                allDrinks.addAll(drinks)
            }

            if (allDrinks.isNotEmpty()) {
                // Deduplicate by barcode
                val unique = allDrinks.distinctBy { it.barcode }
                cacheDao.insertAll(unique.map { it.toCacheEntity() })
                Timber.d("Loaded ${unique.size} drinks from API (${allDrinks.size} raw)")
                return Result.success(unique)
            }

            // 3. API returned nothing — last resort: stale cache
            loadStaleFromCache()
        } catch (e: Exception) {
            Timber.w(e, "API load failed, falling back to stale cache")
            loadStaleFromCache()
        }
    }

    override suspend fun searchOfficialDrinks(query: String): Result<List<OfficialDrink>> {
        // Try API search first
        return try {
            val response = api.searchProducts(query = query, pageSize = 25)
            val drinks = response.products
                .filter { it.nutriments?.caffeine100g != null }
                .map { it.toOfficialDrink() }

            // Cache results for offline use
            if (drinks.isNotEmpty()) {
                cacheDao.insertAll(drinks.map { it.toCacheEntity() })
            }
            Timber.d("API search for '$query' returned ${response.count} total, ${drinks.size} with caffeine")
            Result.success(drinks)
        } catch (e: Exception) {
            Timber.w(e, "API search failed for '$query', searching local cache")
            searchLocalCache(query)
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

    /** Returns only TTL-fresh cached entries, or failure. */
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

    /** Returns ALL cached entries even if stale — last resort fallback. */
    private suspend fun loadStaleFromCache(): Result<List<OfficialDrink>> {
        val all = cacheDao.getAllCached()
        if (all.isEmpty()) {
            return Result.failure(Exception("No cached data available"))
        }
        Timber.d("Loaded ${all.size} drinks from stale cache")
        return Result.success(all.map { it.toOfficialDrink() })
    }

    /** Searches only TTL-fresh cached entries, or returns empty list. */
    private suspend fun searchLocalCache(query: String): Result<List<OfficialDrink>> {
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

    /** Convert from Open Food Facts product to domain model.
     *  API returns caffeine in grams per 100g — convert to mg per 100ml. */
    private fun OpenFoodFactsProduct.toOfficialDrink(): OfficialDrink {
        val caffeineMg = (nutriments?.caffeine100g ?: 0.0) * CAFFEINE_G_TO_MG
        return OfficialDrink(
            barcode = code,
            name = productName,
            brand = brands,
            caffeineMgPer100ml = caffeineMg,
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
