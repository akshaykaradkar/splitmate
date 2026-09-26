package com.splitmate.app.ui.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.splitmate.app.AuditVaultScreen
import com.splitmate.app.GreedySettlementScreen
import com.splitmate.app.LedgersDashboardScreen
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.data.PnrNetworkRepository
import com.splitmate.app.data.UniversalFlightTicketExtractor
import com.splitmate.app.ui.ContactPickerBottomSheet
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.components.ActiveTravelPassMode
import com.splitmate.app.ui.screens.FlightExpenseReviewScreen
import com.splitmate.app.ui.screens.PnrExpenseReviewScreen
import com.splitmate.app.ui.screens.QuickExpenseScreen
import com.splitmate.app.ui.screens.TripHomeScreen
import com.splitmate.app.ui.screens.TripHubSectionTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ==============================================================================
// 1. SPLITMATE UNIFIED ROUTING GRAPH (v2.0 Production Architecture)
// ==============================================================================
/**
 * [SplitMateRoute] represents the production state machine for SplitMate v2.0.
 *
 * Bridges:
 * 1. Global Navigation Tabs (Ledgers, Quick Split, Settle, Audit)
 * 2. Deep Travel Review Flows (Train PNR Review, Flight PDF Review)
 * 3. The "Trip as Single Source of Truth" Shared Hub (Overview, Plan, Travel, Money, People)
 */
sealed class SplitMateRoute {
    // --------------------------------------------------------------------------
    // Level 1: Global App Tabs (Docked Bottom Bar Visible at Root)
    // --------------------------------------------------------------------------
    object DashboardLedgers : SplitMateRoute()
    object QuickExpense : SplitMateRoute()
    object GreedySettlement : SplitMateRoute()
    object AuditVault : SplitMateRoute()

    // --------------------------------------------------------------------------
    // Level 2: Deep Travel Feature Ingestion Flows (Zero Hardcoded Demo PNRs)
    // --------------------------------------------------------------------------
    data class TrainPnrReview(val initialPnr: String = "") : SplitMateRoute()
    data class FlightPdfReview(
        val pnrCode: String = "",
        val extractedTicket: UniversalFlightTicketExtractor.UniversalFlightTicketResult? = null
    ) : SplitMateRoute()

    // --------------------------------------------------------------------------
    // Level 3: The "Single Source of Truth" Shared Trip Hub
    // (Fullscreen mode - Global Bottom Bar smoothly hides for maximum immersion)
    // --------------------------------------------------------------------------
    data class TripHub(
        val tripId: String,
        val tripName: String = "",
        val initialTab: TripHubTab = TripHubTab.OVERVIEW
    ) : SplitMateRoute()
}

enum class TripHubTab(val title: String) {
    OVERVIEW("Overview"),
    PLAN("Plan"),
    TRAVEL("Travel"),
    MONEY("Money"),
    PEOPLE("People");

    fun toSectionTab(): TripHubSectionTab = when (this) {
        OVERVIEW -> TripHubSectionTab.OVERVIEW
        PLAN -> TripHubSectionTab.PLAN
        TRAVEL -> TripHubSectionTab.TRAVEL
        MONEY -> TripHubSectionTab.MONEY
        PEOPLE -> TripHubSectionTab.PEOPLE
    }
}

enum class GlobalNavTab(val label: String, val icon: ImageVector) {
    LEDGERS("Ledgers", Icons.Rounded.AccountBalanceWallet),
    SPLIT("Split", Icons.AutoMirrored.Rounded.ReceiptLong),
    SETTLE("Settle", Icons.Rounded.SwapHoriz),
    AUDIT("Audit", Icons.Rounded.HistoryEdu)
}

