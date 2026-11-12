package com.example.ui.screens

import android.accounts.AccountManager
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import coil.compose.AsyncImage
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.AppAvatarSpec
import com.example.data.model.SocialUserProfile
import com.example.data.repository.SocialFirebaseRepository
import com.example.ui.theme.ArabicFontFamily
import com.example.ui.theme.FoulGoldStar
import com.example.ui.theme.FoulLiveGreen
import com.example.ui.theme.FoulLiveRed

// ============================================================================
// FOUL TV Authentication Luxury Color Palette
// ============================================================================
private val AuthOledBlack = Color(0xFF05030B)
private val AuthCardDark = Color(0xFF10091C)
private val AuthInputBg = Color(0xFF170E29)
private val AuthInputBgFocused = Color(0xFF1F1238)
private val AuthPurplePrimary = Color(0xFF6515C8)
private val AuthPurpleBright = Color(0xFF922BFF)
private val AuthPurpleLight = Color(0xFFB45CFF)
private val AuthTextPrimary = Color(0xFFF8F7FF)
private val AuthTextSecondary = Color(0xFFA6A0B7)
private val AuthBorderSubtle = Color(0xFF341F54)

enum class OnboardingAuthStep {
    WELCOME_CHOICE,
    AUTH_FORM,
    GOOGLE_COMPLETE_PROFILE,
    PROFILE_PHOTO_SELECTION
}

enum class AuthFormTab {
    SIGN_UP,
    SIGN_IN
}

private enum class AuthLegalOverlayPage {
    NONE,
    TERMS_OF_USE,
    PRIVACY_POLICY
}

data class DeviceGoogleAccountItem(
    val email: String,
    val displayName: String,
    val badgeLabel: String = "حساب Google في الجهاز"
)

