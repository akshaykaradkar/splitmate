# Floating Action Buttons (FABs) - Universal AI Detection Guide \& M3 Specifications

This reference provides universal visual heuristics for detecting Floating Action Buttons, Extended FABs, and FAB Menus (prominent floating action containers) across any design system or platform, followed by Google Material 3 (M3) specifications and design token bindings for compliance auditing.

---

## Part 1: Universal AI Detection Heuristics

A **Floating Action Button (FAB)** is a prominent, high-priority interactive container designed to represent the primary or most common constructive action on a screen (e.g., Compose, Create, Add). Agents must detect FABs, Extended FABs, and FAB Menus based on their floating placement, distinctive geometry, and high-contrast visual layers, regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **Placement \& Floating Anchoring**: FAB components float above all other underlying page content, maintaining their position during page scroll.
    -   *Compact / Mobile Screens*: Consistently anchored in the lower trailing quadrant (bottom-right for LTR layouts, bottom-left for RTL layouts) or bottom-center of the viewport.
    -   *Expanded / Desktop Screens*: Placed in the bottom trailing corner OR integrated directly into the upper leading region of an expanded navigation rail.
-   **Variants \& Visual Signatures**: Look for three primary structural patterns:
    -   **Standard FAB (Circular / Rounded Square)**: A compact floating container displaying a single prominent icon.
    -   **Extended FAB (Pill / Rounded Rectangle)**: A wider, horizontally elongated floating container housing a clear icon followed by a concise text label (1-2 words). Tightly hugs its content.
    -   **FAB Menu (Action Stack)**: A vertical cluster of related secondary actions (2-6 items) sprouting from a base FAB. When open, the base FAB transforms into a distinct "Close" button (e.g., an "X" or chevron).
-   **Core Anatomy \& Touch Targets**:
    -   *Container*: A prominent bounding box featuring a high-contrast background fill and a distinct drop shadow (elevation).
    -   *Action Media*: A centered icon (Standard FAB) or leading icon plus text label (Extended FAB / Menu items).
-   **Adaptive Behavior**: Extended FABs frequently collapse into standard circular/rounded-square FABs on downward scroll to maximize viewing space, expanding back on upward scroll.
-   **FABs vs. Chips**: Extended FABs can visually resemble Chips. However, FABs are distinguished by their **floating behavior** (anchored above content, persistent on scroll) and **primary action** status. Standalone floating pill-shaped elements must be mapped to **`Extended FAB`**, never to `Chip`.

---

## Part 2: M3 Specifications \& Compliance Auditing

When conducting a Google Material 3 (M3) adherence audit, evaluate detected FAB components against the following strict standards and exact token bindings:

### M3 Sizing Tiers \& Sizing Specs

Google Material 3 establishes specific sizing tiers, geometries, and behavioral rules for FAB components:

#### 1. Standard FAB Specifications

-   **Tiers**: M3 defines three sizes (Small, Default FAB, and Large FAB):
    -   **Default FAB**: Dimensions are exactly **56x56dp** with a **16px** corner radius: `--md-sys-shape-corner-large` (`16px`). *Note: Standard M3 FABs use rounded square geometry with slightly flattened sides, not perfect circles.*
    -   **Large FAB**: Dimensions are exactly **96x96dp** with a **28px** corner radius: `--md-sys-shape-corner-extra-large` (`28px`).
    -   **Small FAB**: Dimensions are exactly **40x40dp** with an **8px** corner radius: `--md-sys-shape-corner-small` (`8px`). *Note: Under M3 Expressive update, the Small FAB is deprecated and no longer recommended.*
-   **Content**: Features exactly one filled icon (24dp, centered), never outline styles. No labels or notifications may be attached.

#### 2. Extended FAB Specifications

-   **Tiers**: Height tiers under the latest M3 Expressive specifications are:
    -   **Small Extended FAB**: Height is exactly **56dp** with a corner radius of **16px** (`--md-sys-shape-corner-large`) or fully rounded pill **28px** (`--md-sys-shape-corner-full`). *Replaces the legacy baseline extended FAB.*
    -   **Medium Extended FAB**: Height is exactly **80dp**.
    -   **Large Extended FAB**: Height is exactly **96dp**.
