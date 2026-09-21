---
version: 'alpha'
name: 'Android Pixel Design System'
description: ''
colors:
  black: '#000000'
  brand-a:
    light: '#0058bc'
    dark: '#85adff'
  brand-b:
    light: '#92a6ff'
    dark: '#fbf8ff'
  brand-c:
    light: '#0072f1'
    dark: '#4c8eff'
  brand-d:
    light: '#c472cd'
    dark: '#fbb4ff'
  clock-hour:
    light: '#0058bc'
    dark: '#85adff'
  clock-minute:
    light: '#2680ff'
    dark: '#0075f5'
  clock-second:
    light: '#9749a1'
    dark: '#c774cf'
  error:
    light: '#b31b25'
    dark: '#ff716c'
  error-container:
    light: '#fb5151'
    dark: '#9f0519'
  inverse-on-surface:
    light: '#929bc6'
    dark: '#4a537a'
  inverse-primary:
    light: '#4c8eff'
    dark: '#005bc2'
  inverse-surface:
    light: '#020a2f'
    dark: '#fbf8ff'
  on-error:
    light: '#ffefee'
    dark: '#490006'
  on-error-container:
    light: '#570008'
    dark: '#ffa8a3'
  on-primary:
    light: '#f0f2ff'
    dark: '#002c65'
  on-primary-container: '#00214f'
  on-primary-fixed: '#000000'
  on-primary-fixed-variant: '#002a61'
  on-secondary:
    light: '#f2f1ff'
    dark: '#00185e'
  on-secondary-container:
    light: '#213ea2'
    dark: '#c5ceff'
  on-secondary-fixed: '#00288d'
  on-secondary-fixed-variant: '#2c48ac'
  on-surface:
    light: '#232c51'
    dark: '#e1e4ff'
  on-surface-variant:
    light: '#515981'
    dark: '#a0a9d5'
  on-tertiary:
    light: '#ffeefb'
    dark: '#6d2179'
  on-tertiary-container: '#62156e'
  on-tertiary-fixed: '#450051'
  on-tertiary-fixed-variant: '#6c2178'
  on-theme-app:
    light: '#001231'
    dark: '#000000'
  outline:
    light: '#6c759e'
    dark: '#6b739d'
  outline-variant:
    light: '#a2abd7'
    dark: '#3d466c'
  overview-background:
    light: '#9cabd1'
    dark: '#384667'
  primary:
    light: '#0058bc'
    dark: '#85adff'
  primary-container: '#6d9fff'
  primary-fixed: '#6d9fff'
  primary-fixed-dim: '#5291ff'
  ref-p90: '#d8e2ff' # Non-GM3
  scontainer-high:
    light: '#dde1ff'
    dark: '#0e1b4c'
  scontainer-highest:
    light: '#d5dbff'
    dark: '#132156'
  scontainer-low:
    light: '#efefff'
    dark: '#040f38'
  scontainer-lowest:
    light: '#ffffff'
    dark: '#000000'
  secondary:
    light: '#3953b7'
    dark: '#7f98ff'
  secondary-container:
    light: '#c7cfff'
    dark: '#203da1'
  secondary-fixed: '#c7cfff'
  secondary-fixed-dim: '#b4c1ff'
  surface:
    light: '#f7f5ff'
    dark: '#020a2f'
  surface-bright:
    light: '#f7f5ff'
    dark: '#182760'
  surface-container:
    light: '#e4e7ff'
    dark: '#091542'
  surface-dim:
    light: '#cad2ff'
    dark: '#020a2f'
  tertiary:
    light: '#883c93'
    dark: '#fbb4ff'
  tertiary-container: '#f79ffe'
  tertiary-fixed: '#f79ffe'
  tertiary-fixed-dim: '#e891ef'
  theme-app:
    light: '#6d9fff'
    dark: '#017aff'
  theme-app-ring:
    light: '#006de7'
    dark: '#00377a'
  theme-notif: '#faabff'
  white: '#ffffff' # Non-GM3
  widget-background:
    light: '#e4e7ff'
    dark: '#182760'
shapes:
typography:
measurements:
---

# Android Pixel Design System

## Overview

The **Android Pixel Design System (APDS)** (also referred to as **AuxDS** or **APUX DS**) defines the premium, highly personalized, and expressive visual identity of Google's flagship Pixel experience. APDS is built on a tripartite model:

