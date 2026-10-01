package com.splitmate.app.ui.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AirportShuttle
import androidx.compose.material.icons.rounded.Attractions
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CarRental
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.DinnerDining
import androidx.compose.material.icons.rounded.DirectionsBoat
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.ElectricRickshaw
import androidx.compose.material.icons.rounded.ElectricScooter
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.FreeBreakfast
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.House
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material.icons.rounded.Kayaking
import androidx.compose.material.icons.rounded.Liquor
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Museum
import androidx.compose.material.icons.rounded.Paragliding
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PedalBike
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.RamenDining
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ScubaDiving
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Soap
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Subway
import androidx.compose.material.icons.rounded.Surfing
import androidx.compose.material.icons.rounded.TempleHindu
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material.icons.rounded.Tour
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.ui.SplitMateBrandFontFamily
import com.splitmate.app.ui.SplitMateDisplayFontFamily
import com.splitmate.app.SplitMateTheme

// ==============================================================================
// Icon + tone resolution
// ==============================================================================

object ExpenseCategoryIcons {
    fun forKey(key: String): ImageVector = when (key) {
        "restaurant" -> Icons.Rounded.Restaurant
        "breakfast" -> Icons.Rounded.FreeBreakfast
        "lunch" -> Icons.Rounded.LunchDining
        "dinner" -> Icons.Rounded.DinnerDining
        "cafe" -> Icons.Rounded.LocalCafe
        "coffee" -> Icons.Rounded.Coffee
        "ramen" -> Icons.Rounded.RamenDining
        "pizza" -> Icons.Rounded.LocalPizza
        "bakery" -> Icons.Rounded.BakeryDining
        "icecream" -> Icons.Rounded.Icecream
        "fastfood" -> Icons.Rounded.Fastfood
        "water" -> Icons.Rounded.LocalDrink
        "bar" -> Icons.Rounded.LocalBar
        "liquor" -> Icons.Rounded.Liquor
        "train" -> Icons.Rounded.Train
        "flight" -> Icons.Rounded.Flight
        "taxi" -> Icons.Rounded.LocalTaxi
        "auto_rickshaw" -> Icons.Rounded.ElectricRickshaw
        "two_wheeler" -> Icons.Rounded.TwoWheeler
        "scooter" -> Icons.Rounded.ElectricScooter
        "bicycle" -> Icons.Rounded.PedalBike
        "car_rental" -> Icons.Rounded.CarRental
        "fuel" -> Icons.Rounded.LocalGasStation
        "parking" -> Icons.Rounded.LocalParking
        "toll" -> Icons.Rounded.Toll
        "bus" -> Icons.Rounded.DirectionsBus
        "metro" -> Icons.Rounded.Subway
        "boat" -> Icons.Rounded.DirectionsBoat
        "shuttle" -> Icons.Rounded.AirportShuttle
        "walk" -> Icons.AutoMirrored.Rounded.DirectionsWalk
        "hotel" -> Icons.Rounded.Hotel
        "house" -> Icons.Rounded.House
        "cottage" -> Icons.Rounded.Cottage
        "bed" -> Icons.Rounded.Bed
        "camping" -> Icons.Rounded.Park
        "ticket" -> Icons.Rounded.ConfirmationNumber
        "attractions" -> Icons.Rounded.Attractions
        "tour" -> Icons.Rounded.Tour
        "museum" -> Icons.Rounded.Museum
        "temple" -> Icons.Rounded.TempleHindu
        "hiking" -> Icons.Rounded.Hiking
        "kayaking" -> Icons.Rounded.Kayaking
        "paragliding" -> Icons.Rounded.Paragliding
        "surfing" -> Icons.Rounded.Surfing
        "scuba" -> Icons.Rounded.ScubaDiving
        "spa" -> Icons.Rounded.Spa
        "movie" -> Icons.Rounded.Movie
        "beach" -> Icons.Rounded.BeachAccess
        "camera" -> Icons.Rounded.PhotoCamera
        "celebration" -> Icons.Rounded.Celebration
        "sports" -> Icons.Rounded.SportsSoccer
        "groceries" -> Icons.Rounded.ShoppingCart
        "shopping" -> Icons.Rounded.ShoppingBag
        "gift" -> Icons.Rounded.CardGiftcard
        "pharmacy" -> Icons.Rounded.LocalPharmacy
        "hospital" -> Icons.Rounded.LocalHospital
        "soap" -> Icons.Rounded.Soap
        "sim" -> Icons.Rounded.SimCard
        "wifi" -> Icons.Rounded.Wifi
        "laundry" -> Icons.Rounded.LocalLaundryService
        "luggage" -> Icons.Rounded.Luggage
        "tips" -> Icons.Rounded.VolunteerActivism
        "atm" -> Icons.Rounded.LocalAtm
        "payments" -> Icons.Rounded.Payments
        "savings" -> Icons.Rounded.Savings
        "work" -> Icons.Rounded.Work
        "school" -> Icons.Rounded.School
        "pets" -> Icons.Rounded.Pets
        "more" -> Icons.Rounded.MoreHoriz
        else -> Icons.AutoMirrored.Rounded.ReceiptLong
    }