@Composable
fun FoulAuthOnboardingScreen(
    currentUser: SocialUserProfile?,
    isLoading: Boolean,
    errorMessage: String?,
    startDirectlyOnAuthForm: Boolean = true,
    allowBackToApp: Boolean = false,
    onClearError: () -> Unit,
    onConfirmSkipAndStart: () -> Unit,
    onRegisterWithEmail: (personalName: String, username: String, email: String, password: String, avatarId: String) -> Unit,
    onLoginWithEmail: (email: String, password: String) -> Unit,
    onSignInWithGoogleGmail: (googleEmail: String, suggestedName: String) -> Unit,
    onCompleteGoogleProfile: (personalName: String, username: String, avatarId: String) -> Unit,
    onCompleteProfilePhotoStep: (pickedPhotoUri: Uri?) -> Unit = {},
    onBackToApp: () -> Unit = {}
) {
    val context = LocalContext.current

    var currentStep by remember(
        currentUser?.uid,
        currentUser?.isProfileComplete,
        currentUser?.hasCompletedPhotoStep
    ) {
        mutableStateOf(
            when {
                currentUser != null && !currentUser.isProfileComplete ->
                    OnboardingAuthStep.GOOGLE_COMPLETE_PROFILE
                currentUser != null && !currentUser.hasCompletedPhotoStep ->
                    OnboardingAuthStep.PROFILE_PHOTO_SELECTION
                else ->
                    OnboardingAuthStep.AUTH_FORM
            }
        )
    }

    LaunchedEffect(
        currentUser?.uid,
        currentUser?.isProfileComplete,
        currentUser?.hasCompletedPhotoStep
    ) {
        if (currentUser != null && !currentUser.isProfileComplete) {
            currentStep = OnboardingAuthStep.GOOGLE_COMPLETE_PROFILE
        } else if (currentUser != null && !currentUser.hasCompletedPhotoStep) {
            currentStep = OnboardingAuthStep.PROFILE_PHOTO_SELECTION
        }
    }

    var activeLegalPage by remember { mutableStateOf(AuthLegalOverlayPage.NONE) }
    var showSkipWarningModal by remember { mutableStateOf(false) }
    var showGoogleAccountPickerSheet by remember { mutableStateOf(false) }

    // Discover Google accounts registered on this Android device
    val deviceGoogleAccounts = remember { mutableStateListOf<DeviceGoogleAccountItem>() }

    fun refreshDeviceGoogleAccounts() {
        val discovered = SocialFirebaseRepository.getDeviceGoogleAccounts(context)
        deviceGoogleAccounts.clear()
        for ((email, name) in discovered) {
            deviceGoogleAccounts.add(
                DeviceGoogleAccountItem(
                    email = email,
                    displayName = name.ifBlank { email.substringBefore("@") },
                    badgeLabel = "متصل على هذا الجهاز"
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        refreshDeviceGoogleAccounts()
    }

    // Native Android System Google Account Chooser (`AccountManager.newChooseAccountIntent`)
    val systemAccountChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val selectedEmail = result.data
                ?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                ?.trim()
                ?.lowercase()
                .orEmpty()
            if (selectedEmail.contains("@")) {
                val suggestedName = selectedEmail.substringBefore("@")
                    .replace(".", " ")
                    .replace("_", " ")
                    .trim()
                SocialFirebaseRepository.rememberDeviceGoogleAccount(context, selectedEmail, suggestedName)
                refreshDeviceGoogleAccounts()
                showGoogleAccountPickerSheet = false
                onSignInWithGoogleGmail(selectedEmail, suggestedName)
                return@rememberLauncherForActivityResult
            }
        }
        refreshDeviceGoogleAccounts()
        showGoogleAccountPickerSheet = true
    }

    fun launchSystemGoogleAccountPicker() {
        try {
            @Suppress("DEPRECATION")
            val intent = AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                false,
                null,
                null,
                null,
                null
            )
            systemAccountChooserLauncher.launch(intent)
        } catch (_: Exception) {
            showGoogleAccountPickerSheet = true
        }
    }

    fun triggerGoogleSignInFlow() {
        onClearError()
        refreshDeviceGoogleAccounts()
        launchSystemGoogleAccountPicker()
    }

    if (activeLegalPage == AuthLegalOverlayPage.PRIVACY_POLICY) {
        PrivacyPolicyIndependentScreen(
            onBack = { activeLegalPage = AuthLegalOverlayPage.NONE }
        )
        return
    }

    if (activeLegalPage == AuthLegalOverlayPage.TERMS_OF_USE) {
        AppDescriptionIndependentScreen(
            onBack = { activeLegalPage = AuthLegalOverlayPage.NONE }
        )
        return
    }

    BackHandler(enabled = allowBackToApp) {
        if (allowBackToApp) {
            onBackToApp()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(AuthOledBlack)
                .testTag("foul_auth_onboarding_root")
        ) {
            val isWideScreen = maxWidth >= 600.dp
            // Treat all handheld phones (< 900.dp height) as compact so 100% of elements fit on screen without clipping
            val isCompactHeight = maxHeight < 900.dp

            // 1. Luxury OLED Black & Soft Violet Glassmorphic Background Canvas
            FoulReferenceLuxuryBackground(modifier = Modifier.fillMaxSize())

            // 2. Fixed Non-Scrolling Single-Screen Layout Fitted 100% to Phone Viewport
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(
                        horizontal = if (isWideScreen) 28.dp else 14.dp,
                        vertical = 4.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Section: Language Bar + Compact Brand Identity Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    AuthTopLanguageAndBackBar(
                        allowBackToApp = allowBackToApp,
                        onBackToApp = onBackToApp
                    )

                    FoulTvBrandIdentityHeader(isCompactHeight = isCompactHeight)
                }

                // Middle Section: Unified Fixed Auth Card, Google Profile Completion, or Profile Photo Choice
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                        },
                        label = "auth_step_content"
                    ) { step ->
                        when (step) {
                            OnboardingAuthStep.GOOGLE_COMPLETE_PROFILE -> {
                                LuxuryGoogleCompleteProfileSection(
                                    googleEmail = currentUser?.email.orEmpty(),
                                    initialPersonalName = currentUser?.personalName.orEmpty(),
                                    isLoading = isLoading,
                                    errorMessage = errorMessage,
                                    isCompactHeight = isCompactHeight,
                                    onClearError = onClearError,
                                    onSubmitProfile = onCompleteGoogleProfile
                                )
                            }

                            OnboardingAuthStep.PROFILE_PHOTO_SELECTION -> {
                                LuxuryProfilePhotoSelectionSection(
                                    user = currentUser,
                                    isLoading = isLoading,
                                    onConfirmPhotoAndStart = { pickedUri ->
                                        onCompleteProfilePhotoStep(pickedUri)
                                    },
                                    onSkipPhotoAndStart = {
                                        onCompleteProfilePhotoStep(null)
                                    }
                                )
                            }

                            else -> {
                                UnifiedReferenceAuthSection(
                                    isLoading = isLoading,
                                    errorMessage = errorMessage,
                                    isCompactHeight = isCompactHeight,
                                    deviceGoogleAccounts = deviceGoogleAccounts,
                                    onClearError = onClearError,
                                    onSubmitSignUp = onRegisterWithEmail,
                                    onSubmitSignIn = onLoginWithEmail,
                                    onOpenGoogleAccountPicker = ::triggerGoogleSignInFlow,
                                    onQuickSelectGoogleAccount = { acc ->
                                        onClearError()
                                        onSignInWithGoogleGmail(acc.email, acc.displayName)
                                    },
                                    onOpenTermsOfUse = {
                                        activeLegalPage = AuthLegalOverlayPage.TERMS_OF_USE
                                    },
                                    onOpenPrivacyPolicy = {
                                        activeLegalPage = AuthLegalOverlayPage.PRIVACY_POLICY
                                    },
                                    onSkipToAppClick = {
                                        showSkipWarningModal = true
                                    }
                                )
                            }
                        }
                    }
                }

                // Bottom Section: Glowing Neon Play Emblem
                BottomGlowingPlaySkipFooter(
                    onClickSkip = {
                        if (currentStep == OnboardingAuthStep.PROFILE_PHOTO_SELECTION) {
                            onCompleteProfilePhotoStep(null)
                        } else {
                            showSkipWarningModal = true
                        }
                    }
                )
            }
        }
    }

    // Skip Restriction Warning Dialog
    if (showSkipWarningModal) {
        LuxurySkipRestrictionDialog(
            onDismiss = { showSkipWarningModal = false },
            onCreateAccountNow = {
                showSkipWarningModal = false
                onClearError()
                currentStep = OnboardingAuthStep.AUTH_FORM
            },
            onConfirmSkipAndStart = {
                showSkipWarningModal = false
                onConfirmSkipAndStart()
            }
        )
    }

    // Google Device Accounts Picker Bottom Sheet
    if (showGoogleAccountPickerSheet) {
        GoogleDeviceAccountsBottomSheet(
            isLoading = isLoading,
            deviceAccounts = deviceGoogleAccounts,
            onDismiss = { showGoogleAccountPickerSheet = false },
            onLaunchAndroidSystemPicker = ::launchSystemGoogleAccountPicker,
            onSelectGoogleAccount = { email, displayName ->
                SocialFirebaseRepository.rememberDeviceGoogleAccount(context, email, displayName)
                refreshDeviceGoogleAccounts()
                showGoogleAccountPickerSheet = false
                onSignInWithGoogleGmail(email, displayName)
            }
        )
    }
}

/**
 * Compact Top Bar containing the Arabic Language Pill ("العربية") and optional Back button
 */
