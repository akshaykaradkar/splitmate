# Carousels - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Carousels
(dialog-based sliding containment components used for onboarding or announcements)
across any platform, followed by Elements GM3 specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Carousel** is an interactive, modal-based slider container that groups a series of
slides (typically containing illustrations, text, and actions) to guide users through
onboarding or feature announcements. Unlike persistent list-scrolling widgets, a carousel
at rest in Elements GM3 sits visually above the background content as a modal dialog and
disables standard app functionality until dismissed.

### Key Visual & Geometric Heuristics

-   **Modal Containment**: Sits in front of a dim page overlay or background,
    represented as a distinct centered dialog card.
-   **Sequence Pagination Indicators**: Look for a series of small inline dots or pill
    markers centered at the bottom of the container, indicating the current position and
    total number of slides.
-   **Sequential Navigation Arrows**: Flanking the pagination dots or positioned in the
    footer, find "Next" and "Previous" directional arrow icon buttons.
-   **Close Action Affordance**: A prominent "X" close icon button positioned in the upper
    corner (typically top-right) of the container.
-   **Structured Layout Flow**: Slides feature an illustration at the top (centered), followed
    by a header title, descriptive body text, and interactive action buttons at the bottom.

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected carousels against
the following strict standards derived from Elements GM3 guidelines:

### Elements GM3 Visual & Behavioral Rules

-   **Differences from GM2**:
    -   *Shape*: Dialog cards feature more rounded corners (Extra Large corner radius - **28dp**, corresponding to `--cee3-sys-shape-corner-extra-large` / baseline shape style), and updated pagination shapes.
    -   *Color*: Applied new color mappings with surface-container tokens and accessible state layers.
    -   *Typography*: Title and body texts mapped to GM3 typescales.
-   **Anatomy & Hierarchy**:
    1.  **Close Icon**: A close "X" icon button must always be present to allow immediate dialog dismissal.
    2.  **Illustration**: Centered within the allocated image area, always positioned above text/UI content.
    3.  **Header and Body**: Left or center-aligned. Title text must be a brief statement (maximum 45 characters, single-line preferred). Supporting body text must be 1 to 2 clear sentences (under 300 characters total).
    4.  **Action Buttons**: Use sparingly, with a maximum of **two buttons**. If two actions are present, one must be a secondary button style (never make both primary or include a close/exit button as one of the two).
    5.  **Navigation & Pagination**: "Next" and "Previous" icon buttons are required for accessible keyboard navigation. Pagination dots must be visible to show slide progress but are explicitly **non-interactive** (to prevent accidental triggers).
-   **Slide Count Limit**: Strictly limited to a **maximum of 5 slides** to prevent cognitive overload.
-   **Interactive Target Sizes**: All interactive targets (including navigation arrows, buttons, and close icons) must meet the minimum recommended accessibility target size of **48x48px**.
-   **Rotation Behavior & GAR compliance**:
    -   *Manual Rotation*: Preferred approach. Slide movement is controlled entirely by user clicks or swipe gestures.
    -   *Automatic Rotation (Autorotation)*: If used, any moving content that starts automatically and lasts more than 5 seconds **must** offer an easily accessible option to pause, stop, or hide it (complying with GAR Motion Stoppable).
    -   *Mobile Autorotation*: Explicitly **forbidden** on small screen viewports.
-   **A11y Labeling**:
    -   *Illustration Annotation*: Visuals that reinforce text but don't add standalone information must be marked as decorative/hidden from screen readers. If the image is informative, alternative text (alt text) is mandatory.
    -   *Button Text verbosity*: Button text must clearly describe the destination without relying on surrounding context. Do not use generic phrases like "Learn more" (violates GAR 1.11).
    -   *Focus Trap*: Keyboard focus must remain "trapped" within the modal ARIA Dialog until explicitly closed or dismissed.

### Critical Elements GM3 Violations to Flag

-   **Missing Close Option**: Omitting a clear, keyboard-accessible "X" button to dismiss the modal.
-   **More than 5 Slides**: Packing too many frames (6+) into the onboarding flow, violating simplicity guidelines.
-   **Over-Verbose Text**: Title exceeding 45 characters, or body copy exceeding 300 characters.
-   **Interactive Dots Misuse**: Allowing users to tap pagination dots directly instead of utilizing Next/Prev arrows and swipe gestures.
-   **Illegal Mobile Autorotation**: Permitting slide carousel screens to rotate automatically on mobile viewports.
-   **Generic Button Labels**: Using "Learn more" or "Click here" for the footer action buttons instead of contextual labels (e.g., "Set up backup", "Go to dashboard").
