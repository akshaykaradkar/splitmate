package com.splitmate.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.ContactPickerBottomSheet
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.DeviceContact
import com.splitmate.app.ui.SplitMateBrandFontFamily
import com.splitmate.app.ui.SplitMateDisplayFontFamily
import com.splitmate.app.ui.SplitMateExpressiveTypography
import com.splitmate.app.ui.SplitMateMonospaceTextStyle
import com.splitmate.app.ui.SplitMateTnumMonospace
import com.splitmate.app.ui.components.ConnectedButtonGroup
import com.splitmate.app.ui.components.ExpressiveGapLinearProgressIndicator
import com.splitmate.app.ui.components.LinearWavyProgressIndicator
import com.splitmate.app.ui.components.MaterialShapes
import com.splitmate.app.ui.components.MorphPolygonShape
import com.splitmate.app.ui.components.SplitMateMotion
import com.splitmate.app.ui.queryAllDeviceContacts
import androidx.graphics.shapes.Morph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.buildDiceBearOpenPeepsUrl
import com.splitmate.app.ui.extractInitialsFromNameOrSeed
import com.splitmate.app.ui.resolveGroupCategoryIcon
import java.text.NumberFormat
import java.util.Locale

enum class QuickSplitMode(val label: String) {
    EQUAL("Equal"),
    EXACT("Exact"),
    PERCENTAGE("%"),
    SHARES("Shares")
}

// ==============================================================================
// SPLITMATE MATERIAL 3 EXPRESSIVE THEME TOKENS (GM3 3-THEME COMPLIANT)
// ==============================================================================
object QuickExpenseThemeTokens {
    val ScreenBg: Color
        get() = DesignSystemBindings.activePalette.surfaceContainerLow
    val PrimaryDark: Color
        get() = DesignSystemBindings.activePalette.onSurface
    val AccentSage: Color
        get() = DesignSystemBindings.activePalette.primaryContainer
    val SageSurface: Color
        get() = DesignSystemBindings.activePalette.primaryContainer
    val SageText: Color
        get() = DesignSystemBindings.activePalette.onPrimaryContainer
    val SageBorder: Color
        get() = DesignSystemBindings.activePalette.primary
    val TerracottaSurface: Color
        get() = DesignSystemBindings.activePalette.secondaryContainer
    val TerracottaText: Color
        get() = DesignSystemBindings.activePalette.onSecondaryContainer
    val SurfaceWhite: Color
        get() = DesignSystemBindings.activePalette.surfaceContainerLowest
    val SurfaceKeypad: Color
        get() = DesignSystemBindings.activePalette.surfaceContainer
    val SurfaceKeypadBorder: Color
        get() = DesignSystemBindings.activePalette.outline
    val BorderLight: Color
        get() = DesignSystemBindings.activePalette.outline
    val TextSecondary: Color
        get() = DesignSystemBindings.activePalette.onSurfaceVariant

    val RadiusHero = DesignSystemBindings.GM3ShapeExtraLarge
    val RadiusCard = RoundedCornerShape(20.dp)
    val RadiusKeySquircle = DesignSystemBindings.GM3ShapeLarge
    val RadiusPill = DesignSystemBindings.GM3ShapePill
}

// Split Participant Model
data class QuickParticipant(
    val id: String,
    val name: String,
    val initials: String,
    val avatarSeed: String,
    val avatarBg: Color,
    val avatarFg: Color,
    val upiId: String = ""
)

private val ParticipantPalette = listOf(
    Color(0xFFD7E8B6) to Color(0xFF23201E),
    Color(0xFFFFD8CC) to Color(0xFF8A2E1A),
    Color(0xFFD0E2FF) to Color(0xFF143E82),
    Color(0xFFFFD5E5) to Color(0xFF801844),
    Color(0xFFE5DCFF) to Color(0xFF452285),
    Color(0xFFD2F5DC) to Color(0xFF1B6331)
)

