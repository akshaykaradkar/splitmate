package com.splitmate.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

data class CloudUserProfileRecord(
    val phone10: String,
    val name: String,
    val handle: String,
    val upiVpa: String,
    val avatarStyle: String,
    val avatarColorPreset: String,
    val pinHash: String,
    val updatedAtEpochMs: Long,
    val avatarSeed: String = ""
)

data class CloudPhoneGroupIndexEntry(
    val groupId: String,
    val groupName: String,
    val inviterName: String,
    val inviterPhone: String,
    val inviteStatus: String,
    val updatedAtEpochMs: Long
)

data class CloudGroupLedgerDocument(
    val group: ExpenseGroupEntity,
    val members: List<GroupMemberEntity>,
    val expenses: List<ExpenseEntity>,
    val splits: List<ExpenseSplitEntity>,
    val settlements: List<SettlementEntity>,
    val deletedExpenseIds: Map<String, Long>,
    val flightVaultByPnr: Map<String, String>,
    val trainSnapshotByPnr: Map<String, String>,
    val updatedAtEpochMs: Long,
    val memberPresenceByPhone: Map<String, Long> = emptyMap()
)

data class CloudRestoreSummary(
    val restoredJoinedGroupsCount: Int,
    val discoveredPendingInvitesCount: Int,
    val memberPresenceByPhone: Map<String, Long> = emptyMap()
)

object CloudGroupSyncRepository {

    const val ONLINE_PRESENCE_TTL_MS: Long = 90_000L

