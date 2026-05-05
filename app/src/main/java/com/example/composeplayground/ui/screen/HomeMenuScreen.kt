package com.example.composeplayground.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.composeplayground.R

/**
 * App 進入後的首頁菜單，列出可用的功能模組。
 *
 * 版面：頂部 Hero 區（App 名稱 + slogan）+ 下方 Material You 功能卡片。
 */
@Composable
fun HomeMenuScreen(
    onNavigateToPokemon: () -> Unit,
    onNavigateToPicsum: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeTheme {
        Scaffold(modifier = modifier) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize()) {
                HeroSection(
                    onNavigateToSettings = onNavigateToSettings,
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FeatureCard(
                        icon = Icons.Filled.CatchingPokemon,
                        iconBackgroundColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        title = stringResource(R.string.feature_pokemon_title),
                        subtitle = stringResource(R.string.feature_pokemon_subtitle),
                        onClick = onNavigateToPokemon,
                    )
                    FeatureCard(
                        icon = Icons.Filled.PhotoLibrary,
                        iconBackgroundColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        title = stringResource(R.string.feature_picsum_title),
                        subtitle = stringResource(R.string.feature_picsum_subtitle),
                        onClick = onNavigateToPicsum,
                    )
                }
            }
        }
    }
}

// ── Private composables ──────────────────────────────────────────────────────

/**
 * 全寬 Hero 區：App 名稱、slogan、Settings 入口。
 * 背景以 [MaterialTheme.colorScheme.primary] 延伸至 Status Bar 後方。
 */
@Composable
private fun HeroSection(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 40.dp),
    ) {
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(top = 48.dp),
        ) {
            Text(
                text = "ComposePlayground",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = stringResource(R.string.home_slogan),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Material You 風格的功能入口卡片。
 *
 * @param icon            功能代表 icon
 * @param iconBackgroundColor icon 容器的背景色（通常為 primary/secondary）
 * @param containerColor  Card 背景色（通常為 primaryContainer/secondaryContainer）
 * @param contentColor    標題與 chevron 的顏色
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureCard(
    icon: ImageVector,
    iconBackgroundColor: Color,
    containerColor: Color,
    contentColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(iconBackgroundColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
