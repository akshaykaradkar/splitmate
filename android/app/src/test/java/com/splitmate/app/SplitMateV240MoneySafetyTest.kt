package com.splitmate.app

import com.splitmate.app.data.CloudGroupLedgerDocument
import com.splitmate.app.data.CloudGroupSyncRepository
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseGroupEntity
import com.splitmate.app.data.ExpenseRevisionEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.ExpenseVersionSync
import com.splitmate.app.data.GroupMemberEntity
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * v2.4.0 money safety (RCA Bike Rentals): P1 per-expense versions, P2 quarantine, P4 history, P5 tests.
 */
@DisplayName("v2.4.0 money safety: per-expense versions, quarantine, edit history")
class SplitMateV240MoneySafetyTest {

    private val day = 24L * 60 * 60 * 1000
    private val t0 = 1_791_000_000_000L

    // --------------------------------------------------------------------------------------------
    // Fixtures
    // --------------------------------------------------------------------------------------------

    private fun group(id: String) = ExpenseGroupEntity(
        groupId = id, name = "Ratnagiri", currencyCode = "INR", iconName = "Flight", isDemoSeed = false, createdAt = t0
    )

    private val allMembers = mutableListOf<GroupMemberEntity>()

    private fun member(id: String, gid: String, name: String, phone: String, me: Boolean = false) = GroupMemberEntity(
        memberId = id, groupId = gid, name = name, avatarSeed = "$name|Neutral|open-peeps|Buckwheat",
        isCurrentUser = me, upiId = "$phone@upi", userPhone = phone, inviteStatus = "JOINED"
    ).also { allMembers += it }

    /** Same device-independent member key the sync uses (phone number). */
    private fun key(): (String) -> String = ExpenseVersioning.memberKeyOf(allMembers.toList())

    private fun expense(id: String, gid: String, payer: String, total: Long, rv: Long = 0L, by: String? = null, title: String = "Bike rentals") =
        ExpenseEntity(
            expenseId = id, groupId = gid, title = title, payerId = payer,
            baseSubtotalCents = total, taxCents = 0L, tipCents = 0L, totalAmountCents = total,
            lockedMultiplier = 1.0, unassignedBaseCents = 0L, currencyCode = "INR", lockedExchangeRate = 1.0,
            syncStatus = "SYNCED", createdAt = t0, rowVersion = rv, rowUpdatedBy = by
        )

    /** Same split ids for every edit (`<id>_sp_<n>`), exactly like the real edit screen. */
    private fun shares(exp: ExpenseEntity, members: List<GroupMemberEntity>): List<ExpenseSplitEntity> {
        val each = exp.totalAmountCents / members.size
        var left = exp.totalAmountCents - each * members.size
        return members.mapIndexed { i, m ->
            val extra = if (left > 0) 1L.also { left-- } else 0L
            ExpenseSplitEntity("${exp.expenseId}_sp_$i", exp.expenseId, m.memberId, each + extra, each + extra, false)
        }
    }

    private fun doc(gid: String, members: List<GroupMemberEntity>, exps: List<ExpenseEntity>, splits: List<ExpenseSplitEntity>, at: Long) =
        CloudGroupLedgerDocument(
            group = group(gid), members = members, expenses = exps, splits = splits, settlements = emptyList(),
            deletedExpenseIds = emptyMap(), flightVaultByPnr = emptyMap(), trainSnapshotByPnr = emptyMap(), updatedAtEpochMs = at
        )

    private fun historyOf(vararg pairs: Pair<ExpenseEntity, List<ExpenseSplitEntity>>) = ExpenseVersioning.History(
        knownHashesByExpense = pairs.groupBy({ it.first.expenseId }, { ExpenseVersioning.contentHash(it.first, it.second, key()) })
            .mapValues { it.value.toSet() },
        maxSeenVersion = pairs.maxOf { it.first.rowVersion },
        firstOwnVersionByExpense = pairs.groupBy({ it.first.expenseId }, { it.first.rowVersion }).mapValues { it.value.min() }
    )

    private fun merge(local: CloudGroupLedgerDocument, remote: CloudGroupLedgerDocument, h: ExpenseVersioning.History?) =
        CloudGroupSyncRepository.mergeGroupLedgerDocuments(local, remote, localUserPhone10 = "9000000001", versionHistory = h)

    private fun sixPeople(gid: String) = (1..6).map { member("m$it", gid, "P$it", "90000000%02d".format(it), it == 1) }

    // --------------------------------------------------------------------------------------------
    // P5.1 The RCA reproduction, plus the mirror case
    // --------------------------------------------------------------------------------------------

    @Test
    fun `RCA - an edit is never undone by a phone holding the older copy`() {
        val gid = "g_v240_rca"
        val people = sixPeople(gid)
        val old = expense("exp_bike", gid, "m2", 795_000L)
        val oldShares = shares(old, people) // 6 x 1,325
        val edited = expense("exp_bike", gid, "m2", 495_000L, rv = t0 + 10, by = "m2")
        val editedShares = shares(edited, people) // 6 x 825, SAME split ids

        // Gaurii's phone: old untouched copy, but its document is stamped newer (pending change elsewhere).
        val gauriiDoc = doc(gid, people, listOf(old), oldShares, at = t0 + 99_999)
        val siddheshDoc = doc(gid, people, listOf(edited), editedShares, at = t0 + 10)
        val knowsOld = historyOf(old to oldShares)

        for ((local, remote) in listOf(gauriiDoc to siddheshDoc, siddheshDoc to gauriiDoc)) {
            val merged = merge(local, remote, knowsOld)
            val exp = merged.expenses.single()
            val rows = merged.splits.filter { it.expenseId == exp.expenseId }
            assertEquals(495_000L, exp.totalAmountCents, "the edit must win")
            assertEquals(List(6) { 82_500L }, rows.map { it.finalOwedCents }, "shares must come from the same copy")
            assertTrue(ExpenseSplitIntegrity.isConsistent(exp, rows))
        }
    }

