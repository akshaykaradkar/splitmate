# Chips - Universal AI Detection Guide & MD3 Specifications

This reference provides universal visual heuristics for detecting Chips (compact
interactive elements used for entering information, filtering, or triggering
contextual actions) across any design system or platform, followed by Material
Design 3 (MD3) specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Chip** is a compact, dynamic UI element that allows users to enter
information, make selections, filter content, or trigger contextual actions.
Unlike static persistent buttons, chips appear dynamically in related groups.
Agents must detect chips based on their container geometry, grouped clustering,
and state markers, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Geometry & Visual Pattern**: Look for small, pill-like or rounded
    rectangular containers framing a concise text label, accompanied by optional
    leading or trailing icons.
-   **Group Behavior & Clustering**: Chips rarely appear alone; they are
    typically presented in cohesive horizontal rows or wrapped multi-line grids.
    Horizontal chip groups frequently support horizontal scrolling or reflow
    dynamically to fit container widths.
-   **State & Variant Signatures**:
    -   *Selection/Filtering*: A leading checkmark icon appearing inside the
        container indicates an active/selected state.
    -   *Deletion/Input*: A trailing "x" or remove icon indicates an input or
        removable chip.
    -   *Dropdown/Selection*: A trailing dropdown arrow icon (e.g.,
        chevron-down) indicates the chip opens a menu of options (e.g., date
        range or filter selectors).
-   **Core Anatomy & Touch Targets**:
    -   *Container*: The outer bounding shape enclosing the label and icons.
    -   *Label Text*: A brief, scannable text label.
    -   *Leading/Trailing Media*: Optional miniature icons, brand logos, or
        circular user avatars.
-   **Interactive DOM/Node Heuristics**: While the visual container is compact,
    the underlying interactive touch target is a larger 48px x 48px area.
    -   *Dual Actions*: If a chip features both a primary selection action and a
        trailing remove icon, agents scanning accessibility trees should note
        they act as two distinct focusable interactive elements.

-   **Chip vs. Button Distinction**: Focus on the functional purpose
    (filter/selection vs. action) and visual grouping:
    *   **Chip**: Used for filtering content, selecting options from a set, or
        representing input tags. They often appear in filter bars or tag groups,
        and typically have a smaller corner radius (e.g., 8px rounded
        rectangle). Standalone filter dropdowns (e.g., selecting a time range)
        function as filters and must be mapped to `Chip`, not to generic
        buttons.
    *   **Button**: Triggers a primary action (e.g., Submit, Save, Compose) or
        toggles a primary system/feature state. Buttons are typically standalone
        or in action groups, and are usually fully rounded (pill-shaped).

-   **Chip vs. FAB Distinction**: Do not classify a component as a `Chip` if it
    floats above content (persistent on scroll) and functions as a primary or
    contextual action trigger. Standalone floating elements (pill-shaped or
    rounded-rectangular) must be mapped to `Extended FAB` (or `FAB`), not
    `Chip`.

--------------------------------------------------------------------------------

## Part 2: MD3 Specifications & Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected
chips against the following strict standards derived from Material guidelines:

### MD3 Chip Variants & Semantics

Material Design 3 distinguishes four specific chip variants based on purpose and
authorship:

| Variant             | Purpose & Authorship     | Rationale & Typical Use Case |
| :------------------ | :----------------------- | :-------------------------- |
| **Assist Chip**     | Action, Product-authored | Triggers smart or automated actions related to the current context (e.g., "Add to calendar"). |
| **Filter Chip**     | Filter, Product-authored | Filters content in a collection. Supports single or multi-select; displays a leading checkmark when active. |
| **Input Chip**      | Information, User-authored | Represents discrete user-entered information (e.g., Gmail contacts in a "To" field). Features a trailing remove icon. |
| **Suggestion Chip** | Information, Product-authored | Narrows user intent by presenting dynamically generated suggestions (e.g., AI chat replies). |

### MD3 Tokens & Measurements

-   **Container Height**: Strictly 32px across all standard variants (`--md-sys-measurement-space400`).
-   **Corner Radius**: Standard chip containers feature an **8px corner radius** (`--md-sys-shape-corner-small`). *Exception*: People input chips are fully rounded (16px radius, i.e., `--md-sys-shape-corner-large` or pill shape `--md-sys-shape-corner-full`).
-   **Icon & Avatar Sizing**: Standard icons are 18px. Leading avatars in input chips are 24px with a 12px corner radius (`--md-sys-shape-corner-medium`).
-   **Internal Padding**:
    -   *Without Icon*: 16px padding on the left and right (`--md-sys-measurement-space200`).
    -   *With Icon*: 8px padding on the icon side (`--md-sys-measurement-space100`).
    -   *Element Spacing*: 8px padding between internal elements (`--md-sys-measurement-space100`, e.g., avatar and text).
-   **External Spacing**: Chip sets must maintain a minimum of 8px spacing between adjacent chips (`--md-sys-measurement-space100`).
-   **Touch Target & Density**: Minimum 48px x 48px touch target (`--md-sys-measurement-space600`). Products must not apply high density by default, which would shrink touch targets below 48px.
-   **Color & Stroke**: Chip elevation defaults to `--md-sys-elevation-level0` (flat). Container stroke uses the `outline-variant` (`--md-sys-color-outline-variant`) color role by default to soften the borders, but can use `outline` (`--md-sys-color-outline`) to ensure a minimum 3:1 contrast for accessibility/visibility. Label text must maintain at least a 3:1 contrast ratio with the background.

### Critical MD3 Violations to Flag

-   **Conflating Chips with Buttons**: Using chips as persistent fixtures for
    linear workflow steps (where a `Button` is required), or placing more than 3
    standard buttons in a cluster meant for dynamic chips.
-   **Label Text Overflow**: Exceeding 20 characters for a chip label, violating
    Material brevity guidelines.
-   **Default High Density**: Forcing dense chip layouts by default that shrink
    interactive touch targets below 48px x 48px (`--md-sys-measurement-space600`) without providing an accessible user
    override.
