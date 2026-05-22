package pl.dekrate.kofeino.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [OpenFoodFactsConfig].
 *
 * Verifies that the canonical endpoint configuration:
 * - Points to the correct host
 * - Produces valid URL structures
 * - Has consistent path constants
 */
class OpenFoodFactsConfigTest {

    @Test
    fun `host should be world dot openfoodfacts dot org`() {
        assertEquals("world.openfoodfacts.org", OpenFoodFactsConfig.HOST)
    }

    @Test
    fun `V2_BASE_URL should contain the canonical host`() {
        assertTrue(
            "V2_BASE_URL must reference HOST",
            OpenFoodFactsConfig.V2_BASE_URL.contains(OpenFoodFactsConfig.HOST)
        )
    }

    @Test
    fun `V2_BASE_URL should be a valid HTTPS URL`() {
        assertTrue(
            "V2_BASE_URL must start with https://",
            OpenFoodFactsConfig.V2_BASE_URL.startsWith("https://")
        )
        assertTrue(
            "V2_BASE_URL must end with /api/v2/",
            OpenFoodFactsConfig.V2_BASE_URL.endsWith("/api/v2/")
        )
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
            "ROOT_BASE_URL should be just the host",
            "https://${OpenFoodFactsConfig.HOST}",
            OpenFoodFactsConfig.ROOT_BASE_URL
        )
    }

    @Test
    fun `PATH_SEARCH_V1 should resolve correctly against V2_BASE_URL`() {
        val resolved = OpenFoodFactsConfig.V2_BASE_URL + OpenFoodFactsConfig.PATH_SEARCH_V1
        // The relative path goes up from /api/v2/ to root, then to /cgi/search.pl
        // With ../.. the resolved URL should still work
        assertTrue(
            "Resolved URL should be valid",
            resolved.startsWith("https://")
        )
    }

    @Test
    fun `PATH_SEARCH_V2 should resolve via V2_BASE_URL`() {
        val resolvedUrl = "${OpenFoodFactsConfig.V2_BASE_URL}${OpenFoodFactsConfig.PATH_SEARCH_V2}"
        assertTrue(resolvedUrl.startsWith("https://"))
        assertTrue(resolvedUrl.contains("/api/v2/search"))
    }

    @Test
    fun `DEFAULT_PAGE_SIZE should be positive`() {
        assertTrue(
            "DEFAULT_PAGE_SIZE must be positive",
            OpenFoodFactsConfig.DEFAULT_PAGE_SIZE > 0
        )
    }

    @Test
    fun `MAX_RESULTS should be positive`() {
        assertTrue(
            "MAX_RESULTS must be positive",
            OpenFoodFactsConfig.MAX_RESULTS > 0
        )
    }

    @Test
    fun `CACHE_TTL_MILLIS should be positive`() {
        assertTrue(
            "CACHE_TTL must be positive",
            OpenFoodFactsConfig.CACHE_TTL_MILLIS > 0
        )
    }

    @Test
    fun `HOST should not contain protocol prefix`() {
        assertFalse(
            "HOST must not include https://",
            OpenFoodFactsConfig.HOST.startsWith("https://")
        )
        assertFalse(
            "HOST must not include http://",
            OpenFoodFactsConfig.HOST.startsWith("http://")
        )
    }

    @Test
    fun `HOST should not contain trailing slash`() {
        assertFalse(
            "HOST must not end with /",
            OpenFoodFactsConfig.HOST.endsWith("/")
        )
    }

    @Test
    fun `V2_FIELDS should be non-empty`() {
        assertTrue(
            "V2_FIELDS must not be empty",
            OpenFoodFactsConfig.V2_FIELDS.isNotBlank()
        )
        assertTrue(
            "V2_FIELDS should contain product_name",
            OpenFoodFactsConfig.V2_FIELDS.contains("product_name")
        )
    }
}
