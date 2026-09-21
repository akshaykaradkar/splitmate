# Side Sheets - Universal AI Detection Guide & GM3 Specifications

This reference provides universal visual heuristics for detecting Side Sheets
(vertical auxiliary panels anchored to the trailing or leading screen edge)
across any design system or platform, followed by Google Material 3 (GM3)
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
    -   *Trailing Edge (Default)*: Anchored to the far right edge in LTR layouts (or far left edge in RTL layouts).
    -   *Leading Edge*: Auxiliary panels anchored to the far left edge (e.g., supplementary navigation or tool panels).
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

## Part 2: GM3 Specifications & Compliance Auditing

When conducting a Google Material 3 (GM3) adherence audit, evaluate detected
side sheets against the following strict standards derived from Material
guidelines:

### GM3 Component Variants & Sizing

Material Design 3 establishes specific sizing tokens and behavioral rules for
side sheets:

1.  **Standard Side Sheet Specifications**:
    -   **Usage**: Intended primarily for medium to expanded window sizes
        (tablets, desktops). Co-exists coplanarly with the primary UI, allowing
        users to view and interact with both surfaces simultaneously. When
        opened, the primary body area compresses dynamically to accommodate the
        sheet's width.
    -   **Measurements & Tokens**: 
        -   Max-width: Strictly **400dp** (or custom-sized based on layout grids).
        -   Start/end padding: 24dp.
        -   Padding between top header elements: 12dp.
        -   Bottom action bar height: 72dp (with 16dp top padding and 24dp bottom padding).
        -   Bottom actions alignment: Left-aligned.
        -   Container Fill: Uses surface-derived container colors (e.g., `--md-sys-color-surface` or `--md-sys-color-surface-container`).
2.  **Modal Side Sheet Specifications**:
    -   **Usage**: Intended primarily for compact window sizes (mobile screens)
        where screen space is limited. Appears in front of app content,
        disabling underlying page interaction with a dark `scrim` overlay.
    -   **Measurements & Tokens**: 
        -   Width and padding: Matches standard side sheets (400dp max-width, 24dp start/end padding).
        -   Start padding with icon: 16dp.
        -   Corner Radius: Features rounded corners with **16dp** radius on the side facing the content, utilizing the `--md-sys-shape-corner-large` design token (standard side sheets are typically flat).
        -   Container Fill & Shadow: Uses `--md-sys-color-surface-container-low` container fill with elevation level 1 shadow (`--md-sys-elevation-level1`) over the `--md-sys-color-scrim` overlay.
3.  **Anatomy & Dismissal Rules**:
    -   The container is the only required element; headers, actions, and dividers are optional.
    -   **Close Affordance**: A close icon button ("X" or back arrow) is highly recommended and required to guarantee accessible keyboard and screen reader dismissal.
    -   **Dividers**: Optional 1dp dividers (using `--md-sys-color-outline-variant`) should separate pinned bottom action bars from scrolling content.

### GM3 Behavioral & Exclusivity Rules

-   **Independent Vertical Scrolling**: Side sheets must support vertical
    scrolling completely independent of the primary UI body. They must **never**
    scroll horizontally.
-   **RTL Mirroring**: In right-to-left languages, side sheets must mirror
    completely to anchor to the left edge of the screen with all child elements
    reversed.
-   **Predictive Back (Android)**: On Android, a swipe gesture detaches the side sheet from the top and bottom edges of the screen to signal closing, revealing a preview of the previous screen.

### Critical GM3 Violations to Flag

-   **Missing Modal Scrim on Mobile**: Presenting a side sheet on a compact
    mobile screen without a background scrim overlay (`--md-sys-color-scrim`), failing to focus user attention or disable background UI.
-   **Horizontal Scrolling**: Introducing horizontal scrollbars inside a side
    sheet container, violating Material layout constraints.
-   **Missing Parent Container Mapping**: Conflating a side sheet container with
    a generic layout or failing to classify the outer `Side sheet` bounding box during an audit.
-   **Missing Close Icon Button**: Presenting a side sheet without a visible close icon button ("X" or back arrow), violating crucial accessibility requirements.
