# Chips - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Chips (compact
interactive elements used for entering information, filtering, or triggering
contextual actions) across any design system or platform, followed by Elements
GM3 specifications for compliance auditing.

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
    the underlying interactive touch target is a larger 48x48dp area.
    -   *Dual Actions (Split Chips)*: If a chip features both a primary selection
        action and a trailing remove icon, agents scanning accessibility trees
        should note they act as two distinct focusable interactive elements.

-   **Chip vs. Button Distinction**: Focus on the functional purpose
    (filter/selection vs. action) and visual grouping:
    *   **Chip**: Used for filtering content, selecting options from a set, or
        representing input tags. They often appear in filter bars or tag groups,
        and typically have a smaller corner radius (e.g., 8dp rounded
        rectangle). Standalone filter dropdowns (e.g., selecting a time range)
        function as filters and must be mapped to `Chip`, not to generic buttons.
    *   **Button**: Triggers a primary action (e.g., Submit, Save, Compose) or
        toggles a primary system/feature state. Buttons are typically standalone
        or in action groups, and are usually fully rounded (pill-shaped).

-   **Chip vs. FAB Distinction**: Do not classify a component as a `Chip` if it
    floats above content (persistent on scroll) and functions as a primary or
    contextual action trigger. Standalone floating elements (pill-shaped or
    rounded-rectangular) must be mapped to `Extended FAB` (or `FAB`), not `Chip`.

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected chips
against the following strict standards derived from Elements GM3 guidelines:

### Elements GM3 Chip Variants & Semantics

Elements GM3 distinguishes four specific chip variants based on purpose and authorship:

-   **Input Chip**: Represents discrete user-entered information (e.g., Gmail contacts).
    Features a visible trailing "X" remove icon. Deleting an input chip must remove the
    entire chip rather than individual characters.
-   **Assistive Chip**: Represents smart, automated, multi-step actions (e.g., "Add to
    calendar" or "Open in Drive"). Text typically starts with a short verb.
-   **Suggestive Chip**: Narrows user intent by presenting dynamically generated options
    (e.g., suggested AI replies). Text labels are typically nouns or categories.
-   **Filter Chip**: Filters a collection of items. Supports:
    -   *Toggle on/off*: Flips between applied and unapplied states.
    -   *Dropdown Filter*: Includes a trailing chevron icon opening a menu. Follows the ARIA
        "menu button" pattern with `menuitemradio` roles. The first menu item must represent the
        unfiltered state (e.g., "All products") and be selected by default. Upon selection,
        the chip updates its label to reflect the active filter option.

### Elements GM3 Tokens & Measurements

-   **Differences from GM2**: All chips (except People chips) have **8dp rounded corners**
    instead of being fully rounded. Outlines in Elements GM3 are intentionally **darker**
    to strictly satisfy Non-Text Contrast (NTC) requirements.
-   **Container Height**: Standard **32dp** across all variants.
-   **Corner Radius**:
    -   *Standard Chips*: **8dp corner radius** (Small baseline shape style, corresponding to `--cee3-sys-shape-corner-small`).
    -   *People Chips*: Google-specific prefab containing rich information, styled with a **fully rounded 16dp / pill-shape corner radius** (Full shape style, `--cee3-sys-shape-corner-full`) to accommodate standard circular user avatars.
-   **Elevation**:
    -   *Flat (Default)*: Mapped to Resting Elevation **0** (0dp height).
    -   *Elevated*: Resting Elevation **+1** (1dp height, `--cee3-sys-elevation-level-1` or equivalent). Used only when placement requires visual protection (e.g., overlaying a pattern or busy background imagery).
-   **Internal Paddings & Spacing**:
    -   Standard padding is **16dp** left/right without icons.
    -   Padding reduces to **8dp** on the icon side if a leading or trailing icon is present.
    -   Minimum **8dp** spacing must be maintained between adjacent chips in groups.

### Layout, Interaction, & A11y Guidelines

-   **Basic vs. Split Chips**:
    -   *Basic Chips*: Perform a single action; the entire chip functions as a single Tab stop.
    -   *Split Chips*: Contain two distinct interactive zones (e.g., a primary link and a trailing remove button). Each zone acts as a separate focusable element. Avoid dense arrangements of split chips due to lengthy keyboard traversal. Expand target sizes beyond visual bounds where possible, avoiding overlap. Split trailing 'X' buttons are not recommended on mobile; use an additional trailing 18dp 'X' to improve mobile touch target compliance.
-   **Placement & Overflow**:
    -   *Input fields*: Chips must be placed **above** input fields for multi-selection, preventing the chips from being obscured by active dropdown suggestion lists (ARIA compliant).
    -   *Wrapping*: Chips can wrap to multiple lines, or use horizontal overflow indicators to maintain a single-row toolbar. Filter chip toolbars use single-row overflow configurations.
-   **Label Limits & Truncation**:
    -   Avoid labels exceeding **20 characters**. If longer text is necessary, the chip must have a fixed pixel width, apply truncation, and display the full text string inside a focus-triggered / hover-triggered **tooltip**.
-   **Accessibility Auditing & DOM**:
    -   *Labels*: Removable chips or split-chips with delete actions must use the accessibility label `"Remove [visible chip text]"` (GAR-compliant).
    -   *Icon Decoration*: Supporting icons (such as thumbnail avatars) must be marked as decorative (`aria-hidden="true"`) to avoid redundant screen reader announcement.
    -   *Announcements*: Successful interactions (such as chip deletion or filter updates) must immediately announce to assistive tech using an ARIA live region (e.g., "Results updated" or "XYZ removed").

### Critical Elements GM3 Violations to Flag

-   **Improper Corner Roundedness**: Applying a fully rounded pill-shape to standard chips instead of the mandatory 8dp corner radius.
-   **Weak Contrast Outlines**: Utilizing thin, light border outlines that violate Non-Text Contrast (NTC) guidelines.
-   **Obscured Input Fields**: Placing selected chips inside or below active text inputs where they get blocked by open dropdown menus.
-   **Missing Deletion Labels**: Failing to provide screen readers with explicit "Remove [label]" labels on removable chips.
-   **Missing Suggestion/Update Announcements**: Filtering or removing chips silently without calling an ARIA live region update.
-   **Over-long Labels**: Labels exceeding 20 characters without ellipsis truncation or tooltip support.
