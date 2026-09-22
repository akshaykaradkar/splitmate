package com.splitmate.app.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.splitmate.app.SplitMateTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.R

// ==============================================================================
// CANONICAL BINDINGS TO THE 4 GOOGLE DESIGN SYSTEMS (`design_systems/`)
// ==============================================================================
object DesignSystemBindings {
    // 1. Google Material 3 (`design_systems/google-material-3/DESIGN.md`)
    val GM3LightBackground = Color(0xFFFAF7F2)
    val GM3LightCardSurface = Color(0xFFFFFFFF)
    val GM3LightKeypadSurface = Color(0xFFF3EFEA)
    val GM3LightPrimaryText = Color(0xFF23201E)
    val GM3LightSubtitleText = Color(0xFF6E6863)

    val GM3DarkBackground = Color(0xFF121212)
    val GM3DarkCardSurface = Color(0xFF1E1E1E)
    val GM3DarkKeypadSurface = Color(0xFF2A2A2A)
    val GM3DarkPrimaryText = Color(0xFFFAF7F2)
    val GM3DarkSubtitleText = Color(0xFFA09890)
    val GM3DarkBorder = Color(0xFF343535)

    val GM3ShapeExtraLarge = RoundedCornerShape(28.dp)
    val GM3ShapeLarge = RoundedCornerShape(24.dp)
    val GM3ShapePill = RoundedCornerShape(50)

    // 2. Android Motion (`design_systems/android-motion/DESIGN.md`)
    fun <T> themeColorTween() = tween<T>(durationMillis = 400, easing = FastOutSlowInEasing)
    fun <T> tactileSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // 3. Elements GM3 (`design_systems/elements-3/DESIGN.md`)
    val ElementsCreditorContainer = Color(0xFFD7E8B6)
    val ElementsCreditorOnContainer = Color(0xFF2D4810)
    val ElementsDebtorContainer = Color(0xFFFED8C8)
    val ElementsDebtorOnContainer = Color(0xFF7C2D12)
    val ElementsInfoPeriwinkle = Color(0xFFDCE3FD)

    val ElementsPositiveContainer = Color(0xFFD7E8B6)
    val ElementsPositiveText = Color(0xFF416913)
    val ElementsNegativeText = Color(0xFFE06B52)

    // 4. Android Pixel Design System (`design_systems/android-pixel-design-system/DESIGN.md`)
    val PixelCardInternalPadding = 14.dp
    val PixelSectionSpacing = 12.dp
    val PixelCompactItemSpacing = 8.dp
}

// Stitch "Organic Tactile Financial" (Buckwheat) + HCT Expressive Tokens
val BuckwheatCanvas = Color(0xFFFAF6F0)
val BuckwheatSurface = Color(0xFFFFFFFF)
val BuckwheatSunken = Color(0xFFF4EFE6)
val BuckwheatCharcoal = Color(0xFF23201E)
val BuckwheatSecondaryText = Color(0xFF6E675F)
val BuckwheatBorder = Color(0xFFEDE7DF)

val BuckwheatOlivePrimary = Color(0xFF365314)
val BuckwheatSageContainer = Color(0xFFD7E8B6)
val BuckwheatTerracotta = Color(0xFFE06B52)
val BuckwheatPeachContainer = Color(0xFFFED8C8)
val BuckwheatTerracottaDark = Color(0xFF7C2D12)
val BuckwheatLavenderContainer = Color(0xFFDCE3FD)
val BuckwheatLavenderText = Color(0xFF3730A3)

