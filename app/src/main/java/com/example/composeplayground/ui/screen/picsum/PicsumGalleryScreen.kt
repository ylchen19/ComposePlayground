package com.example.composeplayground.ui.screen.picsum

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.composeplayground.R
import com.example.composeplayground.data.model.PicsumPhoto
import com.example.composeplayground.ui.screen.picsum.components.PicsumGridCard
import com.example.composeplayground.ui.screen.picsum.components.PicsumListCard
import com.example.composeplayground.ui.screen.picsum.components.PicsumShimmerGridCard
import com.example.composeplayground.ui.screen.picsum.components.PicsumShimmerListItem
import com.example.composeplayground.ui.screen.picsum.components.PicsumShimmerStaggeredCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PicsumGalleryScreen(
    viewModel: PicsumGalleryViewModel,
    onNavigateToDetail: (PicsumPhoto) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingItems = viewModel.photos.collectAsLazyPagingItems()

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val staggeredState = rememberLazyStaggeredGridState()
    val coroutineScope = rememberCoroutineScope()

    val showScrollToTop by remember {
        derivedStateOf {
            when (uiState.viewMode) {
                PicsumViewMode.Grid -> gridState.firstVisibleItemIndex > 0
                PicsumViewMode.List -> listState.firstVisibleItemIndex > 0
                PicsumViewMode.StaggeredGrid -> staggeredState.firstVisibleItemIndex > 0
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.sortLoadError) {
        uiState.sortLoadError?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSortError()
        }
    }

    LaunchedEffect(uiState.sortMode) {
        when (uiState.viewMode) {
            PicsumViewMode.Grid -> gridState.scrollToItem(0)
            PicsumViewMode.List -> listState.scrollToItem(0)
            PicsumViewMode.StaggeredGrid -> staggeredState.scrollToItem(0)
        }
    }

    PicsumTheme {
        Scaffold(
            modifier = modifier,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    if (uiState.isSearchActive) {
                        TopAppBar(
                            title = {
                                TextField(
                                    value = uiState.searchQuery,
                                    onValueChange = viewModel::updateSearchQuery,
                                    placeholder = { Text(stringResource(R.string.search_author)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                    )
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = viewModel::toggleSearchActive) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "關閉搜尋")
                                }
                            },
                            actions = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "清除")
                                    }
                                }
                            }
                        )
                        val showSuggestions = uiState.authorSuggestions.isNotEmpty() &&
                                !(uiState.authorSuggestions.size == 1 && uiState.authorSuggestions.first().equals(uiState.searchQuery, ignoreCase = true))

                        if (showSuggestions) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                            ) {
                                items(uiState.authorSuggestions.size) { index ->
                                    val author = uiState.authorSuggestions[index]
                                    SuggestionChip(
                                        onClick = { viewModel.selectAuthor(author) },
                                        label = { Text(author) }
                                    )
                                }
                            }
                        }
                    } else {
                        TopAppBar(
                            title = { Text(stringResource(R.string.picsum_gallery)) },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                                }
                            },
                            actions = {
                                IconButton(onClick = viewModel::toggleSearchActive) {
                                    Icon(Icons.Default.Search, contentDescription = "搜尋")
                                }
                                SortMenuButton(
                                    currentSort = uiState.sortMode,
                                    onSortSelected = viewModel::setSortMode,
                                )
                                IconButton(onClick = viewModel::cycleViewMode) {
                                    when (uiState.viewMode) {
                                        PicsumViewMode.Grid -> Icon(
                                            Icons.AutoMirrored.Filled.List,
                                            contentDescription = "切換為列表",
                                        )
                                        PicsumViewMode.List -> StaggeredGridIcon()
                                        PicsumViewMode.StaggeredGrid -> GridViewIcon()
                                    }
                                }
                            },
                        )
                    }
                }
            },
            floatingActionButton = {
                if (showScrollToTop) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                when (uiState.viewMode) {
                                    PicsumViewMode.Grid -> gridState.animateScrollToItem(0)
                                    PicsumViewMode.List -> listState.animateScrollToItem(0)
                                    PicsumViewMode.StaggeredGrid -> staggeredState.animateScrollToItem(0)
                                }
                            }
                        },
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "回到頂部")
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                PicsumPagingContent(
                    viewMode = uiState.viewMode,
                    pagingItems = pagingItems,
                    onClickPhoto = onNavigateToDetail,
                    gridState = gridState,
                    listState = listState,
                    staggeredState = staggeredState,
                )
                if (uiState.isLoadingAllForSort) {
                    SortLoadingOverlay(progress = uiState.sortLoadProgress)
                }
            }
        }
    }
}

