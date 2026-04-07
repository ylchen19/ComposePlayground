package com.example.composeplayground.network.connectivity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 網路連線狀態監聽介面。
 *
 * 提供兩種方式取得連線狀態：
 * - [isConnected]：熱流（StateFlow），可立即讀取目前狀態，適合 UI 層直接 collectAsState
 * - [observe]：冷流（Flow），每次 collect 時才開始監聽，適合需要感知狀態轉換細節的場景
 */
interface ConnectivityObserver {
    /** 目前是否有網際網路連線，初始值由系統當前狀態決定。 */
    val isConnected: StateFlow<Boolean>

    /**
     * 以 [Flow] 持續觀察連線狀態變化，重複值會被 [kotlinx.coroutines.flow.distinctUntilChanged] 過濾。
     *
     * @return 發出 [Status] 事件的 Flow，collect 取消後自動移除系統回呼
     */
    fun observe(): Flow<Status>

    /** 網路連線的四種狀態，對應 [android.net.ConnectivityManager.NetworkCallback] 的回呼。 */
    enum class Status {
        /** 網路可用（onAvailable）。 */
        Available,
        /** 網路完全無法使用（onUnavailable）。 */
        Unavailable,
        /** 網路即將斷線（onLosing），還有剩餘存活時間。 */
        Losing,
        /** 網路已斷線（onLost）。 */
        Lost,
    }
}