@Composable
private fun AuthTopLanguageAndBackBar(
    allowBackToApp: Boolean,
    onBackToApp: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (allowBackToApp) {
            Surface(
                onClick = onBackToApp,
                shape = CircleShape,
                color = AuthCardDark.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, AuthPurpleLight.copy(alpha = 0.35f)),
                modifier = Modifier
                    .size(32.dp)
                    .testTag("auth_form_back_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = AuthTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.size(32.dp))
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = AuthCardDark.copy(alpha = 0.78f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        AuthPurpleBright.copy(alpha = 0.55f),
                        AuthBorderSubtle
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AuthTextSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "العربية",
                    fontFamily = ArabicFontFamily,
                    color = AuthTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "اللغة العربية",
                    tint = AuthPurpleLight,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Proportional Phone-Fitted Header:
 * - Official `ic_foul_tv_launcher` icon with soft purple halo
 * - `FOUL` in white + `TV` in vibrant purple
 * - Tagline `"مشاهدة مباشرة • قنوات متنوعة • تجربة استثنائية"`
 */
@Composable
private fun FoulTvBrandIdentityHeader(isCompactHeight: Boolean) {
    val logoBoxSize = if (isCompactHeight) 44.dp else 54.dp
    val haloSize = if (isCompactHeight) 56.dp else 70.dp
    val titleFontSize = if (isCompactHeight) 19.sp else 22.sp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(haloSize)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AuthPurpleBright.copy(alpha = 0.42f),
                                AuthPurplePrimary.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AuthOledBlack,
                border = BorderStroke(
                    width = 1.3.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AuthPurpleLight,
                            AuthPurpleBright,
                            AuthPurplePrimary
                        )
                    )
                ),
                shadowElevation = 10.dp,
                modifier = Modifier.size(logoBoxSize)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_foul_tv_launcher),
                    contentDescription = "شعار FOUL TV",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "FOUL ",
                    color = AuthTextPrimary,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "TV",
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(AuthPurpleLight, AuthPurpleBright)
                        ),
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 1.sp
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                AuthPurpleBright.copy(alpha = 0.50f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Text(
                text = "مشاهدة مباشرة • قنوات متنوعة • تجربة استثنائية",
                fontFamily = ArabicFontFamily,
                color = AuthTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                AuthPurpleBright.copy(alpha = 0.50f)
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Main Unified Fixed-Screen Authentication Section:
 * - Fitted 100% on a single phone screen without scrolling or clipping
 * - Single password field (NO confirm password field!)
 * - Profile photo selection happens in a dedicated step right after account creation
 */
@Composable
private fun UnifiedReferenceAuthSection(
    isLoading: Boolean,
    errorMessage: String?,
    isCompactHeight: Boolean,
    deviceGoogleAccounts: List<DeviceGoogleAccountItem>,
    onClearError: () -> Unit,
    onSubmitSignUp: (personalName: String, username: String, email: String, password: String, avatarId: String) -> Unit,
    onSubmitSignIn: (email: String, password: String) -> Unit,
    onOpenGoogleAccountPicker: () -> Unit,
    onQuickSelectGoogleAccount: (DeviceGoogleAccountItem) -> Unit,
    onOpenTermsOfUse: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onSkipToAppClick: () -> Unit
) {
    var activeTab by remember { mutableStateOf(AuthFormTab.SIGN_UP) }
    var personalName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val selectedAvatarId = remember {
        SocialFirebaseRepository.APP_AVATARS.first().id
    }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    val displayedError = localValidationError ?: errorMessage
    val fieldHeight = if (isCompactHeight) 39.dp else 44.dp
    val fieldSpacing = if (isCompactHeight) 5.5.dp else 7.dp
    val buttonHeight = if (isCompactHeight) 41.dp else 45.dp

    fun handlePrimarySubmit() {
        if (isLoading) return
        localValidationError = null
        onClearError()

        if (activeTab == AuthFormTab.SIGN_UP) {
            val cleanPersonal = personalName.trim()
            val cleanUser = username.trim().removePrefix("@").trim()
            val cleanEmail = email.trim()

            if (cleanPersonal.isBlank()) {
                localValidationError = "يرجى إدخال الاسم الشخصي الكامل"
                return
            }
            if (cleanUser.length < 3) {
                localValidationError = "يرجى إدخال اسم مستخدم صالح (3 أحرف على الأقل)"
                return
            }
            if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                localValidationError = "يرجى إدخال بريد إلكتروني صحيح"
                return
            }
            if (password.length < 6) {
                localValidationError = "كلمة المرور يجب أن تكون 6 أحرف أو أرقام على الأقل"
                return
            }
            onSubmitSignUp(cleanPersonal, cleanUser, cleanEmail, password, selectedAvatarId)
        } else {
            val cleanEmail = email.trim()
            if (cleanEmail.isBlank() || password.isBlank()) {
                localValidationError = "يرجى إدخال البريد الإلكتروني وكلمة المرور"
                return
            }
            onSubmitSignIn(cleanEmail, password)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 5.dp else 8.dp)
    ) {
        // 1. Segmented Control Pill Bar: "إنشاء حساب" | "تسجيل الدخول"
        Surface(
            shape = RoundedCornerShape(50),
            color = AuthCardDark.copy(alpha = 0.90f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        AuthPurpleBright.copy(alpha = 0.45f),
                        AuthBorderSubtle,
                        AuthPurplePrimary.copy(alpha = 0.45f)
                    )
                )
            ),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompactHeight) 38.dp else 43.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SegmentedAuthTabPill(
                    title = "إنشاء حساب",
                    icon = Icons.Default.PersonAdd,
                    selected = activeTab == AuthFormTab.SIGN_UP,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("auth_tab_sign_up"),
                    onClick = {
                        localValidationError = null
                        onClearError()
                        activeTab = AuthFormTab.SIGN_UP
                    }
                )

                SegmentedAuthTabPill(
                    title = "تسجيل الدخول",
                    icon = Icons.AutoMirrored.Filled.Login,
                    selected = activeTab == AuthFormTab.SIGN_IN,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("auth_tab_sign_in"),
                    onClick = {
                        localValidationError = null
                        onClearError()
                        activeTab = AuthFormTab.SIGN_IN
                    }
                )
            }
        }

        // 2. Single Central Glassmorphic Authentication Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = AuthCardDark.copy(alpha = 0.93f),
            border = BorderStroke(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AuthPurpleLight.copy(alpha = 0.72f),
                        AuthPurpleBright.copy(alpha = 0.30f),
                        AuthBorderSubtle.copy(alpha = 0.55f),
                        AuthPurpleBright.copy(alpha = 0.35f)
                    )
                )
            ),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("full_screen_auth_form_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AuthPurplePrimary.copy(alpha = 0.16f),
                                AuthCardDark.copy(alpha = 0.95f),
                                Color(0xFF0B0614)
                            )
                        )
                    )
                    .padding(
                        horizontal = 14.dp,
                        vertical = if (isCompactHeight) 9.dp else 12.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(fieldSpacing)
            ) {
                // Compact Top Icon + Title + Subtitle Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AuthPurpleBright.copy(alpha = 0.35f),
                                        AuthInputBg
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(AuthPurpleLight, AuthPurpleBright)
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (activeTab == AuthFormTab.SIGN_UP) {
                                Icons.Default.PersonAdd
                            } else {
                                Icons.AutoMirrored.Filled.Login
                            },
                            contentDescription = null,
                            tint = AuthTextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = if (activeTab == AuthFormTab.SIGN_UP) {
                                "إنشاء حساب جديد"
                            } else {
                                "مرحباً بعودتك"
                            },
                            fontFamily = ArabicFontFamily,
                            color = AuthTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                        Text(
                            text = if (activeTab == AuthFormTab.SIGN_UP) {
                                "انضم إلى عائلة FOUL TV واستمتع بالمزيد"
                            } else {
                                "سجل الدخول إلى حسابك واستكمل متعة المشاهدة"
                            },
                            fontFamily = ArabicFontFamily,
                            color = AuthTextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    }
                }

                // Error Banner (if any)
                AnimatedVisibility(
                    visible = !displayedError.isNullOrBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = FoulLiveRed.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, FoulLiveRed.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFFF8A80),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = displayedError.orEmpty(),
                                fontFamily = ArabicFontFamily,
                                color = Color(0xFFFF8A80),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Sign Up Extra Fields: Personal Name + Username
                AnimatedVisibility(
                    visible = activeTab == AuthFormTab.SIGN_UP,
                    enter = fadeIn(tween(180)) + expandVertically(tween(180)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(fieldSpacing)
                    ) {
                        // 1. الاسم الشخصي الكامل
                        LuxuryGlassInputField(
                            value = personalName,
                            onValueChange = {
                                localValidationError = null
                                onClearError()
                                personalName = it
                            },
                            placeholder = "الاسم الشخصي الكامل",
                            leadingIcon = Icons.Default.Person,
                            fieldHeight = fieldHeight,
                            testTag = "signup_personal_name_input"
                        )

                        // 2. اسم المستخدم الخاص بك
                        LuxuryGlassInputField(
                            value = username,
                            onValueChange = {
                                localValidationError = null
                                onClearError()
                                username = it
                            },
                            placeholder = "اسم المستخدم الخاص بك (مثال: ahmed_99)",
                            leadingIcon = Icons.Default.AlternateEmail,
                            fieldHeight = fieldHeight,
                            isLtrContent = true,
                            testTag = "signup_username_input"
                        )
                    }
                }

                // 3. البريد الإلكتروني
                LuxuryGlassInputField(
                    value = email,
                    onValueChange = {
                        localValidationError = null
                        onClearError()
                        email = it
                    },
                    placeholder = "البريد الإلكتروني (example@gmail.com)",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    fieldHeight = fieldHeight,
                    isLtrContent = true,
                    testTag = "auth_email_input"
                )

                // 4. كلمة المرور (مرة واحدة فقط بدون تأكيد كلمة المرور)
                LuxuryGlassInputField(
                    value = password,
                    onValueChange = {
                        localValidationError = null
                        onClearError()
                        password = it
                    },
                    placeholder = "كلمة المرور",
                    leadingIcon = Icons.Default.Lock,
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    isPasswordVisible = isPasswordVisible,
                    onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                    fieldHeight = fieldHeight,
                    isLtrContent = true,
                    testTag = "auth_password_input"
                )

                // Primary Gradient Action Button
                LuxuryPrimaryAuthButton(
                    text = if (activeTab == AuthFormTab.SIGN_UP) "إنشاء الحساب" else "تسجيل الدخول",
                    icon = if (activeTab == AuthFormTab.SIGN_UP) {
                        Icons.Default.PersonAdd
                    } else {
                        Icons.AutoMirrored.Filled.Login
                    },
                    buttonHeight = buttonHeight,
                    isLoading = isLoading,
                    onClick = ::handlePrimarySubmit,
                    modifier = Modifier.testTag("auth_submit_primary_button")
                )

                // Compact Divider: "أو المتابعة باستخدام"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        AuthPurpleBright.copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Text(
                        text = "أو المتابعة باستخدام",
                        fontFamily = ArabicFontFamily,
                        color = AuthTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        AuthPurpleBright.copy(alpha = 0.45f)
                                    )
                                )
                            )
                    )
                }

                // White Google Sign-In Button
                LuxuryGoogleWhiteButton(
                    buttonHeight = if (isCompactHeight) 39.dp else 43.dp,
                    enabled = !isLoading,
                    onClick = onOpenGoogleAccountPicker,
                    modifier = Modifier.testTag("auth_google_gmail_button")
                )

                // Compact 1-Tap Device Google Account Chips (single horizontal strip if available)
                if (deviceGoogleAccounts.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(deviceGoogleAccounts, key = { it.email }) { acc ->
                            CompactDeviceGoogleChip(
                                account = acc,
                                onClick = { onQuickSelectGoogleAccount(acc) }
                            )
                        }
                    }
                }

                // Compact Terms & Privacy Policy + Skip Link Footer
                TermsAndPrivacyFooterRow(
                    onOpenTermsOfUse = onOpenTermsOfUse,
                    onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                    onSkipToAppClick = onSkipToAppClick
                )
            }
        }
    }
}

