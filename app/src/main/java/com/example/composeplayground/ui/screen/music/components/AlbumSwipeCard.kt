package com.example.composeplayground.ui.screen.music.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.composeplayground.R
import com.example.composeplayground.data.model.Album
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class SwipeDirection { Skip, Like }

/**
 * 兩張一組的卡片堆疊：[nextAlbum] 靜態墊在後面，[album] 可左右拖曳。
 */
@Composable
fun AlbumSwipeCardStack(
    album: Album,
    nextAlbum: Album?,
    onSwiped: (SwipeDirection) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val dismissThreshold = constraints.maxWidth * DISMISS_THRESHOLD_RATIO
        val flyAwayDistance = constraints.maxWidth.toFloat() * 1.5f

        if (nextAlbum != null) {
            AlbumCard(
                album = nextAlbum,
                modifier = Modifier.graphicsLayer {
                    scaleX = BACK_CARD_SCALE
                    scaleY = BACK_CARD_SCALE
                    translationY = BACK_CARD_OFFSET_Y
                },
            )
        }

        // key 讓每張卡片拿到全新的 Animatable，否則下一張會沿用上一張滑出畫面的位移。
        key(album.id) {
            val offsetX = remember { Animatable(0f) }
            val scope = rememberCoroutineScope()

            AlbumCard(
                album = album,
                overlay = when {
                    offsetX.value > OVERLAY_REVEAL_PX -> SwipeDirection.Like
                    offsetX.value < -OVERLAY_REVEAL_PX -> SwipeDirection.Skip
                    else -> null
                },
                overlayAlpha = (abs(offsetX.value) / dismissThreshold).coerceIn(0f, 1f),
                onClick = { onOpenAlbum(album) },
                modifier = Modifier
                    .graphicsLayer {
                        translationX = offsetX.value
                        rotationZ = offsetX.value / ROTATION_DAMPING
                    }
                    .pointerInput(album.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val current = offsetX.value
                                if (abs(current) < dismissThreshold) {
                                    scope.launch { offsetX.animateTo(0f, tween(ANIMATION_MS)) }
                                    return@detectHorizontalDragGestures
                                }
                                val direction = if (current > 0) SwipeDirection.Like else SwipeDirection.Skip
                                scope.launch {
                                    val target = if (current > 0) flyAwayDistance else -flyAwayDistance
                                    offsetX.animateTo(target, tween(ANIMATION_MS))
                                    onSwiped(direction)
                                }
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                        }
                    },
            )
        }
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    modifier: Modifier = Modifier,
    overlay: SwipeDirection? = null,
    overlayAlpha: Float = 0f,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            ) {
                AsyncImage(
                    model = album.artworkUrl,
                    contentDescription = album.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                if (overlay != null) {
                    SwipeOverlayLabel(direction = overlay, alpha = overlayAlpha)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = album.artistName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = album.subtitleLine(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SwipeOverlayLabel(direction: SwipeDirection, alpha: Float) {
    val isLike = direction == SwipeDirection.Like
    val container = if (isLike) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha },
        contentAlignment = if (isLike) Alignment.TopStart else Alignment.TopEnd,
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = container),
        ) {
            Text(
                text = stringResource(
                    if (isLike) R.string.music_roulette_like else R.string.music_roulette_skip,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isLike) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onError
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** 「發行年 · 類別1 / 類別2」，缺欄位時自動略過該段。 */
private fun Album.subtitleLine(): String = buildList {
    releaseDate?.take(4)?.takeIf { it.isNotBlank() }?.let(::add)
    genres.takeIf { it.isNotEmpty() }?.let { add(it.joinToString(" / ") { genre -> genre.label }) }
}.joinToString(" · ")

private const val DISMISS_THRESHOLD_RATIO = 0.3f
private const val BACK_CARD_SCALE = 0.92f
private const val BACK_CARD_OFFSET_Y = 24f
private const val OVERLAY_REVEAL_PX = 8f
private const val ROTATION_DAMPING = 40f
private const val ANIMATION_MS = 250
