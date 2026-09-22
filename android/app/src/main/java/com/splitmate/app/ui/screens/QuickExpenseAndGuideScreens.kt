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
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import com.splitmate.app.ui.queryAllDeviceContacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.buildDiceBearOpenPeepsUrl
import com.splitmate.app.ui.extractInitialsFromNameOrSeed
import com.splitmate.app.ui.resolveGroupCategoryIcon
import java.text.NumberFormat
import java.util.Locale

// ==============================================================================
// SPLITMATE MATERIAL 3 EXPRESSIVE THEME TOKENS (GM3 DARK ELEVATION COMPLIANT)
// ==============================================================================
object QuickExpenseThemeTokens {
    val ScreenBg: Color
        get() = if (SplitMateTheme.isDark) DesignSystemBindings.GM3DarkBackground else DesignSystemBindings.GM3LightBackground
    val PrimaryDark: Color
        get() = if (SplitMateTheme.isDark) DesignSystemBindings.GM3DarkPrimaryText else DesignSystemBindings.GM3LightPrimaryText
    val AccentSage = DesignSystemBindings.ElementsPositiveContainer
    val SageSurface = Color(0xFFEAF3DC)
    val SageText = DesignSystemBindings.ElementsPositiveText
    val SageBorder = Color(0xFF5A8E24)
    val TerracottaSurface = Color(0xFFFCECE7)
    val TerracottaText = DesignSystemBindings.ElementsNegativeText
    val SurfaceWhite: Color
        get() = if (SplitMateTheme.isDark) DesignSystemBindings.GM3DarkCardSurface else DesignSystemBindings.GM3LightCardSurface
    val SurfaceKeypad: Color
        get() = if (SplitMateTheme.isDark) DesignSystemBindings.GM3DarkKeypadSurface else DesignSystemBindings.GM3LightKeypadSurface
    val SurfaceKeypadBorder: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF383838) else Color(0xFFE8E2D8)
    val BorderLight: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF333333) else Color(0xFFE5DFC5)
    val TextSecondary: Color
        get() = if (SplitMateTheme.isDark) DesignSystemBindings.GM3DarkSubtitleText else DesignSystemBindings.GM3LightSubtitleText

    val RadiusHero = DesignSystemBindings.GM3ShapeExtraLarge
    val RadiusCard = DesignSystemBindings.GM3ShapeLarge
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
    onSaveSplit: (amount: Long, selectedMembers: List<QuickParticipant>) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val uiState = viewModel?.uiState?.collectAsState()?.value
    val activeMembers = viewModel?.activeGroupMembers?.collectAsState()?.value
        ?: uiState?.activeGroupMembers
        ?: emptyList()
    val isDark = uiState?.isDarkTheme == true || SplitMateTheme.isDark

    val screenBg by animateColorAsState(
        targetValue = if (isDark) DesignSystemBindings.GM3DarkBackground else DesignSystemBindings.GM3LightBackground,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "QuickExpenseScreenBg"
    )
    val surfaceColor by animateColorAsState(
        targetValue = if (isDark) DesignSystemBindings.GM3DarkCardSurface else DesignSystemBindings.GM3LightCardSurface,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "QuickExpenseCardSurface"
    )
    val textPrimary by animateColorAsState(
        targetValue = if (isDark) DesignSystemBindings.GM3DarkPrimaryText else DesignSystemBindings.GM3LightPrimaryText,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "QuickExpenseTextPrimary"
    )
    val textSecondary by animateColorAsState(
        targetValue = if (isDark) DesignSystemBindings.GM3DarkSubtitleText else DesignSystemBindings.GM3LightSubtitleText,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "QuickExpenseTextSecondary"
    )
    val keypadBg by animateColorAsState(
        targetValue = if (isDark) DesignSystemBindings.GM3DarkKeypadSurface else DesignSystemBindings.GM3LightKeypadSurface,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "QuickExpenseKeypadBg"
    )
    val keypadBorder by animateColorAsState(
        targetValue = if (isDark) Color(0xFF3A3A3A) else Color(0xFFE8E2D8),
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

    var amountDigits by remember { mutableStateOf("450") }
    var expenseCategoryTitle by remember { mutableStateOf("Dinner & Food") }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var showGroupDropdown by remember { mutableStateOf(false) }
    var editingFriend by remember { mutableStateOf<GroupMemberEntity?>(null) }

    var selectedMemberIds by remember(participants) {
        mutableStateOf(participants.map { it.id }.toSet())
    }
    val haptic = LocalHapticFeedback.current

    val currencySymbol = "₹"
    val selectedGroup = uiState?.activeGroup
    val hasSelectedGroup = selectedGroup != null && participants.isNotEmpty()
    val activeGroupName = selectedGroup?.name ?: "Select a Group"
    val activeGroupIconName = selectedGroup?.iconName ?: "Flight"

    val numericVal = amountDigits.toLongOrNull() ?: 0L
    val formattedDisplay = if (numericVal == 0L) {
        "$currencySymbol 0"
    } else {
        "$currencySymbol " + NumberFormat.getNumberInstance(Locale("en", "IN")).format(numericVal)
    }

    val memberCount = selectedMemberIds.size
    val perPerson = if (memberCount > 0) numericVal / memberCount else 0L
    val remainder = if (memberCount > 0) numericVal % memberCount else 0L

    if (showEditTitleDialog) {
        var draftTitle by remember { mutableStateOf(expenseCategoryTitle) }
        val presetCategories = remember {
            listOf(
                "Dinner & Food" to Icons.Rounded.Restaurant,
                "Travel & Flight" to Icons.Rounded.Flight,
                "Groceries" to Icons.Rounded.ShoppingCart,
                "Home & Rent" to Icons.Rounded.Home,
                "Party & Drinks" to Icons.Rounded.LocalBar,
                "Coffee & Cafe" to Icons.Rounded.LocalCafe
            )
        }
        AlertDialog(
            onDismissRequest = { showEditTitleDialog = false },
            containerColor = surfaceColor,
            title = {
                Text(
                    "Log Expense Description",
                    fontFamily = SplitMateDisplayFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presetCategories) { (catLabel, catIcon) ->
                            val isSelected = draftTitle.equals(catLabel, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { draftTitle = catLabel },
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
                    }
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        label = { Text("What was this expense for?", fontFamily = SplitMateBrandFontFamily) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = textPrimary,
                            unfocusedBorderColor = keypadBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (draftTitle.isNotBlank()) expenseCategoryTitle = draftTitle.trim()
                        showEditTitleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = textPrimary,
                        contentColor = screenBg
                    )
                ) {
                    Text("Save", fontFamily = SplitMateBrandFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitleDialog = false }) {
                    Text("Cancel", fontFamily = SplitMateBrandFontFamily, color = textSecondary)
                }
            }
        )
    }

    editingFriend?.let { friend ->
        EditFriendUpiDialog(
            member = friend,
            onDismiss = { editingFriend = null },
            onSave = { newName, newUpi, newAvatarSeed ->
                viewModel?.updateFriendUpi(friend.memberId, newName, newUpi, newAvatarSeed)
                editingFriend = null
            }
        )
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            // Root BottomNav destination — NO back arrow navigationIcon!
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
                                                "${grp.name} (₹ INR)",
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
                    Surface(
                        shape = QuickExpenseThemeTokens.RadiusPill,
                        color = surfaceColor,
                        border = BorderStroke(1.dp, keypadBorder),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "₹ INR • Equal Split",
                            fontFamily = SplitMateBrandFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
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
                                .height(50.dp)
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
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ==================================================================
                // 1. TOP SECTION: Elevated Category Pill + Tightly Coupled DisplayLarge Amount
                // ==================================================================
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp)
                ) {
                    Surface(
                        onClick = { showEditTitleDialog = true },
                        shape = QuickExpenseThemeTokens.RadiusPill,
                        color = surfaceColor,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF4A332C) else Color(0xFFFFD9CE)),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        colors = if (isDark) {
                                            listOf(Color(0xFF28201D), Color(0xFF33241F))
                                        } else {
                                            listOf(Color(0xFFFFF7F3), Color(0xFFFDECE5))
                                        }
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = resolveGroupCategoryIcon("", expenseCategoryTitle),
                                contentDescription = null,
                                tint = textPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = expenseCategoryTitle,
                                fontFamily = SplitMateBrandFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit Category",
                                tint = textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // DisplayLarge Amount closely beneath category chip
                    Text(
                        text = formattedDisplay,
                        fontFamily = SplitMateDisplayFontFamily,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        letterSpacing = (-1.2).sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 52.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Real-time Exact Split / Unassigned Badge
                    Surface(
                        shape = QuickExpenseThemeTokens.RadiusPill,
                        color = if (memberCount > 0) QuickExpenseThemeTokens.SageSurface else QuickExpenseThemeTokens.TerracottaSurface,
                        border = BorderStroke(
                            1.dp,
                            if (memberCount > 0) QuickExpenseThemeTokens.AccentSage else Color(0xFFFFCCBA)
                        ),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = if (memberCount > 0) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = if (memberCount > 0) QuickExpenseThemeTokens.SageText else QuickExpenseThemeTokens.TerracottaText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (memberCount > 0) {
                                    "$currencySymbol ${NumberFormat.getNumberInstance(Locale("en", "IN")).format(perPerson)} / person · $memberCount splitting" +
                                        if (remainder > 0L) " · +$currencySymbol$remainder Unassigned" else " · Exact Split"
                                } else {
                                    "Select at least 1 person to split"
                                },
                                fontFamily = SplitMateBrandFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (memberCount > 0) QuickExpenseThemeTokens.SageText else QuickExpenseThemeTokens.TerracottaText
                            )
                        }
                    }
                }

                // ==================================================================
                // 2. MID SECTION: Compact Avatar Strip Directly Above Keypad
                // Clear Button vertically aligned on the exact same row as "Who's in on this?"
                // ==================================================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Who's in on this?",
                            fontFamily = SplitMateDisplayFontFamily,
                            fontSize = 15.sp,
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
                            border = BorderStroke(1.dp, QuickExpenseThemeTokens.AccentSage.copy(alpha = 0.8f))
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (selectedMemberIds.size == participants.size) "Clear" else "Select All (${participants.size})",
                                    fontFamily = SplitMateBrandFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = QuickExpenseThemeTokens.SageText
                                )
                            }
                        }
                    }

                    Text(
                        text = "Tap to toggle · Long-press friend to link Contact UPI",
                        fontFamily = SplitMateBrandFontFamily,
                        fontSize = 11.sp,
                        color = textSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(participants, key = { it.id }) { person ->
                            val isSelected = selectedMemberIds.contains(person.id)
                            val matchingRoomMember = activeMembers.find { it.memberId == person.id }
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
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .shadow(
                                            elevation = if (isSelected) 5.dp else 1.dp,
                                            shape = CircleShape
                                        )
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) person.avatarBg else person.avatarBg.copy(alpha = 0.45f)
                                        )
                                        .border(
                                            width = if (isSelected) 3.dp else 1.5.dp,
                                            color = if (isSelected) QuickExpenseThemeTokens.SageBorder else surfaceColor,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = person.initials,
                                        fontFamily = SplitMateDisplayFontFamily,
                                        fontSize = 15.sp,
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

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(QuickExpenseThemeTokens.SageBorder)
                                                .border(1.5.dp, surfaceColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = person.name,
                                    fontFamily = SplitMateBrandFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) textPrimary else textSecondary.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // ==================================================================
                // 3. BOTTOM SECTION: Tactile M3 Squarcles Keypad + 2-Row Log & Split FAB
                // (4 rows * 54dp + 3 gaps * 8dp = 240dp; Right Column = 54dp Note + 54dp C + 116dp 2-Row FAB)
                // ==================================================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
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
                                    appendDigit(key, amountDigits) { amountDigits = it }
                                }
                            )

                            KeypadRow(
                                keys = listOf("4", "5", "6"),
                                keypadBg = keypadBg,
                                keypadBorder = keypadBorder,
                                textPrimary = textPrimary,
                                onKeyPress = { key ->
                                    appendDigit(key, amountDigits) { amountDigits = it }
                                }
                            )

                            KeypadRow(
                                keys = listOf("7", "8", "9"),
                                keypadBg = keypadBg,
                                keypadBorder = keypadBorder,
                                textPrimary = textPrimary,
                                onKeyPress = { key ->
                                    appendDigit(key, amountDigits) { amountDigits = it }
                                }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TactileSquircleKey(
                                    label = "00",
                                    keypadBg = keypadBg,
                                    keypadBorder = keypadBorder,
                                    textPrimary = textPrimary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (amountDigits.isNotEmpty() && amountDigits.length < 8) {
                                            amountDigits += "00"
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
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (amountDigits != "0" && amountDigits.length < 8) {
                                            amountDigits += "0"
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
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (amountDigits.isNotEmpty()) {
                                            amountDigits = amountDigits.dropLast(1)
                                        }
                                    }
                                )
                            }
                        }

                        // Right: 4th Column (Row 1 = 54dp Note, Row 2 = 54dp Clear "C", Rows 3 & 4 = 116dp 2-Row Log & Split FAB)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showEditTitleDialog = true
                                    onLogExpenseClick()
                                },
                                shape = QuickExpenseThemeTokens.RadiusKeySquircle,
                                color = QuickExpenseThemeTokens.SageSurface,
                                shadowElevation = 2.dp,
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
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    amountDigits = "0"
                                }
                            )

                            // Log & Split FAB spanning EXACTLY two rows in height (54dp + 8dp + 54dp = 116dp)
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val selected = participants.filter { selectedMemberIds.contains(it.id) }
                                    viewModel?.commitQuickEqualExpense(
                                        title = expenseCategoryTitle,
                                        totalAmountCents = numericVal * 100L,
                                        selectedMemberIds = selected.map { it.id }
                                    )
                                    onSaveSplit(numericVal, selected)
                                },
                                shape = QuickExpenseThemeTokens.RadiusKeySquircle,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = textPrimary,
                                    contentColor = screenBg
                                ),
                                enabled = hasSelectedGroup && numericVal > 0L && selectedMemberIds.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(116.dp)
                                    .shadow(6.dp, shape = QuickExpenseThemeTokens.RadiusKeySquircle),
                                contentPadding = PaddingValues(6.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isDark) Color(0xFF416913).copy(alpha = 0.35f) else QuickExpenseThemeTokens.AccentSage.copy(alpha = 0.25f),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.ElectricBolt,
                                                contentDescription = null,
                                                tint = if (isDark) Color(0xFF416913) else QuickExpenseThemeTokens.AccentSage,
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
                                        color = screenBg,
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
    val haptic = LocalHapticFeedback.current

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
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "KeyScale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = QuickExpenseThemeTokens.RadiusKeySquircle,
        color = if (isPressed) keypadBorder else keypadBg,
        shadowElevation = if (isPressed) 1.dp else 3.dp,
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
                    color = textPrimary
                )
            }
        }
    }
}

