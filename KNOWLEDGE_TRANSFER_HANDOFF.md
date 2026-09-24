# SplitMate (`v1.9.0`) — Principal Engineer Knowledge Transfer (KT) & Context Handoff

> **Last Updated**: Release `v1.9.0` (`HEAD` commit `ba70901`, GitHub Release: https://github.com/akshaykaradkar/splitmate/releases/tag/v1.9.0)
> **Workspace Root**: `/usr/local/google/home/karadkar/splitmate`
> **Gradle Binary**: `/usr/local/google/home/karadkar/splitmate/.android-sdk/gradle-8.7/bin/gradle` (run with `Cwd = /usr/local/google/home/karadkar/splitmate/android`)

---

## 1. Mandatory Source-of-Truth Documents in Repository Root
Before making any UI, Room DB, Math, or PNR API change, any agent working in this repository MUST read:
1. **[`PRINCIPAL_ENGINEER_AUDIT_REPORT.md`](file:///usr/local/google/home/karadkar/splitmate/PRINCIPAL_ENGINEER_AUDIT_REPORT.md)** — The 3-Pass, 6-Pillar Staff/Principal Engineering Audit covering Material 3 Expressive & Figtree `tnum` typography, Zero Ghost Data, APK size bloat (`17.1 MB` → `2.3 MB`), Room v5 relational schema & `AES256-GCM` security, PNR network resilience, and `0.00¢` Largest Remainder math.
2. **[`PHASED_EXECUTION_PLAN.md`](file:///usr/local/google/home/karadkar/splitmate/PHASED_EXECUTION_PLAN.md)** (`v4.0 — Pass-3 Final L8 Sign-Off Edition`) — The exact architectural blueprint for Phases 1–4.
3. **[`EXECUTION_TASK_TRACKER.md`](file:///usr/local/google/home/karadkar/splitmate/EXECUTION_TASK_TRACKER.md)** — The completed `[x]` checklist across all 4 phases (`Baseline` + `Phases 1–4`).
4. **[`GEMINI.md`](file:///usr/local/google/home/karadkar/splitmate/GEMINI.md)** & **[`BRD.md`](file:///usr/local/google/home/karadkar/splitmate/BRD.md)** — Canonical Stitch "Organic Tactile Financial" (Buckwheat) design system rules and mathematical invariants.

---

## 2. Summary of Completed 4-Phase Engineering Overhaul (`v1.8.7` → `v1.9.0`)

### Phase 1 (`1060d79` / rebased in `ba70901`): Database Integrity, Security & Lifecycle Safety
- **Room `version = 5` Schema (`RoomEntities.kt` & `SplitMateRoomDatabase.kt`)**:
  - Enforced **6 `ForeignKey` (`ON DELETE CASCADE`)** constraints and **11 indices** across `group_members`, `expenses`, `expense_splits`, and `settlements`.
  - Implemented topological **`MIGRATION_4_5`** (`group_members` → `expenses` → `expense_splits` → `settlements`) with 8-path orphan cleanup (`DELETE FROM ... WHERE ... NOT IN (...)`) and replaced `.fallbackToDestructiveMigration()` with `.addMigrations(MIGRATION_4_5).fallbackToDestructiveMigrationOnDowngrade()`.
- **`@Upsert` Safety (`SplitMateDao.kt`)**:
  - Replaced `@Insert(onConflict = OnConflictStrategy.REPLACE)` with `@Upsert` across all 7 DAO methods so updating a member or profile never triggers `DELETE` + `ON DELETE CASCADE` on `ExpenseSplitEntity`.
- **Hardware-Backed `AES256-GCM` Vault (`EncryptedPrefsProvider.kt`)**:
  - Created `EncryptedPrefsProvider.get(context)` (`splitmate_pnr_secure_vault`) with self-healing `KeyStoreException`/`AEADBadTagException` recovery and one-time migration + wipe of legacy cleartext `"splitmate_pnr_rate_guard"`.
- **Lifecycle-Aware Collectors**:
  - Migrated all 18 `collectAsState()` calls across `MainActivity.kt`, `SplitMateAppComposable.kt`, `PnrExpenseReviewScreen.kt`, and `QuickExpenseAndGuideScreens.kt` to `collectAsStateWithLifecycle()`.
  - Switched `SharingStarted.Eagerly` to `SharingStarted.WhileSubscribed(5000)` in `SplitMateViewModel.kt`.

### Phase 2 (`6350c3d` / rebased in `ba70901`): Financial Math (`0.00¢` Drift), Zero Ghost Data & PNR Network Resilience
- **Payer-First Largest Remainder & Custom Payer Selection (`SplitMateMathEngine.kt`)**:
  - Added `SplitMateMathEngine.orderParticipantsPayerFirst(memberIds, payerId, currentUserId)` so all 3 split paths (`SplitMateViewModel`, `PnrExpenseReviewScreen`, `QuickExpenseScreen`) deterministically assign `+1` paise remainder pennies to the actual Payer first (`0.00¢` drift).
  - Added `"Paid by: [Member]"` horizontal chip selectors in `PnrExpenseReviewScreen` and `QuickExpenseScreen`.
  - Fixed `QuickExpenseAndGuideScreens.kt` Largest Remainder badge so each member displays their exact allocated penny (`+₹0.01`) instead of total `remainderPaise` (`+₹0.02`).
- **Zero Ghost Data & Manual Ticket Entry Mode**:
  - Deleted `OfflineIndianTrainCatalog`, `OfflineTrainIntermediateRadar`, `enrichTicketWithOfflineCatalog`, and the modulo-hash synthetic WL/RAC/CNF generator while preserving `OfflineStationNames`.
  - When live PNR lookup is unreachable (`isLiveVerified == false`), `PnrExpenseReviewScreen` populates `LivePnrStatusSnapshot(..., isLiveVerified = false, isManualEntry = true)` to unlock the **Manual Ticket Fare & Route Card** (`manualTotalFareInput`, `manualFromStationInput`, `manualToStationInput`), member split selector, and bottom Confirm CTA.
- **5-Stage `PnrNetworkRepository.kt`**:
  - L1 `LruCache(32)` + L2 `EncryptedSharedPreferences` → 6-hour positive TTL / 60s negative debounce → in-flight `Mutex` request coalescing (`inFlightPnrDeferreds`) on `SupervisorJob() + Dispatchers.IO` → sliding 5-calls-per-5-minutes token bucket (`"global_pnr_fetch_epochs_csv"`) → `4,000ms` connect/read timeout with `finally { conn.disconnect() }`.
- **Idempotent 10-Digit PNR Deduplication & Fee Protection**:
  - `commitQuickEqualExpense` and `editExistingExpense` reject duplicate 10-digit PNRs within the same group (`onDuplicatePnrError` callback).
  - Inspecting/editing an already-logged PNR locks `effectiveTotalPaise = selectedExistingExpense.totalAmountCents` so IRCTC convenience (`+₹23.60`) and travel insurance (`+₹0.45/pax`) fees are never double-counted.

### Phase 3 (`c2966e9` / rebased in `ba70901`): Figtree `tnum` Typography, Dark-Mode Tactile Paper Pass & Ticket De-Cluttering
- **Universal `FigtreeFontFamily` + OpenType Tabular Numerals (`tnum`)**:
  - Added `fontFeatureSettings = "tnum"` across all 14 `SplitMateTypography` text styles in `SplitMateTheme.kt` and bound `val SplitMateTnumMonospace: FontFamily = FigtreeFontFamily` (zero raw `FontFamily.Monospace`).
- **Dark-Mode `TactilePaperPassTokens` & Physics/Haptics**:
  - Made `TactilePaperPassTokens` (`PnrExpenseReviewScreen.kt`) dark-mode adaptive via `SplitMateTheme.isDark` (`#141311` canvas, `#1F1D1A` paper surface, `#F4EFEA` primary ink) and added `tactileTextFieldColors()`.
  - Replaced linear `tween(180)` with `spring(dampingRatio = 0.8f, stiffness = 380f)` (`SmoothSettleSpring`) and wired `LocalHapticFeedback.current` / `performCrispTactileHaptic`.
- **Ticket De-Cluttering (`CompactLedgerTicketStub`)**:
  - Replaced multi-hundred-dp inline `GroupBoardingPassCard` in Group Overview and Activity Audit rows with `CompactLedgerTicketStub` (`76dp` single-row pill with `"Paper Pass ↗"` CTA) + Group **"Tactile Paper IRCTC PNR Studio"** Hero Launcher Card and horizontal scrollable logged-PNR pill strip.

### Phase 4 (`ba70901`): APK Bloat Elimination (`17.1 MB` → `2.3 MB`), Dead Code Purge & Modularization
- **R8 Shrinking & Release Build (`splitmate-1.9.0.apk`)**:
  - Enabled `minifyEnabled true`, `shrinkResources true`, `proguardFiles(...)`, and `resConfigs "en"` in `android/app/build.gradle` (`versionCode 16`, `versionName "1.9.0"`).
  - Shrunk release APK from `17.1 MB` down to **`2.3 MB`** (`86.5%` reduction).
- **Dead Code Purge & Package Modularization**:
  - Purged dead symbols (`ReceiptLineItem`, `defaultSeedReceiptItems`, `commitCollaborativeExpense`, `HorizontalFloatingToolbar`, `GroupBoardingPassCard`).
  - Extracted dialog/atomic composables into [`ui/dialogs/GroupAndSettlementDialogs.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/dialogs/GroupAndSettlementDialogs.kt) and PNR domain models/parsers into [`ui/pnr/PnrTicketModels.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/pnr/PnrTicketModels.kt).
