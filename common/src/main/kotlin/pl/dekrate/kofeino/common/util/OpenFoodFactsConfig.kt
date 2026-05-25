package pl.dekrate.kofeino.common.util

object OpenFoodFactsConfig {
    const val HOST: String = "world.openfoodfacts.org"
    const val ROOT_BASE_URL: String = "https://$HOST"
    const val V2_BASE_URL: String = "https://$HOST/api/v2/"
    const val PATH_SEARCH_V2: String = "search"
    const val PATH_CATEGORY_SEARCH: String = "search"
    const val PATH_PRODUCT_V2: String = "product/{barcode}"
    const val MAX_RESULTS: Int = 50
    const val CACHE_TTL_MILLIS: Long = 60 * 60 * 1000L
    const val V2_FIELDS: String = "code,product_name,brands,nutriments,quantity"
    const val HTTP_TOO_MANY_REQUESTS: Int = 429
    const val HTTP_SERVICE_UNAVAILABLE: Int = 503
    const val RATE_LIMIT_CIRCUIT_BREAKER_THRESHOLD: Int = 3
    const val RATE_LIMIT_RETRY_DELAY_MS: Long = 60_000L

    // --- V1 CGI search path (for ROOT_BASE_URL-based Retrofit clients) ---
    const val PATH_SEARCH_V1: String = "cgi/search.pl"

    // --- V1 CGI search path relative from V2_BASE_URL (for V2_BASE_URL-based Retrofit clients) ---
    const val PATH_SEARCH_V1_FROM_V2: String = "../../cgi/search.pl"

    // --- V0 product endpoint ---
    const val PATH_PRODUCT_V0: String = "api/v0/product/{barcode}.json"

    // --- CGI search fields ---
    const val CGI_FIELDS: String = "code,product_name,brands,product_quantity,nutriments"

    // --- Default page size for CGI search ---
    const val CGI_PAGE_SIZE: Int = 25
}
