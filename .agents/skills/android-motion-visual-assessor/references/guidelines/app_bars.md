# App Bars - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting App Bars (also
known as top navigation bars, headers, or top bars) across any design system or
platform, followed by Android Motion design system specifications for compliance
auditing.

## Part 1: Universal AI Detection Heuristics

An **App bar** is the primary structural container located at the top of a
screen or pane, dedicated to branding, navigation, and global actions. Agents
must detect app bars based on structural and geometric qualities, regardless of
design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Anchoring**: Always anchored at the extreme top of the screen
    (immediately below the OS status bar, if present) or at the extreme top of a
    distinct structural pane in multi-pane/split-view layouts. It spans the full
    width of its parent screen or pane.
-   **Visual Boundaries & Containment**: App bars can be visually defined by a
    solid background color, a bottom divider line, a subtle elevation shadow, OR
    be completely transparent.
    -   *Minimalist/Transparent Headers*: Even if an app bar lacks a distinct
        background color or border (blending seamlessly into the page
        background), the top layout region grouping the title, back button, or
        global actions MUST be bounded and classified as an `App bar`.
-   **Corner Geometry**: Typically features straight, flush corners meeting the
    screen or pane edges. (Distinct from floating pills or cards).
-   **Core Anatomy (Child Elements)**:
    -   *Leading Controls*: Navigation cues such as a Back arrow, hamburger
        Menu, or close button.
    -   *Title/Branding*: A text headline (page name, app title) or product
        logo/wordmark (left-aligned or centered).
    -   *Trailing Controls*: Action buttons (search, overflow, filter, share,
        settings) or a user profile avatar.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion design system adherence audit, evaluate the detected
app bar against the following strict standards:

### Android Motion Visual & Behavioral Rules

-   **Corner Shape**: Must use **straight corners** with 0px radius (`--droid-sys-shape-corner-none`). Any
    rounding of the outer container indicates an improper container or a
    floating sheet.
-   **Color Shift on Scroll**: In scrolled states, the app bar should transition
    from `Surface` color (`--droid-sys-color-surface`, light-dark(`#fdfcfb`, `#1f1f1f`)) to `Surface Variant` color (`--droid-sys-color-surface-variant`, light-dark(`#e1e3e1`, `#444746`)) or utilize a subtle
    elevation shadow (`--droid-sys-elevation-level1` / 1px or `--droid-sys-elevation-level2` / 3px) to maintain visual separation from scrolling body content.
-   **Action Limits (Trailing Controls)**: Standard app bars should contain a
    maximum of 2 trailing icon buttons, OR 1 specialized button (Filled or
    Tonal) plus an overflow menu. Excessive actions should be moved to a
    secondary toolbar.
-   **Headline Behavior**: Titles must never be truncated with ellipses. Long
    titles should wrap to a second line or utilize collapsing Medium/Large
    variants.
-   **Icon Font & Render**: Interactive icons within the app bar must use the official `--md-icon-font` ('Google Symbols') and be centered inside a standard 48x48dp bounding touch target, rendered at 24x24dp.

### Android Motion App Bar Variants & Typography

| Variant    | Typography Role & Token | Typical Use Case                            |
| :--------- | :---------------------- | :------------------------------------------ |
| **Search** | `--droid-sys-typescale-body-large`<br>(`1rem/1.5rem 'Google Sans Text'`) | Home screens where query entry is primary;<br>houses a search container. |
| **Small**  | `--droid-sys-typescale-title-large`<br>(`1.4rem/1.8rem 'Google Sans'`) | Default compact header for dense layouts or<br>scrolled states. |
| **Medium** | `--droid-sys-typescale-headline-medium`<br>(`1.8rem/2.3rem 'Google Sans'`) | Taller header with expanded title;<br>collapses to Small on scroll. |
| **Large**  | `--droid-sys-typescale-display-small`<br>(`2.3rem/2.8rem 'Google Sans'`) | Most prominent header for top-level<br>screens; collapses to Small on scroll. |

### Critical Android Motion Violations to Flag

-   **Curved Corners**: Any rounding on the app bar container's outer corners (violating `--droid-sys-shape-corner-none`).
-   **Text Truncation**: Ellipses or clipping on the headline text instead of proper wrapping/scaling.
-   **Action Overcrowding**: More than 3 trailing actions cluttering the primary bar.
-   **Incorrect Font Family**: Using standard serif or sans-serif fonts instead of `'Google Sans'` for Small/Medium/Large headlines, or failing to use `'Google Sans Text'` for subtitles or Search text.
