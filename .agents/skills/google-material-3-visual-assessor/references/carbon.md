# Google Material 3 Design System: Reference \& Bug Manual

This document is the authoritative reference and bug manual for the **Google Material 3 (M3) Design System**. It provides precise specifications, concrete token mappings, component geometries, interaction states, and accessibility standards. It is designed to guide LLM-based visual assessors and human auditors in evaluating UI screenshots, codebases, and design implementations for strict adherence to Material Design 3.

---

## 1. Foundational Design Tokens \& Values

The following tokens are defined in the M3 design system and must be implemented exactly. They correspond to the bindings in `assets/design-system-bindings.css`.

### 1.1 Color System \& Semantic Color Roles

Material 3 utilizes a semantic "paint-by-number" color system where elements are mapped to semantic color roles rather than hardcoded hex values.

|Semantic Color Role|Light Theme Value|Dark Theme Value|Core Purpose \& Application|
|:---|:---|:---|:---|
|`--md-sys-color-primary`|`#0b57d0`|`#a8c7fa`|Primary accent color for high-emphasis actions, active states, and key branding.|
|`--md-sys-color-on-primary`|`#ffffff`|`#062e6f`|Contrast-passing text/icons shown against the Primary color.|
|`--md-sys-color-primary-container`|`#d3e3fd`|`#0842a0`|Prominent, standout background container fill for key components.|
|`--md-sys-color-on-primary-container`|`#0842a0`|`#d3e3fd`|Contrast-passing text/icons shown against the Primary Container color.|
|`--md-sys-color-secondary`|`#00639b`|`#7fcfff`|Less prominent accent color for medium-emphasis components.|
|`--md-sys-color-on-secondary`|`#ffffff`|`#003355`|Contrast-passing text/icons shown against the Secondary color.|
|`--md-sys-color-secondary-container`|`#c2e7ff`|`#004a77`|Soft background container fill for components like tonal buttons.|
|`--md-sys-color-on-secondary-container`|`#004a77`|`#c2e7ff`|Contrast-passing text/icons shown against the Secondary Container color.|
|`--md-sys-color-tertiary`|`#146c2e`|`#6dd58c`|Complementary accent color for input fields, badges, or special context.|
|`--md-sys-color-on-tertiary`|`#ffffff`|`#0a3818`|Contrast-passing text/icons shown against the Tertiary color.|
|`--md-sys-color-tertiary-container`|`#c4eed0`|`#0f5223`|Soft background container fill for tertiary accent components.|
|`--md-sys-color-on-tertiary-container`|`#0f5223`|`#c4eed0`|Contrast-passing text/icons shown against the Tertiary Container color.|
|`--md-sys-color-error`|`#b3261e`|`#f2b8b5`|Urgently alerts users of critical failures, invalid inputs, or warnings.|
|`--md-sys-color-on-error`|`#ffffff`|`#601410`|Contrast-passing text/icons shown against the Error color.|
|`--md-sys-color-error-container`|`#f9dedc`|`#8c1d18`|Container color for error messages, error text fields, and badges.|
|`--md-sys-color-on-error-container`|`#8c1d18`|`#f9dedc`|Contrast-passing text/icons shown against the Error Container color.|
|`--md-sys-color-surface`|`#ffffff`|`#131314`|Neutral background color for screens and large container areas.|
|`--md-sys-color-on-surface`|`#1f1f1f`|`#e3e3e3`|Neutral high-emphasis text and icons shown against surface colors.|
|`--md-sys-color-on-surface-variant`|`#444746`|`#c4c7c5`|Medium-emphasis text, icons, and boundary borders.|
|`--md-sys-color-outline`|`#747775`|`#8e918f`|Important component boundaries, outlines, and border edges.|
|`--md-sys-color-outline-variant`|`#c4c7c5`|`#444746`|Decorative boundaries, borders, and non-essential list dividers.|
|`--md-sys-color-surface-container-lowest`|`#ffffff`|`#0e0e0e`|Lowest elevation container (e.g., card background).|
|`--md-sys-color-surface-container-low`|`#f8fafd`|`#1b1b1b`|Low elevation container.|
|`--md-sys-color-surface-container`|`#f0f4f9`|`#1e1f20`|Default container background color for cards, sheets, or menus.|
|`--md-sys-color-surface-container-high`|`#e9eef6`|`#282a2c`|High elevation container background.|
|`--md-sys-color-surface-container-highest`|`#dde3ea`|`#333537`|Highest elevation container background.|
|`--md-sys-color-inverse-surface`|`#303030`|`#e3e3e3`|Contrasting background to emphasize snackbars or floating elements.|
|`--md-sys-color-inverse-on-surface`|`#f2f2f2`|`#303030`|Text/icons shown against the Inverse Surface background.|
|`--md-sys-color-inverse-primary`|`#a8c7fa`|`#0b57d0`|Primary accent color on inverse backgrounds (e.g., snackbar buttons).|
|`--md-sys-color-scrim`|`#000000`|`#000000`|Overlay background color used to dim screens behind modals or dialogs.|

