# Aurora (AI Gradients \& Glows) - Universal AI Detection Guide \& Elements GM3 Specifications

This reference provides visual heuristics for detecting Aurora elements (AI-driven gradients and glows) across any user interface, followed by the strict **Elements GM3** specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

**Aurora** elements represent AI capabilities, energy surfaces, and intelligence features using luminous, iridescent, or multi-colored gradient designs. Agents must detect Aurora components based on their distinct color transitions and spatial glow patterns, regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **AI Gradient Fills**: Luminous or multi-colored gradient fills applied directly to interactive components (buttons, progress indicators, ghost loading bars) or headings.
-   **Background Gradient Glows (Energy Surfaces)**: Luminous background ellipses, glows, or soft gradient spotlights layered behind AI response areas (e.g., chat answers, summary blocks) to visually isolate AI-generated content from standard UI content.
-   **AI Accent Elements**: Subtle color treatments or icons that indicate active AI processing, generation, or suggestions (e.g., suggestion chips with gradient borders).

---

## Part 2: Elements GM3 Specifications \& Compliance Auditing

CE Elements products do not yet support dynamic color; instead, they utilize the **Baseline AI colors** defined in GM3. When conducting an Elements GM3 compliance audit for AI gradient and glow elements, evaluate against the following specifications:

### 1. General Rules \& Cardinality

-   **Single Glow Cardinality**: At most **ONE active background gradient glow** is permitted per screen at any given time.
-   **AI Signal Exclusivity**: Gradient fills and background glows must be applied **exclusively** to signal active AI capabilities and assist the user's understanding that AI is present or processing. Do not use AI gradients as general decorative styling.
-   **Multiple AI Outputs**: For screens containing multiple AI outputs or cards, **DO NOT use background gradient glows**. Instead, distinguish these elements using a combination of standard iconography, clear content, and elevation.

### 2. Background Gradient Glows (Energy Surfaces)

-   **Composition**: A gradient glow is built as a blurred ellipse combining:
    1.  **AI analog variant** (recommended at **\<= 60% opacity**)
    2.  **AI complement variant** (recommended at **\<= 45% opacity**)
-   **Blur Effect**: A strong blur filter must be applied to create a seamless, non-distracting energy glow behind content.
-   **Permitted Backgrounds**: Gradient glows may only be layered on top of **Surface**, **Surface Container Lowest**, or **Surface Container Low** backgrounds.
-   **Coverage**: The active background gradient glow must extend fully behind the relevant AI-generated output.

### 3. Gradient Fills (Muted \& Rich Variants)

-   **Muted Gradient Fills**:
    -   *Usage*: Applied to interactive buttons, progress indicators, and ghost loading bars.
    -   *Behavior*: Moving gradient fills are used to represent the active AI "thinking" or "generating" state.
    -   *Composition*: Formed by blending lighter variant color tones in this order: **AI main variant** -\> **AI analog variant** -\> **AI complement variant**.
    -   *Pairing Requirement*: Progress indicators using gradient fills must be paired with clear text and iconography to explain the current AI state.
-   **Rich Gradient Fills**:
    -   *Usage*: Applied to decorative greeting headlines when a user enters an AI-first experience.
    -   *Composition*: Formed by combining darker color tones in this order: **AI main** -\> **AI analog** -\> **AI complement**.
    -   *Contrast Exception*: Rich gradient fills applied to text do not meet strict accessibility contrast ratios. Therefore, they must **only** be applied to purely decorative headlines (e.g., "Good morning, how can I help?").

### 4. Content Legibility \& Contrast

-   **Legibility Requirement**: Text, icons, and media layered over or adjacent to a gradient glow or gradient fill must maintain excellent readability.
-   **Contrast Minimums**: All text combinations over an AI background glow must meet the **4.5:1 contrast minimum**. When gradient glows are applied correctly on surface/surface-container backdrops, standard on-surface and on-surface variant colored text should meet this contrast ratio.
-   **Media Clarity**: Images, videos, or illustrations must remain sharp and unobscured by overlapping gradient flows.

---

## Critical Elements GM3 Violations to Flag

1.  **Multiple Gradient Glows**: Having more than one background gradient glow active on a single screen.
2.  **Improper Color Ordering**: Constructing gradient fills or glows using an improper color sequence (e.g., reversing the main, analog, and complement tones).
3.  **Contrast Failures on AI Glows**: Standard text layered over a gradient glow failing the 4.5:1 contrast minimum.
4.  **Non-Decorative Gradient Text**: Applying rich gradient fills to functional body copy or critical labels that require high contrast.
5.  **Glows on Unsupported Backgrounds**: Overlaying background gradient glows on dark, vibrant, or elevated card backgrounds other than Surface, Surface Container Lowest, or Surface Container Low.
6.  **Glows on Multiple AI Cards**: Using background glows under multiple side-by-side or stacked AI cards instead of using elevation and icons.
