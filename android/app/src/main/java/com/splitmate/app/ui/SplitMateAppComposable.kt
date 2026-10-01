package com.splitmate.app

import com.splitmate.app.ui.*
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.graphics.shapes.Morph
import com.splitmate.app.data.ExpenseGroupEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.data.PhoneIdentityValidator
import com.splitmate.app.data.PhoneOtpAuthManager
import com.splitmate.app.ui.ContactPickerBottomSheet
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.GroupCategoryIcons
import com.splitmate.app.ui.NewGroupMemberDraft
import com.splitmate.app.ui.SplitMateBrandFontFamily
import com.splitmate.app.ui.SplitMateDisplayFontFamily
import com.splitmate.app.ui.SplitMateThemeMode
import com.splitmate.app.ui.SplitMateThemeState
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.buildDiceBearOpenPeepsUrl
import com.splitmate.app.ui.extractInitialsFromNameOrSeed
import com.splitmate.app.ui.resolveGroupCategoryIcon
import com.splitmate.app.ui.components.ActiveTravelPassMode
import com.splitmate.app.ui.components.AnimatedTransitDeckHeroCard
import com.splitmate.app.ui.components.ButtonGroup
import com.splitmate.app.ui.components.CircularWavyProgressIndicator
import com.splitmate.app.ui.components.ConnectedButtonGroup
import com.splitmate.app.ui.components.ContainedLoadingIndicator
import com.splitmate.app.ui.components.ExperimentalMaterial3ExpressiveApi
import com.splitmate.app.ui.components.ExpressiveMenuAction
import com.splitmate.app.ui.components.FloatingToolbarDefaults
import com.splitmate.app.ui.components.HorizontalFloatingToolbar
import com.splitmate.app.ui.components.LinearWavyProgressIndicator
import com.splitmate.app.ui.components.MaterialShapes
import com.splitmate.app.ui.components.MorphPolygonShape
import com.splitmate.app.ui.components.RoundedPolygonShape
import com.splitmate.app.ui.components.SplitButtonLayout
import com.splitmate.app.ui.components.SplitMateMotion
import com.splitmate.app.ui.components.UpiExpressPaymentSheet
import com.splitmate.app.ui.components.WavyProgressIndicatorDefaults
import com.splitmate.app.ui.components.floatingToolbarVerticalNestedScroll
import com.splitmate.app.ui.components.rememberAnimatedSegmentedIslandItemShape
import com.splitmate.app.ui.components.segmentedIslandItemShape
import com.splitmate.app.ui.components.toShape
import com.splitmate.app.ui.screens.ActivityDetailSheet
import com.splitmate.app.ui.screens.EditFriendUpiDialog
import com.splitmate.app.ui.screens.FlightExpenseReviewScreen
import com.splitmate.app.ui.screens.OnboardingSetupScreen
import com.splitmate.app.ui.screens.PnrExpenseReviewScreen
import com.splitmate.app.ui.screens.QuickExpenseScreen
import com.splitmate.app.ui.screens.UserSettingsScreen
import com.splitmate.app.ui.toSmartTitleCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

// ==============================================================================
// 1. MATERIAL 3 EXPRESSIVE PALETTE & TOKENS (GM3 3-Theme Expressive & DesignSystemBindings)
// ==============================================================================
object SplitMateTheme {
    var isDark by mutableStateOf(false)

    private val resolvedPalette: SplitMateExpressivePalette
        get() = when {
            isDark && !DesignSystemBindings.activeThemeMode.isDark ->
                SplitMateThemeMode.WARM_ESPRESSO_NIGHT.toPalette()
            !isDark && DesignSystemBindings.activeThemeMode.isDark ->
                SplitMateThemeMode.SUNLIT_BUCKWHEAT.toPalette()
            else -> DesignSystemBindings.activePalette
        }

    val ScreenBg: Color
        get() = resolvedPalette.surfaceContainerLow
    val PrimaryDark: Color
        get() = resolvedPalette.onSurface
    val AccentSage: Color
        get() = resolvedPalette.primaryContainer
    val SageSurface: Color
        get() = if (isDark || DesignSystemBindings.activeThemeMode.isDark) {
            resolvedPalette.primaryContainer
        } else if (DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) {
            resolvedPalette.primaryContainer
        } else {
            Color(0xFFEAF3DC)
        }
    val SageText: Color
        get() = if (isDark || DesignSystemBindings.activeThemeMode.isDark) {
            resolvedPalette.onPrimaryContainer
        } else if (DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) {
            resolvedPalette.onPrimaryContainer
        } else {
            DesignSystemBindings.ElementsPositiveText
        }
    val TerracottaSurface: Color
        get() = if (isDark || DesignSystemBindings.activeThemeMode.isDark) {
            Color(0xFF3A2019)
        } else {
            resolvedPalette.secondaryContainer.copy(alpha = 0.65f)
        }
    val TerracottaText: Color
        get() = if (isDark || DesignSystemBindings.activeThemeMode.isDark) {
            Color(0xFFFECDD3)
        } else {
            resolvedPalette.onSecondaryContainer
        }
    val BrandCoral: Color
        get() = resolvedPalette.secondary
    val SurfaceWhite: Color
        get() = resolvedPalette.surfaceContainerLowest
    val SurfaceMuted: Color
        get() = resolvedPalette.surfaceContainer
    val BorderLight: Color
        get() = resolvedPalette.outline
    val TextSecondary: Color
        get() = resolvedPalette.onSurfaceVariant

    val RadiusHero = DesignSystemBindings.GM3ShapeExtraLarge
    // v2.3.2 M3E geometry: cards standardize on 20.dp corners.
    val RadiusCard = RoundedCornerShape(20.dp)
    val RadiusPanel = RoundedCornerShape(20.dp)
    val RadiusButton = RoundedCornerShape(16.dp)
    val RadiusDialog = RoundedCornerShape(28.dp)
    val RadiusInput = RoundedCornerShape(20.dp)
    val RadiusBadge = DesignSystemBindings.GM3ShapePill

    val FontRounded = SplitMateBrandFontFamily
    val FontDisplay = SplitMateDisplayFontFamily
}

// ==============================================================================
// 2. ROOT NAVIGATION HOST & SCAFFOLD
// ==============================================================================
enum class SplitMateTab(
    val label: String,
    val icon: ImageVector,
    val showInPrimaryNav: Boolean = true
) {
    LEDGERS("Ledgers", Icons.Rounded.AccountBalanceWallet, showInPrimaryNav = true),
    SPLIT("Split", Icons.AutoMirrored.Rounded.ReceiptLong, showInPrimaryNav = false),
    SETTLE("Settle", Icons.Rounded.SwapHoriz, showInPrimaryNav = true),
    AUDIT("Audit", Icons.Rounded.HistoryEdu, showInPrimaryNav = true)
}

