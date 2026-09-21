# Checkboxes - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Checkboxes
(selection controls enabling multi-item selection or binary toggling) across any
design system or platform, followed by Elements GM3 specifications
for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Checkbox** is a compact selection control that allows users to select one or
more items from a list or toggle a binary option on or off. Agents must detect
checkboxes based on their distinct geometric proportions, visual check markers,
and adjacent text layout, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Geometry & Proportions**: Checkboxes appear as small square boxes with a
    strict 1:1 aspect ratio. They typically feature minimal corner rounding
    (e.g., sharp or slightly rounded corners).
-   **Visual Markers & States**:
    -   *Unselected*: An empty square container defined by a simple outer border
        stroke.
    -   *Selected*: A solid filled square container housing a distinct checkmark
        vector inside.
    -   *Indeterminate*: A filled square container housing a horizontal line or
        dash inside (indicating a mixed state of selected and unselected child
        items).
-   **Placement & Contextual Layout**: Checkboxes are almost always positioned
    immediately adjacent to a text label (typically on the left of the label).
    They commonly appear in stacked vertical groups or lists. Multiple small
    square boxes aligned vertically alongside text lines indicate a checklist or
    multi-select group.
-   **Interactive Area & DOM Heuristics**: While the visible square box is
    small, the underlying interactive touch target is a larger invisible square
    area centered over the visual box. When scanning UI trees or accessibility
    layers, agents should look for this expanded touch target area.

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected
checkboxes against the following strict standards derived from Elements GM3 guidelines:

### Elements GM3 Visual & Behavioral Rules

-   **Selection Control Distinction**:
    -   *Checkboxes*: Must be used when multiple related options can be selected
        from a set, or for non-immediate binary options (e.g., inside forms).
    -   *Radio Buttons*: Reserved for selecting a single mutually exclusive
        option from a list.
    -   *Switches*: Reserved for standalone, immediate-action settings, and are
        highly preferred on mobile/tablet settings pages (for desktop, checkboxes or
        radio buttons are preferred to save space).
-   **Tokens & Measurements**:
    -   **Container Size**: Strictly **18dp x 18dp** visual box.
    -   **Corner Radius**: **2dp corner radius** (Extra Small shape style).
    -   **Icon Size**: 18dp checkmark or dash icon, perfectly center-aligned.
    -   **Touch Target**: Minimum **48dp x 48dp** invisible interactive square
        to satisfy accessibility standards (GAR/WCAG).
-   **Color Roles & Typography**:
    -   Active checkboxes use CEE GM3 primary color roles for the container fill
        (e.g., `--cee3-sys-color-primary`) and `On Primary` (`--cee3-sys-color-on-primary`)
        for the checkmark.
    -   Adjacent text labels must use the `On Surface` (`--cee3-sys-color-on-surface`)
        color role, written in sentence case with no ending punctuation.
-   **Parent-Child Hierarchy (Sub-selections)**:
    -   Checking a parent checkbox must check all child checkboxes. Unchecking a
        parent unchecks all children.
    -   If some, but not all, child checkboxes are checked, the parent checkbox
        must display the **indeterminate** dash state. Tapping an indeterminate parent
        checks all child items.
-   **Single & Group Errors**:
    -   When an error occurs on a term-and-condition checkbox or a group of controls,
        the error text must appear and sit directly below the checkbox/selection group.
-   **Density Rules**:
    -   High density must not be applied to checkboxes by default, as doing so shrinks
        touch targets below the mandatory 48x48dp CSS pixel best practice.

### Critical Elements GM3 Violations to Flag

-   **Single Selection Misuse**: Using checkboxes for mutually exclusive
    single-select lists where a `Radio button` is required by design semantics.
-   **Default High Density**: Forcing dense checkbox layouts by default that
    reduce touch targets below 48x48dp without providing an accessible user override.
-   **Missing Indeterminate State**: Failing to present the indeterminate dash
    state on a parent checkbox when child selections are mixed.
-   **Displaced Error Text**: Positioning validation error messages in tooltips or
    remote page areas instead of directly beneath the failed checkbox or checkbox group.
