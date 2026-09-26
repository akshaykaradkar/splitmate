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
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.ContactPickerBottomSheet
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.GroupCategoryIcons
import com.splitmate.app.ui.NewGroupMemberDraft
import com.splitmate.app.ui.SplitMateBrandFontFamily
import com.splitmate.app.ui.SplitMateDisplayFontFamily
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.buildDiceBearOpenPeepsUrl
import com.splitmate.app.ui.extractInitialsFromNameOrSeed
import com.splitmate.app.ui.resolveGroupCategoryIcon
import com.splitmate.app.ui.components.ActiveTravelPassMode
import com.splitmate.app.ui.components.AnimatedTransitDeckHeroCard
import com.splitmate.app.ui.screens.EditFriendUpiDialog
import com.splitmate.app.ui.screens.FlightExpenseReviewScreen
import com.splitmate.app.ui.screens.OnboardingSetupScreen
import com.splitmate.app.ui.screens.PnrExpenseReviewScreen
import com.splitmate.app.ui.components.UpiExpressPaymentSheet
import com.splitmate.app.ui.screens.QuickExpenseScreen
import com.splitmate.app.ui.screens.UserSettingsScreen
import com.splitmate.app.ui.toSmartTitleCase
import kotlinx.coroutines.launch
import java.util.Locale

// ==============================================================================
// 1. MATERIAL 3 EXPRESSIVE PALETTE & TOKENS (GM3 Dark Elevation & DesignSystemBindings)
// ==============================================================================
object SplitMateTheme {
    var isDark by mutableStateOf(false)

    val ScreenBg: Color
        get() = if (isDark) DesignSystemBindings.GM3DarkBackground else DesignSystemBindings.GM3LightBackground
    val PrimaryDark: Color
        get() = if (isDark) DesignSystemBindings.GM3DarkPrimaryText else DesignSystemBindings.GM3LightPrimaryText
    val AccentSage = DesignSystemBindings.ElementsPositiveContainer
    val SageSurface: Color
        get() = if (isDark) Color(0xFF233216) else Color(0xFFEAF3DC)
    val SageText: Color
        get() = if (isDark) Color(0xFFD7E8B6) else DesignSystemBindings.ElementsPositiveText
    val TerracottaSurface: Color
        get() = if (isDark) Color(0xFF3A2019) else Color(0xFFFCECE7)
    val TerracottaText: Color
        get() = if (isDark) Color(0xFFFECDD3) else DesignSystemBindings.ElementsNegativeText
    val BrandCoral = Color(0xFFE06B52)
    val SurfaceWhite: Color
        get() = if (isDark) DesignSystemBindings.GM3DarkCardSurface else DesignSystemBindings.GM3LightCardSurface
    val SurfaceMuted: Color
        get() = if (isDark) DesignSystemBindings.GM3DarkKeypadSurface else DesignSystemBindings.GM3LightKeypadSurface
    val BorderLight: Color
        get() = if (isDark) Color(0xFF38312B) else Color(0xFFE6E2D8)
    val TextSecondary: Color
        get() = if (isDark) DesignSystemBindings.GM3DarkSubtitleText else DesignSystemBindings.GM3LightSubtitleText

    val RadiusHero = DesignSystemBindings.GM3ShapeExtraLarge
    val RadiusCard = DesignSystemBindings.GM3ShapeLarge
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
enum class SplitMateTab(val label: String, val icon: ImageVector) {
    LEDGERS("Ledgers", Icons.Rounded.AccountBalanceWallet),
    SPLIT("Split", Icons.AutoMirrored.Rounded.ReceiptLong),
    SETTLE("Settle", Icons.Rounded.SwapHoriz),
    AUDIT("Audit", Icons.Rounded.HistoryEdu)
}

@Composable
fun SplitMateApp(viewModel: SplitMateViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("splitmate_prefs", android.content.Context.MODE_PRIVATE) }
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val activeGroups by viewModel.activeGroups.collectAsStateWithLifecycle()

