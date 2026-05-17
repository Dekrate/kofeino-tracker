package pl.dekrate.kofeino.tracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache for official drink data retrieved from Open Food Facts.
 * Stores the last-fetched data so it's available offline.
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
