package com.splitmate.app.ui.screens

import android.net.Uri
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

// ==============================================================================
// SPLITMATE M3 EXPRESSIVE THEME TOKENS & SHAPES
// ==============================================================================
object SplitMateThemeTokens {
    val ScreenBg = Color(0xFFFAF7F2)               // Warm Cream Eggshell
    val PrimaryDark = Color(0xFF23201E)            // Charcoal Espresso
    val AccentSage = Color(0xFFD7E8B6)             // Active Pill / Secondary Accent
    val SageSurface = Color(0xFFEAF3DC)            // Soft Sage Surface
    val SageText = Color(0xFF2D4810)               // Deep Green Text
    val TerracottaSurface = Color(0xFFFCECE7)      // Blush Terracotta Surface
    val TerracottaText = Color(0xFFC23E2A)         // Terracotta Accent Text
    val BrandCoral = Color(0xFFE06B52)             // Signature Leaf Brand
    val SurfaceWhite = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF2EFE9)
    val BorderLight = Color(0xFFE6E1D6)
    val TextSecondary = Color(0xFF756F68)

    // Exaggerated Expressive Radii
    val RadiusHero = RoundedCornerShape(32.dp)
    val RadiusCard = RoundedCornerShape(24.dp)
    val RadiusPanel = RoundedCornerShape(20.dp)
    val RadiusButton = RoundedCornerShape(16.dp)
    val RadiusPill = RoundedCornerShape(999.dp)
}

// Data models for Currencies & Countries (Circular symbol badges, zero emoji flags)
data class CountryCurrency(
    val code: String,
    val country: String,
    val currencyName: String,
    val symbol: String,
    val badgeBg: Color = Color(0xFFD7E8B6),
    val badgeFg: Color = Color(0xFF2D4810)
)

