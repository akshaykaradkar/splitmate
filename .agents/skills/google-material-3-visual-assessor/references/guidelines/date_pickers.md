# Date Pickers - Universal AI Detection Guide \& MD3 Specifications

This reference provides universal visual heuristics for detecting Date Pickers (components enabling date or date range selection) across any design system or platform, followed by Google Material 3 (M3) specifications and design token bindings for compliance auditing.

---

## Part 1: Universal AI Detection Heuristics

A **Date picker** is an interactive UI component that allows users to select a single date or a date range (e.g., scheduling flights, booking hotels, or entering a birthdate). Agents must detect date pickers based on their calendar grid layouts, date input fields, and range highlights, regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **Calendar Grid Pattern**: A primary visual signature of a date picker is the presence of a conventional 7-column grid representing days of the week. Days of the week headers (e.g., S, M, T, W, T, F, S) are typically abbreviated and non-interactive.
-   **Variant \& Layout Signatures**:
    -   *Docked Date Picker*: A text input field paired with a trailing calendar icon. When active, a calendar dropdown grid docks directly below the input field.
    -   *Modal Date Picker*: A popup dialog containing a prominent date header, month/year navigation arrows, a calendar grid, and bottom confirmation/cancel actions.
    -   *Modal Date Input*: Labeled direct text entry fields (often with `MM/DD/YYYY` helper text) housed inside a modal dialog, commonly featuring an edit/calendar icon to toggle between picker and input modes.
-   **Range Selection Visuals**: Look for distinct circular or rounded-rectangular selection shapes marking the start and end dates, connected by a lighter continuous background fill spanning the intermediate dates.
-   **Navigation Cues**: Arrow buttons for previous/next month navigation, and dropdown chevrons next to month/year titles.

---

## Part 2: M3 Specifications \& Compliance Auditing

When conducting a Google Material 3 (M3) adherence audit, evaluate detected date pickers against the following strict standards and exact token bindings:

### M3 Component Variants \& Usage

1.  **Docked Date Picker**:
    -   Standard for medium and expanded breakpoints (tablets, foldables, and desktops).
    -   Opens as a dropdown calendar anchored directly below a text input field.
    -   Allows both keyboard typing in the text field and calendar grid clicking.
2.  **Modal Date Picker**:
    -   Standard for compact/mobile screens to maximize touch targets and readability.
    -   Can expand full-screen or appear as a centered modal card.
    -   Highly recommended for selecting dates close to the present (e.g., booking appointments, flights, reservations).
3.  **Modal Date Input**:
    -   Displays manual text input fields inside a modal dialog.
    -   Required for selecting distant dates (e.g., Date of Birth or historical dates), where navigating a month-by-month calendar grid would be extremely tedious and bad UX.

---

### M3 Visual, Spacing, and Token Specifications

Evaluate the CSS and visual rendering against these exact tokens from the design system bindings:

#### 1. Color Tokens

-   **Container Background**: Must resolve to `--md-sys-color-surface-container-high` (`light-dark(#e9eef6, #282a2c)`) or `--md-sys-color-surface` (`light-dark(#ffffff, #131314)`).
-   **Header / Title / Labels Text**: Must resolve to `--md-sys-color-on-surface` (`light-dark(#1f1f1f, #e3e3e3)`).
-   **Selected Day Container**: Must use a solid fill of `--md-sys-color-primary` (`light-dark(#0b57d0, #a8c7fa)`).
-   **Selected Day Label**: Must use `--md-sys-color-on-primary` (`light-dark(#ffffff, #062e6f)`).
-   **Current Day (Today) Highlight**: Outlined with `--md-sys-color-primary` with text in the same color, unless selected (which switches to a solid fill).
-   **Range Highlight Fill (In-between dates)**: Uses a lighter tint matching `--md-sys-color-secondary-container` or `--md-sys-color-primary-container`.
-   **Error States (Invalid Manual Input)**: Text field and helper text must use `--md-sys-color-error` (`light-dark(#b3261e, #f2b8b5)`) and `--md-sys-color-error-container`.

#### 2. Shape \& Corners

-   **Main Container**: Uses a rounded rectangle with a distinct 28px corner radius: `--md-sys-shape-corner-extra-large` (`28px`).
-   **Day Selector / Highlighters**: Selected days use circular selectors bound to `--md-sys-shape-corner-full` (`max(50cqw, 50cqh)`).
-   **Docked Text Field Container**: Typically uses a 4px corner radius: `--md-sys-shape-corner-extra-small`.

#### 3. Typography Scales

-   **Header / Selected Date Display**: Uses `--md-sys-typescale-headline-medium` (`1.8rem/2.3rem 'Google Sans'`) or `--md-sys-typescale-title-large` (`1.4rem/1.8rem 'Google Sans'`).
-   **Month / Year Navigation Text**: Uses `--md-sys-typescale-label-large` (`500 0.9rem/1.3rem 'Google Sans Text'`).
-   **Day Names Headers**: Uses `--md-sys-typescale-body-small` (`0.8rem/1rem 'Google Sans Text'`).
-   **Grid Days Numbers**: Uses `--md-sys-typescale-body-large` (`1rem/1.5rem 'Google Sans Text'`) or `--md-sys-typescale-body-medium` (`0.9rem/1.3rem 'Google Sans Text'`).

#### 4. Spacing, Padding, and Touch Targets

-   **Interactive Targets**: All interactive grid days, navigation buttons, and action buttons must have a minimum touch target of **48x48px** (`--md-sys-measurement-space600`).
-   **Header Padding**: Top/Left/Right/Bottom padding is strictly `--md-sys-measurement-space300` (`24px`).
-   **Spacing between Title and Body Grid**: `--md-sys-measurement-space200` (`16px`).
-   **Spacing between Calendar and Bottom Action Buttons**: `--md-sys-measurement-space300` (`24px`).

---

### M3 Behavioral Rules

-   **Mode Switching**: Dialog-based date pickers must feature an edit (pencil) icon or calendar icon acting as an accessible toggle to seamlessly transition between visual calendar grid selection and direct keyboard text entry.
-   **Manual Date Input Helper Text**: Input fields must display helper text indicating the expected date format (e.g., `MM/DD/YYYY` or `DD/MM/YYYY`) using `--md-sys-typescale-body-small`. Slashes or input masks should **not** auto-populate while typing as it disrupts screen reader verbalization; auto-formatting must only occur after focus leaves the field or the user presses Enter.
-   **Optional Clear Action**: A "Clear" button is optional. It can be removed to minimize tab stops for keyboard accessibility.

---

### Critical M3 Violations to Flag

-   **Grid Selection for Distant Dates**: Forcing users to click through a calendar grid month-by-month to select a distant birthdate instead of providing a direct text entry field.
-   **Missing Mode Toggle**: Lacking an edit (pencil) icon to toggle between text input and visual calendar grid modes.
-   **Improper Container Shape**: Modal picker containers using legacy sharp corners or minimal rounding (e.g., 4px-8px) instead of the standard `--md-sys-shape-corner-extra-large` (28px) shape.
-   **Sub-48px Touch Targets**: Compacting date grid squares or navigation arrows below 48x48px, violating standard physical interaction specifications.
