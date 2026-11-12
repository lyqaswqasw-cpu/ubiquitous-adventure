package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AppVersionConfig
import com.example.data.model.ReelItem
import com.example.data.model.SocialUserProfile
import com.example.ui.components.FoulSectionSubBar
import com.example.ui.components.FoulSmartTopBar
import com.example.ui.components.OrderedColorfulDivider
import com.example.ui.components.tvFocusableClickable
import com.example.ui.player.VodPlayerPreferencesManager
import com.example.ui.theme.FoulBlackBg
import com.example.ui.theme.FoulCardDark
import com.example.ui.theme.FoulCardElevated
import com.example.ui.theme.FoulDivider
import com.example.ui.theme.FoulGoldStar
import com.example.ui.theme.FoulLiveGreen
import com.example.ui.theme.FoulLiveRed
import com.example.ui.theme.FoulPurpleAccent
import com.example.ui.theme.FoulPurpleBright
import com.example.ui.theme.FoulPurpleDeep
import com.example.ui.theme.FoulPurpleGlow
import com.example.ui.theme.FoulPurplePrimary
import com.example.ui.theme.FoulTeamAwayCyan
import com.example.ui.theme.FoulTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class SettingsSubPage {
    NONE,
    PRIVACY_POLICY,
    APP_DESCRIPTION
}

/**
 * Custom Bespoke Broadcast Icon Types for Settings Cards
 */
private enum class SettingsProIconType {
    SUPPORT_HEADSET,
    TELEGRAM_PLANE,
    SEEK_CONTROL,
    APP_ARCHITECTURE,
    PRIVACY_SHIELD,
    DEVELOPER_CONSOLE
}

/**
 * Master visibility toggle for Developer Control Panel entry points across Settings,
 * Mandatory Update Screen, and Top Bar icons.
 * Enabled (`true`) as requested by the user ("إظهر لوحة تحكم المطور في الإعدادات").
 */
const val SHOW_DEVELOPER_PANEL_ENTRY = true

/**
 * In-memory cache of extracted video cover frames (`غلاف الفيديو`) keyed by Reel ID.
 */
private val ReelVideoCoverBitmapCache = mutableStateMapOf<String, Bitmap>()

