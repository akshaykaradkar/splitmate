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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64
import java.util.Locale
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

import com.splitmate.app.data.CloudGroupSyncRepository
import com.splitmate.app.data.PhoneIdentityValidator
import com.splitmate.app.data.UserProfileEntity

data class GroupSyncExportBundle(
    val groupId: String,
    val groupName: String,
    val memberCount: Int,
    val expenseCount: Int,
    val settlementCount: Int,
    val totalSpendCents: Long,
    val syncToken: String,
    val deepLinkUri: String,
    val whatsappShareText: String,
    val compressedBytesSize: Int = syncToken.length,
    val joinCode6: String = ""
) {
    val payloadToken: String get() = syncToken
    val compactToken: String get() = syncToken
    val deepLinkUrl: String get() = deepLinkUri
    val shareMessage: String get() = whatsappShareText
}

data class GroupSyncMergeResult(
    val success: Boolean,
    val groupId: String = "",
    val groupName: String = "",
    val claimedMemberId: String? = null,
    val claimedMemberName: String? = null,
    val mergedMemberCount: Int = 0,
    val mergedExpenseCount: Int = 0,
    val mergedSplitCount: Int = 0,
    val mergedSettlementCount: Int = 0,
    val newlyAddedExpenseCount: Int = 0,
    val newlyAddedSettlementCount: Int = 0,
    val message: String = ""
) {
    val isSuccess: Boolean get() = success
    val resolvedMemberId: String? get() = claimedMemberId
    val resolvedMemberName: String? get() = claimedMemberName
    val statusMessage: String get() = message
}

data class SplitMateUiState(
    val hasRegisteredProfile: Boolean = true,
    val currentUserName: String = "Akshay",
    val currentUserSeed: String = "Akshay|Masculine",
    val currentUserCountry: String = "India",
    val userUpiId: String = "",
    val userPhone: String = "",
    val isDarkTheme: Boolean = false,
    val activeThemeMode: SplitMateThemeMode = SplitMateThemeMode.SUNLIT_BUCKWHEAT,
    val activeCurrencyCode: String = "INR",
    val isOfflineMode: Boolean = false,
    val currencyRates: List<CurrencyRateEntity> = defaultSeedCurrencies(),
    val groups: List<ExpenseGroupEntity> = emptyList(),
    val activeGroupId: String = "g_tahoe",
    val members: List<GroupMemberEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val splits: List<ExpenseSplitEntity> = emptyList(),
    val settlements: List<SettlementEntity> = emptyList(),
    val statusBannerMessage: String? = null,
    val selectedTabName: String = "LEDGERS",
    val openedGroupDetailId: String? = null,
    val returnToGroupDetailId: String? = null,
    val isPhoneVerified: Boolean = false,
    val pinHash: String = "",
    val avatarStyleId: String = "open-peeps",
    val avatarColorPresetId: String = "Buckwheat",
    val avatarGender: String = "Neutral",
    val pendingOtpPhone10: String = "",
    val isOtpChallengeActive: Boolean = false,
    val otpDeliveryStatusText: String = "",
    val otpResendAvailableAtEpochMs: Long = 0L,
    val isCloudSyncing: Boolean = false,
    val cloudRestoreSummary: com.splitmate.app.data.CloudRestoreSummary? = null,
    val discoveredCloudProfile: com.splitmate.app.data.CloudUserProfileRecord? = null,
    val memberPresenceByPhone: Map<String, Long> = emptyMap()
) {
    val activeExpressiveTheme: SplitMateThemeMode
        get() = activeThemeMode

    val avatarSeed: String
        get() = currentUserSeed

    fun isMemberOnline(member: GroupMemberEntity, nowMs: Long = System.currentTimeMillis()): Boolean {
        val myPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(userPhone)
        val mPhone10 = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(member.userPhone, member.upiId)
        if (myPhone10.length == 10 && mPhone10 == myPhone10) return true
        if (myPhone10.isEmpty() && member.isCurrentUser && member.memberId.endsWith("_me")) return true
        if (mPhone10.length != 10) return false
        return com.splitmate.app.data.CloudGroupSyncRepository.isPhoneOnlineNow(mPhone10, memberPresenceByPhone, nowMs)
    }

    fun onlineFriendsCount(groupId: String? = null, nowMs: Long = System.currentTimeMillis()): Int {
        val userPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(userPhone)
        val candidateMembers = if (groupId != null) {
            members.filter { it.groupId == groupId }
        } else {
            members
        }
        val friendPhones = candidateMembers
            .filterNot { it.isCurrentUser }
            .map { com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) }
            .filter { it.length == 10 && it != userPhone10 }
            .toSet()
        return friendPhones.count { p ->
            com.splitmate.app.data.CloudGroupSyncRepository.isPhoneOnlineNow(p, memberPresenceByPhone, nowMs)
        }
    }

    val pendingInviteGroups: List<ExpenseGroupEntity>
        get() {
            val userPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(userPhone)
            return groups.filter { g ->
                if (userPhone10.length == 10 &&
                    com.splitmate.app.data.CloudGroupSyncRepository.isPhoneRemovedFromGroup(null, g.groupId, userPhone10)
                ) {
                    return@filter false
                }
                val gMembers = members.filter { it.groupId == g.groupId }
                val me = gMembers.find {
                    userPhone10.length == 10 &&
                        com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == userPhone10
                } ?: gMembers.find { it.isCurrentUser }
                me?.inviteStatus == "PENDING"
            }
        }

    val pendingInvitedGroups: List<ExpenseGroupEntity>
        get() = pendingInviteGroups

    val declinedInviteGroups: List<ExpenseGroupEntity>
        get() {
            val userPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(userPhone)
            return groups.filter { g ->
                val gMembers = members.filter { it.groupId == g.groupId }
                val me = gMembers.find {
                    userPhone10.length == 10 &&
                        com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == userPhone10
                } ?: gMembers.find { it.isCurrentUser }
                me?.inviteStatus == "DECLINED"
            }
        }

    val activeJoinedGroups: List<ExpenseGroupEntity>
        get() {
            val cleanUserPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(userPhone)
            return groups.filter { g ->
                if (cleanUserPhone10.isNotEmpty() &&
                    com.splitmate.app.data.CloudGroupSyncRepository.isPhoneRemovedFromGroup(null, g.groupId, cleanUserPhone10)
                ) {
                    return@filter false
                }
                val gMembers = members.filter { it.groupId == g.groupId }
                val me = gMembers.find {
                    cleanUserPhone10.isNotEmpty() &&
                        com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == cleanUserPhone10
                } ?: gMembers.find { it.isCurrentUser }
                if (me != null) {
                    me.inviteStatus.equals("JOINED", ignoreCase = true)
                } else {
                    cleanUserPhone10.isEmpty() &&
                        gMembers.isNotEmpty() &&
                        gMembers.any { it.inviteStatus.equals("JOINED", ignoreCase = true) }
                }
            }
        }
    val activeCurrency: CurrencyRateEntity
        get() = currencyRates.find { it.currencyCode == activeCurrencyCode }
            ?: CurrencyRateEntity("INR", "Indian Rupee", "₹", 1.0, baseCurrency = "INR")

    val selectedGroupId: String?
        get() = openedGroupDetailId?.takeIf { it.isNotBlank() } ?: activeGroupId.takeIf { it.isNotBlank() }

    val activeGroup: ExpenseGroupEntity?
        get() = groups.find { it.groupId == activeGroupId } ?: groups.firstOrNull()

    val activeGroupMembers: List<GroupMemberEntity>
        get() {
            val targetGroupId = activeGroup?.groupId ?: activeGroupId
            return syncLocalOwnerAvatarSeeds(
                members = members.filter { it.groupId == targetGroupId },
                rawLocalPhone = userPhone,
                currentUserName = currentUserName,
                currentUserSeed = currentUserSeed
            )
        }
}

internal fun isTrueDeviceOwnerMember(
    member: GroupMemberEntity,
    groupHasPhoneMatchedOwner: Boolean,
    userPhone10: String,
    currentUserName: String
): Boolean {
    val memberPhone10 = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(member.userPhone, member.upiId)
    if (userPhone10.length == 10 && memberPhone10 == userPhone10) {
        return true
    }
    if (groupHasPhoneMatchedOwner || memberPhone10.isNotEmpty()) {
        return false
    }
    if (member.memberId.endsWith("_me") || member.memberId == "m_1" || member.memberId == "m_1_apt") {
        return true
    }
    return member.isCurrentUser &&
        currentUserName.isNotBlank() &&
        member.name.trim().equals(currentUserName.trim(), ignoreCase = true)
}

internal fun isTrueDeviceOwnerMember(
    member: GroupMemberEntity,
    rawLocalPhone: String,
    fallbackPhone: String = ""
): Boolean {
    val userPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(rawLocalPhone)
        .ifEmpty { com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(fallbackPhone) }
    val memberPhone10 = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(member.userPhone, member.upiId)
    if (userPhone10.length == 10 && memberPhone10 == userPhone10) {
        return true
    }
    if (memberPhone10.isNotEmpty()) {
        return false
    }
    return member.memberId.endsWith("_me") ||
        member.memberId == "m_1" ||
        member.memberId == "m_1_apt"
}

internal fun syncLocalOwnerAvatarSeeds(
    members: List<GroupMemberEntity>,
    rawLocalPhone: String,
    currentUserName: String,
    currentUserSeed: String
): List<GroupMemberEntity> {
    if (currentUserSeed.isBlank() || members.isEmpty()) return members
    val userPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(rawLocalPhone)
    val groupsWithPhoneMatch = if (userPhone10.length == 10) {
        members.filter {
            com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == userPhone10
        }.map { it.groupId }.toSet()
    } else {
        emptySet()
    }
    val groupsWithAnyCurrentUser = members.filter { it.isCurrentUser }.map { it.groupId }.toSet()
    return members.map { m ->
        val isTrueOwner = isTrueDeviceOwnerMember(
            member = m,
            groupHasPhoneMatchedOwner = m.groupId in groupsWithPhoneMatch,
            userPhone10 = userPhone10,
            currentUserName = currentUserName
        )
        if (isTrueOwner) {
            val nextIsCurrentUser = if (m.groupId in groupsWithAnyCurrentUser) m.isCurrentUser else true
            if (m.avatarSeed != currentUserSeed || m.isCurrentUser != nextIsCurrentUser) {
                m.copy(avatarSeed = currentUserSeed, isCurrentUser = nextIsCurrentUser)
            } else {
                m
            }
        } else {
            m
        }
    }
}

internal fun syncLocalOwnerAvatarSeeds(
    members: List<GroupMemberEntity>,
    currentUserSeed: String,
    rawLocalPhone: String
): List<GroupMemberEntity> = syncLocalOwnerAvatarSeeds(
    members = members,
    rawLocalPhone = rawLocalPhone,
    currentUserName = "",
    currentUserSeed = currentUserSeed
)

