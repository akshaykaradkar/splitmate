# Progress & Loading Indicators - Universal AI Detection Guide & Google Material 3 Specifications

This reference provides universal visual heuristics for detecting Progress Indicators (linear bars or circular spinning arcs), Loading Indicators (shape-morphing sequences), and AI Processing/Generating Indicators communicating ongoing process states across any design system or platform, followed by Google Material 3 (GM3) specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

**Progress indicators** and **Loading indicators** are dynamic visual components used to communicate the real-time status of an ongoing process (e.g., loading an app, submitting a form, or fetching data). Unlike static icons, they capture user attention through motion, active track fills, shape morphing, or conversational status text. Agents must detect these indicators based on their geometry, track division, morphing sequences, and contextual placement, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Linear Indicators (Horizontal Bars)**: Look for a distinct horizontal bar split into two contrasting visual segments: an active filled track (representing progress made) and an inactive background track (representing remaining progress).
    -   *Stop Marker*: A tiny circular dot marking the ending tip of the active colored segment in determinate states.
    -   *Expressive/Wavy Style*: An undulating or wavy line rather than a straight rectangular bar.
    -   *Discrete/Dotted Status Tracks*: A horizontal row of discrete elements (such as dots, blocks, or segments) styled in a color gradient or solid color, paired with a visual marker (like an elongated dot, arrow, or pin) indicating the current value along the scale. Map these custom status tracks to **`Progress indicator`**.
    -   *Placement*: Typically anchored flush to the extreme top edge of a card, dialog, sheet, or page container.
    -   *Media/Playback Integration*: Thin horizontal line (often blue, red, or brand-colored) positioned at the bottom of a card or media player to show playback progress.
    -   *Button Integration*: Embedded directly inside interactive buttons (e.g., a thin horizontal progress line at the bottom of a button).
-   **Circular Indicators (Spinning Rings/Arcs)**: Look for a thin circular arc, spinning ring, or expanding circular track.
    -   *Button Integration*: Frequently embedded directly inside interactive buttons (often replacing the leading icon or text label); typically rendered as a monochrome arc matching the text color without an inactive background track.
    -   *Placement*: Centered prominently within empty layout spaces, cards, or surfaces where content is actively loading.
-   **Loading Indicators (Morphing Shape Sequences)**: Look for a looping, dynamic animation sequence displaying morphing geometric shapes (e.g., cycling through distinct polygon or curved bounding contours).
    -   *Pull-to-Refresh*: Commonly positioned at the very top of a scrollable list or grid, frequently housed inside an elevated circular or pill-shaped background container when overlapping content.
-   **Determinate vs. Indeterminate Signatures**: Look for two distinct behavioral states:
    -   *Determinate (Linear/Circular)*: The active colored track grows steadily in one direction, reflecting a known, measurable completion percentage.
    -   *Indeterminate (Linear/Circular/Loading)*: The active colored segment repeatedly grows, shrinks, oscillates, or morphs shapes, communicating an unknown wait time.
-   **AI Processing & Generating Indicators**: In conversational or generative AI contexts:
    -   *AI Processing (Thinking Dots & Status Text)*: Look for three horizontal dots in motion (thinking dots step) followed by concise status text (e.g., "Searching...", "Drafting...") with optional source icons. Often paired with glowing gradients or decorative energy paths.
    -   *AI Generating (Placeholder Container)*: A container representing the shape and size of media content being created (images/videos) with a fluid, moving gradient path along its borders or background.

---

## Part 2: Google Material 3 Specifications & Compliance Auditing

When conducting a Google Material 3 (GM3) adherence audit, evaluate detected indicators against the following strict standards derived from GM3 and GM3 Expressive guidelines:

### GM3 Component Specifications & Sizing

1.  **Linear Progress Indicator Specs**:
    -   **Minimum Width**: Strictly 40dp minimum width.
    -   **Stop Indicator**: Complying with the December 2023 Non-Text Contrast (NTC) update, a mandatory 4dp circular end stop indicator must be present at the trailing tip of linear determinate tracks to improve contrast and accessibility. This is required if track contrast against the background is < 3:1.
    -   **Track Thickness**: Defaults to 4dp. GM3 Expressive (Aug 2024) introduces configurable track thickness and wavy shape options.
    -   **RTL Mirroring**: Linear indicators must be mirrored horizontally for right-to-left languages.
    -   **Tokens**: Uses `--md-sys-color-primary` for the active track and `--md-sys-color-surface-variant` (or `--md-sys-color-outline-variant` / `--md-sys-color-surface-container`) for the inactive track.

