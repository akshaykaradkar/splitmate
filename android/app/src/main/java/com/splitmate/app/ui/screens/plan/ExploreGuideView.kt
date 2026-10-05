@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.splitmate.app.ui.screens.plan

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.EditLocationAlt
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.splitmate.app.data.guide.CommonsImage
import com.splitmate.app.data.guide.GuidePack
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.ui.components.ContainedLoadingIndicator
import com.splitmate.app.ui.components.LoadingIndicatorDefaults
import com.splitmate.app.ui.components.LocalMotionScheme

/**
 * Explore sub-view (audit 5.2 A-C): search and suggestions, the loading canvas, then the hydrated
 * editorial guide. Attribution is always rendered at the bottom.
 */
@Composable
fun ExploreGuideView(
    state: TripGuideUiState,
    actions: TripGuideActions,
    onOpenStaySheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val phase = state.phase
    val pack = state.pack
    val motion = LocalMotionScheme.current
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    val expandedSections = remember { mutableStateMapOf<PlaceKind, Boolean>() }

    val hasGuide = pack != null && when (phase) {
        GuidePhase.Ready, is GuidePhase.Partial, is GuidePhase.Error -> true
        else -> false
    }
    val showSearch = searchOpen || when (phase) {
        GuidePhase.Empty, GuidePhase.Resolving, is GuidePhase.Disambiguate -> true
        is GuidePhase.Error -> pack == null
        else -> false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(PlanGuideDefaults.CardSpacing)
    ) {
        val offlineLabel = state.offlineSavedLabel
        if (state.isOffline && offlineLabel != null) {
            item(key = "offline") {
                OfflineStatusChip(
                    label = offlineLabel,
                    icon = Icons.Rounded.CloudOff,
                    modifier = Modifier
                        .padding(horizontal = PlanGuideDefaults.ScreenGutter)
                        .animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                )
            }
        }
        if (state.guideUpdateAvailable && hasGuide) {
            item(key = "guide_updated") {
                PlanInlineBanner(
                    message = "Guide updated. A newer version of this guide is available.",
                    tone = PlanBannerTone.INFO,
                    icon = Icons.Rounded.NewReleases,
                    actionLabel = "Update",
                    onAction = { actions.acceptGuideUpdate() },
                    modifier = Modifier
                        .padding(horizontal = PlanGuideDefaults.ScreenGutter)
                        .animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                )
            }
        }

        if (showSearch) {
            item(key = "search") {
                DestinationSearch(
                    state = state,
                    actions = actions,
                    onClose = if (searchOpen && hasGuide) ({ searchOpen = false }) else null,
                    onSubmitted = { searchOpen = false },
                    modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                )
            }
            if (phase is GuidePhase.Disambiguate) {
                item(key = "disambiguate") {
                    DisambiguationChips(
                        phase = phase,
                        actions = actions,
                        onChosen = { searchOpen = false },
                        modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                    )
                }
            }
            if (phase is GuidePhase.Error && pack == null) {
                item(key = "error") {
                    PhaseErrorBanner(phase, actions, Modifier.animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null))
                }
            }
            if (phase == GuidePhase.Resolving) {
                item(key = "resolving_skeleton") {
                    HeroSkeleton(
                        label = "Finding your destination",
                        modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                    )
                }
            }
        }

        when {
            phase is GuidePhase.Hydrating && !searchOpen -> {
                val sameDestination = pack != null && pack.destination.qid == phase.candidate.qid
                item(key = "hero") {
                    GuideHero(
                        title = PlanGuideFormat.sanitizeDisplay(phase.candidate.label) ?: phase.candidate.label,
                        subtitle = PlanGuideFormat.sanitizeDisplay(phase.candidate.description),
                        hero = if (sameDestination) pack?.destination?.hero else null,
                        showPlaceholderIcon = false,
                        modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                    )
                }
                item(key = "hydrating_skeleton") {
                    SectionSkeletons(modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null))
                }
            }
            hasGuide && pack != null && !searchOpen -> {
                hydratedGuide(
                    state = state,
                    pack = pack,
                    actions = actions,
                    expandedSections = expandedSections,
                    onOpenStaySheet = onOpenStaySheet,
                    onChangeDestination = { searchOpen = true }
                )
            }
        }

        item(key = "attribution") {
            AttributionFooter(
                lines = state.attribution,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Hydrated guide
// -------------------------------------------------------------------------------------------------

private fun LazyListScope.hydratedGuide(
    state: TripGuideUiState,
    pack: GuidePack,
    actions: TripGuideActions,
    expandedSections: MutableMap<PlaceKind, Boolean>,
    onOpenStaySheet: () -> Unit,
    onChangeDestination: () -> Unit
) {
    val phase = state.phase
    item(key = "hero") {
        val motion = LocalMotionScheme.current
        GuideHero(
            title = PlanGuideFormat.sanitizeDisplay(pack.destination.label) ?: pack.destination.qid,
            subtitle = PlanGuideFormat.heroSubtitle(pack.destination.description, state.sections),
            hero = pack.destination.hero,
            showPlaceholderIcon = true,
            modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
        )
    }
    item(key = "hero_chips") {
        val motion = LocalMotionScheme.current
        GuideChipsRow(
            state = state,
            actions = actions,
            onOpenStaySheet = onOpenStaySheet,
            onChangeDestination = onChangeDestination,
            modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
        )
    }
    if (phase is GuidePhase.Partial) {
        item(key = "partial") {
            val motion = LocalMotionScheme.current
            PlanInlineBanner(
                message = PlanGuideFormat.sanitizeDisplay(phase.message) ?: "Some parts of the guide didn't load.",
                tone = PlanBannerTone.WARNING,
                icon = Icons.Rounded.WarningAmber,
                actionLabel = "Retry",
                onAction = { actions.retry() },
                modifier = Modifier
                    .padding(horizontal = PlanGuideDefaults.ScreenGutter)
                    .animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
            )
        }
    }
    if (phase is GuidePhase.Error) {
        item(key = "error") {
            val motion = LocalMotionScheme.current
            PhaseErrorBanner(phase, actions, Modifier.animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null))
        }
    }
    if (pack.wikivoyage == null || pack.places.isEmpty()) {
        item(key = "no_curated") {
            val motion = LocalMotionScheme.current
            PlanInlineBanner(
                message = "No curated guide yet. Showing nearby landmarks from Wikidata.",
                tone = PlanBannerTone.NEUTRAL,
                icon = Icons.Rounded.Info,
                modifier = Modifier
                    .padding(horizontal = PlanGuideDefaults.ScreenGutter)
                    .animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
            )
        }
    }

    val sections = state.sections.filter { it.places.isNotEmpty() }
    if (sections.isEmpty()) {
        item(key = "sections_empty") {
            Text(
                text = "Nearby landmarks will appear here once they load.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = PlanGuideDefaults.ScreenGutter, vertical = 8.dp)
            )
        }
    }
    sections.forEach { section ->
        val sectionKey = section.kind.name
        val expanded = expandedSections[section.kind] == true
        val limit = section.initiallyVisible.coerceAtLeast(1)
        val visible = if (expanded) section.places else section.places.take(limit)
        val hiddenCount = (section.places.size - limit).coerceAtLeast(0)
        val title = PlanGuideFormat.sanitizeDisplay(section.title) ?: PlanGuideFormat.kindLabel(section.kind)

        item(key = "header_$sectionKey") {
            val motion = LocalMotionScheme.current
            PlanSectionHeader(
                title = title,
                modifier = Modifier
                    .padding(start = PlanGuideDefaults.ScreenGutter, end = PlanGuideDefaults.ScreenGutter, top = 16.dp)
                    .animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
            )
        }
        visible.forEach { card ->
            item(key = "place_${sectionKey}_${card.place.id}") {
                val motion = LocalMotionScheme.current
                PlaceCard(
                    card = card,
                    sectionTitle = title,
                    actions = actions,
                    modifier = Modifier
                        .padding(horizontal = PlanGuideDefaults.ScreenGutter)
                        .animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                )
            }
        }
        if (hiddenCount > 0) {
            item(key = "more_$sectionKey") {
                val motion = LocalMotionScheme.current
                TextButton(
                    onClick = { expandedSections[section.kind] = !expanded },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .heightIn(min = 48.dp)
                        .animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                ) {
                    Text(
                        text = if (expanded) "Show less" else PlanGuideFormat.showMoreLabel(hiddenCount),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Borderless white ElevatedCard: one primary action (Directions), tonal "Add to loop". */
@Composable
private fun PlaceCard(
    card: PlaceCardUi,
    sectionTitle: String,
    actions: TripGuideActions,
    modifier: Modifier = Modifier
) {
    val place = card.place
    val motion = LocalMotionScheme.current
    val interaction = remember { MutableInteractionSource() }
    val scale by rememberPressScale(interaction)
    val name = PlanGuideFormat.sanitizeDisplay(place.name) ?: place.name
    val blurb = PlanGuideFormat.sanitizeDisplay(place.blurb)
    val a11yLabel = PlanGuideFormat.placeA11yLabel(name, card.distanceLabel, sectionTitle)

    ElevatedCard(
        onClick = { actions.openPlace(place) },
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = PlanGuideDefaults.CardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = PlanGuideDefaults.CardElevation,
            pressedElevation = PlanGuideDefaults.CardPressedElevation
        ),
        interactionSource = interaction
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(motion.defaultSpatialSpec())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { contentDescription = a11yLabel },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    card.distanceLabel?.let { distance ->
                        Text(
                            text = distance,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (blurb != null) {
                        Text(
                            text = blurb,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    PlaceLabelsRow(kind = place.kind, isExcerpt = card.isExcerpt, sourceLabel = card.sourceLabel)
                }
                place.image?.let { image -> PlaceThumbnail(image = image, size = 76.dp) }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (place.location != null) {
                    DirectionsSplitButton(place = place, actions = actions)
                }
                AddToLoopToggle(pinned = card.isPinned, onToggle = { actions.togglePin(place.id) })
            }
        }
    }
}

@Composable
private fun GuideChipsRow(
    state: TripGuideUiState,
    actions: TripGuideActions,
    onOpenStaySheet: () -> Unit,
    onChangeDestination: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipColors = AssistChipDefaults.elevatedAssistChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    )
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PlanGuideDefaults.ScreenGutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val stayLabel = state.stay.stay?.label?.let { PlanGuideFormat.sanitizeDisplay(it) }
            ?.let { PlanGuideFormat.truncate(it, 24) }
        ElevatedAssistChip(
            onClick = onOpenStaySheet,
            label = { ChipText(if (stayLabel != null) "Stay: $stayLabel" else "Stay: not set") },
            leadingIcon = { Icon(Icons.Rounded.Hotel, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = { Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp)) },
            colors = chipColors,
            elevation = AssistChipDefaults.elevatedAssistChipElevation(elevation = 1.dp)
        )
        val size = state.saveOfflineSizeLabel
        if (state.saveOfflineEnabled || size != null) {
            ElevatedAssistChip(
                onClick = { actions.setSaveOffline(!state.saveOfflineEnabled) },
                label = {
                    ChipText(
                        when {
                            state.saveOfflineEnabled -> "Saved offline"
                            size != null -> "Save offline $size"
                            else -> "Save offline"
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (state.saveOfflineEnabled) Icons.Rounded.CheckCircle else Icons.Rounded.DownloadForOffline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = chipColors,
                elevation = AssistChipDefaults.elevatedAssistChipElevation(elevation = 1.dp)
            )
        }
        ElevatedAssistChip(
            onClick = onChangeDestination,
            label = { ChipText("Change place") },
            leadingIcon = { Icon(Icons.Rounded.EditLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp)) },
            colors = chipColors,
            elevation = AssistChipDefaults.elevatedAssistChipElevation(elevation = 1.dp)
        )
    }
}

@Composable
private fun PhaseErrorBanner(phase: GuidePhase.Error, actions: TripGuideActions, modifier: Modifier = Modifier) {
    PlanInlineBanner(
        message = PlanGuideFormat.sanitizeDisplay(phase.message) ?: "The guide couldn't load.",
        tone = PlanBannerTone.ERROR,
        icon = Icons.Rounded.ErrorOutline,
        actionLabel = if (phase.retryable) "Retry" else null,
        onAction = if (phase.retryable) ({ actions.retry() }) else null,
        modifier = modifier.padding(horizontal = PlanGuideDefaults.ScreenGutter)
    )
}

// -------------------------------------------------------------------------------------------------
// Hero
// -------------------------------------------------------------------------------------------------

/**
 * 24dp hero with a 16:9 minimum height that grows with the text (200% font scale never clips).
 * The morph loader shows only while Coil reports the image as loading; the image then crossfades
 * in under a bottom scrim that keeps the white title above 4.5:1.
 */
@Composable
private fun GuideHero(
    title: String,
    subtitle: String?,
    hero: CommonsImage?,
    showPlaceholderIcon: Boolean,
    modifier: Modifier = Modifier
) {
    val url = hero?.thumbUrl?.takeIf { PlanGuideFormat.isSafeHttpsUrl(it) }
    var imageState by remember(url) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    val imageLoading = url != null &&
        (imageState is AsyncImagePainter.State.Loading || imageState is AsyncImagePainter.State.Empty)
    val imageShown = url != null && imageState is AsyncImagePainter.State.Success
    var creditOpen by remember(url) { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val heroShape = RoundedCornerShape(24.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        val minHeight = maxWidth * (9f / 16f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(heroShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            if (url != null) {
                AsyncImage(
                    model = rememberGuideImageRequest(url),
                    contentDescription = "Photo of $title",
                    contentScale = ContentScale.Crop,
                    onState = { imageState = it },
                    modifier = Modifier.matchParentSize()
                )
            } else if (showPlaceholderIcon) {
                Icon(
                    imageVector = Icons.Rounded.Landscape,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                )
            }
            if (imageLoading) {
                ContainedLoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    containerSize = 96.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    containerShape = RoundedCornerShape(24.dp),
                    polygons = LoadingIndicatorDefaults.GuideHeroPolygons,
                    contentDescription = "Loading guide"
                )
            }
            if (imageShown) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.4f to Color.Black.copy(alpha = 0.12f),
                                1f to Color.Black.copy(alpha = PlanGuideDefaults.HeroScrimAlpha)
                            )
                        )
                )
            }
            val titleColor = if (imageShown) Color.White else MaterialTheme.colorScheme.onSurface
            val subtitleColor = if (imageShown) Color.White.copy(alpha = 0.92f) else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 72.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AutoShrinkTitle(text = title, color = titleColor)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = subtitleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (imageShown && hero != null) {
                val credit = PlanGuideFormat.imageCredit(hero)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    IconButton(
                        onClick = { creditOpen = !creditOpen },
                        modifier = Modifier.semantics { contentDescription = "Photo credit" }
                    ) {
                        Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.45f), contentColor = Color.White) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }
                    }
                    if (creditOpen) {
                        val source = hero.sourceUrl?.takeIf { PlanGuideFormat.isSafeHttpsUrl(it) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            contentColor = Color.White,
                            modifier = Modifier
                                .padding(start = 48.dp)
                                .then(
                                    if (source != null) {
                                        Modifier.clickable(role = Role.Button, onClickLabel = "Open photo source") {
                                            runCatching { uriHandler.openUri(source) }
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            Text(
                                text = credit,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** displaySmall destination title: shrinks to 70% before wrapping to 2 lines with an ellipsis. */
@Composable
private fun AutoShrinkTitle(text: String, color: Color, modifier: Modifier = Modifier) {
    val base = MaterialTheme.typography.displaySmall
    var scale by remember(text) { mutableFloatStateOf(1f) }
    val style = base.copy(
        fontSize = if (base.fontSize.isSpecified) base.fontSize * scale else base.fontSize,
        lineHeight = if (base.lineHeight.isSpecified) base.lineHeight * scale else base.lineHeight
    )
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.semantics { heading() },
        onTextLayout = { result ->
            if (result.hasVisualOverflow && scale > MIN_TITLE_SCALE) {
                scale = (scale - 0.1f).coerceAtLeast(MIN_TITLE_SCALE)
            }
        }
    )
}

private const val MIN_TITLE_SCALE = 0.7f

// -------------------------------------------------------------------------------------------------
// Search, suggestions, disambiguation, skeletons
// -------------------------------------------------------------------------------------------------

@Composable
private fun DestinationSearch(
    state: TripGuideUiState,
    actions: TripGuideActions,
    onClose: (() -> Unit)?,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PlanGuideDefaults.ScreenGutter),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Where is this trip headed?",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close search")
                }
            }
        }
        OutlinedTextField(
            value = state.destinationQuery,
            onValueChange = { actions.updateDestinationQuery(it) },
            placeholder = { Text("Search a city, town or region") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (state.destinationQuery.isNotEmpty()) {
                    IconButton(onClick = { actions.updateDestinationQuery("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = {
                actions.submitDestinationQuery()
                focusManager.clearFocus()
                onSubmitted()
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (state.bookingSuggestions.isNotEmpty()) {
            Text(
                text = "Suggestions from your bookings",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                state.bookingSuggestions.forEach { suggestion ->
                    val text = PlanGuideFormat.sanitizeDisplay(suggestion) ?: return@forEach
                    SuggestionChip(
                        onClick = {
                            focusManager.clearFocus()
                            actions.useBookingSuggestion(suggestion)
                            onSubmitted()
                        },
                        label = { ChipText(text) },
                        icon = { Icon(Icons.Rounded.Place, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DisambiguationChips(
    phase: GuidePhase.Disambiguate,
    actions: TripGuideActions,
    onChosen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PlanGuideDefaults.ScreenGutter),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Which one did you mean?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            phase.candidates.take(3).forEach { candidate ->
                SuggestionChip(
                    onClick = {
                        actions.chooseCandidate(candidate)
                        onChosen()
                    },
                    label = {
                        Text(
                            text = PlanGuideFormat.candidateLabel(candidate),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Rounded.TravelExplore, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }
    }
}

/** Resolving: empty 24dp canvas plus title/chip skeletons (the wavy bar carries the progress). */
@Composable
private fun HeroSkeleton(label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = label
                liveRegion = LiveRegionMode.Polite
            },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxWidth * (9f / 16f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .planDecorative()
            )
        }
        Column(
            modifier = Modifier
                .padding(horizontal = PlanGuideDefaults.ScreenGutter)
                .planDecorative(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SkeletonBlock(widthFraction = 0.6f, height = 28.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBlock(widthFraction = 0.28f, height = 32.dp)
                SkeletonBlock(widthFraction = 0.38f, height = 32.dp)
                SkeletonBlock(widthFraction = 0.6f, height = 32.dp)
            }
        }
    }
}

/** Hydrating: chip skeletons plus two card-shaped placeholders. */
@Composable
private fun SectionSkeletons(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PlanGuideDefaults.ScreenGutter)
            .planDecorative(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBlock(widthFraction = 0.3f, height = 32.dp)
            SkeletonBlock(widthFraction = 0.45f, height = 32.dp)
        }
        Spacer(Modifier.heightIn(min = 4.dp))
        SkeletonBlock(widthFraction = 0.45f, height = 32.dp)
        repeat(2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .clip(PlanGuideDefaults.CardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )
        }
    }
}
