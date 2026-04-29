package com.example.composeplayground.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * App 進入後的首頁菜單，列出可用的功能模組。
 *
 * 目前提供：
 * - Pokémon 圖鑑
 * - Picsum 圖庫
 *
 * TopBar 右側提供 Settings 入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMenuScreen(
    onNavigateToPokemon: () -> Unit,
    onNavigateToPicsum: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("ComposePlayground", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "設定")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(PaddingValues(horizontal = 16.dp, vertical = 12.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MenuCard(
                title = "Pokémon 圖鑑",
                subtitle = "瀏覽全部寶可夢、依屬性分類、查看詳細資訊",
                gradient = Brush.horizontalGradient(
                    listOf(Color(0xFFE53935), Color(0xFFFFC107)),
                ),
                onClick = onNavigateToPokemon,
            )
            MenuCard(
                title = "Picsum 圖庫",
                subtitle = "載入大量大檔案圖片，示範 Coil 快取與 Paging 串流",
                gradient = Brush.horizontalGradient(
                    listOf(Color(0xFF1565C0), Color(0xFF26C6DA)),
                ),
                onClick = onNavigateToPicsum,
            )
        }
    }
}

@Composable
private fun MenuCard(
    title: String,
    subtitle: String,
    gradient: Brush,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(gradient),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
