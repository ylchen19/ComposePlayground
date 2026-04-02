package com.example.composeplayground.network.connectivity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ConnectivityObserver {
    val isConnected: StateFlow<Boolean>
    fun observe(): Flow<Status>

    enum class Status { Available, Unavailable, Losing, Lost }
}
