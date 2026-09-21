# Text Buttons - Universal AI Detection Guide & MD3 Specifications

This reference provides universal visual heuristics for detecting Text Buttons
(actionable interactive text elements lacking a container at rest) across any
design system or platform, followed by Material Design 3 (MD3) specifications
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

## Part 2: MD3 Specifications & Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected
text buttons against the following strict standards derived from Material
guidelines and design system tokens:

### MD3 Visual & Behavioral Rules

-   **Label Typography & Casing**:
    -   Must utilize concise label text (1-3 words) written in **sentence case**
        (capitalizing only the first word and proper nouns, e.g., "Learn more",
        not "LEARN MORE" or "Learn More").
    -   Typography role must use `var(--md-sys-typescale-label-large)` (`500 0.9rem/1.3rem 'Google Sans Text'`) or the expressive emphasized variant `var(--md-sys-typescale-emphasized-label-large)` (`700 0.9rem/1.3rem 'Google Sans Text'`).
    -   Label text must never be truncated or wrapped to a second line; it must
        remain fully visible on a single line.
    -   *No Underlines*: Products must **never** underline text buttons.
        Underlines are strictly reserved for inline body text hyperlinks.
-   **Color Hierarchy & Contrast**:
    -   Text buttons represent the lowest emphasis in Material's 5-level button
        hierarchy (below Outlined, Elevated, Tonal, and Filled).
    -   *Color Role*: Standard text buttons utilize the `var(--md-sys-color-primary)` (`light-dark(#0b57d0, #a8c7fa)`) color role for the text label. For destructive or warning actions, text buttons may use the `var(--md-sys-color-error)` (`light-dark(#b3261e, #f2b8b5)`) color role.
    -   *Contrast*: Because there is no resting container, the label text color must
        always maintain a minimum 3:1 contrast ratio against the background and
        be easily distinguishable from non-interactive body text (`var(--md-sys-color-on-surface)`).
    -   *Background Constraints*: Must be placed on clean, simple background
        surfaces. Products must avoid placing text buttons over visually
        prominent backgrounds such as full-bleed photographs or videos.
-   **Sizing & State Layers**:
    -   **Android & Web**:
        -   Default resting height is 40dp (`var(--md-sys-measurement-space500)`).
        -   The state layer (which renders on hover, focus, or press highlights) is styled as a fully rounded pill-shaped container using shape token `var(--md-sys-shape-corner-full)` (`max(50cqw, 50cqh)`).
        -   Internal padding on the left and right must be at least `var(--md-sys-measurement-space150)` (12px), or `var(--md-sys-measurement-space100)` (8px) when an icon is present.
    -   **iOS Platform**:
        -   Default height is 44pt (to meet Apple tap target requirements).
        -   Hover and focus states are unavailable. Labels use `SF Pro Text 11pt` or `var(--md-sys-typescale-label-large)` depending on system bridges.
-   **Touch Target**:
    -   An invisible, fully interactive bounding box measuring at least 48x48dp (`var(--md-sys-measurement-space600)`) must wrap the text button to ensure accessible activation.

| Property | Standard MD3 Token Binding | Default Concrete Value | Rationale & Rules |
| :--- | :--- | :--- | :--- |
| **Typography Scale** | `var(--md-sys-typescale-label-large)` | `500 0.9rem/1.3rem 'Google Sans Text'` | Sentence-cased, single-line text; no underlines. |
| **Label Color (Primary)** | `var(--md-sys-color-primary)` | `light-dark(#0b57d0, #a8c7fa)` | High contrast, distinct from standard body text. |
| **Label Color (Destructive)** | `var(--md-sys-color-error)` | `light-dark(#b3261e, #f2b8b5)` | Used strictly for dangerous or critical dismissals. |
| **State Layer Shape** | `var(--md-sys-shape-corner-full)` | `max(50cqw, 50cqh)` | Fully rounded pill-shaped container on hover/focus. |
| **Minimum Height** | `var(--md-sys-measurement-space500)` | `40px` (or 40dp) | 44pt on iOS; ensures alignment across grids. |
| **Minimum Touch Target** | `var(--md-sys-measurement-space600)` | `48px` (or 48dp) | Accessible tap target surrounding the text. |

### Critical MD3 Violations to Flag

-   **Underlining Text Buttons**: Applying text underlines to a text button,
    violating Material typography rules (which reserve underlines for inline
    hyperlinks).
-   **Text Wrapping or Truncation**: Wrapping text button labels to multiple
    lines or clipping them with ellipses instead of adjusting adjacent margins or container widths.
-   **Insufficient Text Contrast**: Placing text buttons over complex or high-contrast
    photographic backgrounds, or using low-contrast text colors that fail WCAG
    accessibility standards.
-   **Incorrect Case / ALL CAPS**: Styling labels in uppercase (e.g. "CANCEL") instead of the mandatory sentence case (e.g. "Cancel") unless specifically required by strict brand guidelines.
-   **Sharp Hover States**: State overlay layers that exhibit sharp corners (e.g., 0px or 4px) instead of the required fully rounded pill shape (`var(--md-sys-shape-corner-full)`).
