package com.example.composeplayground.ui.screen.camera.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.composeplayground.ui.screen.camera.CameraUiState
import com.example.composeplayground.ui.screen.camera.model.WhiteBalance
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
internal fun ExposureControls(
    uiState: CameraUiState,
    onEvChanged: (Int) -> Unit,
    onManualExposureToggle: (Boolean) -> Unit,
    onIsoChanged: (Int) -> Unit,
    onShutterSpeedChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ControlSlider(
            label = "EV",
            valueLabel = uiState.evIndex.toEvLabel(),
            enabled = true,
            value = uiState.evIndex.toFloat(),
            valueRange = uiState.evMin.toFloat().safeRangeTo(uiState.evMax.toFloat()),
            onValueChange = { onEvChanged(it.roundToInt()) },
        )
        ManualControlSlider(
            label = "ISO",
            valueLabel = if (uiState.isManualExposure) "${uiState.isoValue}" else "Auto",
            isManual = uiState.isManualExposure,
            onManualChange = onManualExposureToggle,
            value = uiState.isoValue.toFloat(),
            valueRange = uiState.isoMin.toFloat().safeRangeTo(uiState.isoMax.toFloat()),
            onValueChange = { onIsoChanged(it.roundToInt()) },
        )
        ControlSlider(
            label = "快門",
            valueLabel = if (uiState.isManualExposure) uiState.shutterSpeedNs.toShutterLabel() else "Auto",
            enabled = uiState.isManualExposure,
            value = uiState.shutterSpeedNs.toFloat(),
            valueRange = uiState.shutterMin.toFloat().safeRangeTo(uiState.shutterMax.toFloat()),
            onValueChange = { onShutterSpeedChanged(it.roundToLong()) },
        )
    }
}

@Composable
internal fun FocusControls(
    uiState: CameraUiState,
    onManualFocusToggle: (Boolean) -> Unit,
    onFocusDistanceChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ManualControlSlider(
            label = "對焦",
            valueLabel = if (uiState.isManualFocus) "${"%.2f".format(uiState.focusDistance)}" else "AF",
            isManual = uiState.isManualFocus,
            onManualChange = onManualFocusToggle,
            value = uiState.focusDistance,
            valueRange = 0f.safeRangeTo(uiState.maxFocusDistance.coerceAtLeast(1f)),
            onValueChange = onFocusDistanceChanged,
        )
    }
}

@Composable
internal fun WhiteBalanceControls(
    selected: WhiteBalance,
    onSelected: (WhiteBalance) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(WhiteBalance.entries, key = { it.name }) { wb ->
            FilterChip(
                selected = wb == selected,
                onClick = { onSelected(wb) },
                label = { Text(wb.label, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    labelColor = Color.White.copy(alpha = 0.82f),
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun ManualControlSlider(
    label: String,
    valueLabel: String,
    isManual: Boolean,
    onManualChange: (Boolean) -> Unit,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ControlLabel(label = label, valueLabel = valueLabel, active = isManual)
            Switch(checked = isManual, onCheckedChange = onManualChange)
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = isManual,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ControlSlider(
    label: String,
    valueLabel: String,
    enabled: Boolean,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ControlLabel(label = label, valueLabel = valueLabel, active = enabled)
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ControlLabel(
    label: String,
    valueLabel: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.66f),
        )
        Text(
            text = valueLabel,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.66f),
            fontWeight = FontWeight.SemiBold,
        )
    }
}