    @Test
    fun `mirror case - new total with old shares from an old app is quarantined, never repaired`() {
        val gid = "g_v240_mirror"
        val people = sixPeople(gid)
        val old = expense("exp_bike", gid, "m2", 795_000L)
        val oldShares = shares(old, people)
        val mixed = old.copy(totalAmountCents = 495_000L, baseSubtotalCents = 495_000L) // new total, old shares
        val local = doc(gid, people, listOf(old), oldShares, at = t0)
        val remote = doc(gid, people, listOf(mixed), oldShares, at = t0 + 5)

        val merged = merge(local, remote, ExpenseVersioning.History())
        val exp = merged.expenses.single()
        val rows = merged.splits.filter { it.expenseId == exp.expenseId }
        assertEquals(495_000L, exp.totalAmountCents, "no blind repair: the shares are not trusted over the total")
        assertFalse(ExpenseSplitIntegrity.isConsistent(exp, rows))

        val json = JSONObject(CloudGroupSyncRepository.encodeGroupLedgerDocument(merged))
        val wire = json.getJSONArray("expenses").getJSONObject(0)
        assertTrue(wire.optBoolean("q"), "an inconsistent expense still uploads, flagged q")
        assertEquals(1, json.getJSONArray("expenses").length(), "quarantine never blocks the upload")
    }

    // --------------------------------------------------------------------------------------------
    // P5.3 Old-app round trip; P5.4 clock ahead; concurrent edits
    // --------------------------------------------------------------------------------------------

    @Test
    fun `old app - a stale copy loses, a re-upload of the same content is harmless, a real old-app edit wins`() {
        val gid = "g_v240_oldapp"
        val people = sixPeople(gid).take(3)
        val v0 = expense("exp_cab", gid, "m1", 90_000L)
        val v0Shares = shares(v0, people)
        val v1 = expense("exp_cab", gid, "m1", 60_000L, rv = t0 + 100, by = "m1")
        val v1Shares = shares(v1, people)
        val history = historyOf(v0 to v0Shares, v1 to v1Shares)
        val mine = doc(gid, people, listOf(v1), v1Shares, at = t0)

        // Stale: old app uploads v0 content without a version, with a newer document time.
        val stale = merge(mine, doc(gid, people, listOf(v0), v0Shares, at = t0 + day), history)
        assertEquals(60_000L, stale.expenses.single().totalAmountCents)

        // Same content re-uploaded by the old app (version dropped): nothing changes.
        val same = merge(mine, doc(gid, people, listOf(v1.copy(rowVersion = 0L, rowUpdatedBy = null)), v1Shares, at = t0 + day), history)
        assertEquals(60_000L, same.expenses.single().totalAmountCents)
        assertEquals(v1.rowVersion, same.expenses.single().rowVersion)

        // Genuine old-app edit (content never seen): it wins and gets the next version.
        val oldAppEdit = v0.copy(totalAmountCents = 75_000L, baseSubtotalCents = 75_000L)
        val oldAppShares = shares(oldAppEdit, people)
        val adopted = merge(mine, doc(gid, people, listOf(oldAppEdit), oldAppShares, at = t0 - 1), history)
        val exp = adopted.expenses.single()
        assertEquals(75_000L, exp.totalAmountCents)
        assertEquals(v1.rowVersion + 1, exp.rowVersion)
        assertEquals(ExpenseVersioning.LEGACY_EDITOR, exp.rowUpdatedBy)
        assertEquals(75_000L, adopted.splits.filter { it.expenseId == exp.expenseId }.sumOf { it.finalOwedCents })
    }

    @Test
    fun `a phone clock 3 days ahead cannot win forever`() {
        val now = t0
        val skewed = ExpenseVersioning.nextVersion(now + 3 * day, 0L)
        val next = ExpenseVersioning.nextVersion(now + 60_000, maxSeenVersion = skewed)
        assertTrue(next > skewed, "the next edit anywhere goes past the skewed version")

        val gid = "g_v240_clock"
        val people = sixPeople(gid).take(2)
        val fromSkewed = expense("exp_x", gid, "m1", 10_000L, rv = skewed, by = "m2")
        val fromHonest = expense("exp_x", gid, "m1", 12_000L, rv = next, by = "m1")
        val merged = merge(
            doc(gid, people, listOf(fromSkewed), shares(fromSkewed, people), at = now + 3 * day),
            doc(gid, people, listOf(fromHonest), shares(fromHonest, people), at = now),
            ExpenseVersioning.History()
        )
        assertEquals(12_000L, merged.expenses.single().totalAmountCents)
    }

