# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# ComposePlayground

Android Compose 練習專案，用於探索現代 Android 開發技術棧，並以「合理範圍內的 Clean Architecture」作為架構基線。

## Tech Stack

- **Language:** Kotlin 2.3.21
- **UI:** Jetpack Compose (BOM 2026.04.01) + Material 3
- **DI:** Koin 4.2.1（非 Hilt，無需 KAPT/KSP）
- **Network:** Ktor Client 3.4.3 + OkHttp Engine + Kotlinx Serialization 1.11.0
- **Navigation:** Navigation3 1.1.1（`NavDisplay` + `rememberNavBackStack` + `NavKey`）
- **Image:** Coil 3.4.0
- **Paging:** Paging 3
- **On-device AI:** ML Kit Image Labeling 17.0.9 + ML Kit GenAI Prompt 1.0.0-beta2（Gemini Nano）
- **Build:** AGP 9.2.0, Gradle 9.4.1, Version Catalog (`libs.versions.toml`)
- **Target:** compileSdk 36, minSdk 29

## Build Commands

```bash
./gradlew :app:compileDebugKotlin   # 編譯檢查
./gradlew :app:assembleDebug        # 建置 debug APK
./gradlew test                      # 執行所有 unit tests
./gradlew :app:test                 # 執行 app module unit tests
./gradlew connectedAndroidTest      # 執行 instrumented tests
```

執行單一測試類別：
```bash
./gradlew :app:test --tests "com.example.composeplayground.data.paging.InMemoryPicsumPagingSourceTest"
```

## Architecture

### Clean Architecture 分層

採用「實用主義版」Clean Architecture——**不為了純度引入 UseCase**，但堅守層間依賴方向、DTO/Domain 隔離與依賴反轉：

```
┌─────────────────────────────────────────────┐
│ UI Layer  (Composable + ViewModel)          │  → 觀察 StateFlow，呼叫 ViewModel intent
│   ↓ 依賴                                    │
│ Domain Layer  (Repository interface + Model)│  → 定義業務契約，不知道資料來源
│   ↑ 實作                                    │
│ Data Layer  (RepositoryImpl + Paging + DTO) │  → DTO ↔ Domain 映射發生在這層
│   ↓ 依賴                                    │
│ Network Layer  (ApiService + Ktor + Cache)  │  → 框架細節（:core:network），feature 不知道 Ktor 存在
└─────────────────────────────────────────────┘
```

**依賴反轉**：上層只依賴下層的「介面」，實作由 Koin 在 composition root 組裝。例：`PokemonListViewModel` 只認得 `PokemonRepository` 介面，不知道有 `PokemonRepositoryImpl` 與 `KtorApiService`。

### 模組依賴方向

```
:app  ──→  :core:network
  │
  ├───→  :core:designsystem
  │
  └───→  :core:navigation
```

- `:app → :core:*`，`:core` 模組之間互不依賴
- Feature 程式碼（Pokémon、Picsum）暫時保留在 `:app`，未來會逐步抽成 `:feature:pokemon` / `:feature:picsum`
- **規則**：不要在 `:core:network` 引入任何 feature 概念；不要讓 `:core:designsystem` 依賴 `:core:network`

### 模組職責

| 模組 | 職責 | 典型內容 |
|------|------|---------|
| `:app` | Composition root、navigation、feature 程式碼 | `Application`、`AppNavHost`、`*Module.kt`、各 feature 的 ui/data |
| `:core:network` | HTTP 基礎設施 | `ApiService`、`HttpClientFactory`、`NetworkResult`、`safeApiCall`、`ConnectivityObserver`、`TokenProvider` |
| `:core:designsystem` | Material 3 主題與偏好設定 | `ComposePlaygroundTheme`、`ThemeViewModel`、`DataStoreThemeRepository` |
| `:core:navigation` | 共用導航 Key | `NavKeys.kt`（`@Serializable + NavKey`） |

## Coding Conventions

### 1. DTO ↔ Domain Model 嚴格隔離

**Repository 介面只回傳 Domain Model**，DTO 不外流：

