---
name: gbreeze
description: >-
  Generates Material Design web user interfaces using CSS custom properties, utility classes, and custom elements from the Material Web Components (MWC) gBreeze labs project. Use when generating web prototypes that follow Material Design principles, including custom design systems derived from Material such as Google Material 3 (GM3).
---

# gBreeze

## Quick recipe: Hello World with CDN

```html
<script type="importmap">
{
  "imports": {
    "@material/web/": "https://esm.sh/@material/web@nightly/"
  }
}
</script>

<!-- Import design system stylesheet -->
<link rel="stylesheet" href="https://esm.sh/@material/web@nightly/labs/gb/styles/m3.css">
<!-- Import typescale and icon fonts -->
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,slnt,wdth,wght,GRAD,ROND@6..144,-10..0,25..151,1..1000,0..100,0..100&display=swap">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200">

<!-- Import components -->
<script type="module">
  import '@material/web/labs/gb/components/button/md-button.js';
</script>

<!-- Use custom elements -->
<md-button color="filled">Hello World</md-button>
```

## Resources

*   [Quickstart](references/g3doc/quickstart.md): installation, setup, and usage
    quickstart, including a hello world example web app that uses a CDN.
*   [Styles documentation](references/g3doc/styles.md): documentation for system
    stylesheets and global `--md-*` custom property tokens provided for color,
    elevation, icons, motion, shape, typography, and spacing.
*   [Tailwind](references/tailwind.md): importing and using the Material
    Tailwind theme.

## Components

### Actions

*   [Button](references/g3doc/components/button.md): Buttons, links, and toggle
    buttons with various emphasis options such as filled, elevated, and outlined
    styles.
*   [FAB](references/g3doc/components/fab.md): A single prominent floating
    action button used for the primary or most common action on a screen.
*   [Icon button](references/g3doc/components/iconbutton.md): A compact button,
    link, or toggle button containing an icon with various emphasis options such
    as filled, outlined, or standard.
*   [Split button](references/g3doc/components/splitbutton.md): A dual-function
    button with a primary action and secondary action that shows a menu of
    options.

### Form controls

*   [Checkbox](references/g3doc/components/checkbox.md): A form checkbox for
    multiple selection in a group.
*   [Radio](references/g3doc/components/radio.md): A form radio for single
    selection in a group.
*   [Switch](references/g3doc/components/switch.md): Toggles between selected
    and unselected state for an item.

### Layout & containers

*   [Card](references/g3doc/components/card.md): A flexible container to
    visually group and display collections of related information and actions.
*   [Divider](references/g3doc/components/divider.md): A thin line that
    separates content.
*   [List](references/g3doc/components/list.md): A flexible container of items
    with rich text and content that can be used for actions, selection, or as
    static content.
*   [Menu](references/g3doc/components/menu.md): Displays a list of actions or
    selectable items on a popover surface that is attached to the component that
    opened it.

### Building blocks

*   [Focus ring](references/g3doc/components/focus.md): A focus outline
    indicator for accessibility during keyboard navigation.
*   [Ripple](references/g3doc/components/ripple.md): A surface effect for touch
    and pointer feedback that indicates an action.

## Common gotchas

*   **Icon usage**: Use `<md-icon>` from `/styles/icon/md-icon`, or the
    `.md-icon` utility class. Do not use default icon utility classes, such as
    `.material-symbols` or `.material-symbols-outlined`.
*   **Google Material 3 (GM3)**: Import
    <https://static.corp.google.com/material-web/gbreeze/latest/styles/gm3.css>
    instead of `m3.css`. Import `'Google Sans Flex'` and use `--md-icon-font:
    'Google Symbols'` for icons.
*   **Fallback to MWC**: Use non-gBreeze components from `@material/web` as a
    fallback for components that are not yet implemented.
*   **google3 build**: Reference [google3](references/google3.md) to use gBreeze
    with `blaze` and google3 `BUILD` rules instead of NPM and CDNs.

### Tailwind

*   **Import Tailwind & theme first**: Import Tailwind and the `tailwind.css`
    theme file before importing a design system stylesheet.
*   **Use `.typescale-<name>-<size>` for fonts**: Use the custom typescale
    utility class. Do not use `.text-<font-family>`, `.text-<size>`,
    `.tracking-*` or `.leading-*`.
*   **Component interaction styles and shadows**: gBreeze components, like
    `<md-button>`, provide their own interactive states and elevation shadows.
    Do not add Tailwind utilities like `.shadow-sm` or `.hover:bg-surface/10` to
    gBreeze components.