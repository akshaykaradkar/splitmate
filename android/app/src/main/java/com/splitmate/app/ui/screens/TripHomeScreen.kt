@file:OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)

package com.splitmate.app.ui.screens

import com.splitmate.app.ui.components.expressivePressScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.runtime.saveable.rememberSaveable
import com.splitmate.app.ui.components.LocalMotionScheme
import androidx.compose.foundation.layout.ColumnScope
import kotlin.math.roundToInt
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.clipToBounds
import kotlin.math.roundToLong
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.animation.togetherWith
import com.splitmate.app.ui.theme.HubExtendedColors
import android.content.Intent
import android.net.Uri
import com.splitmate.app.ui.screens.plan.PlanSubView
import com.splitmate.app.ui.screens.plan.TripPlanTabHost
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.graphics.shapes.Morph
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.LocalActivity
import androidx.compose.material.icons.rounded.LocalMall
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.Send
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.pulltorefresh.pullToRefresh
import com.splitmate.app.ui.components.staggeredEntrance
import com.splitmate.app.ui.components.ExpressiveSwipeAction
import com.splitmate.app.ui.components.ExpressiveSwipeActionsBox
import com.splitmate.app.ui.components.SettledCelebrationBadge
import com.splitmate.app.ui.components.SettledCelebrationDefaults
import com.splitmate.app.ui.components.SharedGroupKeys
import com.splitmate.app.ui.components.sharedGroupElement
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import android.content.Context
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.splitmate.app.SplitMateMathEngine
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseGroupEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.data.PnrNetworkRepository
import com.splitmate.app.ui.BuckwheatOlivePrimary
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.FigtreeFontFamily
import com.splitmate.app.ui.LivePnrStatusSnapshot
import com.splitmate.app.ui.ParsedTravelTicket
import com.splitmate.app.ui.SplitMateTnumMonospace
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.buildDiceBearOpenPeepsUrl
import com.splitmate.app.ui.cleanDisplayExpenseTitle
import com.splitmate.app.ui.components.ActiveTravelPassMode
import com.splitmate.app.ui.components.AnimatedTransitDeckHeroCard
import com.splitmate.app.ui.components.ButtonGroup
import com.splitmate.app.ui.components.ContainedLoadingIndicator
import com.splitmate.app.ui.components.ExperimentalMaterial3ExpressiveApi
import com.splitmate.app.ui.components.ExpressiveActionItem
import com.splitmate.app.ui.components.ExpressiveFabMenuItem
import com.splitmate.app.ui.components.ExpressiveMenuAction
import com.splitmate.app.ui.components.FloatingActionButtonMenu
import com.splitmate.app.ui.components.MaterialShapes
import com.splitmate.app.ui.components.RoundedPolygonShape
import com.splitmate.app.ui.components.SplitButtonLayout
import com.splitmate.app.ui.components.SplitMateMotion
import com.splitmate.app.ui.components.WavyProgressIndicatorDefaults
import com.splitmate.app.ui.components.rememberAnimatedSegmentedIslandItemShape
import com.splitmate.app.ui.components.segmentedIslandItemShape
import com.splitmate.app.ui.components.toShape
import com.splitmate.app.ui.components.EditorialFinancialTotalText
import androidx.compose.foundation.layout.consumeWindowInsets
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

/** v2.4.0 Trip Hub step 2: 4 tabs. Tickets now sit on their day in Plan (the Travel tab is gone). */
enum class TripHubSectionTab(val title: String) {
    OVERVIEW("Overview"),
    PLAN("Plan"),
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
    FOOD("Food & Drinks", Icons.Rounded.Restaurant),
    ACTIVITIES("Activities", Icons.Rounded.LocalActivity),
    SHOPPING("Shopping", Icons.Rounded.LocalMall),
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
 * Strictly checks `TRAIN` before `FLIGHT` so 10-digit PNRs, 5-digit train numbers, `"Train/Flight"`
 * prefixes, and train E-Ticket PDFs without an airline/flight number are never misclassified as Flights.
 */
fun classifyGroupExpenseForTripHub(expense: ExpenseEntity, customCategories: List<com.splitmate.app.ui.category.ExpenseCategory> = emptyList()): TripHubBookingCategory {
    // 0. Structured category written by the PNR / travel flows wins (unchanged since v2.0).
    when (expense.expenseCategory.trim().uppercase(Locale.US)) {
        "TRAIN" -> return TripHubBookingCategory.TRAIN
        "FLIGHT" -> return TripHubBookingCategory.FLIGHT
        "STAY", "HOTEL" -> return TripHubBookingCategory.STAY
        "RENTAL" -> return TripHubBookingCategory.RENTAL
        "CAB" -> return TripHubBookingCategory.CAB
        "FOOD", "DINING" -> return TripHubBookingCategory.FOOD
        "GROCERIES", "SHOPPING" -> return TripHubBookingCategory.SHOPPING
    }

    // 0b. v2.3.5: an explicit category chip (categoryRef) is the user's intent; travel tickets still
    // fall through to the ticket detectors below so boarding-pass / train cards are unchanged.
    val refBucket = expense.categoryRef?.takeIf { it.isNotBlank() }?.let { ref ->
        com.splitmate.app.ui.category.ExpenseBucketResolver.resolveRef(ref, expense.title, customCategories)
            ?.let { com.splitmate.app.ui.category.ExpenseBucketResolver.resolveBucket(it) }
    }
    if (refBucket != null && refBucket != com.splitmate.app.ui.category.SpendBucket.TRAVEL_TICKETS) {
        return tripHubCategoryForBucket(refBucket, expense.title)
    }

    val rawTitle = expense.title.trim()
    val lower = rawTitle.replace(Regex("""train/flight|flight/train""", RegexOption.IGNORE_CASE), "train").lowercase(Locale.US)
    val parsedTicket = extractTravelTicketFromTitle(rawTitle)

    // 1. Train check FIRST (10-digit numeric PNR, 5-digit train number, Indian Railways keywords,
    // or a PDF ticket saved with generic "Flight (" prefix but zero airline name and zero flight number)
    val hasTenDigitPnr = Regex("""\b\d{10}\b""").containsMatchIn(rawTitle) ||
        (parsedTicket?.pnr?.length == 10 && parsedTicket.pnr.all { it.isDigit() })
    val hasFiveDigitTrainNo = Regex("""^\d{5}\b""").containsMatchIn(parsedTicket?.trainOrFlightNo?.trim().orEmpty()) ||
        Regex("""\btrain\s+\d{5}\b""").containsMatchIn(lower)
    val hasExplicitAirlineOrFlightNo = Regex("""\b(indigo|air india|akasa|spicejet|vistara|airasia|alliance air|star air|fly91|emirates|qatar|lufthansa)\b""").containsMatchIn(lower) ||
        Regex("""\b(6e|ai|ix|qp|sg|uk|i5|9i|s5)[\s\-]?\d{2,4}\b""").containsMatchIn(lower)
    val hasSixCharFlightPnr = parsedTicket?.pnr?.let { it.length == 6 && it.any { ch -> ch.isLetter() } } == true
    val isTrainPdfMislabelledAsFlight = rawTitle.startsWith("Flight", ignoreCase = true) &&
        parsedTicket?.trainOrFlightNo.isNullOrBlank() &&
        !hasExplicitAirlineOrFlightNo &&
        !hasSixCharFlightPnr &&
        (rawTitle.startsWith("Flight  (") || rawTitle.startsWith("Flight ("))
    val sanitizedLowerForTrain = lower.replace("air india express", "air india")

    if (hasTenDigitPnr ||
        hasFiveDigitTrainNo ||
        isTrainPdfMislabelledAsFlight ||
        (!hasExplicitAirlineOrFlightNo && !hasSixCharFlightPnr && Regex("""\b(train|irctc|express|shatabdi|rajdhani|vande bharat|vande|duronto|sleeper|berth|coach|3a|2a|1a|3e|2s)\b""").containsMatchIn(sanitizedLowerForTrain))
    ) {
        return TripHubBookingCategory.TRAIN
    }

    // 2. Flight check SECOND (6-char alphanumeric PNR with explicit airline/flight indicators)
    if (isFlightTicketExpense(rawTitle, parsedTicket) ||
        hasExplicitAirlineOrFlightNo ||
        Regex("""\b(flight|airfare|boarding pass)\b""").containsMatchIn(lower)
    ) {
        return TripHubBookingCategory.FLIGHT
    }

    // Fallback travel ticket metadata defaults to TRAIN
    if (parsedTicket != null && parsedTicket.hasTicketMetadata) {
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

    // 6. v2.3.5: anything that used to land in "Other" now goes through the category catalog
    // (built-in exact titles + group-shared custom categories with their parent bucket), then a
    // conservative food keyword pass (breakfast / lunch / dinner ...).
    val titleBucket = com.splitmate.app.ui.category.ExpenseBucketResolver.resolveRef(null, rawTitle, customCategories)
        ?.let { com.splitmate.app.ui.category.ExpenseBucketResolver.resolveBucket(it) }
    if (titleBucket != null &&
        titleBucket != com.splitmate.app.ui.category.SpendBucket.OTHER &&
        titleBucket != com.splitmate.app.ui.category.SpendBucket.TRAVEL_TICKETS
    ) {
        return tripHubCategoryForBucket(titleBucket, rawTitle)
    }
    if (Regex("""\b(breakfast|lunch|dinner|brunch|snacks?|food|meal|restaurant|cafe|café|dhaba|tea|chai|coffee|drinks|juice|biryani|thali|seafood|fish)\b""").containsMatchIn(lower)) {
        return TripHubBookingCategory.FOOD
    }
    if (Regex("""\b(fuel|petrol|diesel|toll|parking)\b""").containsMatchIn(lower)) {
        return TripHubBookingCategory.CAB
    }

    // 7. General Shared Expense
    return TripHubBookingCategory.GENERAL
}

/** v2.3.5: parent spend bucket -> Trip Hub card/filter archetype. */
internal fun tripHubCategoryForBucket(
    bucket: com.splitmate.app.ui.category.SpendBucket,
    title: String
): TripHubBookingCategory = when (bucket) {
    com.splitmate.app.ui.category.SpendBucket.STAY -> TripHubBookingCategory.STAY
    com.splitmate.app.ui.category.SpendBucket.LOCAL_TRANSPORT ->
        if (Regex("""\b(rental|enfield|moped|scooter|bike|scooty|two wheeler|cycle|kayak|coracle)\b""", RegexOption.IGNORE_CASE).containsMatchIn(title)) {
            TripHubBookingCategory.RENTAL
        } else {
            TripHubBookingCategory.CAB
        }
    com.splitmate.app.ui.category.SpendBucket.FOOD -> TripHubBookingCategory.FOOD
    com.splitmate.app.ui.category.SpendBucket.ACTIVITIES -> TripHubBookingCategory.ACTIVITIES
    com.splitmate.app.ui.category.SpendBucket.SHOPPING -> TripHubBookingCategory.SHOPPING
    com.splitmate.app.ui.category.SpendBucket.TRAVEL_TICKETS -> TripHubBookingCategory.GENERAL
    com.splitmate.app.ui.category.SpendBucket.OTHER -> TripHubBookingCategory.GENERAL
}

data class ResolvedExpenseSchedule(
    val effectiveEpochMs: Long,
    val shortDateLabel: String,
    val fullDateLabel: String,
    val timeLabel: String,
    val hasExplicitTicketDate: Boolean
)

private val resolvedScheduleCache = java.util.concurrent.ConcurrentHashMap<String, ResolvedExpenseSchedule>()

/**
 * Resolves the actual travel/departure date & time from a Train PNR or Flight Boarding Pass PDF
 * (via `PnrNetworkRepository` vault or `ParsedTravelTicket` title metadata), falling back to
 * `expense.createdAtEpochMs` only for general expenses that have no travel date.
 */
fun resolveExpenseSchedule(
    context: Context?,
    expense: ExpenseEntity
): ResolvedExpenseSchedule {
    val cacheKey = "${expense.expenseId}_${expense.title.hashCode()}_${expense.scheduledAtEpochMs}_${expense.createdAt}"
    resolvedScheduleCache[cacheKey]?.let { return it }

    val explicitEpoch = expense.scheduledAtEpochMs
    if (explicitEpoch != null && explicitEpoch > 0L) {
        val dateObj = Date(explicitEpoch)
        val shortDate = SimpleDateFormat("d MMM", Locale.US).format(dateObj)
        val fullDate = SimpleDateFormat("d MMM yyyy", Locale.US).format(dateObj)
        val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(dateObj)
        return ResolvedExpenseSchedule(
            effectiveEpochMs = explicitEpoch,
            shortDateLabel = shortDate,
            fullDateLabel = fullDate,
            timeLabel = timeStr,
            hasExplicitTicketDate = true
        ).also { resolvedScheduleCache[cacheKey] = it }
    }

    val parsedTicket = extractTravelTicketFromTitle(expense.title)
    val pnrCandidate = expense.travelPnr?.trim()?.takeIf { it.isNotBlank() }
        ?: parsedTicket?.pnr?.trim()?.takeIf { it.isNotBlank() }
        ?: ""

    val flightVault = if (context != null && (pnrCandidate.length == 6 || pnrCandidate.length == 10)) {
        PnrNetworkRepository.loadConfirmedFlightTicketResult(context, pnrCandidate)
    } else null

    val pnrSnapshot = if (context != null && (pnrCandidate.length == 6 || pnrCandidate.length == 10)) {
        loadPersistedPnrSnapshot(context, pnrCandidate)
    } else null

    val candidateDateTimeStrings = listOfNotNull(
        flightVault?.let { listOf(it.travelDate, it.departureTime).filter { s -> s.isNotBlank() }.joinToString(" ") }?.takeIf { it.isNotBlank() },
        pnrSnapshot?.departureTime?.takeIf { it.isNotBlank() },
        parsedTicket?.departureInfo?.takeIf { it.isNotBlank() },
        parsedTicket?.departureDate?.takeIf { it.isNotBlank() },
        parsedTicket?.departureTime?.takeIf { it.isNotBlank() }
    )

    var parsedInstant: com.splitmate.app.data.TicketDateTimeParser.ParsedTicketInstant? = null
    for (rawCandidate in candidateDateTimeStrings) {
        parsedInstant = com.splitmate.app.data.TicketDateTimeParser.parse(rawCandidate, expense.createdAtEpochMs)
        if (parsedInstant != null) break
    }

    val effectiveEpoch = parsedInstant?.epochMs ?: expense.createdAtEpochMs
    val effectiveDate = Date(effectiveEpoch)
    val shortStr = SimpleDateFormat("d MMM", Locale.US).format(effectiveDate)
    val fullStr = SimpleDateFormat("d MMM yyyy", Locale.US).format(effectiveDate)
    val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(effectiveDate)

    return ResolvedExpenseSchedule(
        effectiveEpochMs = effectiveEpoch,
        shortDateLabel = shortStr,
        fullDateLabel = fullStr,
        timeLabel = timeStr,
        hasExplicitTicketDate = parsedInstant != null
    ).also { resolvedScheduleCache[cacheKey] = it }
}

@Composable
fun TripHubMemberAvatar(
    seedOrName: String,
    fallbackName: String = seedOrName,
    size: Dp = 42.dp,
    backgroundColor: Color,
    textColor: Color,
    fontSize: TextUnit = 14.sp,
    isOnline: Boolean = false
) {
    val context = LocalContext.current
    val effectiveSeed = seedOrName.ifBlank { fallbackName }
    val parsedDescriptor = remember(effectiveSeed) {
        com.splitmate.app.ui.AvatarSeedCodec.parse(effectiveSeed)
    }
    val presetBg = remember(effectiveSeed, parsedDescriptor.colorPresetId, backgroundColor) {
        if (effectiveSeed.contains('|')) {
            com.splitmate.app.ui.SplitMateAvatarColorPresets
                .find { it.id.equals(parsedDescriptor.colorPresetId, ignoreCase = true) }
                ?.primaryBgColor ?: backgroundColor
        } else {
            backgroundColor
        }
    }
    val svgUrl = remember(effectiveSeed) { buildDiceBearOpenPeepsUrl(effectiveSeed) }
    val initials = remember(fallbackName) { extractInitialsFromNameOrSeed(fallbackName) }

    // M3 Expressive avatar geometry: 9-sided cookie instead of a plain circle.
    val avatarShape = remember { MaterialShapes.Cookie9Sided.toShape() }

    Box(modifier = Modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(avatarShape)
                .background(presetBg)
                .border(1.5.dp, TripHubTokens.CardSurface, avatarShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                fontFamily = FigtreeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize,
                color = textColor
            )
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(svgUrl)
                    .decoderFactory(SvgDecoder.Factory())
                    .diskCacheKey("dicebear_avatar_$svgUrl")
                    .memoryCacheKey("dicebear_avatar_$svgUrl")
                    .crossfade(true)
                    .build(),
                contentDescription = fallbackName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(avatarShape)
            )
        }
        if (isOnline) {
            val dotSize = if (size >= 40.dp) 12.dp else 9.dp
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(HubExtendedColors.presence().color)
                    .border(1.5.dp, TripHubTokens.CardSurface, CircleShape)
            )
        }
    }
}

// ==============================================================================
// 2. 3-THEME EXPRESSIVE TOKENS (`TripHubTokens`) — BUCKWHEAT, ESPRESSO & MATCHA
// ==============================================================================

object TripHubTokens {
    val CanvasBg: Color
        get() = DesignSystemBindings.activePalette.surfaceContainerLow
    val CardSurface: Color
        get() = DesignSystemBindings.activePalette.surfaceContainerLowest
    val SunkenWell: Color
        get() = DesignSystemBindings.activePalette.surfaceContainer
    val CardBorder: Color
        get() = DesignSystemBindings.activePalette.outlineVariant
    val TextPrimary: Color
        get() = DesignSystemBindings.activePalette.onSurface
    val TextSecondary: Color
        get() = DesignSystemBindings.activePalette.onSurfaceVariant
    val TextMuted: Color
        get() = DesignSystemBindings.activePalette.onSurfaceVariant.copy(alpha = 0.78f)