    fun isPhoneOnlineNow(
        phone10: String,
        presenceMap: Map<String, Long>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Boolean {
        val norm = PhoneIdentityValidator.normalizeIndianPhone10(phone10)
        if (norm.length != 10) return false
        val lastSeen = presenceMap[norm] ?: return false
        return lastSeen > 0L && (nowEpochMs - lastSeen) <= ONLINE_PRESENCE_TTL_MS
    }

    private fun sanitizeTopicKey(key: String): String {
        return key.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    // =========================================================================
    // ZERO-DEPENDENCY JSON SERIALIZATION (`org.json.JSONObject` / `JSONArray`)
    // =========================================================================

    fun encodeUserProfileRecord(record: CloudUserProfileRecord): String {
        return JSONObject().apply {
            put("phone10", record.phone10)
            put("name", record.name)
            put("handle", record.handle)
            put("upiVpa", record.upiVpa)
            put("avatarStyle", record.avatarStyle)
            put("avatarColorPreset", record.avatarColorPreset)
            put("pinHash", record.pinHash)
            put("updatedAtEpochMs", record.updatedAtEpochMs)
            put("avatarSeed", record.avatarSeed)
        }.toString()
    }

    fun decodeUserProfileRecord(jsonStr: String): CloudUserProfileRecord? {
        return try {
            val obj = JSONObject(jsonStr)
            val phone10 = obj.optString("phone10", "")
            if (phone10.isBlank()) return null
            CloudUserProfileRecord(
                phone10 = phone10,
                name = obj.optString("name", ""),
                handle = obj.optString("handle", ""),
                upiVpa = obj.optString("upiVpa", ""),
                avatarStyle = obj.optString("avatarStyle", "open-peeps"),
                avatarColorPreset = obj.optString("avatarColorPreset", "Buckwheat"),
                pinHash = obj.optString("pinHash", ""),
                updatedAtEpochMs = obj.optLong("updatedAtEpochMs", 0L),
                avatarSeed = obj.optString("avatarSeed", "")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun encodePhoneIndexEntry(entry: CloudPhoneGroupIndexEntry): String {
        return JSONObject().apply {
            put("groupId", entry.groupId)
            put("groupName", entry.groupName)
            put("inviterName", entry.inviterName)
            put("inviterPhone", entry.inviterPhone)
            put("inviteStatus", entry.inviteStatus)
            put("updatedAtEpochMs", entry.updatedAtEpochMs)
        }.toString()
    }

    fun decodePhoneIndexEntry(jsonStr: String): CloudPhoneGroupIndexEntry? {
        return try {
            val obj = JSONObject(jsonStr)
            val groupId = obj.optString("groupId", "")
            if (groupId.isBlank()) return null
            CloudPhoneGroupIndexEntry(
                groupId = groupId,
                groupName = obj.optString("groupName", ""),
                inviterName = obj.optString("inviterName", ""),
                inviterPhone = obj.optString("inviterPhone", ""),
                inviteStatus = obj.optString("inviteStatus", "PENDING"),
                updatedAtEpochMs = obj.optLong("updatedAtEpochMs", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun encodeGroupLedgerDocument(doc: CloudGroupLedgerDocument): String {
        val root = JSONObject()
        root.put("updatedAtEpochMs", doc.updatedAtEpochMs)
        root.put("group", JSONObject().apply {
            put("groupId", doc.group.groupId)
            put("name", doc.group.name)
            put("currencyCode", doc.group.currencyCode)
            put("iconName", doc.group.iconName)
            put("isDemoSeed", doc.group.isDemoSeed)
            put("createdAt", doc.group.createdAt)
        })

        val membersArr = JSONArray()
        doc.members.forEach { m ->
            membersArr.put(JSONObject().apply {
                put("memberId", m.memberId)
                put("groupId", m.groupId)
                put("name", m.name)
                put("avatarSeed", m.avatarSeed)
                put("isCurrentUser", m.isCurrentUser)
                put("upiId", m.upiId)
                put("userPhone", m.userPhone)
                put("inviteStatus", m.inviteStatus)
            })
        }
        root.put("members", membersArr)

        val expensesArr = JSONArray()
        doc.expenses.forEach { e ->
            expensesArr.put(JSONObject().apply {
                put("expenseId", e.expenseId)
                put("groupId", e.groupId)
                put("title", e.title)
                put("payerId", e.payerId)
                put("baseSubtotalCents", e.baseSubtotalCents)
                put("taxCents", e.taxCents)
                put("tipCents", e.tipCents)
                put("totalAmountCents", e.totalAmountCents)
                put("lockedMultiplier", e.lockedMultiplier)
                put("unassignedBaseCents", e.unassignedBaseCents)
                put("currencyCode", e.currencyCode)
                put("lockedExchangeRate", e.lockedExchangeRate)
                put("expenseCategory", e.expenseCategory)
                put("travelPnr", e.travelPnr)
                put("providerName", e.providerName)
                if (e.scheduledAtEpochMs != null) {
                    put("scheduledAtEpochMs", e.scheduledAtEpochMs)
                }
                put("syncStatus", e.syncStatus)
                put("createdAt", e.createdAt)
            })
        }
        root.put("expenses", expensesArr)

        val splitsArr = JSONArray()
        doc.splits.forEach { s ->
            splitsArr.put(JSONObject().apply {
                put("splitId", s.splitId)
                put("expenseId", s.expenseId)
                put("memberId", s.memberId)
                put("baseClaimedCents", s.baseClaimedCents)
                put("finalOwedCents", s.finalOwedCents)
                put("plusOneCent", s.plusOneCent)
            })
        }
        root.put("splits", splitsArr)

        val settlementsArr = JSONArray()
        doc.settlements.forEach { st ->
            settlementsArr.put(JSONObject().apply {
                put("settlementId", st.settlementId)
                put("groupId", st.groupId)
                put("fromMemberId", st.fromMemberId)
                put("fromMemberName", st.fromMemberName)
                put("toMemberId", st.toMemberId)
                put("toMemberName", st.toMemberName)
                put("amountCents", st.amountCents)
                put("currencyCode", st.currencyCode)
                put("lockedExchangeRate", st.lockedExchangeRate)
                put("syncStatus", st.syncStatus)
                put("settledAt", st.settledAt)
            })
        }
        root.put("settlements", settlementsArr)

        val deletedObj = JSONObject()
        doc.deletedExpenseIds.forEach { (expId, ts) -> deletedObj.put(expId, ts) }
        root.put("deletedExpenseIds", deletedObj)

        val flightObj = JSONObject()
        doc.flightVaultByPnr.forEach { (pnr, json) -> flightObj.put(pnr, json) }
        root.put("flightVaultByPnr", flightObj)

        val trainObj = JSONObject()
        doc.trainSnapshotByPnr.forEach { (pnr, json) -> trainObj.put(pnr, json) }
        root.put("trainSnapshotByPnr", trainObj)

        val presenceObj = JSONObject()
        doc.memberPresenceByPhone.forEach { (phone10, ts) -> presenceObj.put(phone10, ts) }
        root.put("memberPresenceByPhone", presenceObj)

        return root.toString()
    }

    fun decodeGroupLedgerDocument(jsonStr: String): CloudGroupLedgerDocument? {
        return try {
            val root = JSONObject(jsonStr)
            val grpObj = root.optJSONObject("group") ?: return null
            val groupId = grpObj.optString("groupId", "")
            if (groupId.isBlank()) return null

            val group = ExpenseGroupEntity(
                groupId = groupId,
                name = grpObj.optString("name", "Shared Group"),
                currencyCode = grpObj.optString("currencyCode", "INR"),
                iconName = grpObj.optString("iconName", "Flight"),
                isDemoSeed = grpObj.optBoolean("isDemoSeed", false),
                createdAt = grpObj.optLong("createdAt", System.currentTimeMillis())
            )

            val members = mutableListOf<GroupMemberEntity>()
            val membersArr = root.optJSONArray("members") ?: JSONArray()
            for (i in 0 until membersArr.length()) {
                val m = membersArr.optJSONObject(i) ?: continue
                val rawUpi = m.optString("upiId", "")
                val rawPhone = m.optString("userPhone", "")
                val extractedPhone = PhoneIdentityValidator.extractMemberPhone10(rawPhone, rawUpi)
                members.add(
                    GroupMemberEntity(
                        memberId = m.optString("memberId", ""),
                        groupId = m.optString("groupId", groupId),
                        name = m.optString("name", ""),
                        avatarSeed = m.optString("avatarSeed", ""),
                        isCurrentUser = m.optBoolean("isCurrentUser", false),
                        upiId = rawUpi,
                        userPhone = extractedPhone.ifBlank { rawPhone },
                        inviteStatus = m.optString("inviteStatus", "JOINED")
                    )
                )
            }

            val expenses = mutableListOf<ExpenseEntity>()
            val expensesArr = root.optJSONArray("expenses") ?: JSONArray()
            for (i in 0 until expensesArr.length()) {
                val e = expensesArr.optJSONObject(i) ?: continue
                val scheduledAt = if (e.has("scheduledAtEpochMs") && !e.isNull("scheduledAtEpochMs")) {
                    e.optLong("scheduledAtEpochMs")
                } else {
                    null
                }
                expenses.add(
                    ExpenseEntity(
                        expenseId = e.optString("expenseId", ""),
                        groupId = e.optString("groupId", groupId),
                        title = e.optString("title", ""),
                        payerId = e.optString("payerId", ""),
                        baseSubtotalCents = e.optLong("baseSubtotalCents", 0L),
                        taxCents = e.optLong("taxCents", 0L),
                        tipCents = e.optLong("tipCents", 0L),
                        totalAmountCents = e.optLong("totalAmountCents", 0L),
                        lockedMultiplier = e.optDouble("lockedMultiplier", 1.0),
                        unassignedBaseCents = e.optLong("unassignedBaseCents", 0L),
                        currencyCode = e.optString("currencyCode", "INR"),
                        lockedExchangeRate = e.optDouble("lockedExchangeRate", 1.0),
                        expenseCategory = e.optString("expenseCategory", "OTHER"),
                        travelPnr = e.optString("travelPnr", ""),
                        providerName = e.optString("providerName", ""),
                        scheduledAtEpochMs = scheduledAt,
                        syncStatus = e.optString("syncStatus", "SYNCED"),
                        createdAt = e.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val splits = mutableListOf<ExpenseSplitEntity>()
            val splitsArr = root.optJSONArray("splits") ?: JSONArray()
            for (i in 0 until splitsArr.length()) {
                val s = splitsArr.optJSONObject(i) ?: continue
                splits.add(
                    ExpenseSplitEntity(
                        splitId = s.optString("splitId", ""),
                        expenseId = s.optString("expenseId", ""),
                        memberId = s.optString("memberId", ""),
                        baseClaimedCents = s.optLong("baseClaimedCents", 0L),
                        finalOwedCents = s.optLong("finalOwedCents", 0L),
                        plusOneCent = s.optBoolean("plusOneCent", false)
                    )
                )
            }

            val settlements = mutableListOf<SettlementEntity>()
            val settlementsArr = root.optJSONArray("settlements") ?: JSONArray()
            for (i in 0 until settlementsArr.length()) {
                val st = settlementsArr.optJSONObject(i) ?: continue
                settlements.add(
                    SettlementEntity(
                        settlementId = st.optString("settlementId", ""),
                        groupId = st.optString("groupId", groupId),
                        fromMemberId = st.optString("fromMemberId", ""),
                        fromMemberName = st.optString("fromMemberName", ""),
                        toMemberId = st.optString("toMemberId", ""),
                        toMemberName = st.optString("toMemberName", ""),
                        amountCents = st.optLong("amountCents", 0L),
                        currencyCode = st.optString("currencyCode", "INR"),
                        lockedExchangeRate = st.optDouble("lockedExchangeRate", 1.0),
                        syncStatus = st.optString("syncStatus", "SYNCED"),
                        settledAt = st.optLong("settledAt", System.currentTimeMillis())
                    )
                )
            }

            val deletedExpenseIds = mutableMapOf<String, Long>()
            val deletedObj = root.optJSONObject("deletedExpenseIds")
            if (deletedObj != null) {
                val keys = deletedObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    deletedExpenseIds[k] = deletedObj.optLong(k, 0L)
                }
            }

            val flightVaultByPnr = mutableMapOf<String, String>()
            val flightObj = root.optJSONObject("flightVaultByPnr")
            if (flightObj != null) {
                val keys = flightObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    flightVaultByPnr[k] = flightObj.optString(k, "")
                }
            }

            val trainSnapshotByPnr = mutableMapOf<String, String>()
            val trainObj = root.optJSONObject("trainSnapshotByPnr")
            if (trainObj != null) {
                val keys = trainObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    trainSnapshotByPnr[k] = trainObj.optString(k, "")
                }
            }

            val memberPresenceByPhone = mutableMapOf<String, Long>()
            val presenceObj = root.optJSONObject("memberPresenceByPhone")
            if (presenceObj != null) {
                val keys = presenceObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val normP = PhoneIdentityValidator.normalizeIndianPhone10(k)
                    if (normP.length == 10) {
                        memberPresenceByPhone[normP] = presenceObj.optLong(k, 0L)
                    }
                }
            }

            CloudGroupLedgerDocument(
                group = group,
                members = members,
                expenses = expenses,
                splits = splits,
                settlements = settlements,
                deletedExpenseIds = deletedExpenseIds,
                flightVaultByPnr = flightVaultByPnr,
                trainSnapshotByPnr = trainSnapshotByPnr,
                updatedAtEpochMs = root.optLong("updatedAtEpochMs", System.currentTimeMillis()),
                memberPresenceByPhone = memberPresenceByPhone
            )
        } catch (_: Exception) {
            null
        }
    }

    // =========================================================================
    // CLOUD NTFY JSON CHANNEL TRANSPORT (ZERO USER SETUP + GZIP + ATTACHMENT SUPPORT)
    // =========================================================================

    private const val GZIP_PREFIX = "SMGZ:"

    internal fun compressPayloadIfNeeded(rawJson: String): String {
        val rawBytes = rawJson.toByteArray(Charsets.UTF_8)
        if (rawBytes.size <= 3000) return rawJson
        return try {
            val bos = java.io.ByteArrayOutputStream()
            java.util.zip.GZIPOutputStream(bos).use { gzip ->
                gzip.write(rawBytes)
            }
            GZIP_PREFIX + java.util.Base64.getEncoder().encodeToString(bos.toByteArray())
        } catch (_: Exception) {
            rawJson
        }
    }

    internal fun decompressPayloadIfNeeded(payload: String): String {
        val trimmed = payload.trim()
        if (!trimmed.startsWith(GZIP_PREFIX)) return trimmed
        return try {
            val compressedBytes = java.util.Base64.getDecoder().decode(trimmed.removePrefix(GZIP_PREFIX))
            java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(compressedBytes)).use { gis ->
                InputStreamReader(gis, Charsets.UTF_8).readText()
            }
        } catch (_: Exception) {
            trimmed
        }
    }

    private fun downloadAttachmentText(attachmentUrl: String): String? {
        return try {
            val conn = (URL(attachmentUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }
            if (conn.responseCode == 200) {
                InputStreamReader(conn.inputStream, Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractNtfyEventPayload(eventJson: JSONObject): String? {
        val attachmentUrl = eventJson.optJSONObject("attachment")?.optString("url", "").orEmpty()
        val rawContent = if (attachmentUrl.isNotBlank()) {
            downloadAttachmentText(attachmentUrl) ?: eventJson.optString("message", "")
        } else {
            eventJson.optString("message", "")
        }
        if (rawContent.isBlank()) return null
        return decompressPayloadIfNeeded(rawContent).takeIf { it.isNotBlank() }
    }

    private suspend fun fetchNtfySnapshot(topic: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://ntfy.sh/$topic/json?poll=1&since=all")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                var bestEventJson: JSONObject? = null
                var bestTime = 0L

                val reader = InputStreamReader(conn.inputStream, Charsets.UTF_8)
                val lines = reader.readLines()
                for (line in lines) {
                    if (line.isBlank()) continue
                    try {
                        val json = JSONObject(line)
                        if (json.optString("event") == "message") {
                            val time = json.optLong("time", 0L)
                            if (time >= bestTime) {
                                bestTime = time
                                bestEventJson = json
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
                return@withContext bestEventJson?.let { extractNtfyEventPayload(it) }
            }
        } catch (_: Exception) {
        }
        return@withContext null
    }

    private suspend fun fetchAllNtfyMessages(topic: String): List<String> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<String>()
        try {
            val url = URL("https://ntfy.sh/$topic/json?poll=1&since=all")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                val reader = InputStreamReader(conn.inputStream, Charsets.UTF_8)
                val lines = reader.readLines()
                for (line in lines) {
                    if (line.isBlank()) continue
                    try {
                        val json = JSONObject(line)
                        if (json.optString("event") == "message") {
                            val extracted = extractNtfyEventPayload(json)
                            if (!extracted.isNullOrBlank()) {
                                messages.add(extracted)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (_: Exception) {
        }
        return@withContext messages
    }

    private suspend fun pushNtfySnapshot(topic: String, jsonPayload: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val wirePayload = compressPayloadIfNeeded(jsonPayload)
            val url = URL("https://ntfy.sh/$topic")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Title", "SplitMateSync")
            conn.setRequestProperty("Cache", "yes")
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            conn.outputStream.use { os ->
                os.write(wirePayload.toByteArray(Charsets.UTF_8))
            }

            return@withContext conn.responseCode == 200
        } catch (_: Exception) {
            return@withContext false
        }
    }

    // =========================================================================
    // ATOMIC READ-BEFORE-WRITE UNION MERGE & CRDT TOMBSTONE ENGINE
    // =========================================================================

    fun mergeGroupLedgerDocuments(
        localDoc: CloudGroupLedgerDocument?,
        remoteDoc: CloudGroupLedgerDocument?,
        localUserPhone10: String,
        localUserAvatarSeed: String = "",
        localPresenceEpochMs: Long = System.currentTimeMillis()
    ): CloudGroupLedgerDocument {
        val normLocalPhone = PhoneIdentityValidator.normalizeIndianPhone10(localUserPhone10)
        if (localDoc == null && remoteDoc == null) throw IllegalArgumentException("Both docs null")
        if (localDoc == null) {
            return adjustMembersForLocalUser(
                doc = remoteDoc!!,
                localUserPhone10 = normLocalPhone,
                localUserAvatarSeed = localUserAvatarSeed,
                isRemoteOnly = true,
                localPresenceEpochMs = localPresenceEpochMs
            )
        }
        if (remoteDoc == null) {
            return adjustMembersForLocalUser(
                doc = localDoc,
                localUserPhone10 = normLocalPhone,
                localUserAvatarSeed = localUserAvatarSeed,
                isRemoteOnly = false,
                localPresenceEpochMs = localPresenceEpochMs
            )
        }

        val mergedDeletedExpenseIds = mutableMapOf<String, Long>()
        (localDoc.deletedExpenseIds.keys + remoteDoc.deletedExpenseIds.keys).forEach { id ->
            mergedDeletedExpenseIds[id] = max(
                localDoc.deletedExpenseIds[id] ?: 0L,
                remoteDoc.deletedExpenseIds[id] ?: 0L
            )
        }

        val allExpenses = (localDoc.expenses + remoteDoc.expenses)
            .filter { it.expenseId !in mergedDeletedExpenseIds }
            .groupBy { it.expenseId }
            .map { (_, group) ->
                val fromLocal = group.find { localDoc.expenses.contains(it) }
                val fromRemote = group.find { remoteDoc.expenses.contains(it) }
                if (fromLocal != null && fromRemote != null) {
                    if (localDoc.updatedAtEpochMs >= remoteDoc.updatedAtEpochMs) fromLocal else fromRemote
                } else {
                    fromLocal ?: fromRemote!!
                }
            }

        val survivingExpenseIds = allExpenses.map { it.expenseId }.toSet()
        val allSplits = (localDoc.splits + remoteDoc.splits)
            .filter { it.expenseId in survivingExpenseIds }
            .groupBy { it.splitId }
            .map { (_, group) ->
                val fromLocal = group.find { localDoc.splits.contains(it) }
                val fromRemote = group.find { remoteDoc.splits.contains(it) }
                if (fromLocal != null && fromRemote != null) {
                    if (localDoc.updatedAtEpochMs >= remoteDoc.updatedAtEpochMs) fromLocal else fromRemote
                } else {
                    fromLocal ?: fromRemote!!
                }
            }

        val allSettlements = (localDoc.settlements + remoteDoc.settlements)
            .groupBy { it.settlementId }
            .map { (_, group) ->
                val fromLocal = group.find { localDoc.settlements.contains(it) }
                val fromRemote = group.find { remoteDoc.settlements.contains(it) }
                if (fromLocal != null && fromRemote != null) {
                    if (localDoc.updatedAtEpochMs >= remoteDoc.updatedAtEpochMs) fromLocal else fromRemote
                } else {
                    fromLocal ?: fromRemote!!
                }
            }

        val localPreferredSeed = localUserAvatarSeed.takeIf { it.isNotBlank() }
            ?: localDoc.members.firstOrNull { m ->
                val normPhone = PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId)
                (normLocalPhone.length == 10 && normPhone == normLocalPhone) || m.isCurrentUser
            }?.avatarSeed?.takeIf { it.contains("|") }.orEmpty()

        val memberMap = linkedMapOf<String, GroupMemberEntity>()
        val allMembersWithDocTime = localDoc.members.map { it to localDoc.updatedAtEpochMs } +
            remoteDoc.members.map { it to remoteDoc.updatedAtEpochMs }
        for ((member, docTime) in allMembersWithDocTime) {
            val normPhone = PhoneIdentityValidator.extractMemberPhone10(member.userPhone, member.upiId)
            val normalizedMember = if (normPhone.isNotBlank() && member.userPhone != normPhone) {
                member.copy(userPhone = normPhone)
            } else {
                member
            }
            val key = if (normPhone.isNotBlank()) normPhone else normalizedMember.memberId

            val existing = memberMap[key]
            if (existing == null) {
                memberMap[key] = normalizedMember
            } else {
                val existingDocTime = if (localDoc.members.any { it.memberId == existing.memberId }) {
                    localDoc.updatedAtEpochMs
                } else {
                    remoteDoc.updatedAtEpochMs
                }
                if (docTime >= existingDocTime) {
                    memberMap[key] = normalizedMember.copy(
                        memberId = existing.memberId,
                        userPhone = normPhone.ifBlank { existing.userPhone }
                    )
                }
            }
        }
        val localMeMemberIds = localDoc.members.filter { it.isCurrentUser }.map { it.memberId }.toSet()
        val hasPhoneMatchedLocalUser = normLocalPhone.length == 10 && memberMap.values.any {
            PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == normLocalPhone
        }
        val mergedMembers = memberMap.values.map { m ->
            val normPhone = PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId)
            val isMe = if (hasPhoneMatchedLocalUser) {
                normPhone == normLocalPhone
            } else {
                (normPhone == normLocalPhone && normLocalPhone.length == 10) ||
                    (normPhone.isEmpty() && m.memberId in localMeMemberIds)
            }
            m.copy(
                userPhone = normPhone.ifBlank { m.userPhone },
                isCurrentUser = isMe,
                avatarSeed = if (isMe && localPreferredSeed.isNotBlank()) localPreferredSeed else m.avatarSeed
            )
        }

        val mergedFlights = localDoc.flightVaultByPnr.toMutableMap()
        remoteDoc.flightVaultByPnr.forEach { (k, v) -> mergedFlights[k] = v }
        val mergedTrains = localDoc.trainSnapshotByPnr.toMutableMap()
        remoteDoc.trainSnapshotByPnr.forEach { (k, v) -> mergedTrains[k] = v }

        val mergedPresence = mutableMapOf<String, Long>()
        (localDoc.memberPresenceByPhone.keys + remoteDoc.memberPresenceByPhone.keys).forEach { p ->
            val normP = PhoneIdentityValidator.normalizeIndianPhone10(p)
            if (normP.length == 10) {
                mergedPresence[normP] = max(
                    localDoc.memberPresenceByPhone[p] ?: 0L,
                    remoteDoc.memberPresenceByPhone[p] ?: 0L
                )
            }
        }
        if (normLocalPhone.length == 10) {
            mergedPresence[normLocalPhone] = max(mergedPresence[normLocalPhone] ?: 0L, localPresenceEpochMs)
        }

        return CloudGroupLedgerDocument(
            group = if (localDoc.updatedAtEpochMs >= remoteDoc.updatedAtEpochMs) localDoc.group else remoteDoc.group,
            members = mergedMembers,
            expenses = allExpenses,
            splits = allSplits,
            settlements = allSettlements,
            deletedExpenseIds = mergedDeletedExpenseIds,
            flightVaultByPnr = mergedFlights,
            trainSnapshotByPnr = mergedTrains,
            updatedAtEpochMs = max(localDoc.updatedAtEpochMs, remoteDoc.updatedAtEpochMs),
            memberPresenceByPhone = mergedPresence
        )
    }

    private fun adjustMembersForLocalUser(
        doc: CloudGroupLedgerDocument,
        localUserPhone10: String,
        localUserAvatarSeed: String = "",
        isRemoteOnly: Boolean = false,
        localPresenceEpochMs: Long = System.currentTimeMillis()
    ): CloudGroupLedgerDocument {
        val normLocalPhone = PhoneIdentityValidator.normalizeIndianPhone10(localUserPhone10)
        val updatedPresence = doc.memberPresenceByPhone.toMutableMap()
        if (normLocalPhone.length == 10) {
            updatedPresence[normLocalPhone] = max(updatedPresence[normLocalPhone] ?: 0L, localPresenceEpochMs)
        }
        if (normLocalPhone.length != 10 && localUserAvatarSeed.isBlank()) {
            return doc.copy(memberPresenceByPhone = updatedPresence)
        }
        val hasPhoneMatch = normLocalPhone.length == 10 && doc.members.any {
            PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == normLocalPhone
        }
        val adjustedMembers = doc.members.map { m ->
            val normPhone = PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId)
            val isMe = when {
                hasPhoneMatch -> normPhone == normLocalPhone
                !isRemoteOnly -> normPhone.isEmpty() && m.isCurrentUser
                else -> false
            }
            m.copy(
                userPhone = normPhone.ifBlank { m.userPhone },
                isCurrentUser = isMe,
                avatarSeed = if (isMe && localUserAvatarSeed.isNotBlank()) localUserAvatarSeed else m.avatarSeed
            )
        }
        return doc.copy(
            members = adjustedMembers,
            memberPresenceByPhone = updatedPresence
        )
    }

    suspend fun pushUserProfileToCloud(
        profile: UserProfileEntity,
        avatarStyle: String = "open-peeps",
        avatarColorPreset: String = "Buckwheat"
    ): Boolean {
        val phone10 = PhoneIdentityValidator.normalizeIndianPhone10(profile.userPhone)
        if (phone10.isEmpty()) return false
        val topic = "splitmate_v2_u_$phone10"
        val record = CloudUserProfileRecord(
            phone10 = phone10,
            name = profile.name,
            handle = profile.name.lowercase().replace(" ", "_"),
            upiVpa = profile.upiId,
            avatarStyle = avatarStyle,
            avatarColorPreset = avatarColorPreset,
            pinHash = profile.pinHash,
            updatedAtEpochMs = System.currentTimeMillis(),
            avatarSeed = profile.avatarSeed
        )
        return pushNtfySnapshot(topic, encodeUserProfileRecord(record))
    }

    suspend fun fetchUserProfileFromCloud(phone10: String): CloudUserProfileRecord? {
        val normPhone = PhoneIdentityValidator.normalizeIndianPhone10(phone10)
        if (normPhone.isEmpty()) return null
        val topic = "splitmate_v2_u_$normPhone"
        val json = fetchNtfySnapshot(topic) ?: return null
        return decodeUserProfileRecord(json)
    }

    suspend fun pushCrossDeviceOtpChallengeToVerifiedPrimary(
        phone10: String,
        encryptedChallengeJson: String
    ): Boolean {
        val normPhone = PhoneIdentityValidator.normalizeIndianPhone10(phone10)
        if (normPhone.isEmpty() || encryptedChallengeJson.isBlank()) return false
        val existingProfile = fetchUserProfileFromCloud(normPhone) ?: return false
        if (existingProfile.pinHash.isBlank()) return false
        val topic = "splitmate_v2_otp_push_$normPhone"
        return pushNtfySnapshot(topic, encryptedChallengeJson)
    }

    suspend fun pushPhoneIndexEntry(entry: CloudPhoneGroupIndexEntry, targetPhone10: String): Boolean {
        val normPhone = PhoneIdentityValidator.normalizeIndianPhone10(targetPhone10)
        if (normPhone.isEmpty()) return false
        val idxTopic = "splitmate_v2_idx_$normPhone"
        return pushNtfySnapshot(idxTopic, encodePhoneIndexEntry(entry))
    }

    suspend fun syncGroupWithCloud(
        context: Context?,
        dao: SplitMateDao,
        groupId: String,
        localUserPhone10: String,
        localUserName: String,
        additionalTombstones: Map<String, Long> = emptyMap()
    ): CloudGroupLedgerDocument? {
        val normLocalPhone = PhoneIdentityValidator.normalizeIndianPhone10(localUserPhone10)
            .ifBlank {
                PhoneIdentityValidator.normalizeIndianPhone10(dao.getUserProfile()?.userPhone.orEmpty())
            }
        val topic = "splitmate_v2_grp_${sanitizeTopicKey(groupId)}"
        val remoteJson = fetchNtfySnapshot(topic)
        val remoteDoc = if (remoteJson != null) decodeGroupLedgerDocument(remoteJson) else null

        val localProfile = dao.getUserProfile()
        val localAvatarSeed = localProfile?.avatarSeed?.takeIf { it.isNotBlank() }.orEmpty()

        val localGroup = dao.getGroupById(groupId)
        var localDoc: CloudGroupLedgerDocument? = null

        if (localGroup != null) {
            val members = dao.getMembersForGroup(groupId).map { m ->
                val extractedPhone = PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId)
                val finalPhone = if (m.isCurrentUser && extractedPhone.isBlank() && normLocalPhone.length == 10) {
                    normLocalPhone
                } else {
                    extractedPhone.ifBlank { m.userPhone }
                }
                if (finalPhone != m.userPhone) m.copy(userPhone = finalPhone) else m
            }
            val expenses = dao.getExpensesForGroup(groupId)
            val splits = dao.getSplitsForGroup(groupId)
            val settlements = dao.getSettlementsForGroup(groupId)

            val flights = context?.let { PnrNetworkRepository.exportAllFlightVaultJsonByPnr(it) } ?: emptyMap()
            val trains = context?.let { PnrNetworkRepository.exportAllTrainSnapshotJsonByPnr(it) } ?: emptyMap()

            val nowMs = System.currentTimeMillis()
            val initialPresence = if (normLocalPhone.length == 10) mapOf(normLocalPhone to nowMs) else emptyMap()

            localDoc = CloudGroupLedgerDocument(
                group = localGroup,
                members = members,
                expenses = expenses,
                splits = splits,
                settlements = settlements,
                deletedExpenseIds = additionalTombstones,
                flightVaultByPnr = flights,
                trainSnapshotByPnr = trains,
                updatedAtEpochMs = nowMs,
                memberPresenceByPhone = initialPresence
            )
        }

        if (localDoc == null && remoteDoc == null) return null

        val mergedDoc = mergeGroupLedgerDocuments(
            localDoc = localDoc,
            remoteDoc = remoteDoc,
            localUserPhone10 = normLocalPhone,
            localUserAvatarSeed = localAvatarSeed
        )

        // Delete any local expenses that were tombstoned in cloud
        mergedDoc.deletedExpenseIds.keys.forEach { deletedExpId ->
            dao.deleteSplitsForExpense(deletedExpId)
            dao.deleteExpense(deletedExpId)
        }

        // Write merged state to Room using @Upsert (never triggers ON DELETE CASCADE)
        dao.insertGroup(mergedDoc.group)
        dao.insertMembers(mergedDoc.members)
        mergedDoc.expenses.forEach { dao.insertExpense(it) }
        dao.insertExpenseSplits(mergedDoc.splits)
        mergedDoc.settlements.forEach { dao.insertSettlement(it) }

        if (context != null) {
            PnrNetworkRepository.importFlightVaultJsonByPnr(context, mergedDoc.flightVaultByPnr)
            PnrNetworkRepository.importTrainSnapshotJsonByPnr(context, mergedDoc.trainSnapshotByPnr)
        }

        val nowMs = System.currentTimeMillis()
        val updatedPresence = mergedDoc.memberPresenceByPhone.toMutableMap()
        if (normLocalPhone.length == 10) {
            updatedPresence[normLocalPhone] = nowMs
        }
        val mergedDocUpdated = mergedDoc.copy(
            updatedAtEpochMs = nowMs,
            memberPresenceByPhone = updatedPresence
        )
        pushNtfySnapshot(topic, encodeGroupLedgerDocument(mergedDocUpdated))

        val pushedPhones = HashSet<String>()
        mergedDocUpdated.members.forEach { m ->
            val p = PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId)
            if (p.length == 10 && pushedPhones.add(p)) {
                val idxEntry = CloudPhoneGroupIndexEntry(
                    groupId = mergedDocUpdated.group.groupId,
                    groupName = mergedDocUpdated.group.name,
                    inviterName = localUserName,
                    inviterPhone = normLocalPhone,
                    inviteStatus = m.inviteStatus,
                    updatedAtEpochMs = mergedDocUpdated.updatedAtEpochMs
                )
                pushPhoneIndexEntry(idxEntry, p)
            }
        }

        return mergedDocUpdated
    }

    suspend fun restoreAndSyncAllForVerifiedPhone(
        context: Context?,
        dao: SplitMateDao,
        phone10: String
    ): CloudRestoreSummary {
        val normPhone = PhoneIdentityValidator.normalizeIndianPhone10(phone10)
        if (normPhone.isEmpty()) return CloudRestoreSummary(0, 0)

        // Purge untouched demo seed groups (using structured isDemoSeed column)
        val allGroups = dao.getAllGroups()
        allGroups.filter { it.isDemoSeed }.forEach { g ->
            val exps = dao.getExpensesForGroup(g.groupId)
            // Demo seed groups have 0 user-added expenses or only default seeded expenses when untouched
            if (exps.all { it.syncStatus == "SYNCED" && it.travelPnr.isBlank() } && exps.size <= 3) {
                dao.deleteGroupById(g.groupId)
            }
        }

        val topic = "splitmate_v2_idx_$normPhone"
        val messages = fetchAllNtfyMessages(topic)
        val entries = messages.mapNotNull { decodePhoneIndexEntry(it) }

        val uniqueEntries = entries.groupBy { entry -> entry.groupId }
            .mapNotNull { (_, list) -> list.maxByOrNull { item -> item.updatedAtEpochMs } }

        var joined = 0
        var pending = 0
        val aggregatedPresence = mutableMapOf<String, Long>()
        aggregatedPresence[normPhone] = System.currentTimeMillis()

        val localProfile = dao.getUserProfile()
        val localName = localProfile?.name ?: "You"

        for (entry in uniqueEntries) {
            val syncedDoc = syncGroupWithCloud(context, dao, entry.groupId, normPhone, localName, emptyMap())
            syncedDoc?.memberPresenceByPhone?.forEach { (p, ts) ->
                val normP = PhoneIdentityValidator.normalizeIndianPhone10(p)
                if (normP.length == 10) {
                    aggregatedPresence[normP] = max(aggregatedPresence[normP] ?: 0L, ts)
                }
            }

            val members = dao.getMembersForGroup(entry.groupId)
            val myMember = members.find {
                PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == normPhone
            } ?: members.find { it.isCurrentUser }
            if (myMember != null) {
                when (myMember.inviteStatus) {
                    "JOINED" -> joined++
                    "PENDING" -> pending++
                }
            }
        }

        // Also push any existing local non-seed groups created by this user
        val remainingLocalGroups = dao.getAllGroups().filter { !it.isDemoSeed }
        for (localGroup in remainingLocalGroups) {
            if (uniqueEntries.none { it.groupId == localGroup.groupId }) {
                val syncedDoc = syncGroupWithCloud(context, dao, localGroup.groupId, normPhone, localName, emptyMap())
                syncedDoc?.memberPresenceByPhone?.forEach { (p, ts) ->
                    val normP = PhoneIdentityValidator.normalizeIndianPhone10(p)
                    if (normP.length == 10) {
                        aggregatedPresence[normP] = max(aggregatedPresence[normP] ?: 0L, ts)
                    }
                }
                joined++
            }
        }

        return CloudRestoreSummary(
            restoredJoinedGroupsCount = joined,
            discoveredPendingInvitesCount = pending,
            memberPresenceByPhone = aggregatedPresence
        )
    }
}
