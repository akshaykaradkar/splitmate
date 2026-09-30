@file:OptIn(ExperimentalLayoutApi::class)

package com.splitmate.app.ui.screens.plan

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.splitmate.app.data.guide.CommonsImage
import com.splitmate.app.data.guide.DestinationCandidate
import com.splitmate.app.data.guide.GuideHttp
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.data.guide.nav.TravelMode
import com.splitmate.app.ui.components.GuideShapeTokens
import com.splitmate.app.ui.components.LocalMotionScheme
import com.splitmate.app.ui.components.SplitButtonDefaults
import com.splitmate.app.ui.components.SplitButtonLayout
import java.util.Locale
import kotlin.math.roundToInt

// =================================================================================================
// Pure formatting helpers (no Compose types, so they are JVM unit-testable).
// =================================================================================================

/**
 * Display and accessibility copy for the Plan tab. Every string that reaches the screen from
 * external sources (Wikivoyage, Wikidata, Commons, pasted links) passes through [sanitizeDisplay],
 * so markup, control characters and raw JSON never render.
 */
object PlanGuideFormat {

    /** A tappable https link inside a line of text: `[start, end)` in the original string. */
    data class LinkSpan(val start: Int, val end: Int, val url: String)

    /** One run of an attribution line; [url] is non-null for a link run showing [text]. */
    data class AttributionSegment(val text: String, val url: String?)

    private val HTTPS_URL = Regex("""https://[^\s<>"()\[\]{}]+""")
    private val HTML_TAG = Regex("<[^>]*>")
    private val WHITESPACE = Regex("\\s+")
    private val KM = Regex("""(\d+(?:\.\d+)?)\s*km\b""")
    private val METRES = Regex("""(\d+(?:\.\d+)?)\s*m\b""")
    private val COORDINATES =
        Regex("""^([+-]?\d{1,2}(?:\.\d+)?)(?:\s*[,;]\s*|\s+)([+-]?\d{1,3}(?:\.\d+)?)$""")

    /** Editorial category label (matches the section titles). */
    fun kindLabel(kind: PlaceKind): String = when (kind) {
        PlaceKind.SEE -> "Must See"
        PlaceKind.DO -> "Things to Do"
        PlaceKind.EAT -> "Local Eats"
        PlaceKind.SLEEP -> "Sleep"
    }

    /** "850 m" below 1 km (rounded to 10 m), otherwise "1.2 km". Invalid input gives "". */
    fun formatKm(km: Double): String {
        if (km.isNaN() || km.isInfinite() || km < 0.0) return ""
        return if (km < 1.0) {
            val metres = ((km * 1000.0) / 10.0).roundToInt() * 10
            "${metres.coerceAtLeast(10)} m"
        } else {
            String.format(Locale.US, "%.1f km", km)
        }
    }

    /** TalkBack wording for a distance label: "1.2 km ≈" -> "1.2 kilometres". */
    fun spokenDistance(label: String?): String? {
        if (label.isNullOrBlank()) return null
        var s = label.replace("≈", " ").replace("~", " ")
        s = KM.replace(s) { m ->
            val n = m.groupValues[1]
            if (n == "1") "1 kilometre" else "$n kilometres"
        }
        s = METRES.replace(s) { m ->
            val n = m.groupValues[1]
            if (n == "1") "1 metre" else "$n metres"
        }
        return s.replace(WHITESPACE, " ").trim().ifBlank { null }
    }

    /** Card label, e.g. "Virupaksha Temple, 1.2 kilometres, Must See". */
    fun placeA11yLabel(name: String, distanceLabel: String?, sectionTitle: String?): String =
        listOfNotNull(
            name.trim().ifBlank { null },
            spokenDistance(distanceLabel),
            sectionTitle?.trim()?.ifBlank { null }
        ).joinToString(", ")

    /** "15.3350, 76.4600" (Locale.US, 4 decimals is about 11 m). */
    fun coordinateLabel(location: LatLng): String =
        String.format(Locale.US, "%.4f, %.4f", location.lat, location.lng)

    fun precisionLabel(approximate: Boolean): String = if (approximate) "Approximate" else "Exact"

    /** Stay preview line: "15.3350, 76.4600 · Exact". */
    fun stayPreviewLine(location: LatLng, approximate: Boolean): String =
        "${coordinateLabel(location)} · ${precisionLabel(approximate)}"