    val ActiveTabPillBg: Color
        get() = if (SplitMateTheme.isDark) DesignSystemBindings.activePalette.primary else DesignSystemBindings.activePalette.onSurface
    val ActiveTabPillText: Color
        get() = if (SplitMateTheme.isDark) DesignSystemBindings.activePalette.onPrimary else DesignSystemBindings.activePalette.surfaceContainerLowest
    val InactiveTabPillBg: Color
        get() = DesignSystemBindings.activePalette.surfaceContainer
    val InactiveTabPillText: Color
        get() = DesignSystemBindings.activePalette.onSurfaceVariant

    // Deep Forest Green Train Pass Tokens (Sunlight-Grade Contrast >= 5.8:1)
    val TrainForestTop: Color
        get() = HubExtendedColors.trainPass().forestTop
    val TrainForestBottom: Color
        get() = HubExtendedColors.trainPass().forestBottom
    val TrainOnPass: Color
        get() = HubExtendedColors.trainPass().onPass
    val TrainNextUpPillBg: Color
        get() = HubExtendedColors.trainPass().nextUpPillBg
    val TrainNextUpPillText: Color
        get() = HubExtendedColors.trainPass().nextUpPillText
    val TrainBerthCellBg: Color
        get() = HubExtendedColors.trainPass().berthCellBg
    val TrainBerthCellBorder: Color
        get() = HubExtendedColors.trainPass().berthCellBorder
    val TrainAccentLime: Color
        get() = HubExtendedColors.trainPass().accentLime
    val TrainSecondarySage: Color
        get() = HubExtendedColors.trainPass().secondarySage
    val TrainPrimaryCtaBg: Color
        get() = HubExtendedColors.trainPass().primaryCtaBg
    val TrainPrimaryCtaText: Color
        get() = HubExtendedColors.trainPass().primaryCtaText

    // Aviation Periwinkle Flight Pass Tokens
    val FlightNavyTop: Color
        get() = HubExtendedColors.flightPass().navyTop
    val FlightNavyBottom: Color
        get() = HubExtendedColors.flightPass().navyBottom
    val FlightOnPass: Color
        get() = HubExtendedColors.flightPass().onPass
    val FlightSecondaryLavender: Color
        get() = HubExtendedColors.flightPass().secondaryLavender
    val FlightAccentPeriwinkle: Color
        get() = HubExtendedColors.flightPass().accentPeriwinkle

    // Semantic Status & Category Badges
    val PositiveSageText: Color
        get() = DesignSystemBindings.activePalette.onPrimaryContainer
    val PositiveSagePillBg: Color
        get() = DesignSystemBindings.activePalette.primaryContainer
    val WarningRacText: Color
        get() = HubExtendedColors.warning().color
    val TerracottaPeachBg: Color
        get() = DesignSystemBindings.activePalette.secondaryContainer
    val TerracottaIconTint: Color
        get() = DesignSystemBindings.activePalette.onSecondaryContainer
    val PeriwinkleBoxBg: Color
        get() = DesignSystemBindings.activePalette.tertiaryContainer
    val PeriwinkleIconTint: Color
        get() = DesignSystemBindings.activePalette.onTertiaryContainer
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

    // v2.3.6: each time a trip is opened, its feeds may play their list entrance again.
    remember(resolvedGroupId) {
        com.splitmate.app.ui.components.ExpressiveEntranceRegistry.beginVisit(resolvedGroupId)
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
    val activePerspectiveMember: GroupMemberEntity? = remember(groupMembers, uiState.userPhone) {
        val normMyPhone = uiState.userPhone.filter { it.isDigit() }.takeLast(10)
        (if (normMyPhone.length == 10) {
            groupMembers.find { it.userPhone.filter { ch -> ch.isDigit() }.takeLast(10) == normMyPhone }
        } else null)
            ?: groupMembers.find { it.isCurrentUser }
            ?: groupMembers.firstOrNull()
    }
    val groupExpenses: List<ExpenseEntity> = remember(uiState.expenses, resolvedGroupId, context) {
        uiState.expenses
            .filter { it.groupId == resolvedGroupId }
            .sortedBy { resolveExpenseSchedule(context, it).effectiveEpochMs }
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
    // v2.3.6 P3 Money check: read-only audit from the exact rows the balances use.
    val groupSettlements = remember(uiState.settlements, resolvedGroupId) {
        uiState.settlements.filter { it.groupId == resolvedGroupId }
    }
    val moneyCheckReport = remember(groupMembers, groupExpenses, groupSplits, groupSettlements) {
        com.splitmate.app.MoneyCheck.run(groupMembers, groupExpenses, groupSplits, groupSettlements)
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

    LaunchedEffect(resolvedGroupId) {
        com.splitmate.app.data.GroupLedgerExtrasStore.load(context, resolvedGroupId)
        com.splitmate.app.ui.category.CustomExpenseCategoryStore.bindGroup(context, resolvedGroupId)
    }
    val groupExtras by com.splitmate.app.data.GroupLedgerExtrasStore.extrasByGroup.collectAsStateWithLifecycle()
    val groupCustomCategories = remember(groupExtras, resolvedGroupId) {
        groupExtras[resolvedGroupId]?.customCategories?.filter { !it.deleted }?.map { cloudCat ->
            com.splitmate.app.ui.category.ExpenseCategoryCatalog.custom(
                title = cloudCat.title,
                iconKey = cloudCat.iconKey,
                parentBucket = com.splitmate.app.ui.category.SpendBucket.fromNameOrNull(cloudCat.parentBucket),
                customId = cloudCat.id,
                createdByPhone = cloudCat.createdByPhone,
                isGroupShared = true
            )
        }.orEmpty()
    }
    val localCustomCategories by com.splitmate.app.ui.category.CustomExpenseCategoryStore.categories.collectAsStateWithLifecycle()
    val allCustomCategories = remember(localCustomCategories, groupCustomCategories) {
        (groupCustomCategories + localCustomCategories).distinctBy { it.customId ?: it.title.lowercase() }
    }

    // Proactively seed confirmed train/flight tickets into the local offline vault so opening a trip
    // or inspecting any ticket never triggers redundant live PNR / flight network calls.
    LaunchedEffect(groupExpenses, groupMembers.size) {
        if (groupExpenses.isNotEmpty()) {
            PnrNetworkRepository.seedConfirmedTicketsFromExpenses(
                context = context,
                expenses = groupExpenses,
                members = groupMembers,
                splits = groupSplits
            )
        }
    }

    // Classify every real expense in Room
    val classifiedExpenses = remember(groupExpenses, allCustomCategories) {
        groupExpenses.map { exp -> exp to classifyGroupExpenseForTripHub(exp, allCustomCategories) }
    }

    // v2.3.5 (#1): trip lifecycle (organizer wrap-up + automatic return-ticket arrival detection).
    val tripLifecycleRecord = groupExtras[resolvedGroupId]?.tripLifecycle
    
    var returnArrivalEpochMs by remember { mutableStateOf<Long?>(null) }
    var tripLastActivityMs by remember { mutableStateOf(0L) }
    var tripFirstActivityMs by remember { mutableStateOf(0L) }
    var tripLifecycleState by remember { mutableStateOf(com.splitmate.app.data.TripLifecycleResolver.State.ACTIVE) }

    LaunchedEffect(classifiedExpenses, resolvedGroupId, tripLifecycleRecord) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            while (true) {
                val currentEpochMs = System.currentTimeMillis()
                
                val travelLegs = classifiedExpenses.mapNotNull { (exp, cat) ->
                    if (cat != TripHubBookingCategory.TRAIN && cat != TripHubBookingCategory.FLIGHT) return@mapNotNull null
                    val sched = resolveExpenseSchedule(context, exp)
                    if (!sched.hasExplicitTicketDate) return@mapNotNull null
                    val parsed = extractTravelTicketFromTitle(exp.title)
                    val rawPnr = exp.travelPnr.trim().ifBlank { parsed?.pnr?.trim().orEmpty() }
                    val cleanPnr = if (rawPnr.isNotBlank()) PnrNetworkRepository.normalizePnrKey(rawPnr)
                        else PnrNetworkRepository.extractPnrFromFreeText(exp.title)
                    val snap = if (cleanPnr.length == 6 || cleanPnr.length == 10) loadPersistedPnrSnapshot(context, cleanPnr) else null
                    val flightRes = if (cleanPnr.length == 6) PnrNetworkRepository.loadConfirmedFlightTicketResult(context, cleanPnr) else null
                    val fromCode = flightRes?.originIata?.ifBlank { null }
                        ?: snap?.fromStation?.ifBlank { null }
                        ?: parsed?.fromStation.orEmpty()
                    val toCode = flightRes?.destinationIata?.ifBlank { null }
                        ?: snap?.toStation?.ifBlank { null }
                        ?: parsed?.toStation.orEmpty()
                    if (fromCode.isBlank() || toCode.isBlank()) return@mapNotNull null
                    com.splitmate.app.data.TripLifecycleResolver.TravelLegSchedule(
                        fromCode = fromCode,
                        toCode = toCode,
                        departureEpochMs = sched.effectiveEpochMs
                    )
                }
                val newReturnArrival = com.splitmate.app.data.TripLifecycleResolver.resolveReturnArrivalEpochMs(travelLegs)

                val newLastActivity = groupExpenses.maxOfOrNull { exp ->
                    val schedMs = resolveExpenseSchedule(context, exp).takeIf { it.hasExplicitTicketDate }?.effectiveEpochMs ?: 0L
                    maxOf(exp.createdAt, exp.scheduledAtEpochMs ?: 0L, schedMs)
                } ?: 0L

                val newFirstActivity = groupExpenses.minOfOrNull { exp ->
                    val schedMs = resolveExpenseSchedule(context, exp).takeIf { it.hasExplicitTicketDate }?.effectiveEpochMs
                    val candidate = exp.scheduledAtEpochMs?.takeIf { it > 0L } ?: schedMs
                    if (candidate != null && candidate > 0L) minOf(candidate, exp.createdAt) else exp.createdAt
                } ?: 0L

                val newState = com.splitmate.app.data.TripLifecycleResolver.resolve(
                    tripLifecycleRecord,
                    currentEpochMs,
                    newLastActivity,
                    newReturnArrival
                )

                returnArrivalEpochMs = newReturnArrival
                tripLastActivityMs = newLastActivity
                tripFirstActivityMs = newFirstActivity
                tripLifecycleState = newState

                kotlinx.coroutines.delay(15 * 60 * 1000L)
            }
        }
    }
    val isTripOrganizer = remember(resolvedGroupId, uiState.groups, uiState.members, uiState.userPhone) {
        runCatching { viewModel.isUserGroupOrganizer(resolvedGroupId, uiState) }.getOrDefault(false)
    }
    val tripEndedByName = remember(tripLifecycleRecord, groupMembers) {
        val phone = tripLifecycleRecord?.endedByPhone.orEmpty()
        if (phone.isBlank()) null else groupMembers.firstOrNull { it.userPhone == phone }?.name
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

    // Dynamic subtitle derived from actual travel ticket dates when available (fallback to logged dates)
    val dynamicTripSubtitle = remember(groupExpenses, groupMembers.size, isTravelGroup, context) {
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
            val schedules = groupExpenses.map { resolveExpenseSchedule(context, it) }
            val explicitTicketSchedules = schedules.filter { it.hasExplicitTicketDate }
            val pool = explicitTicketSchedules.ifEmpty { schedules }
            val minSchedule = pool.minByOrNull { it.effectiveEpochMs } ?: schedules.first()
            val maxSchedule = pool.maxByOrNull { it.effectiveEpochMs } ?: schedules.last()
            val startStr = minSchedule.shortDateLabel
            val endStr = maxSchedule.shortDateLabel
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

    val onlineFriendsCount = remember(uiState.memberPresenceByPhone, uiState.members, resolvedGroupId, uiState.userPhone) {
        uiState.onlineFriendsCount(resolvedGroupId)
    }

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

    var isFabMenuExpanded by remember { mutableStateOf(false) }
    // v2.3.4: Plan sub-view (Bookings | Explore | Loop) reported by TripPlanTabHost. The Add-Booking
    // FAB belongs to the Bookings sub-view only, so it hides on Explore / Loop (audit 5.9).
    var planSubView by remember(resolvedGroupId) { mutableStateOf(PlanSubView.BOOKINGS) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val openGroupDebtCents = remember(netBalancesMap) {
        netBalancesMap.values.filter { it > 0L }.sum()
    }
    val isAllSettled = remember(activeMemberNetCents, openGroupDebtCents) {
        activeMemberNetCents == 0L && openGroupDebtCents == 0L
    }

    Scaffold(
        containerColor = TripHubTokens.CanvasBg,
        topBar = {
            // =================================================================
            // SUBTASK 3.1.1 & 3.1.2: COLLAPSING LARGE TOP APP BAR + 5 SECTION PILLS
            // =================================================================
            TripHubTopBar(
                groupName = (group?.name ?: "Trip Hub").toSmartTitleCase(),
                // Not shared: a sharedBounds title inside LargeFlexibleTopAppBar loops its collapse
                // measurement (Compose never idles). Only the balance figure flies across.
                sharedTitleKey = null,
                subtitle = if (tripLifecycleState == com.splitmate.app.data.TripLifecycleResolver.State.ENDED) "Ended · $dynamicTripSubtitle" else dynamicTripSubtitle,
                activePerspectiveMember = activePerspectiveMember,
                onlineFriendsCount = onlineFriendsCount,
                isCloudSyncing = uiState.isCloudSyncing,
                moneyNeedsAttention = !moneyCheckReport.passed,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onToggleSearch = {
                    performCrispTactileHaptic(context, localView, heavy = false)
                    isSearchExpanded = !isSearchExpanded
                    if (!isSearchExpanded) searchQuery = ""
                },
                onManualSyncClick = {
                    performCrispTactileHaptic(context, localView, heavy = false)
                    viewModel.syncActiveGroupNow(context = context, groupId = resolvedGroupId, silent = false)
                },
                onCycleThemeClick = {
                    performCrispTactileHaptic(context, localView, heavy = false)
                    viewModel.cycleExpressiveThemeMode(context)
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
                },
                selectedSectionTab = selectedSectionTab,
                onSelectSectionTab = { newTab ->
                    performCrispTactileHaptic(context, localView, heavy = false)
                    selectedSectionTab = newTab
                    viewModel.syncActiveGroupNow(context = context, groupId = resolvedGroupId, silent = true)
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            val planHidesFab = selectedSectionTab == TripHubSectionTab.PLAN && planSubView != PlanSubView.BOOKINGS
            if (selectedSectionTab != TripHubSectionTab.PEOPLE && selectedSectionTab != TripHubSectionTab.MONEY && !planHidesFab) {
                FloatingActionButtonMenu(
                    expanded = isFabMenuExpanded,
                    onToggle = {
                        performCrispTactileHaptic(context, localView, heavy = false)
                        isFabMenuExpanded = !isFabMenuExpanded
                    },
                    toggleIcon = Icons.Rounded.Add,
                    toggleLabel = "Add booking",
                    items = listOf(
                        ExpressiveFabMenuItem(
                            label = "Train ticket",
                            icon = Icons.Rounded.Train,
                            subtitle = "Add with your PNR",
                            onClick = {
                                performCrispTactileHaptic(context, localView, heavy = false)
                                onOpenTrainPnrReviewClick("")
                            }
                        ),
                        ExpressiveFabMenuItem(
                            label = "Flight",
                            icon = Icons.Rounded.FlightTakeoff,
                            subtitle = "Upload your boarding pass",
                            onClick = {
                                performCrispTactileHaptic(context, localView, heavy = false)
                                onOpenFlightReviewClick("")
                            }
                        ),
                        ExpressiveFabMenuItem(
                            label = "Split a bill",
                            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                            subtitle = "Equal, exact or by shares",
                            onClick = {
                                performCrispTactileHaptic(context, localView, heavy = false)
                                onLogQuickExpenseClick()
                            }
                        ),
                        ExpressiveFabMenuItem(
                            label = "More",
                            icon = Icons.Rounded.ViewAgenda,
                            subtitle = "Stay, rental or cab",
                            onClick = {
                                performCrispTactileHaptic(context, localView, heavy = false)
                                showAddBookingBottomSheet = true
                            }
                        )
                    )
                )
            }
        },
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        // v2.3.6: chips, the hero spend card and lifecycle/declined banners live in a collapsing
        // header. It slides away as soon as you scroll into the bookings and returns when you
        // scroll back to the top, so tickets get the full height. Section tabs stay pinned.
        val collapsingHeaderState = remember(resolvedGroupId) { TripHubCollapsingHeaderState() }
        val headerResetSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
        LaunchedEffect(selectedSectionTab) {
            collapsingHeaderState.expand(headerResetSpec)
        }
        // v2.3.6 Step C1: real M3 Expressive pull-to-refresh (morphing LoadingIndicator) that runs a
        // cloud sync of this trip. It only engages once the app bar and the collapsing header are
        // fully expanded, so pulling down first restores them. The indicator is shown only while
        // the user-requested sync is in flight (min 400ms), never for cached/offline data.
        val pullToRefreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
        var isPullRefreshRequested by remember(resolvedGroupId) { mutableStateOf(false) }
        LaunchedEffect(isPullRefreshRequested, uiState.isCloudSyncing) {
            if (isPullRefreshRequested && !uiState.isCloudSyncing) {
                kotlinx.coroutines.delay(com.splitmate.app.ui.components.WavyProgressIndicatorDefaults.MIN_VISIBLE_MILLIS)
                if (!viewModel.uiState.value.isCloudSyncing) isPullRefreshRequested = false
            }
        }
        val isPullToRefreshEngaged = scrollBehavior.state.collapsedFraction == 0f &&
            collapsingHeaderState.offsetPx == 0f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TripHubTokens.CanvasBg)
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .pullToRefresh(
                    isRefreshing = isPullRefreshRequested,
                    state = pullToRefreshState,
                    enabled = isPullToRefreshEngaged || isPullRefreshRequested,
                    onRefresh = {
                        performCrispTactileHaptic(context, localView, heavy = false)
                        isPullRefreshRequested = true
                        viewModel.syncActiveGroupNow(context = context, groupId = resolvedGroupId, silent = true)
                    }
                )
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(collapsingHeaderState.nestedScrollConnection)
        ) {
            TripHubCollapsingHeader(state = collapsingHeaderState) {

            // =================================================================
            // SUBTASK 3.1.3: COMPACT PERSPECTIVE NET BALANCE STRIP
            // =================================================================
            if (!TripHubLayoutRules.showsSpendCard(selectedSectionTab)) {
                // v2.4.0 Trip Hub step 1: money stays on Money; elsewhere a small balance chip opens it.
                TripHubBalanceChip(
                    activeMemberNetCents = activeMemberNetCents,
                    onClick = {
                        performCrispTactileHaptic(context, localView, heavy = false)
                        selectedSectionTab = TripHubSectionTab.MONEY
                    }
                )
            } else
            CompactPerspectiveNetBalanceStrip(
                sharedNetKey = SharedGroupKeys.net(groupId),
                totalGroupSpendCents = totalGroupSpendCents,
                activeMemberNetCents = activeMemberNetCents,
                activeMemberName = activePerspectiveMember?.name ?: "You",
                isAllSettled = isAllSettled,
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

            // v2.3.5 (#1): wrap-up summary / organizer nudge (Overview + Money only).
            if (selectedSectionTab == TripHubSectionTab.OVERVIEW || selectedSectionTab == TripHubSectionTab.MONEY) {
                TripLifecycleCard(
                    state = tripLifecycleState,
                    record = tripLifecycleRecord,
                    endedByName = tripEndedByName,
                    isOrganizer = isTripOrganizer,
                    stats = TripWrapUpStats(
                        totalSpentCents = totalGroupSpendCents,
                        memberCount = groupMembers.size,
                        days = com.splitmate.app.data.TripLifecycleResolver.tripDays(tripFirstActivityMs, tripLastActivityMs),
                        topCategories = computeTripTopCategories(classifiedExpenses),
                        isAllSettled = isAllSettled
                    ),
                    onWrapUp = {
                        performCrispTactileHaptic(context, localView, heavy = true)
                        viewModel.wrapUpTrip(resolvedGroupId)
                    },
                    onReopen = {
                        performCrispTactileHaptic(context, localView, heavy = false)
                        viewModel.reopenTrip(resolvedGroupId)
                    },
                    onSettleUp = {
                        performCrispTactileHaptic(context, localView, heavy = false)
                        if (selectedSectionTab != TripHubSectionTab.MONEY) selectedSectionTab = TripHubSectionTab.MONEY else onOpenSettleUpClick()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    isReturnJourneySuggestion = com.splitmate.app.data.TripLifecycleResolver.isReturnJourneySuggestion(
                        tripLifecycleRecord,
                        System.currentTimeMillis(),
                        returnArrivalEpochMs
                    ),
                    onDismissSuggestion = {
                        viewModel.reopenTrip(resolvedGroupId)
                    }
                )
            }

            // =================================================================
            // DECLINED MEMBER SHARE REASSIGNMENT BANNER (0.00c DRIFT)
            // =================================================================
            val declinedMembersWithOpenSplits = remember(groupMembers, groupSplits) {
                groupMembers.filter { m ->
                    m.inviteStatus.equals("DECLINED", ignoreCase = true) &&
                        groupSplits.any { sp -> sp.finalOwedCents > 0L && sp.memberId == m.memberId }
                }
            }
            declinedMembersWithOpenSplits.forEach { declinedMember ->
                val declinedShareCents = groupSplits
                    .filter { it.memberId == declinedMember.memberId && it.finalOwedCents > 0L }
                    .sumOf { it.finalOwedCents }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignSystemBindings.activePalette.secondaryContainer,
                    border = BorderStroke(1.dp, DesignSystemBindings.activePalette.secondary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonRemove,
                                contentDescription = null,
                                tint = DesignSystemBindings.activePalette.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${declinedMember.name} declined this group invite (${formatIndianRupeesFromCents(declinedShareCents, includePlusSign = false)} unassigned)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                color = DesignSystemBindings.activePalette.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = DesignSystemBindings.activePalette.secondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    performCrispTactileHaptic(context, localView, heavy = true)
                                    viewModel.reassignDeclinedMemberSharesEqually(
                                        context = context,
                                        groupId = resolvedGroupId,
                                        declinedMemberId = declinedMember.memberId
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapHoriz,
                                    contentDescription = null,
                                    tint = DesignSystemBindings.activePalette.onSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reassign ${declinedMember.name}'s Share Equally",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DesignSystemBindings.activePalette.onSecondary
                                )
                            }
                        }
                    }
                }
            }

            } // TripHubCollapsingHeader

            // =================================================================
            // SECTION BODY CONTENT (`Overview`, `Plan`, `Travel`, `Money`, `People`)
            // =================================================================
            // v2.3.5 (#4): cards open ActivityDetailSheet -> EditLoggedExpenseDialog via this host.
            TripHubExpenseManagementHost(
                viewModel = viewModel,
                uiState = uiState,
                onOpenTrainPnrReviewClick = onOpenTrainPnrReviewClick,
                onOpenFlightReviewClick = onOpenFlightReviewClick
            ) {
            // v2.3.6 Step B: fade-through between sections on the theme's effects springs
            // (MaterialTheme.motionScheme; snaps when system animations are off).
            val sectionEnterSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            val sectionExitSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
            androidx.compose.animation.AnimatedContent(
                targetState = selectedSectionTab,
                transitionSpec = {
                    (androidx.compose.animation.fadeIn(sectionEnterSpec) +
                        androidx.compose.animation.scaleIn(sectionEnterSpec, initialScale = 0.98f)) togetherWith
                        androidx.compose.animation.fadeOut(sectionExitSpec)
                },
                label = "TripHubSectionFadeThrough"
            ) { sectionTab ->
            when (sectionTab) {
                TripHubSectionTab.OVERVIEW -> {
                    val overviewNowMs = remember(classifiedExpenses) { System.currentTimeMillis() }
                    val overviewItems = remember(classifiedExpenses, overviewNowMs, tripLifecycleState) {
                        val scheduleOf: (ExpenseEntity) -> Long = { resolveExpenseSchedule(context, it).effectiveEpochMs }
                        // v2.4.0 Trip Hub step 3: lead with what matters for where the trip is.
                        val phase = TripHubLayoutRules.phaseOf(
                            isEnded = tripLifecycleState == com.splitmate.app.data.TripLifecycleResolver.State.ENDED,
                            scheduledMs = classifiedExpenses.map { scheduleOf(it.first) },
                            nowMs = overviewNowMs
                        )
                        TripHubLayoutRules.overviewItems(
                            all = classifiedExpenses,
                            scheduleMsOf = scheduleOf,
                            nowMs = overviewNowMs,
                            phase = phase
                        )
                    }
                    TripHubOverviewFeed(
                        filteredClassifiedExpenses = overviewItems,
                        totalExpenseCount = classifiedExpenses.size,
                        onSeeAllExpensesClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            selectedSectionTab = TripHubSectionTab.MONEY
                        },
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
                    // v2.4.0 Trip Hub step 2: Days | Explore. Tickets sit on their day (signature ticket
                    // card); Loop opens from a "Make a loop" button on each day.
                    TripPlanTabHost(
                        groupId = resolvedGroupId,
                        bookingsContent = { onMakeLoop ->
                            TripHubPlanTimelineView(
                                classifiedExpenses = classifiedExpenses,
                                groupMembers = groupMembers,
                                onOpenTrainPnrReviewClick = onOpenTrainPnrReviewClick,
                                onOpenFlightReviewClick = onOpenFlightReviewClick,
                                onLogQuickExpenseClick = onLogQuickExpenseClick,
                                allSplits = groupSplits,
                                activePerspectiveMember = activePerspectiveMember,
                                onInspectBerthChart = { exp, snap -> inspectedBerthChartSnapshot = exp to snap },
                                onDeleteExpense = { expenseId -> viewModel.rollbackExpense(expenseId) },
                                onMakeLoop = onMakeLoop
                            )
                        },
                        onSubViewChanged = { planSubView = it }
                    )
                }

                TripHubSectionTab.MONEY -> {
                    TripHubMoneySettlementView(
                        viewModel = viewModel,
                        groupId = resolvedGroupId,
                        groupName = group?.name ?: "Trip Hub",
                        groupMembers = groupMembers,
                        netBalancesMap = netBalancesMap,
                        moneyCheckReport = moneyCheckReport,
                        onOpenSettleUpClick = onOpenSettleUpClick,
                        // v2.4.0 Trip Hub step 1: the one full expense list lives here.
                        allExpensesSection = {
                            if (classifiedExpenses.isNotEmpty()) {
                                item(key = "money_all_expenses_header") {
                                    Text(
                                        text = "All expenses (${classifiedExpenses.size})",
                                        style = MaterialTheme.typography.titleMediumEmphasized,
                                        color = TripHubTokens.TextPrimary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 20.dp, bottom = 4.dp)
                                            .semantics { heading() }
                                    )
                                }
                                val moneyLegNumbers = classifiedExpenses
                                    .filter { it.second == TripHubBookingCategory.TRAIN }
                                    .mapIndexed { idx, (exp, _) -> exp.expenseId to (idx + 1) }
                                    .toMap()
                                items(items = classifiedExpenses, key = { "money_exp_" + it.first.expenseId }) { (expense, category) ->
                                    TripHubFeedCard(
                                        expense = expense,
                                        category = category,
                                        legNumber = moneyLegNumbers[expense.expenseId] ?: 1,
                                        groupMembers = groupMembers,
                                        allSplits = groupSplits,
                                        activePerspectiveMember = activePerspectiveMember,
                                        onOpenTrainPnrReviewClick = onOpenTrainPnrReviewClick,
                                        onOpenFlightReviewClick = onOpenFlightReviewClick,
                                        onInspectBerthChart = { exp, snap -> inspectedBerthChartSnapshot = exp to snap },
                                        onDeleteExpense = { expenseId -> viewModel.rollbackExpense(expenseId) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null)
                                    )
                                }
                            }
                        }
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
                        onOpenSettleUpClick = onOpenSettleUpClick,
                        onLeaveGroupSuccess = onBackClick,
                        isTravelGroup = isTravelGroup
                    )
                }
            }
            }
            }
        }
            androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator(
                state = pullToRefreshState,
                isRefreshing = isPullRefreshRequested,
                modifier = Modifier.align(Alignment.TopCenter)
            )
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
        val berthTicketTransform = com.splitmate.app.ui.components.LocalTicketContainerTransform.current
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
                // The card is still behind the dialog, so the ticket can morph out of it.
                if (resolvedPnr.isNotBlank()) berthTicketTransform?.controller?.markSource(exp.expenseId)
                onOpenTrainPnrReviewClick(resolvedPnr)
            }
        )
    }
}


