package com.example.composeplayground.ui.screen.pokemon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.composeplayground.data.model.Pokemon
import com.example.composeplayground.data.repository.PokemonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class PokemonTypeSection(
    val typeName: String,
    val pokemon: List<Pokemon> = emptyList(),
    val isLoading: Boolean = true,
)

@Immutable
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

    val uiState: StateFlow<PokemonTypeGalleryUiState>
        field = MutableStateFlow(PokemonTypeGalleryUiState())

    init {
        loadAllTypes()
    }

    private fun loadAllTypes() {
        ALL_POKEMON_TYPES.forEach { typeName ->
            viewModelScope.launch {
                runCatching { repository.fetchPokemonByType(typeName) }
                    .onSuccess { pokemon ->
                        uiState.update { state ->
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
                        uiState.update { state ->
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
