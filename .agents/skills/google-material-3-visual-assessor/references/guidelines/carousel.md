# Carousels - Universal AI Detection Guide & MD3 Specifications

This reference provides universal visual heuristics for detecting Carousels
(scrolling containment groupings displaying collections of items) across any
design system or platform, followed by Material Design 3 (MD3) specifications
for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Carousel** is a dedicated structural container used to group and present a
collection of related items (such as cards, images, or featured spotlights) that
can be scrolled on and off the screen. Agents must detect carousels based on
their motion axis, containment grouping, and edge cut-offs, regardless of design
system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Motion Axis**: Most carousels are arranged horizontally,
    enabling left-to-right (or right-to-left) horizontal scrolling across a
    screen or layout pane (with the exception of immersive full-screen vertical
    feeds).
-   **Visual Boundaries & Edge Peek**: A primary visual signature of a carousel
    is the presence of **edge peek** (items partially visible, squished, or cut
    off at the screen or container edge). This sneak peek provides a clear
    affordance that additional content is available via scrolling.
-   **Core Anatomy & Child Elements**:
    -   *Outer Carousel Container*: The bounding box grouping the entire
        scrollable collection of items.
    -   *Inner Items*: The individual child components housed within the
        carousel (e.g., `Card`, `Image`, or promotional banners).
    -   *Indicators / Navigation Cues (Optional)*: Visual indicators such as
        pagination dots, pill markers, or arrow icon buttons indicating scroll
        position or enabling manual navigation.
-   **Mandatory Container Mapping**: When analyzing screens featuring horizontal
    scrolling collections, featured spotlighting, or carousel indicators, agents
    MUST explicitly bound and classify the outer container as `Carousel`. Agents
    must not omit the outer `Carousel` component or misclassify its container as
    a generic `List` or `Custom layout`. Inner items must be mapped as child
    components (e.g., `Card` or `Image`).
-   **Content Types (Universal Scope)**: While Material Design 3 carousels are
    often associated with visual imagery (photos, banners), for detection
    purposes, any horizontally scrolling track of related items—including
    text-heavy cards, icon-labeled shortcuts, or data columns —MUST be mapped to
    **`Carousel`**. Do not restrict detection only to image-based carousels.

--------------------------------------------------------------------------------

## Part 2: MD3 Specifications & Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected
carousels against the following strict standards derived from Material
guidelines:

### MD3 Carousel Layouts & Signatures

Material Design 3 defines distinct carousel layout structures to support
different content densities and visual hierarchies:

| Layout                  | Visual Signature        | Typical Use Case         |
| :---------------------- | :---------------------- | :----------------------- |
| **Multi-browse**        | Displays a mix of Large, Medium, and Small items simultaneously. Items scale dynamically as they scroll. | Visual collections, photo galleries, and dynamic feeds. |
| **Uncontained**         | Displays items of a uniform size that scroll continuously to the container edge without outer containment padding. | Text-heavy cards and traditional multi-item carousels. |
| **Hero**                | Features one large dominant item paired with one small preview item peek at the trailing edge. | Featured spotlighting and prominent promotions. |
| **Center-aligned Hero** | Features one large dominant item centered, flanked by small preview items peeking at *both* edges. | Focused, centered spotlight content. |
| **Full-screen**         | Displays a single edge-to-edge large item (0px padding), often oriented vertically. | Immersive video feeds or full-screen content exploration. |

### MD3 Tokens & Measurements

-   **Item Corner Radius**: MD3 specifies a standard **28px corner radius** (`--md-sys-shape-corner-extra-large`) for items within Multi-browse, Uncontained, and Hero layouts (Full-screen items use 0px/straight edges, i.e., `--md-sys-shape-corner-none`).
-   **Standard Paddings**:
    -   *Multi-browse & Hero*: 16px (`--md-sys-measurement-space200`) leading/trailing outer padding, 8px (`--md-sys-measurement-space100`) top/bottom padding, and 8px (`--md-sys-measurement-space100`) spacing between items.
    -   *Uncontained*: 16px (`--md-sys-measurement-space200`) leading padding, 8px (`--md-sys-measurement-space100`) top/bottom padding, and 8px (`--md-sys-measurement-space100`) spacing between items.
    -   *Full-screen*: 0px (`--md-sys-measurement-space0`) leading/trailing/top/bottom padding, with 16px (`--md-sys-measurement-space200`) spacing between vertical items.
-   **Small Item Width**: Functions as a structural detection anchor, specifically defined as **40px to 56px** (`--md-sys-measurement-space500` to `--md-sys-measurement-space700`).
-   **Large Item Width**: Dynamic, adjusting to fill the remaining container space up to a set maximum.

### MD3 Interaction & Accessibility Rules

-   **Scrolling Behavior**:
    -   *Snap-Scrolling*: Recommended for Multi-browse and Hero layouts, and mandatory for Full-screen feeds. Items snap to align with the layout grid upon release.
    -   *Free Scrolling*: Continuous scrolling where items can stop at any position; recommended for Uncontained carousels.
-   **Accessibility Requirements (Show All Button)**: On vertically scrolling pages, carousels require an accessible alternative to view all items without horizontal scrolling (e.g., adding a "Show all" button or arrow icon button to open a dedicated vertical list page).

### Critical MD3 Violations to Flag

-   **Missing Carousel Container Mapping**: Conflating a horizontal carousel container with a generic `List` or `Custom layout` during an audit.
-   **Improper Item Corner Radii**: Presenting non-fullscreen carousel items with sharp 0px corners instead of the mandatory 28px (`--md-sys-shape-corner-extra-large`) rounded shape.
-   **Lacking Edge Peek Affordance**: Designing a horizontal carousel where items align perfectly flush to the screen edge without any partial item visibility, concealing scrollability from the user.
