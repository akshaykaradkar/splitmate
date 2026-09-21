# Checkboxes - Universal AI Detection Guide \& Android Motion Specifications

This reference provides universal visual heuristics for detecting Checkboxes (selection controls enabling multi-item selection or binary toggling) across any design system or platform, followed by Android Motion design system specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Checkbox** is a compact selection control that allows users to select one or more items from a list or toggle a binary option on or off. Agents must detect checkboxes based on their distinct geometric proportions, visual check markers, and adjacent text layout, regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **Geometry \& Proportions**: Checkboxes appear as small square boxes with a strict 1:1 aspect ratio. They typically feature minimal corner rounding (e.g., sharp or slightly rounded corners).
-   **Visual Markers \& States**:
    -   *Unselected*: An empty square container defined by a simple outer border stroke.
    -   *Selected*: A solid filled square container housing a distinct checkmark vector inside.
    -   *Indeterminate*: A filled square container housing a horizontal line or dash inside (indicating a mixed state of selected and unselected child items).
-   **Placement \& Contextual Layout**: Checkboxes are almost always positioned immediately adjacent to a text label (typically on the left of the label). They commonly appear in stacked vertical groups or lists. Multiple small square boxes aligned vertically alongside text lines indicate a checklist or multi-select group.
-   **Interactive Area \& DOM Heuristics**: While the visible square box is small, the underlying interactive touch target is a larger invisible square area centered over the visual box. When scanning UI trees or accessibility layers, agents should look for this expanded touch target area.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications \& Compliance Auditing

When conducting an Android Motion design system adherence audit, evaluate detected checkboxes against the following strict standards:

### Android Motion Visual \& Behavioral Rules

-   **Selection Control Distinction**:
    -   *Checkboxes*: Must be used when multiple related options can be selected from a list.
    -   *Radio Buttons*: Reserved for selecting a single mutually exclusive option from a list.
    -   *Switches*: Reserved for standalone or verbose binary options (like global settings).
-   **Tokens \& Measurements**:
    -   **Container Size**: Strictly **18px x 18px** visual box.
    -   **Corner Radius**: Strictly **4px** corner rounding, mapping to `--droid-sys-shape-corner-extra-small` (4px).
    -   **Icon Size**: 18px checkmark or dash icon, perfectly center-aligned.
    -   **Touch Target**: Minimum **48px x 48px** invisible interactive square (to meet accessibility standards).
    -   **State Layer**: 40px diameter circular hover/focus highlight centered on the checkbox.
-   **Color Roles**:
    -   *Selected / Active State*: Container fill uses `--droid-sys-color-primary` (Light `#0b57d0` / Dark `#a8c7fa`), and the checkmark/dash uses `--droid-sys-color-on-primary` (Light `#ffffff` / Dark `#062e6f`).
    -   *Unselected State*: Container is transparent, defined by an outline stroke using `--droid-sys-color-outline` (Light `#747775` / Dark `#8e918f`).
    -   *Adjacent Labels*: Must use the `--droid-sys-color-on-surface` (Light `#1f1f1f` / Dark `#e3e3e3`) or `--droid-sys-color-on-background` color role, remaining consistent during interaction.
-   **Parent-Child Hierarchy**:
    -   Checking a parent checkbox must check all child checkboxes. Unchecking a parent unchecks all children.
    -   If a parent has a mix of checked and unchecked children, it must display the **indeterminate** dash state. Tapping an indeterminate parent checks all child items.
-   **Density Rules**: High density must not be applied by default, as shrinking the interactive touch targets below the 48px x 48px boundary violates accessibility standards.

### Android Motion Animation \& State Transitions

To adhere to the Android Motion identity, selection states must feature fluid, high-quality transitions rather than abrupt cuts:

-   **Morphing \& Stroke Drawing**: Transitioning from unselected to selected must animate the container's background color fill and scale simultaneously, while drawing the checkmark path dynamically from start to end. This uses `--droid-sys-motion-easing-standard` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) over a duration of `--droid-sys-motion-duration-150` (150ms).
-   **Collapse \& Fade**: Deselecting a checkbox should collapse the fill and fade out the checkmark using `--droid-sys-motion-easing-standard-accelerate` over `--droid-sys-motion-duration-100` (100ms).

### Critical Android Motion Violations to Flag

-   **Single Selection Misuse**: Using checkboxes for mutually exclusive single-select lists where a `Radio button` is required by semantics.
-   **Default High Density**: Forcing dense checkbox layouts by default that reduce touch targets below 48px x 48px without providing an accessible user override.
-   **Missing Indeterminate State**: Failing to present the indeterminate dash state on a parent checkbox when child selections are mixed.
-   **Incorrect Corner Rounding**: Using sharp 0px or overly rounded shapes (\>4px) instead of the standard `--droid-sys-shape-corner-extra-small` (4px).
-   **Static Transitions**: Failing to animate the checkmark selection, which violates the motion-first principles of the Android Motion design system.
