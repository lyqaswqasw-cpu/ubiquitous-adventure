package com.example.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import com.example.data.model.ReelModerationStage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.ReelCommentItem
import com.example.data.model.ReelItem
import com.example.data.model.SocialUserProfile
import com.example.ui.components.copyTextToClipboard
import com.example.ui.theme.FoulBlackBg
import com.example.ui.theme.FoulCardDark
import com.example.ui.theme.FoulGoldStar
import com.example.ui.theme.FoulLiveGreen
import com.example.ui.theme.FoulLiveRed
import com.example.ui.theme.FoulPurpleBright
import com.example.ui.theme.FoulPurplePrimary
import com.example.ui.theme.FoulTextSecondary

private val FoulGoldAccent = FoulGoldStar
private val FoulPurpleSoft = Color(0xFFB388FF)

@Composable
fun ReelsScreen(
    reels: List<ReelItem>,
    currentUserId: String = "",
    followingUserIds: Set<String> = emptySet(),
    isLoading: Boolean,
    isUploading: Boolean,
    moderationStage: ReelModerationStage = ReelModerationStage.IDLE,
    uploadStatusMessage: String?,
    selectedReelIdFromDeepLink: String?,
    officialChannelUrl: String,
    commentsForActiveReel: List<ReelCommentItem>,
    isLoadingComments: Boolean,
    onRefreshReels: () -> Unit,
    onUploadReelVideo: (Uri, String, String) -> Unit,
    onToggleLike: (ReelItem) -> Unit,
    onRecordView: (ReelItem) -> Unit,
    onRecordShare: (ReelItem) -> Unit,
    onOpenComments: (ReelItem) -> Unit,
    onAddComment: (ReelItem, String) -> Unit,
    onDeleteReel: (ReelItem) -> Unit = {},
    onToggleFollowUser: (String) -> Unit = {},
    onConsumeDeepLinkReelId: () -> Unit
) {
    val context = LocalContext.current

    var isFullScreenPublishOpen by remember { mutableStateOf(false) }
    var activeCommentsReel by remember { mutableStateOf<ReelItem?>(null) }
    var activeOptionsReel by remember { mutableStateOf<ReelItem?>(null) }
    var showPublishAccountRequiredDialog by remember { mutableStateOf(false) }
    var isAnyEdgeFastForwarding by remember { mutableStateOf(false) }

    // Automatically close full-screen publish page when upload succeeds
    LaunchedEffect(isUploading, uploadStatusMessage) {
        if (!isUploading && isFullScreenPublishOpen && uploadStatusMessage != null) {
            if (uploadStatusMessage.contains("تم رفع ونشر") || uploadStatusMessage.contains("بنجاح")) {
                isFullScreenPublishOpen = false
            }
        }
    }

    val triggerPublishReel: () -> Unit = {
        if (!com.example.data.repository.SocialFirebaseRepository.isUserAuthenticatedWithCompleteProfile()) {
            showPublishAccountRequiredDialog = true
        } else {
            isFullScreenPublishOpen = true
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { reels.size.coerceAtLeast(1) }
    )

    // Scroll to deep-linked Reel if present
    LaunchedEffect(selectedReelIdFromDeepLink, reels.size) {
        val targetId = selectedReelIdFromDeepLink?.trim().orEmpty()
        if (targetId.isNotEmpty() && reels.isNotEmpty()) {
            val idx = reels.indexOfFirst {
                it.id.equals(targetId, ignoreCase = true) ||
                    "${it.chatId}_${it.messageId}".equals(targetId, ignoreCase = true)
            }
            if (idx >= 0) {
                pagerState.scrollToPage(idx)
                onConsumeDeepLinkReelId()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FoulBlackBg)
            .testTag("reels_screen_root")
    ) {
        if (isLoading && reels.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = FoulPurplePrimary)
                Text(
                    text = "جاري تحميل مقاطع FOUL REELS...",
                    color = FoulTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else if (reels.isEmpty()) {
            EmptyReelsState(
                officialChannelUrl = officialChannelUrl,
                onPublishClick = triggerPublishReel,
                onRefreshClick = onRefreshReels
            )
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reels_vertical_pager"),
                beyondViewportPageCount = 1,
                key = { page -> reels.getOrNull(page)?.id ?: "empty_$page" }
            ) { page ->
                val reel = reels.getOrNull(page)
                if (reel != null) {
                    val isCurrentPage = pagerState.settledPage == page
                    ReelPagePlayerItem(
                        reel = reel,
                        currentUserId = currentUserId,
                        isFollowingCreator = followingUserIds.contains(reel.userId),
                        isCurrentPage = isCurrentPage,
                        onLikeClick = { onToggleLike(reel) },
                        onCommentClick = {
                            activeCommentsReel = reel
                            onOpenComments(reel)
                        },
                        onViewRecorded = { onRecordView(reel) },
                        onOpenLongPressOptions = {
                            activeOptionsReel = reel
                        },
                        onToggleFollowCreator = {
                            if (reel.userId.isNotBlank()) {
                                onToggleFollowUser(reel.userId)
                            }
                        },
                        onEdgeFastForwardChanged = { active ->
                            isAnyEdgeFastForwarding = active
                        }
                    )
                }
            }
        }

        // Top Overlay Bar (hidden while pressing screen edges for 2x speed)
        AnimatedVisibility(
            visible = !isAnyEdgeFastForwarding,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Pure '+' Publish Icon Button in high-contrast White & Black ("بشكل فقط زائد بلون أسود أو أبيض حسب البارز")
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, Color.Black.copy(alpha = 0.35f), CircleShape)
                        .clickable(onClick = triggerPublishReel)
                        .testTag("publish_reel_top_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "نشر ريلز",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onRefreshReels,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.62f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                        .testTag("reels_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تحديث الريلز",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // Upload progress banner if uploading
        AnimatedVisibility(
            visible = isUploading || !uploadStatusMessage.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 62.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1A142D).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, FoulPurpleBright),
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = FoulPurpleSoft,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = uploadStatusMessage ?: "جاري رفع ونشر الريلز على FOUL TV...",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Full-Screen Professional Video Publish Screen ("فتح صفحة بملء كامل واحترافية جداً ومنظمة")
    if (isFullScreenPublishOpen) {
        ReelsPublishFullScreen(
            isUploading = isUploading,
            moderationStage = moderationStage,
            externalStatusMessage = uploadStatusMessage,
            onPublishVideo = { videoUri, caption ->
                onUploadReelVideo(videoUri, caption, caption)
            },
            onClose = {
                if (!isUploading) {
                    isFullScreenPublishOpen = false
                }
            }
        )
    }

    // Comments Dialog
    if (activeCommentsReel != null) {
        ReelCommentsDialog(
            reel = activeCommentsReel!!,
            comments = commentsForActiveReel,
            isLoading = isLoadingComments,
            onDismiss = { activeCommentsReel = null },
            onSendComment = { commentText ->
                onAddComment(activeCommentsReel!!, commentText)
            }
        )
    }

    // Long-Press Options Dialog:
    // 1. "نسخ رابط الفيديو" (Smart Share Link + Caption + Official Telegram Channel URL ONLY)
    // 2. "حذف الريلز" (Visible ONLY to the owner who published the Reel)
    if (activeOptionsReel != null) {
        val targetReel = activeOptionsReel!!
        val isOwner = currentUserId.isNotBlank() && targetReel.userId == currentUserId
        ReelLongPressOptionsDialog(
            reel = targetReel,
            isOwner = isOwner,
            officialChannelUrl = officialChannelUrl,
            onDismiss = { activeOptionsReel = null },
            onCopySmartVideoLink = {
                onRecordShare(targetReel)
                val telegramUrl = targetReel.channelUrl.ifBlank { officialChannelUrl }
                val smartCopyText = buildString {
                    append("🎬 شاهد هذا الفيديو على تطبيق FOUL TV:\n")
                    if (targetReel.caption.isNotBlank()) {
                        append("${targetReel.caption}\n\n")
                    }
                    append("🔗 رابط الفيديو الذكي:\n")
                    append("${targetReel.shareUrl}\n\n")
                    append("📢 قناتنا الرسمية على تيلجرام:\n")
                    append(telegramUrl)
                }
                copyTextToClipboard(
                    context = context,
                    label = "FOUL Smart Video Link",
                    text = smartCopyText,
                    toastMessage = "تم نسخ رابط الفيديو الذكي مع رابط قناة التيلجرام!"
                )
                activeOptionsReel = null
            },
            onDeleteReel = {
                activeOptionsReel = null
                onDeleteReel(targetReel)
            }
        )
    }

    // Account Required Dialog when a Guest/Skipped user tries to Publish a Reel
    if (showPublishAccountRequiredDialog) {
        Dialog(onDismissRequest = { showPublishAccountRequiredDialog = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF141122),
                border = BorderStroke(1.2.dp, FoulGoldAccent.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "نشر الريلز يتطلب إنشاء حساب",
                        color = FoulGoldAccent,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "لقد اخترت تخطي إنشاء الحساب عند الدخول. لا يمكنك نشر مقاطع ريلز أو نسخ أي رابط فيديو أو قنوات إلا بعد إكمال المطلوب وإنشاء حساب أو تسجيل الدخول.",
                        color = FoulTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            showPublishAccountRequiredDialog = false
                            com.example.data.repository.SocialFirebaseRepository.requestOpenFullAuthScreen()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FoulPurplePrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("reels_publish_locked_create_account_button")
                    ) {
                        Text(
                            text = "إنشاء حساب / تسجيل الدخول الآن",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                    Surface(
                        onClick = { showPublishAccountRequiredDialog = false },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "إغلاق",
                                color = FoulTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ReelPagePlayerItem(
    reel: ReelItem,
    currentUserId: String,
    isFollowingCreator: Boolean,
    isCurrentPage: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onViewRecorded: () -> Unit,
    onOpenLongPressOptions: () -> Unit,
    onToggleFollowCreator: () -> Unit,
    onEdgeFastForwardChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPausedManually by remember(reel.id) { mutableStateOf(false) }
    var isBuffering by remember(reel.id) { mutableStateOf(true) }
    var hasRecordedView by remember(reel.id) { mutableStateOf(false) }
    var usedFallbackUrl by remember(reel.id) { mutableStateOf(false) }
    var isFastForward2x by remember(reel.id) { mutableStateOf(false) }

    val exoPlayer = remember(reel.id) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("FOUL-TV-ExoPlayer/1.2.2")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1_500,
                15_000,
                500,
                1_000
            )
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .setLoadControl(loadControl)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                val initialUrl = reel.streamUrl.ifBlank { reel.videoUrl }
                if (initialUrl.isNotBlank()) {
                    setMediaItem(MediaItem.fromUri(initialUrl))
                    prepare()
                }
            }
    }

    LaunchedEffect(isFastForward2x) {
        exoPlayer.setPlaybackSpeed(if (isFastForward2x) 2.0f else 1.0f)
        onEdgeFastForwardChanged(isFastForward2x)
    }

    DisposableEffect(exoPlayer, reel.id) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY && isCurrentPage && !hasRecordedView) {
                    hasRecordedView = true
                    onViewRecorded()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                if (!usedFallbackUrl && reel.videoUrl.isNotBlank() && reel.videoUrl != reel.streamUrl) {
                    usedFallbackUrl = true
                    exoPlayer.setMediaItem(MediaItem.fromUri(reel.videoUrl))
                    exoPlayer.prepare()
                    if (isCurrentPage && !isPausedManually) {
                        exoPlayer.playWhenReady = true
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            onEdgeFastForwardChanged(false)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isCurrentPage, isPausedManually) {
        if (isCurrentPage && !isPausedManually) {
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        } else {
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer, isCurrentPage) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (isCurrentPage && !isPausedManually) {
                        exoPlayer.play()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(reel.id) {
                detectTapGestures(
                    onPress = { offset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val isEdgeTouch = offset.x < w * 0.24f || offset.x > w * 0.76f
                        if (isEdgeTouch) {
                            // Pressing on the left/right edges speeds up video 2x and hides overlay buttons
                            isFastForward2x = true
                            try {
                                tryAwaitRelease()
                            } finally {
                                isFastForward2x = false
                            }
                        }
                    },
                    onLongPress = { offset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val isCenterTouch = offset.x in (w * 0.24f)..(w * 0.76f)
                        if (isCenterTouch) {
                            onOpenLongPressOptions()
                        }
                    },
                    onTap = { offset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val isCenterTouch = offset.x in (w * 0.24f)..(w * 0.76f)
                        if (isCenterTouch) {
                            isPausedManually = !isPausedManually
                        }
                    }
                )
            }
    ) {
        // Google Media3 ExoPlayer Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = exoPlayer
                }
            },
            update = { view ->
                if (view.player !== exoPlayer) {
                    view.player = exoPlayer
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("reel_exoplayer_view_${reel.id}")
        )

        // Subtle 2x Fast-Forward Pill Indicator when holding screen edges
        AnimatedVisibility(
            visible = isFastForward2x,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 18.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.68f),
                border = BorderStroke(1.dp, FoulPurpleBright.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        tint = FoulGoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "تسريع الفيديو 2x",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Center Play icon indicator when paused manually
        AnimatedVisibility(
            visible = isPausedManually && !isFastForward2x,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "تشغيل",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        if (isBuffering && isCurrentPage) {
            CircularProgressIndicator(
                color = FoulPurpleBright,
                strokeWidth = 2.5.dp,
                modifier = Modifier
                    .size(38.dp)
                    .align(Alignment.Center)
            )
        }

        // Overlay UI (Hidden automatically when pressing screen edges for 2x speed)
        AnimatedVisibility(
            visible = !isFastForward2x,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Bottom gradient scrim for readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.50f),
                                    Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                )

                // Right-Side Interactive Action Rail: ONLY Like + Comment (with numeric counter, NO views counter!)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // 1. Like Button with Counter
                    ReelActionCircleButton(
                        icon = if (reel.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        iconTint = if (reel.isLikedByMe) Color(0xFFFF1744) else Color.White,
                        label = reel.likesCount.toString(),
                        testTag = "reel_like_button_${reel.id}",
                        onClick = onLikeClick
                    )

                    // 2. Comment Button with Numeric Comments Counter (instead of static word "تعليق")
                    ReelActionCircleButton(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        iconTint = Color.White,
                        label = reel.commentsCount.toString(),
                        testTag = "reel_comment_button_${reel.id}",
                        onClick = onCommentClick
                    )

                    // 3. Share Icon & "مشاركة" Label ("جعل خيارت يكون بشكل أيقونة مشاركة ونص مشاركة")
                    ReelActionCircleButton(
                        icon = Icons.Default.Share,
                        iconTint = Color.White,
                        label = "مشاركة",
                        testTag = "reel_options_button_${reel.id}",
                        onClick = onOpenLongPressOptions
                    )
                }

                // Bottom-Start Creator Info (Profile Photo + Personal Name + @username in small font) & Caption
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.78f)
                        .padding(start = 16.dp, end = 12.dp, bottom = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Creator Profile Photo
                        Surface(
                            shape = CircleShape,
                            color = FoulPurplePrimary,
                            border = BorderStroke(1.5.dp, FoulPurpleBright),
                            modifier = Modifier.size(40.dp)
                        ) {
                            AsyncImage(
                                model = reel.userPhotoUrl.ifBlank { SocialUserProfile.DEFAULT_PROFILE_PHOTO_URL },
                                contentDescription = reel.userPersonalName.ifBlank { reel.userName },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            // Personal Name on top
                            Text(
                                text = reel.userPersonalName.ifBlank { reel.userName.ifBlank { "مستخدم FOUL TV" } },
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // @username underneath in small font
                            Text(
                                text = if (reel.userHandle.startsWith("@")) reel.userHandle else "@${reel.userHandle.ifBlank { "foultv" }}",
                                color = FoulPurpleSoft,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Follow / Following Pill if not current user
                        if (reel.userId.isNotBlank() && reel.userId != currentUserId && reel.userId != "foul_user") {
                            Surface(
                                onClick = onToggleFollowCreator,
                                shape = RoundedCornerShape(999.dp),
                                color = if (isFollowingCreator) {
                                    Color.White.copy(alpha = 0.14f)
                                } else {
                                    FoulPurplePrimary
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isFollowingCreator) Color.White.copy(alpha = 0.35f) else FoulPurpleBright
                                ),
                                modifier = Modifier.testTag("reel_follow_creator_button_${reel.id}")
                            ) {
                                Text(
                                    text = if (isFollowingCreator) "تتابعه" else "متابعة",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (reel.caption.isNotBlank()) {
                        Text(
                            text = reel.caption,
                            color = Color.White,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Long-Press Options Dialog for a Reel:
 * - "نسخ رابط الفيديو": Copies ONLY the Smart Share Link formatted with caption and Official Telegram Channel URL.
 * - "حذف الريلز": Displayed strictly ONLY to the publisher who owns the Reel (`isOwner == true`).
 */
@Composable
private fun ReelLongPressOptionsDialog(
    reel: ReelItem,
    isOwner: Boolean,
    officialChannelUrl: String,
    onDismiss: () -> Unit,
    onCopySmartVideoLink: () -> Unit,
    onDeleteReel: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF141122),
            border = BorderStroke(1.2.dp, FoulPurplePrimary.copy(alpha = 0.65f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reel_long_press_options_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "خيارات المقطع",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = FoulTextSecondary
                        )
                    }
                }

                // Option 1: نسخ رابط الفيديو (Smart Share Link + Telegram Channel URL ONLY)
                Surface(
                    onClick = onCopySmartVideoLink,
                    shape = RoundedCornerShape(16.dp),
                    color = FoulPurplePrimary.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, FoulPurpleBright.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reel_option_copy_smart_video_link")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = FoulGoldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "نسخ رابط الفيديو",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp
                            )
                            Text(
                                text = "نسخ الرابط الذكي للمقطع مع رابط قناة التيلجرام الرسمية (${officialChannelUrl.removePrefix("https://")})",
                                color = FoulTextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                // Option 2: حذف الريلز (Visible ONLY if the current user published this Reel)
                if (isOwner) {
                    Surface(
                        onClick = onDeleteReel,
                        shape = RoundedCornerShape(16.dp),
                        color = FoulLiveRed.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, FoulLiveRed.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reel_option_delete_own_reel")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF6E6E),
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "حذف الريلز",
                                    color = Color(0xFFFF8A80),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.5.sp
                                )
                                Text(
                                    text = "حذف هذا المقطع الذي قمت بنشره نهائياً",
                                    color = FoulTextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReelActionCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.52f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(23.dp)
            )
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun EmptyReelsState(
    officialChannelUrl: String,
    onPublishClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = FoulPurplePrimary.copy(alpha = 0.18f),
            border = BorderStroke(1.5.dp, FoulPurplePrimary.copy(alpha = 0.55f)),
            modifier = Modifier.size(84.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = FoulPurpleSoft,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "مرحباً بك في قسم FOUL REELS",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "متصل بقاعدة بيانات FOUL الجديدة ومشغل Google Media3 ExoPlayer. يمكنك نشر أول مقطع ريلز من هاتفك الآن أو تحديث القائمة.",
            color = FoulTextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(22.dp))
        Button(
            onClick = onPublishClick,
            colors = ButtonDefaults.buttonColors(containerColor = FoulPurplePrimary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("empty_reels_publish_button")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "نشر مقطع ريلز من الهاتف",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onRefreshClick,
                shape = RoundedCornerShape(14.dp),
                color = FoulCardDark,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "تحديث المقاطع",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
            Surface(
                onClick = {
                    copyTextToClipboard(
                        context = context,
                        label = "FOUL Official Channel",
                        text = officialChannelUrl,
                        toastMessage = "تم نسخ رابط القناة الرسمية!"
                    )
                },
                shape = RoundedCornerShape(14.dp),
                color = FoulCardDark,
                border = BorderStroke(1.dp, FoulGoldAccent.copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "نسخ رابط القناة",
                        color = FoulGoldAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PublishReelModalDialog(
    videoUri: Uri,
    onDismiss: () -> Unit,
    onConfirmPublish: (caption: String, overlayText: String) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var overlayText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF141122),
            border = BorderStroke(1.dp, FoulPurplePrimary.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "نشر مقطع ريلز جديد",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = FoulTextSecondary
                        )
                    }
                }

                Text(
                    text = "تم اختيار الفيديو بنجاح (${videoUri.lastPathSegment ?: "video.mp4"}). سيتم رفعه ومزامنته مباشرة مع قاعدة بيانات FOUL وتوليد روابط النسخ والمشاركة الذكية.",
                    color = FoulTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("وصف المقطع (Caption)") },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FoulPurplePrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = FoulPurpleSoft,
                        unfocusedLabelColor = FoulTextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_reel_caption_input")
                )

                OutlinedTextField(
                    value = overlayText,
                    onValueChange = { overlayText = it },
                    label = { Text("نص إضافي على الفيديو (اختياري)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FoulPurplePrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = FoulPurpleSoft,
                        unfocusedLabelColor = FoulTextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_reel_overlay_input")
                )

                Button(
                    onClick = { onConfirmPublish(caption.trim(), overlayText.trim()) },
                    colors = ButtonDefaults.buttonColors(containerColor = FoulPurplePrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_publish_reel_button")
                ) {
                    Text(
                        text = "رفع ونشر الآن",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ReelCommentsDialog(
    reel: ReelItem,
    comments: List<ReelCommentItem>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF141122),
            border = BorderStroke(1.dp, FoulPurplePrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التعليقات (${comments.size.coerceAtLeast(reel.commentsCount)})",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = FoulTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isLoading && comments.isEmpty()) {
                        CircularProgressIndicator(
                            color = FoulPurplePrimary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else if (comments.isEmpty()) {
                        Text(
                            text = "لا توجد تعليقات بعد. كن أول من يعلق على هذا المقطع!",
                            color = FoulTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(comments, key = { it.id }) { comment ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.05f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = comment.userName,
                                            color = FoulPurpleSoft,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = comment.text,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("أضف تعليقاً...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FoulPurplePrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reel_comment_input")
                    )
                    IconButton(
                        onClick = {
                            val trimmed = inputText.trim()
                            if (trimmed.isNotEmpty()) {
                                onSendComment(trimmed)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(FoulPurplePrimary)
                            .testTag("reel_comment_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال التعليق",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
