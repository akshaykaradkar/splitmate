package com.splitmate.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.splitmate.app.ui.components.ContainedLoadingIndicator
import com.splitmate.app.ui.components.EditorialFinancialTotalText
import com.splitmate.app.ui.components.SplitMateMotion
import com.splitmate.app.ui.components.rememberAnimatedSegmentedIslandItemShape
import com.splitmate.app.ui.components.segmentedIslandItemShape
import com.splitmate.app.ui.fetchLivePnrAndTrainStatus
import com.splitmate.app.ui.formatTravelExpenseTitle
import com.splitmate.app.ui.theme.activeTransitExtendedColors
import androidx.compose.animation.core.spring
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
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
// 3. DATA MODELS FOR THE UI
// ============================================================================
data class PassengerTicketRow(
    val id: String,
    val memberId: String?,
    val name: String,
    val isPayer: Boolean = false,
    val bookingStatus: String,
    val currentStatus: String,
    val isConfirmed: Boolean = false
)

// ============================================================================
// 4. MAIN SCREEN: PNR SEARCH + BOARDING PASS + MEMBER SPLIT + LEDGER CONFIRM
// ============================================================================
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

    // Start empty unless launched from a specific logged ticket's 10-digit PNR
    var pnrInput by remember(initialPnr) { mutableStateOf(initialPnr.filter { it.isDigit() }.take(10)) }
    var isFetching by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Manual ticket entry overrides when CRIS is unreachable, rate-limited, or entered manually
    var manualTotalFareInput by remember { mutableStateOf("") }
    var manualFromStationInput by remember { mutableStateOf("") }
    var manualToStationInput by remember { mutableStateOf("") }

    var liveSnapshot by remember { mutableStateOf<LivePnrStatusSnapshot?>(null) }

    // Check which 10-digit IRCTC Train PNRs are already logged in the active group (excluding 6-char Flight PNRs)
    val existingPnrExpensesInGroup = remember(uiState.expenses, uiState.activeGroupId) {
        uiState.expenses.filter { exp ->
            exp.groupId == uiState.activeGroupId &&
                exp.title.contains("PNR:", ignoreCase = true) &&
                com.splitmate.app.ui.extractTravelTicketFromTitle(exp.title)?.pnr?.let { pnr ->
                    pnr.length == 10 && pnr.all { ch -> ch.isDigit() }
                } == true
        }
    }

    // Reactive lookup of an existing logged expense matching the active 10-digit PNR
    val selectedExistingExpense = remember(existingPnrExpensesInGroup, uiState.expenses, pnrInput, liveSnapshot) {
        val clean10 = (liveSnapshot?.pnr?.takeIf { it.length == 10 } ?: pnrInput.filter { it.isDigit() }).take(10)
        if (clean10.length == 10) {
            existingPnrExpensesInGroup.firstOrNull { it.title.contains(clean10) }
                ?: viewModel.findExistingExpenseByPnr(clean10, uiState.activeGroupId)?.first
        } else null
    }

    LaunchedEffect(selectedExistingExpense?.groupId) {
        val matchedGroupId = selectedExistingExpense?.groupId
        if (!matchedGroupId.isNullOrBlank() && matchedGroupId != uiState.activeGroupId) {
            viewModel.selectActiveGroup(matchedGroupId)
        }
    }

    // Selected member IDs for splitting the ticket (hydrates from existing expense splits if inspecting/editing)
    var selectedMemberIds by remember(groupMembers, selectedExistingExpense?.expenseId) {
        val existingSplitMemberIds = if (selectedExistingExpense != null) {
            uiState.splits
                .filter { it.expenseId == selectedExistingExpense.expenseId && it.finalOwedCents > 0L }
                .map { it.memberId }
                .toSet()
        } else emptySet()
        mutableStateOf(existingSplitMemberIds.ifEmpty { groupMembers.map { it.memberId }.toSet() })
    }

    // Selected Payer ID (hydrates from existing expense payer or defaults to Current User)
    var selectedPayerId by remember(groupMembers, selectedExistingExpense?.expenseId) {
        mutableStateOf(
            selectedExistingExpense?.payerId
                ?: groupMembers.firstOrNull { it.isCurrentUser }?.memberId
                ?: groupMembers.firstOrNull()?.memberId.orEmpty()
        )
    }

    fun triggerLivePnrLookup(targetPnr: String) {
        val clean = targetPnr.filter { it.isDigit() }.take(10)
        if (clean.length != 10) {
            fetchError = "Please enter a valid 10-digit IRCTC PNR number"
            return
        }
        fetchError = null
        val existingMatch = existingPnrExpensesInGroup.firstOrNull { it.title.contains(clean) || it.travelPnr == clean }
            ?: viewModel.findExistingExpenseByPnr(clean, uiState.activeGroupId)?.first
            ?: uiState.expenses.firstOrNull { it.title.contains(clean) || it.travelPnr == clean }
        val existingParsed = existingMatch?.let { exp ->
            com.splitmate.app.ui.extractTravelTicketFromTitle(exp.title)?.let { parsed ->
                if (parsed.fareRupees.isBlank() && exp.totalAmountCents > 0L) {
                    parsed.copy(pnr = clean, fareRupees = (exp.totalAmountCents / 100L).toString())
                } else {
                    parsed.copy(pnr = clean)
                }
            }
        }
        
        isFetching = true
        coroutineScope.launch {
            // Check local vault first before showing any network spinner
            val cachedLocal = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.splitmate.app.data.PnrNetworkRepository.loadPersistedPnrSnapshot(context, clean)
            }
            if (cachedLocal != null && com.splitmate.app.data.PnrNetworkRepository.isSnapshotAllConfirmed(cachedLocal)) {
                isFetching = false
                liveSnapshot = cachedLocal
                if (cachedLocal.fromStation.isNotBlank() && manualFromStationInput.isBlank()) {
                    manualFromStationInput = cachedLocal.fromStation
                }
                if (cachedLocal.toStation.isNotBlank() && manualToStationInput.isBlank()) {
                    manualToStationInput = cachedLocal.toStation
                }
                if (existingMatch != null) {
                    val existingSplits = uiState.splits
                        .filter { it.expenseId == existingMatch.expenseId && it.finalOwedCents > 0L }
                        .map { it.memberId }
                        .toSet()
                    if (existingSplits.isNotEmpty()) {
                        selectedMemberIds = existingSplits
                    }
                    selectedPayerId = existingMatch.payerId
                }
                return@launch
            }

            val fetched = fetchLivePnrAndTrainStatus(
                pnr = clean,
                fallbackTicket = existingParsed ?: ParsedTravelTicket(pnr = clean),
                forceManualRefresh = true,
                context = context
            )
            isFetching = false
            liveSnapshot = fetched
            if (fetched.fromStation.isNotBlank() && manualFromStationInput.isBlank()) {
                manualFromStationInput = fetched.fromStation
            }
            if (fetched.toStation.isNotBlank() && manualToStationInput.isBlank()) {
                manualToStationInput = fetched.toStation
            }
            if (existingMatch != null) {
                val existingSplits = uiState.splits
                    .filter { it.expenseId == existingMatch.expenseId && it.finalOwedCents > 0L }
                    .map { it.memberId }
                    .toSet()
                if (existingSplits.isNotEmpty()) {
                    selectedMemberIds = existingSplits
                }
                selectedPayerId = existingMatch.payerId
            } else if (fetched.isLiveVerified && fetched.totalFareRupees > 0) {
                val matchedTrainMembers = fetched.structuredPassengers.mapNotNull { pax ->
                    matchSinglePassengerToGroupMember(pax.passengerNumber, groupMembers)
                }.distinctBy { it.memberId }
                if (matchedTrainMembers.isNotEmpty()) {
                    selectedPayerId = matchedTrainMembers.first().memberId
                }
                val paxCount = fetched.effectivePassengerCount.coerceAtLeast(1)
                if (groupMembers.isNotEmpty()) {
                    selectedMemberIds = if (matchedTrainMembers.size >= 2) {
                        matchedTrainMembers.map { it.memberId }.toSet()
                    } else {
                        groupMembers.take(paxCount.coerceAtLeast(groupMembers.size)).map { it.memberId }.toSet()
                    }
                }
            } else if (!fetched.isLiveVerified) {
                fetchError = "Live IRCTC server unreachable or rate-limited — Enter ticket fare & route manually below."
            }
        }
    }

    LaunchedEffect(initialPnr) {
        val cleanInit = initialPnr.filter { it.isDigit() }.take(10)
        if (cleanInit.length == 10 && liveSnapshot == null) {
            triggerLivePnrLookup(cleanInit)
        }
    }

    val pnrTearProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    val pnrStampScale = remember { androidx.compose.animation.core.Animatable(1.75f) }
    val pnrStampAlpha = remember { androidx.compose.animation.core.Animatable(0f) }

    // Format helper for Rupee strings
    fun formatPaiseDisplay(paise: Long): String {
        val rupees = paise / 100
        val remPaise = kotlin.math.abs(paise % 100).toInt()
        return if (remPaise == 0) {
            "₹%,d".format(Locale.US, rupees)
        } else {
            "₹%,d.%02d".format(Locale.US, rupees, remPaise)
        }
    }

    val snapshot = liveSnapshot
    val ticketPassengerCount = snapshot?.effectivePassengerCount?.coerceAtLeast(1) ?: selectedMemberIds.size.coerceAtLeast(1)
    val manualFarePaise = remember(manualTotalFareInput) {
        val parsed = manualTotalFareInput.trim().toDoubleOrNull() ?: 0.0
        kotlin.math.round(parsed * 100.0).toLong().coerceAtLeast(0L)
    }
    // Prevent double-counted IRCTC fees (+₹25.40) when inspecting or editing an already-logged PNR ticket
    val baseFarePaise = when {
        selectedExistingExpense != null && manualFarePaise == 0L -> selectedExistingExpense.totalAmountCents
        manualFarePaise > 0L -> manualFarePaise
        else -> snapshot?.baseFarePaise ?: 0L
    }
    val convenienceFeePaise = if (selectedExistingExpense == null && manualFarePaise == 0L && snapshot != null && snapshot.totalFareRupees > 0) {
        snapshot.irctcConvenienceFeeUpiPaise
    } else 0L
    val insuranceFeePaise = if (selectedExistingExpense == null && manualFarePaise == 0L && snapshot != null && snapshot.totalFareRupees > 0) {
        snapshot.travelInsurancePaise
    } else 0L
    val effectiveTotalPaise = if (selectedExistingExpense != null && manualFarePaise == 0L) {
        selectedExistingExpense.totalAmountCents
    } else {
        baseFarePaise + convenienceFeePaise + insuranceFeePaise
    }

    val selectedMembersList = remember(groupMembers, selectedMemberIds) {
        groupMembers.filter { it.memberId in selectedMemberIds }
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

    // Build PassengerTicketRow list strictly from snapshot.structuredPassengers + selected group members
    val passengerRows = remember(snapshot, selectedMembersList, selectedPayerId) {
        if (snapshot == null) {
            emptyList()
        } else {
            val basePax = snapshot.structuredPassengers.ifEmpty {
                selectedMembersList.mapIndexed { idx, _ ->
                    LivePnrPassenger(
                        passengerNumber = "P${idx + 1}",
                        initialStatus = if (snapshot.isLiveVerified) "CNF" else "MANUAL",
                        currentStatus = if (snapshot.isLiveVerified) "CNF" else "Included",
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
                PassengerTicketRow(
                    id = cleanPaxId,
                    memberId = matchedMember?.memberId,
                    name = displayName,
                    isPayer = matchedMember?.memberId == selectedPayerId,
                    bookingStatus = pax.initialStatus,
                    currentStatus = pax.currentStatus,
                    isConfirmed = isCnf
                )
            }
        }
    }

    val canConfirmExpense = snapshot != null &&
        effectiveTotalPaise > 0L &&
        selectedMemberIds.isNotEmpty() &&
        !isSubmitting

    Scaffold(
        containerColor = TactilePaperPassTokens.CanvasBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    // 48dp touch bounds below add 3dp around each 42dp circle; padding reduced 20/14 -> 17/11
                    // so the visual circle positions and bar height remain identical.
                    .padding(horizontal = 17.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = onBackClick,
                    shape = CircleShape,
                    color = TactilePaperPassTokens.PaperSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.HairlineBorder),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = TactilePaperPassTokens.InkPrimary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Train ticket",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TactilePaperPassTokens.InkPrimary
                    )
                    Text(
                        text = "Active Group: ${activeGroup?.name ?: "Select a Group"}",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = TactilePaperPassTokens.InkSecondary
                    )
                }

                var showGroupDropdown by remember { mutableStateOf(false) }
                Box {
                    Surface(
                        onClick = { showGroupDropdown = true },
                        shape = CircleShape,
                        color = TactilePaperPassTokens.PaperSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.HairlineBorder),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Groups,
                                contentDescription = "Switch Group",
                                // Dark: #D7E8B6 on #1F1D1A paper (ForestTop #264010 would be ~1.4:1). Light: unchanged.
                                tint = if (SplitMateTheme.isDark) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.ForestTop,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showGroupDropdown,
                        onDismissRequest = { showGroupDropdown = false }
                    ) {
                        uiState.groups.forEach { group ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Train,
                                        contentDescription = null,
                                        tint = TactilePaperPassTokens.SageConfirmedText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        text = group.name,
                                        fontFamily = FigtreeFontFamily,
                                        fontWeight = if (group.groupId == activeGroup?.groupId) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    viewModel.selectActiveGroup(group.groupId)
                                    showGroupDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (snapshot != null) {
                Surface(
                    color = TactilePaperPassTokens.CanvasBackground.copy(alpha = 0.96f),
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        val commitScope = rememberCoroutineScope()
                        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                        Button(
                            onClick = {
                                if (!canConfirmExpense || isSubmitting) return@Button
                                isSubmitting = true
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                playBoardingPassTearAndStampOneShot(true)
                                val fromCode = manualFromStationInput.ifBlank { snapshot.fromStation }.ifBlank { "ORG" }.uppercase(Locale.US)
                                val toCode = manualToStationInput.ifBlank { snapshot.toStation }.ifBlank { "DST" }.uppercase(Locale.US)
                                val chartText = if (snapshot.chartPrepared) "Chart Prepared" else "Chart Not Prepared"
                                val formattedTitle = formatTravelExpenseTitle(
                                    baseCategory = "${snapshot.trainName.ifBlank { "Train Ticket" }} ($fromCode - $toCode)",
                                    ticket = ParsedTravelTicket(
                                        pnr = snapshot.pnr,
                                        trainOrFlightNo = "${snapshot.trainNo} ${snapshot.trainName}".trim().ifBlank { "IRCTC Express" },
                                        fromStation = fromCode,
                                        toStation = toCode,
                                        departureTime = snapshot.departureTime,
                                        coachAndSeats = "Class ${snapshot.travelClass.ifBlank { "SL/3A" }} · ${selectedMemberIds.size} Pax" + when (reviewSplitNature) {
                                            com.splitmate.app.ExpenseSplitNature.PERSONAL -> " · ${com.splitmate.app.ExpenseSplitCopy.PERSONAL_EXPENSE}"
                                            com.splitmate.app.ExpenseSplitNature.ON_BEHALF -> " · for ${com.splitmate.app.ExpenseSplitCopy.beneficiaryLabel(reviewSoleMember?.name, false)}"
                                            com.splitmate.app.ExpenseSplitNature.SHARED -> " (${formatPaiseDisplay(perSelectedMemberSharePaise)}/person)"
                                        },
                                        bookingStatus = snapshot.bookingStatusBadge,
                                        chartStatus = chartText
                                    )
                                )
                                commitScope.launch {
                                    pnrTearProgress.animateTo(1f, SplitMateMotion.fastEffects())
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    launch { pnrStampAlpha.animateTo(1f, SplitMateMotion.fastEffects()) }
                                    pnrStampScale.animateTo(
                                        targetValue = 1f,
                                        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.52f, stiffness = 680f)
                                    )
                                    kotlinx.coroutines.delay(95L)
                                    if (selectedExistingExpense != null) {
                                        viewModel.editExistingExpense(
                                            expenseId = selectedExistingExpense.expenseId,
                                            newTitle = formattedTitle,
                                            newTotalRupees = effectiveTotalPaise / 100.0,
                                            newPayerId = selectedPayerId,
                                            selectedMemberIds = selectedMemberIds.toList()
                                        )
                                    } else {
                                        viewModel.commitQuickEqualExpense(
                                            title = formattedTitle,
                                            totalAmountCents = effectiveTotalPaise,
                                            selectedMemberIds = selectedMemberIds.toList(),
                                            payerMemberId = selectedPayerId
                                        )
                                    }
                                    onExpenseAdded()
                                }
                            },
                            enabled = canConfirmExpense && !isSubmitting,
                            shape = RoundedCornerShape(16.dp),
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
                                    selectedMemberIds.isEmpty() -> "Select At Least 1 Group Member Below"
                                    effectiveTotalPaise <= 0L -> "Enter Ticket Fare Above to Split"
                                    selectedExistingExpense != null -> "Update Split & Save Changes · ${formatPaiseDisplay(effectiveTotalPaise)}"
                                    reviewSplitNature == com.splitmate.app.ExpenseSplitNature.PERSONAL ->
                                        "Confirm & Add ${formatPaiseDisplay(effectiveTotalPaise)} · ${com.splitmate.app.ExpenseSplitCopy.PERSONAL_EXPENSE}"
                                    reviewSplitNature == com.splitmate.app.ExpenseSplitNature.ON_BEHALF ->
                                        "Confirm & Add ${formatPaiseDisplay(effectiveTotalPaise)} · for $reviewSoleLabel"
                                    else -> "Confirm & Add ${formatPaiseDisplay(effectiveTotalPaise)} (${formatPaiseDisplay(perSelectedMemberSharePaise)}/person)"
                                },
                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. PNR SEARCH / LOOKUP BAR (Starts empty, auto-fetches on 10 digits or button tap)
            PnrSearchLookupCard(
                pnrValue = pnrInput,
                onPnrChange = { newValue ->
                    val digits = newValue.filter { ch -> ch.isDigit() }.take(10)
                    pnrInput = digits
                    if (digits.length == 10 && digits != liveSnapshot?.pnr && !isFetching) {
                        triggerLivePnrLookup(digits)
                    }
                },
                isFetching = isFetching,
                onFetchClick = { triggerLivePnrLookup(pnrInput) }
            )

            // Show already-logged PNRs in this group (tap any pill to load & inspect/edit that ticket)
            if (existingPnrExpensesInGroup.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TactilePaperPassTokens.PaperSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Train tickets in ${activeGroup?.name ?: "this trip"} (${existingPnrExpensesInGroup.size}) · tap to open or edit",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            letterSpacing = 0.1.sp,
                            color = TactilePaperPassTokens.SageConfirmedText
                        )
                        existingPnrExpensesInGroup.forEach { exp ->
                            val extractedPnr = Regex("""PNR:\s*(\d{10})""").find(exp.title)?.groupValues?.getOrNull(1) ?: "Ticket"
                            val isActiveTicket = selectedExistingExpense?.expenseId == exp.expenseId
                            Surface(
                                onClick = {
                                    pnrInput = extractedPnr
                                    triggerLivePnrLookup(extractedPnr)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isActiveTicket) TactilePaperPassTokens.SageConfirmedBg else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isActiveTicket) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.InkSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "PNR $extractedPnr · ${formatPaiseDisplay(exp.totalAmountCents)} ${if (isActiveTicket) "(Editing below)" else "(Tap to view pass)"}",
                                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                                        fontFamily = FigtreeFontFamily,
                                        fontWeight = if (isActiveTicket) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (isActiveTicket) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.InkSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (fetchError != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TactilePaperPassTokens.TerracottaWaitlistBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.TerracottaWaitlistBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = fetchError!!,
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = TactilePaperPassTokens.TerracottaWaitlistText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (selectedExistingExpense != null && snapshot != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TactilePaperPassTokens.SageConfirmedBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.SageConfirmedBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Inspecting logged PNR ${snapshot.pnr} (${formatPaiseDisplay(selectedExistingExpense.totalAmountCents)}). You can adjust passengers or who paid below and tap 'Update Split & Save Changes'.",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TactilePaperPassTokens.SageConfirmedText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            // 2. EMPTY STATE WHEN NO PNR IS ENTERED YET vs LIVE BOARDING PASS WHEN FETCHED
            if (snapshot == null) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
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
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = TactilePaperPassTokens.InkPrimary
                        )
                        Text(
                            text = "Nothing is pre-filled or hardcoded. Type your 10-digit PNR number above to fetch live train route, passenger statuses, and exact IRCTC bill.",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = TactilePaperPassTokens.InkSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            onClick = {
                                val manualPnr = pnrInput.filter { it.isDigit() }.padEnd(10, '0').take(10)
                                pnrInput = manualPnr
                                liveSnapshot = com.splitmate.app.data.PnrNetworkRepository.buildUnverifiedManualFallbackSnapshot(
                                    cleanPnr = manualPnr
                                )
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = TactilePaperPassTokens.AmberChartBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.AmberChartBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Enter Ticket Details Manually",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
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
                // Manual Ticket Fare & Route Override Card when CRIS is offline / unverified or fare is 0
                if (snapshot.isManualEntry || !snapshot.isLiveVerified || effectiveTotalPaise <= 0L) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
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
                                text = "Enter fare and route",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                letterSpacing = 0.1.sp,
                                color = TactilePaperPassTokens.AmberChartText
                            )
                            OutlinedTextField(
                                value = manualTotalFareInput,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() || it == '.' }) {
                                        manualTotalFareInput = input
                                    }
                                },
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
                                    onValueChange = { manualFromStationInput = it.uppercase(Locale.US).take(24) },
                                    label = { Text("From Station", fontFamily = FigtreeFontFamily) },
                                    placeholder = { Text("NDLS / Delhi", fontFamily = FigtreeFontFamily) },
                                    singleLine = true,
                                    colors = TactilePaperPassTokens.tactileTextFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = manualToStationInput,
                                    onValueChange = { manualToStationInput = it.uppercase(Locale.US).take(24) },
                                    label = { Text("To Station", fontFamily = FigtreeFontFamily) },
                                    placeholder = { Text("MMCT / Mumbai", fontFamily = FigtreeFontFamily) },
                                    singleLine = true,
                                    colors = TactilePaperPassTokens.tactileTextFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                val depParts = snapshot.departureTime.split("·", " ").map { it.trim() }.filter { it.isNotBlank() }
                val depDateDisplay = depParts.firstOrNull().orEmpty()
                val depTimeDisplay = depParts.getOrNull(1).orEmpty()
                val effectiveFromCode = manualFromStationInput.ifBlank { snapshot.fromStation }.ifBlank { "---" }
                val effectiveToCode = manualToStationInput.ifBlank { snapshot.toStation }.ifBlank { "---" }

                // LIVE TACTILE PAPER BOARDING PASS
                TactilePaperBoardingPass(
                    pnrNumber = snapshot.pnr,
                    trainNumber = snapshot.trainNo.ifBlank { "IRCTC" },
                    trainName = snapshot.trainName.ifBlank { "Manual Ticket Entry" }
                        .lowercase(Locale.US)
                        .split(" ")
                        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } },
                    travelClass = "Class ${snapshot.travelClass.ifBlank { "SL/3A" }} · ${
                        if (snapshot.quotaText == "TQ") "Tatkal" else "IRCTC"
                    } Electronic Ticket",
                    chartStatus = if (snapshot.chartPrepared) "Chart Prepared" else "Chart Not Prepared",
                    originCode = effectiveFromCode,
                    originName = snapshot.fromStationName.ifBlank { effectiveFromCode },
                    departureTime = depTimeDisplay,
                    departureDate = depDateDisplay,
                    destinationCode = effectiveToCode,
                    destinationName = snapshot.toStationName.ifBlank { effectiveToCode },
                    arrivalTime = snapshot.arrivalTime,
                    arrivalDate = if (snapshot.arrivalTime.isNotBlank()) "Scheduled Arrival" else "",
                    duration = snapshot.durationText,
                    totalFareDisplay = formatPaiseDisplay(effectiveTotalPaise),
                    baseFareDisplay = formatPaiseDisplay(baseFarePaise),
                    convenienceFeeDisplay = formatPaiseDisplay(convenienceFeePaise),
                    insuranceFeeDisplay = formatPaiseDisplay(insuranceFeePaise),
                    perPersonShareDisplay = formatPaiseDisplay(perSelectedMemberSharePaise),
                    passengerCount = ticketPassengerCount,
                    passengers = passengerRows,
                    splitNatureCaption = when (reviewSplitNature) {
                        com.splitmate.app.ExpenseSplitNature.PERSONAL -> com.splitmate.app.ExpenseSplitCopy.PERSONAL_NOT_SPLIT
                        com.splitmate.app.ExpenseSplitNature.ON_BEHALF -> "Paid for $reviewSoleLabel"
                        com.splitmate.app.ExpenseSplitNature.SHARED -> null
                    }
                )

                // 3. INTERACTIVE MEMBER SELECTION CARD: ASK WHICH MEMBERS FROM THE GROUP ARE ON THIS TICKET
                MemberSplitSelectionCard(
                    groupName = activeGroup?.name ?: "Group",
                    ticketPassengerCount = ticketPassengerCount,
                    members = groupMembers,
                    selectedMemberIds = selectedMemberIds,
                    selectedPayerId = selectedPayerId,
                    onSelectPayer = { newPayerId -> selectedPayerId = newPayerId },
                    exactAllocationsMap = exactAllocationsMap,
                    formatPaise = ::formatPaiseDisplay,
                    onToggleMember = { memberId ->
                        selectedMemberIds = if (memberId in selectedMemberIds) {
                            if (selectedMemberIds.size > 1) selectedMemberIds - memberId else selectedMemberIds
                        } else {
                            selectedMemberIds + memberId
                        }
                    },
                    onSelectExactTicketCount = {
                        selectedMemberIds = groupMembers.take(ticketPassengerCount).map { it.memberId }.toSet()
                    },
                    onAddMemberToGroup = { newFriendName ->
                        viewModel.addMemberToActiveGroup(newFriendName)
                    },
                    totalFareDisplay = formatPaiseDisplay(effectiveTotalPaise),
                    perMemberShareDisplay = formatPaiseDisplay(perSelectedMemberSharePaise),
                    payerReimbursementDisplay = formatPaiseDisplay(
                        (effectiveTotalPaise - payerSharePaise).coerceAtLeast(0L)
                    ),
                    effectiveTotalPaise = effectiveTotalPaise
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        BoardingPassCommitStampOverlay(
            tearProgress = pnrTearProgress.value,
            stampScale = pnrStampScale.value,
            stampAlpha = pnrStampAlpha.value,
            accentColor = TactilePaperPassTokens.ForestTop,
            stampSubLabel = "IRCTC PNR ${liveSnapshot?.pnr ?: pnrInput} · Verified Split"
        )
        }
    }
}

// ============================================================================
// 5. COMPONENT: PNR SEARCH INPUT BAR
// ============================================================================
@Composable
fun PnrSearchLookupCard(
    pnrValue: String,
    onPnrChange: (String) -> Unit,
    isFetching: Boolean = false,
    onFetchClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
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
                        text = "Enter your 10-digit PNR",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.1.sp,
                        color = TactilePaperPassTokens.InkMuted
                    )
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (pnrValue.isEmpty()) {
                            Text(
                                text = "Tap here to enter 10-digit PNR...",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = TactilePaperPassTokens.InkMuted
                            )
                        }
                        BasicTextField(
                            value = pnrValue,
                            onValueChange = onPnrChange,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                letterSpacing = 1.4.sp,
                                fontFeatureSettings = "tnum",
                                color = TactilePaperPassTokens.InkPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Surface(
                onClick = onFetchClick,
                shape = RoundedCornerShape(12.dp),
                color = TactilePaperPassTokens.ForestTop
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFetching) {
                        ContainedLoadingIndicator(
                            containerSize = 20.dp,
                            containerColor = TactilePaperPassTokens.ForestBadgeFill,
                            indicatorColor = TactilePaperPassTokens.ForestAccent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Fetch Live PNR",
                            tint = TactilePaperPassTokens.ForestAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isFetching) "Fetching..." else "Fetch PNR",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TactilePaperPassTokens.OnForest
                    )
                }
            }
        }
    }
}

