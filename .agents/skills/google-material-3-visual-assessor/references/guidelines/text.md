# Static Text \& Typography - Universal AI Detection Guide \& MD3 Specifications

This reference provides universal visual heuristics for detecting static Text
typography (titles, headlines, body paragraphs, captions, and labels) across any
design system or platform, followed by Material Design 3 (MD3) specifications
for compliance auditing.

## Part 1: Universal AI Detection Heuristics

**Text** represents pure, static, non-editable typographical strings displayed
on a screen. It serves to establish information hierarchy, convey narrative or
descriptive content, and label structural UI areas. Agents must detect static
text based on character glyph rendering, typographic hierarchy, and strict
negative constraints separating text from interactive containers, regardless of
design system adherence.

### Key Visual \& Typographic Heuristics

-   **Glyph Rendering \& Typographic Hierarchy**: Look for rendered character
    vectors or font glyphs organized into distinct typographic roles based on
    visual scale, weight, and line height:
    -   *Display \& Headlines*: Large, high-emphasis text strings (typically
        24pt/dp to 57pt/dp) reserved for short hero statements, page titles, or
        prominent numerical callouts.
    -   *Titles*: Medium-emphasis text strings (typically 16pt/dp to 22pt/dp)
        dividing secondary passages or marking regional containers.
    -   *Body Text*: Standard text blocks (typically 14pt/dp to 16pt/dp) used
        for longer descriptive paragraphs or multi-line passages.
    -   *Labels \& Captions*: Small, utilitarian text strings (typically 11pt/dp
        to 14pt/dp) used for metadata, timestamps, or supporting captions.
-   **Layout Alignment \& Typesetting**: Text blocks align to established layout
    grids or bounding containers (start-aligned in LTR, center-aligned in hero
    lockups, or justified).
    -   *Tabular Numbers*: In tables, clocks, or financial widgets, numerical
        strings utilize monospaced tabular figures to ensure vertical optical
        alignment during scanning.
-   **Mandatory Static Text Capture**: Agents MUST NOT omit static text elements
    during an audit. Every significant visible text string on a screen must be
    distinctively identified and mapped to `Text` (category: `Primitives` or
    `Content`) in the final output.
-   **Critical Negative Constraints (Text vs. Interactive Controls)**: Agents
    MUST strictly distinguish between pure static text elements and interactive
    action containers housing text labels:
    -   **`Text` vs. `Button`, `Chip`, `Tab`, `Menu item`, `List item`**: If
        text is enclosed within an interactive bounding box or acts as a
        clickable touch target (even a `Text button` lacking a visible container
        at rest), agents MUST map the surrounding bounding box to its specific
        interactive component type (e.g., `Button`, `Chip`, `Tab`), NOT solely
        to `Text`.
    -   **`Text` vs. `Text field`**: An interactive form input container where a
        user types or edits text (e.g., email input, search box, chat prompt
        bar) MUST be mapped to `Text field` (category: `Text Input Components`).
        Agents must NEVER map an interactive input box to `Text`, as `Text` is
        strictly reserved for static typography.

--------------------------------------------------------------------------------

## Part 2: MD3 Specifications \& Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected
static text against the following strict standards derived from Material
guidelines and the design system's css variables:

### MD3 Typography Scale \& Font Themes

Material Design 3 establishes a rigid 30-style type scale and specific font
theming rules. The static type scale utilizes `Google Sans` for display/headlines/titles and `Google Sans Text` for body/labels. The variable type scale leverages `Google Sans Flex` (GSF) to offer fluid adjustments.

#### 1. The Standard \& Emphasized GM3 Type Scales

Below are the official css variables representing the baseline and emphasized typographic style scales of Google Material 3:

