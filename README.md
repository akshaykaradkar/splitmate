# SplitMate — 100% Native Android (Kotlin + Jetpack Compose Material 3 Expressive)

**SplitMate** is an offline-first, peer-to-peer group expense management Android application built with **100% Native Kotlin & Jetpack Compose (Material 3 Expressive)**, **Jetpack Room**, **Retrofit (`Frankfurter Currency API`)**, and **Coil (`SvgDecoder`)**, styled in the **Stitch "Organic Tactile Financial" (Buckwheat)** visual identity.

---

## 📲 Direct Android APK Download (`v3.0.0-native-compose`)

- **[Download `splitmate-debug.apk` (v3.0.0 Release)](https://github.com/akshaykaradkar/splitmate/releases/download/v3.0.0/splitmate-debug.apk)** (`minSdk 26` / `targetSdk 34`, signed with APK Signature Scheme v2)
- **[Root Repository APK](./splitmate-debug.apk)**

---

## 🏗️ Native Android Architecture

1. **100% Native Jetpack Compose UI ([`SplitMateAppComposable.kt`](./android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt) & [`MainActivity.kt`](./android/app/src/main/java/com/splitmate/app/MainActivity.kt))**:
   - Zero WebView, zero HTML/JS assets.
   - Implements Material 3 Expressive `HorizontalFloatingToolbar` ([`Components.kt`](./android/app/src/main/java/com/splitmate/app/ui/Components.kt)) for the bottom NumPad & quick-entry cluster (`+$1`, `+$5`, `+$10`, `+18% Tip`), collapsing into a Floating Action Button via spring physics (`Spring.DampingRatioLowBouncy`) on scroll.
   - Enforces `Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)` across all interactive controls.
   - Uses `remember` + `derivedStateOf` for heavy graph/remainder calculations and explicit `key`s in every `LazyColumn`.

2. **Live Currency Rates & Offline-First Caching ([`FrankfurterApiService.kt`](./android/app/src/main/java/com/splitmate/app/data/FrankfurterApiService.kt) & [`SplitMateRoomDatabase.kt`](./android/app/src/main/java/com/splitmate/app/data/SplitMateRoomDatabase.kt))**:
   - Fetches live world conversion rates at runtime from `https://api.frankfurter.dev/v1/latest` via **Retrofit** and immediately persists them into **Jetpack Room** (`CurrencyRateEntity`) for offline use.
   - **Transaction Exchange Rate Locking**: Locks `lockedExchangeRate` inside `ExpenseEntity` at the exact timestamp an expense is created so historical debts never drift. Offline transactions automatically queue as `syncStatus = "PENDING"`.

3. **Dynamic SVG Avatars with Indefinite Disk Cache ([`SplitMateApplication.kt`](./android/app/src/main/java/com/splitmate/app/SplitMateApplication.kt))**:
   - Generates illustrated character portraits dynamically via the **DiceBear Open-Peeps API** (`https://api.dicebear.com/9.x/open-peeps/svg?seed={userName}`).
   - Configures a singleton `ImageLoader` with `SvgDecoder.Factory()` (`coil-compose` + `coil-svg`) and a 50 MB persistent `DiskCache` (`respectCacheHeaders(false)`) plus a local Android Vector Drawable fallback ([`ic_avatar_placeholder.xml`](./android/app/src/main/res/drawable/ic_avatar_placeholder.xml)).

4. **Collaborative Manual Entry & Mathematical Engines ([`SplitMateMathEngine.kt`](./android/app/src/main/java/com/splitmate/app/SplitMateMathEngine.kt))**:
   - **Proportional Auxiliary Splitting**: Locks $m = T / B$ against the receipt's Base Subtotal and distributes tax & tip proportionally.
   - **Real-Time Remainder Engine**: Tracks `unassignedBaseCents` & `unassignedFinalCents` with a 1-tap `Split Remainder Equally` action.
   - **Largest Remainder Method (`0.00¢ Drift`)**: Deterministic integer-cent (`Long`) penny reconciliation.
   - **Greedy Minimum Cash Flow Algorithm**: Uses two `PriorityQueue` Max-Heaps ($V_+$ and $V_-$) to reduce $O(N^2)$ cross-debts into at most $N - 1$ transfers.

5. **Automated QA & Test Suite**:
   - **JUnit 5**: [`SplitMateDebtSimplificationTest.kt`](./android/app/src/test/java/com/splitmate/app/SplitMateDebtSimplificationTest.kt)
   - **CashApp Turbine**: [`SplitMateViewModelTurbineTest.kt`](./android/app/src/test/java/com/splitmate/app/SplitMateViewModelTurbineTest.kt)
   - **Compose UI Test Rule**: [`SplitMateComposeUiTest.kt`](./android/app/src/androidTest/java/com/splitmate/app/SplitMateComposeUiTest.kt)
