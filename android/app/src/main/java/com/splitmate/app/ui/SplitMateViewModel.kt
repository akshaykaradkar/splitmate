package com.splitmate.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitmate.app.SplitMateMathEngine
import com.splitmate.app.data.CurrencyRateEntity
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseGroupEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.FrankfurterApiService
import com.splitmate.app.data.FrankfurterNetwork
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.data.SettlementEntity
import com.splitmate.app.data.SplitMateDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class ReceiptLineItem(
    val itemId: String,
    val title: String,
    val priceCents: Long,
    val claimedByMemberIds: Set<String> = emptySet()
)

data class SplitMateUiState(
    val currentUserName: String = "Akshay",
    val currentUserSeed: String = "Akshay",
    val activeCurrencyCode: String = "INR",
    val isOfflineMode: Boolean = false,
    val isSyncingRates: Boolean = false,
    val currencyRates: List<CurrencyRateEntity> = defaultSeedCurrencies(),
    val groups: List<ExpenseGroupEntity> = emptyList(),
    val activeGroupId: String = "g_tahoe",
    val members: List<GroupMemberEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val splits: List<ExpenseSplitEntity> = emptyList(),
    val settlements: List<SettlementEntity> = emptyList(),
    // Collaborative Manual Entry / Live Receipt Matrix State (Bypasses "Split Equally" default)
    val receiptTitle: String = "Osteria Del Sole · Table 14",
    val receiptPayerId: String = "m_1",
    val activeClaimerPersonaId: String = "m_1",
    val receiptTaxPercent: Double = 8.875,
    val receiptTipPercent: Double = 18.0,
    val receiptItems: List<ReceiptLineItem> = defaultSeedReceiptItems(),
    val statusBannerMessage: String? = null
) {
    val activeCurrency: CurrencyRateEntity
        get() = currencyRates.find { it.currencyCode == activeCurrencyCode }
            ?: CurrencyRateEntity("INR", "Indian Rupee", "₹", 83.95)

    val activeGroupMembers: List<GroupMemberEntity>
        get() = members.filter { it.groupId == activeGroupId }
}

fun defaultSeedCurrencies(): List<CurrencyRateEntity> = listOf(
    CurrencyRateEntity("INR", "Indian Rupee", "₹", 83.95),
    CurrencyRateEntity("USD", "United States Dollar", "$", 1.00),
    CurrencyRateEntity("EUR", "Euro", "€", 0.92),
    CurrencyRateEntity("GBP", "British Pound Sterling", "£", 0.79),
    CurrencyRateEntity("JPY", "Japanese Yen", "¥", 151.40),
    CurrencyRateEntity("AUD", "Australian Dollar", "A$", 1.52),
    CurrencyRateEntity("CAD", "Canadian Dollar", "CA$", 1.36),
    CurrencyRateEntity("CHF", "Swiss Franc", "Fr", 0.88),
    CurrencyRateEntity("SGD", "Singapore Dollar", "S$", 1.34),
    CurrencyRateEntity("AED", "UAE Dirham", "د.إ", 3.67),
    CurrencyRateEntity("THB", "Thai Baht", "฿", 35.80),
    CurrencyRateEntity("MYR", "Malaysian Ringgit", "RM", 4.68),
    CurrencyRateEntity("IDR", "Indonesian Rupiah", "Rp", 15650.0),
    CurrencyRateEntity("KRW", "South Korean Won", "₩", 1345.0),
    CurrencyRateEntity("BRL", "Brazilian Real", "R$", 5.05),
    CurrencyRateEntity("MXN", "Mexican Peso", "Mex$", 16.80),
    CurrencyRateEntity("ZAR", "South African Rand", "R", 18.70),
    CurrencyRateEntity("CNY", "Chinese Renminbi Yuan", "¥", 7.23),
    CurrencyRateEntity("HKD", "Hong Kong Dollar", "HK$", 7.82),
    CurrencyRateEntity("NZD", "New Zealand Dollar", "NZ$", 1.64),
    CurrencyRateEntity("SEK", "Swedish Krona", "kr", 10.55),
    CurrencyRateEntity("NOK", "Norwegian Krone", "kr", 10.72),
    CurrencyRateEntity("DKK", "Danish Krone", "kr", 6.86),
    CurrencyRateEntity("PLN", "Polish Złoty", "zł", 3.98)
)

