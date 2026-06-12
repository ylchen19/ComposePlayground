package com.example.composeplayground.ui.screen.camera.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.composeplayground.ui.screen.camera.CameraUiState
import com.example.composeplayground.ui.screen.camera.model.CameraControlGroup
import com.example.composeplayground.ui.screen.camera.model.CameraFilter
import com.example.composeplayground.ui.screen.camera.model.CameraMode
import com.example.composeplayground.ui.screen.camera.model.WhiteBalance

@Composable
internal fun CameraControlTray(
    uiState: CameraUiState,
    onControlGroupChange: (CameraControlGroup) -> Unit,
    onFilterSelected: (CameraFilter) -> Unit,
    onEvChanged: (Int) -> Unit,
    onWhiteBalanceSelected: (WhiteBalance) -> Unit,
    onManualFocusToggle: (Boolean) -> Unit,
    onFocusDistanceChanged: (Float) -> Unit,
    onManualExposureToggle: (Boolean) -> Unit,
    onIsoChanged: (Int) -> Unit,
    onShutterSpeedChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = when (uiState.mode) {
        CameraMode.Normal -> listOf(CameraControlGroup.Filters)
        CameraMode.Advanced -> CameraControlGroup.entries
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.72f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (groups.size > 1) {
                ControlGroupSelector(
                    groups = groups,
                    selected = uiState.activeControlGroup,
                    onSelected = onControlGroupChange,
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            }
            when (uiState.activeControlGroup) {
                CameraControlGroup.Filters -> FilterSelector(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = onFilterSelected,
                    modifier = Modifier.fillMaxWidth(),
                )

                CameraControlGroup.Exposure -> ExposureControls(
                    uiState = uiState,
                    onEvChanged = onEvChanged,
                    onManualExposureToggle = onManualExposureToggle,
                    onIsoChanged = onIsoChanged,
                    onShutterSpeedChanged = onShutterSpeedChanged,
                )

                CameraControlGroup.Focus -> FocusControls(
                    uiState = uiState,
                    onManualFocusToggle = onManualFocusToggle,
                    onFocusDistanceChanged = onFocusDistanceChanged,
                )

                CameraControlGroup.WhiteBalance -> WhiteBalanceControls(
                    selected = uiState.whiteBalance,
                    onSelected = onWhiteBalanceSelected,
                )
            }
        }
    }
}

@Composable
private fun ControlGroupSelector(
    groups: List<CameraControlGroup>,
    selected: CameraControlGroup,
    onSelected: (CameraControlGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(groups, key = { it.name }) { group ->
            FilterChip(
                selected = group == selected,
                onClick = { onSelected(group) },
                leadingIcon = {
                    Icon(
                        imageVector = group.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                label = { Text(group.label, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    labelColor = Color.White.copy(alpha = 0.82f),
                    iconColor = Color.White.copy(alpha = 0.82f),
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = group == selected,
                    borderColor = Color.White.copy(alpha = 0.16f),
                    selectedBorderColor = Color.Transparent,
                ),
            )
        }
    }
}

private val CameraControlGroup.label: String
    get() = when (this) {
        CameraControlGroup.Filters -> "濾鏡"
        CameraControlGroup.Exposure -> "曝光"
        CameraControlGroup.Focus -> "對焦"
        CameraControlGroup.WhiteBalance -> "白平衡"
    }

private val CameraControlGroup.icon: ImageVector
    get() = when (this) {
        CameraControlGroup.Filters -> Icons.Filled.FilterVintage
        CameraControlGroup.Exposure -> Icons.Filled.Exposure
        CameraControlGroup.Focus -> Icons.Filled.CenterFocusStrong
        CameraControlGroup.WhiteBalance -> Icons.Filled.WbSunny
    }
