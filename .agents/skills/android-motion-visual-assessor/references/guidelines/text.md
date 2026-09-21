# Static Text & Typography - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting static Text typography (titles, headlines, body paragraphs, captions, and labels) across any design system or platform, followed by Android Motion design system specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

**Text** represents pure, static, non-editable typographical strings displayed on a screen. It serves to establish information hierarchy, convey narrative or descriptive content, and label structural UI areas. Agents must detect static text based on character glyph rendering, typographic hierarchy, and strict negative constraints separating text from interactive containers, regardless of design system adherence.

### Key Visual & Typographic Heuristics

-   **Glyph Rendering & Typographic Hierarchy**: Look for rendered character vectors or font glyphs organized into distinct typographic roles based on visual scale, weight, and line height:
    -   *Display & Headlines*: Large, high-emphasis text strings (typically 24pt/dp to 57pt/dp) reserved for short hero statements, page titles, or prominent numerical callouts.
    -   *Titles*: Medium-emphasis text strings (typically 16pt/dp to 22pt/dp) dividing secondary passages or marking regional containers.
    -   *Body Text*: Standard text blocks (typically 14pt/dp to 16pt/dp) used for longer descriptive paragraphs or multi-line passages.
    -   *Labels & Captions*: Small, utilitarian text strings (typically 11pt/dp to 14pt/dp) used for metadata, timestamps, or supporting captions.
-   **Layout Alignment & Typesetting**: Text blocks align to established layout grids or bounding containers (start-aligned in LTR, center-aligned in hero lockups, or justified).
    -   *Tabular Numbers*: In tables, clocks, or financial widgets, numerical strings utilize monospaced tabular figures to ensure vertical optical alignment during scanning.
-   **Mandatory Static Text Capture**: Agents MUST NOT omit static text elements during an audit. Every significant visible text string on a screen must be distinctively identified and mapped to `Text` (category: `Primitives` or `Content`) in the final output.
-   **Critical Negative Constraints (Text vs. Interactive Controls)**: Agents MUST strictly distinguish between pure static text elements and interactive action containers housing text labels:
    -   **`Text` vs. `Button`, `Chip`, `Tab`, `Menu item`, `List item`**: If text is enclosed within an interactive bounding box or acts as a clickable touch target (even a `Text button` lacking a visible container at rest), agents MUST map the surrounding bounding box to its specific interactive component type (e.g., `Button`, `Chip`, `Tab`), NOT solely to `Text`.
    -   **`Text` vs. `Text field`**: An interactive form input container where a user types or edits text (e.g., email input, search box, chat prompt bar) MUST be mapped to `Text field` (category: `Text Input Components`). Agents must NEVER map an interactive input box to `Text`, as `Text` is strictly reserved for static typography.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion design system adherence audit, evaluate detected static text against the following strict standards derived from Android Motion guidelines:

### Android Motion Typography Scale & Font Themes

The Android Motion design system establishes a rigid 15-style type scale and specific font theming rules:

1.  **The 15-Style Android Motion Type Scale**:
    Android Motion defines five core roles (`Display`, `Headline`, `Title`, `Body`, `Label`), each divided into three sizes (`Large`, `Medium`, `Small`):
    -   **Display Large**: `3.6rem/4rem 'Google Sans'`
    -   **Display Medium**: `2.8rem/3.3rem 'Google Sans'`
    -   **Display Small**: `2.3rem/2.8rem 'Google Sans'`
    -   **Headline Large**: `2rem/2.5rem 'Google Sans'`
    -   **Headline Medium**: `1.8rem/2.3rem 'Google Sans'`
    -   **Headline Small**: `1.5rem/2rem 'Google Sans'`
    -   **Title Large**: `1.4rem/1.8rem 'Google Sans'`
    -   **Title Medium**: `500 1rem/1.5rem 'Google Sans Text'`
    -   **Title Small**: `500 0.9rem/1.3rem 'Google Sans Text'`
    -   **Body Large**: `1rem/1.5rem 'Google Sans Text'`
    -   **Body Medium**: `0.9rem/1.3rem 'Google Sans Text'`
    -   **Body Small**: `0.8rem/1rem 'Google Sans Text'` (with Tracking: `0`)
    -   **Label Large**: `500 0.9rem/1.3rem 'Google Sans Text'`
    -   **Label Medium**: `500 0.8rem/1rem 'Google Sans Text'` (with Tracking: `0`)
    -   **Label Small**: `500 0.7rem/1rem 'Google Sans Text'` (with Tracking: `0`)

2.  **Font Themes**:
    -   **Display, Headline, and Title Large** styles strictly utilize the `'Google Sans'` font family.
    -   **Title Medium, Title Small, Body, and Label** styles strictly utilize the `'Google Sans Text'` font family.
    -   Approximations or fallbacks (like Arial, Helvetica, standard Roboto, or sans-serif) that do not bind to these Google Sans font families do not comply with the design system.

3.  **Color Roles & Inline Hyperlinks**:
    -   *Default Text*: Standard body and heading typography utilizes the `--droid-sys-color-on-background` (Light `#1f1f1f` / Dark `#e3e3e3`) or `--droid-sys-color-on-surface` (Light `#1f1f1f` / Dark `#e3e3e3`) color role.
    -   *Supporting/Muted Text*: Captions, metadata, and placeholder elements utilize the `--droid-sys-color-on-surface-variant` (Light `#444746` / Dark `#c4c7c5`) color role.
    -   *Inline Hyperlinks*: Hyperlinked text appearing over a surface must utilize the `--droid-sys-color-primary` (Light `#0b57d0` / Dark `#a8c7fa`) or `--droid-sys-color-tertiary` (Light `#146c2e` / Dark `#6dd58c`) color role. Hyperlinked text **must obligatorily be underlined** to ensure accessible visual identification.

### Android Motion Typesetting & Accessibility (Contrast)

-   **Typesetting & Baseline Alignment**:
    -   All web layouts utilize bounding box half-leading CSS height centering.
    -   Android implementations measure vertical distances directly from the invisible text baseline.
-   **WCAG Contrast Minimums**: Text must maintain a high-contrast ratio against its background surface/background:
    -   **Large Text** (Display & Headline styles): Minimum **3:1 contrast ratio**.
    -   **Small Text** (Title, Body, & Label styles): Minimum **4.5:1 contrast ratio**.
-   **Text Scaling (200% Rule)**: Layouts must support dynamic text resizing up to 200% without clipping, overlapping, or excessive truncation (e.g., dialog titles must remain fully legible and fit within wrapping boundaries).

### Critical Android Motion Violations to Flag

-   **Conflating Text Fields or Buttons with Static Text**: Misclassifying interactive input boxes (`Text field`) or clickable buttons as static `Text` during an audit.
-   **Incorrect Font Family**: Using standard system fallback fonts instead of `'Google Sans'` (for display/headline/title-large) and `'Google Sans Text'` (for other styles) as specified in `--droid-sys-typescale-*`.
-   **Low Text Contrast**: Presenting body text or labels with a contrast ratio below 4.5:1 (or display/headlines below 3:1) against background surfaces.
-   **Un-underlined Hyperlinks**: Presenting clickable inline body links without an underline, violating accessibility and Android Motion typography standards.
-   **Text Overlaps at 200% Scale**: Designing rigid containers where text strings collide, clip, or overlap when scaled to 200% by assistive technology.
