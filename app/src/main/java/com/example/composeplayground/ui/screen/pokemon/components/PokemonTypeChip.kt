package com.example.composeplayground.ui.screen.pokemon.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// Type color map
val pokemonTypeColors = mapOf(
    "normal" to Color(0xFFA8A77A),
    "fire" to Color(0xFFEE8130),
    "water" to Color(0xFF6390F0),
    "electric" to Color(0xFFF7D02C),
    "grass" to Color(0xFF7AC74C),
    "ice" to Color(0xFF96D9D6),
    "fighting" to Color(0xFFC22E28),
    "poison" to Color(0xFFA33EA1),
    "ground" to Color(0xFFE2BF65),
    "flying" to Color(0xFFA98FF3),
    "psychic" to Color(0xFFF95587),
    "bug" to Color(0xFFA6B91A),
    "rock" to Color(0xFFB6A136),
    "ghost" to Color(0xFF735797),
    "dragon" to Color(0xFF6F35FC),
    "dark" to Color(0xFF705746),
    "steel" to Color(0xFFB7B7CE),
    "fairy" to Color(0xFFD685AD),
)

@Composable
fun PokemonTypeLabel(
    typeName: String,
    modifier: Modifier = Modifier,
) {
    val color = pokemonTypeColors[typeName] ?: Color.Gray
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color,
    ) {
        Text(
            text = typeName.replaceFirstChar { it.titlecase(Locale.ROOT) },
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun PokemonTypeFilterChip(
    typeName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = pokemonTypeColors[typeName] ?: Color.Gray
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = typeName.replaceFirstChar { it.titlecase(Locale.ROOT) },
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.2f),
            selectedLabelColor = color,
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = color.copy(alpha = 0.5f),
            selectedBorderColor = color,
            enabled = true,
            selected = isSelected,
        ),
        modifier = modifier,
    )
}
