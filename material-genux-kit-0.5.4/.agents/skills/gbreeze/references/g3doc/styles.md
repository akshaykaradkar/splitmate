<!-- go/g3mark-in-g3doc -->

# gBreeze styles

go/gbreeze/styles

<!--*
# Document freshness: For more information, see go/fresh-source.
freshness: { owner: 'lizmitchell' reviewed: '2026-04-20' }
*-->

[TOC]

## Stylesheets

A design system stylesheet defines global `--md-*` custom properties for
Material's token subsystems.

gBreeze provides a default `m3.css` stylesheet file.

```css
@import '@material/web/labs/gb/styles/m3.css';
```

Custom design systems, like Google Material 3 (GM3), replace or extend from the
default stylesheet.

## Color

Color custom properties use the `light-dark()` function.

`color-scheme: light dark` is set by default to support user preferences, and
may be overridden to control light and dark mode.

| Custom property                             | Type      |
| :------------------------------------------ | :-------- |
| `--md-sys-color-primary`                    | `<color>` |
| `--md-sys-color-on-primary`                 | `<color>` |
| `--md-sys-color-primary-container`          | `<color>` |
| `--md-sys-color-on-primary-container`       | `<color>` |
| `--md-sys-color-primary-fixed`              | `<color>` |
| `--md-sys-color-primary-fixed-dim`          | `<color>` |
| `--md-sys-color-on-primary-fixed`           | `<color>` |
| `--md-sys-color-on-primary-fixed-variant`   | `<color>` |
| `--md-sys-color-secondary`                  | `<color>` |
| `--md-sys-color-on-secondary`               | `<color>` |
| `--md-sys-color-secondary-container`        | `<color>` |
| `--md-sys-color-on-secondary-container`     | `<color>` |
| `--md-sys-color-secondary-fixed`            | `<color>` |
| `--md-sys-color-secondary-fixed-dim`        | `<color>` |
| `--md-sys-color-on-secondary-fixed`         | `<color>` |
| `--md-sys-color-on-secondary-fixed-variant` | `<color>` |
| `--md-sys-color-tertiary`                   | `<color>` |
| `--md-sys-color-on-tertiary`                | `<color>` |
| `--md-sys-color-tertiary-container`         | `<color>` |
| `--md-sys-color-on-tertiary-container`      | `<color>` |
| `--md-sys-color-tertiary-fixed`             | `<color>` |
| `--md-sys-color-tertiary-fixed-dim`         | `<color>` |
| `--md-sys-color-on-tertiary-fixed`          | `<color>` |
| `--md-sys-color-on-tertiary-fixed-variant`  | `<color>` |
| `--md-sys-color-error`                      | `<color>` |
| `--md-sys-color-on-error`                   | `<color>` |
| `--md-sys-color-error-container`            | `<color>` |
| `--md-sys-color-on-error-container`         | `<color>` |
| `--md-sys-color-surface`                    | `<color>` |
| `--md-sys-color-surface-dim`                | `<color>` |
| `--md-sys-color-surface-bright`             | `<color>` |
| `--md-sys-color-surface-container-lowest`   | `<color>` |
| `--md-sys-color-surface-container-low`      | `<color>` |
| `--md-sys-color-surface-container`          | `<color>` |
| `--md-sys-color-surface-container-high`     | `<color>` |
| `--md-sys-color-surface-container-highest`  | `<color>` |
| `--md-sys-color-on-surface`                 | `<color>` |
| `--md-sys-color-on-surface-variant`         | `<color>` |
| `--md-sys-color-outline`                    | `<color>` |
| `--md-sys-color-outline-variant`            | `<color>` |
| `--md-sys-color-inverse-surface`            | `<color>` |
| `--md-sys-color-inverse-on-surface`         | `<color>` |
| `--md-sys-color-inverse-primary`            | `<color>` |
| `--md-sys-color-scrim`                      | `<color>` |
| `--md-sys-color-shadow`                     | `<color>` |