1. **System UI:** The foundational layer that governs core features, including notifications, quick settings, the lock screen, the status bar, and customization.
2. **Pixel-Only Features \& Apps:** Specialized applications and capabilities crafted exclusively for Pixel hardware (e.g., Gboard, Launcher, Universal Media Object).
3. **Material Design (GM3) Alignment:** The overarching visual framework that guarantees cohesive visual rhythm, modern aesthetic consistency, and platform-level cross-compatibility.

The design philosophy of APDS centers on **empowered self-expression, adaptive layouts, and dynamic fluidity**. APDS does not force a single rigid theme onto the user; instead, it dynamically adapts to the user's wallpaper, device color, or explicit in-app content. By utilizing **Google Sans Flex (GSF)** and the **HCT (Hue, Chroma, Tone)** color space, APDS ensures that this elaborate personalization is mathematically paired with strict readability and contrast requirements, giving users an interface that is beautiful, uniquely theirs, and highly accessible.

---

## Colors

Color in APDS is much more than aesthetics—it is a functional utility. APDS uses the **HCT (Hue, Chroma, Tone)** model, which decouples color intensity (chroma) and color name (hue) from visual weight/brightness (tone). Contrast is calculated entirely on Tone (on a scale from 0 to 100, where 0 is pure black and 100 is pure white), allowing APDS to guarantee accessibility compliance programmatically across millions of dynamic color palettes.

### Dynamic Color Palette Generation

Whenever a user selects a wallpaper or views in-app content, a **seed color** is extracted via a quantization algorithm. This seed color is used to generate five key tonal palettes:

- **Primary:** Dynamic accent colors used for high-emphasis elements, active states, and core branding.
- **Secondary:** RECESSIVE accent colors used for auxiliary elements like filter chips or utility headers.
- **Tertiary:** COMPLEMENTARY contrast accents used for broad expression and input elements.
- **Neutral:** Sourced for background surfaces, panels, and large-scale containers.
- **Neutral Variant:** Sourced for outline variants, subtle borders, and secondary text roles.

### System Tokens vs. Reference Tokens

APDS abstracts color usage by separating system behavior from raw color values. Designers and developers should **only use System Tokens** in code and layout designs.

- **System Tokens** are named based on their semantic purpose (e.g., `colors.primary`, `colors.surface`). Their underlying values point to reference tokens that dynamically swap depending on the theme (e.g., Light vs. Dark theme).
- **On-Colors** (prefixed with `on-`, such as `colors.on-primary` or `colors.on-surface`) are paired systematically with their background colors to guarantee a minimum contrast ratio of **3:1 for graphical elements** and **4.5:1 for normal text**.
- **Containers** (such as `colors.primary-container` or `colors.secondary-container`) provide fill backgrounds for foreground components. They are never to be used for text or icons directly.

### Paired Accent \& Surface Tokens

The APDS token schema includes:

- **Primary Set:** `colors.primary`, `colors.on-primary`, `colors.primary-container`, `colors.on-primary-container`.
- **Secondary Set:** `colors.secondary`, `colors.on-secondary`, `colors.secondary-container`, `colors.on-secondary-container`.
- **Tertiary Set:** `colors.tertiary`, `colors.on-tertiary`, `colors.tertiary-container`, `colors.on-tertiary-container`.
- **Surface \& Containers:**
  - `colors.surface` (default background color).
  - `colors.surface-bright` and `colors.surface-dim` (maintain relative brightness across light/dark modes).
  - **Surface Container Scale:** Ordered from lowest to highest emphasis: `colors.scontainer-lowest`, `colors.scontainer-low`, `colors.surface-container` (default), `colors.scontainer-high`, `colors.scontainer-highest`.
- **Inverse Colors:** `colors.inverse-surface`, `colors.inverse-on-surface`, and `colors.inverse-primary` are selectively applied to provide sharp visual contrasts (such as in-app toasts).
- **Outlines:** `colors.outline` (high-contrast boundaries, e.g., input borders) and `colors.outline-variant` (decorative elements, e.g., dividers).

### Fixed Accent Tokens

For components that must maintain the same visual tone regardless of the system theme (Light or Dark), APDS provides "Fixed" tokens:

