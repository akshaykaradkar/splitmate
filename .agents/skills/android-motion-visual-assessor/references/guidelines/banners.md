# Banners - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Banners
(prominent page-level messaging containers) across any design system or
platform, followed by Android Motion design system specifications for
compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Banner** is a prominent, full-width or large-format messaging container used
to communicate timely, actionable information or optional alerts without
blocking user workflows. Agents must detect banners based on their structural
placement and containment qualities, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Anchoring**: Typically placed at the upper portion of a screen
    or layout region.
    -   *Top-Level Banners*: Anchored directly below the app bar or persistent
        search bar, spanning the full width of the screen above all body
        content.
    -   *Inline Banners*: Placed directly within the flow of scrolling body
        content or above a navigation rail.
-   **Visual Boundaries & Containment**: Banners appear as distinct rectangular
    containers contrasting with the background, featuring a solid colored fill
    and clear internal padding.
-   **Core Anatomy**:
    -   *Messaging Content*: A concise text headline and/or body description
        (1-3 lines), accompanied by an optional leading icon or illustration
        thumbnail.
    -   *Actions*: Up to two prominent action buttons (often rendered as text
        buttons) and/or a dedicated close/dismiss icon.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion adherence audit, evaluate detected banners against the following strict standards derived from Android Motion guidelines:

### Android Motion Banner Specifications

-   **Banner Variants**:
    -   **Square Basic Banner**: Full-width rectangular container with straight corners (using `--droid-sys-shape-corner-none`), placed directly below the app bar. On desktop, buttons are inline; on mobile, buttons wrap below the text.
    -   **Round Basic Banner**: Features 28px rounded corners (`--droid-sys-shape-corner-extra-large`) and >=16px outer margins, placed above or inline with body content.
    -   **Rich Banner**: Supports 1-3 lines of text and an optional 80x80px leading image. Always placed inline with scrolling body content. On mobile, round basic banners should swap to rich banners.
-   **Tokens & Measurements**:
    -   **Container Color**: Background uses `--droid-sys-color-surface-variant` or a contrasting surface container.
    -   **Text Color**: Messaging text uses `--droid-sys-color-on-surface-variant`.
    -   **Bottom Border/Divider**: If full-width, a 1px divider using `--droid-sys-color-outline` separates it from content below.
-   **Typography**:
    -   Headline/Message Text: Uses `--droid-sys-typescale-body-large` (1rem/1.5rem `'Google Sans Text'`) or `--droid-sys-typescale-title-medium` (500 1rem/1.5rem `'Google Sans Text'`).
    -   Action Buttons: Use `--droid-sys-typescale-label-large` (500 0.9rem/1.3rem `'Google Sans Text'`).
-   **Dismissal & Action Rules**:
    -   All banners must provide a way to be dismissed. A banner can include a close icon button OR up to two dismiss/action buttons, but **never both**.
    -   Buttons must be placed with the secondary action on the left and the primary confirming action on the right. Buttons must not be changed to split buttons or button groups.
-   **Behavior & Loading (Android Motion Exclusives)**:
    -   Banners should ideally appear with initial page load.
    -   If a banner is inserted dynamically (e.g. system connectivity warning), it must transition in using a smooth expansion:
        -   **Duration**: `--droid-sys-motion-duration-300` (300ms) or `--droid-sys-motion-duration-350` (350ms).
        -   **Easing**: `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`).

### Critical Android Motion Violations to Flag

-   **Redundant Banner Dismissal**: Including both a close "x" icon AND a "Dismiss" text button within the same banner container.
-   **Dynamic Banner Insertion without Easing**: Popping a banner into the UI post-load without smooth `--droid-sys-motion-easing-emphasized` transition, causing jarring layout shifts.
-   **Non-compliant Typography**: Using non-standard fonts instead of `'Google Sans Text'`.
