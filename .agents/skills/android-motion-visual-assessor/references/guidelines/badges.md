# Badges - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Badges
(indicating notifications, counts, or status) across any design system or
platform, followed by Android Motion design system specifications for
compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Badge** is a compact visual indicator attached to navigation items, icons,
or tabs to communicate unread notifications, numerical counts, or status
updates. Agents must detect badges based on their distinct geometric and
structural qualities, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Anchoring**: Badges are consistently anchored at the upper
    trailing edge (top-right quadrant in left-to-right layouts) of a parent
    control. They frequently overlap the bounding box of underlying `Icon
    button`, `Tab`, `Navigation bar`, or `Navigation rail` destinations.
-   **Visual Boundaries & Containment**: Badges appear as small, solid-colored
    shapes contrasting sharply with the underlying element or background.
    -   *Dot Badges*: Small, circular indicators without text.
    -   *Pill / Count Badges*: Horizontally expanding rounded rectangles or pill
        shapes containing numbers or short text.
-   **Core Anatomy**:
    -   *Container*: A high-contrast filled background shape (often red, blue,
        or tertiary brand colors).
    -   *Label (Optional)*: A numerical count (e.g., "1", "99+") or short status
        text rendered in a miniature font size.
-   **Mandatory Dual Mapping (Parent + Badge)**: When a badge is attached to or
    overlapping another control, agents MUST explicitly identify BOTH components
    as separate objects. Agents must not assume the badge is merely an internal
    property of the parent icon.
    -   **Parent Component**: Identify the underlying interactive target (e.g.,
        `Icon button`, `Tab`, `Navigation bar` item).
    -   **Attached Badge (`Badge`) [MANDATORY]**: Exclusively bound the small
        overlay dot or counter indicator and classify it independently as
        `Badge`.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion adherence audit, evaluate detected badges against the following strict standards derived from Android Motion guidelines:

### Android Motion Visual & Behavioral Rules

-   **Badge Variants**:
    -   **Small Badge**: A simple 6px circular dot (3px corner radius or `--droid-sys-shape-corner-full`) indicating an unread notification. It contains no label text.
    -   **Large Badge**: A 16px tall rounded container with an 8px corner radius (`--droid-sys-shape-corner-small`) containing label text for counts or status. It expands horizontally as digits increase (up to a maximum width of 34px).
-   **Content & Character Limits**: Large badges must limit their content to a maximum of four characters (e.g., "999+").
-   **Color Mappings & Roles**:
    -   Default container color: `--droid-sys-color-error` (for urgent alerts) or `--droid-sys-color-tertiary` / `--droid-sys-color-primary` (for non-urgent status).
    -   Default label color: `--droid-sys-color-on-error` or the matching `--droid-sys-color-on-[role]` token.
-   **Anchor Distances & Padding**:
    -   *Small Badge Anchor Distance*: 6x6px from the icon anchor point.
    -   *Large Badge Anchor Distance*: 14x12px from the icon anchor point.
    -   *Large Badge Padding*: 4px internal padding (`--droid-sys-shape-corner-extra-small` width) on the left and right.
-   **Typography**:
    -   Large badge label text must use `--droid-sys-typescale-label-small` (500 0.7rem/1rem `'Google Sans Text'`).
-   **Behavior, Dismissal & Motion**:
    -   Badges are temporary status indicators; a badge indicating an unread notification must be hidden once the user selects the corresponding navigation destination.
    -   **Appearing Motion (Android Motion Exclusives)**: When appearing, a badge must scale up from the anchor point using `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) over `--droid-sys-motion-duration-150` (150ms) or `--droid-sys-motion-duration-200` (200ms).
    -   *RTL Layouts*: In right-to-left languages, the badge position mirrors to the upper leading (top-left) edge.

### Critical Android Motion Violations to Flag

-   **Character Overflow**: Displaying more than four characters in a large badge (e.g., "10000" instead of "999+"), violating layout constraints.
-   **Improper Anchoring**: Positioning the badge completely detached from its parent icon or on the wrong corner (e.g., bottom-left quadrant in an LTR layout).
-   **Missing Independent Classification**: Failing to classify the `Badge` independently from its parent container during a component audit.
-   **No/Abrupt Motion**: Having badges pop in abruptly without the standard scale-up transition over `--droid-sys-motion-duration-150`.
