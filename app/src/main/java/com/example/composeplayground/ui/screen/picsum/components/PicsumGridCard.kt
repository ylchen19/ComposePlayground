package com.example.composeplayground.ui.screen.picsum.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.composeplayground.data.model.PicsumPhoto

/**
 * Picsum 卡片：支援固定 1:1 等高（Grid 模式）或原始比例（StaggeredGrid 模式）。
 *
 * @param useOriginalAspectRatio 為 true 時使用圖片原始長寬比，false 時固定 1:1。
 */
@Composable
fun PicsumGridCard(
    photo: PicsumPhoto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useOriginalAspectRatio: Boolean = false,
) {
    val ratio = if (useOriginalAspectRatio) {
        photo.originalWidth.toFloat() / photo.originalHeight.toFloat()
    } else {
        1f
    }
    val imageUrl = if (useOriginalAspectRatio) {
        photo.scaledUrl(maxWidth = 600)
    } else {
        photo.thumbnailUrl(1080)
    }

    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(ratio),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Photo by ${photo.author}",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .align(Alignment.BottomCenter),
            ) {
                Text(
                    text = photo.author,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${photo.originalWidth}×${photo.originalHeight}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                )
            }
        }
    }
}