val SupportedCurrencies = listOf(
    CountryCurrency("INR", "India", "Indian Rupee", "₹", Color(0xFFD7E8B6), Color(0xFF2D4810)),
    CountryCurrency("USD", "United States", "US Dollar", "$", Color(0xFFFFD8CC), Color(0xFF8A2E1A)),
    CountryCurrency("EUR", "European Union", "Euro", "€", Color(0xFFD0E2FF), Color(0xFF143E82)),
    CountryCurrency("GBP", "United Kingdom", "British Pound", "£", Color(0xFFFFD5E5), Color(0xFF801844)),
    CountryCurrency("JPY", "Japan", "Japanese Yen", "¥", Color(0xFFE5DCFF), Color(0xFF452285)),
    CountryCurrency("CAD", "Canada", "Canadian Dollar", "C$", Color(0xFFD2F5DC), Color(0xFF1B6331)),
    CountryCurrency("AUD", "Australia", "Australian Dollar", "A$", Color(0xFFD7E8B6), Color(0xFF2D4810)),
    CountryCurrency("SGD", "Singapore", "Singapore Dollar", "S$", Color(0xFFFFD8CC), Color(0xFF8A2E1A)),
    CountryCurrency("AED", "United Arab Emirates", "UAE Dirham", "د.إ", Color(0xFFD0E2FF), Color(0xFF143E82)),
    CountryCurrency("CHF", "Switzerland", "Swiss Franc", "Fr", Color(0xFFE5DCFF), Color(0xFF452285))
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
    var selectedCurrency by remember { mutableStateOf(SupportedCurrencies[0]) } // Default India - INR
    var isCurrencyDropdownExpanded by remember { mutableStateOf(false) }

    val effectiveSeed = remember(nameText, randomSeedSuffix) {
        if (nameText.isBlank()) "Explorer_$randomSeedSuffix" else "${nameText.trim()}_$randomSeedSuffix"
    }
    val diceBearSvgUrl = remember(effectiveSeed) {
        "https://api.dicebear.com/9.x/open-peeps/svg?seed=${Uri.encode(effectiveSeed)}&backgroundColor=d7e8b6,fed8c8,dce3fd"
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
                Spacer(modifier = Modifier.height(20.dp))

                // Brand Pill / Badge
                Surface(
                    shape = SplitMateThemeTokens.RadiusPill,
                    color = SplitMateThemeTokens.SageSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
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
                            text = "Zero-Signup · Private Local Vault",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitMateThemeTokens.SageText
                        )
                    }
                }

                // Header
                Text(
                    text = "Welcome to SplitMate",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SplitMateThemeTokens.PrimaryDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Let's set up your profile.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = SplitMateThemeTokens.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

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
                                text = "dicebear.svg",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitMateThemeTokens.ScreenBg,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tap to randomize avatar",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SplitMateThemeTokens.TextSecondary,
                    modifier = Modifier.clickable { randomSeedSuffix = (100..999).random() }
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Input 1: Material 3 OutlinedTextField for "Your Name"
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Your Name", fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("e.g. Maya Lin") },
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

                // Input 2: Exposed Dropdown Selector for "Home Country & Currency" with Circular Symbol Badges
                ExposedDropdownMenuBox(
                    expanded = isCurrencyDropdownExpanded,
                    onExpandedChange = { isCurrencyDropdownExpanded = !isCurrencyDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "${selectedCurrency.country} (${selectedCurrency.code} · ${selectedCurrency.symbol})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Home Country & Currency", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(selectedCurrency.badgeBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedCurrency.symbol,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = selectedCurrency.badgeFg
                                )
                            }
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCurrencyDropdownExpanded)
                        },
                        shape = SplitMateThemeTokens.RadiusCard,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SplitMateThemeTokens.SurfaceWhite,
                            unfocusedContainerColor = SplitMateThemeTokens.SurfaceWhite,
                            focusedBorderColor = SplitMateThemeTokens.PrimaryDark,
                            unfocusedBorderColor = SplitMateThemeTokens.BorderLight,
                            focusedLabelColor = SplitMateThemeTokens.PrimaryDark,
                            unfocusedLabelColor = SplitMateThemeTokens.TextSecondary
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .sizeIn(minHeight = 56.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isCurrencyDropdownExpanded,
                        onDismissRequest = { isCurrencyDropdownExpanded = false },
                        modifier = Modifier
                            .background(SplitMateThemeTokens.SurfaceWhite)
                            .clip(SplitMateThemeTokens.RadiusCard)
                    ) {
                        SupportedCurrencies.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(item.badgeBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item.symbol,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = item.badgeFg
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${item.country} (${item.code})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = SplitMateThemeTokens.PrimaryDark
                                            )
                                            Text(
                                                text = "${item.currencyName} · Symbol: ${item.symbol}",
                                                fontSize = 12.sp,
                                                color = SplitMateThemeTokens.TextSecondary
                                            )
                                        }
                                        if (item.code == selectedCurrency.code) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = SplitMateThemeTokens.SageText,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCurrency = item
                                    isCurrencyDropdownExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                                text = "Zero Rounding Drift Engine",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SplitMateThemeTokens.SageText
                            )
                            Text(
                                text = "Integer-cent precision with local Room SQLite storage.",
                                fontSize = 12.sp,
                                color = SplitMateThemeTokens.PrimaryDark
                            )
                        }
                    }
                }
            }

            // CTA: Massive, pill-shaped primary button at bottom (#23201E background, white text)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val finalName = if (nameText.isBlank()) "Explorer" else nameText.trim()
                        onCompleteProfile(finalName, selectedCurrency, effectiveSeed)
                    },
                    shape = SplitMateThemeTokens.RadiusPill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SplitMateThemeTokens.PrimaryDark,
                        contentColor = Color.White
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
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Encrypted On-Device · No External Accounts Needed",
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
    userName: String = "Maya Lin",
    avatarSeed: String = userName,
    upiId: String = "mayalin@okaxis",
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
    var showEditCurrencyDialog by remember { mutableStateOf(false) }

    val screenBg = if (isDarkTheme) Color(0xFF141311) else SplitMateThemeTokens.ScreenBg
    val cardBg = if (isDarkTheme) Color(0xFF1F1D1A) else SplitMateThemeTokens.SurfaceWhite
    val mutedBg = if (isDarkTheme) Color(0xFF282521) else SplitMateThemeTokens.SurfaceMuted
    val textPrimary = if (isDarkTheme) Color(0xFFF6F2EA) else SplitMateThemeTokens.PrimaryDark
    val textSecondary = if (isDarkTheme) Color(0xFFB5ADA3) else SplitMateThemeTokens.TextSecondary
    val borderColor = if (isDarkTheme) Color(0xFF38332D) else SplitMateThemeTokens.BorderLight

    val diceBearSvgUrl = remember(avatarSeed) {
        "https://api.dicebear.com/9.x/open-peeps/svg?seed=${Uri.encode(avatarSeed)}&backgroundColor=d7e8b6,fed8c8,dce3fd"
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
                                text = "v6.0 Online",
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
                                        Text("Balance", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SplitMateThemeTokens.SageText)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(totalBalanceText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = SplitMateThemeTokens.SageText)
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
                                        Text("Active Groups", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("$activeGroupsCount Ledgers", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = textPrimary)
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

            // Section 1: "Payment & Currency"
            item {
                Column {
                    Text(
                        text = "Payment & Currency",
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
                                icon = Icons.Rounded.QrCodeScanner,
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

                            // Row 2: Default Currency (160+ World Currencies ModalBottomSheet)
                            SettingsRowItem(
                                icon = Icons.Rounded.CurrencyExchange,
                                iconBg = SplitMateThemeTokens.TerracottaSurface,
                                iconTint = SplitMateThemeTokens.TerracottaText,
                                title = "Default Ledger Currency (160+ World)",
                                subtitle = defaultCurrencyCode,
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = textSecondary
                                    )
                                },
                                onClick = { showEditCurrencyDialog = true }
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
                                subtitle = if (isDarkTheme) "Dark Espresso mode enabled" else "Warm Cream Eggshell",
                                trailingContent = {
                                    Switch(
                                        checked = isDarkTheme,
                                        onCheckedChange = {
                                            isDarkTheme = it
                                            onThemeToggle(it)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = SplitMateThemeTokens.SurfaceWhite,
                                            checkedTrackColor = SplitMateThemeTokens.AccentSage,
                                            uncheckedThumbColor = SplitMateThemeTokens.PrimaryDark,
                                            uncheckedTrackColor = SplitMateThemeTokens.SurfaceMuted
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
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitMateThemeTokens.SageText
                            )
                            Text(
                                text = "All splits calculated with 0.00¢ remainder parity and held securely in local SQLite.",
                                fontSize = 11.sp,
                                color = SplitMateThemeTokens.PrimaryDark
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
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                    }
                    Text(
                        text = "This will erase all cached receipts, group ledgers, and participant claim records stored on this device. This action cannot be undone.",
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
                            Text("Cancel", fontWeight = FontWeight.Bold, color = textPrimary)
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
                            Text("Clear All Data", fontWeight = FontWeight.Bold, color = Color.White)
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    OutlinedTextField(
                        value = upiInput,
                        onValueChange = { upiInput = it },
                        label = { Text("UPI Virtual Payment Address") },
                        placeholder = { Text("e.g. yourname@okhdfcbank") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditUpiDialog = false }) {
                            Text("Cancel", fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onUpdateUpiId(upiInput)
                                showEditUpiDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SplitMateThemeTokens.PrimaryDark),
                            shape = SplitMateThemeTokens.RadiusButton
                        ) {
                            Text("Save VPA", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // M3 Expressive ModalBottomSheet 3: Select Default Currency from 160+ World Currencies
    if (showEditCurrencyDialog) {
        CurrencyModalBottomSheet(
            currencies = allCurrencies,
            activeCurrencyCode = defaultCurrencyCode.substringBefore(" ").trim(),
            onSelectCurrency = { code ->
                onUpdateCurrencyCode(code)
                showEditCurrencyDialog = false
            },
            onSyncLiveRates = onSyncLiveRates,
            onDismiss = { showEditCurrencyDialog = false }
        )
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDestructive) SplitMateThemeTokens.TerracottaText else SplitMateThemeTokens.PrimaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
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
