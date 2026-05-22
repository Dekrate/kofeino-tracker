package pl.dekrate.kofeino.data.remote

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.dekrate.kofeino.data.remote.dto.OpenFoodFactsSearchResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Integration test verifying that Retrofit endpoint resolution
 * works correctly with the configured base URL and path constants.
 *
 * Uses OkHttp MockWebServer to simulate the API without network.
 */
class OpenFoodFactsEndpointIntegrationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: CaffeineApiService

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Create a Retrofit instance pointing to the mock server
        // but using the same path constants from OpenFoodFactsConfig
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/api/v2/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(CaffeineApiService::class.java)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `searchBeveragesWithCaffeine should resolve correct path`() = kotlinx.coroutines.test.runTest {
        // Enqueue a mock response
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"count":2,"products":[{"code":"123","product_name":"Test","nutriments":{"caffeine_100g":0.05}}]}""")
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.searchBeveragesWithCaffeine()

        assertNotNull(response)
        assertEquals(2, response.count)
        assertEquals("Test", response.products[0].productName)

        // Verify the request path
        val request = mockWebServer.takeRequest()
        val requestPath = request.path
        assertTrue(
            "Request path should contain 'search'",
            requestPath?.contains("search") == true
        )
        assertTrue(
            "Request should include caffeine_100g filter",
            requestPath?.contains("caffeine_100g") == true
        )
    }

    @Test
    fun `searchProducts should resolve correct CGI path`() = kotlinx.coroutines.test.runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"count":1,"products":[{"code":"456","product_name":"Coffee","nutriments":{"caffeine_100g":0.06}}]}""")
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.searchProducts(query = "coffee")

        assertNotNull(response)
        assertEquals(1, response.count)

        val request = mockWebServer.takeRequest()
        val requestPath = request.path
        // The CGI path from config is "../../cgi/search.pl" which resolves
        // relative to /api/v2/ → /cgi/search.pl
        assertTrue(
            "Request path should point to cgi/search.pl",
            requestPath?.contains("cgi/search.pl") == true
        )
        assertTrue(
            "Request should include search_terms=coffee",
            requestPath?.contains("search_terms=coffee") == true
        )
    }

    @Test
    fun `getProductByBarcode should resolve correct path`() = kotlinx.coroutines.test.runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"code":"5901234123457","product":{"product_name":"Test Product"}}""")
                .addHeader("Content-Type", "application/json")
        )

        val barcode = "5901234123457"
        val response = apiService.getProductByBarcode(barcode)

        assertNotNull(response)

        val request = mockWebServer.takeRequest()
        val requestPath = request.path
        assertTrue(
            "Request path should contain the barcode",
            requestPath?.contains(barcode) == true
        )
    }

    @Test
    fun `searchBeveragesWithCaffeine should use default page size`() = kotlinx.coroutines.test.runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"count":0,"products":[]}""")
                .addHeader("Content-Type", "application/json")
        )

        apiService.searchBeveragesWithCaffeine()

        val request = mockWebServer.takeRequest()
        assertTrue(
            "Request should include page_size parameter",
            request.path?.contains("page_size") == true
        )
    }

    @Test
    fun `searchProducts with Polish locale should include lc and cc`() = kotlinx.coroutines.test.runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"count":0,"products":[]}""")
                .addHeader("Content-Type", "application/json")
        )

        apiService.searchProducts(query = "herbata", locale = "pl", country = "PL")

        val request = mockWebServer.takeRequest()
        val path = request.path ?: ""
        assertTrue("Request should include lc=pl", path.contains("lc=pl"))
        assertTrue("Request should include cc=PL", path.contains("cc=PL"))
    }
}
