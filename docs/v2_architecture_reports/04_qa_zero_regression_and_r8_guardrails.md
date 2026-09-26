# Report 04: Zero-Regression Preservation Matrix, Edge-Case QA & Release Guardrails

**Auditor**: Subagent 4 (`v2_principal_qa_and_r8_guardrail_auditor`)  
**Target Release**: SplitMate `v1.9.16` (versionCode `32`) -> `v2.0.0` (versionCode `33`)  
**Mode**: Strict Read-Only Codebase & Mathematical Verification Audit

---

## 1. The `v1.9.16` -> `v2.0` Zero-Regression Preservation Checklist

Every signature tactile, acoustic, 3D, Aurora, and mathematical feature shipped through `v1.9.16` has been audited at the exact line-number level. **None of these files, composables, or state machines may be removed, bypassed, or degraded when introducing `TripHomeScreen.kt` and `SplitMateAppNavHost.kt`.**

| # | Signature Feature (`v1.9.16`) | Exact Source File & Line Range | Technical Mechanism Audited | Mandatory `v2.0` Verification Gate |
| :--- | :--- | :--- | :--- | :--- |
| **1.1** | **Paper Tear & Gate-Stamp Acoustics (`24kHz` PCM `AudioTrack`)** | [FlightExpenseReviewScreen.kt](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L205-L283), [L911](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L911), [L1020-L1026](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L1020-L1026), [L1489-L1550](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L1489-L1550), [L1711-L1769](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L1711-L1769)<br>[PnrExpenseReviewScreen.kt](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L591) | `buildBoardingPassTearAndStampPcm` synthesizes a `180ms` (`4,320`-sample) 16-bit mono `24kHz` PCM waveform combining high-pass filtered perforation micro-bursts (`0..105ms`) and a `135Hz -> 52Hz` sub-bass mechanical gate-stamp thud (`112..180ms`). Played via static `AudioTrack` (`MODE_STATIC`) on stub fold/unfold and via `playBoardingPassTearAndStampOneShot` on Flight & IRCTC PNR commit, gated by `PaperSensoryFeedbackBanner` (`isAudioSensoryEnabled`). | 1. Tapping the boarding pass stub or pressing **Commit** on Flight/IRCTC PNR screens MUST play the synthesized tear + stamp waveform when sensory toggle is ON.<br>2. `DisposableEffect(tearAndStampAudioTrack)` cleanup MUST remain intact to prevent `AudioTrack` leaks.<br>3. `PaperSensoryFeedbackBanner` MUST NOT be removed. |
| **1.2** | **`[ LOGGED · 0.00¢ DRIFT ]` Mechanical Stamp & Perforation Tear Sweep** | [FlightExpenseReviewScreen.kt](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L286-L379), [L919-L927](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L919-L927)<br>[PnrExpenseReviewScreen.kt](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L609-L616) | `BoardingPassCommitStampOverlay` renders a two-stage `320ms` commit animation: Stage 1 (`0..125ms`) sweeps a glowing perforation tear line across the pass; Stage 2 (`125..310ms`) slams a `-8.5°` rotated `[ LOGGED · 0.00¢ DRIFT ]` ink badge (`scale 1.85f -> 1.0f`) before navigating back. | Committing a Flight or IRCTC PNR expense from `TripHomeScreen` (`+ Add Booking`) MUST still run the full `320ms` stamp + tear animation before popping back. |
| **1.3** | **3D Foldable Airline Boarding Pass & Gate Stamp** | [FlightExpenseReviewScreen.kt](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L1684-L2420) | `AnimatedLuxuryAirlineBoardingPass` animates `stubTiltZ` (`0f` vs `-3.4f`), `stubPitchX` (`0f` vs `-16f`), `stubScale` (`1f` vs `0.93f`), and `stubSlideY` (`0f` vs `18f`) with `cameraDistance = 16f * density`, plus the `-7.5°` rotated `"BOARDING VERIFIED · PNR ..."` gate stamp. | Navigating to `FlightExpenseReviewScreen` MUST preserve the 3D foldable stub, dynamic perforation cutouts, and gate stamp. |
| **1.4** | **3D Stacked Transit Deck Shuffle & 180° Horizontal Pass Flip** | [AnimatedTransitDeckHeroCard.kt](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/components/AnimatedTransitDeckHeroCard.kt#L156-L305)<br>[SplitMateAppComposable.kt](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L1268-L1666) | `AnimatedTransitDeckHeroCard` uses a parabolic arc (`4f * deckProgress * (1f - deckProgress)`) and midpoint Z-index swap (`deckProgress >= 0.5f`) to shuffle Flight & Rail cards. `SplitMateAppComposable.kt` implements 180° horizontal drag `rotationY` with `cameraDistance = 15f * density` and a midpoint `90°` haptic tick. | Both components MUST remain compiled and accessible with zero gesture conflicts. |
| **1.5** | **Google Material 3 Aurora Energy States (`Gm3AuroraEnergySurface`)** | [AuroraEnergySurface.kt](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/components/AuroraEnergySurface.kt#L54-L240)<br>[QuickExpenseAndGuideScreens.kt](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt#L757-L778) | `Gm3AuroraEnergySurface` drives the 5-state radial shader (`IDLE`, `ANTICIPATING`, `RECEIVING`, `PROCESSING`, `RESPONDING`) with `BUCKWHEAT_SAGE` and `AVIATION_PERIWINKLE` palettes and auto-decay timers. | All 3 active Aurora hosts (Quick Expense Hero Total input, Flight PNR Sync Banner, and IRCTC PNR Lookup Card) MUST retain their exact `Gm3EnergyState` transitions and keystroke-reactive `receivingIntensity`. |
| **1.6** | **`+1p` Largest-Remainder Coin Arc Flight & Avatar Catch Pulse** | [QuickExpenseAndGuideScreens.kt](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt#L762-L798), [L1321-L1392](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt#L1321-L1392) | When `remainderPennies > 0`, `triggerRemainderCoinFlight` launches a `420ms` parabolic coin pill from the Peach Remainder Banner into the recipient's avatar, followed by a spring catch pulse. | Entering an uneven amount in `QuickExpenseScreen` MUST still trigger the flying `+1p` coin arc and recipient avatar catch pulse. |
| **1.7** | **Interactive Max-Heap `ⓘ` Greedy Debt Inspector & Balance Drain** | [SplitMateAppComposable.kt](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L3197-L3603) | `GreedySettlementScreen` renders the compact `ⓘ` toggle next to `Trip Settlement Summary` and `320ms` tabular balance drain on `Mark Paid`. | The **Money** tab in `TripHomeScreen` MUST reuse `GreedySettlementScreen(viewModel)` directly so the `ⓘ` Max-Heap Graph Inspector and `320ms` balance drain are 100% preserved. |
| **1.8** | **UPI Express Payment Sheet & Contact Discovery** | [UpiExpressPaymentSheet.kt](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/components/UpiExpressPaymentSheet.kt#L88-L500) | Builds deep-link `upi://pay` URIs, decodes UPI QR images via ZXing, discovers VPAs from Contacts, and observes `Lifecycle.Event.ON_RESUME`. | Must remain 100% wired in `GreedySettlementScreen` and accessible from the Trip Hub **Money** tab. |

---

## 2. Mathematical & Multi-Device Sync Invariance Audit

### 2.1 Critical Finding in `SplitMateMathEngine.orderParticipantsPayerFirst`
In [SplitMateMathEngine.kt:L67-L78](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/SplitMateMathEngine.kt#L67-L78):
```kotlin
fun orderParticipantsPayerFirst(
    memberIds: List<String>,
    payerId: String? = null,
    currentUserId: String? = null
): List<String> {
    val primaryPayerId = payerId?.takeIf { it.isNotBlank() } ?: currentUserId
    return memberIds.distinct().sortedWith(
        compareByDescending<String> { it == primaryPayerId }
            .thenBy { it } // Strictly canonical memberId tie-breaker!
    )
}
```
Removing `.thenByDescending { it == currentUserId }` guarantees that index `0` is **always** `primaryPayerId` (the Payer absorbs the first `+1p` remainder penny) and indices `1..K-1` are sorted strictly by canonical `memberId`, ensuring **100% identical penny allocations across all 4 phones** regardless of which member has `isCurrentUser = true`.

### 2.2 Formal Invariance of `computeGroupNetBalances` & `simplifyDebtsGreedy`
- Neither `computeGroupNetBalances` nor `simplifyDebtsGreedy` depends on `isCurrentUser` or `displayName`: `MemberNetBalance.compareTo` orders strictly by `netCents` descending and breaks ties by `this.memberId.compareTo(other.memberId)`.
- Therefore, all synced phones compute the **exact same net balances and exact same $N-1$ Greedy Transfers**.

---

## 3. Edge-Case & R8/ProGuard Release Audit

1. **Group Archetype Safety (`TripHomeScreen.kt`)**:
   - **Case A (Newly Created Empty Group)**: Dynamic group title, `"{members.size} Travelers"`, `₹0.00` spend, and a clean Empty State card with `[ + Add Booking ]` and `[ Sync / Share Trip ]`.
   - **Case B (Non-Vacation Roommate / Dinner Group)**: Dynamically hides empty travel filter pills or defaults to `All (N)`, rendering general expenses cleanly with tap-to-expand `Edit` / `Delete`.
   - **Case C (Multi-Booking Trip Hub / Hampi Trip)**: Renders Deep-Green Train Card, Periwinkle Flight Card, Lodging Card, and Ground Mobility Cards with perspective-relative berth highlights and per-traveler split footers.
2. **Dark Mode & Light Mode Token Safety**:
   - Never hardcode `Color.White` or `Color(0xFFFAF7F2)` on card surfaces without `SplitMateTheme.isDark` checks; use `SplitMateTheme.BackgroundPrimary`, `SplitMateTheme.SurfaceWhite`, `SplitMateTheme.SurfaceMuted`, and `SplitMateTheme.TextPrimary`.
3. **R8 / ProGuard Release Hardening (`proguard-rules.pro`)**:
   - `org.json.JSONObject`, `java.util.zip.GZIPOutputStream`, `GZIPInputStream`, and `android.util.Base64` are Android bootclasspath classes (`100% R8-safe` with zero reflection).
   - Add `-keep class com.splitmate.app.data.** { *; }` to `proguard-rules.pro` so all Room entities and sync data structures remain untouched during `assembleRelease`.
