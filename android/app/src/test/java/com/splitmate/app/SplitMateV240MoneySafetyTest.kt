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

    private fun member(id: String, gid: String, name: String, phone: String, me: Boolean = false) = GroupMemberEntity(
        memberId = id, groupId = gid, name = name, avatarSeed = "$name|Neutral|open-peeps|Buckwheat",
        isCurrentUser = me, upiId = "$phone@upi", userPhone = phone, inviteStatus = "JOINED"
    )

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
        knownHashesByExpense = pairs.groupBy({ it.first.expenseId }, { ExpenseVersioning.contentHash(it.first, it.second) })
            .mapValues { it.value.toSet() },
        maxSeenVersion = pairs.maxOf { it.first.rowVersion }
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

        // Edited on this phone (any screen): new version above everything seen, including the cloud.
        val edited = e.copy(totalAmountCents = 8_000L, baseSubtotalCents = 8_000L)
        val (stamps2, revs2) = ExpenseVersionSync.planLocalStamps(listOf(edited), shares(edited, people), seeds, sequenceOf(t0 + 50_000), "m1", t0 + 2)
        val stamped = stamps2.getValue("exp_s")
        assertEquals(t0 + 50_001, stamped.rowVersion)
        assertEquals("m1", stamped.rowUpdatedBy)
        assertEquals(listOf("local"), revs2.map { it.kind })
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
        var maxSeen = 0L
        fun history() = ExpenseVersioning.History(known.mapValues { it.value.toSet() }, maxSeen)
        fun remember(d: CloudGroupLedgerDocument?) {
            d ?: return
            d.expenses.forEach { e ->
                known.getOrPut(e.expenseId) { mutableSetOf() } += ExpenseVersioning.contentHash(e, d.splits)
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
