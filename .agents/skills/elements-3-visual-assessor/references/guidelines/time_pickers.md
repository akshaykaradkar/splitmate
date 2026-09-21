# Time Pickers - Elements GM3 Design System Guide \& Specifications

This reference provides visual heuristics for detecting and evaluating Time Pickers under the Elements GM3 design system (including CEE3 system-level and GM3 component specifications) for visual and codebase auditing.

## Part 1: Universal AI Detection Heuristics for Elements GM3

An Elements GM3 **Time picker** is an interactive modal dialog that enables selecting hours and minutes. It supports two primary visual configurations (Dial view and Input view) and includes AM/PM selectors and action confirmation buttons.

### Key Visual \& Geometric Heuristics

-   **Modal Card and Scrim**: The time picker is housed within an elevated rectangular card container featuring generously rounded corners. It is displayed centered on top of a darkened background scrim overlay.
-   **Variant Configurations**:
    -   **Dial Picker (Clock Face)**: Displays a circular clock dial with hours (1-12 or 1-24) or minutes (0-59) around the perimeter, and a center-anchored radial pointer line connecting to the active value.
    -   **Input Picker (Digital Fields)**: Displays two large, prominent numeric text input fields representing hours and minutes, separated by a colon, with the active field clearly highlighted.
-   **AM/PM Segmented Selector**: A vertical segmented control containing "AM" and "PM" options positioned alongside the digital inputs (for 12-hour format).
-   **Mode Toggle Affordance**: A small keyboard icon (in Dial view) or clock icon (in Input view) positioned at the bottom-left corner of the modal container.
-   **Trailing Action Buttons**: Pinned "Cancel" and "OK" text buttons aligned to the bottom trailing edge of the container.

---

## Part 2: Elements GM3 Component Specifications

When auditing for Elements GM3 compliance, evaluate time pickers against these specific guidelines and measurements:

### 1. Sizing, Layout, and Sizing Configurations

-   **Portrait Sizing**: Digital time fields (hours/minutes input boxes) are stacked directly **above** the circular clock dial.
-   **Landscape Sizing**: Digital time fields are positioned **horizontally to the left** of the circular clock dial.
-   **Container Shape**: The outer card uses the standard GM3 extra-large shape token (matching `--cee3-sys-color-` compatibility, with rounded corners).
-   **Container Color**: Typically utilizes the `surface-container-high` or `surface` token to stand out prominently on top of the darkened scrim.

### 2. Sizing \& Accessibility Requirements

-   **Interactive Touch Targets**: To meet GAR and general accessibility standards, the interactive numbers on the clock dial and the mode toggle buttons must strictly meet the **48x48dp (48x48px)** minimum target size.
-   **Mandatory Mode Switch Toggle**: The time picker must feature an explicit keyboard/clock toggle button at the bottom-left to allow seamless switching between visual dial picking and manual digital text entry. This ensures full accessibility for screen readers and keyboard-only users.
-   **Labels**: Large numeric text input boxes must have clear, readable labels ("Hour" and "Minute") positioned directly below them.
-   **iOS Platform (Liquid Glass)**: GM3 iOS time pickers receive Liquid Glass styling updates and adhere to Apple Human Interface Guidelines for pickers.

---

## Part 3: Critical Elements GM3 Violations to Flag

-   **Missing Mode Switch Toggle**: Failing to provide the keyboard/clock icon toggle button in the bottom-left corner of the dialog container (a severe accessibility blocker).
-   **Undersized Dial Touch Targets**: Shrinking the clock dial numbers or active touch targets below the mandatory 48x48dp size.
-   **Missing Scrim Overlay**: Rendering the time picker dialog without a background dimming scrim, failing to isolate focus or block interaction with underlying content.
-   **Missing Labels**: Displaying the digital hours/minutes inputs without accompanying "Hour" and "Minute" labels underneath.
