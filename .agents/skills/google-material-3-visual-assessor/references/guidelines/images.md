# Images - Universal AI Detection Guide & MD3 Specifications

This reference provides universal visual heuristics for detecting Images
(photographs, illustrations, charts, and graphical media containers) across any
design system or platform, followed by Material Design 3 (MD3) specifications
for compliance auditing.

## Part 1: Universal AI Detection Heuristics

An **Image** is a dedicated visual container displaying raster or rich graphical
media (such as photographs, detailed illustrations, data visualizations, or
video thumbnails). Agents must detect images based on raster texture, bounding
geometry, and layout hierarchy, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Raster Texture & Graphic Density**: Look for areas exhibiting rich
    photographic texture, multi-colored illustrative detail, gradients, or
    complex data visualizations (charts/graphs) that differ distinctly from
    solid-colored UI containers or monochromatic vector icons.
-   **Bounding Geometry & Aspect Ratios**: Images are framed within defined
    geometric boundaries, most commonly standard rectangular aspect ratios
    (16:9, 4:3, 1:1) or custom cropping shapes (circles, super-ellipses). They
    may appear as full-bleed backgrounds, standalone hero assets, or leading
    thumbnails.
-   **Placement & Contextual Hierarchy**: Images appear in predictable
    structures across UI layouts:
    -   *Cards*: Leading media blocks at the top of a card container (hero
        images) or author thumbnails.
    -   *Lists & Tables*: Leading square or rectangular thumbnails anchored to
        the far left of list item rows (e.g., album art, video previews).
    -   *Carousels & Feeds*: Collections of coplanar or dynamically scaled image
        containers arranged in horizontal or vertical scrolling groupings.
-   **Mandatory Dual Mapping (Avatar + Embedded Image)**: When analyzing user
    profile pictures, account icons, or circular avatars displaying an embedded
    photograph, agents MUST NOT map the element solely to `Avatar`. Downstream
    evaluation systems require the inner image content to be captured. Agents
    MUST explicitly identify BOTH:
    -   **Inner Image (`Image`) [MANDATORY]**: Bound the photographic picture or
        artwork contained within the avatar boundary and classify it as `Image`
        (category: `Primitives` or `Content`).
    -   **Outer Avatar (`Avatar`)**: Bound the surrounding circular or shaped
        boundary container representing the user entity and classify it as
        `Avatar`.
    -   *Images vs. Avatars*: Use `Image` classifications for general content
        (products, document previews, video thumbnails, charts). Reserve
        `Avatar` specifically for entity/user containers.

--------------------------------------------------------------------------------

## Part 2: MD3 Specifications & Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected
images against the following strict standards derived from Material guidelines
and design system tokens:

### MD3 Imagery Types & Accessibility (Alt Text)

Material Design 3 establishes rigorous guidelines for imagery presentation, sizing, clipping shapes, and accessibility:

1.  **Alternative Text (Alt Text)**:
    -   **Mandatory Requirement**: All informative images (photos,
        illustrations, charts, motion GIFs) must provide descriptive off-screen
        alt text for screen reader accessibility. If an image is purely
        decorative (meaning no information is lost if removed), it must be
        marked decorative (e.g., `alt=""`).
    -   **Brevity & Formatting**: The recommended length for alt text is 125
        characters or fewer to prevent screen reader cut-offs. Alt text must
        describe the meaning and context rather than literal background details,
        and must **never** start with redundant phrases like "image of".
    -   **Charts & Graphs**: Alt text for data visualizations must summarize the
        core takeaway and data type (e.g., "Summary of [data type] + [reason for
        chart]"). For complex analysis charts, interactive tooltips or data
        tables are strongly recommended over static images.
2.  **Layering Text over Images (Contrast Scrims)**:
    -   Placing raw text or icons directly over background images is strictly
        not recommended due to unpredictable contrast failure.
    -   **Translucent Scrims**: Whenever text or icons are layered over an
        image, products must incorporate a translucent background scrim,
        gradient overlay, or solid bounding shape beneath the text to guarantee
        WCAG accessibility contrast compliance.
    -   **Design Token Mapping**:
        -   Scrim color: `var(--md-sys-color-scrim)` (`#000000`).
        -   Text color over scrim: `var(--md-sys-color-inverse-on-surface)` (`#f2f2f2` / `#303030`) or a custom translucent color layer that meets the minimum contrast ratio of 4.5:1 against the shaded image background.
3.  **Media Slots in Components & Shapes**:
    -   *Lists*: Leading image slots must use square or rectangular geometry for
        general content (products/videos), utilizing `var(--md-sys-shape-corner-small)` (8px) or `var(--md-sys-shape-corner-medium)` (12px), reserving circular shapes (`var(--md-sys-shape-corner-full)`) for entity avatars.
    -   *Cards & Sheets*: Media blocks support full-bleed images. When embedded in cards, top corner rounding must match the card container's shape token:
        -   For standard cards: Top-left and top-right corners must use `var(--md-sys-shape-corner-large)` (16px) or `var(--md-sys-shape-corner-medium)` (12px) rounding, while bottom corners are flat (0px) where the image borders list details.
    -   *Spacing and Margins*: Layout offsets around images must align to MD3 measurement grid tokens:
        -   Grid Gaps & Padding: `var(--md-sys-measurement-space100)` (8px), `var(--md-sys-measurement-space200)` (16px), or `var(--md-sys-measurement-space300)` (24px).

| Visual Context | Recommended Shape Token | Rounded Corners | Rationale & Use Case |
| :--- | :--- | :--- | :--- |
| **Card Hero Media** | `var(--md-sys-shape-corner-large)` (or `-medium`) | 16px (or 12px) top corners | Blends with card container corners; bottom edge remains straight. |
| **List Row Thumbnail** | `var(--md-sys-shape-corner-small)` | 8px corners | Compact presentation for list-anchored products/videos. |
| **User Profile/Avatar** | `var(--md-sys-shape-corner-full)` | Fully rounded (pill/circle) | Reserved exclusively for user/entity representation. |
| **Standalone Artwork** | `var(--md-sys-shape-corner-medium)` | 12px corners | Standard decorative or informative editorial images. |

### Critical MD3 Violations to Flag

-   **Unscrimmed Text over Images**: Layering text directly over a photograph or
    illustration without a protective translucent scrim using `var(--md-sys-color-scrim)` or a solid bounding shape beneath the text, failing WCAG contrast minimums.
-   **Missing Alt Text on Informative Media**: Presenting non-decorative product
    photos, charts, or illustrations without descriptive alt text during an
    accessibility audit.
-   **Redundant Alt Text Phrasing**: Starting image alt text strings with "image
    of", "picture of", or repeating the exact visible caption word-for-word.
-   **Conflating General Images with Avatars**: Applying circular entity styling
    (`var(--md-sys-shape-corner-full)`) to general non-entity content (like a video preview or product shot).
-   **Misaligned Shape Corners**: Full-bleed card media that fails to inherit or align with the rounded corners of its parent card container (`var(--md-sys-shape-corner-large)` / 16px).
