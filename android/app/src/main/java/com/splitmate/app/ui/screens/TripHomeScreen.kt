@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.splitmate.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonPin
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.splitmate.app.SplitMateMathEngine
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseGroupEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.data.PnrNetworkRepository
import com.splitmate.app.ui.BuckwheatOlivePrimary
import com.splitmate.app.ui.BuckwheatPeachContainer
import com.splitmate.app.ui.BuckwheatSageContainer
import com.splitmate.app.ui.BuckwheatTerracottaDark
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.FigtreeFontFamily
import com.splitmate.app.ui.LivePnrStatusSnapshot
import com.splitmate.app.ui.ParsedTravelTicket
import com.splitmate.app.ui.SettlementTransferUiModel
import com.splitmate.app.ui.SplitMateTnumMonospace
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.cleanDisplayExpenseTitle
import com.splitmate.app.ui.cleanIndianTenDigitPhone
import com.splitmate.app.ui.components.ActiveTravelPassMode
import com.splitmate.app.ui.components.AnimatedTransitDeckHeroCard
import com.splitmate.app.ui.components.UpiExpressPaymentSheet
import com.splitmate.app.ui.dialogs.PerspectiveAndSyncHeaderPill
import com.splitmate.app.ui.dialogs.TripSyncAndPerspectiveSheet
import com.splitmate.app.ui.extractInitialsFromNameOrSeed
import com.splitmate.app.ui.extractTravelTicketFromTitle
import com.splitmate.app.ui.formatIndianRupeesFromCents
import com.splitmate.app.ui.isFlightTicketExpense
import com.splitmate.app.ui.loadPersistedPnrSnapshot
import com.splitmate.app.ui.performCrispTactileHaptic
import com.splitmate.app.ui.resolveStationDisplayName
import com.splitmate.app.ui.toSmartTitleCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// ==============================================================================
// 1. TRIP HUB SECTION TABS & CATEGORY CLASSIFIER (100% REAL ROOM DATA)
// ==============================================================================

enum class TripHubSectionTab(val title: String) {
    OVERVIEW("Overview"),
    PLAN("Plan"),
    TRAVEL("Travel"),
    MONEY("Money"),
    PEOPLE("People")
}

enum class TripHubBookingCategory(val filterTitle: String, val icon: ImageVector) {
    ALL("All", Icons.Rounded.ViewAgenda),
    TRAIN("Trains", Icons.Rounded.Train),
    FLIGHT("Flights", Icons.Rounded.FlightTakeoff),
    STAY("Stays", Icons.Rounded.Apartment),
    RENTAL("Rentals", Icons.Rounded.TwoWheeler),
    CAB("Cabs", Icons.Rounded.DirectionsCar),
    GENERAL("Other", Icons.AutoMirrored.Rounded.ReceiptLong)
}

val ExpenseEntity.createdAtEpochMs: Long
    get() = this.createdAt

val ExpenseEntity.amountCents: Long
    get() = this.totalAmountCents

/**
 * Helper extension on [SplitMateViewModel] to record a Greedy Minimum Cash Flow settlement
 * scoped to [groupId] while keeping Room & reactive UI state synchronized.
 */
fun SplitMateViewModel.recordSettlement(
    groupId: String,
    fromMemberId: String,
    toMemberId: String,
    amountCents: Long
) {
    if (groupId.isNotBlank()) {
        selectActiveGroup(groupId)
    }
    val groupMembers = uiState.value.members.filter { it.groupId == groupId }
    val fromName = groupMembers.find { it.memberId == fromMemberId }?.name ?: fromMemberId
    val toName = groupMembers.find { it.memberId == toMemberId }?.name ?: toMemberId
    markGreedyTransferSettled(
        SplitMateMathEngine.SimplifiedTransfer(
            fromMemberId = fromMemberId,
            fromName = fromName,
            toMemberId = toMemberId,
            toName = toName,
            amountCents = amountCents
        )
    )
}

/**
 * Deterministic Room-to-Card Classifier (`classifyGroupExpenseForTripHub`):
 * Maps any real [ExpenseEntity] logged in Room to its canonical Stitch v2.0 card archetype.
 */
fun classifyGroupExpenseForTripHub(expense: ExpenseEntity): TripHubBookingCategory {
    val rawTitle = expense.title.trim()
    val lower = rawTitle.lowercase(Locale.US)
    val parsedTicket = extractTravelTicketFromTitle(rawTitle)

    // 1. Flight check (6-char alphanumeric PNR or airline keywords)
    if (isFlightTicketExpense(rawTitle, parsedTicket) ||
        Regex("""\b(flight|indigo|air india|akasa|spicejet|vistara|boarding pass)\b""").containsMatchIn(lower) ||
        Regex("""\b(6e|ai|qp|sg|uk)-\d{2,4}\b""").containsMatchIn(lower)
    ) {
        return TripHubBookingCategory.FLIGHT
    }

    // 2. Train check (10-digit numeric PNR or Indian Railways keywords)
    val hasTenDigitPnr = Regex("""\b\d{10}\b""").containsMatchIn(rawTitle) ||
        (parsedTicket?.pnr?.length == 10 && parsedTicket.pnr.all { it.isDigit() })
    if (hasTenDigitPnr ||
        (parsedTicket != null && parsedTicket.hasTicketMetadata) ||
        Regex("""\b(train|irctc|pnr|express|shatabdi|rajdhani|vande bharat|sleeper|3a|2a|1a)\b""").containsMatchIn(lower)
    ) {
        return TripHubBookingCategory.TRAIN
    }

    // 3. Lodging & Stays check
    if (Regex("""\b(hotel|resort|stay|villa|airbnb|hostel|cottage|lodge|homestay|check-in|nights|room)\b""").containsMatchIn(lower)) {
        return TripHubBookingCategory.STAY
    }

    // 4. Two-Wheeler / Adventure Rental check
    if (Regex("""\b(rental|enfield|moped|scooter|bike|scooty|two wheeler|cycle|kayak|coracle)\b""").containsMatchIn(lower)) {
        return TripHubBookingCategory.RENTAL
    }

    // 5. Cab / Station Transfer / Ground Transit check
    if (Regex("""\b(cab|taxi|uber|ola|rapido|auto|rickshaw|innova|transfer|pickup|drop|bus)\b""").containsMatchIn(lower)) {
        return TripHubBookingCategory.CAB
    }

    // 6. General Shared Expense
    return TripHubBookingCategory.GENERAL
}

// ==============================================================================
// 2. LIGHT & DARK MODE TOKENS (`TripHubTokens`) — BUCKWHEAT & WARM ESPRESSO
// ==============================================================================

object TripHubTokens {
    val CanvasBg: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF181512) else Color(0xFFFAF6F0)
    val CardSurface: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF24201C) else Color(0xFFFFFFFF)
    val SunkenWell: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF2E2823) else Color(0xFFF4EFE6)
    val CardBorder: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF38312B) else Color(0xFFEDE7DF)
    val TextPrimary: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFFAF6F0) else Color(0xFF23201E)
    val TextSecondary: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFB5ACA2) else Color(0xFF6E675F)
    val TextMuted: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF857D73) else Color(0xFF8C857B)

    val ActiveTabPillBg: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFFAF6F0) else Color(0xFF1E1C1A)
    val ActiveTabPillText: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF181512) else Color(0xFFFFFFFF)
    val InactiveTabPillBg: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF28241F) else Color(0xFFEFEAE1)
    val InactiveTabPillText: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFC5BDB3) else Color(0xFF4A453E)

    // Deep Forest Green Train Pass Tokens (Sunlight-Grade Contrast >= 5.8:1)
    val TrainForestTop: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF264210) else Color(0xFF2D4F12)
    val TrainForestBottom: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF1B3009) else Color(0xFF213B0C)
    val TrainNextUpPillBg: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF759943) else Color(0xFF84A950)
    val TrainNextUpPillText: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF0F1F03) else Color(0xFF132604)
    val TrainBerthCellBg: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF1A300A) else Color(0xFF223D0D)
    val TrainBerthCellBorder: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF345718) else Color(0xFF3E651E)
    val TrainAccentLime = Color(0xFFB5DC86)     // > 7:1 contrast on Deep Forest Green
    val TrainSecondarySage = Color(0xFFD7E8B6)  // > 5.8:1 contrast on Deep Forest Green
    val TrainPrimaryCtaBg: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF8BB85A) else Color(0xFF80AD47)
    val TrainPrimaryCtaText = Color(0xFF132604)

    // Aviation Periwinkle Flight Pass Tokens
    val FlightNavyTop: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF242059) else Color(0xFF2B2768)
    val FlightNavyBottom: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF16133B) else Color(0xFF1B1849)
    val FlightSecondaryLavender = Color(0xFFDCE3FD)
    val FlightAccentPeriwinkle = Color(0xFFC7D2FE)

    // Semantic Status & Category Badges
    val PositiveSageText: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFD7E8B6) else Color(0xFF416913)
    val PositiveSagePillBg: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF233216) else Color(0xFFD7E8B6)
    val WarningRacText: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFFDBA74) else Color(0xFF9A3412)
    val TerracottaPeachBg: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF3D231B) else Color(0xFFFCE3D7)
    val TerracottaIconTint: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFFEB49C) else Color(0xFF9A3412)
    val PeriwinkleBoxBg: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF282552) else Color(0xFFDCE3FD)
    val PeriwinkleIconTint: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFC7D2FE) else Color(0xFF3730A3)
}

// ==============================================================================
// 3. FLEXIBLE ENTRY COMPOSABLES (`TripHomeScreen` Primary & Convenience Overload)
// ==============================================================================

/**
 * Convenience overload for `SplitMateAppNavHost` and standalone callers.
 * Bridges seamlessly into the primary Room-backed [TripHomeScreen].
 */
@Composable
fun TripHomeScreen(
    tripName: String = "Trip Hub",
    tripId: String = "",
    @Suppress("UNUSED_PARAMETER") tripDates: String = "",
    @Suppress("UNUSED_PARAMETER") totalSpendFormatted: String = "",
    @Suppress("UNUSED_PARAMETER") netOwedFormatted: String = "",
    viewModel: SplitMateViewModel? = null,
    initialTab: TripHubSectionTab = TripHubSectionTab.OVERVIEW,
    onBackClick: () -> Unit = {},
    onSwitchToClassicLedgerClick: () -> Unit = {},
    onSettleUpClick: () -> Unit = {},
    onAddTravelObjectClick: () -> Unit = {},
    onViewTicketClick: (String) -> Unit = {},
    onOpenFlightReviewClick: (String) -> Unit = {},
    onLogQuickExpenseClick: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onViewBalancesClick: () -> Unit = {},
    onAddMemberClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val resolvedViewModel = viewModel ?: remember { SplitMateViewModel() }
    val uiState by resolvedViewModel.uiState.collectAsStateWithLifecycle()
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

    TripHomeScreen(
        viewModel = resolvedViewModel,
        groupId = resolvedGroupId,
        initialTab = initialTab,
        onBackClick = onBackClick,
        onSwitchToClassicLedgerClick = onSwitchToClassicLedgerClick,
        onOpenTrainPnrReviewClick = { pnr ->
            if (pnr.isNotBlank()) onViewTicketClick(pnr) else onAddTravelObjectClick()
        },
        onOpenFlightReviewClick = onOpenFlightReviewClick,
        onLogQuickExpenseClick = onLogQuickExpenseClick,
        onOpenSettleUpClick = onSettleUpClick,
        onAddMemberClick = onAddMemberClick,
        modifier = modifier
    )
}

