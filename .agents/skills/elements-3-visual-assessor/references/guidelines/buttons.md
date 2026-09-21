# Buttons - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Buttons
(actionable interactive elements prompting primary, secondary, or tertiary
actions) across any design system or platform, followed by Elements GM3
specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Button** is an interactive UI element designed to communicate and prompt
user actions (e.g., submitting a form, confirming a modal, navigating, or
toggling a state). Agents must detect buttons based on their structural,
geometric, and textual qualities, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Contextual Anchoring**: Buttons appear throughout UI layouts:
    -   *Dialogs & Modals*: Typically anchored at the bottom trailing edge
        (confirm/cancel actions).
    -   *Forms & Cards*: Placed inline or at the bottom of containers to submit
        data or trigger card-specific actions.
    -   *Toolbars & App Bars*: Positioned as high-priority leading or trailing
        actions (e.g., Back, Done, Cancel).
-   **Visual Boundaries & Containment**: Buttons are defined by distinct
    bounding boxes that separate them from static body text:
    -   *Solid / Filled Containers*: High-contrast background shapes
        (pill-shaped, rectangular, or rounded squares).
    -   *Outlined Containers*: Transparent background fill enclosed by a
        distinct border stroke.
    -   *Text-Only / Minimalist*: Lacking a visible container at rest, relying
        on distinctive link colors, casing, or placement (e.g., card/dialog
        action rows) to indicate interactivity.
-   **Core Anatomy**:

    -   *Label Text*: Concise text (1-3 words) describing the action.
    -   *Leading/Trailing Icon (Optional)*: An accompanying visual icon placed
        next to the text label.
    -   *Container*: The interactive touch target enclosing the label and icon.

-   **Button vs. Card Distinction**: Do not classify a component as a `Card` if
    it only serves as a single action trigger, even if it is large, rectangular,
    or has rounded corners. A Button is a single interactive target with a
    simple label (1-3 words) and/or icon representing a single action, whereas a
    Card groups composite content (e.g. text descriptions, metrics, images) or
    multiple actions.

-   **Button vs. Chip Distinction**: Focus on the functional purpose (action vs.
    filter/selection) and visual grouping:

    *   **Button**: Triggers a primary action (e.g., Submit, Save, Compose) or
        toggles a primary system/feature state. Buttons are typically standalone
        or in action groups and are usually fully rounded (pill-shaped).
    *   **Chip**: Used for filtering content, selecting options from a set, or
        representing input tags. Chips typically appear in filter bars or tag
        groups, and often have a smaller corner radius (e.g., 8dp rounded
        rectangle). A standalone filter dropdown (e.g., selecting a time range)
        functions as a filter and should be mapped to `Chip` (specifically
        behaving as a filter chip), not to a generic button.

-   **Embedded Components inside Buttons**:

    *   **Progress Tracks / Indicators**: Some buttons may contain an embedded linear or circular progress indicator to express connection between interaction and activity status. You MUST identify and map this progress line as a separate `Progress indicator` component, nested conceptually within the button area.

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected
buttons against the following strict standards derived from Elements GM3 guidelines:

### Elements GM3 Visual & Behavioral Rules

-   **Label Typography & Formatting**:
    -   Must utilize concise label text (1-3 words) written in **sentence case**
        (capitalizing only the first word and proper nouns, e.g., "Book with flights", not "BOOK WITH FLIGHTS" or "Book With Flights").
    -   Label text must never be truncated or wrapped to a second line; it must
        remain fully visible on a single line.
-   **Shapes & Corner Radii**:
    -   All buttons with containers (Filled, Tonal, Elevated, Outlined) must utilize the **Full shape style (circular/pill-shaped)** with a corner radius of **20dp** (which is 50% of the default 40dp container height).
-   **Sizing & Spacing measurements**:
    -   *Android/Web*: Default button height is **40dp** with a minimum tap target size of **48dp**.
    -   *iOS Platform*: Default button height is **44pt** to meet iOS tap target requirements.
    -   *Spacing between adjacent/paired buttons*: Must use exactly **8px (8dp)** spacing between paired button containers.
-   **Icon Specs inside Buttons**:
    -   Icons must be standard **18 x 18dp** size.
    -   Icons should always appear in the leading position (to the left of the text label in LTR).
    -   Icons and label text must share the exact same color.
-   **Disabled Button Guidelines**:
    -   Be extremely cautious with disabled buttons. Buttons at the end of a form should **always remain in the enabled state** (GAR guidelines). If required fields are invalid upon submit, clicking triggers relevant validation/error callouts.
    -   *Soft disabled*: Focusable in tab order, can trigger tooltip, but has muted contrast.
    -   *Hard disabled*: Not focusable in tab order, cannot trigger tooltip, and has muted contrast.

### Elements GM3 Button Color Styles & Hierarchy

Elements GM3 defines distinct button styles to establish a clear visual hierarchy of emphasis:

1.  **Button filled (Filled Button)**: High visual impact, using the primary color palette. Reserved for important, final unblocking actions (e.g., Save, Confirm, Done). Must be used sparingly (ideally at most one per page or dialog container).
2.  **Button tonal (Tonal Button)**: Medium-high emphasis, using the secondary color mapping and a softer tonal fill. Used for final, supporting, or secondary unblocking actions.
3.  **Button elevated (Elevated Button)**: A tonal button featuring an elevation shadow (resting level 1 / 1dp shadow). Only use when a button requires visual separation from a busy or patterned background.
4.  **Button outlined (Outlined Button)**: Medium emphasis, featuring a visible outline border stroke and transparent fill. Perfect for actions that need attention but aren’t primary (e.g. "Reply", "View all", "Add to cart"), or giving someone the opportunity to escape a flow.
5.  **Button text (Text Button)**: Lowest emphasis, with no visible container or border at rest. Used for optional, tertiary, or supplementary actions (e.g. "Learn more", "Change account", "Turn on"). *Note: The Neutral text button from GM2 has been deprecated.*
6.  **Icon button**: Highly compact, subtle icon-only button used for supplementary, lower-priority actions like "Bookmark" or "Star".

### Critical Elements GM3 Violations to Flag

-   **Text Truncation or Wrapping**: Wrapping button labels to multiple lines or clipping text with ellipses instead of adjusting container width.
-   **Improper Casing**: Using ALL CAPS or Title Case for button labels instead of the required **sentence case**.
-   **Overusing High-Emphasis Buttons**: Placing multiple prominent `Button filled` instances adjacent to one another or within the same view, disrupting the visual hierarchy.
-   **Incorrect Corner Shapes**: Standalone buttons with a container having square corners (0dp) or non-pill rounded corners instead of the required **Full shape style (20dp / 50% radius)**.
-   **Disabled Form Submission Buttons**: Disabling the submit button at the end of a form, violating GAR web accessibility requirements.
-   **Redundant Color Alerts**: Using red color buttons in general UI flows when they should be reserved strictly for confirmation dialogs for irreversible/destructive actions (e.g. paired with a trash can icon).
