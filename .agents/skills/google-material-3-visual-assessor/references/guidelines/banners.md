# Banners - Universal AI Detection Guide & Google Material 3 Specifications

This reference provides universal visual heuristics for detecting Banners (prominent page-level messaging containers) across any design system or platform, followed by Google Material 3 (GM3/MD3) specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Banner** is a prominent, full-width or large-format messaging container used to communicate timely, actionable information or optional alerts without blocking user workflows. Agents must detect banners based on their structural placement and containment qualities, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Anchoring**: Typically placed at the upper portion of a screen or layout region.
    -   *Top-Level Banners*: Anchored directly below the app bar or persistent search bar, spanning the full width of the screen above all body content.
    -   *Inline Banners*: Placed directly within the flow of scrolling body content or above a navigation rail.
-   **Visual Boundaries & Containment**: Banners appear as distinct rectangular containers contrasting with the background, featuring a solid colored fill and clear internal padding.
-   **Core Anatomy**:
    -   *Messaging Content*: A concise text headline and/or body description (1-3 lines), accompanied by an optional leading icon or illustration thumbnail.
    -   *Actions*: Up to two prominent action buttons (often rendered as text buttons) and/or a dedicated close/dismiss icon.

---

## Part 2: Google Material 3 Specifications & Compliance Auditing

When conducting a Google Material 3 adherence audit, evaluate detected banners against the following strict standards:

### Google Material 3 Banner Specifications

-   **Banner Variants & Shapes**:
    -   **Square Basic Banner** (Baseline GM3): Full-width rectangular container with straight corners mapped to `--md-sys-shape-corner-none` (0px). Placed directly below the app bar. No margins. On desktop, buttons are inline; on mobile, buttons wrap below the text.
    -   **Round Basic Banner** (GM3 Expressive): Features rounded corners mapped to `--md-sys-shape-corner-extra-large` (28px). Must have margins of at least `--md-sys-measurement-space200` (16px) from the window edges. Placed above or inline with body content.
    -   **Rich Banner** (GM3 Expressive): Supports 1-3 lines of text and an optional 80x80dp leading image. Always placed inline with scrolling body content. Corner shape is `--md-sys-shape-corner-extra-large` (28px), with at least `--md-sys-measurement-space200` (16px) margins. On mobile, round basic banners should swap to rich banners.
-   **Color Mappings**:
    -   **Standard Color Scheme**: Neutral background using `--md-sys-color-surface-container` (light-dark(#f0f4f9, #1e1f20)) or `--md-sys-color-surface-container-low` (light-dark(#f8fafd, #1b1b1b)). Label text uses `--md-sys-color-on-surface` (light-dark(#1f1f1f, #e3e3e3)).
    -   **Vibrant Color Scheme**: High-emphasis background using `--md-sys-color-primary-container` (light-dark(#d3e3fd, #0842a0)) or secondary/tertiary container roles. Label text maps to `--md-sys-color-on-primary-container` or respective "on-container" roles.
-   **Dismissal & Action Rules**:
    -   All banners must provide a way to be dismissed. A banner can include a close icon button OR up to two dismiss/action buttons, but **never both**.
    -   Buttons must be placed with the secondary action on the left and the primary confirming action on the right. Buttons must not be changed to split buttons or button groups.
    -   The buttons nested within banners are small/XS buttons (using `--md-sys-shape-corner-full` or shape configurations from button specifications).
-   **Behavior & Loading**:
    -   Banners must appear with initial page load. They must not animate in dynamically to push existing content down.
    -   On desktop, square basic banners remain fixed at the top (above main content elevation, `--md-sys-elevation-level1` or higher) and do not scroll. Round/rich banners scroll away inline with content.

### iOS Platform Specifics (iOS 26 Liquid Glass Update)

-   **Shape & Layout**: All iOS banners use an inset and rounded layout (no square variants).
-   **Action Buttons**: Secondary actions in the iOS banner use **outlined** buttons (mapped to `--md-sys-color-outline`) instead of **text** buttons to match iOS pattern conventions.
-   **Glass Effects**: 
    -   *Floating Banners* (remaining fixed as content scrolls behind): exist in the *functional layer* and use Material Glass (semi-transparent container with a specular highlight edge).
    -   *Inline Banners* (scrolling along with content): exist in the *content layer* and use solid surface color tokens without glass effects.

### Critical Google Material 3 Violations to Flag

-   **Redundant Banner Dismissal**: Including both a close "x" icon AND a "Dismiss" text button within the same banner container.
-   **Dynamic Banner Insertion**: Expanding or popping a banner into the UI post-load, causing jarring layout shifts.
-   **Incorrect Corner Radii**: Using sharp (0dp) corners for round basic or rich banners instead of `--md-sys-shape-corner-extra-large` (28px).
-   **Missing Margins on Large Screens**: Expanding a round basic or rich banner to full screen-width without the mandatory 16dp margins from window edges.
