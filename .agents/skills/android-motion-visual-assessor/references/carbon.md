# Android Motion Design System Assessor's Reference \& Bug Manual

Welcome to the official, authoritative visual audit and bug identification manual for the **Android Motion Design System**. This document defines the structural specifications, component geometries, interactive state behaviors, and design token configurations required to evaluate visual rendering and codebase implementation for full design compliance.

Use this manual as a standard rubric and heuristic guidelines during UI inspection to identify and report design bugs, structural deviations, and visual hygiene violations.

---

## 1. Design System Foundations \& Bindings

The Android Motion design system relies on a set of semantic tokens mapped to specific values. All assessed interfaces (both mocks and codebases) must strictly resolve to these token families.

### 1.1 Color Palette \& System Roles

Android Motion utilizes a tone-based system mapping contrasting colors for maximum legibility (minimum 3:1 for large text/graphics, 4.5:1 for small body text). In codebase audits, confirm properties are bound to `--droid-sys-color-*` variables rather than hardcoded hex values.

|Token Key|Light Theme Value|Dark Theme Value|Core Application|
|:---|:---|:---|:---|
|`--droid-sys-color-primary`|`#0b57d0`|`#a8c7fa`|Primary brand accent; high-emphasis text and active states.|
|`--droid-sys-color-on-primary`|`#ffffff`|`#062e6f`|Foreground text/icons on primary color containers.|
|`--droid-sys-color-primary-container`|`#d3e3fd`|`#0842a0`|Standout background for key actions and components.|
|`--droid-sys-color-on-primary-container`|`#041e49`|`#d3e3fd`|Highly legible foreground elements on primary container fills.|
|`--droid-sys-color-secondary`|`#00639b`|`#7fcfff`|Secondary accent; less prominent labels or active highlights.|
|`--droid-sys-color-on-secondary`|`#ffffff`|`#003355`|Foreground text/icons on secondary color surfaces.|
|`--droid-sys-color-secondary-container`|`#c2e7ff`|`#004a77`|Fills for recessive elements like tonal buttons or toggle tracks.|
|`--droid-sys-color-on-secondary-container`|`#001d35`|`#c2e7ff`|Text/icons on secondary containers.|
|`--droid-sys-color-tertiary`|`#146c2e`|`#6dd58c`|Contrasting accents or subtle emphasis highlights.|
|`--droid-sys-color-on-tertiary`|`#ffffff`|`#0a3818`|Text/icons against tertiary surfaces.|
|`--droid-sys-color-tertiary-container`|`#c4eed0`|`#0f5223`|High-contrast fills for distinct input blocks.|
|`--droid-sys-color-on-tertiary-container`|`#072711`|`#c4eed0`|Foreground details against tertiary containers.|
|`--droid-sys-color-background`|`#fdfcfb`|`#1f1f1f`|Static workspace canvas behind scrollable content.|
|`--droid-sys-color-on-background`|`#1f1f1f`|`#e3e3e3`|Body copy and content iconography on page backgrounds.|
|`--droid-sys-color-surface`|`#fdfcfb`|`#1f1f1f`|Component backdrops (cards, sheets, dropdown menus).|
|`--droid-sys-color-on-surface`|`#1f1f1f`|`#e3e3e3`|Default high-contrast foreground text/icons on surfaces.|
|`--droid-sys-color-surface-variant`|`#e1e3e1`|`#444746`|Alternate background or container fill indicating state.|
|`--droid-sys-color-on-surface-variant`|`#444746`|`#c4c7c5`|Mid-emphasis details, metadata, and placeholder text.|
|`--droid-sys-color-outline`|`#747775`|`#8e918f`|Structural lines, field borders, and high-contrast dividers.|
|`--droid-sys-color-inverse-surface`|`#303030`|`#e3e3e3`|Opposite contrast background for snackbars and banners.|
|`--droid-sys-color-inverse-on-surface`|`#f2f2f2`|`#303030`|High-contrast label text on inverse surfaces.|
|`--droid-sys-color-inverse-primary`|`#a8c7fa`|`#0b57d0`|Action links or highlights shown against inverse surfaces.|
|`--droid-sys-color-error`|`#b3261e`|`#f2b8b5`|Severe errors, validation failures, or destructive actions.|
|`--droid-sys-color-on-error`|`#ffffff`|`#601410`|Foreground elements on error container backdrops.|
|`--droid-sys-color-error-container`|`#f9dedc`|`#8c1d18`|Fills highlighting critical failure fields.|
|`--droid-sys-color-on-error-container`|`#410e0b`|`#f2b8b5`|Context text/icons displayed within error containers.|
|`--droid-sys-color-shadow`|`#000000`|`#000000`|Shadow casting applied for structural depth.|