private val SplitMateLightColorScheme = lightColorScheme(
    primary = BuckwheatOlivePrimary,
    onPrimary = Color.White,
    primaryContainer = BuckwheatSageContainer,
    onPrimaryContainer = BuckwheatOlivePrimary,
    secondary = BuckwheatTerracotta,
    onSecondary = Color.White,
    secondaryContainer = BuckwheatPeachContainer,
    onSecondaryContainer = BuckwheatTerracottaDark,
    tertiary = BuckwheatLavenderText,
    onTertiary = Color.White,
    tertiaryContainer = BuckwheatLavenderContainer,
    onTertiaryContainer = BuckwheatLavenderText,
    background = DesignSystemBindings.GM3LightBackground,
    onBackground = DesignSystemBindings.GM3LightPrimaryText,
    surface = DesignSystemBindings.GM3LightCardSurface,
    onSurface = DesignSystemBindings.GM3LightPrimaryText,
    surfaceVariant = BuckwheatSunken,
    onSurfaceVariant = DesignSystemBindings.GM3LightSubtitleText,
    outline = BuckwheatBorder
)

private val SplitMateDarkColorScheme = darkColorScheme(
    primary = Color(0xFFAED581),
    onPrimary = Color(0xFF1B3300),
    primaryContainer = Color(0xFF2A420E),
    onPrimaryContainer = BuckwheatSageContainer,
    secondary = Color(0xFFFFB4A1),
    onSecondary = Color(0xFF561E0F),
    secondaryContainer = Color(0xFF733423),
    onSecondaryContainer = BuckwheatPeachContainer,
    tertiary = Color(0xFFBDC5FF),
    onTertiary = Color(0xFF1E2678),
    tertiaryContainer = Color(0xFF353E90),
    onTertiaryContainer = BuckwheatLavenderContainer,
    background = DesignSystemBindings.GM3DarkBackground,
    onBackground = DesignSystemBindings.GM3DarkPrimaryText,
    surface = DesignSystemBindings.GM3DarkCardSurface,
    onSurface = DesignSystemBindings.GM3DarkPrimaryText,
    surfaceVariant = DesignSystemBindings.GM3DarkKeypadSurface,
    onSurfaceVariant = DesignSystemBindings.GM3DarkSubtitleText,
    outline = DesignSystemBindings.GM3DarkBorder
)

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val figtreeGoogleFont = GoogleFont("Figtree")

val FigtreeFontFamily = FontFamily(
    Font(googleFont = figtreeGoogleFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_regular, weight = FontWeight.Normal),
    Font(googleFont = figtreeGoogleFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_medium, weight = FontWeight.Medium),
    Font(googleFont = figtreeGoogleFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_semibold, weight = FontWeight.SemiBold),
    Font(googleFont = figtreeGoogleFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_bold, weight = FontWeight.Bold),
    Font(googleFont = figtreeGoogleFont, fontProvider = fontProvider, weight = FontWeight.ExtraBold),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_extrabold, weight = FontWeight.ExtraBold)
)

val PlusJakartaSansFont = FigtreeFontFamily
val JetBrainsMonoFont = FigtreeFontFamily

val SplitMateTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = "tnum"
    ),
    displayMedium = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = "tnum"
    ),
    displaySmall = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-1.0).sp,
        fontFeatureSettings = "tnum"
    ),
    headlineLarge = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum"
    ),
    headlineMedium = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp,
        fontFeatureSettings = "tnum"
    ),
    headlineSmall = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontFeatureSettings = "tnum"
    ),
    titleMedium = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum"
    ),
    labelSmall = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )
)

/**
 * Extracts strictly the first 1 or 2 uppercase letters of the person's ACTUAL NAME.
 * Never renders "Masculine", "Feminine", "Neutral", or numeric suffixes.
 */
fun extractInitialsFromNameOrSeed(rawNameOrSeed: String): String {
    val basePart = rawNameOrSeed
        .substringBefore("|")
        .substringBefore("_")
        .replace("(You)", "", ignoreCase = true)
        .replace("Masculine", "", ignoreCase = true)
        .replace("Feminine", "", ignoreCase = true)
        .replace("Neutral", "", ignoreCase = true)
        .trim()

    val words = basePart.split(Regex("\\s+")).filter { it.isNotBlank() && it.first().isLetter() }
    return when {
        words.size >= 2 -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
        words.size == 1 -> words[0].take(1).uppercase()
        else -> basePart.firstOrNull { it.isLetter() }?.uppercaseChar()?.toString() ?: "S"
    }
}

