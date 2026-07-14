package com.example.composeplayground.ui.screen.music.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.composeplayground.R
import com.example.composeplayground.data.repository.MusicGenre
import com.example.composeplayground.data.repository.MusicRegion

@Composable
fun MusicGenreFilterRow(
    selectedGenre: MusicGenre,
    onSelectGenre: (MusicGenre) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = MusicGenre.entries,
            key = { it.name },
            contentType = { "genre_filter_chip" },
        ) { genre ->
            val selected = genre == selectedGenre
            FilterChip(
                selected = selected,
                onClick = { onSelectGenre(genre) },
                label = {
                    Text(
                        text = genre.displayName(),
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
fun MusicRegionFilterRow(
    selectedRegion: MusicRegion,
    onSelectRegion: (MusicRegion) -> Unit,
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
            contentType = { "region_filter_chip" },
        ) { region ->
            val selected = region == selectedRegion
            FilterChip(
                selected = selected,
                onClick = { onSelectRegion(region) },
                label = {
                    Text(
                        text = region.displayName(),
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun MusicGenre.displayName(): String = when (this) {
    MusicGenre.All -> stringResource(R.string.music_genre_all)
    MusicGenre.Pop -> stringResource(R.string.music_genre_pop)
    MusicGenre.Rock -> stringResource(R.string.music_genre_rock)
    MusicGenre.HipHopRap -> stringResource(R.string.music_genre_hiphop)
    MusicGenre.Country -> stringResource(R.string.music_genre_country)
    MusicGenre.Alternative -> stringResource(R.string.music_genre_alternative)
    MusicGenre.RnbSoul -> stringResource(R.string.music_genre_rnb)
    MusicGenre.Electronic -> stringResource(R.string.music_genre_electronic)
    MusicGenre.Jazz -> stringResource(R.string.music_genre_jazz)
    MusicGenre.Classical -> stringResource(R.string.music_genre_classical)
    MusicGenre.Latin -> stringResource(R.string.music_genre_latin)
    MusicGenre.World -> stringResource(R.string.music_genre_world)
    MusicGenre.Reggae -> stringResource(R.string.music_genre_reggae)
}

@Composable
private fun MusicRegion.displayName(): String = when (this) {
    MusicRegion.Global -> stringResource(R.string.music_region_global)
    MusicRegion.Taiwan -> stringResource(R.string.music_region_taiwan)
    MusicRegion.Japan -> stringResource(R.string.music_region_japan)
    MusicRegion.Korea -> stringResource(R.string.music_region_korea)
    MusicRegion.France -> stringResource(R.string.music_region_france)
    MusicRegion.Germany -> stringResource(R.string.music_region_germany)
    MusicRegion.Spain -> stringResource(R.string.music_region_spain)
}