fun defaultSeedCurrencies(): List<CurrencyRateEntity> = listOf(
    CurrencyRateEntity("INR", "Indian Rupee", "₹", 1.0, baseCurrency = "INR")
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
    val statusPillText: String,
    val onlineFriendsCount: Int = 0,
    val memberOnlineFlags: List<Boolean> = emptyList()
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val appContext: android.content.Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        if (dao == null) createInitialSeededState() else createCleanProductionInitialState()
    )
    val uiState: StateFlow<SplitMateUiState> = _uiState.asStateFlow()

    val activeGroupMembers: StateFlow<List<GroupMemberEntity>> = _uiState.map { state ->
        state.activeGroupMembers
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deviceContacts = MutableStateFlow<List<DeviceContact>>(emptyList())
    val deviceContacts: StateFlow<List<DeviceContact>> = _deviceContacts.asStateFlow()

    private val _isLoadingContacts = MutableStateFlow(false)
    val isLoadingContacts: StateFlow<Boolean> = _isLoadingContacts.asStateFlow()

    fun loadDeviceContacts(context: android.content.Context, forceRefresh: Boolean = false) {
        if (!forceRefresh && _deviceContacts.value.isNotEmpty()) return
        viewModelScope.launch(ioDispatcher) {
            _isLoadingContacts.value = true
            val loaded = queryAllDeviceContacts(context.applicationContext)
            _deviceContacts.value = loaded
            _isLoadingContacts.value = false
        }
    }

    val totalBalance: StateFlow<String> = _uiState.map { state ->
        val sym = "₹"
        if (state.activeJoinedGroups.isEmpty() || state.expenses.isEmpty()) {
            "${sym}0.00"
        } else {
            val netCents = computeOverallUserBalanceCents(state)
            when {
                netCents > 0L -> formatIndianRupeesFromCents(netCents, includePlusSign = true, currencySymbol = sym)
                netCents < 0L -> formatIndianRupeesFromCents(netCents, includePlusSign = false, currencySymbol = sym)
                else -> "${sym}0.00"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "₹0.00")

    val activeGroups: StateFlow<List<ActiveGroupCardUiModel>> = _uiState.map { state ->
        val joinedGroups = state.activeJoinedGroups
        if (joinedGroups.isEmpty()) {
            emptyList()
        } else {
            val sym = "₹"
            val nowMs = System.currentTimeMillis()
            joinedGroups.map { group ->
                val groupMembers = syncLocalOwnerAvatarSeeds(
                    members = state.members.filter { it.groupId == group.groupId },
                    rawLocalPhone = state.userPhone,
                    currentUserName = state.currentUserName,
                    currentUserSeed = state.currentUserSeed
                )
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
                val formattedAbs = formatIndianRupeesFromCents(kotlin.math.abs(myNetCents), includePlusSign = false, currencySymbol = sym)
                val badgeText = when {
                    myNetCents > 0L -> "You get back $formattedAbs"
                    myNetCents < 0L -> "You owe $formattedAbs"
                    else -> "All settled up"
                }
                val memberNoun = if (groupMembers.size == 1) "member" else "members"
                val activityDetail = when {
                    groupExpenses.isEmpty() -> "No expenses yet"
                    simplified.isEmpty() -> "Last active today"
                    else -> "${simplified.size} open ${if (simplified.size == 1) "settlement" else "settlements"}"
                }
                val pillText = "${groupMembers.size} $memberNoun · $activityDetail"
                val visibleMembers = groupMembers.take(4)
                val visibleSeeds = visibleMembers.map { m -> m.avatarSeed }
                val visibleOnlineFlags = visibleMembers.map { m -> state.isMemberOnline(m, nowMs) }
                val onlineFriendsInGroup = state.onlineFriendsCount(group.groupId, nowMs)
                val rem = (groupMembers.size - visibleSeeds.size).coerceAtLeast(0)

                ActiveGroupCardUiModel(
                    groupId = group.groupId,
                    name = group.name.toSmartTitleCase(),
                    iconName = group.iconName,
                    memberCount = groupMembers.size,
                    memberSeeds = visibleSeeds,
                    remainingCount = rem,
                    netBalanceCents = myNetCents,
                    formattedBadgeText = badgeText,
                    statusPillText = pillText,
                    onlineFriendsCount = onlineFriendsInGroup,
                    memberOnlineFlags = visibleOnlineFlags
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                val clean10Phone = if (toMember != null) {
                    com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(toMember.userPhone, toMember.upiId)
                } else ""
                val hasPhoneLinked = clean10Phone.length == 10
                val majorStr = String.format(Locale.US, "%.2f", tr.amountCents / 100.0)
                SettlementTransferUiModel(
                    transfer = tr,
                    fromMemberId = tr.fromMemberId,
                    fromName = if (fromMember?.isCurrentUser == true) "You" else tr.fromName,
                    fromSeed = fromMember?.avatarSeed ?: tr.fromName,
                    toMemberId = tr.toMemberId,
                    toName = if (toMember?.isCurrentUser == true) "You" else tr.toName,
                    toSeed = toMember?.avatarSeed ?: tr.toName,
                    upiId = "",
                    cleanPhone = if (hasPhoneLinked) clean10Phone else "",
                    hasLinkedPhone = hasPhoneLinked,
                    amount = majorStr,
                    formattedDisplayAmount = SplitMateMathEngine.formatCurrencyCents(tr.amountCents, sym),
                    isCurrentUserDebtor = (tr.fromMemberId == meMember?.memberId) || (fromMember?.isCurrentUser == true)
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @Volatile
    private var isRoomHydrated: Boolean = (dao == null)
    private val pendingColdStartSyncRequests = mutableListOf<Pair<String, Boolean>>()

    @Volatile
    private var hasExplicitPersistedThemeMode: Boolean = false

    init {
        loadInitialPersistedThemeFromPrefs(appContext)
        if (dao != null) {
            observeRoomDatabase(dao)
        }
        // v2.3.5 (#5): a category created / deleted for the trip is pushed with the ledger.
        com.splitmate.app.ui.category.CustomExpenseCategoryStore.onGroupCategoriesChanged = { gid ->
            runCatching { syncActiveGroupNow(groupId = gid) }
        }
    }

    private data class ParsedAvatarSeedDescriptor(
        val seedKey: String,
        val gender: String,
        val styleId: String,
        val presetId: String
    )

    private fun normalizeAvatarGenderToken(raw: String, fallback: String = "Neutral"): String {
        return when (raw.trim().lowercase(Locale.US)) {
            "male", "masculine", "m", "man", "boy" -> "Male"
            "female", "feminine", "f", "woman", "girl" -> "Female"
            "neutral", "nonbinary", "any", "auto" -> "Neutral"
            else -> when (fallback.trim().lowercase(Locale.US)) {
                "male", "masculine" -> "Male"
                "female", "feminine" -> "Female"
                else -> "Neutral"
            }
        }
    }

    private fun parseAvatarDescriptorFromSeed(
        seed: String,
        defaultSeedKey: String = "Explorer",
        defaultStyle: String = "open-peeps",
        defaultPreset: String = "Buckwheat",
        defaultGender: String = "Neutral"
    ): ParsedAvatarSeedDescriptor {
        val tokens = seed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        val validStyles = setOf(
            "open-peeps", "adventurer", "avataaars", "big-ears", "bottts",
            "dylan", "fun-emoji", "lorelei", "micah", "miniavs",
            "notionists", "personas", "toon-head"
        )
        val validPresets = setOf(
            "Buckwheat", "PastelWall", "BoldPop", "Electric", "Terracotta", "Periwinkle",
            "SageForest", "WarmAmber", "CoralPeach", "OceanMint", "BerryPlum",
            "SlateMist", "RoseQuartz", "MidnightGold",
            "WarmSand", "SageMeadow", "SunsetClay", "LavenderMist", "OceanBreeze",
            "GoldenHour", "BerryBlush", "ForestCanopy", "SlateCloud", "CitrusZest",
            "Rosewater", "CosmicIndigo"
        )
        val validGendersLower = setOf("male", "female", "neutral", "masculine", "feminine")

        val resolvedStyle = tokens.firstOrNull { validStyles.contains(it) }
            ?: defaultStyle.ifBlank { "open-peeps" }
        val resolvedPreset = tokens.firstOrNull { validPresets.contains(it) }
            ?: tokens.getOrNull(3)?.takeIf { !validStyles.contains(it) && !validGendersLower.contains(it.lowercase(Locale.US)) }
            ?: defaultPreset.ifBlank { "Buckwheat" }
        val resolvedGenderToken = tokens.firstOrNull { validGendersLower.contains(it.lowercase(Locale.US)) }
            ?: defaultGender
        val resolvedGender = normalizeAvatarGenderToken(resolvedGenderToken, defaultGender)
        val candidateSeedKey = tokens.firstOrNull {
            !validStyles.contains(it) &&
                !validPresets.contains(it) &&
                !validGendersLower.contains(it.lowercase(Locale.US))
        } ?: defaultSeedKey.replace("|", " ").trim().ifBlank { "Explorer" }

        return ParsedAvatarSeedDescriptor(
            seedKey = candidateSeedKey.replace("|", " ").trim().ifBlank { "Explorer" },
            gender = resolvedGender,
            styleId = resolvedStyle,
            presetId = resolvedPreset
        )
    }

    private fun encodeCanonicalAvatarSeed(
        seedKey: String,
        gender: String,
        styleId: String,
        presetId: String
    ): String {
        val cleanKey = seedKey.replace("|", " ").trim().ifEmpty { "Explorer" }
        val cleanGender = normalizeAvatarGenderToken(gender)
        val cleanStyle = styleId.replace("|", "").trim().ifEmpty { "open-peeps" }
        val cleanPreset = presetId.replace("|", "").trim().ifEmpty { "Buckwheat" }
        return "$cleanKey|$cleanGender|$cleanStyle|$cleanPreset"
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
            ExpenseGroupEntity("g_tahoe", "Lake Tahoe Cabin", "INR", isDemoSeed = true),
            ExpenseGroupEntity("g_mission", "Mission Apt Roommates", "INR", isDemoSeed = true)
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
                lockedExchangeRate = 1.0,
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
            if (roomDao.getCurrencyRate("INR") == null) {
                roomDao.upsertCurrencyRates(defaultSeedCurrencies())
            }
            val initialProfile = roomDao.observeUserProfile().first()
            val initialGroups = roomDao.observeGroups().first()
            val initialMembers = roomDao.observeAllMembers().first()
            val initialExpenses = roomDao.observeAllExpenses().first()
            val initialSplits = roomDao.observeAllSplits().first()
            val initialSettlements = roomDao.observeAllSettlements().first()
            val initialRates = roomDao.observeCurrencyRates().first()

            _uiState.update { curr ->
                val nextActiveGroup = curr.openedGroupDetailId?.takeIf { id -> initialGroups.any { it.groupId == id } }
                    ?: curr.activeGroupId.takeIf { id -> initialGroups.any { it.groupId == id } }
                    ?: initialGroups.firstOrNull()?.groupId.orEmpty()
                val initDesc = if (initialProfile != null) {
                    parseAvatarDescriptorFromSeed(
                        seed = initialProfile.avatarSeed,
                        defaultSeedKey = initialProfile.name,
                        defaultStyle = curr.avatarStyleId,
                        defaultPreset = curr.avatarColorPresetId,
                        defaultGender = curr.avatarGender
                    )
                } else {
                    ParsedAvatarSeedDescriptor(
                        seedKey = curr.currentUserName.ifBlank { "Explorer" },
                        gender = curr.avatarGender,
                        styleId = curr.avatarStyleId,
                        presetId = curr.avatarColorPresetId
                    )
                }
                curr.copy(
                    hasRegisteredProfile = initialProfile != null && initialProfile.name.isNotBlank(),
                    currentUserName = initialProfile?.name ?: curr.currentUserName,
                    currentUserSeed = initialProfile?.avatarSeed ?: curr.currentUserSeed,
                    currentUserCountry = initialProfile?.countryName ?: curr.currentUserCountry,
                    activeCurrencyCode = initialProfile?.currencyCode ?: curr.activeCurrencyCode,
                    userUpiId = initialProfile?.upiId ?: curr.userUpiId,
                    userPhone = initialProfile?.userPhone ?: curr.userPhone,
                    isPhoneVerified = (initialProfile?.isPhoneVerified == true) ||
                        com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(initialProfile?.userPhone.orEmpty()).length == 10,
                    pinHash = initialProfile?.pinHash ?: curr.pinHash,
                    avatarStyleId = initDesc.styleId,
                    avatarColorPresetId = initDesc.presetId,
                    avatarGender = initDesc.gender,
                    isDarkTheme = if (hasExplicitPersistedThemeMode) curr.isDarkTheme else (initialProfile?.isDarkTheme ?: curr.isDarkTheme),
                    activeThemeMode = resolveThemeModeWithProfileDark(
                        currentMode = curr.activeThemeMode,
                        profileDark = initialProfile?.isDarkTheme,
                        respectExplicitThemePref = hasExplicitPersistedThemeMode
                    ),
                    groups = initialGroups,
                    activeGroupId = nextActiveGroup,
                    members = initialMembers,
                    expenses = initialExpenses,
                    splits = initialSplits,
                    settlements = initialSettlements,
                    currencyRates = initialRates.ifEmpty { curr.currencyRates }
                )
            }

            val initPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(initialProfile?.userPhone.orEmpty())
            if (initialProfile != null && initPhone10.length == 10) {
                syncAllGroupsWithCloud(null, silent = true)
            }

            val queuedRequests = synchronized(pendingColdStartSyncRequests) {
                isRoomHydrated = true
                val copy = pendingColdStartSyncRequests.toList()
                pendingColdStartSyncRequests.clear()
                copy
            }
            queuedRequests.forEach { (rawPayload, openAfter) ->
                importAndMergeGroupSyncPayload(
                    rawPayloadOrMessage = rawPayload,
                    openGroupAfterMerge = openAfter
                )
            }

            launch {
                roomDao.observeUserProfile().collect { profile ->
                    if (profile != null) {
                        _uiState.update { curr ->
                            val pDesc = parseAvatarDescriptorFromSeed(
                                seed = profile.avatarSeed,
                                defaultSeedKey = profile.name,
                                defaultStyle = curr.avatarStyleId,
                                defaultPreset = curr.avatarColorPresetId,
                                defaultGender = curr.avatarGender
                            )
                            val profPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(profile.userPhone)
                            val syncedMembers = syncLocalOwnerAvatarSeeds(
                                members = curr.members,
                                rawLocalPhone = profile.userPhone,
                                currentUserName = profile.name,
                                currentUserSeed = profile.avatarSeed
                            )
                            curr.copy(
                                hasRegisteredProfile = profile.name.isNotBlank() || curr.hasRegisteredProfile,
                                currentUserName = profile.name,
                                currentUserSeed = profile.avatarSeed,
                                currentUserCountry = profile.countryName,
                                activeCurrencyCode = profile.currencyCode,
                                userUpiId = profile.upiId,
                                userPhone = profile.userPhone,
                                isPhoneVerified = profile.isPhoneVerified || profPhone10.length == 10,
                                pinHash = profile.pinHash,
                                avatarStyleId = pDesc.styleId,
                                avatarColorPresetId = pDesc.presetId,
                                avatarGender = pDesc.gender,
                                isDarkTheme = if (hasExplicitPersistedThemeMode && curr.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU && !profile.isDarkTheme) false else profile.isDarkTheme,
                                activeThemeMode = resolveThemeModeWithProfileDark(
                                    currentMode = curr.activeThemeMode,
                                    profileDark = profile.isDarkTheme,
                                    respectExplicitThemePref = hasExplicitPersistedThemeMode
                                ),
                                members = syncedMembers
                            )
                        }
                    } else {
                        _uiState.update { it.copy(hasRegisteredProfile = false) }
                    }
                }
            }
            launch {
                roomDao.observeGroups().collect { groups ->
                    _uiState.update { curr ->
                        val nextActiveGroup = curr.openedGroupDetailId?.takeIf { id -> groups.any { it.groupId == id } }
                            ?: curr.activeGroupId.takeIf { id -> groups.any { it.groupId == id } }
                            ?: groups.firstOrNull()?.groupId.orEmpty()
                        curr.copy(groups = groups, activeGroupId = nextActiveGroup)
                    }
                }
            }
            launch {
                roomDao.observeAllMembers().collect { members ->
                    _uiState.update { curr ->
                        val syncedMembers = syncLocalOwnerAvatarSeeds(
                            members = members,
                            rawLocalPhone = curr.userPhone,
                            currentUserName = curr.currentUserName,
                            currentUserSeed = curr.currentUserSeed
                        )
                        curr.copy(members = syncedMembers)
                    }
                }
            }
            launch {
                roomDao.observeAllExpenses().collect { expenses ->
                    _uiState.update { it.copy(expenses = expenses) }
                }
            }
            launch {
                roomDao.observeAllSplits().collect { splits ->
                    _uiState.update { it.copy(splits = splits) }
                }
            }
            launch {
                roomDao.observeAllSettlements().collect { settlements ->
                    _uiState.update { it.copy(settlements = settlements) }
                }
            }
            launch {
                roomDao.observeCurrencyRates().collect { rates ->
                    if (rates.isNotEmpty()) {
                        _uiState.update { it.copy(currencyRates = rates) }
                    }
                }
            }
        }
    }

    fun completeOnboarding(
        name: String,
        countryName: String = "India",
        currencyCode: String = "INR",
        currencySymbol: String = "₹",
        avatarSeed: String = name,
        userPhone: String = ""
    ) {
        val cleanName = name.replace("|", " ").trim().ifEmpty { "Explorer" }
        val currentState = _uiState.value
        val parsedAvatar = parseAvatarDescriptorFromSeed(
            seed = avatarSeed.trim().ifEmpty { cleanName },
            defaultSeedKey = cleanName,
            defaultStyle = currentState.avatarStyleId,
            defaultPreset = currentState.avatarColorPresetId,
            defaultGender = currentState.avatarGender
        )
        val cleanSeed = encodeCanonicalAvatarSeed(
            seedKey = parsedAvatar.seedKey,
            gender = parsedAvatar.gender,
            styleId = parsedAvatar.styleId,
            presetId = parsedAvatar.presetId
        )
        val cleanPhone = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(userPhone)
        val resolvedUpi = if (cleanPhone.length == 10) "${cleanPhone}@upi" else ""
        val resolvedVerified = currentState.isPhoneVerified || cleanPhone.length == 10
        val profile = UserProfileEntity(
            profileId = "me",
            name = cleanName,
            avatarSeed = cleanSeed,
            countryName = countryName,
            currencyCode = currencyCode,
            currencySymbol = currencySymbol,
            upiId = resolvedUpi,
            userPhone = cleanPhone,
            isPhoneVerified = resolvedVerified,
            pinHash = currentState.pinHash,
            isDarkTheme = currentState.isDarkTheme
        )

        val cleanFirstToken = cleanName.split(Regex("\\s+")).firstOrNull()?.lowercase(Locale.US).orEmpty()
        val persistedModifiedMembers = mutableListOf<GroupMemberEntity>()

        _uiState.update { state ->
            val updatedMembersByGroup = state.members.groupBy { it.groupId }.flatMap { (_, groupMembers) ->
                val matchingExistingMember = groupMembers.firstOrNull { m ->
                    (cleanPhone.length == 10 &&
                        com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId) == cleanPhone) ||
                        m.name.trim().equals(cleanName, ignoreCase = true) ||
                        (cleanFirstToken.length >= 3 &&
                            m.name.trim().split(Regex("\\s+")).firstOrNull()?.lowercase(Locale.US) == cleanFirstToken)
                }

                val updatedGroupList = if (matchingExistingMember != null) {
                    groupMembers.map { m ->
                        if (m.memberId == matchingExistingMember.memberId) {
                            m.copy(
                                isCurrentUser = true,
                                upiId = m.upiId.ifBlank { resolvedUpi },
                                userPhone = m.userPhone.ifBlank { cleanPhone },
                                avatarSeed = cleanSeed
                            )
                        } else if (m.isCurrentUser) {
                            m.copy(isCurrentUser = false)
                        } else {
                            m
                        }
                    }
                } else {
                    groupMembers.map { m ->
                        if (m.isCurrentUser) {
                            m.copy(
                                name = cleanName,
                                avatarSeed = cleanSeed,
                                upiId = m.upiId.ifBlank { resolvedUpi },
                                userPhone = m.userPhone.ifBlank { cleanPhone }
                            )
                        } else {
                            m
                        }
                    }
                }
                persistedModifiedMembers.addAll(updatedGroupList.filterIndexed { idx, upd -> upd != groupMembers[idx] })
                updatedGroupList
            }

            state.copy(
                hasRegisteredProfile = true,
                currentUserName = cleanName,
                currentUserSeed = cleanSeed,
                currentUserCountry = countryName,
                activeCurrencyCode = currencyCode,
                userUpiId = resolvedUpi,
                userPhone = cleanPhone,
                isPhoneVerified = resolvedVerified,
                avatarStyleId = parsedAvatar.styleId,
                avatarColorPresetId = parsedAvatar.presetId,
                avatarGender = parsedAvatar.gender,
                members = updatedMembersByGroup,
                statusBannerMessage = "Welcome $cleanName"
            )
        }
        viewModelScope.launch(ioDispatcher) {
            val d = dao
            d?.upsertUserProfile(profile)
            if (persistedModifiedMembers.isNotEmpty()) {
                d?.insertMembers(persistedModifiedMembers)
            }
            if (cleanPhone.length == 10 && d != null) {
                runCatching {
                    com.splitmate.app.data.CloudGroupSyncRepository.pushUserProfileToCloud(
                        profile = profile,
                        avatarStyle = parsedAvatar.styleId,
                        avatarColorPreset = parsedAvatar.presetId
                    )
                    val summary = com.splitmate.app.data.CloudGroupSyncRepository.restoreAndSyncAllForVerifiedPhone(
                        context = null,
                        dao = d,
                        phone10 = cleanPhone
                    )
                    val postSyncMembersToUpdate = d.getAllGroups().flatMap { g ->
                        val gMembers = d.getMembersForGroup(g.groupId)
                        val hasPhoneMatch = cleanPhone.length == 10 && gMembers.any {
                            com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == cleanPhone
                        }
                        gMembers.mapNotNull { m ->
                            if (isTrueDeviceOwnerMember(m, hasPhoneMatch, cleanPhone, cleanName)) {
                                m.copy(name = cleanName, avatarSeed = cleanSeed, upiId = resolvedUpi, userPhone = cleanPhone, isCurrentUser = true)
                            } else null
                        }
                    }
                    if (postSyncMembersToUpdate.isNotEmpty()) {
                        d.insertMembers(postSyncMembersToUpdate)
                    }
                    refreshStateFromDaoSnapshot(
                        d = d,
                        summary = summary,
                        statusMsg = if (summary.discoveredPendingInvitesCount > 0 || summary.restoredJoinedGroupsCount > 0) {
                            "Welcome $cleanName • Synced ${summary.restoredJoinedGroupsCount} group(s), ${summary.discoveredPendingInvitesCount} invite(s)"
                        } else {
                            "Welcome $cleanName"
                        }
                    )
                }
            }
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

    private fun resolveThemeModeWithProfileDark(
        currentMode: SplitMateThemeMode,
        profileDark: Boolean?,
        respectExplicitThemePref: Boolean
    ): SplitMateThemeMode {
        if (profileDark == null) return currentMode
        if (profileDark) return SplitMateThemeMode.WARM_ESPRESSO_NIGHT
        if (respectExplicitThemePref && currentMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) {
            return SplitMateThemeMode.KYOTO_MATCHA_YUZU
        }
        return if (currentMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) {
            SplitMateThemeMode.KYOTO_MATCHA_YUZU
        } else {
            SplitMateThemeMode.SUNLIT_BUCKWHEAT
        }
    }

    @JvmOverloads
    fun loadInitialPersistedThemeFromPrefs(context: android.content.Context? = appContext) {
        val ctx = context?.applicationContext ?: context ?: appContext ?: return
        runCatching {
            val prefs = ctx.getSharedPreferences(PREFS_NAME_SPLITMATE, android.content.Context.MODE_PRIVATE) ?: return
            val rawModeId = prefs.getString(PREF_KEY_EXPRESSIVE_THEME_MODE, null)
            val hasDarkKey = prefs.contains(PREF_KEY_IS_DARK_THEME)
            val savedDark = if (hasDarkKey) prefs.getBoolean(PREF_KEY_IS_DARK_THEME, false) else _uiState.value.isDarkTheme
            if (!rawModeId.isNullOrBlank()) {
                val resolved = SplitMateThemeMode.fromId(rawModeId, fallbackDark = savedDark)
                hasExplicitPersistedThemeMode = true
                SplitMateThemeState.activeThemeMode = resolved
                com.splitmate.app.SplitMateTheme.isDark = resolved.isDark
                _uiState.update {
                    it.copy(
                        activeThemeMode = resolved,
                        isDarkTheme = resolved.isDark
                    )
                }
            } else if (hasDarkKey) {
                val resolved = SplitMateThemeMode.fromId(null, fallbackDark = savedDark)
                hasExplicitPersistedThemeMode = true
                SplitMateThemeState.activeThemeMode = resolved
                com.splitmate.app.SplitMateTheme.isDark = savedDark
                _uiState.update {
                    it.copy(
                        activeThemeMode = resolved,
                        isDarkTheme = savedDark
                    )
                }
            }
        }
    }

    private fun persistThemeModeToPrefs(mode: SplitMateThemeMode, context: android.content.Context? = appContext) {
        val ctx = context?.applicationContext ?: context ?: appContext ?: return
        runCatching {
            val prefs = ctx.getSharedPreferences(PREFS_NAME_SPLITMATE, android.content.Context.MODE_PRIVATE) ?: return
            prefs.edit()
                .putString(PREF_KEY_EXPRESSIVE_THEME_MODE, mode.id)
                .putBoolean(PREF_KEY_IS_DARK_THEME, mode.isDark)
                .apply()
        }
    }

    @JvmOverloads
    fun setExpressiveThemeMode(
        mode: SplitMateThemeMode,
        context: android.content.Context? = null
    ) {
        hasExplicitPersistedThemeMode = true
        SplitMateThemeState.activeThemeMode = mode
        com.splitmate.app.SplitMateTheme.isDark = mode.isDark
        _uiState.update {
            it.copy(
                activeThemeMode = mode,
                isDarkTheme = mode.isDark
            )
        }
        persistThemeModeToPrefs(mode, context ?: appContext)
        viewModelScope.launch(ioDispatcher) {
            val existing = dao?.getUserProfile()
            if (existing != null && existing.isDarkTheme != mode.isDark) {
                dao?.upsertUserProfile(existing.copy(isDarkTheme = mode.isDark))
            }
        }
    }

    @JvmOverloads
    fun selectExpressiveTheme(
        mode: SplitMateThemeMode,
        context: android.content.Context? = null
    ) = setExpressiveThemeMode(mode = mode, context = context)

    @JvmOverloads
    fun cycleExpressiveThemeMode(
        context: android.content.Context? = null
    ): SplitMateThemeMode {
        val modes = SplitMateThemeMode.values()
        val currentIdx = modes.indexOf(_uiState.value.activeThemeMode).coerceAtLeast(0)
        val nextMode = modes[(currentIdx + 1) % modes.size]
        setExpressiveThemeMode(mode = nextMode, context = context)
        return nextMode
    }

    fun toggleDarkTheme(isDark: Boolean) {
        hasExplicitPersistedThemeMode = true
        val targetMode = if (isDark) {
            SplitMateThemeMode.WARM_ESPRESSO_NIGHT
        } else {
            val currMode = _uiState.value.activeThemeMode
            if (currMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) {
                SplitMateThemeMode.KYOTO_MATCHA_YUZU
            } else {
                SplitMateThemeMode.SUNLIT_BUCKWHEAT
            }
        }
        SplitMateThemeState.activeThemeMode = targetMode
        com.splitmate.app.SplitMateTheme.isDark = isDark
        _uiState.update {
            it.copy(
                isDarkTheme = isDark,
                activeThemeMode = targetMode
            )
        }
        persistThemeModeToPrefs(targetMode, appContext)
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
        val extractedPhone10 = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(newUpiId, newUpiId)
        // v2.3.5 (#7): the stored upiId is resolved per member below via
        // MemberProfileEditRules.resolveUpiId so a real VPA is never clobbered by "<phone>@upi".
        val effectiveSeed = newAvatarSeed?.trim()?.ifEmpty { cleanName } ?: cleanName
        var updatedTargetMember: GroupMemberEntity? = null
        _uiState.update { state ->
            val updatedMembers = state.members.map { m ->
                if (m.memberId == memberId) {
                    val finalPhone = extractedPhone10.ifBlank {
                        if (newUpiId.isBlank()) "" else m.userPhone
                    }
                    val nextInviteStatus = when {
                        m.isCurrentUser -> "JOINED"
                        finalPhone.length == 10 && (m.userPhone.isBlank() || m.inviteStatus.isBlank() || m.userPhone != finalPhone) -> "PENDING"
                        else -> m.inviteStatus.ifBlank { "JOINED" }
                    }
                    val updated = m.copy(
                        name = cleanName,
                        avatarSeed = effectiveSeed,
                        upiId = com.splitmate.app.MemberProfileEditRules.resolveUpiId(
                            existingUpiId = m.upiId,
                            phoneInput = newUpiId
                        ),
                        userPhone = finalPhone,
                        inviteStatus = nextInviteStatus
                    )
                    updatedTargetMember = updated
                    updated
                } else m
            }
            val phoneLabel = if (extractedPhone10.length == 10) "+91 $extractedPhone10" else "offline profile"
            state.copy(
                members = updatedMembers,
                statusBannerMessage = "Updated $cleanName ($phoneLabel)"
            )
        }
        val immediateTarget = updatedTargetMember
        if (immediateTarget != null && extractedPhone10.length == 10) {
            com.splitmate.app.data.CloudGroupSyncRepository.unTombstonePhones(
                context = null,
                groupId = immediateTarget.groupId,
                phonesToRestore = listOf(extractedPhone10),
                memberIdsToRestore = listOf(immediateTarget.memberId)
            )
        }
        viewModelScope.launch(ioDispatcher) {
            val d = dao
            val target = updatedTargetMember
            if (d != null && target != null) {
                d.insertMembers(listOf(target))
                val normUserPhone = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(_uiState.value.userPhone)
                runCatching {
                    com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                        context = null,
                        dao = d,
                        groupId = target.groupId,
                        localUserPhone10 = normUserPhone,
                        localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                        forceIndexPush = true
                    )
                }
            } else {
                val fallbackUpi = target?.upiId ?: com.splitmate.app.MemberProfileEditRules.resolveUpiId(
                    existingUpiId = "",
                    phoneInput = newUpiId
                )
                d?.updateMemberProfile(memberId, cleanName, fallbackUpi, effectiveSeed)
            }
        }
    }

    private var lastExpenseCommitMs: Long = 0L
    private var lastExpenseCommitSignature: String = ""
    private var lastGroupCreateMs: Long = 0L

    private data class InferredExpenseMetadata(
        val expenseCategory: String,
        val travelPnr: String,
        val providerName: String,
        val scheduledAtEpochMs: Long?
    )

    private fun parseTravelDateTimeToEpochMs(rawCandidate: String): Long? {
        val cleaned = rawCandidate
            .replace("•", " ")
            .replace(Regex("""^(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)[a-z]*,?\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)[a-z]*\b""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (cleaned.isBlank()) return null

        var parsedTimeHourMin: Pair<Int, Int>? = null
        val m12 = Regex("""\b(0?[1-9]|1[0-2]):([0-5]\d)\s*(AM|PM)\b""", RegexOption.IGNORE_CASE).find(cleaned)
        val m24 = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)(?:\s*hrs)?\b""", RegexOption.IGNORE_CASE).find(cleaned)
        if (m12 != null) {
            val h12 = m12.groupValues[1].toIntOrNull() ?: 0
            val min = m12.groupValues[2].toIntOrNull() ?: 0
            val ampm = m12.groupValues[3].uppercase(Locale.US)
            val h24 = when {
                ampm == "PM" && h12 < 12 -> h12 + 12
                ampm == "AM" && h12 == 12 -> 0
                else -> h12
            }
            parsedTimeHourMin = h24 to min
        } else if (m24 != null) {
            val h24 = m24.groupValues[1].toIntOrNull() ?: 0
            val min = m24.groupValues[2].toIntOrNull() ?: 0
            parsedTimeHourMin = h24 to min
        }

        val dateOnly = cleaned
            .replace(Regex("""\b(0?[1-9]|1[0-2]):([0-5]\d)\s*(AM|PM)\b""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\b([01]?\d|2[0-3]):([0-5]\d)(?:\s*hrs)?\b""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)[a-z]*,?\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trim(',', '-', '•')
            .trim()
        if (dateOnly.isBlank()) return null

        val fullPatterns = listOf(
            "dd MMM yyyy", "d MMM yyyy", "dd MMMM yyyy", "d MMMM yyyy",
            "yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "dd-MMM-yyyy",
            "dd-MMM-yy", "MMM dd, yyyy", "MMM d, yyyy", "dd MMM yy"
        )
        val shortPatterns = listOf("dd MMM", "d MMM", "dd MMMM", "d MMMM", "MMM dd", "MMM d")
        val fallbackYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

        var parsedDateMillis: Long? = null
        for (pattern in fullPatterns) {
            val parsed = runCatching {
                java.text.SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(dateOnly)
            }.getOrNull()
            if (parsed != null) {
                parsedDateMillis = parsed.time
                break
            }
        }
        if (parsedDateMillis == null) {
            for (shortPattern in shortPatterns) {
                val parsed = runCatching {
                    java.text.SimpleDateFormat(shortPattern, Locale.US).apply { isLenient = false }.parse(dateOnly)
                }.getOrNull()
                if (parsed != null) {
                    val c = java.util.Calendar.getInstance().apply {
                        time = parsed
                        set(java.util.Calendar.YEAR, fallbackYear)
                    }
                    parsedDateMillis = c.timeInMillis
                    break
                }
            }
        }
        val baseDate = parsedDateMillis ?: return null
        return java.util.Calendar.getInstance().apply {
            timeInMillis = baseDate
            if (parsedTimeHourMin != null) {
                set(java.util.Calendar.HOUR_OF_DAY, parsedTimeHourMin.first)
                set(java.util.Calendar.MINUTE, parsedTimeHourMin.second)
                set(java.util.Calendar.SECOND, 0)
            } else {
                set(java.util.Calendar.HOUR_OF_DAY, 12)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
            }
        }.timeInMillis
    }

    private fun inferStructuredExpenseMetadata(
        rawTitle: String,
        explicitCategory: String? = null,
        explicitPnr: String? = null,
        explicitProvider: String? = null,
        explicitScheduledAt: Long? = null
    ): InferredExpenseMetadata {
        val trimmed = rawTitle.trim()
        val lower = trimmed.replace(Regex("""train/flight|flight/train""", RegexOption.IGNORE_CASE), "train").lowercase(Locale.US)
        val parsedTicket = runCatching { extractTravelTicketFromTitle(trimmed) }.getOrNull()

        val bracketPnr = Regex("""\[PNR:([A-Za-z0-9]{6,12})\]""", RegexOption.IGNORE_CASE)
            .find(trimmed)?.groupValues?.getOrNull(1)?.uppercase(Locale.US)
        val labelledPnr = Regex("""\bPNR:\s*([A-Za-z0-9]{6,10})\b""", RegexOption.IGNORE_CASE)
            .find(trimmed)?.groupValues?.getOrNull(1)?.uppercase(Locale.US)
        val pnrDigits = Regex("""\b(\d{10})\b""").find(trimmed)?.groupValues?.getOrNull(1)
        val resolvedPnr = explicitPnr?.trim()?.takeIf { it.isNotEmpty() }?.uppercase(Locale.US)
            ?: parsedTicket?.pnr?.trim()?.takeIf { it.isNotEmpty() }?.uppercase(Locale.US)
            ?: bracketPnr
            ?: labelledPnr
            ?: pnrDigits
            ?: ""

        val hasTenDigitPnr = (resolvedPnr.length == 10 && resolvedPnr.all { it.isDigit() }) || pnrDigits != null
        val hasFiveDigitTrainNo = Regex("""^\d{5}\b""").containsMatchIn(parsedTicket?.trainOrFlightNo?.trim().orEmpty()) ||
            Regex("""\btrain\s+\d{5}\b""").containsMatchIn(lower)
        val hasExplicitAirlineOrFlightNo = Regex("""\b(indigo|air india|akasa|spicejet|vistara|airasia|alliance air|star air|fly91|emirates|qatar|lufthansa)\b""").containsMatchIn(lower) ||
            Regex("""\b(6e|ai|ix|qp|sg|uk|i5|9i|s5)[\s\-]?\d{2,4}\b""").containsMatchIn(lower)
        val isTrainPdfMislabelledAsFlight = trimmed.startsWith("Flight", ignoreCase = true) &&
            parsedTicket?.trainOrFlightNo.isNullOrBlank() &&
            !hasExplicitAirlineOrFlightNo &&
            (trimmed.startsWith("Flight  (") || trimmed.startsWith("Flight ("))

        val resolvedCategory = when {
            !explicitCategory.isNullOrBlank() -> explicitCategory.trim().uppercase(Locale.US)
            hasTenDigitPnr ||
                hasFiveDigitTrainNo ||
                isTrainPdfMislabelledAsFlight ||
                Regex("""\b(train|irctc|express|shatabdi|rajdhani|vande bharat|vande|duronto|sleeper|berth|coach|3a|2a|1a|3e|2s)\b""").containsMatchIn(lower) -> "TRAIN"
            isFlightTicketExpense(trimmed, parsedTicket) ||
                hasExplicitAirlineOrFlightNo ||
                Regex("""\b(flight|airfare|boarding pass)\b""").containsMatchIn(lower) -> "FLIGHT"
            parsedTicket != null && parsedTicket.hasTicketMetadata -> "TRAIN"
            Regex("""\b(hotel|resort|stay|villa|airbnb|hostel|cottage|lodge|homestay|check-in|nights|room)\b""").containsMatchIn(lower) -> "STAY"
            Regex("""\b(rental|enfield|moped|scooter|bike|scooty|two wheeler|cycle|kayak|coracle)\b""").containsMatchIn(lower) -> "RENTAL"
            Regex("""\b(cab|taxi|uber|ola|rapido|auto|rickshaw|innova|transfer|pickup|drop|bus)\b""").containsMatchIn(lower) -> "CAB"
            else -> "OTHER"
        }

        val resolvedProvider = when {
            !explicitProvider.isNullOrBlank() -> explicitProvider.trim()
            parsedTicket?.trainOrFlightNo?.isNotBlank() == true -> parsedTicket.trainOrFlightNo.trim()
            resolvedCategory == "TRAIN" -> "IRCTC"
            resolvedCategory == "FLIGHT" -> trimmed.substringBefore("(").substringBefore("|").trim().take(40)
            else -> ""
        }

        val resolvedScheduledAt = explicitScheduledAt?.takeIf { it > 0L } ?: run {
            val depCandidate = listOfNotNull(
                parsedTicket?.departureInfo?.takeIf { it.isNotBlank() },
                parsedTicket?.departureDate?.takeIf { it.isNotBlank() }
            ).firstOrNull()
            if (depCandidate.isNullOrBlank()) {
                null
            } else {
                parseTravelDateTimeToEpochMs(depCandidate)
            }
        }

        return InferredExpenseMetadata(
            expenseCategory = resolvedCategory,
            travelPnr = resolvedPnr,
            providerName = resolvedProvider,
            scheduledAtEpochMs = resolvedScheduledAt
        )
    }

    /**
     * Commits a Quick Equal Expense from QuickExpenseScreen, FlightExpenseReviewScreen, or PnrExpenseReviewScreen:
     * Strictly divides [totalAmountCents] equally among [selectedMemberIds] with Payer-First Largest Remainder (`0.00¢` drift).
     */
    fun commitQuickEqualExpense(
        title: String,
        totalAmountCents: Long,
        selectedMemberIds: List<String>,
        payerMemberId: String? = null,
        explicitCategory: String? = null,
        explicitTravelPnr: String? = null,
        explicitProviderName: String? = null,
        explicitScheduledAtEpochMs: Long? = null,
        categoryRef: String? = null
    ) {
        if (totalAmountCents <= 0L) return
        val state = _uiState.value
        val groupMembers = state.activeGroupMembers
        if (groupMembers.isEmpty()) return

        val nowMs = System.currentTimeMillis()
        val commitSig = "${state.activeGroupId}|${title.trim()}|$totalAmountCents|${selectedMemberIds.sorted().joinToString(",")}"
        if (commitSig == lastExpenseCommitSignature && (nowMs - lastExpenseCommitMs) < 800L) {
            return
        }
        lastExpenseCommitMs = nowMs
        lastExpenseCommitSignature = commitSig

        val inferred = inferStructuredExpenseMetadata(
            rawTitle = title,
            explicitCategory = explicitCategory,
            explicitPnr = explicitTravelPnr,
            explicitProvider = explicitProviderName,
            explicitScheduledAt = explicitScheduledAtEpochMs
        )

        // Guard against adding the exact same 6-char Flight PNR or 10-digit Train PNR twice to the group ledger
        val detectedPnr = inferred.travelPnr.takeIf { it.isNotBlank() }

        if (!detectedPnr.isNullOrBlank()) {
            val wordBoundaryPnrRegex = Regex("""\b${Regex.escape(detectedPnr)}\b""", RegexOption.IGNORE_CASE)
            val existingExp = state.expenses.firstOrNull { exp ->
                exp.groupId == state.activeGroupId && (
                    exp.travelPnr.equals(detectedPnr, ignoreCase = true) ||
                        exp.title.contains("[PNR:$detectedPnr]", ignoreCase = true) ||
                        wordBoundaryPnrRegex.containsMatchIn(exp.title)
                    )
            }
            if (existingExp != null) {
                // v2.3.5 (#4): a member who may not edit the earlier expense must not alter it here.
                if (!canCurrentUserModifyExpense(existingExp, state)) {
                    _uiState.update { curr ->
                        curr.copy(statusBannerMessage = "PNR $detectedPnr is already logged. ${com.splitmate.app.ExpenseEditPermission.READ_ONLY_REASON}")
                    }
                    return
                }
                // Do NOT count as a new expense — update the earlier logged expense in place with pure Long paise
                editExistingExpense(
                    expenseId = existingExp.expenseId,
                    newTitle = title,
                    newTotalRupees = totalAmountCents / 100.0,
                    newPayerId = payerMemberId ?: existingExp.payerId,
                    selectedMemberIds = selectedMemberIds,
                    newTotalCentsOverride = totalAmountCents
                )
                _uiState.update { curr ->
                    curr.copy(statusBannerMessage = "PNR $detectedPnr already added — updated earlier expense without duplicating")
                }
                return
            }
        }

        val chosenMembers = groupMembers.filter { selectedMemberIds.contains(it.memberId) }
            .ifEmpty { groupMembers }
        val currentUser = groupMembers.find { it.isCurrentUser }
        val payer = groupMembers.find { it.memberId == payerMemberId }
            ?: currentUser
            ?: groupMembers.first()

        val lockedRate = state.activeCurrency.rateFromBase
        val syncStatus = if (dao != null || state.isOfflineMode) "PENDING" else "SYNCED"
        val expenseId = "exp_${System.currentTimeMillis()}"
        com.splitmate.app.data.CloudGroupSyncRepository.setGroupPendingCloudPush(null, state.activeGroupId, true)

        val equalAllocations = SplitMateMathEngine.splitEquallyZeroDrift(
            totalCents = totalAmountCents,
            members = chosenMembers.map { it.memberId to it.name },
            payerId = payer.memberId,
            currentUserId = currentUser?.memberId
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
            expenseCategory = inferred.expenseCategory,
            travelPnr = inferred.travelPnr,
            providerName = inferred.providerName,
            scheduledAtEpochMs = inferred.scheduledAtEpochMs,
            syncStatus = syncStatus,
            // v2.3.5 (#4): who logged it (creator may edit/delete). Null when the user has no phone.
            createdByPhone = com.splitmate.app.ExpenseEditPermission.creatorStampOf(
                currentUserPhone10(state.activeGroupId, state)
            ),
            categoryRef = categoryRef?.takeIf { it.isNotBlank() }
        )
        com.splitmate.app.ui.category.CustomExpenseCategoryStore.ensurePublished(appContext, state.activeGroupId, categoryRef)

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
            val d = dao
            if (d != null) {
                com.splitmate.app.data.ExpenseEditClock.record(appContext, listOf(expenseEntity.expenseId)) // v2.4.0 P1: version by edit time
                d.insertExpenseWithSplits(expenseEntity, splitEntities)
                val normUserPhone = resolveEffectiveUserPhone10(d)
                runCatching {
                    com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                        context = null,
                        dao = d,
                        groupId = expenseEntity.groupId,
                        localUserPhone10 = normUserPhone,
                        localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                        isLocalMutation = true
                    )
                    refreshStateFromDaoSnapshot(d = d)
                }
            }
        }
    }

    fun logExpense(
        title: String,
        totalAmountCents: Long,
        selectedMemberIds: List<String>,
        payerMemberId: String? = null,
        expenseCategory: String = "OTHER",
        travelPnr: String = "",
        providerName: String = "",
        scheduledAtEpochMs: Long? = null
    ) {
        commitQuickEqualExpense(
            title = title,
            totalAmountCents = totalAmountCents,
            selectedMemberIds = selectedMemberIds,
            payerMemberId = payerMemberId,
            explicitCategory = expenseCategory.takeIf { it.isNotBlank() && it != "OTHER" },
            explicitTravelPnr = travelPnr,
            explicitProviderName = providerName,
            explicitScheduledAtEpochMs = scheduledAtEpochMs
        )
    }

    fun wrapUpTrip(groupId: String) {
        val state = _uiState.value
        if (!runCatching { isUserGroupOrganizer(groupId, state) }.getOrDefault(false)) return
        val record = com.splitmate.app.data.TripLifecycleResolver.buildWrapUpRecord(
            existing = com.splitmate.app.data.GroupLedgerExtrasStore.tripLifecycle(groupId),
            nowEpochMs = System.currentTimeMillis(),
            endedByPhone = currentUserPhone10(groupId, state)
        )
        com.splitmate.app.data.GroupLedgerExtrasStore.setTripLifecycle(null, groupId, record)
        syncActiveGroupNow(groupId = groupId)
    }

    /**
     * v2.3.5 (#1, C1): organizer-only. Works even when no lifecycle record exists yet (e.g. the
     * card showed a return-ticket / quiet-trip suggestion); the reopen stamp suppresses that
     * suggestion so it doesn't immediately reappear.
     */
    fun reopenTrip(groupId: String) {
        val state = _uiState.value
        if (!runCatching { isUserGroupOrganizer(groupId, state) }.getOrDefault(false)) return
        val record = com.splitmate.app.data.TripLifecycleResolver.buildReopenRecord(
            existing = com.splitmate.app.data.GroupLedgerExtrasStore.tripLifecycle(groupId),
            nowEpochMs = System.currentTimeMillis()
        )
        com.splitmate.app.data.GroupLedgerExtrasStore.setTripLifecycle(null, groupId, record)
        syncActiveGroupNow(groupId = groupId)
    }

    fun setOfflineMode(offline: Boolean) {
        _uiState.update { it.copy(isOfflineMode = offline) }
    }

    fun updateUserProfile(newName: String, newPhone: String, newSeedOrCurrency: String, newUpiId: String? = null) {
        val cleanName = newName.replace("|", " ").trim().ifEmpty { "Akshay" }
        val cleanPhone = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(newPhone)
        var resolvedSeed = ""
        var resolvedUpi = ""
        var resolvedDark = false
        var resolvedCountry = "India"
        var resolvedStyleId = "open-peeps"
        var resolvedPresetId = "Buckwheat"
        var resolvedIsPhoneVerified = false
        var resolvedPinHash = ""
        var updatedCurrentUserMembers = emptyList<GroupMemberEntity>()

        _uiState.update { state ->
            val currentDesc = parseAvatarDescriptorFromSeed(
                seed = state.currentUserSeed,
                defaultSeedKey = state.currentUserName.ifBlank { cleanName },
                defaultStyle = state.avatarStyleId,
                defaultPreset = state.avatarColorPresetId,
                defaultGender = state.avatarGender
            )
            val parsedArg = parseAvatarDescriptorFromSeed(
                seed = newSeedOrCurrency.ifBlank { state.currentUserSeed },
                defaultSeedKey = cleanName,
                defaultStyle = currentDesc.styleId,
                defaultPreset = currentDesc.presetId,
                defaultGender = currentDesc.gender
            )
            val resolvedSeedKey = if (
                newSeedOrCurrency.contains('|') &&
                parsedArg.seedKey.isNotBlank() &&
                parsedArg.seedKey != state.currentUserName &&
                parsedArg.seedKey != "INR"
            ) {
                parsedArg.seedKey
            } else {
                cleanName
            }
            val updatedSeed = encodeCanonicalAvatarSeed(
                seedKey = resolvedSeedKey,
                gender = parsedArg.gender,
                styleId = parsedArg.styleId,
                presetId = parsedArg.presetId
            )
            val finalUpi = newUpiId?.trim() ?: state.userUpiId
            resolvedSeed = updatedSeed
            resolvedUpi = finalUpi
            resolvedDark = state.isDarkTheme
            resolvedCountry = state.currentUserCountry
            resolvedStyleId = parsedArg.styleId
            resolvedPresetId = parsedArg.presetId
            resolvedIsPhoneVerified = state.isPhoneVerified
            resolvedPinHash = state.pinHash

            val updatedMembers = state.members.map { m ->
                if (isTrueDeviceOwnerMember(m, cleanPhone, state.userPhone)) {
                    m.copy(
                        name = cleanName,
                        avatarSeed = updatedSeed,
                        upiId = finalUpi,
                        userPhone = cleanPhone
                    )
                } else {
                    m
                }
            }
            updatedCurrentUserMembers = updatedMembers.filter { isTrueDeviceOwnerMember(it, cleanPhone, state.userPhone) }
            state.copy(
                currentUserName = cleanName,
                currentUserSeed = updatedSeed,
                userUpiId = finalUpi,
                userPhone = cleanPhone,
                avatarStyleId = parsedArg.styleId,
                avatarColorPresetId = parsedArg.presetId,
                avatarGender = parsedArg.gender,
                activeCurrencyCode = "INR",
                members = updatedMembers,
                statusBannerMessage = "Saved profile & payment preferences"
            )
        }

        viewModelScope.launch(ioDispatcher) {
            val existingProfile = dao?.getUserProfile()
            val profileEntity = UserProfileEntity(
                profileId = "me",
                name = cleanName,
                avatarSeed = resolvedSeed,
                countryName = resolvedCountry,
                currencyCode = "INR",
                currencySymbol = "₹",
                upiId = resolvedUpi,
                userPhone = cleanPhone,
                isPhoneVerified = existingProfile?.isPhoneVerified ?: resolvedIsPhoneVerified,
                pinHash = existingProfile?.pinHash ?: resolvedPinHash,
                isDarkTheme = resolvedDark,
                createdAt = existingProfile?.createdAt ?: System.currentTimeMillis()
            )
            dao?.upsertUserProfile(profileEntity)
            if (updatedCurrentUserMembers.isNotEmpty()) {
                dao?.insertMembers(updatedCurrentUserMembers)
            }
            com.splitmate.app.data.CloudGroupSyncRepository.pushUserProfileToCloud(
                profile = profileEntity,
                avatarStyle = resolvedStyleId,
                avatarColorPreset = resolvedPresetId
            )
            syncAllGroupsWithCloud(null)
        }
    }

    private suspend fun dispatchOnMain(block: () -> Unit) {
        runCatching {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                block()
            }
        }.getOrElse {
            block()
        }
    }

    private suspend fun refreshStateFromDaoSnapshot(
        d: SplitMateDao,
        summary: com.splitmate.app.data.CloudRestoreSummary? = null,
        statusMsg: String? = null
    ) {
        val profile = d.getUserProfile()
        val allGroups = d.getAllGroups()
        val allMembers = allGroups.flatMap { d.getMembersForGroup(it.groupId) }
        val allExpenses = allGroups
            .flatMap { d.getExpensesForGroup(it.groupId) }
            .sortedWith(compareByDescending<ExpenseEntity> { it.createdAt }.thenByDescending { it.expenseId })
        val allSplits = allGroups.flatMap { d.getSplitsForGroup(it.groupId) }
        val allSettlements = allGroups.flatMap { d.getSettlementsForGroup(it.groupId) }
        _uiState.update { curr ->
            val nextActiveGroup = curr.openedGroupDetailId?.takeIf { id -> allGroups.any { it.groupId == id } }
                ?: curr.activeGroupId.takeIf { id -> allGroups.any { it.groupId == id } }
                ?: allGroups.firstOrNull()?.groupId.orEmpty()
            val pDesc = if (profile != null) {
                parseAvatarDescriptorFromSeed(
                    seed = profile.avatarSeed,
                    defaultSeedKey = profile.name,
                    defaultStyle = curr.avatarStyleId,
                    defaultPreset = curr.avatarColorPresetId,
                    defaultGender = curr.avatarGender
                )
            } else {
                ParsedAvatarSeedDescriptor(
                    seedKey = curr.currentUserName.ifBlank { "Explorer" },
                    gender = curr.avatarGender,
                    styleId = curr.avatarStyleId,
                    presetId = curr.avatarColorPresetId
                )
            }
            val resolvedSeed = profile?.avatarSeed ?: curr.currentUserSeed
            val resolvedPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(profile?.userPhone ?: curr.userPhone)
            val syncedAllMembers = syncLocalOwnerAvatarSeeds(allMembers, resolvedSeed, resolvedPhone10)
            val mergedPresence = if (summary != null && summary.memberPresenceByPhone.isNotEmpty()) {
                val combined = curr.memberPresenceByPhone.toMutableMap()
                summary.memberPresenceByPhone.forEach { (phone, ts) ->
                    val prev = combined[phone] ?: 0L
                    if (ts > prev) combined[phone] = ts
                }
                combined
            } else {
                curr.memberPresenceByPhone
            }
            curr.copy(
                hasRegisteredProfile = (profile != null && profile.name.isNotBlank()) || curr.hasRegisteredProfile,
                currentUserName = profile?.name ?: curr.currentUserName,
                currentUserSeed = resolvedSeed,
                userUpiId = profile?.upiId ?: curr.userUpiId,
                userPhone = profile?.userPhone ?: curr.userPhone,
                isPhoneVerified = (profile?.isPhoneVerified ?: curr.isPhoneVerified) || resolvedPhone10.length == 10,
                pinHash = profile?.pinHash ?: curr.pinHash,
                avatarStyleId = pDesc.styleId,
                avatarColorPresetId = pDesc.presetId,
                avatarGender = pDesc.gender,
                groups = allGroups,
                activeGroupId = nextActiveGroup,
                members = syncedAllMembers,
                expenses = allExpenses,
                splits = allSplits,
                settlements = allSettlements,
                isCloudSyncing = false,
                cloudRestoreSummary = summary ?: curr.cloudRestoreSummary,
                memberPresenceByPhone = mergedPresence,
                statusBannerMessage = statusMsg ?: curr.statusBannerMessage
            )
        }
    }

    fun lookupCloudProfileForPhone(
        rawPhone: String,
        onFound: (com.splitmate.app.data.CloudUserProfileRecord?) -> Unit = {}
    ) {
        val phone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(rawPhone)
        if (!com.splitmate.app.data.PhoneIdentityValidator.isValidIndianMobile10(phone10)) {
            _uiState.update { it.copy(discoveredCloudProfile = null) }
            onFound(null)
            return
        }
        if (dao == null) {
            onFound(_uiState.value.discoveredCloudProfile)
            return
        }
        viewModelScope.launch(ioDispatcher) {
            val record = com.splitmate.app.data.CloudGroupSyncRepository.fetchUserProfileFromCloud(phone10)
            _uiState.update { it.copy(discoveredCloudProfile = record) }
            dispatchOnMain { onFound(record) }
        }
    }

    fun requestPhoneOtp(
        context: android.content.Context?,
        rawPhone: String
    ): com.splitmate.app.data.OtpDispatchResult? {
        val res = com.splitmate.app.data.PhoneOtpAuthManager.sendOtp(context, rawPhone) { deliveryMsg ->
            _uiState.update {
                it.copy(
                    otpDeliveryStatusText = deliveryMsg,
                    statusBannerMessage = deliveryMsg
                )
            }
        }
        if (res != null) {
            _uiState.update {
                it.copy(
                    pendingOtpPhone10 = res.phone10,
                    isOtpChallengeActive = true,
                    otpDeliveryStatusText = res.statusMessage,
                    otpResendAvailableAtEpochMs = res.resendAvailableAtEpochMs,
                    statusBannerMessage = res.statusMessage
                )
            }
            lookupCloudProfileForPhone(res.phone10)
        } else {
            _uiState.update {
                it.copy(statusBannerMessage = "Enter a valid 10-digit Indian mobile number")
            }
        }
        return res
    }

    fun verifyPinAndRestoreCloud(
        context: android.content.Context?,
        rawPhone: String,
        enteredPin4: String = "",
        fallbackUserName: String = "",
        fallbackUpiId: String = "",
        avatarStyleId: String = "open-peeps",
        avatarColorPresetId: String = "Buckwheat",
        avatarGender: String = "",
        customSeedKey: String = "",
        preferLocalAvatarChoice: Boolean = false,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        com.splitmate.app.data.CloudGroupSyncRepository.init(context)
        val phone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(rawPhone)
        if (!com.splitmate.app.data.PhoneIdentityValidator.isValidIndianMobile10(phone10)) {
            onResult(false, "Enter a valid 10-digit Indian mobile number")
            return
        }
        val cleanPin4 = enteredPin4.trim()
        if (cleanPin4.isNotEmpty() && (cleanPin4.length != 4 || !cleanPin4.all { it.isDigit() })) {
            onResult(false, "Enter your 4-digit Security PIN")
            return
        }
        _uiState.update { it.copy(isCloudSyncing = true) }
        viewModelScope.launch(ioDispatcher) {
            val localProfile = dao?.getUserProfile()
            val cachedRemoteProfile = _uiState.value.discoveredCloudProfile?.takeIf { it.phone10 == phone10 }
            val localStatePinHash = _uiState.value.pinHash.takeIf {
                it.isNotBlank() && com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(_uiState.value.userPhone) == phone10
            }
            val remoteProfile = cachedRemoteProfile ?: if (dao != null) {
                com.splitmate.app.data.CloudGroupSyncRepository.fetchUserProfileFromCloud(phone10)
            } else {
                null
            }
            val expectedPinHash = remoteProfile?.pinHash?.takeIf { it.isNotBlank() }
                ?: localProfile?.pinHash?.takeIf { it.isNotBlank() && com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(localProfile.userPhone) == phone10 }
                ?: localStatePinHash.orEmpty()

            val hasExisting4DigitPin = expectedPinHash.isNotBlank() &&
                com.splitmate.app.data.PhoneOtpAuthManager.is4DigitPinHash(phone10, expectedPinHash)

            val finalPinHash = if (hasExisting4DigitPin) {
                if (cleanPin4.length != 4) {
                    val errMsg = "Enter your 4-digit Security PIN"
                    _uiState.update { it.copy(isCloudSyncing = false, statusBannerMessage = errMsg) }
                    dispatchOnMain { onResult(false, errMsg) }
                    return@launch
                }
                val pinValid = com.splitmate.app.data.PhoneOtpAuthManager.verifyPin(phone10, cleanPin4, expectedPinHash)
                if (!pinValid) {
                    val errMsg = "Incorrect 4-digit Security PIN for +91 $phone10."
                    _uiState.update { it.copy(isCloudSyncing = false, statusBannerMessage = errMsg) }
                    dispatchOnMain { onResult(false, errMsg) }
                    return@launch
                }
                expectedPinHash
            } else if (cleanPin4.length == 4) {
                // Brand-new account or upgrading a legacy pre-PIN profile to a 4-digit Security PIN
                com.splitmate.app.data.PhoneOtpAuthManager.hashPin(phone10, cleanPin4)
            } else {
                // PIN is optional during initial registration when no 4-digit PIN exists yet
                expectedPinHash.ifBlank {
                    com.splitmate.app.data.PhoneOtpAuthManager.getOrCreateDeviceOwnershipToken(context, phone10)
                }
            }

            val resolvedName = (
                if (preferLocalAvatarChoice && fallbackUserName.trim().isNotBlank() && fallbackUserName.trim() != "Explorer") {
                    fallbackUserName.trim()
                } else {
                    remoteProfile?.name?.takeIf { it.isNotBlank() && it != "Explorer" }
                        ?: fallbackUserName.trim().takeIf { it.isNotBlank() }
                        ?: localProfile?.name?.takeIf { it.isNotBlank() }
                        ?: "Explorer"
                }
            ).replace("|", " ").trim()

            val explicitGender = if (avatarGender.isNotBlank()) {
                com.splitmate.app.ui.AvatarGender.fromId(avatarGender).id
            } else {
                _uiState.value.avatarGender
            }

            val updatedSeed = if (preferLocalAvatarChoice || remoteProfile?.avatarSeed.isNullOrBlank()) {
                val resolvedSeedKey = customSeedKey.replace("|", " ").trim().ifBlank { resolvedName }
                val resolvedStyle = avatarStyleId.ifBlank { "open-peeps" }
                val resolvedPreset = avatarColorPresetId.ifBlank { "PastelWall" }
                encodeCanonicalAvatarSeed(
                    seedKey = resolvedSeedKey,
                    gender = explicitGender,
                    styleId = resolvedStyle,
                    presetId = resolvedPreset
                )
            } else {
                val resolvedStyle = remoteProfile?.avatarStyle?.takeIf { it.isNotBlank() } ?: avatarStyleId
                val resolvedPreset = remoteProfile?.avatarColorPreset?.takeIf { it.isNotBlank() } ?: avatarColorPresetId
                val parsedRemoteAvatar = parseAvatarDescriptorFromSeed(
                    seed = remoteProfile?.avatarSeed?.takeIf { it.isNotBlank() }
                        ?: localProfile?.avatarSeed.orEmpty(),
                    defaultSeedKey = resolvedName,
                    defaultStyle = resolvedStyle,
                    defaultPreset = resolvedPreset,
                    defaultGender = explicitGender
                )
                encodeCanonicalAvatarSeed(
                    seedKey = parsedRemoteAvatar.seedKey.ifBlank { resolvedName },
                    gender = parsedRemoteAvatar.gender,
                    styleId = parsedRemoteAvatar.styleId,
                    presetId = parsedRemoteAvatar.presetId
                )
            }

            val finalAvatarDesc = parseAvatarDescriptorFromSeed(
                seed = updatedSeed,
                defaultSeedKey = resolvedName,
                defaultStyle = avatarStyleId,
                defaultPreset = avatarColorPresetId,
                defaultGender = explicitGender
            )

            val resolvedUpi = remoteProfile?.upiVpa?.takeIf { it.isNotBlank() }
                ?: fallbackUpiId.trim().takeIf { it.isNotBlank() }
                ?: "$phone10@upi"

            val statusText = if (hasExisting4DigitPin) {
                "Welcome back $resolvedName (+91 $phone10)"
            } else {
                "Verified +91 $phone10 with 4-Digit Security PIN"
            }

            val d = dao
            if (d != null) {
                val profileEntity = UserProfileEntity(
                    profileId = "me",
                    name = resolvedName,
                    avatarSeed = updatedSeed,
                    countryName = localProfile?.countryName ?: _uiState.value.currentUserCountry,
                    currencyCode = "INR",
                    currencySymbol = "₹",
                    upiId = resolvedUpi,
                    userPhone = phone10,
                    isPhoneVerified = true,
                    pinHash = finalPinHash,
                    isDarkTheme = localProfile?.isDarkTheme ?: _uiState.value.isDarkTheme,
                    createdAt = localProfile?.createdAt ?: System.currentTimeMillis()
                )
                d.upsertUserProfile(profileEntity)
                com.splitmate.app.data.CloudGroupSyncRepository.pushUserProfileToCloud(
                    profile = profileEntity,
                    avatarStyle = finalAvatarDesc.styleId,
                    avatarColorPreset = finalAvatarDesc.presetId
                )
                val summary = com.splitmate.app.data.CloudGroupSyncRepository.restoreAndSyncAllForVerifiedPhone(
                    context = context,
                    dao = d,
                    phone10 = phone10
                )
                val postSyncMembersToUpdate = d.getAllGroups().flatMap { g ->
                    d.getMembersForGroup(g.groupId).mapNotNull { m ->
                        if (isTrueDeviceOwnerMember(m, phone10, localProfile?.userPhone.orEmpty())) {
                            m.copy(name = resolvedName, avatarSeed = updatedSeed, upiId = resolvedUpi, userPhone = phone10, isCurrentUser = true)
                        } else null
                    }
                }
                if (postSyncMembersToUpdate.isNotEmpty()) {
                    d.insertMembers(postSyncMembersToUpdate)
                }
                refreshStateFromDaoSnapshot(
                    d = d,
                    summary = summary,
                    statusMsg = statusText
                )
            } else {
                _uiState.update { curr ->
                    val updatedMembers = curr.members.map { m ->
                        if (isTrueDeviceOwnerMember(m, phone10, curr.userPhone)) {
                            m.copy(name = resolvedName, avatarSeed = updatedSeed, upiId = resolvedUpi, userPhone = phone10, isCurrentUser = true)
                        } else m
                    }
                    curr.copy(
                        hasRegisteredProfile = true,
                        currentUserName = resolvedName,
                        currentUserSeed = updatedSeed,
                        userUpiId = resolvedUpi,
                        userPhone = phone10,
                        isPhoneVerified = true,
                        pinHash = finalPinHash,
                        avatarStyleId = finalAvatarDesc.styleId,
                        avatarColorPresetId = finalAvatarDesc.presetId,
                        avatarGender = finalAvatarDesc.gender,
                        members = updatedMembers,
                        isCloudSyncing = false,
                        statusBannerMessage = statusText
                    )
                }
            }
            dispatchOnMain {
                onResult(true, statusText)
            }
        }
    }

    fun verifyPhoneOtpAndSyncCloud(
        context: android.content.Context?,
        rawPhone: String,
        enteredOtp: String,
        userName: String,
        upiId: String,
        avatarStyleId: String = "open-peeps",
        avatarColorPresetId: String = "Buckwheat",
        optionalPin4: String = "",
        avatarGender: String = "",
        customSeedKey: String = "",
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val verified = com.splitmate.app.data.PhoneOtpAuthManager.verifyOtp(context, rawPhone, enteredOtp)
        if (!verified) {
            val errMsg = "Invalid or expired verification code"
            _uiState.update { it.copy(statusBannerMessage = errMsg) }
            onResult(false, errMsg)
            return
        }

        val phone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(rawPhone)
        val newPinHash = if (optionalPin4.trim().length >= 4) {
            com.splitmate.app.data.PhoneOtpAuthManager.hashPin(phone10, optionalPin4)
        } else {
            com.splitmate.app.data.PhoneOtpAuthManager.getOrCreateDeviceOwnershipToken(context, phone10)
        }
        val typedName = userName.replace("|", " ").trim()
        val cleanName = typedName.ifEmpty {
            _uiState.value.discoveredCloudProfile?.name?.takeIf { it.isNotBlank() }
                ?: _uiState.value.currentUserName.ifEmpty { "Explorer" }
        }.replace("|", " ").trim()
        val cleanHandle = cleanName.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "").ifEmpty { "explorer" }
        val cleanUpi = upiId.trim().ifEmpty {
            _uiState.value.discoveredCloudProfile?.upiVpa?.takeIf { it.isNotBlank() }
                ?: _uiState.value.userUpiId.ifEmpty {
                    if (phone10.length == 10) "${phone10}@upi" else "${cleanHandle}@okaxis"
                }
        }
        val resolvedGender = normalizeAvatarGenderToken(
            raw = avatarGender.ifBlank { _uiState.value.avatarGender },
            fallback = _uiState.value.avatarGender
        )
        val resolvedSeedKey = customSeedKey.replace("|", " ").trim().ifEmpty { cleanName }
        val updatedSeed = encodeCanonicalAvatarSeed(
            seedKey = resolvedSeedKey,
            gender = resolvedGender,
            styleId = avatarStyleId,
            presetId = avatarColorPresetId
        )
        var updatedCurrentUserMembers = emptyList<GroupMemberEntity>()

        _uiState.update { state ->
            val updatedMembers = state.members.map { m ->
                if (isTrueDeviceOwnerMember(m, phone10, state.userPhone)) {
                    m.copy(
                        name = cleanName,
                        avatarSeed = updatedSeed,
                        upiId = cleanUpi,
                        userPhone = phone10,
                        isCurrentUser = true
                    )
                } else {
                    m
                }
            }
            updatedCurrentUserMembers = updatedMembers.filter { isTrueDeviceOwnerMember(it, phone10, state.userPhone) }
            state.copy(
                hasRegisteredProfile = true,
                currentUserName = cleanName,
                currentUserSeed = updatedSeed,
                userUpiId = cleanUpi,
                userPhone = phone10,
                isPhoneVerified = true,
                pinHash = newPinHash.ifEmpty { state.discoveredCloudProfile?.pinHash ?: state.pinHash },
                avatarStyleId = avatarStyleId,
                avatarColorPresetId = avatarColorPresetId,
                avatarGender = resolvedGender,
                isOtpChallengeActive = false,
                otpDeliveryStatusText = "",
                otpResendAvailableAtEpochMs = 0L,
                isCloudSyncing = true,
                members = updatedMembers,
                statusBannerMessage = "Verified +91 $phone10"
            )
        }

        val d = dao
        if (d == null) {
            _uiState.update { curr ->
                val remainingGroups = curr.groups.filterNot { g ->
                    g.isDemoSeed && curr.expenses.count { it.groupId == g.groupId } <= 3
                }
                val remainingGroupIds = remainingGroups.map { it.groupId }.toSet()
                curr.copy(
                    groups = remainingGroups,
                    members = curr.members.filter { remainingGroupIds.contains(it.groupId) },
                    expenses = curr.expenses.filter { remainingGroupIds.contains(it.groupId) },
                    isCloudSyncing = false,
                    cloudRestoreSummary = com.splitmate.app.data.CloudRestoreSummary(
                        restoredJoinedGroupsCount = remainingGroups.size,
                        discoveredPendingInvitesCount = 0
                    )
                )
            }
            onResult(true, "Verified +91 $phone10")
            return
        }

        viewModelScope.launch(ioDispatcher) {
            val remoteProfile = _uiState.value.discoveredCloudProfile?.takeIf { it.phone10 == phone10 }
                ?: com.splitmate.app.data.CloudGroupSyncRepository.fetchUserProfileFromCloud(phone10)
            val existingProfile = d.getUserProfile()
            val effectiveName = (if (typedName.isNotEmpty() && typedName != "Explorer") {
                typedName
            } else {
                remoteProfile?.name?.takeIf { it.isNotBlank() } ?: cleanName
            }).replace("|", " ").trim()
            val effectiveStyle = avatarStyleId.takeIf { it.isNotBlank() }
                ?: remoteProfile?.avatarStyle?.takeIf { it.isNotBlank() }
                ?: "open-peeps"
            val effectivePreset = avatarColorPresetId.takeIf { it.isNotBlank() }
                ?: remoteProfile?.avatarColorPreset?.takeIf { it.isNotBlank() }
                ?: "PastelWall"
            val parsedRemoteAvatar = if (remoteProfile != null && remoteProfile.avatarSeed.isNotBlank()) {
                parseAvatarDescriptorFromSeed(
                    seed = remoteProfile.avatarSeed,
                    defaultSeedKey = effectiveName,
                    defaultStyle = effectiveStyle,
                    defaultPreset = effectivePreset,
                    defaultGender = resolvedGender
                )
            } else null
            val effectiveGender = if (avatarGender.isNotBlank() || parsedRemoteAvatar == null) {
                resolvedGender
            } else {
                parsedRemoteAvatar.gender
            }
            val effectiveSeedKey = when {
                customSeedKey.isNotBlank() -> customSeedKey.replace("|", " ").trim()
                parsedRemoteAvatar != null && parsedRemoteAvatar.seedKey != remoteProfile?.name -> parsedRemoteAvatar.seedKey
                else -> effectiveName
            }
            val effectiveUpi = if (upiId.trim().isNotEmpty()) {
                upiId.trim()
            } else {
                remoteProfile?.upiVpa?.takeIf { it.isNotBlank() } ?: cleanUpi
            }
            val effectivePinHash = when {
                optionalPin4.trim().length >= 4 && newPinHash.isNotEmpty() -> newPinHash
                !remoteProfile?.pinHash.isNullOrBlank() -> remoteProfile!!.pinHash
                !existingProfile?.pinHash.isNullOrBlank() -> existingProfile!!.pinHash
                newPinHash.isNotEmpty() -> newPinHash
                else -> ""
            }
            val effectiveSeed = encodeCanonicalAvatarSeed(
                seedKey = effectiveSeedKey,
                gender = effectiveGender,
                styleId = effectiveStyle,
                presetId = effectivePreset
            )

            val profileEntity = UserProfileEntity(
                profileId = "me",
                name = effectiveName,
                avatarSeed = effectiveSeed,
                countryName = existingProfile?.countryName ?: _uiState.value.currentUserCountry,
                currencyCode = "INR",
                currencySymbol = "₹",
                upiId = effectiveUpi,
                userPhone = phone10,
                isPhoneVerified = true,
                pinHash = effectivePinHash,
                isDarkTheme = existingProfile?.isDarkTheme ?: _uiState.value.isDarkTheme,
                createdAt = existingProfile?.createdAt ?: System.currentTimeMillis()
            )
            d.upsertUserProfile(profileEntity)
            if (updatedCurrentUserMembers.isNotEmpty()) {
                d.insertMembers(updatedCurrentUserMembers.map {
                    it.copy(name = effectiveName, avatarSeed = effectiveSeed, upiId = effectiveUpi, userPhone = phone10)
                })
            }
            com.splitmate.app.data.CloudGroupSyncRepository.pushUserProfileToCloud(
                profile = profileEntity,
                avatarStyle = effectiveStyle,
                avatarColorPreset = effectivePreset
            )
            val summary = com.splitmate.app.data.CloudGroupSyncRepository.restoreAndSyncAllForVerifiedPhone(
                context = context,
                dao = d,
                phone10 = phone10
            )
            val postSyncMembersToUpdate = d.getAllGroups().flatMap { g ->
                d.getMembersForGroup(g.groupId).mapNotNull { m ->
                    if (isTrueDeviceOwnerMember(m, phone10, existingProfile?.userPhone.orEmpty())) {
                        m.copy(name = effectiveName, avatarSeed = effectiveSeed, upiId = effectiveUpi, userPhone = phone10, isCurrentUser = true)
                    } else null
                }
            }
            if (postSyncMembersToUpdate.isNotEmpty()) {
                d.insertMembers(postSyncMembersToUpdate)
            }
            refreshStateFromDaoSnapshot(
                d = d,
                summary = summary,
                statusMsg = "Verified +91 $phone10"
            )
            dispatchOnMain {
                onResult(true, "Verified +91 $phone10")
            }
        }
    }

    fun resetPinAfterOtpVerified(phone10: String, newPin4: String) {
        val normPhone = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(phone10)
            .ifEmpty { com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(_uiState.value.userPhone) }
        if (normPhone.isEmpty()) return
        val newPinHash = com.splitmate.app.data.PhoneOtpAuthManager.hashPin(normPhone, newPin4)
        _uiState.update {
            it.copy(
                pinHash = newPinHash,
                statusBannerMessage = "4-digit recovery PIN updated"
            )
        }
        viewModelScope.launch(ioDispatcher) {
            val d = dao ?: return@launch
            val existing = d.getUserProfile()
            if (existing != null) {
                val updated = existing.copy(pinHash = newPinHash)
                d.upsertUserProfile(updated)
                com.splitmate.app.data.CloudGroupSyncRepository.pushUserProfileToCloud(
                    profile = updated,
                    avatarStyle = _uiState.value.avatarStyleId,
                    avatarColorPreset = _uiState.value.avatarColorPresetId
                )
            }
        }
    }

    private suspend fun resolveEffectiveUserPhone10(d: SplitMateDao?): String {
        val fromState = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(_uiState.value.userPhone)
        if (fromState.length == 10) return fromState
        val fromDao = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(d?.getUserProfile()?.userPhone.orEmpty())
        if (fromDao.length == 10) return fromDao
        val currentMember = _uiState.value.members.firstOrNull { it.isCurrentUser && it.userPhone.isNotBlank() }
        return com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(
            currentMember?.userPhone.orEmpty(),
            currentMember?.upiId.orEmpty()
        )
    }

    fun syncActiveGroupNow(
        context: android.content.Context? = null,
        groupId: String? = null,
        silent: Boolean = true
    ) {
        val d = dao ?: return
        val state = _uiState.value
        val targetGroupId = groupId?.takeIf { it.isNotBlank() }
            ?: state.openedGroupDetailId?.takeIf { it.isNotBlank() }
            ?: state.activeGroupId
        if (targetGroupId.isBlank()) return
        if (!silent) {
            _uiState.update { it.copy(isCloudSyncing = true) }
        }
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                val phone10 = resolveEffectiveUserPhone10(d)
                val syncedDoc = com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                    context = context,
                    dao = d,
                    groupId = targetGroupId,
                    localUserPhone10 = phone10,
                    localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                    forceIndexPush = !silent,
                    isLocalMutation = false
                )
                val presenceSummary = if (syncedDoc != null) {
                    com.splitmate.app.data.CloudRestoreSummary(
                        restoredJoinedGroupsCount = 1,
                        discoveredPendingInvitesCount = 0,
                        memberPresenceByPhone = syncedDoc.memberPresenceByPhone
                    )
                } else null
                refreshStateFromDaoSnapshot(
                    d = d,
                    summary = presenceSummary,
                    statusMsg = if (!silent) "Synced latest trip expenses & balances" else null
                )
            }.onFailure {
                if (!silent) {
                    _uiState.update { it.copy(isCloudSyncing = false) }
                }
            }
        }
    }

    fun syncAllGroupsWithCloud(
        context: android.content.Context? = null,
        silent: Boolean = false
    ) {
        val d = dao ?: return
        val state = _uiState.value
        val previousGroupIds = state.groups.map { it.groupId }.toSet()
        if (!silent) {
            _uiState.update { it.copy(isCloudSyncing = true) }
        }
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                val phone10 = resolveEffectiveUserPhone10(d)
                val hasNonSeedGroups = d.getAllGroups().any { !it.isDemoSeed }
                if (phone10.length != 10 && !hasNonSeedGroups) {
                    if (!silent) {
                        _uiState.update { it.copy(isCloudSyncing = false) }
                    }
                    return@launch
                }
                val summary = com.splitmate.app.data.CloudGroupSyncRepository.restoreAndSyncAllForVerifiedPhone(
                    context = context,
                    dao = d,
                    phone10 = phone10
                )
                val postGroups = d.getAllGroups()
                val newlyDiscoveredCount = postGroups.count { !previousGroupIds.contains(it.groupId) }
                val bannerMsg = when {
                    !silent -> "Cloud sync complete (${summary.restoredJoinedGroupsCount} active, ${summary.discoveredPendingInvitesCount} pending)"
                    newlyDiscoveredCount > 0 -> "Discovered $newlyDiscoveredCount new group invite(s)"
                    else -> null
                }
                refreshStateFromDaoSnapshot(
                    d = d,
                    summary = summary,
                    statusMsg = bannerMsg
                )
            }.onFailure {
                if (!silent) {
                    _uiState.update { it.copy(isCloudSyncing = false) }
                }
            }
        }
    }

    fun performSilentAutoCloudSync(context: android.content.Context? = null) {
        syncAllGroupsWithCloud(context = context, silent = true)
    }

    /**
     * Automatically triggered by `ConnectivityManager.NetworkCallback` whenever internet connectivity
     * changes on the device. When the phone reconnects (`isConnected == true`), any expenses, edits,
     * deletions, or settlements queued while offline (`syncStatus == "PENDING"` or `pending_push_<groupId>`)
     * are pushed automatically in the background without requiring any manual button press.
     */
    fun onNetworkConnectivityChanged(
        context: android.content.Context?,
        isConnected: Boolean
    ) {
        val wasOffline = _uiState.value.isOfflineMode
        _uiState.update { it.copy(isOfflineMode = !isConnected) }
        if (isConnected && _uiState.value.hasRegisteredProfile) {
            val activeOrOpenedGroupId = _uiState.value.openedGroupDetailId?.takeIf { it.isNotBlank() }
                ?: _uiState.value.activeGroupId
            if (activeOrOpenedGroupId.isNotBlank()) {
                syncActiveGroupNow(context = context, groupId = activeOrOpenedGroupId, silent = true)
            }
            if (wasOffline || _uiState.value.expenses.any { it.syncStatus == "PENDING" } || _uiState.value.settlements.any { it.syncStatus == "PENDING" }) {
                performSilentAutoCloudSync(context)
            }
        }
    }

    /**
     * Triggered in < 1 second whenever a live `ntfy.sh` message event arrives on any of the user's
     * active group topics or phone index topic.
     */
    fun handleLiveCloudTopicEvent(
        context: android.content.Context?,
        changedTopic: String
    ) {
        if (changedTopic.isBlank() || !_uiState.value.hasRegisteredProfile) return
        val matchedGroup = _uiState.value.groups.firstOrNull { g ->
            com.splitmate.app.data.CloudGroupSyncRepository.groupTopicForGroupId(g.groupId) == changedTopic
        }
        if (matchedGroup != null) {
            syncActiveGroupNow(context = context, groupId = matchedGroup.groupId, silent = true)
        } else {
            performSilentAutoCloudSync(context)
        }
    }

    fun acceptGroupInvite(context: android.content.Context?, groupId: String) {
        val state = _uiState.value
        val userPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(state.userPhone)
        val groupName = state.groups.find { it.groupId == groupId }?.name ?: "Group"
        var updatedGroupMembers = emptyList<GroupMemberEntity>()

        _uiState.update { curr ->
            val gMembers = curr.members.filter { it.groupId == groupId }
            val targetMemberId = gMembers.find { it.isCurrentUser }?.memberId
                ?: gMembers.find {
                    userPhone10.length == 10 &&
                        com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(it.userPhone) == userPhone10
                }?.memberId
            val nextMembers = curr.members.map { m ->
                if (m.groupId == groupId && m.memberId == targetMemberId) {
                    m.copy(inviteStatus = "JOINED", isCurrentUser = true)
                } else {
                    m
                }
            }
            updatedGroupMembers = nextMembers.filter { it.groupId == groupId }
            curr.copy(
                members = nextMembers,
                activeGroupId = groupId,
                statusBannerMessage = "Joined \"$groupName\""
            )
        }

        viewModelScope.launch(ioDispatcher) {
            val d = dao ?: return@launch
            if (updatedGroupMembers.isNotEmpty()) {
                d.insertMembers(updatedGroupMembers)
            }
            val effectivePhone10 = userPhone10.ifBlank { resolveEffectiveUserPhone10(d) }
            runCatching {
                com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                    context = context,
                    dao = d,
                    groupId = groupId,
                    localUserPhone10 = effectivePhone10,
                    localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                    forceIndexPush = true,
                    isLocalMutation = true
                )
                refreshStateFromDaoSnapshot(d = d)
            }
        }
    }

    fun declineGroupInvite(context: android.content.Context?, groupId: String) {
        val state = _uiState.value
        val userPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(state.userPhone)
        val groupName = state.groups.find { it.groupId == groupId }?.name ?: "Group"
        var updatedGroupMembers = emptyList<GroupMemberEntity>()

        _uiState.update { curr ->
            val gMembers = curr.members.filter { it.groupId == groupId }
            val targetMemberId = gMembers.find { it.isCurrentUser }?.memberId
                ?: gMembers.find {
                    userPhone10.length == 10 &&
                        com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(it.userPhone) == userPhone10
                }?.memberId
            val nextMembers = curr.members.map { m ->
                if (m.groupId == groupId && m.memberId == targetMemberId) {
                    m.copy(inviteStatus = "DECLINED")
                } else {
                    m
                }
            }
            updatedGroupMembers = nextMembers.filter { it.groupId == groupId }
            curr.copy(
                members = nextMembers,
                statusBannerMessage = "Declined invite to \"$groupName\""
            )
        }

        viewModelScope.launch(ioDispatcher) {
            val d = dao ?: return@launch
            if (updatedGroupMembers.isNotEmpty()) {
                d.insertMembers(updatedGroupMembers)
            }
            val effectivePhone10 = userPhone10.ifBlank { resolveEffectiveUserPhone10(d) }
            runCatching {
                com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                    context = context,
                    dao = d,
                    groupId = groupId,
                    localUserPhone10 = effectivePhone10,
                    localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                    forceIndexPush = true,
                    isLocalMutation = true
                )
                refreshStateFromDaoSnapshot(d = d)
            }
        }
    }

    fun restoreDeclinedGroupInvite(context: android.content.Context?, groupId: String) {
        val state = _uiState.value
        val userPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(state.userPhone)
        val groupName = state.groups.find { it.groupId == groupId }?.name ?: "Group"
        var updatedGroupMembers = emptyList<GroupMemberEntity>()

        _uiState.update { curr ->
            val gMembers = curr.members.filter { it.groupId == groupId }
            val targetMemberId = gMembers.find { it.isCurrentUser }?.memberId
                ?: gMembers.find {
                    userPhone10.length == 10 &&
                        com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(it.userPhone) == userPhone10
                }?.memberId
            val nextMembers = curr.members.map { m ->
                if (m.groupId == groupId && m.memberId == targetMemberId) {
                    m.copy(inviteStatus = "JOINED", isCurrentUser = true)
                } else {
                    m
                }
            }
            updatedGroupMembers = nextMembers.filter { it.groupId == groupId }
            curr.copy(
                members = nextMembers,
                activeGroupId = groupId,
                statusBannerMessage = "Restored and joined \"$groupName\""
            )
        }

        viewModelScope.launch(ioDispatcher) {
            val d = dao ?: return@launch
            if (updatedGroupMembers.isNotEmpty()) {
                d.insertMembers(updatedGroupMembers)
            }
            val effectivePhone10 = userPhone10.ifBlank { resolveEffectiveUserPhone10(d) }
            runCatching {
                com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                    context = context,
                    dao = d,
                    groupId = groupId,
                    localUserPhone10 = effectivePhone10,
                    localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                    forceIndexPush = true,
                    isLocalMutation = true
                )
                refreshStateFromDaoSnapshot(d = d)
            }
        }
    }

    fun reassignDeclinedMemberSharesEqually(
        context: android.content.Context?,
        groupId: String,
        declinedMemberId: String
    ) {
        val state = _uiState.value
        val groupMembers = state.members.filter { it.groupId == groupId }
        val declinedMember = groupMembers.find { it.memberId == declinedMemberId }
        val activeJoinedMembers = groupMembers.filter {
            it.memberId != declinedMemberId && it.inviteStatus == "JOINED"
        }
        if (activeJoinedMembers.isEmpty()) return

        val currentUser = activeJoinedMembers.find { it.isCurrentUser }
        val groupExpenses = state.expenses.filter { it.groupId == groupId }
        val affectedExpenses = groupExpenses.filter { exp ->
            state.splits.any { sp ->
                sp.expenseId == exp.expenseId &&
                    sp.memberId == declinedMemberId &&
                    sp.finalOwedCents > 0L
            }
        }
        if (affectedExpenses.isEmpty()) return

        val replacementSplitsByExpenseId = linkedMapOf<String, List<ExpenseSplitEntity>>()
        affectedExpenses.forEach { expense ->
            // PA-6: Strictly preserve subset splits — only redistribute across the remaining active participants of THIS expense
            val existingExpSplits = state.splits.filter { it.expenseId == expense.expenseId && it.finalOwedCents > 0L }
            val subsetParticipants = existingExpSplits
                .mapNotNull { sp -> activeJoinedMembers.find { it.memberId == sp.memberId } }
                .distinctBy { it.memberId }
                .ifEmpty { activeJoinedMembers }

            val equalAllocations = SplitMateMathEngine.splitEquallyZeroDrift(
                totalCents = expense.totalAmountCents,
                members = subsetParticipants.map { it.memberId to it.name },
                payerId = expense.payerId,
                currentUserId = currentUser?.memberId
            )
            val newSplits = equalAllocations.mapIndexed { idx, alloc ->
                ExpenseSplitEntity(
                    splitId = "${expense.expenseId}_sp_$idx",
                    expenseId = expense.expenseId,
                    memberId = alloc.memberId,
                    baseClaimedCents = alloc.baseClaimedCents,
                    finalOwedCents = alloc.finalCents,
                    plusOneCent = alloc.plusOneCent
                )
            }
            replacementSplitsByExpenseId[expense.expenseId] = newSplits
        }

        val affectedExpenseIds = replacementSplitsByExpenseId.keys
        val flattenedNewSplits = replacementSplitsByExpenseId.values.flatten()
        val declinedName = declinedMember?.name ?: "Declined member"

        _uiState.update { curr ->
            curr.copy(
                splits = curr.splits.filterNot { affectedExpenseIds.contains(it.expenseId) } + flattenedNewSplits,
                statusBannerMessage = "Reassigned $declinedName's share equally across affected expenses"
            )
        }

        viewModelScope.launch(ioDispatcher) {
            val d = dao ?: return@launch
            com.splitmate.app.data.ExpenseEditClock.record(appContext, replacementSplitsByExpenseId.keys) // v2.4.0 P1: version by edit time
            replacementSplitsByExpenseId.forEach { (expId, newSplits) ->
                d.replaceExpenseSplits(expId, newSplits)
            }
            d.markExpensesPending(replacementSplitsByExpenseId.keys.toList()) // v2.4.0 review N2
            val userPhone10 = resolveEffectiveUserPhone10(d)
            runCatching {
                com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                    context = context,
                    dao = d,
                    groupId = groupId,
                    localUserPhone10 = userPhone10,
                    localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                    isLocalMutation = true
                )
                refreshStateFromDaoSnapshot(d = d)
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
        syncActiveGroupNow(context = null, groupId = groupId, silent = true)
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
        var resolvedTarget: String? = null
        _uiState.update { state ->
            val targetGroup = explicitGroupId ?: state.returnToGroupDetailId ?: state.openedGroupDetailId ?: state.activeGroup?.groupId
            resolvedTarget = targetGroup
            state.copy(
                selectedTabName = "LEDGERS",
                openedGroupDetailId = targetGroup,
                returnToGroupDetailId = null
            )
        }
        if (!resolvedTarget.isNullOrBlank()) {
            syncActiveGroupNow(context = null, groupId = resolvedTarget, silent = true)
        }
    }

    fun selectActiveGroup(groupId: String) {
        _uiState.update { state ->
            state.copy(
                activeGroupId = groupId
            )
        }
    }

    fun createNewGroupWithContacts(
        name: String,
        iconName: String,
        memberDrafts: List<NewGroupMemberDraft>
    ) {
        val nowMs = System.currentTimeMillis()
        if ((nowMs - lastGroupCreateMs) < 800L) return
        lastGroupCreateMs = nowMs

        val cleanGroup = name.toSmartTitleCase().ifEmpty { "New Group" }
        val cleanIcon = iconName.trim().ifEmpty { "Flight" }
        val baseSeedId = "g_$nowMs"
        val joinCode6 = com.splitmate.app.data.CloudGroupSyncRepository.deriveGroupJoinCode6(baseSeedId)
        val groupId = "${baseSeedId}_$joinCode6"
        val newGroup = ExpenseGroupEntity(
            groupId = groupId,
            name = cleanGroup,
            currencyCode = "INR",
            iconName = cleanIcon
        )

        val normUserPhone = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(_uiState.value.userPhone)
        val meMember = GroupMemberEntity(
            memberId = "${groupId}_me",
            groupId = groupId,
            name = _uiState.value.currentUserName.ifBlank { "You" },
            avatarSeed = _uiState.value.currentUserSeed,
            upiId = _uiState.value.userUpiId,
            userPhone = normUserPhone,
            isCurrentUser = true,
            inviteStatus = "JOINED"
        )
        val friendMembers = memberDrafts.mapIndexedNotNull { index, draft ->
            val fName = draft.name.trim()
            val cleanPhone = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(draft.cleanPhone)
            val isSelf = normUserPhone.isNotBlank() && cleanPhone.isNotBlank() && cleanPhone == normUserPhone
            if (fName.isEmpty() || isSelf) null else {
                val autoUpiId = if (cleanPhone.length == 10) "${cleanPhone}@upi" else ""
                val style = draft.presentationStyle.ifBlank { "Neutral" }
                GroupMemberEntity(
                    memberId = "${groupId}_f$index",
                    groupId = groupId,
                    name = fName,
                    avatarSeed = "$fName|$style",
                    upiId = autoUpiId,
                    userPhone = cleanPhone,
                    isCurrentUser = false,
                    inviteStatus = if (cleanPhone.length == 10) "PENDING" else "JOINED"
                )
            }
        }
        val allNewMembers = listOf(meMember) + friendMembers

        com.splitmate.app.data.CloudGroupSyncRepository.recordLocalTombstones(
            context = null,
            groupId = groupId,
            organizerPhone10 = normUserPhone,
            joinCode6 = joinCode6
        )
        val invitedPhones = friendMembers.map { it.userPhone }.filter { it.length == 10 }
        if (invitedPhones.isNotEmpty()) {
            com.splitmate.app.data.CloudGroupSyncRepository.unTombstonePhones(
                context = null,
                groupId = groupId,
                phonesToRestore = invitedPhones
            )
        }

        _uiState.update { state ->
            state.copy(
                groups = listOf(newGroup) + state.groups,
                members = state.members + allNewMembers,
                activeGroupId = groupId,
                statusBannerMessage = "Created group \"$cleanGroup\" with ${allNewMembers.size} members"
            )
        }

        viewModelScope.launch(ioDispatcher) {
            val d = dao
            if (d != null) {
                d.insertGroup(newGroup)
                d.insertMembers(allNewMembers)
                com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                    context = null,
                    dao = d,
                    groupId = groupId,
                    localUserPhone10 = normUserPhone,
                    localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                    forceIndexPush = true
                )
            }
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
        createNewGroupWithContacts(name = name.toSmartTitleCase(), iconName = iconName, memberDrafts = drafts)
    }

    /**
     * Renames an existing group (and optionally updates its iconName) and reflects it reactively
     * across all screens, dropdowns, and Room persistence. Always normalizes to Title Case.
     */
    fun renameGroup(groupId: String, newName: String, newIconName: String? = null) {
        val cleanName = newName.toSmartTitleCase()
        if (cleanName.isEmpty()) return
        val existingGroup = _uiState.value.groups.find { it.groupId == groupId } ?: return
        val resolvedIcon = newIconName?.trim()?.ifEmpty { existingGroup.iconName } ?: existingGroup.iconName

        _uiState.update { state ->
            val updatedGroups = state.groups.map { grp ->
                if (grp.groupId == groupId) grp.copy(name = cleanName, iconName = resolvedIcon) else grp
            }
            state.copy(
                groups = updatedGroups,
                statusBannerMessage = "Renamed group to \"$cleanName\""
            )
        }

        viewModelScope.launch(ioDispatcher) {
            dao?.updateGroupDetails(
                groupId = groupId,
                newName = cleanName,
                newIconName = resolvedIcon
            )
        }
    }

    /**
     * Finds an already-logged expense matching [pnr] (6-char Flight PNR or 10-digit Train PNR),
     * prioritizing [preferredGroupId] (e.g., activeGroupId / openedGroupDetailId) and falling back
     * to any group in the ledger.
     */
    fun findExistingExpenseByPnr(
        pnr: String,
        preferredGroupId: String? = null
    ): Pair<ExpenseEntity, ExpenseGroupEntity?>? {
        val cleanPnr = pnr.trim().uppercase(Locale.US)
        if (cleanPnr.length < 6) return null
        val state = _uiState.value
        val targetGroupId = preferredGroupId ?: state.openedGroupDetailId ?: state.activeGroupId

        val allMatches = state.expenses.filter { exp ->
            exp.travelPnr.equals(cleanPnr, ignoreCase = true) ||
                exp.title.contains("[PNR:$cleanPnr]", ignoreCase = true) ||
                Regex("""\b${Regex.escape(cleanPnr)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(exp.title)
        }
        val bestMatch = allMatches.firstOrNull { it.groupId == targetGroupId }
            ?: allMatches.firstOrNull()
            ?: return null

        val matchedGroup = state.groups.find { it.groupId == bestMatch.groupId }
        return bestMatch to matchedGroup
    }

    fun addMemberToActiveGroup(friendName: String, rawPhone: String = "") {
        val clean = friendName.trim()
        if (clean.isEmpty()) return
        val state = _uiState.value
        val groupId = state.activeGroupId
        val normUserPhone = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(state.userPhone)
        val cleanPhone = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(rawPhone)
        if (normUserPhone.isNotBlank() && cleanPhone.isNotBlank() && cleanPhone == normUserPhone) return

        if (cleanPhone.length == 10) {
            com.splitmate.app.data.CloudGroupSyncRepository.unTombstonePhones(
                context = null,
                groupId = groupId,
                phonesToRestore = listOf(cleanPhone)
            )
        }

        val newMember = GroupMemberEntity(
            memberId = "m_${System.currentTimeMillis()}",
            groupId = groupId,
            name = clean,
            avatarSeed = "$clean|Neutral",
            upiId = if (cleanPhone.length == 10) "${cleanPhone}@upi" else "",
            userPhone = cleanPhone,
            isCurrentUser = false,
            inviteStatus = if (cleanPhone.length == 10) "PENDING" else "JOINED"
        )
        _uiState.update { curr ->
            curr.copy(
                members = curr.members + newMember,
                statusBannerMessage = "Added $clean to group"
            )
        }
        viewModelScope.launch(ioDispatcher) {
            val d = dao
            if (d != null) {
                d.insertMembers(listOf(newMember))
                com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                    context = null,
                    dao = d,
                    groupId = groupId,
                    localUserPhone10 = normUserPhone,
                    localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                    forceIndexPush = true
                )
            }
        }
    }

    fun canSafelyRemoveMember(memberId: String): Boolean {
        val state = _uiState.value
        val hasPayerHistory = state.expenses.any { it.payerId == memberId }
        val hasSplitHistory = state.splits.any { it.memberId == memberId }
        val hasSettlementHistory = state.settlements.any { it.fromMemberId == memberId || it.toMemberId == memberId }
        return !hasPayerHistory && !hasSplitHistory && !hasSettlementHistory
    }

    fun addContactsToGroup(groupId: String, contacts: List<DeviceContact>) {
        if (contacts.isEmpty()) return
        val stateBefore = _uiState.value
        val normUserPhone = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(stateBefore.userPhone)
        val existingMembers = stateBefore.members.filter { it.groupId == groupId }
        val existingByPhone = existingMembers.mapNotNull { m ->
            val p = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId)
            if (p.length == 10) p to m else null
        }.toMap()
        val existingPhonelessByName = existingMembers.filter { m ->
            com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId).isEmpty()
        }.associateBy { it.name.trim().lowercase() }.toMutableMap()
        val existingNames = existingMembers.map { it.name.trim().lowercase() }.toSet()
        val now = System.currentTimeMillis()
        val seenBatchPhones = HashSet<String>()
        val seenBatchPhonelessNames = HashSet<String>()

        val upgradedExistingMembers = mutableListOf<GroupMemberEntity>()
        val newMembers = contacts.mapIndexedNotNull { idx, c ->
            val cleanName = c.name.trim()
            val cleanPhone = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(c.cleanPhone)
            val isSelfPhone = normUserPhone.isNotBlank() && cleanPhone.isNotBlank() && cleanPhone == normUserPhone
            if (cleanName.isEmpty() || isSelfPhone) return@mapIndexedNotNull null

            if (cleanPhone.length == 10) {
                if (!seenBatchPhones.add(cleanPhone)) return@mapIndexedNotNull null
                val alreadyByPhone = existingByPhone[cleanPhone]
                if (alreadyByPhone != null) {
                    val refreshed = if (alreadyByPhone.inviteStatus.equals("JOINED", ignoreCase = true)) {
                        alreadyByPhone
                    } else {
                        alreadyByPhone.copy(inviteStatus = "PENDING", userPhone = cleanPhone, upiId = "${cleanPhone}@upi")
                    }
                    upgradedExistingMembers.add(refreshed)
                    return@mapIndexedNotNull null
                }
                val phonelessMatch = existingPhonelessByName.remove(cleanName.lowercase())
                if (phonelessMatch != null && !phonelessMatch.isCurrentUser) {
                    upgradedExistingMembers.add(
                        phonelessMatch.copy(
                            name = cleanName,
                            userPhone = cleanPhone,
                            upiId = "${cleanPhone}@upi",
                            inviteStatus = "PENDING"
                        )
                    )
                    return@mapIndexedNotNull null
                }
            } else {
                val isDuplicatePhoneLessName = existingNames.contains(cleanName.lowercase()) || !seenBatchPhonelessNames.add(cleanName.lowercase())
                if (isDuplicatePhoneLessName) return@mapIndexedNotNull null
            }

            GroupMemberEntity(
                memberId = "${groupId}_c_${now}_$idx",
                groupId = groupId,
                name = cleanName,
                avatarSeed = "$cleanName|Neutral",
                upiId = if (cleanPhone.length == 10) "${cleanPhone}@upi" else "",
                userPhone = cleanPhone,
                isCurrentUser = false,
                inviteStatus = if (cleanPhone.length == 10) "PENDING" else "JOINED"
            )
        }
        val allTouchedMembers = upgradedExistingMembers + newMembers
        if (allTouchedMembers.isEmpty()) return
        val reInvitedPhones = allTouchedMembers.map { it.userPhone }.filter { it.length == 10 }
        if (reInvitedPhones.isNotEmpty()) {
            com.splitmate.app.data.CloudGroupSyncRepository.unTombstonePhones(
                context = null,
                groupId = groupId,
                phonesToRestore = reInvitedPhones,
                memberIdsToRestore = allTouchedMembers.map { it.memberId }
            )
        }
        val upgradedById = upgradedExistingMembers.associateBy { it.memberId }
        _uiState.update { state ->
            state.copy(
                members = state.members.map { upgradedById[it.memberId] ?: it } + newMembers,
                statusBannerMessage = if (newMembers.isNotEmpty()) {
                    "Added ${newMembers.size} contact(s) & sent cloud invite(s)"
                } else {
                    "Updated & re-invited ${upgradedExistingMembers.size} contact(s)"
                }
            )
        }
        viewModelScope.launch(ioDispatcher) {
            val d = dao
            if (d != null) {
                d.insertMembers(allTouchedMembers)
                com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                    context = null,
                    dao = d,
                    groupId = groupId,
                    localUserPhone10 = normUserPhone,
                    localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                    forceIndexPush = true
                )
            }
        }
    }

    // =========================================================================
    // v2.2 GROUP ORGANIZER GOVERNANCE, LEAVE TRIP & 6-CHAR JOIN CODE ENGINE (F6, F8)
    // =========================================================================

    fun getGroupOrganizerMember(
        groupId: String = _uiState.value.activeGroupId,
        state: SplitMateUiState = _uiState.value
    ): GroupMemberEntity? {
        val gMembers = state.members.filter { it.groupId == groupId }
        if (gMembers.isEmpty()) return null
        val storedOrgPhone = com.splitmate.app.data.CloudGroupSyncRepository.getOrganizerPhone10(null, groupId)
        if (storedOrgPhone.length == 10) {
            val byPhone = gMembers.firstOrNull {
                com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == storedOrgPhone
            }
            if (byPhone != null) return byPhone
        }
        return gMembers.firstOrNull { it.memberId.endsWith("_me") }
            ?: gMembers.firstOrNull { it.memberId == "m_1" || it.memberId == "m_1_apt" }
            ?: gMembers.firstOrNull()
    }

    /**
     * Returns all active Trip Organizers for [groupId] (primary creator + any co-organizers promoted
     * via WhatsApp-style "Make Organizer"). Guarantees at least 1 organizer is always returned.
     */
    fun getGroupOrganizerMembers(
        groupId: String = _uiState.value.activeGroupId,
        state: SplitMateUiState = _uiState.value
    ): List<GroupMemberEntity> {
        val gMembers = state.members.filter { it.groupId == groupId }
        if (gMembers.isEmpty()) return emptyList()
        val primaryOrg = getGroupOrganizerMember(groupId, state)
        val rolesMap = com.splitmate.app.data.CloudGroupSyncRepository.getOrganizerRolesByKey(null, groupId)
        val explicitOrganizers = gMembers.filter { m ->
            val phone10 = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId)
            val phoneRole = if (phone10.length == 10) rolesMap[phone10] else null
            val idRole = rolesMap[m.memberId]
            val effectiveRole = when {
                phoneRole != null && idRole != null ->
                    if (kotlin.math.abs(phoneRole) >= kotlin.math.abs(idRole)) phoneRole else idRole
                phoneRole != null -> phoneRole
                idRole != null -> idRole
                else -> null
            }
            when {
                effectiveRole != null -> effectiveRole > 0L
                else -> m.memberId == primaryOrg?.memberId
            }
        }
        return explicitOrganizers.ifEmpty { listOfNotNull(primaryOrg ?: gMembers.firstOrNull()) }
    }

    fun isMemberGroupOrganizer(
        groupId: String = _uiState.value.activeGroupId,
        member: GroupMemberEntity,
        state: SplitMateUiState = _uiState.value
    ): Boolean {
        return getGroupOrganizerMembers(groupId, state).any { it.memberId == member.memberId }
    }

    fun isUserGroupOrganizer(
        groupId: String,
        state: SplitMateUiState
    ): Boolean = isUserGroupOrganizer(groupId = groupId, userPhone = state.userPhone, state = state)

    fun isUserGroupOrganizer(
        groupId: String = _uiState.value.activeGroupId,
        userPhone: String = _uiState.value.userPhone,
        state: SplitMateUiState = _uiState.value
    ): Boolean {
        val organizers = getGroupOrganizerMembers(groupId, state)
        if (organizers.isEmpty()) return false
        val gMembers = state.members.filter { it.groupId == groupId }
        val activePerspectiveMember = gMembers.find { it.isCurrentUser }
        val isDefaultUserPhone = userPhone == state.userPhone
        if (isDefaultUserPhone && activePerspectiveMember != null) {
            return organizers.any { it.memberId == activePerspectiveMember.memberId }
        }
        val myPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(userPhone)
        return organizers.any { organizer ->
            val orgPhone10 = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(organizer.userPhone, organizer.upiId)
            if (myPhone10.length == 10 && orgPhone10.length == 10) {
                myPhone10 == orgPhone10
            } else {
                organizer.isCurrentUser
            }
        }
    }

    fun isCurrentUserOrganizer(groupId: String = _uiState.value.activeGroupId): Boolean =
        isUserGroupOrganizer(groupId)

    /**
     * WhatsApp-style "Make Organizer": Allows any existing Trip Organizer to promote another member
     * in [groupId] to Trip Organizer and syncs the promotion across all devices.
     */
    fun promoteMemberToOrganizer(
        context: android.content.Context? = null,
        groupId: String = _uiState.value.activeGroupId,
        targetMemberId: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ): Boolean {
        com.splitmate.app.data.CloudGroupSyncRepository.init(context)
        val state = _uiState.value
        val gMembers = state.members.filter { it.groupId == groupId }
        val targetMember = gMembers.find { it.memberId == targetMemberId }
        if (targetMember == null) {
            val msg = "Member not found in group"
            onResult(false, msg)
            return false
        }
        if (!isUserGroupOrganizer(groupId)) {
            val msg = "Only a Trip Organizer can make another member an organizer"
            _uiState.update { it.copy(statusBannerMessage = msg) }
            onResult(false, msg)
            return false
        }
        val targetPhone10 = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(targetMember.userPhone, targetMember.upiId)
        com.splitmate.app.data.CloudGroupSyncRepository.setMemberOrganizerRole(
            context = context,
            groupId = groupId,
            memberId = targetMember.memberId,
            memberPhone10 = targetPhone10,
            isOrganizer = true
        )
        val statusMsg = "${targetMember.name} is now a Trip Organizer"
        _uiState.update { curr ->
            curr.copy(
                members = curr.members.toList(),
                statusBannerMessage = statusMsg
            )
        }
        viewModelScope.launch(ioDispatcher) {
            val d = dao
            if (d != null) {
                val normUserPhone = resolveEffectiveUserPhone10(d)
                runCatching {
                    com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                        context = context,
                        dao = d,
                        groupId = groupId,
                        localUserPhone10 = normUserPhone,
                        localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                        forceIndexPush = true,
                        isLocalMutation = true
                    )
                    refreshStateFromDaoSnapshot(d = d, statusMsg = statusMsg)
                }
            }
        }
        onResult(true, statusMsg)
        return true
    }

    /**
     * WhatsApp-style "Dismiss as Organizer": Allows an existing Trip Organizer to dismiss another
     * organizer as long as at least 1 organizer remains in the group.
     */
    fun dismissMemberAsOrganizer(
        context: android.content.Context? = null,
        groupId: String = _uiState.value.activeGroupId,
        targetMemberId: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ): Boolean {
        com.splitmate.app.data.CloudGroupSyncRepository.init(context)
        val state = _uiState.value
        val gMembers = state.members.filter { it.groupId == groupId }
        val targetMember = gMembers.find { it.memberId == targetMemberId }
        if (targetMember == null) {
            val msg = "Member not found in group"
            onResult(false, msg)
            return false
        }
        if (!isUserGroupOrganizer(groupId)) {
            val msg = "Only a Trip Organizer can dismiss an organizer"
            _uiState.update { it.copy(statusBannerMessage = msg) }
            onResult(false, msg)
            return false
        }
        val currentOrganizers = getGroupOrganizerMembers(groupId, state)
        if (currentOrganizers.size <= 1 && currentOrganizers.any { it.memberId == targetMemberId }) {
            val msg = "Trip must have at least one Organizer"
            _uiState.update { it.copy(statusBannerMessage = msg) }
            onResult(false, msg)
            return false
        }
        val targetPhone10 = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(targetMember.userPhone, targetMember.upiId)
        com.splitmate.app.data.CloudGroupSyncRepository.setMemberOrganizerRole(
            context = context,
            groupId = groupId,
            memberId = targetMember.memberId,
            memberPhone10 = targetPhone10,
            isOrganizer = false
        )
        val statusMsg = "Dismissed ${targetMember.name} as Trip Organizer"
        _uiState.update { curr ->
            curr.copy(
                members = curr.members.toList(),
                statusBannerMessage = statusMsg
            )
        }
        viewModelScope.launch(ioDispatcher) {
            val d = dao
            if (d != null) {
                val normUserPhone = resolveEffectiveUserPhone10(d)
                runCatching {
                    com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                        context = context,
                        dao = d,
                        groupId = groupId,
                        localUserPhone10 = normUserPhone,
                        localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                        isLocalMutation = true
                    )
                    refreshStateFromDaoSnapshot(d = d, statusMsg = statusMsg)
                }
            }
        }
        onResult(true, statusMsg)
        return true
    }

    /**
     * Determines whether the current local user is allowed to click `Mark paid` on a settlement
     * transfer where [toMemberId] is the recipient (the member receiving the money):
     * 1. Allowed if the current local user IS the recipient (`toMemberId`) receiving the money.
     * 2. Allowed if the current local user is a Trip Organizer / co-organizer (v2.3.6: for any
     *    recipient, joined or not).
     */
    fun canCurrentUserMarkTransferPaid(
        groupId: String = _uiState.value.activeGroupId,
        toMemberId: String,
        state: SplitMateUiState = _uiState.value
    ): Boolean {
        val gMembers = state.members.filter { it.groupId == groupId }
        if (gMembers.isEmpty()) return true
        val toMember = gMembers.find { it.memberId == toMemberId }
        val activePerspectiveMember = gMembers.find { it.isCurrentUser }
        val myPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(state.userPhone)
        val toPhone10 = toMember?.let {
            com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId)
        }.orEmpty()

        // 1. Current user is the recipient (the member who is going to get the money)
        val isRecipientCurrentUser = if (activePerspectiveMember != null) {
            activePerspectiveMember.memberId == toMemberId
        } else {
            myPhone10.length == 10 && toPhone10.length == 10 && myPhone10 == toPhone10
        }
        if (isRecipientCurrentUser) return true

        // 2. v2.3.6 (product decision): a Trip Organizer (or co-organizer) can mark ANY transfer paid,
        //    including ones where the recipient has joined the app, e.g. when cash was handed over
        //    in person. Everyone else still needs to be the recipient.
        return isUserGroupOrganizer(groupId, state)
    }

    fun getMarkPaidRestrictionLabel(
        groupId: String = _uiState.value.activeGroupId,
        toMemberId: String,
        toMemberName: String,
        state: SplitMateUiState = _uiState.value
    ): String {
        val gMembers = state.members.filter { it.groupId == groupId }
        val toMember = gMembers.find { it.memberId == toMemberId }
        val cleanToName = toMember?.name?.ifBlank { toMemberName } ?: toMemberName
        return "Only $cleanToName or an organizer can Mark paid"
    }

    fun getGroupJoinCode(groupId: String = _uiState.value.activeGroupId): String {
        if (groupId.isBlank()) return ""
        val stored = com.splitmate.app.data.CloudGroupSyncRepository.loadLocalTombstones(null, groupId).joinCode6
        return stored.ifBlank { com.splitmate.app.data.CloudGroupSyncRepository.deriveGroupJoinCode6(groupId) }
    }

    fun getFormattedGroupJoinCode(groupId: String = _uiState.value.activeGroupId): String {
        val raw = getGroupJoinCode(groupId)
        return com.splitmate.app.data.CloudGroupSyncRepository.formatJoinCode6(raw)
    }

    fun launchDirectMemberWhatsAppOrShareInvite(
        context: android.content.Context?,
        groupId: String,
        groupName: String,
        memberName: String,
        cleanPhone10: String
    ) {
        if (context == null) return
        val formattedCode = getFormattedGroupJoinCode(groupId)
        val rawCode6 = getGroupJoinCode(groupId)
        val encodedName = android.net.Uri.encode(groupName)
        val deepLink = "https://splitmate.app/join?groupId=${android.net.Uri.encode(groupId)}&name=$encodedName&code=$rawCode6"
        val shareBody = buildString {
            append("Hey ${memberName.trim().ifBlank { "there" }}! Join our trip \"$groupName\" on SplitMate.\n\n")
            append("Trip Join Code: $formattedCode\n")
            append("Tap to join or enter the code in SplitMate:\n")
            append(deepLink)
        }
        runCatching {
            val waUri = android.net.Uri.parse(
                "https://api.whatsapp.com/send?phone=91$cleanPhone10&text=${android.net.Uri.encode(shareBody)}"
            )
            val waIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, waUri).apply {
                setPackage("com.whatsapp")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(waIntent)
        }.onFailure {
            runCatching {
                val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Join $groupName on SplitMate")
                    putExtra(android.content.Intent.EXTRA_TEXT, shareBody)
                }
                context.startActivity(
                    android.content.Intent.createChooser(fallbackIntent, "Send Invite to $memberName").apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }
    }

    fun sendOrResendDirectMemberInvite(
        context: android.content.Context? = null,
        groupId: String = _uiState.value.activeGroupId,
        memberId: String,
        launchWhatsAppShare: Boolean = true,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ): Boolean {
        com.splitmate.app.data.CloudGroupSyncRepository.init(context)
        val state = _uiState.value
        val existingMember = state.members.find { it.groupId == groupId && it.memberId == memberId }
        if (existingMember == null) {
            onComplete(false, "Member not found")
            return false
        }
        val cleanPhone10 = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(
            existingMember.userPhone,
            existingMember.upiId
        )
        if (!com.splitmate.app.data.PhoneIdentityValidator.isValidIndianMobile10(cleanPhone10)) {
            val errMsg = "Link a valid 10-digit mobile number for ${existingMember.name} first"
            _uiState.update { it.copy(statusBannerMessage = errMsg) }
            onComplete(false, errMsg)
            return false
        }
        val groupName = state.groups.find { it.groupId == groupId }?.name
            ?: state.activeGroup?.name
            ?: "SplitMate Trip"
        val formattedCode = getFormattedGroupJoinCode(groupId)
        val updatedStatus = if (existingMember.isCurrentUser || existingMember.inviteStatus.equals("JOINED", ignoreCase = true)) {
            "JOINED"
        } else {
            "PENDING"
        }
        val updatedMember = existingMember.copy(
            userPhone = cleanPhone10,
            upiId = if (existingMember.upiId.isBlank()) "${cleanPhone10}@upi" else existingMember.upiId,
            inviteStatus = updatedStatus
        )
        com.splitmate.app.data.CloudGroupSyncRepository.unTombstonePhones(
            context = context,
            groupId = groupId,
            phonesToRestore = listOf(cleanPhone10),
            memberIdsToRestore = listOf(memberId)
        )
        val okMsg = "Invite sent to ${existingMember.name} (+91 $cleanPhone10) · Trip Code $formattedCode"
        _uiState.update { curr ->
            curr.copy(
                members = curr.members.map { if (it.memberId == memberId) updatedMember else it },
                statusBannerMessage = okMsg
            )
        }
        val myPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(state.userPhone)
        viewModelScope.launch(ioDispatcher) {
            val d = dao
            if (d != null) {
                d.insertMembers(listOf(updatedMember))
                com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                    context = context,
                    dao = d,
                    groupId = groupId,
                    localUserPhone10 = myPhone10,
                    localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                    forceIndexPush = true
                )
            }
        }
        if (launchWhatsAppShare && context != null) {
            launchDirectMemberWhatsAppOrShareInvite(
                context = context,
                groupId = groupId,
                groupName = groupName,
                memberName = existingMember.name,
                cleanPhone10 = cleanPhone10
            )
        }
        onComplete(true, okMsg)
        return true
    }

    fun updateMemberPhoneAndResendInvite(
        groupId: String,
        memberId: String,
        rawPhone: String,
        newName: String = ""
    ): Boolean = updateMemberPhoneAndResendInvite(
        context = null,
        groupId = groupId,
        memberId = memberId,
        rawPhone = rawPhone,
        newName = newName
    )

    fun updateMemberPhoneAndResendInvite(
        context: android.content.Context? = null,
        groupId: String = _uiState.value.activeGroupId,
        memberId: String,
        rawPhone: String,
        newName: String = "",
        launchWhatsAppShare: Boolean = false,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ): Boolean {
        com.splitmate.app.data.CloudGroupSyncRepository.init(context)
        val normA = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(rawPhone)
        val normB = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(newName)
        val (effectivePhoneInput, effectiveNameInput) = if (
            !com.splitmate.app.data.PhoneIdentityValidator.isValidIndianMobile10(normA) &&
            com.splitmate.app.data.PhoneIdentityValidator.isValidIndianMobile10(normB)
        ) {
            newName to rawPhone
        } else {
            rawPhone to newName
        }

        val cleanPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(effectivePhoneInput)
        if (!com.splitmate.app.data.PhoneIdentityValidator.isValidIndianMobile10(cleanPhone10)) {
            val errMsg = "Enter a valid 10-digit Indian mobile number"
            _uiState.update { it.copy(statusBannerMessage = errMsg) }
            onComplete(false, errMsg)
            return false
        }
        val state = _uiState.value
        val myPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(state.userPhone)
        if (myPhone10.length == 10 && cleanPhone10 == myPhone10) {
            val errMsg = "Cannot assign your own mobile number to a friend"
            _uiState.update { it.copy(statusBannerMessage = errMsg) }
            onComplete(false, errMsg)
            return false
        }
        val existingMember = state.members.find { it.groupId == groupId && it.memberId == memberId }
        if (existingMember == null) {
            onComplete(false, "Member not found")
            return false
        }
        val resolvedName = effectiveNameInput.trim().ifEmpty { existingMember.name }
        val updatedMember = existingMember.copy(
            name = resolvedName,
            userPhone = cleanPhone10,
            upiId = "${cleanPhone10}@upi",
            inviteStatus = if (existingMember.isCurrentUser) "JOINED" else "PENDING"
        )

        com.splitmate.app.data.CloudGroupSyncRepository.unTombstonePhones(
            context = context,
            groupId = groupId,
            phonesToRestore = listOf(cleanPhone10),
            memberIdsToRestore = listOf(memberId)
        )

        val formattedCode = getFormattedGroupJoinCode(groupId)
        val okMsg = "Linked +91 $cleanPhone10 to $resolvedName & sent invite (Code: $formattedCode)"
        _uiState.update { curr ->
            curr.copy(
                members = curr.members.map { if (it.memberId == memberId) updatedMember else it },
                statusBannerMessage = okMsg
            )
        }

        viewModelScope.launch(ioDispatcher) {
            val d = dao ?: return@launch
            d.insertMembers(listOf(updatedMember))
            com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                context = context,
                dao = d,
                groupId = groupId,
                localUserPhone10 = myPhone10,
                localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                forceIndexPush = true
            )
        }
        if (launchWhatsAppShare && context != null) {
            val groupName = state.groups.find { it.groupId == groupId }?.name
                ?: state.activeGroup?.name
                ?: "SplitMate Trip"
            launchDirectMemberWhatsAppOrShareInvite(
                context = context,
                groupId = groupId,
                groupName = groupName,
                memberName = resolvedName,
                cleanPhone10 = cleanPhone10
            )
        }
        onComplete(true, okMsg)
        return true
    }

    fun removeMemberFromGroup(
        groupId: String,
        targetMemberId: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ): Boolean = removeMemberFromGroup(
        context = null,
        groupId = groupId,
        targetMemberId = targetMemberId,
        onResult = onResult
    )

    fun removeMemberFromGroup(
        context: android.content.Context? = null,
        groupId: String = _uiState.value.activeGroupId,
        targetMemberId: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ): Boolean {
        com.splitmate.app.data.CloudGroupSyncRepository.init(context)
        val state = _uiState.value
        val gMembers = state.members.filter { it.groupId == groupId }
        val targetMember = gMembers.find { it.memberId == targetMemberId }
        if (targetMember == null) {
            val msg = "Member not found in group"
            onResult(false, msg)
            return false
        }
        val organizer = getGroupOrganizerMember(groupId)
        val myPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(state.userPhone)
        val targetPhone10 = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(targetMember.userPhone, targetMember.upiId)
        if (!isUserGroupOrganizer(groupId) || organizer == null) {
            val msg = "Only the Trip Organizer can remove members from this group"
            _uiState.update { it.copy(statusBannerMessage = msg) }
            onResult(false, msg)
            return false
        }
        val isRemovingSelf = targetMember.isCurrentUser ||
            (myPhone10.length == 10 && targetPhone10.length == 10 && myPhone10 == targetPhone10)
        if (isRemovingSelf) {
            val msg = "Trip Organizer cannot remove themselves; use Leave Group instead"
            _uiState.update { it.copy(statusBannerMessage = msg) }
            onResult(false, msg)
            return false
        }

        val survivingMembers = gMembers.filter { it.memberId != targetMemberId }
        if (survivingMembers.isEmpty()) {
            val msg = "Group must retain at least 1 member"
            _uiState.update { it.copy(statusBannerMessage = msg) }
            onResult(false, msg)
            return false
        }

        val nowMs = System.currentTimeMillis()
        com.splitmate.app.data.CloudGroupSyncRepository.setMemberOrganizerRole(
            context = context,
            groupId = groupId,
            memberId = targetMemberId,
            memberPhone10 = targetPhone10,
            isOrganizer = false,
            timestampMs = nowMs
        )
        val orgPhone10 = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(organizer.userPhone, organizer.upiId)
            .ifBlank { myPhone10 }

        val deletedMemberIds = mapOf(targetMemberId to nowMs)
        val removedPhones = if (targetPhone10.length == 10) mapOf(targetPhone10 to nowMs) else emptyMap()

        val activeSurvivingMembers = survivingMembers.filter { !it.inviteStatus.equals("DECLINED", ignoreCase = true) }
            .ifEmpty { survivingMembers }
        val currentUserMember = survivingMembers.find { it.isCurrentUser } ?: organizer

        // PA-1: Reassign any expenses paid by the removed member to the Organizer
        val groupExpenses = state.expenses.filter { it.groupId == groupId }
        val updatedGroupExpenses = groupExpenses.map { exp ->
            val reassignedPayer = if (exp.payerId == targetMemberId) organizer.memberId else exp.payerId
            if (reassignedPayer != exp.payerId || exp.unassignedBaseCents != 0L) {
                exp.copy(payerId = reassignedPayer, unassignedBaseCents = 0L)
            } else {
                exp
            }
        }
        val updatedExpenseById = updatedGroupExpenses.associateBy { it.expenseId }

        // PA-6: Redistribute splits of affected expenses strictly across the remaining active participants of each expense (0.00c drift)
        val groupExpenseIds = groupExpenses.map { it.expenseId }.toSet()
        val replacementSplitsByExpenseId = linkedMapOf<String, List<ExpenseSplitEntity>>()
        for (exp in updatedGroupExpenses) {
            val expSplits = state.splits.filter { it.expenseId == exp.expenseId }
            val hadRemovedMember = expSplits.any { it.memberId == targetMemberId }
            if (hadRemovedMember) {
                val survivingExpParticipants = expSplits
                    .filter { it.memberId != targetMemberId && it.finalOwedCents > 0L }
                    .mapNotNull { sp -> activeSurvivingMembers.find { it.memberId == sp.memberId } }
                    .distinctBy { it.memberId }
                    .ifEmpty { activeSurvivingMembers }

                val equalAllocations = SplitMateMathEngine.splitEquallyZeroDrift(
                    totalCents = exp.totalAmountCents,
                    members = survivingExpParticipants.map { it.memberId to it.name },
                    payerId = exp.payerId,
                    currentUserId = currentUserMember.memberId
                )
                val existingByMember = expSplits.associateBy { it.memberId }
                val newSplits = equalAllocations.mapIndexed { idx, alloc ->
                    ExpenseSplitEntity(
                        splitId = existingByMember[alloc.memberId]?.splitId ?: "${exp.expenseId}_sp_$idx",
                        expenseId = exp.expenseId,
                        memberId = alloc.memberId,
                        baseClaimedCents = alloc.baseClaimedCents,
                        finalOwedCents = alloc.finalCents,
                        plusOneCent = alloc.plusOneCent
                    )
                }
                replacementSplitsByExpenseId[exp.expenseId] = newSplits
            }
        }

        // Tombstone any settlements involving the removed member
        val removedSettlements = state.settlements.filter {
            it.groupId == groupId && (it.fromMemberId == targetMemberId || it.toMemberId == targetMemberId)
        }
        val deletedSettlementIds = removedSettlements.associate { it.settlementId to nowMs }

        com.splitmate.app.data.CloudGroupSyncRepository.recordLocalTombstones(
            context = context,
            groupId = groupId,
            deletedMemberIds = deletedMemberIds,
            removedMemberPhones = removedPhones,
            deletedSettlementIds = deletedSettlementIds,
            organizerPhone10 = orgPhone10
        )

        val statusMsg = "Removed ${targetMember.name} from group and rebalanced shared expenses"
        _uiState.update { curr ->
            val nextMembers = curr.members.filterNot { it.groupId == groupId && it.memberId == targetMemberId }
            val nextExpenses = curr.expenses.map { updatedExpenseById[it.expenseId] ?: it }
            val affectedExpIds = replacementSplitsByExpenseId.keys
            val untouchedSplits = curr.splits.filterNot {
                (it.expenseId in groupExpenseIds && it.memberId == targetMemberId) ||
                    it.expenseId in affectedExpIds
            }
            val nextSplits = untouchedSplits + replacementSplitsByExpenseId.values.flatten()
            val nextSettlements = curr.settlements.filterNot { it.settlementId in deletedSettlementIds }
            curr.copy(
                members = nextMembers,
                expenses = nextExpenses,
                splits = nextSplits,
                settlements = nextSettlements,
                statusBannerMessage = statusMsg
            )
        }

        viewModelScope.launch(ioDispatcher) {
            val d = dao
            if (d != null) {
                // v2.4.0 P1: version by edit time (payer moves + recomputed shares are real edits).
                com.splitmate.app.data.ExpenseEditClock.record(appContext, updatedGroupExpenses.map { it.expenseId } + replacementSplitsByExpenseId.keys)
                d.reassignExpensePayer(groupId, targetMemberId, organizer.memberId)
                updatedGroupExpenses.forEach { d.insertExpense(it) }
                replacementSplitsByExpenseId.forEach { (expId, newSplits) ->
                    d.replaceExpenseSplits(expId, newSplits)
                }
                d.markExpensesPending((updatedGroupExpenses.map { it.expenseId } + replacementSplitsByExpenseId.keys).distinct()) // v2.4.0 review N2
                d.deleteSettlementsForMember(groupId, targetMemberId)
                d.deleteMemberById(targetMemberId)
                com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                    context = context,
                    dao = d,
                    groupId = groupId,
                    localUserPhone10 = orgPhone10,
                    localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                    additionalDeletedMemberIds = deletedMemberIds,
                    additionalRemovedMemberPhones = removedPhones,
                    additionalDeletedSettlementIds = deletedSettlementIds,
                    forceIndexPush = true
                )
            }
        }

        onResult(true, statusMsg)
        return true
    }

    fun leaveGroup(
        groupId: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ): Boolean = leaveGroup(
        context = null,
        groupId = groupId,
        onResult = onResult
    )

    fun leaveGroup(
        context: android.content.Context? = null,
        groupId: String = _uiState.value.activeGroupId,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ): Boolean {
        com.splitmate.app.data.CloudGroupSyncRepository.init(context)
        val state = _uiState.value
        val group = state.groups.find { it.groupId == groupId }
        val groupName = group?.name ?: "Group"
        val gMembers = state.members.filter { it.groupId == groupId }
        val myPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(state.userPhone)
        val meMember = gMembers.find {
            myPhone10.length == 10 &&
                com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == myPhone10
        } ?: gMembers.find { it.isCurrentUser }

        if (meMember == null) {
            val msg = "You are not a member of \"$groupName\""
            onResult(false, msg)
            return false
        }

        val balances = computeGroupMemberNetBalances(groupId)
        val myNetCents = balances[meMember.memberId] ?: 0L
        if (myNetCents != 0L) {
            val formattedNet = formatIndianRupeesFromCents(kotlin.math.abs(myNetCents), includePlusSign = false, currencySymbol = "₹")
            val msg = "Settle your $formattedNet balance before leaving \"$groupName\""
            _uiState.update { it.copy(statusBannerMessage = msg) }
            onResult(false, msg)
            return false
        }

        val nowMs = System.currentTimeMillis()
        val effectivePhone10 = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(meMember.userPhone, meMember.upiId)
            .ifBlank { myPhone10 }
        val deletedMemberIds = mapOf(meMember.memberId to nowMs)
        val removedPhones = if (effectivePhone10.length == 10) mapOf(effectivePhone10 to nowMs) else emptyMap()
        val groupSettlements = state.settlements.filter {
            it.groupId == groupId && (it.fromMemberId == meMember.memberId || it.toMemberId == meMember.memberId)
        }
        val deletedSettlementIds = groupSettlements.associate { it.settlementId to nowMs }

        com.splitmate.app.data.CloudGroupSyncRepository.recordLocalTombstones(
            context = context,
            groupId = groupId,
            deletedMemberIds = deletedMemberIds,
            removedMemberPhones = removedPhones,
            deletedSettlementIds = deletedSettlementIds
        )

        val statusMsg = "You left \"$groupName\""
        _uiState.update { curr ->
            val remainingGroups = curr.groups.filterNot { it.groupId == groupId }
            val remainingExpenseIds = curr.expenses.filter { it.groupId == groupId }.map { it.expenseId }.toSet()
            val nextActiveId = remainingGroups.firstOrNull()?.groupId.orEmpty()
            curr.copy(
                groups = remainingGroups,
                members = curr.members.filterNot { it.groupId == groupId },
                expenses = curr.expenses.filterNot { it.groupId == groupId },
                splits = curr.splits.filterNot { it.expenseId in remainingExpenseIds },
                settlements = curr.settlements.filterNot { it.groupId == groupId },
                activeGroupId = if (curr.activeGroupId == groupId) nextActiveId else curr.activeGroupId,
                openedGroupDetailId = if (curr.openedGroupDetailId == groupId) null else curr.openedGroupDetailId,
                returnToGroupDetailId = if (curr.returnToGroupDetailId == groupId) null else curr.returnToGroupDetailId,
                statusBannerMessage = statusMsg
            )
        }

        viewModelScope.launch(ioDispatcher) {
            val d = dao
            if (d != null) {
                com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                    context = context,
                    dao = d,
                    groupId = groupId,
                    localUserPhone10 = effectivePhone10,
                    localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                    additionalDeletedMemberIds = deletedMemberIds,
                    additionalRemovedMemberPhones = removedPhones,
                    additionalDeletedSettlementIds = deletedSettlementIds,
                    forceIndexPush = true
                )
                d.deleteGroupCascade(groupId)
            }
        }

        onResult(true, statusMsg)
        return true
    }

    fun joinGroupByCodeOrLink(
        rawInput: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) = joinGroupByCodeOrLink(
        context = null,
        rawInput = rawInput,
        onResult = onResult
    )

    fun joinGroupByCodeOrLink(
        context: android.content.Context? = null,
        rawInput: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        com.splitmate.app.data.CloudGroupSyncRepository.init(context)
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            val msg = "Enter a 6-character Trip Join Code or paste a Sync Capsule"
            _uiState.update { it.copy(statusBannerMessage = msg) }
            onResult(false, msg)
            return
        }

        // 1. If input contains an SM2_ sync capsule, merge it immediately
        if (extractSyncTokenFromRawInput(trimmed) != null) {
            val res = importAndMergeGroupSyncPayload(trimmed, openGroupAfterMerge = true)
            onResult(res.success, res.message)
            return
        }

        // 2. Extract 6-char Join Code and optional explicit groupId from deep link or raw code
        val explicitGid = if (trimmed.contains("gid=", ignoreCase = true)) {
            trimmed.substringAfter("gid=", "").substringBefore("&").substringBefore(" ").trim()
        } else {
            ""
        }
        val normCode6 = com.splitmate.app.data.CloudGroupSyncRepository.normalizeJoinCode6(trimmed)
        if (normCode6.length != 6 && explicitGid.isBlank()) {
            val msg = "Invalid Join Code. Enter a 6-character code (e.g., K7X-9M2)"
            _uiState.update { it.copy(statusBannerMessage = msg) }
            onResult(false, msg)
            return
        }

        val state = _uiState.value
        val myPhone10 = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(state.userPhone)
        val myName = state.currentUserName.trim().ifBlank { "Member" }
        val mySeed = state.currentUserSeed.trim().ifBlank { "$myName|Neutral|open-peeps|Buckwheat" }

        // 3. Check if target group is already available in local memory (offline/instant join)
        val localTargetGroup = state.groups.firstOrNull { g ->
            (explicitGid.isNotBlank() && g.groupId == explicitGid) ||
                (normCode6.length == 6 && getGroupJoinCode(g.groupId).equals(normCode6, ignoreCase = true))
        }
        if (localTargetGroup != null) {
            val targetGid = localTargetGroup.groupId
            if (myPhone10.length == 10) {
                com.splitmate.app.data.CloudGroupSyncRepository.unTombstonePhones(context, targetGid, listOf(myPhone10))
            }
            val existingMembers = state.members.filter { it.groupId == targetGid }
            val matchedByPhone = if (myPhone10.length == 10) {
                existingMembers.firstOrNull {
                    com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == myPhone10
                }
            } else null
            val matchedByName = if (matchedByPhone == null) {
                existingMembers.firstOrNull { m ->
                    com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId).isEmpty() &&
                        m.name.trim().equals(myName, ignoreCase = true)
                }
            } else null

            val updatedGroupMembers = when {
                matchedByPhone != null -> existingMembers.map { m ->
                    if (m.memberId == matchedByPhone.memberId) {
                        m.copy(name = myName, userPhone = myPhone10, inviteStatus = "JOINED", isCurrentUser = true, avatarSeed = mySeed)
                    } else {
                        m.copy(isCurrentUser = false)
                    }
                }
                matchedByName != null -> existingMembers.map { m ->
                    if (m.memberId == matchedByName.memberId) {
                        m.copy(userPhone = myPhone10.ifBlank { m.userPhone }, inviteStatus = "JOINED", isCurrentUser = true, avatarSeed = mySeed)
                    } else {
                        m.copy(isCurrentUser = false)
                    }
                }
                else -> {
                    val newMember = GroupMemberEntity(
                        memberId = "m_${System.currentTimeMillis()}_${myPhone10.takeLast(4).ifBlank { "join" }}",
                        groupId = targetGid,
                        name = myName,
                        avatarSeed = mySeed,
                        upiId = if (myPhone10.length == 10) "${myPhone10}@upi" else "",
                        userPhone = myPhone10,
                        isCurrentUser = true,
                        inviteStatus = "JOINED"
                    )
                    existingMembers.map { it.copy(isCurrentUser = false) } + newMember
                }
            }

            val msg = "Joined \"${localTargetGroup.name}\" via code ${com.splitmate.app.data.CloudGroupSyncRepository.formatJoinCode6(normCode6.ifBlank { getGroupJoinCode(targetGid) })}"
            _uiState.update { curr ->
                curr.copy(
                    members = curr.members.filterNot { it.groupId == targetGid } + updatedGroupMembers,
                    activeGroupId = targetGid,
                    openedGroupDetailId = targetGid,
                    selectedTabName = "LEDGERS",
                    statusBannerMessage = msg
                )
            }
            viewModelScope.launch(ioDispatcher) {
                val d = dao
                if (d != null) {
                    d.insertMembers(updatedGroupMembers)
                    com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                        context = context,
                        dao = d,
                        groupId = targetGid,
                        localUserPhone10 = myPhone10,
                        localUserName = myName,
                        forceIndexPush = true
                    )
                }
            }
            onResult(true, msg)
            return
        }

        // 4. Cloud lookup via splitmate_v2_code_<CODE6>
        val d = dao
        if (d == null) {
            val msg = "Trip code $normCode6 not found locally while offline"
            _uiState.update { it.copy(statusBannerMessage = msg) }
            onResult(false, msg)
            return
        }

        _uiState.update { it.copy(isCloudSyncing = true) }
        viewModelScope.launch(ioDispatcher) {
            val joinedDoc = com.splitmate.app.data.CloudGroupSyncRepository.joinGroupByCodeFromCloud(
                context = context,
                dao = d,
                rawCode6 = normCode6,
                explicitGroupId = explicitGid,
                localUserPhone10 = myPhone10,
                localUserName = myName,
                localUserAvatarSeed = mySeed
            )
            if (joinedDoc != null) {
                val msg = "Joined \"${joinedDoc.group.name}\""
                refreshStateFromDaoSnapshot(d = d, statusMsg = msg)
                openGroupDetail(joinedDoc.group.groupId)
                dispatchOnMain { onResult(true, msg) }
            } else {
                val errMsg = "Could not find an active trip for code ${com.splitmate.app.data.CloudGroupSyncRepository.formatJoinCode6(normCode6)}"
                _uiState.update { it.copy(isCloudSyncing = false, statusBannerMessage = errMsg) }
                dispatchOnMain { onResult(false, errMsg) }
            }
        }
    }

    fun markGreedyTransferSettled(transfer: SplitMateMathEngine.SimplifiedTransfer) {
        val state = _uiState.value
        if (!canCurrentUserMarkTransferPaid(state.activeGroupId, transfer.toMemberId, state)) {
            val restrictionMsg = getMarkPaidRestrictionLabel(
                groupId = state.activeGroupId,
                toMemberId = transfer.toMemberId,
                toMemberName = transfer.toName,
                state = state
            )
            _uiState.update { curr ->
                curr.copy(statusBannerMessage = restrictionMsg)
            }
            return
        }
        // v2.3.6 (device bug): never record the same payment twice. A double tap, or a stale row that
        // is still on screen after the payment was recorded, used to add a second identical settlement.
        val groupSettlements = state.settlements.filter { it.groupId == state.activeGroupId }
        val currentNet = computeGroupMemberNetBalances(state.activeGroupId)
        if (com.splitmate.app.SettlementDuplicateGuard.isDuplicateOrAlreadySettled(
                fromMemberId = transfer.fromMemberId,
                toMemberId = transfer.toMemberId,
                amountCents = transfer.amountCents,
                existingSettlements = groupSettlements,
                netBalances = currentNet,
                nowMillis = System.currentTimeMillis()
            )
        ) {
            _uiState.update { curr ->
                curr.copy(statusBannerMessage = "Already recorded: ${transfer.fromName} → ${transfer.toName}")
            }
            return
        }
        insertSettlementAndSync(state, transfer, "Settled ${transfer.fromName} → ${transfer.toName}")
    }

    /**
     * v2.3.6: records a payment that is not one of the suggested transfers (any payer, receiver and
     * amount), e.g. one person paid another directly instead of following the plan. Same rule as
     * Mark paid: only the receiver or an organizer can record it. Returns null on success, otherwise
     * the reason it was refused.
     */
    fun recordManualPayment(fromMemberId: String, toMemberId: String, amountCents: Long): String? {
        val state = _uiState.value
        val groupMembers = state.members.filter { it.groupId == state.activeGroupId }
        val from = groupMembers.find { it.memberId == fromMemberId } ?: return "Choose who paid"
        val to = groupMembers.find { it.memberId == toMemberId } ?: return "Choose who received it"
        if (from.memberId == to.memberId) return "Payer and receiver must be different people"
        if (amountCents <= 0L) return "Enter an amount above zero"
        if (!canCurrentUserMarkTransferPaid(state.activeGroupId, to.memberId, state)) {
            return getMarkPaidRestrictionLabel(
                groupId = state.activeGroupId,
                toMemberId = to.memberId,
                toMemberName = to.name,
                state = state
            )
        }
        val groupSettlements = state.settlements.filter { it.groupId == state.activeGroupId }
        val now = System.currentTimeMillis()
        val recentTwin = groupSettlements.any {
            it.fromMemberId == from.memberId && it.toMemberId == to.memberId && it.amountCents == amountCents &&
                now - it.settledAt in 0..com.splitmate.app.SettlementDuplicateGuard.DuplicateWindowMillis
        }
        if (recentTwin) return "Already recorded: ${from.name} → ${to.name}"
        val transfer = SplitMateMathEngine.SimplifiedTransfer(
            fromMemberId = from.memberId,
            fromName = from.name,
            toMemberId = to.memberId,
            toName = to.name,
            amountCents = amountCents
        )
        insertSettlementAndSync(state, transfer, "Recorded ${from.name} → ${to.name}")
        return null
    }

    /** True when the current user can record at least one payment in the active trip. */
    fun canCurrentUserRecordPayments(state: SplitMateUiState = _uiState.value): Boolean {
        val groupMembers = state.members.filter { it.groupId == state.activeGroupId }
        return groupMembers.any { canCurrentUserMarkTransferPaid(state.activeGroupId, it.memberId, state) }
    }

    private fun insertSettlementAndSync(
        state: SplitMateUiState,
        transfer: SplitMateMathEngine.SimplifiedTransfer,
        banner: String
    ) {
        com.splitmate.app.data.CloudGroupSyncRepository.setGroupPendingCloudPush(null, state.activeGroupId, true)
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
            syncStatus = if (dao != null || state.isOfflineMode) "PENDING" else "SYNCED"
        )

        _uiState.update { curr ->
            curr.copy(
                settlements = listOf(settlement) + curr.settlements,
                statusBannerMessage = banner
            )
        }

        viewModelScope.launch(ioDispatcher) {
            val d = dao
            if (d != null) {
                d.insertSettlement(settlement)
                val normUserPhone = resolveEffectiveUserPhone10(d)
                runCatching {
                    com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                        context = null,
                        dao = d,
                        groupId = settlement.groupId,
                        localUserPhone10 = normUserPhone,
                        localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                        isLocalMutation = true
                    )
                    refreshStateFromDaoSnapshot(d = d)
                }
            }
        }
    }

    /**
     * v2.3.5 (#4): the device user's 10-digit phone for audit stamps / permission checks. Prefers the
     * profile phone, falls back to the active group's `isCurrentUser` member row.
     */
    fun currentUserPhone10(groupId: String? = null, state: SplitMateUiState = _uiState.value): String {
        val profilePhone = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(state.userPhone)
        if (profilePhone.length == 10) return profilePhone
        val gId = groupId ?: state.activeGroupId
        val me = state.members.firstOrNull { it.groupId == gId && it.isCurrentUser } ?: return ""
        return com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(me.userPhone, me.upiId)
    }

    /**
     * v2.3.5 (#4): only an organizer, the payer, or the creator may edit / delete [expense].
     * Legacy rows (null `createdByPhone`) fall back to organizer + payer.
     */
    fun canCurrentUserModifyExpense(expense: ExpenseEntity, state: SplitMateUiState = _uiState.value): Boolean {
        val groupMembers = state.members.filter { it.groupId == expense.groupId }
        val me = groupMembers.firstOrNull { it.isCurrentUser }
        val payer = groupMembers.firstOrNull { it.memberId == expense.payerId }
        return com.splitmate.app.ExpenseEditPermission.canModify(
            currentPhone = currentUserPhone10(expense.groupId, state),
            isOrganizer = runCatching { isUserGroupOrganizer(expense.groupId, state) }.getOrDefault(false),
            payerPhone = payer?.let { com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) },
            createdByPhone = expense.createdByPhone,
            isCurrentUserPayer = me != null && me.memberId == expense.payerId
        )
    }

    fun canCurrentUserModifyExpense(expenseId: String): Boolean {
        val exp = _uiState.value.expenses.find { it.expenseId == expenseId } ?: return false
        return canCurrentUserModifyExpense(exp)
    }

    /** v2.3.5 (#4): true when the quick edit dialog must lock amount + shares (itemized receipt). */
    fun isExpenseItemized(expense: ExpenseEntity, state: SplitMateUiState = _uiState.value): Boolean =
        com.splitmate.app.ItemizedExpenseGuard.isItemized(
            expense,
            state.splits.filter { it.expenseId == expense.expenseId }
        )

    fun rollbackExpense(expenseId: String) {
        val removedExpense = _uiState.value.expenses.find { it.expenseId == expenseId }
        // v2.3.5 (#4): enforce the edit/delete permission in the ViewModel, not only in the UI.
        if (removedExpense != null && !canCurrentUserModifyExpense(removedExpense)) {
            _uiState.update { it.copy(statusBannerMessage = com.splitmate.app.ExpenseEditPermission.READ_ONLY_REASON) }
            return
        }
        val targetGroupId = removedExpense?.groupId ?: _uiState.value.activeGroupId
        val rolledBackAtMs = System.currentTimeMillis()
        if (targetGroupId.isNotBlank()) {
            com.splitmate.app.data.CloudGroupSyncRepository.setGroupPendingCloudPush(null, targetGroupId, true)
            // v2.3.6: durable tombstone before the local delete (same race as undoSettlement).
            if (removedExpense != null) {
                com.splitmate.app.data.CloudGroupSyncRepository.recordLocalTombstones(
                    context = appContext,
                    groupId = targetGroupId,
                    deletedExpenseIds = mapOf(expenseId to rolledBackAtMs)
                )
            }
        }
        _uiState.update { curr ->
            val removed = curr.expenses.find { it.expenseId == expenseId }
            curr.copy(
                expenses = curr.expenses.filterNot { it.expenseId == expenseId },
                splits = curr.splits.filterNot { it.expenseId == expenseId },
                statusBannerMessage = "Rolled back \"${removed?.title ?: "Expense"}\""
            )
        }
        viewModelScope.launch(ioDispatcher) {
            val d = dao
            if (d != null) {
                d.deleteSplitsForExpense(expenseId)
                d.deleteExpense(expenseId)
                val normUserPhone = resolveEffectiveUserPhone10(d)
                if (targetGroupId.isNotBlank()) {
                    runCatching {
                        com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                            context = null,
                            dao = d,
                            groupId = targetGroupId,
                            localUserPhone10 = normUserPhone,
                            localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                            additionalTombstones = mapOf(expenseId to rolledBackAtMs),
                            isLocalMutation = true
                        )
                        refreshStateFromDaoSnapshot(d = d)
                    }
                }
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        rollbackExpense(expenseId)
    }

    fun undoSettlement(settlementId: String) {
        val removedSettlement = _uiState.value.settlements.find { it.settlementId == settlementId }
        val targetGroupId = removedSettlement?.groupId ?: _uiState.value.activeGroupId
        val undoneAtMs = System.currentTimeMillis()
        if (targetGroupId.isNotBlank()) {
            com.splitmate.app.data.CloudGroupSyncRepository.setGroupPendingCloudPush(null, targetGroupId, true)
            // v2.3.6 (device bug): write the tombstone durably BEFORE the local delete. It used to be
            // recorded only inside the follow-up cloud sync, so a sync that ran first (or never ran)
            // merged the cloud copy straight back and the undone payment reappeared next to the new one.
            com.splitmate.app.data.CloudGroupSyncRepository.recordLocalTombstones(
                context = appContext,
                groupId = targetGroupId,
                deletedSettlementIds = mapOf(settlementId to undoneAtMs)
            )
        }
        _uiState.update { curr ->
            val removed = curr.settlements.find { it.settlementId == settlementId }
            curr.copy(
                settlements = curr.settlements.filterNot { it.settlementId == settlementId },
                statusBannerMessage = "Reverted settlement (${removed?.fromMemberName ?: ""} → ${removed?.toMemberName ?: ""})"
            )
        }
        viewModelScope.launch(ioDispatcher) {
            val d = dao
            if (d != null) {
                d.deleteSettlementById(settlementId)
                val normUserPhone = resolveEffectiveUserPhone10(d)
                if (targetGroupId.isNotBlank()) {
                    runCatching {
                        com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                            context = null,
                            dao = d,
                            groupId = targetGroupId,
                            localUserPhone10 = normUserPhone,
                            localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                            additionalDeletedSettlementIds = mapOf(settlementId to undoneAtMs),
                            isLocalMutation = true
                        )
                        refreshStateFromDaoSnapshot(d = d)
                    }
                }
            }
        }
    }

    data class BatchMemberUpdate(
        val memberId: String,
        val name: String,
        val upiId: String,
        val avatarSeed: String,
        /** v2.3.5 (#7): the "UPI ID" field when the user edited it; null = keep existing VPA rules. */
        val vpaOverride: String? = null
    )

    fun updateAllGroupMembers(updates: List<BatchMemberUpdate>) {
        if (updates.isEmpty()) return
        val updateMap = updates.associateBy { it.memberId }
        var updatedEntitiesToPersist = emptyList<GroupMemberEntity>()
        _uiState.update { curr ->
            val persistedList = mutableListOf<GroupMemberEntity>()
            val nextMembers = curr.members.map { mbr ->
                val upd = updateMap[mbr.memberId]
                if (upd != null) {
                    val extractedPhone10 = com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(upd.upiId, upd.upiId)
                    // v2.3.5 (#7): only derive "<phone>@upi" when there is no real VPA to keep.
                    val cleanUpi = com.splitmate.app.MemberProfileEditRules.resolveUpiId(
                        existingUpiId = mbr.upiId,
                        phoneInput = upd.upiId,
                        vpaInput = upd.vpaOverride
                    )
                    val finalPhone = extractedPhone10.ifBlank {
                        if (upd.upiId.isBlank()) "" else mbr.userPhone
                    }
                    val nextInviteStatus = when {
                        mbr.isCurrentUser -> "JOINED"
                        finalPhone.length == 10 && (mbr.userPhone.isBlank() || mbr.inviteStatus.isBlank()) -> "PENDING"
                        else -> mbr.inviteStatus.ifBlank { "JOINED" }
                    }
                    val updated = mbr.copy(
                        name = upd.name.trim().ifEmpty { mbr.name },
                        upiId = cleanUpi,
                        userPhone = finalPhone,
                        inviteStatus = nextInviteStatus,
                        avatarSeed = upd.avatarSeed.trim().ifEmpty { mbr.avatarSeed }
                    )
                    persistedList.add(updated)
                    updated
                } else mbr
            }
            updatedEntitiesToPersist = persistedList
            curr.copy(
                members = nextMembers,
                statusBannerMessage = "Saved ${updates.size} group member profile(s)"
            )
        }
        viewModelScope.launch(ioDispatcher) {
            val d = dao
            if (d != null && updatedEntitiesToPersist.isNotEmpty()) {
                d.insertMembers(updatedEntitiesToPersist)
                val normUserPhone = resolveEffectiveUserPhone10(d)
                val affectedGroupIds = updatedEntitiesToPersist.map { it.groupId }.toSet()
                affectedGroupIds.forEach { gId ->
                    runCatching {
                        com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                            context = null,
                            dao = d,
                            groupId = gId,
                            localUserPhone10 = normUserPhone,
                            localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                            forceIndexPush = true,
                            isLocalMutation = true
                        )
                    }
                }
                runCatching { refreshStateFromDaoSnapshot(d = d) }
            }
        }
    }

    fun editExistingExpense(
        expenseId: String,
        newTitle: String,
        newTotalRupees: Double,
        newPayerId: String,
        selectedMemberIds: List<String>? = null,
        newTotalCentsOverride: Long? = null,
        /** v2.3.5: new categoryRef; [KEEP_CATEGORY_REF] (default) leaves the stored one untouched. */
        categoryRef: String? = KEEP_CATEGORY_REF
    ) {
        val state = _uiState.value
        val existing = state.expenses.find { it.expenseId == expenseId } ?: return
        // v2.3.5 (#4): only organizer / payer / creator may edit (enforced here, not just in the UI).
        if (!canCurrentUserModifyExpense(existing, state)) {
            _uiState.update { it.copy(statusBannerMessage = com.splitmate.app.ExpenseEditPermission.READ_ONLY_REASON) }
            return
        }
        val cleanTitle = newTitle.trim().ifEmpty { existing.title }
        val inferred = inferStructuredExpenseMetadata(cleanTitle)

        // Guard against editing an expense to collide with another expense's PNR in the same group
        // (labelled "PNR: XXXXXX" 6-10 alphanumeric Flight/Train PNR, or bare 10-digit Train PNR)
        val newPnrDigits = inferred.travelPnr.takeIf { it.isNotBlank() }
        if (!newPnrDigits.isNullOrBlank()) {
            val pnrWordMatcher = Regex("""\b${Regex.escape(newPnrDigits)}\b""", RegexOption.IGNORE_CASE)
            val conflictingExpense = state.expenses.firstOrNull { other ->
                other.groupId == existing.groupId && other.expenseId != expenseId &&
                    (other.travelPnr.equals(newPnrDigits, ignoreCase = true) ||
                        other.title.contains(newPnrDigits) ||
                        pnrWordMatcher.containsMatchIn(other.title))
            }
            if (conflictingExpense != null) {
                _uiState.update { curr ->
                    curr.copy(statusBannerMessage = "PNR $newPnrDigits is already logged in another expense in this group")
                }
                return
            }
        }

        val newTotalCents = (newTotalCentsOverride ?: kotlin.math.round(newTotalRupees * 100.0).toLong()).coerceAtLeast(1L)

        val existingSplits = state.splits.filter { it.expenseId == expenseId && it.finalOwedCents > 0L }
        val groupMembers = state.members.filter { it.groupId == existing.groupId }
        val splitMemberIds = selectedMemberIds?.filter { id -> groupMembers.any { it.memberId == id } }
            ?.ifEmpty { null }
            ?: existingSplits.map { it.memberId }.ifEmpty { groupMembers.map { it.memberId } }

        val resolvedPayerId = newPayerId.ifBlank { existing.payerId }
        val currentUser = groupMembers.find { it.isCurrentUser }
        val chosenMembers = groupMembers.filter { splitMemberIds.contains(it.memberId) }.ifEmpty { groupMembers }
        val equalAllocations = SplitMateMathEngine.splitEquallyZeroDrift(
            totalCents = newTotalCents,
            members = chosenMembers.map { it.memberId to it.name },
            payerId = resolvedPayerId,
            currentUserId = currentUser?.memberId
        )

        // v2.3.5 (#4 guard a): never silently flatten an itemized receipt (tax / tip / remainder /
        // unequal item claims) into an equal split. Money or share changes are refused; title and
        // payer edits keep the original split rows untouched.
        val allExistingSplits = state.splits.filter { it.expenseId == expenseId }
        val isItemized = com.splitmate.app.ItemizedExpenseGuard.isItemized(existing, allExistingSplits)
        if (isItemized && com.splitmate.app.ItemizedExpenseGuard.isBlockedEdit(
                expense = existing,
                splits = allExistingSplits,
                newTotalCents = newTotalCents,
                newParticipantIds = selectedMemberIds?.filter { id -> groupMembers.any { it.memberId == id } }?.ifEmpty { null }
            )
        ) {
            _uiState.update { it.copy(statusBannerMessage = com.splitmate.app.ItemizedExpenseGuard.LOCKED_REASON) }
            return
        }

        com.splitmate.app.data.CloudGroupSyncRepository.setGroupPendingCloudPush(null, existing.groupId, true)
        val updatedExpense = existing.copy(
            title = cleanTitle,
            payerId = resolvedPayerId,
            baseSubtotalCents = if (isItemized) existing.baseSubtotalCents else newTotalCents,
            totalAmountCents = if (isItemized) existing.totalAmountCents else newTotalCents,
            expenseCategory = inferred.expenseCategory.takeIf { it != "OTHER" } ?: existing.expenseCategory,
            travelPnr = inferred.travelPnr.ifBlank { existing.travelPnr },
            providerName = inferred.providerName.ifBlank { existing.providerName },
            scheduledAtEpochMs = inferred.scheduledAtEpochMs ?: existing.scheduledAtEpochMs,
            syncStatus = if (dao != null || state.isOfflineMode) "PENDING" else "SYNCED",
            categoryRef = if (categoryRef == KEEP_CATEGORY_REF) existing.categoryRef else categoryRef?.takeIf { it.isNotBlank() }
        )
        if (categoryRef != KEEP_CATEGORY_REF) {
            com.splitmate.app.ui.category.CustomExpenseCategoryStore.ensurePublished(appContext, existing.groupId, categoryRef)
        }

        val updatedSplits = if (isItemized) allExistingSplits else equalAllocations.mapIndexed { idx, alloc ->
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
                statusBannerMessage = if (isItemized) {
                    "Updated \"$cleanTitle\" (itemized shares kept, ${formatIndianRupeesFromCents(updatedExpense.totalAmountCents)})"
                } else {
                    "Updated \"$cleanTitle\" across ${chosenMembers.size} members (${formatIndianRupeesFromCents(newTotalCents)})"
                }
            )
        }

        viewModelScope.launch(ioDispatcher) {
            val d = dao
            if (d != null) {
                com.splitmate.app.data.ExpenseEditClock.record(appContext, listOf(updatedExpense.expenseId)) // v2.4.0 P1: version by edit time
                d.insertExpenseWithSplits(updatedExpense, updatedSplits)
                val normUserPhone = resolveEffectiveUserPhone10(d)
                runCatching {
                    com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                        context = null,
                        dao = d,
                        groupId = updatedExpense.groupId,
                        localUserPhone10 = normUserPhone,
                        localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                        isLocalMutation = true
                    )
                    refreshStateFromDaoSnapshot(d = d)
                }
            }
        }
    }

    /**
     * v2.3.6 P3 Money check, 1-tap fix: sets a plain (non-itemized) expense's total to the sum of its
     * shares. Only the organizer, the payer or the creator may do it (same rule as editing), and only
     * when [com.splitmate.app.MoneyCheck.canUseSharesTotal] says it is safe. Returns the previous
     * total (for Undo), or null when nothing changed.
     */
    /** v2.4.0 P4: the expense's local edit history, newest first (empty without a database). */
    fun observeExpenseRevisions(expenseId: String): kotlinx.coroutines.flow.Flow<List<com.splitmate.app.data.ExpenseRevisionEntity>> =
        revisionFlowsByExpense.getOrPut(expenseId) {
            dao?.observeExpenseRevisions(expenseId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
        }

    private val revisionFlowsByExpense = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.flow.Flow<List<com.splitmate.app.data.ExpenseRevisionEntity>>>()

    /**
     * v2.4.0 P4: "Restore ₹4,950" after an amount was changed back. A normal edit (same title, payer
     * and people), so it gets a new version and wins everywhere. Itemized receipts stay protected by
     * the itemized edit guard.
     */
    fun restoreExpenseTotal(expenseId: String, totalCents: Long) {
        val state = _uiState.value
        val existing = state.expenses.find { it.expenseId == expenseId } ?: return
        if (totalCents <= 0L) return
        val participants = state.splits.filter { it.expenseId == expenseId && it.finalOwedCents > 0L }.map { it.memberId }
        editExistingExpense(
            expenseId = expenseId,
            newTitle = existing.title,
            newTotalRupees = totalCents / 100.0,
            newPayerId = existing.payerId,
            selectedMemberIds = participants.ifEmpty { null },
            newTotalCentsOverride = totalCents
        )
    }

    fun useSharesTotalForExpense(expenseId: String): Long? {
        val state = _uiState.value
        val existing = state.expenses.find { it.expenseId == expenseId } ?: return null
        if (!canCurrentUserModifyExpense(existing, state)) {
            _uiState.update { it.copy(statusBannerMessage = com.splitmate.app.ExpenseEditPermission.READ_ONLY_REASON) }
            return null
        }
        val rows = state.splits.filter { it.expenseId == expenseId }
        if (!com.splitmate.app.MoneyCheck.canUseSharesTotal(existing, rows)) return null
        val sharesTotal = rows.sumOf { it.finalOwedCents }
        persistExpenseTotal(existing, sharesTotal)
        _uiState.update {
            it.copy(statusBannerMessage = "${cleanDisplayExpenseTitle(existing.title)} set to ${formatIndianRupeesFromCents(sharesTotal)}")
        }
        return existing.totalAmountCents
    }

    private fun persistExpenseTotal(existing: ExpenseEntity, totalCents: Long) {
        com.splitmate.app.data.CloudGroupSyncRepository.setGroupPendingCloudPush(null, existing.groupId, true)
        val updated = existing.copy(
            totalAmountCents = totalCents,
            baseSubtotalCents = totalCents,
            syncStatus = "PENDING"
        )
        _uiState.update { curr ->
            curr.copy(expenses = curr.expenses.map { if (it.expenseId == existing.expenseId) updated else it })
        }
        viewModelScope.launch(ioDispatcher) {
            val d = dao ?: return@launch
            com.splitmate.app.data.ExpenseEditClock.record(appContext, listOf(updated.expenseId)) // v2.4.0 P1: version by edit time
            d.insertExpense(updated)
            val normUserPhone = resolveEffectiveUserPhone10(d)
            runCatching {
                com.splitmate.app.data.CloudGroupSyncRepository.syncGroupWithCloud(
                    context = null,
                    dao = d,
                    groupId = updated.groupId,
                    localUserPhone10 = normUserPhone,
                    localUserName = _uiState.value.currentUserName.ifBlank { "You" },
                    isLocalMutation = true
                )
                refreshStateFromDaoSnapshot(d = d)
            }
        }
    }

    /**
     * Switches the local device's active perspective ("Viewing as: <Member> (You)") within [groupId]
     * in `<16ms` by toggling `GroupMemberEntity.isCurrentUser` and persisting to Room.
     */
    fun claimGroupMemberPerspective(groupId: String, memberId: String) {
        val state = _uiState.value
        val targetGroupMembers = state.members.filter { it.groupId == groupId }
        val claimedMember = targetGroupMembers.find { it.memberId == memberId } ?: return
        val groupName = state.groups.find { it.groupId == groupId }?.name ?: "Trip"

        val updatedGroupMembers = targetGroupMembers.map { m ->
            m.copy(isCurrentUser = (m.memberId == memberId))
        }

        _uiState.update { curr ->
            curr.copy(
                members = curr.members.map { m ->
                    if (m.groupId == groupId) {
                        m.copy(isCurrentUser = (m.memberId == memberId))
                    } else {
                        m
                    }
                },
                statusBannerMessage = "Viewing \"$groupName\" as ${claimedMember.name} (You)"
            )
        }

        viewModelScope.launch(ioDispatcher) {
            dao?.insertMembers(updatedGroupMembers)
        }
    }


    /**
     * Computes the canonical per-member net balance map (in integer paise/cents) for [groupId].
     * Positive = creditor (`YOU GET BACK`), Negative = debtor (`YOU OWE`), Zero = settled.
     */
    fun computeGroupMemberNetBalances(groupId: String = _uiState.value.activeGroupId): Map<String, Long> {
        val state = _uiState.value
        val groupMembers = state.members.filter { it.groupId == groupId }
        if (groupMembers.isEmpty()) return emptyMap()
        val groupExpenses = state.expenses.filter { it.groupId == groupId }
        val groupExpenseIds = groupExpenses.map { it.expenseId }.toSet()
        val groupSplits = state.splits.filter { groupExpenseIds.contains(it.expenseId) }
        val groupSettlements = state.settlements.filter { it.groupId == groupId }
        return computeGroupNetBalances(groupMembers, groupExpenses, groupSplits, groupSettlements)
    }

    /**
     * Exports a $0.00-server-cost compressed Sync Capsule (`SM2_<base64url>`) containing the
     * canonical Group (`G`), Members (`M`), Expenses (`E`), Splits (`S`), and Settlements (`T`).
     * Strictly zero Unicode emojis in [GroupSyncExportBundle.whatsappShareText].
     */
    fun exportGroupSyncPayload(groupId: String = _uiState.value.activeGroupId): GroupSyncExportBundle? {
        val state = _uiState.value
        val group = state.groups.find { it.groupId == groupId } ?: return null
        val groupMembers = state.members
            .filter { it.groupId == groupId }
            .sortedBy { it.memberId }
        if (groupMembers.isEmpty()) return null

        val groupExpenses = state.expenses
            .filter { it.groupId == groupId }
            .sortedWith(compareBy<ExpenseEntity> { it.createdAt }.thenBy { it.expenseId })
        val groupExpenseIds = groupExpenses.map { it.expenseId }.toSet()
        val groupSplits = state.splits
            .filter { groupExpenseIds.contains(it.expenseId) }
            .sortedWith(compareBy<ExpenseSplitEntity> { it.expenseId }.thenBy { it.memberId }.thenBy { it.splitId })
        val groupSettlements = state.settlements
            .filter { it.groupId == groupId }
            .sortedWith(compareBy<SettlementEntity> { it.settledAt }.thenBy { it.settlementId })

        val lines = mutableListOf<String>()
        lines.add("V|2")
        lines.add(
            listOf(
                "G",
                urlEnc(group.groupId),
                urlEnc(group.name),
                urlEnc(group.currencyCode),
                urlEnc(group.iconName),
                group.createdAt.toString()
            ).joinToString("|")
        )
        groupMembers.forEach { m ->
            val phone10 = PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId)
            lines.add(
                listOf(
                    "M",
                    urlEnc(m.memberId),
                    urlEnc(m.groupId),
                    urlEnc(m.name),
                    urlEnc(m.avatarSeed),
                    urlEnc(m.upiId),
                    if (m.isCurrentUser) "1" else "0",
                    urlEnc(phone10),
                    urlEnc(m.inviteStatus)
                ).joinToString("|")
            )
        }
        groupExpenses.forEach { e ->
            lines.add(
                listOf(
                    "E",
                    urlEnc(e.expenseId),
                    urlEnc(e.groupId),
                    urlEnc(e.title),
                    urlEnc(e.payerId),
                    e.baseSubtotalCents.toString(),
                    e.taxCents.toString(),
                    e.tipCents.toString(),
                    e.totalAmountCents.toString(),
                    e.lockedMultiplier.toString(),
                    e.unassignedBaseCents.toString(),
                    urlEnc(e.currencyCode),
                    e.lockedExchangeRate.toString(),
                    urlEnc(e.syncStatus),
                    e.createdAt.toString()
                ).joinToString("|")
            )
        }
        groupSplits.forEach { s ->
            lines.add(
                listOf(
                    "S",
                    urlEnc(s.splitId),
                    urlEnc(s.expenseId),
                    urlEnc(s.memberId),
                    s.baseClaimedCents.toString(),
                    s.finalOwedCents.toString(),
                    if (s.plusOneCent) "1" else "0"
                ).joinToString("|")
            )
        }
        groupSettlements.forEach { t ->
            lines.add(
                listOf(
                    "T",
                    urlEnc(t.settlementId),
                    urlEnc(t.groupId),
                    urlEnc(t.fromMemberId),
                    urlEnc(t.fromMemberName),
                    urlEnc(t.toMemberId),
                    urlEnc(t.toMemberName),
                    t.amountCents.toString(),
                    urlEnc(t.currencyCode),
                    t.lockedExchangeRate.toString(),
                    urlEnc(t.syncStatus),
                    t.settledAt.toString()
                ).joinToString("|")
            )
        }

        val wireText = lines.joinToString("\n")
        val compressedBytes = ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { gzip ->
                gzip.write(wireText.toByteArray(Charsets.UTF_8))
            }
            baos.toByteArray()
        }
        val base64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(compressedBytes)
        val syncToken = "SM2_$base64Url"
        val deepLinkUri = "splitmate://trip-sync?payload=$syncToken"
        val joinCode6 = getGroupJoinCode(group.groupId)
        val formattedJoinCode = CloudGroupSyncRepository.formatJoinCode6(joinCode6)
        val totalSpendCents = groupExpenses.sumOf { it.totalAmountCents }
        val formattedTotalSpend = formatIndianRupeesFromCents(
            cents = totalSpendCents,
            includePlusSign = false,
            currencySymbol = "₹"
        )
        val memberNamesSummary = groupMembers.joinToString(", ") { it.name }

        // Strictly zero Unicode emojis anywhere in whatsappShareText
        val whatsappShareText = buildString {
            append("*SplitMate Trip Sync: ${group.name}*\n")
            append("Trip Join Code: $formattedJoinCode\n")
            append("Travelers (${groupMembers.size}): $memberNamesSummary\n")
            append("Bookings & Expenses: ${groupExpenses.size} | Total Spend: $formattedTotalSpend\n\n")
            append("1. Enter Trip Join Code in SplitMate: $formattedJoinCode\n")
            append("2. Or tap link to open & sync in SplitMate:\n")
            append("$deepLinkUri\n\n")
            append("3. Or copy this message and tap 'Paste & Merge Sync Capsule' in SplitMate:\n")
            append(syncToken)
        }

        return GroupSyncExportBundle(
            groupId = group.groupId,
            groupName = group.name,
            memberCount = groupMembers.size,
            expenseCount = groupExpenses.size,
            settlementCount = groupSettlements.size,
            totalSpendCents = totalSpendCents,
            syncToken = syncToken,
            deepLinkUri = deepLinkUri,
            whatsappShareText = whatsappShareText,
            compressedBytesSize = compressedBytes.size,
            joinCode6 = joinCode6
        )
    }

    /**
     * Uploads the SM2_ sync capsule to a zero-cost cloud KV store (bytebin.lucko.me) 
     * and returns a short, clickable invite link.
     */
    fun generateShortInviteLink(groupId: String, onResult: (String) -> Unit) {
        val bundle = exportGroupSyncPayload(groupId) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val payload = bundle.syncToken
            val shortUrl = runCatching {
                val url = java.net.URL("https://bytebin.lucko.me/post")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("User-Agent", "SplitMate/2.0")
                conn.setRequestProperty("Content-Type", "text/plain")
                conn.doOutput = true
                conn.outputStream.write(payload.toByteArray(Charsets.UTF_8))
                
                val response = conn.inputStream.bufferedReader().readText()
                val shortKey = org.json.JSONObject(response).getString("key")
                "https://akshaykaradkar.github.io/splitmate/join?g=$shortKey"
            }.getOrElse {
                // Fallback to long deep link if offline
                bundle.deepLinkUri
            }
            
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onResult(shortUrl)
            }
        }
    }

    /**
     * Fetches a sync capsule from the cloud KV store and merges it.
     */
    fun resolveAndMergeShortInviteKey(shortKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val payload = runCatching {
                val url = java.net.URL("https://bytebin.lucko.me/$shortKey")
                url.readText()
            }.getOrNull()
            
            if (!payload.isNullOrBlank()) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    importAndMergeGroupSyncPayload(payload, openGroupAfterMerge = true)
                }
            }
        }
    }

    /**
     * Imports an `SM2_<base64url>` Sync Capsule (or full WhatsApp message / `splitmate://trip-sync` URI),
     * performs an idempotent CRDT-style union merge by primary keys (`groupId`, `memberId`, `expenseId`,
     * `splitId`, `settlementId`), and resolves the local user's perspective via 4-stage identity resolution.
     */
    fun importAndMergeGroupSyncPayload(
        rawPayloadOrMessage: String,
        openGroupAfterMerge: Boolean = true
    ): GroupSyncMergeResult {
        val token = extractSyncTokenFromRawInput(rawPayloadOrMessage)
            ?: return GroupSyncMergeResult(
                success = false,
                message = "No valid SplitMate SM2_ sync capsule found"
            )

        val wireText = runCatching {
            val base64Part = token.removePrefix("SM2_").trim()
            val compressed = Base64.getUrlDecoder().decode(base64Part)
            GZIPInputStream(ByteArrayInputStream(compressed)).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrElse {
            return GroupSyncMergeResult(
                success = false,
                message = "Invalid or corrupted SplitMate sync capsule"
            )
        }

        var parsedGroup: ExpenseGroupEntity? = null
        val parsedMembers = mutableListOf<GroupMemberEntity>()
        val parsedExpenses = mutableListOf<ExpenseEntity>()
        val parsedSplits = mutableListOf<ExpenseSplitEntity>()
        val parsedSettlements = mutableListOf<SettlementEntity>()

        wireText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                val parts = line.split("|")
                when (parts.firstOrNull()) {
                    "G" -> if (parts.size >= 6) {
                        parsedGroup = ExpenseGroupEntity(
                            groupId = urlDec(parts[1]),
                            name = urlDec(parts[2]).toSmartTitleCase(),
                            currencyCode = urlDec(parts[3]).ifBlank { "INR" },
                            iconName = urlDec(parts[4]).ifBlank { "Flight" },
                            createdAt = parts[5].toLongOrNull() ?: System.currentTimeMillis()
                        )
                    }
                    "M" -> if (parts.size >= 7) {
                        val rawUpi = urlDec(parts[5])
                        val rawPhone = if (parts.size >= 8) urlDec(parts[7]) else ""
                        val rawStatus = if (parts.size >= 9) urlDec(parts[8]).ifBlank { "JOINED" } else "JOINED"
                        val phone10 = PhoneIdentityValidator.extractMemberPhone10(rawPhone, rawUpi)
                        parsedMembers.add(
                            GroupMemberEntity(
                                memberId = urlDec(parts[1]),
                                groupId = urlDec(parts[2]),
                                name = urlDec(parts[3]),
                                avatarSeed = urlDec(parts[4]).ifBlank { urlDec(parts[3]) },
                                upiId = rawUpi,
                                isCurrentUser = parts[6] == "1",
                                userPhone = phone10,
                                inviteStatus = rawStatus
                            )
                        )
                    }
                    "E" -> if (parts.size >= 15) {
                        parsedExpenses.add(
                            ExpenseEntity(
                                expenseId = urlDec(parts[1]),
                                groupId = urlDec(parts[2]),
                                title = urlDec(parts[3]),
                                payerId = urlDec(parts[4]),
                                baseSubtotalCents = parts[5].toLongOrNull() ?: 0L,
                                taxCents = parts[6].toLongOrNull() ?: 0L,
                                tipCents = parts[7].toLongOrNull() ?: 0L,
                                totalAmountCents = parts[8].toLongOrNull() ?: 0L,
                                lockedMultiplier = parts[9].toDoubleOrNull() ?: 1.0,
                                unassignedBaseCents = parts[10].toLongOrNull() ?: 0L,
                                currencyCode = urlDec(parts[11]).ifBlank { "INR" },
                                lockedExchangeRate = parts[12].toDoubleOrNull() ?: 1.0,
                                syncStatus = urlDec(parts[13]).ifBlank { "SYNCED" },
                                createdAt = parts[14].toLongOrNull() ?: System.currentTimeMillis()
                            )
                        )
                    }
                    "S" -> if (parts.size >= 7) {
                        parsedSplits.add(
                            ExpenseSplitEntity(
                                splitId = urlDec(parts[1]),
                                expenseId = urlDec(parts[2]),
                                memberId = urlDec(parts[3]),
                                baseClaimedCents = parts[4].toLongOrNull() ?: 0L,
                                finalOwedCents = parts[5].toLongOrNull() ?: 0L,
                                plusOneCent = parts[6] == "1"
                            )
                        )
                    }
                    "T" -> if (parts.size >= 12) {
                        parsedSettlements.add(
                            SettlementEntity(
                                settlementId = urlDec(parts[1]),
                                groupId = urlDec(parts[2]),
                                fromMemberId = urlDec(parts[3]),
                                fromMemberName = urlDec(parts[4]),
                                toMemberId = urlDec(parts[5]),
                                toMemberName = urlDec(parts[6]),
                                amountCents = parts[7].toLongOrNull() ?: 0L,
                                currencyCode = urlDec(parts[8]).ifBlank { "INR" },
                                lockedExchangeRate = parts[9].toDoubleOrNull() ?: 1.0,
                                syncStatus = urlDec(parts[10]).ifBlank { "SYNCED" },
                                settledAt = parts[11].toLongOrNull() ?: System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

        val incomingGroup = parsedGroup ?: return GroupSyncMergeResult(
            success = false,
            message = "Sync capsule is missing group metadata"
        )
        val groupId = incomingGroup.groupId
        val incomingGroupMembers = parsedMembers.filter { it.groupId == groupId }
        if (incomingGroupMembers.isEmpty()) {
            return GroupSyncMergeResult(
                success = false,
                message = "Sync capsule has no group members"
            )
        }

        // Record remote organizer's phone if not yet recorded locally
        if (CloudGroupSyncRepository.getOrganizerPhone10(groupId).isBlank()) {
            val remoteSender = incomingGroupMembers.firstOrNull { it.isCurrentUser } ?: incomingGroupMembers.firstOrNull()
            val remoteSenderPhone = remoteSender?.let { PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) }.orEmpty()
            if (remoteSenderPhone.length == 10) {
                CloudGroupSyncRepository.setOrganizerPhone10(groupId, remoteSenderPhone)
            }
        }

        if (!isRoomHydrated) {
            synchronized(pendingColdStartSyncRequests) {
                if (!isRoomHydrated) {
                    pendingColdStartSyncRequests.add(
                        Pair(rawPayloadOrMessage, openGroupAfterMerge)
                    )
                    return GroupSyncMergeResult(
                        success = true,
                        groupId = groupId,
                        groupName = incomingGroup.name,
                        mergedMemberCount = incomingGroupMembers.size,
                        mergedExpenseCount = parsedExpenses.count { it.groupId == groupId },
                        message = "Queued cold-start sync for \"${incomingGroup.name}\""
                    )
                }
            }
        }

        val state = _uiState.value
        val existingGroup = state.groups.find { it.groupId == groupId }
        val mergedGroup = if (existingGroup != null) {
            incomingGroup.copy(
                name = incomingGroup.name.ifBlank { existingGroup.name },
                iconName = incomingGroup.iconName.ifBlank { existingGroup.iconName },
                createdAt = minOf(existingGroup.createdAt, incomingGroup.createdAt)
            )
        } else {
            incomingGroup
        }

        // 1. Union merge members by memberId
        val existingGroupMembers = state.members.filter { it.groupId == groupId }
        val memberMap = linkedMapOf<String, GroupMemberEntity>()
        existingGroupMembers.forEach { m -> memberMap[m.memberId] = m }
        incomingGroupMembers.forEach { inc ->
            val prev = memberMap[inc.memberId]
            val incPhone = PhoneIdentityValidator.extractMemberPhone10(inc.userPhone, inc.upiId)
            memberMap[inc.memberId] = if (prev != null) {
                val prevPhone = PhoneIdentityValidator.extractMemberPhone10(prev.userPhone, prev.upiId)
                prev.copy(
                    name = inc.name.ifBlank { prev.name },
                    avatarSeed = inc.avatarSeed.ifBlank { prev.avatarSeed },
                    upiId = inc.upiId.ifBlank { prev.upiId },
                    userPhone = incPhone.ifBlank { prevPhone }
                )
            } else {
                inc.copy(userPhone = incPhone)
            }
        }
        val mergedMembersUnlocked = memberMap.values.toList()

        // 2. Automatic Local Perspective Identity Resolution (PA-8)
        val matchedPerspectiveMemberId = findMatchingLocalPerspectiveMemberId(
            mergedGroupMembers = mergedMembersUnlocked,
            existingLocalGroupMembers = existingGroupMembers,
            state = state
        )

        val finalGroupMembers: List<GroupMemberEntity>
        val resolvedPerspectiveMemberId: String
        if (matchedPerspectiveMemberId != null) {
            resolvedPerspectiveMemberId = matchedPerspectiveMemberId
            val localPhone10 = PhoneIdentityValidator.extractMemberPhone10(state.userPhone, state.userUpiId)
            finalGroupMembers = mergedMembersUnlocked.map { m ->
                val isMe = (m.memberId == resolvedPerspectiveMemberId)
                m.copy(
                    isCurrentUser = isMe,
                    userPhone = if (isMe && m.userPhone.isBlank() && localPhone10.length == 10) localPhone10 else m.userPhone,
                    inviteStatus = if (isMe) "JOINED" else m.inviteStatus
                )
            }
        } else {
            // PA-8: Uninvited user importing capsule or QR code — never adopt remote sender's isCurrentUser flag;
            // append a new GroupMemberEntity for the local user with isCurrentUser = true and inviteStatus = "JOINED".
            val localPhone10 = PhoneIdentityValidator.extractMemberPhone10(state.userPhone, state.userUpiId)
            val newMemberId = if (localPhone10.length == 10) {
                "m_${groupId}_${localPhone10}"
            } else {
                "m_${groupId}_${UUID.randomUUID().toString().take(8)}"
            }
            val localDisplayName = state.currentUserName.trim().ifBlank { "Member" }
            val localUpi = state.userUpiId.trim().ifBlank {
                if (localPhone10.length == 10) "$localPhone10@upi" else ""
            }
            val newLocalMember = GroupMemberEntity(
                memberId = newMemberId,
                groupId = groupId,
                name = localDisplayName,
                avatarSeed = localDisplayName,
                upiId = localUpi,
                isCurrentUser = true,
                userPhone = localPhone10,
                inviteStatus = "JOINED"
            )
            resolvedPerspectiveMemberId = newMemberId
            finalGroupMembers = mergedMembersUnlocked.map { it.copy(isCurrentUser = false) } + newLocalMember
        }

        val claimedMember = finalGroupMembers.find { it.memberId == resolvedPerspectiveMemberId }
        val validMemberIds = finalGroupMembers.map { it.memberId }.toSet()

        // 3. Union merge expenses by expenseId (defensively filtered against validMemberIds for FK safety)
        val existingGroupExpenses = state.expenses.filter {
            it.groupId == groupId && validMemberIds.contains(it.payerId)
        }
        val existingExpenseIds = existingGroupExpenses.map { it.expenseId }.toSet()
        val incomingGroupExpenses = parsedExpenses.filter {
            it.groupId == groupId && validMemberIds.contains(it.payerId)
        }
        val newlyAddedExpenseCount = incomingGroupExpenses.count { !existingExpenseIds.contains(it.expenseId) }

        val expenseMap = linkedMapOf<String, ExpenseEntity>()
        existingGroupExpenses.forEach { e -> expenseMap[e.expenseId] = e }
        incomingGroupExpenses.forEach { inc ->
            // v2.3.5 (#4): the SM2 capsule has no createdByPhone column; keep the local audit stamp.
            val prior = expenseMap[inc.expenseId]
            expenseMap[inc.expenseId] = if (inc.createdByPhone == null && prior?.createdByPhone != null) {
                inc.copy(createdByPhone = prior.createdByPhone)
            } else {
                inc
            }
        }
        val mergedGroupExpenses = expenseMap.values.sortedWith(
            compareByDescending<ExpenseEntity> { it.createdAt }.thenBy { it.expenseId }
        )
        val mergedGroupExpenseIds = mergedGroupExpenses.map { it.expenseId }.toSet()

        // 4. Union merge splits by (expenseId, memberId) so unique & FK constraints are strictly respected
        val existingGroupSplits = state.splits.filter {
            mergedGroupExpenseIds.contains(it.expenseId) && validMemberIds.contains(it.memberId)
        }
        val incomingGroupSplits = parsedSplits.filter {
            mergedGroupExpenseIds.contains(it.expenseId) && validMemberIds.contains(it.memberId)
        }
        val incomingSplitExpenseIds = incomingGroupSplits.map { it.expenseId }.toSet()
        val splitKeyMap = linkedMapOf<String, ExpenseSplitEntity>()
        // Keep existing splits for expenses not updated in incoming capsule (or as baseline)
        existingGroupSplits.filterNot { incomingSplitExpenseIds.contains(it.expenseId) }.forEach { sp ->
            splitKeyMap["${sp.expenseId}|${sp.memberId}"] = sp
        }
        incomingGroupSplits.forEach { sp ->
            splitKeyMap["${sp.expenseId}|${sp.memberId}"] = sp
        }
        val mergedGroupSplits = splitKeyMap.values.toList()

        // 5. Union merge settlements by settlementId (defensively filtered against validMemberIds for FK safety)
        val existingGroupSettlements = state.settlements.filter {
            it.groupId == groupId &&
                validMemberIds.contains(it.fromMemberId) &&
                validMemberIds.contains(it.toMemberId)
        }
        val existingSettlementIds = existingGroupSettlements.map { it.settlementId }.toSet()
        val incomingGroupSettlements = parsedSettlements.filter {
            it.groupId == groupId &&
                validMemberIds.contains(it.fromMemberId) &&
                validMemberIds.contains(it.toMemberId)
        }
        val newlyAddedSettlementCount = incomingGroupSettlements.count { !existingSettlementIds.contains(it.settlementId) }

        val settlementMap = linkedMapOf<String, SettlementEntity>()
        existingGroupSettlements.forEach { st -> settlementMap[st.settlementId] = st }
        incomingGroupSettlements.forEach { st -> settlementMap[st.settlementId] = st }
        val mergedGroupSettlements = settlementMap.values.sortedWith(
            compareByDescending<SettlementEntity> { it.settledAt }.thenBy { it.settlementId }
        )

        val statusMsg = "Synced \"${mergedGroup.name}\" as ${claimedMember?.name ?: "Member"} (${mergedGroupExpenses.size} expenses)"

        _uiState.update { curr ->
            val otherGroups = curr.groups.filterNot { it.groupId == groupId }
            val otherMembers = curr.members.filterNot { it.groupId == groupId }
            val otherExpenses = curr.expenses.filterNot { it.groupId == groupId }
            val currentGroupExpenseIds = (curr.expenses.filter { it.groupId == groupId }.map { it.expenseId } + mergedGroupExpenseIds).toSet()
            val otherSplits = curr.splits.filterNot { currentGroupExpenseIds.contains(it.expenseId) }
            val otherSettlements = curr.settlements.filterNot { it.groupId == groupId }

            curr.copy(
                groups = listOf(mergedGroup) + otherGroups,
                members = otherMembers + finalGroupMembers,
                expenses = mergedGroupExpenses + otherExpenses,
                splits = otherSplits + mergedGroupSplits,
                settlements = mergedGroupSettlements + otherSettlements,
                activeGroupId = if (openGroupAfterMerge) groupId else curr.activeGroupId.ifBlank { groupId },
                openedGroupDetailId = if (openGroupAfterMerge) groupId else curr.openedGroupDetailId,
                selectedTabName = if (openGroupAfterMerge) "LEDGERS" else curr.selectedTabName,
                statusBannerMessage = statusMsg
            )
        }

        viewModelScope.launch(ioDispatcher) {
            dao?.insertGroup(mergedGroup)
            dao?.insertMembers(finalGroupMembers)
            mergedGroupExpenses.forEach { exp ->
                val expSplits = mergedGroupSplits.filter { it.expenseId == exp.expenseId }
                dao?.insertExpenseWithSplits(exp, expSplits)
            }
            mergedGroupSettlements.forEach { st ->
                dao?.insertSettlement(st)
            }
        }

        return GroupSyncMergeResult(
            success = true,
            groupId = groupId,
            groupName = mergedGroup.name,
            claimedMemberId = resolvedPerspectiveMemberId,
            claimedMemberName = claimedMember?.name,
            mergedMemberCount = finalGroupMembers.size,
            mergedExpenseCount = mergedGroupExpenses.size,
            mergedSplitCount = mergedGroupSplits.size,
            mergedSettlementCount = mergedGroupSettlements.size,
            newlyAddedExpenseCount = newlyAddedExpenseCount,
            newlyAddedSettlementCount = newlyAddedSettlementCount,
            message = statusMsg
        )
    }

    /**
     * Automatic Identity Resolution for local perspective (`isCurrentUser`):
     * 1. Existing local claim in this group
     * 2. 10-digit Indian mobile number match (`state.userPhone` + `userUpiId` extraction)
     * 3. Normalized UPI VPA match
     * 4. Local profile name / first-name match
     */
    fun resolveLocalPerspectiveMemberId(
        mergedGroupMembers: List<GroupMemberEntity>,
        existingLocalGroupMembers: List<GroupMemberEntity> = emptyList(),
        state: SplitMateUiState = _uiState.value
    ): String {
        if (mergedGroupMembers.isEmpty()) return ""
        return findMatchingLocalPerspectiveMemberId(
            mergedGroupMembers = mergedGroupMembers,
            existingLocalGroupMembers = existingLocalGroupMembers,
            state = state
        ) ?: mergedGroupMembers.firstOrNull { it.isCurrentUser }?.memberId
            ?: mergedGroupMembers.first().memberId
    }

    private fun findMatchingLocalPerspectiveMemberId(
        mergedGroupMembers: List<GroupMemberEntity>,
        existingLocalGroupMembers: List<GroupMemberEntity> = emptyList(),
        state: SplitMateUiState = _uiState.value
    ): String? {
        if (mergedGroupMembers.isEmpty()) return null

        // Stage 1: Existing local claim in this group on this device
        val existingLocalClaim = existingLocalGroupMembers.find { it.isCurrentUser }
        if (existingLocalClaim != null && mergedGroupMembers.any { it.memberId == existingLocalClaim.memberId }) {
            return existingLocalClaim.memberId
        }

        // Collect local user's phone(s) and VPA(s) from userPhone, userUpiId and other local groups' isCurrentUser records
        val localRawTokens = buildList {
            if (state.userPhone.isNotBlank()) add(state.userPhone)
            addAll(state.userUpiId.split("|").map { it.trim() }.filter { it.isNotEmpty() })
            state.members.filter { it.isCurrentUser && it.groupId != mergedGroupMembers.first().groupId }
                .forEach { m ->
                    if (m.userPhone.isNotBlank()) add(m.userPhone)
                    addAll(m.upiId.split("|").map { it.trim() }.filter { it.isNotEmpty() })
                }
        }

        val localPhones = localRawTokens
            .map { cleanIndianTenDigitPhone(it.substringBefore("@")) }
            .filter { it.length == 10 }
            .toSet()

        // Stage 2: 10-Digit Indian Phone Match
        if (localPhones.isNotEmpty()) {
            val phoneMatch = mergedGroupMembers.firstOrNull { m ->
                val mPhones = buildList {
                    if (m.userPhone.isNotBlank()) add(cleanIndianTenDigitPhone(m.userPhone))
                    addAll(m.upiId.split("|").map { cleanIndianTenDigitPhone(it.trim().substringBefore("@")) })
                }
                mPhones.any { memberPhone -> memberPhone.length == 10 && localPhones.contains(memberPhone) }
            }
            if (phoneMatch != null) return phoneMatch.memberId
        }

        // Stage 3: Normalized UPI VPA Match
        val localVpas = localRawTokens
            .filter { it.contains("@") }
            .map { it.lowercase(Locale.US) }
            .toSet()
        if (localVpas.isNotEmpty()) {
            val vpaMatch = mergedGroupMembers.firstOrNull { m ->
                m.upiId.split("|").any { part ->
                    val cleanPart = part.trim().lowercase(Locale.US)
                    cleanPart.contains("@") && localVpas.contains(cleanPart)
                }
            }
            if (vpaMatch != null) return vpaMatch.memberId
        }

        // Stage 4: Local Profile Name / First-Name Match
        val cleanLocalName = state.currentUserName.trim()
        if (cleanLocalName.isNotBlank() &&
            !cleanLocalName.equals("You", ignoreCase = true) &&
            !cleanLocalName.equals("Explorer", ignoreCase = true)
        ) {
            val exactNameMatch = mergedGroupMembers.firstOrNull {
                it.name.trim().equals(cleanLocalName, ignoreCase = true)
            }
            if (exactNameMatch != null) return exactNameMatch.memberId

            val localFirst = cleanLocalName.split(Regex("\\s+")).firstOrNull()?.lowercase(Locale.US).orEmpty()
            if (localFirst.length >= 3) {
                val firstNameMatch = mergedGroupMembers.firstOrNull { m ->
                    val memberFirst = m.name.trim().split(Regex("\\s+")).firstOrNull()?.lowercase(Locale.US).orEmpty()
                    memberFirst == localFirst
                }
                if (firstNameMatch != null) return firstNameMatch.memberId
            }
        }

        return null
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
        return state.activeJoinedGroups.sumOf { group ->
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
        val rows: List<MemberSplitBreakdownRow>,
        // v2.3.3: UI-only classification (PERSONAL / ON_BEHALF / SHARED); never affects balances.
        val splitNature: com.splitmate.app.ExpenseSplitNature = com.splitmate.app.ExpenseSplitNature.SHARED,
        val soleParticipantMemberId: String? = null,
        val soleParticipantName: String? = null,
        val soleParticipantIsCurrentUser: Boolean = false,
        val soleParticipantShare: String = ""
    ) {
        /** Full "Paid by …" subtitle; SHARED keeps [sharedDetail] verbatim. */
        fun paidBySubtitle(payerName: String, sharedDetail: String): String =
            com.splitmate.app.ExpenseSplitCopy.paidBySubtitle(
                splitNature, payerName, soleParticipantName, soleParticipantIsCurrentUser, soleParticipantShare, sharedDetail
            )

        /** Compact history-row subtitle; SHARED keeps [sharedDetail] verbatim. */
        fun historyRowSubtitle(payerName: String, sharedDetail: String): String =
            com.splitmate.app.ExpenseSplitCopy.historyRowSubtitle(
                splitNature, payerName, soleParticipantName, soleParticipantIsCurrentUser, soleParticipantShare, sharedDetail
            )

        /** Short label replacing per-person figures; SHARED keeps [sharedDetail] verbatim. */
        fun compactDetail(sharedDetail: String): String =
            com.splitmate.app.ExpenseSplitCopy.compactDetail(
                splitNature, soleParticipantName, soleParticipantIsCurrentUser, sharedDetail
            )

        /** Caption replacing "₹x / traveler"; SHARED keeps [sharedDetail] verbatim. */
        fun perPersonCaption(sharedDetail: String): String =
            com.splitmate.app.ExpenseSplitCopy.perPersonCaption(
                splitNature, soleParticipantName, soleParticipantIsCurrentUser, soleParticipantShare, sharedDetail
            )

        /** Label replacing "Equal split · N members"; SHARED keeps [sharedDetail] verbatim. */
        fun splitModeLabel(sharedDetail: String): String =
            com.splitmate.app.ExpenseSplitCopy.splitModeLabel(
                splitNature, soleParticipantName, soleParticipantIsCurrentUser, sharedDetail
            )
    }

    companion object {
        const val PREFS_NAME_SPLITMATE = "splitmate_prefs"
        const val PREF_KEY_EXPRESSIVE_THEME_MODE = "expressive_theme_mode"
        const val PREF_KEY_IS_DARK_THEME = "is_dark_theme"
        /** v2.3.5: editExistingExpense default meaning "leave categoryRef as stored". */
        const val KEEP_CATEGORY_REF = "\u0000keep"

        private fun urlEnc(raw: String): String = URLEncoder.encode(raw, Charsets.UTF_8.name())
        private fun urlDec(encoded: String): String = URLDecoder.decode(encoded, Charsets.UTF_8.name())

        /**
         * Extracts the `SM2_<base64url>` compressed token from a raw token string, a
         * `splitmate://trip-sync?payload=SM2_...` deep-link URI, or a full multi-line WhatsApp share message.
         */
        fun extractSyncTokenFromRawInput(rawInput: String): String? {
            if (rawInput.isBlank()) return null
            val match = Regex("""SM2_[A-Za-z0-9_-]+""").find(rawInput)
            return match?.value?.trim()?.takeIf { it.length > 10 }
        }

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
            fun formatShareAmount(cents: Long): String {
                return if (currencySymbol == "₹") {
                    formatIndianRupeesFromCents(cents)
                } else {
                    "$currencySymbol${String.format(Locale.US, "%.2f", cents / 100.0)}"
                }
            }
            val perPersonHeadlineShare = formatShareAmount(perPersonAvgCents)

            val headerLabel = if (hasExplicitSplits && splittingCount < groupMembers.size) {
                "$headerPrefix ($splittingCount of ${groupMembers.size} members splitting)"
            } else {
                "$headerPrefix ($splittingCount members)"
            }

            val fallbackAllocationsMap = if (!hasExplicitSplits) {
                val currentUser = groupMembers.find { it.isCurrentUser }
                SplitMateMathEngine.splitEquallyZeroDrift(
                    totalCents = expense.totalAmountCents,
                    members = groupMembers.map { it.memberId to it.name },
                    payerId = expense.payerId,
                    currentUserId = currentUser?.memberId
                ).associateBy { it.memberId }
            } else {
                emptyMap()
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
                            formatShareAmount(owed)
                        } else {
                            "${currencySymbol}0.00 (Excluded)"
                        }
                    )
                } else {
                    val alloc = fallbackAllocationsMap[mbr.memberId]
                    val owed = alloc?.finalCents ?: perPersonAvgCents
                    MemberSplitBreakdownRow(
                        memberId = mbr.memberId,
                        displayName = displayName,
                        isCurrentUser = mbr.isCurrentUser,
                        isIncludedInSplit = true,
                        owedCents = owed,
                        plusOneCent = alloc?.plusOneCent == true,
                        formattedShare = formatShareAmount(owed)
                    )
                }
            }

            // v2.3.3: UI-only split nature (PERSONAL / ON_BEHALF / SHARED), derived from the same
            // non-zero shares shown above. Cents, rows and balances are untouched.
            val participantShareCents: Map<String, Long> = if (hasExplicitSplits) {
                expenseSplits.associate { it.memberId to it.finalOwedCents }
            } else {
                rows.filter { it.isIncludedInSplit }.associate { it.memberId to it.owedCents }
            }
            val splitNature = com.splitmate.app.ExpenseSplitClassifier.classify(expense.payerId, participantShareCents)
            val soleParticipantId = if (splitNature == com.splitmate.app.ExpenseSplitNature.SHARED) {
                null
            } else {
                com.splitmate.app.ExpenseSplitClassifier.soleParticipantId(participantShareCents)
            }
            val soleMember = soleParticipantId?.let { id -> groupMembers.find { it.memberId == id } }
            val soleShareCents = soleParticipantId?.let { participantShareCents[it] } ?: 0L
            val resolvedHeaderLabel = when (splitNature) {
                com.splitmate.app.ExpenseSplitNature.PERSONAL -> com.splitmate.app.ExpenseSplitCopy.PERSONAL_EXPENSE_NOT_SPLIT
                com.splitmate.app.ExpenseSplitNature.ON_BEHALF -> {
                    val who = com.splitmate.app.ExpenseSplitCopy.beneficiaryLabel(soleMember?.name, soleMember?.isCurrentUser == true)
                    "$headerPrefix (paid in full for $who)"
                }
                com.splitmate.app.ExpenseSplitNature.SHARED -> headerLabel
            }

            return ExpenseSplitBreakdownSummary(
                totalMembersInGroup = groupMembers.size,
                splittingMembersCount = splittingCount,
                perPersonHeadlineShare = perPersonHeadlineShare,
                headerLabel = resolvedHeaderLabel,
                rows = rows,
                splitNature = splitNature,
                soleParticipantMemberId = soleParticipantId,
                soleParticipantName = soleMember?.name,
                soleParticipantIsCurrentUser = soleMember?.isCurrentUser == true,
                soleParticipantShare = if (soleParticipantId != null) formatShareAmount(soleShareCents) else ""
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
