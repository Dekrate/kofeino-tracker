package pl.dekrate.kofeino.data.remote

import pl.dekrate.kofeino.common.util.OpenFoodFactsConfig as CommonOpenFoodFactsConfig
import pl.dekrate.kofeino.data.remote.dto.OpenFoodFactsSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service for Open Food Facts API.
 *
 * Base URL is configured in [OpenFoodFactsConfig.V2_BASE_URL].
 * All endpoint paths resolve against that canonical base URL.
 *
 * Documentation: https://openfoodfacts.github.io/openfoodfacts-server/api/
 * API is fully open, no API key required.
 * Rate limits: 10 req/min for search, 15 req/min for product read.
 */
interface CaffeineApiService {

    /**
     * Searches beverages with caffeine data (v2 search).
     */
    @GET(CommonOpenFoodFactsConfig.PATH_SEARCH_V2)
    suspend fun searchBeveragesWithCaffeine(
        @Query("categories_tags_en") categories: String = "en:beverages",
        @Query("caffeine_100g") caffeineFilter: String = ">0",
        @Query("page_size") pageSize: Int = OpenFoodFactsConfig.DEFAULT_PAGE_SIZE,
        @Query("page") page: Int = 1,
        @Query("fields") fields: String = CommonOpenFoodFactsConfig.V2_FIELDS
    ): OpenFoodFactsSearchResponse

    /**
     * Searches in a specific subcategory (coffees, teas, energy drinks).
     */
    @GET(CommonOpenFoodFactsConfig.PATH_CATEGORY_SEARCH)
    suspend fun searchByCategory(
        @Query("categories_tags_en") category: String,
        @Query("caffeine_100g") caffeineFilter: String = ">0",
        @Query("page_size") pageSize: Int = 20,
        @Query("page") page: Int = 1,
        @Query("fields") fields: String = CommonOpenFoodFactsConfig.V2_FIELDS
    ): OpenFoodFactsSearchResponse

    /**
     * Full-text search via v1 CGI search endpoint.
     *
     * @param query Search phrase (e.g. "red bull", "coca cola")
     * @param pageSize Number of results
     * @param fields Fields to return
     * @param locale Language (pl, en, fr, de...)
     * @param country Country (PL, US, FR, DE...)
     */
    @GET(OpenFoodFactsConfig.PATH_SEARCH_V1)
    suspend fun searchProducts(
        @Query("search_terms") query: String,
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("fields") fields: String = "code,product_name,brands,nutriments,quantity",
        @Query("lc") locale: String = "pl",
        @Query("cc") country: String = "PL"
    ): OpenFoodFactsSearchResponse

    /**
     * Fetches product details by barcode.
     */
    @GET(CommonOpenFoodFactsConfig.PATH_PRODUCT_V2)
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = "code,product_name,brands,nutriments,quantity"
    ): retrofit2.Response<OpenFoodFactsSearchResponse>
}
