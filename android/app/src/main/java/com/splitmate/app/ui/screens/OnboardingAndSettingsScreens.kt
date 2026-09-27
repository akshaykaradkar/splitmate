package com.splitmate.app.ui.screens

import androidx.compose.material.icons.automirrored.rounded.Send
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.splitmate.app.ui.AvatarGender
import com.splitmate.app.ui.AvatarSeedCodec
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.SplitMateAvatarColorPresets
import com.splitmate.app.ui.SplitMateBrandFontFamily
import com.splitmate.app.ui.SplitMateCharacterAvatar
import com.splitmate.app.ui.SplitMateDiceBearStyles
import com.splitmate.app.ui.SplitMateDisplayFontFamily
import com.splitmate.app.ui.buildDiceBearOpenPeepsUrl
import com.splitmate.app.ui.extractInitialsFromNameOrSeed
import com.splitmate.app.ui.inferGenderFromFirstName

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
    onCompleteProfile: (name: String, phone: String, currency: CountryCurrency, avatarSeed: String) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    var nameText by remember { mutableStateOf("") }
    var phoneText by remember { mutableStateOf("") }
    var randomSeedSuffix by remember { mutableStateOf(101) }
    var selectedGender by remember { mutableStateOf(AvatarGender.NEUTRAL) }
    var hasUserManuallySelectedGender by remember { mutableStateOf(false) }
    val selectedCurrency = SupportedCurrencies[0] // Strictly locked to INR (₹)

    LaunchedEffect(nameText, hasUserManuallySelectedGender) {
        if (!hasUserManuallySelectedGender && nameText.isNotBlank()) {
            val inferred = inferGenderFromFirstName(nameText)
            if (inferred != null) {
                selectedGender = inferred
            }
        }
    }

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
    val compositeSeed = remember(effectiveSeed, selectedGender) {
        AvatarSeedCodec.encode(
            seedKey = effectiveSeed,
            gender = selectedGender,
            styleId = "open-peeps",
            colorPresetId = "PastelWall"
        )
    }

    Scaffold(
        containerColor = screenBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
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

                // Avatar Preview
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .clickable { randomSeedSuffix = (100..999).random() },
                    contentAlignment = Alignment.Center
                ) {
                    SplitMateCharacterAvatar(
                        name = compositeSeed,
                        phone = phoneText,
                        size = 106.dp,
                        styleId = "open-peeps",
                        colorPresetId = "PastelWall",
                        gender = selectedGender,
                        highlighted = true
                    )
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

                // Input 1: Presentation Style Toggle (Male, Female, Neutral)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Character Presentation",
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
                            AvatarGender.entries.forEach { genderOption ->
                                val isSelected = selectedGender == genderOption
                                Surface(
                                    onClick = {
                                        com.splitmate.app.ui.performCrispTactileHaptic(context, heavy = false)
                                        hasUserManuallySelectedGender = true
                                        selectedGender = genderOption
                                    },
                                    shape = SplitMateThemeTokens.RadiusPill,
                                    color = if (isSelected) primaryText else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = genderOption.label,
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

                Spacer(modifier = Modifier.height(12.dp))

                // Input 3: Mobile Number
                OutlinedTextField(
                    value = phoneText,
                    onValueChange = { phoneText = it },
                    label = { Text("10-Digit Mobile Number", fontFamily = SplitMateBrandFontFamily, fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("e.g. 9876543210", fontFamily = SplitMateBrandFontFamily) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Phone,
                            contentDescription = null,
                            tint = primaryText
                        )
                    },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
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
                        val finalPhone = phoneText.trim()
                        val styledSeed = AvatarSeedCodec.encode(
                            seedKey = effectiveSeed,
                            gender = selectedGender,
                            styleId = "open-peeps",
                            colorPresetId = "PastelWall"
                        )
                        onCompleteProfile(finalName, finalPhone, selectedCurrency, styledSeed)
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
    userPhone: String = "",
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
    onUpdateUserProfile: (newName: String, newPhone: String, newSeed: String) -> Unit = { _, _, _ -> },
    onThemeToggle: (isDark: Boolean) -> Unit = {},
    onExportLedgerText: () -> String = { "" },
    onClearVaultClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { com.splitmate.app.data.EncryptedPrefsProvider.get(context) }
    var isDarkTheme by remember(isDarkThemeInitial) { mutableStateOf(isDarkThemeInitial) }
    var showResetDataDialog by remember { mutableStateOf(false) }

    var editedName by remember(userName) { mutableStateOf(userName) }
    var editedPhone by remember(userPhone) { mutableStateOf(userPhone) }
    var editedUpiId by remember(upiId) { mutableStateOf(upiId) }
    @Suppress("UNUSED_VARIABLE") var largestRemainderEnabled by remember { mutableStateOf(prefs.getBoolean("pref_largest_remainder", true)) }
    var includeUpiInWhatsApp by remember { mutableStateOf(prefs.getBoolean("pref_whatsapp_upi", true)) }
    var hapticsEnabled by remember { mutableStateOf(prefs.getBoolean("pref_haptics", true)) }

    val parsedInitialDescriptor = remember(avatarSeed, userName) {
        AvatarSeedCodec.parse(
            rawSeed = avatarSeed.ifBlank { userName },
            fallbackStyleId = "open-peeps",
            fallbackColorPresetId = "PastelWall"
        )
    }
    val initialSeedSuffix = remember(parsedInitialDescriptor.seedKey) {
        if (parsedInitialDescriptor.seedKey.contains('_')) {
            parsedInitialDescriptor.seedKey.substringAfterLast('_')
        } else {
            ""
        }
    }
    var selectedGender by remember(parsedInitialDescriptor.gender) { mutableStateOf(parsedInitialDescriptor.gender) }
    var selectedStyleId by remember(parsedInitialDescriptor.styleId) { mutableStateOf(parsedInitialDescriptor.styleId) }
    var selectedColorPresetId by remember(parsedInitialDescriptor.colorPresetId) { mutableStateOf(parsedInitialDescriptor.colorPresetId) }
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

    val effectiveSeedKey = remember(editedName, currentSeedSuffix, parsedInitialDescriptor.seedKey, userName) {
        val cleanEdited = editedName.trim().ifEmpty { "Explorer" }
        if (cleanEdited == userName.trim() && currentSeedSuffix == initialSeedSuffix && parsedInitialDescriptor.seedKey.isNotBlank()) {
            parsedInitialDescriptor.seedKey
        } else if (currentSeedSuffix.isNotBlank()) {
            "${cleanEdited}_$currentSeedSuffix"
        } else {
            cleanEdited
        }
    }
    val effectiveSeed = remember(effectiveSeedKey, selectedGender, selectedStyleId, selectedColorPresetId) {
        AvatarSeedCodec.encode(
            seedKey = effectiveSeedKey,
            gender = selectedGender,
            styleId = selectedStyleId,
            colorPresetId = selectedColorPresetId
        )
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
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 1: User Profile Name, UPI ID & Full 4-Token Avatar Studio
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
                                    .size(92.dp)
                                    .clickable { currentSeedSuffix = (100..999).random().toString() },
                                contentAlignment = Alignment.Center
                            ) {
                                SplitMateCharacterAvatar(
                                    name = effectiveSeed,
                                    phone = editedPhone,
                                    size = 86.dp,
                                    styleId = selectedStyleId,
                                    colorPresetId = selectedColorPresetId,
                                    gender = selectedGender,
                                    highlighted = true
                                )
                                Surface(
                                    onClick = { currentSeedSuffix = (100..999).random().toString() },
                                    shape = CircleShape,
                                    color = Color(0xFF365314),
                                    border = BorderStroke(1.5.dp, Color(0xFFFAF6F0)),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .align(Alignment.BottomEnd)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Casino,
                                            contentDescription = "Shuffle Look",
                                            tint = Color(0xFFD7E8B6),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Tap avatar to shuffle look",
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
                                value = editedPhone,
                                onValueChange = { editedPhone = it },
                                label = {
                                    Text(
                                        text = "10-Digit Mobile Number",
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                placeholder = {
                                    Text("e.g. 9876543210", fontFamily = SplitMateBrandFontFamily)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Phone,
                                        contentDescription = null,
                                        tint = textPrimary
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
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

                            // A. Character Presentation (Male, Female, Neutral)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Character Presentation",
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
                                        AvatarGender.entries.forEach { genderOption ->
                                            val isSelected = selectedGender == genderOption
                                            Surface(
                                                onClick = { selectedGender = genderOption },
                                                shape = SplitMateThemeTokens.RadiusPill,
                                                color = if (isSelected) textPrimary else Color.Transparent,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = genderOption.label,
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

                            // B. 13 Curated Character Art Styles (Static Preview Seeds)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Character Art Style",
                                    fontFamily = SplitMateBrandFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(SplitMateDiceBearStyles, key = { it.id }) { styleSpec ->
                                        val isSelected = selectedStyleId == styleSpec.id
                                        val staticChipSeed = "StylePreview_${styleSpec.id}|${selectedGender.id}|${styleSpec.id}|$selectedColorPresetId"
                                        Surface(
                                            onClick = { selectedStyleId = styleSpec.id },
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) Color(0xFF365314) else cardBg,
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF416913) else borderColor
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                SplitMateCharacterAvatar(
                                                    name = staticChipSeed,
                                                    phone = "",
                                                    size = 28.dp,
                                                    styleId = styleSpec.id,
                                                    colorPresetId = selectedColorPresetId,
                                                    gender = selectedGender,
                                                    highlighted = isSelected
                                                )
                                                Column {
                                                    Text(
                                                        text = styleSpec.label,
                                                        fontFamily = SplitMateBrandFontFamily,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isSelected) Color(0xFFFAF6F0) else textPrimary
                                                    )
                                                    Text(
                                                        text = styleSpec.subtitle,
                                                        fontFamily = SplitMateBrandFontFamily,
                                                        fontSize = 10.sp,
                                                        color = if (isSelected) Color(0xFFD7E8B6) else textSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // C. 12 Curated Backdrop Color Palettes
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Backdrop Palette",
                                    fontFamily = SplitMateBrandFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(SplitMateAvatarColorPresets, key = { it.id }) { preset ->
                                        val isSelected = selectedColorPresetId == preset.id
                                        Surface(
                                            onClick = { selectedColorPresetId = preset.id },
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) preset.primaryBgColor else cardBg,
                                            border = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) preset.accentRingColor else borderColor
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            Brush.linearGradient(
                                                                colors = listOf(preset.primaryBgColor, preset.secondarySwatchColor)
                                                            )
                                                        )
                                                        .border(1.5.dp, preset.accentRingColor, CircleShape)
                                                )
                                                Text(
                                                    text = preset.label,
                                                    fontFamily = SplitMateBrandFontFamily,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                    color = if (isSelected) Color(0xFF23201E) else textPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val clean = editedName.trim().ifEmpty { "Explorer" }
                                    val cleanPhone = editedPhone.trim()
                                    val cleanUpi = editedUpiId.trim()
                                    val encodedSeed = AvatarSeedCodec.encode(
                                        seedKey = effectiveSeedKey,
                                        gender = selectedGender,
                                        styleId = selectedStyleId,
                                        colorPresetId = selectedColorPresetId
                                    )
                                    onUpdateUserProfile(clean, cleanPhone, encodedSeed)
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

            // Section 2: Sharing & Trip Preferences
            item {
                Column {
                    Text(
                        text = "Sharing & Trip Preferences",
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
                                icon = Icons.AutoMirrored.Rounded.Send,
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
                                iconBg = if (isDarkTheme) Color(0xFF282552) else Color(0xFFEEF2FF),
                                iconTint = if (isDarkTheme) Color(0xFFDCE3FD) else Color(0xFF3730A3),
                                title = "Export & Share Trip Summary",
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
                                            android.content.Intent.createChooser(shareIntent, "Share Trip Summary")
                                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Section 3: Appearance & Haptics
            item {
                val settingsLocalView = androidx.compose.ui.platform.LocalView.current
                Column {
                    Text(
                        text = "Appearance & Haptics",
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
                                iconBg = if (isDarkTheme) Color(0xFF282552) else Color(0xFFEEF2FF),
                                iconTint = if (isDarkTheme) Color(0xFFDCE3FD) else Color(0xFF3730A3),
                                title = "Dark Mode (Warm Espresso)",
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
