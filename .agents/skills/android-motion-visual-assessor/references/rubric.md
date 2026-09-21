# Android Motion Rubric (v1.0.0)

--------------------------------------------------------------------------------

## General

### Typography

1.  [ID: typography_sentence_case] Does system-provided UI text appear to comply
    with sentence case by only capitalizing the first word of a sentence in all
    headlines, body text, and labels? Exclude any user-generated content like
    reviews, article titles, article snippets, and text embedded in images.
    [Metric type: Adherence] [Weight: 10]

2.  [ID: typography_display_large] If Display Large
    (droid-sys-typescale-display-large) type class is present, does it comply with the
    specification: font size 3.6rem (approx. 57.6px), line height 4rem (approx. 64px),
    font family 'Google Sans' (or system fallback), and font weight Regular (400)?
    Requires exact multi-property conformance. Approximate sizes or standard system fonts
    (e.g., standard Roboto/Arial) must be evaluated as No. [Metric type:
    Adherence] [Weight: 3]

3.  [ID: typography_display_medium] If Display Medium
    (droid-sys-typescale-display-medium) type class is present, does it comply with
    the specification: font size 2.8rem (approx. 44.8px), line height 3.3rem (approx. 52.8px),
    font family 'Google Sans' (or system fallback), and font weight Regular (400)?
    Requires exact multi-property conformance. [Metric type:
    Adherence] [Weight: 3]

4.  [ID: typography_display_small] If Display Small
    (droid-sys-typescale-display-small) type class is present, does it comply with the
    specification: font size 2.3rem (approx. 36.8px), line height 2.8rem (approx. 44.8px),
    font family 'Google Sans' (or system fallback), and font weight Regular (400)?
    Requires exact multi-property conformance. [Metric type:
    Adherence] [Weight: 3]

5.  [ID: typography_headline_large] If Headline Large
    (droid-sys-typescale-headline-large) type class is present, does it comply with
    the specification: font size 2rem (32px), line height 2.5rem (40px),
    font family 'Google Sans' (or system fallback), and font weight Regular (400)?
    Requires exact multi-property conformance. [Metric type:
    Adherence] [Weight: 3]

6.  [ID: typography_headline_medium] If Headline Medium
    (droid-sys-typescale-headline-medium) type class is present, does it comply with
    the specification: font size 1.8rem (approx. 28.8px), line height 2.3rem (approx. 36.8px),
    font family 'Google Sans' (or system fallback), and font weight Regular (400)?
    Requires exact multi-property conformance. [Metric type:
    Adherence] [Weight: 3]

7.  [ID: typography_headline_small] If Headline Small
    (droid-sys-typescale-headline-small) type class is present, does it comply with
    the specification: font size 1.5rem (24px), line height 2rem (32px),
    font family 'Google Sans' (or system fallback), and font weight Regular (400)?
    Requires exact multi-property conformance. [Metric type:
    Adherence] [Weight: 3]

8.  [ID: typography_title_large] If Title Large (droid-sys-typescale-title-large) type
    class is present, does it comply with the specification: font size 1.4rem (approx. 22.4px),
    line height 1.8rem (approx. 28.8px), font family 'Google Sans' (or system fallback),
    and font weight Regular (400)? Requires exact multi-property conformance. [Metric type: Adherence] [Weight: 3]

9.  [ID: typography_title_medium] If Title Medium (droid-sys-typescale-title-medium)
    type class is present, does it comply with the specification: font size 1rem (16px),
    line height 1.5rem (24px), font family 'Google Sans Text' (or system fallback),
    and font weight Medium (500)? Requires exact multi-property conformance. [Metric type: Adherence] [Weight: 3]

10. [ID: typography_title_small] If Title Small (droid-sys-typescale-title-small) type
    class is present, does it comply with the specification: font size 0.9rem (approx. 14.4px),
    line height 1.3rem (approx. 20.8px), font family 'Google Sans Text' (or system fallback),
    and font weight Medium (500)? Requires exact multi-property conformance. [Metric type: Adherence] [Weight: 3]

11. [ID: typography_body_large] If Body Large (droid-sys-typescale-body-large) type
    class is present, does it comply with the specification: font size 1rem (16px),
    line height 1.5rem (24px), font family 'Google Sans Text' (or system fallback),
    and font weight Regular (400)? Requires exact multi-property conformance. [Metric type: Adherence] [Weight: 3]

12. [ID: typography_body_medium] If Body Medium (droid-sys-typescale-body-medium) type
    class is present, does it comply with the specification: font size 0.9rem (approx. 14.4px),
    line height 1.3rem (approx. 20.8px), font family 'Google Sans Text' (or system fallback),
    and font weight Regular (400)? Requires exact multi-property conformance. [Metric type: Adherence] [Weight: 3]

