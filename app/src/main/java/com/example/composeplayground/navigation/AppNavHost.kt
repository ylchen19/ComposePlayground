package com.example.composeplayground.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.composeplayground.ui.screen.DetailScreen
import com.example.composeplayground.ui.screen.HomeScreen
import com.example.composeplayground.ui.screen.SettingsScreen

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Home)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Home> {
                HomeScreen(
                    onNavigateToDetail = { id -> backStack.add(Detail(id)) },
                    onNavigateToSettings = { backStack.add(Settings) },
                )
            }
            entry<Detail> { key ->
                DetailScreen(
                    itemId = key.itemId,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
            entry<Settings> {
                SettingsScreen(
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
