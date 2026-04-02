package com.example.composeplayground.ui.screen.pokemon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.composeplayground.data.model.PokemonDetail
import com.example.composeplayground.data.repository.PokemonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PokemonDetailUiState {
    data object Loading : PokemonDetailUiState
    data class Success(val pokemon: PokemonDetail) : PokemonDetailUiState
    data class Error(val message: String) : PokemonDetailUiState
}

class PokemonDetailViewModel(
    private val pokemonId: Int,
    private val repository: PokemonRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PokemonDetailUiState>(PokemonDetailUiState.Loading)
    val uiState: StateFlow<PokemonDetailUiState> = _uiState.asStateFlow()

    init {
        loadPokemonDetail()
    }

    fun retry() {
        loadPokemonDetail()
    }

    private fun loadPokemonDetail() {
        _uiState.value = PokemonDetailUiState.Loading
        viewModelScope.launch {
            try {
                val detail = repository.fetchPokemonDetail(pokemonId)
                _uiState.value = PokemonDetailUiState.Success(detail)
            } catch (e: Exception) {
                _uiState.value = PokemonDetailUiState.Error(
                    e.localizedMessage ?: "Failed to load Pokémon details"
                )
            }
        }
    }
}
