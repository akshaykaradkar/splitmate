<!-- go/g3mark-in-g3doc -->

# Quickstart: Set up gBreeze

go/gbreeze/quickstart

<!--*
# Document freshness: For more information, see go/fresh-source.
freshness: { owner: 'lizmitchell' reviewed: '2026-04-20' }
tag: 'docType:quickstart'
*-->

Learn how to install and set up gBreeze styles and components for a Material
Design web application.

[TOC]

## Before you begin

Install gBreeze from `@nightly` releases of the `@material/web` NPM package.

<section class="tabs" markdown="1">

<h3 class="new-tab">Using CDN</h3>

Use an import map to install from a CDN (esm.sh is recommended) for quick,
build-less prototypes.

```html
<script type="importmap">
{
  "imports": {
    "@material/web/": "https://esm.sh/@material/web@nightly/"
  }
}
</script>
```

> TIP: To avoid breaking changes, pin an exact nightly version number.
>
> Run `npm info @material/web dist-tags.nightly` to get the latest exact version
> number.

<h3 class="new-tab">Using npm</h3>

Install using `npm` for projects with a build system, such as Vite or rollup.

```sh
npm install @material/web@nightly --save-exact
```

## Import styles

A design system stylesheet defines global `--md-*` custom properties for
Material's token subsystems, such as color, typescale, and shape.

1.  Import a design system stylesheet. gBreeze provides a default stylesheet for
    Material 3 (M3).

    ```html
    <!-- Import default M3 system stylesheet. -->
    <link rel="stylesheet" href="https://esm.sh/@material/web@nightly/labs/gb/styles/m3.css">
    ```

2.  Import the font families for typescales and icons used by the system
    stylesheet.

    The default `m3.css` stylesheet file uses Roboto and Material Symbols.

    ```html
    <!-- Import M3 typescale and icon fonts. -->
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Roboto+Flex:opsz,slnt,wdth,wght,GRAD@8..144,-10..0,25..151,100..1000,-200..150&display=swap">
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200">
    ```

3.  Add custom CSS and run the application to verify a design system stylesheet
    is loaded.

    ```html
    <!-- Using styles. -->
    <div class="custom-alert">Using tokens</div>
    <style>
      .custom-alert {
        display: flex;
        padding: 16px;
        border-radius: var(--md-sys-shape-corner-lg);
        background: var(--md-sys-color-error-container);
        color: var(--md-sys-color-on-error-container);
        font: var(--md-sys-typescale-body-md);
        letter-spacing: var(--md-sys-typescale-body-md-tracking);
      }
    </style>
    ```

### GM3

Custom design systems, like Google Material 3, provide their own stylesheets and
may use different fonts.

GM3 stylesheets are published on SCS instead of NPM.

```html
<!-- Import GM3 system stylesheet. -->
<link rel="stylesheet" href="https://static.corp.google.com/material-web/gbreeze/latest/styles/gm3.css">
<!-- Import GM3 typescale and icon fonts. -->
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,slnt,wdth,wght,GRAD,ROND@6..144,-10..0,25..151,1..1000,0..100,0..100&display=swap">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Google+Symbols:opsz,wght,FILL,GRAD,ROND@20..48,100..700,0..1,-50..200,0..100">
<style>
  :root {
    --md-icon-font: 'Google Symbols';
  }
</style>
```

> TIP: The default `--md-icon-font` value is `'Material Symbols'`, and needs to
> be changed for different icon fonts, such as `'Google Symbols'`.

## Import components

Import component custom elements to register their tag name for use in HTML.

1.  Import custom elements tag names.

    ```js
    import '@material/web/labs/gb/components/button/md-button.js';
    import '@material/web/labs/gb/components/checkbox/md-checkbox.js';
    ```

2.  Run the application and use custom elements in HTML to verify the components
    are loaded.

    ```html
    <md-button color="filled">Hello Material</md-button>
    <md-checkbox checked></md-checkbox>
    ```

## Import Tailwind CSS (optional)

Import Tailwind and the Material for Tailwind theme to generate utility classes
for Material tokens.

```html
<script src="https://esm.sh/@tailwindcss/browser"></script>
<style type="text/tailwindcss">
  @import 'https://esm.sh/@material/web@nightly/labs/gb/styles/tailwind.css';
</style>
<link rel="stylesheet" href="https://esm.sh/@material/web@nightly/labs/gb/styles/m3.css">
```

> IMPORTANT: Import Tailwind and the Material for Tailwind theme file **before**
> importing a design system stylesheet file, like `m3.css`.

## Full CDN example

```html
<!-- Install from CDN. -->
<script type="importmap">
{
  "imports": {
    "@material/web/": "https://esm.sh/@material/web@nightly/"
  }
}
</script>

<!-- Setup Tailwind (optional). -->
<script src="https://esm.sh/@tailwindcss/browser"></script>
<style type="text/tailwindcss">
  @import 'https://esm.sh/@material/web@nightly/labs/gb/styles/tailwind.css';
</style>

<!-- Import GM3 system stylesheet. -->
<link rel="stylesheet" href="https://static.corp.google.com/material-web/gbreeze/latest/styles/gm3.css">
<!-- Import GM3 typescale and icon fonts. -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,slnt,wdth,wght,GRAD,ROND@6..144,-10..0,25..151,1..1000,0..100,0..100&display=swap">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Google+Symbols:opsz,wght,FILL,GRAD,ROND@20..48,100..700,0..1,-50..200,0..100">
<style>
  :root {
    --md-icon-font: 'Google Symbols';
  }
</style>

<!-- Using styles. -->
<div class="custom-alert">Using tokens</div>
<style>
  .custom-alert {
    display: flex;
    padding: 16px;
    border-radius: var(--md-sys-shape-corner-lg);
    background: var(--md-sys-color-error-container);
    color: var(--md-sys-color-on-error-container);
    font: var(--md-sys-typescale-body-md);
    letter-spacing: var(--md-sys-typescale-body-md-tracking);
  }
</style>

<!-- Import components. -->
<script type="module">
  import '@material/web/labs/gb/components/button/md-button.js';
  import '@material/web/labs/gb/components/checkbox/md-checkbox.js';
</script>

<!-- Using components. -->
<md-button color="filled">Hello Google Material</md-button>
<md-checkbox checked></md-checkbox>

<!-- Using Tailwind -->
<div class="bg-primary text-on-primary typescale-body-lg rounded-lg flex items-center">
  Tailwind utilities
</div>
```

## Troubleshoot

### **Failed to resolve module specifier. Relative references must start with either "/", "./", or "../".**

Issue: an error is thrown when a bare module import `{module_name}` is missing
an `importmap` entry.

```sh
Uncaught TypeError: Failed to resolve module specifier "{module_name}".
Relative references must start with either "/", "./", or "../".
```

Fix: add a `<script type="importmap">` entry for `{module_name}`. Load the
module from a CDN or from an installed path.

```html
<!-- Uncaught TypeError: Failed to resolve module specifier "tslib".
     Relative references must start with either "/", "./", or "../". -->

<!-- Fix: -->
<script type="importmap">
{
  "imports": {
    "tslib": "https://esm.sh/tslib"
  }
}
</script>
```

## What's next

  - Learn how to [use system style tokens](styles.md).
  - Read through the [list of available components](components/).
