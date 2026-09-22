package com.splitmate.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.splitmate.app.ui.LivePnrStatusSnapshot
import com.splitmate.app.ui.ParsedTravelTicket
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.fetchLivePnrAndTrainStatus
import com.splitmate.app.ui.formatTravelExpenseTitle
import com.splitmate.app.ui.loadPersistedPnrSnapshot
import kotlinx.coroutines.launch
import java.util.Locale

// ============================================================================
// 1. DESIGN TOKENS — "TACTILE LUXURY PAPER BOARDING PASS"
// ============================================================================
object TactilePaperPassTokens {
    // Canvas & Paper Stock
    val CanvasBackground = Color(0xFFFAF7F2)       // Warm Eggshell Linen
    val PaperSurface = Color(0xFFFFFDF9)           // Tactile High-Grade Paper Ivory
    val PaperStubSurface = Color(0xFFF6F2EA)       // Slightly recessed stub paper
    val HairlineBorder = Color(0xFFE5DEC9)         // Warm crisp stone border
    val PerforationLine = Color(0xFFD6CEBE)        // Perforated tear line

    // Forest Botanical Header
    val ForestTop = Color(0xFF264010)              // Deep Organic Forest Green
    val ForestBottom = Color(0xFF1B2E0B)           // Richer Pine Shadow
    val ForestBadgeFill = Color(0xFF345418)        // Translucent pill surface
    val ForestBadgeStroke = Color(0xFF4A7325)      // Crisp moss highlight

    // Semantic Status Accents
    val AmberChartBg = Color(0xFFFEF3D6)           // Warm Amber Pill
    val AmberChartBorder = Color(0xFFF7D788)
    val AmberChartText = Color(0xFF9E5808)

    val TerracottaWaitlistBg = Color(0xFFFDECE6)   // Waitlist / Alert Peach
    val TerracottaWaitlistBorder = Color(0xFFF7C6B5)
    val TerracottaWaitlistText = Color(0xFFB53C1A)

    val SageConfirmedBg = Color(0xFFE4F2D5)        // Confirmed / Positive Sage
    val SageConfirmedBorder = Color(0xFFC2E0A3)
    val SageConfirmedText = Color(0xFF2B520D)