-   **Content**: Requires a clear text label in sentence case (using `--md-sys-typescale-title-medium` or `--md-sys-typescale-label-large` depending on size), and an optional but highly recommended leading filled icon.

#### 3. FAB Menu Specifications

-   **Structure**: Opens exclusively from a standard 56dp FAB (never from Extended FABs).
-   **Action Count**: Must contain between **2 to 6 related actions** floating vertically above.
-   **Close Button**: Upon opening, the base FAB morphs into a **56dp close button** displaying a clean "X" icon.
-   **Gap \& Placement**: A gap of exactly **4dp** (`--md-sys-measurement-space50` / 4px) is recommended between the close button and the lowest menu item. Items scroll behind the close button if height is limited.
-   **Item Specs**: Menu items share measurements with the M3 medium button and must contain both text labels and identifiers.

---

### M3 Color \& Elevation Tokens

-   **Color Styles**: M3 introduces specific color container options mapping directly to system tokens (Surface and branded styles are deprecated):
    -   **Primary Container (Default)**: Container uses `--md-sys-color-primary-container` (`light-dark(#d3e3fd, #0842a0)`); content/icon uses `--md-sys-color-on-primary-container` (`light-dark(#0842a0, #d3e3fd)`).
    -   **Secondary Container**: Container uses `--md-sys-color-secondary-container` (`light-dark(#c2e7ff, #004a77)`); content/icon uses `--md-sys-color-on-secondary-container` (`light-dark(#004a77, #c2e7ff)`).
    -   **Tertiary Container**: Container uses `--md-sys-color-tertiary-container` (`light-dark(#c4eed0, #0f5223)`); content/icon uses `--md-sys-color-on-tertiary-container` (`light-dark(#0f5223, #c4eed0)`).
    -   **Primary / Secondary / Tertiary Baseline**: Containers can also map to primary (`--md-sys-color-primary`), secondary (`--md-sys-color-secondary`), or tertiary (`--md-sys-color-tertiary`) solid colors.
-   **Contrast Constraint**: Icons and text labels must have at least a **3:1 contrast ratio** with their surrounding FAB container.
-   **Elevation \& Drop Shadows**: FABs float at high elevations to separate them from underlying scrolls:
    -   *Resting Elevation*: Level 3, `--md-sys-elevation-level3` (`6px`).
    -   *Hover / Focus Elevation*: Level 4, `--md-sys-elevation-level4` (`8px`).
-   **Interaction State Layer**: Ripple colors must map to the icon/text style (e.g., state layer on a Primary style FAB must resolve to `--md-sys-color-primary` / `--md-sys-color-on-primary` ripple).

---

### M3 Behavioral Rules

-   **Exclusivity**: Only **one** primary FAB or Extended FAB should be visible per screen. Competing FABs disrupt visual hierarchy and are a severe violation.
-   **Screen Margins**:
    -   *Compact/Medium breakpoints*: Bottom-trailing anchor with **16dp** screen edge margin (`--md-sys-measurement-space200`).
    -   *Expanded/Desktop breakpoints*: Top slot of an expanded navigation rail, or bottom-trailing with **24dp** screen edge margin (`--md-sys-measurement-space300`).
-   **Accessibility Focus**: Extended FAB container, icon, and label act as a single focusable target with no tooltip required. Standard FABs require a descriptive screen reader accessibility label and focused tooltips on web.
-   **Interactive Safety**: FABs must **never be disabled**. If the action is unavailable, the FAB must be hidden from the screen. It should also temporarily fade/disappear when shifting between tabs.

---

### Critical M3 Violations to Flag

-   **Multiple Floating Actions**: Displaying multiple FABs or Extended FABs simultaneously on a single screen.
-   **Incorrect Standard Shape**: Rendering standard FABs as legacy perfect circles rather than the standard M3 boxier rounded-square shape (16px corner radius for default, 28px for large).
-   **Deprecated Small FABs**: Utilizing legacy 40x40dp Small FAB stacks or outdated speed dial lists instead of the unified `FAB Menu`.
-   **Disabled FAB State**: Displaying a faded, disabled FAB, which violates the "active or hidden" behavioral guideline.
