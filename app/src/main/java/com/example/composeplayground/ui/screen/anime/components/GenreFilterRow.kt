package com.example.composeplayground.ui.screen.anime.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.composeplayground.data.model.AnimeGenre

@Composable
fun GenreFilterRow(
    genres: List<AnimeGenre>,
    selectedGenreIds: Set<Int>,
    onToggleGenre: (Int) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all", contentType = "filter_chip") {
            FilterChip(
                selected = selectedGenreIds.isEmpty(),
                onClick = onClearAll,
                label = {
                    Text(
                        text = "All",
                        fontSize = 12.sp,
                        fontWeight = if (selectedGenreIds.isEmpty()) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.tertiary,
                ),
            )
        }
        items(
            items = genres,
            key = { genre -> genre.id },
            contentType = { "filter_chip" },
        ) { genre ->
            val selected = genre.id in selectedGenreIds
            FilterChip(
                selected = selected,
                onClick = { onToggleGenre(genre.id) },
                label = {
                    Text(
                        text = genre.name,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.tertiary,
                ),
            )
        }
    }
}
