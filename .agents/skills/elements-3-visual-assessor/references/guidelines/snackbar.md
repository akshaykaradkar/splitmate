# Snackbars - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Snackbars (temporary bottom-anchored process notifications) across any design system or platform, followed by Elements GM3 specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Snackbar** is a concise, temporary messaging container displayed at the bottom of a screen to inform users of a process the app has performed or will perform. Unlike modal dialogs, snackbars are non-blocking and do not interrupt the user's ongoing experience. Agents must detect snackbars based on bottom anchoring, non-modal layering, and temporal dismissal, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Bottom Anchoring & Floating/Flush Layer**: Look for a distinct horizontal container positioned at the extreme bottom edge of the viewport. Snackbars typically float directly in front of main page content (above bottom navigation or FABs). They can also appear as a full-width banner flush with the bottom and side edges on web layouts.
-   **Layout Anatomy & Action Lockup**: Defined by a solid rectangular bounding box housing a single row lockup: a start-aligned text description on the left, paired with an optional single text button on the far right (e.g., "Undo", "Retry"), and an optional close button.
-   **Non-Modal Containment**: Snackbars cover only a small, localized portion of the bottom UI. They lack a background dimming scrim overlay, allowing users to freely browse and interact with underlying page content.
-   **Temporal Auto-Dismissal / Persistence**: Snackbars appear suddenly and may auto-dismiss after a short duration if no action button is present.

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected snackbars against the following strict standards derived from Elements guidelines:

### Elements GM3 Component Specifications & Sizing

-   **Quantity & Frequency Limits**: Strictly **one** snackbar may be displayed on screen at a time. Stacking multiple snackbars simultaneously is a severe layout violation. If multiple are needed, they should appear one at a time.
-   **Container Sizes & Dimensions**:
    -   *Sizing*: Defined by content.
    -   *Minimum Width*: **344dp**.
    -   *Maximum Width*: **672dp**.
    -   *Line Count & Height*: Text wrapping is supported up to 2 lines on compact viewports (expanding to a 3-line layout if the action CTA is long).
-   **Color & Theming**:
    -   Utilizes a high-contrast rectangular container with an completely opaque **Inverse Surface** background fill to ensure maximum text legibility against standard surfaces.
-   **Text Labels**:
    -   **Max Character Count**: Highly recommended maximum of **70 characters** to prevent exceeding two lines in compact layouts.
    -   **No Icons in Snackbar Text**: Inserting icons within the text description area is strictly prohibited.
-   **Actions & Buttons**:
    -   A snackbar can contain a maximum of **one** action, formatted as a single `Text button`, plus an optional close button ("X") to dismiss.
    -   **Writing CTA**: Use a 1-word, active-voice CTA (e.g., "View", "Undo") to prompt necessary action. The action text itself must never be "Close" or "Cancel."
-   **Close Affordance**:
    -   CE Elements recommends that snackbars with actions always include an explicit close affordance ("X").
-   **Error Messaging Restrictions**:
    -   Don't use snackbars for critical, persistent, or bulk errors (use `Callout` or `Dialog` instead). Snackbars should only be used for low-impact, transient errors.

---

### Elements GM3 Behavioral & Accessibility Rules

-   **Self-Dismiss Behavior**:
    -   *Without Action*: Snackbars without actions automatically timeout and self-dismiss after **5 seconds** on mobile/tablet. **Avoid using auto-dismissing snackbars on web** unless there is also inline feedback (e.g., changing a button label to "Saved").
    -   *With Action*: **Must not timeout or self-dismiss**. Since they don't self-dismiss, a close button ("X") is required to prevent blocking the UI, which is particularly critical for screen-magnification users.
-   **Keyboard & Focus Navigation**:
    -   Focus is not automatically forced onto the snackbar upon appearing (it is announced verbally via ARIA live regions).
    -   For snackbars with actions, the buttons must be reachable as part of the page's natural focus order. While focus is within the snackbar, `Tab` traverses the actions and `Esc` closes the snackbar.
-   **Overlapping and Nudging (WCAG Success Criterion 2.4.11)**:
    -   Snackbars must never obscure persistent footer elements (like a FAB) or a currently focused interactive element. The snackbar must reposition vertically (nudge upward) to remain visible alongside other elements and accommodate focus rings.

### Critical Elements GM3 Violations to Flag

1.  **Snackbar Stacking**: Displaying two or more snackbars on screen at the same time.
2.  **Auto-Dismissing with Action**: Allowing a snackbar containing an action button to self-dismiss without user interaction.
3.  **Prohibited Action CTAs / Multiple Actions**: Including multiple action buttons, using non-text buttons, or using "Close"/"Cancel" as the action CTA.
4.  **Exceeding Character Limits**: Overcrowding the text description (exceeding 70 characters or 2 lines) when a simple `Dialog` or `Callout` is more appropriate.
5.  **Icons inside Text**: Including warning or info icons inside the snackbar text description.
6.  **Web Auto-Dismiss without Inline Feedback**: Auto-dismissing a snackbar on the web without any coordinating inline feedback.
