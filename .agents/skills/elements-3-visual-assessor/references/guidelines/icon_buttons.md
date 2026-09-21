# Icon Buttons - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Icon Buttons (compact interactive controls featuring a standalone icon) across any UI, followed by Elements GM3 design system specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

An **Icon Button** is a compact interactive target featuring a standalone central icon without accompanying label text. They allow users to execute common actions (e.g., bookmarking, filtering, opening menus, editing, deleting) with a single tap. Agents must detect icon buttons based on their central icon isolation, container styling, and padded touch zones, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Central Icon Isolation**: Look for a single, perfectly centered system icon (e.g., magnifying glass, heart, bookmark, three dots, pencil, trash) with absolutely no accompanying text label inside the control boundary.
-   **Container Styling & Visual Boundaries**: Detect two primary representations:
    -   *Contained Container*: A solid colored or bordered background shape (circle or rounded square) enclosing the icon.
    -   *Standard (Isolated) Icon*: Lacking any visible container at rest; the container and background state layers become visible only upon hover, focus, or press.
-   **Toggle State Signatures**: Toggle icon buttons represent binary states (e.g., favorited vs. unfavorited, bookmark on/off). Look for two distinct visual state changes:
    -   *Style Shift*: Outlined icon (unselected) switching to a solid filled icon (selected).
    -   *Weight Shift*: Regular stroke weight (unselected) switching to a semibold stroke weight (selected) as a fallback when no filled vector exists.
-   **Interactive Area & Touch Targets**: Smaller visual variants have visual bounding boxes that are smaller than their minimum accessible touch targets. Sighted targets must remain at least 48x48dp.

---

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected icon buttons against the following strict standards:

### Component Variants & Sizing

Elements GM3 establishes specific visual styles and dimensions:

-   **Icon Button Types**:
    -   **Standard Icon Button**: Lowest emphasis, featuring an isolated `On Surface Variant` icon with no container visible at rest.
    -   **Contained Icon Button**: Used when more visual separation from the background is required. Includes:
        1.  **Filled**: Highest emphasis, using a `Primary` container fill and `On Primary` icon. Used sparingly for high-impact key actions.
        2.  **Filled Tonal**: Medium emphasis, using a `Secondary Container` fill and `On Secondary Container` icon. Ideal for secondary actions paired with high-emphasis buttons.
        3.  **Outlined**: Medium emphasis, featuring an `Outline Variant` border stroke. Useful for browsing card sets or secondary toolbar actions.
-   **Component Sizing**:
    -   **Icon Size**: Standard size is **24dp** (with a absolute minimum of 20dp).
    -   **Container Size**: Standard size is **40dp** (for contained styles).
    -   **Touch Target Size**: Must be at least **48x48dp** by default.
-   **Spacing**:
    -   Use **4dp** spacing between separate adjacent elements.
    -   Use **0dp** spacing when elements are nested next to each other inside a parent container.

### Toggle Selection & Compliance

-   **NTC (No Tech-only Cue) Compliance**: Change in color alone is NOT sufficient to communicate selection to visually impaired users.
-   **Visual Selection Cues**:
    -   Toggle icon buttons must use an **outlined icon** for the inactive (untoggled) state and a **filled icon** for the active (toggled) state.
    -   If a Google Symbols icon is used that only exists in a single style (outline only or filled only), the active state must change the icon's weight to **semibold** upon selection.
    -   An optional background shape with a `secondary-container` color can be added as an additional visual cue for active states.

### Tooltips, Density & Accessibility

-   **Tooltips on Hover**: Icon buttons must display a plain tooltip on hover that describes the action (e.g., "Add to favorites") rather than the literal icon name.
    -   Plain tooltips are positioned exactly **4dp** above the target area.
    -   If the button is in a top app bar, the tooltip appears **4dp** below the element.
-   **No Default Density**: Do not apply density by default to icon buttons, as it shrinks interactive touch targets below the mandatory 48x48dp minimum. Keep density setting at 0 by default, allowing user-discretion theme overrides.
-   **Role Definition**: ARIA role is set to `button` by default. If the icon button navigates to a new page, annotate `Role = Link`.
-   **External Links**: If an icon button opens a new window or tab, append the accessibility label with "opens new window" and define the role as `link`.
-   **Disabled Buttons**:
    -   *Hard Disabled*: Pagination first/previous page buttons when on the first page. These are removed from the tab order.
    -   *Soft Disabled*: Can have a tooltip explanation on hover/focus to educate users on why the button is inactive.

### Critical Elements GM3 Violations to Flag

-   **Color-Only Toggles**: Indicated selection change solely through a color shift, failing NTC standards.
-   **Default High Density**: Shrunk touch targets below 48x48dp by applying density settings by default.
-   **Missing Tooltips**: Presenting icon buttons without on-hover tooltips explaining the action.
-   **Incorrect Icon Weight/Fill Fallback**: Failing to change icon weight to semibold for single-variant toggle icons.
