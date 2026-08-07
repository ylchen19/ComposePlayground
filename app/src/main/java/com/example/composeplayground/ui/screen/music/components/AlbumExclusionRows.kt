package com.example.composeplayground.ui.screen.music.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.composeplayground.R
import com.example.composeplayground.data.model.AlbumGenre
import com.example.composeplayground.data.repository.MusicRegion

/**
 * 排除式多選 chip row。語意與一般篩選相反——**選中代表「已排除」**，故用
 * errorContainer 配色 + 打叉 icon + 刪節線，避免與 `GenreFilterRow` 的「選中＝只看這個」
 * 混淆。
 */
@Composable
fun GenreExclusionRow(
    genres: List<AlbumGenre>,
    excludedGenreIds: Set<String>,
    onToggleGenre: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "clear", contentType = "clear_chip") {
            AssistChip(
                onClick = onClearAll,
                enabled = excludedGenreIds.isNotEmpty(),
                label = { Text(text = stringResource(R.string.music_roulette_clear_exclusions), fontSize = 12.sp) },
            )
        }
        items(
            items = genres,
            key = { it.id },
            contentType = { "exclusion_chip" },
        ) { genre ->
            ExclusionChip(
                label = genre.label,
                excluded = genre.id in excludedGenreIds,
                onClick = { onToggleGenre(genre.id) },
            )
        }
    }
}

@Composable
fun RegionExclusionRow(
    excludedRegions: Set<MusicRegion>,
    onToggleRegion: (MusicRegion) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = MusicRegion.entries,
            key = { it.name },
            contentType = { "exclusion_chip" },
        ) { region ->
            ExclusionChip(
                label = stringResource(region.labelRes),
                excluded = region in excludedRegions,
                onClick = { onToggleRegion(region) },
            )
        }
    }
}

@Composable
private fun ExclusionChip(
    label: String,
    excluded: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = excluded,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (excluded) FontWeight.Bold else FontWeight.Normal,
                textDecoration = if (excluded) TextDecoration.LineThrough else null,
            )
        },
        leadingIcon = if (excluded) {
            {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    )
}

private val MusicRegion.labelRes: Int
    get() = when (this) {
        MusicRegion.Global -> R.string.music_region_global
        MusicRegion.Taiwan -> R.string.music_region_taiwan
        MusicRegion.Japan -> R.string.music_region_japan
        MusicRegion.Korea -> R.string.music_region_korea
        MusicRegion.France -> R.string.music_region_france
        MusicRegion.Germany -> R.string.music_region_germany
        MusicRegion.Spain -> R.string.music_region_spain
    }