/**
 * Post-Signup Profile Photo Selection Step:
 * - Immediately displays `SocialUserProfile.DEFAULT_PROFILE_PHOTO_URL` ("https://i.ibb.co/v4TM7kbC/images-19.jpg")
 *   inside the circular avatar with a `+` badge on top of it so the user can pick a photo from their phone.
 * - Asks the user: "هل تريد اختيار صورة لحسابك أم تخطي؟" (without mentioning any URL).
 * - If the user taps "تخطي والبدء الآن", keeps the default image and opens the app immediately.
 */
@Composable
private fun LuxuryProfilePhotoSelectionSection(
    user: SocialUserProfile?,
    isLoading: Boolean,
    onConfirmPhotoAndStart: (Uri?) -> Unit,
    onSkipPhotoAndStart: () -> Unit
) {
    var selectedLocalUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedLocalUri = uri
        }
    }

    val displayedPhotoModel: Any = selectedLocalUri
        ?: user?.profilePhotoUrl?.takeIf { it.isNotBlank() }
        ?: SocialUserProfile.DEFAULT_PROFILE_PHOTO_URL

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AuthCardDark.copy(alpha = 0.94f),
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    AuthPurpleLight.copy(alpha = 0.78f),
                    AuthPurpleBright.copy(alpha = 0.35f),
                    AuthBorderSubtle
                )
            )
        ),
        shadowElevation = 20.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_photo_selection_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "الصورة الشخصية لحسابك",
                fontFamily = ArabicFontFamily,
                color = AuthTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "هل تريد اختيار صورة لحسابك أم تخطي؟",
                fontFamily = ArabicFontFamily,
                color = AuthTextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            // Circular Profile Photo with "+" Badge to pick from device
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(108.dp)
                    .clickable(enabled = !isLoading) {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("profile_photo_circle_picker")
            ) {
                Surface(
                    shape = CircleShape,
                    color = AuthInputBg,
                    border = BorderStroke(
                        width = 2.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(AuthPurpleLight, AuthPurpleBright, FoulGoldStar)
                        )
                    ),
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AsyncImage(
                        model = displayedPhotoModel,
                        contentDescription = "الصورة الشخصية",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                // Glowing "+" Badge on the corner of the profile image
                Surface(
                    shape = CircleShape,
                    color = AuthPurpleBright,
                    border = BorderStroke(2.dp, AuthOledBlack),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إضافة صورة من الهاتف",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (user != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = user.personalName.ifBlank { user.username },
                        fontFamily = ArabicFontFamily,
                        color = AuthTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                    Text(
                        text = "@${user.username.removePrefix("@")}",
                        color = AuthPurpleLight,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            // Pick / Change Photo from Phone Button
            Surface(
                onClick = {
                    if (!isLoading) {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                color = AuthInputBg,
                border = BorderStroke(1.dp, AuthPurpleLight.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("pick_photo_from_device_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = AuthPurpleLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedLocalUri == null) "اختيار صورة من الهاتف" else "تغيير الصورة المختارة",
                        fontFamily = ArabicFontFamily,
                        color = AuthTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Confirm / Continue Primary Button
            LuxuryPrimaryAuthButton(
                text = if (selectedLocalUri != null) "حفظ الصورة والبدء" else "متابعة والبدء",
                icon = Icons.Default.CheckCircle,
                buttonHeight = 44.dp,
                isLoading = isLoading,
                onClick = { onConfirmPhotoAndStart(selectedLocalUri) },
                modifier = Modifier.testTag("confirm_profile_photo_button")
            )

            // Skip Button (keeps default https://i.ibb.co/v4TM7kbC/images-19.jpg silently)
            Text(
                text = "تخطي والبدء الآن",
                fontFamily = ArabicFontFamily,
                color = AuthTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !isLoading, onClick = onSkipPhotoAndStart)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("skip_profile_photo_button")
            )
        }
    }
}

/**
 * Compact 1-Tap Google Email Pill for fixed single-screen display
 */
@Composable
private fun CompactDeviceGoogleChip(
    account: DeviceGoogleAccountItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = AuthInputBg,
        border = BorderStroke(1.dp, AuthPurpleBright.copy(alpha = 0.45f)),
        modifier = Modifier.testTag("device_google_account_row_${account.email}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GoogleGLogoCanvas(modifier = Modifier.size(13.dp))
            Text(
                text = account.email,
                color = AuthTextPrimary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "• دخول",
                fontFamily = ArabicFontFamily,
                color = AuthPurpleLight,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Segmented Tab Pill Button inside the top Segmented Control
 */
@Composable
private fun SegmentedAuthTabPill(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "tab_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxSize()
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            AuthPurpleBright,
                            AuthPurplePrimary
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Transparent)
                    )
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) AuthTextPrimary else AuthTextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                fontFamily = ArabicFontFamily,
                color = if (selected) AuthTextPrimary else AuthTextSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Custom Luxury Glassmorphic Input Field fitted for single-screen phone display
 */
@Composable
private fun LuxuryGlassInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    fieldHeight: Dp = 46.dp,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    isLtrContent: Boolean = false,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderBrush = if (isFocused) {
        Brush.horizontalGradient(
            colors = listOf(AuthPurpleLight, AuthPurpleBright)
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                AuthBorderSubtle.copy(alpha = 0.85f),
                AuthBorderSubtle.copy(alpha = 0.55f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(fieldHeight)
            .clip(RoundedCornerShape(15.dp))
            .background(if (isFocused) AuthInputBgFocused else AuthInputBg)
            .border(
                width = if (isFocused) 1.3.dp else 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(15.dp)
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (isFocused) AuthPurpleLight else AuthPurpleLight.copy(alpha = 0.78f),
                modifier = Modifier.size(19.dp)
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontFamily = ArabicFontFamily,
                        color = AuthTextSecondary.copy(alpha = 0.78f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = ArabicFontFamily,
                        color = AuthTextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Start,
                        textDirection = if (isLtrContent && value.isNotEmpty()) {
                            TextDirection.Ltr
                        } else {
                            TextDirection.ContentOrRtl
                        }
                    ),
                    cursorBrush = SolidColor(AuthPurpleLight),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation = if (isPassword && !isPasswordVisible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
                )
            }

            if (isPassword && onTogglePasswordVisibility != null) {
                IconButton(
                    onClick = onTogglePasswordVisibility,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isPasswordVisible) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        },
                        contentDescription = "إظهار أو إخفاء كلمة المرور",
                        tint = AuthTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Primary Purple Gradient Action Button
 */
@Composable
private fun LuxuryPrimaryAuthButton(
    text: String,
    icon: ImageVector,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Dp = 47.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isLoading) 0.98f else 1f,
        animationSpec = tween(120),
        label = "primary_btn_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .height(buttonHeight)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = AuthPurpleBright,
                spotColor = AuthPurpleBright
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFA033FF),
                        AuthPurpleBright,
                        AuthPurplePrimary
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        AuthPurpleLight.copy(alpha = 0.85f),
                        AuthPurpleBright.copy(alpha = 0.45f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isLoading,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = AuthTextPrimary,
                strokeWidth = 2.2.dp,
                modifier = Modifier.size(21.dp)
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AuthTextPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = text,
                        fontFamily = ArabicFontFamily,
                        color = AuthTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.28f))
                        .border(1.dp, AuthPurpleLight.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = AuthTextPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * White Google Sign-In Button
 */
@Composable
private fun LuxuryGoogleWhiteButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Dp = 45.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = tween(120),
        label = "google_btn_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .height(buttonHeight)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(23.dp)
            )
            .clip(RoundedCornerShape(23.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFEDEAF5)
                    )
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = "المتابعة باستخدام Google",
                fontFamily = ArabicFontFamily,
                color = Color(0xFF14111F),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            GoogleGLogoCanvas(modifier = Modifier.size(19.dp))
        }
    }
}

/**
 * Compact Terms of Use & Privacy Policy + Skip Link Footer
 */
@Composable
private fun TermsAndPrivacyFooterRow(
    onOpenTermsOfUse: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onSkipToAppClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "بمتابعتك، أنت توافق على ",
                fontFamily = ArabicFontFamily,
                color = AuthTextSecondary,
                fontSize = 10.5.sp
            )
            Text(
                text = "شروط الاستخدام",
                fontFamily = ArabicFontFamily,
                color = AuthPurpleLight,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onOpenTermsOfUse)
                    .padding(horizontal = 2.dp)
                    .testTag("auth_terms_of_use_link")
            )
            Text(
                text = " و ",
                fontFamily = ArabicFontFamily,
                color = AuthTextSecondary,
                fontSize = 10.5.sp
            )
            Text(
                text = "سياسة الخصوصية",
                fontFamily = ArabicFontFamily,
                color = AuthPurpleLight,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onOpenPrivacyPolicy)
                    .padding(horizontal = 2.dp)
                    .testTag("auth_privacy_policy_link")
            )
        }

        Text(
            text = "تخطي والبدء بدون حساب",
            fontFamily = ArabicFontFamily,
            color = AuthTextSecondary.copy(alpha = 0.78f),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onSkipToAppClick)
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .testTag("auth_form_skip_link_button")
        )
    }
}