@Composable
fun SplitMateApp(viewModel: SplitMateViewModel) {
    val context = LocalContext.current
    remember(context) {
        com.splitmate.app.data.CloudGroupSyncRepository.init(context.applicationContext)
        true
    }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences("splitmate_prefs", android.content.Context.MODE_PRIVATE) }
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val activeGroups by viewModel.activeGroups.collectAsStateWithLifecycle()

    // Observe and sync 3-Theme Expressive preference across app restarts
    LaunchedEffect(Unit) {
        viewModel.loadInitialPersistedThemeFromPrefs(context)
        if (!prefs.contains("expressive_theme_mode") && prefs.contains("is_dark_theme")) {
            val savedDark = prefs.getBoolean("is_dark_theme", false)
            if (savedDark != uiState.isDarkTheme) {
                viewModel.toggleDarkTheme(savedDark)
            }
        }
    }
    SplitMateTheme.isDark = uiState.isDarkTheme
    SplitMateThemeState.activeThemeMode = uiState.activeThemeMode
    DesignSystemBindings.activeThemeMode = uiState.activeThemeMode

    // Automatic Network Connectivity Monitor: flushes any offline PENDING expenses the instant internet returns
    DisposableEffect(context, uiState.hasRegisteredProfile) {
        val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val initialOnline = com.splitmate.app.data.CloudGroupSyncRepository.isInternetAvailable(context)
        viewModel.onNetworkConnectivityChanged(context, initialOnline)
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                viewModel.onNetworkConnectivityChanged(context, true)
            }

            override fun onCapabilitiesChanged(
                network: android.net.Network,
                networkCapabilities: android.net.NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (hasInternet) {
                    viewModel.onNetworkConnectivityChanged(context, true)
                }
            }

            override fun onLost(network: android.net.Network) {
                val stillOnline = com.splitmate.app.data.CloudGroupSyncRepository.isInternetAvailable(context)
                viewModel.onNetworkConnectivityChanged(context, stillOnline)
            }
        }
        runCatching {
            connectivityManager?.registerDefaultNetworkCallback(callback)
        }
        onDispose {
            runCatching {
                connectivityManager?.unregisterNetworkCallback(callback)
            }
        }
    }

    // Real-Time Live Cloud Stream (< 1s delivery whenever any group member logs/edits an expense or settles up)
    // v2.3.4: the OPENED trip's plan topic rides the same stream (added early so it stays inside the
    // 12-topic cap). A plan-topic event triggers a Plan-tab plan sync, never a ledger sync.
    val liveStreamTopicKey = remember(uiState.hasRegisteredProfile, uiState.userPhone, uiState.groups, uiState.openedGroupDetailId) {
        if (!uiState.hasRegisteredProfile) {
            emptyList()
        } else {
            val normPhone = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(uiState.userPhone)
            buildList {
                if (normPhone.length == 10) {
                    add("splitmate_v2_idx_$normPhone")
                }
                uiState.openedGroupDetailId
                    ?.takeIf { opened -> uiState.groups.any { it.groupId == opened && !it.isDemoSeed } }
                    ?.let { opened -> add(com.splitmate.app.data.CloudGroupSyncRepository.groupPlanTopicForGroupId(opened)) }
                uiState.groups.filter { !it.isDemoSeed }.forEach { g ->
                    add(com.splitmate.app.data.CloudGroupSyncRepository.groupTopicForGroupId(g.groupId))
                }
            }.distinct()
        }
    }
    LaunchedEffect(liveStreamTopicKey) {
        if (liveStreamTopicKey.isNotEmpty()) {
            while (true) {
                val changedTopic = com.splitmate.app.data.CloudGroupSyncRepository.awaitLiveCloudTopicChange(liveStreamTopicKey)
                if (!changedTopic.isNullOrBlank()) {
                    if (com.splitmate.app.data.guide.TripGuideServices.isPlanTopic(changedTopic)) {
                        uiState.openedGroupDetailId?.let { com.splitmate.app.data.guide.TripGuideServices.onPlanTopicChanged(it) }
                    } else {
                        viewModel.handleLiveCloudTopicEvent(context, changedTopic)
                    }
                    kotlinx.coroutines.delay(500L)
                } else {
                    kotlinx.coroutines.delay(3_000L)
                }
            }
        }
    }

    // Fast active-trip Cloud Sync loop (every 12s while viewing a specific trip so new expenses from other members appear within seconds)
    LaunchedEffect(uiState.openedGroupDetailId, uiState.hasRegisteredProfile) {
        val openedGroupId = uiState.openedGroupDetailId
        if (uiState.hasRegisteredProfile && !openedGroupId.isNullOrBlank()) {
            while (true) {
                viewModel.syncActiveGroupNow(context = context, groupId = openedGroupId, silent = true)
                kotlinx.coroutines.delay(12_000L)
            }
        }
    }

    // Automatic fault-tolerant background Cloud Sync & Online Presence heartbeat (every 45s while app is open)
    LaunchedEffect(uiState.userPhone, uiState.hasRegisteredProfile) {
        if (uiState.hasRegisteredProfile) {
            while (true) {
                viewModel.performSilentAutoCloudSync(context)
                kotlinx.coroutines.delay(45_000L)
            }
        }
    }

    // Immediate silent auto-sync whenever the app returns to foreground (ON_RESUME)
    DisposableEffect(lifecycleOwner, uiState.userPhone, uiState.hasRegisteredProfile, uiState.openedGroupDetailId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && uiState.hasRegisteredProfile) {
                val openedId = uiState.openedGroupDetailId
                if (!openedId.isNullOrBlank()) {
                    viewModel.syncActiveGroupNow(context = context, groupId = openedId, silent = true)
                }
                viewModel.performSilentAutoCloudSync(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val startRoute = if (uiState.hasRegisteredProfile) "dashboard" else "onboarding"

    LaunchedEffect(uiState.hasRegisteredProfile) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (!uiState.hasRegisteredProfile && currentRoute != "onboarding") {
            navController.navigate("onboarding") {
                popUpTo(0) { inclusive = true }
            }
        } else if (uiState.hasRegisteredProfile && currentRoute == "onboarding") {
            navController.navigate("dashboard") {
                popUpTo("onboarding") { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startRoute
    ) {
        composable("onboarding") {
            SplitMateCloudOtpOnboardingScreen(
                viewModel = viewModel,
                onCompleteToDashboard = {
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            SplitMateMainDashboardScaffold(
                viewModel = viewModel,
                onOpenSettings = { navController.navigate("settings") }
            )
        }

        composable("settings") {
            UserSettingsScreen(
                userName = uiState.currentUserName.ifBlank { "Explorer" },
                userPhone = uiState.userPhone,
                avatarSeed = uiState.currentUserSeed,
                upiId = uiState.userUpiId,
                defaultCurrencyCode = "INR (₹)",
                totalBalanceText = totalBalance,
                activeGroupsCount = activeGroups.size,
                isDarkThemeInitial = uiState.isDarkTheme,
                activeThemeMode = uiState.activeThemeMode,
                allCurrencies = uiState.currencyRates,
                onSyncLiveRates = {},
                onBackClick = { navController.popBackStack() },
                onUpdateUpiId = { newUpi -> 
                    viewModel.updateUserProfile(uiState.currentUserName, uiState.userPhone, uiState.currentUserSeed, newUpi) 
                },
                onUpdateCurrencyCode = {},
                onUpdateUserProfile = { newName, newPhone, newSeed ->
                    viewModel.updateUserProfile(newName, newPhone, newSeed, uiState.userUpiId)
                },
                onThemeToggle = { isDark ->
                    prefs.edit().putBoolean("is_dark_theme", isDark).apply()
                    viewModel.toggleDarkTheme(isDark)
                },
                onSelectThemeMode = { mode ->
                    prefs.edit().putBoolean("is_dark_theme", mode.isDark).apply()
                    viewModel.setExpressiveThemeMode(mode, context)
                },
                onExportLedgerText = {
                    buildString {
                        appendLine("SplitMate Trip & Ledger Summary")
                        appendLine("User: ${uiState.currentUserName} (${uiState.userPhone.ifBlank { "Offline Ledger" }})")
                        appendLine("Overall Net Position: $totalBalance")
                        appendLine("Active Groups (${uiState.groups.size}):")
                        uiState.groups.forEach { g ->
                            val gExps = uiState.expenses.filter { it.groupId == g.groupId }
                            val gSpend = gExps.sumOf { it.totalAmountCents } / 100.0
                            appendLine("• ${g.name}: ${gExps.size} expenses (Total ₹${String.format(Locale.US, "%.2f", gSpend)})")
                        }
                    }
                },
                onClearVaultClick = { viewModel.clearLocalVault() }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SplitMateCloudOtpOnboardingScreen(
    viewModel: SplitMateViewModel,
    onCompleteToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val initialDescriptor = remember(uiState.avatarSeed, uiState.avatarStyleId, uiState.avatarColorPresetId) {
        AvatarSeedCodec.parse(
            rawSeed = uiState.avatarSeed.ifBlank { uiState.currentUserName },
            fallbackStyleId = uiState.avatarStyleId.ifBlank { "open-peeps" },
            fallbackColorPresetId = uiState.avatarColorPresetId.ifBlank { "PastelWall" }
        )
    }

    var onboardingName by rememberSaveable { mutableStateOf(uiState.currentUserName.takeIf { it != "Explorer" }.orEmpty()) }
    var onboardingPhone by rememberSaveable { mutableStateOf(uiState.userPhone) }
    var onboardingUpi by rememberSaveable { mutableStateOf(uiState.userUpiId) }
    var selectedAvatarStyleId by rememberSaveable { mutableStateOf(initialDescriptor.styleId) }
    var selectedColorPresetId by rememberSaveable { mutableStateOf(initialDescriptor.colorPresetId) }
    var selectedGenderId by rememberSaveable { mutableStateOf(initialDescriptor.gender.id) }
    var hasUserManuallySelectedGender by rememberSaveable { mutableStateOf(false) }
    var hasUserCustomizedAvatar by rememberSaveable { mutableStateOf(false) }
    var customSeedKey by rememberSaveable {
        mutableStateOf(
            initialDescriptor.seedKey.takeIf {
                it.isNotBlank() && it != "Explorer" && it != uiState.currentUserName
            }.orEmpty()
        )
    }
    var isStudioExpanded by rememberSaveable { mutableStateOf(false) }
    var enteredOtpCode by rememberSaveable { mutableStateOf("") }
    var enteredPin4 by rememberSaveable { mutableStateOf("") }
    var simVerifiedPhone10 by rememberSaveable { mutableStateOf("") }
    var otpFeedbackMessage by remember { mutableStateOf<String?>(null) }

    val selectedGender = remember(selectedGenderId) { AvatarGender.fromId(selectedGenderId) }

    // 300ms debounce on the live studio avatar seed so rapid typing does not flood network SVG requests
    var debouncedStudioName by remember { mutableStateOf(onboardingName) }
    LaunchedEffect(onboardingName) {
        delay(300L)
        debouncedStudioName = onboardingName
    }

    // Auto-infer gender from first name when typing unless the user explicitly selected a gender segment
    LaunchedEffect(onboardingName, hasUserManuallySelectedGender) {
        if (!hasUserManuallySelectedGender && onboardingName.isNotBlank()) {
            val inferred = inferGenderFromFirstName(onboardingName)
            if (inferred != null && inferred.id != selectedGenderId) {
                selectedGenderId = inferred.id
            }
        }
    }

    val normalizedPhone10 = remember(onboardingPhone) {
        PhoneIdentityValidator.normalizeIndianPhone10(onboardingPhone)
    }
    val isValidPhone10 = remember(normalizedPhone10) {
        PhoneIdentityValidator.isValidIndianMobile10(normalizedPhone10)
    }

    val effectiveSeedKey = remember(customSeedKey, debouncedStudioName) {
        customSeedKey.ifBlank { debouncedStudioName.trim().ifBlank { "Explorer" } }
    }
    val compositeLiveSeed = remember(effectiveSeedKey, selectedGender, selectedAvatarStyleId, selectedColorPresetId) {
        AvatarSeedCodec.encode(
            seedKey = effectiveSeedKey,
            gender = selectedGender,
            styleId = selectedAvatarStyleId,
            colorPresetId = selectedColorPresetId
        )
    }

    LaunchedEffect(normalizedPhone10, isValidPhone10) {
        if (isValidPhone10) {
            viewModel.lookupCloudProfileForPhone(normalizedPhone10) { record ->
                if (record != null) {
                    if (onboardingName.isBlank() && record.name.isNotBlank() && record.name != "Explorer") {
                        onboardingName = record.name
                    }
                    if (onboardingUpi.isBlank() && record.upiVpa.isNotBlank()) {
                        onboardingUpi = record.upiVpa
                    }
                    // Never overwrite avatar style, palette, gender, or customSeedKey if the user already customized them on this screen
                    if (!hasUserCustomizedAvatar) {
                        if (record.avatarStyle.isNotBlank()) {
                            selectedAvatarStyleId = AvatarSeedCodec.parse(
                                rawSeed = record.avatarSeed.ifBlank { record.name },
                                fallbackStyleId = record.avatarStyle,
                                fallbackColorPresetId = record.avatarColorPreset
                            ).styleId
                        }
                        if (record.avatarColorPreset.isNotBlank()) {
                            selectedColorPresetId = AvatarSeedCodec.parse(
                                rawSeed = record.avatarSeed.ifBlank { record.name },
                                fallbackStyleId = record.avatarStyle,
                                fallbackColorPresetId = record.avatarColorPreset
                            ).colorPresetId
                        }
                        if (record.avatarSeed.isNotBlank()) {
                            val parsedRemote = AvatarSeedCodec.parse(
                                rawSeed = record.avatarSeed,
                                fallbackStyleId = record.avatarStyle.ifBlank { selectedAvatarStyleId },
                                fallbackColorPresetId = record.avatarColorPreset.ifBlank { selectedColorPresetId }
                            )
                            selectedGenderId = parsedRemote.gender.id
                            if (parsedRemote.seedKey.isNotBlank() && parsedRemote.seedKey != record.name) {
                                customSeedKey = parsedRemote.seedKey
                            }
                        }
                    }
                }
            }
        }
    }

    val discoveredProfile = uiState.discoveredCloudProfile?.takeIf { it.phone10 == normalizedPhone10 }
    val hasExisting4DigitPin = remember(discoveredProfile, normalizedPhone10) {
        val hash = discoveredProfile?.pinHash.orEmpty()
        hash.isNotBlank() && PhoneOtpAuthManager.is4DigitPinHash(normalizedPhone10, hash)
    }
    var isPinVisible by rememberSaveable { mutableStateOf(false) }

    val nameBringIntoViewRequester = remember { BringIntoViewRequester() }
    val phoneBringIntoViewRequester = remember { BringIntoViewRequester() }
    val pinBringIntoViewRequester = remember { BringIntoViewRequester() }

    val isImeVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0

    Scaffold(
        containerColor = SplitMateTheme.ScreenBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val onboardingStepCount = remember(
                onboardingName,
                discoveredProfile?.name,
                isValidPhone10,
                enteredPin4
            ) {
                var steps = 1 // Step 1: Avatar Studio ready
                if (onboardingName.trim().isNotBlank() || !discoveredProfile?.name.isNullOrBlank()) steps++
                if (isValidPhone10) steps++
                if (enteredPin4.count { it.isDigit() } >= 4) steps++
                steps.coerceIn(1, 4)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Welcome to SplitMate",
                        fontFamily = SplitMateTheme.FontDisplay,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SplitMateTheme.PrimaryDark
                    )
                    Surface(
                        shape = CircleShape,
                        color = SplitMateTheme.SageSurface
                    ) {
                        Text(
                            text = "STEP $onboardingStepCount OF 4",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateTheme.SageText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                LinearWavyProgressIndicator(
                    progress = onboardingStepCount / 4f,
                    modifier = Modifier.fillMaxWidth(),
                    color = SplitMateTheme.SageText,
                    trackColor = SplitMateTheme.BorderLight
                )
            }

            // 1. Hybrid 5-Character Open-Peeps Hero Stage (Compacts automatically when IME or OTP challenge is active)
            OpenPeepsHeroStage(
                name = compositeLiveSeed,
                phone = onboardingPhone,
                selectedStyleId = selectedAvatarStyleId,
                selectedColorPresetId = selectedColorPresetId,
                isCompactMode = isImeVisible || uiState.isOtpChallengeActive
            )

            // 2. Unified M3 Expressive "Identity & Avatar Studio" Bento Card (28.dp corner radius)
            val selectedStyleSpec = remember(selectedAvatarStyleId) {
                SplitMateDiceBearStyles.find { it.id == selectedAvatarStyleId } ?: SplitMateDiceBearStyles.first()
            }
            val selectedPresetSpec = remember(selectedColorPresetId) {
                SplitMateAvatarColorPresets.find { it.id == selectedColorPresetId } ?: SplitMateAvatarColorPresets.first()
            }

            val avatarMorphProgress by animateFloatAsState(
                targetValue = if (isStudioExpanded) 1f else 0f,
                animationSpec = SplitMateMotion.fastSpatial(),
                label = "OnboardingAvatarMorph"
            )
            val avatarMorphShape = MorphPolygonShape(
                morph = remember { Morph(MaterialShapes.SoftBurst, MaterialShapes.Cookie9Sided) },
                percentage = avatarMorphProgress
            )

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SplitMateTheme.BorderLight, RoundedCornerShape(28.dp))
                    .animateContentSize(animationSpec = SplitMateMotion.defaultSpatial())
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Studio Header Row: 76.dp Live Avatar Medallion + Shuffle Badge + Customize Look Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(84.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(82.dp)
                                    .clip(avatarMorphShape)
                                    .background(SplitMateTheme.AccentSage.copy(alpha = 0.38f)),
                                contentAlignment = Alignment.Center
                            ) {
                                SplitMateCharacterAvatar(
                                    name = compositeLiveSeed,
                                    phone = onboardingPhone,
                                    size = 74.dp,
                                    styleId = selectedAvatarStyleId,
                                    colorPresetId = selectedColorPresetId,
                                    gender = selectedGender,
                                    highlighted = true
                                )
                            }
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    hasUserCustomizedAvatar = true
                                    val base = onboardingName.trim().ifBlank { "Explorer" }
                                    customSeedKey = "${base}_${(100..999).random()}"
                                },
                                shape = CircleShape,
                                color = Color(0xFF365314),
                                border = BorderStroke(1.5.dp, Color(0xFFFAF6F0)),
                                modifier = Modifier
                                    .size(28.dp)
                                    .align(Alignment.BottomEnd)
                                    .testTag("RegistrationShuffleAvatarButton")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Casino,
                                        contentDescription = "Shuffle Look",
                                        tint = Color(0xFFD7E8B6),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = onboardingName.trim().ifBlank { "Your Travel Identity" },
                                fontFamily = SplitMateTheme.FontDisplay,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SplitMateTheme.PrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${selectedStyleSpec.label} · ${selectedPresetSpec.label} · ${selectedGender.label}",
                                fontFamily = SplitMateTheme.FontRounded,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SplitMateTheme.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isStudioExpanded = !isStudioExpanded
                                    },
                                    shape = SplitMateTheme.RadiusBadge,
                                    color = if (isStudioExpanded) Color(0xFF365314) else SplitMateTheme.SurfaceMuted,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isStudioExpanded) Color(0xFF416913) else SplitMateTheme.BorderLight
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Palette,
                                            contentDescription = null,
                                            tint = if (isStudioExpanded) Color(0xFFD7E8B6) else SplitMateTheme.PrimaryDark,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (isStudioExpanded) "Done" else "Customize Look",
                                            fontFamily = SplitMateTheme.FontRounded,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isStudioExpanded) Color(0xFFFAF6F0) else SplitMateTheme.PrimaryDark
                                        )
                                        Icon(
                                            imageVector = if (isStudioExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                            contentDescription = null,
                                            tint = if (isStudioExpanded) Color(0xFFD7E8B6) else SplitMateTheme.PrimaryDark,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Collapsible Avatar Studio Drawer (3 Gender Segments + 13 Styles + 12 Pastel & Vibrant Palettes)
                    AnimatedVisibility(
                        visible = isStudioExpanded,
                        enter = expandVertically(animationSpec = SplitMateMotion.defaultSpatial()) + fadeIn(animationSpec = SplitMateMotion.fastEffects()),
                        exit = shrinkVertically(animationSpec = SplitMateMotion.fastSpatial()) + fadeOut(animationSpec = SplitMateMotion.fastEffects())
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(SplitMateTheme.SurfaceMuted.copy(alpha = 0.65f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // A. Gender Identity 3-Segment Selector (M3 Expressive ConnectedButtonGroup)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Character Presentation",
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SplitMateTheme.PrimaryDark
                                )
                                val genderEntries = remember { AvatarGender.entries }
                                ConnectedButtonGroup(
                                    options = genderEntries.map { it.label },
                                    selectedIndex = genderEntries.indexOf(selectedGender).coerceAtLeast(0),
                                    onSelect = { index, _ ->
                                        val genderOption = genderEntries.getOrElse(index) { AvatarGender.NEUTRAL }
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        hasUserManuallySelectedGender = true
                                        hasUserCustomizedAvatar = true
                                        selectedGenderId = genderOption.id
                                    },
                                    labelProvider = { it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // B. 13 Curated DiceBear 9.x Character Art Styles (Static Preview Seeds -> Zero Network Thrashing)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Character Art Style (13 Curated)",
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SplitMateTheme.PrimaryDark
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(SplitMateDiceBearStyles, key = { it.id }) { styleSpec ->
                                        val isSelected = selectedAvatarStyleId == styleSpec.id
                                        val staticChipSeed = "StylePreview_${styleSpec.id}|${selectedGender.id}|${styleSpec.id}|$selectedColorPresetId"
                                        Surface(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                hasUserCustomizedAvatar = true
                                                selectedAvatarStyleId = styleSpec.id
                                            },
                                            shape = SplitMateTheme.RadiusBadge,
                                            color = if (isSelected) Color(0xFF365314) else SplitMateTheme.SurfaceWhite,
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF416913) else SplitMateTheme.BorderLight
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                SplitMateCharacterAvatar(
                                                    name = staticChipSeed,
                                                    phone = "",
                                                    size = 28.dp,
                                                    styleId = styleSpec.id,
                                                    colorPresetId = selectedColorPresetId,
                                                    gender = selectedGender,
                                                    highlighted = isSelected
                                                )
                                                Column {
                                                    Text(
                                                        text = styleSpec.label,
                                                        fontFamily = SplitMateTheme.FontRounded,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isSelected) Color(0xFFFAF6F0) else SplitMateTheme.PrimaryDark
                                                    )
                                                    Text(
                                                        text = styleSpec.subtitle,
                                                        fontFamily = SplitMateTheme.FontRounded,
                                                        fontSize = 10.sp,
                                                        color = if (isSelected) Color(0xFFD7E8B6) else SplitMateTheme.TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // C. 12 Curated Backdrop Color Palettes (Two-Tone Swatch Circles)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Backdrop Palette (12 Pastel & Expressive)",
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SplitMateTheme.PrimaryDark
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(SplitMateAvatarColorPresets, key = { it.id }) { preset ->
                                        val isSelected = selectedColorPresetId == preset.id
                                        Surface(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                hasUserCustomizedAvatar = true
                                                selectedColorPresetId = preset.id
                                            },
                                            shape = SplitMateTheme.RadiusBadge,
                                            color = if (isSelected) preset.primaryBgColor else SplitMateTheme.SurfaceWhite,
                                            border = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) preset.accentRingColor else SplitMateTheme.BorderLight
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            Brush.linearGradient(
                                                                colors = listOf(preset.primaryBgColor, preset.secondarySwatchColor)
                                                            )
                                                        )
                                                        .border(1.5.dp, preset.accentRingColor, CircleShape)
                                                )
                                                Text(
                                                    text = preset.label,
                                                    fontFamily = SplitMateTheme.FontRounded,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                    color = if (isSelected) Color(0xFF23201E) else SplitMateTheme.PrimaryDark
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = SplitMateTheme.BorderLight)

                    // Form Inputs with BringIntoViewRequester so soft keyboard never occludes active fields
                    OutlinedTextField(
                        value = onboardingName,
                        onValueChange = { onboardingName = it },
                        label = { Text("Your Name", fontFamily = SplitMateTheme.FontRounded) },
                        placeholder = { Text("e.g. Akshay Karadkar") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = SplitMateTheme.PrimaryDark)
                        },
                        singleLine = true,
                        shape = SplitMateTheme.RadiusInput,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(nameBringIntoViewRequester)
                            .onFocusEvent { state ->
                                if (state.isFocused) {
                                    coroutineScope.launch {
                                        delay(120L)
                                        nameBringIntoViewRequester.bringIntoView()
                                    }
                                }
                            }
                    )

                    OutlinedTextField(
                        value = onboardingPhone,
                        onValueChange = { onboardingPhone = it },
                        label = { Text("10-Digit Indian Mobile (+91)", fontFamily = SplitMateTheme.FontRounded) },
                        placeholder = { Text("e.g. 9876543210") },
                        leadingIcon = {
                            Icon(Icons.Rounded.PhoneIphone, contentDescription = null, tint = SplitMateTheme.PrimaryDark)
                        },
                        trailingIcon = {
                            if (isValidPhone10) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Valid 10-digit mobile",
                                    tint = Color(0xFF416913)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = SplitMateTheme.RadiusInput,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(phoneBringIntoViewRequester)
                            .onFocusEvent { state ->
                                if (state.isFocused) {
                                    coroutineScope.launch {
                                        delay(120L)
                                        phoneBringIntoViewRequester.bringIntoView()
                                    }
                                }
                            }
                    )

                    if (isValidPhone10) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (hasExisting4DigitPin) Color(0xFFD7E8B6).copy(alpha = 0.55f) else Color(0xFFF4EFE6),
                            border = BorderStroke(1.dp, if (hasExisting4DigitPin) Color(0xFF416913).copy(alpha = 0.35f) else SplitMateTheme.BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (hasExisting4DigitPin) Icons.Rounded.VerifiedUser else Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF365314),
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = if (hasExisting4DigitPin) {
                                        "Welcome back, ${discoveredProfile?.name ?: "Explorer"}! Enter your 4-Digit PIN to unlock +91 $normalizedPhone10."
                                    } else {
                                        "Ready to sync +91 $normalizedPhone10! Optional 4-Digit PIN below locks your account across devices."
                                    },
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF365314)
                                )
                            }
                        }
                    }

                    // 4-Digit Account Security PIN (Optional for new profiles, required if existing PIN is set)
                    OutlinedTextField(
                        value = enteredPin4,
                        onValueChange = { rawPin ->
                            enteredPin4 = rawPin.filter { it.isDigit() }.take(4)
                        },
                        label = {
                            Text(
                                text = if (hasExisting4DigitPin) {
                                    "Enter 4-Digit Security PIN to Unlock Account"
                                } else {
                                    "Optional 4-Digit Security PIN (Recommended for Multi-Device Lock)"
                                },
                                fontFamily = SplitMateTheme.FontRounded
                            )
                        },
                        placeholder = { Text("Optional 4-digit PIN (or leave blank)") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = SplitMateTheme.PrimaryDark)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPinVisible = !isPinVisible }) {
                                Icon(
                                    imageVector = if (isPinVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (isPinVisible) "Hide PIN" else "Show PIN",
                                    tint = SplitMateTheme.PrimaryDark
                                )
                            }
                        },
                        visualTransformation = if (isPinVisible) {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        shape = SplitMateTheme.RadiusInput,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(pinBringIntoViewRequester)
                            .onFocusEvent { state ->
                                if (state.isFocused) {
                                    coroutineScope.launch {
                                        delay(120L)
                                        pinBringIntoViewRequester.bringIntoView()
                                    }
                                }
                            }
                    )

                    // Primary Unified CTA: 10-Digit Phone Number + Optional 4-Digit Security PIN + Awaited Cloud Discovery
                    val isPinReady = if (hasExisting4DigitPin) {
                        enteredPin4.length == 4
                    } else {
                        enteredPin4.isEmpty() || enteredPin4.length == 4
                    }
                    Button(
                        onClick = {
                            val resolvedName = onboardingName.trim().ifBlank {
                                discoveredProfile?.name?.takeIf { it.isNotBlank() } ?: "Explorer"
                            }
                            val resolvedUpi = onboardingUpi.trim().ifBlank {
                                discoveredProfile?.upiVpa?.takeIf { it.isNotBlank() } ?: "$normalizedPhone10@upi"
                            }
                            val finalSeedKey = customSeedKey.ifBlank { resolvedName }
                            otpFeedbackMessage = "Checking invitations for +91 $normalizedPhone10..."
                            viewModel.verifyPinAndRestoreCloud(
                                context = context,
                                rawPhone = onboardingPhone,
                                enteredPin4 = enteredPin4,
                                fallbackUserName = resolvedName,
                                fallbackUpiId = resolvedUpi,
                                avatarStyleId = selectedAvatarStyleId,
                                avatarColorPresetId = selectedColorPresetId,
                                avatarGender = selectedGender.id,
                                customSeedKey = finalSeedKey,
                                preferLocalAvatarChoice = hasUserCustomizedAvatar || !hasExisting4DigitPin
                            ) { ok, msg ->
                                otpFeedbackMessage = msg
                                if (ok) {
                                    onCompleteToDashboard()
                                }
                            }
                        },
                        enabled = isValidPhone10 && (onboardingName.trim().isNotBlank() || !discoveredProfile?.name.isNullOrBlank()) && isPinReady && !uiState.isCloudSyncing,
                        shape = SplitMateTheme.RadiusButton,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SplitMateTheme.SageText,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(
                            imageVector = if (hasExisting4DigitPin) Icons.Rounded.LockOpen else Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = SplitMateTheme.SageSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                uiState.isCloudSyncing -> "Checking invitations for +91 $normalizedPhone10..."
                                hasExisting4DigitPin -> "Unlock Account & Sync Trips ->"
                                else -> "Save Profile & Find My Trips ->"
                            },
                            fontFamily = SplitMateTheme.FontRounded,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        if (!uiState.isCloudSyncing) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    otpFeedbackMessage?.let { feedback ->
                        Text(
                            text = feedback,
                            fontFamily = SplitMateTheme.FontRounded,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (
                                feedback.contains("Invalid", ignoreCase = true) ||
                                feedback.contains("Incorrect", ignoreCase = true) ||
                                feedback.contains("Enter", ignoreCase = true)
                            ) {
                                SplitMateTheme.TerracottaText
                            } else {
                                SplitMateTheme.SageText
                            }
                        )
                    }
                }
            }

            // Secondary Offline-Only Action (clearly distinguished from Cloud Sync)
            TextButton(
                onClick = {
                    val resolvedName = onboardingName.trim().ifBlank { "Explorer" }
                    val finalSeedKey = customSeedKey.ifBlank { resolvedName }
                    val styledSeed = AvatarSeedCodec.encode(
                        seedKey = finalSeedKey,
                        gender = selectedGender,
                        styleId = selectedAvatarStyleId,
                        colorPresetId = selectedColorPresetId
                    )
                    viewModel.completeOnboarding(
                        name = resolvedName,
                        countryName = "India",
                        currencyCode = "INR",
                        currencySymbol = "₹",
                        avatarSeed = styledSeed,
                        userPhone = normalizedPhone10
                    )
                    onCompleteToDashboard()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Continue in Offline Mode without Cloud Sync",
                    fontFamily = SplitMateTheme.FontRounded,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SplitMateTheme.TextSecondary
                )
            }
        }
    }
}

/**
 * M3 Expressive 6-Cell Segmented OTP Input (`48.dp × 54.dp` tactile cells, `14.dp` radius)
 * with clipboard paste support, backspace cell navigation, and tabular numerals (`tnum`).
 */
@Composable
fun OtpSixDigitSegmentedField(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val sanitized = otpValue.filter { it.isDigit() }.take(6)

    BasicTextField(
        value = sanitized,
        onValueChange = { rawInput ->
            val digitsOnly = rawInput.filter { it.isDigit() }.take(6)
            onOtpChange(digitsOnly)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        modifier = modifier
            .focusRequester(focusRequester)
            .testTag("OtpSixDigitSegmentedField"),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (index in 0 until 6) {
                    val charStr = sanitized.getOrNull(index)?.toString().orEmpty()
                    val isFilled = charStr.isNotEmpty()
                    val isCurrentActiveCell = sanitized.length == index || (sanitized.length == 6 && index == 5)
                    
                    val scale by animateFloatAsState(
                        targetValue = if (isCurrentActiveCell) 1.04f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "OtpCellScale"
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isFilled) {
                            Color(0xFFD7E8B6).copy(alpha = if (SplitMateTheme.isDark) 0.22f else 0.45f)
                        } else {
                            SplitMateTheme.SurfaceMuted
                        },
                        border = BorderStroke(
                            width = if (isCurrentActiveCell) 2.dp else 1.dp,
                            color = when {
                                isCurrentActiveCell -> Color(0xFF416913)
                                isFilled -> Color(0xFF416913).copy(alpha = 0.6f)
                                else -> SplitMateTheme.BorderLight
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = 44.dp, max = 52.dp)
                            .height(54.dp)
                            .scale(scale)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = charStr,
                                fontFamily = SplitMateTheme.FontDisplay,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = SplitMateTheme.PrimaryDark,
                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun PendingGroupInviteCard(
    group: ExpenseGroupEntity,
    memberCount: Int,
    userPhone: String,
    onAcceptClick: () -> Unit,
    onDeclineClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val groupIcon = resolveGroupCategoryIcon(group.iconName, group.name)
    val cleanPhoneDisplay = PhoneIdentityValidator.normalizeIndianPhone10(userPhone).ifBlank { userPhone }

    Card(
        shape = SplitMateTheme.RadiusCard,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF6F0)),
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFF416913), SplitMateTheme.RadiusCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(MaterialShapes.Cookie9Sided.toShape())
                            .background(Color(0xFFD7E8B6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = groupIcon,
                            contentDescription = null,
                            tint = Color(0xFF365314),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = SplitMateTheme.RadiusBadge,
                                color = Color(0xFFDCE3FD)
                            ) {
                                Text(
                                    text = "GROUP INVITE",
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF23201E),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "$memberCount members",
                                fontFamily = SplitMateTheme.FontRounded,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SplitMateTheme.TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = group.name.toSmartTitleCase(),
                            fontFamily = SplitMateTheme.FontDisplay,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF23201E)
                        )
                        Text(
                            text = "You were invited via +91 $cleanPhoneDisplay",
                            fontFamily = SplitMateTheme.FontRounded,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF416913)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAcceptClick,
                    shape = SplitMateTheme.RadiusButton,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF416913),
                        contentColor = Color(0xFFFAF6F0)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFFFAF6F0),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Accept & Join",
                        fontFamily = SplitMateTheme.FontRounded,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = Color(0xFFFAF6F0)
                    )
                }

                OutlinedButton(
                    onClick = onDeclineClick,
                    shape = SplitMateTheme.RadiusButton,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFFED8C8),
                        contentColor = Color(0xFFE06B52)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE06B52)),
                    modifier = Modifier
                        .weight(0.75f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = Color(0xFFE06B52),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Decline",
                        fontFamily = SplitMateTheme.FontRounded,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = Color(0xFFE06B52)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SplitMateMainDashboardScaffold(
    viewModel: SplitMateViewModel,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTab = remember(uiState.selectedTabName) {
        runCatching { SplitMateTab.valueOf(uiState.selectedTabName) }.getOrDefault(SplitMateTab.LEDGERS)
    }
    var showPnrReviewScreen by remember { mutableStateOf(false) }
    var activeReviewPnr by remember { mutableStateOf("") }
    var activeFlightTicketResult by remember {
        mutableStateOf<com.splitmate.app.data.UniversalFlightTicketExtractor.UniversalFlightTicketResult?>(null)
    }

    val flightPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val activeGrp = uiState.activeGroup ?: uiState.groups.firstOrNull()
            val memberNames = if (activeGrp != null) {
                uiState.members.filter { it.groupId == activeGrp.groupId }.map { it.name }
            } else {
                uiState.members.map { it.name }.distinct()
            }
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val extracted = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        com.splitmate.app.data.UniversalFlightTicketExtractor.extractFromPdfStream(
                            inputStream = stream,
                            groupMemberNames = memberNames,
                            context = context
                        )
                    }
                }.getOrNull()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (extracted != null && (extracted.isValidFlightTicket || extracted.totalFarePaise > 0L)) {
                        val existingMatch = viewModel.findExistingExpenseByPnr(
                            pnr = extracted.pnr,
                            preferredGroupId = uiState.openedGroupDetailId ?: uiState.activeGroupId
                        )
                        if (existingMatch != null) {
                            val (existingExp, matchedGroup) = existingMatch
                            viewModel.selectActiveGroup(existingExp.groupId)
                            if (uiState.openedGroupDetailId != null) {
                                viewModel.openGroupDetail(existingExp.groupId)
                            }
                            Toast.makeText(
                                context,
                                "Already added! PNR ${extracted.pnr} (${matchedGroup?.name ?: "Group"}) — showing earlier expense.",
                                Toast.LENGTH_LONG
                            ).show()
                            val cachedEarlierFlight = com.splitmate.app.data.PnrNetworkRepository.loadConfirmedFlightTicketResult(context, extracted.pnr)
                            activeFlightTicketResult = cachedEarlierFlight ?: extracted
                        } else {
                            activeFlightTicketResult = extracted
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Could not detect a valid Flight E-Ticket in this PDF.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    fun openFlightTicketReview(pnrOrExpenseKey: String) {
        activeFlightTicketResult = com.splitmate.app.data.PnrNetworkRepository.resolveOrReconstructFlightTicket(
            context = context,
            pnrOrExpenseKey = pnrOrExpenseKey,
            expenses = uiState.expenses,
            members = uiState.members,
            splits = uiState.splits,
            preferredGroupId = uiState.openedGroupDetailId ?: uiState.activeGroupId
        )
    }

    fun openPnrOrFlightTicket(pnrRaw: String) {
        if (pnrRaw.startsWith("EXPENSE:", ignoreCase = true)) {
            val expId = pnrRaw.substringAfter(":").trim()
            val matchingExp = uiState.expenses.find { it.expenseId == expId }
            if (matchingExp == null || isFlightTicketExpense(matchingExp.title) || matchingExp.expenseCategory.equals("FLIGHT", ignoreCase = true)) {
                openFlightTicketReview(pnrRaw)
                return
            }
        }
        val normalized = com.splitmate.app.data.PnrNetworkRepository.normalizePnrKey(pnrRaw)
        val matchingFlightExpense = uiState.expenses.firstOrNull { exp ->
            (isFlightTicketExpense(exp.title) || exp.expenseCategory.equals("FLIGHT", ignoreCase = true)) &&
                (exp.expenseId == pnrRaw || (normalized.isNotBlank() && exp.title.contains(normalized, ignoreCase = true)))
        }
        if (normalized.length == 6 || matchingFlightExpense != null) {
            openFlightTicketReview(pnrRaw)
            return
        }
        activeReviewPnr = pnrRaw
        showPnrReviewScreen = true
    }

    // Intercept system Back when Flight review, Train PNR review, or sub-tab is open
    androidx.activity.compose.BackHandler(
        enabled = activeFlightTicketResult != null || showPnrReviewScreen || currentTab != SplitMateTab.LEDGERS
    ) {
        if (activeFlightTicketResult != null) {
            activeFlightTicketResult = null
        } else if (showPnrReviewScreen) {
            showPnrReviewScreen = false
            activeReviewPnr = ""
        } else if (uiState.returnToGroupDetailId != null) {
            viewModel.finishSubFlowToGroupDetail(uiState.returnToGroupDetailId)
        } else {
            viewModel.selectTab(SplitMateTab.LEDGERS.name)
        }
    }

    activeFlightTicketResult?.let { flightResult ->
        com.splitmate.app.ui.screens.FlightExpenseReviewScreen(
            viewModel = viewModel,
            extractedTicket = flightResult,
            onPickAnotherPdfClick = {
                flightPdfPickerLauncher.launch("application/pdf")
            },
            onBackClick = {
                activeFlightTicketResult = null
            },
            onConfirmAndAddToLedger = {
                activeFlightTicketResult = null
                val loggedGroupId = viewModel.uiState.value.activeGroup?.groupId
                viewModel.finishSubFlowToGroupDetail(loggedGroupId)
            }
        )
        return
    }

    if (showPnrReviewScreen) {
        PnrExpenseReviewScreen(
            viewModel = viewModel,
            initialPnr = activeReviewPnr,
            onBackClick = {
                showPnrReviewScreen = false
                activeReviewPnr = ""
            },
            onExpenseAdded = {
                showPnrReviewScreen = false
                activeReviewPnr = ""
                val loggedGroupId = viewModel.uiState.value.activeGroup?.groupId
                viewModel.finishSubFlowToGroupDetail(loggedGroupId)
            }
        )
        return
    }

    val animatedScreenBg by animateColorAsState(
        targetValue = SplitMateTheme.ScreenBg,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "DashboardScreenBg"
    )
    val animatedPrimaryDark by animateColorAsState(
        targetValue = SplitMateTheme.PrimaryDark,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "DashboardPrimaryDark"
    )

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isMediumOrExpandedWindow = configuration.screenWidthDp >= 600
    val isTwoPaneTabletWithGroups = configuration.screenWidthDp >= 720 && uiState.groups.isNotEmpty()
    var useTripHubV2View by rememberSaveable { mutableStateOf(true) }
    val isImmersiveTripHubOpen =
        currentTab == SplitMateTab.LEDGERS && uiState.openedGroupDetailId != null && useTripHubV2View
    val isTripHubCanvasVisible =
        currentTab == SplitMateTab.LEDGERS && useTripHubV2View &&
            (uiState.openedGroupDetailId != null || isTwoPaneTabletWithGroups)

    var isToolbarExpanded by remember {
        mutableStateOf(true)
    }
    LaunchedEffect(currentTab, uiState.openedGroupDetailId) {
        isToolbarExpanded = true
    }

    val activeFabPalette = DesignSystemBindings.activePalette
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .floatingToolbarVerticalNestedScroll(
                expanded = isToolbarExpanded,
                onExpand = { isToolbarExpanded = true },
                onCollapse = { isToolbarExpanded = false }
            ),
        containerColor = animatedScreenBg,
        floatingActionButton = {
            AnimatedVisibility(
                visible = isMediumOrExpandedWindow && (
                    (currentTab == SplitMateTab.LEDGERS && !isTripHubCanvasVisible) ||
                        currentTab == SplitMateTab.AUDIT
                    ),
                enter = slideInVertically(
                    animationSpec = SplitMateMotion.defaultSpatial(),
                    initialOffsetY = { it }
                ) + fadeIn(animationSpec = SplitMateMotion.defaultEffects()),
                exit = slideOutVertically(
                    animationSpec = SplitMateMotion.defaultSpatial(),
                    targetOffsetY = { it }
                ) + fadeOut(animationSpec = SplitMateMotion.defaultEffects())
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.navigateToSubFlow(
                            targetTabName = SplitMateTab.SPLIT.name,
                            originGroupDetailId = uiState.openedGroupDetailId
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.ElectricBolt,
                            contentDescription = "Log Expense",
                            tint = activeFabPalette.onPrimary
                        )
                    },
                    text = {
                        Text(
                            text = "Log Expense",
                            fontWeight = FontWeight.ExtraBold,
                            color = activeFabPalette.onPrimary,
                            fontFamily = SplitMateTheme.FontRounded
                        )
                    },
                    containerColor = activeFabPalette.primary,
                    contentColor = activeFabPalette.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 10.dp
                    ),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                )
            }
        },
        bottomBar = {
            if (!isMediumOrExpandedWindow) {
                AnimatedVisibility(
                    visible = !isImmersiveTripHubOpen &&
                        currentTab != SplitMateTab.SPLIT,
                    enter = slideInVertically(
                        animationSpec = SplitMateMotion.defaultSpatial(),
                        initialOffsetY = { it }
                    ) + fadeIn(animationSpec = SplitMateMotion.defaultEffects()),
                    exit = slideOutVertically(
                        animationSpec = SplitMateMotion.defaultSpatial(),
                        targetOffsetY = { it }
                    ) + fadeOut(animationSpec = SplitMateMotion.defaultEffects())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                            .testTag("HorizontalFloatingToolbar")
                    ) {
                        SplitMateBottomNavigationBar(
                            selectedTab = currentTab,
                            expanded = isToolbarExpanded,
                            onTabSelected = { tab -> viewModel.selectTab(tab.name) },
                            onQuickSplitClick = {
                                viewModel.navigateToSubFlow(
                                    targetTabName = SplitMateTab.SPLIT.name,
                                    originGroupDetailId = uiState.openedGroupDetailId
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // v2.3.2 inset fix: the immersive Trip Hub renders its own LargeTopAppBar, which handles the
        // status-bar inset (and paints its container behind it). Applying the root innerPadding here as
        // well produced a double status-bar gap, so it is skipped for the single-pane Trip Hub. Any top
        // padding that IS applied is consumed so nested Scaffolds/TopAppBars never re-apply it.
        val tripHubOwnsStatusBar = isImmersiveTripHubOpen && !isTwoPaneTabletWithGroups
        val appliedTopInsetPadding = if (tripHubOwnsStatusBar) 0.dp else innerPadding.calculateTopPadding()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = appliedTopInsetPadding)
                .consumeWindowInsets(PaddingValues(top = appliedTopInsetPadding))
                .imePadding()
        ) {
            if (isMediumOrExpandedWindow && currentTab != SplitMateTab.SPLIT) {
                ExpressiveNavigationRail(
                    selectedTab = currentTab,
                    containerColor = animatedScreenBg,
                    onTabSelected = { tab -> viewModel.selectTab(tab.name) }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (currentTab) {
                SplitMateTab.LEDGERS -> LedgersDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToSplit = {
                        viewModel.navigateToSubFlow(
                            targetTabName = SplitMateTab.SPLIT.name,
                            originGroupDetailId = uiState.openedGroupDetailId
                        )
                    },
                    onNavigateToSettle = {
                        viewModel.navigateToSubFlow(
                            targetTabName = SplitMateTab.SETTLE.name,
                            originGroupDetailId = uiState.openedGroupDetailId
                        )
                    },
                    onNavigateToPnrSplit = {
                        if (uiState.groups.isNotEmpty()) {
                            activeReviewPnr = ""
                            showPnrReviewScreen = true
                        }
                    },
                    onUploadFlightPdf = {
                        if (uiState.groups.isNotEmpty()) {
                            flightPdfPickerLauncher.launch("application/pdf")
                        }
                    },
                    onOpenPnrWithTicket = { pnr ->
                        if (uiState.groups.isNotEmpty()) {
                            openPnrOrFlightTicket(pnr)
                        }
                    },
                    onAvatarSettingsClick = onOpenSettings,
                    useTripHubV2View = useTripHubV2View,
                    onToggleTripHubView = { useTripHubV2View = it }
                )
                SplitMateTab.SPLIT -> QuickExpenseScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        if (uiState.returnToGroupDetailId != null) {
                            viewModel.finishSubFlowToGroupDetail(uiState.returnToGroupDetailId)
                        } else {
                            viewModel.selectTab(SplitMateTab.LEDGERS.name)
                        }
                    },
                    onOpenPnrDirectSplit = {
                        if (uiState.groups.isNotEmpty()) {
                            activeReviewPnr = ""
                            showPnrReviewScreen = true
                        } else {
                            viewModel.selectTab(SplitMateTab.LEDGERS.name)
                        }
                    },
                    onSaveSplit = { _, _ ->
                        // Return directly inside the group where the expense was logged!
                        val loggedGroupId = viewModel.uiState.value.activeGroup?.groupId
                        viewModel.finishSubFlowToGroupDetail(loggedGroupId)
                    }
                )
                SplitMateTab.SETTLE -> GreedySettlementScreen(viewModel = viewModel)
                SplitMateTab.AUDIT -> AuditVaultScreen(
                    viewModel = viewModel,
                    onOpenPnrWithTicket = { groupId, pnr ->
                        viewModel.selectActiveGroup(groupId)
                        openPnrOrFlightTicket(pnr)
                    }
                )
            }
            }
        }
    }
}

