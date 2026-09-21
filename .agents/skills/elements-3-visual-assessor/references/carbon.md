# Elements GM3 Design System Reference Manual

This is the authoritative reference manual for the **Elements GM3 Design System** (formerly CE Elements). It contains all concrete design tokens, component geometries, layout behaviors, interaction states, and accessibility standards required to perform visual and codebase compliance audits.

This manual is tailored exclusively to the **Elements GM3 Design System**, substituting all previous baseline Material 3 (M3) and Carbon references with Elements GM3 specifications, token structures, and web-component bindings.

---

## 1. Introduction to Elements GM3

The **Elements GM3 Design System** is Google's core enterprise and productivity design system, extending the visual language of Google Material 3 (GM3) for first-party (1P) and Corp Eng applications. It establishes strict rules for branding, usability, and digital accessibility, ensuring all Google applications maintain high visual harmony and user trust.

### 1.1 Contrast Levels \& HCT Space

The Elements GM3 color system is powered by the **HCT (Hue, Chroma, Tone)** color space. By separating tone (luminance/lightness) from hue and chroma, Elements GM3 guarantees visual accessibility algorithmically:

*   **Standard Contrast (Default):** Highlights hierarchy using distinct high-emphasis and low-emphasis elements.
*   **Medium Contrast / iOS Increase Contrast:** Minimizes low-contrast elements for users with visual fatigue or mild impairments.
*   **High Contrast Mode:** Maximizes legibility by using a limited color palette (typically pure black/white outlines and high-luminance fills).
*   **Contrast Rules:** Standard body text must achieve a **4.5:1** minimum contrast ratio with its background. Large text (14pt bold / 18pt regular and above) and decorative boundaries (like outlines) must achieve a **3:1** minimum contrast.

### 1.2 Elements GM3 Frameworks

Elements GM3 is implemented across three primary engineering platforms:

1.  **GM3 Wiz:** The latest web implementation supporting expressive features, dynamic color, and complex components.
2.  **Angular Material:** Supports baseline Elements GM3 styles using component-level tokens and override APIs.
3.  **ACX Web Components:** Tailored for Dart-based enterprise applications, supporting static token maps.

---

## 2. Foundational Design Tokens

Elements GM3 uses a unified naming convention where token IDs begin with `--cee3-` (CSS Custom Properties) or `cee3.sys.` / `cee3.comp.` (Reference system).

### 2.1 Color Tokens \& Mappings

The following table details the core extended color roles of the Elements GM3 design system, as defined in `design-system-bindings.css`. These roles support both Light and Dark themes dynamically via `light-dark()` CSS declarations.

