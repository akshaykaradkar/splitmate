package com.splitmate.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.GroupCategoryIcons
import com.splitmate.app.ui.NewGroupMemberDraft
import com.splitmate.app.ui.SplitMateBrandFontFamily
import com.splitmate.app.ui.SplitMateDisplayFontFamily
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.buildDiceBearOpenPeepsUrl
import com.splitmate.app.ui.extractInitialsFromNameOrSeed
import com.splitmate.app.ui.extractPhoneAndNameFromContactUri
import com.splitmate.app.ui.resolveGroupCategoryIcon
import com.splitmate.app.ui.screens.EditFriendUpiDialog
import com.splitmate.app.ui.screens.OnboardingSetupScreen
import com.splitmate.app.ui.screens.QuickExpenseScreen
import com.splitmate.app.ui.screens.UserSettingsScreen
import java.util.Locale

// ==============================================================================
// 1. MATERIAL 3 EXPRESSIVE PALETTE & TOKENS (GM3 Dark Elevation & DesignSystemBindings)
// ==============================================================================
object SplitMateTheme {
    var isDark by mutableStateOf(false)

    val ScreenBg: Color
        get() = if (isDark) DesignSystemBindings.GM3DarkBackground else DesignSystemBindings.GM3LightBackground
    val PrimaryDark: Color
        get() = if (isDark) DesignSystemBindings.GM3DarkPrimaryText else DesignSystemBindings.GM3LightPrimaryText
    val AccentSage = DesignSystemBindings.ElementsPositiveContainer
    val SageSurface: Color
        get() = if (isDark) Color(0xFF243314) else Color(0xFFEAF3DC)
    val SageText: Color
        get() = if (isDark) Color(0xFFD7E8B6) else DesignSystemBindings.ElementsPositiveText
    val TerracottaSurface: Color
        get() = if (isDark) Color(0xFF3A1E18) else Color(0xFFFCECE7)
    val TerracottaText: Color
        get() = if (isDark) Color(0xFFFFB4A4) else DesignSystemBindings.ElementsNegativeText
    val BrandCoral = Color(0xFFE06B52)
    val SurfaceWhite: Color
        get() = if (isDark) DesignSystemBindings.GM3DarkCardSurface else DesignSystemBindings.GM3LightCardSurface
    val SurfaceMuted: Color
        get() = if (isDark) DesignSystemBindings.GM3DarkKeypadSurface else DesignSystemBindings.GM3LightKeypadSurface
    val BorderLight: Color
        get() = if (isDark) Color(0xFF333333) else Color(0xFFE6E2D8)
    val TextSecondary: Color
        get() = if (isDark) DesignSystemBindings.GM3DarkSubtitleText else DesignSystemBindings.GM3LightSubtitleText

    val RadiusHero = DesignSystemBindings.GM3ShapeExtraLarge
    val RadiusCard = DesignSystemBindings.GM3ShapeLarge
    val RadiusPanel = RoundedCornerShape(20.dp)
    val RadiusButton = RoundedCornerShape(16.dp)
    val RadiusDialog = RoundedCornerShape(28.dp)
    val RadiusInput = RoundedCornerShape(20.dp)
    val RadiusBadge = DesignSystemBindings.GM3ShapePill

    val FontRounded = SplitMateBrandFontFamily
    val FontDisplay = SplitMateDisplayFontFamily
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
                        currencyCode = "INR",
                        currencySymbol = "₹",
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
                defaultCurrencyCode = "INR (₹)",
                totalBalanceText = totalBalance,
                activeGroupsCount = activeGroups.size,
                isDarkThemeInitial = uiState.isDarkTheme,
                allCurrencies = uiState.currencyRates,
                onSyncLiveRates = {},
                onBackClick = { navController.popBackStack() },
                onUpdateUpiId = { newUpi -> viewModel.updateUpiId(newUpi) },
                onUpdateCurrencyCode = {},
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
    val animatedScreenBg by animateColorAsState(
        targetValue = SplitMateTheme.ScreenBg,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "DashboardScreenBg"
    )
    val animatedPrimaryDark by animateColorAsState(
        targetValue = SplitMateTheme.PrimaryDark,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "DashboardPrimaryDark"
    )

