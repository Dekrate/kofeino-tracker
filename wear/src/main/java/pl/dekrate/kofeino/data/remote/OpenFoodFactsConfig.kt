package pl.dekrate.kofeino.data.remote

import pl.dekrate.kofeino.common.util.OpenFoodFactsConfig as CommonConfig

object OpenFoodFactsConfig {
    const val HOST: String = CommonConfig.HOST
    const val V2_BASE_URL: String = CommonConfig.V2_BASE_URL
    const val ROOT_BASE_URL: String = CommonConfig.ROOT_BASE_URL
    const val PATH_SEARCH_V2: String = CommonConfig.PATH_SEARCH_V2
    const val PATH_CATEGORY_SEARCH: String = CommonConfig.PATH_CATEGORY_SEARCH
    const val PATH_PRODUCT_V2: String = CommonConfig.PATH_PRODUCT_V2
    const val MAX_RESULTS: Int = CommonConfig.MAX_RESULTS
    const val CACHE_TTL_MILLIS: Long = CommonConfig.CACHE_TTL_MILLIS
    const val V2_FIELDS: String = CommonConfig.V2_FIELDS
    const val HTTP_TOO_MANY_REQUESTS: Int = CommonConfig.HTTP_TOO_MANY_REQUESTS
    const val HTTP_SERVICE_UNAVAILABLE: Int = CommonConfig.HTTP_SERVICE_UNAVAILABLE
    const val RATE_LIMIT_CIRCUIT_BREAKER_THRESHOLD: Int = CommonConfig.RATE_LIMIT_CIRCUIT_BREAKER_THRESHOLD
    const val RATE_LIMIT_RETRY_DELAY_MS: Long = CommonConfig.RATE_LIMIT_RETRY_DELAY_MS

    const val PATH_SEARCH_V1: String = "../../cgi/search.pl"
    const val DEFAULT_PAGE_SIZE: Int = 30
}