fun defaultSeedReceiptItems(): List<ReceiptLineItem> = listOf(
    ReceiptLineItem("item_1", "Truffle Tagliatelle", 2800L, setOf("m_1")),
    ReceiptLineItem("item_2", "Wood-Fired Diavola Pizza", 2400L, setOf("m_2")),
    ReceiptLineItem("item_3", "Artisanal Carafe Wine", 4200L, setOf("m_1", "m_2", "m_3")),
    ReceiptLineItem("item_4", "Shared Antipasto Misto", 2600L, emptySet()) // Unclaimed Remainder!
)

data class ActiveGroupCardUiModel(
    val groupId: String,
    val name: String,
    val memberCount: Int,
    val memberSeeds: List<String>,
    val remainingCount: Int,
    val netBalanceCents: Long,
    val formattedBadgeText: String,
    val statusPillText: String
)

data class SettlementTransferUiModel(
    val transfer: SplitMateMathEngine.SimplifiedTransfer,
    val fromName: String,
    val fromSeed: String,
    val toName: String,
    val toSeed: String,
    val upiId: String,
    val amount: String,
    val formattedDisplayAmount: String,
    val isCurrentUserDebtor: Boolean
)

class SplitMateViewModel(
    private val dao: SplitMateDao? = null,
    private val api: FrankfurterApiService = FrankfurterNetwork.api,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow( createInitialSeededState() )
    val uiState: StateFlow<SplitMateUiState> = _uiState.asStateFlow()

    val totalBalance: StateFlow<String> = _uiState.map { state ->
        val sym = state.activeCurrency.symbol
        val netCents = computeOverallUserBalanceCents(state)
        val absMajor = String.format(Locale.US, "%.2f", kotlin.math.abs(netCents) / 100.0)
        when {
            netCents > 0L -> "+$sym$absMajor"
            netCents < 0L -> "-$sym$absMajor"
            else -> "${sym}0.00"
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "+₹28.33")

    val activeGroups: StateFlow<List<ActiveGroupCardUiModel>> = _uiState.map { state ->
        val sym = state.activeCurrency.symbol
        state.groups.map { group ->
            val groupMembers = state.members.filter { it.groupId == group.groupId }
            val groupExpenses = state.expenses.filter { it.groupId == group.groupId }
            val groupExpenseIds = groupExpenses.map { it.expenseId }.toSet()
            val groupSplits = state.splits.filter { groupExpenseIds.contains(it.expenseId) }
            val groupSettlements = state.settlements.filter { it.groupId == group.groupId }

            val meMember = groupMembers.find { it.isCurrentUser } ?: groupMembers.firstOrNull()
            val balances = computeGroupNetBalances(groupMembers, groupExpenses, groupSplits, groupSettlements)
            val myNetCents = if (meMember != null) (balances[meMember.memberId] ?: 0L) else 0L
            val simplified = SplitMateMathEngine.simplifyDebtsGreedy(
                groupMembers.map { m ->
                    SplitMateMathEngine.MemberNetBalance(m.memberId, m.name, balances[m.memberId] ?: 0L)
                }
            )
            val absStr = String.format(Locale.US, "%.2f", kotlin.math.abs(myNetCents) / 100.0)
            val badgeText = when {
                myNetCents > 0L -> "YOU GET BACK $sym$absStr"
                myNetCents < 0L -> "YOU OWE $sym$absStr"
                else -> "✓ ${sym}0.00 All settled up"
            }
            val rawEdges = (groupExpenses.size * groupMembers.size).coerceAtLeast(simplified.size)
            val pillText = if (simplified.isEmpty()) {
                "Equilibrium Reached"
            } else {
                "⚡ Greedy: $rawEdges → ${simplified.size} transfers"
            }
            val visibleSeeds = groupMembers.take(4).map { it.avatarSeed }
            val rem = (groupMembers.size - visibleSeeds.size).coerceAtLeast(0)

            ActiveGroupCardUiModel(
                groupId = group.groupId,
                name = group.name,
                memberCount = groupMembers.size,
                memberSeeds = visibleSeeds,
                remainingCount = rem,
                netBalanceCents = myNetCents,
                formattedBadgeText = badgeText,
                statusPillText = pillText
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val settlementPlan: StateFlow<List<SettlementTransferUiModel>> = _uiState.map { state ->
        val sym = state.activeCurrency.symbol
        val groupMembers = state.activeGroupMembers
        val groupExpenses = state.expenses.filter { it.groupId == state.activeGroupId }
        val groupExpenseIds = groupExpenses.map { it.expenseId }.toSet()
        val groupSplits = state.splits.filter { groupExpenseIds.contains(it.expenseId) }
        val groupSettlements = state.settlements.filter { it.groupId == state.activeGroupId }
        val balances = computeGroupNetBalances(groupMembers, groupExpenses, groupSplits, groupSettlements)
        val meMember = groupMembers.find { it.isCurrentUser }

        val transfers = SplitMateMathEngine.simplifyDebtsGreedy(
            groupMembers.map { m ->
                SplitMateMathEngine.MemberNetBalance(m.memberId, m.name, balances[m.memberId] ?: 0L)
            }
        )
        transfers.map { tr ->
            val fromMember = groupMembers.find { it.memberId == tr.fromMemberId }
            val toMember = groupMembers.find { it.memberId == tr.toMemberId }
            val cleanHandle = tr.toName.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "").ifEmpty { "splitmate" }
            val majorStr = String.format(Locale.US, "%.2f", tr.amountCents / 100.0)
            SettlementTransferUiModel(
                transfer = tr,
                fromName = if (fromMember?.isCurrentUser == true) "You" else tr.fromName,
                fromSeed = fromMember?.avatarSeed ?: tr.fromName,
                toName = if (toMember?.isCurrentUser == true) "You" else tr.toName,
                toSeed = toMember?.avatarSeed ?: tr.toName,
                upiId = "$cleanHandle@okhdfcbank",
                amount = majorStr,
                formattedDisplayAmount = "$sym$majorStr",
                isCurrentUserDebtor = (tr.fromMemberId == meMember?.memberId) || (fromMember?.isCurrentUser == true)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        if (dao != null) {
            observeRoomDatabase(dao)
            syncLiveCurrencyRatesFromFrankfurter()
        }
    }

    private fun createInitialSeededState(): SplitMateUiState {
        val initialGroups = listOf(
            ExpenseGroupEntity("g_tahoe", "Lake Tahoe Cabin", "INR"),
            ExpenseGroupEntity("g_mission", "Mission Apt Roommates", "INR")
        )
        val initialMembers = listOf(
            GroupMemberEntity("m_1", "g_tahoe", "Akshay", "Akshay", isCurrentUser = true),
            GroupMemberEntity("m_2", "g_tahoe", "Sam", "Sam", isCurrentUser = false),
            GroupMemberEntity("m_3", "g_tahoe", "Priya", "Priya", isCurrentUser = false),
            GroupMemberEntity("m_4", "g_tahoe", "Maya", "Maya", isCurrentUser = false),
            GroupMemberEntity("m_1_apt", "g_mission", "Akshay", "Akshay", isCurrentUser = true),
            GroupMemberEntity("m_5_apt", "g_mission", "Rohan", "Rohan", isCurrentUser = false),
            GroupMemberEntity("m_6_apt", "g_mission", "Sneha", "Sneha", isCurrentUser = false)
        )
        val initialExpenses = listOf(
            ExpenseEntity(
                expenseId = "exp_seed_1",
                groupId = "g_tahoe",
                title = "Cabin Booking & Firewood",
                payerId = "m_1",
                baseSubtotalCents = 4250L,
                taxCents = 0L,
                tipCents = 0L,
                totalAmountCents = 4250L,
                lockedMultiplier = 1.0,
                unassignedBaseCents = 0L,
                currencyCode = "INR",
                lockedExchangeRate = 83.95,
                syncStatus = "SYNCED"
            )
        )
        val initialSplits = listOf(
            ExpenseSplitEntity("sp_1", "exp_seed_1", "m_1", 1417L, 1417L, plusOneCent = true),
            ExpenseSplitEntity("sp_2", "exp_seed_1", "m_2", 1417L, 1417L, plusOneCent = true),
            ExpenseSplitEntity("sp_3", "exp_seed_1", "m_3", 1416L, 1416L, plusOneCent = false)
        )
        return SplitMateUiState(
            groups = initialGroups,
            members = initialMembers,
            expenses = initialExpenses,
            splits = initialSplits
        )
    }

    private fun observeRoomDatabase(roomDao: SplitMateDao) {
        viewModelScope.launch(ioDispatcher) {
            // Seed Room if empty
            val current = _uiState.value
            current.groups.forEach { roomDao.insertGroup(it) }
            roomDao.insertMembers(current.members)
            roomDao.upsertCurrencyRates(current.currencyRates)
            current.expenses.forEach { roomDao.insertExpense(it) }
            roomDao.insertExpenseSplits(current.splits)
        }
        viewModelScope.launch(ioDispatcher) {
            roomDao.observeCurrencyRates().collect { rates ->
                if (rates.isNotEmpty()) {
                    _uiState.update { it.copy(currencyRates = rates) }
                }
            }
        }
    }

    /**
     * Fetches live conversion rates at runtime from `https://api.frankfurter.dev/v1/latest`
     * and caches them immediately into Jetpack Room (`CurrencyRateEntity`) for offline use.
     */
    fun syncLiveCurrencyRatesFromFrankfurter(base: String = "USD") {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isSyncingRates = true) }
            try {
                val latest = api.getLatestRates(base)
                val namesMap = runCatching { api.getSupportedCurrencies() }.getOrDefault(emptyMap())
                val existingByCode = _uiState.value.currencyRates.associateBy { it.currencyCode }

                val merged = latest.rates.map { (code, rate) ->
                    val prev = existingByCode[code]
                    CurrencyRateEntity(
                        currencyCode = code,
                        currencyName = namesMap[code] ?: prev?.currencyName ?: code,
                        symbol = prev?.symbol ?: currencySymbolFor(code),
                        rateFromBase = rate,
                        baseCurrency = latest.base,
                        updatedAt = System.currentTimeMillis()
                    )
                }.toMutableList()

                // Ensure base currency itself is included at 1.00
                if (merged.none { it.currencyCode == base }) {
                    merged.add(0, CurrencyRateEntity(base, namesMap[base] ?: base, currencySymbolFor(base), 1.0, base))
                }

                dao?.upsertCurrencyRates(merged)
                dao?.markPendingExpensesSynced()

                _uiState.update {
                    it.copy(
                        isSyncingRates = false,
                        isOfflineMode = false,
                        currencyRates = merged.sortedBy { c -> c.currencyCode },
                        statusBannerMessage = "Synced ${merged.size} live currency rates from Frankfurter API"
                    )
                }
            } catch (e: Exception) {
                // Offline-First Graceful Fallback: keep cached Room rates & flag offline state
                _uiState.update {
                    it.copy(
                        isSyncingRates = false,
                        isOfflineMode = true,
                        statusBannerMessage = "Offline mode active · Using Room cached currency rates"
                    )
                }
            }
        }
    }

    fun setOfflineMode(offline: Boolean) {
        _uiState.update { it.copy(isOfflineMode = offline) }
    }

    fun updateUserProfile(newName: String, newCurrencyCode: String) {
        val cleanName = newName.trim().ifEmpty { "Akshay" }
        _uiState.update { state ->
            val updatedMembers = state.members.map { m ->
                if (m.isCurrentUser) m.copy(name = cleanName, avatarSeed = cleanName) else m
            }
            state.copy(
                currentUserName = cleanName,
                currentUserSeed = cleanName,
                activeCurrencyCode = newCurrencyCode,
                members = updatedMembers
            )
        }
    }

    fun selectActiveGroup(groupId: String) {
        _uiState.update { state ->
            val groupMembers = state.members.filter { it.groupId == groupId }
            val firstMemberId = groupMembers.firstOrNull()?.memberId ?: "m_1"
            state.copy(
                activeGroupId = groupId,
                receiptPayerId = firstMemberId,
                activeClaimerPersonaId = firstMemberId
            )
        }
    }

    fun createNewGroup(name: String, currencyCode: String, friendNamesCsv: String) {
        val cleanGroup = name.trim().ifEmpty { "New Group" }
        val groupId = "g_${System.currentTimeMillis()}"
        val newGroup = ExpenseGroupEntity(groupId, cleanGroup, currencyCode)

        val friendList = friendNamesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val meMember = GroupMemberEntity(
            memberId = "${groupId}_me",
            groupId = groupId,
            name = _uiState.value.currentUserName,
            avatarSeed = _uiState.value.currentUserSeed,
            isCurrentUser = true
        )
        val friendMembers = friendList.mapIndexed { index, fName ->
            GroupMemberEntity(
                memberId = "${groupId}_f$index",
                groupId = groupId,
                name = fName,
                avatarSeed = fName,
                isCurrentUser = false
            )
        }
        val allNewMembers = listOf(meMember) + friendMembers

        _uiState.update { state ->
            state.copy(
                groups = listOf(newGroup) + state.groups,
                members = state.members + allNewMembers,
                activeGroupId = groupId,
                receiptPayerId = meMember.memberId,
                activeClaimerPersonaId = meMember.memberId,
                statusBannerMessage = "Created group \"$cleanGroup\" with ${allNewMembers.size} members"
            )
        }

        viewModelScope.launch(ioDispatcher) {
            dao?.insertGroup(newGroup)
            dao?.insertMembers(allNewMembers)
        }
    }

    fun addMemberToActiveGroup(friendName: String) {
        val clean = friendName.trim()
        if (clean.isEmpty()) return
        val groupId = _uiState.value.activeGroupId
        val newMember = GroupMemberEntity(
            memberId = "m_${System.currentTimeMillis()}",
            groupId = groupId,
            name = clean,
            avatarSeed = clean,
            isCurrentUser = false
        )
        _uiState.update { state ->
            state.copy(
                members = state.members + newMember,
                statusBannerMessage = "Added $clean to group"
            )
        }
        viewModelScope.launch(ioDispatcher) {
            dao?.insertMembers(listOf(newMember))
        }
    }

    // --- Collaborative Receipt Claim & Remainder Engine Actions ---
    fun setClaimerPersona(memberId: String) {
        _uiState.update { it.copy(activeClaimerPersonaId = memberId) }
    }

    fun setReceiptPayer(memberId: String) {
        _uiState.update { it.copy(receiptPayerId = memberId) }
    }

    fun updateReceiptTaxAndTip(taxPercent: Double, tipPercent: Double) {
        _uiState.update { it.copy(receiptTaxPercent = taxPercent, receiptTipPercent = tipPercent) }
    }

    fun toggleReceiptItemClaim(itemId: String, memberId: String = _uiState.value.activeClaimerPersonaId) {
        _uiState.update { state ->
            val updatedItems = state.receiptItems.map { item ->
                if (item.itemId == itemId) {
                    val nextSet = item.claimedByMemberIds.toMutableSet()
                    if (!nextSet.add(memberId)) nextSet.remove(memberId)
                    item.copy(claimedByMemberIds = nextSet)
                } else item
            }
            state.copy(receiptItems = updatedItems)
        }
    }

    fun addReceiptLineItem(title: String, priceCents: Long) {
        if (priceCents <= 0L) return
        val cleanTitle = title.trim().ifEmpty { "Custom Item" }
        val newItem = ReceiptLineItem(
            itemId = "item_${System.currentTimeMillis()}",
            title = cleanTitle,
            priceCents = priceCents,
            claimedByMemberIds = setOf(_uiState.value.activeClaimerPersonaId)
        )
        _uiState.update { state ->
            state.copy(receiptItems = state.receiptItems + newItem)
        }
    }

    fun splitUnassignedRemainderEqually() {
        _uiState.update { state ->
            val allMemberIds = state.activeGroupMembers.map { it.memberId }.toSet()
            val updatedItems = state.receiptItems.map { item ->
                if (item.claimedByMemberIds.isEmpty()) {
                    item.copy(claimedByMemberIds = allMemberIds)
                } else item
            }
            state.copy(
                receiptItems = updatedItems,
                statusBannerMessage = "Unassigned remainder split equally across all members (0.00¢ drift)"
            )
        }
    }

    /**
     * Commits an Itemized Receipt or Custom Expense into the Ledger:
     * - Locks the exact exchange rate (`lockedExchangeRate`) at transaction creation time.
     * - Queues as `"PENDING"` when offline (`isOfflineMode == true`) or `"SYNCED"` when online.
     * - Reconciles penny rounding via Largest Remainder (`0.00¢ drift`).
     */
    fun commitCollaborativeExpense(
        title: String = _uiState.value.receiptTitle,
        customTotalCents: Long? = null
    ) {
        val state = _uiState.value
        val groupMembers = state.activeGroupMembers
        if (groupMembers.isEmpty()) return

        val lockedRate = state.activeCurrency.rateFromBase
        val syncStatus = if (state.isOfflineMode) "PENDING" else "SYNCED"
        val expenseId = "exp_${System.currentTimeMillis()}"

        val proportionalResult: SplitMateMathEngine.ProportionalSplitResult = if (customTotalCents != null) {
            val equalAllocations = SplitMateMathEngine.splitEquallyZeroDrift(
                customTotalCents,
                groupMembers.map { it.memberId to it.name }
            )
            SplitMateMathEngine.ProportionalSplitResult(
                lockedMultiplier = 1.0,
                baseSubtotalCents = customTotalCents,
                taxCents = 0L,
                tipCents = 0L,
                totalFinalCents = customTotalCents,
                unassignedBaseCents = 0L,
                unassignedFinalCents = 0L,
                allocations = equalAllocations,
                driftCents = 0L
            )
        } else {
            val baseSubtotal = state.receiptItems.sumOf { it.priceCents }
            val taxCents = Math.round(baseSubtotal * (state.receiptTaxPercent / 100.0))
            val tipCents = Math.round(baseSubtotal * (state.receiptTipPercent / 100.0))
            val memberClaimsMap = groupMembers.associate { it.memberId to 0L }.toMutableMap()

            state.receiptItems.forEach { item ->
                val validClaimers = item.claimedByMemberIds.filter { memberClaimsMap.containsKey(it) }
                if (validClaimers.isNotEmpty()) {
                    val shares = SplitMateMathEngine.splitEquallyZeroDrift(
                        item.priceCents,
                        validClaimers.map { id -> id to id }
                    )
                    shares.forEach { s ->
                        memberClaimsMap[s.memberId] = (memberClaimsMap[s.memberId] ?: 0L) + s.finalCents
                    }
                }
            }

            SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = baseSubtotal,
                taxCents = taxCents,
                tipCents = tipCents,
                payerId = state.receiptPayerId,
                memberBaseClaimsCents = groupMembers.map { m ->
                    Triple(m.memberId, m.name, memberClaimsMap[m.memberId] ?: 0L)
                },
                attributeRemainderToPayer = true
            )
        }

        val expenseEntity = ExpenseEntity(
            expenseId = expenseId,
            groupId = state.activeGroupId,
            title = title.ifBlank { "Collaborative Split" },
            payerId = state.receiptPayerId,
            baseSubtotalCents = proportionalResult.baseSubtotalCents,
            taxCents = proportionalResult.taxCents,
            tipCents = proportionalResult.tipCents,
            totalAmountCents = proportionalResult.totalFinalCents,
            lockedMultiplier = proportionalResult.lockedMultiplier,
            unassignedBaseCents = proportionalResult.unassignedBaseCents,
            currencyCode = state.activeCurrencyCode,
            lockedExchangeRate = lockedRate,
            syncStatus = syncStatus
        )

        val splitEntities = proportionalResult.allocations.mapIndexed { idx, alloc ->
            ExpenseSplitEntity(
                splitId = "${expenseId}_sp_$idx",
                expenseId = expenseId,
                memberId = alloc.memberId,
                baseClaimedCents = alloc.baseClaimedCents,
                finalOwedCents = alloc.finalCents,
                plusOneCent = alloc.plusOneCent
            )
        }

        _uiState.update { curr ->
            curr.copy(
                expenses = listOf(expenseEntity) + curr.expenses,
                splits = curr.splits + splitEntities,
                statusBannerMessage = "Saved \"${expenseEntity.title}\" · Rate locked @ $lockedRate (${expenseEntity.syncStatus})"
            )
        }

        viewModelScope.launch(ioDispatcher) {
            dao?.insertExpenseWithSplits(expenseEntity, splitEntities)
        }
    }

    fun markGreedyTransferSettled(transfer: SplitMateMathEngine.SimplifiedTransfer) {
        val state = _uiState.value
        val settlement = SettlementEntity(
            settlementId = "settle_${System.currentTimeMillis()}",
            groupId = state.activeGroupId,
            fromMemberId = transfer.fromMemberId,
            fromMemberName = transfer.fromName,
            toMemberId = transfer.toMemberId,
            toMemberName = transfer.toName,
            amountCents = transfer.amountCents,
            currencyCode = state.activeCurrencyCode,
            lockedExchangeRate = state.activeCurrency.rateFromBase,
            syncStatus = if (state.isOfflineMode) "PENDING" else "SYNCED"
        )

        _uiState.update { curr ->
            curr.copy(
                settlements = listOf(settlement) + curr.settlements,
                statusBannerMessage = "Settled ${transfer.fromName} → ${transfer.toName}"
            )
        }

        viewModelScope.launch(ioDispatcher) {
            dao?.insertSettlement(settlement)
        }
    }

    fun rollbackExpense(expenseId: String) {
        _uiState.update { curr ->
            val removed = curr.expenses.find { it.expenseId == expenseId }
            curr.copy(
                expenses = curr.expenses.filterNot { it.expenseId == expenseId },
                splits = curr.splits.filterNot { it.expenseId == expenseId },
                statusBannerMessage = "Rolled back \"${removed?.title ?: "Expense"}\""
            )
        }
    }

    private fun computeGroupNetBalances(
        groupMembers: List<GroupMemberEntity>,
        groupExpenses: List<ExpenseEntity>,
        groupSplits: List<ExpenseSplitEntity>,
        groupSettlements: List<SettlementEntity>
    ): Map<String, Long> {
        val netMap = groupMembers.associate { it.memberId to 0L }.toMutableMap()
        groupExpenses.forEach { exp ->
            netMap[exp.payerId] = (netMap[exp.payerId] ?: 0L) + exp.totalAmountCents
        }
        groupSplits.forEach { sp ->
            netMap[sp.memberId] = (netMap[sp.memberId] ?: 0L) - sp.finalOwedCents
        }
        groupSettlements.forEach { st ->
            netMap[st.fromMemberId] = (netMap[st.fromMemberId] ?: 0L) + st.amountCents
            netMap[st.toMemberId] = (netMap[st.toMemberId] ?: 0L) - st.amountCents
        }
        return netMap
    }

    private fun computeOverallUserBalanceCents(state: SplitMateUiState): Long {
        return state.groups.sumOf { group ->
            val gMembers = state.members.filter { it.groupId == group.groupId }
            val gExpenses = state.expenses.filter { it.groupId == group.groupId }
            val gExpenseIds = gExpenses.map { it.expenseId }.toSet()
            val gSplits = state.splits.filter { gExpenseIds.contains(it.expenseId) }
            val gSettlements = state.settlements.filter { it.groupId == group.groupId }
            val me = gMembers.find { it.isCurrentUser } ?: gMembers.firstOrNull()
            val netMap = computeGroupNetBalances(gMembers, gExpenses, gSplits, gSettlements)
            if (me != null) (netMap[me.memberId] ?: 0L) else 0L
        }
    }

    companion object {
        fun currencySymbolFor(code: String): String = when (code.uppercase()) {
            "INR" -> "₹"
            "USD", "AUD", "CAD", "SGD", "NZD", "HKD", "MXN" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY", "CNY" -> "¥"
            "CHF" -> "Fr"
            "AED" -> "د.إ"
            "THB" -> "฿"
            "KRW" -> "₩"
            "BRL" -> "R$"
            else -> code
        }
    }
}
