package com.example.composeplayground.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 導航目的地的 Key 定義。
 *
 * 每個 Key 都必須：
 * 1. 標注 [@Serializable]，供 Navigation3 序列化 back stack 狀態使用
 * 2. 實作 [NavKey]，讓 [androidx.navigation3.ui.NavDisplay] 辨識目的地
 *
 * 新增頁面時在此檔案加入對應 Key，再至 [AppNavHost] 的 `entryProvider` 中補上 `entry<Key>` 區塊。
 */

/** 首頁（Pokémon 列表），無需額外參數。 */
@Serializable
data object Home : NavKey

/**
 * Pokémon 詳細資訊頁。
 *
 * @param pokemonId 要顯示的 Pokémon ID，由列表頁傳入，再透過 Koin [parametersOf] 注入至 ViewModel。
 */
@Serializable
data class PokemonDetail(val pokemonId: Int) : NavKey

/** 設定頁（主題切換等偏好設定），無需額外參數。 */
@Serializable
data object Settings : NavKey

/** 依屬性分類的寶可夢圖鑑，每種屬性顯示為一列 LazyRow。 */
@Serializable
data object PokemonTypeGallery : NavKey
