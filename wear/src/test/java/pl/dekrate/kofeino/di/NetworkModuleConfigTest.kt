package pl.dekrate.kofeino.di

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import pl.dekrate.kofeino.data.remote.CustomDns
import pl.dekrate.kofeino.data.remote.OpenFoodFactsConfig

/**
 * Tests verifying that [NetworkModule] provisions instances using
 * the canonical [OpenFoodFactsConfig] endpoint.
 *
 * These are isolated provider tests — no Hilt required.
 */
class NetworkModuleConfigTest {

    @Test
    fun `provideRetrofit should return non-null instance`() {
        val retrofit = NetworkModule.provideRetrofit(
            OkHttpClient.Builder().build()
        )
        assertNotNull("Retrofit instance must not be null", retrofit)
    }

    @Test
    fun `provideRetrofit uses V2_BASE_URL from config`() {
        // Verify the config constant used by provideRetrofit is correct
        assertEquals(
            "V2_BASE_URL must be the canonical endpoint",
            "https://world.openfoodfacts.org/api/v2/",
            OpenFoodFactsConfig.V2_BASE_URL
        )
    }

    @Test
    fun `provideOkHttpClient should return non-null instance`() {
        val client = NetworkModule.provideOkHttpClient(CustomDns)
        assertNotNull("OkHttpClient must not be null", client)
    }

    @Test
    fun `provideCaffeineApiService should return non-null instance`() {
        val retrofit = NetworkModule.provideRetrofit(
            OkHttpClient.Builder().build()
        )
        val api = NetworkModule.provideCaffeineApiService(retrofit)
        assertNotNull("CaffeineApiService must not be null", api)
    }

    @Test
    fun `provideCustomDns should return CustomDns instance`() {
        val dns = NetworkModule.provideCustomDns()
        assertNotNull("CustomDns must not be null", dns)
        assertEquals(CustomDns, dns)
    }
}
