# Tooltips - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Tooltips
(plain or rich contextual text popovers) across any design system or platform,
followed by Android Motion specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Tooltip** is a compact, transient popover container that displays brief
labels, descriptions, or detailed explanations providing additional context for
a parent UI element (such as an icon-only button or navigation item). Agents
must detect tooltips based on floating popover geometry, proximity anchoring,
and transient behaviors, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Floating Popover Geometry & Proximity**: Look for small floating
    rectangular or rounded containers appearing in immediate proximity to a
    parent UI element (typically triggered upon hover, focus, long-press, or
    element selection).
-   **Variant Signatures**: Detect two primary structural lockups:
    -   **Plain Tooltip**: A small, compact rectangular container housing a
        single concise text string (e.g., describing an icon's action).
        Positioned directly **above** the parent element by default (or below if
        the parent sits inside a top app bar).
    -   **Rich Tooltip**: A larger, distinctly rounded popover container housing
        multi-line supporting text, an optional bold subhead title, and up to
        two optional action text buttons. Positioned at the **bottom-right** of
        the parent element by default.
-   **Transient vs. Persistent Behaviors**:
    -   *Transient (Plain)*: Appears on hover/focus and disappears automatically
        (e.g., 1.5s after navigating away).
    -   *Persistent (Rich)*: Triggered by click or page load (e.g., feature
        onboarding tour) and remains visible on screen until the user interacts
        with another element or clicks an internal action button.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion adherence audit, evaluate detected
tooltips against the following strict standards derived from Android Motion
guidelines:

### Android Motion Component Specifications & Sizing

1.  **Plain Tooltip Specifications**:
    -   **Dimensions & Padding**: Container height is strictly **24dp**.
        Internal padding is **8dp** on all sides.
    -   **Proximity Offsets**: Offset distance is 4dp if the parent element has
        a visible bounding container (like a filled button); 8dp if the parent
        lacks a visible boundary (like a text baseline).
    -   **Shape & Elevation**: Uses `--droid-sys-shape-corner-extra-small` (4px) and sits at `--droid-sys-elevation-level0` or `--droid-sys-elevation-level1` (1px).
    -   **Color Roles**: Uses `--droid-sys-color-inverse-surface` (light: `#303030`, dark: `#e3e3e3`) container fill paired with `--droid-sys-color-inverse-on-surface` (light: `#f2f2f2`, dark: `#303030`) text for high contrast.
    -   **Typography**: Plain tooltip text is styled using `--droid-sys-typescale-body-small` (`0.8rem/1rem 'Google Sans Text'`).
2.  **Rich Tooltip Specifications**:
    -   **Dimensions & Padding**: Top padding 12dp; Bottom padding 8dp;
        Left/Right padding 16dp. Features distinctly rounded corners
        (`--droid-sys-shape-corner-medium` which is 12px, or larger).
    -   **Elevation**: Mapped to `--droid-sys-elevation-level2` (3px) or `--droid-sys-elevation-level3` (6px).
    -   **Color Roles**: Uses `--droid-sys-color-surface-variant` (light: `#e1e3e1`, dark: `#444746`) or `--droid-sys-color-surface` (light: `#fdfcfb`, dark: `#1f1f1f`) paired with `--droid-sys-color-on-surface` (light: `#1f1f1f`, dark: `#e3e3e3`) for readable body text and titles.
    -   **Typography**: Subhead titles utilize `--droid-sys-typescale-title-small` (`500 0.9rem/1.3rem 'Google Sans Text'`) or `--droid-sys-typescale-title-medium` (`500 1rem/1.5rem 'Google Sans Text'`). Supporting body text uses `--droid-sys-typescale-body-medium` (`0.9rem/1.3rem 'Google Sans Text'`). Action buttons use `--droid-sys-typescale-label-large` (`500 0.9rem/1.3rem 'Google Sans Text'`).
    -   **Placement Increments**: Adjusts dynamically in 8dp layout increments
        to avoid clipping off-screen. Must **never** cover or obscure the parent
        element it describes.
3.  **Motion & Transitions**:
    -   Entrance transition uses `--droid-sys-motion-duration-150` (150ms) for plain or `--droid-sys-motion-duration-200` (200ms) for rich tooltips, paired with the organic `--droid-sys-motion-easing-emphasized-decelerate` (`cubic-bezier(0.05, 0.7, 0.1, 1.0)`) curve.
    -   Exit transition uses `--droid-sys-motion-duration-100` (100ms) with `--droid-sys-motion-easing-emphasized-accelerate` (`cubic-bezier(0.3, 0.0, 0.8, 0.15)`).
4.  **Accessibility & Focus Rules**:
    -   *Focus Order*: Focus order within a rich tooltip moves linearly
        top-to-bottom between interactive elements (buttons).
    -   *No Trapping*: Products must avoid trapping screen reader and keyboard
        focus inside rich tooltips; users must be able to move linearly through
        the rest of the page.
    -   *Icon Font*: Any decorative or functional symbols in rich tooltips must use the official `--md-icon-font` ('Google Symbols').

### Critical Android Motion Violations to Flag

-   **Obscuring Parent Elements**: Positioning a rich or plain tooltip such that
    it covers or obscures the underlying parent UI element it is meant to
    describe.
-   **Focus Trapping in Rich Tooltips**: Trapping keyboard or screen reader
    focus inside a rich tooltip popover without allowing linear navigation to
    the underlying page.
-   **Improper Plain Tooltip Padding**: Presenting a plain tooltip with
    excessive or uneven padding violating the compact 24dp height / 8dp padding
    specification.
-   **Wrong Font Styling**: Using incorrect typography weight or face for tooltip body text instead of standard `--droid-sys-typescale-body-small` or `--droid-sys-typescale-body-medium`.
