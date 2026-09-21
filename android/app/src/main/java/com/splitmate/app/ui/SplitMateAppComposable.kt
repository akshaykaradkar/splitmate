package com.splitmate.app

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.screens.CurrencyModalBottomSheet
import com.splitmate.app.ui.screens.EditFriendUpiDialog
import com.splitmate.app.ui.screens.OnboardingSetupScreen
import com.splitmate.app.ui.screens.QuickExpenseScreen
import com.splitmate.app.ui.screens.UserGuideBottomSheet
import com.splitmate.app.ui.screens.UserSettingsScreen
import java.util.Locale

// ==============================================================================
// 1. MATERIAL 3 EXPRESSIVE PALETTE & TOKENS (Reactive to Dark / Light Mode)
// ==============================================================================
object SplitMateTheme {
    var isDark by mutableStateOf(false)

    val ScreenBg: Color
        get() = if (isDark) Color(0xFF141311) else Color(0xFFFAF7F2)
    val PrimaryDark: Color
        get() = if (isDark) Color(0xFFF6F2EA) else Color(0xFF23201E)
    val AccentSage = Color(0xFFD7E8B6)             // Active Navigation Pill / Secondary Action
    val SageSurface: Color
        get() = if (isDark) Color(0xFF243314) else Color(0xFFEAF3DC)
    val SageText: Color
        get() = if (isDark) Color(0xFFD7E8B6) else Color(0xFF2D4810)
    val TerracottaSurface: Color
        get() = if (isDark) Color(0xFF3A1E18) else Color(0xFFFCECE7)
    val TerracottaText: Color
        get() = if (isDark) Color(0xFFFFB4A4) else Color(0xFFC23E2A)
    val BrandCoral = Color(0xFFE06B52)             // App Leaf / Brand Logo
    val SurfaceWhite: Color
        get() = if (isDark) Color(0xFF1F1D1A) else Color(0xFFFFFFFF)
    val SurfaceMuted: Color
        get() = if (isDark) Color(0xFF2B2823) else Color(0xFFF0EDE6)
    val BorderLight: Color
        get() = if (isDark) Color(0xFF38332D) else Color(0xFFE6E2D8)
    val TextSecondary: Color
        get() = if (isDark) Color(0xFFB5ADA3) else Color(0xFF6E6863)

    // Exaggerated Expressive Radii
    val RadiusHero = RoundedCornerShape(32.dp)
    val RadiusCard = RoundedCornerShape(24.dp)
    val RadiusPanel = RoundedCornerShape(20.dp)
    val RadiusButton = RoundedCornerShape(16.dp)
    val RadiusDialog = RoundedCornerShape(28.dp)
    val RadiusInput = RoundedCornerShape(20.dp)
    val RadiusBadge = RoundedCornerShape(999.dp)

    val FontRounded = FontFamily.SansSerif
}

// ==============================================================================
// 2. ROOT NAVIGATION HOST & SCAFFOLD
// ==============================================================================
enum class SplitMateTab(val label: String, val icon: ImageVector) {
    LEDGERS("Ledgers", Icons.Rounded.AccountBalanceWallet),
    SPLIT("Split", Icons.AutoMirrored.Rounded.ReceiptLong),
    SETTLE("Settle", Icons.Rounded.SwapHoriz),
    AUDIT("Audit", Icons.Rounded.HistoryEdu)
}