|CSS Custom Property|Light Value|Dark Value|Semantic Role / Component Mapping|
|:---|:---|:---|:---|
|`--cee3-sys-color-extended-blue`|`#1157ce`|`#a1c9ff`|Core accent color for primary actions, active indicators, and high-emphasis components.|
|`--cee3-sys-color-extended-blue-container`|`#d0e4ff`|`#04409f`|Low-emphasis container background for blue-themed elements.|
|`--cee3-sys-color-extended-blue-fill`|`#1157ce`|`#a1c9ff`|Solid background color for filled primary buttons and active indicators.|
|`--cee3-sys-color-extended-blue-outline`|`#d0e4ff`|`#04409f`|Outer border stroke for outlined blue components.|
|`--cee3-sys-color-extended-blue-tonal`|`#d0e4ff`|`#04409f`|Container background for tonal blue buttons and recessive tabs.|
|`--cee3-sys-color-extended-caution`|`#8f4e06`|`#fcbd00`|Warning accent color for medium-severity notices, banners, and caution states.|
|`--cee3-sys-color-extended-caution-container`|`#ffe07c`|`#6d3a01`|Background container for caution alerts, ensuring readable contrast.|
|`--cee3-sys-color-extended-cyan`|`#00687c`|`#60d5f3`|Secondary accent color used for informative callouts, chips, or auxiliary tags.|
|`--cee3-sys-color-extended-cyan-container`|`#acedff`|`#004e5d`|Recessive container for cyan accents.|
|`--cee3-sys-color-extended-error-container`|`#ffdadc`|`#8a1a16`|Background container color for text validation errors, inline alerts, and invalid text fields.|
|`--cee3-sys-color-extended-green`|`#006c35`|`#80da88`|Success state color for positive outcomes, completed processes, and active indicators.|
|`--cee3-sys-color-extended-green-container`|`#beefbb`|`#00522c`|Tonal background container for green/success elements.|
|`--cee3-sys-color-extended-grey`|`#5e5e5e`|`#c7c7c7`|Neutral gray accent used for unselected, inactive, or disabled elements.|
|`--cee3-sys-color-extended-grey-container`|`#e3e3e3`|`#474747`|Background container for neutral cards, divider lines, and unselected chips.|
|`--cee3-sys-color-extended-info`|`#1157ce`|`#a1c9ff`|Accent color for generic informational banners, callouts, and system updates.|
|`--cee3-sys-color-extended-informational`|`#1157ce`|`#a1c9ff`|Identical to extended-info; maps to informational-themed components.|
|`--cee3-sys-color-extended-link`|`#0b57d0`|`#a8c7fa`|Default hyperlink text color on surface backgrounds; must be paired with an underline.|
|`--cee3-sys-color-extended-link-hover`|`#0842a0`|`#d3e3fd`|Hyperlink text color during cursor hover interactions.|
|`--cee3-sys-color-extended-link-visited`|`#7438d2`|`#d9bafd`|Hyperlink text color after a destination has been visited.|
|`--cee3-sys-color-extended-link-visited-hover`|`#5629a4`|`#eedcfe`|Visited hyperlink text color during cursor hover interactions.|
|`--cee3-sys-color-extended-on-blue`|`#ffffff`|`#012c6f`|Foreground text/icon color against an `extended-blue` solid background.|
|`--cee3-sys-color-extended-on-blue-container`|`#04409f`|`#d0e4ff`|Foreground text/icon color against an `extended-blue-container` background.|
|`--cee3-sys-color-extended-on-caution`|`#ffffff`|`#4d2600`|Foreground text/icon color against an `extended-caution` background.|
|`--cee3-sys-color-extended-on-caution-container`|`#1b1b1c`|`#1b1b1c`|Fixed dark foreground color against an `extended-caution-container` background.|
|`--cee3-sys-color-extended-on-cyan`|`#ffffff`|`#003641`|Foreground text/icon color against an `extended-cyan` background.|
|`--cee3-sys-color-extended-on-error`|`#ffffff`|`#60150f`|Foreground text/icon color against a semantic error background.|
|`--cee3-sys-color-extended-on-error-container`|`#8a1a16`|`#ffdadc`|Foreground text/icon color against an `extended-error-container` background.|
|`--cee3-sys-color-extended-on-green`|`#ffffff`|`#00381f`|Foreground text/icon color against an `extended-green` background.|
|`--cee3-sys-color-extended-on-green-container`|`#00522c`|`#beefbb`|Foreground text/icon color against an `extended-green-container` background.|
|`--cee3-sys-color-extended-on-grey`|`#ffffff`|`#303030`|Foreground text/icon color against an `extended-grey` background.|
|`--cee3-sys-color-extended-on-grey-container`|`#474747`|`#e3e3e3`|Foreground text/icon color against an `extended-grey-container` background.|
|`--cee3-sys-color-extended-on-info`|`#ffffff`|`#012c6f`|Foreground text/icon color against an `extended-info` background.|
|`--cee3-sys-color-extended-on-informational`|`#ffffff`|`#012c6f`|Foreground text/icon color against an `extended-informational` background.|
|`--cee3-sys-color-extended-on-orange`|`#ffffff`|`#522302`|Foreground text/icon color against an `extended-orange` background.|
|`--cee3-sys-color-extended-on-orange-container`|`#753403`|`#ffdcc3`|Foreground text/icon color against an `extended-orange-container` background.|
|`--cee3-sys-color-extended-on-pink`|`#ffffff`|`#620438`|Foreground text/icon color against an `extended-pink` background.|
|`--cee3-sys-color-extended-on-pink-container`|`#8d0053`|`#ffd8ef`|Foreground text/icon color against an `extended-pink-container` background.|
|`--cee3-sys-color-extended-on-purple`|`#ffffff`|`#400b84`|Foreground text/icon color against an `extended-purple` background.|
|`--cee3-sys-color-extended-on-purple-container`|`#5629a4`|`#eedcfe`|Foreground text/icon color against an `extended-purple-container` background.|
|`--cee3-sys-color-extended-on-red`|`#ffffff`|`#60150f`|Foreground text/icon color against an `extended-red` background.|
|`--cee3-sys-color-extended-on-red-container`|`#8a1a16`|`#ffdadc`|Foreground text/icon color against an `extended-red-container` background.|
|`--cee3-sys-color-extended-on-success`|`#ffffff`|`#00381f`|Foreground text/icon color against an `extended-success` background.|
|`--cee3-sys-color-extended-on-success-container`|`#00522c`|`#beefbb`|Foreground text/icon color against an `extended-success-container` background.|
|`--cee3-sys-color-extended-on-warning`|`#ffffff`|`#4d2600`|Foreground text/icon color against an `extended-warning` background.|
|`--cee3-sys-color-extended-on-warning-container`|`#6d3a01`|`#ffe07c`|Foreground text/icon color against an `extended-warning-container` background.|
|`--cee3-sys-color-extended-on-yellow`|`#ffffff`|`#4d2600`|Foreground text/icon color against an `extended-yellow` background.|
|`--cee3-sys-color-extended-on-yellow-container`|`#6d3a01`|`#ffe07c`|Foreground text/icon color against an `extended-yellow-container` background.|
|`--cee3-sys-color-extended-orange`|`#9a4600`|`#ffb683`|Tertiary/accent color for orange-themed cards, chips, or focus elements.|
|`--cee3-sys-color-extended-orange-container`|`#ffdcc3`|`#753403`|Background container for orange-themed UI blocks.|
|`--cee3-sys-color-extended-pink`|`#b60d6e`|`#ffaee4`|Accent color for pink-themed tags, badges, and creative callouts.|
|`--cee3-sys-color-extended-pink-container`|`#ffd8ef`|`#8d0053`|Background container for pink-themed UI blocks.|
|`--cee3-sys-color-extended-purple`|`#7438d2`|`#d9bafd`|Accent color for purple-themed tags, badges, and creative callouts.|
|`--cee3-sys-color-extended-purple-container`|`#eedcfe`|`#5629a4`|Background container for purple-themed UI blocks.|
|`--cee3-sys-color-extended-red`|`#b3251e`|`#ffb3ae`|Semantic color for high-priority alerts, errors, and destructive actions.|
|`--cee3-sys-color-extended-red-container`|`#ffdadc`|`#8a1a16`|Background container for high-priority error prompts and destructive dialogs.|
|`--cee3-sys-color-extended-success`|`#006c35`|`#80da88`|Identical to extended-green; used for active validation and checklist success states.|
|`--cee3-sys-color-extended-success-container`|`#beefbb`|`#00522c`|Tonal background container for checklist success states.|
|`--cee3-sys-color-extended-warning`|`#8f4e06`|`#fcbd00`|Identical to extended-caution; used for warning banners and status alerts.|
|`--cee3-sys-color-extended-warning-container`|`#ffe07c`|`#6d3a01`|Background container for warning banners.|
|`--cee3-sys-color-extended-yellow`|`#8f4e06`|`#fcbd00`|Identical to extended-caution; used for warning banners and status alerts.|
|`--cee3-sys-color-extended-yellow-container`|`#ffe07c`|`#6d3a01`|Background container for yellow accents.|

#### 2.1.1 State Layer Opacity Tokens

To visualize interaction states systematically on any color role, Elements GM3 overlays a semi-transparent state layer using the same color as the content/foreground (typically the corresponding `on-` color role) with fixed opacity:

