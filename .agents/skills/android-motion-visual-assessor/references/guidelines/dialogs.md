# Dialogs - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Dialogs (modal
popup windows requesting user decisions or presenting critical information)
across any design system or platform, followed by Android Motion design system
specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Dialog** is a modal popup container appearing in front of app content to
provide critical information, alert users, or request a decision. It disables
functionality outside the dialog container until addressed. Agents must detect
dialogs based on modality, scrim overlays, and containment geometry, regardless
of design system adherence.

### Key Visual & Geometric Heuristics

-   **Modality & Scrim Overlays**: Look for a distinct card or container
    floating on top of the primary UI, typically accompanied by a background
    **scrim** (a darkened, dimming, or blurred overlay layer) that obscures the
    underlying page content.
-   **Containment & Shape Geometry**:
    -   *Basic Dialogs*: Floating rectangular cards featuring prominent rounded
        corners, typically positioned in the center of the screen.
    -   *Full-Screen Dialogs*: Modal containers that expand to cover the entire
        screen viewport (on mobile) or take full width up to a set maximum on
        larger screens.
-   **Core Anatomy & Layout Flow**:
    -   *Headline / Title*: Bold, concise title text at the top explaining the
        dialog's purpose.
    -   *Body Content*: Supporting text description, lists, or form inputs.
    -   *Action Bar*: Action buttons (usually 1 or 2) aligned to the bottom
        trailing edge (bottom-right quadrant in LTR layouts).
-   **Action Hierarchy**: In a standard two-button confirmation lockup, the
    primary confirming action (e.g., Save, Confirm) is positioned at the far
    trailing edge (rightmost button), while the dismissive action (e.g., Cancel)
    is placed to its left.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion adherence audit, evaluate detected dialogs against the following strict standards derived from Android Motion guidelines:

### Android Motion Dialog Variants & Measurements

-   **Basic Dialog Specifications**:
    -   **Corner Radius**: Android Motion specifies a distinct **28px corner radius** (`--droid-sys-shape-corner-extra-large`).
    -   **Width & Padding**: Minimum width 280px; Maximum width 560px. Standard outer padding is 24px on all sides (Top, Left, Right, Bottom).
    -   **Internal Spacing**: 16px between title and body; 16px between icon and title; 24px between body and action buttons; 8px between action buttons.
    -   **Elevation**: Placed at the highest resting level in the product using `--droid-sys-elevation-level5` (12px drop shadow) to clearly establish separation from the scrim.
    -   **Color Mappings**:
        -   Container background: `--droid-sys-color-surface`.
        -   Text titles: `--droid-sys-color-on-surface`.
        -   Action buttons: utilize `--droid-sys-color-primary` (for filled text) or `--droid-sys-color-on-surface-variant` (for secondary dismissive actions).
    -   **Typography**:
        -   Title/Headline: `--droid-sys-typescale-headline-small` (1.5rem/2rem `'Google Sans'`).
        -   Body Content: `--droid-sys-typescale-body-medium` (0.9rem/1.3rem `'Google Sans Text'`).
-   **Full-Screen Dialog Specifications**:
    -   **Corner Radius**: 0px (straight edges, utilizing `--droid-sys-shape-corner-none`). Fills the screen on compact mobile devices; max width 560px on larger screens.
    -   **Header & Action Bar**: Header height is 56px (housing a close "X" icon button). Bottom action bar height is 56px.
    -   **Padding**: 24px top/left/right padding, with 8px between elements.

### Android Motion Behavioral, Action & Motion Rules

-   **Action Limits & Naming**: Dialogs must contain a maximum of two actions (one confirming, one dismissing). Adding a third action is strictly not recommended. Confirmation actions must use clear, specific verbs (e.g., "Send", "Create", "Save") rather than vague terms like "Done", "OK", or "Close".
-   **Headline Formatting**: Headlines must be brief and succinct. On Android, headlines must be concise enough to fit within 4 lines when text size is increased to 200%.
-   **Scrolling Constraints**: Dialog content should avoid scrolling. If scrolling is necessary, the title must be pinned at the top and action buttons pinned at the bottom so they remain visible.
-   **Entry / Exit Motion (Android Motion Exclusives)**:
    -   **Entry Animation**: The dialog container must scale up and fade in utilizing `--droid-sys-motion-easing-emphasized-decelerate` (`cubic-bezier(0.05, 0.7, 0.1, 1.0)`) over `--droid-sys-motion-duration-500` (500ms).
    -   **Exit Animation**: The dialog container must scale down and fade out utilizing `--droid-sys-motion-easing-emphasized-accelerate` (`cubic-bezier(0.3, 0.0, 0.8, 0.15)`) over `--droid-sys-motion-duration-150` (150ms).
    -   **Scrim Animation**: The scrim background should transition (fade in/out) smoothly over `--droid-sys-motion-duration-150` (150ms) using `--droid-sys-motion-easing-linear` in sync with the dialog container's life cycle.

### Critical Android Motion Violations to Flag

-   **Improper Action Naming or Count**: Using vague confirmation labels ("OK") or including 3 or more action buttons inside a basic dialog.
-   **Missing 28px Corner Radii**: Presenting a basic dialog with sharp or minimally rounded corners instead of the required 28px shape (`--droid-sys-shape-corner-extra-large`).
-   **Lacking Scrim Overlay**: Displaying a modal dialog without a background scrim, failing to focus user attention and obscure background UI.
-   **Incorrect Motion Curves or Durations**: Utilizing rigid linear/instantaneous transitions or incorrect easing curves for the entry/exit sequence.
