# Radio Buttons - Universal AI Detection Guide \& Android Motion Specifications

This reference provides universal visual heuristics for detecting Radio Buttons (selection controls enabling mutually exclusive single-item selection) across any design system or platform, followed by Android Motion design system specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Radio button** is a compact selection control that allows users to choose exactly one option from a mutually exclusive set of related choices (typically 5 or fewer). Agents must detect radio buttons based on their circular geometry, inner dot markers, grouped clustering, and adjacent text layout, regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **Circular Geometry \& Inner Dot Markers**: Look for small circular shapes (e.g., 20px rendered size) featuring two distinct visual states:
    -   *Unselected State*: An open, empty circular outline stroke with a transparent fill.
    -   *Selected State*: A circular outline containing a prominent, solid filled center dot.
-   **Grouped Clustering Behavior**: Radio buttons rarely appear alone; they are consistently presented in related groups (stacked vertical lists or horizontal rows) where exactly **one** item displays the selected inner dot state.
-   **Placement \& Contextual Layout**: Each radio button is obligatorily paired with an adjacent text label describing the choice (typically placed to the right of the radio button). Look for vertical stacks of circular icons aligned alongside text rows.
-   **Interactive Touch Area \& DOM Heuristics**: While the visible circular icon is small (20px), the underlying interactive touch target is a larger invisible 48px x 48px square centered over the icon. Agents scanning accessibility trees should note that clicking either the radio icon or its adjacent text label triggers selection.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications \& Compliance Auditing

When conducting an Android Motion design system adherence audit, evaluate detected radio buttons against the following strict standards:

### Android Motion Visual \& Behavioral Rules

-   **Selection Control Semantics**:
    -   *Radio Buttons*: Mandatory when users must select exactly **one** item from a short list of mutually exclusive options (5 or fewer).
    -   *Checkboxes*: Reserved for selecting multiple options from a list.
    -   *Switches*: Reserved for standalone binary toggles (like global settings).
    -   *Dropdown Menus*: Recommended over radio buttons only when screen space is severely constrained.
-   **Tokens \& Measurements**:
    -   **Icon Size**: Strictly **20px x 20px** visual circle.
    -   **Touch Target Size**: Minimum **48px x 48px** invisible square (to meet accessibility standards).
    -   **State Layer Size**: 40px diameter circular hover/focus highlight centered on the icon.
-   **Color Roles**:
    -   *Selected State*: The outer circle outline and the inner dot strictly use `--droid-sys-color-primary` (Light `#0b57d0` / Dark `#a8c7fa`).
    -   *Unselected State*: The outer circle uses `--droid-sys-color-outline` (Light `#747775` / Dark `#8e918f`), and the inner dot is empty/transparent.
    -   *Adjacent Labels*: Must use the `--droid-sys-color-on-surface` (Light `#1f1f1f` / Dark `#e3e3e3`) or `--droid-sys-color-on-background` color role, remaining consistent during interaction.
-   **Mutually Exclusive Selection \& Deselection**:
    -   Selecting one radio button automatically deselects all other radio buttons in the group.
    -   *Permanent Selection*: A radio group cannot be returned to an entirely unselected state once a choice is made. To allow users to opt out, products must provide an explicit "None", "Not applicable", or "Clear selection" option.
-   **Density Rules**: High density must not be applied by default, as shrinking the interactive touch targets below the 48px x 48px boundary violates accessibility.

### Android Motion Animation \& State Transitions

As a core element of the Android Motion design system, state changes must be accompanied by smooth physical motion:

-   **Inner Dot Growth**: Selecting a radio button must trigger an animation where the inner solid dot scales up from 0px to its full diameter (typically 10px). This should use the standard easing token `--droid-sys-motion-easing-standard` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) or the emphasized easing token `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) over a duration of `--droid-sys-motion-duration-150` (150ms).
-   **Deselection Shrinking**: The previously selected radio button's inner dot must shrink and fade out concurrently over a duration of `--droid-sys-motion-duration-100` (100ms) with a smooth standard-accelerate easing.

### Critical Android Motion Violations to Flag

-   **Multi-Select Misuse**: Allowing multiple radio buttons within a single grouped set to be selected simultaneously (where `Checkbox` controls are required).
-   **Missing Adjacent Labels**: Presenting a radio button without an accompanying text label, violating accessibility guidelines.
-   **Default High Density**: Forcing dense radio button layouts by default that reduce touch targets below 48px x 48px without providing an accessible user override.
-   **Static State Changes**: Having the inner dot pop into existence instantly without transition, failing the motion standards of the design system.