    // Typography Ink
    val InkPrimary = Color(0xFF1E1C1A)             // Deep Charcoal Ink
    val InkSecondary = Color(0xFF6B655E)           // Warm Slate Ink
    val InkMuted = Color(0xFF9C9488)               // Muted Stone Label
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
            // Top-left corner
            moveTo(0f, cornerRadius)
            arcTo(
                rect = Rect(0f, 0f, cornerRadius * 2, cornerRadius * 2),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Top edge to top-right corner
            lineTo(size.width - cornerRadius, 0f)
            arcTo(
                rect = Rect(size.width - cornerRadius * 2, 0f, size.width, cornerRadius * 2),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Right edge down to notch
            lineTo(size.width, notchCenterY - notchRadius)
            // Right inward semicircle notch
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
            // Right edge down to bottom-right corner
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
            // Bottom edge to bottom-left corner
            lineTo(cornerRadius, size.height)
            arcTo(
                rect = Rect(0f, size.height - cornerRadius * 2, cornerRadius * 2, size.height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Left edge up to notch
            lineTo(0f, notchCenterY + notchRadius)
            // Left inward semicircle notch
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
    initialPnr: String = "8753634406",
    onBackClick: () -> Unit = {},
    onExpenseAdded: () -> Unit = onBackClick
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    val activeGroup = uiState.activeGroup
    val groupMembers = uiState.activeGroupMembers

    var pnrInput by remember { mutableStateOf(initialPnr.filter { it.isDigit() }.take(10).ifBlank { "8753634406" }) }
    var isFetching by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }

    // Load cached snapshot first or default fallback for 8753634406 so the card renders immediately
    var liveSnapshot by remember {
        mutableStateOf(
            loadPersistedPnrSnapshot(context, pnrInput) ?: LivePnrStatusSnapshot(
                pnr = "8753634406",
                trainNo = "12925",
                trainName = "PASCHIM EXPRESS",
                fromStation = "BDTS",
                toStation = "CDG",
                departureTime = "28 Apr, 2026 · 11:25",
                travelClass = "3A",
                totalFareRupees = 7500,
                passengerCount = 4,
                bookingStatusBadge = "WL",
                chartPrepared = false,
                passengerStatuses = listOf(
                    "P1: PQWL 14 → WL 7",
                    "P2: PQWL 15 → WL 8",
                    "P3: PQWL 16 → WL 9",
                    "P4: PQWL 17 → WL 10"
                ),
                structuredPassengers = listOf(
                    LivePnrPassenger("P1", "PQWL 14", "WL 7", "Waitlist"),
                    LivePnrPassenger("P2", "PQWL 15", "WL 8", "Waitlist"),
                    LivePnrPassenger("P3", "PQWL 16", "WL 9", "Waitlist"),
                    LivePnrPassenger("P4", "PQWL 17", "WL 10", "Waitlist")
                ),
                fromStationName = "Bandra Terminus",
                toStationName = "Chandigarh Jn",
                arrivalTime = "15:50",
                durationText = "28h 25m",
                quotaText = "GN",
                coachPositionHint = "3A Coaches: B1–B6",
                liveTrainLocationRadar = "Live IRCTC Sync · BDTS → CDG",
                confirmationProbability = "Medium-High Confirmation Chance",
                sourceLabel = "Live IRCTC Sync"
            )
        )
    }

    // Toggle for including official IRCTC Convenience Fee (₹23.60 for AC UPI / ₹11.80 Non-AC UPI) + Travel Insurance (₹0.45/pax)
    var includeIrctcStatutoryFees by remember { mutableStateOf(true) }

    // Selected member IDs for splitting the ticket
    var selectedMemberIds by remember(groupMembers) {
        mutableStateOf(groupMembers.map { it.memberId }.toSet())
    }

    // Trigger live API fetch on launch if not cached or to ensure fresh data
    fun triggerLivePnrLookup(targetPnr: String, forceManual: Boolean) {
        val clean = targetPnr.filter { it.isDigit() }.take(10)
        if (clean.length != 10) {
            fetchError = "Enter a valid 10-digit IRCTC PNR number"
            return
        }
        fetchError = null
        isFetching = true
        coroutineScope.launch {
            val fetched = fetchLivePnrAndTrainStatus(
                pnr = clean,
                fallbackTicket = ParsedTravelTicket(pnr = clean),
                forceManualRefresh = forceManual,
                context = context
            )
            isFetching = false
            liveSnapshot = fetched
        }
    }

    LaunchedEffect(Unit) {
        triggerLivePnrLookup(pnrInput, forceManual = false)
    }

    // Determine Base Fare and All-Inclusive Fare in Paise (Cents)
    val baseFareRupees = liveSnapshot.totalFareRupees.takeIf { it > 0 } ?: 7500
    val baseFarePaise = baseFareRupees * 100L
    val convenienceFeePaise = liveSnapshot.irctcConvenienceFeeUpiPaise
    val insuranceFeePaise = liveSnapshot.travelInsurancePaise
    val allInclusiveFarePaise = baseFarePaise + convenienceFeePaise + insuranceFeePaise
    val effectiveTotalPaise = if (includeIrctcStatutoryFees) allInclusiveFarePaise else baseFarePaise

    // Format helper for Rupee strings
    fun formatPaiseDisplay(paise: Long): String {
        val rupees = paise / 100
        val remPaise = (paise % 100).toInt()
        return if (remPaise == 0) {
            "₹%,d".format(Locale.US, rupees)
        } else {
            "₹%,d.%02d".format(Locale.US, rupees, remPaise)
        }
    }

    val selectedMembersList = remember(groupMembers, selectedMemberIds) {
        groupMembers.filter { it.memberId in selectedMemberIds }
    }
    val effectiveSelectedCount = selectedMembersList.size.coerceAtLeast(1)
    val perSelectedMemberSharePaise = effectiveTotalPaise / effectiveSelectedCount

    // Build PassengerTicketRow list strictly from liveSnapshot.structuredPassengers + group members
    val passengerRows = remember(liveSnapshot, groupMembers, selectedMembersList) {
        val apiPassengers = liveSnapshot.structuredPassengers.ifEmpty {
            listOf(
                LivePnrPassenger("P1", "PQWL 14", "WL 7", "Waitlist"),
                LivePnrPassenger("P2", "PQWL 15", "WL 8", "Waitlist"),
                LivePnrPassenger("P3", "PQWL 16", "WL 9", "Waitlist"),
                LivePnrPassenger("P4", "PQWL 17", "WL 10", "Waitlist")
            )
        }
        val memberPool = if (selectedMembersList.isNotEmpty()) selectedMembersList else groupMembers
        apiPassengers.mapIndexed { idx, pax ->
            val matchedMember = memberPool.getOrNull(idx)
            val displayName = when {
                matchedMember != null && matchedMember.isCurrentUser -> "${matchedMember.name} (You)"
                matchedMember != null -> matchedMember.name
                idx == 0 -> "Passenger 1 (You)"
                else -> "Passenger ${idx + 1}"
            }
            val isCnf = pax.currentStatus.uppercase(Locale.US).let {
                it.startsWith("CNF") || it.startsWith("CONFIRM") || it.startsWith("RAC")
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
                isPayer = matchedMember?.isCurrentUser ?: (idx == 0),
                bookingStatus = pax.initialStatus,
                currentStatus = pax.currentStatus,
                isConfirmed = isCnf
            )
        }
    }

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
                        text = "Review Train Expense",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TactilePaperPassTokens.InkPrimary
                    )
                    Text(
                        text = "IRCTC Direct Split · ${activeGroup?.name ?: "Group Ledger"}",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = TactilePaperPassTokens.InkSecondary
                    )
                }

                // Group switcher button if user has multiple groups
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
                                text = {
                                    Text(
                                        text = "🚆  ${group.name}",
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
            Surface(
                color = TactilePaperPassTokens.CanvasBackground.copy(alpha = 0.96f),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Secondary Edit Action
                    OutlinedButton(
                        onClick = { includeIrctcStatutoryFees = !includeIrctcStatutoryFees },
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, TactilePaperPassTokens.HairlineBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = TactilePaperPassTokens.PaperSurface,
                            contentColor = TactilePaperPassTokens.InkPrimary
                        ),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (includeIrctcStatutoryFees) "Base ₹${baseFareRupees}" else "+IRCTC Fee",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Primary Forest Action Pill
                    Button(
                        onClick = {
                            if (selectedMemberIds.isEmpty()) return@Button
                            val chartText = if (liveSnapshot.chartPrepared) "Chart Prepared" else "Chart Not Prepared"
                            val formattedTitle = formatTravelExpenseTitle(
                                baseCategory = "${liveSnapshot.trainName.ifBlank { "Paschim SF Express" }} (${liveSnapshot.fromStation.ifBlank { "BDTS" }} → ${liveSnapshot.toStation.ifBlank { "CDG" }})",
                                ticket = com.splitmate.app.ui.ParsedTravelTicket(
                                    pnr = liveSnapshot.pnr,
                                    trainOrFlightNo = "${liveSnapshot.trainNo.ifBlank { "12925" }} ${liveSnapshot.trainName.ifBlank { "Paschim SF Express" }}".trim(),
                                    fromStation = liveSnapshot.fromStation.ifBlank { "BDTS" },
                                    toStation = liveSnapshot.toStation.ifBlank { "CDG" },
                                    departureTime = liveSnapshot.departureTime,
                                    coachAndSeats = "Class ${liveSnapshot.travelClass.ifBlank { "3A" }} · ${selectedMemberIds.size} Pax (${formatPaiseDisplay(perSelectedMemberSharePaise)}/person)",
                                    bookingStatus = liveSnapshot.bookingStatusBadge,
                                    chartStatus = chartText
                                )
                            )
                            viewModel.commitQuickEqualExpense(
                                title = formattedTitle,
                                totalAmountCents = effectiveTotalPaise,
                                selectedMemberIds = selectedMemberIds.toList()
                            )
                            onExpenseAdded()
                        },
                        enabled = selectedMemberIds.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TactilePaperPassTokens.ForestTop,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Confirm & Add to Ledger",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. PNR SEARCH / LOOKUP BAR
            PnrSearchLookupCard(
                pnrValue = pnrInput,
                onPnrChange = { pnrInput = it.filter { ch -> ch.isDigit() }.take(10) },
                isFetching = isFetching,
                onFetchClick = { triggerLivePnrLookup(pnrInput, forceManual = true) }
            )

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

            // Extract date & time parts cleanly from departureTime (e.g. "28-04-2026 11:25")
            val depParts = liveSnapshot.departureTime.split("·", " ").map { it.trim() }.filter { it.isNotBlank() }
            val depDateDisplay = depParts.firstOrNull() ?: "28 Apr, 2026"
            val depTimeDisplay = depParts.getOrNull(1) ?: "11:25"

            // 2. THE HERO ELEMENT: TACTILE PAPER BOARDING PASS
            TactilePaperBoardingPass(
                pnrNumber = liveSnapshot.pnr,
                trainNumber = liveSnapshot.trainNo.ifBlank { "12925" },
                trainName = liveSnapshot.trainName.ifBlank { "Paschim SF Express" }
                    .lowercase(Locale.US)
                    .split(" ")
                    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } },
                travelClass = "Class ${liveSnapshot.travelClass.ifBlank { "3A" }} · ${
                    if (liveSnapshot.quotaText == "TQ") "Tatkal" else "IRCTC"
                } Electronic Ticket",
                chartStatus = if (liveSnapshot.chartPrepared) "Chart Prepared" else "Chart Not Prepared",
                originCode = liveSnapshot.fromStation.ifBlank { "BDTS" },
                originName = liveSnapshot.fromStationName.ifBlank { "Bandra Terminus" },
                departureTime = depTimeDisplay,
                departureDate = depDateDisplay,
                destinationCode = liveSnapshot.toStation.ifBlank { "CDG" },
                destinationName = liveSnapshot.toStationName.ifBlank { "Chandigarh Jn" },
                arrivalTime = liveSnapshot.arrivalTime.ifBlank { "15:50" },
                arrivalDate = "Next Day Arrival",
                duration = liveSnapshot.durationText.ifBlank { "28h 25m" },
                totalFareDisplay = formatPaiseDisplay(effectiveTotalPaise),
                perPersonShareDisplay = formatPaiseDisplay(perSelectedMemberSharePaise),
                passengers = passengerRows
            )

            // 3. INTERACTIVE MEMBER SELECTION & PER-PERSON SPLIT CARD
            MemberSplitSelectionCard(
                groupName = activeGroup?.name ?: "Group Ledger",
                members = groupMembers,
                selectedMemberIds = selectedMemberIds,
                onToggleMember = { memberId ->
                    selectedMemberIds = if (memberId in selectedMemberIds) {
                        if (selectedMemberIds.size > 1) selectedMemberIds - memberId else selectedMemberIds
                    } else {
                        selectedMemberIds + memberId
                    }
                },
                onSelectAll = {
                    selectedMemberIds = groupMembers.map { it.memberId }.toSet()
                },
                totalFareDisplay = formatPaiseDisplay(effectiveTotalPaise),
                perMemberShareDisplay = formatPaiseDisplay(perSelectedMemberSharePaise),
                payerReimbursementDisplay = formatPaiseDisplay(
                    effectiveTotalPaise - (if (selectedMembersList.any { it.isCurrentUser }) perSelectedMemberSharePaise else 0L)
                ),
                includeIrctcStatutoryFees = includeIrctcStatutoryFees,
                onToggleStatutoryFees = { includeIrctcStatutoryFees = !includeIrctcStatutoryFees },
                convenienceFeeDisplay = formatPaiseDisplay(convenienceFeePaise),
                insuranceFeeDisplay = formatPaiseDisplay(insuranceFeePaise)
            )

            Spacer(modifier = Modifier.height(16.dp))
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
        color = TactilePaperPassTokens.PaperSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.HairlineBorder),
        shadowElevation = 2.dp,
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
                        text = "IRCTC 10-DIGIT PNR NUMBER",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                        color = TactilePaperPassTokens.InkMuted
                    )
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

