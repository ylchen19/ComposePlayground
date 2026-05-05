package com.example.composeplayground.data.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.util.LruCache
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 圖片描述三層策略（全程 on-device）：
 *
 * 1. **Gemini Nano 多模態**（Pixel 9 Pro+ 支援）：直接把圖片餵給模型，語意理解最準確。
 * 2. **ML Kit 標籤 → Gemini Nano 文字模式**（Pixel 8 / 9a 等有 Gemini Nano 但不支援多模態）：
 *    先用 ML Kit 抓出視覺元素，再讓 LLM 把標籤組成自然語句，品質遠優於模板。
 * 3. **ML Kit 標籤 → 句子模板**（不支援 Gemini Nano 的裝置）：純本地邏輯，即時可用。
 *
 * [LruCache] 以 photoId 為 key，跨頁面共用快取。
 *
 * 診斷：adb logcat -s GeminiNanoAnalyzer
 */
class GeminiNanoPicsumImageAnalyzer(
    private val appContext: Context,
    private val modelManager: GeminiNanoModelManager,
) : PicsumImageAnalyzer {

    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    private val cache = LruCache<String, String>(CACHE_SIZE)

    override suspend fun summarize(
        photoId: String,
        imageUrl: String,
    ): Result<String> {
        cache.get(photoId)?.let { return Result.success(it) }

        return withContext(Dispatchers.Default) {
            runCatching {
                val bitmap = loadBitmap(imageUrl) ?: error("無法載入縮圖：$imageUrl")
                val description = generateDescription(bitmap)
                cache.put(photoId, description)
                description
            }
        }
    }

    // ── 主流程 ───────────────────────────────────────────────────────────────

    private suspend fun generateDescription(bitmap: Bitmap): String {
        val model = modelManager.ensureReady()
        // 先跑 ML Kit，讓標籤在 Tier 1 失敗時可立即重用，不必重跑
        val labels = runLabelerSafe(bitmap)
        Log.d(TAG, "ML Kit labels: ${labels.map { "${it.first}(${String.format("%.2f", it.second)})" }}")

        if (model != null) {
            // Tier 1：多模態（Pixel 9 Pro+）
            val multimodalResult = tryMultimodal(model, bitmap)
            if (multimodalResult != null) {
                Log.d(TAG, "Tier 1 (multimodal) succeeded")
                return multimodalResult
            }

            // Tier 2：ML Kit 標籤 → Gemini Nano 文字模式（Pixel 8/9a+）
            val textResult = tryTextOnly(model, labels)
            if (textResult != null) {
                Log.d(TAG, "Tier 2 (text-only) succeeded")
                return textResult
            }

            Log.w(TAG, "Tier 1 & 2 both failed, falling to template")
            return buildMlKitDescription(labels)
        }

        // Tier 3：無 Gemini Nano 支援，純模板
        Log.w(TAG, "No Gemini Nano model, using template")
        return buildMlKitDescription(labels)
    }

    // ── Gemini Nano ──────────────────────────────────────────────────────────

    /** 多模態推論；Pixel 8/9a 不支援圖像輸入時擲例外，catch 後回傳 null。 */
    private suspend fun tryMultimodal(model: GenerativeModel, bitmap: Bitmap): String? = try {
        val request = GenerateContentRequest.builder(
            ImagePart(bitmap),
            TextPart("請用一句繁體中文描述這張照片的主要內容，簡潔扼要，不超過30字。只輸出描述句，不要加標點以外的任何說明。"),
        ).build()
        model.generateContent(request).firstCandidate()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.d(TAG, "Tier 1 multimodal failed: ${e.javaClass.simpleName}: ${e.message}")
        null
    }

    /**
     * 把 ML Kit 標籤傳給文字模式的 Gemini Nano，讓 LLM 寫自然語句。
     *
     * Prompt 設計原則：
     * - 簡短指令（短 prompt 在小模型上比長 prompt 更穩定）
     * - 不說「most confident first」以免觸發表格輸出（Samsung Gemini Nano 已觀察到）
     * - 明確禁止表格、條列
     * - 信心度 ≥ 0.6 的標籤優先（最多 8 個），不足時退回 top-5
     */
    private suspend fun tryTextOnly(model: GenerativeModel, labels: List<Pair<String, Float>>): String? = try {
        if (labels.isEmpty()) return null
        val selected = labels.filter { it.second >= 0.6f }.take(8).ifEmpty { labels.take(5) }
        val labelList = selected.joinToString(", ") { it.first }
        val prompt = "Image labels: $labelList.\n" +
            "Write one sentence in Traditional Chinese describing this photo. " +
            "Output only the sentence, no tables, no lists."
        val request = GenerateContentRequest.builder(TextPart(prompt)).build()
        val candidate = model.generateContent(request).firstCandidate()
        Log.d(TAG, "Tier 2 raw response: $candidate")
        candidate
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Tier 2 text-only failed: ${e.javaClass.simpleName}: ${e.message}")
        null
    }

    /**
     * 逐行找第一個有效中文描述句，過濾表格線、條列符號與過短/過長的行。
     *
     * Samsung 等裝置的 Gemini Nano 有時會把標籤輸出成 Unicode 表格
     * （含 ├ ┼ │ 等字元），此處直接拒絕含有這些字元的行，
     * 只接受 6–60 字的純中文句子。
     */
    private fun com.google.mlkit.genai.prompt.GenerateContentResponse.firstCandidate(): String? {
        val raw = candidates.firstOrNull()?.text?.trim() ?: return null
        return raw.lineSequence()
            .map { it.trim() }
            .filter { line ->
                line.isNotBlank() &&
                // 排除表格線、分隔線、條列符號
                line.none { c -> c in "├┤┼│┌┐└┘─|+-" && line.count { it == c } > 3 } &&
                // 至少含 6 個中文字（確認是中文句子，非英文說明）
                line.count { it.code in 0x4E00..0x9FFF } >= 6
            }
            .firstOrNull()
            ?.takeIf { it.length <= 60 }  // 超過 60 字通常是說明文字而非描述句
    }

    // ── ML Kit ───────────────────────────────────────────────────────────────

    private suspend fun runLabelerSafe(bitmap: Bitmap): List<Pair<String, Float>> = try {
        runLabeler(bitmap).sortedByDescending { it.confidence }.map { Pair(it.text, it.confidence) }
    } catch (_: Exception) { emptyList() }

    private suspend fun runLabeler(
        bitmap: Bitmap,
    ): List<com.google.mlkit.vision.label.ImageLabel> =
        suspendCancellableCoroutine { cont ->
            labeler.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        }

    private fun buildMlKitDescription(labels: List<Pair<String, Float>>): String {
        if (labels.isEmpty()) return "無法識別圖片內容。"

        val top = labels.take(8)
        val names = top.map { it.first }.toSet()

        // ── 修飾語（非主角，作為場景補充）────────────────────────────────────
        val isVacation = top.any { it.first in setOf("Vacation", "Leisure") && it.second >= 0.6f }
        val isSunset   = "Sunset" in names
        val isSunrise  = "Sunrise" in names
        val isNight    = "Night" in names
        val isSnow     = names.any { it in setOf("Snow", "Ice") }
        val hasSky     = "Sky" in names
        val hasCloud   = "Cloud" in names
        val hasFog     = "Fog" in names
        val hasRock    = "Rock" in names

        // ── 主場景 ───────────────────────────────────────────────────────────
        val isCoastal  = names.any { it in setOf("Beach", "Coast", "Sea", "Ocean", "Sand") }
        val isMountain = names.any { it in setOf("Mountain", "Mountain range", "Cliff") }
        val isWater    = !isCoastal && names.any { it in setOf("Lake", "River", "Waterfall") }
        val isForest   = names.any { it in setOf("Forest", "Tree") }
        val isFloral   = !isForest && names.any { it in setOf("Flower", "Plant") }
        val isUrban    = names.any { it in setOf("City", "Building", "Architecture", "Street", "Bridge", "Road") }
        val isPeople   = names.any { it in setOf("Person", "Portrait", "Crowd") }
        val isAnimal   = names.any { it in setOf("Dog", "Cat", "Bird", "Animal") }
        val isFood     = names.any { it in setOf("Food", "Drink") }
        val isFireworks = "Fireworks" in names

        return when {
            isFireworks -> "絢爛的煙火在夜空中盛開，色彩繽紛的光芒劃破夜幕。"

            isFood -> "色香俱全的食物特寫，呈現出令人垂涎的視覺享受。"

            isPeople -> if ("Crowd" in names) "人群聚集的熱鬧場景，記錄著生活中的精彩時刻。"
                        else "以人物為主角的影像，神情姿態捕捉了真實的生活瞬間。"

            isAnimal -> {
                val specific = top.firstOrNull { it.first in setOf("Dog", "Cat", "Bird") }?.first
                "一張${LABEL_TRANSLATIONS[specific] ?: "動物"}的生動特寫，展現出生命的靈動神韻。"
            }

            isSnow -> "白雪覆蓋的冰封景象，銀白世界呈現出純淨的冬日風情。"

            // 海岸 + 山崖組合（如本次範例）
            isCoastal && isMountain -> {
                val suffix = if (isVacation) "，宛如令人嚮往的度假勝地" else ""
                "礁岩海岸搭配山崖的壯麗景色，藍天映照下美不勝收${suffix}。"
            }

            isCoastal -> {
                val suffix = if (isVacation) "，充滿悠閒的度假氛圍" else ""
                if (hasRock) "礁石海岸的自然風光，海浪與岩石交織出壯觀的濱海景色${suffix}。"
                else "開闊的海灘風景，海天一色呈現出迷人的濱海美景${suffix}。"
            }

            isMountain -> when {
                hasFog     -> "雲霧繚繞的山巒景色，朦朧霧氣更顯山勢的神秘壯闊。"
                hasSky     -> "巍峨山脈與蒼茫天際交相輝映，展現出大自然的雄偉壯闊。"
                else       -> "高山峻嶺的壯闊景色，岩石峭壁構成大自然的磅礴畫卷。"
            }

            isWater -> when {
                "Waterfall" in names -> "奔流而下的瀑布，訴說著自然水流的磅礴力量與壯麗之美。"
                "River" in names     -> "蜿蜒河流的靜謐風光，水面倒影映照出四周的自然景致。"
                else                 -> "靜謐湖面映照著四周景色，宛如一面天然的鏡子呈現出靜美風光。"
            }

            isSunset  -> "天際染上橙紅色的壯麗晚霞，光影變幻渲染出令人屏息的夕陽美景。"
            isSunrise -> "朝霞初現的晨光美景，金色光芒灑遍天際迎來嶄新的一天。"

            isNight && isUrban -> "城市在夜幕下璀璨生輝，萬家燈火交織出都會的迷人夜景。"
            isUrban            -> "城市建築的都會風貌，幾何線條勾勒出現代都市的繁華輪廓。"

            isFloral -> "生機盎然的花卉特寫，鮮豔色彩展現出大自然的旺盛生命力。"

            isForest -> if (hasFog) "霧氣瀰漫的幽靜林野，朦朧景致別有一番神秘氛圍。"
                        else "鬱鬱蔥蔥的自然林野，陽光穿透葉縫灑下斑駁的迷人光影。"

            hasFog               -> "霧氣瀰漫的朦朧景象，如夢似幻的氛圍令人心曠神怡。"
            hasSky || hasCloud   -> "開闊的天空景象，雲層流動展現出自然氣象的壯麗變幻。"

            else -> {
                // 兜底：用翻譯標籤組句
                val translated = labels.take(3).map { (text, _) -> LABEL_TRANSLATIONS[text] ?: text }
                if (translated.size >= 2) "這張圖片以${translated[0]}與${translated[1]}為主，展現出獨特的視覺風貌。"
                else "這張圖片呈現${translated[0]}的景象。"
            }
        }
    }

    // ── 共用工具 ─────────────────────────────────────────────────────────────

    private suspend fun loadBitmap(imageUrl: String): Bitmap? {
        val request = ImageRequest.Builder(appContext)
            .data(imageUrl)
            .allowHardware(false)
            .build()
        val result = appContext.imageLoader.execute(request)
        if (result !is SuccessResult) return null
        return (result.image.asDrawable(appContext.resources) as? BitmapDrawable)?.bitmap
    }

    companion object {
        const val ANALYZE_THUMB_SIZE = 384
        private const val CACHE_SIZE = 128
        private const val TAG = "GeminiNanoAnalyzer"

        private val LABEL_TRANSLATIONS = mapOf(
            "Sky" to "天空", "Cloud" to "雲朵", "Mountain" to "山脈",
            "Mountain range" to "山脈", "Nature" to "自然景觀",
            "Lake" to "湖泊", "Water" to "水體", "Ocean" to "海洋",
            "Sea" to "海洋", "Beach" to "海灘", "Coast" to "海岸",
            "Forest" to "森林", "Tree" to "樹木", "Grass" to "草地",
            "Plant" to "植物", "Flower" to "花卉", "Leaf" to "葉子",
            "Snow" to "雪景", "Ice" to "冰雪", "Fog" to "霧",
            "Sunset" to "日落", "Sunrise" to "日出", "Night" to "夜景",
            "City" to "城市", "Building" to "建築", "Architecture" to "建築",
            "Street" to "街道", "Bridge" to "橋樑", "Road" to "道路",
            "Person" to "人物", "Portrait" to "人像", "Crowd" to "人群",
            "Animal" to "動物", "Dog" to "狗", "Cat" to "貓",
            "Bird" to "鳥類", "Food" to "食物", "Drink" to "飲品",
            "Outdoor" to "戶外", "Indoor" to "室內", "Landscape" to "風景",
            "Reflection" to "倒影", "Sand" to "沙地", "Rock" to "岩石",
            "River" to "河流", "Waterfall" to "瀑布", "Field" to "田野",
            "Abstract" to "抽象", "Fireworks" to "煙火", "Rain" to "雨景",
        )
    }
}
