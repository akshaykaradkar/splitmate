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
import com.splitmate.app.ui.extractPhoneAndNameFromContactUri

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
                            text = "Zero-Signup · Native INR (₹) Vault",
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
                    text = "Set up your local profile & avatar persona.",
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
                        // Clean 1-2 letter name initials ONLY (never "Masculine"/"Feminine")
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
                        text = "Avatar Hair & Presentation Style",
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
                                    onClick = { selectedPresentationStyle = style },
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

                Spacer(modifier = Modifier.height(12.dp))

                // Math & Vault Trust Indicator
                Surface(
                    shape = SplitMateThemeTokens.RadiusCard,
                    color = SplitMateThemeTokens.SageSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = SplitMateThemeTokens.SageText,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Exact Split Precision Engine (₹ INR)",
                                fontFamily = SplitMateBrandFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SplitMateThemeTokens.SageText
                            )
                            Text(
                                text = "Integer-paise precision with local Room SQLite storage.",
                                fontFamily = SplitMateBrandFontFamily,
                                fontSize = 12.sp,
                                color = Color(0xFF23201E)
                            )
                        }
                    }
                }
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
                            text = "Create Profile & Enter Vault",
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

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Encrypted On-Device · No External Accounts Needed",
                    fontFamily = SplitMateBrandFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryText
                )
            }
        }
    }
}

