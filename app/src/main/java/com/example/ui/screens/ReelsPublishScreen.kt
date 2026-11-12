package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReelModerationStage
import com.example.data.repository.ReelAiModerationEngine
import kotlinx.coroutines.launch
import java.util.Locale

// ============================================================================
// Official Reference Palette for FOUL TV Full-Screen Publish Screen
// ============================================================================
private val PublishBgDeepBlack = Color(0xFF050505)
private val PublishCardBg = Color(0xFF101014)
private val PublishTextPrimary = Color(0xFFFFFFFF)
private val PublishTextSecondary = Color(0xFFAAAAB5)
private val PublishAccentPurple = Color(0xFF862BEB)
private val PublishBorderPurple = Color(0xFF6321A8)
private val PublishSubtleBorder = Color(0xFF282634)

/**
 * Full-Screen Professional Video Publish Screen for FOUL TV Reels.
 * Designed to match the reference design:
 * - Deep `#050505` background with subtle corner purple wave ribbons
 * - Top bar with ONLY a single circular back arrow (no FOUL TV text, no logo, no extra titles)
 * - Dashed rounded Video Picker card with Clapperboard + Play icon & purple `+` badge
 * - Programmatic 30-second video duration validation via `MediaMetadataRetriever`
 * - Video Description card (`#101014`) with purple pencil icon, 500-char limit & `0/500` counter
 * - 30-second maximum duration notice pill with clock icon
 * - Live AI Content Moderation progress banner
 * - Full-width glowing purple `"نشر الفيديو"` button with paper-plane icon
 */