    // Observe and sync Dark Theme preference across app restarts
    LaunchedEffect(Unit) {
        if (prefs.contains("is_dark_theme")) {
            val savedDark = prefs.getBoolean("is_dark_theme", false)
            if (savedDark != uiState.isDarkTheme) {
                viewModel.toggleDarkTheme(savedDark)
            }
        }
    }
    SplitMateTheme.isDark = uiState.isDarkTheme

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
            OnboardingSetupScreen(
                onCompleteProfile = { name, currency, avatarSeed ->
                    viewModel.completeOnboarding(
                        name = name,
                        countryName = currency.country,
                        currencyCode = "INR",
                        currencySymbol = "₹",
                        avatarSeed = avatarSeed
                    )
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
                avatarSeed = uiState.currentUserSeed,
                upiId = uiState.userUpiId,
                defaultCurrencyCode = "INR (₹)",
                totalBalanceText = totalBalance,
                activeGroupsCount = activeGroups.size,
                isDarkThemeInitial = uiState.isDarkTheme,
                allCurrencies = uiState.currencyRates,
                onSyncLiveRates = {},
                onBackClick = { navController.popBackStack() },
                onUpdateUpiId = { newUpi -> viewModel.updateUserProfile(uiState.currentUserName, uiState.currentUserSeed, newUpi) },
                onUpdateCurrencyCode = {},
                onUpdateUserProfile = { newName, newSeed ->
                    viewModel.updateUserProfile(newName, newSeed, uiState.userUpiId)
                },
                onThemeToggle = { isDark ->
                    prefs.edit().putBoolean("is_dark_theme", isDark).apply()
                    viewModel.toggleDarkTheme(isDark)
                },
                onExportLedgerText = {
                    buildString {
                        appendLine("SplitMate Trip & Ledger Summary")
                        appendLine("User: ${uiState.currentUserName} (${uiState.userUpiId.ifBlank { "UPI not set" }})")
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

@OptIn(ExperimentalMaterial3Api::class)
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

    fun openPnrOrFlightTicket(pnrRaw: String) {
        val normalized = com.splitmate.app.data.PnrNetworkRepository.normalizePnrKey(pnrRaw)
        if (normalized.length == 6) {
            val cachedFlight = com.splitmate.app.data.PnrNetworkRepository.loadConfirmedFlightTicketResult(context, normalized)
            if (cachedFlight != null) {
                activeFlightTicketResult = cachedFlight
                return
            }
            val persistedSnap = com.splitmate.app.data.PnrNetworkRepository.loadPersistedPnrSnapshot(context, normalized)
            if (persistedSnap != null) {
                activeFlightTicketResult = com.splitmate.app.data.UniversalFlightTicketExtractor.UniversalFlightTicketResult(
                    pnr = normalized,
                    otaBookingId = "PNR-$normalized",
                    airlineCode = persistedSnap.trainNo.substringBefore(" "),
                    airlineName = persistedSnap.trainName,
                    flightNumber = persistedSnap.trainNo,
                    originIata = persistedSnap.fromStation,
                    originCity = persistedSnap.fromStationName,
                    originAirportName = persistedSnap.fromStationName,
                    destinationIata = persistedSnap.toStation,
                    destinationCity = persistedSnap.toStationName,
                    destinationAirportName = persistedSnap.toStationName,
                    travelDate = persistedSnap.departureTime.substringBefore("•").trim(),
                    bookingDate = "",
                    departureTime = persistedSnap.departureTime.substringAfter("•", persistedSnap.departureTime).trim(),
                    arrivalTime = persistedSnap.arrivalTime,
                    durationText = persistedSnap.durationText,
                    cabinClass = persistedSnap.travelClass.substringBefore("•").trim().ifBlank { "Economy" },
                    fareType = persistedSnap.quotaText,
                    cabinBaggage = "7 Kgs",
                    checkInBaggage = "15 Kgs",
                    passengers = persistedSnap.structuredPassengers.map { sp ->
                        com.splitmate.app.data.UniversalFlightTicketExtractor.ExtractedFlightPassenger(
                            fullName = sp.passengerNumber,
                            seatNumber = sp.currentStatus.substringAfter("/", "-").trim(),
                            eTicketOrPnr = normalized
                        )
                    },
                    matchedGroupMembers = emptyList(),
                    totalFarePaise = persistedSnap.totalFareRupees.toLong() * 100L,
                    discountSavedPaise = 0L,
                    paymentMethod = "UPI",
                    extractionDurationMs = 0L
                )
                return
            }
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

    Scaffold(
        containerColor = animatedScreenBg,
        floatingActionButton = {
            if (currentTab == SplitMateTab.LEDGERS && !isTripHubCanvasVisible) {
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
                            tint = animatedScreenBg
                        )
                    },
                    text = {
                        Text(
                            text = "Log Expense",
                            fontWeight = FontWeight.ExtraBold,
                            color = animatedScreenBg,
                            fontFamily = SplitMateTheme.FontRounded
                        )
                    },
                    containerColor = animatedPrimaryDark,
                    contentColor = animatedScreenBg,
                    shape = SplitMateTheme.RadiusBadge,
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .shadow(10.dp, SplitMateTheme.RadiusBadge)
                )
            }
        },
        bottomBar = {
            if (!isMediumOrExpandedWindow) {
                AnimatedVisibility(
                    visible = !isImmersiveTripHubOpen,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                            .testTag("HorizontalFloatingToolbar")
                    ) {
                        SplitMateBottomNavigationBar(
                            selectedTab = currentTab,
                            onTabSelected = { tab -> viewModel.selectTab(tab.name) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isMediumOrExpandedWindow) {
                NavigationRail(
                    containerColor = animatedScreenBg,
                    contentColor = SplitMateTheme.PrimaryDark,
                    modifier = Modifier.testTag("AdaptiveNavigationRail")
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SplitMateTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(tab.name) },
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
                                selectedIconColor = Color(0xFF23201E),
                                selectedTextColor = SplitMateTheme.PrimaryDark,
                                indicatorColor = SplitMateTheme.AccentSage,
                                unselectedIconColor = SplitMateTheme.TextSecondary,
                                unselectedTextColor = SplitMateTheme.TextSecondary
                            )
                        )
                    }
                }
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
fun SplitMateBottomNavigationBar(
    selectedTab: SplitMateTab,
    onTabSelected: (SplitMateTab) -> Unit
) {
    val navSurfaceColor by animateColorAsState(
        targetValue = SplitMateTheme.SurfaceWhite,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "BottomNavSurface"
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = navSurfaceColor,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SplitMateTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                val tabCornerRadius by animateDpAsState(
                    targetValue = if (isSelected) 14.dp else 24.dp,
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 480f),
                    label = "navTabCornerMorph"
                )
                Surface(
                    onClick = { onTabSelected(tab) },
                    shape = RoundedCornerShape(tabCornerRadius),
                    color = if (isSelected) SplitMateTheme.AccentSage else Color.Transparent,
                    modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) Color(0xFF23201E) else SplitMateTheme.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        AnimatedVisibility(visible = isSelected) {
                            Text(
                                text = "  ${tab.label}",
                                color = Color(0xFF23201E),
                                fontFamily = SplitMateTheme.FontRounded,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
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
    var editingFriend by remember { mutableStateOf<GroupMemberEntity?>(null) }
    val openedGroupDetailId = uiState.openedGroupDetailId
    var expandedExpenseId by remember { mutableStateOf<String?>(null) }
    var editingExpense by remember { mutableStateOf<com.splitmate.app.data.ExpenseEntity?>(null) }
    var deletingExpense by remember { mutableStateOf<com.splitmate.app.data.ExpenseEntity?>(null) }
    var showAddContactsToExistingGroupSheet by remember { mutableStateOf(false) }
    var showClassicSyncSheet by remember { mutableStateOf(false) }

    // Intercept system Back when viewing inside a specific Group so user returns to All Groups list instead of exiting the app!
    androidx.activity.compose.BackHandler(
        enabled = openedGroupDetailId != null || showClassicSyncSheet || showAddContactsToExistingGroupSheet
    ) {
        if (showClassicSyncSheet) {
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
        val formattedDeleteAmt = "₹${String.format(Locale.US, "%.2f", expToDelete.totalAmountCents / 100.0)}"
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
                                        .clip(CircleShape)
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
                                    text = "₹${String.format(Locale.US, "%.2f", totalGroupSpendCents / 100.0)}",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SplitMateTheme.PrimaryDark
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
                                Text(
                                    text = "Manage All →",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SplitMateTheme.SageText,
                                    modifier = Modifier.clickable { editingFriend = firstEditable }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(groupMembers, key = { it.memberId }) { mbr ->
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
                                            size = 30
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (mbr.isCurrentUser) "${mbr.name} (You)" else mbr.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SplitMateTheme.PrimaryDark
                                            )
                                            val phone = mbr.upiId.substringBefore("@").replace(Regex("[^0-9]"), "")
                                            Text(
                                                text = if (phone.length == 10) "+91 $phone" else if (mbr.isCurrentUser) "Group Admin" else "Tap to link phone",
                                                fontSize = 10.sp,
                                                color = if (phone.length == 10 || mbr.isCurrentUser) SplitMateTheme.SageText else SplitMateTheme.TerracottaText
                                            )
                                        }
                                    }
                                }
                            }
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

                // Bidirectional drag progress (0°..175° regardless of whether user swipes ← Left or → Right)
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
                                                        text = "PNR ${ticket.pnr} (${ticket.fromStation.ifBlank { "ORG" }}→${ticket.toStation.ifBlank { "DST" }} · $formattedFare) ↗",
                                                        fontFamily = SplitMateTheme.FontRounded,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = SplitMateTheme.PrimaryDark
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
                                                onClick = { onOpenPnrWithTicket(ticket.pnr) },
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
                                                        text = "FLIGHT ${ticket.pnr} (${ticket.fromStation.ifBlank { "ORG" }}→${ticket.toStation.ifBlank { "DST" }} · $formattedFare) ↗",
                                                        fontFamily = SplitMateTheme.FontRounded,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = if (SplitMateTheme.isDark) Color(0xFFE6EAFF) else Color(0xFF1F1C4D)
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
                items(groupExpenses, key = { it.expenseId }) { expense ->
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

                    Card(
                        onClick = {
                            expandedExpenseId = if (isExpanded) null else expense.expenseId
                        },
                        shape = SplitMateTheme.RadiusCard,
                        colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
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
                                            text = "Paid by ${payer?.name ?: "You"} · $perPersonShare / person (${breakdown.splittingMembersCount} splitting)",
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
                                    Text(
                                        text = if (isExpanded) "Hide split ▲" else "View split ▼",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFlightCard) {
                                            if (SplitMateTheme.isDark) Color(0xFFA5B4FC) else Color(0xFF3730A3)
                                        } else {
                                            SplitMateTheme.SageText
                                        }
                                    )
                                }
                            }

                            if (parsedTicketInGroup != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                CompactLedgerTicketStub(
                                    ticket = parsedTicketInGroup,
                                    totalAmountDisplay = formattedTotal,
                                    perPersonShareDisplay = perPersonShare,
                                    onInspectTactilePass = {
                                        if (parsedTicketInGroup.pnr.isNotBlank()) {
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
                                Text(
                                    text = breakdown.headerLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SplitMateTheme.PrimaryDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                breakdown.rows.forEach { row ->
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
                                                bg = if (row.isIncludedInSplit) SplitMateTheme.SageSurface else SplitMateTheme.SurfaceMuted,
                                                textColor = SplitMateTheme.SageText,
                                                size = 24
                                            )
                                            Text(
                                                text = row.displayName,
                                                fontSize = 12.sp,
                                                color = if (row.isIncludedInSplit) SplitMateTheme.TextSecondary else SplitMateTheme.TextSecondary.copy(alpha = 0.5f)
                                            )
                                        }
                                        Text(
                                            text = row.formattedShare,
                                            fontSize = 12.sp,
                                            fontWeight = if (row.isIncludedInSplit) FontWeight.Bold else FontWeight.Medium,
                                            color = if (row.isIncludedInSplit) SplitMateTheme.PrimaryDark else SplitMateTheme.TextSecondary.copy(alpha = 0.55f)
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
                                            else -> "Shared Ledger Entry"
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

    val renderMasterGroupListPane: @Composable () -> Unit = {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(DesignSystemBindings.PixelSectionSpacing)
    ) {
        // 1. Custom Top Bar (Subtitle "Fun & Trip Expenses", Clickable Avatar — NO redundant INR pill)
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
                    // 1-Tap Warm Espresso Night (#181512) Theme Toggle (Global across all tabs & Settings)
                    Surface(
                        onClick = {
                            val nextDark = !uiState.isDarkTheme
                            SplitMateTheme.isDark = nextDark
                            context.getSharedPreferences("splitmate_prefs", android.content.Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("is_dark_theme", nextDark)
                                .apply()
                            viewModel.toggleDarkTheme(nextDark)
                        },
                        shape = CircleShape,
                        color = SplitMateTheme.SurfaceWhite,
                        border = BorderStroke(1.dp, SplitMateTheme.BorderLight)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = "Toggle Warm Espresso Night Theme",
                                tint = if (uiState.isDarkTheme) Color(0xFFD7E8B6) else SplitMateTheme.PrimaryDark,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isDarkTheme) "Cream" else "Espresso",
                                fontFamily = SplitMateTheme.FontRounded,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SplitMateTheme.PrimaryDark
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onAvatarSettingsClick)
                    ) {
                        AvatarToken(
                            initials = uiState.currentUserSeed,
                            bg = SplitMateTheme.AccentSage,
                            textColor = Color(0xFF23201E),
                            size = 38
                        )
                    }
                }
            }
        }

        // 2. Hero Balance Card (32dp Radius, Warm Espresso Night Adaptive Gradient, Buckwheat-Inspired 64.sp Numbers)
        item {
            val heroGradientColors = if (SplitMateTheme.isDark) {
                listOf(Color(0xFF233216), Color(0xFF24201C))
            } else {
                listOf(Color(0xFFF5F8EC), Color(0xFFFDF1EC))
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
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.isOfflineMode) SplitMateTheme.BrandCoral else Color(0xFF388E3C))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isOfflineMode) "Offline" else "Live Sync",
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isOfflineMode) SplitMateTheme.BrandCoral else Color(0xFF388E3C)
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
                            text = if (activeGroups.size == 1) "1 active group" else "Across ${activeGroups.size} active groups",
                            fontFamily = SplitMateTheme.FontRounded,
                            fontSize = 13.sp,
                            letterSpacing = 0.sp,
                            color = SplitMateTheme.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Single primary action inside Hero Balance Card (+ New Group, plus Settle Up when open balances exist;
                        // duplicate "Log Expense" button removed since floating FAB is the primary entry point)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showNewGroupDialog = true },
                                shape = SplitMateTheme.RadiusButton,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SplitMateTheme.PrimaryDark,
                                    contentColor = SplitMateTheme.ScreenBg
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .sizeIn(minHeight = 46.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = SplitMateTheme.ScreenBg, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ New Group", fontFamily = SplitMateTheme.FontRounded, color = SplitMateTheme.ScreenBg, fontWeight = FontWeight.Bold)
                            }

                            if (totalBalance != "₹0.00") {
                                FilledTonalButton(
                                    onClick = onNavigateToSettle,
                                    shape = SplitMateTheme.RadiusButton,
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = SplitMateTheme.SurfaceWhite),
                                    modifier = Modifier
                                        .weight(1f)
                                        .sizeIn(minHeight = 46.dp)
                                ) {
                                    Icon(Icons.Rounded.TaskAlt, contentDescription = null, tint = SplitMateTheme.PrimaryDark, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Settle Up →", fontFamily = SplitMateTheme.FontRounded, color = SplitMateTheme.PrimaryDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2.5. STANDALONE HERO FEATURE ELEMENT: Interactive Stacked Transit Pass Deck (Train PNR + Flight PDF)
        item {
            val hasGroups = activeGroups.isNotEmpty()
            val totalLoggedTrainPnrs = remember(uiState.expenses) {
                uiState.expenses.count {
                    it.title.contains("PNR:", ignoreCase = true) &&
                        !it.title.contains("Flight", ignoreCase = true) &&
                        !it.title.contains("Airfare", ignoreCase = true)
                }
            }
            val totalLoggedFlightPnrs = remember(uiState.expenses) {
                uiState.expenses.count {
                    it.title.contains("PNR:", ignoreCase = true) &&
                        (it.title.contains("Flight", ignoreCase = true) || it.title.contains("Airfare", ignoreCase = true))
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

        // 3. Section Header: Active Groups
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
                                text = "${activeGroups.size}",
                                fontFamily = SplitMateTheme.FontRounded,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitMateTheme.PrimaryDark,
                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Sort by balance",
                        fontFamily = SplitMateTheme.FontRounded,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SplitMateTheme.TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.SwapVert,
                        contentDescription = null,
                        tint = SplitMateTheme.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 4. Illustrated M3 Empty State Card when activeGroups is empty
        item {
            AnimatedVisibility(
                visible = activeGroups.isEmpty(),
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
                            text = "No active trips. Tap + New Group to create your first ledger!",
                            fontFamily = SplitMateTheme.FontDisplay,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateTheme.PrimaryDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pick members directly from Android Contacts with automatic UPI routing.",
                            fontFamily = SplitMateTheme.FontRounded,
                            fontSize = 12.sp,
                            color = SplitMateTheme.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showNewGroupDialog = true },
                            shape = SplitMateTheme.RadiusBadge,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SplitMateTheme.PrimaryDark,
                                contentColor = SplitMateTheme.ScreenBg
                            )
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = SplitMateTheme.ScreenBg, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create First Group", fontFamily = SplitMateTheme.FontRounded, color = SplitMateTheme.ScreenBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Dynamic Group Cards (24dp radius, 8-Icon Category support, 14dp compact padding)
        items(activeGroups, key = { it.groupId }) { groupCard ->
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

            Card(
                onClick = {
                    viewModel.openGroupDetail(groupCard.groupId)
                },
                shape = SplitMateTheme.RadiusCard,
                colors = CardDefaults.cardColors(containerColor = cardSurfaceColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                    .then(
                        if (groupCard.netBalanceCents == 0L) {
                            Modifier.border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
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
                                    .clip(CircleShape)
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
                        OverlappingAvatarStack(groupCard.memberSeeds, remainingCount = groupCard.remainingCount)
                        // Bottom-right space dedicated to active group metadata ("7 members · Last active today")
                        // instead of repeating a redundant "All settled up" status pill
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
                (fadeIn(tween(240)) + scaleIn(
                    initialScale = 0.93f,
                    animationSpec = spring(dampingRatio = 0.76f, stiffness = 380f)
                )) togetherWith (fadeOut(tween(180)) + scaleOut(
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
                                text = "Create Ledger Group",
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
                            if (matchingDeviceContacts.isNotEmpty()) {
                                FilledTonalButton(
                                    onClick = {
                                        val topMatch = matchingDeviceContacts.first()
                                        selectedMembers = selectedMembers + NewGroupMemberDraft(
                                            name = topMatch.name,
                                            cleanPhone = topMatch.cleanPhone
                                        )
                                        contactSearchQuery = ""
                                    },
                                    shape = SplitMateTheme.RadiusButton,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = SplitMateTheme.AccentSage,
                                        contentColor = Color(0xFF23201E)
                                    ),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Icon(Icons.Rounded.PersonAdd, contentDescription = "Add Matched Contact", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Live matching contact suggestions from the user's phonebook
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
                                Text(
                                    text = "No matching contact in phonebook for \"$contactSearchQuery\"",
                                    fontSize = 11.sp,
                                    color = SplitMateTheme.TerracottaText,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
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
                                val finalDrafts = if (topMatch != null) {
                                    selectedMembers + NewGroupMemberDraft(
                                        name = topMatch.name,
                                        cleanPhone = topMatch.cleanPhone
                                    )
                                } else {
                                    selectedMembers
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
    }
}

// ==============================================================================
// TAB 3: SETTLE (Greedy Debt Simplification & Direct UPI + Direct WhatsApp Link)
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
            Toast.makeText(context, "Contacts permission required to link phone for UPI", Toast.LENGTH_SHORT).show()
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

    // Removed unlicensed P2P UPI payment sheet per NPCI/Google Pay security policy to avoid user confusion

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
                        newUpiId = "${picked.cleanPhone}@upi",
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
    val displayedMemberSummaries = remember(memberSummaries, sortByBalanceMagnitude) {
        if (sortByBalanceMagnitude) {
            memberSummaries.sortedByDescending { maxOf(it.totalOutgoingCents, it.totalIncomingCents) }
        } else {
            memberSummaries
        }
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
                text = "Simplified balances grouped by member with direct WhatsApp & UPI",
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

        // 1. TRIP SETTLEMENT SUMMARY CARD (Top Overview Grid of Who Receives & Who Needs to Pay)
        if (topGridSummaries.isNotEmpty()) {
            item(key = "trip_settlement_summary_card") {
                val summaryContainerBg = if (SplitMateTheme.isDark) {
                    SplitMateTheme.SurfaceWhite
                } else {
                    Color(0xFFF3F7EB)
                }
                val summaryBorderColor = if (SplitMateTheme.isDark) {
                    SplitMateTheme.BorderLight
                } else {
                    Color(0xFFDCE6C8)
                }
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = summaryContainerBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, summaryBorderColor, RoundedCornerShape(24.dp))
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(SplitMateTheme.SageSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Groups,
                                    contentDescription = null,
                                    tint = SplitMateTheme.SageText,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Trip Settlement Summary",
                                        fontFamily = SplitMateTheme.FontDisplay,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SplitMateTheme.PrimaryDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Tiny unobtrusive ⓘ icon next to Settle Up — hides the Max-Heap Graph Inspector until tapped
                                    Surface(
                                        onClick = { showMaxHeapGraphInspector = !showMaxHeapGraphInspector },
                                        shape = RoundedCornerShape(inspectorCornerRadius),
                                        color = if (showMaxHeapGraphInspector) Color(0xFF365314) else SplitMateTheme.SageSurface,
                                        border = BorderStroke(1.dp, Color(0xFF416913).copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Info,
                                                contentDescription = "Why were debts simplified?",
                                                tint = if (showMaxHeapGraphInspector) Color(0xFFD7E8B6) else SplitMateTheme.SageText,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Quick view of who receives and who needs to pay",
                                    fontSize = 11.sp,
                                    color = SplitMateTheme.TextSecondary
                                )
                            }
                        }

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
                                            text = "Raw Pairwise Web ($rawPairwiseIouCount edges)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SplitMateTheme.TerracottaText
                                        )
                                        Text(
                                            text = "Max-Heap Minimum Cash Flow ($simplifiedTransferCount)",
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
                                                color = Color(0xFFE06B52).copy(alpha = 0.55f),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                    width = 1.6.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                                )
                                            )
                                            drawCircle(
                                                color = Color(0xFFE06B52),
                                                radius = 4.dp.toPx(),
                                                center = startPt
                                            )
                                        }

                                        // Center: Max-Heap Simplifier Hub
                                        drawRoundRect(
                                            color = Color(0xFFD7E8B6),
                                            topLeft = Offset(midX - 18.dp.toPx(), midY - 14.dp.toPx()),
                                            size = androidx.compose.ui.geometry.Size(36.dp.toPx(), 28.dp.toPx()),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                        )
                                        drawCircle(
                                            color = Color(0xFF365314),
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
                                                color = Color(0xFF416913),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
                                            )
                                            drawCircle(
                                                color = Color(0xFF416913),
                                                radius = 5.dp.toPx(),
                                                center = endPt
                                            )
                                            // Animated kinetic settlement particle along simplified edge
                                            val dotX = startHub.x + (endPt.x - startHub.x) * flowProgress
                                            val dotY = startHub.y + (endPt.y - startHub.y) * flowProgress
                                            drawCircle(
                                                color = Color(0xFFD7E8B6),
                                                radius = 4.dp.toPx(),
                                                center = Offset(dotX, dotY)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            topGridSummaries.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { summary ->
                                        val shortName = summary.memberName.substringBefore(" ").ifBlank { summary.memberName }
                                        val isReceiverTile = summary.hasIncoming && !summary.hasOutgoing
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = SplitMateTheme.SurfaceWhite,
                                            border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AvatarToken(
                                                    initials = summary.avatarSeed,
                                                    bg = if (isReceiverTile) SplitMateTheme.SageSurface else SplitMateTheme.TerracottaSurface,
                                                    textColor = if (isReceiverTile) SplitMateTheme.SageText else SplitMateTheme.TerracottaText,
                                                    size = 36
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = shortName,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 13.sp,
                                                        color = SplitMateTheme.PrimaryDark,
                                                        maxLines = 1
                                                    )
                                                    if (summary.hasBothDirections) {
                                                        Text(
                                                            text = "Receives & Pays",
                                                            fontSize = 10.sp,
                                                            color = SplitMateTheme.TextSecondary
                                                        )
                                                        Text(
                                                            text = "+${summary.formattedTotalIncoming}",
                                                            fontFamily = SplitMateTheme.FontDisplay,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 12.sp,
                                                            color = SplitMateTheme.SageText
                                                        )
                                                        Text(
                                                            text = "-${summary.formattedTotalOutgoing}",
                                                            fontFamily = SplitMateTheme.FontDisplay,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 12.sp,
                                                            color = SplitMateTheme.TerracottaText
                                                        )
                                                    } else if (summary.hasIncoming) {
                                                        Text(
                                                            text = "Receives",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = SplitMateTheme.SageText
                                                        )
                                                        Text(
                                                            text = "+${summary.formattedTotalIncoming}",
                                                            fontFamily = SplitMateTheme.FontDisplay,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 14.sp,
                                                            color = SplitMateTheme.SageText
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "Total to pay",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = SplitMateTheme.TextSecondary
                                                        )
                                                        Text(
                                                            text = "-${summary.formattedTotalOutgoing}",
                                                            fontFamily = SplitMateTheme.FontDisplay,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 14.sp,
                                                            color = SplitMateTheme.TerracottaText
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. SECTION HEADER: INDIVIDUAL MEMBER DETAILS + SORT TOGGLE
            item(key = "individual_member_details_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Individual Member Details",
                        fontFamily = SplitMateTheme.FontDisplay,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SplitMateTheme.PrimaryDark
                    )
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

            // 3. INDIVIDUAL MEMBER CARDS (RECEIVES Cards & PAYS Cards with Multi-Payment Breakdown)
            items(displayedMemberSummaries, key = { "member_detail_${it.memberId}" }) { summary ->
                // A. If the member has INCOMING payments (RECEIVES Card)
                if (summary.hasIncoming) {
                    val receiverCardBg = if (SplitMateTheme.isDark) {
                        SplitMateTheme.SurfaceWhite
                    } else {
                        Color(0xFFF1F7E8)
                    }
                    val receiverBorderColor = if (SplitMateTheme.isDark) {
                        SplitMateTheme.BorderLight
                    } else {
                        Color(0xFFD8E5C2)
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
                                        size = 46
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = summary.memberName,
                                                fontFamily = SplitMateTheme.FontDisplay,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 17.sp,
                                                color = SplitMateTheme.PrimaryDark
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = SplitMateTheme.RadiusBadge,
                                                color = SplitMateTheme.SageSurface
                                            ) {
                                                Text(
                                                    text = "RECEIVES",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = SplitMateTheme.SageText,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Owes you from others",
                                            fontSize = 12.sp,
                                            color = SplitMateTheme.TextSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = "+${summary.formattedTotalIncoming}",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp,
                                    color = SplitMateTheme.SageText
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                summary.incomingPayments.forEach { leg ->
                                    val matchingTransfer = settlementPlan.find {
                                        it.fromMemberId == leg.counterpartyMemberId && it.toMemberId == summary.memberId
                                    }
                                    val fromRoomMember = uiState.members.find { it.memberId == leg.counterpartyMemberId }
                                    val debtorPhone = cleanIndianTenDigitPhone(fromRoomMember?.upiId?.substringBefore('@') ?: "")

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
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

                                            if (matchingTransfer != null) {
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
                                                    val markPaidCornerIn by animateDpAsState(
                                                        targetValue = if (isDrainingIn) 12.dp else 20.dp,
                                                        animationSpec = spring(dampingRatio = 0.62f, stiffness = 500f),
                                                        label = "MarkPaidCornerIn"
                                                    )
                                                    Button(
                                                        onClick = {
                                                            if (isDrainingIn) return@Button
                                                            isDrainingIn = true
                                                            drainHapticIn.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            drainScopeIn.launch {
                                                                kotlinx.coroutines.delay(140L)
                                                                drainHapticIn.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                kotlinx.coroutines.delay(180L)
                                                                viewModel.markGreedyTransferSettled(matchingTransfer.transfer)
                                                                isDrainingIn = false
                                                            }
                                                        },
                                                        shape = RoundedCornerShape(markPaidCornerIn),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isDrainingIn) Color(0xFF365314) else SplitMateTheme.PrimaryDark,
                                                            contentColor = SplitMateTheme.ScreenBg
                                                        ),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(40.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = if (isDrainingIn) "₹0.00 · Settled" else "Mark Paid",
                                                            fontFamily = SplitMateTheme.FontRounded,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            maxLines = 1
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

                if (summary.hasBothDirections) {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // B. If the member has OUTGOING payments (PAYS Card with Multi-Payment Breakdown)
                if (summary.hasOutgoing) {
                    var isPayerExpanded by remember(summary.memberId) { mutableStateOf(true) }
                    val peachBoxBg = if (SplitMateTheme.isDark) {
                        SplitMateTheme.SurfaceMuted
                    } else {
                        Color(0xFFFDF2EE)
                    }
                    val peachBoxBorder = if (SplitMateTheme.isDark) {
                        SplitMateTheme.BorderLight
                    } else {
                        Color(0xFFF7E0D7)
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
                                        size = 46
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = summary.memberName,
                                                fontFamily = SplitMateTheme.FontDisplay,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 17.sp,
                                                color = SplitMateTheme.PrimaryDark
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = SplitMateTheme.RadiusBadge,
                                                color = SplitMateTheme.TerracottaSurface
                                            ) {
                                                Text(
                                                    text = "PAYS",
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

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = leg.formattedAmount,
                                                            fontFamily = SplitMateTheme.FontDisplay,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 14.sp,
                                                            color = SplitMateTheme.PrimaryDark
                                                        )
                                                    }
                                                }

                                                if (matchingTransfer != null) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        var isDrainingOut by remember { mutableStateOf(false) }
                                                        val drainScopeOut = rememberCoroutineScope()
                                                        val drainHapticOut = LocalHapticFeedback.current
                                                        val markPaidCornerOut by animateDpAsState(
                                                            targetValue = if (isDrainingOut) 12.dp else 20.dp,
                                                            animationSpec = spring(dampingRatio = 0.62f, stiffness = 500f),
                                                            label = "MarkPaidCornerOut"
                                                        )
                                                        Button(
                                                            onClick = {
                                                                if (isDrainingOut) return@Button
                                                                isDrainingOut = true
                                                                drainHapticOut.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                drainScopeOut.launch {
                                                                    kotlinx.coroutines.delay(140L)
                                                                    drainHapticOut.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    kotlinx.coroutines.delay(180L)
                                                                    viewModel.markGreedyTransferSettled(matchingTransfer.transfer)
                                                                    isDrainingOut = false
                                                                }
                                                            },
                                                            shape = RoundedCornerShape(markPaidCornerOut),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = if (isDrainingOut) Color(0xFF365314) else SplitMateTheme.PrimaryDark,
                                                                contentColor = SplitMateTheme.ScreenBg
                                                            ),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(40.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Check,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = if (isDrainingOut) "₹0.00 · Settled" else "Mark Paid",
                                                                fontFamily = SplitMateTheme.FontRounded,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp
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
        }

        // 5. Sleek 56dp Settled Equilibrium Banner (No longer hogs the viewport above Settled & Paid History)
        if (settlementPlan.isEmpty()) {
            item {
                Surface(
                    shape = SplitMateTheme.RadiusCard,
                    color = SplitMateTheme.SageSurface,
                    border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SplitMateTheme.SurfaceWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = SplitMateTheme.SageText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "All Accounts Balanced",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
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
                            color = SplitMateTheme.SurfaceWhite.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = "₹0.00",
                                fontFamily = SplitMateTheme.FontDisplay,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = SplitMateTheme.SageText,
                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
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
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            Surface(
                                onClick = { selectedSettlementForSheet = settlement },
                                shape = RoundedCornerShape(12.dp),
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditVaultScreen(
    viewModel: SplitMateViewModel,
    onOpenPnrWithTicket: (groupId: String, pnr: String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sym = "₹"
    var selectedGroupFilterId by remember { mutableStateOf<String?>(null) }
    var selectedExpenseForDetailSheet by remember { mutableStateOf<com.splitmate.app.data.ExpenseEntity?>(null) }
    var editingExpenseEntity by remember { mutableStateOf<com.splitmate.app.data.ExpenseEntity?>(null) }

    val filteredExpenses = remember(uiState.expenses, selectedGroupFilterId) {
        if (selectedGroupFilterId == null) {
            uiState.expenses
        } else {
            uiState.expenses.filter { it.groupId == selectedGroupFilterId }
        }
    }

    // Group expenses chronologically by day with friendly labels ("Today · 25 Sep", "Yesterday · 24 Sep", etc.)
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
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header + Direct Horizontal Filter Chip Strip (Removed redundant outer white card wrapper)
        item {
            Column {
                Text(
                    text = "Activity & History",
                    fontFamily = SplitMateTheme.FontDisplay,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SplitMateTheme.PrimaryDark
                )
                Text(
                    text = "Tap any expense for split breakdown, boarding pass, or edit",
                    fontFamily = SplitMateTheme.FontRounded,
                    fontSize = 12.sp,
                    color = SplitMateTheme.TextSecondary
                )

                if (uiState.groups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            val allSelected = selectedGroupFilterId == null
                            val allCornerRadius by animateDpAsState(
                                targetValue = if (allSelected) 10.dp else 20.dp,
                                animationSpec = spring(dampingRatio = 0.65f, stiffness = 480f),
                                label = "allGroupsChipCornerMorph"
                            )
                            Surface(
                                onClick = { selectedGroupFilterId = null },
                                shape = RoundedCornerShape(allCornerRadius),
                                color = if (allSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.SurfaceWhite,
                                border = BorderStroke(1.dp, if (allSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.BorderLight),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .heightIn(min = 32.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "All Groups (${uiState.expenses.size})",
                                        fontFamily = SplitMateTheme.FontRounded,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (allSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark,
                                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                                    )
                                }
                            }
                        }
                        items(uiState.groups, key = { it.groupId }) { grp ->
                            val isSelected = selectedGroupFilterId == grp.groupId
                            val count = uiState.expenses.count { it.groupId == grp.groupId }
                            val chipCornerRadius by animateDpAsState(
                                targetValue = if (isSelected) 10.dp else 20.dp,
                                animationSpec = spring(dampingRatio = 0.65f, stiffness = 480f),
                                label = "groupFilterChipCornerMorph"
                            )
                            Surface(
                                onClick = { selectedGroupFilterId = if (isSelected) null else grp.groupId },
                                shape = RoundedCornerShape(chipCornerRadius),
                                color = if (isSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.SurfaceWhite,
                                border = BorderStroke(1.dp, if (isSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.BorderLight),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .heightIn(min = 32.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${grp.name.toSmartTitleCase()} ($count)",
                                        fontFamily = SplitMateTheme.FontRounded,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark,
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
                        Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null, tint = SplitMateTheme.SageText, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No expenses recorded yet", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SplitMateTheme.PrimaryDark)
                        Text("Expenses you split will appear here chronologically.", fontSize = 12.sp, color = SplitMateTheme.TextSecondary)
                    }
                }
            }
        }

        // 2. Chronological Date Dividers + Flattened Single-Level Expense Cards
        groupedExpensesByDate.forEach { (dateHeaderLabel, dateExpenses) ->
            item(key = "date_header_$dateHeaderLabel") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dateHeaderLabel,
                        fontFamily = SplitMateTheme.FontDisplay,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SplitMateTheme.TextSecondary,
                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                    )
                    Text(
                        text = if (dateExpenses.size == 1) "1 entry" else "${dateExpenses.size} entries",
                        fontFamily = SplitMateTheme.FontRounded,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SplitMateTheme.TextSecondary,
                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }

            items(dateExpenses, key = { it.expenseId }) { expense ->
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
                val displayExpenseTitle = remember(expense.title) {
                    cleanDisplayExpenseTitle(expense.title)
                }
                val payerFirstName = if (isMePayer) "You" else (payer?.name?.substringBefore(" ") ?: "Member")

                // Flattened Single-Level Card (No box-in-box ticket stub or persistent Edit/Undo buttons)
                Card(
                    onClick = { selectedExpenseForDetailSheet = expense },
                    shape = SplitMateTheme.RadiusCard,
                    colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                                        text = "Paid by $payerFirstName · ${breakdown.splittingMembersCount} splitting ($perPersonShare/ea)",
                                        fontFamily = SplitMateTheme.FontRounded,
                                        fontSize = 12.sp,
                                        color = SplitMateTheme.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                                    )
                                    if (parsedTravelTicket != null && parsedTravelTicket.pnr.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = SplitMateTheme.RadiusBadge,
                                                color = SplitMateTheme.SurfaceMuted
                                            ) {
                                                Text(
                                                    text = "PNR ${parsedTravelTicket.pnr} · ${parsedTravelTicket.bookingStatus}",
                                                    fontFamily = SplitMateTheme.FontRounded,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SplitMateTheme.PrimaryDark,
                                                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            if (!group?.name.isNullOrBlank()) {
                                                Text(
                                                    text = "in ${group?.name?.toSmartTitleCase()}",
                                                    fontFamily = SplitMateTheme.FontRounded,
                                                    fontSize = 11.sp,
                                                    color = SplitMateTheme.TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formattedSignedTotal,
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    // Restrict Forest Green strictly to positive financial balances (+₹7,525.40)
                                    color = if (isMePayer) SplitMateTheme.SageText else SplitMateTheme.PrimaryDark,
                                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (parsedTravelTicket != null) "Boarding Pass ↗" else "Details ›",
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SplitMateTheme.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Contextual Expense Breakdown, Boarding Pass & Edit/Undo Modal Bottom Sheet
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
        val parsedTravelTicket = remember(selectedExp.title) {
            extractTravelTicketFromTitle(selectedExp.title)
        }
        val cleanTitle = remember(selectedExp.title) {
            cleanDisplayExpenseTitle(selectedExp.title)
        }
        val formattedTotal = formatIndianRupeesFromCents(
            cents = selectedExp.totalAmountCents,
            includePlusSign = false,
            currencySymbol = sym
        )

        ModalBottomSheet(
            onDismissRequest = { selectedExpenseForDetailSheet = null },
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
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = cleanTitle,
                            fontFamily = SplitMateTheme.FontDisplay,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = SplitMateTheme.PrimaryDark
                        )
                        Text(
                            text = "Paid by ${payer?.name ?: "You"} · ${breakdown.perPersonHeadlineShare}/person",
                            fontFamily = SplitMateTheme.FontRounded,
                            fontSize = 12.sp,
                            color = SplitMateTheme.TextSecondary,
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                    Text(
                        text = formattedTotal,
                        fontFamily = SplitMateTheme.FontDisplay,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = SplitMateTheme.PrimaryDark,
                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                    )
                }

                if (parsedTravelTicket != null && parsedTravelTicket.pnr.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val gId = selectedExp.groupId
                            val pnrCode = parsedTravelTicket.pnr
                            selectedExpenseForDetailSheet = null
                            onOpenPnrWithTicket(gId, pnrCode)
                        },
                        shape = SplitMateTheme.RadiusButton,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SplitMateTheme.PrimaryDark,
                            contentColor = SplitMateTheme.ScreenBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = if (isFlightTicketExpense(selectedExp.title, parsedTravelTicket)) Icons.Rounded.FlightTakeoff else Icons.Rounded.Train,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Full Boarding Pass (PNR ${parsedTravelTicket.pnr}) ↗",
                            fontFamily = SplitMateTheme.FontRounded,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = SplitMateTheme.BorderLight)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = breakdown.headerLabel,
                    fontFamily = SplitMateTheme.FontDisplay,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SplitMateTheme.PrimaryDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                breakdown.rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = row.displayName,
                            fontFamily = SplitMateTheme.FontRounded,
                            fontSize = 13.sp,
                            color = if (row.isIncludedInSplit) SplitMateTheme.PrimaryDark else SplitMateTheme.TextSecondary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = row.formattedShare,
                            fontFamily = SplitMateTheme.FontDisplay,
                            fontSize = 13.sp,
                            fontWeight = if (row.isIncludedInSplit) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (row.isIncludedInSplit) SplitMateTheme.PrimaryDark else SplitMateTheme.TextSecondary.copy(alpha = 0.55f),
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val expToEdit = selectedExp
                            selectedExpenseForDetailSheet = null
                            editingExpenseEntity = expToEdit
                        },
                        shape = SplitMateTheme.RadiusButton,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SplitMateTheme.PrimaryDark),
                        border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Expense", fontFamily = SplitMateTheme.FontRounded, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val idToRollback = selectedExp.expenseId
                            selectedExpenseForDetailSheet = null
                            viewModel.rollbackExpense(idToRollback)
                        },
                        shape = SplitMateTheme.RadiusButton,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SplitMateTheme.TerracottaText),
                        border = BorderStroke(1.dp, SplitMateTheme.TerracottaSurface),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Undo Entry", fontFamily = SplitMateTheme.FontRounded, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
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