    /** Buckwheat badge pair (container, content) — same pairs used by the legacy keyword badge engine. */
    fun toneColors(tone: ExpenseCategoryTone, isDark: Boolean): Pair<Color, Color> = when (tone) {
        ExpenseCategoryTone.SAGE ->
            if (isDark) Color(0xFF283A18) to Color(0xFFD7E8B6) else Color(0xFFDCE9B9) to Color(0xFF365314)
        ExpenseCategoryTone.PERIWINKLE ->
            if (isDark) Color(0xFF222A4A) to Color(0xFFC7D2FE) else Color(0xFFE0E7FF) to Color(0xFF3730A3)
        ExpenseCategoryTone.AMBER ->
            if (isDark) Color(0xFF3D2E14) to Color(0xFFFDE68A) else Color(0xFFFEF3C7) to Color(0xFF92400E)
        ExpenseCategoryTone.MINT ->
            if (isDark) Color(0xFF1F3833) to Color(0xFFA7F3D0) else Color(0xFFD1FAE5) to Color(0xFF065F46)
        ExpenseCategoryTone.ROSE ->
            if (isDark) Color(0xFF3B1D2E) to Color(0xFFFBCFE8) else Color(0xFFFCE7F3) to Color(0xFF9D174D)
        ExpenseCategoryTone.PEACH ->
            if (isDark) Color(0xFF3D231B) to Color(0xFFFED8C8) else Color(0xFFFCE3D7) to Color(0xFF7C2D12)
    }
}

/** Exact-title lookup used by the global icon/badge resolvers (null = fall back to keyword engine). */
fun exactExpenseCategoryFor(title: String): ExpenseCategory? =
    ExpenseCategoryCatalog.exactMatch(title, CustomExpenseCategoryStore.current)

// ==============================================================================
// Reusable chip
// ==============================================================================

@Composable
fun ExpenseCategoryChip(
    category: ExpenseCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = category.title,
    trailing: (@Composable () -> Unit)? = null
) {
    val isDark = SplitMateTheme.isDark
    val (toneBg, toneFg) = ExpenseCategoryIcons.toneColors(category.tone, isDark)
    val selectedBg = SplitMateTheme.PrimaryDark
    val selectedFg = SplitMateTheme.SurfaceWhite
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(if (selected) 12.dp else 20.dp),
        color = if (selected) selectedBg else SplitMateTheme.SurfaceWhite,
        border = BorderStroke(1.dp, if (selected) selectedBg else SplitMateTheme.BorderLight),
        modifier = modifier
            .heightIn(min = 40.dp)
            .semantics {
                this.selected = selected
                role = Role.Button
            }
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = if (trailing != null) 4.dp else 12.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(if (selected) selectedFg.copy(alpha = 0.16f) else toneBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ExpenseCategoryIcons.forKey(category.iconKey),
                    contentDescription = null,
                    tint = if (selected) selectedFg else toneFg,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = label,
                fontFamily = SplitMateBrandFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) selectedFg else SplitMateTheme.PrimaryDark
            )
            if (trailing != null) {
                Spacer(modifier = Modifier.width(2.dp))
                trailing()
            }
        }
    }
}

