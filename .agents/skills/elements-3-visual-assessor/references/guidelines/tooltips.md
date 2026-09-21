# Tooltips - Elements GM3 AI Detection Guide & Specifications

This reference provides visual heuristics for detecting Tooltips (plain or rich contextual text popovers) across the Elements GM3 design system, followed by concrete specifications for compliance auditing.

## Part 1: Elements GM3 AI Detection Heuristics

A **Tooltip** is a compact, transient popover container that displays brief labels, descriptions, or detailed explanations providing additional context for a parent UI element (such as an icon button, link, or status badge). Agents must detect tooltips based on floating popover geometry, proximity anchoring, and transient behaviors, regardless of platform implementation.

### Key Visual & Geometric Heuristics

-   **Floating Popover Geometry & Proximity**: Look for small floating rectangular or rounded containers appearing in immediate proximity to a parent UI element (typically triggered upon hover, focus, or element selection).
-   **Variant Signatures**: Detect two primary structural lockups:
    -   **Plain Tooltip**: A small, compact rectangular container housing a single concise text string (e.g., identifying an icon's action or displaying truncated text). Positioned directly **above** the parent element by default (or **below** if the parent sits inside a top app bar).
    -   **Rich Tooltip**: A larger, distinctly rounded popover container housing multi-line supporting text, an optional title, and up to two optional action text buttons. Positioned to the **bottom-right** of the parent element by default.
-   **Transient vs. Persistent Behaviors**:
    -   *Transient (Plain & Default Rich)*: Appears on hover/focus and disappears automatically when the cursor or focus moves away.
    -   *Persistent (Wiz/ACX Rich)*: Triggered by a click or Enter key on a dedicated anchor (such as an 'i' info icon), remaining visible until the user presses Escape, clicks the anchor again, or clicks outside.

---

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected tooltips against the following strict standards:

### 1. Plain Tooltip Specifications

-   **Anatomy & Content**: Contains a brief, succinct text label inside a flat container. It must remain completely non-interactive (no buttons or links).
-   **Dimensions & Sizing**:
    -   *Height*: Minimum height is strictly **24dp**; maximum height is 50% of the viewport.
    -   *Width*: Minimum width is **40dp**; maximum width is **200dp**.
    -   *Corner Radius*: Strictly **4dp** corner radius.
    -   *Paddings*: **4dp** top and bottom padding; **8dp** left and right padding.
-   **Placement & Distance**:
    -   *Default Positioning*: Placed directly above the anchor element (or below if the anchor is in a top app bar).
    -   *Offsets*: **4dp** from defined boundaries if the anchor has a visual container (like a button); **8dp** if the anchor lacks a visual boundary (like text baselines).
-   **Color & Elevation**:
    -   *Container Color*: Inverse Surface (`var(--md-sys-color-inverse-surface)`).
    -   *Text Color*: Inverse On Surface (`var(--md-sys-color-inverse-on-surface)`).
    -   *Elevation*: Strictly **0** elevation (no shadows).

### 2. Rich Tooltip Specifications

-   **Anatomy & Content**:
    -   *Title (Optional)*: Utilizes `Google Sans Text` subheadings.
    -   *Body Text (Required)*: Inline hyperlinks can be optionally included.
    -   *Actions (Optional)*: A maximum of **2 action buttons**. Only plain text buttons are supported inside rich tooltips (no filled or tonal button variants allowed).
-   **Dimensions & Sizing**:
    -   *Width*: Fixed width is strictly **320dp**.
    -   *Height*: Minimum height is **24dp**; maximum is Auto-expanding based on content.
    -   *Corner Radius*: Strictly **12dp** (a critical update from GM2's 8dp).
    -   *Paddings*: **12dp** top padding; **8dp** bottom padding; **16dp** left and right padding.
-   **Placement & Offsets**:
    -   *Default Positioning*: Bottom-right of the anchor element.
    -   *Dynamic Alignment*: Adjusts dynamically in **8dp layout increments** to avoid clipping off-screen. It must **never** cover or obscure its parent anchor element.
-   **Color & Elevation**:
    -   *Container Color*: Surface Container Low (`var(--md-sys-color-surface-container-low)`).
    -   *Elevation*: Strictly **+2** elevation (shadow enabled for visual separation).

### 3. Interaction & Accessibility (GAR Compliance)

-   **Focusable Anchor Elements**: Any control or UI element (including badges, truncated text, or icons) that triggers a tooltip must be keyboard focusable so keyboard and screen reader users can access its content.
-   **Hoverable Content**: The open tooltip itself must be hoverable so magnification tool users can move their mouse over the popover without it disappearing.
-   **Close Actions**: Pressing the **Escape (Esc)** key must close any open tooltip immediately, regardless of its trigger mechanism.
-   **Focus order (Linear & No Trapping)**: Keyboard focus inside a rich tooltip must move linearly top-to-bottom and must never trap focus. Focus must exit seamlessly after the last interactive button.
-   **Alignment with Accessible Names**: For plain tooltips on icon buttons, the button's accessibility label (`aria-label`) and the tooltip text should match.

### Critical Elements GM3 Violations to Flag

-   **Obscuring Anchor Elements**: Placing a tooltip such that it covers or obscures the underlying UI element it describes.
-   **Excessive Buttons in Rich Tooltips**: Including more than 2 buttons inside a rich tooltip, or using filled/tonal button styles instead of plain text buttons.
-   **Improper Corner Radius**: Rendering rich tooltips with an 8dp corner radius (GM2) instead of the compliant **12dp** radius.
-   **Non-Focusable Anchors**: Anchoring tooltips to static elements (like standard text or badges) that cannot receive keyboard focus, preventing accessibility compliance.
-   **Missing Esc Key Close**: Failing to close a tooltip upon pressing the Escape key.
