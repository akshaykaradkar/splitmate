package com.splitmate.app

import com.splitmate.app.ui.*
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
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
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
import com.splitmate.app.ui.ContactPickerBottomSheet
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.GroupCategoryIcons
import com.splitmate.app.ui.NewGroupMemberDraft
import com.splitmate.app.ui.SplitMateBrandFontFamily
import com.splitmate.app.ui.SplitMateDisplayFontFamily
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.buildDiceBearOpenPeepsUrl
import com.splitmate.app.ui.extractInitialsFromNameOrSeed
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
                onUpdateUpiId = { newUpi -> viewModel.updateUserProfile(uiState.currentUserName, uiState.currentUserSeed, newUpi) },
                onUpdateCurrencyCode = {},
                onUpdateUserProfile = { newName, newSeed ->
                    viewModel.updateUserProfile(newName, newSeed, uiState.userUpiId)
                },
                onThemeToggle = { isDark ->
                    prefs.edit().putBoolean("is_dark_theme", isDark).apply()
                    viewModel.toggleDarkTheme(isDark)
                },
                onExportLedgerText = {
                    buildString {
                        appendLine("📊 SplitMate Trip & Ledger Summary")
                        appendLine("User: ${uiState.currentUserName} (${uiState.userUpiId.ifBlank { "UPI not set" }})")
                        appendLine("Overall Net Position: $totalBalance")
                        appendLine("Active Groups (${uiState.groups.size}):")
                        uiState.groups.forEach { g ->
                            val gExps = uiState.expenses.filter { it.groupId == g.groupId }
                            val gSpend = gExps.sumOf { it.totalAmountCents } / 100.0
                            appendLine("• ${g.name}: ${gExps.size} expenses (Total ₹${String.format(Locale.US, "%.2f", gSpend)})")
                        }
                    }
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
    val uiState by viewModel.uiState.collectAsState()
    val currentTab = remember(uiState.selectedTabName) {
        runCatching { SplitMateTab.valueOf(uiState.selectedTabName) }.getOrDefault(SplitMateTab.LEDGERS)
    }

    // Intercept system Back when on SPLIT, SETTLE, or AUDIT so user returns to their active group or Ledgers instead of exiting the app!
    androidx.activity.compose.BackHandler(enabled = currentTab != SplitMateTab.LEDGERS) {
        if (uiState.returnToGroupDetailId != null) {
            viewModel.finishSubFlowToGroupDetail(uiState.returnToGroupDetailId)
        } else {
            viewModel.selectTab(SplitMateTab.LEDGERS.name)
        }
    }

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
                    onClick = {
                        viewModel.navigateToSubFlow(
                            targetTabName = SplitMateTab.SPLIT.name,
                            originGroupDetailId = uiState.openedGroupDetailId
                        )
                    },
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
                    onTabSelected = { tab -> viewModel.selectTab(tab.name) }
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
                    onNavigateToSplit = {
                        viewModel.navigateToSubFlow(
                            targetTabName = SplitMateTab.SPLIT.name,
                            originGroupDetailId = uiState.openedGroupDetailId
                        )
                    },
                    onNavigateToSettle = {
                        viewModel.navigateToSubFlow(
                            targetTabName = SplitMateTab.SETTLE.name,
                            originGroupDetailId = uiState.openedGroupDetailId
                        )
                    },
                    onAvatarSettingsClick = onOpenSettings
                )
                SplitMateTab.SPLIT -> QuickExpenseScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        if (uiState.returnToGroupDetailId != null) {
                            viewModel.finishSubFlowToGroupDetail(uiState.returnToGroupDetailId)
                        } else {
                            viewModel.selectTab(SplitMateTab.LEDGERS.name)
                        }
                    },
                    onSaveSplit = { _, _ ->
                        // Return directly inside the group where the expense was logged!
                        val loggedGroupId = viewModel.uiState.value.activeGroup?.groupId
                        viewModel.finishSubFlowToGroupDetail(loggedGroupId)
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
// TAB 1: LEDGERS (Dashboard Screen + Inside-Group Detail View)
// ==============================================================================
@Composable
fun LedgersDashboardScreen(
    viewModel: SplitMateViewModel,
    onNavigateToSplit: () -> Unit = {},
    onNavigateToSettle: () -> Unit = {},
    onAvatarSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val totalBalance by viewModel.totalBalance.collectAsState()
    val activeGroups by viewModel.activeGroups.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var editingFriend by remember { mutableStateOf<GroupMemberEntity?>(null) }
    val openedGroupDetailId = uiState.openedGroupDetailId
    var expandedExpenseId by remember { mutableStateOf<String?>(null) }
    var editingExpense by remember { mutableStateOf<com.splitmate.app.data.ExpenseEntity?>(null) }
    var showAddContactsToExistingGroupSheet by remember { mutableStateOf(false) }

    // Intercept system Back when viewing inside a specific Group so user returns to All Groups list instead of exiting the app!
    androidx.activity.compose.BackHandler(enabled = openedGroupDetailId != null) {
        if (editingExpense != null) {
            editingExpense = null
        } else if (editingFriend != null) {
            editingFriend = null
        } else if (showAddContactsToExistingGroupSheet) {
            showAddContactsToExistingGroupSheet = false
        } else {
            viewModel.closeGroupDetail()
        }
    }

    editingExpense?.let { exp ->
        val expGroupMembers = uiState.members.filter { it.groupId == exp.groupId }
        EditLoggedExpenseDialog(
            expense = exp,
            groupMembers = expGroupMembers,
            onDismiss = { editingExpense = null },
            onSave = { newTitle, newAmountRupees, newPayerId ->
                viewModel.editExistingExpense(
                    expenseId = exp.expenseId,
                    newTitle = newTitle,
                    newTotalRupees = newAmountRupees,
                    newPayerId = newPayerId
                )
                editingExpense = null
            }
        )
    }

    val deviceContacts by viewModel.deviceContacts.collectAsState()
    val isLoadingContacts by viewModel.isLoadingContacts.collectAsState()

    val addToGroupPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.loadDeviceContacts(context)
            showAddContactsToExistingGroupSheet = true
        }
    }

    val isNegativeBalance = totalBalance.startsWith("-")

    editingFriend?.let { friend ->
        val editableGroupMembers = uiState.members.filter {
            it.groupId == friend.groupId && !it.isCurrentUser
        }.ifEmpty { listOf(friend) }
        EditFriendUpiDialog(
            member = friend,
            allGroupMembers = editableGroupMembers,
            onSelectMember = { editingFriend = it },
            onDismiss = { editingFriend = null },
            onSave = { newName, newUpi, newAvatarSeed ->
                viewModel.updateFriendUpi(friend.memberId, newName, newUpi, newAvatarSeed)
                editingFriend = null
            }
        )
    }

    val openedGroup = uiState.groups.find { it.groupId == openedGroupDetailId }
    if (showAddContactsToExistingGroupSheet && openedGroup != null) {
        ContactPickerBottomSheet(
            contacts = deviceContacts,
            isLoading = isLoadingContacts,
            multiSelect = true,
            title = "Add Contacts to ${openedGroup.name}",
            subtitle = "Select contacts to add to this group",
            onDismissRequest = { showAddContactsToExistingGroupSheet = false },
            onConfirmSelected = { selected ->
                viewModel.addContactsToGroup(openedGroup.groupId, selected)
                showAddContactsToExistingGroupSheet = false
            }
        )
    }

    if (openedGroup != null) {
        val groupMembers = uiState.members.filter { it.groupId == openedGroup.groupId }
        val groupExpenses = uiState.expenses.filter { it.groupId == openedGroup.groupId }
        val totalGroupSpendCents = groupExpenses.sumOf { it.totalAmountCents }
        val groupIcon = resolveGroupCategoryIcon(openedGroup.iconName, openedGroup.name)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back to All Groups Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { viewModel.closeGroupDetail() },
                        shape = SplitMateTheme.RadiusBadge,
                        color = SplitMateTheme.SurfaceWhite,
                        border = BorderStroke(1.dp, SplitMateTheme.BorderLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back to All Groups",
                                tint = SplitMateTheme.PrimaryDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "All Groups",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SplitMateTheme.PrimaryDark
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_CONTACTS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    viewModel.loadDeviceContacts(context)
                                    showAddContactsToExistingGroupSheet = true
                                } else {
                                    addToGroupPermLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            },
                            shape = SplitMateTheme.RadiusBadge,
                            color = SplitMateTheme.SageSurface,
                            border = BorderStroke(1.dp, SplitMateTheme.AccentSage)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PersonAdd,
                                    contentDescription = "Add Contact",
                                    tint = SplitMateTheme.SageText,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "+ Add Contact",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = SplitMateTheme.SageText
                                )
                            }
                        }

                        Surface(
                            onClick = onNavigateToSettle,
                            shape = SplitMateTheme.RadiusBadge,
                            color = SplitMateTheme.PrimaryDark
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapHoriz,
                                    contentDescription = "Settle Up",
                                    tint = SplitMateTheme.ScreenBg,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Settle Up",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = SplitMateTheme.ScreenBg
                                )
                            }
                        }
                    }
                }
            }

            // Group Hero Summary Card
            item {
                Card(
                    shape = SplitMateTheme.RadiusHero,
                    colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusHero)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(SplitMateTheme.SageSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(groupIcon, contentDescription = null, tint = SplitMateTheme.SageText, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = openedGroup.name,
                                        fontFamily = SplitMateTheme.FontDisplay,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SplitMateTheme.PrimaryDark
                                    )
                                    Text(
                                        text = "${groupMembers.size} members · ${groupExpenses.size} expenses",
                                        fontSize = 12.sp,
                                        color = SplitMateTheme.TextSecondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Spend",
                                    fontSize = 11.sp,
                                    color = SplitMateTheme.TextSecondary
                                )
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", totalGroupSpendCents / 100.0)}",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SplitMateTheme.PrimaryDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = SplitMateTheme.BorderLight)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Group Members Horizontal Strip (Tap any member to edit/link contact)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Group Members (Tap to edit)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitMateTheme.PrimaryDark
                            )
                            val firstEditable = groupMembers.firstOrNull { !it.isCurrentUser }
                            if (firstEditable != null) {
                                Text(
                                    text = "Manage All →",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SplitMateTheme.SageText,
                                    modifier = Modifier.clickable { editingFriend = firstEditable }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(groupMembers, key = { it.memberId }) { mbr ->
                                Surface(
                                    onClick = {
                                        if (!mbr.isCurrentUser) {
                                            editingFriend = mbr
                                        } else {
                                            onAvatarSettingsClick()
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = SplitMateTheme.SurfaceMuted,
                                    border = BorderStroke(1.dp, SplitMateTheme.BorderLight)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AvatarToken(
                                            initials = mbr.avatarSeed,
                                            bg = SplitMateTheme.AccentSage,
                                            textColor = Color(0xFF23201E),
                                            size = 30
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (mbr.isCurrentUser) "${mbr.name} (You)" else mbr.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SplitMateTheme.PrimaryDark
                                            )
                                            val phone = mbr.upiId.substringBefore("@").replace(Regex("[^0-9]"), "")
                                            Text(
                                                text = if (phone.length == 10) "+91 $phone" else if (mbr.isCurrentUser) "Group Admin" else "Tap to link phone",
                                                fontSize = 10.sp,
                                                color = if (phone.length == 10 || mbr.isCurrentUser) SplitMateTheme.SageText else SplitMateTheme.TerracottaText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Expenses Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Group Expenses (${groupExpenses.size})",
                        fontFamily = SplitMateTheme.FontDisplay,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SplitMateTheme.PrimaryDark
                    )
                    Surface(
                        onClick = onNavigateToSplit,
                        shape = SplitMateTheme.RadiusBadge,
                        color = SplitMateTheme.PrimaryDark
                    ) {
                        Text(
                            text = "+ Log Expense",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitMateTheme.ScreenBg,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (groupExpenses.isEmpty()) {
                item {
                    Card(
                        shape = SplitMateTheme.RadiusCard,
                        colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = SplitMateTheme.SageText,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No expenses in ${openedGroup.name} yet",
                                fontFamily = SplitMateTheme.FontDisplay,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SplitMateTheme.PrimaryDark
                            )
                            Text(
                                text = "Tap + Log Expense to record your first shared bill.",
                                fontSize = 12.sp,
                                color = SplitMateTheme.TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(groupExpenses, key = { it.expenseId }) { expense ->
                    val payer = uiState.members.find { it.memberId == expense.payerId }
                    val isMePayer = payer?.isCurrentUser == true
                    val formattedTotal = "₹${String.format(Locale.US, "%.2f", expense.totalAmountCents / 100.0)}"
                    val isExpanded = expandedExpenseId == expense.expenseId
                    val memberCount = groupMembers.size.coerceAtLeast(1)
                    val perPersonShare = "₹${String.format(Locale.US, "%.2f", (expense.totalAmountCents / memberCount) / 100.0)}"

                    Card(
                        onClick = {
                            expandedExpenseId = if (isExpanded) null else expense.expenseId
                        },
                        shape = SplitMateTheme.RadiusCard,
                        colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
                            .animateContentSize()
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
                                            .background(if (isMePayer) Color(0xFFE0E7FF) else SplitMateTheme.TerracottaSurface),
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
                                        Text(
                                            text = expense.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = SplitMateTheme.PrimaryDark
                                        )
                                        Text(
                                            text = "Paid by ${payer?.name ?: "You"} · $perPersonShare / person",
                                            fontFamily = SplitMateTheme.FontRounded,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 0.sp,
                                            fontSize = 12.sp,
                                            color = SplitMateTheme.TextSecondary
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = (if (isMePayer) "+" else "-") + formattedTotal,
                                        fontFamily = SplitMateTheme.FontDisplay,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                                        color = if (isMePayer) SplitMateTheme.SageText else SplitMateTheme.TerracottaText
                                    )
                                    Text(
                                        text = if (isExpanded) "Hide split ▲" else "View split ▼",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SplitMateTheme.SageText
                                    )
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = SplitMateTheme.BorderLight)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Split Breakdown (${groupMembers.size} members)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SplitMateTheme.PrimaryDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                groupMembers.forEach { mbr ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (mbr.isCurrentUser) "${mbr.name} (You)" else mbr.name,
                                            fontSize = 12.sp,
                                            color = SplitMateTheme.TextSecondary
                                        )
                                        Text(
                                            text = perPersonShare,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SplitMateTheme.PrimaryDark
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Surface(
                                        onClick = { viewModel.rollbackExpense(expense.expenseId) },
                                        shape = SplitMateTheme.RadiusBadge,
                                        color = SplitMateTheme.TerracottaSurface
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.Undo,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = SplitMateTheme.TerracottaText
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Delete / Undo Expense",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SplitMateTheme.TerracottaText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(DesignSystemBindings.PixelSectionSpacing)
    ) {
        // 1. Custom Top Bar (Subtitle "Fun & Trip Expenses", Clickable Avatar — NO redundant INR pill)
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
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isNegativeBalance) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isNegativeBalance) SplitMateTheme.TerracottaText else SplitMateTheme.SageText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isNegativeBalance) "You owe overall" else "You are owed overall",
                                        fontFamily = SplitMateTheme.FontRounded,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNegativeBalance) SplitMateTheme.TerracottaText else SplitMateTheme.SageText
                                    )
                                }
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
                                    text = if (uiState.isOfflineMode) "Offline" else "Live Sync",
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
                            letterSpacing = (-1.5).sp,
                            color = SplitMateTheme.PrimaryDark,
                            fontFamily = SplitMateTheme.FontDisplay,
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                        )

                        Text(
                            text = "Across ${activeGroups.size} active groups",
                            fontFamily = SplitMateTheme.FontRounded,
                            fontSize = 13.sp,
                            letterSpacing = 0.sp,
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
                                Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null, tint = Color(0xFF23201E), modifier = Modifier.size(18.dp))
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Sort by balance",
                        fontFamily = SplitMateTheme.FontRounded,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SplitMateTheme.TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.SwapVert,
                        contentDescription = null,
                        tint = SplitMateTheme.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
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
                onClick = {
                    viewModel.openGroupDetail(groupCard.groupId)
                },
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
                                Text("${groupCard.memberCount} members · Tap to view expenses", fontSize = 12.sp, color = SplitMateTheme.TextSecondary)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = SplitMateTheme.RadiusBadge,
                                color = if (groupCard.netBalanceCents == 0L) SplitMateTheme.SageSurface else SplitMateTheme.SurfaceWhite.copy(alpha = 0.9f)
                            ) {
                                Text(
                                    text = groupCard.statusPillText,
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    color = badgeTextColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = groupCard.formattedBadgeText,
                                fontFamily = SplitMateTheme.FontDisplay,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.8).sp,
                                color = badgeTextColor,
                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
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
                                            imageVector = Icons.Rounded.ManageAccounts,
                                            contentDescription = "Edit Group Members",
                                            tint = SplitMateTheme.PrimaryDark,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Members",
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
                            subtitle = "Paid by ${if (isMePayer) "you" else (payer?.name ?: "Member")} · Tap group to view details",
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
    // CREATE GROUP DIALOG (100% Real Contacts Based + Live Inline Contact Search)
    // =========================================================================
    if (showNewGroupDialog) {
        var groupNameInput by remember { mutableStateOf("") }
        var selectedIconKey by remember { mutableStateOf("Flight") }
        var selectedMembers by remember { mutableStateOf(listOf<NewGroupMemberDraft>()) }
        var contactSearchQuery by remember { mutableStateOf("") }
        var showMultiContactSheet by remember { mutableStateOf(false) }

        val contactsPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted: Boolean ->
            if (granted) {
                viewModel.loadDeviceContacts(context)
            } else {
                Toast.makeText(context, "Contacts permission is required to search and add contacts", Toast.LENGTH_SHORT).show()
            }
        }

        // Automatically preload device contacts as soon as CreateGroupDialog opens if permission is granted
        LaunchedEffect(Unit) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm && deviceContacts.isEmpty()) {
                viewModel.loadDeviceContacts(context)
            }
        }

        // Live filtered contacts matching contactSearchQuery (excluding already selected contacts)
        val matchingDeviceContacts = remember(contactSearchQuery, deviceContacts, selectedMembers) {
            val q = contactSearchQuery.trim()
            if (q.isEmpty()) {
                emptyList()
            } else {
                val selectedPhones = selectedMembers.map { it.cleanPhone }.toSet()
                val selectedNames = selectedMembers.map { it.name.lowercase(Locale.US) }.toSet()
                deviceContacts.filter { c ->
                    !selectedPhones.contains(c.cleanPhone) &&
                        !selectedNames.contains(c.name.lowercase(Locale.US)) &&
                        (c.name.contains(q, ignoreCase = true) || c.cleanPhone.contains(q))
                }.take(4)
            }
        }

        if (showMultiContactSheet) {
            // CreateGroupDialog is temporarily hidden while ContactPickerBottomSheet is active
            // so the Dialog Window never overlaps or blocks the BottomSheet.
            ContactPickerBottomSheet(
                contacts = deviceContacts,
                isLoading = isLoadingContacts,
                multiSelect = true,
                initialSelectedPhones = selectedMembers.map { it.cleanPhone }.filter { it.isNotEmpty() }.toSet(),
                onDismiss = { showMultiContactSheet = false },
                onConfirmSelection = { chosenContacts ->
                    val newDrafts = chosenContacts.map { contact ->
                        NewGroupMemberDraft(
                            name = contact.name,
                            cleanPhone = contact.cleanPhone
                        )
                    }
                    val existingNames = selectedMembers.map { it.name.lowercase(Locale.US) }.toSet()
                    val merged = selectedMembers + newDrafts.filterNot {
                        existingNames.contains(it.name.lowercase(Locale.US))
                    }
                    selectedMembers = merged
                    showMultiContactSheet = false
                }
            )
        } else {
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
                                text = "Search or pick members directly from your Contacts",
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

                    // 3. Prominent "+ Browse All Contacts" Button (Opens In-App Multi-Select Sheet)
                    Button(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                viewModel.loadDeviceContacts(context)
                                showMultiContactSheet = true
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
                            text = "+ Browse & Multi-Select Contacts",
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
                                                    text = "+91 ${member.cleanPhone}",
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

                    // 5. Inline Live Contact Lookup Search Bar (Searches real deviceContacts as you type!)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = contactSearchQuery,
                                onValueChange = { query ->
                                    contactSearchQuery = query
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.READ_CONTACTS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm && deviceContacts.isEmpty()) {
                                        viewModel.loadDeviceContacts(context)
                                    } else if (!hasPerm && query.length == 1) {
                                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = null,
                                        tint = SplitMateTheme.TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                placeholder = { Text("Search contact by name or phone...", fontSize = 12.sp) },
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
                            if (matchingDeviceContacts.isNotEmpty()) {
                                FilledTonalButton(
                                    onClick = {
                                        val topMatch = matchingDeviceContacts.first()
                                        selectedMembers = selectedMembers + NewGroupMemberDraft(
                                            name = topMatch.name,
                                            cleanPhone = topMatch.cleanPhone
                                        )
                                        contactSearchQuery = ""
                                    },
                                    shape = SplitMateTheme.RadiusButton,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = SplitMateTheme.AccentSage,
                                        contentColor = Color(0xFF23201E)
                                    ),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Icon(Icons.Rounded.PersonAdd, contentDescription = "Add Matched Contact", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Live matching contact suggestions from the user's phonebook
                        if (contactSearchQuery.isNotBlank()) {
                            if (matchingDeviceContacts.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SplitMateTheme.SurfaceMuted)
                                        .border(1.dp, SplitMateTheme.BorderLight, RoundedCornerShape(14.dp))
                                        .padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    matchingDeviceContacts.forEach { matchedContact ->
                                        Surface(
                                            onClick = {
                                                selectedMembers = selectedMembers + NewGroupMemberDraft(
                                                    name = matchedContact.name,
                                                    cleanPhone = matchedContact.cleanPhone
                                                )
                                                contactSearchQuery = ""
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = SplitMateTheme.SurfaceWhite,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(SplitMateTheme.AccentSage),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = extractInitialsFromNameOrSeed(matchedContact.name),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFF23201E)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = matchedContact.name,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SplitMateTheme.PrimaryDark
                                                        )
                                                        Text(
                                                            text = "+91 ${matchedContact.cleanPhone}",
                                                            fontSize = 11.sp,
                                                            color = SplitMateTheme.SageText,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "+ Add",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = SplitMateTheme.SageText
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "No matching contact in phonebook for \"$contactSearchQuery\"",
                                    fontSize = 11.sp,
                                    color = SplitMateTheme.TerracottaText,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }

                    // 6. Action Buttons (Proper 8.dp spacing, End alignment, non-squished height)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showNewGroupDialog = false },
                            modifier = Modifier.height(46.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontWeight = FontWeight.Bold,
                                color = SplitMateTheme.TextSecondary,
                                maxLines = 1
                            )
                        }
                        Button(
                            onClick = {
                                val topMatch = matchingDeviceContacts.firstOrNull()
                                val finalDrafts = if (topMatch != null) {
                                    selectedMembers + NewGroupMemberDraft(
                                        name = topMatch.name,
                                        cleanPhone = topMatch.cleanPhone
                                    )
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
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text(
                                text = "Create Group",
                                color = SplitMateTheme.ScreenBg,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

// ==============================================================================
// TAB 3: SETTLE (Greedy Debt Simplification & Direct UPI + Direct WhatsApp Link)
// ==============================================================================
@Composable
fun GreedySettlementScreen(viewModel: SplitMateViewModel) {
    val context = LocalContext.current
    val settlementPlan by viewModel.settlementPlan.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val deviceContacts by viewModel.deviceContacts.collectAsState()
    val isLoadingContacts by viewModel.isLoadingContacts.collectAsState()

    var targetMemberForContactLink by remember { mutableStateOf<GroupMemberEntity?>(null) }
    var showContactLinkSheet by remember { mutableStateOf(false) }

    val rawDebtEdges = (uiState.expenses.size * uiState.activeGroupMembers.size).coerceAtLeast(settlementPlan.size)

    val directContactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted && targetMemberForContactLink != null) {
            viewModel.loadDeviceContacts(context)
            showContactLinkSheet = true
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
            viewModel.loadDeviceContacts(context)
            showContactLinkSheet = true
        } else {
            directContactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    if (showContactLinkSheet && targetMemberForContactLink != null) {
        val memberToUpdate = targetMemberForContactLink!!
        ContactPickerBottomSheet(
            contacts = deviceContacts,
            isLoading = isLoadingContacts,
            multiSelect = false,
            onDismiss = {
                showContactLinkSheet = false
                targetMemberForContactLink = null
            },
            onConfirmSelection = { chosen ->
                val picked = chosen.firstOrNull()
                if (picked != null && picked.cleanPhone.length == 10) {
                    viewModel.updateFriendUpi(
                        memberId = memberToUpdate.memberId,
                        newName = memberToUpdate.name,
                        newUpiId = "${picked.cleanPhone}@upi",
                        newAvatarSeed = memberToUpdate.avatarSeed
                    )
                }
                showContactLinkSheet = false
                targetMemberForContactLink = null
            }
        )
    }

    val owedToYouTransfers = remember(settlementPlan, uiState.currentUserName) {
        settlementPlan.filter { t ->
            !t.isCurrentUserDebtor && (
                t.toName.equals("You", ignoreCase = true) ||
                    t.toName.equals(uiState.currentUserName, ignoreCase = true) ||
                    uiState.members.find { it.memberId == t.toMemberId }?.isCurrentUser == true
                )
        }
    }
    val youOweTransfers = remember(settlementPlan) {
        settlementPlan.filter { it.isCurrentUserDebtor }
    }
    val peerToPeerByReceiver = remember(settlementPlan, owedToYouTransfers, youOweTransfers) {
        val handledKeys = (owedToYouTransfers + youOweTransfers)
            .map { "${it.fromMemberId}_${it.toMemberId}" }
            .toSet()
        settlementPlan
            .filterNot { handledKeys.contains("${it.fromMemberId}_${it.toMemberId}") }
            .groupBy { it.toName }
    }

    val totalOwedToYouCents = remember(owedToYouTransfers) {
        owedToYouTransfers.sumOf { it.transfer.amountCents }
    }
    val totalYouOweCents = remember(youOweTransfers) {
        youOweTransfers.sumOf { it.transfer.amountCents }
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
                text = "Settle Up & Balances",
                fontFamily = SplitMateTheme.FontDisplay,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SplitMateTheme.PrimaryDark
            )
            Text(
                text = "Simplified balances grouped by member with direct WhatsApp & UPI",
                fontFamily = SplitMateTheme.FontRounded,
                fontSize = 12.sp,
                color = SplitMateTheme.TextSecondary
            )

            if (uiState.groups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                val resolvedActiveGroupId = uiState.activeGroup?.groupId ?: uiState.activeGroupId
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.groups, key = { it.groupId }) { grp ->
                        val isSelected = grp.groupId == resolvedActiveGroupId
                        Surface(
                            onClick = { viewModel.selectActiveGroup(grp.groupId) },
                            shape = SplitMateTheme.RadiusBadge,
                            color = if (isSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.SurfaceWhite,
                            border = BorderStroke(1.dp, if (isSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.BorderLight)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.Groups,
                                    contentDescription = null,
                                    tint = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = grp.name,
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. Summary Overview Banner
        if (settlementPlan.isNotEmpty()) {
            item {
                Card(
                    shape = SplitMateTheme.RadiusPanel,
                    colors = CardDefaults.cardColors(
                        containerColor = if (totalYouOweCents > totalOwedToYouCents) {
                            SplitMateTheme.TerracottaSurface
                        } else {
                            SplitMateTheme.SageSurface
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarToken(
                                initials = uiState.currentUserSeed,
                                bg = SplitMateTheme.SurfaceWhite,
                                textColor = SplitMateTheme.PrimaryDark,
                                size = 40
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (totalOwedToYouCents > 0L) {
                                        "${owedToYouTransfers.size} member(s) owe you"
                                    } else if (totalYouOweCents > 0L) {
                                        "You owe ${youOweTransfers.size} member(s)"
                                    } else {
                                        "${settlementPlan.size} direct settlement(s)"
                                    },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = SplitMateTheme.PrimaryDark
                                )
                                Text(
                                    text = "Simplified from $rawDebtEdges cross-debts into ${settlementPlan.size} payment(s)",
                                    fontSize = 11.sp,
                                    color = SplitMateTheme.TextSecondary
                                )
                            }
                        }
                        if (totalOwedToYouCents > 0L) {
                            Text(
                                text = "+₹${String.format(Locale.US, "%.2f", totalOwedToYouCents / 100.0)}",
                                fontFamily = SplitMateTheme.FontDisplay,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = SplitMateTheme.SageText
                            )
                        } else if (totalYouOweCents > 0L) {
                            Text(
                                text = "-₹${String.format(Locale.US, "%.2f", totalYouOweCents / 100.0)}",
                                fontFamily = SplitMateTheme.FontDisplay,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = SplitMateTheme.TerracottaText
                            )
                        }
                    }
                }
            }
        }

        // 2. SECTION A: FRIENDS WHO OWE YOU (Single Unified Card — Receiver "You" Shown Once!)
        if (owedToYouTransfers.isNotEmpty()) {
            item {
                Card(
                    shape = SplitMateTheme.RadiusCard,
                    colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Friends Who Owe You",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SplitMateTheme.PrimaryDark
                                )
                                Text(
                                    text = "Send a WhatsApp reminder or mark as paid once received",
                                    fontSize = 11.sp,
                                    color = SplitMateTheme.TextSecondary
                                )
                            }
                            Surface(
                                shape = SplitMateTheme.RadiusBadge,
                                color = SplitMateTheme.SageSurface
                            ) {
                                Text(
                                    text = "${owedToYouTransfers.size} Pending",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SplitMateTheme.SageText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        owedToYouTransfers.forEachIndexed { idx, transfer ->
                            val fromRoomMember = uiState.members.find { it.memberId == transfer.fromMemberId }
                            val debtorPhone = fromRoomMember?.upiId?.substringBefore('@')?.replace(Regex("[^0-9]"), "") ?: ""
                            if (idx > 0) {
                                HorizontalDivider(
                                    color = SplitMateTheme.BorderLight,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AvatarToken(
                                            initials = transfer.fromSeed,
                                            bg = SplitMateTheme.SageSurface,
                                            textColor = SplitMateTheme.SageText,
                                            size = 38
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = transfer.fromName,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = SplitMateTheme.PrimaryDark
                                            )
                                            Text(
                                                text = "Owes you ${transfer.formattedDisplayAmount}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SplitMateTheme.SageText
                                            )
                                        }
                                    }

                                    Text(
                                        text = "+${transfer.formattedDisplayAmount}",
                                        fontFamily = SplitMateTheme.FontDisplay,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = SplitMateTheme.SageText
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (debtorPhone.length == 10) {
                                        OutlinedButton(
                                            onClick = {
                                                val whatsappUri = Uri.parse(
                                                    "https://api.whatsapp.com/send?phone=91" +
                                                        debtorPhone +
                                                        "&text=" +
                                                        Uri.encode(
                                                            "Hey ${transfer.fromName}, friendly reminder for your ₹${transfer.amount} share on SplitMate."
                                                        )
                                                )
                                                context.startActivity(Intent(Intent.ACTION_VIEW, whatsappUri))
                                            },
                                            shape = SplitMateTheme.RadiusButton,
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SplitMateTheme.PrimaryDark),
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .height(42.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.Chat,
                                                contentDescription = null,
                                                tint = SplitMateTheme.PrimaryDark,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Remind on WhatsApp",
                                                fontFamily = SplitMateTheme.FontRounded,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                    } else {
                                        FilledTonalButton(
                                            onClick = { launchContactPickerForMember(fromRoomMember) },
                                            shape = SplitMateTheme.RadiusButton,
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = SplitMateTheme.SurfaceMuted,
                                                contentColor = SplitMateTheme.PrimaryDark
                                            ),
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .height(42.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Contacts,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Link Phone",
                                                fontFamily = SplitMateTheme.FontRounded,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { viewModel.markGreedyTransferSettled(transfer.transfer) },
                                        shape = SplitMateTheme.RadiusButton,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SplitMateTheme.PrimaryDark,
                                            contentColor = SplitMateTheme.ScreenBg
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Mark Paid",
                                            fontFamily = SplitMateTheme.FontRounded,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. SECTION B: PAYMENTS YOU NEED TO MAKE (When You Owe Someone — Shows Pay via UPI!)
        if (youOweTransfers.isNotEmpty()) {
            item {
                Card(
                    shape = SplitMateTheme.RadiusCard,
                    colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Payments You Need to Make",
                            fontFamily = SplitMateTheme.FontDisplay,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateTheme.PrimaryDark
                        )
                        Text(
                            text = "Pay directly via UPI or mark as settled",
                            fontSize = 11.sp,
                            color = SplitMateTheme.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        youOweTransfers.forEachIndexed { idx, transfer ->
                            val toRoomMember = uiState.members.find { it.memberId == transfer.toMemberId }
                            if (idx > 0) {
                                HorizontalDivider(
                                    color = SplitMateTheme.BorderLight,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AvatarToken(
                                            initials = transfer.toSeed,
                                            bg = SplitMateTheme.TerracottaSurface,
                                            textColor = SplitMateTheme.TerracottaText,
                                            size = 38
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = transfer.toName,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = SplitMateTheme.PrimaryDark
                                            )
                                            Text(
                                                text = "You owe ${transfer.formattedDisplayAmount}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SplitMateTheme.TerracottaText
                                            )
                                        }
                                    }

                                    Text(
                                        text = "-${transfer.formattedDisplayAmount}",
                                        fontFamily = SplitMateTheme.FontDisplay,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = SplitMateTheme.TerracottaText
                                    )
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
                                                    "upi://pay?pa=${transfer.cleanPhone}@upi&pn=" +
                                                        Uri.encode(transfer.toName) +
                                                        "&am=" + transfer.amount +
                                                        "&cu=INR"
                                                )
                                                val intent = Intent(Intent.ACTION_VIEW, upiUri)
                                                context.startActivity(Intent.createChooser(intent, "Pay with UPI"))
                                            },
                                            shape = SplitMateTheme.RadiusButton,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SplitMateTheme.PrimaryDark,
                                                contentColor = SplitMateTheme.ScreenBg
                                            ),
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .height(42.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Pay via UPI",
                                                fontFamily = SplitMateTheme.FontRounded,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    } else {
                                        FilledTonalButton(
                                            onClick = { launchContactPickerForMember(toRoomMember) },
                                            shape = SplitMateTheme.RadiusButton,
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = SplitMateTheme.AccentSage,
                                                contentColor = Color(0xFF23201E)
                                            ),
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .height(42.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Contacts,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Link Phone for UPI",
                                                fontFamily = SplitMateTheme.FontRounded,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.markGreedyTransferSettled(transfer.transfer) },
                                        shape = SplitMateTheme.RadiusButton,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SplitMateTheme.PrimaryDark),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Text(
                                            text = "Mark Paid",
                                            fontFamily = SplitMateTheme.FontRounded,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. SECTION C: BETWEEN OTHER MEMBERS (Grouped by Receiver so Receiver Names Never Duplicate!)
        peerToPeerByReceiver.forEach { (receiverName, transfersForReceiver) ->
            item(key = "receiver_$receiverName") {
                val firstTransfer = transfersForReceiver.first()
                Card(
                    shape = SplitMateTheme.RadiusCard,
                    colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarToken(
                                initials = firstTransfer.toSeed,
                                bg = SplitMateTheme.SageSurface,
                                textColor = SplitMateTheme.SageText,
                                size = 34
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Payments to $receiverName",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SplitMateTheme.PrimaryDark
                                )
                                Text(
                                    text = "${transfersForReceiver.size} member(s) settling with $receiverName",
                                    fontSize = 11.sp,
                                    color = SplitMateTheme.TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        transfersForReceiver.forEachIndexed { idx, transfer ->
                            if (idx > 0) {
                                HorizontalDivider(
                                    color = SplitMateTheme.BorderLight,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AvatarToken(
                                        initials = transfer.fromSeed,
                                        bg = SplitMateTheme.SurfaceMuted,
                                        textColor = SplitMateTheme.PrimaryDark,
                                        size = 30
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = transfer.fromName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = SplitMateTheme.PrimaryDark
                                        )
                                        Text(
                                            text = "Pays ${transfer.formattedDisplayAmount}",
                                            fontSize = 11.sp,
                                            color = SplitMateTheme.TextSecondary
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { viewModel.markGreedyTransferSettled(transfer.transfer) },
                                    shape = SplitMateTheme.RadiusButton,
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = "Mark Paid",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SplitMateTheme.PrimaryDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Settled Equilibrium State
        if (settlementPlan.isEmpty()) {
            item {
                Card(
                    shape = SplitMateTheme.RadiusCard,
                    colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SageSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
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
                        Text("Zero open balances across group members", fontSize = 12.sp, color = SplitMateTheme.TextSecondary)
                    }
                }
            }
        }

        // 6. PERMANENT SETTLED & PAID HISTORY RECORD (Kept as a permanent record instead of vanishing!)
        val resolvedActiveGroupIdForSettlements = uiState.activeGroup?.groupId ?: uiState.activeGroupId
        val recordedSettlements = uiState.settlements.filter { it.groupId == resolvedActiveGroupIdForSettlements }
        item {
            Card(
                shape = SplitMateTheme.RadiusCard,
                colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SplitMateTheme.SageSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.TaskAlt,
                                    contentDescription = null,
                                    tint = SplitMateTheme.SageText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Settled & Paid History",
                                    fontFamily = SplitMateTheme.FontDisplay,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SplitMateTheme.PrimaryDark
                                )
                                Text(
                                    text = "Completed payments stay recorded here for group transparency",
                                    fontSize = 11.sp,
                                    color = SplitMateTheme.TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = SplitMateTheme.RadiusBadge,
                            color = SplitMateTheme.SageSurface
                        ) {
                            Text(
                                text = "${recordedSettlements.size} Paid",
                                fontFamily = SplitMateTheme.FontRounded,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SplitMateTheme.SageText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (recordedSettlements.isEmpty()) {
                        Surface(
                            shape = SplitMateTheme.RadiusPanel,
                            color = SplitMateTheme.SurfaceMuted,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = SplitMateTheme.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "When you tap 'Mark Paid' on any settlement above, it will be saved here as a permanent receipt record instead of vanishing.",
                                    fontSize = 11.sp,
                                    color = SplitMateTheme.TextSecondary
                                )
                            }
                        }
                    } else {
                        val dateFormatter = remember {
                            java.text.SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
                        }
                        recordedSettlements.forEachIndexed { idx, settlement ->
                            val fromMbr = uiState.members.find { it.memberId == settlement.fromMemberId }
                            val toMbr = uiState.members.find { it.memberId == settlement.toMemberId }
                            val fromLabel = if (fromMbr?.isCurrentUser == true) "You (${fromMbr.name})" else (fromMbr?.name ?: "Member")
                            val toLabel = if (toMbr?.isCurrentUser == true) "You (${toMbr.name})" else (toMbr?.name ?: "Member")
                            val formattedPaid = "₹${String.format(Locale.US, "%.2f", settlement.amountCents / 100.0)}"
                            val formattedDate = remember(settlement.settledAt) {
                                dateFormatter.format(java.util.Date(settlement.settledAt))
                            }

                            if (idx > 0) {
                                HorizontalDivider(
                                    color = SplitMateTheme.BorderLight,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AvatarToken(
                                        initials = fromMbr?.avatarSeed ?: fromLabel.take(2),
                                        bg = SplitMateTheme.SageSurface,
                                        textColor = SplitMateTheme.SageText,
                                        size = 34
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "$fromLabel → $toLabel",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = SplitMateTheme.PrimaryDark
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = SplitMateTheme.RadiusBadge,
                                                color = SplitMateTheme.SageSurface
                                            ) {
                                                Text(
                                                    text = "✓ PAID",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = SplitMateTheme.SageText,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Settled on $formattedDate",
                                            fontSize = 11.sp,
                                            color = SplitMateTheme.TextSecondary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = formattedPaid,
                                        fontFamily = SplitMateTheme.FontDisplay,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = SplitMateTheme.SageText,
                                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        onClick = { viewModel.undoSettlement(settlement.settlementId) },
                                        shape = SplitMateTheme.RadiusBadge,
                                        color = SplitMateTheme.SurfaceMuted
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.Undo,
                                                contentDescription = "Undo Paid Mark",
                                                tint = SplitMateTheme.PrimaryDark,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Undo",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SplitMateTheme.PrimaryDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// TAB 4: ACTIVITY & HISTORY (Interactive Expense Breakdown + Group Filters)
// ==============================================================================
@Composable
fun AuditVaultScreen(viewModel: SplitMateViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val sym = "₹"
    var selectedGroupFilterId by remember { mutableStateOf<String?>(null) }
    var showFilterBar by remember { mutableStateOf(true) }
    var expandedExpenseId by remember { mutableStateOf<String?>(null) }
    var editingExpenseEntity by remember { mutableStateOf<com.splitmate.app.data.ExpenseEntity?>(null) }

    val filteredExpenses = remember(uiState.expenses, selectedGroupFilterId) {
        if (selectedGroupFilterId == null) {
            uiState.expenses
        } else {
            uiState.expenses.filter { it.groupId == selectedGroupFilterId }
        }
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
                text = "Activity & History",
                fontFamily = SplitMateTheme.FontDisplay,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SplitMateTheme.PrimaryDark
            )
            Text(
                text = "Tap any transaction to inspect its breakdown or edit details",
                fontFamily = SplitMateTheme.FontRounded,
                fontSize = 12.sp,
                color = SplitMateTheme.TextSecondary
            )
        }

        // 1. Interactive Expense History Card (Toggles Group Filter Pills)
        item {
            Card(
                onClick = { showFilterBar = !showFilterBar },
                shape = SplitMateTheme.RadiusPanel,
                colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusPanel)
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(SplitMateTheme.AccentSage),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.History, contentDescription = null, tint = Color(0xFF23201E))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Expense History", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SplitMateTheme.PrimaryDark)
                                Text("Tap to filter by group · Tap any expense to inspect or edit", fontSize = 11.sp, color = SplitMateTheme.TextSecondary)
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
                                Text("${filteredExpenses.size} Recorded", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SplitMateTheme.SageText)
                            }
                        }
                    }

                    if (showFilterBar && uiState.groups.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                Surface(
                                    onClick = { selectedGroupFilterId = null },
                                    shape = SplitMateTheme.RadiusBadge,
                                    color = if (selectedGroupFilterId == null) SplitMateTheme.PrimaryDark else SplitMateTheme.SurfaceMuted
                                ) {
                                    Text(
                                        text = "All Groups (${uiState.expenses.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedGroupFilterId == null) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            items(uiState.groups, key = { it.groupId }) { grp ->
                                val isSelected = selectedGroupFilterId == grp.groupId
                                val count = uiState.expenses.count { it.groupId == grp.groupId }
                                Surface(
                                    onClick = { selectedGroupFilterId = if (isSelected) null else grp.groupId },
                                    shape = SplitMateTheme.RadiusBadge,
                                    color = if (isSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.SurfaceMuted
                                ) {
                                    Text(
                                        text = "${grp.name} ($count)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Transaction Feed
        item {
            Text(
                text = "Recent Expenses (Tap card for breakdown or edit)",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SplitMateTheme.TextSecondary
            )
        }

        if (filteredExpenses.isEmpty()) {
            item {
                Card(
                    shape = SplitMateTheme.RadiusCard,
                    colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null, tint = SplitMateTheme.SageText, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No expenses recorded yet", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SplitMateTheme.PrimaryDark)
                        Text("Expenses you split will appear here with full receipt breakdowns.", fontSize = 12.sp, color = SplitMateTheme.TextSecondary)
                    }
                }
            }
        }

        items(filteredExpenses, key = { it.expenseId }) { expense ->
            val payer = uiState.members.find { it.memberId == expense.payerId }
            val group = uiState.groups.find { it.groupId == expense.groupId }
            val groupMembers = uiState.members.filter { it.groupId == expense.groupId }
            val memberCount = groupMembers.size.coerceAtLeast(1)
            val perPersonShare = "$sym${String.format(Locale.US, "%.2f", (expense.totalAmountCents / memberCount) / 100.0)}"
            val isMePayer = payer?.isCurrentUser == true
            val formattedTotal = "$sym${String.format(Locale.US, "%.2f", expense.totalAmountCents / 100.0)}"
            val isExpanded = expandedExpenseId == expense.expenseId
            val parsedTravelTicket = remember(expense.title) {
                extractTravelTicketFromTitle(expense.title)
            }
            val displayExpenseTitle = remember(expense.title, parsedTravelTicket) {
                parsedTravelTicket?.cleanTitle ?: expense.title
            }

            Card(
                onClick = {
                    expandedExpenseId = if (isExpanded) null else expense.expenseId
                },
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
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isMePayer) Color(0xFFE0E7FF) else Color(0xFFFFE4E6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (parsedTravelTicket != null) Icons.Rounded.Train else Icons.Rounded.Dining,
                                    contentDescription = null,
                                    tint = if (isMePayer) Color(0xFF3730A3) else SplitMateTheme.TerracottaText
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(displayExpenseTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SplitMateTheme.PrimaryDark)
                                Text(
                                    text = "${group?.name ?: "Group"} · Paid by ${payer?.name ?: "You"}",
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
                            Text(
                                text = if (isExpanded) "Hide Breakdown ▲" else "Tap for Breakdown ▼",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitMateTheme.SageText
                            )
                        }
                    }

                    if (parsedTravelTicket != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        GroupBoardingPassCard(ticket = parsedTravelTicket)
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = SplitMateTheme.BorderLight)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Individual Share Breakdown (${groupMembers.size} members)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitMateTheme.PrimaryDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        groupMembers.forEach { mbr ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (mbr.isCurrentUser) "${mbr.name} (You)" else mbr.name,
                                    fontSize = 12.sp,
                                    color = SplitMateTheme.TextSecondary
                                )
                                Text(
                                    text = perPersonShare,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SplitMateTheme.PrimaryDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = SplitMateTheme.BorderLight)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$perPersonShare / person",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SplitMateTheme.TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = { editingExpenseEntity = expense },
                                shape = SplitMateTheme.RadiusBadge,
                                color = SplitMateTheme.SageSurface
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = SplitMateTheme.SageText)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SplitMateTheme.SageText)
                                }
                            }
                            Surface(
                                onClick = { viewModel.rollbackExpense(expense.expenseId) },
                                shape = SplitMateTheme.RadiusBadge,
                                color = SplitMateTheme.SurfaceMuted
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.size(14.dp), tint = SplitMateTheme.PrimaryDark)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Undo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SplitMateTheme.PrimaryDark)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingExpenseEntity != null) {
        val exp = editingExpenseEntity!!
        val expGroupMembers = uiState.members.filter { it.groupId == exp.groupId }
        EditLoggedExpenseDialog(
            expense = exp,
            groupMembers = expGroupMembers,
            onDismiss = { editingExpenseEntity = null },
            onSave = { newTitle, newRupees, newPayerId ->
                viewModel.editExistingExpense(
                    expenseId = exp.expenseId,
                    newTitle = newTitle,
                    newTotalRupees = newRupees,
                    newPayerId = newPayerId
                )
                editingExpenseEntity = null
            }
        )
    }
}

// ==============================================================================
// DIALOG FOR EDITING AN ALREADY-LOGGED EXPENSE (Category, Amount, Payer, PNR)
// ==============================================================================
@Composable
fun EditLoggedExpenseDialog(
    expense: com.splitmate.app.data.ExpenseEntity,
    groupMembers: List<com.splitmate.app.data.GroupMemberEntity>,
    onDismiss: () -> Unit,
    onSave: (String, Double, String) -> Unit
) {
    val existingTicket = remember(expense.title) { extractTravelTicketFromTitle(expense.title) }
    var editedTitle by remember(expense.title) {
        mutableStateOf(existingTicket?.cleanTitle ?: expense.title)
    }
    var editedAmountStr by remember(expense.totalAmountCents) {
        val rupees = expense.totalAmountCents / 100.0
        mutableStateOf(if (rupees % 1.0 == 0.0) rupees.toLong().toString() else String.format(Locale.US, "%.2f", rupees))
    }
    var selectedPayerId by remember(expense.payerId) {
        mutableStateOf(expense.payerId)
    }
    var includeTravelTicket by remember(existingTicket) {
        mutableStateOf(existingTicket != null)
    }
    var pnrNumber by remember(existingTicket) { mutableStateOf(existingTicket?.pnr ?: "") }
    var trainOrFlightNo by remember(existingTicket) { mutableStateOf(existingTicket?.trainOrFlightNo ?: "") }
    var routeFromTo by remember(existingTicket) { mutableStateOf(existingTicket?.route ?: "") }
    var departureInfo by remember(existingTicket) { mutableStateOf(existingTicket?.departureInfo ?: "") }
    var coachAndSeats by remember(existingTicket) { mutableStateOf(existingTicket?.coachAndSeats ?: "") }

    val quickCategories = listOf(
        "🚆 Train / PNR",
        "🍽️ Dinner & Food",
        "🚕 Cab & Auto",
        "🏨 Hotel & Stay",
        "🛒 Groceries",
        "🎉 Drinks & Outing"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SplitMateTheme.SurfaceWhite,
        shape = SplitMateTheme.RadiusCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = SplitMateTheme.SageText,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Logged Expense",
                    fontFamily = SplitMateTheme.FontDisplay,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    color = SplitMateTheme.PrimaryDark
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Quick Category Switch:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SplitMateTheme.TextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickCategories) { cat ->
                        val isTrainCat = cat.contains("Train")
                        val isSelected = editedTitle.equals(cat, ignoreCase = true)
                        Surface(
                            onClick = {
                                editedTitle = cat
                                if (isTrainCat) includeTravelTicket = true
                            },
                            shape = SplitMateTheme.RadiusBadge,
                            color = if (isSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.SurfaceMuted
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Expense Title / Category", fontSize = 12.sp) },
                    singleLine = true,
                    shape = SplitMateTheme.RadiusPanel,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editedAmountStr,
                    onValueChange = { editedAmountStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Total Amount (₹)", fontSize = 12.sp) },
                    singleLine = true,
                    shape = SplitMateTheme.RadiusPanel,
                    modifier = Modifier.fillMaxWidth()
                )

                if (groupMembers.isNotEmpty()) {
                    Text(
                        text = "Paid By:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SplitMateTheme.TextSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(groupMembers, key = { it.memberId }) { mbr ->
                            val isSelected = mbr.memberId == selectedPayerId
                            Surface(
                                onClick = { selectedPayerId = mbr.memberId },
                                shape = SplitMateTheme.RadiusBadge,
                                color = if (isSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.SurfaceMuted
                            ) {
                                Text(
                                    text = if (mbr.isCurrentUser) "${mbr.name} (You)" else mbr.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚆 Attach / Edit Train PNR & Berths",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SplitMateTheme.PrimaryDark
                    )
                    Switch(
                        checked = includeTravelTicket,
                        onCheckedChange = { includeTravelTicket = it }
                    )
                }

                if (includeTravelTicket) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = pnrNumber,
                            onValueChange = { pnrNumber = it.filter { ch -> ch.isDigit() }.take(10) },
                            label = { Text("10-Digit PNR", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = trainOrFlightNo,
                            onValueChange = { trainOrFlightNo = it },
                            label = { Text("Train #", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = routeFromTo,
                            onValueChange = { routeFromTo = it },
                            label = { Text("From → To", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = coachAndSeats,
                            onValueChange = { coachAndSeats = it },
                            label = { Text("Coach / Berths", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedRupees = editedAmountStr.toDoubleOrNull() ?: (expense.totalAmountCents / 100.0)
                    val finalTitle = if (includeTravelTicket && (pnrNumber.isNotBlank() || trainOrFlightNo.isNotBlank() || coachAndSeats.isNotBlank())) {
                        val parsedFrom = routeFromTo.substringBefore("→").substringBefore("-").trim()
                        val parsedTo = routeFromTo.substringAfter("→", routeFromTo.substringAfter("-", "")).trim()
                        val enriched = enrichTicketWithOfflineCatalog(
                            ParsedTravelTicket(
                                pnr = pnrNumber.trim(),
                                trainOrFlightNo = trainOrFlightNo.trim(),
                                fromStation = parsedFrom,
                                toStation = parsedTo,
                                departureTime = departureInfo.trim(),
                                coachAndSeats = coachAndSeats.trim(),
                                cleanTitle = editedTitle.trim().ifBlank { "🚆 Train / PNR Ticket" }
                            )
                        )
                        formatTravelExpenseTitle(editedTitle.trim().ifBlank { "🚆 Train / PNR Ticket" }, enriched)
                    } else {
                        editedTitle.trim().ifBlank { "Group Expense" }
                    }
                    onSave(finalTitle, parsedRupees, selectedPayerId)
                },
                shape = SplitMateTheme.RadiusButton,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SplitMateTheme.PrimaryDark,
                    contentColor = SplitMateTheme.ScreenBg
                )
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Bold, color = SplitMateTheme.TextSecondary)
            }
        }
    )
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
