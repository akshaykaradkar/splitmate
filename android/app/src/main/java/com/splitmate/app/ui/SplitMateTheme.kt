package com.splitmate.app.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cabin
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.R

// ==============================================================================
// CANONICAL BINDINGS TO THE 4 GOOGLE DESIGN SYSTEMS (`design_systems/`)
// ==============================================================================
object DesignSystemBindings {
    // 1. Google Material 3 (`design_systems/google-material-3/DESIGN.md`)
    //    Dark Tonal Elevation Hierarchy (`ref-palette-neutral10` #1E1E1E, `ref-palette-neutral17` #2A2A2A)
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
    //    Container Color Morphing (400ms FastOutSlowInEasing) & Tactile Spring Physics
    fun <T> themeColorTween() = tween<T>(durationMillis = 400, easing = FastOutSlowInEasing)
    fun <T> tactileSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // 3. Elements GM3 (`design_systems/elements-3/DESIGN.md`)
    //    Semantic Financial State Tokens (Creditor Sage vs. Debtor Terracotta)
    val ElementsCreditorContainer = Color(0xFFD7E8B6)
    val ElementsCreditorOnContainer = Color(0xFF2D4810)
    val ElementsDebtorContainer = Color(0xFFFED8C8)
    val ElementsDebtorOnContainer = Color(0xFF7C2D12)
    val ElementsInfoPeriwinkle = Color(0xFFDCE3FD)

    val ElementsPositiveContainer = Color(0xFFD7E8B6)
    val ElementsPositiveText = Color(0xFF416913)
    val ElementsNegativeText = Color(0xFFE06B52)

    // 4. Android Pixel Design System (`design_systems/android-pixel-design-system/DESIGN.md`)
    //    High-density 8dp/4dp rhythm & compact fintech ergonomics
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

// Premium Fintech Google Fonts ("Plus Jakarta Sans" & "Outfit")
private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val plusJakartaSansFont = GoogleFont("Plus Jakarta Sans")
private val outfitFont = GoogleFont("Outfit")

val SplitMateDisplayFontFamily = FontFamily(
    Font(googleFont = outfitFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = outfitFont, fontProvider = googleFontProvider, weight = FontWeight.ExtraBold),
    Font(googleFont = outfitFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold)
)

val SplitMateBrandFontFamily = FontFamily(
    Font(googleFont = plusJakartaSansFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = plusJakartaSansFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = plusJakartaSansFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = plusJakartaSansFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = plusJakartaSansFont, fontProvider = googleFontProvider, weight = FontWeight.ExtraBold)
)

val SplitMateTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SplitMateDisplayFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 42.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1.0).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = SplitMateDisplayFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SplitMateDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = SplitMateBrandFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SplitMateBrandFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SplitMateBrandFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SplitMateBrandFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SplitMateBrandFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    )
)

/**
 * Extracts ONLY the first 1-2 uppercase letters of a user's ACTUAL NAME.
 * Never returns "Masculine", "Feminine", or "Neutral" even if the seed is "Akshay|Masculine".
 */
fun extractInitialsFromNameOrSeed(rawNameOrSeed: String): String {
    val withoutStyle = rawNameOrSeed
        .substringBefore('|')
        .substringBefore('_')
        .replace("(You)", "", ignoreCase = true)
        .replace("Masculine", "", ignoreCase = true)
        .replace("Feminine", "", ignoreCase = true)
        .replace("Neutral", "", ignoreCase = true)
        .trim()

    if (withoutStyle.isEmpty()) return "A"
    val parts = withoutStyle.split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        else -> parts[0].take(1).uppercase()
    }
}

/**
 * Builds a DiceBear Open-Peeps SVG URL with Presentation Style (`Masculine`, `Feminine`, `Neutral`).
 * Supports encoding the style in the seed as `"Name|Masculine"`, `"Name|Feminine"`, or `"Name|Neutral"`.
 */
fun buildDiceBearOpenPeepsUrl(rawSeed: String, styleOverride: String? = null): String {
    val parts = rawSeed.split("|")
    val seedBase = parts.firstOrNull()?.trim()?.ifEmpty { "Explorer" } ?: "Explorer"
    val style = (styleOverride ?: parts.getOrNull(1)?.trim() ?: "Neutral").lowercase()
    val headParam = when (style) {
        "masculine" -> "&head=flatTop,short1,short2,short3,short4"
        "feminine" -> "&head=long1,long2,long3,long4,buns"
        else -> ""
    }
    return "https://api.dicebear.com/9.x/open-peeps/svg?seed=${Uri.encode(seedBase)}&backgroundColor=d7e8b6,fed8c8,dce3fd$headParam"
}

/**
 * 8 Expressive Material 3 Category Icons for Group Creation & Group Cards (Point 3).
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

/**
 * Queries Android's ContentResolver for a picked Contact URI, retrieves their `DISPLAY_NAME`
 * and `NUMBER` from `ContactsContract.CommonDataKinds.Phone`, strips all non-digits (`+91`, spaces, hyphens),
 * and returns `(displayName, cleanTenDigits)`.
 */
fun extractPhoneAndNameFromContactUri(
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
