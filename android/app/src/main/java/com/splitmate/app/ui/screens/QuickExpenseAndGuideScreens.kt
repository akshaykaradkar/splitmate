package com.splitmate.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.data.CurrencyRateEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.SplitMateViewModel
import java.text.NumberFormat
import java.util.Locale

// ==============================================================================
// SPLITMATE MATERIAL 3 EXPRESSIVE THEME TOKENS
// ==============================================================================
object QuickExpenseThemeTokens {
    val ScreenBg = Color(0xFFFAF7F2)               // Warm Eggshell Canvas
    val PrimaryDark = Color(0xFF23201E)            // Charcoal Espresso
    val AccentSage = Color(0xFFD7E8B6)             // Active Accent / Glowing Bolt
    val SageSurface = Color(0xFFEAF3DC)            // Soft Sage Surface (Receipt Key / Badge)
    val SageText = Color(0xFF2D4810)               // Deep Forest Green Text
    val SageBorder = Color(0xFF5A8E24)             // Selected Avatar High-Contrast Border
    val TerracottaSurface = Color(0xFFFCECE7)      // Warning / Debit Surface
    val TerracottaText = Color(0xFFC23E2A)         // Terracotta Text
    val SurfaceWhite = Color(0xFFFFFFFF)
    val SurfaceKeypad = Color(0xFFF3EFEA)          // Tactile Keypad Squircle Background
    val SurfaceKeypadBorder = Color(0xFFE8E2D8)    // Subtle Key Border
    val BorderLight = Color(0xFFE5DFC5)
    val TextSecondary = Color(0xFF7A746D)
    val ShadowSoft = Color(0x1A23201E)

    // Expressive Radii
    val RadiusHero = RoundedCornerShape(32.dp)
    val RadiusCard = RoundedCornerShape(24.dp)
    val RadiusKeySquircle = RoundedCornerShape(24.dp)
    val RadiusPill = RoundedCornerShape(999.dp)
}

// Split Participant Model
data class QuickParticipant(
    val id: String,
    val name: String,
    val initials: String,
    val avatarBg: Color,
    val avatarFg: Color,
    val upiId: String = ""
)