// ============================================================================
// 6. HERO COMPONENT: SKEUOMORPHIC PAPER BOARDING PASS
// ============================================================================
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
    splitNatureCaption: String? = null
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
                        text = "Passengers on this PNR (${passengers.size})",
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
                                        text = splitNatureCaption ?: "Per-Passenger Share: $perPersonShareDisplay",
                                        style = MaterialTheme.typography.labelSmall,
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

            // SECTION D: BOTTOM TICKET STUB (TOTAL ALL-INCLUSIVE FARE, PER-PASSENGER SHARE & BARCODE)
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
                        fontSize = 10.sp,
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
                        val formattedPnr = if (pnrNumber.length == 10) {
                            "${pnrNumber.substring(0, 4)}-${pnrNumber.substring(4, 7)}-${pnrNumber.substring(7, 10)}"
                        } else pnrNumber
                        Text(
                            text = "PNR $formattedPnr",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
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
                            text = splitNatureCaption ?: "$perPersonShareDisplay / each",
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
                    fontSize = 10.sp,
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

                Surface(
                    onClick = onSelectExactTicketCount,
                    shape = RoundedCornerShape(999.dp),
                    color = TactilePaperPassTokens.SageConfirmedBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.SageConfirmedBorder),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(
                        text = "Select $ticketPassengerCount Pax",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TactilePaperPassTokens.SageConfirmedText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // If the group currently has fewer members than the ticket's passenger count, let the user add the remaining passengers right here!
            if (members.size < ticketPassengerCount) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
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
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TactilePaperPassTokens.AmberChartText
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = quickAddName,
                                onValueChange = { quickAddName = it },
                                placeholder = { Text("Enter passenger name (e.g. Rahul)", fontSize = 12.sp) },
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
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TactilePaperPassTokens.ForestTop)
                            ) {
                                Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", fontFamily = FigtreeFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