### 1.2 Typography \& Type Scale

The type scale employs two brand-aligned typefaces: **Google Sans** for expressive, larger roles (display, headline, title) and **Google Sans Text** for plain, highly-readable roles (body, label).

|System Token Key|Font Style (Weight / Size / Line-Height / Family)|Typical Layout Application|
|:---|:---|:---|
|`--droid-sys-typescale-display-large`|`3.6rem / 4rem 'Google Sans'`|Hero numerals, high-impact branding blocks.|
|`--droid-sys-typescale-display-medium`|`2.8rem / 3.3rem 'Google Sans'`|Standalone short visual titles on large screens.|
|`--droid-sys-typescale-display-small`|`2.3rem / 2.8rem 'Google Sans'`|Compact promotional headers, editorial entries.|
|`--droid-sys-typescale-headline-large`|`2rem / 2.5rem 'Google Sans'`|Primary page-level headers and screen descriptors.|
|`--droid-sys-typescale-headline-medium`|`1.8rem / 2.3rem 'Google Sans'`|Section headers inside panels or split-panes.|
|`--droid-sys-typescale-headline-small`|`1.5rem / 2rem 'Google Sans'`|Nested subsection titles within cards or sheets.|
|`--droid-sys-typescale-title-large`|`1.4rem / 1.8rem 'Google Sans'`|Component-level headers (Dialog titles, App bars).|
|`--droid-sys-typescale-title-medium`|`500 1rem / 1.5rem 'Google Sans Text'`|Subheadings, selected list labels, unread items.|
|`--droid-sys-typescale-title-small`|`500 0.9rem / 1.3rem 'Google Sans Text'`|Compact card headers, nested labels.|
|`--droid-sys-typescale-body-large`|`1rem / 1.5rem 'Google Sans Text'`|Primary paragraph copy, form entries, description text.|
|`--droid-sys-typescale-body-medium`|`0.9rem / 1.3rem 'Google Sans Text'`|Supporting blocks, secondary descriptions.|
|`--droid-sys-typescale-body-small`|`0.8rem / 1rem 'Google Sans Text'`|Captions, legal caveats, helper text below form fields.|
|`--droid-sys-typescale-label-large`|`500 0.9rem / 1.3rem 'Google Sans Text'`|Component actions (Button text, Tab headers).|
|`--droid-sys-typescale-label-medium`|`500 0.8rem / 1rem 'Google Sans Text'`|Segment headers, compact action buttons.|
|`--droid-sys-typescale-label-small`|`500 0.7rem / 1rem 'Google Sans Text'`|Badge counters, status trackers, sub-labels.|

### 1.3 Corner Radius Scale

The Android Motion system defines standard rounding increments. To ensure optical roundness when nesting elements (e.g., a card containing a button), the inner radius should equal `Outer Radius - Padding`.