// ==============================================================================
// v2.3.6: COLLAPSING TRIP HUB HEADER (chips + hero + banners scroll away)
// ==============================================================================

/** Collapsing header offset: 0 = fully shown, -[heightPx] = fully hidden. */
@androidx.compose.runtime.Stable
internal class TripHubCollapsingHeaderState {
    var heightPx by mutableFloatStateOf(0f)
        internal set
    var offsetPx by mutableFloatStateOf(0f)
        private set

    /** Hides the header before the list scrolls; shows it again only once the list is back at its top. */
    val nestedScrollConnection = object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
        override fun onPreScroll(
            available: androidx.compose.ui.geometry.Offset,
            source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
        ): androidx.compose.ui.geometry.Offset =
            if (available.y < 0f) androidx.compose.ui.geometry.Offset(0f, consume(available.y))
            else androidx.compose.ui.geometry.Offset.Zero

        override fun onPostScroll(
            consumed: androidx.compose.ui.geometry.Offset,
            available: androidx.compose.ui.geometry.Offset,
            source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
        ): androidx.compose.ui.geometry.Offset =
            if (available.y > 0f) androidx.compose.ui.geometry.Offset(0f, consume(available.y))
            else androidx.compose.ui.geometry.Offset.Zero
    }

    /** Applies [dy] within bounds and returns the amount consumed. */
    internal fun consume(dy: Float): Float {
        val old = offsetPx
        offsetPx = (old + dy).coerceIn(-heightPx, 0f)
        return offsetPx - old
    }

    /** Springs the header back into view (theme motion; snaps under reduced motion). */
    suspend fun expand(spec: androidx.compose.animation.core.AnimationSpec<Float>) {
        if (offsetPx == 0f) return
        androidx.compose.animation.core.animate(offsetPx, 0f, animationSpec = spec) { value, _ ->
            offsetPx = value
        }
    }
}

@Composable
internal fun TripHubCollapsingHeader(
    state: TripHubCollapsingHeaderState,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = { Column(modifier = Modifier.fillMaxWidth(), content = content) },
        modifier = Modifier.clipToBounds()
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(
            constraints.copy(minHeight = 0, maxHeight = androidx.compose.ui.unit.Constraints.Infinity)
        )
        state.heightPx = placeable.height.toFloat()
        val offset = state.offsetPx.coerceIn(-placeable.height.toFloat(), 0f).roundToInt()
        layout(placeable.width, (placeable.height + offset).coerceAtLeast(0)) {
            placeable.placeRelative(0, offset)
        }
    }
}

