# gBreeze with Tailwind CSS

## Import

Import Tailwind first, the Material for Tailwind theme second, and then a design
system stylesheet third.

```css
/* 1. Import Tailwind. */
@import 'tailwindcss';
/* 2. Import Material for Tailwind theme. */
@import '@material/web/labs/gb/styles/tailwind.css';
/* 3. Import design system stylesheet. */
@import '@material/web/labs/gb/styles/m3.css';
```

## Usage

### Color

Use Material color token names for color utilities.

```html
<main class="bg-surface text-on-surface">
  Surface / on-surface

  <div class="bg-primary text-on-primary w-[200px] h-[200px] p-2">
    Primary / on-primary
  </div>
</main>
```

### Typography

Use the custom utility `.typescale-<name>-<size>` to apply typography tokens.

```html
<h1 class="typescale-display-md">Display medium</h1>

<p class="typescale-body-lg">Large body with <b class="font-weight-bold">bold</b> text.</p>
```

### Shape

Supports all Tailwind sizes from `--radius-xs` to `--radius-4xl`.

```html
<div class="rounded-md border border-outline-variant p-1">
  Rounded with outline
</div>
```

### Elevation

Use Tailwind sizes from `--shadow-xs` to `--shadow-xl` for Material elevation
levels 1 to 5. Do not use `--shadow-2xs` or `--shadow-2xl`.

```html
<div class="shadow-sm">Level 2 shadow</div>
```

### Spacing

Use `.p-s*`, `.m-s*`, and `.gap-*` utility classes for Material's spacing
system.

```html
<div class="p-s100">8dp padding</div>
<div class="ps-s50">4dp start padding</div>
<div class="m-s200">16dp margin</div>
<div class="flex gap-s100">...</div>
```

### Breakpoints

Use `sm:` to `xl:` for Material window size breakpoints.

Material window size | Tailwind breakpoint
-------------------- | -------------------
Compact              | (Default)
Medium               | `sm:`
Expanded             | `md:`
Large                | `lg:`
Extra large          | `xl:`