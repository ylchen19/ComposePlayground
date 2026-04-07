# Testing Patterns for Android Clean Architecture

## Table of Contents

- [ViewModel Tests (MVI)](#viewmodel-tests-mvi)
- [UseCase Tests](#usecase-tests)
- [Repository Tests](#repository-tests)
- [Compose UI Tests](#compose-ui-tests)
- [Dependencies](#dependencies)

## Dependencies

Add to `libs.versions.toml`:

```toml
[versions]
junit5 = "5.10.2"
turbine = "1.1.0"
mockk = "1.13.10"
coroutines-test = "1.8.1"

[libraries]
junit5-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit5" }
junit5-engine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junit5" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines-test" }
```

## ViewModel Tests (MVI)

Use Turbine to test `StateFlow` and `Channel` emissions:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ItemListViewModelTest {

    // Replace Main dispatcher — mandatory for ViewModel tests
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherExtension()

    private lateinit var viewModel: ItemListViewModel
    private val fakeRepository = FakeItemRepository()
    private val getItemsUseCase = GetItemsUseCase(fakeRepository)

    @BeforeEach
    fun setup() {
        viewModel = ItemListViewModel(getItemsUseCase)
    }

    @Test
    fun `loading state then data on success`() = runTest {
        val testItems = listOf(Item("1", "Test"))
        fakeRepository.emitItems(testItems)

        viewModel.uiState.test {
            // Skip initial loading state
            val loading = awaitItem()
            assertThat(loading.isLoading).isTrue()

            val success = awaitItem()
            assertThat(success.isLoading).isFalse()
            assertThat(success.items).hasSize(1)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error state on failure`() = runTest {
        fakeRepository.shouldFail = true

        viewModel.onAction(ItemListUiAction.LoadData)

        viewModel.uiState.test {
            skipItems(1) // skip loading
            val errorState = awaitItem()
            assertThat(errorState.error).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `item click emits navigation event`() = runTest {
        viewModel.uiEvent.test {
            viewModel.onAction(ItemListUiAction.ItemClicked("42"))

            val event = awaitItem()
            assertThat(event).isInstanceOf(ItemListUiEvent.NavigateToDetail::class.java)
            assertThat((event as ItemListUiEvent.NavigateToDetail).id).isEqualTo("42")

            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### MainDispatcherExtension (JUnit 5)

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherExtension(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(dispatcher)
    }

    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
    }
}
```

## UseCase Tests

UseCases are pure Kotlin — the simplest to test:

```kotlin
class GetItemsUseCaseTest {

    private val fakeRepository = FakeItemRepository()
    private val useCase = GetItemsUseCase(fakeRepository)

    @Test
    fun `returns mapped domain items`() = runTest {
        fakeRepository.emitItems(listOf(Item("1", "Kotlin")))

        useCase().test {
            val result = awaitItem()
            assertThat(result).hasSize(1)
            assertThat(result.first().name).isEqualTo("Kotlin")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

## Repository Tests

Use in-memory Room + MockWebServer for integration-level tests:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ItemRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ItemDao
    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: ItemApiService
    private lateinit var repo: ItemRepositoryImpl

    @BeforeEach
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.itemDao()

        mockWebServer = MockWebServer()
        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ItemApiService::class.java)

        repo = ItemRepositoryImpl(api, dao, ItemMapper())
    }

    @AfterEach
    fun teardown() {
        db.close()
        mockWebServer.shutdown()
    }

    @Test
    fun `refresh fetches remote and persists to Room`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""[{"id":"1","name":"Test"}]""")
                .setHeader("Content-Type", "application/json")
        )

        repo.refresh()

        dao.observeAll().test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

## Compose UI Tests

```kotlin
class ItemListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `displays items when loaded`() {
        val state = ItemListUiState(
            items = listOf(ItemUiModel("1", "Kotlin"), ItemUiModel("2", "Compose")),
        )

        composeTestRule.setContent {
            ItemListContent(
                uiState = state,
                onAction = {},
            )
        }

        composeTestRule.onNodeWithText("Kotlin").assertIsDisplayed()
        composeTestRule.onNodeWithText("Compose").assertIsDisplayed()
    }

    @Test
    fun `shows loading indicator`() {
        composeTestRule.setContent {
            ItemListContent(
                uiState = ItemListUiState(isLoading = true),
                onAction = {},
            )
        }

        composeTestRule
            .onNode(hasTestTag("loading_indicator"))
            .assertIsDisplayed()
    }

    @Test
    fun `clicking item triggers action`() {
        var capturedAction: ItemListUiAction? = null
        val state = ItemListUiState(
            items = listOf(ItemUiModel("1", "Kotlin")),
        )

        composeTestRule.setContent {
            ItemListContent(
                uiState = state,
                onAction = { capturedAction = it },
            )
        }

        composeTestRule.onNodeWithText("Kotlin").performClick()
        assertThat(capturedAction).isEqualTo(ItemListUiAction.ItemClicked("1"))
    }
}
```

## Fake vs Mock guideline

| Layer | Approach | Reason |
|-------|----------|--------|
| Repository (in ViewModel tests) | **Fake** (in-memory implementation) | Deterministic, no framework coupling |
| API / DAO (in Repository tests) | **Real** (MockWebServer + in-memory Room) | Tests actual serialization + SQL |
| Use cases | **Never mock** | They're simple enough to use directly |
| Android framework (Context, etc.) | **Robolectric** or **Instrumentation** | Can't fake reliably |

## DI framework testing notes

### Recommended: DI-agnostic unit tests

For ViewModel and UseCase tests, **skip the DI framework entirely** and use plain
constructor injection. This works identically for Hilt and Koin:

```kotlin
class ItemListViewModelTest {
    private val fakeRepository = FakeItemRepository()
    private val useCase = GetItemsUseCase(fakeRepository)

    // Direct construction — no Hilt, no Koin, no framework coupling
    private val viewModel = ItemListViewModel(useCase)
}
```

This is the fastest, simplest, and most portable approach.

### Koin: checkModules() in CI

If using Koin, add a module verification test to catch missing bindings early
(since Koin resolves at runtime, not compile-time):

```kotlin
class AllModulesCheckTest : KoinTest {
    @Test
    fun `all modules resolve correctly`() {
        koinApplication {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(networkModule, databaseModule, itemModule, userModule)
        }.checkModules()
    }
}
```

Run this in CI on every PR. It's Koin's equivalent of Hilt's compile-time safety.

### Koin: overriding dependencies in integration tests

```kotlin
@ExtendWith(KoinTestExtension::class)
class ItemFeatureIntegrationTest : KoinTest {

    @JvmField
    @RegisterExtension
    val koinExtension = KoinTestExtension.create {
        modules(
            module {
                single<ItemRepository> { FakeItemRepository() }  // override
                factory { GetItemsUseCase(get()) }
                viewModel { ItemListViewModel(get()) }
            }
        )
    }

    @Test
    fun `full feature flow`() = runTest {
        val viewModel: ItemListViewModel = get()
        // ... test the complete flow
    }
}
```
