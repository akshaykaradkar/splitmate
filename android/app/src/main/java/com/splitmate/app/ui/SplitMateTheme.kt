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
import kotlinx.coroutines.launch
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

    // Warm Espresso Night Theme (#181512 Leather-Journal Dark Palette)
    val GM3DarkBackground = Color(0xFF181512)
    val GM3DarkCardSurface = Color(0xFF24201C)
    val GM3DarkKeypadSurface = Color(0xFF2E2823)
    val GM3DarkPrimaryText = Color(0xFFFAF6F0)
    val GM3DarkSubtitleText = Color(0xFFB5ACA2)
    val GM3DarkBorder = Color(0xFF38312B)

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
    primary = Color(0xFFD7E8B6),
    onPrimary = Color(0xFF181512),
    primaryContainer = Color(0xFF233216),
    onPrimaryContainer = Color(0xFFD7E8B6),
    secondary = Color(0xFFFEB49C),
    onSecondary = Color(0xFF3A2019),
    secondaryContainer = Color(0xFF3A2019),
    onSecondaryContainer = Color(0xFFFECDD3),
    tertiary = Color(0xFFBDC5FF),
    onTertiary = Color(0xFF1E2678),
    tertiaryContainer = Color(0xFF2A263D),
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

// Bundled TrueType Figtree fonts placed FIRST so they load synchronously on Frame 0 with zero Roboto fallback
val FigtreeFontFamily = FontFamily(
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_regular, weight = FontWeight.Light),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_regular, weight = FontWeight.Normal),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_medium, weight = FontWeight.Medium),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_semibold, weight = FontWeight.SemiBold),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_bold, weight = FontWeight.Bold),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_extrabold, weight = FontWeight.ExtraBold),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_extrabold, weight = FontWeight.Black),
    Font(googleFont = figtreeGoogleFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = figtreeGoogleFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = figtreeGoogleFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = figtreeGoogleFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = figtreeGoogleFont, fontProvider = fontProvider, weight = FontWeight.ExtraBold)
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
            "${ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER} = 1",
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                if (contactsByPhone.size >= 500) break
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
// EXPENSE CATEGORY ICON & COLOR RESOLVER (Tab 1 Recent Activity, Group View & Tab 4 Audit)
// ==============================================================================
fun resolveExpenseCategoryIcon(title: String): ImageVector {
    val lower = title.lowercase()
    return when {
        lower.contains("train") || lower.contains("pnr") || lower.contains("irctc") ||
            lower.contains("express") || lower.contains("rajdhani") || lower.contains("shatabdi") ||
            lower.contains("vande") || lower.contains("rail") || lower.contains("berth") -> Icons.Rounded.Train
        lower.contains("flight") || lower.contains("indigo") || lower.contains("air ") ||
            lower.contains("airport") || lower.contains("vistara") || lower.contains("boarding") -> Icons.Rounded.FlightTakeoff
        lower.contains("cab") || lower.contains("auto") || lower.contains("uber") ||
            lower.contains("ola") || lower.contains("rapido") || lower.contains("taxi") ||
            lower.contains("fuel") || lower.contains("petrol") || lower.contains("toll") -> Icons.Rounded.LocalTaxi
        lower.contains("hotel") || lower.contains("stay") || lower.contains("resort") ||
            lower.contains("villa") || lower.contains("airbnb") || lower.contains("hostel") ||
            lower.contains("room") -> Icons.Rounded.Hotel
        lower.contains("grocer") || lower.contains("mart") || lower.contains("blinkit") ||
            lower.contains("zepto") || lower.contains("instamart") || lower.contains("supermarket") -> Icons.Rounded.ShoppingCart
        lower.contains("drink") || lower.contains("outing") || lower.contains("bar") ||
            lower.contains("pub") || lower.contains("club") || lower.contains("party") ||
            lower.contains("beer") -> Icons.Rounded.LocalBar
        lower.contains("coffee") || lower.contains("cafe") || lower.contains("tea") ||
            lower.contains("chai") || lower.contains("bakery") || lower.contains("snack") ||
            lower.contains("breakfast") -> Icons.Rounded.LocalCafe
        lower.contains("movie") || lower.contains("cinema") || lower.contains("concert") ||
            lower.contains("show") || lower.contains("event") || lower.contains("museum") -> Icons.Rounded.ConfirmationNumber
        lower.contains("shop") || lower.contains("gift") || lower.contains("clothes") ||
            lower.contains("souvenir") -> Icons.Rounded.ShoppingBag
        lower.contains("dinner") || lower.contains("food") || lower.contains("lunch") ||
            lower.contains("restaurant") || lower.contains("pizza") || lower.contains("biryani") ||
            lower.contains("zomato") || lower.contains("swiggy") || lower.contains("meal") -> Icons.Rounded.Restaurant
        else -> Icons.Rounded.ReceiptLong
    }
}

