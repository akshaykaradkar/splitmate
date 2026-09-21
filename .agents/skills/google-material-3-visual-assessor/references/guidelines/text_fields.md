# Text Fields - Universal AI Detection Guide & MD3 Specifications

This reference provides universal visual heuristics for detecting Text Fields
(interactive form input containers) across any design system or platform,
followed by Material Design 3 (MD3) specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Text field** is an interactive form input container where users type, edit,
or select text (e.g., email inputs, password boxes, chat prompt bars, or form
entry rows). Agents must detect text fields based on container geometry,
floating label positions, and active focus/error state markers, regardless of
design system adherence.

### Key Visual & Geometric Heuristics

-   **Container Geometry & Bounding Box**: Look for elongated rectangular
    containers spanning form rows or docked at the bottom of chat screens:
    -   *Filled Variant*: Solid background color fill featuring a prominent
        horizontal bottom line indicator. Top corners are rounded; bottom
        corners are flat.
    -   *Outlined Variant*: Transparent background fill enclosed within a full
        perimeter border stroke with fully rounded corners.
-   **Label Placement (Resting vs. Floating)**: Look for text labels describing
    the input field:
    -   *Resting State*: When empty and unfocused, label text sits vertically
        centered inside the container.
    -   *Floating State*: On active focus or when text is entered, the label
        shrinks and floats to the top. In outlined fields, the floating label
        cuts directly through the top border stroke.
-   **State Cues (Focus & Error)**:
    -   *Focus*: Border stroke or bottom indicator line thickens and adopts the
        primary accent color. The on-screen keyboard is visible.
    -   *Error*: Container fill or border turns red. A mandatory red error icon
        appears at the trailing edge, and custom error text replaces supporting
        text below.
-   **Numeric Input Slots inside Cards**: In some designs, input fields for
    numeric or text values are embedded inside larger card-like containers
    alongside labels, icons, and units. In these cases:
    -   The outer bounding container MUST be mapped to **`Card`**.
    -   The specific text element displaying the value (e.g., "30", "250")
        functions as the interactive input slot and MUST be mapped to **`Text
        field`** (not just static `Text`).
-   **Control Elements & Mandatory Distinctions**: Look for trailing interactive
    controls (e.g., circular clear "X" icons, eye icons for password toggles, or
    dropdown chevrons).
    -   **Chat Prompt Inputs vs. Search Bars**: Agents MUST strictly distinguish
        between top-level search bars and conversational message/prompt inputs.
        An input container located at the bottom of a screen in messaging or
        conversational AI apps (e.g., Gemini) used to type prompts is a **`Text
        field`**, NOT a `Search bar`.

--------------------------------------------------------------------------------

## Part 2: MD3 Specifications & Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected
text fields against the following strict standards derived from Material
guidelines:

### MD3 Component Specifications & Sizing

-   **Dimensions & Sizing Tokens**:
    -   *Android/Web*: Default container height is **56dp**. Outlined fields
        feature a 4dp corner radius (`--md-sys-shape-corner-extra-small`). Filled fields feature rounded top corners (`--md-sys-shape-corner-extra-small-top` which is 4px) and square bottom corners.
    -   *iOS Platform*: Default container height is **44dp** (to align with
        Apple tap target standards). Outlined fields feature a **10dp** corner
        radius, with labels placed adjacent to the field rather than floating
        inside. Focused states use iOS primary blue.
-   **Internal Paddings & Icons**:
    -   *Paddings*: 16dp (`--md-sys-measurement-space200`) left/right outer padding; compresses to 12dp (`--md-sys-measurement-space150`) when
        leading/trailing icons are present.
    -   *Padding between Icon and Text*: 16dp (`--md-sys-measurement-space200`).
    -   *Icons*: 24dp assets. Leading icons indicate input type (e.g., search, voice);
        trailing icons handle state/secondary actions (e.g., clear, error, dropdown).
    -   *Supporting & Error Text*: Positioned 4dp (`--md-sys-measurement-space50`) below the container, providing helper
        instructions or error messages.
-   **Token Bindings & Colors**:
    -   **Resting Outline / Border Stroke**: `--md-sys-color-outline` (outlined variant) or `--md-sys-color-outline-variant` (filled bottom stroke).
    -   **Active Outline / Border Stroke**: `--md-sys-color-primary` (increases to 2dp thickness on focus).
    -   **Filled Background**: `--md-sys-color-surface-container` or `--md-sys-color-surface-variant` (resting), which provides visual contrast.
    -   **Error State Elements**: Container outline, text label, error icon, and supporting text must all adopt `--md-sys-color-error`.
    -   **Label & Input Text Color**: `--md-sys-color-on-surface-variant` (resting label) and `--md-sys-color-on-surface` (input text and floating label on focus).
    -   **Supporting Text Color**: `--md-sys-color-on-surface-variant`.
-   **Typography Mappings**:
    -   *Input Text*: `--md-sys-typescale-body-large` (or variable `--md-sys-typescale-variable-body-large`).
    -   *Resting Label*: `--md-sys-typescale-body-large`.
    -   *Floating Label*: `--md-sys-typescale-body-small` (or `--md-sys-typescale-label-small`).
    -   *Supporting & Error Text*: `--md-sys-typescale-body-small`.
-   **Required Field Formatting**: Scan for an asterisk (`*`) immediately
    following the label text to identify mandatory form fields. Both the text label and its screen reader accessibility label must contain the asterisk.

### Critical MD3 Violations to Flag

-   **Conflating Chat Inputs with Search Bars**: Misclassifying bottom-anchored
    conversational prompt input boxes as `Search bar` instead of `Text field`
    during an audit.
-   **Missing Error Icons**: Presenting a form error state without the mandatory
    trailing red error icon (`--md-sys-color-error`).
-   **Improper Container Heights**: Presenting standard text fields with
    compressed heights (<56dp on Android/Web or <44dp on iOS) that fail touch target accessibility.
-   **Mismatched Text Field Variants in Same Region**: Mixing filled and outlined text field variants within the same form section or region (they should be used consistently within sections).
-   **Truncated or Multi-line Labels**: Wrapping text field labels across multiple lines or truncating them with ellipses instead of keeping them concise and fully visible.
