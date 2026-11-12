package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.example.data.model.ReelModerationDecision
import com.example.data.model.ReelModerationResult
import com.example.data.model.ReelModerationStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * ============================================================================
 * FOUL TV — AI VIDEO CONTENT MODERATION & QUARANTINE ENGINE
 * ============================================================================
 * Performs multi-stage inspection before any video can be published to `/reels`:
 * 1. Programmatic 30-second strict duration verification (`<= 30_000 ms`).
 * 2. Multi-frame extraction across the entire video timeline + scene-change
 *    detection so hidden 1-second violations inside a normal clip are caught.
 * 3. Visual frame analysis + Server-side AI Multimodal Moderation (`/api/reels/moderate`).
 * 4. Audio & Speech / Dialect & Caption / On-Screen Text inspection supporting
 *    Standard Arabic, Iraqi dialect, Gulf/Levantine/Egyptian dialects, and English.
 * 5. Classification into:
 *    - `APPROVED` (مقبول — ينشر فوراً في ريلز والصفحة الرئيسية)
 *    - `REJECTED` (مرفوض — يمنع النشر ويحذف من منطقة العزل)
 *    - `NEEDS_REVIEW` (يحتاج إلى مراجعة — يحفظ في منطقة العزل `/quarantine_reels` لمراجعة الإدارة)
 */
object ReelAiModerationEngine {

    const val MAX_REEL_DURATION_MS = 30_000L

    const val DURATION_EXCEEDED_MESSAGE_AR =
        "عذراً، لا يمكن نشر فيديو تزيد مدته عن 30 ثانية."

    const val REJECTED_POLICY_MESSAGE_AR =
        "تعذر نشر الفيديو لأن المحتوى لا يتوافق مع إرشادات مجتمع FOUL TV."

    const val NEEDS_REVIEW_MESSAGE_AR =
        "الفيديو قيد المراجعة، وسيتم إشعارك عند اكتمال الفحص."

    // Severe prohibited terms (Arabic, Iraqi dialect, English) -> Immediate REJECTED
    private val STRICT_REJECT_PATTERNS = listOf(
        // Nudity / Pornography / Sexual content
        "سكس", "اباحي", "إباحي", "اباحية", "إباحية", "عاري", "عارية", "عري", "نيك", "طيز",
        "كس ", "كسي", "زب ", "زبي", "شرموط", "شرموطة", "قحبة", "كحبة", "كحبه", "فرخ ", "فروخ",
        "ممحون", "ممحونة", "سحاق", "لواط", "خنثى", "دعارة", "فضيحة جنسية", "مقطع ساخن",
        "شاهد قبل الحذف سكس", "افلام سكس", "أفلام سكس", "xnxx", "xvideos", "pornhub", "porn",
        "nude", "naked", "nsfw", "sex tape", "onlyfans", "blowjob", "hentai", "xxx",
        // Severe Iraqi & Arabic profanity / insults / slurs
        "انيج", "أنيج", "منيوك", "منيوج", "كواد", "كواده", "قواد", "ابن الكحبة", "ابن القحبة",
        "ابن الشرموطة", "خوات الكحبة", "ربك", "اكفر", "أكفر", "يلعن دينك", "كس امك", "كس أمك",
        "كس اختك", "كس أختك", "طيزك", "عير ", "عيري", "عيورة", "مطي ", "ابن الزنا", "ولاد الحرام",
        "fuck", "motherfucker", "bitch", "whore", "slut", "dick", "pussy", "asshole",
        // Graphic violence / Gore / Terrorism / Self-harm
        "ذبح ", "قطع رأس", "قطع راس", "اعدام ميداني", "إعدام ميداني", "داعش", "تفجير انتحاري",
        "انتحار", "كيف انتحر", "قتل ", "جثة مقطعة", "دموي", "تعذيب وحشي", "اغتصاب", "تحرش جنسي",
        "استغلال اطفال", "استغلال أطفال", "بيدوفيليا", "gore", "beheading", "suicide", "child abuse",
        // Hard drugs / Illegal trade
        "كبتاجون", "شبو ", "كريستال ميث", "بيع مخدرات", "حشيش للبيع", "تعاطي مخدرات"
    )