fun resolveExpenseCategoryBadgeColors(title: String, isDark: Boolean): Pair<Color, Color> {
    val lower = title.lowercase()
    return when {
        lower.contains("train") || lower.contains("pnr") || lower.contains("irctc") || lower.contains("rail") ->
            if (isDark) Color(0xFF283A18) to Color(0xFFD7E8B6) else Color(0xFFDCE9B9) to Color(0xFF365314)
        lower.contains("flight") || lower.contains("air") || lower.contains("hotel") || lower.contains("stay") ->
            if (isDark) Color(0xFF222A4A) to Color(0xFFC7D2FE) else Color(0xFFE0E7FF) to Color(0xFF3730A3)
        lower.contains("cab") || lower.contains("auto") || lower.contains("uber") || lower.contains("taxi") || lower.contains("fuel") ->
            if (isDark) Color(0xFF3D2E14) to Color(0xFFFDE68A) else Color(0xFFFEF3C7) to Color(0xFF92400E)
        lower.contains("grocer") || lower.contains("mart") || lower.contains("shop") ->
            if (isDark) Color(0xFF1F3833) to Color(0xFFA7F3D0) else Color(0xFFD1FAE5) to Color(0xFF065F46)
        lower.contains("drink") || lower.contains("outing") || lower.contains("bar") || lower.contains("party") ->
            if (isDark) Color(0xFF3B1D2E) to Color(0xFFFBCFE8) else Color(0xFFFCE7F3) to Color(0xFF9D174D)
        else ->
            if (isDark) Color(0xFF3D231B) to Color(0xFFFED8C8) else Color(0xFFFCE3D7) to Color(0xFF7C2D12)
    }
}

// ==============================================================================
// CRISP HARDWARE VIBRATOR & TACTILE KEYPAD HAPTIC ENGINE
// ==============================================================================
fun performCrispTactileHaptic(
    context: Context,
    view: android.view.View? = null,
    heavy: Boolean = false
) {
    val prefs = context.getSharedPreferences("splitmate_prefs", Context.MODE_PRIVATE)
    if (!prefs.getBoolean("pref_haptics", true)) return

    runCatching {
        view?.performHapticFeedback(
            if (heavy) android.view.HapticFeedbackConstants.LONG_PRESS
            else android.view.HapticFeedbackConstants.KEYBOARD_TAP,
            android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    runCatching {
        val vibrator: android.os.Vibrator? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val durationMs = if (heavy) 28L else 16L
                val amplitude = if (heavy) 245 else 195
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (heavy) 28L else 16L)
            }
        }
    }
}

// ==============================================================================
// INDIAN RAILWAY (IRCTC) PNR, CNF/WL/RAC & LIVE TRAIN RUNNING STATUS ENGINE
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
    val bookingStatus: String = "CNF", // CNF, WL, RAC
    val chartStatus: String = "Chart Prepared",
    val liveTrainRadar: String = "",
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

data class LivePnrPassenger(
    val passengerNumber: String,
    val initialStatus: String,
    val currentStatus: String,
    val statusLabel: String
)

