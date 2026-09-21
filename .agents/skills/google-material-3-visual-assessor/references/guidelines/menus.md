# Menus & Menu Items - Google Material 3 Spec & Compliance Guide

This guide provides the necessary guidelines and specifications to evaluate the implementation of Menus (temporary floating containers) and Menu Items (individual option rows) in Google Material 3 (GM3). It is designed to be used by an LLM visual assessor to detect menu components, inspect styling in code, and audit compliance with Material Design 3 guidelines.

## Part 1: Universal Visual Detection Heuristics

A **Menu** is a temporary, floating structural container that displays a list of selectable choices or related actions (known as **Menu items**). Unlike persistent toolbars or navigation bars, menus appear dynamically in response to a user trigger (e.g., tapping an overflow icon, right-clicking an image, or interacting with a dropdown text field). Visual assessors must detect menus based on their temporary modal layer, floating containment geometry, and listed option rows, regardless of custom styling or platform.

### Key Visual & Geometric Heuristics

-   **Temporary Floating Layer & Anchoring**: Menus appear on a temporary elevated layer directly in front of all permanent UI elements. They are typically anchored to the triggering element (e.g., spawning directly below a text field, next to an overflow button, or at the exact cursor location for context menus). If positioned near a screen edge, they automatically invert or shift to avoid being cropped.
-   **Containment & Visual Boundaries (`Menu`)**: Defined by a distinct outer bounding box (usually a rounded rectangle) featuring a solid background fill and a prominent drop shadow (elevation shadow) separating it from the underlying page content.
-   **Core Anatomy & Option Rows (`Menu item`)**: Inside the menu container, look for a vertical stack of individual selectable rows (`Menu item`). Each menu item tightly frames its content and follows a standard horizontal slot anatomy:
    -   *Leading Media (Optional)*: An accompanying leading icon, checkmark (for selected states), or miniature avatar.
    -   *Label Text (Required)*: Concise text describing the action or option value.
    -   *Trailing Media (Optional)*: Trailing keyboard shortcut text, navigation chevrons (indicating a submenu), or status icons.
-   **Mandatory Dual Mapping (Menu Container + Child Items)**: When analyzing screens presenting an active menu, visual assessors MUST explicitly identify BOTH the outer menu container (`Menu`) and the individual child choices (`Menu item` or options).
-   **Trigger Controls vs. Menus**: Do not classify the *trigger control* (the button, chip, or text field that opens the menu) as a `Menu`. The trigger control must be mapped to its own type (e.g., `Button`, `Chip`, `Text field`, or `Icon button`). The `Menu` component only exists when the menu is actively open and floating on the screen.

---

## Part 2: GM3 Specifications & Compliance Auditing

When conducting a Google Material 3 adherence audit, evaluate detected menus and menu items against the following strict standards:

### 1. Component Variants & Structural Rules

Material Design 3 defines explicit menu variants and layout configurations:

-   **Vertical Menus (Expressive Update)**: Recommended for new designs. Features rounded corners, Standard (surface-based) or Vibrant (tertiary-based) color styles, shape morphing during focus/selection, and refined submenu motion.
-   **Baseline Menu**: Traditional planar dropdown menu. Dropdown menus and exposed dropdown menus are consolidated into `Menu`.
-   **Context Menus**: Provide actions for a specific element (like an image or highlighted text), typically opened via secondary click (right-click or long press).
-   **Adaptive Guidance**: On compact window sizes (mobile devices), products should consider adapting complex menus into `Bottom sheet` containers to provide adequate touch space and accommodate longer text labels.

### 2. Color Options & Tokens

Menus have two primary color mappings:

-   **Standard (Surface-Based)**: Mapped to `sys.color.surface-container` (`--md-sys-color-surface-container`) or `sys.color.surface-container-high` (`--md-sys-color-surface-container-high`) with lower visual emphasis. Label text uses `sys.color.on-surface` (`--md-sys-color-on-surface`).
-   **Vibrant (Tertiary-Based)**: Mapped to `sys.color.tertiary-container` (`--md-sys-color-tertiary-container`) and `sys.color.on-tertiary-container` (`--md-sys-color-on-tertiary-container`) with higher visual emphasis. Used sparingly.
-   **Elevation Drop Shadows**: Standard menus must utilize drop shadows corresponding to `sys.elevation.level2` (`--md-sys-elevation-level2` / 3px) or `sys.elevation.level3` (`--md-sys-elevation-level3` / 6px) to float cleanly above underlying content.
-   **iOS 26 Liquid Glass update**: Branded Google iOS menus are styled utilizing translucent Material Glass, which is tuned for increased opacity to enhance legibility while maintaining Google's baseline brand palette.

### 3. Sizing, Paddings & Geometry

-   **Menu Corner Shapes**:
    -   *Baseline Menus*: Use standard rounded corners of 4dp (`sys.shape.corner.extra-small` / `--md-sys-shape-corner-extra-small`).
    -   *Vertical Menus*: Use larger rounded corners of 12dp (`sys.shape.corner.medium` / `--md-sys-shape-corner-medium`) or 16dp (`sys.shape.corner.large` / `--md-sys-shape-corner-large`).
-   **Item Touch Targets & Height**: Height **MUST** be at least **48dp** for each menu item to comply with minimum touch targets. top and bottom padding may decrease under density scaling on Web only.
-   **Gaps & Dividers**:
    -   *Gaps*: Visually divide menu items into distinct groups. Size of the gap must not be changed. Gaps are limited to 1 or 2 per menu and are **forbidden** in scrollable menus. Gaps do not receive focus.
    -   *Dividers*: Subtle 1dp horizontal lines mapped to `sys.color.outline-variant` (`--md-sys-color-outline-variant`). Used for scrollable menus or text field dropdowns. Dividers do not receive focus.
-   **Submenu Corner Morphing**: As keyboard focus moves between submenus, the corners of the active/focused submenu morph to become more rounded, highlighting the active path.
-   **Persistent Scrollbars**: Menus must display a persistent scrollbar when the list of options exceeds the maximum available viewport height. Do not use gaps if a menu scrolls.

### 4. Interaction & Accessibility Rules

-   **No Action Stacking**: Products **MUST NOT** insert independent buttons, switches, or other secondary interactive controls into a menu item slot. All nested elements must perform a single, unified action to preserve screen reader and keyboard navigation integrity.
-   **Selection Cues**: Selected items should change shape and color. For Google products, selected menu items must apply **all three cues**: shape change, color contrast of 3:1 between selected/unselected, and a visual icon cue like a checkmark.
-   **Trigger Button State**: While the menu is open, the triggering element (button, chip, or icon button) must remain visually active with a pressed state layer applied.
-   **Initial Focus**: When a menu opens, keyboard or screen reader focus must automatically land on the **first menu item**, allowing immediate navigation.

### Critical GM3 Violations to Flag

1.  **Action Stacking in Menu Items**: Inserting secondary interactive switches or buttons inside a menu item cell, breaking accessibility navigation trees.
2.  **Undersized Menu Items**: Shrinking menu item height or touch targets below the mandatory 48x48dp minimum.
3.  **Missing Outer Menu Mapping**: Conflating an active dropdown menu entirely into a generic custom layout or failing to classify the outer `Menu` container.
4.  **No Visual Cue on Selection**: Communicating selection in a menu solely via color changes without adding a checkmark or container shape morph.
5.  **Focus Traps or Misses**: Failing to place initial focus on the first menu item upon menu expansion.
