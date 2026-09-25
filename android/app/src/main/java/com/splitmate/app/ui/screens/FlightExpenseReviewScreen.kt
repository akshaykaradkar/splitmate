package com.splitmate.app.ui.screens

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.splitmate.app.SplitMateMathEngine
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.data.PnrNetworkRepository
import com.splitmate.app.data.UniversalFlightTicketExtractor
import com.splitmate.app.ui.ParsedTravelTicket
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.extractInitialsFromNameOrSeed
import com.splitmate.app.ui.formatTravelExpenseTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.random.Random

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
    val SkyBlueBorder: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF4E48A6) else Color(0xFFC7D2FE)
    val TicketPaperWhite: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF1F1D1A) else Color(0xFFFFFFFF)
    val TicketPaperEdge: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF38342E) else Color(0xFFEAE6DF)
    val StatusGreenSurface: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF233316) else Color(0xFFEAF3DC)
    val StatusGreenText: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFD7E8B6) else Color(0xFF2D4810)
    val StatusGreenBorder: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF3D5428) else Color(0xFFC7E2A4)
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
    val BarcodeBarColor: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFDCE3FD) else Color(0xFF2B2768)

    // Expressive Radii
    val RadiusCardCorner = 24.dp
    val RadiusInner = RoundedCornerShape(16.dp)
    val RadiusPill = RoundedCornerShape(999.dp)
}

private val IGNORED_TITLES = setOf("mr", "mrs", "ms", "miss", "mstr", "master", "dr", "prof", "shri", "smt", "kumari", "adult", "child", "infant")

private fun tokenizePersonName(rawName: String): List<String> {
    return rawName
        .lowercase(Locale.US)
        .replace(Regex("[^a-z\\s]"), " ")
        .split(Regex("\\s+"))
        .map { it.trim() }
        .filter { it.length >= 2 && it !in IGNORED_TITLES }
}

/**
 * Matches a single passenger full name from the flight PDF against the active group's members.
 * Returns the highest-scoring matching [GroupMemberEntity], or null if no member matches.
 */
internal fun matchSinglePassengerToGroupMember(
    passengerFullName: String,
    groupMembers: List<GroupMemberEntity>
): GroupMemberEntity? {
    val paxTokens = tokenizePersonName(passengerFullName)
    if (paxTokens.isEmpty()) return null

    var bestMember: GroupMemberEntity? = null
    var bestScore = 0

    for (member in groupMembers) {
        val memberTokens = tokenizePersonName(member.name)
        if (memberTokens.isEmpty()) continue

        var score = 0
        val memberFirst = memberTokens.first()
        val paxFirst = paxTokens.first()

        // 1. Exact full token-set match
        if (memberTokens.size > 1 && paxTokens.containsAll(memberTokens)) {
            score = 100
        }
        // 2. First name exact match (e.g., "Pratiksha" == "Pratiksha" in "Pratiksha Pandurang Jadhav")
        else if (memberFirst.length >= 3 && memberFirst == paxFirst) {
            score = 90
        }
        // 3. Member's first name matches ANY token in passenger's full name
        else if (memberFirst.length >= 3 && paxTokens.any { it == memberFirst }) {
            score = 80
        }
        // 4. Any token >= 3 chars shared between member and passenger
        else if (memberTokens.any { mTok -> mTok.length >= 3 && paxTokens.any { pTok -> pTok == mTok } }) {
            score = 70
        }
        // 5. Prefix match >= 4 chars (e.g., "Pratik" vs "Pratiksha")
        else if (memberFirst.length >= 4 && paxTokens.any { pTok -> pTok.length >= 4 && (pTok.startsWith(memberFirst) || memberFirst.startsWith(pTok)) }) {
            score = 55
        }

        if (score > bestScore) {
            bestScore = score
            bestMember = member
        }
    }
    return if (bestScore >= 55) bestMember else null
}

/**
 * Returns all matched [GroupMemberEntity]s in the exact passenger order on the flight ticket.
 * This ensures Passenger 1 (Primary Ticket Holder) is the first matched member and is automatically
 * assigned as the Payer!
 */
internal fun findMatchedGroupMembersForFlightTicket(
    extractedTicket: UniversalFlightTicketExtractor.UniversalFlightTicketResult,
    groupMembers: List<GroupMemberEntity>
): List<GroupMemberEntity> {
    if (groupMembers.isEmpty()) return emptyList()
    val orderedMatches = LinkedHashMap<String, GroupMemberEntity>()

    // 1. Match directly against each passenger on the ticket in P1, P2... order
    for (pax in extractedTicket.passengers) {
        val match = matchSinglePassengerToGroupMember(pax.fullName, groupMembers)
        if (match != null) {
            orderedMatches.putIfAbsent(match.memberId, match)
        }
    }

    // 2. Also check extractedTicket.matchedGroupMembers Hints
    for (hintName in extractedTicket.matchedGroupMembers) {
        val match = matchSinglePassengerToGroupMember(hintName, groupMembers)
        if (match != null) {
            orderedMatches.putIfAbsent(match.memberId, match)
        }
    }

    return orderedMatches.values.toList()
}

/**
 * Synthesizes a zero-UI-thread-blocking PCM waveform (`24000 Hz` mono, `235ms`) that combines:
 *  - Stage 1 (`0..125ms`): Crisp paper perforation micro-tear (envelope-modulated noise + 9 perforation tooth clicks).
 *  - Stage 2 (`125..235ms`): Heavy mechanical aviation gate-stamp thud (`94Hz -> 42Hz` resonant impulse).
 */
