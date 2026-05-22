package pl.dekrate.kofeino.tracker.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [OpenFoodFactsConfig] in the [:app] module.
 *
 * Also includes cross-module consistency checks to ensure
 * both [:wear] and [:app] modules use the same canonical endpoint.
 */
class OpenFoodFactsConfigTest {

    @Test
    fun `host should be world dot openfoodfacts dot org`() {
        assertEquals("world.openfoodfacts.org", OpenFoodFactsConfig.HOST)
    }

    @Test
    fun `ROOT_BASE_URL should contain the canonical host`() {
        assertTrue(
            "ROOT_BASE_URL must reference HOST",
            OpenFoodFactsConfig.ROOT_BASE_URL.contains(OpenFoodFactsConfig.HOST)
        )
    }

    @Test
    fun `ROOT_BASE_URL should be a valid HTTPS URL`() {
        assertTrue(
            "ROOT_BASE_URL must start with https://",
            OpenFoodFactsConfig.ROOT_BASE_URL.startsWith("https://")
        )
        assertEquals(
            "ROOT_BASE_URL should be just https://host",
            "https://${OpenFoodFactsConfig.HOST}",
            OpenFoodFactsConfig.ROOT_BASE_URL
        )
    }

    @Test
    fun `V2_BASE_URL should contain the canonical host`() {
        assertTrue(
            "V2_BASE_URL must reference HOST",
            OpenFoodFactsConfig.V2_BASE_URL.contains(OpenFoodFactsConfig.HOST)
        )
    }

    @Test
    fun `V2_BASE_URL should end with api v2 path`() {
        assertTrue(
            "V2_BASE_URL should end with /api/v2/",
            OpenFoodFactsConfig.V2_BASE_URL.endsWith("/api/v2/")
        )
    }

    @Test
    fun `PATH_SEARCH_V1 should be cgi search pl`() {
        assertEquals("cgi/search.pl", OpenFoodFactsConfig.PATH_SEARCH_V1)
    }

    @Test
    fun `PATH_PRODUCT_V0 should be product barcode path`() {
        assertTrue(
            "PATH_PRODUCT_V0 should contain {barcode}",
            OpenFoodFactsConfig.PATH_PRODUCT_V0.contains("{barcode}")
        )
    }

    @Test
    fun `DEFAULT_PAGE_SIZE should be positive`() {
        assertTrue(OpenFoodFactsConfig.DEFAULT_PAGE_SIZE > 0)
    }

    @Test
    fun `MAX_RESULTS should be positive`() {
        assertTrue(OpenFoodFactsConfig.MAX_RESULTS > 0)
    }

    @Test
    fun `CACHE_TTL_MILLIS should be positive`() {
        assertTrue(OpenFoodFactsConfig.CACHE_TTL_MILLIS > 0)
    }

    @Test
    fun `CGI_FIELDS should contain nutriments`() {
        assertTrue(
            "CGI_FIELDS must include nutriments for caffeine filtering",
            OpenFoodFactsConfig.CGI_FIELDS.contains("nutriments")
        )
    }

    @Test
    fun `V2_FIELDS should contain product_name`() {
        assertTrue(
            "V2_FIELDS must include product_name",
            OpenFoodFactsConfig.V2_FIELDS.contains("product_name")
        )
    }

    // ── Cross-module consistency ────────────────────────────

    @Test
    fun `cross_module host must match wear module host`() {
        // This ensures both modules use the exact same canonical hostname.
        // When the :common module is extracted (issues #1.1-#1.7),
        // this config will be shared directly instead of duplicated.
        assertEquals(
            "Both modules MUST use the same HOST",
            "world.openfoodfacts.org",
            OpenFoodFactsConfig.HOST
        )
    }

    @Test
    fun `cross_module both URLs resolve to same host`() {
        val rootHost = OpenFoodFactsConfig.ROOT_BASE_URL.removePrefix("https://")
            .removeSuffix("/")
        val v2Host = OpenFoodFactsConfig.V2_BASE_URL.removePrefix("https://")
            .substringBefore("/")
        assertEquals(
            "Both ROOT and V2 URLs must use the same host",
            rootHost, v2Host
        )
    }
}
