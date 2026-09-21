# Split Buttons - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Split Buttons (paired main action and trailing menu buttons) across any design system or platform, followed by Elements GM3 specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Split button** is a composite interactive component that pairs a primary main action button with an immediately adjacent trailing menu button. It reduces visual complexity by hiding secondary related options inside a collapsible menu. Agents must detect split buttons based on button proximity, style uniformity, and trailing chevron/caret icons, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Visual Composition & Proximity**: Look for a prominent main action button (containing a text label, icon, or both) positioned immediately adjacent to a smaller trailing icon button.
    -   *Proximity Gap / Divider*: The two touch targets may be separated by an extremely narrow gap (e.g., 2px), or they may be housed within a single unified pill-shaped container divided by a thin vertical line. In both cases, they function visually as a single component lockup.
-   **Style Uniformity & Corner Treatment**: Both the leading and trailing targets share the exact same container style (e.g., filled, tonal, outlined, elevated) and color scheme.
    -   *Divided Pill*: If housed in a single pill container, the outer boundary has standard rounded corners (pill shape), while the inner division is a straight vertical line.
    -   *Adjacent Buttons*: If separated by a small gap, the adjacent "inner" corners facing the gap feature straight edges (reduced/no corner rounding) compared to the fully rounded "outer" corners.
-   **Trailing Menu Icon**: The trailing button features an expand/collapse icon (typically a downward arrow or dropdown caret) that triggers a menu.
-   **Core Anatomy**:
    -   *Leading Button*: The primary action touch target housing concise text (1-2 words, sentence case) and an optional leading icon.
    -   *Trailing Button*: The secondary menu touch target housing the downward arrow or dropdown caret icon.

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected split buttons against the following strict standards derived from Elements GM3 guidelines:

### Elements GM3 Component Specifications & Sizing

-   **Container Shape & Geometry**:
    -   **Straight Inner Edges**: The vertical dividing line or inner edge of both the leading and trailing buttons must be completely straight (0px/0dp border-radius).
    -   **Fully Rounded Outer Edges**: The outer corners of the combined split button container must be fully rounded to form a unified pill shape.
-   **Height & Sizing**:
    -   **Height**: Standard Elements GM3 button height is strictly **40dp**.
    -   **Icons**: Standard size for leading and trailing icons inside the buttons is **18dp**, utilizing `--md-icon-font` (`'Google Symbols'`).
-   **Typography**:
    -   **Casing**: The leading button text must utilize **sentence case** (e.g., "Save options", not "SAVE OPTIONS" or "Save Options"). All-caps text is deprecated in Elements GM3.
    -   **Font**: Must use standard typography tokens (e.g., `--cee3-typescale-label-large-link` or similar Google Sans Text styling).
-   **Color Variants & States**:
    -   **Filled**: High emphasis, using primary fill tokens like `--cee3-sys-color-extended-blue` or `--cee3-sys-color-extended-blue-fill`.
    -   **Tonal**: Medium emphasis, using tonal/secondary containers like `--cee3-sys-color-extended-blue-container` or `--cee3-sys-color-extended-grey-container`.
    -   **Outlined**: Low emphasis, utilizing an outline token like `--cee3-sys-color-extended-grey-outline` or `--cee3-sys-color-extended-blue-outline`.
    -   **Divider Stroke**: The vertical dividing line separating the two buttons must be extremely subtle and thin (typically 1dp), utilizing light-dark border tokens.
-   **Accessibility & Touch Targets**:
    -   **Touch Target Size**: Even if the visual height of the buttons is 40dp, the interactive touch target must maintain a minimum **48x48dp** footprint. No density should be applied that reduces this touch target below 48x48dp.

### Critical Elements GM3 Violations to Flag

-   **Mismatched Inner/Outer Corner Rounding**: Rounded inner corners where the leading and trailing buttons meet, or square/insufficiently rounded outer corners, violating the unified pill-shape requirement.
-   **Improper Text Casing**: Using all-caps (ALL CAPS) or title-case (Title Case) instead of the required sentence case for button labels.
-   **Undersized Touch Targets**: Designing the component with touch targets smaller than 48x48dp or applying default density that reduces the accessible click area.
-   **Mismatched Button Styles**: Pairing a filled leading button with an outlined trailing button, violating style uniformity rules.
