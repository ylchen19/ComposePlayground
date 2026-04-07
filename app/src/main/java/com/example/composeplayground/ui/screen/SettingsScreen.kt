package com.example.composeplayground.ui.screen

import android.os.Build
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.composeplayground.ui.theme.DarkModeOption
import com.example.composeplayground.ui.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val themeConfig by themeViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            }
        }
    }
}

// ── Private composables ──────────────────────────────────────────────────────

@Composable
private fun DarkModeSection(
    selectedOption: DarkModeOption,
    onOptionSelected: (DarkModeOption) -> Unit,
) {
    Text(
        text = "Dark Mode",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
    Column(modifier = Modifier.selectableGroup()) {
        DarkModeOption.entries.forEach { option ->
            val label = when (option) {
                DarkModeOption.SYSTEM -> "Follow system"
                DarkModeOption.LIGHT -> "Light"
                DarkModeOption.DARK -> "Dark"
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
        text = "Dynamic Color",
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
            Text(text = "Use wallpaper colors", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Apply colors from your wallpaper to the app theme",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}
