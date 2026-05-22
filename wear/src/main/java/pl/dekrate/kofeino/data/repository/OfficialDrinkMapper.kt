package pl.dekrate.kofeino.data.repository

import pl.dekrate.kofeino.data.local.OfficialDrinkCacheEntity
import pl.dekrate.kofeino.data.remote.dto.OpenFoodFactsProductDto
import pl.dekrate.kofeino.domain.model.OfficialDrink

/**
 * Mapper functions between Open Food Facts DTOs and domain models.
 *
 * Extracted from [OfficialDrinkRepositoryImpl] to reduce function count
 * and separate mapping concerns from repository orchestration.
 */
internal object OfficialDrinkMapper {

    fun OpenFoodFactsProductDto.hasValidCaffeineData(): Boolean {
        val caffeine100g = nutriments?.caffeine100g ?: return false
        return caffeine100g > 0.0
    }

    fun OpenFoodFactsProductDto.toOfficialDrink(): OfficialDrink {
        val caffeineGrams = nutriments?.caffeine100g ?: 0.0
        val displayName = when {
            !productName.isNullOrBlank() -> productName
            !brands.isNullOrBlank() -> brands
            else -> "Drink #${code?.takeLast(6) ?: "???"}"
        }
        return OfficialDrink(
            barcode = code ?: "unknown",
            name = displayName,
            brand = brands,
            caffeineMgPer100ml = caffeineGrams * 1000.0,
            energyKcalPer100ml = nutriments?.energyKcal100g,
            quantity = quantity,
            source = "Open Food Facts"
        )
    }

    fun OfficialDrink.toCacheEntity(): OfficialDrinkCacheEntity {
        return OfficialDrinkCacheEntity(
            barcode = barcode,
            name = name,
            brand = brand,
            caffeineMgPer100ml = caffeineMgPer100ml,
            energyKcalPer100ml = energyKcalPer100ml,
            quantity = quantity
        )
    }

    fun OfficialDrinkCacheEntity.toOfficialDrink(): OfficialDrink {
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