|Typographic Role \& Size|Baseline Style Token|Emphasized Style Token|CSS Properties \& Font Family|
|:---|:---|:---|:---|
|**Display Large**|`var(--md-sys-typescale-display-large)`|`var(--md-sys-typescale-emphasized-display-large)`|`3.6rem/4rem 'Google Sans'` (Emphasized: `500 3.6rem/4rem`)|
|**Display Medium**|`var(--md-sys-typescale-display-medium)`|`var(--md-sys-typescale-emphasized-display-medium)`|`2.8rem/3.3rem 'Google Sans'` (Emphasized: `500 2.8rem/3.3rem`)|
|**Display Small**|`var(--md-sys-typescale-display-small)`|`var(--md-sys-typescale-emphasized-display-small)`|`2.3rem/2.8rem 'Google Sans'` (Emphasized: `500 2.3rem/2.8rem`)|
|**Headline Large**|`var(--md-sys-typescale-headline-large)`|`var(--md-sys-typescale-emphasized-headline-large)`|`2rem/2.5rem 'Google Sans'` (Emphasized: `500 2rem/2.5rem`)|
|**Headline Medium**|`var(--md-sys-typescale-headline-medium)`|`var(--md-sys-typescale-emphasized-headline-medium)`|`1.8rem/2.3rem 'Google Sans'` (Emphasized: `500 1.8rem/2.3rem`)|
|**Headline Small**|`var(--md-sys-typescale-headline-small)`|`var(--md-sys-typescale-emphasized-headline-small)`|`1.5rem/2rem 'Google Sans'` (Emphasized: `500 1.5rem/2rem`)|
|**Title Large**|`var(--md-sys-typescale-title-large)`|`var(--md-sys-typescale-emphasized-title-large)`|`1.4rem/1.8rem 'Google Sans'` (Emphasized: `500 1.4rem/1.8rem`)|
|**Title Medium**|`var(--md-sys-typescale-title-medium)`|`var(--md-sys-typescale-emphasized-title-medium)`|`500 1rem/1.5rem 'Google Sans Text'` (Emphasized: `700 1rem/1.5rem`)|
|**Title Small**|`var(--md-sys-typescale-title-small)`|`var(--md-sys-typescale-emphasized-title-small)`|`500 0.9rem/1.3rem 'Google Sans Text'` (Emphasized: `700 0.9rem/1.3rem`)|
|**Body Large**|`var(--md-sys-typescale-body-large)`|`var(--md-sys-typescale-emphasized-body-large)`|`1rem/1.5rem 'Google Sans Text'` (Emphasized: `500 1rem/1.5rem`)|
|**Body Medium**|`var(--md-sys-typescale-body-medium)`|`var(--md-sys-typescale-emphasized-body-medium)`|`0.9rem/1.3rem 'Google Sans Text'` (Emphasized: `500 0.9rem/1.3rem`)|
|**Body Small**|`var(--md-sys-typescale-body-small)`|`var(--md-sys-typescale-emphasized-body-small)`|`0.8rem/1rem 'Google Sans Text'` (Emphasized: `500 0.8rem/1rem`)|
|**Label Large**|`var(--md-sys-typescale-label-large)`|`var(--md-sys-typescale-emphasized-label-large)`|`500 0.9rem/1.3rem 'Google Sans Text'` (Emphasized: `700 0.9rem/1.3rem`)|
|**Label Medium**|`var(--md-sys-typescale-label-medium)`|`var(--md-sys-typescale-emphasized-label-medium)`|`500 0.8rem/1rem 'Google Sans Text'` (Emphasized: `700 0.8rem/1rem`)|
|**Label Small**|`var(--md-sys-typescale-label-small)`|`var(--md-sys-typescale-emphasized-label-small)`|`500 0.7rem/1rem 'Google Sans Text'` (Emphasized: `700 0.7rem/1rem`)|

#### 2. Variable Flex Font Themes (Opt-In Upgrade)

When using dynamic fluid typography, the style scale maps to variable `Google Sans Flex` (`GSF`) tokens. This allows fine-tuned control over custom axes (Grade `GRAD`, Roundness `ROND`, Optical Size `opsz`, Slant `slnt`, Width `wdth`, Weight `wght`, Cursive `CRSV`):

- `var(--md-sys-typescale-variable-body-large)` (Axes: `var(--md-sys-typescale-variable-body-large-axes)`)
- `var(--md-sys-typescale-variable-emphasized-body-large)` (Axes: `var(--md-sys-typescale-variable-emphasized-body-large-axes)`)
- Same patterns apply for display, headline, title, body, and label roles.

#### 3. Color Roles \& Inline Hyperlinks

-   **Primary Body Text**: Standard typography uses `var(--md-sys-color-on-surface)` (`light-dark(#1f1f1f, #e3e3e3)`).
-   **Secondary / Supporting Text**: Subdued strings use `var(--md-sys-color-on-surface-variant)` (`light-dark(#444746, #c4c7c5)`).
-   **Inline Hyperlinks**: Hyperlinked body text must utilize `var(--md-sys-color-primary)` (`light-dark(#0b57d0, #a8c7fa)`) (or `var(--md-sys-color-tertiary)` for secondary hierarchies). Hyperlinked text **must obligatorily be underlined** to ensure accessible identification.

### MD3 Typesetting \& Accessibility (Contrast)

-   **Typesetting Baselines vs. Bounding Boxes**:
    -   *Web / iOS*: Utilizes bounding box half-leading CSS height centering.
    -   *Android*: Measures vertical distances directly from the invisible text baseline.
-   **WCAG Contrast Minimums**: Text must maintain sufficient contrast ratio against its background surface:
    -   **Large Text** (Display / Headline / Title Large): Minimum **3:1 contrast ratio**.
    -   **Small Text** (Body / Label / Title Medium \& Small): Minimum **4.5:1 contrast ratio**.
-   **Text Scaling (200% Rule)**: Layouts must support dynamic text resizing up to 200% without clipping, overlapping, or excessive truncation (e.g., dialog headlines must fit within 4 lines at 200% scale).

### Critical MD3 Violations to Flag

-   **Conflating Text Fields or Buttons with Static Text**: Misclassifying interactive input boxes (`Text field`) or clickable buttons as static `Text` during an audit.
-   **Low Text Contrast**: Presenting body text or labels with a contrast ratio below 4.5:1 (or headlines below 3:1) against background surfaces.
-   **Un-underlined Hyperlinks**: Presenting clickable inline body links without an underline, violating accessibility and Material typography standards.
-   **Text Overlaps at 200% Scale**: Designing rigid layouts or containers where text strings collide, clip, or overlap when scaled to 200% by assistive technology.
-   **Incorrect Font Association**: Using incorrect font families (e.g. using a generic system serif font instead of the Google Sans system family `Google Sans` or `Google Sans Text`/`Google Sans Flex` for the corresponding scale categories).
