package com.example.composeplayground.ui.screen.anime

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.example.composeplayground.R
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.composeplayground.data.model.Anime
import com.example.composeplayground.data.repository.AnimeOrderBy
import com.example.composeplayground.data.repository.SortDirection
import com.example.composeplayground.ui.screen.anime.components.AnimeGridCard
import com.example.composeplayground.ui.screen.anime.components.AnimeListItem
import com.example.composeplayground.ui.screen.anime.components.AnimeShimmerGridCard
import com.example.composeplayground.ui.screen.anime.components.AnimeShimmerListItem
import com.example.composeplayground.ui.screen.anime.components.GenreFilterRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeListScreen(
    viewModel: AnimeListViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingItems = viewModel.animePagingFlow.collectAsLazyPagingItems()

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showScrollToTop by remember {
        derivedStateOf {
            if (uiState.viewMode == AnimeViewMode.Grid) {
                gridState.firstVisibleItemIndex > 0
            } else {
                listState.firstVisibleItemIndex > 0
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AnimeListTopBar(
                viewMode = uiState.viewMode,
                orderBy = uiState.orderBy,
                sortDirection = uiState.sortDirection,
                onToggleViewMode = viewModel::toggleViewMode,
                onSelectOrderBy = viewModel::setOrderBy,
                onToggleSortDirection = viewModel::toggleSortDirection,
                onBack = onBack,
            )
        },
        floatingActionButton = {
            if (showScrollToTop) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            if (uiState.viewMode == AnimeViewMode.Grid) {
                                gridState.animateScrollToItem(0)
                            } else {
                                listState.animateScrollToItem(0)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll to top",
                        tint = MaterialTheme.colorScheme.onTertiary,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AnimeSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
            )
            if (uiState.genres.isNotEmpty()) {
                GenreFilterRow(
                    genres = uiState.genres,
                    selectedGenreIds = uiState.selectedGenreIds,
                    onToggleGenre = viewModel::toggleGenre,
                    onClearAll = viewModel::clearGenres,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            AnimePagingContent(
                viewMode = uiState.viewMode,
                pagingItems = pagingItems,
                onNavigateToDetail = onNavigateToDetail,
                gridState = gridState,
                listState = listState,
            )
        }
    }
}

// ── Private composables ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimeListTopBar(
    viewMode: AnimeViewMode,
    orderBy: AnimeOrderBy,
    sortDirection: SortDirection,
    onToggleViewMode: () -> Unit,
    onSelectOrderBy: (AnimeOrderBy) -> Unit,
    onToggleSortDirection: () -> Unit,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.anime_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        },
        actions = {
            SortMenuButton(
                orderBy = orderBy,
                sortDirection = sortDirection,
                onSelectOrderBy = onSelectOrderBy,
                onToggleSortDirection = onToggleSortDirection,
            )
            IconButton(onClick = onToggleViewMode) {
                if (viewMode == AnimeViewMode.Grid) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.anime_switch_list),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = stringResource(R.string.anime_switch_grid),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortMenuButton(
    orderBy: AnimeOrderBy,
    sortDirection: SortDirection,
    onSelectOrderBy: (AnimeOrderBy) -> Unit,
    onToggleSortDirection: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "Sort",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        IconButton(onClick = onToggleSortDirection) {
            Icon(
                imageVector = if (sortDirection == SortDirection.Desc) {
                    Icons.Default.KeyboardArrowDown
                } else {
                    Icons.Default.KeyboardArrowUp
                },
                contentDescription = if (sortDirection == SortDirection.Desc) stringResource(R.string.anime_sort_descending) else stringResource(R.string.anime_sort_ascending),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AnimeOrderBy.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.displayName(),
                            fontWeight = if (option == orderBy) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSelectOrderBy(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AnimeOrderBy.displayName(): String = when (this) {
    AnimeOrderBy.Score -> stringResource(R.string.anime_sort_score)
    AnimeOrderBy.Popularity -> stringResource(R.string.anime_sort_popularity)
    AnimeOrderBy.Rank -> stringResource(R.string.anime_sort_rank)
    AnimeOrderBy.Title -> stringResource(R.string.anime_sort_title)
    AnimeOrderBy.StartDate -> stringResource(R.string.anime_sort_start_date)
    AnimeOrderBy.Episodes -> stringResource(R.string.anime_sort_episodes)
}

@Composable
private fun AnimeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.anime_search_placeholder)) },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
private fun AnimePagingContent(
    viewMode: AnimeViewMode,
    pagingItems: LazyPagingItems<Anime>,
    onNavigateToDetail: (Int) -> Unit,
    gridState: LazyGridState,
    listState: LazyListState,
) {
    AnimatedContent(
        targetState = viewMode,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "anime_view_mode",
    ) { mode ->
        when {
            pagingItems.loadState.refresh is LoadState.Loading -> AnimeShimmerContent(mode)
            pagingItems.loadState.refresh is LoadState.Error -> {
                val error = (pagingItems.loadState.refresh as LoadState.Error).error
                AnimeErrorContent(
                    message = error.localizedMessage ?: stringResource(R.string.anime_error_load_list),
                    onRetry = pagingItems::retry,
                )
            }
            pagingItems.itemCount == 0 -> AnimeEmptyContent()
            mode == AnimeViewMode.Grid -> AnimeGrid(pagingItems, onNavigateToDetail, gridState)
            else -> AnimeList(pagingItems, onNavigateToDetail, listState)
        }
    }
}

@Composable
private fun AnimeShimmerContent(viewMode: AnimeViewMode) {
    if (viewMode == AnimeViewMode.Grid) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(8) { AnimeShimmerGridCard() }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(6) { AnimeShimmerListItem() }
        }
    }
}

@Composable
private fun AnimeErrorContent(message: String, onRetry: () -> Unit) {
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
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.anime_action_retry))
            }
        }
    }
}

@Composable
private fun AnimeEmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.anime_empty_list),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun AnimeGrid(
    pagingItems: LazyPagingItems<Anime>,
    onNavigateToDetail: (Int) -> Unit,
    state: LazyGridState,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        state = state,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            count = pagingItems.itemCount,
            key = { index -> pagingItems[index]?.id ?: index },
            contentType = { "anime_grid" },
        ) { index ->
            val anime = pagingItems[index]
            if (anime != null) {
                AnimeGridCard(
                    anime = anime,
                    onClick = { onNavigateToDetail(anime.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        if (pagingItems.loadState.append is LoadState.Loading) {
            item(contentType = "loader") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun AnimeList(
    pagingItems: LazyPagingItems<Anime>,
    onNavigateToDetail: (Int) -> Unit,
    state: LazyListState,
) {
    LazyColumn(
        state = state,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            count = pagingItems.itemCount,
            key = { index -> pagingItems[index]?.id ?: index },
            contentType = { "anime_list" },
        ) { index ->
            val anime = pagingItems[index]
            if (anime != null) {
                AnimeListItem(
                    anime = anime,
                    onClick = { onNavigateToDetail(anime.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        if (pagingItems.loadState.append is LoadState.Loading) {
            item(contentType = "loader") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
    }
}
