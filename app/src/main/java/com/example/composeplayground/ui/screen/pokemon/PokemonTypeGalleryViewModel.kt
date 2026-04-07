package com.example.composeplayground.ui.screen.pokemon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.composeplayground.data.model.Pokemon
import com.example.composeplayground.data.repository.PokemonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PokemonTypeSection(
    val typeName: String,
    val pokemon: List<Pokemon> = emptyList(),
    val isLoading: Boolean = true,
)

data class PokemonTypeGalleryUiState(
    val sections: List<PokemonTypeSection> = ALL_POKEMON_TYPES.map { PokemonTypeSection(it) },
    val error: String? = null,
)

internal val ALL_POKEMON_TYPES = listOf(
    "normal", "fire", "water", "electric", "grass", "ice",
    "fighting", "poison", "ground", "flying", "psychic", "bug",
    "rock", "ghost", "dragon", "dark", "steel", "fairy",
)

class PokemonTypeGalleryViewModel(
    private val repository: PokemonRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PokemonTypeGalleryUiState())
    val uiState: StateFlow<PokemonTypeGalleryUiState> = _uiState.asStateFlow()

    init {
        loadAllTypes()
    }

    private fun loadAllTypes() {
        ALL_POKEMON_TYPES.forEach { typeName ->
            viewModelScope.launch {
                runCatching { repository.fetchPokemonByType(typeName) }
                    .onSuccess { pokemon ->
                        _uiState.update { state ->
                            state.copy(
                                sections = state.sections.map { section ->
                                    if (section.typeName == typeName) {
                                        section.copy(
                                            pokemon = pokemon,
                                            isLoading = false,
                                        )
                                    } else {
                                        section
                                    }
                                },
                            )
                        }
                    }
                    .onFailure {
                        _uiState.update { state ->
                            state.copy(
                                sections = state.sections.map { section ->
                                    if (section.typeName == typeName) {
                                        section.copy(isLoading = false)
                                    } else {
                                        section
                                    }
                                },
                            )
                        }
                    }
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
