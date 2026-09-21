# Static Text & Typography - Elements GM3 AI Detection Guide & Specifications

This reference provides visual heuristics for detecting static Text typography (displays, headlines, titles, body paragraphs, captions, and labels) across the Elements GM3 design system, followed by concrete specifications for compliance auditing.

## Part 1: Elements GM3 AI Detection Heuristics

**Text** represents pure, static, non-editable typographical strings displayed on a screen. It serves to establish information hierarchy, convey narrative or descriptive content, and label structural UI areas. Agents must detect static text based on character glyph rendering, typographic hierarchy, and strict negative constraints separating text from interactive containers, regardless of platform implementation.

### Key Visual & Typographic Heuristics

-   **Glyph Rendering & Typographic Hierarchy**: Look for rendered character vectors or font glyphs organized into distinct typographic roles based on visual scale, weight, and line height:
    -   *Display*: Large, high-emphasis text strings (Large: 57sp/64px line height, Medium: 45sp/52px line height, Small: 36sp/44px line height) reserved for short, important text or numerals on large viewports.
    -   *Headline*: Short, high-emphasis text strings (Large: 32sp/40px line height, Medium: 28sp/36px line height, Small: 24sp/32px line height) used to mark primary passages or important regions of content.
    -   *Title*: Medium-emphasis, short text strings (Large: 22sp/28px line height, Medium: 16sp/24px line height, Small: 14sp/20px line height) used to divide secondary passages or secondary regions of content.
    -   *Body Text*: Standard text blocks (Large: 16sp/24px line height, Medium: 14sp/20px line height, Small: 12sp/16px line height) used for longer descriptive paragraphs, multi-line passages, or detailed readouts.
    -   *Labels*: Small, utilitarian text strings (Large: 14sp/20px line height, Medium: 12sp/16px line height, Small: 11sp/16px line height) used for inside components (like button text) or supporting captions.
-   **Layout Alignment & Typesetting**: Text blocks align to established layout grids or bounding containers (start-aligned in LTR, center-aligned in hero lockups).
    -   *Tabular Numbers*: In tables, clocks, or financial widgets, numerical strings must utilize monospaced tabular figures rather than proportional digits to ensure vertical optical alignment during scanning.
-   **Mandatory Static Text Capture**: Agents MUST NOT omit static text elements during an audit. Every significant visible text string on a screen must be distinctively identified and mapped to `Text` in the final output.
-   **Critical Negative Constraints (Text vs. Interactive Controls)**: Agents MUST strictly distinguish between pure static text elements and interactive action containers housing text labels:
    -   **`Text` vs. `Button`, `Chip`, `Tab`, `Menu item`, `List item`**: If text is enclosed within an interactive bounding box or acts as a clickable touch target (even a text-only button lacking a visible container at rest), agents MUST map the surrounding bounding box to its specific interactive component type, NOT solely to `Text`.
    -   **`Text` vs. `Text field`**: An interactive form input container where a user types or edits text (e.g., email input, search box, chat prompt bar) MUST be mapped to `Text field` (category: `Text Input Components`). Agents must NEVER map an interactive input box to `Text`, as `Text` is strictly reserved for static typography.

---

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected static text against the following strict standards:

### Typography Scale & Font Themes

Elements GM3 establishes a rigid typographic scale and specific font theming rules for enterprise usability:

1.  **The 15 Baseline GM3 Type Scale**:
    -   Elements GM3 defines five core roles (`Display`, `Headline`, `Title`, `Body`, `Label`), each divided into three sizes (`Large`, `Medium`, `Small`).
    -   *Strict Baseline Standard*: Internal enterprise products must stick **exclusively** to the 15 Baseline type styles and combine them with bold format when needed. Emphasized type styles (featuring increased weight and roundedness) must be avoided in general layouts to prevent visual inconsistency.
