package pl.dekrate.kofeino.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.OkHttpClient
import pl.dekrate.kofeino.data.remote.CaffeineApiService
import pl.dekrate.kofeino.data.remote.ConnectivityObserver
import pl.dekrate.kofeino.data.remote.CustomDns
import pl.dekrate.kofeino.data.remote.dto.OpenFoodFactsNutrimentsDto
import pl.dekrate.kofeino.data.remote.dto.OpenFoodFactsProductDto
import pl.dekrate.kofeino.data.remote.dto.OpenFoodFactsSearchResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Test module that replaces [NetworkModule] and provides a mock [CaffeineApiService].
 *
 * The mock returns controlled responses so E2E tests are deterministic.
 */
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class]
)
@Module
object TestNetworkModule {

    /** MOCK JSON fixture: 4 drinks with caffeine + 2 invalid (filtered out). */
    private val mockResponse = OpenFoodFactsSearchResponse(
        count = 4,
        page = 1,
        pageSize = 20,
        pageCount = 1,
        products = listOf(
            // 1. Normalny napój z kofeiną
            OpenFoodFactsProductDto(
                code = "0012345678901",
                productName = "Espresso",
                brands = "Illy",
                nutriments = OpenFoodFactsNutrimentsDto(
                    caffeine100g = 0.063,
                    energyKcal100g = 9.0
                ),
                quantity = "250 ml"
            ),
            // 2. Energy drink
            OpenFoodFactsProductDto(
                code = "0012345678902",
                productName = "Red Bull Energy Drink",
                brands = "Red Bull",
                nutriments = OpenFoodFactsNutrimentsDto(
                    caffeine100g = 0.032,
                    energyKcal100g = 45.0
                ),
                quantity = "250 ml"
            ),
            // 3. Cola zero
            OpenFoodFactsProductDto(
                code = "0012345678903",
                productName = "Coca-Cola Zero",
                brands = "Coca-Cola",
                nutriments = OpenFoodFactsNutrimentsDto(
                    caffeine100g = 0.010,
                    energyKcal100g = 0.3
                ),
                quantity = "330 ml"
            ),
            // 4. Monster
            OpenFoodFactsProductDto(
                code = "0012345678904",
                productName = "Monster Energy Ultra",
                brands = "Monster",
                nutriments = OpenFoodFactsNutrimentsDto(
                    caffeine100g = 0.030,
                    energyKcal100g = 10.0
                ),
                quantity = "500 ml"
            ),
            // 5. caffeine = 0 — powinien być odfiltrowany
            OpenFoodFactsProductDto(
                code = "0012345678905",
                productName = "Woda niegazowana",
                brands = "Żywiec Zdrój",
                nutriments = OpenFoodFactsNutrimentsDto(
                    caffeine100g = 0.0,
                    energyKcal100g = 0.0
                ),
                quantity = "500 ml"
            ),
            // 6. pusta nazwa — powinien być odfiltrowany
            OpenFoodFactsProductDto(
                code = "0012345678906",
                productName = null,
                brands = null,
                nutriments = OpenFoodFactsNutrimentsDto(
                    caffeine100g = 0.050,
                    energyKcal100g = null
                ),
                quantity = null
            )
        )
    )

    /** Empty response for "no results" search queries. */
    private val emptyResponse = OpenFoodFactsSearchResponse(
        count = 0, page = 1, pageSize = 20, pageCount = 0, products = emptyList()
    )

    @Provides
    @Singleton
    fun provideCustomDns(): CustomDns = CustomDns()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://example.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMockCaffeineApiService(): CaffeineApiService {
        val mock = mockk<CaffeineApiService>()

        // --- searchBeveragesWithCaffeine (główny fetch) ---
        coEvery {
            mock.searchBeveragesWithCaffeine(
                any(), any(), any(), any(), any()
            )
        } returns mockResponse

        // --- searchByCategory ---
        coEvery {
            mock.searchByCategory(any(), any(), any(), any(), any())
        } returns mockResponse

        // --- searchProducts (wyszukiwarka) ---
        coEvery {
            mock.searchProducts(
                query = any(),
                json = any(),
                pageSize = any(),
                fields = any(),
                locale = any(),
                country = any()
            )
        } answers {
            val query = firstArg<String>()
            if (query.isBlank() || query.lowercase().contains("zzz")) {
                emptyResponse
            } else if (query.lowercase().contains("red")) {
                // Tylko Red Bull
                OpenFoodFactsSearchResponse(
                    count = 1, page = 1, pageSize = 20, pageCount = 1,
                    products = listOf(mockResponse.products[1])
                )
            } else {
                mockResponse
            }
        }

        return mock
    }

    @Provides
    @Singleton
    fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver {
        return ConnectivityObserver(context)
    }
}