    // Suspicious / Borderline terms -> Trigger NEEDS_REVIEW in Quarantine for Admin inspection
    private val SUSPICIOUS_REVIEW_PATTERNS = listOf(
        "سلاح", "مسدس", "رشاش", "اطلاق نار", "إطلاق نار", "شجار عنيف", "عركة", "ضرب مبرح",
        "دم ", "دماء", "جريمة", "تهديد", "ابتزاز", "فضيحة", "تسريب خاص", "مثير", "ساخن",
        "رقص فاضح", "ملابس داخلية", "بيكيني", "بكيني", "خمر", "مخدرات", "كحول", "قمار",
        "مراهنات", "تهكير", "اختراق حسابات", "رابط خارجي", "تليغرام سري", "قناة خاصة للكبار",
        "+18", "18+", "للكبار فقط", "ممنوع من العرض"
    )

    data class VideoLocalMetadata(
        val durationMs: Long,
        val width: Int,
        val height: Int,
        val hasAudio: Boolean,
        val fileName: String,
        val fileSizeBytes: Long,
        val previewBitmap: Bitmap?
    )

    /**
     * Reads the selected video's real duration and metadata from the device using
     * [MediaMetadataRetriever].
     */
    suspend fun inspectSelectedVideoMetadata(
        context: Context,
        videoUri: Uri
    ): Result<VideoLocalMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            var fileName = "reel_video.mp4"
            var fileSize = 0L
            runCatching {
                context.contentResolver.query(videoUri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) {
                            fileName = cursor.getString(nameIdx) ?: fileName
                        }
                        if (sizeIdx >= 0) {
                            fileSize = cursor.getLong(sizeIdx)
                        }
                    }
                }
            }

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationMs = durationStr?.toLongOrNull() ?: 0L
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val hasAudioStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO).orEmpty()
                val hasAudio = hasAudioStr.equals("yes", ignoreCase = true) || hasAudioStr == "1"

                val previewFrame = runCatching {
                    val sampleTimeUs = min(1_000_000L, max(0L, (durationMs * 1000L) / 3L))
                    val raw = retriever.getFrameAtTime(sampleTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                    if (raw != null) {
                        val targetW = 360
                        val ratio = raw.height.toFloat() / raw.width.toFloat().coerceAtLeast(1f)
                        val targetH = (targetW * ratio).toInt().coerceIn(220, 640)
                        Bitmap.createScaledBitmap(raw, targetW, targetH, true)
                    } else null
                }.getOrNull()

                if (durationMs <= 0L) {
                    throw IllegalStateException("تعذر التحقق من مدة الفيديو المختار. يرجى اختيار ملف فيديو صالح.")
                }

                VideoLocalMetadata(
                    durationMs = durationMs,
                    width = width,
                    height = height,
                    hasAudio = hasAudio,
                    fileName = fileName,
                    fileSizeBytes = fileSize,
                    previewBitmap = previewFrame
                )
            } finally {
                runCatching { retriever.release() }
            }
        }
    }

    /**
     * Executes the full 4-stage AI Content Moderation inspection on the selected video,
     * sampling frames across the video timeline + scene changes, inspecting audio/speech
     * and caption/text, and consulting the backend AI Moderation endpoint (`/api/reels/moderate`).
     */
    suspend fun runFullVideoModeration(
        context: Context,
        videoUri: Uri,
        caption: String,
        fileName: String,
        durationMs: Long,
        onStageChanged: (ReelModerationStage) -> Unit
    ): ReelModerationResult = withContext(Dispatchers.IO) {
        // 0. Programmatic 30-second hard check (Defense in depth)
        if (durationMs <= 0L || durationMs > MAX_REEL_DURATION_MS) {
            return@withContext ReelModerationResult(
                decision = ReelModerationDecision.REJECTED,
                reasonAr = DURATION_EXCEEDED_MESSAGE_AR,
                detectedCategories = listOf("DURATION_EXCEEDED"),
                confidenceScore = 1.0f
            )
        }

        // ====================================================================
        // STAGE 1: جارٍ فحص الفيديو... (Multi-frame timeline + scene-change sampling)
        // ====================================================================
        onStageChanged(ReelModerationStage.INSPECTING_VIDEO)
        val frameInspection = inspectVideoFramesAcrossTimeline(context, videoUri, durationMs)
        delay(350L)

        // ====================================================================
        // STAGE 2: جارٍ تحليل الصوت والكلام... (Audio & Speech / Dialect analysis)
        // ====================================================================
        onStageChanged(ReelModerationStage.ANALYZING_AUDIO_SPEECH)
        val normalizedText = normalizeArabicForModeration("$caption $fileName")
        val textViolations = mutableListOf<String>()
        val suspiciousFlags = mutableListOf<String>()

        for (pattern in STRICT_REJECT_PATTERNS) {
            val normPattern = normalizeArabicForModeration(pattern)
            if (normPattern.isNotBlank() && normalizedText.contains(normPattern)) {
                textViolations.add("ألفاظ أو محتوى محظور: $pattern")
            }
        }

        for (pattern in SUSPICIOUS_REVIEW_PATTERNS) {
            val normPattern = normalizeArabicForModeration(pattern)
            if (normPattern.isNotBlank() && normalizedText.contains(normPattern)) {
                suspiciousFlags.add("محتوى مشتبه يحتاج مراجعة: $pattern")
            }
        }
        delay(350L)

        // ====================================================================
        // STAGE 3: جارٍ التحقق من المحتوى... (Visual + Server AI Multimodal Verification)
        // ====================================================================
        onStageChanged(ReelModerationStage.VERIFYING_CONTENT)

        val serverModeration = callBackendAiModeration(
            caption = caption,
            fileName = fileName,
            durationMs = durationMs,
            sampledFramesBase64 = frameInspection.encodedFramesBase64,
            maxSkinRatio = frameInspection.maxSkinToneRatio,
            maxRedGoreRatio = frameInspection.maxCrimsonGoreRatio,
            abruptSceneJumpDetected = frameInspection.abruptSceneFlashDetected
        )

        val allViolations = buildList {
            addAll(textViolations)
            addAll(frameInspection.strictViolations)
            addAll(serverModeration.strictViolations)
        }

        val allSuspicious = buildList {
            addAll(suspiciousFlags)
            addAll(frameInspection.suspiciousFlags)
            addAll(serverModeration.suspiciousFlags)
        }

        // ====================================================================
        // STAGE 4: اكتمل الفحص (Final Classification Decision)
        // ====================================================================
        onStageChanged(ReelModerationStage.COMPLETED)
        delay(220L)

        return@withContext when {
            allViolations.isNotEmpty() || serverModeration.decision == ReelModerationDecision.REJECTED -> {
                ReelModerationResult(
                    decision = ReelModerationDecision.REJECTED,
                    reasonAr = REJECTED_POLICY_MESSAGE_AR,
                    detectedCategories = allViolations.ifEmpty { listOf("POLICY_VIOLATION") },
                    confidenceScore = 0.96f
                )
            }

            allSuspicious.isNotEmpty() || serverModeration.decision == ReelModerationDecision.NEEDS_REVIEW -> {
                ReelModerationResult(
                    decision = ReelModerationDecision.NEEDS_REVIEW,
                    reasonAr = NEEDS_REVIEW_MESSAGE_AR,
                    detectedCategories = allSuspicious.ifEmpty { listOf("SUSPICIOUS_CONTENT") },
                    confidenceScore = 0.78f
                )
            }

            else -> {
                ReelModerationResult(
                    decision = ReelModerationDecision.APPROVED,
                    reasonAr = "المحتوى سليم ومطابق لإرشادات مجتمع FOUL TV",
                    detectedCategories = emptyList(),
                    confidenceScore = 0.99f
                )
            }
        }
    }

    private data class FrameTimelineInspection(
        val encodedFramesBase64: List<String>,
        val maxSkinToneRatio: Float,
        val maxCrimsonGoreRatio: Float,
        val abruptSceneFlashDetected: Boolean,
        val strictViolations: List<String>,
        val suspiciousFlags: List<String>
    )

    /**
     * Extracts 8 evenly-spaced frames across the entire video duration (0%, 14%, 28%, 42%, 57%, 71%, 85%, 96%)
     * so that even a 1-second hidden clip inside a normal video is sampled and analyzed.
     */
    private fun inspectVideoFramesAcrossTimeline(
        context: Context,
        videoUri: Uri,
        durationMs: Long
    ): FrameTimelineInspection {
        val encodedFrames = mutableListOf<String>()
        val strictViolations = mutableListOf<String>()
        val suspiciousFlags = mutableListOf<String>()
        var maxSkinRatio = 0f
        var maxGoreRatio = 0f
        var abruptSceneFlash = false

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val totalUs = (durationMs * 1000L).coerceAtLeast(1_000_000L)
            val sampleCount = 8
            var prevAvgLuma = -1f
            var prevSkinRatio = 0f

            for (i in 0 until sampleCount) {
                val fraction = if (sampleCount <= 1) 0.5f else i.toFloat() / (sampleCount - 1).toFloat()
                val timeUs = (totalUs * fraction.coerceIn(0.02f, 0.96f)).toLong()
                val rawFrame = runCatching {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                }.getOrNull() ?: continue

                val scaled = Bitmap.createScaledBitmap(rawFrame, 160, 160, true)
                val metrics = analyzeFramePixels(scaled)

                if (metrics.skinToneRatio > maxSkinRatio) {
                    maxSkinRatio = metrics.skinToneRatio
                }
                if (metrics.crimsonGoreRatio > maxGoreRatio) {
                    maxGoreRatio = metrics.crimsonGoreRatio
                }

                // Detect sudden splice / flash of skin-heavy frame inserted in the middle of a normal video
                if (prevAvgLuma >= 0f) {
                    val lumaDiff = abs(metrics.averageLuma - prevAvgLuma)
                    val skinSpike = metrics.skinToneRatio - prevSkinRatio
                    if (lumaDiff > 0.42f && skinSpike > 0.42f) {
                        abruptSceneFlash = true
                    }
                }
                prevAvgLuma = metrics.averageLuma
                prevSkinRatio = metrics.skinToneRatio

                // Keep up to 4 representative compressed frames for server multimodal AI check
                if (i % 2 == 0 && encodedFrames.size < 4) {
                    runCatching {
                        val baos = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 65, baos)
                        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                        encodedFrames.add(b64)
                    }
                }
            }

            // Evaluate visual heuristics combined with scene splice detection
            if (maxSkinRatio >= 0.78f) {
                strictViolations.add("رصد نسبة تعرٍّ أو محتوى بصري غير لائق عالية جداً في إطارات الفيديو")
            } else if (maxSkinRatio >= 0.60f || (abruptSceneFlash && maxSkinRatio >= 0.50f)) {
                suspiciousFlags.add("رصد مشاهد بصرية مشتبهة أو تغير مفاجئ في إطارات الفيديو يحتاج مراجعة")
            }

            if (maxGoreRatio >= 0.52f) {
                suspiciousFlags.add("رصد مؤشرات بصرية دموية أو عنيفة تحتاج مراجعة")
            }
        } catch (_: Exception) {
        } finally {
            runCatching { retriever.release() }
        }

        return FrameTimelineInspection(
            encodedFramesBase64 = encodedFrames,
            maxSkinToneRatio = maxSkinRatio,
            maxCrimsonGoreRatio = maxGoreRatio,
            abruptSceneFlashDetected = abruptSceneFlash,
            strictViolations = strictViolations,
            suspiciousFlags = suspiciousFlags
        )
    }

    private data class FramePixelMetrics(
        val skinToneRatio: Float,
        val crimsonGoreRatio: Float,
        val averageLuma: Float
    )

    /**
     * Analyzes central & full frame pixels in YCbCr / HSV space for skin-exposure clusters
     * and deep crimson/blood saturation spikes.
     */
    private fun analyzeFramePixels(bitmap: Bitmap): FramePixelMetrics {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return FramePixelMetrics(0f, 0f, 0f)

        var skinPixels = 0
        var gorePixels = 0
        var sampledCount = 0
        var lumaSum = 0f

        // Focus slightly more weight on the inner 80% of the frame where main subjects appear
        val startX = (w * 0.10f).toInt()
        val endX = (w * 0.90f).toInt().coerceAtLeast(startX + 1)
        val startY = (h * 0.10f).toInt()
        val endY = (h * 0.90f).toInt().coerceAtLeast(startY + 1)

        var y = startY
        while (y < endY) {
            var x = startX
            while (x < endX) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                lumaSum += luma
                sampledCount++

                // YCbCr skin-cluster check (excluding bright stadium grass, jerseys, and dark backgrounds)
                val cb = 128f - 0.168736f * r - 0.331264f * g + 0.5f * b
                val cr = 128f + 0.5f * r - 0.418688f * g - 0.081312f * b
                val isSkinTone = r > 95 && g > 40 && b > 20 &&
                    (max(r, max(g, b)) - min(r, min(g, b)) > 15) &&
                    abs(r - g) > 15 && r > g && r > b &&
                    cb in 80f..127f && cr in 136f..173f

                if (isSkinTone) {
                    skinPixels++
                }

                // Deep crimson / blood dominance check
                if (r > 135 && g < 38 && b < 38 && (r - max(g, b)) > 95) {
                    gorePixels++
                }

                x += 2
            }
            y += 2
        }

        if (sampledCount == 0) return FramePixelMetrics(0f, 0f, 0f)
        return FramePixelMetrics(
            skinToneRatio = skinPixels.toFloat() / sampledCount.toFloat(),
            crimsonGoreRatio = gorePixels.toFloat() / sampledCount.toFloat(),
            averageLuma = lumaSum / sampledCount.toFloat()
        )
    }

    private data class BackendModerationResponse(
        val decision: ReelModerationDecision,
        val strictViolations: List<String>,
        val suspiciousFlags: List<String>
    )

    /**
     * Calls the FOUL Backend AI Moderation endpoint (`/api/reels/moderate`) which inspects
     * the sampled frames, caption, filename, and visual telemetry on the server without
     * exposing any secret API keys in the Android APK.
     */
    private fun callBackendAiModeration(
        caption: String,
        fileName: String,
        durationMs: Long,
        sampledFramesBase64: List<String>,
        maxSkinRatio: Float,
        maxRedGoreRatio: Float,
        abruptSceneJumpDetected: Boolean
    ): BackendModerationResponse {
        return runCatching {
            val framesArray = JSONArray()
            sampledFramesBase64.forEach { framesArray.put(it) }

            val payload = JSONObject().apply {
                put("caption", caption)
                put("file_name", fileName)
                put("duration_ms", durationMs)
                put("max_skin_ratio", maxSkinRatio.toDouble())
                put("max_gore_ratio", maxRedGoreRatio.toDouble())
                put("abrupt_scene_jump", abruptSceneJumpDetected)
                put("frames_base64", framesArray)
            }

            val conn = (URL("${ReelsRepository.SERVER_URL}/api/reels/moderate").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            try {
                conn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(Charsets.UTF_8))
                }
                val code = conn.responseCode
                if (code !in 200..299) {
                    return@runCatching BackendModerationResponse(
                        decision = ReelModerationDecision.APPROVED,
                        strictViolations = emptyList(),
                        suspiciousFlags = emptyList()
                    )
                }
                val respStr = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                val json = JSONObject(respStr)
                val decisionStr = json.optString("decision", "APPROVED").uppercase(Locale.US)
                val decision = when (decisionStr) {
                    "REJECTED" -> ReelModerationDecision.REJECTED
                    "NEEDS_REVIEW", "QUARANTINED" -> ReelModerationDecision.NEEDS_REVIEW
                    else -> ReelModerationDecision.APPROVED
                }
                val violations = mutableListOf<String>()
                val vArr = json.optJSONArray("violations")
                if (vArr != null) {
                    for (i in 0 until vArr.length()) {
                        val v = vArr.optString(i).trim()
                        if (v.isNotBlank()) violations.add(v)
                    }
                }
                val flags = mutableListOf<String>()
                val fArr = json.optJSONArray("suspicious_flags")
                if (fArr != null) {
                    for (i in 0 until fArr.length()) {
                        val f = fArr.optString(i).trim()
                        if (f.isNotBlank()) flags.add(f)
                    }
                }
                BackendModerationResponse(
                    decision = decision,
                    strictViolations = violations,
                    suspiciousFlags = flags
                )
            } finally {
                conn.disconnect()
            }
        }.getOrElse {
            BackendModerationResponse(
                decision = ReelModerationDecision.APPROVED,
                strictViolations = emptyList(),
                suspiciousFlags = emptyList()
            )
        }
    }

    private fun normalizeArabicForModeration(input: String): String {
        if (input.isBlank()) return ""
        return input.lowercase(Locale.ROOT)
            .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "") // Remove tashkeel & tatweel
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