### 1.2 Typography scale \& Font Themes

M3 utilizes **Google Sans** (for headlines, titles, and displays) and **Google Sans Text** (for bodies and labels). GSF (Google Sans Flex) is the preferred variable font supporting granular axis manipulation.

|Typographic Token|Font Family|Size (rem)|Line Height|Weight / Description|
|:---|:---|:---|:---|:---|
|`--md-sys-typescale-display-large`|'Google Sans'|`3.6rem`|`4.0rem`|Regular|
|`--md-sys-typescale-display-medium`|'Google Sans'|`2.8rem`|`3.3rem`|Regular|
|`--md-sys-typescale-display-small`|'Google Sans'|`2.3rem`|`2.8rem`|Regular|
|`--md-sys-typescale-headline-large`|'Google Sans'|`2.0rem`|`2.5rem`|Regular|
|`--md-sys-typescale-headline-medium`|'Google Sans'|`1.8rem`|`2.3rem`|Regular|
|`--md-sys-typescale-headline-small`|'Google Sans'|`1.5rem`|`2.0rem`|Regular|
|`--md-sys-typescale-title-large`|'Google Sans'|`1.4rem`|`1.8rem`|Regular|
|`--md-sys-typescale-title-medium`|'Google Sans Text'|`1.0rem`|`1.5rem`|Medium (500)|
|`--md-sys-typescale-title-small`|'Google Sans Text'|`0.9rem`|`1.3rem`|Medium (500)|
|`--md-sys-typescale-body-large`|'Google Sans Text'|`1.0rem`|`1.5rem`|Regular|
|`--md-sys-typescale-body-medium`|'Google Sans Text'|`0.9rem`|`1.3rem`|Regular|
|`--md-sys-typescale-body-small`|'Google Sans Text'|`0.8rem`|`1.0rem`|Regular|
|`--md-sys-typescale-label-large`|'Google Sans Text'|`0.9rem`|`1.3rem`|Medium (500)|
|`--md-sys-typescale-label-medium`|'Google Sans Text'|`0.8rem`|`1.0rem`|Medium (500)|
|`--md-sys-typescale-label-small`|'Google Sans Text'|`0.7rem`|`1.0rem`|Medium (500)|

*Note: Emphasized variants of the type scale (e.g., `--md-sys-typescale-emphasized-body-large`) apply a bold/semi-bold weight (typically weight 500 or 700) to add expression or communicate hierarchy (such as active state or unread status).*

### 1.3 Shapes \& Corner Radii Scale

All components map to the size-based corner scale to define their roundedness.

|Shape Token Name|Corner Radius Value|Application Examples|
|:---|:---|:---|
|`--md-sys-shape-corner-none`|`0`|Full-screen dialogs, edge-to-edge banners.|
|`--md-sys-shape-corner-extra-small`|`4px`|Text field outlines (Android), small badges.|
|`--md-sys-shape-corner-small`|`8px`|Chips, unselected list items, small buttons.|
|`--md-sys-shape-corner-medium`|`12px`|Menus, segmented buttons, small cards.|
|`--md-sys-shape-corner-large`|`16px`|Standard cards, modal side sheets.|
|`--md-sys-shape-corner-large-increased`|`20px`|Medium card variants, larger buttons.|
|`--md-sys-shape-corner-extra-large`|`28px`|Bottom sheets, standard dialogs, standard FABs.|
|`--md-sys-shape-corner-extra-large-increased`|`32px`|Giant buttons, larger FAB variants.|
|`--md-sys-shape-corner-full`|`max(50cqw, 50cqh)`|Pill shape: buttons, icon buttons, switch, slider handles, search bars.|

### 1.4 Spacing \& Measurement System

Padding, margins, and layout slots are built in increments of 4px.

