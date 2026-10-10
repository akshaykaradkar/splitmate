package com.splitmate.app

import com.splitmate.app.data.guide.CommonsImage
import com.splitmate.app.data.guide.DestinationCandidate
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.ui.screens.plan.GuideSection
import com.splitmate.app.ui.screens.plan.PlaceCardUi
import com.splitmate.app.ui.screens.plan.PlanGuideFormat
import com.splitmate.app.ui.screens.plan.SHARE_STAY_PRIVACY_BLURB
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

/**
 * SplitMate v2.3.4 Plan tab (Explore / Loop / sheets) UI compliance suite, stream E2.
 *
 * Part 1 is house-style source inspection of `ui/screens/plan/` and the `TripHomeScreen` PLAN
 * branch: wait-state rules (wavy bar only for in-flight network work, morph hero loader only while
 * the image loads), borderless guide cards, typography roles, attribution, privacy defaults,
 * geometry tokens, theme-role colours, motion tokens and effect handling.
 *
 * Part 2 unit-tests the pure `PlanGuideFormat` helpers (TalkBack copy, distance formatting,
 * coordinate parsing, sanitising, link extraction).
 *
 * Strictly zero Unicode emoji characters appear in this file.
 */
@DisplayName("SplitMate v2.3.4 Plan tab UI compliance (E2)")
class SplitMateV234PlanUiComplianceTest {

    // ---------------------------------------------------------------------------------------------
    // Source helpers
    // ---------------------------------------------------------------------------------------------

