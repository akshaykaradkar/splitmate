# Menus \& Menu Items - Universal AI Detection Guide \& Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Menus (temporary floating action containers) and Menu Items (individual selectable option rows) across any design system or platform, followed by Elements GM3 specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Menu** is a temporary, floating structural container that displays a list of selectable choices or related actions (known as **Menu items**). Unlike persistent toolbars or navigation bars, menus appear dynamically in response to a user trigger (e.g., tapping an overflow icon, right-clicking an image, or interacting with a dropdown text field). Agents must detect menus based on their temporary modal layer, containment geometry, and listed option rows, regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **Temporary Floating Layer \& Anchoring**: Menus appear on a temporary elevation layer directly in front of all permanent UI elements. They are typically anchored to the triggering element (e.g., sprouting directly below a text field, next to an overflow button, or at the exact cursor location for context menus). If positioned near a screen edge, they automatically invert or shift to avoid being cropped.
-   **Containment \& Visual Boundaries (`Menu`)**: Defined by a distinct outer bounding box (usually a rounded rectangle) featuring a solid background fill and a prominent drop shadow (elevation) separating it from the underlying page content.
-   **Core Anatomy \& Option Rows (`Menu item`)**: Inside the menu container, look for a vertical stack of individual selectable rows (`Menu item`). Each menu item tightly frames its content and follows a standard horizontal slot anatomy:
    -   *Leading Media (Optional)*: An accompanying leading icon, checkmark (for selected states), or miniature avatar.
    -   *Label Text (Required)*: Concise text describing the action or option value.
    -   *Trailing Media (Optional)*: Trailing keyboard shortcut text, navigation chevrons (indicating a submenu), or status icons.
-   **Mandatory Dual Mapping (Menu Container + Child Items)**: When analyzing screens presenting an active menu, agents MUST explicitly identify BOTH the outer menu container (`Menu`) and the individual child choices (`Menu item`, `Text`, or `Icon`). Agents must not conflate the entire menu popup into a single monolithic button or omit the outer container.
-   **Trigger Controls vs. Menus**: Do not classify the *trigger control* (the button, chip, or text field that opens the menu) as a `Menu`. The trigger control must be mapped to its own type (e.g., `Button`, `Chip`, `Text field`, or `Icon button`) based on its visual style. The `Menu` component only exists when the menu is actively open and floating on the screen.

---

## Part 2: Elements GM3 Specifications \& Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected menus and menu items against the following standards derived from Corp Eng guidelines:

### Elements GM3 Menu Anatomy \& Structure

An active Elements GM3 Menu is defined by the following elements:

1.  **Container**: The floating surface. Min width is **112dp**. Default corner radius is **4dp**.
    -   *Special Corner Rule*: If the menu is located directly below a filled text field, the top corner radius must be **0dp**.
2.  **List Items**: Individual horizontal rows within the menu. Standard height is **48dp**.
3.  **Leading Icon (Optional)**: System icon to call attention to important items.
4.  **Trailing Text (Optional)**: Visual shortcut indicators or navigational chevrons.
5.  **Trailing Icon (Optional)**: Arrow for cascading submenus.
6.  **Divider**: Horizontal separator lines. Fixed height of **1dp** with dynamic width and exactly **8dp top and bottom padding**.

### Sizing, Paddings, \& Alignments

-   **Horizontal Alignment**: Menu item text labels must be **start-aligned**.
-   **Vertical Alignment**: Menu item text labels must be **center-aligned**.
-   **Internal Paddings**: Left and right margins inside the menu item are exactly **12dp** (with or without icons).
-   **Item Spacing**: Spacing between individual elements inside a list item row is exactly **12dp**.
-   **Label Length \& Truncation**: Keep labels under **18 characters** to minimize truncation on small viewports. Menu width expands to accommodate the longest text label.

### Interaction \& Accessibility Rules

-   **Persistent Selection States (GAR NTC Compliance)**: To meet Non-Text Contrast (NTC) criteria, a persistent selection state must not be indicated with background or text color alone. A **checkmark icon** must be used in addition to color.
-   **No Mixed Indentation**: If some menu items have leading icons, all other items in that group must remain left-aligned to align with the icons (no mixed visual alignment).
-   **Selection State Icons**: If items in the menu remain selected after closing, avoid placing decorative icons at the start of the list to ensure the checkmark selections are clear and unambiguous.
-   **Multi-Select Menu Behavior**:
    -   Checkboxes (Selected, Unselected, Indeterminate) clearly identify states.
    -   A "Select all" checkbox can be placed at the top to expedite interaction.
    -   Unlike standard menus, multi-select menus **stay open** when items are tapped, dismissing only when clicking outside, or pressing ESC or TAB.
-   **Disabled Menu Items**: Since disabled items remain focusable in menus but cannot be activated, they must include a **tooltip** explaining why the action is disabled to prevent user confusion.
-   **Forbidden Menu Controls**:
    -   **Secondary Interactive Controls**: Direct actions (like embedding switches or custom buttons) inside a menu item are forbidden. The menu role supports only one action per row.
    -   **Standard Buttons**: Action buttons are not supported within menu lists. Replace "Select All" buttons with checkboxes.
    -   **Search Boxes**: Standard search inputs are not recommended inside menus due to accessibility constraints (use an autocomplete combobox input instead).
    -   **Headers \& Footers**: Avoid fixed header or footer panels.
-   **Section Headers**: Section titles are optional. They must **never** be used for groups containing only a single item.
-   **Keyboard \& Focus Navigation**:
    -   Enter or Space activates the triggering button, opens the menu, and automatically moves focus to the first or selected menu item.
    -   Up/Down Arrow keys move focus vertically.
    -   Enter or Space selects the item or toggles its selection state.
    -   ESC closes the menu and returns focus to the trigger control.
    -   TAB closes the menu and moves focus to the next item in tab order.
-   **Aria Roles \& Properties**:
    -   Trigger element requires `aria-haspopup` matching the popup type.
    -   Trigger requires `aria-expanded="true"` when the menu is active.
    -   Use `aria-live` regions to announce visible UI changes resulting from selecting menu items (e.g., "Sorted by date").

### Critical Elements GM3 Violations to Flag

-   **Color-Only Selected State**: Communicating a selected option in a dropdown menu using only text or background container coloring without a checkmark.
-   **Interactive Action Stacking**: Placing secondary interactive toggles, buttons, or links inside a single menu item row.
-   **Incorrect Corner Radii**: Using a non-4dp corner radius, or failing to set a 0dp top corner radius when the menu is positioned beneath a filled text field.
-   **Mixed Indentation**: Failing to left-align text labels in menu items that lack icons when they are grouped with icon-containing items.
-   **Select-All Buttons**: Using text buttons (e.g., "Select All") in menus rather than checkboxes.
-   **Search Input in Menu**: Embedding a search box directly inside a menu dropdown list.
-   **Missing Bounding Boxes**: Failing to capture the outer `Menu` boundary or individual `Menu item` cells.
