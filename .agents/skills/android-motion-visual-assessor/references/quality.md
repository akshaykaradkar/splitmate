# Quality

1.  [ID: quality_color_theme_consistency] Does the UI maintain consistent tonal
    theme roles across all surfaces without mixing mismatched light and dark
    container surfaces in the same view? References `--droid-sys-color-*` roles. [Metric type: Adherence] [Weight: 5]

2.  [ID: quality_color_prefers_color_scheme] Does styling configure display
    theme preferences using media queries
    (e.g., `@media (prefers-color-scheme: dark)`) or semantic platform theme
    tokens? [Requires Code Inspection] [Metric type: Adherence] [Weight: 3]

3.  [ID: quality_typography_no_clipping_wrapping] Are all text labels, headings,
    and navigation titles fully visible without unexpected line clipping,
    awkward mid-word breaks, or truncation ellipsis? [Metric type: Hygiene]
    [Weight: 5]

4.  [ID: quality_grid_spacing_8dp_alignment] Do layout margins, container
    padding, and spacing gaps between adjacent elements align to a visible 8dp /
    8px incremental spatial grid rhythm? [Metric type: Hygiene] [Weight: 3]

5.  [ID: quality_edge_margins_page_boundaries] Do primary content containers and
    cards maintain visible breathing margins from the screen boundaries rather
    than flush colliding against viewport edges? [Metric type: Hygiene] [Weight:
    3]

6.  [ID: quality_shape_nested_corner_proportionality] If rounded containers
    contain nested child elements (such as inner cards or buttons), do the inner
    corner radii appear proportionally smaller than outer corners, matching the `--droid-sys-shape-corner-*` hierarchy? [Metric type:
    Hygiene] [Weight: 3]

7.  [ID: quality_shape_continuous_corner_radii] On Android Motion card
    containers, do container corners appear smoothly and continuously rounded
    (pill, squircle, or standard `--droid-sys-shape-corner-*` tokens) rather than sharp or unstyled?
    [Metric type: Adherence] [Weight: 3]

8.  [ID: quality_elevation_backdrop_blur] If floating bars, modal sheets, or
    transient overlays sit over scrollable content, does the overlay feature a
    translucent blur/vibrancy treatment so background shapes remain softly
    discernible? [Metric type: Adherence] [Weight: 3]

9.  [ID: quality_interactive_focus_ring_visible] If a control is focused via
    keyboard or D-pad navigation, is a high-contrast focus indicator ring
    (:focus-visible) clearly visible around the focused control without
    obstruction? [Metric type: Hygiene] [Weight: 5]

10. [ID: quality_states_empty_actionable_cta] If an empty or zero-data state is
    shown, does the view feature clear explanatory copy accompanied by at least
    one prominent, actionable CTA button? [Metric type: Hygiene] [Weight: 3]

11. [ID: quality_loading_skeleton_dimensions] If skeleton loading cards are
    visible, do their placeholder shapes and container heights match the
    proportions of the actual content cards being loaded? [Metric type:
    Hygiene] [Weight: 3]

12. [ID: quality_accessibility_screen_reader_labels] Do standalone icon buttons,
    custom controls, and images contain explicit semantic labels (e.g.,
    `contentDescription`, `accessibilityLabel`, or `aria-label`)? [Requires
    Code Inspection] [Metric type: Adherence] [Weight: 5]

13. [ID: quality_accessibility_prefers_reduced_motion] Are motion transitions
    configured with reduced-motion queries (e.g.,
    `@media (prefers-reduced-motion: reduce)`) or platform accessibility checks?
    When active, animations must either gracefully scale back or switch to low-energy fading transitions.
    [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]

14. [ID: quality_aurora_spark_icon_brand_tint] If AI assistance or generative
    features are present, are they signaled with the standardized Material
    Symbols spark indicator icon with appropriate brand color tint (`--droid-sys-color-primary`)? [Metric
    type: Adherence] [Weight: 3]

15. [ID: quality_aurora_streaming_text_container] When AI streaming text output
    is displayed, is the generated content contained within a dedicated
    bounding card or bubble that preserves layout structure? [Metric type:
    Hygiene] [Weight: 3]

16. [ID: quality_writing_no_conversational_fluff] Does UI instructional text
    avoid conversational filler words ('please', 'sorry', 'thank you') and
    first-person corporate pronouns ('I', 'we')? [Metric type: Hygiene]
    [Weight: 1]

17. [ID: quality_writing_actionable_error_messages] Do error messages
    explicitly state what problem occurred and provide concrete, actionable
    guidance on how the user can resolve it? [Metric type: Hygiene] [Weight: 3]

18. [ID: quality_writing_button_cta_verb_length] Do action button CTAs begin
    with an active verb and contain at most 3 words (e.g., 'Save draft',
    'Confirm order', 'Get started')? [Metric type: Hygiene] [Weight: 1]

19. [ID: quality_writing_platform_native_verbs] Does instructional copy adhere
    to platform-native interaction vocabulary (e.g., 'Tap' on iOS/mobile touch
    vs. 'Click' or 'Press' on Web/Desktop)? [Metric type: Adherence] [Weight: 1]

20. [ID: quality_forms_static_field_labels] Do form text fields maintain visible
    static labels or permanent container helper text rather than relying solely
    on disappearing placeholder prompts? [Metric type: Hygiene] [Weight: 3]

21. [ID: quality_forms_native_input_types] Are input fields configured with
    appropriate semantic types (e.g., `type="email"`,
    `keyboardType="numeric"`) to invoke the correct input keypad? [Requires Code
    Inspection] [Metric type: Adherence] [Weight: 3]

22. [ID: quality_wayfinding_persistent_search_bar] On search-centric views, is a
    static, discoverable search bar anchored prominently at the top of the
    content area? [Metric type: Hygiene] [Weight: 5]

23. [ID: quality_adaptive_window_size_classes] Does navigation match the window
    size class: bottom navigation bar on compact mobile portrait views, or
    navigation rail / drawer on expanded desktop views? [Metric type: Adherence]
    [Weight: 10]

24. [ID: quality_adaptive_hinge_foldable_avoidance] If a foldable or dual-screen
    device layout is displayed, are critical interactive controls, dialogs, and
    text columns positioned safely away from the physical center hinge fold?
    [Metric type: Adherence] [Weight: 5]

25. [ID: quality_adaptive_horizontal_overflow_scroll] Does body text and primary
    list content fit within the viewport width without triggering unintended
    horizontal scrollbars? [Metric type: Hygiene] [Weight: 5]

26. [ID: quality_canvas_edge_to_edge_system_bars] Does the primary application
    surface render full-bleed behind transparent or translucent system status
    and navigation bars without artificial opaque letterboxing? [Metric type:
    Adherence] [Weight: 5]

27. [ID: quality_scroll_lift_on_scroll_headers] When content is scrolled under a
    top app bar, does the bar display an elevated tonal color, subtle shadow
    line, or blur to visually separate from content, easing into place with `--droid-sys-motion-easing-emphasized`? [Metric type: Adherence]
    [Weight: 3]

28. [ID: quality_scaffolds_canonical_expressive_layouts] For tablet or desktop
    screen widths, does the view implement a recognized canonical multi-pane
    scaffold (List-Detail, Supporting Pane, or Feed)? [Metric type: Adherence]
    [Weight: 5]

29. [ID: quality_motion_token_integration] Do transitions, hover animations, active presses,
    or page entries in the stylesheet bind to standard Android Motion token properties
    (e.g., `--droid-sys-motion-easing-emphasized`, `--droid-sys-motion-duration-250`)
    rather than arbitrary cubic-beziers or raw timing values? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]
