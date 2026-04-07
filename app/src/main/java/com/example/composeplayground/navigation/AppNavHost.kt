package com.example.composeplayground.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.composeplayground.ui.screen.SettingsScreen
import com.example.composeplayground.ui.screen.pokemon.PokemonDetailScreen
import com.example.composeplayground.ui.screen.pokemon.PokemonDetailViewModel
import com.example.composeplayground.ui.screen.pokemon.PokemonListScreen
import com.example.composeplayground.ui.screen.pokemon.PokemonListViewModel
import com.example.composeplayground.ui.theme.ThemeViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 應用程式的頂層導航容器，使用 Navigation3 的 [NavDisplay] 管理整個頁面堆疊。
 *
 * ## 架構說明
 * - **back stack**：以 [rememberNavBackStack] 建立，初始目的地為 [Home]，狀態在重組間持久保留
 * - **entryDecorators**：兩個裝飾器按順序套用：
 *   1. [rememberSaveableStateHolderNavEntryDecorator]：讓各頁面的 `rememberSaveable` 狀態在離開後仍可復原
 *   2. [rememberViewModelStoreNavEntryDecorator]：將 ViewModel 的生命週期綁定至各自的 NavEntry，頁面從 back stack 移除時 ViewModel 一併清除
 * - **entryProvider**：以型別安全的 `entry<Key>` DSL 對應每個 [NavKey] 與其 Composable 內容
 *
 * ## 導航規則
 * - 前往新頁面：`backStack.add(Key)`
 * - 返回上一頁：`backStack.removeLastOrNull()`（[Home] 為底，移除後 back stack 為空由系統處理退出）
 * - Screen composable 僅接收事件回呼，不直接持有 back stack 引用，保持 UI 層與導航邏輯分離
 */
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    // 以 Home 為初始目的地建立 back stack，狀態跨重組持久保留
    val backStack = rememberNavBackStack(Home)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryDecorators = listOf(
            // 保留各頁面 rememberSaveable 狀態（如滾動位置、輸入內容）
            rememberSaveableStateHolderNavEntryDecorator(),
            // 將 ViewModel 生命週期綁定至 NavEntry，頁面移除時自動清除
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            // 首頁：Pokémon 列表
            entry<Home> {
                val viewModel = koinViewModel<PokemonListViewModel>()
                PokemonListScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id -> backStack.add(PokemonDetail(id)) },
                    onNavigateToSettings = { backStack.add(Settings) },
                )
            }
            // 詳細頁：透過 key.pokemonId 以 parametersOf 注入 ViewModel
            entry<PokemonDetail> { key ->
                val viewModel = koinViewModel<PokemonDetailViewModel>(
                    parameters = { parametersOf(key.pokemonId) },
                )
                PokemonDetailScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
            // 設定頁：主題切換等偏好設定
            entry<Settings> {
                val themeViewModel = koinViewModel<ThemeViewModel>()
                SettingsScreen(
                    themeViewModel = themeViewModel,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