*   **Hover State Layer:** `0.08` (8% opacity)
*   **Focus State Layer:** `0.10` (10% opacity)
*   **Pressed State Layer:** `0.10` (10% opacity, accompanied by touch ripple animations on mobile)
*   **Dragged State Layer:** `0.16` (16% opacity)

### 2.2 Typography Link Tokens

The following table details the specialized Link typescale tokens of Elements GM3. These styles bind directly to their target fonts, line heights, and letter-spacing values to maintain maximum readability and visual hierarchy on all screen sizes.

|CSS Custom Property|Standard Rule Value|Font Family|Line Height|
|:---|:---|:---|:---|
|`--cee3-typescale-body-large-link`|`1rem/1.5rem 'Google Sans Text'`|Google Sans Text|`1.5rem` (`24px`)|
|`--cee3-typescale-body-medium-link`|`0.9rem/1.3rem 'Google Sans Text'`|Google Sans Text|`1.3rem` (`20.8px`)|
|`--cee3-typescale-body-small-link`|`0.8rem/1rem 'Google Sans Text'`|Google Sans Text|`1rem` (`16px`)|
|`--cee3-typescale-display-large-link`|`3.6rem/4rem 'Google Sans'`|Google Sans (Brand)|`4rem` (`64px`)|
|`--cee3-typescale-display-medium-link`|`2.8rem/3.3rem 'Google Sans'`|Google Sans (Brand)|`3.3rem` (`52.8px`)|
|`--cee3-typescale-display-small-link`|`2.3rem/2.8rem 'Google Sans'`|Google Sans (Brand)|`2.8rem` (`44.8px`)|
|`--cee3-typescale-headline-large-link`|`2rem/2.5rem 'Google Sans'`|Google Sans (Brand)|`2.5rem` (`40px`)|
|`--cee3-typescale-headline-medium-link`|`1.8rem/2.3rem 'Google Sans'`|Google Sans (Brand)|`2.3rem` (`36.8px`)|
|`--cee3-typescale-headline-small-link`|`1.5rem/2rem 'Google Sans'`|Google Sans (Brand)|`2rem` (`32px`)|
|`--cee3-typescale-label-large-link`|`500 0.9rem/1.3rem 'Google Sans Text'`|Google Sans Text|`1.3rem` (`20.8px`)|
|`--cee3-typescale-label-medium-link`|`500 0.8rem/1rem 'Google Sans Text'`|Google Sans Text|`1rem` (`16px`)|
|`--cee3-typescale-label-small-link`|`500 0.7rem/1rem 'Google Sans Text'`|Google Sans Text|`1rem` (`16px`)|
|`--cee3-typescale-title-large-link`|`1.4rem/1.8rem 'Google Sans'`|Google Sans (Brand)|`1.8rem` (`28.8px`)|
|`--cee3-typescale-title-medium-link`|`500 1rem 'Google Sans Text'`|Google Sans Text|`1.5rem` (`24px` default)|
|`--cee3-typescale-title-small-link`|`500 0.9rem/1.3rem 'Google Sans Text'`|Google Sans Text|`1.3rem` (`20.8px`)|

#### 2.2.1 Tracking (Letter Spacing) Link Tokens

*   `--cee3-typescale-body-small-link-tracking`: `0`
*   `--cee3-typescale-label-medium-link-tracking`: `0`
*   `--cee3-typescale-label-small-link-tracking`: `0`

#### 2.2.2 Typesetting Rules

Vertical typesetting within Elements GM3 relies on two methodologies:

1.  **Padding \& Bounding Boxes (Web/iOS):** Line height matches bounding box height. Text is vertically centered inside the bounding box according to CSS "half-leading" specs. Spacing is measured from container borders to the outer edge of the bounding box.
2.  **Baseline-to-Baseline Spacing (Android):** Spacing is measured strictly from the invisible baseline upon which characters rest. Line height represents the vertical distance between adjacent baselines.

### 2.3 Icon Font Token

The Elements GM3 design system utilizes variable-font-driven system iconography, bound to a single font family token:

*   `--md-icon-font: 'Google Symbols'`

#### 2.3.1 Google Symbols Axis Specifications

Google Symbols icons support dynamic visual customizations across five variable axes:

*   **Weight (wght):** Defines stroke weight from thin (`100`) to bold (`700`). Default resting weight is `400` (Regular).
*   **Fill (FILL):** Controls transition from outlined (`0`) to fully solid (`1`). Used primarily to denote active/selected states.
*   **Grade (GRAD):** Subtle thickness modifications (`-25` to `200`) without altering boundary dimensions. Used to compensate for visual bleed (e.g., setting `-25` for light icons on dark surfaces to maintain equal perceived weight).
*   **Optical Size (opsz):** Standard sizes from `20dp` to `48dp`. Automatically adjusts internal stroke density dynamically as boundaries change.
*   **Round (RND):** Controls stroke end rounding from sharp (`0`) to fully rounded (`100`). Default is `50`.

### 2.4 Shape Corner Radius Scale

The Elements GM3 shape system uses a standard, size-based scale with ten distinct corner radius styles. These are applied to component container outlines using logical design tokens.

|Shape Style|Radius (dp)|Typical Component Applications|
|:---|:---|:---|
|**None**|`0`|Full-screen containers, dividers, and compact web search views.|
|**Extra Small**|`4`|Small tooltips, snackbars, and narrow inputs.|
|**Small**|`8`|Chips (assist, filter, suggestion), checklists, and unselected segmented items.|
|**Medium**|`12`|Standard card containers, submenus, and text field outlines.|
|**Large**|`16`|Standard dialogs, navigation rails, and modal drawer container corners.|
|**Large Increased**|`20`|Dynamic lists, segmented buttons, and elevated panels.|
|**Extra Large**|`28`|Standard bottom sheets, modal side sheets, and carousel item containers.|
|**Extra Large Increased**|`32`|Expressive cards, expanded hero elements, and promotional banners.|
|**Extra Extra Large**|`48`|Prominent buttons, floating action buttons (FABs), and standalone sliders.|
|**Full**|`Full`|Fully rounded pill shapes (e.g., standard common buttons, switch tracks, and toggle handles).|