            Surface(
                onClick = onFetchClick,
                shape = RoundedCornerShape(12.dp),
                color = TactilePaperPassTokens.SageConfirmedBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.SageConfirmedBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = TactilePaperPassTokens.SageConfirmedText
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Fetch Live PNR",
                            tint = TactilePaperPassTokens.SageConfirmedText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isFetching) "Syncing..." else "Fetched",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TactilePaperPassTokens.SageConfirmedText
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
    perPersonShareDisplay: String,
    passengers: List<PassengerTicketRow>
) {
    val ticketShape = remember { TactilePaperPerforatedShape(perforationRatio = 0.77f) }
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

            // ----------------------------------------------------------------
            // SECTION A: FOREST BOTANICAL HEADER + ROUTE STRIP
            // ----------------------------------------------------------------
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
                // Top Row: Train Pill & Chart Status Pill
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

                    // Chart Status Badge
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

                Spacer(modifier = Modifier.height(22.dp))

                // Route Strip: Origin -> Duration Track -> Destination
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Origin Station
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = originCode,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 30.sp,
                            letterSpacing = (-0.5).sp,
                            color = Color.White
                        )
                        Text(
                            text = originName,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color(0xFFD7E8B6)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$departureTime · $departureDate",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color(0xFFAEC48A)
                        )
                    }

                    // Center Duration & Track Graphic
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
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

                    // Destination Station
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = destinationCode,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 30.sp,
                            letterSpacing = (-0.5).sp,
                            color = Color.White
                        )
                        Text(
                            text = destinationName,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color(0xFFD7E8B6)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$arrivalTime · $arrivalDate",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color(0xFFAEC48A)
                        )
                    }
                }
            }

            // ----------------------------------------------------------------
            // SECTION B: PASSENGER MANIFEST & LIVE STATUS TABLE
            // ----------------------------------------------------------------
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
                        text = "VERIFIED PASSENGER STATUS (${passengers.size})",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        color = TactilePaperPassTokens.InkMuted
                    )
                    Text(
                        text = "INITIAL → CURRENT",
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
                            // Passenger Index Badge
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
                                    fontSize = 12.sp,
                                    color = if (pax.isPayer) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.InkSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = pax.name,
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TactilePaperPassTokens.InkPrimary
                                )
                                Text(
                                    text = if (pax.isPayer) "Ticket Payer · Paid $totalFareDisplay" else "Share: $perPersonShareDisplay",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = TactilePaperPassTokens.InkSecondary
                                )
                            }
                        }

                        // Booking -> Current Status Pill
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

            // ----------------------------------------------------------------
            // SECTION C: PHYSICAL PERFORATED TEAR-OFF LINE
            // ----------------------------------------------------------------
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            ) {
                val yCenter = size.height / 2f
                drawLine(
                    color = TactilePaperPassTokens.PerforationLine,
                    start = Offset(18.dp.toPx(), yCenter),
                    end = Offset(size.width - 18.dp.toPx(), yCenter),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                )
            }

            // ----------------------------------------------------------------
            // SECTION D: BOTTOM TICKET STUB (TOTAL FARE & BARCODE)
            // ----------------------------------------------------------------
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
                        text = "TOTAL VERIFIED FARE",
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
                            fontSize = 26.sp,
                            letterSpacing = (-0.5).sp,
                            color = TactilePaperPassTokens.InkPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TactilePaperPassTokens.SageConfirmedBg
                        ) {
                            Text(
                                text = "$perPersonShareDisplay / person",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = TactilePaperPassTokens.SageConfirmedText,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Realistic Vertical Barcode + PNR Stamp
                Column(horizontalAlignment = Alignment.End) {
                    TactileBarcode(
                        modifier = Modifier
                            .width(96.dp)
                            .height(28.dp)
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
// 7. INTERACTIVE GROUP MEMBER SELECTION & SPLIT BREAKDOWN CARD
// ============================================================================
@Composable
private fun MemberSplitSelectionCard(
    groupName: String,
    members: List<GroupMemberEntity>,
    selectedMemberIds: Set<String>,
    onToggleMember: (String) -> Unit,
    onSelectAll: () -> Unit,
    totalFareDisplay: String,
    perMemberShareDisplay: String,
    payerReimbursementDisplay: String,
    includeIrctcStatutoryFees: Boolean,
    onToggleStatutoryFees: () -> Unit,
    convenienceFeeDisplay: String,
    insuranceFeeDisplay: String
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = TactilePaperPassTokens.PaperSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.HairlineBorder),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Splitting equally among X members + Select All pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Groups,
                        contentDescription = null,
                        tint = TactilePaperPassTokens.ForestTop,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Splitting equally among ${selectedMemberIds.size} members",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TactilePaperPassTokens.InkPrimary
                    )
                }

                Surface(
                    onClick = onSelectAll,
                    shape = RoundedCornerShape(999.dp),
                    color = TactilePaperPassTokens.SageConfirmedBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.SageConfirmedBorder)
                ) {
                    Text(
                        text = if (selectedMemberIds.size == members.size) "$perMemberShareDisplay / each" else "Select All (${members.size})",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TactilePaperPassTokens.SageConfirmedText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Optional IRCTC Statutory Fee Breakdown Banner (toggleable)
            Surface(
                onClick = onToggleStatutoryFees,
                shape = RoundedCornerShape(14.dp),
                color = if (includeIrctcStatutoryFees) TactilePaperPassTokens.SageConfirmedBg.copy(alpha = 0.65f) else TactilePaperPassTokens.PaperStubSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (includeIrctcStatutoryFees) TactilePaperPassTokens.SageConfirmedBorder else TactilePaperPassTokens.HairlineBorder
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (includeIrctcStatutoryFees)
                                "✓ IRCTC All-Inclusive Bill ($totalFareDisplay)"
                            else
                                "Base Ticket Fare ($totalFareDisplay) · Tap to add IRCTC Fees",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (includeIrctcStatutoryFees) TactilePaperPassTokens.SageConfirmedText else TactilePaperPassTokens.InkPrimary
                        )
                        Text(
                            text = "Convenience Fee $convenienceFeeDisplay (incl. GST) + Insurance $insuranceFeeDisplay",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = TactilePaperPassTokens.InkSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (includeIrctcStatutoryFees) TactilePaperPassTokens.ForestTop else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TactilePaperPassTokens.HairlineBorder)
                    ) {
                        Text(
                            text = if (includeIrctcStatutoryFees) "INCLUDED" else "+ ADD",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            color = if (includeIrctcStatutoryFees) Color.White else TactilePaperPassTokens.InkPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Member Rows (Tap to toggle inclusion on this Train Ticket)
            members.forEachIndexed { index, member ->
                val isSelected = member.memberId in selectedMemberIds
                val rowBg by animateColorAsState(
                    targetValue = if (isSelected) Color.Transparent else TactilePaperPassTokens.PaperStubSurface.copy(alpha = 0.6f),
                    animationSpec = tween(180),
                    label = "memberRowBg"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(rowBg)
                        .clickable { onToggleMember(member.memberId) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Tactile Checkbox / Avatar Badge
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !isSelected -> Color(0xFFEAE4D7)
                                        member.isCurrentUser -> TactilePaperPassTokens.ForestTop
                                        else -> TactilePaperPassTokens.SageConfirmedBg
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) TactilePaperPassTokens.SageConfirmedBorder else TactilePaperPassTokens.HairlineBorder,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = if (member.isCurrentUser) Color.White else TactilePaperPassTokens.SageConfirmedText,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Excluded",
                                    tint = TactilePaperPassTokens.InkMuted,
                                    modifier = Modifier.size(16.dp)
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
                                    !isSelected -> "Not on this ticket · Excluded"
                                    member.isCurrentUser -> "Paid full $totalFareDisplay"
                                    else -> "Owes you · via UPI"
                                },
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isSelected) TactilePaperPassTokens.InkSecondary else TactilePaperPassTokens.InkMuted
                            )
                        }
                    }

                    // Right-aligned Share & Payer Reimbursement
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isSelected) perMemberShareDisplay else "₹0",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (isSelected) TactilePaperPassTokens.InkPrimary else TactilePaperPassTokens.InkMuted
                        )
                        if (member.isCurrentUser && isSelected) {
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
                    color = TactilePaperPassTokens.InkPrimary,
                    topLeft = Offset(currentX, 0f),
                    size = Size(w, size.height)
                )
            }
            currentX += w + (2.5f * scale)
        }
    }
}