/**
 * Compact Glowing Neon Play Emblem at the bottom of the screen
 */
@Composable
private fun BottomGlowingPlaySkipFooter(
    onClickSkip: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClickSkip)
            .testTag("welcome_skip_initial_button")
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val playPath = Path().apply {
                moveTo(w * 0.24f, h * 0.14f)
                lineTo(w * 0.86f, h * 0.50f)
                lineTo(w * 0.24f, h * 0.86f)
                close()
            }
            drawPath(
                path = playPath,
                color = AuthPurpleBright.copy(alpha = 0.35f),
                style = Stroke(
                    width = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            drawPath(
                path = playPath,
                brush = Brush.linearGradient(
                    colors = listOf(AuthPurpleLight, AuthPurpleBright)
                ),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * Background Canvas matching Reference Image
 */
@Composable
private fun FoulReferenceLuxuryBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0B0418),
                    AuthOledBlack,
                    Color(0xFF080312)
                )
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AuthPurplePrimary.copy(alpha = 0.22f * glowPulse),
                    AuthPurpleBright.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.46f),
                radius = w * 0.72f
            ),
            radius = w * 0.72f,
            center = Offset(w * 0.5f, h * 0.46f)
        )

        val topWave = Path().apply {
            moveTo(0f, 0f)
            lineTo(w * 0.36f, 0f)
            quadraticTo(w * 0.24f, h * 0.08f, 0f, h * 0.14f)
            close()
        }
        drawPath(
            path = topWave,
            brush = Brush.linearGradient(
                colors = listOf(
                    AuthPurplePrimary.copy(alpha = 0.38f),
                    AuthPurpleBright.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(w * 0.36f, h * 0.14f)
            )
        )

        val topWaveStroke = Path().apply {
            moveTo(w * 0.34f, 0f)
            quadraticTo(w * 0.22f, h * 0.08f, 0f, h * 0.135f)
        }
        drawPath(
            path = topWaveStroke,
            brush = Brush.linearGradient(
                colors = listOf(
                    AuthPurpleLight.copy(alpha = 0.65f),
                    AuthPurpleBright.copy(alpha = 0.25f),
                    Color.Transparent
                )
            ),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        val bottomLeftWave = Path().apply {
            moveTo(0f, h * 0.91f)
            quadraticTo(w * 0.18f, h * 0.95f, w * 0.32f, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = bottomLeftWave,
            brush = Brush.linearGradient(
                colors = listOf(
                    AuthPurpleBright.copy(alpha = 0.30f),
                    AuthPurplePrimary.copy(alpha = 0.12f)
                )
            )
        )

        val bottomLeftStroke = Path().apply {
            moveTo(0f, h * 0.915f)
            quadraticTo(w * 0.17f, h * 0.955f, w * 0.30f, h)
        }
        drawPath(
            path = bottomLeftStroke,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    AuthPurpleLight.copy(alpha = 0.70f),
                    AuthPurpleBright.copy(alpha = 0.20f)
                )
            ),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        val bottomRightStroke = Path().apply {
            moveTo(w, h * 0.92f)
            quadraticTo(w * 0.83f, h * 0.96f, w * 0.70f, h)
        }
        drawPath(
            path = bottomRightStroke,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    AuthPurpleBright.copy(alpha = 0.20f),
                    AuthPurpleLight.copy(alpha = 0.60f)
                )
            ),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )

        val dotSpacing = 14.dp.toPx()
        val baseDotRadius = 1.8.dp.toPx()
        for (row in 0..6) {
            for (col in 0..6) {
                if (row + col <= 6) {
                    val alpha = (0.22f * (1f - (row + col) / 7f)).coerceIn(0.03f, 0.22f)
                    drawCircle(
                        color = AuthPurpleLight.copy(alpha = alpha),
                        radius = baseDotRadius,
                        center = Offset(
                            x = 12.dp.toPx() + col * dotSpacing,
                            y = 14.dp.toPx() + row * dotSpacing
                        )
                    )
                    drawCircle(
                        color = AuthPurpleLight.copy(alpha = alpha),
                        radius = baseDotRadius,
                        center = Offset(
                            x = w - 12.dp.toPx() - col * dotSpacing,
                            y = h - 14.dp.toPx() - row * dotSpacing
                        )
                    )
                }
            }
        }
    }
}

