# Dialogs - Universal AI Detection Guide \& MD3 Specifications

This reference provides universal visual heuristics for detecting Dialogs (modal popup windows requesting user decisions or presenting critical information) across any design system or platform, followed by Google Material 3 (M3) specifications and design token bindings for compliance auditing.

---

## Part 1: Universal AI Detection Heuristics

A **Dialog** is a modal popup container appearing in front of app content to provide critical information, alert users, or request a decision. It disables app functionality outside the dialog container until addressed. Agents must detect dialogs based on modality, scrim overlays, and containment geometry, regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **Modality \& Scrim Overlays**: Look for a distinct card or container floating on top of the primary UI, typically accompanied by a background **scrim** (a darkened, semi-transparent overlay layer) that obscures the underlying page content and prevents user interaction outside the container.
-   **Containment \& Shape Geometry**:
    -   *Basic Dialogs*: Floating rectangular cards featuring prominent rounded corners, typically positioned in the center of the screen.
    -   *Full-Screen Dialogs*: Modal containers that expand to cover the entire screen viewport (on compact mobile) or take full width up to a set maximum on larger screens.
-   **Core Anatomy \& Layout Flow**:
    -   *Headline / Title*: Bold, concise title text at the top explaining the dialog's purpose.
    -   *Body Content*: Supporting text description, lists, checkboxes, or form inputs.
    -   *Action Bar*: Action buttons (usually 1 or 2) aligned to the bottom trailing edge (bottom-right quadrant in LTR layouts).
-   **Action Hierarchy**: In a standard two-button confirmation lockup, the primary confirming action (e.g., Save, Confirm) is positioned at the far trailing edge (rightmost button), while the dismissive action (e.g., Cancel) is placed to its left.

---

## Part 2: M3 Specifications \& Compliance Auditing

When conducting a Google Material 3 (M3) adherence audit, evaluate detected dialogs against the following strict standards and exact token bindings:

### M3 Dialog Variants \& Sizing Specs

Evaluate the component structure and visual dimensions based on these M3 standards:

#### 1. Basic Dialog Specifications

-   **Container Shape / Corner Radius**: Must resolve to `--md-sys-shape-corner-extra-large` (`28px`). legacy sharp corners are a direct violation.
-   **Sizing**: Minimum width is 280dp; Maximum width is 560dp. Height is dynamic to wrap content.
-   **Color Roles**:
    -   **Container Background**: `--md-sys-color-surface-container-high` (`light-dark(#e9eef6, #282a2c)`) or `--md-sys-color-surface` (`light-dark(#ffffff, #131314)`).
    -   **Title / Headline Text**: `--md-sys-color-on-surface` (`light-dark(#1f1f1f, #e3e3e3)`).
    -   **Supporting / Body Text**: `--md-sys-color-on-surface-variant` (`light-dark(#444746, #c4c7c5)`).
    -   **Scrim Overlay**: Uses `--md-sys-color-scrim` (`#000000` with standard opacity, usually 32%).
-   **Internal Paddings \& Spacing Tokens**:
    -   **Outer Container Padding**: `--md-sys-measurement-space300` (`24px`) on all sides (Top, Left, Right, Bottom).
    -   **Spacing between Icon and Title**: `--md-sys-measurement-space200` (`16px`).
    -   **Spacing between Title and Body Content**: `--md-sys-measurement-space200` (`16px`).
    -   **Spacing between Body and Action Buttons**: `--md-sys-measurement-space300` (`24px`).
    -   **Gap between Action Buttons**: `--md-sys-measurement-space100` (`8px`).
-   **Internal Alignment**:
    -   *With Icon*: All header elements (Icon, Title) must be **center-aligned**.
    -   *Without Icon*: All header elements must be **start-aligned** (left-aligned in LTR layouts).

#### 2. Full-Screen Dialog Specifications

-   **Container Shape / Corner Radius**: `--md-sys-shape-corner-none` (`0px`).
-   **Sizing**: Fills the entire screen viewport on compact mobile devices; max width of 560dp on wider tablet/desktop screens.
-   **Header Height**: Exactly 56dp (houses a 24dp close "X" icon button and start-aligned title).
-   **Bottom Action Bar Height**: Exactly 56dp.
-   **Paddings**: `--md-sys-measurement-space300` (`24px`) top/left/right padding; elements spaced with a `--md-sys-measurement-space100` (`8px`) gap.

---

### M3 Behavioral \& Action Rules

-   **Action Button Constraints**:
    -   Basic dialogs must contain a **maximum of two actions** (one confirming, one dismissing).
    -   Providing a third action (e.g., "Learn more" or "Help") is strictly not recommended as it distracts and navigiates users away from the focused task. Use inline expansion instead.
    -   If a single button is used, it must be an acknowledgement action (e.g., "Understood").
-   **Action Naming**: Confirming actions must use clear, specific, action-oriented verbs (e.g., "Send", "Create", "Save", "Discard") instead of vague terms like "Done", "OK", or "Close".
-   **Headline Constraints**: Headlines must be concise and avoid apologetic language (e.g., "Sorry for the interruption") or alarmist framing ("Warning!"). On Android, headlines must fit within a maximum of **4 lines** when the text size is scaled to 200%.
-   **Scrolling Behavior**: Dialog content should ideally fit without scrolling. If scrolling is required, the title must remain **pinned at the top** and the action bar **pinned at the bottom** (usually separated from the scrolling body with a 1px `--md-sys-color-outline-variant` divider), ensuring interaction points are never lost.
-   **Focus Flow**: When a dialog is launched, focus must automatically land on the first interactive element inside the container. Keyboard navigation must cycle strictly within the dialog using Tab / Shift+Tab and Escape must dismiss it.

---

### Critical M3 Violations to Flag

-   **Legacy Container Rounding**: Displaying basic dialogs with sharp or minimally rounded corners (e.g., legacy 4px/8px shapes) instead of the mandatory `--md-sys-shape-corner-extra-large` (28px).
-   **Vague Action Wording**: Labeling a primary confirmation action button with non-descriptive text like "OK" or "Done".
-   **Excessive Action Count**: Adding three or more action buttons to the action bar of a basic dialog.
-   **Missing Scrim Overlay**: Presenting a modal dialog without a dark background scrim, failing to isolate user focus.
-   **Scrolling Title/Actions**: Letting the header title or bottom action buttons scroll out of view when body content is scrolled.
