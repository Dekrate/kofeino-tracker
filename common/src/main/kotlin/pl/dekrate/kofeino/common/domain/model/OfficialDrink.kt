package pl.dekrate.kofeino.common.domain.model

/**
 * The canonical shared domain model for a drink looked up from an
 * official data source (e.g. Open Food Facts).
 *
 * This is a pure Kotlin data class (no Room or Android dependencies) used
 * across both `:app` and `:wear` modules.
 *
 * @property barcode               Product barcode (GTIN / EAN-13).
 * @property name                  Product name.
 * @property brand                 Brand name (nullable — not always available).
 * @property caffeineMgPer100ml    Caffeine content in mg per 100 ml of product.
 * @property energyKcalPer100ml    Energy in kcal per 100 ml (nullable).
 * @property quantity              Package quantity string, e.g. "250ml" (nullable).
 * @property source                Source attribution, e.g. "Open Food Facts".
 */
data class OfficialDrink(
    val barcode: String,
    val name: String,
    val brand: String? = null,
    val caffeineMgPer100ml: Double,
    val energyKcalPer100ml: Double? = null,
    val quantity: String? = null,
    val source: String = "Open Food Facts",
)
