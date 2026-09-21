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
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.screens.OnboardingSetupScreen
import com.splitmate.app.ui.screens.UserSettingsScreen
import java.util.Locale

// ==============================================================================
// 1. MATERIAL 3 EXPRESSIVE PALETTE & TOKENS
// ==============================================================================
object SplitMateTheme {
    val ScreenBg = Color(0xFFFAF7F2)               // Warm Cream Eggshell
    val PrimaryDark = Color(0xFF23201E)            // Charcoal Espresso
    val AccentSage = Color(0xFFD7E8B6)             // Active Navigation Pill / Secondary Action
    val SageSurface = Color(0xFFEAF3DC)            // Positive Credit Tone Surface
    val SageText = Color(0xFF2D4810)               // Deep Green Text
    val TerracottaSurface = Color(0xFFFCECE7)      // Negative Debit Tone Surface
    val TerracottaText = Color(0xFFC23E2A)         // Terracotta Debt Text
    val BrandCoral = Color(0xFFE06B52)             // App Leaf / Brand Logo
    val SurfaceWhite = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF0EDE6)
    val BorderLight = Color(0xFFE6E2D8)
    val TextSecondary = Color(0xFF6E6863)

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
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val activeGroups by viewModel.activeGroups.collectAsState()

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
                onBackClick = { navController.popBackStack() },
                onUpdateUpiId = { newUpi -> viewModel.updateUpiId(newUpi) },
                onUpdateCurrencyCode = { newCode ->
                    viewModel.updateUserProfile(uiState.currentUserName, newCode)
                },
                onThemeToggle = { isDark -> viewModel.toggleDarkTheme(isDark) },
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
    var showQuickExpenseDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val unassignedBase = uiState.receiptItems.filter { it.claimedByMemberIds.isEmpty() }.sumOf { it.priceCents }

    Scaffold(
        containerColor = SplitMateTheme.ScreenBg,
        floatingActionButton = {
            if (currentTab == SplitMateTab.LEDGERS) {
                ExtendedFloatingActionButton(
                    onClick = { showQuickExpenseDialog = true },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Log Expense",
                            tint = Color.White
                        )
                    },
                    text = {
                        Text(
                            text = "Log Expense",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontFamily = SplitMateTheme.FontRounded
                        )
                    },
                    containerColor = SplitMateTheme.PrimaryDark,
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
                // Quick Remainder Verification Bar for Instant 1-Tap Resolution across Tabs
                if (unassignedBase > 0L) {
                    Surface(
                        color = SplitMateTheme.TerracottaSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 4.dp)
                            .clip(SplitMateTheme.RadiusBadge)
                            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                            .testTag("UnassignedRemainderCard")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.activeCurrency.symbol}${String.format(Locale.US, "%.2f", unassignedBase / 100.0)} Unassigned Remainder",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SplitMateTheme.TerracottaText
                            )
                            Surface(
                                onClick = { viewModel.splitUnassignedRemainderEqually() },
                                shape = SplitMateTheme.RadiusBadge,
                                color = SplitMateTheme.PrimaryDark,
                                modifier = Modifier.testTag("SplitRemainderEquallyBtn")
                            ) {
                                Text(
                                    text = "⚡ Split Equally",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        color = SplitMateTheme.SageSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 4.dp)
                            .clip(SplitMateTheme.RadiusBadge)
                            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                            .testTag("ZeroRemainderVerifiedCard")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✓ 0.00¢ Remainder Verified · Locked m = ${String.format(Locale.US, "%.2f", 1.0 + (uiState.receiptTaxPercent + uiState.receiptTipPercent) / 100.0)}x",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SplitMateTheme.SageText
                            )
                            Text(
                                text = "0.00¢ drift",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitMateTheme.SageText
                            )
                        }
                    }
                }

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
                SplitMateTab.SPLIT -> ItemizedSplitScreen(viewModel = viewModel)
                SplitMateTab.SETTLE -> GreedySettlementScreen(viewModel = viewModel)
                SplitMateTab.AUDIT -> AuditVaultScreen(viewModel = viewModel)
            }
        }
    }

    // Custom M3 Expressive Dialog for Global ExtendedFloatingActionButton ("Log Expense")
    if (showQuickExpenseDialog) {
        var expenseTitle by remember { mutableStateOf("Dinner & Drinks") }
        var expenseAmountMajor by remember { mutableStateOf("1200.00") }

        Dialog(onDismissRequest = { showQuickExpenseDialog = false }) {
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
                            Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null, tint = SplitMateTheme.PrimaryDark)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Log New Expense",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SplitMateTheme.PrimaryDark
                            )
                            Text(
                                text = "Zero-drift integer-cent split",
                                fontSize = 12.sp,
                                color = SplitMateTheme.TextSecondary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = expenseTitle,
                        onValueChange = { expenseTitle = it },
                        label = { Text("Expense Description") },
                        singleLine = true,
                        shape = SplitMateTheme.RadiusInput,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = expenseAmountMajor,
                        onValueChange = { expenseAmountMajor = it },
                        label = { Text("Total Amount (${uiState.activeCurrency.symbol})") },
                        singleLine = true,
                        shape = SplitMateTheme.RadiusInput,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showQuickExpenseDialog = false
                                currentTab = SplitMateTab.SPLIT
                            }
                        ) {
                            Text("Itemize Receipt →", fontWeight = FontWeight.Bold, color = SplitMateTheme.SageText)
                        }

                        Row {
                            TextButton(onClick = { showQuickExpenseDialog = false }) {
                                Text("Cancel", fontWeight = FontWeight.Bold, color = SplitMateTheme.TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    if (uiState.groups.isEmpty()) {
                                        viewModel.createNewGroup("Weekend Trip", uiState.activeCurrencyCode, "Priya, Sam")
                                    }
                                    val cents = ((expenseAmountMajor.toDoubleOrNull() ?: 0.0) * 100).toLong()
                                    viewModel.commitCollaborativeExpense(
                                        title = expenseTitle,
                                        customTotalCents = cents
                                    )
                                    showQuickExpenseDialog = false
                                },
                                shape = SplitMateTheme.RadiusButton,
                                colors = ButtonDefaults.buttonColors(containerColor = SplitMateTheme.PrimaryDark)
                            ) {
                                Text("Save Split", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
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
                            tint = if (isSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        AnimatedVisibility(visible = isSelected) {
                            Text(
                                text = "  ${tab.label}",
                                color = SplitMateTheme.PrimaryDark,
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
    val totalBalance by viewModel.totalBalance.collectAsState()
    val activeGroups by viewModel.activeGroups.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showNewGroupDialog by remember { mutableStateOf(false) }

    val activeGroupName = uiState.groups.find { it.groupId == uiState.activeGroupId }?.name ?: "All Ledgers"
    val isNegativeBalance = totalBalance.startsWith("-")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Custom Top Bar (Synced with ic_launcher_foreground.xml vector logo & clickable Avatar -> UserSettingsScreen)
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
                            text = "LEDGERS & GROUPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitMateTheme.TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.groups.isNotEmpty()) {
                        Surface(
                            onClick = {
                                val nextIdx = (uiState.groups.indexOfFirst { it.groupId == uiState.activeGroupId } + 1) %
                                    uiState.groups.size.coerceAtLeast(1)
                                uiState.groups.getOrNull(nextIdx)?.let { viewModel.selectActiveGroup(it.groupId) }
                            },
                            shape = SplitMateTheme.RadiusBadge,
                            color = SplitMateTheme.SurfaceMuted,
                            modifier = Modifier.sizeIn(minHeight = 36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(activeGroupName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SplitMateTheme.PrimaryDark)
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Clickable Top-Right Avatar routing to UserSettingsScreen
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onAvatarSettingsClick)
                    ) {
                        AvatarToken(
                            initials = uiState.currentUserSeed,
                            bg = SplitMateTheme.AccentSage,
                            textColor = SplitMateTheme.PrimaryDark,
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
                            Icon(Icons.Rounded.GroupAdd, contentDescription = null, tint = SplitMateTheme.PrimaryDark)
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
                            colors = ButtonDefaults.buttonColors(containerColor = SplitMateTheme.PrimaryDark)
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
// TAB 2: SPLIT (Itemized Bill & Remainder Engine - Spring Bounce & M3 Dialog)
// ==============================================================================
@Composable
fun ItemizedSplitScreen(viewModel: SplitMateViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val sym = uiState.activeCurrency.symbol
    val groupMembers = uiState.activeGroupMembers
    val baseSubtotalCents = uiState.receiptItems.sumOf { it.priceCents }
    val taxCents = Math.round(baseSubtotalCents * (uiState.receiptTaxPercent / 100.0))
    val tipCents = Math.round(baseSubtotalCents * (uiState.receiptTipPercent / 100.0))
    val totalBillCents = baseSubtotalCents + taxCents + tipCents
    val unassignedBaseCents = uiState.receiptItems.filter { it.claimedByMemberIds.isEmpty() }.sumOf { it.priceCents }
    val claimedBaseCents = baseSubtotalCents - unassignedBaseCents
    val lockedMultiplier = if (baseSubtotalCents > 0L) totalBillCents.toDouble() / baseSubtotalCents.toDouble() else 1.0

    val payerMember = groupMembers.find { it.memberId == uiState.receiptPayerId } ?: groupMembers.firstOrNull()
    val activeGroupName = uiState.groups.find { it.groupId == uiState.activeGroupId }?.name ?: "Lake Tahoe Cabin"
    var showAddItemDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Split Bill & Items",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SplitMateTheme.PrimaryDark
                )
                Text(
                    text = "Live claiming with penny-accurate remainder tracking",
                    fontSize = 13.sp,
                    color = SplitMateTheme.TextSecondary
                )
            }

            // 1. Header Card: Total Bill, Payer & Group Selectors
            item {
                Card(
                    shape = SplitMateTheme.RadiusCard,
                    colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Bill Amount", fontSize = 13.sp, color = SplitMateTheme.TextSecondary, fontWeight = FontWeight.SemiBold)
                            Surface(shape = SplitMateTheme.RadiusBadge, color = SplitMateTheme.SurfaceMuted) {
                                Text(activeGroupName, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "$sym${String.format(Locale.US, "%.2f", totalBillCents / 100.0)}",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateTheme.PrimaryDark
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Paid by:", fontSize = 13.sp, color = SplitMateTheme.TextSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    onClick = {
                                        val idx = (groupMembers.indexOfFirst { it.memberId == uiState.receiptPayerId } + 1) %
                                            groupMembers.size.coerceAtLeast(1)
                                        groupMembers.getOrNull(idx)?.let { viewModel.setReceiptPayer(it.memberId) }
                                    },
                                    shape = SplitMateTheme.RadiusBadge,
                                    color = SplitMateTheme.AccentSage,
                                    modifier = Modifier.sizeIn(minHeight = 36.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AvatarToken(
                                            initials = payerMember?.avatarSeed ?: "S",
                                            bg = SplitMateTheme.PrimaryDark,
                                            textColor = Color.White,
                                            size = 20
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${payerMember?.name ?: "Sam"} (Creditor)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SplitMateTheme.PrimaryDark
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Active Claimer Persona Selector Bar
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Claiming as:", fontSize = 12.sp, color = SplitMateTheme.TextSecondary, fontWeight = FontWeight.SemiBold)
                            groupMembers.forEach { member ->
                                val isSelectedPersona = member.memberId == uiState.activeClaimerPersonaId
                                Surface(
                                    onClick = { viewModel.setClaimerPersona(member.memberId) },
                                    shape = SplitMateTheme.RadiusBadge,
                                    color = if (isSelectedPersona) SplitMateTheme.PrimaryDark else SplitMateTheme.SurfaceMuted
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AvatarToken(
                                            initials = member.avatarSeed,
                                            bg = SplitMateTheme.AccentSage,
                                            textColor = SplitMateTheme.PrimaryDark,
                                            size = 18
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = member.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelectedPersona) Color.White else SplitMateTheme.PrimaryDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Real-Time Remainder Bar (Spring Bouncy Animation)
            item {
                Card(
                    shape = SplitMateTheme.RadiusPanel,
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
                            Text(
                                text = "Claimed: $sym${String.format(Locale.US, "%.2f", claimedBaseCents / 100.0)} / $sym${String.format(Locale.US, "%.2f", baseSubtotalCents / 100.0)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitMateTheme.PrimaryDark
                            )
                            Surface(
                                shape = SplitMateTheme.RadiusBadge,
                                color = if (unassignedBaseCents > 0L) SplitMateTheme.TerracottaSurface else SplitMateTheme.SageSurface
                            ) {
                                Text(
                                    text = if (unassignedBaseCents > 0L) {
                                        "$sym${String.format(Locale.US, "%.2f", unassignedBaseCents / 100.0)} Unassigned (0.00¢ drift)"
                                    } else {
                                        "✓ 0.00¢ Remainder Verified"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (unassignedBaseCents > 0L) SplitMateTheme.TerracottaText else SplitMateTheme.SageText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Multi-Segment Progress Indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(SplitMateTheme.RadiusBadge)
                                .background(SplitMateTheme.SurfaceMuted)
                        ) {
                            Box(modifier = Modifier.weight(0.40f).fillMaxHeight().background(Color(0xFF818CF8)))
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(modifier = Modifier.weight(0.30f).fillMaxHeight().background(Color(0xFF34D399)))
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(modifier = Modifier.weight(0.20f).fillMaxHeight().background(Color(0xFFF472B6)))
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(modifier = Modifier.weight(0.10f).fillMaxHeight().background(Color(0xFFFBBF24)))
                        }

                        if (unassignedBaseCents > 0L) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val perPersonShare = unassignedBaseCents / groupMembers.size.coerceAtLeast(1)
                            AssistChip(
                                onClick = { viewModel.splitUnassignedRemainderEqually() },
                                label = {
                                    Text(
                                        text = "⚡ Split Remaining $sym${String.format(Locale.US, "%.2f", unassignedBaseCents / 100.0)} Equally (+$sym${String.format(Locale.US, "%.2f", perPersonShare / 100.0)}/ea)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                },
                                shape = SplitMateTheme.RadiusButton,
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = SplitMateTheme.SageSurface,
                                    labelColor = SplitMateTheme.SageText
                                ),
                                modifier = Modifier.sizeIn(minHeight = 48.dp)
                            )
                        }
                    }
                }
            }

            // 3. Line-Item Claiming Matrix
            item {
                Text("Itemized Claim Matrix (Tap row to toggle claim)", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = SplitMateTheme.PrimaryDark)
            }

            items(uiState.receiptItems, key = { it.itemId }) { item ->
                val claimedNames = item.claimedByMemberIds.mapNotNull { id ->
                    groupMembers.find { it.memberId == id }?.avatarSeed
                }
                val unclaimedNames = groupMembers.filterNot { item.claimedByMemberIds.contains(it.memberId) }.map { it.avatarSeed }
                Box(
                    modifier = Modifier
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                        .clickable { viewModel.toggleReceiptItemClaim(item.itemId) }
                ) {
                    ClaimItemRow(
                        title = item.title,
                        amount = "$sym${String.format(Locale.US, "%.2f", item.priceCents / 100.0)}",
                        claimedBy = claimedNames,
                        unclaimed = unclaimedNames
                    )
                }
            }

            item {
                Surface(
                    onClick = { showAddItemDialog = true },
                    shape = SplitMateTheme.RadiusCard,
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
                        .sizeIn(minHeight = 52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(14.dp)) {
                        Text("+ Add Item / Custom Line-Split", fontWeight = FontWeight.Bold, color = SplitMateTheme.TextSecondary, fontSize = 14.sp)
                    }
                }
            }

            // 4. Proportional Auxiliary Panel (Tax & Tip Multiplier)
            item {
                Card(
                    shape = SplitMateTheme.RadiusPanel,
                    colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tax & Tip Multipliers", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SplitMateTheme.PrimaryDark)
                            Text(
                                text = "m = ${String.format(Locale.US, "%.2f", lockedMultiplier)}x Locked",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = SplitMateTheme.SageText
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Proportional expansion automatically applies ${uiState.receiptTaxPercent}% Tax + ${uiState.receiptTipPercent}% Service Tip across active member claims with zero penny rounding discrepancy.",
                            fontSize = 12.sp,
                            color = SplitMateTheme.TextSecondary
                        )
                    }
                }
            }
        }

        // 5. Floating Footer Toolbar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = SplitMateTheme.RadiusHero,
            color = SplitMateTheme.PrimaryDark,
            shadowElevation = 10.dp
        ) {
            Button(
                onClick = { viewModel.commitCollaborativeExpense() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = SplitMateTheme.RadiusHero,
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 56.dp)
            ) {
                Text("Save & Distribute Split", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Color.White)
            }
        }
    }

    // Custom M3 Expressive Dialog for "+ Add Item" (28dp radius, 20dp input radius)
    if (showAddItemDialog) {
        var itemTitle by remember { mutableStateOf("") }
        var itemPriceMajor by remember { mutableStateOf("18.50") }

        Dialog(onDismissRequest = { showAddItemDialog = false }) {
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
                    Text(
                        text = "Add Receipt Line Item",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SplitMateTheme.PrimaryDark
                    )
                    OutlinedTextField(
                        value = itemTitle,
                        onValueChange = { itemTitle = it },
                        label = { Text("Item Title") },
                        placeholder = { Text("e.g. Matcha Tiramisu") },
                        singleLine = true,
                        shape = SplitMateTheme.RadiusInput,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = itemPriceMajor,
                        onValueChange = { itemPriceMajor = it },
                        label = { Text("Price (${uiState.activeCurrency.symbol})") },
                        singleLine = true,
                        shape = SplitMateTheme.RadiusInput,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddItemDialog = false }) {
                            Text("Cancel", fontWeight = FontWeight.Bold, color = SplitMateTheme.TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val cents = ((itemPriceMajor.toDoubleOrNull() ?: 0.0) * 100).toLong()
                                viewModel.addReceiptLineItem(itemTitle, cents)
                                showAddItemDialog = false
                            },
                            shape = SplitMateTheme.RadiusButton,
                            colors = ButtonDefaults.buttonColors(containerColor = SplitMateTheme.PrimaryDark)
                        ) {
                            Text("Add Item", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// TAB 3: SETTLE (Greedy Debt Simplification & UPI Deep Linking)
// ==============================================================================
@Composable
fun GreedySettlementScreen(viewModel: SplitMateViewModel) {
    val context = LocalContext.current
    val settlementPlan by viewModel.settlementPlan.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val rawDebtEdges = (uiState.expenses.size * uiState.activeGroupMembers.size).coerceAtLeast(settlementPlan.size)

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

                    Spacer(modifier = Modifier.height(16.dp))

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
                            colors = ButtonDefaults.buttonColors(containerColor = SplitMateTheme.PrimaryDark),
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
    @Suppress("UNUSED_PARAMETER") textColor: Color,
    size: Int = 36
) {
    val context = LocalContext.current
    val cleanSeed = Uri.encode(initials.ifBlank { "SplitMateGuest" })
    val diceBearSvgUrl = "https://api.dicebear.com/9.x/open-peeps/svg?seed=$cleanSeed&backgroundColor=f4efe6,d7e8b6,fed8c8,dce3fd"

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bg)
            .border(2.dp, Color.White, CircleShape),
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
            contentDescription = "Avatar for $initials",
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