|Token Key|Rounding Radius (px)|Typical Layout Application|
|:---|:---:|:---|
|`--droid-sys-shape-corner-none`|`0`|Full-bleed elements (e.g. full-screen dialogs, edge-to-edge app bars).|
|`--droid-sys-shape-corner-extra-small`|`4px`|Tiny elements, input indicators, custom details.|
|`--droid-sys-shape-corner-small`|`8px`|Chips, standard card sub-components, small popovers.|
|`--droid-sys-shape-corner-medium`|`12px`|Standard cards, split-button inner corners, menus.|
|`--droid-sys-shape-corner-large`|`16px`|Standard side sheets, modal dialog boundaries.|
|`--droid-sys-shape-corner-extra-large`|`28px`|Bottom sheets, expanded carousels, large overlays.|
|`--droid-sys-shape-corner-full`|`max(50cqw, 50cqh)`|Fully rounded elements (e.g. pill buttons, switch handles, sliders).|

### 1.4 Elevation Levels

Elevation in Android Motion is expressed primarily through tonal differences (using surface container variants) and selectively through visible shadows to separate overlapping surfaces.

- **`--droid-sys-elevation-level0` (`0`):** Resting buttons, filled/outlined cards, dividers, list items, background.
- **`--droid-sys-elevation-level1` (`1px`):** Tonal buttons, elevated cards, modal side sheets, banner surfaces.
- **`--droid-sys-elevation-level2` (`3px`):** Scrolled app bars, floating menus, navigation bars, toolbars.
- **`--droid-sys-elevation-level3` (`6px`):** Modal dialogs, floating action buttons (FABs) at rest, time/date pickers.
- **`--droid-sys-elevation-level4` (`8px`):** Hovered or focused interactive modal components.
- **`--droid-sys-elevation-level5` (`12px`):** Active dragged states or highly elevated panels.

### 1.5 Motion Easing \& Springs

Android Motion prioritizes **physics-based spring animations** for micro-interactions and gestures to make the interface feel responsive and alive. Legacy easing curves are reserved only for basic screen transitions.

- **Emphasized Easing (Default Transition):** `cubic-bezier(0.2, 0.0, 0.0, 1.0)` / `--droid-sys-motion-easing-emphasized`
- **Emphasized Decelerate (Enter Screen):** `cubic-bezier(0.05, 0.7, 0.1, 1.0)` / `--droid-sys-motion-easing-emphasized-decelerate`
- **Emphasized Accelerate (Exit Screen):** `cubic-bezier(0.3, 0.0, 0.8, 0.15)` / `--droid-sys-motion-easing-emphasized-accelerate`
- **Standard Easing (Utility Transitions):** `cubic-bezier(0.2, 0.0, 0.0, 1.0)` / `--droid-sys-motion-easing-standard`

---

## 2. Interactive States \& Shape Morphing

Android Motion uses interactive **shape morphing** to indicate state changes rather than relying solely on color shifts. This adds dynamic, physical feedback during touch or keyboard input.

### 2.1 State Layers \& Opacity

State layers are semi-transparent overlays that sit directly on top of components, matching the color of their nested content (icons or labels):

- **Hover State:** Apply a `10%` opacity state layer of the content color.
- **Focus State:** Apply a `12%` opacity state layer of the content color, accompanied by a high-contrast focus ring.
- **Pressed State:** Represented by an active visual ripple or a shape morph.
- **Dragged State:** Apply a `16%` opacity state layer, accompanied by a dynamic elevation rise of `+1` level.

### 2.2 Shape Morphing Mechanics

To signify changes in selection and focus, components physically change their corner radii:

- **Button Pressed State:** Pill-shaped buttons (`--droid-sys-shape-corner-full`) morph towards squarer corners (e.g., `8px` or `12px` corner radius) while active.
- **Toggle Button Selection:** Unselected buttons are round (`--droid-sys-shape-corner-full`), but morph directly to square corners (`12px` or `16px`) when toggled to the **Selected** state.
- **Focus Progression:** In composite elements (such as submenus), moving focus between items causes the active item's corners to become highly rounded while the unfocused items flatten out.

---

## 3. Component-Specific Specifications

Every layout component must strictly adhere to the following geometric, color, and behavior specifications. Deviation from these parameters constitutes a design bug.

### 3.1 Buttons \& Icon Buttons