@Composable
fun ReelsPublishFullScreen(
    isUploading: Boolean,
    moderationStage: ReelModerationStage,
    externalStatusMessage: String?,
    onPublishVideo: (videoUri: Uri, caption: String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(enabled = !isUploading) {
        onClose()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoDurationMs by remember { mutableLongStateOf(0L) }
    var selectedVideoFileName by remember { mutableStateOf("") }
    var selectedVideoPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isInspectingLocalMetadata by remember { mutableStateOf(false) }

    var captionText by remember { mutableStateOf("") }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isInspectingLocalMetadata = true
            localValidationError = null
            coroutineScope.launch {
                val metaResult = ReelAiModerationEngine.inspectSelectedVideoMetadata(context, uri)
                isInspectingLocalMetadata = false
                metaResult.onSuccess { meta ->
                    if (meta.durationMs <= 0L || meta.durationMs > ReelAiModerationEngine.MAX_REEL_DURATION_MS) {
                        selectedVideoUri = null
                        selectedVideoDurationMs = 0L
                        selectedVideoFileName = ""
                        selectedVideoPreviewBitmap = null
                        localValidationError = ReelAiModerationEngine.DURATION_EXCEEDED_MESSAGE_AR
                    } else {
                        selectedVideoUri = uri
                        selectedVideoDurationMs = meta.durationMs
                        selectedVideoFileName = meta.fileName
                        selectedVideoPreviewBitmap = meta.previewBitmap
                        localValidationError = null
                    }
                }.onFailure { err ->
                    selectedVideoUri = null
                    selectedVideoDurationMs = 0L
                    selectedVideoFileName = ""
                    selectedVideoPreviewBitmap = null
                    localValidationError = err.message ?: "تعذر قراءة بيانات الفيديو المختار."
                }
            }
        }
    }

    val isVideoValid = selectedVideoUri != null &&
        selectedVideoDurationMs in 1L..ReelAiModerationEngine.MAX_REEL_DURATION_MS

    val activeErrorOrStatus = localValidationError ?: externalStatusMessage

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PublishBgDeepBlack)
            .drawBehind {
                val w = size.width
                val h = size.height

                // Top-Right decorative purple wave glow (matching reference image)
                val topWave = Path().apply {
                    moveTo(w * 0.56f, 0f)
                    cubicTo(w * 0.72f, h * 0.05f, w * 0.88f, h * 0.06f, w, h * 0.12f)
                    lineTo(w, 0f)
                    close()
                }
                drawPath(
                    path = topWave,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            PublishBorderPurple.copy(alpha = 0.28f),
                            PublishAccentPurple.copy(alpha = 0.45f)
                        ),
                        start = Offset(w * 0.6f, 0f),
                        end = Offset(w, h * 0.12f)
                    )
                )
                drawPath(
                    path = topWave,
                    color = PublishAccentPurple.copy(alpha = 0.55f),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Bottom-Left decorative purple wave ribbon (matching reference image)
                val bottomWave = Path().apply {
                    moveTo(0f, h * 0.89f)
                    cubicTo(w * 0.18f, h * 0.92f, w * 0.34f, h * 0.96f, w * 0.56f, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = bottomWave,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            PublishAccentPurple.copy(alpha = 0.50f),
                            PublishBorderPurple.copy(alpha = 0.22f)
                        ),
                        start = Offset(0f, h * 0.90f),
                        end = Offset(w * 0.56f, h)
                    )
                )
                drawPath(
                    path = bottomWave,
                    color = PublishAccentPurple.copy(alpha = 0.65f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("reels_publish_fullscreen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // ================================================================
                // 1. TOP BAR: ONLY A SINGLE CIRCULAR BACK ARROW ON THE LEFT
                //    (No FOUL TV text, no logo, no extra titles)
                // ================================================================
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PublishCardBg)
                                .border(1.dp, PublishSubtleBorder, CircleShape)
                                .clickable(enabled = !isUploading) { onClose() }
                                .testTag("publish_reel_back_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = PublishTextPrimary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }

                // ================================================================
                // 2. VIDEO SELECTION CARD (Dashed Rounded Box / Selected Preview)
                // ================================================================
                val pickerShape = RoundedCornerShape(24.dp)
                val dashColor = if (isVideoValid) PublishAccentPurple else Color(0xFF3E3A50)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(232.dp)
                        .clip(pickerShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF14111D),
                                    PublishCardBg,
                                    Color(0xFF0D0C12)
                                )
                            )
                        )
                        .drawBehind {
                            val strokeWidth = 1.35.dp.toPx()
                            val cornerRadius = 24.dp.toPx()
                            drawRoundRect(
                                color = dashColor,
                                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                style = Stroke(
                                    width = strokeWidth,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                                )
                            )
                        }
                        .clickable(enabled = !isUploading && !isInspectingLocalMetadata) {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                        .testTag("publish_select_video_box"),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isInspectingLocalMetadata -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = PublishAccentPurple,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "جارٍ التحقق من مدة وصيغة الفيديو...",
                                    color = PublishTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        selectedVideoUri != null && isVideoValid -> {
                            // Selected Valid Video Preview Card
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (selectedVideoPreviewBitmap != null) {
                                    Image(
                                        bitmap = selectedVideoPreviewBitmap!!.asImageBitmap(),
                                        contentDescription = "معاينة الفيديو",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(pickerShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.52f))
                                    )
                                }

                                // Top Row: Duration Pill + Remove/Change Buttons
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color(0xCC101014),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, PublishAccentPurple)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            val secs = ((selectedVideoDurationMs + 500L) / 1000L).toInt().coerceAtLeast(1)
                                            Text(
                                                text = String.format(Locale.US, "00:%02d / 00:30", secs),
                                                color = PublishTextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            onClick = {
                                                if (!isUploading) {
                                                    videoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                                    )
                                                }
                                            },
                                            color = Color(0xCC101014),
                                            shape = RoundedCornerShape(50),
                                            border = BorderStroke(1.dp, PublishSubtleBorder)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "تغيير الفيديو",
                                                    tint = PublishTextPrimary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "تغيير",
                                                    color = PublishTextPrimary,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xCCEF4444))
                                                .clickable(enabled = !isUploading) {
                                                    selectedVideoUri = null
                                                    selectedVideoDurationMs = 0L
                                                    selectedVideoFileName = ""
                                                    selectedVideoPreviewBitmap = null
                                                }
                                                .testTag("publish_remove_selected_video_button"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف الفيديو المختار",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                // Center Play Emblem
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(PublishAccentPurple.copy(alpha = 0.88f))
                                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                // Bottom File Name & Ready Status
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = selectedVideoFileName.ifBlank { "فيديو جاهز للنشر" },
                                        color = PublishTextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "مدة الفيديو مطابقة للحد المسموح (أقل من 30 ثانية)",
                                        color = Color(0xFF10B981),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        else -> {
                            // Default Empty State (Matches Reference Image 100%)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            ) {
                                ClapperboardVideoPlusGraphic()

                                Spacer(modifier = Modifier.height(18.dp))

                                Text(
                                    text = "اختر فيديو من جهازك",
                                    color = PublishTextPrimary,
                                    fontSize = 20.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "أو اضغط هنا لاختيار الفيديو",
                                    color = PublishTextSecondary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // ================================================================
                // 3. VALIDATION ERROR OR AI MODERATION STATUS BANNER
                // ================================================================
                AnimatedVisibility(visible = !activeErrorOrStatus.isNullOrBlank()) {
                    val msg = activeErrorOrStatus.orEmpty()
                    val isPendingReview = msg.contains("قيد المراجعة")
                    val bannerColor = if (isPendingReview) Color(0xFFF59E0B) else Color(0xFFEF4444)

                    Surface(
                        color = bannerColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publish_validation_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = bannerColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = msg,
                                color = PublishTextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق التنبيه",
                                tint = PublishTextSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { localValidationError = null }
                            )
                        }
                    }
                }

                // Live AI Content Moderation Progress Stage Card
                AnimatedVisibility(visible = isUploading && moderationStage != ReelModerationStage.IDLE) {
                    Surface(
                        color = PublishCardBg,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, PublishAccentPurple.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publish_ai_moderation_stage_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = PublishAccentPurple,
                                strokeWidth = 2.2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = PublishAccentPurple,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "نظام الفحص الذكي لـ FOUL TV",
                                        color = PublishAccentPurple,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Text(
                                    text = moderationStage.labelAr.ifBlank { "جارٍ فحص الفيديو..." },
                                    color = PublishTextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // ================================================================
                // 4. VIDEO DESCRIPTION SECTION (قسم وصف الفيديو)
                // ================================================================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Right-aligned header: Purple Pencil Icon + "وصف الفيديو"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = PublishAccentPurple,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = "وصف الفيديو",
                            color = PublishTextPrimary,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Description Input Box (#101014) with 0/500 counter in bottom-left
                    val descShape = RoundedCornerShape(18.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(142.dp)
                            .clip(descShape)
                            .background(PublishCardBg)
                            .border(
                                width = 1.dp,
                                color = if (captionText.isNotEmpty()) PublishBorderPurple else PublishSubtleBorder,
                                shape = descShape
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        BasicTextField(
                            value = captionText,
                            onValueChange = { newText ->
                                if (newText.length <= 500) {
                                    captionText = newText
                                }
                            },
                            enabled = !isUploading,
                            textStyle = TextStyle(
                                color = PublishTextPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Right,
                                textDirection = TextDirection.Rtl,
                                lineHeight = 21.sp
                            ),
                            cursorBrush = SolidColor(PublishAccentPurple),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 24.dp)
                                .testTag("publish_reel_caption_input"),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (captionText.isEmpty()) {
                                        Text(
                                            text = "اكتب وصفاً مختصراً للفيديو...",
                                            color = PublishTextSecondary,
                                            fontSize = 14.5.sp,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // Character counter in Bottom-Left ("0/500")
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = "${captionText.length}/500",
                                color = PublishTextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .testTag("publish_caption_counter")
                            )
                        }
                    }
                }

                // ================================================================
                // 5. 30-SECOND DURATION NOTICE CARD (ملاحظة مدة الفيديو)
                // ================================================================
                val noticeShape = RoundedCornerShape(16.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(noticeShape)
                        .background(PublishCardBg)
                        .border(1.dp, PublishSubtleBorder, noticeShape)
                        .padding(horizontal = 16.dp)
                        .testTag("publish_duration_notice_card"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Right side (in RTL): Clock Icon
                        ClockNoticeIcon(modifier = Modifier.size(24.dp))

                        // Notice Text with "30 ثانية" highlighted in #862BEB
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = PublishTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append("الحد الأقصى لمدة الفيديو هو ")
                                }
                                withStyle(
                                    SpanStyle(
                                        color = PublishAccentPurple,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                ) {
                                    append("30 ثانية")
                                }
                                withStyle(
                                    SpanStyle(
                                        color = PublishTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(" فقط")
                                }
                            },
                            fontSize = 14.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ====================================================================
            // 6. FULL-WIDTH PUBLISH BUTTON ("نشر الفيديو")
            // ====================================================================
            val buttonShape = RoundedCornerShape(28.dp)
            val canClickPublish = isVideoValid && !isUploading && !isInspectingLocalMetadata

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(58.dp)
                    .clip(buttonShape)
                    .background(
                        brush = if (canClickPublish || isUploading) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF4B1882),
                                    PublishAccentPurple,
                                    Color(0xFF9B4DFF),
                                    PublishAccentPurple,
                                    Color(0xFF4B1882)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF28183C),
                                    Color(0xFF3A1F5D),
                                    Color(0xFF28183C)
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.2.dp,
                        color = if (canClickPublish) Color(0xFFB76EFF) else PublishBorderPurple.copy(alpha = 0.5f),
                        shape = buttonShape
                    )
                    .clickable(enabled = canClickPublish) {
                        val uri = selectedVideoUri ?: return@clickable
                        if (selectedVideoDurationMs <= 0L || selectedVideoDurationMs > ReelAiModerationEngine.MAX_REEL_DURATION_MS) {
                            localValidationError = ReelAiModerationEngine.DURATION_EXCEEDED_MESSAGE_AR
                            return@clickable
                        }
                        localValidationError = null
                        onPublishVideo(uri, captionText.trim())
                    }
                    .testTag("confirm_publish_reel_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.4.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "جارٍ نشر الفيديو...",
                            color = PublishTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = if (canClickPublish) PublishTextPrimary else PublishTextPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "نشر الفيديو",
                            color = if (canClickPublish) PublishTextPrimary else PublishTextPrimary.copy(alpha = 0.6f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom Clapperboard + Play Icon with a glowing Purple `+` badge at the bottom-right corner,
 * matching the reference image.
 */
@Composable
private fun ClapperboardVideoPlusGraphic() {
    Box(
        modifier = Modifier.size(92.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(80.dp)) {
            val w = size.width
            val h = size.height

            // Lower Clapperboard body
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2B2838), Color(0xFF171520))
                ),
                topLeft = Offset(w * 0.08f, h * 0.34f),
                size = Size(w * 0.84f, h * 0.56f),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF49445E),
                topLeft = Offset(w * 0.08f, h * 0.34f),
                size = Size(w * 0.84f, h * 0.56f),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 1.4.dp.toPx())
            )

            // Tilted Top Clapper Slate Bar
            val slatePath = Path().apply {
                moveTo(w * 0.08f, h * 0.30f)
                lineTo(w * 0.88f, h * 0.14f)
                lineTo(w * 0.92f, h * 0.27f)
                lineTo(w * 0.10f, h * 0.36f)
                close()
            }
            drawPath(
                path = slatePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF9E9AB0), Color(0xFF5D5872))
                )
            )

            // Diagonal stripes on clapper bar
            val stripeOffsets = listOf(0.24f, 0.46f, 0.68f)
            stripeOffsets.forEach { frac ->
                drawLine(
                    color = Color(0xFF1B1924),
                    start = Offset(w * frac, h * (0.29f - frac * 0.14f)),
                    end = Offset(w * (frac + 0.06f), h * (0.35f - frac * 0.12f)),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Center White Play Triangle
            val playPath = Path().apply {
                moveTo(w * 0.42f, h * 0.49f)
                lineTo(w * 0.62f, h * 0.62f)
                lineTo(w * 0.42f, h * 0.75f)
                close()
            }
            drawPath(path = playPath, color = Color.White)
        }

        // Purple circular '+' badge at bottom-right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-2).dp, y = (-2).dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(PublishAccentPurple)
                .border(2.dp, PublishBgDeepBlack, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Crisp white clock vector icon for the 30-second notice card.
 */
@Composable
private fun ClockNoticeIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f - 2.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = Color.White,
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
        // Hour & minute hands
        drawLine(
            color = Color.White,
            start = center,
            end = Offset(center.x, center.y - radius * 0.55f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = center,
            end = Offset(center.x + radius * 0.42f, center.y + radius * 0.22f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