*   `--md-sys-measurement-space0`: `0`
*   `--md-sys-measurement-space25`: `2px`
*   `--md-sys-measurement-space50`: `4px`
*   `--md-sys-measurement-space75`: `6px`
*   `--md-sys-measurement-space100`: `8px` (Base unit)
*   `--md-sys-measurement-space125`: `10px`
*   `--md-sys-measurement-space150`: `12px`
*   `--md-sys-measurement-space175`: `14px`
*   `--md-sys-measurement-space200`: `16px` (Default text field padding)
*   `--md-sys-measurement-space250`: `20px`
*   `--md-sys-measurement-space300`: `24px` (Default layout margin for Expanded screens)
*   `--md-sys-measurement-space400`: `32px`
*   `--md-sys-measurement-space450`: `36px`
*   `--md-sys-measurement-space500`: `40px`
*   `--md-sys-measurement-space600`: `48px` (Minimum touch target size boundary)
*   `--md-sys-measurement-space700`: `56px` (Standard App Bar / Floating Action Button height)
*   `--md-sys-measurement-space800`: `64px` (Navigation Bar default height)
*   `--md-sys-measurement-space900`: `72px` (Large lists / Time Picker input height)

### 1.5 Elevation Levels

Elevation in M3 is depicted primarily through tonal differences (Surface container variations) and optional shadows.

*   `--md-sys-elevation-level0`: `0` (Resting state for flat buttons, text fields, lists, containers)
*   `--md-sys-elevation-level1`: `1px` (Elevated cards, elevated buttons, standard bottom sheets)
*   `--md-sys-elevation-level2`: `3px` (Scrolled App Bar, floating toolbars, menus)
*   `--md-sys-elevation-level3`: `6px` (Standard FABs, dialogs, time/date pickers)
*   `--md-sys-elevation-level4`: `8px` (Hovered standard FABs)
*   `--md-sys-elevation-level5`: `12px` (Pressed standard FABs)

---

## 2. Component Geometries \& Specifications

This section defines the precise layout requirements, margins, padding, colors, shapes, and accessibility criteria for every component checked by the assessor.

### 2.1 Buttons (Common Buttons)

M3 defines standard action buttons: Filled, Elevated, Tonal, Outlined, and Text.

*   **Height**: Standard buttons are `40dp` tall. Extra-small (`32dp`), Medium (`48dp`), Large (`56dp`), and Extra-large (`72dp`) are supported in M3 Expressive.
*   **Shape**: `--md-sys-shape-corner-full` (Pill shape) by default. Square button options use `--md-sys-shape-corner-medium` (`12px`) and morph to `--md-sys-shape-corner-small` (`8px`) on press.
*   **Typography**: Label text must use `--md-sys-typescale-label-large` (Sentence case, never all-caps).
*   **Spacing**: Standard side padding is `--md-sys-measurement-space200` (`16px`). If a leading icon is present, side padding is `--md-sys-measurement-space150` (`12px`), with a `--md-sys-measurement-space100` (`8px`) gap between icon and text.
*   **Color Mappings**:
    *   **Filled Button**: Container is `--md-sys-color-primary` (`#0b57d0` / `#a8c7fa`); text/icon is `--md-sys-color-on-primary` (`#ffffff` / `#062e6f`).
    *   **Elevated Button**: Container is `--md-sys-color-surface-container-low` (`#f8fafd` / `#1b1b1b`) with a shadow; text/icon is `--md-sys-color-primary` (`#0b57d0` / `#a8c7fa`).
    *   **Tonal Button**: Container is `--md-sys-color-secondary-container` (`#c2e7ff` / `#004a77`); text/icon is `--md-sys-color-on-secondary-container` (`#004a77` / `#c2e7ff`).
    *   **Outlined Button**: Container has a `1px` border of `--md-sys-color-outline` (`#747775` / `#8e918f`); text/icon is `--md-sys-color-primary` (`#0b57d0` / `#a8c7fa`).
    *   **Text Button**: No container fill or stroke at rest; text/icon is `--md-sys-color-primary` (`#0b57d0` / `#a8c7fa`).
*   **Interaction State**: Hover, focus, and press states overlay a state layer (10% opacity) matching the text/icon color role on top of the container.
*   **A11y/GAR Compliance**: Touch target must be at least `48dp x 48dp` (achieved via touch target expansion margins for buttons under 48dp height).

### 2.2 Floating Action Buttons (FAB \& Extended FAB)

The FAB represents the primary, most crucial action on a screen.

