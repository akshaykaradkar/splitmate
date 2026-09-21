# Snackbars - Universal AI Detection Guide \& MD3 Specifications

This reference provides universal visual heuristics for detecting Snackbars (temporary bottom-anchored process notifications) across any design system or platform, followed by Material Design 3 (MD3) specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Snackbar** is a concise, temporary messaging container displayed at the bottom of a screen to inform users of a process the app has performed or will perform (e.g., "Message sent", "Item archived"). Unlike modal dialogs, snackbars are non-blocking and do not interrupt the user's ongoing experience. Agents must detect snackbars based on bottom anchoring, non-modal layering, and temporal dismissal, regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **Bottom Anchoring \& Floating/Flush Layer**: Look for a distinct horizontal container positioned at the extreme bottom edge of the viewport. Snackbars typically float directly in front of main page content (above bottom navigation, toolbars, or FABs) but on web or certain desktop views, they can also appear as a full-width banner flush with the bottom and side edges. Despite lacking rounded corners or floating margins, these bottom-flush dismissable banners function as snackbars and MUST be mapped to **`Snackbar`**.
-   **Layout Anatomy \& Action Lockup**: Defined by a solid rectangular bounding box housing a single row lockup: a start-aligned text description on the left, paired with an optional single text button on the far right (e.g., "Undo", "Retry").
-   **Non-Modal Containment**: Snackbars cover only a small, localized portion of the bottom UI. They obligatorily lack a background dimming scrim overlay, allowing users to freely browse and interact with underlying page content while the snackbar remains visible.
-   **Temporal Auto-Dismissal**: Agents analyzing multi-state UI flows should note that snackbars appear suddenly and frequently auto-dismiss after a short duration (e.g., 4 to 10 seconds) if no user action is taken.

---

## Part 2: MD3 Specifications \& Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected snackbars against the following strict standards derived from Material guidelines and token bindings:

### MD3 Component Specifications \& Sizing

-   **Quantity \& Frequency Limits**: Strictly **one** snackbar may be displayed on screen at a time. Stacking multiple snackbars simultaneously is a severe layout violation.
-   **Dimensions \& Padding Tokens**:
    -   *Compact Screens (Mobile)*: Spans full width (minus standard outer side margins). Height is 48dp for 1-line text strings, expanding to 64dp for 2-line text strings.
    -   *Medium / Expanded Screens (Tablet/Desktop)*: Bounded/fixed container width, positioned as a floating box either centered or left-aligned at the bottom of the screen.
-   **Container Shape**: Features a corner radius of 4dp (`--md-sys-shape-corner-extra-small`).
-   **Color \& Theming**: Utilizes a high-contrast, fully opaque background fill (e.g., `--md-sys-color-inverse-surface` container fill paired with `--md-sys-color-inverse-on-surface` text) to ensure prominent visibility against standard surface backgrounds.
-   **Action Button Restrictions**: A snackbar may contain a maximum of **one** action button, which must be formatted as a text button with `--md-sys-color-inverse-primary` color. Inserting icon buttons, filled buttons, or multiple competing action buttons inside the snackbar is strictly prohibited. "Dismiss" or "Cancel" actions (typically via an optional 'x' or ESC key on web) are allowed but must not interfere with the primary action.

### MD3 Component Messaging Hierarchy

Products must select the correct messaging component based on urgency and interruption requirements:

-   **Snackbar**: Low priority, temporary, non-modal, bottom-anchored. Does not require user action.
-   **Banner**: Medium priority, persistent inline banner, usually anchored near the top of the screen below the app bar. Remains on screen until explicitly dismissed.
-   **Dialog**: High priority, modal overlay (blocks underlying page interaction with a dark scrim), centered prominently.

### MD3 Accessibility \& Behavioral Rules

-   **Auto-Dismiss Limitations**: Snackbars without actions can auto-dismiss after 4–10 seconds. However, on the web, auto-dismissing snackbars can present accessibility challenges. Inaccessible auto-dismissal must be resolved by either adding inline feedback (updating nearby elements, e.g. changing "Save" to "Saved") or making the snackbar persistent/actionable.
-   **Focus Behavior**: When a snackbar appears, announce the message using a polite live region (polite queue), but **do not automatically move or trap keyboard focus** inside the snackbar, allowing users to freely navigate.
-   **Keyboard Controls**: On web, users must be able to dismiss an in-focus snackbar using the `Esc` key, and move between interactive elements using `Tab`.

### Critical MD3 Violations to Flag

-   **Simultaneous Snackbar Stacking**: Displaying two or more snackbar containers on screen at the same time.
-   **Prohibited Button Types**: Inserting an `Icon button` or an `Elevated`/`Filled` button inside a snackbar container instead of the required `Text button`.
-   **Exceeding Text Limits**: Overcrowding a snackbar with 3 or more lines of text, violating mobile brevity constraints.
-   **Keyboard Focus Interruption**: Grabbing active keyboard focus or trapping focus when a snackbar appears.
