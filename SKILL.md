---
name: android-clean-arch
description: >-
  Build production-ready Android features using Clean Architecture (UI → Domain → Data),
  Jetpack Compose, Kotlin Coroutines/Flow, Hilt or Koin DI, and MVI/MVVM patterns. Use this
  skill whenever the user asks to create an Android screen, feature module, ViewModel,
  Repository, UseCase, API integration, Room database entity, Compose UI component, navigation
  setup, dependency injection setup, or any Android architecture question. Also trigger when
  the user mentions Kotlin, Compose, Hilt, Koin, Dagger, DI, Retrofit, Room, DataStore,
  Proto DataStore, WorkManager, Paging 3, KMM, KMP, or Android testing (unit test, UI test,
  instrumentation test). This skill covers the full Modern Android Development (MAD) stack
  for enterprise-grade apps targeting Android 14+/SDK 34+.
license: Apache-2.0
metadata:
  author: android-senior-engineer
  version: "1.0"
  min-sdk: "26"
  target-sdk: "35"
compatibility: >-
  Designed for Android Studio Ladybug or later. Requires JDK 17+, Gradle 8.5+,
  AGP 8.5+, and Kotlin 2.0+. Works with Claude Code, GitHub Copilot, Cursor,
  Gemini CLI, or any agent supporting the agentskills.io spec.
---

# Android Clean Architecture Skill

Build scalable, testable Android features using the **Modern Android Development (MAD)** stack.

## When to activate

- User wants to create a new feature / screen / module
- User asks about architecture decisions (MVVM vs MVI, module boundaries)
- User needs Compose UI with proper state management
- User needs data layer setup (API + local DB + repository)
- User asks about testing strategies
- User mentions performance, memory leaks, or lifecycle issues

## Core Architecture: Clean Architecture + MVI

```
┌─────────────────────────────────────────────┐
│  UI Layer (Compose)                         │
│  Screen → ViewModel → UiState / UiEvent     │
├─────────────────────────────────────────────┤
│  Domain Layer (Pure Kotlin)                 │
│  UseCase → Repository Interface → Model     │
├─────────────────────────────────────────────┤
│  Data Layer                                 │
│  RepositoryImpl → RemoteDataSource / Local  │
│  ApiService (Retrofit) / Dao (Room)         │
└─────────────────────────────────────────────┘
```

**Dependency rule**: Dependencies point inward only. Domain NEVER depends on Data or UI.

## Step 1: Feature module structure

Every feature follows this package layout. Use this as the default scaffold:

```
feature-{name}/
├── data/
│   ├── remote/
│   │   ├── dto/          # API response DTOs (keep thin, map to domain)
│   │   └── {Name}ApiService.kt
│   ├── local/
│   │   ├── entity/       # Room entities
│   │   └── {Name}Dao.kt
│   ├── mapper/           # DTO ↔ Domain ↔ Entity mappers
│   └── {Name}RepositoryImpl.kt
├── domain/
│   ├── model/            # Domain models (data class, no framework deps)
│   ├── repository/       # Repository interfaces
│   └── usecase/          # Single-responsibility use cases
└── ui/
    ├── {Name}Screen.kt       # Compose entry point
    ├── {Name}ViewModel.kt    # MVI ViewModel
    ├── {Name}UiState.kt      # Sealed state + events
    ├── component/             # Reusable Compose components
    └── navigation/            # NavGraph registration
```

## Step 2: MVI state management pattern

Use this exact pattern for every ViewModel. It enforces a single source of truth and unidirectional data flow.

```kotlin
// ── UiState: immutable snapshot of the screen ──
data class {Name}UiState(
    val isLoading: Boolean = false,
    val items: List<ItemUiModel> = emptyList(),
    val error: UiText? = null, // UiText wraps string res + dynamic text
)

// ── UiEvent: one-shot side effects (snackbar, navigation) ──
sealed interface {Name}UiEvent {
    data class ShowSnackbar(val message: UiText) : {Name}UiEvent
    data class NavigateToDetail(val id: String) : {Name}UiEvent
}

// ── UiAction: user intents flowing INTO the ViewModel ──
sealed interface {Name}UiAction {
    data object LoadData : {Name}UiAction
    data object Refresh : {Name}UiAction
    data class ItemClicked(val id: String) : {Name}UiAction
}
```