*   **Sizes \& Geometries**:
    *   **FAB (Standard)**: `56dp x 56dp`. Uses `--md-sys-shape-corner-extra-large` (`28px`). Icon is `24dp` centered.
    *   **Medium FAB**: `80dp x 80dp`. Uses `--md-sys-shape-corner-extra-large-increased` (`32px`). Icon is `28dp` centered.
    *   **Large FAB**: `96dp x 96dp`. Uses `--md-sys-shape-corner-extra-large-increased` (`32px`). Icon is `36dp` centered.
    *   **Extended FAB**: Height is matching its sibling FAB size. Hugs its contents with `--md-sys-shape-corner-extra-large` (`28px`).
*   **Color Styles**:
    *   *Primary Container style*: Container is `--md-sys-color-primary-container` (`#d3e3fd` / `#0842a0`); icon/label is `--md-sys-color-on-primary-container` (`#0842a0` / `#d3e3fd`).
    *   *Surface style (Deprecated but supported)*: Container is `--md-sys-color-surface-container-high` (`#e9eef6` / `#282a2c`); icon/label is `--md-sys-color-primary` (`#0b57d0` / `#a8c7fa`).
*   **Anatomy of Extended FAB**: Must contain an icon, followed by a brief 1-2 word text label using `--md-sys-typescale-title-medium` or `--md-sys-typescale-label-large`.
*   **Placement**: In compact screens, place in the bottom-right corner with a `--md-sys-measurement-space200` (`16px`) margin from edges. In expanded layouts, anchor within the top-left section of the navigation rail or container.
*   **A11y/GAR Compliance**: Must never be shown as disabled; if the action is unavailable, the FAB must be hidden completely.

### 2.3 Icon Buttons

Subtle, compact buttons for low-to-medium emphasis actions.

*   **Variants**: Default (single action) and Toggle (binary selection).
*   **Sizes**: Extra small (`32dp`), Small (`40dp` default), Medium (`56dp`), Large (`96dp`), Extra large (`136dp`).
*   **Standard Color Configurations**:
    *   *Filled*: Container is `--md-sys-color-primary`; icon is `--md-sys-color-on-primary`.
    *   *Tonal*: Container is `--md-sys-color-secondary-container`; icon is `--md-sys-color-on-secondary-container`.
    *   *Outlined*: Container has `1px` stroke of `--md-sys-color-outline-variant`; icon is `--md-sys-color-on-surface`.
    *   *Standard*: No container background; icon is `--md-sys-color-on-surface-variant`.
*   **Toggle Behavior**: Unselected state uses outlined icon style; selected state uses filled icon style or heavier weight (semibold). Selected background fills container (e.g. primary container or filled) to clearly indicate status.
*   **A11y/GAR Compliance**: Minimum touch target of `48dp x 48dp` must be enforced for XS and S sizes via an invisible active hit area.

### 2.4 Button Groups (Standard \& Connected)

Button groups organize peer buttons or switch states.

*   **Standard Button Group**: Adds space between buttons so they can animate width/shape on tap.
    *   Padding varies based on size to ensure each button maintains a `48dp` target.
    *   Google products use full round (`full`) corners for unselected buttons and square (`square`, `--md-sys-shape-corner-medium`) for selected ones.
*   **Connected Button Group**: Replaces segmented buttons. Buttons are unified into a single bounded container with a `2dp` padding or divider.
    *   Height: `40dp`.
    *   Outer border is `--md-sys-color-outline` (`#747775` / `#8e918f`).
    *   Selected segment gets a filled background and appends a checkmark icon to the leading edge of the label.

### 2.5 Chips (Assist, Filter, Input, Suggestion)

Chips represent smart actions, filters, user inputs, or suggestions.

*   **Height**: Standard chip is `32dp` tall.
*   **Shape**: `--md-sys-shape-corner-small` (`8px` corner radius).
*   **Typography**: Label text must use `--md-sys-typescale-label-large` (maximum 20 characters, brief noun or verb).
*   **Visual Elements**:
    *   *Leading Visual*: Optional system icon (`18dp` width) or circular avatar image (`24dp` diameter).
    *   *Trailing Icon*: Optional close icon (`18dp` width) for removing chips (e.g., input chips).
*   **Color Mappings**:
    *   Unselected chip uses `--md-sys-color-surface-container-low` (`#f8fafd` / `#1b1b1b`) or outline border `--md-sys-color-outline-variant` (`#c4c7c5` / `#444746`). Text/icon is `--md-sys-color-on-surface-variant` (`#444746` / `#c4c7c5`).
    *   Selected filter chip uses `--md-sys-color-secondary-container` (`#c2e7ff` / `#004a77`); text/icon is `--md-sys-color-on-secondary-container` (`#004a77` / `#c2e7ff`).
