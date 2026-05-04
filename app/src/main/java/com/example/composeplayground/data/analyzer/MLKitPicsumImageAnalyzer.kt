package com.example.composeplayground.data.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit Image Labeling 實作。
 *
 * - 用 Coil 的 ImageLoader 載入縮圖（[ANALYZE_THUMB_SIZE] px）；hardware bitmap 不能餵 ML Kit
 *   故 `allowHardware(false)`
 * - 用 [suspendCancellableCoroutine] 包 Tasks API，避免引入 `kotlinx-coroutines-play-services`
 * - [LruCache] 以 photoId 為 key，singleton scope 跨頁面共用結果
 */
class MLKitPicsumImageAnalyzer(
    private val appContext: Context,
) : PicsumImageAnalyzer {

    private val labeler: ImageLabeler =
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    private val cache = LruCache<String, List<String>>(CACHE_SIZE)

    override suspend fun summarize(photoId: String, imageUrl: String): Result<List<String>> {
        cache.get(photoId)?.let { return Result.success(it) }

        return withContext(Dispatchers.Default) {
            runCatching {
                val bitmap = loadBitmap(imageUrl)
                    ?: error("無法載入縮圖：$imageUrl")
                val labels = runLabeler(bitmap).map { it.text }
                cache.put(photoId, labels)
                labels
            }
        }
    }

    private suspend fun loadBitmap(imageUrl: String): Bitmap? {
        val request = ImageRequest.Builder(appContext)
            .data(imageUrl)
            .allowHardware(false)
            .build()
        val result = appContext.imageLoader.execute(request)
        if (result !is SuccessResult) return null
        val drawable = result.image.asDrawable(appContext.resources)
        return (drawable as? BitmapDrawable)?.bitmap
    }

    private suspend fun runLabeler(bitmap: Bitmap): List<com.google.mlkit.vision.label.ImageLabel> =
        suspendCancellableCoroutine { cont ->
            val input = InputImage.fromBitmap(bitmap, 0)
            labeler.process(input)
                .addOnSuccessListener { labels -> if (cont.isActive) cont.resume(labels) }
                .addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
        }

    companion object {
        const val ANALYZE_THUMB_SIZE = 384
        private const val CACHE_SIZE = 128
    }
}
