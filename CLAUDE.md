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

多模組架構：`:app` + `:core:network` + `:core:designsystem` + `:core:navigation`。Feature 程式碼（Pokémon、Picsum）暫時保留在 `:app`，未來會逐步抽成 `:feature:pokemon` / `:feature:picsum`。

```
ComposePlayground/
├── app/                                          # application module
│   └── src/main/java/com/example/composeplayground/
│       ├── ComposePlaygroundApp.kt               # Application，初始化 Koin
│       │                                         # (coreNetworkModule, designSystemModule,
│       │                                         #  appNetworkModule, pokemonModule, picsumModule)
│       ├── MainActivity.kt
│       ├── di/
│       │   ├── NetworkModule.kt                  # appNetworkModule：feature URL / HttpClient / ApiService 實例
│       │   ├── PokemonModule.kt
│       │   └── PicsumModule.kt
│       ├── navigation/
│       │   └── AppNavHost.kt                     # NavDisplay + entryProvider + ViewModel/State decorators
│       ├── data/
│       │   ├── model/                            # PokemonApiModels, PokemonDomainModels, PicsumModels
│       │   ├── paging/                           # PokemonPagingSource, TypeFilteredPagingSource, PicsumPagingSource
│       │   └── repository/                       # PokemonRepository(Impl), PicsumRepository(Impl)
│       └── ui/screen/
│           ├── HomeMenuScreen.kt
│           ├── SettingsScreen.kt
│           ├── pokemon/                          # List / Detail / TypeGallery + ViewModel + components
│           └── picsum/                           # Gallery / Detail + ViewModel + components
│
├── core/
│   ├── network/                                  # 網路層基礎設施（無 feature 相依）
│   │   └── src/main/kotlin/com/example/composeplayground/network/
│   │       ├── NetworkResult.kt                  # sealed interface Success/Error/Loading + safeApiCall
│   │       ├── api/
│   │       │   ├── ApiService.kt                 # HTTP 操作介面 + inline reified extensions
│   │       │   └── KtorApiService.kt             # Ktor 實作
│   │       ├── auth/
│   │       │   ├── TokenProvider.kt
│   │       │   └── InMemoryTokenProvider.kt
│   │       ├── cache/CacheConfig.kt              # OkHttp disk cache 設定
│   │       ├── client/HttpClientFactory.kt       # Ktor HttpClient 工廠（Auth, Logging, ContentNegotiation）
│   │       ├── connectivity/                     # ConnectivityObserver(+ NetworkConnectivityObserver)
│   │       └── di/CoreNetworkModule.kt           # coreNetworkModule（HttpClientFactory, TokenProvider, CacheConfig 等）
│   │
│   ├── designsystem/                             # Material 3 主題與相關 ViewModel
│   │   └── src/main/kotlin/com/example/composeplayground/ui/theme/
│   │       ├── Color.kt / Theme.kt / Type.kt     # ComposePlaygroundTheme
│   │       ├── ThemeConfig.kt                    # DarkModeOption + ThemeConfig data class
│   │       ├── ThemeRepository.kt
│   │       ├── DataStoreThemeRepository.kt       # DataStore Preferences 實作（檔名 theme_preferences）
│   │       ├── ThemeViewModel.kt
│   │       └── di/DesignSystemModule.kt          # designSystemModule
│   │
│   └── navigation/                               # 共用導航 Key
│       └── src/main/kotlin/com/example/composeplayground/navigation/
│           └── NavKeys.kt                        # @Serializable + NavKey
```

依賴方向：`:app → :core:network`、`:app → :core:designsystem`、`:app → :core:navigation`，:core 模組之間互不依賴。

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
