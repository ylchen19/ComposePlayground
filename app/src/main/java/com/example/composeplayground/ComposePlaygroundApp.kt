package com.example.composeplayground

import android.app.Application
import com.example.composeplayground.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ComposePlaygroundApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ComposePlaygroundApp)
            modules(appModule)
        }
    }
}