/**
 * Primary ViewModel-backed Stitch v2.0 Shared Trip Hub (`TripHomeScreen`).
 *
 * 100% real-Room-data-backed:
 * - Zero hardcoded demo dates (all date headers and subtitles derive strictly from Room timestamps)
 * - Zero fake static tourist props
 * - Dynamic category sub-filter chips materialize strictly when `count > 0` in Room
 * - Sunlight-grade contrast (`>= 5.8:1`) on Deep-Forest Green Train & Aviation Periwinkle Flight cards
 * - WCAG 2.5.5 `48.dp` minimum touch targets across all interactive pills, chips, and buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHomeScreen(
    viewModel: SplitMateViewModel,
    groupId: String,
    initialTab: TripHubSectionTab = TripHubSectionTab.OVERVIEW,
    onBackClick: () -> Unit = {},
    onSwitchToClassicLedgerClick: () -> Unit = {},
    onOpenTrainPnrReviewClick: (pnr: String) -> Unit = {},
    onOpenFlightReviewClick: (pnrOrTrigger: String) -> Unit = {},
    onLogQuickExpenseClick: () -> Unit = {},
    onOpenSettleUpClick: () -> Unit = {},
    onAddMemberClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localView = LocalView.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val resolvedGroupId = remember(groupId, uiState.groups, uiState.activeGroupId) {
        if (groupId.isNotBlank() && uiState.groups.any { it.groupId == groupId }) {
            groupId
        } else {
            uiState.activeGroup?.groupId ?: uiState.activeGroupId
        }
    }

    LaunchedEffect(resolvedGroupId) {
        if (resolvedGroupId.isNotBlank() && uiState.activeGroupId != resolvedGroupId) {
            viewModel.selectActiveGroup(resolvedGroupId)
        }
    }

    val group: ExpenseGroupEntity? = remember(uiState.groups, resolvedGroupId) {
        uiState.groups.find { it.groupId == resolvedGroupId } ?: uiState.activeGroup
    }
    val groupMembers: List<GroupMemberEntity> = remember(uiState.members, resolvedGroupId) {
        uiState.members.filter { it.groupId == resolvedGroupId }
    }
    val activePerspectiveMember: GroupMemberEntity? = remember(groupMembers) {
        groupMembers.find { it.isCurrentUser } ?: groupMembers.firstOrNull()
    }
    val groupExpenses: List<ExpenseEntity> = remember(uiState.expenses, resolvedGroupId) {
        uiState.expenses
            .filter { it.groupId == resolvedGroupId }
            .sortedBy { it.createdAtEpochMs }
    }
    val groupExpenseIds = remember(groupExpenses) {
        groupExpenses.map { it.expenseId }.toSet()
    }
    val groupSplits: List<ExpenseSplitEntity> = remember(uiState.splits, groupExpenseIds) {
        uiState.splits.filter { groupExpenseIds.contains(it.expenseId) }
    }

    // Net balances & total spend strictly computed from real Room records
    val netBalancesMap: Map<String, Long> = remember(
        resolvedGroupId,
        groupMembers,
        groupExpenses,
        groupSplits,
        uiState.settlements
    ) {
        viewModel.computeGroupMemberNetBalances(resolvedGroupId)
    }
    val totalGroupSpendCents: Long = remember(groupExpenses) {
        groupExpenses.sumOf { it.totalAmountCents }
    }
    val activeMemberNetCents: Long = remember(activePerspectiveMember, netBalancesMap) {
        if (activePerspectiveMember != null) {
            netBalancesMap[activePerspectiveMember.memberId] ?: 0L
        } else {
            0L
        }
    }

    // Classify every real expense in Room
    val classifiedExpenses = remember(groupExpenses) {
        groupExpenses.map { exp -> exp to classifyGroupExpenseForTripHub(exp) }
    }

    // Adaptive terminology: travel groups ("Flight"/"Cabin" category in Room, or any logged Train/Flight
    // booking) use "Travelers"/"Trip Spend"; household/dining/event ledgers use "Members"/"Group Spend".
    val isTravelGroup: Boolean = remember(group?.iconName, classifiedExpenses) {
        val travelIconIds = setOf("Flight", "Cabin")
        val hasTravelCategory = group?.iconName?.let { icon ->
            travelIconIds.any { it.equals(icon.trim(), ignoreCase = true) }
        } ?: true
        hasTravelCategory || classifiedExpenses.any { (_, cat) ->
            cat == TripHubBookingCategory.TRAIN || cat == TripHubBookingCategory.FLIGHT
        }
    }

    // Dynamic subtitle derived strictly from real Room timestamps
    val dynamicTripSubtitle = remember(groupExpenses, groupMembers.size, isTravelGroup) {
        val memberNoun = when {
            isTravelGroup && groupMembers.size == 1 -> "Traveler"
            isTravelGroup -> "Travelers"
            groupMembers.size == 1 -> "Member"
            else -> "Members"
        }
        val travelerLabel = "${groupMembers.size} $memberNoun"
        if (groupExpenses.isEmpty()) {
            if (isTravelGroup) "$travelerLabel · Ready to log bookings" else "$travelerLabel · Ready to log expenses"
        } else {
            val minEpoch = groupExpenses.minOf { it.createdAtEpochMs }
            val maxEpoch = groupExpenses.maxOf { it.createdAtEpochMs }
            val fmt = SimpleDateFormat("dd MMM", Locale.US)
            val startStr = fmt.format(Date(minEpoch))
            val endStr = fmt.format(Date(maxEpoch))
            val rangeStr = if (startStr == endStr) startStr else "$startStr - $endStr"
            "$rangeStr · $travelerLabel"
        }
    }

    var selectedSectionTab by remember(initialTab) { mutableStateOf(initialTab) }
    var selectedCategoryFilter by remember { mutableStateOf(TripHubBookingCategory.ALL) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var showSyncAndPerspectiveSheet by remember { mutableStateOf(false) }
    var showAddBookingBottomSheet by remember { mutableStateOf(false) }
    var inspectedBerthChartSnapshot by remember { mutableStateOf<Pair<ExpenseEntity, LivePnrStatusSnapshot?>?>(null) }
    var activeUpiSettlementTransfer by remember { mutableStateOf<SettlementTransferUiModel?>(null) }

    // Category counts in Room (strictly > 0 for sub-filter chips)
    val categoryCounts: Map<TripHubBookingCategory, Int> = remember(classifiedExpenses) {
        val counts = LinkedHashMap<TripHubBookingCategory, Int>()
        if (classifiedExpenses.isNotEmpty()) {
            counts[TripHubBookingCategory.ALL] = classifiedExpenses.size
            TripHubBookingCategory.entries
                .filter { it != TripHubBookingCategory.ALL }
                .forEach { cat ->
                    val count = classifiedExpenses.count { it.second == cat }
                    if (count > 0) {
                        counts[cat] = count
                    }
                }
        }
        counts
    }

    // Reset filter to ALL if selected category count drops to 0
    LaunchedEffect(categoryCounts) {
        if (selectedCategoryFilter != TripHubBookingCategory.ALL &&
            (categoryCounts[selectedCategoryFilter] ?: 0) == 0
        ) {
            selectedCategoryFilter = TripHubBookingCategory.ALL
        }
    }

    // Apply search query & category filter
    val filteredClassifiedExpenses = remember(
        classifiedExpenses,
        selectedCategoryFilter,
        searchQuery,
        groupMembers
    ) {
        val q = searchQuery.trim().lowercase(Locale.US)
        classifiedExpenses.filter { (exp, cat) ->
            val matchesCategory = selectedCategoryFilter == TripHubBookingCategory.ALL || cat == selectedCategoryFilter
            val payerName = groupMembers.find { it.memberId == exp.payerId }?.name.orEmpty().lowercase(Locale.US)
            val matchesSearch = q.isEmpty() ||
                exp.title.lowercase(Locale.US).contains(q) ||
                payerName.contains(q)
            matchesCategory && matchesSearch
        }
    }

    // Track first chronological train leg id so Leg 1 renders as DeepGreenTrainTicketCard
    // and Leg 2+ renders as ReturnTransitTrainCard (with inline expand toggle)
    val firstTrainExpenseId: String? = remember(classifiedExpenses) {
        classifiedExpenses.firstOrNull { it.second == TripHubBookingCategory.TRAIN }?.first?.expenseId
    }

    Scaffold(
        containerColor = TripHubTokens.CanvasBg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    performCrispTactileHaptic(context, localView, heavy = false)
                    showAddBookingBottomSheet = true
                },
                containerColor = TripHubTokens.ActiveTabPillBg,
                contentColor = TripHubTokens.ActiveTabPillText,
                shape = CircleShape,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .defaultMinSize(minHeight = 52.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add Booking",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Booking",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TripHubTokens.CanvasBg)
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // =================================================================
            // SUBTASK 3.1.1: TOP APP BAR + PERSPECTIVE & SYNC PILL BAR
            // =================================================================
            TripHubTopBar(
                groupName = (group?.name ?: "Trip Hub").toSmartTitleCase(),
                subtitle = dynamicTripSubtitle,
                activePerspectiveMember = activePerspectiveMember,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onToggleSearch = {
                    performCrispTactileHaptic(context, localView, heavy = false)
                    isSearchExpanded = !isSearchExpanded
                    if (!isSearchExpanded) searchQuery = ""
                },
                onBackClick = {
                    performCrispTactileHaptic(context, localView, heavy = false)
                    onBackClick()
                },
                onSwitchToClassicLedgerClick = {
                    performCrispTactileHaptic(context, localView, heavy = false)
                    onSwitchToClassicLedgerClick()
                },
                onOpenSyncAndPerspectiveSheet = {
                    performCrispTactileHaptic(context, localView, heavy = false)
                    showSyncAndPerspectiveSheet = true
                }
            )

            // =================================================================
            // SUBTASK 3.1.2: 5 SECTION PILLS + DYNAMIC CATEGORY SUB-CHIPS
            // =================================================================
            TripHubSectionTabsRow(
                selectedTab = selectedSectionTab,
                onSelectTab = { newTab ->
                    performCrispTactileHaptic(context, localView, heavy = false)
                    selectedSectionTab = newTab
                }
            )

            if ((selectedSectionTab == TripHubSectionTab.OVERVIEW || selectedSectionTab == TripHubSectionTab.TRAVEL) &&
                groupExpenses.isNotEmpty()
            ) {
                DynamicCategorySubFilterRow(
                    categoryCounts = categoryCounts,
                    selectedCategory = selectedCategoryFilter,
                    onSelectCategory = { cat ->
                        performCrispTactileHaptic(context, localView, heavy = false)
                        selectedCategoryFilter = cat
                    }
                )
            }

            // =================================================================
            // SUBTASK 3.1.3: COMPACT PERSPECTIVE NET BALANCE STRIP
            // =================================================================
            CompactPerspectiveNetBalanceStrip(
                totalGroupSpendCents = totalGroupSpendCents,
                activeMemberNetCents = activeMemberNetCents,
                activeMemberName = activePerspectiveMember?.name ?: "You",
                onSettleUpClick = {
                    performCrispTactileHaptic(context, localView, heavy = false)
                    if (selectedSectionTab != TripHubSectionTab.MONEY) {
                        selectedSectionTab = TripHubSectionTab.MONEY
                    } else {
                        onOpenSettleUpClick()
                    }
                },
                isTravelGroup = isTravelGroup
            )

            // =================================================================
            // SECTION BODY CONTENT (`Overview`, `Plan`, `Travel`, `Money`, `People`)
            // =================================================================
            when (selectedSectionTab) {
                TripHubSectionTab.OVERVIEW -> {
                    TripHubOverviewFeed(
                        filteredClassifiedExpenses = filteredClassifiedExpenses,
                        firstTrainExpenseId = firstTrainExpenseId,
                        groupMembers = groupMembers,
                        allSplits = groupSplits,
                        activePerspectiveMember = activePerspectiveMember,
                        onOpenTrainPnrReviewClick = onOpenTrainPnrReviewClick,
                        onOpenFlightReviewClick = onOpenFlightReviewClick,
                        onLogQuickExpenseClick = onLogQuickExpenseClick,
                        onInspectBerthChart = { exp, snap ->
                            inspectedBerthChartSnapshot = exp to snap
                        },
                        onDeleteExpense = { expenseId ->
                            viewModel.rollbackExpense(expenseId)
                        }
                    )
                }

                TripHubSectionTab.PLAN -> {
                    TripHubPlanTimelineView(
                        classifiedExpenses = filteredClassifiedExpenses,
                        groupMembers = groupMembers,
                        onOpenTrainPnrReviewClick = onOpenTrainPnrReviewClick,
                        onOpenFlightReviewClick = onOpenFlightReviewClick,
                        onLogQuickExpenseClick = onLogQuickExpenseClick
                    )
                }

                TripHubSectionTab.TRAVEL -> {
                    TripHubTravelWalletView(
                        classifiedExpenses = filteredClassifiedExpenses,
                        firstTrainExpenseId = firstTrainExpenseId,
                        groupMembers = groupMembers,
                        allSplits = groupSplits,
                        activePerspectiveMember = activePerspectiveMember,
                        onOpenTrainPnrReviewClick = onOpenTrainPnrReviewClick,
                        onOpenFlightReviewClick = onOpenFlightReviewClick,
                        onSwitchToClassicLedgerClick = onSwitchToClassicLedgerClick,
                        onInspectBerthChart = { exp, snap ->
                            inspectedBerthChartSnapshot = exp to snap
                        },
                        onDeleteExpense = { expenseId ->
                            viewModel.rollbackExpense(expenseId)
                        }
                    )
                }

                TripHubSectionTab.MONEY -> {
                    TripHubMoneySettlementView(
                        viewModel = viewModel,
                        groupId = resolvedGroupId,
                        groupName = group?.name ?: "Trip Hub",
                        groupMembers = groupMembers,
                        netBalancesMap = netBalancesMap,
                        onOpenUpiPaymentSheet = { transferUiModel ->
                            activeUpiSettlementTransfer = transferUiModel
                        },
                        onOpenSettleUpClick = onOpenSettleUpClick
                    )
                }

                TripHubSectionTab.PEOPLE -> {
                    TripHubPeoplePerspectiveView(
                        viewModel = viewModel,
                        groupId = resolvedGroupId,
                        groupMembers = groupMembers,
                        netBalancesMap = netBalancesMap,
                        onAddMemberClick = onAddMemberClick,
                        onOpenSyncSheetClick = {
                            showSyncAndPerspectiveSheet = true
                        },
                        isTravelGroup = isTravelGroup
                    )
                }
            }
        }
    }

    // =========================================================================
    // MODAL 1: PERSPECTIVE SWITCHER & P2P TRIP SYNC CAPSULE SHEET
    // =========================================================================
    if (showSyncAndPerspectiveSheet) {
        TripSyncAndPerspectiveSheet(
            viewModel = viewModel,
            groupId = resolvedGroupId,
            groupName = group?.name ?: "Trip Hub",
            members = groupMembers,
            onDismiss = { showSyncAndPerspectiveSheet = false }
        )
    }

    // =========================================================================
    // MODAL 2: `+ ADD BOOKING` QUICK BOOKING SHEET (Subtask 3.3.5)
    // =========================================================================
    if (showAddBookingBottomSheet) {
        AddBookingQuickSheet(
            onDismiss = { showAddBookingBottomSheet = false },
            onSelectTrainPnr = {
                showAddBookingBottomSheet = false
                onOpenTrainPnrReviewClick("")
            },
            onSelectFlightPass = {
                showAddBookingBottomSheet = false
                onOpenFlightReviewClick("")
            },
            onSelectSharedExpense = {
                showAddBookingBottomSheet = false
                onLogQuickExpenseClick()
            },
            onSelectSyncAndPerspective = {
                showAddBookingBottomSheet = false
                showSyncAndPerspectiveSheet = true
            }
        )
    }

    // =========================================================================
    // MODAL 3: INLINE BERTH & COACH INSPECTOR DIALOG
    // =========================================================================
    val berthInspection = inspectedBerthChartSnapshot
    if (berthInspection != null) {
        val (exp, snap) = berthInspection
        val parsedTicket = remember(exp.title) { extractTravelTicketFromTitle(exp.title) }
        val resolvedPnr = snap?.pnr?.ifBlank { parsedTicket?.pnr.orEmpty() } ?: parsedTicket?.pnr.orEmpty()
        BerthChartInspectorDialog(
            expense = exp,
            snapshot = snap,
            parsedTicket = parsedTicket,
            groupMembers = groupMembers,
            allSplits = groupSplits,
            activePerspectiveMember = activePerspectiveMember,
            onDismiss = { inspectedBerthChartSnapshot = null },
            onOpenFullETicket = {
                inspectedBerthChartSnapshot = null
                onOpenTrainPnrReviewClick(resolvedPnr)
            }
        )
    }

    // =========================================================================
    // MODAL 4: UPI EXPRESS PAYMENT SHEET (From `Money` Tab)
    // =========================================================================
    val currentUpiTransfer = activeUpiSettlementTransfer
    if (currentUpiTransfer != null) {
        val creditorMember = groupMembers.find { it.memberId == currentUpiTransfer.toMemberId }
        UpiExpressPaymentSheet(
            transferModel = currentUpiTransfer,
            groupName = group?.name ?: "Trip Hub",
            initialSavedUpiId = creditorMember?.upiId ?: currentUpiTransfer.upiId,
            onSaveMemberUpi = { updatedUpi ->
                if (creditorMember != null) {
                    viewModel.updateFriendUpi(
                        memberId = creditorMember.memberId,
                        newName = creditorMember.name,
                        newUpiId = updatedUpi,
                        newAvatarSeed = creditorMember.avatarSeed
                    )
                }
            },
            onMarkSettled = {
                viewModel.recordSettlement(
                    groupId = resolvedGroupId,
                    fromMemberId = currentUpiTransfer.fromMemberId,
                    toMemberId = currentUpiTransfer.toMemberId,
                    amountCents = currentUpiTransfer.transfer.amountCents
                )
                activeUpiSettlementTransfer = null
            },
            onDismiss = { activeUpiSettlementTransfer = null }
        )
    }
}

// ==============================================================================
// 4. TASK 3.1 COMPONENTS: TOP BAR, 5 PILL TABS, DYNAMIC CHIPS & BALANCE STRIP
// ==============================================================================

@Composable
private fun TripHubTopBar(
    groupName: String,
    subtitle: String,
    activePerspectiveMember: GroupMemberEntity?,
    isSearchExpanded: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onBackClick: () -> Unit,
    onSwitchToClassicLedgerClick: () -> Unit,
    onOpenSyncAndPerspectiveSheet: () -> Unit
) {
    val context = LocalContext.current
    val localView = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to Ledgers",
                        tint = TripHubTokens.TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = groupName,
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = TripHubTokens.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = TextStyle(
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = TripHubTokens.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = "Search Bookings and Expenses",
                        tint = TripHubTokens.TextPrimary,
                        modifier = Modifier.size(21.dp)
                    )
                }

                IconButton(
                    onClick = onSwitchToClassicLedgerClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ViewAgenda,
                        contentDescription = "Switch to Classic Ledger & 3D Pass",
                        tint = TripHubTokens.TextPrimary,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }

        // Perspective & Sync Header Pill + Share Sync Action Pill (both enforce >= 48.dp touch bounds)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PerspectiveAndSyncHeaderPill(
                activeMember = activePerspectiveMember,
                onClick = onOpenSyncAndPerspectiveSheet,
                modifier = Modifier.weight(1f, fill = false)
            )

            Surface(
                onClick = {
                    performCrispTactileHaptic(context, localView, heavy = false)
                    onOpenSyncAndPerspectiveSheet()
                },
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(TripHubTokens.SunkenWell, CircleShape)
                            .border(1.dp, TripHubTokens.CardBorder, CircleShape)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share Sync Capsule",
                            tint = TripHubTokens.TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Share Sync",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TripHubTokens.TextPrimary
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = isSearchExpanded) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search PNR, train, flight, hotel, or payer...",
                        fontFamily = FigtreeFontFamily,
                        fontSize = 13.sp,
                        color = TripHubTokens.TextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = TripHubTokens.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = TripHubTokens.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TripHubTokens.TextPrimary,
                    unfocusedTextColor = TripHubTokens.TextPrimary,
                    focusedContainerColor = TripHubTokens.CardSurface,
                    unfocusedContainerColor = TripHubTokens.CardSurface,
                    focusedBorderColor = BuckwheatOlivePrimary,
                    unfocusedBorderColor = TripHubTokens.CardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TripHubSectionTabsRow(
    selectedTab: TripHubSectionTab,
    onSelectTab: (TripHubSectionTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TripHubSectionTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            val pillBg = if (isSelected) TripHubTokens.ActiveTabPillBg else TripHubTokens.InactiveTabPillBg
            val pillText = if (isSelected) TripHubTokens.ActiveTabPillText else TripHubTokens.InactiveTabPillText

            Surface(
                onClick = { onSelectTab(tab) },
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(pillBg, CircleShape)
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = pillText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicCategorySubFilterRow(
    categoryCounts: Map<TripHubBookingCategory, Int>,
    selectedCategory: TripHubBookingCategory,
    onSelectCategory: (TripHubBookingCategory) -> Unit
) {
    if (categoryCounts.isEmpty()) return

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            items = categoryCounts.entries.toList(),
            key = { it.key.name }
        ) { (category, count) ->
            val isSelected = selectedCategory == category
            val chipBg = if (isSelected) TripHubTokens.ActiveTabPillBg else TripHubTokens.InactiveTabPillBg
            val chipText = if (isSelected) TripHubTokens.ActiveTabPillText else TripHubTokens.InactiveTabPillText

            Surface(
                onClick = { onSelectCategory(category) },
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(chipBg, CircleShape)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else TripHubTokens.CardBorder,
                                shape = CircleShape
                            )
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (category != TripHubBookingCategory.ALL) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                tint = chipText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "${category.filterTitle} ($count)",
                            style = TextStyle(
                                fontFamily = FigtreeFontFamily,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 12.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = chipText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactPerspectiveNetBalanceStrip(
    totalGroupSpendCents: Long,
    activeMemberNetCents: Long,
    activeMemberName: String,
    onSettleUpClick: () -> Unit,
    isTravelGroup: Boolean = true
) {
    val isDark = SplitMateTheme.isDark
    val formattedTotalSpend = formatIndianRupeesFromCents(
        cents = totalGroupSpendCents,
        includePlusSign = false,
        currencySymbol = "₹"
    )
    val formattedAbsNet = formatIndianRupeesFromCents(
        cents = abs(activeMemberNetCents),
        includePlusSign = false,
        currencySymbol = "₹"
    )

    val netBadgeText = when {
        activeMemberNetCents > 0L -> "YOU GET BACK +$formattedAbsNet"
        activeMemberNetCents < 0L -> "YOU OWE -$formattedAbsNet"
        else -> "ALL SETTLED ₹0.00"
    }
    val netBadgeBg = when {
        activeMemberNetCents > 0L && isDark -> Color(0xFF233216)
        activeMemberNetCents > 0L -> BuckwheatSageContainer
        activeMemberNetCents < 0L && isDark -> Color(0xFF3A2019)
        activeMemberNetCents < 0L -> BuckwheatPeachContainer
        else -> TripHubTokens.SunkenWell
    }
    val netBadgeTextColor = when {
        activeMemberNetCents > 0L && isDark -> BuckwheatSageContainer
        activeMemberNetCents > 0L -> BuckwheatOlivePrimary
        activeMemberNetCents < 0L && isDark -> Color(0xFFFECDD3)
        activeMemberNetCents < 0L -> BuckwheatTerracottaDark
        else -> TripHubTokens.TextSecondary
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = TripHubTokens.CardSurface,
        border = BorderStroke(1.dp, TripHubTokens.CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${if (isTravelGroup) "Trip Spend" else "Group Spend"}: $formattedTotalSpend",
                        style = TextStyle(
                            fontFamily = SplitMateTnumMonospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = TripHubTokens.TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = netBadgeBg
                ) {
                    Text(
                        text = "$activeMemberName · $netBadgeText",
                        style = TextStyle(
                            fontFamily = SplitMateTnumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = netBadgeTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Surface(
                onClick = onSettleUpClick,
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(TripHubTokens.ActiveTabPillBg, CircleShape)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = null,
                            tint = TripHubTokens.ActiveTabPillText,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Settle Up",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = TripHubTokens.ActiveTabPillText
                        )
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 5. TASK 3.2 COMPONENTS: REAL-DATA BOOKING & ARTIFACT CARDS (ZERO FAKE DATA)
// ==============================================================================

@Composable
private fun TripHubOverviewFeed(
    filteredClassifiedExpenses: List<Pair<ExpenseEntity, TripHubBookingCategory>>,
    firstTrainExpenseId: String?,
    groupMembers: List<GroupMemberEntity>,
    allSplits: List<ExpenseSplitEntity>,
    activePerspectiveMember: GroupMemberEntity?,
    onOpenTrainPnrReviewClick: (String) -> Unit,
    onOpenFlightReviewClick: (String) -> Unit,
    onLogQuickExpenseClick: () -> Unit,
    onInspectBerthChart: (ExpenseEntity, LivePnrStatusSnapshot?) -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    if (filteredClassifiedExpenses.isEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "empty_trip_hub_state") {
                EmptyTripHubStateCard(
                    onLogTrainPnrClick = { onOpenTrainPnrReviewClick("") },
                    onUploadFlightPdfClick = { onOpenFlightReviewClick("") },
                    onLogSharedExpenseClick = onLogQuickExpenseClick,
                    modifier = Modifier.animateItemPlacement(spring(dampingRatio = 0.78f, stiffness = 380f))
                )
            }
        }
        return
    }

    val trainLegNumberById = remember(filteredClassifiedExpenses) {
        filteredClassifiedExpenses
            .filter { it.second == TripHubBookingCategory.TRAIN }
            .mapIndexed { idx, (exp, _) -> exp.expenseId to (idx + 1) }
            .toMap()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = filteredClassifiedExpenses,
            key = { it.first.expenseId }
        ) { (expense, category) ->
            val itemModifier = Modifier
                .fillMaxWidth()
                .animateItemPlacement(spring(dampingRatio = 0.78f, stiffness = 380f))

            when (category) {
                TripHubBookingCategory.TRAIN -> {
                    val legNumber = trainLegNumberById[expense.expenseId] ?: 1
                    val isPrimaryLeg = expense.expenseId == firstTrainExpenseId || legNumber == 1
                    if (isPrimaryLeg) {
                        DeepGreenTrainTicketCard(
                            expense = expense,
                            legNumber = 1,
                            groupMembers = groupMembers,
                            allSplits = allSplits,
                            activePerspectiveMember = activePerspectiveMember,
                            onOpenTrainPnrReviewClick = onOpenTrainPnrReviewClick,
                            onInspectBerthChart = onInspectBerthChart,
                            modifier = itemModifier
                        )
                    } else {
                        ReturnTransitTrainCard(
                            expense = expense,
                            legNumber = legNumber,
                            groupMembers = groupMembers,
                            allSplits = allSplits,
                            activePerspectiveMember = activePerspectiveMember,
                            onOpenTrainPnrReviewClick = onOpenTrainPnrReviewClick,
                            onInspectBerthChart = onInspectBerthChart,
                            modifier = itemModifier
                        )
                    }
                }

                TripHubBookingCategory.FLIGHT -> {
                    PeriwinkleFlightBookingCard(
                        expense = expense,
                        groupMembers = groupMembers,
                        allSplits = allSplits,
                        activePerspectiveMember = activePerspectiveMember,
                        onOpenFlightReviewClick = onOpenFlightReviewClick,
                        modifier = itemModifier
                    )
                }

                TripHubBookingCategory.STAY -> {
                    LodgingBookingCard(
                        expense = expense,
                        groupMembers = groupMembers,
                        allSplits = allSplits,
                        onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                        modifier = itemModifier
                    )
                }

                TripHubBookingCategory.RENTAL -> {
                    GroundMobilityBookingCard(
                        expense = expense,
                        isTwoWheelerRental = true,
                        groupMembers = groupMembers,
                        allSplits = allSplits,
                        onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                        modifier = itemModifier
                    )
                }

                TripHubBookingCategory.CAB -> {
                    GroundMobilityBookingCard(
                        expense = expense,
                        isTwoWheelerRental = false,
                        groupMembers = groupMembers,
                        allSplits = allSplits,
                        onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                        modifier = itemModifier
                    )
                }

                TripHubBookingCategory.GENERAL,
                TripHubBookingCategory.ALL -> {
                    GeneralSharedExpenseCard(
                        expense = expense,
                        groupMembers = groupMembers,
                        allSplits = allSplits,
                        onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                        modifier = itemModifier
                    )
                }
            }
        }
    }
}

private data class BerthCellDisplayModel(
    val berthCode: String,
    val statusCode: String,
    val travelerName: String,
    val isCurrentUser: Boolean
)

/**
 * Resolves passenger berth cells from real [LivePnrStatusSnapshot], [ParsedTravelTicket],
 * and [GroupMemberEntity] splits.
 */
