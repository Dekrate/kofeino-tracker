package pl.dekrate.kofeino.tracker.data.repository

import pl.dekrate.kofeino.tracker.domain.model.OfficialDrink

/**
 * Repository for official caffeine data.
 * Retrieves data from Open Food Facts API (when online) or local cache.
 *
 * Full network integration will be added in #9 (Data Layer - API + Preferences).
 * This version provides the interface and a cache-only implementation.
 */
interface OfficialDrinkRepository {

    /** Fetch list of official drinks with caffeine data. */
    suspend fun getOfficialDrinks(): Result<List<OfficialDrink>>

    /** Search drinks by query string. */
    suspend fun searchOfficialDrinks(query: String): Result<List<OfficialDrink>>

    /** Check if fresh cache data is available. */
    suspend fun hasFreshCache(): Boolean

    /** Clear the local cache. */
    suspend fun clearCache()
}