Buttons are primary call-to-actions. Standard arrangements should limit high-emphasis buttons to one per screen.

#### Standard Buttons (Filled, Tonal, Outlined, Elevated, Text)

- **Geometries:**
  - **Height:** `56dp` (Large), `40dp` (Small / default), `32dp` (Extra Small). On iOS systems, default small button height increases to `44pt` to match platform standards.
  - **Target Size:** Must retain a minimum interactive target area of `48 x 48dp`.
  - **Unselected Shape:** Pill-shaped by default (`--droid-sys-shape-corner-full`).
  - **Selected Shape (Toggle Mode):** Morphs to square (`12dp` or `16dp` corner radius).
  - **Pressed State Morph:** Corner radius shrinks dynamically to `8dp` (Small) or `12dp` (Medium) under direct touch.
- **Colors:**
  - **Filled:** Background is `--droid-sys-color-primary` (`#0b57d0` / `#a8c7fa`); text/icons are `--droid-sys-color-on-primary`.
  - **Tonal:** Background is `--droid-sys-color-secondary-container` (`#c2e7ff` / `#004a77`); text/icons are `--droid-sys-color-on-secondary-container`.
  - **Outlined:** Boundary stroke is `1px` using `--droid-sys-color-outline` (`#747775` / `#8e918f`); no container fill.

#### Split Buttons

- **Anatomy:** Consists of a leading action button and a trailing dropdown menu icon button separated by a `2dp` gap.
- **Geometries:** Inner corners are square (`--droid-sys-shape-corner-none`) to visually group the two elements, while outer corners maintain button defaults.
- **Selected Behavior:** Selecting the menu button rotates the icon `180°` using `--droid-sys-motion-easing-standard`.

---

### 3.2 Floating Action Buttons (FABs)

FABs represent the most important, constructive action on a screen.

#### Medium FAB (Default)

- **Geometries:**
  - **Size:** `56 x 56dp`.
  - **Resting Elevation:** `--droid-sys-elevation-level3` (`6dp`).
  - **Hover/Focus Elevation:** `--droid-sys-elevation-level4` (`8dp`).
  - **Shape:** Round container (`28px` corner radius / `--droid-sys-shape-corner-extra-large`).
- **Colors:** Default mapping is `--droid-sys-color-primary-container` backdrop with `--droid-sys-color-on-primary-container` iconography.
- **Compliance Rules:** Must remain persistent on screen during scroll. Never apply a badge or notification indicator directly to a FAB container.

#### Extended FAB

- **Anatomy:** Combines a leading system icon with a concise text label (`title-medium` typography).
- **Behavior:** On scroll down, the Extended FAB collapses into a standard medium FAB to maximize content space; it expands back when scrolling up.
- **Geometries:** Container width is dynamic, hugging text with `16dp` horizontal padding.

---

### 3.3 App Bars

App bars reside at the top of the screen to guide local navigation and brand identity.

#### Search App Bar

- **Geometries:**
  - **Height:** `56dp`.
  - **Width:** Spans `100%` of window width.
  - **Alignment:** Text placeholder is start-aligned with `16dp` left padding.
- **Colors:** Defaults to `--droid-sys-color-surface-variant` (`#e1e3e1` / `#444746`) to distinguish it from the page background. On scroll, it transitions container color to `--droid-sys-color-surface-variant` with a prominent `1px` bottom border of `--droid-sys-color-outline`.

#### Flexible App Bars (Medium \& Large)

- **Anatomy:** Supports multi-line titles, optional subtitles, and background imagery.
- **Behavior:** On page scroll, collapsible medium and large app bars must smoothly transform into a compact `56dp` app bar using `--droid-sys-motion-easing-emphasized-decelerate`.

---

### 3.4 Navigation Suite

Android Motion organizes layout structure across compact, medium, and expanded screens.

#### Navigation Bar

