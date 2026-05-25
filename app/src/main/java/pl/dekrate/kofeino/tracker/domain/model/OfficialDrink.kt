package pl.dekrate.kofeino.tracker.domain.model

/**
 * A drink with official caffeine data (from Open Food Facts).
 * [caffeineMgPer100ml] is in mg per 100ml.
 *
 * @deprecated Moved to [pl.dekrate.kofeino.common.domain.model.OfficialDrink].
 *   Use the common version instead.
 */
@Deprecated(
    message = "Moved to pl.dekrate.kofeino.common.domain.model",
    replaceWith = ReplaceWith(
        expression = "OfficialDrink(barcode, name, brand, caffeineMgPer100ml, energyKcalPer100ml, quantity, source)",
        imports = ["pl.dekrate.kofeino.common.domain.model.OfficialDrink"]
    )
)
data class OfficialDrink(
    val barcode: String,
    val name: String,
    val brand: String?,
    val caffeineMgPer100ml: Double,
    val energyKcalPer100ml: Double?,
    val quantity: String?,
    val source: String = "Open Food Facts"
)
