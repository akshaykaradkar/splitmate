# Date Pickers - Elements GM3 Design System Guide \& Specifications

This reference provides visual heuristics for detecting and evaluating Date Pickers under the Elements GM3 design system (including CEE3 system-level and GM3 component specifications) for visual and codebase auditing.

## Part 1: Universal AI Detection Heuristics for Elements GM3

An Elements GM3 **Date picker** is an integrated interactive component that enables selecting a date or date range. It consists of an outlined text input field and a dropdown/docked calendar modal. It is primarily used within forms or as a filter for displayed data.

### Key Visual \& Geometric Heuristics

-   **Integrated Pair Lockup**: The date picker must always be paired with an outlined date entry text field. The calendar icon button (usually a trailing icon) serves as the **exclusive entry point** to open the calendar dropdown.
-   **Docked Calendar Dropdown**: Clicking the calendar icon opens a menu-like container that docks directly below (or above, if space is limited) the text input.
-   **Calendar Grid Pattern**: A conventional 7-column grid representing days of the week, with abbreviated non-interactive weekday headers (S, M, T, W, T, F, S).
-   **Month/Year Dropdowns**: At the top of the calendar, select elements for month and year navigation are displayed as dropdown buttons next to month/year labels.
-   **Interactive Month/Year Arrows**: Arrow buttons for previous/next month and previous/next year are present on the header.
-   **Action Buttons Lockup**: "OK" and "Cancel" buttons are aligned to the bottom trailing edge of the calendar. An optional "Clear" button can also be present next to them.

---

## Part 2: Elements GM3 Component Specifications

When auditing for Elements GM3 compliance, evaluate date pickers against these specific guidelines and measurements:

### 1. Sizing, Layout, and Touch Targets

-   **Exclusive Entry Point**: The calendar icon inside the text field is the *only* way to trigger the calendar. Clicking the text field itself must focus the field for direct typing rather than opening the calendar.
-   **Touch Targets**: To meet GAR (Google Accessibility Requirements) standards, interactive targets for **all elements** (previous/next arrows, month/year dropdowns, calendar grid days, OK/Cancel/Clear buttons) must strictly be at least **48x48dp (48x48px)**. Component density must not be adjusted in isolation to avoid shrinking these targets.
-   **Visual Grid Days**: Dates outside the current month (preceding or succeeding days) must remain visible and **interactive** to improve efficiency for mouse users. Weekdays headers are non-interactive.
-   **Interactive Grid Days**: Active or focused dates show a distinct circular hover and select boundary, with the selected date represented by a **solid filled circle** using CEE3 primary color tokens.

### 2. Sizing \& Typography Roles

-   **Input Width**: Dictated by the length of the placeholder/output copy plus the calendar icon.
-   **Header Typography**: The calendar header utilizes standard GM3 typescale tokens:
    -   Default headline size: `GM3/static/headline/small` (corresponds to `--cee3-typescale-headline-small-link` styles).
    -   Alternative for longer titles: `GM3/static/title/large` (corresponds to `--cee3-typescale-title-large-link` styles).
-   **Helper Text**: Positioned below the input field, indicating the required format (e.g., `MM/DD/YYYY`). It remains on screen during editing. On error, it is replaced by red error text.

### 3. Sizing \& Interaction Rules

-   **No Input Masks**: While typing directly into the text field, the component **must not** automatically add slashes or special characters (input masks), as this disrupts screen reader announcements.
-   **Multi-format Acceptance**: The input field should recognize a variety of user-typed formats (dashes, spaces, slashes, dots, and optional leading zeros) and auto-format only after the user hits "Enter" or tabs out.
-   **Date Range Selection (Interim Solution)**: The unified date range picker is not GAR 4 compliant. The recommended interim solution for selecting date ranges is to use **two single date pickers with separate entry points** (stacked or side-by-side) for the start and end dates.
-   **Clear Button (Optional)**: If used, the "Clear" button removes the selected date, closes the calendar, and reverts the text field to its placeholder value.

---

## Part 3: Critical Elements GM3 Violations to Flag

-   **Non-Exclusive Trigger**: Opening the calendar dropdown by clicking anywhere inside the text field instead of reserving it exclusively for the calendar icon button (violates screen reader efficiency).
-   **Undersized Touch Targets**: Navigation arrows or calendar grid dates below the 48x48dp touch target threshold.
-   **Input Masks Enabled**: Formatting input with slashes dynamically as the user types.
-   **Use of Unified Range Picker**: Implementing a single date range picker instead of the interim two-picker solution, which violates current GAR compliance.
-   **Missing OK/Cancel Buttons**: Closing the modal dropdown on a date selection click without explicit action button confirmation (unless explicitly specified for simple filters).
