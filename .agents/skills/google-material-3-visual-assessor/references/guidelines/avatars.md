# Avatars - Universal AI Detection Guide & MD3 Specifications

This reference provides universal visual heuristics for detecting Avatars
(representing user profiles, entities, or account icons) across any design
system or platform, followed by Material Design 3 (MD3) specifications for
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

## Part 2: MD3 Specifications & Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate the detected
avatar against the following strict standards derived from Material guidelines:

### MD3 Visual & Behavioral Rules

-   **Geometric Masking & Expressive Shapes**: MD3 encourages circular masks or
    expressive shapes to represent people and entities. Circular avatars must be masked utilizing the fully rounded token `--md-sys-shape-corner-full` (`max(50cqw, 50cqh)`).
-   **Standard Dimensions & Sizing**:
    -   **In List Items (Leading Slot)**: Default avatar diameter is **40dp** (utilizing standard shape masking).
    -   **In App Bars & Search Bars**: Default avatar diameter is typically **32dp** or **40dp**, depending on the bar layout density.
    -   **In Chips (Input Chip / People Chip)**: Embedded avatar size is strictly **24dp** in height/width, featuring fully rounded corners (`--md-sys-shape-corner-full`).
-   **Color & Contrast Roles for Monograms**:
    -   When an avatar displays textual user initials (monogram) instead of an image, the background container and text must use complementary color-contrast roles.
    -   Recommended combinations:
        -   Container: `--md-sys-color-primary-container` paired with Text: `--md-sys-color-on-primary-container`.
        -   Container: `--md-sys-color-secondary-container` paired with Text: `--md-sys-color-on-secondary-container`.
        -   Container: `--md-sys-color-tertiary-container` paired with Text: `--md-sys-color-on-tertiary-container`.
        -   Container: `--md-sys-color-surface-variant` paired with Text: `--md-sys-color-on-surface-variant`.
    -   The monogram initials must utilize clear and legible typography, typically leveraging `--md-sys-typescale-title-small` or `--md-sys-typescale-label-large` (such as `Google Sans Text`).
-   **App Bar & Search Bar Constraints**:
    -   In search app bars and standard app bars, an avatar is typically placed
        at the trailing edge.
    -   *Action Limits*: Products must not use more than two trailing icon
        buttons alongside an avatar. If additional actions are needed, they must
        be housed in a separate overflow menu or toolbar.
-   **List Item Integration & Selection**:
    -   When placed in a list item's leading slot, the avatar anchors the visual
        hierarchy.
    -   *Interactive Shortcut*: Avatars in lists can serve as interactive
        selection shortcuts; tapping a leading avatar can transition the list
        item into a selected state (often replacing the avatar with a checkmark or displaying an badge on the bottom-right corner of the avatar).

### Critical MD3 Violations to Flag

-   **Excessive Trailing Actions**: Placing more than two icon buttons next to
    an account avatar in an app bar or search bar lockup.
-   **Conflating Avatars with Content Thumbnails**: Using circular avatar
    styling for non-entity content (like a video preview or product shot), which
    violates Material shape semantics (video/content thumbnails must use rounded rectangles such as `--md-sys-shape-corner-medium` or `--md-sys-shape-corner-small`).
-   **Missing Inner Content Classification**: Failing to classify the inner
    `Image` when an avatar contains a user profile photo during an audit.
-   **Poor Contrast on Monogram Avatars**: Using random or un-themed background fills for monogram avatars that violate the required 4.5:1 contrast ratio against the text initials (must adhere to M3 container and on-container token combinations).
