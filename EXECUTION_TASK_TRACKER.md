# SplitMate v1.9.0 — Phase-by-Phase Execution & Audit Task Tracker

> **Protocol**: Every item is tracked live (`[ ]` Pending → `[/]` In Progress → `[x]` Verified & Audited). At the end of each Phase, we run `./gradlew testDebugUnitTest`, perform a line-by-line self-audit against [`PHASED_EXECUTION_PLAN.md` (v4.0)](file:///usr/local/google/home/karadkar/splitmate/PHASED_EXECUTION_PLAN.md), update this checklist, and create an atomic Git commit checkpoint.

---

## Baseline Checkpoint
- [x] **B.1** Save Master 6-Pillar Audit Report (`PRINCIPAL_ENGINEER_AUDIT_REPORT.md`)
- [x] **B.2** Save & 3x Subagent-Audit Phased Execution Plan (`PHASED_EXECUTION_PLAN.md` v4.0)
- [x] **B.3** Create baseline Git commit (`8da7676`)

---

## Phase 1: Database Integrity, Security & Lifecycle Safety
- [x] **1.1** **`RoomEntities.kt` — 6 ForeignKey (`ON DELETE CASCADE`) & 11 Index Schema**:
  - Add `ForeignKey` & `Index` definitions to `GroupMemberEntity`, `ExpenseEntity`, `ExpenseSplitEntity`, and `SettlementEntity` while keeping constructor defaults `defaultValue = null` for Room `TableInfo.read()` parity.
- [x] **1.2** **`SplitMateDao.kt` — Replace `OnConflictStrategy.REPLACE` with `@Upsert`**:
  - Replace `@Insert(onConflict = OnConflictStrategy.REPLACE)` with `@Upsert` across all 7 DAO methods so `updateCurrentUserProfile()` never triggers `ON DELETE CASCADE` on `ExpenseSplitEntity`.
  - Add `@Transaction suspend fun replaceExpenseSplits(expenseId: String, splits: List<ExpenseSplitEntity>)` and `deleteGroupById(groupId: String)`.
- [x] **1.3** **`SplitMateRoomDatabase.kt` — `MIGRATION_4_5` (`version = 5`) & Remove Destructive Fallback**:
  - Implement topological `MIGRATION_4_5` (`group_members` → `expenses` → `expense_splits` → `settlements`) with 8-path orphan cleanup and replace `.fallbackToDestructiveMigration()` with `.addMigrations(MIGRATION_4_5).fallbackToDestructiveMigrationOnDowngrade()`.
- [x] **1.4** **`EncryptedPrefsProvider.kt`, `build.gradle` & `proguard-rules.pro` — AES256-GCM Vault**:
  - Add `androidx.security:security-crypto:1.1.0-alpha06` to `android/app/build.gradle`.
  - Create `android/app/proguard-rules.pro` with Google Tink (`com.google.crypto.tink.**`), Room, and HttpURLConnection R8 keep rules.
  - Create `EncryptedPrefsProvider.kt` with self-healing `KeyStoreException`/`AEADBadTagException` recovery and one-time migration + wipe of legacy `"splitmate_pnr_rate_guard"`.
- [x] **1.5** **Lifecycle-Aware State Collection (`collectAsStateWithLifecycle`) & Contact Cursor Bounds**:
  - Replace all 18 `collectAsState()` calls in `MainActivity.kt`, `SplitMateAppComposable.kt`, `PnrExpenseReviewScreen.kt`, and `QuickExpenseAndGuideScreens.kt` with `collectAsStateWithLifecycle()`.
  - Switch `SharingStarted.Eagerly` to `SharingStarted.WhileSubscribed(5000)` in `SplitMateViewModel.kt`.
  - Bound `queryAllDeviceContacts` (`HAS_PHONE_NUMBER = 1`, `500` row cap) and guard `loadDeviceContacts(context, forceRefresh = false)` against redundant re-queries.
- [x] **1.6** **Phase 1 Verification, Self-Audit & Git Checkpoint Commit**:
  - Run `./gradlew testDebugUnitTest` & verify `SplitMateRoomDatabase_Impl.java` 1:1 schema parity.
  - Audit `git diff` against Phase 1 spec and create Git commit `feat(phase-1): ...`.

---

## Phase 2: Financial Math Drift, Ghost Data Elimination & Network Resilience
- [x] **2.1** **`SplitMateMathEngine.kt` & Custom Payer Selection (`payerMemberId`)**:
  - Implement `SplitMateMathEngine.orderParticipantsPayerFirst(memberIds, payerId, currentUserId)` so all 3 split paths (`SplitMateViewModel`, `PnrExpenseReviewScreen`, `QuickExpenseScreen`) assign Largest Remainder pennies (`+1` paise) to the actual Payer first (`0.00¢` drift).
  - Add `payerMemberId: String? = null` to `commitQuickEqualExpense`, add `"Paid by: [Member]"` chip selector in `PnrExpenseReviewScreen` and `QuickExpenseScreen`, and replace all 5 hardcoded `member.isCurrentUser` checks in `PnrExpenseReviewScreen.kt` with `member.memberId == selectedPayerId`.
  - Fix `QuickExpenseAndGuideScreens.kt:L896` so the Largest Remainder badge reads `memberOwedCents[member.memberId]` (`+₹0.01`) instead of total `remainderPaise` (`+₹0.02`).
  - Fix `resolveExpenseSplitBreakdown` (`PnrExpenseReviewScreen.kt:L1246`) to use `orderParticipantsPayerFirst`.
- [x] **2.2** **Zero Ghost Data & Manual Ticket Entry Mode (`isLiveVerified` / `isManualEntry`)**:
  - Remove `OfflineIndianTrainCatalog`, `OfflineTrainIntermediateRadar`, `enrichTicketWithOfflineCatalog`, and the modulo-hash synthetic WL/RAC/CNF generator (`SplitMateTheme.kt:L1425-L1496`) while preserving `OfflineStationNames`.
  - Update all 4 call sites of deleted functions (`SplitMateTheme.kt:L1522, L1559, L1633` and `SplitMateAppComposable.kt:L3878`).
  - When live PNR fetch fails (`isLiveVerified == false`), populate `liveSnapshot` with `LivePnrStatusSnapshot(pnr = clean, isLiveVerified = false, isManualEntry = true, ...)` to unlock the **Manual Ticket Fare & Route Card** (`manualTotalFareInput`, `manualFromStationInput`, `manualToStationInput`), member split selector, and bottom Confirm CTA.
  - Gate `dao.upsertCurrencyRates(defaultSeedCurrencies())` behind `if (roomDao.getCurrencyRate("INR") == null)` and set INR `lockedExchangeRate = 1.0`.
- [x] **2.3** **`PnrNetworkRepository.kt` — 5-Stage Token Bucket, Coalescing & Bounded Timeouts**:
  - Implement `PnrNetworkRepository.kt` with the strict 5-stage pipeline: L1 `LruCache(32)` + L2 `EncryptedSharedPreferences` → 6h positive TTL / 60s negative debounce → `coalesceMutex` `inFlightPnrDeferreds` on `SupervisorJob() + Dispatchers.IO` → `"global_pnr_fetch_epochs_csv"` 5-calls-per-5-minutes token bucket → `4,000ms` connect/read socket timeout with `finally { conn.disconnect() }`.
- [x] **2.4** **Idempotent PNR Deduping & Cross-Group Navigation (`PnrReviewLaunchRequest`)**:
  - Add 10-digit PNR deduplication inside `commitQuickEqualExpense` (`SplitMateViewModel.kt:L480`) and self-excluding duplicate-PNR guard inside `editExistingExpense` (`L679`).
  - Add `PnrReviewLaunchRequest(groupId, initialPnr, returnToTab)` so tapping a PNR ticket from Global Activity switches `selectActiveGroup(expense.groupId)` before opening the Studio.
- [x] **2.5** **Phase 2 Verification, Self-Audit & Git Checkpoint Commit**:
  - Update unit tests in `SplitMateViewModelTurbineTest.kt` for `1.0` INR exchange rate, custom payer LRM, and duplicate-PNR rejection.
  - Run `./gradlew testDebugUnitTest`, audit `git diff`, and create Git commit `feat(phase-2): ...`.

---

## Phase 3: Material Design 3 Expressive, Typography (`tnum`), Haptics & Ticket De-Cluttering
- [x] **3.1** **Universal `tnum` Enforcement, Dark-Mode `TactilePaperPassTokens` & Haptics**:
  - Apply `fontFeatureSettings = "tnum"` to all 14 `SplitMateTypography` slots (`SplitMateTheme.kt:L201-L343`) and bind `SplitMateTnumMonospace = FigtreeFontFamily`.
  - Replace all raw `FontFamily.Monospace` occurrences (`PnrExpenseReviewScreen.kt`, `QuickExpenseAndGuideScreens.kt`, `SplitMateAppComposable.kt`, `OnboardingAndSettingsScreens.kt`) with `SplitMateTnumMonospace` (`FigtreeFontFamily`).
  - Make `TactilePaperPassTokens` (`PnrExpenseReviewScreen.kt:L60-L120`) dark-mode adaptive via `SplitMateTheme.isDark` and add explicit `tactileTextFieldColors()`.
  - Wire `LocalHapticFeedback.current` across `PnrExpenseReviewScreen.kt`, `OnboardingAndSettingsScreens.kt`, and `SplitMateTheme.kt`, and replace linear `tween` with `spring(dampingRatio = 0.8f, stiffness = 380f)` (`SmoothSettleSpring`).
- [x] **3.2** **Ticket UI De-Cluttering & Logged-Ticket Fee Protection**:
  - Create `CompactLedgerTicketStub` (`76dp` single-row pill) for Group Overview & Activity Audit feeds, replacing full `GroupBoardingPassCard` in list rows.
  - Redesign the Group "Split PNR" tab (`SplitMateAppComposable.kt`) with the top **"Tactile Paper IRCTC PNR Studio"** Hero Launcher Card (`#EAF3D5` / `#1F2B16`) + compact horizontal logged-ticket strip.
  - Derive `selectedExistingExpense` reactively from `pnrInput` in `PnrExpenseReviewScreen.kt`, lock `effectiveTotalPaise = selectedExistingExpense.totalAmountCents` when inspecting/editing an existing logged ticket (preventing double-counted `+₹25.40` IRCTC fees), and regenerate `formattedTitle` on split update.
- [x] **3.3** **Phase 3 Verification, Self-Audit & Git Checkpoint Commit**:
  - Run `./gradlew testDebugUnitTest` (`BUILD SUCCESSFUL`, all 10 unit tests pass), audit `git diff`, and create Git commit `feat(phase-3): ...`.

---

## Phase 4: APK Bloat Elimination (~17.1 MB → ~4.0 MB), Dead Code Purge & Architecture Modularization
- [/] **4.1** **R8 Shrinking, Resource Shrinking & Dead WebView Asset Deletion**:
  - Delete `android/app/src/main/assets/index.html`, `splitmate-ui.js`, `stitch-interactive.js`, and `styles.css`.
  - Enable `minifyEnabled true`, `shrinkResources true`, `proguardFiles(...)`, and `resConfigs "en"` in `android/app/build.gradle`, and bump `versionCode 16` / `versionName "1.9.0"`.
- [/] **4.2** **Dead Code Purge & Test Suite Alignment**:
  - Remove `ReceiptLineItem`, `defaultSeedReceiptItems()`, `claimReceiptItemToggle()`, `splitUnassignedRemainderEqually()`, `commitCollaborativeExpense()`, `HorizontalFloatingToolbar`, and `SplitMateQuickActionFab` from `SplitMateViewModel.kt`, `Components.kt`, and `SplitMateAppComposable.kt`.
  - Update `SplitMateViewModelTurbineTest.kt` and `SplitMateComposeUiTest.kt` so all unit and UI tests compile and pass without dead symbols.
- [/] **4.3** **UI Package Modularization (`ui/dialogs/` & `ui/pnr/`)**:
  - Extract dialog composables from `SplitMateAppComposable.kt` into `ui/dialogs/GroupAndSettlementDialogs.kt` and PNR helper functions/models into `ui/pnr/PnrTicketModels.kt` (preserving `package com.splitmate.app` in `SplitMateAppComposable.kt`).
- [/] **4.4** **Phase 4 Final Verification, Release APK Size Audit (`< 5.0 MB`) & Final Git Commit**:
  - Run `./gradlew testDebugUnitTest` and `./gradlew assembleRelease`.
  - Verify release APK size (`splitmate-1.9.0.apk` < `5.0 MB`) and create final Git commit `feat(phase-4): ...`.
