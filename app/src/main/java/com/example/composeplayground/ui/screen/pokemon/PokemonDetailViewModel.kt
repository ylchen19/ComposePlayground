package com.example.composeplayground.ui.screen.pokemon

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.composeplayground.data.model.EvolutionNode
import com.example.composeplayground.data.model.PokemonDetail
import com.example.composeplayground.data.repository.PokemonRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface PokemonDetailUiState {
    data object Loading : PokemonDetailUiState
    @Immutable
    data class Success(
        val pokemon: PokemonDetail,
        val evolutionChain: List<EvolutionNode> = emptyList()
    ) : PokemonDetailUiState
    data class Error(val message: String) : PokemonDetailUiState
}

class PokemonDetailViewModel(
    private val pokemonId: Int,
    private val repository: PokemonRepository,
) : ViewModel() {

    val uiState: StateFlow<PokemonDetailUiState>
        field = MutableStateFlow<PokemonDetailUiState>(PokemonDetailUiState.Loading)

    init {
        loadPokemonDetail()
    }

    fun retry() {
        loadPokemonDetail()
    }

    private fun loadPokemonDetail() {
        uiState.value = PokemonDetailUiState.Loading
        viewModelScope.launch {
            try {
                val detailDeferred = async { repository.fetchPokemonDetail(pokemonId) }
                val evolutionDeferred = async { repository.fetchEvolutionChain(pokemonId) }

                val detail = detailDeferred.await()
                val evolutionChain = runCatching { evolutionDeferred.await() }.getOrElse { emptyList() }

                uiState.value = PokemonDetailUiState.Success(
                    pokemon = detail,
                    evolutionChain = evolutionChain
                )
            } catch (e: Exception) {
                uiState.value = PokemonDetailUiState.Error(
                    e.localizedMessage ?: "Failed to load Pokémon details"
                )
            }
        }
    }
}
