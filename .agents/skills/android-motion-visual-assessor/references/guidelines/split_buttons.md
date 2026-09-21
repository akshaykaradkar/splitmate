# Split Buttons - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Split Buttons
(paired main action and trailing menu buttons) across any design system or
platform, followed by Android Motion design system specifications for compliance
auditing.

## Part 1: Universal AI Detection Heuristics

A **Split button** is a composite interactive component that pairs a primary
main action button with an immediately adjacent trailing menu button. It reduces
visual complexity by hiding secondary related options inside a collapsible menu.
Agents must detect split buttons based on button proximity, style uniformity,
and trailing chevron icons, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Visual Composition & Proximity**: Look for a prominent main action button
    (containing a text label, icon, or both) positioned immediately adjacent to
    a smaller trailing icon button.
    -   *Proximity Gap / Divider*: The two touch targets may be separated by an
        extremely narrow gap (e.g., 2dp), or they may be housed within a single
        unified pill-shaped container divided by a thin vertical line. In both
        cases, they function visually as a single component lockup.
-   **Style Uniformity & Corner Treatment**: Both the leading and trailing
    targets share the exact same container style (e.g., filled, tonal, outlined,
    elevated) and color scheme.
    -   *Divided Pill*: If housed in a single pill container, the outer boundary
        has standard rounded corners (e.g., pill shape), while the inner
        division is a straight line.
    -   *Adjacent Buttons*: If separated by a 2dp gap, the adjacent "inner"
        corners facing the gap frequently feature reduced corner rounding
        compared to the fully rounded "outer" corners.
-   **Trailing Menu Icon**: The trailing button obligatorily features an
    expand/collapse icon (typically a down chevron) that rotates 180° when the
    menu is active.
-   **Core Anatomy**:
    -   *Leading Button*: The primary action touch target housing concise text
        (1-2 words) and an optional leading icon.
    -   *Trailing Button*: The secondary menu touch target housing the chevron
        icon.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion design system adherence audit, evaluate detected
split buttons against the following strict standards derived from Android Motion guidelines:

### Android Motion Component Specifications & Sizing

-   **Sizing Tiers**: Android Motion introduces five standardized height tiers
    matching common buttons: `Extra Small (XS)`, `Small (S)`, `Medium (M)` (default),
    `Large (L)`, and `Extra Large (XL)`.
-   **Color Variants & State Layers**:
    -   **Filled**: High emphasis, utilizing `--droid-sys-color-primary` (light-dark(`#0b57d0`, `#a8c7fa`)) container fill. Tapping
        the trailing menu applies a 10% opacity state layer over the primary fill. Content/text uses `--droid-sys-color-on-primary` (light-dark(`#ffffff`, `#062e6f`)).
    -   **Tonal**: Medium emphasis, utilizing `--droid-sys-color-secondary-container` (light-dark(`#c2e7ff`, `#004a77`)) fill.
        Tapping applies a 10% opacity state layer. Content/text uses `--droid-sys-color-on-secondary-container` (light-dark(`#001d35`, `#c2e7ff`)).
    -   **Elevated**: `--droid-sys-color-surface` (light-dark(`#fdfcfb`, `#1f1f1f`)) fill featuring an elevation drop shadow (`--droid-sys-elevation-level1` or `--droid-sys-elevation-level2`). Content uses `--droid-sys-color-primary`.
    -   **Outlined**: Low emphasis, featuring an `--droid-sys-color-outline` (light-dark(`#747775`, `#8e918f`)) border stroke and transparent container fill. Content uses `--droid-sys-color-primary`.
    -   *Selection Distinction*: Unlike toggle buttons, a split button's base
        color does not invert or change when selected; only a 10% state layer is
        applied to the trailing button.
-   **Spacing, Corner Treatment & Menu Alignment Tokens**:
    -   **Inter-Button Gap**: Strictly fixed at `2dp`.
    -   **Corner Treatment**: Outer corners are fully rounded using `--droid-sys-shape-corner-full` (for default sizes), while the adjacent inner corners facing the 2dp gap are straight/unrounded (`--droid-sys-shape-corner-none`).
    -   **Menu Offset**: The opened menu must be offset by `4dp` from the split
        button container, typically aligning flush with the trailing button
        edge.
-   **Accessibility & Platform Rules**:
    -   *Touch Targets*: Each button in the split lockup requires a minimum
        48x48dp touch target. Smaller XS/S sizes require padded invisible click
        zones to meet this requirement.
    -   *iOS Platform*: Split buttons are not natively supported on iOS but can be
        constructed manually. On iOS, small split button targets match the visual 44x44pt button size.
    -   *Labeling*: The trailing button must have an explicit accessibility
        label indicating menu expansion (e.g., "More Watch Options" if the
        leading button is "Watch Later").

### Critical Android Motion Violations to Flag

-   **Inter-Button Gap Widening**: Expanding the gap between leading and
    trailing buttons beyond 2dp, breaking the visual grouping.
-   **Mismatched Button Styles**: Pairing a filled leading button with an
    outlined trailing button, violating style uniformity rules.
-   **Undersized Touch Targets & Missing Labels**: Shrinking touch targets below
    48x48dp (on Android/Web) or failing to provide descriptive accessibility
    labels for the trailing menu button.
