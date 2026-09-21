# Search & Search Bars - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Search
components—Search Bars (persistent query entry boxes) and Search Views (expanded
results/suggestions modals)—across any design system or platform, followed by
Android Motion specifications for compliance auditing.

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
        connected directly below the search bar container.
-   **Component Mapping Synonyms**: Agents analyzing search components should
    note that downstream evaluation systems recognize both `Search` and `Search
    bar` as valid component classifications. When a search bar is placed inside
    a top header region, agents MUST explicitly identify BOTH the outer header
    container (`App bar`) and the inner search field (`Search bar`).

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion adherence audit, evaluate detected
search components against the following strict standards derived from Android Motion
guidelines:

### Android Motion Search Bar & Search View Specifications

Android Motion establishes precise sizing tokens, shape mappings, typography roles, and behavioral rules for
search components:

1.  **Search Bar Specifications**:
    -   **Dimensions**: Container height is strictly fixed at **56dp**.
        Container width ranges from 360dp (minimum) to 720dp (maximum).
    -   **Shape**: Utilizes a fully rounded pill-shaped container (`--droid-sys-shape-corner-full` which is `max(50cqw, 50cqh)`).
    -   **Elevation**: Features a flat profile at rest with no drop shadow, mapped to `--droid-sys-elevation-level0`.
    -   **Color & Fill**: Uses `--droid-sys-color-surface-variant` (light: `#e1e3e1`, dark: `#444746`) or `--droid-sys-color-surface` (light: `#fdfcfb`, dark: `#1f1f1f`) to contrast against the canvas background. Content elements use `--droid-sys-color-on-surface` for active text/icons, and `--droid-sys-color-on-surface-variant` for hint text.
    -   **Typography**: Input and hint text must be mapped to `--droid-sys-typescale-body-large` (`1rem/1.5rem 'Google Sans Text'`).
    -   **Paddings**: 16dp padding on the left and right edges, and 16dp spacing between internal elements (e.g., leading icon and hint text).
    -   **Configurations**: Supports standard lockups (magnifying glass + hint text), trailing avatar lockups (max 2 trailing icons plus an avatar), and product-specific branding.
2.  **Search View Specifications**:
    -   **Dimensions & Headers**: Full-screen mobile search view header height is 72dp. Docked tablet/desktop search view header height is 56dp. Docked container height ranges from 240dp (min) up to 2/3 of screen height (max).
    -   **Predictive Back (Android)**: On Android, executing a back swipe detaches the search bar from the screen edge to reveal a previous screen preview before minimizing.
    -   **Motion & Easing**: Transitioning from a Search Bar to an expanded Search View must utilize the `--droid-sys-motion-duration-300` (300ms) or `--droid-sys-motion-duration-350` (350ms) with the signature `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) for premium, organic scaling.
3.  **Accessibility & Screen Reader Rules**:
    -   *Autosuggest*: Screen readers must announce the search bar as an autocomplete field and announce when autocomplete results appear on screen.
    -   *Keyboard Navigation*: On web/desktop, pressing the Down Arrow key navigates directly to the first suggestion. Pressing Tab moves focus to trailing action icons. All icons must have separate accessibility labels.
    -   *Icon Font*: All graphical symbols within the search container must use the official `--md-icon-font` ('Google Symbols').

### Critical Android Motion Violations to Flag

-   **Conflating Search Bars with Prompt Inputs**: Misclassifying bottom-anchored chat or conversational AI prompt inputs (which are `Text field` containers) as `Search bar` during an audit.
-   **Missing Outer App Bar Mapping**: Failing to classify the outer `App bar` container when a search bar is embedded within the top header region.
-   **Improper Search Bar Heights or Shapes**: Presenting a top-level search bar with sharp corners (missing `--droid-sys-shape-corner-full` rendering) or unpredictable heights instead of the standardized 56dp height.
-   **Incorrect Typography/Font Choice**: Using system serif fonts or unauthorized families for search text instead of `--droid-sys-typescale-body-large` leveraging `'Google Sans Text'`.
