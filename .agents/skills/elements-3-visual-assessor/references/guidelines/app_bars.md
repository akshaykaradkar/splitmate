# App Bars - Elements GM3 Design System Specifications \& Guidelines

This reference provides specifications and visual heuristics for evaluating and auditing App Bars within the **Elements GM3** design system.

## Part 1: Universal AI Detection \& Elements Heuristics

An **App bar** is the primary structural header and navigation container located at the top of a screen, pane, or split-view layout. In the **Elements GM3** system, the app bar is optimized for Google internal and enterprise applications, focusing on product branding, search integration, keyboard accessibility, and unified utility menus.

### Key Visual \& Geometric Heuristics

-   **Placement \& Anchoring**: Anchored at the extreme top of the screen or pane, spanning the full width of its parent container.
-   **Visual Boundaries \& Containment**:
    -   *Default State*: At rest, the app bar does not use elevation shadow or bottom divider lines. It is styled with a flat background.
    -   *Background Color*: By default, Elements GM3 products use **Surface Container Low** (light blue tonal fill) for primary views, or optionally **Surface Container Lowest** (pure white) based on design context.
-   **Corner Geometry**: Outer corners must use straight edges (**0dp corner radius**). Any rounding of the outer app bar container indicates a layout violation.
-   **Core Anatomy (Child Elements)**:
    -   *Skip Link*: A "skip to main content" link is embedded as the first tab stop (invisible to mouse users, focusable on keyboard tab).
    -   *Leading Slot*: Houses a navigation menu (hamburger icon) for collapsible drawers, or a contextual exit button (Back arrow `arrow_back` / `arrow_back_ios` or Close `close`).
    -   *Product Branding*: Houses the optional product icon and the **required** product name (H1 heading).
    -   *Product Badges*: Tonal indicator badges hanging from the top app bar to denote lifecycle stages (Alpha, Beta, Dogfood).
    -   *Search Bar*: A global search input positioned either left-aligned or center-aligned.
    -   *Right-Hand Side Actions (Charms)*: Max of 6 actions including custom buttons, the Help icon, Settings, and the user Profile image.
    -   *Profile / User Avatar*: Far right user account particle pulling the photo from Moma teams.

---

## Part 2: Elements GM3 Specifications \& Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate the app bar against these strict specifications:

### 1. Branding \& Product Name

-   **Required Elements**: The product name is mandatory. The product icon is optional but recommended.
-   **Name Length**: Product names should be short (recommended **10 characters or fewer**).
-   **Typography**: Product name uses Elements GM3 Title typography (standardized using `Google Sans` or `Google Sans Text`).
-   **Headings**: The app bar can be configured to use the product name as the H1 heading. If not, the main page content must define a clear H1 heading.

### 2. Right-Hand Side Actions (Charms)

-   **Maximum Actions**: The app bar can include a **maximum of 6 actions**, including the search bar.
-   **Required Target Size**: All interactive buttons/icons inside the app bar MUST meet the **48x48dp** touch target size (`gar-web-target-size`).
-   **Positioning Sequence**: The right-to-left layout order is fixed:
    1.  **Profile / User Avatar** (extreme far right)
    2.  **Settings** (always directly to the left of the profile image, if present)
    3.  **Support Menu** (triggered by the Help icon, directly to the left of Settings)
    4.  **Custom Actions** (up to 3-5, placed to the left of Support, with the most important custom action on the left)
-   **Overflow Behavior**: On smaller screens/viewports, custom actions and the Support action collapse into a single vertical overflow menu. Search collapses into a standard search icon.

### 3. Support Menu Consolidation

-   **Unified Support**: If a product has **2 or more** help/feedback-related actions (e.g., Help, Educational content, About team, Send feedback), they **MUST** be consolidated under a single **Support** menu triggered by a single Help icon button.
-   **Minimum Requirements**: The Support menu requires a minimum of 2 items.
-   **Labels \& Tooltips**:
    -   The Help icon's tooltip and ARIA label must be set to "Support".
    -   Menu item labels must be concise (recommended **under 18 characters** to prevent truncation).
    -   Items navigating away from the web app must include an "open in new window" icon.
    -   Support menu items should *not* contain decorative leading icons to avoid confusion with the triggering Help icon.

