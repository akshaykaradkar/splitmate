# Button Groups - Universal AI Detection Guide & Google Material 3 Specifications

This reference provides universal visual heuristics for detecting Button Groups (grouped interactive buttons) across any design system or platform, followed by Google Material 3 (GM3/MD3) specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Button group** is a linear cluster of related interactive buttons organized within a shared container or layout arrangement to manage choices or linked actions. Agents must detect button groups based on their distinct geometric and structural qualities, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Arrangement**: Consistently arranged in a single horizontal line. Buttons within a group hug one another closely, share common dividing borders, or are separated by consistent narrow padding. They must move through layouts together without wrapping.
-   **Visual Boundaries & Containment**: Organizes multiple independent button touch targets (each with its own label or icon) inside a shared bounding area. This bounding area can be:
    -   A visible outer container (like a card or outline).
    -   An implicit layout zone (where related buttons are placed adjacent to each other to form a functional block).
    -   A row of pill-shaped choice/category buttons where *every* item has a visible background container (e.g., active is filled, inactive are tonal) and they are clustered locally rather than spanning the screen as tabs. This MUST be mapped to **`Button group`**.
-   **Core Anatomy**:
    -   *Child Buttons*: Individual buttons or icon buttons representing related actions or selectable options.
    -   *Dividers / Spacing*: Uniform internal padding or thin vertical divider strokes separating adjacent button segments.

---

## Part 2: Google Material 3 Specifications & Compliance Auditing

When conducting a Google Material 3 (GM3/MD3) adherence audit, evaluate detected button groups against the following standards:

### Google Material 3 Button Group Specifications

-   **Button Group Variants**:
    -   **Standard Button Group**: Organizes related standalone actions (e.g., Back, Pause, Next). Pressing or selecting a button triggers dynamic width, shape, and padding changes in both that button and adjacent buttons. Unselected buttons are round (`--md-sys-shape-corner-full`); selected toggle buttons morph to square.
    -   **Connected Button Group**: Replaces the legacy Segmented Button (which is no longer recommended). Used for single-select or multi-select choices. Buttons are joined or separated by a consistent `--md-sys-measurement-space25` (2px) padding. Selection does not affect adjacent buttons, only the active button's resting shape morphs between round and square.
-   **Sizing & Layout Rules**:
    -   Works across all Expressive button sizes (XS, S, M, L, XL). By default, all buttons in a group must share the same size and shape, avoiding frequent size mixing (except for rare, designated "hero moments").
    -   *Single-Line Constraint*: Button groups must remain on a horizontal line and **never wrap** to a second line.
-   **Pressed & Selected States (Shape Morphing)**:
    -   Google products should always use a round shape (`--md-sys-shape-corner-full`) for unselected buttons and morph to square for selected buttons.
    -   Square corner radius mappings depend on the button size:
        -   **XS / S**: `--md-sys-shape-corner-medium` (12px)
        -   **M**: `--md-sys-shape-corner-large` (16px)
        -   **L / XL**: `--md-sys-shape-corner-extra-large` (28px)
-   **Minimum Width & Tap Target Specs**:
    -   Each button in a group must maintain a minimum accessible target size of 48x48dp on Android/web, or 44x44pt on iOS.
    -   To meet iOS requirements, standard button groups add extra padding to XS and narrow S sizes. Connected S groups on iOS enforce a 44pt height (vs. 40dp height on Android).

### iOS Platform Specifics (iOS 26 Liquid Glass Update)

-   **Glass Effects (Liquid Glass)**:
    -   When a button group appears in the *functional layer* (floating above content), it should use the glass effect (semi-transparent container and specular edge highlights).
    -   Button groups in the *content layer* must not use the glass effect (relying instead on solid surface tokens).
    -   **No Mixing**: A button group must never mix buttons with and without glass effects in the same group. Either all buttons in the group use the glass effect, or none.

### Critical Google Material 3 Violations to Flag

-   **Button Group Wrapping**: Allowing a button group to wrap onto multiple lines instead of resizing or utilizing flexible width layouts.
-   **Legacy Segmented Buttons**: Using the baseline segmented button component instead of the updated `Connected button group`.
-   **Mixed Button Sizes**: Frequently mixing different button sizes (e.g. combining XS and L buttons) within a single group.
-   **Inconsistent Tap Targets**: Reducing standard inner padding on XS/S groups below the accessible thresholds.
