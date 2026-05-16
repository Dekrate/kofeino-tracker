package pl.dekrate.kofeino.domain.model

/**
 * Napój z oficjalnymi danymi o kofeinie (z Open Food Facts).
 * caffeineMg jest już przeliczony na mg na standardową porcję 100ml.
 */
data class OfficialDrink(
    val barcode: String,
    val name: String,
    val brand: String?,
    val caffeineMgPer100ml: Double,   // mg kofeiny na 100ml
    val energyKcalPer100ml: Double?,
    val quantity: String?,
    val source: String = "Open Food Facts"
)