```kotlin
// ❌ 不要
interface PicsumRepository {
    suspend fun fetchPhotos(page: Int): List<PicsumPhotoDto>  // DTO 外洩
}

// ✅ 要這樣
interface PicsumRepository {
    suspend fun fetchPhotos(page: Int, limit: Int): List<PicsumPhoto>  // domain model
}
```

- DTO 命名：`XxxDto` 或 `XxxResponse`，標 `@Serializable`
- Domain Model 命名：純名詞（`Pokemon`、`PicsumPhoto`），標 `@Immutable`
- 映射函式以 extension 形式放在 model 檔尾或 RepositoryImpl 內：`fun XxxDto.toDomain(): XxxDomain`

### 2. Domain Model 一律 `@Immutable`

供 Compose 跳過不必要的重組——`List<String>` 在未標記時會被推斷為 unstable。所有 UI 會消費的 data class 一律加上 `@Immutable`，包含 `UiState`、domain model、callback 載體。

### 3. Repository 模式

- 介面與實作分檔（或同檔但介面在前）：`PokemonRepository` + `PokemonRepositoryImpl`
- 實作層負責：呼叫 `ApiService` → 解包 `NetworkResult` → 映射成 domain model
- 解包失敗時拋例外，由上層（PagingSource / ViewModel）統一捕捉。私用 `private fun NetworkResult<T>.getOrThrow()` extension（參考 `PokemonRepositoryImpl.kt:85`）

### 4. Network 層

- 所有 API 呼叫透過 `ApiService` 介面，**不直接使用 `HttpClient`**
- 呼叫端用 reified extension：`apiService.get<PokemonDetailResponse>("pokemon/$id")`
- 回傳 `NetworkResult<T>`（`Success | Error | Loading`），實作層以 `safeApiCall {}` 統一例外處理（4xx/5xx/IOException/SerializationException）
- **多 Base URL**：每個第三方 API 在 `app/di/NetworkModule.kt` 以 `named("xxxBaseUrl")` + `named("xxxClient")` + `named("xxxApi")` 三件組註冊獨立實例，feature module 用 `get(named("xxxApi"))` 注入

### 5. Paging 3

- 一個 use case 對應一個 `PagingSource` 子類（`PokemonPagingSource`、`TypeFilteredPagingSource`、`PicsumPagingSource`、`InMemoryPicsumPagingSource`）
- `PAGE_SIZE` 定義為 PagingSource 的 `companion object` 常數，避免 magic number 散落
- `params.loadSize` ≠ `PAGE_SIZE` — initial load 會放大成 `initialLoadSize`，若以 `loadSize` 當 API 的 `limit` 會造成跨頁重複（參考 `PicsumPagingSource.kt:25-29` 的註解）
- ViewModel 端以 `Pager(...).flow.cachedIn(viewModelScope)` 包裝，避免設定旋轉時重抓

### 6. ViewModel & UiState

- **單一 `UiState` data class** 收斂所有畫面狀態，標 `@Immutable`
- 對外暴露 `StateFlow<UiState>`，使用 Kotlin 2.x 的 `field` backing property 寫法：
  ```kotlin
  val uiState: StateFlow<MyUiState>
      field = MutableStateFlow(MyUiState())
  ```
- 狀態更新一律 `uiState.update { it.copy(...) }`，不要直接 `.value =`
- 衍生狀態用 `combine` + `flatMapLatest`，不要在 ViewModel 維護重複 source of truth（參考 `PokemonListViewModel.kt:44-64` 用 `uiState.map` 衍生 paging flow）
- 使用者輸入做 `debounce(300)` 再觸發網路（參考 `PokemonListViewModel.kt:45`）

### 7. Compose Screen

- Screen composable **只接收事件 callback**（`onNavigateTo`, `onBack`, `onSelect`），不直接持有 `backStack` 或 `NavController` 引用
- ViewModel 由 `koinViewModel<T>()` 在 `entry<Key>` 內取得，再以參數傳入 Screen
- Screen 內以 `viewModel.uiState.collectAsStateWithLifecycle()` 訂閱
- Composable 命名：頂層頁面叫 `XxxScreen`，子元件放 `components/` 子目錄

