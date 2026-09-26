# Report 05: Material GenUX & Google Material 3 Expressive Compliance Audit Report

> **Auditor**: Principal Material GenUX Kit & Google Material 3 Expressive Design Philosophy Auditor (`genux` + `google-material-3-visual-assessor` + `splitmate-m3-expressive-assessor-and-builder`)
> **Alignment Score**: **97.6 / 100** (**100 / 100** with the 5 pre-implementation micro-adjustments below)
> **Verdict**: **APPROVED FOR IMPLEMENTATION**

---

## 1. Philosophy Alignment Score Breakdown (`97.6 / 100`)

| Dimension | Score | Key Audit Findings |
| :--- | :---: | :--- |
| **1. Tonal Hierarchy & Surface Elevation** | **98 / 100** | Replaces harsh drop shadows with HCT tonal steps (`#FAF6F0` canvas, `#FFFFFF` / `#24201C` cards, `#F4EFE6` / `#2E2823` sunken wells) framed by `1.dp` hairline borders (`#EDE7DF` / `#38312B`). Seamless Warm Espresso (`#181512`) Dark Mode. |
| **2. Typography & Scannability** | **98 / 100** | Bundled offline Figtree font family (400–800) + strict `fontFeatureSettings = "tnum"` on every financial figure, PNR, train number, berth code, date, and net balance. Zero Unicode emojis (`Icons.Rounded.*` only). |
| **3. Expressive Shape & Component Anatomy** | **96 / 100** | Canonical GM3 Expressive radii (`24.dp`–`28.dp` cards, full `CircleShape` pill filter tabs/chips, perforated ticket divider). |
| **4. Generative / Adaptive UX ("Zero-Placeholder Surface")** | **99 / 100** | 100% real Room data: category chips (`Trains`, `Flights`, `Stays`, `Rentals`, `Cabs`) and booking cards materialize **only** when backed by real logged expenses. Instant (`<16ms`) perspective re-projection (`Viewing as: <Member> (You)`). |
| **5. Motion & Multisensory Choreography** | **97 / 100** | Full preservation of `24kHz` procedural PCM paper-tear & gate-stamp acoustics, 3D foldable passes, `Gm3AuroraEnergySurface`, and Android Motion `tactileSpring()` physics. |

---

## 2. Five Micro-Adjustments Locked In for `100 / 100` Implementation

1. **`48.dp` Minimum Touch Targets (`WCAG 2.5.5`) on Compact Pills & Chips**:
   - Wrap compact visual pills (`PerspectiveAndSyncHeaderPill`, `Berth Chart`, `Maps`, `View E-Ticket`, and category filter chips) with `Modifier.defaultMinSize(minHeight = 48.dp)` (or `minimumInteractiveComponentSize()`) so touch targets are `48.dp` even when the visual pill height is `32.dp`–`38.dp`.
2. **Spatial Spring Choreography on Dynamic Category Filtering & Card Expansion**:
   - Apply `Modifier.animateItem()` on `TripHomeScreen` `LazyColumn` items and `Modifier.animateContentSize(animationSpec = DesignSystemBindings.tactileSpring())` on expandable expense breakdown drawers.
3. **Sunlight-Grade Contrast ($\ge 5.8:1$) on Deep-Green Train & Navy Flight Cards**:
   - On the Deep-Forest Green Train Card (`#2D4F12` -> `#213B0C`), use pure `#FFFFFF` for station codes/titles, `#B5DC86` (`> 7:1`) for berth/PNR highlights, and `#D7E8B6` (`> 5.8:1` soft sage) for secondary station/time labels instead of muted grey.
4. **Canonical M3 `ModalBottomSheet` Tokens on `TripSyncAndPerspectiveSheet.kt`**:
   - Use `shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)`, `containerColor = SplitMateTheme.ScreenBg`, `scrimColor = Color.Black.copy(alpha = 0.55f)`, and explicit `BottomSheetDefaults.DragHandle(color = SplitMateTheme.BorderLight, width = 36.dp, height = 4.dp)`.
5. **Progressive Disclosure on `>4` Passenger Berths + Tactile Haptic on Perspective Switch**:
   - Show the 2x2 berth grid for up to 4 passengers (with `"YOU"` badge on the active perspective member) and a `+N more passengers` pill if `>4` passengers exist; fire `performCrispTactileHaptic(context, heavy = false)` whenever the user taps a member chip to switch perspective.