/**
 * Builds a gender-aware DiceBear 9.x `open-peeps` SVG URL.
 */
fun buildDiceBearOpenPeepsUrl(rawSeed: String, styleOverride: String? = null): String {
    val parts = rawSeed.split("|")
    val seedBase = parts.firstOrNull()?.trim()?.ifEmpty { "Explorer" } ?: "Explorer"
    val style = (styleOverride ?: parts.getOrNull(1)?.trim() ?: "Neutral").lowercase()
    val headParam = when (style) {
        "masculine" -> "&head=flatTop,short1,short2,short3,short4,short5,pomp&maskProbability=0"
        "feminine" -> "&head=long,longBangs,longCurly,bun,bun2,buns,bangs,mediumStraight&facialHairProbability=0&maskProbability=0"
        else -> "&head=medium1,medium2,medium3,afro,twists,hatBeanie&facialHairProbability=0&maskProbability=0"
    }
    return "https://api.dicebear.com/9.x/open-peeps/svg?seed=${Uri.encode(seedBase)}&backgroundColor=d7e8b6,fed8c8,dce3fd$headParam"
}

/**
 * 8 Expressive Material 3 Category Icons for Group Creation & Group Cards.
 */
data class GroupCategoryIconOption(
    val id: String,
    val label: String,
    val icon: ImageVector
) {
    val key: String get() = id
}

val GroupCategoryIcons: List<GroupCategoryIconOption> = listOf(
    GroupCategoryIconOption("Flight", "Trip", Icons.Rounded.Flight),
    GroupCategoryIconOption("Cabin", "Cabin", Icons.Rounded.Cabin),
    GroupCategoryIconOption("Home", "Home", Icons.Rounded.Home),
    GroupCategoryIconOption("Restaurant", "Dining", Icons.Rounded.Restaurant),
    GroupCategoryIconOption("LocalCafe", "Coffee", Icons.Rounded.LocalCafe),
    GroupCategoryIconOption("LocalBar", "Party", Icons.Rounded.LocalBar),
    GroupCategoryIconOption("ShoppingCart", "Groceries", Icons.Rounded.ShoppingCart),
    GroupCategoryIconOption("Celebration", "Event", Icons.Rounded.Celebration)
)

fun resolveGroupCategoryIcon(iconName: String, fallbackGroupName: String = ""): ImageVector {
    val matched = GroupCategoryIcons.find { it.id.equals(iconName, ignoreCase = true) }
    if (matched != null) return matched.icon
    return when {
        fallbackGroupName.contains("Cabin", ignoreCase = true) -> Icons.Rounded.Cabin
        fallbackGroupName.contains("Apt", ignoreCase = true) ||
            fallbackGroupName.contains("Room", ignoreCase = true) ||
            fallbackGroupName.contains("Home", ignoreCase = true) -> Icons.Rounded.Home
        fallbackGroupName.contains("Cafe", ignoreCase = true) ||
            fallbackGroupName.contains("Coffee", ignoreCase = true) -> Icons.Rounded.LocalCafe
        fallbackGroupName.contains("Party", ignoreCase = true) ||
            fallbackGroupName.contains("Drinks", ignoreCase = true) -> Icons.Rounded.LocalBar
        fallbackGroupName.contains("Grocer", ignoreCase = true) -> Icons.Rounded.ShoppingCart
        fallbackGroupName.contains("Event", ignoreCase = true) -> Icons.Rounded.Celebration
        fallbackGroupName.contains("Trip", ignoreCase = true) ||
            fallbackGroupName.contains("Goa", ignoreCase = true) -> Icons.Rounded.Flight
        else -> Icons.Rounded.Restaurant
    }
}

// ==============================================================================
// IN-APP DEVICE CONTACT MODEL, PHONE CLEANER & MULTI-SELECT MODAL BOTTOM SHEET
// ==============================================================================

data class DeviceContact(
    val name: String,
    val cleanPhone: String,
    val formattedPhone: String
)

