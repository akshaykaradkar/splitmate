# Banners & Callouts - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Banners and Callouts
(prominent page-level or contextual messaging containers) across any design system or
platform, followed by Elements GM3 specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Banner** (often implemented as an **App-level or Page-level Callout** in Elements GM3) is a prominent, full-width or large-format messaging container used to communicate timely, actionable information, warnings, or confirmations without blocking user workflows. Agents must detect these containers based on their structural placement and containment qualities, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Anchoring**: Typically placed at the upper portion of a screen, layout region, or contextual component area.
    -   *App-Level Banners / Callouts*: Anchored directly below the App Bar (and navigation rail/drawer if persistent), spanning the full width of the screen above all body content.
    -   *Page-Level Banners / Callouts*: Local to a specific page, placed directly within the flow of scrolling body content below the page header.
    -   *Component-Level Banners / Callouts*: Placed locally within or immediately adjacent to a specific section, card, or group of form controls.
-   **Visual Boundaries & Containment**: Banners and callouts appear as distinct rectangular containers contrasting with the background, featuring a solid colored fill, clear internal padding, and optional elevation or thin border strokes.
-   **Core Anatomy**:
    -   *Container*: Bounding box defining the alert area.
    -   *Leading Icon*: Meaningful visual icon representing the alert type (Error, Caution, Info, Success). Meet contrast requirements and never be marked as decorative.
    -   *Messaging Content*: Concise text headline and/or body description (fewer than 200 characters).
    -   *Actions*: Optional dismiss action or up to two prominent action buttons (e.g., text buttons, or filled buttons for extremely important actions).

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected banners and callouts against the following strict standards derived from Elements GM3 guidelines:

### Elements GM3 Banner & Callout Specifications

-   **Callout & Banner Variants**:
    -   **App-Level Callout (Banner)**: Spans the full width of the screen below the App Bar with straight corners (**0dp corner radius, None shape style**). When the page scrolls, it remains fixed along with the App Bar and Navigation Drawer. Always include an action button or close button to allow immediate user dismissal.
    -   **Page-Level Callout**: Local to a specific page. Uses a flat rectangular container with straight corners (**0dp corner radius, None shape style**).
    -   **Component-Level Callout**: Contextual alert positioned near a specific component group or card. Uses a container with rounded corners (**8dp corner radius, Small shape style** / `sys.shape.corner.small`).
-   **Anatomy & Styling**:
    -   **Color Mappings**: Containers must use the designated container background and text/icon colors to maintain a clear 3:1 contrast ratio:
        *   *Error*: Fill uses `--cee3-sys-color-extended-error-container`, and text/icon use `--cee3-sys-color-extended-on-error-container`.
        *   *Caution (Warning)*: Fill uses `--cee3-sys-color-extended-caution-container`, and text/icon use `--cee3-sys-color-extended-on-caution-container`.
        *   *Informational*: Fill uses `--cee3-sys-color-extended-info-container` or `--cee3-sys-color-extended-informational-container`, and text/icon use `--cee3-sys-color-extended-on-info-container` or `--cee3-sys-color-extended-on-informational-container`.
        *   *Success*: Fill uses `--cee3-sys-color-extended-success-container`, and text/icon use `--cee3-sys-color-extended-on-success-container`.
-   **Dismissal & Action Rules**:
    -   Callouts do not require a dismiss action by default, with the exception of **App-level callouts**, which must always include a dismissal mechanism.
    -   If a dismiss control is present, it must feature EITHER an "X" close icon button OR a secondary "Dismiss" text button, but **never both**.
    -   Standard action buttons within the container should use text buttons (unless the action is extremely important, in which case a filled button can be used). These actions are part of the screen's default tab order.
-   **Message Text Formatting**:
    -   Copy must be highly concise and under **200 characters**.
    -   Do not repeat the callout type (e.g. "Warning:" or "Error:") as the first word of the message text. Screen readers automatically verbalize the callout type, making this redundant.
    -   Per Elements GM3 writing guidelines, do not end a single-sentence message with a period.
-   **Grouping & Stacking**:
    -   If multiple callouts or banners are present, they must be stacked and ordered by urgency (Error > Caution > Informational > Success) from top to bottom.
    -   There must be exactly **4dp** of vertical spacing between stacked callout containers.

### Critical Elements GM3 Violations to Flag

-   **Redundant Dismissal Controls**: Including both an "X" close icon button AND a "Dismiss" text button within the same container.
-   **Incorrect Corner Shapes**: App-level or page-level callouts with rounded corners instead of sharp 0dp (None shape style), or component-level callouts with sharp 0dp or incorrect corner radii instead of 8dp (Small shape style).
-   **Redundant Text Prefix**: Starting the message content with redundant words like "Error" or "Warning".
-   **Improper Stack Spacing / Ordering**: Stacking multiple callouts with spacing other than 4dp, or failing to order them by descending urgency.
-   **Punctuation Error**: Placing a period at the end of a single-sentence callout message.
