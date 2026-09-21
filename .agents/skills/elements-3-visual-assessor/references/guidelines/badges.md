# Badges - Universal AI Detection Guide \& Elements GM3 Specifications

This reference provides visual heuristics for detecting Badges across any user interface, followed by the strict **Elements GM3** specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Badge** is a compact visual indicator that provides supplemental, non-actionable information about an object or alerts users to status changes. Agents must detect badges based on their distinct geometric, containment, and structural relationship with their parent components, regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **Placement \& Contextual Anchoring**:
    -   *Overlay Badges*: Consistently anchored at the upper trailing edge (top-right quadrant in left-to-right layouts) overlapping the icon or text label of a parent navigation item, tab, or button.
    -   *Inline Badges*: Embedded adjacent to text, lists, table items, or product names to represent status or categories.
-   **Visual Boundaries \& Containment**: Small, solid-colored geometric shapes (dots, pills, or rounded rectangles) contrasting sharply with their parent elements.
-   **Mandatory Dual Mapping (Parent + Badge)**: When a badge is attached to an interactive icon or element, agents **MUST** explicitly identify both components:
    1.  **Parent Component**: Identify the underlying control (e.g., `Icon button`, `Navigation rail` item, `Tab`).
    2.  **Attached Badge (`Badge`)**: Exclusively bound the overlay indicator and classify it independently as `Badge`.

---

## Part 2: Elements GM3 Specifications \& Compliance Auditing

In the **Elements GM3** design system, badges are non-actionable elements (unless hosting a tooltip) that provide structured, compact details. Evaluate detected badges against the following specifications:

### 1. The Five Elements GM3 Badge Types

|Badge Type|Height|Corner Radius|Left/Right Padding|Description \& Typical Use Case|
|:---|:---|:---|:---|:---|
|**Product Badge**|16dp|`0dp, 0dp, 16dp, 16dp`|16dp|Hanging bottom-rounded indicators in top app bars for versioning (Alpha, Beta, Dogfood).|
|**Information Badge**|20dp|4dp|4dp (8dp with icon)|Inline status labels (New, Success, Fail, Triaged) or categorization tags.|
|**Navigation Feature Badge**|16dp|8dp|4dp|Highlights new features in navigation drawers (e.g., "New", "Beta").|
|**Count Badge**|16dp|8dp|4dp|Numerical count overlay for navigation icons or items.|
|**Summary Badge**|6dp (W/H)|Fully rounded (3dp)|N/A|Miniature circular red/alert dot indicating general updates or attention needed.|

### 2. Information Badge Specs \& Styles

-   **Styles**: Three visual treatments are available depending on page hierarchy:
    -   *Filled*: Used when the badge is a main highlight element on the page.
    -   *Tonal*: Medium emphasis, utilizing tonal container fills.
    -   *Outlined*: Used when there are many badges on the page to avoid visual noise.
-   **Icons**: Leading icons are permitted inside Information Badges and must be rendered at **16dp**. Left/right padding increases to 8dp on the icon side, and space between internal elements must be **4dp**.
-   **Color Meanings**:
    -   *Grey*: Neutral, not started.
    -   *Blue*: In progress, success, positive, new.
    -   *Green*: Complete, success.
    -   *Yellow*: Alert, caution (requires attention).
    -   *Red*: Alert, error, urgency, severity.
-   **Contrast Rules**: Color alone must not be the only indicator of status. The text label must explicitly convey the status. Default color mappings must be used to ensure text meets a minimum **4.5:1 contrast ratio** against the badge background.

### 3. Interactive Information Badges (Tooltip Anchors)

-   **Interactive Exception**: If an Information Badge serves as the anchor for a tooltip, it is considered **interactive** because it must be keyboard focusable.
-   **HTML \& Accessibility Role**:
    -   Must use the `button` role.
    -   Must use the `aria-describedby` attribute to link the badge to its tooltip text.
    -   The badge container must meet **GAR Non-text Contrast** (3:1 minimum container boundary contrast) to clearly signal its interactive capability.

### 4. Count \& Feature Badge Constraints

-   **Digit Caps**: Depending on product use case, count badges can cap values at single, double, or triple digits. Numbers exceeding the cap must use a "+" sign (e.g., "99+").
-   **Labeling and Screen Readers**:
    -   *Count Badges*: The number must be appended directly to the associated control's accessibility label so it is read as a single sensible string (e.g., "Inbox, 12 unread items").
    -   *Navigation Feature Badges*: Must keep names extremely short (one word, like "New") to avoid truncation inside drawers. The feature text must be included in the associated control's accessibility label.

---

## Critical Elements GM3 Violations to Flag

1.  **Uppercase Badge Text**: Using all-uppercase text in product or info badges, which impairs readability and causes screen readers to read letter-by-letter.
2.  **Interactive Badge Missing Focus/Role**: An information badge anchoring a tooltip that is not keyboard focusable, lacks a `button` role, or fails GAR Non-text Contrast.
3.  **Standalone Count/Summary Badges**: Placing count or summary badges floating standalone rather than anchored directly to a parent icon or navigation item.
4.  **Incorrect Shape/Geometry**:
    -   Using standard rounded rectangles for summary badges (must be a 6dp circle).
    -   Using standard rounded pills for product badges (must be hanging `0dp, 0dp, 16dp, 16dp`).
5.  **Color-Only Status Indicators**: Relying exclusively on color (e.g. green vs red dot) without corresponding textual labels or accessible strings to convey status.
