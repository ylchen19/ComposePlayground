# ComposePlayground

Android Compose 練習專案，以 PokéAPI 為後端，探索現代 Android 開發技術棧。

## 功能

- **寶可夢列表**：分頁載入、Shimmer 骨架讀取效果
- **寶可夢詳情**：顯示屬性、圖片、基本數值
- **屬性圖鑑**：以屬性分類，每種屬性呈現為一列橫向捲動列表
- **主題切換**：Light / Dark / System，偏好設定持久化至 DataStore
- **網路監聽**：即時偵測連線狀態

## Tech Stack

| 層級 | 技術 |
|------|------|
| 語言 | Kotlin 2.3.20 |
| UI | Jetpack Compose (BOM 2026.03.01) + Material 3 |
| DI | Koin 4.1.0 |
| 網路 | Ktor Client 3.1.3 + OkHttp Engine |
| 序列化 | Kotlinx Serialization 1.8.1 |
| 導航 | Navigation3 1.0.1 |
| 圖片 | Coil Compose 2.7.0 |
| 分頁 | Paging 3 |
| Build | AGP 9.1.0，Gradle 9.3.1，Version Catalog |
| Target | compileSdk 36，minSdk 29 |

## 架構

單模組 `:app`，採用 Clean Architecture 分層：

```
UI Layer        → Screen + ViewModel（MVI/MVVM）
Domain Layer    → Domain Models、Repository 介面
Data Layer      → RepositoryImpl、PagingSource、API Models
Network Layer   → Ktor HttpClient、ApiService、Token/Cache
```

### 導航頁面

| Key | 說明 |
|-----|------|
| `Home` | 寶可夢列表（首頁） |
| `PokemonDetail(pokemonId)` | 寶可夢詳細資訊 |
| `PokemonTypeGallery` | 依屬性分類圖鑑 |
| `Settings` | 主題偏好設定 |

## Build

```bash
# 編譯檢查
./gradlew :app:compileDebugKotlin

# 建置 debug APK
./gradlew :app:assembleDebug

# 執行 unit tests
./gradlew test
```

## API

使用 [PokéAPI](https://pokeapi.co/)（公開 API，無需金鑰）。
Base URL：`https://pokeapi.co/api/v2/`
