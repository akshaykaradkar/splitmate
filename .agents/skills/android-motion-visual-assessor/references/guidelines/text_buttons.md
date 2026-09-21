# Text Buttons - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Text Buttons
(actionable interactive text elements lacking a container at rest) across any
design system or platform, followed by Android Motion design system specifications
for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Text button** is a clean, low-emphasis interactive button that appears as
standalone text without a visible container bounding box at rest. They are used
for secondary or low-priority actions where visible containers would distract
from nearby primary content. Agents must detect text buttons based on graphic
isolation, casing, color contrast, and contextual placement, regardless of
design system adherence.

### Key Visual & Geometric Heuristics

-   **Absence of Resting Container**: Look for isolated text strings that
    function as clickable buttons but exhibit absolutely no background color
    fill, border stroke, or drop shadow at rest. The rectangular container
    bounding box becomes visible only upon hover, focus, or press.
-   **Contextual Placement & Anchoring**: Text buttons appear in highly
    predictable structural locations across UI layouts:
    -   *Dialogs & Modals*: Positioned at the bottom trailing action bar (e.g.,
        aligning right in LTR layouts) for confirming or dismissing actions
        (e.g., "Cancel", "Agree").
    -   *Cards*: Placed in bottom action rows to provide card-specific secondary
        actions without cluttering the card's visual hierarchy.
    -   *Snackbars & Banners*: Positioned at the trailing edge to offer timely
        dismissal or follow-up actions (e.g., "Undo", "Dismiss").
    -   *App Bars*: Used in top navigation headers for overarching page actions
        (e.g., "Back", "Done", "Save").
-   **Visual Cues & Contrast**: Because there is no resting container, text
    buttons rely on distinct brand or primary text colors (contrasting with
    standard body text), deliberate layout isolation, and consistent casing to
    communicate interactivity.
-   **Interactive DOM/Node Heuristics**: While the resting visual appearance is
    merely a text string, the underlying interactive touch target is a fully
    padded rectangular bounding box measuring at least 48x48dp. Agents scanning
    accessibility trees must look for this expanded touch target area.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion design system adherence audit, evaluate detected
text buttons against the following strict standards derived from Android Motion guidelines:

### Android Motion Visual & Behavioral Rules

-   **Label Typography & Casing**:
    -   Must utilize concise label text (1-3 words) written in **sentence case**
        (capitalizing only the first word and proper nouns, e.g., "Learn more",
        not "LEARN MORE", unless overriding for specific brand guidelines).
    -   Label text must never be truncated or wrapped to a second line; it must
        remain fully visible on a single line.
    -   *Typeface*: Button text uses the `'Google Sans Text'` family, defined by
        `--droid-sys-typescale-label-large` (`500 0.9rem/1.3rem 'Google Sans Text'`).
    -   *No Underlines*: Products must **never** underline text buttons.
        Underlines are strictly reserved for inline body text hyperlinks.
-   **Color Hierarchy & Contrast**:
    -   Text buttons represent the lowest emphasis in the Android Motion 5-level button
        hierarchy (below Outlined, Elevated, Tonal, and Filled).
    -   *Color Role*: Standard text buttons utilize the `--droid-sys-color-primary`
        (light-dark(`#0b57d0`, `#a8c7fa`)) color role for the text label. Because there is
        no container, the label text color must always maintain a minimum 3:1 contrast
        ratio against the background surface (`--droid-sys-color-background` or
        `--droid-sys-color-surface`) and be easily distinguishable from non-interactive body text.
    -   *Background Constraints*: Must be placed on clean, simple background
        surfaces. Products must avoid placing text buttons over visually
        prominent backgrounds such as full-bleed photographs or videos.
-   **Sizing & State Layers**:
    -   *Android/Web*: Default height is 40dp. Android Motion introduces multiple sizes
        (XS, S, M, L, XL). State layers (hover/focus/press highlights) render as
        a fully rounded pill-shaped container using `--droid-sys-shape-corner-full` behind the text,
        tinted with the label's primary color role.
    -   *iOS Platform*: Default height is 44pt (to meet Apple tap target requirements).
        Hover and focus states are unavailable. Labels use SF Pro 11pt as fallback.

### Critical Android Motion Violations to Flag

-   **Underlining Text Buttons**: Applying text underlines to a text button,
    violating Android Motion typography rules (which reserve underlines for inline hyperlinks).
-   **Text Wrapping or Truncation**: Wrapping text button labels to multiple
    lines or clipping them with ellipses.
-   **Insufficient Text Contrast**: Placing text buttons over complex
    photographic backgrounds or using low-contrast text colors that fail WCAG
    accessibility standards.
