package com.splitmate.app

import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.PersonNames
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.withDisplayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 * v2.4.0 rc12: people's names are Title Case app-wide, normalised where they enter the app
 * (keyboard, invite links, contacts, Room reads, cloud sync), so every screen agrees.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplitMateV240Rc12PersonNamesTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun src(path: String) = File("src/main/java/com/splitmate/app/$path").readText()

    @Test
    fun `RC12_01 the rule title-cases typed names without breaking real ones`() {
        assertEquals("Sam", PersonNames.display("sam"))
        assertEquals("Sam", PersonNames.display("SAM"))
        assertEquals("Priya Shah", PersonNames.display("  priya   shah "))
        assertEquals("Priya Shah", PersonNames.display("PRIYA SHAH"))
        assertEquals("Anne-Marie", PersonNames.display("anne-marie"))
        assertEquals("Ravi McKenzie", PersonNames.display("ravi McKenzie"), "mixed-case word keeps its casing")
        assertEquals("Rohan 2", PersonNames.display("rohan 2"))
        assertEquals("", PersonNames.display("   "))
        val ok = GroupMemberEntity("m1", "g", "Maya", "Maya")
        assertSame(ok, ok.withDisplayName(), "already clean: same instance, no churn")
        assertEquals("Maya", GroupMemberEntity("m1", "g", "maya", "maya").withDisplayName().name)
    }

    @Test
    fun `RC12_02 names typed in any case are shown in title case across the app`() = runTest(testDispatcher) {
        val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        vm.createNewGroup(name = "Goa", currencyCode = "INR", friendNamesCsv = "priyanka, SAM")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.addMemberToActiveGroup("rahul sharma")
        testDispatcher.scheduler.advanceUntilIdle()
        val names = vm.uiState.value.activeGroupMembers.map { it.name }
        assertTrue(names.containsAll(listOf("Priyanka", "Sam", "Rahul Sharma")), "got $names")
        assertTrue(names.none { it == "priyanka" || it == "SAM" || it == "rahul sharma" }, "got $names")
    }

    @Test
    fun `RC12_03 every entry point uses the one rule`() {
        val vm = src("ui/SplitMateViewModel.kt")
        val cloud = src("data/CloudGroupSyncRepository.kt")
        assertTrue(vm.contains("roomDao.observeAllMembers().first().withDisplayNames()"), "first Room read")
        assertTrue(vm.contains(".map { it.withDisplayNames() } // rc12"), "live Room updates")
        assertTrue(Regex("""PersonNames\.display\(""").findAll(vm).count() >= 15, "local entry + profile name")
        assertTrue(cloud.contains("name = com.splitmate.app.ui.PersonNames.display(m.optString(\"name\", \"\"))"), "names from other phones")
        assertTrue(cloud.contains("dao.getMembersForGroup(groupId).withDisplayNames()"))
        assertTrue(src("ui/components/ExpenseUiMappers.kt").contains("PersonNames.display(raw)"), "Money cards share the rule")
    }
}
