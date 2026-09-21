# Quality (Google Material 3 System)

1.  [ID: quality_color_theme_consistency] Does the UI maintain consistent tonal
    theme roles across all surfaces without mixing mismatched light and dark
    container surfaces in the same view? Verify adherence to M3 system colors like
    `--md-sys-color-surface`, `--md-sys-color-surface-container`,
    `--md-sys-color-surface-container-high`, and `--md-sys-color-on-surface`.
    [Metric type: Adherence] [Weight: 5]

2.  [ID: quality_color_prefers_color_scheme] Does styling configure display
    theme preferences using media queries (e.g., `@media (prefers-color-scheme: dark)`)
    or semantic platform theme tokens that switch colors from light-dark modes
    such as those specified in the Google Material 3 CSS bindings file?
    [Requires Code Inspection] [Metric type: Adherence] [Weight: 3]

3.  [ID: quality_typography_no_clipping_wrapping] Are all text labels, headings,
    and navigation titles fully visible without unexpected line clipping,
    awkward mid-word breaks, or truncation ellipsis? Ensure that text styles are
    mapped to the standard Google Material 3 typescales (e.g.,
    `--md-sys-typescale-body-large`, `--md-sys-typescale-title-medium`, using
    `Google Sans Text` or `Google Sans Flex`). [Metric type: Hygiene] [Weight: 5]

4.  [ID: quality_grid_spacing_8dp_alignment] Do layout margins, container
    padding, and spacing gaps between adjacent elements align to a visible 8dp /
    8px incremental spatial grid rhythm, leveraging M3 measurement tokens like
    `--md-sys-measurement-space100` (8px), `--md-sys-measurement-space200` (16px),
    or `--md-sys-measurement-space300` (24px)? [Metric type: Hygiene] [Weight: 3]

5.  [ID: quality_edge_margins_page_boundaries] Do primary content containers and
    cards maintain visible breathing margins from the screen boundaries (typically
    16px/`--md-sys-measurement-space200` or 24px/`--md-sys-measurement-space300` per M3
    layout grid guidelines) rather than flush colliding against viewport edges?
    [Metric type: Hygiene] [Weight: 3]

6.  [ID: quality_shape_nested_corner_proportionality] If rounded containers
    contain nested child elements (such as inner cards or buttons), do the inner
    corner radii appear proportionally smaller than outer corners? Verify that they
    follow the nested relationships of M3 shape tokens (e.g., outer card utilizing
    `--md-sys-shape-corner-large` [16px] has an inner button utilizing
    `--md-sys-shape-corner-small` [8px] or `--md-sys-shape-corner-medium` [12px]).
    [Metric type: Hygiene] [Weight: 3]

7.  [ID: quality_shape_continuous_corner_radii] On iOS or Material card
    containers, do container corners appear smoothly and continuously rounded
    (using standard M3 shape tokens like `--md-sys-shape-corner-extra-large` [28px],
    `--md-sys-shape-corner-large` [16px], `--md-sys-shape-corner-medium` [12px],
    or `--md-sys-shape-corner-full`) rather than sharp, raw, or unstyled?
    [Metric type: Adherence] [Weight: 3]

8.  [ID: quality_elevation_backdrop_blur] If floating bars, modal sheets, or
    transient overlays sit over scrollable content, does the overlay feature a
    translucent blur/vibrancy treatment and appropriate M3 elevation tokens (such as
    `--md-sys-elevation-level1` [1px] through `--md-sys-elevation-level5` [12px],
    combined with `--md-sys-color-scrim` or `--md-sys-color-shadow`) so background
    shapes remain softly discernible? [Metric type: Adherence] [Weight: 3]

9.  [ID: quality_interactive_focus_ring_visible] If a control is focused via
    keyboard or D-pad navigation, is a high-contrast focus indicator ring
    (:focus-visible) clearly visible around the focused control without
    obstruction, adhering to M3 focus indicator tokens (`--md-sys-state-focus-indicator-thickness` [3px],
    `--md-sys-state-focus-indicator-outer-offset` [2px])? [Metric type: Hygiene]
    [Weight: 5]

10. [ID: quality_states_empty_actionable_cta] If an empty or zero-data state is
    shown, does the view feature clear explanatory copy accompanied by at least
    one prominent, actionable CTA button (such as an M3 Filled Button using
    `--md-sys-color-primary` background and `--md-sys-shape-corner-full`)?
    [Metric type: Hygiene] [Weight: 3]

11. [ID: quality_loading_skeleton_dimensions] If skeleton loading cards are
    visible, do their placeholder shapes and container heights match the
    proportions of the actual content cards being loaded and use the standard
    M3 container colors (e.g., `--md-sys-color-surface-container` or
    `--md-sys-color-surface-container-high`)? [Metric type: Hygiene] [Weight: 3]

12. [ID: quality_accessibility_screen_reader_labels] Do standalone icon buttons,
    custom controls, and images contain explicit semantic labels (e.g.,
    `contentDescription`, `accessibilityLabel`, or `aria-label`)? [Requires
    Code Inspection] [Metric type: Adherence] [Weight: 5]

