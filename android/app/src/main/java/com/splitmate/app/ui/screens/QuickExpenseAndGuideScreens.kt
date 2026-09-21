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
import com.splitmate.app.ui.SplitMateBrandFontFamily
import com.splitmate.app.ui.SplitMateDisplayFontFamily
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.buildDiceBearOpenPeepsUrl
import java.text.NumberFormat
import java.util.Locale

// ==============================================================================
// SPLITMATE MATERIAL 3 EXPRESSIVE THEME TOKENS (DARK-MODE ADAPTIVE)
// ==============================================================================
object QuickExpenseThemeTokens {
    val ScreenBg: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF121212) else Color(0xFFFAF7F2)
    val PrimaryDark: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFFAF7F2) else Color(0xFF23201E)
    val AccentSage = Color(0xFFD7E8B6)             // Active Accent / Glowing Bolt
    val SageSurface = Color(0xFFEAF3DC)            // Soft Sage Surface (Note Key / Badge)
    val SageText = Color(0xFF2D4810)               // Deep Forest Green Text
    val SageBorder = Color(0xFF5A8E24)             // Selected Avatar High-Contrast Border
    val TerracottaSurface = Color(0xFFFCECE7)      // Warning / Debit Surface
    val TerracottaText = Color(0xFFC23E2A)         // Terracotta Text
    val SurfaceWhite: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF1E1D1B) else Color(0xFFFFFFFF)
    val SurfaceKeypad: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF242220) else Color(0xFFF3EFEA)
    val SurfaceKeypadBorder: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF38332D) else Color(0xFFE8E2D8)
    val BorderLight: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF38332D) else Color(0xFFE5DFC5)
    val TextSecondary: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFB5ADA3) else Color(0xFF7A746D)

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
    val avatarSeed: String,
    val avatarBg: Color,
    val avatarFg: Color,
    val upiId: String = ""
)

