# Aurora - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Aurora
components (AI energy surfaces and intelligence elements) across design systems
and platforms, followed by Android Motion specifications for
compliance auditing.

## Part 1: Universal AI Detection Heuristics

**Aurora** components represent AI capabilities, energy surfaces, and
intelligence features (e.g., chat bubbles, prompt fields, response carousels,
response lists, and suggestions).

### Key Visual & Geometric Heuristics

-   **Aurora Energy Surface**: Iridescent, luminous, or multi-colored gradient
    energy treatments applied to component boundaries, containers, or
    background surfaces to indicate active AI presence or processing.
-   **Aurora Chat Bubble**: Conversational message containers featuring AI
    energy accents or distinct intelligence styling.
-   **Aurora Prompt Field**: Text entry containers specifically designed for AI
    prompt input, often bordered or glowing with Aurora energy effects.
-   **Aurora Response Carousel**: A horizontal swipeable container presenting
    multiple AI-generated responses or suggestions.
-   **Aurora Response List**: A vertical container grouping AI response
    elements or generated text blocks.
-   **Aurora Suggestions**: Interactive suggestion chips or pill containers
    offering recommended follow-up prompts or AI actions.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion adherence audit for Aurora elements,
evaluate against the following standards derived from Android Motion guidelines:

### Android Motion Visual & Behavioral Rules

-   **Energy Surface Cardinality**: At most ONE active Aurora energy element per
    screen at any given time.
-   **AI Signal Exclusivity**: Aurora energy effects must be applied
    exclusively to signal active AI capabilities.
-   **Container Edges & Fills**:
    -   *Small Containers*: Blurred (uncontained) edge energy surfaces must use
        low-contrast tonal fills, such as `--droid-sys-color-tertiary-container` (light: `#c4eed0`, dark: `#0f5223`) or `--droid-sys-color-secondary-container` (light: `#c2e7ff`, dark: `#004a77`).
    -   *Large Containers*: Large energy containers must maintain crisp, sharp
        outer boundary edges using standard corner shapes like `--droid-sys-shape-corner-large` (16px) or `--droid-sys-shape-corner-extra-large` (28px).
    -   *Parent Edge Extension*: Energy effects must extend fully to parent
        container boundaries.
-   **Variant Specifications**:
    -   *4C Aurora Variant*: Restricted to signature, high-impact brand-AI
        moments.
    -   *Neural Aurora Variant*: Restricted exclusively to Gemini-branded
        intelligence features.
-   **Content Legibility**: Text, icons, and media layered over or adjacent to
    Aurora energy surfaces must maintain high-contrast legibility without
    interference, ensuring active text meets a contrast ratio of at least 4.5:1 relative to any point of the underlying gradient.
-   **Motion & Ambient Energy**:
    -   Aurora gradients should animate to represent processing or thinking states.
    -   Breathing, pulsing, or wave cycling animations must utilize standard slow motion tokens such as `--droid-sys-motion-duration-800` (800ms) or `--droid-sys-motion-duration-900` (900ms) with `--droid-sys-motion-easing-linear` or `--droid-sys-motion-easing-emphasized` to prevent visual fatigue.
