package com.example.composeplayground.ui.screen.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.composeplayground.R
import com.example.composeplayground.data.model.Album
import com.example.composeplayground.data.model.RecommendationReason
import com.example.composeplayground.ui.screen.music.components.AlbumSwipeCardStack
import com.example.composeplayground.ui.screen.music.components.GenreExclusionRow
import com.example.composeplayground.ui.screen.music.components.LikedAlbumsSheet
import com.example.composeplayground.ui.screen.music.components.RegionExclusionRow
import com.example.composeplayground.ui.screen.music.components.SwipeDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumRouletteScreen(
    viewModel: AlbumRouletteViewModel,
    onBack: () -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val openAlbum: (Album) -> Unit = { album -> album.appleMusicUrl?.let(onOpenExternalUrl) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.music_roulette_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::resetTaste,
                        enabled = uiState.tasteObservationCount > 0,
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = stringResource(R.string.music_roulette_reset_taste),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    IconButton(onClick = viewModel::openLikedSheet) {
                        BadgedBox(
                            badge = {
                                if (uiState.likedAlbums.isNotEmpty()) {
                                    Badge { Text(uiState.likedAlbums.size.toString()) }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.music_roulette_liked_title),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            GenreExclusionRow(
                genres = uiState.availableGenres,
                excludedGenreIds = uiState.excludedGenreIds,
                onToggleGenre = viewModel::toggleGenreExclusion,
                onClearAll = viewModel::clearGenreExclusions,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            RegionExclusionRow(
                excludedRegions = uiState.excludedRegions,
                onToggleRegion = viewModel::toggleRegionExclusion,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            Box(modifier = Modifier.weight(1f)) {
                val topAlbum = uiState.topAlbum
                when {
                    uiState.isLoading -> CenteredProgress()
                    uiState.errorMessage != null -> RouletteMessage(
                        message = uiState.errorMessage.orEmpty(),
                        isError = true,
                        actionLabel = stringResource(R.string.music_action_retry),
                        onAction = viewModel::retry,
                    )
                    topAlbum != null -> AlbumSwipeCardStack(
                        album = topAlbum,
                        nextAlbum = uiState.nextAlbum,
                        onSwiped = { direction ->
                            when (direction) {
                                SwipeDirection.Skip -> viewModel.skipTop()
                                SwipeDirection.Like -> viewModel.likeTop()
                            }
                        },
                        onOpenAlbum = openAlbum,
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> RouletteMessage(
                        message = stringResource(
                            if (uiState.excludedGenreIds.isEmpty()) {
                                R.string.music_roulette_deck_finished
                            } else {
                                R.string.music_roulette_all_excluded
                            },
                        ),
                        isError = false,
                        actionLabel = stringResource(R.string.music_roulette_reshuffle),
                        onAction = viewModel::reshuffle,
                    )
                }
            }

            RecommendationReasonLabel(reason = uiState.recommendationReason)

            SwipeActionBar(
                enabled = uiState.topAlbum != null,
                onSkip = viewModel::skipTop,
                onReshuffle = viewModel::reshuffle,
                onLike = viewModel::likeTop,
            )
        }
    }

    if (uiState.showLikedSheet) {
        LikedAlbumsSheet(
            albums = uiState.likedAlbums,
            onDismiss = viewModel::closeLikedSheet,
            onOpenAlbum = openAlbum,
            onRemove = viewModel::unlike,
        )
    }
}

// ── Private composables ──────────────────────────────────────────────────────

/** 讓推薦演算法的判斷可見——沒有理由時不佔版面高度，避免卡片區抖動。 */
@Composable
private fun RecommendationReasonLabel(reason: RecommendationReason?) {
    if (reason == null) return
    Text(
        text = when (reason) {
            is RecommendationReason.FavoriteGenre ->
                stringResource(R.string.music_roulette_reason_genre, reason.label)
            is RecommendationReason.FavoriteArtist ->
                stringResource(R.string.music_roulette_reason_artist, reason.name)
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        textAlign = TextAlign.Center,
    )
}

/** 與左右滑等價的按鈕操作，同時也是不便做拖曳手勢時的無障礙路徑。 */
@Composable
private fun SwipeActionBar(
    enabled: Boolean,
    onSkip: () -> Unit,
    onReshuffle: () -> Unit,
    onLike: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onSkip,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.music_roulette_skip))
        }
        FilledTonalButton(onClick = onReshuffle) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = stringResource(R.string.music_roulette_reshuffle),
                modifier = Modifier.size(18.dp),
            )
        }
        Button(
            onClick = onLike,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.music_roulette_like))
        }
    }
}

@Composable
private fun CenteredProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun RouletteMessage(
    message: String,
    isError: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(actionLabel)
            }
        }
    }
}