2.  **Circular Progress Indicator Specs**:
    -   **Size Range**: Variable diameter ranging from 24dp (embedded inside small buttons) up to 240dp (large hero loading moments).
    -   **Button Integration**: When embedded inside a button, the inactive track must be removed, and the active indicator must use the same color role as the button's icon or label text to ensure at least 3:1 contrast against the button's background container.
    -   **RTL Mirroring**: No horizontal mirroring needed.

3.  **Loading Indicator Specs (Shape Morphing)**:
    -   **Anatomy**: Introduced in the GM3 Expressive update (Aug 2024). Composed of a looping shape-morphing sequence cycling through seven unique Material 3 geometric shapes.
    -   **Containment Configurations**:
        -   *Default (Uncontained)*: Sits directly on a surface. Uses the `--md-sys-color-primary` color role.
        -   *Contained*: Housed in a distinct elevated circular or pill-shaped container (default 48dp container size). Used primarily when overlapping content (such as in pull-to-refresh). The active shape transitions to the `--md-sys-color-on-primary-container` role on a `--md-sys-color-primary-container` or `--md-sys-color-surface-container-high` fill.
    -   **Size Range**: Flexible sizing from 24dp to 240dp. The proportion between the optional outer container and the active indicator stays constant when resizing.

4.  **AI Conversational Indicators (Aurora Update)**:
    -   **Processing Indicator**:
        -   *Thinking Dots Step*: Three horizontal dots in motion to signal system operations are starting. No text should be displayed during this step.
        -   *Status Text Step*: Thinking dots fade out as status text fades in. Uses a maximum of 24 characters. Ellipses are strictly prohibited.
        -   *Energy Types*: Uses decorative Aurora "energy" motion (fluid, smooth moving gradients). Defaults to 1C (one-color) energy, with 4C (four-color) energy reserved for primary brand criteria.
        -   *Styles*: *Standard* (in conversation pane, uses energy) and *Uncontained* (nested in a container, e.g., prompt field, no energy).
    -   **Generating Indicator**:
        -   *Usage*: Used when image/video generation takes > 1s to signal the size and shape of expected media and minimize layout shifts.
        -   *Anatomy*: Composed of a surface container with fluid moving processing energy (Aurora gradient path) along the bottom and sides. Uses an active gerund text label without an ellipsis (e.g., "Generating image") above the container.

### Process Duration Rules & Hierarchy

-   **Instant (< 200ms)**: No indicator should be shown.
-   **Loading Indicator (200ms to 5s)**: Recommended for short processes. Uses shape animation to mitigate perceived latency. Must **not** transition into a progress indicator.
-   **Progress Indicator (> 5s)**: Required for long, heavy processes. Products should allow users to navigate away while long processes finish in the background.
-   **AI Processes**: Processing or generating indicators should be shown immediately upon prompt submission if the response is expected to take longer than 1s.

### GM3 Accessibility & Interaction Rules

-   **Contrast Minimums**: The active indicator track or morphing shape must maintain at least a **3:1 contrast ratio** against its container or underlying surface.
-   **Role & Labeling**: Indicators must have an explicit accessibility label with the `progressbar` role describing the specific activity (e.g., "Loading news article", "Refreshing page", "Searching images").
-   **Non-Gesture Refresh Alternative**: Pull-to-refresh interactions must provide an accessible non-gesture alternative (such as a dedicated "Refresh" menu item or button).
-   **Decorative Elements**: AI energy effects, glow, blurs, and thinking dots are decorative and must be hidden from assistive technologies.

### Critical GM3 Violations to Flag

-   **Missing Stop Indicator**: Presenting a linear determinate progress bar without the required 4dp end stop marker where track contrast is low (< 3:1).
-   **Conflating Loading and Progress Indicators**: Transitioning a short-term `Loading indicator` directly into a long-term `Progress indicator` during a single process flow.
-   **Missing Non-Gesture Refresh**: Implementing pull-to-refresh without providing an accessible menu item or button alternative for refreshing.
-   **Inconsistent Process Theming**: Using a circular indicator for a specific process (like refreshing) on one screen, but switching to a linear indicator for the same process on another screen.
-   **Ellipses in AI Status Text**: Including ellipses in AI status text or generating indicators (e.g., "Searching..." is a violation; "Searching" is correct).
-   **Exposing AI Decorative Motion**: Exposing thinking dots, energy effects, or source logos as separate accessibility objects to screen readers.
