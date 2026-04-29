package com.example.composeplayground.ui.screen.picsum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.composeplayground.data.model.PicsumPhoto
import com.example.composeplayground.ui.screen.picsum.components.PicsumGridCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PicsumGalleryScreen(
    viewModel: PicsumGalleryViewModel,
    onNavigateToDetail: (PicsumPhoto) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagingItems = viewModel.photos.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Picsum 圖庫") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                pagingItems.loadState.refresh is LoadState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                pagingItems.loadState.refresh is LoadState.Error -> {
                    val err = (pagingItems.loadState.refresh as LoadState.Error).error
                    PicsumErrorContent(
                        message = err.localizedMessage ?: "載入失敗",
                        onRetry = pagingItems::retry,
                    )
                }
                else -> PicsumGrid(
                    pagingItems = pagingItems,
                    state = gridState,
                    onClickPhoto = onNavigateToDetail,
                )
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
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(onClick = pagingItems::retry) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("重試")
                    }
                }
            }
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
                Text("重試")
            }
        }
    }
}
