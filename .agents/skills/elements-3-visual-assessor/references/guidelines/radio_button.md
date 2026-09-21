# Radio Buttons - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Radio Buttons
(selection controls enabling mutually exclusive single-item selection) across
any design system or platform, followed by Elements GM3
specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Radio button** is a compact selection control that allows users to choose
exactly one option from a mutually exclusive set of related choices (typically 5
or fewer). Agents must detect radio buttons based on their circular geometry,
inner dot markers, grouped clustering, and adjacent text layout, regardless of
design system adherence.

### Key Visual & Geometric Heuristics

-   **Circular Geometry & Inner Dot Markers**: Look for small circular shapes
    (e.g., 20dp rendered size) featuring two distinct visual states:
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
-   **Interactive Touch Area & DOM Heuristics**: While the visible circular icon
    is small (20dp), the underlying interactive touch target is a larger
    invisible 48x48dp square centered over the icon. Agents scanning
    accessibility trees should note that clicking either the radio icon or its
    adjacent text label triggers selection.

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected
radio buttons against the following strict standards derived from CE Elements guidelines:

### Elements GM3 Visual & Behavioral Rules

-   **Selection Control Semantics**:
    -   *Radio Buttons*: Mandatory when users must select exactly **one** item
        from a short list of mutually exclusive options (5 or fewer).
    -   *Checkboxes*: Reserved for selecting one or multiple options from a list.
    -   *Switches*: Reserved for standalone binary toggles (like global
        settings) that take immediate effect.
    -   *Dropdown Menus*: Recommended over radio buttons only when screen space
        is severely constrained, as dropdowns require more clicks and cognitive
        effort.
-   **Tokens, Shapes & Measurements**:
    -   **Shape**: Outer circular outline and selected inner dot must be perfectly circular, mapping to the **Full (Circular)** shape style (where the border radius is exactly half of the container's height).
    -   **Icon Size**: Strictly 20dp x 20dp visual circle.
    -   **Touch Target Size**: Minimum 48dp x 48dp invisible square centered on the icon to meet accessibility standards.
    -   **State Layer Size**: 40dp diameter circular hover/focus highlight centered on the icon.
-   **Color Roles**:
    -   Active (Selected) radio buttons use standard **Primary** color roles for the outer circle and inner dot.
    -   Adjacent text labels must use the standard **On Surface** text color (e.g., `--cee3-sys-color-extended-on-grey` or `--cee3-sys-color-extended-on-grey-container`).
    -   **Disabled State Persistence**: When a selection control is disabled, the paired label text must maintain its normal color/readability (do not over-fade) so users can still easily read the disabled options.
-   **Default Selection & Mutually Exclusive Behavior**:
    -   **Select the First Option by Default**: It is strongly recommended to have a selected radio button by default. Because users cannot deselect and return a radio group to an entirely unselected state once a choice is made, a default selection sets proper expectations and prevents illegal empty states.
    -   *Opt-Out/Clear Selection*: To allow users to opt out after selecting, products must provide an explicit, selectable option such as "None", "Not applicable", or "Clear selection".
-   **Group Labels & Accessibility**:
    -   A visible group header (e.g. "Select language") must be present and is used as the accessibility label for the group of controls that follows it. The label should clearly describe the relationship between grouped items.
-   **Focus Ring Interaction**:
    -   When a card layout contains only one interactive element (such as a single selection control), the focus ring can encompass the entire card.
    -   If a card includes multiple interactive elements, the focus ring must highlight only the radio button itself.
-   **Platform Rules**:
    -   *Density*: High density layouts must not be applied by default as they shrink touch targets below 48x48dp.
    -   *iOS Platform*: Radio buttons are not natively supported in CE Elements iOS; checkmarked lists are the recommended standard pattern for iOS.

### Critical Elements GM3 Violations to Flag

-   **Multi-Select Misuse**: Allowing multiple radio buttons in the same grouped set to be selected simultaneously (which requires `Checkbox` controls instead).
-   **Missing Adjacent Labels**: Presenting a radio button without an accompanying text label, violating accessibility guidelines.
-   **No Default Selection / No Opt-Out**: Leaving a radio group with no option selected by default while failing to provide a clear "None" or "N/A" option for users to clear their choice.
-   **Default High Density Layouts**: Forcing dense selection control spacing that shrinks touch targets below 48x48dp without providing an accessible user override.