- **Application:** Reserved for compact (mobile) screens only.
- **Geometries:** Height is `64dp`. Contains `3` to `5` evenly-spaced destinations. On iOS platforms, height is shortened to `56pt` to maintain system patterns.
- **Active Indicator:** Highlighted with a pill backdrop (`--droid-sys-shape-corner-full`) using `--droid-sys-color-secondary-container`.
- **Colors:** Inactive icons are `--droid-sys-color-on-surface-variant`; active icons use `--droid-sys-color-on-secondary-container`. Active text uses `--droid-sys-color-secondary`.

#### Navigation Rail

- **Application:** Ideal for medium screens (tablets) in vertical layout.
- **Anatomy:** Standard vertical container along the leading edge containing `3` to `7` destinations.
- **Expanded Rail (Expressive):** Integrates modal and non-modal behaviors, substituting the old navigation drawer on tablets and desktops.
- **Alignment:** Menu buttons and FABs are top-aligned, while destination items are center-grouped for ergonomic reachability.

---

### 3.5 Sliders

Sliders allow users to make continuous or discrete selections from a range of values.

- **Geometries:**
  - **Track Height:** XS (`16dp`), S (`24dp`), M (`40dp`), L (`56dp`), XL (`96dp`).
  - **Active Track shape:** Fully rounded endpoints.
  - **Handle Width:** Shrinks and flattens dynamically when actively dragged to indicate tactile pressure.
- **Visual Contrast Rule:** To ensure accessibility, stop indicators or visual anchors at the end of the inactive track must maintain a minimum `3:1` contrast with the background.
- **Inset Icon:** Standard sliders (sizes M, L, XL) can contain an inset icon inside the active track. When the handle slides near the zero-value, the icon must smoothly transition to the inactive track.

---

### 3.6 Bottom Sheets \& Side Sheets

Sheets provide supplemental content surfaces anchored to window edges.

#### Bottom Sheets (Standard \& Modal)

- **Geometries:**
  - **Shape:** Top corners have a `28px` radius (`--droid-sys-shape-corner-extra-large-top`); bottom corners are square (`0`).
  - **Drag Handle:** Central top-aligned bar with a minimum `48 x 48dp` touch target area.
  - **Sizing:** Spans full window width up to a maximum cap of `640dp`. When screen width exceeds `640dp`, side and top margins of `56dp` are enforced.
- **Predictive Back Gestures:** When swiping left or right, the sheet container must visually detach from the screen boundaries and shrink to preview the previous destination.

#### Side Sheets

- **Application:** Used on expanded/large desktop windows on the trailing edge of the screen (or leading edge for RTL).
- **Geometries:** Container height is `100%`, with a fixed standard width of `360dp`. Outer corners have a `16px` radius.

---

### 3.7 Chips

Chips represent discrete, dynamic contextual filters, inputs, or tasks.

- **Anatomy:** Consists of a container, a label text (`label-large`), an optional leading icon/avatar, and an optional trailing remove button.
- **Geometries:**
  - **Height:** `32dp`.
  - **Shape:** Rounded rectangle with `8px` corner radius (`--droid-sys-shape-corner-small`). People chips use fully rounded shapes (`16px`).
  - **Padding:** `16dp` horizontal padding. When leading icons are present, left padding is reduced to `8dp`.
- **States:**
  - **Filter Chip Selected State:** Appends a leading checkmark icon and changes the background to `--droid-sys-color-secondary-container`.
  - **Input Chip Remove Action:** Trailing close icon button must have a dedicated touch target of at least `48 x 48dp` (achieved on a `32dp` chip by extending target padding beyond the visual container bounds).

---

### 3.8 Progress \& Loading Indicators

Indicate background processes and wait times.

#### Progress Indicators (Linear \& Circular)

- **Determinate Mode:** Displays actual progress. Linear determinate progress indicators require a `4dp` circular **stop indicator** at the end of the active track to mark the boundary clearly.
- **Wavy Shape (Expressive):** For enhanced brand expression, the linear active track can morph into a wavy pattern utilizing specific **amplitude** and **wavelength** attributes.
- **Accessibility Contrast:** The active indicator must maintain a minimum `3:1` contrast with the background.