2.  **Google Brand Font Themes (Static Theme Only)**:
    -   **Static Font Theme (Mandatory)**: Display, headline, and large title styles (sizes 18sp and up) utilize `Google Sans` (`var(--cee3-typescale-display-large-link)`, `var(--cee3-typescale-headline-large-link)`, etc.).
    -   **Google Sans Text (GST)**: For smaller sizes (17sp and below, including body, label, and title-small/medium styles) and longer blocks of text, products must use `Google Sans Text` (`var(--cee3-typescale-body-large-link)`, `var(--cee3-typescale-label-large-link)`, etc.) to improve readability.
    -   *No Variable Fonts*: Refrain from using variable fonts (such as Google Sans Flex) or custom axes in general enterprise apps to ensure visual cohesion.
3.  **Color Roles & Inline Hyperlinks**:
    -   *Neutral Text Roles*: Standard high-emphasis typography utilizes `On Surface` (`var(--cee3-sys-color-on-surface)`). Supporting, lower-emphasis, or muted text utilizes `On Surface Variant` (`var(--cee3-sys-color-on-surface-variant)`).
    -   *Outline Colors Forbidden*: Outline color roles (like `outline` or `outline variant`) must only be used for boundaries (borders, dividers) and never applied to text characters.
    -   *Inline Hyperlinks*: Hyperlinked text on a surface must utilize the `Primary` link color role (or `Tertiary` for secondary links). All clickable inline body links **must obligatorily be underlined** to ensure accessible identification.

### Spacing, Grid, and Accessibility (Contrast)

-   **The 4dp Typography Grid**:
    -   A text string's baseline must sit precisely on the **4dp grid**.
    -   To maintain the grid, line-height values must be divisible by 4 (e.g., 16px, 20px, 24px, 32px, 40px).
-   **WCAG Contrast Minimums (Level AA)**: Text must maintain sufficient contrast ratio against its background:
    -   **Large Text** (any text 14pt/sp bold or 18pt/sp regular/medium or larger, including Google Sans 16sp+ medium text): Minimum **3:1 contrast ratio**.
    -   **Normal Text** (less than 18pt/sp regular/medium, except GS 16sp+ medium): Minimum **4.5:1 contrast ratio**.
    -   **State/Placeholder Contrast**: A minimum **4.5:1 contrast ratio** is required for all body/label text including placeholder text and hover/focus states.
-   **Text Resizing & Spacing Compliance**:
    -   **The 200% Rule**: Web interfaces must support text resizing up to 200% without clipping, overlapping, or disappearing.
    -   **Spacing Resiliency**: Elements and text containers must not break when users override paragraph, line, word, or letter spacing. Desired solutions in order of preference:
        1. Allow containers to expand in width/height.
        2. Allow text to wrap to multiple lines.
        3. If fixed-width constraints require truncation, display a focusable interactive tooltip containing the full text.
-   **Line Length (Character Count)**:
    -   Body lines are most comfortable between **40 to 60 characters** (maximum of 80 characters for WCAG AAA compliance). Wider lines (up to 120 characters) require increasing line height (e.g., from 20px to 24px) to preserve readability.
-   **No Justified Text**: Alignment must never be justified, as justified text creates inconsistent letter spacing that disrupts word recognition.

### Critical Elements GM3 Violations to Flag

-   **Conflating Text Fields or Buttons with Static Text**: Misclassifying interactive input boxes (`Text field`) or clickable buttons (`Button`) as static `Text` during an audit.
-   **Low Text Contrast**: Presenting body text or labels with a contrast ratio below 4.5:1 (or large headlines below 3:1) against background surfaces.
-   **Un-underlined Hyperlinks**: Presenting clickable inline body links without an underline, violating accessibility and Material typography standards.
-   **Using Outline Colors for Text**: Setting text character colors to `outline` or `outline-variant`, leading to severe contrast violations.
-   **Rigid Height Truncation/Clipping**: Placing text in fixed-height containers that clip characters or overlap when scaled to 200% or when custom spacing is applied.