*   **Symmetry vs. Asymmetry:** Most components use symmetric corner radii. Asymmetric shapes are reserved for grouped elements (e.g., inner corners of split buttons and vertical menus) or conversational bubbles (e.g., suggestion chips).
*   **Optical Roundness Rule:** When nesting a rounded object inside another, the inner corner radius must be adjusted to prevent visual imbalance. The formula is: `Outer Radius - Padding = Inner Radius`.

### 2.5 Spacing, Padding, and Margins

Spacing in Elements GM3 layout structures is strictly measured in increments of **4dp**:

*   **Margins:** Space between the window edge and primary body content:
    *   *Compact Screens (\<600dp):* `16dp` margins.
    *   *Medium, Expanded, Large, and Extra-Large Screens (\>=600dp):* `24dp` margins.
*   **Spacers:** Vertical or horizontal gutters separating distinct panes. Spacers measure `24dp` wide and may contain layout drag handles.
*   **Padding:** Spacing between individual elements inside a container. Standard values include `4dp`, `8dp`, `12dp`, `16dp`, and `24dp`.

---

## 3. Core Component Specifications

Evaluating Elements GM3 compliance requires auditing component geometries, colors, and interactive behaviors against these baseline specifications.

### 3.1 Buttons \& Icon Buttons

#### 3.1.1 Common Buttons

*   **Variants:** Elevated, Filled, Tonal, Outlined, and Text.
*   **Geometries:**
    *   *Default Height:* `40dp` (Android/Web), `44pt` on iOS to align with system list cells.
    *   *Default Corner Radius:* `Full` (fully rounded pill container).
    *   *Label Text:* Sentence case only (e.g., "Add to calendar", not "ADD TO CALENDAR"). Truncation or wrapping is strictly prohibited; labels must remain on a single line.
    *   *Icon Placement:* Optional leading icon, placed before the text label.
*   **Interactive States \& Shape Morphing:**
    *   *Pressed State:* Buttons morph to a more boxy shape during tap interactions. The pressed corner radius is `8dp` (XS/S), `12dp` (M), or `16dp` (L/XL).
    *   *Toggle Variant:* Toggle buttons change their resting shape from round (`Full` unselected) to square (`12dp` to `28dp` corner radius when selected) in Google products.
*   **Color Fills (Light/Dark):**
    *   *Filled Button:* Uses `--cee3-sys-color-extended-blue-fill` container with `--cee3-sys-color-extended-on-blue-fill` label.
    *   *Tonal Button:* Uses `--cee3-sys-color-extended-blue-tonal` container with `--cee3-sys-color-extended-on-blue-tonal` label.
    *   *Outlined Button:* Transparent background with a `1dp` stroke using `--cee3-sys-color-extended-blue-outline`.

#### 3.1.2 Icon Buttons

*   **Variants:** Default (single-action) and Toggle (selection binary).
*   **Geometries:**
    *   *Standard Sizing:* Extra Small (`32dp`), Small (`40dp` default), Medium (`56dp`), Large (`96dp`), Extra Large (`136dp`).
    *   *Width Configurations:* Narrow, Default, Wide.
    *   *Touch Targets:* XS and S icon buttons must retain a minimum physical interaction target of `48x48dp` (`44x44pt` on iOS) via invisible padding.
*   **Visual Selection Cues (Toggle):**
    *   Unselected states use an outlined icon (Google Symbols `FILL@0`, `wght@400`).
    *   Selected states use a filled icon (Google Symbols `FILL@1`, `wght@400`). If a filled version is unavailable, the stroke weight must increase to semibold (`wght@600`). Selection must never be communicated through color alone.

### 3.2 Button Groups

Button groups organize interactive buttons and icon buttons into aligned clusters.

*   **Standard Button Group:**
    *   An invisible container holding adjacent buttons separated by responsive padding.
    *   *Pressed/Selected Behavior:* Pressing a button dynamically morphs its shape and width while pushing adjacent buttons, creating a tactile ripple effect.
*   **Connected Button Group:**
    *   *Purpose:* Replaces the legacy segmented button component for single-select or multi-select options.
    *   *Anatomy:* Elements are fully contiguous, separated only by a `2dp` internal divider line. Container has fully rounded corners.
    *   *Selected state:* Toggle buttons within the group morph from round (unselected) to square (selected).

### 3.3 Chips

Chips help users enter information, make selections, filter content, or trigger contextual actions.

*   **Variants:** Assist, Filter, Input, and Suggestion chips.
*   **Geometries:**
    *   *Default Height:* `32dp` (Mobile), `36dp` (Desktop).
    *   *Default Corner Radius:* `8dp` rounded rectangle. People chips (representing users) are an exception, utilizing a fully rounded `16dp` radius.
    *   *Spacing:* Minimum `8dp` horizontal/vertical spacing between adjacent chips.
    *   *Minimum Target Area:* Must retain a `48x48dp` touch target, extending beyond the visible container if necessary.
*   **Icon and Media Rules:**
    *   *Leading Visual:* Optional circular avatar (`24dp` diameter, `12dp` radius) or standard icon (`18dp` bounding box).
    *   *Trailing Visual:* Optional remove icon (`18dp`) for input and filter chips.
*   **A11y Reflow Methods:**
    When chip rows exceed the screen width on vertical scrollable pages, they must not scroll infinitely horizontally. Instead, use a leading Filter chip to trigger either a **Reflow** (wrapping chips to a second row) or a **Menu** (collapsing overflowing options into a standard vertical menu).

### 3.4 Text Fields

Text fields allow users to enter and edit text inputs.

*   **Variants:** Filled and Outlined text fields.
*   **Geometries:**
    *   *Default Height:* `56dp` (`44dp` on iOS).
    *   *Corner Radius:* Filled container has `4dp` top corner radius and square bottom; Outlined container has a symmetric `12dp` corner radius (`10dp` on iOS).
    *   *Stroke Weight:* `1dp` default outline; increases to `2dp` when active/focused.
