package com.splitmate.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.splitmate.app.R
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.SplitMateBrandFontFamily
import com.splitmate.app.ui.SplitMateDisplayFontFamily
import com.splitmate.app.ui.buildDiceBearOpenPeepsUrl
import com.splitmate.app.ui.extractInitialsFromNameOrSeed

// ==============================================================================
// SPLITMATE M3 EXPRESSIVE THEME TOKENS & SHAPES (GM3 DARK ELEVATION COMPLIANT)
// ==============================================================================
object SplitMateThemeTokens {
    val ScreenBg: Color
        get() = if (SplitMateTheme.isDark) DesignSystemBindings.GM3DarkBackground else DesignSystemBindings.GM3LightBackground
    val PrimaryDark: Color
        get() = if (SplitMateTheme.isDark) DesignSystemBindings.GM3DarkPrimaryText else DesignSystemBindings.GM3LightPrimaryText
    val AccentSage = DesignSystemBindings.ElementsPositiveContainer
    val SageSurface = Color(0xFFEAF3DC)
    val SageText = DesignSystemBindings.ElementsPositiveText
    val TerracottaSurface = Color(0xFFFCECE7)
    val TerracottaText = DesignSystemBindings.ElementsNegativeText
    val BrandCoral = Color(0xFFE06B52)
    val SurfaceWhite: Color
        get() = if (SplitMateTheme.isDark) DesignSystemBindings.GM3DarkCardSurface else DesignSystemBindings.GM3LightCardSurface
    val SurfaceMuted: Color
        get() = if (SplitMateTheme.isDark) DesignSystemBindings.GM3DarkKeypadSurface else DesignSystemBindings.GM3LightKeypadSurface
    val BorderLight: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF333333) else Color(0xFFE6E1D6)
    val TextSecondary: Color
        get() = if (SplitMateTheme.isDark) DesignSystemBindings.GM3DarkSubtitleText else DesignSystemBindings.GM3LightSubtitleText

    val RadiusHero = DesignSystemBindings.GM3ShapeExtraLarge
    val RadiusCard = DesignSystemBindings.GM3ShapeLarge
    val RadiusPanel = RoundedCornerShape(20.dp)
    val RadiusButton = RoundedCornerShape(16.dp)
    val RadiusPill = DesignSystemBindings.GM3ShapePill
}

// Data model locked strictly to India - INR (₹)
data class CountryCurrency(
    val code: String,
    val country: String,
    val currencyName: String,
    val symbol: String,
    val badgeBg: Color = Color(0xFFD7E8B6),
    val badgeFg: Color = Color(0xFF2D4810)
)

val SupportedCurrencies = listOf(
    CountryCurrency("INR", "India", "Indian Rupee", "₹", Color(0xFFD7E8B6), Color(0xFF2D4810))
)

