# Paging 3 with Compose

## Table of Contents

- [Dependencies](#dependencies)
- [PagingSource (network-only)](#pagingsource-network-only)
- [RemoteMediator (offline-first paging)](#remotemediator-offline-first-paging)
- [Compose integration](#compose-integration)

## Dependencies

```toml
[versions]
paging = "3.3.4"

[libraries]
paging-runtime = { module = "androidx.paging:paging-runtime", version.ref = "paging" }
paging-compose = { module = "androidx.paging:paging-compose", version.ref = "paging" }
paging-testing = { module = "androidx.paging:paging-testing", version.ref = "paging" }
```

## PagingSource (network-only)

```kotlin
class ItemPagingSource @Inject constructor(
    private val api: ItemApiService,
    private val mapper: ItemMapper,
) : PagingSource<Int, Item>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item> {
        val page = params.key ?: 1
        return try {
            val response = api.getItems(page = page, pageSize = params.loadSize)
            LoadResult.Page(
                data = response.items.map(mapper::toDomain),
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.items.isEmpty()) null else page + 1,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Item>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
}
```

ViewModel:

```kotlin
val items: Flow<PagingData<Item>> = Pager(
    config = PagingConfig(pageSize = 20, prefetchDistance = 5),
    pagingSourceFactory = { ItemPagingSource(api, mapper) },
).flow.cachedIn(viewModelScope)
```

## RemoteMediator (offline-first paging)

For offline-first apps, use `RemoteMediator` to coordinate between Room and API:

```kotlin
@OptIn(ExperimentalPagingApi::class)
class ItemRemoteMediator @Inject constructor(
    private val api: ItemApiService,
    private val db: AppDatabase,
    private val mapper: ItemMapper,
) : RemoteMediator<Int, ItemEntity>() {

    private val dao = db.itemDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ItemEntity>,
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val lastItem = state.lastItemOrNull()
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
                lastItem.nextPage ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        return try {
            val response = api.getItems(page = page, pageSize = state.config.pageSize)
            val entities = response.items.map(mapper::toEntity)

            db.withTransaction {
                if (loadType == LoadType.REFRESH) dao.clearAll()
                dao.upsertAll(entities)
            }

            MediatorResult.Success(endOfPaginationReached = response.items.isEmpty())
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
```

## Compose integration

```kotlin
@Composable
fun PagingItemList(
    items: LazyPagingItems<Item>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(
            count = items.itemCount,
            key = items.itemKey { it.id },
            contentType = items.itemContentType { "item" },
        ) { index ->
            val item = items[index]
            if (item != null) {
                ItemCard(item = item, onClick = { onItemClick(item.id) })
            }
        }

        // Loading states
        when (items.loadState.append) {
            is LoadState.Loading -> item { LoadingFooter() }
            is LoadState.Error -> item {
                ErrorFooter(onRetry = { items.retry() })
            }
            else -> Unit
        }
    }

    // Pull-to-refresh state
    if (items.loadState.refresh is LoadState.Loading) {
        // Show full-screen loading
    }
}

// In screen composable:
@Composable
fun ItemListScreen(viewModel: ItemListViewModel = hiltViewModel()) {
    val items = viewModel.items.collectAsLazyPagingItems()
    PagingItemList(items = items, onItemClick = { /* navigate */ })
}
```

## Gotchas

- **`cachedIn(viewModelScope)` is mandatory** in the ViewModel. Without it, every
  recomposition restarts paging from scratch.
- **`collectAsLazyPagingItems()` must be called in the Composable** that owns the
  `LazyColumn`. Don't pass `PagingData<T>` to child composables — pass `LazyPagingItems<T>`.
- **RemoteMediator `REFRESH` must clear + re-insert in a single transaction**.
  Otherwise the UI briefly shows empty state.