13. [ID: quality_accessibility_prefers_reduced_motion] Are motion transitions
    configured with reduced-motion queries (e.g.,
    `@media (prefers-reduced-motion: reduce)`) or platform accessibility checks
    and map to M3 motion easing/duration tokens (e.g., `--md-sys-motion-easing-emphasized`
    and `--md-sys-motion-duration-medium2`)? [Requires Code Inspection] [Metric
    type: Adherence] [Weight: 5]

14. [ID: quality_aurora_spark_icon_brand_tint] If AI assistance or generative
    features are present, are they signaled with the standardized Material
    Symbols spark indicator icon (`--md-icon-font: 'Google Symbols'`) with
    appropriate brand color tint (e.g., utilizing M3 tertiary roles like
    `--md-sys-color-tertiary` or generative gradients)? [Metric type: Adherence]
    [Weight: 3]

15. [ID: quality_aurora_streaming_text_container] When AI streaming text output
    is displayed, is the generated content contained within a dedicated
    bounding card or bubble that preserves layout structure and uses M3 shape/color
    roles (e.g., `--md-sys-shape-corner-large` and `--md-sys-color-surface-container`)?
    [Metric type: Hygiene] [Weight: 3]

16. [ID: quality_writing_no_conversational_fluff] Does UI instructional text
    avoid conversational filler words ('please', 'sorry', 'thank you') and
    first-person corporate pronouns ('I', 'we')? [Metric type: Hygiene]
    [Weight: 1]

17. [ID: quality_writing_actionable_error_messages] Do error messages
    explicitly state what problem occurred and provide concrete, actionable
    guidance on how the user can resolve it, styled with the appropriate M3 error roles
    (e.g., `--md-sys-color-error` text or `--md-sys-color-error-container` card)?
    [Metric type: Hygiene] [Weight: 3]

18. [ID: quality_writing_button_cta_verb_length] Do action button CTAs begin
    with an active verb and contain at most 3 words (e.g., 'Save draft',
    'Confirm order', 'Get started')? [Metric type: Hygiene] [Weight: 1]

19. [ID: quality_writing_platform_native_verbs] Does instructional copy adhere
    to platform-native interaction vocabulary (e.g., 'Tap' on iOS/mobile touch
    vs. 'Click' or 'Press' on Web/Desktop)? [Metric type: Adherence] [Weight: 1]

20. [ID: quality_forms_static_field_labels] Do form text fields maintain visible
    static labels or permanent container helper text rather than relying solely
    on disappearing placeholder prompts, adhering to M3 Outlined/Filled Text Field
    anatomy? [Metric type: Hygiene] [Weight: 3]

21. [ID: quality_forms_native_input_types] Are input fields configured with
    appropriate semantic types (e.g., `type="email"`,
    `keyboardType="numeric"`) to invoke the correct input keypad? [Requires Code
    Inspection] [Metric type: Adherence] [Weight: 3]

22. [ID: quality_wayfinding_persistent_search_bar] On search-centric views, is a
    static, discoverable search bar anchored prominently at the top of the
    content area, using the standard M3 search bar shape (`--md-sys-shape-corner-full`)
    and color (`--md-sys-color-surface-container`)? [Metric type: Hygiene]
    [Weight: 5]

23. [ID: quality_adaptive_window_size_classes] Does navigation match the M3 window
    size class: standard bottom navigation bar (`--md-sys-color-surface-container` and
    `--md-sys-shape-corner-full` for active indicators) on compact mobile portrait views, or
    navigation rail / drawer on expanded tablet and desktop views? [Metric type: Adherence]
    [Weight: 10]

24. [ID: quality_adaptive_hinge_foldable_avoidance] If a foldable or dual-screen
    device layout is displayed, are critical interactive controls, dialogs, and
    text columns positioned safely away from the physical center hinge fold, preserving
    appropriate M3 layout structure? [Metric type: Adherence] [Weight: 5]

25. [ID: quality_adaptive_horizontal_overflow_scroll] Does body text and primary
    list content fit within the viewport width without triggering unintended
    horizontal scrollbars? [Metric type: Hygiene] [Weight: 5]

26. [ID: quality_canvas_edge_to_edge_system_bars] Does the primary application
    surface render full-bleed behind transparent or translucent system status
    and navigation bars without artificial opaque letterboxing? [Metric type:
    Adherence] [Weight: 5]

27. [ID: quality_scroll_lift_on_scroll_headers] When content is scrolled under a
    top app bar, does the bar display an elevated tonal color (such as transitioning from
    `--md-sys-color-surface` to `--md-sys-color-surface-container`), subtle shadow line
    (`--md-sys-elevation-level1` / `--md-sys-color-shadow`), or blur to visually
    separate from content? [Metric type: Adherence] [Weight: 3]

28. [ID: quality_scaffolds_canonical_expressive_layouts] For tablet or desktop
    screen widths, does the view implement a recognized Google Material 3 canonical
    multi-pane scaffold (List-Detail, Supporting Pane, or Feed)? [Metric type: Adherence]
    [Weight: 5]
