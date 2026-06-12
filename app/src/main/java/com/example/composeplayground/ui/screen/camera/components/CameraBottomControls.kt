package com.example.composeplayground.ui.screen.camera.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import com.example.composeplayground.ui.screen.camera.model.FlashMode

@Composable
internal fun CameraBottomControls(
    uiState: CameraUiState,
    onFlashToggle: () -> Unit,
    onShutter: () -> Unit,
    onFlip: () -> Unit,
    onZoomClick: () -> Unit,
    onToggleDashboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.54f),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TranslucentIconButton(
                icon = when (uiState.flashMode) {
                    FlashMode.Off -> Icons.Filled.FlashOff
                    FlashMode.Auto -> Icons.Filled.FlashAuto
                    FlashMode.On -> Icons.Filled.FlashOn
                },
                contentDescription = "閃光燈 ${uiState.flashMode.name}",
                onClick = onFlashToggle,
            )
            ShutterButton(
                isCapturing = uiState.isCapturing,
                onClick = onShutter,
            )
            TranslucentIconButton(
                icon = Icons.Filled.Cameraswitch,
                contentDescription = "切換鏡頭",
                onClick = onFlip,
            )
            ZoomButton(
                zoomRatio = uiState.zoomRatio,
                onClick = onZoomClick,
            )
            TranslucentIconButton(
                icon = if (uiState.isDashboardExpanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                contentDescription = if (uiState.isDashboardExpanded) "收合控制" else "展開控制",
                selected = uiState.isDashboardExpanded,
                onClick = onToggleDashboard,
            )
        }
    }
}

@Composable
private fun ShutterButton(
    isCapturing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = onClick,
        enabled = !isCapturing,
        modifier = modifier.size(78.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContainerColor = Color.White.copy(alpha = 0.74f),
            disabledContentColor = Color.Black.copy(alpha = 0.68f),
        ),
    ) {
        if (isCapturing) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Color.Black,
                strokeWidth = 3.dp,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = "拍照",
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun ZoomButton(
    zoomRatio: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.42f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "${"%.1f".format(zoomRatio)}x",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