### 4. Search Bar Integration

-   **Global Position**: There are two valid options for positioning:
    -   *Left-aligned*: Recommended for layouts with wide tabular data, multi-column grids, or split screens.
    -   *Center-aligned*: Recommended for fixed-width content or narrow reading widths.
-   **Interactive States**: On active search focus, the search bar transitions to active state. On narrow viewports, search collapses into a single icon button.

### 5. Product Badges

-   **Lifecycle Indicators**: Standardized non-interactive indicators for **Alpha**, **Beta**, or **Dogfood** stages.
-   **Visual Geometry**:
    -   *Height*: 16dp.
    -   *Shape*: Hanging bottom-rounded pill with `0dp, 0dp, 16dp, 16dp` corner radius (flush straight edges at the top).
    -   *Padding*: 16dp left and right padding.
-   **Color Mappings**:
    -   **Alpha**: Yellow tonal fill (`--cee3-sys-color-extended-caution-container` / `--cee3-sys-color-extended-yellow-container` with `--cee3-sys-color-extended-on-caution-container` text).
    -   **Beta / Custom**: Blue tonal fill (`--cee3-sys-color-extended-blue-container` with `--cee3-sys-color-extended-on-blue-container` text).
    -   **Dogfood**: Green tonal fill (`--cee3-sys-color-extended-green-container` with `--cee3-sys-color-extended-on-green-container` text).
-   **Accessibility**: Product badges must have text meeting the 4.5:1 contrast ratio. The product name's accessibility label must append the badge title (e.g., "Product Name Alpha").

### 6. User Profile \& Account Dialog

-   **ARIA Dialog Pattern**: The user profile avatar opens a menu implemented as a **non-modal ARIA Dialog** (rather than a simple ARIA menu).
-   **Keyboard Focus**: Focus must be trapped inside the profile dialog, and an explicit close button must be provided. Visually, the close button is cleverly styled to look like the avatar button itself.
-   **Links Limit**: To maintain clarity and ease of navigation, limit the profile dialog menu links to a maximum of **5 links** (such as linking to the user's Momateams profile page).

### 7. Contextual App Bar

-   Used for task-focused pages or flows where global search, navigation, or FABs are omitted to minimize distraction.
-   Features an exit/close button (`close`) or back button (`arrow_back` / `arrow_back_ios`) in the top left.
-   **Title and Secondary Label**:
    -   *Large viewports (\>= 1024dp)*: Title and secondary label are positioned inline within the app bar.
    -   *Medium to small viewports (\<= 840dp)*: Title and secondary label are positioned below the app bar controls.
    -   *Wrap Rules*: Page titles and secondary labels must always wrap to multiple lines. **Text truncation with ellipsis is strictly prohibited** on contextual header labels.

---

## Critical Elements GM3 Violations to Flag

1.  **Improper Target Size**: Any interactive icon button or action target smaller than **48x48dp**.
2.  **Separate Help \& Feedback Icons**: Displaying separate "Help" and "Feedback" icon buttons instead of consolidating them into the unified "Support" menu when both are present.
3.  **Truncated Contextual Titles**: Truncating page titles or secondary labels with ellipses (`...`) instead of wrapping them.
4.  **Incorrect Right-Hand Side Sequence**: Placing custom actions, Settings, or Support in a sequence that violates the fixed `Profile -> Settings -> Support -> Custom Actions` layout order.
5.  **Rounded Outer Corners**: Any outer rounding (border-radius \> 0) on the main app bar container.
6.  **Standalone Bottom App Bar Usage**: Elements GM3 prioritizes top app bars; floating/bottom app bars should be avoided or flagged for review in desktop/enterprise layouts.
