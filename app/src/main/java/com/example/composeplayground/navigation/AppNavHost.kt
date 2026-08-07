package com.example.composeplayground.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.example.composeplayground.ui.screen.HomeMenuScreen
import com.example.composeplayground.ui.screen.camera.CameraScreen
import com.example.composeplayground.ui.screen.camera.CameraViewModel
import com.example.composeplayground.ui.screen.SettingsScreen
import com.example.composeplayground.ui.screen.anime.AnimeDetailScreen
import com.example.composeplayground.ui.screen.anime.AnimeDetailViewModel
import com.example.composeplayground.ui.screen.anime.AnimeListScreen
import com.example.composeplayground.ui.screen.anime.AnimeListViewModel
import com.example.composeplayground.ui.screen.music.AlbumRouletteScreen
import com.example.composeplayground.ui.screen.music.AlbumRouletteViewModel
import com.example.composeplayground.ui.screen.music.MusicDetailScreen
import com.example.composeplayground.ui.screen.music.MusicDetailViewModel
import com.example.composeplayground.ui.screen.music.MusicSearchScreen
import com.example.composeplayground.ui.screen.music.MusicSearchViewModel
import com.example.composeplayground.data.model.Track
import com.example.composeplayground.ui.screen.picsum.PicsumDetailScreen
import com.example.composeplayground.ui.screen.picsum.PicsumDetailViewModel
import com.example.composeplayground.ui.screen.picsum.PicsumGalleryScreen
import com.example.composeplayground.ui.screen.picsum.PicsumGalleryViewModel
import com.example.composeplayground.ui.screen.pokemon.PokemonDetailScreen
import com.example.composeplayground.ui.screen.pokemon.PokemonDetailViewModel
import com.example.composeplayground.ui.screen.pokemon.PokemonListScreen
import com.example.composeplayground.ui.screen.pokemon.PokemonListViewModel
import com.example.composeplayground.ui.screen.pokemon.PokemonTypeGalleryScreen
import com.example.composeplayground.ui.screen.pokemon.PokemonTypeGalleryViewModel
import com.example.composeplayground.ui.screen.settings.GeminiNanoSettingsViewModel
import com.example.composeplayground.ui.theme.ThemeViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf


/**
 * 應用程式的頂層導航容器，使用 Navigation3 的 [NavDisplay] 管理整個頁面堆疊。
 *
 * ## 架構說明
 * - **back stack**：以 [rememberNavBackStack] 建立，初始目的地為 [Home]（菜單頁），狀態在重組間持久保留
 * - **entryDecorators**：兩個裝飾器按順序套用：
 *   1. [rememberSaveableStateHolderNavEntryDecorator]：讓各頁面的 `rememberSaveable` 狀態在離開後仍可復原
 *   2. [rememberViewModelStoreNavEntryDecorator]：將 ViewModel 的生命週期綁定至各自的 NavEntry，頁面從 back stack 移除時 ViewModel 一併清除
 * - **entryProvider**：以型別安全的 `entry<Key>` DSL 對應每個 [NavKey] 與其 Composable 內容
 * - **navigate throttle**：NavDisplay 預設以 700ms fade 做頁面轉場，轉場期間離開的頁面仍在
 *   composition 中且可接受點擊事件（click-through）。所有 back stack 操作統一透過 [navigate]
 *   包裝，觀察 [LocalNavAnimatedContentScope] 的 `transition.isRunning` 狀態，在轉場進行中時
 *   封鎖後續的導航呼叫，防止跨圖庫誤觸，無需依賴固定延遲秒數。
 *
 * ## 導航規則
 * - 前往新頁面：`navigate { backStack.add(Key) }`
 * - 返回上一頁：`navigate { backStack.removeLastOrNull() }`
 * - Screen composable 僅接收事件回呼，不直接持有 back stack 引用，保持 UI 層與導航邏輯分離
 */
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Home)

    // Observe NavDisplay's actual transition state via LocalNavAnimatedContentScope.
    // During the enter/exit animation, transition.isRunning == true, so navigate() is blocked.
    // This avoids any hardcoded delay — the gate lifts exactly when the animation settles.
    val isTransitioning = remember { mutableStateOf(false) }
    val transitionTrackingDecorator: NavEntryDecorator<Any> = remember {
        NavEntryDecorator { entry ->
            val transitioning = LocalNavAnimatedContentScope.current.transition.isRunning
            SideEffect { isTransitioning.value = transitioning }
            entry.Content()
        }
    }
    val navigate: (() -> Unit) -> Unit = remember {
        { action -> if (!isTransitioning.value) action() }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryDecorators = listOf(
            transitionTrackingDecorator,
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Home> {
                HomeMenuScreen(
                    onNavigateToPokemon = { navigate { backStack.add(PokemonHome) } },
                    onNavigateToPicsum = { navigate { backStack.add(PicsumGallery) } },
                    onNavigateToAnime = { navigate { backStack.add(AnimeHome) } },
                    onNavigateToCamera = { navigate { backStack.add(CameraHome) } },
                    onNavigateToMusic = { navigate { backStack.add(MusicHome) } },
                    onNavigateToSettings = { navigate { backStack.add(Settings) } },
                )
            }
            entry<PokemonHome> {
                val viewModel = koinViewModel<PokemonListViewModel>()
                PokemonListScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id -> navigate { backStack.add(PokemonDetail(id)) } },
                    onNavigateToSettings = { navigate { backStack.add(Settings) } },
                    onNavigateToTypeGallery = { navigate { backStack.add(PokemonTypeGallery) } },
                )
            }
            entry<PokemonDetail> { key ->
                val viewModel = koinViewModel<PokemonDetailViewModel>(
                    parameters = { parametersOf(key.pokemonId) },
                )
                PokemonDetailScreen(
                    viewModel = viewModel,
                    onBack = { navigate { backStack.removeLastOrNull() } },
                    onNavigateToPokemon = { id -> navigate { backStack.add(PokemonDetail(id)) } },
                )
            }
            entry<Settings> {
                val themeViewModel = koinViewModel<ThemeViewModel>()
                val geminiNanoVm = koinViewModel<GeminiNanoSettingsViewModel>()
                SettingsScreen(
                    themeViewModel = themeViewModel,
                    geminiNanoVm = geminiNanoVm,
                    onBack = { navigate { backStack.removeLastOrNull() } },
                )
            }
            entry<PokemonTypeGallery> {
                val viewModel = koinViewModel<PokemonTypeGalleryViewModel>()
                PokemonTypeGalleryScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id -> navigate { backStack.add(PokemonDetail(id)) } },
                    onBack = { navigate { backStack.removeLastOrNull() } },
                )
            }
            entry<PicsumGallery> {
                val viewModel = koinViewModel<PicsumGalleryViewModel>()
                PicsumGalleryScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { photo ->
                        navigate {
                            backStack.add(
                                PicsumDetail(
                                    photoId = photo.id,
                                    author = photo.author,
                                    originalWidth = photo.originalWidth,
                                    originalHeight = photo.originalHeight,
                                ),
                            )
                        }
                    },
                    onBack = { navigate { backStack.removeLastOrNull() } },
                )
            }
            entry<PicsumDetail> { key ->
                val viewModel = koinViewModel<PicsumDetailViewModel>(
                    parameters = {
                        parametersOf(
                            key.photoId,
                            key.author,
                            key.originalWidth,
                            key.originalHeight,
                        )
                    },
                )
                PicsumDetailScreen(
                    viewModel = viewModel,
                    onBack = { navigate { backStack.removeLastOrNull() } },
                )
            }
            entry<AnimeHome> {
                val viewModel = koinViewModel<AnimeListViewModel>()
                AnimeListScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id -> navigate { backStack.add(AnimeDetail(id)) } },
                    onBack = { navigate { backStack.removeLastOrNull() } },
                )
            }
            entry<CameraHome> {
                val viewModel = koinViewModel<CameraViewModel>()
                CameraScreen(
                    viewModel = viewModel,
                    onBack = { navigate { backStack.removeLastOrNull() } },
                )
            }
            entry<AnimeDetail> { key ->
                val viewModel = koinViewModel<AnimeDetailViewModel>(
                    parameters = { parametersOf(key.animeId) },
                )
                AnimeDetailScreen(
                    viewModel = viewModel,
                    onBack = { navigate { backStack.removeLastOrNull() } },
                    onNavigateToAnime = { id -> navigate { backStack.add(AnimeDetail(id)) } },
                )
            }
            entry<MusicHome> {
                val viewModel = koinViewModel<MusicSearchViewModel>()
                MusicSearchScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { track ->
                        navigate {
                            backStack.add(
                                MusicDetail(
                                    id = track.id,
                                    trackName = track.trackName,
                                    artistName = track.artistName,
                                    collectionName = track.collectionName,
                                    artworkUrl = track.artworkUrl,
                                    previewUrl = track.previewUrl,
                                    trackTimeMillis = track.trackTimeMillis,
                                    releaseDate = track.releaseDate,
                                    genre = track.genre,
                                ),
                            )
                        }
                    },
                    onNavigateToRoulette = { navigate { backStack.add(MusicAlbumRoulette) } },
                    onBack = { navigate { backStack.removeLastOrNull() } },
                )
            }
            entry<MusicAlbumRoulette> {
                val context = LocalContext.current
                AlbumRouletteScreen(
                    viewModel = koinViewModel<AlbumRouletteViewModel>(),
                    onBack = { navigate { backStack.removeLastOrNull() } },
                    onOpenExternalUrl = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    },
                )
            }
            entry<MusicDetail> { key ->
                val track = Track(
                    id = key.id,
                    trackName = key.trackName,
                    artistName = key.artistName,
                    collectionName = key.collectionName,
                    artworkUrl = key.artworkUrl,
                    previewUrl = key.previewUrl,
                    trackTimeMillis = key.trackTimeMillis,
                    releaseDate = key.releaseDate,
                    genre = key.genre,
                )
                val viewModel = koinViewModel<MusicDetailViewModel>(
                    parameters = { parametersOf(track) },
                )
                MusicDetailScreen(
                    viewModel = viewModel,
                    onBack = { navigate { backStack.removeLastOrNull() } },
                )
            }
        },
    )
}