### 8. Navigation

新增頁面的兩步驟：
1. 在 `core/navigation/.../NavKeys.kt` 加 `@Serializable data object/class : NavKey`
2. 在 `app/.../navigation/AppNavHost.kt` 的 `entryProvider` 補上 `entry<Key>`

導航規則：
- 前往新頁面：`navigate { backStack.add(Key) }`
- 返回上一頁：`navigate { backStack.removeLastOrNull() }`
- **詳細頁參數直接攜帶在 NavKey**（如 `PicsumDetail(photoId, author, w, h)`），避免詳細頁再回打 API 查單筆
- **點擊穿透防護**：所有導航統一透過 `navigate {}` 包裝，利用 `LocalNavAnimatedContentScope.current.transition.isRunning` 在頁面轉場時封鎖重複點擊（參考 `AppNavHost.kt:55-68`），無需固定延遲

## Dependency Injection（Koin）

### Scope 慣例

- `single { }` — singleton（Repository、ApiService、HttpClient、TokenProvider、CacheConfig）
- `factory { }` — 每次新建（目前未大量使用，必要時用於 stateless helper）
- `viewModel { }` — ViewModel，由 `koinViewModel<T>()` 取得

### Module 拆分原則

每個邏輯邊界一個 module，在 `Application.onCreate()` 統一 `modules(...)`：

| Module | 屬於 | 內容 |
|--------|------|------|
| `coreNetworkModule` | `:core:network` | `HttpClientFactory`、`TokenProvider`、`CacheConfig`、`ConnectivityObserver` |
| `designSystemModule` | `:core:designsystem` | `ThemeRepository`、`ThemeViewModel` |
| `appNetworkModule` | `:app/di` | 各 feature 的 base URL、`HttpClient(named)`、`ApiService(named)` |
| `pokemonModule` | `:app/di` | `PokemonRepository` + 三個 ViewModel |
| `picsumModule` | `:app/di` | `PicsumRepository` + `PicsumImageAnalyzer` + 兩個 ViewModel |

新增 feature 時建立對應 `xxxModule.kt`，並在 `ComposePlaygroundApp.kt` 的 `modules(...)` 註冊。

### 注入導航參數

ViewModel 需要從 NavKey 取得參數（如 `pokemonId`）時：

```kotlin
// Module 註冊
viewModel { params -> PokemonDetailViewModel(pokemonId = params.get(), repository = get()) }

// AppNavHost 內取得
val viewModel = koinViewModel<PokemonDetailViewModel>(
    parameters = { parametersOf(key.pokemonId) },
)
```

### 依賴反轉的實踐

- 介面定義在 domain 層（`PokemonRepository`），實作在 data 層（`PokemonRepositoryImpl`）
- Koin module 是唯一同時知道介面與實作的地方：`single<PokemonRepository> { PokemonRepositoryImpl(get()) }`
- ViewModel 的建構子只接受介面型別，不接受實作

## Testing

- 框架：JUnit 4 + `kotlinx-coroutines-test` (`runTest`)
- PagingSource 測試使用 Paging 3 的 `TestPager`（參考 `InMemoryPicsumPagingSourceTest.kt`）
- 單元測試聚焦 pure logic（PagingSource、domain model、轉換函式），ViewModel 與 Compose 測試暫缺
- 測試命名使用 backtick 描述句：`` `first page has no prevKey and correct nextKey` ``

## Clean Code 原則（專案內已落實）

以下是專案已採用的具體做法，新增程式碼時請延續：

