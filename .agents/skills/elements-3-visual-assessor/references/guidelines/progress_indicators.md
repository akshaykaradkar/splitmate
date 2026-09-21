# Progress & Loading Indicators - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Progress
Indicators (linear bars or circular spinning arcs) and Loading Indicators
communicating ongoing process states across any design system or platform,
followed by Elements GM3 specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

**Progress indicators** and **Loading indicators** are dynamic visual components
used to communicate the real-time status of an ongoing process (e.g., loading an
app, submitting a form, or fetching data). Unlike static icons, they capture
user attention through motion, active track fills, or shape morphing. Agents
must detect these indicators based on their geometry, track division, morphing
sequences, and contextual placement, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Linear Indicators (Horizontal Bars)**: Look for a distinct horizontal bar
    split into two contrasting visual segments: an active filled track
    (representing progress made) and an inactive background track (representing
    remaining progress).
    -   *Stop Marker / Terminus Dot*: A tiny circular dot (e.g., 4dp) marking the
        ending tip of the active colored segment in determinate states.
    -   *Placement*: Typically anchored flush to the extreme top edge of a card,
        dialog, sheet, or page container.
    -   *Media/Playback Integration*: In media players or content cards, a thin
        horizontal line (often blue, red, or brand-colored) is frequently
        positioned at the bottom of a card to show playback progress.
    -   *Button Integration*: Can be embedded directly inside interactive
        buttons (e.g., a thin horizontal progress line at the bottom of a button
        to show associated progress, such as media playback status).
-   **Circular Indicators (Spinning Rings/Arcs)**: Look for a thin circular arc,
    spinning ring, or expanding circular track.
    -   *Button Integration*: Frequently embedded directly inside interactive
        buttons (often replacing the leading icon or text label); typically
        rendered as a monochrome arc matching the text color without an inactive
        background track.
    -   *Placement*: Centered prominently within empty layout spaces, cards, or
        surfaces where content is actively loading.
-   **Determinate vs. Indeterminate Signatures**: Look for two distinct
    behavioral states:
    -   *Determinate (Linear/Circular)*: The active colored track grows steadily
        in one direction, reflecting a known, measurable completion percentage.
    -   *Indeterminate (Linear/Circular)*: The active colored segment
        repeatedly grows, shrinks, or oscillates, communicating an unknown wait
        time.

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected
progress and loading indicators against the following strict standards derived
from CE Elements guidelines:

### Elements GM3 Component Specifications & Sizing

1.  **Strict Prohibition of GM3 Expressive / Wavy Tracks**:
    -   **CE Elements does not adopt GM3 Expressive attributes**. Progress indicators must NOT feature custom wavy or sine-wave patterns, configurable amplitudes, or decorative elements.
    -   Indicators must use the standard flat, horizontal, or circular tracks with solid fills.
2.  **Linear Progress Indicator Specs**:
    -   **Minimum Width**: Strictly 40dp minimum width.
    -   **Stop Indicator / Terminus Dot**: Elements GM3 mandates a 4dp circular end stop indicator at the trailing tip of linear determinate tracks to improve non-text contrast (NTC) accessibility. This is required to ensure GAR compliance regardless of context, as the browser edge itself is not sufficiently contrastive.
    -   **RTL Mirroring**: Must be mirrored horizontally for right-to-left
        languages.
3.  **Circular Progress Indicator Specs**:
    -   **Size Range**: Variable diameter ranging from 24dp (embedded inside
        small buttons) up to 240dp (large loading moments).
    -   **RTL Mirroring**: No horizontal mirroring needed.
4.  **Visual Track Shapes & Roundedness**:
    -   Progress indicators themselves must have cleanly rounded corners or rounded ends.
    -   By default, progress indicator components belong to the **None (0dp)** baseline shape style for their outer positioning container, but the active visual track and background track must feature round ends on their edges.
5.  **Color Roles**:
    -   Progress indicators must be used only for loading primary action processes and must utilize the **Primary** color scheme.
    -   The active indicatorDisplays progress and uses standard primary colors. Keep the loader color consistent across the entire product.
6.  **Duration Rules & Process Hierarchy**:
    -   *Instant (<200ms)*: No indicator should be shown.
    -   *Momentary / Short Activities (2s to 5s)*: Use a circular progress indicator integrated into buttons or icons to show connection with the action and that the button is temporarily disabled.
    -   *Longer Activities (>5s)*: Required for bulk uploads, wait times with high variability, or long processes. These require alternate communication methods such as snackbars or notifications and pairing with descriptive context copy.

### Elements GM3 Accessibility & Keyboard Rules

-   **Contrast Minimums**: The active indicator track must maintain at least a **3:1 contrast ratio** against its background track or underlying surface.
-   **Screen Reader Announcements & ARIA**:
    -   *Default Verbalization*: Indeterminate loaders are announced as "Loading", and determinate loaders are announced with progress percentages. The loader should be placed inside an ARIA live region with `role="status"` and `aria-live="polite"`, and `aria-busy="true"` applied to the loading page section.
    -   *Custom Verbalization*: For long wait times (e.g., bulk uploads), if custom or fractional updates (e.g. "2 of 5 files uploaded") are needed, the loader must **NOT** be placed inside a live region (to prevent duplicate over-announcements). Instead, create a separate hidden live region with `role="status"`, `aria-live="polite"`, and use an `aria-label` to feed the custom verbalization.
    -   *Multiple Loaders*: Avoid using multiple loaders at once. In unavoidable multi-file situations, verbalize only one loader at a time to prevent concurrent verbalization overlap from overwhelming screen reader users.
    -   *Completion Feedback*: Always confirm loading completion (or an error) with a final announcement to reduce user effort in verifying success.

### Critical Elements GM3 Violations to Flag

-   **Expressive / Wavy Tracks**: Using wavy, undulating, or sine-wave tracks instead of straight, flat linear bars or circular arcs.
-   **Missing Stop Indicator / Terminus Dot**: Presenting a linear determinate progress bar without the mandatory 4dp end stop marker.
-   **Multiple Concurrent Screen Reader Verbalizations**: Triggering multiple loaders with simultaneous live regions that overwhelm screen reader users.
-   **Inconsistent Process Loader Styles**: Using a circular loader for a specific action (e.g., refreshing) on one screen, but using a linear loader for the same action elsewhere in the app.
