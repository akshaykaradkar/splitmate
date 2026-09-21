# Lists \& List Items - Google Material 3 Spec \& Compliance Guide

This guide provides the necessary guidelines and specifications to evaluate the implementation of Lists (outer vertical containers) and List Items (individual content rows) in Google Material 3 (GM3). It is designed to be used by an LLM visual assessor to detect list components, inspect styling in code, and audit compliance with Material Design 3 guidelines.

## Part 1: Universal Visual Detection Heuristics

A **List** is a continuous vertical group of text, icons, images, or selection controls organized into distinct horizontal rows called **List items**. They are optimized for reading comprehension and rapid scanning. Visual assessors must detect lists and list items based on their vertical flow, repeating row structures, and slot compositions, regardless of custom styling or platform.

### Key Visual \& Geometric Heuristics

-   **Vertical Flow \& Grouping (`List`)**: Look for a primary vertical container grouping multiple related horizontal rows. Lists often span the full width of a layout pane or screen, showing partial cut-off rows at the scroll limits or accompanied by a vertical scrollbar.
-   **Row Repetition \& Bounding (`List item`)**: Inside the parent list, look for repeating horizontal rectangular bounding boxes. Each distinct horizontal row is a `List item`.
-   **Core Anatomy \& Slot Structure**: List items follow a highly predictable horizontal sequence divided into three primary functional slots:
    -   *Leading Slot (Optional)*: Visual anchors on the far left (e.g., circular user avatars, brand icons, video thumbnails, or selection controls like checkboxes/radio buttons).
    -   *Content Slot (Required)*: The central, largest-width area containing primary label text and optional supporting body text or badges.
    -   *Trailing Slot (Optional)*: Visual or interactive indicators on the far right (e.g., navigation chevrons, trailing timestamps, switches, or action icon buttons).
-   **Mandatory Dual Mapping (List + List Item)**: When conducting an audit, visual assessors MUST explicitly identify BOTH the outer container and each individual row:
    -   **Outer Container (`List`) \[MANDATORY\]**: Bound the entire scrollable vertical container and classify it as `List`.
    -   **Individual Rows (`List item`) \[MANDATORY\]**: Exclusively bound every distinct horizontal row within that list and classify it as `List item`.

---

## Part 2: GM3 Specifications \& Compliance Auditing

When conducting a Google Material 3 adherence audit, evaluate detected lists and list items against the following strict standards:

### 1. Component Variants \& Visual Styles

Material Design 3 defines two primary structural variations for lists:

-   **Expressive Lists**: Recommended for new designs. Features flexible styling, highlighted selection states (tonal background container fills or shape changes), gaps between items/groups, and customizable slots.
-   **Baseline Lists**: Traditional planar lists. Available as 1-line, 2-line, or 3-line configurations, where height adapts based on text density.
-   **iOS Platform Styles**: UIKit lists styled for Google support three styles:
    -   *Plain*: Full-width cells running edge-to-edge.
    -   *Grouped*: Cells run full width but are divided into distinct groups with sticky headers and a tonal background.
    -   *Inset Grouped*: Cells are fully contained with outer margins on every side, making them easily scannable.

### 2. Sizing, Height \& Padding Measurements

The height of a list item is determined by the tallest element inside it, conforming to these standard heights:

-   **1-Line List Item**: Height **MUST** be exactly **56dp** (leading/trailing elements vertically centered).
-   **2-Line List Item**: Height **MUST** be exactly **72dp** (leading/trailing elements vertically centered).
-   **3-Line List Item**: Height **MUST** be exactly **88dp** (leading/trailing elements are top-aligned rather than centered, with 8dp or 12dp top padding).

#### Padding and Spacing (from `design-system-bindings.css` spacers):

-   **Label Left Padding**: 16dp (`--md-sys-measurement-space200`) from the container edge or leading element.
-   **Trailing Padding**: 16dp (`--md-sys-measurement-space200`) left padding from text; 24dp (`--md-sys-measurement-space300`) right padding from the container edge.
-   **Dividers**: Optional 1dp dividers can separate list items (inset 16dp on the left to align with text labels, 24dp right padding).

### 3. Container Corner Geometry (Contained vs. Uncontained)

-   **Uncontained (Edge-to-Edge) List Items**: Must use a corner radius of **0dp** (`sys.shape.corner.none` / `--md-sys-shape-corner-none`).
-   **Contained (Card/Grouped) List Items**: Must use a corner radius of **12dp** (`sys.shape.corner.medium` / `--md-sys-shape-corner-medium`) or **16dp** (`sys.shape.corner.large` / `--md-sys-shape-corner-large`) to complement other expressive UI elements.

### 4. Color \& Typography Tokens

Ensure that the list item content binds to the correct semantic tokens:

-   **Container Background**: `sys.color.surface` (`--md-sys-color-surface`) or `sys.color.surface-container` (`--md-sys-color-surface-container`).
-   **Selected Container Highlight**: `sys.color.secondary-container` (`--md-sys-color-secondary-container`) or `sys.color.surface-variant` (`--md-sys-color-surface-variant`).
-   **Label Text (Primary)**: `sys.color.on-surface` (`--md-sys-color-on-surface`).
-   **Supporting Text (Secondary)**: `sys.color.on-surface-variant` (`--md-sys-color-on-surface-variant`).
-   **Dividers**: `sys.color.outline-variant` (`--md-sys-color-outline-variant`).

### 5. Media \& Control Best Practices

-   **Avatar vs. Thumbnail Shapes**:
    -   *Avatars* (representing people or entities) **MUST** be circular (`sys.shape.corner.full` / `--md-sys-shape-corner-full`).
    -   *Thumbnails/Images/Videos* (representing products, media, or video feeds) **MUST** be square or rectangular with rounded corners (typically 8dp or 12dp corner radius).
-   **Selection Controls**: Checkboxes, switches, and radio buttons must be positioned at the absolute leading or trailing edge.
    -   *Multi-Select*: Use checkboxes on the leading edge (or trailing switches for settings).
    -   *Single-Select*: Use radio buttons on the leading edge (or checkmarks on iOS).
-   **Secondary Interactive Targets**: Nested interactive elements within trailing slots must have an independent, accessible touch target of at least **48x48dp** (44x44pt on iOS) and perform only a single action to prevent breaking screen reader and keyboard linear navigation.

### Critical GM3 Violations to Flag

1.  **Missing Outer/Inner List Mapping**: Failing to explicitly identify and map both the outer `List` container and the individual `List item` rows.
2.  **Multiple Interactive Actions per Slot**: Overcrowding the trailing slot of a single list item with multiple independent button controls, violating keyboard traversal and screen reader accessibility guidelines.
3.  **Mismatched Media Shapes**: Using square avatars for people or circular thumbnails for products/media.
4.  **Undersized Touch Targets**: Compact list rows where interactive trailing elements fall below the mandatory 48x48dp target.
5.  **Inconsistent Text Alignment**: Varying the horizontal or vertical position of primary/secondary text across different items in the same list.