    @Test
    fun `two edits at the same time - higher version wins, ties are decided the same way on every phone`() {
        val gid = "g_v240_concurrent"
        val people = sixPeople(gid).take(2)
        val a = expense("exp_c", gid, "m1", 10_000L, rv = t0 + 5, by = "m1")
        val b = expense("exp_c", gid, "m1", 20_000L, rv = t0 + 5, by = "m2")
        val docA = doc(gid, people, listOf(a), shares(a, people), at = t0 + 5)
        val docB = doc(gid, people, listOf(b), shares(b, people), at = t0 + 9)
        val onA = merge(docA, docB, ExpenseVersioning.History())
        val onB = merge(docB, docA, ExpenseVersioning.History())
        assertEquals(onA.expenses.single().totalAmountCents, onB.expenses.single().totalAmountCents)
        assertEquals(20_000L, onA.expenses.single().totalAmountCents, "tie goes to the higher editor id")

        val d = ExpenseVersioning.decide(a, shares(a, people), b, shares(b, people), localDocWins = true, history = null)
        assertTrue(d.concurrent)
    }

    @Test
    fun `without any version information the old document rule is unchanged`() {
        val gid = "g_v240_legacy_rule"
        val people = sixPeople(gid).take(2)
        val x = expense("exp_l", gid, "m1", 10_000L)
        val y = expense("exp_l", gid, "m1", 20_000L)
        val merged = merge(
            doc(gid, people, listOf(x), shares(x, people), at = t0),
            doc(gid, people, listOf(y), shares(y, people), at = t0 + 1),
            ExpenseVersioning.History()
        )
        assertEquals(20_000L, merged.expenses.single().totalAmountCents)
    }

    // --------------------------------------------------------------------------------------------
    // P2 / R5 and the codec
    // --------------------------------------------------------------------------------------------

    @Test
    fun `an expense with no shares fails the check`() {
        val e = expense("exp_empty", "g", "m1", 5_000L)
        assertFalse(ExpenseSplitIntegrity.isConsistent(e, emptyList()))
    }

    @Test
    fun `versions survive the sync codec and unversioned rows stay byte-compatible`() {
        val gid = "g_v240_codec"
        val people = sixPeople(gid).take(2)
        val v = expense("exp_v", gid, "m1", 10_000L, rv = t0 + 7, by = "m1")
        val u = expense("exp_u", gid, "m1", 10_000L)
        val d = doc(gid, people, listOf(v, u), shares(v, people) + shares(u, people), at = t0)
        val json = CloudGroupSyncRepository.encodeGroupLedgerDocument(d)
        val back = CloudGroupSyncRepository.decodeGroupLedgerDocument(json)
        assertNotNull(back)
        assertEquals(t0 + 7, back!!.expenses.first { it.expenseId == "exp_v" }.rowVersion)
        assertEquals("m1", back.expenses.first { it.expenseId == "exp_v" }.rowUpdatedBy)
        val wireU = JSONObject(json).getJSONArray("expenses").let { arr ->
            (0 until arr.length()).map { arr.getJSONObject(it) }.first { it.getString("expenseId") == "exp_u" }
        }
        assertFalse(wireU.has("rv"))
        assertFalse(wireU.has("rby"))
        assertFalse(wireU.has("q"))
    }

    @Test
    fun `content hash ignores fields older apps drop and sync bookkeeping`() {
        val e = expense("exp_h", "g", "m1", 10_000L)
        val s = listOf(ExpenseSplitEntity("exp_h_sp_0", "exp_h", "m1", 10_000L, 10_000L, false))
        val h = ExpenseVersioning.contentHash(e, s)
        assertEquals(h, ExpenseVersioning.contentHash(e.copy(categoryRef = "builtin:food", createdByPhone = "9000000001", syncStatus = "PENDING", rowVersion = 9, rowUpdatedBy = "x"), s))
        assertTrue(h != ExpenseVersioning.contentHash(e.copy(totalAmountCents = 10_001L), s))
    }

    // --------------------------------------------------------------------------------------------
    // P1/P4 stamping + history planning
    // --------------------------------------------------------------------------------------------

    @Test
    fun `first sync after upgrade only seeds history, later edits get a new version`() {
        val gid = "g_v240_stamp"
        val people = sixPeople(gid).take(2)
        val e = expense("exp_s", gid, "m1", 10_000L)
        val s = shares(e, people)
        val (stamps0, seeds) = ExpenseVersionSync.planLocalStamps(listOf(e), s, emptyList(), emptySequence(), "m1", t0)
        assertTrue(stamps0.isEmpty(), "upgrade must not change any row")
        assertEquals(listOf("seed"), seeds.map { it.kind })
        assertEquals(0L, seeds.single().rowVersion)

        // Unchanged row: nothing to do.
        val (stamps1, revs1) = ExpenseVersionSync.planLocalStamps(listOf(e), s, seeds, emptySequence(), "m1", t0 + 1)
        assertTrue(stamps1.isEmpty() && revs1.isEmpty())

        // Edited on this phone (any screen, so the row is PENDING): new version above everything seen.
        val edited = e.copy(totalAmountCents = 8_000L, baseSubtotalCents = 8_000L, syncStatus = "PENDING")
        val (stamps2, revs2) = ExpenseVersionSync.planLocalStamps(listOf(edited), shares(edited, people), seeds, sequenceOf(t0 + 50_000), "m1", t0 + 2)
        val stamped = stamps2.getValue("exp_s")
        assertEquals(t0 + 50_001, stamped.rowVersion)
        assertEquals("m1", stamped.rowUpdatedBy)
        assertEquals(listOf("local", "superseded"), revs2.map { it.kind }, "the replaced copy is kept as a fingerprint")
    }

