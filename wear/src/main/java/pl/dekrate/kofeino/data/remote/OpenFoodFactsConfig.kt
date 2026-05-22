package pl.dekrate.kofeino.data.remote

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
 * ### Previous endpoint (watch module)
 * `world-fr.openfoodfacts.org` — French mirror with narrower geographic coverage.
 * Some product databases (e.g., non-European products) had incomplete data.
 *
 * @see <a href="https://openfoodfacts.github.io/openfoodfacts-server/api/">OFF API Docs</a>
 */
object OpenFoodFactsConfig {

    /** Canonical hostname for Open Food Facts API. */
    const val HOST: String = "world.openfoodfacts.org"

    /** Base URL for v2 API (used by [:wear] module). */
    const val V2_BASE_URL: String = "https://$HOST/api/v2/"

    /** Base URL for root-level API (used by [:app] module). */
    const val ROOT_BASE_URL: String = "https://$HOST"

    // ── API Endpoint Paths ──────────────────────────────────

    /** Full-text search (v1 CGI) — [/cgi/search.pl]. */
    const val PATH_SEARCH_V1: String = "../../cgi/search.pl"

    /** Beverage search (v2) — [/api/v2/search]. */
    const val PATH_SEARCH_V2: String = "search"

    /** Category-based search (v2) — [/api/v2/search]. */
    const val PATH_CATEGORY_SEARCH: String = "search"

    /** Product by barcode (v2) — [/api/v2/product/{barcode}]. */
    const val PATH_PRODUCT_V2: String = "product/{barcode}"

    // ── Request Defaults ────────────────────────────────────

    /** Default page size for search requests. */
    const val DEFAULT_PAGE_SIZE: Int = 30

    /** Maximum results to process per endpoint call. */
    const val MAX_RESULTS: Int = 50

    /** Cache TTL: 1 hour in milliseconds. */
    const val CACHE_TTL_MILLIS: Long = 60 * 60 * 1000L

    /** Fields returned by v2 API responses. */
    const val DEFAULT_FIELDS: String = "code,product_name,brands,nutriments,quantity"

    /** Fields returned by v2 API responses (alias for watch module). */
    const val V2_FIELDS: String = "code,product_name,brands,nutriments,quantity"

    // ── Rate Limit Protection ───────────────────────────────

    /** HTTP status code for Too Many Requests. */
    const val HTTP_TOO_MANY_REQUESTS: Int = 429

    /** HTTP status code for Service Unavailable. */
    const val HTTP_SERVICE_UNAVAILABLE: Int = 503

    /** Minimum delay (ms) before retrying after a rate limit hit. */
    const val RATE_LIMIT_RETRY_DELAY_MS: Long = 60_000L // 1 minute

    /** Maximum rate limit events before circuit-breaker engages. */
    const val RATE_LIMIT_CIRCUIT_BREAKER_THRESHOLD: Int = 3
}
