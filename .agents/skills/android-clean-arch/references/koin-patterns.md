# Koin DI Patterns for Android Clean Architecture

## Table of Contents

- [Dependencies](#dependencies)
- [Core module structure](#core-module-structure)
- [Feature module pattern](#feature-module-pattern)
- [Scoped modules](#scoped-modules)
- [Named & qualifier bindings](#named--qualifier-bindings)
- [Koin with Compose](#koin-with-compose)
- [KMP shared DI](#kmp-shared-di)
- [Testing with Koin](#testing-with-koin)
- [Migration from Hilt to Koin](#migration-from-hilt-to-koin)

## Dependencies

`libs.versions.toml`:

```toml
[versions]
koin = "4.0.2"
koin-compose = "4.0.2"

[libraries]
# Core
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }

# Compose integration
koin-compose = { module = "io.insert-koin:koin-androidx-compose", version.ref = "koin-compose" }

# WorkManager integration
koin-workmanager = { module = "io.insert-koin:koin-androidx-workmanager", version.ref = "koin" }

# Testing
koin-test = { module = "io.insert-koin:koin-test", version.ref = "koin" }
koin-test-junit5 = { module = "io.insert-koin:koin-test-junit5", version.ref = "koin" }

# KMP (if using Kotlin Multiplatform)
koin-core-kmp = { module = "io.insert-koin:koin-core", version.ref = "koin" }

[bundles]
koin = ["koin-core", "koin-android", "koin-compose"]
koin-test = ["koin-test", "koin-test-junit5"]
```

Module `build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.bundles.koin)
    testImplementation(libs.bundles.koin.test)
}
```

**No KSP / KAPT / annotation processing needed.** This is Koin's main build-speed advantage.

## Core module structure

Organize modules by architectural layer, not by feature:

```kotlin
// ── core/di/NetworkModule.kt ──
val networkModule = module {
    single {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) BODY else NONE
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(get<String>(named("baseUrl")))
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    // Provide base URL as named parameter — easy to swap in tests
    single(named("baseUrl")) { "https://api.example.com/" }
}

// ── core/di/DatabaseModule.kt ──
val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "app_database",
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    // Expose individual DAOs
    single { get<AppDatabase>().itemDao() }
    single { get<AppDatabase>().userDao() }
}
```

## Feature module pattern

One Koin module per feature — register Data, Domain, and UI layers together:

```kotlin
// ── feature/item/di/ItemModule.kt ──
val itemModule = module {
    // Data layer
    single<ItemApiService> { get<Retrofit>().create(ItemApiService::class.java) }
    factory { ItemMapper() }
    single<ItemRepository> { ItemRepositoryImpl(get(), get(), get()) }

    // Domain layer — UseCases are factory (stateless, cheap to create)
    factory { GetItemsUseCase(get()) }
    factory { GetItemByIdUseCase(get()) }
    factory { RefreshItemsUseCase(get()) }

    // UI layer — viewModel follows ViewModel lifecycle
    viewModel { ItemListViewModel(get()) }
    viewModel { (itemId: String) ->  // parametrized ViewModel
        ItemDetailViewModel(itemId, get())
    }
}
```

Application setup:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@MyApp)
            modules(
                // Core
                networkModule,
                databaseModule,
                // Features — add new features here
                itemModule,
                userModule,
                settingsModule,
            )
        }
    }
}
```

## Scoped modules

Use `scope` for dependencies tied to a specific lifecycle (e.g., logged-in session):

```kotlin
val sessionModule = module {
    // Scope tied to a session — all dependencies inside share the same lifecycle
    scope<UserSession> {
        scoped { UserPreferences(get()) }
        scoped { AuthTokenProvider(get()) }
        scoped<ProfileRepository> { ProfileRepositoryImpl(get(), get()) }
    }
}

// Open scope on login
class AuthManager : KoinComponent {
    private var sessionScope: Scope? = null

    fun onLogin(session: UserSession) {
        sessionScope = getKoin().createScope<UserSession>("session_scope")
    }

    fun onLogout() {
        sessionScope?.close() // All scoped instances are destroyed
        sessionScope = null
    }
}
```

## Named & qualifier bindings

When you have multiple implementations of the same interface:

```kotlin
val dispatcherModule = module {
    single(named("io")) { Dispatchers.IO }
    single(named("default")) { Dispatchers.Default }
    single(named("main")) { Dispatchers.Main }
}

// Usage in class
class ItemRepositoryImpl(
    private val api: ItemApiService,
    private val ioDispatcher: CoroutineDispatcher, // injected via named
) : ItemRepository {
    override suspend fun refresh() = withContext(ioDispatcher) { ... }
}

// Wiring
single<ItemRepository> {
    ItemRepositoryImpl(get(), get(named("io")))
}
```

Using custom qualifiers (type-safe alternative to strings):

```kotlin
// Define qualifier
val IoDispatcher = named("IoDispatcher")
val DefaultDispatcher = named("DefaultDispatcher")

// Or use Koin's StringQualifier
val itemApi = named("itemApi")
val userApi = named("userApi")

val module = module {
    single<ApiService>(itemApi) { ItemApiServiceImpl() }
    single<ApiService>(userApi) { UserApiServiceImpl() }
}
```

## Koin with Compose

```kotlin
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun ItemListScreen(
    viewModel: ItemListViewModel = koinViewModel(),
    onNavigateToDetail: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ... same Compose patterns as Hilt version
}

// For parametrized ViewModels
@Composable
fun ItemDetailScreen(
    itemId: String,
    viewModel: ItemDetailViewModel = koinViewModel(
        parameters = { parametersOf(itemId) },
    ),
) { ... }

// Inject non-ViewModel dependencies in Compose
@Composable
fun SettingsScreen() {
    val analytics: AnalyticsTracker = koinInject()
    // ...
}
```

## KMP shared DI

Koin's biggest advantage — share DI definitions across Android and iOS:

```kotlin
// ── shared/src/commonMain/kotlin/di/SharedModule.kt ──
val sharedModule = module {
    // Domain (pure Kotlin, works everywhere)
    factory { GetItemsUseCase(get()) }
    factory { GetItemByIdUseCase(get()) }

    // Data (shared interfaces, platform-specific impls)
    single<ItemRepository> { ItemRepositoryImpl(get(), get()) }
}

// ── shared/src/androidMain/kotlin/di/PlatformModule.kt ──
actual val platformModule = module {
    single<DatabaseDriver> { AndroidSqliteDriver(AppDatabase.Schema, get(), "app.db") }
    single { ItemLocalDataSource(get()) }
}

// ── shared/src/iosMain/kotlin/di/PlatformModule.kt ──
actual val platformModule = module {
    single<DatabaseDriver> { NativeSqliteDriver(AppDatabase.Schema, "app.db") }
    single { ItemLocalDataSource(get()) }
}

// ── Android Application ──
startKoin {
    androidContext(this@MyApp)
    modules(sharedModule, platformModule, androidUiModule)
}

// ── iOS entry point (Swift) ──
// KoinHelperKt.doInitKoin() // called from Swift AppDelegate
```

Kotlin side helper for iOS:

```kotlin
// shared/src/iosMain/kotlin/di/KoinHelper.kt
fun initKoin() {
    startKoin {
        modules(sharedModule, platformModule)
    }
}
```

## Testing with Koin

### Module verification (compile-time-like safety)

```kotlin
class KoinModuleCheckTest : KoinTest {

    @Test
    fun `verify all modules`() {
        koinApplication {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(
                networkModule,
                databaseModule,
                itemModule,
                userModule,
            )
        }.checkModules()  // Throws if any dependency is missing
    }
}
```

**Run this test in CI.** It compensates for Koin's lack of compile-time checking.

### Override dependencies in tests

```kotlin
@ExtendWith(KoinTestExtension::class)
class ItemListViewModelTest : KoinTest {

    private val fakeRepository = FakeItemRepository()

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(
            module {
                // Override real repository with fake
                single<ItemRepository> { fakeRepository }
                factory { GetItemsUseCase(get()) }
                viewModel { ItemListViewModel(get()) }
            }
        )
    }

    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherExtension()

    @Test
    fun `loads items successfully`() = runTest {
        fakeRepository.emitItems(listOf(Item("1", "Test")))
        val viewModel: ItemListViewModel = get() // resolve from Koin

        viewModel.uiState.test {
            skipItems(1) // loading
            val success = awaitItem()
            assertThat(success.items).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### Simpler approach: no Koin in unit tests

For ViewModel tests, you can skip Koin entirely and use constructor injection:

```kotlin
class ItemListViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherExtension()

    private val fakeRepository = FakeItemRepository()
    private val useCase = GetItemsUseCase(fakeRepository)

    @Test
    fun `loads items`() = runTest {
        val viewModel = ItemListViewModel(useCase) // direct construction
        // ... test as usual
    }
}
```

This is the recommended approach: Koin handles wiring in production, but tests use
plain constructors. It's faster, simpler, and proves your classes have clean dependencies.

## Migration from Hilt to Koin

| Hilt | Koin equivalent |
|------|----------------|
| `@HiltViewModel` | Remove annotation, register with `viewModel { }` |
| `@Inject constructor(...)` | Remove annotation, plain `class Foo(...)` |
| `@Module @InstallIn(SingletonComponent)` | `module { single { ... } }` |
| `@Binds` | `single<Interface> { Impl(get()) }` |
| `@Provides` | `single { ... }` or `factory { ... }` |
| `@Singleton` | `single { ... }` (default is singleton) |
| `@ViewModelScoped` | `viewModel { ... }` (auto-scoped) |
| `hiltViewModel()` | `koinViewModel()` |
| `@EntryPoint` | `KoinComponent` interface + `get()` |
| `HiltAndroidApp` | `startKoin { }` in `Application.onCreate()` |

### Migration steps

1. Add Koin dependencies alongside Hilt (both can coexist temporarily)
2. Create Koin modules mirroring Hilt `@Module` classes
3. Migrate feature by feature: remove `@Inject`/`@HiltViewModel`, add Koin module entries
4. Update Compose screens: `hiltViewModel()` → `koinViewModel()`
5. Replace `@HiltAndroidApp` with `startKoin` in Application
6. Remove Hilt dependencies and KSP/KAPT plugin
7. Run `checkModules()` test to verify all bindings

## Gotchas

- **`get()` in Koin module DSL resolves lazily at call time, not at module declaration**.
  Circular dependencies won't be caught until first resolution → use `checkModules()`.
- **`viewModel { }` must be used for ViewModel, not `single { }` or `factory { }`**.
  Using `single` makes the ViewModel survive configuration changes incorrectly (never recreated).
  Using `factory` creates a new ViewModel on every recomposition.
- **Koin 4.x changed the Compose artifact name**: it's now `koin-androidx-compose` (not
  `koin-android-compose` or `koin-compose`). Wrong artifact = class not found at runtime.
- **Don't call `startKoin` twice**. In tests, use `koinApplication { }` or
  `KoinTestExtension`. Double initialization throws.
- **`androidContext()` is only available inside `startKoin { }` block**, not inside
  `module { }`. In module DSL, use `androidContext()` function (imported from koin-android)
  to retrieve it.
