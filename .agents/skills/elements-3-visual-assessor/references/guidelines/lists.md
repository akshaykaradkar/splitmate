# Lists \& List Items - Universal AI Detection Guide \& Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Lists (outer scrollable vertical containers) and List Items (individual horizontal content rows) across any design system or platform, followed by Elements GM3 specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **List** is a continuous vertical group of text or images organized into distinct horizontal rows called **List items**. They are commonly used for data feeds, settings menus, and navigation trees. Agents must detect lists and list items based on their vertical containment flow, row repetition, and slot structures, regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **Vertical Flow \& Grouping (`List`)**: Look for a primary vertical container grouping multiple related horizontal rows. Lists often occupy the full width of a screen or layout pane and frequently feature a vertical scrollbar or show partial cut-off rows at the bottom.
-   **Row Repetition \& Bounding (`List item`)**: Inside the parent list, look for repeating horizontal rectangular bounding boxes spanning the full width of the list container. Each distinct horizontal row is a `List item`.
-   **Core Anatomy \& Slot Structure**: List items follow a highly predictable horizontal sequence divided into three primary functional slots:
    -   *Leading Slot (Optional)*: Visual anchors on the far left (e.g., circular user avatars, brand icons, video thumbnails, or selection controls like checkboxes/radio buttons).
    -   *Content Slot (Required)*: The central, largest-width area containing primary label text and optional supporting body text or badges.
    -   *Trailing Slot (Optional)*: Visual or interactive indicators on the far right (e.g., navigation chevrons, trailing timestamps, switches, or icon buttons).
-   **Mandatory Dual Mapping (List + List Item)**: When analyzing screens presenting a vertical continuous flow of data rows, agents MUST explicitly identify BOTH the outer container and each individual row:
    -   **Outer Container (`List`) \[MANDATORY\]**: Bound the entire scrollable vertical container that groups all related items and classify it as `List`.
    -   **Individual Rows (`List item`) \[MANDATORY\]**: Exclusively bound every distinct horizontal row within that list and classify it as `List item`. Agents must not stop at identifying just the embedded child widgets (switches, avatars) inside the row; both the parent `List item` bounding box and its embedded controls must be captured.

---

## Part 2: Elements GM3 Specifications \& Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected lists and list items against the following standards derived from Corp Eng guidelines:

### Elements GM3 List Types \& Variants

1.  **Basic Lists**: Static lists used to communicate simple information, consisting of text labels paired with optional icons or supporting visual elements.
2.  **Selection Lists**: Allow users to make selections using text paired with an interactive control:
    -   *Primary Action*: The checkbox/control is both the primary action and state indicator. Contains: Checkbox, optional Overline, Primary Text, and optional Secondary Text.
    -   *Secondary Action*: The selection control is a secondary action on the far right. Contains: optional Supporting Visual (avatar/icon/image), optional Overline, Primary Text, optional Secondary Text, and Checkbox.
3.  **Link Lists**: Lists consisting entirely of text links. Link elements always have a fixed height of **56dp**.
4.  **Complex Lists**: Feature multiple columns. Individual cells are highly versatile, supporting text, links, numbers, buttons, icons, chips, tooltips, and data graphs. Complex lists must always use dividers.

### Elements GM3 Tokens \& Sizing Measurements

-   **List Item Heights \& Densities**:
    -   Height is determined dynamically by the tallest element within the list item.
    -   *1-Line List Item / Link Element*: **56dp** height.
    -   *2-Line List Item*: **72dp** height.
    -   *3-Line List Item*: **88dp** height.
-   **Alignment Rule**:
    -   Elements in a list item are **middle-aligned (vertically centered)** by default.
    -   For heights **\>= 88dp** (such as 3-line configurations), elements must be **top-aligned** to ensure readability.
-   **Paddings \& Spacing**:
    -   *Complex Lists*: Keep **8dp spacing** above and below each element, with **16dp padding** within the cells.
    -   *Label \& Trailing Padding*: Follow standard GM3 spacings. 16dp padding from the container edge LTR.
-   **Touch Targets \& Accessibility**: Minimum accessible touch target is **48x48dp** for any interactive element or secondary control within a list item row.
-   **Dividers**:
    -   Subtle **1dp** dividers separate list items.
    -   Used only as a secondary element when whitespace alone cannot adequately distinguish items (default in Basic/Selection).
    -   **Mandatory** for Complex Lists.

### Typography Guidelines

-   **Primary Text**: Body/Large scale (`--cee3-typescale-body-large-link`) using Google Sans with the `on_surface` color token for hierarchy and Google branding emphasis.
-   **Secondary Text**: Body/Medium scale (`--cee3-typescale-body-medium-link`) using `on_surface_variant` color token.
-   **Overline (Caption)**: Label/Medium scale (`--cee3-typescale-label-medium-link`) using `on_surface_variant` color token. Located above primary text.

### Elements GM3 Interaction \& Accessibility Rules

-   **No Color-Only Selected States**: Relying solely on color is insufficient for accessibility. Selected states must have a second visual signifier, such as a checkmark, checkbox, radio button, or underlined text.
-   **Focus Flow**:
    -   On Tab, initial focus must land on the first list item or the selected element.
    -   Up/Down Arrow keys move focus vertically between list items.
    -   Space or Enter selects/activates the focused item.
-   **Link Underlines in Lists**: For lists with over 20 links, removing link underlines at rest is acceptable to prevent visual clutter, provided underlines reappear on hover or focus.
-   **Scannability \& Mixed Content**: Never mix different content types (e.g., mixing icon-based rows with avatar-based rows) in areas where quick scanning is critical, such as dropdown menus.
-   **ARIA Roles**:
    -   The parent container must utilize the `list` role or semantic HTML (`<ul>`/`<ol>`).
    -   Each horizontal row must utilize the `listitem` role or semantic HTML (`<li>`).
    -   For extremely long lists with dense interactive controls, consider the `grid` ARIA role to reduce tab-stop fatigue.

### Critical Elements GM3 Violations to Flag

-   **Color-Only Selection**: Highlighting a row's background or text color to indicate selection without an accompanying checkmark, checkbox, or icon.
-   **Incorrect Vertical Alignment**: Top-aligning elements in a 1-line or 2-line list item (should be middle-aligned) or middle-aligning elements in a 3-line list item (should be top-aligned).
-   **Action Stacking in Slots**: Placing multiple independent interactive buttons or switches inside a single slot in a way that interferes with screen readers.
-   **Undersized Touch Targets**: Compact list items where interactive controls drop below the mandatory **48x48dp** minimum target boundary.
-   **Missing Bounding Boxes**: Failing to identify both the outer `List` container and the individual `List item` rows in visual audits.
