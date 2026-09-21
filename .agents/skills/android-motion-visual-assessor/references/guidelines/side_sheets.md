# Side Sheets - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Side Sheets
(vertical auxiliary panels anchored to the trailing or leading screen edge)
across any design system or platform, followed by Android Motion design system
specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Side sheet** is a persistent or modal vertical containment panel anchored to
the side edge of a screen or window. They are designed to display supplementary
content, filters, or contextual actions alongside primary page content. Agents
must detect side sheets based on their vertical orientation, side edge
anchoring, and independent scrolling containers, regardless of design system
adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Side Edge Anchoring**: Side sheets are consistently anchored
    flush against the vertical side boundary of a viewport:
    -   *Trailing Edge (Default)*: Anchored to the far right edge in
        left-to-right (LTR) layouts (or far left edge in RTL layouts).
    -   *Leading Edge*: Auxiliary panels anchored to the far left edge (e.g.,
        supplementary navigation or tool panels).
-   **Visual Boundaries & Containment**: Defined by a distinct vertical
    container spanning the full height (or near full height) of the screen. They
    are visually separated from the primary page body by a solid background
    fill, a vertical dividing border stroke, or an elevation drop shadow.
    -   *Modal Scrim*: If the side sheet is modal, the remaining primary page
        content is covered by a darkened, dimming overlay (`scrim`).
-   **Core Anatomy & Layout Hierarchy**: Side sheets maintain an organized
    vertical structure:
    -   *Header Lockup*: A top bar containing a bold title and an optional close
        affordance (e.g., an "X" or back arrow).
    -   *Scrollable Content Area*: The primary container area housing filters,
        form fields, lists, or supplementary tabular data. Supports vertical
        scrolling independent of the main page body.
    -   *Bottom Action Bar (Optional)*: Pinned action buttons (e.g., "Save",
        "Apply", "Clear") positioned at the bottom of the sheet.
-   **Mandatory Container Mapping**: Agents MUST bound and classify the outer
    vertical container as `Side sheet`. Agents must not merely map the internal
    child widgets (such as checkboxes, buttons, or text fields) while omitting
    the parent sheet container.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion design system adherence audit, evaluate detected
side sheets against the following strict standards derived from Android Motion
guidelines:

### Android Motion Component Variants & Sizing

Android Motion establishes specific sizing tokens, shape mappings, and behavioral rules for
side sheets:

1.  **Standard Side Sheet Specifications**:
    -   **Usage**: Intended primarily for medium to expanded window sizes
        (tablets, desktops). Co-exists coplanarly with the primary UI, allowing
        users to view and interact with both surfaces simultaneously. When
        opened, the primary body area compresses dynamically to accommodate the
        sheet's width.
    -   **Measurements & Tokens**: Fixed width (default 400dp or custom-sized
        based on layout grids). Start/end padding is 24dp; 12dp padding between
        top header elements. Bottom action bar height is 72dp (16dp top padding,
        24dp bottom padding). Uses surface-derived container colors like `--droid-sys-color-surface` (light-dark(`#fdfcfb`, `#1f1f1f`)) or `--droid-sys-color-surface-variant` (light-dark(`#e1e3e1`, `#444746`)).
2.  **Modal Side Sheet Specifications**:
    -   **Usage**: Intended primarily for compact window sizes (mobile screens)
        where screen space is limited. Appears in front of app content,
        disabling underlying page interaction with a dark `scrim` overlay.
    -   **Measurements & Tokens**: Features identical width and padding tokens
        to standard side sheets (24dp outer padding, 16dp start padding with
        icon). Uses `--droid-sys-color-surface` container fill featuring an elevation shadow
        `--droid-sys-elevation-level1` (1px) or `--droid-sys-elevation-level2` (3px) over the modal scrim.
3.  **Shape & Corner Treatment**:
    -   In standard side sheets anchored to the trailing edge, the leading (left) corners may feature rounded shapes such as `--droid-sys-shape-corner-extra-large` (`28px`), while the trailing (right) corners remain sharp (`--droid-sys-shape-corner-none` / `0`) where they touch the outer screen edges.
4.  **Anatomy & Dismissal Rules**:
    -   The container is the only required element; headers, actions, and
        dividers are optional.
    -   **Close Affordance**: A close icon button ("X" or back arrow) utilizing `'Google Symbols'` (`--md-icon-font`) is highly
        recommended to ensure accessible keyboard and screen reader dismissal
        affordances.
    -   **Dividers**: Optional 1px dividers should separate pinned bottom action
        bars from scrolling content, styled with `--droid-sys-color-outline` (light-dark(`#747775`, `#8e918f`)).
5.  **Motion & Fluid Transitions**:
    -   The entrance and exit transitions of a side sheet must utilize standard motion tokens to feel highly responsive. Sliding in/out should map to `--droid-sys-motion-duration-300` (300ms) or `--droid-sys-motion-duration-350` (350ms) for modal sheets, and `--droid-sys-motion-duration-250` (250ms) or `--droid-sys-motion-duration-300` (300ms) for standard sheets, always driven by the premium `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) easing curve.

### Android Motion Behavioral & Exclusivity Rules

-   **Independent Vertical Scrolling**: Side sheets must support vertical
    scrolling completely independent of the primary UI body. They must **never**
    scroll horizontally.
-   **RTL Mirroring**: In right-to-left languages, side sheets must mirror
    completely to anchor to the left edge of the screen with all child elements
    reversed.

### Critical Android Motion Violations to Flag

-   **Missing Modal Scrim on Mobile**: Presenting a side sheet on a compact
    mobile screen without a background scrim overlay, failing to focus user
    attention or disable background UI.
-   **Horizontal Scrolling**: Introducing horizontal scrollbars inside a side
    sheet container, violating Android Motion layout constraints.
-   **Missing Parent Container Mapping**: Conflating a side sheet container with
    a generic `Custom layout` or failing to classify the outer `Side sheet`
    bounding box during an audit.
-   **Improper Easing Curve**: Using basic linear transitions or lack of easing specifications, violating the standard `--droid-sys-motion-easing-emphasized` curve guidelines.