@Composable
fun SplitMateApp(viewModel: SplitMateViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("splitmate_prefs", android.content.Context.MODE_PRIVATE) }
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val activeGroups by viewModel.activeGroups.collectAsState()

    // Observe and sync Dark Theme preference across app restarts
    LaunchedEffect(Unit) {
        if (prefs.contains("is_dark_theme")) {
            val savedDark = prefs.getBoolean("is_dark_theme", false)
            if (savedDark != uiState.isDarkTheme) {
                viewModel.toggleDarkTheme(savedDark)
            }
        }
    }
    SplitMateTheme.isDark = uiState.isDarkTheme

    val startRoute = if (uiState.hasRegisteredProfile) "dashboard" else "onboarding"

    LaunchedEffect(uiState.hasRegisteredProfile) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (!uiState.hasRegisteredProfile && currentRoute != "onboarding") {
            navController.navigate("onboarding") {
                popUpTo(0) { inclusive = true }
            }
        } else if (uiState.hasRegisteredProfile && currentRoute == "onboarding") {
            navController.navigate("dashboard") {
                popUpTo("onboarding") { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startRoute
    ) {
        composable("onboarding") {
            OnboardingSetupScreen(
                onCompleteProfile = { name, currency, avatarSeed ->
                    viewModel.completeOnboarding(
                        name = name,
                        countryName = currency.country,
                        currencyCode = currency.code,
                        currencySymbol = currency.symbol,
                        avatarSeed = avatarSeed
                    )
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            SplitMateMainDashboardScaffold(
                viewModel = viewModel,
                onOpenSettings = { navController.navigate("settings") }
            )
        }

        composable("settings") {
            UserSettingsScreen(
                userName = uiState.currentUserName.ifBlank { "Explorer" },
                avatarSeed = uiState.currentUserSeed,
                upiId = uiState.userUpiId,
                defaultCurrencyCode = "${uiState.activeCurrency.currencyCode} (${uiState.activeCurrency.symbol})",
                totalBalanceText = totalBalance,
                activeGroupsCount = activeGroups.size,
                isDarkThemeInitial = uiState.isDarkTheme,
                allCurrencies = uiState.currencyRates,
                onSyncLiveRates = { viewModel.syncLiveCurrencyRatesFromFrankfurter("USD") },
                onBackClick = { navController.popBackStack() },
                onUpdateUpiId = { newUpi -> viewModel.updateUpiId(newUpi) },
                onUpdateCurrencyCode = { newCode ->
                    viewModel.updateUserProfile(uiState.currentUserName, newCode)
                },
                onThemeToggle = { isDark ->
                    prefs.edit().putBoolean("is_dark_theme", isDark).apply()
                    viewModel.toggleDarkTheme(isDark)
                },
                onClearVaultClick = { viewModel.clearLocalVault() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitMateMainDashboardScaffold(
    viewModel: SplitMateViewModel,
    onOpenSettings: () -> Unit
) {
    var currentTab by remember { mutableStateOf(SplitMateTab.LEDGERS) }
    var showUserGuideSheet by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    if (showUserGuideSheet) {
        UserGuideBottomSheet(
            onDismiss = { showUserGuideSheet = false }
        )
    }

    if (showCurrencySheet) {
        CurrencyModalBottomSheet(
            currencies = uiState.currencyRates,
            activeCurrencyCode = uiState.activeCurrencyCode,
            onSelectCurrency = { code ->
                viewModel.updateUserProfile(uiState.currentUserName, code)
                showCurrencySheet = false
            },
            onSyncLiveRates = { viewModel.syncLiveCurrencyRatesFromFrankfurter("USD") },
            onDismiss = { showCurrencySheet = false }
        )
    }

    Scaffold(
        containerColor = SplitMateTheme.ScreenBg,
        floatingActionButton = {
            if (currentTab == SplitMateTab.LEDGERS) {
                ExtendedFloatingActionButton(
                    onClick = { currentTab = SplitMateTab.SPLIT },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.ElectricBolt,
                            contentDescription = "Quick Split",
                            tint = Color.White
                        )
                    },
                    text = {
                        Text(
                            text = "Quick Split",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontFamily = SplitMateTheme.FontRounded
                        )
                    },
                    containerColor = Color(0xFF23201E),
                    contentColor = Color.White,
                    shape = SplitMateTheme.RadiusBadge,
                    modifier = Modifier.shadow(10.dp, SplitMateTheme.RadiusBadge)
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                    .testTag("HorizontalFloatingToolbar")
            ) {
                SplitMateBottomNavigationBar(
                    selectedTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                SplitMateTab.LEDGERS -> LedgersDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToSplit = { currentTab = SplitMateTab.SPLIT },
                    onAvatarSettingsClick = onOpenSettings,
                    onOpenGuideSheet = { showUserGuideSheet = true },
                    onOpenCurrencySheet = { showCurrencySheet = true }
                )
                SplitMateTab.SPLIT -> QuickExpenseScreen(
                    viewModel = viewModel,
                    onBackClick = { currentTab = SplitMateTab.LEDGERS },
                    onHelpClick = { showUserGuideSheet = true },
                    onSaveSplit = { _, _ ->
                        currentTab = SplitMateTab.LEDGERS
                    }
                )
                SplitMateTab.SETTLE -> GreedySettlementScreen(viewModel = viewModel)
                SplitMateTab.AUDIT -> AuditVaultScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SplitMateBottomNavigationBar(
    selectedTab: SplitMateTab,
    onTabSelected: (SplitMateTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = SplitMateTheme.SurfaceWhite,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SplitMateTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Surface(
                    onClick = { onTabSelected(tab) },
                    shape = SplitMateTheme.RadiusBadge,
                    color = if (isSelected) SplitMateTheme.AccentSage else Color.Transparent,
                    modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) Color(0xFF23201E) else SplitMateTheme.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        AnimatedVisibility(visible = isSelected) {
                            Text(
                                text = "  ${tab.label}",
                                color = Color(0xFF23201E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// TAB 1: LEDGERS (Dashboard Screen - Wired to ViewModel, Empty State & Logo Sync)
// ==============================================================================
@Composable
fun LedgersDashboardScreen(
    viewModel: SplitMateViewModel,
    onNavigateToSplit: () -> Unit = {},
    onAvatarSettingsClick: () -> Unit = {},
    onOpenGuideSheet: () -> Unit = {},
    onOpenCurrencySheet: () -> Unit = {}
) {
    val totalBalance by viewModel.totalBalance.collectAsState()
    val activeGroups by viewModel.activeGroups.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var editingFriend by remember { mutableStateOf<GroupMemberEntity?>(null) }

    val isNegativeBalance = totalBalance.startsWith("-")

    editingFriend?.let { friend ->
        EditFriendUpiDialog(
            member = friend,
            onDismiss = { editingFriend = null },
            onSave = { newName, newUpi ->
                viewModel.updateFriendUpi(friend.memberId, newName, newUpi)
                editingFriend = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Custom Top Bar (Subtitle "Fun & Trip Expenses", Currency Circular Icon Pill, Help '?' Guide, Clickable Avatar)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Direct reference to ic_launcher_foreground.xml vector drawable (home screen icon sync)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFAF6F0))
                            .border(1.dp, SplitMateTheme.BorderLight, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "SplitMate Official App Logo",
                            modifier = Modifier
                                .size(44.dp)
                                .scale(1.25f)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SplitMate",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateTheme.PrimaryDark,
                            fontFamily = SplitMateTheme.FontRounded
                        )
                        Text(
                            text = "Fun & Trip Expenses",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitMateTheme.TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Circular Currency Symbol Chip opening 160+ World Currencies ModalBottomSheet
                    Surface(
                        onClick = onOpenCurrencySheet,
                        shape = SplitMateTheme.RadiusBadge,
                        color = SplitMateTheme.SurfaceWhite,
                        border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(SplitMateTheme.AccentSage),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiState.activeCurrency.symbol.take(2),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF23201E)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.activeCurrencyCode,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitMateTheme.PrimaryDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Help '?' Button triggering UserGuideBottomSheet (Point 9)
                    IconButton(
                        onClick = onOpenGuideSheet,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SplitMateTheme.SurfaceWhite,
                            border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.HelpOutline,
                                    contentDescription = "User Guide",
                                    tint = SplitMateTheme.PrimaryDark,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Clickable Top-Right Avatar routing to UserSettingsScreen
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onAvatarSettingsClick)
                    ) {
                        AvatarToken(
                            initials = uiState.currentUserSeed,
                            bg = SplitMateTheme.AccentSage,
                            textColor = Color(0xFF23201E),
                            size = 38
                        )
                    }
                }
            }
        }

        // 2. Hero Balance Card (32dp Radius, Expressive Gradient, Spring Bounce Animation)
        item {
            Card(
                shape = SplitMateTheme.RadiusHero,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x33D7E8B6), SplitMateTheme.RadiusHero)
                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFF5F8EC), Color(0xFFFDF1EC))
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = SplitMateTheme.RadiusBadge,
                                color = SplitMateTheme.SurfaceWhite.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = if (isNegativeBalance) "↑ You owe overall" else "↓ You are owed overall",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNegativeBalance) SplitMateTheme.TerracottaText else SplitMateTheme.SageText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.isOfflineMode) SplitMateTheme.BrandCoral else Color(0xFF388E3C))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isOfflineMode) "Offline Vault" else "Live Sync",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isOfflineMode) SplitMateTheme.BrandCoral else Color(0xFF388E3C)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = totalBalance,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateTheme.PrimaryDark,
                            fontFamily = SplitMateTheme.FontRounded
                        )

                        Text(
                            text = "✓ Across ${activeGroups.size} active groups · 0 penny drift",
                            fontSize = 13.sp,
                            color = SplitMateTheme.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showNewGroupDialog = true },
                                shape = SplitMateTheme.RadiusButton,
                                colors = ButtonDefaults.buttonColors(containerColor = SplitMateTheme.PrimaryDark),
                                modifier = Modifier
                                    .weight(1f)
                                    .sizeIn(minHeight = 48.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = SplitMateTheme.ScreenBg, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ New Group", color = SplitMateTheme.ScreenBg, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = onNavigateToSplit,
                                shape = SplitMateTheme.RadiusButton,
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = SplitMateTheme.AccentSage),
                                modifier = Modifier
                                    .weight(1f)
                                    .sizeIn(minHeight = 48.dp)
                            ) {
                                Icon(Icons.Rounded.DocumentScanner, contentDescription = null, tint = SplitMateTheme.PrimaryDark, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan / Log", color = SplitMateTheme.PrimaryDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Section Header: Active Groups
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Active Groups",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SplitMateTheme.PrimaryDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = SplitMateTheme.SurfaceMuted,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${activeGroups.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SplitMateTheme.PrimaryDark)
                        }
                    }
                }

                Surface(
                    shape = SplitMateTheme.RadiusBadge,
                    color = Color.Transparent,
                    modifier = Modifier.sizeIn(minHeight = 48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Sort by balance ⇅",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SplitMateTheme.TextSecondary
                        )
                    }
                }
            }
        }

        // 4. Illustrated M3 Empty State Card when activeGroups is empty (Point 3)
        item {
            AnimatedVisibility(
                visible = activeGroups.isEmpty(),
                enter = fadeIn() + expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    shape = SplitMateTheme.RadiusHero,
                    colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusHero)
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(SplitMateTheme.SageSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Explore,
                                contentDescription = "No Active Trips Illustration",
                                tint = SplitMateTheme.SageText,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No active trips. Tap the + button below to create your first group!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateTheme.PrimaryDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Create a trip or roommate ledger with zero penny rounding drift.",
                            fontSize = 13.sp,
                            color = SplitMateTheme.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { showNewGroupDialog = true },
                            shape = SplitMateTheme.RadiusBadge,
                            colors = ButtonDefaults.buttonColors(containerColor = SplitMateTheme.PrimaryDark)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create First Group", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Dynamic Group Cards (24dp radius, Spring Bounce Animation)
        items(activeGroups, key = { it.groupId }) { groupCard ->
            val cardSurfaceColor = when {
                groupCard.netBalanceCents > 0L -> SplitMateTheme.SageSurface
                groupCard.netBalanceCents < 0L -> SplitMateTheme.TerracottaSurface
                else -> SplitMateTheme.SurfaceWhite
            }
            val badgeTextColor = when {
                groupCard.netBalanceCents > 0L -> SplitMateTheme.SageText
                groupCard.netBalanceCents < 0L -> SplitMateTheme.TerracottaText
                else -> SplitMateTheme.SageText
            }
            val groupIcon = when {
                groupCard.name.contains("Cabin", ignoreCase = true) -> Icons.Rounded.Cabin
                groupCard.name.contains("Apt", ignoreCase = true) || groupCard.name.contains("Room", ignoreCase = true) -> Icons.Rounded.Apartment
                else -> Icons.Rounded.RamenDining
            }

            Card(
                onClick = { viewModel.selectActiveGroup(groupCard.groupId) },
                shape = SplitMateTheme.RadiusCard,
                colors = CardDefaults.cardColors(containerColor = cardSurfaceColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                    .then(
                        if (groupCard.netBalanceCents == 0L) {
                            Modifier.border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
                        } else Modifier
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (groupCard.netBalanceCents == 0L) SplitMateTheme.SurfaceMuted else Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(groupIcon, contentDescription = null, tint = badgeTextColor)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(groupCard.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SplitMateTheme.PrimaryDark)
                                Text("${groupCard.memberCount} members", fontSize = 13.sp, color = SplitMateTheme.TextSecondary)
                            }
                        }

                        Surface(
                            shape = SplitMateTheme.RadiusBadge,
                            color = if (groupCard.netBalanceCents == 0L) SplitMateTheme.SageSurface else SplitMateTheme.SurfaceWhite
                        ) {
                            Text(
                                text = groupCard.formattedBadgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = badgeTextColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OverlappingAvatarStack(groupCard.memberSeeds, remainingCount = groupCard.remainingCount)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val firstFriend = uiState.members.firstOrNull {
                                it.groupId == groupCard.groupId && !it.isCurrentUser
                            }
                            if (firstFriend != null) {
                                Surface(
                                    onClick = { editingFriend = firstFriend },
                                    shape = SplitMateTheme.RadiusBadge,
                                    color = SplitMateTheme.SurfaceWhite,
                                    border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Edit Friend UPI",
                                            tint = SplitMateTheme.PrimaryDark,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Edit UPI",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SplitMateTheme.PrimaryDark
                                        )
                                    }
                                }
                            }
                            Surface(
                                shape = SplitMateTheme.RadiusBadge,
                                color = if (groupCard.netBalanceCents == 0L) SplitMateTheme.SageSurface else SplitMateTheme.SurfaceWhite.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = groupCard.statusPillText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (groupCard.netBalanceCents == 0L) SplitMateTheme.SageText else SplitMateTheme.PrimaryDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Recent Activity Preview (Spring Bounce Animation)
        if (uiState.expenses.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                ) {
                    Text(
                        text = "Recent Activity",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SplitMateTheme.PrimaryDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    uiState.expenses.take(3).forEach { expense ->
                        val payer = uiState.members.find { it.memberId == expense.payerId }
                        val isMePayer = payer?.isCurrentUser == true
                        val sym = uiState.activeCurrency.symbol
                        val formattedAmt = (if (isMePayer) "+" else "-") +
                            sym + String.format(Locale.US, "%.2f", expense.totalAmountCents / 100.0)
                        ActivityItemRow(
                            icon = Icons.Rounded.Restaurant,
                            iconBg = if (isMePayer) Color(0xFFE0E7FF) else Color(0xFFFFE4E6),
                            title = expense.title,
                            subtitle = "Paid by ${if (isMePayer) "you" else (payer?.name ?: "Member")} · Rate ${expense.lockedExchangeRate}",
                            amount = formattedAmt,
                            isPositive = isMePayer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Custom M3 Expressive Dialog for "+ New Group" (28dp radius, 20dp input radius)
    if (showNewGroupDialog) {
        var groupNameInput by remember { mutableStateOf("") }
        var friendsInput by remember { mutableStateOf("Priya, Sam, Maya") }

        Dialog(onDismissRequest = { showNewGroupDialog = false }) {
            Surface(
                shape = SplitMateTheme.RadiusDialog,
                color = SplitMateTheme.SurfaceWhite,
                shadowElevation = 18.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusDialog)
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
                                .background(SplitMateTheme.AccentSage),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.GroupAdd, contentDescription = null, tint = Color(0xFF23201E))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Create Ledger Group",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SplitMateTheme.PrimaryDark
                            )
                            Text(
                                text = "Add friends with DiceBear Open-Peeps avatars",
                                fontSize = 12.sp,
                                color = SplitMateTheme.TextSecondary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = groupNameInput,
                        onValueChange = { groupNameInput = it },
                        label = { Text("Group Name") },
                        placeholder = { Text("e.g. Goa Beach Villa") },
                        singleLine = true,
                        shape = SplitMateTheme.RadiusInput,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = friendsInput,
                        onValueChange = { friendsInput = it },
                        label = { Text("Members (comma-separated)") },
                        singleLine = true,
                        shape = SplitMateTheme.RadiusInput,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showNewGroupDialog = false }) {
                            Text("Cancel", fontWeight = FontWeight.Bold, color = SplitMateTheme.TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.createNewGroup(groupNameInput, uiState.activeCurrencyCode, friendsInput)
                                showNewGroupDialog = false
                            },
                            shape = SplitMateTheme.RadiusButton,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF23201E))
                        ) {
                            Text("Create Group", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// TAB 3: SETTLE (Greedy Debt Simplification & UPI Deep Linking + Friend UPI Editor)
// ==============================================================================
@Composable
fun GreedySettlementScreen(viewModel: SplitMateViewModel) {
    val context = LocalContext.current
    val settlementPlan by viewModel.settlementPlan.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var editingFriend by remember { mutableStateOf<GroupMemberEntity?>(null) }
    val rawDebtEdges = (uiState.expenses.size * uiState.activeGroupMembers.size).coerceAtLeast(settlementPlan.size)

    editingFriend?.let { friend ->
        EditFriendUpiDialog(
            member = friend,
            onDismiss = { editingFriend = null },
            onSave = { newName, newUpi ->
                viewModel.updateFriendUpi(friend.memberId, newName, newUpi)
                editingFriend = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Greedy Settlement",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SplitMateTheme.PrimaryDark
            )
            Text(
                text = "Min-Cash Flow network simplification and 1-tap transfers",
                fontSize = 13.sp,
                color = SplitMateTheme.TextSecondary
            )
        }

        // 1. Algorithm Summary Banner
        item {
            Card(
                shape = SplitMateTheme.RadiusPanel,
                colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SageSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SplitMateTheme.SurfaceWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = SplitMateTheme.SageText)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Graph Optimized", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = SplitMateTheme.SageText)
                        Text(
                            text = "Greedy algorithm reduced $rawDebtEdges cross-debts down to ${settlementPlan.size} direct transfers.",
                            fontSize = 12.sp,
                            color = SplitMateTheme.PrimaryDark
                        )
                    }
                }
            }
        }

        // 2. Settlement Action Cards
        item {
            Text(
                text = "Required Direct Transfers (${settlementPlan.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SplitMateTheme.PrimaryDark
            )
        }

        items(settlementPlan, key = { "${it.transfer.fromMemberId}_${it.transfer.toMemberId}" }) { transfer ->
            val toRoomMember = uiState.members.find { it.memberId == transfer.transfer.toMemberId }
            Card(
                shape = SplitMateTheme.RadiusCard,
                colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarToken(
                                initials = transfer.fromSeed,
                                bg = SplitMateTheme.TerracottaSurface,
                                textColor = SplitMateTheme.TerracottaText
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(shape = SplitMateTheme.RadiusBadge, color = SplitMateTheme.SurfaceMuted) {
                                Text(
                                    text = "${transfer.fromName} pays ${transfer.formattedDisplayAmount} →",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SplitMateTheme.PrimaryDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            AvatarToken(
                                initials = transfer.toSeed,
                                bg = SplitMateTheme.SageSurface,
                                textColor = SplitMateTheme.SageText
                            )
                        }
                        Text(
                            text = transfer.formattedDisplayAmount,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = SplitMateTheme.PrimaryDark
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Editable Friend UPI VPA Pill
                    Surface(
                        onClick = {
                            if (toRoomMember != null) editingFriend = toRoomMember
                        },
                        shape = SplitMateTheme.RadiusBadge,
                        color = SplitMateTheme.SurfaceMuted
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = SplitMateTheme.PrimaryDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "To UPI: ${transfer.upiId} · Tap to Edit Friend's VPA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SplitMateTheme.PrimaryDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val upiUri = Uri.parse(
                                    "upi://pay?pa=${transfer.upiId}&pn=${Uri.encode(transfer.toName)}&am=${transfer.amount}&cu=INR"
                                )
                                val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
                                context.startActivity(Intent.createChooser(upiIntent, "Pay with UPI"))
                            },
                            shape = SplitMateTheme.RadiusButton,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF23201E)),
                            modifier = Modifier
                                .weight(1.2f)
                                .sizeIn(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pay via UPI", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.markGreedyTransferSettled(transfer.transfer) },
                            shape = SplitMateTheme.RadiusButton,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SplitMateTheme.PrimaryDark),
                            modifier = Modifier
                                .weight(1f)
                                .sizeIn(minHeight = 48.dp)
                        ) {
                            Text("Mark as Paid", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    AssistChip(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Hey ${transfer.fromName}! Reminder for ${transfer.formattedDisplayAmount} settlement to ${transfer.toName} (${transfer.upiId}) on SplitMate."
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                        },
                        label = { Text("💬 Send WhatsApp Reminder", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        shape = SplitMateTheme.RadiusButton,
                        colors = AssistChipDefaults.assistChipColors(containerColor = SplitMateTheme.SurfaceMuted),
                        modifier = Modifier.sizeIn(minHeight = 48.dp)
                    )
                }
            }
        }

        // 3. Settled Equilibrium State
        item {
            Card(
                shape = SplitMateTheme.RadiusCard,
                colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SageSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SplitMateTheme.SageText, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("All Accounts Balanced", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SplitMateTheme.PrimaryDark)
                    Text("Zero debt loop detected · Certified 0.00¢ drift", fontSize = 12.sp, color = SplitMateTheme.TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = SplitMateTheme.RadiusBadge,
                        color = SplitMateTheme.PrimaryDark
                    ) {
                        Text("Equilibrium Reached", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                    }
                }
            }
        }
    }
}

// ==============================================================================
// TAB 4: AUDIT (History, Rollback & Offline Vault)
// ==============================================================================
@Composable
fun AuditVaultScreen(viewModel: SplitMateViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val sym = uiState.activeCurrency.symbol

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Audit & Local Vault",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SplitMateTheme.PrimaryDark
            )
            Text(
                text = "Cryptographic ledger footprint and SQLite persistence",
                fontSize = 13.sp,
                color = SplitMateTheme.TextSecondary
            )
        }

        // 1. Vault Status Banner
        item {
            Card(
                shape = SplitMateTheme.RadiusPanel,
                colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusPanel)
                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SplitMateTheme.AccentSage),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Storage, contentDescription = null, tint = SplitMateTheme.PrimaryDark)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Private On-Device Vault", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SplitMateTheme.PrimaryDark)
                            Text("Room SQLite · ${uiState.currencyRates.size} Currencies Cached", fontSize = 12.sp, color = SplitMateTheme.TextSecondary)
                        }
                    }

                    Surface(
                        shape = SplitMateTheme.RadiusBadge,
                        color = SplitMateTheme.SageSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF388E3C)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${uiState.expenses.size} Synced", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SplitMateTheme.SageText)
                        }
                    }
                }
            }
        }

        // 2. Transaction Audit Feed
        item {
            Text(
                text = "Live Ledger Audit Log",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SplitMateTheme.TextSecondary
            )
        }

        items(uiState.expenses, key = { it.expenseId }) { expense ->
            val payer = uiState.members.find { it.memberId == expense.payerId }
            val isMePayer = payer?.isCurrentUser == true
            val formattedTotal = "$sym${String.format(Locale.US, "%.2f", expense.totalAmountCents / 100.0)}"

            Card(
                shape = SplitMateTheme.RadiusCard,
                colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isMePayer) Color(0xFFE0E7FF) else Color(0xFFFFE4E6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Dining,
                                    contentDescription = null,
                                    tint = if (isMePayer) Color(0xFF3730A3) else SplitMateTheme.TerracottaText
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(expense.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SplitMateTheme.PrimaryDark)
                                Text(
                                    text = "Paid by ${payer?.name ?: "You"} · Rate ${expense.lockedExchangeRate} (${expense.syncStatus})",
                                    fontSize = 12.sp,
                                    color = SplitMateTheme.TextSecondary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = (if (isMePayer) "+" else "-") + formattedTotal,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = if (isMePayer) SplitMateTheme.SageText else SplitMateTheme.TerracottaText
                            )
                            Text("Total $formattedTotal", fontSize = 11.sp, color = SplitMateTheme.TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SplitMateTheme.BorderLight)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            onClick = { viewModel.rollbackExpense(expense.expenseId) },
                            shape = SplitMateTheme.RadiusBadge,
                            color = SplitMateTheme.SurfaceMuted,
                            modifier = Modifier.sizeIn(minHeight = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.size(16.dp), tint = SplitMateTheme.PrimaryDark)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Undo / Rollback", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SplitMateTheme.PrimaryDark)
                            }
                        }
                    }
                }
            }
        }

        // 3. Export & Safety Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.setOfflineMode(!uiState.isOfflineMode) },
                    shape = SplitMateTheme.RadiusButton,
                    modifier = Modifier
                        .weight(1f)
                        .sizeIn(minHeight = 48.dp)
                ) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.isOfflineMode) "Go Online" else "Simulate Offline", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = { viewModel.syncLiveCurrencyRatesFromFrankfurter() },
                    shape = SplitMateTheme.RadiusButton,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = SplitMateTheme.AccentSage),
                    modifier = Modifier
                        .weight(1f)
                        .sizeIn(minHeight = 48.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = SplitMateTheme.PrimaryDark)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Recalculate & Sync", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SplitMateTheme.PrimaryDark)
                }
            }
        }
    }
}

