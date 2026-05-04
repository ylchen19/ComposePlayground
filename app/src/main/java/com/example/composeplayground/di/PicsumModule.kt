package com.example.composeplayground.di

import com.example.composeplayground.data.analyzer.GeminiNanoPicsumImageAnalyzer
import com.example.composeplayground.data.analyzer.PicsumImageAnalyzer
import com.example.composeplayground.data.repository.PicsumRepository
import com.example.composeplayground.data.repository.PicsumRepositoryImpl
import com.example.composeplayground.ui.screen.picsum.PicsumDetailViewModel
import com.example.composeplayground.ui.screen.picsum.PicsumGalleryViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Picsum 圖庫模組的 Koin 註冊。
 *
 * Repository 注入 named `picsumApi` 的 [com.example.composeplayground.network.api.ApiService]，
 * 對應 [networkModule] 中以 `picsumBaseUrl` 設定的 HttpClient（base URL: `https://picsum.photos/`）。
 *
 * [PicsumDetailViewModel] 的建構參數透過導航層的 `parametersOf(id, author, w, h)` 注入。
 *
 * [PicsumImageAnalyzer] 為 singleton（[GeminiNanoPicsumImageAnalyzer]）——
 * 優先使用 Gemini Nano on-device 多模態（Pixel 9+ 才支援），
 * 不支援時自動降級為 ML Kit 標籤句子；內部 LruCache 跨頁面有效，重複進同一張詳細頁可秒出描述。
 */
val picsumModule = module {
    single<PicsumRepository> { PicsumRepositoryImpl(get(named("picsumApi"))) }
    single<PicsumImageAnalyzer> { GeminiNanoPicsumImageAnalyzer(appContext = androidContext()) }

    viewModel { PicsumGalleryViewModel(get()) }
    viewModel { params ->
        PicsumDetailViewModel(
            photoId = params.get(),
            author = params.get(),
            originalWidth = params.get(),
            originalHeight = params.get(),
            analyzer = get(),
        )
    }
}