- `colors.primary-fixed`, `colors.secondary-fixed`, `colors.tertiary-fixed`.
- `colors.primary-fixed-dim`, `colors.secondary-fixed-dim`, `colors.tertiary-fixed-dim` (for deeper, more prioritized fixed states).
- `colors.on-primary-fixed`, `colors.on-primary-fixed-variant`.

### Android System UI Specific Color Tokens

Bespoke tokens are provided to secure brand integrity and contrast on key system interfaces:

- **Brand Tokens (formerly Super G):** Sourced as `colors.brand-a`, `colors.brand-b`, `colors.brand-c`, and `colors.brand-d`. These map Google’s corporate identity colors for the Quick Search Bar (QSB), boot-up animations, and Google Lens icons.
- **Widgets Background:** `colors.widget-background` (Light: `#e4e7ff`, Dark: `#182760`).
- **Themed App Icons:** `colors.theme-app` (Light: `#6d9fff`, Dark: `#017aff`), `colors.on-theme-app` (Light: `#001231`, Dark: `#000000`), and `colors.theme-app-ring` (Light: `#006de7`, Dark: `#00377a`).
- **Overview Background:** `colors.overview-background` (Light: `#9cabd1`, Dark: `#384667`).
- **System Scrim:** Bound globally to `#000000` with **32% opacity**. Dynamic colored scrims are fully deprecated.

### Semantic Colors (Safety, Security, \& Privacy)

Semantic colors represent alert levels and are **immune to dynamic color shifts** to protect their cognitive meaning:

- **Red Alert / Critical Error:** mapped to `colors.error`, `colors.on-error`, `colors.error-container`, and `colors.on-error-container`.
- **Yellow / Caution Alert:** intentionally repeats visual values between medium and high container states for safety compliance.
- **Green / Safe State:** utilized by security panels to represent secure conditions.

---

## Typography

APDS utilizes **Google Sans Flex (GSF)** as the system-wide typographic standard. GSF is a robust variable font that unlocks fine-tuned customization along six distinct axes: **Roundness, Weight, Width, Grade, Optical Size, and Slant**.

### Font Themes \& Typographic Roles

The type scale is split into **15 Baseline styles** and **15 Emphasized styles**, grouped under five functional roles:

1. **Display (Large, Medium, Small):** The largest text on screen, reserved for high-impact numerals or editorial copy. Typically works best on expanded display orientations.
2. **Headline (Large, Medium, Small):** Short, high-emphasis text used to demarcate primary layout headings or main screen regions.
3. **Title (Large, Medium, Small):** Medium-emphasis text used for secondary passages, subheaders, or list group titles.
4. **Body (Large, Medium, Small):** Optimized using **Google Sans Text** for long-form legibility and reading comfort.
5. **Label (Large, Medium, Small):** Highly utilitarian, small-scale text used for captions, metadata, or the text inside buttons and chips.

### Baseline vs. Emphasized Styles

- **Baseline Styles:** The default static style for standard reading and secondary elements.
- **Emphasized Styles:** Leverage increased font weight and roundness to add expression, highlight active/selected states, draw focus to key actions, or indicate unread items (e.g., in notification badging).

### Numeric and Accessibility Rules

- **Monospaced Tabular Numbers:** Clocks, timer widgets, data tables, and dynamic counters must use tabular/monospaced digits to prevent optical shifting.
- **Conversion Ratios:** The web calculates units relative to rem (16px base): `Conversion = SP_SIZE / 16`.
- **Letter Spacing Conversion:** Android uses `em` (`tracking in px / font size in sp`).
- **Contrast \& Formatting:** Text must achieve a minimum 4.5:1 contrast against its background (`colors.on-surface` or `colors.on-surface-variant`). Hyperlinked inline text must be underlined and colored with `colors.primary` or `colors.tertiary`.

---

## Layout \& Spacing

To guarantee a clean responsive rhythm on anything from a compact phone to a large foldable screen, APDS organizes layouts based on **Window Size Classes** and **Structural Regions**.

### Window Size Classes

Breakpoints are determined by the available horizontal window width (dp):

