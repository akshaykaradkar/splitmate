# Avatars - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Avatars
(representing user profiles, entities, or account icons) across any design
system or platform, followed by Android Motion specifications for
compliance auditing.

## Part 1: Universal AI Detection Heuristics

An **Avatar** is a prominent visual container used to represent a person, user
account, or organizational entity. Agents must detect avatars based on
structural, geometric, and semantic qualities, regardless of design system
adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Contextual Anchoring**: Avatars appear in predictable
    locations depending on the parent component:
    -   *App Bars / Search Bars*: Typically placed at the far trailing edge
        (top-right quadrant) acting as an account profile or switcher.
    -   *Lists / Tables*: Anchored at the extreme leading edge (far left) of a
        list item row to identify the subject of the row (e.g., contacts,
        messaging conversations).
    -   *Cards / Bottom Sheets*: Placed in header lockups or thumbnail slots to
        attribute content to an author or entity.
    -   *Chips*: Embedded as a leading miniature visual element inside filter,
        input, or suggestion chips.
-   **Visual Boundaries & Shape Geometry**: Most commonly framed by circular or
    rounded bounding boxes (e.g., circles, super-ellipses, or rounded
    squares/rectangles). They may feature a distinct border/stroke, a solid
    background color (with monogram initials), or act as a direct clipping mask
    for a photo.
-   **Core Anatomy & Content Mappings**: Avatars typically contain one of three
    visual representations:
    -   *Photographic / Illustrative*: An uploaded user profile picture or
        artwork.
    -   *Monogram / Textual*: User initials rendered over a solid, colored
        background fill.
    -   *Iconic / Fallback*: A generic person silhouette or account symbol when
        no custom image is available.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion adherence audit, evaluate the detected
avatar against the following strict standards derived from Android Motion guidelines:

### Android Motion Visual & Behavioral Rules

-   **Geometric Masking & Expressive Shapes**: Android Motion enforces circular masks using `--droid-sys-shape-corner-full` (max(50cqw, 50cqh)) as the default to represent people, users, and organizational entities. Customized masks or rounded corner squares using `--droid-sys-shape-corner-large` (16px) or `--droid-sys-shape-corner-medium` (12px) may be used for branded account categories, provided consistency is maintained.
-   **App Bar & Search Bar Constraints**:
    -   In search app bars and standard app bars, an avatar is typically placed at the trailing edge.
    -   *Action Limits*: Products must not use more than two trailing icon buttons alongside an avatar. If additional actions are needed, they must be housed in a separate overflow menu or toolbar.
-   **List Item Integration & Selection**:
    -   When placed in a list item's leading slot, the avatar anchors the visual hierarchy.
    -   *Interactive Shortcut*: Avatars in lists can serve as interactive selection shortcuts; tapping a leading avatar transitions the list item into a selected state. This selection transition should morph the avatar into a checkmark graphic or apply a badge overlay, animating smoothly over `--droid-sys-motion-duration-200` (200ms) with `--droid-sys-motion-easing-standard`.
-   **Chip Integration Specs**:
    -   When embedded within a chip (e.g., Input Chip or People Chip), the standard avatar size is **24dp** with a fully rounded border-radius utilizing `--droid-sys-shape-corner-full`.
-   **Typography & Colors for Monograms**:
    -   Initials inside monograms should use `--droid-sys-typescale-title-small` (`500 0.9rem/1.3rem 'Google Sans Text'`) or `--droid-sys-typescale-title-medium` (`500 1rem/1.5rem 'Google Sans Text'`) depending on the scale.
    -   Monogram backgrounds must utilize `--droid-sys-color-primary-container` (light: `#d3e3fd`, dark: `#0842a0`) paired with `--droid-sys-color-on-primary-container` (light: `#041e49`, dark: `#d3e3fd`) or other contrasting brand combinations like `--droid-sys-color-surface-variant` to ensure WCAG readability.

### Critical Android Motion Violations to Flag

-   **Excessive Trailing Actions**: Placing more than two icon buttons next to an account avatar in an app bar or search bar lockup.
-   **Conflating Avatars with Content Thumbnails**: Using circular avatar styling (`--droid-sys-shape-corner-full`) for non-entity content (like a video preview or product shot), which violates shape semantics.
-   **Missing Inner Content Classification**: Failing to classify the inner `Image` when an avatar contains a user profile photo during an audit.
-   **Wrong Text Scaling**: Using body copy or incorrect font weights for user initials instead of `--droid-sys-typescale-title-small` or `--droid-sys-typescale-title-medium`.