1. **單一資料來源**：UiState 是 ViewModel 的唯一狀態載體，衍生流以 `combine + map` 從 UiState 推導，避免雙寫
2. **不可變優先**：`val` 優先；data class 標 `@Immutable`；UiState 用 `copy()` 更新
3. **錯誤處理集中**：網路錯誤統一在 `safeApiCall` 包成 `NetworkResult.Error`，業務層用 `getOrThrow()` 解包，UI 層由 PagingSource 包成 `LoadResult.Error` 或 ViewModel 寫入 UiState 錯誤欄位
4. **Magic number 命名化**：常數放 `companion object`（`PAGE_SIZE`、`BULK_PAGE_LIMIT`、`BULK_CONCURRENCY`、`MAX_BULK_PAGES`）
5. **註解寫「為什麼」不寫「做什麼」**：函式名與型別已說明 what；註解只在 workaround、非直覺取捨、生命週期陷阱（如 Picsum 排序遮罩生命週期）時出現
6. **不過度抽象**：目前不引入 UseCase 層——Repository 已能承擔業務邏輯；只有當同一邏輯被多個 ViewModel 重複時才考慮抽出
7. **小函式 + 早返回**：複雜流程拆成 private helper（如 `loadAllPhotos()`），避免巢狀超過兩層

## Picsum AI 圖片描述（三層策略）

`PicsumDetail` 頁面在進入時自動分析縮圖，產生一句繁體中文描述，顯示於底部資訊列。

### 介面與實作

- `PicsumImageAnalyzer`：define `suspend fun summarize(photoId, imageUrl): Result<String>`
- `GeminiNanoPicsumImageAnalyzer`：singleton（`picsumModule`），以 `LruCache(128)` 以 photoId 為 key 快取跨頁面結果

### 三層降級策略

| Tier | 條件 | 機制 |
|------|------|------|
| 1 | Pixel 9 Pro+ 支援 Gemini Nano 多模態 | 圖片直接餵給模型，語意理解最準確 |
| 2 | 有 Gemini Nano 但不支援多模態（Pixel 8 / 9a） | ML Kit 抓視覺標籤 → LLM 組句，品質遠優於模板 |
| 3 | 不支援 Gemini Nano | ML Kit 標籤 + 硬編碼中文句子模板，即時可用 |

### 設計重點

- `GeminiNanoModelManager` (singleton)：模型生命週期單一管理者，對外暴露 `status: StateFlow<ModelStatus>`，內部以 `Mutex + Deferred` 防止 detail 與 settings 並發觸發雙下載
- 兩個入口：Settings 主動下載（`startDownload()`）；詳細頁懶載入（`ensureReady()` 自動觸發並等候）
- Gemini Nano 回應過濾：逐行取第一個有效中文句（6–60 字，排除 Unicode 表格線）——Samsung 裝置有時輸出表格格式
- Prompt 設計：短指令（小模型用短 prompt 比長 prompt 更穩定）+ 明確禁止表格/條列

### ViewModel 整合

```kotlin
// PicsumDetailViewModel：本地分析狀態 + manager 下載狀態 combine 成 UI 狀態
val uiState = combine(analysisState, modelManager.status) { local, model ->
    val summary = when {
        local is ImageSummaryState.Loading && model is ModelStatus.Downloading ->
            ImageSummaryState.Downloading(model.progress)
        else -> local
    }
    PicsumDetailUiState(photo, summary)
}.stateIn(viewModelScope, SharingStarted.Eagerly, PicsumDetailUiState(photo))
```

`ImageSummaryState`：`Idle | Loading | Downloading(progress) | Success | Error`，標 `@Immutable`
`ModelStatus`：`Unknown | NotSupported | Downloadable | Downloading(progress) | Ready | Failed(message)`

## Picsum 排序策略（非直覺實作）

`/v2/list` 不支援排序，採用**批次平行抓取 + in-memory cache**：

- 切換到需排序的模式時，以 `Semaphore(6)` 限流批次平行抓取全部照片（~1084 張，limit=100/頁，~600ms）
- 抓完後寫入 `allPhotosCache`，後續切換排序模式全部 in-memory 即時
- **遮罩生命週期陷阱**：資料就緒後**先** `setLoadingAll(false)` 關掉遮罩，**再** `emitAll(Pager.flow)`。`Pager.flow` 是 hot flow 永不完成，若把 `setLoadingAll(false)` 只放在 `finally`，遮罩會一直卡在畫面上
- 關鍵常數定義於 `PicsumGalleryViewModel.companion`：`BULK_PAGE_LIMIT=100`、`BULK_CONCURRENCY=6`、`MAX_BULK_PAGES=30`

完整策略與替代方案比較見 `README.md` 的「Picsum 排序策略」章節。
