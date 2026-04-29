package com.example.composeplayground.di

import com.example.composeplayground.data.repository.PicsumRepository
import com.example.composeplayground.data.repository.PicsumRepositoryImpl
import com.example.composeplayground.ui.screen.picsum.PicsumDetailViewModel
import com.example.composeplayground.ui.screen.picsum.PicsumGalleryViewModel
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
 */
val picsumModule = module {
    single<PicsumRepository> { PicsumRepositoryImpl(get(named("picsumApi"))) }

    viewModel { PicsumGalleryViewModel(get()) }
    viewModel { params ->
        PicsumDetailViewModel(
            photoId = params.get(),
            author = params.get(),
            originalWidth = params.get(),
            originalHeight = params.get(),
        )
    }
}