13. [ID: typography_body_small] If Body Small (droid-sys-typescale-body-small) type
    class is present, does it comply with the specification: font size 0.8rem (approx. 12.8px),
    line height 1rem (16px), font family 'Google Sans Text' (or system fallback),
    and font weight Regular (400)? Requires exact multi-property conformance. [Metric type: Adherence] [Weight: 3]

14. [ID: typography_label_large] If Label Large (droid-sys-typescale-label-large) type
    class is present, does it comply with the specification: font size 0.9rem (approx. 14.4px),
    line height 1.3rem (approx. 20.8px), font family 'Google Sans Text' (or system fallback),
    and font weight Medium (500)? Requires exact multi-property conformance. [Metric type: Adherence] [Weight: 3]

15. [ID: typography_label_medium] If Label Medium (droid-sys-typescale-label-medium)
    type class is present, does it comply with the specification: font size 0.8rem (approx. 12.8px),
    line height 1rem (16px), font family 'Google Sans Text' (or system fallback),
    and font weight Medium (500)? Requires exact multi-property conformance. [Metric type: Adherence] [Weight: 3]

16. [ID: typography_label_small] If Label Small (droid-sys-typescale-label-small) type
    class is present, does it comply with the specification: font size 0.7rem (approx. 11.2px),
    line height 1rem (16px), font family 'Google Sans Text' (or system fallback),
    and font weight Medium (500)? Requires exact multi-property conformance. [Metric type: Adherence] [Weight: 3]

### Color

1.  [ID: color_drop_shadows] If elevation drop shadows are present, are they
    strictly mapped to standard Android Motion elevation tokens (droid-sys-elevation-level1
    through level5) on designated elevated components (e.g., Elevated Card,
    Modal Bottom Sheet, Dialog, FAB) rather than custom/arbitrary CSS box-shadow
    styles on inline elements? [Metric type: Hygiene] [Weight: 10]

2.  [ID: color_error_alerts] Is color used to differentiate error or alert
    elements (such as error messages, input validation, system warnings, or
    notification badges) from standard body text? This applies specifically to
    system errors and critical alerts; do not include general status indicators
    such as stock fluctuations, flight delays, or price changes.
    [Metric type: Hygiene] [Weight: 3]

3.  [ID: color_surface_roles] Does the layout use standard surface color tokens
    to establish tonal hierarchy between body and navigation areas: body content
    container mapped to droid-sys-color-surface (or droid-sys-color-background),
    while navigation bars or rails are mapped to a distinct surface-variant or container
    representation? [Metric type: Adherence] [Weight: 10]

4.  [ID: color_primary_reservation] In static Android Motion baseline color mode, is the
    droid-sys-color-primary role (and primary-container) strictly reserved
    for key focal elements: FABs, at most one filled button, primary action text
    labels, and active selection state indicators? [Metric type: Hygiene] [Weight: 10]

5.  [ID: color_disabled_elements] Are disabled or non-interactive elements
    visually muted or "grayed out" compared to active interactive elements?
    [Metric type: Hygiene] [Weight: 5]

6.  [ID: color_active_element] Is the currently selected or active item (like a
    tab or menu item) marked with a distinct container fill or high-contrast
    color? [Metric type: Hygiene] [Weight: 5]

7.  [ID: color_text_contrast] Are text labels and icons easily legible and
    high-contrast against their colored container backgrounds? [Metric type:
    Hygiene] [Weight: 5]

8.  [ID: color_primary] If Primary is present, does code reference
    droid-sys-color-primary (e.g., CSS: `var(--droid-sys-color-primary)`)
    resolving to baseline hex #0b57d0 (Light) / #a8c7fa (Dark), without hardcoded color overrides?
    Hardcoded hexes, arbitrary brand gradients, or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

9.  [ID: color_on_primary] If text or icons are placed over Primary containers,
    does code strictly bind to droid-sys-color-on-primary (CSS: `var(--droid-sys-color-on-primary)`)
    resolving to baseline hex #ffffff (Light) / #062e6f (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

10. [ID: color_primary_container] If Primary Container is present, does code
    reference droid-sys-color-primary-container (CSS: `var(--droid-sys-color-primary-container)`)
    resolving to baseline hex #d3e3fd (Light) / #0842a0 (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

11. [ID: color_on_primary_container] If text or icons are placed over Primary
    Container, does code bind to droid-sys-color-on-primary-container (CSS: `var(--droid-sys-color-on-primary-container)`)
    resolving to baseline hex #041e49 (Light) / #d3e3fd (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

12. [ID: color_secondary] If Secondary is present, does code reference
    droid-sys-color-secondary (CSS: `var(--droid-sys-color-secondary)`)
    resolving to baseline hex #00639b (Light) / #7fcfff (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