val DefaultParticipants = listOf(
    QuickParticipant("1", "You", "AK", "Akshay|Masculine", Color(0xFFD7E8B6), Color(0xFF23201E)),
    QuickParticipant("2", "Maya", "ML", "Maya|Feminine", Color(0xFFFFD8CC), Color(0xFF8A2E1A)),
    QuickParticipant("3", "Sam", "SK", "Sam|Masculine", Color(0xFFD0E2FF), Color(0xFF143E82)),
    QuickParticipant("4", "Priya", "PR", "Priya|Feminine", Color(0xFFFFD5E5), Color(0xFF801844))
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
    val isDark = uiState?.isDarkTheme == true || SplitMateTheme.isDark
    val screenBg = if (isDark) Color(0xFF121212) else Color(0xFFFAF7F2)
    val surfaceColor = if (isDark) Color(0xFF1E1D1B) else Color(0xFFFFFFFF)
    val textPrimary = if (isDark) Color(0xFFFAF7F2) else Color(0xFF23201E)
    val textSecondary = if (isDark) Color(0xFFB5ADA3) else Color(0xFF7A746D)
    val keypadBg = if (isDark) Color(0xFF242220) else Color(0xFFF3EFEA)
    val keypadBorder = if (isDark) Color(0xFF38332D) else Color(0xFFE8E2D8)

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
                    avatarSeed = m.avatarSeed,
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

    val currencySymbol = "₹"
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
            title = {
                Text(
                    "Log Expense Description",
                    fontFamily = SplitMateDisplayFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )
            },
            text = {
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
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = surfaceColor,
                            border = BorderStroke(1.dp, keypadBorder),
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
                    // Locked Native INR & Exact Split badge (No '?' Help icon)
                    Surface(
                        shape = QuickExpenseThemeTokens.RadiusPill,
                        color = surfaceColor.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, keypadBorder),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "₹ INR • Exact Split",
                            fontFamily = SplitMateBrandFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
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
                    color = surfaceColor,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF4A332C) else Color(0xFFFFD9CE)),
                    modifier = Modifier.padding(bottom = 12.dp)
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
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = expenseCategoryTitle,
                            fontFamily = SplitMateBrandFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit Category",
                            tint = textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Massive 56sp DisplayLarge Amount Field (Outfit Font)
                Text(
                    text = formattedDisplay,
                    fontFamily = SplitMateDisplayFontFamily,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary,
                    letterSpacing = (-1.5).sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 62.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                            fontFamily = SplitMateDisplayFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                        Text(
                            text = "Tap avatar to include · Long-press friend to link Contact UPI",
                            fontFamily = SplitMateBrandFontFamily,
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
                            fontFamily = SplitMateBrandFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuickExpenseThemeTokens.SageText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Avatar strip with DiceBear Open-Peeps SVG & Presentation Style support
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(participants) { person ->
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
                                        color = if (isSelected) QuickExpenseThemeTokens.SageBorder else surfaceColor,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = person.initials,
                                    fontFamily = SplitMateDisplayFontFamily,
                                    fontSize = 16.sp,
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

                                // Active Checkmark Badge
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(QuickExpenseThemeTokens.SageBorder)
                                            .border(2.dp, surfaceColor, CircleShape),
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
                                fontFamily = SplitMateBrandFontFamily,
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

                    // Right: 4th Column (Note / Category Button & Giant Split & Save FAB)
                    Column(
                        modifier = Modifier
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Key 1: Dedicated "Log Expense Note" Key in Keypad Grid (Sage Tint #EAF3DC)
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showEditTitleDialog = true
                                onLogExpenseClick()
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
                                    fontFamily = SplitMateBrandFontFamily,
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
                                containerColor = textPrimary,
                                contentColor = screenBg
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
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDark) Color(0xFF416913).copy(alpha = 0.35f) else QuickExpenseThemeTokens.AccentSage.copy(alpha = 0.25f),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.ElectricBolt,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF416913) else QuickExpenseThemeTokens.AccentSage,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Log &\nSplit",
                                    fontFamily = SplitMateDisplayFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = screenBg,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 17.sp
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
// EDIT FRIEND PERSONA & NATIVE CONTACTS UPI DIALOG (Points 1 & 4)
// ==============================================================================
@Composable
fun EditFriendUpiDialog(
    member: GroupMemberEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, upiId: String, avatarSeed: String) -> Unit
) {
    val context = LocalContext.current
    val initialStyle = remember(member.avatarSeed) {
        member.avatarSeed.substringAfter('|', "Neutral").takeIf {
            it in listOf("Masculine", "Feminine", "Neutral")
        } ?: "Neutral"
    }
    var friendName by remember(member) { mutableStateOf(member.name) }
    var selectedPresentationStyle by remember(member) { mutableStateOf(initialStyle) }
    var selectedUpiHandleSuffix by remember { mutableStateOf("upi") }

    val initialCleanDigits = remember(member.upiId) {
        member.upiId.substringBefore('@').replace(Regex("[^0-9]"), "")
    }
    var pickedPhoneNumber by remember(member) { mutableStateOf(initialCleanDigits) }
    var friendUpi by remember(member, pickedPhoneNumber, selectedUpiHandleSuffix) {
        val resolved = if (pickedPhoneNumber.isNotEmpty()) {
            "$pickedPhoneNumber@$selectedUpiHandleSuffix"
        } else {
            member.upiId.ifBlank {
                "${member.name.lowercase().replace(Regex("[^a-z0-9]"), "")}@upi"
            }
        }
        mutableStateOf(resolved)
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        if (contactUri != null) {
            val extracted = extractPhoneAndNameFromContactUri(context, contactUri)
            if (extracted != null) {
                val (contactName, cleanDigits) = extracted
                if (contactName.isNotBlank()) {
                    friendName = contactName
                }
                if (cleanDigits.isNotBlank()) {
                    pickedPhoneNumber = cleanDigits
                    friendUpi = "$cleanDigits@$selectedUpiHandleSuffix"
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            contactPickerLauncher.launch(null)
        }
    }

    val avatarPreviewUrl = remember(friendName, selectedPresentationStyle) {
        buildDiceBearOpenPeepsUrl(friendName.ifBlank { member.name }, selectedPresentationStyle)
    }

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
                        text = "Friend Profile & UPI",
                        fontFamily = SplitMateDisplayFontFamily,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = QuickExpenseThemeTokens.PrimaryDark
                    )
                    Text(
                        text = "Avatar Style & Native Contact Link",
                        fontFamily = SplitMateBrandFontFamily,
                        fontSize = 12.sp,
                        color = QuickExpenseThemeTokens.TextSecondary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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

                // 3. Pick from Contacts Button (Replaces manual UPI ID text input)
                Button(
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            contactPickerLauncher.launch(null)
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
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContactPhone,
                        contentDescription = null,
                        tint = QuickExpenseThemeTokens.SageText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pick from Contacts",
                        fontFamily = SplitMateBrandFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = QuickExpenseThemeTokens.SageText
                    )
                }

                // 4. Resolved Mobile UPI VPA Preview Card + @upi / @paytm Suffix Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = QuickExpenseThemeTokens.SurfaceKeypad,
                    border = BorderStroke(1.dp, QuickExpenseThemeTokens.BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Linked UPI Handle",
                                    fontFamily = SplitMateBrandFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = QuickExpenseThemeTokens.TextSecondary
                                )
                                Text(
                                    text = friendUpi,
                                    fontFamily = SplitMateDisplayFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = QuickExpenseThemeTokens.PrimaryDark
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("upi", "paytm").forEach { suffix ->
                                    val active = selectedUpiHandleSuffix == suffix
                                    Surface(
                                        onClick = {
                                            selectedUpiHandleSuffix = suffix
                                            val base = pickedPhoneNumber.ifEmpty {
                                                friendUpi.substringBefore('@')
                                            }
                                            friendUpi = "$base@$suffix"
                                        },
                                        shape = QuickExpenseThemeTokens.RadiusPill,
                                        color = if (active) QuickExpenseThemeTokens.PrimaryDark else Color.Transparent,
                                        border = BorderStroke(1.dp, QuickExpenseThemeTokens.BorderLight)
                                    ) {
                                        Text(
                                            text = "@$suffix",
                                            fontFamily = SplitMateBrandFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) QuickExpenseThemeTokens.ScreenBg else QuickExpenseThemeTokens.TextSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = friendName.trim().ifEmpty { member.name }
                    val styledSeed = "$cleanName|$selectedPresentationStyle"
                    onSave(cleanName, friendUpi, styledSeed)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuickExpenseThemeTokens.PrimaryDark,
                    contentColor = QuickExpenseThemeTokens.ScreenBg
                )
            ) {
                Text(
                    "Save Friend & UPI",
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

/**
 * Queries Android's ContentResolver for a picked Contact URI, retrieves their Display Name
 * and Phone Number, strips all non-digit characters (spaces, dashes, '+91'), and returns
 * `(displayName, cleanMobileDigits)`.
 */
private fun extractPhoneAndNameFromContactUri(
    context: Context,
    contactUri: Uri
): Pair<String, String>? {
    return try {
        var contactId = ""
        var displayName = ""
        context.contentResolver.query(
            contactUri,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                if (idIdx >= 0) contactId = cursor.getString(idIdx).orEmpty()
                if (nameIdx >= 0) displayName = cursor.getString(nameIdx).orEmpty()
            }
        }

        var rawNumber = ""
        if (contactId.isNotEmpty()) {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { phoneCursor ->
                if (phoneCursor.moveToFirst()) {
                    val numIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numIdx >= 0) rawNumber = phoneCursor.getString(numIdx).orEmpty()
                }
            }
        }

        val digitsOnly = rawNumber.replace(Regex("[^0-9]"), "")
        val normalizedTenDigits = when {
            digitsOnly.length == 12 && digitsOnly.startsWith("91") -> digitsOnly.substring(2)
            digitsOnly.length == 11 && digitsOnly.startsWith("0") -> digitsOnly.substring(1)
            else -> digitsOnly
        }
        displayName to normalizedTenDigits
    } catch (e: Exception) {
        null
    }
}
