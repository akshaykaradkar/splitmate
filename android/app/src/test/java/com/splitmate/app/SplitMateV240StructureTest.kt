package com.splitmate.app

import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.screens.TripHubLayoutRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** v2.4.0 D7 remaining structure: calm empty states, keypad-first entry, share progress, member joined. */
class SplitMateV240StructureTest {

    private fun src(path: String) = File("src/main/java/com/splitmate/app/$path").readText()

    private fun member(
        id: String,
        group: String = "g1",
        name: String = id.replaceFirstChar { it.uppercase() },
        phone: String = "",
        status: String = "JOINED",
        me: Boolean = false
    ) = GroupMemberEntity(memberId = id, groupId = group, name = name, avatarSeed = id, isCurrentUser = me, userPhone = phone, inviteStatus = status)

    private val names = mapOf("g1" to "Goa", "g2" to "Lake Tahoe Cabin")
    private val me = member("me", phone = "9000000001", me = true)

    // ---------------------------------------------------------------- empty states

    @Test
    fun `D7_01 a trip is empty only with no expenses and every balance zero`() {
        assertTrue(TripHubLayoutRules.isEmptyTrip(0, listOf(0L, 0L)))
        assertTrue(TripHubLayoutRules.isEmptyTrip(0, emptyList()))
        assertFalse(TripHubLayoutRules.isEmptyTrip(1, listOf(0L, 0L)), "any expense shows the money UI")
        assertFalse(TripHubLayoutRules.isEmptyTrip(0, listOf(1417L, -1417L)), "a leftover balance is never hidden")
    }

    @Test
    fun `D7_02 Trip Hub uses the calm empty state, the busy card is gone`() {
        val trip = src("ui/screens/TripHomeScreen.kt")
        assertFalse(trip.contains("EmptyTripHubStateCard"))
        assertFalse(trip.contains("Your Trip Hub is Ready"))
        assertFalse(trip.contains("+ Log Train PNR"))
        assertTrue(Regex("CalmEmptyState\\(").findAll(trip).count() >= 3, "Overview, Plan and Money")
        assertTrue(trip.contains("if (isEmptyTrip)"), "header and Money follow the one rule")
        val calm = src("ui/components/CalmEmptyState.kt")
        assertTrue(calm.contains("MaterialShapes.Cookie4Sided"), "one soft shape that is not an avatar shape")
        assertFalse(calm.contains("infiniteRepeatable"), "static: nothing animates")
    }

    // ---------------------------------------------------------------- keypad-first

    @Test
    fun `D7_03 expense form opens keypad-first with no progress line on money`() {
        val form = src("ui/screens/QuickExpenseAndGuideScreens.kt")
        assertTrue(form.contains("AmountInputCursor("))
        assertTrue(form.contains("rememberReducedMotionEnabled()"), "cursor holds still under reduced motion")
        assertFalse(form.contains("LinearWavyProgressIndicator("))
        assertTrue(form.contains("if (totalAmountPaise > 0L && isZeroDriftVerified)"), "'Adds up exactly' waits for an amount")
        assertTrue(form.contains("if (totalAmountPaise > 0L || memberCount == 0)"), "per-person pill waits for an amount")
    }

    // ---------------------------------------------------------------- share progress

    @Test
    fun `D7_04 share image shows the M3 Expressive loading shape and a plain label`() {
        val share = src("ui/share/SettleUpShareButton.kt")
        assertTrue(share.contains("com.splitmate.app.ui.components.LoadingIndicator("))
        assertFalse(share.contains("CircularProgressIndicator("))
        assertTrue(share.contains("\"Making image…\""))
        assertTrue(share.contains("MIN_PROGRESS_VISIBLE_MS = 400L"))
    }

    // ---------------------------------------------------------------- member joined

    @Test
    fun `D7_05 an accepted invite is announced once`() {
        val before = listOf(me, member("priya", phone = "9000000002", status = "PENDING"))
        val after = listOf(me, member("priya", phone = "9000000002", status = "JOINED"))
        val arrivals = MemberArrivals.detect(setOf("g1"), before, after, names, "9000000001")
        assertEquals(listOf(MemberArrival("g1", "Goa", "Priya")), arrivals)
        assertEquals(listOf("Priya joined Goa"), MemberArrivals.messages(arrivals))
        // The next refresh with the same data announces nobody.
        assertTrue(MemberArrivals.detect(setOf("g1"), after, after, names, "9000000001").isEmpty())
    }

