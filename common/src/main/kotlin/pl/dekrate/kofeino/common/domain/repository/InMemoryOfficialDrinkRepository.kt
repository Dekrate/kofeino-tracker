package pl.dekrate.kofeino.common.domain.repository

import pl.dekrate.kofeino.common.domain.model.OfficialDrink

/**
 * In-memory implementation of [OfficialDrinkRepository] for use in contract tests.
 *
 * This fake repository uses an in-memory list of [OfficialDrink] entries as its
 * backing store so that every call returns instant results without any network or
 * database I/O overhead.
 *
 * @param drinks  Seed values for the drink backing store (defaults to empty list).
 */
class InMemoryOfficialDrinkRepository(
    private val drinks: List<OfficialDrink> = emptyList(),
) : OfficialDrinkRepository {

    private var isCacheFresh: Boolean = true

    // ------------------------------------------------------------------
    // Drink operations
    // ------------------------------------------------------------------

    override suspend fun getOfficialDrinks(): Result<List<OfficialDrink>> {
        return Result.success(drinks.toList())
    }

    override suspend fun searchOfficialDrinks(query: String): Result<List<OfficialDrink>> {
        if (query.isBlank()) {
            return Result.success(drinks.toList())
        }
        val filtered = drinks.filter { drink ->
            drink.name.contains(query, ignoreCase = true)
                || drink.brand?.contains(query, ignoreCase = true) == true
        }
        return Result.success(filtered)
    }

    // ------------------------------------------------------------------
    // Cache operations
    // ------------------------------------------------------------------

    override suspend fun hasFreshCache(): Boolean {
        return isCacheFresh
    }

    override suspend fun clearCache() {
        isCacheFresh = false
    }
}
