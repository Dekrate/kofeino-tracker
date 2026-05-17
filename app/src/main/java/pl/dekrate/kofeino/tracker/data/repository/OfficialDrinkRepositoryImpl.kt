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
 * Current strategy:
 * 1. Return data from local cache if available
 * 2. Return failure if cache is empty
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
        return loadFromCache()
    }

    override suspend fun searchOfficialDrinks(query: String): Result<List<OfficialDrink>> {
        return searchCacheLocally(query)
    }

    override suspend fun hasFreshCache(): Boolean {
        val cached = cacheDao.getAllCached()
        if (cached.isEmpty()) return false
        val now = System.currentTimeMillis()
        return cached.any { now - it.fetchedAtMillis < CACHE_TTL_MILLIS }
    }

    override suspend fun clearCache() {
        cacheDao.clearAll()
        Timber.d("Official drink cache cleared")
    }

    private suspend fun loadFromCache(): Result<List<OfficialDrink>> {
        val cached = cacheDao.getAllCached()
        if (cached.isEmpty()) {
            return Result.failure(Exception("No cached data available"))
        }
        Timber.d("Loaded ${cached.size} drinks from local cache")
        return Result.success(cached.map { it.toOfficialDrink() })
    }

    private suspend fun searchCacheLocally(query: String): Result<List<OfficialDrink>> {
        val all = cacheDao.getAllCached()
        val lower = query.lowercase()
        val filtered = all.filter {
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