    /**
     * Parses "lat, lng" / "lat lng" / "(lat,lng)" decimal degrees. Returns null for anything else or
     * out-of-range values. Used only to enable the "Use coordinates" button; the ViewModel parses again.
     */
    fun parseCoordinates(text: String): LatLng? {
        val cleaned = text.trim().removePrefix("(").removeSuffix(")").trim()
        val m = COORDINATES.matchEntire(cleaned) ?: return null
        val lat = m.groupValues[1].toDoubleOrNull() ?: return null
        val lng = m.groupValues[2].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        return LatLng(lat, lng)
    }

    /**
     * Makes external text safe to show: strips HTML tags, decodes a few common entities, replaces
     * control characters, collapses whitespace. Returns null for blank text or anything that looks
     * like raw JSON (never render raw JSON to users).
     */
    fun sanitizeDisplay(text: String?): String? {
        if (text == null) return null
        var s = text.replace(HTML_TAG, " ")
        s = s.replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
        s = buildString(s.length) { s.forEach { ch -> append(if (ch.isISOControl()) ' ' else ch) } }
        s = s.replace(WHITESPACE, " ").trim()
        if (s.isEmpty()) return null
        if (looksLikeJson(s)) return null
        return s
    }

    private fun looksLikeJson(s: String): Boolean =
        (s.startsWith("{") && s.endsWith("}")) ||
            (s.startsWith("[") && s.endsWith("]") && s.contains("\""))

    /** True for a well-formed https URL (no whitespace or control characters, bounded length). */
    fun isSafeHttpsUrl(url: String?): Boolean =
        url != null &&
            url.startsWith("https://") &&
            url.length in 9..2048 &&
            url.none { it.isWhitespace() || it.isISOControl() }

    /** All https links in [text]; trailing sentence punctuation is not part of a link. */
    fun httpsLinks(text: String): List<LinkSpan> =
        HTTPS_URL.findAll(text).mapNotNull { m ->
            var url = m.value
            while (url.isNotEmpty() && url.last() in ".,;:!?'") url = url.dropLast(1)
            if (url.length <= "https://".length) null
            else LinkSpan(m.range.first, m.range.first + url.length, url)
        }.toList()

    /** Short, readable label for a link: its host without "www.", e.g. "wikidata.org". */
    fun displayUrl(url: String): String =
        url.removePrefix("https://")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .removePrefix("www.")
            .lowercase(Locale.US)

    /** Splits an attribution line into text and link runs; links show their host (see [displayUrl]). */
    fun attributionSegments(line: String): List<AttributionSegment> {
        val links = httpsLinks(line)
        if (links.isEmpty()) return listOf(AttributionSegment(line, null))
        val out = ArrayList<AttributionSegment>()
        var cursor = 0
        links.forEach { link ->
            if (link.start > cursor) out += AttributionSegment(line.substring(cursor, link.start), null)
            out += AttributionSegment(displayUrl(link.url), link.url)
            cursor = link.end
        }
        if (cursor < line.length) out += AttributionSegment(line.substring(cursor), null)
        return out
    }

    /** Hero subtitle: "Karnataka · 12 must-sees · 8 eats · 5 things to do". */
    fun heroSubtitle(description: String?, sections: List<GuideSection>): String? {
        val parts = ArrayList<String>()
        sanitizeDisplay(description)?.let { d -> parts += d.replaceFirstChar { it.uppercaseChar() } }
        val counts = sections.groupBy { it.kind }.mapValues { (_, list) -> list.sumOf { it.places.size } }
        listOf(PlaceKind.SEE, PlaceKind.EAT, PlaceKind.DO).forEach { kind ->
            val n = counts[kind] ?: 0
            if (n > 0) parts += "$n ${countNoun(kind, n)}"
        }
        return parts.joinToString(" · ").ifBlank { null }
    }

    private fun countNoun(kind: PlaceKind, n: Int): String = when (kind) {
        PlaceKind.SEE -> if (n == 1) "must-see" else "must-sees"
        PlaceKind.EAT -> if (n == 1) "eat" else "eats"
        PlaceKind.DO -> if (n == 1) "thing to do" else "things to do"
        PlaceKind.SLEEP -> if (n == 1) "stay" else "stays"
    }

