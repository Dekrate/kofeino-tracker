package pl.dekrate.kofeino.tracker.data.remote

import pl.dekrate.kofeino.common.util.OpenFoodFactsConfig as CommonConfig

/**
 * @deprecated Moved to [pl.dekrate.kofeino.common.util.OpenFoodFactsConfig].
 *   Use the common version directly. Phone-specific constants
 *   (PATH_SEARCH_V1, PATH_PRODUCT_V0, DEFAULT_PAGE_SIZE, CGI_FIELDS)
 *   remain in this object and should be migrated separately.
 */
@Deprecated(
    message = "Moved to pl.dekrate.kofeino.common.util.OpenFoodFactsConfig",
    replaceWith = ReplaceWith(
        expression = "OpenFoodFactsConfig",
        imports = ["pl.dekrate.kofeino.common.util.OpenFoodFactsConfig"]
    )
)
object OpenFoodFactsConfig {
    const val HOST: String = CommonConfig.HOST
    const val ROOT_BASE_URL: String = CommonConfig.ROOT_BASE_URL
    const val V2_BASE_URL: String = CommonConfig.V2_BASE_URL
    const val PATH_SEARCH_V2: String = CommonConfig.PATH_SEARCH_V2
    const val PATH_CATEGORY_SEARCH: String = CommonConfig.PATH_CATEGORY_SEARCH
    const val PATH_PRODUCT_V2: String = CommonConfig.PATH_PRODUCT_V2
    const val MAX_RESULTS: Int = CommonConfig.MAX_RESULTS
    const val CACHE_TTL_MILLIS: Long = CommonConfig.CACHE_TTL_MILLIS
    const val V2_FIELDS: String = CommonConfig.V2_FIELDS

    const val PATH_SEARCH_V1: String = "cgi/search.pl"
    const val PATH_PRODUCT_V0: String = "api/v0/product/{barcode}.json"
    const val DEFAULT_PAGE_SIZE: Int = 25
    const val CGI_FIELDS: String = "code,product_name,brands,product_quantity,nutriments"
}
