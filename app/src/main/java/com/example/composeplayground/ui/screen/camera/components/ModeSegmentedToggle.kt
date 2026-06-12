package com.example.composeplayground.ui.screen.camera.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.composeplayground.ui.screen.camera.model.CameraMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSegmentedToggle(
    selectedMode: CameraMode,
    onModeChange: (CameraMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = CameraMode.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selectedMode,
                onClick = { onModeChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = Color.White.copy(alpha = 0.25f),
                    activeContentColor = Color.White,
                    inactiveContainerColor = Color.Black.copy(alpha = 0.3f),
                    inactiveContentColor = Color.White.copy(alpha = 0.7f),
                ),
                label = {
                    Text(when (mode) {
                        CameraMode.Normal   -> "一般"
                        CameraMode.Advanced -> "進階"
                    })
                },
            )
        }
    }
}