    /** Disambiguation chip text: "Hampi · village in Karnataka" (description capped at 48 chars). */
    fun candidateLabel(candidate: DestinationCandidate): String {
        val label = sanitizeDisplay(candidate.label) ?: candidate.qid
        val desc = sanitizeDisplay(candidate.description)?.let { truncate(it, 48) }
        return if (desc == null) label else "$label · $desc"
    }

    /** Loop step label, announced in order: "Stop 1 of 6: Virupaksha Temple, Must See, 1.1 kilometres from your stay". */
    fun stepA11yLabel(index: Int, total: Int, name: String, kind: PlaceKind, legKm: Double?): String {
        val from = if (index == 1) "your stay" else "the previous stop"
        val distance = legKm?.let { spokenDistance(formatKm(it)) }
        val tail = if (distance != null) ", $distance from $from" else ""
        return "Stop $index of $total: $name, ${kindLabel(kind)}$tail"
    }

    fun returnA11yLabel(legKm: Double?): String {
        val distance = legKm?.let { spokenDistance(formatKm(it)) }
        return if (distance != null) "Back to your stay, $distance" else "Back to your stay"
    }

    fun showMoreLabel(hiddenCount: Int): String = "Show $hiddenCount more"

    /** Source and licence line for the place sheet. */
    fun sourceLine(place: Place): String = when (place.source) {
        PlaceSource.WIKIVOYAGE -> "Source: Wikivoyage · CC BY-SA 4.0"
        PlaceSource.WIKIDATA -> "Source: Wikidata · CC0 1.0"
        PlaceSource.USER -> "Added by your group"
    }

    /** "Photo by Jane Doe · CC BY-SA 4.0 · Wikimedia Commons". */
    fun imageCredit(image: CommonsImage): String {
        val artist = sanitizeDisplay(image.artist)?.let { truncate(it, 60) }
        val head = if (artist != null) "Photo by $artist" else "Photo"
        return listOfNotNull(head, sanitizeDisplay(image.license), "Wikimedia Commons").joinToString(" · ")
    }

    /** Fallback excerpt detection when no [PlaceCardUi] is available (handoff note 5). */
    fun isExcerptHeuristic(blurb: String?): Boolean =
        blurb != null && (blurb.length >= 279 || blurb.endsWith("…"))

    fun truncate(text: String, max: Int): String =
        if (text.length <= max) text else text.take((max - 1).coerceAtLeast(1)).trimEnd() + "…"
}

// =================================================================================================
// Shared composables
// =================================================================================================

/** Plan-tab geometry and motion tokens. */
internal object PlanGuideDefaults {
    val CardShape = RoundedCornerShape(20.dp)
    val HeroShape = RoundedCornerShape(24.dp)
    val CardSpacing: Dp = 12.dp
    val ScreenGutter: Dp = 16.dp
    val CardElevation: Dp = 1.dp
    val CardPressedElevation: Dp = 2.dp
    const val PressedScale: Float = 0.98f
    const val HeroScrimAlpha: Float = 0.78f
}

/** Press feedback: scale to 0.98 with the fast spatial spring (snaps under reduced motion). */
@Composable
internal fun rememberPressScale(interactionSource: InteractionSource): State<Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    val motion = LocalMotionScheme.current
    return animateFloatAsState(
        targetValue = if (pressed) PlanGuideDefaults.PressedScale else 1f,
        animationSpec = motion.fastSpatialSpec(),
        label = "PlanPressScale"
    )
}

/** Coil request for Wikimedia images: identified User-Agent, https only, crossfade. */
@Composable
internal fun rememberGuideImageRequest(url: String): ImageRequest {
    val context = LocalContext.current
    return remember(url, context) {
        ImageRequest.Builder(context)
            .data(url)
            .setHeader("User-Agent", GuideHttp.USER_AGENT)
            .crossfade(true)
            .build()
    }
}

