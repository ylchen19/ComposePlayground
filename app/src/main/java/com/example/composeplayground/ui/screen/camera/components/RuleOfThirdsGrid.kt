package com.example.composeplayground.ui.screen.camera.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun RuleOfThirdsGrid(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val lineColor = Color.White.copy(alpha = 0.42f)
        val stroke = 1.dp.toPx()
        val verticalOne = size.width / 3f
        val verticalTwo = size.width * 2f / 3f
        val horizontalOne = size.height / 3f
        val horizontalTwo = size.height * 2f / 3f

        drawLine(lineColor, Offset(verticalOne, 0f), Offset(verticalOne, size.height), stroke)
        drawLine(lineColor, Offset(verticalTwo, 0f), Offset(verticalTwo, size.height), stroke)
        drawLine(lineColor, Offset(0f, horizontalOne), Offset(size.width, horizontalOne), stroke)
        drawLine(lineColor, Offset(0f, horizontalTwo), Offset(size.width, horizontalTwo), stroke)
    }
}
