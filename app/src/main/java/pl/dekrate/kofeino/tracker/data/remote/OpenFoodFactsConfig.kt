package pl.dekrate.kofeino.tracker.data.remote

/**
 * Central configuration for Open Food Facts API endpoint.
 *
 * Both [:wear] and [:app] modules MUST use the same canonical base URL
 * to ensure consistent data across devices.
 *
 * ## Canonical Endpoint
 * ```
 * https://world.openfoodfacts.org
 * ```
 *
 * ### Rationale
 * - **Broader global coverage** — `world` is the community standard recommended by OFF
 * - **Consistent data** — eliminates discrepancies between regional mirrors
 * - **Simpler debugging** — one endpoint to monitor and test
 *
 * @see <a href="https://openfoodfacts.github.io/openfoodfacts-server/api/">OFF API Docs</a>
 */
object OpenFoodFactsConfig {

    /** Canonical hostname for Open Food Facts API. */
    const val HOST: String = "world.openfoodfacts.org"

    /** Base URL for root-level API.
     * Used by [:app] module's Retrofit instance.
     * Verified compatible with all required endpoint paths. */
    const val ROOT_BASE_URL: String = "https://$HOST"

    /** Base URL for v2 API (used by [:wear] module). */
    const val V2_BASE_URL: String = "https://$HOST/api/v2/"

    // ── API Endpoint Paths ──────────────────────────────────

    /** Full-text search (v1 CGI). */
    const val PATH_SEARCH_V1: String = "cgi/search.pl"

    /** Beverage search (v2). */
    const val PATH_SEARCH_V2: String = "search"

    /** Category-based search (v2). */
    const val PATH_CATEGORY_SEARCH: String = "search"

    /** Product by barcode (v0). */
    const val PATH_PRODUCT_V0: String = "api/v0/product/{barcode}.json"

    /** Product by barcode (v2). */
    const val PATH_PRODUCT_V2: String = "product/{barcode}"

    // ── Request Defaults ────────────────────────────────────

    /** Default page size for search requests. */
    const val DEFAULT_PAGE_SIZE: Int = 25

    /** Maximum results to process per endpoint call. */
    const val MAX_RESULTS: Int = 50

    /** Cache TTL: 1 hour in milliseconds. */
    const val CACHE_TTL_MILLIS: Long = 60 * 60 * 1000L

    /** Fields returned by CGI API responses (phone module). */
    const val CGI_FIELDS: String = "code,product_name,brands,product_quantity,nutriments"

    /** Fields returned by v2 API responses (watch module). */
    const val V2_FIELDS: String = "code,product_name,brands,nutriments,quantity"
}
