package com.splitmate.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.FigtreeFontFamily
import com.splitmate.app.ui.LivePnrPassenger
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.ui.LivePnrStatusSnapshot
import com.splitmate.app.ui.ParsedTravelTicket
import com.splitmate.app.ui.SplitMateThemeMode
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.components.ConnectedButtonGroup
import com.splitmate.app.ui.components.ContainedLoadingIndicator
import com.splitmate.app.ui.components.EditorialFinancialTotalText
import com.splitmate.app.ui.components.ExpressiveTooltipIconButton
import com.splitmate.app.ui.components.ExpressiveTooltipOutlinedIconButton
import com.splitmate.app.ui.components.SplitMateMotion
import com.splitmate.app.ui.components.rememberAnimatedSegmentedIslandItemShape
import com.splitmate.app.ui.components.rememberReducedMotionEnabled
import com.splitmate.app.ui.components.segmentedIslandItemShape
import com.splitmate.app.ui.feel.MoneyScaffoldSnackbarHost
import com.splitmate.app.ui.fetchLivePnrAndTrainStatus
import com.splitmate.app.ui.formatTravelExpenseTitle
import com.splitmate.app.ui.theme.activeTransitExtendedColors
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.splitmate.app.ui.components.InFlightWavyProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// ============================================================================
// 1. DESIGN TOKENS — "TACTILE LUXURY PAPER BOARDING PASS" (3-THEME EXPRESSIVE)
// ============================================================================
object TactilePaperPassTokens {
    val CanvasBackground: Color
        get() = DesignSystemBindings.activePalette.surfaceContainerLow
    val PaperSurface: Color
        get() = DesignSystemBindings.activePalette.surfaceContainerLowest
    val PaperStubSurface: Color
        get() = DesignSystemBindings.activePalette.surfaceContainer
    val HairlineBorder: Color
        get() = DesignSystemBindings.activePalette.outlineVariant
    // v2.3.6: boarding-pass / forest-pass / status-pill colours are EXTENDED colours resolved
    // per theme from `TransitExtendedColors` (hex lives only there).
    val PerforationLine: Color
        get() = activeTransitExtendedColors.boardingPass.perforation

    val ForestTop: Color
        get() = activeTransitExtendedColors.trainTicket.color
    val ForestBottom: Color
        get() = activeTransitExtendedColors.trainTicket.colorDeep
    val ForestBadgeFill: Color
        get() = activeTransitExtendedColors.trainTicket.colorRaised
    val ForestBadgeStroke: Color
        get() = activeTransitExtendedColors.trainTicket.colorRaisedOutline
    /** Headline ink on the forest pass (ForestTop / ForestBottom / ForestBadgeFill). */
    val OnForest: Color
        get() = activeTransitExtendedColors.trainTicket.onColor
    /** Sage accent text / icons on the forest pass. */
    val ForestAccent: Color
        get() = activeTransitExtendedColors.trainTicket.onColorVariant
    /** Travel-class caption on the forest pass. */
    val ForestSubtle: Color
        get() = activeTransitExtendedColors.trainTicket.onColorSubtle
    /** Departure / arrival time captions on the forest pass. */
    val ForestMuted: Color
        get() = activeTransitExtendedColors.trainTicket.onColorMuted
    /** Decorative dashed route line on the forest pass. */
    val ForestRouteLine: Color
        get() = activeTransitExtendedColors.trainTicket.decoration

    val AmberChartBg: Color
        get() = activeTransitExtendedColors.statusPending.roles.container
    val AmberChartBorder: Color
        get() = activeTransitExtendedColors.statusPending.outline
    val AmberChartText: Color
        get() = activeTransitExtendedColors.statusPending.roles.onContainer

    val TerracottaWaitlistBg: Color
        get() = DesignSystemBindings.activePalette.secondaryContainer
    val TerracottaWaitlistBorder: Color
        get() = activeTransitExtendedColors.statusWaitlist.outline
    val TerracottaWaitlistText: Color
        get() = DesignSystemBindings.activePalette.onSecondaryContainer

    val SageConfirmedBg: Color
        get() = DesignSystemBindings.activePalette.primaryContainer
    val SageConfirmedBorder: Color
        get() = activeTransitExtendedColors.statusConfirmed.outline
    val SageConfirmedText: Color
        get() = when (DesignSystemBindings.activeThemeMode) {
            SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> DesignSystemBindings.activePalette.onPrimaryContainer
            SplitMateThemeMode.SUNLIT_BUCKWHEAT,
            SplitMateThemeMode.KYOTO_MATCHA_YUZU -> DesignSystemBindings.activePalette.primary
        }
    /** Icon ink on a [SageConfirmedText]-filled badge (role pair, >= 4.5:1 in every theme). */
    val SageConfirmedOnText: Color
        get() = when (DesignSystemBindings.activeThemeMode) {
            SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> DesignSystemBindings.activePalette.primaryContainer
            SplitMateThemeMode.SUNLIT_BUCKWHEAT,
            SplitMateThemeMode.KYOTO_MATCHA_YUZU -> DesignSystemBindings.activePalette.onPrimary
        }

    val InkPrimary: Color
        get() = DesignSystemBindings.activePalette.onSurface
    val InkSecondary: Color
        get() = DesignSystemBindings.activePalette.onSurfaceVariant
    /**
     * v2.3.6: was a per-theme grey (#9C9488 Sunlit = 2.9:1 on white, below WCAG 4.5:1).
     * Excluded rows keep their strike-through; the ink now uses onSurfaceVariant.
     */
    val InkMuted: Color
        get() = DesignSystemBindings.activePalette.onSurfaceVariant

    /** Neutral chip / avatar disc behind InkSecondary or SageConfirmedText. */
    val NeutralChip: Color
        get() = DesignSystemBindings.activePalette.surfaceContainer
    /** Disabled CTA slab (dark: raised well, light: deepest tonal surface). */
    val DisabledSlab: Color
        get() = if (DesignSystemBindings.activePalette.mode.isDark) {
            DesignSystemBindings.activePalette.surfaceContainerHigh
        } else {
            DesignSystemBindings.activePalette.surfaceContainerHighest
        }
    /** Excluded-member badge fill (M3 outline: >= 3:1 against every surface). */
    val ExcludedBadge: Color
        get() = DesignSystemBindings.activePalette.outline
    val ExcludedBadgeIcon: Color
        get() = DesignSystemBindings.activePalette.surfaceContainerLowest

    @Composable
    fun tactileTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = InkPrimary,
        unfocusedTextColor = InkPrimary,
        focusedContainerColor = PaperSurface,
        unfocusedContainerColor = PaperSurface,
        focusedBorderColor = SageConfirmedText,
        unfocusedBorderColor = HairlineBorder,
        focusedLabelColor = SageConfirmedText,
        unfocusedLabelColor = InkSecondary,
        cursorColor = SageConfirmedText
    )
}

