# Search - Universal AI Detection Guide & GM3 Specifications

This reference provides universal visual heuristics for detecting Search
components—Search Bars (persistent query entry boxes) and Search Views (expanded
results/suggestions modals)—across any design system or platform, followed by
Google Material 3 (GM3) specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Search bar** is a prominent interactive input container designed to allow
users to enter keywords or phrases to query and filter information across an app
or dataset. When active, it expands into a **Search view** displaying real-time
suggestions or results. Agents must detect search bars and search views based on
their prominent placement anchoring, container lockups, and expandable modal
states, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Top-Level Anchoring**: Search bars are persistently positioned
    at or near the top of a layout hierarchy:
    -   *Standalone Banners*: Anchored prominently near the top of a screen or
        pane, directly below or integrated within an app bar / header region.
    -   *Header Integration (Search App Bar)*: Embedded directly inside the top
        structural layout region spanning the top of the screen.
-   **Visual Boundaries (`Search bar`)**: Defined by a distinct pill-shaped
    (fully rounded corners) or heavily rounded rectangular input container
    contrasting with the underlying background.
-   **Core Anatomy & Layout Flow**: Search bars maintain a consistent horizontal
    sequence: `[Leading Element] [Hint/Input Text] [Trailing Element(s)]`.
    -   *Leading Element*: A magnifying glass search icon (🔍) or a navigational
        control (hamburger menu or back arrow).
    -   *Text*: A single line of start-aligned pale hint text (e.g.,
        "Search...", "Search inbox") or active user query text.
    -   *Trailing Element(s)*: Up to two action icons (e.g., microphone voice
        search, clear "X" button) and/or a circular user profile avatar.
-   **Active State Signatures (`Search view`)**: Look for the expanded, focused
    state of the search bar:
    -   *Compact / Mobile Screens*: Expands into a full-screen modal covering
        the viewport, showing a list of query suggestions or search results
        below the header. The on-screen keyboard is visible.
    -   *Expanded / Desktop Screens*: Expands into a large docked modal overlay
        connected directly below the search bar container, with the underlying page
        masked by a scrim overlay.
-   **Component Mapping Synonyms**: Downstream evaluation systems recognize both `Search` and `Search bar` as valid component classifications. When a search bar is placed inside a top header region, agents MUST explicitly identify BOTH the outer header container (`App bar`) and the inner search field (`Search bar`).

--------------------------------------------------------------------------------

## Part 2: GM3 Specifications & Compliance Auditing

When conducting a Google Material 3 (GM3) adherence audit, evaluate detected
search components against the following strict standards derived from Material
guidelines:

### GM3 Search Specifications & Sizing

Material Design 3 establishes precise sizing tokens and behavioral rules for
search components:

1.  **Search Bar Specifications**:
    -   **Dimensions**: Container height is strictly fixed at **56dp**. Container width ranges from 360dp (minimum) to 720dp (maximum).
    -   **Shape**: Utilizes a pill-shaped container featuring fully rounded corners defined by the `--md-sys-shape-corner-full` (28dp radius) design token.
    -   **Elevation**: Features level 0 elevation (`--md-sys-elevation-level0`) with no drop shadow at rest by default.
    -   **Color Roles & Design Tokens**:
        -   The search bar container must use the `--md-sys-color-surface-container-high` color role.
        -   This role ensures clear contrast when the screen background uses `--md-sys-color-surface` or is white.
        -   *Contrast Warning*: Avoid placing a `--md-sys-color-surface-container-high` search bar on a `--md-sys-color-surface-container` background. Surface container roles must be more than one step apart to guarantee adequate contrast.
        -   Input text uses `--md-sys-color-on-surface` and hint text uses `--md-sys-color-on-surface-variant`.
        -   Typography maps to `--md-sys-typescale-body-large` (1rem/1.5rem 'Google Sans Text').
    -   **Paddings & Margins**:
        -   Unfocused container margins: 24dp from screen edges (`--md-sys-measurement-space300`).
        -   Focused container margins (GM3 Expressive): 12dp from screen edges (`--md-sys-measurement-space150`) as the bar expands wider.
        -   Internal leading and trailing padding: 24dp when unfocused, 16dp when focused.
        -   Internal padding between elements (e.g., between leading icon and hint text): 16dp.
    -   **Configurations**: Supports standard lockups (magnifying glass + hint text), trailing avatar lockups (avatar size strictly 30dp; max 2 trailing icons plus an avatar), and Google-specific product lockups (incorporating a Google brand mark or app name).
2.  **Search View Specifications**:
    -   **Dimensions & Headers**: Full-screen mobile search view header height is 72dp. Docked desktop/tablet search view header height is 56dp. Docked container height ranges from 240dp (min) up to 2/3 of screen height (max).
    -   **Scrim**: Docked layouts cover underlying content with a `--md-sys-color-scrim` overlay.
    -   **Predictive Back (Android)**: On Android, executing a back swipe detaches the search bar from the screen edge to reveal a previous screen preview before minimizing.
3.  **Accessibility & Keyboard Rules**:
    -   *Autosuggest*: Screen readers must announce the search bar as an autocomplete field and announce when autocomplete results appear on screen.
    -   *Keyboard Navigation*: On web/desktop, pressing the Arrow keys navigates directly through suggestion/result items. Pressing Tab or Shift+Tab navigates between interactive elements. Space or Enter activates the input field. All icons must have separate accessibility labels.

### Critical GM3 Violations to Flag

-   **Conflating Search Bars with Prompt Inputs**: Misclassifying bottom-anchored chat or conversational AI prompt inputs (which are `Text field` containers) as `Search bar` during an audit.
-   **Missing Outer App Bar Mapping**: Failing to classify the outer `App bar` container when a search bar is embedded within the top header region.
-   **Improper Search Bar Heights or Shapes**: Presenting a top-level search bar with sharp corners or unpredictable heights instead of the standardized 56dp pill shape with `--md-sys-shape-corner-full` (28dp).
-   **Poor Background Contrast**: Placing the search bar on a background of `--md-sys-color-surface-container`, resulting in a flat layout where the search bar blends in.