val DefaultParticipants = listOf(
    QuickParticipant("1", "You", "JD", Color(0xFFD7E8B6), Color(0xFF23201E)),
    QuickParticipant("2", "Maya", "ML", Color(0xFFFFD8CC), Color(0xFF8A2E1A)),
    QuickParticipant("3", "Sam", "SK", Color(0xFFD0E2FF), Color(0xFF143E82)),
    QuickParticipant("4", "Priya", "PR", Color(0xFFFFD5E5), Color(0xFF801844)),
    QuickParticipant("5", "Kai", "KL", Color(0xFFE5DCFF), Color(0xFF452285)),
    QuickParticipant("6", "Aria", "AZ", Color(0xFFD2F5DC), Color(0xFF1B6331))
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
    onScanReceiptClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onSaveSplit: (amount: Long, selectedMembers: List<QuickParticipant>) -> Unit = { _, _ -> }
) {
    val uiState = viewModel?.uiState?.collectAsState()?.value
    val isDark = uiState?.isDarkTheme == true
    val screenBg = if (isDark) Color(0xFF141311) else QuickExpenseThemeTokens.ScreenBg
    val surfaceColor = if (isDark) Color(0xFF1F1D1A) else QuickExpenseThemeTokens.SurfaceWhite
    val textPrimary = if (isDark) Color(0xFFF6F2EA) else QuickExpenseThemeTokens.PrimaryDark
    val textSecondary = if (isDark) Color(0xFFB5ADA3) else QuickExpenseThemeTokens.TextSecondary
    val keypadBg = if (isDark) Color(0xFF282521) else QuickExpenseThemeTokens.SurfaceKeypad
    val keypadBorder = if (isDark) Color(0xFF3B3630) else QuickExpenseThemeTokens.SurfaceKeypadBorder

    val activeMembers = uiState?.activeGroupMembers ?: emptyList()
    val participants: List<QuickParticipant> = remember(activeMembers) {
        if (activeMembers.isNotEmpty()) {
            activeMembers.mapIndexed { idx, m ->
                val (bg, fg) = ParticipantPalette[idx % ParticipantPalette.size]
                val parts = m.name.trim().split(" ").filter { it.isNotBlank() }
                val initials = when {
                    parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
                    parts.size == 1 -> parts[0].take(2).uppercase()
                    else -> "SM"
                }
                QuickParticipant(
                    id = m.memberId,
                    name = if (m.isCurrentUser) "${m.name} (You)" else m.name,
                    initials = initials,
                    avatarBg = bg,
                    avatarFg = fg,
                    upiId = m.upiId
                )
            }
        } else {
            DefaultParticipants
        }
    }

    var amountDigits by remember { mutableStateOf("450") }
    var expenseCategoryTitle by remember { mutableStateOf("🍜 Dinner & Food Tab") }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var showGroupDropdown by remember { mutableStateOf(false) }
    var editingFriend by remember { mutableStateOf<GroupMemberEntity?>(null) }

    var selectedMemberIds by remember(participants) {
        mutableStateOf(participants.map { it.id }.toSet())
    }
    val haptic = LocalHapticFeedback.current

    val currencySymbol = uiState?.activeCurrency?.symbol ?: "₹"
    val activeGroupName = uiState?.activeGroup?.name ?: "Create / Select Group"

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
        AlertDialog(
            onDismissRequest = { showEditTitleDialog = false },
            containerColor = surfaceColor,
            title = { Text("Expense Description", fontWeight = FontWeight.ExtraBold, color = textPrimary) },
            text = {
                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it },
                    label = { Text("What was this expense for?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (draftTitle.isNotBlank()) expenseCategoryTitle = draftTitle.trim()
                        showEditTitleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QuickExpenseThemeTokens.PrimaryDark,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitleDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    editingFriend?.let { friend ->
        EditFriendUpiDialog(
            member = friend,
            onDismiss = { editingFriend = null },
            onSave = { newName, newUpi ->
                viewModel?.updateFriendUpi(friend.memberId, newName, newUpi)
                editingFriend = null
            }
        )
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
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
                                    imageVector = Icons.Rounded.FlightTakeoff,
                                    contentDescription = null,
                                    tint = QuickExpenseThemeTokens.SageText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeGroupName,
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
                                    text = { Text("No groups yet — create one in Ledgers") },
                                    onClick = {
                                        showGroupDropdown = false
                                        onBackClick()
                                    }
                                )
                            } else {
                                groups.forEach { grp ->
                                    DropdownMenuItem(
                                        text = { Text("${grp.name} (${grp.currencyCode})", fontWeight = FontWeight.SemiBold) },
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
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = surfaceColor,
                            border = BorderStroke(1.dp, QuickExpenseThemeTokens.BorderLight.copy(alpha = 0.5f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Currency auto-peg tag
                    Surface(
                        shape = QuickExpenseThemeTokens.RadiusPill,
                        color = surfaceColor.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, QuickExpenseThemeTokens.BorderLight.copy(alpha = 0.6f)),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = "${uiState?.activeCurrencyCode ?: "INR"} • 0.00¢ drift",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    IconButton(
                        onClick = onHelpClick,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = surfaceColor,
                            border = BorderStroke(1.dp, QuickExpenseThemeTokens.BorderLight.copy(alpha = 0.5f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.HelpOutline,
                                    contentDescription = "Guide",
                                    tint = textPrimary,
                                    modifier = Modifier.size(20.dp)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ==================================================================
            // 1. TOP SECTION: Elevated Category Pill + Massive DisplayLarge Amount
            // ==================================================================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp)
            ) {
                // Elevated Card Behind Category Chip
                Surface(
                    onClick = { showEditTitleDialog = true },
                    shape = QuickExpenseThemeTokens.RadiusPill,
                    color = QuickExpenseThemeTokens.SurfaceWhite,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, Color(0xFFFFD9CE)),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFFF7F3),
                                        Color(0xFFFDECE5)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = expenseCategoryTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuickExpenseThemeTokens.PrimaryDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit Category",
                            tint = QuickExpenseThemeTokens.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Massive 56sp DisplayLarge Amount Field
                Text(
                    text = formattedDisplay,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary,
                    letterSpacing = (-1.5).sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 62.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Real-time Remainder Allocation Badge
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
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (memberCount > 0) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = if (memberCount > 0) QuickExpenseThemeTokens.SageText else QuickExpenseThemeTokens.TerracottaText,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (memberCount > 0) {
                                "$currencySymbol ${NumberFormat.getNumberInstance(Locale("en", "IN")).format(perPerson)} / person · $memberCount splitting" +
                                        if (remainder > 0L) " · (+$currencySymbol$remainder remainder safe)" else " · exact"
                            } else {
                                "Select at least 1 person to split"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (memberCount > 0) QuickExpenseThemeTokens.SageText else QuickExpenseThemeTokens.TerracottaText
                        )
                    }
                }
            }

            // ==================================================================
            // 2. MID SECTION: Overlapping M3 Profile Structure & Multi-Selector
            // ==================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Who's in on this?",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                        Text(
                            text = "Tap avatar to include · Long-press friend to edit UPI ID",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }

                    // Select All / Reset Pill Button
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
                        modifier = Modifier.sizeIn(minHeight = 36.dp)
                    ) {
                        Text(
                            text = if (selectedMemberIds.size == participants.size) "Clear" else "Select All (${participants.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuickExpenseThemeTokens.SageText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Overlapping avatar strip with 2dp white borders and thick Sage border on active
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(participants) { person ->
                        val isSelected = selectedMemberIds.contains(person.id)
                        val matchingRoomMember = activeMembers.find { it.memberId == person.id }

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
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .shadow(
                                        elevation = if (isSelected) 6.dp else 1.dp,
                                        shape = CircleShape
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) person.avatarBg else person.avatarBg.copy(alpha = 0.45f)
                                    )
                                    .border(
                                        width = if (isSelected) 3.5.dp else 2.dp,
                                        color = if (isSelected) QuickExpenseThemeTokens.SageBorder else Color.White,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = person.initials,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) person.avatarFg else person.avatarFg.copy(alpha = 0.45f)
                                )

                                // Active Checkmark Badge
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(QuickExpenseThemeTokens.SageBorder)
                                            .border(2.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = person.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) textPrimary else textSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // ==================================================================
            // 3. BOTTOM SECTION: Tactile M3 Squarcles Keypad + Giant FAB
            // ==================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left: 3x4 Keypad Column Grid
                    Column(
                        modifier = Modifier.weight(3f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Row 1: 1, 2, 3
                        KeypadRow(
                            keys = listOf("1", "2", "3"),
                            keypadBg = keypadBg,
                            keypadBorder = keypadBorder,
                            textPrimary = textPrimary,
                            onKeyPress = { key ->
                                appendDigit(key, amountDigits) { amountDigits = it }
                            }
                        )

                        // Row 2: 4, 5, 6
                        KeypadRow(
                            keys = listOf("4", "5", "6"),
                            keypadBg = keypadBg,
                            keypadBorder = keypadBorder,
                            textPrimary = textPrimary,
                            onKeyPress = { key ->
                                appendDigit(key, amountDigits) { amountDigits = it }
                            }
                        )

                        // Row 3: 7, 8, 9
                        KeypadRow(
                            keys = listOf("7", "8", "9"),
                            keypadBg = keypadBg,
                            keypadBorder = keypadBorder,
                            textPrimary = textPrimary,
                            onKeyPress = { key ->
                                appendDigit(key, amountDigits) { amountDigits = it }
                            }
                        )

                        // Row 4: 00, 0, ⌫
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TactileSquircleKey(
                                label = "00",
                                keypadBg = keypadBg,
                                keypadBorder = keypadBorder,
                                textPrimary = textPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp),
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
                                    .height(60.dp),
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
                                icon = Icons.Rounded.Backspace,
                                keypadBg = keypadBg,
                                keypadBorder = keypadBorder,
                                textPrimary = textPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (amountDigits.isNotEmpty()) {
                                        amountDigits = amountDigits.dropLast(1)
                                    }
                                }
                            )
                        }
                    }

                    // Right: 4th Column (Receipt / Category Button & Giant Split & Save FAB)
                    Column(
                        modifier = Modifier
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Key 1: Dedicated "Receipt / Title" Key in Keypad Grid (Sage Tint #EAF3DC)
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showEditTitleDialog = true
                                onScanReceiptClick()
                            },
                            shape = QuickExpenseThemeTokens.RadiusKeySquircle,
                            color = QuickExpenseThemeTokens.SageSurface,
                            shadowElevation = 3.dp,
                            border = BorderStroke(1.dp, QuickExpenseThemeTokens.AccentSage),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ReceiptLong,
                                    contentDescription = "Edit Note",
                                    tint = QuickExpenseThemeTokens.SageText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Note",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = QuickExpenseThemeTokens.SageText
                                )
                            }
                        }

                        // Giant Extended "Split & Save" FAB taking bottom-right space
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
                                containerColor = QuickExpenseThemeTokens.PrimaryDark,
                                contentColor = Color.White
                            ),
                            enabled = numericVal > 0L && selectedMemberIds.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .shadow(8.dp, shape = QuickExpenseThemeTokens.RadiusKeySquircle),
                            contentPadding = PaddingValues(6.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Glowing Accent Sage Lightning Bolt
                                Surface(
                                    shape = CircleShape,
                                    color = QuickExpenseThemeTokens.AccentSage.copy(alpha = 0.25f),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.ElectricBolt,
                                            contentDescription = null,
                                            tint = QuickExpenseThemeTokens.AccentSage,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Split &\nSave",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
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
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        keys.forEach { key ->
            TactileSquircleKey(
                label = key,
                keypadBg = keypadBg,
                keypadBorder = keypadBorder,
                textPrimary = textPrimary,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
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
// USER GUIDE BOTTOM SHEET (Triggered by '?' Help Icon in Top Bar)
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserGuideBottomSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QuickExpenseThemeTokens.ScreenBg,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = QuickExpenseThemeTokens.AccentSage,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = QuickExpenseThemeTokens.SageText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "How SplitMate Works",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = QuickExpenseThemeTokens.PrimaryDark
                        )
                        Text(
                            text = "Zero-Drift Quick Splits & UPI Guide",
                            fontSize = 12.sp,
                            color = QuickExpenseThemeTokens.TextSecondary
                        )
                    }
                }
            }

            GuideStepItem(
                stepNumber = "1",
                title = "Create a Trip or Group Ledger",
                subtitle = "Tap '+ Create New Group' on Ledgers, name your trip, and add your friends. Long-press any friend's avatar or tap 'Edit UPI' to save their UPI ID (e.g. rahul@okaxis).",
                icon = Icons.Rounded.GroupAdd,
                badgeColor = Color(0xFFD7E8B6),
                badgeTextColor = Color(0xFF2D4810)
            )

            GuideStepItem(
                stepNumber = "2",
                title = "Tap Avatars & Enter Total",
                subtitle = "Open the Split tab, enter the total amount on the tactile squircle keypad, and tap the avatars of everyone sharing the bill. SplitMate divides the total equally with 0.00¢ drift.",
                icon = Icons.Rounded.Calculate,
                badgeColor = Color(0xFFFFD8CC),
                badgeTextColor = Color(0xFF8A2E1A)
            )

            GuideStepItem(
                stepNumber = "3",
                title = "Greedy Debt Simplification & UPI",
                subtitle = "Open the Settle tab to collapse circular debts into minimum transfers. Tap 'Pay via UPI' to launch Google Pay, PhonePe, or Paytm directly with your friend's custom UPI ID.",
                icon = Icons.Rounded.ElectricBolt,
                badgeColor = Color(0xFFD0E2FF),
                badgeTextColor = Color(0xFF143E82)
            )

            GuideStepItem(
                stepNumber = "4",
                title = "160+ World Currencies & Offline Vault",
                subtitle = "Switch between 160+ live currencies anytime via the Currency Bottom Sheet. All ledgers and rates are cached in your local SQLite Room Vault.",
                icon = Icons.Rounded.CurrencyExchange,
                badgeColor = Color(0xFFE5DCFF),
                badgeTextColor = Color(0xFF452285)
            )

            Button(
                onClick = onDismiss,
                shape = QuickExpenseThemeTokens.RadiusPill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuickExpenseThemeTokens.PrimaryDark,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text("Got It, Let's Split!", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun GuideStepItem(
    stepNumber: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    badgeTextColor: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, QuickExpenseThemeTokens.BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = stepNumber,
                        tint = badgeTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$stepNumber. $title",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = QuickExpenseThemeTokens.PrimaryDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = QuickExpenseThemeTokens.TextSecondary,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

// ==============================================================================
// 160+ WORLD CURRENCIES MODAL BOTTOM SHEET WITH CIRCULAR SYMBOL ICONS
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyModalBottomSheet(
    currencies: List<CurrencyRateEntity>,
    activeCurrencyCode: String,
    onSelectCurrency: (String) -> Unit,
    onSyncLiveRates: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(currencies, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) currencies
        else currencies.filter {
            it.currencyCode.lowercase().contains(q) ||
                it.currencyName.lowercase().contains(q) ||
                it.symbol.lowercase().contains(q)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QuickExpenseThemeTokens.ScreenBg,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .animateContentSize()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "World Currencies (${currencies.size})",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = QuickExpenseThemeTokens.PrimaryDark
                    )
                    Text(
                        text = "Live Open Exchange Rates API · Offline Room Cache",
                        fontSize = 12.sp,
                        color = QuickExpenseThemeTokens.TextSecondary
                    )
                }
                TextButton(onClick = onSyncLiveRates) {
                    Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search 160+ currencies (e.g. INR, USD, JPY, AED)") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filtered, key = { it.currencyCode }) { rate ->
                    val isSelected = rate.currencyCode == activeCurrencyCode
                    val paletteIdx = kotlin.math.abs(rate.currencyCode.hashCode()) % ParticipantPalette.size
                    val (circleBg, circleFg) = ParticipantPalette[paletteIdx]

                    Surface(
                        onClick = {
                            onSelectCurrency(rate.currencyCode)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) QuickExpenseThemeTokens.SageSurface else Color.White,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) QuickExpenseThemeTokens.SageBorder else QuickExpenseThemeTokens.BorderLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Premium Circular Symbol Icon (No Emoji Flags)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(circleBg)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rate.symbol.take(3),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = circleFg
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${rate.currencyCode} · ${rate.currencyName}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = QuickExpenseThemeTokens.PrimaryDark
                                )
                                Text(
                                    text = "1 ${rate.baseCurrency} = ${String.format(Locale.US, "%.4f", rate.rateFromBase)} ${rate.currencyCode}",
                                    fontSize = 12.sp,
                                    color = QuickExpenseThemeTokens.TextSecondary
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = QuickExpenseThemeTokens.SageBorder,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// EDIT FRIEND UPI ID DIALOG (Point 3)
// ==============================================================================
@Composable
fun EditFriendUpiDialog(
    member: GroupMemberEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, upiId: String) -> Unit
) {
    var friendName by remember(member) { mutableStateOf(member.name) }
    var friendUpi by remember(member) {
        val fallbackUpi = member.upiId.ifBlank {
            "${member.name.lowercase().replace(Regex("[^a-z0-9]"), "")}@okhdfcbank"
        }
        mutableStateOf(fallbackUpi)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = QuickExpenseThemeTokens.ScreenBg,
        title = {
            Text(
                text = "Edit Friend & UPI ID",
                fontWeight = FontWeight.ExtraBold,
                color = QuickExpenseThemeTokens.PrimaryDark
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Set ${member.name}'s verified UPI VPA so 1-tap 'Pay via UPI' routes directly to their bank account.",
                    fontSize = 12.sp,
                    color = QuickExpenseThemeTokens.TextSecondary
                )
                OutlinedTextField(
                    value = friendName,
                    onValueChange = { friendName = it },
                    label = { Text("Friend's Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = friendUpi,
                    onValueChange = { friendUpi = it },
                    label = { Text("Friend's UPI ID (e.g. name@okaxis)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(friendName, friendUpi) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuickExpenseThemeTokens.PrimaryDark,
                    contentColor = Color.White
                )
            ) {
                Text("Save UPI ID", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = QuickExpenseThemeTokens.TextSecondary)
            }
        }
    )
}