// ==============================================================================
// 2. ROOT APPLICATION ENTRY COMPOSABLE: SplitMateAppNavHost
// ==============================================================================
/**
 * Production-hardened [SplitMateAppNavHost] connecting [SplitMateViewModel],
 * [flightPdfPickerLauncher], [BackHandler], [ActiveTravelPassMode], and
 * [TripHomeScreen] with 1-tap `Classic Ledger` toggle support.
 *
 * ### Relationship with `SplitMateMainDashboardScaffold` (which one is primary)
 *
 * - **Primary (mounted) root:** `MainActivity.setContent { ... }` renders `SplitMateApp(viewModel)`
 *   (`ui/SplitMateAppComposable.kt`), whose Jetpack Navigation `NavHost` routes
 *   `onboarding` -> `dashboard` -> `settings`. The `dashboard` destination hosts
 *   `SplitMateMainDashboardScaffold`, which is the canonical, shipping owner of the global
 *   Ledgers / Split / Settle / Audit bottom bar, the two-pane tablet (`>= 720.dp`) layout,
 *   the Classic v1.x Group Detail, the Train PNR / Flight PDF review overlays, and the
 *   v2.0 `TripHomeScreen` canvas. All user-facing navigation in production flows through it.
 * - **This composable ([SplitMateAppNavHost]):** a self-contained, `NavController`-free
 *   route state machine over [SplitMateRoute] (manual back stack + [BackHandler]) that renders
 *   the same feature screens (`LedgersDashboardScreen`, `QuickExpenseScreen`,
 *   `GreedySettlementScreen`, `AuditVaultScreen`, `PnrExpenseReviewScreen`,
 *   `FlightExpenseReviewScreen`, `TripHomeScreen`). It is **not** currently mounted by
 *   `MainActivity` and does not handle onboarding or settings (those are delegated via
 *   [onOpenSettings]).
 *
 * ### When to use which
 *
 * - Use `SplitMateMainDashboardScaffold` (through `SplitMateApp`) for all production entry points,
 *   including deep links / Share Sheet sync intents handled in `MainActivity`.
 * - Use [SplitMateAppNavHost] only as an optional alternative root for isolated previews,
 *   instrumentation / screenshot tests, or embedding hosts that need typed [SplitMateRoute]
 *   deep-entry (e.g. [initialRoute] = [SplitMateRoute.TripHub]) without the onboarding gate.
 *
 * Both roots share the single [SplitMateViewModel] / Room source of truth, so feature behavior
 * (integer-cent math, PNR de-duplication, Greedy settlement) is identical regardless of host.
 * When adding a new top-level destination, wire it into `SplitMateMainDashboardScaffold` first,
 * then mirror it here as a [SplitMateRoute] to keep the two hosts in parity.
 */