*   **Text and Label Alignment:**
    *   *Unpopulated State:* The text label is vertically centered within the container.
    *   *Populated / Active State:* The label shrinks proportionally and moves to the top edge (or rests atop the outline in Outlined variants).
    *   *Adjacent Labels:* Must align with the leading edge of the container.
*   **Validation \& Error States:**
    *   *Visual Indicators:* In an error state, the container outline/stroke, label text, and supporting text must change to `--cee3-sys-color-extended-red` (Light theme) or `--cee3-sys-color-extended-on-red-container` (Dark theme).
    *   *Error Icon:* An exclamation or alert icon (`24dp`) must be displayed on the trailing edge of the field.
    *   *Error Text:* Replaces default supporting helper text below the field to prevent layout shift.

### 3.5 Menus \& Dropdowns

*   **Variants:** Vertical (Expressive) and Baseline Dropdown menus.
*   **Anatomy:** Contains a vertical stack of menu items enclosed within a container surface (`12dp` Medium corner radius). Standard vertical menus utilize gaps or `1dp` dividers to group related items.
*   **Color Mappings:**
    *   *Standard:* Surface-based container, low visual emphasis.
    *   *Vibrant:* Tertiary-based, high visual emphasis. Used sparingly for critical context actions.
*   **Interactive Behavior (Magic Triangle):**
    On web environments, submenus must implement "magic triangle" cursor tracking. This prevents submenus from closing when the cursor moves diagonally from a trigger item to the submenu container.
*   **Submenu Focus Morphing:**
    As focus shifts between nested submenus, the corner shapes morph dynamically: the active submenu corners become more rounded, while the unfocused menu corners flatten slightly.

### 3.6 Switches, Checkboxes, and Radio Buttons

#### 3.Switch

*   **Geometries:**
    *   *Track:* `32dp` height, `52dp` width. Rounded corners (`Full`).
    *   *Handle:* Unselected handle is `16dp` wide; selected handle is `24dp` wide. Under pressed states, the handle grows to `28dp` to provide interaction feedback.
*   **Anatomy:** Track enclosing a sliding handle. Handle can contain optional icons (e.g., checkmark for ON, cross for OFF) sized at `18dp` to denote selection states.

#### 3.6.2 Checkbox

*   **Geometries:**
    *   *Container:* `18dp` height, `18dp` width. Corner radius is `2dp`.
    *   *Check Icon:* `18dp` bounding box, centered.
    *   *State Layer:* `40dp` circular hover indicator; target size remains `48dp`.
*   **States:** Selected, Unselected, and Indeterminate (representing a partially checked parent-child group).

#### 3.6.3 Radio Button

*   **Geometries:**
    *   *Icon Diameter:* `20dp`.
    *   *Interactive Target:* `48dp` minimum.
*   **Selection Rule:** Only one radio button in a group can be active. Once selected, a radio button group cannot be deselected; developers must provide a "Clear" or "Not applicable" option if opt-out is required.

### 3.7 Tabs

Tabs organize related peer content at the same level of hierarchy.

*   **Variants:** Primary (placed beneath app bars, representing main destinations) and Secondary (nested within content panes to further divide content).
*   **Geometries:**
    *   *Height (Label Only):* `47dp`.
    *   *Height (Icon + Label):* `63dp`.
    *   *Icon Size:* `24dp`.
    *   *Divider Line:* `1dp` height, spanning the container base.
    *   *Primary Active Indicator:* A horizontal highlight line (`3dp` height) beneath the active tab text.
    *   *Secondary Active Indicator:* A horizontal highlight line (`2dp` height).
    *   *Active Indicator Shape:* `3dp` top-corner rounding (`3, 3, 0, 0` radii), minimum length of `24dp`.
*   **Navigation \& Scrollable Tabs:**
    *   *Fixed Tabs:* Show all tab items simultaneously; best for \<=4 destinations.
    *   *Scrollable Tabs:* Scroll horizontally when tabs exceed the viewport. The first tab must offset from the leading edge by exactly `52dp` (both web and mobile) to preserve readability.

### 3.8 App Bars

App bars are anchored at the top of the viewport to display navigation, page titles, and primary contextual actions.

*   **Variants:** Search app bar, Small, Medium flexible, and Large flexible.
*   **Geometries \& Layouts:**
    *   *Small App Bar:* `64dp` default height (`44pt` on iOS). Text and icons are vertically centered.
    *   *Medium Flexible App Bar:* Large title text, supports multi-line titles and subtitles, collapses to a Small app bar on scroll.
    *   *Large Flexible App Bar:* Emphasized title size, supports imagery backgrounds, collapses to a Small app bar on scroll.
    *   *Search App Bar:* Integrates a centered search input container inside the app bar area. The search input uses `--cee3-sys-color-extended-grey-container` by default to contrast with the app background.
*   **Anatomy rules:**
    *   *Container:* Spans 100% of the window width.
    *   *Leading Button:* Used exclusively for navigation (e.g., hamburger menu to open drawer, or back arrow).
    *   *Headline:* Short title. Do not truncate headlines; wrap to a second line in flexible variants.
    *   *Trailing Actions:* Maximum of two icon buttons on mobile. On larger screens, the Search app bar can scale to show up to four trailing actions.
*   **Scroll Behavior:** App bars should initially share the background color of the body pane, then fill with `--cee3-sys-color-extended-grey-container` (or corresponding surface container role) upon vertical scrolling to provide visual separation.

### 3.9 Navigation Components

#### 3.9.1 Navigation Bar (Bottom Navigation)

*   **Use Case:** Desktop-incompatible; reserved for compact and medium window widths (e.g., mobile devices).
*   **Geometries:**
    *   *Container Height:* `64dp` (`56pt` on iOS).
    *   *Item Count:* Consistently displays 3 to 5 destinations of equal importance.
    *   *Item Layout:* Compact windows use vertical layouts (text below icon); medium windows use horizontal layouts (icon next to text).
