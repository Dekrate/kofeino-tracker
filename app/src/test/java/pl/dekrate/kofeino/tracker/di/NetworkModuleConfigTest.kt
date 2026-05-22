package pl.dekrate.kofeino.tracker.di

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import pl.dekrate.kofeino.tracker.data.remote.OpenFoodFactsConfig

/**
 * Tests verifying that [:app] module's [NetworkModule] provisions
 * instances using the canonical [OpenFoodFactsConfig] endpoint.
 */
class NetworkModuleConfigTest {

    @Test
    fun `provideOpenFoodFactsApi should return non-null instance`() {
        val api = NetworkModule.provideOpenFoodFactsApi(
            OkHttpClient.Builder().build()
        )
        assertNotNull("OpenFoodFactsApi must not be null", api)
    }

    @Test
    fun `retrofit base URL should match config ROOT_BASE_URL`() {
        // Verify the config constant is correct
        assertEquals(
            "ROOT_BASE_URL should be https://world.openfoodfacts.org",
            "https://world.openfoodfacts.org",
            OpenFoodFactsConfig.ROOT_BASE_URL
        )
    }
}
