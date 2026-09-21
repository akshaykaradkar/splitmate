# Text Buttons - Elements GM3 AI Detection Guide & Specifications

This reference provides visual heuristics for detecting Text Buttons (actionable interactive text elements lacking a container at rest) across the Elements GM3 design system, followed by concrete specifications for compliance auditing.

## Part 1: Elements GM3 AI Detection Heuristics

A **Text button** (also referred to as Button text) is a clean, low-emphasis interactive button that appears as standalone text without a visible container bounding box at rest. They are used for optional or tertiary actions with the least amount of prominence where visible containers would distract from nearby primary content. Agents must detect text buttons based on graphic isolation, casing, color contrast, and contextual placement, regardless of platform implementation.

### Key Visual & Geometric Heuristics

-   **Absence of Resting Container**: Look for isolated text strings that function as clickable buttons but exhibit absolutely no background color fill, border stroke, or drop shadow at rest. The rectangular container bounding box (state layer) becomes visible only upon hover, focus, or press.
-   **Contextual Placement & Anchoring**: Text buttons appear in highly predictable structural locations across UI layouts:
    -   *Dialogs & Modals*: Positioned at the bottom trailing or leading edge of action bars for secondary, non-unblocking or escaping actions (e.g., "Cancel", "Learn more").
    -   *Cards & Expansions*: Placed in bottom action rows or footers to provide card-specific tertiary actions without cluttering the card's visual hierarchy.
    -   *Snackbars & Banners*: Positioned at the trailing edge to offer timely dismissal or optional actions (e.g., "Undo", "Dismiss").
    -   *Search Bars & Filters*: Placed at the trailing edge of filter rows or inline forms for clearing inputs or opening advanced options.
-   **Visual Cues & Contrast**: Because there is no resting container, text buttons rely on distinct primary or brand text colors (contrasting with standard body text), deliberate layout isolation, and consistent casing to communicate interactivity.
-   **Interactive Touch Target Constraints**: While the resting visual appearance is merely a text string, the underlying interactive touch target is a fully padded rectangular bounding box measuring at least **48x48dp**. Agents scanning code or accessibility trees must look for this expanded target area.

---

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected text buttons against the following strict standards:

### Component Design Specs & Sizing

-   **Height & Dimensions**:
    -   *Default Height*: The visible text button height is strictly **40dp** with a **48dp minimum touch target size** to ensure touch targets meet accessibility standards.
    -   *Corner Radius*: The state layer container is a **fully rounded pill shape** (all corners rounded to 20dp or more).
-   **Typography & Styling**:
    -   *Scale & Font*: Text buttons utilize the **Label Large** baseline style: `500 0.9rem/1.3rem 'Google Sans Text'` (`var(--cee3-typescale-label-large-link)`).
    -   *Casing*: Label text must be written in **sentence case** (e.g., "Learn more", "Save changes", NOT ALL CAPS like "LEARN MORE" or "SAVE CHANGES"). This is a critical departure from GM2.
    -   *No Underlines*: Text buttons must **never** be underlined. Underlines are strictly reserved for inline body text hyperlinks.
    -   *No Truncation*: Text button labels must be short (1-3 words) and must never wrap to a second line or be truncated with ellipses.
-   **Iconography**:
    -   *Size*: Leading or trailing icons inside text buttons must be strictly **18x18dp** (using Material Symbols, `--md-icon-font: 'Google Symbols'`).
    -   *Placement*: Icons must appear to the left of the text label (or right for RTL languages).
    -   *Color*: The icon and text label must share the **exact same color** role (e.g., primary or brand color).
-   **Colors & Contrast**:
    -   *Deprecated Neutral Text Button*: The neutral grey text button is deprecated in Elements GM3. Text buttons must utilize the active `Primary` color role (`var(--cee3-sys-color-extended-link)` or `--md-sys-color-primary`) to communicate interactivity clearly.
    -   *Contrast*: The label text must meet a minimum **4.5:1 contrast ratio** against its resting background surface (as it is normal text below 18pt).
    -   *Background Constraints*: Text buttons must only be placed on clean, solid background surfaces (e.g., `surface` or `surface-container-low`). Avoid placing them over complex photographic or patterned backgrounds.
-   **Layout & Button Spacing**:
    -   *Spacing*: When multiple text buttons are placed horizontally adjacent (e.g., in dialog footers), they must have precisely **8px spacing** between them.

### Form Submission & Disabled States

-   **Form Submit Buttons Must Stay Enabled**:
    -   Per accessibility criteria (GAR), the submit/action button at the end of a form must always be in the **enabled state**, even if required fields are not yet filled or valid.
    -   If required fields are missing, clicking the enabled button should trigger inline error messaging below each invalid text field. This ensures keyboard and screen reader users can discover the submission button and recover from errors.
-   **State Hierarchy**:
    -   *Enabled*: Meets visual contrast standards; focusable and fully interactive.
    -   *"Soft" Disabled (Recommended over Hard)*: Does not meet full visual contrast standards but remains in the Tab order so screen readers can discover and focus it, often triggering a supporting tooltip explaining why the action is unavailable.
    -   *"Hard" Disabled (Avoid where possible)*: Completely removed from the Tab order (not focusable or discoverable by screen readers), and cannot trigger a tooltip.

### Critical Elements GM3 Violations to Flag

-   **Using ALL CAPS**: Formatting button text in all capital letters (e.g., "CANCEL"), which violates the sentence-case standard.
-   **Underlined Labels**: Underlining text button labels, which conflates them with inline hyperlinks.
-   **Incorrect Height or Touch Target**: Utilizing buttons smaller than 40dp height or failing to provide the 48dp minimum touch target.
-   **Applying Neutral Grey Text**: Using deprecated neutral text button styles instead of the primary color role.
-   **Hard Disabling Form Submit Buttons**: Disabling the primary form action button, preventing users from validating fields and recovering from errors.
