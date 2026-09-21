# Text Fields - Elements GM3 AI Detection Guide & Specifications

This reference provides visual heuristics for detecting Text Fields (interactive form input containers) across the Elements GM3 design system, followed by concrete specifications for compliance auditing.

## Part 1: Elements GM3 AI Detection Heuristics

A **Text field** is an interactive form input container where users type, edit, or select text (e.g., email inputs, search queries, password boxes, chat prompt bars, or form entry rows). Agents must detect text fields based on container geometry, floating label positions, and active focus/error state markers, regardless of platform implementation.

### Key Visual & Geometric Heuristics

-   **Container Geometry & Bounding Box**: Look for elongated rectangular containers spanning form rows or docked at the bottom of screens:
    -   *Outlined Variant*: Transparent or low-tonal background fill enclosed within a full perimeter border stroke with fully rounded corners. **Outlined text fields are the recommended type for Elements products.**
    -   *Filled Variant*: Solid background color fill featuring a horizontal bottom line indicator. Top corners are rounded; bottom corners are flat. **Filled text fields must be avoided due to accessibility concerns (poor background contrast).**
-   **Label Placement (Resting vs. Floating)**: Look for text labels describing the input field:
    -   *Resting State*: When empty and unfocused, label text sits vertically centered inside the container.
    -   *Floating State*: On active focus or when text is entered, the label shrinks and floats to the top. In outlined fields, the floating label cuts directly through the top border stroke.
-   **State Cues (Focus & Error)**:
    -   *Focus*: Border stroke thickens and adopts the primary accent color.
    -   *Error*: Border stroke turns red. A trailing red error icon (e.g., exclamation symbol) appears at the trailing edge, and red error text replaces supporting helper text below.
-   **Numeric Input Slots inside Cards**: In some layouts, small numeric inputs are embedded directly inside larger cards alongside labels and units. In these cases, the outer bounding container must be mapped to `Card`, and the specific editable text box must be mapped to `Text field` (not static `Text`).
-   **Chat Prompt Inputs vs. Search Bars**: Agents MUST strictly distinguish between top-level search bars and conversational message/prompt inputs. An input container located at the bottom of a screen in messaging or conversational AI apps (e.g., Gemini) used to type prompts is a **`Text field`**, NOT a `Search bar`.

---

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected text fields against the following strict standards:

### Component Design Specs & Sizing

-   **Height & Density**:
    -   *Standard Text Field*: The text field container height is strictly **56dp** itself (excluding helper text, margin, and floating label).
    -   *Dense Text Field*: For high-density interfaces where screen space is critical, a dense text field is available with a container height of **40dp** (uses density -4). Dense fields must not be used on mobile or touch-based interfaces as they reduce touch target sizes.
-   **Spacing & Width**:
    -   *Maximum Width*: A maximum container length of **512dp** is recommended for readability.
    -   *Sibling Spacing*: Form text fields must maintain exactly **36dp spacing** between their borders.
-   **Internal Paddings & Icons**:
    -   *Padding*: Strictly **16dp left and right padding** inside the text field container.
    -   *Icons*: Lead and trailing icons utilize **24dp assets**. Trailing icons are used for secondary interaction (clear text, dropdown arrows, or password visibility toggle).
    -   *Helper/Error Text*: Positioned exactly **4dp below** the container, providing instructions, character count, or error messages.

### State Specifications

Elements GM3 text fields must strictly adhere to these state styling and stroke token specifications:

1.  **Inactive (Empty)**:
    -   *Stroke*: **1dp** outline (`var(--cee3-sys-color-extended-grey-outline)` or outline variant).
    -   *Label Text*: On Surface Variant (`var(--cee3-sys-color-on-surface-variant)`).
    -   *Helper Text*: On Surface Variant (`var(--cee3-sys-color-on-surface-variant)`).
2.  **Focused**:
    -   *Stroke*: **2dp** primary accent outline (`var(--cee3-sys-color-extended-link)` or `--md-sys-color-primary`).
    -   *Label Text*: Primary (`var(--cee3-sys-color-extended-link)`).
    -   *Helper Text*: On Surface Variant.
3.  **Activated (Filled)**:
    -   *Stroke*: **1dp** outline.
    -   *Label & Input Text*: On Surface (`var(--cee3-sys-color-on-surface)`).
    -   *Helper Text*: On Surface Variant.
4.  **Error (Inactive/Empty)**:
    -   *Stroke*: **1dp** error outline (`var(--cee3-sys-color-extended-red)` or `--md-sys-color-error`).
    -   *Helper Text*: Error text (`var(--cee3-sys-color-extended-red)`).
5.  **Error (Focused)**:
    -   *Stroke*: **2dp** error outline.
    -   *Label & Helper Text*: Error color (`var(--cee3-sys-color-extended-red)`).
    -   *Input Text*: On Surface.
6.  **Error (Activated/Filled)**:
    -   *Stroke*: **1dp** error outline.
    -   *Label & Helper Text*: Error color.
    -   *Input Text*: On Surface Variant.

### Form Behavior & Labels

-   **Visible Labels Required**:
    -   Only omit a visible label if the function of the field is completely obvious from the context (e.g., search bar, grouping with external labels).
    -   In all cases, a hidden accessible screen-reader label is mandatory.
-   **Required Fields**:
    -   Required form fields are indicated by displaying a **red asterisk (*)** immediately following the label text. The asterisk is colored red only when empty or focused.
    -   If most fields are required, indicate optional fields by displaying the word `(optional)` in parentheses next to the label.
-   **Error Visibility**:
    -   Error states must include a **trailing red error icon** in addition to the red border and error text, ensuring colorblind users can immediately identify invalid inputs. Do not rely on color alone to communicate invalid fields.

### Critical Elements GM3 Violations to Flag

-   **Using Filled Text Fields**: Using filled text fields on standard pages instead of the recommended outlined style.
-   **Incorrect Container Heights**: Using heights other than 56dp (standard) or 40dp (dense), failing standard touch-target accessibility.
-   **Missing Red Asterisk (*)**: Failing to include a red asterisk immediately next to a required field's label.
-   **Missing Trailing Error Icons**: Communicating an invalid/error state without displaying the trailing red error icon inside the container.
-   **Using Outline Colors for Text**: Using outline-variant or outline color roles for rendering text labels or inputs, which violates contrast guidelines.