// ==============================================================================
// "All categories" bottom sheet
// ==============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseCategoryPickerSheet(
    selectedTitle: String,
    onDismiss: () -> Unit,
    onSelect: (ExpenseCategory) -> Unit,
    showTrainPnr: Boolean = true
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { CustomExpenseCategoryStore.ensureLoaded(context) }
    val custom by CustomExpenseCategoryStore.categories.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ExpenseCategory?>(null) }

    val visible = remember(query, custom, showTrainPnr) {
        ExpenseCategoryCatalog.search(query, custom).filter { showTrainPnr || !it.opensPnrFlow }
    }
    val grouped = remember(visible) {
        val byGroup = visible.groupBy { it.group }
        // "Your Categories" first, then the built-in groups in declaration order.
        (listOf(ExpenseCategoryGroup.CUSTOM) + ExpenseCategoryGroup.values().filter { it != ExpenseCategoryGroup.CUSTOM })
            .mapNotNull { g -> byGroup[g]?.let { g to it } }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SplitMateTheme.ScreenBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "All categories",
                fontFamily = SplitMateDisplayFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = SplitMateTheme.PrimaryDark
            )
            Text(
                text = "Pick one for this expense, or create your own with an icon",
                fontFamily = SplitMateBrandFontFamily,
                fontSize = 12.sp,
                color = SplitMateTheme.TextSecondary
            )
            Spacer(modifier = Modifier.padding(top = 12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("Search breakfast, auto, fuel…", fontFamily = SplitMateBrandFontFamily) },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SplitMateTheme.PrimaryDark,
                    unfocusedBorderColor = SplitMateTheme.BorderLight,
                    focusedContainerColor = SplitMateTheme.SurfaceWhite,
                    unfocusedContainerColor = SplitMateTheme.SurfaceWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(top = 8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // "Your Categories" is always first: the create chip, then the user's own categories.
                item(key = "custom_section") {
                    val customVisible = grouped.firstOrNull { it.first == ExpenseCategoryGroup.CUSTOM }?.second.orEmpty()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(ExpenseCategoryGroup.CUSTOM.label)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CreateCategoryChip(
                                label = if (query.isNotBlank() && visible.none { it.title.equals(query.trim(), true) })
                                    "Create \"${ExpenseCategoryCatalog.sanitizeTitle(query)}\""
                                else "New category",
                                onClick = { showCreate = true }
                            )
                            customVisible.forEach { cat ->
                                ExpenseCategoryChip(
                                    category = cat,
                                    selected = cat.title.equals(selectedTitle.trim(), ignoreCase = true),
                                    onClick = { onSelect(cat) },
                                    trailing = {
                                        Surface(
                                            onClick = { pendingDelete = cat },
                                            shape = CircleShape,
                                            color = Color.Transparent,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .semantics { contentDescription = "Remove ${cat.title}" }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Rounded.Close,
                                                    contentDescription = null,
                                                    tint = SplitMateTheme.TextSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                items(grouped.filter { it.first != ExpenseCategoryGroup.CUSTOM }, key = { it.first.name }) { (group, cats) ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(group.label)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cats.forEach { cat ->
                                ExpenseCategoryChip(
                                    category = cat,
                                    selected = cat.title.equals(selectedTitle.trim(), ignoreCase = true),
                                    onClick = { onSelect(cat) }
                                )
                            }
                        }
                    }
                }
                if (visible.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = "No category matches \"${query.trim()}\". Create it above.",
                            fontFamily = SplitMateBrandFontFamily,
                            fontSize = 12.sp,
                            color = SplitMateTheme.TextSecondary
                        )
                    }
                }
                item(key = "bottom_space") { Spacer(modifier = Modifier.padding(bottom = 16.dp)) }
            }
        }
    }

    if (showCreate) {
        CreateExpenseCategoryDialog(
            initialName = query.trim(),
            onDismiss = { showCreate = false },
            onCreated = { created ->
                showCreate = false
                query = ""
                onSelect(created)
            }
        )
    }

    pendingDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = SplitMateTheme.SurfaceWhite,
            shape = SplitMateTheme.RadiusDialog,
            title = {
                Text(
                    "Remove \"${cat.title}\"?",
                    fontFamily = SplitMateDisplayFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = SplitMateTheme.PrimaryDark
                )
            },
            text = {
                Text(
                    "It disappears from your category list only. Expenses already logged with it stay exactly as they are.",
                    fontFamily = SplitMateBrandFontFamily,
                    fontSize = 13.sp,
                    color = SplitMateTheme.TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    CustomExpenseCategoryStore.remove(context, cat.title)
                    pendingDelete = null
                }) { Text("Remove", fontWeight = FontWeight.Bold, color = SplitMateTheme.TerracottaText) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Keep", fontWeight = FontWeight.Bold, color = SplitMateTheme.PrimaryDark)
                }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontFamily = SplitMateBrandFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        color = SplitMateTheme.TextSecondary
    )
}

