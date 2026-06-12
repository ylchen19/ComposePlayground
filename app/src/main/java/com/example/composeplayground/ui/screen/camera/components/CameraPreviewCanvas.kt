package com.example.composeplayground.ui.screen.camera.components

import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.graphics.withTranslation
import com.example.composeplayground.ui.screen.camera.model.CameraFilter

@Composable
fun CameraPreviewCanvas(
    frame: Bitmap?,
    filter: CameraFilter,
    modifier: Modifier = Modifier,
) {
    if (frame == null) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text("啟動相機中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val paint = remember { Paint() }
    paint.colorFilter = ColorMatrixColorFilter(filter.buildMatrix())

    Canvas(modifier = modifier.fillMaxSize()) {
        drawIntoCanvas { composeCanvas ->
            val nativeCanvas = composeCanvas.nativeCanvas
            val scaleX = size.width / frame.width
            val scaleY = size.height / frame.height
            val scale = maxOf(scaleX, scaleY)
            val dx = (size.width - frame.width * scale) / 2f
            val dy = (size.height - frame.height * scale) / 2f
            nativeCanvas.withTranslation(dx, dy) {
                scale(scale, scale)
                drawBitmap(frame, 0f, 0f, paint)
            }
        }
    }
}