// ── Private composables ──────────────────────────────────────────────────────

@Composable
private fun PicsumPagingContent(
    viewMode: PicsumViewMode,
    pagingItems: LazyPagingItems<PicsumPhoto>,
    onClickPhoto: (PicsumPhoto) -> Unit,
    gridState: LazyGridState,
    listState: LazyListState,
    staggeredState: LazyStaggeredGridState,
) {
    AnimatedContent(
        targetState = viewMode,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "picsum_view_mode_transition",
    ) { mode ->
        when {
            pagingItems.loadState.refresh is LoadState.Loading -> PicsumShimmerContent(mode)
            pagingItems.loadState.refresh is LoadState.Error -> {
                val err = (pagingItems.loadState.refresh as LoadState.Error).error
                PicsumErrorContent(
                    message = err.localizedMessage ?: "載入失敗",
                    onRetry = pagingItems::retry,
                )
            }
            else -> when (mode) {
                PicsumViewMode.Grid -> PicsumGrid(pagingItems, gridState, onClickPhoto)
                PicsumViewMode.List -> PicsumList(pagingItems, listState, onClickPhoto)
                PicsumViewMode.StaggeredGrid -> PicsumStaggeredGrid(pagingItems, staggeredState, onClickPhoto)
            }
        }
    }
}

@Composable
private fun PicsumShimmerContent(viewMode: PicsumViewMode) {
    val staggeredRatios = remember {
        listOf(1f, 0.75f, 1.2f, 0.6f, 1.5f, 0.8f, 1.1f, 0.7f, 0.9f, 1.3f, 0.65f, 1.4f)
    }
    when (viewMode) {
        PicsumViewMode.Grid -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(8) { PicsumShimmerGridCard() }
            }
        }
        PicsumViewMode.List -> {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(6) { PicsumShimmerListItem() }
            }
        }
        PicsumViewMode.StaggeredGrid -> {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp,
            ) {
                items(staggeredRatios.size) { index ->
                    PicsumShimmerStaggeredCard(aspectRatio = staggeredRatios[index])
                }
            }
        }
    }
}

@Composable
private fun PicsumGrid(
    pagingItems: LazyPagingItems<PicsumPhoto>,
    state: LazyGridState,
    onClickPhoto: (PicsumPhoto) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = state,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            count = pagingItems.itemCount,
            key = { index -> pagingItems[index]?.id ?: index },
            contentType = { "picsum_grid" },
        ) { index ->
            val photo = pagingItems[index]
            if (photo != null) {
                PicsumGridCard(
                    photo = photo,
                    onClick = { onClickPhoto(photo) },
                )
            }
        }
        if (pagingItems.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(2) }, contentType = "loader") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
        if (pagingItems.loadState.append is LoadState.Error) {
            item(span = { GridItemSpan(2) }, contentType = "append_error") {
                AppendErrorItem(onRetry = pagingItems::retry)
            }
        }
    }
}

@Composable
private fun PicsumList(
    pagingItems: LazyPagingItems<PicsumPhoto>,
    state: LazyListState,
    onClickPhoto: (PicsumPhoto) -> Unit,
) {
    LazyColumn(
        state = state,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            count = pagingItems.itemCount,
            key = { index -> pagingItems[index]?.id ?: index },
            contentType = { "picsum_list" },
        ) { index ->
            val photo = pagingItems[index]
            if (photo != null) {
                PicsumListCard(
                    photo = photo,
                    onClick = { onClickPhoto(photo) },
                )
            }
        }
        if (pagingItems.loadState.append is LoadState.Loading) {
            item(contentType = "loader") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
        if (pagingItems.loadState.append is LoadState.Error) {
            item(contentType = "append_error") {
                AppendErrorItem(onRetry = pagingItems::retry)
            }
        }
    }
}

