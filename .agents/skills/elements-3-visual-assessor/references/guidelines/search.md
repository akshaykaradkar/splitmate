# Search & Search Bars - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Search
components—Search Bars (persistent query entry boxes) and Search Views (expanded
results/suggestions modals)—across any design system or platform, followed by
Elements GM3 specifications for compliance auditing.

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

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected
search components against the following strict standards derived from CE Elements guidelines:

### Elements GM3 Search Bar & Search View Specifications

Elements GM3 establishes precise sizing tokens, shapes, and behavioral rules for
search components:

1.  **Search Bar Specifications**:
    -   **Container Dimensions**: Container height is strictly fixed at **56dp**.
        Container width ranges from a minimum of **306dp** to a maximum of **720dp**.
    -   **Shape**: Utilizes a pill-shaped container (fully rounded corners mapping to the **Full** baseline shape style, with a border radius of exactly half the height: 28dp).
    -   **Anatomy Flow**: Consists of:
        1. Container
        2. Hint text ("Search" by default, configurable)
        3. Search button
        4. Input text
        5. Cursor
        6. Close button ("X")
    -   **Elevation**: Resting search bars are flat, residing at **Elevation 0** (no drop shadows). When active, the container rises to **Elevation 3 (6dp)**.
    -   **Responsiveness & Small Viewports**: When the screen is resized below the minimum width collapse threshold, the search bar collapses into an expandable icon inside the app bar. Upon selecting the icon, the search experience expands to fill the entire screen, and a back arrow is displayed to collapse it back.
2.  **Auto-Suggest Specifications**:
    -   **Dropdown Container**: Appears beneath the search field, with its width matching the active search field's width.
    -   **Item Capacity**: Strictly limited to a maximum of **8 list items** to avoid overwhelming users.
    -   **Group Headers**: If the results return multiple item types (e.g., people, teams, products), group them under headers using the **overline text style**, with text labels limited to 1-2 words.
    -   **Bolding Behavior**: Characters already typed by the user must appear in **bold**, while characters suggested by the system appear normal.
    -   **No Matches State**: Displays the standard error message: "No items match your search" (configurable based on product).
    -   **Autocomplete vs. Result Selection**:
        -   Focusing on a *suggestion item* auto-populates the search input.
        -   Focusing on a *result item* does not populate the input, as it links directly to its target page.
3.  **Advanced Search (Optional Dialog)**:
    -   **Activation**: Triggered by clicking the caret icon button in the search field, opening a dialog overlay.
    -   **Anatomy & Layout**: Supports single-column or two-column layouts containing text fields, section headers, menus, date pickers, selection controls, and must include Clear, Cancel, and Search action buttons at the bottom.
    -   **Dimensions**: Width is determined by the search bar width. Height is determined by content but should fit within the viewport, ideally not exceeding **680dp**. A scroll bar must be enabled to support text scaling and zoom.
    -   **State Transfer**: Any text entered in the search bar *prior* to opening advanced search must be automatically populated into the dialog's "Has the words" input field.
4.  **Corpus Selector (Optional Scoped Search)**:
    -   **Anatomy**: Composed of a dropdown button to the left of the search field, a vertical divider line, and a dropdown menu.
    -   **Default Scope**: Must always default to **"all"** (e.g. "All spaces") so that users do not inadvertently limit their search scope.
    -   **Width Limits**: Menu width must be between a minimum of **112dp** and a maximum of **280dp**.
    -   **Truncation Support**: Keep corpus labels short. When truncation is unavoidable, CEE components must display a basic tooltip containing the full text on hover or focus.
    -   *Responsive Collapse*: When the search bar reaches its minimum threshold, the corpus selector button truncates to a single character and ellipsis.

### Elements GM3 Accessibility & Screen Reader Rules

-   **Focus Order Alignment**: The actions and focusable items in the search bar must be ordered so that the visual order matches LTR Tab focus order, aligning with assistive technology expectations.
-   **Suggestion Icon Annotation**:
    -   *Meaningful Icons (e.g., historical search icon)*: Annotate an accessibility label that starts with the suggestion's visible text and appends the icon's purpose (e.g. `"[text], previous search"`). Mark the icon element as hidden/decorative.
    -   *Decorative Icons*: Annotate an accessibility label that reads only the visible text, with the icon marked as hidden/decorative.
-   **Advanced Search Focus Flow**: Visual order and keyboard focus must align, traveling top-to-bottom, left-to-right through the form elements.

### Critical Elements GM3 Violations to Flag

-   **Conflating Search Bars with Prompt Inputs**: Classifying bottom-anchored chat inputs or conversational AI prompts (which are standard `Text field` elements) as a `Search bar`.
-   **Improper Container Sizing or Shapes**: Implementing search bars with sharp corners or custom heights instead of the standard 56dp tall, Full (Circular) pill container.
-   **Defaulting Scoped Search**: Setting the corpus selector to a specific scoped category instead of defaulting to "all".
-   **Overloading Suggestions**: Displaying more than 8 auto-suggest items in the dropdown.