// ==============================================================================
// ELEVATED QuickExpenseScreen (M3 Expressive)
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuickExpenseScreen(
    viewModel: SplitMateViewModel? = null,
    onBackClick: () -> Unit = {},
    onLogExpenseClick: () -> Unit = {},
    onOpenPnrDirectSplit: () -> Unit = {},
    onSaveSplit: (amount: Long, selectedMembers: List<QuickParticipant>) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val uiState = viewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val activeMembers = viewModel?.activeGroupMembers?.collectAsStateWithLifecycle()?.value
        ?: uiState?.activeGroupMembers
        ?: emptyList()
    val isDark = uiState?.isDarkTheme == true || SplitMateTheme.isDark
    val activePalette = DesignSystemBindings.activePalette

    val screenBg by animateColorAsState(
        targetValue = activePalette.surfaceContainerLow,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "QuickExpenseScreenBg"
    )
    val surfaceColor by animateColorAsState(
        targetValue = activePalette.surfaceContainerLowest,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "QuickExpenseCardSurface"
    )
    val textPrimary by animateColorAsState(
        targetValue = activePalette.onSurface,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "QuickExpenseTextPrimary"
    )
    val textSecondary by animateColorAsState(
        targetValue = activePalette.onSurfaceVariant,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "QuickExpenseTextSecondary"
    )
    val keypadBg by animateColorAsState(
        targetValue = activePalette.surfaceContainer,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "QuickExpenseKeypadBg"
    )
    val keypadBorder by animateColorAsState(
        targetValue = activePalette.outline,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "QuickExpenseKeypadBorder"
    )

    val participants: List<QuickParticipant> = remember(activeMembers) {
        activeMembers.mapIndexed { idx, m ->
            val (bg, fg) = ParticipantPalette[idx % ParticipantPalette.size]
            val cleanInitials = extractInitialsFromNameOrSeed(m.name)
            QuickParticipant(
                id = m.memberId,
                name = if (m.isCurrentUser) "${m.name} (You)" else m.name,
                initials = cleanInitials,
                avatarSeed = m.avatarSeed,
                avatarBg = bg,
                avatarFg = fg,
                upiId = m.upiId
            )
        }
    }

    var amountDigits by remember { mutableStateOf("0") }
    var expenseCategoryTitle by remember { mutableStateOf("Dinner & Food") }
    var expenseCategoryRef by remember { mutableStateOf<String?>(null) }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var pendingCommitAfterCategorySelection by remember { mutableStateOf(false) }
    // v2.3.4: expanded category catalog + user-created categories (title-based; no data change).
    var showCategorySheet by remember { mutableStateOf(false) }
    val customCategories by com.splitmate.app.ui.category.CustomExpenseCategoryStore.categories.collectAsState()
    // v2.3.5 (#5): show the active trip's shared categories alongside this phone's own.
    val categoryBindContext = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(uiState?.activeGroupId) {
        com.splitmate.app.ui.category.CustomExpenseCategoryStore.bindGroup(categoryBindContext, uiState?.activeGroupId)
    }
    // v2.3.5 (#1): late expenses on a wrapped-up trip are allowed after an explicit confirmation.
    val extrasByGroup by com.splitmate.app.data.GroupLedgerExtrasStore.extrasByGroup.collectAsStateWithLifecycle()
    val activeTripEnded = uiState?.activeGroupId?.let { gid -> extrasByGroup[gid]?.tripLifecycle?.isEnded } == true
    var lateExpenseConfirmedFor by remember { mutableStateOf<String?>(null) }
    if (activeTripEnded && lateExpenseConfirmedFor != uiState?.activeGroupId) {
        LateExpenseConfirmDialog(
            tripName = uiState?.activeGroup?.name ?: "This trip",
            onAddAnyway = { lateExpenseConfirmedFor = uiState?.activeGroupId },
            onCancel = onBackClick
        )
    }
    var showGroupDropdown by remember { mutableStateOf(false) }
    var editingFriend by remember { mutableStateOf<GroupMemberEntity?>(null) }

    var selectedMemberIds by remember(participants) {
        mutableStateOf(participants.map { it.id }.toSet())
    }
    var selectedPayerId by remember(activeMembers) {
        mutableStateOf(
            activeMembers.firstOrNull { it.isCurrentUser }?.memberId
                ?: activeMembers.firstOrNull()?.memberId.orEmpty()
        )
    }
    val haptic = LocalHapticFeedback.current

    val currencySymbol = "₹"
    val selectedGroup = uiState?.activeGroup
    val hasSelectedGroup = selectedGroup != null && participants.isNotEmpty()
    val activeGroupName = selectedGroup?.name ?: "Select a Group"
    val activeGroupIconName = selectedGroup?.iconName ?: "Flight"

    val totalAmountPaise = remember(amountDigits) {
        parseAmountInputToPaise(amountDigits)
    }
    val numericVal = totalAmountPaise / 100L
    val formattedDisplay = remember(amountDigits, totalAmountPaise) {
        formatPaiseWithInputDisplay(totalAmountPaise, amountDigits, currencySymbol)
    }

    val memberCount = selectedMemberIds.size
    val perPersonPaise = if (memberCount > 0) totalAmountPaise / memberCount else 0L
    // v2.3.3: UI-only split nature for the badge copy (math above is untouched).
    val quickSplitNature = com.splitmate.app.ExpenseSplitClassifier.classifySelection(selectedPayerId, selectedMemberIds)
    val quickSoleMember = activeMembers.firstOrNull { it.memberId == selectedMemberIds.singleOrNull() }
    val remainderPaise = if (memberCount > 0) totalAmountPaise % memberCount else 0L
    val payerExtraPaise = if (remainderPaise > 0L) 1L else 0L
    val remainderRecipientIds = remember(totalAmountPaise, selectedMemberIds, selectedPayerId, activeMembers) {
        val chosen = activeMembers
            .filter { selectedMemberIds.contains(it.memberId) }
            .ifEmpty { activeMembers }
        val currentUserId = activeMembers.firstOrNull { it.isCurrentUser }?.memberId
        val payerId = selectedPayerId.ifBlank {
            currentUserId ?: chosen.firstOrNull()?.memberId.orEmpty()
        }
        com.splitmate.app.SplitMateMathEngine.splitEquallyZeroDrift(
            totalCents = totalAmountPaise,
            members = chosen.map { it.memberId to it.name },
            payerId = payerId,
            currentUserId = currentUserId
        ).filter { it.plusOneCent }.map { it.memberId }.toSet()
    }

    if (showCategorySheet) {
        com.splitmate.app.ui.category.ExpenseCategoryPickerSheet(
            selectedTitle = expenseCategoryTitle,
            onDismiss = { showCategorySheet = false },
            onSelect = { cat ->
                showCategorySheet = false
                if (cat.opensPnrFlow) {
                    onOpenPnrDirectSplit()
                } else {
                    expenseCategoryTitle = cat.title
                    expenseCategoryRef = if (cat.isCustom) cat.customId?.let { "custom:$it" } else "builtin:${cat.title}"
                }
            }
        )
    }

    if (showEditTitleDialog) {
        val existingTicket = remember(expenseCategoryTitle) {
            com.splitmate.app.ui.extractTravelTicketFromTitle(expenseCategoryTitle)
        }
        var draftTitle by remember {
            mutableStateOf(
                if (expenseCategoryTitle.contains("PNR:") || expenseCategoryTitle.contains("Train", ignoreCase = true)) "Train / PNR Ticket"
                else expenseCategoryTitle
            )
        }
        var isTravelTicketMode by remember {
            mutableStateOf(
                existingTicket != null ||
                    draftTitle.contains("Train", ignoreCase = true) ||
                    draftTitle.contains("Flight", ignoreCase = true) ||
                    draftTitle.contains("Travel", ignoreCase = true) ||
                    draftTitle.contains("PNR", ignoreCase = true)
            )
        }
        var pnrInput by remember { mutableStateOf(existingTicket?.pnr ?: "") }
        var trainNoInput by remember { mutableStateOf(existingTicket?.trainOrFlightNo ?: "") }
        var fromStationInput by remember { mutableStateOf(existingTicket?.fromStation ?: "") }
        var toStationInput by remember { mutableStateOf(existingTicket?.toStation ?: "") }
        var depTimeInput by remember { mutableStateOf(existingTicket?.departureTime ?: "") }
        var coachSeatsInput by remember { mutableStateOf(existingTicket?.coachAndSeats ?: "") }
        var bookingStatusInput by remember { mutableStateOf(existingTicket?.bookingStatus ?: "CNF") }
        var chartStatusInput by remember { mutableStateOf(existingTicket?.chartStatus ?: "Chart Prepared") }
        var liveRadarPreview by remember { mutableStateOf(existingTicket?.liveTrainRadar ?: "") }
        var isCheckingLivePnr by remember { mutableStateOf(false) }
        var fetchedPnrSnapshot by remember {
            mutableStateOf(
                existingTicket?.pnr?.takeIf { it.length == 10 }?.let {
                    com.splitmate.app.ui.loadPersistedPnrSnapshot(context, it)
                }
            )
        }
        var irctcPaymentMode by remember { mutableStateOf("UPI") } // "UPI", "CARD", or "BASE"
        var includeIrctcInsurance by remember { mutableStateOf(true) }
        val pnrScope = androidx.compose.runtime.rememberCoroutineScope()
        val dialogView = androidx.compose.ui.platform.LocalView.current

        val syncDynamicIrctcFare: (com.splitmate.app.ui.LivePnrStatusSnapshot, String, Boolean) -> Unit = { snap, payMode, withIns ->
            if (snap.totalFareRupees > 0) {
                val computedPaise = snap.computeCustomTotalPaise(payMode, withIns)
                amountDigits = snap.formatPaiseAsDecimalRupees(computedPaise)
                val convPaise = when (payMode) {
                    "UPI" -> snap.irctcConvenienceFeeUpiPaise
                    "CARD" -> snap.irctcConvenienceFeeCardPaise
                    else -> 0L
                }
                val insPaise = if (withIns) snap.travelInsurancePaise else 0L
                liveRadarPreview = buildString {
                    append("All-Incl Bill: ₹${snap.formatPaiseAsDecimalRupees(computedPaise)} ")
                    append("(Base ₹${snap.totalFareRupees} + ${if (snap.isAcClass) "AC" else "Non-AC"} $payMode Fee ₹${snap.formatPaiseAsDecimalRupees(convPaise)}")
                    if (withIns) {
                        append(" + Ins ${snap.effectivePassengerCount}×₹0.45=₹${snap.formatPaiseAsDecimalRupees(insPaise)}")
                    }
                    append(") · ${snap.liveTrainLocationRadar}")
                }
            } else {
                liveRadarPreview = "${snap.liveTrainLocationRadar} · ${snap.confirmationProbability}"
            }
        }

        @Suppress("UNUSED_VARIABLE") val triggerLivePnrLookup: (String) -> Unit = { targetPnr ->
            val clean10 = targetPnr.replace(Regex("[^0-9]"), "").take(10)
            if (clean10.length == 10 && !isCheckingLivePnr) {
                com.splitmate.app.ui.performCrispTactileHaptic(context, dialogView, heavy = true)
                isCheckingLivePnr = true
                pnrScope.launch {
                    val snap = com.splitmate.app.ui.fetchLivePnrAndTrainStatus(
                        pnr = clean10,
                        fallbackTicket = com.splitmate.app.ui.ParsedTravelTicket(
                            pnr = clean10,
                            trainOrFlightNo = trainNoInput,
                            fromStation = fromStationInput,
                            toStation = toStationInput,
                            departureTime = depTimeInput,
                            coachAndSeats = coachSeatsInput,
                            bookingStatus = bookingStatusInput.ifBlank { "UNKNOWN" }
                        ),
                        forceManualRefresh = true,
                        context = context
                    )
                    fetchedPnrSnapshot = snap
                    pnrInput = snap.pnr
                    trainNoInput = snap.trainNo
                    fromStationInput = snap.fromStation
                    toStationInput = snap.toStation
                    depTimeInput = snap.departureTime
                    coachSeatsInput = snap.passengerStatuses.joinToString(", ")
                    bookingStatusInput = snap.bookingStatusBadge
                    chartStatusInput = if (snap.chartPrepared) "Chart Prepared" else "Chart Not Prepared"
                    if (snap.trainName.isNotBlank()) {
                        draftTitle = "Train ${snap.trainNo} ${snap.trainName}"
                    }
                    syncDynamicIrctcFare(snap, irctcPaymentMode, includeIrctcInsurance)
                    isCheckingLivePnr = false
                }
            }
        }

        val presetCategories = remember {
            listOf(
                "Train / PNR Ticket" to Icons.Rounded.Train,
                "Dinner & Food" to Icons.Rounded.Restaurant,
                "Travel & Flight" to Icons.Rounded.Flight,
                "Stay & Hotel" to Icons.Rounded.Hotel,
                "Cab & Local" to Icons.Rounded.LocalTaxi,
                "Groceries" to Icons.Rounded.ShoppingCart,
                "Party & Drinks" to Icons.Rounded.LocalBar
            )
        }
        var showDialogCategorySheet by remember { mutableStateOf(false) }
        if (showDialogCategorySheet) {
            com.splitmate.app.ui.category.ExpenseCategoryPickerSheet(
                selectedTitle = draftTitle,
                onDismiss = { showDialogCategorySheet = false },
                onSelect = { cat ->
                    showDialogCategorySheet = false
                    if (cat.opensPnrFlow) {
                        showEditTitleDialog = false
                        onOpenPnrDirectSplit()
                    } else {
                        draftTitle = cat.title
                        isTravelTicketMode = cat.title.contains("Flight", ignoreCase = true)
                        expenseCategoryRef = if (cat.isCustom) cat.customId?.let { "custom:$it" } else "builtin:${cat.title}"
                    }
                }
            )
        }
        AlertDialog(
            onDismissRequest = {
                showEditTitleDialog = false
                pendingCommitAfterCategorySelection = false
            },
            containerColor = surfaceColor,
            title = {
                Column {
                    Text(
                        if (pendingCommitAfterCategorySelection) "Which type of expense is this?" else "Expense Category & Travel PNR",
                        fontFamily = SplitMateDisplayFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = textPrimary
                    )
                    Text(
                        if (pendingCommitAfterCategorySelection) "Pick a category or enter a title to finish logging ₹$numericVal"
                        else "Select category or enter a 10-digit IRCTC PNR for live status & fare",
                        fontFamily = SplitMateBrandFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presetCategories) { (catLabel, catIcon) ->
                            val isSelected = draftTitle.equals(catLabel, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    com.splitmate.app.ui.performCrispTactileHaptic(context, dialogView, heavy = false)
                                    if (catLabel.contains("Train", ignoreCase = true) || catLabel.contains("PNR", ignoreCase = true)) {
                                        showEditTitleDialog = false
                                        onOpenPnrDirectSplit()
                                    } else {
                                        draftTitle = catLabel
                                        isTravelTicketMode = catLabel.contains("Flight", ignoreCase = true)
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = catIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = catLabel,
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                        // v2.3.4: the user's own categories, then the full catalog.
                        items(customCategories, key = { "dlg_custom_${it.title}" }) { cat ->
                            FilterChip(
                                selected = draftTitle.equals(cat.title, ignoreCase = true),
                                onClick = {
                                    com.splitmate.app.ui.performCrispTactileHaptic(context, dialogView, heavy = false)
                                    draftTitle = cat.title
                                    isTravelTicketMode = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = com.splitmate.app.ui.category.ExpenseCategoryIcons.forKey(cat.iconKey),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = cat.title,
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                        item(key = "dlg_more_categories") {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    com.splitmate.app.ui.performCrispTactileHaptic(context, dialogView, heavy = false)
                                    showDialogCategorySheet = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreHoriz,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "More categories",
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        label = { Text("Expense Title / Category *", fontFamily = SplitMateBrandFontFamily) },
                        placeholder = { Text("e.g. Paschim SF Express, Beach Shack Lunch", fontFamily = SplitMateBrandFontFamily) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = textPrimary,
                            unfocusedBorderColor = keypadBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dedicated 1-Tap Launch for IRCTC Train PNR Direct Split
                    Surface(
                        onClick = {
                            com.splitmate.app.ui.performCrispTactileHaptic(context, dialogView, heavy = true)
                            showEditTitleDialog = false
                            onOpenPnrDirectSplit()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF264010),
                        border = BorderStroke(1.dp, Color(0xFF4A7325)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "IRCTC Train PNR Direct Split",
                                    fontFamily = SplitMateBrandFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Open tactile boarding pass · Enter 10-digit PNR & select members",
                                    fontFamily = SplitMateBrandFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = Color(0xFFC5D6A7)
                                )
                            }
                            Surface(
                                shape = QuickExpenseThemeTokens.RadiusPill,
                                color = Color(0xFFD7E8B6)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "Open",
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF1B2E0B)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFF1B2E0B),
                                        modifier = Modifier.size(12.dp)
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
                        com.splitmate.app.ui.performCrispTactileHaptic(context, dialogView, heavy = true)
                        val finalTitle = if (isTravelTicketMode) {
                            com.splitmate.app.ui.formatTravelExpenseTitle(
                                baseCategory = draftTitle.ifBlank { "Train / PNR Ticket" },
                                ticket = com.splitmate.app.ui.ParsedTravelTicket(
                                    pnr = pnrInput.trim(),
                                    trainOrFlightNo = trainNoInput.trim(),
                                    fromStation = fromStationInput.trim(),
                                    toStation = toStationInput.trim(),
                                    departureTime = depTimeInput.trim(),
                                    coachAndSeats = coachSeatsInput.trim(),
                                    bookingStatus = bookingStatusInput.trim().ifBlank { "CNF" },
                                    chartStatus = chartStatusInput.trim().ifBlank { "Chart Prepared" }
                                )
                            )
                        } else {
                            draftTitle.trim().ifEmpty { "General Expense" }
                        }
                        expenseCategoryTitle = finalTitle
                        showEditTitleDialog = false

                        val currentAmountPaise = parseAmountInputToPaise(amountDigits)
                        if (pendingCommitAfterCategorySelection && hasSelectedGroup && currentAmountPaise > 0L && selectedMemberIds.isNotEmpty()) {
                            pendingCommitAfterCategorySelection = false
                            val selected = participants.filter { selectedMemberIds.contains(it.id) }
                            viewModel?.commitQuickEqualExpense(
                                title = finalTitle,
                                totalAmountCents = currentAmountPaise,
                                selectedMemberIds = selected.map { it.id },
                                payerMemberId = selectedPayerId,
                                categoryRef = com.splitmate.app.ui.category.ExpenseCategoryRefs.refForTitle(finalTitle, customCategories)
                            )
                            onSaveSplit(currentAmountPaise / 100L, selected)
                        } else {
                            pendingCommitAfterCategorySelection = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = textPrimary,
                        contentColor = screenBg
                    )
                ) {
                    Text(
                        if (pendingCommitAfterCategorySelection) "Save & Log Split" else "Save Category",
                        fontFamily = SplitMateBrandFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditTitleDialog = false
                        pendingCommitAfterCategorySelection = false
                    }
                ) {
                    Text("Cancel", fontFamily = SplitMateBrandFontFamily, color = textSecondary)
                }
            }
        )
    }

    editingFriend?.let { friend ->
        val editableGroupMembers = activeMembers.filter { !it.isCurrentUser }.ifEmpty { listOf(friend) }
        EditFriendUpiDialog(
            member = friend,
            allGroupMembers = editableGroupMembers,
            onSelectMember = { editingFriend = it },
            onDismiss = { editingFriend = null },
            onSave = { newName, newUpi, newAvatarSeed ->
                viewModel?.updateFriendUpi(friend.memberId, newName, newUpi, newAvatarSeed)
                editingFriend = null
            },
            // v2.3.5 (#7 RC1): "Save All Members (N)" must persist every member, not just the selected one.
            onSaveAll = { batchUpdates ->
                viewModel?.updateAllGroupMembers(batchUpdates)
                editingFriend = null
            }
        )
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            // Root BottomNav destination — NO back arrow navigationIcon and NO INR/Equal Split badge!
            TopAppBar(
                title = {
                    Box {
                        Surface(
                            onClick = { showGroupDropdown = true },
                            shape = QuickExpenseThemeTokens.RadiusPill,
                            color = QuickExpenseThemeTokens.SageSurface,
                            border = BorderStroke(1.dp, QuickExpenseThemeTokens.AccentSage.copy(alpha = 0.7f)),
                            modifier = Modifier.clip(QuickExpenseThemeTokens.RadiusPill)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    imageVector = resolveGroupCategoryIcon(activeGroupIconName, activeGroupName),
                                    contentDescription = null,
                                    tint = QuickExpenseThemeTokens.SageText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeGroupName,
                                    fontFamily = SplitMateBrandFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = QuickExpenseThemeTokens.SageText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Switch Group",
                                    tint = QuickExpenseThemeTokens.SageText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showGroupDropdown,
                            onDismissRequest = { showGroupDropdown = false }
                        ) {
                            val groups = uiState?.groups ?: emptyList()
                            if (groups.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No groups yet — create one in Ledgers", fontFamily = SplitMateBrandFontFamily) },
                                    onClick = {
                                        showGroupDropdown = false
                                        onBackClick()
                                    }
                                )
                            } else {
                                groups.forEach { grp ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                grp.name,
                                                fontFamily = SplitMateBrandFontFamily,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        onClick = {
                                            viewModel?.selectActiveGroup(grp.groupId)
                                            showGroupDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    val firstEditable = activeMembers.firstOrNull { !it.isCurrentUser }
                    if (hasSelectedGroup && firstEditable != null) {
                        Surface(
                            onClick = { editingFriend = firstEditable },
                            shape = QuickExpenseThemeTokens.RadiusPill,
                            color = surfaceColor,
                            border = BorderStroke(1.dp, keypadBorder),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ManageAccounts,
                                    contentDescription = "Edit Members",
                                    tint = textPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Edit Members",
                                    fontFamily = SplitMateBrandFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = screenBg
                )
            )
        }
    ) { innerPadding ->
        if (!hasSelectedGroup) {
            // ==================================================================
            // M3 EMPTY STATE WHEN NO GROUP IS SELECTED
            // Hides the Avatar row and Keypad, shows center M3 card & disables Log & Split FAB
            // ==================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    shape = QuickExpenseThemeTokens.RadiusHero,
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, keypadBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(QuickExpenseThemeTokens.SageSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Groups,
                                contentDescription = null,
                                tint = QuickExpenseThemeTokens.SageText,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Please select or create a group to start splitting.",
                            fontFamily = SplitMateDisplayFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create a group in the Ledgers tab or pick an existing group from the top selector.",
                            fontFamily = SplitMateBrandFontFamily,
                            fontSize = 13.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = QuickExpenseThemeTokens.RadiusPill,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ElectricBolt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Log & Split",
                                fontFamily = SplitMateDisplayFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ==================================================================
                // 1. TOP CONTEXT SECTION: Stepped M3 surface-container-low Tonal Pane
                // ==================================================================
                var isRemainderEquallySplit by remember(totalAmountPaise, selectedMemberIds) {
                    mutableStateOf(false)
                }
                var mathEnergyState by remember {
                    mutableStateOf(com.splitmate.app.ui.components.Gm3EnergyState.IDLE)
                }
                val calcEnergyScope = rememberCoroutineScope()
                val keystrokeDynamicIntensity = remember { androidx.compose.animation.core.Animatable(0f) }
                val remainderCoinFlightProgress = remember { androidx.compose.animation.core.Animatable(0f) }
                val avatarCatchPulseScale = remember { androidx.compose.animation.core.Animatable(1f) }

                fun triggerKeystrokeEnergyPulse(updatedDigits: String) {
                    mathEnergyState = com.splitmate.app.ui.components.Gm3EnergyState.RECEIVING
                    calcEnergyScope.launch {
                        val peak = (0.32f + (updatedDigits.length * 0.05f)).coerceAtMost(0.60f)
                        keystrokeDynamicIntensity.snapTo(peak)
                        keystrokeDynamicIntensity.animateTo(
                            targetValue = 0f,
                            animationSpec = com.splitmate.app.ui.components.SplitMateMotion.slowEffects()
                        )
                        if (mathEnergyState == com.splitmate.app.ui.components.Gm3EnergyState.RECEIVING) {
                            mathEnergyState = com.splitmate.app.ui.components.Gm3EnergyState.IDLE
                        }
                    }
                }

                fun triggerRemainderCoinFlight() {
                    if (isRemainderEquallySplit) return
                    calcEnergyScope.launch {
                        remainderCoinFlightProgress.snapTo(0.02f)
                        remainderCoinFlightProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = com.splitmate.app.ui.components.SplitMateMotion.defaultEffects()
                        )
                        isRemainderEquallySplit = true
                        mathEnergyState = com.splitmate.app.ui.components.Gm3EnergyState.RESPONDING
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        remainderCoinFlightProgress.snapTo(0f)
                        avatarCatchPulseScale.snapTo(1.16f)
                        avatarCatchPulseScale.animateTo(
                            targetValue = 1f,
                            animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.52f, stiffness = 520f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = keypadBg,
                    border = BorderStroke(1.dp, keypadBorder),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Unified Single Category Chip Strip (Tapping Train / PNR directly opens PNR flow; no redundant banner)
                    val quickCategoryPills = remember {
                        listOf(
                            Triple("Train / PNR", "Train / PNR Ticket", Icons.Rounded.Train),
                            Triple("Food", "Dinner & Food", Icons.Rounded.Restaurant),
                            Triple("Cab", "Cab & Local", Icons.Rounded.LocalTaxi),
                            Triple("Stay", "Stay & Hotel", Icons.Rounded.Hotel),
                            Triple("Groceries", "Groceries", Icons.Rounded.ShoppingCart),
                            Triple("Drinks", "Party & Drinks", Icons.Rounded.LocalBar)
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickCategoryPills) { (chipLabel, fullCategory, chipIcon) ->
                            val isChosen = expenseCategoryTitle.startsWith(fullCategory, ignoreCase = true) ||
                                (fullCategory == "Train / PNR Ticket" && (expenseCategoryTitle.contains("PNR:") || expenseCategoryTitle.contains("Train", ignoreCase = true)))
                            val selectedBg = if (isDark) activePalette.primary else activePalette.onSurface
                            val selectedFg = if (isDark) activePalette.onPrimary else activePalette.surfaceContainerLowest
                            val chipCorner by androidx.compose.animation.core.animateDpAsState(
                                targetValue = if (isChosen) 10.dp else 20.dp,
                                animationSpec = SplitMateMotion.fastSpatial(),
                                label = "GM3CategoryChipCorner"
                            )
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (fullCategory == "Train / PNR Ticket") {
                                        onOpenPnrDirectSplit()
                                    } else {
                                        expenseCategoryTitle = fullCategory
                                    }
                                },
                                shape = RoundedCornerShape(chipCorner),
                                color = if (isChosen) selectedBg else QuickExpenseThemeTokens.SageSurface,
                                border = BorderStroke(1.dp, if (isChosen) selectedBg else QuickExpenseThemeTokens.AccentSage),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .heightIn(min = 32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = chipIcon,
                                        contentDescription = null,
                                        tint = if (isChosen) selectedFg else QuickExpenseThemeTokens.SageText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = chipLabel,
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChosen) selectedFg else QuickExpenseThemeTokens.SageText
                                    )
                                }
                            }
                        }

                        // v2.3.4: user-created categories sit right after the built-in pills.
                        items(customCategories, key = { "custom_cat_${it.title}" }) { cat ->
                            val isChosen = expenseCategoryTitle.equals(cat.title, ignoreCase = true)
                            val selectedBg = if (isDark) activePalette.primary else activePalette.onSurface
                            val selectedFg = if (isDark) activePalette.onPrimary else activePalette.surfaceContainerLowest
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    expenseCategoryTitle = cat.title
                                },
                                shape = RoundedCornerShape(if (isChosen) 10.dp else 20.dp),
                                color = if (isChosen) selectedBg else QuickExpenseThemeTokens.SageSurface,
                                border = BorderStroke(1.dp, if (isChosen) selectedBg else QuickExpenseThemeTokens.AccentSage),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .heightIn(min = 32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = com.splitmate.app.ui.category.ExpenseCategoryIcons.forKey(cat.iconKey),
                                        contentDescription = null,
                                        tint = if (isChosen) selectedFg else QuickExpenseThemeTokens.SageText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = cat.shortLabel.take(16),
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChosen) selectedFg else QuickExpenseThemeTokens.SageText
                                    )
                                }
                            }
                        }

                        // v2.3.4: "More" opens the full catalog (Breakfast, Auto, Bike Rental, Fuel…) + create-your-own.
                        item(key = "more_categories") {
                            com.splitmate.app.ui.category.MoreCategoriesChip(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showCategorySheet = true
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                        }

                        // Inline Custom Title / Note Chip (replaces the separate terracotta banner)
                        item {
                            val isCustomNote = quickCategoryPills.none { (_, fullCat, _) ->
                                expenseCategoryTitle.equals(fullCat, ignoreCase = true)
                            } && customCategories.none { expenseCategoryTitle.equals(it.title, ignoreCase = true) } &&
                                expenseCategoryTitle.isNotBlank()
                            // A picked catalog category (e.g. "Breakfast") shows its own icon instead of the pencil.
                            val matchedCatalogCategory = com.splitmate.app.ui.category.exactExpenseCategoryFor(expenseCategoryTitle)
                            val selectedBg = if (isDark) activePalette.primary else activePalette.onSurface
                            val selectedFg = if (isDark) activePalette.onPrimary else activePalette.surfaceContainerLowest
                            val customChipCorner by androidx.compose.animation.core.animateDpAsState(
                                targetValue = if (isCustomNote) 10.dp else 20.dp,
                                animationSpec = SplitMateMotion.fastSpatial(),
                                label = "GM3CustomChipCorner"
                            )
                            Surface(
                                onClick = { showEditTitleDialog = true },
                                shape = RoundedCornerShape(customChipCorner),
                                color = if (isCustomNote) selectedBg else surfaceColor,
                                border = BorderStroke(1.dp, if (isCustomNote) selectedBg else keypadBorder),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .heightIn(min = 32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCustomNote && matchedCatalogCategory != null) {
                                            com.splitmate.app.ui.category.ExpenseCategoryIcons.forKey(matchedCatalogCategory.iconKey)
                                        } else Icons.Rounded.Edit,
                                        contentDescription = if (isCustomNote && matchedCatalogCategory != null) null else "Custom Note",
                                        tint = if (isCustomNote) selectedFg else textSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isCustomNote) expenseCategoryTitle.take(16) else "+ Custom Note",
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCustomNote) selectedFg else textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Compact Circular Avatars + Short Names for "Paid by" (Eliminates horizontal pill waste)
                    if (participants.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paid by",
                                fontFamily = SplitMateDisplayFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textSecondary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                items(participants, key = { "payer_${it.id}" }) { person ->
                                    val isPayer = person.id == selectedPayerId
                                    val shortFirstName = person.name.removeSuffix(" (You)").substringBefore(" ").ifBlank { person.name }
                                    val payerAvatarUrl = remember(person.avatarSeed) {
                                        buildDiceBearOpenPeepsUrl(person.avatarSeed)
                                    }
                                    Surface(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedPayerId = person.id
                                        },
                                        shape = QuickExpenseThemeTokens.RadiusPill,
                                        color = if (isPayer) QuickExpenseThemeTokens.SageSurface else surfaceColor,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isPayer) QuickExpenseThemeTokens.SageBorder else keypadBorder
                                        ),
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(start = 4.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(person.avatarBg)
                                                    .border(
                                                        width = if (isPayer) 1.5.dp else 0.5.dp,
                                                        color = if (isPayer) QuickExpenseThemeTokens.SageBorder else surfaceColor,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = person.initials,
                                                    fontFamily = SplitMateDisplayFontFamily,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = person.avatarFg
                                                )
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(payerAvatarUrl)
                                                        .decoderFactory(SvgDecoder.Factory())
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = shortFirstName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (person.name.contains("(You)")) "$shortFirstName (You)" else shortFirstName,
                                                fontFamily = SplitMateBrandFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = if (isPayer) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (isPayer) QuickExpenseThemeTokens.SageText else textPrimary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // "Who's in on this?" Section with Direct +UPI Badge on Avatars Missing Linked UPI
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Who's in on this?",
                                fontFamily = SplitMateDisplayFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary
                            )

                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedMemberIds = if (selectedMemberIds.size == participants.size) {
                                        setOf(participants.first().id)
                                    } else {
                                        participants.map { it.id }.toSet()
                                    }
                                },
                                shape = QuickExpenseThemeTokens.RadiusPill,
                                color = QuickExpenseThemeTokens.SageSurface,
                                border = BorderStroke(1.dp, QuickExpenseThemeTokens.AccentSage.copy(alpha = 0.8f)),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (selectedMemberIds.size == participants.size) "Clear" else "Select All (${participants.size})",
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = QuickExpenseThemeTokens.SageText,
                                        style = TextStyle(fontFeatureSettings = "tnum")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(participants, key = { it.id }) { person ->
                                val isSelected = selectedMemberIds.contains(person.id)
                                val matchingRoomMember = activeMembers.find { it.memberId == person.id }
                                val shortName = person.name.removeSuffix(" (You)").substringBefore(" ").ifBlank { person.name }
                                val avatarUrl = remember(person.avatarSeed) {
                                    buildDiceBearOpenPeepsUrl(person.avatarSeed)
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(QuickExpenseThemeTokens.RadiusCard)
                                        .combinedClickable(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                selectedMemberIds = if (isSelected) {
                                                    selectedMemberIds - person.id
                                                } else {
                                                    selectedMemberIds + person.id
                                                }
                                            },
                                            onLongClick = {
                                                if (matchingRoomMember != null && !matchingRoomMember.isCurrentUser) {
                                                    editingFriend = matchingRoomMember
                                                }
                                            }
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    val isRemainderRecipient = isSelected && remainderRecipientIds.contains(person.id) && remainderPaise > 0L
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .graphicsLayer {
                                                if (isRemainderRecipient) {
                                                    scaleX = avatarCatchPulseScale.value
                                                    scaleY = avatarCatchPulseScale.value
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) person.avatarBg else person.avatarBg.copy(alpha = 0.45f)
                                                )
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.5.dp,
                                                    color = if (isSelected) QuickExpenseThemeTokens.SageBorder else surfaceColor,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = person.initials,
                                                fontFamily = SplitMateDisplayFontFamily,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isSelected) person.avatarFg else person.avatarFg.copy(alpha = 0.45f)
                                            )
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(avatarUrl)
                                                    .decoderFactory(SvgDecoder.Factory())
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = person.name,
                                                contentScale = ContentScale.Crop,
                                                alpha = if (isSelected) 1f else 0.45f,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                            )
                                        }

                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .size(17.dp)
                                                    .clip(CircleShape)
                                                    .background(QuickExpenseThemeTokens.SageBorder)
                                                    .border(1.5.dp, surfaceColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }

                                        if (isRemainderRecipient && isRemainderEquallySplit) {
                                            Surface(
                                                shape = QuickExpenseThemeTokens.RadiusPill,
                                                color = QuickExpenseThemeTokens.SageSurface,
                                                border = BorderStroke(1.dp, QuickExpenseThemeTokens.SageBorder),
                                                modifier = Modifier.align(Alignment.TopStart)
                                            ) {
                                                Text(
                                                    text = "+${payerExtraPaise}p",
                                                    fontFamily = SplitMateBrandFontFamily,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = QuickExpenseThemeTokens.SageText,
                                                    modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Text(
                                        text = if (person.name.contains("(You)")) "$shortName (You)" else shortName,
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) textPrimary else textSecondary.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    val shareRatio = if (isSelected && memberCount > 0) {
                                        (1f / memberCount.toFloat()).coerceIn(0f, 1f)
                                    } else 0f
                                    ExpressiveGapLinearProgressIndicator(
                                        progress = { shareRatio },
                                        modifier = Modifier
                                            .width(42.dp)
                                            .height(3.dp),
                                        color = if (isSelected) QuickExpenseThemeTokens.SageBorder else keypadBorder,
                                        trackColor = keypadBorder.copy(alpha = 0.45f),
                                        gapSize = 3.dp,
                                        stopSize = 2.dp,
                                        strokeWidth = 3.dp
                                    )
                                }
                            }
                        }
                    }
                }
                }

                // ==================================================================
                // 2. BOTTOM UNIFIED CLUSTER: Hero ₹ 0 Amount Display + Split Pill + Tactile Calculator Keypad
                // Grouping the amount display and numeric keypad together so the user's thumb & eye stay coupled
                // ==================================================================
                var activeSplitMode by remember { mutableStateOf(QuickSplitMode.EQUAL) }
                val isZeroDriftVerified = memberCount > 0 && (remainderPaise == 0L || isRemainderEquallySplit)
                val badgeMorphProgress by animateFloatAsState(
                    targetValue = if (isZeroDriftVerified) 1f else 0f,
                    animationSpec = SplitMateMotion.defaultSpatial(),
                    label = "ZeroDriftBadgeMorph"
                )
                val badgeMorph = remember {
                    Morph(MaterialShapes.SoftBurst, MaterialShapes.Cookie9Sided)
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = surfaceColor,
                    border = BorderStroke(1.dp, keypadBorder),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        ConnectedButtonGroup(
                            options = QuickSplitMode.entries,
                            selectedIndex = QuickSplitMode.entries.indexOf(activeSplitMode),
                            onSelect = { _, mode ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                activeSplitMode = mode
                            },
                            labelProvider = { it.label },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        )

                        // Hero Tabular Amount Display wrapped in Gm3AuroraEnergySurface (1.2s pulse on math reconciliation + keystroke dynamicIntensity)
                        com.splitmate.app.ui.components.Gm3AuroraEnergySurface(
                            state = mathEnergyState,
                            onStateAutoTransition = { nextState -> mathEnergyState = nextState },
                            palette = com.splitmate.app.ui.components.Gm3EnergyAccentPalette.BUCKWHEAT_SAGE,
                            dynamicIntensity = keystrokeDynamicIntensity.value,
                            shape = RoundedCornerShape(18.dp),
                            borderWidth = if (mathEnergyState != com.splitmate.app.ui.components.Gm3EnergyState.IDLE) 1.dp else 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = expenseCategoryTitle.ifBlank { "Dinner & Food" },
                                            fontFamily = SplitMateBrandFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFD6CEC4) else textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = formattedDisplay,
                                            fontFamily = SplitMateDisplayFontFamily,
                                            fontSize = 42.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = textPrimary,
                                            letterSpacing = (-1.5).sp,
                                            lineHeight = 46.sp,
                                            style = TextStyle(fontFeatureSettings = "tnum")
                                        )
                                    }

                                    // Real-time Exact Split / Per-Person Share Pill (tapping triggers 1.2s 0.00c Drift verification pulse)
                                    Surface(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (remainderPaise > 0L && !isRemainderEquallySplit) {
                                                triggerRemainderCoinFlight()
                                            } else {
                                                isRemainderEquallySplit = true
                                                mathEnergyState = com.splitmate.app.ui.components.Gm3EnergyState.RESPONDING
                                            }
                                        },
                                        shape = QuickExpenseThemeTokens.RadiusPill,
                                        color = if (memberCount > 0) QuickExpenseThemeTokens.SageSurface else QuickExpenseThemeTokens.TerracottaSurface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (memberCount > 0) QuickExpenseThemeTokens.AccentSage else Color(0xFFFFCCBA)
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(MorphPolygonShape(badgeMorph, badgeMorphProgress))
                                                    .background(
                                                        if (memberCount > 0) QuickExpenseThemeTokens.SageBorder.copy(alpha = 0.18f)
                                                        else QuickExpenseThemeTokens.TerracottaText.copy(alpha = 0.18f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (memberCount > 0) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = if (memberCount > 0) QuickExpenseThemeTokens.SageText else QuickExpenseThemeTokens.TerracottaText,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (memberCount > 0) {
                                                    com.splitmate.app.ExpenseSplitCopy.compactDetail(
                                                        nature = quickSplitNature,
                                                        beneficiaryName = quickSoleMember?.name,
                                                        beneficiaryIsCurrentUser = quickSoleMember?.isCurrentUser == true,
                                                        sharedDetail = "$currencySymbol${formatPaiseForSplitBadge(perPersonPaise)}/person · Exact Split"
                                                    )
                                                } else {
                                                    "Pick ≥1"
                                                },
                                                fontFamily = SplitMateBrandFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (memberCount > 0) QuickExpenseThemeTokens.SageText else QuickExpenseThemeTokens.TerracottaText,
                                                style = TextStyle(fontFeatureSettings = "tnum")
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                val allocatedRatio = if (memberCount > 0) {
                                    if (remainderPaise == 0L || isRemainderEquallySplit) 1f else 0.92f
                                } else 0f
                                LinearWavyProgressIndicator(
                                    progress = { allocatedRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = if (isZeroDriftVerified) QuickExpenseThemeTokens.SageBorder else activePalette.secondary,
                                    trackColor = keypadBorder.copy(alpha = 0.45f),
                                    amplitude = if (isZeroDriftVerified) 0f else 1f,
                                    wavelength = 24.dp,
                                    gapSize = 4.dp,
                                    stopSize = 4.dp,
                                    strokeWidth = 5.dp
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = "0.00¢ DRIFT • EVERY PENNY ACCOUNTED FOR",
                                    style = SplitMateExpressiveTypography.labelSmallEmphasized.merge(SplitMateMonospaceTextStyle),
                                    fontSize = 9.5.sp,
                                    color = if (isZeroDriftVerified) QuickExpenseThemeTokens.SageText else textSecondary,
                                    letterSpacing = 0.6.sp
                                )

                                // High-contrast Peach Remainder Banner when remainderPaise > 0L & not yet reconciled
                                if (remainderPaise > 0L && (!isRemainderEquallySplit || remainderCoinFlightProgress.value > 0f)) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Surface(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                triggerRemainderCoinFlight()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = QuickExpenseThemeTokens.TerracottaSurface.copy(alpha = (1f - remainderCoinFlightProgress.value * 0.7f).coerceIn(0.2f, 1f)),
                                            border = BorderStroke(1.dp, activePalette.secondary),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Extra +${payerExtraPaise}p rounded to Payer",
                                                    fontFamily = SplitMateBrandFontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = QuickExpenseThemeTokens.TerracottaText
                                                )
                                                Surface(
                                                    shape = QuickExpenseThemeTokens.RadiusPill,
                                                    color = activePalette.secondary
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Text(
                                                            text = "Split Remainder Equally",
                                                            fontFamily = SplitMateBrandFontFamily,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = activePalette.onSecondary
                                                        )
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                                            contentDescription = null,
                                                            tint = activePalette.onSecondary,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Flying +1p Largest-Remainder Coin Pill arcing upward toward recipient avatar
                                        if (remainderCoinFlightProgress.value > 0.01f) {
                                            val t = remainderCoinFlightProgress.value
                                            Surface(
                                                shape = QuickExpenseThemeTokens.RadiusPill,
                                                color = QuickExpenseThemeTokens.SageSurface,
                                                border = BorderStroke(1.5.dp, QuickExpenseThemeTokens.SageBorder),
                                                shadowElevation = 6.dp,
                                                modifier = Modifier
                                                    .align(Alignment.CenterStart)
                                                    .graphicsLayer {
                                                        translationX = (1f - t) * 180.dp.toPx() + 16.dp.toPx()
                                                        translationY = -t * 145.dp.toPx() - kotlin.math.sin(t * Math.PI.toFloat()) * 32.dp.toPx()
                                                        scaleX = 1f + kotlin.math.sin(t * Math.PI.toFloat()) * 0.28f
                                                        scaleY = 1f + kotlin.math.sin(t * Math.PI.toFloat()) * 0.28f
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Text(
                                                        text = "+${payerExtraPaise}p",
                                                        fontFamily = SplitMateBrandFontFamily,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = QuickExpenseThemeTokens.SageText
                                                    )
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                                        contentDescription = null,
                                                        tint = QuickExpenseThemeTokens.SageText,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Text(
                                                        text = "Payer",
                                                        fontFamily = SplitMateBrandFontFamily,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = QuickExpenseThemeTokens.SageText
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        // Left: 3x4 Keypad Column Grid (4 rows * 54dp + 3 gaps * 8dp = 240dp)
                        Column(
                            modifier = Modifier.weight(3f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KeypadRow(
                                keys = listOf("1", "2", "3"),
                                keypadBg = keypadBg,
                                keypadBorder = keypadBorder,
                                textPrimary = textPrimary,
                                onKeyPress = { key ->
                                    appendDigit(key, amountDigits) {
                                        amountDigits = it
                                        triggerKeystrokeEnergyPulse(it)
                                    }
                                }
                            )

                            KeypadRow(
                                keys = listOf("4", "5", "6"),
                                keypadBg = keypadBg,
                                keypadBorder = keypadBorder,
                                textPrimary = textPrimary,
                                onKeyPress = { key ->
                                    appendDigit(key, amountDigits) {
                                        amountDigits = it
                                        triggerKeystrokeEnergyPulse(it)
                                    }
                                }
                            )

                            KeypadRow(
                                keys = listOf("7", "8", "9"),
                                keypadBg = keypadBg,
                                keypadBorder = keypadBorder,
                                textPrimary = textPrimary,
                                onKeyPress = { key ->
                                    appendDigit(key, amountDigits) {
                                        amountDigits = it
                                        triggerKeystrokeEnergyPulse(it)
                                    }
                                }
                            )

                            val keypadView = androidx.compose.ui.platform.LocalView.current
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TactileSquircleKey(
                                    label = ".",
                                    keypadBg = keypadBg,
                                    keypadBorder = keypadBorder,
                                    textPrimary = textPrimary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp),
                                    onClick = {
                                        com.splitmate.app.ui.performCrispTactileHaptic(context, keypadView, heavy = false)
                                        if (!amountDigits.contains(".") && amountDigits.length < 9) {
                                            amountDigits = if (amountDigits.isEmpty()) "0." else "$amountDigits."
                                            triggerKeystrokeEnergyPulse(amountDigits)
                                        }
                                    }
                                )

                                TactileSquircleKey(
                                    label = "0",
                                    keypadBg = keypadBg,
                                    keypadBorder = keypadBorder,
                                    textPrimary = textPrimary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp),
                                    onClick = {
                                        com.splitmate.app.ui.performCrispTactileHaptic(context, keypadView, heavy = false)
                                        appendDigit("0", amountDigits) {
                                            amountDigits = it
                                            triggerKeystrokeEnergyPulse(it)
                                        }
                                    }
                                )

                                TactileSquircleKey(
                                    label = "BACK",
                                    isIcon = true,
                                    icon = Icons.AutoMirrored.Rounded.Backspace,
                                    keypadBg = keypadBg,
                                    keypadBorder = keypadBorder,
                                    textPrimary = textPrimary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp),
                                    onClick = {
                                        com.splitmate.app.ui.performCrispTactileHaptic(context, keypadView, heavy = false)
                                        if (amountDigits.isNotEmpty()) {
                                            val next = amountDigits.dropLast(1)
                                            amountDigits = next.ifEmpty { "0" }
                                            triggerKeystrokeEnergyPulse(amountDigits)
                                        }
                                    }
                                )
                            }
                        }

                        // Right: 4th Column (Row 1 = 54dp Note, Row 2 = 54dp Clear "C", Rows 3 & 4 = 116dp 2-Row Log & Split FAB)
                        val rightColView = androidx.compose.ui.platform.LocalView.current
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = {
                                    com.splitmate.app.ui.performCrispTactileHaptic(context, rightColView, heavy = true)
                                    showEditTitleDialog = true
                                    onLogExpenseClick()
                                },
                                shape = QuickExpenseThemeTokens.RadiusKeySquircle,
                                color = QuickExpenseThemeTokens.SageSurface,
                                shadowElevation = 0.dp,
                                border = BorderStroke(1.dp, QuickExpenseThemeTokens.AccentSage),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                        contentDescription = "Edit Note",
                                        tint = QuickExpenseThemeTokens.SageText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Note",
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = QuickExpenseThemeTokens.SageText
                                    )
                                }
                            }

                            TactileSquircleKey(
                                label = "C",
                                keypadBg = keypadBg,
                                keypadBorder = keypadBorder,
                                textPrimary = textPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                onClick = {
                                    com.splitmate.app.ui.performCrispTactileHaptic(context, rightColView, heavy = false)
                                    amountDigits = "0"
                                }
                            )

                            // Log & Split FAB spanning EXACTLY two rows in height (54dp + 8dp + 54dp = 116dp)
                            // Always 100% opaque — never transparent or washed-out white/grey when amount is 0.
                            val canCommitSplit = hasSelectedGroup && totalAmountPaise > 0L && selectedMemberIds.isNotEmpty()
                            val ctaEnabledContainer = if (isDark) activePalette.primary else activePalette.onSurface
                            val ctaEnabledContent = if (isDark) activePalette.onPrimary else activePalette.surfaceContainerLowest
                            val ctaContainerColor = if (canCommitSplit) {
                                ctaEnabledContainer
                            } else {
                                QuickExpenseThemeTokens.SageSurface
                            }
                            val ctaContentColor = if (canCommitSplit) {
                                ctaEnabledContent
                            } else {
                                QuickExpenseThemeTokens.SageText
                            }
                            Surface(
                                onClick = {
                                    com.splitmate.app.ui.performCrispTactileHaptic(context, rightColView, heavy = true)
                                    if (canCommitSplit) {
                                        if (expenseCategoryTitle.isBlank()) {
                                            pendingCommitAfterCategorySelection = true
                                            showEditTitleDialog = true
                                        } else {
                                            val selected = participants.filter { selectedMemberIds.contains(it.id) }
                                            viewModel?.commitQuickEqualExpense(
                                                title = expenseCategoryTitle,
                                                totalAmountCents = totalAmountPaise,
                                                selectedMemberIds = selected.map { it.id },
                                                payerMemberId = selectedPayerId,
                                                categoryRef = com.splitmate.app.ui.category.ExpenseCategoryRefs.refForTitle(expenseCategoryTitle, customCategories)
                                            )
                                            onSaveSplit(totalAmountPaise / 100L, selected)
                                        }
                                    }
                                },
                                shape = QuickExpenseThemeTokens.RadiusKeySquircle,
                                color = ctaContainerColor,
                                shadowElevation = if (canCommitSplit) 6.dp else 2.dp,
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    color = if (canCommitSplit) ctaEnabledContainer else QuickExpenseThemeTokens.AccentSage
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(116.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (canCommitSplit) {
                                            if (isDark) ctaEnabledContent.copy(alpha = 0.12f) else QuickExpenseThemeTokens.AccentSage.copy(alpha = 0.25f)
                                        } else {
                                            QuickExpenseThemeTokens.AccentSage.copy(alpha = 0.45f)
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.ElectricBolt,
                                                contentDescription = null,
                                                tint = if (canCommitSplit) {
                                                    if (isDark) ctaEnabledContent else QuickExpenseThemeTokens.AccentSage
                                                } else {
                                                    QuickExpenseThemeTokens.SageText
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Log &\nSplit",
                                        fontFamily = SplitMateDisplayFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ctaContentColor,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
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

// ==============================================================================
// KEYPAD ROW COMPOSABLE
// ==============================================================================
@Composable
private fun KeypadRow(
    keys: List<String>,
    keypadBg: Color = QuickExpenseThemeTokens.SurfaceKeypad,
    keypadBorder: Color = QuickExpenseThemeTokens.SurfaceKeypadBorder,
    textPrimary: Color = QuickExpenseThemeTokens.PrimaryDark,
    onKeyPress: (String) -> Unit
) {
    val context = LocalContext.current
    val localView = androidx.compose.ui.platform.LocalView.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { key ->
            TactileSquircleKey(
                label = key,
                keypadBg = keypadBg,
                keypadBorder = keypadBorder,
                textPrimary = textPrimary,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                onClick = {
                    com.splitmate.app.ui.performCrispTactileHaptic(context, localView, heavy = false)
                    onKeyPress(key)
                }
            )
        }
    }
}

// ==============================================================================
// TACTILE M3 SQUIRCLE KEY COMPONENT (24dp Radius + Soft Physics Shadow)
// ==============================================================================
@Composable
fun TactileSquircleKey(
    label: String,
    modifier: Modifier = Modifier,
    isIcon: Boolean = false,
    icon: ImageVector? = null,
    keypadBg: Color = QuickExpenseThemeTokens.SurfaceKeypad,
    keypadBorder: Color = QuickExpenseThemeTokens.SurfaceKeypadBorder,
    textPrimary: Color = QuickExpenseThemeTokens.PrimaryDark,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = SplitMateMotion.fastSpatial(),
        label = "KeyScale"
    )
    val morphCorner by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isPressed) 12.dp else 24.dp,
        animationSpec = SplitMateMotion.fastSpatial(),
        label = "GM3KeyCornerMorph"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(morphCorner),
        color = if (isPressed) keypadBorder else keypadBg,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, keypadBorder),
        modifier = modifier
            .scale(scale)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isIcon && icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = label,
                    fontFamily = SplitMateDisplayFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }
        }
    }
}

// Helper functions for appending keypad input & exact paise parsing
private fun appendDigit(key: String, current: String, onUpdate: (String) -> Unit) {
    if (current.contains(".")) {
        val decimals = current.substringAfter(".")
        if (decimals.length < 2 && current.length < 11) {
            onUpdate(current + key)
        }
    } else if (current.length < 8) {
        if (current == "0") {
            onUpdate(key)
        } else {
            onUpdate(current + key)
        }
    }
}

private fun parseAmountInputToPaise(input: String): Long {
    val clean = input.trim()
    if (clean.isEmpty() || clean == "0" || clean == "0.") return 0L
    return if (clean.contains(".")) {
        val whole = clean.substringBefore(".").toLongOrNull() ?: 0L
        val decRaw = clean.substringAfter(".").take(2)
        val decPaise = when (decRaw.length) {
            0 -> 0L
            1 -> (decRaw.toLongOrNull() ?: 0L) * 10L
            else -> decRaw.toLongOrNull() ?: 0L
        }
        whole * 100L + decPaise
    } else {
        (clean.toLongOrNull() ?: 0L) * 100L
    }
}

private fun formatPaiseWithInputDisplay(paise: Long, rawInput: String, currencySymbol: String): String {
    if (paise == 0L && !rawInput.contains(".")) return "$currencySymbol 0"
    val whole = paise / 100L
    val formattedWhole = com.splitmate.app.ui.formatIndianIntegerGrouping(whole)
    return if (rawInput.contains(".")) {
        val decPart = rawInput.substringAfter(".").take(2)
        "$currencySymbol $formattedWhole.$decPart"
    } else {
        val remPaise = kotlin.math.abs(paise % 100L)
        if (remPaise > 0L) {
            "$currencySymbol $formattedWhole.${String.format(Locale.US, "%02d", remPaise)}"
        } else {
            "$currencySymbol $formattedWhole"
        }
    }
}

private fun formatPaiseForSplitBadge(paise: Long): String {
    val whole = paise / 100L
    val rem = kotlin.math.abs(paise % 100L)
    val formattedWhole = com.splitmate.app.ui.formatIndianIntegerGrouping(whole)
    return if (rem == 0L) formattedWhole else "$formattedWhole.${String.format(Locale.US, "%02d", rem)}"
}

private data class EditableMemberDraft(
    val memberId: String,
    val name: String,
    val style: String,
    val upiId: String,
    /** v2.3.5: real VPA (e.g. `rohan@okaxis`); derived `<phone>@upi` handles show blank. */
    val vpa: String = "",
    /** v2.3.5: true once the user edited the UPI ID field (blank then means "clear it"). */
    val vpaTouched: Boolean = false
)

/** v2.3.5: one place that builds a member's initial draft from the stored row (issue #7 RC2). */
private fun initialMemberDraftOf(m: GroupMemberEntity): EditableMemberDraft = EditableMemberDraft(
    memberId = m.memberId,
    name = m.name,
    style = com.splitmate.app.MemberProfileEditRules.initialGenderIdOf(m.avatarSeed, m.name),
    upiId = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId),
    vpa = com.splitmate.app.MemberProfileEditRules.editableVpaOf(m.upiId)
)

// ==============================================================================
// EDIT FRIEND PERSONA, 10-DIGIT MOBILE NUMBER & BATCH MEMBER EDITOR DIALOG
// ==============================================================================
@Composable
fun EditFriendUpiDialog(
    member: GroupMemberEntity,
    allGroupMembers: List<GroupMemberEntity> = listOf(member),
    onSelectMember: (GroupMemberEntity) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (name: String, upiId: String, avatarSeed: String) -> Unit,
    onSaveAll: ((List<com.splitmate.app.ui.SplitMateViewModel.BatchMemberUpdate>) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val effectiveMembers = remember(allGroupMembers, member) {
        if (allGroupMembers.isEmpty()) listOf(member) else allGroupMembers
    }

    // Persistent draft map across ALL members in the group so switching members never loses edits!
    // Note: EditableMemberDraft.upiId stores the 10-digit mobile number string for phone sync.
    // v2.3.5 (#7): keyed on the member ids only, so a background sync that touches member rows
    // while the dialog is open no longer resets the user's in-progress edits.
    val effectiveMemberIds = effectiveMembers.map { it.memberId }
    val memberDrafts = remember(effectiveMemberIds) {
        androidx.compose.runtime.mutableStateMapOf<String, EditableMemberDraft>().apply {
            effectiveMembers.forEach { m -> put(m.memberId, initialMemberDraftOf(m)) }
        }
    }

    var activeMemberId by remember(member.memberId) { mutableStateOf(member.memberId) }
    val activeMember = remember(activeMemberId, effectiveMembers) {
        effectiveMembers.find { it.memberId == activeMemberId } ?: member
    }
    val activeDraft = memberDrafts[activeMember.memberId] ?: initialMemberDraftOf(activeMember)

    var showInAppContactPicker by remember { mutableStateOf(false) }
    var deviceContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var isLoadingContacts by remember { mutableStateOf(false) }

    val loadAndOpenSheet = {
        showInAppContactPicker = true
        scope.launch {
            isLoadingContacts = true
            deviceContacts = withContext(Dispatchers.IO) { queryAllDeviceContacts(context) }
            isLoadingContacts = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            loadAndOpenSheet()
        }
    }

    val activeCompositeSeed = remember(activeMember.avatarSeed, activeDraft.name, activeDraft.style) {
        val baseDesc = com.splitmate.app.ui.AvatarSeedCodec.parse(activeMember.avatarSeed.ifBlank { activeMember.name })
        val resolvedSeedKey = if (activeDraft.name.trim() == activeMember.name.trim() && baseDesc.seedKey.isNotBlank()) {
            baseDesc.seedKey
        } else {
            activeDraft.name.trim().ifEmpty { activeMember.name }
        }
        com.splitmate.app.ui.AvatarSeedCodec.encode(
            seedKey = resolvedSeedKey,
            gender = com.splitmate.app.ui.AvatarGender.fromId(activeDraft.style),
            styleId = baseDesc.styleId,
            colorPresetId = baseDesc.colorPresetId
        )
    }
    val avatarPreviewUrl = remember(activeCompositeSeed) {
        buildDiceBearOpenPeepsUrl(activeCompositeSeed)
    }
    val cleanInitials = remember(activeDraft.name, activeMember.name) {
        extractInitialsFromNameOrSeed(activeDraft.name.ifBlank { activeMember.name })
    }

    if (showInAppContactPicker) {
        ContactPickerBottomSheet(
            contacts = deviceContacts,
            isLoading = isLoadingContacts,
            multiSelect = false,
            title = "Select Contact for ${activeDraft.name.ifBlank { activeMember.name }}",
            subtitle = "Choose a contact from your phonebook to link name & 10-digit mobile number",
            onDismissRequest = { showInAppContactPicker = false },
            onConfirmSelected = { selected ->
                val chosen = selected.firstOrNull()
                if (chosen != null) {
                    val cleanPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(chosen.cleanPhone)
                    memberDrafts[activeMember.memberId] = activeDraft.copy(
                        name = chosen.name,
                        upiId = if (cleanPhone10.length == 10) cleanPhone10 else activeDraft.upiId
                    )
                }
                showInAppContactPicker = false
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = QuickExpenseThemeTokens.SurfaceWhite,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(QuickExpenseThemeTokens.AccentSage)
                            .border(2.dp, QuickExpenseThemeTokens.SageBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cleanInitials,
                            fontFamily = SplitMateDisplayFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = QuickExpenseThemeTokens.SageText
                        )
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(avatarPreviewUrl)
                                .decoderFactory(SvgDecoder.Factory())
                                .diskCacheKey("dicebear_avatar_$avatarPreviewUrl")
                                .memoryCacheKey("dicebear_avatar_$avatarPreviewUrl")
                                .crossfade(true)
                                .build(),
                            contentDescription = "Friend Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Edit Group Members",
                            fontFamily = SplitMateDisplayFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = QuickExpenseThemeTokens.PrimaryDark
                        )
                        Text(
                            text = "Update member names, avatars & optional mobile numbers",
                            fontFamily = SplitMateBrandFontFamily,
                            fontSize = 12.sp,
                            color = QuickExpenseThemeTokens.TextSecondary
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // 0. Batch Member Quick-Matrix (Configure Masculine/Feminine/Neutral for ALL members one by one without closing!)
                    if (effectiveMembers.size > 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "All Group Members (${effectiveMembers.size}) — Tap M / F / N or Select to Edit",
                                fontFamily = SplitMateBrandFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = QuickExpenseThemeTokens.PrimaryDark
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                effectiveMembers.forEach { candidate ->
                                    val draft = memberDrafts[candidate.memberId] ?: initialMemberDraftOf(candidate)
                                    val candidateBaseDesc = com.splitmate.app.ui.AvatarSeedCodec.parse(candidate.avatarSeed.ifBlank { candidate.name })
                                    val candidateCompositeSeed = com.splitmate.app.ui.AvatarSeedCodec.encode(
                                        seedKey = if (draft.name.trim() == candidate.name.trim() && candidateBaseDesc.seedKey.isNotBlank()) candidateBaseDesc.seedKey else draft.name.trim().ifEmpty { candidate.name },
                                        gender = com.splitmate.app.ui.AvatarGender.fromId(draft.style),
                                        styleId = candidateBaseDesc.styleId,
                                        colorPresetId = candidateBaseDesc.colorPresetId
                                    )
                                    val candidateUrl = buildDiceBearOpenPeepsUrl(candidateCompositeSeed)
                                    val isCurrentTarget = candidate.memberId == activeMember.memberId
                                    val cleanDraftPhone10 = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(draft.upiId, draft.upiId)
                                    Surface(
                                        onClick = {
                                            activeMemberId = candidate.memberId
                                            onSelectMember(candidate)
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isCurrentTarget) QuickExpenseThemeTokens.SageSurface else QuickExpenseThemeTokens.SurfaceKeypad,
                                        border = BorderStroke(
                                            if (isCurrentTarget) 1.5.dp else 1.dp,
                                            if (isCurrentTarget) QuickExpenseThemeTokens.SageBorder else QuickExpenseThemeTokens.BorderLight
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(QuickExpenseThemeTokens.AccentSage),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(context)
                                                            .data(candidateUrl)
                                                            .decoderFactory(SvgDecoder.Factory())
                                                            .diskCacheKey("dicebear_avatar_$candidateUrl")
                                                            .memoryCacheKey("dicebear_avatar_$candidateUrl")
                                                            .build(),
                                                        contentDescription = draft.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = draft.name,
                                                        fontFamily = SplitMateBrandFontFamily,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = QuickExpenseThemeTokens.PrimaryDark,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = if (cleanDraftPhone10.length == 10) "+91 $cleanDraftPhone10" else "Offline Member • Tap to link phone",
                                                        fontFamily = SplitMateBrandFontFamily,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (cleanDraftPhone10.length == 10) QuickExpenseThemeTokens.SageText else QuickExpenseThemeTokens.TextSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            // Inline 1-Tap M / F / N pills for every member so all can be set rapidly!
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                // v2.3.5 (#7 RC2): ids are the stored AvatarGender ids (Male/Female/Neutral).
                                                com.splitmate.app.MemberProfileEditRules.GENDER_PILLS.forEach { (fullStyle, shortCode) ->
                                                    val selected = com.splitmate.app.MemberProfileEditRules.isGenderSelected(draft.style, fullStyle)
                                                    Surface(
                                                        onClick = {
                                                            // v2.3.5 (#7 RC3): keep host + dialog selection in sync.
                                                            activeMemberId = candidate.memberId
                                                            onSelectMember(candidate)
                                                            memberDrafts[candidate.memberId] = draft.copy(style = fullStyle)
                                                        },
                                                        shape = CircleShape,
                                                        color = if (selected) QuickExpenseThemeTokens.PrimaryDark else QuickExpenseThemeTokens.SurfaceWhite,
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (selected) QuickExpenseThemeTokens.PrimaryDark else QuickExpenseThemeTokens.BorderLight
                                                        ),
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = shortCode,
                                                                fontFamily = SplitMateBrandFontFamily,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = if (selected) QuickExpenseThemeTokens.ScreenBg else QuickExpenseThemeTokens.TextSecondary
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

                    // 1. Active Member Presentation Style Toggle (Masculine, Feminine, Neutral)
                    Column {
                        Text(
                            text = "Avatar Style for ${activeDraft.name}",
                            fontFamily = SplitMateBrandFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuickExpenseThemeTokens.PrimaryDark,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Surface(
                            shape = QuickExpenseThemeTokens.RadiusPill,
                            color = QuickExpenseThemeTokens.SurfaceKeypad,
                            border = BorderStroke(1.dp, QuickExpenseThemeTokens.BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // v2.3.5 (#7 RC2): stored ids (Male/Female/Neutral) with human labels.
                                com.splitmate.app.MemberProfileEditRules.GENDER_SEGMENTS.forEach { (style, styleLabel) ->
                                    val isSelected = com.splitmate.app.MemberProfileEditRules.isGenderSelected(activeDraft.style, style)
                                    Surface(
                                        onClick = {
                                            memberDrafts[activeMember.memberId] = activeDraft.copy(style = style)
                                        },
                                        shape = QuickExpenseThemeTokens.RadiusPill,
                                        color = if (isSelected) QuickExpenseThemeTokens.PrimaryDark else Color.Transparent,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = styleLabel,
                                                fontFamily = SplitMateBrandFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (isSelected) QuickExpenseThemeTokens.ScreenBg else QuickExpenseThemeTokens.TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // v2.3.5 (#7): editable display name for the selected member.
                    OutlinedTextField(
                        value = activeDraft.name,
                        onValueChange = { newName ->
                            memberDrafts[activeMember.memberId] = activeDraft.copy(name = newName.replace("|", " "))
                        },
                        label = { Text("Name", fontFamily = SplitMateBrandFontFamily, fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QuickExpenseThemeTokens.SageText,
                            unfocusedBorderColor = QuickExpenseThemeTokens.BorderLight,
                            focusedContainerColor = QuickExpenseThemeTokens.SurfaceKeypad,
                            unfocusedContainerColor = QuickExpenseThemeTokens.SurfaceKeypad,
                            focusedTextColor = QuickExpenseThemeTokens.PrimaryDark,
                            unfocusedTextColor = QuickExpenseThemeTokens.PrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2. EDITABLE 10-DIGIT MOBILE NUMBER FIELD
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "10-Digit Mobile Number for ${activeDraft.name} (Optional)",
                            fontFamily = SplitMateBrandFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuickExpenseThemeTokens.PrimaryDark
                        )
                        OutlinedTextField(
                            value = activeDraft.upiId,
                            onValueChange = { newPhone ->
                                memberDrafts[activeMember.memberId] = activeDraft.copy(upiId = newPhone.trim())
                            },
                            placeholder = {
                                Text(
                                    text = "e.g. 9876543210 (leave blank for offline member)",
                                    fontFamily = SplitMateBrandFontFamily,
                                    fontSize = 12.sp,
                                    color = QuickExpenseThemeTokens.TextSecondary
                                )
                            },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = QuickExpenseThemeTokens.SageText,
                                unfocusedBorderColor = QuickExpenseThemeTokens.BorderLight,
                                focusedContainerColor = QuickExpenseThemeTokens.SurfaceKeypad,
                                unfocusedContainerColor = QuickExpenseThemeTokens.SurfaceKeypad,
                                focusedTextColor = QuickExpenseThemeTokens.PrimaryDark,
                                unfocusedTextColor = QuickExpenseThemeTokens.PrimaryDark
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // v2.3.5 (#7): optional real UPI ID. Left blank -> "<phone>@upi" is derived;
                    // a typed VPA (e.g. rohan@okaxis) is kept and never overwritten by phone edits.
                    OutlinedTextField(
                        value = activeDraft.vpa,
                        onValueChange = { newVpa ->
                            memberDrafts[activeMember.memberId] = activeDraft.copy(vpa = newVpa.trim(), vpaTouched = true)
                        },
                        label = { Text("UPI ID (optional)", fontFamily = SplitMateBrandFontFamily, fontSize = 12.sp) },
                        placeholder = {
                            Text(
                                text = "e.g. name@okaxis (blank = phone@upi)",
                                fontFamily = SplitMateBrandFontFamily,
                                fontSize = 12.sp,
                                color = QuickExpenseThemeTokens.TextSecondary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QuickExpenseThemeTokens.SageText,
                            unfocusedBorderColor = QuickExpenseThemeTokens.BorderLight,
                            focusedContainerColor = QuickExpenseThemeTokens.SurfaceKeypad,
                            unfocusedContainerColor = QuickExpenseThemeTokens.SurfaceKeypad,
                            focusedTextColor = QuickExpenseThemeTokens.PrimaryDark,
                            unfocusedTextColor = QuickExpenseThemeTokens.PrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. Pick / Replace from Contacts Button (Opens In-App ContactPickerBottomSheet)
                    Button(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                loadAndOpenSheet()
                            } else {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = QuickExpenseThemeTokens.SageSurface,
                            contentColor = QuickExpenseThemeTokens.SageText
                        ),
                        border = BorderStroke(1.5.dp, QuickExpenseThemeTokens.SageBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Contacts,
                            contentDescription = null,
                            tint = QuickExpenseThemeTokens.SageText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Link / Replace from Phone Contacts",
                            fontFamily = SplitMateBrandFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = QuickExpenseThemeTokens.SageText
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onSaveAll != null) {
                            val batchUpdates = effectiveMembers.map { m ->
                                val d = memberDrafts[m.memberId] ?: initialMemberDraftOf(m)
                                val cleanName = d.name.trim().ifEmpty { m.name }
                                val encodedSeed = com.splitmate.app.MemberProfileEditRules.resolveMemberAvatarSeed(
                                    existingSeed = m.avatarSeed,
                                    existingName = m.name,
                                    draftName = cleanName,
                                    genderId = d.style
                                )
                                com.splitmate.app.ui.SplitMateViewModel.BatchMemberUpdate(
                                    memberId = m.memberId,
                                    name = cleanName,
                                    upiId = d.upiId.trim(),
                                    avatarSeed = encodedSeed,
                                    vpaOverride = if (d.vpaTouched) d.vpa.trim() else null
                                )
                            }
                            onSaveAll(batchUpdates)
                        } else {
                            val cleanName = activeDraft.name.trim().ifEmpty { activeMember.name }
                            val baseDesc = com.splitmate.app.ui.AvatarSeedCodec.parse(activeMember.avatarSeed.ifBlank { activeMember.name })
                            val resolvedSeedKey = if (cleanName == activeMember.name.trim() && baseDesc.seedKey.isNotBlank()) baseDesc.seedKey else cleanName
                            val styledSeed = com.splitmate.app.ui.AvatarSeedCodec.encode(
                                seedKey = resolvedSeedKey,
                                gender = com.splitmate.app.ui.AvatarGender.fromId(activeDraft.style),
                                styleId = baseDesc.styleId,
                                colorPresetId = baseDesc.colorPresetId
                            )
                            onSave(cleanName, activeDraft.upiId.trim(), styledSeed)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QuickExpenseThemeTokens.PrimaryDark,
                        contentColor = QuickExpenseThemeTokens.ScreenBg
                    )
                ) {
                    Text(
                        text = if (effectiveMembers.size > 1) "Save All Members (${effectiveMembers.size})" else "Save Member",
                        fontFamily = SplitMateBrandFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        "Cancel",
                        fontFamily = SplitMateBrandFontFamily,
                        color = QuickExpenseThemeTokens.TextSecondary
                    )
                }
            }
        )
    }
}

