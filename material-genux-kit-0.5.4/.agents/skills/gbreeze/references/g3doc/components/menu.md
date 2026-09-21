<!-- go/g3mark-in-g3doc -->

# gBreeze menu

## \<md-menu\> & \<md-menu-group\> & \<md-menu-item\>

### Import

```js
import '@material/web/labs/gb/components/menu/md-menu.js';
import '@material/web/labs/gb/components/menu/md-menu-group.js';
import '@material/web/labs/gb/components/menu/md-menu-item.js';
```

### Usage

```html
<button popovertarget="menu">Open Menu</button>
<md-menu id="menu">
  <md-menu-item>Item 1</md-menu-item>
  <md-menu-item>
    Item 2
    <span slot="supporting-text">Supporting text</span>
  </md-menu-item>
  <md-menu-item disabled>Disabled item</md-menu-item>
  <md-menu-group checkable="single">
    <md-menu-item>Radio item 1</md-menu-item>
    <md-menu-item>Radio item 2</md-menu-item>
    <md-menu-item>Radio item 3</md-menu-item>
  </md-menu-group>
  <md-menu-group checkable="multiple">
    <md-menu-item>Checkbox item 1</md-menu-item>
    <md-menu-item checked>Checkbox item 2</md-menu-item>
    <md-menu-item>Checkbox item 3</md-menu-item>
  </md-menu-group>
</md-menu>
```

### API

#### \<md-menu\>

`attributes`, `.properties`, `@events`, and `methods()`.

| API                | Type                      | Description                                  |
| :----------------- | :------------------------ | :------------------------------------------- |
| `color` / `.color` | `'standard' \| 'vibrant'` | Default `'standard'`. The color of the menu. |

#### \<md-menu-group\>

| API                        | Type                             | Description                                                  |
| :------------------------- | :------------------------------- | :----------------------------------------------------------- |
| `checkable` / `.checkable` | `'single' \| 'multiple' \| null` | Default `null`. Defines the selection behavior of the group. |

#### \<md-menu-item\>

| API                      | Type      | Description                                                    |
| :----------------------- | :-------- | :------------------------------------------------------------- |
| `checked` / `.checked`   | `boolean` | Default `false`. Whether the item is checked.                  |
| `disabled` / `.disabled` | `boolean` | Default `false`. Whether the item is disabled.                 |
| `@change`                | `Event`   | Fired when a checkable item's check state changes.             |
| `@input`                 | `Event`   | Fired when a checkable item's check state changes.             |
| `slot="leading"`         |           | Content, such as an icon, that appears before the main text.   |
| `slot="supporting-text"` |           | Secondary text that appears below the main text.               |
| `slot="trailing-text"`   |           | Text or metadata that appears after the main text.             |
| `slot="trailing"`        |           | Content, such as an icon, that appears at the end of the item. |

## Classes

```js
import menuStylesheet from '@material/web/labs/gb/components/menu/menu.css' with {type: 'css'};
```

| Class           | Type        |
| :-------------- | :---------- |
| `.menu`         | Root        |
| `.menu-vibrant` | Modifier    |
| `.menu-host`    | Parent part |

| Class                        | Type       |
| :--------------------------- | :--------- |
| `.menu-item`                 | Root       |
| `.menu-item-content`         | Child part |
| `.menu-item-leading`         | Child part |
| `.menu-item-trailing`        | Child part |
| `.menu-item-supporting-text` | Child part |
| `.menu-item-trailing-text`   | Child part |
