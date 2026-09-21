# Carousel

1.  [ID: carousel_usage] Does the component showcase a set of visual items in a
    dense, swipeable horizontal layout? [Metric type: Hygiene] [Weight: 3]

2.  [ID: carousel_items_rounded_corners] Do carousel items have clearly rounded
    corners (spec: 28px using sys.shape.corner.extra-large / --md-sys-shape-corner-extra-large, unless full-screen which uses 0px / sys.shape.corner.none / --md-sys-shape-corner-none)? [Metric type: Hygiene] [Weight: 5]

3.  [ID: carousel_adjacent_spacing_gap] Is there a consistent spacing gap
    between adjacent carousel items (spec: 8px using --md-sys-measurement-space100 for non-fullscreen layouts, and 16px using --md-sys-measurement-space200 for full-screen)? [Metric type: Hygiene] [Weight: 1]

4.  [ID: carousel_navigation_placement] Are UI navigation arrows or pagination
    dots placed safely outside the scrolling carousel bounds? [Metric type:
    Hygiene] [Weight: 3]