private fun resolvePassengerBerthCells(
    snapshot: LivePnrStatusSnapshot?,
    parsedTicket: ParsedTravelTicket?,
    splittingMembers: List<GroupMemberEntity>,
    activePerspectiveMember: GroupMemberEntity?
): List<BerthCellDisplayModel> {
    val seatTokensFromTitle = parsedTicket?.coachAndSeats
        ?.split(Regex("""[,;/]"""))
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty()

    val structuredPax = snapshot?.structuredPassengers.orEmpty()
    val rawPaxStatuses = snapshot?.passengerStatuses.orEmpty()

    val passengerCount = maxOf(
        structuredPax.size,
        rawPaxStatuses.size,
        seatTokensFromTitle.size,
        splittingMembers.size.coerceAtLeast(1)
    )

    return (0 until passengerCount).map { idx ->
        val member = splittingMembers.getOrNull(idx)
        val structPax = structuredPax.getOrNull(idx)
        val rawStatus = rawPaxStatuses.getOrNull(idx).orEmpty()
        val seatToken = seatTokensFromTitle.getOrNull(idx).orEmpty()

        val rawSeatSpec = when {
            structPax != null && structPax.currentStatus.isNotBlank() -> structPax.currentStatus
            rawStatus.isNotBlank() -> rawStatus.substringAfter(":", rawStatus).trim()
            seatToken.isNotBlank() -> seatToken
            else -> parsedTicket?.bookingStatus?.ifBlank { "CNF" } ?: "CNF"
        }

        val berthLabel = rawSeatSpec
            .replace("CNF/", "", ignoreCase = true)
            .replace("CNF /", "", ignoreCase = true)
            .replace("/", " · ")
            .trim()
            .ifBlank { "Seat ${idx + 1}" }

        val statusBadge = when {
            structPax != null && structPax.statusLabel.isNotBlank() -> structPax.statusLabel
            rawSeatSpec.contains("MB", ignoreCase = true) -> "(MB)"
            rawSeatSpec.contains("UB", ignoreCase = true) -> "(UB)"
            rawSeatSpec.contains("LB", ignoreCase = true) -> "(LB)"
            rawSeatSpec.contains("SL", ignoreCase = true) -> "(SL)"
            rawSeatSpec.contains("SU", ignoreCase = true) -> "(SU)"
            rawSeatSpec.contains("RAC", ignoreCase = true) -> "(RAC)"
            rawSeatSpec.contains("WL", ignoreCase = true) -> "(WL)"
            else -> "(CNF)"
        }

        val resolvedTravelerName = member?.name
            ?: structPax?.passengerNumber?.takeIf { !it.matches(Regex("""P\d+""", RegexOption.IGNORE_CASE)) }
            ?: "Traveler ${idx + 1}"

        val isMe = (member != null && activePerspectiveMember != null && member.memberId == activePerspectiveMember.memberId) ||
            (member?.isCurrentUser == true)

        BerthCellDisplayModel(
            berthCode = berthLabel,
            statusCode = statusBadge,
            travelerName = resolvedTravelerName,
            isCurrentUser = isMe
        )
    }
}