- **Compact (\<600dp):** Mobile phones in portrait mode. Uses single-pane views. Navigation is anchored to the bottom using a Bottom Navigation Bar.
- **Medium (600–839dp):** Foldables in portrait or small tablets. Single-pane is recommended; dual-pane can be used for low-density content. Uses a Collapsed Navigation Rail.
- **Expanded (840–1199dp):** Tablets or foldables in landscape. Two-pane layout is standard. Uses a standard Expanded Navigation Rail or Navigation Drawer.
- **Large (1200–1599dp):** Desktop screens. Two-pane standard.
- **Extra-Large (1600dp+):** Large monitors or ultra-wides. Supports 1 to 3 panes of content.

### Structural Regions

Every APDS window is divided into three primary regions:

1. **Navigation Region:** Holds the primary navigation components (bar, rail, or drawer). Always placed close to the window edges (left for LTR, right for RTL).
2. **Body Region:** Houses the main scrollable content.
3. **Panes:** Architectural containers of content. Single-pane layouts use one flexible pane. Two-pane split layouts utilize a 50/50 split separated by a **24dp spacer**.

### Foldables \& Multi-Window Mode

- **Fold Alignment:** When a physical hinge or flexible fold splits the display, UI components and text must never overlay the crease.
- **Multi-Window Mode:** Enables side-by-side or partitioned app viewing. As an app window is resized (e.g., from 50/50 to 1:3), it must seamlessly transition down to compact styling formats.

---

## Elevation \& Depth

APDS establishes depth and vertical hierarchy through **Tonal Layering** and **Elevation Borders** rather than traditional drop shadows.

### Tonal Layering

Content containers are differentiated by shifting background container shades (using the `colors.surface-container` scale). For example, a primary background sits on `colors.surface`, while elevated secondary content card components sit on top of a lighter, higher-contrast `colors.scontainer-high` container.

### Elevation Borders

Physical hierarchy and elevation states are reinforced through structured borders:

- `measurements.border-none` (0dp)
- `measurements.border-small` (1dp): Used for disabled or low-priority flat elements.
- `measurements.border-medium` (2dp): Default container state.
- `measurements.border-large` (3dp): Used to denote focus rings and prioritized elements.
- `measurements.border-extraLarge` (6dp): Highly prominent boundaries, such as suggestion banners.

### Opacities (Alpha Levels)

- `measurements.alpha-8` (8%): Unselected/disabled state overlay layer.
- `measurements.alpha-11` (11%): Default hover state overlay layer.
- `measurements.alpha-12` (12%): Disabled state container fills.
- `measurements.alpha-15` (15%): Pressed or focused state overlay layer.
- `measurements.alpha-32` (32%): Default scrim opacity.
- `measurements.alpha-38` (38%): Disabled text/icon opacity.
- `measurements.alpha-60` (60%): Secondary supporting details.
- `measurements.alpha-full` (100%): Solid fills.

---

## Shapes

APDS features a highly refined, mathematically graded corner radius scale. Elements range from architectural crispness to organic circularity based on their container type, sizing, and position in the hierarchy.

### Graded Radii Scale

- `shapes.radius-none` (0px)
- `shapes.radius-extraSmall1` (2px): Inner corner radius of sliding volume bars.
- `shapes.radius-extraSmall2` (4px): Inner segments of grouped toggle buttons or wallet/payment components.
- `shapes.radius-small` (8px): Interactive chip containers (Input, Assistive, Filter, Suggestion).
- `shapes.radius-medium` (12px): Tabs, AOB (Onboarding) tiles, and slider outer tracks.
- `shapes.radius-large1` (16px): Standard content carousels and floating action buttons (FABs).
- `shapes.radius-large2` (20px): System notification cards, avatars, and list container outer bounds.
- `shapes.radius-large3` (24px): Large cards, system dialogs.
- `shapes.radius-extraLarge1` (28px): System folders, bottom sheets, toolbars, and Heads-Up Notifications (HUN).
- `shapes.radius-extraLarge2` (32px): Floating cards and expanded system sheets.
- `shapes.radius-extraLarge3` (42px): Large image carousels, tablet panels, and edge-to-edge floating containers.
- `shapes.radius-full` (360px): Fully rounded pill or circular containers (e.g., standard action buttons, translation/active chips, app icons).

---

## Components

APDS enforces exact structural padding, touch targets, and color pairings across its central interactive components.

### Buttons