private fun buildBoardingPassTearAndStampPcm(sampleRate: Int = 24000): ShortArray {
    val durationSec = 0.235
    val totalSamples = (sampleRate * durationSec).toInt()
    val pcm = ShortArray(totalSamples)
    val rng = Random(600304L)
    var prevNoise = 0.0

    for (i in 0 until totalSamples) {
        val t = i.toDouble() / sampleRate
        var sample = 0.0

        // Stage 1: Paper perforation tear (0.000s -> 0.125s)
        if (t <= 0.125) {
            val progress = t / 0.125
            val white = rng.nextDouble(-1.0, 1.0)
            // High-pass filter for crisp paper fiber texture
            val highPass = white - 0.72 * prevNoise
            prevNoise = white
            // 9 discrete perforation tooth snaps along the tear line
            val toothModulation = 0.55 + 0.45 * sin(2.0 * PI * 72.0 * t)
            val env = sin(PI * progress) * (1.0 - 0.25 * progress)
            sample += highPass * toothModulation * env * 0.62
        }

        // Stage 2: Heavy mechanical gate-stamp thud (0.115s -> 0.235s)
        if (t >= 0.115) {
            val stampT = t - 0.115
            val freq = 42.0 + 52.0 * exp(-stampT * 38.0)
            val subBody = sin(2.0 * PI * freq * stampT) + 0.45 * sin(2.0 * PI * (freq * 2.15) * stampT)
            val clickTransient = if (stampT < 0.014) sin(2.0 * PI * 1150.0 * stampT) * exp(-stampT * 180.0) * 0.45 else 0.0
            val stampEnv = exp(-stampT * 26.0)
            sample += (subBody * stampEnv * 0.75) + clickTransient
        }

        val clamped = sample.coerceIn(-0.98, 0.98)
        pcm[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
    }
    return pcm
}

internal fun playBoardingPassTearAndStampOneShot(isSensorySoundEnabled: Boolean = true) {
    if (!isSensorySoundEnabled) return
    kotlinx.coroutines.CoroutineScope(Dispatchers.Default).launch {
        runCatching {
            val sampleRate = 24000
            val pcmData = buildBoardingPassTearAndStampPcm(sampleRate)
            val byteCount = pcmData.size * 2
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteCount)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            audioTrack.write(pcmData, 0, pcmData.size)
            audioTrack.play()
            delay(320L)
            audioTrack.stop()
            audioTrack.release()
        }
    }
}