/** Semantic category accent as a small tonal chip (SEE olive, EAT terracotta, DO periwinkle). */
@Composable
fun PlaceKindChip(kind: PlaceKind, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (kind) {
        PlaceKind.SEE -> scheme.primaryContainer to scheme.onPrimaryContainer
        PlaceKind.EAT -> scheme.secondaryContainer to scheme.onSecondaryContainer
        PlaceKind.DO -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        PlaceKind.SLEEP -> scheme.surfaceContainerHigh to scheme.onSurfaceVariant
    }
    Surface(shape = CircleShape, color = container, contentColor = content, modifier = modifier) {
        Text(
            text = PlanGuideFormat.kindLabel(kind),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Quiet provenance label ("Excerpt", "From Wikidata"). */
@Composable
fun PlanMetaLabel(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Editorial section header ("Must See", "Things to Do", "Local Eats"). */
@Composable
fun PlanSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.semantics { heading() }
    )
}

enum class PlanBannerTone { INFO, WARNING, ERROR, NEUTRAL }

/** Inline tonal banner (partial/error/update/offline notes). Never a dialog. */
@Composable
fun PlanInlineBanner(
    message: String,
    tone: PlanBannerTone,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (tone) {
        PlanBannerTone.INFO -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        PlanBannerTone.WARNING -> scheme.secondaryContainer to scheme.onSecondaryContainer
        PlanBannerTone.ERROR -> scheme.errorContainer to scheme.onErrorContainer
        PlanBannerTone.NEUTRAL -> scheme.surfaceContainerHigh to scheme.onSurfaceVariant
    }
    Surface(
        shape = PlanGuideDefaults.CardShape,
        color = container,
        contentColor = content,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    colors = ButtonDefaults.textButtonColors(contentColor = content)
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * The single primary action on a place: slot-based [SplitButtonLayout] (audit 5.8).
 * Leading "Directions" fires the `geo:` intent; the menu holds Walk, Drive, another app, copy
 * coordinates and share to group.
 */
@Composable
fun DirectionsSplitButton(
    place: Place,
    actions: TripGuideActions,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false
) {
    var menuOpen by remember { mutableStateOf(false) }
    val height = if (fillWidth) SplitButtonDefaults.MediumContainerHeight else SplitButtonDefaults.ContainerHeight
    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = { actions.directions(place) },
                height = height,
                modifier = Modifier.semantics { contentDescription = "Directions to ${place.name}" }
            ) {
                Icon(imageVector = Icons.Rounded.NearMe, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Directions",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        },
        trailingButton = {
            SplitButtonDefaults.TrailingButton(
                checked = menuOpen,
                onCheckedChange = { menuOpen = it },
                height = height,
                content = {
                    SplitButtonDefaults.TrailingIcon(
                        checked = menuOpen,
                        contentDescription = "More ways to get to ${place.name}"
                    )
                }
            )
        },
        modifier = modifier,
        fillWidth = fillWidth,
        menuExpanded = menuOpen,
        onMenuDismissRequest = { menuOpen = false },
        menuContent = {
            DirectionsMenuItem("Walk", Icons.AutoMirrored.Rounded.DirectionsWalk) {
                menuOpen = false
                actions.navigate(place, TravelMode.WALKING)
            }
            DirectionsMenuItem("Drive", Icons.Rounded.DirectionsCar) {
                menuOpen = false
                actions.navigate(place, TravelMode.DRIVING)
            }
            DirectionsMenuItem("Open in another app", Icons.AutoMirrored.Rounded.OpenInNew) {
                menuOpen = false
                actions.openInOtherApp(place)
            }
            DirectionsMenuItem("Copy coordinates", Icons.Rounded.ContentCopy) {
                menuOpen = false
                actions.copyCoordinates(place)
            }
            DirectionsMenuItem("Share to group", Icons.Rounded.Share) {
                menuOpen = false
                actions.sharePlace(place)
            }
        }
    )
}

@Composable
private fun DirectionsMenuItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = { Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp)
    )
}

/** Tonal secondary action: "Add to loop" / "In loop" (pinned). */
@Composable
fun AddToLoopToggle(pinned: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val colors = if (pinned) {
        ButtonDefaults.filledTonalButtonColors(
            containerColor = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer
        )
    } else {
        ButtonDefaults.filledTonalButtonColors(
            containerColor = scheme.surfaceContainerHigh,
            contentColor = scheme.onSurface
        )
    }
    FilledTonalButton(
        onClick = onToggle,
        colors = colors,
        modifier = modifier
            .heightIn(min = 40.dp)
            .semantics { stateDescription = if (pinned) "In loop" else "Not in loop" }
    ) {
        Icon(
            imageVector = if (pinned) Icons.Rounded.Check else Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (pinned) "In loop" else "Add to loop",
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}

/** POI thumbnail: 16dp squircle (GuideShapeTokens.PlaceThumbnail), Commons image via Coil. */
@Composable
fun PlaceThumbnail(image: CommonsImage, size: Dp, modifier: Modifier = Modifier) {
    val url = image.thumbUrl.takeIf { PlanGuideFormat.isSafeHttpsUrl(it) } ?: return
    AsyncImage(
        model = rememberGuideImageRequest(url),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(GuideShapeTokens.PlaceThumbnail)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    )
}

/** Non-interactive offline status chip ("Offline · guide saved on 29 Sep"). */
@Composable
fun OfflineStatusChip(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Static tonal placeholder block for skeleton states (no shimmer: calm, and reduced-motion safe). */
@Composable
fun SkeletonBlock(widthFraction: Float, height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction.coerceIn(0.05f, 1f))
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    )
}

/**
 * Sources and credits, always rendered on Explore (CC BY-SA 4.0 requires it). Links show their
 * host and open in the browser (https only). TalkBack gets one custom action per link.
 */
@Composable
fun AttributionFooter(lines: List<String>, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val open: (String) -> Unit = { url ->
        if (PlanGuideFormat.isSafeHttpsUrl(url)) runCatching { uriHandler.openUri(url) }
    }
    val safeLines = remember(lines) { lines.mapNotNull { PlanGuideFormat.sanitizeDisplay(it) } }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PlanGuideDefaults.ScreenGutter, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Sources & credits",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        if (safeLines.isEmpty()) {
            AttributionLine(
                line = "Guide text from Wikivoyage, CC BY-SA 4.0: $CC_BY_SA_URL · " +
                    "Place data from Wikidata, CC0 1.0: $CC0_URL",
                onOpen = open
            )
        } else {
            safeLines.forEach { line -> AttributionLine(line = line, onOpen = open) }
        }
    }
}

private const val CC_BY_SA_URL = "https://creativecommons.org/licenses/by-sa/4.0/"
private const val CC0_URL = "https://creativecommons.org/publicdomain/zero/1.0/"

@Composable
private fun AttributionLine(line: String, onOpen: (String) -> Unit) {
    val segments = remember(line) { PlanGuideFormat.attributionSegments(line) }
    val linkColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val annotated = remember(segments, linkColor) {
        buildAnnotatedString {
            segments.forEach { seg ->
                if (seg.url == null) {
                    append(seg.text)
                } else {
                    val start = length
                    append(seg.text)
                    addStyle(
                        SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                        start,
                        length
                    )
                    addStringAnnotation(tag = LINK_TAG, annotation = seg.url, start = start, end = length)
                }
            }
        }
    }
    val links = segments.mapNotNull { it.url }
    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodySmall.copy(color = textColor),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                if (links.isNotEmpty()) {
                    customActions = links.map { url ->
                        CustomAccessibilityAction(label = "Open ${PlanGuideFormat.displayUrl(url)}") {
                            onOpen(url)
                            true
                        }
                    }
                }
            },
        onClick = { offset ->
            annotated.getStringAnnotations(tag = LINK_TAG, start = offset, end = offset)
                .firstOrNull()
                ?.let { onOpen(it.item) }
        }
    )
}

private const val LINK_TAG = "plan_link"

/** A label/value row for the place sheet (hours, price, address). */
@Composable
fun PlanDetailRow(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Kind chip plus provenance labels in a wrapping row. */
@Composable
fun PlaceLabelsRow(
    kind: PlaceKind,
    isExcerpt: Boolean,
    sourceLabel: String?,
    modifier: Modifier = Modifier,
    distanceLabel: String? = null
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PlaceKindChip(kind)
        if (distanceLabel != null) PlanMetaLabel(distanceLabel)
        if (isExcerpt) PlanMetaLabel("Excerpt")
        if (sourceLabel != null) PlanMetaLabel(sourceLabel)
    }
}

/** Hidden-from-TalkBack decorative container (leg connectors, skeletons). */
internal fun Modifier.planDecorative(): Modifier = this.clearAndSetSemantics { }

/** Overflow-safe single-line text helper for chips. */
@Composable
internal fun ChipText(text: String, color: Color = Color.Unspecified) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