APDS provides four primary button variants: **Filled**, **Filled Tonal**, **Outlined**, and **Text**.

- **Anatomy:** Includes an optional leading icon, label text, and a fully rounded container (`shapes.radius-full`). Label text is always set in sentence case.
- **Measurements:**
  - Container Height: 40dp.
  - Touch Target: Must be at least 48dp (`measurements.size-medium4`).
  - Top/Bottom Padding: 10dp (`measurements.space-extraSmall5`).
  - Left/Right Padding: 16dp (`measurements.space-small1`).
  - Gap (Icon-to-Label): 8dp (`measurements.space-extraSmall4`).
- **Button Groups:**
  - Standard gap between adjacent buttons: 8dp.
  - Left/Right group padding relative to the parent frame: 24dp.
- **Color Mappings:**
  - **Filled:** Container is `colors.primary`, label and icon are `colors.on-primary`.
  - **Outlined:** Border is `colors.outline-variant` (1dp), label and icon are `colors.primary`.
  - **Text:** No container fill. Label and icon are `colors.primary`.

### Chips

Chips represent context-aware actions, filters, or inputs. APDS maps standard GM3 chips alongside three unique Android-only classes:

1. **Input Chip:** Discrete user inputs. (Container: `shapes.radius-small`, Height: 32dp. Left padding: 4dp, Gap: 8dp, Right padding: 8dp. Icon: 24dp).
2. **Assistive Chip:** Automated smart actions. (Container: `shapes.radius-small`, Height: 32dp. Left padding: 8dp, Gap: 8dp, Right padding: 16dp. Icon: 20dp).
3. **Filter Chip:** Option tags. (Container: `shapes.radius-small`, Height: 32dp. Left padding: 8dp, Gap: 8dp, Right padding: 16dp. Icon: 20dp).
4. **Suggestion Chip:** Dynamic responses. (Container: `shapes.radius-small`, Height: 32dp. Left/Right padding: 16dp, Top/Bottom padding: 6dp).
5. **Profile Chip (Android-Only):** Represents a user or targeted device. (Container: `shapes.radius-full`, Height: 24dp. Left padding: 4dp, Gap: 4dp, Right padding: 8dp. Icon: 24dp).
6. **Translation Chip (Android-Only):** Allows real-time display translation. Uses Active chip styling. (Container: `shapes.radius-full`, Height: 40dp. Left/Right padding: 16dp).
7. **Navigation Chip (Android-Only):** Triggers instant routing. (Container: `shapes.radius-full`, Height: 32dp. Left padding: 8dp, Right/Top/Bottom padding: 4dp. Trailing Icon: 24dp).

### Lists

Lists are highly scannable, vertically ordered elements used for browsing and actions.

- **Sizes:** One-line, two-line, or three-line layouts.
- **Alignment:** Center-aligned if text is under three lines; top-aligned if text is three lines or more.
- **Measurements (No Surface Container):**
  - Top/Bottom Padding: 12dp.
  - Left/Right Padding: 24dp.
  - Minimum Height (Text-only): 56dp.
  - Minimum Height (with Icon): 64dp.
- **Measurements (With Filled Surface Container):**
  - Top/Bottom Padding: 12dp.
  - Left/Right External Padding: 16dp.
  - Left/Right Internal Padding: 16dp.
- **Grouped Lists:**
  - Padding between standard list items: 0dp.
  - Space between separate list groups without a subheader: 16dp.
  - Space between groups with a subheader: 0dp.
- **Subheaders:** Height: 48dp, Left/Right padding: 24dp, Top padding: 20dp, Bottom padding: 8dp.

### Checkboxes

- **Anatomy:** Comprises a container fill, a container outline border, and a checkmark vector graphic.
- **Measurements:** Touch target: 48dp, Container size: 18dp. State layer (for hover/press): 40dp.
- **Color Mappings:**
  - **Enabled/Selected:** Container is `colors.primary`, icon is `colors.on-primary`.
  - **Unselected:** Container fill is transparent, container outline is `colors.on-surface-variant`.
  - **Disabled:** State layer is `colors.on-surface` with 38% opacity (`measurements.alpha-38`).

### Radio Buttons

