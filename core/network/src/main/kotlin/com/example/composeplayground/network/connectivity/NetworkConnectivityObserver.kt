package com.example.composeplayground.network.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * [ConnectivityObserver] 的 Android 實作，透過 [ConnectivityManager.NetworkCallback] 監聽連線變化。
 *
 * - 建構時立即呼叫 [checkCurrentConnectivity] 取得初始狀態，避免首次 collect 前狀態為 null
 * - [observe] 使用 [callbackFlow] 將回呼橋接為 Flow，並在 collector 取消時自動反註冊，防止記憶體洩漏
 * - 需要在 AndroidManifest 宣告 `ACCESS_NETWORK_STATE` 權限
 *
 * @param context 建議傳入 ApplicationContext，避免 Activity 洩漏
 */
class NetworkConnectivityObserver(
    context: Context,
) : ConnectivityObserver {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableStateFlow(checkCurrentConnectivity())
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    override fun observe(): Flow<ConnectivityObserver.Status> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isConnected.value = true
                trySend(ConnectivityObserver.Status.Available)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                trySend(ConnectivityObserver.Status.Losing)
            }

            override fun onLost(network: Network) {
                _isConnected.value = false
                trySend(ConnectivityObserver.Status.Lost)
            }

            override fun onUnavailable() {
                _isConnected.value = false
                trySend(ConnectivityObserver.Status.Unavailable)
            }
        }

        // 註冊預設網路回呼，系統切換網路時會自動觸發
        connectivityManager.registerDefaultNetworkCallback(callback)

        // Flow 被取消時移除回呼，避免記憶體洩漏
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    /**
     * 同步檢查目前是否有已驗證的網際網路連線。
     *
     * 同時要求 [NetworkCapabilities.NET_CAPABILITY_INTERNET]（宣告有網路）
     * 與 [NetworkCapabilities.NET_CAPABILITY_VALIDATED]（系統已確認能連外網）兩個能力，
     * 避免 Captive Portal（公共 Wi-Fi 驗證頁面）被誤判為有效連線。
     */
    private fun checkCurrentConnectivity(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
