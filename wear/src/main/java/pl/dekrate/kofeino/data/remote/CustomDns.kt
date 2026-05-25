package pl.dekrate.kofeino.data.remote

import androidx.annotation.VisibleForTesting
import okhttp3.Dns
import pl.dekrate.kofeino.BuildConfig
import timber.log.Timber
import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure DNS resolver for OkHttp.
 *
 * Uses [Dns.SYSTEM] (Android system DNS) as the primary resolver,
 * respecting DNS-over-TLS/HTTPS and other system security features.
 *
 * On debug builds, if system DNS fails, a secondary attempt is made
 * using [InetAddress.getAllByName] as a lightweight retry.
 * This handles transient failures without compromising security.
 *
 * All hardcoded IP addresses and raw UDP DNS packet construction
 * have been removed. This is a pure delegation-based resolver.
 */
@Singleton
class CustomDns @Inject constructor() : Dns {

    /**
     * The actual DNS resolver to delegate to.
     * Exposed for testing — production uses [Dns.SYSTEM].
     */
    @VisibleForTesting
    internal var dnsDelegate: Dns = Dns.SYSTEM

    override fun lookup(hostname: String): List<InetAddress> {
        try {
            val addresses = dnsDelegate.lookup(hostname)
            Timber.d(
                "CustomDns: resolved %s -> %d addresses via system DNS",
                hostname,
                addresses.size
            )
            return addresses
        } catch (e: UnknownHostException) {
            Timber.w(e, "CustomDns: system DNS failed for %s", hostname)
            if (BuildConfig.DEBUG) {
                return debugLookup(hostname)
            }
            throw e
        }
    }

    /**
     * Debug-only fallback that retries resolution via the standard
     * Java API. On an emulator this routes through the emulator's
     * built-in DNS proxy, which may succeed when system DNS has a
     * transient issue in restricted network environments.
     */
    private fun debugLookup(hostname: String): List<InetAddress> {
        try {
            val addresses = InetAddress.getAllByName(hostname)
            Timber.d(
                "CustomDns: resolved %s via InetAddress.getAllByName (debug fallback)",
                hostname
            )
            return addresses.toList()
        } catch (e: UnknownHostException) {
            Timber.w(e, "CustomDns: debug fallback also failed for %s", hostname)
            throw e
        }
    }
}