- **Anatomy:** Selected center-dot state or unselected circle vector graphic.
- **Measurements:** Touch target: 48dp, Icon size: 24dp. State layer: 40dp.
- **Color Mappings:**
  - **Selected:** Inner dot and outer ring are `colors.primary`. Hover overlay uses `colors.primary` at 8% opacity.
  - **Unselected:** Outer ring is `colors.on-surface-variant`. Hover overlay uses `colors.on-surface` at 8% opacity.
  - **Disabled:** Icon is `colors.on-surface` at 38% opacity (`measurements.alpha-38`).

### Switches

Switches are binary, instant toggles used exclusively for settings and feature controls.

- **Anatomy:** Comprises a pill-shaped track, a sliding handle, an optional icon nested inside the handle, a track outline border, and offset padding.
- **Measurements:**
  - Touch Target: 48dp.
  - Track Dimensions: 52dp (Width) x 32dp (Height).
  - Handle Size: 24dp. (Grows to 28dp in Pressed state).
  - Nested Icon Size: 16dp.
  - Track Outline Thickness: 3dp.
  - Handle Offset Padding: 4dp.
- **Color Mappings (Checked / Enabled):**
  - Track fill is `colors.primary`, handle is `colors.on-primary`, icon is `colors.on-primary-container`.
- **Color Mappings (Unchecked / Enabled):**
  - Track fill is `colors.scontainer-highest`, track outline is `colors.outline`, handle is `colors.outline`, icon is `colors.scontainer-high` (representing empty container depth).

### Sliders

Sliders let users select values across a broad continuous range.

- **Sizes:** APDS scales sliders into several distinct sizes: **X-Thin, Slim, Thin, X-Small, Small, Medium, Large, and X-Large**.
- **Structural Variants:** APDS features horizontal range sliders, interval step-sliders (5-step or 7-step with single/double icons), and vertical volume sliders. It does not support standard GM3 ranged or centered sliders.
- **Measurements (XT - X-Thin Horizontal):**
  - Track Height: 12dp.
  - Handle size: 24dp x 4dp. Corner radius: 4dp.
  - Accessible indicator dot size: 4dp (within a 12dp container).
  - Track Color: Filled track is `colors.primary`, empty track is `colors.scontainer-high` (shortened as `colors.scontainer-high`).
- **Measurements (Slim Horizontal):**
  - Track Height: 16dp.
  - Handle size: 28dp x 4dp.
- **Vertical Volume Slider (Default Android State):**
  - Displays as a vertical pill with the volume icon nested inside the filled lower section.
  - **Expanded Vertical Drawer:** Triggering the physical volume buttons slides out a secondary volume slider panel adjacent to the main vertical volume controller, sharing a flush visual boundary.

---

## Do's and Don'ts

### Do's

- **Do** align all primary user actions, active navigation states, and selected list markers to `colors.primary` to establish clear focal priority.
- **Do** utilize **Google Sans Flex (GSF)** for all modern system interfaces to reduce font-loading latency and enable precise axis optimization.
- **Do** top-align list items when their accompanying secondary/supporting copy exceeds three lines to maximize reading scanning speeds.
- **Do** maintain a strict **48dp touch target** for all minor interactive elements, including checkboxes, radio buttons, and switches, to ensure ergonomic ease.
- **Do** use monospaced, tabular numbers on all time-sensitive layouts, clocks, and numeric columns to prevent layout shifting during updates.
- **Do** ensure that semantic alert colors (red/yellow/green) remain static and are never overridden by the user's dynamic wallpaper theme.

### Don'ts

- **Don't** use container tokens (e.g., `colors.primary-container`) as text colors; they are reserved strictly for component fill backgrounds.
- **Don't** overlay layout controls, interactive buttons, or primary typography over the hinge or crease line on foldable tabletop screens.
- **Don't** apply dynamic custom colors to the system scrim. Scrim must remain an absolute `#000000` with **32% opacity**.
- **Don't** mix inconsistent corner radii in the same view. Align nested elements hierarchically (e.g., a card using `shapes.radius-large3` should hold chips using `shapes.radius-small`).
- **Don't** apply Emphasized typography styles universally across a screen; excessive emphasis degrades readability and ruins visual hierarchy.
- **Don't** swap structurally non-equivalent components (e.g., do not swap standard buttons for filter chips, or lists for cards without a distinct functional purpose).
