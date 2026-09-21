# Icons - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Icons
(standalone, decorative, or interactive graphical symbols) across any design
system or platform, followed by Android Motion specifications for
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

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion adherence audit, evaluate detected
icons against the following strict standards derived from Android Motion guidelines:

### Android Motion Icon Classifications & Semantics

Android Motion categorizes icons into three distinct systemic and branded classifications, utilizing the unified icon font:

1.  **Google Symbols (System Icons)**:
    -   **Font Token**: Must be explicitly mapped to `--md-icon-font` ('Google Symbols').
    -   **Role**: Used for all universal system actions, tools, and navigation
        (e.g., close "X", search glass, settings gear).
    -   **Styling**: Monochromatic, clean, and designed for extreme legibility
        at small scales:
        -   Default **18dp** in chips, checkboxes, or selection controls.
        -   Default **24dp** in app bars, dialogs, and navigation drawers.
    -   **Color Theming**: Supports dynamic color theming utilizing `--droid-sys-color-primary`, `--droid-sys-color-on-surface`, or `--droid-sys-color-on-surface-variant` color roles depending on active state.
2.  **Product Icons**:
    -   **Role**: Branded symbols representing flagship products or services
        (e.g., Gmail, Drive, Maps).
    -   **Design Principles**: Must fit cohesively within Android's broader icon
        family. Utilizes the four brand colors (blue, red, yellow, green),
        embraces whitespace, and relies on clear metaphors.
    -   **Letter Restrictions**: Product icons must avoid incorporating letters
        or typography (with the sole historical exception of the Google Search 'G').
3.  **AI Feature & Product Icons**:
    -   **AI Feature Icons (Single Spark)**: Represent actions powered by
        generative AI. They feature a distinctive **single spark** vector to
        clearly signify GenAI capabilities, building user trust and transparency.
    -   **AI Product Icons**: Branded full-color lockups reserved exclusively
        for standalone AI-first flagship products (e.g., Gemini).
4.  **Motion & Morphing**:
    -   Interactive icons (such as hamburger-to-arrow or play-to-pause transitions) should animate smoothly using Vector Drawables (AVDs) with a duration of `--droid-sys-motion-duration-200` (200ms) or `--droid-sys-motion-duration-250` (250ms) and easing `--droid-sys-motion-easing-standard`.

### Critical Android Motion Violations to Flag

-   **Missing Standalone Graphic Mapping**: Conflating clickable icons entirely
    into `Icon button` without classifying the underlying `Icon` primitive
    during an audit.
-   **Typography in Product Icons**: Incorporating letters or words inside a
    newly designed product icon (violating brand guidelines).
-   **Improper AI Feature Branding**: Using generic stars or unbranded vectors
    instead of the official single-spark icon for generative AI features.
-   **Incorrect Font Association**: Hardcoding generic or non-system web fonts for system icons instead of utilizing the `--md-icon-font` token.
