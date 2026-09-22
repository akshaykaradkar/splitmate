# SplitMate Principal Engineer Remediation & Phased Execution Plan (v4.0 — Pass-3 Final L8 Sign-Off Edition)
**Target Baseline**: `v1.8.7` → `v1.9.0` (Zero-Shortcut Architectural, Mathematical & M3 Expressive Overhaul)  
**Reference Document**: [`PRINCIPAL_ENGINEER_AUDIT_REPORT.md`](file:///usr/local/google/home/karadkar/splitmate/PRINCIPAL_ENGINEER_AUDIT_REPORT.md) (Master Report + Subagent Appendices 1, 2 & 3)  
**Audit Verification**: Cross-examined across **Three Full Parallel Rounds (Pass 1 + Pass 2 + Pass 3)** by 3 Specialized Principal Plan-Audit Subagents (`Subagent 1: UI/UX & Ghost Data`, `Subagent 2: Architecture, DB & Bloat`, `Subagent 3: API, Math & Edge-Cases`)  
**Execution Status**: **PLANNING ONLY (Zero source code files modified, edited, or committed)**

---

## Master Ledger of Engineering Traps Caught & Remediated Across Pass 1, Pass 2 & Pass 3

| Pass | # | Engineering Trap / Defect Caught by Parallel Subagents | Exact Architectural Fix in Plan v4.0 |
| :---: | :- | :--- | :--- |
| **Pass 3** | **1** | **Room `TableInfo.read` `DEFAULT` Schema Validation Crash & Complete 6-FK / 11-Index SQLite DDL** (`Phase 1 §2.1`) | In `RoomEntities.kt`, `isCurrentUser = false`, `upiId = ""`, `syncStatus = "SYNCED"`, and `plusOneCent = false` are **Kotlin constructor defaults** (not `@ColumnInfo(defaultValue = ...)`). Writing SQL `DEFAULT` in `MIGRATION_4_5` would crash Room's `TableInfo` validator (`Expected: defaultValue=null`). Embedded the **exact `TableInfo`-compliant SQLite DDL** with all **6 Foreign Keys** (`payerId`, `fromMemberId`, `toMemberId` included) and **11 Indices** (`fromMemberId`, `toMemberId` included) in strict topological order (`group_members` → `expenses` → `expense_splits` → `settlements`). |
| **Pass 3** | **2** | **Custom Payer Selection (`payerMemberId`) Missing in `commitQuickEqualExpense`, `PnrExpenseReviewScreen` & `QuickExpenseScreen`** (`Phase 2 §2.1`) | `commitQuickEqualExpense` (`SplitMateViewModel.kt:L488-L509`) lacked a `payerMemberId` parameter and hardcoded `isCurrentUser` as the Payer, and `PnrExpenseReviewScreen` (`L305, L672, L1410, L1452, L1471`) ignored `selectedExistingExpense.payerId`. Added `payerMemberId: String? = null` to `commitQuickEqualExpense`, added `"Paid by: [Member]"` selector chips to both screens, and updated `resolveExpenseSplitBreakdown` (`L1246`) to use `orderParticipantsPayerFirst`. |
| **Pass 3** | **3** | **Double-Counted IRCTC Fees (`+₹25.40`) on Existing/Fallback Tickets, Reactive `selectedExistingExpense` & Duplicate-PNR Guard in `editExistingExpense`** (`Phase 2 & 3`) |Anchored `effectiveTotalPaise = selectedExistingExpense.totalAmountCents` when `selectedExistingExpense != null` (so `2360` + `180` paise fees are never added twice), derived `selectedExistingExpense` reactively from `pnrInput`, regenerated `formattedTitle` on split updates, and added the duplicate-PNR guard (`other.expenseId != expenseId`) to `editExistingExpense`. |
| **Pass 3** | **4** | **Strict 5-Stage Execution Order for `PnrNetworkRepository` Global Token Bucket** (`Phase 2 §2.3`) | Locked the 5-stage pipeline so `Stage 1` (`LruCache` + `EncryptedSharedPreferences`), `Stage 2` (6h positive TTL / 60s negative debounce), and `Stage 3` (`coalesceMutex` `inFlightPnrDeferreds` join) run **before** `Stage 4` (`"global_pnr_fetch_epochs_csv"` 5-calls-per-5-min reservation), ensuring cache hits and coalesced callers consume `0` tokens. |
| **Pass 3** | **5** | **`snapshot == null` Manual Ticket Entry Deadlock & Dark-Mode `TactilePaperPassTokens` / `OutlinedTextField` Contrast** (`Phase 2 §2.2 & Phase 3 §2.1`) | When `fetched.isLiveVerified == false`, `triggerLivePnrLookup` sets `liveSnapshot = LivePnrStatusSnapshot(pnr = clean, isLiveVerified = false, isManualEntry = true, ...)` so `MemberSplitSelectionCard` (`L651`), the **Manual Ticket Fare & Route Card**, and `Scaffold.bottomBar` (`L406`) remain visible. Made `TactilePaperPassTokens` dark-mode aware via `LocalSplitMatePalette.current` and added explicit `OutlinedTextFieldDefaults.colors(...)` at `L1354`. |
| **Pass 3** | **6** | **Test Suite Compilation (`SplitMateViewModelTurbineTest.kt` & `SplitMateComposeUiTest.kt`) & Package Preservation Contract** (`Phase 2 & 4`) | Added `SplitMateViewModelTurbineTest.kt` (`L41-L83`) and `SplitMateComposeUiTest.kt` (`L34, L40`) to the Impacted Files lists (updating tests that called deleted `commitCollaborativeExpense`, `splitUnassignedRemainderEqually`, `83.95` FX rate, and `HorizontalFloatingToolbar`), and preserved `package com.splitmate.app` in `ui/SplitMateAppComposable.kt:L1`. |
| **Pass 2** | **7** | **SQLite `OnConflictStrategy.REPLACE` + `ForeignKey.CASCADE` Data-Wiping Bomb** (`SplitMateDao.kt:L33-L50`) | Replaced `@Insert(onConflict = OnConflictStrategy.REPLACE)` with Room 2.6.1's **`@Upsert`** (`ON CONFLICT DO UPDATE`) across `SplitMateDao.kt` so `updateCurrentUserProfile` (`L624`) updates `GroupMemberEntity` rows in-place without firing `ON DELETE CASCADE` on `ExpenseSplitEntity`. |
| **Pass 2** | **8** | **Google Tink R8 Reflection Stripping & Keystore `AEADBadTagException` Self-Healing** (`Phase 1 & 4`) | Added `-keep class com.google.crypto.tink.** { *; }` + `-dontwarn com.google.crypto.tink.**` to `proguard-rules.pro` and wrapped `EncryptedSharedPreferences.create(...)` in a self-healing Keystore fallback. |
| **Pass 2** | **9** | **`QuickExpenseScreen` (`L896-L897`) Bogus `"+₹0.02 Largest Remainder"` Badge, `SupervisorJob` Coalescer & Cross-Group `PnrReviewLaunchRequest`** (`Phase 2 & 3`) | Fixed `QuickExpenseScreen` badge to `"+₹0.01 on $remainderPaise people (Payer first)"` with per-avatar shares, isolated `inFlightPnrDeferreds` on `CoroutineScope(SupervisorJob() + Dispatchers.IO)`, and added `PnrReviewLaunchRequest(groupId, initialPnr, returnToTab)` calling `viewModel.selectActiveGroup(expense.groupId)`. |
| **Pass 1** | **10** | **`orderParticipantsPayerFirst`, `compositionLocalOf` Theme, Root `SplitMateMaterialTheme`, `OfflineStationNames` & WCAG AA (`#635D54`)** (`Phases 1–3`) | Shared `SplitMateMathEngine.orderParticipantsPayerFirst`, dynamic `compositionLocalOf`, root `SplitMateMaterialTheme` + `BasicTextField` `tnum`, retained `OfflineStationNames` (`NDLS` → `New Delhi`), dual-format PNR regex `(?:PNR:\s*|\[PNR:)(\d{10})\]?`, and darkened `InkMuted` to `#635D54` (`4.96:1–5.48:1`). |

---

## PHASE 1: Database Integrity, Lifecycle Architecture & Local Security Hardening

### 1. Exact Audit Findings Addressed
- **`[CRITICAL] 4.1`, Appendix 2 `4.1.1–4.1.4`, Pass-2 Trap B (`@Upsert`) & Pass-3 Traps A & B (`TableInfo` 6-FK / 11-Index DDL)**:
  - `fallbackToDestructiveMigration()` in [`SplitMateRoomDatabase.kt:L36`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/data/SplitMateRoomDatabase.kt#L36) wipes user financial ledgers on schema changes.
  - Zero `@ForeignKey` constraints (6 required across `groupId`, `expenseId`, `memberId`, `payerId`, `fromMemberId`, `toMemberId`) and missing secondary indices (11 required including `createdAt`, `payerId`, `settledAt`, `fromMemberId`, `toMemberId`, `isCurrentUser`) in [`RoomEntities.kt:L35-L114`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/data/RoomEntities.kt#L35-L114).
  - `@Insert(onConflict = OnConflictStrategy.REPLACE)` in [`SplitMateDao.kt:L20-L54`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/data/SplitMateDao.kt#L20-L54) executing `DELETE` before `INSERT` and triggering `ON DELETE CASCADE` during `updateCurrentUserProfile` (`L624`).
  - Non-atomic multi-table mutations in [`SplitMateViewModel.kt:L1015-L1016`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt#L1015-L1016) and [`L1106-L1109`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt#L1106-L1109).
- **`[HIGH] 4.2`, Appendix 2 `4.3.1–4.3.3` & Pass-2 Trap A (Self-Healing Keystore Vault)**:
  - Plaintext storage of PNR travel records in `splitmate_pnr_rate_guard.xml`, `android:allowBackup="true"`, incomplete `clearLocalVault()`, unflagged clipboard copies (`SplitMateTheme.kt:L2016`), and main-thread `SharedPreferences` reads in `performCrispTactileHaptic` (`L833`).
- **`[HIGH] 4.3` & Appendix 2 `4.2.1–4.2.3`**:
  - Global mutable singleton `SplitMateTheme.isDark` mutated during Composition, `0` uses of `collectAsStateWithLifecycle()` across 18 collectors, and un-memoized $O(G \times (M + E + S + T))$ / $O(E \times S)$ calculations.

### 2. Specific Architectural & Code Fixes Proposed
1. **KSP Schema Export & Exact `TableInfo`-Verified Room `MIGRATION_4_5` (`version = 4 → 5`, 6 FKs + 11 Indices)**:
   - In [`android/app/build.gradle`](file:///usr/local/google/home/karadkar/splitmate/android/app/build.gradle), add `ksp { arg("room.schemaLocation", "$projectDir/schemas"); arg("room.incremental", "true") }` inside `defaultConfig`.
   - In [`RoomEntities.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/data/RoomEntities.kt), keep all Kotlin constructor defaults untouched (do **not** add `@ColumnInfo(defaultValue = ...)` so `defaultValue = null` matches `TableInfo`) and declare all **6 Foreign Keys** (`ON DELETE CASCADE`) and **11 Indices**:
     - `GroupMemberEntity`: `ForeignKey(ExpenseGroupEntity::class, ["groupId"], ["groupId"], CASCADE)`, `indices = [Index("groupId"), Index("isCurrentUser")]`
     - `ExpenseEntity`: `ForeignKey(ExpenseGroupEntity::class, ["groupId"], ["groupId"], CASCADE)`, `ForeignKey(GroupMemberEntity::class, ["memberId"], ["payerId"], CASCADE)`, `indices = [Index("groupId"), Index("createdAt"), Index("payerId")]`
     - `ExpenseSplitEntity`: `ForeignKey(ExpenseEntity::class, ["expenseId"], ["expenseId"], CASCADE)`, `ForeignKey(GroupMemberEntity::class, ["memberId"], ["memberId"], CASCADE)`, `indices = [Index("expenseId"), Index("memberId")]`
     - `SettlementEntity`: `ForeignKey(ExpenseGroupEntity::class, ["groupId"], ["groupId"], CASCADE)`, `ForeignKey(GroupMemberEntity::class, ["memberId"], ["fromMemberId"], CASCADE)`, `ForeignKey(GroupMemberEntity::class, ["memberId"], ["toMemberId"], CASCADE)`, `indices = [Index("groupId"), Index("settledAt"), Index("fromMemberId"), Index("toMemberId")]`
   - In [`SplitMateRoomDatabase.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/data/SplitMateRoomDatabase.kt), remove `.fallbackToDestructiveMigration()` and register `MIGRATION_4_5` with the exact `TableInfo`-verified SQL DDL:
     ```kotlin
     val MIGRATION_4_5 = object : Migration(4, 5) {
         override fun migrate(db: SupportSQLiteDatabase) {
             // 1. Sanitize all 8 relational orphan paths in strict top-down parent-to-child order
             db.execSQL("DELETE FROM `group_members` WHERE `groupId` NOT IN (SELECT `groupId` FROM `expense_groups`)")
             db.execSQL("DELETE FROM `expenses` WHERE `groupId` NOT IN (SELECT `groupId` FROM `expense_groups`) OR `payerId` NOT IN (SELECT `memberId` FROM `group_members`)")
             db.execSQL("DELETE FROM `expense_splits` WHERE `expenseId` NOT IN (SELECT `expenseId` FROM `expenses`) OR `memberId` NOT IN (SELECT `memberId` FROM `group_members`)")
             db.execSQL("DELETE FROM `settlements` WHERE `groupId` NOT IN (SELECT `groupId` FROM `expense_groups`) OR `fromMemberId` NOT IN (SELECT `memberId` FROM `group_members`) OR `toMemberId` NOT IN (SELECT `memberId` FROM `group_members`)")

             // 2. Rebuild `group_members` -> 3. Rebuild `expenses` -> 4. Rebuild `expense_splits` -> 5. Rebuild `settlements`
             // (With exact TEXT/INTEGER/REAL NOT NULL types, ON UPDATE NO ACTION ON DELETE CASCADE, and all 11 CREATE INDEX IF NOT EXISTS statements)
         }
     }
     ```
2. **Replace `@Insert(onConflict = OnConflictStrategy.REPLACE)` with `@Upsert` & Atomic `@Transaction` Mutations**:
   - In [`SplitMateDao.kt:L20-L54, L77, L93`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/data/SplitMateDao.kt#L20-L54), replace `@Insert(onConflict = OnConflictStrategy.REPLACE)` with Room's `@Upsert` (`ON CONFLICT DO UPDATE`) across `upsertCurrencyRates`, `insertGroup`, `insertMembers`, `insertExpense`, `insertExpenseSplits`, `insertSettlement`, and `upsertUserProfile`.
   - Add `@Transaction suspend fun replaceExpenseAndSplitsAtomically(updatedExpense: ExpenseEntity, newSplits: List<ExpenseSplitEntity>)` and `@Transaction suspend fun deleteExpenseAndSplitsAtomically(expenseId: String)`, and update `clearAllLedgerData()` (`L115`) to also call `deleteUserProfile()`.
3. **Dynamic `compositionLocalOf` Theme Architecture Across All 5 UI Token Objects**:
   - Define `val LocalSplitMatePalette = compositionLocalOf { LightBuckwheatPalette }` in `SplitMateTheme.kt`, wrap `SplitMateApp()` (`SplitMateAppComposable.kt:L160`) in `SplitMateMaterialTheme(darkTheme = uiState.isDarkTheme)`, and wire `SplitMateTheme`, `SplitMateThemeTokens`, `QuickExpenseThemeTokens`, and `TactilePaperPassTokens` (`PnrExpenseReviewScreen.kt:L60-L87`) to `LocalSplitMatePalette.current`.
4. **All 18 `collectAsStateWithLifecycle()` Sites, Memoized ViewModel Debt Flows & $O(1)$ `LazyColumn` Maps**:
   - Convert all **18** `collectAsState()` call sites across `MainActivity.kt` (`1`), `SplitMateAppComposable.kt` (`14`), `QuickExpenseAndGuideScreens.kt` (`2`), and `PnrExpenseReviewScreen.kt` (`1`) to `collectAsStateWithLifecycle()`, pre-compute `activeGroups` / `totalBalance` inside `SplitMateViewModel.init`, and hoist `splitsByExpenseId` / `membersById` maps above `LazyColumn` blocks.
5. **Self-Healing Encrypted Local Storage (`EncryptedSharedPreferences`), Sensitive Clipboard & Complete Vault Wipe**:
   - Implement `getEncryptedPnrVault(context)` (`"splitmate_pnr_secure_vault"`, AES256-GCM) with a self-healing `try / catch` (`context.deleteSharedPreferences` on `AEADBadTagException`) and in-memory `LruCache<String, LivePnrStatusSnapshot>(50)`, cache `isHapticsEnabled` in `AtomicBoolean`, mark clipboard copies with `ClipDescription.EXTRA_IS_SENSITIVE = true`, set `android:allowBackup="false"` + `android:dataExtractionRules="@xml/data_extraction_rules"`, and wipe all vaults in `clearLocalVault(context)`.

### 3. Exact Files Impacted
- [`android/app/build.gradle`](file:///usr/local/google/home/karadkar/splitmate/android/app/build.gradle)
- [`android/app/src/main/AndroidManifest.xml`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/AndroidManifest.xml) & [`android/app/src/main/res/xml/data_extraction_rules.xml`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/res/xml/data_extraction_rules.xml) *(New)*
- [`android/app/src/main/java/com/splitmate/app/MainActivity.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/MainActivity.kt)
- [`android/app/src/main/java/com/splitmate/app/data/RoomEntities.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/data/RoomEntities.kt)
- [`android/app/src/main/java/com/splitmate/app/data/SplitMateDao.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/data/SplitMateDao.kt)
- [`android/app/src/main/java/com/splitmate/app/data/SplitMateRoomDatabase.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/data/SplitMateRoomDatabase.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/screens/OnboardingAndSettingsScreens.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/OnboardingAndSettingsScreens.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt)

---

## PHASE 2: Zero-Drift Math Reconciliation, Ghost Data Elimination & API Rate-Limit Protection

### 1. Exact Audit Findings Addressed
- **`[CRITICAL] 6.1` & Pass-3 Trap A (End-to-End `payerMemberId` Selection & `0.00¢` Reconciliation)**:
  - Truncated integer division in `PnrExpenseReviewScreen.kt`, bogus `"+₹0.02 Largest Remainder"` badge in `QuickExpenseAndGuideScreens.kt:L896`, truncated fallback in `resolveExpenseSplitBreakdown` (`SplitMateViewModel.kt:L1246`), and hardcoded `isCurrentUser` payer in `commitQuickEqualExpense` (`L488-L509`) and `PnrExpenseReviewScreen` (`L305, L672, L1410, L1452, L1471`).
- **`[CRITICAL] 2.1`, `[HIGH] 2.2`, Pass-3 Manual Entry Deadlock & Test Suite Compilation (`SplitMateViewModelTurbineTest.kt`)**:
  - Fake train/passenger hash fabrication (`SplitMateTheme.kt:L976-L1000, L1032-L1042, L1425-L1512`) + 4 call sites (`L1522, L1559, L1633` & `SplitMateAppComposable.kt:L3878`), `snapshot == null` hiding `MemberSplitSelectionCard` (`PnrExpenseReviewScreen.kt:L570`) when live verification fails, second PNR lookup in `QuickExpenseAndGuideScreens.kt:L282-L316`, orphaned ghost data in `SplitMateViewModel.kt`, and `SplitMateViewModelTurbineTest.kt` (`L41-L83`) calling deleted methods.
- **`[CRITICAL] 5.1`, `[HIGH] 5.2` & Pass-3 Trap C (5-Stage Token Bucket Pipeline)**:
  - Auto-input 6h cache bypass (`forceManualRefresh = true`), UI-scope `CancellationException` propagation in `inFlightPnrDeferreds`, failed-lookup retry storm, and strict 5-stage token bucket ordering.
- **`[HIGH] 6.2`**:
  - Numeric/decimal bounds (`MAX_EXPENSE_PAISE = 99_999_999_00L`, `^\d{0,8}(\.\d{0,2})?$`), `maxLines = 1` on `48.sp` hero amount (`QuickExpenseAndGuideScreens.kt:L859`), and character bounds (`40`/`60`/`64`).

### 2. Specific Architectural & Code Fixes Proposed
1. **End-to-End Custom Payer (`payerMemberId`) & `SplitMateMathEngine.orderParticipantsPayerFirst` Across All Screens**:
   - Add `SplitMateMathEngine.orderParticipantsPayerFirst(chosenMembers: List<GroupMemberEntity>, payerMemberId: String): List<Pair<String, String>>`.
   - Update `SplitMateViewModel.commitQuickEqualExpense` (`L488-L518`) to accept `payerMemberId: String? = null`, resolving `val payer = groupMembers.find { it.memberId == payerMemberId } ?: groupMembers.find { it.isCurrentUser } ?: groupMembers.first()`.
   - Track `selectedPayerId` (`remember(groupMembers, selectedExistingExpense?.expenseId) { mutableStateOf(selectedExistingExpense?.payerId ?: currentUserMemberId) }`) with a `"Paid by: [Member]"` chip selector in both `PnrExpenseReviewScreen` (`MemberSplitSelectionCard`) and `QuickExpenseScreen`, replacing all 5 hardcoded `member.isCurrentUser` payer checks in `PnrExpenseReviewScreen.kt` (`L305, L672, L1410, L1452, L1471`) with `member.memberId == selectedPayerId`.
   - Update `editExistingExpense` (`SplitMateViewModel.kt:L1071-L1112`) to accept `newTotalCents: Long`, enforce the duplicate-PNR guard (`other.expenseId != expenseId`), preserve `existingSplits` when only the title changes, and otherwise call `orderParticipantsPayerFirst(chosenMembers, effectivePayerId)`.
   - Update `resolveExpenseSplitBreakdown` (`SplitMateViewModel.kt:L1246`) fallback branch to use `orderParticipantsPayerFirst(groupMembers, expense.payerId)` + `splitEquallyZeroDrift`.
2. **Eliminate Fake Train/Passenger Hash Generators, Break the `snapshot == null` Manual Entry Deadlock & Update `SplitMateViewModelTurbineTest.kt`**:
   - Delete `OfflineIndianTrainCatalog`, `OfflineTrainIntermediateRadar`, `enrichTicketWithOfflineCatalog`, and `L1425-L1512` fake train/passenger synthesis while **preserving `OfflineStationNames` (`L1002-L1030`)** and updating all 4 call sites (`SplitMateTheme.kt:L1522, L1559, L1633`, `SplitMateAppComposable.kt:L3878`) to use `resolveTicketStationNames`.
   - When `fetched.isLiveVerified == false` in `PnrExpenseReviewScreen.kt:L238-L247`, set `fetchError` AND initialize `liveSnapshot = LivePnrStatusSnapshot(pnr = clean, trainName = "IRCTC Train Ticket", travelClass = "3A", isLiveVerified = false, isManualEntry = true, effectivePassengerCount = groupMembers.size.coerceAtLeast(1))` alongside a **Manual Ticket Fare & Route Card** (`manualTotalFareInput`, `manualFromStationInput`, `manualToStationInput`) so `snapshot != null` holds and `MemberSplitSelectionCard` (`L651`) + `Scaffold.bottomBar` (`L406`) remain visible and interactive!
   - Update [`SplitMateViewModelTurbineTest.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/test/java/com/splitmate/app/SplitMateViewModelTurbineTest.kt) (`L40-L83`) to create a group explicitly (`viewModel.createNewGroup(...)`), test `commitQuickEqualExpense` (`lockedExchangeRate == 1.0`), and verify `SplitMateMathEngine.orderParticipantsPayerFirst` zero-drift splits (`₹7,525.40` ÷ 3 = `250847`, `250847`, `250846` paise).
3. **Strict 5-Stage Execution Pipeline in `PnrNetworkRepository`**:
   - Pass `forceManualRefresh = false` on automatic 10-digit completion in both `PnrExpenseReviewScreen.kt:L234, L491` and `QuickExpenseAndGuideScreens.kt:L298`.
   - Execute `fetchLivePnrAndTrainStatus` in strict 5-stage order:
     - **Stage 1**: Hydrate `cachedSnapshot` and `last_sync_$cleanPnr` from `LruCache(50)` / `EncryptedSharedPreferences` (`0` tokens consumed).
     - **Stage 2**: Enforce 6h positive cache TTL (`cachedSnapshot != null`) and 60s negative debounce (`cachedSnapshot == null`) (`0` tokens consumed).
     - **Stage 3**: Join active `inFlightPnrDeferreds[cleanPnr]` via `coalesceMutex` on `CoroutineScope(SupervisorJob() + Dispatchers.IO)` (`0` additional tokens consumed).
     - **Stage 4**: Prune timestamps older than `300_000L` (`5 min`) from `"global_pnr_fetch_epochs_csv"`. If `recentEpochs.size >= 5`, return rate-limited state **without** updating `last_sync_$cleanPnr`; otherwise reserve `1` token and stamp `last_sync_$cleanPnr = now`.
     - **Stage 5**: Execute `railyatri.in` / `confirmtkt.com` with `4.0s` timeouts and `conn.disconnect()` in `finally`.

### 3. Exact Files Impacted
- [`android/app/src/main/java/com/splitmate/app/SplitMateMathEngine.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/SplitMateMathEngine.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/screens/OnboardingAndSettingsScreens.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/OnboardingAndSettingsScreens.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt)
- [`android/app/src/test/java/com/splitmate/app/SplitMateViewModelTurbineTest.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/test/java/com/splitmate/app/SplitMateViewModelTurbineTest.kt)

---

## PHASE 3: Material Design 3 Expressive UI/UX, Compact Ticket Stubs, Multi-PNR Wallet, `tnum` Typography, Contrast & Haptics

### 1. Exact Audit Findings Addressed
- **`[CRITICAL] 1.1` & Cross-Group `PnrReviewLaunchRequest`**:
  - Quadruple-nested `GroupBoardingPassCard` clutter in Group Detail (`SplitMateAppComposable.kt:L971-L979`) and Audit Vault (`L3515-L3523`), plus cross-group navigation when clicking `View Pass →` from Tab 4 (`AuditVaultScreen`).
- **`[CRITICAL] 1.2` & Pass-3 Trap B (Multi-PNR Wallet Fare Preservation & Reactive `selectedExistingExpense`)**:
  - Static 1-line strings for logged PNRs (`PnrExpenseReviewScreen.kt:L500-L531`), double-counted IRCTC fees (`+₹25.40`) when viewing/synthesizing already-logged tickets (`exp.totalAmountCents`), and `selectedExistingExpense` desynchronization when typing a new PNR.
- **`[CRITICAL] 1.3`, `[HIGH] 1.4`, `[HIGH] 1.5`, `[HIGH] 6.3` & Dark-Mode / WCAG AA Contrast (`#635D54`)**:
  - Root `SplitMateMaterialTheme` wiring, `BasicTextField` `tnum` (`PnrExpenseReviewScreen.kt:L751`), 10 bare `TextStyle(tnum)` overrides, Dark-Mode `TactilePaperPassTokens` + `OutlinedTextFieldDefaults.colors` (`L1354`), `InkMuted` (`#635D54`) contrast, haptics, `spatialScreenSpring()` / `expressiveComponentSpring()`, hoisted `LazyListState`, and `48.dp` / `Role.Checkbox` accessibility.

### 2. Specific Architectural & UI Fixes Proposed
1. **Replace `GroupBoardingPassCard` with `CompactTactileTicketStub` & Cross-Group `PnrReviewLaunchRequest`**:
   - Replace `GroupBoardingPassCard` in both Group Detail (`L971-L979`) and Audit Vault (`L3515-L3523`) with **`CompactTactileTicketStub`** (`72.dp` height, perforated `#FFFDF9` Warm Ivory / Dark `#1E1C19` ticket strip with `10.dp` side notches, `#264010` Forest Green accent bar, Route `NDLS ➔ MMCT`, Train/Class badge, Live Status Pill, and **`View Pass →`** CTA wrapped in `CompositionLocalProvider(LocalContentColor provides TactilePaperPassTokens.InkPrimary)`).
   - Replace `showPnrReviewScreen: Boolean` (`SplitMateAppComposable.kt:L238`) with `pnrReviewLaunchRequest: PnrReviewLaunchRequest?` (`groupId`, `initialPnr`, `returnToTab`) calling `viewModel.selectActiveGroup(expense.groupId)` on launch and returning to `returnToTab` on exit.
2. **Interactive Multi-PNR Boarding Pass Wallet with Exact Fare Preservation (`selectedExistingExpense.totalAmountCents`)**:
   - Render horizontal selectable ticket tabs (`[ 🎫 PNR 2525492404 · ₹7,525.40 ]`, `[ 🎫 PNR 8753634406 · ₹3,240.00 ]`, `[ + Split New PNR ]`) in `PnrExpenseReviewScreen.kt:L500-L531`.
   - Derive `val selectedExistingExpense = remember(existingPnrExpensesInGroup, pnrInput) { existingPnrExpensesInGroup.find { extractTenDigitPnr(it.title) == pnrInput.filter(Char::isDigit).take(10) } }`.
   - When `selectedExistingExpense != null`:
     - Anchor `val effectiveTotalPaise = selectedExistingExpense.totalAmountCents` so IRCTC convenience/insurance fees (`2360` + `180` paise) are **never** added a second time onto an already all-inclusive `totalAmountCents`.
     - Hydrate `selectedMemberIds` and `selectedPayerId` from `selectedExistingExpense`, suppress the duplicate-PNR warning (`L544`), and wire the bottom CTA to `"Update Split for PNR ${snapshot.pnr}"` (`regenerating formattedTitle` via `formatTravelExpenseTitle` and calling `viewModel.editExistingExpense(...)`).
3. **Global `SplitMateMaterialTheme` Root Wiring, `BasicTextField` `tnum`, Dark-Mode `TactilePaperPassTokens` & WCAG AA Contrast**:
   - Add `fontFeatureSettings = "tnum"` to all 15 `SplitMateTypography` styles, wrap `SplitMateApp()` in `SplitMateMaterialTheme`, replace all 10 bare `TextStyle(tnum)` overrides with `LocalTextStyle.current.copy(fontFeatureSettings = "tnum")`, and add `fontFeatureSettings = "tnum"` to `BasicTextField` (`PnrExpenseReviewScreen.kt:L751`).
   - Make `TactilePaperPassTokens` dark-mode adaptive via `LocalSplitMatePalette.current`, pass explicit `OutlinedTextFieldDefaults.colors(focusedTextColor = TactilePaperPassTokens.InkPrimary, ...)` at `PnrExpenseReviewScreen.kt:L1354`, darken light-mode `InkMuted` to `Color(0xFF635D54)` (`4.96:1–5.48:1`) and `disabledContentColor` to `Color(0xFF524D47)` (`4.62:1`), wire haptics & `spatialScreenSpring()` / `expressiveComponentSpring()`, hoist `LazyListState`, and enforce `48.dp` touch targets + `Role.Checkbox`.

### 3. Exact Files Impacted
- [`android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/screens/OnboardingAndSettingsScreens.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/OnboardingAndSettingsScreens.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt)

---

## PHASE 4: Modular Codebase Decomposition, R8 Size Shrinking (`17.1 MB` → `~4.0 MB`) & Dead-Code Purge

### 1. Exact Audit Findings Addressed
- **`[CRITICAL] 3.1`, Appendix 2 `3.1` & Google Tink R8 Rules**:
  - `17.09 MB` APK bloat (`minifyEnabled false`, missing `shrinkResources true` & `proguardFiles`), Google Tink reflection under R8, variant APK filename collision, triplicated Figtree fonts (`figtree_variable.ttf` + `font_certs.xml`), and committed `16.98 MB` `splitmate-1.8.4.apk`.
- **`[HIGH] 3.2`, `[HIGH] 3.3`, Pass-3 Package Contract & `SplitMateComposeUiTest.kt`**:
  - 100% dead code (`HorizontalFloatingToolbar`, `ActivityItemRow`, `ClaimItemRow`, `SplitMateCircularLogoBadge`, `PlusJakartaSansFont`, `JetBrainsMonoFont`, 3 unused DAO methods), 6 duplicated `AsyncImage` blocks, monolithic God-Files (`SplitMateAppComposable.kt`, `SplitMateTheme.kt`), package preservation in `ui/SplitMateAppComposable.kt:L1`, and `SplitMateComposeUiTest.kt` (`L34, L40`).

### 2. Specific Architectural & Code Fixes Proposed
1. **R8 Shrinking, Google Tink + Room ProGuard Rules, Variant APK Naming & Resource Cleanup**:
   - Delete `splitmate-1.8.4.apk` (`16.98 MB`), add `*.apk` to `.gitignore`, delete `res/font/figtree_variable.ttf` (`62,712 B`), delete `res/values/font_certs.xml` (`4,184 B`), remove `ui-text-google-fonts:1.6.6`, and configure `minifyEnabled true`, `shrinkResources true`, `proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'`, and distinct `outputFileName` (`splitmate-1.9.0.apk` vs `splitmate-1.9.0-debug.apk`) in `android/app/build.gradle`.
   - Create [`android/app/proguard-rules.pro`](file:///usr/local/google/home/karadkar/splitmate/android/app/proguard-rules.pro) keeping `com.splitmate.app.data.**`, `com.google.crypto.tink.**`, `androidx.security.crypto.**`, `org.json.**`, and `coil.decode.SvgDecoder`.
2. **Dead-Code Purge, `SplitMateComposeUiTest.kt` Update & Singleton `DiceBearAvatar` Unification**:
   - Delete `HorizontalFloatingToolbar`, `ActivityItemRow`, `ClaimItemRow`, `SplitMateCircularLogoBadge`, `PlusJakartaSansFont`, `JetBrainsMonoFont`, and unused DAO queries (`getCurrencyRate`, `observeGroupMembers`, `markPendingExpensesSynced`).
   - Update [`SplitMateComposeUiTest.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/androidTest/java/com/splitmate/app/SplitMateComposeUiTest.kt) (`L34, L40`) to use `SplitMateMaterialTheme` and verify dashboard/QuickExpense navigation without referencing deleted `HorizontalFloatingToolbar`.
   - Consolidate all 6 duplicated `AsyncImage` blocks into `DiceBearAvatar` (`Components.kt:L63`) backed by `SplitMateApplication`'s singleton `ImageLoader`.
3. **Modular Extraction with Explicit Package & Import Contract**:
   - **Keep `package com.splitmate.app` in `ui/SplitMateAppComposable.kt:L1`** so `MainActivity.kt:L39`, `SplitMateComposeUiTest.kt:L35`, and `import com.splitmate.app.SplitMateTheme` across all screens never break.
   - Extract `PnrNetworkRepository.kt` (`package com.splitmate.app.data`), `ContactPickerSheet.kt` (`package com.splitmate.app.ui.components`), and `CompactTactileTicketStub.kt` (`package com.splitmate.app.ui.components`), adding explicit `import com.splitmate.app.data.*` and `import com.splitmate.app.ui.components.*` statements to consuming UI files.

### 3. Exact Files Impacted
- [`.gitignore`](file:///usr/local/google/home/karadkar/splitmate/.gitignore) & removal of [`splitmate-1.8.4.apk`](file:///usr/local/google/home/karadkar/splitmate/splitmate-1.8.4.apk)
- Removal of [`android/app/src/main/res/font/figtree_variable.ttf`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/res/font/figtree_variable.ttf) & [`android/app/src/main/res/values/font_certs.xml`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/res/values/font_certs.xml)
- [`android/app/build.gradle`](file:///usr/local/google/home/karadkar/splitmate/android/app/build.gradle)
- [`android/app/proguard-rules.pro`](file:///usr/local/google/home/karadkar/splitmate/android/app/proguard-rules.pro) *(New)*
- [`android/app/src/main/java/com/splitmate/app/data/PnrNetworkRepository.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/data/PnrNetworkRepository.kt) *(New)*
- [`android/app/src/main/java/com/splitmate/app/ui/components/ContactPickerSheet.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/components/ContactPickerSheet.kt) *(New)*
- [`android/app/src/main/java/com/splitmate/app/ui/components/CompactTactileTicketStub.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/components/CompactTactileTicketStub.kt) *(New)*
- [`android/app/src/main/java/com/splitmate/app/ui/Components.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/Components.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt)
- [`android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt)
- [`android/app/src/androidTest/java/com/splitmate/app/SplitMateComposeUiTest.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/androidTest/java/com/splitmate/app/SplitMateComposeUiTest.kt)

---

## Verification & Release Gate (`v1.9.0`)
1. **Unit & Math Verification (`./gradlew testDebugUnitTest`)**: All unit tests in `SplitMateMathEngineTest` and `SplitMateViewModelTurbineTest` compile and pass (`0` errors), verifying `0.00¢` (`0` paise) alignment across custom & default payers (`orderParticipantsPayerFirst`), `@Upsert` non-destructive member updates, dual-format PNR regex extraction, and the 5-stage `PnrNetworkRepository` token bucket.
2. **Release R8 APK Build & Size Verification (`./gradlew assembleRelease`)**: Confirm `splitmate-1.9.0.apk` compiles cleanly with KSP Room schema export and R8 (`minifyEnabled true`, `shrinkResources true`, Tink ProGuard rules) and shrinks from `17.09 MB` down to `~4.0 MB`.
3. **Single-Asset GitHub Release**: Publish `v1.9.0` (`versionCode = 16`) to GitHub Releases (`--latest`) with a single `splitmate-1.9.0.apk` asset.
