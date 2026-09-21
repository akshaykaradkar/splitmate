package com.splitmate.app.ui.screens

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
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.splitmate.app.R
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.ui.SplitMateBrandFontFamily
import com.splitmate.app.ui.SplitMateDisplayFontFamily
import com.splitmate.app.ui.buildDiceBearOpenPeepsUrl

// ==============================================================================
// SPLITMATE M3 EXPRESSIVE THEME TOKENS & SHAPES (DARK-MODE ADAPTIVE)
// ==============================================================================
object SplitMateThemeTokens {
    val ScreenBg: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF121212) else Color(0xFFFAF7F2)
    val PrimaryDark: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFFAF7F2) else Color(0xFF23201E)
    val AccentSage = Color(0xFFD7E8B6)             // Active Pill / Secondary Accent
    val SageSurface = Color(0xFFEAF3DC)            // Soft Sage Surface
    val SageText = Color(0xFF2D4810)               // Deep Green Text
    val TerracottaSurface = Color(0xFFFCECE7)      // Blush Terracotta Surface
    val TerracottaText = Color(0xFFC23E2A)         // Terracotta Accent Text
    val BrandCoral = Color(0xFFE06B52)             // Signature Leaf Brand
    val SurfaceWhite: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF1E1D1B) else Color(0xFFFFFFFF)
    val SurfaceMuted: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF282521) else Color(0xFFF2EFE9)
    val BorderLight: Color
        get() = if (SplitMateTheme.isDark) Color(0xFF38332D) else Color(0xFFE6E1D6)
    val TextSecondary: Color
        get() = if (SplitMateTheme.isDark) Color(0xFFB5ADA3) else Color(0xFF756F68)

    // Exaggerated Expressive Radii
    val RadiusHero = RoundedCornerShape(32.dp)
    val RadiusCard = RoundedCornerShape(24.dp)
    val RadiusPanel = RoundedCornerShape(20.dp)
    val RadiusButton = RoundedCornerShape(16.dp)
    val RadiusPill = RoundedCornerShape(999.dp)
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

    val effectiveSeed = remember(nameText, randomSeedSuffix) {
        if (nameText.isBlank()) "Explorer_$randomSeedSuffix" else "${nameText.trim()}_$randomSeedSuffix"
    }
    val diceBearSvgUrl = remember(effectiveSeed, selectedPresentationStyle) {
        buildDiceBearOpenPeepsUrl(effectiveSeed, selectedPresentationStyle)
    }

    Scaffold(
        containerColor = SplitMateThemeTokens.ScreenBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Brand Pill / Badge
                Surface(
                    shape = SplitMateThemeTokens.RadiusPill,
                    color = SplitMateThemeTokens.SageSurface,
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
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
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SplitMateThemeTokens.PrimaryDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Let's set up your profile & avatar persona.",
                    fontFamily = SplitMateBrandFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = SplitMateThemeTokens.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Avatar Preview (120dp circular placeholder with live DiceBear Open-Peeps SVG)
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    SplitMateThemeTokens.AccentSage,
                                    SplitMateThemeTokens.TerracottaSurface
                                )
                            )
                        )
                        .border(4.dp, SplitMateThemeTokens.SurfaceWhite, CircleShape)
                        .shadow(elevation = 12.dp, shape = CircleShape)
                        .clickable { randomSeedSuffix = (100..999).random() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE9F2D8)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(diceBearSvgUrl)
                                .decoderFactory(SvgDecoder.Factory())
                                .crossfade(true)
                                .build(),
                            placeholder = painterResource(id = R.drawable.ic_avatar_placeholder),
                            error = painterResource(id = R.drawable.ic_avatar_placeholder),
                            contentDescription = "DiceBear Avatar Mockup",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                        Surface(
                            shape = SplitMateThemeTokens.RadiusPill,
                            color = SplitMateThemeTokens.PrimaryDark,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = (-4).dp)
                        ) {
                            Text(
                                text = selectedPresentationStyle,
                                fontFamily = SplitMateBrandFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitMateThemeTokens.ScreenBg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tap avatar to randomize look",
                    fontFamily = SplitMateBrandFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SplitMateThemeTokens.TextSecondary,
                    modifier = Modifier.clickable { randomSeedSuffix = (100..999).random() }
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Input 1: Presentation Style Toggle (Masculine, Feminine, Neutral)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Presentation Style",
                        fontFamily = SplitMateBrandFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SplitMateThemeTokens.PrimaryDark,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                    Surface(
                        shape = SplitMateThemeTokens.RadiusPill,
                        color = SplitMateThemeTokens.SurfaceMuted,
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
                                    color = if (isSelected) SplitMateThemeTokens.PrimaryDark else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = style,
                                            fontFamily = SplitMateBrandFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (isSelected) SplitMateThemeTokens.ScreenBg else SplitMateThemeTokens.TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

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
                            tint = SplitMateThemeTokens.PrimaryDark
                        )
                    },
                    singleLine = true,
                    shape = SplitMateThemeTokens.RadiusCard,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SplitMateThemeTokens.PrimaryDark,
                        unfocusedTextColor = SplitMateThemeTokens.PrimaryDark,
                        focusedContainerColor = SplitMateThemeTokens.SurfaceWhite,
                        unfocusedContainerColor = SplitMateThemeTokens.SurfaceWhite,
                        focusedBorderColor = SplitMateThemeTokens.PrimaryDark,
                        unfocusedBorderColor = SplitMateThemeTokens.BorderLight,
                        focusedLabelColor = SplitMateThemeTokens.PrimaryDark,
                        unfocusedLabelColor = SplitMateThemeTokens.TextSecondary,
                        cursorColor = SplitMateThemeTokens.PrimaryDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(minHeight = 56.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Math & Vault Trust Indicator
                Surface(
                    shape = SplitMateThemeTokens.RadiusCard,
                    color = SplitMateThemeTokens.SageSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = SplitMateThemeTokens.SageText,
                            modifier = Modifier.size(26.dp)
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

            // CTA: Massive, pill-shaped primary button at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 12.dp),
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
                        containerColor = SplitMateThemeTokens.PrimaryDark,
                        contentColor = SplitMateThemeTokens.ScreenBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(8.dp, shape = SplitMateThemeTokens.RadiusPill)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Create Profile & Enter Vault",
                            fontFamily = SplitMateBrandFontFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateThemeTokens.ScreenBg
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = SplitMateThemeTokens.ScreenBg,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Encrypted On-Device · No External Accounts Needed",
                    fontFamily = SplitMateBrandFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SplitMateThemeTokens.TextSecondary
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
    upiId: String = "akshay@okaxis",
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
    var showEditUpiDialog by remember { mutableStateOf(false) }

    val screenBg = if (isDarkTheme) Color(0xFF121212) else Color(0xFFFAF7F2)
    val cardBg = if (isDarkTheme) Color(0xFF1E1D1B) else Color(0xFFFFFFFF)
    val mutedBg = if (isDarkTheme) Color(0xFF282521) else Color(0xFFF2EFE9)
    val textPrimary = if (isDarkTheme) Color(0xFFFAF7F2) else Color(0xFF23201E)
    val textSecondary = if (isDarkTheme) Color(0xFFB5ADA3) else Color(0xFF756F68)
    val borderColor = if (isDarkTheme) Color(0xFF38332D) else Color(0xFFE6E1D6)

    val diceBearSvgUrl = remember(avatarSeed) {
        buildDiceBearOpenPeepsUrl(avatarSeed)
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
                                text = "v7.0 INR Vault",
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
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header: Large Avatar (100dp) overlapping a card with User's Name and UPI ID
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 30.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Profile Card Container (24dp radius)
                    Card(
                        shape = SplitMateThemeTokens.RadiusCard,
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp)
                            .border(1.dp, borderColor, SplitMateThemeTokens.RadiusCard)
                            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp)
                                .padding(top = 55.dp), // Space for overlapping 100dp avatar
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = userName,
                                fontFamily = SplitMateDisplayFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // UPI ID Chip with Edit Action
                            Surface(
                                shape = SplitMateThemeTokens.RadiusPill,
                                color = mutedBg,
                                onClick = { showEditUpiDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountBalance,
                                        contentDescription = null,
                                        tint = textPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = upiId,
                                        fontFamily = SplitMateBrandFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit UPI",
                                        tint = textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

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

                    // Overlapping Avatar (100dp with Coil DiceBear Open-Peeps SVG, zero raw SVG text)
                    Box(
                        modifier = Modifier
                            .size(100.dp)
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
                            text = userName.trim().take(2).uppercase().ifEmpty { "SM" },
                            fontFamily = SplitMateDisplayFontFamily,
                            fontSize = 24.sp,
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
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                    )

                    Card(
                        shape = SplitMateThemeTokens.RadiusCard,
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, SplitMateThemeTokens.RadiusCard)
                    ) {
                        Column {
                            // Row 1: Edit UPI ID
                            SettingsRowItem(
                                icon = Icons.Rounded.AccountBalanceWallet,
                                iconBg = SplitMateThemeTokens.SageSurface,
                                iconTint = SplitMateThemeTokens.SageText,
                                title = "UPI Virtual Payment Address",
                                subtitle = upiId,
                                trailingContent = {
                                    Surface(
                                        shape = SplitMateThemeTokens.RadiusPill,
                                        color = mutedBg
                                    ) {
                                        Text(
                                            text = "Edit",
                                            fontFamily = SplitMateBrandFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                },
                                onClick = { showEditUpiDialog = true }
                            )

                            HorizontalDivider(
                                color = borderColor.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 16.dp)
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
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
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
                                subtitle = if (isDarkTheme) "Dark Mode (#121212) enabled" else "Warm Cream Eggshell (#FAF7F2)",
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
                                modifier = Modifier.padding(horizontal = 16.dp)
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
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = SplitMateThemeTokens.SageText,
                            modifier = Modifier.size(24.dp)
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
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
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

    // M3 Expressive Dialog 2: Edit UPI Virtual Payment Address
    if (showEditUpiDialog) {
        var upiInput by remember(upiId) { mutableStateOf(upiId) }
        Dialog(onDismissRequest = { showEditUpiDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = cardBg,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Update UPI ID",
                        fontFamily = SplitMateDisplayFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    OutlinedTextField(
                        value = upiInput,
                        onValueChange = { upiInput = it },
                        label = { Text("UPI Virtual Payment Address", fontFamily = SplitMateBrandFontFamily) },
                        placeholder = { Text("e.g. yourname@okhdfcbank", fontFamily = SplitMateBrandFontFamily) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedContainerColor = mutedBg,
                            unfocusedContainerColor = mutedBg,
                            focusedBorderColor = textPrimary,
                            unfocusedBorderColor = borderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditUpiDialog = false }) {
                            Text("Cancel", fontFamily = SplitMateBrandFontFamily, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onUpdateUpiId(upiInput)
                                showEditUpiDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = textPrimary,
                                contentColor = screenBg
                            ),
                            shape = SplitMateThemeTokens.RadiusButton
                        ) {
                            Text("Save VPA", fontFamily = SplitMateBrandFontFamily, fontWeight = FontWeight.Bold, color = screenBg)
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
            .sizeIn(minHeight = 64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

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