    @Test
    fun `merge history records a revert as a heads-up and remembers losing copies`() {
        val gid = "g_v240_history"
        val people = sixPeople(gid).take(2)
        val v0 = expense("exp_r", gid, "m1", 795_000L)
        val v1 = expense("exp_r", gid, "m1", 495_000L, rv = t0 + 1, by = "m1")
        val v2back = v0.copy(rowVersion = t0 + 2, rowUpdatedBy = "m2") // someone saved the old amount again
        val local = doc(gid, people, listOf(v1), shares(v1, people), at = t0)
        val remote = doc(gid, people, listOf(v2back), shares(v2back, people), at = t0)
        val history = historyOf(v0 to shares(v0, people), v1 to shares(v1, people))
        val merged = merge(local, remote, history)
        assertEquals(795_000L, merged.expenses.single().totalAmountCents)
        val revs: List<ExpenseRevisionEntity> = ExpenseVersionSync.planMergeRevisions(local, remote, merged, history, emptySet(), emptySet(), t0 + 3)
        assertEquals("revert", revs.first { it.rowVersion == t0 + 2 }.kind)
    }

    // --------------------------------------------------------------------------------------------
    // Edit time + synced history (follow-ups)
    // --------------------------------------------------------------------------------------------

    @Test
    fun `an edit is versioned by when it was made, not when it synced`() {
        val gid = "g_v240_edittime"
        val people = sixPeople(gid).take(2)
        val e = expense("exp_t", gid, "m1", 10_000L)
        val seeds = ExpenseVersionSync.planLocalStamps(listOf(e), shares(e, people), emptyList(), emptySequence(), "m1", t0).second
        val edited = e.copy(totalAmountCents = 9_000L, baseSubtotalCents = 9_000L)
        // Edited offline at t0 + 1 hour, synced a day later.
        val (stamps, _) = ExpenseVersionSync.planLocalStamps(
            listOf(edited), shares(edited, people), seeds, emptySequence(), "m1", nowMs = t0 + day,
            editTimeOf = { t0 + 3_600_000L }
        )
        assertEquals(t0 + 3_600_000L, stamps.getValue("exp_t").rowVersion)

        // A concurrent edit made later on another phone (t0 + 2 hours) wins, even though ours synced last.
        val other = e.copy(totalAmountCents = 7_000L, baseSubtotalCents = 7_000L, rowVersion = t0 + 7_200_000L, rowUpdatedBy = "m2")
        val merged = merge(
            doc(gid, people, listOf(stamps.getValue("exp_t")), shares(edited, people), at = t0 + day),
            doc(gid, people, listOf(other), shares(other, people), at = t0 + 7_200_000L),
            ExpenseVersioning.History()
        )
        assertEquals(7_000L, merged.expenses.single().totalAmountCents)
    }

    @Test
    fun `edit history syncs compactly - member ids only, newest 5 per expense, deterministic union`() {
        val gid = "g_v240_revsync"
        val revs = (1..8).map { i ->
            ExpenseRevisionEntity("exp_a_${i}_h$i", "exp_a", gid, i.toLong(), "h$i", "", i * 100L, "m1", "m2", t0 + i, "local")
        } + ExpenseRevisionEntity("exp_gone_1_hx", "exp_gone", gid, 1L, "hx", "", 5L, "m1", null, t0, "local")
        val compact = ExpenseVersionSync.compactForSync(revs, deletedExpenseIds = setOf("exp_gone"))
        assertEquals(5, compact.size)
        assertEquals((4L..8L).toSet(), compact.map { it.rowVersion }.toSet(), "newest 5 kept")

        val json = ExpenseVersionSync.encodeRevisions(compact).toString()
        assertFalse(json.contains("9000000"), "no phone numbers on the public topic")
        val back = ExpenseVersionSync.decodeRevisions(org.json.JSONArray(json), gid)
        assertEquals(compact.map { it.revisionId }.toSet(), back.map { it.revisionId }.toSet())

        val a = ExpenseVersionSync.mergeSyncedRevisions(compact.take(3), compact.drop(2), emptySet())
        val b = ExpenseVersionSync.mergeSyncedRevisions(compact.drop(2), compact.take(3), emptySet())
        assertEquals(a, b, "every phone ends with the same list")
    }

    @Test
    fun `a version recorded by another phone makes an old app's re-upload of it stale`() {
        val gid = "g_v240_revknown"
        val people = sixPeople(gid).take(2)
        val v0 = expense("exp_k", gid, "m1", 90_000L)
        val v1 = expense("exp_k", gid, "m1", 60_000L, rv = t0 + 9, by = "m2")
        // This phone never saw v0 itself, but the cloud history (from m2's phone) lists it.
        val synced = listOf(ExpenseRevisionEntity("exp_k_0_x", "exp_k", gid, 0L, ExpenseVersioning.contentHash(v0, shares(v0, people)), "", 90_000L, "m1", null, t0, "seed"))
        val mine = doc(gid, people, listOf(v1), shares(v1, people), at = t0).copy(revisions = synced)
        val oldApp = doc(gid, people, listOf(v0), shares(v0, people), at = t0 + day)
        val merged = merge(mine, oldApp, ExpenseVersioning.History())
        assertEquals(60_000L, merged.expenses.single().totalAmountCents)
        assertTrue(merged.revisions.isNotEmpty(), "history keeps travelling")
    }

