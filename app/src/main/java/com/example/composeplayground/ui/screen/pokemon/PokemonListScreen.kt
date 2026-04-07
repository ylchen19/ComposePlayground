package com.example.composeplayground.ui.screen.pokemon

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.composeplayground.ui.theme.PokemonRed
import com.example.composeplayground.ui.theme.PokemonYellow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.LazyPagingItems
import com.example.composeplayground.data.model.Pokemon
import com.example.composeplayground.ui.screen.pokemon.components.PokemonGridCard
import com.example.composeplayground.ui.screen.pokemon.components.PokemonListItem
import com.example.composeplayground.ui.screen.pokemon.components.PokemonTypeFilterChip
import com.example.composeplayground.ui.screen.pokemon.components.ShimmerGridCard
import com.example.composeplayground.ui.screen.pokemon.components.ShimmerListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonListScreen(
    viewModel: PokemonListViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pokemonPagingFlow.collectAsLazyPagingItems()

    Scaffold(
        modifier = modifier,
        topBar = {
            PokemonListTopBar(
                viewMode = uiState.viewMode,
                onToggleViewMode = viewModel::toggleViewMode,
                onNavigateToSettings = onNavigateToSettings,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PokemonSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
            )
            PokemonTypeFilterRow(
                availableTypes = viewModel.availableTypes,
                selectedType = uiState.selectedType,
                onSelectType = viewModel::selectType,
            )
            Spacer(modifier = Modifier.height(8.dp))
            PokemonPagingContent(
                viewMode = uiState.viewMode,
                pagingItems = pagingItems,
                onNavigateToDetail = onNavigateToDetail,
            )
        }
    }
}

// ── Private composables ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PokemonListTopBar(
    viewMode: ViewMode,
    onToggleViewMode: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = "Pokédex",
                fontWeight = FontWeight.Bold,
                color = PokemonYellow,
            )
        },
        actions = {
            IconButton(onClick = onToggleViewMode) {
                if (viewMode == ViewMode.Grid) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Switch to list", tint = Color.White)
                } else {
                    GridViewIcon(color = Color.White)
                }
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = PokemonRed),
    )
}

@Composable
private fun PokemonSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search Pokémon...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = PokemonRed)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PokemonRed,
            focusedLabelColor = PokemonRed,
            cursorColor = PokemonRed,
        ),
    )
}

@Composable
private fun PokemonTypeFilterRow(
    availableTypes: List<String>,
    selectedType: String?,
    onSelectType: (String?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            PokemonTypeFilterChip(
                typeName = "All",
                isSelected = selectedType == null,
                onClick = { onSelectType(null) },
            )
        }
        items(availableTypes) { type ->
            PokemonTypeFilterChip(
                typeName = type,
                isSelected = selectedType == type,
                onClick = { onSelectType(if (selectedType == type) null else type) },
            )
        }
    }
}

@Composable
private fun PokemonPagingContent(
    viewMode: ViewMode,
    pagingItems: LazyPagingItems<Pokemon>,
    onNavigateToDetail: (Int) -> Unit,
) {
    AnimatedContent(
        targetState = viewMode,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "view_mode_transition",
    ) { mode ->
        when {
            pagingItems.loadState.refresh is LoadState.Loading -> PokemonShimmerContent(mode)
            pagingItems.loadState.refresh is LoadState.Error -> {
                val error = (pagingItems.loadState.refresh as LoadState.Error).error
                PokemonErrorContent(
                    message = error.localizedMessage ?: "An error occurred",
                    onRetry = pagingItems::retry,
                )
            }
            mode == ViewMode.Grid -> PokemonGrid(pagingItems, onNavigateToDetail)
            else -> PokemonList(pagingItems, onNavigateToDetail)
        }
    }
}

@Composable
private fun PokemonShimmerContent(viewMode: ViewMode) {
    if (viewMode == ViewMode.Grid) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(12) { ShimmerGridCard() }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(8) { ShimmerListItem() }
        }
    }
}

@Composable
private fun PokemonErrorContent(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PokemonRed),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
private fun PokemonGrid(
    pagingItems: LazyPagingItems<Pokemon>,
    onNavigateToDetail: (Int) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            count = pagingItems.itemCount,
            key = { index -> pagingItems[index]?.id ?: index },
            contentType = { "pokemon_grid" },
        ) { index ->
            val pokemon = pagingItems[index]
            if (pokemon != null) {
                PokemonGridCard(
                    pokemon = pokemon,
                    onClick = { onNavigateToDetail(pokemon.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        if (pagingItems.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun PokemonList(
    pagingItems: LazyPagingItems<Pokemon>,
    onNavigateToDetail: (Int) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            count = pagingItems.itemCount,
            key = { index -> pagingItems[index]?.id ?: index },
            contentType = { "pokemon_list" },
        ) { index ->
            val pokemon = pagingItems[index]
            if (pokemon != null) {
                PokemonListItem(
                    pokemon = pokemon,
                    onClick = { onNavigateToDetail(pokemon.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        if (pagingItems.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun GridViewIcon(color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(modifier = Modifier.size(8.dp).background(color, MaterialTheme.shapes.extraSmall))
            Box(modifier = Modifier.size(8.dp).background(color, MaterialTheme.shapes.extraSmall))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(modifier = Modifier.size(8.dp).background(color, MaterialTheme.shapes.extraSmall))
            Box(modifier = Modifier.size(8.dp).background(color, MaterialTheme.shapes.extraSmall))
        }
    }
}
