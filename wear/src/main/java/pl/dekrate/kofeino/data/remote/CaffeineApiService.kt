package pl.dekrate.kofeino.data.remote

import pl.dekrate.kofeino.data.remote.dto.OpenFoodFactsSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service dla Open Food Facts API.
 *
 * Dokumentacja: https://openfoodfacts.github.io/openfoodfacts-server/api/
 * API jest w pełni otwarte, nie wymaga klucza.
 * Limit: 10 zapytań/min dla search, 15/min dla product read.
 *
 * Bazowy URL: https://world-fr.openfoodfacts.org/api/v2/
 */
interface CaffeineApiService {

    /**
     * Szuka napojów z danymi o kofeinie (v2 search).
     */
    @GET("search")
    suspend fun searchBeveragesWithCaffeine(
        @Query("categories_tags_en") categories: String = "en:beverages",
        @Query("caffeine_100g") caffeineFilter: String = ">0",
        @Query("page_size") pageSize: Int = 30,
        @Query("page") page: Int = 1,
        @Query("fields") fields: String = "code,product_name,brands,nutriments,quantity"
    ): OpenFoodFactsSearchResponse

    /**
     * Szuka w konkretnej podkategorii (np. kawy, herbaty, energetyki).
     */
    @GET("search")
    suspend fun searchByCategory(
        @Query("categories_tags_en") category: String,
        @Query("caffeine_100g") caffeineFilter: String = ">0",
        @Query("page_size") pageSize: Int = 20,
        @Query("page") page: Int = 1,
        @Query("fields") fields: String = "code,product_name,brands,nutriments,quantity"
    ): OpenFoodFactsSearchResponse

    /**
     * Full-text search via v1 search endpoint (the only one supporting search_terms).
     *
     * @param query Search phrase (e.g. "red bull", "coca cola")
     * @param pageSize Number of results
     * @param fields Fields to return
     * @param locale Language (pl, en, fr, de...)
     * @param country Country (PL, US, FR, DE...)
     */
    @GET("../../cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") query: String,
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("fields") fields: String = "code,product_name,brands,nutriments,quantity",
        @Query("lc") locale: String = "pl",
        @Query("cc") country: String = "PL"
    ): OpenFoodFactsSearchResponse

    /**
     * Pobiera szczegóły produktu po kodzie kreskowym.
     */
    @GET("product/{barcode}")
    suspend fun getProductByBarcode(
        @retrofit2.http.Path("barcode") barcode: String,
        @Query("fields") fields: String = "code,product_name,brands,nutriments,quantity"
    ): retrofit2.Response<OpenFoodFactsSearchResponse>
}