*   **A11y/GAR Compliance**: A chip's interactive hit target must be `48dp` in height, even if the visual container is only `32dp`.

### 2.6 Checkboxes, Radio Buttons, \& Switches

These components are standard binary selectors.

*   **Checkbox**:
    *   Container size: `18dp x 18dp`.
    *   Corner shape: `--md-sys-shape-corner-extra-small` (`2px`).
    *   Selected state: Filled with `--md-sys-color-primary`, displaying a checkmark.
    *   Active target size: `48dp` via state layer bounds.
*   **Radio Button**:
    *   Diameter: `20dp`.
    *   Selected state: Outer ring is `--md-sys-color-primary`, inner dot is `--md-sys-color-primary`.
    *   Unselected state: Outer ring is `--md-sys-color-on-surface-variant`.
*   **Switch**:
    *   Track Dimensions: Width `52dp`, Height `32dp`. Shape is `--md-sys-shape-corner-full` (Pill).
    *   Handle size: Unselected is `16dp` diameter; Selected is `24dp` diameter; pressed is `28dp` diameter.
    *   Track Color: Unselected is `--md-sys-color-surface-container-highest` (`#dde3ea` / `#333537`); Selected is `--md-sys-color-primary` (`#0b57d0` / `#a8c7fa`).
    *   Handle Color: Unselected is `--md-sys-color-outline` (`#747775` / `#8e918f`); Selected is `--md-sys-color-on-primary` (`#ffffff` / `#062e6f`).
    *   Optional Icon: Toggle icons (e.g. checkmark or check) can reside in the handle.

### 2.7 Sliders

Sliders allow selection from a continuous range of values.

*   **Track Height**: XS size is `16dp` tall, S is `24dp`, M is `40dp`, L is `56dp`, XL is `96dp`.
*   **Active Track Color**: `--md-sys-color-primary` (`#0b57d0` / `#a8c7fa`).
*   **Inactive Track Color**: `--md-sys-color-surface-container-highest` (`#dde3ea` / `#333537`).
*   **Handle Shape**: `--md-sys-shape-corner-full` (Pill shape) that shrinks/morphs width slightly when pressed/active.
*   **Stop Indicators**: Small `4dp` circles along the track to indicate discrete values. If inactive track contrast with background is under 3:1, stop indicators must be added to mark boundaries.

### 2.8 Tabs

Tabs group related content of equal hierarchical status.

*   **Variants**: Primary (main content screens, placed under App Bars) and Secondary (nested sub-groups within a content pane).
*   **Heights**: Label-only tabs are `47dp` tall; Icon + Label tabs are `63dp` tall.
*   **Active Indicator**:
    *   *Primary Tab*: Active indicator is a horizontal line of `--md-sys-elevation-level3` (`3dp` height) with `--md-sys-shape-corner-extra-small` (`3px 3px 0 0`) corners, spanning only the width of the label content.
    *   *Secondary Tab*: Active indicator is `2dp` height, spanning the full width of the tab container.
*   **Colors**: Active text/icon is `--md-sys-color-primary` (`#0b57d0` / `#a8c7fa`); inactive text/icon is `--md-sys-color-on-surface-variant` (`#444746` / `#c4c7c5`).
*   **A11y/GAR Compliance**: Tab touch target must be at least `48dp` tall (achieved via expanding bounds).

### 2.9 Cards

Cards display cohesive content blocks about a single subject.

*   **Variants**: Elevated, Filled, and Outlined.
*   **Shape**: `--md-sys-shape-corner-medium` (`12px`) for standard cards.
*   **Border/Outline**: Standard outlined cards use `1px` stroke of `--md-sys-color-outline-variant` (`#c4c7c5` / `#444746`).
*   **Color Mappings**:
    *   *Elevated Card*: Background container is `--md-sys-color-surface-container-low` (`#f8fafd` / `#1b1b1b`) with resting elevation `--md-sys-elevation-level1` (`1px` shadow).
    *   *Filled Card*: Background container is `--md-sys-color-surface-container` (`#f0f4f9` / `#1e1f20`); no shadow.
    *   *Outlined Card*: Background container is `--md-sys-color-surface` (`#ffffff` / `#131314`) with a stroke; no shadow.
