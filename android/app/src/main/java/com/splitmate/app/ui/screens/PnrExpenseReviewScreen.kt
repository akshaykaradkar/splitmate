package com.splitmate.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
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
import com.splitmate.app.ui.FigtreeFontFamily
import com.splitmate.app.ui.LivePnrPassenger
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.ui.LivePnrStatusSnapshot
import com.splitmate.app.ui.ParsedTravelTicket
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.fetchLivePnrAndTrainStatus
import com.splitmate.app.ui.formatTravelExpenseTitle
import androidx.compose.animation.core.spring
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.util.Locale

// ============================================================================
// 1. DESIGN TOKENS — "TACTILE LUXURY PAPER BOARDING PASS" (LIGHT & DARK ADAPTIVE)
// ============================================================================
object TactilePaperPassTokens {
    val CanvasBackground: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFF141311) else Color(0xFFFAF7F2)
    val PaperSurface: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFF1F1D1A) else Color(0xFFFFFDF9)
    val PaperStubSurface: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFF282521) else Color(0xFFF6F2EA)
    val HairlineBorder: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFF38342E) else Color(0xFFE5DEC9)
    val PerforationLine: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFF4A443C) else Color(0xFFD6CEBE)

    val ForestTop = Color(0xFF264010)              // Deep Organic Forest Green
    val ForestBottom = Color(0xFF1B2E0B)           // Richer Pine Shadow
    val ForestBadgeFill = Color(0xFF345418)        // Translucent pill surface
    val ForestBadgeStroke = Color(0xFF4A7325)      // Crisp moss highlight

    val AmberChartBg: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFF332610) else Color(0xFFFEF3D6)
    val AmberChartBorder: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFF6B4E1B) else Color(0xFFF7D788)
    val AmberChartText: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFFFCD34D) else Color(0xFF9E5808)

    val TerracottaWaitlistBg: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFF3A1F18) else Color(0xFFFDECE6)
    val TerracottaWaitlistBorder: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFF7C3725) else Color(0xFFF7C6B5)
    val TerracottaWaitlistText: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFFFED8C8) else Color(0xFFB53C1A)

    val SageConfirmedBg: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFF233316) else Color(0xFFE4F2D5)
    val SageConfirmedBorder: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFF426128) else Color(0xFFC2E0A3)
    val SageConfirmedText: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFFD7E8B6) else Color(0xFF2B520D)

    val InkPrimary: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFFF4EFEA) else Color(0xFF1E1C1A)
    val InkSecondary: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFFB8B0A4) else Color(0xFF6B655E)
    val InkMuted: Color
        @Composable get() = if (SplitMateTheme.isDark) Color(0xFF857D73) else Color(0xFF9C9488)

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
        isFetching = true
        val existingMatch = existingPnrExpensesInGroup.firstOrNull { it.title.contains(clean) }
        val existingParsed = existingMatch?.let { com.splitmate.app.ui.extractTravelTicketFromTitle(it.title) }
        coroutineScope.launch {
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
            } else {
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
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = onBackClick,
                    shape = CircleShape,
                    color = TactilePaperPassTokens.PaperSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.HairlineBorder),
                    modifier = Modifier.size(42.dp)
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
                        text = "IRCTC Train PNR Split",
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
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Groups,
                                contentDescription = "Switch Group",
                                tint = TactilePaperPassTokens.ForestTop,
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
                                    baseCategory = "${snapshot.trainName.ifBlank { "Train Ticket" }} ($fromCode → $toCode)",
                                    ticket = ParsedTravelTicket(
                                        pnr = snapshot.pnr,
                                        trainOrFlightNo = "${snapshot.trainNo} ${snapshot.trainName}".trim().ifBlank { "IRCTC Express" },
                                        fromStation = fromCode,
                                        toStation = toCode,
                                        departureTime = snapshot.departureTime,
                                        coachAndSeats = "Class ${snapshot.travelClass.ifBlank { "SL/3A" }} · ${selectedMemberIds.size} Pax (${formatPaiseDisplay(perSelectedMemberSharePaise)}/person)",
                                        bookingStatus = snapshot.bookingStatusBadge,
                                        chartStatus = chartText
                                    )
                                )
                                commitScope.launch {
                                    pnrTearProgress.animateTo(1f, androidx.compose.animation.core.tween(125, easing = androidx.compose.animation.core.FastOutLinearInEasing))
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    launch { pnrStampAlpha.animateTo(1f, androidx.compose.animation.core.tween(60)) }
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
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFD5CFC2),
                                disabledContentColor = Color(0xFF6B655E)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    selectedMemberIds.isEmpty() -> "Select At Least 1 Group Member Below"
                                    effectiveTotalPaise <= 0L -> "Enter Ticket Fare Above to Split"
                                    selectedExistingExpense != null -> "Update Split & Save Changes · ${formatPaiseDisplay(effectiveTotalPaise)}"
                                    else -> "Confirm & Add ${formatPaiseDisplay(effectiveTotalPaise)} (${formatPaiseDisplay(perSelectedMemberSharePaise)}/person)"
                                },
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            text = "LOGGED TRAIN TICKETS IN ${activeGroup?.name?.uppercase(Locale.US) ?: "THIS GROUP"} (${existingPnrExpensesInGroup.size}) · TAP TO INSPECT OR EDIT",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            letterSpacing = 0.7.sp,
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
                                Text(
                                    text = "✓ PNR $extractedPnr · ${formatPaiseDisplay(exp.totalAmountCents)} ${if (isActiveTicket) "(Editing below)" else "(Tap to view pass)"}",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = if (isActiveTicket) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = if (isActiveTicket) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.InkSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
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
                            Text(
                                text = "Enter Ticket Details Manually →",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TactilePaperPassTokens.AmberChartText,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
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
                                text = "MANUAL TICKET FARE & ROUTE ENTRY (ZERO SYNTHETIC DATA)",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                letterSpacing = 0.7.sp,
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
                                    placeholder = { Text("NDLS / DELHI", fontFamily = FigtreeFontFamily) },
                                    singleLine = true,
                                    colors = TactilePaperPassTokens.tactileTextFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = manualToStationInput,
                                    onValueChange = { manualToStationInput = it.uppercase(Locale.US).take(24) },
                                    label = { Text("To Station", fontFamily = FigtreeFontFamily) },
                                    placeholder = { Text("MMCT / MUMBAI", fontFamily = FigtreeFontFamily) },
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
                    passengers = passengerRows
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
                    )
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
    var energyState by remember {
        mutableStateOf(com.splitmate.app.ui.components.Gm3EnergyState.ANTICIPATING)
    }
    LaunchedEffect(isFetching) {
        energyState = if (isFetching) {
            com.splitmate.app.ui.components.Gm3EnergyState.PROCESSING
        } else if (energyState == com.splitmate.app.ui.components.Gm3EnergyState.PROCESSING) {
            com.splitmate.app.ui.components.Gm3EnergyState.RESPONDING
        } else {
            energyState
        }
    }

    com.splitmate.app.ui.components.Gm3AuroraEnergySurface(
        state = energyState,
        onStateAutoTransition = { nextState -> energyState = nextState },
        palette = com.splitmate.app.ui.components.Gm3EnergyAccentPalette.BUCKWHEAT_SAGE,
        shape = RoundedCornerShape(18.dp),
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
                        text = "ENTER 10-DIGIT IRCTC PNR NUMBER",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
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
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Fetch Live PNR",
                            tint = Color(0xFFD7E8B6),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isFetching) "Fetching..." else "Fetch PNR",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
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
    passengers: List<PassengerTicketRow>
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
                                    tint = Color(0xFFD7E8B6),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$trainNumber · $trainName",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
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
                            color = Color(0xFFC5D6A7)
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
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = originName,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color(0xFFD7E8B6),
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
                                color = Color(0xFFAEC48A)
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
                                    color = Color(0xFFD7E8B6),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                        ) {
                            val yCenter = size.height / 2f
                            drawLine(
                                color = Color(0xFF6B9440),
                                start = Offset(8f, yCenter),
                                end = Offset(size.width - 8f, yCenter),
                                strokeWidth = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                            drawCircle(
                                color = Color(0xFFD7E8B6),
                                radius = 4.dp.toPx(),
                                center = Offset(4.dp.toPx(), yCenter)
                            )
                            drawCircle(
                                color = Color(0xFFD7E8B6),
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
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = destinationName,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color(0xFFD7E8B6),
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
                                color = Color(0xFFAEC48A)
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
                        text = "VERIFIED PASSENGERS ON PNR (${passengers.size})",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        color = TactilePaperPassTokens.InkMuted
                    )
                    Text(
                        text = "BOOKING → LIVE STATUS",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        color = TactilePaperPassTokens.InkMuted
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                passengers.forEachIndexed { index, pax ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            com.splitmate.app.AvatarToken(
                                initials = pax.name,
                                bg = if (pax.isPayer) TactilePaperPassTokens.SageConfirmedBg else Color(0xFFF0ECE1),
                                textColor = if (pax.isPayer) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.InkSecondary,
                                size = 34
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (pax.isPayer) TactilePaperPassTokens.SageConfirmedBg else Color(0xFFF0ECE1),
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

                            Column {
                                Text(
                                    text = pax.name,
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TactilePaperPassTokens.InkPrimary
                                )
                                Text(
                                    text = "Per-Passenger Share: $perPersonShareDisplay",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = TactilePaperPassTokens.SageConfirmedText
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = pax.bookingStatus,
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
                                Text(
                                    text = "→ ${pax.currentStatus}",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = if (pax.isConfirmed) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.TerracottaWaitlistText,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (index < passengers.lastIndex) {
                        HorizontalDivider(
                            color = TactilePaperPassTokens.HairlineBorder.copy(alpha = 0.55f),
                            thickness = 1.dp
                        )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TactilePaperPassTokens.PaperStubSurface)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ALL-INCLUSIVE IRCTC FARE ($passengerCount PASSENGERS)",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                        color = TactilePaperPassTokens.InkMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = totalFareDisplay,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            letterSpacing = (-0.5).sp,
                            color = TactilePaperPassTokens.InkPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TactilePaperPassTokens.SageConfirmedBg
                        ) {
                            Text(
                                text = "$perPersonShareDisplay / each",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = TactilePaperPassTokens.SageConfirmedText,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Base $baseFareDisplay + IRCTC Conv. $convenienceFeeDisplay + Insurance $insuranceFeeDisplay",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = TactilePaperPassTokens.InkSecondary
                    )
                }

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
        }
    }
}

// ============================================================================
// 7. INTERACTIVE GROUP MEMBER SELECTION ("SELECT WHICH PASSENGERS ARE ON THIS TICKET")
// ============================================================================
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
    payerReimbursementDisplay: String
) {
    var quickAddName by remember { mutableStateOf("") }
    val payerMemberName = remember(members, selectedPayerId) {
        members.firstOrNull { it.memberId == selectedPayerId }?.let {
            if (it.isCurrentUser) "You (${it.name})" else it.name
        } ?: "you"
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
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
            // "Paid by: [Member]" horizontal chip selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "WHO PAID FOR THIS IRCTC TICKET?",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    letterSpacing = 0.7.sp,
                    color = TactilePaperPassTokens.InkSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    members.forEach { member ->
                        val isCurrentPayer = member.memberId == selectedPayerId
                        Surface(
                            onClick = { onSelectPayer(member.memberId) },
                            shape = RoundedCornerShape(999.dp),
                            color = if (isCurrentPayer) TactilePaperPassTokens.ForestTop else TactilePaperPassTokens.PaperStubSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrentPayer) TactilePaperPassTokens.ForestTop else TactilePaperPassTokens.HairlineBorder
                            )
                        ) {
                            Text(
                                text = if (member.isCurrentUser) "Paid by ${member.name} (You)" else "Paid by ${member.name}",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = if (isCurrentPayer) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = if (isCurrentPayer) Color.White else TactilePaperPassTokens.InkSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = TactilePaperPassTokens.HairlineBorder.copy(alpha = 0.45f),
                thickness = 1.dp
            )

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
                    border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.SageConfirmedBorder)
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
                                shape = RoundedCornerShape(12.dp),
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
                val rowBg by animateColorAsState(
                    targetValue = if (isSelected) Color.Transparent else TactilePaperPassTokens.PaperStubSurface.copy(alpha = 0.6f),
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                    label = "memberRowBg"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(rowBg)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleMember(member.memberId)
                        }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
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
                                bg = if (isSelected) TactilePaperPassTokens.SageConfirmedBg else Color(0xFFEAE4D7),
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
                                            !isSelected -> Color(0xFFD6CFC0)
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
                                    tint = Color.White,
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
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (isSelected) TactilePaperPassTokens.InkPrimary else TactilePaperPassTokens.InkMuted
                        )
                        if (isPayer) {
                            Text(
                                text = "Getting back $payerReimbursementDisplay",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = TactilePaperPassTokens.SageConfirmedText
                            )
                        }
                    }
                }

                if (index < members.lastIndex) {
                    HorizontalDivider(
                        color = TactilePaperPassTokens.HairlineBorder.copy(alpha = 0.45f),
                        thickness = 1.dp
                    )
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