    // --------------------------------------------------------------------------------------------
    // Independent review findings (F1-F7) regression tests
    // --------------------------------------------------------------------------------------------

    @Test
    fun `F1 - an unsynced edit on the first sync after upgrade gets a version instead of being seeded`() {
        val gid = "g_v240_f1"
        val people = sixPeople(gid).take(2)
        val untouched = expense("exp_u", gid, "m1", 10_000L)
        val pending = expense("exp_p", gid, "m1", 5_400L).copy(syncStatus = "PENDING")
        val edited = expense("exp_e", gid, "m1", 4_950L)
        val (stamps, revs) = ExpenseVersionSync.planLocalStamps(
            listOf(untouched, pending, edited), shares(untouched, people) + shares(pending, people) + shares(edited, people),
            emptyList(), emptySequence(), "m1", nowMs = t0 + 100, editTimeOf = { id -> if (id == "exp_e") t0 + 50 else null },
            lastLocalChangeMs = t0 + 60 // B's offline edit on the old app happened at t0 + 60
        )
        assertEquals(setOf("exp_p", "exp_e"), stamps.keys)
        assertEquals(t0 + 50, stamps.getValue("exp_e").rowVersion)
        assertEquals("seed", revs.single { it.expenseId == "exp_u" }.kind)

        // Scenario A from the review: the stamped 5,400 now beats the other phone's older versioned 6,000.
        val remote6000 = expense("exp_p", gid, "m1", 6_000L, rv = t0 + 10, by = "m2")
        val merged = merge(
            doc(gid, people, listOf(stamps.getValue("exp_p")), shares(pending, people), at = t0 + 100),
            doc(gid, people, listOf(remote6000), shares(remote6000, people), at = t0 + 10),
            ExpenseVersionSync.historyOf(revs)
        )
        assertEquals(5_400L, merged.expenses.single().totalAmountCents)
    }

    @Test
    fun `F2 - the edit clock keeps the latest edit since the last sync`() {
        com.splitmate.app.data.ExpenseEditClock.clear(null, listOf("exp_f2"))
        com.splitmate.app.data.ExpenseEditClock.record(null, listOf("exp_f2"), t0 + 1_000)
        com.splitmate.app.data.ExpenseEditClock.record(null, listOf("exp_f2"), t0 + 5_000)
        com.splitmate.app.data.ExpenseEditClock.record(null, listOf("exp_f2"), t0 + 3_000)
        assertEquals(t0 + 5_000, com.splitmate.app.data.ExpenseEditClock.editTimeOf(null, "exp_f2"))
        com.splitmate.app.data.ExpenseEditClock.clear(null, listOf("exp_f2"))
    }

    @Test
    fun `F3 - a phone with incomplete history never adopts an unknown old-app copy`() {
        val gid = "g_v240_f3"
        val people = sixPeople(gid).take(2)
        val v1 = expense("exp_f3", gid, "m1", 495_000L, rv = t0 + 10, by = "m2")
        val staleUnknown = expense("exp_f3", gid, "m1", 795_000L) // this phone never saw 7,950
        // This phone first saw the expense at version T (joined/reinstalled later): incomplete record.
        val joinedLate = ExpenseVersioning.History(firstOwnVersionByExpense = mapOf("exp_f3" to t0 + 10))
        val merged = merge(
            doc(gid, people, listOf(v1), shares(v1, people), at = t0),
            doc(gid, people, listOf(staleUnknown), shares(staleUnknown, people), at = t0 + day),
            joinedLate
        )
        assertEquals(495_000L, merged.expenses.single().totalAmountCents)
        assertEquals(t0 + 10, merged.expenses.single().rowVersion, "no legacy version is invented")

        // Synced compaction drops display entries before older-copy fingerprints.
        val many = (1..400).map { i -> ExpenseRevisionEntity("e${i}_1_c", "e$i", gid, 1L, "c$i", "", 1L, "m1", null, t0 + i, "local") } +
            (1..50).map { i -> ExpenseRevisionEntity("e${i}_0_s", "e$i", gid, 0L, "s$i", "", 1L, "m1", null, t0, "seed") }
        val compact = ExpenseVersionSync.compactForSync(many)
        assertEquals(50, compact.count { it.kind == "seed" }, "every seed fingerprint survives the trip cap")
        assertTrue(compact.size <= ExpenseVersionSync.SYNC_PER_TRIP)
    }

    @Test
    fun `F4 - the same person under another phone's member id is recognised as the same copy`() {
        val gid = "g_v240_f4"
        val a = member("grp_akshay", gid, "Akshay", "9000000001", me = true)
        val bAlias = member("b_me", gid, "Akshay", "9000000001")
        val p = member("m2", gid, "Priya", "9000000002")
        val old = expense("exp_f4", gid, "grp_akshay", 795_000L)
        val oldShares = shares(old, listOf(a, p))
        val edited = expense("exp_f4", gid, "grp_akshay", 495_000L, rv = t0 + 10, by = "grp_akshay")
        // Old phone B: same stale 7,950, but Akshay is "b_me" there.
        val staleOnB = old.copy(payerId = "b_me")
        val staleShares = shares(staleOnB, listOf(bAlias, p))
        val merged = merge(
            doc(gid, listOf(a, p), listOf(edited), shares(edited, listOf(a, p)), at = t0),
            doc(gid, listOf(bAlias, p), listOf(staleOnB), staleShares, at = t0 + day),
            historyOf(old to oldShares, edited to shares(edited, listOf(a, p)))
        )
        assertEquals(495_000L, merged.expenses.single().totalAmountCents)
    }

