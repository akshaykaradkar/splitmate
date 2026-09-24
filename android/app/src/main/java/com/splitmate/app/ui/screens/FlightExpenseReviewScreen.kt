package com.splitmate.app.ui.screens

import android.view.SoundEffectConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.splitmate.app.SplitMateMathEngine
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.data.PnrNetworkRepository
import com.splitmate.app.data.UniversalFlightTicketExtractor
import com.splitmate.app.ui.ParsedTravelTicket
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.extractInitialsFromNameOrSeed
import com.splitmate.app.ui.formatTravelExpenseTitle
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

// ==============================================================================
// 1. MATERIAL 3 EXPRESSIVE & LUXURY AVIATION TOKENS (LIGHT & DARK ADAPTIVE)
// ==============================================================================
object FlightPassTokens {
    val AppBackground: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF141311) else Color(0xFFFAF7F2)
    val PrimaryDark: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFF4EFEA) else Color(0xFF23201E)
    val AviationNavy = Color(0xFF2B2768)             // Warm Periwinkle-Indigo Dusk Header
    val AviationNavyGradient = Color(0xFF1B1849)     // Deep Periwinkle Midnight
    val SkyBlue: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF282552) else Color(0xFFEEF2FF)
    val SkyBlueText: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFDCE3FD) else Color(0xFF2B2768)
    val TicketPaperWhite: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF1F1D1A) else Color(0xFFFFFFFF)
    val TicketPaperEdge: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF38342E) else Color(0xFFEAE6DF)
    val StatusGreenSurface: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF233316) else Color(0xFFEAF3DC)
    val StatusGreenText: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFD7E8B6) else Color(0xFF2D4810)
    val StatusGreenDot = Color(0xFF4CAF50)           // Active Live Status Beacon
    val BorderSubtle: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF322E28) else Color(0xFFEFECE6)
    val BorderDashed: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF4A443C) else Color(0xFFD6CFC3)
    val TextSecondary: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFB8B0A4) else Color(0xFF756F68)
    val TextMuted: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF857D73) else Color(0xFF9E978E)
    val AccentSageGlow = Color(0xFFD7E8B6)           // Glowing Check Icon Accent

    // Expressive Radii
    val RadiusCardCorner = 24.dp
    val RadiusInner = RoundedCornerShape(16.dp)
    val RadiusPill = RoundedCornerShape(999.dp)
}

private fun formatFlightPaiseExact(paise: Long): String {
    val wholeRupees = paise / 100L
    val remPaise = kotlin.math.abs(paise % 100L)
    val formattedRupees = NumberFormat.getNumberInstance(Locale("en", "IN")).format(wholeRupees)
    return if (remPaise == 0L) {
        "₹$formattedRupees"
    } else {
        String.format(Locale.US, "₹%s.%02d", formattedRupees, remPaise)
    }
}

// Data Models
data class FlightPassenger(
    val id: String,
    val passengerNumber: String,
    val name: String,
    val roleSubtitle: String,
    val seatNumber: String,
    val seatType: String
)

data class FlightSplitMember(
    val id: String,
    val name: String,
    val initials: String,
    val shareAmount: Long,
    val shareAmountPaise: Long = shareAmount * 100L,
    val avatarBg: Color,
    val avatarFg: Color,
    val isPayer: Boolean = false,
    val isSelected: Boolean = true
)

private val MemberAvatarPalette = listOf(
    Color(0xFFD7E8B6) to Color(0xFF2D4810),
    Color(0xFFFFD8CC) to Color(0xFF8A2E1A),
    Color(0xFFD0E2FF) to Color(0xFF143E82),
    Color(0xFFD3D7FD) to Color(0xFF343B80),
    Color(0xFFFCE3D7) to Color(0xFF7C2D12),
    Color(0xFFE0F2FE) to Color(0xFF075985)
)

