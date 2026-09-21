# Icons - Universal AI Detection Guide & MD3 Specifications

This reference provides universal visual heuristics for detecting Icons
(standalone, decorative, or interactive graphical symbols) across any design
system or platform, followed by Material Design 3 (MD3) specifications for
compliance auditing.

## Part 1: Universal AI Detection Heuristics

An **Icon** is a compact graphical vector or symbol used to visually communicate
concepts, tools, actions, or product branding. Agents must detect icons based on
their vector geometry, graphic isolation, and structural context, regardless of
design system adherence.

### Key Visual & Geometric Heuristics

-   **Vector Geometry & Isolation**: Look for isolated, monochromatic or branded
    vector graphics (e.g., silhouettes, glyphs, or outline symbols) that convey
    meaning without relying on adjacent text. They appear at small visual scales
    (typically 18dp to 24dp).
-   **Placement & Structural Context**: Icons appear in predictable locations
    across UI layouts:
    -   *App Bars & Toolbars*: Navigation cues (hamburger menu, back arrow) and
        global actions (magnifying glass search, three-dots overflow).
    -   *Buttons & Chips*: Embedded as leading or trailing visual anchors
        alongside text labels.
    -   *Lists & Tables*: Positioned in leading slots to indicate row
        functionality or trailing slots (chevrons) to indicate navigation.
-   **Mandatory Dual Mapping (Icon Graphic + Interactive Target)**: When
    analyzing screens featuring clickable icons, agents MUST NOT map the element
    solely to `Icon button`. Downstream evaluation systems assert the presence
    of underlying `Icon` components. Agents MUST explicitly output BOTH
    components as separate objects:
    -   **Standalone Icon Graphic (`Icon`) [MANDATORY]**: Exclusively bound the
        visual icon vector itself and classify it as `Icon` (category:
        `Primitives` or `Content`).
    -   **Interactive Target (`Icon button`)**: Bound the surrounding clickable
        touch target area or outer action container and classify it as `Icon
        button`.
    -   *Non-Clickable Icons*: If an icon is purely informational or decorative
        (e.g., a calendar graphic inside a card header), classify it solely as
        `Icon`.

--------------------------------------------------------------------------------

## Part 2: MD3 Specifications & Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected
icons against the following strict standards derived from Material guidelines:

### MD3 Icon Classifications & Semantics

Material Design 3 categorizes icons into three distinct systemic and branded
classifications:

1.  **Google Symbols (System Icons)**:
    -   **Role**: Used for all universal system actions, tools, and navigation
        (e.g., close "X", search glass, settings gear).
    -   **Font Family**: Must strictly use the Google Symbols font, defined by `--md-icon-font: 'Google Symbols';`.
    -   **Sizing Rules**:
        -   Default size inside check boxes, radio buttons, and chips is **18dp**.
        -   Default size inside app bars, dialogs, buttons, list items, and navigation rails is **24dp**.
    -   **Color and Theming**: Supports dynamic color theming. Must utilize valid MD3 color tokens based on emphasis and state:
        -   Primary actions: `--md-sys-color-primary` (or `--md-sys-color-on-primary` / `--md-sys-color-on-primary-container` when inside containers).
        -   Neutral body/standard icons: `--md-sys-color-on-surface` or `--md-sys-color-on-surface-variant`.
        -   Semantic errors: `--md-sys-color-error`.
2.  **Product Icons**:
    -   **Role**: Branded symbols representing flagship products or services
        (e.g., Gmail, Drive, Maps).
    -   **Design Principles**: Must fit cohesively within Google's broader icon
        family. Utilizes the four Google brand colors (blue, red, yellow,
        green), embraces whitespace, and relies on clear metaphors.
    -   **Letter Restrictions**: Product icons must avoid incorporating letters
        or typography (with the sole historical exception of the Google Search
        'G').
3.  **AI Feature & Product Icons**:
    -   **AI Feature Icons (Single Spark)**: Represent actions powered by
        generative AI. They feature a distinctive **single spark** vector (such as the spark symbol from Google Symbols) to clearly signify GenAI capabilities, building user trust and transparency. They can utilize specialized dynamic gradients or primary branding accents (like `--md-ref-palette-purple50` or similar purple/blue variants) to separate them from standard system actions.
    -   **AI Product Icons**: Branded full-color lockups reserved exclusively
        for standalone AI-first flagship products (e.g., Gemini).

### Critical MD3 Violations to Flag

-   **Missing Standalone Graphic Mapping**: Conflating clickable icons entirely
    into `Icon button` without classifying the underlying `Icon` primitive
    during an audit.
-   **Typography in Product Icons**: Incorporating letters or words inside a
    newly designed product icon (violating Google brand guidelines).
-   **Improper AI Feature Branding**: Using generic multi-stars or unbranded star vectors
    instead of the official single-spark icon for generative AI features.
-   **Inconsistent Sizing**: Deviating from the mandatory 18dp size inside chips/checkboxes, or 24dp size inside app bars/buttons/navigation.
-   **Hardcoded Non-Themed Colors**: Using hardcoded color values instead of the official dynamic color roles (`--md-sys-color-*`) for system icons.
