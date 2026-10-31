package com.splitmate.app.ui.category

import com.splitmate.app.ui.theme.HubCategoryHue
import com.splitmate.app.ui.theme.HubExtendedColors
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
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.semantics.error
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
import com.splitmate.app.ui.components.ExpressivePlainTooltipBox
import com.splitmate.app.ui.components.ExpressiveTooltipIconButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.minimumInteractiveComponentSize
import java.util.Locale

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
    fun toneColors(tone: ExpenseCategoryTone, @Suppress("UNUSED_PARAMETER") isDark: Boolean): Pair<Color, Color> {
        val hue = when (tone) {
            ExpenseCategoryTone.SAGE -> com.splitmate.app.ui.theme.HubCategoryHue.SAGE
            ExpenseCategoryTone.PERIWINKLE -> com.splitmate.app.ui.theme.HubCategoryHue.PERIWINKLE
            ExpenseCategoryTone.AMBER -> com.splitmate.app.ui.theme.HubCategoryHue.AMBER
            ExpenseCategoryTone.MINT -> com.splitmate.app.ui.theme.HubCategoryHue.MINT
            ExpenseCategoryTone.ROSE -> com.splitmate.app.ui.theme.HubCategoryHue.ROSE
            ExpenseCategoryTone.PEACH -> com.splitmate.app.ui.theme.HubCategoryHue.PEACH
        }
        val roles = com.splitmate.app.ui.theme.HubExtendedColors.categoryHue(hue)
        return roles.colorContainer to roles.onColorContainer
    }
}

/** Exact-title lookup used by the global icon/badge resolvers (null = fall back to keyword engine). */
fun exactExpenseCategoryFor(title: String): ExpenseCategory? =
    ExpenseCategoryCatalog.exactMatch(title, CustomExpenseCategoryStore.current)

// ==============================================================================
// Reusable chip
// ==============================================================================

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    FilterChip(
        selected = selected,
        onClick = onClick,
        shapes = FilterChipDefaults.shapes(),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SplitMateTheme.SurfaceWhite,
            selectedContainerColor = selectedBg
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = SplitMateTheme.BorderLight,
            selectedBorderColor = selectedBg,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp
        ),
        modifier = modifier
            .heightIn(min = 40.dp)
            .semantics {
                this.selected = selected
                role = Role.Button
            },
        leadingIcon = {
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
        },
        trailingIcon = trailing,
        label = {
            Text(
                text = label,
                fontFamily = SplitMateBrandFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) selectedFg else SplitMateTheme.PrimaryDark
            )
        }
    )
}

// ==============================================================================
// "All categories" bottom sheet
// ==============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
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
                label = { Text("Search categories", fontFamily = SplitMateBrandFontFamily) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        ExpressiveTooltipIconButton(
                            tooltipText = "Clear search",
                            onClick = { query = "" }
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                } else null,
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
                                val isCatSelected = cat.title.equals(selectedTitle.trim(), ignoreCase = true)
                                val (toneBg, toneFg) = ExpenseCategoryIcons.toneColors(cat.tone, SplitMateTheme.isDark)
                                val selectedBg = SplitMateTheme.PrimaryDark
                                val selectedFg = SplitMateTheme.SurfaceWhite
                                InputChip(
                                    selected = isCatSelected,
                                    onClick = { onSelect(cat) },
                                    shapes = InputChipDefaults.shapes(),
                                    colors = InputChipDefaults.inputChipColors(
                                        containerColor = SplitMateTheme.SurfaceWhite,
                                        selectedContainerColor = selectedBg,
                                        labelColor = SplitMateTheme.PrimaryDark,
                                        selectedLabelColor = selectedFg
                                    ),
                                    border = InputChipDefaults.inputChipBorder(
                                        enabled = true,
                                        selected = isCatSelected,
                                        borderColor = SplitMateTheme.BorderLight,
                                        selectedBorderColor = selectedBg,
                                        borderWidth = 1.dp,
                                        selectedBorderWidth = 1.dp
                                    ),
                                    modifier = Modifier.heightIn(min = 40.dp),
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(
                                                    if (isCatSelected) selectedFg.copy(alpha = 0.16f) else toneBg,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = ExpenseCategoryIcons.forKey(cat.iconKey),
                                                contentDescription = null,
                                                tint = if (isCatSelected) selectedFg else toneFg,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = cat.title,
                                            fontFamily = SplitMateBrandFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isCatSelected) selectedFg else SplitMateTheme.PrimaryDark
                                        )
                                    },
                                    trailingIcon = {
                                        ExpressiveTooltipIconButton(
                                            tooltipText = "Remove ${cat.title}",
                                            onClick = { pendingDelete = cat },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove ${cat.title}",
                                                tint = if (isCatSelected) selectedFg else SplitMateTheme.TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
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
                    "It disappears from the category list${if (cat.isGroupShared) " for everyone in this trip" else ""}. Expenses already logged with it stay exactly as they are.",
                    fontFamily = SplitMateBrandFontFamily,
                    fontSize = 13.sp,
                    color = SplitMateTheme.TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    CustomExpenseCategoryStore.remove(context, cat.title)
                    pendingDelete = null
                }, shapes = ButtonDefaults.shapes()) { Text("Remove", fontWeight = FontWeight.Bold, color = SplitMateTheme.TerracottaText) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }, shapes = ButtonDefaults.shapes()) {
                    Text("Keep", fontWeight = FontWeight.Bold, color = SplitMateTheme.PrimaryDark)
                }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontFamily = SplitMateBrandFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        letterSpacing = 0.1.sp,
        color = SplitMateTheme.TextSecondary
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CreateCategoryChip(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = SplitMateTheme.SageSurface,
            contentColor = SplitMateTheme.SageText
        ),
        border = BorderStroke(1.dp, SplitMateTheme.AccentSage),
        contentPadding = ButtonDefaults.SmallContentPadding,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .minimumInteractiveComponentSize()
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = SplitMateTheme.SageText, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
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

