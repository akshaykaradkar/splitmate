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
import androidx.graphics.shapes.Morph
import com.splitmate.app.R
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.ui.AvatarGender
import com.splitmate.app.ui.AvatarSeedCodec
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.OpenPeepsHeroStage
import com.splitmate.app.ui.SplitMateAvatarColorPresets
import com.splitmate.app.ui.SplitMateBrandFontFamily
import com.splitmate.app.ui.SplitMateCharacterAvatar
import com.splitmate.app.ui.SplitMateDiceBearStyles
import com.splitmate.app.ui.SplitMateDisplayFontFamily
import com.splitmate.app.ui.SplitMateThemeMode
import com.splitmate.app.ui.SplitMateThemeState
import com.splitmate.app.ui.buildDiceBearOpenPeepsUrl
import com.splitmate.app.ui.components.ConnectedButtonGroup
import com.splitmate.app.ui.components.LinearWavyProgressIndicator
import com.splitmate.app.ui.components.MaterialShapes
import com.splitmate.app.ui.components.MorphPolygonShape
import com.splitmate.app.ui.components.RoundedPolygonShape
import com.splitmate.app.ui.components.SplitMateMotion
import com.splitmate.app.ui.components.segmentedIslandItemShape
import com.splitmate.app.ui.extractInitialsFromNameOrSeed
import com.splitmate.app.ui.inferGenderFromFirstName
import com.splitmate.app.ui.toPalette

// ==============================================================================
// SPLITMATE M3 EXPRESSIVE THEME TOKENS & SHAPES (3-THEME EXPRESSIVE PALETTE)
// ==============================================================================
private val SplitMateThemeMode.shortBadgeLabel: String
    get() = when (this) {
        SplitMateThemeMode.SUNLIT_BUCKWHEAT -> "Buckwheat"
        SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> "Espresso"
        SplitMateThemeMode.KYOTO_MATCHA_YUZU -> "Matcha"
    }

