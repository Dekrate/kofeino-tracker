package pl.dekrate.kofeino.tracker.domain.model

/**
 * A drink with official caffeine data (from Open Food Facts).
 * [caffeineMgPer100ml] is in mg per 100ml.
 */
data class OfficialDrink(
    val barcode: String,
    val name: String,
    val brand: String?,
    val caffeineMgPer100ml: Double,
    val energyKcalPer100ml: Double?,
    val quantity: String?,
    val source: String = "Open Food Facts"
)
