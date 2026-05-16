package pl.dekrate.kofeino.data.repository

import pl.dekrate.kofeino.data.local.OfficialDrinkCacheDao
import pl.dekrate.kofeino.data.local.OfficialDrinkCacheEntity
import pl.dekrate.kofeino.data.remote.CaffeineApiService
import pl.dekrate.kofeino.data.remote.ConnectivityObserver
import pl.dekrate.kofeino.data.remote.dto.OpenFoodFactsProductDto
import pl.dekrate.kofeino.domain.model.OfficialDrink
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacja OfficialDrinkRepository.
 *
 * Strategia:
 * 1. Jeśli online → fetch z API, zapisz do cache, zwróć wyniki
 * 2. Jeśli offline → zwróć z cache (jeśli istnieje)
 * 3. Jeśli offline i brak cache → błąd
 *
 * Cache jest ważny przez 1 godzinę.
 */
@Singleton
class OfficialDrinkRepositoryImpl @Inject constructor(
    private val apiService: CaffeineApiService,
    private val cacheDao: OfficialDrinkCacheDao,
    private val connectivityObserver: ConnectivityObserver
) : OfficialDrinkRepository {

    companion object {
        /** Czas ważności cache: 1 godzina */
        private const val CACHE_TTL_MILLIS = 60 * 60 * 1000L
        /** Maksymalna liczba wyników z API */
        private const val MAX_RESULTS = 50
    }

    override suspend fun getOfficialDrinks(): Result<List<OfficialDrink>> {
        return if (connectivityObserver.isOnline) {
            fetchFromApi()
        } else {
            loadFromCache()
        }
    }

    override suspend fun searchOfficialDrinks(query: String): Result<List<OfficialDrink>> {
        return if (connectivityObserver.isOnline) {
            searchFromApi(query)
        } else {
            // Offline — przeszukaj cache po nazwie
            searchCacheLocally(query)
        }
    }

    /**
     * Wyszukuje w API przez v1 search (full-text).
     */
    private suspend fun searchFromApi(query: String): Result<List<OfficialDrink>> {
        return try {
            Timber.d("Searching API for: $query")
            val response = apiService.searchProducts(query = query)

            val drinks = response.products
                .filter { it.hasValidCaffeineData() }
                .map { it.toOfficialDrink() }
                .distinctBy { it.barcode }
                .take(MAX_RESULTS)

            Timber.d("Search returned ${drinks.size} drinks for: $query")
            Result.success(drinks)
        } catch (e: Exception) {
            Timber.e(e, "Search failed for: $query")
            searchCacheLocally(query)
        }
    }

    /**
     * Filtruje cache lokalny po nazwie (fallback offline).
     */
    private suspend fun searchCacheLocally(query: String): Result<List<OfficialDrink>> {
        val all = cacheDao.getAllCached()
        val lower = query.lowercase()
        val filtered = all.filter {
            it.name.lowercase().contains(lower) ||
                (it.brand?.lowercase()?.contains(lower) == true)
        }
        if (filtered.isEmpty()) {
            return Result.failure(Exception("No drinks found for: $query"))
        }
        return Result.success(filtered.map { it.toOfficialDrink() })
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

    /**
     * Pobiera dane z Open Food Facts API.
     * Łączy wyniki z różnych kategorii (beverages, coffees, teas, energy drinks).
     */
    private suspend fun fetchFromApi(): Result<List<OfficialDrink>> {
        return try {
            Timber.d("Fetching official drinks from Open Food Facts API")

            // Pobierz z głównej kategorii beverages
            val response = apiService.searchBeveragesWithCaffeine(pageSize = 30)

            if (response.products.isEmpty()) {
                Timber.w("API returned no products, falling back to cache")
                return loadFromCache().map { cached ->
                    if (cached.isNotEmpty()) cached
                    else emptyList()
                }
            }

            val drinks = response.products
                .filter { it.hasValidCaffeineData() }
                .map { it.toOfficialDrink() }
                .distinctBy { it.barcode }
                .take(MAX_RESULTS)

            // Zapisz do cache
            val cacheEntities = drinks.map { it.toCacheEntity() }
            cacheDao.clearAll()
            cacheDao.insertAll(cacheEntities)

            Timber.d("Fetched ${drinks.size} official drinks from API")
            Result.success(drinks)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch official drinks from API")
            // Fallback do cache
            val cached = cacheDao.getAllCached()
            if (cached.isNotEmpty()) {
                Timber.d("Falling back to ${cached.size} cached drinks")
                Result.success(cached.map { it.toOfficialDrink() })
            } else {
                Result.failure(e)
            }
        }
    }

    private suspend fun loadFromCache(): Result<List<OfficialDrink>> {
        val cached = cacheDao.getAllCached()
        if (cached.isEmpty()) {
            return Result.failure(Exception("Brak danych w cache i brak połączenia"))
        }
        Timber.d("Loaded ${cached.size} drinks from local cache")
        return Result.success(cached.map { it.toOfficialDrink() })
    }

    private fun OpenFoodFactsProductDto.hasValidCaffeineData(): Boolean {
        val caffeine100g = nutriments?.caffeine100g ?: return false
        return caffeine100g > 0.0
    }

    private fun OpenFoodFactsProductDto.toOfficialDrink(): OfficialDrink {
        val caffeineGrams = nutriments?.caffeine100g ?: 0.0
        // Użyj nazwy produktu, marki lub kodu kreskowego jako wyświetlanej nazwy
        val displayName = when {
            !productName.isNullOrBlank() -> productName
            !brands.isNullOrBlank() -> brands
            else -> "Napój #${code?.takeLast(6) ?: "???"}"
        }
        return OfficialDrink(
            barcode = code ?: "unknown",
            name = displayName,
            brand = brands,
            caffeineMgPer100ml = caffeineGrams * 1000.0, // g → mg
            energyKcalPer100ml = nutriments?.energyKcal100g,
            quantity = quantity,
            source = "Open Food Facts"
        )
    }

    private fun OfficialDrink.toCacheEntity(): OfficialDrinkCacheEntity {
        return OfficialDrinkCacheEntity(
            barcode = barcode,
            name = name,
            brand = brand,
            caffeineMgPer100ml = caffeineMgPer100ml,
            energyKcalPer100ml = energyKcalPer100ml,
            quantity = quantity
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
            source = "Open Food Facts"
        )
    }
}