// ==============================================================================
// SCREEN 2: UserSettingsScreen()
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsScreen(
    userName: String = "Akshay",
    avatarSeed: String = userName,
    upiId: String = "9876543210@upi",
    defaultCurrencyCode: String = "INR (₹)",
    totalBalanceText: String = "+₹0.00",
    activeGroupsCount: Int = 0,
    isDarkThemeInitial: Boolean = false,
    allCurrencies: List<com.splitmate.app.data.CurrencyRateEntity> = emptyList(),
    onSyncLiveRates: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onUpdateUpiId: (String) -> Unit = {},
    onUpdateCurrencyCode: (String) -> Unit = {},
    onThemeToggle: (isDark: Boolean) -> Unit = {},
    onClearVaultClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var isDarkTheme by remember(isDarkThemeInitial) { mutableStateOf(isDarkThemeInitial) }
    var showClearVaultDialog by remember { mutableStateOf(false) }
    var showLinkPhoneDialog by remember { mutableStateOf(false) }

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

    val diceBearSvgUrl = remember(avatarSeed) {
        buildDiceBearOpenPeepsUrl(avatarSeed)
    }
    val cleanInitials = remember(userName) {
        extractInitialsFromNameOrSeed(userName)
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
                actions = {
                    Surface(
                        shape = SplitMateThemeTokens.RadiusPill,
                        color = SplitMateThemeTokens.SageSurface,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF388E3C))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "v8.0 INR Vault",
                                fontFamily = SplitMateBrandFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitMateThemeTokens.SageText
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Avatar (88dp) overlapping a compact card with User's Name and Phone-linked UPI
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Card(
                        shape = SplitMateThemeTokens.RadiusCard,
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 44.dp)
                            .border(1.dp, borderColor, SplitMateThemeTokens.RadiusCard)
                            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .padding(top = 42.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = userName,
                                fontFamily = SplitMateDisplayFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Phone-linked UPI Chip with Contact Picker Action
                            Surface(
                                shape = SplitMateThemeTokens.RadiusPill,
                                color = mutedBg,
                                onClick = { showLinkPhoneDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContactPhone,
                                        contentDescription = null,
                                        tint = textPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = upiId.ifBlank { "Link Phone via Contacts" },
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Contacts,
                                        contentDescription = "Link Contact Phone",
                                        tint = textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Stats Bento Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = SplitMateThemeTokens.RadiusPanel,
                                    color = SplitMateThemeTokens.SageSurface,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Balance", fontFamily = SplitMateBrandFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SplitMateThemeTokens.SageText)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(totalBalanceText, fontFamily = SplitMateDisplayFontFamily, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = SplitMateThemeTokens.SageText)
                                    }
                                }

                                Surface(
                                    shape = SplitMateThemeTokens.RadiusPanel,
                                    color = mutedBg,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Active Groups", fontFamily = SplitMateBrandFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("$activeGroupsCount Ledgers", fontFamily = SplitMateDisplayFontFamily, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = textPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // Overlapping Avatar (88dp with clean initials fallback + DiceBear SVG)
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        SplitMateThemeTokens.AccentSage,
                                        Color(0xFFB5DC82)
                                    )
                                )
                            )
                            .border(4.dp, cardBg, CircleShape)
                            .shadow(elevation = 8.dp, shape = CircleShape),
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
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }
            }

            // Section 1: "Payment & Native INR Vault"
            item {
                Column {
                    Text(
                        text = "Payment & UPI Settings",
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
                            // Row 1: Link Phone via Contacts for UPI
                            SettingsRowItem(
                                icon = Icons.Rounded.ContactPhone,
                                iconBg = SplitMateThemeTokens.SageSurface,
                                iconTint = SplitMateThemeTokens.SageText,
                                title = "Phone-Linked UPI Address",
                                subtitle = upiId.ifBlank { "Tap to link via Android Contacts" },
                                trailingContent = {
                                    Surface(
                                        shape = SplitMateThemeTokens.RadiusPill,
                                        color = mutedBg
                                    ) {
                                        Text(
                                            text = "Link Contact",
                                            fontFamily = SplitMateBrandFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                },
                                onClick = { showLinkPhoneDialog = true }
                            )

                            HorizontalDivider(
                                color = borderColor.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )

                            // Row 2: Native INR Currency (Locked)
                            SettingsRowItem(
                                icon = Icons.Rounded.AccountBalanceWallet,
                                iconBg = SplitMateThemeTokens.SageSurface,
                                iconTint = SplitMateThemeTokens.SageText,
                                title = "Native Ledger Currency",
                                subtitle = "Indian Rupee (₹ INR) · UPI Native",
                                trailingContent = {
                                    Surface(
                                        shape = SplitMateThemeTokens.RadiusPill,
                                        color = SplitMateThemeTokens.SageSurface
                                    ) {
                                        Text(
                                            text = "₹ INR",
                                            fontFamily = SplitMateBrandFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = SplitMateThemeTokens.SageText,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                },
                                onClick = {}
                            )
                        }
                    }
                }
            }

            // Section 2: "App Preferences"
            item {
                Column {
                    Text(
                        text = "App Preferences",
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
                            // Row 1: Dark/Light Theme Switch
                            SettingsRowItem(
                                icon = if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                iconBg = Color(0xFFE8EDFB),
                                iconTint = Color(0xFF244896),
                                title = "Dark / Light Theme",
                                subtitle = if (isDarkTheme) "Dark Surface (#121212 · #1E1E1E)" else "Warm Cream Eggshell (#FAF7F2)",
                                trailingContent = {
                                    Switch(
                                        checked = isDarkTheme,
                                        onCheckedChange = {
                                            isDarkTheme = it
                                            onThemeToggle(it)
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
                                }
                            )

                            HorizontalDivider(
                                color = borderColor.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )

                            // Row 2: Clear Local Vault
                            SettingsRowItem(
                                icon = Icons.Rounded.DeleteForever,
                                iconBg = SplitMateThemeTokens.TerracottaSurface,
                                iconTint = SplitMateThemeTokens.TerracottaText,
                                title = "Clear Local Vault",
                                subtitle = "Reset SQLite database & erase offline ledgers",
                                isDestructive = true,
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = SplitMateThemeTokens.TerracottaText
                                    )
                                },
                                onClick = { showClearVaultDialog = true }
                            )
                        }
                    }
                }
            }

            // About & Safety Assurance Footer
            item {
                Card(
                    shape = SplitMateThemeTokens.RadiusPanel,
                    colors = CardDefaults.cardColors(containerColor = SplitMateThemeTokens.SageSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = SplitMateThemeTokens.SageText,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Cryptographic Ledger Sovereignty",
                                fontFamily = SplitMateBrandFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitMateThemeTokens.SageText
                            )
                            Text(
                                text = "All splits calculated with Exact Split parity and held securely in local SQLite.",
                                fontFamily = SplitMateBrandFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFF23201E)
                            )
                        }
                    }
                }
            }
        }
    }

    // M3 Expressive Dialog 1: Clear Local Vault Confirmation
    if (showClearVaultDialog) {
        Dialog(onDismissRequest = { showClearVaultDialog = false }) {
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
                            text = "Clear Local Vault?",
                            fontFamily = SplitMateDisplayFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                    }
                    Text(
                        text = "This will erase all cached expenses, group ledgers, and participant claim records stored on this device. This action cannot be undone.",
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
                            onClick = { showClearVaultDialog = false },
                            shape = SplitMateThemeTokens.RadiusButton
                        ) {
                            Text("Cancel", fontFamily = SplitMateBrandFontFamily, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showClearVaultDialog = false
                                onClearVaultClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SplitMateThemeTokens.TerracottaText),
                            shape = SplitMateThemeTokens.RadiusButton
                        ) {
                            Text("Clear All Data", fontFamily = SplitMateBrandFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // M3 Expressive Dialog 2: Phone-Linked UPI Selector via Android Contacts (Zero Manual UPI Text Fields!)
    if (showLinkPhoneDialog) {
        val initialDigits = remember(upiId) {
            upiId.substringBefore("@").filter { it.isDigit() }
        }
        var pickedPhoneDigits by remember(initialDigits) { mutableStateOf(initialDigits) }
        var selectedSuffix by remember(upiId) {
            mutableStateOf(if (upiId.endsWith("@paytm")) "@paytm" else "@upi")
        }

        val contactLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickContact()
        ) { uri ->
            if (uri != null) {
                val extracted = extractPhoneAndNameFromContactUri(context, uri)
                if (extracted != null && extracted.second.isNotEmpty()) {
                    pickedPhoneDigits = extracted.second
                } else {
                    Toast.makeText(context, "Could not read phone number from selected contact", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                contactLauncher.launch(null)
            } else {
                Toast.makeText(context, "Contacts permission required to link phone number", Toast.LENGTH_SHORT).show()
            }
        }

        Dialog(onDismissRequest = { showLinkPhoneDialog = false }) {
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
                    Text(
                        text = "Link Phone for UPI",
                        fontFamily = SplitMateDisplayFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Text(
                        text = "Pick your phone number from Android Contacts to derive your native UPI route automatically.",
                        fontFamily = SplitMateBrandFontFamily,
                        fontSize = 13.sp,
                        color = textSecondary
                    )

                    Button(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                contactLauncher.launch(null)
                            } else {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SplitMateThemeTokens.AccentSage,
                            contentColor = SplitMateThemeTokens.SageText
                        ),
                        shape = SplitMateThemeTokens.RadiusButton,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Contacts, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pick Phone from Contacts",
                            fontFamily = SplitMateBrandFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("@upi", "@paytm").forEach { suffix ->
                            val isSelected = selectedSuffix == suffix
                            Surface(
                                onClick = { selectedSuffix = suffix },
                                shape = SplitMateThemeTokens.RadiusPill,
                                color = if (isSelected) textPrimary else mutedBg,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Route: $suffix",
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) screenBg else textPrimary
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        shape = SplitMateThemeTokens.RadiusButton,
                        color = mutedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Derived UPI Route:",
                                fontFamily = SplitMateBrandFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary
                            )
                            Text(
                                text = if (pickedPhoneDigits.length >= 6) "$pickedPhoneDigits$selectedSuffix" else "No Contact Linked",
                                fontFamily = SplitMateBrandFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showLinkPhoneDialog = false }) {
                            Text("Cancel", fontFamily = SplitMateBrandFontFamily, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (pickedPhoneDigits.length >= 6) {
                                    onUpdateUpiId("$pickedPhoneDigits$selectedSuffix")
                                }
                                showLinkPhoneDialog = false
                            },
                            enabled = pickedPhoneDigits.length >= 6,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = textPrimary,
                                contentColor = screenBg
                            ),
                            shape = SplitMateThemeTokens.RadiusButton
                        ) {
                            Text("Save Phone Link", fontFamily = SplitMateBrandFontFamily, fontWeight = FontWeight.Bold, color = screenBg)
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
                        color = if (isDestructive) SplitMateThemeTokens.TerracottaText else SplitMateThemeTokens.PrimaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontFamily = SplitMateBrandFontFamily,
                        fontSize = 12.sp,
                        color = SplitMateThemeTokens.TextSecondary,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}
