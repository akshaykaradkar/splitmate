---
name: splitmate-m3-expressive-assessor-and-builder
description: >-
  Authoritative skill for designing, implementing, and auditing SplitMate's UI using
  Google Material 3 Expressive (GM3), Android Motion spring physics, and Elements GM3
  financial semantic tokens across Jetpack Compose and Web/CSS surfaces.
---

# SplitMate Material 3 Expressive & Android Motion Skill

Use this skill whenever building or auditing UI screens, Jetpack Compose components (`Color.kt`, `Theme.kt`, `Type.kt`, `Shape.kt`), or web previews for **SplitMate**. It unifies the **Google Material 3 Visual Assessor**, **Android Motion Visual Assessor**, and **Elements GM3** design specifications.

> [!IMPORTANT]
> **MANDATORY REFERENCE FILES:** Before generating or auditing any UI component, consult:
> - [DESIGN.md](file:///usr/local/google/home/karadkar/splitmate/DESIGN.md): Complete HCT color tokens, 5-tier Surface Container scale, typography, optical shape nesting, and motion physics.
> - [design-system-bindings.css](file:///usr/local/google/home/karadkar/splitmate/design-system-bindings.css): Canonical `--md-sys-*`, `--droid-sys-*`, and `--cee3-sys-*` token bindings.
> - [BRD.md](file:///usr/local/google/home/karadkar/splitmate/BRD.md): Mathematical & functional specifications for the Remainder Engine, Locked Proportional Tax/Tip Multiplier, and Greedy Minimum Cash Flow algorithm.

---

## Step 1: Mandatory Design & Engineering Rules (Generation Phase)

When writing Jetpack Compose (`androidx.compose.material3`) or HTML/CSS UI code for SplitMate, enforce every rule below:

### 1. Color & Tonal Surface Hierarchy (GM3 + Elements GM3)
* **Never use hard black drop shadows or legacy GM2 elevation overlays.** Use the 5-level tonal surface container progression:
  * Screen Background Canvas: `MaterialTheme.colorScheme.surface` (`#fdfcfb` light / `#131314` dark)
  * Expense & Member Split Cards: `MaterialTheme.colorScheme.surfaceContainer` (`#f0f4f9` light / `#1e1f20` dark)
  * Hero Net Balance & Active Persona Bar: `MaterialTheme.colorScheme.surfaceContainerHigh` (`#e9eef6` light / `#282a2c` dark)
  * Floating NumPad Toolbar (`HorizontalFloatingToolbar`): `MaterialTheme.colorScheme.surfaceContainerHighest` (`#dde3ea` light / `#333537` dark)
* **Semantic Financial Tokens (Elements GM3):**
  * **100% Balanced / Creditor State:** `extended-success-balanced-container` (`#beefbb` light / `#00522c` dark) with `extended-on-success-balanced-container` (`#00522c` light / `#beefbb` dark).
  * **Unassigned Remainder State ($R_{\text{base}} > 0$, held on Payer):** `extended-warning-remainder-container` (`#ffe07c` light / `#6d3a01` dark) with `extended-on-warning-remainder-container` (`#6d3a01` light / `#ffe07c` dark).
  * **Over-Claimed State ($\sum b_i > B$):** `errorContainer` (`#f9dedc` light / `#8c1d18` dark) with `onErrorContainer` (`#8c1d18` light / `#f9dedc` dark).

### 2. Typography & Tabular Numerals (`Google Sans` + `Google Sans Text`)
* **Display & Headline (`Google Sans`):** Use `displayMedium` / `headlineLarge` for hero net balances and total receipt readouts. Never truncate page titles.
* **Body & Labels (`Google Sans Text`):** Use sentence case for all buttons and chips (never `ALL CAPS`).
* **Tabular Numerals (`tnum`):** Every monetary value, Remainder Engine readout, percentage chip, and NumPad display MUST specify `fontFeatureSettings = "tnum"` (in Compose `TextStyle`) or `font-variant-numeric: tabular-nums` (in CSS) so digits remain optically aligned during live typing.

### 3. Expressive Shapes & Optical Nesting
* **Shape Scale:**
  * Resting Buttons, Persona Chips, Quick-Add Chips (`+$1`, `+$5`, `+$10`, `+$20`): `RoundedCornerShape(percent = 50)` (`full` pill).
  * Pressed / Selected Morph State for Buttons & Chips: `RoundedCornerShape(16.dp)` (`large`).
  * Expense & Member Split Matrix Cards: `RoundedCornerShape(20.dp)` (`large-increased`).
  * Hero Summary Cards, Modal Bottom Sheets, `HorizontalFloatingToolbar`: `RoundedCornerShape(28.dp)` (`extra-large`).
* **Optical Roundness Formula:** Nested containers inside a card with padding $P$ must use `InnerRadius = max(8.dp, OuterRadius - P)`.

### 4. Android Motion & Spring Physics
* **Spring Equations Only:** Use `spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)` for spatial transitions, progress bar segment fills, and row expansions.
* **Tactile Press Morphing:** Animate button/key corner radius from `50%` pill to `16.dp` and scale to `0.96f` using `animateDpAsState` / `animateFloatAsState` with `Spring.StiffnessHigh`.
* **No Direct Font-Size Animation:** Scale text containers via `Modifier.graphicsLayer` transforms rather than animating `fontSize` frame-by-frame.

### 5. Universal Accessibility & Touch Targets (GAR 2024)
* **Strict `48dp × 48dp` Minimum Touch Targets:** Every interactive button, icon, persona chip, member row, and NumPad key must enforce `Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)`.
* **Outlined Text Fields Only:** Use `OutlinedTextField` (`56.dp` height, `12.dp` corner radius) rather than filled text fields, with inline error replacement text so layouts never shift vertically on validation errors.
* **Single High-Emphasis Primary Action:** Only one `Button` (Filled Primary) per screen; secondary actions (`Split Remainder Equally`, `Add Shadow Guest`) use `FilledTonalButton` or `OutlinedButton`.

---

## Step 2: Two-Pass Visual & Codebase Compliance Audit Workflow

When inspecting a rendered UI or reviewing Compose/CSS code, execute both passes below:

### Pass 1: Foundational Screen-Level Checklist
1. **Color & Contrast:** Do all text/background pairings pass WCAG AA (`4.5:1` for normal text, `3:1` for large text >= 18pt or 16pt medium Google Sans)? Are surface containers used instead of heavy black drop shadows?
2. **Typography & Alignment:** Are all monetary values using tabular numerals (`tnum`)? Do line heights sit on the `4dp` baseline grid? Are all button/chip labels in sentence case?
3. **Layout & Touch Targets:** Does the mobile layout adhere to a clean 1-column stack with `16dp` outer margins, `16dp` card inset padding, and `>= 48dp × 48dp` interactive hit boxes separated by at least `8dp`?
4. **Android Motion:** Are state transitions (Remainder progress bar, Persona selection, NumPad press) driven by spring physics and container/corner morphing rather than abrupt jumps or linear curves?

### Pass 2: SplitMate Component-Specific Rubric
1. **In-App Persona Switcher Pill Bar:** Clearly highlights the active claiming persona (`primaryContainer` `16dp` morphed pill) and displays each member's avatar and claimed base status.
2. **Remainder Engine Progress Indicator:** Displays proportional multi-member colored segments plus a distinct Amber (`extended-warning-remainder`) segment when $R_{\text{base}} > 0$ (indicating the remainder is temporarily held on the Payer) and transitions smoothly to Emerald (`extended-success-balanced`) when $R_{\text{base}} = \$0.00$.
3. **`HorizontalFloatingToolbar` NumPad & Quick Chips:** Floating `28dp` container at `surfaceContainerHighest` with `+$1`, `+$5`, `+$10`, `+$20`, `Split Remainder Equally`, and `48dp+` tactile NumPad keys showing real-time `Base + Locked Tax/Tip = Total Owed`.
