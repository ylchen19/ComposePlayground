# Offline-First Architecture Pattern

## Table of Contents

- [Core principle](#core-principle)
- [Repository pattern with NetworkBoundResource](#repository-pattern-with-networkboundresource)
- [Connectivity-aware refresh](#connectivity-aware-refresh)
- [Conflict resolution strategies](#conflict-resolution-strategies)

## Core principle

Room is the **single source of truth**. The UI always reads from Room.
The network is a refresh mechanism, not a data source.

```
UI ←── Flow ←── Room DAO
                  ↑
               upsert
                  ↑
            API response
```

## Repository pattern with NetworkBoundResource

A reusable `networkBoundResource` utility that handles the cache-then-fetch flow:

```kotlin
inline fun <ResultType, RequestType> networkBoundResource(
    crossinline query: () -> Flow<ResultType>,
    crossinline fetch: suspend () -> RequestType,
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    crossinline shouldFetch: (ResultType) -> Boolean = { true },
): Flow<Resource<ResultType>> = flow {

    val data = query().first()

    val flow = if (shouldFetch(data)) {
        emit(Resource.Loading(data))
        try {
            saveFetchResult(fetch())
            query().map { Resource.Success(it) }
        } catch (throwable: Throwable) {
            query().map { Resource.Error(throwable, it) }
        }
    } else {
        query().map { Resource.Success(it) }
    }

    emitAll(flow)
}

sealed class Resource<T>(val data: T?, val error: Throwable?) {
    class Success<T>(data: T) : Resource<T>(data, null)
    class Loading<T>(data: T? = null) : Resource<T>(data, null)
    class Error<T>(throwable: Throwable, data: T? = null) : Resource<T>(data, throwable)
}
```

Usage in repository:

```kotlin
class ArticleRepositoryImpl @Inject constructor(
    private val api: ArticleApiService,
    private val dao: ArticleDao,
    private val mapper: ArticleMapper,
) : ArticleRepository {

    override fun getArticles(): Flow<Resource<List<Article>>> =
        networkBoundResource(
            query = { dao.observeAll().map { it.map(mapper::toDomain) } },
            fetch = { api.getArticles() },
            saveFetchResult = { dtos ->
                dao.upsertAll(dtos.map(mapper::toEntity))
            },
            shouldFetch = { cachedData ->
                // Refresh if cache is empty or stale (>15 min)
                cachedData.isEmpty() || dao.lastUpdated() < System.currentTimeMillis() - 15.minutes.inWholeMilliseconds
            },
        )
}
```

## Connectivity-aware refresh

```kotlin
@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val isOnline: Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService<ConnectivityManager>()!!
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        manager.registerNetworkCallback(request, callback)
        // Emit initial state
        trySend(manager.activeNetwork != null)

        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
```

## Conflict resolution strategies

| Strategy | When to use | Implementation |
|----------|------------|----------------|
| **Last-write-wins** | Simple CRUD, low conflict risk | Server timestamp comparison |
| **Client-wins** | Offline-heavy, user edits matter | Queue local changes, push on reconnect |
| **Server-wins** | Authoritative backend | Always overwrite local on sync |
| **Merge** | Collaborative editing | Field-level diff + merge logic |

For most apps, **last-write-wins** with a `modified_at` timestamp is sufficient.
Implement a `SyncManager` with `WorkManager` for reliable background sync.
