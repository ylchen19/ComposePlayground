package com.example.composeplayground.ui.screen

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.composeplayground.BuildConfig
import com.example.composeplayground.R
import com.example.composeplayground.data.analyzer.ModelStatus
import com.example.composeplayground.ui.screen.settings.GeminiNanoSettingsViewModel
import com.example.composeplayground.ui.theme.DarkModeOption
import com.example.composeplayground.ui.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    geminiNanoVm: GeminiNanoSettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val themeConfig by themeViewModel.uiState.collectAsStateWithLifecycle()
    val geminiNanoStatus by geminiNanoVm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { geminiNanoVm.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            DarkModeSection(
                selectedOption = themeConfig.darkModeOption,
                onOptionSelected = themeViewModel::setDarkModeOption,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                DynamicColorSection(
                    enabled = themeConfig.dynamicColor,
                    onToggle = themeViewModel::setDynamicColor,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            HorizontalDivider()
            if (BuildConfig.DEBUG) {
                PerformanceMetricsSection(
                    enabled = themeConfig.showPerformanceMetrics,
                    onToggle = themeViewModel::setPerformanceMetricsEnabled,
                )
                HorizontalDivider()
            }
            GeminiNanoSection(
                status = geminiNanoStatus,
                onDownload = geminiNanoVm::startDownload,
            )
        }
    }
}

// ── Private composables ──────────────────────────────────────────────────────

@Composable
private fun PerformanceMetricsSection(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.developer_options),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.show_perf_dashboard),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.show_perf_dashboard_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun DarkModeSection(
    selectedOption: DarkModeOption,
    onOptionSelected: (DarkModeOption) -> Unit,
) {
    Text(
        text = stringResource(R.string.dark_mode),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
    Column(modifier = Modifier.selectableGroup()) {
        DarkModeOption.entries.forEach { option ->
            val label = when (option) {
                DarkModeOption.SYSTEM -> stringResource(R.string.dark_mode_system)
                DarkModeOption.LIGHT -> stringResource(R.string.dark_mode_light)
                DarkModeOption.DARK -> stringResource(R.string.dark_mode_dark)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedOption == option,
                        onClick = { onOptionSelected(option) },
                        role = Role.RadioButton,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                RadioButton(selected = selectedOption == option, onClick = null)
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun DynamicColorSection(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.dynamic_color),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.use_wallpaper_colors),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.dynamic_color_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun GeminiNanoSection(
    status: ModelStatus,
    onDownload: () -> Unit,
) {
    Text(
        text = stringResource(R.string.gemini_status),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            text = "Picsum 圖庫的 AI 圖片描述功能需要 Gemini Nano 模型（約 2 GB）。" +
                "於此預先下載可避免進入詳細頁時臨時等候。建議在 Wi-Fi 環境下載。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        when (status) {
            ModelStatus.Unknown -> {
                Text(
                    text = "正在檢查模型狀態…",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            ModelStatus.NotSupported -> {
                Text(
                    text = "此裝置不支援 Gemini Nano，將自動使用 ML Kit 模板描述。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ModelStatus.Downloadable -> {
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.download_gemini_model))
                }
            }
            is ModelStatus.Downloading -> {
                val percent = (status.progress * 100).toInt()
                Text(
                    text = if (status.progress > 0f) {
                        stringResource(R.string.downloading) + " $percent%"
                    } else {
                        stringResource(R.string.starting_download)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                if (status.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            ModelStatus.Ready -> {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "✓ 模型已就緒，可直接使用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            is ModelStatus.Failed -> {
                Text(
                    text = "下載失敗：${status.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onDownload) { Text(stringResource(R.string.retry)) }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
