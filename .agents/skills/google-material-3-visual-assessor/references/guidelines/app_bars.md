# App Bars - Universal AI Detection Guide & GM3 Specifications

This reference provides universal visual heuristics for detecting App Bars (also known as top navigation bars, headers, or top bars) across any user interface, followed by Google Material 3 (GM3) specifications and design system tokens for compliance auditing.

## Part 1: Universal AI Detection Heuristics

An **App bar** is the primary structural container located at the top of a screen or pane, dedicated to branding, page context, navigation, and global actions. Visual models must detect app bars based on structural and geometric qualities, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Anchoring**: Always anchored at the extreme top of the screen (immediately below the OS status bar, if present) or at the extreme top of a distinct structural pane in multi-pane/split-view layouts. It spans 100% width of its parent screen or pane.
-   **Visual Boundaries & Containment**: App bars can be visually defined by a solid background color, a bottom divider line, a subtle elevation shadow, or be completely transparent.
    -   *Minimalist/Transparent Headers*: Even if an app bar lacks a distinct background color or border (blending seamlessly into the page background), the top layout region grouping the title, back button, or global actions MUST be bounded and classified as an `App bar`.
-   **Corner Geometry**: Features straight, flush corners meeting the screen or pane edges. Rounding of the outer container indicates an improper container or a floating sheet.
-   **Core Anatomy (Child Elements)**:
    -   *Leading Controls*: Navigation cues such as a Back arrow (`arrow_back`, `arrow_back_ios`), hamburger Menu (`menu`), or close button.
    -   *Title/Branding*: A text headline (page name, app title) or product logo/wordmark (left-aligned or centered).
    -   *Trailing Controls*: Action buttons (search, overflow, filter, share, settings) or a user profile avatar.

---

## Part 2: GM3 Specifications & Compliance Auditing

When conducting a Google Material 3 (GM3) adherence audit, evaluate the detected app bar against the following strict standards:

### GM3 Visual & Behavioral Rules

-   **Corner Shape**: Must use **straight corners** (`--md-sys-shape-corner-none` / `0` border radius). Any rounding of the outer container is a violation.
-   **Color Shift on Scroll**: In scrolled states, the app bar must transition from the background/surface color (`--md-sys-color-surface`, `light-dark(#ffffff, #131314)`) to the surface container color (`--md-sys-color-surface-container`, `light-dark(#f0f4f9, #1e1f20)`) to maintain visual separation from scrolling body content without using drop shadows.
-   **Action Limits (Trailing Controls)**: Standard app bars should contain a maximum of 3 trailing icon buttons. If many actions are needed, they should be placed in a toolbar instead.
-   **Headline Behavior**: Titles must never be truncated with ellipses or clipped. In compact Small app bars, the headline must fit on a single line. In Medium Flexible and Large Flexible variants, the headline can wrap to a maximum of 2 lines.
-   **Primary Action Emphasis**: For trailing actions, a single primary action can use a filled or tonal icon button style for high visibility. Multiple filled/tonal buttons are prohibited.

### GM3 App Bar Variants & Typography

| Variant | Typography Token / Style | Description & Typical Use Case |
| :--- | :--- | :--- |
| **Search app bar** | `--md-sys-typescale-body-large` or `--md-sys-typescale-variable-body-large`<br>(1rem/1.5rem 'Google Sans Text' / 'Google Sans Flex') | Home screens where query entry is primary; houses a search container using `--md-sys-color-surface-container` (or `--md-sys-color-surface-bright` on dark backgrounds) with hint text containing the word "Search". |
| **Small** | `--md-sys-typescale-title-large` or `--md-sys-typescale-variable-title-large`<br>(1.4rem/1.8rem 'Google Sans' / 'Google Sans Flex') | Default compact header for dense layouts or scrolled states. Standard back navigation utilizes `arrow_back` or `arrow_back_ios` (or iOS equivalents) rendered at 24x24dp inside a 48x48dp touch target. |
| **Medium flexible** | `--md-sys-typescale-headline-medium` or `--md-sys-typescale-variable-headline-medium`<br>(1.8rem/2.3rem 'Google Sans' / 'Google Sans Flex') | Taller header with expanded title supporting subtitles, multi-line wrapping, images, and centered text configurations; collapses to Small on scroll. |
| **Large flexible** | `--md-sys-typescale-display-small` or `--md-sys-typescale-variable-display-small`<br>(2.3rem/2.8rem 'Google Sans' / 'Google Sans Flex') | Most prominent header for top-level screens; supports flexible imagery and collapses to Small on scroll. |

### Critical GM3 Violations to Flag

-   **Deprecated Baseline Variants**: Baseline GM3 **Medium** and **Large** app bars are deprecated and must be replaced with **Medium flexible** and **Large flexible** variants (which feature reduced overall heights and multi-line support).
-   **Curved Corners**: Any rounding on the app bar container's outer corners.
-   **Text Truncation**: Ellipses (`...`) or clipping on the headline text instead of proper wrapping/scaling.
-   **Action Overcrowding**: More than 3 trailing actions cluttering the primary bar on mobile.
-   **Simultaneous Bottom App Bar and Bottom Navigation**: Bottom app bars and bottom navigation bars must never appear on the same screen.