#### Loading Indicator

- **Anatomy:** A looping shape morph sequence composed of seven unique shapes from the Android Motion shape library.
- **Containment:** When displayed directly on a surface, the indicator has no container. When overlaid on top of content (e.g. pull-to-refresh), it must be housed inside a circular container with an `--droid-sys-color-primary-container` fill.

---

### 3.9 Dialogs

Dialogs are highly interruptive windows requiring immediate action.

#### Basic Dialog

- **Geometries:**
  - **Width:** Min `280dp`, Max `560dp`.
  - **Shape:** Rounded container with a `28px` corner radius (`--droid-sys-shape-corner-extra-large`).
  - **Padding:** Standard `24dp` top/bottom/left/right padding around content.
  - **Actions:** Button triggers are aligned to the trailing edge. The confirmation button is placed closest to the outer edge. Max `2` actions are recommended.
- **Scrim:** Background overlay is dimmed to `32%` opacity using `--droid-sys-color-shadow`.

#### Full-Screen Dialog

- **Application:** Compact window sizes only.
- **Geometries:** Container has `0` corner rounding (`--droid-sys-shape-corner-none`) and covers the entire screen.
- **Header:** Features a `56dp` header containing a leading close 'X' icon button and a trailing 'Save' action.

---

### 3.10 Carousels

Carousels showcase visual collections scrolled on a horizontal axis.

|Carousel Layout|Visual Behavior / Sizing|Scrolling Interaction|
|:---|:---|:---|
|**Multi-Browse**|Displays at least one large, medium, and small item. Small items are `40--56dp`.|**Snap-Scrolling** (releases snap to the grid boundaries).|
|**Uncontained**|Traditional layout where all items are uniform size and scroll past screen boundaries.|Standard scrolling.|
|**Hero**|Spotlights one large item with a small preview on the trailing edge.|**Snap-Scrolling** (swipes items one at a time).|
|**Center-Aligned Hero**|Centers the prominent large item, previewing small items on both left and right edges.|**Snap-Scrolling**.|
|**Full-Screen**|Immersive vertically-scrolling full-frame visual feed (compact/portrait only).|Strict edge-to-edge **Snap-Scrolling**.|

- **Geometries:** Corner radius of carousel items is `28px` (`--droid-sys-shape-corner-extra-large`).
- **Usability Heuristic:** Vertically-scrolling screens containing carousels must feature an accessible **"Show All"** or "See More" button to allow users with screen readers or limited mobility to view items sequentially without horizontal panning.

---

### 3.11 Cards

Cards collect content and actions about a single topic.

- **Geometries:**
  - **Shape:** Rounded rectangle with `12px` corner radius (`--droid-sys-shape-corner-medium`).
  - **Padding:** `16dp` left/right padding.
- **Variants:**
  - **Elevated:** Backdrop is `--droid-sys-color-surface`; resting elevation is `--droid-sys-elevation-level1`.
  - **Filled:** Backdrop is `--droid-sys-color-surface-variant` (`#e1e3e1` / `#444746`); resting elevation is `--droid-sys-elevation-level0`.
  - **Outlined:** Backdrop is `--droid-sys-color-surface`; boundary stroke is `1px` `--droid-sys-color-outline`; resting elevation is `--droid-sys-elevation-level0`.
- **Interaction Constraints:**
  - **Actionable Cards:** The entire card container acts as a single tab stop. Placing nested interactive buttons or links directly inside an actionable card container is strictly prohibited to prevent overlapping tap targets.
  - **Non-Actionable Cards:** The card itself is not a tab stop, but nested buttons or action links inside must be individually focusable.

---

### 3.12 Button Groups

Button groups align multiple actions together in a structured arrangement.

#### Standard Button Groups

- **Behavior:** Pressing or selecting a button dynamically adjusts its width/shape and morphs adjacent buttons to create cooperative physical movement.
- **Geometries:** Standard groups apply responsive padding between buttons based on component size.