    @Test
    fun `D7_06 someone joining with the code is announced`() {
        val before = listOf(me)
        val after = listOf(me, member("sam", phone = "+91 90000 00003"))
        assertEquals(listOf("Sam joined Goa"), MemberArrivals.messages(MemberArrivals.detect(setOf("g1"), before, after, names, "9000000001")))
    }

    @Test
    fun `D7_07 never announced - you, name-only members, new trips, row id changes`() {
        val before = listOf(me, member("ravi", phone = "9000000004"))
        val cases = listOf(
            // name-only member added on this phone
            listOf(me, member("ravi", phone = "9000000004"), member("maya")),
            // the current user row
            listOf(me, member("ravi", phone = "9000000004"), member("me2", phone = "9000000009", me = true)),
            // your own number under another row
            listOf(me, member("ravi", phone = "9000000004"), member("dup", phone = "9000000001")),
            // same person, new row id, already joined before
            listOf(me, member("ravi2", name = "Ravi", phone = "9000000004")),
            // still pending
            listOf(me, member("ravi", phone = "9000000004"), member("anu", phone = "9000000005", status = "PENDING"))
        )
        cases.forEachIndexed { i, after ->
            assertTrue(MemberArrivals.detect(setOf("g1"), before, after, names, "9000000001").isEmpty(), "case $i")
        }
        // A trip that is new on this phone (you just joined or restored it): nobody is announced.
        val newTrip = listOf(member("zoe", group = "g2", phone = "9000000006"))
        assertTrue(MemberArrivals.detect(setOf("g1"), before, before + newTrip, names, "9000000001").isEmpty())
    }

    @Test
    fun `D7_08 several arrivals in one sync become one short line per trip`() {
        fun arrival(n: String, g: String = "g1") = MemberArrival(g, names.getValue(g), n)
        assertEquals(listOf("Priya and Sam joined Goa"), MemberArrivals.messages(listOf(arrival("Priya"), arrival("Sam"))))
        assertEquals(listOf("3 people joined Goa"), MemberArrivals.messages(listOf(arrival("A"), arrival("B"), arrival("C"))))
        assertEquals(
            listOf("Priya joined Goa", "Sam joined Lake Tahoe Cabin"),
            MemberArrivals.messages(listOf(arrival("Priya"), arrival("Sam", "g2")))
        )
    }

    @Test
    fun `D7_09 the joined line never knocks away an Undo and app start stays quiet`() {
        val snackbar = src("ui/feel/MoneySnackbar.kt")
        val announce = snackbar.substringAfter("fun announce(").substringBefore("\n    }\n")
        assertFalse(announce.contains("dismiss()"), "announce must wait, not dismiss")
        assertTrue(announce.contains("first { it == null }"), "announce waits until nothing is showing")
        val offerUndo = snackbar.substringAfter("fun offerUndo(").substringBefore("\n    }\n")
        val say = snackbar.substringAfter("fun say(").substringBefore("\n    }\n")
        assertTrue(offerUndo.contains("cancelAnnouncements()"), "review J3: Undo is never queued behind joined lines")
        assertTrue(say.contains("cancelAnnouncements()"))

        val vm = src("ui/SplitMateViewModel.kt")
        // Review J1/J2: one serialized source (Room members stream); its first emission is the baseline.
        val collector = vm.substringAfter("roomDao.observeAllMembers().collect { members ->").substringBefore("\n            }\n")
        assertTrue(collector.contains("lastMembers?.let { previous -> announceMemberArrivals(previous, members) }"))
        assertTrue(collector.contains("lastMembers = members"))
        assertFalse(vm.contains("memberArrivalBaselineReady"), "no racy diff in refreshStateFromDaoSnapshot")
    }

    @Test
    fun `D7_10 nobody is announced in a trip you have not joined yourself`() {
        val pendingMe = member("me", phone = "9000000001", status = "PENDING", me = true)
        val before = listOf(pendingMe, member("priya", phone = "9000000002", status = "PENDING"))
        val after = listOf(pendingMe, member("priya", phone = "9000000002"))
        assertTrue(MemberArrivals.detect(setOf("g1"), before, after, names, "9000000001").isEmpty())
        // Matched by phone too (your row may not carry isCurrentUser on every phone).
        val meByPhone = member("me", phone = "9000000001")
        val before2 = listOf(meByPhone, member("priya", phone = "9000000002", status = "PENDING"))
        val after2 = listOf(meByPhone, member("priya", phone = "9000000002"))
        assertEquals(listOf("Priya joined Goa"), MemberArrivals.messages(MemberArrivals.detect(setOf("g1"), before2, after2, names, "9000000001")))
    }
}