// ============================================================================
// 2. CUSTOM SHAPE: PHYSICAL BOARDING PASS WITH SIDE NOTCHES
// ============================================================================
class TactilePaperPerforatedShape(
    private val cornerRadiusDp: Float = 24f,
    private val notchRadiusDp: Float = 12f,
    private val perforationRatio: Float = 0.77f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cornerRadius = cornerRadiusDp * density.density
        val notchRadius = notchRadiusDp * density.density
        val notchCenterY = size.height * perforationRatio

        val path = Path().apply {
            moveTo(0f, cornerRadius)
            arcTo(
                rect = Rect(0f, 0f, cornerRadius * 2, cornerRadius * 2),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(size.width - cornerRadius, 0f)
            arcTo(
                rect = Rect(size.width - cornerRadius * 2, 0f, size.width, cornerRadius * 2),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(size.width, notchCenterY - notchRadius)
            arcTo(
                rect = Rect(
                    size.width - notchRadius,
                    notchCenterY - notchRadius,
                    size.width + notchRadius,
                    notchCenterY + notchRadius
                ),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            lineTo(size.width, size.height - cornerRadius)
            arcTo(
                rect = Rect(
                    size.width - cornerRadius * 2,
                    size.height - cornerRadius * 2,
                    size.width,
                    size.height
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(cornerRadius, size.height)
            arcTo(
                rect = Rect(0f, size.height - cornerRadius * 2, cornerRadius * 2, size.height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(0f, notchCenterY + notchRadius)
            arcTo(
                rect = Rect(
                    -notchRadius,
                    notchCenterY - notchRadius,
                    notchRadius,
                    notchCenterY + notchRadius
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            close()
        }
        return Outline.Generic(path)
    }
}

// ============================================================================
// 3. DATA MODELS & CONCURRENCY-SAFE SESSION CONTROLLER FOR THE UI
// ============================================================================
data class PassengerTicketRow(
    val id: String,
    val memberId: String?,
    val name: String,
    val isPayer: Boolean = false,
    val bookingStatus: String,
    val currentStatus: String,
    val isConfirmed: Boolean = false,
    val exactShareDisplay: String? = null
)

/**
 * Concurrency-safe state coordinator for [PnrExpenseReviewScreen].
 *
 * Enforces:
 * - C-01: Cancels any prior in-flight lookup Job when PNR digits change and rejects stale completions.
 * - C-02: Preserves mid-fetch user member/payer customizations (`hasUserCustomizedSplit` / `hasUserCustomizedPayer`)
 *         and reads the latest group member list at completion time.
 * - C-03: Isolates per-PNR transient manual station/fare overrides so PNR B never inherits PNR A's route or fare.
 * - M-06: Selects `paxCount.coerceAtMost(groupMembers.size)` members when matching by passenger count.
 */
class PnrReviewSessionController(
    private val scope: CoroutineScope,
    private val fetcher: suspend (cleanPnr: String, forceManualRefresh: Boolean) -> LivePnrStatusSnapshot
) {
    var pnrInput by mutableStateOf("")
    var isFetching by mutableStateOf(false)
    var fetchError by mutableStateOf<String?>(null)
    var liveSnapshot by mutableStateOf<LivePnrStatusSnapshot?>(null)

    var manualTotalFareInput by mutableStateOf("")
    var manualFromStationInput by mutableStateOf("")
    var manualToStationInput by mutableStateOf("")
    var manualDepartureDateInput by mutableStateOf("")
    var manualDepartureTimeInput by mutableStateOf("")

    var selectedMemberIds by mutableStateOf<Set<String>>(emptySet())
    var selectedPayerId by mutableStateOf("")
    var hasUserCustomizedSplit by mutableStateOf(false)
    var hasUserCustomizedPayer by mutableStateOf(false)

    private var activeLookupJob: Job? = null

    fun syncForGroupOrExistingExpense(
        groupMembers: List<GroupMemberEntity>,
        existingExpenseInGroup: com.splitmate.app.data.ExpenseEntity?,
        existingSplitIdsInGroup: Set<String>,
        forceResetUserCustomizations: Boolean = false
    ) {
        if (forceResetUserCustomizations) {
            hasUserCustomizedSplit = false
            hasUserCustomizedPayer = false
        }
        val validMemberIdSet = groupMembers.map { it.memberId }.toSet()
        if (!hasUserCustomizedSplit || selectedMemberIds.none { it in validMemberIdSet }) {
            selectedMemberIds = existingSplitIdsInGroup
                .intersect(validMemberIdSet)
                .ifEmpty { validMemberIdSet }
        } else {
            selectedMemberIds = selectedMemberIds.intersect(validMemberIdSet).ifEmpty { validMemberIdSet }
        }
        if (!hasUserCustomizedPayer || selectedPayerId !in validMemberIdSet) {
            val existingPayer = existingExpenseInGroup?.payerId?.takeIf { it in validMemberIdSet }
            selectedPayerId = existingPayer
                ?: groupMembers.firstOrNull { it.isCurrentUser }?.memberId
                ?: groupMembers.firstOrNull()?.memberId.orEmpty()
        }
    }

    fun onToggleMember(memberId: String) {
        hasUserCustomizedSplit = true
        selectedMemberIds = if (memberId in selectedMemberIds) {
            if (selectedMemberIds.size > 1) selectedMemberIds - memberId else selectedMemberIds
        } else {
            selectedMemberIds + memberId
        }
    }

    fun onSelectExactTicketCount(groupMembers: List<GroupMemberEntity>, ticketPassengerCount: Int) {
        hasUserCustomizedSplit = true
        val safeTake = ticketPassengerCount.coerceAtLeast(1).coerceAtMost(groupMembers.size)
        selectedMemberIds = groupMembers.take(safeTake).map { it.memberId }.toSet()
    }

    fun onSelectPayer(payerId: String) {
        hasUserCustomizedPayer = true
        selectedPayerId = payerId
    }

    fun onPnrDigitsChanged(
        rawValue: String,
        groupMembersProvider: () -> List<GroupMemberEntity>,
        existingExpenseProvider: (String) -> com.splitmate.app.data.ExpenseEntity?,
        existingSplitsProvider: (com.splitmate.app.data.ExpenseEntity?) -> Set<String>,
        autoTriggerWhen10Digits: Boolean = true
    ) {
        val digits = rawValue.filter { it.isDigit() }.take(10)
        val changed = digits != pnrInput
        pnrInput = digits
        if (changed) {
            activeLookupJob?.cancel()
            activeLookupJob = null
            isFetching = false
            fetchError = null
            if (liveSnapshot?.pnr != digits) {
                liveSnapshot = null
                manualTotalFareInput = ""
                manualFromStationInput = ""
                manualToStationInput = ""
                manualDepartureDateInput = ""
                manualDepartureTimeInput = ""
                val membersNow = groupMembersProvider()
                val existingNow = if (digits.length == 10) existingExpenseProvider(digits) else null
                syncForGroupOrExistingExpense(
                    groupMembers = membersNow,
                    existingExpenseInGroup = existingNow,
                    existingSplitIdsInGroup = existingSplitsProvider(existingNow),
                    forceResetUserCustomizations = true
                )
            }
        }
        val currentSnap = liveSnapshot
        if (autoTriggerWhen10Digits &&
            digits.length == 10 &&
            (currentSnap == null || currentSnap.pnr != digits || !currentSnap.isLiveVerified)
        ) {
            triggerLookup(
                targetPnr = digits,
                forceManualRefresh = false,
                groupMembersProvider = groupMembersProvider,
                existingExpenseProvider = existingExpenseProvider,
                existingSplitsProvider = existingSplitsProvider
            )
        }
    }

    fun applyImportedSnapshot(
        imported: LivePnrStatusSnapshot,
        groupMembers: List<GroupMemberEntity>,
        existingExpenseInGroup: com.splitmate.app.data.ExpenseEntity?,
        existingSplitsInGroup: Set<String>
    ) {
        activeLookupJob?.cancel()
        activeLookupJob = null
        isFetching = false
        fetchError = null
        pnrInput = imported.pnr
        liveSnapshot = imported
        manualTotalFareInput = ""
        manualFromStationInput = ""
        manualToStationInput = ""
        manualDepartureDateInput = ""
        manualDepartureTimeInput = ""
        applyFetchedMemberDefaults(
            fetched = imported,
            membersNow = groupMembers,
            existingMatch = existingExpenseInGroup,
            existingSplits = existingSplitsInGroup
        )
    }

    fun triggerLookup(
        targetPnr: String,
        forceManualRefresh: Boolean,
        groupMembersProvider: () -> List<GroupMemberEntity>,
        existingExpenseProvider: (String) -> com.splitmate.app.data.ExpenseEntity?,
        existingSplitsProvider: (com.splitmate.app.data.ExpenseEntity?) -> Set<String>,
        onCompleted: ((LivePnrStatusSnapshot) -> Unit)? = null
    ) {
        val clean = targetPnr.filter { it.isDigit() }.take(10)
        if (clean.length != 10) {
            fetchError = "Please enter a valid 10-digit IRCTC PNR number"
            return
        }
        pnrInput = clean
        fetchError = null
        activeLookupJob?.cancel()
        isFetching = true

        activeLookupJob = scope.launch {
            val fetched = try {
                fetcher(clean, forceManualRefresh)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                com.splitmate.app.data.PnrNetworkRepository.buildUnverifiedManualFallbackSnapshot(cleanPnr = clean)
            }
            if (!isActive || pnrInput != clean) return@launch

            isFetching = false
            liveSnapshot = fetched

            val membersNow = groupMembersProvider()
            val existingMatch = existingExpenseProvider(clean)
            val existingSplits = existingSplitsProvider(existingMatch)
            applyFetchedMemberDefaults(
                fetched = fetched,
                membersNow = membersNow,
                existingMatch = existingMatch,
                existingSplits = existingSplits
            )

            fetchError = when {
                fetched.isInvalidOrFlushedPnr ->
                    "PNR $clean was reported invalid or flushed by CRIS (expired/past journey). You can still log ticket details manually below."
                !fetched.isLiveVerified && existingMatch == null ->
                    "Live IRCTC server unreachable or rate-limited — paste your IRCTC SMS/PDF above or enter ticket details manually below."
                else -> null
            }
            onCompleted?.invoke(fetched)
        }
    }

    private fun applyFetchedMemberDefaults(
        fetched: LivePnrStatusSnapshot,
        membersNow: List<GroupMemberEntity>,
        existingMatch: com.splitmate.app.data.ExpenseEntity?,
        existingSplits: Set<String>
    ) {
        val validIds = membersNow.map { it.memberId }.toSet()
        if (existingMatch != null) {
            val validExistingSplits = existingSplits.intersect(validIds)
            if (!hasUserCustomizedSplit && validExistingSplits.isNotEmpty()) {
                selectedMemberIds = validExistingSplits
            }
            if (!hasUserCustomizedPayer && existingMatch.payerId in validIds) {
                selectedPayerId = existingMatch.payerId
            }
        } else if (fetched.isLiveVerified || fetched.baseFarePaise > 0L || fetched.structuredPassengers.isNotEmpty()) {
            val matchedTrainMembers = fetched.structuredPassengers.mapNotNull { pax ->
                matchSinglePassengerToGroupMember(pax.passengerNumber, membersNow)
            }.distinctBy { it.memberId }

            if (!hasUserCustomizedPayer && matchedTrainMembers.isNotEmpty()) {
                selectedPayerId = matchedTrainMembers.first().memberId
            }
            val paxCount = fetched.effectivePassengerCount.coerceAtLeast(1)
            if (!hasUserCustomizedSplit && membersNow.isNotEmpty()) {
                selectedMemberIds = if (matchedTrainMembers.size >= 2) {
                    matchedTrainMembers.map { it.memberId }.toSet()
                } else {
                    membersNow.take(paxCount.coerceAtMost(membersNow.size)).map { it.memberId }.toSet()
                }
            }
        }
    }
}

// ============================================================================
// 4. MAIN SCREEN: PNR SEARCH + BOARDING PASS + MEMBER SPLIT + LEDGER CONFIRM
// ============================================================================
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PnrExpenseReviewScreen(
    viewModel: SplitMateViewModel,
    initialPnr: String = "",
    onBackClick: () -> Unit,
    onExpenseAdded: () -> Unit = onBackClick
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val activeGroup = uiState.activeGroup
    val groupMembers = uiState.activeGroupMembers
    val latestGroupMembers by rememberUpdatedState(groupMembers)
    val latestUiState by rememberUpdatedState(uiState)

    val initialExpenseId = remember(initialPnr) {
        initialPnr.takeIf { it.startsWith("EXPENSE:", ignoreCase = true) }
            ?.substringAfter(":")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
    val initialExpenseMatch = remember(initialExpenseId, uiState.expenses) {
        initialExpenseId?.let { expId -> uiState.expenses.find { it.expenseId == expId } }
    }
    val resolvedInitialPnrDigits = remember(initialPnr, initialExpenseMatch?.expenseId) {
        if (initialExpenseMatch != null) {
            initialExpenseMatch.travelPnr.filter { it.isDigit() }.take(10).ifBlank {
                com.splitmate.app.ui.extractTravelTicketFromTitle(initialExpenseMatch.title)
                    ?.pnr
                    ?.filter { it.isDigit() }
                    ?.take(10)
                    .orEmpty()
            }
        } else {
            initialPnr.filter { it.isDigit() }.take(10)
        }
    }

    // One-time group alignment on initial screen launch ONLY (never hijacks manual group switches!)
    var didInitialGroupAlign by remember(initialPnr) { mutableStateOf(false) }
    LaunchedEffect(initialPnr, uiState.expenses.size) {
        if (!didInitialGroupAlign && initialPnr.isNotBlank()) {
            val targetExpense = initialExpenseMatch ?: resolvedInitialPnrDigits
                .takeIf { it.length == 10 }
                ?.let { p10 ->
                    uiState.expenses.firstOrNull { it.groupId == uiState.activeGroupId && (it.travelPnr == p10 || it.title.contains(p10)) }
                        ?: uiState.expenses.firstOrNull { it.travelPnr == p10 || it.title.contains(p10) }
                }
            if (targetExpense != null) {
                didInitialGroupAlign = true
                if (targetExpense.groupId.isNotBlank() && targetExpense.groupId != uiState.activeGroupId) {
                    viewModel.selectActiveGroup(targetExpense.groupId)
                }
            } else if (uiState.expenses.isNotEmpty() || initialExpenseId == null) {
                didInitialGroupAlign = true
            }
        }
    }

    // Check which Train tickets are already logged in the ACTIVE group (strictly group-scoped)
    val existingPnrExpensesInGroup = remember(uiState.expenses, uiState.activeGroupId) {
        uiState.expenses.filter { exp ->
            exp.groupId == uiState.activeGroupId && (
                exp.expenseCategory.equals("TRAIN", ignoreCase = true) ||
                    (exp.title.contains("PNR:", ignoreCase = true) &&
                        com.splitmate.app.ui.extractTravelTicketFromTitle(exp.title)?.pnr?.let { pnr ->
                            pnr.length == 10 && pnr.all { ch -> ch.isDigit() }
                        } == true)
                )
        }
    }

    fun findExistingTrainExpenseInActiveGroup(clean10: String): com.splitmate.app.data.ExpenseEntity? {
        val stateNow = latestUiState
        val activeGid = stateNow.activeGroupId
        if (initialExpenseMatch != null && initialExpenseMatch.groupId == activeGid) {
            val initPnr = initialExpenseMatch.travelPnr.filter { it.isDigit() }.take(10)
            if (clean10.isBlank() || initPnr == clean10 || initialExpenseMatch.title.contains(clean10)) {
                return initialExpenseMatch
            }
        }
        if (clean10.length != 10) return null
        return stateNow.expenses.firstOrNull { exp ->
            exp.groupId == activeGid && (exp.travelPnr == clean10 || exp.title.contains(clean10))
        }
    }

    fun existingSplitsForExpense(exp: com.splitmate.app.data.ExpenseEntity?): Set<String> {
        if (exp == null) return emptySet()
        return latestUiState.splits
            .filter { it.expenseId == exp.expenseId && it.finalOwedCents > 0L }
            .map { it.memberId }
            .toSet()
    }

    val controller = remember(coroutineScope) {
        PnrReviewSessionController(
            scope = coroutineScope,
            fetcher = { clean, forceManualRefresh ->
                val stateNow = latestUiState
                val activeMatch = findExistingTrainExpenseInActiveGroup(clean)
                    ?: stateNow.expenses.firstOrNull { it.travelPnr == clean || it.title.contains(clean) }
                val existingParsed = activeMatch?.let { exp ->
                    val parsed = com.splitmate.app.ui.extractTravelTicketFromTitle(exp.title)
                        ?: ParsedTravelTicket(pnr = clean, cleanTitle = exp.title)
                    val exactDecimalFare = com.splitmate.app.ui.LivePnrStatusSnapshot(
                        pnr = clean,
                        trainNo = "",
                        trainName = "",
                        fromStation = "",
                        toStation = "",
                        departureTime = "",
                        bookingStatusBadge = "",
                        chartPrepared = false,
                        passengerStatuses = emptyList(),
                        coachPositionHint = "",
                        liveTrainLocationRadar = "",
                        confirmationProbability = "",
                        sourceLabel = ""
                    ).formatPaiseAsDecimalRupees(exp.totalAmountCents)
                    parsed.copy(
                        pnr = clean,
                        fareRupees = parsed.fareRupees.ifBlank { if (exp.totalAmountCents > 0L) exactDecimalFare else "" }
                    )
                }
                val cachedLocal = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.splitmate.app.data.PnrNetworkRepository.loadPersistedPnrSnapshot(context, clean)
                }
                if (!forceManualRefresh &&
                    cachedLocal != null &&
                    (cachedLocal.isLiveVerified || com.splitmate.app.data.PnrNetworkRepository.isSnapshotAllConfirmed(cachedLocal))
                ) {
                    return@PnrReviewSessionController cachedLocal
                }
                val sanitizedFallback = (existingParsed ?: ParsedTravelTicket(pnr = clean)).let { ticket ->
                    val cleanFrom = ticket.fromStation
                        .takeIf { it.isNotBlank() && !it.equals("ORG", ignoreCase = true) && !it.equals("ORIG", ignoreCase = true) }
                        ?: cachedLocal?.fromStation?.takeIf { !it.equals("ORG", ignoreCase = true) && !it.equals("ORIG", ignoreCase = true) }
                            .orEmpty()
                    val cleanTo = ticket.toStation
                        .takeIf { it.isNotBlank() && !it.equals("DST", ignoreCase = true) && !it.equals("DEST", ignoreCase = true) }
                        ?: cachedLocal?.toStation?.takeIf { !it.equals("DST", ignoreCase = true) && !it.equals("DEST", ignoreCase = true) }
                            .orEmpty()
                    ticket.copy(fromStation = cleanFrom, toStation = cleanTo)
                }
                fetchLivePnrAndTrainStatus(
                    pnr = clean,
                    fallbackTicket = sanitizedFallback,
                    forceManualRefresh = forceManualRefresh,
                    context = context
                )
            }
        )
    }

    var isSubmitting by remember { mutableStateOf(false) }

    val pnrInput = controller.pnrInput
    val isFetching = controller.isFetching
    val fetchError = controller.fetchError
    val liveSnapshot = controller.liveSnapshot
    val manualTotalFareInput = controller.manualTotalFareInput
    val manualFromStationInput = controller.manualFromStationInput
    val manualToStationInput = controller.manualToStationInput
    val manualDepartureDateInput = controller.manualDepartureDateInput
    val manualDepartureTimeInput = controller.manualDepartureTimeInput
    val selectedMemberIds = controller.selectedMemberIds
    val selectedPayerId = controller.selectedPayerId

    // Strictly scoped to the ACTIVE group (M-05: never edits Group A's expense when Group B is selected)
    val selectedExistingExpense = remember(
        existingPnrExpensesInGroup,
        uiState.activeGroupId,
        pnrInput,
        liveSnapshot,
        initialExpenseMatch?.expenseId
    ) {
        val clean10 = (liveSnapshot?.pnr?.takeIf { it.length == 10 } ?: pnrInput.filter { it.isDigit() }).take(10)
        if (initialExpenseMatch != null && initialExpenseMatch.groupId == uiState.activeGroupId &&
            (clean10.isBlank() || initialExpenseMatch.travelPnr == clean10 || initialExpenseMatch.title.contains(clean10))
        ) {
            initialExpenseMatch
        } else if (clean10.length == 10) {
            existingPnrExpensesInGroup.firstOrNull { it.travelPnr == clean10 || it.title.contains(clean10) }
        } else null
    }

    // Keep selectedMemberIds & selectedPayerId strictly inside activeGroupMembers whenever active group changes
    LaunchedEffect(uiState.activeGroupId, groupMembers, selectedExistingExpense?.expenseId) {
        controller.syncForGroupOrExistingExpense(
            groupMembers = groupMembers,
            existingExpenseInGroup = selectedExistingExpense,
            existingSplitIdsInGroup = existingSplitsForExpense(selectedExistingExpense),
            forceResetUserCustomizations = false
        )
    }

    var cooldownRemainingSec by remember(pnrInput) {
        mutableIntStateOf(
            com.splitmate.app.data.PnrNetworkRepository.getRemainingManualRefreshCooldownSeconds(context, pnrInput)
        )
    }

    LaunchedEffect(pnrInput, liveSnapshot?.fetchedAtEpochMs) {
        cooldownRemainingSec = maxOf(
            liveSnapshot?.cooldownRemainingSeconds ?: 0,
            com.splitmate.app.data.PnrNetworkRepository.getRemainingManualRefreshCooldownSeconds(context, pnrInput)
        )
    }

    LaunchedEffect(pnrInput, cooldownRemainingSec > 0) {
        while (cooldownRemainingSec > 0) {
            kotlinx.coroutines.delay(1_000L)
            cooldownRemainingSec = com.splitmate.app.data.PnrNetworkRepository
                .getRemainingManualRefreshCooldownSeconds(context, pnrInput)
        }
    }

    fun triggerLivePnrLookup(targetPnr: String, isExplicitManualRefresh: Boolean = true) {
        controller.triggerLookup(
            targetPnr = targetPnr,
            forceManualRefresh = isExplicitManualRefresh,
            groupMembersProvider = { latestGroupMembers },
            existingExpenseProvider = ::findExistingTrainExpenseInActiveGroup,
            existingSplitsProvider = ::existingSplitsForExpense,
            onCompleted = { fetched ->
                cooldownRemainingSec = maxOf(
                    fetched.cooldownRemainingSeconds,
                    com.splitmate.app.data.PnrNetworkRepository.getRemainingManualRefreshCooldownSeconds(context, fetched.pnr)
                )
            }
        )
    }

    LaunchedEffect(initialPnr, initialExpenseMatch?.expenseId) {
        if (resolvedInitialPnrDigits.length == 10 && liveSnapshot == null) {
            controller.pnrInput = resolvedInitialPnrDigits
            triggerLivePnrLookup(resolvedInitialPnrDigits, isExplicitManualRefresh = false)
        } else if (initialExpenseMatch != null && resolvedInitialPnrDigits.isBlank() && liveSnapshot == null) {
            val parsedFromExp = com.splitmate.app.ui.extractTravelTicketFromTitle(initialExpenseMatch.title)
                ?: ParsedTravelTicket(cleanTitle = initialExpenseMatch.title)
            val fallbackSnap = com.splitmate.app.data.PnrNetworkRepository.buildUnverifiedManualFallbackSnapshot(
                cleanPnr = "",
                fallbackTicket = parsedFromExp
            )
            controller.liveSnapshot = fallbackSnap.copy(
                baseFarePaiseOverride = initialExpenseMatch.totalAmountCents,
                totalPayablePaiseOverride = initialExpenseMatch.totalAmountCents,
                irctcPaymentMode = "NONE",
                includeInsurance = false
            )
            controller.manualFromStationInput = parsedFromExp.fromStation
            controller.manualToStationInput = parsedFromExp.toStation
            controller.manualDepartureDateInput = parsedFromExp.departureDate
            controller.manualDepartureTimeInput = parsedFromExp.departureTime
        }
    }

    // Companion PDF picker for IRCTC Electronic Reservation Slips (ERS)
    val irctcPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val memberNamesSnapshot = latestGroupMembers.map { it.name }
            coroutineScope.launch {
                val extracted = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            com.splitmate.app.data.UniversalFlightTicketExtractor.extractFromPdfStream(
                                inputStream = stream,
                                groupMemberNames = memberNamesSnapshot,
                                context = context
                            )
                        }
                    }.getOrNull()
                }
                if (extracted != null && extracted.isValidFlightTicket) {
                    val snap = extracted.toLivePnrStatusSnapshot()
                    if (snap.pnr.length == 10 && snap.isLiveVerified) {
                        com.splitmate.app.data.PnrNetworkRepository.savePersistedPnrSnapshot(context, snap)
                    }
                    val existingExp = findExistingTrainExpenseInActiveGroup(snap.pnr)
                    controller.applyImportedSnapshot(
                        imported = snap,
                        groupMembers = latestGroupMembers,
                        existingExpenseInGroup = existingExp,
                        existingSplitsInGroup = existingSplitsForExpense(existingExp)
                    )
                } else {
                    controller.fetchError = "Could not extract an IRCTC train ticket from the selected PDF."
                }
            }
        }
    }

    fun handlePasteIrctcSmsFromClipboard() {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clipText = clipboard?.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?.trim()
            .orEmpty()
        if (clipText.isBlank()) {
            controller.fetchError = "Clipboard is empty — copy your IRCTC SMS or 10-digit PNR first."
            return
        }
        val parsedSms = com.splitmate.app.data.IrctcTicketTextExtractor.extractFromText(clipText)
        if (parsedSms != null) {
            if (parsedSms.pnr.length == 10 && parsedSms.fromStation.isNotBlank() && parsedSms.toStation.isNotBlank()) {
                com.splitmate.app.data.PnrNetworkRepository.savePersistedPnrSnapshot(context, parsedSms)
            }
            val existingExp = findExistingTrainExpenseInActiveGroup(parsedSms.pnr)
            controller.applyImportedSnapshot(
                imported = parsedSms,
                groupMembers = latestGroupMembers,
                existingExpenseInGroup = existingExp,
                existingSplitsInGroup = existingSplitsForExpense(existingExp)
            )
            return
        }
        val barePnr = com.splitmate.app.data.PnrNetworkRepository.extractPnrFromFreeText(clipText)
            .takeIf { it.length == 10 && it.all { ch -> ch.isDigit() } }
        if (barePnr != null) {
            controller.onPnrDigitsChanged(
                rawValue = barePnr,
                groupMembersProvider = { latestGroupMembers },
                existingExpenseProvider = ::findExistingTrainExpenseInActiveGroup,
                existingSplitsProvider = ::existingSplitsForExpense,
                autoTriggerWhen10Digits = true
            )
        } else {
            controller.fetchError = "No 10-digit IRCTC PNR or ticket SMS found in clipboard."
        }
    }

    val pnrTearProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    val pnrStampScale = remember { androidx.compose.animation.core.Animatable(1.75f) }
    val pnrStampAlpha = remember { androidx.compose.animation.core.Animatable(0f) }

    // Format helper delegating to Indian Lakh/Crore paise formatter (M-08: sign-safe & exact)
    fun formatPaiseDisplay(paise: Long): String =
        com.splitmate.app.ui.formatIndianRupeesFromCents(cents = paise, trimZeroDecimals = true)

    val snapshot = liveSnapshot
    val selectedMembersList = remember(groupMembers, selectedMemberIds) {
        groupMembers.filter { it.memberId in selectedMemberIds }
    }
    val ticketPassengerCount = when {
        snapshot == null -> selectedMembersList.size.coerceAtLeast(1)
        else -> maxOf(snapshot.effectivePassengerCount, selectedMembersList.size).coerceAtLeast(1)
    }

    // v1.8.5 Official IRCTC Dynamic Bill Engine Controls (UPI default | Card | Base ₹0 + Insurance)
    var selectedIrctcPaymentMode by remember(snapshot?.pnr, snapshot?.isManualEntry) {
        val defaultMode = when {
            snapshot == null -> "UPI"
            snapshot.isManualEntry && snapshot.baseFarePaise == 0L -> "NONE"
            snapshot.irctcPaymentMode.isNotBlank() -> snapshot.irctcPaymentMode.uppercase(Locale.US)
            else -> "UPI"
        }
        mutableStateOf(defaultMode)
    }
    var includeIrctcInsurance by remember(snapshot?.pnr, snapshot?.isPurelyWaitlistedAtBooking, snapshot?.isManualEntry) {
        val defaultIns = when {
            snapshot == null -> true
            snapshot.isManualEntry && snapshot.baseFarePaise == 0L -> false
            else -> snapshot.defaultIncludeInsurance
        }
        mutableStateOf(defaultIns)
    }
    var hasUserAdjustedIrctcFeeControls by remember(snapshot?.pnr, selectedExistingExpense?.expenseId) {
        mutableStateOf(false)
    }

    val manualFarePaise = remember(manualTotalFareInput) {
        com.splitmate.app.ui.parseRupeesToPaiseExact(manualTotalFareInput) ?: 0L
    }

    // Separate baseFarePaise vs convenience/insurance fees so Vault never compounds +₹25.00 (M-01 & M-04)
    val baseFarePaise = when {
        manualFarePaise > 0L -> manualFarePaise
        snapshot != null && snapshot.baseFarePaise > 0L -> snapshot.baseFarePaise
        selectedExistingExpense != null -> selectedExistingExpense.totalAmountCents
        else -> 0L
    }
    val billReferenceSnapshot = remember(snapshot, baseFarePaise, ticketPassengerCount) {
        snapshot?.copy(
            baseFarePaiseOverride = baseFarePaise,
            passengerCount = ticketPassengerCount
        )
    }
    val useExistingLockedTotal = selectedExistingExpense != null &&
        manualFarePaise == 0L &&
        !hasUserAdjustedIrctcFeeControls &&
        (snapshot == null || snapshot.baseFarePaise <= 0L || snapshot.totalPayablePaiseOverride == selectedExistingExpense.totalAmountCents)

    val convenienceFeePaise = when {
        billReferenceSnapshot == null || baseFarePaise <= 0L -> 0L
        useExistingLockedTotal && snapshot?.baseFarePaise?.let { it > 0L && it < selectedExistingExpense!!.totalAmountCents } != true -> 0L
        selectedIrctcPaymentMode == "UPI" -> billReferenceSnapshot.irctcConvenienceFeeUpiPaise
        selectedIrctcPaymentMode == "CARD" -> billReferenceSnapshot.irctcConvenienceFeeCardPaise
        else -> 0L
    }
    val insuranceFeePaise = when {
        billReferenceSnapshot == null || baseFarePaise <= 0L -> 0L
        useExistingLockedTotal && snapshot?.baseFarePaise?.let { it > 0L && it < selectedExistingExpense!!.totalAmountCents } != true -> 0L
        includeIrctcInsurance -> billReferenceSnapshot.travelInsurancePaise
        else -> 0L
    }
    val effectiveTotalPaise = when {
        manualFarePaise > 0L -> baseFarePaise + convenienceFeePaise + insuranceFeePaise
        hasUserAdjustedIrctcFeeControls && baseFarePaise > 0L -> baseFarePaise + convenienceFeePaise + insuranceFeePaise
        selectedExistingExpense != null -> selectedExistingExpense.totalAmountCents
        snapshot?.totalPayablePaiseOverride != null && !hasUserAdjustedIrctcFeeControls -> snapshot.totalPayablePaiseOverride
        else -> baseFarePaise + convenienceFeePaise + insuranceFeePaise
    }

    val currentUser = remember(groupMembers) { groupMembers.find { it.isCurrentUser } }
    val exactAllocationsMap = remember(effectiveTotalPaise, selectedMembersList, selectedPayerId, currentUser) {
        com.splitmate.app.SplitMateMathEngine.splitEquallyZeroDrift(
            totalCents = effectiveTotalPaise,
            members = selectedMembersList.map { it.memberId to it.name },
            payerId = selectedPayerId,
            currentUserId = currentUser?.memberId
        ).associateBy { it.memberId }
    }

    val effectiveDividerCount = if (selectedMembersList.size >= 2) {
        selectedMembersList.size
    } else {
        selectedMembersList.size.coerceAtLeast(ticketPassengerCount)
    }
    val perPersonTicketSharePaise = if (effectiveDividerCount > 0) effectiveTotalPaise / effectiveDividerCount else 0L
    val perSelectedMemberSharePaise = if (selectedMembersList.isNotEmpty()) effectiveTotalPaise / selectedMembersList.size else perPersonTicketSharePaise
    val perPersonHeadlineCaption = remember(effectiveTotalPaise, selectedMembersList.size) {
        com.splitmate.app.ExpenseSplitCopy.perPersonCaption(
            totalCents = effectiveTotalPaise,
            memberCount = selectedMembersList.size.coerceAtLeast(1)
        )
    }
    val perPersonEachChipLabel = remember(effectiveTotalPaise, selectedMembersList.size) {
        com.splitmate.app.ExpenseSplitCopy.perPersonCaption(
            totalCents = effectiveTotalPaise,
            memberCount = selectedMembersList.size.coerceAtLeast(1),
            unitSuffix = " / each"
        )
    }
    val payerSharePaise = exactAllocationsMap[selectedPayerId]?.finalCents
        ?: if (selectedMembersList.any { it.memberId == selectedPayerId }) perSelectedMemberSharePaise else 0L
    // v2.3.3: UI-only split nature for review copy (no effect on the committed split).
    val reviewSplitNature = remember(selectedPayerId, selectedMemberIds) {
        com.splitmate.app.ExpenseSplitClassifier.classifySelection(selectedPayerId, selectedMemberIds)
    }
    val reviewSoleMember = selectedMembersList.singleOrNull()
    val reviewSoleLabel = com.splitmate.app.ExpenseSplitCopy.beneficiaryLabel(
        reviewSoleMember?.name, reviewSoleMember?.isCurrentUser == true
    )

    // Build PassengerTicketRow list strictly from snapshot.structuredPassengers + exact Largest-Remainder shares (M-03)
    val passengerRows = remember(snapshot, selectedMembersList, selectedPayerId, exactAllocationsMap) {
        if (snapshot == null) {
            emptyList()
        } else {
            val targetPaxCount = maxOf(snapshot.structuredPassengers.size, selectedMembersList.size).coerceAtLeast(1)
            val fallbackStatusCode = snapshot.bookingStatusBadge.substringBefore(" ").trim().ifBlank {
                if (snapshot.isLiveVerified) "CNF" else "MANUAL"
            }
            val basePax = (0 until targetPaxCount).map { idx ->
                val existingPax = snapshot.structuredPassengers.getOrNull(idx)
                val hasValidBerthStatus = existingPax != null &&
                    existingPax.currentStatus.isNotBlank() &&
                    !existingPax.currentStatus.startsWith("Class ", ignoreCase = true) &&
                    !existingPax.currentStatus.contains("/person", ignoreCase = true) &&
                    !existingPax.currentStatus.matches(Regex("""^\d+(\.\d+)?/person\)?$""", RegexOption.IGNORE_CASE))
                if (hasValidBerthStatus && existingPax != null) {
                    existingPax
                } else {
                    LivePnrPassenger(
                        passengerNumber = "P${idx + 1}",
                        initialStatus = if (snapshot.isLiveVerified) fallbackStatusCode else "MANUAL",
                        currentStatus = if (snapshot.isLiveVerified) fallbackStatusCode else "Included",
                        statusLabel = if (snapshot.isLiveVerified) "Confirmed" else "Manual Split"
                    )
                }
            }
            basePax.mapIndexed { idx, pax ->
                val matchedMember = selectedMembersList.getOrNull(idx)
                val displayName = when {
                    matchedMember != null && matchedMember.isCurrentUser -> "${matchedMember.name} (You)"
                    matchedMember != null -> matchedMember.name
                    else -> "Passenger ${idx + 1} (Tap below to assign member)"
                }
                val isCnf = pax.currentStatus.uppercase(Locale.US).let {
                    it.startsWith("CNF") || it.startsWith("CONFIRM") || it.startsWith("RAC") || it.startsWith("INC")
                }
                val cleanPaxId = if (pax.passengerNumber.startsWith("P", ignoreCase = true)) {
                    pax.passengerNumber.uppercase(Locale.US)
                } else {
                    "P${pax.passengerNumber}"
                }
                val memberExactPaise = matchedMember?.let { exactAllocationsMap[it.memberId]?.finalCents }
                PassengerTicketRow(
                    id = cleanPaxId,
                    memberId = matchedMember?.memberId,
                    name = displayName,
                    isPayer = matchedMember?.memberId == selectedPayerId,
                    bookingStatus = pax.initialStatus,
                    currentStatus = pax.currentStatus,
                    isConfirmed = isCnf,
                    exactShareDisplay = memberExactPaise?.let { formatPaiseDisplay(it) }
                )
            }
        }
    }

    val resolvedFromCode = remember(manualFromStationInput, snapshot?.fromStation) {
        manualFromStationInput.ifBlank { snapshot?.fromStation.orEmpty() }
            .trim()
            .takeIf { it.isNotBlank() && !it.equals("ORG", ignoreCase = true) && !it.equals("ORIG", ignoreCase = true) && it != "---" }
            ?.uppercase(Locale.US)
            .orEmpty()
    }
    val resolvedToCode = remember(manualToStationInput, snapshot?.toStation) {
        manualToStationInput.ifBlank { snapshot?.toStation.orEmpty() }
            .trim()
            .takeIf { it.isNotBlank() && !it.equals("DST", ignoreCase = true) && !it.equals("DEST", ignoreCase = true) && it != "---" }
            ?.uppercase(Locale.US)
            .orEmpty()
    }
    val canModifySelectedExistingExpense = remember(selectedExistingExpense, uiState) {
        selectedExistingExpense == null || viewModel.canCurrentUserModifyExpense(selectedExistingExpense, uiState)
    }

    val reducedMotion = rememberReducedMotionEnabled()
    var showManualDatePicker by remember { mutableStateOf(false) }
    var showManualTimePicker by remember { mutableStateOf(false) }

    val canConfirmExpense = snapshot != null &&
        effectiveTotalPaise > 0L &&
        resolvedFromCode.isNotBlank() &&
        resolvedToCode.isNotBlank() &&
        selectedMemberIds.isNotEmpty() &&
        canModifySelectedExistingExpense &&
        !isSubmitting

    Scaffold(
        containerColor = TactilePaperPassTokens.CanvasBackground,
        snackbarHost = { MoneyScaffoldSnackbarHost() },
        topBar = {
            var showGroupDropdown by remember { mutableStateOf(false) }
            TopAppBar(
                title = {
                    Text(
                        text = "Train ticket",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = TactilePaperPassTokens.InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                subtitle = {
                    Text(
                        text = "Active Group: ${activeGroup?.name ?: "Select a Group"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = TactilePaperPassTokens.InkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                titleHorizontalAlignment = Alignment.CenterHorizontally,
                navigationIcon = {
                    ExpressiveTooltipOutlinedIconButton(
                        tooltipText = "Back",
                        onClick = onBackClick,
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            containerColor = TactilePaperPassTokens.PaperSurface,
                            contentColor = TactilePaperPassTokens.InkPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.HairlineBorder)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = TactilePaperPassTokens.InkPrimary
                        )
                    }
                },
                actions = {
                    Box {
                        ExpressiveTooltipOutlinedIconButton(
                            tooltipText = "Switch Group",
                            onClick = { showGroupDropdown = true },
                            colors = IconButtonDefaults.outlinedIconButtonColors(
                                containerColor = TactilePaperPassTokens.PaperSurface
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.HairlineBorder)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Groups,
                                contentDescription = "Switch Group",
                                // Dark: #D7E8B6 on #1F1D1A paper (ForestTop #264010 would be ~1.4:1). Light: unchanged.
                                tint = if (SplitMateTheme.isDark) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.ForestTop,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenuPopup(
                            expanded = showGroupDropdown,
                            onDismissRequest = { showGroupDropdown = false }
                        ) {
                            DropdownMenuGroup(
                                shapes = MenuDefaults.groupShape(index = 0, count = 1)
                            ) {
                                uiState.groups.forEachIndexed { idx, group ->
                                    val isGroupSelected = group.groupId == activeGroup?.groupId
                                    DropdownMenuItem(
                                        selected = isGroupSelected,
                                        onClick = {
                                            viewModel.selectActiveGroup(group.groupId)
                                            showGroupDropdown = false
                                        },
                                        shapes = MenuDefaults.itemShape(index = idx, count = uiState.groups.size),
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Train,
                                                contentDescription = null,
                                                tint = TactilePaperPassTokens.SageConfirmedText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        selectedLeadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = TactilePaperPassTokens.SageConfirmedText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = group.name,
                                                fontFamily = FigtreeFontFamily,
                                                fontWeight = if (isGroupSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TactilePaperPassTokens.CanvasBackground
                )
            )
        },
        bottomBar = {
            if (snapshot != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            val commitScope = rememberCoroutineScope()
                            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                            Button(
                                onClick = {
                                    if (!canConfirmExpense || isSubmitting) return@Button
                                    val capturedExistingExpense = selectedExistingExpense
                                    if (capturedExistingExpense != null && !viewModel.canCurrentUserModifyExpense(capturedExistingExpense)) {
                                        controller.fetchError = com.splitmate.app.ExpenseEditPermission.READ_ONLY_REASON
                                        return@Button
                                    }
                                    val capturedSnapshot = snapshot
                                    val fromCode = resolvedFromCode
                                    val toCode = resolvedToCode
                                    if (fromCode.isBlank() || toCode.isBlank()) return@Button

                                    // C-04: Snapshot all commit inputs immutably at t=0ms before the stamp animation
                                    val capturedBaseFarePaise = baseFarePaise
                                    val capturedTotalPaise = effectiveTotalPaise
                                    val capturedPaymentMode = selectedIrctcPaymentMode
                                    val capturedIncludeInsurance = includeIrctcInsurance
                                    val capturedPayerId = selectedPayerId
                                    val capturedMemberIds = selectedMemberIds.toList()
                                    val capturedPassengerRows = passengerRows.toList()
                                    val capturedSplitNature = reviewSplitNature
                                    val capturedSoleName = reviewSoleMember?.name
                                    val capturedPerPersonCaption = perPersonHeadlineCaption
                                    val capturedDepartureDate = manualDepartureDateInput.trim().ifBlank { capturedSnapshot.departureDate }
                                    val capturedDepartureTime = manualDepartureTimeInput.trim().ifBlank { capturedSnapshot.departureTime }

                                    isSubmitting = true
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.SegmentTick)
                                    playBoardingPassTearAndStampOneShot(true)

                                    val chartText = when {
                                        !capturedSnapshot.isLiveVerified -> ""
                                        capturedSnapshot.chartPrepared -> "Chart Prepared"
                                        else -> "Chart Not Prepared"
                                    }
                                    val validBerths = capturedPassengerRows
                                        .map { it.currentStatus.trim() }
                                        .filter {
                                            it.isNotBlank() &&
                                                !it.equals("MANUAL", ignoreCase = true) &&
                                                !it.equals("Included", ignoreCase = true) &&
                                                !it.startsWith("Class ", ignoreCase = true) &&
                                                !it.contains("/person", ignoreCase = true)
                                        }
                                    val berthSegment = if (validBerths.isNotEmpty()) {
                                        " · " + validBerths.mapIndexed { idx, s -> "P${idx + 1}: $s" }.joinToString(", ")
                                    } else ""
                                    val classPrefix = if (capturedSnapshot.travelClass.isNotBlank()) {
                                        "Class ${capturedSnapshot.travelClass} · "
                                    } else ""
                                    val formattedTitle = formatTravelExpenseTitle(
                                        baseCategory = "${capturedSnapshot.trainName.ifBlank { "Train Ticket" }} ($fromCode - $toCode)",
                                        ticket = ParsedTravelTicket(
                                            pnr = capturedSnapshot.pnr,
                                            trainOrFlightNo = "${capturedSnapshot.trainNo} ${capturedSnapshot.trainName}".trim(),
                                            fromStation = fromCode,
                                            toStation = toCode,
                                            departureDate = capturedDepartureDate,
                                            departureTime = capturedDepartureTime,
                                            coachAndSeats = "$classPrefix${capturedMemberIds.size} Pax" + when (capturedSplitNature) {
                                                com.splitmate.app.ExpenseSplitNature.PERSONAL -> " · ${com.splitmate.app.ExpenseSplitCopy.PERSONAL_EXPENSE}"
                                                com.splitmate.app.ExpenseSplitNature.ON_BEHALF -> " · for ${com.splitmate.app.ExpenseSplitCopy.beneficiaryLabel(capturedSoleName, false)}"
                                                com.splitmate.app.ExpenseSplitNature.SHARED -> " ($capturedPerPersonCaption)"
                                            } + berthSegment,
                                            bookingStatus = if (capturedSnapshot.isLiveVerified) capturedSnapshot.bookingStatusBadge else "",
                                            chartStatus = chartText
                                        )
                                    )
                                    val persistedSnapshot = capturedSnapshot.copy(
                                        fromStation = fromCode,
                                        toStation = toCode,
                                        fromStationName = com.splitmate.app.ui.resolveStationDisplayName(fromCode),
                                        toStationName = com.splitmate.app.ui.resolveStationDisplayName(toCode),
                                        departureDate = capturedDepartureDate,
                                        departureTime = capturedDepartureTime,
                                        passengerCount = maxOf(capturedSnapshot.passengerCount, capturedMemberIds.size),
                                        totalFareRupees = (capturedBaseFarePaise / 100L).toInt(),
                                        baseFarePaiseOverride = capturedBaseFarePaise.takeIf { it > 0L },
                                        totalPayablePaiseOverride = capturedTotalPaise,
                                        irctcPaymentMode = capturedPaymentMode,
                                        includeInsurance = capturedIncludeInsurance,
                                        structuredPassengers = capturedPassengerRows.map { row ->
                                            LivePnrPassenger(
                                                passengerNumber = row.id,
                                                initialStatus = row.bookingStatus,
                                                currentStatus = row.currentStatus,
                                                statusLabel = if (row.isConfirmed) "Confirmed" else "Waitlisted"
                                            )
                                        }
                                    )
                                    commitScope.launch {
                                        pnrTearProgress.animateTo(1f, SplitMateMotion.fastEffects())
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm)
                                        launch { pnrStampAlpha.animateTo(1f, SplitMateMotion.fastEffects()) }
                                        pnrStampScale.animateTo(
                                            targetValue = 1f,
                                            animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.52f, stiffness = 680f)
                                        )
                                        kotlinx.coroutines.delay(95L)
                                        // R-07 & C-04: Persist Vault only for verified / real 10-digit PNRs, atomically with Room commit
                                        if (capturedSnapshot.pnr.length == 10 &&
                                            (capturedSnapshot.isLiveVerified || !capturedSnapshot.isManualEntry)
                                        ) {
                                            com.splitmate.app.data.PnrNetworkRepository.savePersistedPnrSnapshot(context, persistedSnapshot)
                                        }
                                        if (capturedExistingExpense != null) {
                                            viewModel.editExistingExpense(
                                                expenseId = capturedExistingExpense.expenseId,
                                                newTitle = formattedTitle,
                                                newTotalRupees = capturedTotalPaise / 100.0,
                                                newPayerId = capturedPayerId,
                                                selectedMemberIds = capturedMemberIds,
                                                newTotalCentsOverride = capturedTotalPaise,
                                                categoryRef = "TRAIN",
                                                explicitTravelPnr = capturedSnapshot.pnr
                                            )
                                        } else {
                                            viewModel.commitQuickEqualExpense(
                                                title = formattedTitle,
                                                totalAmountCents = capturedTotalPaise,
                                                selectedMemberIds = capturedMemberIds,
                                                payerMemberId = capturedPayerId,
                                                explicitCategory = "TRAIN",
                                                explicitTravelPnr = capturedSnapshot.pnr,
                                                explicitProviderName = capturedSnapshot.trainName
                                            )
                                        }
                                        onExpenseAdded()
                                    }
                                },
                                enabled = canConfirmExpense && !isSubmitting,
                                shapes = ButtonDefaults.shapes(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TactilePaperPassTokens.ForestTop,
                                    contentColor = TactilePaperPassTokens.OnForest,
                                    disabledContainerColor = TactilePaperPassTokens.DisabledSlab,
                                    disabledContentColor = TactilePaperPassTokens.InkSecondary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                if (isSubmitting) {
                                    ContainedLoadingIndicator(
                                        containerSize = 20.dp,
                                        containerColor = TactilePaperPassTokens.ForestBadgeFill,
                                        indicatorColor = TactilePaperPassTokens.ForestAccent
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        isSubmitting -> "Saving Pass..."
                                        !canModifySelectedExistingExpense -> com.splitmate.app.ExpenseEditPermission.READ_ONLY_REASON
                                        selectedMemberIds.isEmpty() -> "Select At Least 1 Group Member Below"
                                        resolvedFromCode.isBlank() || resolvedToCode.isBlank() -> "Enter Origin & Destination Stations Above"
                                        effectiveTotalPaise <= 0L -> "Enter Ticket Fare Above to Split"
                                        selectedExistingExpense != null -> "Update Split & Save Changes · ${formatPaiseDisplay(effectiveTotalPaise)}"
                                        reviewSplitNature == com.splitmate.app.ExpenseSplitNature.PERSONAL ->
                                            "Confirm & Add ${formatPaiseDisplay(effectiveTotalPaise)} · ${com.splitmate.app.ExpenseSplitCopy.PERSONAL_EXPENSE}"
                                        reviewSplitNature == com.splitmate.app.ExpenseSplitNature.ON_BEHALF ->
                                            "Confirm & Add ${formatPaiseDisplay(effectiveTotalPaise)} · for $reviewSoleLabel"
                                        else -> "Confirm & Add ${formatPaiseDisplay(effectiveTotalPaise)} ($perPersonHeadlineCaption)"
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val isTicketFinalConfirmed = remember(snapshot) {
            com.splitmate.app.data.PnrNetworkRepository.isSnapshotAllConfirmed(snapshot)
        }
        val pullToRefreshState = rememberPullToRefreshState()
        val canPullRefreshPnr = !isSubmitting &&
            pnrInput.filter { it.isDigit() }.length == 10 &&
            !(snapshot != null && isTicketFinalConfirmed) &&
            !(snapshot != null && cooldownRemainingSec > 0 && !isTicketFinalConfirmed)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            PullToRefreshBox(
                isRefreshing = isFetching,
                onRefresh = {
                    if (canPullRefreshPnr) {
                        triggerLivePnrLookup(pnrInput, isExplicitManualRefresh = true)
                    }
                },
                state = pullToRefreshState,
                enabled = canPullRefreshPnr,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    if (!reducedMotion) {
                        PullToRefreshDefaults.LoadingIndicator(
                            state = pullToRefreshState,
                            isRefreshing = isFetching,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    } else {
                        PullToRefreshDefaults.Indicator(
                            state = pullToRefreshState,
                            isRefreshing = isFetching,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mode switcher: Live 10-Digit IRCTC PNR vs Manual Ticket entry
                    val pnrEntryModes = remember { listOf("Live 10-Digit PNR", "Manual Ticket") }
                    val isManualModeSelected = snapshot?.isManualEntry == true
                    ConnectedButtonGroup(
                        options = pnrEntryModes,
                        selectedIndex = if (isManualModeSelected) 1 else 0,
                        onSelect = { index, _ ->
                            if (isSubmitting) return@ConnectedButtonGroup
                            if (index == 1 && !isManualModeSelected) {
                                val manualPnr = pnrInput.filter { it.isDigit() }.take(10)
                                controller.pnrInput = manualPnr
                                controller.liveSnapshot = com.splitmate.app.data.PnrNetworkRepository.buildUnverifiedManualFallbackSnapshot(
                                    cleanPnr = manualPnr
                                )
                            } else if (index == 0 && isManualModeSelected) {
                                val cleanDigits = pnrInput.filter { it.isDigit() }.take(10)
                                controller.liveSnapshot = null
                                if (cleanDigits.length == 10) {
                                    triggerLivePnrLookup(cleanDigits, isExplicitManualRefresh = false)
                                }
                            }
                        },
                        labelProvider = { it },
                        iconProvider = { _, isChecked -> if (isChecked) Icons.Rounded.Check else null }
                    )

                    // 1. PNR SEARCH / LOOKUP BAR (Starts empty, auto-fetches on 10 digits, rate-limit shielded)
                    PnrSearchLookupCard(
                        pnrValue = pnrInput,
                        onPnrChange = { newValue ->
                            if (!isSubmitting) {
                                controller.onPnrDigitsChanged(
                                    rawValue = newValue,
                                    groupMembersProvider = { latestGroupMembers },
                                    existingExpenseProvider = ::findExistingTrainExpenseInActiveGroup,
                                    existingSplitsProvider = ::existingSplitsForExpense,
                                    autoTriggerWhen10Digits = true
                                )
                            }
                        },
                        isFetching = isFetching,
                        hasLoadedSnapshot = snapshot != null,
                        isFinalConfirmed = isTicketFinalConfirmed,
                        cooldownRemainingSeconds = cooldownRemainingSec,
                        isControlsLocked = isSubmitting,
                        onFetchClick = { triggerLivePnrLookup(pnrInput, isExplicitManualRefresh = true) },
                        onPasteSmsClick = { if (!isSubmitting) handlePasteIrctcSmsFromClipboard() },
                        onImportPdfClick = { if (!isSubmitting) irctcPdfLauncher.launch("application/pdf") }
                    )

                    // Show already-logged PNRs in this group (HorizontalUncontainedCarousel when >= 2, clean single item when == 1)
                    if (existingPnrExpensesInGroup.isNotEmpty()) {
                        fun onSelectLoggedTrainExpense(exp: com.splitmate.app.data.ExpenseEntity, extractedPnr: String) {
                            if (isSubmitting) return
                            if (extractedPnr.length == 10) {
                                controller.onPnrDigitsChanged(
                                    rawValue = extractedPnr,
                                    groupMembersProvider = { latestGroupMembers },
                                    existingExpenseProvider = ::findExistingTrainExpenseInActiveGroup,
                                    existingSplitsProvider = ::existingSplitsForExpense,
                                    autoTriggerWhen10Digits = false
                                )
                                triggerLivePnrLookup(extractedPnr, isExplicitManualRefresh = false)
                            } else {
                                val parsed = com.splitmate.app.ui.extractTravelTicketFromTitle(exp.title)
                                    ?: ParsedTravelTicket(cleanTitle = exp.title)
                                val snap = com.splitmate.app.data.PnrNetworkRepository.buildUnverifiedManualFallbackSnapshot(
                                    cleanPnr = "",
                                    fallbackTicket = parsed
                                ).copy(
                                    baseFarePaiseOverride = exp.totalAmountCents,
                                    totalPayablePaiseOverride = exp.totalAmountCents,
                                    irctcPaymentMode = "NONE",
                                    includeInsurance = false
                                )
                                controller.applyImportedSnapshot(
                                    imported = snap,
                                    groupMembers = latestGroupMembers,
                                    existingExpenseInGroup = exp,
                                    existingSplitsInGroup = existingSplitsForExpense(exp)
                                )
                                controller.manualFromStationInput = parsed.fromStation
                                controller.manualToStationInput = parsed.toStation
                                controller.manualDepartureDateInput = parsed.departureDate
                                controller.manualDepartureTimeInput = parsed.departureTime
                            }
                        }

                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = TactilePaperPassTokens.PaperSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.HairlineBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Train tickets in ${activeGroup?.name ?: "this trip"} (${existingPnrExpensesInGroup.size}) · tap to open or edit",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TactilePaperPassTokens.SageConfirmedText
                                )
                                if (existingPnrExpensesInGroup.size >= 2) {
                                    val loggedTicketsCarouselState = key(existingPnrExpensesInGroup.size) {
                                        rememberCarouselState { existingPnrExpensesInGroup.size }
                                    }
                                    HorizontalUncontainedCarousel(
                                        state = loggedTicketsCarouselState,
                                        itemWidth = 264.dp,
                                        itemSpacing = 8.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { index ->
                                        val exp = existingPnrExpensesInGroup.getOrNull(index) ?: return@HorizontalUncontainedCarousel
                                        val extractedPnr = exp.travelPnr.filter { it.isDigit() }.take(10).ifBlank {
                                            Regex("""PNR:\s*(\d{10})""").find(exp.title)?.groupValues?.getOrNull(1).orEmpty()
                                        }
                                        val isActiveTicket = selectedExistingExpense?.expenseId == exp.expenseId
                                        LoggedPnrTicketStubCard(
                                            expenseTitle = exp.title,
                                            extractedPnr = extractedPnr,
                                            formattedAmount = formatPaiseDisplay(exp.totalAmountCents),
                                            isActiveTicket = isActiveTicket,
                                            onClick = { onSelectLoggedTrainExpense(exp, extractedPnr) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .maskClip(MaterialTheme.shapes.medium)
                                                .heightIn(min = 48.dp)
                                        )
                                    }
                                } else {
                                    val exp = existingPnrExpensesInGroup.first()
                                    val extractedPnr = exp.travelPnr.filter { it.isDigit() }.take(10).ifBlank {
                                        Regex("""PNR:\s*(\d{10})""").find(exp.title)?.groupValues?.getOrNull(1).orEmpty()
                                    }
                                    val isActiveTicket = selectedExistingExpense?.expenseId == exp.expenseId
                                    LoggedPnrTicketStubCard(
                                        expenseTitle = exp.title,
                                        extractedPnr = extractedPnr,
                                        formattedAmount = formatPaiseDisplay(exp.totalAmountCents),
                                        isActiveTicket = isActiveTicket,
                                        onClick = { onSelectLoggedTrainExpense(exp, extractedPnr) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.medium)
                                            .heightIn(min = 48.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (fetchError != null) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = TactilePaperPassTokens.TerracottaWaitlistBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.TerracottaWaitlistBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = fetchError,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = TactilePaperPassTokens.TerracottaWaitlistText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    if (selectedExistingExpense != null && snapshot != null) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = TactilePaperPassTokens.SageConfirmedBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.SageConfirmedBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val pnrDesc = snapshot.pnr.takeIf { it.isNotBlank() }?.let { "PNR $it" } ?: "train ticket"
                            Text(
                                text = "Inspecting logged $pnrDesc (${formatPaiseDisplay(selectedExistingExpense.totalAmountCents)}). You can adjust passengers, IRCTC bill mode, or who paid below and tap 'Update Split & Save Changes'.",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = TactilePaperPassTokens.SageConfirmedText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // 2. EMPTY STATE WHEN NO PNR IS ENTERED YET vs LIVE BOARDING PASS WHEN FETCHED
                    if (snapshot == null) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = TactilePaperPassTokens.PaperSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.HairlineBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(TactilePaperPassTokens.SageConfirmedBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Train,
                                        contentDescription = null,
                                        tint = TactilePaperPassTokens.SageConfirmedText,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "Enter Your 10-Digit IRCTC PNR Above",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TactilePaperPassTokens.InkPrimary
                                )
                                Text(
                                    text = "Nothing is pre-filled or hardcoded. Type your 10-digit PNR number, paste your IRCTC SMS, or import your ERS PDF above to load live train route, passenger berths, and exact IRCTC bill.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    color = TactilePaperPassTokens.InkSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                FilledTonalButton(
                                    onClick = {
                                        // Zero-Placeholder Mandate (#3): never fabricate trailing '0' digits!
                                        val manualPnr = pnrInput.filter { it.isDigit() }.take(10)
                                        controller.pnrInput = manualPnr
                                        controller.liveSnapshot = com.splitmate.app.data.PnrNetworkRepository.buildUnverifiedManualFallbackSnapshot(
                                            cleanPnr = manualPnr
                                        )
                                    },
                                    shapes = ButtonDefaults.shapes(),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = TactilePaperPassTokens.AmberChartBg,
                                        contentColor = TactilePaperPassTokens.AmberChartText
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.AmberChartBorder),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Enter Ticket Details Manually",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = FigtreeFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = TactilePaperPassTokens.AmberChartText
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                            contentDescription = null,
                                            tint = TactilePaperPassTokens.AmberChartText,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // R-08: Split strictly on "•" or "·" when departureDate is not already populated (never split "15 Mar 2026" on spaces!)
                        val depBulletParts = snapshot.departureTime.split("•", "·").map { it.trim() }.filter { it.isNotBlank() }
                        val defaultDepDateDisplay = snapshot.departureDate.ifBlank {
                            if (depBulletParts.size >= 2) depBulletParts.first() else ""
                        }
                        val defaultDepTimeDisplay = if (snapshot.departureDate.isNotBlank()) {
                            if (depBulletParts.size >= 2) depBulletParts.last() else snapshot.departureTime
                        } else {
                            if (depBulletParts.size >= 2) depBulletParts.last() else snapshot.departureTime
                        }
                        val depDateDisplay = manualDepartureDateInput.trim().ifBlank { defaultDepDateDisplay }
                        val depTimeDisplay = manualDepartureTimeInput.trim().ifBlank { defaultDepTimeDisplay }

                        // Manual Ticket Fare, Route & Departure Date/Time Card when CRIS is offline / unverified or fare is 0
                        if (snapshot.isManualEntry || !snapshot.isLiveVerified || effectiveTotalPaise <= 0L || resolvedFromCode.isBlank() || resolvedToCode.isBlank()) {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = TactilePaperPassTokens.PaperSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.AmberChartBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Enter fare, route and departure",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FigtreeFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TactilePaperPassTokens.AmberChartText
                                    )
                                    OutlinedTextField(
                                        value = manualTotalFareInput,
                                        onValueChange = { input ->
                                            if (!isSubmitting) {
                                                controller.manualTotalFareInput = com.splitmate.app.ui.sanitizeDecimalRupeesInput(input)
                                            }
                                        },
                                        enabled = !isSubmitting,
                                        label = { Text("Total Ticket Fare (₹)", fontFamily = FigtreeFontFamily) },
                                        placeholder = { Text("e.g. 2450.00", fontFamily = FigtreeFontFamily) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        colors = TactilePaperPassTokens.tactileTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = manualFromStationInput,
                                            onValueChange = {
                                                if (!isSubmitting) {
                                                    controller.manualFromStationInput = it.uppercase(Locale.US).take(24)
                                                }
                                            },
                                            enabled = !isSubmitting,
                                            label = { Text("From Station", fontFamily = FigtreeFontFamily) },
                                            placeholder = { Text("BDTS / NDLS", fontFamily = FigtreeFontFamily) },
                                            singleLine = true,
                                            colors = TactilePaperPassTokens.tactileTextFieldColors(),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = manualToStationInput,
                                            onValueChange = {
                                                if (!isSubmitting) {
                                                    controller.manualToStationInput = it.uppercase(Locale.US).take(24)
                                                }
                                            },
                                            enabled = !isSubmitting,
                                            label = { Text("To Station", fontFamily = FigtreeFontFamily) },
                                            placeholder = { Text("CDG / MMCT", fontFamily = FigtreeFontFamily) },
                                            singleLine = true,
                                            colors = TactilePaperPassTokens.tactileTextFieldColors(),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = manualDepartureDateInput.ifBlank { defaultDepDateDisplay },
                                            onValueChange = {
                                                if (!isSubmitting) {
                                                    controller.manualDepartureDateInput = it.take(24)
                                                }
                                            },
                                            enabled = !isSubmitting,
                                            label = { Text("Departure Date", style = MaterialTheme.typography.bodySmall, fontFamily = FigtreeFontFamily) },
                                            placeholder = { Text("e.g. 15 Nov 2026", style = MaterialTheme.typography.bodySmall, fontFamily = FigtreeFontFamily) },
                                            trailingIcon = {
                                                ExpressiveTooltipIconButton(
                                                    tooltipText = "Pick departure date",
                                                    onClick = { if (!isSubmitting) showManualDatePicker = true }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.CalendarMonth,
                                                        contentDescription = "Pick departure date",
                                                        tint = TactilePaperPassTokens.SageConfirmedText
                                                    )
                                                }
                                            },
                                            singleLine = true,
                                            colors = TactilePaperPassTokens.tactileTextFieldColors(),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = manualDepartureTimeInput.ifBlank { defaultDepTimeDisplay },
                                            onValueChange = {
                                                if (!isSubmitting) {
                                                    controller.manualDepartureTimeInput = it.take(16)
                                                }
                                            },
                                            enabled = !isSubmitting,
                                            label = { Text("Departure Time", style = MaterialTheme.typography.bodySmall, fontFamily = FigtreeFontFamily) },
                                            placeholder = { Text("e.g. 11:30", style = MaterialTheme.typography.bodySmall, fontFamily = FigtreeFontFamily) },
                                            trailingIcon = {
                                                ExpressiveTooltipIconButton(
                                                    tooltipText = "Pick departure time",
                                                    onClick = { if (!isSubmitting) showManualTimePicker = true }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Schedule,
                                                        contentDescription = "Pick departure time",
                                                        tint = TactilePaperPassTokens.SageConfirmedText
                                                    )
                                                }
                                            },
                                            singleLine = true,
                                            colors = TactilePaperPassTokens.tactileTextFieldColors(),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        val effectiveFromCode = resolvedFromCode.ifBlank { "—" }
                        val effectiveToCode = resolvedToCode.ifBlank { "—" }
                        val effectiveFromName = when {
                            resolvedFromCode.isBlank() -> "Enter origin above"
                            !resolvedFromCode.equals(snapshot.fromStation, ignoreCase = true) || snapshot.fromStationName.isBlank() ->
                                com.splitmate.app.ui.resolveStationDisplayName(resolvedFromCode)
                            else -> snapshot.fromStationName
                        }
                        val effectiveToName = when {
                            resolvedToCode.isBlank() -> "Enter destination above"
                            !resolvedToCode.equals(snapshot.toStation, ignoreCase = true) || snapshot.toStationName.isBlank() ->
                                com.splitmate.app.ui.resolveStationDisplayName(resolvedToCode)
                            else -> snapshot.toStationName
                        }
                        val formattedRailClass = com.splitmate.app.ui.formatIndianRailClassLabel(snapshot.travelClass)
                        val quotaLabel = when (snapshot.quotaText.trim().uppercase(Locale.US)) {
                            "TQ", "TATKAL" -> "Tatkal"
                            "PT" -> "Premium Tatkal"
                            "LD" -> "Ladies Quota"
                            "GN", "GENERAL" -> "General Quota"
                            else -> "IRCTC"
                        }
                        val travelClassCaption = listOfNotNull(
                            formattedRailClass.takeIf { it.isNotBlank() },
                            "$quotaLabel E-Ticket"
                        ).joinToString(" · ")
                        val upiFeePreviewDisplay = formatPaiseDisplay(billReferenceSnapshot?.irctcConvenienceFeeUpiPaise ?: 0L)
                        val cardFeePreviewDisplay = formatPaiseDisplay(billReferenceSnapshot?.irctcConvenienceFeeCardPaise ?: 0L)
                        val insFeePreviewDisplay = formatPaiseDisplay(billReferenceSnapshot?.travelInsurancePaise ?: 0L)

                        // LIVE TACTILE PAPER BOARDING PASS
                        TactilePaperBoardingPass(
                            pnrNumber = snapshot.pnr,
                            trainNumber = snapshot.trainNo.ifBlank { "Train" },
                            trainName = snapshot.trainName.ifBlank {
                                if (snapshot.isLiveVerified) "Indian Railways" else "Manual Ticket"
                            }
                                .lowercase(Locale.US)
                                .split(" ")
                                .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } },
                            travelClass = travelClassCaption,
                            chartStatus = when {
                                snapshot.isInvalidOrFlushedPnr -> "Flushed / Expired"
                                !snapshot.isLiveVerified -> "Manual Entry"
                                snapshot.chartPrepared -> "Chart Prepared"
                                else -> "Chart Not Prepared"
                            },
                            originCode = effectiveFromCode,
                            originName = effectiveFromName,
                            departureTime = depTimeDisplay,
                            departureDate = depDateDisplay,
                            destinationCode = effectiveToCode,
                            destinationName = effectiveToName,
                            arrivalTime = snapshot.arrivalTime,
                            arrivalDate = snapshot.arrivalDate,
                            duration = snapshot.durationText,
                            totalFareDisplay = formatPaiseDisplay(effectiveTotalPaise),
                            baseFareDisplay = formatPaiseDisplay(baseFarePaise),
                            convenienceFeeDisplay = formatPaiseDisplay(convenienceFeePaise),
                            insuranceFeeDisplay = formatPaiseDisplay(insuranceFeePaise),
                            perPersonShareDisplay = perPersonEachChipLabel,
                            passengerCount = ticketPassengerCount,
                            passengers = passengerRows,
                            splitNatureCaption = when (reviewSplitNature) {
                                com.splitmate.app.ExpenseSplitNature.PERSONAL -> com.splitmate.app.ExpenseSplitCopy.PERSONAL_NOT_SPLIT
                                com.splitmate.app.ExpenseSplitNature.ON_BEHALF -> "Paid for $reviewSoleLabel"
                                com.splitmate.app.ExpenseSplitNature.SHARED -> null
                            },
                            showIrctcBillControls = baseFarePaise > 0L,
                            selectedPaymentMode = selectedIrctcPaymentMode,
                            includeInsurance = includeIrctcInsurance,
                            upiFeeChipLabel = "UPI +$upiFeePreviewDisplay",
                            cardFeeChipLabel = "Card +$cardFeePreviewDisplay",
                            insuranceChipLabel = "Insurance +$insFeePreviewDisplay",
                            onSelectPaymentMode = { mode ->
                                if (!isSubmitting) {
                                    hasUserAdjustedIrctcFeeControls = true
                                    selectedIrctcPaymentMode = mode
                                }
                            },
                            onToggleInsurance = {
                                if (!isSubmitting) {
                                    hasUserAdjustedIrctcFeeControls = true
                                    includeIrctcInsurance = !includeIrctcInsurance
                                }
                            }
                        )

                        // 3. INTERACTIVE MEMBER SELECTION CARD: ASK WHICH MEMBERS FROM THE GROUP ARE ON THIS TICKET
                        MemberSplitSelectionCard(
                            groupName = activeGroup?.name ?: "Group",
                            ticketPassengerCount = ticketPassengerCount,
                            members = groupMembers,
                            selectedMemberIds = selectedMemberIds,
                            selectedPayerId = selectedPayerId,
                            onSelectPayer = { newPayerId ->
                                if (!isSubmitting) controller.onSelectPayer(newPayerId)
                            },
                            exactAllocationsMap = exactAllocationsMap,
                            formatPaise = ::formatPaiseDisplay,
                            onToggleMember = { memberId ->
                                if (!isSubmitting) controller.onToggleMember(memberId)
                            },
                            onSelectExactTicketCount = {
                                if (!isSubmitting) controller.onSelectExactTicketCount(groupMembers, ticketPassengerCount)
                            },
                            onAddMemberToGroup = { newFriendName ->
                                if (!isSubmitting) viewModel.addMemberToActiveGroup(newFriendName)
                            },
                            totalFareDisplay = formatPaiseDisplay(effectiveTotalPaise),
                            perMemberShareDisplay = perPersonHeadlineCaption,
                            payerReimbursementDisplay = formatPaiseDisplay(
                                (effectiveTotalPaise - payerSharePaise).coerceAtLeast(0L)
                            ),
                            effectiveTotalPaise = effectiveTotalPaise
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            val stampLabelPnr = (liveSnapshot?.pnr ?: pnrInput).takeIf { it.isNotBlank() }?.let { "PNR $it · " }.orEmpty()
            BoardingPassCommitStampOverlay(
                tearProgress = pnrTearProgress.value,
                stampScale = pnrStampScale.value,
                stampAlpha = pnrStampAlpha.value,
                accentColor = TactilePaperPassTokens.ForestTop,
                stampSubLabel = "IRCTC ${stampLabelPnr}Split Saved"
            )
        }
    }

    if (showManualDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showManualDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val formattedDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))
                            controller.manualDepartureDateInput = formattedDate
                        }
                        showManualDatePicker = false
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showManualDatePicker = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = true
            )
        }
    }

    if (showManualTimePicker) {
        val existingTime = manualDepartureTimeInput.trim().ifBlank { snapshot?.departureTime.orEmpty() }
        val parsedHourMin = remember(existingTime) {
            val match = Regex("""(\d{1,2}):(\d{2})""").find(existingTime)
            val h = match?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
            val m = match?.groupValues?.getOrNull(2)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
            h to m
        }
        val timePickerState = rememberTimePickerState(
            initialHour = parsedHourMin.first,
            initialMinute = parsedHourMin.second,
            is24Hour = true
        )
        var timeDisplayMode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }
        TimePickerDialog(
            onDismissRequest = { showManualTimePicker = false },
            title = {
                Text(
                    text = "Select departure time",
                    style = MaterialTheme.typography.labelLarge
                )
            },
            modeToggleButton = {
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = {
                        timeDisplayMode = if (timeDisplayMode == TimePickerDisplayMode.Picker) {
                            TimePickerDisplayMode.Input
                        } else {
                            TimePickerDisplayMode.Picker
                        }
                    },
                    displayMode = timeDisplayMode
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        controller.manualDepartureTimeInput = String.format(
                            Locale.US,
                            "%02d:%02d",
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        showManualTimePicker = false
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showManualTimePicker = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("Cancel")
                }
            }
        ) {
            if (timeDisplayMode == TimePickerDisplayMode.Picker) {
                TimePicker(state = timePickerState)
            } else {
                TimeInput(state = timePickerState)
            }
        }
    }
}

@Composable
private fun LoggedPnrTicketStubCard(
    expenseTitle: String,
    extractedPnr: String,
    formattedAmount: String,
    isActiveTicket: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val labelPrefix = if (extractedPnr.length == 10) {
        "PNR $extractedPnr"
    } else {
        com.splitmate.app.ui.cleanDisplayExpenseTitle(expenseTitle)
    }
    val actionHint = if (isActiveTicket) "Editing below" else "Tap to view pass"
    val fullA11yLabel = "$labelPrefix, $formattedAmount, $actionHint"

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isActiveTicket) TactilePaperPassTokens.SageConfirmedBg else TactilePaperPassTokens.PaperStubSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isActiveTicket) TactilePaperPassTokens.SageConfirmedBorder else TactilePaperPassTokens.HairlineBorder
        ),
        modifier = modifier.clearAndSetSemantics {
            contentDescription = fullA11yLabel
            role = Role.Button
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = if (isActiveTicket) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.InkSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$labelPrefix · $formattedAmount",
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    fontFamily = FigtreeFontFamily,
                    fontWeight = if (isActiveTicket) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isActiveTicket) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = actionHint,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = if (isActiveTicket) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.InkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ============================================================================
// 5. COMPONENT: PNR SEARCH INPUT BAR + ANTI-BAN COOLDOWN + SMS/PDF COMPANIONS
// ============================================================================
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PnrSearchLookupCard(
    pnrValue: String,
    onPnrChange: (String) -> Unit,
    isFetching: Boolean = false,
    hasLoadedSnapshot: Boolean = false,
    isFinalConfirmed: Boolean = false,
    cooldownRemainingSeconds: Int = 0,
    isControlsLocked: Boolean = false,
    onFetchClick: () -> Unit,
    onPasteSmsClick: () -> Unit = {},
    onImportPdfClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val isCooldownBlocked = hasLoadedSnapshot && cooldownRemainingSeconds > 0 && !isFinalConfirmed
    val isButtonEnabled = !isFetching && !isControlsLocked && !isCooldownBlocked && !(hasLoadedSnapshot && isFinalConfirmed)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(TactilePaperPassTokens.SageConfirmedBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ConfirmationNumber,
                            contentDescription = null,
                            tint = TactilePaperPassTokens.SageConfirmedText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "10-digit IRCTC PNR",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = TactilePaperPassTokens.InkMuted
                        )
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (pnrValue.isEmpty()) {
                                Text(
                                    text = "Tap here to enter 10-digit PNR...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TactilePaperPassTokens.InkMuted
                                )
                            }
                            BasicTextField(
                                value = pnrValue,
                                onValueChange = onPnrChange,
                                enabled = !isControlsLocked,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.4.sp,
                                    fontFeatureSettings = "tnum",
                                    color = TactilePaperPassTokens.InkPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "10-digit IRCTC PNR" }
                            )
                        }
                    }
                }

                // v2.4.0 rc3: official Button with the shapes() press morph + Layer-1 rage-tap lock + 48dp touch target.
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onFetchClick()
                    },
                    enabled = isButtonEnabled,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TactilePaperPassTokens.ForestTop,
                        contentColor = TactilePaperPassTokens.ForestAccent,
                        disabledContainerColor = TactilePaperPassTokens.DisabledSlab,
                        disabledContentColor = TactilePaperPassTokens.InkSecondary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .heightIn(min = 48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            isFetching -> {
                                ContainedLoadingIndicator(
                                    containerSize = 20.dp,
                                    containerColor = TactilePaperPassTokens.ForestBadgeFill,
                                    indicatorColor = TactilePaperPassTokens.ForestAccent
                                )
                            }
                            hasLoadedSnapshot && isFinalConfirmed -> {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Confirmed",
                                    tint = TactilePaperPassTokens.InkSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            isCooldownBlocked -> {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = "Refresh Cooldown",
                                    tint = TactilePaperPassTokens.InkSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            hasLoadedSnapshot -> {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    contentDescription = "Refresh Live PNR",
                                    tint = TactilePaperPassTokens.ForestAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Fetch Live PNR",
                                    tint = TactilePaperPassTokens.ForestAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = when {
                                isFetching -> "Fetching..."
                                hasLoadedSnapshot && isFinalConfirmed -> "Final CNF"
                                isCooldownBlocked -> "Wait ${cooldownRemainingSeconds}s"
                                hasLoadedSnapshot -> "Refresh"
                                else -> "Fetch PNR"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (isButtonEnabled) TactilePaperPassTokens.OnForest else TactilePaperPassTokens.InkSecondary
                        )
                    }
                }
            }

            // Network-only wait indicator (>= 400ms min visibility, hidden on cached/static state)
            InFlightWavyProgressIndicator(
                inFlight = isFetching,
                color = TactilePaperPassTokens.SageConfirmedText,
                trackColor = TactilePaperPassTokens.SageConfirmedBg,
                contentDescription = "Fetching live IRCTC PNR status",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            )

            if (isCooldownBlocked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = TactilePaperPassTokens.SageConfirmedText,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Updated just now · Next live check in ${cooldownRemainingSeconds}s",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = TactilePaperPassTokens.SageConfirmedText
                    )
                }
            }

            // Zero-permission on-device companions: Paste IRCTC SMS or Import IRCTC ERS PDF
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onPasteSmsClick,
                    enabled = !isControlsLocked && !isFetching,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    label = {
                        Text(
                            text = "Paste IRCTC SMS",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                )
                AssistChip(
                    onClick = onImportPdfClick,
                    enabled = !isControlsLocked && !isFetching,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    label = {
                        Text(
                            text = "Import Ticket PDF",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                )
            }
        }
    }
}

// ============================================================================
// 6. HERO COMPONENT: SKEUOMORPHIC PAPER BOARDING PASS
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TactilePaperBoardingPass(
    pnrNumber: String,
    trainNumber: String,
    trainName: String,
    travelClass: String,
    chartStatus: String,
    originCode: String,
    originName: String,
    departureTime: String,
    departureDate: String,
    destinationCode: String,
    destinationName: String,
    arrivalTime: String,
    arrivalDate: String,
    duration: String,
    totalFareDisplay: String,
    baseFareDisplay: String,
    convenienceFeeDisplay: String,
    insuranceFeeDisplay: String,
    perPersonShareDisplay: String,
    passengerCount: Int,
    passengers: List<PassengerTicketRow>,
    // v2.3.3: non-null for PERSONAL / ON_BEHALF tickets; replaces per-person share captions.
    splitNatureCaption: String? = null,
    showIrctcBillControls: Boolean = false,
    selectedPaymentMode: String = "UPI",
    includeInsurance: Boolean = true,
    upiFeeChipLabel: String = "UPI",
    cardFeeChipLabel: String = "Card",
    insuranceChipLabel: String = "Insurance",
    onSelectPaymentMode: (String) -> Unit = {},
    onToggleInsurance: () -> Unit = {}
) {
    val ticketShape = remember { TactilePaperPerforatedShape(perforationRatio = 0.76f) }
    val isChartPrepared = chartStatus.lowercase(Locale.US).let {
        it.contains("prepared") && !it.contains("not")
    }

    Surface(
        shape = ticketShape,
        color = TactilePaperPassTokens.PaperSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.HairlineBorder),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // SECTION A: FOREST BOTANICAL HEADER + ROUTE STRIP
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                TactilePaperPassTokens.ForestTop,
                                TactilePaperPassTokens.ForestBottom
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TactilePaperPassTokens.ForestBadgeFill,
                            border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.ForestBadgeStroke)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Train,
                                    contentDescription = null,
                                    tint = TactilePaperPassTokens.ForestAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$trainNumber · $trainName",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TactilePaperPassTokens.OnForest,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = travelClass,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = TactilePaperPassTokens.ForestSubtle
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isChartPrepared) TactilePaperPassTokens.SageConfirmedBg else TactilePaperPassTokens.AmberChartBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isChartPrepared) TactilePaperPassTokens.SageConfirmedBorder else TactilePaperPassTokens.AmberChartBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isChartPrepared) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.AmberChartText
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = chartStatus,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isChartPrepared) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.AmberChartText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = originCode.trim().substringBefore(" ").take(5),
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 30.sp,
                            letterSpacing = (-0.5).sp,
                            color = TactilePaperPassTokens.OnForest,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = originName,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = TactilePaperPassTokens.ForestAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (departureTime.isNotBlank() || departureDate.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = listOf(departureTime, departureDate).filter { it.isNotBlank() }.joinToString(" · "),
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = TactilePaperPassTokens.ForestMuted
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        if (duration.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = TactilePaperPassTokens.ForestBadgeFill
                            ) {
                                Text(
                                    text = duration,
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = TactilePaperPassTokens.ForestAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        val routeLineColor = TactilePaperPassTokens.ForestRouteLine
                        val routeStopColor = TactilePaperPassTokens.ForestAccent
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                        ) {
                            val yCenter = size.height / 2f
                            drawLine(
                                color = routeLineColor,
                                start = Offset(8f, yCenter),
                                end = Offset(size.width - 8f, yCenter),
                                strokeWidth = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                            drawCircle(
                                color = routeStopColor,
                                radius = 4.dp.toPx(),
                                center = Offset(4.dp.toPx(), yCenter)
                            )
                            drawCircle(
                                color = routeStopColor,
                                radius = 4.dp.toPx(),
                                center = Offset(size.width - 4.dp.toPx(), yCenter)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = destinationCode.trim().substringBefore(" ").take(5),
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 30.sp,
                            letterSpacing = (-0.5).sp,
                            color = TactilePaperPassTokens.OnForest,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = destinationName,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = TactilePaperPassTokens.ForestAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (arrivalTime.isNotBlank() || arrivalDate.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = listOf(arrivalTime, arrivalDate).filter { it.isNotBlank() }.joinToString(" · "),
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = TactilePaperPassTokens.ForestMuted
                            )
                        }
                    }
                }
            }

            // SECTION B: PASSENGER MANIFEST & LIVE STATUS TABLE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Passengers on this ticket (${passengers.size})",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.1.sp,
                        color = TactilePaperPassTokens.InkMuted
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Booking",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.1.sp,
                            color = TactilePaperPassTokens.InkMuted
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = TactilePaperPassTokens.InkMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Live status",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.1.sp,
                            color = TactilePaperPassTokens.InkMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                passengers.forEachIndexed { index, pax ->
                    val paxIslandShape = segmentedIslandItemShape(
                        index = index,
                        totalCount = passengers.size,
                        isSelected = pax.isConfirmed
                    )
                    Surface(
                        shape = paxIslandShape,
                        color = TactilePaperPassTokens.PaperStubSurface.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            TactilePaperPassTokens.HairlineBorder.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                com.splitmate.app.AvatarToken(
                                    initials = pax.name,
                                    bg = if (pax.isPayer) TactilePaperPassTokens.SageConfirmedBg else TactilePaperPassTokens.NeutralChip,
                                    textColor = if (pax.isPayer) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.InkSecondary,
                                    size = 34
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (pax.isPayer) TactilePaperPassTokens.SageConfirmedBg else TactilePaperPassTokens.NeutralChip,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (pax.isPayer) TactilePaperPassTokens.SageConfirmedBorder else TactilePaperPassTokens.HairlineBorder
                                    )
                                ) {
                                    Text(
                                        text = pax.id,
                                        fontFamily = FigtreeFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = if (pax.isPayer) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.InkSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // v2.3.6 fix: the name column takes the remaining width (weight) and
                                // wraps/ellipsizes by word, so long statuses can't crush it into
                                // one letter per line.
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pax.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TactilePaperPassTokens.InkPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = splitNatureCaption
                                            ?: pax.exactShareDisplay?.let { "Exact Share: $it" }
                                            ?: "Share: $perPersonShareDisplay",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        fontWeight = FontWeight.SemiBold,
                                        color = TactilePaperPassTokens.SageConfirmedText,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Status side is capped so it can never take the name's space.
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.widthIn(max = 148.dp)
                            ) {
                                Text(
                                    text = pax.bookingStatus,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    textDecoration = if (pax.bookingStatus != pax.currentStatus) TextDecoration.LineThrough else TextDecoration.None,
                                    color = TactilePaperPassTokens.InkMuted
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (pax.isConfirmed) TactilePaperPassTokens.SageConfirmedBg else TactilePaperPassTokens.TerracottaWaitlistBg,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (pax.isConfirmed) TactilePaperPassTokens.SageConfirmedBorder else TactilePaperPassTokens.TerracottaWaitlistBorder
                                    )
                                ) {
                                    val statusColor = if (pax.isConfirmed) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.TerracottaWaitlistText
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = pax.currentStatus,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = statusColor,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (index < passengers.lastIndex) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }

            // SECTION C: PHYSICAL PERFORATED TEAR-OFF LINE
            val perforationLineColor = TactilePaperPassTokens.PerforationLine
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            ) {
                val yCenter = size.height / 2f
                drawLine(
                    color = perforationLineColor,
                    start = Offset(18.dp.toPx(), yCenter),
                    end = Offset(size.width - 18.dp.toPx(), yCenter),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                )
            }

            // SECTION D: BOTTOM TICKET STUB (TOTAL ALL-INCLUSIVE FARE, IRCTC BILL ENGINE & BARCODE)
            // v2.3.2: static fare data carries no progress bar; hierarchy comes from type + spacing.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TactilePaperPassTokens.PaperStubSurface)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total fare ($passengerCount ${if (passengerCount == 1) "passenger" else "passengers"})",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.1.sp,
                        color = TactilePaperPassTokens.InkMuted,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp, top = 2.dp)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        TactileBarcode(
                            modifier = Modifier
                                .width(88.dp)
                                .height(26.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val formattedPnr = when {
                            pnrNumber.length == 10 -> "${pnrNumber.substring(0, 4)}-${pnrNumber.substring(4, 7)}-${pnrNumber.substring(7, 10)}"
                            pnrNumber.isNotBlank() -> pnrNumber
                            else -> "Manual Entry"
                        }
                        Text(
                            text = if (pnrNumber.isNotBlank()) "PNR $formattedPnr" else formattedPnr,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.6.sp,
                            color = TactilePaperPassTokens.InkSecondary
                        )
                    }
                }

                // Editorial hero total (displaySmall, auto-fits a single line).
                EditorialFinancialTotalText(
                    text = totalFareDisplay,
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.Black,
                    color = TactilePaperPassTokens.InkPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TactilePaperPassTokens.SageConfirmedBg
                    ) {
                        Text(
                            text = splitNatureCaption ?: perPersonShareDisplay,
                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = TactilePaperPassTokens.SageConfirmedText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Base $baseFareDisplay + IRCTC Conv. $convenienceFeeDisplay + Insurance $insuranceFeeDisplay",
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = TactilePaperPassTokens.InkSecondary
                )

                // Restored v1.8.5 Official IRCTC Dynamic Bill Engine (UPI default | Card | Base ₹0 + Insurance)
                if (showIrctcBillControls) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val paymentModes = listOf(
                            "UPI" to upiFeeChipLabel,
                            "CARD" to cardFeeChipLabel,
                            "NONE" to "Base ₹0"
                        )
                        paymentModes.forEach { (modeKey, modeLabel) ->
                            val isModeSelected = selectedPaymentMode.equals(modeKey, ignoreCase = true)
                            FilterChip(
                                selected = isModeSelected,
                                onClick = { onSelectPaymentMode(modeKey) },
                                shapes = FilterChipDefaults.shapes(),
                                modifier = Modifier.minimumInteractiveComponentSize(),
                                label = {
                                    Text(
                                        text = modeLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        fontFamily = FigtreeFontFamily,
                                        fontWeight = if (isModeSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = if (isModeSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TactilePaperPassTokens.SageConfirmedBg,
                                    selectedLabelColor = TactilePaperPassTokens.SageConfirmedText,
                                    selectedLeadingIconColor = TactilePaperPassTokens.SageConfirmedText
                                )
                            )
                        }
                        FilterChip(
                            selected = includeInsurance,
                            onClick = onToggleInsurance,
                            shapes = FilterChipDefaults.shapes(),
                            modifier = Modifier.minimumInteractiveComponentSize(),
                            label = {
                                Text(
                                    text = insuranceChipLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = if (includeInsurance) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            },
                            leadingIcon = if (includeInsurance) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TactilePaperPassTokens.SageConfirmedBg,
                                selectedLabelColor = TactilePaperPassTokens.SageConfirmedText,
                                selectedLeadingIconColor = TactilePaperPassTokens.SageConfirmedText
                            )
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 7. INTERACTIVE GROUP MEMBER SELECTION ("SELECT WHICH PASSENGERS ARE ON THIS TICKET")
// ============================================================================
@Suppress("UNUSED_PARAMETER")
@Composable
private fun MemberSplitSelectionCard(
    groupName: String,
    ticketPassengerCount: Int,
    members: List<GroupMemberEntity>,
    selectedMemberIds: Set<String>,
    selectedPayerId: String,
    onSelectPayer: (String) -> Unit,
    exactAllocationsMap: Map<String, com.splitmate.app.SplitMateMathEngine.SplitAllocation>,
    formatPaise: (Long) -> String,
    onToggleMember: (String) -> Unit,
    onSelectExactTicketCount: () -> Unit,
    onAddMemberToGroup: (String) -> Unit,
    totalFareDisplay: String,
    perMemberShareDisplay: String,
    payerReimbursementDisplay: String,
    effectiveTotalPaise: Long = 0L
) {
    var quickAddName by remember { mutableStateOf("") }
    val payerMemberName = remember(members, selectedPayerId) {
        members.firstOrNull { it.memberId == selectedPayerId }?.let {
            if (it.isCurrentUser) "You (${it.name})" else it.name
        } ?: "you"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = TactilePaperPassTokens.PaperSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.HairlineBorder),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // "Who paid" M3 SingleChoiceSegmentedButtonRow (scrolls horizontally for 4+ members)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Who paid for this ticket?",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.1.sp,
                    color = TactilePaperPassTokens.InkSecondary
                )
                PayerSegmentedButtonRow(
                    members = members,
                    selectedPayerId = selectedPayerId,
                    onSelectPayer = onSelectPayer
                )
            }

            // M3E spacing replaces the old hairline divider between payer and passenger sections.
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Who is on this ticket? (${selectedMemberIds.size} of $ticketPassengerCount selected)",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = TactilePaperPassTokens.InkPrimary
                    )
                    Text(
                        text = "Select the $ticketPassengerCount passengers from $groupName ($perMemberShareDisplay each)",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = TactilePaperPassTokens.InkSecondary
                    )
                }

                val isExactSelectionMatched = if (ticketPassengerCount < members.size) {
                    selectedMemberIds.size == ticketPassengerCount
                } else {
                    members.isNotEmpty() && selectedMemberIds.size == members.size
                }
                val selectionChipText = when {
                    ticketPassengerCount < members.size -> "Select $ticketPassengerCount Pax"
                    isExactSelectionMatched -> "All selected"
                    else -> "Select all"
                }
                FilterChip(
                    selected = isExactSelectionMatched,
                    onClick = onSelectExactTicketCount,
                    shapes = FilterChipDefaults.shapes(),
                    leadingIcon = if (isExactSelectionMatched) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = TactilePaperPassTokens.PaperStubSurface,
                        labelColor = TactilePaperPassTokens.SageConfirmedText,
                        selectedContainerColor = TactilePaperPassTokens.SageConfirmedBg,
                        selectedLabelColor = TactilePaperPassTokens.SageConfirmedText,
                        selectedLeadingIconColor = TactilePaperPassTokens.SageConfirmedText
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.SageConfirmedBorder),
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    label = {
                        Text(
                            text = selectionChipText,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            // If the group currently has fewer members than the ticket's passenger count, let the user add the remaining passengers right here!
            if (members.size < ticketPassengerCount) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = TactilePaperPassTokens.AmberChartBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.AmberChartBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "This PNR has $ticketPassengerCount passengers, but $groupName currently has ${members.size} member(s). Add ${ticketPassengerCount - members.size} more passenger(s) to split $totalFareDisplay ÷ $ticketPassengerCount:",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = TactilePaperPassTokens.AmberChartText
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = quickAddName,
                                onValueChange = { quickAddName = it },
                                label = { Text("Passenger name", style = MaterialTheme.typography.bodySmall, fontFamily = FigtreeFontFamily) },
                                placeholder = { Text("e.g. Rahul", style = MaterialTheme.typography.bodySmall, fontFamily = FigtreeFontFamily) },
                                singleLine = true,
                                colors = TactilePaperPassTokens.tactileTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (quickAddName.isNotBlank()) {
                                        onAddMemberToGroup(quickAddName.trim())
                                        quickAddName = ""
                                    }
                                },
                                shapes = ButtonDefaults.shapes(),
                                colors = ButtonDefaults.buttonColors(containerColor = TactilePaperPassTokens.ForestTop),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", style = MaterialTheme.typography.labelMedium, fontFamily = FigtreeFontFamily, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            val haptic = LocalHapticFeedback.current

            // Member Rows (Tap to toggle inclusion on this Train Ticket)
            members.forEachIndexed { index, member ->
                val isSelected = member.memberId in selectedMemberIds
                val isPayer = member.memberId == selectedPayerId
                val exactMemberCents = exactAllocationsMap[member.memberId]?.finalCents
                val memberExactShareDisplay = if (exactMemberCents != null) formatPaise(exactMemberCents) else perMemberShareDisplay
                val rowShape = rememberAnimatedSegmentedIslandItemShape(
                    index = index,
                    totalCount = members.size,
                    isSelected = isSelected
                )
                val rowBg by animateColorAsState(
                    targetValue = if (isSelected) TactilePaperPassTokens.SageConfirmedBg.copy(alpha = 0.25f) else TactilePaperPassTokens.PaperStubSurface.copy(alpha = 0.6f),
                    animationSpec = SplitMateMotion.defaultEffects(),
                    label = "memberRowBg"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(rowShape)
                        .background(rowBg)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) TactilePaperPassTokens.SageConfirmedBorder else TactilePaperPassTokens.HairlineBorder.copy(alpha = 0.5f),
                            shape = rowShape
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onToggleMember(member.memberId)
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
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
                            Box(modifier = Modifier.size(40.dp)) {
                                com.splitmate.app.AvatarToken(
                                    initials = member.avatarSeed.ifBlank { member.name },
                                    // Unselected disc: surfaceContainer behind SageConfirmedText initials in every theme.
                                    bg = if (isSelected) TactilePaperPassTokens.SageConfirmedBg else TactilePaperPassTokens.NeutralChip,
                                    textColor = TactilePaperPassTokens.SageConfirmedText,
                                    size = 38
                                )
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                !isSelected -> TactilePaperPassTokens.ExcludedBadge
                                                isPayer -> TactilePaperPassTokens.ForestTop
                                                else -> TactilePaperPassTokens.SageConfirmedText
                                            }
                                        )
                                        .border(1.5.dp, TactilePaperPassTokens.PaperSurface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.Close,
                                        contentDescription = if (isSelected) "Selected" else "Excluded",
                                        tint = when {
                                            !isSelected -> TactilePaperPassTokens.ExcludedBadgeIcon
                                            isPayer -> TactilePaperPassTokens.OnForest
                                            else -> TactilePaperPassTokens.SageConfirmedOnText
                                        },
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = if (member.isCurrentUser) "${member.name} (You)" else member.name,
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isSelected) TactilePaperPassTokens.InkPrimary else TactilePaperPassTokens.InkMuted,
                                    textDecoration = if (isSelected) TextDecoration.None else TextDecoration.LineThrough
                                )
                                Text(
                                    text = when {
                                        !isSelected && isPayer -> "Paid full $totalFareDisplay · Not on ticket"
                                        !isSelected -> "Not on this ticket · Excluded"
                                        isPayer -> "Paid full $totalFareDisplay"
                                        else -> "Owes $payerMemberName $memberExactShareDisplay · via UPI"
                                    },
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isSelected || isPayer) TactilePaperPassTokens.InkSecondary else TactilePaperPassTokens.InkMuted
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isSelected) memberExactShareDisplay else "₹0",
                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = if (isSelected) TactilePaperPassTokens.InkPrimary else TactilePaperPassTokens.InkMuted
                            )
                            if (isPayer) {
                                Text(
                                    text = "Getting back $payerReimbursementDisplay",
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = TactilePaperPassTokens.SageConfirmedText
                                )
                            }
                        }
                    }
                }

                if (index < members.lastIndex) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

// ============================================================================
// 7b. "WHO PAID" M3 SINGLE-CHOICE SEGMENTED BUTTON ROW
// ============================================================================
/**
 * M3 `SingleChoiceSegmentedButtonRow` payer picker. With up to 3 members the segments share
 * the full width; with 4+ members each segment keeps a 104.dp minimum and the row scrolls
 * horizontally, with first-name labels truncated by ellipsis so it never wraps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayerSegmentedButtonRow(
    members: List<GroupMemberEntity>,
    selectedPayerId: String,
    onSelectPayer: (String) -> Unit
) {
    if (members.isEmpty()) return
    val minSegmentWidth = 104.dp
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val rowWidth = maxOf(maxWidth, minSegmentWidth * members.size)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.width(rowWidth)) {
                members.forEachIndexed { index, member ->
                    val firstName = member.name.trim().substringBefore(' ').ifBlank { member.name }
                    SegmentedButton(
                        selected = member.memberId == selectedPayerId,
                        onClick = { onSelectPayer(member.memberId) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = members.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = TactilePaperPassTokens.ForestTop,
                            activeContentColor = TactilePaperPassTokens.OnForest,
                            activeBorderColor = TactilePaperPassTokens.ForestTop,
                            inactiveContainerColor = TactilePaperPassTokens.PaperStubSurface,
                            inactiveContentColor = TactilePaperPassTokens.InkSecondary,
                            inactiveBorderColor = TactilePaperPassTokens.HairlineBorder
                        )
                    ) {
                        Text(
                            text = if (member.isCurrentUser) "$firstName (You)" else firstName,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 8. BARCODE GRAPHIC GENERATOR
// ============================================================================
@Composable
fun TactileBarcode(modifier: Modifier = Modifier) {
    val barcodeInk = TactilePaperPassTokens.InkPrimary
    Canvas(modifier = modifier) {
        val barWidths = floatArrayOf(
            3f, 1.5f, 4.5f, 1.5f, 1.5f, 3f, 6f, 1.5f, 3f, 1.5f,
            4.5f, 3f, 1.5f, 1.5f, 4.5f, 1.5f, 3f, 4.5f, 1.5f, 3f
        )
        var currentX = 0f
        val totalPatternWidth = barWidths.sum() + (barWidths.size * 2.5f)
        val scale = size.width / totalPatternWidth

        barWidths.forEach { rawWidth ->
            val w = rawWidth * scale
            if (currentX + w <= size.width) {
                drawRect(
                    color = barcodeInk,
                    topLeft = Offset(currentX, 0f),
                    size = Size(w, size.height)
                )
            }
            currentX += w + (2.5f * scale)
        }
    }
}
