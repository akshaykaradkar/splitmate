package com.splitmate.app.guide.vm

import com.splitmate.app.data.guide.BookingDestinationSuggestions
import com.splitmate.app.data.guide.MapShareTextDetector
import com.splitmate.app.data.guide.TripGuideServices
import com.splitmate.app.data.guide.sync.NtfyPlanManifestSync
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("v2.3.4 E1: share-intent detection, booking suggestions, service hooks")
class TripGuideHelpersTest {

    // ---- MapShareTextDetector --------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://maps.app.goo.gl/AbC123xyz",
            "Hotel Mayura Bhuvaneswari\nhttps://maps.app.goo.gl/AbC123xyz",
            "Check this https://goo.gl/maps/Xy12",
            "https://g.co/kgs/Ab12Cd",
            "https://www.google.com/maps/place/Hampi/@15.335,76.46,15z",
            "https://www.google.co.in/maps?q=15.3350,76.4600",
            "http://maps.google.com/?q=15.335,76.46",
            "geo:15.3350,76.4600?q=Hampi",
            "Our stay: geo:-8.65,115.21"
        ]
    )
    fun `map share texts are detected`(text: String) {
        assertTrue(MapShareTextDetector.isMapShareText(text), text)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "   ",
            "SM2_H4sIAAAAAAAA https://maps.app.goo.gl/AbC123xyz",
            "https://akshaykaradkar.github.io/splitmate/join?g=Ab12",
            "notgoo.gl/maps/abc",
            "https://evilgoogle.com/maps/x",
            "https://maps.google.evil.example/",
            "I love maps and geography: Hampi",
            "Paid 450 for dinner at Mango Tree"
        ]
    )
    fun `other shared texts are ignored`(text: String) {
        assertFalse(MapShareTextDetector.isMapShareText(text), text)
    }

    @Test
    fun `null text and links beyond the size cap are ignored`() {
        assertFalse(MapShareTextDetector.isMapShareText(null))
        val padded = "x".repeat(MapShareTextDetector.MAX_SHARED_CHARS) + " https://maps.app.goo.gl/AbC"
        assertFalse(MapShareTextDetector.isMapShareText(padded))
    }

    // ---- BookingDestinationSuggestions -----------------------------------------------------------

    @Test
    fun `station and airport names become short destination suggestions`() {
        assertEquals(listOf("Hosapete"), BookingDestinationSuggestions.candidatesFor("HPT (Hosapete Jn)"))
        assertEquals(listOf("Goa", "Madgaon"), BookingDestinationSuggestions.candidatesFor("Madgaon (Goa)"))
        assertEquals(listOf("Hampi", "Hosapete"), BookingDestinationSuggestions.candidatesFor("HPT (Hosapete (Hampi))"))
        assertEquals(listOf("Bengaluru City"), BookingDestinationSuggestions.candidatesFor("KSR Bengaluru City Jn"))
        assertEquals(listOf("Goa"), BookingDestinationSuggestions.candidatesFor("Goa International Airport"))
        assertEquals(listOf("Mumbai"), BookingDestinationSuggestions.candidatesFor("BOM (Mumbai)"))
        assertEquals(listOf("Varanasi"), BookingDestinationSuggestions.candidatesFor("Varanasi Junction"))
    }

    @Test
    fun `codes, placeholders and blanks are dropped`() {
        assertEquals(emptyList<String>(), BookingDestinationSuggestions.candidatesFor("GOI"))
        assertEquals(emptyList<String>(), BookingDestinationSuggestions.candidatesFor("Destination Station"))
        assertEquals(emptyList<String>(), BookingDestinationSuggestions.candidatesFor(null))
        assertEquals(emptyList<String>(), BookingDestinationSuggestions.candidatesFor("   "))
    }

    @Test
    fun `normalise de-duplicates case-insensitively and caps the list`() {
        assertEquals(listOf("Hampi"), BookingDestinationSuggestions.normalise(listOf("Hampi", "hampi", "HAMPI")))
        val many = listOf("Madgaon (Goa)", "HPT (Hosapete Jn)", "Varanasi Junction", "BOM (Mumbai)")
        assertEquals(listOf("Goa", "Madgaon", "Hosapete"), BookingDestinationSuggestions.normalise(many))
        assertEquals(listOf("Goa"), BookingDestinationSuggestions.normalise(many, limit = 1))
        assertEquals(emptyList<String>(), BookingDestinationSuggestions.normalise(emptyList()))
    }

    // ---- TripGuideServices hooks (pure parts only) -----------------------------------------------

    @Test
    fun `plan topics are distinguished from ledger topics`() {
        assertTrue(TripGuideServices.isPlanTopic(NtfyPlanManifestSync.TOPIC_PREFIX + "abc123"))
        assertFalse(TripGuideServices.isPlanTopic("splitmate_v2_grp_abc123"))
        assertFalse(TripGuideServices.isPlanTopic("splitmate_v2_idx_9876543210"))
        assertFalse(TripGuideServices.isPlanTopic(null))
    }

    @Test
    fun `shared stay text is trimmed, capped and blank text ignored`() {
        TripGuideServices.offerSharedStayText("   ")
        val before = TripGuideServices.pendingSharedStayText.value
        assertEquals(before, TripGuideServices.pendingSharedStayText.value)
        TripGuideServices.offerSharedStayText("  https://maps.app.goo.gl/AbC  " + "y".repeat(3_000))
        val pending = TripGuideServices.pendingSharedStayText.value!!
        assertTrue(pending.startsWith("https://maps.app.goo.gl/AbC"))
        assertEquals(MapShareTextDetector.MAX_SHARED_CHARS, pending.length)
    }
}
