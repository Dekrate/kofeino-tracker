package pl.dekrate.kofeino.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO dla odpowiedzi z Open Food Facts API (wyszukiwarka).
 */
data class OpenFoodFactsSearchResponse(
    val count: Int = 0,
    val page: Int = 1,
    @SerializedName("page_size")
    val pageSize: Int = 20,
    @SerializedName("page_count")
    val pageCount: Int = 0,
    val products: List<OpenFoodFactsProductDto> = emptyList()
)

/**
 * DTO dla pojedynczego produktu z Open Food Facts.
 * Wszystkie pola opcjonalne – API często zwraca null dla brakujących danych.
 */
data class OpenFoodFactsProductDto(
    val code: String? = null,
    @SerializedName("product_name")
    val productName: String? = null,
    val brands: String? = null,
    val nutriments: OpenFoodFactsNutrimentsDto? = null,
    val quantity: String? = null,
    @SerializedName("categories_tags_en")
    val categoriesTagsEn: List<String>? = null
)

/**
 * DTO dla wartości odżywczych.
 * caffeine_100g jest w GRAMACH na 100g (np. 0.063 = 63mg/100ml).
 * Do przeliczenia na mg: caffeineMg = caffeine_100g * 1000
 */
data class OpenFoodFactsNutrimentsDto(
    @SerializedName("caffeine_100g")
    val caffeine100g: Double? = null,
    @SerializedName("caffeine_value")
    val caffeineValue: Double? = null,
    @SerializedName("caffeine_unit")
    val caffeineUnit: String? = null,
    @SerializedName("energy-kcal_100g")
    val energyKcal100g: Double? = null,
    @SerializedName("energy-kcal_value")
    val energyKcalValue: Double? = null
)
