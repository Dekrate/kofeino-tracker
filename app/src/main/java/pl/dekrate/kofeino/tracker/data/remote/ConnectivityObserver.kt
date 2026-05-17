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
 * The NetworkCallback is registered eagerly in [init] so that [status]
 * and [isOnline] remain current regardless of whether any caller collects
 * [observeConnectivity].
 *
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

    /** Shared callback that updates [_status] on connectivity changes. */
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.d("Network available: $network")
            _status.value = currentConnectivityStatus()
        }

        override fun onLost(network: Network) {
            Timber.d("Network lost: $network")
            _status.value = currentConnectivityStatus()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities
        ) {
            _status.value = parseCapabilities(capabilities)
        }
    }

    /** Eager registration — keeps [_status] and [isOnline] up to date. */
    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    /**
     * Reactive connectivity flow.
     *
     * Delivers the current state on first collection, then emits on every change.
     * The underlying NetworkCallback is already registered in [init]; this Flow
     * simply bridges it to the coroutine world and unregisters on cancellation
     * of **this specific collector** (the shared callback in [init] survives).
     */
    val observeConnectivity: Flow<ConnectivityStatus> = callbackFlow {
        // 1. Emit the current state immediately
        trySend(_status.value)

        // 2. Bridge the shared callback to this Flow
        val bridgeCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(_status.value) }
            override fun onLost(network: Network) { trySend(_status.value) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(_status.value)
            }
        }
        val bridgeRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(bridgeRequest, bridgeCallback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(bridgeCallback)
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
