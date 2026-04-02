package com.example.composeplayground.ui.screen.pokemon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.composeplayground.data.model.PokemonDetail
import com.example.composeplayground.data.model.PokemonStatInfo
import com.example.composeplayground.ui.screen.pokemon.components.PokemonTypeLabel
import com.example.composeplayground.ui.screen.pokemon.components.pokemonTypeColors
import com.example.composeplayground.ui.theme.PokemonRed
import com.example.composeplayground.ui.theme.PokemonYellow
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailScreen(
    viewModel: PokemonDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarColor = when (val state = uiState) {
        is PokemonDetailUiState.Success -> {
            pokemonTypeColors[state.pokemon.types.firstOrNull()] ?: PokemonRed
        }
        else -> PokemonRed
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    when (val state = uiState) {
                        is PokemonDetailUiState.Success -> Text(
                            state.pokemon.name.replaceFirstChar {
                                it.titlecase(Locale.ROOT)
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        else -> Text("Pokémon Detail", color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor,
                ),
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is PokemonDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is PokemonDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = viewModel::retry,
                            colors = ButtonDefaults.buttonColors(containerColor = PokemonRed),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }

            is PokemonDetailUiState.Success -> {
                PokemonDetailContent(
                    pokemon = state.pokemon,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PokemonDetailContent(
    pokemon: PokemonDetail,
    modifier: Modifier = Modifier,
) {
    val primaryTypeColor = pokemonTypeColors[pokemon.types.firstOrNull()] ?: Color.Gray
    var animateStats by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        animateStats = true
    }

    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero image with rich type-color background + pokéball decoration
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                primaryTypeColor.copy(alpha = 0.7f),
                                primaryTypeColor.copy(alpha = 0.2f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // Large pokéball decoration circle (outer ring)
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 60.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                )
                // Inner pokéball circle
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 60.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                )
                AsyncImage(
                    model = pokemon.imageUrl,
                    contentDescription = pokemon.name,
                    modifier = Modifier.size(220.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        // ID and name
        item {
            Text(
                text = "#${pokemon.id.toString().padStart(3, '0')}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pokemon.name.replaceFirstChar { it.titlecase(Locale.ROOT) },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Type chips
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(
                    8.dp,
                    Alignment.CenterHorizontally,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                pokemon.types.forEach { type ->
                    PokemonTypeLabel(typeName = type)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Basic info cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InfoCard(
                    title = "Height",
                    value = "${pokemon.height / 10.0} m",
                    typeColor = primaryTypeColor,
                    modifier = Modifier.weight(1f),
                )
                InfoCard(
                    title = "Weight",
                    value = "${pokemon.weight / 10.0} kg",
                    typeColor = primaryTypeColor,
                    modifier = Modifier.weight(1f),
                )
                InfoCard(
                    title = "ID",
                    value = "#${pokemon.id}",
                    typeColor = primaryTypeColor,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Stats section header
        item {
            SectionHeader(title = "Base Stats", typeColor = primaryTypeColor)
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(pokemon.stats) { stat ->
            AnimatedVisibility(
                visible = animateStats,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)),
            ) {
                StatBar(
                    stat = stat,
                    animate = animateStats,
                    typeColor = primaryTypeColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        // Abilities section header
        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader(title = "Abilities", typeColor = primaryTypeColor)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(pokemon.abilities) { ability ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = primaryTypeColor.copy(alpha = 0.1f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ability.name
                            .replace("-", " ")
                            .replaceFirstChar { it.titlecase(Locale.ROOT) },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    if (ability.isHidden) {
                        Text(
                            text = "Hidden",
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryTypeColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Bottom spacing
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    typeColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(typeColor),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    value: String,
    typeColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = typeColor.copy(alpha = 0.12f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = typeColor.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StatBar(
    stat: PokemonStatInfo,
    animate: Boolean,
    typeColor: Color,
    modifier: Modifier = Modifier,
) {
    val maxStat = 255f
    val progress by animateFloatAsState(
        targetValue = if (animate) stat.baseStat / maxStat else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "stat_progress",
    )

    val statColor = when {
        stat.baseStat >= 150 -> Color(0xFF4CAF50)
        stat.baseStat >= 100 -> Color(0xFF8BC34A)
        stat.baseStat >= 75 -> Color(0xFFFFEB3B)
        stat.baseStat >= 50 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    val statLabel = when (stat.name) {
        "hp" -> "HP"
        "attack" -> "ATK"
        "defense" -> "DEF"
        "special-attack" -> "SpATK"
        "special-defense" -> "SpDEF"
        "speed" -> "SPD"
        else -> stat.name.uppercase(Locale.ROOT)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = statLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(52.dp),
            color = typeColor.copy(alpha = 0.8f),
        )
        Text(
            text = stat.baseStat.toString().padStart(3),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(typeColor.copy(alpha = 0.15f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(statColor),
            )
        }
    }
}
