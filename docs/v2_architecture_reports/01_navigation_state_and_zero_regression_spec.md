# Report 01: Navigation, State & Zero-Regression Integration Specification (`v2.0` Option A)

**Author**: Subagent 1 (`v2_nav_and_zero_regression_architect`)  
**Target Files**: `MainActivity.kt`, `ui/navigation/SplitMateAppNavHost.kt`, `ui/SplitMateAppComposable.kt`

---

## 1. Exact Composable Hierarchy & Wiring Diagram (`MainActivity.kt` -> `SplitMateAppNavHost.kt` -> Child Screens)

```mermaid
graph TD
    MA["MainActivity.kt (L36-41)<br/>setContent { SplitMateMaterial3ExpressiveTheme { SplitMateApp(viewModel) } }"]
    
    MA --> APP["SplitMateApp(viewModel: SplitMateViewModel)<br/>SplitMateAppComposable.kt (L142-242)<br/>Preserves SharedPreferences Dark Theme Sync + NavHost Gate"]
    
    APP -->|route = 'onboarding'| ONB["OnboardingSetupScreen<br/>(OnboardingAndSettingsScreens.kt)"]
    APP -->|route = 'settings'| SET["UserSettingsScreen<br/>(OnboardingAndSettingsScreens.kt)"]
    APP -->|route = 'dashboard'| NAV["SplitMateAppNavHost(viewModel, onOpenSettings)<br/>ui/navigation/SplitMateAppNavHost.kt"]

    subgraph "SplitMateAppNavHost (State Machine + BackHandler + flightPdfPickerLauncher)"
        NAV --> BAR["AnimatedVisibility(isGlobalTab)<br/>spring(dampingRatio = 0.76f, stiffness = 380f)<br/>SplitMateGlobalBottomBar / Adaptive NavigationRail"]
        
        subgraph "Level 1: Global Tabs (Bottom Bar VISIBLE)"
            NAV -->|SplitMateRoute.DashboardLedgers| T1["LedgersDashboardScreen(viewModel, ...)<br/>Hero Balance + AnimatedTransitDeckHeroCard + Active Groups + CreateNewGroupDialog"]
            NAV -->|SplitMateRoute.QuickExpense| T2["QuickExpenseScreen(viewModel, ...)<br/>0.00¢ Drift Calculator + AuroraEnergySurface + Coin Flight"]
            NAV -->|SplitMateRoute.GreedySettlement| T3["GreedySettlementScreen(viewModel)<br/>Max-Heap ⓘ Graph Inspector + UpiExpressPaymentSheet + Mark Paid"]
            NAV -->|SplitMateRoute.AuditVault| T4["AuditVaultScreen(viewModel, ...)<br/>Chronological Timeline + Boarding Pass Inspector Sheet"]
        end

        subgraph "Level 2 & 3: Fullscreen Immersive Flows (Bottom Bar AUTO-HIDDEN)"
            T1 -->|Tap Group Card: onGroupClick(groupId)| HUB["SplitMateRoute.TripHub(tripId)<br/>ConnectedTripHubContainer(viewModel, tripId, ...)"]
            HUB --> THS["TripHomeScreen(viewModel, tripId, ...)<br/>4 Top Section Pills: Overview | Travel | Money | People<br/>+ Category Filter Chips + '+ Add Booking' Modal Sheet"]
            THS -->|Toggle 'Classic View / 3D Pass'| CLASSIC["LedgersDashboardScreen (openedGroupDetailId = tripId)<br/>Preserves renderGroupDetailPane + 3D Flip Travel Pass 100% intact"]
            
            T1 & THS & T4 -->|Enter/Inspect 10-Digit PNR| PNR["SplitMateRoute.TrainPnrReview(initialPnr)<br/>PnrExpenseReviewScreen(viewModel, initialPnr, ...)"]
            T1 & THS & T4 -->|flightPdfPickerLauncher / 6-Char PNR| FLT["SplitMateRoute.FlightPdfReview(extractedTicket)<br/>FlightExpenseReviewScreen(viewModel, extractedTicket, ...)<br/>Preserves 24kHz PCM Tear/Stamp Acoustics + Toggle + 3D Fold Pass"]
        end
    end
```

