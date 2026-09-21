package com.splitmate.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.SplitMateMathEngine
import java.util.Locale

enum class NativeTab(val title: String) {
    SPLIT_MATRIX("Split Matrix"),
    LEDGERS("Ledgers"),
    SETTLE_UP("Settle Up"),
    CURRENCIES("Currencies")
}

fun formatCents(cents: Long, symbol: String): String {
    val sign = if (cents < 0) "-" else ""
    val abs = kotlin.math.abs(cents) / 100.0
    return String.format(Locale.US, "%s%s%.2f", sign, symbol, abs)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitMateNativeApp(
    viewModel: SplitMateViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(NativeTab.SPLIT_MATRIX) }
    var showNumPadDialog by rememberSaveable { mutableStateOf(false) }
    var showNewGroupDialog by rememberSaveable { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Compose Hygiene: derivedStateOf for scroll-triggered HorizontalFloatingToolbar collapse
    val isToolbarCollapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40
        }
    }

    // Compose Hygiene: derivedStateOf for Live Proportional Auxiliary Multiplier & Remainder Engine
    val liveProportionalResult by remember(
        uiState.receiptItems,
        uiState.receiptTaxPercent,
        uiState.receiptTipPercent,
        uiState.receiptPayerId,
        uiState.members,
        uiState.activeGroupId
    ) {
        derivedStateOf {
            val groupMembers = uiState.activeGroupMembers
            val baseSubtotal = uiState.receiptItems.sumOf { it.priceCents }
            val taxCents = Math.round(baseSubtotal * (uiState.receiptTaxPercent / 100.0))
            val tipCents = Math.round(baseSubtotal * (uiState.receiptTipPercent / 100.0))

            val memberBaseMap = groupMembers.associate { it.memberId to 0L }.toMutableMap()
            uiState.receiptItems.forEach { item ->
                val validClaimers = item.claimedByMemberIds.filter { memberBaseMap.containsKey(it) }
                if (validClaimers.isNotEmpty()) {
                    val shares = SplitMateMathEngine.splitEquallyZeroDrift(
                        item.priceCents,
                        validClaimers.map { it to it }
                    )
                    shares.forEach { s ->
                        memberBaseMap[s.memberId] = (memberBaseMap[s.memberId] ?: 0L) + s.finalCents
                    }
                }
            }

            SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = baseSubtotal,
                taxCents = taxCents,
                tipCents = tipCents,
                payerId = uiState.receiptPayerId,
                memberBaseClaimsCents = groupMembers.map { m ->
                    Triple(m.memberId, m.name, memberBaseMap[m.memberId] ?: 0L)
                },
                attributeRemainderToPayer = true
            )
        }
    }

    // Compose Hygiene: derivedStateOf for Greedy Minimum Cash Flow Debt Simplification
    val greedySettlementPlan by remember(
        uiState.expenses,
        uiState.splits,
        uiState.settlements,
        uiState.activeGroupId,
        uiState.members
    ) {
        derivedStateOf {
            val groupMembers = uiState.activeGroupMembers
            val netMap = groupMembers.associate { it.memberId to 0L }.toMutableMap()
            val groupExpenses = uiState.expenses.filter { it.groupId == uiState.activeGroupId }
            val expenseIds = groupExpenses.map { it.expenseId }.toSet()

            groupExpenses.forEach { exp ->
                netMap[exp.payerId] = (netMap[exp.payerId] ?: 0L) + exp.totalAmountCents
            }
            uiState.splits.filter { it.expenseId in expenseIds }.forEach { sp ->
                netMap[sp.memberId] = (netMap[sp.memberId] ?: 0L) - sp.finalOwedCents
            }
            uiState.settlements.filter { it.groupId == uiState.activeGroupId }.forEach { st ->
                netMap[st.fromMemberId] = (netMap[st.fromMemberId] ?: 0L) + st.amountCents
                netMap[st.toMemberId] = (netMap[st.toMemberId] ?: 0L) - st.amountCents
            }

            val balances = groupMembers.map { m ->
                SplitMateMathEngine.MemberNetBalance(
                    memberId = m.memberId,
                    displayName = m.name,
                    netCents = netMap[m.memberId] ?: 0L
                )
            }
            val transfers = SplitMateMathEngine.simplifyDebtsGreedy(balances)
            balances to transfers
        }
    }

    val currencySymbol = uiState.activeCurrency.symbol

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BuckwheatCanvas,
                    titleContentColor = BuckwheatCharcoal
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DiceBearAvatar(
                            seed = uiState.currentUserSeed,
                            contentDescription = "User Profile Avatar",
                            size = 38.dp
                        )
                        Column {
                            Text(
                                text = "SplitMate",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp
                            )
                            Text(
                                text = "${uiState.activeCurrency.currencyCode} (${uiState.activeCurrency.symbol}) · Rate ${String.format(Locale.US, "%.2f", uiState.activeCurrency.rateFromBase)}",
                                fontSize = 11.sp,
                                color = BuckwheatSecondaryText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showNewGroupDialog = true },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BuckwheatSageContainer,
                            contentColor = BuckwheatOlivePrimary
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("TopBarNewGroupBtn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Group")
                        Spacer(Modifier.width(4.dp))
                        Text("Group", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Material 3 Expressive HorizontalFloatingToolbar on the Collaborative Split Matrix screen
                if (selectedTab == NativeTab.SPLIT_MATRIX) {
                    HorizontalFloatingToolbar(
                        collapsed = isToolbarCollapsed,
                        currencySymbol = currencySymbol,
                        onQuickAddCents = { cents ->
                            viewModel.addReceiptLineItem("Quick Add +${formatCents(cents, currencySymbol)}", cents)
                        },
                        onApplyEighteenPercentTip = {
                            viewModel.updateReceiptTaxAndTip(uiState.receiptTaxPercent, 18.0)
                        },
                        onOpenNumPadSheet = { showNumPadDialog = true },
                        onCommitExpense = { viewModel.commitCollaborativeExpense() }
                    )
                }

                NavigationBar(
                    containerColor = BuckwheatSurface,
                    tonalElevation = 4.dp
                ) {
                    NativeTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("NavTab_${tab.name}"),
                            icon = {
                                val icon = when (tab) {
                                    NativeTab.SPLIT_MATRIX -> Icons.Default.ViewModule
                                    NativeTab.LEDGERS -> Icons.Default.ReceiptLong
                                    NativeTab.SETTLE_UP -> Icons.Default.AccountBalance
                                    NativeTab.CURRENCIES -> Icons.Default.CurrencyExchange
                                }
                                Icon(icon, contentDescription = tab.title)
                            },
                            label = {
                                Text(tab.title, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Physics-based spring transition between tabs
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(spring(stiffness = Spring.StiffnessMediumLow))
                    .togetherWith(fadeOut(spring(stiffness = Spring.StiffnessMedium)))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BuckwheatCanvas),
            label = "MainTabSpringTransition"
        ) { currentTab ->
            when (currentTab) {
                NativeTab.SPLIT_MATRIX -> CollaborativeSplitMatrixScreen(
                    uiState = uiState,
                    proportionalResult = liveProportionalResult,
                    listState = listState,
                    onSelectGroup = viewModel::selectActiveGroup,
                    onSelectPersona = viewModel::setClaimerPersona,
                    onToggleItemClaim = viewModel::toggleReceiptItemClaim,
                    onSplitRemainderEqually = viewModel::splitUnassignedRemainderEqually,
                    onTaxTipChanged = viewModel::updateReceiptTaxAndTip
                )

                NativeTab.LEDGERS -> ExpenseLedgersScreen(
                    uiState = uiState,
                    onSelectGroup = viewModel::selectActiveGroup,
                    onOpenNumPad = { showNumPadDialog = true }
                )

                NativeTab.SETTLE_UP -> GreedySettleUpScreen(
                    uiState = uiState,
                    balances = greedySettlementPlan.first,
                    transfers = greedySettlementPlan.second,
                    onMarkTransferSettled = viewModel::markGreedyTransferSettled
                )

                NativeTab.CURRENCIES -> LiveCurrenciesAndProfileScreen(
                    uiState = uiState,
                    onSyncLiveRates = { viewModel.syncLiveCurrencyRatesFromFrankfurter("USD") },
                    onToggleOfflineMode = viewModel::setOfflineMode,
                    onSelectCurrency = { code -> viewModel.updateUserProfile(uiState.currentUserName, code) },
                    onUpdateUserName = { name -> viewModel.updateUserProfile(name, uiState.activeCurrencyCode) }
                )
            }
        }
    }

    if (showNumPadDialog) {
        TactileNumPadDialog(
            currencySymbol = currencySymbol,
            onDismiss = { showNumPadDialog = false },
            onSubmitItem = { title, cents ->
                viewModel.addReceiptLineItem(title, cents)
                showNumPadDialog = false
            }
        )
    }

    if (showNewGroupDialog) {
        NewGroupDialog(
            defaultCurrency = uiState.activeCurrencyCode,
            onDismiss = { showNewGroupDialog = false },
            onCreateGroup = { name, friendsCsv ->
                viewModel.createNewGroup(name, uiState.activeCurrencyCode, friendsCsv)
                showNewGroupDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaborativeSplitMatrixScreen(
    uiState: SplitMateUiState,
    proportionalResult: SplitMateMathEngine.ProportionalSplitResult,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSelectGroup: (String) -> Unit,
    onSelectPersona: (String) -> Unit,
    onToggleItemClaim: (String, String) -> Unit,
    onSplitRemainderEqually: () -> Unit,
    onTaxTipChanged: (Double, Double) -> Unit
) {
    val symbol = uiState.activeCurrency.symbol
    val groupMembers = uiState.activeGroupMembers

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("SplitMatrixLazyColumn")
    ) {
        // 1. Group Switcher Row
        item(key = "header_groups") {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.groups.forEach { group ->
                    FilterChip(
                        selected = group.groupId == uiState.activeGroupId,
                        onClick = { onSelectGroup(group.groupId) },
                        label = { Text(group.name, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    )
                }
            }
        }

        // 2. Receipt Header & Locked Multiplier Card
        item(key = "header_multiplier") {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = BuckwheatSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BuckwheatBorder, RoundedCornerShape(26.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(uiState.receiptTitle, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text(
                                "Base ${formatCents(proportionalResult.baseSubtotalCents, symbol)} + Tax/Tip ${formatCents(proportionalResult.taxCents + proportionalResult.tipCents, symbol)}",
                                fontSize = 12.sp,
                                color = BuckwheatSecondaryText
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatCents(proportionalResult.totalFinalCents, symbol),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = BuckwheatOlivePrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = BuckwheatSageContainer
                            ) {
                                Text(
                                    text = String.format(Locale.US, "Locked m = %.4f×", proportionalResult.lockedMultiplier),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BuckwheatOlivePrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Tax & Tip Quick Controls (48dp touch targets)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.0 to "0% Tip", 10.0 to "10% Tip", 18.0 to "18% Tip").forEach { (tip, label) ->
                            OutlinedButton(
                                onClick = { onTaxTipChanged(uiState.receiptTaxPercent, tip) },
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. In-App Persona Switcher (`Claiming As:`) with Coil SVG Avatars
        item(key = "persona_switcher") {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BuckwheatSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "CLAIMING AS (TAP PERSONA TO ASSIGN ITEMS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BuckwheatSecondaryText
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        proportionalResult.allocations.forEach { alloc ->
                            val member = groupMembers.find { it.memberId == alloc.memberId }
                            val isSelected = uiState.activeClaimerPersonaId == alloc.memberId
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (isSelected) BuckwheatCharcoal else BuckwheatSunken,
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .clickable { onSelectPersona(alloc.memberId) }
                                    .testTag("PersonaChip_${alloc.memberId}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    DiceBearAvatar(
                                        seed = member?.avatarSeed ?: alloc.displayName,
                                        contentDescription = alloc.displayName,
                                        size = 30.dp
                                    )
                                    Column {
                                        Text(
                                            text = if (alloc.plusOneCent) "${alloc.displayName} (+1¢)" else alloc.displayName,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else BuckwheatCharcoal
                                        )
                                        Text(
                                            text = formatCents(alloc.finalCents, symbol),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isSelected) BuckwheatSageContainer else BuckwheatSecondaryText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Real-Time Remainder Engine Alert Card
        item(key = "remainder_engine_banner") {
            if (proportionalResult.unassignedBaseCents > 0L) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BuckwheatPeachContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("UnassignedRemainderCard")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unassigned Balance: ${formatCents(proportionalResult.unassignedFinalCents, symbol)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = BuckwheatTerracottaDark
                            )
                            Text(
                                text = "Base ${formatCents(proportionalResult.unassignedBaseCents, symbol)} temporarily held on Payer",
                                fontSize = 12.sp,
                                color = BuckwheatTerracottaDark
                            )
                        }
                        Button(
                            onClick = onSplitRemainderEqually,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BuckwheatTerracotta,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("SplitRemainderEquallyBtn")
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Split Equally", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BuckwheatSageContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ZeroRemainderVerifiedCard")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BuckwheatOlivePrimary)
                        Text(
                            text = "100% Receipt Allocated · 0.00¢ Rounding Drift Verified",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = BuckwheatOlivePrimary
                        )
                    }
                }
            }
        }

        // 5. Itemized Receipt Matrix (key = { it.itemId } for zero unnecessary recomposition)
        items(
            items = uiState.receiptItems,
            key = { it.itemId }
        ) { item ->
            val isUnclaimed = item.claimedByMemberIds.isEmpty()
            val itemWithTax = Math.round(item.priceCents * proportionalResult.lockedMultiplier)

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnclaimed) Color(0xFFFFF8F5) else BuckwheatSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minWidth = 48.dp, minHeight = 64.dp)
                    .border(
                        width = 1.dp,
                        color = if (isUnclaimed) BuckwheatTerracotta else BuckwheatBorder,
                        shape = RoundedCornerShape(22.dp)
                    )
                    .clickable { onToggleItemClaim(item.itemId, uiState.activeClaimerPersonaId) }
                    .testTag("ReceiptItem_${item.itemId}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            groupMembers.forEach { m ->
                                val claimed = m.memberId in item.claimedByMemberIds
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (claimed) BuckwheatCharcoal else BuckwheatSunken,
                                    modifier = Modifier
                                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                        .clickable { onToggleItemClaim(item.itemId, m.memberId) }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = m.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (claimed) Color.White else BuckwheatSecondaryText
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatCents(item.priceCents, symbol),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${formatCents(itemWithTax, symbol)} w/ tax",
                            fontSize = 11.sp,
                            color = BuckwheatSecondaryText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseLedgersScreen(
    uiState: SplitMateUiState,
    onSelectGroup: (String) -> Unit,
    onOpenNumPad: () -> Unit
) {
    val symbol = uiState.activeCurrency.symbol
    val groupExpenses = remember(uiState.expenses, uiState.activeGroupId) {
        uiState.expenses.filter { it.groupId == uiState.activeGroupId }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("LedgerExpensesLazyColumn")
    ) {
        item(key = "ledger_group_selector") {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.groups.forEach { group ->
                    FilterChip(
                        selected = group.groupId == uiState.activeGroupId,
                        onClick = { onSelectGroup(group.groupId) },
                        label = { Text(group.name, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    )
                }
            }
        }

        items(
            items = groupExpenses,
            key = { it.expenseId }
        ) { expense ->
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BuckwheatSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BuckwheatBorder, RoundedCornerShape(24.dp))
                    .testTag("LedgerItem_${expense.expenseId}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(expense.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text(
                            text = "Locked Rate: 1 USD = ${String.format(Locale.US, "%.2f", expense.lockedExchangeRate)} ${expense.currencyCode}",
                            fontSize = 12.sp,
                            color = BuckwheatSecondaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (expense.syncStatus == "PENDING") BuckwheatPeachContainer else BuckwheatSageContainer
                        ) {
                            Text(
                                text = "Status: ${expense.syncStatus}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (expense.syncStatus == "PENDING") BuckwheatTerracottaDark else BuckwheatOlivePrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = formatCents(expense.totalAmountCents, symbol),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = BuckwheatOlivePrimary
                    )
                }
            }
        }
    }
}

@Composable
fun GreedySettleUpScreen(
    uiState: SplitMateUiState,
    balances: List<SplitMateMathEngine.MemberNetBalance>,
    transfers: List<SplitMateMathEngine.SimplifiedTransfer>,
    onMarkTransferSettled: (SplitMateMathEngine.SimplifiedTransfer) -> Unit
) {
    val symbol = uiState.activeCurrency.symbol

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("SettleUpLazyColumn")
    ) {
        item(key = "settle_banner") {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = BuckwheatSageContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Greedy Minimum Cash Flow Simplifier",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = BuckwheatOlivePrimary
                    )
                    Text(
                        "Reduced group cross-debts to ${transfers.size} optimal direct transfer(s)",
                        fontSize = 13.sp,
                        color = BuckwheatCharcoal
                    )
                }
            }
        }

        items(
            items = transfers,
            key = { "${it.fromMemberId}_${it.toMemberId}_${it.amountCents}" }
        ) { transfer ->
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BuckwheatSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BuckwheatBorder, RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        DiceBearAvatar(seed = transfer.fromName, contentDescription = transfer.fromName, size = 40.dp)
                        Column {
                            Text(
                                "${transfer.fromName} pays ${transfer.toName}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                formatCents(transfer.amountCents, symbol),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = BuckwheatTerracottaDark
                            )
                        }
                    }

                    Button(
                        onClick = { onMarkTransferSettled(transfer) },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BuckwheatCharcoal,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("SettleTransferBtn_${transfer.fromMemberId}_${transfer.toMemberId}")
                    ) {
                        Text("✓ Mark Paid", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }

        item(key = "net_balances_header") {
            Text("Member Net Positions (∑ = 0.00)", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }

        items(
            items = balances,
            key = { "net_${it.memberId}" }
        ) { b ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BuckwheatSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DiceBearAvatar(seed = b.displayName, contentDescription = b.displayName, size = 34.dp)
                        Text(b.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        text = (if (b.netCents > 0) "+" else "") + formatCents(b.netCents, symbol),
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            b.netCents > 0 -> BuckwheatOlivePrimary
                            b.netCents < 0 -> BuckwheatTerracottaDark
                            else -> BuckwheatSecondaryText
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LiveCurrenciesAndProfileScreen(
    uiState: SplitMateUiState,
    onSyncLiveRates: () -> Unit,
    onToggleOfflineMode: (Boolean) -> Unit,
    onSelectCurrency: (String) -> Unit,
    onUpdateUserName: (String) -> Unit
) {
    var editedName by rememberSaveable(uiState.currentUserName) { mutableStateOf(uiState.currentUserName) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("CurrenciesLazyColumn")
    ) {
        item(key = "profile_dicebear_card") {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = BuckwheatSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DiceBearAvatar(seed = editedName, contentDescription = editedName, size = 56.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dynamic DiceBear Open-Peeps Profile", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text("SVG decoded via Coil + SvgDecoder & cached offline", fontSize = 11.sp, color = BuckwheatSecondaryText)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text("Your Name (Avatar Seed)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { onUpdateUserName(editedName) },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Text("Apply", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        item(key = "frankfurter_sync_bar") {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BuckwheatSageContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Frankfurter Live Currency API + Room Cache", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text("https://api.frankfurter.dev/v1/latest", fontSize = 11.sp, color = BuckwheatOlivePrimary)
                        }
                        Button(
                            onClick = onSyncLiveRates,
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (uiState.isSyncingRates) "Syncing..." else "Sync Live", fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Simulate Offline Mode (Queue as PENDING)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Switch(
                            checked = uiState.isOfflineMode,
                            onCheckedChange = onToggleOfflineMode,
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        )
                    }
                }
            }
        }

        items(
            items = uiState.currencyRates,
            key = { it.currencyCode }
        ) { rate ->
            val selected = rate.currencyCode == uiState.activeCurrencyCode
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) BuckwheatSageContainer else BuckwheatSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minWidth = 48.dp, minHeight = 56.dp)
                    .clickable { onSelectCurrency(rate.currencyCode) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "${rate.currencyCode} · ${rate.currencyName}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            "1 ${rate.baseCurrency} = ${String.format(Locale.US, "%.4f", rate.rateFromBase)} ${rate.currencyCode}",
                            fontSize = 12.sp,
                            color = BuckwheatSecondaryText
                        )
                    }
                    Text(
                        rate.symbol,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = BuckwheatOlivePrimary
                    )
                }
            }
        }
    }
}

@Composable
fun TactileNumPadDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSubmitItem: (String, Long) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("Custom Receipt Item") }
    var cents by rememberSaveable { mutableLongStateOf(1250L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Item via Tactile NumPad", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Item Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = formatCents(cents, currencySymbol),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = BuckwheatOlivePrimary
                )
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("00", "0", "⌫")
                )
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { key ->
                            FilledTonalButton(
                                onClick = {
                                    when (key) {
                                        "⌫" -> cents /= 10L
                                        "00" -> if (cents < 999999L) cents *= 100L
                                        else -> {
                                            val d = key.toLongOrNull() ?: 0L
                                            if (cents < 9999999L) cents = cents * 10L + d
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
                                Text(key, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitItem(title, cents) },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Add Item", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NewGroupDialog(
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onCreateGroup: (String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var friendsCsv by rememberSaveable { mutableStateOf("Rohan, Sneha, Vikram") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Expense Group", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name (e.g. Goa Trip)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = friendsCsv,
                    onValueChange = { friendsCsv = it },
                    label = { Text("Members (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreateGroup(name, friendsCsv) },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Create ($defaultCurrency)", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
