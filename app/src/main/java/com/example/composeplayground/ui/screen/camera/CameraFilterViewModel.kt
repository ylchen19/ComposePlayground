package com.example.composeplayground.ui.screen.camera

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.example.composeplayground.ui.screen.camera.model.CameraFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class CameraFilterUiState(
    val hasCameraPermission: Boolean = false,
    val selectedFilter: CameraFilter = CameraFilter.Original,
)

class CameraFilterViewModel : ViewModel() {

    val uiState: StateFlow<CameraFilterUiState>
        field = MutableStateFlow(CameraFilterUiState())

    fun onPermissionResult(granted: Boolean) {
        uiState.update { it.copy(hasCameraPermission = granted) }
    }

    fun selectFilter(filter: CameraFilter) {
        uiState.update { it.copy(selectedFilter = filter) }
    }
}