#### Connected Button Groups

- **Behavior:** Replaces the legacy segmented button. Buttons are joined side-by-side. Tapping a button morphs only its individual shape and state color, leaving neighbors unaffected.
- **Geometries:** Features a tight, consistent `2dp` padding between all connected elements.

---

### 3.13 Banners \& Badges

Information and notification details.

#### Banners (Basic \& Rich)

- **Basic Banner:** Displays `1` or `2` lines of text. Available in **Square** (spans `100%` width at the top of the screen) and **Round** (features `28px` rounded corners and `16dp` layout margins).
- **Rich Banner:** Displays complex text, images, and buttons. Must be placed inline with page content.
- **Vibrant Color Mode:** High-emphasis option mapping background colors to `--droid-sys-color-tertiary-container` to grab user attention.

#### Badges

- **Small Badge:** Simple `6 x 6dp` circle used for general notifications. Mapped to `--droid-sys-color-error`.
- **Large Badge:** Contains label text (maximum `4` characters). Height is `16dp`.
- **Placement:** Badges are anchored at the top-trailing edge of a navigation or tab icon container, overlapping the icon border.

---

### 3.14 Snackbars

Snackbars display brief, non-interruptive updates at the bottom of the screen.

- **Geometries:**
  - **Height:** Expands vertically from `48dp` (one line) to `64dp` (two lines) in compact windows.
  - **Placement:** Must float above FABs and bottom navigation, maintaining a consistent margin from window edges.
- **Colors:** Uses `--droid-sys-color-inverse-surface` backdrop with `--droid-sys-color-inverse-on-surface` text and `--droid-sys-color-inverse-primary` action links.
- **Compliance Rules:** Auto-dismissing snackbars (dismissing within `4--10` seconds) on the web are a visual compliance violation unless they are accompanied by immediate, accessible inline feedback (e.g. updating a button label from "Save" to "Saved").

---

## 4. Visual Assessor Bug Detection Guide

This guide is a structured, programmatic checklist for the visual assessor to evaluate UI screenshots and codebases. Use these visual heuristics to detect styling violations, structural bugs, and alignment errors.

### 4.1 Bug Heuristics Checklist

#### \[A\] Color \& Contrast Compliance

- [ ] **Contrast Violations:** Any text on a container must meet contrast ratios (e.g., text on `--droid-sys-color-primary` must be `--droid-sys-color-on-primary`, achieving at least `4.5:1` for body text and `3:1` for large text).
- [ ] **Hardcoded Values:** Check codebase stylesheets for hardcoded hex values (e.g., `#0b57d0` or `#ffffff`). These must be flagged as bugs and replaced with `--droid-sys-color-*` tokens to support theme switching.
- [ ] **Surface Elevation Bugs:** Nested or overlapping panels must have distinct color roles (e.g., a card on a background must transition from `--droid-sys-color-background` to `--droid-sys-color-surface`) to establish visual separation.

#### \[B\] Shape \& Rounding Geometry

- [ ] **Asymmetry Failures:** Multi-item groups (such as split buttons) must use square inner corners (`--droid-sys-shape-corner-none`) to visually join the elements, while keeping outer corners rounded. If inner corners are rounded, flag it as a geometry bug.
- [ ] **Radius Violations:** Confirm cards are utilizing `--droid-sys-shape-corner-medium` (`12px`) and dialogs are utilizing `--droid-sys-shape-corner-large` (`16px`). Deviation indicates non-compliance.
- [ ] **Nesting/Optical Roundness:** Ensure that concentric rounded shapes look balanced. If the inner component has the same corner radius as the outer wrapper, it violates the optical roundness principle.

#### \[C\] Interactive Target Areas

- [ ] **Target Size Violations:** Check that every actionable component (buttons, chips, sliders, checkbox targets) provides a minimum hit area of `48 x 48dp` (`44 x 44pt` on iOS). Small buttons that do not extend target padding beyond their visual bounds are critical accessibility bugs.
- [ ] **Target Overlap:** Ensure nested components (like close icons in input chips or slider handles) do not overlap adjacent interactive boundaries, preventing tap interference.

