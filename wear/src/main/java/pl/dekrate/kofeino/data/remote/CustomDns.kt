package pl.dekrate.kofeino.data.remote

import okhttp3.Dns
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.UnknownHostException
/**
 * Custom DNS resolver for OkHttp.
 *
 * Sends raw DNS queries over UDP to multiple DNS servers,
 * bypassing the system resolver. Solves DNS failures on
 * emulators in restricted networks (university, corporate).
 *
 * DNS servers tried in order:
 * 1. 10.0.2.3 – emulator DNS proxy (forwards to host)
 * 2. 8.8.8.8  – Google Public DNS
 * 3. 8.8.4.4  – Google Public DNS (secondary)
 *
 * Falls back to hardcoded IP cache, then to system resolver.
 */
object CustomDns : Dns {

    /** Emulator DNS proxy — always reachable from emulator. */
    private val DNS_EMULATOR = InetAddress.getByName("10.0.2.3")
    private val DNS_GOOGLE1 = InetAddress.getByName("8.8.8.8")
    private val DNS_GOOGLE2 = InetAddress.getByName("8.8.4.4")

    /** Hardcoded IPs for known domains — final fallback if all DNS fail. */
    private val HARDCODED_IPS = mapOf(
        "world.openfoodfacts.org" to "151.115.132.10",
        "world-fr.openfoodfacts.org" to "151.115.132.10",
        "openfoodfacts.org" to "151.115.132.10"
    )

    private const val DNS_PORT = 53
    private const val TIMEOUT_MS = 3000
    private const val MAX_PACKET_SIZE = 512

    override fun lookup(hostname: String): List<InetAddress> {
        // 1. Try direct UDP DNS resolution to each server
        val dnsServers = listOf(DNS_EMULATOR, DNS_GOOGLE1, DNS_GOOGLE2)
        for (dns in dnsServers) {
            try {
                val addresses = resolveViaUdp(hostname, dns)
                if (addresses.isNotEmpty()) {
                    Timber.d("CustomDns: resolved $hostname -> ${addresses.size} addrs via UDP ${dns.hostAddress}")
                    return addresses
                }
            } catch (e: Exception) {
                Timber.w(e, "CustomDns: UDP DNS failed for $hostname via ${dns.hostAddress}")
            }
        }

        // 2. Try hardcoded IP cache
        val hardcodedIp = HARDCODED_IPS[hostname]
        if (hardcodedIp != null) {
            try {
                val address = InetAddress.getByName(hardcodedIp)
                Timber.d("CustomDns: resolved $hostname -> $hardcodedIp (hardcoded)")
                return listOf(address)
            } catch (e: Exception) {
                Timber.w(e, "CustomDns: hardcoded IP $hardcodedIp invalid for $hostname")
            }
        }

        // 3. Fallback to system resolver
        try {
            val systemAddresses = InetAddress.getAllByName(hostname).toList()
            if (systemAddresses.isNotEmpty()) {
                Timber.d("CustomDns: resolved $hostname via system")
                return systemAddresses
            }
        } catch (e: UnknownHostException) {
            Timber.w(e, "CustomDns: system resolver failed for $hostname")
        }

        throw UnknownHostException("Unable to resolve: $hostname (UDP DNS + hardcoded + system all failed)")
    }

    private fun resolveViaUdp(hostname: String, dnsServer: InetAddress): List<InetAddress> {
        val query = buildDnsQuery(hostname)

        val socket = DatagramSocket()
        socket.use { sock ->
            sock.soTimeout = TIMEOUT_MS

            val request = DatagramPacket(query, query.size, dnsServer, DNS_PORT)
            sock.send(request)

            val buf = ByteArray(MAX_PACKET_SIZE)
            val response = DatagramPacket(buf, buf.size)
            sock.receive(response)

            return parseDnsResponse(response.data, response.length)
        }
    }

