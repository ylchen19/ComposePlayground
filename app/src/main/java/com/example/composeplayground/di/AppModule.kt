package com.example.composeplayground.di

import org.koin.dsl.module

class GreetingProvider {
    fun greeting(name: String): String = "Hello $name!"
}

val appModule = module {
    single { GreetingProvider() }
}