// ==============================================================================
// SCREEN 1: OnboardingSetupScreen()
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingSetupScreen(
    onCompleteProfile: (name: String, currency: CountryCurrency, avatarSeed: String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var nameText by remember { mutableStateOf("") }
    var randomSeedSuffix by remember { mutableStateOf(101) }
    var selectedPresentationStyle by remember { mutableStateOf("Masculine") }
    val selectedCurrency = SupportedCurrencies[0] // Strictly locked to INR (₹)

    val screenBg by animateColorAsState(
        targetValue = SplitMateThemeTokens.ScreenBg,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "OnboardingScreenBg"
    )
    val primaryText by animateColorAsState(
        targetValue = SplitMateThemeTokens.PrimaryDark,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "OnboardingPrimaryText"
    )
    val cardBg by animateColorAsState(
        targetValue = SplitMateThemeTokens.SurfaceWhite,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "OnboardingCardBg"
    )
    val mutedBg by animateColorAsState(
        targetValue = SplitMateThemeTokens.SurfaceMuted,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "OnboardingMutedBg"
    )
    val secondaryText by animateColorAsState(
        targetValue = SplitMateThemeTokens.TextSecondary,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "OnboardingSecondaryText"
    )

    val effectiveSeed = remember(nameText, randomSeedSuffix) {
        if (nameText.isBlank()) "Explorer_$randomSeedSuffix" else "${nameText.trim()}_$randomSeedSuffix"
    }
    val diceBearSvgUrl = remember(effectiveSeed, selectedPresentationStyle) {
        buildDiceBearOpenPeepsUrl(effectiveSeed, selectedPresentationStyle)
    }
    val cleanInitials = remember(nameText) {
        extractInitialsFromNameOrSeed(nameText.ifBlank { "Explorer" })
    }

    Scaffold(
        containerColor = screenBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // Brand Pill / Badge
                Surface(
                    shape = SplitMateThemeTokens.RadiusPill,
                    color = SplitMateThemeTokens.SageSurface,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SplitMateThemeTokens.BrandCoral)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Split Bills Effortlessly with Friends",
                            fontFamily = SplitMateBrandFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitMateThemeTokens.SageText
                        )
                    }
                }

                // Header
                Text(
                    text = "Welcome to SplitMate",
                    fontFamily = SplitMateDisplayFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryText,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Set up your profile name and avatar style.",
                    fontFamily = SplitMateBrandFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = secondaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Avatar Preview (112dp circular placeholder with clean initials fallback & live DiceBear SVG)
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    SplitMateThemeTokens.AccentSage,
                                    SplitMateThemeTokens.TerracottaSurface
                                )
                            )
                        )
                        .border(3.dp, cardBg, CircleShape)
                        .shadow(elevation = 10.dp, shape = CircleShape)
                        .clickable { randomSeedSuffix = (100..999).random() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(102.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE9F2D8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cleanInitials,
                            fontFamily = SplitMateDisplayFontFamily,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateThemeTokens.SageText
                        )
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(diceBearSvgUrl)
                                .decoderFactory(SvgDecoder.Factory())
                                .crossfade(true)
                                .build(),
                            placeholder = painterResource(id = R.drawable.ic_avatar_placeholder),
                            error = painterResource(id = R.drawable.ic_avatar_placeholder),
                            contentDescription = "DiceBear Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap avatar to randomize look",
                    fontFamily = SplitMateBrandFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryText,
                    modifier = Modifier.clickable { randomSeedSuffix = (100..999).random() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Input 1: Presentation Style Toggle (Masculine, Feminine, Neutral)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Avatar Presentation Style",
                        fontFamily = SplitMateBrandFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                    Surface(
                        shape = SplitMateThemeTokens.RadiusPill,
                        color = mutedBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SplitMateThemeTokens.BorderLight, SplitMateThemeTokens.RadiusPill)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Masculine", "Feminine", "Neutral").forEach { style ->
                                val isSelected = selectedPresentationStyle == style
                                Surface(
                                    onClick = {
                                        com.splitmate.app.ui.performCrispTactileHaptic(context, heavy = false)
                                        selectedPresentationStyle = style
                                    },
                                    shape = SplitMateThemeTokens.RadiusPill,
                                    color = if (isSelected) primaryText else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = style,
                                            fontFamily = SplitMateBrandFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (isSelected) screenBg else secondaryText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input 2: Material 3 OutlinedTextField for "Your Name"
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Your Name", fontFamily = SplitMateBrandFontFamily, fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("e.g. Akshay Karadkar", fontFamily = SplitMateBrandFontFamily) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = primaryText
                        )
                    },
                    singleLine = true,
                    shape = SplitMateThemeTokens.RadiusCard,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText,
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedBorderColor = primaryText,
                        unfocusedBorderColor = SplitMateThemeTokens.BorderLight,
                        focusedLabelColor = primaryText,
                        unfocusedLabelColor = secondaryText,
                        cursorColor = primaryText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(minHeight = 54.dp)
                )
            }

            // CTA: Primary pill button at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        com.splitmate.app.ui.performCrispTactileHaptic(context, heavy = true)
                        val finalName = if (nameText.isBlank()) "Explorer" else nameText.trim()
                        val styledSeed = "$effectiveSeed|$selectedPresentationStyle"
                        onCompleteProfile(finalName, selectedCurrency, styledSeed)
                    },
                    shape = SplitMateThemeTokens.RadiusPill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryText,
                        contentColor = screenBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(8.dp, shape = SplitMateThemeTokens.RadiusPill)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Continue to SplitMate",
                            fontFamily = SplitMateBrandFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = screenBg
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = screenBg,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==============================================================================
// SCREEN 2: UserSettingsScreen()
// Strictly contains:
// 1. User Profile Name & Avatar presentation style
// 2. Appearance (Dark Theme toggle)
// 3. Data Management (Reset App Data)
// ==============================================================================
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsScreen(
    userName: String = "Akshay",
    avatarSeed: String = userName,
    upiId: String = "",
    defaultCurrencyCode: String = "INR",
    totalBalanceText: String = "₹0.00",
    activeGroupsCount: Int = 0,
    isDarkThemeInitial: Boolean = false,
    allCurrencies: List<com.splitmate.app.data.CurrencyRateEntity> = emptyList(),
    onSyncLiveRates: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onUpdateUpiId: (String) -> Unit = {},
    onUpdateCurrencyCode: (String) -> Unit = {},
    onUpdateUserProfile: (newName: String, newSeed: String) -> Unit = { _, _ -> },
    onThemeToggle: (isDark: Boolean) -> Unit = {},
    onExportLedgerText: () -> String = { "" },
    onClearVaultClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { com.splitmate.app.data.EncryptedPrefsProvider.get(context) }
    var isDarkTheme by remember(isDarkThemeInitial) { mutableStateOf(isDarkThemeInitial) }
    var showResetDataDialog by remember { mutableStateOf(false) }

    var editedName by remember(userName) { mutableStateOf(userName) }
    var editedUpiId by remember(upiId) { mutableStateOf(upiId) }
    var largestRemainderEnabled by remember { mutableStateOf(prefs.getBoolean("pref_largest_remainder", true)) }
    var includeUpiInWhatsApp by remember { mutableStateOf(prefs.getBoolean("pref_whatsapp_upi", true)) }
    var hapticsEnabled by remember { mutableStateOf(prefs.getBoolean("pref_haptics", true)) }

    val initialStyle = remember(avatarSeed) {
        val part = avatarSeed.substringAfter('|', "Masculine")
        if (part in listOf("Masculine", "Feminine", "Neutral")) part else "Masculine"
    }
    val initialSeedSuffix = remember(avatarSeed) {
        val basePart = avatarSeed.substringBefore('|')
        if (basePart.contains('_')) basePart.substringAfterLast('_') else ""
    }
    var selectedStyle by remember(initialStyle) { mutableStateOf(initialStyle) }
    var currentSeedSuffix by remember(initialSeedSuffix) { mutableStateOf(initialSeedSuffix) }

    val screenBg by animateColorAsState(
        targetValue = if (isDarkTheme) DesignSystemBindings.GM3DarkBackground else DesignSystemBindings.GM3LightBackground,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "SettingsScreenBg"
    )
    val cardBg by animateColorAsState(
        targetValue = if (isDarkTheme) DesignSystemBindings.GM3DarkCardSurface else DesignSystemBindings.GM3LightCardSurface,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "SettingsCardBg"
    )
    val mutedBg by animateColorAsState(
        targetValue = if (isDarkTheme) DesignSystemBindings.GM3DarkKeypadSurface else DesignSystemBindings.GM3LightKeypadSurface,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "SettingsMutedBg"
    )
    val textPrimary by animateColorAsState(
        targetValue = if (isDarkTheme) DesignSystemBindings.GM3DarkPrimaryText else DesignSystemBindings.GM3LightPrimaryText,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "SettingsPrimaryText"
    )
    val textSecondary by animateColorAsState(
        targetValue = if (isDarkTheme) DesignSystemBindings.GM3DarkSubtitleText else DesignSystemBindings.GM3LightSubtitleText,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "SettingsSecondaryText"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isDarkTheme) Color(0xFF333333) else Color(0xFFE6E1D6),
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "SettingsBorderColor"
    )

    val effectiveSeed = remember(editedName, currentSeedSuffix, selectedStyle, avatarSeed, userName) {
        val cleanEdited = editedName.trim().ifEmpty { "Explorer" }
        val basePart = if (cleanEdited == userName.trim() && currentSeedSuffix == initialSeedSuffix && avatarSeed.isNotBlank()) {
            avatarSeed.substringBefore('|').ifBlank { cleanEdited }
        } else if (currentSeedSuffix.isNotBlank()) {
            "${cleanEdited}_$currentSeedSuffix"
        } else {
            cleanEdited
        }
        "$basePart|$selectedStyle"
    }
    val diceBearSvgUrl = remember(effectiveSeed) {
        buildDiceBearOpenPeepsUrl(effectiveSeed)
    }
    val cleanInitials = remember(editedName) {
        extractInitialsFromNameOrSeed(editedName.ifBlank { userName })
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Preferences",
                        fontFamily = SplitMateDisplayFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = screenBg
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 1: User Profile Name, UPI ID & Avatar Presentation Style
            item {
                Column {
                    Text(
                        text = "Profile, Avatar & UPI Handle",
                        fontFamily = SplitMateDisplayFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Card(
                        shape = SplitMateThemeTokens.RadiusCard,
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, SplitMateThemeTokens.RadiusCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                SplitMateThemeTokens.AccentSage,
                                                Color(0xFFB5DC82)
                                            )
                                        )
                                    )
                                    .border(3.dp, cardBg, CircleShape)
                                    .shadow(elevation = 6.dp, shape = CircleShape)
                                    .clickable { currentSeedSuffix = (100..999).random().toString() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cleanInitials,
                                    fontFamily = SplitMateDisplayFontFamily,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SplitMateThemeTokens.SageText
                                )
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(diceBearSvgUrl)
                                        .decoderFactory(SvgDecoder.Factory())
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profile Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                            Text(
                                text = "Tap avatar to randomize look",
                                fontFamily = SplitMateBrandFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = textSecondary,
                                modifier = Modifier.clickable { currentSeedSuffix = (100..999).random().toString() }
                            )

                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                label = {
                                    Text(
                                        text = "Display Name",
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = textPrimary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedContainerColor = mutedBg.copy(alpha = 0.4f),
                                    unfocusedContainerColor = mutedBg.copy(alpha = 0.4f),
                                    focusedBorderColor = textPrimary,
                                    unfocusedBorderColor = borderColor,
                                    focusedLabelColor = textPrimary,
                                    unfocusedLabelColor = textSecondary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = editedUpiId,
                                onValueChange = { editedUpiId = it },
                                label = {
                                    Text(
                                        text = "Your UPI ID / Phone (for WhatsApp Reminders)",
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                placeholder = {
                                    Text("e.g. akshay@okaxis or 9876543210@upi", fontFamily = SplitMateBrandFontFamily)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCode2,
                                        contentDescription = null,
                                        tint = textPrimary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedContainerColor = mutedBg.copy(alpha = 0.4f),
                                    unfocusedContainerColor = mutedBg.copy(alpha = 0.4f),
                                    focusedBorderColor = textPrimary,
                                    unfocusedBorderColor = borderColor,
                                    focusedLabelColor = textPrimary,
                                    unfocusedLabelColor = textSecondary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Avatar Presentation Style",
                                    fontFamily = SplitMateBrandFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                                )
                                Surface(
                                    shape = SplitMateThemeTokens.RadiusPill,
                                    color = mutedBg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, borderColor, SplitMateThemeTokens.RadiusPill)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Masculine", "Feminine", "Neutral").forEach { style ->
                                            val isSelected = selectedStyle == style
                                            Surface(
                                                onClick = { selectedStyle = style },
                                                shape = SplitMateThemeTokens.RadiusPill,
                                                color = if (isSelected) textPrimary else Color.Transparent,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = style,
                                                        fontFamily = SplitMateBrandFontFamily,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                        color = if (isSelected) screenBg else textSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val clean = editedName.trim().ifEmpty { "Explorer" }
                                    val cleanUpi = editedUpiId.trim()
                                    onUpdateUserProfile(clean, "$clean|$selectedStyle")
                                    onUpdateUpiId(cleanUpi)
                                    Toast.makeText(context, "Saved Profile & UPI Handle", Toast.LENGTH_SHORT).show()
                                },
                                shape = SplitMateThemeTokens.RadiusPill,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = textPrimary,
                                    contentColor = screenBg
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "Save Profile & UPI Handle",
                                    fontFamily = SplitMateBrandFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Split Engine & WhatsApp Preferences
            item {
                Column {
                    Text(
                        text = "Split Engine & Trip Tools",
                        fontFamily = SplitMateDisplayFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Card(
                        shape = SplitMateThemeTokens.RadiusCard,
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, SplitMateThemeTokens.RadiusCard)
                    ) {
                        Column {
                            SettingsRowItem(
                                icon = Icons.Rounded.Send,
                                iconBg = SplitMateThemeTokens.AccentSage.copy(alpha = 0.45f),
                                iconTint = SplitMateThemeTokens.SageText,
                                title = "Include My UPI ID in WhatsApp Reminders",
                                subtitle = "Embed your UPI handle in 1-tap WhatsApp settlement messages (INR ₹)",
                                titleColor = textPrimary,
                                subtitleColor = textSecondary,
                                trailingContent = {
                                    Switch(
                                        checked = includeUpiInWhatsApp,
                                        onCheckedChange = {
                                            includeUpiInWhatsApp = it
                                            prefs.edit().putBoolean("pref_whatsapp_upi", it).apply()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF416913),
                                            uncheckedThumbColor = Color(0xFF23201E),
                                            uncheckedTrackColor = mutedBg
                                        )
                                    )
                                },
                                onClick = {
                                    includeUpiInWhatsApp = !includeUpiInWhatsApp
                                    prefs.edit().putBoolean("pref_whatsapp_upi", includeUpiInWhatsApp).apply()
                                }
                            )

                            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))

                            SettingsRowItem(
                                icon = Icons.Rounded.Share,
                                iconBg = Color(0xFFE8EDFB),
                                iconTint = Color(0xFF244896),
                                title = "Export & Share Trip Ledger Summary",
                                subtitle = "Share a clean WhatsApp/Clipboard summary of all group balances & expenses ($activeGroupsCount active groups)",
                                titleColor = textPrimary,
                                subtitleColor = textSecondary,
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = textPrimary
                                    )
                                },
                                onClick = {
                                    val summaryText = onExportLedgerText().ifBlank {
                                        "SplitMate Trip Summary (${editedName.ifBlank { userName }})\nActive Groups: $activeGroupsCount\nUPI Handle: ${editedUpiId.ifBlank { "Not configured" }}"
                                    }
                                    runCatching {
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_TEXT, summaryText)
                                        }
                                        context.startActivity(
                                            android.content.Intent.createChooser(shareIntent, "Share Trip Ledger Summary")
                                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Section 3: Appearance & Tactile Feedback
            item {
                val settingsLocalView = androidx.compose.ui.platform.LocalView.current
                Column {
                    Text(
                        text = "Appearance & Tactile Physics",
                        fontFamily = SplitMateDisplayFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Card(
                        shape = SplitMateThemeTokens.RadiusCard,
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, SplitMateThemeTokens.RadiusCard)
                    ) {
                        Column {
                            SettingsRowItem(
                                icon = if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                iconBg = Color(0xFFE8EDFB),
                                iconTint = Color(0xFF244896),
                                title = "Dark Theme (Warm Espresso #181512)",
                                subtitle = "Switch between Buckwheat Cream Light and Warm Espresso Night canvas",
                                titleColor = textPrimary,
                                subtitleColor = textSecondary,
                                trailingContent = {
                                    Switch(
                                        checked = isDarkTheme,
                                        onCheckedChange = {
                                            isDarkTheme = it
                                            onThemeToggle(it)
                                            com.splitmate.app.ui.performCrispTactileHaptic(context, settingsLocalView, heavy = false)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF416913),
                                            uncheckedThumbColor = Color(0xFF23201E),
                                            uncheckedTrackColor = mutedBg
                                        )
                                    )
                                },
                                onClick = {
                                    isDarkTheme = !isDarkTheme
                                    onThemeToggle(isDarkTheme)
                                    com.splitmate.app.ui.performCrispTactileHaptic(context, settingsLocalView, heavy = false)
                                }
                            )

                            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))

                            SettingsRowItem(
                                icon = Icons.Rounded.Vibration,
                                iconBg = SplitMateThemeTokens.AccentSage.copy(alpha = 0.45f),
                                iconTint = SplitMateThemeTokens.SageText,
                                title = "Tactile Keypad Haptics",
                                subtitle = "Crisp hardware vibration feedback when typing amounts & splitting",
                                titleColor = textPrimary,
                                subtitleColor = textSecondary,
                                trailingContent = {
                                    Switch(
                                        checked = hapticsEnabled,
                                        onCheckedChange = {
                                            hapticsEnabled = it
                                            prefs.edit().putBoolean("pref_haptics", it).apply()
                                            if (it) {
                                                com.splitmate.app.ui.performCrispTactileHaptic(context, settingsLocalView, heavy = true)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF416913),
                                            uncheckedThumbColor = Color(0xFF23201E),
                                            uncheckedTrackColor = mutedBg
                                        )
                                    )
                                },
                                onClick = {
                                    hapticsEnabled = !hapticsEnabled
                                    prefs.edit().putBoolean("pref_haptics", hapticsEnabled).apply()
                                    if (hapticsEnabled) {
                                        com.splitmate.app.ui.performCrispTactileHaptic(context, settingsLocalView, heavy = true)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Section 3: Data Management (Reset App Data)
            item {
                Column {
                    Text(
                        text = "Data Management",
                        fontFamily = SplitMateDisplayFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Card(
                        shape = SplitMateThemeTokens.RadiusCard,
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, SplitMateThemeTokens.RadiusCard)
                    ) {
                        SettingsRowItem(
                            icon = Icons.Rounded.DeleteForever,
                            iconBg = SplitMateThemeTokens.TerracottaSurface,
                            iconTint = SplitMateThemeTokens.TerracottaText,
                            title = "Reset App Data",
                            subtitle = "Permanently delete all groups, members, and expense history from this device.",
                            titleColor = SplitMateThemeTokens.TerracottaText,
                            subtitleColor = textSecondary,
                            isDestructive = true,
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = SplitMateThemeTokens.TerracottaText
                                )
                            },
                            onClick = { showResetDataDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Reset App Data
    if (showResetDataDialog) {
        Dialog(onDismissRequest = { showResetDataDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = cardBg,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SplitMateThemeTokens.TerracottaSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = SplitMateThemeTokens.TerracottaText)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Reset App Data?",
                            fontFamily = SplitMateDisplayFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                    }
                    Text(
                        text = "Permanently delete all groups, members, and expense history from this device. This action cannot be undone.",
                        fontFamily = SplitMateBrandFontFamily,
                        color = textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showResetDataDialog = false },
                            shape = SplitMateThemeTokens.RadiusButton
                        ) {
                            Text("Cancel", fontFamily = SplitMateBrandFontFamily, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showResetDataDialog = false
                                onClearVaultClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SplitMateThemeTokens.TerracottaText),
                            shape = SplitMateThemeTokens.RadiusButton
                        ) {
                            Text("Reset Data", fontFamily = SplitMateBrandFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// REUSABLE M3 EXPRESSIVE SETTINGS ROW COMPONENT
// ==============================================================================
@Composable
fun SettingsRowItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    titleColor: Color = SplitMateThemeTokens.PrimaryDark,
    subtitleColor: Color = SplitMateThemeTokens.TextSecondary,
    isDestructive: Boolean = false,
    trailingContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 60.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontFamily = SplitMateBrandFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDestructive) SplitMateThemeTokens.TerracottaText else titleColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontFamily = SplitMateBrandFontFamily,
                        fontSize = 12.sp,
                        color = subtitleColor,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}