/**
 * Instagram-style Profile Screen (`الملف الشخصي`) with:
 * - Top bar featuring `@username` and the 3-lines Hamburger Menu button (`☰`) that opens Settings.
 * - Profile Header with circular Profile Photo (+ device picker button), Posts count, Followers count, Following count.
 * - Personal Name + `@username` below the photo.
 * - Horizontal Divider Line (`فاصل خط`) separating the profile info from the posts grid.
 * - 3-Column Published Reels Grid showing the extracted Video Cover Frame (`غلاف الفيديو`) and Views Counter (`عداد المشاهدات`) on top of each post.
 * - 3-Lines Hamburger Side Menu Sheet containing:
 *   - "عدد الفيديوهات التي تمت رؤيتها" (showing ONLY the numeric counter badge)
 *   - "لوحة تحكم المطور"
 *   - All app settings (Account, Contact Us, Official Telegram, Player Seek Control, App Description, Privacy Policy)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    telegramUrl: String = "https://t.me/jdj_q",
    currentUser: SocialUserProfile? = null,
    myPublishedReels: List<ReelItem> = emptyList(),
    watchedReelsCount: Int = 0,
    isDevLoginSheetOpen: Boolean = false,
    isLoggingInDeveloper: Boolean = false,
    devLoginError: String? = null,
    onOpenAuthScreen: () -> Unit = {},
    onLogoutUser: () -> Unit = {},
    onUpdateProfilePhoto: (Uri) -> Unit = {},
    onOpenReelFromProfile: (ReelItem) -> Unit = {},
    onDeleteReelFromProfile: (ReelItem) -> Unit = {},
    onOpenContactUsScreen: () -> Unit = {},
    onOpenDeveloperSheet: () -> Unit = {},
    onDismissDeveloperSheet: () -> Unit = {},
    onSubmitDeveloperLogin: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        VodPlayerPreferencesManager.initialize(context)
    }
    val vodPrefs by VodPlayerPreferencesManager.preferencesFlow.collectAsState()
    val targetUrl = telegramUrl.ifBlank { "https://t.me/jdj_q" }
    var activeSubPage by remember { mutableStateOf(SettingsSubPage.NONE) }
    var isSideSettingsMenuOpen by remember { mutableStateOf(false) }

    val profilePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateProfilePhoto(uri)
        }
    }

    // Independent Full-Screen Page 1: Privacy Policy
    if (activeSubPage == SettingsSubPage.PRIVACY_POLICY) {
        PrivacyPolicyIndependentScreen(
            onBack = { activeSubPage = SettingsSubPage.NONE }
        )
        return
    }

    // Independent Full-Screen Page 2: App Description
    if (activeSubPage == SettingsSubPage.APP_DESCRIPTION) {
        AppDescriptionIndependentScreen(
            onBack = { activeSubPage = SettingsSubPage.NONE }
        )
        return
    }

    // Half-Screen Developer Login Modal Bottom Sheet
    if (SHOW_DEVELOPER_PANEL_ENTRY && isDevLoginSheetOpen) {
        DeveloperLoginBottomSheet(
            isLoading = isLoggingInDeveloper,
            errorMessage = devLoginError,
            onDismiss = onDismissDeveloperSheet,
            onLogin = onSubmitDeveloperLogin
        )
    }

    // 3-Lines Hamburger Side Menu Sheet (Settings + Watched Videos Counter + Developer Panel)
    if (isSideSettingsMenuOpen) {
        ProfileThreeLinesSettingsSideSheet(
            currentUser = currentUser,
            telegramUrl = targetUrl,
            watchedReelsCount = watchedReelsCount,
            activeSeekSeconds = vodPrefs.forwardSeekSeconds,
            onDismiss = { isSideSettingsMenuOpen = false },
            onOpenAuthScreen = {
                isSideSettingsMenuOpen = false
                onOpenAuthScreen()
            },
            onLogoutUser = {
                isSideSettingsMenuOpen = false
                onLogoutUser()
            },
            onOpenContactUsScreen = {
                isSideSettingsMenuOpen = false
                onOpenContactUsScreen()
            },
            onOpenTelegram = {
                isSideSettingsMenuOpen = false
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                    context.startActivity(intent)
                } catch (_: Exception) {
                }
            },
            onUpdateSeekSeconds = { sec ->
                VodPlayerPreferencesManager.updateUnifiedSeekStep(sec)
            },
            onOpenAppDescription = {
                isSideSettingsMenuOpen = false
                activeSubPage = SettingsSubPage.APP_DESCRIPTION
            },
            onOpenPrivacyPolicy = {
                isSideSettingsMenuOpen = false
                activeSubPage = SettingsSubPage.PRIVACY_POLICY
            },
            onOpenDeveloperSheet = {
                isSideSettingsMenuOpen = false
                onOpenDeveloperSheet()
            }
        )
    }

    val isLoggedAndComplete = currentUser != null && currentUser.isProfileComplete
    val displayPhotoUrl = currentUser?.profilePhotoUrl?.takeIf { it.isNotBlank() }
        ?: SocialUserProfile.DEFAULT_PROFILE_PHOTO_URL
    val displayPersonalName = if (isLoggedAndComplete) {
        currentUser?.personalName?.ifBlank { currentUser.username } ?: "مستخدم FOUL TV"
    } else {
        "زائر FOUL TV"
    }
    val displayUsername = if (isLoggedAndComplete) {
        "@${currentUser?.username?.removePrefix("@").orEmpty()}"
    } else {
        "@guest_foultv"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FoulBlackBg)
            .testTag("profile_screen_root")
    ) {
        // 1. Top Bar: Username Handle + 3-Lines Hamburger Menu Icon (`☰`) for Settings
        Surface(
            color = Color(0xFF0B0714),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.07f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isLoggedAndComplete) displayUsername else "الملف الشخصي",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    if (isLoggedAndComplete) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "موثق",
                            tint = FoulPurpleBright,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // 3-Lines Hamburger Menu Icon (Settings Trigger)
                Surface(
                    onClick = { isSideSettingsMenuOpen = true },
                    shape = RoundedCornerShape(12.dp),
                    color = FoulCardDark,
                    border = BorderStroke(1.dp, FoulPurpleBright.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("profile_side_menu_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "قائمة الإعدادات الجانبية",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 2. Instagram-Style Profile Header (Avatar + Posts / Followers / Following Counters + Bio)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular Profile Photo with "+" badge to pick/change photo from device
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .size(84.dp)
                        .clickable {
                            if (isLoggedAndComplete) {
                                profilePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else {
                                onOpenAuthScreen()
                            }
                        }
                        .testTag("profile_avatar_picker_box")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = FoulCardDark,
                        border = BorderStroke(
                            width = 2.2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(FoulPurpleBright, FoulPurplePrimary, FoulGoldStar)
                            )
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AsyncImage(
                            model = displayPhotoUrl,
                            contentDescription = "صورة الحساب الشخصي",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    // "+" Badge on Profile Photo
                    Surface(
                        shape = CircleShape,
                        color = FoulPurplePrimary,
                        border = BorderStroke(1.8.dp, FoulBlackBg),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "تغيير الصورة الشخصية",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Instagram-Style 3 Stat Columns: المنشورات | المتابعون | أتابعهم
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InstagramStatColumn(
                        count = myPublishedReels.size,
                        label = "المنشورات",
                        testTag = "profile_posts_count"
                    )
                    InstagramStatColumn(
                        count = currentUser?.followersCount ?: 0,
                        label = "المتابعون",
                        testTag = "profile_followers_count"
                    )
                    InstagramStatColumn(
                        count = currentUser?.followingCount ?: 0,
                        label = "أتابعهم",
                        testTag = "profile_following_count"
                    )
                }
            }

            // Personal Name + @username underneath
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = displayPersonalName,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.5.sp
                )
                Text(
                    text = displayUsername,
                    color = FoulPurpleGlow,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }

            // If Guest (skipped onboarding), show a quick Create Account / Sign In banner
            if (!isLoggedAndComplete) {
                Surface(
                    onClick = onOpenAuthScreen,
                    shape = RoundedCornerShape(12.dp),
                    color = FoulPurplePrimary.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, FoulPurpleBright.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_guest_login_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "سجل دخولك أو أنشئ حساباً لنشر الريلز وتخصيص ملفك الشخصي",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FoulPurplePrimary
                        ) {
                            Text(
                                text = "دخول / حساب",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Horizontal Divider Line (`فاصل خط`) between Followers/Profile Info and Posts below
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = FoulPurpleBright.copy(alpha = 0.35f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0918))
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = "المنشورات",
                    tint = FoulPurpleBright,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "المنشورات (${myPublishedReels.size})",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = Color.White.copy(alpha = 0.10f)
            )
        }

        // 4. Published Reels 3-Column Grid below the Divider Line
        if (myPublishedReels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = FoulCardDark,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = FoulTextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = "لا توجد منشورات بعد",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "عندما تقوم بنشر مقاطع ريلز، ستظهر هنا بشكل مرتب مع غلاف الفيديو وعداد المشاهدات",
                        color = FoulTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("profile_published_reels_grid")
            ) {
                items(myPublishedReels, key = { it.id }) { reel ->
                    ProfilePublishedReelGridCard(
                        reel = reel,
                        onClick = { onOpenReelFromProfile(reel) },
                        onDeleteClick = { onDeleteReelFromProfile(reel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InstagramStatColumn(
    count: Int,
    label: String,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.testTag(testTag)
    ) {
        Text(
            text = count.toString(),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )
        Text(
            text = label,
            color = FoulTextSecondary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

/**
 * Individual Reel Card in the User's Profile 3-Column Grid:
 * - Extracts and displays the Video Cover Frame (`غلاف الفيديو`) from the video URL.
 * - Shows the Views Counter (`عداد المشاهدات`) on top of the post card (`فوق عداد المشاهدات`).
 * - Includes a quick Delete button for the owner to delete their published Reel.
 */