```kotlin
// ── ViewModel (Hilt version) ──
@HiltViewModel
class {Name}ViewModel @Inject constructor(
    private val getItemsUseCase: GetItemsUseCase,
) : ViewModel() {
// ... (same body below)

// ── ViewModel (Koin version) ──
class {Name}ViewModel(
    private val getItemsUseCase: GetItemsUseCase,
) : ViewModel() {
// ... (same body below)

    private val _uiState = MutableStateFlow({Name}UiState())
    val uiState: StateFlow<{Name}UiState> = _uiState.asStateFlow()

    // Channel for one-shot events — never use SharedFlow for navigation/snackbar
    private val _uiEvent = Channel<{Name}UiEvent>()
    val uiEvent: Flow<{Name}UiEvent> = _uiEvent.receiveAsFlow()

    init { onAction({Name}UiAction.LoadData) }

    fun onAction(action: {Name}UiAction) {
        when (action) {
            is {Name}UiAction.LoadData -> loadData()
            is {Name}UiAction.Refresh -> loadData()
            is {Name}UiAction.ItemClicked -> {
                viewModelScope.launch {
                    _uiEvent.send({Name}UiEvent.NavigateToDetail(action.id))
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getItemsUseCase()
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.toUiText())
                    }
                }
                .collect { items ->
                    _uiState.update {
                        it.copy(isLoading = false, items = items.toUiModels())
                    }
                }
        }
    }
}
```

## Step 3: Compose screen wiring

```kotlin
@Composable
fun {Name}Screen(
    // Hilt: viewModel = hiltViewModel()
    // Koin:  viewModel = koinViewModel()
    viewModel: {Name}ViewModel = hiltViewModel(), // or koinViewModel()
    onNavigateToDetail: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Collect one-shot events — lifecycle-aware, no leaks
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is {Name}UiEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(event.message.asString(context))
                is {Name}UiEvent.NavigateToDetail ->
                    onNavigateToDetail(event.id)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        {Name}Content(
            uiState = uiState,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(padding),
        )
    }
}

// Stateless content — easy to preview and test
@Composable
private fun {Name}Content(
    uiState: {Name}UiState,
    onAction: ({Name}UiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Compose UI here — always pass onAction down, never the ViewModel
}
```

## Step 4: Domain layer (pure Kotlin)

```kotlin
// ── UseCase: single responsibility, injectable ──
// Hilt: use @Inject constructor
// Koin: plain constructor (registered in Koin module)
class GetItemsUseCase(
    private val repository: ItemRepository,
) {
    operator fun invoke(): Flow<List<Item>> = repository.getItems()
}

// ── Repository interface: belongs to domain, zero framework deps ──
interface ItemRepository {
    fun getItems(): Flow<List<Item>>
    suspend fun getItemById(id: String): Item
    suspend fun refresh()
}
```

## Step 5: Data layer

```kotlin
// ── RepositoryImpl: single source of truth pattern ──
// Hilt: add @Inject constructor
// Koin: plain constructor (registered in Koin module)
class ItemRepositoryImpl(
    private val api: ItemApiService,
    private val dao: ItemDao,
    private val mapper: ItemMapper,
) : ItemRepository {

    // Room as single source of truth, API refreshes cache
    override fun getItems(): Flow<List<Item>> =
        dao.observeAll().map { entities -> entities.map(mapper::toDomain) }

    override suspend fun refresh() {
        val dtos = api.fetchItems()
        dao.upsertAll(dtos.map(mapper::toEntity))
    }
}
```

## Step 6: Dependency Injection — Hilt vs Koin

### Decision matrix: which DI framework?

| Criteria | **Hilt** | **Koin** |
|---|---|---|
| 類型安全 | 編譯期檢查，缺少綁定直接報錯 | 執行期解析，缺少綁定 runtime crash |
| KMM / KMP 共享 | Android only，無法跨平台共用 | **原生支援 KMP**，DI 定義可共用 |
| 學習曲線 | 較高（Dagger 底層、annotation processing） | 較低（純 Kotlin DSL，無 codegen） |
| 建置速度 | 較慢（KSP/KAPT codegen） | **較快**（零 codegen） |
| Google 官方支援 | **官方推薦**、與 Jetpack 深度整合 | 社群維護，但生態完善 |
| 多模組大型專案 | 強項（Component hierarchy 天然隔離） | 需自行管理 module scope |
| 測試便利性 | 需 HiltTestApplication 或 uninstall module | **極簡**：直接覆蓋 module 即可 |

**建議原則**：
- 純 Android 大型專案、需要編譯期安全 → **Hilt**
- KMM/KMP 專案、中小團隊快速迭代、偏好 DSL → **Koin**

### Hilt wiring

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class {Name}Module {
    @Binds
    abstract fun bindRepository(impl: ItemRepositoryImpl): ItemRepository
}