### Extended ref.palette tokens

Palette tokens are not included by default. To use `--md-ref-palette-*` tokens,
import the "full" token stylesheet version.

```css
@import '@material/web/labs/gb/styles/color/md-color-tokens-full.css';
```

These stylesheets are larger. Use them only when palette tokens are required.

| Custom property                 | Type      |
| :------------------------------ | :-------- |
| `--md-ref-palette-black`        | `<color>` |
| `--md-ref-palette-white`        | `<color>` |
| `--md-ref-palette-<name><tone>` | `<color>` |

Palette names and tones include, but are not limited to:

  * `<name>`: `primary`, `secondary`, `tertiary`, `error`, `neutral`,
    `neutral-variant`
  * `<tone>`: `0`, `10`, `20`, `30`, `40`, `50`, `60`, `70`, `80`, `90`, `95`,
    `98`, `100`

Additional names and tones may be provided based on the design system, such as
`--md-ref-palette-blue50` or `--md-ref-palette-neutral6`.

## Elevation

| Custom property               | Type            |
| :---------------------------- | :-------------- |
| `--md-sys-elevation-shadow-0` | `box-shadow: *` |
| `--md-sys-elevation-shadow-1` | `box-shadow: *` |
| `--md-sys-elevation-shadow-2` | `box-shadow: *` |
| `--md-sys-elevation-shadow-3` | `box-shadow: *` |
| `--md-sys-elevation-shadow-4` | `box-shadow: *` |
| `--md-sys-elevation-shadow-5` | `box-shadow: *` |

## Icons

| Custom property  | Type                  |
| :--------------- | :-------------------- |
| `--md-icon-size` | `<length-percentage>` |
| `--md-icon-font` | `font-family: *`      |
| `--md-icon-opsz` | `<number>`            |
| `--md-icon-wght` | `<number>`            |
| `--md-icon-fill` | `<number>`            |
| `--md-icon-grad` | `<number>`            |

Use the `<md-icon>` custom element or `.md-icon` utility class to style font and
SVG icons.

### Elements

Use `<md-icon>` when using other custom elements, or when using icons inside
shadow roots.

```html
<script type="module">
  import '@material/web/labs/gb/styles/icon/md-icon.js';
</script>
<md-icon>google</md-icon>
<md-icon>favorite</md-icon>
<md-icon>material_design</md-icon>

<md-icon><svg>...</svg></md-icon>
```

### Classes

Use the `.md-icon` utility class for light DOM icons and when custom elements
cannot be used.

Set `aria-hidden="true"` on elements when using the utility class.

```html
<span class="md-icon" aria-hidden="true">google</span>
<span class="md-icon" aria-hidden="true">favorite</span>
<span class="md-icon" aria-hidden="true">material_design</span>

<svg class="md-icon" aria-hidden="true">...</svg>
```

> IMPORTANT: Do not use `.material-symbols`, `.material-symbols-outlined`, or
> other similar built-in icon utility classes.

### Font icon family

`--md-icon-font` must be defined when using icon fonts that are not the default
`'Material Symbols'` font.

```css
@import url("https://fonts.googleapis.com/css2?family=Google+Symbols");

:root {
  --md-icon-font: 'Google Symbols';
}
```

## Motion

