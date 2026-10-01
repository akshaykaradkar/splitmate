package com.splitmate.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ElectricRickshaw
import androidx.compose.material.icons.rounded.FreeBreakfast
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.TwoWheeler
import com.splitmate.app.ui.category.ExpenseCategoryCatalog
import com.splitmate.app.ui.category.ExpenseCategoryGroup
import com.splitmate.app.ui.category.ExpenseCategoryIconKeys
import com.splitmate.app.ui.category.ExpenseCategoryIcons
import com.splitmate.app.ui.resolveExpenseCategoryIcon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * v2.3.4 Expense categories: more built-ins (from user interviews) + user-created categories with icons.
 * The core guarantee under test: nothing that already exists changes.
 */
class SplitMateV234ExpenseCategoriesTest {

    @Test
    fun `C01 legacy preset titles are kept verbatim and listed first in the same order`() {
        val legacy = listOf(
            "Train / PNR Ticket", "Dinner & Food", "Travel & Flight", "Stay & Hotel",
            "Cab & Local", "Groceries", "Party & Drinks"
        )
        assertEquals(legacy, ExpenseCategoryCatalog.LEGACY_PRESET_TITLES)
        assertEquals(legacy, ExpenseCategoryCatalog.BUILT_IN.take(legacy.size).map { it.title })
        assertTrue(ExpenseCategoryCatalog.BUILT_IN.first().opensPnrFlow)
    }

    @Test
    fun `C02 interview categories are available`() {
        val titles = ExpenseCategoryCatalog.BUILT_IN.map { it.title }.toSet()
        listOf(
            "Breakfast", "Lunch", "Dinner", "Snacks & Chai", "Auto Rickshaw", "Bike Rental",
            "Fuel & Petrol", "Tolls & Parking", "Bus", "Metro", "Entry Tickets", "Medicines"
        ).forEach { assertTrue(it in titles, "missing built-in $it") }
    }

    @Test
    fun `C03 built-ins have unique titles and every icon key is known`() {
        val norm = ExpenseCategoryCatalog.BUILT_IN.map { it.title.lowercase() }
        assertEquals(norm.size, norm.toSet().size)
        ExpenseCategoryCatalog.BUILT_IN.forEach {
            assertTrue(ExpenseCategoryIconKeys.isKnown(it.iconKey), "unknown icon ${it.iconKey} for ${it.title}")
        }
        assertEquals(ExpenseCategoryIconKeys.ALL.size, ExpenseCategoryIconKeys.ALL.toSet().size)
    }

    @Test
    fun `C04 every icon key maps to a real icon (not the receipt fallback) except receipt itself`() {
        val fallback = Icons.AutoMirrored.Rounded.ReceiptLong
        ExpenseCategoryIconKeys.ALL.filter { it != "receipt" }.forEach {
            assertTrue(ExpenseCategoryIcons.forKey(it) !== fallback, "icon key $it falls back to receipt")
        }
    }

    @Test
    fun `C05 legacy titles never exact-match so their icons still come from the keyword engine`() {
        ExpenseCategoryCatalog.LEGACY_PRESET_TITLES.forEach {
            assertNull(ExpenseCategoryCatalog.exactMatch(it, emptyList()), it)
        }
        assertEquals(Icons.Rounded.Restaurant, resolveExpenseCategoryIcon("Dinner & Food"))
        assertEquals(Icons.Rounded.LocalTaxi, resolveExpenseCategoryIcon("Cab & Local"))
        assertEquals(Icons.Rounded.Hotel, resolveExpenseCategoryIcon("Stay & Hotel"))
        assertEquals(Icons.Rounded.ShoppingCart, resolveExpenseCategoryIcon("Groceries"))
        assertEquals(Icons.Rounded.LocalBar, resolveExpenseCategoryIcon("Party & Drinks"))
        assertEquals(Icons.Rounded.Train, resolveExpenseCategoryIcon("Train ride"))
        assertEquals(Icons.Rounded.LocalTaxi, resolveExpenseCategoryIcon("Uber to station"))
        assertEquals(Icons.AutoMirrored.Rounded.ReceiptLong, resolveExpenseCategoryIcon("Random thing"))
    }

    @Test
    fun `C06 new built-in titles resolve to their dedicated icons`() {
        assertEquals(Icons.Rounded.FreeBreakfast, resolveExpenseCategoryIcon("Breakfast"))
        assertEquals(Icons.Rounded.ElectricRickshaw, resolveExpenseCategoryIcon("auto rickshaw"))
        assertEquals(Icons.Rounded.TwoWheeler, resolveExpenseCategoryIcon("  Bike Rental "))
        assertEquals(Icons.Rounded.LocalGasStation, resolveExpenseCategoryIcon("Fuel & Petrol"))
    }