@Module
@InstallIn(SingletonComponent::class)
object {Name}NetworkModule {
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ItemApiService =
        retrofit.create(ItemApiService::class.java)
}
```

### Koin wiring

```kotlin
// ── feature module ──
val itemFeatureModule = module {
    // Data
    single<ItemApiService> { get<Retrofit>().create(ItemApiService::class.java) }
    single<ItemRepository> { ItemRepositoryImpl(get(), get(), get()) }
    factory { ItemMapper() }

    // Domain
    factory { GetItemsUseCase(get()) }

    // UI
    viewModel { {Name}ViewModel(get()) }
}

// ── Application.kt ──
class MyApp : Application(), KoinComponent {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApp)
            modules(
                coreModule,       // Retrofit, Room, OkHttp
                itemFeatureModule, // per-feature module
            )
        }
    }
}

// ── Compose screen injection ──
// import org.koin.androidx.compose.koinViewModel
@Composable
fun {Name}Screen(
    viewModel: {Name}ViewModel = koinViewModel(),
) { ... }
```

For full Koin patterns including scoped modules, KMP shared DI, and testing overrides,
see [references/koin-patterns.md](references/koin-patterns.md).

## Gotchas

- **Never collect Flow in ViewModel using `collect {}` without `viewModelScope.launch`**.
  Forgetting the coroutine scope leaks the collection.
- **Use `Channel` (not `SharedFlow`) for one-shot UI events** (snackbar, navigation).
  SharedFlow with `replay=0` can lose events on config change. Channel buffers them.
- **`collectAsStateWithLifecycle()` requires `lifecycle-runtime-compose` dependency**.
  Without it, the flow keeps collecting when the app is backgrounded → battery drain.
- **Room DAO `Flow` queries auto-requery on data change**. Don't wrap them in
  `flow { emit(dao.getAll()) }` — that defeats Room's reactivity.
- **Mapper classes should be `@Inject constructor()` (Hilt) or `factory { }` (Koin) with no state**.
  Don't make mappers `object` — it blocks testing with fakes.
- **Android 14+ requires `FOREGROUND_SERVICE_*` type in manifest** for foreground services.
  Missing it crashes silently on older targetSdk but throws on 34+.
- **Compose `LazyColumn` keys must be unique AND stable across recompositions**.
  Using `items.indexOf(item)` as key causes full recomposition on list changes.
- **Hilt `@Binds` must be in an `abstract class` module, `@Provides` in an `object` module**.
  Mixing them in the same module causes compilation errors.
- **Koin `single` vs `factory` vs `viewModel` scope matters**:
  `single` = app 級單例, `factory` = 每次新建, `viewModel` = 跟隨 ViewModel lifecycle.
  UseCase 用 `factory`，Repository 用 `single`，ViewModel 用 `viewModel`.
- **Koin 缺少綁定只會在 runtime crash，不會在編譯期報錯**.
  務必在 test 中加 `checkModules {}` 驗證所有依賴都能正確解析。
- **Koin `androidContext()` 必須在 `startKoin {}` 內呼叫**.
  在 module DSL 裡用 `androidContext()` 取得 Context，不要自己傳。

## Compose performance checklist

Before shipping any Compose screen:

- [ ] `LazyColumn`/`LazyRow` items have stable `key`
- [ ] Heavy computations wrapped in `remember { }` or `derivedStateOf { }`
- [ ] No lambda allocations in hot paths (use `remember { { ... } }` or method references)
- [ ] Image loading uses Coil `AsyncImage` with `placeholder` and `crossfade`
- [ ] Lists >50 items use `@Immutable` or `@Stable` on data classes
- [ ] Layout Inspector shows no unnecessary recompositions

## Testing strategy

See [references/testing.md](references/testing.md) for full testing patterns.

Quick summary:
- **Unit tests**: UseCase, ViewModel, Mapper — use Turbine for Flow, MockK for fakes
- **Integration tests**: Repository with in-memory Room + MockWebServer
- **UI tests**: Compose testing APIs with `createComposeRule()`

## Navigation (Type-Safe)

See [references/navigation.md](references/navigation.md) for Compose Navigation with
type-safe args (Kotlin Serialization route pattern, recommended for new projects).

## When to consult references

| Situation | Read |
|---|---|
| Need Koin DI patterns, KMP DI, testing overrides | [references/koin-patterns.md](references/koin-patterns.md) |
| Need Paging 3 setup with Compose | [references/paging.md](references/paging.md) |
| Need offline-first / caching strategy | [references/offline-first.md](references/offline-first.md) |
| Need testing patterns | [references/testing.md](references/testing.md) |
| Need type-safe navigation | [references/navigation.md](references/navigation.md) |
| Need Gradle version catalog setup | [references/gradle-setup.md](references/gradle-setup.md) |
