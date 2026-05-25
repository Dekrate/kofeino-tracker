package pl.dekrate.kofeino.domain.model

/**
 * Napój z oficjalnymi danymi o kofeinie (z Open Food Facts).
 * caffeineMg jest już przeliczony na mg na standardową porcję 100ml.
 *
 * @deprecated Przeniesione do [pl.dekrate.kofeino.common.domain.model.OfficialDrink].
 *   Używaj wersji wspólnej.
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
    val caffeineMgPer100ml: Double,   // mg kofeiny na 100ml
    val energyKcalPer100ml: Double?,
    val quantity: String?,
    val source: String = "Open Food Facts"
)
