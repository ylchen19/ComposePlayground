package com.example.composeplayground

import android.app.Application
import com.example.composeplayground.di.appNetworkModule
import com.example.composeplayground.di.picsumModule
import com.example.composeplayground.di.pokemonModule
import com.example.composeplayground.network.di.coreNetworkModule
import com.example.composeplayground.ui.theme.di.designSystemModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ComposePlaygroundApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ComposePlaygroundApp)
            modules(
                coreNetworkModule,
                designSystemModule,
                appNetworkModule,
                pokemonModule,
                picsumModule,
            )
        }
    }
}
