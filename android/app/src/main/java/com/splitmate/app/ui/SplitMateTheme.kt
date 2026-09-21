package com.splitmate.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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
    background = BuckwheatCanvas,
    onBackground = BuckwheatCharcoal,
    surface = BuckwheatSurface,
    onSurface = BuckwheatCharcoal,
    surfaceVariant = BuckwheatSunken,
    onSurfaceVariant = BuckwheatSecondaryText,
    outline = BuckwheatBorder
)

private val SplitMateDarkColorScheme = darkColorScheme(
    primary = BuckwheatSageContainer,
    onPrimary = BuckwheatOlivePrimary,
    primaryContainer = BuckwheatOlivePrimary,
    onPrimaryContainer = BuckwheatSageContainer,
    secondary = BuckwheatPeachContainer,
    onSecondary = BuckwheatTerracottaDark,
    secondaryContainer = BuckwheatTerracottaDark,
    onSecondaryContainer = BuckwheatPeachContainer,
    background = Color(0xFF191715),
    onBackground = BuckwheatCanvas,
    surface = Color(0xFF23201E),
    onSurface = BuckwheatCanvas
)

val SplitMateBrandFontFamily = FontFamily.SansSerif

private val SplitMateTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SplitMateBrandFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 54.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1.2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = SplitMateBrandFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 42.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = SplitMateBrandFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SplitMateBrandFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = SplitMateBrandFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SplitMateBrandFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SplitMateBrandFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SplitMateBrandFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SplitMateBrandFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
)

@Composable
fun SplitMateMaterial3ExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useHctDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        useHctDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
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