data class LivePnrStatusSnapshot(
    val pnr: String,
    val trainNo: String,
    val trainName: String,
    val fromStation: String,
    val toStation: String,
    val departureTime: String,
    val travelClass: String = "",
    val totalFareRupees: Int = 0,
    val passengerCount: Int = 1,
    val bookingStatusBadge: String, // "CNF", "WL", "RAC", or "MANUAL"
    val chartPrepared: Boolean,
    val passengerStatuses: List<String>,
    val structuredPassengers: List<LivePnrPassenger> = emptyList(),
    val fromStationName: String = "",
    val toStationName: String = "",
    val arrivalTime: String = "",
    val durationText: String = "",
    val quotaText: String = "GN",
    val coachPositionHint: String,
    val liveTrainLocationRadar: String,
    val confirmationProbability: String,
    val sourceLabel: String,
    val isLiveVerified: Boolean = true,
    val isManualEntry: Boolean = false
) {
    /**
     * Official Indian Railways / IRCTC Tariff Classification:
     * Non-AC classes ("SL", "2S", "II", "GN", "UR") are charged ₹10+18% GST (₹11.80 UPI) / ₹15+18% GST (₹17.70 Card).
     * AC classes ("1A", "2A", "3A", "3E", "CC", "EC", "EA", "FC") are charged ₹20+18% GST (₹23.60 UPI) / ₹30+18% GST (₹35.40 Card).
     */
    val isAcClass: Boolean
        get() = travelClass.trim().uppercase() !in setOf("SL", "2S", "II", "GN", "UR")

    val effectivePassengerCount: Int
        get() = passengerCount.coerceAtLeast(passengerStatuses.size.coerceAtLeast(1))

    val baseFarePaise: Long
        get() = totalFareRupees.toLong() * 100L

    // IRCTC Convenience Fee per PNR (inclusive of 18% GST)
    val irctcConvenienceFeeUpiPaise: Long
        get() = if (totalFareRupees > 0) (if (isAcClass) 2360L else 1180L) else 0L

    val irctcConvenienceFeeCardPaise: Long
        get() = if (totalFareRupees > 0) (if (isAcClass) 3540L else 1770L) else 0L

    // IRCTC Optional Travel Insurance Premium: ₹0.45 (45 paise) per passenger (inclusive of 18% GST)
    val travelInsurancePaise: Long
        get() = if (totalFareRupees > 0) effectivePassengerCount * 45L else 0L

    // Dynamic All-Inclusive IRCTC Totals in Paise
    val allInclusiveUpiPaise: Long
        get() = baseFarePaise + irctcConvenienceFeeUpiPaise + travelInsurancePaise

    val allInclusiveCardPaise: Long
        get() = baseFarePaise + irctcConvenienceFeeCardPaise + travelInsurancePaise

    fun computeCustomTotalPaise(paymentMode: String = "UPI", includeInsurance: Boolean = true): Long {
        if (totalFareRupees <= 0) return 0L
        val convFee = when (paymentMode.uppercase()) {
            "UPI" -> irctcConvenienceFeeUpiPaise
            "CARD" -> irctcConvenienceFeeCardPaise
            else -> 0L
        }
        val insFee = if (includeInsurance) travelInsurancePaise else 0L
        return baseFarePaise + convFee + insFee
    }

    fun formatPaiseAsDecimalRupees(paise: Long): String {
        val whole = paise / 100L
        val rem = kotlin.math.abs(paise % 100L)
        return if (rem == 0L) "$whole" else String.format(java.util.Locale.US, "%d.%02d", whole, rem)
    }
}

