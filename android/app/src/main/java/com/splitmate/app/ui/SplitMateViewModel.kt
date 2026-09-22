package com.splitmate.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitmate.app.SplitMateMathEngine
import com.splitmate.app.data.CurrencyRateEntity
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseGroupEntity
import com.splitmate.app.data.ExpenseSplitEntity
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

import com.splitmate.app.data.UserProfileEntity

data class ReceiptLineItem(
    val itemId: String,
    val title: String,
    val priceCents: Long,
    val claimedByMemberIds: Set<String> = emptySet()
)

data class SplitMateUiState(
    val hasRegisteredProfile: Boolean = true,
    val currentUserName: String = "Akshay",
    val currentUserSeed: String = "Akshay|Masculine",
    val currentUserCountry: String = "India",
    val userUpiId: String = "",
    val isDarkTheme: Boolean = false,
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
    val receiptTitle: String = "Osteria Del Sole · Table 14",
    val receiptPayerId: String = "m_1",
    val activeClaimerPersonaId: String = "m_1",
    val receiptTaxPercent: Double = 8.875,
    val receiptTipPercent: Double = 18.0,
    val receiptItems: List<ReceiptLineItem> = defaultSeedReceiptItems(),
    val statusBannerMessage: String? = null,
    val selectedTabName: String = "LEDGERS",
    val openedGroupDetailId: String? = null,
    val returnToGroupDetailId: String? = null
) {
    val activeCurrency: CurrencyRateEntity
        get() = CurrencyRateEntity("INR", "Indian Rupee", "₹", 83.95)

    val activeGroup: ExpenseGroupEntity?
        get() = groups.find { it.groupId == activeGroupId } ?: groups.firstOrNull()

    val activeGroupMembers: List<GroupMemberEntity>
        get() {
            val targetGroupId = activeGroup?.groupId ?: activeGroupId
            return members.filter { it.groupId == targetGroupId }
        }
}

fun defaultSeedCurrencies(): List<CurrencyRateEntity> = listOf(
    CurrencyRateEntity("INR", "Indian Rupee", "₹", 83.95)
)

fun defaultSeedReceiptItems(): List<ReceiptLineItem> = listOf(
    ReceiptLineItem("item_1", "Truffle Tagliatelle", 2800L, setOf("m_1")),
    ReceiptLineItem("item_2", "Wood-Fired Diavola Pizza", 2400L, setOf("m_2")),
    ReceiptLineItem("item_3", "Artisanal Carafe Wine", 4200L, setOf("m_1", "m_2", "m_3")),
    ReceiptLineItem("item_4", "Shared Antipasto Misto", 2600L, emptySet())
)

data class NewGroupMemberDraft(
    val name: String,
    val cleanPhone: String = "",
    val presentationStyle: String = "Neutral"
)

data class ActiveGroupCardUiModel(
    val groupId: String,
    val name: String,
    val iconName: String = "Flight",
    val memberCount: Int,
    val memberSeeds: List<String>,
    val remainingCount: Int,
    val netBalanceCents: Long,
    val formattedBadgeText: String,
    val statusPillText: String
)

data class SettlementTransferUiModel(
    val transfer: SplitMateMathEngine.SimplifiedTransfer,
    val fromMemberId: String,
    val fromName: String,
    val fromSeed: String,
    val toMemberId: String,
    val toName: String,
    val toSeed: String,
    val upiId: String,
    val cleanPhone: String,
    val hasLinkedPhone: Boolean,
    val amount: String,
    val formattedDisplayAmount: String,
    val isCurrentUserDebtor: Boolean
)

