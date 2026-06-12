package com.example.composeplayground.ui.screen.camera.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.composeplayground.ui.screen.camera.CameraUiState
import com.example.composeplayground.ui.screen.camera.model.CameraControlGroup
import com.example.composeplayground.ui.screen.camera.model.CameraFilter
import com.example.composeplayground.ui.screen.camera.model.CameraMode
import com.example.composeplayground.ui.screen.camera.model.FlashMode
import com.example.composeplayground.ui.screen.camera.model.WhiteBalance

@Composable
fun CameraDashboardOverlay(
    uiState: CameraUiState,
    onBack: () -> Unit,
    onModeChange: (CameraMode) -> Unit,
    onToggleGrid: () -> Unit,
    onToggleDashboard: () -> Unit,
    onControlGroupChange: (CameraControlGroup) -> Unit,
    onFilterSelected: (CameraFilter) -> Unit,
    onFlashToggle: () -> Unit,
    onShutter: () -> Unit,
    onFlip: () -> Unit,
    onZoomClick: () -> Unit,
    onEvChanged: (Int) -> Unit,
    onWhiteBalanceSelected: (WhiteBalance) -> Unit,
    onManualFocusToggle: (Boolean) -> Unit,
    onFocusDistanceChanged: (Float) -> Unit,
    onManualExposureToggle: (Boolean) -> Unit,
    onIsoChanged: (Int) -> Unit,
    onShutterSpeedChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.showGrid) {
            RuleOfThirdsGrid(modifier = Modifier.fillMaxSize())
        }

        Column(modifier = Modifier.fillMaxSize()) {
            CameraTopBar(
                uiState = uiState,
                onBack = onBack,
                onModeChange = onModeChange,
                onToggleGrid = onToggleGrid,
            )
            Spacer(modifier = Modifier.weight(1f))
            AnimatedVisibility(
                visible = uiState.isDashboardExpanded,
                enter = fadeIn() + slideInVertically { it / 3 },
                exit = fadeOut() + slideOutVertically { it / 3 },
            ) {
                CameraControlTray(
                    uiState = uiState,
                    onControlGroupChange = onControlGroupChange,
                    onFilterSelected = onFilterSelected,
                    onEvChanged = onEvChanged,
                    onWhiteBalanceSelected = onWhiteBalanceSelected,
                    onManualFocusToggle = onManualFocusToggle,
                    onFocusDistanceChanged = onFocusDistanceChanged,
                    onManualExposureToggle = onManualExposureToggle,
                    onIsoChanged = onIsoChanged,
                    onShutterSpeedChanged = onShutterSpeedChanged,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            CameraBottomControls(
                uiState = uiState,
                onFlashToggle = onFlashToggle,
                onShutter = onShutter,
                onFlip = onFlip,
                onZoomClick = onZoomClick,
                onToggleDashboard = onToggleDashboard,
            )
        }
    }
}