13. [ID: color_on_secondary] If text or icons are placed over Secondary
    containers, does code bind to droid-sys-color-on-secondary (CSS: `var(--droid-sys-color-on-secondary)`)
    resolving to baseline hex #ffffff (Light) / #003355 (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

14. [ID: color_secondary_container] If Secondary Container is present (e.g.,
    selection chips, nav pills), does code reference
    droid-sys-color-secondary-container (CSS: `var(--droid-sys-color-secondary-container)`)
    resolving to baseline hex #c2e7ff (Light) / #004a77 (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

15. [ID: color_on_secondary_container] If text or icons are placed over
    Secondary Container, does code bind to droid-sys-color-on-secondary-container
    (CSS: `var(--droid-sys-color-on-secondary-container)`)
    resolving to baseline hex #001d35 (Light) / #c2e7ff (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

16. [ID: color_tertiary] If Tertiary accents are present, does code reference
    droid-sys-color-tertiary (CSS: `var(--droid-sys-color-tertiary)`)
    resolving to baseline hex #146c2e (Light) / #6dd58c (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

17. [ID: color_on_tertiary] If text or icons are placed over Tertiary
    containers, does code bind to droid-sys-color-on-tertiary (CSS: `var(--droid-sys-color-on-tertiary)`)
    resolving to baseline hex #ffffff (Light) / #0a3818 (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

18. [ID: color_tertiary_container] If Tertiary Container is present, does code
    reference droid-sys-color-tertiary-container (CSS: `var(--droid-sys-color-tertiary-container)`)
    resolving to baseline hex #c4eed0 (Light) / #0f5223 (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

19. [ID: color_on_tertiary_container] If text or icons are placed over Tertiary
    Container, does code bind to droid-sys-color-on-tertiary-container (CSS: `var(--droid-sys-color-on-tertiary-container)`)
    resolving to baseline hex #072711 (Light) / #c4eed0 (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

20. [ID: color_error] If Error state is present, does code reference
    droid-sys-color-error (CSS: `var(--droid-sys-color-error)`)
    resolving to baseline hex #b3261e (Light) / #f2b8b5 (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

21. [ID: color_on_error] If text or icons are placed over Error containers, does
    code bind to droid-sys-color-on-error (CSS: `var(--droid-sys-color-on-error)`)
    resolving to baseline hex #ffffff (Light) / #601410 (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

22. [ID: color_error_container] If Error Container is present (e.g., error
    banners, validation cards), does code reference droid-sys-color-error-container
    (CSS: `var(--droid-sys-color-error-container)`)
    resolving to baseline hex #f9dedc (Light) / #8c1d18 (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

23. [ID: color_on_error_container] If text or icons are placed over Error
    Container, does code bind to droid-sys-color-on-error-container (CSS: `var(--droid-sys-color-on-error-container)`)
    resolving to baseline hex #410e0b (Light) / #f2b8b5 (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

24. [ID: color_surface] If base surface is present, does code reference
    droid-sys-color-surface (CSS: `var(--droid-sys-color-surface)`)
    resolving to baseline hex #fdfcfb (Light) / #1f1f1f (Dark)? Hardcoded hexes, arbitrary brand gradients, or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

25. [ID: color_background] If base background layer is present, does code reference
    droid-sys-color-background (CSS: `var(--droid-sys-color-background)`)
    resolving to baseline hex #fdfcfb (Light) / #1f1f1f (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

26. [ID: color_on_surface] If body typography or primary icons are rendered on
    surfaces, does code bind to droid-sys-color-on-surface (CSS: `var(--droid-sys-color-on-surface)`)
    resolving to baseline hex #1f1f1f (Light) / #e3e3e3 (Dark)? Hardcoded hexes, arbitrary brand gradients, or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

27. [ID: color_on_surface_variant] If secondary text, captions, or muted icons
    are rendered, does code bind to droid-sys-color-on-surface-variant (CSS: `var(--droid-sys-color-on-surface-variant)`)
    resolving to baseline hex #444746 (Light) / #c4c7c5 (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

28. [ID: color_outline] If component borders or text field outlines are present,
    does code reference droid-sys-color-outline (CSS: `var(--droid-sys-color-outline)`)
    resolving to baseline hex #747775 (Light) / #8e918f (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

29. [ID: color_surface_variant] If secondary surfaces or background tracks are present,
    does code reference droid-sys-color-surface-variant (CSS: `var(--droid-sys-color-surface-variant)`)
    resolving to baseline hex #e1e3e1 (Light) / #444746 (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

30. [ID: color_inverse_surface] If transient overlay containers (e.g., Snackbars, tooltips) are present,
    does code reference droid-sys-color-inverse-surface (CSS: `var(--droid-sys-color-inverse-surface)`)
    resolving to baseline hex #303030 (Light) / #e3e3e3 (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