/**
 * Subtask 3.2.1: Deep-Forest Green Train Ticket Card (`UPCOMING DEPARTURE`).
 *
 * Enforces:
 * - Sunlight-Grade Contrast (`>= 5.8:1`): `#FFFFFF` station codes/titles, `#B5DC86` (`> 7:1`) PNR/berth/time highlights,
 *   and `#D7E8B6` (`> 5.8:1`) secondary labels (never muted grey on the dark forest background).
 * - Progressive Disclosure on `>4` Berths: 2x2 Passenger Berth Grid for up to 4 passengers (with `"YOU"` badge)
 *   + `"+N more passengers"` interactive overflow pill (`>= 48.dp` touch bounds).
 */
@Composable
fun DeepGreenTrainTicketCard(
    expense: ExpenseEntity,
    legNumber: Int,
    groupMembers: List<GroupMemberEntity>,
    allSplits: List<ExpenseSplitEntity>,
    activePerspectiveMember: GroupMemberEntity?,
    onOpenTrainPnrReviewClick: (String) -> Unit,
    onInspectBerthChart: (ExpenseEntity, LivePnrStatusSnapshot?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localView = LocalView.current

    val parsedTicket = remember(expense.title) { extractTravelTicketFromTitle(expense.title) }
    val pnrDigits = remember(expense.title, parsedTicket) {
        parsedTicket?.pnr?.takeIf { it.isNotBlank() }
            ?: Regex("""\b(\d{10})\b""").find(expense.title)?.groupValues?.getOrNull(1).orEmpty()
    }
    val snapshot = remember(context, pnrDigits) {
        if (pnrDigits.isNotBlank()) loadPersistedPnrSnapshot(context, pnrDigits) else null
    }

    val splitBreakdown = remember(expense, groupMembers, allSplits) {
        SplitMateViewModel.resolveExpenseSplitBreakdown(
            expense = expense,
            groupMembers = groupMembers,
            allSplits = allSplits,
            currencySymbol = "₹"
        )
    }
    val splittingMembers = remember(groupMembers, splitBreakdown) {
        val includedIds = splitBreakdown.rows.filter { it.isIncludedInSplit }.map { it.memberId }.toSet()
        groupMembers.filter { includedIds.contains(it.memberId) }.ifEmpty { groupMembers }
    }
    val payer = remember(groupMembers, expense.payerId) {
        groupMembers.find { it.memberId == expense.payerId }
    }

    val berthCells = remember(snapshot, parsedTicket, splittingMembers, activePerspectiveMember) {
        resolvePassengerBerthCells(snapshot, parsedTicket, splittingMembers, activePerspectiveMember)
    }
    var showAllOverflowBerths by remember { mutableStateOf(false) }

    val fromCode = (snapshot?.fromStation?.ifBlank { parsedTicket?.fromStation.orEmpty() }
        ?: parsedTicket?.fromStation.orEmpty()).ifBlank { "ORG" }.uppercase(Locale.US)
    val toCode = (snapshot?.toStation?.ifBlank { parsedTicket?.toStation.orEmpty() }
        ?: parsedTicket?.toStation.orEmpty()).ifBlank { "DST" }.uppercase(Locale.US)

    val fromFullName = snapshot?.fromStationName?.takeIf { it.isNotBlank() }
        ?: resolveStationDisplayName(fromCode).substringAfter("(", "").removeSuffix(")").ifBlank { fromCode }
    val toFullName = snapshot?.toStationName?.takeIf { it.isNotBlank() }
        ?: resolveStationDisplayName(toCode).substringAfter("(", "").removeSuffix(")").ifBlank { toCode }

    val trainNumberAndName = remember(snapshot, parsedTicket, expense.title) {
        when {
            snapshot != null && snapshot.trainNo.isNotBlank() ->
                "${snapshot.trainNo} ${snapshot.trainName}".trim()
            parsedTicket != null && parsedTicket.trainOrFlightNo.isNotBlank() ->
                "${parsedTicket.trainOrFlightNo} ${parsedTicket.trainOrCarrierName}".trim()
            else -> cleanDisplayExpenseTitle(expense.title)
        }
    }
    val travelClassText = remember(snapshot, parsedTicket) {
        snapshot?.travelClass?.ifBlank { null }
            ?: parsedTicket?.bookingStatus?.ifBlank { "3A Sleeper" }
            ?: "Confirmed"
    }
    val loggedDateLabel = remember(expense.createdAtEpochMs) {
        SimpleDateFormat("dd MMM", Locale.US).format(Date(expense.createdAtEpochMs))
    }
    val depTimeLabel = remember(snapshot, parsedTicket, loggedDateLabel) {
        snapshot?.departureTime?.takeIf { it.isNotBlank() }
            ?: parsedTicket?.departureInfo?.takeIf { it.isNotBlank() }
            ?: loggedDateLabel
    }
    val arrTimeLabel = remember(snapshot) {
        snapshot?.arrivalTime?.takeIf { it.isNotBlank() } ?: "Scheduled Arrival"
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Section Eyebrow Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "UPCOMING DEPARTURE",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    color = TripHubTokens.TextMuted
                )
                Surface(
                    shape = CircleShape,
                    color = TripHubTokens.PositiveSagePillBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(TripHubTokens.PositiveSageText)
                        )
                        Text(
                            text = if (snapshot?.isLiveVerified == true) "Live Sync" else "Confirmed",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = TripHubTokens.PositiveSageText
                        )
                    }
                }
            }
            Text(
                text = "$loggedDateLabel · Leg $legNumber",
                style = TextStyle(
                    fontFamily = SplitMateTnumMonospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    fontFeatureSettings = "tnum"
                ),
                color = TripHubTokens.TextMuted
            )
        }

        // Main Deep-Green Ticket Pass + Bottom Stub
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = TripHubTokens.CardSurface,
            border = BorderStroke(1.dp, TripHubTokens.CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = DesignSystemBindings.tactileSpring())
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // UPPER DEEP FOREST GREEN SECTION
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    TripHubTokens.TrainForestTop,
                                    TripHubTokens.TrainForestBottom
                                )
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Pill Row: Departure Schedule Pill + PNR Digits
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TripHubTokens.TrainNextUpPillBg,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = "DEPARTURE · ${depTimeLabel.uppercase(Locale.US)}",
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.TrainNextUpPillText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "PNR",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = TripHubTokens.TrainSecondarySage
                            )
                            Text(
                                text = pnrDigits.ifBlank { "VERIFIED" },
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.TrainAccentLime
                            )
                        }
                    }

                    // Station Codes & Center Train Track Row (Sunlight-Grade Contrast)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fromCode,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp,
                                color = Color.White
                            )
                            Text(
                                text = fromFullName,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = TripHubTokens.TrainSecondarySage,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = depTimeLabel,
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.TrainAccentLime,
                                maxLines = 1
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = trainNumberAndName,
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.TrainSecondarySage,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(TripHubTokens.TrainAccentLime)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height(1.5.dp)
                                        .background(TripHubTokens.TrainAccentLime.copy(alpha = 0.65f))
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Train,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height(1.5.dp)
                                        .background(TripHubTokens.TrainAccentLime.copy(alpha = 0.65f))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(TripHubTokens.TrainAccentLime)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = travelClassText,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = TripHubTokens.TrainSecondarySage
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = toCode,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp,
                                color = Color.White
                            )
                            Text(
                                text = toFullName,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = TripHubTokens.TrainSecondarySage,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = arrTimeLabel,
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.TrainAccentLime,
                                maxLines = 1
                            )
                        }
                    }

                    // Mandatory Refinement 5: 2x2 Passenger Berth Grid (up to 4 visible by default + progressive disclosure)
                    val visibleBerths = if (showAllOverflowBerths) berthCells else berthCells.take(4)
                    val chunkedRows = visibleBerths.chunked(2)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        chunkedRows.forEach { rowCells ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowCells.forEach { cell ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = TripHubTokens.TrainBerthCellBg,
                                        border = BorderStroke(
                                            width = if (cell.isCurrentUser) 1.5.dp else 1.dp,
                                            color = if (cell.isCurrentUser) TripHubTokens.TrainAccentLime else TripHubTokens.TrainBerthCellBorder
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "${cell.berthCode} ${cell.statusCode}",
                                                    style = TextStyle(
                                                        fontFamily = SplitMateTnumMonospace,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        fontFeatureSettings = "tnum"
                                                    ),
                                                    color = TripHubTokens.TrainAccentLime,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (cell.isCurrentUser) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = TripHubTokens.TrainAccentLime
                                                    ) {
                                                        Text(
                                                            text = "YOU",
                                                            fontFamily = FigtreeFontFamily,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 9.sp,
                                                            color = TripHubTokens.TrainPrimaryCtaText,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = cell.travelerName,
                                                fontFamily = FigtreeFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (rowCells.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        if (berthCells.size > 4) {
                            val overflowCount = berthCells.size - 4
                            Surface(
                                onClick = {
                                    performCrispTactileHaptic(context, localView, heavy = false)
                                    showAllOverflowBerths = !showAllOverflowBerths
                                },
                                shape = CircleShape,
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .defaultMinSize(minHeight = 48.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(TripHubTokens.TrainBerthCellBg, CircleShape)
                                        .border(1.dp, TripHubTokens.TrainBerthCellBorder, CircleShape)
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (showAllOverflowBerths) {
                                            "Show fewer passengers"
                                        } else {
                                            "+$overflowCount more passengers"
                                        },
                                        style = TextStyle(
                                            fontFamily = SplitMateTnumMonospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = TripHubTokens.TrainSecondarySage
                                    )
                                }
                            }
                        }
                    }
                }

                // PERFORATED TICKET NOTCH DIVIDER
                PerforatedTicketStubDivider()

                // BOTTOM TICKET STUB (Payer Split Box + 48dp Action Buttons)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TripHubTokens.CardSurface)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sunken Payer & Per-Traveler Split Summary Well
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = TripHubTokens.SunkenWell,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(TripHubTokens.TerracottaPeachBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = extractInitialsFromNameOrSeed(payer?.name ?: "P"),
                                        fontFamily = FigtreeFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = TripHubTokens.TerracottaIconTint
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Payer",
                                        fontFamily = FigtreeFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        color = TripHubTokens.TextSecondary
                                    )
                                    Text(
                                        text = payer?.name ?: "Group Payer",
                                        fontFamily = FigtreeFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = TripHubTokens.TextPrimary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatIndianRupeesFromCents(expense.totalAmountCents),
                                    style = TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = TripHubTokens.TextPrimary
                                )
                                Text(
                                    text = "${splitBreakdown.perPersonHeadlineShare} / traveler (${splitBreakdown.splittingMembersCount}-way split)",
                                    style = TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = TripHubTokens.PositiveSageText
                                )
                            }
                        }
                    }

                    // 2 Action Buttons (WCAG 2.5.5 >= 48.dp touch targets)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = {
                                performCrispTactileHaptic(context, localView, heavy = false)
                                onInspectBerthChart(expense, snapshot)
                            },
                            shape = CircleShape,
                            color = TripHubTokens.SunkenWell,
                            border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EventSeat,
                                    contentDescription = null,
                                    tint = TripHubTokens.TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Berth Chart",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TripHubTokens.TextPrimary
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                performCrispTactileHaptic(context, localView, heavy = false)
                                onOpenTrainPnrReviewClick(pnrDigits)
                            },
                            shape = CircleShape,
                            color = TripHubTokens.TrainPrimaryCtaBg,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ConfirmationNumber,
                                    contentDescription = null,
                                    tint = TripHubTokens.TrainPrimaryCtaText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "View E-Ticket",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = TripHubTokens.TrainPrimaryCtaText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Subsequent Train Leg Card (`RETURN TRANSIT` matching `Debug/screen.png`).
 * Can also be expanded inline into the full Deep-Green Train Ticket Pass.
 */
