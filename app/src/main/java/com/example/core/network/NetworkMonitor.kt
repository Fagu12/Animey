package com.example.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interface for observing real-time network connectivity.
 */
interface NetworkMonitor {
    /**
     * Emits true when an active validated network connection is available, false otherwise.
     */
    val isOnline: Flow<Boolean>

    /**
     * Synchronous snapshot check of current network availability.
     */
    val isCurrentlyOnline: Boolean
}

/**
 * In-memory test implementation of NetworkMonitor for unit testing and offline simulation.
 */
class TestNetworkMonitor(initialOnline: Boolean = true) : NetworkMonitor {
    private val _isOnline = MutableStateFlow(initialOnline)
    override val isOnline: Flow<Boolean> = _isOnline.asStateFlow()
    override val isCurrentlyOnline: Boolean get() = _isOnline.value

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }
}
