package pl.dekrate.kofeino.tracker.domain.model

data class OfficialDrink(
    val barcode: String,
    val name: String,
    val brand: String?,
    val caffeineMgPer100ml: Double,
    val energyKcalPer100ml: Double?,
    val quantity: String?,
    val source: String = "Open Food Facts"
)
