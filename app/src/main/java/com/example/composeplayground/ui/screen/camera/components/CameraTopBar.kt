package com.example.composeplayground.ui.screen.camera.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.composeplayground.ui.screen.camera.CameraUiState
import com.example.composeplayground.ui.screen.camera.model.CameraMode

@Composable
internal fun CameraTopBar(
    uiState: CameraUiState,
    onBack: () -> Unit,
    onModeChange: (CameraMode) -> Unit,
    onToggleGrid: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TranslucentIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                onClick = onBack,
            )
            CameraStatusStrip(
                uiState = uiState,
                modifier = Modifier.weight(1f),
            )
            TranslucentIconButton(
                icon = Icons.Filled.GridOn,
                contentDescription = if (uiState.showGrid) "關閉格線" else "開啟格線",
                selected = uiState.showGrid,
                onClick = onToggleGrid,
            )
        }
        ModeSegmentedToggle(
            selectedMode = uiState.mode,
            onModeChange = onModeChange,
            modifier = Modifier
                .padding(top = 8.dp)
                .widthIn(max = 184.dp),
        )
    }
}

@Composable
private fun CameraStatusStrip(
    uiState: CameraUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(44.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.42f),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusValue(label = "MODE", value = if (uiState.mode == CameraMode.Normal) "AUTO" else "PRO")
            StatusValue(label = "EV", value = uiState.evIndex.toEvLabel())
            StatusValue(label = "WB", value = uiState.whiteBalance.shortLabel)
            StatusValue(
                label = "ISO",
                value = if (uiState.isManualExposure) "${uiState.isoValue}" else "AUTO",
            )
            StatusValue(
                label = "S",
                value = if (uiState.isManualExposure) uiState.shutterSpeedNs.toShutterLabel() else "AUTO",
            )
        }
    }
}

@Composable
private fun StatusValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.58f),
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