#### \[D\] Motion \& Easing Curves

- [ ] **Stiff Transitions:** Page entries and container expands must utilize the dynamic emphasized curves (`--droid-sys-motion-easing-emphasized-decelerate` or `--droid-sys-motion-easing-emphasized`). Using linear (`--droid-sys-motion-easing-linear`) or standard curves for spatial movement looks mechanical and must be flagged as a motion bug.
- [ ] **Bouncing Effects:** Ensure spatial movements overshoot and bounce slightly into place (using physical spring models) rather than stopping instantly.

#### \[E\] Bidirectionality \& Right-To-Left (RTL) Layouts

- [ ] **Mirroring Failures:** For RTL languages (Arabic, Hebrew, Urdu), ensure page layout, app bars, navigation components, and icons are mirrored horizontally.
- [ ] **Unmirrored Exceptions:** Clocks, circular progress indicators, and media player buttons (play, pause, fast-forward) must remain left-to-right (LTR) even in RTL modes. Mirroring these media controls is a layout bug.

---

### 4.2 Programmatic Error Matrix

|Visual Error Category|Visual Symptom (Screenshot/Mock)|Codebase Symptom (HTML/CSS)|Adherence Severity|Corrective Action|
|:---|:---|:---|:---:|:---|
|**Color Violation**|Text or icon lacks legibility against its container backdrop.|Incorrect mapping (e.g., using `--droid-sys-color-primary` text on a `--droid-sys-color-surface-variant` background).|**CRITICAL**|Remap the foreground style to the corresponding "on-" token (e.g., `--droid-sys-color-on-primary`).|
|**Hardcoded Color**|Visual colors appear correct but fail to adapt when switching between light and dark themes.|CSS declaration uses static hex values (`#0b57d0`, `#1f1f1f`) instead of variables.|**HIGH**|Replace hex values with `--droid-sys-color-*` tokens.|
|**Target Size Bug**|Micro-elements (e.g. checkbox icons, chip close buttons) are hard to select.|Visual container is `< 48px` without touch target extension padding.|**CRITICAL**|Inject target expansion padding to enforce `48 x 48dp` boundary.|
|**Shape Radii Defect**|Dialog corner feels boxy or inconsistent with nearby rounded cards.|Missing shape variable or wrong mapping (e.g., using `--droid-sys-shape-corner-extra-small` on a dialog).|**MEDIUM**|Remap the layout boundary to `--droid-sys-shape-corner-large` (`16px`).|
|**Asymmetry Failure**|Nested submenu list item borders overlap with rounded shapes.|Symmetrical corner tokens applied to grouped inner elements.|**MEDIUM**|Apply `--droid-sys-shape-corner-none` to inner adjacent borders.|
|**RTL Layout Defect**|Back arrow points left in RTL Arabic viewport, or sidebar appears on the wrong edge.|Absolute left/right CSS position parameters used instead of logical properties (start/end).|**HIGH**|Refactor layout positioning using logical flexbox parameters or `start` / `end` margin properties.|
|**Motion Failure**|Full-pane dialog expands with an instant, jarring jump cut.|Easing bound to linear transition, or animation is altogether missing.|**MEDIUM**|Bind the transition path to `--droid-sys-motion-easing-emphasized` curve with a `500ms` duration.|
|**Carousel Usability**|Vertical scroll screen has horizontal carousel but no alternative view option.|Missing programmatic / keyboard sequential "Show All" or "See More" trigger button.|**HIGH**|Inject a `Show All` text button underneath the carousel.|
|**Text Truncation**|Important headers or button label texts are cut off on compact viewports.|Text container has fixed dimensions without an accessible tooltip trigger.|**HIGH**|Enforce text wrapping or provide a touch-and-hold tooltip displaying the full string.|
