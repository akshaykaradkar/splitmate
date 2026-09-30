package com.splitmate.app.guide.content

import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.data.guide.content.ParsedArticle
import com.splitmate.app.data.guide.content.ParsedListing
import com.splitmate.app.data.guide.content.WikiTextSanitizer
import com.splitmate.app.data.guide.content.WikivoyageClient
import com.splitmate.app.data.guide.content.WikivoyageListingParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class WikivoyageListingParserTest {

    private fun hampi(): ParsedArticle =
        WikivoyageListingParser.parse("Hampi", 4500101L, Fixtures.text("wikivoyage_hampi.wikitext"))

    private fun ParsedArticle.byId(id: String): ParsedListing =
        listings.firstOrNull { it.place.id == id } ?: throw AssertionError("no listing $id in ${listings.map { it.place.id }}")

    private fun one(wikitext: String): List<ParsedListing> = WikivoyageListingParser.parse("T", 1L, wikitext).listings

    @Nested
    inner class HampiFixture {

        @Test
        fun `extracts every listing in document order with stable ids`() {
            val a = hampi()
            assertEquals("Hampi", a.title)
            assertEquals(4500101L, a.revisionId)
            assertEquals(
                listOf(
                    "wv:see:virupaksha-temple", "wv:see:hemakuta-hill", "wv:see:sasivekalu-ganesha",
                    "wv:see:lotus-mahal", "wv:see:elephant-stables", "wv:see:vittala-temple",
                    "wv:see:hampi-archaeological-museum", "wv:see:virupaksha-temple-2",
                    "wv:do:coracle-ride", "wv:do:bouldering-at-hampi",
                    "wv:eat:mango-tree", "wv:eat:laughing-buddha", "wv:eat:gopi-rooftop", "wv:eat:chai-point-hampi",
                    "wv:sleep:evolve-back-kamalapura-palace", "wv:sleep:hampi-s-boulders", "wv:sleep:gopi-guest-house"
                ),
                a.listings.map { it.place.id }
            )
            assertFalse(a.isOutline)
            assertEquals(17, a.places.size)
        }

        @Test
        fun `kind counts - SLEEP retained - go and buy ignored`() {
            val kinds = hampi().places.groupingBy { it.kind }.eachCount()
            assertEquals(8, kinds.getValue(PlaceKind.SEE))
            assertEquals(2, kinds.getValue(PlaceKind.DO))
            assertEquals(4, kinds.getValue(PlaceKind.EAT))
            assertEquals(3, kinds.getValue(PlaceKind.SLEEP))
            val names = hampi().places.map { it.name }
            assertFalse(names.contains("Hospet Junction"), "type=go must be skipped")
            assertFalse(names.contains("Hampi Bazaar stalls"), "{{buy}} must be skipped")
            assertFalse(names.contains("Souvenir shop"), "untyped listing in Buy section must be skipped")
            assertTrue(hampi().places.all { it.source == PlaceSource.WIKIVOYAGE })
        }

        @Test
        fun `multi-line listing with all params`() {
            val v = hampi().byId("wv:see:virupaksha-temple")
            assertEquals(PlaceKind.SEE, v.place.kind)
            assertEquals("Virupaksha Temple", v.place.name)
            assertEquals(LatLng(15.335, 76.46), v.place.location)
            assertEquals("Q9000111", v.place.qid)
            assertEquals("6AM–1PM, 5PM–9PM", v.place.hours)
            assertEquals("Free; camera ₹50", v.place.price)
            assertEquals("Hampi Bazaar", v.place.address)
            assertEquals("Pampapathi Temple", v.alt)
            assertEquals("Virupaksha Temple, Hampi.jpg", v.imageFile)
            assertEquals("See", v.section)
            assertEquals("see", v.template)
            assertEquals(
                "The oldest functioning temple in Hampi, dedicated to Lord Shiva. Its 50-metre gopuram dominates the bazaar. Temple elephant Lakshmi blesses visitors in the morning.",
                v.place.blurb
            )
            assertFalse(v.blurbTruncated)
        }

        @Test
        fun `nested km template - comment and bullet prefix across lines`() {
            val h = hampi().byId("wv:see:hemakuta-hill")
            assertEquals("Cluster of early temples; the best sunset spot near the bazaar, 0.5 km south of Virupaksha.", h.place.blurb)
            assertEquals("Hemakuta hill temples.jpg", h.imageFile)
            assertEquals(LatLng(15.3342, 76.4585), h.place.location)
        }

        @Test
        fun `file link with nested link caption is removed`() {
            assertEquals("A 2.4-m monolithic Ganesha; see nearby.", hampi().byId("wv:see:sasivekalu-ganesha").place.blurb)
        }

        @Test
        fun `piped anchor link inside price does not split params`() {
            val l = hampi().byId("wv:see:lotus-mahal")
            assertEquals("Indians ₹40, foreigners ₹600 (combined ticket with Vittala Temple)", l.place.price)
            assertEquals("Kamal Mahal", l.alt)
            assertEquals("8AM–5:30PM", l.place.hours)
            assertEquals("An elegant two-storey pavilion mixing Hindu and Islamic styles, inside the Zenana Enclosure.", l.place.blurb)
        }

        @Test
        fun `long blurb truncated to 280 with flag - markup stripped - lowercase qid normalised`() {
            val v = hampi().byId("wv:see:vittala-temple")
            val blurb = v.place.blurb!!
            assertTrue(v.blurbTruncated)
            assertTrue(blurb.length <= 280, "len=${blurb.length}")
            assertTrue(blurb.endsWith("…"))
            assertTrue(blurb.startsWith("Home to the iconic stone chariot and the musical pillars."))
            assertTrue(blurb.contains("ವಿಠ್ಠಲ"), "lang template renders its text")
            assertFalse(blurb.contains("'''") || blurb.contains("[[") || blurb.contains("{{") || blurb.contains("<!--"))
            assertEquals("Q9000112", v.place.qid)
        }

        @Test
        fun `empty lat and long give null location`() {
            val m = hampi().byId("wv:see:hampi-archaeological-museum")
            assertNull(m.place.location)
            assertEquals("10AM–5PM, closed Fridays", m.place.hours)
        }

        @Test
        fun `duplicate names get deterministic suffix`() {
            val d = hampi().byId("wv:see:virupaksha-temple-2")
            assertEquals("Virupaksha Temple", d.place.name)
            assertNull(d.place.location)
            assertEquals(hampi().places.map { it.id }, hampi().places.map { it.id }, "ids stable across parses")
        }

        @Test
        fun `empty alt is null and spaced heading is recognised`() {
            val c = hampi().byId("wv:do:coracle-ride")
            assertNull(c.alt)
            assertEquals("Do", c.section)
            assertEquals("₹100 per person", c.place.price)
            assertEquals("Ride a round parisal basket boat on the Tungabhadra.", c.place.blurb)
            val b = hampi().byId("wv:do:bouldering-at-hampi")
            assertEquals("listing", b.template)
            assertEquals(PlaceKind.DO, b.place.kind)
        }

        @Test
        fun `html entities and tags and br and script are sanitised`() {
            assertEquals("A Hampi institution with river views; thali recommended. Cash only & no alcohol.",
                hampi().byId("wv:eat:mango-tree").place.blurb)
            assertEquals("Chill café across the river at Virupapur Gaddi. Reach by ferry.",
                hampi().byId("wv:eat:laughing-buddha").place.blurb)
            val gopi = hampi().byId("wv:eat:gopi-rooftop").place.blurb!!
            assertEquals("Multi-cuisine rooftop.", gopi)
            assertFalse(gopi.contains("alert"))
        }

        @Test
        fun `drink maps to EAT`() {
            val c = hampi().byId("wv:eat:chai-point-hampi")
            assertEquals(PlaceKind.EAT, c.place.kind)
            assertEquals("drink", c.template)
            assertEquals("Drink", c.section)
        }

        @Test
        fun `sleep listings keep address and price and File-prefixed image - subsection inherits kind`() {
            val e = hampi().byId("wv:sleep:evolve-back-kamalapura-palace")
            assertEquals("Evolve Back Hampi.jpg", e.imageFile)
            assertEquals("₹25,000+", e.place.price)
            assertEquals("Kamalapura", e.place.address)
            val g = hampi().byId("wv:sleep:gopi-guest-house")
            assertEquals(PlaceKind.SLEEP, g.place.kind)
            assertEquals("Sleep", g.section, "level-3 'Budget' inherits level-2 'Sleep'")
            assertFalse(hampi().places.any { it.blurb == "Nameless listing is skipped." })
        }

        @Test
        fun `headings collected in order`() {
            assertEquals(
                listOf("Understand", "Get in", "See", "Hampi Bazaar", "Royal Centre", "Vittala area",
                    "Do", "Buy", "Eat", "Drink", "Sleep", "Budget", "Stay safe"),
                hampi().headings
            )
        }
    }

    @Nested
    inner class OtherFixtures {

        @Test
        fun `gokarna - two listings on one line - spaced coords - listing type see - missing content`() {
            val a = WikivoyageListingParser.parse("Gokarna", 1L, Fixtures.text("wikivoyage_gokarna.wikitext"))
            assertEquals(
                listOf("wv:see:mahabaleshwar-temple", "wv:see:om-beach", "wv:see:kudle-beach", "wv:see:half-moon-beach",
                    "wv:see:paradise-beach", "wv:do:beach-trek", "wv:eat:namaste-cafe", "wv:sleep:swaswara", "wv:sleep:zostel-gokarna"),
                a.places.map { it.id }
            )
            assertEquals(LatLng(14.5196, 74.3242), a.byId("wv:see:om-beach").place.location)
            assertEquals("Houses the Atmalinga; men must remove shirts. Dress code applies.", a.byId("wv:see:mahabaleshwar-temple").place.blurb)
            assertEquals("Reachable only on foot or by boat from Om Beach.", a.byId("wv:see:half-moon-beach").place.blurb)
            assertNull(a.byId("wv:see:paradise-beach").place.blurb)
            assertEquals("Namaste Café", a.byId("wv:eat:namaste-cafe").place.name)
            assertEquals(2, a.places.count { it.kind == PlaceKind.SLEEP })
        }

        @Test
        fun `ratnagiri - prose-only Eat section yields no fabricated listing`() {
            val a = WikivoyageListingParser.parse("Ratnagiri", 2L, Fixtures.text("wikivoyage_ratnagiri.wikitext"))
            assertEquals(listOf("wv:see:ratnadurg-fort", "wv:see:thibaw-palace", "wv:sleep:mtdc-ganpatipule"), a.places.map { it.id })
            assertEquals(0, a.places.count { it.kind == PlaceKind.EAT })
        }

        @Test
        fun `outline article from action parse JSON has no listings`() {
            val article = WikivoyageClient.parseResponse(Fixtures.text("wikivoyage_parse_outline_dapoli.json"), "Dapoli")!!
            assertEquals("Dapoli", article.title)
            assertEquals(4500301L, article.revisionId)
            val a = WikivoyageListingParser.parse(article.title, article.revisionId, article.wikitext)
            assertTrue(a.isOutline)
            assertTrue(a.places.isEmpty())
            assertEquals(listOf("Understand", "See", "Eat", "Sleep"), a.headings)
        }

        @Test
        fun `missing title response maps to null`() {
            assertNull(WikivoyageClient.parseResponse(Fixtures.text("wikivoyage_parse_missing.json"), "Nowhere"))
        }

        @Test
        fun `formatversion 1 wikitext star object is accepted`() {
            val v1 = """{"parse":{"title":"X","revid":7,"wikitext":{"*":"==See==\n{{see|name=Y}}"}}}"""
            val art = WikivoyageClient.parseResponse(v1, "X")!!
            assertEquals(7L, art.revisionId)
            assertEquals("wv:see:y", WikivoyageListingParser.parse(art.title, art.revisionId, art.wikitext).places.single().id)
        }
    }

    @Nested
    inner class EdgeCases {

        @Test
        fun `listing type overrides section - untyped listing inherits section`() {
            val l = one("==Eat==\n{{listing|type=see|name=Typed}}\n{{listing|name=Untyped}}\n{{listing|type=weird|name=Odd}}")
            assertEquals(listOf(PlaceKind.SEE, PlaceKind.EAT, PlaceKind.EAT), l.map { it.place.kind })
        }

        @Test
        fun `listing type buy and go are skipped`() {
            assertTrue(one("==See==\n{{listing|type=buy|name=A}}\n{{listing|type=go|name=B}}").isEmpty())
        }

        @Test
        fun `non-listing level-2 heading resets inherited kind`() {
            assertTrue(one("==See==\n==Understand==\n===Sub===\n{{listing|name=X}}").isEmpty())
        }

        @Test
        fun `template name is case- and space-insensitive and sleep under See stays SLEEP`() {
            val l = one("==See==\n{{ See | name = Spaced Name }}\n{{sleep|name=Inn}}")
            assertEquals("Spaced Name", l[0].place.name)
            assertEquals(PlaceKind.SEE, l[0].place.kind)
            assertEquals(PlaceKind.SLEEP, l[1].place.kind)
        }

        @Test
        fun `coordinate edge cases`() {
            fun loc(lat: String, long: String) = one("{{see|name=P|lat=$lat|long=$long}}").single().place.location
            assertNull(loc("abc", "76"))
            assertNull(loc("91", "76"))
            assertNull(loc("15", "181"))
            assertNull(loc("0", "0"))
            assertEquals(LatLng(-33.8, 151.2), loc("-33.8", "151.2"))
            assertEquals(LatLng(-33.8, 151.2), loc("−33.8", "151.2"))
            assertEquals(LatLng(15.1, 76.2), loc("15.1<!-- approx -->", "76.2"))
            assertNull(one("{{see|name=P|lat=15.1}}").single().place.location)
            assertNull(WikivoyageListingParser.parseLatLng(null, "1"))
        }

        @Test
        fun `unbalanced trailing template is salvaged`() {
            val l = one("==See==\n{{see|name=Broken|content=no closing braces")
            assertEquals("Broken", l.single().place.name)
            assertEquals("no closing braces", l.single().place.blurb)
        }

        @Test
        fun `pipe escape template and diacritics in name`() {
            val l = one("{{see|name=Fort {{!}} Museum}}\n{{eat|name=Café Île}}")
            assertEquals("Fort | Museum", l[0].place.name)
            assertEquals("wv:see:fort-museum", l[0].place.id)
            assertEquals("wv:eat:cafe-ile", l[1].place.id)
        }

        @Test
        fun `non-latin name falls back to deterministic hash slug`() {
            val a = one("{{see|name=ವಿಠ್ಠಲ}}").single().place.id
            val b = one("{{see|name=ವಿಠ್ಠಲ}}").single().place.id
            assertTrue(a.startsWith("wv:see:listing-"))
            assertEquals(a, b)
        }

        @Test
        fun `convert template - plural link trail and description alias`() {
            val l = one("{{do|name=Walk|description=A {{convert|3|km}} walk past [[temple]]s and [[Foo|baz]].}}").single()
            assertEquals("A 3 km walk past temples and baz.", l.place.blurb)
        }

        @Test
        fun `heading-like line inside a multi-line template is not a heading`() {
            val l = one("==Eat==\n{{listing\n|name=Multi\n|content=Line one\n==Not a heading==\nline two\n}}")
            assertEquals(PlaceKind.EAT, l.single().place.kind)
            assertTrue(l.single().place.blurb!!.contains("Line one"))
        }

        @Test
        fun `blurb of exactly 280 chars is not truncated but 281 is`() {
            val s280 = "a".repeat(280)
            val s281 = "a".repeat(281)
            val p280 = one("{{see|name=A|content=$s280}}").single()
            val p281 = one("{{see|name=B|content=$s281}}").single()
            assertFalse(p280.blurbTruncated)
            assertEquals(280, p280.place.blurb!!.length)
            assertTrue(p281.blurbTruncated)
            assertTrue(p281.place.blurb!!.length <= 280)
        }

        @Test
        fun `invalid wikidata and templated image are dropped`() {
            val l = one("{{see|name=A|wikidata=P31|image={{PAGENAME}}.jpg}}").single()
            assertNull(l.place.qid)
            assertNull(l.imageFile)
        }

        @Test
        fun `self-closing and named refs are removed without eating text`() {
            val l = one("{{see|name=A|content=Before<ref name=\"a/b\">note</ref> middle<ref name=x /> after.}}").single()
            assertEquals("Before middle after.", l.place.blurb)
        }

        @Test
        fun `empty and garbage input`() {
            assertTrue(one("").isEmpty())
            assertTrue(one("}} {{ [[ ]] == ==").isEmpty())
            assertTrue(one("{{see}}").isEmpty(), "listing without name is skipped")
        }
    }

    @Nested
    inner class Sanitizer {

        @Test
        fun `plain text conversions`() {
            assertEquals("bold italic", WikiTextSanitizer.toPlainText("'''bold''' ''italic''"))
            assertEquals("label", WikiTextSanitizer.toPlainText("[https://example.org label]"))
            assertEquals("", WikiTextSanitizer.toPlainText("[https://example.org]"))
            assertEquals("Target", WikiTextSanitizer.toPlainText("[[Target]]"))
            assertEquals("", WikiTextSanitizer.toPlainText("[[Category:Hampi]]"))
            assertEquals("x y", WikiTextSanitizer.toPlainText("x &lt; y"), "decoded angle brackets are stripped")
            assertEquals("A–B", WikiTextSanitizer.toPlainText("A&ndash;B"))
            assertEquals("é", WikiTextSanitizer.toPlainText("&#233;"))
            assertEquals("é", WikiTextSanitizer.toPlainText("&#xE9;"))
            assertEquals("a b", WikiTextSanitizer.toPlainText("a\n\n\tb"))
            assertEquals("", WikiTextSanitizer.toPlainText(null))
        }

        @Test
        fun `entity-encoded markup never survives as tags`() {
            val out = WikiTextSanitizer.toPlainText("&lt;script&gt;alert(1)&lt;/script&gt; ok")
            assertFalse(out.contains("<") || out.contains(">"))
            assertTrue(out.endsWith("ok"))
        }

        @Test
        fun `unterminated comment runs to end`() {
            assertEquals("keep", WikiTextSanitizer.toPlainText("keep<!-- dropped forever"))
        }

        @Test
        fun `truncate and slug`() {
            val t = WikiTextSanitizer.truncate("word ".repeat(100).trim(), 50)
            assertTrue(t.truncated)
            assertTrue(t.text.length <= 50)
            assertTrue(t.text.endsWith("word…"))
            assertEquals("hampi-s-boulders", WikiTextSanitizer.slugify("Hampi's Boulders"))
            assertEquals("a-b", WikiTextSanitizer.slugify("  A & B  "))
            assertEquals(60, WikiTextSanitizer.slugify("x".repeat(100)).length)
        }

        @Test
        fun `lang and w templates render text`() {
            assertEquals("ಹಂಪೆ", WikiTextSanitizer.toPlainText("{{lang|kn|ಹಂಪೆ}}"))
            assertEquals("Label", WikiTextSanitizer.toPlainText("{{w|Target|Label}}"))
            assertEquals("", WikiTextSanitizer.toPlainText("{{citation needed|date=2024}}"))
            assertNotNull(WikiTextSanitizer.toPlainText("{{unclosed"))
        }
    }
}