/**
 * Mandatory Post-Google-Login Profile Completion Section (Fixed single-screen layout)
 */
@Composable
private fun LuxuryGoogleCompleteProfileSection(
    googleEmail: String,
    initialPersonalName: String,
    isLoading: Boolean,
    errorMessage: String?,
    isCompactHeight: Boolean,
    onClearError: () -> Unit,
    onSubmitProfile: (personalName: String, username: String, avatarId: String) -> Unit
) {
    var personalName by remember(initialPersonalName) { mutableStateOf(initialPersonalName) }
    var username by remember(googleEmail) {
        val suggested = googleEmail.substringBefore("@").replace(Regex("[^a-zA-Z0-9_]"), "_")
        mutableStateOf(suggested)
    }
    var selectedAvatarId by remember {
        mutableStateOf(SocialFirebaseRepository.APP_AVATARS.first().id)
    }
    var localError by remember { mutableStateOf<String?>(null) }

    val activeError = localError ?: errorMessage
    val fieldHeight = if (isCompactHeight) 44.dp else 48.dp

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AuthCardDark.copy(alpha = 0.94f),
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    AuthPurpleLight.copy(alpha = 0.75f),
                    AuthPurpleBright.copy(alpha = 0.35f),
                    AuthBorderSubtle
                )
            )
        ),
        shadowElevation = 20.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("google_complete_profile_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    GoogleGLogoCanvas(modifier = Modifier.size(22.dp))
                }
            }

            Text(
                text = "إكمال بيانات حسابك",
                fontFamily = ArabicFontFamily,
                color = AuthTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = FoulLiveGreen.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, FoulLiveGreen.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = FoulLiveGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = googleEmail.ifBlank { "تم ربط حساب Google بنجاح" },
                        fontFamily = ArabicFontFamily,
                        color = AuthTextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "أضف اسمك الشخصي واسم المستخدم الخاص بك لإتمام التسجيل وتفعيل كامل مزايا FOUL TV",
                fontFamily = ArabicFontFamily,
                color = AuthTextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center
            )

            if (!activeError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = FoulLiveRed.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, FoulLiveRed.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = activeError,
                        fontFamily = ArabicFontFamily,
                        color = Color(0xFFFF8A80),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            LuxuryGlassInputField(
                value = personalName,
                onValueChange = {
                    localError = null
                    onClearError()
                    personalName = it
                },
                placeholder = "الاسم الشخصي الكامل",
                leadingIcon = Icons.Default.Person,
                fieldHeight = fieldHeight,
                testTag = "google_complete_personal_name_input"
            )

            LuxuryGlassInputField(
                value = username,
                onValueChange = {
                    localError = null
                    onClearError()
                    username = it
                },
                placeholder = "اسم المستخدم الخاص بك (مثال: ahmed_99)",
                leadingIcon = Icons.Default.AlternateEmail,
                fieldHeight = fieldHeight,
                isLtrContent = true,
                testTag = "google_complete_username_input"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "الرمزية:",
                    fontFamily = ArabicFontFamily,
                    color = AuthTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(SocialFirebaseRepository.APP_AVATARS, key = { it.id }) { av ->
                        AvatarMiniChoiceChip(
                            avatar = av,
                            isSelected = av.id == selectedAvatarId,
                            onClick = { selectedAvatarId = av.id }
                        )
                    }
                }
            }

            LuxuryPrimaryAuthButton(
                text = "حفظ البيانات والبدء",
                icon = Icons.Default.CheckCircle,
                buttonHeight = if (isCompactHeight) 45.dp else 48.dp,
                isLoading = isLoading,
                onClick = {
                    val cleanName = personalName.trim()
                    val cleanUser = username.trim().removePrefix("@").trim()
                    if (cleanName.isBlank()) {
                        localError = "يرجى إدخال الاسم الشخصي الكامل"
                        return@LuxuryPrimaryAuthButton
                    }
                    if (cleanUser.length < 3) {
                        localError = "يرجى إدخال اسم مستخدم صالح (3 أحرف على الأقل)"
                        return@LuxuryPrimaryAuthButton
                    }
                    onSubmitProfile(cleanName, cleanUser, selectedAvatarId)
                },
                modifier = Modifier.testTag("google_complete_submit_button")
            )
        }
    }
}