// ==============================================================================
// 4. TASK 3.1 COMPONENTS: TOP BAR, 5 PILL TABS, DYNAMIC CHIPS & BALANCE STRIP
// ==============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripHubTopBar(
    groupName: String,
    sharedTitleKey: String? = null,
    subtitle: String,
    @Suppress("UNUSED_PARAMETER") activePerspectiveMember: GroupMemberEntity?,
    onlineFriendsCount: Int = 0,
    isCloudSyncing: Boolean = false,
    /** v2.3.6 P3: a Money check fails, so the Money tab shows an error dot. */
    moneyNeedsAttention: Boolean = false,
    isSearchExpanded: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onManualSyncClick: () -> Unit = {},
    onCycleThemeClick: () -> Unit = {},
    onBackClick: () -> Unit,
    onSwitchToClassicLedgerClick: () -> Unit,
    onOpenSyncAndPerspectiveSheet: () -> Unit,
    selectedSectionTab: TripHubSectionTab,
    onSelectSectionTab: (TripHubSectionTab) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val context = LocalContext.current
    val localView = LocalView.current
    val collapsedFraction = scrollBehavior.state.collapsedFraction

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TripHubTokens.CanvasBg)
    ) {
        // v2.3.6 Wave 4: M3 Expressive LargeFlexibleTopAppBar (title + subtitle slots; theme type
        // roles scale between expanded headline and collapsed title). Max 3 visible actions; the
        // rest live in an overflow menu per M3 app-bar guidance.
        var showHeaderOverflow by remember { mutableStateOf(false) }
        androidx.compose.material3.LargeFlexibleTopAppBar(
            navigationIcon = {
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
            },
            title = {
                Text(
                    text = groupName,
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = TripHubTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (sharedTitleKey != null) Modifier.sharedGroupElement(sharedTitleKey) else Modifier
                )
            },
            subtitle = {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.merge(
                        TextStyle(
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontFeatureSettings = "tnum"
                        )
                    ),
                    color = TripHubTokens.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            actions = {
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
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (onlineFriendsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(HubExtendedColors.presence().color)
                                )
                                Text(
                                    text = "$onlineFriendsCount Online",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TripHubTokens.TextPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.PersonAdd,
                                    contentDescription = "Invite Friends via Link",
                                    tint = TripHubTokens.TextPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Invite",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TripHubTokens.TextPrimary
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onManualSyncClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .defaultMinSize(minWidth = 44.dp, minHeight = 48.dp)
                ) {
                    if (isCloudSyncing) {
                        ContainedLoadingIndicator(
                            containerSize = 24.dp,
                            containerColor = TripHubTokens.PositiveSagePillBg,
                            indicatorColor = TripHubTokens.PositiveSageText
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = "Sync Trip Expenses Now",
                            tint = TripHubTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .defaultMinSize(minWidth = 44.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = "Search Bookings and Expenses",
                        tint = TripHubTokens.TextPrimary,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showHeaderOverflow = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .defaultMinSize(minWidth = 44.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More trip options",
                            tint = TripHubTokens.TextPrimary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showHeaderOverflow,
                        onDismissRequest = { showHeaderOverflow = false },
                        shape = MaterialTheme.shapes.large,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Change theme", fontFamily = FigtreeFontFamily, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = "Cycle Expressive Theme Mode",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showHeaderOverflow = false
                                onCycleThemeClick()
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Classic ledger view", fontFamily = FigtreeFontFamily, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.ViewAgenda,
                                    contentDescription = "Switch ticket view",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showHeaderOverflow = false
                                onSwitchToClassicLedgerClick()
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.largeTopAppBarColors(
                containerColor = TripHubTokens.CanvasBg,
                scrolledContainerColor = TripHubTokens.CardSurface,
                navigationIconContentColor = TripHubTokens.TextPrimary,
                titleContentColor = TripHubTokens.TextPrimary,
                actionIconContentColor = TripHubTokens.TextPrimary
            ),
            scrollBehavior = scrollBehavior
        )

        TripHubSectionTabsRow(
            selectedTab = selectedSectionTab,
            onSelectTab = onSelectSectionTab,
            attentionTab = if (moneyNeedsAttention) TripHubSectionTab.MONEY else null
        )

        AnimatedVisibility(visible = isSearchExpanded) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search PNR, train, flight, hotel, or payer...",
                        style = MaterialTheme.typography.bodyMedium,
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
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripHubSectionTabsRow(
    selectedTab: TripHubSectionTab,
    onSelectTab: (TripHubSectionTab) -> Unit,
    /** v2.3.6 P3: tab that gets an error dot (Money while a Money check fails). */
    attentionTab: TripHubSectionTab? = null
) {
    val tabs = remember { TripHubSectionTab.entries.toList() }
    val selectedIdx = tabs.indexOf(selectedTab).coerceAtLeast(0)
    // v2.3.6 Step B: M3 primary tabs (icon + label). Section switching inside one screen is the
    // Tabs pattern; the indicator slides between tabs on the theme's spatial spring.
    androidx.compose.material3.PrimaryTabRow(
        selectedTabIndex = selectedIdx,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) },
        modifier = Modifier.fillMaxWidth()
    ) {
        tabs.forEachIndexed { idx, tab ->
            val selected = idx == selectedIdx
            val needsAttention = tab == attentionTab
            androidx.compose.material3.Tab(
                selected = selected,
                onClick = { onSelectTab(tab) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .heightIn(min = 64.dp)
                    .semantics { if (needsAttention) stateDescription = "Something doesn't add up" }
            ) {
                androidx.compose.material3.BadgedBox(
                    badge = {
                        if (needsAttention) {
                            androidx.compose.material3.Badge(containerColor = MaterialTheme.colorScheme.error)
                        }
                    }
                ) {
                    Icon(imageVector = tripHubSectionIcon(tab, selected), contentDescription = null)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tab.title,
                    style = if (selected) MaterialTheme.typography.titleSmallEmphasized else MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}

private fun tripHubSectionIcon(tab: TripHubSectionTab, selected: Boolean): ImageVector = when (tab) {
    TripHubSectionTab.OVERVIEW -> if (selected) Icons.Rounded.SpaceDashboard else Icons.Outlined.SpaceDashboard
    TripHubSectionTab.PLAN -> if (selected) Icons.Rounded.Map else Icons.Outlined.Map
    TripHubSectionTab.MONEY -> if (selected) Icons.Rounded.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet
    TripHubSectionTab.PEOPLE -> if (selected) Icons.Rounded.Group else Icons.Outlined.Group
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

/**
 * v2.4.0 Trip Hub step 1: compact balance chip shown on every tab except Money. Icon + words (never
 * colour alone), 48dp target, one spoken sentence, opens Money.
 */
@Composable
private fun TripHubBalanceChip(
    activeMemberNetCents: Long,
    onClick: () -> Unit
) {
    val formattedAbs = formatIndianRupeesFromCents(cents = abs(activeMemberNetCents), includePlusSign = false, currencySymbol = "₹")
    val label = TripHubLayoutRules.balanceChipLabel(activeMemberNetCents, formattedAbs)
    val (container, content) = when {
        activeMemberNetCents > 0L -> TripHubTokens.PositiveSagePillBg to TripHubTokens.PositiveSageText
        activeMemberNetCents < 0L -> TripHubTokens.TerracottaPeachBg to TripHubTokens.TerracottaIconTint
        else -> TripHubTokens.SunkenWell to TripHubTokens.TextSecondary
    }
    val spoken = when {
        activeMemberNetCents > 0L -> "You get back ${com.splitmate.app.ui.a11y.SpokenMoney.rupees(activeMemberNetCents)}"
        activeMemberNetCents < 0L -> "You owe ${com.splitmate.app.ui.a11y.SpokenMoney.rupees(activeMemberNetCents)}"
        else -> "All settled"
    }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = container,
            contentColor = content,
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .clearAndSetSemantics {
                    role = androidx.compose.ui.semantics.Role.Button
                    contentDescription = "$spoken. Opens Money."
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = when {
                        activeMemberNetCents > 0L -> Icons.Rounded.ArrowDownward
                        activeMemberNetCents < 0L -> Icons.Rounded.ArrowUpward
                        else -> Icons.Rounded.CheckCircle
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.merge(TextStyle(fontFeatureSettings = "tnum")),
                    fontWeight = FontWeight.Bold
                )
                Icon(imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun CompactPerspectiveNetBalanceStrip(
    totalGroupSpendCents: Long,
    sharedNetKey: String? = null,
    activeMemberNetCents: Long,
    activeMemberName: String,
    isAllSettled: Boolean = false,
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
        activeMemberNetCents > 0L -> "You get back $formattedAbsNet"
        activeMemberNetCents < 0L -> "You owe $formattedAbsNet"
        else -> "All settled"
    }
    val netBadgeBg = when {
        activeMemberNetCents > 0L -> TripHubTokens.PositiveSagePillBg
        activeMemberNetCents < 0L -> TripHubTokens.TerracottaPeachBg
        else -> TripHubTokens.SunkenWell
    }
    val netBadgeTextColor = when {
        activeMemberNetCents > 0L -> TripHubTokens.PositiveSageText
        activeMemberNetCents < 0L -> TripHubTokens.TerracottaIconTint
        else -> TripHubTokens.TextSecondary
    }
    // v2.3.2 M3E audit: static financial data (spend/net) carries no wavy/linear progress
    // indicators. Hierarchy is expressed through an editorial headlineLarge figure + spacing.
    // v2.3.6 Step B: hero card uses the 28dp extraLarge corner (top of the corner hierarchy).
    // The total rolls to its new value on the theme's slow effects spring when spend changes
    // (snaps under reduced motion); the final frame always shows the exact cents.
    val spendRoll = remember { androidx.compose.animation.core.Animatable(totalGroupSpendCents.toFloat()) }
    val rollSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
    LaunchedEffect(totalGroupSpendCents) {
        spendRoll.animateTo(totalGroupSpendCents.toFloat(), rollSpec)
    }
    val displayedSpendCents = if (spendRoll.isRunning) spendRoll.value.roundToLong() else totalGroupSpendCents
    val displayedTotalSpend = formatIndianRupeesFromCents(
        cents = displayedSpendCents,
        includePlusSign = false,
        currencySymbol = "₹"
    )
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = TripHubTokens.CardSurface,
        border = BorderStroke(1.dp, TripHubTokens.CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isTravelGroup) "Trip spend" else "Group spend",
                        style = MaterialTheme.typography.labelLarge,
                        color = TripHubTokens.TextSecondary
                    )
                    // Editorial hero total: headlineLarge, auto-shrinks so it never overflows the Settle up column.
                    EditorialFinancialTotalText(
                        text = displayedTotalSpend,
                        style = MaterialTheme.typography.displaySmallEmphasized,
                        fontFamily = SplitMateTnumMonospace,
                        fontWeight = FontWeight.Black,
                        color = TripHubTokens.TextPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(modifier = Modifier.width(118.dp)) {
                    ButtonGroup(
                        items = listOf(
                            ExpressiveActionItem(
                                label = "Settle up",
                                icon = Icons.Rounded.SwapHoriz,
                                onClick = onSettleUpClick,
                                isPrimary = true,
                                containerColor = TripHubTokens.ActiveTabPillBg,
                                contentColor = TripHubTokens.ActiveTabPillText
                            )
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isAllSettled) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "All Settled",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Settled",
                                style = MaterialTheme.typography.labelMediumEmphasized,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = netBadgeBg,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .then(if (sharedNetKey != null) Modifier.sharedGroupElement(sharedNetKey) else Modifier)
                ) {
                    Text(
                        text = "$activeMemberName · $netBadgeText",
                        style = MaterialTheme.typography.labelLarge.merge(com.splitmate.app.ui.SplitMateMonospaceTextStyle),
                        color = netBadgeTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
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
    onDeleteExpense: (String) -> Unit,
    /** v2.4.0: total number of expenses in the trip (Overview shows only the next ticket + recent). */
    totalExpenseCount: Int = filteredClassifiedExpenses.size,
    /** v2.4.0: opens the one full expense list on Money. */
    onSeeAllExpensesClick: (() -> Unit)? = null
) {
    if (filteredClassifiedExpenses.isEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "empty_trip_hub_state") {
                EmptyTripHubStateCard(
                    onLogTrainPnrClick = { onOpenTrainPnrReviewClick("") },
                    onUploadFlightPdfClick = { onOpenFlightReviewClick("") },
                    onLogSharedExpenseClick = onLogQuickExpenseClick,
                    modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null)
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

    // v2.3.6 Step C: entrance stagger for the top cards (once per trip visit).
    val feedEntranceStart = com.splitmate.app.ui.components.rememberFirstOpenEntrance(
        "trip-overview:" + (filteredClassifiedExpenses.firstOrNull()?.first?.groupId ?: "")
    )
    val entranceIndexById = remember(filteredClassifiedExpenses) {
        filteredClassifiedExpenses.mapIndexed { idx, (exp, _) -> exp.expenseId to idx }.toMap()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = filteredClassifiedExpenses,
            key = { it.first.expenseId }
        ) { (expense, category) ->
            TripHubFeedCard(
                expense = expense,
                category = category,
                legNumber = trainLegNumberById[expense.expenseId] ?: 1,
                groupMembers = groupMembers,
                allSplits = allSplits,
                activePerspectiveMember = activePerspectiveMember,
                onOpenTrainPnrReviewClick = onOpenTrainPnrReviewClick,
                onOpenFlightReviewClick = onOpenFlightReviewClick,
                onInspectBerthChart = onInspectBerthChart,
                onDeleteExpense = onDeleteExpense,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null)
                    .staggeredEntrance(entranceIndexById[expense.expenseId] ?: Int.MAX_VALUE, feedEntranceStart)
                    .expressivePressScale()
            )
        }
        if (onSeeAllExpensesClick != null && totalExpenseCount > filteredClassifiedExpenses.size) {
            item(key = "overview_see_all_expenses") {
                // v2.4.0: the full list lives on Money (one home for each thing).
                androidx.compose.material3.OutlinedButton(
                    onClick = onSeeAllExpensesClick,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null)
                ) {
                    Text(text = "See all $totalExpenseCount expenses", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/**
 * v2.4.0 Trip Hub step 1: one expense card renderer, shared by Overview (next ticket + recent) and
 * the single full expense list on Money.
 */
@Composable
private fun TripHubFeedCard(
    expense: ExpenseEntity,
    category: TripHubBookingCategory,
    legNumber: Int,
    groupMembers: List<GroupMemberEntity>,
    allSplits: List<ExpenseSplitEntity>,
    activePerspectiveMember: GroupMemberEntity?,
    onOpenTrainPnrReviewClick: (String) -> Unit,
    onOpenFlightReviewClick: (String) -> Unit,
    onInspectBerthChart: (ExpenseEntity, LivePnrStatusSnapshot?) -> Unit,
    onDeleteExpense: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemModifier = modifier
    val ticketTransform = com.splitmate.app.ui.components.LocalTicketContainerTransform.current
    when (category) {
        TripHubBookingCategory.TRAIN -> {
            // legNumber is passed in
            // v2.3.5 (#4): Train / Flight cards also get Edit / Delete.
            Column(modifier = itemModifier) {
                TripHubSwipeToManage(
                    expense = expense,
                    onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                com.splitmate.app.ui.components.TicketContainerSource(
                    sourceKey = expense.expenseId,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DeepGreenTrainTicketCard(
                        expense = expense,
                        legNumber = legNumber,
                        groupMembers = groupMembers,
                        allSplits = allSplits,
                        activePerspectiveMember = activePerspectiveMember,
                        onOpenTrainPnrReviewClick = { pnr ->
                            if (pnr.isNotBlank()) ticketTransform?.controller?.markSource(expense.expenseId)
                            onOpenTrainPnrReviewClick(pnr)
                        },
                        onInspectBerthChart = onInspectBerthChart,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                }
                TripHubManageExpenseRow(
                    expense = expense,
                    onDeleteExpense = { onDeleteExpense(expense.expenseId) }
                )
            }
        }

        TripHubBookingCategory.FLIGHT -> {
            Column(modifier = itemModifier) {
                TripHubSwipeToManage(
                    expense = expense,
                    onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                com.splitmate.app.ui.components.TicketContainerSource(
                    sourceKey = expense.expenseId,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PeriwinkleFlightBookingCard(
                        expense = expense,
                        groupMembers = groupMembers,
                        allSplits = allSplits,
                        activePerspectiveMember = activePerspectiveMember,
                        onOpenFlightReviewClick = { key ->
                            if (key.isNotBlank()) ticketTransform?.controller?.markSource(expense.expenseId)
                            onOpenFlightReviewClick(key)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                }
                TripHubManageExpenseRow(
                    expense = expense,
                    onDeleteExpense = { onDeleteExpense(expense.expenseId) }
                )
            }
        }

        TripHubBookingCategory.STAY -> {
            TripHubSwipeToManage(
                expense = expense,
                onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                modifier = itemModifier
            ) {
                LodgingBookingCard(
                    expense = expense,
                    groupMembers = groupMembers,
                    allSplits = allSplits,
                    onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        TripHubBookingCategory.RENTAL -> {
            TripHubSwipeToManage(
                expense = expense,
                onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                modifier = itemModifier
            ) {
                GroundMobilityBookingCard(
                    expense = expense,
                    isTwoWheelerRental = true,
                    groupMembers = groupMembers,
                    allSplits = allSplits,
                    onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        TripHubBookingCategory.CAB -> {
            TripHubSwipeToManage(
                expense = expense,
                onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                modifier = itemModifier
            ) {
                GroundMobilityBookingCard(
                    expense = expense,
                    isTwoWheelerRental = false,
                    groupMembers = groupMembers,
                    allSplits = allSplits,
                    onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        TripHubBookingCategory.FOOD,
        TripHubBookingCategory.ACTIVITIES,
        TripHubBookingCategory.SHOPPING,
        TripHubBookingCategory.GENERAL,
        TripHubBookingCategory.ALL -> {
            TripHubSwipeToManage(
                expense = expense,
                onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                modifier = itemModifier
            ) {
                GeneralSharedExpenseCard(
                    expense = expense,
                    groupMembers = groupMembers,
                    allSplits = allSplits,
                    onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                    modifier = Modifier.fillMaxWidth()
                )
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
    // v2.3.2 M3E surfaces: inner passenger/berth boxes are borderless tonal washes of
    // colorScheme.surfaceContainerHigh over the deep-green card (white/sage text stays AA-readable).
    val trainBerthSurface = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
        alpha = if (SplitMateTheme.isDark) 0.42f else 0.12f
    )
    val trainBerthSurfaceEmphasis = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
        alpha = if (SplitMateTheme.isDark) 0.60f else 0.22f
    )

    val parsedTicket = remember(expense.title) { extractTravelTicketFromTitle(expense.title) }
    val pnrDigits = remember(expense.title, parsedTicket) {
        parsedTicket?.pnr?.takeIf { it.isNotBlank() }
            ?: Regex("""\b(\d{10})\b""").find(expense.title)?.groupValues?.getOrNull(1).orEmpty()
    }
    val snapshot = remember(context, pnrDigits) {
        if (pnrDigits.isNotBlank()) loadPersistedPnrSnapshot(context, pnrDigits) else null
    }

    val expenseSplits = remember(expense.expenseId, allSplits) {
        allSplits.filter { it.expenseId == expense.expenseId }
    }
    val splitBreakdown = remember(expense, groupMembers, expenseSplits) {
        SplitMateViewModel.resolveExpenseSplitBreakdown(
            expense = expense,
            groupMembers = groupMembers,
            allSplits = expenseSplits,
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
        ?: parsedTicket?.fromStation.orEmpty()).ifBlank { "—" }.uppercase(Locale.US)
    val toCode = (snapshot?.toStation?.ifBlank { parsedTicket?.toStation.orEmpty() }
        ?: parsedTicket?.toStation.orEmpty()).ifBlank { "—" }.uppercase(Locale.US)

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
    val expenseSchedule = remember(context, expense) {
        resolveExpenseSchedule(context, expense)
    }
    val loggedDateLabel = expenseSchedule.shortDateLabel
    val depTimeLabel = remember(snapshot, parsedTicket, expenseSchedule) {
        val datePrefix = expenseSchedule.shortDateLabel
        val rawDep = snapshot?.departureTime?.takeIf { it.isNotBlank() }
            ?: parsedTicket?.departureInfo?.takeIf { it.isNotBlank() }
            ?: expenseSchedule.timeLabel
        if (rawDep.contains(datePrefix, ignoreCase = true)) rawDep else "$datePrefix · $rawDep"
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
                    text = if (legNumber <= 1) "Next departure" else "Leg $legNumber",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.1.sp,
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
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            color = TripHubTokens.PositiveSageText
                        )
                    }
                }
            }
            Text(
                text = "$loggedDateLabel · Leg $legNumber",
                style = MaterialTheme.typography.labelSmall.merge(
                    TextStyle(
                        fontFamily = SplitMateTnumMonospace,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    )
                ),
                color = TripHubTokens.TextMuted
            )
        }

        // Main Deep-Green Ticket Pass + Bottom Stub.
        // v2.3.6 (device feedback): the whole ticket opens its pass (and morphs into it), not only
        // the small "Pass" button; the Berth Chart / Pass buttons inside keep their own taps.
        Surface(
            onClick = {
                performCrispTactileHaptic(context, localView, heavy = false)
                onOpenTrainPnrReviewClick(pnrDigits)
            },
            enabled = pnrDigits.isNotBlank(),
            shape = RoundedCornerShape(20.dp),
            color = TripHubTokens.CardSurface,
            border = BorderStroke(1.dp, TripHubTokens.CardBorder),
            modifier = Modifier.fillMaxWidth()
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
                                text = "Departs · $depTimeLabel",
                                style = MaterialTheme.typography.labelSmall.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFeatureSettings = "tnum"
                                    )
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
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                color = TripHubTokens.TrainSecondarySage
                            )
                            Text(
                                text = pnrDigits.ifBlank { "Verified" },
                                style = MaterialTheme.typography.labelLarge.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFeatureSettings = "tnum"
                                    )
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
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.headlineMedium,
                                color = DesignSystemBindings.activePalette.onPrimary
                            )
                            Text(
                                text = fromFullName,
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodySmall,
                                color = TripHubTokens.TrainSecondarySage,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = depTimeLabel,
                                style = MaterialTheme.typography.labelMedium.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    )
                                ),
                                color = TripHubTokens.TrainAccentLime,
                                maxLines = 1
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .widthIn(max = 148.dp)
                        ) {
                            Text(
                                text = trainNumberAndName,
                                style = MaterialTheme.typography.labelSmall.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    )
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
                                    tint = DesignSystemBindings.activePalette.onPrimary,
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
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelSmall,
                                color = TripHubTokens.TrainSecondarySage
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = toCode,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.headlineMedium,
                                color = DesignSystemBindings.activePalette.onPrimary
                            )
                            Text(
                                text = toFullName,
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodySmall,
                                color = TripHubTokens.TrainSecondarySage,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = arrTimeLabel,
                                style = MaterialTheme.typography.labelMedium.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    )
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
                                    // v2.3.2: borderless tonal cell (surfaceContainerHigh wash) instead of a hairline box.
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (cell.isCurrentUser) trainBerthSurfaceEmphasis else trainBerthSurface,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "${cell.berthCode} ${cell.statusCode}",
                                                    style = MaterialTheme.typography.labelSmall.merge(
                                                        TextStyle(
                                                            fontFamily = SplitMateTnumMonospace,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFeatureSettings = "tnum"
                                                        )
                                                    ),
                                                    color = TripHubTokens.TrainSecondarySage,
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
                                                            fontWeight = FontWeight.ExtraBold,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TripHubTokens.TrainPrimaryCtaText,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = cell.travelerName,
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = DesignSystemBindings.activePalette.onPrimary,
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
                                        .background(trainBerthSurface, CircleShape)
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (showAllOverflowBerths) {
                                            "Show fewer passengers"
                                        } else {
                                            "+$overflowCount more passengers"
                                        },
                                        style = MaterialTheme.typography.labelMedium.merge(
                                            TextStyle(
                                                fontFamily = SplitMateTnumMonospace,
                                                fontWeight = FontWeight.Bold,
                                                fontFeatureSettings = "tnum"
                                            )
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
                                TripHubMemberAvatar(
                                    seedOrName = payer?.avatarSeed?.ifBlank { payer.name } ?: "Payer",
                                    fallbackName = payer?.name ?: "P",
                                    size = 36.dp,
                                    backgroundColor = TripHubTokens.TerracottaPeachBg,
                                    textColor = TripHubTokens.TerracottaIconTint,
                                    fontSize = 12.sp
                                )
                                Column {
                                    Text(
                                        text = "Payer",
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TripHubTokens.TextSecondary
                                    )
                                    Text(
                                        text = payer?.name ?: "Group Payer",
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleSmall,
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
                                    text = splitBreakdown.perPersonCaption(
                                        "${splitBreakdown.perPersonHeadlineShare} / traveler (${splitBreakdown.splittingMembersCount}-way split)"
                                    ),
                                    style = MaterialTheme.typography.labelSmall.merge(
                                        TextStyle(
                                            fontFamily = SplitMateTnumMonospace,
                                            fontWeight = FontWeight.Bold,
                                            fontFeatureSettings = "tnum"
                                        )
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
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
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
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.labelLarge,
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
    val expenseSplits = remember(expense.expenseId, allSplits) {
        allSplits.filter { it.expenseId == expense.expenseId }
    }
    val flightResult = remember(context, expense, groupMembers, expenseSplits) {
        PnrNetworkRepository.reconstructFlightTicketFromExpense(
            context = context,
            expense = expense,
            groupMembers = groupMembers,
            allSplits = expenseSplits
        )
    }
    val pnrCode = remember(flightResult, parsedTicket, expense.title) {
        flightResult.pnr.takeIf { it.isNotBlank() }
            ?: parsedTicket?.pnr?.takeIf { it.isNotBlank() }
            ?: PnrNetworkRepository.extractPnrFromFreeText(expense.title)
    }
    val splitBreakdown = remember(expense, groupMembers, expenseSplits) {
        SplitMateViewModel.resolveExpenseSplitBreakdown(expense, groupMembers, expenseSplits)
    }
    val payer = remember(groupMembers, expense.payerId) {
        groupMembers.find { it.memberId == expense.payerId }
    }

    val originIata = flightResult.originIata.ifBlank {
        parsedTicket?.fromStation?.ifBlank { "—" } ?: "—"
    }
    val destIata = flightResult.destinationIata.ifBlank {
        parsedTicket?.toStation?.ifBlank { "—" } ?: "—"
    }
    val originCity = flightResult.originCity.ifBlank { originIata }
    val destCity = flightResult.destinationCity.ifBlank { destIata }
    val flightHeader = when {
        flightResult.flightNumber.isNotBlank() ->
            "${flightResult.airlineName} ${flightResult.flightNumber}".trim()
        parsedTicket != null && parsedTicket.trainOrFlightNo.isNotBlank() ->
            "${parsedTicket.trainOrCarrierName} ${parsedTicket.trainOrFlightNo}".trim()
        else -> cleanDisplayExpenseTitle(expense.title)
    }
    val expenseSchedule = remember(context, expense) {
        resolveExpenseSchedule(context, expense)
    }
    val loggedDateLabel = remember(expenseSchedule, flightResult) {
        val depTime = flightResult.departureTime.takeIf { it.isNotBlank() }
        if (depTime != null && !expenseSchedule.shortDateLabel.contains(depTime)) {
            "${expenseSchedule.shortDateLabel} · $depTime"
        } else {
            expenseSchedule.shortDateLabel
        }
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
                text = "Flight",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.1.sp,
                color = TripHubTokens.TextMuted
            )
            Text(
                text = loggedDateLabel,
                style = MaterialTheme.typography.labelSmall.merge(
                    TextStyle(
                        fontFamily = SplitMateTnumMonospace,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    )
                ),
                color = TripHubTokens.TextMuted
            )
        }

        Surface(
            onClick = {
                performCrispTactileHaptic(context, localView, heavy = false)
                onOpenFlightReviewClick("EXPENSE:${expense.expenseId}")
            },
            shape = RoundedCornerShape(20.dp),
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
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                                alpha = if (SplitMateTheme.isDark) 0.42f else 0.16f
                            )
                        ) {
                            Text(
                                text = flightHeader,
                                style = MaterialTheme.typography.labelSmall.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFeatureSettings = "tnum"
                                    )
                                ),
                                color = DesignSystemBindings.activePalette.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        if (pnrCode.isNotBlank()) {
                            Text(
                                text = "PNR $pnrCode",
                                style = MaterialTheme.typography.labelLarge.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFeatureSettings = "tnum"
                                    )
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
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.headlineMedium,
                                color = DesignSystemBindings.activePalette.onPrimary
                            )
                            Text(
                                text = originCity,
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodySmall,
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
                                tint = DesignSystemBindings.activePalette.onPrimary,
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
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.headlineMedium,
                                color = DesignSystemBindings.activePalette.onPrimary
                            )
                            Text(
                                text = destCity,
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodySmall,
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
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = TripHubTokens.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
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
                                text = splitBreakdown.perPersonCaption("${splitBreakdown.perPersonHeadlineShare} / traveler"),
                                style = MaterialTheme.typography.labelSmall.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    )
                                ),
                                color = TripHubTokens.PositiveSageText
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            onOpenFlightReviewClick("EXPENSE:${expense.expenseId}")
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
                                text = "Boarding pass",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelLarge,
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
    val expenseSplits = remember(expense.expenseId, allSplits) {
        allSplits.filter { it.expenseId == expense.expenseId }
    }
    val splitBreakdown = remember(expense, groupMembers, expenseSplits) {
        SplitMateViewModel.resolveExpenseSplitBreakdown(expense, groupMembers, expenseSplits)
    }
    val payer = remember(groupMembers, expense.payerId) {
        groupMembers.find { it.memberId == expense.payerId }
    }
    val loggedDateFormatted = remember(expense.createdAtEpochMs) {
        SimpleDateFormat("d MMM yyyy · h:mm a", Locale.US).format(Date(expense.createdAtEpochMs))
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
                text = "Stays",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.1.sp,
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
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    color = TripHubTokens.PositiveSageText
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TripHubTokens.CardSurface,
            border = BorderStroke(1.dp, TripHubTokens.CardBorder),
            modifier = Modifier.fillMaxWidth()
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
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = TripHubTokens.TextPrimary,
                                maxLines = 2, // v2.4.0 D2: wrap long titles instead of cutting them
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Logged on $loggedDateFormatted",
                                style = MaterialTheme.typography.bodySmall.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Medium,
                                        fontFeatureSettings = "tnum"
                                    )
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
                            style = MaterialTheme.typography.labelSmall.merge(
                                TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                )
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
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.labelSmall,
                                color = TripHubTokens.TextSecondary
                            )
                            Text(
                                text = SimpleDateFormat("d MMM yyyy", Locale.US).format(Date(expense.createdAtEpochMs)),
                                style = MaterialTheme.typography.labelLarge.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFeatureSettings = "tnum"
                                    )
                                ),
                                color = TripHubTokens.TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Group Split Mode",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.labelSmall,
                                color = TripHubTokens.TextSecondary
                            )
                            Text(
                                text = splitBreakdown.splitModeLabel("${splitBreakdown.splittingMembersCount} Travelers · Exact Split"),
                                style = MaterialTheme.typography.labelLarge.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    )
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
                        modifier = Modifier.weight(1f),
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
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelSmall,
                                color = TripHubTokens.PositiveSageText
                            )
                        }
                        Column {
                            Text(
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                text = "Paid by ${payer?.name ?: "Member"}",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium,
                                color = TripHubTokens.TextPrimary
                            )
                            Text(
                                text = splitBreakdown.splitModeLabel("All ${splitBreakdown.splittingMembersCount} shared equally"),
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.labelSmall,
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
                            text = splitBreakdown.perPersonCaption("${splitBreakdown.perPersonHeadlineShare} / person"),
                            style = MaterialTheme.typography.labelSmall.merge(
                                TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                )
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
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
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
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = TripHubTokens.TextPrimary
                            )
                        }
                    }
                }

                if (showSplitDrawer) {
                    ExpandableSplitBreakdownDrawer(
                        splitBreakdown = splitBreakdown,
                        onDeleteExpense = onDeleteExpense,
                        expense = expense
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
    val expenseSplits = remember(expense.expenseId, allSplits) {
        allSplits.filter { it.expenseId == expense.expenseId }
    }
    val splitBreakdown = remember(expense, groupMembers, expenseSplits) {
        SplitMateViewModel.resolveExpenseSplitBreakdown(expense, groupMembers, expenseSplits)
    }
    val payer = remember(groupMembers, expense.payerId) {
        groupMembers.find { it.memberId == expense.payerId }
    }
    val loggedDateStr = remember(expense.createdAtEpochMs) {
        SimpleDateFormat("d MMM · h:mm a", Locale.US).format(Date(expense.createdAtEpochMs))
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
                text = if (isTwoWheelerRental) "Rentals" else "Cabs and transfers",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.1.sp,
                color = TripHubTokens.TextMuted
            )
            Text(
                text = loggedDateStr,
                style = MaterialTheme.typography.labelSmall.merge(
                    TextStyle(
                        fontFamily = SplitMateTnumMonospace,
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum"
                    )
                ),
                color = TripHubTokens.TextMuted
            )
        }

        Surface(
            onClick = {
                performCrispTactileHaptic(context, localView, heavy = false)
                showSplitDrawer = !showSplitDrawer
            },
            shape = RoundedCornerShape(20.dp),
            color = TripHubTokens.CardSurface,
            border = BorderStroke(1.dp, TripHubTokens.CardBorder),
            modifier = Modifier.fillMaxWidth()
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
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = TripHubTokens.TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Logged $loggedDateStr · Tap for split details",
                                style = MaterialTheme.typography.labelSmall.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Medium,
                                        fontFeatureSettings = "tnum"
                                    )
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
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
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
                        modifier = Modifier.weight(1f),
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
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelSmall,
                                color = iconTint
                            )
                        }
                        Text(
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            text = "Paid by ${payer?.name ?: "Member"}",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge,
                            color = TripHubTokens.TextSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatIndianRupeesFromCents(expense.totalAmountCents),
                            style = MaterialTheme.typography.titleMedium.merge(
                                TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFeatureSettings = "tnum"
                                )
                            ),
                            color = TripHubTokens.TextPrimary
                        )
                        Text(
                            text = splitBreakdown.perPersonCaption("${splitBreakdown.perPersonHeadlineShare} / $unitSuffix"),
                            style = MaterialTheme.typography.labelSmall.merge(
                                TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                )
                            ),
                            color = TripHubTokens.PositiveSageText
                        )
                    }
                }

                if (showSplitDrawer) {
                    ExpandableSplitBreakdownDrawer(
                        splitBreakdown = splitBreakdown,
                        onDeleteExpense = onDeleteExpense,
                        expense = expense
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
    val expenseSplits = remember(expense.expenseId, allSplits) {
        allSplits.filter { it.expenseId == expense.expenseId }
    }
    val splitBreakdown = remember(expense, groupMembers, expenseSplits) {
        SplitMateViewModel.resolveExpenseSplitBreakdown(expense, groupMembers, expenseSplits)
    }
    val payer = remember(groupMembers, expense.payerId) {
        groupMembers.find { it.memberId == expense.payerId }
    }
    val dateLabel = remember(expense.createdAtEpochMs) {
        SimpleDateFormat("d MMM", Locale.US).format(Date(expense.createdAtEpochMs))
    }

    val expenseActions = LocalTripHubExpenseActions.current

    Surface(
        onClick = {
            performCrispTactileHaptic(context, localView, heavy = false)
            // v2.3.5 (#4): tap opens details (Edit / Delete); the chevron toggles the inline split.
            if (expenseActions != null) {
                expenseActions.openDetails(expense)
            } else {
                expanded = !expanded
            }
        },
        shape = RoundedCornerShape(20.dp),
        color = TripHubTokens.CardSurface,
        border = BorderStroke(1.dp, TripHubTokens.CardBorder),
        modifier = modifier
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
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = TripHubTokens.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Paid by ${payer?.name ?: "Member"} · $dateLabel",
                            style = MaterialTheme.typography.bodySmall.merge(
                                TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Medium,
                                    fontFeatureSettings = "tnum"
                                )
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
                        style = MaterialTheme.typography.titleMedium.merge(
                            TextStyle(
                                fontFamily = SplitMateTnumMonospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontFeatureSettings = "tnum"
                            )
                        ),
                        color = TripHubTokens.TextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = splitBreakdown.perPersonCaption("${splitBreakdown.perPersonHeadlineShare} each"),
                            style = MaterialTheme.typography.labelSmall.merge(
                                TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                )
                            ),
                            color = TripHubTokens.PositiveSageText
                        )
                        IconButton(
                            onClick = {
                                performCrispTactileHaptic(context, localView, heavy = false)
                                expanded = !expanded
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (expanded) "Hide split" else "Show split",
                                tint = TripHubTokens.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (expanded) {
                ExpandableSplitBreakdownDrawer(
                    splitBreakdown = splitBreakdown,
                    onDeleteExpense = onDeleteExpense,
                    expense = expense
                )
            }
        }
    }
}

@Composable
private fun ExpandableSplitBreakdownDrawer(
    splitBreakdown: SplitMateViewModel.ExpenseSplitBreakdownSummary,
    onDeleteExpense: () -> Unit,
    expense: ExpenseEntity? = null
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
            Text(
                text = splitBreakdown.headerLabel,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelMedium,
                color = TripHubTokens.TextPrimary
            )
            // v2.3.5 (#4): Edit + Delete (with confirmation) instead of an instant delete.
            TripHubManageExpenseRow(expense = expense, onDeleteExpense = onDeleteExpense)

            splitBreakdown.rows.filter { it.isIncludedInSplit }.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = row.displayName,
                        fontWeight = if (row.isCurrentUser) FontWeight.ExtraBold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodySmall,
                        color = TripHubTokens.TextPrimary
                    )
                    Text(
                        text = row.formattedShare,
                        style = MaterialTheme.typography.labelMedium.merge(
                            TextStyle(
                                fontFamily = SplitMateTnumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            )
                        ),
                        color = TripHubTokens.PositiveSageText
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
        shape = RoundedCornerShape(20.dp),
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
                text = "Add a train ticket with its PNR, upload a boarding pass, or add a stay, rental, cab or bill. Your bookings show up here as you add them.",
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
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
                        containerColor = DesignSystemBindings.activePalette.primary,
                        contentColor = DesignSystemBindings.activePalette.onPrimary
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
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleSmall)
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
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
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
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
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
    onLogQuickExpenseClick: () -> Unit,
    allSplits: List<ExpenseSplitEntity> = emptyList(),
    activePerspectiveMember: GroupMemberEntity? = null,
    onInspectBerthChart: (ExpenseEntity, LivePnrStatusSnapshot?) -> Unit = { _, _ -> },
    onDeleteExpense: (String) -> Unit = {},
    /** v2.4.0: opens the day loop (null when the guide is off). */
    onMakeLoop: (() -> Unit)? = null
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
                    modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null)
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val groupedByDay = remember(classifiedExpenses, context) {
        classifiedExpenses
            .map { (exp, cat) -> Triple(exp, cat, resolveExpenseSchedule(context, exp)) }
            .sortedBy { it.third.effectiveEpochMs }
            .groupBy { it.third.fullDateLabel }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        groupedByDay.entries.forEachIndexed { dayIndex, (dateHeader, dayItems) ->
            item(key = "day_header_$dateHeader") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (dayIndex == 0) 2.dp else 8.dp)
                        .animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null),
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
                                text = "Day ${dayIndex + 1}",
                                style = MaterialTheme.typography.labelSmall.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFeatureSettings = "tnum"
                                    )
                                ),
                                color = TripHubTokens.ActiveTabPillText,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = dateHeader,
                            style = MaterialTheme.typography.titleSmall.merge(
                                TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFeatureSettings = "tnum"
                                )
                            ),
                            color = TripHubTokens.TextPrimary
                        )
                    }
                    if (onMakeLoop != null) {
                        // v2.4.0 Trip Hub step 2: Loop plans one day's route, so it lives on the day.
                        androidx.compose.material3.FilledTonalButton(
                            onClick = onMakeLoop,
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .semantics { contentDescription = "Make a loop for day ${dayIndex + 1}" }
                        ) {
                            Icon(imageVector = Icons.Rounded.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Make a loop", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        Text(
                            text = "${dayItems.size} ${if (dayItems.size == 1) "item" else "items"}",
                            style = MaterialTheme.typography.labelSmall.merge(
                                TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum"
                                )
                            ),
                            color = TripHubTokens.TextMuted
                        )
                    }
                }
            }

            itemsIndexed(
                items = dayItems,
                key = { _, it -> "plan_${it.first.expenseId}" }
            ) { itemIndex, (expense, category, schedule) ->
                if (TripHubLayoutRules.isTicket(category)) {
                    // v2.4.0 Trip Hub step 2: tickets keep their signature card (and open animation) on their day.
                    TripHubFeedCard(
                        expense = expense,
                        category = category,
                        legNumber = (dayItems.filter { it.second == TripHubBookingCategory.TRAIN }.indexOfFirst { it.first.expenseId == expense.expenseId } + 1).coerceAtLeast(1),
                        groupMembers = groupMembers,
                        allSplits = allSplits,
                        activePerspectiveMember = activePerspectiveMember,
                        onOpenTrainPnrReviewClick = onOpenTrainPnrReviewClick,
                        onOpenFlightReviewClick = onOpenFlightReviewClick,
                        onInspectBerthChart = onInspectBerthChart,
                        onDeleteExpense = onDeleteExpense,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null)
                    )
                    return@itemsIndexed
                }
                val payer = groupMembers.find { it.memberId == expense.payerId }
                val timeLabel = schedule.timeLabel
                val itemShape = segmentedIslandItemShape(
                    index = itemIndex,
                    totalCount = dayItems.size,
                    outerCorner = 18.dp,
                    innerCorner = 6.dp
                )

                Surface(
                    shape = itemShape,
                    color = TripHubTokens.CardSurface,
                    border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null)
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
                                    .clip(RoundedPolygonShape(MaterialShapes.Sunny))
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
                                        style = MaterialTheme.typography.labelSmall.merge(
                                            TextStyle(
                                                fontFamily = SplitMateTnumMonospace,
                                                fontWeight = FontWeight.Bold,
                                                fontFeatureSettings = "tnum"
                                            )
                                        ),
                                        color = TripHubTokens.TextMuted
                                    )
                                }
                                Text(
                                    text = cleanDisplayExpenseTitle(expense.title),
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TripHubTokens.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Paid by ${payer?.name ?: "Member"}",
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TripHubTokens.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = formatIndianRupeesFromCents(expense.totalAmountCents),
                            style = MaterialTheme.typography.titleMedium.merge(
                                TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFeatureSettings = "tnum"
                                )
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
    @Suppress("UNUSED_PARAMETER") firstTrainExpenseId: String?,
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

    // v2.3.6 Step C: entrance stagger for the top cards (once per trip visit).
    val feedEntranceStart = com.splitmate.app.ui.components.rememberFirstOpenEntrance(
        "trip-travel:" + (travelOnlyItems.firstOrNull()?.first?.groupId ?: "")
    )
    val entranceIndexById = remember(travelOnlyItems) {
        travelOnlyItems.mapIndexed { idx, (exp, _) -> exp.expenseId to idx }.toMap()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Interactive 3D Flip Travel Pass Deck Launcher
        item(key = "classic_3d_transit_deck_launcher") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tickets",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.1.sp,
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
                                text = "List view",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
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
                .animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null)
                .staggeredEntrance(entranceIndexById[expense.expenseId] ?: Int.MAX_VALUE, feedEntranceStart)
                .expressivePressScale()
            // v2.3.6 Step C1: booking cards morph into their ticket (container transform).
            val ticketTransform = com.splitmate.app.ui.components.LocalTicketContainerTransform.current

            when (category) {
                TripHubBookingCategory.TRAIN -> {
                    val legNumber = trainLegNumberById[expense.expenseId] ?: 1
                    // v2.3.5 (#4): Train / Flight cards also get Edit / Delete.
                    Column(modifier = itemModifier) {
                        TripHubSwipeToManage(
                            expense = expense,
                            onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                        com.splitmate.app.ui.components.TicketContainerSource(
                            sourceKey = expense.expenseId,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DeepGreenTrainTicketCard(
                                expense = expense,
                                legNumber = legNumber,
                                groupMembers = groupMembers,
                                allSplits = allSplits,
                                activePerspectiveMember = activePerspectiveMember,
                                onOpenTrainPnrReviewClick = { pnr ->
                                    if (pnr.isNotBlank()) ticketTransform?.controller?.markSource(expense.expenseId)
                                    onOpenTrainPnrReviewClick(pnr)
                                },
                                onInspectBerthChart = onInspectBerthChart,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        }
                        TripHubManageExpenseRow(
                            expense = expense,
                            onDeleteExpense = { onDeleteExpense(expense.expenseId) }
                        )
                    }
                }

                TripHubBookingCategory.FLIGHT -> {
                    Column(modifier = itemModifier) {
                        TripHubSwipeToManage(
                            expense = expense,
                            onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                        com.splitmate.app.ui.components.TicketContainerSource(
                            sourceKey = expense.expenseId,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PeriwinkleFlightBookingCard(
                                expense = expense,
                                groupMembers = groupMembers,
                                allSplits = allSplits,
                                activePerspectiveMember = activePerspectiveMember,
                                onOpenFlightReviewClick = { key ->
                                    if (key.isNotBlank()) ticketTransform?.controller?.markSource(expense.expenseId)
                                    onOpenFlightReviewClick(key)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        }
                        TripHubManageExpenseRow(
                            expense = expense,
                            onDeleteExpense = { onDeleteExpense(expense.expenseId) }
                        )
                    }
                }

                TripHubBookingCategory.STAY -> {
                    TripHubSwipeToManage(
                        expense = expense,
                        onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                        modifier = itemModifier
                    ) {
                        LodgingBookingCard(
                            expense = expense,
                            groupMembers = groupMembers,
                            allSplits = allSplits,
                            onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                TripHubBookingCategory.RENTAL -> {
                    TripHubSwipeToManage(
                        expense = expense,
                        onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                        modifier = itemModifier
                    ) {
                        GroundMobilityBookingCard(
                            expense = expense,
                            isTwoWheelerRental = true,
                            groupMembers = groupMembers,
                            allSplits = allSplits,
                            onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                TripHubBookingCategory.CAB -> {
                    TripHubSwipeToManage(
                        expense = expense,
                        onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                        modifier = itemModifier
                    ) {
                        GroundMobilityBookingCard(
                            expense = expense,
                            isTwoWheelerRental = false,
                            groupMembers = groupMembers,
                            allSplits = allSplits,
                            onDeleteExpense = { onDeleteExpense(expense.expenseId) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                else -> {}
            }
        }
    }
}

/**
 * Subtask 3.3.3: `Money` Tab — Greedy Minimum Cash Flow Settlements (`SplitMateMathEngine.simplifyDebtsGreedy`)
 * + Compact Max-Heap Graph Inspector (`Icons.Rounded.Info`, `48.dp` touch bounds) + `Mark paid`.
 */
@Composable
private fun TripHubMoneySettlementView(
    viewModel: SplitMateViewModel,
    groupId: String,
    @Suppress("UNUSED_PARAMETER") groupName: String,
    groupMembers: List<GroupMemberEntity>,
    netBalancesMap: Map<String, Long>,
    moneyCheckReport: com.splitmate.app.MoneyCheck.Report,
    onOpenSettleUpClick: () -> Unit,
    /** v2.4.0 Trip Hub step 1: the trip's one full expense list, appended under the settle section. */
    allExpensesSection: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val localView = LocalView.current
    val uiState by viewModel.uiState.collectAsState()
    var showGraphInspector by remember { mutableStateOf(false) }

    // v2.3.6 P3 Money check: sheet + "Review" (opens the failing expense's detail sheet).
    var showMoneyCheckSheet by rememberSaveable { mutableStateOf(false) }
    val expenseActions = LocalTripHubExpenseActions.current
    val moneyCheckSync = com.splitmate.app.MoneyCheckSyncStatus(
        isSyncing = uiState.isCloudSyncing,
        isOffline = uiState.isOfflineMode,
        hasPendingPush = com.splitmate.app.data.CloudGroupSyncRepository.isGroupPendingCloudPush(null, groupId),
        lastConfirmedEpochMs = com.splitmate.app.data.CloudGroupSyncRepository.lastConfirmedInSyncEpoch(groupId)
    )
    val expenseTitleOf: (String) -> String = { id ->
        uiState.expenses.find { it.expenseId == id }?.let { cleanDisplayExpenseTitle(it.title) } ?: "An expense"
    }
    val openExpense: (String) -> Unit = { id ->
        uiState.expenses.find { it.expenseId == id }?.let { exp ->
            showMoneyCheckSheet = false
            expenseActions?.openDetails?.invoke(exp)
        }
    }
    val firstFailingExpense = moneyCheckReport.failingExpenseIds.firstOrNull()
        ?.let { id -> uiState.expenses.find { it.expenseId == id } }
    val markPaidWarning = if (moneyCheckReport.passed) null else com.splitmate.app.MoneyCheckCopy.MARK_PAID_WARNING
    if (showMoneyCheckSheet) {
        MoneyCheckSheet(
            report = moneyCheckReport,
            sync = moneyCheckSync,
            titleOf = expenseTitleOf,
            onOpenExpense = openExpense,
            onDismiss = { showMoneyCheckSheet = false }
        )
    }

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
    val currentUserMember = remember(groupMembers) {
        groupMembers.find { it.isCurrentUser }
    }
    val currentUserId = currentUserMember?.memberId
    val unsettledTransfers = simplifiedTransfers
    val netCents = currentUserMember?.let { netBalancesMap[it.memberId] ?: 0L } ?: 0L
    val isAllSettled = netCents == 0L && unsettledTransfers.isEmpty()
    val (myTransfers, otherTransfers) = remember(simplifiedTransfers, currentUserId) {
        simplifiedTransfers.partition {
            currentUserId != null && (it.fromMemberId == currentUserId || it.toMemberId == currentUserId)
        }
    }
    // v2.3.6 Step C: celebrate only the moment your last payment clears (not on every open).
    val hasMyTransfersNow = myTransfers.isNotEmpty()
    var hadMyTransfers by remember(groupId) { mutableStateOf(hasMyTransfersNow) }
    var celebrateSettled by remember(groupId) { mutableStateOf(false) }
    LaunchedEffect(hasMyTransfersNow) {
        if (SettledCelebrationDefaults.shouldCelebrate(hadMyTransfers, hasMyTransfersNow)) celebrateSettled = true
        hadMyTransfers = hasMyTransfersNow
    }
    val settlementProgress = remember(isAllSettled, simplifiedTransfers.size, groupMembers.size) {
        if (isAllSettled || simplifiedTransfers.isEmpty()) {
            1f
        } else {
            val maxPossibleTransfers = (groupMembers.size - 1).coerceAtLeast(1)
            (1f - (simplifiedTransfers.size.toFloat() / (maxPossibleTransfers + 1).toFloat())).coerceIn(0.25f, 0.9f)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // v2.3.6 P3: calm, non-dismissable card while any Money check fails (springs in/out).
        if (!moneyCheckReport.passed) {
            item(key = "money_check_failure") {
                MoneyCheckFailureCard(
                    report = moneyCheckReport,
                    titleOf = expenseTitleOf,
                    canFix = firstFailingExpense?.let { expenseActions?.canModify?.invoke(it) } ?: false,
                    onReview = {
                        performCrispTactileHaptic(context, localView, heavy = false)
                        if (moneyCheckReport.failingExpenseIds.size == 1 && firstFailingExpense != null) {
                            openExpense(firstFailingExpense.expenseId)
                        } else {
                            showMoneyCheckSheet = true
                        }
                    },
                    modifier = Modifier.animateItem(
                        fadeInSpec = LocalMotionScheme.current.defaultEffectsSpec(),
                        placementSpec = LocalMotionScheme.current.defaultSpatialSpec(),
                        fadeOutSpec = LocalMotionScheme.current.fastEffectsSpec()
                    )
                )
            }
        }
        // Header + Compact Max-Heap Graph Inspector Toggle (`Icons.Rounded.Info`, >= 48.dp touch bounds)
        item(key = "greedy_settlement_header") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = TripHubTokens.CardSurface,
                border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null)
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
                                    text = "Smart settle up",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TripHubTokens.TextPrimary
                                )
                                Text(
                                    text = if (simplifiedTransfers.isEmpty()) "Everyone is settled"
                                    else "${simplifiedTransfers.size} ${if (simplifiedTransfers.size == 1) "payment" else "payments"} to settle everyone",
                                    style = MaterialTheme.typography.bodySmall.merge(
                                        TextStyle(
                                            fontFamily = SplitMateTnumMonospace,
                                            fontWeight = FontWeight.Medium,
                                            fontFeatureSettings = "tnum"
                                        )
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
                                contentDescription = "How payments were simplified",
                                tint = TripHubTokens.PositiveSageText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // v2.3.6 P3 Money check: the old static "Exact to the last paisa" claim is now a
                    // live, checked fact. Tap for the full checklist.
                    MoneyCheckStatusRow(
                        report = moneyCheckReport,
                        sync = moneyCheckSync,
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            showMoneyCheckSheet = true
                        }
                    )

                    // v2.3.6: no progress line here. Settlement totals are static money data
                    // (v2.3.2 rule); wavy progress is reserved for in-flight network work.

                    // v2.3.5 (#2): share the whole settle-up (same greedy transfers as below) as an image.
                    com.splitmate.app.ui.share.SettleUpShareButton(
                        buildModel = {
                            com.splitmate.app.ui.share.buildSettleUpShareModelForGroup(
                                groupName = groupName,
                                groupMembers = groupMembers,
                                groupExpenses = uiState.expenses.filter { it.groupId == groupId },
                                transfers = simplifiedTransfers,
                                currentUserName = uiState.currentUserName,
                                tripEnded = com.splitmate.app.data.GroupLedgerExtrasStore.tripLifecycle(groupId)?.isEnded == true
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (simplifiedTransfers.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TripHubTokens.SunkenWell,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = null,
                                    tint = TripHubTokens.PositiveSageText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Recipients confirm payments once received · Organizers can settle for offline members",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TripHubTokens.TextSecondary
                                )
                            }
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
                                    text = "Combined into the fewest possible payments",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TripHubTokens.TextPrimary
                                )
                                Text(
                                    text = "Automatically combines everyone's shared expenses so your group settles up with the fewest possible direct payments.",
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodySmall,
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
                val settledMorph = remember {
                    Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny)
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = TripHubTokens.CardSurface,
                    border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // v2.3.6 Step C: the Cookie9Sided -> Sunny morph now actually plays (bouncy spring +
                        // confirm haptic) the moment your last payment clears; static otherwise.
                        SettledCelebrationBadge(
                            celebrate = celebrateSettled,
                            morph = settledMorph,
                            containerColor = TripHubTokens.PositiveSagePillBg,
                            iconTint = TripHubTokens.PositiveSageText,
                            size = 38.dp
                        )
                        Column {
                            Text(
                                text = "All Group Debts Settled (₹0.00)",
                                style = MaterialTheme.typography.titleMedium.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFeatureSettings = "tnum"
                                    )
                                ),
                                color = TripHubTokens.TextPrimary
                            )
                            Text(
                                text = "Every traveler's balance is settled to the exact paisa.",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodySmall,
                                color = TripHubTokens.TextSecondary
                            )
                        }
                    }
                }
            }
        } else {
            // SECTION 1: YOUR SETTLEMENTS (Me-First Hierarchy)
            if (!moneyCheckReport.passed) {
                item(key = "money_check_hold_note") {
                    MoneyCheckSettleHoldNote()
                }
            }
            item(key = "your_settlements_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Your settlements",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = TripHubTokens.TextPrimary
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (myTransfers.isEmpty()) TripHubTokens.PositiveSagePillBg else TripHubTokens.SunkenWell
                    ) {
                        Text(
                            text = if (myTransfers.isEmpty()) "All settled" else "${myTransfers.size} ${if (myTransfers.size == 1) "payment" else "payments"}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (myTransfers.isEmpty()) TripHubTokens.PositiveSageText else TripHubTokens.TextSecondary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (myTransfers.isEmpty()) {
                item(key = "your_settlements_zero_banner") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = TripHubTokens.CardSurface,
                        border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SettledCelebrationBadge(
                                celebrate = celebrateSettled,
                                containerColor = TripHubTokens.PositiveSagePillBg,
                                iconTint = TripHubTokens.PositiveSageText
                            )
                            Column {
                                Text(
                                    text = "You're all settled up (₹0.00)",
                                    style = MaterialTheme.typography.labelLarge.merge(
                                        TextStyle(
                                            fontFamily = SplitMateTnumMonospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFeatureSettings = "tnum"
                                        )
                                    ),
                                    color = TripHubTokens.TextPrimary
                                )
                                Text(
                                    text = "No payments needed from or to you.",
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TripHubTokens.TextSecondary
                                )
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = myTransfers,
                    key = { _, it -> "my_${it.fromMemberId}_${it.toMemberId}_${it.amountCents}" }
                ) { index, settlement ->
                    val fromMember = groupMembers.find { it.memberId == settlement.fromMemberId }
                    val toMember = groupMembers.find { it.memberId == settlement.toMemberId }
                    val isCurrentUserPayer = currentUserId == settlement.fromMemberId
                    val isFromOnline = fromMember != null && uiState.isMemberOnline(fromMember)
                    val isToOnline = toMember != null && uiState.isMemberOnline(toMember)
                    val formattedAmount = formatIndianRupeesFromCents(settlement.amountCents)
                    val canMarkPaid = viewModel.canCurrentUserMarkTransferPaid(
                        groupId = groupId,
                        toMemberId = settlement.toMemberId,
                        state = uiState
                    )
                    val restrictionLabel = viewModel.getMarkPaidRestrictionLabel(
                        groupId = groupId,
                        toMemberId = settlement.toMemberId,
                        toMemberName = settlement.toName,
                        state = uiState
                    )
                    val recipientFirstName = settlement.toName.trim().substringBefore(" ").ifBlank { settlement.toName }
                    var showRestrictionHint by remember(settlement.fromMemberId, settlement.toMemberId) { mutableStateOf(false) }
                    var pendingMarkPaidConfirm by remember(settlement.fromMemberId, settlement.toMemberId) { mutableStateOf(false) }
                    val rowShape = segmentedIslandItemShape(
                        index = index,
                        totalCount = myTransfers.size,
                        outerCorner = 20.dp,
                        innerCorner = 6.dp
                    )

                    if (pendingMarkPaidConfirm) {
                        TripHubMarkPaidConfirmDialog(
                            fromName = settlement.fromName,
                            toName = settlement.toName,
                            formattedAmount = formattedAmount,
                            warning = markPaidWarning,
                            onConfirm = {
                                pendingMarkPaidConfirm = false
                                performCrispTactileHaptic(context, localView, heavy = false)
                                viewModel.recordSettlement(
                                    groupId = groupId,
                                    fromMemberId = settlement.fromMemberId,
                                    toMemberId = settlement.toMemberId,
                                    amountCents = settlement.amountCents
                                )
                            },
                            onDismiss = { pendingMarkPaidConfirm = false }
                        )
                    }
                    ExpressiveSwipeActionsBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null),
                        startAction = if (canMarkPaid) {
                            ExpressiveSwipeAction(
                                label = "Mark paid",
                                icon = Icons.Rounded.CheckCircleOutline,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onTrigger = { pendingMarkPaidConfirm = true }
                            )
                        } else null,
                        shape = rowShape
                    ) {
                        Surface(
                            shape = rowShape,
                            color = TripHubTokens.CardSurface,
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = if (isCurrentUserPayer) TripHubTokens.TerracottaIconTint.copy(alpha = 0.35f) else BuckwheatOlivePrimary.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(SplitMateMotion.defaultSpatial())
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(15.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        // v2.4.0 D2: TalkBack hears one sentence ("Sam pays you 14 rupees 17 paise")
                                        .clearAndSetSemantics {
                                            contentDescription = com.splitmate.app.ui.a11y.SpokenMoney.settlementSentence(
                                                fromName = settlement.fromName,
                                                toName = settlement.toName,
                                                amountCents = settlement.amountCents,
                                                currentUserIsPayer = isCurrentUserPayer,
                                                currentUserIsReceiver = !isCurrentUserPayer
                                            )
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        TripHubMemberAvatar(
                                            seedOrName = if (isCurrentUserPayer) {
                                                toMember?.avatarSeed?.ifBlank { settlement.toName } ?: settlement.toName
                                            } else {
                                                fromMember?.avatarSeed?.ifBlank { settlement.fromName } ?: settlement.fromName
                                            },
                                            fallbackName = if (isCurrentUserPayer) settlement.toName else settlement.fromName,
                                            size = 36.dp,
                                            backgroundColor = if (isCurrentUserPayer) TripHubTokens.TerracottaPeachBg else TripHubTokens.PositiveSagePillBg,
                                            textColor = if (isCurrentUserPayer) TripHubTokens.TerracottaIconTint else TripHubTokens.PositiveSageText,
                                            fontSize = 12.sp,
                                            isOnline = if (isCurrentUserPayer) isToOnline else isFromOnline
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (isCurrentUserPayer) TripHubTokens.TerracottaPeachBg else TripHubTokens.PositiveSagePillBg
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                    ) {
                                                        // v2.4.0 D2: owe vs receive shown by icon + words, not colour alone
                                                        Icon(
                                                            imageVector = if (isCurrentUserPayer) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                                            contentDescription = null,
                                                            tint = if (isCurrentUserPayer) TripHubTokens.TerracottaIconTint else TripHubTokens.PositiveSageText,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = if (isCurrentUserPayer) "You pay" else "You receive",
                                                            fontWeight = FontWeight.ExtraBold,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isCurrentUserPayer) TripHubTokens.TerracottaIconTint else TripHubTokens.PositiveSageText
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = if (isCurrentUserPayer) "to ${settlement.toName}" else "from ${settlement.fromName}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = TripHubTokens.TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (!canMarkPaid && isCurrentUserPayer) {
                                                Text(
                                                    text = "Tap status pill for confirmation info",
                                                    fontWeight = FontWeight.Medium,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TripHubTokens.TextSecondary
                                                )
                                            }
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (isCurrentUserPayer) "-$formattedAmount" else "+$formattedAmount",
                                            style = TextStyle(
                                                fontFamily = SplitMateTnumMonospace,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = if (isCurrentUserPayer) TripHubTokens.TerracottaIconTint else TripHubTokens.PositiveSageText
                                        )
                                        if (!canMarkPaid && isCurrentUserPayer) {
                                            Surface(
                                                onClick = {
                                                    performCrispTactileHaptic(context, localView, heavy = false)
                                                    showRestrictionHint = !showRestrictionHint
                                                },
                                                shape = CircleShape,
                                                color = TripHubTokens.TerracottaPeachBg,
                                                border = BorderStroke(
                                                    1.dp,
                                                    TripHubTokens.TerracottaIconTint.copy(alpha = 0.25f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Schedule,
                                                        contentDescription = null,
                                                        tint = TripHubTokens.TerracottaIconTint,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Text(
                                                        text = "Awaiting $recipientFirstName",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TripHubTokens.TerracottaIconTint
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (canMarkPaid) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SplitButtonLayout(
                                            leadingText = "Mark paid",
                                            leadingIcon = Icons.Rounded.CheckCircleOutline,
                                            onLeadingClick = {
                                                performCrispTactileHaptic(context, localView, heavy = false)
                                                viewModel.recordSettlement(
                                                    groupId = groupId,
                                                    fromMemberId = settlement.fromMemberId,
                                                    toMemberId = settlement.toMemberId,
                                                    amountCents = settlement.amountCents
                                                )
                                            },
                                            menuItems = listOf(
                                                ExpressiveMenuAction(
                                                    label = "Confirm Full Settlement ($formattedAmount)",
                                                    icon = Icons.Rounded.CheckCircleOutline,
                                                    subtitle = "${settlement.fromName} to ${settlement.toName}",
                                                    onClick = {
                                                        performCrispTactileHaptic(context, localView, heavy = false)
                                                        viewModel.recordSettlement(
                                                            groupId = groupId,
                                                            fromMemberId = settlement.fromMemberId,
                                                            toMemberId = settlement.toMemberId,
                                                            amountCents = settlement.amountCents
                                                        )
                                                    }
                                                ),
                                                ExpressiveMenuAction(
                                                    label = "Open Settle up Sheet",
                                                    icon = Icons.Rounded.AccountBalanceWallet,
                                                    subtitle = "See every balance and settle your way",
                                                    onClick = onOpenSettleUpClick
                                                )
                                            ),
                                            containerColor = DesignSystemBindings.activePalette.primary,
                            contentColor = DesignSystemBindings.activePalette.onPrimary
                                        )
                                    }
                                } else {
                                    AnimatedVisibility(visible = showRestrictionHint) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = TripHubTokens.SunkenWell,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Info,
                                                    contentDescription = null,
                                                    tint = TripHubTokens.TextSecondary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = restrictionLabel,
                                                    fontWeight = FontWeight.Medium,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TripHubTokens.TextSecondary
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

            // SECTION 2: OTHER TRAVELERS' SETTLEMENTS (No repetitive per-row confirmation pills)
            if (otherTransfers.isNotEmpty()) {
                item(key = "other_settlements_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Other travelers' settlements",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = TripHubTokens.TextPrimary
                            )
                            Text(
                                text = "${otherTransfers.size} ${if (otherTransfers.size == 1) "transfer" else "transfers"}",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelSmall,
                                color = TripHubTokens.TextSecondary
                            )
                        }
                        Text(
                            text = "Confirmed by each recipient once received",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.labelSmall,
                            color = TripHubTokens.TextSecondary
                        )
                    }
                }

                itemsIndexed(
                    items = otherTransfers,
                    key = { _, it -> "other_${it.fromMemberId}_${it.toMemberId}_${it.amountCents}" }
                ) { index, settlement ->
                    val fromMember = groupMembers.find { it.memberId == settlement.fromMemberId }
                    val toMember = groupMembers.find { it.memberId == settlement.toMemberId }
                    val isFromOnline = fromMember != null && uiState.isMemberOnline(fromMember)
                    val isToOnline = toMember != null && uiState.isMemberOnline(toMember)
                    val formattedAmount = formatIndianRupeesFromCents(settlement.amountCents)
                    val canMarkPaid = viewModel.canCurrentUserMarkTransferPaid(
                        groupId = groupId,
                        toMemberId = settlement.toMemberId,
                        state = uiState
                    )
                    val rowShape = segmentedIslandItemShape(
                        index = index,
                        totalCount = otherTransfers.size,
                        outerCorner = 18.dp,
                        innerCorner = 6.dp
                    )

                    Surface(
                        shape = rowShape,
                        color = TripHubTokens.CardSurface,
                        border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    TripHubMemberAvatar(
                                        seedOrName = fromMember?.avatarSeed?.ifBlank { settlement.fromName } ?: settlement.fromName,
                                        fallbackName = settlement.fromName,
                                        size = 30.dp,
                                        backgroundColor = TripHubTokens.SunkenWell,
                                        textColor = TripHubTokens.TextPrimary,
                                        fontSize = 11.sp,
                                        isOnline = isFromOnline
                                    )
                                    Text(
                                        text = settlement.fromName,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TripHubTokens.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = "pays",
                                        tint = TripHubTokens.TextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    TripHubMemberAvatar(
                                        seedOrName = toMember?.avatarSeed?.ifBlank { settlement.toName } ?: settlement.toName,
                                        fallbackName = settlement.toName,
                                        size = 30.dp,
                                        backgroundColor = TripHubTokens.PositiveSagePillBg,
                                        textColor = TripHubTokens.PositiveSageText,
                                        fontSize = 11.sp,
                                        isOnline = isToOnline
                                    )
                                    Text(
                                        text = settlement.toName,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TripHubTokens.PositiveSageText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = formattedAmount,
                                    style = MaterialTheme.typography.titleMedium.merge(
                                        TextStyle(
                                            fontFamily = SplitMateTnumMonospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFeatureSettings = "tnum"
                                        )
                                    ),
                                    color = TripHubTokens.TextPrimary
                                )
                            }

                            var pendingOtherConfirm by remember(settlement.fromMemberId, settlement.toMemberId) { mutableStateOf(false) }
                            if (pendingOtherConfirm) {
                                TripHubMarkPaidConfirmDialog(
                                    fromName = settlement.fromName,
                                    toName = settlement.toName,
                                    formattedAmount = formattedAmount,
                                    warning = markPaidWarning,
                                    onConfirm = {
                                        pendingOtherConfirm = false
                                        viewModel.recordSettlement(
                                            groupId = groupId,
                                            fromMemberId = settlement.fromMemberId,
                                            toMemberId = settlement.toMemberId,
                                            amountCents = settlement.amountCents
                                        )
                                    },
                                    onDismiss = { pendingOtherConfirm = false }
                                )
                            }
                            if (!canMarkPaid) {
                                // v2.3.6 (device feedback): an unpaid transfer you can't confirm used to show
                                // no status at all, so it was unclear whether it was paid. Say who confirms it.
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Not paid yet · ${settlement.toName.substringBefore(" ")} or an organizer can confirm",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (canMarkPaid) {
                                Button(
                                    onClick = {
                                        performCrispTactileHaptic(context, localView, heavy = false)
                                        pendingOtherConfirm = true
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DesignSystemBindings.activePalette.primary,
                        contentColor = DesignSystemBindings.activePalette.onPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .defaultMinSize(minHeight = 40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircleOutline,
                                        contentDescription = null,
                                        tint = DesignSystemBindings.activePalette.onPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Mark paid",
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // v2.3.6: payments that happened outside the suggested plan (e.g. someone paid a
            // different person directly) can be recorded here; the plan recalculates from them.
            val receiverIdsICanConfirm = groupMembers
                .filter { viewModel.canCurrentUserMarkTransferPaid(groupId, it.memberId, uiState) }
                .map { it.memberId }
                .toSet()
            if (receiverIdsICanConfirm.isNotEmpty() && groupMembers.size > 1) {
                item(key = "record_manual_payment") {
                    var showRecordPayment by remember { mutableStateOf(false) }
                    FilledTonalButton(
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            showRecordPayment = true
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Record a payment", style = MaterialTheme.typography.labelLarge)
                    }
                    if (showRecordPayment) {
                        RecordPaymentDialog(
                            members = groupMembers,
                            allowedReceiverIds = receiverIdsICanConfirm,
                            onRecord = { fromId, toId, cents ->
                                if (groupId.isNotBlank()) viewModel.selectActiveGroup(groupId)
                                viewModel.recordManualPayment(fromId, toId, cents)
                            },
                            onDismiss = { showRecordPayment = false }
                        )
                    }
                }
            }
        }
        allExpensesSection()
    }
}

/**
 * Subtask 3.3.4: `People` Tab — Lists all real `GroupMemberEntity` travelers with avatar initials,
 * live online presence dot + badge, phone status, live `tnum` net balance pill, and 1-tap perspective switching.
 */
@Composable
private fun TripHubPeoplePerspectiveView(
    viewModel: SplitMateViewModel,
    groupId: String,
    groupMembers: List<GroupMemberEntity>,
    netBalancesMap: Map<String, Long>,
    onAddMemberClick: () -> Unit,
    onOpenSyncSheetClick: () -> Unit,
    onOpenSettleUpClick: () -> Unit = {},
    onLeaveGroupSuccess: () -> Unit = {},
    isTravelGroup: Boolean = true
) {
    val context = LocalContext.current
    val localView = LocalView.current
    val uiState by viewModel.uiState.collectAsState()

    val formattedJoinCode = remember(groupId, uiState.groups) {
        viewModel.getFormattedGroupJoinCode(groupId)
    }
    val organizerMembers = remember(groupMembers, uiState.groups, uiState.members, uiState.statusBannerMessage, groupId) {
        viewModel.getGroupOrganizerMembers(groupId, uiState)
    }
    val isCurrentUserOrganizer = remember(groupMembers, uiState.groups, uiState.members, uiState.userPhone, uiState.statusBannerMessage, groupId) {
        viewModel.isUserGroupOrganizer(groupId, uiState)
    }

    var editingMemberForPhoneInvite by remember { mutableStateOf<GroupMemberEntity?>(null) }
    var editedMemberName by remember { mutableStateOf("") }
    var editedMemberPhone by remember { mutableStateOf("") }
    var editPhoneDialogStatus by remember { mutableStateOf<String?>(null) }
    var isEditPhoneError by remember { mutableStateOf(false) }

    var confirmingRemoveMember by remember { mutableStateOf<GroupMemberEntity?>(null) }
    var removeMemberStatusMsg by remember { mutableStateOf<String?>(null) }

    var showLeaveTripConfirmDialog by remember { mutableStateOf(false) }
    var leaveTripStatusMsg by remember { mutableStateOf<String?>(null) }

    var peopleFeedbackBanner by remember { mutableStateOf<String?>(null) }
    var isPeopleFeedbackError by remember { mutableStateOf(false) }

    editingMemberForPhoneInvite?.let { targetMember ->
        val cleanTypedPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianMobile(editedMemberPhone)
        AlertDialog(
            onDismissRequest = {
                editingMemberForPhoneInvite = null
                editPhoneDialogStatus = null
            },
            containerColor = TripHubTokens.CanvasBg,
            title = {
                Text(
                    text = "Update Mobile & Send Invite",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = TripHubTokens.TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Link a 10-digit +91 mobile number for ${targetMember.name} so this trip syncs automatically to their phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TripHubTokens.TextSecondary
                    )
                    OutlinedTextField(
                        value = editedMemberName,
                        onValueChange = {
                            editedMemberName = it
                            editPhoneDialogStatus = null
                        },
                        label = { Text("Member Name", fontFamily = FigtreeFontFamily, fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedMemberPhone,
                        onValueChange = { raw ->
                            editedMemberPhone = raw.filter { it.isDigit() || it == '+' || it == ' ' }.take(15)
                            editPhoneDialogStatus = null
                        },
                        label = { Text("10-Digit Mobile Number", fontFamily = FigtreeFontFamily, fontSize = 12.sp) },
                        prefix = {
                            Text(
                                text = "+91 ",
                                fontFamily = SplitMateTnumMonospace,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                color = TripHubTokens.PositiveSageText
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    editPhoneDialogStatus?.let { status ->
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isEditPhoneError) TripHubTokens.TerracottaIconTint else TripHubTokens.PositiveSageText
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateMemberPhoneAndResendInvite(
                            context = context,
                            groupId = groupId,
                            memberId = targetMember.memberId,
                            rawPhone = editedMemberPhone,
                            newName = editedMemberName.trim().ifBlank { targetMember.name },
                            launchWhatsAppShare = true
                        ) { ok, msg ->
                            isEditPhoneError = !ok
                            editPhoneDialogStatus = msg
                            isPeopleFeedbackError = !ok
                            peopleFeedbackBanner = msg
                            if (ok) {
                                editingMemberForPhoneInvite = null
                                editPhoneDialogStatus = null
                            }
                        }
                    },
                    enabled = cleanTypedPhone10.length == 10 && editedMemberName.trim().isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesignSystemBindings.activePalette.primary,
                        contentColor = DesignSystemBindings.activePalette.onPrimary
                    )
                ) {
                    Text(
                        text = "Save & Send Invite",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelMedium)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editingMemberForPhoneInvite = null
                        editPhoneDialogStatus = null
                    }
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = TripHubTokens.TextSecondary
                    )
                }
            }
        )
    }

    confirmingRemoveMember?.let { memberToRemove ->
        val memberNetCents = netBalancesMap[memberToRemove.memberId] ?: 0L
        val isSettled = memberNetCents == 0L
        val memberNetFormatted = formatIndianRupeesFromCents(abs(memberNetCents))
        val memberNetSignLabel = when {
            memberNetCents > 0L -> "+$memberNetFormatted"
            memberNetCents < 0L -> "-$memberNetFormatted"
            else -> "₹0.00"
        }
        AlertDialog(
            onDismissRequest = {
                confirmingRemoveMember = null
                removeMemberStatusMsg = null
            },
            containerColor = TripHubTokens.CanvasBg,
            title = {
                Text(
                    text = if (isSettled) "Remove ${memberToRemove.name}?" else "Remove & Rebalance ${memberToRemove.name}?",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = TripHubTokens.TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isSettled) {
                        Text(
                            text = "Remove ${memberToRemove.name} from this trip? Their balance is settled (₹0.00).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TripHubTokens.TextSecondary
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TripHubTokens.TerracottaPeachBg,
                            border = BorderStroke(1.dp, DesignSystemBindings.activePalette.secondary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${memberToRemove.name} has an unsettled balance ($memberNetSignLabel). Removing them will reassign any expenses they paid to the Organizer and redistribute their split shares across the surviving participants, down to the last paisa.",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TripHubTokens.TerracottaIconTint,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                confirmingRemoveMember = null
                                removeMemberStatusMsg = null
                                onOpenSettleUpClick()
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, TripHubTokens.PositiveSageText.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Settle Balance First",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelMedium,
                                color = TripHubTokens.PositiveSageText
                            )
                        }
                    }
                    removeMemberStatusMsg?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TripHubTokens.TerracottaIconTint
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeMemberFromGroup(
                            context = context,
                            groupId = groupId,
                            targetMemberId = memberToRemove.memberId
                        ) { ok, msg ->
                            if (ok) {
                                isPeopleFeedbackError = false
                                peopleFeedbackBanner = msg
                                confirmingRemoveMember = null
                                removeMemberStatusMsg = null
                            } else {
                                removeMemberStatusMsg = msg
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesignSystemBindings.activePalette.secondary,
                            contentColor = DesignSystemBindings.activePalette.onSecondary
                    )
                ) {
                    Text(
                        text = if (isSettled) "Remove member" else "Remove & Rebalance",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelMedium)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmingRemoveMember = null
                        removeMemberStatusMsg = null
                    }
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = TripHubTokens.TextSecondary
                    )
                }
            }
        )
    }

    if (showLeaveTripConfirmDialog) {
        val myMember = groupMembers.find { it.isCurrentUser }
        val myNetCents = myMember?.let { netBalancesMap[it.memberId] ?: 0L } ?: 0L
        val myNetFormatted = formatIndianRupeesFromCents(abs(myNetCents))
        val myNetSignLabel = when {
            myNetCents > 0L -> "+$myNetFormatted"
            myNetCents < 0L -> "-$myNetFormatted"
            else -> "₹0.00"
        }
        AlertDialog(
            onDismissRequest = {
                showLeaveTripConfirmDialog = false
                leaveTripStatusMsg = null
            },
            containerColor = TripHubTokens.CanvasBg,
            title = {
                Text(
                    text = "Leave trip?",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = TripHubTokens.TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (myNetCents != 0L) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TripHubTokens.TerracottaPeachBg,
                            border = BorderStroke(1.dp, DesignSystemBindings.activePalette.secondary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "You have an unsettled balance ($myNetSignLabel) in this group. Please settle up before leaving.",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TripHubTokens.TerracottaIconTint,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Leave this trip? It will be removed from your active groups list, and remaining members will keep the ledger.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TripHubTokens.TextSecondary
                        )
                    }
                    leaveTripStatusMsg?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TripHubTokens.TerracottaIconTint
                        )
                    }
                }
            },
            confirmButton = {
                if (myNetCents != 0L) {
                    Button(
                        onClick = {
                            showLeaveTripConfirmDialog = false
                            leaveTripStatusMsg = null
                            onOpenSettleUpClick()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DesignSystemBindings.activePalette.primary,
                        contentColor = DesignSystemBindings.activePalette.onPrimary
                        )
                    ) {
                        Text(
                            text = "Settle up first",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.leaveGroup(context = context, groupId = groupId) { ok, msg ->
                                if (ok) {
                                    showLeaveTripConfirmDialog = false
                                    leaveTripStatusMsg = null
                                    onLeaveGroupSuccess()
                                } else {
                                    leaveTripStatusMsg = msg
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DesignSystemBindings.activePalette.secondary,
                            contentColor = DesignSystemBindings.activePalette.onSecondary
                        )
                    ) {
                        Text(
                            text = "Confirm Leave trip",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLeaveTripConfirmDialog = false
                        leaveTripStatusMsg = null
                    }
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = TripHubTokens.TextSecondary
                    )
                }
            }
        )
    }

    var selectedMemberForActions by remember { mutableStateOf<GroupMemberEntity?>(null) }

    selectedMemberForActions?.let { targetMember ->
        val isTargetMe = targetMember.isCurrentUser
        val isTargetOrganizer = organizerMembers.any { it.memberId == targetMember.memberId }
        val isTargetOnline = uiState.isMemberOnline(targetMember)
        val targetCleanPhone10 = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(
            targetMember.userPhone,
            targetMember.upiId
        )
        val targetFormattedPhone10 = if (targetCleanPhone10.length == 10) {
            "${targetCleanPhone10.substring(0, 5)} ${targetCleanPhone10.substring(5)}"
        } else {
            targetCleanPhone10
        }
        val targetPhoneSubtitle = if (targetCleanPhone10.length == 10) {
            "+91 $targetFormattedPhone10"
        } else {
            "No phone linked"
        }
        val canEditPhoneOrInvite = !isTargetMe &&
            (targetCleanPhone10.isEmpty() || targetMember.inviteStatus.equals("PENDING", ignoreCase = true) || isCurrentUserOrganizer)
        val canShareWhatsAppInvite = !isTargetMe && targetCleanPhone10.length == 10
        val canPromoteToOrganizer = isCurrentUserOrganizer && !isTargetMe && !isTargetOrganizer
        val canDismissOrganizer = isCurrentUserOrganizer && !isTargetMe && isTargetOrganizer && organizerMembers.size > 1
        val canRemoveTargetMember = isCurrentUserOrganizer && !isTargetMe && groupMembers.size > 1

        ModalBottomSheet(
            onDismissRequest = { selectedMemberForActions = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = TripHubTokens.CanvasBg,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = TripHubTokens.CardBorder) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        TripHubMemberAvatar(
                            seedOrName = targetMember.avatarSeed.ifBlank { targetMember.name },
                            fallbackName = targetMember.name,
                            size = 44.dp,
                            backgroundColor = TripHubTokens.SunkenWell,
                            textColor = TripHubTokens.TextPrimary,
                            fontSize = 15.sp,
                            isOnline = isTargetOnline
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = targetMember.name,
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = TripHubTokens.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                when {
                                    targetMember.inviteStatus.equals("DECLINED", ignoreCase = true) -> {
                                        Surface(
                                            shape = CircleShape,
                                            color = TripHubTokens.TerracottaPeachBg
                                        ) {
                                            Text(
                                                text = "Declined",
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TripHubTokens.TerracottaIconTint,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    targetMember.inviteStatus.equals("PENDING", ignoreCase = true) -> {
                                        Surface(
                                            shape = CircleShape,
                                            color = TripHubTokens.PeriwinkleBoxBg
                                        ) {
                                            Text(
                                                text = "Invite pending",
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TripHubTokens.PeriwinkleIconTint,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    isTargetOrganizer -> {
                                        Surface(
                                            shape = CircleShape,
                                            color = TripHubTokens.PositiveSagePillBg
                                        ) {
                                            Text(
                                                text = "Organizer",
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TripHubTokens.PositiveSageText,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = targetPhoneSubtitle,
                                style = MaterialTheme.typography.bodySmall.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Medium,
                                        fontFeatureSettings = "tnum"
                                    )
                                ),
                                color = TripHubTokens.TextSecondary
                            )
                        }
                    }
                }

                HorizontalDivider(color = TripHubTokens.CardBorder)

                if (canEditPhoneOrInvite) {
                    val isMissingPhone = targetCleanPhone10.isEmpty()
                    Surface(
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            selectedMemberForActions = null
                            editedMemberName = targetMember.name
                            editedMemberPhone = targetCleanPhone10
                            editPhoneDialogStatus = null
                            editingMemberForPhoneInvite = targetMember
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = TripHubTokens.CardSurface,
                        border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isMissingPhone) Icons.Rounded.PhoneIphone else Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = TripHubTokens.PositiveSageText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isMissingPhone) "Add phone and invite" else "Edit name & phone",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = TripHubTokens.TextPrimary
                            )
                        }
                    }
                }

                if (canShareWhatsAppInvite) {
                    Surface(
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            selectedMemberForActions = null
                            viewModel.sendOrResendDirectMemberInvite(
                                context = context,
                                groupId = groupId,
                                memberId = targetMember.memberId,
                                launchWhatsAppShare = true
                            ) { ok, msg ->
                                isPeopleFeedbackError = !ok
                                peopleFeedbackBanner = msg
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = TripHubTokens.PositiveSagePillBg,
                        border = BorderStroke(1.dp, TripHubTokens.PositiveSageText.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = null,
                                tint = TripHubTokens.PositiveSageText,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Share invite on WhatsApp",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TripHubTokens.PositiveSageText
                                )
                                Text(
                                    text = "Send trip code $formattedJoinCode & join link",
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TripHubTokens.PositiveSageText.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                if (canPromoteToOrganizer) {
                    Surface(
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            selectedMemberForActions = null
                            viewModel.promoteMemberToOrganizer(
                                context = context,
                                groupId = groupId,
                                targetMemberId = targetMember.memberId
                            ) { ok, msg ->
                                isPeopleFeedbackError = !ok
                                peopleFeedbackBanner = msg
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = TripHubTokens.CardSurface,
                        border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = TripHubTokens.PositiveSageText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Make Organizer",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = TripHubTokens.TextPrimary
                            )
                        }
                    }
                }

                if (canDismissOrganizer) {
                    Surface(
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            selectedMemberForActions = null
                            viewModel.dismissMemberAsOrganizer(
                                context = context,
                                groupId = groupId,
                                targetMemberId = targetMember.memberId
                            ) { ok, msg ->
                                isPeopleFeedbackError = !ok
                                peopleFeedbackBanner = msg
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = TripHubTokens.CardSurface,
                        border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonRemove,
                                contentDescription = null,
                                tint = TripHubTokens.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Dismiss as Organizer",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = TripHubTokens.TextSecondary
                            )
                        }
                    }
                }

                if (canRemoveTargetMember) {
                    Surface(
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            selectedMemberForActions = null
                            removeMemberStatusMsg = null
                            confirmingRemoveMember = targetMember
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = TripHubTokens.TerracottaPeachBg,
                        border = BorderStroke(1.dp, DesignSystemBindings.activePalette.secondary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonRemove,
                                contentDescription = null,
                                tint = TripHubTokens.TerracottaIconTint,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Remove member",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleSmall,
                                color = TripHubTokens.TerracottaIconTint
                            )
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item(key = "people_actions_row") {
            Column(
                modifier = Modifier.padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null),
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
                            text = if (isTravelGroup) "Add traveler" else "Add member",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
                            onOpenSyncSheetClick()
                        },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, TripHubTokens.CardBorder),
                        modifier = Modifier
                            .weight(1.25f)
                            .minimumInteractiveComponentSize()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            tint = TripHubTokens.TextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Share code $formattedJoinCode",
                            fontFamily = SplitMateTnumMonospace,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            color = TripHubTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                peopleFeedbackBanner?.let { banner ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPeopleFeedbackError) TripHubTokens.TerracottaPeachBg else TripHubTokens.PositiveSagePillBg,
                        border = BorderStroke(
                            1.dp,
                            if (isPeopleFeedbackError) DesignSystemBindings.activePalette.secondary.copy(alpha = 0.4f) else TripHubTokens.PositiveSageText.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPeopleFeedbackError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircleOutline,
                                contentDescription = null,
                                tint = if (isPeopleFeedbackError) TripHubTokens.TerracottaIconTint else TripHubTokens.PositiveSageText,
                                modifier = Modifier.padding(end = 8.dp).size(18.dp)
                            )
                            Text(
                                text = banner,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPeopleFeedbackError) TripHubTokens.TerracottaIconTint else TripHubTokens.PositiveSageText,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { peopleFeedbackBanner = null },
                                modifier = Modifier // v2.4.0 D2: default 48dp target
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Dismiss",
                                    tint = if (isPeopleFeedbackError) TripHubTokens.TerracottaIconTint else TripHubTokens.PositiveSageText,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // M3 Expressive Segmented Contained List (slim ~58dp rows, 20dp outer corners, 4dp inner corners, 2dp gap)
        itemsIndexed(
            items = groupMembers,
            key = { _, it -> it.memberId }
        ) { index, member ->
            val isMe = member.isCurrentUser
            val isMemberOrganizer = organizerMembers.any { it.memberId == member.memberId }
            val isOnline = uiState.isMemberOnline(member)
            val cleanPhone10 = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(member.userPhone, member.upiId)
            val formattedPhone10 = if (cleanPhone10.length == 10) {
                "${cleanPhone10.substring(0, 5)} ${cleanPhone10.substring(5)}"
            } else {
                cleanPhone10
            }
            val phoneSubtitle = if (cleanPhone10.length == 10) {
                "+91 $formattedPhone10"
            } else {
                "No phone linked"
            }
            val showQuickInviteAction = !isMe && (cleanPhone10.isEmpty() || member.inviteStatus.equals("PENDING", ignoreCase = true))
            val hasMemberActions = !isMe && (showQuickInviteAction || isCurrentUserOrganizer)
            val segmentedShape = rememberAnimatedSegmentedIslandItemShape(
                index = index,
                totalCount = groupMembers.size,
                isSelected = selectedMemberForActions?.memberId == member.memberId,
                outerCorner = 20.dp,
                innerCorner = 6.dp
            )

            Surface(
                onClick = {
                    if (!hasMemberActions) return@Surface
                    performCrispTactileHaptic(context, localView, heavy = false)
                    selectedMemberForActions = member
                },
                enabled = hasMemberActions,
                shape = segmentedShape,
                color = TripHubTokens.CardSurface,
                border = BorderStroke(
                    width = if (isMe) 1.2.dp else 1.dp,
                    color = if (isMe) BuckwheatOlivePrimary.copy(alpha = 0.55f) else TripHubTokens.CardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(fadeInSpec = null, placementSpec = SplitMateMotion.defaultSpatial(), fadeOutSpec = null)
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        TripHubMemberAvatar(
                            seedOrName = member.avatarSeed.ifBlank { member.name },
                            fallbackName = member.name,
                            size = 38.dp,
                            backgroundColor = if (isMe) TripHubTokens.PositiveSagePillBg else TripHubTokens.SunkenWell,
                            textColor = if (isMe) TripHubTokens.PositiveSageText else TripHubTokens.TextPrimary,
                            fontSize = 13.sp,
                            isOnline = isOnline
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FigtreeFontFamily, fontSize = 16.sp,
                                                color = TripHubTokens.TextPrimary
                                            )
                                        ) {
                                            append(member.name)
                                        }
                                        if (isMe) {
                                            withStyle(
                                                SpanStyle(
                                                    fontFamily = FigtreeFontFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    color = TripHubTokens.TextSecondary
                                                )
                                            ) {
                                                append(" (You)")
                                            }
                                        }
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                // Single-Badge Rule: at most 1 mutually-exclusive status pill per member row
                                when {
                                    !isMe && member.inviteStatus.equals("DECLINED", ignoreCase = true) -> {
                                        Surface(
                                            shape = CircleShape,
                                            color = TripHubTokens.TerracottaPeachBg
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.PersonRemove,
                                                    contentDescription = null,
                                                    tint = TripHubTokens.TerracottaIconTint,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = "Declined",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TripHubTokens.TerracottaIconTint
                                                )
                                            }
                                        }
                                    }
                                    !isMe && member.inviteStatus.equals("PENDING", ignoreCase = true) -> {
                                        Surface(
                                            shape = CircleShape,
                                            color = TripHubTokens.PeriwinkleBoxBg,
                                            border = BorderStroke(1.dp, TripHubTokens.PeriwinkleIconTint.copy(alpha = 0.25f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Schedule,
                                                    contentDescription = null,
                                                    tint = TripHubTokens.PeriwinkleIconTint,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = "Invite pending",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TripHubTokens.PeriwinkleIconTint
                                                )
                                            }
                                        }
                                    }
                                    isMemberOrganizer -> {
                                        Surface(
                                            shape = CircleShape,
                                            color = TripHubTokens.PositiveSagePillBg
                                        ) {
                                            Text(
                                                text = "Organizer",
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TripHubTokens.PositiveSageText,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = phoneSubtitle,
                                style = MaterialTheme.typography.labelSmall.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Medium,
                                        fontFeatureSettings = "tnum"
                                    )
                                ),
                                color = TripHubTokens.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (showQuickInviteAction) {
                            val isMissingPhone = cleanPhone10.isEmpty()
                            Surface(
                                onClick = {
                                    performCrispTactileHaptic(context, localView, heavy = false)
                                    if (isMissingPhone) {
                                        editedMemberName = member.name
                                        editedMemberPhone = cleanPhone10
                                        editPhoneDialogStatus = null
                                        editingMemberForPhoneInvite = member
                                    } else {
                                        viewModel.sendOrResendDirectMemberInvite(
                                            context = context,
                                            groupId = groupId,
                                            memberId = member.memberId,
                                            launchWhatsAppShare = true
                                        ) { ok, msg ->
                                            isPeopleFeedbackError = !ok
                                            peopleFeedbackBanner = msg
                                        }
                                    }
                                },
                                shape = CircleShape,
                                color = TripHubTokens.PositiveSagePillBg,
                                border = BorderStroke(1.dp, TripHubTokens.PositiveSageText.copy(alpha = 0.35f)),
                                modifier = Modifier.minimumInteractiveComponentSize().size(32.dp) // v2.4.0 D2: 48dp touch area
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isMissingPhone) Icons.Rounded.PersonAdd else Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = if (isMissingPhone) "Add phone and invite" else "Share invite on WhatsApp",
                                        tint = TripHubTokens.PositiveSageText,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                        if (hasMemberActions) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Member options",
                                tint = TripHubTokens.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        item(key = "people_leave_trip_footer") {
            OutlinedButton(
                onClick = {
                    performCrispTactileHaptic(context, localView, heavy = false)
                    leaveTripStatusMsg = null
                    showLeaveTripConfirmDialog = true
                },
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, DesignSystemBindings.activePalette.secondary.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = TripHubTokens.TerracottaPeachBg.copy(alpha = if (SplitMateTheme.isDark) 0.75f else 0.45f),
                    contentColor = TripHubTokens.TerracottaIconTint
                ),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                    contentDescription = null,
                    tint = TripHubTokens.TerracottaIconTint,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Leave trip",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelLarge,
                    color = TripHubTokens.TerracottaIconTint
                )
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
        scrimColor = DesignSystemBindings.activePalette.scrim.copy(alpha = 0.55f),
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
                text = "Choose a booking type or invite friends to your travel group.",
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                color = TripHubTokens.TextSecondary
            )

            val actions = listOf(
                Triple("Train ticket", Icons.Rounded.Train, onSelectTrainPnr),
                Triple("Flight", Icons.Rounded.FlightTakeoff, onSelectFlightPass),
                Triple("Stay, rental, cab or bill", Icons.AutoMirrored.Rounded.ReceiptLong, onSelectSharedExpense),
                Triple("Invite friends with the trip code", Icons.Rounded.PersonAdd, onSelectSyncAndPerspective)
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
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
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
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    color = TripHubTokens.TextSecondary
                )
                if (!snapshot?.coachPositionHint.isNullOrBlank()) {
                    Text(
                        text = snapshot?.coachPositionHint.orEmpty(),
                        style = MaterialTheme.typography.labelMedium.merge(
                            TextStyle(
                                fontFamily = SplitMateTnumMonospace,
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            )
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
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelLarge,
                                color = TripHubTokens.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${cell.berthCode} ${cell.statusCode}",
                                style = MaterialTheme.typography.labelMedium.merge(
                                    TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    )
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
                    containerColor = DesignSystemBindings.activePalette.primary,
                        contentColor = DesignSystemBindings.activePalette.onPrimary
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
