# Chips - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Chips (compact
interactive elements used for entering information, filtering, or triggering
contextual actions) across any design system or platform, followed by Android
Motion design system specifications for compliance auditing.

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
    the underlying interactive touch target is a larger 48x48px area.

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

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion adherence audit, evaluate detected chips against the following strict standards derived from Android Motion guidelines:

### Android Motion Chip Variants & Semantics

The Android Motion design system distinguishes four specific chip variants based on purpose and authorship:

| Variant             | Purpose & Authorship     | Rationale & Typical Use Case |
| :------------------ | :----------------------- | :--------------------------- |
| **Assist Chip**     | Action, Product-authored | Triggers smart or automated actions related to the current context (e.g., "Add to calendar"). |
| **Filter Chip**     | Filter, Product-authored | Filters content in a collection. Supports single or multi-select; displays a leading checkmark when active. |
| **Input Chip**      | Information, User-authored | Represents discrete user-entered information (e.g., email contacts). Features a trailing remove icon. |
| **Suggestion Chip** | Information, Product-authored | Narrows user intent by presenting dynamically generated suggestions (e.g., AI chat replies). |

### Android Motion Tokens & Measurements

-   **Container Height**: Strictly 32px across all standard variants.
-   **Corner Radius**: Standard chip containers feature an **8px corner radius** (`--droid-sys-shape-corner-small`). *Exception*: People input chips are fully rounded using `--droid-sys-shape-corner-full` or `--droid-sys-shape-corner-large` (16px).
-   **Icon & Avatar Sizing**: Standard icons are 18px. Leading avatars in input chips are 24px with a 12px corner radius (`--droid-sys-shape-corner-medium`).
-   **Internal Padding**:
    -   *Without Icon*: 16px padding on the left and right.
    -   *With Icon*: 8px padding on the icon side.
    -   *Element Spacing*: 8px padding between internal elements (e.g., avatar and text).
-   **External Spacing**: Chip sets must maintain a minimum of 8px spacing between adjacent chips.
-   **Touch Target & Density**: Minimum 48x48px interactive touch target.
-   **Color & Stroke**:
    -   Elevation defaults to level0 (`--droid-sys-elevation-level0`).
    -   Container stroke uses the `--droid-sys-color-outline` color role by default.
    -   Active / Selected Filter Chips utilize `--droid-sys-color-secondary-container` for container fill and `--droid-sys-color-on-secondary-container` for label and icon.
-   **Typography**:
    -   Chip label text must use `--droid-sys-typescale-label-large` (500 0.9rem/1.3rem `'Google Sans Text'`).
-   **Motion & Easing (Android Motion Exclusives)**:
    -   **Selection Transition**: When a Filter Chip is selected, the appearing checkmark must scale and fade in over `--droid-sys-motion-duration-150` (150ms) using `--droid-sys-motion-easing-standard` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`).
    -   **Hover / Focus State**: The hover state overlay should transition using `--droid-sys-motion-duration-100` (100ms) with `--droid-sys-motion-easing-linear`.

### Critical Android Motion Violations to Flag

-   **Conflating Chips with Buttons**: Using chips as persistent fixtures for linear workflow steps (where a standard `Button` is required), or placing more than 3 standard buttons in a cluster meant for dynamic chips.
-   **Label Text Overflow**: Exceeding 20 characters for a chip label, violating brevity guidelines.
-   **Default High Density**: Forcing dense chip layouts by default that shrink interactive touch targets below 48x48px without providing an accessible user override.
-   **Non-compliant Typography**: Using non-system fonts or incorrect styles instead of `'Google Sans Text'`.