fun cleanIndianTenDigitPhone(rawNumber: String): String {
    val digitsOnly = rawNumber.replace(Regex("[^0-9]"), "")
    return when {
        digitsOnly.length >= 12 && digitsOnly.startsWith("91") -> digitsOnly.substring(2).takeLast(10)
        digitsOnly.length == 11 && digitsOnly.startsWith("0") -> digitsOnly.substring(1)
        digitsOnly.length > 10 -> digitsOnly.takeLast(10)
        else -> digitsOnly
    }
}

fun formatTenDigitIndianPhone(cleanPhone: String): String {
    return if (cleanPhone.length == 10) {
        "+91 ${cleanPhone.substring(0, 5)} ${cleanPhone.substring(5)}"
    } else {
        cleanPhone
    }
}

fun queryAllDeviceContacts(context: Context): List<DeviceContact> {
    val contactsByPhone = linkedMapOf<String, DeviceContact>()
    try {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val rawName = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty().trim() else ""
                val rawNumber = if (numberIndex >= 0) cursor.getString(numberIndex).orEmpty().trim() else ""
                val clean10 = cleanIndianTenDigitPhone(rawNumber)
                if (rawName.isNotEmpty() && clean10.length == 10 && !contactsByPhone.containsKey(clean10)) {
                    contactsByPhone[clean10] = DeviceContact(
                        name = rawName,
                        cleanPhone = clean10,
                        formattedPhone = formatTenDigitIndianPhone(clean10)
                    )
                }
            }
        }
    } catch (_: SecurityException) {
    } catch (_: Exception) {
    }
    return contactsByPhone.values.toList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerBottomSheet(
    contacts: List<DeviceContact>,
    isLoading: Boolean = false,
    multiSelect: Boolean = true,
    preSelectedPhones: Set<String> = emptySet(),
    initialSelectedPhones: Set<String> = preSelectedPhones,
    title: String = "Add Members from Contacts",
    subtitle: String = "Select friends from your phonebook",
    onDismissRequest: () -> Unit = {},
    onDismiss: () -> Unit = onDismissRequest,
    onConfirmSelected: (List<DeviceContact>) -> Unit = {},
    onConfirmSelection: (List<DeviceContact>) -> Unit = onConfirmSelected
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedPhones by remember(initialSelectedPhones) {
        mutableStateOf(initialSelectedPhones.toSet())
    }

    val filteredContacts = remember(contacts, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            contacts
        } else {
            contacts.filter {
                it.name.lowercase().contains(q) || it.cleanPhone.contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SplitMateTheme.ScreenBg,
        contentColor = SplitMateTheme.PrimaryDark,
        scrimColor = Color.Black.copy(alpha = 0.72f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Surface(
            color = SplitMateTheme.ScreenBg,
            contentColor = SplitMateTheme.PrimaryDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SplitMateTheme.ScreenBg)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = SplitMateTheme.PrimaryDark
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SplitMateTheme.TextSecondary
                        )
                    }
                    if (multiSelect && contacts.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                selectedPhones = if (selectedPhones.size == filteredContacts.size && filteredContacts.isNotEmpty()) {
                                    emptySet()
                                } else {
                                    selectedPhones + filteredContacts.map { it.cleanPhone }
                                }
                            }
                        ) {
                            Text(
                                text = if (selectedPhones.size == filteredContacts.size && filteredContacts.isNotEmpty()) {
                                    "Clear"
                                } else {
                                    "Select All"
                                },
                                fontWeight = FontWeight.Bold,
                                color = SplitMateTheme.SageText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search by name or 10-digit phone...",
                            color = SplitMateTheme.TextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search contacts",
                            tint = SplitMateTheme.TextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    tint = SplitMateTheme.TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SplitMateTheme.PrimaryDark,
                        unfocusedTextColor = SplitMateTheme.PrimaryDark,
                        focusedBorderColor = SplitMateTheme.PrimaryDark,
                        unfocusedBorderColor = SplitMateTheme.BorderLight,
                        focusedContainerColor = SplitMateTheme.SurfaceWhite,
                        unfocusedContainerColor = SplitMateTheme.SurfaceWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SplitMateTheme.PrimaryDark)
                    }
                } else if (filteredContacts.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = SplitMateTheme.SurfaceWhite,
                        border = BorderStroke(1.dp, SplitMateTheme.BorderLight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Contacts,
                                contentDescription = null,
                                tint = SplitMateTheme.TextSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = if (contacts.isEmpty()) {
                                    "No contacts with 10-digit phone numbers found on this device"
                                } else {
                                    "No contacts matching \"$searchQuery\""
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SplitMateTheme.TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredContacts, key = { it.cleanPhone }) { contact ->
                            val isChecked = selectedPhones.contains(contact.cleanPhone)
                            val toggleSelection = {
                                selectedPhones = if (multiSelect) {
                                    if (isChecked) selectedPhones - contact.cleanPhone else selectedPhones + contact.cleanPhone
                                } else {
                                    setOf(contact.cleanPhone)
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { toggleSelection() },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isChecked) {
                                    SplitMateTheme.SageSurface
                                } else {
                                    SplitMateTheme.SurfaceWhite
                                },
                                border = BorderStroke(
                                    width = if (isChecked) 1.5.dp else 1.dp,
                                    color = if (isChecked) BuckwheatOlivePrimary else SplitMateTheme.BorderLight
                                )
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
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isChecked) BuckwheatSageContainer else SplitMateTheme.SurfaceMuted
                                                )
                                                .border(1.dp, SplitMateTheme.BorderLight, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = extractInitialsFromNameOrSeed(contact.name),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = if (isChecked) BuckwheatOlivePrimary else SplitMateTheme.PrimaryDark
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = contact.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = SplitMateTheme.PrimaryDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = contact.formattedPhone,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = SplitMateTheme.TextSecondary
                                            )
                                        }
                                    }
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { toggleSelection() },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = BuckwheatOlivePrimary,
                                            checkmarkColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val selectedCount = selectedPhones.size
                Button(
                    onClick = {
                        val selectedList = contacts.filter { selectedPhones.contains(it.cleanPhone) }
                        if (selectedList.isNotEmpty()) {
                            onConfirmSelection(selectedList)
                        }
                        onDismiss()
                    },
                    enabled = selectedCount > 0,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SplitMateTheme.PrimaryDark,
                        contentColor = SplitMateTheme.ScreenBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = if (multiSelect) {
                            "Add Selected ($selectedCount)"
                        } else {
                            "Link Selected Contact"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = SplitMateTheme.ScreenBg
                    )
                }
            }
        }
    }
}