    /** Build a DNS A-record query packet. */
    private fun buildDnsQuery(hostname: String): ByteArray {
        val labels = hostname.split(".")
        // Header (12 bytes) + QNAME variable + QTYPE (2) + QCLASS (2)
        val qname = encodeQName(labels)
        val packet = ByteArray(12 + qname.size + 4)

        // --- Header ---
        // Transaction ID (random)
        val id = (Math.random() * 65535).toInt()
        packet[0] = (id shr 8).toByte()
        packet[1] = id.toByte()

        // Flags: 0x0100 = standard query, recursion desired
        packet[2] = 0x01.toByte()
        packet[3] = 0x00.toByte()

        // QDCOUNT: 1 question
        packet[4] = 0x00
        packet[5] = 0x01

        // ANCOUNT, NSCOUNT, ARCOUNT = 0
        // (bytes 6-11 are already 0)

        // --- Question ---
        qname.copyInto(packet, 12)
        val qnameOffset = 12 + qname.size
        // QTYPE = 1 (A record)
        packet[qnameOffset] = 0x00
        packet[qnameOffset + 1] = 0x01
        // QCLASS = 1 (IN)
        packet[qnameOffset + 2] = 0x00
        packet[qnameOffset + 3] = 0x01

        return packet
    }

    /** Encode a domain name as DNS label sequence. */
    private fun encodeQName(labels: List<String>): ByteArray {
        val parts = mutableListOf<ByteArray>()
        for (label in labels) {
            val bytes = label.toByteArray(Charsets.UTF_8)
            if (bytes.size > 63) throw IllegalArgumentException("Label too long: $label")
            parts.add(byteArrayOf(bytes.size.toByte()))
            parts.add(bytes)
        }
        parts.add(byteArrayOf(0x00)) // root terminator
        return parts.flatMap { it.toList() }.toByteArray()
    }

    /** Parse a DNS response and extract A record IPs. */
    private fun parseDnsResponse(data: ByteArray, length: Int): List<InetAddress> {
        if (length < 12) return emptyList()

        // Check that response is for a standard query
        val flags = ((data[2].toInt() shl 8) or (data[3].toInt() and 0xFF))
        val rcode = flags and 0x0F

        if (rcode != 0) {
            Timber.w("CustomDns: DNS server returned error code $rcode")
            return emptyList()
        }

        val ancount = ((data[6].toInt() shl 8) or (data[7].toInt() and 0xFF))

        // Skip header (12) and question section
        var offset = 12
        val qdcount = ((data[4].toInt() shl 8) or (data[5].toInt() and 0xFF))
        for (i in 0 until qdcount) {
            offset = skipName(data, offset, length)
            if (offset < 0) return emptyList()
            offset += 4 // QTYPE + QCLASS
        }

        val addresses = mutableListOf<InetAddress>()

        // Parse answer records
        for (i in 0 until ancount) {
            offset = skipName(data, offset, length)
            if (offset < 0) return emptyList()

            if (offset + 10 > length) return emptyList()
            val type = ((data[offset].toInt() shl 8) or (data[offset + 1].toInt() and 0xFF))
            val rdlength = ((data[offset + 8].toInt() shl 8) or (data[offset + 9].toInt() and 0xFF))
            offset += 10

            if (offset + rdlength > length) return emptyList()

            if (type == 1 && rdlength == 4) { // A record, IPv4
                val ip = InetAddress.getByAddress(data.copyOfRange(offset, offset + 4))
                addresses.add(ip)
            }

            offset += rdlength
        }

        return addresses
    }

    /**
     * Skip a domain name (handles labels and pointer compression).
     * Returns the new offset, or -1 on error.
     */
    private fun skipName(data: ByteArray, startOffset: Int, length: Int): Int {
        var offset = startOffset
        while (offset < length) {
            val len = data[offset].toInt() and 0xFF
            if (len == 0) {
                return offset + 1
            }
            if ((len and 0xC0) == 0xC0) {
                // Pointer compression (2 bytes total)
                return offset + 2
            }
            // Regular label
            offset += 1 + len
        }
        return -1
    }
}
