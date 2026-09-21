# Side Sheets - Universal AI Detection Guide \& Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Side Sheets (vertical auxiliary panels anchored to the screen edge) across any design system or platform, followed by CE Elements GM3 specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Side sheet** is a persistent or modal vertical containment panel anchored to the side edge of a screen or window. They are designed to display supplementary content, filters, or contextual actions alongside primary page content. Agents must detect side sheets based on their vertical orientation, side edge anchoring, and independent scrolling containers, regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **Placement \& Side Edge Anchoring**: Side sheets are consistently anchored flush against the vertical side boundary of a viewport (typically the trailing edge, which is the far right edge in LTR layouts).
-   **Visual Boundaries \& Containment**: Defined by a distinct vertical container spanning the full height (or near full height) of the screen. They are visually separated from the primary page body by a solid background fill, a vertical dividing border stroke, or an elevation shadow.
-   **Core Anatomy \& Layout Hierarchy**: Side sheets maintain an organized vertical structure:
    -   *Header Lockup*: A top bar containing a bold title and a required close affordance ("X" icon button) on the top-right corner.
    -   *Scrollable Content Area*: Supports vertical scrolling independent of the main page body.
    -   *Bottom Action Bar / Footer*: Pinned action buttons positioned at the bottom of the sheet.
-   **Mandatory Container Mapping**: Agents MUST bound and classify the outer vertical container as `Side sheet`. Agents must not merely map the internal child widgets while omitting the parent sheet container.

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications \& Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected side sheets against the following strict standards:

### Elements GM3 Component Variants \& Sizing

CE Elements GM3 defines two side sheet use cases with different behaviors and styling rules:

#### 1. Standard Side Sheets

-   **Usage \& Placement**: Complements primary content without blocking it. Co-exists coplanarly; the main body resizes or shifts when the side sheet is opened.
-   **Measurements \& Spacing**:
    -   *Width Sizes*: Default recommended width is **360dp**. A secondary width of **480dp** is permitted for complex content.
    -   *Margins*: Keep a minimum of **24dp right margin** and a **24dp margin** between the main page body and the side sheet.
    -   *Height*: Extends to the full height of the main content area.
    -   *Header Padding*: 24dp left/right padding for 1st-level pages. For 2nd-level pages, left padding reduces to **16dp** to accommodate a top-left back arrow button.
    -   *Compact Screen Behavior*: On viewports below **640dp**, standard side sheets expand to 100% width, and padding within the container reduces to **16dp**.
-   **Colors \& Theming**:
    -   Supports two color options in light/dark themes to be used in accordance with the UI background color (mapped to standard surface or surface-container tokens).
-   **Save/Cancel \& Dismissal Rules**:
    -   **No Save or Cancel buttons are permitted in the footer**. Edits made in standard side sheets must automatically save and update the main body dynamically.
    -   Close button ("X" icon) is required in the top-right corner to manually collapse the sheet.
    -   Esc key is **not recommended** for dismissing standard side sheets to prevent accidental closing.

#### 2. Modal Side Sheets

-   **Usage \& Placement**: Session-based panels used for sub-tasks or complex configurations. Overlays the primary UI and disables background interaction.
-   **Measurements \& Spacing**:
    -   *Width Size*: Has exactly **one fixed width size of 720dp**.
    -   *Height*: Spans the full height of the screen.
    -   *Compact Screen Behavior*: Expands to take over the full page for screen sizes below **720dp**.
-   **Colors \& Theming**:
    -   Consistently uses **`surface_container_low`** (using `--md-sys-color-surface-container-low`) for the container fill regardless of the background color.
    -   Uses a dark gray scrim overlay over the primary UI: **`add_on_surface_colors / scrim (32%)`**.
    -   Headline, subtitle, and icon buttons use **`on_surface_variant`** (`--md-sys-color-on-surface-variant`).
-   **Save/Cancel \& Dismissal Rules**:
    -   **A Save button is required in the footer** if there are editing actions. Edits are only saved and synced when the user clicks "Save" (which also dismisses the sheet).
    -   A Cancel button is optional in the footer. If clicked (or if the close button "X" is clicked after taking action), a confirmation dialog must appear before closing without saving.
    -   Keyboard focus is trapped inside the modal side sheet. Esc key is allowed to close it.

---

### Anatomy \& Typography Specifications (Both Variants)

-   **Header**: Required for all side sheets.
    -   **Title Typography**: Main title uses **`Title Medium`** (`--cee3-typescale-title-medium-link` or standard baseline Title Medium: `500 1rem 'Google Sans Text'`), updated from legacy Title Large.
    -   **Icon Buttons**: Standard size for header icon buttons is **40dp** (reduced from 48dp).
-   **Header Actions (Optional)**: Can contain **Tabs** (requires a 1dp bottom divider line) or **Filter chips** directly beneath the header.
-   **Content Spacing**:
    -   Vertically stacked content elements use **16dp** or **24dp** spacing between items (**12dp** or **16dp** on compact screens).
    -   To separate distinct vertical sections, use **48dp** spacing.
-   **A11y Labels**: Close button must have a contextual accessibility label like "close {side sheet title}".

### Critical Elements GM3 Violations to Flag

1.  **Bottom Save/Cancel Buttons on Standard Side Sheets**: Placing "Save" or "Cancel" actions in a standard side sheet instead of auto-saving edits, violating Elements GM3 interaction patterns.
2.  **Incorrect Width Sizing**: Creating a standard side sheet with a non-standard width (not 360dp or 480dp), or a modal side sheet not sized to 720dp.
3.  **Missing Close Affordance**: Omitting the required top-right close icon button ("X"), which harms keyboard and screen reader accessibility.
4.  **Incorrect Typography**: Using `Title Large` or other legacy headers instead of the required `Title Medium` for side sheet headers.
5.  **Missing Scrim on Modal Side Sheet**: Failing to overlay a 32% dark scrim when displaying a modal side sheet, allowing background page elements to remain interactive.
