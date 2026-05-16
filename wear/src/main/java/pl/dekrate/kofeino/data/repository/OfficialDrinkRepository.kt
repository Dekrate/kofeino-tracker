package pl.dekrate.kofeino.data.repository

import pl.dekrate.kofeino.domain.model.OfficialDrink

/**
 * Repozytorium oficjalnych danych o kofeinie.
 * Pobiera dane z Open Food Facts API (gdy online) lub z lokalnego cache.
 */
interface OfficialDrinkRepository {

    /** Pobiera listę oficjalnych napojów z kofeiną. */
    suspend fun getOfficialDrinks(): Result<List<OfficialDrink>>

    /** Wyszukuje napoje według frazy. */
    suspend fun searchOfficialDrinks(query: String): Result<List<OfficialDrink>>

    /** Sprawdza czy dostępne są świeże dane w cache. */
    suspend fun hasFreshCache(): Boolean

    /** Czyści cache. */
    suspend fun clearCache()
}