    @Test
    fun `F7 - a plain newer edit from another phone is not labelled as two edits at the same time`() {
        val gid = "g_v240_f7"
        val people = sixPeople(gid).take(2)
        val mine = expense("exp_f7", gid, "m1", 10_000L, rv = t0 + 1, by = "m1")
        val newer = expense("exp_f7", gid, "m1", 12_000L, rv = t0 + 9, by = "m2")
        val local = doc(gid, people, listOf(mine), shares(mine, people), at = t0)
        val remote = doc(gid, people, listOf(newer), shares(newer, people), at = t0)
        val merged = merge(local, remote, historyOf(mine to shares(mine, people)))
        val revs = ExpenseVersionSync.planMergeRevisions(local, remote, merged, historyOf(mine to shares(mine, people)), emptySet(), emptySet(), t0 + 10)
        assertEquals("remote", revs.first { it.rowVersion == t0 + 9 }.kind)

        val minePending = local.copy(expenses = listOf(mine.copy(syncStatus = "PENDING")))
        val revs2 = ExpenseVersionSync.planMergeRevisions(minePending, remote, merged, null, emptySet(), emptySet(), t0 + 10)
        assertEquals("concurrent", revs2.first { it.rowVersion == t0 + 9 }.kind)
    }

    @Test
    fun `N1 - received history never counts as this phone's own record, and a replaced copy becomes a fingerprint`() {
        val gid = "g_v240_n1"
        val people = sixPeople(gid).take(2)
        val received = ExpenseRevisionEntity("x_5_h", "x", gid, 5L, "h", "", 1L, "m1", null, t0, "rx-local")
        val own = ExpenseRevisionEntity("x_9_g", "x", gid, 9L, "g", "", 1L, "m1", null, t0, "remote")
        val h = ExpenseVersionSync.historyOf(listOf(received, own))
        assertFalse(h.isCompleteUpTo("x", 9L), "first own observation is 9; the received 5 doesn't count")
        assertTrue(h.knows("x", "h"), "received entries still add known copies")

        val v0 = expense("exp_n1", gid, "m1", 795_000L)
        val seeds = ExpenseVersionSync.planLocalStamps(listOf(v0), shares(v0, people), emptyList(), emptySequence(), "m1", t0).second
        val edited = v0.copy(totalAmountCents = 495_000L, baseSubtotalCents = 495_000L, syncStatus = "PENDING")
        val (_, revs) = ExpenseVersionSync.planLocalStamps(listOf(edited), shares(edited, people), seeds, emptySequence(), "m1", t0 + 5)
        val fp = revs.single { it.kind == "superseded" }
        assertEquals(seeds.single().contentHash, fp.contentHash)
        assertTrue(fp.revisionId.endsWith("_fp"), "fingerprint never collides with the displayed entry")
        val wire = ExpenseVersionSync.decodeRevisions(ExpenseVersionSync.encodeRevisions(listOf(fp)), gid).single()
        assertEquals(fp.revisionId, wire.revisionId)
    }

    @Test
    fun `R2 - a fingerprint of a rejected copy never makes a late-joining phone look complete`() {
        val gid = "g_v240_r2"
        val ownAtT = ExpenseRevisionEntity("x_10_a", "x", gid, 10L, "a", "", 1L, "m1", null, t0, "remote")
        val rejectedOldCopy = ExpenseRevisionEntity("x_0_b_fp", "x", gid, 0L, "b", "", 1L, "m1", null, t0, "superseded")
        val h = ExpenseVersionSync.historyOf(listOf(ownAtT, rejectedOldCopy))
        assertFalse(h.isCompleteUpTo("x", 10L))
        assertTrue(h.knows("x", "b"), "still recognised as a known stale copy")
    }

    @Test
    fun `R3 - replacing a copy held only as received history still fingerprints it`() {
        val gid = "g_v240_r3"
        val people = sixPeople(gid).take(2)
        val v1 = expense("exp_r3", gid, "m1", 10_000L, rv = t0 + 1, by = "m2")
        val receivedV1 = ExpenseRevisionEntity("exp_r3_${t0 + 1}_x", "exp_r3", gid, t0 + 1, ExpenseVersioning.contentHash(v1, shares(v1, people)), "", 10_000L, "m1", "m2", t0, "rx-local")
        val edited = v1.copy(totalAmountCents = 9_000L, baseSubtotalCents = 9_000L, syncStatus = "PENDING")
        val (_, revs) = ExpenseVersionSync.planLocalStamps(listOf(edited), shares(edited, people), listOf(receivedV1), emptySequence(), "m1", t0 + 5)
        assertEquals(receivedV1.contentHash, revs.single { it.kind == "superseded" }.contentHash)
    }

    @Test
    fun `R5 - a member without a phone on one side resolves to the same key through the remap`() {
        val gid = "g_v240_r5"
        val withPhone = member("grp_a", gid, "Akshay", "9000000001")
        val noPhone = GroupMemberEntity(memberId = "old_a", groupId = gid, name = "Akshay", avatarSeed = "", isCurrentUser = false, upiId = "", userPhone = "", inviteStatus = "JOINED")
        val key = ExpenseVersioning.memberKeyOf(listOf(withPhone, noPhone)) { if (it == "old_a") "grp_a" else it }
        assertEquals(key("grp_a"), key("old_a"))
    }