    Scaffold(
        containerColor = animatedScreenBg,
        floatingActionButton = {
            if (currentTab == SplitMateTab.LEDGERS) {
                ExtendedFloatingActionButton(
                    onClick = { currentTab = SplitMateTab.SPLIT },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.ElectricBolt,
                            contentDescription = "Log Expense",
                            tint = animatedScreenBg
                        )
                    },
                    text = {
                        Text(
                            text = "Log Expense",
                            fontWeight = FontWeight.ExtraBold,
                            color = animatedScreenBg,
                            fontFamily = SplitMateTheme.FontRounded
                        )
                    },
                    containerColor = animatedPrimaryDark,
                    contentColor = animatedScreenBg,
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
                    onAvatarSettingsClick = onOpenSettings
                )
                SplitMateTab.SPLIT -> QuickExpenseScreen(
                    viewModel = viewModel,
                    onBackClick = { currentTab = SplitMateTab.LEDGERS },
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
    val navSurfaceColor by animateColorAsState(
        targetValue = SplitMateTheme.SurfaceWhite,
        animationSpec = DesignSystemBindings.themeColorTween(),
        label = "BottomNavSurface"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = navSurfaceColor,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
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
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
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
                                fontFamily = SplitMateTheme.FontRounded,
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
    onAvatarSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
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
            onSave = { newName, newUpi, newAvatarSeed ->
                viewModel.updateFriendUpi(friend.memberId, newName, newUpi, newAvatarSeed)
                editingFriend = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(DesignSystemBindings.PixelSectionSpacing)
    ) {
        // 1. Custom Top Bar (Subtitle "Fun & Trip Expenses", Static ₹ INR Badge, Clickable Avatar)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SplitMateTheme.SurfaceWhite)
                            .border(1.dp, SplitMateTheme.BorderLight, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "SplitMate Official App Logo",
                            modifier = Modifier
                                .size(42.dp)
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
                            fontFamily = SplitMateTheme.FontDisplay
                        )
                        Text(
                            text = "Fun & Trip Expenses",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitMateTheme.TextSecondary,
                            fontFamily = SplitMateTheme.FontRounded
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = SplitMateTheme.RadiusBadge,
                        color = SplitMateTheme.SurfaceWhite,
                        border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(SplitMateTheme.AccentSage),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "₹",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF23201E)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "INR",
                                fontFamily = SplitMateTheme.FontRounded,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitMateTheme.PrimaryDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

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

        // 2. Hero Balance Card (32dp Radius, Dark-Mode Adaptive Gradient, Compact 16.dp Padding)
        item {
            val heroGradientColors = if (SplitMateTheme.isDark) {
                listOf(Color(0xFF1D2416), Color(0xFF261C19))
            } else {
                listOf(Color(0xFFF5F8EC), Color(0xFFFDF1EC))
            }
            Card(
                shape = SplitMateTheme.RadiusHero,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusHero)
                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
            ) {
                Box(
                    modifier = Modifier
                        .background(Brush.linearGradient(colors = heroGradientColors))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = SplitMateTheme.RadiusBadge,
                                color = SplitMateTheme.SurfaceWhite.copy(alpha = 0.85f)
                            ) {
                                Text(
                                    text = if (isNegativeBalance) "↑ You owe overall" else "↓ You are owed overall",
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNegativeBalance) SplitMateTheme.TerracottaText else SplitMateTheme.SageText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
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
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isOfflineMode) SplitMateTheme.BrandCoral else Color(0xFF388E3C)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = totalBalance,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateTheme.PrimaryDark,
                            fontFamily = SplitMateTheme.FontDisplay
                        )

                        Text(
                            text = "✓ Across ${activeGroups.size} active groups · Exact Split",
                            fontFamily = SplitMateTheme.FontRounded,
                            fontSize = 13.sp,
                            color = SplitMateTheme.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showNewGroupDialog = true },
                                shape = SplitMateTheme.RadiusButton,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SplitMateTheme.PrimaryDark,
                                    contentColor = SplitMateTheme.ScreenBg
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .sizeIn(minHeight = 46.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = SplitMateTheme.ScreenBg, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ New Group", fontFamily = SplitMateTheme.FontRounded, color = SplitMateTheme.ScreenBg, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = onNavigateToSplit,
                                shape = SplitMateTheme.RadiusButton,
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = SplitMateTheme.AccentSage),
                                modifier = Modifier
                                    .weight(1f)
                                    .sizeIn(minHeight = 46.dp)
                            ) {
                                Icon(Icons.Rounded.ReceiptLong, contentDescription = null, tint = Color(0xFF23201E), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log Expense", fontFamily = SplitMateTheme.FontRounded, color = Color(0xFF23201E), fontWeight = FontWeight.Bold)
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
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Active Groups",
                        fontFamily = SplitMateTheme.FontDisplay,
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
                            Text("${activeGroups.size}", fontFamily = SplitMateTheme.FontRounded, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SplitMateTheme.PrimaryDark)
                        }
                    }
                }

                Text(
                    text = "Sort by balance ⇅",
                    fontFamily = SplitMateTheme.FontRounded,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SplitMateTheme.TextSecondary
                )
            }
        }

