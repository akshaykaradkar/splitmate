# Dividers - Universal AI Detection Guide \& MD3 Specifications

This reference provides universal visual heuristics for detecting Dividers (thin separating lines used to group content or establish hierarchy) across any design system or platform, followed by Google Material 3 (M3) specifications and design token bindings for compliance auditing.

---

## Part 1: Universal AI Detection Heuristics

A **Divider** is a thin visual line or rule used to separate unrelated content, group related items within lists, or establish structural hierarchy across a UI. Agents must detect dividers based on line thickness, orientation, and layout margins, regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **Line Thickness \& Geometry**: Look for continuous, unbroken lines with a fine visual thickness of approximately 1dp (or 1px).
-   **Orientation \& Alignment**: Most dividers are horizontal lines spanning across containers or lists. Vertical dividers are less common but appear in wide desktop/tablet layouts to separate side-by-side content panes or toolbar groupings.
-   **Spacing \& Layout Margins**:
    -   *Full-Width*: Spans 100% of the parent container's width, separating distinct major sections or interactive vs. non-interactive areas.
    -   *Inset*: Features a left indent (e.g., aligning with text margins while bypassing leading avatars/icons), separating related items within a continuous list.
    -   *Middle-Inset*: Features indents on both the left and right sides.
-   **Visual Contrast**: Dividers act as subtle decorative boundaries. They exhibit low visual contrast relative to the background surface, ensuring they do not overpower adjacent text or interactive components.

---

## Part 2: M3 Specifications \& Compliance Auditing

When conducting a Google Material 3 (M3) adherence audit, evaluate detected dividers against the following strict standards and exact token bindings:

### M3 Divider Configurations \& Sizing Specs

Evaluate the physical dimensions and placements of dividers using these specific specifications:

#### 1. Dimensions \& Spacings

-   **Thickness**: Strictly **1dp** height (for horizontal dividers) or 1dp width (for vertical dividers). Thick lines (\> 1dp) are a direct violation.
-   **Full-Width Divider**: Spans exactly **100%** of the parent container width with **0dp** left/right margins.
-   **Inset Divider**: Standard left margin is **16dp** (`--md-sys-measurement-space200`), aligning perfectly with standard list item text padding. Right margin is **0dp**.
-   **Middle-Inset Divider**: Left margin is **16dp** (`--md-sys-measurement-space200`) and right margin is **16dp** (`--md-sys-measurement-space200`).
-   **Vertical Spacing / Margins**:
    -   Space between divider and supporting text: **4dp** (`--md-sys-measurement-space50`).
    -   Divider right margin (or horizontal gap): **8dp** (`--md-sys-measurement-space100`).
    -   Divider bottom margin: **8dp** (`--md-sys-measurement-space100`).

#### 2. Color \& Styling Tokens

-   **Divider Color**: Uses `--md-sys-color-outline-variant` (`light-dark(#c4c7c5, #444746)`) to maintain a subtle, low-contrast boundary.
-   **Contrast Rules**: Classified as decorative elements, so they have **no mandatory WCAG contrast minimums**. They must never use high-contrast colors (like solid black on white), as bold lines disrupt visual flow and hierarchy.
-   **Drop Shadows**: Dividers must remain **completely flat** with `--md-sys-elevation-level0` (0dp elevation) and must **never use drop shadows**.

---

### M3 Contextual Usage Rules

-   **Full-Width Usage**: Use to separate larger, completely unrelated sections of content, or to separate sticky app headers and footers from scrollable body areas.
-   **Inset Usage**: Use inside lists or menu items to separate related content cards or rows.
-   **Spacing Alternative**: List items with repetitive and clean formats can omit dividers entirely, relying on open vertical margins and negative space to imply groupings.
-   **Platform Considerations**:
    -   *Android \& Web*: Standard divider usage is fully supported.
    -   *iOS*: Dividers are **not supported on iOS** in M3 guidelines. Apple Human Interface Guidelines conventions should be preferred for native iOS applications.

---

### Critical M3 Violations to Flag

-   **Heavy/Thick Barriers**: Divider thickness exceeding 1dp (e.g., 2dp-4dp lines), which creates distracting visual anchors.
-   **High Contrast Styling**: Dividers rendered in bold, stark, or dark colors instead of the standard `--md-sys-color-outline-variant`.
-   **Misaligned Insets**: Inset divider margins that do not align with the standard 16dp text padding of the list item (e.g., margins of 8dp or 24dp).
-   **Elevation/Shadow Effects**: Adding drop shadows to dividers, destroying their flat decorative role.
