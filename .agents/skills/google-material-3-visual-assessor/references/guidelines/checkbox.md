# Checkboxes - Universal AI Detection Guide & MD3 Specifications

This reference provides universal visual heuristics for detecting Checkboxes
(selection controls enabling multi-item selection or binary toggling) across any
design system or platform, followed by Material Design 3 (MD3) specifications
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

## Part 2: MD3 Specifications & Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected
checkboxes against the following strict standards derived from Material
guidelines:

### MD3 Visual & Behavioral Rules

-   **Selection Control Distinction**:
    -   *Checkboxes*: Must be used when multiple related options can be selected
        from a list.
    -   *Radio Buttons*: Reserved for selecting a single mutually exclusive
        option from a list.
    -   *Switches*: Reserved for standalone or verbose binary options (like
        global settings).
-   **Tokens & Measurements**:
    -   **Container Size**: Strictly 18px x 18px visual box.
    -   **Corner Radius**: 2px corner radius (Note: `--md-sys-shape-corner-extra-small` is 4px in bindings, but MD3 checkbox uses a 2px/2dp corner shape).
    -   **Icon Size**: 18px checkmark or dash icon, perfectly center-aligned.
    -   **Touch Target**: Minimum 48px x 48px (`--md-sys-measurement-space600` is 48px!) invisible interactive square (to meet accessibility standards).
    -   **State Layer**: 40px diameter (`--md-sys-measurement-space500` is 40px!) circular hover/focus highlight.
-   **Color Roles**: Active checkboxes use primary color roles (`--md-sys-color-primary`) for the container fill and `On Primary` (`--md-sys-color-on-primary`) for the checkmark. Adjacent text labels must use the `On Surface` (`--md-sys-color-on-surface`) color role, remaining consistent during interaction.
-   **Parent-Child Hierarchy**:
    -   Checking a parent checkbox must check all child checkboxes. Unchecking a
        parent unchecks all children.
    -   If a parent has a mix of checked and unchecked children, it must display
        the **indeterminate** dash state. Tapping an indeterminate parent checks
        all child items.
-   **Density Rules**: Products must not apply high density to checkboxes by
    default, as doing so shrinks the interactive touch targets below the
    mandatory 48px x 48px (`--md-sys-measurement-space600`) best practice.

### Critical MD3 Violations to Flag

-   **Single Selection Misuse**: Using checkboxes for mutually exclusive
    single-select lists where a `Radio button` is required by Material
    semantics.
-   **Default High Density**: Forcing dense checkbox layouts by default that
    reduce touch targets below 48px x 48px (`--md-sys-measurement-space600`) without providing an accessible user
    override.
-   **Missing Indeterminate State**: Failing to present the indeterminate dash
    state on a parent checkbox when child selections are mixed.