    @Test
    fun `Q1 - owner phoneless in the cloud but phone-patched locally - stale copy on first sync still loses`() {
        val gid = "g_v240_q1"
        val bLocal = member("B_me", gid, "Bhavesh", "9000000007", me = true)          // phone patched in locally
        val bCloud = bLocal.copy(userPhone = "", upiId = "", isCurrentUser = false)    // old app never stored it
        val a = member("A1", gid, "Akshay", "9000000008")
        val stale = expense("exp_q1", gid, "B_me", 795_000L)
        val staleShares = shares(stale, listOf(bLocal, a))
        val edited = expense("exp_q1", gid, "B_me", 495_000L, rv = t0 + 10, by = "A1")
        val editedShares = shares(edited, listOf(bLocal, a))
        val localDoc = doc(gid, listOf(bLocal, a), listOf(stale), staleShares, at = t0 + day)
        val remoteDoc = doc(gid, listOf(bCloud, a), listOf(edited), editedShares, at = t0 + 10)

        // B's first sync after upgrading: seeds with the same member set the sync passes.
        val seeds = ExpenseVersionSync.planLocalStamps(
            listOf(stale), staleShares, emptyList(), emptySequence(), "B_me", t0 + day,
            memberKey = ExpenseVersioning.memberKeyOf(localDoc.members + remoteDoc.members)
        ).second
        assertEquals("seed", seeds.single().kind)
        val merged = merge(localDoc, remoteDoc, ExpenseVersionSync.historyOf(seeds))
        assertEquals(495_000L, merged.expenses.single().totalAmountCents, "A's edit must win; B's stale copy is known")
        assertEquals(t0 + 10, merged.expenses.single().rowVersion, "no legacy version is invented")
    }

    @Test
    fun `N3 - a PENDING row without an edit time is versioned by its creation time, not now`() {
        val gid = "g_v240_n3"
        val people = sixPeople(gid).take(2)
        val stalePending = expense("exp_n3", gid, "m1", 795_000L).copy(syncStatus = "PENDING")
        val (stamps, _) = ExpenseVersionSync.planLocalStamps(listOf(stalePending), shares(stalePending, people), emptyList(), emptySequence(), "m1", nowMs = t0 + 5 * day)
        assertEquals(t0, stamps.getValue("exp_n3").rowVersion, "createdAt, so a newer real edit elsewhere still wins")
    }

    // --------------------------------------------------------------------------------------------
    // P4 presentation
    // --------------------------------------------------------------------------------------------

    private fun rev(v: Long, total: Long, kind: String, at: Long, by: String? = "m1") = ExpenseRevisionEntity(
        revisionId = "exp_p_${v}_$kind", expenseId = "exp_p", groupId = "g", rowVersion = v, contentHash = "h$v$kind",
        title = "Bike rentals", totalAmountCents = total, payerId = "m2", editedBy = by, observedAtEpochMs = at, kind = kind
    )

    @Test
    fun `history shows changes newest first, hides seeds and losing copies, flags a change back`() {
        val revs = listOf(
            rev(0, 795_000, "seed", t0),
            rev(1, 495_000, "local", t0 + 10, by = "m2"),
            rev(1, 795_000, "superseded", t0 + 11, by = null),
            rev(2, 795_000, "revert", t0 + 20, by = "m3")
        )
        val changes = com.splitmate.app.ui.screens.ExpenseHistoryPresentation.changes(revs)
        assertEquals(2, changes.size)
        assertTrue(changes[0].isRevert)
        assertEquals(495_000L, changes[0].oldTotalCents)
        assertEquals(795_000L, changes[0].newTotalCents)
        assertEquals(795_000L, changes[1].oldTotalCents)
        assertEquals(495_000L, changes[1].newTotalCents)
        assertEquals("An older app", com.splitmate.app.ui.screens.ExpenseHistoryPresentation.whoLabel(ExpenseVersioning.LEGACY_EDITOR, emptyMap()))
        assertTrue(com.splitmate.app.ui.screens.ExpenseHistoryPresentation.changes(listOf(rev(0, 1, "seed", t0))).isEmpty(), "no history row before any edit")
    }

    // --------------------------------------------------------------------------------------------
    // P5.2 Random merge testing
    // --------------------------------------------------------------------------------------------

    private class Phone(val id: String, val oldApp: Boolean, val clockSkew: Long) {
        var doc: CloudGroupLedgerDocument? = null
        val known = mutableMapOf<String, MutableSet<String>>()
        val firstOwn = mutableMapOf<String, Long>()
        var maxSeen = 0L
        fun history() = ExpenseVersioning.History(known.mapValues { it.value.toSet() }, maxSeen, firstOwn.toMap())
        fun remember(d: CloudGroupLedgerDocument?) {
            d ?: return
            val k = ExpenseVersioning.memberKeyOf(d.members)
            d.expenses.forEach { e ->
                known.getOrPut(e.expenseId) { mutableSetOf() } += ExpenseVersioning.contentHash(e, d.splits, k)
                firstOwn[e.expenseId] = minOf(firstOwn[e.expenseId] ?: Long.MAX_VALUE, e.rowVersion)
                maxSeen = maxOf(maxSeen, e.rowVersion)
            }
        }
    }

