@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.splitmate.app.ui.screens.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.splitmate.app.data.guide.CommonsImage
import com.splitmate.app.data.guide.Place
import com.splitmate.app.ui.components.GuideShapeTokens

/**
 * Place detail sheet (audit 5.9: details live here, not on the card): photo with credit, name,
 * kind, full blurb, hours, price, address, the Directions split button, pin/hide, and the source
 * and licence line.
 */
@Composable
fun PlaceDetailSheet(
    place: Place,
    card: PlaceCardUi?,
    actions: TripGuideActions,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val name = PlanGuideFormat.sanitizeDisplay(place.name) ?: place.name
    val blurb = PlanGuideFormat.sanitizeDisplay(place.blurb)
    val isExcerpt = card?.isExcerpt ?: PlanGuideFormat.isExcerptHeuristic(place.blurb)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            place.image?.let { image -> PlaceSheetImage(image = image, placeName = name) }

            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            PlaceLabelsRow(
                kind = place.kind,
                isExcerpt = isExcerpt && blurb != null,
                sourceLabel = card?.sourceLabel,
                distanceLabel = card?.distanceLabel
            )
            if (blurb != null) {
                Text(
                    text = blurb,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            PlanGuideFormat.sanitizeDisplay(place.hours)?.let { PlanDetailRow(Icons.Rounded.Schedule, "Hours", it) }
            PlanGuideFormat.sanitizeDisplay(place.price)?.let { PlanDetailRow(Icons.Rounded.Payments, "Price", it) }
            PlanGuideFormat.sanitizeDisplay(place.address)?.let { PlanDetailRow(Icons.Rounded.Place, "Address", it) }

            if (place.location != null) {
                DirectionsSplitButton(place = place, actions = actions, fillWidth = true)
            } else {
                Text(
                    text = "This place has no map location yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AddToLoopToggle(pinned = card?.isPinned == true, onToggle = { actions.togglePin(place.id) })
                TextButton(
                    onClick = {
                        actions.hidePlace(place.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Hide from guide", style = MaterialTheme.typography.labelLarge)
                }
            }

            Text(
                text = PlanGuideFormat.sourceLine(place),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** POI photo (16dp squircle token) with its Commons credit; the credit opens the file page. */
@Composable
private fun PlaceSheetImage(image: CommonsImage, placeName: String) {
    val url = image.thumbUrl.takeIf { PlanGuideFormat.isSafeHttpsUrl(it) } ?: return
    val source = image.sourceUrl?.takeIf { PlanGuideFormat.isSafeHttpsUrl(it) }
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AsyncImage(
            model = rememberGuideImageRequest(url),
            contentDescription = "Photo of $placeName",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(GuideShapeTokens.PlaceThumbnail)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
        Text(
            text = PlanGuideFormat.imageCredit(image),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = if (source != null) {
                Modifier
                    .heightIn(min = 32.dp)
                    .clickable(role = Role.Button, onClickLabel = "Open photo source") {
                        runCatching { uriHandler.openUri(source) }
                    }
            } else {
                Modifier
            }
        )
    }
}
