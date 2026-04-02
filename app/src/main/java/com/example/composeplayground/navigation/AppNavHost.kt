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
                val viewModel = koinViewModel<PokemonListViewModel>()
                PokemonListScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id -> backStack.add(PokemonDetail(id)) },
                    onNavigateToSettings = { backStack.add(Settings) },
                )
            }
            entry<PokemonDetail> { key ->
                val viewModel = koinViewModel<PokemonDetailViewModel>(
                    parameters = { parametersOf(key.pokemonId) },
                )
                PokemonDetailScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
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
