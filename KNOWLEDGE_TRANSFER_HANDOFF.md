# SplitMate (`v1.9.0`) — Principal Engineer & UI/UX Design System Knowledge Transfer (KT)

> **Last Updated**: Release `v1.9.0` (GitHub Release: https://github.com/akshaykaradkar/splitmate/releases/tag/v1.9.0)
> **Workspace Root**: `/usr/local/google/home/karadkar/splitmate`
> **Gradle Binary**: `/usr/local/google/home/karadkar/splitmate/.android-sdk/gradle-8.7/bin/gradle` (run with `Cwd = /usr/local/google/home/karadkar/splitmate/android`)

---

## 1. Mandatory Source-of-Truth Documents in Repository Root
Before making any UI, Room DB, Math, or PNR API change, any agent working in this repository MUST read:
1. **[`PRINCIPAL_ENGINEER_AUDIT_REPORT.md`](file:///usr/local/google/home/karadkar/splitmate/PRINCIPAL_ENGINEER_AUDIT_REPORT.md)** — The 3-Pass, 6-Pillar Staff/Principal Engineering Audit covering Material 3 Expressive & Figtree `tnum` typography, Zero Ghost Data, APK size bloat (`17.1 MB` → `2.3 MB`), Room v5 relational schema & `AES256-GCM` security, PNR network resilience, and `0.00¢` Largest Remainder math.
2. **[`PHASED_EXECUTION_PLAN.md`](file:///usr/local/google/home/karadkar/splitmate/PHASED_EXECUTION_PLAN.md)** (`v4.0 — Pass-3 Final L8 Sign-Off Edition`) & **[`EXECUTION_TASK_TRACKER.md`](file:///usr/local/google/home/karadkar/splitmate/EXECUTION_TASK_TRACKER.md)**.
3. **[`GEMINI.md`](file:///usr/local/google/home/karadkar/splitmate/GEMINI.md)** & **[`BRD.md`](file:///usr/local/google/home/karadkar/splitmate/BRD.md)** — Canonical Stitch "Organic Tactile Financial" (Buckwheat) design system rules and mathematical invariants.

---

## 2. Canonical Design Philosophy, The 4 Google Design Systems & 5 UI/UX Skills

### A. Primary Visual Identity — Stitch "Organic Tactile Financial" (Buckwheat Style) + "Tactile Luxury Paper Boarding Pass"
- **Canonical Spec**: [`stitch_splitmate_design_system/organic_tactile_financial/DESIGN.md`](file:///usr/local/google/home/karadkar/splitmate/stitch_splitmate_design_system/organic_tactile_financial/DESIGN.md)
- **Light Mode ("Warm Buckwheat Paper")**:
  - Canvas Base: `#FAF6F0` / `#FAF7F2`, Card Surface: `#FFFFFF` (`#FFFDF9` Paper Pass), Sunken Surface: `#F4EFE6`, Hairline Border: `1dp` `#EDE7DF` (`#E5DEC9` Paper Pass).
  - Primary Olive/Sage: `#365314` / `#416913` deep olive text & CTAs, `#D7E8B6` / `#DCE9B9` soft sage containers, `#264010` → `#1B2E0B` Forest Green Boarding Pass header.
  - Secondary Terracotta/Peach: `#E06B52` coral-terracotta CTA, `#FED8C8` / `#FCE3D7` peach container, `#7C2D12` dark terracotta text for `You Owe`, Unassigned Remainder, and Waitlist (`WL`) alerts.
  - Tertiary Periwinkle/Lavender: `#DCE3FD` container, `#3730A3` text.
  - Soft Charcoal Ink: `#23201E` (`#1E1C1A` Paper Ink) for primary typography and dark action pills.
- **Dark Mode ("Warm Espresso Leather-Journal Night")**:
  - Canvas Base: `#181512` (`#141311` Paper Pass), Card Surface: `#24201C` (`#1F1D1A` Paper Surface), Keypad/Stub Surface: `#2E2823` (`#282521`), Border: `#38312B` (`#38342E`), Primary Ink: `#FAF6F0` (`#F4EFEA`).
- **Strict Visual Anti-Patterns**:
  - **NEVER** use cold corporate blue/slate (`#0066FF`, `#F8FAFC`) or harsh drop shadows.
  - **NEVER** use `FontFamily.Monospace` or allow system Roboto fallback. Always use **`FigtreeFontFamily`** (bundled synchronously from Frame 0 in `res/font/figtree_*.ttf`) with OpenType tabular numerals (**`fontFeatureSettings = "tnum"`**) across all 14 `SplitMateTypography` slots and `SplitMateTnumMonospace`.

### B. The 4 Official Google Design Systems (`design_systems/` & `DesignSystemBindings` in `SplitMateTheme.kt`)
Every screen in SplitMate synthesizes these 4 official Google design systems:
1. **Google Material 3 Expressive (`design_systems/google-material-3/DESIGN.md`)**:
   - Governs HCT surface hierarchy (`GM3LightBackground`, `GM3DarkBackground`), expressive geometry (`GM3ShapeExtraLarge = 28.dp`, `GM3ShapeLarge = 24.dp`, `GM3ShapePill = 50%`), and strict `48dp × 48dp` minimum accessible touch targets.
2. **Android Motion (`design_systems/android-motion/DESIGN.md`)**:
   - Governs physics-driven spatial springs (`SmoothSettleSpring = spring(dampingRatio = 0.8f, stiffness = 380f)`, `tactileSpring()`, `Spring.DampingRatioLowBouncy`) and `themeColorTween(400ms)` for dark/light transitions.
   - All interactive chips, toggles, numpad keys, and CTAs must trigger crisp tactile haptics via `LocalHapticFeedback.current` / `performCrispTactileHaptic(context, view)` (respecting `EncryptedPrefsProvider` `"haptic_feedback_enabled"`).
3. **Elements GM3 (`design_systems/elements-3/DESIGN.md`)**:
   - Governs semantic financial polarity tokens: `ElementsCreditorContainer` (`#D7E8B6` / `#2D4810`) for Owed/Confirmed (`CNF`), `ElementsDebtorContainer` (`#FED8C8` / `#7C2D12`) for You Owe/Unassigned Remainder/Waitlist (`WL`), and `ElementsInfoPeriwinkle` (`#DCE3FD` / `#3730A3`) for RAC/Chart info.
4. **Android Pixel Design System (`design_systems/android-pixel-design-system/DESIGN.md`)**:
   - Governs editorial display typography (`FigtreeFontFamily` + `"tnum"`), Pixel spacing rhythm (`PixelCardInternalPadding = 14.dp`, `PixelSectionSpacing = 12.dp`, `PixelCompactItemSpacing = 8.dp`), and scannable feed density (`CompactLedgerTicketStub` `76dp` single-row ticket pill in Group Overview & Activity feeds, launching the full `PnrExpenseReviewScreen` Tactile Paper Studio on tap).

### C. The 5 Specialized UI/UX Skills (`.agents/skills/`)
Whenever designing, modifying, or auditing UI screens, read and apply these workspace skills via `view_file`:
- **SplitMate Master Builder & Assessor**: `/usr/local/google/home/karadkar/splitmate/.agents/skills/splitmate-m3-expressive-assessor-and-builder/SKILL.md`
- **Google Material 3 Assessor**: `/usr/local/google/home/karadkar/splitmate/.agents/skills/google-material-3-visual-assessor/SKILL.md`
- **Android Motion Assessor**: `/usr/local/google/home/karadkar/splitmate/.agents/skills/android-motion-visual-assessor/SKILL.md`
- **Elements GM3 Financial Assessor**: `/usr/local/google/home/karadkar/splitmate/.agents/skills/elements-3-visual-assessor/SKILL.md`
- **Android Pixel Design System Assessor**: `/usr/local/google/home/karadkar/splitmate/.agents/skills/android-pixel-design-system-visual-assessor/SKILL.md`

---

## 3. Summary of Completed 4-Phase Engineering Overhaul (`v1.8.7` → `v1.9.0`)

### Phase 1 (`1060d79` / rebased in `ba70901`): Database Integrity, Security & Lifecycle Safety
- **Room `version = 5` Schema (`RoomEntities.kt` & `SplitMateRoomDatabase.kt`)**:
  - Enforced **6 `ForeignKey` (`ON DELETE CASCADE`)** constraints and **11 indices** across `group_members`, `expenses`, `expense_splits`, and `settlements`.
  - Implemented topological **`MIGRATION_4_5`** (`group_members` → `expenses` → `expense_splits` → `settlements`) with 8-path orphan cleanup and replaced `.fallbackToDestructiveMigration()` with `.addMigrations(MIGRATION_4_5).fallbackToDestructiveMigrationOnDowngrade()`.
- **`@Upsert` Safety (`SplitMateDao.kt`)**:
  - Replaced `@Insert(onConflict = OnConflictStrategy.REPLACE)` with `@Upsert` across all 7 DAO methods so updating a member or profile never triggers `DELETE` + `ON DELETE CASCADE` on `ExpenseSplitEntity`.
- **Hardware-Backed `AES256-GCM` Vault (`EncryptedPrefsProvider.kt`)**:
  - Created `EncryptedPrefsProvider.get(context)` (`splitmate_pnr_secure_vault`) with self-healing `KeyStoreException`/`AEADBadTagException` recovery and one-time migration + wipe of legacy cleartext `"splitmate_pnr_rate_guard"`.
- **Lifecycle-Aware Collectors**:
  - Migrated all 18 `collectAsState()` calls across `MainActivity.kt`, `SplitMateAppComposable.kt`, `PnrExpenseReviewScreen.kt`, and `QuickExpenseAndGuideScreens.kt` to `collectAsStateWithLifecycle()`.

### Phase 2 (`6350c3d` / rebased in `ba70901`): Financial Math (`0.00¢` Drift), Zero Ghost Data & PNR Network Resilience
- **Payer-First Largest Remainder & Custom Payer Selection (`SplitMateMathEngine.kt`)**:
  - Added `SplitMateMathEngine.orderParticipantsPayerFirst(memberIds, payerId, currentUserId)` so all 3 split paths (`SplitMateViewModel`, `PnrExpenseReviewScreen`, `QuickExpenseScreen`) deterministically assign `+1` paise remainder pennies to the actual Payer first (`0.00¢` drift).
  - Added `"Paid by: [Member]"` horizontal chip selectors in `PnrExpenseReviewScreen` and `QuickExpenseScreen`.
- **Zero Ghost Data & Manual Ticket Entry Mode**:
  - Deleted `OfflineIndianTrainCatalog`, `OfflineTrainIntermediateRadar`, `enrichTicketWithOfflineCatalog`, and the modulo-hash synthetic WL/RAC/CNF generator while preserving `OfflineStationNames`.
  - When live PNR lookup is unreachable (`isLiveVerified == false`), `PnrExpenseReviewScreen` populates `LivePnrStatusSnapshot(..., isLiveVerified = false, isManualEntry = true)` to unlock the **Manual Ticket Fare & Route Card** (`manualTotalFareInput`, `manualFromStationInput`, `manualToStationInput`).
- **5-Stage `PnrNetworkRepository.kt` & Idempotent PNR Deduplication**:
  - L1 `LruCache(32)` + L2 `EncryptedSharedPreferences` → 6-hour positive TTL / 60s negative debounce → in-flight `Mutex` request coalescing (`inFlightPnrDeferreds`) → sliding 5-calls-per-5-minutes token bucket → `4,000ms` socket timeouts.
  - Rejects duplicate 10-digit PNRs per group and locks `effectiveTotalPaise = selectedExistingExpense.totalAmountCents` when inspecting/editing logged tickets so IRCTC convenience/insurance fees are never double-counted.

### Phase 3 (`c2966e9` / rebased in `ba70901`): Figtree `tnum` Typography, Dark-Mode Tactile Paper Pass & Ticket De-Cluttering
- Universal `FigtreeFontFamily` + `"tnum"` across all 14 `SplitMateTypography` slots and `SplitMateTnumMonospace`.
- Dark-mode adaptive `TactilePaperPassTokens` + `tactileTextFieldColors()` + `SmoothSettleSpring` + crisp haptics.
- Replaced multi-hundred-dp inline boarding passes in Group Overview and Activity Audit rows with `CompactLedgerTicketStub` (`76dp` single-row pill) + Group **"Tactile Paper IRCTC PNR Studio"** Hero Launcher Card.

### Phase 4 (`ba70901`): APK Bloat Elimination (`17.1 MB` → `2.3 MB`), Dead Code Purge & Modularization
- Enabled R8 (`minifyEnabled true`, `shrinkResources true`, `resConfigs "en"`) in `android/app/build.gradle` (`versionCode 16`, `versionName "1.9.0"`), shrinking `splitmate-1.9.0.apk` by **86.5%** from `17.1 MB` down to **`2.3 MB`**.
- Extracted dialog/atomic composables into `ui/dialogs/GroupAndSettlementDialogs.kt` and PNR domain models/parsers into `ui/pnr/PnrTicketModels.kt`.
