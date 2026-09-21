# Images - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Images (photographs, illustrations, charts, and graphical media containers) across any UI, followed by Elements GM3 design system specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

An **Image** is a dedicated visual container displaying raster or rich graphical media (such as photographs, detailed illustrations, data visualizations, or video thumbnails). Agents must detect images based on raster texture, bounding geometry, and layout hierarchy, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Raster Texture & Graphic Density**: Look for areas exhibiting rich photographic texture, multi-colored illustrative detail, gradients, or complex data visualizations (charts/graphs) that differ distinctly from solid-colored UI containers or monochromatic vector icons.
-   **Bounding Geometry & Aspect Ratios**: Images are framed within defined geometric boundaries, most commonly standard rectangular aspect ratios (16:9, 4:3, 1:1) or custom cropping shapes (circles, super-ellipses). They may appear as full-bleed backgrounds, standalone hero assets, or leading thumbnails.
-   **Placement & Contextual Hierarchy**: Images appear in predictable structures across UI layouts:
    -   *Cards*: Leading media blocks at the top of a card container (hero images) or author thumbnails.
    -   *Lists & Tables*: Leading square or rectangular thumbnails anchored to the far left of list item rows (e.g., album art, video previews).
    -   *Carousels & Feeds*: Collections of coplanar or dynamically scaled image containers arranged in horizontal or vertical scrolling groupings.
-   **Mandatory Dual Mapping (Avatar + Embedded Image)**: When analyzing user profile pictures, account icons, or circular avatars displaying an embedded photograph, agents MUST NOT map the element solely to `Avatar`. Downstream evaluation systems require the inner image content to be captured. Agents MUST explicitly identify BOTH:
    -   **Inner Image (`Image`) [MANDATORY]**: Bound the photographic picture or artwork contained within the avatar boundary and classify it as `Image` (category: `Primitives` or `Content`).
    -   **Outer Avatar (`Avatar`)**: Bound the surrounding circular or shaped boundary container representing the user entity and classify it as `Avatar`.
    -   *Images vs. Avatars*: Use `Image` classifications for general content (products, document previews, video thumbnails, charts). Reserve `Avatar` specifically for entity/user containers.

---

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected images against the following strict standards:

### Image Types, Sizing & Principles

Elements GM3 establishes principles and rules for imagery use to align with enterprise needs across products:

1.  **Core Imagery Principles**:
    -   **Represent Diversity**: Reflect a diverse, inclusive, and equitable environment, representing different countries, cultures, and backgrounds.
    -   **Digital Wellbeing**: Imagery must be harmonious with the surrounding UI, not shouting for users' attention. It should have a pleasing, consistent visual style.
    -   **Be Accessible**: Ensure accessibility compliance through contrast, alternative text, and captions.
2.  **Decorative vs. Informative Images**:
    -   **Decorative Images**:
        -   Do not add meaningful information to the content of a page (e.g., background textures, decorative visual fills).
        -   Must **never** have captions.
        -   Do not need to meet color contrast guidelines.
        -   Must be marked as decorative by providing empty alt tags (`alt=""`) or `aria-hidden="true"`, ensuring they are skipped by screen readers.
    -   **Informative Images**:
        -   Add key details or explanations to the content (e.g., diagrams, illustrations, charts, substantive photos).
        -   Must have descriptive alternative text (**alt text**) and **captions**.
        -   Essential visual components within the image must meet strict contrast guidelines (see below).
3.  **Alternative Text (Alt Text) Standards**:
    -   Must concisely and meaningfully translate the visual context into text.
    -   **Length Limit**: Recommended length is **125 characters or fewer** to prevent screen reader cutoffs.
    -   **Content**: Describe the contextual meaning rather than literal pixel details.
    -   **Redundancy Restriction**: Never start alt text with phrases like "image of", "picture of", or "graphic showing" (since screen readers already announce the image role). Do not repeat the caption word-for-word.
4.  **Captions**:
    -   Captions must appear **directly below** the visual asset.
    -   Explain the contextual information of the asset (the who, what, when, and where) rather than a literal transcription of the visual content.
5.  **Color Contrast Requirements**:
    -   **Essential Elements**: Key visual assets and representations within informative images must maintain at least a **3:1** color contrast ratio against adjacent backdrops, aiming for **4.5:1** contrast.
    -   **Non-Essential Elements**: Purely decorative background details or personality embellishments have no contrast minimums.
6.  **Text Over Images (Contrast Scrims)**:
    -   Layering text directly over raw background images is strictly prohibited due to unpredictable contrast failure.
    -   Whenever text or icons are overlaid on background images, a **translucent scrim**, gradient overlay, or solid bounding background shape must be integrated beneath the text to guarantee contrast compliance.

### Critical Elements GM3 Violations to Flag

-   **Raw Text Overlaid on Images**: Presenting overlay text on a background image without a protective translucent scrim or background shape.
-   **Missing Alt Text or Captions**: Providing informative images, illustrations, or diagrams without descriptive alt text or visible captions.
-   **Redundant Alt Text Phrasing**: Beginning alternative text with "image of" or repeating caption text word-for-word inside the alt attribute.
-   **Missing Decorative Tags**: Leaving decorative images with active screen-reader labels or missing `alt=""` attributes.
-   **Low Contrast in Essential Visuals**: Essential elements inside informative diagrams/charts falling below the 3:1 contrast minimum.