    @Test
    fun `C07 custom categories exact-match by title`() {
        val custom = listOf(ExpenseCategoryCatalog.custom("Paragliding Bir", "paragliding"))
        val hit = ExpenseCategoryCatalog.exactMatch("paragliding bir", custom)
        assertNotNull(hit)
        assertEquals("paragliding", hit!!.iconKey)
        assertEquals(ExpenseCategoryGroup.CUSTOM, hit.group)
        assertNull(ExpenseCategoryCatalog.exactMatch("Paragliding Bir 2nd day", custom))
    }

    @Test
    fun `C08 validation rejects blank, duplicates of built-ins and customs, and caps the count`() {
        val custom = listOf(ExpenseCategoryCatalog.custom("Kerala Toll", "toll"))
        assertTrue(ExpenseCategoryCatalog.validateNew(" ", custom) is ExpenseCategoryCatalog.Validation.Error)
        assertTrue(ExpenseCategoryCatalog.validateNew("breakfast", custom) is ExpenseCategoryCatalog.Validation.Error)
        assertTrue(ExpenseCategoryCatalog.validateNew("Dinner & Food", custom) is ExpenseCategoryCatalog.Validation.Error)
        assertTrue(ExpenseCategoryCatalog.validateNew("KERALA TOLL", custom) is ExpenseCategoryCatalog.Validation.Error)
        val ok = ExpenseCategoryCatalog.validateNew("  Houseboat   Stay ", custom)
        assertEquals(ExpenseCategoryCatalog.Validation.Ok("Houseboat Stay"), ok)
        val full = (1..ExpenseCategoryCatalog.MAX_CUSTOM_CATEGORIES).map { ExpenseCategoryCatalog.custom("Cat $it", "more") }
        assertTrue(ExpenseCategoryCatalog.validateNew("One more", full) is ExpenseCategoryCatalog.Validation.Error)
    }

    @Test
    fun `C09 codec round-trips and sanitises separators`() {
        val list = listOf(
            ExpenseCategoryCatalog.custom("Houseboat", "boat"),
            ExpenseCategoryCatalog.custom("Bad|Name\nX", "nope")
        )
        val decoded = ExpenseCategoryCatalog.decodeCustom(ExpenseCategoryCatalog.encodeCustom(list))
        assertEquals(listOf("Houseboat", "Bad Name X"), decoded.map { it.title })
        assertEquals("boat", decoded[0].iconKey)
        assertEquals(ExpenseCategoryIconKeys.DEFAULT_CUSTOM, decoded[1].iconKey)
        assertTrue(decoded.all { it.isCustom })
    }

    @Test
    fun `C10 decoder tolerates garbage, duplicates and built-in collisions`() {
        val raw = "boat|Houseboat\n\ngarbage line\n|NoIcon\nboat|houseboat\nbreakfast|Breakfast\nbed|Zostel"
        assertEquals(listOf("Houseboat", "Zostel"), ExpenseCategoryCatalog.decodeCustom(raw).map { it.title })
        assertTrue(ExpenseCategoryCatalog.decodeCustom(null).isEmpty())
    }

    @Test
    fun `C11 icon suggestion follows the typed name`() {
        assertEquals("two_wheeler", ExpenseCategoryCatalog.suggestIconKey("Royal Enfield bike"))
        assertEquals("fuel", ExpenseCategoryCatalog.suggestIconKey("Diesel refill"))
        assertEquals("auto_rickshaw", ExpenseCategoryCatalog.suggestIconKey("Auto to station"))
        assertEquals(ExpenseCategoryIconKeys.DEFAULT_CUSTOM, ExpenseCategoryCatalog.suggestIconKey("Zzz"))
    }

    @Test
    fun `C12 search covers titles, short labels and groups, customs included`() {
        val custom = listOf(ExpenseCategoryCatalog.custom("Houseboat", "boat"))
        assertTrue(ExpenseCategoryCatalog.search("petrol", custom).any { it.title == "Fuel & Petrol" })
        assertTrue(ExpenseCategoryCatalog.search("getting around", custom).any { it.title == "Bus" })
        assertTrue(ExpenseCategoryCatalog.search("house", custom).any { it.title == "Houseboat" })
        assertEquals(ExpenseCategoryCatalog.all(custom).size, ExpenseCategoryCatalog.search("", custom).size)
    }

    @Test
    fun `C13 no schema or money code was touched by the category feature`() {
        val root = File("src/main/java/com/splitmate/app/ui/category")
        assertTrue(root.isDirectory)
        root.listFiles()!!.forEach { f ->
            val src = f.readText()
            assertTrue("@Entity" !in src && "Migration" !in src && "amountCents" !in src, "${f.name} must stay UI/title-only")
        }
    }
}
