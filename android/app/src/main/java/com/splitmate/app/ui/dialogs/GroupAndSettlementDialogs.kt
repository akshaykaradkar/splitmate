package com.splitmate.app

import com.splitmate.app.ui.theme.HubExtendedColors
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.ParsedTravelTicket
import com.splitmate.app.ui.buildDiceBearOpenPeepsUrl
import com.splitmate.app.ui.extractInitialsFromNameOrSeed
import com.splitmate.app.ui.components.ExpressiveTooltipIconButton
import com.splitmate.app.ui.components.MaterialShapes
import com.splitmate.app.ui.components.SplitMateMotion
import com.splitmate.app.ui.components.toShape
import com.splitmate.app.ui.extractTravelTicketFromTitle
import com.splitmate.app.ui.formatTravelExpenseTitle
import com.splitmate.app.ui.category.CustomExpenseCategoryStore
import com.splitmate.app.ui.category.ExpenseCategoryIcons
import com.splitmate.app.ui.category.ExpenseCategoryPickerSheet
import com.splitmate.app.ui.category.MoreCategoriesChip
import androidx.compose.runtime.collectAsState
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditLoggedExpenseDialog(
    expense: ExpenseEntity,
    groupMembers: List<GroupMemberEntity>,
    initialSplitMemberIds: Set<String> = groupMembers.map { it.memberId }.toSet(),
    onDismiss: () -> Unit,
    onSave: (String, Double, String, List<String>, String?) -> Unit,
    /**
     * v2.3.5 (#4 guard a): true for itemized receipts. Amount and split chips are locked so the
     * edit cannot flatten tax / tip / item claims into an equal split; title + payer stay editable.
     */
    lockAmountAndSplit: Boolean = false,
    onSaveExact: ((
        newTitle: String,
        exactTotalCents: Long,
        newPayerId: String,
        updatedSplitMemberIds: List<String>,
        newCategoryRef: String?,
        explicitTravelPnr: String?
    ) -> Unit)? = null
) {
    val existingTicket = remember(expense.title) { extractTravelTicketFromTitle(expense.title) }
    var editedTitle by remember(expense.title) {
        mutableStateOf(existingTicket?.cleanTitle ?: expense.title)
    }
    var editedCategoryRef by remember(expense.categoryRef) {
        mutableStateOf(expense.categoryRef)
    }
    var editedAmountStr by remember(expense.totalAmountCents) {
        val whole = expense.totalAmountCents / 100L
        val rem = kotlin.math.abs(expense.totalAmountCents % 100L)
        mutableStateOf(if (rem == 0L) whole.toString() else String.format(Locale.US, "%d.%02d", whole, rem))
    }
    var selectedPayerId by remember(expense.payerId) {
        mutableStateOf(expense.payerId)
    }
    var selectedSplitMemberIds by remember(expense.expenseId, initialSplitMemberIds) {
        mutableStateOf(initialSplitMemberIds.ifEmpty { groupMembers.map { it.memberId }.toSet() })
    }
    var includeTravelTicket by remember(existingTicket, expense.travelPnr) {
        mutableStateOf(existingTicket != null || expense.travelPnr.isNotBlank())
    }
    var pnrNumber by remember(existingTicket, expense.travelPnr) {
        mutableStateOf(existingTicket?.pnr?.ifBlank { expense.travelPnr } ?: expense.travelPnr)
    }
    var trainOrFlightNo by remember(existingTicket) { mutableStateOf(existingTicket?.trainOrFlightNo ?: "") }
    var routeFromTo by remember(existingTicket) { mutableStateOf(existingTicket?.route ?: "") }
    var departureDateInput by remember(existingTicket) { mutableStateOf(existingTicket?.departureDate ?: "") }
    var departureTimeInput by remember(existingTicket) { mutableStateOf(existingTicket?.departureTime ?: "") }
    var coachAndSeats by remember(existingTicket) { mutableStateOf(existingTicket?.coachAndSeats ?: "") }
    var showDepartureDatePicker by remember { mutableStateOf(false) }
    var showDepartureTimePicker by remember { mutableStateOf(false) }
    var timePickerDisplayMode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }

    val quickCategories = listOf(
        Triple("Train / PNR", Icons.Rounded.Train, true),
        Triple("Dinner & Food", Icons.Rounded.Restaurant, false),
        Triple("Cab & Auto", Icons.Rounded.LocalTaxi, false),
        Triple("Hotel & Stay", Icons.Rounded.Hotel, false),
        Triple("Groceries", Icons.Rounded.ShoppingCart, false),
        Triple("Drinks & Outing", Icons.Rounded.LocalBar, false)
    )

    // v2.3.4: full category catalog + user-created categories (title-based; existing data untouched).
    val customCategories by CustomExpenseCategoryStore.categories.collectAsState()
    var showCategorySheet by remember { mutableStateOf(false) }
    if (showCategorySheet) {
        ExpenseCategoryPickerSheet(
            selectedTitle = editedTitle,
            showTrainPnr = false,
            onDismiss = { showCategorySheet = false },
            onSelect = { cat ->
                showCategorySheet = false
                editedTitle = cat.title
                editedCategoryRef = com.splitmate.app.ui.category.ExpenseCategoryRefs.refFor(cat)
            }
        )
    }

    if (showDepartureDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDepartureDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            departureDateInput = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))
                        }
                        showDepartureDatePicker = false
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDepartureDatePicker = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("Cancel")
                }
            },
            colors = androidx.compose.material3.DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            DatePicker(state = datePickerState, showModeToggle = true)
        }
    }

    if (showDepartureTimePicker) {
        val timePickerState = rememberTimePickerState(is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { showDepartureTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        departureTimeInput = String.format(Locale.US, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                        showDepartureTimePicker = false
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDepartureTimePicker = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("Cancel")
                }
            },
            title = {
                Text(
                    if (timePickerDisplayMode == TimePickerDisplayMode.Picker) "Select departure time" else "Enter departure time"
                )
            },
            modeToggleButton = {
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = {
                        timePickerDisplayMode = if (timePickerDisplayMode == TimePickerDisplayMode.Picker) {
                            TimePickerDisplayMode.Input
                        } else {
                            TimePickerDisplayMode.Picker
                        }
                    },
                    displayMode = timePickerDisplayMode
                )
            }
        ) {
            if (timePickerDisplayMode == TimePickerDisplayMode.Picker) {
                TimePicker(state = timePickerState)
            } else {
                TimeInput(state = timePickerState)
            }
        }
    }

    val performSave: () -> Unit = {
        val exactCents = if (lockAmountAndSplit) {
            expense.totalAmountCents
        } else {
            com.splitmate.app.ui.parseRupeesToPaiseExact(editedAmountStr) ?: expense.totalAmountCents
        }
        val parsedRupees = exactCents / 100.0
        val splitIdsToSave = if (lockAmountAndSplit) initialSplitMemberIds.toList() else selectedSplitMemberIds.toList()
        val cleanPnrTrim = pnrNumber.trim().uppercase(Locale.US)
        val explicitTravelPnrOverride: String = if (includeTravelTicket) cleanPnrTrim else ""
        val finalTitle = if (includeTravelTicket &&
            (cleanPnrTrim.isNotBlank() ||
                trainOrFlightNo.isNotBlank() ||
                coachAndSeats.isNotBlank() ||
                departureDateInput.isNotBlank() ||
                departureTimeInput.isNotBlank())
        ) {
            val routeParts = routeFromTo
                .split(Regex("""\s*(?:->|→|-|\bto\b)\s*""", RegexOption.IGNORE_CASE), limit = 2)
                .map { it.trim() }
            val parsedFrom = routeParts.getOrNull(0).orEmpty()
            val parsedTo = routeParts.getOrNull(1).orEmpty()
            val baseMode = if (cleanPnrTrim.length == 6 && cleanPnrTrim.any { it.isLetter() }) "Flight" else "Train / PNR Ticket"
            val parsedTicket = ParsedTravelTicket(
                pnr = cleanPnrTrim,
                trainOrFlightNo = trainOrFlightNo.trim(),
                fromStation = parsedFrom,
                toStation = parsedTo,
                departureDate = departureDateInput.trim(),
                departureTime = departureTimeInput.trim(),
                coachAndSeats = coachAndSeats.trim(),
                cleanTitle = editedTitle.trim().ifBlank { baseMode }
            )
            formatTravelExpenseTitle(editedTitle.trim().ifBlank { baseMode }, parsedTicket)
        } else {
            editedTitle.trim().ifBlank { "Group Expense" }
        }
        if (onSaveExact != null) {
            onSaveExact(
                finalTitle,
                exactCents,
                selectedPayerId,
                splitIdsToSave,
                editedCategoryRef,
                explicitTravelPnrOverride
            )
        } else {
            onSave(finalTitle, parsedRupees, selectedPayerId, splitIdsToSave, editedCategoryRef)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Edit expense",
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = SplitMateTheme.FontDisplay,
                                fontWeight = FontWeight.ExtraBold,
                                color = SplitMateTheme.PrimaryDark,
                                modifier = Modifier.semantics { heading() }
                            )
                        },
                        navigationIcon = {
                            ExpressiveTooltipIconButton(
                                tooltipText = "Close",
                                onClick = onDismiss
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = SplitMateTheme.PrimaryDark
                                )
                            }
                        },
                        actions = {
                            TextButton(
                                onClick = performSave,
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(
                                    text = "Save",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SplitMateTheme.SageText
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                }
            ) { innerPadding ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .imePadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                Text(
                    text = "Quick Category Switch:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SplitMateTheme.TextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickCategories) { (cat, catIcon, isTrainCat) ->
                        val isSelected = editedTitle.equals(cat, ignoreCase = true)
                        // v2.4.0 rc3: official M3 Expressive FilterChip (selected semantics, press morph).
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                editedTitle = cat
                                editedCategoryRef = null
                                if (isTrainCat) includeTravelTicket = true
                            },
                            shapes = FilterChipDefaults.shapes(shape = SplitMateTheme.RadiusBadge, selectedShape = SplitMateTheme.RadiusBadge),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SplitMateTheme.SurfaceMuted,
                                selectedContainerColor = SplitMateTheme.PrimaryDark
                            ),
                            border = null,
                            leadingIcon = {
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = cat,
                                    tint = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark,
                                    modifier = Modifier.size(13.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark
                                )
                            }
                        )
                    }
                    // v2.3.4: the user's own categories + "More" (full catalog / create new).
                    items(customCategories, key = { "edit_custom_${it.title}" }) { cat ->
                        val isSelected = editedTitle.equals(cat.title, ignoreCase = true)
                        // v2.4.0 rc3: official M3 Expressive FilterChip (selected semantics, press morph).
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                editedTitle = cat.title
                                editedCategoryRef = com.splitmate.app.ui.category.ExpenseCategoryRefs.refFor(cat)
                            },
                            shapes = FilterChipDefaults.shapes(shape = SplitMateTheme.RadiusBadge, selectedShape = SplitMateTheme.RadiusBadge),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SplitMateTheme.SurfaceMuted,
                                selectedContainerColor = SplitMateTheme.PrimaryDark
                            ),
                            border = null,
                            leadingIcon = {
                                Icon(
                                    imageVector = ExpenseCategoryIcons.forKey(cat.iconKey),
                                    contentDescription = null,
                                    tint = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark,
                                    modifier = Modifier.size(13.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = cat.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark
                                )
                            }
                        )
                    }
                    item(key = "edit_more_categories") {
                        MoreCategoriesChip(onClick = { showCategorySheet = true })
                    }
                }

                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it; editedCategoryRef = null },
                    label = { Text("Expense Title / Category", fontSize = 12.sp) },
                    singleLine = true,
                    shape = SplitMateTheme.RadiusPanel,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editedAmountStr,
                    onValueChange = { editedAmountStr = com.splitmate.app.ui.sanitizeDecimalRupeesInput(it) },
                    label = { Text("Total Amount (₹)", fontSize = 12.sp) },
                    singleLine = true,
                    enabled = !lockAmountAndSplit,
                    shape = SplitMateTheme.RadiusPanel,
                    modifier = Modifier.fillMaxWidth()
                )

                if (lockAmountAndSplit) {
                    Surface(
                        color = SplitMateTheme.TerracottaSurface,
                        shape = SplitMateTheme.RadiusPanel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = ItemizedExpenseGuard.LOCKED_REASON,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SplitMateTheme.TerracottaText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }

                if (groupMembers.isNotEmpty()) {
                    Text(
                        text = "Paid By:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SplitMateTheme.TextSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(groupMembers, key = { it.memberId }) { mbr ->
                            val isSelected = mbr.memberId == selectedPayerId
                            // v2.4.0 rc3: official M3 Expressive FilterChip (selected semantics, press morph).
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPayerId = mbr.memberId },
                                shapes = FilterChipDefaults.shapes(shape = SplitMateTheme.RadiusBadge, selectedShape = SplitMateTheme.RadiusBadge),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SplitMateTheme.SurfaceMuted,
                                    selectedContainerColor = SplitMateTheme.PrimaryDark
                                ),
                                border = null,
                                label = {
                                    Text(
                                        text = if (mbr.isCurrentUser) "${mbr.name} (You)" else mbr.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark
                                    )
                                }
                            )
                        }
                    }

                    if (!lockAmountAndSplit) {
                    Text(
                        text = "Split Equally Among (${selectedSplitMemberIds.size} of ${groupMembers.size} selected):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SplitMateTheme.TextSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(groupMembers, key = { "split_${it.memberId}" }) { mbr ->
                            val isIncluded = selectedSplitMemberIds.contains(mbr.memberId)
                            // v2.4.0 rc3: official M3 Expressive FilterChip (selected semantics, press morph).
                            FilterChip(
                                selected = isIncluded,
                                onClick = {
                                    selectedSplitMemberIds = if (isIncluded && selectedSplitMemberIds.size > 1) {
                                        selectedSplitMemberIds - mbr.memberId
                                    } else {
                                        selectedSplitMemberIds + mbr.memberId
                                    }
                                },
                                shapes = FilterChipDefaults.shapes(shape = SplitMateTheme.RadiusBadge, selectedShape = SplitMateTheme.RadiusBadge),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SplitMateTheme.SurfaceMuted,
                                    selectedContainerColor = SplitMateTheme.SageSurface
                                ),
                                border = null,
                                leadingIcon = if (isIncluded) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = SplitMateTheme.SageText,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                } else {
                                    null
                                },
                                label = {
                                    Text(
                                        text = if (mbr.isCurrentUser) "${mbr.name} (You)" else mbr.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isIncluded) SplitMateTheme.SageText else SplitMateTheme.TextSecondary
                                    )
                                }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Train,
                            contentDescription = null,
                            tint = SplitMateTheme.PrimaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Attach / Edit Train PNR & Berths",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitMateTheme.PrimaryDark
                        )
                    }
                    Switch(
                        checked = includeTravelTicket,
                        onCheckedChange = { includeTravelTicket = it }
                    )
                }

                if (includeTravelTicket) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = pnrNumber,
                            onValueChange = {
                                pnrNumber = it.filter { ch -> ch.isLetterOrDigit() }.uppercase(Locale.US).take(10)
                            },
                            label = { Text("Train (10) / Flight (6) PNR", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = trainOrFlightNo,
                            onValueChange = { trainOrFlightNo = it },
                            label = { Text("Train / Flight #", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = routeFromTo,
                            onValueChange = { routeFromTo = it },
                            label = { Text("From - To", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = coachAndSeats,
                            onValueChange = { coachAndSeats = it },
                            label = { Text("Coach / Berths", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = departureDateInput,
                            onValueChange = { departureDateInput = it },
                            label = { Text("Departure Date", style = MaterialTheme.typography.bodySmall) },
                            trailingIcon = {
                                ExpressiveTooltipIconButton(
                                    tooltipText = "Pick departure date",
                                    onClick = { showDepartureDatePicker = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = "Pick departure date",
                                        tint = SplitMateTheme.SageText
                                    )
                                }
                            },
                            singleLine = true,
                            shape = SplitMateTheme.RadiusPanel,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = departureTimeInput,
                            onValueChange = { departureTimeInput = it },
                            label = { Text("Departure Time", style = MaterialTheme.typography.bodySmall) },
                            trailingIcon = {
                                ExpressiveTooltipIconButton(
                                    tooltipText = "Pick departure time",
                                    onClick = { showDepartureTimePicker = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = "Pick departure time",
                                        tint = SplitMateTheme.SageText
                                    )
                                }
                            },
                            singleLine = true,
                            shape = SplitMateTheme.RadiusPanel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = SplitMateTheme.TextSecondary
                        )
                    }
                    Button(
                        onClick = performSave,
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SplitMateTheme.PrimaryDark,
                            contentColor = SplitMateTheme.ScreenBg
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "Save Changes",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
fun AvatarToken(
    initials: String,
    bg: Color,
    textColor: Color,
    size: Int = 36,
    isOnline: Boolean = false
) {
    val context = LocalContext.current
    val parsedDescriptor = remember(initials) {
        com.splitmate.app.ui.AvatarSeedCodec.parse(initials)
    }
    val presetBg = remember(initials, parsedDescriptor.colorPresetId, bg) {
        if (initials.contains('|')) {
            com.splitmate.app.ui.SplitMateAvatarColorPresets
                .find { it.id.equals(parsedDescriptor.colorPresetId, ignoreCase = true) }
                ?.primaryBgColor ?: bg
        } else {
            bg
        }
    }
    val diceBearSvgUrl = remember(initials) {
        buildDiceBearOpenPeepsUrl(initials)
    }
    val cleanInitials = remember(initials) {
        extractInitialsFromNameOrSeed(initials)
    }

    // M3 Expressive avatar geometry: 9-sided cookie instead of a plain circle.
    val avatarShape = remember { MaterialShapes.Cookie9Sided.toShape() }

    Box(modifier = Modifier.size(size.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(avatarShape)
                .background(presetBg)
                .border(2.dp, SplitMateTheme.SurfaceWhite, avatarShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cleanInitials,
                fontFamily = SplitMateTheme.FontDisplay,
                fontSize = (size * 0.34f).sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(diceBearSvgUrl)
                    .decoderFactory(SvgDecoder.Factory())
                    .diskCacheKey("dicebear_avatar_$diceBearSvgUrl")
                    .memoryCacheKey("dicebear_avatar_$diceBearSvgUrl")
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(avatarShape)
            )
        }
        if (isOnline) {
            val dotSize = if (size >= 36) 11.dp else 9.dp
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(com.splitmate.app.ui.theme.HubExtendedColors.presence().color)
                    .border(1.5.dp, SplitMateTheme.SurfaceWhite, CircleShape)
            )
        }
    }
}

@Composable
fun OverlappingAvatarStack(
    avatars: List<String>,
    remainingCount: Int = 0,
    onlineFlags: List<Boolean> = emptyList()
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        avatars.forEachIndexed { index, seed ->
            Box(modifier = Modifier.offset(x = (-index * 8).dp)) {
                AvatarToken(
                    initials = seed,
                    bg = com.splitmate.app.ui.DesignSystemBindings.activePalette.surfaceContainerHighest,
                    textColor = SplitMateTheme.PrimaryDark,
                    size = 28,
                    isOnline = onlineFlags.getOrNull(index) == true
                )
            }
        }
        if (remainingCount > 0) {
            val overflowBadgeShape = remember { MaterialShapes.Cookie9Sided.toShape() }
            Box(
                modifier = Modifier
                    .offset(x = (-avatars.size * 8).dp)
                    .size(28.dp)
                    .clip(overflowBadgeShape)
                    .background(SplitMateTheme.SurfaceMuted)
                    .border(2.dp, SplitMateTheme.SurfaceWhite, overflowBadgeShape),
                contentAlignment = Alignment.Center
            ) {
                Text("+$remainingCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SplitMateTheme.PrimaryDark)
            }
        }
    }
}

@Composable
fun ActivityItemRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color = SplitMateTheme.PrimaryDark,
    title: String,
    subtitle: String,
    amount: String,
    isPositive: Boolean
) {
    Card(
        shape = SplitMateTheme.RadiusCard,
        colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
            .animateContentSize(animationSpec = SplitMateMotion.defaultSpatial())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontFamily = SplitMateTheme.FontDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SplitMateTheme.PrimaryDark,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontFamily = SplitMateTheme.FontRounded,
                        fontSize = 12.sp,
                        color = SplitMateTheme.TextSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = amount,
                fontFamily = SplitMateTheme.FontDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                color = if (isPositive) SplitMateTheme.SageText else SplitMateTheme.TerracottaText
            )
        }
    }
}