@Composable
private fun CreateCategoryChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = SplitMateTheme.SageSurface,
        border = BorderStroke(1.dp, SplitMateTheme.AccentSage),
        modifier = Modifier.heightIn(min = 40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = SplitMateTheme.SageText, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontFamily = SplitMateBrandFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = SplitMateTheme.SageText
            )
        }
    }
}

/** Compact "More" chip used at the end of the existing quick-category rows. */
@Composable
fun MoreCategoriesChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = SplitMateTheme.SurfaceWhite,
        border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
        modifier = modifier.heightIn(min = 32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.MoreHoriz,
                contentDescription = null,
                tint = SplitMateTheme.PrimaryDark,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "More",
                fontFamily = SplitMateBrandFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SplitMateTheme.PrimaryDark
            )
        }
    }
}

// ==============================================================================
// Create-category dialog (name + icon grid + live preview)
// ==============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateExpenseCategoryDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onCreated: (ExpenseCategory) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(ExpenseCategoryCatalog.sanitizeTitle(initialName)) }
    var iconKey by remember { mutableStateOf(ExpenseCategoryCatalog.suggestIconKey(initialName)) }
    var iconPickedManually by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val preview = ExpenseCategoryCatalog.custom(name.ifBlank { "Your category" }, iconKey)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SplitMateTheme.SurfaceWhite,
        shape = SplitMateTheme.RadiusDialog,
        title = {
            Column {
                Text(
                    "New category",
                    fontFamily = SplitMateDisplayFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = SplitMateTheme.PrimaryDark
                )
                Text(
                    "Name it and pick an icon. It's saved on this phone for all your trips.",
                    fontFamily = SplitMateBrandFontFamily,
                    fontSize = 12.sp,
                    color = SplitMateTheme.TextSecondary
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { raw ->
                        name = raw.take(ExpenseCategoryCatalog.MAX_TITLE_LENGTH)
                        error = null
                        if (!iconPickedManually) iconKey = ExpenseCategoryCatalog.suggestIconKey(raw)
                    },
                    label = { Text("Category name", fontFamily = SplitMateBrandFontFamily) },
                    placeholder = { Text("e.g. Paragliding, Kerala Toll", fontFamily = SplitMateBrandFontFamily) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = {
                        Text(
                            error ?: "${name.length}/${ExpenseCategoryCatalog.MAX_TITLE_LENGTH}",
                            fontFamily = SplitMateBrandFontFamily,
                            fontSize = 11.sp
                        )
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SplitMateTheme.PrimaryDark,
                        unfocusedBorderColor = SplitMateTheme.BorderLight,
                        focusedTextColor = SplitMateTheme.PrimaryDark,
                        unfocusedTextColor = SplitMateTheme.PrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Preview",
                        fontFamily = SplitMateBrandFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = SplitMateTheme.TextSecondary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    ExpenseCategoryChip(category = preview, selected = false, onClick = {})
                }

                Text(
                    "ICON",
                    fontFamily = SplitMateBrandFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    color = SplitMateTheme.TextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExpenseCategoryIconKeys.ALL.forEach { key ->
                        val isPicked = key == iconKey
                        Surface(
                            onClick = {
                                iconKey = key
                                iconPickedManually = true
                            },
                            shape = RoundedCornerShape(if (isPicked) 14.dp else 24.dp),
                            color = if (isPicked) SplitMateTheme.SageSurface else SplitMateTheme.SurfaceMuted,
                            border = BorderStroke(
                                if (isPicked) 2.dp else 1.dp,
                                if (isPicked) SplitMateTheme.SageText else SplitMateTheme.BorderLight
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .semantics {
                                    selected = isPicked
                                    contentDescription = "Icon ${key.replace('_', ' ')}"
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ExpenseCategoryIcons.forKey(key),
                                    contentDescription = null,
                                    tint = if (isPicked) SplitMateTheme.SageText else SplitMateTheme.PrimaryDark,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Surface(
                onClick = {
                    CustomExpenseCategoryStore.add(context, name, iconKey)
                        .onSuccess(onCreated)
                        .onFailure { error = it.message }
                },
                shape = RoundedCornerShape(24.dp),
                color = SplitMateTheme.PrimaryDark,
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        "Create",
                        fontFamily = SplitMateBrandFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = SplitMateTheme.SurfaceWhite
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Bold, color = SplitMateTheme.PrimaryDark)
            }
        }
    )
}
