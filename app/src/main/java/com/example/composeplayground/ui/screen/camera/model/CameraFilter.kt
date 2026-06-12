package com.example.composeplayground.ui.screen.camera.model

import android.graphics.ColorMatrix

/**
 * 相機即時濾鏡，靈感來自 VSCO / Instagram / Hollywood 最熱門色調風格。
 *
 * 複合效果以 [ColorMatrix.postConcat] 串接：先套用 this，再套用參數矩陣。
 * ColorMatrix 格式：4×5 row-major，每列為 [R係數, G係數, B係數, A係數, Bias]，Bias 為 0–255。
 * 對比度矩陣中心公式：bias = 128 × (1 − scale)。
 */
enum class CameraFilter(val label: String, val buildMatrix: () -> ColorMatrix) {

    Original("Original", {
        ColorMatrix()
    }),

    /**
     * 好萊塢電影標準色調：暗部偏青藍（Teal），亮部偏橙（Orange）。
     * 透過 cross-channel 係數讓高紅區（膚色）往橙色推、高藍區（天空/陰影）往藍綠推，
     * 再加對比度 ×1.2 使效果更立體。
     */
    TealOrange("Teal & Orange", {
        ColorMatrix(floatArrayOf(
             1.2f,  0.1f, -0.1f, 0f,  5f,
            -0.1f,  0.95f, 0.0f, 0f, -5f,
            -0.1f,  0.2f,  0.85f,0f, -5f,
             0f,    0f,    0f,   1f,  0f,
        )).apply {
            // contrast ×1.2，bias = 128*(1−1.2) = −26
            postConcat(ColorMatrix(floatArrayOf(
                1.2f, 0f,   0f,   0f, -26f,
                0f,   1.2f, 0f,   0f, -26f,
                0f,   0f,   1.2f, 0f, -26f,
                0f,   0f,   0f,   1f,   0f,
            )))
        }
    }),

    /**
     * 模擬 Kodak Portra 400 底片：微去飽和、提亮暗部（黑不到底）、輕微暖調。
     * 黑色輸出 RGB(28, 24, 20) = 帶暖意的深灰；白色輸出略帶奶白。
     */
    Portra("Portra", {
        ColorMatrix().apply {
            setSaturation(0.85f)
            postConcat(ColorMatrix(floatArrayOf(
                0.88f, 0f,    0f,   0f, 28f,
                0f,    0.85f, 0f,   0f, 24f,
                0f,    0f,    0.80f,0f, 20f,
                0f,    0f,    0f,   1f,  0f,
            )))
        }
    }),

    /**
     * 黃金時刻：強烈橙金色調，大幅壓低藍色，模擬夕陽低角度暖光。
     */
    GoldenHour("Golden Hour", {
        ColorMatrix(floatArrayOf(
            1.25f, 0f,    0f,    0f,  15f,
            0f,    1.05f, 0f,    0f,   8f,
            0f,    0f,    0.55f, 0f, -15f,
            0f,    0f,    0f,    1f,   0f,
        ))
    }),

    /**
     * 膠捲霧面感（Instagram / VSCO 最熱門）：黑色被提亮至 RGB(55, 50, 45) 而非純黑，
     * 高光略壓縮，輕去飽和，打造「黑不到底」的 matte 低對比美學。
     */
    Matte("Matte", {
        ColorMatrix().apply {
            setSaturation(0.65f)
            postConcat(ColorMatrix(floatArrayOf(
                0.75f, 0f,    0f,    0f, 55f,
                0f,    0.75f, 0f,    0f, 50f,
                0f,    0f,    0.72f, 0f, 45f,
                0f,    0f,    0f,    1f,  0f,
            )))
        }
    }),

    /**
     * 電影黑白：完全去色後套用 ×1.7 高對比（bias = 128*(1−1.7) = −90），
     * 暗部更深、亮部更亮，呈現強烈光影層次。
     */
    Mono("Mono", {
        ColorMatrix().apply {
            setSaturation(0f)
            postConcat(ColorMatrix(floatArrayOf(
                1.7f, 0f,   0f,   0f, -90f,
                0f,   1.7f, 0f,   0f, -90f,
                0f,   0f,   1.7f, 0f, -90f,
                0f,   0f,   0f,   1f,   0f,
            )))
        }
    }),
}