@Composable
fun ReturnTransitTrainCard(
    expense: ExpenseEntity,
    legNumber: Int,
    groupMembers: List<GroupMemberEntity>,
    allSplits: List<ExpenseSplitEntity>,
    activePerspectiveMember: GroupMemberEntity?,
    onOpenTrainPnrReviewClick: (String) -> Unit,
    onInspectBerthChart: (ExpenseEntity, LivePnrStatusSnapshot?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localView = LocalView.current
    var expandAsFullGreenCard by remember { mutableStateOf(false) }

    if (expandAsFullGreenCard) {
        DeepGreenTrainTicketCard(
            expense = expense,
            legNumber = legNumber,
            groupMembers = groupMembers,
            allSplits = allSplits,
            activePerspectiveMember = activePerspectiveMember,
            onOpenTrainPnrReviewClick = onOpenTrainPnrReviewClick,
            onInspectBerthChart = onInspectBerthChart,
            modifier = modifier
        )
        return
    }

    val parsedTicket = remember(expense.title) { extractTravelTicketFromTitle(expense.title) }
    val pnrDigits = remember(expense.title, parsedTicket) {
        parsedTicket?.pnr?.takeIf { it.isNotBlank() }
            ?: Regex("""\b(\d{10})\b""").find(expense.title)?.groupValues?.getOrNull(1).orEmpty()
    }
    val snapshot = remember(context, pnrDigits) {
        if (pnrDigits.isNotBlank()) loadPersistedPnrSnapshot(context, pnrDigits) else null
    }
    val splitBreakdown = remember(expense, groupMembers, allSplits) {
        SplitMateViewModel.resolveExpenseSplitBreakdown(expense, groupMembers, allSplits)
    }
    val payer = remember(groupMembers, expense.payerId) {
        groupMembers.find { it.memberId == expense.payerId }
    }

    val loggedDateLabel = remember(expense.createdAtEpochMs) {
        SimpleDateFormat("dd MMM", Locale.US).format(Date(expense.createdAtEpochMs))
    }
    val depLabel = snapshot?.departureTime?.takeIf { it.isNotBlank() }
        ?: parsedTicket?.departureInfo?.takeIf { it.isNotBlank() }
        ?: loggedDateLabel
    val classAndStatus = buildString {
        val cls = snapshot?.travelClass?.ifBlank { "3A Sleeper" } ?: "3A Sleeper"
        append(cls)
        val status = snapshot?.bookingStatusBadge?.ifBlank { parsedTicket?.bookingStatus.orEmpty() }
            ?: parsedTicket?.bookingStatus.orEmpty()
        if (status.isNotBlank()) append(" · ").append(status)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "RETURN TRANSIT",
                fontFamily = FigtreeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                color = TripHubTokens.TextMuted
            )
            Text(
                text = "$loggedDateLabel · Leg $legNumber",
                style = TextStyle(
                    fontFamily = SplitMateTnumMonospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    fontFeatureSettings = "tnum"
                ),
                color = TripHubTokens.TextMuted
            )
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = TripHubTokens.CardSurface,
            border = BorderStroke(1.dp, TripHubTokens.CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = DesignSystemBindings.tactileSpring())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TripHubTokens.PeriwinkleBoxBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Train,
                                contentDescription = null,
                                tint = TripHubTokens.PeriwinkleIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cleanDisplayExpenseTitle(expense.title),
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = TripHubTokens.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val routeDisplay = if (!parsedTicket?.fromStation.isNullOrBlank() && !parsedTicket?.toStation.isNullOrBlank()) {
                                "${parsedTicket?.fromStation} to ${parsedTicket?.toStation}"
                            } else {
                                "Logged Return Train Ticket"
                            }
                            Text(
                                text = routeDisplay,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = TripHubTokens.TextSecondary
                            )
                        }
                    }

                    if (pnrDigits.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TripHubTokens.SunkenWell
                        ) {
                            Text(
                                text = "PNR $pnrDigits",
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Sunken Departure & Class/Berth Row
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TripHubTokens.SunkenWell,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Departure",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = TripHubTokens.TextSecondary
                            )
                            Text(
                                text = depLabel,
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Class / Berth",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = TripHubTokens.TextSecondary
                            )
                            val isRacOrWl = classAndStatus.contains("RAC", ignoreCase = true) ||
                                classAndStatus.contains("WL", ignoreCase = true)
                            Text(
                                text = classAndStatus,
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = if (isRacOrWl) TripHubTokens.WarningRacText else TripHubTokens.PositiveSageText
                            )
                        }
                    }
                }

                // Payer + Amount Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(TripHubTokens.PeriwinkleBoxBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = extractInitialsFromNameOrSeed(payer?.name ?: "P"),
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = TripHubTokens.PeriwinkleIconTint
                            )
                        }
                        Column {
                            Text(
                                text = "Paid by ${payer?.name ?: "Member"}",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = TripHubTokens.TextPrimary
                            )
                            Text(
                                text = "Equal split · ${splitBreakdown.splittingMembersCount} members",
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.TextSecondary
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatIndianRupeesFromCents(expense.totalAmountCents),
                            style = TextStyle(
                                fontFamily = SplitMateTnumMonospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = TripHubTokens.TextPrimary
                        )
                        Text(
                            text = "${splitBreakdown.perPersonHeadlineShare} / traveler",
                            style = TextStyle(
                                fontFamily = SplitMateTnumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = TripHubTokens.PositiveSageText
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            expandAsFullGreenCard = true
                        },
                        shape = CircleShape,
                        color = TripHubTokens.SunkenWell,
                        border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EventSeat,
                                contentDescription = null,
                                tint = TripHubTokens.TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Expand Berths",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TripHubTokens.TextPrimary
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            onOpenTrainPnrReviewClick(pnrDigits)
                        },
                        shape = CircleShape,
                        color = TripHubTokens.SunkenWell,
                        border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                        modifier = Modifier
                            .weight(1.2f)
                            .minimumInteractiveComponentSize()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ConfirmationNumber,
                                contentDescription = null,
                                tint = TripHubTokens.TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open Offline Pass",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TripHubTokens.TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Subtask 3.2.2: Aviation Periwinkle Flight Booking Card (`PeriwinkleFlightBookingCard`).
 * Hydrates from `PnrNetworkRepository.loadConfirmedFlightTicketResult(context, pnr)` or `extractTravelTicketFromTitle`.
 */
@Composable
fun PeriwinkleFlightBookingCard(
    expense: ExpenseEntity,
    groupMembers: List<GroupMemberEntity>,
    allSplits: List<ExpenseSplitEntity>,
    @Suppress("UNUSED_PARAMETER") activePerspectiveMember: GroupMemberEntity?,
    onOpenFlightReviewClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localView = LocalView.current

    val parsedTicket = remember(expense.title) { extractTravelTicketFromTitle(expense.title) }
    val pnrCode = remember(expense.title, parsedTicket) {
        parsedTicket?.pnr?.takeIf { it.isNotBlank() }
            ?: PnrNetworkRepository.normalizePnrKey(expense.title)
    }
    val flightResult = remember(context, pnrCode) {
        if (pnrCode.length == 6) PnrNetworkRepository.loadConfirmedFlightTicketResult(context, pnrCode) else null
    }
    val splitBreakdown = remember(expense, groupMembers, allSplits) {
        SplitMateViewModel.resolveExpenseSplitBreakdown(expense, groupMembers, allSplits)
    }
    val payer = remember(groupMembers, expense.payerId) {
        groupMembers.find { it.memberId == expense.payerId }
    }

    val originIata = flightResult?.originIata?.ifBlank { parsedTicket?.fromStation.orEmpty() }
        ?: parsedTicket?.fromStation?.ifBlank { "ORG" } ?: "ORG"
    val destIata = flightResult?.destinationIata?.ifBlank { parsedTicket?.toStation.orEmpty() }
        ?: parsedTicket?.toStation?.ifBlank { "DST" } ?: "DST"
    val originCity = flightResult?.originCity?.ifBlank { originIata } ?: originIata
    val destCity = flightResult?.destinationCity?.ifBlank { destIata } ?: destIata
    val flightHeader = when {
        flightResult != null && flightResult.flightNumber.isNotBlank() ->
            "${flightResult.airlineName} ${flightResult.flightNumber}".trim()
        parsedTicket != null && parsedTicket.trainOrFlightNo.isNotBlank() ->
            "${parsedTicket.trainOrCarrierName} ${parsedTicket.trainOrFlightNo}".trim()
        else -> cleanDisplayExpenseTitle(expense.title)
    }
    val loggedDateLabel = remember(expense.createdAtEpochMs) {
        SimpleDateFormat("dd MMM", Locale.US).format(Date(expense.createdAtEpochMs))
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "AVIATION BOARDING PASS",
                fontFamily = FigtreeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                color = TripHubTokens.TextMuted
            )
            Text(
                text = loggedDateLabel,
                style = TextStyle(
                    fontFamily = SplitMateTnumMonospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    fontFeatureSettings = "tnum"
                ),
                color = TripHubTokens.TextMuted
            )
        }

        Surface(
            shape = RoundedCornerShape(26.dp),
            color = TripHubTokens.CardSurface,
            border = BorderStroke(1.dp, TripHubTokens.CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    TripHubTokens.FlightNavyTop,
                                    TripHubTokens.FlightNavyBottom
                                )
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = flightHeader,
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        if (pnrCode.isNotBlank()) {
                            Text(
                                text = "PNR $pnrCode",
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.FlightAccentPeriwinkle
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = originIata.uppercase(Locale.US),
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp,
                                color = Color.White
                            )
                            Text(
                                text = originCity,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = TripHubTokens.FlightSecondaryLavender
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(1.5.dp)
                                    .background(TripHubTokens.FlightAccentPeriwinkle.copy(alpha = 0.6f))
                            )
                            Icon(
                                imageVector = Icons.Rounded.FlightTakeoff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(1.5.dp)
                                    .background(TripHubTokens.FlightAccentPeriwinkle.copy(alpha = 0.6f))
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = destIata.uppercase(Locale.US),
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp,
                                color = Color.White
                            )
                            Text(
                                text = destCity,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = TripHubTokens.FlightSecondaryLavender
                            )
                        }
                    }
                }

                PerforatedTicketStubDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TripHubTokens.CardSurface)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Paid by ${payer?.name ?: "Member"}",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TripHubTokens.TextPrimary
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatIndianRupeesFromCents(expense.totalAmountCents),
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.TextPrimary
                            )
                            Text(
                                text = "${splitBreakdown.perPersonHeadlineShare} / traveler",
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.PositiveSageText
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            onOpenFlightReviewClick(pnrCode)
                        },
                        shape = CircleShape,
                        color = TripHubTokens.PeriwinkleBoxBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ConfirmationNumber,
                                contentDescription = null,
                                tint = TripHubTokens.PeriwinkleIconTint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "3D Boarding Pass",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = TripHubTokens.PeriwinkleIconTint
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Subtask 3.2.3: Lodging & Stays Booking Card (`LodgingBookingCard`).
 * Bound strictly to real logged `expense.createdAtEpochMs` date or real parsed notes in `expense.title`
 * (zero hardcoded demo timestamps).
 */