*   **Active Indicator:** A pill-shaped background element enclosing the active icon. On selection, the pill expands outwards from the center of the icon along a single horizontal axis.

#### 3.9.2 Navigation Rail

*   **Use Case:** Placed along the leading edge (left in LTR, right in RTL) of medium, expanded, and large displays.
*   **Variants:** Collapsed and Expanded.
    *   *Collapsed Navigation Rail:* Standard vertical rail showing 3 to 7 icons with text labels below.
    *   *Expanded Navigation Rail:* Replaces the legacy navigation drawer on large screens. Standard width is `360dp`. Active indicator hugs the text label.
*   **Anatomy:** Holds navigation items, an optional top-aligned menu button (to toggle collapsed/expanded states), and an optional top-anchored FAB. The menu button and FAB are always top-aligned, while navigation items can align vertically centered or top-aligned.

#### 3.9.3 Navigation Drawer

*   **Use Case:** Standard drawers are permanently visible on expanded/large displays (`360dp` container width, `100%` height). Modal drawers use a translucent scrim overlay and apply to compact/medium viewports.
*   **Geometries:**
    *   *Standard Drawer Container:* `360dp` width, corner radius is `0, 16dp, 16dp, 0` (rounded on the trailing edge).
    *   *Active Indicator Height:* `56dp`, width is `336dp`. Corner radius is `28dp`.
*   **States \& Visuals:** Active items use filled icons; inactive items use outlined icons. Section labels and dividers segment long lists.

### 3.10 Sheets (Bottom \& Side)

#### 3.10.1 Bottom Sheets

*   **Variants:** Standard (co-exists with main screen, no scrim) and Modal (blocks interaction, requires scrim overlay).
*   **Geometries:**
    *   *Width:* Spans full window width up to a maximum of `640dp`.
    *   *Corner Radius:* `28dp` top corner radius.
    *   *Drag Handle:* Center-aligned horizontally. Top `48dp` portion of the sheet acts as the interactive hit target for resizing.
*   **Behavior (Predictive Back):** On Android, swiping left or right on a modal bottom sheet detaches the sheet edges from the screen boundaries, revealing a preview of the previous screen to signal it is closing.

#### 3.10.2 Side Sheets

*   **Variants:** Standard and Modal. Standard side sheets shrink the body pane width when opened; modal side sheets slide on top, blocking background clicks.
*   **Geometries:**
    *   *Width:* Standard is `360dp` by default; max-width is `400dp`.
    *   *Corner Radius:* Modal side sheets have a `16dp` corner radius. Standard side sheets have a `0dp` corner radius.
    *   *Padding:* `24dp` start/end padding; `16dp` if leading icons are present.
*   **Accessibility:** A visible close icon button is mandatory within all side sheets to ensure keyboard and screen reader accessibility.

### 3.11 Dialogs

Dialogs provide important prompts, requiring users to stop tasks and make a decision.

*   **Variants:** Basic (Alerts, quick lists) and Full-screen (multi-task flows).
*   **Geometries:**
    *   *Basic Dialog Container:* Minimum `280dp` width, maximum `560dp`. Corner radius is `28dp`.
    *   *Basic Dialog Padding:* `24dp` padding around top, left, right, and bottom edges. Spacing between action buttons is `8dp`. Spacing between body and actions is `24dp`.
    *   *Full-screen Dialog:* Compact window sizes only. Corner radius is `0dp`. Title text is start-aligned in the header. Header height is `56dp`.
*   **Anatomy Rules:**
    *   *Buttons:* Action buttons must align to the trailing edge (bottom-right in LTR). The confirmation action (e.g., "Save", "Create") must be positioned closest to the trailing edge. A basic dialog must contain a maximum of two buttons.
    *   *Scrim:* Dialogs must be modal, appearing above a semi-transparent scrim (`32%` opacity of the black/scrim color role) to block background interactions.

### 3.12 Sliders

*   **Variants:** Standard (continuous), Centered (positive/negative balance), and Range (minimum and maximum).
*   **Geometries (by Size Token):**
    *   *XS:* Track height `16dp`, label container `44x48dp`, handle width `4dp`, track corner shape `8dp`.
    *   *S:* Track height `24dp`, handle height `44dp`, track corner `8dp`.
    *   *M:* Track height `40dp`, handle height `52dp`, track corner `12dp`, inset icon `24dp`.
    *   *L:* Track height `56dp`, handle height `68dp`, track corner `16dp`, inset icon `24dp`.
    *   *XL:* Track height `96dp`, handle height `108dp`, track corner `28dp`, inset icon `32dp`.
*   **Interactive Behavior:**
    During drag interactions, the handle width shrinks and a numeric value indicator appears above. When hovered, the cursor must transform to a pointer.
*   **Color Contrast Anchor:**
    To ensure visual contrast, the end of a slider's inactive track must achieve at least a `3:1` contrast with the background. This is achieved by default using visual stop indicators (stops configuration).

### 3.13 Progress \& Loading Indicators

#### 3.13.1 Progress Indicators

*   **Variants:** Linear and Circular. Can be Determinate (known progress) or Indeterminate (unknown wait time).
*   **Geometries:**
    *   *Linear Progress:* `4dp` track thickness. Determinate linear indicators require a `4dp` circular **stop indicator** at the end of the active track to mark the progress boundary.
    *   *Circular Progress:* Bounding box ranges from `24dp` (compact) to `240dp` (extra-large).
    *   *Wavy Shape Configuration:* Linear and Circular indicators can morph into a wavy shape. Height is defined by **amplitude** and **wavelength** values.
*   **Integration in Buttons:** When integrated into a button container to show action-in-progress, the background track must be removed, and the active indicator must match the color of the button's label text.

#### 3.13.2 Loading Indicators

