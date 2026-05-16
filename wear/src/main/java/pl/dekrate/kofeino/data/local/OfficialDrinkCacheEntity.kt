package pl.dekrate.kofeino.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache dla oficjalnych danych o napojach.
 * Przechowuje ostatnio pobrane dane z Open Food Facts,
 * aby były dostępne offline.
 */
@Entity(tableName = "official_drink_cache")
data class OfficialDrinkCacheEntity(
    @PrimaryKey
    val barcode: String,
    val name: String,
    val brand: String?,
    val caffeineMgPer100ml: Double,
    val energyKcalPer100ml: Double?,
    val quantity: String?,
    val fetchedAtMillis: Long = System.currentTimeMillis()
)
