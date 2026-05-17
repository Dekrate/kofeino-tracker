package pl.dekrate.kofeino.tracker.data.repository

import pl.dekrate.kofeino.tracker.data.local.OfficialDrinkCacheDao
import pl.dekrate.kofeino.tracker.data.local.OfficialDrinkCacheEntity
import pl.dekrate.kofeino.tracker.domain.model.OfficialDrink
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache-only implementation of [OfficialDrinkRepository].
 *
 * When #9 (Data Layer - API + Preferences) is implemented, this will be
 * extended with network backing via Retrofit/CaffeineApiService.
 *
 * Strategy:
 * - All load/search methods filter by TTL, returning only fresh entries.
 * - If filtering leaves zero entries, the result is a failure (no fresh data).
 * - [hasFreshCache] uses `all` because the cache is batch-populated —
 *   partial expiry shouldn't claim freshness.
 */
@Singleton
class OfficialDrinkRepositoryImpl @Inject constructor(
    private val cacheDao: OfficialDrinkCacheDao
) : OfficialDrinkRepository {

    companion object {
        /** Cache TTL: 1 hour */
        private const val CACHE_TTL_MILLIS = 60 * 60 * 1000L
    }

    override suspend fun getOfficialDrinks(): Result<List<OfficialDrink>> {
        return loadFreshFromCache()
    }

    override suspend fun searchOfficialDrinks(query: String): Result<List<OfficialDrink>> {
        return searchFreshCacheLocally(query)
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

    /** Returns only TTL-fresh cached entries, or failure if none are fresh. */
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

    /** Searches only TTL-fresh cached entries, or returns empty list if none fresh. */
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

    private fun OfficialDrinkCacheEntity.toOfficialDrink(): OfficialDrink {
        return OfficialDrink(
            barcode = barcode,
            name = name,
            brand = brand,
            caffeineMgPer100ml = caffeineMgPer100ml,
            energyKcalPer100ml = energyKcalPer100ml,
            quantity = quantity,
            source = "Local Cache"
        )
    }
}
