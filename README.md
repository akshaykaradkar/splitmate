# SplitMate `v2.0.0` — The Premium Trip Hub & `$0.00` Multi-Member P2P Sync

**SplitMate `v2.0.0` (`versionCode 33`)** is a 100% Native Android (**Kotlin + Jetpack Compose Material 3 Expressive + Room SQLite `v8`**) offline-first group expense & travel ledger application styled in the **Stitch "Organic Tactile Financial" (Buckwheat)** visual identity and engineered to the **4 Official Google Design Systems** (`Google Material 3 Expressive`, `Elements GM3`, `Android Motion`, and `Android Pixel Design System`).

---

## Direct Android APK Download (`v2.0.0` · `versionCode 33`)

- **[Download `splitmate-2.0.0.apk` (GitHub Release `v2.0.0`)](https://github.com/akshaykaradkar/splitmate/releases/download/v2.0.0/splitmate-2.0.0.apk)** (`2.6 MB`, R8-minified release build, `minSdk 26` / `targetSdk 34`, APK Signature Scheme v2/v3)
- **[Repository Root APK (`./splitmate-2.0.0.apk`)](./splitmate-2.0.0.apk)**
  - **SHA-256**: `f43a01aa0ef217341d3adb1111576a40006f3fe4a50b65174ee7393b35fa54c5`
  - **Version**: `2.0.0` (`versionCode 33`)

---

## What's New in SplitMate `v2.0.0` ("The Premium Trip Hub")

### 1. Unified Trip Hub Canvas & Adaptive Terminology ([`TripHomeScreen.kt`](./android/app/src/main/java/com/splitmate/app/ui/screens/TripHomeScreen.kt))
- **3-Tab Scoped Architecture (`Bookings` · `Money` · `People`)**: Consolidates trains, flights, hotels, dining, and general shared expenses into a single progressive-disclosure trip workspace while preserving a 1-tap toggle back to the Classic Ledger view.
- **Sunlight-Grade Contrast ($\ge 5.8:1$) Deep-Green IRCTC Train Card & Aviation Periwinkle Flight Pass**:
  - Live or offline-parsed PNR cards with `#FFFFFF` station headers, `#B5DC86` (`> 7:1` lime) PNR & berth badges, and `#D7E8B6` (`> 5.8:1` soft sage) secondary labels.
  - **Progressive Disclosure on $>4$ Berths**: Renders a compact $2 \times 2$ berth grid by default with an expandable `"View All N Passengers"` spring-physics drawer (`Modifier.animateContentSize(tactileSpring())`).
- **Adaptive Group Terminology**: Automatically switches between `"Travelers"` / `"Trip Spend"` for travel groups and `"Members"` / `"Group Spend"` for household, dining, or event groups.

### 2. `$0.00` Serverless Multi-Member P2P Sync & Perspective Switcher ([`GroupSyncPayloadCodec.kt`](./android/app/src/main/java/com/splitmate/app/data/GroupSyncPayloadCodec.kt) & [`TripSyncAndPerspectiveSheet.kt`](./android/app/src/main/java/com/splitmate/app/ui/dialogs/TripSyncAndPerspectiveSheet.kt))
- **Compact `SM2:` GZIP + Base64URL Sync Token**: Encodes the entire group roster, expenses, item-level splits, and settlement state into a URL-safe offline token shareable via WhatsApp, SMS, QR code, clipboard, or Android `https://splitmate.app/sync` / `splitmate://sync` deep links and `ACTION_SEND` (`text/plain`) share targets.
- **256 KB Decompression Bomb Guard & Idempotent Merge**: Defends against malformed payloads and deduplicates expenses deterministically via `expenseSyncUid` + content-hash matching inside an atomic Room `@Transaction` (`importAndMergeGroupSyncPayload`).
- **Instant In-App Perspective Switcher (`Viewing as: <Member>`)**: Tap the header pill on any device to recompute all `"You are owed"` / `"You owe"` net balances, expense cards, and `Settle Up` Max-Heap transfers from any group member's point of view—persisted per-group in Room (`activePerspectiveMemberName`).

### 3. Zero-Drift (`0.00¢`) Mathematical Engine ([`SplitMateMathEngine.kt`](./android/app/src/main/java/com/splitmate/app/SplitMateMathEngine.kt))
- **100% Integer Cents / Paise (`Long`) Storage**: Zero floating-point representation drift across Room entities, ViewModel state, and UI previews.
- **Locked Proportional Auxiliary Multiplier ($m = T / B$)**: Distributes tax, GST, convenience fees, and tips strictly in proportion to each participant's claimed base subtotal.
- **Real-Time Remainder Engine**: Holds unassigned receipt remainders temporarily on the Payer with a striped Terracotta/Peach alert banner and 1-tap `Split Remainder Equally` reconciliation.
- **Largest Remainder Method (`0.00¢ Drift`)**: Reconciles indivisible pennies (`₹100.00 ÷ 3 = ₹33.34 + ₹33.33 + ₹33.33`) deterministically (Payer-first, then stable `memberId` tie-breaking), with `1:1` parity between live UI previews and persisted Room splits.
- **Greedy Minimum Cash Flow Simplification**: Uses two Max-Priority Queues (`PriorityQueue` for Creditors $V_+$ and Debtors $V_-$) to collapse $O(N^2)$ cross-debts into at most $N - 1$ optimal transfers.

### 4. Material GenUX, Stitch "Organic Tactile Financial" & Dark Mode Parity
- **Stitch Buckwheat Palette (Light & Warm Espresso Dark Mode)**:
  - **Light Mode**: Buckwheat Cream canvas (`#FAF6F0` / `#FCF9F4`), Pure White cards (`#FFFFFF`) with `#EDE7DF` borders, Deep Olive/Sage (`#416913` / `#D7E8B6`) for positive/settled states, Terracotta/Peach (`#E06B52` / `#FED8C8`) for debt/remainder alerts, and Aviation Periwinkle (`#3730A3` / `#EEF2FF`) for transit/sync surfaces.
  - **Dark Mode**: Warm Obsidian/Espresso canvas (`#161412` / `#24201C`) with adaptive contrast across [`Gm3AuroraEnergySurface`](./android/app/src/main/java/com/splitmate/app/ui/components/AuroraEnergySurface.kt), [`QuickExpenseScreen`](./android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt), [`FlightExpenseReviewScreen`](./android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt), and [`PnrExpenseReviewScreen`](./android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt).
- **WCAG 2.5.5 `48.dp` Touch Targets & Tabular Numerals (`tnum`)**: Enforces `.minimumInteractiveComponentSize()` across compact chips/buttons and OpenType `fontFeatureSettings = "tnum"` on all monetary figures.
- **Zero-Emoji Vector Purity**: 100% Material 3 Rounded vector icons (`Icons.Rounded` & `Icons.AutoMirrored.Rounded`) with zero Unicode emojis in Kotlin source code.

### 5. Offline Transit & Receipt Intelligence + Memory-Safe QR Decoding
- **IRCTC Train PNR Studio ([`PnrExpenseReviewScreen.kt`](./android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt))**: Live & offline 10-digit PNR lookup, coach/berth parser, and UPI/Card IRCTC convenience fee calculator.
- **Universal Flight Boarding Pass & E-Ticket PDF Extractor ([`UniversalFlightTicketExtractor.kt`](./android/app/src/main/java/com/splitmate/app/data/UniversalFlightTicketExtractor.kt) & [`FlightExpenseReviewScreen.kt`](./android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt))**: Multi-passenger PDF/OCR extraction with per-seat tax/ancillary breakdown.
- **UPI Express Payment Sheet ([`UpiExpressPaymentSheet.kt`](./android/app/src/main/java/com/splitmate/app/ui/components/UpiExpressPaymentSheet.kt))**: Deep-link UPI intent launcher (`upi://pay`) plus memory-safe 2-pass ZXing QR decoding (`inJustDecodeBounds` + power-of-two `inSampleSize <= 1600px` + `Bitmap.recycle()`).

---

## Architecture & Audit Documentation (`docs/v2_architecture_reports/`)

| Report | Document | Summary |
| :---: | :--- | :--- |
| **00** | [`00_SPLITMATE_V2_MASTER_SOLUTION_ARCHITECTURE.md`](./docs/v2_architecture_reports/00_SPLITMATE_V2_MASTER_SOLUTION_ARCHITECTURE.md) | End-to-End `v2.0.0` Solution Architecture, Room `v8` Schema, and `$0.00` P2P Sync Blueprint |
| **01** | [`01_SPLITMATE_V2_MASTER_TASK_AND_SUBTASK_LIST.md`](./docs/v2_architecture_reports/01_SPLITMATE_V2_MASTER_TASK_AND_SUBTASK_LIST.md) | 5-Phase Master Execution Tracker (`T1.1`–`T5.2`, `10 / 10 COMPLETED`) |
| **06–10** | [Phase 1](./docs/v2_architecture_reports/06_phase1_strict_qa_audit_report.md) · [Phase 2](./docs/v2_architecture_reports/07_phase2_strict_qa_audit_report.md) · [Phase 3](./docs/v2_architecture_reports/08_phase3_strict_qa_audit_report.md) · [Phase 4](./docs/v2_architecture_reports/09_phase4_strict_qa_audit_report.md) · [Phase 5](./docs/v2_architecture_reports/10_phase5_strict_qa_audit_report.md) | Per-Phase Strict QA & R8 Guardrail Audit Reports |
| **11** | [`11_WHOLE_APP_MATERIAL_GENUX_AND_ANDROID_ARCHITECTURE_AUDIT.md`](./docs/v2_architecture_reports/11_WHOLE_APP_MATERIAL_GENUX_AND_ANDROID_ARCHITECTURE_AUDIT.md) | Baseline Whole-App (`v1.0`–`v2.0.0`) Material GenUX & Android Architecture Audit |
| **12** | [`12_phase6_whole_app_polish_strict_qa_audit_report.md`](./docs/v2_architecture_reports/12_phase6_whole_app_polish_strict_qa_audit_report.md) | Phase 6 Whole-App Polish Strict QA Verification Report (`17 / 17 PASS`) |
| **13** | [`13_GEMINI_PRO_WHOLE_APP_MATERIAL_GENUX_AND_ANDROID_MASTER_AUDIT.md`](./docs/v2_architecture_reports/13_GEMINI_PRO_WHOLE_APP_MATERIAL_GENUX_AND_ANDROID_MASTER_AUDIT.md) | **Final Whole-App Master Audit (`100 / 100 RELEASE CERTIFIED`)** |

---

## Build, Test & Verify Locally

```bash
cd android
/usr/local/google/home/karadkar/splitmate/.android-sdk/gradle-8.7/bin/gradle testDebugUnitTest assembleRelease
```

- **Unit Test Suites**:
  - [`SplitMateV2ZeroRegressionAndSyncTest.kt`](./android/app/src/test/java/com/splitmate/app/SplitMateV2ZeroRegressionAndSyncTest.kt) — Multi-Member `SM2:` P2P Sync, 256 KB bomb defense, perspective switching, and `0.00¢ drift` invariance tests.
  - [`SplitMateDebtSimplificationTest.kt`](./android/app/src/test/java/com/splitmate/app/SplitMateDebtSimplificationTest.kt) — Greedy Minimum Cash Flow & Largest Remainder unit tests.
  - [`UniversalFlightTicketExtractorTest.kt`](./android/app/src/test/java/com/splitmate/app/UniversalFlightTicketExtractorTest.kt) — Multi-airline PNR, IATA route chain, and fare parser tests.
- **Release Output**: `android/app/build/outputs/apk/release/splitmate-2.0.0.apk`
