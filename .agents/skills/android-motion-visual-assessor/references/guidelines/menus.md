# Menus & Menu Items - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Menus
(temporary floating action containers) and Menu Items (individual selectable
option rows) across any design system or platform, followed by Android Motion Design System
specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Menu** is a temporary, floating structural container that displays a list of
selectable choices or related actions (known as **Menu items**). Unlike
persistent toolbars or navigation bars, menus appear dynamically in response to
a user trigger (e.g., tapping an overflow icon, right-clicking an image, or
interacting with a dropdown text field). Agents must detect menus based on their
temporary modal layer, containment geometry, and listed option rows, regardless
of design system adherence.

### Key Visual & Geometric Heuristics

-   **Temporary Floating Layer & Anchoring**: Menus appear on a temporary
    elevation layer directly in front of all permanent UI elements. They are
    typically anchored to the triggering element (e.g., sprouting directly below
    a text field, next to an overflow button, or at the exact cursor location
    for context menus). If positioned near a screen edge, they automatically
    invert or shift to avoid being cropped.
-   **Containment & Visual Boundaries (`Menu`)**: Defined by a distinct outer
    bounding box (usually a rounded rectangle) featuring a solid background fill
    and a prominent drop shadow (elevation) separating it from the underlying
    page content.
-   **Core Anatomy & Option Rows (`Menu item`)**: Inside the menu container,
    look for a vertical stack of individual selectable rows (`Menu item`). Each
    menu item tightly frames its content and follows a standard horizontal slot
    anatomy:
    -   *Leading Media (Optional)*: An accompanying leading icon, checkmark (for
        selected states), or miniature avatar.
    -   *Label Text (Required)*: Concise text describing the action or option
        value.
    -   *Trailing Media (Optional)*: Trailing keyboard shortcut text, navigation
        chevrons (indicating a submenu), or status icons.
-   **Mandatory Dual Mapping (Menu Container + Child Items)**: When analyzing
    screens presenting an active menu, agents MUST explicitly identify BOTH the
    outer menu container (`Menu`) and the individual child choices (`Menu item`,
    `Text`, or `Icon`). Agents must not conflate the entire menu popup into a
    single monolithic button or omit the outer container.

-   **Trigger Controls vs. Menus**: Do not classify the *trigger control* (the
    button, chip, or text field that opens the menu) as a `Menu`. The trigger
    control must be mapped to its own type (e.g., `Button`, `Chip`, `Text
    field`, or `Icon button`) based on its visual style. The `Menu` component
    only exists when the menu is actively open and floating on the screen.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion Design System adherence audit, evaluate detected
menus and menu items against the following strict standards derived from
Android Motion guidelines:

### Android Motion Menu Variants & Structural Rules

Android Motion Design System defines explicit menu variants and layout configurations:

1.  **Vertical Menus (Expressive Update)**: Recommended for new designs.
    Features rounded corners, Standard (surface-based) or Vibrant
    (tertiary-based) color styles, shape morphing during focus/selection, and
    refined submenu motion.
2.  **Baseline Menu**: Traditional planar dropdown menu. (Note: Legacy
    distinctions between "dropdown menu" and "exposed dropdown menu" are
    consolidated into `Menu`).
3.  **Context Menus**: Provide actions for a specific element (like an image or
    highlighted text), typically opened via secondary click (right-click or long
    press).
4.  **Adaptive Guidance**: On compact window sizes (mobile devices), products
    should consider adapting complex menus into `Bottom sheet` containers to
    provide adequate touch space and accommodate longer text labels.

### Android Motion Tokens & Layout Measurements

-   **Item Touch Targets & Padding**: Minimum accessible touch target is
    **48x48dp** for each menu item. Standard internal padding must be maintained
    across all custom slots.
-   **Typography**: Menu items use `--droid-sys-typescale-label-large` or `--droid-sys-typescale-body-large` utilizing `'Google Sans Text'`.
-   **Colors & Backgrounds**:
    -   Menu container uses `--droid-sys-color-surface` and utilizes a drop shadow mapped to `--droid-sys-color-shadow` with elevation `--droid-sys-elevation-level2` or `--droid-sys-elevation-level3`.
    -   Menu item text/icon uses `--droid-sys-color-on-surface`.
    -   Leading/trailing elements use `--droid-sys-color-on-surface-variant`.
-   **Grouping & Dividers**:
    -   *Gaps*: Visually divide menu items into distinct groups (highly
        expressive; limited to 1 or 2 gaps per menu; forbidden in scrollable
        menus).
    -   *Dividers*: Subtle 1dp horizontal lines used for scrollable menus or
        text field dropdowns, utilizing `--droid-sys-color-outline` or `--droid-sys-color-surface-variant`. Dividers and gaps cannot receive focus.
-   **Shape**:
    -   Menu container corner radius uses `--droid-sys-shape-corner-extra-small` (4px), `--droid-sys-shape-corner-small` (8px), or `--droid-sys-shape-corner-medium` (12px).
-   **Scrolling**: Menus must display a persistent scrollbar when the list of
    items exceeds the maximum available viewport height.
-   **Submenu Morphing**: As keyboard focus moves between submenus, the corners
    of the focused submenu morph to become more rounded, highlighting the active
    path. On web, submenus utilize a "magic triangle" hover zone to prevent
    accidental closure during diagonal mouse movement.

### Android Motion Interaction & Accessibility Rules

-   **Slot Action Restrictions**: Products must never insert independent
    buttons, switches, or other direct secondary actions into a menu item slot.
    All nested elements within a menu item must perform only a single unified
    action to preserve screen reader and keyboard navigation integrity.
-   **Trigger Button State**: While a menu remains open, the corresponding
    triggering button or icon button must display an active/pressed visual
    state.

### Critical Android Motion Violations to Flag

-   **Action Stacking in Menu Items**: Inserting secondary interactive switches
    or buttons inside a menu item cell, breaking accessibility navigation trees.
-   **Undersized Menu Items**: Shrinking menu item touch targets below the
    mandatory 48x48dp minimum height.
-   **Missing Outer Menu Mapping**: Conflating an active dropdown menu entirely
    into a generic `Custom layout` or failing to classify the outer `Menu`
    container during an audit.
-   **Inconsistent Corner Radius**: Menu containers that do not adhere to standard `--droid-sys-shape-corner-` tokens.
