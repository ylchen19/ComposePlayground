package com.example.composeplayground.ui.screen.pokemon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.composeplayground.ui.screen.pokemon.components.PokemonGridCard
import com.example.composeplayground.ui.screen.pokemon.components.ShimmerGridCard
import com.example.composeplayground.ui.screen.pokemon.components.UniformHeightLazyRow
import com.example.composeplayground.ui.screen.pokemon.components.pokemonTypeColors
import com.example.composeplayground.ui.theme.PokemonRed
import com.example.composeplayground.ui.theme.PokemonYellow
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonTypeGalleryScreen(
    modifier: Modifier = Modifier,
    viewModel: PokemonTypeGalleryViewModel = koinViewModel(),
    onNavigateToDetail: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.composeplayground.R.string.type_gallery),
                        fontWeight = FontWeight.Bold,
                        color = PokemonYellow,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(com.example.composeplayground.R.string.back),
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PokemonRed),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = uiState.sections,
                key = { it.typeName },
            ) { section ->
                PokemonTypeSectionRow(
                    section = section,
                    onNavigateToDetail = onNavigateToDetail,
                )
            }
        }
    }
}

@Composable
private fun PokemonTypeSectionRow(
    section: PokemonTypeSection,
    onNavigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeColor = pokemonTypeColors[section.typeName] ?: Color.Gray

    // 卡片寬度隨系統字體縮放等比放大，上限 2×
    val fontScale = LocalDensity.current.fontScale
    val cardWidth = (140 * fontScale.coerceAtMost(2f)).dp
    val textAreaWidth = cardWidth - 24.dp // Card 內 padding 12dp × 2
    val nameStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)

    // 取名稱最長的寶可夢名稱作為 probe（O(1) 比較）
    val longestName = remember(section.pokemon) {
        section.pokemon.maxByOrNull { it.name.length }
            ?.name?.replaceFirstChar { c -> c.titlecase() } ?: ""
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // ── 屬性標題列 ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 屬性色塊指示條
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .background(typeColor, RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = section.typeName.replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = typeColor,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (!section.isLoading) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.composeplayground.R.string.pokemon_count, section.pokemon.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── 水平捲動的寶可夢列表 ───────────────────────────────────────────────
        if (section.isLoading) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(4) {
                    ShimmerGridCard(modifier = Modifier.width(cardWidth))
                }
            }
        } else if (section.pokemon.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.composeplayground.R.string.no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val listState = rememberLazyListState()
            var visibleCount by remember(section.typeName) {
                mutableIntStateOf(PokemonTypeGalleryViewModel.PAGE_SIZE)
            }

            // 偵測捲動到接近末尾時，增加顯示數量
            val nearEnd by remember {
                derivedStateOf {
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    lastVisible >= visibleCount - PRELOAD_THRESHOLD
                }
            }
            LaunchedEffect(nearEnd) {
                if (nearEnd && visibleCount < section.pokemon.size) {
                    visibleCount = minOf(
                        visibleCount + PokemonTypeGalleryViewModel.PAGE_SIZE,
                        section.pokemon.size,
                    )
                }
            }

            UniformHeightLazyRow(
                probeText = longestName,
                probeStyle = nameStyle,
                probeMaxWidth = textAreaWidth,
            ) { minTextHeight ->
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        count = minOf(visibleCount, section.pokemon.size),
                        key = { index -> section.pokemon[index].id },
                        // Gap 2: explicit contentType lets Compose recycle slots only among
                        // structurally identical composables (cards with cards, loader with loader).
                        contentType = { "pokemon_card" },
                    ) { index ->
                        PokemonGridCard(
                            pokemon = section.pokemon[index],
                            onClick = { onNavigateToDetail(section.pokemon[index].id) },
                            modifier = Modifier.width(cardWidth),
                            minTextHeight = minTextHeight,
                        )
                    }
                    // 尾端載入指示器
                    if (visibleCount < section.pokemon.size) {
                        item(key = "loader_${section.typeName}", contentType = "loader") {
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(180.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = typeColor,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

private const val PRELOAD_THRESHOLD = 5