private val OfflineStationNames = mapOf(
    "BDTS" to "Mumbai Bandra Terminus",
    "CDG" to "Chandigarh",
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
    "RKMP" to "Rani Kamalapati",
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

fun loadPersistedPnrSnapshot(context: Context, pnr: String): LivePnrStatusSnapshot? =
    com.splitmate.app.data.PnrNetworkRepository.loadPersistedPnrSnapshot(context, pnr)

fun shouldSkipAutoPnrNetworkPoll(
    context: Context,
    pnr: String,
    ticket: ParsedTravelTicket
): Boolean = com.splitmate.app.data.PnrNetworkRepository.shouldSkipAutoPnrNetworkPoll(context, pnr, ticket)

fun recordPnrSyncTimestamp(context: Context, pnr: String) =
    com.splitmate.app.data.PnrNetworkRepository.recordPnrSyncTimestamp(context, pnr)

suspend fun fetchLivePnrAndTrainStatus(
    pnr: String,
    fallbackTicket: ParsedTravelTicket = ParsedTravelTicket(),
    forceManualRefresh: Boolean = false,
    context: Context? = null
): LivePnrStatusSnapshot = com.splitmate.app.data.PnrNetworkRepository.fetchLivePnrStatus(
    pnr = pnr,
    fallbackTicket = fallbackTicket,
    forceManualRefresh = forceManualRefresh,
    context = context
)

fun formatTravelExpenseTitle(baseCategory: String, ticket: ParsedTravelTicket): String {
    if (!ticket.hasTicketMetadata) return baseCategory.ifBlank { "Travel & Ticket" }
    val parts = mutableListOf<String>()
    val labelPrefix = when {
        ticket.trainOrCarrierName.isNotBlank() && ticket.trainOrFlightNo.isNotBlank() ->
            "Train ${ticket.trainOrFlightNo} ${ticket.trainOrCarrierName}"
        ticket.trainOrFlightNo.isNotBlank() -> "Train/Flight ${ticket.trainOrFlightNo}"
        ticket.trainOrCarrierName.isNotBlank() -> ticket.trainOrCarrierName
        else -> baseCategory.ifBlank { "Train / Travel Ticket" }
    }
    parts.add(labelPrefix)
    if (ticket.pnr.isNotBlank()) parts.add("PNR: ${ticket.pnr}")
    if (ticket.bookingStatus.isNotBlank()) parts.add("Status: ${ticket.bookingStatus}")
    if (ticket.fromStation.isNotBlank() && ticket.toStation.isNotBlank()) {
        parts.add("${ticket.fromStation.uppercase()}->${ticket.toStation.uppercase()}")
    }
    if (ticket.departureTime.isNotBlank() || ticket.departureDate.isNotBlank()) {
        val dt = listOf(ticket.departureDate, ticket.departureTime).filter { it.isNotBlank() }.joinToString(" ")
        parts.add("Dep: $dt")
    }
    if (ticket.coachAndSeats.isNotBlank()) {
        parts.add("Seats: ${ticket.coachAndSeats}")
    }
    return parts.joinToString(" | ")
}

fun extractTravelTicketFromTitle(title: String): ParsedTravelTicket? {
    if (!title.contains("PNR:", ignoreCase = true) &&
        !title.contains("Seats:", ignoreCase = true) &&
        !title.contains("Train", ignoreCase = true) &&
        !title.contains("->") &&
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
    var status = "CNF"

    segments.forEachIndexed { idx, seg ->
        when {
            seg.startsWith("PNR:", ignoreCase = true) -> pnr = seg.substringAfter(":").trim()
            seg.startsWith("Status:", ignoreCase = true) -> status = seg.substringAfter(":").trim()
            seg.startsWith("Dep:", ignoreCase = true) -> dep = seg.substringAfter(":").trim()
            seg.startsWith("Seats:", ignoreCase = true) || seg.startsWith("Coach", ignoreCase = true) ->
                seats = seg.substringAfter(":").trim()
            seg.contains("->") -> {
                fromSt = seg.substringBefore("->").trim()
                toSt = seg.substringAfter("->").trim()
            }
            seg.contains("→") -> {
                fromSt = seg.substringBefore("→").trim()
                toSt = seg.substringAfter("→").trim()
            }
            idx == 0 -> {
                val cleanFirst = seg.replace("Train/Flight", "").replace("Train", "").trim()
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
    if (seats.contains("WL", ignoreCase = true) && status == "CNF") status = "WL"
    if (seats.contains("RAC", ignoreCase = true) && status == "CNF") status = "RAC"

    val parsed = ParsedTravelTicket(
        pnr = pnr,
        trainOrFlightNo = trainNo,
        trainOrCarrierName = trainName,
        fromStation = fromSt,
        toStation = toSt,
        departureTime = dep,
        coachAndSeats = seats,
        bookingStatus = status,
        chartStatus = if (status.contains("WL", ignoreCase = true)) "Chart Not Prepared" else "Chart Prepared",
        cleanTitle = segments.firstOrNull()?.trim().orEmpty().ifBlank { "Train / PNR Ticket" }
    )
    return if (parsed.hasTicketMetadata) parsed else null
}

@Composable
fun SplitMateCircularLogoBadge(
    sizeDp: Int = 44,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(SplitMateTheme.SurfaceWhite, CircleShape)
            .border(2.dp, SplitMateTheme.PrimaryDark.copy(alpha = 0.28f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(CircleShape)
        ) {
            drawArc(
                color = Color(0xFFD7E8B6),
                startAngle = 135f,
                sweepAngle = 180f,
                useCenter = true
            )
            drawArc(
                color = Color(0xFFE06B52),
                startAngle = 315f,
                sweepAngle = 180f,
                useCenter = true
            )
            drawCircle(
                color = Color(0xFF365314),
                radius = size.minDimension * 0.48f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF365314),
                radius = size.minDimension * 0.10f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.36f, size.height * 0.36f)
            )
            drawCircle(
                color = Color(0xFFFAF6F0),
                radius = size.minDimension * 0.10f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.64f, size.height * 0.64f)
            )
        }
    }
}

@Composable
fun GroupBoardingPassCard(
    ticket: ParsedTravelTicket,
    modifier: Modifier = Modifier,
    onPersistUpdatedTitle: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val localView = androidx.compose.ui.platform.LocalView.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val cleanCardPnr = ticket.pnr.replace(Regex("[^0-9]"), "").take(10)
    var liveSnapshot by androidx.compose.runtime.remember(ticket.pnr, ticket.coachAndSeats) {
        androidx.compose.runtime.mutableStateOf<LivePnrStatusSnapshot?>(loadPersistedPnrSnapshot(context, cleanCardPnr))
    }
    var isRefreshingLive by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    fun syncAndPersist(snapshot: LivePnrStatusSnapshot) {
        liveSnapshot = snapshot
        if (snapshot.sourceLabel.contains("Live", ignoreCase = true) && snapshot.passengerStatuses.isNotEmpty()) {
            recordPnrSyncTimestamp(context, snapshot.pnr)
            val shortStatus = when {
                snapshot.bookingStatusBadge.startsWith("CNF") -> "CNF"
                snapshot.bookingStatusBadge.startsWith("RAC") -> "RAC"
                else -> "WL"
            }
            val updatedTicket = ticket.copy(
                pnr = snapshot.pnr,
                trainOrFlightNo = snapshot.trainNo.ifBlank { ticket.trainOrFlightNo },
                trainOrCarrierName = snapshot.trainName.ifBlank { ticket.trainOrCarrierName },
                fromStation = snapshot.fromStation.ifBlank { ticket.fromStation },
                toStation = snapshot.toStation.ifBlank { ticket.toStation },
                departureTime = snapshot.departureTime.ifBlank { ticket.departureTime },
                coachAndSeats = snapshot.passengerStatuses.joinToString(", "),
                bookingStatus = shortStatus,
                chartStatus = if (snapshot.chartPrepared) "Chart Prepared" else "Chart Not Prepared",
                liveTrainRadar = snapshot.liveTrainLocationRadar
            )
            onPersistUpdatedTitle?.invoke(formatTravelExpenseTitle("Train / PNR", updatedTicket))
        }
    }

    // Smart Rate-Limit-Guarded Auto-Sync:
    // - 0 network calls if already CNF + Chart Prepared
    // - 0 network calls during CRIS nightly downtime (23:30-00:30 IST)
    // - 0 network calls if already synced within the last 6 hours (or 30m on Travel Day)
    androidx.compose.runtime.LaunchedEffect(ticket.pnr) {
        if (cleanCardPnr.length == 10 && !shouldSkipAutoPnrNetworkPoll(context, cleanCardPnr, ticket)) {
            isRefreshingLive = true
            val fetched = fetchLivePnrAndTrainStatus(cleanCardPnr, ticket, forceManualRefresh = false, context = context)
            syncAndPersist(fetched)
            isRefreshingLive = false
        } else if (liveSnapshot == null && cleanCardPnr.length == 10) {
            liveSnapshot = loadPersistedPnrSnapshot(context, cleanCardPnr)
        }
    }

    val effectiveStatus = liveSnapshot?.bookingStatusBadge ?: ticket.bookingStatus.ifBlank { "CNF" }
    val isWaitlistedOrRac = effectiveStatus.contains("WL", ignoreCase = true) || effectiveStatus.contains("RAC", ignoreCase = true)

    val passBg = if (SplitMateTheme.isDark) Color(0xFF233216) else Color(0xFFF6F9EE)
    val passBorder = if (SplitMateTheme.isDark) Color(0xFF3E5626) else Color(0xFFC5DCA0)
    val passAccent = if (SplitMateTheme.isDark) Color(0xFFD7E8B6) else BuckwheatOlivePrimary
    val statusBadgeBg = if (isWaitlistedOrRac) Color(0xFFE06B52) else BuckwheatOlivePrimary

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = passBg,
        border = BorderStroke(1.dp, passBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Rounded.Train,
                        contentDescription = null,
                        tint = passAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = buildString {
                            val trNo = liveSnapshot?.trainNo ?: ticket.trainOrFlightNo
                            val trName = liveSnapshot?.trainName ?: ticket.trainOrCarrierName
                            if (trNo.isNotBlank()) append("#$trNo ")
                            append(trName.ifBlank { "Group Travel Pass" })
                        },
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = passAccent
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusBadgeBg
                    ) {
                        Text(
                            text = effectiveStatus,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (ticket.pnr.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SplitMateTheme.PrimaryDark
                        ) {
                            Text(
                                text = "PNR: ${ticket.pnr}",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                color = SplitMateTheme.ScreenBg,
                                style = TextStyle(fontFeatureSettings = "tnum"),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            val fromSt = liveSnapshot?.fromStation ?: ticket.fromStation
            val toSt = liveSnapshot?.toStation ?: ticket.toStation
            val depTm = liveSnapshot?.departureTime ?: ticket.departureTime

            if (fromSt.isNotBlank() || toSt.isNotBlank() || depTm.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (fromSt.isNotBlank() || toSt.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = resolveStationDisplayName(fromSt.ifBlank { "Origin" }),
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SplitMateTheme.PrimaryDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = passAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = resolveStationDisplayName(toSt.ifBlank { "Dest" }),
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SplitMateTheme.PrimaryDark
                            )
                        }
                    }
                    if (depTm.isNotBlank()) {
                        Text(
                            text = "Dep $depTm",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SplitMateTheme.TextSecondary,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }
            }

            val seatsText = liveSnapshot?.passengerStatuses?.joinToString(" · ") ?: ticket.coachAndSeats
            if (seatsText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SplitMateTheme.SurfaceWhite,
                    border = BorderStroke(1.dp, passBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (liveSnapshot?.sourceLabel?.contains("Live") == true) {
                                    "🟢 ${liveSnapshot!!.sourceLabel}"
                                } else {
                                    "Passenger Status (CNF / WL / RAC · 6h Smart Cache)"
                                },
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = SplitMateTheme.TextSecondary
                            )
                            Text(
                                text = if (liveSnapshot != null) {
                                    if (liveSnapshot!!.chartPrepared) "✓ Chart Prepared" else "⏳ Chart Not Prepared"
                                } else {
                                    ticket.chartStatus
                                },
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                color = if (isWaitlistedOrRac) Color(0xFFE06B52) else passAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = seatsText,
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = if (isWaitlistedOrRac) Color(0xFFE06B52) else passAccent,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }
            }

            // Live Radar / WhereIsMyTrain & ConfirmTkt Status Box (shown when user taps Refresh or when radar info exists)
            val liveRadar = liveSnapshot?.liveTrainLocationRadar ?: ticket.liveTrainRadar
            if (liveSnapshot != null || liveRadar.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (SplitMateTheme.isDark) Color(0xFF1A2510) else Color(0xFFECF4DC),
                    border = BorderStroke(1.dp, passBorder)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = "🛰️ Live Train Radar: ${liveRadar.ifBlank { "Running On Time · NTES Tracked" }}",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SplitMateTheme.PrimaryDark
                        )
                        if (liveSnapshot != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "🚃 ${liveSnapshot!!.coachPositionHint} · ${liveSnapshot!!.confirmationProbability}",
                                fontFamily = FigtreeFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = SplitMateTheme.TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        performCrispTactileHaptic(context, localView, heavy = false)
                        isRefreshingLive = true
                        coroutineScope.launch {
                            val fetched = fetchLivePnrAndTrainStatus(
                                ticket.pnr,
                                ticket,
                                forceManualRefresh = true,
                                context = context
                            )
                            syncAndPersist(fetched)
                            isRefreshingLive = false
                        }
                    },
                    shape = RoundedCornerShape(50),
                    color = SplitMateTheme.PrimaryDark
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = null,
                            tint = SplitMateTheme.ScreenBg,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRefreshingLive) "Syncing Live CRIS..." else "Refresh Live CNF/WL & Save",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = SplitMateTheme.ScreenBg
                        )
                    }
                }

                if (ticket.pnr.isNotBlank()) {
                    Surface(
                        onClick = {
                            performCrispTactileHaptic(context, localView, heavy = false)
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
                        color = if (SplitMateTheme.isDark) Color(0xFF3E5626) else Color(0xFFDCE9B9)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ConfirmTkt ↗",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = passAccent
                            )
                        }
                    }
                }
            }
        }
    }
}


