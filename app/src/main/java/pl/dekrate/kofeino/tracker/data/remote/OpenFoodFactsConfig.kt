package pl.dekrate.kofeino.tracker.data.remote

object OpenFoodFactsConfig {
    const val PATH_SEARCH_V1: String = "cgi/search.pl"
    const val PATH_PRODUCT_V0: String = "api/v0/product/{barcode}.json"
    const val DEFAULT_PAGE_SIZE: Int = 25
    const val CGI_FIELDS: String = "code,product_name,brands,product_quantity,nutriments"
}