@Composable
fun LodgingBookingCard(
    expense: ExpenseEntity,
    groupMembers: List<GroupMemberEntity>,
    allSplits: List<ExpenseSplitEntity>,
    onDeleteExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localView = LocalView.current
    var showSplitDrawer by remember { mutableStateOf(false) }

    val cleanTitle = remember(expense.title) { cleanDisplayExpenseTitle(expense.title) }
    val splitBreakdown = remember(expense, groupMembers, allSplits) {
        SplitMateViewModel.resolveExpenseSplitBreakdown(expense, groupMembers, allSplits)
    }
    val payer = remember(groupMembers, expense.payerId) {
        groupMembers.find { it.memberId == expense.payerId }
    }
    val loggedDateFormatted = remember(expense.createdAtEpochMs) {
        SimpleDateFormat("dd MMM yyyy · hh:mm a", Locale.US).format(Date(expense.createdAtEpochMs))
    }
    val nightsBadge = remember(expense.title) {
        Regex("""\b(\d+\s*Nights?)\b""", RegexOption.IGNORE_CASE).find(expense.title)?.groupValues?.getOrNull(1)
            ?: "Stay Logged"
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "LODGING & STAYS",
                fontFamily = FigtreeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                color = TripHubTokens.TextMuted
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircleOutline,
                    contentDescription = null,
                    tint = TripHubTokens.PositiveSageText,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Confirmed Booking",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = TripHubTokens.PositiveSageText
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = TripHubTokens.CardSurface,
            border = BorderStroke(1.dp, TripHubTokens.CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = DesignSystemBindings.tactileSpring())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(TripHubTokens.SunkenWell),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Apartment,
                                contentDescription = null,
                                tint = TripHubTokens.TerracottaIconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cleanTitle,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = TripHubTokens.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Logged on $loggedDateFormatted",
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.TextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = TripHubTokens.SunkenWell
                    ) {
                        Text(
                            text = nightsBadge,
                            style = TextStyle(
                                fontFamily = SplitMateTnumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = TripHubTokens.TextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Sunken Date & Split Summary Box (100% Real Logged Data)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TripHubTokens.SunkenWell,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Logged Date",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = TripHubTokens.TextSecondary
                            )
                            Text(
                                text = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(expense.createdAtEpochMs)),
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Group Split Mode",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = TripHubTokens.TextSecondary
                            )
                            Text(
                                text = "${splitBreakdown.splittingMembersCount} Travelers (0.00¢ Drift)",
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.TextPrimary
                            )
                        }
                    }
                }

                // Payer & Total Spend Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(TripHubTokens.PositiveSagePillBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = extractInitialsFromNameOrSeed(payer?.name ?: "P"),
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = TripHubTokens.PositiveSageText
                            )
                        }
                        Column {
                            Text(
                                text = "Paid by ${payer?.name ?: "Member"}",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = TripHubTokens.TextPrimary
                            )
                            Text(
                                text = "All ${splitBreakdown.splittingMembersCount} shared equally",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = TripHubTokens.TextSecondary
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatIndianRupeesFromCents(expense.totalAmountCents),
                            style = TextStyle(
                                fontFamily = SplitMateTnumMonospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = TripHubTokens.TextPrimary
                        )
                        Text(
                            text = "${splitBreakdown.perPersonHeadlineShare} / person",
                            style = TextStyle(
                                fontFamily = SplitMateTnumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = TripHubTokens.PositiveSageText
                        )
                    }
                }

                // Action Chips Row (`Maps` + `Split Details` enforcing >= 48.dp touch bounds)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            try {
                                val mapIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("geo:0,0?q=${Uri.encode(cleanTitle)}")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(mapIntent)
                            } catch (_: Exception) {
                            }
                        },
                        shape = CircleShape,
                        color = TripHubTokens.SunkenWell,
                        border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Map,
                                contentDescription = null,
                                tint = TripHubTokens.TextPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Maps",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TripHubTokens.TextPrimary
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            showSplitDrawer = !showSplitDrawer
                        },
                        shape = CircleShape,
                        color = TripHubTokens.SunkenWell,
                        border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                        modifier = Modifier
                            .weight(1.2f)
                            .minimumInteractiveComponentSize()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = TripHubTokens.TextPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showSplitDrawer) "Hide Split" else "Split Details",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TripHubTokens.TextPrimary
                            )
                        }
                    }
                }

                if (showSplitDrawer) {
                    ExpandableSplitBreakdownDrawer(
                        splitBreakdown = splitBreakdown,
                        onDeleteExpense = onDeleteExpense
                    )
                }
            }
        }
    }
}

/**
 * Subtask 3.2.4: Ground Mobility & Rentals Booking Card (`GroundMobilityBookingCard`).
 * Supports TwoWheeler Peach variant (`isTwoWheelerRental = true`) and DirectionsCar Periwinkle variant (`false`).
 */
@Composable
fun GroundMobilityBookingCard(
    expense: ExpenseEntity,
    isTwoWheelerRental: Boolean,
    groupMembers: List<GroupMemberEntity>,
    allSplits: List<ExpenseSplitEntity>,
    onDeleteExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localView = LocalView.current
    var showSplitDrawer by remember { mutableStateOf(false) }

    val cleanTitle = remember(expense.title) { cleanDisplayExpenseTitle(expense.title) }
    val splitBreakdown = remember(expense, groupMembers, allSplits) {
        SplitMateViewModel.resolveExpenseSplitBreakdown(expense, groupMembers, allSplits)
    }
    val payer = remember(groupMembers, expense.payerId) {
        groupMembers.find { it.memberId == expense.payerId }
    }
    val loggedDateStr = remember(expense.createdAtEpochMs) {
        SimpleDateFormat("dd MMM · hh:mm a", Locale.US).format(Date(expense.createdAtEpochMs))
    }

    val boxBg = if (isTwoWheelerRental) TripHubTokens.TerracottaPeachBg else TripHubTokens.PeriwinkleBoxBg
    val iconTint = if (isTwoWheelerRental) TripHubTokens.TerracottaIconTint else TripHubTokens.PeriwinkleIconTint
    val leadIcon = if (isTwoWheelerRental) Icons.Rounded.TwoWheeler else Icons.Rounded.DirectionsCar
    val unitSuffix = if (isTwoWheelerRental) "rider" else "person"

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isTwoWheelerRental) "GROUND MOBILITY & RENTALS" else "STATION TRANSFER & CABS",
                fontFamily = FigtreeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                color = TripHubTokens.TextMuted
            )
            Text(
                text = loggedDateStr,
                style = TextStyle(
                    fontFamily = SplitMateTnumMonospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    fontFeatureSettings = "tnum"
                ),
                color = TripHubTokens.TextMuted
            )
        }

        Surface(
            onClick = {
                performCrispTactileHaptic(context, localView, heavy = false)
                showSplitDrawer = !showSplitDrawer
            },
            shape = RoundedCornerShape(22.dp),
            color = TripHubTokens.CardSurface,
            border = BorderStroke(1.dp, TripHubTokens.CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = DesignSystemBindings.tactileSpring())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(boxBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = leadIcon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cleanTitle,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = TripHubTokens.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Logged $loggedDateStr · Tap for split details",
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = TripHubTokens.PositiveSagePillBg
                    ) {
                        Text(
                            text = if (isTwoWheelerRental) "Rental" else "Assigned",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = TripHubTokens.PositiveSageText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = TripHubTokens.CardBorder, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(boxBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = extractInitialsFromNameOrSeed(payer?.name ?: "P"),
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = iconTint
                            )
                        }
                        Text(
                            text = "Paid by ${payer?.name ?: "Member"}",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = TripHubTokens.TextSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatIndianRupeesFromCents(expense.totalAmountCents),
                            style = TextStyle(
                                fontFamily = SplitMateTnumMonospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = TripHubTokens.TextPrimary
                        )
                        Text(
                            text = "${splitBreakdown.perPersonHeadlineShare} / $unitSuffix",
                            style = TextStyle(
                                fontFamily = SplitMateTnumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = TripHubTokens.PositiveSageText
                        )
                    }
                }

                if (showSplitDrawer) {
                    ExpandableSplitBreakdownDrawer(
                        splitBreakdown = splitBreakdown,
                        onDeleteExpense = onDeleteExpense
                    )
                }
            }
        }
    }
}

/**
 * General Shared Expense Card (`GeneralSharedExpenseCard`) with tap-to-expand spring drawer.
 */
