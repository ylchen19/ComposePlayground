# ComposePlayground

Android Compose 練習專案，用於探索現代 Android 開發技術棧。目前整合 PokéAPI 與 Picsum Photos 兩個功能模組。

## 功能

### Pokémon 圖鑑
- **寶可夢列表**：分頁載入、Grid / List 雙視圖切換、搜尋、依屬性篩選
- **寶可夢詳情**：屬性、圖片、基本數值
- **屬性圖鑑**：以屬性分類，每種屬性呈現為一列橫向捲動列表
- **Shimmer 讀取效果**：骨架動畫作為載入佔位

### Picsum 圖庫
- **大圖網格**：兩欄等高 grid，每張以 1080×1080 px 大圖載入，示範 Coil 進場效果與磁碟快取
- **無限滾動**：Paging 3 串接，`prefetchDistance` 預取下一頁
- **全螢幕詳細頁**：顯示原始尺寸大圖、作者、原始解析度
- **Pinch-to-zoom**：雙指縮放（1×–5×）+ 平移，放回 1× 時自動回正

### 全域
- **首頁菜單**：入口頁，以漸層大卡片切換各模組
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
UI Layer        → Screen + ViewModel（MVVM）
Domain Layer    → @Immutable Domain Models、Repository 介面
Data Layer      → RepositoryImpl、PagingSource、API DTOs
Network Layer   → Ktor HttpClient、ApiService、Token/Cache
```

每個功能模組（Pokémon、Picsum）各自擁有：
- `di/XxxModule.kt`：Repository + ViewModel 的 Koin 註冊
- `data/model/`、`data/paging/`、`data/repository/`：資料層
- `ui/screen/xxx/`：Screen、ViewModel、components

網路層多 base URL 策略：各模組以 `named("xxxApi")` 的獨立 `ApiService` 注入，底層共用 `HttpClientFactory` 與 `CacheConfig`。

### 導航頁面

| Key | 說明 |
|-----|------|
| `Home` | 首頁菜單（模組入口） |
| `PokemonHome` | 寶可夢列表 |
| `PokemonDetail(pokemonId)` | 寶可夢詳細資訊 |
| `PokemonTypeGallery` | 依屬性分類圖鑑 |
| `PicsumGallery` | Picsum 圖庫（兩欄 grid） |
| `PicsumDetail(photoId, author, w, h)` | 全螢幕大圖 + 縮放 |
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

| 模組 | API | Base URL |
|------|-----|----------|
| Pokémon 圖鑑 | [PokéAPI](https://pokeapi.co/)（公開，無需金鑰） | `https://pokeapi.co/api/v2/` |
| Picsum 圖庫 | [Picsum Photos](https://picsum.photos/)（公開，無需金鑰） | `https://picsum.photos/` |