        // 4. Illustrated M3 Empty State Card when activeGroups is empty
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
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SplitMateTheme.SageSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Explore,
                                contentDescription = "No Active Trips Illustration",
                                tint = SplitMateTheme.SageText,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No active trips. Tap + New Group to create your first ledger!",
                            fontFamily = SplitMateTheme.FontDisplay,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateTheme.PrimaryDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pick members directly from Android Contacts with automatic UPI routing.",
                            fontFamily = SplitMateTheme.FontRounded,
                            fontSize = 12.sp,
                            color = SplitMateTheme.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showNewGroupDialog = true },
                            shape = SplitMateTheme.RadiusBadge,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SplitMateTheme.PrimaryDark,
                                contentColor = SplitMateTheme.ScreenBg
                            )
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = SplitMateTheme.ScreenBg, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create First Group", fontFamily = SplitMateTheme.FontRounded, color = SplitMateTheme.ScreenBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Dynamic Group Cards (24dp radius, 8-Icon Category support, 14dp compact padding)
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
            val groupIcon = resolveGroupCategoryIcon(groupCard.iconName, groupCard.name)

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
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (groupCard.netBalanceCents == 0L) SplitMateTheme.SurfaceMuted else Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(groupIcon, contentDescription = null, tint = badgeTextColor)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(groupCard.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SplitMateTheme.PrimaryDark)
                                Text("${groupCard.memberCount} members", fontSize = 12.sp, color = SplitMateTheme.TextSecondary)
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

                    Spacer(modifier = Modifier.height(12.dp))

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
                                            imageVector = Icons.Rounded.ContactPhone,
                                            contentDescription = "Link Friend Contact",
                                            tint = SplitMateTheme.PrimaryDark,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Contacts",
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

        // 6. Recent Activity Preview
        if (uiState.expenses.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                ) {
                    Text(
                        text = "Recent Activity",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SplitMateTheme.PrimaryDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                            subtitle = "Paid by ${if (isMePayer) "you" else (payer?.name ?: "Member")} · Exact Split",
                            amount = formattedAmt,
                            isPositive = isMePayer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // =========================================================================
    // CREATE GROUP DIALOG (Point 1: Contact Multi-Selector + Point 3: 8-Icon Grid)
    // Zero Comma-Separated Input!
    // =========================================================================
    if (showNewGroupDialog) {
        var groupNameInput by remember { mutableStateOf("") }
        var selectedIconKey by remember { mutableStateOf("Flight") }
        var selectedMembers by remember { mutableStateOf(listOf<NewGroupMemberDraft>()) }
        var customPeerNameInput by remember { mutableStateOf("") }

        val contactPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickContact()
        ) { contactUri: Uri? ->
            if (contactUri != null) {
                val extracted = extractPhoneAndNameFromContactUri(context, contactUri)
                if (extracted != null) {
                    val (contactName, cleanPhone) = extracted
                    val resolvedName = contactName.trim().ifEmpty { "Friend" }
                    if (selectedMembers.none { it.name.equals(resolvedName, ignoreCase = true) }) {
                        selectedMembers = selectedMembers + NewGroupMemberDraft(
                            name = resolvedName,
                            cleanPhone = cleanPhone
                        )
                    }
                } else {
                    Toast.makeText(context, "Unable to read contact details", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val contactsPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted: Boolean ->
            if (granted) {
                contactPickerLauncher.launch(null)
            } else {
                Toast.makeText(context, "Contacts permission allows 1-tap UPI linking", Toast.LENGTH_SHORT).show()
            }
        }

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
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SplitMateTheme.AccentSage),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = resolveGroupCategoryIcon(selectedIconKey, groupNameInput),
                                contentDescription = null,
                                tint = Color(0xFF23201E)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Create Ledger Group",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SplitMateTheme.PrimaryDark
                            )
                            Text(
                                text = "Select icon & add members from Contacts",
                                fontSize = 12.sp,
                                color = SplitMateTheme.TextSecondary
                            )
                        }
                    }

                    // 1. Group Name Field
                    OutlinedTextField(
                        value = groupNameInput,
                        onValueChange = { groupNameInput = it },
                        label = { Text("Group Name") },
                        placeholder = { Text("e.g. Goa Beach Villa") },
                        singleLine = true,
                        shape = SplitMateTheme.RadiusInput,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SplitMateTheme.PrimaryDark,
                            unfocusedTextColor = SplitMateTheme.PrimaryDark,
                            focusedBorderColor = SplitMateTheme.PrimaryDark,
                            unfocusedBorderColor = SplitMateTheme.BorderLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2. Group Category Icon Picker Grid (2x4 Expressive Material 3 Icons)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Category Icon",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitMateTheme.PrimaryDark
                        )
                        GroupCategoryIcons.chunked(4).forEach { rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowIcons.forEach { option ->
                                    val isSelected = selectedIconKey == option.key
                                    val iconScale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.08f else 1.0f,
                                        animationSpec = DesignSystemBindings.tactileSpring(),
                                        label = "GroupIconBounce_${option.key}"
                                    )
                                    Surface(
                                        onClick = { selectedIconKey = option.key },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) SplitMateTheme.AccentSage else SplitMateTheme.SurfaceMuted,
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF416913) else SplitMateTheme.BorderLight
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .scale(iconScale)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = option.icon,
                                                contentDescription = option.label,
                                                tint = if (isSelected) Color(0xFF23201E) else SplitMateTheme.PrimaryDark,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = option.label,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (isSelected) Color(0xFF23201E) else SplitMateTheme.TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Prominent "+ Add Members from Contacts" Button
                    Button(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                contactPickerLauncher.launch(null)
                            } else {
                                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                        shape = SplitMateTheme.RadiusButton,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SplitMateTheme.AccentSage,
                            contentColor = Color(0xFF23201E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Contacts,
                            contentDescription = null,
                            tint = Color(0xFF23201E),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+ Add Members from Contacts",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color(0xFF23201E)
                        )
                    }

                    // 4. Horizontal Scrollable Selected Member Chips (Initials + Name + 'X' to Remove)
                    if (selectedMembers.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(selectedMembers, key = { it.name + it.cleanPhone }) { member ->
                                Surface(
                                    shape = SplitMateTheme.RadiusBadge,
                                    color = SplitMateTheme.SurfaceMuted,
                                    border = BorderStroke(1.dp, SplitMateTheme.BorderLight)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 6.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
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
                                                text = extractInitialsFromNameOrSeed(member.name),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF23201E)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = member.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SplitMateTheme.PrimaryDark
                                            )
                                            if (member.cleanPhone.isNotEmpty()) {
                                                Text(
                                                    text = "${member.cleanPhone}@upi",
                                                    fontSize = 9.sp,
                                                    color = SplitMateTheme.SageText,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                selectedMembers = selectedMembers.filterNot {
                                                    it.name == member.name && it.cleanPhone == member.cleanPhone
                                                }
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove ${member.name}",
                                                tint = SplitMateTheme.TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Quick "+ Add Custom Name" Row for Peers Without Phone Contacts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customPeerNameInput,
                            onValueChange = { customPeerNameInput = it },
                            placeholder = { Text("+ Add Custom Name", fontSize = 13.sp) },
                            singleLine = true,
                            shape = SplitMateTheme.RadiusInput,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SplitMateTheme.PrimaryDark,
                                unfocusedTextColor = SplitMateTheme.PrimaryDark,
                                focusedBorderColor = SplitMateTheme.PrimaryDark,
                                unfocusedBorderColor = SplitMateTheme.BorderLight
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = {
                                val cleanCustom = customPeerNameInput.trim()
                                if (cleanCustom.isNotEmpty()) {
                                    selectedMembers = selectedMembers + NewGroupMemberDraft(name = cleanCustom)
                                    customPeerNameInput = ""
                                }
                            },
                            enabled = customPeerNameInput.isNotBlank(),
                            shape = SplitMateTheme.RadiusButton,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SplitMateTheme.SurfaceMuted,
                                contentColor = SplitMateTheme.PrimaryDark
                            ),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = "Add Peer", modifier = Modifier.size(18.dp))
                        }
                    }

                    // 6. Action Buttons
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
                                val finalDrafts = if (customPeerNameInput.isNotBlank()) {
                                    selectedMembers + NewGroupMemberDraft(name = customPeerNameInput.trim())
                                } else {
                                    selectedMembers
                                }
                                viewModel.createNewGroupWithContacts(
                                    name = groupNameInput,
                                    iconName = selectedIconKey,
                                    memberDrafts = finalDrafts
                                )
                                showNewGroupDialog = false
                            },
                            shape = SplitMateTheme.RadiusButton,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SplitMateTheme.PrimaryDark,
                                contentColor = SplitMateTheme.ScreenBg
                            )
                        ) {
                            Text("Create Group", color = SplitMateTheme.ScreenBg, fontWeight = FontWeight.Bold)
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
    var targetMemberForContactLink by remember { mutableStateOf<GroupMemberEntity?>(null) }
    val rawDebtEdges = (uiState.expenses.size * uiState.activeGroupMembers.size).coerceAtLeast(settlementPlan.size)

    val directContactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        val memberToUpdate = targetMemberForContactLink
        if (contactUri != null && memberToUpdate != null) {
            val extracted = extractPhoneAndNameFromContactUri(context, contactUri)
            if (extracted != null && extracted.second.isNotBlank()) {
                val cleanPhone = extracted.second
                viewModel.updateFriendUpi(
                    memberId = memberToUpdate.memberId,
                    newName = memberToUpdate.name,
                    newUpiId = "$cleanPhone@upi",
                    newAvatarSeed = memberToUpdate.avatarSeed
                )
            } else {
                Toast.makeText(context, "Could not read phone number from selected contact", Toast.LENGTH_SHORT).show()
            }
        }
        targetMemberForContactLink = null
    }

    val directContactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted && targetMemberForContactLink != null) {
            directContactPickerLauncher.launch(null)
        } else {
            Toast.makeText(context, "Contacts permission required to link phone for UPI", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchContactPickerForMember(member: GroupMemberEntity?) {
        if (member == null) return
        targetMemberForContactLink = member
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            directContactPickerLauncher.launch(null)
        } else {
            directContactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    editingFriend?.let { friend ->
        EditFriendUpiDialog(
            member = friend,
            onDismiss = { editingFriend = null },
            onSave = { newName, newUpi, newAvatarSeed ->
                viewModel.updateFriendUpi(friend.memberId, newName, newUpi, newAvatarSeed)
                editingFriend = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(DesignSystemBindings.PixelSectionSpacing)
    ) {
        item {
            Text(
                text = "Greedy Settlement",
                fontFamily = SplitMateTheme.FontDisplay,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SplitMateTheme.PrimaryDark
            )
            Text(
                text = "Min-Cash Flow network simplification & Phone-Linked UPI",
                fontFamily = SplitMateTheme.FontRounded,
                fontSize = 12.sp,
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
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
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
                fontFamily = SplitMateTheme.FontDisplay,
                fontSize = 15.sp,
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
                Column(modifier = Modifier.padding(14.dp)) {
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
                            fontFamily = SplitMateTheme.FontDisplay,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = SplitMateTheme.PrimaryDark
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Phone-Linked Status Chip or "+ Link Contact" Chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { launchContactPickerForMember(toRoomMember) },
                            shape = SplitMateTheme.RadiusBadge,
                            color = if (transfer.hasLinkedPhone) SplitMateTheme.SageSurface else SplitMateTheme.TerracottaSurface,
                            border = BorderStroke(
                                1.dp,
                                if (transfer.hasLinkedPhone) SplitMateTheme.AccentSage else SplitMateTheme.TerracottaText.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (transfer.hasLinkedPhone) Icons.Rounded.CheckCircle else Icons.Rounded.PersonAddAlt1,
                                    contentDescription = null,
                                    tint = if (transfer.hasLinkedPhone) SplitMateTheme.SageText else SplitMateTheme.TerracottaText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (transfer.hasLinkedPhone) {
                                        "Phone Linked: ${transfer.upiId}"
                                    } else {
                                        "+ Link Contact"
                                    },
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (transfer.hasLinkedPhone) SplitMateTheme.SageText else SplitMateTheme.TerracottaText
                                )
                            }
                        }

                        if (toRoomMember != null) {
                            TextButton(
                                onClick = { editingFriend = toRoomMember },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Avatar & Handle",
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SplitMateTheme.TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (transfer.hasLinkedPhone) {
                            Button(
                                onClick = {
                                    val upiUri = Uri.parse(
                                        "upi://pay?pa=${transfer.upiId}&pn=${Uri.encode(transfer.toName)}&am=${transfer.amount}&cu=INR"
                                    )
                                    val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
                                    context.startActivity(Intent.createChooser(upiIntent, "Pay with UPI"))
                                },
                                shape = SplitMateTheme.RadiusButton,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SplitMateTheme.PrimaryDark,
                                    contentColor = SplitMateTheme.ScreenBg
                                ),
                                modifier = Modifier
                                    .weight(1.35f)
                                    .sizeIn(minHeight = 46.dp)
                            ) {
                                Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = SplitMateTheme.ScreenBg, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pay via UPI (Phone Linked)",
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontWeight = FontWeight.Bold,
                                    color = SplitMateTheme.ScreenBg,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        } else {
                            Button(
                                onClick = { launchContactPickerForMember(toRoomMember) },
                                shape = SplitMateTheme.RadiusButton,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SplitMateTheme.AccentSage,
                                    contentColor = Color(0xFF23201E)
                                ),
                                modifier = Modifier
                                    .weight(1.35f)
                                    .sizeIn(minHeight = 46.dp)
                            ) {
                                Icon(Icons.Rounded.Contacts, contentDescription = null, tint = Color(0xFF23201E), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+ Link Contact",
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF23201E),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.markGreedyTransferSettled(transfer.transfer) },
                            shape = SplitMateTheme.RadiusButton,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SplitMateTheme.PrimaryDark),
                            modifier = Modifier
                                .weight(1f)
                                .sizeIn(minHeight = 46.dp)
                        ) {
                            Text("Mark Paid", fontFamily = SplitMateTheme.FontRounded, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    AssistChip(
                        onClick = {
                            val upiText = if (transfer.hasLinkedPhone) " (${transfer.upiId})" else ""
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Hey ${transfer.fromName}! Reminder for ${transfer.formattedDisplayAmount} settlement to ${transfer.toName}$upiText on SplitMate."
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                        },
                        label = { Text("💬 Send WhatsApp Reminder", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        shape = SplitMateTheme.RadiusButton,
                        colors = AssistChipDefaults.assistChipColors(containerColor = SplitMateTheme.SurfaceMuted),
                        modifier = Modifier.sizeIn(minHeight = 42.dp)
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
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SplitMateTheme.SageText, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("All Accounts Balanced", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SplitMateTheme.PrimaryDark)
                    Text("Zero debt loop detected · Exact Split Certified", fontSize = 12.sp, color = SplitMateTheme.TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = SplitMateTheme.RadiusBadge,
                        color = SplitMateTheme.PrimaryDark
                    ) {
                        Text("Equilibrium Reached", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SplitMateTheme.ScreenBg, modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp))
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
    val sym = "₹"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(DesignSystemBindings.PixelSectionSpacing)
    ) {
        item {
            Text(
                text = "Audit & Local Vault",
                fontFamily = SplitMateTheme.FontDisplay,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SplitMateTheme.PrimaryDark
            )
            Text(
                text = "Cryptographic ledger footprint and SQLite persistence",
                fontFamily = SplitMateTheme.FontRounded,
                fontSize = 12.sp,
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
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SplitMateTheme.AccentSage),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Storage, contentDescription = null, tint = Color(0xFF23201E))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Private On-Device Vault", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SplitMateTheme.PrimaryDark)
                            Text("Room SQLite · Native ₹ INR Vault", fontSize = 12.sp, color = SplitMateTheme.TextSecondary)
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
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
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
                                    text = "Paid by ${payer?.name ?: "You"} · ₹ INR (${expense.syncStatus})",
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

                    Spacer(modifier = Modifier.height(10.dp))
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
                            modifier = Modifier.sizeIn(minHeight = 42.dp)
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
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.setOfflineMode(!uiState.isOfflineMode) },
                    shape = SplitMateTheme.RadiusButton,
                    modifier = Modifier
                        .weight(1f)
                        .sizeIn(minHeight = 46.dp)
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
                        .sizeIn(minHeight = 46.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF23201E))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Recalculate & Sync", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF23201E))
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
    val diceBearSvgUrl = remember(initials) {
        buildDiceBearOpenPeepsUrl(initials)
    }
    val cleanInitials = remember(initials) {
        extractInitialsFromNameOrSeed(initials)
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bg)
            .border(2.dp, SplitMateTheme.SurfaceWhite, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cleanInitials,
            fontFamily = SplitMateTheme.FontDisplay,
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