/**
 * 1-Tap Device Google Account Row inside the Google Bottom Sheet
 */
@Composable
private fun DeviceGoogleEmailOneTapRow(
    account: DeviceGoogleAccountItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = AuthInputBg,
        border = BorderStroke(1.dp, AuthPurpleBright.copy(alpha = 0.40f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AuthPurpleBright, AuthPurplePrimary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = account.displayName.take(1).uppercase().ifBlank { "G" },
                        color = AuthTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.displayName.ifBlank { account.email.substringBefore("@") },
                        fontFamily = ArabicFontFamily,
                        color = AuthTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = account.email,
                        color = AuthPurpleLight,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = AuthPurplePrimary.copy(alpha = 0.25f),
                border = BorderStroke(0.8.dp, AuthPurpleLight.copy(alpha = 0.55f))
            ) {
                Text(
                    text = "دخول فوري",
                    fontFamily = ArabicFontFamily,
                    color = AuthTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Google Device Accounts Picker Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoogleDeviceAccountsBottomSheet(
    isLoading: Boolean,
    deviceAccounts: List<DeviceGoogleAccountItem>,
    onDismiss: () -> Unit,
    onLaunchAndroidSystemPicker: () -> Unit,
    onSelectGoogleAccount: (email: String, displayName: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddNewAccountInline by remember { mutableStateOf(false) }
    var newAccountEmail by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AuthCardDark,
        scrimColor = Color.Black.copy(alpha = 0.74f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .testTag("google_device_accounts_sheet"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                GoogleGLogoCanvas(modifier = Modifier.size(19.dp))
                            }
                        }
                        Column {
                            Text(
                                text = "اختر حساب Google للمتابعة",
                                fontFamily = ArabicFontFamily,
                                color = AuthTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            )
                            Text(
                                text = "تسجيل الدخول السريع إلى FOUL TV بدون كلمة مرور",
                                fontFamily = ArabicFontFamily,
                                color = AuthTextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = AuthTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AuthBorderSubtle)
                )

                Surface(
                    onClick = onLaunchAndroidSystemPicker,
                    shape = RoundedCornerShape(16.dp),
                    color = AuthInputBg,
                    border = BorderStroke(1.2.dp, AuthPurpleBright.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_sheet_system_picker_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AuthPurplePrimary.copy(alpha = 0.28f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = AuthPurpleLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "اختيار من حسابات Google المسجلة في النظام",
                                    fontFamily = ArabicFontFamily,
                                    color = AuthTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "يفتح نافذة حسابات أندرويد لاختيار بريدك بنقرة واحدة",
                                    fontFamily = ArabicFontFamily,
                                    color = AuthTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = AuthTextPrimary
                        )
                    }
                }

                if (deviceAccounts.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "الحسابات المتوفرة على هذا الجهاز:",
                            fontFamily = ArabicFontFamily,
                            color = AuthTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )

                        deviceAccounts.forEach { acc ->
                            DeviceGoogleEmailOneTapRow(
                                account = acc,
                                onClick = {
                                    onSelectGoogleAccount(acc.email, acc.displayName)
                                }
                            )
                        }
                    }
                }

                if (!showAddNewAccountInline) {
                    Surface(
                        onClick = { showAddNewAccountInline = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, AuthBorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("google_sheet_use_another_account_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = AuthPurpleLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "استخدام حساب Google آخر",
                                fontFamily = ArabicFontFamily,
                                color = AuthTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!localError.isNullOrBlank()) {
                            Text(
                                text = localError!!,
                                fontFamily = ArabicFontFamily,
                                color = Color(0xFFFF8A80),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LuxuryGlassInputField(
                            value = newAccountEmail,
                            onValueChange = {
                                localError = null
                                newAccountEmail = it
                            },
                            placeholder = "بريد Google Gmail (name@gmail.com)",
                            leadingIcon = Icons.Default.Email,
                            keyboardType = KeyboardType.Email,
                            isLtrContent = true,
                            testTag = "google_sheet_quick_email_input"
                        )

                        LuxuryPrimaryAuthButton(
                            text = "متابعة بهذا الحساب",
                            icon = Icons.Default.CheckCircle,
                            isLoading = isLoading,
                            onClick = {
                                val raw = newAccountEmail.trim().lowercase()
                                val normalized = if (raw.isNotBlank() && !raw.contains("@")) {
                                    "$raw@gmail.com"
                                } else raw

                                if (normalized.isBlank() || !normalized.contains("@")) {
                                    localError = "يرجى كتابة بريد Gmail صحيح"
                                } else {
                                    val derivedName = normalized.substringBefore("@")
                                        .replace(".", " ")
                                        .replace("_", " ")
                                        .trim()
                                    onSelectGoogleAccount(normalized, derivedName)
                                }
                            },
                            modifier = Modifier.testTag("google_sheet_quick_continue_button")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

/**
 * Skip Restriction Confirmation Dialog
 */
@Composable
private fun LuxurySkipRestrictionDialog(
    onDismiss: () -> Unit,
    onCreateAccountNow: () -> Unit,
    onConfirmSkipAndStart: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = AuthCardDark,
                border = BorderStroke(
                    width = 1.3.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AuthPurpleLight.copy(alpha = 0.8f),
                            FoulGoldStar.copy(alpha = 0.65f)
                        )
                    )
                ),
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .widthIn(max = 420.dp)
                    .testTag("welcome_skip_warning_box")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    AuthPurplePrimary.copy(alpha = 0.22f),
                                    AuthCardDark,
                                    AuthOledBlack
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(FoulGoldStar.copy(alpha = 0.14f))
                            .border(1.4.dp, FoulGoldStar.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = FoulGoldStar,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Text(
                        text = "ملاحظة هامة قبل التخطي",
                        fontFamily = ArabicFontFamily,
                        color = AuthTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "ملاحظة: إذا ضغطت تخطي فلا يمكنك نشر ريلزات وكذلك لا يمكنك نسخ أي رابط فيديو أو رابط قنوات أو من المكتبة إلا بعد إكمال المطلوب وإنشاء حساب.",
                        fontFamily = ArabicFontFamily,
                        color = AuthTextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = onCreateAccountNow,
                        colors = ButtonDefaults.buttonColors(containerColor = AuthPurplePrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("warning_want_create_account_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = AuthTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "أريد إنشاء حساب الآن",
                            fontFamily = ArabicFontFamily,
                            color = AuthTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Surface(
                        onClick = onConfirmSkipAndStart,
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, AuthBorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("warning_confirm_skip_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "لا أريد ذلك، ابدأ الآن",
                                fontFamily = ArabicFontFamily,
                                color = FoulGoldStar,
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

@Composable
private fun AvatarMiniChoiceChip(
    avatar: AppAvatarSpec,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) AuthPurplePrimary.copy(alpha = 0.35f) else AuthInputBg,
        border = BorderStroke(
            width = if (isSelected) 1.3.dp else 1.dp,
            color = if (isSelected) AuthPurpleLight else AuthBorderSubtle
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(Color(avatar.accentHex)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatar.nameAr.take(1),
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp
                )
            }
            Text(
                text = avatar.nameAr,
                fontFamily = ArabicFontFamily,
                color = if (isSelected) AuthTextPrimary else AuthTextSecondary,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GoogleGLogoCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.22f
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)

        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 195f,
            sweepAngle = 115f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 125f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 35f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -15f,
            sweepAngle = 55f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.50f, size.height * 0.50f),
            end = Offset(size.width - inset * 0.4f, size.height * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Butt
        )
    }
}
