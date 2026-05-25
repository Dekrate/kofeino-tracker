package pl.dekrate.kofeino.tracker.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import pl.dekrate.kofeino.common.util.OpenFoodFactsConfig as CommonOpenFoodFactsConfig

/**
 * Open Food Facts API interface.
 *
 * Base URL is configured in [CommonOpenFoodFactsConfig.ROOT_BASE_URL].
 * All endpoint paths resolve against that canonical base URL.
 *
 * Search returns products with caffeine data. We filter by [nutriments.caffeine_100g]
 * because not all products have caffeine listed.
 */
interface OpenFoodFactsApi {

    /**
     * Search for products by name.
     *
     * @param query Search terms (e.g. "coffee", "energy drink")
     * @param pageSize Results per page (max 50)
     * @param fields Comma-separated field names to include in response
     */
    @GET(CommonOpenFoodFactsConfig.PATH_SEARCH_V1)
    suspend fun searchProducts(
        @Query("search_terms") query: String,
        @Query("page_size") pageSize: Int = 25,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("fields") fields: String = CommonOpenFoodFactsConfig.CGI_FIELDS
    ): OpenFoodFactsSearchResponse

    /**
     * Get product details by barcode.
     */
    @GET(CommonOpenFoodFactsConfig.PATH_PRODUCT_V0)
    suspend fun getProduct(
        @Path("barcode") barcode: String
    ): OpenFoodFactsProductResponse
}

// ===== Response models =====

data class OpenFoodFactsSearchResponse(
    val count: Int = 0,
    val page: Int = 0,
    @SerializedName("page_size") val pageSize: Int = 0,
    val products: List<OpenFoodFactsProduct> = emptyList()
)

data class OpenFoodFactsProductResponse(
    val code: String = "",
    val product: OpenFoodFactsProduct? = null,
    val status: Int = 0
)

data class OpenFoodFactsProduct(
    val code: String = "",
    @SerializedName("product_name") val productName: String = "",
    val brands: String? = null,
    @SerializedName("product_quantity") val productQuantity: String? = null,
    val nutriments: OpenFoodFactsNutriments? = null
)

data class OpenFoodFactsNutriments(
    /** Caffeine in grams per 100g (API returns grams, not mg). */
    @SerializedName("caffeine_100g") val caffeine100g: Double? = null,
    @SerializedName("energy-kcal_value_100g") val energyKcalValue100g: Double? = null
)