    private fun strip(d: CloudGroupLedgerDocument?) = d?.copy(expenses = d.expenses.map { it.copy(rowVersion = 0L, rowUpdatedBy = null) })

    @Test
    fun `random merges - every phone converges, every expense adds up, nothing disappears without a delete`() {
        repeat(40) { seed ->
            val rnd = Random(seed)
            val gid = "g_v240_fuzz_$seed"
            val people = sixPeople(gid).take(3)
            val phones = listOf(
                Phone("m1", oldApp = false, clockSkew = 0L),
                Phone("m2", oldApp = false, clockSkew = 3 * day),
                Phone("m3", oldApp = rnd.nextBoolean(), clockSkew = 0L)
            )
            var cloud: CloudGroupLedgerDocument? = null
            var clock = t0
            val created = mutableSetOf<String>()
            val deleted = mutableSetOf<String>()

            fun sync(p: Phone) {
                val local = p.doc
                val remote = cloud
                if (local == null && remote == null) return
                val merged = if (p.oldApp) {
                    strip(CloudGroupSyncRepository.mergeGroupLedgerDocuments(strip(local), strip(remote), "9000000001", versionHistory = null))!!
                } else {
                    val m = CloudGroupSyncRepository.mergeGroupLedgerDocuments(local, remote, "9000000001", versionHistory = p.history())
                    p.remember(local); p.remember(remote); p.remember(m)
                    m
                }
                p.doc = merged
                cloud = merged
            }

            repeat(60) {
                clock += rnd.nextLong(1, 5_000)
                val p = phones[rnd.nextInt(phones.size)]
                val now = clock + p.clockSkew
                val d = p.doc ?: doc(gid, people, emptyList(), emptyList(), at = now)
                when (rnd.nextInt(5)) {
                    0 -> { // create
                        val id = "exp_${seed}_${created.size}"
                        created += id
                        val e = expense(id, gid, people[rnd.nextInt(3)].memberId, rnd.nextLong(1_000, 900_000),
                            rv = if (p.oldApp) 0L else ExpenseVersioning.nextVersion(now, p.maxSeen), by = if (p.oldApp) null else p.id)
                        p.maxSeen = maxOf(p.maxSeen, e.rowVersion)
                        p.doc = d.copy(expenses = d.expenses + e, splits = d.splits + shares(e, people), updatedAtEpochMs = now)
                        if (!p.oldApp) p.remember(p.doc)
                    }
                    1, 2 -> { // edit (same split ids)
                        val target = d.expenses.randomOrNull(rnd) ?: return@repeat
                        val e = target.copy(
                            totalAmountCents = rnd.nextLong(1_000, 900_000).also { },
                            rowVersion = if (p.oldApp) 0L else ExpenseVersioning.nextVersion(now, p.maxSeen),
                            rowUpdatedBy = if (p.oldApp) null else p.id
                        ).let { it.copy(baseSubtotalCents = it.totalAmountCents) }
                        p.maxSeen = maxOf(p.maxSeen, e.rowVersion)
                        p.doc = d.copy(
                            expenses = d.expenses.map { if (it.expenseId == e.expenseId) e else it },
                            splits = d.splits.filter { it.expenseId != e.expenseId } + shares(e, people),
                            updatedAtEpochMs = now
                        )
                        if (!p.oldApp) p.remember(p.doc)
                    }
                    3 -> { // delete
                        val target = d.expenses.randomOrNull(rnd) ?: return@repeat
                        deleted += target.expenseId
                        p.doc = d.copy(
                            expenses = d.expenses.filter { it.expenseId != target.expenseId },
                            splits = d.splits.filter { it.expenseId != target.expenseId },
                            deletedExpenseIds = d.deletedExpenseIds + (target.expenseId to now),
                            updatedAtEpochMs = now
                        )
                    }
                    else -> sync(p)
                }
            }
            // Old phones update (or stop); everyone syncs until stable.
            phones.filter { it.oldApp }.forEach { it.doc = null }
            val active = phones.filter { !it.oldApp }
            repeat(4) { active.forEach { sync(it) } }

            val reference = cloud!!
            for (p in active) {
                val mine = p.doc!!
                assertEquals(reference.expenses.map { it.expenseId }.toSet(), mine.expenses.map { it.expenseId }.toSet(), "seed $seed: same expenses on ${p.id}")
                for (e in mine.expenses) {
                    val ref = reference.expenses.first { it.expenseId == e.expenseId }
                    assertEquals(ExpenseVersioning.contentHash(ref, reference.splits), ExpenseVersioning.contentHash(e, mine.splits), "seed $seed: ${e.expenseId} converged")
                }
            }
            for (e in reference.expenses) {
                val rows = reference.splits.filter { it.expenseId == e.expenseId }
                assertTrue(ExpenseSplitIntegrity.isConsistent(e, rows), "seed $seed: ${e.expenseId} adds up")
            }
            val alive = reference.expenses.map { it.expenseId }.toSet()
            val vanished = created - deleted - alive
            // Expenses created only on an old phone that never synced before it was dropped are not lost data.
            assertTrue(vanished.all { id -> phones.none { !it.oldApp && it.known.containsKey(id) } }, "seed $seed: lost without a delete: $vanished")
        }
    }
}