/** Compact "More" button used at the end of the existing quick-category rows. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoreCategoriesChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SplitMateTheme.SurfaceWhite,
            contentColor = SplitMateTheme.PrimaryDark
        ),
        border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
        contentPadding = ButtonDefaults.ExtraSmallContentPadding,
        modifier = modifier
            .heightIn(min = 32.dp)
            .minimumInteractiveComponentSize()
    ) {
        Icon(
            Icons.Rounded.MoreHoriz,
            contentDescription = null,
            tint = SplitMateTheme.PrimaryDark,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.ExtraSmallIconSpacing))
        Text(
            text = "More",
            fontFamily = SplitMateBrandFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SplitMateTheme.PrimaryDark
        )
    }
}

// ==============================================================================
// Create-category dialog (name + icon grid + live preview)
// ==============================================================================

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
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
    // v2.3.5 (#5): parent group so the Trip Hub breakdown never dumps custom categories into "Other".
    var bucket by remember { mutableStateOf(ExpenseBucketResolver.suggestBucket(initialName)) }
    var bucketPickedManually by remember { mutableStateOf(false) }
    val sharedWithTrip = CustomExpenseCategoryStore.activeGroupId.isNotBlank()

    val preview = ExpenseCategoryCatalog.custom(name.ifBlank { "Your category" }, iconKey, parentBucket = bucket)

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
                    if (sharedWithTrip) "Name it, pick an icon and a group. Everyone in this trip will see it."
                    else "Name it, pick an icon and a group. It's saved on this phone for all your trips.",
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
                val currentError = error
                OutlinedTextField(
                    value = name,
                    onValueChange = { raw ->
                        name = raw.take(ExpenseCategoryCatalog.MAX_TITLE_LENGTH)
                        error = null
                        if (!iconPickedManually) iconKey = ExpenseCategoryCatalog.suggestIconKey(raw)
                        if (!bucketPickedManually) bucket = ExpenseBucketResolver.suggestBucket(raw, iconKey)
                    },
                    label = { Text("Category name", fontFamily = SplitMateBrandFontFamily) },
                    placeholder = { Text("e.g. Paragliding, Kerala Toll", fontFamily = SplitMateBrandFontFamily) },
                    singleLine = true,
                    isError = currentError != null,
                    supportingText = {
                        Text(
                            currentError ?: "${name.length}/${ExpenseCategoryCatalog.MAX_TITLE_LENGTH}",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            if (currentError != null) {
                                this.error(currentError)
                            }
                        }
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
                    "Counts under",
                    fontFamily = SplitMateBrandFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.1.sp,
                    color = SplitMateTheme.TextSecondary
                )
                FlowRow(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpendBucket.values().forEach { b ->
                        val isPicked = b == bucket
                        FilterChip(
                            selected = isPicked,
                            onClick = {
                                bucket = b
                                bucketPickedManually = true
                            },
                            shapes = FilterChipDefaults.shapes(),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SplitMateTheme.SurfaceMuted,
                                selectedContainerColor = SplitMateTheme.PrimaryDark
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isPicked,
                                borderColor = SplitMateTheme.BorderLight,
                                selectedBorderColor = SplitMateTheme.PrimaryDark,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp
                            ),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .semantics { selected = isPicked },
                            label = {
                                Text(
                                    b.label,
                                    fontFamily = SplitMateBrandFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isPicked) SplitMateTheme.SurfaceWhite else SplitMateTheme.PrimaryDark
                                )
                            }
                        )
                    }
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
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExpenseCategoryIconKeys.ALL.forEach { key ->
                        val isPicked = key == iconKey
                        val iconLabel = remember(key) {
                            key.replace('_', ' ').replaceFirstChar { ch ->
                                if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString()
                            }
                        }
                        ExpressivePlainTooltipBox(tooltipText = iconLabel) {
                            OutlinedIconToggleButton(
                                checked = isPicked,
                                onCheckedChange = {
                                    iconKey = key
                                    iconPickedManually = true
                                    if (!bucketPickedManually && name.isBlank()) {
                                        bucket = ExpenseBucketResolver.suggestBucket(name, key)
                                    }
                                },
                                shapes = IconButtonDefaults.toggleableShapes(),
                                colors = IconButtonDefaults.outlinedIconToggleButtonColors(
                                    containerColor = SplitMateTheme.SurfaceMuted,
                                    contentColor = SplitMateTheme.PrimaryDark,
                                    checkedContainerColor = SplitMateTheme.SageSurface,
                                    checkedContentColor = SplitMateTheme.SageText
                                ),
                                border = BorderStroke(
                                    if (isPicked) 2.dp else 1.dp,
                                    if (isPicked) SplitMateTheme.SageText else SplitMateTheme.BorderLight
                                ),
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics {
                                        role = Role.RadioButton
                                        selected = isPicked
                                        contentDescription = iconLabel
                                    }
                            ) {
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
            // v2.4.0 rc3: official Button with the shapes() press morph.
            Button(
                onClick = {
                    CustomExpenseCategoryStore.add(context, name, iconKey, bucket)
                        .onSuccess(onCreated)
                        .onFailure { error = it.message }
                },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SplitMateTheme.PrimaryDark,
                    contentColor = SplitMateTheme.SurfaceWhite
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text(
                    "Create",
                    fontFamily = SplitMateBrandFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = SplitMateTheme.SurfaceWhite
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text("Cancel", fontWeight = FontWeight.Bold, color = SplitMateTheme.PrimaryDark)
            }
        }
    )
}
