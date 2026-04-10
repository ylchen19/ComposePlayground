package com.example.composeplayground.ui.screen.pokemon.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

private enum class UniformHeightSlot { Probe, Content }

/**
 * 在 Layout Phase 以 SubcomposeLayout 測量 [probeText] 在指定 [probeStyle] /
 * [probeMaxWidth] 下的渲染高度，再將該高度作為 [minTextHeight] 傳入 [content]。
 *
 * 每個 section 只做 1 次 probe subcomposition（O(1)），
 * 且整個流程在 Layout Phase 完成，不阻塞 Composition Phase 主執行緒。
 */
@Composable
fun UniformHeightLazyRow(
    probeText: String,
    probeStyle: TextStyle,
    probeMaxWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable (minTextHeight: Dp) -> Unit,
) {
    val density = LocalDensity.current
    val probeWidthPx = with(density) { probeMaxWidth.toPx().roundToInt() }

    SubcomposeLayout(modifier = modifier) { constraints ->
        // ── Phase 1：Measure Phase — probe 最長名稱取文字高度 ──────────────────
        val probeHeight = if (probeText.isNotEmpty()) {
            subcompose(UniformHeightSlot.Probe) {
                Text(text = probeText, style = probeStyle)
            }.first().measure(
                Constraints(maxWidth = probeWidthPx),
            ).height
        } else 0

        val minTextHeightDp = with(density) { probeHeight.toDp() }

        // ── Phase 2：Render Phase — 以 minTextHeight 渲染實際內容 ──────────────
        val contentPlaceable = subcompose(UniformHeightSlot.Content) {
            content(minTextHeightDp)
        }.first().measure(constraints)

        layout(contentPlaceable.width, contentPlaceable.height) {
            contentPlaceable.place(0, 0)
        }
    }
}