@Composable
fun GeneralSharedExpenseCard(
    expense: ExpenseEntity,
    groupMembers: List<GroupMemberEntity>,
    allSplits: List<ExpenseSplitEntity>,
    onDeleteExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localView = LocalView.current
    var expanded by remember { mutableStateOf(false) }

    val cleanTitle = remember(expense.title) { cleanDisplayExpenseTitle(expense.title) }
    val splitBreakdown = remember(expense, groupMembers, allSplits) {
        SplitMateViewModel.resolveExpenseSplitBreakdown(expense, groupMembers, allSplits)
    }
    val payer = remember(groupMembers, expense.payerId) {
        groupMembers.find { it.memberId == expense.payerId }
    }
    val dateLabel = remember(expense.createdAtEpochMs) {
        SimpleDateFormat("dd MMM · hh:mm a", Locale.US).format(Date(expense.createdAtEpochMs))
    }

    Surface(
        onClick = {
            performCrispTactileHaptic(context, localView, heavy = false)
            expanded = !expanded
        },
        shape = RoundedCornerShape(22.dp),
        color = TripHubTokens.CardSurface,
        border = BorderStroke(1.dp, TripHubTokens.CardBorder),
        modifier = modifier.animateContentSize(animationSpec = DesignSystemBindings.tactileSpring())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TripHubTokens.SunkenWell),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = TripHubTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cleanTitle,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = TripHubTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Paid by ${payer?.name ?: "Member"} · $dateLabel",
                            style = TextStyle(
                                fontFamily = SplitMateTnumMonospace,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = TripHubTokens.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatIndianRupeesFromCents(expense.totalAmountCents),
                        style = TextStyle(
                            fontFamily = SplitMateTnumMonospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = TripHubTokens.TextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${splitBreakdown.perPersonHeadlineShare}/pax",
                            style = TextStyle(
                                fontFamily = SplitMateTnumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = TripHubTokens.PositiveSageText
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = TripHubTokens.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (expanded) {
                ExpandableSplitBreakdownDrawer(
                    splitBreakdown = splitBreakdown,
                    onDeleteExpense = onDeleteExpense
                )
            }
        }
    }
}

@Composable
private fun ExpandableSplitBreakdownDrawer(
    splitBreakdown: SplitMateViewModel.ExpenseSplitBreakdownSummary,
    onDeleteExpense: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = TripHubTokens.SunkenWell,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = DesignSystemBindings.tactileSpring())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = splitBreakdown.headerLabel,
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = TripHubTokens.TextPrimary
                )
                TextButton(
                    onClick = onDeleteExpense,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete Expense",
                        tint = TripHubTokens.TerracottaIconTint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Delete",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TripHubTokens.TerracottaIconTint
                    )
                }
            }

            splitBreakdown.rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = row.displayName,
                        fontFamily = FigtreeFontFamily,
                        fontWeight = if (row.isCurrentUser) FontWeight.ExtraBold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = if (row.isIncludedInSplit) TripHubTokens.TextPrimary else TripHubTokens.TextMuted
                    )
                    Text(
                        text = row.formattedShare,
                        style = TextStyle(
                            fontFamily = SplitMateTnumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = if (row.isIncludedInSplit) TripHubTokens.PositiveSageText else TripHubTokens.TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTripHubStateCard(
    onLogTrainPnrClick: () -> Unit,
    onUploadFlightPdfClick: () -> Unit,
    onLogSharedExpenseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localView = LocalView.current

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = TripHubTokens.CardSurface,
        border = BorderStroke(1.dp, TripHubTokens.CardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(TripHubTokens.PositiveSagePillBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Train,
                    contentDescription = null,
                    tint = TripHubTokens.PositiveSageText,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "Your Trip Hub is Ready",
                fontFamily = FigtreeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                color = TripHubTokens.TextPrimary
            )

            Text(
                text = "Log an IRCTC 10-digit Train PNR, upload a Flight Boarding Pass PDF, or add a Hotel, Rental, Cab, or Shared Expense. Cards and category filters materialize automatically from your real Room ledger.",
                fontFamily = FigtreeFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = TripHubTokens.TextSecondary
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        performCrispTactileHaptic(context, localView, heavy = false)
                        onLogTrainPnrClick()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BuckwheatOlivePrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Train,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ Log Train PNR",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        performCrispTactileHaptic(context, localView, heavy = false)
                        onUploadFlightPdfClick()
                    },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FlightTakeoff,
                        contentDescription = null,
                        tint = TripHubTokens.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ Upload Flight PDF",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TripHubTokens.TextPrimary
                    )
                }

                OutlinedButton(
                    onClick = {
                        performCrispTactileHaptic(context, localView, heavy = false)
                        onLogSharedExpenseClick()
                    },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                        contentDescription = null,
                        tint = TripHubTokens.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ Log Shared Expense",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TripHubTokens.TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun PerforatedTicketStubDivider() {
    val notchBg = TripHubTokens.CanvasBg
    val dashColor = TripHubTokens.CardBorder

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .background(TripHubTokens.CardSurface),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val midY = size.height / 2f
            drawLine(
                color = dashColor,
                start = Offset(20.dp.toPx(), midY),
                end = Offset(size.width - 20.dp.toPx(), midY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            )
            drawCircle(
                color = notchBg,
                radius = 10.dp.toPx(),
                center = Offset(0f, midY)
            )
            drawCircle(
                color = notchBg,
                radius = 10.dp.toPx(),
                center = Offset(size.width, midY)
            )
        }
    }
}

// ==============================================================================
// 6. TASK 3.3 SUB-VIEWS: `Plan`, `Travel`, `Money`, `People` & `+ Add Booking`
// ==============================================================================

/**
 * Subtask 3.3.1: `Plan` Tab — Chronological Day-by-Day Timeline grouping real `groupExpenses`
 * by formatted calendar date (`SimpleDateFormat("dd MMM yyyy", Locale.US)`).
 */
@Composable
private fun TripHubPlanTimelineView(
    classifiedExpenses: List<Pair<ExpenseEntity, TripHubBookingCategory>>,
    groupMembers: List<GroupMemberEntity>,
    onOpenTrainPnrReviewClick: (String) -> Unit,
    onOpenFlightReviewClick: (String) -> Unit,
    onLogQuickExpenseClick: () -> Unit
) {
    if (classifiedExpenses.isEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item(key = "empty_plan") {
                EmptyTripHubStateCard(
                    onLogTrainPnrClick = { onOpenTrainPnrReviewClick("") },
                    onUploadFlightPdfClick = { onOpenFlightReviewClick("") },
                    onLogSharedExpenseClick = onLogQuickExpenseClick,
                    modifier = Modifier.animateItemPlacement(spring(dampingRatio = 0.78f, stiffness = 380f))
                )
            }
        }
        return
    }

    val dayFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.US) }
    val groupedByDay = remember(classifiedExpenses) {
        classifiedExpenses.groupBy { (exp, _) -> dayFormat.format(Date(exp.createdAtEpochMs)) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        groupedByDay.entries.forEachIndexed { dayIndex, (dateHeader, dayItems) ->
            item(key = "day_header_$dateHeader") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (dayIndex == 0) 2.dp else 8.dp)
                        .animateItemPlacement(spring(dampingRatio = 0.78f, stiffness = 380f)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TripHubTokens.ActiveTabPillBg
                        ) {
                            Text(
                                text = "DAY ${dayIndex + 1}",
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.ActiveTabPillText,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = dateHeader,
                            style = TextStyle(
                                fontFamily = SplitMateTnumMonospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = TripHubTokens.TextPrimary
                        )
                    }
                    Text(
                        text = "${dayItems.size} ${if (dayItems.size == 1) "item" else "items"}",
                        style = TextStyle(
                            fontFamily = SplitMateTnumMonospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = TripHubTokens.TextMuted
                    )
                }
            }

            items(
                items = dayItems,
                key = { "plan_${it.first.expenseId}" }
            ) { (expense, category) ->
                val payer = groupMembers.find { it.memberId == expense.payerId }
                val timeLabel = timeFormat.format(Date(expense.createdAtEpochMs))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = TripHubTokens.CardSurface,
                    border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItemPlacement(spring(dampingRatio = 0.78f, stiffness = 380f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(TripHubTokens.SunkenWell),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = TripHubTokens.TextPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = TripHubTokens.TextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "$timeLabel · ${category.filterTitle}",
                                        style = TextStyle(
                                            fontFamily = SplitMateTnumMonospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = TripHubTokens.TextMuted
                                    )
                                }
                                Text(
                                    text = cleanDisplayExpenseTitle(expense.title),
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = TripHubTokens.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Paid by ${payer?.name ?: "Member"}",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = TripHubTokens.TextSecondary
                                )
                            }
                        }

                        Text(
                            text = formatIndianRupeesFromCents(expense.totalAmountCents),
                            style = TextStyle(
                                fontFamily = SplitMateTnumMonospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = TripHubTokens.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Subtask 3.3.2: `Travel` Tab — Interactive Classic 3D Flip Travel Pass (`Train <-> Flight`) launcher
 * at the top, followed by all real `TRAIN`, `FLIGHT`, `STAY`, `RENTAL`, and `CAB` cards in `groupExpenses`.
 */
@Composable
private fun TripHubTravelWalletView(
    classifiedExpenses: List<Pair<ExpenseEntity, TripHubBookingCategory>>,
    firstTrainExpenseId: String?,
    groupMembers: List<GroupMemberEntity>,
    allSplits: List<ExpenseSplitEntity>,
    activePerspectiveMember: GroupMemberEntity?,
    onOpenTrainPnrReviewClick: (String) -> Unit,
    onOpenFlightReviewClick: (String) -> Unit,
    onSwitchToClassicLedgerClick: () -> Unit,
    onInspectBerthChart: (ExpenseEntity, LivePnrStatusSnapshot?) -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    val context = LocalContext.current
    val localView = LocalView.current

    val travelOnlyItems = remember(classifiedExpenses) {
        classifiedExpenses.filter { (_, cat) ->
            cat == TripHubBookingCategory.TRAIN ||
                cat == TripHubBookingCategory.FLIGHT ||
                cat == TripHubBookingCategory.STAY ||
                cat == TripHubBookingCategory.RENTAL ||
                cat == TripHubBookingCategory.CAB
        }
    }
    val trainCount = remember(classifiedExpenses) {
        classifiedExpenses.count { it.second == TripHubBookingCategory.TRAIN }
    }
    val flightCount = remember(classifiedExpenses) {
        classifiedExpenses.count { it.second == TripHubBookingCategory.FLIGHT }
    }

    val trainLegNumberById = remember(travelOnlyItems) {
        travelOnlyItems
            .filter { it.second == TripHubBookingCategory.TRAIN }
            .mapIndexed { idx, (exp, _) -> exp.expenseId to (idx + 1) }
            .toMap()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Interactive 3D Flip Travel Pass Deck Launcher
        item(key = "classic_3d_transit_deck_launcher") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItemPlacement(spring(dampingRatio = 0.78f, stiffness = 380f)),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "INTERACTIVE 3D TRANSIT DECK",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        color = TripHubTokens.TextMuted
                    )
                    Surface(
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            onSwitchToClassicLedgerClick()
                        },
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Classic Ledger View",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TripHubTokens.PositiveSageText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                AnimatedTransitDeckHeroCard(
                    initialPassMode = if (flightCount > trainCount) ActiveTravelPassMode.FLIGHT else ActiveTravelPassMode.TRAIN,
                    trainCountLogged = trainCount,
                    flightCountActive = flightCount,
                    onEnterTrainPnrClick = { onOpenTrainPnrReviewClick("") },
                    onUploadFlightPdfClick = { onOpenFlightReviewClick("") }
                )
            }
        }

        items(
            items = travelOnlyItems,
            key = { "travel_${it.first.expenseId}" }
        ) { (expense, category) ->
            val itemModifier = Modifier
                .fillMaxWidth()
                .animateItemPlacement(spring(dampingRatio = 0.78f, stiffness = 380f))

            when (category) {
                TripHubBookingCategory.TRAIN -> {
                    val legNumber = trainLegNumberById[expense.expenseId] ?: 1
                    val isPrimaryLeg = expense.expenseId == firstTrainExpenseId || legNumber == 1
                    if (isPrimaryLeg) {
                        DeepGreenTrainTicketCard(
                            expense = expense,
                            legNumber = 1,
                            groupMembers = groupMembers,
                            allSplits = allSplits,
                            activePerspectiveMember = activePerspectiveMember,
                            onOpenTrainPnrReviewClick = onOpenTrainPnrReviewClick,
                            onInspectBerthChart = onInspectBerthChart,
                            modifier = itemModifier
                        )
                    } else {
                        ReturnTransitTrainCard(
                            expense = expense,
                            legNumber = legNumber,
                            groupMembers = groupMembers,
                            allSplits = allSplits,
                            activePerspectiveMember = activePerspectiveMember,
                            onOpenTrainPnrReviewClick = onOpenTrainPnrReviewClick,
                            onInspectBerthChart = onInspectBerthChart,
                            modifier = itemModifier
                        )
                    }
                }

                TripHubBookingCategory.FLIGHT -> {
                    PeriwinkleFlightBookingCard(
                        expense = expense,
                        groupMembers = groupMembers,
                        allSplits = allSplits,
                        activePerspectiveMember = activePerspectiveMember,
                        onOpenFlightReviewClick = onOpenFlightReviewClick,
                        modifier = itemModifier
                    )
                }

                TripHubBookingCategory.STAY -> {
                    LodgingBookingCard(
                        expense = expense,
                        groupMembers = groupMembers,
                        allSplits = allSplits,
                        onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                        modifier = itemModifier
                    )
                }

                TripHubBookingCategory.RENTAL -> {
                    GroundMobilityBookingCard(
                        expense = expense,
                        isTwoWheelerRental = true,
                        groupMembers = groupMembers,
                        allSplits = allSplits,
                        onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                        modifier = itemModifier
                    )
                }

                TripHubBookingCategory.CAB -> {
                    GroundMobilityBookingCard(
                        expense = expense,
                        isTwoWheelerRental = false,
                        groupMembers = groupMembers,
                        allSplits = allSplits,
                        onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                        modifier = itemModifier
                    )
                }

                else -> {}
            }
        }
    }
}

/**
 * Subtask 3.3.3: `Money` Tab — Greedy Minimum Cash Flow Settlements (`SplitMateMathEngine.simplifyDebtsGreedy`)
 * + Compact Max-Heap Graph Inspector (`Icons.Rounded.Info`, `48.dp` touch bounds) + `Pay via UPI` & `Mark Paid`.
 */
