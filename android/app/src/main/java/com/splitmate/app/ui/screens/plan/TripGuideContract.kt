package com.splitmate.app.ui.screens.plan

import com.splitmate.app.data.guide.DestinationCandidate
import com.splitmate.app.data.guide.GuidePack
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.LoopRoute
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.StayPin
import com.splitmate.app.data.guide.nav.TravelMode

/**
 * v2.3.4 Plan tab: ViewModel <-> Screens CONTRACT (owned by the integration lead).
 *
 * `TripGuideViewModel` (Wave 2 stream E1) exposes `StateFlow<TripGuideUiState>` and implements
 * [TripGuideActions]. The Compose screens in this package (Wave 2 stream E2) render
 * [TripGuideUiState] and call [TripGuideActions] only. Screens never touch repositories, and the
 * ViewModel never imports Compose UI.
 */

/** Sub-view switcher inside the Plan tab (decision #9). */
/** v2.4.0: the switch shows Days | Explore; LOOP opens from "Make a loop" on a day. */
enum class PlanSubView(val label: String) { BOOKINGS("Days"), EXPLORE("Explore"), LOOP("Loop") }

/** Guide lifecycle (audit §5.3). */
sealed interface GuidePhase {
    /** No destination chosen yet: show search plus booking-derived suggestions. */
    data object Empty : GuidePhase
    data object Resolving : GuidePhase
    data class Disambiguate(val candidates: List<DestinationCandidate>) : GuidePhase
    /** Destination known (title renders immediately). Content is still hydrating. */
    data class Hydrating(val candidate: DestinationCandidate) : GuidePhase
    data object Ready : GuidePhase
    /** Some sources failed; [message] is user-safe. */
    data class Partial(val message: String) : GuidePhase
    /** Nothing usable; [retryable] shows the Retry button. */
    data class Error(val message: String, val retryable: Boolean) : GuidePhase
}

/** Explore content, grouped for rendering ("Must See", "Things to Do", "Local Eats"). */
data class GuideSection(
    val kind: PlaceKind,
    val title: String,
    val places: List<PlaceCardUi>,
    val initiallyVisible: Int = 3            // progressive disclosure ("Show N more")
)

/** Presentation model for one place card. All strings are ready to display. */
data class PlaceCardUi(
    val place: Place,
    val distanceLabel: String?,             // e.g. "1.2 km ≈" or null
    val isPinned: Boolean,
    val isExcerpt: Boolean,
    val sourceLabel: String?                // e.g. "From Wikidata" for WDQS results, else null
)

/** Stay (hotel) state for the Set-stay sheet and the Loop start. */
data class StayUi(
    val stay: StayPin?,
    val shareWithGroup: Boolean,            // default false (decision #14)
    val isShared: Boolean,                  // a stay came from another member's shared manifest
    val attribution: String?,               // "© OpenStreetMap contributors" when Nominatim-derived
    val sleepListings: List<Place>          // Wikivoyage SLEEP listings for the picker
)

/** Result of a stay input attempt, driving the stay sheet's inline feedback. */
sealed interface StayInputFeedback {
    data object Idle : StayInputFeedback
    data object Working : StayInputFeedback
    data class Preview(val location: LatLng, val label: String?, val approximate: Boolean) : StayInputFeedback
    /** The link had no coordinates: offer "Search by name" / Sleep listing / coordinates. */
    data class NeedsFallback(val placeNameHint: String?) : StayInputFeedback
    data class Rejected(val message: String) : StayInputFeedback
}

/** Loop view state. */
data class LoopUi(
    val route: LoopRoute?,
    val autoSuggested: Boolean,
    val stopsWithoutCoordinates: List<Place>,
    val truncated: Boolean,
    val totalLabel: String?,                // e.g. "6 stops · ≈ 14.2 km"
    val caveats: List<String>               // straight-line / ferry / sunset copy
)

data class TripGuideUiState(
    val subView: PlanSubView = PlanSubView.BOOKINGS,
    val guideEnabled: Boolean = true,       // remote kill-switch
    val phase: GuidePhase = GuidePhase.Empty,
    /** True ONLY while a network job is in flight (drives InFlightWavyProgressIndicator). */
    val networkInFlight: Boolean = false,
    val isOffline: Boolean = false,
    val offlineSavedLabel: String? = null,  // e.g. "Offline · guide saved on 29 Sep"
    val destinationQuery: String = "",
    val bookingSuggestions: List<String> = emptyList(),  // derived from PNR / flight arrival
    val pack: GuidePack? = null,
    val sections: List<GuideSection> = emptyList(),
    val attribution: List<String> = emptyList(),
    val guideUpdateAvailable: Boolean = false,           // soft-expiry refresh found a newer revision
    val saveOfflineEnabled: Boolean = false,
    val saveOfflineSizeLabel: String? = null,            // e.g. "0.9 MB"
    val stay: StayUi = StayUi(null, false, false, null, emptyList()),
    val stayFeedback: StayInputFeedback = StayInputFeedback.Idle,
    val stayFeedbackRequestId: Long = 0L,
    val loop: LoopUi = LoopUi(null, false, emptyList(), false, null, emptyList()),
    val selectedPlace: Place? = null                     // place detail sheet
)

/** One-shot effects the screen must perform with an Android Context. */
sealed interface TripGuideEffect {
    /** Launch ACTION_VIEW on [primaryUri]; on ActivityNotFoundException open [fallbackUrl]. */
    data class OpenMaps(val primaryUri: String, val fallbackUrl: String) : TripGuideEffect
    /** Launch ACTION_VIEW [primaryUri] wrapped in Intent.createChooser([chooserTitle]); fallback [fallbackUrl]. */
    data class OpenMapsChooser(val primaryUri: String, val fallbackUrl: String, val chooserTitle: String) : TripGuideEffect
    data class OpenUrl(val url: String) : TripGuideEffect
    data class CopyToClipboard(val label: String, val text: String) : TripGuideEffect
    data class ShareText(val text: String) : TripGuideEffect
    data class Snackbar(val message: String) : TripGuideEffect
}

/** Every user intent the Plan screens can emit. */
interface TripGuideActions {
    fun selectSubView(view: PlanSubView)
    fun updateDestinationQuery(query: String)
    fun submitDestinationQuery()
    fun chooseCandidate(candidate: DestinationCandidate)
    fun useBookingSuggestion(suggestion: String)
    fun retry()
    fun acceptGuideUpdate()
    fun setSaveOffline(enabled: Boolean)

    fun togglePin(placeId: String)
    fun hidePlace(placeId: String)
    fun openPlace(place: Place)
    fun dismissPlace()

    fun directions(place: Place)
    fun navigate(place: Place, mode: TravelMode)
    fun openInOtherApp(place: Place)
    fun copyCoordinates(place: Place)
    fun sharePlace(place: Place)

    fun submitStayInput(text: String)          // pasted link or coordinates text
    fun searchStayByName(nameHint: String)     // user-initiated Nominatim

    sealed interface StaySelection {
        data class SleepListing(val place: Place) : StaySelection
        data object DestinationCenter : StaySelection
    }
    fun chooseStay(selection: StaySelection)
    fun confirmStayPreview()
    fun dismissStayFeedback() = Unit
    fun clearStay()
    fun setShareStay(share: Boolean)

    fun reoptimizeLoop()
    fun openWholeLoopInMaps()
    fun shareLoop()
}
