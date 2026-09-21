# Button Groups - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Button Groups
(grouped interactive buttons) across any design system or platform, followed by
Elements GM3 specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Button group** is a linear cluster of related interactive buttons organized
within a shared container or layout arrangement to manage choices or linked
actions. Agents must detect button groups based on their distinct geometric and
structural qualities, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Arrangement**: Consistently arranged in a single horizontal
    line. Buttons within a group hug one another closely, share common dividing
    borders, or are separated by consistent narrow padding (e.g., 2dp in
    connected groups). They must move through layouts together without wrapping.
-   **Visual Boundaries & Containment**: Organizes multiple independent button
    touch targets (each with its own label or icon) inside a shared bounding
    area. This bounding area can be:
    -   A visible outer container (like a card or outline).
    -   An implicit layout zone (where related buttons are placed adjacent to
        each other to form a functional block, e.g., Volume Down and Volume Up
        buttons).
    -   A row of pill-shaped choice/category buttons where *every* item has a
        visible background container (e.g., active is filled, inactive are
        tonal) and they are clustered locally (e.g., inline with other header
        elements) rather than spanning the screen as tabs. This MUST be mapped
        to **`Button group`**.
-   **Core Anatomy**:
    -   *Child Buttons*: Individual buttons or icon buttons representing related
        actions (e.g., Back/Pause/Next) or selectable options.
    -   *Dividers / Spacing*: Uniform internal padding or thin vertical divider
        strokes separating adjacent button segments.

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected
button groups against the following strict standards derived from Elements GM3 guidelines:

### Elements GM3 Button Group Specifications

-   **Button Group Variants**:
    -   **Standard Button Group**: Organizes related standalone actions (e.g.,
        Back, Pause, Next). Buttons in a standard button group share identical heights
        (40dp default) and shapes. When grouped, they are placed adjacent to each other with a consistent narrow gap or divider, maintaining a clean visual flow. Individual buttons utilize the **Full shape style (circular/pill-shaped)**.
    -   **Connected Button Group**: Replaces the legacy Segmented Button. Used for
        single-select or multi-select choices. Buttons are joined or separated
        by consistent **2dp** padding.
-   **Sizing & Layout Rules**:
    -   By default, all buttons in a group must share the same size and shape, avoiding frequent size mixing.
    -   *Single-Line Constraint*: Button groups must remain on a single line and
        never wrap.
-   **Platform & Target Size Specs**:
    -   *Android/Web Platform*: Standard button height is 40dp.
    -   *iOS Platform*: Connected groups enforce a 44pt height on iOS (vs. 40dp on Android) to meet Apple's minimum tap target requirements.

### Critical Elements GM3 Violations to Flag

-   **Button Group Wrapping**: Allowing a button group to wrap onto a second
    line instead of adjusting button widths or shrinking container resizing.
-   **Legacy Segmented Buttons**: Flagging legacy segmented buttons and
    recommending the updated `Connected button group` or standard grouped buttons.
-   **Inconsistent Member Sizing**: Mixing buttons of different sizes or shapes
    within the same group.
