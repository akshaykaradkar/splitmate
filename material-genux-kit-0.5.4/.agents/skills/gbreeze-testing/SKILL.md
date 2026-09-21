---
name: gbreeze-testing
description: Best practices for simulating user interactions with gBreeze components in tests or browser automation.
---
# gBreeze UI Interaction Guide

## Component Selector Map

Refer to this table when targeting gBreeze elements in tests:

| React Component | CSS Selector | Accessible Attribute |
| :--- | :--- | :--- |
| `Button` | `.btn` | `role="button"` |
| `IconButton` | `.icon-btn` | `aria-label`, `aria-pressed` |
| `Fab` | `.fab` | `role="button"` |
| `Checkbox` | `.checkbox` | `role="checkbox"`, `aria-checked` |
| `Switch` | `.switch` | `role="switch"`, `aria-checked` |
| `Radio` | `.radio` | `role="radio"`, `aria-checked` |
| `Chip` | `.chip` | `aria-selected` |
| `Dialog` | `.dialog` or `<dialog>` | `role="dialog"`, `aria-modal` |
| `Menu` | `.menu` | `role="menu"` |
| `Tabs` | `.tabs` | `role="tablist"`, `role="tab"` |
| `TextField` | `.text-field` | `role="textbox"` or `<input>` |
| `Slider` | `.slider` | `role="slider"`, `aria-valuenow` |
| `NavigationBar` | `.navigation-bar` | `role="navigation"` |
| `NavigationRail` | `.navigation-rail` | `role="navigation"` |
| `TopAppBar` | `.top-app-bar` | `role="banner"` |
| `Snackbar` | `.snackbar` | `role="status"` |

## Interaction Rules

1. **Target Semantic Classes**: Standard HTML elements are overlaid with custom CSS. To click buttons, target classes like `.icon-btn`, `.btn`, `.chip`, or `.checkbox` instead of generic `<button>` or `<input>` tags.
2. **Use Accessible Labels**: Always prioritize `aria-label` or `aria-pressed` states to identify icon-only buttons (like expand/collapse chevrons or the assign agent button).
3. **Wait for Material Animations**: gBreeze components use complex CSS transitions (`ripple`, `focus-ring`). Add brief waits (~300ms) after clicking interactive elements to ensure layout shifts and dropdown/popovers have fully settled before executing the next action.
4. **Layout Shifts**: Tasks and components use flexbox/grid. Expanding a component (like a nested task list) will physically push other tasks down. Always verify new coordinates or re-query DOM elements after an expansion action.
5. **Popover Elements**: Menus and Dialogs use the HTML `popover` API. They exist in the top layer and may not be found inside the component's DOM subtree. Query them from the document root.

## Test Patterns

### Example: Testing a toggle IconButton
```ts
const btn = page.locator('.icon-btn[aria-label="Favorite"]');
await btn.click();
await expect(btn).toHaveAttribute('aria-pressed', 'true');
```

### Example: Testing a Dialog
```ts
await page.locator('.btn', { hasText: 'Delete' }).click();
const dialog = page.locator('dialog[open]');
await expect(dialog).toBeVisible();
await dialog.locator('.btn', { hasText: 'Confirm' }).click();
await expect(dialog).not.toBeVisible();
```

### Example: Testing a Switch
```ts
const toggle = page.locator('.switch[aria-label="Dark mode"]');
await toggle.click();
await expect(toggle).toHaveAttribute('aria-checked', 'true');
```
