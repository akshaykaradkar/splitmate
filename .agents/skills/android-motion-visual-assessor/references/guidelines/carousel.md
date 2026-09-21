# Carousels - Android Motion Design System Guidelines & Specifications

This reference provides universal visual heuristics for detecting Carousels
(scrolling containment groupings displaying collections of items) across any
application, followed by Android Motion Design System specifications for
compliance auditing.

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
-   **Content Types (Universal Scope)**: While carousels are often associated
    with visual imagery (photos, banners), for detection purposes, any
    horizontally scrolling track of related items—including text-heavy cards,
    icon-labeled shortcuts, or data columns —MUST be mapped to **`Carousel`**.
    Do not restrict detection only to image-based carousels.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion Design System adherence audit, evaluate detected
carousels against the following strict standards:

### Android Motion Carousel Layouts & Signatures

The Android Motion Design System defines distinct carousel layout structures to support
different content densities and visual hierarchies:

| Layout                  | Visual Signature        | Typical Use Case         |
| :---------------------- | :---------------------- | :----------------------- |
| **Multi-browse**        | Displays a mix of       | Visual collections,      |
:                         : Large, Medium, and      : photo galleries, and     :
:                         : Small items             : dynamic feeds.           :
:                         : simultaneously. Items   :                          :
:                         : scale dynamically as    :                          :
:                         : they scroll.            :                          :
| **Uncontained**         | Displays items of a     | Text-heavy cards and     |
:                         : uniform size that       : traditional multi-item   :
:                         : scroll continuously to  : carousels.               :
:                         : the container edge      :                          :
:                         : without outer           :                          :
:                         : containment padding.    :                          :
| **Hero**                | Features one large      | Featured spotlighting    |
:                         : dominant item paired    : and prominent            :
:                         : with one small preview  : promotions.              :
:                         : item peek at the        :                          :
:                         : trailing edge.          :                          :
| **Center-aligned Hero** | Features one large      | Focused, centered        |
:                         : dominant item centered, : spotlight content.       :
:                         : flanked by small        :                          :
:                         : preview items peeking   :                          :
:                         : at *both* edges.        :                          :
| **Full-screen**         | Displays a single       | Immersive video feeds or |
:                         : edge-to-edge large item : full-screen content      :
:                         : (0dp padding), often    : exploration.             :
:                         : oriented vertically.    :                          :

### Android Motion Visual, Color & Typographic Rules

-   **Item Corner Radius**: Non-fullscreen carousel items must feature rounded corners mapping to `--droid-sys-shape-corner-extra-large` (28px). Full-screen carousel items use `--droid-sys-shape-corner-none` (0px) to maximize immersive display.
-   **Standard Paddings**:
    -   *Multi-browse & Hero*: 16dp leading/trailing outer padding, 8dp top/bottom padding, and 8dp spacing between items.
    -   *Uncontained*: 16dp leading padding, 8dp top/bottom padding, and 8dp spacing between items.
    -   *Full-screen*: 0dp leading/trailing/top/bottom padding, with 16dp spacing between vertical items.
-   **Small Item Width**: Sized dynamically as a visual anchor, typically defined as **40dp to 56dp**.
-   **Large Item Width**: Dynamic, adjusting to fill the remaining container space.
-   **Typography**:
    -   Optional overlay captions or text card labels must use `--droid-sys-typescale-title-medium` (500 1rem, 'Google Sans Text') or `--droid-sys-typescale-title-small` (500 0.9rem, 'Google Sans Text').

### Android Motion Dynamics & Scrolling Rules

-   **Parallax Scroll Effect**:
    -   During scroll interactions, carousel items must exhibit a **parallax effect** where the background image moves at a slightly slower speed than the container bounds. This is a signature characteristic of the Android Motion Design System.
-   **Scrolling Behaviors**:
    -   *Snap-Scrolling*: Recommended for Multi-browse and Hero layouts, and mandatory for Full-screen feeds. Items snap dynamically to align with the layout grid upon release using a spring-based physics animation (or a conversion of `--droid-sys-motion-easing-emphasized-decelerate` over a duration of `--droid-sys-motion-duration-400` as a fallback).
    -   *Free Scrolling*: Continuous, uninterrupted scroll where items can stop at any position; recommended for Uncontained carousels.
-   **Reduced Motion Context**:
    -   When device-level reduced motion settings are enabled, the parallax scrolling effect must be deactivated, and items must remain at a uniform static size without scaling as they move through the layout.
-   **Accessibility Alternatives**:
    -   For carousels placed on vertically scrolling pages, an accessible text alternative must be provided to view all items vertically without horizontal scrolling (e.g., adding a "Show all" button or header arrow icon button to open a dedicated vertical list page).

### Critical Android Motion Violations to Flag

-   **Missing Carousel Container Mapping**: Conflating a horizontal carousel container with a generic `List` or `Custom layout` during an audit.
-   **Improper Item Corner Radii**: Presenting non-fullscreen carousel items with sharp 0dp corners instead of the required `--droid-sys-shape-corner-extra-large` (28px) rounded shape.
-   **Lacking Edge Peek Affordance**: Aligning carousel items perfectly flush to the screen edges without any partial item visibility (edge peek), hiding the scrollable nature from the user.
-   **Missing Parallax Motion**: Scrollable carousel items that move flatly with their content, failing to render the signature background parallax effect.
-   **Stiff / Linear Snapping**: Snap-scrolling layouts that use a harsh linear motion curve to snap items into place instead of the fluid, spring-elastic dynamics of the Android Motion design system.