*   **Purpose:** Recommended as a replacement for indeterminate circular progress indicators for short-wait processes (200ms to 5s). Used natively in pull-to-refresh interactions.
*   **Anatomy:**
    *   *Active Indicator:* A looping shape morph sequence composed of seven unique geometric shapes from the Elements GM3 shape library.
    *   *Container:* Optional circular surface. Required when loading indicators are placed on top of visually busy backgrounds (such as imagery).
*   **Color Mappings:** Default state uses `--cee3-sys-color-extended-blue-fill` on a transparent surface. Contained states use `--cee3-sys-color-extended-on-blue-container` on an `--cee3-sys-color-extended-blue-container` background.

### 3.14 Dividers

Dividers are thin visual lines used to group elements, separate sections of content, or establish container boundaries.

*   **Variants:** Full-width (100% container width) and Inset.
*   **Geometries:**
    *   *Thickness:* `1dp`.
    *   *Inset Left Margin:* `16dp` default.
    *   *Inset Right Margin:* `0dp` (standard list inset) or `16dp` (middle-inset variant).
    *   *Spacing:* Standard vertical padding above and below a divider is `0dp` in list items, and `8dp` to `16dp` in card containers.

### 3.15 Carousels

Carousels display horizontally scrollable content collections.

*   **Layout Variants:**
    1.  *Multi-browse:* Shows at least one large, one medium, and one small item.
    2.  *Uncontained:* Items are equal size, flowing past the screen boundaries.
    3.  *Uncontained Multi-Aspect Ratio:* Items are varied widths (ratios `9:16` to `16:9`).
    4.  *Hero:* Spotlights one large item on the leading edge with a small item preview on the trailing edge.
    5.  *Center-aligned Hero:* Spotlights one large centered item flanked by small items on both sides.
    6.  *Full-screen:* Immersive, vertical/horizontal edge-to-edge large items.
*   **Geometries:**
    *   *Small Item Width:* `40dp` to `56dp` (dynamic).
    *   *Item Corner Radius:* `28dp` default rounding.
    *   *Padding:* `16dp` leading/trailing padding; `8dp` top/bottom padding; `8dp` item-to-item gutter.
*   **A11y Scrawl Mandate:**
    On vertically scrolling pages, carousels must include a standalone, accessible **"Show all"** button or arrow icon button near the header. This opens a dedicated, vertically-scrolling page of all carousel items, protecting keyboard-only and screen reader users from horizontal scrolling traps.

### 3.16 Banners

Banners display prominent, timely messages and optional contextual actions.

*   **Variants:** Basic and Rich.
*   **Geometries \& Shapes:**
    *   *Basic Banner (Square):* Spans 100% of the screen width with square corners (`0dp`). Positioned directly below app bars, above body content.
    *   *Basic Banner (Round):* `28dp` corner radius, centered at the top of the content pane with a minimum `16dp` margin.
    *   *Rich Banner:* Spans the width of its parent body pane. Supports an `80x80dp` leading image, multiple text lines, and buttons. Placed strictly inline with body content (scrolls with the page).
*   **Interactive Controls:** Banners support up to two small text buttons and an optional close icon button. Banners must be dismissible; a banner must contain either a text button to dismiss or a close icon button, but never both simultaneously.

### 3.17 Tooltips

Tooltips display brief contextual descriptions.

*   **Variants:** Plain and Rich.
*   **Geometries:**
    *   *Plain Tooltip:* `24dp` container height, `8dp` internal padding. Placed `4dp` from visual boundaries (buttons) or `8dp` from text baselines.
    *   *Rich Tooltip:* `12dp` top padding, `8dp` bottom padding, `16dp` side padding. Supports an optional title, description, and up to two inline text buttons.
*   **Behavior (Transient vs. Persistent):**
    *   *Transient:* Default state; appears on cursor hover or tap-and-hold, disappearing `1.5s` after the interaction ends.
    *   *Persistent Rich Tooltip:* Triggered only by clicking/tapping or automatically upon page load (e.g., to explain new features). Remains visible until the user interacts with another UI element.

### 3.18 Snackbars

Snackbars display brief, low-priority process updates at the bottom of the viewport.

*   **Geometries:**
    *   *Height:* `48dp` (one line of text) to `64dp` (two lines of text).
    *   *Alignment:* Fixed distance from leading, trailing, and bottom edges on mobile. Left-aligned or center-aligned on desktop.
*   **Interactive Actions:** Supports a single text button. Snackbars containing buttons must never auto-dismiss; they must remain visible until acted on or dismissed.
*   **A11y Web Mandate:** Auto-dismissing snackbars are prohibited on web unless an equivalent accessible notice is provided inline near the triggering element (e.g., a "Save" button updating its text label to "Saved" when pressed).

### 3.19 Floating Sheets

*   **Variants:** Standard and Modal. Modal variants appear above a scrim.
*   **Geometries:**
    *   *Margin:* `56dp` from screen boundaries on desktop.
    *   *Dimensions:* Minimum width `280dp`, maximum width `640dp`. Height is variable based on internal contents.
*   **Usability:** Floating sheets are non-disruptive, allowing users to scroll and interact with background content when non-modal.

### 3.20 Search Bars \& Views

*   **Search Bar Geometry:** Container height is `56dp`. Minimum width `360dp`, maximum width `720dp`. Symmetric rounded corners.
*   **Search View Geometry:** Container is full-screen on mobile. On larger displays, it is a modal container anchored directly below the search bar, with a maximum height of `2/3` of the viewport.
*   **Anatomy:** Contains a leading icon button (navigational or search), placeholder text ("Search..."), up to two trailing action buttons (voice, location, or close), and list-based autocomplete suggestions.

### 3.21 Split Buttons

Split buttons group a primary action button and a trailing menu-trigger button into a single component.

*   **Geometries:** Available in XS, S, M, L, XL sizes. Standard `2dp` padding separates the leading button and trailing button. Bounded by standard button shapes (Round or Square).
*   **Interactive Behavior:** Hovering or focusing applies a state layer overlay. Tapping the trailing menu button rotates its internal arrow icon 180° inwards and opens a menu `4dp` from the split button.

### 3.22 FAB Menu