@Composable
private fun ProfilePublishedReelGridCard(
    reel: ReelItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val cachedBitmap = ReelVideoCoverBitmapCache[reel.id]
    val videoSourceUrl = reel.streamUrl.ifBlank { reel.videoUrl }

    // Extract video cover frame asynchronously if not yet cached
    LaunchedEffect(reel.id, videoSourceUrl) {
        if (cachedBitmap == null && reel.thumbnailUrl.isBlank() && videoSourceUrl.startsWith("http")) {
            val extracted = withContext(Dispatchers.IO) {
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(
                            videoSourceUrl,
                             mapOf("User-Agent" to "FOUL-TV-ExoPlayer/1.2.2")
                        )
                        retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            ?: retriever.frameAtTime
                    } finally {
                        runCatching { retriever.release() }
                    }
                }.getOrNull()
            }
            if (extracted != null) {
                ReelVideoCoverBitmapCache[reel.id] = extracted
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF140E24))
            .border(0.7.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .testTag("profile_reel_card_${reel.id}")
    ) {
        // 1. Video Cover Image (`غلاف الفيديو`)
        when {
            cachedBitmap != null -> {
                Image(
                    bitmap = cachedBitmap.asImageBitmap(),
                    contentDescription = reel.caption.ifBlank { "غلاف الفيديو" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            reel.thumbnailUrl.isNotBlank() -> {
                AsyncImage(
                    model = reel.thumbnailUrl,
                    contentDescription = reel.caption.ifBlank { "غلاف الفيديو" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                // Sleek gradient cover with center Play icon while frame extracts
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    FoulPurpleDeep.copy(alpha = 0.85f),
                                    Color(0xFF120B22),
                                    Color(0xFF090612)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Top & Bottom subtle gradient scrims so the Views counter on top and caption below are crisp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.72f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. Views Counter on TOP of the Post Card (`فوق عداد المشاهدات`)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.62f),
                border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.22f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "عدد المشاهدات",
                        tint = FoulLiveGreen,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = reel.viewsCount.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                }
            }

            // Quick Delete Icon for Owner
            Surface(
                onClick = onDeleteClick,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.62f),
                modifier = Modifier
                    .size(22.dp)
                    .testTag("profile_delete_reel_${reel.id}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "حذف الريلز",
                        tint = Color(0xFFFF8A80),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Bottom caption preview strip
        if (reel.caption.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.82f)
                            )
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = reel.caption,
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 3-Lines Hamburger Side Menu Sheet opened from the top of the Profile Screen:
 * - Includes "عدد الفيديوهات التي تمت رؤيتها" with ONLY the numeric counter badge (`watchedReelsCount`).
 * - Includes "لوحة تحكم المطور" (`SHOW_DEVELOPER_PANEL_ENTRY = true`).
 * - Includes all Settings items (Account, Contact Us, Official Telegram, Player Seek Control, App Description, Privacy Policy).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileThreeLinesSettingsSideSheet(
    currentUser: SocialUserProfile?,
    telegramUrl: String,
    watchedReelsCount: Int,
    activeSeekSeconds: Int,
    onDismiss: () -> Unit,
    onOpenAuthScreen: () -> Unit,
    onLogoutUser: () -> Unit,
    onOpenContactUsScreen: () -> Unit,
    onOpenTelegram: () -> Unit,
    onUpdateSeekSeconds: (Int) -> Unit,
    onOpenAppDescription: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenDeveloperSheet: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E0A18),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .padding(bottom = 28.dp)
                .testTag("settings_list"),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الإعدادات والقائمة الجانبية",
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

            OrderedColorfulDivider(thickness = 1.2.dp)

            // 0. Identity & Account Card
            SettingsIdentityHeroBanner(
                currentUser = currentUser,
                versionName = AppVersionConfig.APP_CURRENT_VERSION
            )

            SettingsUserAccountCard(
                currentUser = currentUser,
                onOpenAuthScreen = onOpenAuthScreen,
                onLogoutUser = onLogoutUser
            )

            // 1. NEW: "عدد الفيديوهات التي تمت رؤيتها" — displays ONLY the numeric counter badge
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = FoulCardDark),
                border = BorderStroke(
                    width = 0.9.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(FoulPurpleBright.copy(alpha = 0.55f), FoulDivider)
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_watched_reels_counter_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(FoulPurpleDeep.copy(alpha = 0.45f), FoulCardDark, FoulBlackBg)
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = FoulPurplePrimary.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, FoulPurpleBright.copy(alpha = 0.55f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = FoulLiveGreen,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Text(
                            text = "عدد الفيديوهات التي تمت رؤيتها",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }

                    // Counter ONLY badge
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = FoulLiveGreen.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, FoulLiveGreen.copy(alpha = 0.65f)),
                        modifier = Modifier.testTag("settings_watched_reels_count_badge")
                    ) {
                        Text(
                            text = watchedReelsCount.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Section Header: التواصل والقناة الرسمية
            SettingsGroupHeader(
                title = "التواصل المباشر والقناة الرسمية",
                accentColor = FoulTeamAwayCyan
            )

            // Contact Us ("تواصل معنا")
            ProSettingsOptionCard(
                title = "تواصل معنا • الدعم المباشر",
                subtitle = if (currentUser != null) {
                    "حسابك النشط: ${currentUser.username}"
                } else {
                    "مراسلة فريق الدعم الفني واستلام الردود الفورية"
                },
                iconType = SettingsProIconType.SUPPORT_HEADSET,
                iconBgColors = listOf(Color(0xFF00B4DB), FoulPurplePrimary),
                accentColor = FoulTeamAwayCyan,
                badgeText = "متصل 24/7",
                badgeColor = FoulLiveGreen,
                onClick = onOpenContactUsScreen,
                testTag = "settings_contact_us_button"
            )

            // Official Telegram Channel
            ProSettingsOptionCard(
                title = "قناة التيلجرام الرسمية",
                subtitle = "آخر التحديثات وتنبيهات المباريات (${telegramUrl.removePrefix("https://").removePrefix("http://")})",
                iconType = SettingsProIconType.TELEGRAM_PLANE,
                iconBgColors = listOf(Color(0xFF29B6F6), Color(0xFF0277BD)),
                accentColor = Color(0xFF29B6F6),
                badgeText = "رسمي",
                badgeColor = Color(0xFF29B6F6),
                onClick = onOpenTelegram,
                testTag = "settings_telegram_button"
            )

            // Section: إعدادات مشغل الأفلام والمسلسلات
            SettingsGroupHeader(
                title = "إعدادات مشغل الأفلام والمسلسلات السينمائي",
                accentColor = FoulLiveGreen
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = FoulCardDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 0.9.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(FoulLiveGreen.copy(alpha = 0.48f), FoulDivider)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .testTag("settings_vod_seek_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    FoulPurpleDeep.copy(alpha = 0.42f),
                                    FoulCardDark,
                                    FoulBlackBg
                                )
                            )
                        )
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsProIconBadge(
                            iconType = SettingsProIconType.SEEK_CONTROL,
                            gradientColors = listOf(FoulPurplePrimary, Color(0xFF00897B)),
                            accentGlow = FoulLiveGreen
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = "مدة التقديم والتأخير السريع",
                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "ضبط موحد لقفزة التقديم والتأخير معاً في المشغل",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = FoulTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            color = FoulLiveGreen.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.8.dp, FoulLiveGreen.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = "$activeSeekSeconds ثوانٍ",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(FoulBlackBg.copy(alpha = 0.65f))
                            .border(0.8.dp, FoulDivider, RoundedCornerShape(8.dp))
                            .padding(2.5.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VodPlayerPreferencesManager.AVAILABLE_SEEK_SECONDS.forEach { sec ->
                            val selected = activeSeekSeconds == sec
                            Surface(
                                color = if (selected) FoulPurplePrimary else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                border = if (selected) {
                                    BorderStroke(0.9.dp, FoulPurpleBright)
                                } else {
                                    null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .tvFocusableClickable(
                                        shape = RoundedCornerShape(6.dp),
                                        scaleOnFocus = 1.04f,
                                        onClick = {
                                            onUpdateSeekSeconds(sec)
                                        }
                                    )
                                    .testTag("settings_unified_seek_${sec}s")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$sec ث",
                                        color = if (selected) Color.White else FoulTextSecondary,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section: معلومات المنظومة والأمان والخصوصية
            SettingsGroupHeader(
                title = "معلومات المنظومة والأمان والخصوصية",
                accentColor = FoulPurpleGlow
            )

            ProSettingsOptionCard(
                title = "وصف عن التطبيق",
                subtitle = "تعرّف على مميزات وأقسام ومنظومة Foul TV الكاملة",
                iconType = SettingsProIconType.APP_ARCHITECTURE,
                iconBgColors = listOf(FoulPurpleBright, FoulPurpleDeep),
                accentColor = FoulPurpleGlow,
                badgeText = "دليل شامل",
                badgeColor = FoulPurpleBright,
                onClick = onOpenAppDescription,
                testTag = "settings_app_description_button"
            )

            ProSettingsOptionCard(
                title = "سياسة الخصوصية والأمان",
                subtitle = "معايير التشفير، حماية البيانات وحقوق المستخدم",
                iconType = SettingsProIconType.PRIVACY_SHIELD,
                iconBgColors = listOf(Color(0xFF00897B), FoulPurpleDeep),
                accentColor = FoulLiveGreen,
                badgeText = "مشفّر وآمن",
                badgeColor = FoulLiveGreen,
                onClick = onOpenPrivacyPolicy,
                testTag = "settings_privacy_policy_button"
            )

            // Developer Control Panel Entry (`SHOW_DEVELOPER_PANEL_ENTRY == true`)
            if (SHOW_DEVELOPER_PANEL_ENTRY) {
                SettingsGroupHeader(
                    title = "الإدارة السحابية ولوحة المطور",
                    accentColor = FoulGoldStar
                )

                ProSettingsOptionCard(
                    title = "لوحة تحكم المطور",
                    subtitle = "إدارة الباقات، القنوات، حسابات IPTV والمكتبة والمباريات",
                    iconType = SettingsProIconType.DEVELOPER_CONSOLE,
                    iconBgColors = listOf(FoulPurpleAccent, FoulPurpleDeep),
                    accentColor = FoulGoldStar,
                    badgeText = "نسخة ${AppVersionConfig.APP_CURRENT_VERSION}",
                    badgeColor = FoulPurplePrimary,
                    onClick = onOpenDeveloperSheet,
                    testTag = "settings_developer_panel_button"
                )
            }
        }
    }
}

/**
 * Dedicated User Account & Privileges Card in Settings Side Menu
 */
@Composable
private fun SettingsUserAccountCard(
    currentUser: SocialUserProfile?,
    onOpenAuthScreen: () -> Unit,
    onLogoutUser: () -> Unit
) {
    val isLoggedAndComplete = currentUser != null && currentUser.isProfileComplete
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FoulCardDark),
        border = BorderStroke(
            width = 0.9.dp,
            brush = Brush.horizontalGradient(
                colors = if (isLoggedAndComplete) {
                    listOf(
                        FoulLiveGreen.copy(alpha = 0.70f),
                        FoulPurplePrimary.copy(alpha = 0.45f),
                        FoulDivider
                    )
                } else {
                    listOf(
                        FoulGoldStar.copy(alpha = 0.75f),
                        FoulPurpleBright.copy(alpha = 0.50f),
                        FoulDivider
                    )
                }
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_user_account_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            FoulPurpleDeep.copy(alpha = 0.65f),
                            FoulCardDark,
                            FoulBlackBg
                        )
                    )
                )
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isLoggedAndComplete && currentUser != null) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = currentUser.personalName.ifBlank { currentUser.username },
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = FoulLiveGreen.copy(alpha = 0.16f),
                            border = BorderStroke(0.8.dp, FoulLiveGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (currentUser.authProvider == "google") "Google موثّق" else "موثّق",
                                color = FoulLiveGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "@${currentUser.username} • ${currentUser.email}",
                        color = FoulTeamAwayCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onLogoutUser,
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FoulLiveRed.copy(alpha = 0.18f),
                        contentColor = FoulLiveRed
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("settings_logout_user_button")
                ) {
                    Text(
                        text = "تسجيل الخروج",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    Text(
                        text = "وضع الزائر (تخطي) • النشر والنسخ مقيد",
                        color = FoulGoldStar,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "سجل حسابك لتفعيل نشر الريلز ونسخ روابط القنوات والمكتبة",
                        color = FoulTextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onOpenAuthScreen,
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FoulPurplePrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("settings_open_auth_screen_button")
                ) {
                    Text(
                        text = "إنشاء حساب / دخول",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Compact, sleek single-row Identity Banner
 */
@Composable
private fun SettingsIdentityHeroBanner(
    currentUser: SocialUserProfile?,
    versionName: String
) {
    Card(
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = FoulCardDark),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    FoulPurpleBright.copy(alpha = 0.65f),
                    FoulPurplePrimary.copy(alpha = 0.40f),
                    FoulDivider
                )
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            FoulPurpleDeep.copy(alpha = 0.78f),
                            FoulCardDark,
                            FoulBlackBg
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(FoulBlackBg)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(FoulPurpleGlow, FoulPurplePrimary)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_foul_tv_launcher),
                        contentDescription = "شعار FOUL TV",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "FOUL TV",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                letterSpacing = 0.6.sp
                            ),
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                        Surface(
                            color = FoulPurplePrimary,
                            shape = RoundedCornerShape(5.dp),
                            border = BorderStroke(0.7.dp, FoulPurpleBright)
                        ) {
                            Text(
                                text = "PRO $versionName",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = if (currentUser != null) {
                            "متصل بحساب: ${currentUser.username} • مزامنة سحابية فورية"
                        } else {
                            "منصة البث المباشر، IPTV، السينما والمباريات العالمية"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = FoulTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                color = FoulBlackBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.9.dp, FoulDivider)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(FoulLiveGreen)
                    )
                    Text(
                        text = "v$versionName",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupHeader(
    title: String,
    accentColor: Color = FoulPurpleBright
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * Compact Custom Multi-Layered Canvas Vector Icon Badge for Settings Cards
 */
@Composable
private fun SettingsProIconBadge(
    iconType: SettingsProIconType,
    gradientColors: List<Color>,
    accentGlow: Color
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(colors = gradientColors))
            .border(
                width = 0.9.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.42f), accentGlow.copy(alpha = 0.35f))
                ),
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 1.9.dp.toPx()

            when (iconType) {
                SettingsProIconType.SUPPORT_HEADSET -> {
                    drawArc(
                        color = Color.White,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(w * 0.14f, h * 0.12f),
                        size = Size(w * 0.72f, h * 0.64f),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.08f, h * 0.42f),
                        size = Size(w * 0.16f, h * 0.30f),
                        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.76f, h * 0.42f),
                        size = Size(w * 0.16f, h * 0.30f),
                        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                    )
                    val micPath = Path().apply {
                        moveTo(w * 0.84f, h * 0.70f)
                        quadraticTo(w * 0.82f, h * 0.90f, w * 0.54f, h * 0.88f)
                    }
                    drawPath(
                        path = micPath,
                        color = Color.White,
                        style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round)
                    )
                    drawCircle(
                        color = FoulLiveGreen,
                        radius = w * 0.07f,
                        center = Offset(w * 0.48f, h * 0.88f)
                    )
                }

                SettingsProIconType.TELEGRAM_PLANE -> {
                    val path = Path().apply {
                        moveTo(w * 0.92f, h * 0.14f)
                        lineTo(w * 0.08f, h * 0.47f)
                        lineTo(w * 0.34f, h * 0.58f)
                        lineTo(w * 0.76f, h * 0.28f)
                        lineTo(w * 0.45f, h * 0.64f)
                        lineTo(w * 0.45f, h * 0.86f)
                        lineTo(w * 0.58f, h * 0.72f)
                        lineTo(w * 0.78f, h * 0.86f)
                        close()
                    }
                    drawPath(path = path, color = Color.White)
                }

                SettingsProIconType.SEEK_CONTROL -> {
                    drawArc(
                        color = Color.White,
                        startAngle = -210f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = Offset(w * 0.12f, h * 0.12f),
                        size = Size(w * 0.76f, h * 0.76f),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    val leftChevron = Path().apply {
                        moveTo(w * 0.44f, h * 0.36f)
                        lineTo(w * 0.28f, h * 0.50f)
                        lineTo(w * 0.44f, h * 0.64f)
                    }
                    val rightChevron = Path().apply {
                        moveTo(w * 0.56f, h * 0.36f)
                        lineTo(w * 0.72f, h * 0.50f)
                        lineTo(w * 0.56f, h * 0.64f)
                    }
                    drawPath(
                        path = leftChevron,
                        color = Color.White,
                        style = Stroke(width = stroke * 0.95f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = rightChevron,
                        color = accentGlow,
                        style = Stroke(width = stroke * 0.95f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                SettingsProIconType.APP_ARCHITECTURE -> {
                    val topDiamond = Path().apply {
                        moveTo(w * 0.50f, h * 0.12f)
                        lineTo(w * 0.90f, h * 0.32f)
                        lineTo(w * 0.50f, h * 0.52f)
                        lineTo(w * 0.10f, h * 0.32f)
                        close()
                    }
                    drawPath(
                        path = topDiamond,
                        color = Color.White.copy(alpha = 0.22f)
                    )
                    drawPath(
                        path = topDiamond,
                        color = Color.White,
                        style = Stroke(width = stroke, join = StrokeJoin.Round)
                    )
                    val midLayer = Path().apply {
                        moveTo(w * 0.12f, h * 0.52f)
                        lineTo(w * 0.50f, h * 0.71f)
                        lineTo(w * 0.88f, h * 0.52f)
                    }
                    drawPath(
                        path = midLayer,
                        color = Color.White,
                        style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    val botLayer = Path().apply {
                        moveTo(w * 0.12f, h * 0.70f)
                        lineTo(w * 0.50f, h * 0.89f)
                        lineTo(w * 0.88f, h * 0.70f)
                    }
                    drawPath(
                        path = botLayer,
                        color = accentGlow,
                        style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                SettingsProIconType.PRIVACY_SHIELD -> {
                    val shield = Path().apply {
                        moveTo(w * 0.50f, h * 0.08f)
                        lineTo(w * 0.86f, h * 0.22f)
                        lineTo(w * 0.86f, h * 0.52f)
                        quadraticTo(w * 0.86f, h * 0.80f, w * 0.50f, h * 0.93f)
                        quadraticTo(w * 0.14f, h * 0.80f, w * 0.14f, h * 0.52f)
                        lineTo(w * 0.14f, h * 0.22f)
                        close()
                    }
                    drawPath(
                        path = shield,
                        color = Color.White.copy(alpha = 0.18f)
                    )
                    drawPath(
                        path = shield,
                        color = Color.White,
                        style = Stroke(width = stroke, join = StrokeJoin.Round)
                    )
                    val check = Path().apply {
                        moveTo(w * 0.34f, h * 0.52f)
                        lineTo(w * 0.46f, h * 0.64f)
                        lineTo(w * 0.68f, h * 0.38f)
                    }
                    drawPath(
                        path = check,
                        color = Color.White,
                        style = Stroke(width = stroke * 1.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                SettingsProIconType.DEVELOPER_CONSOLE -> {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.16f),
                        topLeft = Offset(w * 0.08f, h * 0.14f),
                        size = Size(w * 0.84f, h * 0.72f),
                        cornerRadius = CornerRadius(w * 0.15f, w * 0.15f)
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.08f, h * 0.14f),
                        size = Size(w * 0.84f, h * 0.72f),
                        cornerRadius = CornerRadius(w * 0.15f, w * 0.15f),
                        style = Stroke(width = stroke)
                    )
                    val chevron = Path().apply {
                        moveTo(w * 0.26f, h * 0.37f)
                        lineTo(w * 0.44f, h * 0.50f)
                        lineTo(w * 0.26f, h * 0.63f)
                    }
                    drawPath(
                        path = chevron,
                        color = Color.White,
                        style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawLine(
                        color = FoulGoldStar,
                        start = Offset(w * 0.52f, h * 0.63f),
                        end = Offset(w * 0.74f, h * 0.63f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun ProSettingsOptionCard(
    title: String,
    subtitle: String,
    iconType: SettingsProIconType,
    iconBgColors: List<Color>,
    accentColor: Color,
    badgeText: String? = null,
    badgeColor: Color = FoulPurpleDeep,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FoulCardDark),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .tvFocusableClickable(
                shape = RoundedCornerShape(12.dp),
                scaleOnFocus = 1.02f,
                onClick = onClick
            )
            .border(
                width = 0.9.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(accentColor.copy(alpha = 0.48f), FoulDivider)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(FoulPurpleDeep.copy(alpha = 0.42f), FoulCardDark, FoulBlackBg)
                    )
                )
                .padding(horizontal = 11.dp, vertical = 8.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingsProIconBadge(
                iconType = iconType,
                gradientColors = iconBgColors,
                accentGlow = accentColor
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = FoulTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!badgeText.isNullOrBlank()) {
                Surface(
                    color = badgeColor.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }

            CompactTrailingChevron(accentColor = FoulCardElevated)
        }
    }
}

@Composable
private fun CompactTrailingChevron(accentColor: Color) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor)
            .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
        )
    }
}

/**
 * Independent Full-Screen Page: App Description
 */
@Composable
fun AppDescriptionIndependentScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FoulBlackBg)
            .testTag("app_description_screen")
    ) {
        FoulSmartTopBar(
            telegramTestTag = "app_description_telegram_button"
        )

        FoulSectionSubBar(
            title = "وصف عن التطبيق",
            subtitle = "تعرّف على منظومة Foul TV المتكاملة",
            onBackClick = onBack,
            backButtonTestTag = "app_description_back_button"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FoulCardDark),
                border = BorderStroke(1.dp, FoulPurpleBright.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(FoulPurpleDeep.copy(alpha = 0.7f), FoulCardDark)
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(FoulPurpleBright)
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "تطبيق Foul TV الرسمي",
                                fontSize = 13.sp,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "الإصدار ${AppVersionConfig.APP_CURRENT_VERSION} • منصة البث والترفيه والرياضة",
                                fontSize = 10.sp,
                                color = FoulGoldStar
                            )
                        }
                    }

                    Text(
                        text = "منصة عربية شاملة تجمع البث التلفزيوني المباشر عالي الدقة، مكتبة الأفلام والمسلسلات، وتغطية حية للمباريات والريلز.",
                        fontSize = 11.sp,
                        color = Color.White,
                        lineHeight = 15.sp
                    )
                }
            }

            InfoSectionCard(
                icon = Icons.Default.LiveTv,
                accentColor = FoulPurpleBright,
                title = "1. قسم القنوات المباشرة (الرئيسية)",
                bulletPoints = listOf(
                    "تنظيم احترافي للأفرع والأقسام الفرعية بشبكة منظمة 3×3.",
                    "دعم تعدد السيرفرات والجودات (4K / FHD / HD / SD) مع التبديل الفوري."
                )
            )

            InfoSectionCard(
                icon = Icons.Default.Movie,
                accentColor = FoulTeamAwayCyan,
                title = "2. قسم المكتبة والريلز (REELS)",
                bulletPoints = listOf(
                    "مكتبة متجددة من الأفلام والمسلسلات والريلز مع محرك بحث فائق السرعة.",
                    "عرض المواسم والحلقات والتفاصيل الكاملة مع تشغيل فوري وسلس."
                )
            )

            InfoSectionCard(
                icon = Icons.Default.SportsSoccer,
                accentColor = FoulLiveGreen,
                title = "3. قسم جدول المباريات واللاعبين والأندية",
                bulletPoints = listOf(
                    "متابعة نتائج ومواعيد المباريات مع التشكيلة الرسمية والإحصائيات والأخبار."
                )
            )

            InfoSectionCard(
                icon = Icons.Default.Security,
                accentColor = FoulGoldStar,
                title = "4. السرعة والأمان والدعم المستمر",
                bulletPoints = listOf(
                    "مزامنة سحابية لحظية مع قاعدة البيانات ونظام دعم فني مدمج للتواصل المباشر."
                )
            )
        }
    }
}

/**
 * Independent Full-Screen Page: Privacy Policy
 */
@Composable
fun PrivacyPolicyIndependentScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FoulBlackBg)
            .testTag("privacy_policy_screen")
    ) {
        FoulSmartTopBar(
            telegramTestTag = "privacy_policy_telegram_button"
        )

        FoulSectionSubBar(
            title = "سياسة الخصوصية",
            subtitle = "حماية البيانات ومعايير الأمان والخصوصية في Foul TV",
            onBackClick = onBack,
            backButtonTestTag = "privacy_policy_back_button"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoSectionCard(
                icon = Icons.Default.Security,
                accentColor = FoulLiveGreen,
                title = "1. التزامنا بحماية خصوصيتك",
                bulletPoints = listOf(
                    "يحرص تطبيق Foul TV على حماية خصوصية جميع المستخدمين وتأمين بياناتهم وفق أعلى معايير التشفير.",
                    "لا يطلب التطبيق أي أذونات حساسة غير ضرورية لتشغيل البث أو التحميل."
                )
            )

            InfoSectionCard(
                icon = Icons.Default.VerifiedUser,
                accentColor = FoulTeamAwayCyan,
                title = "2. البيانات التي يتم جمعها واستخدامها",
                bulletPoints = listOf(
                    "عند إنشاء حساب يتم حفظ البريد الإلكتروني واسم المستخدم والصورة الرمزية المختارة فقط لتمكينك من النشر ومراسلة الدعم الفني.",
                    "يتم تسجيل عدادات المشاهدة والإعجابات بشكل إحصائي لتحسين جودة القنوات والسيرفرات."
                )
            )

            InfoSectionCard(
                icon = Icons.Default.GppGood,
                accentColor = FoulPurpleBright,
                title = "3. أمان الاتصالات وعدم مشاركة البيانات",
                bulletPoints = listOf(
                    "لا نقوم ببيع أو مشاركة أي بيانات خاصة بالمستخدمين مع أي أطراف خارجية.",
                    "جميع الاتصالات بقواعد البيانات تتم عبر قنوات مشفرة وآمنة (HTTPS)."
                )
            )
        }
    }
}

@Composable
private fun InfoSectionCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    bulletPoints: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FoulCardDark),
        border = BorderStroke(1.dp, FoulDivider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = accentColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            bulletPoints.forEach { point ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(12.dp)
                    )
                    Text(
                        text = point,
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 14.5.sp
                    )
                }
            }
        }
    }
}

