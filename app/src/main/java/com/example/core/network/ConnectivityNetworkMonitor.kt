package com.example.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Android system ConnectivityManager implementation of NetworkMonitor.
 */
class ConnectivityNetworkMonitor(
    private val context: Context
) : NetworkMonitor {

    private val connectivityManager: ConnectivityManager? = context.getSystemService()

    override val isOnline: Flow<Boolean> = callbackFlow {
        val cm = connectivityManager
        if (cm == null) {
            trySend(true)
            close()
            return@callbackFlow
        }

        val networksWithInternet = mutableSetOf<Network>()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val capabilities = cm.getNetworkCapabilities(network)
                val hasInternet = capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (hasInternet) {
                    networksWithInternet.add(network)
                    trySend(true)
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (hasInternet) {
                    networksWithInternet.add(network)
                } else {
                    networksWithInternet.remove(network)
                }
                trySend(networksWithInternet.isNotEmpty())
            }

            override fun onLost(network: Network) {
                networksWithInternet.remove(network)
                trySend(networksWithInternet.isNotEmpty())
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        // Emit current state immediately
        trySend(isCurrentlyOnline)

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            cm.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            trySend(isCurrentlyOnline)
        }

        awaitClose {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                // Callback already unregistered
            }
        }
    }
        .distinctUntilChanged()
        .conflate()

    override val isCurrentlyOnline: Boolean
        get() {
            val cm = connectivityManager ?: return true
            return try {
                val activeNetwork = cm.activeNetwork ?: return false
                val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } catch (e: Exception) {
                true
            }
        }
}
