package com.example.composeplayground.ui.screen.camera

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.example.composeplayground.ui.screen.camera.model.CameraControlGroup
import com.example.composeplayground.ui.screen.camera.model.CameraFilter
import com.example.composeplayground.ui.screen.camera.model.CameraMode
import com.example.composeplayground.ui.screen.camera.model.FlashMode
import com.example.composeplayground.ui.screen.camera.model.WhiteBalance
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class CameraUiState(
    val hasCameraPermission: Boolean = false,
    val mode: CameraMode = CameraMode.Normal,
    val showGrid: Boolean = false,
    val isDashboardExpanded: Boolean = false,
    val activeControlGroup: CameraControlGroup = CameraControlGroup.Filters,
    // 共用
    val selectedFilter: CameraFilter = CameraFilter.Original,
    val isFrontCamera: Boolean = false,
    val flashMode: FlashMode = FlashMode.Auto,
    val zoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 8f,
    val isCapturing: Boolean = false,
    // 進階
    val evIndex: Int = 0,
    val evMin: Int = -6,
    val evMax: Int = 6,
    val whiteBalance: WhiteBalance = WhiteBalance.Auto,
    val isManualFocus: Boolean = false,
    val focusDistance: Float = 0f,
    val maxFocusDistance: Float = 10f,
    val isManualExposure: Boolean = false,
    val isoValue: Int = 400,
    val isoMin: Int = 100,
    val isoMax: Int = 3200,
    val shutterSpeedNs: Long = 1_000_000_000L / 60,
    val shutterMin: Long = 1_000_000_000L / 4000,
    val shutterMax: Long = 1_000_000_000L / 30,
)

class CameraViewModel : ViewModel() {

    val uiState: StateFlow<CameraUiState>
        field = MutableStateFlow(CameraUiState())

    val captureRequestFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun onPermissionResult(granted: Boolean) {
        uiState.update { it.copy(hasCameraPermission = granted) }
    }

    fun setMode(mode: CameraMode) {
        uiState.update {
            when (mode) {
                CameraMode.Normal -> it.copy(
                    mode = mode,
                    isDashboardExpanded = false,
                    activeControlGroup = CameraControlGroup.Filters,
                )

                CameraMode.Advanced -> it.copy(
                    mode = mode,
                    isDashboardExpanded = true,
                    activeControlGroup = CameraControlGroup.Exposure,
                )
            }
        }
    }

    fun toggleGrid() {
        uiState.update { it.copy(showGrid = !it.showGrid) }
    }

    fun toggleDashboard() {
        uiState.update { it.copy(isDashboardExpanded = !it.isDashboardExpanded) }
    }

    fun setActiveControlGroup(group: CameraControlGroup) {
        uiState.update { it.copy(activeControlGroup = group, isDashboardExpanded = true) }
    }

    fun selectFilter(filter: CameraFilter) {
        uiState.update { it.copy(selectedFilter = filter) }
    }

    fun toggleCamera() {
        uiState.update { it.copy(isFrontCamera = !it.isFrontCamera, zoomRatio = it.minZoomRatio) }
    }

    fun setFlashMode(mode: FlashMode) {
        uiState.update { it.copy(flashMode = mode) }
    }

    fun onZoomChanged(ratio: Float) {
        uiState.update { it.copy(zoomRatio = ratio.coerceIn(it.minZoomRatio, it.maxZoomRatio)) }
    }

    fun onZoomBoundsAvailable(min: Float, max: Float) {
        uiState.update {
            it.copy(
                minZoomRatio = min,
                maxZoomRatio = max,
                zoomRatio = it.zoomRatio.coerceIn(min, max),
            )
        }
    }

    fun requestCapture() {
        if (uiState.value.isCapturing) return
        uiState.update { it.copy(isCapturing = true) }
        captureRequestFlow.tryEmit(Unit)
    }

    fun onCaptureComplete() {
        uiState.update { it.copy(isCapturing = false) }
    }

    // ── 進階控制 ──────────────────────────────────────────────────────────

    fun setEvIndex(index: Int) {
        uiState.update { it.copy(evIndex = index.coerceIn(it.evMin, it.evMax)) }
    }

    fun onEvRangeAvailable(min: Int, max: Int) {
        uiState.update { it.copy(evMin = min, evMax = max, evIndex = 0) }
    }

    fun setWhiteBalance(wb: WhiteBalance) {
        uiState.update { it.copy(whiteBalance = wb) }
    }

    fun setManualFocus(enabled: Boolean) {
        uiState.update {
            if (enabled && it.maxFocusDistance == 0f) it
            else it.copy(isManualFocus = enabled)
        }
    }

    fun onFocusDistanceChanged(distance: Float) {
        uiState.update { it.copy(focusDistance = distance.coerceIn(0f, it.maxFocusDistance)) }
    }

    fun onMaxFocusDistanceAvailable(max: Float) {
        uiState.update {
            if (max == 0f) {
                // Fixed-focus lens — disable manual focus
                it.copy(maxFocusDistance = 0f, isManualFocus = false)
            } else {
                it.copy(
                    maxFocusDistance = max,
                    focusDistance = it.focusDistance.coerceIn(0f, max),
                )
            }
        }
    }

    fun setManualExposure(enabled: Boolean) {
        uiState.update { it.copy(isManualExposure = enabled) }
    }

    fun setIso(value: Int) {
        uiState.update { it.copy(isoValue = value.coerceIn(it.isoMin, it.isoMax)) }
    }

    fun setShutterSpeed(ns: Long) {
        uiState.update { it.copy(shutterSpeedNs = ns.coerceIn(it.shutterMin, it.shutterMax)) }
    }

    fun onIsoRangeAvailable(min: Int, max: Int) {
        uiState.update { it.copy(isoMin = min, isoMax = max, isoValue = (min + max) / 2) }
    }

    fun onShutterRangeAvailable(min: Long, max: Long) {
        uiState.update {
            it.copy(shutterMin = min, shutterMax = max, shutterSpeedNs = 1_000_000_000L / 60)
        }
    }
}
