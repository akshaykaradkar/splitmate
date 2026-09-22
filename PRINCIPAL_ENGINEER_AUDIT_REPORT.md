# Principal Android Staff Engineer & Lead UI/UX Architect Audit Report
**Repository**: `akshaykaradkar/splitmate` (`v1.8.7` · Read-Only Audit)  
**Scope**: All 12 Kotlin source files (`12,652` LOC / `621.5 KB`), Gradle build scripts, Room schemas, and resources under [`android/app/src/main`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main)  
**Audit Status**: **100% Read-Only (Zero files modified or committed)**

---

## Executive Summary & Severity Matrix

| Pillar | Critical | High | Medium | Low | Key Architectural & UX Takeaway |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **1. M3 Expressive & UI/UX Feel** | 3 | 4 | 3 | 2 | `GroupBoardingPassCard` nests 4 boxes of raw scraper/radar telemetry inside Group Ledgers & Audit; `PnrExpenseReviewScreen` has **0%** `tnum` tabular typography and **0** haptic calls; logged PNRs inside `Split PNR` are non-clickable 1-line strings. |
| **2. "Ghost Data" & Placeholders** | 2 | 2 | 2 | 1 | When live scraping fails or a random 10-digit number is entered, `fetchLivePnrAndTrainStatus` hashes the PNR (`cleanPnr.hashCode()`) to **silently fabricate a fake train & fake passengers** from `OfflineIndianTrainCatalog`. |
| **3. App Size, Bloat & Stale Code** | 1 | 3 | 2 | 1 | `splitmate-1.8.7.apk` is **17.09 MB** (`minifyEnabled false`, un-shaken `material-icons-extended`, triplicated Figtree fonts, plus a 16.98 MB `splitmate-1.8.4.apk` committed in git root); `Components.kt` (`236` LOC) and `ReceiptLineItem` engines are 100% dead code. |
| **4. Architecture, DB & Security** | 2 | 3 | 2 | 0 | `fallbackToDestructiveMigration()` wipes financial ledgers on schema bumps; **zero `@ForeignKey` constraints** across all Room entities; `0` uses of `collectAsStateWithLifecycle()` across 18 flow collectors; plaintext PNR & UPI storage. |
| **5. API Exposure & Rate Limits** | 2 | 2 | 2 | 0 | `PnrExpenseReviewScreen` hardcodes `forceManualRefresh = true` on automatic 10-digit keystrokes, bypassing the 6-hour cache TTL; rate-limit maps are per-PNR (no global IP token bucket); DiceBear leaks contact names on every keystroke. |
| **6. Ruthless Nitpicking & Math** | 1 | 3 | 3 | 2 | `PnrExpenseReviewScreen` uses truncated integer division (`effectiveTotalPaise / size`), dropping `₹0.01–₹0.02` in preview and overstating Payer reimbursement by `₹0.01` compared to `SplitMateMathEngine.splitEquallyZeroDrift`; `0` `Semantics`/`Role` annotations across the app. |

---

## PILLAR 1: MATERIAL DESIGN 3 EXPRESSIVE & UI/UX FEEL

### `[CRITICAL]` 1.1 Quadruple-Nested Box Clutter & Raw Scraper Telemetry in Group Ledger & Audit (`GroupBoardingPassCard`)
- **Locations**:
  - [`SplitMateTheme.kt:L1701-L2045`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L1701-L2045) (`GroupBoardingPassCard`)
  - [`SplitMateAppComposable.kt:L971-L979`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L971-L979) (Group Detail Expense List)
  - [`SplitMateAppComposable.kt:L3515-L3523`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L3515-L3523) (Tab 4 `AuditVaultScreen`)
- **Finding**:
  1. **4-Level Nested Surface Hierarchy**: Inside every outer Expense `Card` (Level 1), `GroupBoardingPassCard` renders an outer green `Surface` (`RoundedCornerShape(18.dp)`, Level 2), which nests a white Passenger Status `Surface` (`RoundedCornerShape(10.dp)`, Level 3) and a light-green Live Train Radar `Surface` (`RoundedCornerShape(10.dp)`, Level 4) plus two bottom action pills (`Refresh Live CNF/WL & Save` and `ConfirmTkt ↗`).
  2. **Debug & Scraper Strings Exposed to End Users**:
     - [`SplitMateTheme.kt:L1547`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L1547) & [`L1900-L1904`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L1900-L1904): Prints raw engineering labels `"🟢 Live CRIS / RailYatri SSR JSON (₹0 Free)"` and `"Live CRIS Cache (<1m ago · Rate-Limit Protected)"`.
     - [`SplitMateTheme.kt:L994-L999`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L994-L999) & [`L1945-L1960`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L1945-L1960): Prints train consist diagrams (`"🚃 Engine -> EOG -> H1 -> A1 -> A2 -> B1..B6 (3A) -> S1..S6 -> PC"`) and multi-line bracketed passenger strings (`P1: Booked [PQWL/14] → Live [PQWL/7]`) inside a financial ledger list.
  3. **Pipe-Delimited Title Collision**: [`PnrExpenseReviewScreen.kt:L423-L435`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L423-L435) packs `"Class 3A · 4 Pax (₹1,881.35/person)"` into `coachAndSeats`, which [`extractTravelTicketFromTitle`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L1632) re-parses and displays inside Box Level 3, duplicating the outer card's per-person share subtitle.

### `[CRITICAL]` 1.2 Inability to Re-Open the Tactile Paper Boarding Pass for Already-Logged PNRs
- **Location**: [`PnrExpenseReviewScreen.kt:L500-L531`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L500-L531) & [`L570-L646`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L570-L646)
- **Finding**:
  - `existingPnrExpensesInGroup` renders each logged PNR as a static, non-interactive `Text` (`"✓ PNR $extractedPnr · ${formatPaiseDisplay(exp.totalAmountCents)}"` at [`L521-L527`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L521-L527)) with **no `.clickable` handler** to load `loadPersistedPnrSnapshot(context, extractedPnr)` into `liveSnapshot`.
  - Because `liveSnapshot` initializes to `null` (`L199`), returning to `Split PNR` after adding 2 PNRs leaves the user stranded on the `"Enter Your 10-Digit IRCTC PNR Above"` empty card with no way to view the `TactilePaperBoardingPass` (`L813-L1280`) for their existing tickets.

### `[CRITICAL]` 1.3 Complete Absence of Tabular Figures (`tnum`) in `PnrExpenseReviewScreen.kt` (`0%` Coverage) & Gaps in `SplitMateTypography`
- **Locations**:
  - [`PnrExpenseReviewScreen.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt) (`0` occurrences of `fontFeatureSettings = "tnum"` across `1,521` lines)
  - [`SplitMateTheme.kt:L218-L285`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L218-L285) (`SplitMateTypography`)
  - [`SplitMateAppComposable.kt:L731-L737`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L731-L737), [`L942-L949`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L942-L949), [`L1005-L1010`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L1005-L1010), [`L2526-L2540`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L2526-L2540), [`L2623-L2638`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L2623-L2638), [`L3500-L3505`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L3500-L3505)
- **Finding**:
  - In [`PnrExpenseReviewScreen.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt), every single numeric/financial element—the 10-digit PNR input (`L746`), departure/arrival timestamps (`L960, L1036`), passenger share pills (`L1119`), `24.sp` All-Inclusive IRCTC Fare (`L1204`), per-member share column (`L1464`), and Payer reimbursement (`L1472`)—uses proportional glyphs (`1` narrower than `8`), causing vertical column jitter when toggling member inclusion.
  - In [`SplitMateTheme.kt:L218-L285`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L218-L285), `SplitMateTypography` omits `fontFeatureSettings = "tnum"` on `headlineSmall`, `titleMedium`, `titleSmall`, `bodyLarge`, `bodyMedium`, `bodySmall`, `labelLarge`, and `labelSmall`.

### `[HIGH]` 1.4 Zero Haptic Feedback Across `PnrExpenseReviewScreen.kt` (`1,521` LOC) and `SplitMateAppComposable.kt` (`4,088` LOC)
- **Locations**:
  - [`PnrExpenseReviewScreen.kt:L331, L366, L418, L764, L1314, L1389`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L1389)
  - [`SplitMateAppComposable.kt:L279, L406, L566, L630, L900, L2648, L2708, L3456`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L406)