*   **Layout Spacing**: Left/right internal margins must be `--md-sys-measurement-space200` (`16px`). Maximum spacing between cards is `8dp`.

### 2.10 Carousel

Carousels showcase horizontally scrolling image/video feeds.

*   **Layouts**: Multi-browse (at least 1 large, 1 medium, 1 small item), Uncontained (equal size items flowing off-screen), Hero (1 large item, 1 small preview item), and Full-screen (edge-to-edge).
*   **Geometries**:
    *   Small carousel items have a width range of `40dp - 56dp`.
    *   Standard carousel item corner radius: `--md-sys-shape-corner-extra-large` (`28px`).
*   **A11y/GAR Compliance**: On vertically-scrolling pages, a horizontal carousel **must** be accompanied by an accessible "Show All" or vertical-scrolling peer view (e.g. via list or grids) to prevent keyboard traps.

### 2.11 Lists \& List Items

Lists organize continuous indexes of vertical text, icons, and metadata.

*   **Variants**: Expressive segmented list (segmented into standalone cards) and Baseline list (continuous surface).
*   **Container Heights**: Tallest element determines height. Typically `56dp` (one-line), `72dp` (two-line), or `88dp` (three-line).
*   **Alignment**:
    *   In 1-2 line lists, elements are middle-aligned.
    *   In 3-line lists (or `88dp` and taller containers), elements are top-aligned with a `--md-sys-measurement-space150` (`12px`) top margin.
*   **Spacing \& Dividers**: Standard dividers are `1px` thick and use `--md-sys-color-outline-variant`. Left padding is `--md-sys-measurement-space200` (`16px`); trailing element right padding is `--md-sys-measurement-space300` (`24px`).
*   **Selectable States**: Highlighted items use `--md-sys-color-secondary-container` container fill.

### 2.12 App Bars (Top App Bars)

App bars provide page context, branding, and high-level navigation.

*   **Variants**: Search App Bar (Gemini search), Small, Medium Flexible, and Large Flexible.
*   **Heights \& Typography**:
    *   *Small App Bar*: Height `56dp` (Wiz) or `64dp` (Android). Title uses `--md-sys-typescale-title-large` (Centered or Left-aligned).
    *   *Medium Flexible App Bar*: Height `112dp` collapsing to `56dp`. Title uses `--md-sys-typescale-headline-small`.
    *   *Large Flexible App Bar*: Height `152dp` collapsing to `56dp`. Title uses `--md-sys-typescale-headline-large`.
*   **Scroll Behavior**: At rest, container matches background color (`--md-sys-color-surface`). On scroll, container changes color to `--md-sys-color-surface-container` (`#f0f4f9` / `#1e1f20`) and adds a visible divider or slight elevation. No drop shadow is used.

### 2.13 Dialogs

Dialogs present interruptive, high-importance actions.

*   **Variants**: Basic Dialog (centered alert, quick choice) and Full-screen Dialog (compact screens only, spans entire window).
*   **Anatomy \& Geometry of Basic Dialog**:
    *   Width: Minimum `280dp`, Maximum `560dp`.
    *   Shape: `--md-sys-shape-corner-extra-large` (`28px` corner radius).
    *   Padding: All-around container padding is `--md-sys-measurement-space300` (`24px`). Gap between headline and body text is `16px`. Gap between body and buttons is `24px`.
*   **Buttons**: Up to two text buttons aligned to the trailing edge. The confirming action is placed on the far right.
*   **A11y/GAR Compliance**: Scrim (`--md-sys-color-scrim` at 32% opacity) must cover underlying elements. Initial focus must land on the first interactive element.

### 2.14 Bottom Sheets \& Side Sheets

Supplementary containers anchored to device edges.

*   **Bottom Sheet (Mobile/Tablet)**:
    *   Width: Full width up to `640dp`.
    *   Shape: Top corners use `--md-sys-shape-corner-extra-large` (`28px`). Bottom corners are square.
    *   Drag Handle: Pill-shaped, centered horizontally. Top 48dp portion must be interactive for resizing.
*   **Side Sheet (Expanded Desktop/Tablet)**:
    *   Width: Default `360dp` (up to `400dp` maximum).
    *   Shape: Modal side sheets use `--md-sys-shape-corner-large` (`16px`).
*   **A11y/GAR Compliance**: Must provide single-pointer options to close/dismiss without dragging (e.g. close buttons or tap on scrim).

### 2.15 Banners \& Snackbars

Temporary alert messages.

