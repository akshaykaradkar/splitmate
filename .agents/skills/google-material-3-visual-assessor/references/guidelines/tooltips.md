# Tooltips - Universal AI Detection Guide & MD3 Specifications

This reference provides universal visual heuristics for detecting Tooltips
(plain or rich contextual text popovers) across any design system or platform,
followed by Material Design 3 (MD3) specifications for compliance auditing.

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
        (typically 1.5s after navigating away).
    -   *Persistent (Rich)*: Triggered by click or page load (e.g., feature
        onboarding tour) and remains visible on screen until the user interacts
        with another element or clicks an internal action button.

--------------------------------------------------------------------------------

## Part 2: MD3 Specifications & Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected
tooltips against the following strict standards derived from Material
guidelines:

### MD3 Component Specifications & Sizing

1.  **Plain Tooltip Specifications**:
    -   **Dimensions & Padding**: Container height is strictly **24dp**.
        Internal padding is strictly **8dp** on all sides (`--md-sys-measurement-space100`).
    -   **Shape**: Uses small rounded corners (`--md-sys-shape-corner-extra-small` / `4px`).
    -   **Proximity Offsets**: Offset distance is **4dp** (`--md-sys-measurement-space50`) if the parent element has a visible bounding container (like a filled or outlined button); **8dp** (`--md-sys-measurement-space100`) if the parent lacks a visible boundary (like a plain text baseline).
    -   **Color Roles**: Uses high-contrast schemes to stand out against page content. Specifically uses `--md-sys-color-inverse-surface` (light-dark(#303030, #e3e3e3)) for the container background, paired with `--md-sys-color-inverse-on-surface` (light-dark(#f2f2f2, #303030)) for the text.
    -   **App Bar Behavior**: If the parent element is located inside a top app bar, the plain tooltip must appear **below** the element instead of above it, maintaining the same offset distance.

2.  **Rich Tooltip Specifications**:
    -   **Dimensions & Padding**: Top padding is strictly **12dp** (`--md-sys-measurement-space150`); Bottom padding is **8dp** (`--md-sys-measurement-space100`); Left and Right padding is **16dp** (`--md-sys-measurement-space200`).
    -   **Shape**: Features distinctly rounded corners, using `--md-sys-shape-corner-medium` (12px) or larger.
    -   **Typography**: Headline/Subhead titles utilize `Google Sans Text` and should be kept brief (ideally to one line).
    -   **Text Buttons**: Can contain up to two text buttons for quick context actions. Buttons should be short, brief, and placed side-by-side rather than stacked.
    -   **Placement Increments**: Positioned to the bottom-right of the parent element by default. Adjusts dynamically in **8dp** (`--md-sys-measurement-space100`) layout increments to avoid clipping off-screen. It must **never** cover or obscure the parent element it describes.
    -   **Restrictions**: Avoid using persistent rich tooltips on icon buttons.

3.  **Accessibility & Focus Rules**:
    -   *Focus Order*: Focus order within a rich tooltip moves linearly top-to-bottom between interactive elements (text buttons).
    -   *No Trapping*: Products must avoid trapping screen reader and keyboard focus inside rich tooltips; users must be able to move linearly through the rest of the page.

### Critical MD3 Violations to Flag

-   **Obscuring Parent Elements**: Positioning a rich or plain tooltip such that
    it covers or obscures the underlying parent UI element it is meant to
    describe.
-   **Focus Trapping in Rich Tooltips**: Trapping keyboard or screen reader
    focus inside a rich tooltip popover without allowing linear navigation to
    the underlying page.
-   **Improper Plain Tooltip Sizing & Padding**: Presenting a plain tooltip with
    excessive, uneven padding, or violating the compact 24dp height / 8dp padding specification.
-   **Too Many Buttons in Rich Tooltips**: Including more than two buttons inside a rich tooltip.
-   **Incorrect Color Mapping**: Using standard background and text roles instead of the mandatory high-contrast inverse roles (`--md-sys-color-inverse-surface` and `--md-sys-color-inverse-on-surface`) for plain tooltips.