@Composable
fun SplitMateMaterial3ExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SplitMateExpressiveTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}

@Composable
fun SplitMateExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SplitMateDarkColorScheme
        else -> SplitMateLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SplitMateTypography,
        content = content
    )
}

val SplitMateBrandFontFamily: FontFamily = FigtreeFontFamily
val SplitMateDisplayFontFamily: FontFamily = FigtreeFontFamily

// ==============================================================================
// INDIAN RAILWAY (IRCTC) PNR & TRAVEL BOARDING PASS ENGINE
// ==============================================================================
data class ParsedTravelTicket(
    val pnr: String = "",
    val trainOrFlightNo: String = "",
    val trainOrCarrierName: String = "",
    val fromStation: String = "",
    val toStation: String = "",
    val departureDate: String = "",
    val departureTime: String = "",
    val coachAndSeats: String = "",
    val fareRupees: String = "",
    val cleanTitle: String = ""
) {
    val hasTicketMetadata: Boolean
        get() = pnr.isNotBlank() || trainOrFlightNo.isNotBlank() || coachAndSeats.isNotBlank() || (fromStation.isNotBlank() && toStation.isNotBlank())

    val route: String
        get() = if (fromStation.isNotBlank() || toStation.isNotBlank()) "${fromStation}→${toStation}" else ""

    val departureInfo: String
        get() = listOf(departureDate, departureTime).filter { it.isNotBlank() }.joinToString(" ")
}