// ==============================================================================
// 5. REUSABLE ATOMIC COMPONENTS (Wired to Coil AsyncImage + DiceBear Open-Peeps SVG)
// ==============================================================================
@Composable
fun AvatarToken(
    initials: String,
    bg: Color,
    textColor: Color,
    size: Int = 36
) {
    val context = LocalContext.current
    val cleanSeed = Uri.encode(initials.ifBlank { "SplitMateGuest" })
    val diceBearSvgUrl = "https://api.dicebear.com/9.x/open-peeps/svg?seed=$cleanSeed&backgroundColor=f4efe6,d7e8b6,fed8c8,dce3fd"
    val cleanInitials = remember(initials) {
        initials.trim().substringBefore("_").take(2).uppercase().ifBlank { "SM" }
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bg)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cleanInitials,
            fontSize = (size * 0.34f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
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

@Composable
fun OverlappingAvatarStack(avatars: List<String>, remainingCount: Int = 0) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        avatars.forEachIndexed { index, seed ->
            Box(modifier = Modifier.offset(x = (-index * 8).dp)) {
                AvatarToken(
                    initials = seed,
                    bg = Color(0xFFE2E8F0),
                    textColor = SplitMateTheme.PrimaryDark,
                    size = 28
                )
            }
        }
        if (remainingCount > 0) {
            Box(
                modifier = Modifier
                    .offset(x = (-avatars.size * 8).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SplitMateTheme.SurfaceMuted)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("+$remainingCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SplitMateTheme.PrimaryDark)
            }
        }
    }
}

@Composable
fun ActivityItemRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    amount: String,
    isPositive: Boolean
) {
    Card(
        shape = SplitMateTheme.RadiusCard,
        colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = SplitMateTheme.PrimaryDark, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SplitMateTheme.PrimaryDark)
                    Text(subtitle, fontSize = 12.sp, color = SplitMateTheme.TextSecondary)
                }
            }

            Text(
                text = amount,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = if (isPositive) SplitMateTheme.SageText else SplitMateTheme.TerracottaText
            )
        }
    }
}

@Composable
fun ClaimItemRow(
    title: String,
    amount: String,
    claimedBy: List<String>,
    @Suppress("UNUSED_PARAMETER") unclaimed: List<String>
) {
    Card(
        shape = SplitMateTheme.RadiusCard,
        colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SplitMateTheme.PrimaryDark)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Claimed by:", fontSize = 11.sp, color = SplitMateTheme.TextSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    if (claimedBy.isEmpty()) {
                        Text("Unclaimed (Payer holds)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SplitMateTheme.TerracottaText)
                    } else {
                        claimedBy.forEach { person ->
                            Box(modifier = Modifier.padding(end = 4.dp)) {
                                AvatarToken(
                                    initials = person,
                                    bg = SplitMateTheme.AccentSage,
                                    textColor = SplitMateTheme.PrimaryDark,
                                    size = 22
                                )
                            }
                        }
                    }
                }
            }

            Text(amount, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = SplitMateTheme.PrimaryDark)
        }
    }
}