class SplitMateViewModel(
    private val dao: SplitMateDao? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        if (dao == null) createInitialSeededState() else createCleanProductionInitialState()
    )
    val uiState: StateFlow<SplitMateUiState> = _uiState.asStateFlow()

    val activeGroupMembers: StateFlow<List<GroupMemberEntity>> = _uiState.map { state ->
        state.activeGroupMembers
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _deviceContacts = MutableStateFlow<List<DeviceContact>>(emptyList())
    val deviceContacts: StateFlow<List<DeviceContact>> = _deviceContacts.asStateFlow()

    private val _isLoadingContacts = MutableStateFlow(false)
    val isLoadingContacts: StateFlow<Boolean> = _isLoadingContacts.asStateFlow()

    fun loadDeviceContacts(context: android.content.Context) {
        viewModelScope.launch(ioDispatcher) {
            _isLoadingContacts.value = true
            val loaded = queryAllDeviceContacts(context.applicationContext)
            _deviceContacts.value = loaded
            _isLoadingContacts.value = false
        }
    }

    val totalBalance: StateFlow<String> = _uiState.map { state ->
        val sym = "₹"
        if (state.groups.isEmpty() || state.expenses.isEmpty()) {
            "${sym}0.00"
        } else {
            val netCents = computeOverallUserBalanceCents(state)
            val absMajor = String.format(Locale.US, "%.2f", kotlin.math.abs(netCents) / 100.0)
            when {
                netCents > 0L -> "+$sym$absMajor"
                netCents < 0L -> "-$sym$absMajor"
                else -> "${sym}0.00"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "₹0.00")

    val activeGroups: StateFlow<List<ActiveGroupCardUiModel>> = _uiState.map { state ->
        if (state.groups.isEmpty()) {
            emptyList()
        } else {
            val sym = "₹"
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
                    else -> "All settled up"
                }
                val pillText = if (simplified.isEmpty()) {
                    "All settled up"
                } else {
                    "${simplified.size} simplified settlements"
                }
                val visibleSeeds = groupMembers.take(4).map { it.avatarSeed }
                val rem = (groupMembers.size - visibleSeeds.size).coerceAtLeast(0)

                ActiveGroupCardUiModel(
                    groupId = group.groupId,
                    name = group.name,
                    iconName = group.iconName,
                    memberCount = groupMembers.size,
                    memberSeeds = visibleSeeds,
                    remainingCount = rem,
                    netBalanceCents = myNetCents,
                    formattedBadgeText = badgeText,
                    statusPillText = pillText
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val settlementPlan: StateFlow<List<SettlementTransferUiModel>> = _uiState.map { state ->
        if (state.groups.isEmpty()) {
            emptyList()
        } else {
            val sym = "₹"
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
                val savedUpi = toMember?.upiId?.trim().orEmpty()
                val clean10Phone = cleanIndianTenDigitPhone(savedUpi.substringBefore("@"))
                val hasPhoneLinked = clean10Phone.length == 10
                val resolvedUpiId = if (hasPhoneLinked) "${clean10Phone}@upi" else ""
                val majorStr = String.format(Locale.US, "%.2f", tr.amountCents / 100.0)
                SettlementTransferUiModel(
                    transfer = tr,
                    fromMemberId = tr.fromMemberId,
                    fromName = if (fromMember?.isCurrentUser == true) "You" else tr.fromName,
                    fromSeed = fromMember?.avatarSeed ?: tr.fromName,
                    toMemberId = tr.toMemberId,
                    toName = if (toMember?.isCurrentUser == true) "You" else tr.toName,
                    toSeed = toMember?.avatarSeed ?: tr.toName,
                    upiId = resolvedUpiId,
                    cleanPhone = if (hasPhoneLinked) clean10Phone else "",
                    hasLinkedPhone = hasPhoneLinked,
                    amount = majorStr,
                    formattedDisplayAmount = "$sym$majorStr",
                    isCurrentUserDebtor = (tr.fromMemberId == meMember?.memberId) || (fromMember?.isCurrentUser == true)
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        if (dao != null) {
            observeRoomDatabase(dao)
        }
    }

    private fun createCleanProductionInitialState(): SplitMateUiState = SplitMateUiState(
        hasRegisteredProfile = false,
        currentUserName = "",
        currentUserSeed = "SplitMateExplorer",
        currentUserCountry = "India",
        userUpiId = "",
        activeCurrencyCode = "INR",
        groups = emptyList(),
        members = emptyList(),
        expenses = emptyList(),
        splits = emptyList(),
        settlements = emptyList()
    )

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
            roomDao.upsertCurrencyRates(_uiState.value.currencyRates)
        }
        viewModelScope.launch(ioDispatcher) {
            roomDao.observeUserProfile().collect { profile ->
                if (profile != null) {
                    _uiState.update {
                        it.copy(
                            hasRegisteredProfile = true,
                            currentUserName = profile.name,
                            currentUserSeed = profile.avatarSeed,
                            currentUserCountry = profile.countryName,
                            activeCurrencyCode = profile.currencyCode,
                            userUpiId = profile.upiId,
                            isDarkTheme = profile.isDarkTheme
                        )
                    }
                } else {
                    _uiState.update { it.copy(hasRegisteredProfile = false) }
                }
            }
        }
        viewModelScope.launch(ioDispatcher) {
            roomDao.observeGroups().collect { groups ->
                _uiState.update { curr ->
                    val nextActiveGroup = curr.activeGroupId.takeIf { id -> groups.any { it.groupId == id } }
                        ?: groups.firstOrNull()?.groupId.orEmpty()
                    curr.copy(groups = groups, activeGroupId = nextActiveGroup)
                }
            }
        }
        viewModelScope.launch(ioDispatcher) {
            roomDao.observeAllMembers().collect { members ->
                _uiState.update { it.copy(members = members) }
            }
        }
        viewModelScope.launch(ioDispatcher) {
            roomDao.observeAllExpenses().collect { expenses ->
                _uiState.update { it.copy(expenses = expenses) }
            }
        }
        viewModelScope.launch(ioDispatcher) {
            roomDao.observeAllSplits().collect { splits ->
                _uiState.update { it.copy(splits = splits) }
            }
        }
        viewModelScope.launch(ioDispatcher) {
            roomDao.observeAllSettlements().collect { settlements ->
                _uiState.update { it.copy(settlements = settlements) }
            }
        }
        viewModelScope.launch(ioDispatcher) {
            roomDao.observeCurrencyRates().collect { rates ->
                if (rates.isNotEmpty()) {
                    _uiState.update { it.copy(currencyRates = rates) }
                }
            }
        }
    }

    fun completeOnboarding(
        name: String,
        countryName: String,
        currencyCode: String,
        currencySymbol: String,
        avatarSeed: String = name
    ) {
        val cleanName = name.trim().ifEmpty { "Explorer" }
        val cleanSeed = avatarSeed.trim().ifEmpty { cleanName }
        val cleanHandle = cleanName.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "").ifEmpty { "explorer" }
        val defaultUpi = "$cleanHandle@okaxis"
        val profile = UserProfileEntity(
            profileId = "me",
            name = cleanName,
            avatarSeed = cleanSeed,
            countryName = countryName,
            currencyCode = currencyCode,
            currencySymbol = currencySymbol,
            upiId = defaultUpi,
            isDarkTheme = _uiState.value.isDarkTheme
        )
        _uiState.update {
            it.copy(
                hasRegisteredProfile = true,
                currentUserName = cleanName,
                currentUserSeed = cleanSeed,
                currentUserCountry = countryName,
                activeCurrencyCode = currencyCode,
                userUpiId = defaultUpi,
                statusBannerMessage = "Welcome $cleanName"
            )
        }
        viewModelScope.launch(ioDispatcher) {
            dao?.upsertUserProfile(profile)
        }
    }

    fun updateUpiId(newUpiId: String) {
        val cleanUpi = newUpiId.trim().ifEmpty { _uiState.value.userUpiId }
        _uiState.update { it.copy(userUpiId = cleanUpi) }
        viewModelScope.launch(ioDispatcher) {
            val existing = dao?.getUserProfile()
            if (existing != null) {
                dao?.upsertUserProfile(existing.copy(upiId = cleanUpi))
            }
        }
    }

    fun toggleDarkTheme(isDark: Boolean) {
        _uiState.update { it.copy(isDarkTheme = isDark) }
        viewModelScope.launch(ioDispatcher) {
            val existing = dao?.getUserProfile()
            if (existing != null) {
                dao?.upsertUserProfile(existing.copy(isDarkTheme = isDark))
            }
        }
    }

    fun clearLocalVault() {
        _uiState.update {
            it.copy(
                groups = emptyList(),
                members = emptyList(),
                expenses = emptyList(),
                splits = emptyList(),
                settlements = emptyList(),
                receiptItems = emptyList(),
                activeGroupId = "",
                statusBannerMessage = "App data reset"
            )
        }
        viewModelScope.launch(ioDispatcher) {
            dao?.clearAllLedgerData()
        }
    }

    fun updateFriendUpi(
        memberId: String,
        newName: String,
        newUpiId: String,
        newAvatarSeed: String? = null
    ) {
        val cleanName = newName.trim().ifEmpty { "Friend" }
        val cleanUpi = newUpiId.trim()
        val effectiveSeed = newAvatarSeed?.trim()?.ifEmpty { cleanName } ?: cleanName
        _uiState.update { state ->
            val updatedMembers = state.members.map { m ->
                if (m.memberId == memberId) {
                    m.copy(name = cleanName, avatarSeed = effectiveSeed, upiId = cleanUpi)
                } else m
            }
            state.copy(
                members = updatedMembers,
                statusBannerMessage = "Updated $cleanName's UPI ID (${cleanUpi.ifEmpty { "default" }})"
            )
        }
        viewModelScope.launch(ioDispatcher) {
            dao?.updateMemberProfile(memberId, cleanName, cleanUpi, effectiveSeed)
        }
    }

    /**
     * Commits a Quick Equal Expense from QuickExpenseScreen:
     * Strictly divides [totalAmountCents] equally among [selectedMemberIds] with Exact Split.
     */
    fun commitQuickEqualExpense(
        title: String,
        totalAmountCents: Long,
        selectedMemberIds: List<String>
    ) {
        if (totalAmountCents <= 0L) return
        val state = _uiState.value
        val groupMembers = state.activeGroupMembers
        if (groupMembers.isEmpty()) return

        val chosenMembers = groupMembers.filter { selectedMemberIds.contains(it.memberId) }
            .ifEmpty { groupMembers }
        val payer = groupMembers.find { it.isCurrentUser } ?: groupMembers.first()

        val lockedRate = state.activeCurrency.rateFromBase
        val syncStatus = if (state.isOfflineMode) "PENDING" else "SYNCED"
        val expenseId = "exp_${System.currentTimeMillis()}"

        val equalAllocations = SplitMateMathEngine.splitEquallyZeroDrift(
            totalAmountCents,
            chosenMembers.map { it.memberId to it.name }
        )

        val expenseEntity = ExpenseEntity(
            expenseId = expenseId,
            groupId = state.activeGroupId,
            title = title.trim().ifBlank { "Quick Equal Split" },
            payerId = payer.memberId,
            baseSubtotalCents = totalAmountCents,
            taxCents = 0L,
            tipCents = 0L,
            totalAmountCents = totalAmountCents,
            lockedMultiplier = 1.0,
            unassignedBaseCents = 0L,
            currencyCode = "INR",
            lockedExchangeRate = lockedRate,
            syncStatus = syncStatus
        )

        val splitEntities = equalAllocations.mapIndexed { idx, alloc ->
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
                statusBannerMessage = "Split \"${expenseEntity.title}\" equally among ${chosenMembers.size} people (Exact Split)"
            )
        }

        viewModelScope.launch(ioDispatcher) {
            dao?.insertExpenseWithSplits(expenseEntity, splitEntities)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun syncLiveCurrencyRatesFromFrankfurter(base: String = "INR") {
        _uiState.update {
            it.copy(
                isSyncingRates = false,
                isOfflineMode = false,
                activeCurrencyCode = "INR"
            )
        }
    }

    fun setOfflineMode(offline: Boolean) {
        _uiState.update { it.copy(isOfflineMode = offline) }
    }

    fun updateUserProfile(newName: String, newSeedOrCurrency: String, newUpiId: String? = null) {
        val cleanName = newName.trim().ifEmpty { "Akshay" }
        var resolvedSeed = ""
        var resolvedUpi = ""
        var resolvedDark = false
        var resolvedCountry = "India"
        var updatedCurrentUserMembers = emptyList<GroupMemberEntity>()

        _uiState.update { state ->
            val styleFromArg = if (newSeedOrCurrency.contains('|')) {
                newSeedOrCurrency.substringAfter('|', "Masculine")
            } else {
                state.currentUserSeed.substringAfter('|', "Masculine")
            }
            val updatedSeed = "$cleanName|$styleFromArg"
            val finalUpi = newUpiId?.trim() ?: state.userUpiId
            resolvedSeed = updatedSeed
            resolvedUpi = finalUpi
            resolvedDark = state.isDarkTheme
            resolvedCountry = state.currentUserCountry

            val updatedMembers = state.members.map { m ->
                if (m.isCurrentUser) m.copy(name = cleanName, avatarSeed = updatedSeed, upiId = finalUpi) else m
            }
            updatedCurrentUserMembers = updatedMembers.filter { it.isCurrentUser }
            state.copy(
                currentUserName = cleanName,
                currentUserSeed = updatedSeed,
                userUpiId = finalUpi,
                activeCurrencyCode = "INR",
                members = updatedMembers,
                statusBannerMessage = "Saved profile & payment preferences"
            )
        }

        viewModelScope.launch(ioDispatcher) {
            dao?.upsertUserProfile(
                UserProfileEntity(
                    profileId = "me",
                    name = cleanName,
                    avatarSeed = resolvedSeed,
                    countryName = resolvedCountry,
                    currencyCode = "INR",
                    currencySymbol = "₹",
                    upiId = resolvedUpi,
                    isDarkTheme = resolvedDark
                )
            )
            if (updatedCurrentUserMembers.isNotEmpty()) {
                dao?.insertMembers(updatedCurrentUserMembers)
            }
        }
    }

    fun selectTab(tabName: String) {
        _uiState.update { it.copy(selectedTabName = tabName) }
    }

    fun openGroupDetail(groupId: String) {
        selectActiveGroup(groupId)
        _uiState.update {
            it.copy(
                selectedTabName = "LEDGERS",
                openedGroupDetailId = groupId,
                returnToGroupDetailId = groupId
            )
        }
    }

    fun closeGroupDetail() {
        _uiState.update {
            it.copy(
                openedGroupDetailId = null,
                returnToGroupDetailId = null
            )
        }
    }

    fun navigateToSubFlow(targetTabName: String, originGroupDetailId: String?) {
        if (originGroupDetailId != null) {
            selectActiveGroup(originGroupDetailId)
        }
        _uiState.update {
            it.copy(
                selectedTabName = targetTabName,
                returnToGroupDetailId = originGroupDetailId ?: it.openedGroupDetailId
            )
        }
    }

    fun finishSubFlowToGroupDetail(explicitGroupId: String? = null) {
        _uiState.update { state ->
            val targetGroup = explicitGroupId ?: state.returnToGroupDetailId ?: state.openedGroupDetailId ?: state.activeGroup?.groupId
            state.copy(
                selectedTabName = "LEDGERS",
                openedGroupDetailId = targetGroup,
                returnToGroupDetailId = null
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

    fun createNewGroupWithContacts(
        name: String,
        iconName: String,
        memberDrafts: List<NewGroupMemberDraft>
    ) {
        val cleanGroup = name.trim().ifEmpty { "New Group" }
        val cleanIcon = iconName.trim().ifEmpty { "Flight" }
        val groupId = "g_${System.currentTimeMillis()}"
        val newGroup = ExpenseGroupEntity(
            groupId = groupId,
            name = cleanGroup,
            currencyCode = "INR",
            iconName = cleanIcon
        )

        val meMember = GroupMemberEntity(
            memberId = "${groupId}_me",
            groupId = groupId,
            name = _uiState.value.currentUserName.ifBlank { "You" },
            avatarSeed = _uiState.value.currentUserSeed,
            upiId = _uiState.value.userUpiId,
            isCurrentUser = true
        )
        val friendMembers = memberDrafts.mapIndexedNotNull { index, draft ->
            val fName = draft.name.trim()
            if (fName.isEmpty()) null else {
                val cleanPhone = cleanIndianTenDigitPhone(draft.cleanPhone)
                val autoUpiId = if (cleanPhone.length == 10) "${cleanPhone}@upi" else ""
                val style = draft.presentationStyle.ifBlank { "Neutral" }
                GroupMemberEntity(
                    memberId = "${groupId}_f$index",
                    groupId = groupId,
                    name = fName,
                    avatarSeed = "$fName|$style",
                    upiId = autoUpiId,
                    isCurrentUser = false
                )
            }
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

    @Suppress("UNUSED_PARAMETER")
    fun createNewGroup(
        name: String,
        currencyCode: String,
        friendNamesCsv: String,
        iconName: String = "Flight"
    ) {
        val drafts = friendNamesCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { NewGroupMemberDraft(name = it) }
        createNewGroupWithContacts(name = name, iconName = iconName, memberDrafts = drafts)
    }

    fun addMemberToActiveGroup(friendName: String) {
        val clean = friendName.trim()
        if (clean.isEmpty()) return
        val groupId = _uiState.value.activeGroupId
        val newMember = GroupMemberEntity(
            memberId = "m_${System.currentTimeMillis()}",
            groupId = groupId,
            name = clean,
            avatarSeed = "$clean|Neutral",
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

    fun addContactsToGroup(groupId: String, contacts: List<DeviceContact>) {
        if (contacts.isEmpty()) return
        val existingMembers = _uiState.value.members.filter { it.groupId == groupId }
        val existingPhones = existingMembers.map { cleanIndianTenDigitPhone(it.upiId) }.filter { it.isNotEmpty() }.toSet()
        val existingNames = existingMembers.map { it.name.trim().lowercase() }.toSet()
        val now = System.currentTimeMillis()
        val newMembers = contacts.mapIndexedNotNull { idx, c ->
            val cleanName = c.name.trim()
            val cleanPhone = cleanIndianTenDigitPhone(c.cleanPhone)
            if (cleanName.isEmpty()) null
            else if (cleanPhone.isNotEmpty() && existingPhones.contains(cleanPhone)) null
            else if (existingNames.contains(cleanName.lowercase())) null
            else {
                GroupMemberEntity(
                    memberId = "${groupId}_c_${now}_$idx",
                    groupId = groupId,
                    name = cleanName,
                    avatarSeed = "$cleanName|Neutral",
                    upiId = if (cleanPhone.length == 10) "${cleanPhone}@upi" else "",
                    isCurrentUser = false
                )
            }
        }
        if (newMembers.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                members = state.members + newMembers,
                statusBannerMessage = "Added ${newMembers.size} contact(s) to group"
            )
        }
        viewModelScope.launch(ioDispatcher) {
            dao?.insertMembers(newMembers)
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
                statusBannerMessage = "Unassigned remainder split equally across all members (Exact Split)"
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
        viewModelScope.launch(ioDispatcher) {
            dao?.deleteSplitsForExpense(expenseId)
            dao?.deleteExpense(expenseId)
        }
    }

    fun undoSettlement(settlementId: String) {
        _uiState.update { curr ->
            val removed = curr.settlements.find { it.settlementId == settlementId }
            curr.copy(
                settlements = curr.settlements.filterNot { it.settlementId == settlementId },
                statusBannerMessage = "Reverted settlement (${removed?.fromMemberName ?: ""} → ${removed?.toMemberName ?: ""})"
            )
        }
        viewModelScope.launch(ioDispatcher) {
            dao?.deleteSettlementById(settlementId)
        }
    }

    data class BatchMemberUpdate(
        val memberId: String,
        val name: String,
        val upiId: String,
        val avatarSeed: String
    )

    fun updateAllGroupMembers(updates: List<BatchMemberUpdate>) {
        if (updates.isEmpty()) return
        val updateMap = updates.associateBy { it.memberId }
        _uiState.update { curr ->
            val nextMembers = curr.members.map { mbr ->
                val upd = updateMap[mbr.memberId]
                if (upd != null) {
                    mbr.copy(
                        name = upd.name.trim().ifEmpty { mbr.name },
                        upiId = upd.upiId.trim(),
                        avatarSeed = upd.avatarSeed.trim().ifEmpty { mbr.avatarSeed }
                    )
                } else mbr
            }
            curr.copy(
                members = nextMembers,
                statusBannerMessage = "Saved ${updates.size} group member profile(s) & UPI IDs"
            )
        }
        viewModelScope.launch(ioDispatcher) {
            updates.forEach { upd ->
                dao?.updateMemberProfile(
                    memberId = upd.memberId,
                    name = upd.name.trim(),
                    upiId = upd.upiId.trim(),
                    avatarSeed = upd.avatarSeed.trim()
                )
            }
        }
    }

    fun editExistingExpense(
        expenseId: String,
        newTitle: String,
        newTotalRupees: Double,
        newPayerId: String,
        selectedMemberIds: List<String>? = null
    ) {
        val state = _uiState.value
        val existing = state.expenses.find { it.expenseId == expenseId } ?: return
        val cleanTitle = newTitle.trim().ifEmpty { existing.title }
        val newTotalCents = kotlin.math.round(newTotalRupees * 100.0).toLong().coerceAtLeast(1L)

        val existingSplits = state.splits.filter { it.expenseId == expenseId && it.finalOwedCents > 0L }
        val groupMembers = state.members.filter { it.groupId == existing.groupId }
        val splitMemberIds = selectedMemberIds?.filter { id -> groupMembers.any { it.memberId == id } }
            ?.ifEmpty { null }
            ?: existingSplits.map { it.memberId }.ifEmpty { groupMembers.map { it.memberId } }

        val chosenMembers = groupMembers.filter { splitMemberIds.contains(it.memberId) }.ifEmpty { groupMembers }
        val equalAllocations = SplitMateMathEngine.splitEquallyZeroDrift(
            newTotalCents,
            chosenMembers.map { it.memberId to it.name }
        )

        val updatedExpense = existing.copy(
            title = cleanTitle,
            payerId = newPayerId.ifBlank { existing.payerId },
            baseSubtotalCents = newTotalCents,
            totalAmountCents = newTotalCents
        )

        val updatedSplits = equalAllocations.mapIndexed { idx, alloc ->
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
                expenses = curr.expenses.map { if (it.expenseId == expenseId) updatedExpense else it },
                splits = curr.splits.filterNot { it.expenseId == expenseId } + updatedSplits,
                statusBannerMessage = "Updated \"$cleanTitle\" across ${chosenMembers.size} members (₹${String.format(Locale.US, "%.2f", newTotalCents / 100.0)})"
            )
        }

        viewModelScope.launch(ioDispatcher) {
            dao?.deleteSplitsForExpense(expenseId)
            dao?.insertExpenseWithSplits(updatedExpense, updatedSplits)
        }
    }

    fun persistLivePnrUpdate(expenseId: String, updatedTitle: String) {
        val currentState = _uiState.value
        val existing = currentState.expenses.find { it.expenseId == expenseId } ?: return
        val cleanNew = updatedTitle.trim()
        if (cleanNew.isBlank() || existing.title == cleanNew) return

        val updatedExpense = existing.copy(title = cleanNew)
        _uiState.update { curr ->
            curr.copy(
                expenses = curr.expenses.map { if (it.expenseId == expenseId) updatedExpense else it }
            )
        }
        viewModelScope.launch(ioDispatcher) {
            dao?.updateExpenseTitleOnly(expenseId, cleanNew)
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

    data class MemberSplitBreakdownRow(
        val memberId: String,
        val displayName: String,
        val isCurrentUser: Boolean,
        val isIncludedInSplit: Boolean,
        val owedCents: Long,
        val plusOneCent: Boolean,
        val formattedShare: String
    )

    data class ExpenseSplitBreakdownSummary(
        val totalMembersInGroup: Int,
        val splittingMembersCount: Int,
        val perPersonHeadlineShare: String,
        val headerLabel: String,
        val rows: List<MemberSplitBreakdownRow>
    )

    companion object {
        /**
         * Resolves the exact per-member split breakdown for an expense using the persisted
         * [ExpenseSplitEntity] records (`allSplits`), so deselected members are NEVER charged
         * and the denominator is strictly `splittingMembersCount` (NOT `groupMembers.size`).
         */
        fun resolveExpenseSplitBreakdown(
            expense: ExpenseEntity,
            groupMembers: List<GroupMemberEntity>,
            allSplits: List<ExpenseSplitEntity>,
            currencySymbol: String = "₹",
            headerPrefix: String = "Split Breakdown"
        ): ExpenseSplitBreakdownSummary {
            val expenseSplits = allSplits.filter { it.expenseId == expense.expenseId && it.finalOwedCents > 0L }
            val splitMap = expenseSplits.associateBy { it.memberId }
            val hasExplicitSplits = expenseSplits.isNotEmpty()

            val splittingCount = if (hasExplicitSplits) {
                expenseSplits.size.coerceAtLeast(1)
            } else {
                groupMembers.size.coerceAtLeast(1)
            }

            val perPersonAvgCents = expense.totalAmountCents / splittingCount
            val perPersonHeadlineShare = "$currencySymbol${String.format(Locale.US, "%.2f", perPersonAvgCents / 100.0)}"

            val headerLabel = if (hasExplicitSplits && splittingCount < groupMembers.size) {
                "$headerPrefix ($splittingCount of ${groupMembers.size} members splitting)"
            } else {
                "$headerPrefix ($splittingCount members)"
            }

            val rows = groupMembers.map { mbr ->
                val displayName = if (mbr.isCurrentUser) "${mbr.name} (You)" else mbr.name
                if (hasExplicitSplits) {
                    val sp = splitMap[mbr.memberId]
                    val owed = sp?.finalOwedCents ?: 0L
                    val included = sp != null && owed > 0L
                    MemberSplitBreakdownRow(
                        memberId = mbr.memberId,
                        displayName = displayName,
                        isCurrentUser = mbr.isCurrentUser,
                        isIncludedInSplit = included,
                        owedCents = owed,
                        plusOneCent = sp?.plusOneCent == true,
                        formattedShare = if (included) {
                            "$currencySymbol${String.format(Locale.US, "%.2f", owed / 100.0)}"
                        } else {
                            "${currencySymbol}0.00 (Excluded)"
                        }
                    )
                } else {
                    MemberSplitBreakdownRow(
                        memberId = mbr.memberId,
                        displayName = displayName,
                        isCurrentUser = mbr.isCurrentUser,
                        isIncludedInSplit = true,
                        owedCents = perPersonAvgCents,
                        plusOneCent = false,
                        formattedShare = perPersonHeadlineShare
                    )
                }
            }

            return ExpenseSplitBreakdownSummary(
                totalMembersInGroup = groupMembers.size,
                splittingMembersCount = splittingCount,
                perPersonHeadlineShare = perPersonHeadlineShare,
                headerLabel = headerLabel,
                rows = rows
            )
        }

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
