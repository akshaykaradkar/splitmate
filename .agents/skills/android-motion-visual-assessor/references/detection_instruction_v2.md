# UI Detection & Segmentation Reference Guide

This document provides the authoritative scanning rules required during the
`ui-detector` Chain-of-Thought (CoT) iterative verification process. (For the
authoritative JSON output schema, consult [SKILL.md](../detector/SKILL.md)).

**CRITICAL PRINCIPLE - SYSTEM ADHERENCE IS IRRELEVANT:** The sole purpose of
this detection phase is component identification. Whether a detected component
adheres to Android Motion guidelines is completely irrelevant. Do not filter
out or misclassify components based on whether they follow Android Motion rules.

**CRITICAL PRINCIPLE - VISUAL GEOMETRY OVER FUNCTIONAL INTENT:** Map components
based on their visual structure and container geometry, not their functional
purpose.

*   *Example (Button Groups vs. Tabs):* If a row of controls functions as
    category navigation (like tabs) but is visually styled as adjacent,
    individual pill buttons (where all items have visible solid or tonal
    backgrounds, e.g. connected pills) clustered locally in a header, it
    visually represents a **`Button group`** and MUST be mapped as such. Do not
    map it to `Tabs` based on its navigational function.

**CRITICAL RULE - MANDATORY MICRO-COMPONENT & STRUCTURAL INCLUSION (ABSOLUTE
PARITY):** Do NOT omit any identified component from your final JSON
`components` array out of a belief that it is merely a minor, decorative, or
structural sub-element of a larger container. If your `thoughtProcess` reasoning
confirms the identification of ANY component instance (e.g., stating "Identified
1 vertical divider" or "Identified unread badge"), you MUST explicitly output a
dedicated JSON component object for it in the `components` array. Bypassing or
filtering out smaller structural elements causes critical evaluation failures.

--------------------------------------------------------------------------------

## Part 0: Mandatory Guideline Consultation

After viewing the screenshot, but before compiling the final component list and
generating the JSON output, you MUST consult the detailed visual heuristics for
each suspected component:

1.  List the contents of the [guidelines](./guidelines/) directory.
2.  For every component type you identify or suspect is present on the screen,
    you MUST read its corresponding detailed guideline file (e.g.,
    `./guidelines/fabs.md` if you suspect a FAB) to verify it against specific
    visual heuristics before adding it to the final output.
3.  Do NOT rely on default model knowledge; the heuristics in these guidelines
    are authoritative for this audit.

--------------------------------------------------------------------------------

## Part 1: Core Layout & Scanning Principles

### 1. Root Containers & Status Bars

*   **Root Container:** For **Exhaustive Scans**, you MUST output exactly ONE
    full-screen root container bounding the entire frame. Map it to `"Scaffold"`
    (if standard) or `"Custom layout"` (if customized). This root container is
    critical for holistic layout queries, but it MUST NOT be your only output.
*   **System Elements:** Use the platform status bar (Android/iOS) at the very
    top to help identify layout and components. However, do NOT list the OS
    bottom system gesture handle/home indicator or native system back/home
    buttons as components.

### 2. Anti-Laziness Master Completeness Checklist (CRITICAL SANITY CHECK)

To prevent output length laziness during exhaustive audits, before finishing
your output, you MUST double-check your `components` list against this
structural checklist and ensure all are explicitly mapped:

1.  **List Row Checkboxes:** Are there list rows with selection checkboxes? If
    yes, you MUST output a separate **`Checkbox`** component for each.
2.  **Clickable Icons:** Are there single icons acting as action triggers (such
    as Settings gear, Hamburger Menu, Refresh, Close "X", title dropdown
    selectors, info icons next to status labels, or navigation chevrons)? If
    yes, you MUST map them as **`Icon button`** components.
3.  **Attached Badges:** Are there unread dots, red notification counters,
    status indicators, or protocol version indicators physically attached to or
    overlapping tab headers, nav icons, app bar menu/action icons, or
    graph/topology icons? If yes, you MUST output them as separate **`Badge`**
    components.
4.  **Partially Cut-Off Edge Elements:** Are there elements partially cut-off or
    cropped at the screen edges (bottom or side) to hint at scrollability?
    *   *Vertical (Bottom Edge):* If tab rows, chronological lists, cards, or
        sheets are partially cropped at the bottom viewport edge, you MUST map
        them to standard **`Tabs`**, **`List`** (as the parent container of the
        cropped elements), or **`Bottom sheet`** containers.
    *   *Horizontal (Side Edge):* If image cards, action buttons, or suggestion
        cards are aligned in a horizontal track and are partially cropped at the
        right/left viewport edge (edge peek), or show a horizontal
        scrollbar/indicator, you MUST map the parent horizontal track container
        to **`Carousel`** (in addition to mapping the individual inner items,
        such as `Card`, `Button`, or `Image`).
5.  **Vertical Card Groups (List Containers):** Are there multiple cards
    vertically stacked side-by-side or as peer rows (such as stacked status
    cards, metric cards, or settings cards)? If yes, you MUST map each
    individual card to **`Card`**, AND you MUST map the outer bounding vertical
    container grouping them together to **`List`**. Do NOT omit the parent List
    container.
6.  **Numeric or Text Input Slots (Card-like Inputs):** Are there prominent
    containers (often styled as cards or large boxes) containing large numbers
    or text values accompanied by labels and units? If yes, the outer container
    MUST be mapped to **`Card`**, and the nested value text MUST be mapped to
    **`Text field`** (not just static `Text`). You MUST consult the
    corresponding guideline files (`cards.md` and `text_fields.md`) to verify.
7.  **Bottom Banners & Snackbars (Snackbars):** Is there a horizontal message
    container (either floating near the bottom or full-width and flush with the
    bottom edge, such as a cookie consent banner) containing a brief message and
    optionally a dismiss/action button? If yes, you MUST map it to
    **`Snackbar`**. Do not classify bottom-anchored messaging containers as
    `Card`, `Banner` (which are top-aligned/inline), or custom containers.
8.  **Floating Action Buttons (Floating Actions):** Is there a prominent button (circular or
    pill-shaped) floating over the bottom-right or bottom-center area of the
    screen? If yes, you MUST suspect it as a **`FAB`** or **`Extended FAB`** and
    consult the corresponding guideline file (`fabs.md`) to verify. Do not
    classify it as a `Chip` or standard button.
9.  **Primary Navigation (Bars, Rails, Drawers):** Is there a persistent
    horizontal bar at the bottom, or a vertical sidebar/panel on the left/right
    edge containing top-level menu options? If yes, you MUST classify it as a
    navigation component. **CRITICAL GEOMETRY DISTINCTION:**
    *   **Bottom Bars:** Map to **`Navigation bar`** ONLY if they are full-width
        and docked flush with the bottom edge of the screen. If the bottom bar
        is a **floating pill-shaped container** (not full-width, rounded
        corners, floats above content), map it to **`Toolbar`** (floating
        toolbar) instead, even if it functions as navigation.
    *   **Vertical Sidebars/Panels:** Map to **`Navigation rail`** if it is a
        narrow vertical strip (typically housing only icons, or icons with small
        labels stacked vertically underneath). Map to **`Navigation drawer`** if
        it is a **wide vertical panel** housing icons with text labels placed
        horizontally side-by-side (regardless of whether the panel is permanent
        or collapsible/overlay). You MUST consult the corresponding guideline
        files (`navigation.md` and `toolbars.md`) to verify.
10. **Progress & Loading Indicators:** Is there a horizontal progress bar (often
    a thin colored track line showing completion/playback status, e.g. at the
    bottom of a media card or embedded at the bottom of a button), a horizontal
    dotted/segmented status track (representing a value along a scale), a
    circular spinning arc, or a shape-morphing loading sequence? If yes, you
    MUST suspect it as a **`Progress indicator`** or **`Loading indicator`** and
    consult the corresponding guideline file (`progress_indicators.md`) to
    verify. Do not ignore thin progress track lines or custom status tracks.
11. **Chips & Tags (Filter & Selection Controls):** Are there compact,
    pill-shaped or rounded-rectangular elements used for selections, filtering,
    or input (e.g., date range selectors, category filters, or search tags)? If
    yes, you MUST suspect them as **`Chip`** components and consult the
    corresponding guideline file (`chips.md`) to verify. Do not ignore them or
    misclassify them as standard buttons.
12. **Button Groups (Clustered Actions/Choices):** Are there two or more
    related, independent buttons clustered side-by-side (such as adjacent Volume
    Down/Up, D-pad keys, or a row of pill-shaped category choice buttons where
    every item has a visible background)? **CRITICAL: Even if these buttons
    function as category navigation (like tabs), if they are styled as a row of
    individual pills clustered locally (e.g. in a header inline with other
    elements) rather than spanning the full width of a content pane, you MUST
    map them to `Button group` (not `Tabs`).** If yes, you MUST map the
    individual buttons (e.g. `Filled tonal button`, `Filled button`), AND you
    MUST map the outer bounding layout zone to **`Button group`** (category:
    `Action Components`) and consult `button_groups.md`.
13. **Split Buttons (Divided Actions):** Is there a unified pill-shaped control
    or closely paired button lockup divided into a primary action label on the
    left and a dropdown chevron on the right (either separated by a narrow 2dp
    gap or divided by a thin vertical line inside a single pill container)? If
    yes, you MUST map the outer container to **`Split button`** (category:
    `Action Components`) and consult `split_buttons.md`.
14. **Thin Separating Lines (Dividers):** Are there thin horizontal or vertical
    lines (approx. 1dp/1px thick, often light gray or low contrast) separating
    content blocks, list items, or header/footer areas? If yes, you MUST map
    them to **`Divider`** (category: `Layout & Organization` or `Primitives`)
    and consult `divider.md`. Do not ignore them as decorative-only lines.

--------------------------------------------------------------------------------

## Part 2: Structural Layout & Organization

### 3. App Bars & Toolbars (Global vs. Local Controls)

*   **App Bar (`App bar`):** The global, full-width container positioned at the
    very top of the screen holding branding, search interface, global settings,
    and user avatar.
    *   *Transparent App Bars:* You MUST output the outer bounding container of
        a row of global action buttons (Back arrow, Share, Overflow) at the very
        top of the screen as an **`App bar`**, even if it is fully transparent.
    *   *Minimalist & Branding-focused App Bars:* A top header strip holding the
        primary brand name/text (e.g. app or network name) in the center,
        alongside a user profile avatar or settings control on the sides,
        represents the global **`App bar`**, even if it is fully transparent and
        has no prominent action icons (like back arrows or search). You MUST map
        this bounding header container to **`App bar`**.
*   **Toolbar (`Toolbar`):** A distinct, localized horizontal row of
    context-specific action controls.
    *   **Floating Navigation Toolbar (Non-Standard Usage):** If a bottom
        navigation bar is styled as a floating pill-shaped container (not
        full-width, rounded corners, floats above content) containing
        destination options, you MUST map the outer container to **`Toolbar`**
        rather than `Navigation bar` or `Tabs`, prioritizing visual geometry
        over functional intent.
    *   *Mandatory Parent Co-location:* When a toolbar is present, you MUST
        output BOTH: (1) the outer bounding horizontal container mapped to
        **`Toolbar`**, and (2) each of the individual inner action buttons
        mapped to their specific types (such as `Icon button` or standard button
        types). Do NOT omit the parent Toolbar container.
    *   *Transparent Toolbars:* You MUST output the outer bounding container of
        a horizontal row of localized action controls (such as grouped utility
        icons, media controls, or quick actions) as a **`Toolbar`**, even if it
        is fully transparent and has no visible container border, background, or
        physical boundaries.

### 4. Bottom Sheets, Side Sheets & Panels

*   **Bottom Sheet (`Bottom sheet`):** A prominent surface anchored to the
    bottom of the screen holding supplementary content and actions. Identified
    by rounded top corners (e.g., 28px in the `--droid-sys-shape-corner-extra-large` spec)
    and often a horizontal drag handle divider. You MUST explicitly map the outer container to
    **`Bottom sheet`** in addition to any inner lists, tabs, or buttons.
    *   *Persistent Input Bars vs. Bottom Sheets:* Do NOT identify persistent
        bottom input bars, keyboard docks, or chat footers as bottom sheets.
        These are static input areas and should be mapped to `Toolbar` or custom
        layouts.
*   **Side Sheet / Panel (`Side sheet`):** A persistent structural side panel on
    desktop layouts housing companion flows. You MUST map the panel container to
    **`Side sheet`**.
    *   *Cards inside Side Sheets:* Distinct rounded-corner containers nested
        components. Do NOT omit them as implicit sidebar styling.

### 4a. Primary Navigation & Sidebars

*   **Navigation Drawer (`Navigation drawer`):** A wide, vertical navigation
    panel anchored to the leading (left) or trailing (right) edge of the screen,
    typically containing destinations with icons and text labels presented
    side-by-side (horizontally adjacent) or organized into distinct sections.
    You MUST map this container to **`Navigation drawer`**.
*   **Navigation Rail (`Navigation rail`):** A compact, narrow vertical
    navigation container anchored to the leading (left) edge of tablets or
    desktops. It typically contains 3-7 destination icons stacked vertically,
    where labels (if present) are positioned directly below the icons (never to
    the side).
*   **Drawer vs Rail Distinction:** If the vertical navigation panel features
    text labels placed horizontally next to the icons, or holds grouped sections
    with headers, map it strictly as **`Navigation drawer`**, even if it is
    persistently visible in a wide-screen desktop layout. Do NOT map it as
    `Navigation rail` unless it is a narrow, collapsed vertical bar with stacked
    icon-above-label destinations.

### 5. Lists & Carousels (Continuous Flow Containers)

*   **Lists (`List` and `List item`):** When analyzing a vertical continuous
    flow of data rows, settings options, or menu items, you MUST explicitly map
    BOTH the outer parent list container and each individual row inside it:
    *   *Outer List Container (`List`):* Map the entire vertical bounding area
        to `List`.
    *   *Individual Rows (`List item`):* Every distinct horizontal row within
        that list MUST be individually identified and mapped to `List item`. Do
        NOT substitute or demote a list row mapping to `Text` or `Button` simply
        because it contains text or clickable areas. (Exception: If the row is
        styled as a distinct visual card with rounded corners and its own
        elevated or outlined background, map it as **`Card`** per Section 6).
    *   *Contained Lists (List inside a Card container):* In settings screens,
        lists are often enclosed inside a unified, elevated or outlined
        rounded-corner container (visually a Card) to group related settings. In
        this scenario, you MUST output BOTH: (1) the outer container mapped to
        **`Card`**, and (2) the nested list structure mapped to **`List`**
        (along with its individual **`List item`** children). Do NOT omit the
        parent Card container.
*   **Carousels (`Carousel` and child items):** When horizontal scrolling
    collections, featured spotlights, or content feeds are presented on screen:
    *   *Mandatory Parent Co-location:* When a carousel is present, you MUST
        explicitly output BOTH: (1) the outer bounding horizontal track
        container mapped to **`Carousel`**, and (2) each of the individual inner
        items mapped to their specific types (such as `Image` or `Card`). Do NOT
        omit the parent Carousel container.
    *   *Horizontal Scrollable Action Bars & Galleries:* When action buttons,
        shortcuts, chips, media photo cards, or text/icon data columns are
        aligned horizontally and show clear cues of horizontal
        scrollability—such as one or more items being cut off at the screen edge
        (edge peek) or a horizontal scrollbar indicator—you MUST map the parent
        horizontal track container to **`Carousel`** (in addition to mapping all
        individual child items like buttons, cards, images, or text/icons). This
        applies even if the track otherwise functions as a toolbar, chip group,
        or data table row.
    *   *Media Galleries and Photo Grids:* Horizontal photo gallery blocks
        (frequently positioned at the top of detail cards/sheets, consisting of
        a large primary image container next to one or more smaller, sometimes
        vertically stacked, secondary image cards) represent a horizontal
        scrollable track. You MUST map the outer bounding container of this
        photo gallery/grid block to **`Carousel`** (and map the nested
        images/cards inside as separate child components).
    *   *Low-Content/Empty Carousels:* A carousel containing empty-looking
        cards, placeholder images, or low-contrast containers is still a
        **`Carousel`**. You MUST NOT ignore it merely because the items lack
        text or prominent content. Map the parent track to **`Carousel`** and
        the visible items to **`Card`** or **`Image`**.

### 6. Cards

*   **Cards (`Card`):** Traditional dashboard or list containers displaying
    composite content about a single subject (combining text descriptions,
    icons, or metrics within an elevated or outlined surface featuring rounded
    corners). You MUST classify these as **`Card`** components. Do NOT map them
    as standard `Button` types.

    *   *Do NOT use for floating elements:* Never classify standalone
        bottom-right floating elements as Cards (see Section 7 for FABs).

    *   *Cards inside Lists (Carded Lists):* If a vertical list is styled such
        that each item/row is enclosed within its own distinct, elevated, or
        outlined rounded-corner container (visually a Card), you MUST map each
        row as a **`Card`** component (and the parent container as a
        **`List`**). In this scenario, the card styling takes precedence over
        the generic `List item` mapping to satisfy high-fidelity visual
        expectations.

    *   *Card vs. Button Distinction (Avoid Misclassifying Buttons as Cards):*
        Do not classify a component as a `Card` if it only serves as a single
        action trigger, even if it is large, rectangular, or has rounded
        corners. A Card must contain composite content (e.g. text descriptions,
        metrics, images) or represent a distinct content entity, whereas a
        Button is a single interactive target with a simple label and/or icon
        representing a single action.

    *   *Empty or Placeholder Cards:* Do not ignore card containers even if they
        appear empty, contain only image placeholders, or lack text content. If
        they have a visible rounded-corner boundary or contrasting background
        shape, they MUST be mapped to **`Card`**.

### 6a. Dividers (Structural Separators)

*   **Divider (`Divider`):** Thin visual lines (usually 1dp or 1px thick) used
    to separate content sections, group list items, or establish layout
    boundaries.
    *   *Orientation & Layout:* Dividers can be **horizontal** (separating
        vertical content stacks) or **vertical** (separating
        horizontal/side-by-side regions, such as separating a left navigation
        rail/drawer from the main content pane on tablet/desktop layouts).
    *   *Visibility:* Dividers are often faint and low-contrast (e.g., grey
        lines on black/white backgrounds). Look closely for these subtle
        boundary lines and always map them to **`Divider`** (category: `Layout &
        Organization` or `Containment Components`), ensuring they are not
        omitted.

### 6b. Tabs (Horizontal Navigation & Categories)

*   **Tabs (`Tabs` and `Tab`):** An interactive horizontal component used to
    switch between peer categories, views, or datasets at the same level of
    hierarchy.
    *   **CRITICAL DISTINCTION FROM BUTTON GROUPS:** If the category navigation
        is styled as a row of individual pill buttons (where *every* item has a
        visible background container, e.g. active is filled, inactive are tonal)
        and it is clustered locally (e.g. inline with other header elements)
        rather than spanning the full width of a content pane, you MUST map the
        parent container to **`Button group`** (category: `Action Components`)
        and individual items to standard **`Button`** types, rather than
        `Tabs`/`Tab`.
    *   *Mandatory Parent Co-location:* When tabs are present, you MUST
        explicitly output BOTH: (1) the outer parent horizontal row container
        mapped to **`Tabs`**, and (2) each of the individual peer items inside
        it mapped to **`Tab`**.
    *   *Pill-shaped or Stadium-shaped Tabs (Tabs constructed from Chips):* When
        a category navigation strip is presented as a horizontal row where the
        individual items (such as the active/selected category or all
        categories) are styled with visible pill/stadium-shaped containers
        (either filled background pills or outlined border pills), these items
        visually represent **`Chip`** components (category: `Selection & Input`)
        in addition to their functional role as **`Tab`** components. In this
        scenario, you MUST map BOTH: (1) the active/styled item to a **`Chip`**
        component, and (2) the parent container to **`Tabs`** (with all items as
        **`Tab`** children). Do NOT omit the `Chip` mapping simply because it
        sits in a tab row.

--------------------------------------------------------------------------------

## Part 3: Interactive Toggles, Tags & Indicators

### 7. Buttons (Standard & Action Controls)

Strictly map traditional discrete action controls based on their container
styling:

*   **Traditional Buttons:** Map to `"Filled button"`, `"Filled tonal button"`,
    `"Outlined button"`, `"Elevated button"`, or `"Text button"` based on their
    background solid color, tonal hue, outlined border, elevation shadow, or
    text-only container styling.

    *   *Button vs. Chip Distinction:* Focus on purpose (action vs.
        filter/selection). Do not classify a component as a `Chip` if it
        triggers a primary action or toggles a primary system state (map to
        `Button`). Conversely, do not classify a filter or selection control as
        a `Button` if it is styled like a chip (e.g., compact, rounded
        rectangle) and is used to filter content or select options (map to
        `Chip`), even if it is a standalone dropdown selector (e.g., time range
        filter).
    *   *Embedded Progress:* Look closely at the bottom edge of prominent
        buttons (especially playback or action buttons like "Resume"). If there
        is a thin contrasting horizontal line running along the bottom inside
        edge of the button, this is an embedded progress track. You MUST map it
        as a separate **`Progress indicator`** component in addition to the
        **`Button`** container.

*   **Segmented Button (`Segmented button`):** A connected horizontal row of two
    or more segments (outlined, filled, tonal, or unbordered on a shared
    background track) functioning as a single toggle control to select peer
    options, modes, alignment, or views MUST be classified as a **`Segmented
    button`** parent container. Do NOT map them as standalone chips or standard
    buttons.

*   **Button Group (`Button group`):** A horizontal or vertical grouping of two
    or more standard button/action controls functioning as a unified control
    cluster. When such a button cluster is present, you MUST map the parent
    container to **`Button group`** (category: `Action Components`), in addition
    to mapping all individual button child components. Note: While a `Segmented
    button` represents a connected toggle switcher, a `Button group` represents
    a cohesive cluster of discrete, adjacent action buttons.

*   **Split Button (`Split button`):** A composite button pairing a primary main
    action (text/icon) with an immediately adjacent trailing menu target
    (usually a dropdown chevron). The two targets may be separated by a narrow
    2dp gap or housed within a single unified pill-shaped container divided by a
    thin vertical line. You MUST map the outer container to **`Split button`**
    (category: `Action Components`), in addition to mapping the individual child
    components (the leading button/action, the divider line, and the trailing
    icon button).

*   **FAB & Extended FAB (`FAB` / `Extended FAB`):** Prominent, rounded-corner
    container buttons (circular, pill-shaped, or rounded-square) that are
    anchored and persistently float over the bottom or bottom-right of the page
    content MUST be mapped strictly to **`FAB`** or **`Extended FAB`**.
    *   *FAB vs. Chip Distinction:* Do not classify a floating element as a
        `Chip` if it floats above content and functions as an action trigger.
        Standalone floating elements (pill-shaped or rounded-rectangular with
        text and/or icon) must be mapped to `Extended FAB` (or `FAB`), not
        `Chip`.

### 8. Clickable Icons vs. Static Icons

*   **Clickable Icons (`Icon button`):** Any single icon that acts as an
    interactive touch target to trigger an action (such as the Menu hamburger,
    Settings gear, profile close "x", refresh arrow, dropdown/expansion chevrons
    next to text titles, info icons next to status labels, or navigation
    chevrons next to text rows) MUST be classified exactly as **`Icon button`**.
    *   *Contained / Filled Icon Buttons:* Icon buttons frequently have a
        visible background container (such as a filled circular, rounded-square,
        or pill shape) to increase their visual prominence (e.g., filled or tonal
        icon buttons). If a small container encloses ONLY a single visual icon
        and functions as an interactive action trigger (such as a play button or
        scissor/clip button), you MUST map the container to **`Icon button`**
        (and the nested icon primitive to **`Icon`**), rather than passive
        **`Card`** or custom layouts.

*   **Static Icons (`Icon`):** Decorative, non-interactive icons (such as green
    circular checkmark status indicators, stars, or static warning triangles
    that do not trigger actions) should be mapped as `Icon` (or omitted from
    component lists if minor).

### 9. Toggles & Switches

*   **Switch (`Switch`):** A binary selection control featuring a
    stadium/pill-shaped track and a sliding circular thumb (which may optionally
    contain a small icon, such as a flag or checkmark).
    *   *Visual Distinction from Chips:* A Switch **never contains text labels
        inside its track**, whereas a Chip *always* contains a text label.
    *   *Functional Distinction:* Do NOT misclassify a Switch as a Chip simply
        because it is used as a filter (e.g., toggling a "show flagged only"
        state). If it has the track-and-thumb geometry, it MUST be mapped to
        **`Switch`** (category: `Selection Components` or `Selection & Input`).
    *   *Switches with Embedded Icons:* The sliding circular thumb of a Switch
        frequently contains a small icon (such as a checkmark, cross, chat icon,
        or document icon) to hint at the active/inactive state or function. You
        MUST map this entire control to **`Switch`**, regardless of the presence
        of the embedded icon inside the thumb, and you should NOT map the
        internal icon primitive as a separate standalone icon button or icon.
*   **Toggles:** Any other toggle control should be mapped exactly to
    **`Switch`** if it functionally toggles a primary feature state, even if it
    visually resembles a segmented button container.

### 10. Chips & Tags (Selection & Filter Controls)

*   **Chip (`Chip`):** Compact, pill-shaped or rounded-rectangular interactive
    elements used for selections, filtering, or input.
    *   *Dropdown / Filter Chips:* A chip can feature a trailing dropdown arrow
        icon (e.g., chevron-down) to indicate it opens a menu of options (e.g.,
        date range selectors or filter dropdowns). Do NOT misclassify these as
        generic buttons or menus if they share the compact, pill-like style of
        surrounding chips. Map them to **`Chip`** (category: `Selection & Input`
        or `Selection Components`). Note: Do NOT map the inactive trigger
        control to `Menu`; the `Menu` component only exists when the menu is
        actively open and visible on the screen.

--------------------------------------------------------------------------------

## Part 4: Feedback

### 11. Progress & Loading Indicators

*   **Progress Indicator (`Progress indicator`):** Thin horizontal progress
    lines (often colored) indicating playback/completion status, or horizontal
    status tracks (such as a dotted color scale with a value marker)
    representing a value along a range.
    *   *Embedded Progress:* Do not ignore thin progress lines embedded inside
        other components. A thin progress line at the bottom of a card, or
        integrated inside a button container (e.g., indicating completion or
        playback progress for that action), must be mapped as a separate
        `Progress indicator` component in addition to the parent button or card.
    *   *Discrete Status Tracks:* Horizontal rows of discrete elements (dots,
        segments) acting as a status scale function as progress indicators and
        MUST be mapped to `Progress indicator`.

### 12. Snackbars & Bottom Hint Banners

*   **Snackbar (`Snackbar`):** A horizontal message container positioned at the
    bottom of the screen (either floating with rounded corners or full-width and
    flush with the bottom edge, such as a cookie consent notice) that displays a
    brief message and optionally an action button (e.g., "Hide", "Accept",
    "Undo") MUST be classified as a **`Snackbar`** component (category:
    `Feedback`).
*   Do NOT classify these bottom-anchored notification/consent banners as
    standard `Card`, `Banner` (which are top-anchored or inline), or custom
    containers. Map them strictly to **`Snackbar`**.

--------------------------------------------------------------------------------

## Part 5: Global Theme & Motion Extraction

### 13. Page-Level Palette Extraction

*   **Palette:** Use the `color` skill to extract the page-level color scheme from the screenshot. Match the visual or CSS properties to these key Android Motion variables:
    *   `primaryColor`: Extracts from `--droid-sys-color-primary`
    *   `secondaryColor`: Extracts from `--droid-sys-color-secondary`
    *   `backgroundColor`: Extracts from `--droid-sys-color-background`
    *   `surfaceColor`: Extracts from `--droid-sys-color-surface`
    *   `onSurface`: Extracts from `--droid-sys-color-on-surface`
    *   `onSurfaceVariant`: Extracts from `--droid-sys-color-on-surface-variant`

### 14. Type Scale Extraction

*   **Type Scale:** Identify the list of font families and weights used on the
    screen (e.g., `"Google Sans"`, `"Google Sans Text"`) to populate `typeScale`.

### 15. Motion Profile Extraction

*   **Motion Tokens:** Extract declared timing durations (`--droid-sys-motion-duration-*` from `50ms` to `900ms`) and easing beziers (`--droid-sys-motion-easing-emphasized`, etc.) to confirm dynamic alignment with the Android Motion standard.

--------------------------------------------------------------------------------
