package pl.dekrate.kofeino.data.remote

import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Unit tests for [CustomDns].
 *
 * Uses property injection to mock the DNS delegate,
 * avoiding coupling to Robolectric shadows or global mockkStatic.
 */
@RunWith(RobolectricTestRunner::class)
class CustomDnsTest {

    private lateinit var customDns: CustomDns
    private lateinit var mockDelegate: okhttp3.Dns

    @Before
    fun setup() {
        customDns = CustomDns()
        mockDelegate = mockk()
    }

    @After
    fun teardown() {
        // Reset delegate to system DNS
        customDns.dnsDelegate = okhttp3.Dns.SYSTEM
    }

    @Test
    fun `lookup delegates to system DNS and returns addresses`() {
        val hostname = "example.com"
        val addresses = customDns.lookup(hostname)

        assertTrue("System DNS should resolve to at least one address", addresses.isNotEmpty())
    }

    @Test
    fun `lookup returns addresses from delegate when system DNS succeeds`() {
        val hostname = "test.example"
        val expectedAddress = InetAddress.getByName("192.0.2.1")
        every { mockDelegate.lookup(hostname) } returns listOf(expectedAddress)
        customDns.dnsDelegate = mockDelegate

        val result = customDns.lookup(hostname)

        assertEquals(1, result.size)
        assertEquals(expectedAddress, result.first())
    }

    @Test(expected = UnknownHostException::class)
    fun `lookup throws when delegate fails and BuildConfig DEBUG is false`() {
        every { mockDelegate.lookup(any()) } throws UnknownHostException("Simulated DNS failure")
        customDns.dnsDelegate = mockDelegate

        // This test runs in debug mode by default (Robolectric test),
        // but the delegate failure should propagate when debugLookup also fails
        customDns.lookup("any-host-that-fails.test")
    }

    @Test
    fun `lookup returns non-empty result for loopback host`() {
        // Robolectric's shadow DNS resolves localhost without network
        val addresses = customDns.lookup("localhost")
        assertNotNull("localhost should resolve", addresses)
        assertTrue("localhost should have at least one address", addresses.isNotEmpty())
    }
}