    private fun resolveProjectSourceRoot(): File {
        val candidates = listOf(
            File("src/main/java/com/splitmate/app"),
            File("app/src/main/java/com/splitmate/app"),
            File("android/app/src/main/java/com/splitmate/app"),
            File("/usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Unable to locate SplitMate source root from ${File(".").absolutePath}")
    }

    private fun planDir(): File = File(resolveProjectSourceRoot(), "ui/screens/plan").also {
        assertTrue(it.isDirectory, "Expected plan package at ${it.path}")
    }

    private fun plan(name: String): String {
        val file = File(planDir(), name)
        assertTrue(file.exists(), "Expected source file to exist: ${file.path}")
        return file.readText()
    }

    private fun src(relativePath: String): String {
        val file = File(resolveProjectSourceRoot(), relativePath)
        assertTrue(file.exists(), "Expected source file to exist: ${file.path}")
        return file.readText()
    }

    /** All Kotlin sources in the plan package, keyed by file name. */
    private fun planSources(): Map<String, String> =
        planDir().listFiles { f -> f.isFile && f.extension == "kt" }!!
            .sortedBy { it.name }
            .associate { it.name to it.readText() }

    /** Removes block comments and full-line `//` comments (keeps string literals such as URLs intact). */
    private fun stripComments(source: String): String =
        source.replace(Regex("""/\*[\s\S]*?\*/"""), "")
            .replace(Regex("""(?m)^\s*//.*$"""), "")

    private fun allPlanCode(): String = planSources().values.joinToString("\n") { stripComments(it) }

    /** Body of the first function named [name] (brace-balanced, parameter list skipped). */
    private fun functionBody(source: String, name: String): String {
        val match = Regex("""fun\s+$name\s*\(""").find(source) ?: error("Function $name not found")
        var parenDepth = 0
        var paramsEnd = -1
        for (i in match.range.last until source.length) {
            when (source[i]) {
                '(' -> parenDepth++
                ')' -> {
                    parenDepth--
                    if (parenDepth == 0) { paramsEnd = i; break }
                }
            }
        }
        check(paramsEnd > 0) { "Unbalanced parameter list in $name" }
        val open = source.indexOf('{', paramsEnd)
        var depth = 0
        for (i in open until source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(match.range.first, i + 1)
                }
            }
        }
        error("Unbalanced body in $name")
    }

    private fun countOf(haystack: String, needle: String): Int =
        Regex(Regex.escape(needle)).findAll(haystack).count()

    // ---------------------------------------------------------------------------------------------
    // Part 1: source inspection
    // ---------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("Host, switch and wait states")
    inner class HostAndWaitStates {

        @Test
        @DisplayName("E2_01 TripPlanTab keeps the exact signature stream E1 calls")
        fun tripPlanTabSignature() {
            val s = plan("TripPlanTab.kt")
            assertTrue(
                Regex(
                    """fun TripPlanTab\(\s*state: TripGuideUiState,\s*actions: TripGuideActions,\s*""" +
                        """effects: Flow<TripGuideEffect>,\s*bookingsContent: @Composable \(onMakeLoop: \(\(\) -> Unit\)\?\) -> Unit,\s*""" +
                        """modifier: Modifier = Modifier\s*\)"""
                ).containsMatchIn(s),
                "TripPlanTab signature changed"
            )
            assertTrue(s.contains("import kotlinx.coroutines.flow.Flow"), "effects must be a kotlinx Flow")
        }

        @Test
        @DisplayName("E2_02 Switch is the local ConnectedButtonGroup; wavy bar sits directly under it")
        fun switchThenWavyThenContent() {
            val body = stripComments(functionBody(plan("TripPlanTab.kt"), "TripPlanTab"))
            val group = body.indexOf("ConnectedButtonGroup(")
            val wavy = body.indexOf("InFlightWavyProgressIndicator(")
            val content = body.indexOf("Crossfade(")
            assertTrue(group in 0 until wavy, "Wavy bar must follow the switch")
            assertTrue(wavy < content, "Wavy bar must sit above the sub-view content")
            assertTrue(body.contains("actions.selectSubView(view)"))
            assertTrue(body.contains("PlanSubView.BOOKINGS -> bookingsContent("), "Days renders the timeline (v2.4.0: with the Make a loop hook)")
            assertTrue(body.contains("if (state.guideEnabled)"), "Kill-switch must hide Explore and Loop")
        }

        @Test
        @DisplayName("E2_03 Wavy progress only via InFlightWavyProgressIndicator(inFlight = state.networkInFlight)")
        fun wavyOnlyForInFlightNetwork() {
            val code = allPlanCode()
            assertFalse(code.contains("LinearWavyProgressIndicator"), "No raw wavy bars in plan/")
            val calls = countOf(code, "InFlightWavyProgressIndicator(")
            val sanctioned = Regex("""InFlightWavyProgressIndicator\(\s*inFlight = state\.networkInFlight\b""").findAll(code).count()
            assertTrue(calls >= 1, "The Plan tab must show the in-flight bar")
            assertEquals(calls, sanctioned, "Every wavy bar must be driven only by state.networkInFlight")
        }

        @Test
        @DisplayName("E2_04 No CircularProgressIndicator and no dialogs in plan/")
        fun noSpinnersNoDialogs() {
            val code = allPlanCode()
            assertFalse(code.contains("CircularProgressIndicator"))
            assertFalse(code.contains("CircularWavyProgressIndicator"))
            assertFalse(code.contains("AlertDialog"), "Errors are inline tonal banners, never dialogs")
        }

        @Test
        @DisplayName("E2_05 Effects: maps with ActivityNotFoundException fallback, chooser, https URLs, clipboard, share, snackbar")
        fun effectHandling() {
            val s = stripComments(plan("TripPlanTab.kt"))
            listOf(
                "is TripGuideEffect.OpenMaps ->",
                "is TripGuideEffect.OpenMapsChooser ->",
                "is TripGuideEffect.OpenUrl ->",
                "is TripGuideEffect.CopyToClipboard ->",
                "is TripGuideEffect.ShareText ->",
                "is TripGuideEffect.Snackbar ->",
                "catch (_: ActivityNotFoundException)",
                "openHttpsUrl(context, fallbackUrl, snack)",
                "Intent.createChooser(view, chooserTitle)",
                "ClipData.newPlainText",
                "Intent.ACTION_SEND",
                "showSnackbar",
                "PlanGuideFormat.isSafeHttpsUrl(url)"
            ).forEach { assertTrue(s.contains(it), "TripPlanTab effect handling is missing: $it") }
        }

        @Test
        @DisplayName("E2_06 Stay sheet auto-opens on stay feedback; place sheet follows selectedPlace")
        fun sheetsFollowState() {
            val s = stripComments(plan("TripPlanTab.kt"))
            assertTrue(s.contains("state.stayFeedback != StayInputFeedback.Idle"))
            assertTrue(s.contains("StayPinSheet("))
            assertTrue(s.contains("state.selectedPlace"))
            assertTrue(s.contains("PlaceDetailSheet("))
            assertTrue(s.contains("actions.dismissPlace()"))
        }
    }

    @Nested
    @DisplayName("Explore surface")
    inner class ExploreSurface {

        @Test
        @DisplayName("E2_07 Hero: 24dp 16:9 canvas, GuideHeroPolygons loader only while Coil is loading")
        fun heroLoader() {
            val s = stripComments(plan("ExploreGuideView.kt"))
            val hero = functionBody(s, "GuideHero")
            assertTrue(hero.contains("RoundedCornerShape(24.dp)"))
            assertTrue(hero.contains("9f / 16f"), "Hero keeps a 16:9 minimum")
            assertTrue(hero.contains("AsyncImagePainter.State.Loading"))
            assertTrue(hero.contains("if (imageLoading)"), "Loader must be gated on the image loading state")
            val loader = hero.substring(hero.indexOf("ContainedLoadingIndicator("))
            assertTrue(loader.contains("polygons = LoadingIndicatorDefaults.GuideHeroPolygons"))
            assertTrue(loader.contains("contentDescription = \"Loading guide\""))
            assertTrue(hero.contains("Brush.verticalGradient"), "Bottom scrim keeps the title above 4.5:1")
            assertTrue(hero.contains("onState = { imageState = it }"))
        }

        @Test
        @DisplayName("E2_08 Typography: displaySmall title, headlineLarge headers")
        fun typographyRoles() {
            val explore = stripComments(plan("ExploreGuideView.kt"))
            val components = stripComments(plan("PlanGuideComponents.kt"))
            val loop = stripComments(plan("DayLoopView.kt"))
            assertTrue(functionBody(explore, "AutoShrinkTitle").contains("MaterialTheme.typography.displaySmall"))
            assertTrue(functionBody(explore, "AutoShrinkTitle").contains("maxLines = 2"))
            assertTrue(functionBody(components, "PlanSectionHeader").contains("MaterialTheme.typography.headlineLarge"))
            assertTrue(explore.contains("PlanSectionHeader("))
            assertTrue(explore.contains("\"Where is this trip headed?\""))
            assertTrue(loop.contains("\"Today's loop from your stay\""))
            assertTrue(loop.contains("MaterialTheme.typography.headlineLarge"))
        }

        @Test
        @DisplayName("E2_09 Attribution footer is always rendered from state.attribution")
        fun attributionAlways() {
            val explore = stripComments(plan("ExploreGuideView.kt"))
            val body = functionBody(explore, "ExploreGuideView")
            val footer = body.indexOf("item(key = \"attribution\")")
            assertTrue(footer > 0, "Footer item missing")
            assertTrue(body.substring(footer).contains("lines = state.attribution"))
            // The footer item is the last item and is not nested in a phase branch.
            assertTrue(footer > body.lastIndexOf("hydratedGuide("))
            val components = stripComments(plan("PlanGuideComponents.kt"))
            assertTrue(functionBody(components, "AttributionFooter").contains("CC BY-SA 4.0"))
        }

        @Test
        @DisplayName("E2_10 Guide cards are borderless ElevatedCards on surfaceContainerLowest")
        fun borderlessCards() {
            val code = allPlanCode()
            assertFalse(code.contains("BorderStroke"), "No BorderStroke in plan/")
            assertFalse(code.contains(".border("), "No Modifier.border in plan/")
            assertFalse(Regex("""\bborder\s*=""").containsMatchIn(code), "No border parameters in plan/")
            var from = 0
            var cards = 0
            while (true) {
                val at = code.indexOf("ElevatedCard(", from)
                if (at < 0) break
                cards++
                val window = code.substring(at, minOf(code.length, at + 700))
                assertTrue(window.contains("surfaceContainerLowest"), "ElevatedCard must use the white card role")
                assertTrue(window.contains("PlanGuideDefaults.CardShape"), "ElevatedCard must use the 20dp card shape")
                from = at + 1
            }
            assertTrue(cards >= 1)
            assertTrue(stripComments(plan("PlanGuideComponents.kt")).contains("RoundedCornerShape(20.dp)"))
        }

        @Test
        @DisplayName("E2_11 Progressive disclosure, spacedBy(12.dp), inline Retry banners, offline chip, update banner")
        fun exploreStates() {
            val s = stripComments(plan("ExploreGuideView.kt"))
            assertTrue(s.contains("section.places.take(limit)"))
            assertTrue(s.contains("PlanGuideFormat.showMoreLabel(hiddenCount)"))
            assertTrue(stripComments(plan("PlanGuideComponents.kt")).contains("val CardSpacing: Dp = 12.dp"))
            assertTrue(s.contains("Arrangement.spacedBy(PlanGuideDefaults.CardSpacing)"))
            assertTrue(s.contains("actionLabel = \"Retry\""))
            assertTrue(s.contains("actions.retry()"))
            assertTrue(s.contains("OfflineStatusChip("))
            assertTrue(s.contains("actions.acceptGuideUpdate()"))
            assertTrue(s.contains("No curated guide yet. Showing nearby landmarks from Wikidata."))
            assertTrue(s.contains("PlanGuideFormat.placeA11yLabel(name, card.distanceLabel, sectionTitle)"))
        }

        @Test
        @DisplayName("E2_12 Directions uses the slot-based SplitButtonLayout with the full menu")
        fun directionsSplitButton() {
            val body = stripComments(functionBody(plan("PlanGuideComponents.kt"), "DirectionsSplitButton"))
            listOf(
                "SplitButtonLayout(",
                "leadingButton =",
                "trailingButton =",
                "SplitButtonDefaults.LeadingButton(",
                "SplitButtonDefaults.TrailingButton(",
                "menuExpanded = menuOpen",
                "\"Walk\"",
                "\"Drive\"",
                "\"Open in another app\"",
                "\"Copy coordinates\"",
                "\"Share to group\"",
                "actions.directions(place)",
                "TravelMode.WALKING",
                "TravelMode.DRIVING"
            ).forEach { assertTrue(body.contains(it), "Directions split button missing: $it") }
            assertFalse(allPlanCode().contains("leadingText ="), "Legacy non-slot SplitButtonLayout must not be used")
        }
    }

    @Nested
    @DisplayName("Tokens, privacy and integration")
    inner class TokensPrivacyIntegration {

        @Test
        @DisplayName("E2_13 Geometry: POI thumbnails use GuideShapeTokens.PlaceThumbnail; avatars stay Cookie9Sided")
        fun geometryTokens() {
            val code = allPlanCode()
            assertTrue(code.contains("GuideShapeTokens.PlaceThumbnail"))
            assertFalse(code.contains("RoundedCornerShape(16.dp)"), "16dp squircles come from the token only")
            if (code.contains("Avatar")) {
                assertTrue(code.contains("MemberAvatarPolygon"), "Member avatars must use Cookie9Sided")
            }
        }

        @Test
        @DisplayName("E2_14 No hard-coded hex colours or insecure URLs in plan/")
        fun themeRolesAndHttps() {
            val code = allPlanCode()
            assertFalse(Regex("""Color\(\s*0x""").containsMatchIn(code), "Use MaterialTheme roles, not hex")
            assertFalse(Regex("""0x[Ff]{2}[0-9A-Fa-f]{6}""").containsMatchIn(code))
            assertFalse(code.contains("http://"), "HTTPS only")
        }

        @Test
        @DisplayName("E2_15 Motion goes through LocalMotionScheme: springs, press scale 0.98, no tween")
        fun motionTokens() {
            val code = allPlanCode()
            assertFalse(code.contains("tween("), "Springs via LocalMotionScheme, not tween")
            assertTrue(code.contains("LocalMotionScheme.current"))
            assertTrue(code.contains("animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)"))
            assertTrue(code.contains("animateContentSize(motion.defaultSpatialSpec())"))
            assertTrue(code.contains("const val PressedScale: Float = 0.98f"))
            assertTrue(code.contains("motion.fastSpatialSpec()"))
        }

        @Test
        @DisplayName("E2_16 Share stay defaults OFF, with the privacy blurb; Nominatim only on tap, with OSM credit")
        fun stayPrivacy() {
            val contract = plan("TripGuideContract.kt")
            assertTrue(contract.contains("val stay: StayUi = StayUi(null, false, false, null, emptyList())"))
            val sheet = stripComments(plan("StayPinSheet.kt"))
            assertTrue(sheet.contains("checked = stayUi.shareWithGroup"))
            assertTrue(sheet.contains("Switch(checked = checked, onCheckedChange = null)"))
            assertTrue(sheet.contains("role = Role.Switch"))
            assertEquals("Visible to everyone in this trip. Off by default.", SHARE_STAY_PRIVACY_BLURB)
            val code = allPlanCode()
            assertFalse(code.contains("shareWithGroup = true"))
            assertFalse(code.contains("setShareStay(true)"))
            assertEquals(1, countOf(sheet, "actions.searchStayByName("), "Nominatim runs only from the Search by name tap")
            assertTrue(sheet.contains("NominatimGeocoder.ATTRIBUTION"))
            assertTrue(sheet.contains("PlanGuideFormat.stayPreviewLine("))
            assertTrue(sheet.contains("actions.clearStay()"))
        }

        @Test
        @DisplayName("E2_17 Images: Coil with identified User-Agent, crossfade, https only; no emoji in plan/")
        fun imagesAndNoEmoji() {
            val components = stripComments(plan("PlanGuideComponents.kt"))
            val request = functionBody(components, "rememberGuideImageRequest")
            assertTrue(request.contains("setHeader(\"User-Agent\", GuideHttp.USER_AGENT)"))
            assertTrue(request.contains("crossfade(true)"))
            val code = allPlanCode()
            assertTrue(code.contains("AsyncImage("))
            assertFalse(code.contains("Image(painter"), "Images load through Coil only")
            val emoji = planSources().flatMap { (name, text) ->
                text.codePoints().toArray().filter { cp -> cp in 0x1F300..0x1FAFF || cp in 0x2600..0x27BF }
                    .map { name to it }
            }
            assertTrue(emoji.isEmpty(), "Emoji found in plan/: $emoji")
        }

        @Test
        @DisplayName("E2_18 TripHomeScreen PLAN branch calls TripPlanTabHost; FAB hidden off Bookings")
        fun tripHomeIntegration() {
            val home = src("ui/screens/TripHomeScreen.kt")
            val planStart = home.indexOf("TripHubSectionTab.PLAN -> {")
            val planEnd = home.indexOf("TripHubSectionTab.MONEY -> {", planStart)
            assertTrue(planStart > 0 && planEnd > planStart, "PLAN branch not found")
            val branch = home.substring(planStart, planEnd)
            assertTrue(branch.contains("TripPlanTabHost("))
            assertTrue(branch.contains("groupId = resolvedGroupId"))
            assertTrue(branch.contains("bookingsContent = { onMakeLoop ->"))
            assertTrue(branch.indexOf("TripHubPlanTimelineView(") > branch.indexOf("bookingsContent = { onMakeLoop ->"))
            assertTrue(branch.contains("onSubViewChanged = { planSubView = it }"))

            val fabStart = home.indexOf("floatingActionButton = {")
            val fabEnd = home.indexOf("FloatingActionButtonMenu(", fabStart)
            assertTrue(fabStart > 0 && fabEnd > fabStart)
            val fabGuard = home.substring(fabStart, fabEnd)
            assertTrue(fabGuard.contains("planSubView != PlanSubView.BOOKINGS"), "FAB must hide on Explore/Loop")
            assertTrue(home.contains("var planSubView by remember(resolvedGroupId) { mutableStateOf(PlanSubView.BOOKINGS) }"))
        }

        @Test
        @DisplayName("E2_19 TripPlanTabHost keeps the exact signature E1 implements")
        fun tripPlanTabHostSignature() {
            val s = plan("TripPlanTabHost.kt")
            assertTrue(
                Regex(
                    """fun TripPlanTabHost\(\s*groupId: String,\s*bookingsContent: @Composable \(onMakeLoop: \(\(\) -> Unit\)\?\) -> Unit,\s*""" +
                        """onSubViewChanged: \(PlanSubView\) -> Unit,\s*modifier: Modifier = Modifier\s*\)"""
                ).containsMatchIn(s)
            )
        }

        @Test
        @DisplayName("E2_20 Loop: stepper back to stay, ordered TalkBack labels, one primary Maps action, no-stay CTA")
        fun loopSurface() {
            val s = stripComments(plan("DayLoopView.kt"))
            assertTrue(s.contains("item(key = \"stay_start\")"))
            assertTrue(s.contains("item(key = \"stay_end\")"))
            assertTrue(s.contains("route.legStraightKm.getOrNull(stops.size)"), "Return leg is shown")
            assertTrue(s.contains("PlanGuideFormat.stepA11yLabel(index, total, name, place.kind, legKm)"))
            assertTrue(s.contains("actions.openWholeLoopInMaps()"))
            assertTrue(s.contains("actions.reoptimizeLoop()"))
            assertTrue(s.contains("actions.shareLoop()"))
            assertTrue(s.contains("\"Set your stay\""))
            assertTrue(s.contains("loop.stopsWithoutCoordinates"))
            assertEquals(1, countOf(s, "Button(\n            onClick = { actions.openWholeLoopInMaps() }"), "One primary action")
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Part 2: PlanGuideFormat behaviour
    // ---------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("PlanGuideFormat")
    inner class Format {

        private fun place(id: String, kind: PlaceKind) =
            Place(id = id, kind = kind, name = id, location = null, source = PlaceSource.WIKIVOYAGE)

        private fun section(kind: PlaceKind, n: Int) =
            GuideSection(kind, PlanGuideFormat.kindLabel(kind), (1..n).map { PlaceCardUi(place("$kind$it", kind), null, false, false, null) })

        @Test
        @DisplayName("F_01 Card TalkBack label matches the audit example")
        fun cardLabel() {
            assertEquals(
                "Virupaksha Temple, 1.2 kilometres, Must See",
                PlanGuideFormat.placeA11yLabel("Virupaksha Temple", "1.2 km ≈", "Must See")
            )
            assertEquals("Hampi Bazaar", PlanGuideFormat.placeA11yLabel("Hampi Bazaar", null, " "))
        }

        @Test
        @DisplayName("F_02 Spoken distances: metres, singular, blank")
        fun spokenDistance() {
            assertEquals("850 metres", PlanGuideFormat.spokenDistance("850 m"))
            assertEquals("1 kilometre", PlanGuideFormat.spokenDistance("1 km"))
            assertEquals("14.2 kilometres", PlanGuideFormat.spokenDistance("~14.2km"))
            assertNull(PlanGuideFormat.spokenDistance(null))
            assertNull(PlanGuideFormat.spokenDistance("  "))
        }

        @Test
        @DisplayName("F_03 formatKm rounds metres to 10 and km to one decimal; invalid input is blank")
        fun formatKm() {
            assertEquals("850 m", PlanGuideFormat.formatKm(0.853))
            assertEquals("10 m", PlanGuideFormat.formatKm(0.001))
            assertEquals("1.2 km", PlanGuideFormat.formatKm(1.234))
            assertEquals("14.2 km", PlanGuideFormat.formatKm(14.24))
            assertEquals("", PlanGuideFormat.formatKm(Double.NaN))
            assertEquals("", PlanGuideFormat.formatKm(-1.0))
            assertEquals("", PlanGuideFormat.formatKm(Double.POSITIVE_INFINITY))
        }

        @Test
        @DisplayName("F_04 parseCoordinates accepts decimal degree pairs and rejects everything else")
        fun parseCoordinates() {
            assertEquals(LatLng(15.335, 76.46), PlanGuideFormat.parseCoordinates("15.3350, 76.4600"))
            assertEquals(LatLng(15.335, 76.46), PlanGuideFormat.parseCoordinates("15.335 76.46"))
            assertEquals(LatLng(-33.8688, 151.2093), PlanGuideFormat.parseCoordinates(" (-33.8688,151.2093) "))
            assertEquals(LatLng(12.9, 77.6), PlanGuideFormat.parseCoordinates("12.9;77.6"))
            assertNull(PlanGuideFormat.parseCoordinates("91, 10"))
            assertNull(PlanGuideFormat.parseCoordinates("15.3, 181"))
            assertNull(PlanGuideFormat.parseCoordinates("15.3"))
            assertNull(PlanGuideFormat.parseCoordinates("Hampi"))
            assertNull(PlanGuideFormat.parseCoordinates(""))
            assertNull(PlanGuideFormat.parseCoordinates("15.3, 76.4, 3"))
        }

        @Test
        @DisplayName("F_05 Stay preview line shows 4-decimal coordinates and precision")
        fun previewLine() {
            assertEquals("15.3350, 76.4600 · Exact", PlanGuideFormat.stayPreviewLine(LatLng(15.335, 76.46), false))
            assertEquals("15.3350, 76.4600 · Approximate", PlanGuideFormat.stayPreviewLine(LatLng(15.335, 76.46), true))
        }

        @Test
        @DisplayName("F_06 sanitizeDisplay strips markup and control characters and never returns raw JSON")
        fun sanitize() {
            assertEquals("Temple & tank", PlanGuideFormat.sanitizeDisplay("<b>Temple</b> &amp; tank"))
            assertEquals("a b c", PlanGuideFormat.sanitizeDisplay("a\u0000b\nc"))
            assertNull(PlanGuideFormat.sanitizeDisplay("{\"error\":\"rate limited\"}"))
            assertNull(PlanGuideFormat.sanitizeDisplay("[\"x\",\"y\"]"))
            assertNull(PlanGuideFormat.sanitizeDisplay("   "))
            assertNull(PlanGuideFormat.sanitizeDisplay(null))
            assertEquals("[citation needed]", PlanGuideFormat.sanitizeDisplay("[citation needed]"))
        }

        @Test
        @DisplayName("F_07 https links: trailing punctuation dropped, http ignored, host labels")
        fun links() {
            val line = "Text: https://en.wikivoyage.org/w/index.php?oldid=1. (licence: https://creativecommons.org/licenses/by-sa/4.0/)"
            val links = PlanGuideFormat.httpsLinks(line)
            assertEquals(
                listOf("https://en.wikivoyage.org/w/index.php?oldid=1", "https://creativecommons.org/licenses/by-sa/4.0/"),
                links.map { it.url }
            )
            links.forEach { assertEquals(it.url, line.substring(it.start, it.end)) }
            assertTrue(PlanGuideFormat.httpsLinks("see http://insecure.example.org").isEmpty())
            assertEquals("wikidata.org", PlanGuideFormat.displayUrl("https://www.wikidata.org/wiki/Q1"))
        }

        @Test
        @DisplayName("F_08 Attribution segments replace URLs with their host")
        fun segments() {
            val segs = PlanGuideFormat.attributionSegments("Destination data from Wikidata (Q1), CC0 1.0: https://www.wikidata.org/wiki/Q1")
            assertEquals(2, segs.size)
            assertEquals("Destination data from Wikidata (Q1), CC0 1.0: ", segs[0].text)
            assertNull(segs[0].url)
            assertEquals("wikidata.org", segs[1].text)
            assertEquals("https://www.wikidata.org/wiki/Q1", segs[1].url)
            assertEquals(listOf(PlanGuideFormat.AttributionSegment("plain", null)), PlanGuideFormat.attributionSegments("plain"))
        }

        @Test
        @DisplayName("F_09 isSafeHttpsUrl only accepts clean https URLs")
        fun safeUrl() {
            assertTrue(PlanGuideFormat.isSafeHttpsUrl("https://maps.google.com/?q=15.3,76.4"))
            assertFalse(PlanGuideFormat.isSafeHttpsUrl("http://maps.google.com"))
            assertFalse(PlanGuideFormat.isSafeHttpsUrl("javascript:alert(1)"))
            assertFalse(PlanGuideFormat.isSafeHttpsUrl("https://a.org/x y"))
            assertFalse(PlanGuideFormat.isSafeHttpsUrl("https://"))
            assertFalse(PlanGuideFormat.isSafeHttpsUrl(null))
        }

        @Test
        @DisplayName("F_10 Hero subtitle: description plus counts in See, Eat, Do order")
        fun heroSubtitle() {
            val sections = listOf(section(PlaceKind.DO, 1), section(PlaceKind.SEE, 12), section(PlaceKind.EAT, 8))
            assertEquals(
                "Village in Karnataka · 12 must-sees · 8 eats · 1 thing to do",
                PlanGuideFormat.heroSubtitle("village in Karnataka", sections)
            )
            assertEquals("1 must-see", PlanGuideFormat.heroSubtitle(null, listOf(section(PlaceKind.SEE, 1))))
            assertNull(PlanGuideFormat.heroSubtitle(" ", emptyList()))
        }

        @Test
        @DisplayName("F_11 Loop steps are announced in order with the leg distance")
        fun stepLabels() {
            assertEquals(
                "Stop 1 of 3: Virupaksha Temple, Must See, 1.1 kilometres from your stay",
                PlanGuideFormat.stepA11yLabel(1, 3, "Virupaksha Temple", PlaceKind.SEE, 1.1)
            )
            assertEquals(
                "Stop 2 of 3: Mango Tree, Local Eats, 800 metres from the previous stop",
                PlanGuideFormat.stepA11yLabel(2, 3, "Mango Tree", PlaceKind.EAT, 0.8)
            )
            assertEquals("Stop 3 of 3: Coracle ride, Things to Do", PlanGuideFormat.stepA11yLabel(3, 3, "Coracle ride", PlaceKind.DO, null))
            assertEquals("Back to your stay, 500 metres", PlanGuideFormat.returnA11yLabel(0.5))
            assertEquals("Back to your stay", PlanGuideFormat.returnA11yLabel(null))
        }

        @Test
        @DisplayName("F_12 Disambiguation chip text truncates long descriptions")
        fun candidateLabel() {
            val c = DestinationCandidate("Q1", "Hampi", "village in the Vijayanagara district of Karnataka, India, on the Tungabhadra", null, null, null, 1.0)
            val label = PlanGuideFormat.candidateLabel(c)
            assertTrue(label.startsWith("Hampi · village in"))
            assertTrue(label.endsWith("…"))
            assertTrue(label.length <= "Hampi · ".length + 48)
            assertEquals("Hampi", PlanGuideFormat.candidateLabel(c.copy(description = null)))
        }

        @Test
        @DisplayName("F_13 Commons credit is sanitised; source and kind labels")
        fun creditsAndLabels() {
            val img = CommonsImage("Hampi.jpg", "https://upload.wikimedia.org/x.jpg", 640, "<a href=\"u\">Jane Doe</a>", "CC BY-SA 4.0", null, null)
            assertEquals("Photo by Jane Doe · CC BY-SA 4.0 · Wikimedia Commons", PlanGuideFormat.imageCredit(img))
            assertEquals("Photo · Wikimedia Commons", PlanGuideFormat.imageCredit(img.copy(artist = null, license = null)))
            assertEquals("Source: Wikivoyage · CC BY-SA 4.0", PlanGuideFormat.sourceLine(place("a", PlaceKind.SEE)))
            assertEquals("Source: Wikidata · CC0 1.0", PlanGuideFormat.sourceLine(place("b", PlaceKind.SEE).copy(source = PlaceSource.WIKIDATA)))
            assertEquals("Added by your group", PlanGuideFormat.sourceLine(place("c", PlaceKind.SEE).copy(source = PlaceSource.USER)))
            assertEquals(
                listOf("Must See", "Things to Do", "Local Eats", "Sleep"),
                PlaceKind.entries.map { PlanGuideFormat.kindLabel(it) }
            )
        }

        @Test
        @DisplayName("F_14 Excerpt heuristic, truncate and show-more copy")
        fun excerptAndTruncate() {
            assertTrue(PlanGuideFormat.isExcerptHeuristic("x".repeat(279)))
            assertTrue(PlanGuideFormat.isExcerptHeuristic("Built in the 7th century…"))
            assertFalse(PlanGuideFormat.isExcerptHeuristic("Short."))
            assertFalse(PlanGuideFormat.isExcerptHeuristic(null))
            assertEquals("abcd…", PlanGuideFormat.truncate("abcdefgh", 5))
            assertEquals("abc", PlanGuideFormat.truncate("abc", 5))
            assertEquals("Show 9 more", PlanGuideFormat.showMoreLabel(9))
            assertNotNull(PlanGuideFormat.coordinateLabel(LatLng(0.0, 0.0)))
        }
    }
}