31. [ID: color_inverse_on_surface] If text/icons are placed inside inverse surface containers,
    does code bind to droid-sys-color-inverse-on-surface (CSS: `var(--droid-sys-color-inverse-on-surface)`)
    resolving to baseline hex #f2f2f2 (Light) / #303030 (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

32. [ID: color_inverse_primary] If interactive action labels are rendered inside inverse surfaces,
    does code reference droid-sys-color-inverse-primary (CSS: `var(--droid-sys-color-inverse-primary)`)
    resolving to baseline hex #a8c7fa (Light) / #0b57d0 (Dark)? Hardcoded hexes or unmapped custom colors must be evaluated as No. [Metric type: Adherence] [Weight: 3]

### Layout

1.  [ID: layout_portrait_pane] For a portrait orientation screen (phone, tablet,
    or foldable), is there ONLY 1 layout pane? [Metric type: Hygiene] [Weight: 5]

2.  [ID: layout_landscape_max_panes] For a landscape orientation screen (phone,
    tablet, foldable), are there a maximum of 2 layout panes? [Metric type:
    Hygiene] [Weight: 5]

3.  [ID: layout_desktop_max_panes] For a desktop or ultra-wide screen, are there
    a maximum of 3 layout panes? [Metric type: Hygiene] [Weight: 5]

4.  [ID: layout_visual_alignment] Are related UI elements aligned cleanly along
    shared vertical or horizontal axes? [Metric type: Hygiene] [Weight: 1]

5.  [ID: layout_whitespace_division] Is negative space (whitespace) used to
    divide distinct content blocks rather than relying heavily on line dividers?
    [Metric type: Hygiene] [Weight: 5]

6.  [ID: layout_interactive_separation] Are interactive elements spaced
    appropriately so they don't overlap or collide visually? [Metric type:
    Hygiene] [Weight: 5]

7.  [ID: layout_hardware_clipping] Does the content avoid clipping or
    overlapping device hardware features (like camera notches or home
    indicators)? [Metric type: Hygiene] [Weight: 5]

### Elevation

1.  [ID: elevation_modal_separation] Do floating or temporary modal elements
    (like dialogs, FABs, or toolbars) appear distinctly separated from the
    background content via shadows or scrims? [Metric type: Hygiene] [Weight: 5]

2.  [ID: elevation_tonal_variations] Are adjacent content panes, side sheets,
    and modal containers visually distinguished using Android Motion surface tonal tokens
    (droid-sys-color-surface vs droid-sys-color-surface-variant) or outline tokens
    rather than 1px solid black border dividers? [Metric type: Hygiene] [Weight: 10]

3.  [ID: elevation_avoid_inline_shadows] Do flat inline elements (standard list
    items, text buttons, outlined buttons, uncontained cards) maintain an
    elevation of 0 (droid-sys-elevation-level0 / zero box-shadow) in their default
    resting state? [Metric type: Hygiene] [Weight: 10]

### Motion & Transitions (System Core)

1.  [ID: motion_duration_token_binding] If transitions, animations, or hover/press states are implemented,
    do they strictly bind to standard Android Motion duration tokens (e.g., `var(--droid-sys-motion-duration-100)` to
    `var(--droid-sys-motion-duration-900)`) instead of custom hardcoded millisecond numbers?
    [Metric type: Adherence] [Weight: 10]

2.  [ID: motion_easing_token_binding] Do interactive animations and layouts scale/slide using standard Android Motion curves
    (e.g., `--droid-sys-motion-easing-emphasized`: `cubic-bezier(0.2, 0.0, 0.0, 1.0)`, `--droid-sys-motion-easing-standard`,
    or `--droid-sys-motion-easing-legacy`) rather than browser defaults (`linear`, `ease-in-out`, etc.)?
    [Metric type: Adherence] [Weight: 10]

3.  [ID: motion_hierarchy_coherence] Are hover transitions, touch ripple feedbacks, or indicator selections fast and responsive
    (using duration-50 to duration-150) while larger screen transitions and sheet entries use more visible timings
    (duration-250 to duration-500)? [Metric type: Hygiene] [Weight: 5]

### Accessibility

1.  [ID: accessibility_color_alone] Does the UI avoid using color alone to
    convey meaning, state changes, or interactivity (e.g., using redundant cues
    like icons, text labels, or distinct shapes)? [Metric type: Hygiene]
    [Weight: 5]

2.  [ID: accessibility_interactive_boundaries] Do interactive elements maintain
    sufficient visual boundaries and separation to suggest they are easily
    actionable? [Metric type: Hygiene] [Weight: 5]

3.  [ID: accessibility_text_legibility_over_imagery] If text is layered over
    complex imagery or photos, does a translucent scrim, shadow, or container
    ensure the text remains clearly legible? [Metric type: Hygiene] [Weight: 5]