// Helper function for appending keypad input
private fun appendDigit(key: String, current: String, onUpdate: (String) -> Unit) {
    if (current.length < 8) {
        if (current == "0") {
            onUpdate(key)
        } else {
            onUpdate(current + key)
        }
    }
}

// ==============================================================================
// EDIT FRIEND PERSONA & IN-APP CONTACT PICKER DIALOG
// ==============================================================================
@Composable
fun EditFriendUpiDialog(
    member: GroupMemberEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, upiId: String, avatarSeed: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialStyle = remember(member.avatarSeed) {
        member.avatarSeed.substringAfter('|', "Neutral").takeIf {
            it in listOf("Masculine", "Feminine", "Neutral")
        } ?: "Neutral"
    }
    var friendName by remember(member) { mutableStateOf(member.name) }
    var selectedPresentationStyle by remember(member) { mutableStateOf(initialStyle) }

    val initialCleanDigits = remember(member.upiId) {
        member.upiId.substringBefore('@').replace(Regex("[^0-9]"), "")
    }
    var pickedPhoneNumber by remember(member) { mutableStateOf(initialCleanDigits) }
    val resolvedPhoneUpi = remember(pickedPhoneNumber) {
        if (pickedPhoneNumber.length == 10) {
            "$pickedPhoneNumber@upi"
        } else {
            ""
        }
    }

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

    val avatarPreviewUrl = remember(friendName, selectedPresentationStyle) {
        buildDiceBearOpenPeepsUrl(friendName.ifBlank { member.name }, selectedPresentationStyle)
    }
    val cleanInitials = remember(friendName, member.name) {
        extractInitialsFromNameOrSeed(friendName.ifBlank { member.name })
    }

    if (showInAppContactPicker) {
        ContactPickerBottomSheet(
            contacts = deviceContacts,
            isLoading = isLoadingContacts,
            multiSelect = false,
            title = "Link Contact for ${friendName.ifBlank { member.name }}",
            subtitle = "Select a phone contact to enable direct UPI and WhatsApp",
            onDismissRequest = { showInAppContactPicker = false },
            onConfirmSelected = { selected ->
                val chosen = selected.firstOrNull()
                if (chosen != null) {
                    friendName = chosen.name
                    pickedPhoneNumber = chosen.cleanPhone
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
                            text = "Edit Member",
                            fontFamily = SplitMateDisplayFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = QuickExpenseThemeTokens.PrimaryDark
                        )
                        Text(
                            text = "Update profile and phone contact",
                            fontFamily = SplitMateBrandFontFamily,
                            fontSize = 12.sp,
                            color = QuickExpenseThemeTokens.TextSecondary
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 1. Presentation Style Toggle (Masculine, Feminine, Neutral)
                    Column {
                        Text(
                            text = "Presentation Style",
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
                                listOf("Masculine", "Feminine", "Neutral").forEach { style ->
                                    val isSelected = selectedPresentationStyle == style
                                    Surface(
                                        onClick = { selectedPresentationStyle = style },
                                        shape = QuickExpenseThemeTokens.RadiusPill,
                                        color = if (isSelected) QuickExpenseThemeTokens.PrimaryDark else Color.Transparent,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = style,
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

                    // 2. Friend's Name Field
                    OutlinedTextField(
                        value = friendName,
                        onValueChange = { friendName = it },
                        label = { Text("Friend's Name", fontFamily = SplitMateBrandFontFamily) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = QuickExpenseThemeTokens.PrimaryDark,
                            unfocusedTextColor = QuickExpenseThemeTokens.PrimaryDark,
                            focusedBorderColor = QuickExpenseThemeTokens.PrimaryDark,
                            unfocusedBorderColor = QuickExpenseThemeTokens.BorderLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. Pick from Contacts Button (Opens In-App ContactPickerBottomSheet)
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
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Contacts,
                            contentDescription = null,
                            tint = QuickExpenseThemeTokens.SageText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (pickedPhoneNumber.length == 10) "Change Linked Contact (+91 $pickedPhoneNumber)" else "+ Link Contact",
                            fontFamily = SplitMateBrandFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = QuickExpenseThemeTokens.SageText
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = friendName.trim().ifEmpty { member.name }
                        val styledSeed = "$cleanName|$selectedPresentationStyle"
                        onSave(cleanName, resolvedPhoneUpi, styledSeed)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QuickExpenseThemeTokens.PrimaryDark,
                        contentColor = QuickExpenseThemeTokens.ScreenBg
                    )
                ) {
                    Text(
                        "Save Member",
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
