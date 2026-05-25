package pl.dekrate.kofeino.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenFoodFactsConfigTest {
    @Test fun `host should be world openfoodfacts org`() {
        assertEquals("world.openfoodfacts.org", OpenFoodFactsConfig.HOST)
    }
    @Test fun `HOST should not have protocol prefix`() {
        assertFalse(OpenFoodFactsConfig.HOST.startsWith("https://"))
        assertFalse(OpenFoodFactsConfig.HOST.startsWith("http://"))
    }
    @Test fun `HOST should not have trailing slash`() {
        assertFalse(OpenFoodFactsConfig.HOST.endsWith("/"))
    }
    @Test fun `ROOT_BASE_URL should be valid`() {
        assertTrue(OpenFoodFactsConfig.ROOT_BASE_URL.startsWith("https://"))
        assertTrue(OpenFoodFactsConfig.ROOT_BASE_URL.contains(OpenFoodFactsConfig.HOST))
        assertEquals("https://${OpenFoodFactsConfig.HOST}", OpenFoodFactsConfig.ROOT_BASE_URL)
    }
    @Test fun `V2_BASE_URL should be valid`() {
        assertTrue(OpenFoodFactsConfig.V2_BASE_URL.startsWith("https://"))
        assertTrue(OpenFoodFactsConfig.V2_BASE_URL.contains(OpenFoodFactsConfig.HOST))
        assertTrue(OpenFoodFactsConfig.V2_BASE_URL.endsWith("/api/v2/"))
    }
    @Test fun `both URLs should use same host`() {
        val rootHost = OpenFoodFactsConfig.ROOT_BASE_URL.removePrefix("https://").substringBefore("/")
        val v2Host = OpenFoodFactsConfig.V2_BASE_URL.removePrefix("https://").substringBefore("/")
        assertEquals(rootHost, v2Host)
    }
    @Test fun `paths should not be blank`() {
        assertTrue(OpenFoodFactsConfig.PATH_SEARCH_V2.isNotBlank())
        assertTrue(OpenFoodFactsConfig.PATH_CATEGORY_SEARCH.isNotBlank())
    }
    @Test fun `PATH_PRODUCT_V2 should have barcode placeholder`() {
        assertTrue(OpenFoodFactsConfig.PATH_PRODUCT_V2.contains("{barcode}"))
    }
    @Test fun `MAX_RESULTS should be positive`() {
        assertTrue(OpenFoodFactsConfig.MAX_RESULTS > 0)
    }
    @Test fun `CACHE_TTL_MILLIS should be 1 hour`() {
        assertEquals(60 * 60 * 1000L, OpenFoodFactsConfig.CACHE_TTL_MILLIS)
    }
    @Test fun `V2_FIELDS should contain required fields`() {
        assertTrue(OpenFoodFactsConfig.V2_FIELDS.contains("product_name"))
        assertTrue(OpenFoodFactsConfig.V2_FIELDS.contains("nutriments"))
        assertTrue(OpenFoodFactsConfig.V2_FIELDS.contains("code"))
    }
    @Test fun `HTTP status codes should be correct`() {
        assertEquals(429, OpenFoodFactsConfig.HTTP_TOO_MANY_REQUESTS)
        assertEquals(503, OpenFoodFactsConfig.HTTP_SERVICE_UNAVAILABLE)
    }
    @Test fun `rate limit constants should be positive`() {
        assertTrue(OpenFoodFactsConfig.RATE_LIMIT_CIRCUIT_BREAKER_THRESHOLD > 0)
        assertTrue(OpenFoodFactsConfig.RATE_LIMIT_RETRY_DELAY_MS > 0)
    }
    @Test fun `PATH_SEARCH_V1 should be correct`() {
        assertEquals("cgi/search.pl", OpenFoodFactsConfig.PATH_SEARCH_V1)
        assertTrue(OpenFoodFactsConfig.PATH_SEARCH_V1.isNotBlank())
    }
    @Test fun `PATH_SEARCH_V1_FROM_V2 should be relative path`() {
        assertEquals("../../cgi/search.pl", OpenFoodFactsConfig.PATH_SEARCH_V1_FROM_V2)
        assertTrue(OpenFoodFactsConfig.PATH_SEARCH_V1_FROM_V2.startsWith("../"))
    }
    @Test fun `PATH_PRODUCT_V0 should have barcode placeholder`() {
        assertEquals("api/v0/product/{barcode}.json", OpenFoodFactsConfig.PATH_PRODUCT_V0)
        assertTrue(OpenFoodFactsConfig.PATH_PRODUCT_V0.contains("{barcode}"))
    }
    @Test fun `CGI_FIELDS should contain required fields`() {
        assertEquals("code,product_name,brands,product_quantity,nutriments", OpenFoodFactsConfig.CGI_FIELDS)
        assertTrue(OpenFoodFactsConfig.CGI_FIELDS.contains("product_name"))
        assertTrue(OpenFoodFactsConfig.CGI_FIELDS.contains("nutriments"))
        assertTrue(OpenFoodFactsConfig.CGI_FIELDS.contains("code"))
        assertTrue(OpenFoodFactsConfig.CGI_FIELDS.contains("product_quantity"))
    }
    @Test fun `CGI_PAGE_SIZE should be positive`() {
        assertEquals(25, OpenFoodFactsConfig.CGI_PAGE_SIZE)
        assertTrue(OpenFoodFactsConfig.CGI_PAGE_SIZE > 0)
    }
}