@Composable
internal fun BoardingPassCommitStampOverlay(
    tearProgress: Float,
    stampScale: Float,
    stampAlpha: Float,
    accentColor: Color,
    stampSubLabel: String
) {
    if (tearProgress <= 0.01f && stampAlpha <= 0.01f) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1B18).copy(alpha = (tearProgress * 0.18f).coerceAtMost(0.22f))),
        contentAlignment = Alignment.Center
    ) {
        // Stage 1 (0..125ms): Progressive Perforation Tear Sweep Line across the pass
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 24.dp)
        ) {
            val sweepX = size.width * tearProgress.coerceIn(0f, 1f)
            val centerY = size.height * 0.5f
            drawLine(
                color = accentColor.copy(alpha = 0.85f),
                start = Offset(0f, centerY),
                end = Offset(sweepX, centerY),
                strokeWidth = 3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
            )
            if (tearProgress in 0.05f..0.98f) {
                drawCircle(
                    color = Color(0xFFD7E8B6),
                    radius = 7.dp.toPx(),
                    center = Offset(sweepX, centerY)
                )
            }
        }

        // Stage 2 (125..310ms): Mechanical Gate-Stamp Impact ([ LOGGED · 0.00¢ DRIFT ])
        if (stampAlpha > 0.01f) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFFCF7).copy(alpha = 0.96f * stampAlpha),
                border = BorderStroke(3.dp, accentColor.copy(alpha = stampAlpha)),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = stampScale
                        scaleY = stampScale
                        alpha = stampAlpha
                        rotationZ = -8.5f
                    }
            ) {
                Box(
                    modifier = Modifier
                        .padding(5.dp)
                        .border(1.dp, accentColor.copy(alpha = 0.65f * stampAlpha), RoundedCornerShape(10.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "LOGGED · 0.00¢ DRIFT",
                                fontFamily = SplitMateTheme.FontDisplay,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                letterSpacing = 1.1.sp,
                                color = accentColor
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stampSubLabel,
                            fontFamily = SplitMateTheme.FontRounded,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF4A443E)
                        )
                    }
                }
            }
        }
    }
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
    val seatType: String,
    val matchedMemberName: String? = null,
    val isMatchedPayer: Boolean = false,
    val avatarSeed: String = ""
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
    val isSelected: Boolean = true,
    val isOnTicket: Boolean = false,
    val avatarSeed: String = ""
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

    val pnrCode = extractedTicket.pnr.ifBlank { "FLIGHT" }

    // Check if this Flight PNR is already logged in the active group OR any other group
    val existingFlightMatchAnyGroup = remember(uiState.expenses, uiState.groups, activeGroup?.groupId, pnrCode) {
        if (pnrCode.isBlank() || pnrCode == "FLIGHT") {
            null
        } else {
            viewModel.findExistingExpenseByPnr(pnrCode, activeGroup?.groupId)
        }
    }

    // If this PNR was already logged in another group and user uploaded the same PDF from Home, switch to that group automatically
    LaunchedEffect(pnrCode, existingFlightMatchAnyGroup?.first?.groupId) {
        val matchedGroupId = existingFlightMatchAnyGroup?.first?.groupId
        if (!matchedGroupId.isNullOrBlank() && activeGroup?.groupId != matchedGroupId) {
            viewModel.selectActiveGroup(matchedGroupId)
            if (uiState.openedGroupDetailId != null) {
                viewModel.openGroupDetail(matchedGroupId)
            }
        }
    }

    val existingFlightExpenseInGroup = remember(uiState.expenses, activeGroup?.groupId, pnrCode, existingFlightMatchAnyGroup) {
        if (pnrCode.isBlank() || pnrCode == "FLIGHT") {
            null
        } else {
            uiState.expenses.firstOrNull { exp ->
                exp.groupId == activeGroup?.groupId && exp.title.contains(pnrCode, ignoreCase = true)
            } ?: existingFlightMatchAnyGroup?.first
        }
    }

    // Match flight ticket passengers against active group members in exact passenger order (P1 first!)
    val matchedTicketMembers = remember(extractedTicket, groupMembers) {
        findMatchedGroupMembersForFlightTicket(extractedTicket, groupMembers)
    }
    val matchedTicketMemberIds = remember(matchedTicketMembers) {
        matchedTicketMembers.map { it.memberId }.toSet()
    }
    val autoMatchedPrimaryPayer = remember(matchedTicketMembers) {
        matchedTicketMembers.firstOrNull()
    }

    // Smart Payer Selection:
    // 1. If editing an already-logged expense for this PNR, preserve its saved payerId.
    // 2. Otherwise, if a flight passenger matches a group member (e.g., "Pratiksha"), AUTOMATICALLY assign that member as the Payer!
    // 3. Only fall back to currentUser (Admin) if no passenger on the ticket matches any member in the group.
    var selectedPayerId by remember(activeGroup?.groupId, extractedTicket.pnr, groupMembers, existingFlightExpenseInGroup?.expenseId) {
        val defaultPayerId = existingFlightExpenseInGroup?.payerId
            ?: autoMatchedPrimaryPayer?.memberId
            ?: groupMembers.firstOrNull { it.isCurrentUser }?.memberId
            ?: groupMembers.firstOrNull()?.memberId.orEmpty()
        mutableStateOf(defaultPayerId)
    }

    // Smart Split Member Selection:
    // - If editing an existing expense, restore its saved split members.
    // - If 2+ group members are on the flight ticket, select those ticket passengers by default.
    // - If only 1 member (e.g. Pratiksha) is on the flight ticket and she is also the Payer, default to splitting across ALL group members
    //   so other members owe her (instead of a 1-way self-split with ₹0 owed), while providing 1-tap toggle pills for "All Group" vs "Ticket Pax"!
    var selectedMemberIds by remember(activeGroup?.groupId, extractedTicket.pnr, groupMembers, existingFlightExpenseInGroup?.expenseId) {
        val existingSplitIds = if (existingFlightExpenseInGroup != null) {
            uiState.splits
                .filter { it.expenseId == existingFlightExpenseInGroup.expenseId && it.finalOwedCents > 0L }
                .map { it.memberId }
                .toSet()
        } else {
            emptySet()
        }
        val initialIds = when {
            existingSplitIds.isNotEmpty() -> existingSplitIds
            matchedTicketMemberIds.size >= 2 -> matchedTicketMemberIds
            else -> groupMembers.map { it.memberId }.toSet()
        }
        mutableStateOf(initialIds)
    }

    // Keep Payer & Split Members synchronized if groupMembers finish loading or an earlier expense is matched
    LaunchedEffect(activeGroup?.groupId, extractedTicket.pnr, groupMembers.size, existingFlightExpenseInGroup?.expenseId) {
        if (existingFlightExpenseInGroup != null) {
            selectedPayerId = existingFlightExpenseInGroup.payerId
            val savedSplits = uiState.splits
                .filter { it.expenseId == existingFlightExpenseInGroup.expenseId && it.finalOwedCents > 0L }
                .map { it.memberId }
                .toSet()
            if (savedSplits.isNotEmpty()) {
                selectedMemberIds = savedSplits
            }
        } else if (autoMatchedPrimaryPayer != null) {
            selectedPayerId = autoMatchedPrimaryPayer.memberId
        } else if (selectedPayerId.isBlank() || groupMembers.none { it.memberId == selectedPayerId }) {
            selectedPayerId = groupMembers.firstOrNull { it.isCurrentUser }?.memberId
                ?: groupMembers.firstOrNull()?.memberId.orEmpty()
        }
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

    val totalAirfarePaise = remember(extractedTicket.totalFarePaise, existingFlightExpenseInGroup?.totalAmountCents) {
        when {
            extractedTicket.totalFarePaise > 0L -> extractedTicket.totalFarePaise
            existingFlightExpenseInGroup != null && existingFlightExpenseInGroup.totalAmountCents > 0L -> existingFlightExpenseInGroup.totalAmountCents
            else -> 0L
        }
    }
    val totalAirfareRupees = (totalAirfarePaise / 100.0).roundToLong()

    // Map extracted passengers into FlightPassenger UI model with matched group member annotations
    val uiPassengers = remember(extractedTicket, groupMembers, selectedPayerId) {
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
                val matchedMember = matchSinglePassengerToGroupMember(pax.fullName, groupMembers)
                val roleLabel = buildString {
                    append(pax.passengerType.lowercase(Locale.US).replaceFirstChar { it.uppercaseChar() })
                    if (matchedMember != null) {
                        append(" · Matched: ${matchedMember.name}")
                        if (matchedMember.memberId == selectedPayerId) {
                            append(" (Payer)")
                        }
                    } else if (idx == 0) {
                        append(" · Primary Ticket Holder")
                    }
                }
                FlightPassenger(
                    id = "P${idx + 1}",
                    passengerNumber = "P${idx + 1}",
                    name = pax.fullName,
                    roleSubtitle = roleLabel,
                    seatNumber = formattedSeat,
                    seatType = seatPositionType,
                    matchedMemberName = matchedMember?.name,
                    isMatchedPayer = matchedMember?.memberId == selectedPayerId,
                    avatarSeed = matchedMember?.avatarSeed?.ifBlank { matchedMember.name } ?: pax.fullName
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
                    seatType = extractedTicket.cabinClass.ifBlank { "Economy" },
                    avatarSeed = groupMembers.firstOrNull { it.isCurrentUser }?.avatarSeed?.ifBlank { uiState.currentUserName }
                        ?: uiState.currentUserName.ifBlank { "Passenger 1" }
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

    val ledgerSplitMembers = remember(groupMembers, selectedMemberIds, selectedPayerId, splitAllocationsPaise, matchedTicketMemberIds) {
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
                isSelected = isSelected,
                isOnTicket = member.memberId in matchedTicketMemberIds,
                avatarSeed = member.avatarSeed.ifBlank { member.name }
            )
        }
    }

    val payerMember = remember(groupMembers, selectedPayerId) {
        groupMembers.find { it.memberId == selectedPayerId }
    }
    val payerMemberName = remember(payerMember) {
        if (payerMember == null) "You" else if (payerMember.isCurrentUser) "You (${payerMember.name})" else payerMember.name
    }
    val isPayerAutoMatchedFromTicket = remember(selectedPayerId, matchedTicketMemberIds) {
        selectedPayerId in matchedTicketMemberIds
    }

    val nonPayerNames = remember(ledgerSplitMembers) {
        ledgerSplitMembers.filter { it.isSelected && !it.isPayer }.map { it.name.substringBefore(" (") }
    }

    val commitScope = rememberCoroutineScope()
    val commitTearProgress = remember { Animatable(0f) }
    val commitStampScale = remember { Animatable(1.75f) }
    val commitStampAlpha = remember { Animatable(0f) }
    var isCommittingBoardingPass by remember { mutableStateOf(false) }

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
                                color = FlightPassTokens.TicketPaperWhite,
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
                                border = BorderStroke(1.dp, FlightPassTokens.SkyBlueBorder),
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
                                color = FlightPassTokens.AviationNavy,
                                border = BorderStroke(1.dp, FlightPassTokens.SkyBlueBorder),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Groups,
                                        contentDescription = "Switch Group",
                                        tint = Color(0xFFEEF2FF),
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        val formattedButtonTotal = NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalAirfareRupees)
                        Button(
                            onClick = {
                                if (selectedMemberIds.isEmpty() || totalAirfarePaise <= 0L || isCommittingBoardingPass) return@Button
                                isCommittingBoardingPass = true
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                playBoardingPassTearAndStampOneShot(isAudioSensoryEnabled)
                                val parsedTicket = extractedTicket.toParsedTravelTicket().copy(
                                    coachAndSeats = "${extractedTicket.cabinClass.ifBlank { "Economy" }} · ${selectedMemberIds.size} Pax"
                                )
                                val formattedTitle = formatTravelExpenseTitle(
                                    baseCategory = "${extractedTicket.airlineName.ifBlank { "Flight" }} ${extractedTicket.flightNumber} (${extractedTicket.originIata} → ${extractedTicket.destinationIata})",
                                    ticket = parsedTicket
                                )
                                commitScope.launch {
                                    commitTearProgress.animateTo(1f, tween(125, easing = FastOutLinearInEasing))
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    launch { commitStampAlpha.animateTo(1f, tween(60)) }
                                    commitStampScale.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(dampingRatio = 0.52f, stiffness = 680f)
                                    )
                                    delay(95L)
                                    if (existingFlightExpenseInGroup != null) {
                                        viewModel.editExistingExpense(
                                            expenseId = existingFlightExpenseInGroup.expenseId,
                                            newTitle = formattedTitle,
                                            newTotalRupees = totalAirfarePaise / 100.0,
                                            newPayerId = selectedPayerId,
                                            selectedMemberIds = selectedMemberIds.toList()
                                        )
                                    } else {
                                        viewModel.commitQuickEqualExpense(
                                            title = formattedTitle,
                                            totalAmountCents = totalAirfarePaise,
                                            selectedMemberIds = selectedMemberIds.toList(),
                                            payerMemberId = selectedPayerId
                                        )
                                    }
                                    onConfirmAndAddToLedger(totalAirfareRupees)
                                }
                            },
                            enabled = selectedMemberIds.isNotEmpty() && totalAirfarePaise > 0L && !isCommittingBoardingPass,
                            shape = FlightPassTokens.RadiusPill,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (SplitMateTheme.isDark) Color(0xFF282552) else FlightPassTokens.AviationNavy,
                                contentColor = FlightPassTokens.AccentSageGlow
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(12.dp, FlightPassTokens.RadiusPill, spotColor = Color(0x332B2768)),
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
                                    text = if (existingFlightExpenseInGroup != null) {
                                        "Already Added · Update Earlier Split (₹$formattedButtonTotal)"
                                    } else {
                                        "Confirm ₹$formattedButtonTotal · Paid by $payerMemberName"
                                    },
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
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

                // 3. INTERACTIVE 3D FOLDABLE BOARDING PASS WITH ANIMATED PAPER TEAR, WALLET POCKET & SOUND
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
                        "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(perMemberRupees)} × ${selectedMemberIds.size} Split · Paid by $payerMemberName"
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

                // 4. BELOW THE TICKET: Smart Passenger-Matched Payer + Clean Minimal Ledger Split Breakdown
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Auto-matched Payer Intelligence Banner (when a flight passenger matches a group member)
                        if (isPayerAutoMatchedFromTicket && payerMember != null) {
                            Surface(
                                shape = FlightPassTokens.RadiusInner,
                                color = FlightPassTokens.SkyBlue,
                                border = BorderStroke(1.dp, FlightPassTokens.SkyBlueBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
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
                                        Icon(
                                            imageVector = Icons.Rounded.FlightTakeoff,
                                            contentDescription = null,
                                            tint = FlightPassTokens.SkyBlueText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Auto-Matched Ticket Holder as Payer: ${payerMember.name}",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = FlightPassTokens.SkyBlueText
                                            )
                                            Text(
                                                text = "\"${payerMember.name}\" is on this boarding pass, so full airfare (${formatFlightPaiseExact(totalAirfarePaise)}) is credited to ${payerMember.name}.",
                                                fontSize = 11.sp,
                                                color = FlightPassTokens.TextSecondary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        onClick = { showPayerDropdown = true },
                                        shape = FlightPassTokens.RadiusPill,
                                        color = FlightPassTokens.TicketPaperWhite,
                                        border = BorderStroke(1.dp, FlightPassTokens.SkyBlueBorder)
                                    ) {
                                        Text(
                                            text = "Change Payer ▾",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FlightPassTokens.SkyBlueText,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
                                        color = FlightPassTokens.SkyBlue,
                                        border = BorderStroke(1.dp, FlightPassTokens.SkyBlueBorder)
                                    ) {
                                        Text(
                                            text = "Paid by: $payerMemberName ▾",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FlightPassTokens.SkyBlueText,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showPayerDropdown,
                                        onDismissRequest = { showPayerDropdown = false }
                                    ) {
                                        groupMembers.forEach { m ->
                                            val isOnTicket = m.memberId in matchedTicketMemberIds
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = if (m.isCurrentUser) "${m.name} (You)" else m.name,
                                                            fontWeight = if (m.memberId == selectedPayerId) FontWeight.ExtraBold else FontWeight.Medium
                                                        )
                                                        if (isOnTicket) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.FlightTakeoff,
                                                                contentDescription = "On Ticket",
                                                                tint = FlightPassTokens.SkyBlueText,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Text(
                                                                text = "On Ticket",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = FlightPassTokens.SkyBlueText
                                                            )
                                                        }
                                                    }
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
                                    color = FlightPassTokens.StatusGreenSurface,
                                    border = BorderStroke(1.dp, FlightPassTokens.StatusGreenBorder)
                                ) {
                                    Text(
                                        text = "0.00¢ Drift",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FlightPassTokens.StatusGreenText,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Quick Split Scope Presets: All Group Members vs Ticket Passengers Only
                        if (matchedTicketMemberIds.isNotEmpty() && matchedTicketMemberIds.size < groupMembers.size) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val isAllSelected = selectedMemberIds.size == groupMembers.size
                            val isOnlyTicketSelected = selectedMemberIds == matchedTicketMemberIds
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedMemberIds = groupMembers.map { it.memberId }.toSet()
                                    },
                                    shape = FlightPassTokens.RadiusPill,
                                    color = if (isAllSelected) FlightPassTokens.SkyBlue else FlightPassTokens.TicketPaperWhite,
                                    border = BorderStroke(1.dp, if (isAllSelected) FlightPassTokens.SkyBlueBorder else FlightPassTokens.BorderSubtle)
                                ) {
                                    Text(
                                        text = "Split: All ${groupMembers.size} Group Members",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAllSelected) FlightPassTokens.SkyBlueText else FlightPassTokens.TextSecondary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedMemberIds = matchedTicketMemberIds
                                    },
                                    shape = FlightPassTokens.RadiusPill,
                                    color = if (isOnlyTicketSelected) FlightPassTokens.SkyBlue else FlightPassTokens.TicketPaperWhite,
                                    border = BorderStroke(1.dp, if (isOnlyTicketSelected) FlightPassTokens.SkyBlueBorder else FlightPassTokens.BorderSubtle)
                                ) {
                                    Text(
                                        text = "Only Ticket Passenger${if (matchedTicketMemberIds.size > 1) "s" else ""} (${matchedTicketMembers.joinToString { it.name.substringBefore(" ") }})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOnlyTicketSelected) FlightPassTokens.SkyBlueText else FlightPassTokens.TextSecondary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
                                        if (member.isPayer) FlightPassTokens.SkyBlueBorder
                                        else if (member.isSelected) FlightPassTokens.BorderSubtle
                                        else FlightPassTokens.BorderSubtle.copy(alpha = 0.5f)
                                    ),
                                    shadowElevation = 0.dp,
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
                                            com.splitmate.app.AvatarToken(
                                                initials = member.avatarSeed.ifBlank { member.name },
                                                bg = member.avatarBg,
                                                textColor = member.avatarFg,
                                                size = 42
                                            )

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = member.name,
                                                        fontSize = 14.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = FlightPassTokens.PrimaryDark
                                                    )
                                                    if (member.isOnTicket) {
                                                        Surface(
                                                            shape = FlightPassTokens.RadiusPill,
                                                            color = FlightPassTokens.SkyBlue,
                                                            border = BorderStroke(0.5.dp, FlightPassTokens.SkyBlueBorder)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.FlightTakeoff,
                                                                    contentDescription = "On Ticket",
                                                                    tint = FlightPassTokens.SkyBlueText,
                                                                    modifier = Modifier.size(10.dp)
                                                                )
                                                                Text(
                                                                    text = "On Ticket",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = FlightPassTokens.SkyBlueText
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (member.isPayer) {
                                                        Surface(
                                                            shape = FlightPassTokens.RadiusPill,
                                                            color = FlightPassTokens.StatusGreenSurface,
                                                            border = BorderStroke(0.5.dp, FlightPassTokens.StatusGreenBorder)
                                                        ) {
                                                            Text(
                                                                text = "Paid ${formatFlightPaiseExact(totalAirfarePaise)} · Gets back ${formatFlightPaiseExact(returnsPaise)}",
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = FlightPassTokens.StatusGreenText,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
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
                                                    if (!member.isPayer) {
                                                        Text(
                                                            text = "Set as Payer",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = FlightPassTokens.SkyBlueText,
                                                            modifier = Modifier.clickable {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                selectedPayerId = member.id
                                                            }
                                                        )
                                                    }
                                                }
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
                            color = FlightPassTokens.StatusGreenSurface.copy(alpha = if (SplitMateTheme.isDark) 0.75f else 0.55f),
                            border = BorderStroke(1.dp, FlightPassTokens.StatusGreenBorder),
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
                                            "PNR $pnrCode is locked offline forever. ${nonPayerNames.joinToString(", ")} will owe $payerMemberName once confirmed."
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

            BoardingPassCommitStampOverlay(
                tearProgress = commitTearProgress.value,
                stampScale = commitStampScale.value,
                stampAlpha = commitStampAlpha.value,
                accentColor = FlightPassTokens.AviationNavy,
                stampSubLabel = "${extractedTicket.originIata} → ${extractedTicket.destinationIata} · PNR $pnrCode"
            )
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
            text = "Paper Tear & Gate-Stamp Acoustics",
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
            color = if (isFeedbackEnabled) FlightPassTokens.StatusGreenSurface else FlightPassTokens.TicketPaperWhite,
            border = BorderStroke(
                1.dp,
                if (isFeedbackEnabled) FlightPassTokens.StatusGreenBorder else FlightPassTokens.BorderSubtle
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isFeedbackEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                    contentDescription = null,
                    tint = if (isFeedbackEnabled) FlightPassTokens.StatusGreenText else FlightPassTokens.TextSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isFeedbackEnabled) "Paper Tear & Stamp Sound: ON" else "Sensory Sound: OFF",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFeedbackEnabled) FlightPassTokens.StatusGreenText else FlightPassTokens.TextSecondary
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
    var energyState by remember(pnr) {
        mutableStateOf(com.splitmate.app.ui.components.Gm3EnergyState.ANTICIPATING)
    }

    com.splitmate.app.ui.components.Gm3AuroraEnergySurface(
        state = energyState,
        onStateAutoTransition = { nextState -> energyState = nextState },
        palette = com.splitmate.app.ui.components.Gm3EnergyAccentPalette.AVIATION_PERIWINKLE,
        shape = FlightPassTokens.RadiusInner,
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
                            color = FlightPassTokens.StatusGreenSurface,
                            border = BorderStroke(0.5.dp, FlightPassTokens.StatusGreenBorder)
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

            Surface(
                onClick = {
                    energyState = when (energyState) {
                        com.splitmate.app.ui.components.Gm3EnergyState.IDLE ->
                            com.splitmate.app.ui.components.Gm3EnergyState.PROCESSING
                        com.splitmate.app.ui.components.Gm3EnergyState.PROCESSING ->
                            com.splitmate.app.ui.components.Gm3EnergyState.RESPONDING
                        else ->
                            com.splitmate.app.ui.components.Gm3EnergyState.IDLE
                    }
                },
                shape = FlightPassTokens.RadiusPill,
                color = FlightPassTokens.SkyBlue,
                border = BorderStroke(1.dp, FlightPassTokens.SkyBlueBorder)
            ) {
                Text(
                    text = if (energyState == com.splitmate.app.ui.components.Gm3EnergyState.IDLE) {
                        rightStatusLabel
                    } else {
                        "${energyState.label} · $rightStatusLabel"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlightPassTokens.SkyBlueText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

// ==============================================================================
// 6. SUB-COMPONENT: AnimatedLuxuryAirlineBoardingPass
//    ("Lil Animator Boy" Perforated Stub Tear-Off + Periwinkle Wallet Pocket Tuck
//     + Holographic BOARDING VERIFIED Gate Stamp + Synthesized PCM AudioTrack)
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

    // Pre-compute static PCM AudioTrack on background thread so tapping the tear line plays
    // an audible Paper Perforation Tear + Mechanical Gate-Stamp Thud even when OS Touch Sounds are disabled!
    var tearAndStampAudioTrack by remember { mutableStateOf<AudioTrack?>(null) }
    LaunchedEffect(Unit) {
        val track = withContext(Dispatchers.Default) {
            runCatching {
                val sampleRate = 24000
                val pcmData = buildBoardingPassTearAndStampPcm(sampleRate)
                val byteCount = pcmData.size * 2
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(byteCount)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                audioTrack.write(pcmData, 0, pcmData.size)
                audioTrack
            }.getOrNull()
        }
        tearAndStampAudioTrack = track
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                tearAndStampAudioTrack?.stop()
                tearAndStampAudioTrack?.release()
            }
            tearAndStampAudioTrack = null
        }
    }

    fun triggerBoardingPassTearAcoustics() {
        if (!isSensorySoundEnabled) return
        runCatching {
            val track = tearAndStampAudioTrack
            if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.reloadStaticData()
                track.play()
            } else {
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        }.onFailure {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    // 1. Scissor / Perforation Tear-Line Progress (0f -> 1f)
    val tearProgress by animateFloatAsState(
        targetValue = if (isFolded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "PerforationTearProgress"
    )

    // 2. Detached Stub Tilt & Tuck into Periwinkle Leather Wallet Pocket
    val stubTiltZ by animateFloatAsState(
        targetValue = if (isFolded) -3.4f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "StubDetachTiltZ"
    )
    val stubPitchX by animateFloatAsState(
        targetValue = if (isFolded) -16f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "StubPitchX"
    )
    val stubScale by animateFloatAsState(
        targetValue = if (isFolded) 0.93f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "StubScale"
    )
    val stubSlideY by animateFloatAsState(
        targetValue = if (isFolded) 18f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "StubSlideY"
    )

    // 3. Holographic "BOARDING VERIFIED" Gate Stamp Slam (1.6f -> 1.0f bouncy overshoot)
    val stampAlpha by animateFloatAsState(
        targetValue = if (isFolded) 1f else 0f,
        animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing),
        label = "GateStampAlpha"
    )
    val stampScale by animateFloatAsState(
        targetValue = if (isFolded) 1.0f else 1.55f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "GateStampScale"
    )

    // Dynamic inner crease & wallet pocket shadow
    val walletPocketAlpha by animateFloatAsState(
        targetValue = if (isFolded) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "WalletPocketAlpha"
    )

    // Overall card elevation expansion on tear-and-tuck
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
            // 1. TOP HEADER: WARM PERIWINKLE-INDIGO DUSK (#2B2768 -> #1B1849)
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
            // 2. MIDDLE SECTION: Route Strip, Passenger Allocation & Gate Stamp
            // ------------------------------------------------------------------
            Box(modifier = Modifier.fillMaxWidth()) {
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
                                        .background(FlightPassTokens.SkyBlueText)
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
                                    tint = FlightPassTokens.SkyBlueText,
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
                                        .background(FlightPassTokens.SkyBlueText)
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
                            color = FlightPassTokens.StatusGreenSurface,
                            border = BorderStroke(0.5.dp, FlightPassTokens.StatusGreenBorder)
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
                                        FlightPassTokens.AppBackground.copy(alpha = 0.65f),
                                        FlightPassTokens.RadiusInner
                                    )
                                    .border(
                                        1.dp,
                                        if (passenger.isMatchedPayer) FlightPassTokens.SkyBlueBorder else FlightPassTokens.BorderSubtle,
                                        FlightPassTokens.RadiusInner
                                    )
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                com.splitmate.app.AvatarToken(
                                    initials = passenger.avatarSeed.ifBlank { passenger.name },
                                    bg = FlightPassTokens.SkyBlue,
                                    textColor = FlightPassTokens.SkyBlueText,
                                    size = 36
                                )
                                Spacer(modifier = Modifier.width(10.dp))
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
                                        fontWeight = if (passenger.matchedMemberName != null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (passenger.matchedMemberName != null) FlightPassTokens.SkyBlueText else FlightPassTokens.TextSecondary
                                    )
                                }

                                Surface(
                                    shape = FlightPassTokens.RadiusPill,
                                    color = FlightPassTokens.SkyBlue,
                                    border = BorderStroke(0.5.dp, FlightPassTokens.SkyBlueBorder)
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

                // HOLOGRAPHIC AVIATION GATE STAMP OVERLAY (Slams onto boarding pass when torn & tucked!)
                if (stampAlpha > 0.01f) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (SplitMateTheme.isDark) Color(0xEB1B1936) else Color(0xF0EEF2FF),
                        border = BorderStroke(2.2.dp, if (SplitMateTheme.isDark) Color(0xFF818CF8) else Color(0xFF3730A3)),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                alpha = stampAlpha
                                scaleX = stampScale
                                scaleY = stampScale
                                rotationZ = -11.5f
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = if (SplitMateTheme.isDark) Color(0xFFA5B4FC) else Color(0xFF3730A3),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "BOARDING VERIFIED · PNR $pnrNumber",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.1.sp,
                                    color = if (SplitMateTheme.isDark) Color(0xFFE0E7FF) else Color(0xFF1E1B4B)
                                )
                                Text(
                                    text = "STUB DETACHED & TUCKED IN WALLET SLEEVE",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp,
                                    color = if (SplitMateTheme.isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
                                )
                            }
                        }
                    }
                }
            }

            // ------------------------------------------------------------------
            // 3. TACTILE INTERACTIVE PERFORATION TEAR LINE WITH ANIMATED SCISSOR SPARK
            // ------------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(
                            if (isFolded) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress
                        )
                        triggerBoardingPassTearAcoustics()
                        isFolded = !isFolded
                    }
                    .padding(vertical = 4.dp)
            ) {
                // Perforation Dashed Line + Animated Gold/Periwinkle Tear Cut Line
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .padding(horizontal = 24.dp)
                        .align(Alignment.Center)
                ) {
                    val centerY = size.height / 2f
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                    drawLine(
                        color = FlightPassTokens.BorderDashed,
                        start = Offset(0f, centerY),
                        end = Offset(size.width, centerY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = pathEffect
                    )
                    if (tearProgress > 0.01f) {
                        val cutEndX = size.width * tearProgress
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF6366F1),
                                    Color(0xFFF59E0B),
                                    Color(0xFF4F46E5)
                                )
                            ),
                            start = Offset(0f, centerY),
                            end = Offset(cutEndX, centerY),
                            strokeWidth = 3.dp.toPx()
                        )
                        // Glowing perforation spark at the active tear tip
                        drawCircle(
                            color = Color(0xFFF59E0B),
                            radius = 5.dp.toPx(),
                            center = Offset(cutEndX.coerceIn(0f, size.width), centerY)
                        )
                    }
                }

                // Interactive Tap Hint Pill centered on the Perforation line
                Surface(
                    shape = FlightPassTokens.RadiusPill,
                    color = if (isFolded) FlightPassTokens.SkyBlue else FlightPassTokens.TicketPaperWhite,
                    border = BorderStroke(1.dp, if (isFolded) FlightPassTokens.SkyBlueBorder else FlightPassTokens.BorderDashed),
                    shadowElevation = 3.dp,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isFolded) Icons.Rounded.UnfoldMore else Icons.Rounded.ContentCut,
                            contentDescription = null,
                            tint = if (isFolded) FlightPassTokens.SkyBlueText else FlightPassTokens.TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFolded) "Stub Tucked in Wallet · Tap to Unfold" else "Tap Perforation to Tear Stub & Stamp Pass",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isFolded) FlightPassTokens.SkyBlueText else FlightPassTokens.TextSecondary
                        )
                    }
                }
            }

            // ------------------------------------------------------------------
            // 4. BOTTOM TICKET STUB: DETACHES WITH 3D TILT & TUCKS INTO STITCHED
            //    PERIWINKLE LEATHER WALLET POCKET SLEEVE (ZERO SCROLL JUMP!)
            // ------------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isFolded) 10.dp else 0.dp, vertical = if (isFolded) 6.dp else 0.dp)
            ) {
                // Detached Stub Card (Tilts & slides into the Periwinkle Wallet Pocket)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            rotationZ = stubTiltZ
                            rotationX = stubPitchX
                            scaleX = stubScale
                            scaleY = stubScale
                            translationY = stubSlideY
                            cameraDistance = 16f * density
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .background(FlightPassTokens.TicketPaperWhite)
                        .border(
                            width = if (isFolded) 1.5.dp else 0.dp,
                            color = if (isFolded) FlightPassTokens.SkyBlueBorder else Color.Transparent,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 22.dp)
                        .padding(top = 8.dp, bottom = 22.dp)
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
                                color = if (isFolded) FlightPassTokens.SkyBlue else FlightPassTokens.StatusGreenSurface,
                                border = BorderStroke(0.5.dp, if (isFolded) FlightPassTokens.SkyBlueBorder else FlightPassTokens.StatusGreenBorder)
                            ) {
                                Text(
                                    text = if (isFolded) "Tucked in Wallet" else "Ready for Ledger",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFolded) FlightPassTokens.SkyBlueText else FlightPassTokens.StatusGreenText,
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

                // Periwinkle Stitched Leather Wallet Pocket Lip overlapping the bottom of the tucked stub
                if (walletPocketAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .align(Alignment.BottomCenter)
                            .graphicsLayer {
                                alpha = walletPocketAlpha
                            }
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 22.dp, bottomEnd = 22.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF282552),
                                        Color(0xFF1B1849)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF5650B8),
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
                            )
                            .drawBehind {
                                // Gold saddle-stitching line across the leather wallet lip
                                val stitchEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
                                drawLine(
                                    color = Color(0xFFEAB308).copy(alpha = 0.75f),
                                    start = Offset(16.dp.toPx(), 7.dp.toPx()),
                                    end = Offset(size.width - 16.dp.toPx(), 7.dp.toPx()),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = stitchEffect
                                )
                            }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "SPLITMATE AVIATION WALLET SLEEVE",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.9.sp,
                                    color = Color(0xFFEEF2FF)
                                )
                            }
                            Text(
                                text = "PNR $pnrNumber · ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalAirfare)}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }
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
                        .background(FlightPassTokens.BarcodeBarColor.copy(alpha = 0.9f))
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