---

## 2. Forensic Audit of Raw `Debug/splitmateappnavhost.kt.txt` vs. Production Codebase

Dropping [`Debug/splitmateappnavhost.kt.txt`](file:///usr/local/google/home/karadkar/splitmate/Debug/splitmateappnavhost.kt.txt) verbatim fails compilation at **9 exact call sites** and disconnects SQLite Room. The table below maps every discrepancy to its exact production fix:

| # | Location in `splitmateappnavhost.kt.txt` | Raw Mockup Code | Production Symbol & File Reference | Exact Fix in `SplitMateAppNavHost.kt` |
| :- | :--- | :--- | :--- | :--- |
| **1** | [`L17`, `L91`](file:///usr/local/google/home/karadkar/splitmate/Debug/splitmateappnavhost.kt.txt#L17) | `import com.splitmate.app.ui.components.ActiveTravelPass` | [`ActiveTravelPassMode`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/components/AnimatedTransitDeckHeroCard.kt#L70-L73) | Replace with `ActiveTravelPassMode.TRAIN` / `ActiveTravelPassMode.FLIGHT`. |
| **2** | [`L82-85`](file:///usr/local/google/home/karadkar/splitmate/Debug/splitmateappnavhost.kt.txt#L82-L85) | `fun SplitMateAppNavHost(modifier: Modifier, initialRoute: SplitMateRoute)` | [`SplitMateViewModel`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt#L101) | Add `viewModel: SplitMateViewModel` and `onOpenSettings: () -> Unit = {}` parameters. |
| **3** | [`L44`](file:///usr/local/google/home/karadkar/splitmate/Debug/splitmateappnavhost.kt.txt#L44) | `data class FlightPdfReview(val pnrCode: String = "W4X9QP")` | [`UniversalFlightTicketResult`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/data/UniversalFlightTicketExtractor.kt) | Store `val extractedTicket: UniversalFlightTicketExtractor.UniversalFlightTicketResult` inside `SplitMateRoute.FlightPdfReview` (or resolve by PNR via `PnrNetworkRepository.loadConfirmedFlightTicketResult`). |
| **4** | [`L176-202`](file:///usr/local/google/home/karadkar/splitmate/Debug/splitmateappnavhost.kt.txt#L176-L202) | `LedgersDashboardScreen(onEnterTrainPnrClick = ..., onGroupClick = ...)` | [`LedgersDashboardScreen`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L672-L680) | Keep existing signature of `LedgersDashboardScreen` in [`SplitMateAppComposable.kt:L672-680`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L672-L680) and add optional `onOpenTripHub: ((groupId: String) -> Unit)? = null` parameter with default `null` so zero existing callers break. |
| **5** | [`L185-188`](file:///usr/local/google/home/karadkar/splitmate/Debug/splitmateappnavhost.kt.txt#L185-L188) | `onNewGroupClick = { navigateTo(TripHub("hampi")) }` | [`showNewGroupDialog`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L685) (`L2692-3192`) | Do **not** hijack `+ New Group` with hardcoded `"hampi"`. Keep `showNewGroupDialog = true` inside `LedgersDashboardScreen` (`L2229`, `L2422`) opening the real Contact-linked `CreateNewGroupDialog`. |
| **6** | [`L225-231`](file:///usr/local/google/home/karadkar/splitmate/Debug/splitmateappnavhost.kt.txt#L225-L231) | `PnrExpenseReviewScreen(onBackClick, onConfirmAndAddToLedger)` | [`PnrExpenseReviewScreen`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L233-L238) | Call `PnrExpenseReviewScreen(viewModel = viewModel, initialPnr = targetScreen.initialPnr, onBackClick = { navigateBack() }, onExpenseAdded = { navigateBack() })`. |
| **7** | [`L238-244`](file:///usr/local/google/home/karadkar/splitmate/Debug/splitmateappnavhost.kt.txt#L238-L244) | `FlightExpenseReviewScreen(onBackClick, onConfirmAndAddToLedger)` | [`FlightExpenseReviewScreen`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L509-L515) | Call `FlightExpenseReviewScreen(viewModel = viewModel, extractedTicket = targetScreen.extractedTicket, onPickAnotherPdfClick = { flightPdfPickerLauncher.launch("application/pdf") }, onBackClick = { navigateBack() }, onConfirmAndAddToLedger = { _ -> navigateBack() })`. |
| **8** | [`L251-276`](file:///usr/local/google/home/karadkar/splitmate/Debug/splitmateappnavhost.kt.txt#L251-L276) | `QuickExpenseScreen(...)`, `GreedySettlementScreen()`, `AuditVaultScreen()` | [`QuickExpenseScreen`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt#L129-L135), [`GreedySettlementScreen`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L3200), [`AuditVaultScreen`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L4641) | Pass `viewModel = viewModel` to all three composables and wire `onOpenPnrDirectSplit` and `onOpenPnrWithTicket`. |
| **9** | [`L100-107`](file:///usr/local/google/home/karadkar/splitmate/Debug/splitmateappnavhost.kt.txt#L100-L107) | Defines `navigateBack()` but never registers `BackHandler` | `androidx.activity.compose.BackHandler` | Register `BackHandler(enabled = backStack.isNotEmpty() \|\| currentRoute !is SplitMateRoute.DashboardLedgers)` to pop `backStack` or return to `DashboardLedgers`. |

---

## 3. Zero-Regression Verification Checklist (All `v1.9.16` Capabilities Preserved)

| Feature / Ritual from `v1.9.16` | Source File & Lines | Status in `v2.0` Architecture |
| :--- | :--- | :--- |
| **Paper Tear & Gate-Stamp Acoustics (`24kHz` PCM AudioTrack) + Toggle Banner** | [`FlightExpenseReviewScreen.kt:L519`, `L1020-1026`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L1020-L1026) | **100% Untouched**. `FlightExpenseReviewScreen.kt` is not stripped; `PaperSensoryFeedbackBanner` and `isAudioSensoryEnabled` remain 100% intact. |
| **3D Foldable Boarding Pass & Gate Stamp Ritual** | [`FlightExpenseReviewScreen.kt:L1028-1143`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L1028-L1143) | **100% Untouched**. Opened via `SplitMateRoute.FlightPdfReview` from Dashboard, TripHub (`View E-Ticket` / `+ Add Booking`), or AuditVault. |
| **10-Digit IRCTC PNR Live Lookup & Tactile Paper Pass** | [`PnrExpenseReviewScreen.kt:L233-955`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L233-L955) | **100% Untouched**. Opened via `SplitMateRoute.TrainPnrReview(initialPnr)` from Dashboard, TripHub (`View E-Ticket` / `Berth Chart` / `+ Add Booking`), QuickExpense, or AuditVault. |
| **Aurora Energy Surface & Coin Flight Calculator** | [`QuickExpenseAndGuideScreens.kt:L754-798`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt#L754-L798), `AuroraEnergySurface.kt` | **100% Untouched**. Hosted in `SplitMateRoute.QuickExpense` and reachable from Global Bottom Bar (`Split`), FAB (`Log Expense`), and TripHub (`+ Add Booking`). |
| **Max-Heap `ⓘ` Graph Inspector & Kinetic Particle Flow** | [`SplitMateAppComposable.kt:L3406-3603`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L3406-L3603) | **100% Untouched**. Hosted inside `GreedySettlementScreen(viewModel)`, which powers both `SplitMateRoute.GreedySettlement` (`Settle` tab) and `TripHubTab.MONEY` inside `ConnectedTripHubContainer`! |
| **`UpiExpressPaymentSheet` & Direct WhatsApp / Mark Paid** | [`UpiExpressPaymentSheet.kt:L277`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/components/UpiExpressPaymentSheet.kt#L277), [`SplitMateAppComposable.kt:L3896-4265`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L3896-L4265) | **100% Untouched** (and accessible from `TripHomeScreen`'s `Money` tab). |
| **Classic Group Detail (`renderGroupDetailPane`) & 3D Flip Pass** | [`SplitMateAppComposable.kt:L967-1981`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L967-L1981) | **100% Untouched** in `SplitMateAppComposable.kt` and reachable via 1-tap `"Classic Ledger / 3D Pass"` switch from `TripHomeScreen`. |
