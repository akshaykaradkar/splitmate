# Report 02: `TripHomeScreen.kt` UI/UX, Decluttered Layout & Component Specification (Option A)

**Author**: Subagent 2 (`v2_trip_hub_gm3_ui_declutter_lead`)  
**Target Surface**: `com.splitmate.app.ui.screens.TripHomeScreen.kt`  
**Visual North Star**: [`Debug/screen.png`](file:///usr/local/google/home/karadkar/splitmate/Debug/screen.png) + Stitch *Organic Tactile Financial* ([`DESIGN.md`](file:///usr/local/google/home/karadkar/splitmate/stitch_splitmate_design_system/organic_tactile_financial/DESIGN.md)) + [`SplitMateTheme.kt`](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt)

---

## 1. Complete Visual & Token Specification for `TripHomeScreen.kt`

### 1.1 Light & Dark Mode Color Tokens (`TripHubTokens`)
All colors adapt seamlessly between **Buckwheat Warm Cream (Light)** and **Warm Espresso Leather-Journal (`#181512` Dark)** while keeping the iconic **Deep Forest Green Train Pass** and **Aviation Periwinkle Flight Pass** rich and high-contrast in both modes.

| Token Name | Light Mode (`!SplitMateTheme.isDark`) | Dark Mode (`SplitMateTheme.isDark`) | Usage in `TripHomeScreen.kt` |
| :--- | :--- | :--- | :--- |
| `CanvasBg` | `#FAF6F0` (`BuckwheatCanvas`) | `#181512` (`GM3DarkBackground`) | Root screen background |
| `CardSurface` | `#FFFFFF` (`BuckwheatSurface`) | `#24201C` (`GM3DarkCardSurface`) | Lodging, Mobility, Return Transit & General Expense cards |
| `SunkenWell` | `#F4EFE6` (`BuckwheatSunken`) | `#2E2823` (`GM3DarkKeypadSurface`) | Check-In/Check-Out box, Payer split box, inactive filter pills, action chips |
| `CardBorder` | `#EDE7DF` (`BuckwheatBorder`) | `#38312B` (`GM3DarkBorder`) | `1.dp` soft hairline border on all cards |
| `TextPrimary` | `#23201E` (`BuckwheatCharcoal`) | `#FAF6F0` (`GM3DarkPrimaryText`) | Titles, station names, amounts, check-in/out times |
| `TextSecondary` | `#6E675F` (`BuckwheatSecondaryText`) | `#B5ACA2` (`GM3DarkSubtitleText`) | Subtitles, route descriptions, payer labels |
| `TextMuted` | `#8C857B` | `#857D73` | Section headers (`UPCOMING DEPARTURE`, `LODGING & STAYS`), booking refs |
| `ActiveTabPillBg` | `#1E1C1A` (Deep Charcoal) | `#FAF6F0` (Warm Cream) | Selected Top Tab (`Overview \| Travel \| Money \| People`) & active filter chip |
| `ActiveTabPillText` | `#FFFFFF` | `#181512` | Text inside active top tab / active filter chip |
| `InactiveTabPillBg` | `#EFEAE1` | `#28241F` | Unselected Top Tab & unselected filter chips (`Trains`, `Stays`, etc.) |
| `InactiveTabPillText` | `#4A453E` | `#C5BDB3` | Text inside unselected top tab / filter chip |
| `TrainForestTop` | `#2D4F12` | `#264210` | Upper gradient start of Deep-Green Train Card |
| `TrainForestBottom` | `#213B0C` | `#1B3009` | Upper gradient end of Deep-Green Train Card |
| `TrainNextUpPillBg` | `#84A950` | `#759943` | `"NEXT UP · TOMORROW 11:30 AM"` rounded pill inside Train Card |
| `TrainNextUpPillText` | `#132604` | `#0F1F03` | High-contrast dark forest text inside `"NEXT UP"` pill |
| `TrainBerthCellBg` | `#223D0D` | `#1A300A` | Individual passenger berth card background (`B2 · 41 (MB) Akshay`) |
| `TrainBerthCellBorder` | `#3E651E` | `#345718` | `1.dp` border around each passenger berth cell |
| `TrainAccentLime` | `#B5DC86` | `#B5DC86` | Sunlight-grade contrast (`> 7:1`) for berth code (`B2 · 41`), platform time, PNR digits |
| `TrainSecondarySage` | `#D7E8B6` | `#D7E8B6` | Sunlight-grade contrast (`> 5.8:1` soft sage) for secondary station/time labels (never muted grey) |
| `TrainPrimaryCtaBg` | `#80AD47` | `#8BB85A` | `"View E-Ticket"` primary button on Train Card |
| `TrainPrimaryCtaText` | `#132604` | `#132604` | Text & icon tint on `"View E-Ticket"` button |
| `FlightNavyTop` | `#2B2768` | `#242059` | Upper gradient start of Aviation Periwinkle Flight Card |
| `FlightNavyBottom` | `#1B1849` | `#16133B` | Upper gradient end of Aviation Periwinkle Flight Card |
| `PositiveSageText` | `#416913` | `#D7E8B6` | Per-person split text (`₹1,881.35 / traveler`), `"Confirmed Booking"` |
| `PositiveSagePillBg` | `#D7E8B6` | `#233216` | `"Assigned"`, `"Confirmed"` status pills |
| `WarningRacText` | `#9A3412` | `#FDBA74` | `"3A Sleeper · RAC 11–14"` or Waitlist (`WL`) status text |
| `TerracottaPeachBg` | `#FCE3D7` | `#3D231B` | Two-wheeler rental icon box (`#FCE3D7`) & `"AK"` avatar circle |
| `TerracottaIconTint` | `#9A3412` | `#FEB49C` | Two-wheeler icon tint & `"AK"` avatar text |
| `PeriwinkleBoxBg` | `#DCE3FD` | `#282552` | Cab/Station Transfer icon box (`#DCE3FD`) & Return Transit icon circle |
| `PeriwinkleIconTint` | `#3730A3` | `#C7D2FE` | Cab/Train icon tint & `"NI"` / `"GA"` avatar text |

---

### 1.2 Zero-Removal Guarantee + Decluttered Card Architecture + 5 GM3/GenUX Micro-Refinements
1. **100% of Existing `v1.9.16` Features Are Preserved**:
   - `PaperSensoryFeedbackBanner` (`Paper Tear & Gate-Stamp Acoustics` toggle + `24kHz` PCM sound) in `FlightExpenseReviewScreen.kt` and `PnrExpenseReviewScreen.kt` is **NOT removed**.
   - The 3D Foldable Boarding Pass, `[ LOGGED · 0.00¢ DRIFT ]` stamp, Aurora Energy surfaces, Coin Arc Flight, and Classic 3D Travel Pass remain **100% intact**.
2. **What We Cut From Raw `Debug/screen.png` (Option A — Zero Fake Placeholders)**:
   - Cut fake static tourist props (`GROUP TRAVEL VAULT: Govt Photo IDs / 4 Aadhaar Passes` & `ASI Hampi Monument Passes`).
   - **Zero Demo Seeders & Zero Fake Dates**: No `"Load Hampi Trip Demo"` seeder button, no hardcoded `"13 Nov - 17 Nov"` top bar subtitle, and no fake `"14 Nov, 12:00 PM"` hotel check-in timestamps.
   - Dynamic category sub-filter chips (`All`, `Trains`, `Flights`, `Stays`, `Rentals`, `Cabs`) and section cards materialize **strictly when `count > 0` in Room**.
3. **5 Mandatory Material GenUX & GM3 Expressive Micro-Refinements (`100 / 100` Compliance)**:
   - **Refinement 1 — `48.dp` Minimum Touch Targets (`WCAG 2.5.5`)**: Apply `minimumInteractiveComponentSize()` / `48.dp` touch bounds on all compact visual chips (`PerspectiveAndSyncHeaderPill`, `Berth Chart`, `Maps`, `View E-Ticket`, and category filter pills).
   - **Refinement 2 — Spring Choreography on Dynamic Filtering**: Apply `Modifier.animateItem()` on `TripHomeScreen` `LazyColumn` cards and `Modifier.animateContentSize(tactileSpring())` on expandable split drawers.
   - **Refinement 3 — Sunlight-Grade Contrast ($\ge 5.8:1$) on Deep-Green Train Card**: Use `#FFFFFF` for station codes/titles, `#B5DC86` (`> 7:1` lime) for berth/PNR highlights, and `#D7E8B6` (`> 5.8:1` soft sage) for secondary labels instead of muted grey.
   - **Refinement 4 — Canonical M3 `ModalBottomSheet` Anatomy on `TripSyncAndPerspectiveSheet.kt`**: `RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)`, `36.dp x 4.dp` drag handle, and `SplitMateTheme.ScreenBg` container.
   - **Refinement 5 — Progressive Disclosure on `>4` Berths + Tactile Perspective Haptic**: Show a 2x2 berth grid (up to 4 passengers, with `"YOU"` badge on the active perspective member) + `+N more` overflow chip, and fire `performCrispTactileHaptic(context)` when switching perspectives.

---

## 2. Zero-Emoji Material 3 Icon Mapping Table (`Icons.Rounded.*` Exclusively)

| Component / Section | Element in `TripHomeScreen.kt` | Exact Material 3 Icon (`Icons.Rounded.*`) | Icon Size |
| :--- | :--- | :--- | :--- |
| **Top App Bar** | Back Navigation Button | `Icons.AutoMirrored.Rounded.ArrowBack` | `22.dp` |
| **Top App Bar** | Sync & Perspective Switcher | `Icons.Rounded.Sync` / `Icons.Rounded.Share` | `20.dp` |
| **Top App Bar** | Classic Ledger View Toggle | `Icons.Rounded.ViewAgenda` | `22.dp` |
| **Primary Train Card** | Center Route Track Train Icon | `Icons.Rounded.Train` | `15.dp` |
| **Primary Train Card** | `"Berth Chart"` Button | `Icons.Rounded.AirlineSeatReclinerNormal` | `16.dp` |
| **Primary Train Card** | `"View E-Ticket"` Button | `Icons.Rounded.ConfirmationNumber` | `16.dp` |
| **Primary Flight Card** | Center Route Track Plane Icon | `Icons.Rounded.FlightTakeoff` | `15.dp` |
| **Lodging & Stays** | Section Right `"Confirmed Booking"` | `Icons.Rounded.CheckCircleOutline` | `14.dp` |
| **Lodging & Stays** | Property Leading Avatar | `Icons.Rounded.Apartment` | `22.dp` |
| **Lodging & Stays** | `"Maps"` / `"Front Desk"` / `"Voucher"` Chips | `Icons.Rounded.Map` / `Icons.Rounded.Call` / `Icons.Rounded.Description` | `15.dp` |
| **Ground Mobility (2W)** | Bike / Scooter Rental Leading Box | `Icons.Rounded.TwoWheeler` | `22.dp` |
| **Ground Mobility (Cab)**| Station Transfer / Cab Leading Box | `Icons.Rounded.DirectionsCar` | `22.dp` |
| **Return Transit** | Leading Train Icon Badge | `Icons.Rounded.Train` | `20.dp` |
| **Bottom Right FAB** | `"+ Add Booking"` Extended FAB | `Icons.Rounded.Add` | `20.dp` |

---

## 3. Deterministic Room-to-Card Classifier (100% Real Data Only)

1. **Real Room Entity Classification (`classifyGroupExpenseForTripHub`)**:
   - **Rule 1 (Train Pass)**: Matches 10-digit IRCTC PNR expenses -> renders the **Deep-Green Train Ticket Card (`UPCOMING DEPARTURE`)** for Leg 1 and the **Return Transit Card (`RETURN TRANSIT`)** for Leg 2+, hydrating passenger berth cells (2x2 grid up to 4 passengers + `+N more` overflow chip) from `PnrNetworkRepository` + `GroupMemberEntity`.
   - **Rule 2 (Flight Pass)**: Matches 6-char Flight PNR / airline expenses -> renders the **Aviation Periwinkle Flight Pass Card**.
   - **Rule 3 (Lodging & Stays)**: Matches `hotel|resort|stay|villa|airbnb|hostel|cottage|lodge|homestay|check-in|nights` -> renders the **Lodging & Stays Card** with real logged date (or parsed check-in/out notes if entered) and working `Maps` action chip.
   - **Rule 4 (Ground Mobility & Rentals)**: Matches `cab|taxi|uber|ola|rapido|auto|innova|transfer|rental|enfield|moped|scooter|bike` -> renders the **Ground Mobility Card** (Peach `TwoWheeler` badge for rentals, Periwinkle `DirectionsCar` badge for cabs/transfers).
   - **Rule 5 (General Shared Expense)**: Renders a clean Buckwheat shared expense card with tap-to-expand (`animateContentSize(tactileSpring())`) split breakdown + `Edit` / `Delete`.
2. **Clean Empty State**:
   - When `groupExpenses.isEmpty()`, renders a zero-placeholder Empty Trip State card prompting the user to tap `+ Add Booking`.
