# ComposePlayground

Android Compose 練習專案，用於探索現代 Android 開發技術棧。

## Tech Stack

- **Language:** Kotlin 2.3.20
- **UI:** Jetpack Compose (BOM 2026.03.01) + Material 3
- **DI:** Koin 4.1.0（非 Hilt，無需 KAPT/KSP）
- **Network:** Ktor Client 3.1.3 + OkHttp Engine + Kotlinx Serialization 1.8.1
- **Navigation:** Navigation3 1.0.1（`NavDisplay` + `rememberNavBackStack` + `NavKey`）
- **Image:** Coil Compose 2.7.0
- **Build:** AGP 9.1.0, Gradle 9.3.1, Version Catalog (`libs.versions.toml`)
- **Target:** compileSdk 36, minSdk 29

## Project Structure

單模組 `:app`，套件結構如下：

```
com.example.composeplayground/
├── ComposePlaygroundApp.kt        # Application，初始化 Koin (appModule, networkModule, pokemonModule, picsumModule)
├── MainActivity.kt                # 入口 Activity，載入 AppNavHost
├── di/
│   ├── AppModule.kt               # 基本 Koin module
│   ├── NetworkModule.kt           # 網路層 Koin module（HttpClient×2, ApiService×2, Token, Cache）
│   ├── PokemonModule.kt           # Pokémon 功能 Koin module
│   └── PicsumModule.kt            # Picsum 圖庫 Koin module
├── navigation/
│   ├── NavKeys.kt                 # 導航 Key（@Serializable + NavKey）
│   └── AppNavHost.kt              # NavDisplay + entryProvider + ViewModel/State decorators
├── network/
│   ├── NetworkResult.kt           # sealed interface Success/Error/Loading + safeApiCall
│   ├── api/
│   │   ├── ApiService.kt          # HTTP 操作介面 + inline reified extensions
│   │   └── KtorApiService.kt      # Ktor 實作
│   ├── auth/
│   │   ├── TokenProvider.kt       # Token 管理介面
│   │   └── InMemoryTokenProvider.kt
│   ├── cache/
│   │   └── CacheConfig.kt         # OkHttp disk cache 設定
│   ├── client/
│   │   └── HttpClientFactory.kt   # Ktor HttpClient 工廠（Auth, Logging, ContentNegotiation）
│   └── connectivity/
│       ├── ConnectivityObserver.kt # 連線監聽介面（StateFlow + Flow）
│       └── NetworkConnectivityObserver.kt
├── data/
│   ├── model/
│   │   ├── PokemonApiModels.kt    # Pokémon API DTO
│   │   ├── PokemonDomainModels.kt # @Immutable Pokémon domain models
│   │   └── PicsumModels.kt        # PicsumPhotoDto + @Immutable PicsumPhoto（含 thumbnailUrl/fullSizeUrl）
│   ├── paging/
│   │   ├── PokemonPagingSource.kt
│   │   ├── TypeFilteredPagingSource.kt
│   │   └── PicsumPagingSource.kt  # page 1-indexed，空陣列代表最後一頁
│   └── repository/
│       ├── PokemonRepository.kt
│       ├── PokemonRepositoryImpl.kt
│       ├── PicsumRepository.kt    # interface
│       └── PicsumRepositoryImpl.kt
└── ui/
    ├── screen/
    │   ├── HomeMenuScreen.kt       # 首頁菜單，列出 Pokémon / Picsum 入口
    │   ├── SettingsScreen.kt
    │   ├── pokemon/               # Pokémon 功能模組
    │   │   ├── PokemonListScreen.kt
    │   │   ├── PokemonListViewModel.kt
    │   │   ├── PokemonDetailScreen.kt
    │   │   ├── PokemonDetailViewModel.kt
    │   │   ├── PokemonTypeGalleryScreen.kt
    │   │   ├── PokemonTypeGalleryViewModel.kt
    │   │   └── components/
    │   │       ├── PokemonGridCard.kt
    │   │       ├── PokemonListItem.kt
    │   │       ├── PokemonTypeChip.kt
    │   │       ├── ShimmerEffect.kt
    │   │       └── UniformHeightLazyRow.kt
    │   └── picsum/                # Picsum 圖庫模組
    │       ├── PicsumGalleryScreen.kt   # 兩欄等高 grid，1080×1080 大圖縮圖
    │       ├── PicsumGalleryViewModel.kt
    │       ├── PicsumDetailScreen.kt    # 全螢幕大圖 + pinch-zoom
    │       ├── PicsumDetailViewModel.kt
    │       └── components/
    │           ├── PicsumGridCard.kt    # 1:1 卡片，底部作者資訊
    │           └── ZoomableAsyncImage.kt # detectTransformGestures pinch-zoom（1x–5x）
    └── theme/                      # Material 3 主題（Color, Theme, Type）
```

## Build Commands

```bash
./gradlew :app:compileDebugKotlin   # 編譯檢查
./gradlew :app:assembleDebug        # 建置 debug APK
./gradlew test                      # 執行 unit tests
./gradlew connectedAndroidTest      # 執行 instrumented tests
```

## Conventions

- **DI:** 所有依賴透過 Koin module 註冊，使用 `single {}` (singleton) / `factory {}` (每次新建) / `viewModel {}`
- **Navigation:** 新頁面需在 `NavKeys.kt` 加 `@Serializable data object/class : NavKey`，並在 `AppNavHost.kt` 的 `entryProvider` 中加 `entry<Key>`
- **Network:** API 呼叫統一透過 `ApiService` 介面，回傳 `NetworkResult<T>`，使用 `safeApiCall` 包裝錯誤處理
- **多 API Base URL:** 每個第三方 API 在 `NetworkModule.kt` 以 `named("xxxBaseUrl")` + `named("xxxClient")` + `named("xxxApi")` 建立獨立的 HttpClient / ApiService 組合，再於對應的 `XxxModule.kt` 注入
- **Compose:** Screen composable 接收事件回呼（`onNavigateTo`, `onBack`），不直接持有 back stack 引用
- **詳細頁導航參數:** 優先透過 NavKey 攜帶必要欄位（id/primitive fields），避免詳細頁再回打 API 查單筆
