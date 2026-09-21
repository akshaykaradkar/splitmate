# Split Buttons - Universal AI Detection Guide & MD3 Specifications

This reference provides universal visual heuristics for detecting Split Buttons (paired main action and trailing menu buttons) across any design system or platform, followed by Material Design 3 (MD3) specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Split button** is a composite interactive component that pairs a primary main action button with an immediately adjacent trailing menu button. It reduces visual complexity by hiding secondary related options inside a collapsible menu. Agents must detect split buttons based on button proximity, style uniformity, and trailing chevron icons, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Visual Composition & Proximity**: Look for a prominent main action button (containing a text label, icon, or both) positioned immediately adjacent to a smaller trailing icon button.
    -   *Proximity Gap / Divider*: The two touch targets may be separated by an extremely narrow gap (e.g., 2dp), or they may be housed within a single unified pill-shaped container divided by a thin vertical line. In both cases, they function visually as a single component lockup.
-   **Style Uniformity & Corner Treatment**: Both the leading and trailing targets share the exact same container style (e.g., filled, tonal, outlined, elevated) and color scheme.
    -   *Divided Pill*: If housed in a single pill container, the outer boundary has standard rounded corners (e.g., pill shape), while the inner division is a straight line.
    -   *Adjacent Buttons*: If separated by a 2dp gap, the adjacent "inner" corners facing the gap feature straight edges or reduced corner rounding compared to the fully rounded outer corners.
-   **Trailing Menu Icon**: The trailing button obligatorily features an expand/collapse icon (typically a down chevron or arrow dropdown) that rotates 180° when the menu is active.
-   **Core Anatomy**:
    -   *Leading Button*: The primary action touch target housing concise text (1-2 words) and an optional leading icon.
    -   *Trailing Button*: The secondary menu touch target housing the chevron or dropdown icon.

---

## Part 2: MD3 Specifications & Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected split buttons against the following strict standards derived from Material guidelines and token bindings:

### MD3 Component Specifications & Sizing

-   **Sizing Tiers (Expressive Update)**: MD3 Expressive introduces five standardized height tiers matching common buttons: `Extra Small (XS)`, `Small (S)`, `Medium (M)` (default), `Large (L)`, and `Extra Large (XL)`.
-   **Inner & Outer Corner Shapes**:
    -   **Outer Edges**: Must use fully rounded pill corners (`--md-sys-shape-corner-full`).
    -   **Inner Edges**: Must use flat or straight vertical inner boundaries (`--md-sys-shape-corner-none` or extremely reduced rounding) where the leading and trailing buttons meet.
-   **Color Variants & State Layers**:
    -   **Filled**: High emphasis, utilizing `--md-sys-color-primary` container fill and `--md-sys-color-on-primary` content. Tapping the trailing menu applies a 10% opacity state layer over the primary fill.
    -   **Tonal**: Medium emphasis, utilizing `--md-sys-color-secondary-container` container fill and `--md-sys-color-on-secondary-container` content. Tapping applies a 10% opacity state layer.
    -   **Elevated**: Surface container low fill (`--md-sys-color-surface-container-low`) featuring an elevation drop shadow and `--md-sys-color-primary` content.
    -   **Outlined**: Low emphasis, featuring an `--md-sys-color-outline-variant` border stroke and `--md-sys-color-on-surface-variant` content.
    -   *Selection Distinction*: Unlike toggle buttons, a split button's base container color does not invert or change when selected; only a 10% state layer is applied to the trailing button.
-   **Spacing & Menu Alignment Tokens**:
    -   **Inter-Button Gap**: Strictly fixed at `2dp` (approx. 2px).
    -   **Menu Offset**: The opened menu must be offset by `4dp` from the split button container, typically aligning flush with the trailing button edge.

### MD3 Accessibility & Platform Rules

-   **Touch Targets**: Each button in the split lockup requires a minimum 48x48dp touch target on Android/Web. Small S or XS sizes, which visually have a shorter height, require padded invisible click zones to meet this 48x48dp requirement.
-   **iOS Platform Difference**: On iOS, tap targets for small split buttons match the visual 44x44pt button size natively. Split buttons can use semi-transparent container glass effects when placed in the functional layer (iOS 26 Liquid Glass update).
-   **Labeling**: The trailing button must have an explicit accessibility label or state indicating menu expansion (e.g., "More Watch Options" if the leading button is "Watch Later").

### Critical MD3 Violations to Flag

-   **Inter-Button Gap Widening**: Expanding the gap between the leading and trailing buttons beyond 2dp, breaking the visual lockup.
-   **Mismatched Button Styles**: Pairing a filled leading button with an outlined trailing button, violating style uniformity rules. Both buttons must use identical visual styles.
-   **Undersized Touch Targets**: Shrinking touch targets below 48x48dp (Android/Web) or 44x44pt (iOS) without invisible touch padding.
-   **Missing Accessibility Labels**: Failing to provide a descriptive label for the trailing menu button that links its function to the main button (e.g. "More options").
