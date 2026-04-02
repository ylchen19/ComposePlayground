package com.example.composeplayground.di

import com.example.composeplayground.data.repository.PokemonRepository
import com.example.composeplayground.data.repository.PokemonRepositoryImpl
import com.example.composeplayground.ui.screen.pokemon.PokemonDetailViewModel
import com.example.composeplayground.ui.screen.pokemon.PokemonListViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val pokemonModule = module {

    // Dedicated HttpClient for PokéAPI (no auth needed)
    single(named("pokemonClient")) {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                })
            }

            install(Logging) {
                level = LogLevel.HEADERS
            }

            defaultRequest {
                url("https://pokeapi.co/api/v2/")
                contentType(ContentType.Application.Json)
            }
        }
    }

    // Repository
    single<PokemonRepository> { PokemonRepositoryImpl(get(named("pokemonClient"))) }

    // ViewModels
    viewModel { PokemonListViewModel(get()) }
    viewModel { params -> PokemonDetailViewModel(pokemonId = params.get(), repository = get()) }
}