| Custom property                                | Type                |
| :--------------------------------------------- | :------------------ |
| `--md-sys-motion-easing-emphasized`            | `<easing-function>` |
| `--md-sys-motion-easing-emphasized-accelerate` | `<easing-function>` |
| `--md-sys-motion-easing-emphasized-decelerate` | `<easing-function>` |
| `--md-sys-motion-easing-standard`              | `<easing-function>` |
| `--md-sys-motion-easing-standard-accelerate`   | `<easing-function>` |
| `--md-sys-motion-easing-standard-decelerate`   | `<easing-function>` |
| `--md-sys-motion-duration-short1`              | `<time>`            |
| `--md-sys-motion-duration-short2`              | `<time>`            |
| `--md-sys-motion-duration-short3`              | `<time>`            |
| `--md-sys-motion-duration-short4`              | `<time>`            |
| `--md-sys-motion-duration-medium1`             | `<time>`            |
| `--md-sys-motion-duration-medium2`             | `<time>`            |
| `--md-sys-motion-duration-medium3`             | `<time>`            |
| `--md-sys-motion-duration-medium4`             | `<time>`            |
| `--md-sys-motion-duration-long1`               | `<time>`            |
| `--md-sys-motion-duration-long2`               | `<time>`            |
| `--md-sys-motion-duration-long3`               | `<time>`            |
| `--md-sys-motion-duration-long4`               | `<time>`            |
| `--md-sys-motion-duration-extra-long1`         | `<time>`            |
| `--md-sys-motion-duration-extra-long2`         | `<time>`            |
| `--md-sys-motion-duration-extra-long3`         | `<time>`            |
| `--md-sys-motion-duration-extra-long4`         | `<time>`            |

> NOTE: The spring physics motion system is not yet supported.

## Shape

| Custom property                      | Type                  |
| :----------------------------------- | :-------------------- |
| `--md-sys-shape-corner-none`         | `<length-percentage>` |
| `--md-sys-shape-corner-xs`           | `<length-percentage>` |
| `--md-sys-shape-corner-sm`           | `<length-percentage>` |
| `--md-sys-shape-corner-md`           | `<length-percentage>` |
| `--md-sys-shape-corner-lg`           | `<length-percentage>` |
| `--md-sys-shape-corner-lg-increased` | `<length-percentage>` |
| `--md-sys-shape-corner-xl`           | `<length-percentage>` |
| `--md-sys-shape-corner-xl-increased` | `<length-percentage>` |
| `--md-sys-shape-corner-xxl`          | `<length-percentage>` |
| `--md-sys-shape-corner-full`         | `<length-percentage>` |

## Typography

Typescale custom properties use the CSS `font` shorthand syntax.

| Custom property                                     | Type                         |
| :-------------------------------------------------- | :--------------------------- |
| `--md-ref-typeface-brand`                           | `font-family: *`             |
| `--md-ref-typeface-plain`                           | `font-family: *`             |
| `--md-ref-typeface-weight-regular`                  | `font-weight: *`             |
| `--md-ref-typeface-weight-medium`                   | `font-weight: *`             |
| `--md-ref-typeface-weight-bold`                     | `font-weight: *`             |
| `--md-sys-typescale-<scale>-<size>`                 | `font: *`                    |
| `--md-sys-typescale-<scale>-<size>-tracking`        | `letter-spacing: *`          |
| `--md-sys-typescale-<scale>-<size>-axes`            | `font-variation-settings: *` |
| `--md-sys-typescale-emphasized-<scale>-<size>`      | `font: *`                    |
| `--md-sys-typescale-emphasized-<scale>-<size>-axes` | `font-variation-settings: *` |

The default provided scales and sizes include:

  * `<scale>`: `body`, `label`, `title`, `headline`, `display`
  * `<size>`: `sm`, `md`, `lg`

## Spacing

The spacing system is measured on an `8px` unit scale, where `space-100 = 8px`.

| Custom property          | Type                  |
| :----------------------- | :-------------------- |
| `--md-sys-space-unit`    | `<length-percentage>` |
| `--md-sys-space-<scale>` | `<length-percentage>` |

The default provided unit scales include:

  * `<scale>`: `0`, `25`, `50`, `75`, `100`, `125`, `150`, `175`, `200`, `250`,
    `300`, `400`, `450`, `500`, `600`, `700`, `800`, `900`

Additional scales may be defined as a multiplier of the baseline unit.

```css
:root {
  /* Custom unit scales */
  --md-sys-space-550: calc(var(--md-sys-space-unit) * 5.5);
}
```