/**
 * Reusable Professional Telegram Vector Emblem Badge
 */
@Composable
fun TelegramVectorBadge(sizeDp: Int = 50) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF2AABEE), Color(0xFF1E88E5))
                )
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.28f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size((sizeDp * 0.52f).dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.92f, h * 0.14f)
                lineTo(w * 0.08f, h * 0.47f)
                lineTo(w * 0.34f, h * 0.58f)
                lineTo(w * 0.76f, h * 0.28f)
                lineTo(w * 0.45f, h * 0.64f)
                lineTo(w * 0.45f, h * 0.86f)
                lineTo(w * 0.58f, h * 0.72f)
                lineTo(w * 0.78f, h * 0.86f)
                close()
            }
            drawPath(path = path, color = Color.White)
        }
    }
}

/**
 * Half-Screen Modal Bottom Sheet for Developer Email & Password Verification
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperLoginBottomSheet(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FoulCardDark,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OrderedColorfulDivider(thickness = 1.5.dp)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsProIconBadge(
                    iconType = SettingsProIconType.DEVELOPER_CONSOLE,
                    gradientColors = listOf(FoulPurpleBright, FoulPurpleDeep),
                    accentGlow = FoulGoldStar
                )

                Column {
                    Text(
                        text = "تحقق هوية المطور",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "أدخل البريد الإلكتروني وكلمة المرور لفتح لوحة التحكم",
                        style = MaterialTheme.typography.bodySmall,
                        color = FoulTextSecondary
                    )
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("البريد الإلكتروني للمطور", color = FoulTextSecondary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Color.White
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = FoulCardElevated,
                    unfocusedContainerColor = FoulCardElevated,
                    focusedBorderColor = FoulPurpleBright,
                    unfocusedBorderColor = FoulDivider,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dev_login_email_input")
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("كلمة المرور", color = FoulTextSecondary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "إظهار كلمة المرور",
                            tint = FoulTextSecondary
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = FoulCardElevated,
                    unfocusedContainerColor = FoulCardElevated,
                    focusedBorderColor = FoulPurpleBright,
                    unfocusedBorderColor = FoulDivider,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dev_login_password_input")
            )

            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    color = FoulLiveRed.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, FoulLiveRed, RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Button(
                onClick = { onLogin(email, password) },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = FoulPurplePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, FoulPurpleBright.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .testTag("dev_login_submit_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("جاري التحقق...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        text = "تسجيل الدخول وفتح لوحة التحكم",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