@Composable
fun ExpressiveNavigationRail(
    selectedTab: SplitMateTab,
    containerColor: Color,
    onTabSelected: (SplitMateTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        containerColor = containerColor,
        contentColor = SplitMateTheme.PrimaryDark,
        modifier = modifier.testTag("AdaptiveNavigationRail")
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        SplitMateTab.values().filter { it.showInPrimaryNav }.forEach { tab ->
            val isSelected = selectedTab == tab
            NavigationRailItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = SplitMateTheme.FontRounded
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = SplitMateTheme.SageText,
                    selectedTextColor = SplitMateTheme.PrimaryDark,
                    indicatorColor = SplitMateTheme.AccentSage,
                    unselectedIconColor = SplitMateTheme.TextSecondary,
                    unselectedTextColor = SplitMateTheme.TextSecondary
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SplitMateBottomNavigationBar(
    selectedTab: SplitMateTab,
    expanded: Boolean = true,
    onTabSelected: (SplitMateTab) -> Unit,
    onQuickSplitClick: () -> Unit = {}
) {
    val activePalette = DesignSystemBindings.activePalette
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        HorizontalFloatingToolbar(
            expanded = expanded,
            floatingActionButton = {
                FloatingToolbarDefaults.StandardFloatingActionButton(
                    onClick = onQuickSplitClick,
                    containerColor = activePalette.primary,
                    contentColor = activePalette.onPrimary,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ElectricBolt,
                        contentDescription = "Quick Split",
                        tint = activePalette.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        ) {
            SplitMateTab.values().filter { it.showInPrimaryNav }.forEach { tab ->
                val isSelected = selectedTab == tab
                val tabCornerRadius by animateDpAsState(
                    targetValue = if (isSelected) 16.dp else 28.dp,
                    animationSpec = SplitMateMotion.fastSpatial(),
                    label = "navTabCornerMorph"
                )
                val tabPillBg by animateColorAsState(
                    targetValue = if (isSelected) SplitMateTheme.AccentSage else Color.Transparent,
                    animationSpec = SplitMateMotion.fastEffects(),
                    label = "navTabPillBg"
                )
                Surface(
                    onClick = { onTabSelected(tab) },
                    shape = RoundedCornerShape(tabCornerRadius),
                    color = tabPillBg,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .sizeIn(minWidth = 64.dp, minHeight = 48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) activePalette.onPrimaryContainer else SplitMateTheme.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        AnimatedVisibility(visible = isSelected) {
                            Text(
                                text = "  ${tab.label}",
                                color = activePalette.onPrimaryContainer,
                                fontFamily = SplitMateTheme.FontRounded,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// TAB 1: LEDGERS (Dashboard Screen + Inside-Group Detail View)
// ==============================================================================
@Composable
fun LedgersDashboardScreen(
    viewModel: SplitMateViewModel,
    onNavigateToSplit: () -> Unit = {},
    onNavigateToSettle: () -> Unit = {},
    onNavigateToPnrSplit: () -> Unit = {},
    onUploadFlightPdf: () -> Unit = {},
    onOpenPnrWithTicket: (String) -> Unit = {},
    onAvatarSettingsClick: () -> Unit = {},
    useTripHubV2View: Boolean = true,
    onToggleTripHubView: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val activeGroups by viewModel.activeGroups.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var showJoinByCodeDialog by remember { mutableStateOf(false) }
    var editingFriend by remember { mutableStateOf<GroupMemberEntity?>(null) }
    val openedGroupDetailId = uiState.openedGroupDetailId
    var expandedExpenseId by remember { mutableStateOf<String?>(null) }
    var editingExpense by remember { mutableStateOf<com.splitmate.app.data.ExpenseEntity?>(null) }
    var deletingExpense by remember { mutableStateOf<com.splitmate.app.data.ExpenseEntity?>(null) }
    var showAddContactsToExistingGroupSheet by remember { mutableStateOf(false) }
    var showClassicSyncSheet by remember { mutableStateOf(false) }

    // Intercept system Back when viewing inside a specific Group so user returns to All Groups list instead of exiting the app!
    androidx.activity.compose.BackHandler(
        enabled = openedGroupDetailId != null || showClassicSyncSheet || showAddContactsToExistingGroupSheet || showJoinByCodeDialog
    ) {
        if (showJoinByCodeDialog) {
            showJoinByCodeDialog = false
        } else if (showClassicSyncSheet) {
            showClassicSyncSheet = false
        } else if (deletingExpense != null) {
            deletingExpense = null
        } else if (editingExpense != null) {
            editingExpense = null
        } else if (editingFriend != null) {
            editingFriend = null
        } else if (showAddContactsToExistingGroupSheet) {
            showAddContactsToExistingGroupSheet = false
        } else {
            viewModel.closeGroupDetail()
        }
    }

    deletingExpense?.let { expToDelete ->
        val cleanExpName = extractTravelTicketFromTitle(expToDelete.title)?.cleanTitle ?: expToDelete.title
        val formattedDeleteAmt = formatIndianRupeesFromCents(expToDelete.totalAmountCents)
        AlertDialog(
            onDismissRequest = { deletingExpense = null },
            containerColor = SplitMateTheme.SurfaceWhite,
            title = {
                Text(
                    text = "Delete Expense?",
                    fontFamily = SplitMateTheme.FontDisplay,
                    fontWeight = FontWeight.ExtraBold,
                    color = SplitMateTheme.PrimaryDark
                )
            },
            text = {
                Text(
                    text = "Remove \"$cleanExpName\" ($formattedDeleteAmt) from this group? All member balances will recalculate immediately.",
                    fontFamily = SplitMateTheme.FontRounded,
                    fontSize = 13.sp,
                    color = SplitMateTheme.TextSecondary
                )
            },
            confirmButton = {
                Surface(
                    onClick = {
                        val id = expToDelete.expenseId
                        if (expandedExpenseId == id) expandedExpenseId = null
                        if (editingExpense?.expenseId == id) editingExpense = null
                        viewModel.rollbackExpense(id)
                        deletingExpense = null
                    },
                    shape = SplitMateTheme.RadiusBadge,
                    color = SplitMateTheme.TerracottaSurface,
                    border = BorderStroke(1.dp, SplitMateTheme.TerracottaText)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = SplitMateTheme.TerracottaText,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Delete Expense",
                            fontFamily = SplitMateTheme.FontRounded,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = SplitMateTheme.TerracottaText
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingExpense = null }) {
                    Text(
                        text = "Cancel",
                        fontFamily = SplitMateTheme.FontRounded,
                        fontWeight = FontWeight.Bold,
                        color = SplitMateTheme.TextSecondary
                    )
                }
            }
        )
    }

    editingExpense?.let { exp ->
        val expGroupMembers = uiState.members.filter { it.groupId == exp.groupId }
        val expExistingSplits = uiState.splits.filter { it.expenseId == exp.expenseId && it.finalOwedCents > 0L }
        EditLoggedExpenseDialog(
            expense = exp,
            groupMembers = expGroupMembers,
            initialSplitMemberIds = expExistingSplits.map { it.memberId }.toSet().ifEmpty { expGroupMembers.map { it.memberId }.toSet() },
            onDismiss = { editingExpense = null },
            onSave = { newTitle, newAmountRupees, newPayerId, updatedSplitMemberIds ->
                viewModel.editExistingExpense(
                    expenseId = exp.expenseId,
                    newTitle = newTitle,
                    newTotalRupees = newAmountRupees,
                    newPayerId = newPayerId,
                    selectedMemberIds = updatedSplitMemberIds
                )
                editingExpense = null
            }
        )
    }

    val deviceContacts by viewModel.deviceContacts.collectAsStateWithLifecycle()
    val isLoadingContacts by viewModel.isLoadingContacts.collectAsStateWithLifecycle()

    val addToGroupPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.loadDeviceContacts(context)
            showAddContactsToExistingGroupSheet = true
        }
    }

    val isNegativeBalance = totalBalance.startsWith("-")

    editingFriend?.let { friend ->
        val editableGroupMembers = uiState.members.filter {
            it.groupId == friend.groupId && !it.isCurrentUser
        }.ifEmpty { listOf(friend) }
        EditFriendUpiDialog(
            member = friend,
            allGroupMembers = editableGroupMembers,
            onSelectMember = { editingFriend = it },
            onDismiss = { editingFriend = null },
            onSave = { newName, newUpi, newAvatarSeed ->
                viewModel.updateFriendUpi(friend.memberId, newName, newUpi, newAvatarSeed)
                editingFriend = null
            },
            onSaveAll = { batchUpdates ->
                viewModel.updateAllGroupMembers(batchUpdates)
                editingFriend = null
            }
        )
    }

    var editingGroupTarget by remember { mutableStateOf<com.splitmate.app.data.ExpenseGroupEntity?>(null) }

    editingGroupTarget?.let { targetGroup ->
        var draftGroupName by remember(targetGroup.groupId) { mutableStateOf(targetGroup.name.toSmartTitleCase()) }
        var draftIconName by remember(targetGroup.groupId) { mutableStateOf(targetGroup.iconName.ifBlank { "Flight" }) }
        val titleCasePreview = draftGroupName.toSmartTitleCase()

        AlertDialog(
            onDismissRequest = { editingGroupTarget = null },
            containerColor = SplitMateTheme.SurfaceWhite,
            title = {
                Text(
                    text = "Edit Group Title",
                    fontFamily = SplitMateTheme.FontDisplay,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    color = SplitMateTheme.PrimaryDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = draftGroupName,
                        onValueChange = { draftGroupName = it },
                        label = { Text("Group Title (Auto Title-Case)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (titleCasePreview.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SplitMateTheme.SageSurface,
                            border = BorderStroke(1.dp, SplitMateTheme.AccentSage),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "DISPLAY TITLE",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp,
                                    color = SplitMateTheme.SageText
                                )
                                Text(
                                    text = titleCasePreview,
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SplitMateTheme.PrimaryDark
                                )
                            }
                        }
                    }
                    Text(
                        text = "Group Category Icon",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SplitMateTheme.TextSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(GroupCategoryIcons) { option ->
                            val iconKey = option.id
                            val iconVector = option.icon
                            val isSelected = draftIconName.equals(iconKey, ignoreCase = true)
                            Surface(
                                onClick = { draftIconName = iconKey },
                                shape = CircleShape,
                                color = if (isSelected) SplitMateTheme.AccentSage else SplitMateTheme.SurfaceMuted,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.BorderLight
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = option.label,
                                        tint = if (isSelected) Color(0xFF23201E) else SplitMateTheme.TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = draftGroupName.toSmartTitleCase()
                        if (clean.isNotEmpty()) {
                            viewModel.renameGroup(targetGroup.groupId, clean, draftIconName)
                            editingGroupTarget = null
                        }
                    },
                    enabled = draftGroupName.trim().isNotEmpty(),
                    shape = SplitMateTheme.RadiusBadge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SplitMateTheme.PrimaryDark,
                        contentColor = SplitMateTheme.ScreenBg
                    )
                ) {
                    Text("Save Name", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingGroupTarget = null }) {
                    Text("Cancel", color = SplitMateTheme.TextSecondary)
                }
            }
        )
    }

    val isTwoPaneListDetailViewport = LocalConfiguration.current.screenWidthDp >= 720 && uiState.groups.isNotEmpty()
    val currentOpenedGroup = uiState.groups.find { it.groupId == openedGroupDetailId }
    val activeDetailTargetGroup =
        currentOpenedGroup ?: if (isTwoPaneListDetailViewport) uiState.groups.firstOrNull() else null
    if (showAddContactsToExistingGroupSheet && activeDetailTargetGroup != null) {
        ContactPickerBottomSheet(
            contacts = deviceContacts,
            isLoading = isLoadingContacts,
            multiSelect = true,
            title = "Add Contacts to ${activeDetailTargetGroup.name}",
            subtitle = "Select contacts to add to this group",
            onDismissRequest = { showAddContactsToExistingGroupSheet = false },
            onConfirmSelected = { selected ->
                viewModel.addContactsToGroup(activeDetailTargetGroup.groupId, selected)
                showAddContactsToExistingGroupSheet = false
            }
        )
    }

    if (showClassicSyncSheet && activeDetailTargetGroup != null) {
        com.splitmate.app.ui.dialogs.TripSyncAndPerspectiveSheet(
            viewModel = viewModel,
            groupId = activeDetailTargetGroup.groupId,
            groupName = activeDetailTargetGroup.name,
            members = uiState.members.filter { it.groupId == activeDetailTargetGroup.groupId },
            onDismiss = { showClassicSyncSheet = false }
        )
    }

    val renderGroupDetailPane: @Composable (com.splitmate.app.data.ExpenseGroupEntity) -> Unit = { openedGroup ->
        val groupMembers = uiState.members.filter { it.groupId == openedGroup.groupId }
        val groupExpenses = uiState.expenses.filter { it.groupId == openedGroup.groupId }
        val totalGroupSpendCents = groupExpenses.sumOf { it.totalAmountCents }
        val groupIcon = resolveGroupCategoryIcon(openedGroup.iconName, openedGroup.name)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back to All Groups Header — Keep Settle Up 100% visible at all times
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { viewModel.closeGroupDetail() },
                            shape = SplitMateTheme.RadiusBadge,
                            color = SplitMateTheme.SurfaceWhite,
                            border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back to All Groups",
                                    tint = SplitMateTheme.PrimaryDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "All Groups",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = SplitMateTheme.PrimaryDark
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.READ_CONTACTS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
                                        viewModel.loadDeviceContacts(context)
                                        showAddContactsToExistingGroupSheet = true
                                    } else {
                                        addToGroupPermLauncher.launch(Manifest.permission.READ_CONTACTS)
                                    }
                                },
                                shape = SplitMateTheme.RadiusBadge,
                                color = SplitMateTheme.SageSurface,
                                border = BorderStroke(1.dp, SplitMateTheme.AccentSage),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .defaultMinSize(minHeight = 48.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PersonAdd,
                                        contentDescription = "Add Contact",
                                        tint = SplitMateTheme.SageText,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "+ Contact",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = SplitMateTheme.SageText
                                    )
                                }
                            }

                            Surface(
                                onClick = onNavigateToSettle,
                                shape = SplitMateTheme.RadiusBadge,
                                color = SplitMateTheme.PrimaryDark,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .defaultMinSize(minHeight = 48.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SwapHoriz,
                                        contentDescription = "Settle Up",
                                        tint = SplitMateTheme.ScreenBg,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Settle Up",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = SplitMateTheme.ScreenBg
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.splitmate.app.ui.dialogs.PerspectiveAndSyncHeaderPill(
                            members = groupMembers,
                            onClick = { showClassicSyncSheet = true }
                        )

                        Surface(
                            onClick = { onToggleTripHubView(true) },
                            shape = SplitMateTheme.RadiusBadge,
                            color = SplitMateTheme.SurfaceWhite,
                            border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SpaceDashboard,
                                    contentDescription = "Switch to Trip Hub v2.0",
                                    tint = SplitMateTheme.PrimaryDark,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Trip Hub v2.0",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = SplitMateTheme.PrimaryDark
                                )
                            }
                        }
                    }
                }
            }

            // Group Hero Summary Card
            item {
                Card(
                    shape = SplitMateTheme.RadiusHero,
                    colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusHero)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(MaterialShapes.Cookie9Sided.toShape())
                                        .background(SplitMateTheme.SageSurface)
                                        .clickable { editingGroupTarget = openedGroup },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(groupIcon, contentDescription = "Edit Group Icon", tint = SplitMateTheme.SageText, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { editingGroupTarget = openedGroup }
                                ) {
                                    Text(
                                        text = openedGroup.name.toSmartTitleCase(),
                                        fontFamily = SplitMateTheme.FontDisplay,
                                        fontSize = 25.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.6).sp,
                                        lineHeight = 29.sp,
                                        color = SplitMateTheme.PrimaryDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${groupMembers.size} members · ${groupExpenses.size} expenses",
                                        fontFamily = SplitMateTheme.FontDisplay,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SplitMateTheme.TextSecondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Spend",
                                    fontSize = 11.sp,
                                    color = SplitMateTheme.TextSecondary
                                )
                                Text(
                                    text = formatIndianRupeesFromCents(totalGroupSpendCents),
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SplitMateTheme.PrimaryDark,
                                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = SplitMateTheme.BorderLight)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Group Members Horizontal Strip (Tap any member to edit/link contact)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Group Members (Tap to edit)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitMateTheme.PrimaryDark
                            )
                            val firstEditable = groupMembers.firstOrNull { !it.isCurrentUser }
                            if (firstEditable != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.clickable { editingFriend = firstEditable }
                                ) {
                                    Text(
                                        text = "Manage All",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SplitMateTheme.SageText
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = SplitMateTheme.SageText,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(groupMembers, key = { it.memberId }) { mbr ->
                                val isMbrOnline = uiState.isMemberOnline(mbr)
                                Surface(
                                    onClick = {
                                        if (!mbr.isCurrentUser) {
                                            editingFriend = mbr
                                        } else {
                                            onAvatarSettingsClick()
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = SplitMateTheme.SurfaceMuted,
                                    border = BorderStroke(1.dp, SplitMateTheme.BorderLight)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AvatarToken(
                                            initials = mbr.avatarSeed,
                                            bg = SplitMateTheme.AccentSage,
                                            textColor = Color(0xFF23201E),
                                            size = 30,
                                            isOnline = isMbrOnline
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            val isDeclinedMbr = mbr.inviteStatus.equals("DECLINED", ignoreCase = true)
                                            Text(
                                                text = if (mbr.isCurrentUser) "${mbr.name} (You)" else mbr.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SplitMateTheme.PrimaryDark
                                            )
                                            val phone = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(mbr.userPhone, mbr.upiId)
                                            Text(
                                                text = when {
                                                    isDeclinedMbr -> "Declined Invite"
                                                    isMbrOnline -> "● Online now"
                                                    phone.length == 10 -> "+91 $phone"
                                                    mbr.isCurrentUser -> "Group Admin"
                                                    else -> "Tap to link phone"
                                                },
                                                fontSize = 10.sp,
                                                color = when {
                                                    isDeclinedMbr -> SplitMateTheme.TerracottaText
                                                    isMbrOnline -> Color(0xFF15803D)
                                                    phone.length == 10 || mbr.isCurrentUser -> SplitMateTheme.SageText
                                                    else -> SplitMateTheme.TerracottaText
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Declined Member Share Reassignment Banner (Classic Ledger Parity with Trip Hub v2.0)
            val classicGroupExpenseIds = groupExpenses.map { it.expenseId }.toSet()
            val declinedMembersWithShares = groupMembers
                .filter { it.inviteStatus.equals("DECLINED", ignoreCase = true) }
                .mapNotNull { declinedMbr ->
                    val owedCents = uiState.splits.sumOf { split ->
                        if (split.expenseId in classicGroupExpenseIds && split.memberId == declinedMbr.memberId) {
                            split.finalOwedCents
                        } else {
                            0L
                        }
                    }
                    if (owedCents > 0L) declinedMbr to owedCents else null
                }
            items(declinedMembersWithShares, key = { "classic_declined_${it.first.memberId}" }) { (declinedMember, owedCents) ->
                Surface(
                    shape = SplitMateTheme.RadiusCard,
                    color = SplitMateTheme.TerracottaSurface,
                    border = BorderStroke(1.dp, SplitMateTheme.TerracottaText.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonOff,
                                contentDescription = "Declined Member",
                                tint = SplitMateTheme.TerracottaText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${declinedMember.name} declined this group invite",
                                fontFamily = SplitMateTheme.FontDisplay,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = SplitMateTheme.TerracottaText
                            )
                        }
                        Text(
                            text = "Still assigned ₹${String.format(Locale.US, "%,.2f", owedCents / 100.0)} across group expenses.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SplitMateTheme.PrimaryDark
                        )
                        Button(
                            onClick = {
                                viewModel.reassignDeclinedMemberSharesEqually(
                                    context = context,
                                    groupId = openedGroup.groupId,
                                    declinedMemberId = declinedMember.memberId
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SplitMateTheme.PrimaryDark,
                                contentColor = SplitMateTheme.ScreenBg
                            ),
                            shape = SplitMateTheme.RadiusButton,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text(
                                text = "Reassign ${declinedMember.name}'s Share Equally",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Expenses Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Group Expenses (${groupExpenses.size})",
                        fontFamily = SplitMateTheme.FontDisplay,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SplitMateTheme.PrimaryDark
                    )
                    Surface(
                        onClick = onNavigateToSplit,
                        shape = SplitMateTheme.RadiusBadge,
                        color = SplitMateTheme.PrimaryDark,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(
                            text = "+ Log Expense",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitMateTheme.ScreenBg,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // UNIFIED 3D FLIP TRAVEL PASS CARD (Train Pass ⇄ Flight Pass on Button Click — NOT Stacked Cards)
            item {
                val allTravelTicketsInGroup = remember(groupExpenses) {
                    groupExpenses.mapNotNull { exp ->
                        val parsed = extractTravelTicketFromTitle(exp.title)
                        if (parsed != null && parsed.pnr.isNotBlank()) exp to parsed else null
                    }
                }
                val trainPnrExpensesInGroup = remember(allTravelTicketsInGroup) {
                    allTravelTicketsInGroup.filter { (exp, ticket) ->
                        val pnr = ticket.pnr.trim()
                        pnr.length == 10 && pnr.all { it.isDigit() } && !isFlightTicketExpense(exp.title, ticket)
                    }
                }
                val flightPnrExpensesInGroup = remember(allTravelTicketsInGroup) {
                    allTravelTicketsInGroup.filter { (exp, ticket) ->
                        isFlightTicketExpense(exp.title, ticket)
                    }
                }

                var isFlightSide by remember(
                    openedGroup.groupId,
                    flightPnrExpensesInGroup.size,
                    trainPnrExpensesInGroup.size
                ) {
                    mutableStateOf(flightPnrExpensesInGroup.isNotEmpty() && trainPnrExpensesInGroup.isEmpty())
                }
                var rawHorizontalDragPx by remember { mutableFloatStateOf(0f) }
                val travelPassHaptic = LocalHapticFeedback.current

                // Bidirectional drag progress (0°..175° regardless of whether user swipes <- Left or -> Right)
                val dragProgressDeg = (kotlin.math.abs(rawHorizontalDragPx) * 0.68f).coerceIn(0f, 175f)
                val targetFlipDeg = if (isFlightSide) {
                    (180f - dragProgressDeg).coerceIn(0f, 180f)
                } else {
                    dragProgressDeg.coerceIn(0f, 180f)
                }

                val flipRotationY by animateFloatAsState(
                    targetValue = targetFlipDeg,
                    animationSpec = spring(
                        dampingRatio = 0.74f,
                        stiffness = 340f
                    ),
                    label = "TravelPassCardFlipY"
                )
                val showingFlightFace = flipRotationY >= 90f

                // Fire a crisp mechanical haptic tick right as the cardstock crosses the 90-degree perpendicular plane
                LaunchedEffect(showingFlightFace) {
                    travelPassHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                val cardBgColor = if (showingFlightFace) {
                    if (SplitMateTheme.isDark) Color(0xFF1B1936) else Color(0xFFEEF2FF)
                } else {
                    if (SplitMateTheme.isDark) Color(0xFF1F2B16) else Color(0xFFEAF3D5)
                }
                val cardBorderColor = if (showingFlightFace) {
                    if (SplitMateTheme.isDark) Color(0xFF3F3A82) else Color(0xFFC7D2FE)
                } else {
                    if (SplitMateTheme.isDark) Color(0xFF3D5428) else Color(0xFFC5DCA0)
                }

                Surface(
                    shape = SplitMateTheme.RadiusCard,
                    color = cardBgColor,
                    border = BorderStroke(1.2.dp, cardBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(isFlightSide) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (kotlin.math.abs(rawHorizontalDragPx) * 0.68f > 28f) {
                                        isFlightSide = !isFlightSide
                                    }
                                    rawHorizontalDragPx = 0f
                                },
                                onDragCancel = { rawHorizontalDragPx = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    rawHorizontalDragPx = (rawHorizontalDragPx + dragAmount).coerceIn(-320f, 320f)
                                }
                            )
                        }
                        .graphicsLayer {
                            rotationY = flipRotationY
                            cameraDistance = 15f * density
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                // Un-mirror back face past 90 degrees so text & buttons remain crisp and upright
                                if (showingFlightFace) {
                                    rotationY = 180f
                                }
                            }
                    ) {
                        if (!showingFlightFace) {
                            // FRONT FACE: Train Pass (10-digit IRCTC PNRs)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF264010)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Train,
                                                contentDescription = null,
                                                tint = Color(0xFFD7E8B6),
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Train Pass",
                                                fontFamily = SplitMateTheme.FontDisplay,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = SplitMateTheme.PrimaryDark
                                            )
                                            Text(
                                                text = if (trainPnrExpensesInGroup.isEmpty()) {
                                                    "10-digit IRCTC PNR · Split by berth"
                                                } else {
                                                    "${trainPnrExpensesInGroup.size} train pass(es) logged"
                                                },
                                                fontSize = 11.sp,
                                                color = SplitMateTheme.TextSecondary
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Button to Flip to Flight Pass
                                        Surface(
                                            onClick = { isFlightSide = true },
                                            shape = SplitMateTheme.RadiusBadge,
                                            color = if (SplitMateTheme.isDark) Color(0xFF24214A) else Color(0xFFEEF2FF),
                                            border = BorderStroke(1.dp, if (SplitMateTheme.isDark) Color(0xFF4E48A6) else Color(0xFFA5B4FC)),
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.FlightTakeoff,
                                                    contentDescription = "Flip to Flight Pass",
                                                    tint = if (SplitMateTheme.isDark) Color(0xFFDCE3FD) else Color(0xFF3730A3),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "Flight (${flightPnrExpensesInGroup.size})",
                                                    fontFamily = SplitMateTheme.FontRounded,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.sp,
                                                    color = if (SplitMateTheme.isDark) Color(0xFFDCE3FD) else Color(0xFF3730A3)
                                                )
                                                Icon(
                                                    imageVector = Icons.Rounded.SwapHoriz,
                                                    contentDescription = null,
                                                    tint = if (SplitMateTheme.isDark) Color(0xFFDCE3FD) else Color(0xFF3730A3),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }

                                        Surface(
                                            onClick = onNavigateToPnrSplit,
                                            shape = SplitMateTheme.RadiusBadge,
                                            color = Color(0xFF264010),
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Text(
                                                text = "+ PNR",
                                                fontFamily = SplitMateTheme.FontRounded,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }

                                if (trainPnrExpensesInGroup.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        trainPnrExpensesInGroup.forEach { (exp, ticket) ->
                                            val formattedFare = "₹${String.format(Locale.US, "%.0f", exp.totalAmountCents / 100.0)}"
                                            Surface(
                                                onClick = { onOpenPnrWithTicket(ticket.pnr) },
                                                shape = SplitMateTheme.RadiusBadge,
                                                color = SplitMateTheme.SurfaceWhite,
                                                border = BorderStroke(1.dp, SplitMateTheme.BorderLight)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Train,
                                                        contentDescription = null,
                                                        tint = SplitMateTheme.SageText,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "PNR ${ticket.pnr} (${ticket.fromStation.ifBlank { "ORG" }} - ${ticket.toStation.ifBlank { "DST" }} · $formattedFare)",
                                                        fontFamily = SplitMateTheme.FontRounded,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = SplitMateTheme.PrimaryDark
                                                    )
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                                        contentDescription = null,
                                                        tint = SplitMateTheme.SageText,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // BACK FACE: Flight Pass (6-char Airline PNRs)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (SplitMateTheme.isDark) Color(0xFF282552) else Color(0xFF2B2768)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.FlightTakeoff,
                                                contentDescription = null,
                                                tint = if (SplitMateTheme.isDark) Color(0xFFDCE3FD) else Color(0xFFEEF2FF),
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Flight Pass",
                                                fontFamily = SplitMateTheme.FontDisplay,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = if (SplitMateTheme.isDark) Color(0xFFE6EAFF) else Color(0xFF1F1C4D)
                                            )
                                            Text(
                                                text = if (flightPnrExpensesInGroup.isEmpty()) {
                                                    "6-char airline PNR · Auto-match passengers"
                                                } else {
                                                    "${flightPnrExpensesInGroup.size} flight pass(es) logged"
                                                },
                                                fontSize = 11.sp,
                                                color = if (SplitMateTheme.isDark) Color(0xFFB5BEEC) else Color(0xFF433E85)
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Button to Flip to Train Pass
                                        Surface(
                                            onClick = { isFlightSide = false },
                                            shape = SplitMateTheme.RadiusBadge,
                                            color = if (SplitMateTheme.isDark) Color(0xFF1F2B16) else Color(0xFFEAF3D5),
                                            border = BorderStroke(1.dp, if (SplitMateTheme.isDark) Color(0xFF3D5428) else Color(0xFFC5DCA0))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Train,
                                                    contentDescription = "Flip to Train Pass",
                                                    tint = SplitMateTheme.SageText,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "Train (${trainPnrExpensesInGroup.size})",
                                                    fontFamily = SplitMateTheme.FontRounded,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.sp,
                                                    color = SplitMateTheme.SageText
                                                )
                                                Icon(
                                                    imageVector = Icons.Rounded.SwapHoriz,
                                                    contentDescription = null,
                                                    tint = SplitMateTheme.SageText,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }

                                        Surface(
                                            onClick = onUploadFlightPdf,
                                            shape = SplitMateTheme.RadiusBadge,
                                            color = if (SplitMateTheme.isDark) Color(0xFF282552) else Color(0xFF2B2768),
                                            border = BorderStroke(1.dp, if (SplitMateTheme.isDark) Color(0xFF5650B8) else Color(0xFF4B459E))
                                        ) {
                                            Text(
                                                text = "+ PDF",
                                                fontFamily = SplitMateTheme.FontRounded,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                color = if (SplitMateTheme.isDark) Color(0xFFDCE3FD) else Color(0xFFEEF2FF),
                                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }

                                if (flightPnrExpensesInGroup.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        flightPnrExpensesInGroup.forEach { (exp, ticket) ->
                                            val formattedFare = "₹${String.format(Locale.US, "%.0f", exp.totalAmountCents / 100.0)}"
                                            Surface(
                                                onClick = { onOpenPnrWithTicket("EXPENSE:${exp.expenseId}") },
                                                shape = SplitMateTheme.RadiusBadge,
                                                color = if (SplitMateTheme.isDark) Color(0xFF24214A) else Color(0xFFFFFFFF),
                                                border = BorderStroke(1.dp, if (SplitMateTheme.isDark) Color(0xFF4E48A6) else Color(0xFFA5B4FC))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.FlightTakeoff,
                                                        contentDescription = null,
                                                        tint = if (SplitMateTheme.isDark) Color(0xFFA5B4FC) else Color(0xFF3730A3),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "FLIGHT ${ticket.pnr} (${ticket.fromStation.ifBlank { "ORG" }} - ${ticket.toStation.ifBlank { "DST" }} · $formattedFare)",
                                                        fontFamily = SplitMateTheme.FontRounded,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = if (SplitMateTheme.isDark) Color(0xFFE6EAFF) else Color(0xFF1F1C4D)
                                                    )
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                                        contentDescription = null,
                                                        tint = if (SplitMateTheme.isDark) Color(0xFFA5B4FC) else Color(0xFF3730A3),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (groupExpenses.isEmpty()) {
                item {
                    Card(
                        shape = SplitMateTheme.RadiusCard,
                        colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = SplitMateTheme.SageText,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No expenses in ${openedGroup.name} yet",
                                fontFamily = SplitMateTheme.FontDisplay,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SplitMateTheme.PrimaryDark
                            )
                            Text(
                                text = "Tap + Log Expense to record your first shared bill.",
                                fontSize = 12.sp,
                                color = SplitMateTheme.TextSecondary
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(groupExpenses, key = { _, it -> it.expenseId }) { expenseIdx, expense ->
                    val payer = uiState.members.find { it.memberId == expense.payerId }
                    val isMePayer = payer?.isCurrentUser == true
                    val formattedTotal = formatIndianRupeesFromCents(
                        cents = expense.totalAmountCents,
                        includePlusSign = false,
                        currencySymbol = "₹"
                    )
                    val isExpanded = expandedExpenseId == expense.expenseId
                    val breakdown = remember(expense, groupMembers, uiState.splits) {
                        SplitMateViewModel.resolveExpenseSplitBreakdown(
                            expense = expense,
                            groupMembers = groupMembers,
                            allSplits = uiState.splits,
                            currencySymbol = "₹",
                            headerPrefix = "Split Breakdown"
                        )
                    }
                    val perPersonShare = breakdown.perPersonHeadlineShare
                    val expenseIslandShape = rememberAnimatedSegmentedIslandItemShape(
                        index = expenseIdx,
                        totalCount = groupExpenses.size,
                        isSelected = isExpanded,
                        outerCorner = 24.dp,
                        innerCorner = 6.dp
                    )

                    Card(
                        onClick = {
                            expandedExpenseId = if (isExpanded) null else expense.expenseId
                        },
                        shape = expenseIslandShape,
                        colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SplitMateTheme.BorderLight, expenseIslandShape)
                            .animateContentSize()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            val parsedTicketInGroup = remember(expense.title) { extractTravelTicketFromTitle(expense.title) }
                            val cleanTitleInGroup = cleanDisplayExpenseTitle(expense.title)
                            val isFlightCard = remember(expense.title, parsedTicketInGroup) {
                                isFlightTicketExpense(expense.title, parsedTicketInGroup)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (catBadgeBg, catBadgeTint) = resolveExpenseCategoryBadgeColors(expense.title, SplitMateTheme.isDark)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(catBadgeBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = resolveExpenseCategoryIcon(expense.title),
                                            contentDescription = null,
                                            tint = catBadgeTint
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = cleanTitleInGroup,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = SplitMateTheme.PrimaryDark
                                        )
                                        Text(
                                            // v2.3.3: PERSONAL / ON_BEHALF read naturally; SHARED unchanged.
                                            text = breakdown.paidBySubtitle(
                                                payerName = payer?.name ?: "You",
                                                sharedDetail = "$perPersonShare / person (${breakdown.splittingMembersCount} splitting)"
                                            ),
                                            fontFamily = SplitMateTheme.FontRounded,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 0.sp,
                                            fontSize = 12.sp,
                                            color = SplitMateTheme.TextSecondary
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = (if (isMePayer) "+" else "-") + formattedTotal,
                                        fontFamily = SplitMateTheme.FontDisplay,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                                        color = if (isMePayer) SplitMateTheme.SageText else SplitMateTheme.TerracottaText
                                    )
                                    val splitToggleTint = if (isFlightCard) {
                                        if (SplitMateTheme.isDark) Color(0xFFA5B4FC) else Color(0xFF3730A3)
                                    } else {
                                        SplitMateTheme.SageText
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = if (isExpanded) "Hide split" else "View split",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = splitToggleTint
                                        )
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                            contentDescription = null,
                                            tint = splitToggleTint,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            if (parsedTicketInGroup != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                CompactLedgerTicketStub(
                                    ticket = parsedTicketInGroup,
                                    totalAmountDisplay = formattedTotal,
                                    perPersonShareDisplay = perPersonShare,
                                    onInspectTactilePass = {
                                        if (isFlightCard || expense.expenseCategory.equals("FLIGHT", ignoreCase = true)) {
                                            onOpenPnrWithTicket("EXPENSE:${expense.expenseId}")
                                        } else if (parsedTicketInGroup.pnr.isNotBlank()) {
                                            onOpenPnrWithTicket(parsedTicketInGroup.pnr)
                                        } else {
                                            onNavigateToPnrSplit()
                                        }
                                    }
                                )
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = SplitMateTheme.BorderLight)
                                Spacer(modifier = Modifier.height(8.dp))
                                val includedRows = breakdown.rows.filter { it.isIncludedInSplit }
                                val assignedProgressRatio = 1f
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = breakdown.headerLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SplitMateTheme.PrimaryDark
                                    )
                                    Surface(
                                        shape = SplitMateTheme.RadiusBadge,
                                        color = SplitMateTheme.SageSurface
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(RoundedPolygonShape(MaterialShapes.Sunny))
                                                    .background(SplitMateTheme.SageText)
                                            )
                                            Text(
                                                text = "0.00¢ DRIFT • EVERY PENNY ACCOUNTED FOR",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = SplitMateTheme.SageText
                                            )
                                        }
                                    }
                                }
                                LinearWavyProgressIndicator(
                                    progress = { assignedProgressRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    color = SplitMateTheme.SageText,
                                    trackColor = SplitMateTheme.SageSurface,
                                    amplitude = 0f,
                                    wavelength = 22.dp,
                                    strokeWidth = 5.dp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                includedRows.forEach { row ->
                                    val rowMember = groupMembers.find { it.memberId == row.memberId }
                                    val rowAvatarSeed = rowMember?.avatarSeed?.ifBlank { row.displayName } ?: row.displayName
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            AvatarToken(
                                                initials = rowAvatarSeed,
                                                bg = SplitMateTheme.SageSurface,
                                                textColor = SplitMateTheme.SageText,
                                                size = 24
                                            )
                                            Text(
                                                text = row.displayName,
                                                fontSize = 12.sp,
                                                color = SplitMateTheme.TextSecondary
                                            )
                                        }
                                        Text(
                                            text = row.formattedShare,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SplitMateTheme.PrimaryDark
                                        )
                                    }
                                }
                            }

                            // Always-visible quick action footer (Edit + Delete) so mistaken expenses can be removed with 1 tap
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = SplitMateTheme.BorderLight.copy(alpha = 0.65f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = when {
                                            isFlightCard -> Icons.Rounded.FlightTakeoff
                                            parsedTicketInGroup != null -> Icons.Rounded.Train
                                            else -> Icons.AutoMirrored.Rounded.ReceiptLong
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = if (isFlightCard) {
                                            if (SplitMateTheme.isDark) Color(0xFFA5B4FC) else Color(0xFF3730A3)
                                        } else if (parsedTicketInGroup != null) {
                                            SplitMateTheme.SageText
                                        } else {
                                            SplitMateTheme.TextSecondary
                                        }
                                    )
                                    Text(
                                        text = when {
                                            isFlightCard -> "Flight Pass"
                                            parsedTicketInGroup != null -> "Train Pass"
                                            else -> "Shared Expense"
                                        },
                                        fontFamily = SplitMateTheme.FontRounded,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isFlightCard) {
                                            if (SplitMateTheme.isDark) Color(0xFFA5B4FC) else Color(0xFF3730A3)
                                        } else {
                                            SplitMateTheme.TextSecondary
                                        }
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        onClick = { editingExpense = expense },
                                        shape = SplitMateTheme.RadiusBadge,
                                        color = if (isFlightCard) {
                                            if (SplitMateTheme.isDark) Color(0xFF282552) else Color(0xFFEEF2FF)
                                        } else {
                                            SplitMateTheme.SageSurface
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = "Edit Expense",
                                                modifier = Modifier.size(13.dp),
                                                tint = if (isFlightCard) {
                                                    if (SplitMateTheme.isDark) Color(0xFFDCE3FD) else Color(0xFF3730A3)
                                                } else {
                                                    SplitMateTheme.SageText
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Edit",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isFlightCard) {
                                                    if (SplitMateTheme.isDark) Color(0xFFDCE3FD) else Color(0xFF3730A3)
                                                } else {
                                                    SplitMateTheme.SageText
                                                }
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = { deletingExpense = expense },
                                        shape = SplitMateTheme.RadiusBadge,
                                        color = SplitMateTheme.TerracottaSurface,
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Delete Expense",
                                                modifier = Modifier.size(13.dp),
                                                tint = SplitMateTheme.TerracottaText
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Delete",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SplitMateTheme.TerracottaText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val joinedGroupIds = remember(uiState.activeJoinedGroups) {
        uiState.activeJoinedGroups.map { it.groupId }.toSet()
    }
    val displayedActiveGroups = remember(activeGroups, joinedGroupIds) {
        if (joinedGroupIds.isEmpty() && uiState.pendingInviteGroups.isEmpty() && uiState.declinedInviteGroups.isEmpty()) {
            activeGroups
        } else {
            activeGroups.filter { it.groupId in joinedGroupIds }
        }
    }
    var upgradePhoneInput by rememberSaveable(uiState.userPhone) { mutableStateOf(uiState.userPhone) }
    var upgradeOtpInput by rememberSaveable { mutableStateOf("") }
    var showUpgradeOtpExpanded by rememberSaveable { mutableStateOf(false) }
    var upgradeFeedbackMsg by remember { mutableStateOf<String?>(null) }
    var isDeclinedDrawerExpanded by rememberSaveable { mutableStateOf(false) }

    val renderMasterGroupListPane: @Composable () -> Unit = {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(DesignSystemBindings.PixelSectionSpacing)
    ) {
        // 1. Custom Top Bar (Subtitle "Fun & Trip Expenses", Sync Cloud button, Clickable Avatar)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SplitMateTheme.SurfaceWhite)
                            .border(1.dp, SplitMateTheme.BorderLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "SplitMate Official App Logo",
                            modifier = Modifier
                                .size(42.dp)
                                .scale(1.25f)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SplitMate",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateTheme.PrimaryDark,
                            fontFamily = SplitMateTheme.FontDisplay
                        )
                        Text(
                            text = "Fun & Trip Expenses",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitMateTheme.TextSecondary,
                            fontFamily = SplitMateTheme.FontRounded
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val onlineFriendsTotal = remember(uiState.memberPresenceByPhone, uiState.members, uiState.userPhone) {
                        uiState.onlineFriendsCount()
                    }
                    val hasCloudPhone = remember(uiState.userPhone) {
                        com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianMobile(uiState.userPhone).length == 10
                    }
                    // Automatic Cloud Sync & Live Online Presence Pill
                    Surface(
                        onClick = { viewModel.syncAllGroupsWithCloud(context) },
                        shape = CircleShape,
                        color = if (onlineFriendsTotal > 0) Color(0xFFDCFCE7) else SplitMateTheme.SurfaceWhite,
                        border = BorderStroke(
                            1.dp,
                            if (onlineFriendsTotal > 0) Color(0xFF22C55E).copy(alpha = 0.45f) else SplitMateTheme.BorderLight
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            if (uiState.isCloudSyncing) {
                                ContainedLoadingIndicator(
                                    modifier = Modifier.size(16.dp),
                                    containerSize = 16.dp,
                                    containerColor = SplitMateTheme.SageSurface,
                                    indicatorColor = SplitMateTheme.SageText
                                )
                            } else if (onlineFriendsTotal > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDone,
                                    contentDescription = "Auto-Sync Status",
                                    tint = Color(0xFF416913),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = when {
                                    uiState.isCloudSyncing -> "Syncing"
                                    onlineFriendsTotal > 0 -> "$onlineFriendsTotal Online"
                                    hasCloudPhone -> "Auto-Sync"
                                    else -> "Offline Ready"
                                },
                                fontFamily = SplitMateTheme.FontRounded,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (onlineFriendsTotal > 0) Color(0xFF15803D) else SplitMateTheme.PrimaryDark
                            )
                        }
                    }

                    // 1-Tap 3-Theme Expressive Switcher Pill (Sunlit Buckwheat -> Warm Espresso Night -> Kyoto Matcha & Yuzu)
                    ExpressiveThemeModePill(
                        activeThemeMode = uiState.activeThemeMode,
                        isDarkTheme = uiState.isDarkTheme,
                        onCycleTheme = {
                            viewModel.cycleExpressiveThemeMode(context)
                        }
                    )

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onAvatarSettingsClick)
                    ) {
                        SplitMateCharacterAvatar(
                            name = uiState.avatarSeed.ifBlank { uiState.currentUserName.ifBlank { "You" } },
                            phone = uiState.userPhone,
                            size = 38.dp,
                            styleId = uiState.avatarStyleId,
                            colorPresetId = uiState.avatarColorPresetId,
                            highlighted = uiState.isPhoneVerified
                        )
                    }
                }
            }
        }

        // 1.2. Option 1 User Phone + 4-Digit PIN Cloud Sync Banner
        val normalizedUserPhone10 = PhoneIdentityValidator.normalizeIndianPhone10(uiState.userPhone)
        if (!uiState.isPhoneVerified && (normalizedUserPhone10.length == 10 || uiState.userPhone.isBlank())) {
            item(key = "upgrade_phone_otp_banner") {
                Card(
                    shape = SplitMateTheme.RadiusCard,
                    colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF416913).copy(alpha = 0.45f), SplitMateTheme.RadiusCard)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD7E8B6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF365314),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Link Phone & 4-Digit PIN",
                                        fontFamily = SplitMateTheme.FontDisplay,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SplitMateTheme.PrimaryDark
                                    )
                                    Text(
                                        text = if (normalizedUserPhone10.length == 10) {
                                            "Unlock +91 $normalizedUserPhone10 with your 4-Digit PIN to sync shared trips"
                                        } else {
                                            "Link your 10-digit mobile number & 4-Digit PIN to sync groups across phones"
                                        },
                                        fontFamily = SplitMateTheme.FontRounded,
                                        fontSize = 11.sp,
                                        color = SplitMateTheme.TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                onClick = {
                                    showUpgradeOtpExpanded = !showUpgradeOtpExpanded
                                },
                                shape = SplitMateTheme.RadiusBadge,
                                color = Color(0xFF416913)
                            ) {
                                Text(
                                    text = "Link Phone & PIN",
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFAF6F0),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (showUpgradeOtpExpanded) {
                            val cleanUpgradePhone10 = PhoneIdentityValidator.normalizeIndianPhone10(upgradePhoneInput)
                            val isValidUpgradePhone = PhoneIdentityValidator.isValidIndianMobile10(cleanUpgradePhone10)

                            OutlinedTextField(
                                value = upgradePhoneInput,
                                onValueChange = { upgradePhoneInput = it },
                                label = { Text("10-Digit Mobile (+91)") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.PhoneIphone, contentDescription = null, tint = SplitMateTheme.PrimaryDark)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = SplitMateTheme.RadiusInput,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = upgradeOtpInput,
                                onValueChange = { rawPin ->
                                    upgradeOtpInput = rawPin.filter { it.isDigit() }.take(4)
                                },
                                label = { Text("4-Digit Security PIN") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = SplitMateTheme.PrimaryDark)
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                shape = SplitMateTheme.RadiusInput,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val clean10 = PhoneIdentityValidator.normalizeIndianPhone10(upgradePhoneInput)
                                    val parsedDescriptor = AvatarSeedCodec.parse(
                                        rawSeed = uiState.avatarSeed.ifBlank { uiState.currentUserName },
                                        fallbackStyleId = uiState.avatarStyleId,
                                        fallbackColorPresetId = uiState.avatarColorPresetId
                                    )
                                    viewModel.verifyPinAndRestoreCloud(
                                        context = context,
                                        rawPhone = upgradePhoneInput,
                                        enteredPin4 = upgradeOtpInput,
                                        fallbackUserName = uiState.currentUserName.ifBlank { "Akshay" },
                                        fallbackUpiId = uiState.userUpiId.ifBlank { "$clean10@upi" },
                                        avatarStyleId = parsedDescriptor.styleId,
                                        avatarColorPresetId = parsedDescriptor.colorPresetId,
                                        avatarGender = parsedDescriptor.gender.id,
                                        customSeedKey = parsedDescriptor.seedKey,
                                        preferLocalAvatarChoice = true
                                    ) { ok, msg ->
                                        upgradeFeedbackMsg = msg
                                        if (ok) {
                                            showUpgradeOtpExpanded = false
                                        }
                                    }
                                },
                                enabled = isValidUpgradePhone && upgradeOtpInput.length == 4 && !uiState.isCloudSyncing,
                                shape = SplitMateTheme.RadiusButton,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF416913),
                                    contentColor = Color(0xFFFAF6F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isCloudSyncing) "Syncing Shared Trips..." else "Save PIN & Sync Trips",
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            upgradeFeedbackMsg?.let { msg ->
                                Text(
                                    text = msg,
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (msg.contains("Incorrect", ignoreCase = true) || msg.contains("Enter", ignoreCase = true)) {
                                        Color(0xFFE06B52)
                                    } else {
                                        Color(0xFF416913)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1.5. Pending Group Invites (Prominent Buckwheat PendingGroupInviteCard)
        if (uiState.pendingInviteGroups.isNotEmpty()) {
            items(uiState.pendingInviteGroups, key = { "pending_${it.groupId}" }) { pendingGroup ->
                val groupMemberCount = uiState.members.count { it.groupId == pendingGroup.groupId }.coerceAtLeast(1)
                PendingGroupInviteCard(
                    group = pendingGroup,
                    memberCount = groupMemberCount,
                    userPhone = uiState.userPhone.ifBlank { uiState.pendingOtpPhone10 },
                    onAcceptClick = { viewModel.acceptGroupInvite(context, pendingGroup.groupId) },
                    onDeclineClick = { viewModel.declineGroupInvite(context, pendingGroup.groupId) }
                )
            }
        }

        // 2. Hero Balance Card (32dp Radius, Warm Espresso Night & Kyoto Matcha Adaptive Gradient, Buckwheat-Inspired 64.sp Numbers)
        item {
            val heroGradientColors = when {
                SplitMateTheme.isDark -> listOf(Color(0xFF233216), Color(0xFF24201C))
                DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU ->
                    listOf(Color(0xFFC4ECCB), Color(0xFFFEF08A))
                else -> listOf(Color(0xFFF5F8EC), Color(0xFFFDF1EC))
            }
            Card(
                shape = SplitMateTheme.RadiusHero,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusHero)
                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
            ) {
                Box(
                    modifier = Modifier
                        .background(Brush.linearGradient(colors = heroGradientColors))
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isZeroBalance = totalBalance == "₹0.00"
                            Surface(
                                shape = SplitMateTheme.RadiusBadge,
                                color = SplitMateTheme.SurfaceWhite.copy(alpha = 0.90f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when {
                                            isZeroBalance -> Icons.Rounded.CheckCircle
                                            isNegativeBalance -> Icons.AutoMirrored.Rounded.TrendingUp
                                            else -> Icons.AutoMirrored.Rounded.TrendingDown
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            isZeroBalance -> SplitMateTheme.TextSecondary
                                            isNegativeBalance -> SplitMateTheme.TerracottaText
                                            else -> SplitMateTheme.SageText
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when {
                                            isZeroBalance -> "All settled up overall"
                                            isNegativeBalance -> "You owe overall"
                                            else -> "You are owed overall"
                                        },
                                        fontFamily = SplitMateTheme.FontRounded,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isZeroBalance -> SplitMateTheme.TextSecondary
                                            isNegativeBalance -> SplitMateTheme.TerracottaText
                                            else -> SplitMateTheme.SageText
                                        }
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.isOfflineMode) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(SplitMateTheme.BrandCoral)
                                    )
                                } else {
                                    ContainedLoadingIndicator(
                                        modifier = Modifier.size(18.dp),
                                        containerColor = SplitMateTheme.SageSurface,
                                        indicatorColor = SplitMateTheme.SageText
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isOfflineMode) "Offline" else "Live Sync",
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isOfflineMode) SplitMateTheme.BrandCoral else SplitMateTheme.SageText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Buckwheat-Inspired Oversized Tabular Number Display (64.sp Figtree ExtraBold on First Tab ONLY!)
                        val dotIdx = totalBalance.indexOf('.')
                        val mainRupeesPart = if (dotIdx != -1) totalBalance.substring(0, dotIdx) else totalBalance
                        val centsPart = if (dotIdx != -1) totalBalance.substring(dotIdx) else ".00"
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = mainRupeesPart,
                                fontSize = 60.sp,
                                lineHeight = 62.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-2.2).sp,
                                color = SplitMateTheme.PrimaryDark,
                                fontFamily = SplitMateTheme.FontDisplay,
                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                            )
                            Text(
                                text = centsPart,
                                fontSize = 30.sp,
                                lineHeight = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.8).sp,
                                color = SplitMateTheme.TextSecondary,
                                fontFamily = SplitMateTheme.FontDisplay,
                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                                modifier = Modifier.padding(bottom = 6.dp, start = 1.dp)
                            )
                        }

                        Text(
                            text = if (displayedActiveGroups.size == 1) "1 active group" else "Across ${displayedActiveGroups.size} active groups",
                            fontFamily = SplitMateTheme.FontRounded,
                            fontSize = 13.sp,
                            letterSpacing = 0.sp,
                            color = SplitMateTheme.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Phase 3: Promote 'Settle Up' as the sole Primary Action inside the Hero Balance Card
                        if (totalBalance != "₹0.00") {
                            Button(
                                onClick = onNavigateToSettle,
                                shape = SplitMateTheme.RadiusButton,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SplitMateTheme.PrimaryDark,
                                    contentColor = SplitMateTheme.ScreenBg
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .sizeIn(minHeight = 48.dp)
                            ) {
                                Icon(Icons.Rounded.TaskAlt, contentDescription = null, tint = SplitMateTheme.ScreenBg, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Settle Up", fontFamily = SplitMateTheme.FontRounded, color = SplitMateTheme.ScreenBg, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = SplitMateTheme.ScreenBg,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            FilledTonalButton(
                                onClick = onNavigateToSettle,
                                shape = SplitMateTheme.RadiusButton,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SplitMateTheme.SurfaceWhite,
                                    contentColor = SplitMateTheme.PrimaryDark
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .sizeIn(minHeight = 46.dp)
                            ) {
                                Icon(Icons.Rounded.TaskAlt, contentDescription = null, tint = SplitMateTheme.PrimaryDark, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Settle Up · All Balanced", fontFamily = SplitMateTheme.FontRounded, color = SplitMateTheme.PrimaryDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2.5. STANDALONE HERO FEATURE ELEMENT: Interactive Stacked Transit Pass Deck (Train PNR + Flight PDF)
        item {
            val hasGroups = displayedActiveGroups.isNotEmpty()
            val totalLoggedTrainPnrs = remember(uiState.expenses) {
                uiState.expenses.count {
                    it.expenseCategory.equals("TRAIN", ignoreCase = true) ||
                        (it.title.contains("PNR:", ignoreCase = true) &&
                            !it.title.contains("Flight", ignoreCase = true) &&
                            !it.title.contains("Airfare", ignoreCase = true))
                }
            }
            val totalLoggedFlightPnrs = remember(uiState.expenses) {
                uiState.expenses.count {
                    it.expenseCategory.equals("FLIGHT", ignoreCase = true) ||
                        (it.title.contains("PNR:", ignoreCase = true) &&
                            (it.title.contains("Flight", ignoreCase = true) || it.title.contains("Airfare", ignoreCase = true)))
                }
            }

            AnimatedTransitDeckHeroCard(
                initialPassMode = ActiveTravelPassMode.TRAIN,
                trainCountLogged = totalLoggedTrainPnrs,
                flightCountActive = totalLoggedFlightPnrs,
                onEnterTrainPnrClick = {
                    if (hasGroups) {
                        onNavigateToPnrSplit()
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "Please create a Group first before splitting an IRCTC PNR ticket!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        showNewGroupDialog = true
                    }
                },
                onUploadFlightPdfClick = {
                    if (hasGroups) {
                        onUploadFlightPdf()
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "Please create a Group first before uploading a Flight E-Ticket PDF!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        showNewGroupDialog = true
                    }
                }
            )
        }

        // 3. Section Header: Active Groups + M3 Expressive SplitButtonLayout (+ New Group | Join with Code)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Active Groups",
                        fontFamily = SplitMateTheme.FontDisplay,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SplitMateTheme.PrimaryDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = SplitMateTheme.SurfaceMuted,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${displayedActiveGroups.size}",
                                fontFamily = SplitMateTheme.FontRounded,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitMateTheme.PrimaryDark,
                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    }
                }

                SplitButtonLayout(
                    leadingText = "+ New Group",
                    leadingIcon = Icons.Rounded.Add,
                    onLeadingClick = { showNewGroupDialog = true },
                    containerColor = SplitMateTheme.SageSurface,
                    contentColor = SplitMateTheme.SageText,
                    menuItems = listOf(
                        ExpressiveMenuAction(
                            label = "Join with Code",
                            subtitle = "Enter a 6-digit invite code",
                            icon = Icons.Rounded.GroupAdd,
                            onClick = { showJoinByCodeDialog = true }
                        ),
                        ExpressiveMenuAction(
                            label = "Create New Group",
                            subtitle = "Start a shared trip or ledger",
                            icon = Icons.Rounded.Add,
                            onClick = { showNewGroupDialog = true }
                        )
                    )
                )
            }
        }

        // 4. Illustrated M3 Empty State Card when displayedActiveGroups is empty
        item {
            AnimatedVisibility(
                visible = displayedActiveGroups.isEmpty(),
                enter = fadeIn() + expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    shape = SplitMateTheme.RadiusHero,
                    colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusHero)
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SplitMateTheme.SageSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Explore,
                                contentDescription = "No Active Trips Illustration",
                                tint = SplitMateTheme.SageText,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No active trips yet. Tap + New Group or Join with Code!",
                            fontFamily = SplitMateTheme.FontDisplay,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateTheme.PrimaryDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Works 100% offline. Add friends by mobile number for automatic cloud sync.",
                            fontFamily = SplitMateTheme.FontRounded,
                            fontSize = 12.sp,
                            color = SplitMateTheme.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        // v2.3.2: "Create First Group" (primary) + "Join with Code" (menu) condensed into one M3E SplitButtonLayout.
                        SplitButtonLayout(
                            leadingText = "Create First Group",
                            leadingIcon = Icons.Rounded.Add,
                            onLeadingClick = { showNewGroupDialog = true },
                            menuItems = listOf(
                                ExpressiveMenuAction(
                                    label = "Join with Code",
                                    icon = Icons.Rounded.GroupAdd,
                                    subtitle = "Enter an invite code from a friend",
                                    onClick = { showJoinByCodeDialog = true }
                                ),
                                ExpressiveMenuAction(
                                    label = "Create New Group",
                                    icon = Icons.Rounded.Add,
                                    subtitle = "Start a fresh trip or shared ledger",
                                    onClick = { showNewGroupDialog = true }
                                )
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = SplitMateTheme.PrimaryDark,
                            contentColor = SplitMateTheme.ScreenBg,
                            fillWidth = true
                        )
                    }
                }
            }
        }

        // 5. Dynamic Group Cards (24dp outer / 8dp inner Segmented Island radius, 8-Icon Category support, 14dp compact padding)
        itemsIndexed(displayedActiveGroups, key = { _, it -> it.groupId }) { groupIdx, groupCard ->
            val cardSurfaceColor = when {
                groupCard.netBalanceCents > 0L -> SplitMateTheme.SageSurface
                groupCard.netBalanceCents < 0L -> SplitMateTheme.TerracottaSurface
                else -> SplitMateTheme.SurfaceWhite
            }
            // Restrict Forest Green (SageText) strictly to positive financial balances (> 0L)
            val badgeTextColor = when {
                groupCard.netBalanceCents > 0L -> SplitMateTheme.SageText
                groupCard.netBalanceCents < 0L -> SplitMateTheme.TerracottaText
                else -> SplitMateTheme.TextSecondary
            }
            val groupIcon = resolveGroupCategoryIcon(groupCard.iconName, groupCard.name)
            val groupIslandShape = segmentedIslandItemShape(
                index = groupIdx,
                totalCount = displayedActiveGroups.size,
                isSelected = false,
                outerCorner = 24.dp,
                innerCorner = 8.dp
            )

            Card(
                onClick = {
                    viewModel.openGroupDetail(groupCard.groupId)
                },
                shape = groupIslandShape,
                colors = CardDefaults.cardColors(containerColor = cardSurfaceColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                    .then(
                        if (groupCard.netBalanceCents == 0L) {
                            Modifier.border(1.dp, SplitMateTheme.BorderLight, groupIslandShape)
                        } else Modifier
                    )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(MaterialShapes.Cookie9Sided.toShape())
                                    .background(if (groupCard.netBalanceCents == 0L) SplitMateTheme.SurfaceMuted else Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = groupIcon,
                                    contentDescription = null,
                                    tint = if (groupCard.netBalanceCents == 0L) SplitMateTheme.PrimaryDark else badgeTextColor
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = groupCard.name.toSmartTitleCase(),
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    letterSpacing = (-0.4).sp,
                                    lineHeight = 24.sp,
                                    color = SplitMateTheme.PrimaryDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Tap to view expenses & balances",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SplitMateTheme.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        val statusLabel = when {
                            groupCard.netBalanceCents > 0L -> "YOU GET BACK"
                            groupCard.netBalanceCents < 0L -> "YOU OWE"
                            else -> "ALL SETTLED"
                        }
                        val numericBadgeAmount = if (groupCard.netBalanceCents == 0L) {
                            "₹0.00"
                        } else {
                            formatIndianRupeesFromCents(
                                cents = groupCard.netBalanceCents,
                                includePlusSign = true,
                                currencySymbol = "₹"
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = SplitMateTheme.RadiusBadge,
                                color = if (groupCard.netBalanceCents == 0L) SplitMateTheme.SurfaceMuted else SplitMateTheme.SurfaceWhite.copy(alpha = 0.9f)
                            ) {
                                Text(
                                    text = statusLabel,
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    color = badgeTextColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = numericBadgeAmount,
                                fontFamily = SplitMateTheme.FontDisplay,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.9).sp,
                                color = if (groupCard.netBalanceCents == 0L) SplitMateTheme.PrimaryDark else badgeTextColor,
                                maxLines = 1,
                                softWrap = false,
                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OverlappingAvatarStack(
                            avatars = groupCard.memberSeeds,
                            remainingCount = groupCard.remainingCount,
                            onlineFlags = groupCard.memberOnlineFlags
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (groupCard.onlineFriendsCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF22C55E))
                                        )
                                        Text(
                                            text = "${groupCard.onlineFriendsCount} Online",
                                            fontFamily = SplitMateTheme.FontRounded,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = groupCard.statusPillText,
                                fontFamily = SplitMateTheme.FontRounded,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SplitMateTheme.TextSecondary,
                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    }
                }
            }
        }

        // 6. Recent Activity Preview
        if (uiState.expenses.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                ) {
                    Text(
                        text = "Recent Activity",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SplitMateTheme.PrimaryDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.expenses.take(3).forEach { expense ->
                        val payer = uiState.members.find { it.memberId == expense.payerId }
                        val isMePayer = payer?.isCurrentUser == true
                        val sym = uiState.activeCurrency.symbol
                        val signedCents = if (isMePayer) expense.totalAmountCents else -expense.totalAmountCents
                        val formattedAmt = formatIndianRupeesFromCents(
                            cents = signedCents,
                            includePlusSign = true,
                            currencySymbol = sym
                        )
                        val (catBg, catTint) = resolveExpenseCategoryBadgeColors(expense.title, SplitMateTheme.isDark)
                        val parsedTravel = extractTravelTicketFromTitle(expense.title)
                        val cleanTitle = cleanDisplayExpenseTitle(expense.title)
                        val payerShortName = if (isMePayer) "you" else (payer?.name?.substringBefore(" ") ?: "Member")
                        val subText = if (parsedTravel != null && parsedTravel.pnr.isNotBlank()) {
                            "PNR ${parsedTravel.pnr} (${parsedTravel.bookingStatus}) · Paid by $payerShortName"
                        } else {
                            "Paid by $payerShortName · Tap group to view details"
                        }
                        ActivityItemRow(
                            icon = resolveExpenseCategoryIcon(expense.title),
                            iconBg = catBg,
                            iconTint = catTint,
                            title = cleanTitle,
                            subtitle = subText,
                            amount = formattedAmt,
                            isPositive = isMePayer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // 7. Collapsible "Declined Invites (Restore)" Drawer at Bottom of Ledgers Tab
        if (uiState.declinedInviteGroups.isNotEmpty()) {
            item(key = "declined_invites_drawer") {
                Card(
                    shape = SplitMateTheme.RadiusCard,
                    colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
                        .animateContentSize()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDeclinedDrawerExpanded = !isDeclinedDrawerExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Archive,
                                    contentDescription = null,
                                    tint = Color(0xFFE06B52),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Declined Invites (${uiState.declinedInviteGroups.size})",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SplitMateTheme.PrimaryDark
                                )
                            }
                            Icon(
                                imageVector = if (isDeclinedDrawerExpanded) {
                                    Icons.Rounded.KeyboardArrowUp
                                } else {
                                    Icons.Rounded.KeyboardArrowDown
                                },
                                contentDescription = "Toggle Declined Invites",
                                tint = SplitMateTheme.TextSecondary
                            )
                        }

                        if (isDeclinedDrawerExpanded) {
                            uiState.declinedInviteGroups.forEach { declinedGroup ->
                                Surface(
                                    shape = SplitMateTheme.RadiusPanel,
                                    color = Color(0xFFFED8C8).copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, Color(0xFFE06B52).copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = declinedGroup.name.toSmartTitleCase(),
                                                fontFamily = SplitMateTheme.FontDisplay,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = SplitMateTheme.PrimaryDark
                                            )
                                            Text(
                                                text = "Declined group invite · Hidden from active balances",
                                                fontFamily = SplitMateTheme.FontRounded,
                                                fontSize = 11.sp,
                                                color = SplitMateTheme.TextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            onClick = {
                                                viewModel.restoreDeclinedGroupInvite(context, declinedGroup.groupId)
                                            },
                                            shape = SplitMateTheme.RadiusBadge,
                                            color = Color(0xFF416913)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Restore,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFAF6F0),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Restore & Join",
                                                    fontFamily = SplitMateTheme.FontRounded,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFFAF6F0)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }

    if (isTwoPaneListDetailViewport) {
        val activeSupportingGroup = currentOpenedGroup ?: uiState.groups.first()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(0.43f)
                    .widthIn(max = 420.dp)
                    .fillMaxHeight()
            ) {
                renderMasterGroupListPane()
            }
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = SplitMateTheme.SurfaceMuted,
                border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                modifier = Modifier
                    .weight(0.57f)
                    .fillMaxHeight()
            ) {
                if (useTripHubV2View) {
                    com.splitmate.app.ui.screens.TripHomeScreen(
                        viewModel = viewModel,
                        groupId = activeSupportingGroup.groupId,
                        onBackClick = { viewModel.closeGroupDetail() },
                        onSwitchToClassicLedgerClick = { onToggleTripHubView(false) },
                        onOpenTrainPnrReviewClick = { pnr ->
                            if (pnr.isNotBlank()) onOpenPnrWithTicket(pnr) else onNavigateToPnrSplit()
                        },
                        onOpenFlightReviewClick = { pnr ->
                            if (pnr.isNotBlank()) onOpenPnrWithTicket(pnr) else onUploadFlightPdf()
                        },
                        onLogQuickExpenseClick = onNavigateToSplit,
                        onOpenSettleUpClick = onNavigateToSettle,
                        onAddMemberClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                viewModel.loadDeviceContacts(context)
                                showAddContactsToExistingGroupSheet = true
                            } else {
                                addToGroupPermLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                    )
                } else {
                    renderGroupDetailPane(activeSupportingGroup)
                }
            }
        }
    } else {
        AnimatedContent(
            targetState = currentOpenedGroup,
            transitionSpec = {
                (fadeIn(SplitMateMotion.defaultEffects()) + scaleIn(
                    initialScale = 0.93f,
                    animationSpec = spring(dampingRatio = 0.76f, stiffness = 380f)
                )) togetherWith (fadeOut(SplitMateMotion.fastEffects()) + scaleOut(
                    targetScale = 0.95f,
                    animationSpec = spring(dampingRatio = 0.80f, stiffness = 400f)
                )) using SizeTransform(clip = false) { _, _ ->
                    spring(dampingRatio = 0.76f, stiffness = 360f)
                }
            },
            label = "GroupCardContainerTransform"
        ) { targetGroup ->
            if (targetGroup != null) {
                if (useTripHubV2View) {
                    com.splitmate.app.ui.screens.TripHomeScreen(
                        viewModel = viewModel,
                        groupId = targetGroup.groupId,
                        onBackClick = { viewModel.closeGroupDetail() },
                        onSwitchToClassicLedgerClick = { onToggleTripHubView(false) },
                        onOpenTrainPnrReviewClick = { pnr ->
                            if (pnr.isNotBlank()) onOpenPnrWithTicket(pnr) else onNavigateToPnrSplit()
                        },
                        onOpenFlightReviewClick = { pnr ->
                            if (pnr.isNotBlank()) onOpenPnrWithTicket(pnr) else onUploadFlightPdf()
                        },
                        onLogQuickExpenseClick = onNavigateToSplit,
                        onOpenSettleUpClick = onNavigateToSettle,
                        onAddMemberClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                viewModel.loadDeviceContacts(context)
                                showAddContactsToExistingGroupSheet = true
                            } else {
                                addToGroupPermLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                    )
                } else {
                    renderGroupDetailPane(targetGroup)
                }
            } else {
                renderMasterGroupListPane()
            }
        }
    }

    // =========================================================================
    // CREATE GROUP DIALOG (100% Real Contacts Based + Live Inline Contact Search)
    // =========================================================================
    if (showNewGroupDialog) {
        var groupNameInput by remember { mutableStateOf("") }
        var selectedIconKey by remember { mutableStateOf("Flight") }
        var selectedMembers by remember { mutableStateOf(listOf<NewGroupMemberDraft>()) }
        var contactSearchQuery by remember { mutableStateOf("") }
        var showMultiContactSheet by remember { mutableStateOf(false) }

        val contactsPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted: Boolean ->
            if (granted) {
                viewModel.loadDeviceContacts(context)
            } else {
                Toast.makeText(context, "Contacts permission is required to search and add contacts", Toast.LENGTH_SHORT).show()
            }
        }

        // Automatically preload device contacts as soon as CreateGroupDialog opens if permission is granted
        LaunchedEffect(Unit) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm && deviceContacts.isEmpty()) {
                viewModel.loadDeviceContacts(context)
            }
        }

        // Live filtered contacts matching contactSearchQuery (excluding already selected contacts)
        val matchingDeviceContacts = remember(contactSearchQuery, deviceContacts, selectedMembers) {
            val q = contactSearchQuery.trim()
            if (q.isEmpty()) {
                emptyList()
            } else {
                val selectedPhones = selectedMembers.map { it.cleanPhone }.toSet()
                val selectedNames = selectedMembers.map { it.name.lowercase(Locale.US) }.toSet()
                deviceContacts.filter { c ->
                    !selectedPhones.contains(c.cleanPhone) &&
                        !selectedNames.contains(c.name.lowercase(Locale.US)) &&
                        (c.name.contains(q, ignoreCase = true) || c.cleanPhone.contains(q))
                }.take(4)
            }
        }

        if (showMultiContactSheet) {
            // CreateGroupDialog is temporarily hidden while ContactPickerBottomSheet is active
            // so the Dialog Window never overlaps or blocks the BottomSheet.
            ContactPickerBottomSheet(
                contacts = deviceContacts,
                isLoading = isLoadingContacts,
                multiSelect = true,
                initialSelectedPhones = selectedMembers.map { it.cleanPhone }.filter { it.isNotEmpty() }.toSet(),
                onDismiss = { showMultiContactSheet = false },
                onConfirmSelection = { chosenContacts ->
                    val newDrafts = chosenContacts.map { contact ->
                        NewGroupMemberDraft(
                            name = contact.name,
                            cleanPhone = contact.cleanPhone
                        )
                    }
                    val existingNames = selectedMembers.map { it.name.lowercase(Locale.US) }.toSet()
                    val merged = selectedMembers + newDrafts.filterNot {
                        existingNames.contains(it.name.lowercase(Locale.US))
                    }
                    selectedMembers = merged
                    showMultiContactSheet = false
                }
            )
        } else {
            Dialog(onDismissRequest = { showNewGroupDialog = false }) {
            Surface(
                shape = SplitMateTheme.RadiusDialog,
                color = SplitMateTheme.SurfaceWhite,
                shadowElevation = 18.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusDialog)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SplitMateTheme.AccentSage),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = resolveGroupCategoryIcon(selectedIconKey, groupNameInput),
                                contentDescription = null,
                                tint = Color(0xFF23201E)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Create Group",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SplitMateTheme.PrimaryDark
                            )
                            Text(
                                text = "Search or pick members directly from your Contacts",
                                fontSize = 12.sp,
                                color = SplitMateTheme.TextSecondary
                            )
                        }
                    }

                    // 1. Group Name Field
                    OutlinedTextField(
                        value = groupNameInput,
                        onValueChange = { groupNameInput = it },
                        label = { Text("Group Name") },
                        placeholder = { Text("e.g. Goa Beach Villa") },
                        singleLine = true,
                        shape = SplitMateTheme.RadiusInput,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SplitMateTheme.PrimaryDark,
                            unfocusedTextColor = SplitMateTheme.PrimaryDark,
                            focusedBorderColor = SplitMateTheme.PrimaryDark,
                            unfocusedBorderColor = SplitMateTheme.BorderLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2. Group Category Icon Picker Grid (2x4 Expressive Material 3 Icons)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Category Icon",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitMateTheme.PrimaryDark
                        )
                        GroupCategoryIcons.chunked(4).forEach { rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowIcons.forEach { option ->
                                    val isSelected = selectedIconKey == option.key
                                    val iconScale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.08f else 1.0f,
                                        animationSpec = DesignSystemBindings.tactileSpring(),
                                        label = "GroupIconBounce_${option.key}"
                                    )
                                    Surface(
                                        onClick = { selectedIconKey = option.key },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) SplitMateTheme.AccentSage else SplitMateTheme.SurfaceMuted,
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF416913) else SplitMateTheme.BorderLight
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .scale(iconScale)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = option.icon,
                                                contentDescription = option.label,
                                                tint = if (isSelected) Color(0xFF23201E) else SplitMateTheme.PrimaryDark,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = option.label,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (isSelected) Color(0xFF23201E) else SplitMateTheme.TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Secondary Tonal Action: "+ Browse All Contacts" (Canonical GM3 size="sm" 40.dp FilledTonalButton)
                    FilledTonalButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                viewModel.loadDeviceContacts(context)
                                showMultiContactSheet = true
                            } else {
                                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                        shape = SplitMateTheme.RadiusButton,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SplitMateTheme.AccentSage,
                            contentColor = Color(0xFF23201E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Contacts,
                            contentDescription = null,
                            tint = Color(0xFF23201E),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+ Browse & Multi-Select Contacts",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Color(0xFF23201E)
                        )
                    }

                    // 4. Horizontal Scrollable Selected Member Chips (Initials + Name + 'X' to Remove)
                    if (selectedMembers.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(selectedMembers, key = { it.name + it.cleanPhone }) { member ->
                                Surface(
                                    shape = SplitMateTheme.RadiusBadge,
                                    color = SplitMateTheme.SurfaceMuted,
                                    border = BorderStroke(1.dp, SplitMateTheme.BorderLight)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 6.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(SplitMateTheme.AccentSage),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = extractInitialsFromNameOrSeed(member.name),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF23201E)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = member.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SplitMateTheme.PrimaryDark
                                            )
                                            if (member.cleanPhone.isNotEmpty()) {
                                                Text(
                                                    text = "+91 ${member.cleanPhone}",
                                                    fontSize = 9.sp,
                                                    color = SplitMateTheme.SageText,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                selectedMembers = selectedMembers.filterNot {
                                                    it.name == member.name && it.cleanPhone == member.cleanPhone
                                                }
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove ${member.name}",
                                                tint = SplitMateTheme.TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Inline Live Contact Lookup Search Bar (Searches real deviceContacts as you type!)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = contactSearchQuery,
                                onValueChange = { query ->
                                    contactSearchQuery = query
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.READ_CONTACTS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm && deviceContacts.isEmpty()) {
                                        viewModel.loadDeviceContacts(context)
                                    } else if (!hasPerm && query.length == 1) {
                                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = null,
                                        tint = SplitMateTheme.TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                placeholder = { Text("Search contact by name or phone...", fontSize = 12.sp) },
                                singleLine = true,
                                shape = SplitMateTheme.RadiusInput,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = SplitMateTheme.PrimaryDark,
                                    unfocusedTextColor = SplitMateTheme.PrimaryDark,
                                    focusedBorderColor = SplitMateTheme.PrimaryDark,
                                    unfocusedBorderColor = SplitMateTheme.BorderLight
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (matchingDeviceContacts.isNotEmpty() || contactSearchQuery.isNotBlank()) {
                                FilledTonalButton(
                                    onClick = {
                                        val topMatch = matchingDeviceContacts.firstOrNull()
                                        if (topMatch != null) {
                                            selectedMembers = selectedMembers + NewGroupMemberDraft(
                                                name = topMatch.name,
                                                cleanPhone = topMatch.cleanPhone
                                            )
                                        } else {
                                            val typedPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianMobile(contactSearchQuery)
                                            val rawName = contactSearchQuery.replace(Regex("[0-9+\\-()\\s]"), " ").trim()
                                            val resolvedName = rawName.ifBlank {
                                                if (typedPhone10.length == 10) "Friend (${typedPhone10.takeLast(4)})" else contactSearchQuery.trim()
                                            }
                                            if (resolvedName.isNotBlank()) {
                                                selectedMembers = selectedMembers + NewGroupMemberDraft(
                                                    name = resolvedName,
                                                    cleanPhone = typedPhone10
                                                )
                                            }
                                        }
                                        contactSearchQuery = ""
                                    },
                                    shape = SplitMateTheme.RadiusButton,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = SplitMateTheme.AccentSage,
                                        contentColor = Color(0xFF23201E)
                                    ),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Icon(Icons.Rounded.PersonAdd, contentDescription = "Add Member", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Live matching contact suggestions from the user's phonebook (or direct add when not in phonebook)
                        if (contactSearchQuery.isNotBlank()) {
                            if (matchingDeviceContacts.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SplitMateTheme.SurfaceMuted)
                                        .border(1.dp, SplitMateTheme.BorderLight, RoundedCornerShape(14.dp))
                                        .padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    matchingDeviceContacts.forEach { matchedContact ->
                                        Surface(
                                            onClick = {
                                                selectedMembers = selectedMembers + NewGroupMemberDraft(
                                                    name = matchedContact.name,
                                                    cleanPhone = matchedContact.cleanPhone
                                                )
                                                contactSearchQuery = ""
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = SplitMateTheme.SurfaceWhite,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(SplitMateTheme.AccentSage),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = extractInitialsFromNameOrSeed(matchedContact.name),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFF23201E)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = matchedContact.name,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SplitMateTheme.PrimaryDark
                                                        )
                                                        Text(
                                                            text = "+91 ${matchedContact.cleanPhone}",
                                                            fontSize = 11.sp,
                                                            color = SplitMateTheme.SageText,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "+ Add",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = SplitMateTheme.SageText
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                val typedPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianMobile(contactSearchQuery)
                                val rawName = contactSearchQuery.replace(Regex("[0-9+\\-()\\s]"), " ").trim()
                                val resolvedName = rawName.ifBlank {
                                    if (typedPhone10.length == 10) "Friend (${typedPhone10.takeLast(4)})" else contactSearchQuery.trim()
                                }
                                Surface(
                                    onClick = {
                                        if (resolvedName.isNotBlank()) {
                                            selectedMembers = selectedMembers + NewGroupMemberDraft(
                                                name = resolvedName,
                                                cleanPhone = typedPhone10
                                            )
                                            contactSearchQuery = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = SplitMateTheme.SurfaceMuted,
                                    border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (typedPhone10.length == 10) {
                                                    "Add $resolvedName (+91 $typedPhone10)"
                                                } else {
                                                    "Add \"$resolvedName\" as offline member"
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SplitMateTheme.PrimaryDark
                                            )
                                            Text(
                                                text = if (typedPhone10.length == 10) {
                                                    "Auto-discovers group when they join with +91 $typedPhone10"
                                                } else {
                                                    "Works offline · You can link their 10-digit mobile number anytime"
                                                },
                                                fontSize = 10.sp,
                                                color = SplitMateTheme.SageText
                                            )
                                        }
                                        Text(
                                            text = "+ Add",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = SplitMateTheme.SageText
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. Action Buttons (Proper 8.dp spacing, End alignment, non-squished height)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showNewGroupDialog = false },
                            modifier = Modifier.height(40.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontWeight = FontWeight.Bold,
                                color = SplitMateTheme.TextSecondary,
                                maxLines = 1
                            )
                        }
                        Button(
                            onClick = {
                                val topMatch = matchingDeviceContacts.firstOrNull()
                                val finalDrafts = when {
                                    topMatch != null -> {
                                        selectedMembers + NewGroupMemberDraft(
                                            name = topMatch.name,
                                            cleanPhone = topMatch.cleanPhone
                                        )
                                    }
                                    contactSearchQuery.isNotBlank() -> {
                                        val typedPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianMobile(contactSearchQuery)
                                        val rawName = contactSearchQuery.replace(Regex("[0-9+\\-()\\s]"), " ").trim()
                                        val resolvedName = rawName.ifBlank {
                                            if (typedPhone10.length == 10) "Friend (${typedPhone10.takeLast(4)})" else contactSearchQuery.trim()
                                        }
                                        selectedMembers + NewGroupMemberDraft(
                                            name = resolvedName,
                                            cleanPhone = typedPhone10
                                        )
                                    }
                                    else -> selectedMembers
                                }
                                viewModel.createNewGroupWithContacts(
                                    name = groupNameInput,
                                    iconName = selectedIconKey,
                                    memberDrafts = finalDrafts
                                )
                                showNewGroupDialog = false
                            },
                            shape = SplitMateTheme.RadiusButton,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SplitMateTheme.PrimaryDark,
                                contentColor = SplitMateTheme.ScreenBg
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text(
                                text = "Create Group",
                                color = SplitMateTheme.ScreenBg,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
        }

        if (showJoinByCodeDialog) {
            JoinGroupByCodeDialog(
                viewModel = viewModel,
                onDismiss = { showJoinByCodeDialog = false },
                onJoinedGroup = { joinedGroupId ->
                    showJoinByCodeDialog = false
                    if (joinedGroupId.isNotBlank()) {
                        viewModel.openGroupDetail(joinedGroupId)
                    }
                }
            )
        }
    }
}

@Composable
fun JoinGroupByCodeDialog(
    viewModel: SplitMateViewModel,
    onDismiss: () -> Unit,
    onJoinedGroup: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputCodeOrLink by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorStatus by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFFAF6F0),
            border = BorderStroke(1.dp, Color(0xFFEDE7DF)),
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCE9B9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GroupAdd,
                                contentDescription = null,
                                tint = Color(0xFF365314),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Join a Shared Trip",
                                fontFamily = SplitMateTheme.FontDisplay,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF23201E)
                            )
                            Text(
                                text = "6-character Trip Code, WhatsApp invite, or link",
                                fontFamily = SplitMateTheme.FontRounded,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SplitMateTheme.TextSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = SplitMateTheme.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Enter a 6-character Trip Code (e.g. K9X-4M2), paste a WhatsApp invite message, or paste a splitmate:// link.",
                    fontFamily = SplitMateTheme.FontRounded,
                    fontSize = 12.sp,
                    color = SplitMateTheme.TextSecondary,
                    lineHeight = 17.sp
                )

                OutlinedTextField(
                    value = inputCodeOrLink,
                    onValueChange = { raw ->
                        inputCodeOrLink = if (raw.length <= 9 && !raw.contains("://") && !raw.contains(" ")) {
                            raw.uppercase(Locale.US)
                        } else {
                            raw
                        }
                        statusMessage = null
                    },
                    label = {
                        Text(
                            text = "Trip Code (e.g. K9X-4M2) or Invite Link",
                            fontFamily = SplitMateTheme.FontRounded,
                            fontSize = 12.sp
                        )
                    },
                    placeholder = {
                        Text(
                            text = "K9X-4M2",
                            fontFamily = SplitMateTnumMonospace,
                            fontSize = 14.sp
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = SplitMateTnumMonospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF23201E),
                        fontFeatureSettings = "tnum"
                    ),
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = {
                            val clip = clipboardManager.getText()?.text?.trim().orEmpty()
                            if (clip.isNotBlank()) {
                                inputCodeOrLink = clip
                                statusMessage = null
                            } else {
                                isErrorStatus = true
                                statusMessage = "Clipboard is empty. Copy a 6-character Trip Code or WhatsApp invite first."
                            }
                        },
                        shape = SplitMateTheme.RadiusBadge,
                        color = Color(0xFFF4EFE6),
                        border = BorderStroke(1.dp, Color(0xFFEDE7DF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentPaste,
                                contentDescription = null,
                                tint = Color(0xFF365314),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Paste from Clipboard",
                                fontFamily = SplitMateTheme.FontRounded,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF365314)
                            )
                        }
                    }

                    if (inputCodeOrLink.isNotBlank()) {
                        TextButton(onClick = { inputCodeOrLink = "" }) {
                            Text(
                                text = "Clear",
                                fontFamily = SplitMateTheme.FontRounded,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitMateTheme.TextSecondary
                            )
                        }
                    }
                }

                statusMessage?.let { feedback ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isErrorStatus) Color(0xFFFCE3D7) else Color(0xFFDCE9B9),
                        border = BorderStroke(
                            1.dp,
                            if (isErrorStatus) Color(0xFFE06B52).copy(alpha = 0.45f) else Color(0xFF416913).copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = feedback,
                            fontFamily = SplitMateTheme.FontRounded,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isErrorStatus) Color(0xFF7C2D12) else Color(0xFF365314),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.joinGroupByCodeOrLink(context, inputCodeOrLink) { ok, msg ->
                            isErrorStatus = !ok
                            statusMessage = msg
                            if (ok) {
                                val resolvedId = viewModel.uiState.value.openedGroupDetailId?.takeIf { it.isNotBlank() }
                                    ?: viewModel.uiState.value.activeGroupId
                                onJoinedGroup(resolvedId)
                            }
                        }
                    },
                    enabled = inputCodeOrLink.trim().isNotBlank() && !uiState.isCloudSyncing,
                    shape = SplitMateTheme.RadiusButton,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF365314),
                        contentColor = Color(0xFFFAF6F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (uiState.isCloudSyncing) {
                        ContainedLoadingIndicator(
                            modifier = Modifier.size(18.dp),
                            containerColor = Color(0xFFDCE9B9).copy(alpha = 0.22f),
                            indicatorColor = Color(0xFFFAF6F0)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFFDCE9B9),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isCloudSyncing) "Finding Shared Trip..." else "Find & Join Trip",
                        fontFamily = SplitMateTheme.FontRounded,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFAF6F0)
                    )
                }
            }
        }
    }
}

@Composable
fun ExpressiveThemeModePill(
    activeThemeMode: SplitMateThemeMode,
    isDarkTheme: Boolean = activeThemeMode.isDark,
    onCycleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val pillCorner by animateDpAsState(
        targetValue = when (activeThemeMode) {
            SplitMateThemeMode.SUNLIT_BUCKWHEAT -> 20.dp
            SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> 12.dp
            SplitMateThemeMode.KYOTO_MATCHA_YUZU -> 16.dp
        },
        animationSpec = SplitMateMotion.fastSpatial(),
        label = "ExpressiveThemeModePillCorner"
    )
    val badgeLabel = when (activeThemeMode) {
        SplitMateThemeMode.SUNLIT_BUCKWHEAT -> "Buckwheat"
        SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> "Espresso"
        SplitMateThemeMode.KYOTO_MATCHA_YUZU -> "Matcha"
    }
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onCycleTheme()
        },
        shape = RoundedCornerShape(pillCorner),
        color = SplitMateTheme.SageSurface.copy(alpha = if (isDarkTheme) 0.82f else 0.72f),
        border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = 40.dp)
            .testTag("TopBarExpressiveThemePill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = when (activeThemeMode) {
                    SplitMateThemeMode.SUNLIT_BUCKWHEAT -> Icons.Rounded.LightMode
                    SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> Icons.Rounded.DarkMode
                    SplitMateThemeMode.KYOTO_MATCHA_YUZU -> Icons.Rounded.AutoAwesome
                },
                contentDescription = "Switch Theme Mode (${activeThemeMode.displayName})",
                tint = SplitMateTheme.PrimaryDark,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = badgeLabel,
                fontFamily = SplitMateTheme.FontRounded,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SplitMateTheme.PrimaryDark,
                maxLines = 1
            )
        }
    }
}

// ==============================================================================
// TAB 3: SETTLE (Greedy Debt Simplification & Direct WhatsApp Reminder)
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreedySettlementScreen(viewModel: SplitMateViewModel) {
    val context = LocalContext.current
    val settlementPlan by viewModel.settlementPlan.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deviceContacts by viewModel.deviceContacts.collectAsStateWithLifecycle()
    val isLoadingContacts by viewModel.isLoadingContacts.collectAsStateWithLifecycle()

    var targetMemberForContactLink by remember { mutableStateOf<GroupMemberEntity?>(null) }
    var showContactLinkSheet by remember { mutableStateOf(false) }

    val directContactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted && targetMemberForContactLink != null) {
            viewModel.loadDeviceContacts(context)
            showContactLinkSheet = true
        } else {
            Toast.makeText(context, "Contacts permission required to link mobile number", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchContactPickerForMember(member: GroupMemberEntity?) {
        if (member == null) return
        targetMemberForContactLink = member
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.loadDeviceContacts(context)
            showContactLinkSheet = true
        } else {
            directContactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    var editingMemberInSettle by remember { mutableStateOf<GroupMemberEntity?>(null) }
    editingMemberInSettle?.let { targetMember ->
        val editableGroupMembers = uiState.members.filter {
            it.groupId == targetMember.groupId && !it.isCurrentUser
        }.ifEmpty { listOf(targetMember) }
        EditFriendUpiDialog(
            member = targetMember,
            allGroupMembers = editableGroupMembers,
            onSelectMember = { editingMemberInSettle = it },
            onDismiss = { editingMemberInSettle = null },
            onSave = { newName, newUpi, newAvatarSeed ->
                viewModel.updateFriendUpi(targetMember.memberId, newName, newUpi, newAvatarSeed)
                editingMemberInSettle = null
            },
            onSaveAll = { batchUpdates ->
                viewModel.updateAllGroupMembers(batchUpdates)
                editingMemberInSettle = null
            }
        )
    }

    if (showContactLinkSheet && targetMemberForContactLink != null) {
        val memberToUpdate = targetMemberForContactLink!!
        ContactPickerBottomSheet(
            contacts = deviceContacts,
            isLoading = isLoadingContacts,
            multiSelect = false,
            onDismiss = {
                showContactLinkSheet = false
                targetMemberForContactLink = null
            },
            onConfirmSelection = { chosen ->
                val picked = chosen.firstOrNull()
                if (picked != null && picked.cleanPhone.length == 10) {
                    viewModel.updateFriendUpi(
                        memberId = memberToUpdate.memberId,
                        newName = memberToUpdate.name,
                        newUpiId = picked.cleanPhone,
                        newAvatarSeed = memberToUpdate.avatarSeed
                    )
                }
                showContactLinkSheet = false
                targetMemberForContactLink = null
            }
        )
    }

    val memberMetadataMap = remember(uiState.activeGroupMembers, uiState.members, uiState.currentUserName, uiState.currentUserSeed) {
        (uiState.activeGroupMembers + uiState.members).associate { m ->
            val realName = if ((m.isCurrentUser || m.name.equals("You", ignoreCase = true)) && uiState.currentUserName.isNotBlank()) {
                uiState.currentUserName
            } else {
                m.name
            }
            val resolvedSeed = if (m.isCurrentUser && uiState.currentUserSeed.isNotBlank()) {
                uiState.currentUserSeed
            } else {
                m.avatarSeed
            }
            m.memberId to Triple(realName, resolvedSeed, m.isCurrentUser)
        }
    }

    val memberSummaries = remember(settlementPlan, memberMetadataMap) {
        SplitMateMathEngine.computeMemberSettlementSummaries(
            transfers = settlementPlan.map { it.transfer },
            memberMetadata = memberMetadataMap
        )
    }

    val topGridSummaries = remember(memberSummaries) {
        memberSummaries.sortedWith(
            compareByDescending<SplitMateMathEngine.MemberSettlementSummary> { it.hasIncoming && !it.hasOutgoing }
                .thenByDescending { it.isCurrentUser }
                .thenByDescending { maxOf(it.totalIncomingCents, it.totalOutgoingCents) }
                .thenBy { it.memberName }
        )
    }

    var sortByBalanceMagnitude by remember { mutableStateOf(false) }
    var selectedBalanceFilterIndex by rememberSaveable { mutableIntStateOf(0) }
    val resolvedGroupSettlements = remember(uiState.settlements, uiState.activeGroup, uiState.activeGroupId) {
        val gId = uiState.activeGroup?.groupId ?: uiState.activeGroupId
        uiState.settlements.filter { it.groupId == gId }
    }
    val settlementProgressRatio = remember(settlementPlan, resolvedGroupSettlements) {
        val openCents = settlementPlan.sumOf { it.transfer.amountCents }.coerceAtLeast(0L)
        val paidCents = resolvedGroupSettlements.sumOf { it.amountCents }.coerceAtLeast(0L)
        val totalCents = openCents + paidCents
        when {
            settlementPlan.isEmpty() -> 1f
            totalCents <= 0L -> 0.35f
            else -> (paidCents.toFloat() / totalCents.toFloat()).coerceIn(0.18f, 0.96f)
        }
    }
    val displayedMemberSummaries = remember(memberSummaries, sortByBalanceMagnitude, selectedBalanceFilterIndex) {
        val filtered = when (selectedBalanceFilterIndex) {
            1 -> memberSummaries.filter { it.hasIncoming }
            2 -> memberSummaries.filter { it.hasOutgoing }
            else -> memberSummaries
        }
        if (sortByBalanceMagnitude) {
            filtered.sortedByDescending { maxOf(it.totalOutgoingCents, it.totalIncomingCents) }
        } else {
            filtered
        }
    }
    val myMemberSummaries = remember(displayedMemberSummaries) {
        displayedMemberSummaries.filter { it.isCurrentUser }
    }
    val otherMemberSummaries = remember(displayedMemberSummaries) {
        displayedMemberSummaries.filter { !it.isCurrentUser }
    }
    val orderedMemberSummaries = remember(myMemberSummaries, otherMemberSummaries) {
        myMemberSummaries + otherMemberSummaries
    }
    // v2.3.3: "truly settled" is computed from the UNFILTERED summaries; a filter that only hides
    // rows yields a context-aware empty state instead of a false "You're all settled up".
    val yourSettlementsPresentation = remember(memberSummaries, selectedBalanceFilterIndex) {
        com.splitmate.app.SettlementFilterPresentation.resolve(memberSummaries, selectedBalanceFilterIndex)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(DesignSystemBindings.PixelSectionSpacing)
    ) {
        item {
            Text(
                text = "Settle Up & Balances",
                fontFamily = SplitMateTheme.FontDisplay,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SplitMateTheme.PrimaryDark
            )
            Text(
                text = "Your settlements first, followed by simplified group balances",
                fontFamily = SplitMateTheme.FontRounded,
                fontSize = 12.sp,
                color = SplitMateTheme.TextSecondary
            )

            if (uiState.groups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                val resolvedActiveGroupId = uiState.activeGroup?.groupId ?: uiState.activeGroupId
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.groups, key = { it.groupId }) { grp ->
                        val isSelected = grp.groupId == resolvedActiveGroupId
                        Surface(
                            onClick = { viewModel.selectActiveGroup(grp.groupId) },
                            shape = SplitMateTheme.RadiusBadge,
                            color = if (isSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.SurfaceWhite,
                            border = BorderStroke(1.dp, if (isSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.BorderLight)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.Groups,
                                    contentDescription = null,
                                    tint = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = grp.name,
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. TRIP SETTLEMENT SUMMARY CARD (Compact 2-Column GETS BACK | OWES Split Board with 3-Row Accordion)
        if (topGridSummaries.isNotEmpty()) {
            item(key = "trip_settlement_summary_card") {
                val summaryContainerBg = when {
                    SplitMateTheme.isDark -> SplitMateTheme.SurfaceWhite
                    DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU -> Color(0xFFD2F4DC)
                    else -> Color(0xFFF3F7EB)
                }
                val summaryBorderColor = when {
                    SplitMateTheme.isDark -> SplitMateTheme.BorderLight
                    DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU -> Color(0xFF75C993)
                    else -> Color(0xFFDCE6C8)
                }
                val receiversList = remember(topGridSummaries) {
                    topGridSummaries.filter { it.hasIncoming }
                        .sortedWith(
                            compareByDescending<SplitMateMathEngine.MemberSettlementSummary> { it.isCurrentUser }
                                .thenByDescending { it.totalIncomingCents }
                        )
                }
                val payersList = remember(topGridSummaries) {
                    topGridSummaries.filter { it.hasOutgoing }
                        .sortedWith(
                            compareByDescending<SplitMateMathEngine.MemberSettlementSummary> { it.isCurrentUser }
                                .thenByDescending { it.totalOutgoingCents }
                        )
                }
                var isSummaryBoardExpanded by remember { mutableStateOf(false) }
                val maxSummaryRows = maxOf(receiversList.size, payersList.size)
                val visibleRowCount = if (isSummaryBoardExpanded) maxSummaryRows else minOf(maxSummaryRows, 3)

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = summaryContainerBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, summaryBorderColor, RoundedCornerShape(24.dp))
                        .animateContentSize(animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        var showMaxHeapGraphInspector by remember { mutableStateOf(false) }
                        val rawPairwiseIouCount = remember(uiState.expenses.size, settlementPlan.size) {
                            (uiState.expenses.size * 2 + 1).coerceAtLeast(settlementPlan.size + 2)
                        }
                        val simplifiedTransferCount = settlementPlan.size
                        val inspectorCornerRadius by animateDpAsState(
                            targetValue = if (showMaxHeapGraphInspector) 10.dp else 20.dp,
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = 480f),
                            label = "MaxHeapInspectorPillCorner"
                        )

                        val headerMorph = remember {
                            Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny)
                        }
                        val headerMorphProgress by animateFloatAsState(
                            targetValue = if (settlementProgressRatio >= 0.99f) 1f else 0.35f,
                            animationSpec = SplitMateMotion.slowSpatial(),
                            label = "TripSettlementHeaderMorphProgress"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(42.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularWavyProgressIndicator(
                                    progress = { settlementProgressRatio },
                                    modifier = Modifier.fillMaxSize(),
                                    color = SplitMateTheme.SageText,
                                    trackColor = SplitMateTheme.SageSurface,
                                    strokeWidth = 3.dp,
                                    amplitude = if (settlementProgressRatio >= 0.99f) 0f else 0.85f,
                                    wavelength = 18.dp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(MorphPolygonShape(morph = headerMorph, percentage = headerMorphProgress))
                                        .background(SplitMateTheme.SageSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Groups,
                                        contentDescription = null,
                                        tint = SplitMateTheme.SageText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Trip Settlement Summary",
                                        fontFamily = SplitMateTheme.FontDisplay,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SplitMateTheme.PrimaryDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Info icon next to Settle Up — toggles the payment simplification diagram
                                    Surface(
                                        onClick = { showMaxHeapGraphInspector = !showMaxHeapGraphInspector },
                                        shape = RoundedCornerShape(inspectorCornerRadius),
                                        color = if (showMaxHeapGraphInspector) SplitMateTheme.SageText else SplitMateTheme.SageSurface,
                                        border = BorderStroke(1.dp, SplitMateTheme.SageText.copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Info,
                                                contentDescription = "Why were debts simplified?",
                                                tint = if (showMaxHeapGraphInspector) SplitMateTheme.SageSurface else SplitMateTheme.SageText,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${receiversList.size} receiving · ${payersList.size} paying · $simplifiedTransferCount ${if (simplifiedTransferCount == 1) "transfer" else "transfers"}",
                                    fontSize = 11.sp,
                                    color = SplitMateTheme.TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearWavyProgressIndicator(
                            progress = { settlementProgressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = SplitMateTheme.SageText,
                            trackColor = SplitMateTheme.SageSurface.copy(alpha = 0.55f),
                            amplitude = if (settlementProgressRatio >= 0.99f) 0f else 0.85f,
                            wavelength = 22.dp,
                            strokeWidth = 3.dp
                        )

                        AnimatedVisibility(
                            visible = showMaxHeapGraphInspector,
                            enter = expandVertically(animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f)) + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            val graphPulseTransition = rememberInfiniteTransition(label = "MaxHeapGraphFlow")
                            val flowProgress by graphPulseTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "FlowDotProgress"
                            )
                            val activeCoral = SplitMateTheme.BrandCoral
                            val activeSageContainer = SplitMateTheme.SageSurface
                            val activeSageText = SplitMateTheme.SageText
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SplitMateTheme.SurfaceWhite,
                                border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Before: $rawPairwiseIouCount separate payments",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SplitMateTheme.TerracottaText
                                        )
                                        Text(
                                            text = "Simplified: $simplifiedTransferCount direct payments",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = SplitMateTheme.SageText
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(74.dp)
                                    ) {
                                        val midX = size.width * 0.5f
                                        val midY = size.height * 0.5f
                                        val leftNodes = listOf(0.18f, 0.42f, 0.65f, 0.86f)
                                        val rightNodes = listOf(0.25f, 0.50f, 0.75f).take(simplifiedTransferCount.coerceIn(1, 3))

                                        // Left: Tangled dashed terracotta raw pairwise IOU arcs converging into Max-Heap node
                                        leftNodes.forEach { ny ->
                                            val startPt = Offset(14.dp.toPx(), size.height * ny)
                                            val path = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(startPt.x, startPt.y)
                                                cubicTo(
                                                    midX * 0.45f,
                                                    startPt.y,
                                                    midX * 0.65f,
                                                    midY,
                                                    midX - 18.dp.toPx(),
                                                    midY
                                                )
                                            }
                                            drawPath(
                                                path = path,
                                                color = activeCoral.copy(alpha = 0.55f),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                    width = 1.6.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                                )
                                            )
                                            drawCircle(
                                                color = activeCoral,
                                                radius = 4.dp.toPx(),
                                                center = startPt
                                            )
                                        }

                                        // Center: Max-Heap Simplifier Hub
                                        drawRoundRect(
                                            color = activeSageContainer,
                                            topLeft = Offset(midX - 18.dp.toPx(), midY - 14.dp.toPx()),
                                            size = androidx.compose.ui.geometry.Size(36.dp.toPx(), 28.dp.toPx()),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                        )
                                        drawCircle(
                                            color = activeSageText,
                                            radius = 6.dp.toPx(),
                                            center = Offset(midX, midY)
                                        )

                                        // Right: Clean minimum cash-flow vectors + animated flow dot
                                        rightNodes.forEach { ry ->
                                            val endPt = Offset(size.width - 14.dp.toPx(), size.height * ry)
                                            val startHub = Offset(midX + 18.dp.toPx(), midY)
                                            val path = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(startHub.x, startHub.y)
                                                cubicTo(
                                                    midX + (size.width - midX) * 0.4f,
                                                    midY,
                                                    midX + (size.width - midX) * 0.6f,
                                                    endPt.y,
                                                    endPt.x,
                                                    endPt.y
                                                )
                                            }
                                            drawPath(
                                                path = path,
                                                color = activeSageText,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
                                            )
                                            drawCircle(
                                                color = activeSageText,
                                                radius = 5.dp.toPx(),
                                                center = endPt
                                            )
                                            val dotX = startHub.x + (endPt.x - startHub.x) * flowProgress
                                            val dotY = startHub.y + (endPt.y - startHub.y) * flowProgress
                                            drawCircle(
                                                color = activeSageContainer,
                                                radius = 4.dp.toPx(),
                                                center = Offset(dotX, dotY)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Proportional Sage vs Terracotta Balance Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(receiversList.size.coerceAtLeast(1).toFloat())
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(SplitMateTheme.SageText.copy(alpha = 0.75f))
                            )
                            Box(
                                modifier = Modifier
                                    .weight(payersList.size.coerceAtLeast(1).toFloat())
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(SplitMateTheme.TerracottaText.copy(alpha = 0.75f))
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Compact 2-Column Split Summary Board (GETS BACK | OWES)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SplitMateTheme.SurfaceWhite,
                            border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "GETS BACK (${receiversList.size})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SplitMateTheme.SageText,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "OWES (${payersList.size})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SplitMateTheme.TerracottaText,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                HorizontalDivider(color = SplitMateTheme.BorderLight)

                                for (rowIdx in 0 until visibleRowCount) {
                                    if (rowIdx > 0) {
                                        HorizontalDivider(color = SplitMateTheme.BorderLight.copy(alpha = 0.5f))
                                    }
                                    val rec = receiversList.getOrNull(rowIdx)
                                    val pay = payersList.getOrNull(rowIdx)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left Column: Receiver mini-row
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (rec != null) {
                                                val recLabel = if (rec.isCurrentUser) {
                                                    "You"
                                                } else {
                                                    rec.memberName.substringBefore(" ").ifBlank { rec.memberName }
                                                }
                                                AvatarToken(
                                                    initials = rec.avatarSeed,
                                                    bg = SplitMateTheme.SageSurface,
                                                    textColor = SplitMateTheme.SageText,
                                                    size = 22
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = recLabel,
                                                    fontWeight = if (rec.isCurrentUser) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    color = SplitMateTheme.PrimaryDark,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "+${rec.formattedTotalIncoming}",
                                                    fontFamily = SplitMateTheme.FontDisplay,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.sp,
                                                    color = SplitMateTheme.SageText,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .width(1.dp)
                                                .height(22.dp)
                                                .background(SplitMateTheme.BorderLight)
                                        )

                                        // Right Column: Payer mini-row
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (pay != null) {
                                                val payLabel = if (pay.isCurrentUser) {
                                                    "You"
                                                } else {
                                                    pay.memberName.substringBefore(" ").ifBlank { pay.memberName }
                                                }
                                                AvatarToken(
                                                    initials = pay.avatarSeed,
                                                    bg = SplitMateTheme.TerracottaSurface,
                                                    textColor = SplitMateTheme.TerracottaText,
                                                    size = 22
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = payLabel,
                                                    fontWeight = if (pay.isCurrentUser) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    color = SplitMateTheme.PrimaryDark,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "-${pay.formattedTotalOutgoing}",
                                                    fontFamily = SplitMateTheme.FontDisplay,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.sp,
                                                    color = SplitMateTheme.TerracottaText,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                if (maxSummaryRows > 3) {
                                    HorizontalDivider(color = SplitMateTheme.BorderLight)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isSummaryBoardExpanded = !isSummaryBoardExpanded }
                                            .padding(vertical = 7.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isSummaryBoardExpanded) {
                                                "Show top 3 rows"
                                            } else {
                                                "Show all ${topGridSummaries.size} travelers"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SplitMateTheme.SageText
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (isSummaryBoardExpanded) {
                                                Icons.Rounded.KeyboardArrowUp
                                            } else {
                                                Icons.Rounded.KeyboardArrowDown
                                            },
                                            contentDescription = null,
                                            tint = SplitMateTheme.SageText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. SECTION 1 HEADER: YOUR SETTLEMENTS (Me-First Hierarchy + Expressive ConnectedButtonGroup Filter)
            item(key = "your_settlements_section_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Settlements",
                            fontFamily = SplitMateTheme.FontDisplay,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateTheme.PrimaryDark
                        )
                        Surface(
                            shape = SplitMateTheme.RadiusBadge,
                            color = if (yourSettlementsPresentation.showSettledCard) SplitMateTheme.SageSurface else SplitMateTheme.SurfaceMuted
                        ) {
                            Text(
                                text = if (yourSettlementsPresentation.showSettledCard) "All settled" else "Priority view",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (yourSettlementsPresentation.showSettledCard) SplitMateTheme.SageText else SplitMateTheme.TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    ConnectedButtonGroup(
                        options = listOf("All Balances", "Gets Back", "Owes"),
                        selectedIndex = selectedBalanceFilterIndex,
                        onSelect = { idx, _ -> selectedBalanceFilterIndex = idx },
                        labelProvider = { it },
                        modifier = Modifier.fillMaxWidth(),
                        iconProvider = { option, _ ->
                            when (option) {
                                "Gets Back" -> Icons.AutoMirrored.Rounded.TrendingUp
                                "Owes" -> Icons.AutoMirrored.Rounded.TrendingDown
                                else -> Icons.Rounded.Groups
                            }
                        }
                    )
                }
            }

            val myFilterEmptyState = yourSettlementsPresentation.myFilterEmptyState
            if (!yourSettlementsPresentation.showSettledCard && myFilterEmptyState != null) {
                item(key = "your_settlements_filter_empty") {
                    SettlementFilterEmptyStateCard(
                        state = myFilterEmptyState,
                        onSwitchFilter = { selectedBalanceFilterIndex = it }
                    )
                }
            }

            if (yourSettlementsPresentation.showSettledCard) {
                item(key = "your_settlements_settled_card") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SplitMateTheme.SurfaceWhite,
                        border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = SplitMateTheme.SageText,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "You're all settled up (₹0.00)",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = SplitMateTheme.PrimaryDark
                                )
                                Text(
                                    text = "You don't owe or receive anything in this trip.",
                                    fontSize = 11.sp,
                                    color = SplitMateTheme.TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 3. MEMBER SETTLEMENT CARDS (Your Settlements first, then Other Travelers' Settlements)
            itemsIndexed(
                items = orderedMemberSummaries,
                key = { _, it -> "member_detail_${it.memberId}" }
            ) { index, summary ->
                // Render "Other Travelers' Settlements" header right above the first non-current-user card
                if (!summary.isCurrentUser && index == myMemberSummaries.size) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Other Travelers' Settlements",
                                fontFamily = SplitMateTheme.FontDisplay,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SplitMateTheme.PrimaryDark
                            )
                            Text(
                                text = "Confirmed by each recipient once received",
                                fontSize = 11.sp,
                                color = SplitMateTheme.TextSecondary
                            )
                        }
                        Surface(
                            onClick = { sortByBalanceMagnitude = !sortByBalanceMagnitude },
                            shape = SplitMateTheme.RadiusBadge,
                            color = Color.Transparent
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (sortByBalanceMagnitude) "Sorted by amount" else "Sort by balance",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SplitMateTheme.TextSecondary
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Rounded.SwapVert,
                                    contentDescription = "Sort by balance",
                                    tint = SplitMateTheme.TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                // A. If the member has INCOMING payments (RECEIVES Card)
                if (summary.hasIncoming) {
                    val receiverCardBg = when {
                        SplitMateTheme.isDark -> SplitMateTheme.SurfaceWhite
                        DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU -> Color(0xFFD2F4DC)
                        else -> Color(0xFFF1F7E8)
                    }
                    val receiverBorderColor = when {
                        SplitMateTheme.isDark -> SplitMateTheme.BorderLight
                        DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU -> Color(0xFF75C993)
                        else -> Color(0xFFD8E5C2)
                    }

                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = receiverCardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, receiverBorderColor, RoundedCornerShape(22.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AvatarToken(
                                        initials = summary.avatarSeed,
                                        bg = SplitMateTheme.SageSurface,
                                        textColor = SplitMateTheme.SageText,
                                        size = 44
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (summary.isCurrentUser) "${summary.memberName} (You)" else summary.memberName,
                                                fontFamily = SplitMateTheme.FontDisplay,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp,
                                                color = SplitMateTheme.PrimaryDark
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = SplitMateTheme.RadiusBadge,
                                                color = SplitMateTheme.SageSurface
                                            ) {
                                                Text(
                                                    text = if (summary.isCurrentUser) "YOU RECEIVE" else "RECEIVES",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = SplitMateTheme.SageText,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (summary.isCurrentUser) "Incoming from group members" else "Receives from group members",
                                            fontSize = 12.sp,
                                            color = SplitMateTheme.TextSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = "+${summary.formattedTotalIncoming}",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = SplitMateTheme.SageText
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                summary.incomingPayments.forEachIndexed { legIdx, leg ->
                                    val matchingTransfer = settlementPlan.find {
                                        it.fromMemberId == leg.counterpartyMemberId && it.toMemberId == summary.memberId
                                    }
                                    val fromRoomMember = uiState.members.find { it.memberId == leg.counterpartyMemberId }
                                    val debtorPhone = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(
                                        userPhone = fromRoomMember?.userPhone.orEmpty(),
                                        upiId = fromRoomMember?.upiId.orEmpty()
                                    )
                                    val canMarkPaidIn = matchingTransfer != null && viewModel.canCurrentUserMarkTransferPaid(
                                        groupId = uiState.activeGroupId,
                                        toMemberId = matchingTransfer.transfer.toMemberId,
                                        state = uiState
                                    )
                                    val incomingLegShape = segmentedIslandItemShape(
                                        index = legIdx,
                                        totalCount = summary.incomingPayments.size,
                                        outerCorner = 16.dp,
                                        innerCorner = 6.dp
                                    )

                                    Surface(
                                        shape = incomingLegShape,
                                        color = SplitMateTheme.SurfaceWhite,
                                        border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(SplitMateTheme.SageSurface),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Groups,
                                                            contentDescription = null,
                                                            tint = SplitMateTheme.SageText,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = "From ",
                                                        fontSize = 13.sp,
                                                        color = SplitMateTheme.TextSecondary
                                                    )
                                                    Text(
                                                        text = leg.counterpartyName,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SplitMateTheme.PrimaryDark
                                                    )
                                                }

                                                Text(
                                                    text = leg.formattedAmount,
                                                    fontFamily = SplitMateTheme.FontDisplay,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp,
                                                    color = SplitMateTheme.PrimaryDark
                                                )
                                            }

                                            if (matchingTransfer != null && canMarkPaidIn) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    if (summary.isCurrentUser && debtorPhone.length >= 10) {
                                                        OutlinedButton(
                                                            onClick = {
                                                                val waPhone = if (debtorPhone.startsWith("+")) {
                                                                    debtorPhone.removePrefix("+")
                                                                } else {
                                                                    "91$debtorPhone"
                                                                }
                                                                val whatsappUri = Uri.parse(
                                                                    "https://api.whatsapp.com/send?phone=" +
                                                                        waPhone +
                                                                        "&text=" +
                                                                        Uri.encode(
                                                                            "Hey ${leg.counterpartyName}, friendly reminder for your ₹${matchingTransfer.amount} share on SplitMate."
                                                                        )
                                                                )
                                                                runCatching {
                                                                    context.startActivity(Intent(Intent.ACTION_VIEW, whatsappUri))
                                                                }.onFailure {
                                                                    Toast.makeText(context, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show()
                                                                }
                                                            },
                                                            shape = SplitMateTheme.RadiusButton,
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SplitMateTheme.PrimaryDark),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(40.dp),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Rounded.Chat,
                                                                contentDescription = null,
                                                                tint = SplitMateTheme.PrimaryDark,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "Remind",
                                                                fontFamily = SplitMateTheme.FontRounded,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }

                                                    var isDrainingIn by remember { mutableStateOf(false) }
                                                    val drainScopeIn = rememberCoroutineScope()
                                                    val drainHapticIn = LocalHapticFeedback.current
                                                    val performMarkPaidIn: () -> Unit = {
                                                        if (!isDrainingIn) {
                                                            isDrainingIn = true
                                                            drainHapticIn.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            drainScopeIn.launch {
                                                                kotlinx.coroutines.delay(140L)
                                                                drainHapticIn.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                kotlinx.coroutines.delay(180L)
                                                                viewModel.markGreedyTransferSettled(matchingTransfer.transfer)
                                                                isDrainingIn = false
                                                            }
                                                        }
                                                    }
                                                    SplitButtonLayout(
                                                        leadingText = if (isDrainingIn) "₹0.00 · Settled" else "Mark Paid",
                                                        leadingIcon = Icons.Rounded.Check,
                                                        onLeadingClick = performMarkPaidIn,
                                                        menuItems = listOf(
                                                            ExpressiveMenuAction(
                                                                label = "Confirm Payment Received",
                                                                icon = Icons.Rounded.CheckCircle,
                                                                onClick = performMarkPaidIn
                                                            ),
                                                            ExpressiveMenuAction(
                                                                label = "Remind via WhatsApp",
                                                                icon = Icons.AutoMirrored.Rounded.Chat,
                                                                onClick = {
                                                                    val waPhone = if (debtorPhone.startsWith("+")) {
                                                                        debtorPhone.removePrefix("+")
                                                                    } else if (debtorPhone.length >= 10) {
                                                                        "91$debtorPhone"
                                                                    } else {
                                                                        ""
                                                                    }
                                                                    val whatsappUri = Uri.parse(
                                                                        "https://api.whatsapp.com/send?" +
                                                                            (if (waPhone.isNotBlank()) "phone=$waPhone&" else "") +
                                                                            "text=" +
                                                                            Uri.encode(
                                                                                "Hey ${leg.counterpartyName}, friendly reminder for your ₹${matchingTransfer.amount} share on SplitMate."
                                                                            )
                                                                    )
                                                                    runCatching {
                                                                        context.startActivity(Intent(Intent.ACTION_VIEW, whatsappUri))
                                                                    }.onFailure {
                                                                        Toast.makeText(context, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            )
                                                        ),
                                                        modifier = Modifier.weight(1f),
                                                        containerColor = if (isDrainingIn) SplitMateTheme.SageText else SplitMateTheme.PrimaryDark,
                                                        contentColor = SplitMateTheme.ScreenBg
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (summary.hasBothDirections) {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // B. If the member has OUTGOING payments (PAYS Card with Multi-Payment Breakdown)
                if (summary.hasOutgoing) {
                    var isPayerExpanded by remember(summary.memberId) { mutableStateOf(true) }
                    val peachBoxBg = when {
                        SplitMateTheme.isDark -> SplitMateTheme.SurfaceMuted
                        DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU -> Color(0xFFFEF3C7)
                        else -> Color(0xFFFDF2EE)
                    }
                    val peachBoxBorder = when {
                        SplitMateTheme.isDark -> SplitMateTheme.BorderLight
                        DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU -> Color(0xFFF59E0B).copy(alpha = 0.45f)
                        else -> Color(0xFFF7E0D7)
                    }

                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SplitMateTheme.BorderLight, RoundedCornerShape(22.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isPayerExpanded = !isPayerExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AvatarToken(
                                        initials = summary.avatarSeed,
                                        bg = SplitMateTheme.TerracottaSurface,
                                        textColor = SplitMateTheme.TerracottaText,
                                        size = 44
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (summary.isCurrentUser) "${summary.memberName} (You)" else summary.memberName,
                                                fontFamily = SplitMateTheme.FontDisplay,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp,
                                                color = SplitMateTheme.PrimaryDark
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = SplitMateTheme.RadiusBadge,
                                                color = SplitMateTheme.TerracottaSurface
                                            ) {
                                                Text(
                                                    text = if (summary.isCurrentUser) "YOU PAY" else "PAYS",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = SplitMateTheme.TerracottaText,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(3.dp))

                                        if (summary.hasMultipleOutgoing) {
                                            Text(
                                                text = "Total to pay",
                                                fontSize = 12.sp,
                                                color = SplitMateTheme.TextSecondary
                                            )
                                            Text(
                                                text = summary.formattedTotalOutgoing,
                                                fontFamily = SplitMateTheme.FontDisplay,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp,
                                                color = SplitMateTheme.TerracottaText
                                            )
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Total to pay  ",
                                                    fontSize = 12.sp,
                                                    color = SplitMateTheme.TextSecondary
                                                )
                                                Text(
                                                    text = summary.formattedTotalOutgoing,
                                                    fontFamily = SplitMateTheme.FontDisplay,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp,
                                                    color = SplitMateTheme.TerracottaText
                                                )
                                            }
                                        }
                                    }
                                }

                                Icon(
                                    imageVector = if (isPayerExpanded) {
                                        Icons.Rounded.KeyboardArrowUp
                                    } else {
                                        Icons.Rounded.KeyboardArrowDown
                                    },
                                    contentDescription = if (isPayerExpanded) "Collapse" else "Expand",
                                    tint = SplitMateTheme.TextSecondary,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(20.dp)
                                )
                            }

                            if (isPayerExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = peachBoxBg,
                                    border = BorderStroke(1.dp, peachBoxBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                        if (summary.hasMultipleOutgoing) {
                                            Text(
                                                text = "Pays to (${summary.outgoingPayments.size} people)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SplitMateTheme.TextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }

                                        summary.outgoingPayments.forEachIndexed { legIdx, leg ->
                                            val matchingTransfer = settlementPlan.find {
                                                it.fromMemberId == summary.memberId && it.toMemberId == leg.counterpartyMemberId
                                            }
                                            val toRoomMember = uiState.members.find { it.memberId == leg.counterpartyMemberId }
                                            val canMarkPaidOut = matchingTransfer != null && viewModel.canCurrentUserMarkTransferPaid(
                                                groupId = uiState.activeGroupId,
                                                toMemberId = matchingTransfer.transfer.toMemberId,
                                                state = uiState
                                            )
                                            val counterpartyFirstName = leg.counterpartyName.trim().substringBefore(" ").ifBlank { leg.counterpartyName }

                                            if (legIdx > 0) {
                                                HorizontalDivider(
                                                    color = peachBoxBorder,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            }

                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        if (summary.hasMultipleOutgoing) {
                                                            AvatarToken(
                                                                initials = toRoomMember?.avatarSeed ?: leg.counterpartyName,
                                                                bg = SplitMateTheme.SageSurface,
                                                                textColor = SplitMateTheme.SageText,
                                                                size = 28
                                                            )
                                                            Spacer(modifier = Modifier.width(10.dp))
                                                            Text(
                                                                text = leg.counterpartyName,
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = SplitMateTheme.PrimaryDark
                                                            )
                                                        } else {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(28.dp)
                                                                    .clip(CircleShape)
                                                                    .background(SplitMateTheme.SageSurface),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.Groups,
                                                                    contentDescription = null,
                                                                    tint = SplitMateTheme.SageText,
                                                                    modifier = Modifier.size(15.dp)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(10.dp))
                                                            Text(
                                                                text = "To ",
                                                                fontSize = 13.sp,
                                                                color = SplitMateTheme.TextSecondary
                                                            )
                                                            Text(
                                                                text = leg.counterpartyName,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = SplitMateTheme.PrimaryDark
                                                            )
                                                        }
                                                    }

                                                    Column(
                                                        horizontalAlignment = Alignment.End,
                                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Text(
                                                            text = leg.formattedAmount,
                                                            fontFamily = SplitMateTheme.FontDisplay,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 14.sp,
                                                            color = SplitMateTheme.PrimaryDark
                                                        )
                                                        if (summary.isCurrentUser && matchingTransfer != null && !canMarkPaidOut) {
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = SplitMateTheme.TerracottaSurface
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Rounded.Schedule,
                                                                        contentDescription = null,
                                                                        tint = SplitMateTheme.TerracottaText,
                                                                        modifier = Modifier.size(10.dp)
                                                                    )
                                                                    Text(
                                                                        text = "Awaiting $counterpartyFirstName",
                                                                        fontFamily = SplitMateTheme.FontRounded,
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 9.sp,
                                                                        color = SplitMateTheme.TerracottaText
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                if (matchingTransfer != null && canMarkPaidOut) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    var isDrainingOut by remember { mutableStateOf(false) }
                                                    val drainScopeOut = rememberCoroutineScope()
                                                    val drainHapticOut = LocalHapticFeedback.current
                                                    val creditorPhone = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(
                                                        userPhone = toRoomMember?.userPhone.orEmpty(),
                                                        upiId = toRoomMember?.upiId.orEmpty()
                                                    )
                                                    val performMarkPaidOut: () -> Unit = {
                                                        if (!isDrainingOut) {
                                                            isDrainingOut = true
                                                            drainHapticOut.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            drainScopeOut.launch {
                                                                kotlinx.coroutines.delay(140L)
                                                                drainHapticOut.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                kotlinx.coroutines.delay(180L)
                                                                viewModel.markGreedyTransferSettled(matchingTransfer.transfer)
                                                                isDrainingOut = false
                                                            }
                                                        }
                                                    }
                                                    SplitButtonLayout(
                                                        leadingText = if (isDrainingOut) "₹0.00 · Settled" else "Mark Paid",
                                                        leadingIcon = Icons.Rounded.Check,
                                                        onLeadingClick = performMarkPaidOut,
                                                        menuItems = listOf(
                                                            ExpressiveMenuAction(
                                                                label = "Confirm Payment Received",
                                                                icon = Icons.Rounded.CheckCircle,
                                                                onClick = performMarkPaidOut
                                                            ),
                                                            ExpressiveMenuAction(
                                                                label = "Remind via WhatsApp",
                                                                icon = Icons.AutoMirrored.Rounded.Chat,
                                                                onClick = {
                                                                    val waPhone = if (creditorPhone.startsWith("+")) {
                                                                        creditorPhone.removePrefix("+")
                                                                    } else if (creditorPhone.length >= 10) {
                                                                        "91$creditorPhone"
                                                                    } else {
                                                                        ""
                                                                    }
                                                                    val whatsappUri = Uri.parse(
                                                                        "https://api.whatsapp.com/send?" +
                                                                            (if (waPhone.isNotBlank()) "phone=$waPhone&" else "") +
                                                                            "text=" +
                                                                            Uri.encode(
                                                                                "Hey ${leg.counterpartyName}, settling ₹${matchingTransfer.amount} on SplitMate."
                                                                            )
                                                                    )
                                                                    runCatching {
                                                                        context.startActivity(Intent(Intent.ACTION_VIEW, whatsappUri))
                                                                    }.onFailure {
                                                                        Toast.makeText(context, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            )
                                                        ),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        containerColor = if (isDrainingOut) SplitMateTheme.SageText else SplitMateTheme.PrimaryDark,
                                                        contentColor = SplitMateTheme.ScreenBg
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // v2.3.3: other travelers exist but none match the active filter -> explain instead of a blank gap.
            val othersFilterEmptyState = yourSettlementsPresentation.othersFilterEmptyState
            if (othersFilterEmptyState != null) {
                item(key = "other_travelers_filter_empty") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Other Travelers' Settlements",
                            fontFamily = SplitMateTheme.FontDisplay,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateTheme.PrimaryDark,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        SettlementFilterEmptyStateCard(
                            state = othersFilterEmptyState,
                            onSwitchFilter = { selectedBalanceFilterIndex = it }
                        )
                    }
                }
            }
        }

        // 5. HERO MOMENT 1: "All Settled Up" Harmony Seal (Morph(Cookie9Sided, Sunny) + Wavy Progress + Spring Physics)
        if (settlementPlan.isEmpty()) {
            item(key = "all_settled_harmony_seal_card") {
                val harmonyMorph = remember {
                    Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny)
                }
                val harmonyInfinite = rememberInfiniteTransition(label = "HarmonySealMorphTransition")
                val harmonyMorphProgress by harmonyInfinite.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "HarmonySealMorphProgress"
                )
                val sealScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = SplitMateMotion.slowSpatial(),
                    label = "HarmonySealScale"
                )
                Surface(
                    shape = SplitMateTheme.RadiusCard,
                    color = SplitMateTheme.SageSurface,
                    border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(sealScale)
                ) {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularWavyProgressIndicator(
                                        progress = { 1f },
                                        modifier = Modifier.fillMaxSize(),
                                        color = SplitMateTheme.SageText,
                                        trackColor = SplitMateTheme.SurfaceWhite.copy(alpha = 0.6f),
                                        strokeWidth = 2.5.dp,
                                        amplitude = 0.8f,
                                        wavelength = 18.dp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(MorphPolygonShape(morph = harmonyMorph, percentage = harmonyMorphProgress))
                                            .background(SplitMateTheme.SurfaceWhite),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = SplitMateTheme.SageText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "All Accounts Balanced",
                                        fontFamily = SplitMateTheme.FontDisplay,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = SplitMateTheme.PrimaryDark
                                    )
                                    Text(
                                        text = "Zero open balances across group members",
                                        fontFamily = SplitMateTheme.FontRounded,
                                        fontSize = 11.sp,
                                        color = SplitMateTheme.TextSecondary
                                    )
                                }
                            }
                            Surface(
                                shape = SplitMateTheme.RadiusBadge,
                                color = SplitMateTheme.SurfaceWhite.copy(alpha = 0.9f)
                            ) {
                                Text(
                                    text = "₹0.00",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = SplitMateTheme.SageText,
                                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        LinearWavyProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = SplitMateTheme.SageText,
                            trackColor = SplitMateTheme.SurfaceWhite.copy(alpha = 0.55f),
                            amplitude = 0.8f,
                            wavelength = 20.dp,
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            }
        }

        // 6. PERMANENT SETTLED & PAID HISTORY RECORD (Clean overlapping avatars + 2-line hierarchy + tap row for Undo Sheet)
        val resolvedActiveGroupIdForSettlements = uiState.activeGroup?.groupId ?: uiState.activeGroupId
        val recordedSettlements = uiState.settlements.filter { it.groupId == resolvedActiveGroupIdForSettlements }
        item {
            var selectedSettlementForSheet by remember { mutableStateOf<com.splitmate.app.data.SettlementEntity?>(null) }

            Card(
                shape = SplitMateTheme.RadiusCard,
                colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SplitMateTheme.SurfaceMuted),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.TaskAlt,
                                    contentDescription = null,
                                    tint = SplitMateTheme.PrimaryDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Settled & Paid History",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SplitMateTheme.PrimaryDark
                                )
                                Text(
                                    text = "Tap any completed payment for receipt details or undo",
                                    fontSize = 11.sp,
                                    color = SplitMateTheme.TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = SplitMateTheme.RadiusBadge,
                            color = SplitMateTheme.SurfaceMuted
                        ) {
                            Text(
                                text = "${recordedSettlements.size} Paid",
                                fontFamily = SplitMateTheme.FontRounded,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SplitMateTheme.PrimaryDark,
                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (recordedSettlements.isEmpty()) {
                        Surface(
                            shape = SplitMateTheme.RadiusPanel,
                            color = SplitMateTheme.SurfaceMuted,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = SplitMateTheme.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "When you tap 'Mark Paid' on any settlement above, it will be saved here as a permanent receipt record.",
                                    fontSize = 11.sp,
                                    color = SplitMateTheme.TextSecondary
                                )
                            }
                        }
                    } else {
                        val dateFormatter = remember {
                            java.text.SimpleDateFormat("dd MMM · hh:mm a", Locale.US)
                        }
                        recordedSettlements.forEachIndexed { idx, settlement ->
                            val fromMbr = uiState.members.find { it.memberId == settlement.fromMemberId }
                            val toMbr = uiState.members.find { it.memberId == settlement.toMemberId }
                            val fromShortName = (fromMbr?.name ?: "Member").substringBefore(" ")
                            val toShortName = (toMbr?.name ?: "Member").substringBefore(" ")
                            val formattedPaid = formatIndianRupeesFromCents(
                                cents = settlement.amountCents,
                                includePlusSign = false,
                                currencySymbol = "₹"
                            )
                            val formattedDate = remember(settlement.settledAt) {
                                dateFormatter.format(java.util.Date(settlement.settledAt))
                            }

                            if (idx > 0) {
                                HorizontalDivider(
                                    color = SplitMateTheme.BorderLight,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }

                            val historyRowShape = segmentedIslandItemShape(
                                index = idx,
                                totalCount = recordedSettlements.size,
                                isSelected = false,
                                outerCorner = 16.dp,
                                innerCorner = 6.dp
                            )
                            Surface(
                                onClick = { selectedSettlementForSheet = settlement },
                                shape = historyRowShape,
                                color = Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                                    ) {
                                        // Two Overlapping Circular Avatars (Payer & Receiver)
                                        Box(modifier = Modifier.width(52.dp).height(34.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.CenterStart)
                                                    .border(1.5.dp, SplitMateTheme.SurfaceWhite, CircleShape)
                                            ) {
                                                AvatarToken(
                                                    initials = fromMbr?.avatarSeed?.ifBlank { fromShortName.take(2) } ?: fromShortName.take(2),
                                                    bg = SplitMateTheme.SurfaceMuted,
                                                    textColor = SplitMateTheme.PrimaryDark,
                                                    size = 32
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .border(1.5.dp, SplitMateTheme.SurfaceWhite, CircleShape)
                                            ) {
                                                AvatarToken(
                                                    initials = toMbr?.avatarSeed?.ifBlank { toShortName.take(2) } ?: toShortName.take(2),
                                                    bg = SplitMateTheme.SageSurface,
                                                    textColor = SplitMateTheme.SageText,
                                                    size = 32
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "$fromShortName paid $toShortName",
                                                fontFamily = SplitMateTheme.FontDisplay,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = SplitMateTheme.PrimaryDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formattedDate,
                                                fontFamily = SplitMateTheme.FontRounded,
                                                fontSize = 11.sp,
                                                color = SplitMateTheme.TextSecondary,
                                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                                            )
                                        }
                                    }

                                    // Right side dedicated solely to the settled amount (tabular figures)
                                    Text(
                                        text = formattedPaid,
                                        fontFamily = SplitMateTheme.FontDisplay,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = SplitMateTheme.PrimaryDark,
                                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Contextual Settlement Detail & Undo Bottom Sheet
            selectedSettlementForSheet?.let { selectedSettlement ->
                val fromMbr = uiState.members.find { it.memberId == selectedSettlement.fromMemberId }
                val toMbr = uiState.members.find { it.memberId == selectedSettlement.toMemberId }
                val fromFullName = fromMbr?.name ?: "Member"
                val toFullName = toMbr?.name ?: "Member"
                val formattedPaid = formatIndianRupeesFromCents(
                    cents = selectedSettlement.amountCents,
                    includePlusSign = false,
                    currencySymbol = "₹"
                )
                val fullDateStr = remember(selectedSettlement.settledAt) {
                    java.text.SimpleDateFormat("dd MMM yyyy · hh:mm a", Locale.US).format(java.util.Date(selectedSettlement.settledAt))
                }
                ModalBottomSheet(
                    onDismissRequest = { selectedSettlementForSheet = null },
                    containerColor = SplitMateTheme.SurfaceWhite,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "$fromFullName paid $toFullName",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = SplitMateTheme.PrimaryDark
                                )
                                Text(
                                    text = "Settled on $fullDateStr",
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 12.sp,
                                    color = SplitMateTheme.TextSecondary,
                                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                                )
                            }
                            Text(
                                text = formattedPaid,
                                fontFamily = SplitMateTheme.FontDisplay,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = SplitMateTheme.SageText,
                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.undoSettlement(selectedSettlement.settlementId)
                                selectedSettlementForSheet = null
                            },
                            shape = SplitMateTheme.RadiusButton,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SplitMateTheme.TerracottaText
                            ),
                            border = BorderStroke(1.dp, SplitMateTheme.TerracottaSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Undo,
                                contentDescription = "Undo Settlement",
                                tint = SplitMateTheme.TerracottaText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Undo / Rollback Settlement",
                                fontFamily = SplitMateTheme.FontRounded,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = SplitMateTheme.TerracottaText
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

// ==============================================================================
// TAB 4: ACTIVITY & HISTORY (Chronological Timeline + Flattened Cards + Detail Sheet)
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AuditVaultScreen(
    viewModel: SplitMateViewModel,
    onOpenPnrWithTicket: (groupId: String, pnr: String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activePalette = DesignSystemBindings.activePalette
    val haptic = LocalHapticFeedback.current
    val sym = "₹"
    var selectedGroupFilterId by remember { mutableStateOf<String?>(null) }
    var selectedExpenseForDetailSheet by remember { mutableStateOf<com.splitmate.app.data.ExpenseEntity?>(null) }
    var editingExpenseEntity by remember { mutableStateOf<com.splitmate.app.data.ExpenseEntity?>(null) }

    // M3 Expressive spring-driven swipe & neighbour-pull physics state
    var activeDraggingExpenseId by remember { mutableStateOf<String?>(null) }
    var activeDragOffsetPx by remember { mutableFloatStateOf(0f) }

    val filteredExpenses = remember(uiState.expenses, selectedGroupFilterId) {
        val base = if (selectedGroupFilterId == null) {
            uiState.expenses
        } else {
            uiState.expenses.filter { it.groupId == selectedGroupFilterId }
        }
        base.sortedWith(compareByDescending<com.splitmate.app.data.ExpenseEntity> { it.createdAt }.thenByDescending { it.expenseId })
    }

    // Group expenses chronologically by day with friendly labels ("Today · 29 Sep", "Yesterday · 28 Sep", etc.)
    val groupedExpensesByDate = remember(filteredExpenses) {
        val dayKeyFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val displayDayFormat = java.text.SimpleDateFormat("dd MMM", Locale.US)
        val todayKey = dayKeyFormat.format(java.util.Date())
        val yesterdayKey = dayKeyFormat.format(java.util.Date(System.currentTimeMillis() - 86_400_000L))

        filteredExpenses.groupBy { exp ->
            val ts = if (exp.createdAt > 0L) exp.createdAt else System.currentTimeMillis()
            val d = java.util.Date(ts)
            val key = dayKeyFormat.format(d)
            val shortDate = displayDayFormat.format(d)
            when (key) {
                todayKey -> "Today · $shortDate"
                yesterdayKey -> "Yesterday · $shortDate"
                else -> shortDate
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // 1. Header + Direct Horizontal Tonal Filter Chip Strip
        item(key = "audit_vault_top_header") {
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                Text(
                    text = "Activity & History",
                    fontFamily = SplitMateTheme.FontDisplay,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.6).sp,
                    color = SplitMateTheme.PrimaryDark
                )
                Text(
                    text = "Tap any row for split breakdown · Swipe left for boarding pass or quick edit",
                    fontFamily = SplitMateTheme.FontRounded,
                    fontSize = 12.sp,
                    color = SplitMateTheme.TextSecondary
                )

                if (uiState.groups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item(key = "filter_chip_all_groups") {
                            val allSelected = selectedGroupFilterId == null
                            val allCornerRadius by animateDpAsState(
                                targetValue = if (allSelected) 12.dp else 20.dp,
                                animationSpec = SplitMateMotion.fastSpatial(),
                                label = "allGroupsChipCornerMorph"
                            )
                            val allChipBg by animateColorAsState(
                                targetValue = if (allSelected) activePalette.tertiaryContainer else activePalette.surfaceContainerLowest,
                                animationSpec = SplitMateMotion.fastEffects(),
                                label = "allGroupsChipBg"
                            )
                            val allChipContent by animateColorAsState(
                                targetValue = if (allSelected) activePalette.onTertiaryContainer else activePalette.onSurfaceVariant,
                                animationSpec = SplitMateMotion.fastEffects(),
                                label = "allGroupsChipContent"
                            )
                            val allChipBorder by animateColorAsState(
                                targetValue = if (allSelected) activePalette.tertiary.copy(alpha = 0.45f) else activePalette.outlineVariant,
                                animationSpec = SplitMateMotion.fastEffects(),
                                label = "allGroupsChipBorder"
                            )
                            Surface(
                                onClick = { selectedGroupFilterId = null },
                                shape = RoundedCornerShape(allCornerRadius),
                                color = allChipBg,
                                border = BorderStroke(1.dp, allChipBorder),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .heightIn(min = 36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (allSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = allChipContent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "All Groups (${uiState.expenses.size})",
                                        fontFamily = SplitMateTheme.FontRounded,
                                        fontSize = 12.sp,
                                        fontWeight = if (allSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = allChipContent,
                                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                                    )
                                }
                            }
                        }
                        items(uiState.groups, key = { it.groupId }) { grp ->
                            val isSelected = selectedGroupFilterId == grp.groupId
                            val count = uiState.expenses.count { it.groupId == grp.groupId }
                            val chipCornerRadius by animateDpAsState(
                                targetValue = if (isSelected) 12.dp else 20.dp,
                                animationSpec = SplitMateMotion.fastSpatial(),
                                label = "groupFilterChipCornerMorph"
                            )
                            val chipBg by animateColorAsState(
                                targetValue = if (isSelected) activePalette.tertiaryContainer else activePalette.surfaceContainerLowest,
                                animationSpec = SplitMateMotion.fastEffects(),
                                label = "groupFilterChipBg"
                            )
                            val chipContent by animateColorAsState(
                                targetValue = if (isSelected) activePalette.onTertiaryContainer else activePalette.onSurfaceVariant,
                                animationSpec = SplitMateMotion.fastEffects(),
                                label = "groupFilterChipContent"
                            )
                            val chipBorder by animateColorAsState(
                                targetValue = if (isSelected) activePalette.tertiary.copy(alpha = 0.45f) else activePalette.outlineVariant,
                                animationSpec = SplitMateMotion.fastEffects(),
                                label = "groupFilterChipBorder"
                            )
                            Surface(
                                onClick = { selectedGroupFilterId = if (isSelected) null else grp.groupId },
                                shape = RoundedCornerShape(chipCornerRadius),
                                color = chipBg,
                                border = BorderStroke(1.dp, chipBorder),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .heightIn(min = 36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = chipContent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "${grp.name.toSmartTitleCase()} ($count)",
                                        fontFamily = SplitMateTheme.FontRounded,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = chipContent,
                                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (filteredExpenses.isEmpty()) {
            item(key = "audit_vault_empty_state") {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SplitMateTheme.BorderLight, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null, tint = SplitMateTheme.SageText, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No expenses recorded yet", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SplitMateTheme.PrimaryDark)
                        Text("Expenses you split will appear here chronologically.", fontSize = 12.sp, color = SplitMateTheme.TextSecondary)
                    }
                }
            }
        }

        // 2. Editorial Sticky Date Headers + Day-Grouped Dynamic Corner Radius Islands (24dp outer / 4dp inner)
        groupedExpensesByDate.forEach { (dateHeaderLabel, dateExpenses) ->
            val dayTotalCents = dateExpenses.sumOf { it.totalAmountCents }
            val formattedDayTotal = formatIndianRupeesFromCents(
                cents = dayTotalCents,
                includePlusSign = false,
                currencySymbol = sym
            )

            stickyHeader(key = "date_header_$dateHeaderLabel") {
                Surface(
                    color = SplitMateTheme.ScreenBg.copy(alpha = 0.96f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dateHeaderLabel,
                            style = SplitMateExpressiveTypography.headlineMediumEmphasized,
                            fontFamily = SplitMateTheme.FontDisplay,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            color = SplitMateTheme.PrimaryDark
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = activePalette.surfaceContainerHigh,
                            border = BorderStroke(1.dp, activePalette.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "${if (dateExpenses.size == 1) "1 entry" else "${dateExpenses.size} entries"} · $formattedDayTotal",
                                fontFamily = SplitMateTheme.FontRounded,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activePalette.onSurfaceVariant,
                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            itemsIndexed(dateExpenses, key = { _, it -> it.expenseId }) { expIdx, expense ->
                val payer = uiState.members.find { it.memberId == expense.payerId }
                val group = uiState.groups.find { it.groupId == expense.groupId }
                val groupMembers = uiState.members.filter { it.groupId == expense.groupId }
                val breakdown = remember(expense, groupMembers, uiState.splits, sym) {
                    SplitMateViewModel.resolveExpenseSplitBreakdown(
                        expense = expense,
                        groupMembers = groupMembers,
                        allSplits = uiState.splits,
                        currencySymbol = sym,
                        headerPrefix = "Individual Share Breakdown"
                    )
                }
                val perPersonShare = breakdown.perPersonHeadlineShare
                val isMePayer = payer?.isCurrentUser == true
                val signedCents = if (isMePayer) expense.totalAmountCents else -expense.totalAmountCents
                val formattedSignedTotal = formatIndianRupeesFromCents(
                    cents = signedCents,
                    includePlusSign = true,
                    currencySymbol = sym
                )
                val parsedTravelTicket = remember(expense.title) {
                    extractTravelTicketFromTitle(expense.title)
                }
                val hasTravelPass = remember(expense.title, expense.expenseCategory, parsedTravelTicket) {
                    parsedTravelTicket != null && (
                        parsedTravelTicket.pnr.isNotBlank() ||
                            isFlightTicketExpense(expense.title, parsedTravelTicket) ||
                            expense.expenseCategory.equals("FLIGHT", ignoreCase = true)
                        )
                }
                val displayExpenseTitle = remember(expense.title) {
                    cleanDisplayExpenseTitle(expense.title)
                }
                val payerFirstName = if (isMePayer) "You" else (payer?.name?.substringBefore(" ") ?: "Member")

                // Neighbour-pull spring physics calculation within the same day block
                val draggedIdxInDay = remember(activeDraggingExpenseId, dateExpenses) {
                    dateExpenses.indexOfFirst { it.expenseId == activeDraggingExpenseId }
                }
                val isThisCardDragging = activeDraggingExpenseId == expense.expenseId
                val targetCardOffsetPx = when {
                    isThisCardDragging -> activeDragOffsetPx
                    draggedIdxInDay >= 0 && kotlin.math.abs(expIdx - draggedIdxInDay) == 1 -> activeDragOffsetPx * 0.18f
                    draggedIdxInDay >= 0 && kotlin.math.abs(expIdx - draggedIdxInDay) == 2 -> activeDragOffsetPx * 0.06f
                    else -> 0f
                }
                val animatedCardOffsetPx by animateFloatAsState(
                    targetValue = targetCardOffsetPx,
                    animationSpec = SplitMateMotion.fastSpatial(),
                    label = "auditCardSwipeOffset_${expense.expenseId}"
                )
                val isActivelyPulled = kotlin.math.abs(animatedCardOffsetPx) > 10f

                // M3 Expressive Dynamic Corner Radii: 24dp outer caps, 4dp inner seams (morphing to 18dp when swiped)
                val auditIslandShape = segmentedIslandItemShape(
                    index = expIdx,
                    totalCount = dateExpenses.size,
                    isSelected = isActivelyPulled,
                    outerCorner = 24.dp,
                    innerCorner = 4.dp
                )

                val swipeThresholdPx = -170f
                val swipeProgress = (kotlin.math.abs(animatedCardOffsetPx) / kotlin.math.abs(swipeThresholdPx)).coerceIn(0f, 1f)

                val triggerPrimarySwipeAction = {
                    if (hasTravelPass && parsedTravelTicket != null) {
                        val gId = expense.groupId
                        val isFlightSel = isFlightTicketExpense(expense.title, parsedTravelTicket) ||
                            expense.expenseCategory.equals("FLIGHT", ignoreCase = true)
                        val pnrCode = if (isFlightSel) "EXPENSE:${expense.expenseId}" else parsedTravelTicket.pnr
                        onOpenPnrWithTicket(gId, pnrCode)
                    } else {
                        selectedExpenseForDetailSheet = expense
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    // Hidden High-Contrast M3 Expressive Swipe Action Revealed Behind Card
                    if (animatedCardOffsetPx < -6f) {
                        val actionPillColor = if (hasTravelPass) activePalette.tertiary else activePalette.primary
                        val actionContentColor = if (hasTravelPass) activePalette.onTertiary else activePalette.onPrimary
                        val actionIcon = when {
                            hasTravelPass && (isFlightTicketExpense(expense.title, parsedTravelTicket) || expense.expenseCategory.equals("FLIGHT", ignoreCase = true)) -> Icons.Rounded.FlightTakeoff
                            hasTravelPass -> Icons.Rounded.Train
                            else -> Icons.AutoMirrored.Rounded.ReceiptLong
                        }
                        val actionLabel = if (hasTravelPass) "Boarding Pass" else "Breakdown"

                        Surface(
                            onClick = triggerPrimarySwipeAction,
                            shape = RoundedCornerShape(18.dp),
                            color = actionPillColor,
                            contentColor = actionContentColor,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .graphicsLayer {
                                    alpha = swipeProgress
                                    val sc = 0.82f + (0.18f * swipeProgress)
                                    scaleX = sc
                                    scaleY = sc
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = actionIcon,
                                    contentDescription = actionLabel,
                                    tint = actionContentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = actionLabel,
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = actionContentColor
                                )
                            }
                        }
                    }

                    // Foreground Transaction Card with Spring-Driven Swipe & Neighbour-Pull Physics
                    Card(
                        onClick = { selectedExpenseForDetailSheet = expense },
                        shape = auditIslandShape,
                        colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationX = animatedCardOffsetPx
                            }
                            .pointerInput(expense.expenseId) {
                                var hasTriggeredThresholdHaptic = false
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        activeDraggingExpenseId = expense.expenseId
                                        activeDragOffsetPx = 0f
                                        hasTriggeredThresholdHaptic = false
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        val nextOffset = (activeDragOffsetPx + dragAmount * 0.85f).coerceIn(-240f, 24f)
                                        activeDragOffsetPx = nextOffset
                                        if (nextOffset <= swipeThresholdPx && !hasTriggeredThresholdHaptic) {
                                            hasTriggeredThresholdHaptic = true
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } else if (nextOffset > swipeThresholdPx && hasTriggeredThresholdHaptic) {
                                            hasTriggeredThresholdHaptic = false
                                        }
                                    },
                                    onDragEnd = {
                                        val finalOffset = activeDragOffsetPx
                                        activeDraggingExpenseId = null
                                        activeDragOffsetPx = 0f
                                        if (finalOffset <= swipeThresholdPx) {
                                            triggerPrimarySwipeAction()
                                        }
                                    },
                                    onDragCancel = {
                                        activeDraggingExpenseId = null
                                        activeDragOffsetPx = 0f
                                    }
                                )
                            }
                            .border(1.dp, SplitMateTheme.BorderLight, auditIslandShape)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (auditCatBg, auditCatTint) = resolveExpenseCategoryBadgeColors(expense.title, SplitMateTheme.isDark)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(auditCatBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = resolveExpenseCategoryIcon(expense.title),
                                            contentDescription = null,
                                            tint = auditCatTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = displayExpenseTitle,
                                            fontFamily = SplitMateTheme.FontDisplay,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = SplitMateTheme.PrimaryDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            // v2.3.3: "Personal · not split" replaces "1 splitting (₹x/ea)".
                                            text = breakdown.historyRowSubtitle(
                                                payerName = payerFirstName,
                                                sharedDetail = "${breakdown.splittingMembersCount} splitting ($perPersonShare/ea)"
                                            ),
                                            fontFamily = SplitMateTheme.FontRounded,
                                            fontSize = 12.sp,
                                            color = SplitMateTheme.TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                                        )
                                        if (parsedTravelTicket != null && parsedTravelTicket.pnr.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                shape = SplitMateTheme.RadiusBadge,
                                                color = activePalette.tertiaryContainer.copy(alpha = 0.65f)
                                            ) {
                                                Text(
                                                    text = "PNR ${parsedTravelTicket.pnr} · ${parsedTravelTicket.bookingStatus}",
                                                    fontFamily = SplitMateTheme.FontRounded,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = activePalette.onTertiaryContainer,
                                                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Clean Right Financial Anchor (Zero cluttered inline text links)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formattedSignedTotal,
                                        fontFamily = SplitMateTheme.FontDisplay,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = if (isMePayer) SplitMateTheme.SageText else SplitMateTheme.PrimaryDark,
                                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                                    )
                                    if (!group?.name.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = group?.name?.toSmartTitleCase().orEmpty(),
                                            fontFamily = SplitMateTheme.FontRounded,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SplitMateTheme.TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Contextual Expense Breakdown, Boarding Pass & Edit/Undo Modal Bottom Sheet (Phase 4: ActivityDetailSheet)
    selectedExpenseForDetailSheet?.let { selectedExp ->
        val payer = uiState.members.find { it.memberId == selectedExp.payerId }
        val groupMembers = uiState.members.filter { it.groupId == selectedExp.groupId }
        val breakdown = remember(selectedExp, groupMembers, uiState.splits, sym) {
            SplitMateViewModel.resolveExpenseSplitBreakdown(
                expense = selectedExp,
                groupMembers = groupMembers,
                allSplits = uiState.splits,
                currencySymbol = sym,
                headerPrefix = "Individual Share Breakdown"
            )
        }
        ActivityDetailSheet(
            expense = selectedExp,
            payer = payer,
            breakdown = breakdown,
            currencySymbol = sym,
            onDismiss = { selectedExpenseForDetailSheet = null },
            onOpenBoardingPass = { gId, pnrCode ->
                onOpenPnrWithTicket(gId, pnrCode)
            },
            onEditExpense = { expToEdit ->
                editingExpenseEntity = expToEdit
            },
            onUndoExpense = { idToRollback ->
                viewModel.rollbackExpense(idToRollback)
            }
        )
    }

    if (editingExpenseEntity != null) {
        val exp = editingExpenseEntity!!
        val expGroupMembers = uiState.members.filter { it.groupId == exp.groupId }
        val expExistingSplits = uiState.splits.filter { it.expenseId == exp.expenseId && it.finalOwedCents > 0L }
        EditLoggedExpenseDialog(
            expense = exp,
            groupMembers = expGroupMembers,
            initialSplitMemberIds = expExistingSplits.map { it.memberId }.toSet().ifEmpty { expGroupMembers.map { it.memberId }.toSet() },
            onDismiss = { editingExpenseEntity = null },
            onSave = { newTitle, newRupees, newPayerId, updatedSplitMemberIds ->
                viewModel.editExistingExpense(
                    expenseId = exp.expenseId,
                    newTitle = newTitle,
                    newTotalRupees = newRupees,
                    newPayerId = newPayerId,
                    selectedMemberIds = updatedSplitMemberIds
                )
                editingExpenseEntity = null
            }
        )
    }
}

/**
 * v2.3.3: Context-aware empty state for the "Gets Back" / "Owes" settlement filters. Shown when the
 * filter hides rows but the user is NOT settled; tapping jumps to the filter that has their balance.
 */
@Composable
private fun SettlementFilterEmptyStateCard(
    state: com.splitmate.app.SettlementFilterPresentation.FilterEmptyState,
    onSwitchFilter: (Int) -> Unit
) {
    val targetIndex = state.switchToFilterIndex
    Surface(
        onClick = { if (targetIndex != null) onSwitchFilter(targetIndex) },
        enabled = targetIndex != null,
        shape = RoundedCornerShape(18.dp),
        color = SplitMateTheme.SurfaceMuted,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = when (targetIndex) {
                    com.splitmate.app.SettlementFilterPresentation.FILTER_OWES -> Icons.AutoMirrored.Rounded.TrendingDown
                    com.splitmate.app.SettlementFilterPresentation.FILTER_GETS_BACK -> Icons.AutoMirrored.Rounded.TrendingUp
                    else -> Icons.Rounded.Groups
                },
                contentDescription = null,
                tint = SplitMateTheme.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    fontFamily = SplitMateTheme.FontDisplay,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = SplitMateTheme.PrimaryDark
                )
                Text(
                    text = state.subtitle,
                    fontSize = 11.sp,
                    color = SplitMateTheme.TextSecondary,
                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                )
            }
            if (targetIndex != null) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = SplitMateTheme.TextSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = -90f }
                )
            }
        }
    }
}