*   **Banners (Low-to-medium importance)**:
    *   *Basic Square Banner*: Placed immediately below the App Bar. Full width, square corners.
    *   *Basic Round Banner*: Margins of `16dp`. `--md-sys-shape-corner-extra-large` (`28px`) corners.
    *   *Color*: Standard or Vibrant (`--md-sys-color-tertiary-container`).
*   **Snackbars (Low importance, auto-dismissing)**:
    *   Background is `--md-sys-color-inverse-surface` (`#303030` / `#e3e3e3`); text is `--md-sys-color-inverse-on-surface` (`#f2f2f2` / `#303030`).
    *   Contains a single action text button in `--md-sys-color-inverse-primary`.
    *   **A11y/GAR Compliance**: Auto-dismissing snackbars on web must **never** contain critical, non-reproducible text or actions. If actionable, they must remain until dismissed.

### 2.16 Progress Indicators \& Loading Indicators

Feedback loaders representing active computing tasks.

*   **Progress Indicators**: Linear (horizontal) and Circular.
    *   Thickness: Default `4dp`.
    *   Selected style can be Flat or Wavy (M3 Expressive update).
    *   Active Track Color: `--md-sys-color-primary` (`#0b57d0` / `#a8c7fa`).
    *   Stop Indicator: `4dp` circle marking the end of a linear progress indicator if track contrast is under 3:1 with background.
*   **Loading Indicator**: Expressive shape morph sequence utilizing 7 distinct M3 shapes.
    *   Used for tasks loading under 5 seconds (such as pull-to-refresh).
    *   Container is optional. If visible, the active loader switches color to `--md-sys-color-on-primary-container`.

### 2.17 Tooltips \& Badges

Utility markers and micro-text alerts.

*   **Tooltips**:
    *   *Plain Tooltip*: Container height `24dp`, padding `8dp`, font `--md-sys-typescale-body-small`. Background is `--md-sys-color-inverse-surface`.
    *   *Rich Tooltip*: Supports a headline, body text, and up to two buttons. Top padding `12dp`, side padding `16dp`.
*   **Badges**:
    *   *Small Badge*: Empty `6dp` circle.
    *   *Large Badge*: Height `16dp`. Maximum character count is 4 (e.g., `999+`). Outer shape is `--md-sys-shape-corner-small` (`8px` corner radius).
    *   *Color*: Uses `--md-sys-color-error` (`#b3261e` / `#f2b8b5`) background; text is `--md-sys-color-on-error` (`#ffffff` / `#601410`).

### 2.18 Divider

Visual separation lines.

*   **Geometry**: Thickness `1dp`.
*   **Color**: `--md-sys-color-outline-variant` (`#c4c7c5` / `#444746`).
*   **Inset**: Standard left/right inset is `--md-sys-measurement-space200` (`16px`).

---

## 3. Generative AI (GenAI) \& Aurora Components

M3 includes bespoke visual tokens and components specifically for user-initiated Generative AI features to signal dynamic processing.

### 3.1 AI Color Palette \& Gradients

AI colors should always be used as a moving gradient (counter-clockwise rotation) to signify active computation.

*   `--md-sys-color-ai-main`: `#217BFE`
*   `--md-sys-color-ai-analog`: `#078EFB`
*   `--md-sys-color-ai-complement`: `#AC87EB`
*   `--md-sys-color-ai-main-variant`: `#D7E6FF`
*   `--md-sys-color-ai-analog-variant`: `#C7E4FF`
*   `--md-sys-color-ai-complement-variant`: `#DCE2FF`

#### AI Gradient Rules:

*   **Background / Fills (under text)**: Comprised of lighter variant tones (`main-variant`, `analog-variant`, `complement-variant`) to preserve text readability.
*   **Border / Outlines**: Darker tones (`main`, `analog`, `complement-variant`) mapped with a `surface` segment to form a stark visual edge.
    *   *Ordering*: `AI main` ➔ `AI analog` ➔ `AI complement-variant` ➔ `Surface` ➔ `AI analog` ➔ `AI main`.

### 3.2 AI Component Specifications

*   **AI Assist Chips**: Used to initiate AI actions (e.g., "Generate Text").
    *   Height: `32dp` (Mobile) / `36dp` (Desktop).
    *   Must use an AI system icon (Spark silhouette) on the leading edge.
    *   Generating State removes the stroke border and applies the active AI background gradient. Label changes to "Generating...".