@Composable
private fun TripHubMoneySettlementView(
    viewModel: SplitMateViewModel,
    groupId: String,
    @Suppress("UNUSED_PARAMETER") groupName: String,
    groupMembers: List<GroupMemberEntity>,
    netBalancesMap: Map<String, Long>,
    onOpenUpiPaymentSheet: (SettlementTransferUiModel) -> Unit,
    @Suppress("UNUSED_PARAMETER") onOpenSettleUpClick: () -> Unit
) {
    val context = LocalContext.current
    val localView = LocalView.current
    var showGraphInspector by remember { mutableStateOf(false) }

    val memberNetBalances = remember(groupMembers, netBalancesMap) {
        groupMembers.map { m ->
            SplitMateMathEngine.MemberNetBalance(
                memberId = m.memberId,
                displayName = m.name,
                netCents = netBalancesMap[m.memberId] ?: 0L
            )
        }
    }
    val simplifiedTransfers = remember(memberNetBalances) {
        SplitMateMathEngine.simplifyDebtsGreedy(memberNetBalances)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + Compact Max-Heap Graph Inspector Toggle (`Icons.Rounded.Info`, >= 48.dp touch bounds)
        item(key = "greedy_settlement_header") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = TripHubTokens.CardSurface,
                border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItemPlacement(spring(dampingRatio = 0.78f, stiffness = 380f))
                    .animateContentSize(animationSpec = DesignSystemBindings.tactileSpring())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(TripHubTokens.PositiveSagePillBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = TripHubTokens.PositiveSageText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Greedy Debt Simplification",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = TripHubTokens.TextPrimary
                                )
                                Text(
                                    text = "${simplifiedTransfers.size} optimal ${if (simplifiedTransfers.size == 1) "transfer" else "transfers"} · 0.00¢ drift",
                                    style = TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = TripHubTokens.TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                performCrispTactileHaptic(context, localView, heavy = false)
                                showGraphInspector = !showGraphInspector
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "Max-Heap Graph Inspector",
                                tint = TripHubTokens.PositiveSageText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    if (showGraphInspector) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = TripHubTokens.SunkenWell,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Max-Priority Queue Simplification (O(V log V))",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = TripHubTokens.TextPrimary
                                )
                                Text(
                                    text = "Partitions travelers into Creditor Max-Heap (V+) and Debtor Max-Heap (V-), matching largest creditor with largest debtor in each step to eliminate cyclic debts in at most N - 1 transfers.",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = TripHubTokens.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (simplifiedTransfers.isEmpty()) {
            item(key = "all_settled_card") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = TripHubTokens.CardSurface,
                    border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItemPlacement(spring(dampingRatio = 0.78f, stiffness = 380f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = TripHubTokens.PositiveSageText,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "All Group Debts Settled (₹0.00)",
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.TextPrimary
                            )
                            Text(
                                text = "Every traveler's net balance is reconciled to zero paise drift.",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = TripHubTokens.TextSecondary
                            )
                        }
                    }
                }
            }
        } else {
            items(
                items = simplifiedTransfers,
                key = { "${it.fromMemberId}_${it.toMemberId}_${it.amountCents}" }
            ) { settlement ->
                val fromMember = groupMembers.find { it.memberId == settlement.fromMemberId }
                val toMember = groupMembers.find { it.memberId == settlement.toMemberId }
                val rawFields = toMember?.upiId?.split("|")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                val rawPhone = rawFields.firstOrNull { !it.contains("@") } ?: ""
                val cleanDigits = cleanIndianTenDigitPhone(rawPhone)
                val clean10Phone = if (cleanDigits.length == 10) cleanDigits else {
                    rawFields.map { cleanIndianTenDigitPhone(it.substringBefore("@")) }
                        .firstOrNull { it.length == 10 }.orEmpty()
                }
                val customVpa = rawFields.firstOrNull { it.contains("@") && !it.endsWith("@upi", ignoreCase = true) }
                    ?: rawFields.firstOrNull { it.contains("@") }
                    ?: if (clean10Phone.length == 10) "${clean10Phone}@upi" else ""
                val formattedAmount = formatIndianRupeesFromCents(settlement.amountCents)

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = TripHubTokens.CardSurface,
                    border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItemPlacement(spring(dampingRatio = 0.78f, stiffness = 380f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = settlement.fromName,
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = TripHubTokens.TextPrimary
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "pays",
                                    tint = TripHubTokens.TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = settlement.toName,
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = TripHubTokens.PositiveSageText
                                )
                            }

                            Text(
                                text = formattedAmount,
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.TextPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    performCrispTactileHaptic(context, localView, heavy = false)
                                    val transferUiModel = SettlementTransferUiModel(
                                        transfer = settlement,
                                        fromMemberId = settlement.fromMemberId,
                                        fromName = settlement.fromName,
                                        fromSeed = fromMember?.avatarSeed ?: settlement.fromName,
                                        toMemberId = settlement.toMemberId,
                                        toName = settlement.toName,
                                        toSeed = toMember?.avatarSeed ?: settlement.toName,
                                        upiId = customVpa,
                                        cleanPhone = clean10Phone,
                                        hasLinkedPhone = clean10Phone.length == 10,
                                        amount = String.format(Locale.US, "%.2f", settlement.amountCents / 100.0),
                                        formattedDisplayAmount = formattedAmount,
                                        isCurrentUserDebtor = fromMember?.isCurrentUser == true
                                    )
                                    onOpenUpiPaymentSheet(transferUiModel)
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BuckwheatOlivePrimary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text(
                                    text = "Pay via UPI",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    performCrispTactileHaptic(context, localView, heavy = false)
                                    viewModel.recordSettlement(
                                        groupId = groupId,
                                        fromMemberId = settlement.fromMemberId,
                                        toMemberId = settlement.toMemberId,
                                        amountCents = settlement.amountCents
                                    )
                                },
                                shape = CircleShape,
                                border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = TripHubTokens.TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mark Paid",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TripHubTokens.TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Subtask 3.3.4: `People` Tab — Lists all real `GroupMemberEntity` travelers with avatar initials,
 * phone/UPI VPA, live `tnum` net balance pill, and 1-tap `"View as <Name>"` perspective switching.
 */
@Composable
private fun TripHubPeoplePerspectiveView(
    viewModel: SplitMateViewModel,
    groupId: String,
    groupMembers: List<GroupMemberEntity>,
    netBalancesMap: Map<String, Long>,
    onAddMemberClick: () -> Unit,
    onOpenSyncSheetClick: () -> Unit,
    isTravelGroup: Boolean = true
) {
    val context = LocalContext.current
    val localView = LocalView.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "people_actions_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItemPlacement(spring(dampingRatio = 0.78f, stiffness = 380f)),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        performCrispTactileHaptic(context, localView, heavy = false)
                        onAddMemberClick()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TripHubTokens.ActiveTabPillBg,
                        contentColor = TripHubTokens.ActiveTabPillText
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTravelGroup) "+ Add Traveler" else "+ Add Member",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        performCrispTactileHaptic(context, localView, heavy = false)
                        onOpenSyncSheetClick()
                    },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                    modifier = Modifier
                        .weight(1.15f)
                        .minimumInteractiveComponentSize()
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        tint = TripHubTokens.TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share Trip Sync Capsule",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TripHubTokens.TextPrimary,
                        maxLines = 1
                    )
                }
            }
        }

        items(
            items = groupMembers,
            key = { it.memberId }
        ) { member ->
            val isMe = member.isCurrentUser
            val netCents = netBalancesMap[member.memberId] ?: 0L
            val absFormatted = formatIndianRupeesFromCents(abs(netCents))
            val netLabel = when {
                netCents > 0L -> "+$absFormatted"
                netCents < 0L -> "-$absFormatted"
                else -> "₹0.00"
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = TripHubTokens.CardSurface,
                border = BorderStroke(
                    width = if (isMe) 1.5.dp else 1.dp,
                    color = if (isMe) BuckwheatOlivePrimary else TripHubTokens.CardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItemPlacement(spring(dampingRatio = 0.78f, stiffness = 380f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isMe) TripHubTokens.PositiveSagePillBg else TripHubTokens.SunkenWell
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = extractInitialsFromNameOrSeed(member.name),
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (isMe) TripHubTokens.PositiveSageText else TripHubTokens.TextPrimary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.name,
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = TripHubTokens.TextPrimary
                                )
                                Text(
                                    text = member.upiId.ifBlank { "UPI / Phone not linked yet" },
                                    style = TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = TripHubTokens.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                netCents > 0L -> TripHubTokens.PositiveSagePillBg
                                netCents < 0L -> TripHubTokens.TerracottaPeachBg
                                else -> TripHubTokens.SunkenWell
                            }
                        ) {
                            Text(
                                text = netLabel,
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = when {
                                    netCents > 0L -> TripHubTokens.PositiveSageText
                                    netCents < 0L -> TripHubTokens.TerracottaIconTint
                                    else -> TripHubTokens.TextSecondary
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            viewModel.claimGroupMemberPerspective(groupId, member.memberId)
                        },
                        shape = CircleShape,
                        color = if (isMe) TripHubTokens.PositiveSagePillBg else TripHubTokens.SunkenWell,
                        border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isMe) Icons.Rounded.Verified else Icons.Rounded.PersonPin,
                                contentDescription = null,
                                tint = if (isMe) TripHubTokens.PositiveSageText else TripHubTokens.TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMe) "Active Perspective (You)" else "View as ${member.name}",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = if (isMe) TripHubTokens.PositiveSageText else TripHubTokens.TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Subtask 3.3.5: Canonical M3 `ModalBottomSheet` for `+ Add Booking` Extended FAB.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBookingQuickSheet(
    onDismiss: () -> Unit,
    onSelectTrainPnr: () -> Unit,
    onSelectFlightPass: () -> Unit,
    onSelectSharedExpense: () -> Unit,
    onSelectSyncAndPerspective: () -> Unit
) {
    val context = LocalContext.current
    val localView = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = SplitMateTheme.ScreenBg,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = SplitMateTheme.BorderLight,
                width = 36.dp,
                height = 4.dp
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Add to Trip Hub",
                fontFamily = FigtreeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                color = TripHubTokens.TextPrimary
            )
            Text(
                text = "Choose a booking type or sync with your travel group.",
                fontFamily = FigtreeFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = TripHubTokens.TextSecondary
            )

            val actions = listOf(
                Triple("IRCTC Train PNR Ticket", Icons.Rounded.Train, onSelectTrainPnr),
                Triple("Flight Boarding Pass / PDF", Icons.Rounded.FlightTakeoff, onSelectFlightPass),
                Triple("Hotel, Rental, Cab or Shared Expense", Icons.AutoMirrored.Rounded.ReceiptLong, onSelectSharedExpense),
                Triple("Sync & Switch Perspective", Icons.Rounded.Sync, onSelectSyncAndPerspective)
            )

            actions.forEach { (label, icon, callback) ->
                Surface(
                    onClick = {
                        performCrispTactileHaptic(context, localView, heavy = false)
                        callback()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = TripHubTokens.CardSurface,
                    border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .defaultMinSize(minHeight = 52.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(TripHubTokens.SunkenWell),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = TripHubTokens.TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = label,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TripHubTokens.TextPrimary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = TripHubTokens.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BerthChartInspectorDialog(
    expense: ExpenseEntity,
    snapshot: LivePnrStatusSnapshot?,
    parsedTicket: ParsedTravelTicket?,
    groupMembers: List<GroupMemberEntity>,
    allSplits: List<ExpenseSplitEntity>,
    activePerspectiveMember: GroupMemberEntity?,
    onDismiss: () -> Unit,
    onOpenFullETicket: () -> Unit
) {
    val splitBreakdown = remember(expense, groupMembers, allSplits) {
        SplitMateViewModel.resolveExpenseSplitBreakdown(
            expense = expense,
            groupMembers = groupMembers,
            allSplits = allSplits,
            currencySymbol = "₹"
        )
    }
    val splittingMembers = remember(groupMembers, splitBreakdown) {
        val includedIds = splitBreakdown.rows.filter { it.isIncludedInSplit }.map { it.memberId }.toSet()
        groupMembers.filter { includedIds.contains(it.memberId) }.ifEmpty { groupMembers }
    }
    val berthCells = remember(snapshot, parsedTicket, splittingMembers, activePerspectiveMember) {
        resolvePassengerBerthCells(snapshot, parsedTicket, splittingMembers, activePerspectiveMember)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TripHubTokens.CardSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.EventSeat,
                    contentDescription = null,
                    tint = TripHubTokens.PositiveSageText,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Coach & Berth Allocation",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = TripHubTokens.TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = cleanDisplayExpenseTitle(expense.title),
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TripHubTokens.TextSecondary
                )
                if (!snapshot?.coachPositionHint.isNullOrBlank()) {
                    Text(
                        text = snapshot?.coachPositionHint.orEmpty(),
                        style = TextStyle(
                            fontFamily = SplitMateTnumMonospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = TripHubTokens.PositiveSageText
                    )
                }
                berthCells.forEach { cell ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TripHubTokens.SunkenWell,
                        border = BorderStroke(
                            1.dp,
                            if (cell.isCurrentUser) BuckwheatOlivePrimary else TripHubTokens.CardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (cell.isCurrentUser) "${cell.travelerName} (YOU)" else cell.travelerName,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = TripHubTokens.TextPrimary
                            )
                            Text(
                                text = "${cell.berthCode} ${cell.statusCode}",
                                style = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TripHubTokens.PositiveSageText
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenFullETicket,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BuckwheatOlivePrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Text(
                    text = "Open Full E-Ticket",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Text(
                    text = "Close",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = TripHubTokens.TextSecondary
                )
            }
        }
    )
}
