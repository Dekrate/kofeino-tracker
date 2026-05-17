package pl.dekrate.kofeino.tracker.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network connectivity status.
 */
enum class ConnectivityStatus {
    DISCONNECTED,
    CELLULAR,
    WIFI,
    OTHER
}

/**
 * Observes internet connectivity state.
 *
 * Emits changes via [status] StateFlow and [observeConnectivity] Flow.
 * Phone-specific: uses [ConnectivityManager] directly (no wear-specific APIs).
 */
@Singleton
class ConnectivityObserver @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _status = MutableStateFlow(currentConnectivityStatus())
    val status: StateFlow<ConnectivityStatus> = _status.asStateFlow()

    /** Whether any internet connection is available. */
    val isOnline: Boolean
        get() = _status.value != ConnectivityStatus.DISCONNECTED

    /** Flow emitting connectivity changes using callbackFlow. */
    val observeConnectivity: Flow<ConnectivityStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Timber.d("Network available: $network")
                val newStatus = currentConnectivityStatus()
                _status.value = newStatus
                trySend(newStatus)
            }

            override fun onLost(network: Network) {
                Timber.d("Network lost: $network")
                val newStatus = currentConnectivityStatus()
                _status.value = newStatus
                trySend(newStatus)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val newStatus = parseCapabilities(capabilities)
                _status.value = newStatus
                trySend(newStatus)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        // Emit initial state
        val initial = currentConnectivityStatus()
        _status.value = initial
        trySend(initial)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    private fun currentConnectivityStatus(): ConnectivityStatus {
        val network = connectivityManager.activeNetwork ?: return ConnectivityStatus.DISCONNECTED
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return ConnectivityStatus.DISCONNECTED
        return parseCapabilities(capabilities)
    }

    private fun parseCapabilities(capabilities: NetworkCapabilities): ConnectivityStatus {
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                ConnectivityStatus.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                ConnectivityStatus.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ->
                ConnectivityStatus.OTHER
            else -> ConnectivityStatus.DISCONNECTED
        }
    }
}
