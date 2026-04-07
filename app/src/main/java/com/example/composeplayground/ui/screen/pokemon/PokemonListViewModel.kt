package com.example.composeplayground.ui.screen.pokemon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.composeplayground.data.model.Pokemon
import com.example.composeplayground.data.paging.PokemonPagingSource
import com.example.composeplayground.data.paging.TypeFilteredPagingSource
import com.example.composeplayground.data.repository.PokemonRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

enum class ViewMode { Grid, List }

data class PokemonListUiState(
    val viewMode: ViewMode = ViewMode.Grid,
    val searchQuery: String = "",
    val selectedType: String? = null,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PokemonListViewModel(
    private val repository: PokemonRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PokemonListUiState())
    val uiState: StateFlow<PokemonListUiState> = _uiState.asStateFlow()

    // 搜尋加 debounce、型別切換即時生效，兩者皆直接從 _uiState 衍生，消除重複 source of truth
    val pokemonPagingFlow: Flow<PagingData<Pokemon>> = combine(
        _uiState.map { it.searchQuery }.distinctUntilChanged().debounce(300),
        _uiState.map { it.selectedType }.distinctUntilChanged(),
    ) { query, type ->
        query to type
    }.flatMapLatest { (query, type) ->
        Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 10,
                initialLoadSize = 40,
                enablePlaceholders = false,
            ),
        ) {
            if (type != null) {
                TypeFilteredPagingSource(repository, type, query)
            } else {
                PokemonPagingSource(repository, query)
            }
        }.flow
    }.cachedIn(viewModelScope)

    val availableTypes: List<String> = listOf(
        "normal", "fire", "water", "electric", "grass", "ice",
        "fighting", "poison", "ground", "flying", "psychic", "bug",
        "rock", "ghost", "dragon", "dark", "steel", "fairy",
    )

    fun toggleViewMode() {
        _uiState.update {
            it.copy(viewMode = if (it.viewMode == ViewMode.Grid) ViewMode.List else ViewMode.Grid)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectType(type: String?) {
        _uiState.update { it.copy(selectedType = type) }
    }
}