// ==============================================================================
// 2. LUXURY PERFORATED BOARDING PASS SHAPE (16dp Semicircular Inward Notches)
// ==============================================================================
class FlightBoardingPassShape(
    private val cornerRadius: Float,
    private val notchRadius: Float,
    private val notchYPercent: Float = 0.765f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            val notchY = h * notchYPercent

            // Top-left corner
            moveTo(0f, cornerRadius)
            arcTo(
                rect = Rect(0f, 0f, 2 * cornerRadius, 2 * cornerRadius),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Top edge to top-right
            lineTo(w - cornerRadius, 0f)
            arcTo(
                rect = Rect(w - 2 * cornerRadius, 0f, w, 2 * cornerRadius),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Right edge down to notch
            lineTo(w, notchY - notchRadius)
            // Right semicircle cutout (inward bite into ticket body)
            arcTo(
                rect = Rect(w - notchRadius, notchY - notchRadius, w + notchRadius, notchY + notchRadius),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            // Right edge to bottom-right corner
            lineTo(w, h - cornerRadius)
            arcTo(
                rect = Rect(w - 2 * cornerRadius, h - 2 * cornerRadius, w, h),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Bottom edge to bottom-left corner
            lineTo(cornerRadius, h)
            arcTo(
                rect = Rect(0f, h - 2 * cornerRadius, 2 * cornerRadius, h),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Left edge up to notch
            lineTo(0f, notchY + notchRadius)
            // Left semicircle cutout (inward bite into ticket body)
            arcTo(
                rect = Rect(-notchRadius, notchY - notchRadius, notchRadius, notchY + notchRadius),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            // Close up to top-left
            lineTo(0f, cornerRadius)
            close()
        }
        return Outline.Generic(path)
    }
}

// ==============================================================================
// 3. MAIN COMPOSABLE: FlightExpenseReviewScreen
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightExpenseReviewScreen(
    viewModel: SplitMateViewModel,
    extractedTicket: UniversalFlightTicketExtractor.UniversalFlightTicketResult,
    onPickAnotherPdfClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onConfirmAndAddToLedger: (totalAirfare: Long) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isAudioSensoryEnabled by remember { mutableStateOf(true) }
    var showGroupDropdown by remember { mutableStateOf(false) }
    var showPayerDropdown by remember { mutableStateOf(false) }

    val activeGroup = uiState.activeGroup ?: uiState.groups.firstOrNull()
    val groupMembers = remember(uiState.members, activeGroup?.groupId) {
        if (activeGroup != null) {
            uiState.members.filter { it.groupId == activeGroup.groupId }
        } else {
            emptyList()
        }
    }

    // Determine initial selected members from PDF passenger matching (or all group members if none matched)
    var selectedMemberIds by remember(activeGroup?.groupId, extractedTicket.pnr, groupMembers) {
        val matchedSet = extractedTicket.matchedGroupMembers.map { it.lowercase(Locale.US) }.toSet()
        val autoMatchedIds = groupMembers.filter { member ->
            val firstToken = member.name.substringBefore(" ").trim().lowercase(Locale.US)
            firstToken in matchedSet || extractedTicket.passengers.any { pax ->
                pax.fullName.contains(firstToken, ignoreCase = true)
            }
        }.map { it.memberId }.toSet()
        mutableStateOf(autoMatchedIds.ifEmpty { groupMembers.map { it.memberId }.toSet() })
    }

    var selectedPayerId by remember(activeGroup?.groupId, groupMembers) {
        val currentUser = groupMembers.firstOrNull { it.isCurrentUser }
        mutableStateOf(currentUser?.memberId ?: groupMembers.firstOrNull()?.memberId.orEmpty())
    }

    // Optional live flight status / gate enrichment (non-blocking; instant offline fallback)
    var liveFlightHint by remember(extractedTicket.flightNumber) { mutableStateOf("") }
    LaunchedEffect(extractedTicket.flightNumber) {
        if (extractedTicket.flightNumber.isNotBlank()) {
            val status = PnrNetworkRepository.fetchLiveFlightStatusByNumber(extractedTicket.flightNumber)
            if (status != null && status.isLiveVerified) {
                liveFlightHint = status.liveTrainLocationRadar
            }
        }
    }

    val totalAirfarePaise = remember(extractedTicket.totalFarePaise) {
        if (extractedTicket.totalFarePaise > 0L) extractedTicket.totalFarePaise else 0L
    }
    val totalAirfareRupees = (totalAirfarePaise / 100.0).roundToLong()
    val pnrCode = extractedTicket.pnr.ifBlank { "FLIGHT" }

    // Map extracted passengers into FlightPassenger UI model
    val uiPassengers = remember(extractedTicket) {
        if (extractedTicket.passengers.isNotEmpty()) {
            extractedTicket.passengers.mapIndexed { idx, pax ->
                val rawSeat = pax.seatNumber.takeIf { it.isNotBlank() && it != "-" } ?: "Assigned at Check-in"
                val formattedSeat = if (rawSeat.startsWith("Seat", ignoreCase = true) || rawSeat == "Assigned at Check-in") {
                    rawSeat
                } else {
                    "Seat $rawSeat"
                }
                val seatLetter = rawSeat.lastOrNull()?.uppercaseChar()
                val seatPositionType = when (seatLetter) {
                    'A', 'F', 'K' -> "Window"
                    'C', 'D', 'G', 'H' -> "Aisle"
                    'B', 'E', 'J' -> "Middle"
                    else -> pax.passengerType.lowercase(Locale.US).replaceFirstChar { it.uppercaseChar() }
                }
                val roleLabel = buildString {
                    append(pax.passengerType.lowercase(Locale.US).replaceFirstChar { it.uppercaseChar() })
                    if (idx == 0) append(" · Primary Ticket Holder")
                }
                FlightPassenger(
                    id = "P${idx + 1}",
                    passengerNumber = "P${idx + 1}",
                    name = pax.fullName,
                    roleSubtitle = roleLabel,
                    seatNumber = formattedSeat,
                    seatType = seatPositionType
                )
            }
        } else {
            listOf(
                FlightPassenger(
                    id = "P1",
                    passengerNumber = "P1",
                    name = uiState.currentUserName.ifBlank { "Passenger 1" },
                    roleSubtitle = "Confirmed Adult",
                    seatNumber = "Confirmed",
                    seatType = extractedTicket.cabinClass.ifBlank { "Economy" }
                )
            )
        }
    }

    // Calculate exact 0.00¢ drift Largest Remainder split across selected group members
    val splitAllocationsPaise: Map<String, Long> = remember(totalAirfarePaise, selectedMemberIds) {
        val activeIds = selectedMemberIds.toList()
        if (activeIds.isEmpty() || totalAirfarePaise <= 0L) {
            emptyMap()
        } else {
            val count = activeIds.size.toLong()
            val baseShare = totalAirfarePaise / count
            val remainder = (totalAirfarePaise % count).toInt()
            activeIds.mapIndexed { index, memberId ->
                memberId to (baseShare + if (index < remainder) 1L else 0L)
            }.toMap()
        }
    }

    val ledgerSplitMembers = remember(groupMembers, selectedMemberIds, selectedPayerId, splitAllocationsPaise) {
        groupMembers.mapIndexed { idx, member ->
            val (bg, fg) = MemberAvatarPalette[idx % MemberAvatarPalette.size]
            val isSelected = member.memberId in selectedMemberIds
            val memberSharePaise = if (isSelected) (splitAllocationsPaise[member.memberId] ?: 0L) else 0L
            val displayName = if (member.isCurrentUser) "${member.name} (You)" else member.name
            FlightSplitMember(
                id = member.memberId,
                name = displayName,
                initials = extractInitialsFromNameOrSeed(member.name.ifBlank { member.avatarSeed }),
                shareAmount = (memberSharePaise / 100.0).roundToLong(),
                shareAmountPaise = memberSharePaise,
                avatarBg = bg,
                avatarFg = fg,
                isPayer = member.memberId == selectedPayerId,
                isSelected = isSelected
            )
        }
    }

    val payerMemberName = remember(groupMembers, selectedPayerId) {
        val m = groupMembers.find { it.memberId == selectedPayerId }
        if (m == null) "You" else if (m.isCurrentUser) "You (${m.name})" else m.name
    }

    val nonPayerNames = remember(ledgerSplitMembers) {
        ledgerSplitMembers.filter { it.isSelected && !it.isPayer }.map { it.name.substringBefore(" (") }
    }

    // Enforce Tabular Numerals (tnum) and SplitMate Brand Typography (Plus Jakarta Sans / Figtree)
    val tabularTextStyle = LocalTextStyle.current.copy(
        fontFamily = SplitMateTheme.FontRounded,
        fontFeatureSettings = "tnum"
    )

    CompositionLocalProvider(LocalTextStyle provides tabularTextStyle) {
        Scaffold(
            containerColor = FlightPassTokens.AppBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Review Flight Expense",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlightPassTokens.PrimaryDark
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(FlightPassTokens.StatusGreenDot)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Confirmed Offline Vault · ${activeGroup?.name ?: "Trip Group"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FlightPassTokens.StatusGreenText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBackClick()
                            },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                border = BorderStroke(1.dp, FlightPassTokens.BorderSubtle),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back",
                                        tint = FlightPassTokens.PrimaryDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Upload Another PDF Action Button
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPickAnotherPdfClick()
                            }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = FlightPassTokens.SkyBlue,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.UploadFile,
                                        contentDescription = "Upload Another Flight PDF",
                                        tint = FlightPassTokens.SkyBlueText,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }

                        // Switch Group Dropdown Button
                        Box {
                            Surface(
                                onClick = { showGroupDropdown = true },
                                shape = CircleShape,
                                color = Color(0xFF335E26),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Groups,
                                        contentDescription = "Switch Group",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showGroupDropdown,
                                onDismissRequest = { showGroupDropdown = false }
                            ) {
                                uiState.groups.forEach { grp ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = grp.name,
                                                fontWeight = if (grp.groupId == activeGroup?.groupId) FontWeight.ExtraBold else FontWeight.Medium
                                            )
                                        },
                                        onClick = {
                                            viewModel.selectActiveGroup(grp.groupId)
                                            showGroupDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = FlightPassTokens.AppBackground)
                )
            },
            bottomBar = {
                Surface(
                    color = FlightPassTokens.AppBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    val formattedButtonTotal = NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalAirfareRupees)
                    Button(
                        onClick = {
                            if (selectedMemberIds.isEmpty() || totalAirfarePaise <= 0L) return@Button
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val parsedTicket = extractedTicket.toParsedTravelTicket().copy(
                                coachAndSeats = "${extractedTicket.cabinClass.ifBlank { "Economy" }} · ${selectedMemberIds.size} Pax"
                            )
                            val formattedTitle = formatTravelExpenseTitle(
                                baseCategory = "✈️ ${extractedTicket.airlineName.ifBlank { "Flight" }} ${extractedTicket.flightNumber} (${extractedTicket.originIata} → ${extractedTicket.destinationIata})",
                                ticket = parsedTicket
                            )
                            viewModel.commitQuickEqualExpense(
                                title = formattedTitle,
                                totalAmountCents = totalAirfarePaise,
                                selectedMemberIds = selectedMemberIds.toList(),
                                payerMemberId = selectedPayerId
                            )
                            onConfirmAndAddToLedger(totalAirfareRupees)
                        },
                        enabled = selectedMemberIds.isNotEmpty() && totalAirfarePaise > 0L,
                        shape = FlightPassTokens.RadiusPill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FlightPassTokens.PrimaryDark,
                            contentColor = FlightPassTokens.AccentSageGlow
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .shadow(12.dp, FlightPassTokens.RadiusPill, spotColor = Color(0x3323201E)),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = FlightPassTokens.AccentSageGlow,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Confirm & Add ₹$formattedButtonTotal to Ledger",
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. PNR SYNC PILL
                item {
                    val subtitleInfo = buildString {
                        append(extractedTicket.airlineName.ifBlank { "Airline E-Ticket" })
                        if (extractedTicket.fareType.isNotBlank()) {
                            append(" · ${extractedTicket.fareType}")
                        } else {
                            append(" · Offline Vault Locked")
                        }
                        append(" · ${uiPassengers.size} Traveller${if (uiPassengers.size > 1) "s" else ""}")
                    }
                    val syncRightLabel = if (liveFlightHint.isNotBlank()) {
                        liveFlightHint
                    } else {
                        "Parsed in ${extractedTicket.extractionDurationMs}ms"
                    }
                    PnrSyncStatusBanner(
                        pnr = pnrCode,
                        subtitleText = subtitleInfo,
                        rightStatusLabel = syncRightLabel
                    )
                }

                // 2. AUDIO & SHADOW SENSORY TOGGLE PILL
                item {
                    PaperSensoryFeedbackBanner(
                        isFeedbackEnabled = isAudioSensoryEnabled,
                        onToggleFeedback = { isAudioSensoryEnabled = it }
                    )
                }

                // 3. INTERACTIVE 3D FOLDABLE BOARDING PASS WITH ANIMATED PAPER SHADOW & SOUND
                item {
                    val cabinSubtitle = listOf(
                        extractedTicket.cabinClass.ifBlank { "Economy" },
                        extractedTicket.fareType.ifBlank { "Confirmed" }
                    ).joinToString(" · ")
                    val routeSubtitle = if (extractedTicket.viaAirports.isNotEmpty()) {
                        "Via ${extractedTicket.viaAirports.joinToString(", ")}"
                    } else {
                        "${extractedTicket.travelDate.ifBlank { "Confirmed" }} · Non-Stop"
                    }
                    val baggageSummary = buildString {
                        val cab = extractedTicket.cabinBaggage.substringBefore("(").trim().ifBlank { "7 Kgs" }
                        val chk = extractedTicket.checkInBaggage.substringBefore("(").trim().ifBlank { "15 Kgs" }
                        append("Cabin $cab + Check-in $chk")
                    }
                    val firstPax = uiPassengers.firstOrNull()?.name ?: "PASSENGER"
                    val bcbpSurname = firstPax.substringAfterLast(" ").uppercase(Locale.US)
                    val bcbpGiven = firstPax.substringBeforeLast(" ", "").uppercase(Locale.US).ifBlank { "PAX" }
                    val bcbpDisplay = "M1$bcbpSurname/$bcbpGiven E${extractedTicket.originIata}${extractedTicket.destinationIata}${extractedTicket.flightNumber} · PNR $pnrCode"

                    val perSeatDisplay = if (selectedMemberIds.isNotEmpty()) {
                        val perMemberRupees = (totalAirfareRupees / selectedMemberIds.size.coerceAtLeast(1))
                        "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(perMemberRupees)} × ${selectedMemberIds.size} Split · Taxes Included"
                    } else {
                        "Taxes & Airport Fees Included"
                    }

                    AnimatedLuxuryAirlineBoardingPass(
                        airlineName = extractedTicket.airlineName.ifBlank { "Flight" },
                        flightNumber = extractedTicket.flightNumber.ifBlank { pnrCode },
                        aircraftType = cabinSubtitle,
                        gateNumber = extractedTicket.travelDate.ifBlank { "Confirmed" },
                        boardingTime = extractedTicket.departureTime.ifBlank { "On Time" },
                        originCode = extractedTicket.originIata.ifBlank { "ORG" },
                        originAirportName = extractedTicket.originCity.ifBlank { extractedTicket.originAirportName.ifBlank { "Origin Airport" } },
                        departureTime = extractedTicket.departureTime.ifBlank { "--:--" },
                        destinationCode = extractedTicket.destinationIata.ifBlank { "DST" },
                        destinationAirportName = extractedTicket.destinationCity.ifBlank { extractedTicket.destinationAirportName.ifBlank { "Destination Airport" } },
                        arrivalTime = extractedTicket.arrivalTime.ifBlank { "--:--" },
                        flightDuration = extractedTicket.durationText.ifBlank { "Direct" },
                        flightDistance = routeSubtitle,
                        pnrNumber = pnrCode,
                        passengers = uiPassengers,
                        totalAirfare = totalAirfareRupees,
                        perSeatSummaryText = perSeatDisplay,
                        eTicketReference = extractedTicket.otaBookingId.ifBlank { "PNR-$pnrCode" },
                        baggageLabel = baggageSummary,
                        bcbpBarcodeText = bcbpDisplay,
                        isSensorySoundEnabled = isAudioSensoryEnabled
                    )
                }

                // 4. BELOW THE TICKET: Clean Minimal Ledger Split Breakdown
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Splitting ${selectedMemberIds.size} Way${if (selectedMemberIds.size == 1) "" else "s"} Equally",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlightPassTokens.PrimaryDark
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Payer Switcher Pill
                                Box {
                                    Surface(
                                        onClick = { showPayerDropdown = true },
                                        shape = FlightPassTokens.RadiusPill,
                                        color = FlightPassTokens.SkyBlue
                                    ) {
                                        Text(
                                            text = "Paid by: $payerMemberName ▾",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FlightPassTokens.SkyBlueText,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showPayerDropdown,
                                        onDismissRequest = { showPayerDropdown = false }
                                    ) {
                                        groupMembers.forEach { m ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = if (m.isCurrentUser) "${m.name} (You)" else m.name,
                                                        fontWeight = if (m.memberId == selectedPayerId) FontWeight.ExtraBold else FontWeight.Medium
                                                    )
                                                },
                                                onClick = {
                                                    selectedPayerId = m.memberId
                                                    showPayerDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = FlightPassTokens.RadiusPill,
                                    color = FlightPassTokens.StatusGreenSurface
                                ) {
                                    Text(
                                        text = "Exact Split · 100% Balanced",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FlightPassTokens.StatusGreenText,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val baseSharePaise = if (selectedMemberIds.isNotEmpty()) {
                                totalAirfarePaise / selectedMemberIds.size.toLong()
                            } else 0L
                            ledgerSplitMembers.forEach { member ->
                                val returnsPaise = (totalAirfarePaise - member.shareAmountPaise).coerceAtLeast(0L)
                                val absorbedPlusOnePaise = member.isSelected && member.shareAmountPaise > baseSharePaise && (totalAirfarePaise % selectedMemberIds.size.coerceAtLeast(1) != 0L)
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedMemberIds = if (member.isSelected && selectedMemberIds.size > 1) {
                                            selectedMemberIds - member.id
                                        } else {
                                            selectedMemberIds + member.id
                                        }
                                    },
                                    shape = FlightPassTokens.RadiusInner,
                                    color = if (member.isSelected) FlightPassTokens.TicketPaperWhite else FlightPassTokens.TicketPaperWhite.copy(alpha = 0.55f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (member.isSelected) FlightPassTokens.BorderSubtle else FlightPassTokens.BorderSubtle.copy(alpha = 0.5f)
                                    ),
                                    shadowElevation = if (member.isSelected) 1.dp else 0.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 13.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = member.avatarBg,
                                                modifier = Modifier.size(42.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = member.initials,
                                                        fontSize = 14.5.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = member.avatarFg
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = member.name,
                                                        fontSize = 14.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = FlightPassTokens.PrimaryDark
                                                    )
                                                    if (member.isPayer) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Surface(
                                                            shape = FlightPassTokens.RadiusPill,
                                                            color = FlightPassTokens.StatusGreenSurface
                                                        ) {
                                                            Text(
                                                                text = "Paid full ${formatFlightPaiseExact(totalAirfarePaise)} · Returns ${formatFlightPaiseExact(returnsPaise)}",
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = FlightPassTokens.StatusGreenText,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = when {
                                                        !member.isSelected -> "Tap to include in flight split"
                                                        absorbedPlusOnePaise && member.isPayer -> "Payer's Share · +₹0.01 Largest Remainder (0.00¢ drift)"
                                                        absorbedPlusOnePaise -> "Owes $payerMemberName · +₹0.01 Largest Remainder (0.00¢ drift)"
                                                        member.isPayer -> "Payer's Share"
                                                        else -> "Owes $payerMemberName · via UPI Request"
                                                    },
                                                    fontSize = 11.5.sp,
                                                    color = FlightPassTokens.TextSecondary
                                                )
                                            }
                                        }

                                        Text(
                                            text = formatFlightPaiseExact(member.shareAmountPaise),
                                            fontFamily = SplitMateTheme.FontDisplay,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (member.isSelected) FlightPassTokens.PrimaryDark else FlightPassTokens.TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Automated Reminders Notice Card
                        Surface(
                            shape = FlightPassTokens.RadiusInner,
                            color = FlightPassTokens.StatusGreenSurface.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color(0xFFD2E6BA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = FlightPassTokens.StatusGreenText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Offline Vault & UPI Reminders Ready",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FlightPassTokens.PrimaryDark
                                    )
                                    Text(
                                        text = if (nonPayerNames.isNotEmpty()) {
                                            "PNR $pnrCode is locked offline forever. UPI links will be queued for ${nonPayerNames.joinToString(", ")} once confirmed."
                                        } else {
                                            "PNR $pnrCode is locked in your Offline Vault forever (0 internet needed for return trip)."
                                        },
                                        fontSize = 11.sp,
                                        color = FlightPassTokens.TextSecondary
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

// ==============================================================================
// 4. SUB-COMPONENT: PaperSensoryFeedbackBanner (Audio, Haptic & Shadow Sync)
// ==============================================================================
@Composable
fun PaperSensoryFeedbackBanner(
    isFeedbackEnabled: Boolean = true,
    onToggleFeedback: (Boolean) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Paper Sensory Feedback",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = FlightPassTokens.TextSecondary
        )

        Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggleFeedback(!isFeedbackEnabled)
            },
            shape = FlightPassTokens.RadiusPill,
            color = if (isFeedbackEnabled) Color(0xFFEAF3DC) else Color(0xFFEFECE6),
            border = BorderStroke(1.dp, if (isFeedbackEnabled) Color(0xFFC7E2A4) else Color(0xFFDCD6CC))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isFeedbackEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                    contentDescription = null,
                    tint = if (isFeedbackEnabled) Color(0xFF2D4810) else Color(0xFF756F68),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isFeedbackEnabled) "Audio & Shadow Sync: ON" else "Audio: OFF",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFeedbackEnabled) Color(0xFF2D4810) else Color(0xFF756F68)
                )
                if (isFeedbackEnabled) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(FlightPassTokens.StatusGreenDot)
                    )
                }
            }
        }
    }
}

// ==============================================================================
// 5. SUB-COMPONENT: PnrSyncStatusBanner
// ==============================================================================
@Composable
fun PnrSyncStatusBanner(
    pnr: String,
    subtitleText: String = "Direct PDF Sync · Offline Vault",
    rightStatusLabel: String = "Offline Locked"
) {
    Surface(
        shape = FlightPassTokens.RadiusInner,
        color = Color.White,
        border = BorderStroke(1.dp, FlightPassTokens.BorderSubtle),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = FlightPassTokens.AviationNavy,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.FlightTakeoff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PNR $pnr",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = FlightPassTokens.PrimaryDark,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = FlightPassTokens.RadiusPill,
                            color = FlightPassTokens.StatusGreenSurface
                        ) {
                            Text(
                                text = "Confirmed CNF",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlightPassTokens.StatusGreenText,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = subtitleText,
                        fontSize = 11.sp,
                        color = FlightPassTokens.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = rightStatusLabel,
                fontSize = 11.sp,
                color = FlightPassTokens.TextMuted
            )
        }
    }
}

// ==============================================================================
// 6. SUB-COMPONENT: AnimatedLuxuryAirlineBoardingPass
//    (Interactive 3D Fold + Subtle Paper Shadow Animation Synced with Sound)
// ==============================================================================
@Composable
fun AnimatedLuxuryAirlineBoardingPass(
    airlineName: String,
    flightNumber: String,
    aircraftType: String,
    gateNumber: String,
    boardingTime: String,
    originCode: String,
    originAirportName: String,
    departureTime: String,
    destinationCode: String,
    destinationAirportName: String,
    arrivalTime: String,
    flightDuration: String,
    flightDistance: String,
    pnrNumber: String,
    passengers: List<FlightPassenger>,
    totalAirfare: Long,
    perSeatSummaryText: String = "Taxes & Fees Included",
    eTicketReference: String = "E-Ticket",
    baggageLabel: String = "Cabin 7kg + Check-in 15kg",
    bcbpBarcodeText: String = "",
    isSensorySoundEnabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    var isFolded by remember { mutableStateOf(false) }

    // Spring-driven fold angle (0 degrees flat -> -72 degrees 3D folded inward)
    val foldAngle by animateFloatAsState(
        targetValue = if (isFolded) -72f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "FoldRotationX"
    )

    // Synchronized layout height multiplier (1.0f unfolded -> 0.14f folded stub) so folding removes phantom blank space!
    val stubUnfoldFraction by animateFloatAsState(
        targetValue = if (isFolded) 0.14f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "StubUnfoldHeightFraction"
    )

    // Dynamic inner crease shadow opacity along the perforation fold
    val creaseShadowAlpha by animateFloatAsState(
        targetValue = if (isFolded) 0.38f else 0.0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "CreaseShadowAlpha"
    )

    // Dynamic ambient floor shadow depth beneath the folded ticket stub
    val stubFloorShadowAlpha by animateFloatAsState(
        targetValue = if (isFolded) 0.22f else 0.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "StubFloorShadowAlpha"
    )

    // Overall card elevation expansion on fold
    val cardElevation by animateDpAsState(
        targetValue = if (isFolded) 22.dp else 16.dp,
        animationSpec = tween(durationMillis = 300),
        label = "CardElevation"
    )

    val passShape = FlightBoardingPassShape(
        cornerRadius = 24f * 3f,
        notchRadius = 16f * 3f,
        notchYPercent = 0.765f
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = cardElevation,
                shape = passShape,
                spotColor = Color(0x380F1D36),
                ambientColor = Color(0x1F0F1D36)
            )
            .clip(passShape)
            .background(FlightPassTokens.TicketPaperWhite)
            .border(1.2.dp, FlightPassTokens.TicketPaperEdge, passShape)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ------------------------------------------------------------------
            // 1. TOP HEADER: DEEP AVIATION MIDNIGHT NAVY (#0F1D36)
            // ------------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                FlightPassTokens.AviationNavy,
                                FlightPassTokens.AviationNavyGradient
                            )
                        )
                    )
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Flight,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$airlineName · $flightNumber",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = aircraftType,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        shape = FlightPassTokens.RadiusPill,
                        color = FlightPassTokens.SkyBlue
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = FlightPassTokens.SkyBlueText,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "$gateNumber · $boardingTime",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = FlightPassTokens.SkyBlueText
                            )
                        }
                    }
                }
            }

            // ------------------------------------------------------------------
            // 2. MIDDLE SECTION: Route Strip & Passenger Allocation
            // ------------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(top = 22.dp, bottom = 10.dp)
            ) {
                // Route Strip: Origin -> Destination
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = originCode.trim().take(3).uppercase(Locale.US),
                            fontFamily = SplitMateTheme.FontDisplay,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = FlightPassTokens.PrimaryDark,
                            letterSpacing = (-1.5).sp,
                            lineHeight = 46.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = originAirportName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FlightPassTokens.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = departureTime,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlightPassTokens.StatusGreenText
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Surface(
                            shape = FlightPassTokens.RadiusPill,
                            color = FlightPassTokens.AppBackground,
                            border = BorderStroke(1.dp, FlightPassTokens.BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = null,
                                    tint = FlightPassTokens.TextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = flightDuration,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FlightPassTokens.PrimaryDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(FlightPassTokens.AviationNavy)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.dp)
                                    .background(FlightPassTokens.BorderDashed)
                            )
                            Icon(
                                imageVector = Icons.Rounded.FlightTakeoff,
                                contentDescription = null,
                                tint = FlightPassTokens.AviationNavy,
                                modifier = Modifier.size(17.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.dp)
                                    .background(FlightPassTokens.BorderDashed)
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(FlightPassTokens.AviationNavy)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = flightDistance,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = FlightPassTokens.TextMuted,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = destinationCode.trim().take(3).uppercase(Locale.US),
                            fontFamily = SplitMateTheme.FontDisplay,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = FlightPassTokens.PrimaryDark,
                            letterSpacing = (-1.5).sp,
                            lineHeight = 46.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = destinationAirportName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FlightPassTokens.TextSecondary,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = arrivalTime,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlightPassTokens.PrimaryDark,
                            textAlign = TextAlign.End
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PASSENGER ALLOCATION (${passengers.size})",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FlightPassTokens.TextSecondary,
                        letterSpacing = 0.8.sp
                    )

                    Surface(
                        shape = FlightPassTokens.RadiusPill,
                        color = FlightPassTokens.StatusGreenSurface
                    ) {
                        Text(
                            text = baggageLabel,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlightPassTokens.StatusGreenText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    passengers.forEach { passenger ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    FlightPassTokens.AppBackground.copy(alpha = 0.6f),
                                    FlightPassTokens.RadiusInner
                                )
                                .border(1.dp, FlightPassTokens.BorderSubtle, FlightPassTokens.RadiusInner)
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${passenger.passengerNumber} · ${passenger.name}",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FlightPassTokens.PrimaryDark
                                )
                                Text(
                                    text = passenger.roleSubtitle,
                                    fontSize = 11.sp,
                                    color = FlightPassTokens.TextSecondary
                                )
                            }

                            Surface(
                                shape = FlightPassTokens.RadiusPill,
                                color = FlightPassTokens.SkyBlue,
                                border = BorderStroke(0.5.dp, Color(0xFFC7DCF4))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.EventSeat,
                                        contentDescription = null,
                                        tint = FlightPassTokens.SkyBlueText,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "${passenger.seatNumber} (${passenger.seatType})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FlightPassTokens.SkyBlueText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ------------------------------------------------------------------
            // 3. TACTILE INTERACTIVE PERFORATION FOLD LINE WITH DYNAMIC CREASE SHADOW
            // ------------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Spring audio & haptic feedback trigger
                        haptic.performHapticFeedback(
                            if (isFolded) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress
                        )
                        if (isSensorySoundEnabled) {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                        }
                        isFolded = !isFolded
                    }
                    .padding(vertical = 4.dp)
            ) {
                // Perforation Dashed Line
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .padding(horizontal = 24.dp)
                        .align(Alignment.Center)
                ) {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                    drawLine(
                        color = FlightPassTokens.BorderDashed,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = pathEffect
                    )
                }

                // Interactive Tap Hint Pill centered on the Perforation line
                Surface(
                    shape = FlightPassTokens.RadiusPill,
                    color = FlightPassTokens.TicketPaperWhite,
                    border = BorderStroke(1.dp, FlightPassTokens.BorderDashed),
                    shadowElevation = 2.dp,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isFolded) Icons.Rounded.UnfoldMore else Icons.Rounded.ContentCut,
                            contentDescription = null,
                            tint = FlightPassTokens.TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isFolded) "Tap to unfold stub" else "Tap fold line to fold receipt",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlightPassTokens.TextSecondary
                        )
                    }
                }
            }

            // ------------------------------------------------------------------
            // 4. BOTTOM TICKET STUB: 3D ROTATION ALONG X-AXIS WITH SYNCHRONIZED
            //    LAYOUT HEIGHT COLLAPSE, PAPER SHADOW & FLOOR AMBIENT PROJECTION
            // ------------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val collapsedHeight = (placeable.height * stubUnfoldFraction).roundToInt().coerceAtLeast(0)
                        layout(placeable.width, collapsedHeight) {
                            placeable.placeRelative(0, 0)
                        }
                    }
                    // Floor shadow projection beneath folded stub
                    .drawBehind {
                        if (stubFloorShadowAlpha > 0f) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x2B2B2768).copy(alpha = stubFloorShadowAlpha),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = size.height * 0.45f
                                )
                            )
                        }
                    }
                    .graphicsLayer {
                        rotationX = foldAngle
                        cameraDistance = 14f * density
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        clip = true
                    }
                    // Realistic creasing gradient shadow darkening along inner paper bend
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E170F).copy(alpha = creaseShadowAlpha),
                                Color(0xFF1E170F).copy(alpha = creaseShadowAlpha * 0.35f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = 70f
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .padding(top = 4.dp, bottom = 22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL GROUP AIRFARE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = FlightPassTokens.TextMuted,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalAirfare)}",
                                fontFamily = SplitMateTheme.FontDisplay,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = FlightPassTokens.PrimaryDark,
                                letterSpacing = (-1.5).sp,
                                lineHeight = 42.sp
                            )
                            Text(
                                text = perSeatSummaryText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FlightPassTokens.StatusGreenText
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "E-Ticket Ref",
                                fontSize = 11.sp,
                                color = FlightPassTokens.TextMuted
                            )
                            Text(
                                text = eTicketReference.take(16),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlightPassTokens.PrimaryDark
                            )
                            Surface(
                                shape = FlightPassTokens.RadiusPill,
                                color = FlightPassTokens.StatusGreenSurface
                            ) {
                                Text(
                                    text = if (isFolded) "Folded Stub" else "Ready for Ledger",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FlightPassTokens.StatusGreenText,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    EngravedAviationBarcode(
                        pnrNumber = pnrNumber,
                        barcodeCaption = bcbpBarcodeText,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ==============================================================================
// 7. HELPER COMPONENT: EngravedAviationBarcode
// ==============================================================================
@Composable
fun EngravedAviationBarcode(
    pnrNumber: String,
    barcodeCaption: String = "",
    modifier: Modifier = Modifier
) {
    val barWidths = listOf(
        3.dp, 1.dp, 2.dp, 4.dp, 1.dp, 2.dp, 1.dp, 3.dp, 1.dp, 4.dp,
        2.dp, 1.dp, 3.dp, 2.dp, 1.dp, 4.dp, 1.dp, 2.dp, 3.dp, 1.dp,
        2.dp, 4.dp, 1.dp, 3.dp, 2.dp, 1.dp, 3.dp, 1.dp, 2.dp, 4.dp,
        1.dp, 3.dp, 2.dp, 4.dp, 1.dp, 2.dp, 1.dp, 3.dp, 2.dp, 1.dp
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            barWidths.forEach { width ->
                Box(
                    modifier = Modifier
                        .width(width)
                        .fillMaxHeight()
                        .background(FlightPassTokens.AviationNavy.copy(alpha = 0.9f))
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = barcodeCaption.ifBlank { "IATA BCBP · PNR $pnrNumber" },
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FlightPassTokens.TextMuted,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
