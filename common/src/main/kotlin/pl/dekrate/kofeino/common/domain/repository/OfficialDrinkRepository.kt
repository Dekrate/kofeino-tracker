package pl.dekrate.kofeino.common.domain.repository

import pl.dekrate.kofeino.common.domain.model.OfficialDrink

/**
 * Shared repository contract for official caffeine drink data.
 *
 * Retrieves data from a remote source (e.g. Open Food Facts API) when
 * online, or from a local cache.  This interface mirrors the per-module
 * [OfficialDrinkRepository] contracts but operates on the canonical
 * [OfficialDrink] domain model defined in the `:common` module.
 */
interface OfficialDrinkRepository {

    /** Fetch the full list of official drinks with caffeine data. */
    suspend fun getOfficialDrinks(): Result<List<OfficialDrink>>

    /** Search official drinks by query string. */
    suspend fun searchOfficialDrinks(query: String): Result<List<OfficialDrink>>

    /** Check whether a fresh cache of official drinks is available. */
    suspend fun hasFreshCache(): Boolean

    /** Clear the local cache of official drinks. */
    suspend fun clearCache()
}