private val OfflineIndianTrainCatalog = mapOf(
    "16592" to ("Hampi Express" to ("SBC" to "HPT")),
    "16591" to ("Hampi Express" to ("HPT" to "SBC")),
    "12628" to ("Karnataka Express" to ("NDLS" to "SBC")),
    "12627" to ("Karnataka Express" to ("SBC" to "NDLS")),
    "12952" to ("Mumbai Rajdhani" to ("NDLS" to "MMCT")),
    "12951" to ("New Delhi Rajdhani" to ("MMCT" to "NDLS")),
    "22436" to ("Vande Bharat Express" to ("NDLS" to "BSB")),
    "12002" to ("Bhopal Shatabdi" to ("NDLS" to "RKMP")),
    "12138" to ("Punjab Mail" to ("FZR" to "CSMT")),
    "16345" to ("Netravati Express" to ("LTT" to "TVC")),
    "11013" to ("Coimbatore Express" to ("LTT" to "CBE")),
    "12051" to ("Jan Shatabdi Exp" to ("CSMT" to "MAO")),
    "20111" to ("Konkan Kanya Exp" to ("CSMT" to "MAO"))
)

private val OfflineStationNames = mapOf(
    "SBC" to "KSR Bengaluru",
    "HPT" to "Hosapete (Hampi)",
    "NDLS" to "New Delhi",
    "MMCT" to "Mumbai Central",
    "CSMT" to "Mumbai CSMT",
    "LTT" to "Lokmanya Tilak",
    "MAO" to "Madgaon (Goa)",
    "PUNE" to "Pune Jn",
    "HYB" to "Hyderabad",
    "MAS" to "MGR Chennai",
    "HWH" to "Howrah Jn",
    "BSB" to "Varanasi Jn",
    "JP" to "Jaipur Jn",
    "ADI" to "Ahmedabad Jn",
    "GOI" to "Goa Airport",
    "DEL" to "Delhi Airport",
    "BOM" to "Mumbai Airport",
    "BLR" to "Bengaluru Airport"
)

fun resolveStationDisplayName(code: String): String {
    val clean = code.trim().uppercase()
    val fullName = OfflineStationNames[clean]
    return if (fullName != null) "$clean ($fullName)" else clean
}

