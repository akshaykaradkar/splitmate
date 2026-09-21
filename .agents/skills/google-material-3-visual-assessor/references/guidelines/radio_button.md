# Radio Buttons - Universal AI Detection Guide & GM3 Specifications

This reference provides universal visual heuristics for detecting Radio Buttons
(selection controls enabling mutually exclusive single-item selection) across
the Google Material 3 (GM3) design system, followed by concrete specifications
and design tokens for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Radio button** is a compact selection control that allows users to choose
exactly one option from a mutually exclusive set of related choices (typically 5
or fewer). Agents must detect radio buttons based on their circular geometry,
inner dot markers, grouped clustering, and adjacent text layout, regardless of
underlying web component implementation.

### Key Visual & Geometric Heuristics

-   **Circular Geometry & Inner Dot Markers**: Look for small circular shapes
    (20dp rendered size) featuring two distinct visual states:
    -   *Unselected State*: An open, empty circular outline stroke with a
        transparent fill.
    -   *Selected State*: A circular outline containing a prominent, solid
        filled center dot.
-   **Grouped Clustering Behavior**: Radio buttons rarely appear alone; they are
    consistently presented in related groups (stacked vertical lists or
    horizontal rows) where exactly **one** item displays the selected inner dot
    state.
-   **Placement & Contextual Layout**: Each radio button is obligatorily paired
    with an adjacent text label describing the choice (typically placed to the
    right of the radio button). Look for vertical stacks of circular icons
    aligned alongside text rows.
-   **Interactive Touch Area**: While the visible circular icon is small (20dp),
    the underlying interactive touch target is a larger invisible 48x48dp square
    centered over the icon. Clicking either the radio icon or its adjacent text
    label triggers selection.

--------------------------------------------------------------------------------

## Part 2: GM3 Specifications & Compliance Auditing

When conducting a Google Material 3 (GM3) adherence audit, evaluate detected
radio buttons against the following strict standards derived from GM3 guidelines:

### GM3 Visual & Behavioral Rules

-   **Selection Control Semantics**:
    -   *Radio Buttons*: Mandatory when users must select exactly **one** item
        from a short list of mutually exclusive options (5 or fewer).
    -   *Checkboxes*: Reserved for selecting multiple options from a list.
    -   *Switches*: Reserved for standalone binary toggles (like global settings).
    -   *Dropdown Menus*: Recommended over radio buttons only when screen space
        is severely constrained (as dropdowns require more clicks and cognitive
        effort).
-   **Tokens & Measurements**:
    -   **Icon Size**: Strictly 20dp x 20dp visual circle.
    -   **Touch Target Size**: Minimum 48dp x 48dp (to meet accessibility standards).
    -   **State Layer Size**: 40dp diameter circular hover/focus highlight centered on the icon.
-   **Color Roles & Design Tokens**:
    -   **Selected Icon (Outer Ring & Inner Dot)**: Uses the `--md-sys-color-primary` color role.
    -   **Unselected Icon**: Uses the `--md-sys-color-outline` or `--md-sys-color-on-surface-variant` color role.
    -   **Adjacent Text Label**: Must use the `--md-sys-color-on-surface` color role, remaining consistent during interaction.
    -   **Typography**: Label text should map to standard typescales (typically `--md-sys-typescale-body-medium` or `--md-sys-typescale-body-large`).
    -   **Shape**: Perfect circle, utilizing `--md-sys-shape-corner-full`.
-   **Mutually Exclusive Selection & Deselection**:
    -   Selecting one radio button automatically deselects all other radio buttons in the group.
    -   *Permanent Selection*: A radio group cannot be returned to an entirely unselected state once a choice is made. To allow users to opt out, products must provide an explicit "None", "Not applicable", or "Clear selection" option.
-   **Density & Platform Rules**:
    -   *Density*: Products must not apply high density by default, as doing so shrinks touch targets below 48x48dp without user overrides.
    -   *iOS Platform*: Radio buttons are not natively supported in Material iOS; GM3-branded checkmarked lists are the recommended iOS standard pattern.

### Critical GM3 Violations to Flag

-   **Multi-Select Misuse**: Allowing multiple radio buttons within a single grouped set to be selected simultaneously (where Checkbox controls are required).
-   **Missing Adjacent Labels**: Presenting a radio button without an accompanying text label, violating accessibility guidelines.
-   **Default High Density**: Forcing dense radio button layouts by default that reduce touch targets below 48x48dp without providing an accessible user override.
-   **Incorrect Color Mapping**: Using customized or accent colors instead of the standard `--md-sys-color-primary` for selected icons, or failing to use `--md-sys-color-on-surface` for adjacent text.
