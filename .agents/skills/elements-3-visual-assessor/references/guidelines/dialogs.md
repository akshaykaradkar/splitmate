# Dialogs - Elements GM3 Design System Guide \& Specifications

This reference provides visual heuristics for detecting and evaluating Dialogs under the Elements GM3 design system (including CEE3 system-level and GM3 component specifications) for visual and codebase auditing.

## Part 1: Universal AI Detection Heuristics for Elements GM3

An Elements GM3 **Dialog** is a modal popup container appearing on top of the primary UI. It is designed to provide critical information, alert users, or request a decision. It disables interactions with the background page until dismissed.

### Key Visual \& Geometric Heuristics

-   **Modality \& Scrim Backdrop**: A distinct card or container floating in the center of the screen, displayed over a darkened background overlay (**scrim**).
-   **Containment \& Corner Geometry**: Uses a card-like rectangular container with generously rounded corners (increased corner radius compared to GM2).
-   **No Elevation Shadow Obligation**: Under Elements GM3, **no drop shadows are required for elevation** as long as the dialog container is correctly positioned on top of the background scrim.
-   **Structured Hierarchy**:
    -   *Header/Title*: Bold, high-contrast title text explaining the dialog's purpose, with optional supporting icons.
    -   *Body Content*: Explanatory text, forms, tabs, list views, or progress indicators. Can scroll only vertically.
    -   *Footer/Action Bar*: Pinned action buttons (usually 1 or 2) aligned to the bottom-right corner.

---

## Part 2: Elements GM3 Component Specifications

When auditing for Elements GM3 compliance, evaluate detected dialogs against the following strict specifications:

### 1. Types \& Use Cases

-   **Dialog Types**: Classified as either **Information** (presents text or info with a single action button) or **Confirmation** (requires a decision with two action buttons).
-   **Avoid Overuse**: Dialogs must not be used to confirm expected, non-disruptive results (e.g., "File saved successfully"). Use a **Snackbar** for non-disruptive feedback.
-   **Lightweight Experience**: Complex or multi-step tasks must not be sequenced inside dialogs. Use dedicated pages or **Side Sheets** instead.

### 2. Sizing \& Responsive Rules

-   **Responsive Sizing**: Dialog width scales according to its content and is restricted by a minimum **72dp margin** from the screen edges.
-   **Specific Width Thresholds**:
    -   *Default Width*: **560dp**
    -   *Minimum Width*: **280dp**
    -   *Maximum Width*: **712dp**
-   **Vertical Scrolling \& Fixed Lockups**:
    -   Dialogs can only scroll vertically.
    -   The header and footer remain **fixed/pinned** in place while the body scrolls.
    -   A **1px thick divider** colored `outline-variant` must appear when body content is scrolled. This divider may also be visible by default to separate headers containing forms or tabs from the body.

### 3. Typography \& Color Tokens

-   **Typography Mappings**:
    -   *Dialog Title*: Default is `GM3/static/headline/small` (corresponds to `--cee3-typescale-headline-small-link` style). If the title is long, use `GM3/static/title/large` (`--cee3-typescale-title-large-link`) as an alternative.
-   **Color Mappings**:
    -   *Container Background*: `surface-container-high` (or other standard GM3 surface colors as needed, such as `surface`).
    -   *Headline Text*: `on-surface`
    -   *Supporting Text*: `on-surface-variant`
    -   *Dividers*: `outline-variant` (1px thick)

### 4. Sizing \& Interaction Rules

-   **Always Enabled Submit Buttons (GAR 1.14)**: The submit/confirm button at the end of a form inside a dialog **must always remain in the enabled state**. Do not disable buttons for invalid inputs. On click, the form must return a clear error callout or validation text.
-   **Clear Actions**: Titles and action buttons must communicate the dialog's purpose clearly. Avoid apologies ("Sorry for the interruption"), alarms ("Warning!"), or vague redundancy.
-   **Action Placement**: The primary, positive action (e.g., "Send", "Save", "Create") must always be placed at the far trailing edge (rightmost button), with the dismissive action ("Cancel") to its left.
-   **Focus Trap \& Dismiss**: Modal dialogs must trap focus within the modal. They must include a visible, keyboard-focusable dismiss button (usually "Cancel" or a close "X" icon) and support the **Esc key** to close (GAR 2.9). Focus must return to the triggering element upon closing.

---

## Part 3: Critical Elements GM3 Violations to Flag

-   **Disabled Confirm/Submit Button**: Disabling the form confirmation button for invalid inputs instead of keeping it enabled and displaying error callouts on click (violates GAR 1.14 accessibility rules).
-   **Complex Task Sequences**: Sequencing dialogs into a multi-step flow or wizard.
-   **Incorrect Dimensions**: Displaying a basic dialog outside the 280dp - 712dp width range or violating the 72dp screen margin.
-   **Missing Scrim Backdrop**: Failing to use a background scrim, making it possible to interact with behind-content.
-   **Horizontal Scrolling**: Introducing horizontal scrolls inside the dialog body.