@Composable
fun SplitMateAppNavHost(
    viewModel: SplitMateViewModel,
    modifier: Modifier = Modifier,
    initialRoute: SplitMateRoute = SplitMateRoute.DashboardLedgers,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigation Back-Stack State
    var currentRoute by remember { mutableStateOf(initialRoute) }
    var backStack by remember { mutableStateOf(listOf<SplitMateRoute>()) }
    var useTripHubV2View by rememberSaveable { mutableStateOf(true) }

    // Active Travel Pass Mode on Dashboard (Train vs Flight deck toggle)
    var dashboardTravelPassMode by remember { mutableStateOf(ActiveTravelPassMode.TRAIN) }

    // Helper: Push new screen onto history
    fun navigateTo(route: SplitMateRoute) {
        if (currentRoute != route) {
            backStack = backStack + currentRoute
            currentRoute = route
        }
    }

    // Helper: Pop back to previous screen
    fun navigateBack(): Boolean {
        if (backStack.isNotEmpty()) {
            currentRoute = backStack.last()
            backStack = backStack.dropLast(1)
            return true
        }
        if (currentRoute !is SplitMateRoute.DashboardLedgers) {
            currentRoute = SplitMateRoute.DashboardLedgers
            return true
        }
        return false
    }

    // Helper: Switch root tab (clears deep child screens)
    fun switchGlobalTab(tab: GlobalNavTab) {
        backStack = emptyList()
        currentRoute = when (tab) {
            GlobalNavTab.LEDGERS -> SplitMateRoute.DashboardLedgers
            GlobalNavTab.SPLIT -> SplitMateRoute.QuickExpense
            GlobalNavTab.SETTLE -> SplitMateRoute.GreedySettlement
            GlobalNavTab.AUDIT -> SplitMateRoute.AuditVault
        }
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
            coroutineScope.launch(Dispatchers.IO) {
                val extracted = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        UniversalFlightTicketExtractor.extractFromPdfStream(
                            inputStream = stream,
                            groupMemberNames = memberNames,
                            context = context
                        )
                    }
                }.getOrNull()

                withContext(Dispatchers.Main) {
                    if (extracted != null && (extracted.isValidFlightTicket || extracted.totalFarePaise > 0L)) {
                        val existingMatch = viewModel.findExistingExpenseByPnr(
                            pnr = extracted.pnr,
                            preferredGroupId = uiState.openedGroupDetailId ?: uiState.activeGroupId
                        )
                        val ticketToOpen = if (existingMatch != null) {
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
                            PnrNetworkRepository.loadConfirmedFlightTicketResult(context, extracted.pnr) ?: extracted
                        } else {
                            extracted
                        }
                        dashboardTravelPassMode = ActiveTravelPassMode.FLIGHT
                        navigateTo(
                            SplitMateRoute.FlightPdfReview(
                                pnrCode = ticketToOpen.pnr,
                                extractedTicket = ticketToOpen
                            )
                        )
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
        val normalized = PnrNetworkRepository.normalizePnrKey(pnrRaw)
        if (normalized.length == 6) {
            val resolvedFlight = resolveFlightTicketByPnr(context, normalized)
            if (resolvedFlight != null) {
                dashboardTravelPassMode = ActiveTravelPassMode.FLIGHT
                navigateTo(
                    SplitMateRoute.FlightPdfReview(
                        pnrCode = normalized,
                        extractedTicket = resolvedFlight
                    )
                )
                return
            }
        }
        dashboardTravelPassMode = ActiveTravelPassMode.TRAIN
        navigateTo(SplitMateRoute.TrainPnrReview(initialPnr = pnrRaw))
    }

    // Intercept system Back across deep routes and non-root global tabs
    BackHandler(
        enabled = backStack.isNotEmpty() || currentRoute !is SplitMateRoute.DashboardLedgers
    ) {
        navigateBack()
    }

    // Auto-hide the Bottom Bar whenever inside TripHub, TrainPnrReview, FlightPdfReview,
    // OR whenever uiState.openedGroupDetailId != null
    val isGlobalTabVisible = (currentRoute is SplitMateRoute.DashboardLedgers ||
        currentRoute is SplitMateRoute.QuickExpense ||
        currentRoute is SplitMateRoute.GreedySettlement ||
        currentRoute is SplitMateRoute.AuditVault) &&
        uiState.openedGroupDetailId == null

    val selectedGlobalTab = when (currentRoute) {
        is SplitMateRoute.DashboardLedgers -> GlobalNavTab.LEDGERS
        is SplitMateRoute.QuickExpense -> GlobalNavTab.SPLIT
        is SplitMateRoute.GreedySettlement -> GlobalNavTab.SETTLE
        is SplitMateRoute.AuditVault -> GlobalNavTab.AUDIT
        else -> null
    }

    val navBarSpring = spring<IntOffset>(dampingRatio = 0.76f, stiffness = 380f)
    val navBarFadeSpring = spring<Float>(dampingRatio = 0.76f, stiffness = 380f)

    Scaffold(
        containerColor = SplitMateTheme.ScreenBg,
        bottomBar = {
            AnimatedVisibility(
                visible = isGlobalTabVisible && selectedGlobalTab != null,
                enter = slideInVertically(
                    animationSpec = navBarSpring,
                    initialOffsetY = { it }
                ) + fadeIn(animationSpec = navBarFadeSpring),
                exit = slideOutVertically(
                    animationSpec = navBarSpring,
                    targetOffsetY = { it }
                ) + fadeOut(animationSpec = navBarFadeSpring)
            ) {
                if (selectedGlobalTab != null) {
                    SplitMateGlobalBottomBar(
                        selectedTab = selectedGlobalTab,
                        onTabSelected = { tab -> switchGlobalTab(tab) }
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isGlobalTabVisible) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    if (targetState is SplitMateRoute.TripHub ||
                        targetState is SplitMateRoute.TrainPnrReview ||
                        targetState is SplitMateRoute.FlightPdfReview
                    ) {
                        (slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn()) togetherWith
                            (slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut())
                    } else {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    }
                },
                label = "ScreenNavigationRouter"
            ) { targetScreen ->
                when (targetScreen) {
                    // ==========================================================
                    // 1. DASHBOARD LEDGERS
                    // ==========================================================
                    is SplitMateRoute.DashboardLedgers -> {
                        LedgersDashboardScreen(
                            viewModel = viewModel,
                            onNavigateToSplit = {
                                navigateTo(SplitMateRoute.QuickExpense)
                            },
                            onNavigateToSettle = {
                                navigateTo(SplitMateRoute.GreedySettlement)
                            },
                            onNavigateToPnrSplit = {
                                if (uiState.groups.isNotEmpty()) {
                                    dashboardTravelPassMode = ActiveTravelPassMode.TRAIN
                                    navigateTo(SplitMateRoute.TrainPnrReview(initialPnr = ""))
                                }
                            },
                            onUploadFlightPdf = {
                                if (uiState.groups.isNotEmpty()) {
                                    dashboardTravelPassMode = ActiveTravelPassMode.FLIGHT
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
                            onToggleTripHubView = { enabled -> useTripHubV2View = enabled }
                        )
                    }

                    // ==========================================================
                    // 2. THE FLAGSHIP TRIP HUB (Single Source of Truth)
                    // ==========================================================
                    is SplitMateRoute.TripHub -> {
                        ConnectedTripHubContainer(
                            viewModel = viewModel,
                            tripId = targetScreen.tripId,
                            tripName = targetScreen.tripName,
                            initialTab = targetScreen.initialTab,
                            onBackToDashboardClick = { navigateBack() },
                            onSwitchToClassicLedgerClick = {
                                useTripHubV2View = false
                                if (targetScreen.tripId.isNotBlank()) {
                                    viewModel.openGroupDetail(targetScreen.tripId)
                                }
                                backStack = backStack.dropLastWhile { it is SplitMateRoute.DashboardLedgers }
                                currentRoute = SplitMateRoute.DashboardLedgers
                            },
                            onSettleUpClick = { navigateTo(SplitMateRoute.GreedySettlement) },
                            onViewTicketClick = { pnr -> openPnrOrFlightTicket(pnr) },
                            onAddTravelClick = {
                                dashboardTravelPassMode = ActiveTravelPassMode.TRAIN
                                navigateTo(SplitMateRoute.TrainPnrReview(initialPnr = ""))
                            },
                            onUploadFlightPdfClick = {
                                dashboardTravelPassMode = ActiveTravelPassMode.FLIGHT
                                flightPdfPickerLauncher.launch("application/pdf")
                            },
                            onLogQuickExpenseClick = {
                                navigateTo(SplitMateRoute.QuickExpense)
                            },
                            onViewBalancesClick = { navigateTo(SplitMateRoute.GreedySettlement) }
                        )
                    }

                    // ==========================================================
                    // 3. TRAIN PNR REVIEW (IRCTC Live Lookup & Paper Pass)
                    // ==========================================================
                    is SplitMateRoute.TrainPnrReview -> {
                        PnrExpenseReviewScreen(
                            viewModel = viewModel,
                            initialPnr = targetScreen.initialPnr,
                            onBackClick = { navigateBack() },
                            onExpenseAdded = { navigateBack() }
                        )
                    }

                    // ==========================================================
                    // 4. FLIGHT E-TICKET REVIEW (3D Foldable Boarding Pass)
                    // ==========================================================
                    is SplitMateRoute.FlightPdfReview -> {
                        val resolvedTicket = remember(targetScreen.pnrCode, targetScreen.extractedTicket) {
                            targetScreen.extractedTicket
                                ?: resolveFlightTicketByPnr(context, targetScreen.pnrCode)
                        }
                        if (resolvedTicket != null) {
                            FlightExpenseReviewScreen(
                                viewModel = viewModel,
                                extractedTicket = resolvedTicket,
                                onPickAnotherPdfClick = {
                                    flightPdfPickerLauncher.launch("application/pdf")
                                },
                                onBackClick = { navigateBack() },
                                onConfirmAndAddToLedger = { _ ->
                                    navigateBack()
                                }
                            )
                        } else {
                            LaunchedEffect(targetScreen) {
                                flightPdfPickerLauncher.launch("application/pdf")
                                navigateBack()
                            }
                        }
                    }

                    // ==========================================================
                    // 5. QUICK EXPENSE (0.00c Drift Calculator + Coin Flight)
                    // ==========================================================
                    is SplitMateRoute.QuickExpense -> {
                        QuickExpenseScreen(
                            viewModel = viewModel,
                            onBackClick = {
                                if (!navigateBack()) switchGlobalTab(GlobalNavTab.LEDGERS)
                            },
                            onOpenPnrDirectSplit = {
                                if (uiState.groups.isNotEmpty()) {
                                    dashboardTravelPassMode = ActiveTravelPassMode.TRAIN
                                    navigateTo(SplitMateRoute.TrainPnrReview(initialPnr = ""))
                                } else {
                                    switchGlobalTab(GlobalNavTab.LEDGERS)
                                }
                            },
                            onSaveSplit = { _, _ ->
                                if (!navigateBack()) switchGlobalTab(GlobalNavTab.LEDGERS)
                            }
                        )
                    }

                    // ==========================================================
                    // 6. GREEDY DEBT SIMPLIFICATION & SETTLEMENT
                    // ==========================================================
                    is SplitMateRoute.GreedySettlement -> {
                        GreedySettlementScreen(viewModel = viewModel)
                    }

                    // ==========================================================
                    // 7. AUDIT VAULT & HISTORY
                    // ==========================================================
                    is SplitMateRoute.AuditVault -> {
                        AuditVaultScreen(
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
}

// ==============================================================================
// 3. CONNECTED TRIP HUB CONTAINER (Overview | Plan | Travel | Money | People)
// ==============================================================================
/**
 * [ConnectedTripHubContainer] wires directly to the Room-backed [TripHomeScreen]
 * with 1-tap `Classic Ledger` toggle support and real device contact linking.
 */
@Composable
fun ConnectedTripHubContainer(
    viewModel: SplitMateViewModel,
    tripId: String,
    tripName: String = "",
    initialTab: TripHubTab = TripHubTab.OVERVIEW,
    onBackToDashboardClick: () -> Unit,
    onSwitchToClassicLedgerClick: () -> Unit = {},
    onSettleUpClick: () -> Unit = {},
    onViewTicketClick: (String) -> Unit = {},
    onAddTravelClick: () -> Unit = {},
    onUploadFlightPdfClick: () -> Unit = {},
    onLogQuickExpenseClick: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onViewBalancesClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deviceContacts by viewModel.deviceContacts.collectAsStateWithLifecycle()
    val isLoadingContacts by viewModel.isLoadingContacts.collectAsStateWithLifecycle()
    var showAddContactsSheet by remember { mutableStateOf(false) }

    val resolvedGroupId = remember(tripId, tripName, uiState.groups, uiState.activeGroupId) {
        when {
            tripId.isNotBlank() && uiState.groups.any { it.groupId == tripId } -> tripId
            tripName.isNotBlank() -> uiState.groups.find {
                it.name.equals(tripName, ignoreCase = true) ||
                    it.groupId.equals(tripId, ignoreCase = true)
            }?.groupId ?: tripId.ifBlank { uiState.activeGroupId }
            else -> uiState.activeGroupId
        }
    }

    val contactPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.loadDeviceContacts(context)
            showAddContactsSheet = true
        }
    }

    val resolvedGroup = uiState.groups.find { it.groupId == resolvedGroupId }
    if (showAddContactsSheet && resolvedGroup != null) {
        ContactPickerBottomSheet(
            contacts = deviceContacts,
            isLoading = isLoadingContacts,
            multiSelect = true,
            title = "Add Contacts to ${resolvedGroup.name}",
            subtitle = "Select contacts to add to this group",
            onDismissRequest = { showAddContactsSheet = false },
            onConfirmSelected = { selected ->
                viewModel.addContactsToGroup(resolvedGroup.groupId, selected)
                showAddContactsSheet = false
            }
        )
    }

    TripHomeScreen(
        viewModel = viewModel,
        groupId = resolvedGroupId,
        initialTab = initialTab.toSectionTab(),
        onBackClick = onBackToDashboardClick,
        onSwitchToClassicLedgerClick = {
            viewModel.openGroupDetail(resolvedGroupId)
            onSwitchToClassicLedgerClick()
        },
        onOpenTrainPnrReviewClick = { pnr ->
            if (pnr.isNotBlank()) onViewTicketClick(pnr) else onAddTravelClick()
        },
        onOpenFlightReviewClick = { pnr ->
            if (pnr.isNotBlank()) onViewTicketClick(pnr) else onUploadFlightPdfClick()
        },
        onLogQuickExpenseClick = onLogQuickExpenseClick,
        onOpenSettleUpClick = onSettleUpClick,
        onAddMemberClick = {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                viewModel.loadDeviceContacts(context)
                showAddContactsSheet = true
            } else {
                contactPermLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    )
}

// ==============================================================================
// 4. GLOBAL BOTTOM NAVIGATION BAR COMPONENT (WCAG 2.5.5 48.dp Touch Targets)
// ==============================================================================
@Composable
fun SplitMateGlobalBottomBar(
    selectedTab: GlobalNavTab,
    onTabSelected: (GlobalNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = SplitMateTheme.isDark
    val selectedPillBg = if (isDark) Color(0xFF233216) else Color(0xFFD7E8B6)
    val selectedContentColor = if (isDark) Color(0xFFD7E8B6) else Color(0xFF23201E)
    val unselectedContentColor = SplitMateTheme.TextSecondary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = SplitMateTheme.SurfaceWhite,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, SplitMateTheme.BorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlobalNavTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                Surface(
                    onClick = { onTabSelected(tab) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (isSelected) selectedPillBg else Color.Transparent,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) selectedContentColor else unselectedContentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        AnimatedVisibility(visible = isSelected) {
                            Text(
                                text = "  ${tab.label}",
                                color = selectedContentColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                fontFamily = SplitMateTheme.FontRounded
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolveFlightTicketByPnr(
    context: Context,
    pnrRaw: String
): UniversalFlightTicketExtractor.UniversalFlightTicketResult? {
    val normalized = PnrNetworkRepository.normalizePnrKey(pnrRaw)
    if (normalized.length != 6) return null

    PnrNetworkRepository.loadConfirmedFlightTicketResult(context, normalized)?.let {
        return it
    }

    val persistedSnap = PnrNetworkRepository.loadPersistedPnrSnapshot(context, normalized) ?: return null
    return UniversalFlightTicketExtractor.UniversalFlightTicketResult(
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
            UniversalFlightTicketExtractor.ExtractedFlightPassenger(
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
}