- **Finding**:
  - While `QuickExpenseScreen` triggers `performCrispTactileHaptic` on calculator keypad buttons ([`QuickExpenseAndGuideScreens.kt:L1139-L1352`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt#L1139-L1352)), there are **zero haptic feedback calls** in the entire `PnrExpenseReviewScreen.kt` or `SplitMateAppComposable.kt`. Tapping bottom navigation tabs (`L406`), toggling member inclusion checkboxes on a train ticket (`PnrExpenseReviewScreen.kt:L1389`), clicking `Fetch PNR` (`L764`), or marking a settlement paid (`L2708`) produces zero tactile confirmation.

### `[HIGH]` 1.5 Jarring `0ms` Early-Return Screen Cuts Instead of M3 Expressive Spring Transitions
- **Locations**:
  - [`SplitMateAppComposable.kt:L248-L259`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L248-L259) (`if (showPnrReviewScreen) { ... return }`)
  - [`SplitMateAppComposable.kt:L327-L374`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L327-L374) (`when (currentTab)` without `AnimatedContent`)
  - [`SplitMateAppComposable.kt:L544`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L544) (`if (openedGroup != null) { ... return }`)
  - [`PnrExpenseReviewScreen.kt:L570-L647`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L570-L647) (`if (snapshot == null) ... else ...`)
- **Finding**: Opening `PnrExpenseReviewScreen`, drilling into a Group from the dashboard (`openedGroup != null`), switching bottom navigation tabs, or transitioning from the PNR search prompt to the `TactilePaperBoardingPass` executes un-animated `0ms` composition swaps via early `return`s rather than `AnimatedContent` with `spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)`.

---

## PILLAR 2: THE "GHOST DATA" & PLACEHOLDER HUNT

### `[CRITICAL]` 2.1 Silent Hash-Based Fake Train & Fake Passenger Fabrication When Live Scraping Fails
- **Locations**:
  - [`SplitMateTheme.kt:L976-L1024`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L976-L1024) (`OfflineIndianTrainCatalog`, `OfflineTrainIntermediateRadar`, `OfflineStationNames`)
  - [`SplitMateTheme.kt:L1425-L1512`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L1425-L1512) (`fetchLivePnrAndTrainStatus` fallback generator)
  - [`PnrExpenseReviewScreen.kt:L238-L248`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L238-L248)
- **Finding**:
  - When `liveNetworkHit == false` (e.g., user is offline, RailYatri/ConfirmTkt blocks the request, or the user types a fake 10-digit number like `9999999999`), [`SplitMateTheme.kt:L1427-L1439`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L1427-L1439) computes `cleanPnr.hashCode() % catalogKeys.size` to pick one of 14 hardcoded trains from `OfflineIndianTrainCatalog` (`12925 Paschim SF Express`, `16592 Hampi Express`, `12628 Karnataka Express`, etc.) and [`L1455-L1494`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L1455-L1494) inspects the last digit of the PNR (`cleanPnr.lastOrNull()`) to fabricate 2–4 fake passenger rows (`PQWL 14 -> WL 7`, `RAC 6`, `CNF B2-45 LB`).
  - Because `resolvedTrainNo` and `scrapedStructuredPassengers` are **always non-empty** due to this fallback generator, the guard in [`PnrExpenseReviewScreen.kt:L238`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L238) (`if (fetched.totalFareRupees > 0 || fetched.trainNo.isNotBlank() || fetched.structuredPassengers.isNotEmpty())`) evaluates to **`true` 100% of the time**!
  - **Consequence**: The error message at [`PnrExpenseReviewScreen.kt:L246`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L246) (`fetchError = "Could not fetch live IRCTC details..."`) is **unreachable dead code**. Instead of seeing an error when a PNR lookup fails, the user is shown a fabricated train with `totalFareRupees = 0` (`₹0` fare) and a disabled Confirm button!

### `[HIGH]` 2.2 Hardcoded Seeded Receipt Items, Mock Personas, & Static FX Divisor (`84.0`)
- **Locations**:
  - [`SplitMateViewModel.kt:L28-L35, L80-L88, L1179-L1185`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt#L28-L35) (`defaultSeedReceiptItems`: `"Artisanal Sourdough Pizza"`, `"Truffle Fries"`, `"Matcha Tirami-su"`, `"Craft Kombucha Pitcher"`)
  - [`SplitMateViewModel.kt:L62-L63`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt#L62-L63) (`CurrencyRateEntity("INR", "Indian Rupee", "₹", 83.95)`)
  - [`QuickExpenseAndGuideScreens.kt:L72-L81`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt#L72-L81) (`ParticipantPalette`)
- **Finding**: `SplitMateViewModel` still initializes `receiptItems = defaultSeedReceiptItems()` (`L80`) in every `SplitMateUiState` instance even though the receipt-itemization tab was replaced by `QuickExpenseScreen`.

---

## PILLAR 3: APP SIZE, BLOAT & STALE CODE

### `[CRITICAL]` 3.1 17.09 MB APK Bloat (`minifyEnabled false`, Un-Shaken Extended Icons, & 16.98 MB Binary in Git Root)
- **Locations**:
  - [`splitmate-1.8.4.apk`](file:///usr/local/google/home/karadkar/splitmate/splitmate-1.8.4.apk) (`16,981,793` bytes committed in repository root)
  - [`android/app/build.gradle:L21-L35`](file:///usr/local/google/home/karadkar/splitmate/android/app/build.gradle#L21-L35) & [`L80`](file:///usr/local/google/home/karadkar/splitmate/android/app/build.gradle#L80)
  - [`android/app/src/main/res/font/`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/res/font) (`350,716` bytes)
- **Finding**:
  1. **Un-shaken `material-icons-extended` (`~10–12 MB` DEX bloat)**: [`android/app/build.gradle:L80`](file:///usr/local/google/home/karadkar/splitmate/android/app/build.gradle#L80) pulls in `androidx.compose.material:material-icons-extended` while `minifyEnabled false` (`L23`) and missing `shrinkResources true` prevent R8 from stripping the ~2,500 unused Material icon vector classes. Enabling R8 (`minifyEnabled true` + `shrinkResources true`) will shrink the APK from **17.1 MB down to ~4.2 MB**.
  2. **Triplicated Font Assets (`350 KB`)**: `res/font/figtree_variable.ttf` (`62,712` B) has **0 references**; 5 static TTF files (`288,004` B) are bundled in `res/font/`; and [`SplitMateTheme.kt:L150-L172`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L150-L172) simultaneously requests GMS Downloadable Fonts (`androidx.compose.ui:ui-text-google-fonts:1.6.6`) for the exact same 5 weights.
  3. **Stale APK Committed to Git**: `splitmate-1.8.4.apk` (`16.98 MB`) is still sitting in `/usr/local/google/home/karadkar/splitmate/splitmate-1.8.4.apk`.

### `[HIGH]` 3.2 100% Dead Code Files & Orphaned Subsystems
- **Locations**:
  - [`Components.kt:L1-L236`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/Components.kt#L1-L236) (**100% dead file** — `DiceBearAvatar` and `HorizontalFloatingToolbar` have `0` call sites anywhere in production code)
  - [`SplitMateAppComposable.kt:L4041-L4087`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L4041-L4087) (`ClaimItemRow` — `0` call sites)
  - [`SplitMateTheme.kt:L1651-L1698`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L1651-L1698) (`SplitMateCircularLogoBadge` — `0` call sites)
  - [`SplitMateDao.kt:L18, L28, L84, L97`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/data/SplitMateDao.kt#L18) (`getCurrencyRate`, `observeGroupMembers`, `markPendingExpensesSynced`, `deleteUserProfile` — `0` call sites)
- **Finding**: Instead of calling `DiceBearAvatar` (`Components.kt:L63`), 6 different screens copy-paste raw `AsyncImage(model = ImageRequest.Builder(...).decoderFactory(SvgDecoder.Factory())...)` blocks—even though [`SplitMateApplication.kt:L18-L39`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/SplitMateApplication.kt#L18-L39) already registers `SvgDecoder.Factory()` globally in the singleton `ImageLoader`.

### `[HIGH]` 3.3 Monolithic "God-Files" Violating Single Responsibility
- **Locations**:
  - [`SplitMateAppComposable.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt) (`4,088` lines / `215.5 KB`)
  - [`SplitMateTheme.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt) (`2,048` lines / `98.9 KB` — packs Android `ContentResolver` contact queries, hardware vibrator control, HTTP network scraping, regex HTML parsing, SharedPreferences caching, and UI composables inside a UI theme file)

---

## PILLAR 4: ARCHITECTURE, DB, STORAGE & SECURITY

### `[CRITICAL]` 4.1 `fallbackToDestructiveMigration()` & Zero `@ForeignKey` Constraints in Room
- **Locations**:
  - [`SplitMateRoomDatabase.kt:L18-L37`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/data/SplitMateRoomDatabase.kt#L18-L37)
  - [`RoomEntities.kt:L35-L114`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/data/RoomEntities.kt#L35-L114)
- **Finding**:
  1. `SplitMateRoomDatabase` (`version = 4`, `exportSchema = false`) configures `.fallbackToDestructiveMigration()` (`L35`) with zero `Migration` specs. Any schema bump wipes all user groups, expenses, splits, and settlements.
  2. None of `GroupMemberEntity` (`L35`), `ExpenseEntity` (`L58`), `ExpenseSplitEntity` (`L80`), or `SettlementEntity` (`L98`) define SQLite `foreignKeys = [ForeignKey(..., onDelete = ForeignKey.CASCADE)]`. Furthermore, multi-table updates in [`SplitMateViewModel.kt:L1015-L1016`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt#L1015-L1016) (`deleteLoggedExpense`) and [`L1106-L1109`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt#L1106-L1109) (`editExistingExpense`) execute outside a Room `@Transaction`.

### `[HIGH]` 4.2 Unencrypted Storage of PNR Travel Records, Phone Numbers & UPI Handles + `allowBackup="true"`
- **Locations**:
  - [`SplitMateTheme.kt:L1056, L1154`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L1056) (`splitmate_pnr_rate_guard.xml`)
  - [`AndroidManifest.xml:L11`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/AndroidManifest.xml#L11) (`android:allowBackup="true"`)
  - [`SplitMateViewModel.kt:L441-L457`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt#L441-L457) (`clearLocalVault`)
- **Finding**:
  - Full JSON snapshots of IRCTC PNRs, passenger statuses, and fares are stored in plaintext XML (`splitmate_pnr_rate_guard`), while UPI IDs (`UserProfileEntity.upiId`, `GroupMemberEntity.upiId`) are stored in unencrypted SQLite with `android:allowBackup="true"` enabled in `AndroidManifest.xml`.
  - Worse, `clearLocalVault()` (`SplitMateViewModel.kt:L441`) clears Room tables and `splitmate_prefs`, but **forgets to clear `splitmate_pnr_rate_guard` SharedPreferences and `InMemoryPnrSnapshotCache`**, leaving cached PNR records on disk after "Reset App Data".

### `[HIGH]` 4.3 Zero `collectAsStateWithLifecycle()` Usage Across 18 Collectors & Global `SplitMateTheme.isDark` Mutation During Composition
- **Locations**:
  - [`SplitMateAppComposable.kt:L77, L143`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L143) (`SplitMateTheme.isDark = uiState.isDarkTheme`)
  - [`SplitMateViewModel.kt:L149-L211, L322-L381`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt#L149-L211)
- **Finding**:
  - All 18 state collectors across the app use `collectAsState()` instead of `collectAsStateWithLifecycle()`.
  - `SplitMateViewModel` ingests 7 unfiltered `SELECT *` full-table flows (`observeAllMembers`, `observeAllExpenses`, `observeAllSplits`, etc.) into a single monolithic `SplitMateUiState`, causing `totalBalance` (`L149`) and `activeGroups` (`L164`) to re-run $O(G \times (M + E + S + T))$ Greedy Debt PriorityQueue simplification **twice on every minor UI state emission**.

---

## PILLAR 5: API EXPOSURE & RATE LIMITS

### `[CRITICAL]` 5.1 Auto-Input Cache Bypass (`forceManualRefresh = true`) & Per-PNR Rate-Limit Evasion
- **Locations**:
  - [`PnrExpenseReviewScreen.kt:L231-L236`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L231-L236) & [`L488-L494`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L488-L494)
  - [`SplitMateTheme.kt:L1044-L1050, L1228-L1254`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L1228-L1254)
- **Finding**:
  1. **6-Hour Cache TTL Bypassed on Automatic Keystrokes**: In [`PnrExpenseReviewScreen.kt:L491-L493`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L491-L493), typing the 10th digit in `PnrSearchLookupCard` automatically calls `triggerLivePnrLookup(digits)`, which hardcodes `forceManualRefresh = true` (`L234`). In [`SplitMateTheme.kt:L1237`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L1237), `forceManualRefresh = true` overrides the 6-hour cache (`AUTO_SYNC_COOLDOWN_MS`) down to `60s`, forcing redundant live network hits even when the PNR was already cached minutes earlier.
  2. **Per-PNR Rate Limiter Instead of Global Client Token Bucket**: `InMemoryPnrLastFetchEpochMs` and `"last_sync_$cleanPnr"` (`L1231`) only throttle **identical** PNR strings. Editing the last digit (`1234567890` → `1234567891` → `1234567892`) bypasses the rate guard completely, firing back-to-back scraping requests to `railyatri.in` and `confirmtkt.com` and risking immediate IP blacklisting (`HTTP 403/429`).

### `[HIGH]` 5.2 `21.0s` Sequential Timeout Stall & Unthrottled Per-Keystroke DiceBear Requests
- **Locations**:
  - [`SplitMateTheme.kt:L1280-L1283, L1379-L1382`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L1280-L1283) (`HttpURLConnection` timeouts without `.disconnect()`)
  - [`OnboardingAndSettingsScreens.kt:L133-L138, L480-L485`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/OnboardingAndSettingsScreens.kt#L133-L138) & [`QuickExpenseAndGuideScreens.kt:L1557-L1559`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt#L1557-L1559)
- **Finding**:
  - `fetchLivePnrAndTrainStatus` chains RailYatri (`6s` connect + `6s` read = `12s`) and ConfirmTkt (`4.5s` connect + `4.5s` read = `9s`) sequentially on the same coroutine, stalling the UI spinner for up to **21 seconds** on degraded connections.
  - In `OnboardingSetupScreen`, `UserSettingsScreen`, and `EditFriendUpiDialog`, `buildDiceBearOpenPeepsUrl(nameText)` is recomputed on **every character keystroke**, firing live HTTPS requests to `api.dicebear.com` with the user's/contact's name for every intermediate prefix (`"A"`, `"Ak"`, `"Aks"`...).

---

## PILLAR 6: RUTHLESS NITPICKING (MATH DRIFT, OVERFLOWS, A11Y & 1PX BUGS)

### `[CRITICAL]` 6.1 Truncated Integer Division Drift (`₹0.01–₹0.02` Loss) & Payer Reimbursement Overstatement in `PnrExpenseReviewScreen.kt`
- **Location**: [`PnrExpenseReviewScreen.kt:L278-L279, L431, L670-L673`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L278-L279)
- **Finding**:
  - `PnrExpenseReviewScreen.kt` computes per-member share using raw integer division (`val perSelectedMemberSharePaise = effectiveTotalPaise / selectedMembersList.size` at `L279`) instead of calling `SplitMateMathEngine.splitEquallyZeroDrift(...)`.
  - **Mathematical Proof**: When splitting `₹7,525.40` (`752,540` paise) across **3 selected members**:
    - `752540 / 3 = 250846` paise (`₹2,508.46`). Since `250846 × 3 = 752538`, **2 paise (`₹0.02`) vanish** from the Boarding Pass preview (`L1120, L1218`) and Member Selection rows (`L1465`).
    - Worse, `payerReimbursementDisplay` (`L671-L673`) computes `effectiveTotalPaise - perSelectedMemberSharePaise = 752540 - 250846 = 501694` (`₹5,016.94`), whereas `SplitMateMathEngine.splitEquallyZeroDrift` assigns the first extra penny (`250847`) to index `0` (the Payer), making the Payer's actual ledger reimbursement `₹5,016.93`—**overstating "Getting back" by `₹0.01`** and baking the un-reconciled `₹2,508.46/person` string into the persisted `ExpenseEntity.title` (`L431`)!

### `[HIGH]` 6.2 Unbounded Input Lengths, Multi-Dot NaN Revert, & `Long.MAX_VALUE` Ledger Wrap-Around
- **Locations**:
  - [`SplitMateAppComposable.kt:L1914, L3744, L3753, L3874`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L3753)
  - [`SplitMateViewModel.kt:L1081, L1152`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt#L1081)
  - [`PnrExpenseReviewScreen.kt:L356, L1113, L1356, L1442`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L1113)
- **Finding**:
  1. **No Character Limits on Names/Titles**: Group names (`L1914`), passenger names (`PnrExpenseReviewScreen.kt:L1356`), and expense titles (`L3744`) have no `.take(MAX_CHARS)` bound and lack `maxLines = 1` / `TextOverflow.Ellipsis` on `PnrExpenseReviewScreen.kt:L356, L1113, L1442`, allowing long names to stretch boarding pass rows to 15+ lines.
  2. **Multi-Decimal Silent Failure & `Long` Overflow in `EditLoggedExpenseDialog`**: `editedAmountStr` (`L3753`) filters `ch.isDigit() || ch == '.'`, allowing `"12.34.56"` (`toDoubleOrNull() == null`, silently discarding user edits) or a 25-digit number that saturates `round(newTotalRupees * 100.0).toLong()` (`SplitMateViewModel.kt:L1081`) to `Long.MAX_VALUE` and overflows `sumOf { it.totalAmountCents }` into negative numbers.

### `[HIGH]` 6.3 Accessibility (a11y) Failures: `0` Semantics/Roles, `55` Null ContentDescriptions, & Sub-48dp Touch Targets
- **Locations**:
  - [`SplitMateAppComposable.kt:L2078`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L2078) (`20.dp` `IconButton`), [`L3951`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L3951) (`AvatarToken` null `contentDescription`)
  - [`QuickExpenseAndGuideScreens.kt:L1742`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt#L1742) (`28.dp` circular buttons)
  - [`PnrExpenseReviewScreen.kt:L336, L371, L1389`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L336) (`42.dp` top bar buttons & custom checkbox rows without `Role.Checkbox`)
- **Finding**: There are **0** `Semantics` or `Role.Checkbox` annotations across the entire repository, **55** `contentDescription = null` calls, dual nested focusable targets in `ContactPickerBottomSheet` (`SplitMateTheme.kt:L628` & `L685`), and interactive touch targets as small as `20.dp × 20.dp` (`SplitMateAppComposable.kt:L2078`).

---

*End of Read-Only Principal Engineer Audit Report. No files were edited, staged, or committed.*

---

# Verbatim Specialized Subagent Deep-Dive Reports (Appendices A, B & C)

## Subagent Audit Appendix 1

The following is a <SYSTEM_MESSAGE> not actually sent by the user. It is provided by the system as important information to pay attention to.

<SYSTEM_MESSAGE>
[Message] timestamp=2026-09-22T18:54:58Z sender=3b72ce7d-2d5c-4191-841a-04928cf10fbe priority=MESSAGE_PRIORITY_HIGH content=# Principal Engineer Audit Report: Pillars 5 & 6

---

## PILLAR 5: API EXPOSURE, RATE LIMITS & NETWORK FRAGILITY

### 5.1 Auto-Input Cache Bypass & Per-PNR Cooldown Evasion (`PnrExpenseReviewScreen.kt` & `SplitMateTheme.kt`)
1. **Unconditional `forceManualRefresh = true` on Automatic Keystroke Lookup (`PnrExpenseReviewScreen.kt:234` & `488–493`)**:
   - In `PnrSearchLookupCard` (`PnrExpenseReviewScreen.kt:488–494`), the `onPnrChange` callback automatically invokes `triggerLivePnrLookup(digits)` the instant `digits.length == 10 && digits != liveSnapshot?.pnr && !isFetching`.
   - Inside `triggerLivePnrLookup` (`PnrExpenseReviewScreen.kt:231–236`), `fetchLivePnrAndTrainStatus` is hardcoded with `forceManualRefresh = true`—the exact same flag is also passed in `QuickExpenseAndGuideScreens.kt:298` (`triggerLivePnrLookup`).
   - In `SplitMateTheme.kt:1237`, setting `forceManualRefresh = true` downgrades `minWait` from `AUTO_SYNC_COOLDOWN_MS` (`6h` / `21,600,000 ms`, line `1048`) down to `MANUAL_REFRESH_DEBOUNCE_MS` (`60,000 ms`, line `1050`), completely bypassing the long-lived cache TTL even when a valid snapshot is already in `InMemoryPnrSnapshotCache` or SharedPreferences (`splitmate_pnr_rate_guard`).
2. **Per-PNR Cooldown Map vs. Global Rate Limiter (`SplitMateTheme.kt:1044–1046, 1228–1254`)**:
   - The rate-limit tracking structures (`InMemoryPnrSnapshotCache`, `InMemoryPnrLastFetchEpochMs`, and `InFlightPnrSet` at `SplitMateTheme.kt:1044–1046`, plus SharedPreferences keys `"last_sync_$cleanPnr"` at line `1231`) are **strictly keyed per-PNR (`cleanPnr`)**, with **zero global rate-limiting or token bucket across the client IP**.
   - **Attack / Failure Vector**: If a user edits the 10th digit in `PnrSearchLookupCard` (e.g., typing `1234567890`, backspacing one digit, typing `1`, `2`, `3`...), each distinct 10-digit string has `InMemoryPnrLastFetchEpochMs[cleanPnr] == null` (`0L`) and `cachedSnapshot == null`. Furthermore, because `!isFetching` only guards while the previous coroutine is suspended (`PnrExpenseReviewScreen.kt:491`), sequential or rapid edits fire unrestricted outbound scraping requests to RailYatri and ConfirmTkt with zero global cooldown, rapidly triggering Akamai/Cloudflare WAF IP bans (`HTTP 403/429`).
3. **Silent Cache Poisoning Prevention Bug on Failed Lookups (`SplitMateTheme.kt:1247–1254` vs `1549–1551`)**:
   - At `SplitMateTheme.kt:1247–1254`, `last_sync_$cleanPnr` is stamped into `InMemoryPnrLastFetchEpochMs` and SharedPreferences **before** the network socket opens. However, `savePersistedPnrSnapshot` is only called if `liveNetworkHit == true` (`line 1549`). If a network call fails (`liveNetworkHit == false`), `InMemoryPnrSnapshotCache[cleanPnr]` remains `null`. Because line `1236` checks `if (cachedSnapshot != null)` before enforcing `minWait`, a failed lookup **does not actually debounce retries**—subsequent taps on "Fetch PNR" see `cachedSnapshot == null` and immediately hit the network again!
4. **Unbounded SharedPreferences & RAM Cache Growth (`SplitMateTheme.kt:1044–1045, 1154–1158`)**:
   - `InMemoryPnrSnapshotCache` and `InMemoryPnrLastFetchEpochMs` (`ConcurrentHashMap`) and the `"splitmate_pnr_rate_guard"` XML file store `"snapshot_json_$cleanPnr"` and `"last_sync_$cleanPnr"` indefinitely without an LRU eviction policy or TTL cleanup. Moreover, `clearLocalVault()` (`SplitMateViewModel.kt:441–457`) clears Room tables (`dao?.clearAllLedgerData()`) but **never clears `splitmate_pnr_rate_guard` SharedPreferences or `InMemoryPnrSnapshotCache`**, leaking historical PNR numbers across "Reset App Data".

### 5.2 `HttpURLConnection` Timeouts, User-Agent Spoofing & SSR HTML/Regex Scraping Fragility (`SplitMateTheme.kt:1278–1422`)
1. **Worst-Case Sequential Blocking Latency (`21.0 Seconds`) (`SplitMateTheme.kt:1280–1283, 1379–1382`)**:
   - Primary scraper (`https://www.railyatri.in/m/pnr-status/$cleanPnr`) configures `connectTimeout = 6000` and `readTimeout = 6000` (up to `12,000 ms`).
   - Secondary fallback (`https://www.confirmtkt.com/pnr-status/$cleanPnr`) configures `connectTimeout = 4500` and `readTimeout = 4500` (up to `9,000 ms`).
   - Neither `HttpURLConnection` calls `.disconnect()` in a `finally` block. If RailYatri times out and ConfirmTkt times out, the user's coroutine hangs for **21 seconds** before falling back.
2. **Hardcoded User-Agent Spoofing (`SplitMateTheme.kt:1284–1287, 1383–1386`)**:
   - Spoofs `"Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"` without matching `Sec-CH-UA`, `Accept-Language`, or TLS JA3 fingerprints, which modern Akamai/Cloudflare bot detectors flag when paired with `Java/HttpURLConnection` default headers.
3. **Extreme Fragility of HTML Regex & Next.js `__NEXT_DATA__` Scraping (`SplitMateTheme.kt:1291–1296, 1390–1396`)**:
   - **RailYatri (`line 1291`)**: `Regex("""<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)`. Any attribute reordering by Next.js (e.g., `type="application/json" id="__NEXT_DATA__"` or `crossorigin="anonymous"`), or migration to React Server Components (RSC `__next_f.push`), immediately breaks extraction.
   - **ConfirmTkt (`lines 1390–1396`)**: Scrapes raw HTML via regexes like `Regex(""""TrainNo"\s*:\s*"(\d{5})"""")` and `Regex(""""CurrentStatus"\s*:\s*"([^"]+)"""")`. If `"CurrentStatus"` appears anywhere else in the page's embedded JSON state (e.g., ads, related trains, or template metadata), `findAll` (`line 1395`) pollutes `scrapedPassengers` with bogus passenger rows.
4. **Silent Synthetic Fabrication When Scraping Fails (`SplitMateTheme.kt:1425–1496`)**:
   - When `liveNetworkHit == false`, `fetchLivePnrAndTrainStatus` **fabricates fake train and passenger data** using `cleanPnr.hashCode()` (`line 1427`: `val hashIdx = (cleanPnr.hashCode().let { if (it < 0) -it else it }) % catalogKeys.size`, note: `Int.MIN_VALUE` stays negative on `-it`!) and `cleanPnr.lastOrNull()` (`lines 1455–1494`, generating fake `PQWL 14 -> WL 7`, `RAC 6`, or `CNF B2-45 LB` passengers).
   - **Critical Bug in `PnrExpenseReviewScreen.kt:238`**: The screen checks:
     `if (fetched.totalFareRupees > 0 || fetched.trainNo.isNotBlank() || fetched.structuredPassengers.isNotEmpty())`
     Because `fetchLivePnrAndTrainStatus` **always** populates `resolvedTrainNo` (`line 1426`) and `scrapedStructuredPassengers` (`lines 1456–1494`) from the offline fallback, **this condition is ALWAYS `true` (`100%` of the time)**! Thus, line `246` (`fetchError = "Could not fetch live IRCTC details..."`) is **unreachable dead code**, and a user offline or entering a non-existent 10-digit PNR is shown a fabricated train boarding pass with `₹0` total fare (`effectiveTotalPaise == 0L`), which disables the Confirm button (`canConfirmExpense` requires `effectiveTotalPaise > 0L` at line `315`) with **no error banner explaining why**!

### 5.3 Third-Party DiceBear Avatar Network Calls (`SplitMateTheme.kt:313–323`, `Components.kt`, `SplitMateAppComposable.kt`, `QuickExpenseAndGuideScreens.kt`, `OnboardingAndSettingsScreens.kt`)
1. **PII Leakage to External API (`SplitMateTheme.kt:313–323`)**:
   - `buildDiceBearOpenPeepsUrl` sends `seed=${Uri.encode(seedBase)}` directly to `https://api.dicebear.com/9.x/open-peeps/svg?...`.
   - Every user profile name and every phonebook contact name imported into a group (`QuickExpenseAndGuideScreens.kt:978, 1558, 1693`; `SplitMateAppComposable.kt:3924`; `OnboardingAndSettingsScreens.kt:137, 484`) is transmitted in cleartext query parameters to an external third-party server (`api.dicebear.com`) without user consent.
2. **Unthrottled Per-Keystroke Network Requests (`OnboardingAndSettingsScreens.kt:133–138, 480–485` & `QuickExpenseAndGuideScreens.kt:1557–1559, 1693`)**:
   - In `OnboardingSetupScreen` (`lines 133–138, 242–247`), `UserSettingsScreen` (`lines 480–485, 578–583`), and `EditFriendUpiDialog` (`lines 1557–1559`), `diceBearSvgUrl` is recomputed via `remember(nameText)` / `remember(editedName)` on **every single character keystroke** in the Name `OutlinedTextField`, triggering a live HTTP GET + SVG decode via `AsyncImage` for every intermediate substring (`"A"`, `"Ak"`, `"Aks"`, `"Aksh"`, `"Aksha"`, `"Akshay"`).
3. **Missing Explicit Disk/Memory Cache Keys Outside `DiceBearAvatar` (`SplitMateAppComposable.kt:3945–3956`, `QuickExpenseAndGuideScreens.kt:1027–1039, 1605–1616, 1691–1699`)**:
   - While `DiceBearAvatar` in `Components.kt:72–80` sets explicit `.diskCacheKey(...)` and `CachePolicy.ENABLED` (though `DiceBearAvatar` is **unused** anywhere in the app!), the actual avatar instances rendered across the app (`AvatarToken` at `SplitMateAppComposable.kt:3945`, `QuickExpenseScreen` at `1027`, `EditFriendUpiDialog` at `1605` & `1691`, `OnboardingSetupScreen` at `242`, `UserSettingsScreen` at `578`) instantiate raw `ImageRequest.Builder(context).data(url).decoderFactory(SvgDecoder.Factory()).build()` on each recomposition without custom cache keys or fallback drawables (except `OnboardingSetupScreen`).

---

## PILLAR 6: RUTHLESS NITPICKING (MATH DRIFT, OVERFLOWS, A11Y & 1PX BUGS)

### 6.1 Mathematical & Remainder Drift in `PnrExpenseReviewScreen.kt` & `SplitMateViewModel.kt`
1. **Truncated Integer Division Drift in UI Preview vs. Largest Remainder Commit (`PnrExpenseReviewScreen.kt:278–279, 431, 465, 645, 670–673`)**:
   - At `PnrExpenseReviewScreen.kt:278–279`:
     ```kotlin
     val perPersonTicketSharePaise = if (effectiveDividerCount > 0) effectiveTotalPaise / effectiveDividerCount else 0L
     val perSelectedMemberSharePaise = if (selectedMembersList.isNotEmpty()) effectiveTotalPaise / selectedMembersList.size else perPersonTicketSharePaise
     ```
   - **Concrete Proof of Drift**: Suppose a 3-passenger 3A ticket has `totalFareRupees = 7500` (`baseFarePaise = 750000`), `irctcConvenienceFeeUpiPaise = 2360`, and `travelInsurancePaise = 3 * 45 = 135`.
     - `effectiveTotalPaise = 750000 + 2360 + 135 = 752495` paise (`₹7,524.95`). Or with `totalFareRupees = 7500` and 4 passengers (`4 * 45 = 180`), `752540` paise (`₹7,525.40`) split among `3` selected members:
     - `perSelectedMemberSharePaise = 752540 / 3 = 250846` paise (`₹2,508.46`).
     - Since `250846 * 3 = 752538` paise, **2 paise (`₹0.02`) vanish from the UI preview**:
       - Boarding Pass Passenger Rows (`line 645` -> `1120`): displays `Per-Passenger Share: ₹2,508.46` for all 3 passengers.
       - Boarding Pass Bottom Stub (`line 1218`): displays `₹2,508.46 / each`.
       - Member Split Selection Card (`line 670` -> `1465`): displays `₹2,508.46` for **every** selected member (never showing `+₹0.01` for the first two members!).
       - **Payer Reimbursement Overstatement Bug (`lines 671–673` -> `1473`)**:
         ```kotlin
         payerReimbursementDisplay = formatPaiseDisplay(
             (effectiveTotalPaise - (if (selectedMembersList.any { it.isCurrentUser }) perSelectedMemberSharePaise else 0L)).coerceAtLeast(0L)
         )
         ```
         Because `perSelectedMemberSharePaise` is floored (`250846`), `effectiveTotalPaise - perSelectedMemberSharePaise` computes `752540 - 250846 = 501694` (`₹5,016.94`). However, when `commitQuickEqualExpense` runs `SplitMateMathEngine.splitEquallyZeroDrift` (`SplitMateMathEngine.kt:79–92`), index `0` (`isCurrentUser`, the Payer!) is assigned the extra penny (`baseFloor + 1L = 250847`), while member `1` gets `250847` and member `2` gets `250846`. Thus, the Payer's actual reimbursement in the ledger is `250847 + 250846 = 501693` (`₹5,016.93`)—meaning `PnrExpenseReviewScreen` **overstates the Payer's "Getting back" amount by `₹0.01`**!
       - **Persisted Expense Title Drift (`line 431`)**: Even worse, `perSelectedMemberSharePaise` (`₹2,508.46/person`) is baked permanently into `coachAndSeats` inside the Expense title (`line 431`), disagreeing with the actual `ExpenseSplitEntity` rows (`₹2,508.47`, `₹2,508.47`, `₹2,508.46`)!
2. **Identical Truncation in `resolveExpenseSplitBreakdown` (`SplitMateViewModel.kt:1218–1219`)**:
   - `val perPersonAvgCents = expense.totalAmountCents / splittingCount` is used for `perPersonHeadlineShare` (`line 1219`, rendered in `SplitMateAppComposable.kt:3569`), dropping the remainder paise without any `+Largest Remainder` indicator (unlike `QuickExpenseAndGuideScreens.kt:897` which explicitly appends `+₹... Largest Remainder`).

### 6.2 Input Validation, String Length & Numeric Overflow Edge Cases
1. **Zero Length Caps on Group Name, Friend Name, Expense Title & UPI ID**:
   - None of the text inputs across the entire codebase enforce a `.take(MAX_LEN)` length bound:
     - **Group Name (`SplitMateAppComposable.kt:1914`)**: `onValueChange = { groupNameInput = it }`. Entering a 500-character group name persists a 500-char string to Room (`SplitMateViewModel.kt:693`), overflows the `PnrExpenseReviewScreen` top bar subtitle (`PnrExpenseReviewScreen.kt:356`, which lacks `maxLines = 1` and `overflow = TextOverflow.Ellipsis`!), and bloats the warning banner (`PnrExpenseReviewScreen.kt:512, 559` & `1306, 1344`).
     - **Friend Name (`PnrExpenseReviewScreen.kt:1356`, `OnboardingAndSettingsScreens.kt:327, 594`)**: `onValueChange = { quickAddName = it }` / `{ nameText = it }`. A 500-character passenger/friend name wraps uncontrollably in `TactilePaperBoardingPass` (`PnrExpenseReviewScreen.kt:1113`, no `maxLines`!) and `MemberSplitSelectionCard` (`PnrExpenseReviewScreen.kt:1442`, no `maxLines`!), pushing the status badges and rupee amounts off-screen horizontally (`Row` with `Arrangement.SpaceBetween` where the left `Row` has `Modifier.weight(1f)` at `1089` and `1401`, crushing the right column or stretching the row height to 15+ lines).
     - **Expense Title (`QuickExpenseAndGuideScreens.kt:393`, `SplitMateAppComposable.kt:3744`)**: `onValueChange = { draftTitle = it }` / `{ editedTitle = it }`. A 500-character title overflows `ActivityItemRow` (`SplitMateAppComposable.kt:4025`, no `maxLines`!) and `ClaimItemRow` (`SplitMateAppComposable.kt:4062`, no `maxLines`!).
2. **Numeric Overflow & Validation Flaws in `EditLoggedExpenseDialog` (`SplitMateAppComposable.kt:3753, 3874`) & `SplitMateViewModel.kt:1081`**:
   - In `EditLoggedExpenseDialog` (`SplitMateAppComposable.kt:3753`):
     `onValueChange = { editedAmountStr = it.filter { ch -> ch.isDigit() || ch == '.' } }`
     - **Multiple Decimal Points**: Filtering only `ch.isDigit() || ch == '.'` allows inputs like `"12.34.56"`, for which `toDoubleOrNull()` (`line 3874`) returns `null` and silently reverts to the old `expense.totalAmountCents / 100.0` upon clicking "Save Changes"!
     - **Unbounded Digits / `Long` Saturation**: Entering a 30-digit number (e.g. `9999999999999999999999`) causes `kotlin.math.round(newTotalRupees * 100.0).toLong()` (`SplitMateViewModel.kt:1081`) to saturate to `Long.MAX_VALUE` (`9,223,372,036,854,775,807` paise), which then **overflows `Long` addition** (`netMap[exp.payerId] + exp.totalAmountCents`) in `computeGroupNetBalances` (`SplitMateViewModel.kt:1152`) and `sumOf { it.totalAmountCents }` (`SplitMateAppComposable.kt:547`), wrapping the ledger balance into negative trillions!
3. **`QuickExpenseScreen` Keypad & Division-by-Zero Edge Cases (`QuickExpenseAndGuideScreens.kt:214–216, 935–939, 988–992, 1140–1142, 1423–1436`)**:
   - **Division-by-Zero Guard**: In `QuickExpenseScreen` (`lines 988–992`), unlike `PnrExpenseReviewScreen` (which keeps at least 1 member selected at `line 658`), a user **can** deselect all members (`selectedMemberIds` becomes `emptySet()`, `memberCount == 0`). Lines `215–216` safely guard `if (memberCount > 0) totalAmountPaise / memberCount else 0L`, and `canCommitSplit` (`line 1239`) blocks saving when `selectedMemberIds.isEmpty()`.
   - **Decimal Input Length Asymmetry Bug (`QuickExpenseAndGuideScreens.kt:1140` vs `1429`)**:
     - `appendDigit` (`line 1429`) allows up to `8` integer digits (`99,999,999` = `₹9,99,99,999`).
     - The `.` key (`line 1140`) checks `if (!amountDigits.contains(".") && amountDigits.length < 9)`, allowing `"99999999."` (length 9).
     - `appendDigit` (`line 1426`) then checks `if (decimals.length < 2 && current.length < 11)`, allowing `"99999999.99"` (`11` chars).
     - However, when `99,999,999.99` is formatted as `"₹ 9,99,99,999.99"` (`16` characters) inside the `48.sp` `Text` (`QuickExpenseAndGuideScreens.kt:859–869`), which has **no `maxLines = 1` or auto-sizing**, the `48.sp` amount **wraps onto 2 lines** (`104.dp` height), crushing the vertical `Arrangement.SpaceBetween` layout on compact screens!

### 6.3 Accessibility (a11y), Missing Semantics, Sub-48dp Touch Targets & Deprecations
1. **Complete Absence of `Semantics` & `Role` Annotations (`0` occurrences across entire codebase)**:
   - `grep_search` for `semantics` and `Role.` across `/usr/local/google/home/karadkar/splitmate/android/app/src/main` returns **0 matches**.
   - Custom toggleable rows and chips lack `Role.Checkbox`, `Role.Switch`, `Role.RadioButton`, or `Role.Tab` and `toggleableState`:
     - `PnrExpenseReviewScreen.kt:1389–1396`: Member split inclusion row uses `.clickable { onToggleMember(member.memberId) }` with no `Role.Checkbox` or state announcement.
     - `QuickExpenseAndGuideScreens.kt:985–999`: Participant avatar selection uses `.combinedClickable(...)` without `Role.Checkbox` or `selected` semantics.
     - `SplitMateTheme.kt:625–691`: `ContactPickerBottomSheet` contact row has `.clickable { toggleSelection() }` on the outer `Surface` (`line 628`) **AND** an interactive `Checkbox(checked = isChecked, onCheckedChange = { toggleSelection() })` inside (`lines 683–690`), creating **duplicate nested focusable targets** for TalkBack instead of `Modifier.toggleable(..., role = Role.Checkbox)` with `onCheckedChange = null`.
     - `OnboardingAndSettingsScreens.kt:755–782, 843–872, 876–909`: `SettingsRowItem` wraps a clickable `Surface` (`onClick = ...`) around an interactive `Switch(onCheckedChange = ...)` (`lines 764, 852, 885`), again creating nested dual-focus targets for screen readers.
2. **55 Instances of `contentDescription = null` (Including Meaningful & Interactive Icons)**:
   - Total count across `src/main`: **55 occurrences** (`SplitMateAppComposable.kt`: 31, `QuickExpenseAndGuideScreens.kt`: 9, `OnboardingAndSettingsScreens.kt`: 8, `PnrExpenseReviewScreen.kt`: 5, `SplitMateTheme.kt`: 4, `Components.kt`: 2 — note: 4 of the 59 grep matches are inside `SplitMateAppComposable.kt` avatar/icon helpers).
   - Notable a11y failures where `contentDescription = null` strips semantic meaning:
     - `SplitMateAppComposable.kt:3951`: `AvatarToken`'s `AsyncImage` sets `contentDescription = null`, so avatars in `OverlappingAvatarStack` (`line 3965`) and `ClaimItemRow` (`line 4072` — where "Claimed by:" **only** renders `AvatarToken` icons without text names!) are **completely invisible to TalkBack** (screen reader users cannot tell who claimed an item in `ClaimItemRow`!).
     - `SplitMateTheme.kt:1854`: Route direction arrow (`Icons.Rounded.ArrowForward`) between `fromSt` and `toSt` has `contentDescription = null`, so TalkBack reads `"SBC NDLS"` instead of `"SBC to NDLS"`.
3. **Touch Targets Violating the `48dp × 48dp` Android Accessibility Minimum**:
   - **`20.dp × 20.dp` IconButton (`SplitMateAppComposable.kt:2078`)**: Inside `CreateGroupDialog`, the remove-member `'X'` `IconButton` has `modifier = Modifier.size(20.dp)` (`28dp` smaller than the `48dp` minimum!).
   - **`28.dp × 28.dp` Style Toggle Buttons (`QuickExpenseAndGuideScreens.kt:1742`)**: Inside `EditFriendUpiDialog`, the inline `"M"`, `"F"`, `"N"` circular `Surface(onClick = ...)` buttons have `modifier = Modifier.size(28.dp)`.
   - **`36.dp` / `38.dp` / `40.dp` Height Segment Buttons**:
     - `QuickExpenseAndGuideScreens.kt:1795`: Avatar style pill `Surface(onClick = ...)` has `.height(36.dp)`.
     - `OnboardingAndSettingsScreens.kt:689`: Settings avatar style pill `Surface(onClick = ...)` has `.height(38.dp)`.
     - `OnboardingAndSettingsScreens.kt:305`: Onboarding avatar style pill `Surface(onClick = ...)` has `.height(40.dp)`.
   - **`42.dp × 42.dp` TopBar Navigation & Group Switcher Buttons (`PnrExpenseReviewScreen.kt:336, 371`)**:
     - Both the circular Back button (`line 336`) and Switch Group button (`line 371`) use `Surface(onClick = ..., modifier = Modifier.size(42.dp))`, which overrides Material 3's `minimumInteractiveComponentSize` when `size(42.dp)` constrains the layout bounds, falling `6.dp` short of `48.dp`.
   - **Compact Action Pills with `< 32.dp` Total Vertical Height**:
     - `SplitMateTheme.kt:1987–2009` ("Refresh Live CNF/WL & Save") & `2025–2040` ("ConfirmTkt ↗"): `padding(vertical = 5.dp)` + `13.dp` icon = `~23.dp` height.
     - `SplitMateAppComposable.kt:3575–3602` ("Edit" & "Undo" expense pills): `padding(vertical = 6.dp)` + `14.dp` icon = `~26.dp` height.
     - `PnrExpenseReviewScreen.kt:764–796` ("Fetch PNR" button): `padding(vertical = 9.dp)` + `15.dp` icon = `~33.dp` height.
     - `PnrExpenseReviewScreen.kt:1314–1328` ("Select X Pax" pill): `padding(vertical = 5.dp)` + `11.sp` text = `~25.dp` height.
4. **Compiler Deprecation Warnings & RTL Icon Bugs**:
   - **Deprecated Non-AutoMirrored Icons (`SplitMateTheme.kt:1853` & `SplitMateAppComposable.kt:577`)**:
     - `SplitMateTheme.kt:1853` uses `Icons.Rounded.ArrowForward` (deprecated in Compose Material Icons in favor of `Icons.AutoMirrored.Rounded.ArrowForward`).
     - `SplitMateAppComposable.kt:577` uses `Icons.Rounded.ArrowBack` (deprecated in favor of `Icons.AutoMirrored.Rounded.ArrowBack`).
   - **Deprecated `Locale(String, String)` Constructor (`QuickExpenseAndGuideScreens.kt:1458, 1475`)**:
     - `Locale("en", "IN")` is deprecated since Java 19 / Android API 34 in favor of `Locale.forLanguageTag("en-IN")` or `Locale.Builder()`.
   - **Deprecated `SplitMateTab.values()` (`SplitMateAppComposable.kt:403`)**:
     - `SplitMateTab.values().forEach` triggers a Kotlin 1.9+/2.0 compiler warning in favor of `SplitMateTab.entries.forEach`.
   - **Suppressed Hardware Vibrator Deprecations (`SplitMateTheme.kt:849, 859`)**:
     - Uses `@Suppress("DEPRECATION")` for `Context.VIBRATOR_SERVICE` and `vibrator.vibrate(Long)` on pre-S / pre-O SDK branches.
</SYSTEM_MESSAGE>

---

## Subagent Audit Appendix 2

The following is a <SYSTEM_MESSAGE> not actually sent by the user. It is provided by the system as important information to pay attention to.

<SYSTEM_MESSAGE>
[Message] timestamp=2026-09-22T18:56:01Z sender=5bb1028b-3444-4c68-a38a-3e4b04cfd020 priority=MESSAGE_PRIORITY_HIGH content=# Principal Engineer Technical Audit — Pillars 3 & 4

Below is the read-only Principal Engineer audit of **Pillar 3 (App Size, Bloat & Stale Code)** and **Pillar 4 (Architecture, DB, Storage & Security)** across `/usr/local/google/home/karadkar/splitmate/android`, with exact file paths, byte/line metrics, and line numbers.

---

## PILLAR 3: APP SIZE, BLOAT & STALE CODE

### 3.1 Root Causes of APK Bloat (`splitmate-1.8.7.apk` = 17.09 MB / `17,085,021` bytes)
| Artifact / Config | Path & Line Numbers | Size / Metric | Defect & Impact |
| :--- | :--- | :--- | :--- |
| **Debug & Release APK Output** | `android/app/build/outputs/apk/debug/splitmate-1.8.7.apk`<br>`splitmate-1.8.4.apk` (repo root) | `17,085,021 B` (17.09 MB)<br>`16,981,793 B` (16.98 MB) | A 16.98 MB binary (`splitmate-1.8.4.apk`) is committed directly to the git repository root, and `splitmate-1.8.7.apk` is an unminified build artifact. |
| **R8 & Resource Shrinking Disabled** | `android/app/build.gradle:22-29` | `minifyEnabled false`<br>Missing `shrinkResources true` | `release` buildType explicitly sets `minifyEnabled false`, omits `shrinkResources true` (`isShrinkResources = true`), and signs release builds with `signingConfigs.debug` (`line 25`). Furthermore, `applicationVariants.all` (`lines 31-35`) renames **both** `debug` and `release` outputs to `splitmate-${variant.versionName}.apk`, masking whether an artifact is a debug build. |
| **Un-shaken `material-icons-extended`** | `android/app/build.gradle:80` | ~10–12 MB DEX bloat | `implementation 'androidx.compose.material:material-icons-extended'` bundles thousands of Material icon vector factories. Because `minifyEnabled` is `false`, R8 never tree-shakes unused icons—even though the entire codebase only uses ~30 icons. |
| **Triplicated Font Assets (Static TTF + Unused Variable TTF + GMS Downloadable Fonts)** | `android/app/src/main/res/font/`<br>`android/app/build.gradle:77`<br>`ui/SplitMateTheme.kt:150-172`<br>`res/values/font_certs.xml` | `350,716 B` (`res/font/`) + `4,184 B` (`font_certs.xml`) + GMS library overhead | 1) `res/font/figtree_variable.ttf` (`62,712 B`) has **zero references** anywhere in `src/main`.<br>2) 5 static TTF files (`figtree_regular.ttf` `57,504 B`, `figtree_medium.ttf` `57,316 B`, `figtree_semibold.ttf` `57,584 B`, `figtree_bold.ttf` `57,672 B`, `figtree_extrabold.ttf` `57,928 B` = `288,004 B`) are bundled locally.<br>3) `SplitMateTheme.kt:150-172` and `build.gradle:77` (`androidx.compose.ui:ui-text-google-fonts:1.6.6`) simultaneously configure GMS Downloadable Fonts (`GoogleFont("Figtree")`) for the exact same 5 weights. |
| **WebView Assets (`src/main/assets/`) Status** | `android/app/src/main/assets/`<br>`MainActivity.kt:16-19` | `0 B` (Directory removed) | `android/app/src/main/assets/` no longer exists (`index.html`, `splitmate-ui.js`, `stitch-interactive.js` were already purged from `src/main`). `MainActivity.kt:16-19` uses pure Jetpack Compose (`setContent { SplitMateApp(...) }`). Also note `android/app/src/main/res/mipmap-xxxhdpi/` is an empty directory. |

---

### 3.2 Monolithic "God-Files" & Separation-of-Concerns Violations
The entire `src/main/java` source tree consists of **12 Kotlin files totaling 12,652 lines (621,549 bytes)**, dominated by 5 monolithic files that mix UI, networking, HTML scraping, Android ContentResolver queries, and theme tokens:

1. **`ui/SplitMateAppComposable.kt` — `4,088 lines` (`215,565 bytes`)**
   - Declares `package com.splitmate.app` (`line 1`) despite residing in `com/splitmate/app/ui/`.
   - Packs `SplitMateTheme` global singleton (`lines 77-115`), top-level `SplitMateApp` NavHost (`lines 125-445`), `SplitMateMainDashboardScaffold` (`lines 448-561`), `LedgersDashboardScreen` (`lines 564-1125`), `SplitMateBottomNavigationBar` (`lines 1128-1182`), `NewGroupDialog`, `AddFriendDialog`, `SettleUpVerifyDialog`, `EditLoggedExpenseDialog` (`lines 1185-2299`), `GreedySettlementScreen` (`lines 2302-3265`), `AuditVaultScreen` (`lines 3268-3781`), and shared cards/badges (`lines 3784-4088`) into a single file.
2. **`ui/SplitMateTheme.kt` — `2,048 lines` (`98,943 bytes`)**
   - Nominally a theme file, yet only `lines 1-365` relate to colors/typography. The remaining **1,683 lines** contain:
     - **Device Contacts ContentResolver Engine & Phone Normalizer**: `DeviceContactItem`, `normalizePhoneTo10Digits`, `queryAllDeviceContacts` (`lines 367-428`).
     - **Full UI Modal**: `ContactPickerBottomSheet` (`lines 430-731`).
     - **Hardware Vibrator / Haptic Engine**: `performCrispTactileHaptic` (`lines 828-864`).
     - **Static Indian Railways Databases**: `OfflineIndianTrainCatalog`, `OfflineTrainIntermediateRadar`, `OfflineStationNames` (`lines 976-1024`).
     - **Live IRCTC / RailYatri / ConfirmTkt HTML Scraper & JSON Persistence Engine**: `LivePnrTrainSnapshot`, `fetchLivePnrAndTrainStatus`, `estimateTrainDelayMinutes`, `parseLiveStatusString` (`lines 914-1607`) using raw `java.net.HttpURLConnection` and regex parsing inside a UI theme file.
     - **Boarding Pass UI Composable**: `GroupBoardingPassCard` (`lines 1701-2045`).
3. **`ui/screens/QuickExpenseAndGuideScreens.kt` — `1,934 lines` (`102,407 bytes`)**
   - Contains duplicate theme object `QuickExpenseThemeTokens` (`lines 70-100`), `QuickExpenseScreen` (`lines 124-1488`), and `EditFriendUpiDialog` (`lines 1490-1932`).
4. **`ui/screens/PnrExpenseReviewScreen.kt` — `1,521 lines` (`73,322 bytes`)**
   - Contains PNR passenger fare calculation logic, `PnrPassengerSplitRow`, `PnrFareSplitMode`, and `PnrExpenseReviewScreen`.
5. **`ui/SplitMateViewModel.kt` — `1,283 lines` (`52,121 bytes`)**
   - God-ViewModel holding 31 fields in a single `SplitMateUiState` data class (`lines 54-120`), 7 full-table Room collectors, seeding logic, contact resolution, PNR expense creation, and orphaned receipt itemization logic.
6. **`ui/screens/OnboardingAndSettingsScreens.kt` — `1,102 lines` (`54,267 bytes`)**
   - Contains another duplicate theme object `SplitMateThemeTokens` (`lines 52-77`), `GuestOnboardingScreen`, and `ProfileAndSettingsScreen`.

---

### 3.3 100% Dead Code, Unused Composables & Redundant Implementations

1. **`ui/Components.kt` (`236 lines`, `10,809 bytes`) is 100% UNUSED DEAD CODE**:
   - **`DiceBearAvatar` (`Components.kt:63-95`)**: **0 call sites** across the entire repository.
     - Instead of calling `DiceBearAvatar`, developers copy-pasted `AsyncImage(model = ImageRequest.Builder(LocalContext.current).data("https://api.dicebear.com/9.x/micah/svg?seed=...").decoderFactory(SvgDecoder.Factory()).crossfade(true).build(), ...)` **6 separate times**:
       1. `ui/SplitMateAppComposable.kt:3945-3956` (`MemberAvatarBadge`)
       2. `ui/screens/OnboardingAndSettingsScreens.kt:242-255` (`GuestOnboardingScreen`)
       3. `ui/screens/OnboardingAndSettingsScreens.kt:578-589` (`ProfileAndSettingsScreen`)
       4. `ui/screens/QuickExpenseAndGuideScreens.kt:1027-1039` (`QuickExpenseScreen`)
       5. `ui/screens/QuickExpenseAndGuideScreens.kt:1605-1616` (`EditFriendUpiDialog`)
       6. `ui/screens/QuickExpenseAndGuideScreens.kt:1691-1699` (`EditFriendUpiDialog`)
     - Worse yet, `SplitMateApplication.kt:18-39` already registers `SvgDecoder.Factory()` globally in `newImageLoader()`, making every per-call `.decoderFactory(SvgDecoder.Factory())` allocation redundant.
   - **`HorizontalFloatingToolbar` (`Components.kt:106-235`)**: **0 call sites** in production code.
     - Instead, `SplitMateAppComposable.kt:313` attaches `.testTag("HorizontalFloatingToolbar")` onto a standard `Column` wrapping `SplitMateBottomNavigationBar` solely to pass UI tests looking for that test tag.
2. **Dead Composables & Unused Typography Aliases**:
   - **`ClaimItemRow` (`ui/SplitMateAppComposable.kt:4041-4087`)**: **0 call sites** in production or test code.
   - **`SplitMateCircularLogoBadge` (`ui/SplitMateTheme.kt:1651-1698`)**: **0 call sites** anywhere in the codebase.
   - **`PlusJakartaSansFont` & `JetBrainsMonoFont` (`ui/SplitMateTheme.kt:174-175`)**: Unused font aliases (`val PlusJakartaSansFont = FigtreeFontFamily`, `val JetBrainsMonoFont = FigtreeFontFamily`) with 0 external references.
3. **Orphaned Receipt Itemization & Proportional Split Engine (`SplitMateViewModel.kt` & `SplitMateMathEngine.kt`)**:
   - While `QuickExpenseScreen` (`SplitMateAppComposable.kt:349`) replaced the old receipt-claiming tab, the entire Collaborative Receipt Claim & Remainder Engine remains in `SplitMateViewModel.kt` and `SplitMateMathEngine.kt` with **zero UI call sites** (referenced only by unit tests in `SplitMateViewModelTurbineTest.kt`):
     - `ReceiptLineItem` model & `defaultSeedReceiptItems()` (`SplitMateViewModel.kt:28-35, 1179-1185`)
     - `SplitMateUiState` fields: `receiptItems`, `receiptSubtotalCents`, `receiptTaxAndTipCents`, `receiptGrandTotalCents`, `auxiliaryMultiplier`, `unassignedRemainderCents`, `activeClaimerId`, `receiptPayerId`, `lastCommittedExpenseId` (`SplitMateViewModel.kt:80-88`)
     - ViewModel methods: `setClaimerPersona` (`line 819`), `setReceiptPayer` (`line 823`), `updateReceiptTaxAndTip` (`line 827`), `toggleReceiptItemClaim` (`line 831`), `addReceiptLineItem` (`line 844`), `splitUnassignedRemainderEqually` (`line 858`), `commitCollaborativeExpense` (`line 879`), `recomputeReceiptState` (`lines 1128-1161`)
     - Math engine: `SplitMateMathEngine.calculateProportionalReceiptSplits` (`SplitMateMathEngine.kt:101-176`)
4. **Orphaned Currency Sync Engine & Unused DAO Queries**:
   - `SplitMateViewModel.syncLiveCurrencyRatesFromFrankfurter()` (`lines 561-569`) is a hardcoded no-op stub (`isSyncingRates = false, activeCurrencyCode = "INR"`).
   - `SplitMateUiState.activeCurrency` (`lines 62-63`) is a constant property returning `CurrencyRateEntity("INR", "Indian Rupee", "₹", 83.95)` regardless of `availableCurrencies`.
   - **4 unused `@Dao` methods in `data/SplitMateDao.kt`**:
     - `getCurrencyRate(code: String)` (`line 18`) — 0 call sites.
     - `observeGroupMembers(groupId: String)` (`line 28`) — 0 call sites (ViewModel loads all members globally instead).
     - `markPendingExpensesSynced()` (`line 84`) — 0 call sites (`isSynced` column in `ExpenseEntity` is never read or updated).
     - `deleteUserProfile()` (`line 97`) — 0 call sites (`resetAndClearAllData` at `SplitMateViewModel.kt:944-977` calls `clearAllUserProfiles()` at `line 950` and then immediately calls `clearAllUserProfiles()` **a second time** at `line 957`).

---

## PILLAR 4: ARCHITECTURE, DB, STORAGE & SECURITY

### 4.1 Room Database Schema, Missing Foreign Keys, Missing Indices & Destructive Migrations

1. **`fallbackToDestructiveMigration()` in Production (`data/SplitMateRoomDatabase.kt:18-37`)**:
   - `SplitMateRoomDatabase` is at `version = 4` (`line 18`) with `exportSchema = false` (`line 19`) and zero `Migration` paths.
   - `Room.databaseBuilder(...).fallbackToDestructiveMigration().build()` (`lines 31-37`) permanently destroys the user's entire financial history (`groups`, `group_members`, `expenses`, `expense_splits`, `settlements`, `user_profile`) on any schema version increment.
2. **Zero SQLite `@ForeignKey` Constraints Across All Relational Tables (`data/RoomEntities.kt`)**:
   - None of the child tables declare `foreignKeys = [ForeignKey(..., onDelete = ForeignKey.CASCADE)]`:
     - `GroupMemberEntity` (`lines 35-49`): `groupId` has no foreign key to `ExpenseGroupEntity.groupId`.
     - `ExpenseEntity` (`lines 58-77`): `groupId` and `payerId` have no foreign keys to `ExpenseGroupEntity.groupId` or `GroupMemberEntity.memberId`.
     - `ExpenseSplitEntity` (`lines 80-95`): `expenseId` and `memberId` have no foreign keys to `ExpenseEntity.expenseId` or `GroupMemberEntity.memberId`.
     - `SettlementEntity` (`lines 98-114`): `groupId`, `fromMemberId`, and `toMemberId` have no foreign keys.
   - **Consequence**: Deleting a group or member leaves orphaned expenses, splits, and settlements in SQLite, which then corrupt global balance calculations (`computeOverallUserBalanceCents` iterates over `state.groups`, while `state.expenses` and `state.splits` retain orphaned rows). Additionally, multi-table mutations like `deleteLoggedExpense` (`SplitMateViewModel.kt:1015-1016`) and `editExistingExpense` (`lines 1106-1109`) execute separate `dao.deleteSplitsForExpense(...)` and `dao.insertExpenseSplits(...)` calls outside of a Room `@Transaction`.
3. **Missing Secondary Indices (`data/RoomEntities.kt` & `data/SplitMateDao.kt`)**:
   - `ExpenseEntity` (`RoomEntities.kt:58-61`) only indexes `groupId`. It lacks an index on `createdAt` (used by `SELECT * FROM expenses ORDER BY createdAt DESC` at `SplitMateDao.kt:44`, forcing a filesort on every emission) and `payerId`.
   - `SettlementEntity` (`RoomEntities.kt:98-101`) only indexes `groupId`. It lacks an index on `settledAt` (used by `SELECT * FROM settlements ORDER BY settledAt DESC` at `SplitMateDao.kt:75`) and `fromMemberId` / `toMemberId`.
   - `GroupMemberEntity` (`RoomEntities.kt:35-38`) lacks an index on `isCurrentUser`.

---

### 4.2 Unfiltered Full-Table Memory Loads & $O(G \times (M + E + S + T))$ Recomposition Bottlenecks

1. **Full-Database Memory Ingestion (`data/SplitMateDao.kt` & `ui/SplitMateViewModel.kt:322-381`)**:
   - `SplitMateDao.observeGroupMembers(groupId)` (`SplitMateDao.kt:28`) is never used. Instead, `observeRoomDatabase()` (`SplitMateViewModel.kt:322-381`) launches **7 separate coroutines** on `ioDispatcher` collecting unfiltered `SELECT *` full-table flows:
     - `dao.observeGroups()` (`line 326`)
     - `dao.observeAllMembers()` (`line 334`)
     - `dao.observeAllExpenses()` (`line 346`)
     - `dao.observeAllSplits()` (`line 352`)
     - `dao.observeAllSettlements()` (`line 358`)
     - `dao.observeCurrencies()` (`line 364`)
     - `dao.observeUserProfile()` (`line 373`)
   - Whenever a single expense with splits is inserted (`commitQuickEqualExpense` at `lines 539-559`), both `observeAllExpenses()` and `observeAllSplits()` fire separately, triggering redundant full-state copies of `_uiState`.
2. **Duplicate $O(G \times (M + E + S + T))$ Graph Simplification on Every State Emission (`SplitMateViewModel.kt:149-211, 1164-1175`)**:
   - `totalBalance` (`lines 149-162`) and `activeGroups` (`lines 164-211`) are both configured with `SharingStarted.Eagerly` derived directly from `_uiState`.
   - Because `_uiState` is a single monolithic `SplitMateUiState` object (containing UI selection state like `selectedGroupId`, `receiptItems`, `isSyncingRates`, `isDarkTheme` alongside DB tables), **every minor UI state change** triggers both `totalBalance` and `activeGroups`.
   - Both flows independently loop over every group $G$, linearly filter `state.members` ($O(M)$), filter `state.expenses` ($O(E)$), build a `groupExpenseIds` set, linearly filter `state.splits` ($O(S)$), linearly filter `state.settlements` ($O(T)$), and execute `SplitMateMathEngine.simplifyDebtsGreedy` (allocating PriorityQueues). Thus the entire multi-group debt graph is recomputed **twice per `_uiState` mutation**.
3. **Un-remembered $O(N)$ List Filtering Inside Compose Functions & `LazyColumn` Items (`ui/SplitMateAppComposable.kt`)**:
   - Composables repeatedly filter full-table lists directly in the composition body without `remember(...)`:
     - `SplitMateMainDashboardScaffold` (`lines 474-475, 509, 545-547`): filters `uiState.members`, `uiState.expenses`, `uiState.splits` on every recomposition.
     - `LedgersDashboardScreen` (`lines 885, 1700, 1765`): runs `uiState.members.filter { it.groupId == group.groupId }` and `uiState.expenses.filter { it.groupId == group.groupId }` inside `LazyColumn` item blocks.
     - `GreedySettlementScreen` (`lines 2594, 2766, 3066, 3159-3160`) and `AuditVaultScreen` (`lines 3433-3435`): inside `LazyColumn` `items(groupExpenses)`, executes `uiState.splits.filter { it.expenseId == expense.expenseId }` ($O(E \times S)$) and `uiState.members.find { ... }` ($O(E \times M)$) on every scroll frame.

---

### 4.3 Security, Privacy & Manifest Vulnerabilities

1. **Unencrypted Storage of Financial Ledger, UPI IDs, Phone Numbers & IRCTC PNR Records**:
   - **`EncryptedSharedPreferences` is completely absent** (`androidx.security:security-crypto` is not in `build.gradle`).
   - **Plaintext PNR Travel Records (`ui/SplitMateTheme.kt:1056, 1154, 1188, 1207, 1230, 1250`)**: `context.getSharedPreferences("splitmate_pnr_rate_guard", Context.MODE_PRIVATE)` persists full JSON snapshots (`snapshot_json_<10-digit-PNR>`) containing 10-digit PNR numbers, train numbers/names, boarding/destination stations, timestamps, coach/seat/berth allocations, and passenger fares in plaintext XML on disk.
   - **Plaintext SQLite DB (`data/SplitMateRoomDatabase.kt:34`)**: `splitmate_native_room.db` stores user full names, personal UPI IDs (`UserProfileEntity.upiId`, `GroupMemberEntity.upiId`), 10-digit mobile numbers embedded in UPI handles (`${cleanPhone}@upi` at `SplitMateViewModel.kt:715, 801`), and PNR numbers embedded in `ExpenseEntity.title` (`[PNR 2458910324] ...`) without SQLCipher encryption.
   - **Synchronous UI-Thread `SharedPreferences` Reads (`ui/SplitMateTheme.kt:833`)**: `performCrispTactileHaptic` calls `context.getSharedPreferences("splitmate_prefs", Context.MODE_PRIVATE).getBoolean("pref_haptics", true)` on every single calculator keypad tap on the main thread.
2. **Third-Party PNR Data Exfiltration & Unprotected Clipboard Copy**:
   - `fetchLivePnrAndTrainStatus` (`ui/SplitMateTheme.kt:1279, 1378`) transmits the user's sensitive 10-digit IRCTC PNR over unpinned HTTP/HTTPS requests to unofficial third-party commercial websites (`https://www.railyatri.in/m/pnr-status/$cleanPnr` and `https://www.confirmtkt.com/pnr-status/$cleanPnr`) while spoofing a mobile Chrome `User-Agent` header (`lines 1285-1287`).
   - `GroupBoardingPassCard` (`ui/SplitMateTheme.kt:2016-2017`) copies the PNR to the global Android `ClipboardManager` (`ClipData.newPlainText("IRCTC PNR", snap.pnr)`) without marking `ClipDescription.EXTRA_IS_SENSITIVE` on Android 13+, exposing the PNR to any background clipboard listener.
3. **`AndroidManifest.xml` Backup & Network Hardening Deficiencies (`android/app/src/main/AndroidManifest.xml:10-17`)**:
   - `android:allowBackup="true"` (`line 11`) is enabled with **no** `android:fullBackupContent` or `android:dataExtractionRules`. This permits `adb backup` and cloud extraction of both `splitmate_native_room.db` and `splitmate_pnr_rate_guard.xml`.
   - Missing `android:networkSecurityConfig` attribute to explicitly block cleartext traffic and enforce certificate pinning.

---

### 4.4 Lifecycle, State Management & Memory Leak Defects

1. **Zero `collectAsStateWithLifecycle()` Usage Across All 18 Flow Collection Sites**:
   - Even though `androidx.lifecycle:lifecycle-runtime-compose:2.7.0` is included in `android/app/build.gradle:71`, **0 files import or call `collectAsStateWithLifecycle()`**.
   - All **18 state collection sites** use raw `collectAsState()`, keeping upstream flows and Room observers active while the app is backgrounded:
     - `MainActivity.kt:37` (1 site)
     - `ui/SplitMateAppComposable.kt:130, 131, 132, 234, 450, 451, 452, 494, 495, 2306, 2307, 2308, 2309, 3270` (14 sites)
     - `ui/screens/QuickExpenseAndGuideScreens.kt:135, 136` (2 sites)
     - `ui/screens/PnrExpenseReviewScreen.kt:194` (1 site)
2. **Side-Effect Mutation of Global Singleton `SplitMateTheme.isDark` During Composition (`ui/SplitMateAppComposable.kt:77-78, 143`)**:
   - `SplitMateTheme` (`line 77`) is a global singleton object holding `var isDark by mutableStateOf(false)`.
   - Inside `SplitMateApp` (`line 143`), the composable executes `SplitMateTheme.isDark = uiState.isDarkTheme` **directly inside the composition body** (a Compose side-effect violation that can trigger mid-frame recomposition loops), and 3 separate token singletons (`SplitMateTheme` at `SplitMateAppComposable.kt:77`, `SplitMateThemeTokens` at `OnboardingAndSettingsScreens.kt:52`, and `QuickExpenseThemeTokens` at `QuickExpenseAndGuideScreens.kt:70`) read from `SplitMateTheme.isDark` instead of `MaterialTheme.colorScheme`.
3. **Unbounded Process-Lifetime Static Caches (`ui/SplitMateTheme.kt:1044-1046`)**:
   - `InMemoryPnrSnapshotCache`, `InMemoryPnrLastFetchEpochMs`, and `InFlightPnrSet` are top-level static `ConcurrentHashMap` / `synchronizedSet` instances with no LRU eviction bound or lifecycle cleanup.
4. **Dual In-Memory + Room Optimistic State Race Conditions (`ui/SplitMateViewModel.kt`)**:
   - In `commitQuickEqualExpense` (`lines 547-557`), `createNewGroupWithContacts` (`lines 729-743`), `addFriendsToActiveGroup` (`lines 810-814`), and `editExistingExpense` (`lines 1113-1124`), the ViewModel manually mutates `_uiState.update { ... }` with new entities on the caller thread while simultaneously launching an `ioDispatcher` coroutine to insert into Room—which then triggers `observeRoomDatabase()` (`lines 326-361`) to overwrite `_uiState` a second time, causing duplicate recompositions and transient state races if two operations interleave.
</SYSTEM_MESSAGE>

---

## Subagent Audit Appendix 3

The following is a <SYSTEM_MESSAGE> not actually sent by the user. It is provided by the system as important information to pay attention to.

<SYSTEM_MESSAGE>
[Message] timestamp=2026-09-22T18:58:01Z sender=dc5b8b70-ecee-4c2b-8873-093a83b46521 priority=MESSAGE_PRIORITY_HIGH content=# Principal Engineer Audit Report: Pillars 1 & 2 (`android/app/src/main`)

> **Scope Note**: `DesignSystemBindings` is defined as an `object` inside [`SplitMateTheme.kt:46-88`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L46-L88) rather than a standalone `DesignSystemBindings.kt` file. Every `.kt` file under `android/app/src/main/java/com/splitmate/app/` was inspected line-by-line.

---

## PILLAR 1: MATERIAL DESIGN 3 EXPRESSIVE & UI/UX FEEL

### 1. Typography Audit (`FigtreeFontFamily` vs Default `FontFamily` & Missing `fontFeatureSettings = "tnum"`)

#### A. Root Typography & Compose `TextStyle` Pitfalls
1. **Incomplete `tnum` Coverage in `SplitMateTypography` (`SplitMateTheme.kt:177-286`)** *(Severity: HIGH)*
   - `FigtreeFontFamily` (`SplitMateTheme.kt:159-172`) is wired into `SplitMateTypography`, but `fontFeatureSettings = "tnum"` is **only** set on `displayLarge` (`:184`), `displayMedium` (`:192`), `displaySmall` (`:200`), `headlineLarge` (`:208`), `headlineMedium` (`:216`), `titleLarge` (`:229`), and `labelMedium` (`:277`).
   - It is **completely omitted** from `headlineSmall` (`:218-223`), `titleMedium` (`:231-236`), `titleSmall` (`:237-243`), `bodyLarge` (`:244-250`), `bodyMedium` (`:251-257`), `bodySmall` (`:258-264`), `labelLarge` (`:265-271`), and `labelSmall` (`:279-285`). Any currency/number rendered with those styles or with ad-hoc `fontSize = ...` defaults to proportional digits.
2. **Bare `style = TextStyle(fontFeatureSettings = "tnum")` Wiping Ambient Typography** *(Severity: MEDIUM)*
   - In multiple composables (`SplitMateTheme.kt:1823, 1875, 1929`; `QuickExpenseAndGuideScreens.kt:868, 1415`; `SplitMateAppComposable.kt:959, 1246, 1256, 1686, 3228`), passing `style = TextStyle(fontFeatureSettings = "tnum")` instantiates a blank `TextStyle` rather than `LocalTextStyle.current.copy(fontFeatureSettings = "tnum")`, stripping line-height and platform text metrics.

#### B. Exact Currency & Numeric Displays Missing `fontFeatureSettings = "tnum"` (and/or `FigtreeFontFamily`)
1. **`PnrExpenseReviewScreen.kt` — 0% `tnum` Coverage (Never Used Anywhere in the 1,521-Line File)** *(Severity: CRITICAL)*
   - `PnrExpenseReviewScreen.kt:462-470`: Primary bottom CTA `"Confirm & Add ₹... (₹.../person)"` — missing `tnum`.
   - `PnrExpenseReviewScreen.kt:521-527`: Logged PNR banner rows `"✓ PNR $extractedPnr · ${formatPaiseDisplay(exp.totalAmountCents)}"` — missing `tnum`.
   - `PnrExpenseReviewScreen.kt:746-759`: 10-digit PNR `BasicTextField` (`TextStyle(fontFamily = FigtreeFontFamily, fontWeight = Bold, fontSize = 17.sp)`) — missing `fontFeatureSettings = "tnum"`.
   - `PnrExpenseReviewScreen.kt:878-886`: Train number badge `"$trainNumber · $trainName"` — missing `tnum`.
   - `PnrExpenseReviewScreen.kt:960-966` & `:1036-1042`: Boarding pass departure/arrival times (`departureTime`, `arrivalTime`) and dates — missing `tnum`.
   - `PnrExpenseReviewScreen.kt:981-988`: Journey duration badge (`duration`) — missing `tnum`.
   - `PnrExpenseReviewScreen.kt:1119-1125`: `"Per-Passenger Share: $perPersonShareDisplay"` — missing `tnum`.
   - `PnrExpenseReviewScreen.kt:1130-1137` & `:1147-1154`: Passenger booking & live status seat numbers (`WL 14`, `CNF B2-45`) — missing `tnum`.
   - `PnrExpenseReviewScreen.kt:1204-1211`: Hero `"ALL-INCLUSIVE IRCTC FARE"` `totalFareDisplay` (`24.sp`) — missing `tnum`.
   - `PnrExpenseReviewScreen.kt:1217-1224`: `"$perPersonShareDisplay / each"` pill — missing `tnum`.
   - `PnrExpenseReviewScreen.kt:1228-1234`: Fare breakdown `"Base $baseFareDisplay + IRCTC Conv. $convenienceFeeDisplay + Insurance $insuranceFeeDisplay"` — missing `tnum`.
   - `PnrExpenseReviewScreen.kt:1247-1254`: Barcode stub `"PNR $formattedPnr"` — missing `tnum`.
   - `PnrExpenseReviewScreen.kt:1450-1459`: Member row subtitle `"Paid full $totalFareDisplay"` / `"Owes you $perMemberShareDisplay"` — missing `tnum`.
   - `PnrExpenseReviewScreen.kt:1464-1470`: Right-aligned member share amount (`perMemberShareDisplay` / `"₹0"`, `16.sp`) — missing `tnum`.
   - `PnrExpenseReviewScreen.kt:1472-1478`: Payer reimbursement badge `"Getting back $payerReimbursementDisplay"` — missing `tnum`.
2. **`SplitMateAppComposable.kt` — Missing `tnum` & Explicit `fontFamily` on Financial Amounts** *(Severity: HIGH)*
   - `SplitMateAppComposable.kt:731-737`: Inside-Group Hero Card `"Total Spend"` (`₹%.2f`, `18.sp`) — missing `tnum` (whereas adjacent `"Your Net"` at `:686-694` has `tnum`).
   - `SplitMateAppComposable.kt:942-949`: Group Ledger expense row subtitle `"$perPersonShare / person (${breakdown.splittingMembersCount} splitting)"` — missing `tnum`.
   - `SplitMateAppComposable.kt:1005-1010`: Expanded member split breakdown `row.formattedShare` — missing explicit `fontFamily` and `tnum`.
   - `SplitMateAppComposable.kt:1484`: Active Groups count badge `"${activeGroups.size}"` — missing `tnum`.
   - `SplitMateAppComposable.kt:2526-2540`: `GreedySettlementScreen` top net summary banner (`+₹...` / `-₹...`, `18.sp`) — missing `tnum`.
   - `SplitMateAppComposable.kt:2623-2628` & `:2632-2638`: `GreedySettlementScreen` `"Owes you ${transfer.formattedDisplayAmount}"` and right-hand amount `"+${transfer.formattedDisplayAmount}"` (`16.sp`) — missing `tnum`.
   - `SplitMateAppComposable.kt:2823-2829` & `:2833-2839`: `GreedySettlementScreen` `"You owe ${transfer.formattedDisplayAmount}"` and `"-${transfer.formattedDisplayAmount}"` (`16.sp`) — missing `tnum`.
   - `SplitMateAppComposable.kt:3006-3010`: Peer-to-peer transfer card `"Pays ${transfer.formattedDisplayAmount}"` — missing `tnum`.
   - `SplitMateAppComposable.kt:3500-3505`: `AuditVaultScreen` expense card right-hand amount (`(if (isMePayer) "+" else "-") + formattedTotal`, `16.sp`) — missing both explicit `fontFamily` and `tnum`.
   - `SplitMateAppComposable.kt:3549-3554` & `:3568-3573`: `AuditVaultScreen` expanded member share (`row.formattedShare`) and `"$perPersonShare / person"` — missing explicit `fontFamily` and `tnum`.
   - `SplitMateAppComposable.kt:4030-4035` & `:4084`: `ActivityItemRow` and `ClaimItemRow` `amount` (`15.sp`) — missing explicit `fontFamily` and `tnum`.
3. **`QuickExpenseAndGuideScreens.kt` & `Components.kt`** *(Severity: MEDIUM)*
   - `QuickExpenseAndGuideScreens.kt:894-905`: Live split pill `"$currencySymbol ${formatPaiseForSplitBadge(perPersonPaise)} / person · $memberCount splitting..."` — missing `tnum` (only the hero display at `:868` has `tnum`).
   - `Components.kt:195` & `:211`: Quick-add chips (`+$1`, `+$5`, `+$10`, `+18% Tip`) — missing explicit `fontFamily` and `tnum`.

---

### 2. Haptics Audit (`performCrispTactileHaptic` & `LocalHapticFeedback`)

#### A. Where Haptics ARE Present
- **`SplitMateTheme.kt:828-864`**: Defines `performCrispTactileHaptic(view, enabled, isHeavyAction)`. Only invoked inside `SplitMateTheme.kt` on the `GroupBoardingPassCard` Refresh button (`:1974`) and ConfirmTkt button (`:2014`).
- **`QuickExpenseAndGuideScreens.kt`**: Used on keypad keys `.` (`:1139`), `0` (`:1155`), `BACK` (`:1171`), `Note` (`:1189`), `C` (`:1232`), `Log & Split` (`:1252`), digits `1-9` (`:1352`), dialog PNR lookup/chips/confirm (`:285, :363, :409, :461`), plus `LocalHapticFeedback` (`HapticFeedbackType.LongPress`) on the "Clear / Select All" pill (`:934`) and participant avatar toggle (`:987`).
- **`OnboardingAndSettingsScreens.kt`**: Used **only** on the Dark Theme switch (`:857, :870`) and Tactile Keypad Haptics switch (`:891, :906`).

#### B. Where Haptics Are Completely Missing (0 Haptic Calls)
1. **`PnrExpenseReviewScreen.kt` — ZERO Haptics in the Entire File (`1,521` lines)** *(Severity: CRITICAL)*
   - `PnrExpenseReviewScreen.kt:331-345`: Top-left Back navigation button — no haptic.
   - `PnrExpenseReviewScreen.kt:366-400`: Top-right Group Switcher pill & `DropdownMenuItem` group selection — no haptic.
   - `PnrExpenseReviewScreen.kt:418-471`: Bottom `"Confirm & Add ₹..."` primary CTA button — no haptic.
   - `PnrExpenseReviewScreen.kt:491-493` & `:764-796`: Auto-10-digit PNR trigger and `"Fetch PNR"` CTA button — no haptic.
   - `PnrExpenseReviewScreen.kt:1314-1328` & `:1361-1374`: `"Select X Pax"` pill and `"Add"` missing-passenger button — no haptic.
   - `PnrExpenseReviewScreen.kt:1389-1395`: Member split checkbox rows (`.clickable { onToggleMember(member.memberId) }`) — no haptic (`LocalHapticFeedback` is never even imported).
2. **`SplitMateAppComposable.kt` — ZERO Haptics in the Entire File (`4,088` lines)** *(Severity: CRITICAL)*
   - `SplitMateAppComposable.kt:406`: `SplitMateBottomNavigationBar` `NavigationBarItem` tab selection (`LEDGERS`, `SPLIT`, `SETTLE`, `AUDIT`) — no haptic.
   - `SplitMateAppComposable.kt:279-305`: Floating Action Button `"Log Expense"` — no haptic.
   - `SplitMateAppComposable.kt:1109-1118, 1276, 1292, 1317, 1593`: Dashboard theme toggle pill, `+ New Group`, `Log Expense`, IRCTC PNR Hero Card, and Group ledger cards — no haptics.
   - `SplitMateAppComposable.kt:566, 593, 630, 655, 900, 1018`: Inside-group `All Groups` back button, `+ Add Contact`, `Split PNR`, `Settle Up`, Expense card expand/collapse, and `Delete / Undo Expense` — no haptics.
   - `SplitMateAppComposable.kt:2445, 2648, 2682, 2708, 2852, 2918, 3014, 3231` (`GreedySettlementScreen`): Group filter chips, `Remind on WhatsApp`, `Link Phone`, `Mark Paid` buttons, `Pay via UPI`, and `Undo` settlement — zero haptics.
   - `SplitMateAppComposable.kt:3310, 3361, 3378, 3456, 3575, 3589` (`AuditVaultScreen`): Filter bar toggle, group filter chips, expense card expand/collapse, `Edit`, and `Undo` — zero haptics.
3. **`OnboardingAndSettingsScreens.kt` & `ContactPickerBottomSheet` (`SplitMateTheme.kt`)** *(Severity: MEDIUM)*
   - `OnboardingAndSettingsScreens.kt:225, 266, 299, 363`: Onboarding avatar randomize tap, Masculine/Feminine/Neutral style pills, and `"Continue to SplitMate"` CTA — zero haptics.
   - `OnboardingAndSettingsScreens.kt:504, 684, 706, 764, 801, 950`: Settings back button, `"Save Profile & UPI Handle"`, `"Include My UPI ID in WhatsApp"` switch, `"Export & Share Trip Ledger"`, and `"Reset App Data"` — no haptics.
   - `SplitMateTheme.kt:628, 685, 700` (`ContactPickerBottomSheet`): Contact checkbox selection and `"Add Selected"` CTA — no haptics.

---

### 3. Animations & Motion Audit (`spring` vs `tween` vs Jarring Instant Swaps)

#### A. Linear / Tween Animations Used Instead of M3 Expressive Springs
1. **`PnrExpenseReviewScreen.kt:1383-1387` (`MemberSplitRow`)** *(Severity: MEDIUM)*
   - Uses `animateColorAsState(..., animationSpec = tween(180))` for row selection color and has **zero physical press/bounce scale animation** (`MutableInteractionSource` + `collectIsPressedAsState` + `spring(DampingRatioMediumBouncy)`) when toggling member checkboxes.
2. **`SplitMateTheme.kt:67` (`DesignSystemBindings.themeColorTween`)** *(Severity: LOW)*
   - Uses `tween<T>(durationMillis = 400, easing = FastOutSlowInEasing)` across 17 color transitions (`QuickExpenseAndGuideScreens.kt:141-170`, `OnboardingAndSettingsScreens.kt:107-131, 449-478`, `SplitMateAppComposable.kt:264-273, 383-387`).
3. **Unused M3 Spring Component (`Components.kt:85-227`)** *(Severity: LOW)*
   - `HorizontalFloatingToolbar` implements a `spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)` `AnimatedContent` transition (`:122-133`), but `HorizontalFloatingToolbar` is **dead code** (never called anywhere in the app).

#### B. Jarring 0ms Instant Screen & State Swaps (Missing `AnimatedContent` / `Crossfade` / `AnimatedVisibility`)
1. **`SplitMateMainDashboardScaffold` Full-Screen Early Returns (`SplitMateAppComposable.kt:233-262`)** *(Severity: HIGH)*
   - `if (showUserSettingsScreen) { UserSettingsScreen(...); return }` (`:233-248`) and `if (showPnrReviewScreen) { PnrExpenseReviewScreen(...); return }` (`:251-262`) execute hard early `return`s that tear down the entire `Scaffold` in a single frame without `AnimatedContent` or shared-axis slide/fade transitions.
2. **`SplitMateMainDashboardScaffold` Bottom Tab Switching (`SplitMateAppComposable.kt:322-374`)** *(Severity: HIGH)*
   - `when (currentTab)` (`LEDGERS`, `SPLIT`, `SETTLE`, `AUDIT`) is placed directly inside a static `Box` (`:322`) without `AnimatedContent` or `Crossfade`. Switching bottom navigation tabs causes an instant, un-eased frame cut.
3. **`LedgersDashboardScreen` Group Detail Drill-Down (`SplitMateAppComposable.kt:544-1050`)** *(Severity: HIGH)*
   - `if (openedGroup != null) { LazyColumn { ... }; return }` (`:544`) performs another hard early `return`! Tapping a group card (or pressing `"All Groups"` back at `:566`) swaps between the Group Overview and Group Detail `LazyColumn` trees with zero transition.
4. **`PnrExpenseReviewScreen` Empty State → Live Boarding Pass Swap (`PnrExpenseReviewScreen.kt:570-675`)** *(Severity: HIGH)*
   - `if (snapshot == null) { Surface(...) } else { TactilePaperBoardingPass(...); MemberSplitSelectionCard(...) }` swaps the empty prompt card for the Skeuomorphic Paper Boarding Pass and Member Split card instantaneously when `isFetchingPnr` finishes, with no `AnimatedContent` or spring entrance.

---

### 4. Visual & Layout Clutter Audit (`GroupBoardingPassCard` / `TravelTicketLiveCard` & `PnrExpenseReviewScreen`)

#### A. Why `GroupBoardingPassCard` (`SplitMateTheme.kt:1701-2045`) Creates 4 Nested Boxes & Debug Clutter inside Group Ledger & Audit Tab
> Note: The component conceptually referred to as `TravelTicketLiveCard` is named `GroupBoardingPassCard` in [`SplitMateTheme.kt:1701-2045`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L1701-L2045) and is embedded inside every PNR expense item in both the **Group Ledger** ([`SplitMateAppComposable.kt:971-979`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L971-L979), inside the `openedGroup != null` block starting at `:544`) and the **Audit Tab** ([`SplitMateAppComposable.kt:3515-3523`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L3515-L3523)).

1. **Quadruple-Nested Surface/Card Hierarchy**:
   - **Box Level 1**: Outer Expense `Card` (`SplitMateAppComposable.kt:900` in Ledger / `:3456` in Audit).
   - **Box Level 2**: `GroupBoardingPassCard` outer `Surface` (`SplitMateTheme.kt:1763-1768`, `RoundedCornerShape(18.dp)`, `passBg`, `1.dp passBorder`).
   - **Box Level 3**: Passenger Status sub-card `Surface` (`SplitMateTheme.kt:1884-1932`, `RoundedCornerShape(10.dp)`, `SurfaceWhite`, `1.dp passBorder`).
   - **Box Level 4**: Live Train Radar sub-card `Surface` (`SplitMateTheme.kt:1939-1963`, `RoundedCornerShape(10.dp)`, `Color(0xFFECF4DC)`, `1.dp passBorder`) plus 2 nested action pill `Surface`s (`:1972-2009` `"Refresh Live CNF/WL & Save"` and `:2012-2040` `"ConfirmTkt ↗"`).
2. **Raw Internal Scraper, Cache & ML Telemetry Text Leaked into the Financial Ledger UI**:
   - `SplitMateTheme.kt:1900-1904` & `:1547`: Renders `"🟢 Live CRIS / RailYatri SSR JSON (₹0 Free)"`, `"Live CRIS Cache (<1m ago · Rate-Limit Protected)"` (`:1241`), or `"Passenger Status (CNF / WL / RAC · 6h Smart Cache)"` (`:1903`).
   - `SplitMateTheme.kt:1923-1930` & `:1361-1365`: Renders raw bracketed scraper status strings separated by middots: `P1: Booked [PQWL 14] → Live [WL 7] · P2: Booked [PQWL 15] → Live [WL 8]`.
   - `SplitMateTheme.kt:1945-1960` & `:994-999`: Renders `"🛰️ Live Train Radar: ..."` and `"🚃 Engine -> EOG -> H1 -> A1 -> A2 -> B1..B6 (3A) -> S1..S6 -> PC · 88% CNF Probability (ML Trend)"` inside an expense row!
   - **Title-Pipe Parsing Collision (`PnrExpenseReviewScreen.kt:423-435` → `SplitMateTheme.kt:1584-1648`)**: When `PnrExpenseReviewScreen` saves an expense, it packs `coachAndSeats = "Class ${snapshot.travelClass} · ${selectedMemberIds.size} Pax (${formatPaiseDisplay(perSelectedMemberSharePaise)}/person)"` into the pipe-delimited `expense.title`. `extractTravelTicketFromTitle` (`SplitMateTheme.kt:1632`) parses that string back as `ticket.coachAndSeats`, which `GroupBoardingPassCard` (`:1881`) then renders as the passenger status fallback inside Box Level 3, duplicating the per-person share text already shown on the outer expense card!

#### B. Why `PnrExpenseReviewScreen.kt` Only Shows a Static 1-Line Text for Already-Logged PNRs (`PnrExpenseReviewScreen.kt:500-531`)
- At `PnrExpenseReviewScreen.kt:500-531`, `existingPnrExpensesInGroup` is rendered inside a static `Surface` (`:501-530`) that loops through `existingPnrExpensesInGroup.take(4)` and prints a non-interactive 1-line `Text` (`:521-527`):
  `"✓ PNR $extractedPnr · ${formatPaiseDisplay(exp.totalAmountCents)} (Multiple different PNRs supported)"`
- **Root Cause**:
  1. Those rows have **no `.clickable` modifier** to load `loadPersistedPnrSnapshot(context, extractedPnr)` (`SplitMateTheme.kt:1126`) or invoke `triggerLivePnrLookup(extractedPnr)` (`PnrExpenseReviewScreen.kt:223`) so the user can view the `TactilePaperBoardingPass` (`:813-1280`) for an already-added PNR.
  2. Furthermore, `liveSnapshot` starts as `null` (`:189`) whenever `PnrExpenseReviewScreen` opens with an empty `pnrInput`, so `if (snapshot == null)` (`:570-646`) always renders the `"Ready for Live PNR Lookup"` empty state card even when the group already has logged PNRs.

---

## PILLAR 2: THE "GHOST DATA" & PLACEHOLDER HUNT (ALL `.kt` FILES)

### 1. `SplitMateTheme.kt` — Fake Offline Train Catalog, Hash-Generated Fake Passengers, & Fake Radar Telemetry *(Severity: CRITICAL)*
- **`SplitMateTheme.kt:976-991` (`OfflineIndianTrainCatalog`)**: 14 hardcoded trains (`12925` Paschim SF Express, `16592` Hampi Express, `12628` Karnataka Express, `12952` Mumbai Rajdhani, `22436` Vande Bharat Exp, etc.) with hardcoded routes, times, and fares (`1650_00L`, `540_00L`, etc.).
- **`SplitMateTheme.kt:993-1000` (`OfflineTrainIntermediateRadar`)**: Hardcoded fake live GPS/radar strings (`"Crossing Kota Jn (KOTA) at 118 km/h · Platform 1 · On Time"`, `"Engine -> EOG -> H1 -> A1 -> A2 -> B1..B6 (3A)..."`, `"Crossing Dharmavaram Jn (DMM) · Platform 2"`).
- **`SplitMateTheme.kt:1002-1024` (`OfflineStationNames`)**: 22 hardcoded station/airport lookup strings (`BDTS`, `CDG`, `SBC`, `HPT`, `NDLS`, etc.).
- **`SplitMateTheme.kt:1425-1440` (Silent Hash-Based Fake Train Selection)**: If live scraping fails or returns blank for **any arbitrary 10-digit number**, `fetchLivePnrAndTrainStatus` hashes the PNR (`val fallbackIndex = kotlin.math.abs(cleanPnr.hashCode()) % catalogKeys.size`, `:1427-1428`) to pick a random train from `OfflineIndianTrainCatalog`, defaults stations to `"SBC"` → `"HPT"` (`:1435-1436`), and sets `depTime = "22:00"`, `arrTime = "07:10"` (`:1437-1438`)!
- **`SplitMateTheme.kt:1455-1495` (Silent Last-Digit Fake Passenger Generator)**: When `scrapedPassengers.isEmpty()`, it inspects `cleanPnr.lastOrNull()?.digitToIntOrNull()` (`:1455`) and fabricates 2 to 4 fake passengers (`Booked [PQWL 14] → Live [WL 7]`, `Booked [WL 9,GNWL] → Live [RAC 6 (Coach B2 Seat 31 SL)]`, or `Booked [WL 6,GNWL] → Live [CNF B2-45 LB]`). Because `PnrExpenseReviewScreen.kt:238` checks `if (fetched.trainNo.isNotBlank() || fetched.structuredPassengers.isNotEmpty())`, **entering any random 10-digit number (e.g. `9999999999`) silently fabricates a fake train, fake passengers, and fake fare instead of reporting an error!**
- **`SplitMateTheme.kt:1506-1512`**: Hardcoded fake ML probabilities (`"94% Full Berth CNF at Charting"`, `"88% CNF Probability (ML Trend)"`, `"74% CNF / RAC Probability"`).

### 2. `SplitMateViewModel.kt` — Seeded Groups, Sample Personas, Italian Restaurant Receipt Items, & Hardcoded FX Rate *(Severity: HIGH)*
- **`SplitMateViewModel.kt:35-56` (`SplitMateUiState` defaults)**:
  - `:36-38`: Default identity `currentUserName = "Akshay"`, `currentUserSeed = "Akshay|Masculine"`, `currentUserCountry = "India"`.
  - `:46`: `activeGroupId = "g_tahoe"`.
  - `:51`: `receiptTitle = "Osteria Del Sole · Table 14"`.
  - `:52-55`: `receiptPayerId = "m_1"`, `activeClaimerPersonaId = "m_1"`, `receiptTaxPercent = 8.875`, `receiptTipPercent = 20.0`.
- **`SplitMateViewModel.kt:279-301` (`SampleParticipants`)**: Hardcoded 4-person list (`m_1` `"You (Akshay)"` with UPI `"akshay@okicici"`, `m_2` `"Sarah Chen"` `"sarah.chen@okaxis"`, `m_3` `"Marcus Vance"` `"marcus.v@okhdfcbank"`, `m_4` `"Elena Rostova"` `"elena.r@ybl"`).
- **`SplitMateViewModel.kt:303-336` (`SampleReceiptItems`)**: 5 hardcoded USD restaurant receipt items (`r_1` `"Truffle Burrata & Focaccia"` `$24.00`, `r_2` `"Handmade Tagliatelle Bolognese"` `$32.50`, `r_3` `"Wood-Fired Margherita DOC"` `$26.00`, `r_4` `"Barolo Chinato Bottle (Shared)"` `$68.00`, `r_5` `"Artisanal Tiramisu"` `$16.50`).
- **`SplitMateViewModel.kt:364` (`seedInitialDataIfNeeded`)**: Automatically seeds the Room database whenever `dao.getGroupCount() == 0` with:
  - **3 fake groups (`:367-370`)**: `"g_tahoe"` (`"Lake Tahoe Ski Cabin"`, `USD`), `"g_kyoto"` (`"Kyoto Autumn Ramen Tour"`, `JPY`), `"g_ny"` (`"Apt 4B Utilities & Rent"`, `USD`).
  - **4 fake members (`:373-378`)** & **3 fake Tahoe expenses (`:393-427`)**: `"e_seed_1"` (`"Whole Foods Market · Provisions"`, `$248.60`), `"e_seed_2"` (`"Palisades Lift Tickets (4x)"`, `$412.00`), `"e_seed_3"` (`"Fireside Wood & S'mores"`, `$84.20`).
  - **1 fake Kyoto expense (`:430-439`)**: `"e_seed_4"` (`"Ichiran Ramen Shibuya"`, `$64.00`).
- **`SplitMateViewModel.kt:648` & `QuickExpenseAndGuideScreens.kt:267`**: Hardcoded USD/INR conversion factor `val usdCents = (inrRupees * 100.0 / 84.0)` when adding a PNR expense into a non-INR group.

### 3. `QuickExpenseAndGuideScreens.kt` — Unused Mock Arrays, Hardcoded Quick Chips, & Sample Texts *(Severity: MEDIUM)*
- **`QuickExpenseAndGuideScreens.kt:101-125` (`QuickParticipants` & `QuickCategories`)**: Unused top-level mock lists (`"You"`, `"Sarah"`, `"Marcus"`, `"Elena"` with `Color(0xFF4F46E5)` / `0xFF0891B2` corporate colors).
- **`QuickExpenseAndGuideScreens.kt:372-395`**: Hardcoded quick-fill chips inside `SmartAddExpenseDialog` (`" Dinner ₹800"` -> `"800"`, `"☕ Chai ₹120"` -> `"120"`, `"🚕 Auto ₹350"` -> `"350"`).
- **`QuickExpenseAndGuideScreens.kt:445`**: Placeholder `"e.g., Hampi Express 3AC / Dinner at Osteria"`.
- **`QuickExpenseAndGuideScreens.kt:593`**: Hardcoded default `selectedPayer = "You (Akshay)"`.
- **`QuickExpenseAndGuideScreens.kt:1525-1934` (`FeatureWalkthroughScreen` / `InteractiveOnboardingDemo`)**: Dead/unused composable file region containing hardcoded demo step cards (`"Step 1 of 4 · Instant Group Creation"`, `"Goa Beach Villa 2026"`, `"Priya"`, `"Rohan"`, `"PNR 2458193047"`, `"12952 · Mumbai Rajdhani · 4 Pax"`, `"Rohan owes Priya ₹1,420"`).

### 4. `SplitMateAppComposable.kt` — Unused Mock Lists & Sample Placeholders *(Severity: MEDIUM)*
- **`SplitMateAppComposable.kt:132-160`**: Top-level unused static arrays `RecentActivities` (`"Osteria Del Sole"`, `"Whole Foods Market"`, `"tahoe_ski_2026.pdf"`, `"Settlement from Marcus"`) and `ReceiptClaimItems` (`"Truffle Tagliatelle"`, `"Artisanal Burrata"`, `"Pitcher of Sangria"`, `"Tiramisu Classico"`) plus dead helper composables `ActivityItemRow` (`:3995-4038`) and `ClaimItemRow` (`:4043-4087`).
- **`SplitMateAppComposable.kt:1345-1350`**: Dashboard PNR hero card sample prompt `"Try sample: 4512890341 or enter any 10-digit IRCTC PNR"`.
- **`SplitMateAppComposable.kt:1901` & `:2180`**: Group creation dialog placeholder `"e.g., Hampi Trip, Goa 2026"` and contact dialog placeholder `"e.g., Rahul Sharma"`.
- **`SplitMateAppComposable.kt:3763-3769`**: Help sheet step 2 `"Tap 'Split PNR' inside any group and enter your 10-digit PNR (or tap a Demo PNR chip)"`.

### 5. `PnrExpenseReviewScreen.kt`, `OnboardingAndSettingsScreens.kt`, & `Components.kt` *(Severity: LOW)*
- **`PnrExpenseReviewScreen.kt:610-614`**: Empty state subtitle `"Enter a valid 10-digit IRCTC PNR above to fetch real-time train schedule, coach/berth allocations, and exact ticket fare."` (while `fetchLivePnrAndTrainStatus` actually falls back to `OfflineIndianTrainCatalog` when live lookup fails).
- **`PnrExpenseReviewScreen.kt:1249-1250`**: Default barcode fallback `"1234-567-890"` when `pnrNumber` is blank.
- **`OnboardingAndSettingsScreens.kt:238` & `:632`**: Name input placeholder `"e.g., Akshay Karadkar"` and UPI input placeholder `"e.g., akshay@okicici"`.
- **`Components.kt:183, 200, 214`**: Hardcoded USD quick-add buttons (`+$1`, `+$5`, `+$10`, `+18% Tip`) inside dead `HorizontalFloatingToolbar` composable.
</SYSTEM_MESSAGE>

---