@Composable
private fun PicsumStaggeredGrid(
    pagingItems: LazyPagingItems<PicsumPhoto>,
    state: LazyStaggeredGridState,
    onClickPhoto: (PicsumPhoto) -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = state,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            count = pagingItems.itemCount,
            key = { index -> pagingItems[index]?.id ?: index },
            contentType = { "picsum_staggered" },
        ) { index ->
            val photo = pagingItems[index]
            if (photo != null) {
                PicsumGridCard(
                    photo = photo,
                    onClick = { onClickPhoto(photo) },
                    useOriginalAspectRatio = true,
                )
            }
        }
        if (pagingItems.loadState.append is LoadState.Loading) {
            item(span = StaggeredGridItemSpan.FullLine, contentType = "loader") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
        if (pagingItems.loadState.append is LoadState.Error) {
            item(span = StaggeredGridItemSpan.FullLine, contentType = "append_error") {
                AppendErrorItem(onRetry = pagingItems::retry)
            }
        }
    }
}

@Composable
private fun AppendErrorItem(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun PicsumErrorContent(
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
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun GridViewIcon() {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(modifier = Modifier.size(8.dp).background(
                MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.extraSmall,
            ))
            Box(modifier = Modifier.size(8.dp).background(
                MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.extraSmall,
            ))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(modifier = Modifier.size(8.dp).background(
                MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.extraSmall,
            ))
            Box(modifier = Modifier.size(8.dp).background(
                MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.extraSmall,
            ))
        }
    }
}

@Composable
private fun StaggeredGridIcon() {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(modifier = Modifier.size(width = 8.dp, height = 11.dp).background(
                MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.extraSmall,
            ))
            Box(modifier = Modifier.size(width = 8.dp, height = 6.dp).background(
                MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.extraSmall,
            ))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(modifier = Modifier.size(width = 8.dp, height = 6.dp).background(
                MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.extraSmall,
            ))
            Box(modifier = Modifier.size(width = 8.dp, height = 11.dp).background(
                MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.extraSmall,
            ))
        }
    }
}

@Composable
private fun SortMenuButton(
    currentSort: PicsumSortMode,
    onSortSelected: (PicsumSortMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "排序")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SortMenuItem(
                label = "隨機",
                selected = currentSort == PicsumSortMode.Random,
                onClick = {
                    onSortSelected(PicsumSortMode.Random)
                    expanded = false
                },
            )
            SortMenuItem(
                label = "解析度：高 → 低",
                selected = currentSort == PicsumSortMode.ResolutionDesc,
                onClick = {
                    onSortSelected(PicsumSortMode.ResolutionDesc)
                    expanded = false
                },
            )
            SortMenuItem(
                label = "解析度：低 → 高",
                selected = currentSort == PicsumSortMode.ResolutionAsc,
                onClick = {
                    onSortSelected(PicsumSortMode.ResolutionAsc)
                    expanded = false
                },
            )
            SortMenuItem(
                label = "作者 A → Z",
                selected = currentSort == PicsumSortMode.AuthorAsc,
                onClick = {
                    onSortSelected(PicsumSortMode.AuthorAsc)
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun SortMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        trailingIcon = {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null)
            }
        },
    )
}

@Composable
private fun SortLoadingOverlay(progress: SortLoadProgress?) {
    val loaded = progress?.loadedPages ?: 0
    val total = progress?.totalPagesEstimate
    val targetFraction = if (total != null && total > 0) {
        (loaded.toFloat() / total).coerceIn(0f, 1f)
    } else null

    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction ?: 0f,
        label = "sort_load_fraction",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { /* 攔截點擊，避免 loading 期間穿透到底下圖片 */ },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .padding(32.dp)
                .widthIn(min = 240.dp, max = 320.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (targetFraction != null) {
                        CircularProgressIndicator(
                            progress = { animatedFraction },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 6.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Text(
                            text = "${(animatedFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 6.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "正在載入全部相片",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when {
                        total != null -> "已載入 $loaded / $total 頁"
                        loaded > 0 -> "已載入 $loaded 頁…"
                        else -> "準備中…"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
