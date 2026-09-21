# Date Pickers - Android Motion Design System Guidelines & Specifications

This reference provides universal visual heuristics for detecting Date Pickers
(components enabling date or date range selection) across any application,
followed by Android Motion Design System specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Date picker** is an interactive UI component that allows users to select a
single date or a date range (e.g., scheduling flights, booking hotels, or
entering a birthdate). Agents must detect date pickers based on their calendar
grid layouts, date input fields, and range highlights, regardless of design
system adherence.

### Key Visual & Geometric Heuristics

-   **Calendar Grid Pattern**: A primary visual signature of a date picker is
    the presence of a conventional 7-column grid representing days of the week.
    Days of the week headers (e.g., S, M, T, W, T, F, S) are typically
    abbreviated and non-interactive.
-   **Variant & Layout Signatures**:
    -   *Docked Date Picker*: A text input field paired with a trailing calendar
        icon. When active, a calendar dropdown grid docks directly below the
        input field.
    -   *Modal Date Picker*: A popup dialog containing a prominent date header,
        month/year navigation arrows, a calendar grid, and bottom
        confirmation/cancel actions.
    -   *Modal Date Input*: Labeled direct text entry fields (often with
        `MM/DD/YYYY` helper text) housed inside a modal dialog, commonly
        featuring an edit/calendar icon to toggle between picker and input
        modes.
-   **Range Selection Visuals**: Look for distinct circular or
    rounded-rectangular selection shapes marking the start and end dates,
    connected by a lighter continuous background fill spanning the intermediate
    dates.
-   **Navigation Cues**: Arrow buttons for previous/next month navigation, and
    dropdown chevrons next to month/year titles.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion Design System adherence audit, evaluate detected
date pickers against the following strict standards:

### Android Motion Visual, Color & Typographic Rules

-   **Typography Scale**:
    -   The main date header or selected date label inside modal headers must use the Android Motion bold brand typeface: `Google Sans`. Specifically, it must map to `--droid-sys-typescale-headline-medium` (1.8rem, 'Google Sans') or `--droid-sys-typescale-title-large` (1.4rem, 'Google Sans').
    -   Calendar grid numbers and days of the week headers must use the readable plain typeface: `Google Sans Text`. Specifically, grid days must resolve to `--droid-sys-typescale-body-medium` (0.9rem, 'Google Sans Text') or `--droid-sys-typescale-label-large` (0.9rem, 'Google Sans Text').
    -   Helper text and field descriptors must resolve to `--droid-sys-typescale-body-small` (0.8rem, 'Google Sans Text') with tracking specified by `--droid-sys-typescale-body-small-tracking`.
-   **Color Mappings**:
    -   *Modal Container*: Uses `--droid-sys-color-surface` (light-dark(#fdfcfb, #1f1f1f)) to serve as a neutral background.
    -   *Header/Primary Accents*: High-emphasis elements use `--droid-sys-color-primary` (light-dark(#0b57d0, #a8c7fa)).
    -   *Selected Day Circle*: Must be styled with a solid background of `--droid-sys-color-primary` and text/icon using `--droid-sys-color-on-primary` (light-dark(#ffffff, #062e6f)).
    -   *Range Highlight Fill*: The intermediate range days should use `--droid-sys-color-primary-container` (light-dark(#d3e3fd, #0842a0)) as a background fill, with text using `--droid-sys-color-on-primary-container` (light-dark(#041e49, #d3e3fd)).
    -   *Day Hover/State Layer*: Pausing over a grid day applies a state layer overlay using the `--droid-sys-color-on-surface` color role at a standard opacity.
    -   *Input Field Boundaries*: In docked or input variants, text field borders must use `--droid-sys-color-outline` (light-dark(#747775, #8e918f)) or `--droid-sys-color-outline-variant`.
-   **Container Shape**:
    -   *Modal Container*: Must feature a rounded rectangle container with rounded corners mapping to `--droid-sys-shape-corner-extra-large` (28px) or `--droid-sys-shape-corner-large` (16px).
    -   *Selected Day Circle*: Selected days must have a fully circular indicator, using `--droid-sys-shape-corner-full` (max(50cqw, 50cqh)).
-   **Touch Targets & Spacing**:
    -   All interactive elements (grid day cells, month navigation arrows, action text buttons) must meet the mandatory minimum **48x48dp** touch target size.

### Android Motion Dynamics & Transition Rules

-   **Modal Entry Transition**:
    -   The date picker dialog must enter using the system-defined **Emphasized Easing set**, specifically `--droid-sys-motion-easing-emphasized` (cubic-bezier(0.2, 0.0, 0.0, 1.0)) for a natural, snappy take-off and extremely soft landing.
    -   The default transition duration for full screen-level entries must resolve to `--droid-sys-motion-duration-500` (500ms) or `--droid-sys-motion-duration-400` (400ms) for modal overlays.
-   **Mode Switching Animation**:
    -   Swapping between the calendar picker grid and manual text entry view must use the standard fade-through transition, fading the exiting view out before fading the new view in, to avoid overlapping transparent frames. It must utilize `--droid-sys-motion-easing-standard` over a duration of `--droid-sys-motion-duration-300` (300ms).
-   **RTL Mirroring**:
    -   For right-to-left locales, the calendar navigation arrows, grid layout columns, and buttons must be horizontally mirrored.

### Critical Android Motion Violations to Flag

-   **Calendar Grid for Distant Dates**: Forcing users to scroll a month-by-month calendar grid to select distant dates (like Date of Birth) instead of presenting a direct modal date input field.
-   **Undersized Touch Targets**: Interactive grid days or navigation arrows shrunk below the required 48x48dp size without adequate spacer padding.
-   **Missing Mode Toggle**: Failing to provide a clear edit pencil/calendar icon button to toggle between calendar selection and manual entry modes.
-   **Incorrect Font and Style**: Using non-standard typography (e.g. system sans-serif instead of `Google Sans` or `Google Sans Text`) or incorrect token colors (e.g., legacy purple instead of `--droid-sys-color-primary`).
-   **Linear Jump Cut / Stiff Transition**: Presenting dialogs with instant cuts or mechanical linear motion curves rather than the organic `--droid-sys-motion-easing-emphasized` easing curve.