private fun persistAndApplyExpressiveThemeMode(
    context: android.content.Context,
    mode: SplitMateThemeMode
) {
    SplitMateThemeState.activeThemeMode = mode
    DesignSystemBindings.activeThemeMode = mode
    SplitMateTheme.isDark = mode.isDark
    runCatching {
        context.getSharedPreferences("splitmate_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("expressive_theme_mode", mode.id)
            .putBoolean("is_dark_theme", mode.isDark)
            .apply()
    }
}

object SplitMateThemeTokens {
    private val resolvedPalette
        get() = when {
            SplitMateTheme.isDark && !DesignSystemBindings.activeThemeMode.isDark ->
                SplitMateThemeMode.WARM_ESPRESSO_NIGHT.toPalette()
            !SplitMateTheme.isDark && DesignSystemBindings.activeThemeMode.isDark ->
                SplitMateThemeMode.SUNLIT_BUCKWHEAT.toPalette()
            else -> DesignSystemBindings.activePalette
        }

    val ScreenBg: Color
        get() = resolvedPalette.surfaceContainerLow
    val PrimaryDark: Color
        get() = resolvedPalette.onSurface
    val AccentSage: Color
        get() = resolvedPalette.primaryContainer
    val SageSurface: Color
        get() = resolvedPalette.primaryContainer
    val SageText: Color
        get() = resolvedPalette.onPrimaryContainer
    val TerracottaSurface: Color
        get() = resolvedPalette.secondaryContainer
    val TerracottaText: Color
        get() = resolvedPalette.onSecondaryContainer
    val BrandCoral: Color
        get() = resolvedPalette.secondary
    val SurfaceWhite: Color
        get() = resolvedPalette.surfaceContainerLowest
    val SurfaceMuted: Color
        get() = resolvedPalette.surfaceContainer
    val BorderLight: Color
        get() = resolvedPalette.outlineVariant
    val TextSecondary: Color
        get() = resolvedPalette.onSurfaceVariant

    val RadiusHero = DesignSystemBindings.GM3ShapeExtraLarge
    val RadiusCard = RoundedCornerShape(20.dp)
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
    val badgeBg: Color = com.splitmate.app.ui.DesignSystemBindings.activePalette.primaryContainer,
    val badgeFg: Color = com.splitmate.app.ui.DesignSystemBindings.activePalette.onPrimaryContainer
)

val SupportedCurrencies = listOf(
    CountryCurrency("INR", "India", "Indian Rupee", "₹", com.splitmate.app.ui.DesignSystemBindings.activePalette.primaryContainer, com.splitmate.app.ui.DesignSystemBindings.activePalette.onPrimaryContainer)
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

                val setupStepsCompleted = remember(nameText, phoneText, hasUserManuallySelectedGender) {
                    var step = 1
                    if (hasUserManuallySelectedGender) step++
                    if (nameText.trim().isNotBlank()) step++
                    if (phoneText.count { it.isDigit() } >= 10) step++
                    step.coerceIn(1, 4)
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearWavyProgressIndicator(
                    progress = setupStepsCompleted / 4f,
                    modifier = Modifier.fillMaxWidth(),
                    color = SplitMateThemeTokens.SageText,
                    trackColor = SplitMateThemeTokens.BorderLight
                )

                Spacer(modifier = Modifier.height(10.dp))

                OpenPeepsHeroStage(
                    name = compositeSeed,
                    phone = phoneText,
                    selectedStyleId = "open-peeps",
                    selectedColorPresetId = "PastelWall",
                    isCompactMode = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                val avatarMorph = remember {
                    Morph(MaterialShapes.SoftBurst, MaterialShapes.Cookie9Sided)
                }
                val avatarMorphProgress by animateFloatAsState(
                    targetValue = if (randomSeedSuffix % 2 == 0) 0f else 1f,
                    animationSpec = SplitMateMotion.fastSpatial(),
                    label = "OnboardingAvatarMorphProgress"
                )

                // Avatar Preview wrapped in MaterialShapes MorphPolygonShape
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(MorphPolygonShape(morph = avatarMorph, percentage = avatarMorphProgress))
                        .background(SplitMateThemeTokens.SageSurface)
                        .clickable { randomSeedSuffix = (100..999).random() },
                    contentAlignment = Alignment.Center
                ) {
                    SplitMateCharacterAvatar(
                        name = compositeSeed,
                        phone = phoneText,
                        size = 104.dp,
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

                // Input 1: Presentation Style Toggle (Male, Female, Neutral) via M3 Expressive ConnectedButtonGroup
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
                    ConnectedButtonGroup(
                        options = AvatarGender.entries,
                        selectedIndex = AvatarGender.entries.indexOf(selectedGender).coerceAtLeast(0),
                        onSelect = { _, genderOption ->
                            com.splitmate.app.ui.performCrispTactileHaptic(context, heavy = false)
                            hasUserManuallySelectedGender = true
                            selectedGender = genderOption
                        },
                        labelProvider = { it.label },
                        modifier = Modifier.fillMaxWidth()
                    )
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
    activeThemeMode: SplitMateThemeMode = DesignSystemBindings.activeThemeMode,
    allCurrencies: List<com.splitmate.app.data.CurrencyRateEntity> = emptyList(),
    onSyncLiveRates: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onUpdateUpiId: (String) -> Unit = {},
    onUpdateCurrencyCode: (String) -> Unit = {},
    onUpdateUserProfile: (newName: String, newPhone: String, newSeed: String) -> Unit = { _, _, _ -> },
    onThemeToggle: (isDark: Boolean) -> Unit = {},
    onSelectThemeMode: (SplitMateThemeMode) -> Unit = {},
    onExportLedgerText: () -> String = { "" },
    onClearVaultClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { com.splitmate.app.data.EncryptedPrefsProvider.get(context) }
    var selectedThemeMode by remember(activeThemeMode, isDarkThemeInitial) {
        mutableStateOf(
            if (isDarkThemeInitial && activeThemeMode != SplitMateThemeMode.WARM_ESPRESSO_NIGHT) {
                SplitMateThemeMode.WARM_ESPRESSO_NIGHT
            } else {
                activeThemeMode
            }
        )
    }
    var isDarkTheme by remember(selectedThemeMode, isDarkThemeInitial) {
        mutableStateOf(selectedThemeMode.isDark || isDarkThemeInitial)
    }
    val activePalette = remember(selectedThemeMode) { selectedThemeMode.toPalette() }
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
        targetValue = activePalette.surfaceContainerLow,
        animationSpec = SplitMateMotion.defaultEffects(),
        label = "SettingsScreenBg"
    )
    val cardBg by animateColorAsState(
        targetValue = activePalette.surfaceContainerLowest,
        animationSpec = SplitMateMotion.defaultEffects(),
        label = "SettingsCardBg"
    )
    val mutedBg by animateColorAsState(
        targetValue = activePalette.surfaceContainer,
        animationSpec = SplitMateMotion.defaultEffects(),
        label = "SettingsMutedBg"
    )
    val textPrimary by animateColorAsState(
        targetValue = activePalette.onSurface,
        animationSpec = SplitMateMotion.defaultEffects(),
        label = "SettingsPrimaryText"
    )
    val textSecondary by animateColorAsState(
        targetValue = activePalette.onSurfaceVariant,
        animationSpec = SplitMateMotion.defaultEffects(),
        label = "SettingsSecondaryText"
    )
    val borderColor by animateColorAsState(
        targetValue = activePalette.outlineVariant,
        animationSpec = SplitMateMotion.defaultEffects(),
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
            // Section 1: User Profile Name, Mobile Number & Full 4-Token Avatar Studio
            item {
                Column {
                    Text(
                        text = "Profile, Avatar & Mobile Number",
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
                            val settingsAvatarMorph = remember {
                                Morph(MaterialShapes.SoftBurst, MaterialShapes.Cookie9Sided)
                            }
                            val settingsMorphProgress by animateFloatAsState(
                                targetValue = if ((currentSeedSuffix.hashCode() and 1) == 0) 0f else 1f,
                                animationSpec = SplitMateMotion.fastSpatial(),
                                label = "SettingsAvatarMorphProgress"
                            )
                            Box(
                                modifier = Modifier
                                    .size(98.dp)
                                    .clickable { currentSeedSuffix = (100..999).random().toString() },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(MorphPolygonShape(morph = settingsAvatarMorph, percentage = settingsMorphProgress))
                                        .background(activePalette.primaryContainer.copy(alpha = 0.45f))
                                )
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
                                    color = activePalette.primary,
                                    border = BorderStroke(1.5.dp, activePalette.surfaceContainerLow),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .align(Alignment.BottomEnd)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Casino,
                                            contentDescription = "Shuffle Look",
                                            tint = activePalette.onPrimary,
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
                                        text = "10-Digit Mobile Number (Optional for Cloud Backup)",
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
                                val genderEntries = remember { AvatarGender.entries }
                                ConnectedButtonGroup(
                                    options = genderEntries,
                                    selectedIndex = genderEntries.indexOf(selectedGender).coerceAtLeast(0),
                                    onSelect = { _, genderOption ->
                                        selectedGender = genderOption
                                    },
                                    labelProvider = { it.label },
                                    modifier = Modifier.fillMaxWidth()
                                )
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
                                            color = if (isSelected) activePalette.primary else cardBg,
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) activePalette.primary else borderColor
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
                                                        color = if (isSelected) activePalette.onPrimary else textPrimary
                                                    )
                                                    Text(
                                                        text = styleSpec.subtitle,
                                                        fontFamily = SplitMateBrandFontFamily,
                                                        fontSize = 11.sp,
                                                        color = if (isSelected) activePalette.primaryContainer else textSecondary
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
                                                    color = if (isSelected) com.splitmate.app.ui.DesignSystemBindings.activePalette.onSurface else textPrimary
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
                                    val encodedSeed = AvatarSeedCodec.encode(
                                        seedKey = effectiveSeedKey,
                                        gender = selectedGender,
                                        styleId = selectedStyleId,
                                        colorPresetId = selectedColorPresetId
                                    )
                                    onUpdateUserProfile(clean, cleanPhone, encodedSeed)
                                    Toast.makeText(context, "Saved Profile", Toast.LENGTH_SHORT).show()
                                },
                                shape = SplitMateThemeTokens.RadiusPill,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = activePalette.primary,
                                    contentColor = activePalette.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "Save Profile",
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
                                icon = Icons.Rounded.Share,
                                iconBg = activePalette.tertiaryContainer,
                                iconTint = activePalette.onTertiaryContainer,
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
                                        "SplitMate Trip Summary (${editedName.ifBlank { userName }})\nActive Groups: $activeGroupsCount"
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

            // Section 3: Appearance, 3-Theme Expressive Studio & Haptics
            item {
                val settingsLocalView = androidx.compose.ui.platform.LocalView.current
                val themeModes = remember {
                    listOf(
                        SplitMateThemeMode.SUNLIT_BUCKWHEAT,
                        SplitMateThemeMode.WARM_ESPRESSO_NIGHT,
                        SplitMateThemeMode.KYOTO_MATCHA_YUZU
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Appearance & Haptics",
                        fontFamily = SplitMateDisplayFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        modifier = Modifier.padding(start = 4.dp)
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
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Expressive Theme Studio",
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "Curated tactile financial palettes with spring transitions",
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontSize = 12.sp,
                                        color = textSecondary
                                    )
                                }
                                Surface(
                                    shape = SplitMateThemeTokens.RadiusPill,
                                    color = activePalette.primaryContainer,
                                    border = BorderStroke(1.dp, activePalette.primary.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = selectedThemeMode.shortBadgeLabel,
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = activePalette.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            ConnectedButtonGroup(
                                options = themeModes,
                                selectedIndex = themeModes.indexOf(selectedThemeMode).coerceAtLeast(0),
                                onSelect = { _, mode ->
                                    selectedThemeMode = mode
                                    isDarkTheme = mode.isDark
                                    persistAndApplyExpressiveThemeMode(context, mode)
                                    onSelectThemeMode(mode)
                                    com.splitmate.app.ui.performCrispTactileHaptic(context, settingsLocalView, heavy = false)
                                },
                                labelProvider = { it.shortBadgeLabel },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                themeModes.forEachIndexed { idx, mode ->
                                    val modePalette = mode.toPalette()
                                    val isSelected = selectedThemeMode == mode
                                    val rowShape = segmentedIslandItemShape(
                                        index = idx,
                                        totalCount = themeModes.size,
                                        isSelected = isSelected,
                                        outerCorner = 20.dp,
                                        innerCorner = 6.dp
                                    )
                                    val paletteHexSummary = when (mode) {
                                        SplitMateThemeMode.SUNLIT_BUCKWHEAT -> "#FAF6F0 · #365314 · #D9F99D · #FED8C8"
                                        SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> "#1C1815 · #A3E635 · #283D0E · #FB923C"
                                        SplitMateThemeMode.KYOTO_MATCHA_YUZU -> "#DDF0D5 · #0F6B3E · #86EFAC · #FEF08A"
                                    }
                                    Surface(
                                        onClick = {
                                            selectedThemeMode = mode
                                            isDarkTheme = mode.isDark
                                            persistAndApplyExpressiveThemeMode(context, mode)
                                            onSelectThemeMode(mode)
                                            com.splitmate.app.ui.performCrispTactileHaptic(context, settingsLocalView, heavy = false)
                                        },
                                        shape = rowShape,
                                        color = if (isSelected) modePalette.primaryContainer.copy(alpha = 0.52f) else mutedBg.copy(alpha = 0.55f),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) modePalette.primary else borderColor
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                                    listOf(
                                                        modePalette.surfaceContainerLow,
                                                        modePalette.primary,
                                                        modePalette.primaryContainer,
                                                        modePalette.secondaryContainer
                                                    ).forEach { swatch ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(20.dp)
                                                                .clip(CircleShape)
                                                                .background(swatch)
                                                                .border(1.dp, modePalette.onSurface.copy(alpha = 0.3f), CircleShape)
                                                        )
                                                    }
                                                }
                                                Column {
                                                    Text(
                                                        text = mode.displayName,
                                                        fontFamily = SplitMateBrandFontFamily,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = textPrimary
                                                    )
                                                    Text(
                                                        text = mode.subtitle,
                                                        fontFamily = SplitMateBrandFontFamily,
                                                        fontSize = 11.sp,
                                                        color = textSecondary
                                                    )
                                                    Text(
                                                        text = paletteHexSummary,
                                                        fontFamily = com.splitmate.app.ui.SplitMateTnumMonospace,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) modePalette.primary else textSecondary.copy(alpha = 0.85f)
                                                    )
                                                }
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = "Active Theme",
                                                    tint = modePalette.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

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
                                iconBg = activePalette.tertiaryContainer,
                                iconTint = activePalette.onTertiaryContainer,
                                title = "Dark Mode (Warm Espresso)",
                                subtitle = "Switch between Daylight Expressive and Warm Espresso Night canvas",
                                titleColor = textPrimary,
                                subtitleColor = textSecondary,
                                trailingContent = {
                                    Switch(
                                        checked = isDarkTheme,
                                        onCheckedChange = {
                                            isDarkTheme = it
                                            val targetMode = if (it) {
                                                SplitMateThemeMode.WARM_ESPRESSO_NIGHT
                                            } else if (selectedThemeMode == SplitMateThemeMode.WARM_ESPRESSO_NIGHT) {
                                                SplitMateThemeMode.SUNLIT_BUCKWHEAT
                                            } else {
                                                selectedThemeMode
                                            }
                                            selectedThemeMode = targetMode
                                            persistAndApplyExpressiveThemeMode(context, targetMode)
                                            onSelectThemeMode(targetMode)
                                            com.splitmate.app.ui.performCrispTactileHaptic(context, settingsLocalView, heavy = false)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = com.splitmate.app.ui.DesignSystemBindings.activePalette.onPrimary,
                                            checkedTrackColor = activePalette.primary,
                                            uncheckedThumbColor = activePalette.onSurface,
                                            uncheckedTrackColor = mutedBg
                                        )
                                    )
                                },
                                onClick = {
                                    val nextDark = !isDarkTheme
                                    isDarkTheme = nextDark
                                    val targetMode = if (nextDark) {
                                        SplitMateThemeMode.WARM_ESPRESSO_NIGHT
                                    } else {
                                        SplitMateThemeMode.SUNLIT_BUCKWHEAT
                                    }
                                    selectedThemeMode = targetMode
                                    persistAndApplyExpressiveThemeMode(context, targetMode)
                                    onSelectThemeMode(targetMode)
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
                                            checkedThumbColor = com.splitmate.app.ui.DesignSystemBindings.activePalette.onPrimary,
                                            checkedTrackColor = activePalette.primary,
                                            uncheckedThumbColor = activePalette.onSurface,
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

            // Section 4: Data Management (Reset App Data)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Data Management",
                        fontFamily = SplitMateDisplayFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        modifier = Modifier.padding(start = 4.dp)
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

                    Text(
                        text = "SplitMate v2.3.1 | Material 3 Expressive (Build 49)",
                        fontFamily = SplitMateBrandFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )
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
                            Text("Reset Data", fontFamily = SplitMateBrandFontFamily, fontWeight = FontWeight.Bold, color = com.splitmate.app.ui.DesignSystemBindings.activePalette.onPrimary)
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
