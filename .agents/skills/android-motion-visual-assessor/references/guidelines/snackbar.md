# Snackbars - Universal AI Detection Guide \& Android Motion Specifications

This reference provides universal visual heuristics for detecting Snackbars
(temporary bottom-anchored process notifications) across any design system or
platform, followed by Android Motion specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Snackbar** is a concise, temporary messaging container displayed at the
bottom of a screen to inform users of a process the app has performed or will
perform (e.g., "Message sent", "Item archived"). Unlike modal dialogs, snackbars
are non-blocking and do not interrupt the user's ongoing experience. Agents must
detect snackbars based on bottom anchoring, non-modal layering, and temporal
dismissal, regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **Bottom Anchoring \& Floating/Flush Layer**: Look for a distinct horizontal
    container positioned at the extreme bottom edge of the viewport. Snackbars
    typically float directly in front of main page content (above bottom
    nav/FABs) but on web or certain desktop views, they can also appear as a
    full-width banner flush with the bottom and side edges. Despite lacking
    rounded corners or floating margins, these bottom-flush dismissable banners
    function as snackbars and MUST be mapped to **`Snackbar`**.
-   **Layout Anatomy \& Action Lockup**: Defined by a solid rectangular bounding
    box housing a single row lockup: a start-aligned text description on the
    left, paired with an optional single text button on the far right (e.g.,
    "Undo", "Retry").
-   **Non-Modal Containment**: Snackbars cover only a small, localized portion
    of the bottom UI. They obligatorily lack a background dimming scrim overlay,
    allowing users to freely browse and interact with underlying page content
    while the snackbar remains visible.
-   **Temporal Auto-Dismissal**: Agents analyzing multi-state UI flows should
    note that snackbars appear suddenly and frequently auto-dismiss after a
    short duration (e.g., 4 to 10 seconds) if no user action is taken.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications \& Compliance Auditing

When conducting an Android Motion adherence audit, evaluate detected
snackbars against the following strict standards derived from Android Motion
guidelines:

### Android Motion Component Specifications \& Sizing

-   **Quantity \& Frequency Limits**: Strictly **one** snackbar may be displayed
    on screen at a time. Stacking multiple snackbars simultaneously is a severe
    layout violation.
-   **Dimensions \& Padding**:
    -   *Compact Screens (Mobile)*: Spans full width (minus standard 8dp or 16dp outer side margins). Height is 48dp for 1-line text strings, expanding to 64dp for 2-line text strings.
    -   *Medium / Expanded Screens (Tablet/Desktop)*: Bounded/fixed container width, positioned as a floating box either centered or left-aligned at the bottom of the screen.
-   **Shape \& Elevation**:
    -   Uses a subtly rounded container with `--droid-sys-shape-corner-extra-small` (4px).
    -   Elevated above standard content, mapped to `--droid-sys-elevation-level3` (6px) or `--droid-sys-elevation-level2` (3px) depending on the surface structure.
-   **Color \& Theming**:
    -   Uses a high-contrast background fill of `--droid-sys-color-inverse-surface` (light: `#303030`, dark: `#e3e3e3`) to stand out against standard canvas backgrounds.
    -   Body text is styled with `--droid-sys-color-inverse-on-surface` (light: `#f2f2f2`, dark: `#303030`).
    -   The single action button utilizes the high-contrast accent color `--droid-sys-color-inverse-primary` (light: `#a8c7fa`, dark: `#0b57d0`) to ensure legibility and prominence.
-   **Typography**:
    -   Body text is styled using `--droid-sys-typescale-body-medium` (`0.9rem/1.3rem 'Google Sans Text'`).
    -   Action button text is styled using `--droid-sys-typescale-label-large` (`500 0.9rem/1.3rem 'Google Sans Text'`).
-   **Action Button Restrictions**: A snackbar may contain a maximum of **one** action button, which must be formatted as a `Text button`. Inserting icon buttons, filled buttons, or multiple competing action buttons is strictly prohibited. "Dismiss" or "Cancel" actions are optional but recommended for accessibility persistence.
-   **Motion \& Animation**:
    -   Snackbar entrance should use a duration of `--droid-sys-motion-duration-150` (150ms) to `--droid-sys-motion-duration-250` (250ms) with `--droid-sys-motion-easing-emphasized-decelerate` (`cubic-bezier(0.05, 0.7, 0.1, 1.0)`) sliding up and fading in.
    -   Snackbar dismissal should use a duration of `--droid-sys-motion-duration-100` (100ms) with `--droid-sys-motion-easing-emphasized-accelerate` (`cubic-bezier(0.3, 0.0, 0.8, 0.15)`) fading out.

### Android Motion Component Messaging Hierarchy

Products must select the correct messaging component based on urgency and
interruption requirements:

-   **Snackbar**: Low priority, temporary, non-modal, bottom-anchored. Does not require user action.
-   **Banner**: Medium priority, persistent inline banner, usually anchored near the top of the screen below the app bar. Remains on screen until explicitly dismissed.
-   **Dialog**: High priority, modal overlay (blocks underlying page interaction with a dark scrim), centered prominently.

### Critical Android Motion Violations to Flag

-   **Simultaneous Snackbar Stacking**: Displaying two or more snackbar containers on screen at the same time.
-   **Prohibited Button Types**: Inserting an `Icon button` or an `Elevated/Filled button` inside a snackbar container instead of the required `Text button`.
-   **Exceeding Text Limits**: Overcrowding a snackbar with 3 or more lines of text, violating mobile brevity constraints.
-   **Incorrect Color Pairing**: Rendering snackbars using standard `--droid-sys-color-surface` instead of the required high-contrast `--droid-sys-color-inverse-surface` container fill.