fun parseIrctcOrTravelTicketText(rawText: String): ParsedTravelTicket {
    val text = rawText.trim()
    if (text.isEmpty()) return ParsedTravelTicket()

    val pnr10 = Regex("""\b(?:PNR[:\s-]*)?(\d{3}[-\s]?\d{7}|\d{10})\b""", RegexOption.IGNORE_CASE)
        .find(text)?.groupValues?.getOrNull(1)?.replace(Regex("[^0-9]"), "")
    val pnr6 = Regex("""\bPNR[:\s-]*([A-Z0-9]{6})\b""", RegexOption.IGNORE_CASE)
        .find(text)?.groupValues?.getOrNull(1)?.uppercase()
    val pnr = pnr10 ?: pnr6 ?: ""

    val train5 = Regex("""\b(?:TRAIN|TRN|Train No\.?)[:\s-]*(\d{5})\b""", RegexOption.IGNORE_CASE)
        .find(text)?.groupValues?.getOrNull(1)
        ?: Regex("""\b(\d{5})\b""").findAll(text).map { it.value }.firstOrNull { it != pnr.take(5) }
    val flightNo = Regex("""\b(6E|AI|UK|QP|SG)[-\s]?(\d{3,4})\b""", RegexOption.IGNORE_CASE)
        .find(text)?.value?.uppercase()
    val trainOrFlight = train5 ?: flightNo ?: ""

    val catalogMatch = OfflineIndianTrainCatalog[trainOrFlight]
    val trainName = catalogMatch?.first ?: if (flightNo != null) "Flight $flightNo" else ""

    val routeMatch = Regex("""\b([A-Z]{3,5})\s*(?:-|to|->|→)\s*([A-Z]{3,5})\b""").find(text)
    val fromCode = routeMatch?.groupValues?.getOrNull(1) ?: catalogMatch?.second?.first ?: ""
    val toCode = routeMatch?.groupValues?.getOrNull(2) ?: catalogMatch?.second?.second ?: ""

    val dateMatch = Regex("""\b(?:DOJ|Dt|Date)?[:\s-]*(\d{2}[-/]\d{2}[-/]\d{2,4})\b""", RegexOption.IGNORE_CASE)
        .find(text)?.groupValues?.getOrNull(1) ?: ""
    val timeMatch = Regex("""\b(?:Dep|Time|At)?[:\s-]*(\d{2}:\d{2})\b""", RegexOption.IGNORE_CASE)
        .find(text)?.groupValues?.getOrNull(1) ?: ""

    val coachMatches = Regex("""\b([A-Z]{1,2}\d{1,2}|SL|1A|2A|3A|CC)[-\s/](\d{1,3})(?:\s*(LB|MB|UB|SL|SU|WS|MS|AS))?\b""", RegexOption.IGNORE_CASE)
        .findAll(text)
        .map { it.value.trim().uppercase() }
        .toList()
    val coachStr = coachMatches.joinToString(", ")

    val fareMatch = Regex("""(?:Fare|Rs\.?|INR|₹)[:\s]*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
        .find(text)?.groupValues?.getOrNull(1)?.replace(",", "") ?: ""

    return ParsedTravelTicket(
        pnr = pnr,
        trainOrFlightNo = trainOrFlight,
        trainOrCarrierName = trainName,
        fromStation = fromCode,
        toStation = toCode,
        departureDate = dateMatch,
        departureTime = timeMatch,
        coachAndSeats = coachStr,
        fareRupees = fareMatch
    )
}

fun enrichTicketWithOfflineCatalog(ticket: ParsedTravelTicket): ParsedTravelTicket {
    val cleanTrain = ticket.trainOrFlightNo.trim()
    val match = OfflineIndianTrainCatalog[cleanTrain] ?: return ticket
    return ticket.copy(
        trainOrCarrierName = ticket.trainOrCarrierName.ifBlank { match.first },
        fromStation = ticket.fromStation.ifBlank { match.second.first },
        toStation = ticket.toStation.ifBlank { match.second.second }
    )
}

fun formatTravelExpenseTitle(baseCategory: String, ticket: ParsedTravelTicket): String {
    val enriched = enrichTicketWithOfflineCatalog(ticket)
    if (!enriched.hasTicketMetadata) return baseCategory.ifBlank { "Travel & Ticket" }
    val parts = mutableListOf<String>()
    val labelPrefix = when {
        enriched.trainOrCarrierName.isNotBlank() && enriched.trainOrFlightNo.isNotBlank() ->
            "🚆 ${enriched.trainOrFlightNo} ${enriched.trainOrCarrierName}"
        enriched.trainOrFlightNo.isNotBlank() -> "🚆 Train/Flight ${enriched.trainOrFlightNo}"
        else -> "🚆 ${baseCategory.ifBlank { "Travel Ticket" }}"
    }
    parts.add(labelPrefix)
    if (enriched.pnr.isNotBlank()) parts.add("PNR: ${enriched.pnr}")
    if (enriched.fromStation.isNotBlank() && enriched.toStation.isNotBlank()) {
        parts.add("${enriched.fromStation.uppercase()}→${enriched.toStation.uppercase()}")
    }
    if (enriched.departureTime.isNotBlank() || enriched.departureDate.isNotBlank()) {
        val dt = listOf(enriched.departureDate, enriched.departureTime).filter { it.isNotBlank() }.joinToString(" ")
        parts.add("Dep: $dt")
    }
    if (enriched.coachAndSeats.isNotBlank()) {
        parts.add("Seats: ${enriched.coachAndSeats}")
    }
    return parts.joinToString(" | ")
}

fun extractTravelTicketFromTitle(title: String): ParsedTravelTicket? {
    if (!title.contains("PNR:", ignoreCase = true) &&
        !title.contains("Seats:", ignoreCase = true) &&
        !title.contains("🚆") &&
        !title.contains("→")
    ) {
        return null
    }
    val segments = title.split("|").map { it.trim() }
    var pnr = ""
    var trainNo = ""
    var trainName = ""
    var fromSt = ""
    var toSt = ""
    var dep = ""
    var seats = ""

    segments.forEachIndexed { idx, seg ->
        when {
            seg.startsWith("PNR:", ignoreCase = true) -> pnr = seg.substringAfter(":").trim()
            seg.startsWith("Dep:", ignoreCase = true) -> dep = seg.substringAfter(":").trim()
            seg.startsWith("Seats:", ignoreCase = true) || seg.startsWith("Coach", ignoreCase = true) ->
                seats = seg.substringAfter(":").trim()
            seg.contains("→") -> {
                fromSt = seg.substringBefore("→").trim()
                toSt = seg.substringAfter("→").trim()
            }
            idx == 0 -> {
                val cleanFirst = seg.replace("🚆", "").replace("✈️", "").trim()
                val numMatch = Regex("""^(\d{5}|[A-Z0-9]{2}-\d{3,4})\s*(.*)$""").find(cleanFirst)
                if (numMatch != null) {
                    trainNo = numMatch.groupValues[1]
                    trainName = numMatch.groupValues[2]
                } else {
                    trainName = cleanFirst
                }
            }
        }
    }
    val parsed = enrichTicketWithOfflineCatalog(
        ParsedTravelTicket(
            pnr = pnr,
            trainOrFlightNo = trainNo,
            trainOrCarrierName = trainName,
            fromStation = fromSt,
            toStation = toSt,
            departureTime = dep,
            coachAndSeats = seats,
            cleanTitle = segments.firstOrNull()?.trim().orEmpty().ifBlank { "🚆 Train / Travel Ticket" }
        )
    )
    return if (parsed.hasTicketMetadata) parsed else null
}

@Composable
fun GroupBoardingPassCard(
    ticket: ParsedTravelTicket,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF6F9EE),
        border = BorderStroke(1.dp, Color(0xFFC5DCA0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.ConfirmationNumber,
                        contentDescription = null,
                        tint = BuckwheatOlivePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = buildString {
                            if (ticket.trainOrFlightNo.isNotBlank()) append("#${ticket.trainOrFlightNo} ")
                            append(ticket.trainOrCarrierName.ifBlank { "Group Travel Pass" })
                        },
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = BuckwheatOlivePrimary
                    )
                }

                if (ticket.pnr.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = BuckwheatOlivePrimary
                    ) {
                        Text(
                            text = "PNR: ${ticket.pnr}",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = Color.White,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (ticket.fromStation.isNotBlank() || ticket.toStation.isNotBlank() || ticket.departureTime.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (ticket.fromStation.isNotBlank() || ticket.toStation.isNotBlank()) {
                        Text(
                            text = "${resolveStationDisplayName(ticket.fromStation.ifBlank { "Origin" })}  →  ${resolveStationDisplayName(ticket.toStation.ifBlank { "Dest" })}",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BuckwheatCharcoal
                        )
                    }
                    if (ticket.departureTime.isNotBlank()) {
                        Text(
                            text = "Dep ${ticket.departureTime}",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = BuckwheatSecondaryText,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }
            }

            if (ticket.coachAndSeats.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFDCE9B9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Coach / Berth Allocation",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = BuckwheatSecondaryText
                        )
                        Text(
                            text = ticket.coachAndSeats,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = BuckwheatOlivePrimary,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }
            }

            if (ticket.pnr.length == 10) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        onClick = {
                            runCatching {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("IRCTC PNR", ticket.pnr))
                                val uri = Uri.parse("https://www.confirmtkt.com/pnr-status/${ticket.pnr}")
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        },
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFDCE9B9)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Train,
                                contentDescription = null,
                                tint = BuckwheatOlivePrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Check Live PNR & Coach Status ↗",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = BuckwheatOlivePrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