*   **AI Suggestion Chips (Prompt Chips)**: Suggests AI prompts inside chat interfaces.
    *   Asymmetrical corner shape: Top-right corner is `--md-sys-shape-corner-extra-small` (`4px`), while other three corners are half-height rounded (`16px` on mobile, `18dp` on desktop).
*   **AI Outlined Buttons**: Initiates final GenAI generation.
    *   Under generating state, the outline border is removed, and the active rotating AI gradient fills the button container. Text/icon color shifts to `--md-sys-color-primary` to guarantee a `4.5:1` contrast ratio.
*   **AI Icon Buttons**: Inside text inputs or action groups.
    *   When processing, the adjacent prompt field outlines display a `2dp` animated AI gradient.

---

## 4. Interaction States \& State Layers

Visual indicators that communicate component status upon user interaction.

M3 specifies a systematic **State Layer** overlay mechanism. The state layer is a semi-transparent layer applied on top of the component container. It uses the exact color role of the foreground content (text or icon, usually an "On-" color role) with a fixed opacity.

|Interaction State|State Layer Opacity|Component Elevation Adjustment|Visual Behavior \& Criteria|
|:---|:---|:---|:---|
|**Enabled**|`0%` (No overlay)|Resting Level (Default)|Standard component style as defined.|
|**Hover**|`8%` overlay|Resting Level + 1 level|Low-emphasis overlay matching content color. Tooltips appear after 1.5 seconds.|
|**Focus**|`12%` overlay|Resting Level|High-emphasis overlay. Ring-like keyboard focus indicator bounds the component.|
|**Pressed**|`12%` overlay (Ripple)|resting Level + 1 level (or flat)|Circular ripple expand animation originating from the pointer location.|
|**Selected**|Direct Color Shift|Component dependent|Background fills with accent container role. Icons shift from outline to filled.|
|**Dragged**|`16%` overlay|Rest Level + 2 levels (with shadow)|Component lifts with shadow to appear floating above other content.|
|**Disabled**|No overlay (Direct Gray)|Level 0|Container opacity dropped to 12%; text/icon opacity dropped to 38%. Component ignores hover/drag/press.|

---

## 5. Bug Auditing \& Common Compliance Violations

Use this bug checklist to evaluate implementations for compliance errors and file bug tickets.

### 5.1 Critical A11y \& Contrast Violations

*   **Contrast Failures (Text)**: Small text (under `14pt bold` / `18pt regular`) has a contrast ratio below `4.5:1` against the surface or background. Large text has a contrast ratio below `3:1`.
*   **Contrast Failures (Component Boundaries)**: Clustered borders (e.g. outlined text fields, checkboxes, radio groups) have a contrast ratio below `3:1` against the surface.
*   **Actionable Snackbars Auto-Dismissing**: Actionable snackbars on web disappear automatically without user interaction.
*   **Auto-formatting Input Masks**: A text field or date picker automatically injects slashes or spaces while the user is actively typing, disrupting screen readers.
*   **No Alt Text / Label duplication**: Icons or decorative images missing alternative text, or alt text repeating the adjacent visible caption word-for-word.

### 5.2 Touch Target \& Density Violations

*   **Sub-minimum Touch Targets**: Buttons, icon buttons, checkbox containers, or chips have an active pointer target below `48dp x 48dp` (Android/Web) or `44pt x 44pt` (iOS).
*   **Default Denser Layouts**: High density (negative density scale settings) is forced on the user by default, reducing targets below 48dp without an option to opt-out.
*   **Cramped Tab Bars**: More than four tabs crammed onto a compact mobile layout without implementing a scrollable tab bar container.

### 5.3 Geometry \& Structural Violations

*   **Incorrect Button Capitalization**: Button labels are rendered in all-caps (e.g., "SAVE" instead of "Save").
*   **Truncated Headings**: Headings or App Bar titles are truncated with ellipses on standard viewports instead of wrapping or adjusting.
*   **Incorrect Card resting Elevation**: Flat (outlined or filled) cards displaying distinct drop shadows at rest.
*   **Static AI Gradient**: A Generative AI gradient is applied to a button, chip, or input outline as a static, non-moving element during enabled/resting states.
*   **Missing Drag Handles**: A sliding bottom sheet has user-initiated resizing enabled but lacks a distinct, centered horizontal pill-shaped drag handle.
*   **Square Floating Toolbars**: Floating toolbars are rendered with square or slightly rounded corners instead of `--md-sys-shape-corner-full` (Pill shape), causing tension with surrounding components.