A FAB menu opens from a Floating Action Button (FAB) to display multiple related primary actions.

*   **Anatomy:** Composed of a `56dp` circular **Close button** (which replaces the initiating FAB) and a vertical stack of 2 to 6 list-based **Menu items**.
*   **Geometries:** FAB menu items share the same measurements as standard medium common buttons. Margins from the window corner are `16dp` (compact) or `24dp` (expanded).
*   **Expanding Transition:** The transition must originate from the top-trailing corner of the FAB container. Menu items expand using a container transform transition pattern.

---

## 4. Agentic AI Specific Components (CE Elements Agentic)

Elements GM3 Agentic establishes visual and interactive standards for agentic AI features, ensuring users easily distinguish AI-generated processes.

### 4.1 Aurora Chat Interfaces

The **Aurora** system is Elements GM3's flagship chat framework.

*   **Aurora Chat Bubble:** Generates chat inputs and responses.
    *   *User Bubbles:* Right-aligned, utilizing a highly rounded container with an asymmetric trailing-bottom corner (`4dp` radius) to signify user speech.
    *   *AI Bubbles:* Left-aligned, utilizing neutral gray container colors.
*   **Aurora Prompt Field:** The main conversational text input. Placed at the bottom of the body pane, spanning full width with a rounded container.
*   **Aurora Suggestion Chips:** Asymmetric prompt-recommendation chips. The top-trailing corner has a `4dp` radius; all other corners are fully rounded. Suggestion chips utilize a high-emphasis color gradient.
*   **Aurora Response Carousel / List:** Scrollable cards or list items containing AI-generated multimodal responses (text, images, files, or progress bars).

### 4.2 AI Color Gradients \& Generating States

Generative AI actions in progress (such as an active loading state after a user submits a prompt) are communicated via a dynamic, moving color gradient.

*   **The AI Gradient Palette:** Derived from the primary color. It consists of six specific color values defined in Elements GM3:
    1.  `--cee3-sys-color-extended-blue-fill` (AI Main)
    2.  `--cee3-sys-color-extended-cyan` (AI Analog)
    3.  `--cee3-sys-color-extended-purple` (AI Complement)
    4.  `--cee3-sys-color-extended-blue-tonal` (AI Main Variant)
    5.  `--cee3-sys-color-extended-cyan-container` (AI Analog Variant)
    6.  `--cee3-sys-color-extended-purple-container` (AI Complement Variant)
*   **The Generating State Stroke:** When generating an AI output, text field borders or chip outlines are replaced with a `2dp` moving color gradient. The gradient rotates in a **counter-clockwise** direction. It must never remain static.
*   **Text \& Icon Contrast over AI Gradients:** Labels and icons placed on top of AI gradients (e.g., active AI buttons or chips) must use the primary color role (`--cee3-sys-color-extended-blue` or `--cee3-sys-color-extended-on-blue`) to guarantee at least a **4.5:1** contrast ratio.

---

## 5. Accessibility, Usability \& Verification Rules (GAR 2025/2026)

Elements GM3 enforces strict criteria to satisfy global accessibility requirements.

### 5.1 Physical Target Dimensions

All interactive UI elements must retain a minimum physical interaction size to prevent errors and support users with dexterity challenges:

*   **Mobile Touch Targets:** Minimum `48x48dp` (`44x44pt` on iOS). This size must be maintained even if the visual element (e.g., an icon) is smaller, by applying transparent target padding.
*   **Desktop Pointer Targets:** Minimum `44x44dp` (`40x40dp` in dense web tables).
*   **Target Spacing:** Adjacent interactive elements should be separated by at least `8dp` of open space to prevent accidental taps.

### 5.2 Keyboard Navigation \& Key Traversal

All interactive elements in an app must be keyboard-operable in a linear, predictable sequence:

*   **Focus Traversal Order:** Matches the visual top-to-bottom, left-to-right (or right-to-left in RTL) DOM structure.
*   **Keys:**
    *   `Tab` / `Shift+Tab`: Moves focus between interactive controls.
    *   `Arrow Keys`: Navigate within complex components (e.g., selecting cells in a data table, or items within a vertical menu).
    *   `Space` / `Enter`: Activates the currently focused control.
    *   `Escape`: Dismisses dialogs, menus, and persistent tooltips.
*   **Focus Indicators:** When focused via keyboard, an element must display a ring-like, high-contrast **keyboard focus indicator** around its visible container to track focus location.
*   **Keyboard Shortcuts:** Custom shortcuts must require a combination of two or more keys (e.g., `Ctrl+Z` to undo) to prevent accidental triggers by speech-to-text users.

### 5.3 Resizing and Truncation

*   **200% Text Scaling Support:** UI containers, text wrapping, and layouts must support text scaled up to 200% via device settings without overlapping, clipping, or losing informational context.
*   **Line Length Constraint:** Standard body text must be constrained to **40 to 60 characters** per line to optimize reading comprehension. Large displays may scale up to a maximum of 120 characters, provided line-height is adjusted proportionally.
*   **Truncation with Ellipses:** Crucial information must never be truncated permanently. If text is truncated using ellipses (`...`), the full content must be available to users via tooltip interactions (on cursor hover) or screen reader focus.

### 5.4 Screen Reader Verbalizations \& Labeling

*   **Accessibility Labels:** Describing the content, purpose, and behavior of visual-only components (e.g., icon-only buttons).
    *   *Icon-Only Buttons:* Label must describe the action performed (e.g., "Add to favorites", not "star icon").
    *   *Decorative Elements:* Decorative illustrations or icons that do not convey informational meaning must be hidden from screen readers (e.g., using `aria-hidden="true"` or `alt=""` in HTML) to prevent redundant verbalizations.
*   **Roles \& States:** All custom interactive elements must be assigned correct roles (e.g., `role="button"` or `role="dialog"`) and communicate state changes dynamically (e.g., `aria-expanded="true"` when a menu opens). Never include the element role within the descriptive label itself (e.g., labeling a search button as "search button" results in a screen reader announcing "search button, button").
