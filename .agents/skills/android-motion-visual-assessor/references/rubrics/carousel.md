# Carousel Rubric - Android Motion Design System

This rubric evaluates Carousels for visual and functional compliance with the Android Motion Design System.

## Evaluation Questions

1.  [ID: carousel_usage] Does the component showcase a set of visual items in a dense, swipeable horizontal layout?
    -   *Heuristic*: Verify that horizontal scrolling tracks of related items (e.g. cards, visual blocks) are mapped to `Carousel` and not misclassified as standard list groups or generic layouts.
    -   [Metric type: Hygiene] [Weight: 3]

2.  [ID: carousel_items_rounded_corners] Do non-fullscreen carousel items use the correct rounded corners token?
    -   *Heuristic*: Carousel items must feature clearly rounded corners mapping to `--droid-sys-shape-corner-extra-large` (value: `28px`), whereas full-screen items must use `--droid-sys-shape-corner-none` (value: `0px`).
    -   [Metric type: Hygiene] [Weight: 5]

3.  [ID: carousel_adjacent_spacing_gap] Is there a consistent spacing gap between adjacent carousel items?
    -   *Heuristic*: Check for a consistent 8dp spacing gap separating adjacent visual items inside the track.
    -   [Metric type: Hygiene] [Weight: 1]

4.  [ID: carousel_navigation_placement] Are UI navigation arrows or pagination dots placed safely outside the scrolling carousel bounds?
    -   *Heuristic*: Arrow buttons, pagination dots, or pill markers must be positioned outside the scrollable track region to avoid overlap collisions.
    -   [Metric type: Hygiene] [Weight: 3]

5.  [ID: carousel_parallax_scroll] Do visual items exhibit a background parallax effect when scrolled?
    -   *Heuristic*: The background image within carousel items must move at a slightly slower speed than the outer container bounds, creating a dynamic, dimensional parallax effect.
    -   [Metric type: Compliance] [Weight: 5]

6.  [ID: carousel_item_size_transition] Do carousel items dynamically resize and scale when moving through the track layout?
    -   *Heuristic*: Items must dynamically transition in width (large, medium, small) as they scroll to optimize fit and density on screen.
    -   [Metric type: Compliance] [Weight: 3]

7.  [ID: carousel_snap_physics] Does snap-scrolling utilize spring-based physics model?
    -   *Heuristic*: Snapping behavior must utilize spring-based spatial physics (or a fallback curve mimicking `--droid-sys-motion-easing-emphasized-decelerate` over `--droid-sys-motion-duration-400`).
    -   [Metric type: Compliance] [Weight: 3]

8.  [ID: carousel_reduced_motion_behavior] Does the carousel support device-level reduced motion accessibility overrides?
    -   *Heuristic*: When reduced motion is active, parallax effects must be deactivated and items must remain at a static, uniform size.
    -   [Metric type: Compliance] [Weight: 3]

9.  [ID: carousel_accessibility_view_all] Does the page provide a vertical scrollable alternative for screen reader users?
    -   *Heuristic*: Verify that a "Show all" button or header arrow button is visible to open a dedicated vertical list page of all items.
    -   [Metric type: Compliance] [Weight: 5]

10. [ID: carousel_typography] Do text labels within the items utilize standard Google typefaces?
    -   *Heuristic*: Content overlay text must be styled with `--droid-sys-typescale-title-medium` or `--droid-sys-typescale-title-small` in `Google Sans Text` (or `Google Sans`).
    -   [Metric type: Hygiene] [Weight: 1]
