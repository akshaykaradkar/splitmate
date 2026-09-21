# Buttons - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Buttons
(actionable interactive elements prompting primary, secondary, or tertiary
actions) across any design system or platform, followed by Android Motion
design system specifications for compliance auditing.

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
    *   **Progress Tracks**: Some buttons (especially media control buttons like
        "Resume", "Play", or "Download") may contain an embedded linear progress
        indicator (e.g., a thin colored line at the very bottom of the button
        container showing completion or playback progress). You MUST identify
        and map this progress line as a separate `Progress indicator` component,
        nested conceptually within the button area.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion design system adherence audit, evaluate detected
buttons against the following strict standards derived from Android Motion guidelines:

### Android Motion Visual & Behavioral Rules

-   **Label Typography & Formatting**:
    -   Must utilize concise label text (1-3 words) written in **sentence case**
        (capitalizing only the first word and proper nouns, e.g., "Book with
        flights", not "BOOK WITH FLIGHTS").
    -   Label text must never be truncated or wrapped to a second line; it must
        remain fully visible on a single line.
    -   The default typeface for button labels is `'Google Sans Text'`, mapped via
        the `--droid-sys-typescale-label-large` token (`500 0.9rem/1.3rem 'Google Sans Text'`).
-   **Toggle Buttons (Binary Selection)**:
    -   Used for binary choices (e.g., Save, Favorite).
    -   *Visual Cues*: Unselected toggle buttons use an outlined icon; selected
        toggle buttons switch to a filled icon (or increased font weight). By
        default in the Android Motion system, toggle buttons change from round
        (`--droid-sys-shape-corner-full`) to square (e.g., `--droid-sys-shape-corner-medium` / `12px`) when selected.
-   **Sizing & Platform Differences**:
    -   *Android/Web*: Default button height is 40dp. The system supports multiple sizes
        (XS, S, M, L, XL).
    -   *iOS Platform*: Default button height is 44pt (to meet Apple tap target requirements).
        Hover and focus states are unavailable. Labels use SF Pro 11pt as fallback.

### Android Motion Button Color Styles & Hierarchy

The Android Motion design system defines distinct button styles to establish a clear
visual hierarchy of emphasis:

1.  **Filled Button**: High visual impact, using the primary color role.
    Reserved for important, final unblocking actions (e.g., Save, Confirm, Join now).
    Must be used sparingly (ideally one per page).
    -   *Container Color*: `--droid-sys-color-primary` (light-dark(`#0b57d0`, `#a8c7fa`))
    -   *Content/Text Color*: `--droid-sys-color-on-primary` (light-dark(`#ffffff`, `#062e6f`))
2.  **Filled Tonal Button (Tonal)**: Medium-high emphasis, using the secondary container
    color mapping and a softer fill. Used for lower-priority primary actions
    (e.g., Next in onboarding).
    -   *Container Color*: `--droid-sys-color-secondary-container` (light-dark(`#c2e7ff`, `#004a77`))
    -   *Content/Text Color*: `--droid-sys-color-on-secondary-container` (light-dark(`#001d35`, `#c2e7ff`))
3.  **Elevated Button**: A tonal/surface button featuring an elevation shadow. Used
    sparingly to provide necessary visual separation from prominent backgrounds.
    -   *Container Color*: `--droid-sys-color-surface` (light-dark(`#fdfcfb`, `#1f1f1f`))
    -   *Content/Text Color*: `--droid-sys-color-primary` (light-dark(`#0b57d0`, `#a8c7fa`))
    -   *Elevation Shadow*: `--droid-sys-elevation-level1` (1px) or `--droid-sys-elevation-level2` (3px) at rest.
4.  **Outlined Button**: Medium emphasis, featuring a visible border stroke and
    transparent fill. Pairs well with filled buttons for secondary alternative actions.
    -   *Border Stroke*: 1px using `--droid-sys-color-outline` (light-dark(`#747775`, `#8e918f`))
    -   *Content/Text Color*: `--droid-sys-color-primary` (light-dark(`#0b57d0`, `#a8c7fa`))
5.  **Text Button**: Lowest emphasis, with no visible container at rest. Often
    placed in cards, dialogs, and snackbars to avoid distracting from nearby content.
    -   *Container Color*: Transparent background.
    -   *Content/Text Color*: `--droid-sys-color-primary` (light-dark(`#0b57d0`, `#a8c7fa`))

### Critical Android Motion Violations to Flag

-   **Text Truncation or Wrapping**: Wrapping button labels to multiple lines or
    clipping text with ellipses instead of adjusting container width.
-   **Improper Casing**: Using ALL CAPS for button labels instead of the
    required sentence case (unless overriding for specific brand guidelines).
-   **Overusing High-Emphasis Buttons**: Placing multiple prominent `Filled button`
    instances adjacent to one another, disrupting the page's visual hierarchy.
-   **Incorrect Corner Treatment**: Failing to use the fully rounded pill-shape corner
    token `--droid-sys-shape-corner-full` for standard active buttons (resting corner radius must be 20dp or 50% height).
