<!-- go/g3mark-in-g3doc -->

# gBreeze switch

## \<md-switch\>

### Import

```js
import '@material/web/labs/gb/components/switch/md-switch.js';
```

### Usage

```html
<md-switch></md-switch>
<md-switch selected></md-switch>

<md-switch>
  <md-icon slot="off-icon">close</md-icon>
  <md-icon slot="on-icon">check</md-icon>
</md-switch>
```

### API

`attributes`, `.properties`, `@events`, and `methods()`.

| API                                    | Type      | Description                                                        |
| :------------------------------------- | :-------- | :----------------------------------------------------------------- |
| `selected` / `.selected`               | `boolean` | Default `false`. Whether or not the switch is selected.            |
| `defaultselected` / `.defaultSelected` | `boolean` | Default `false`. Whether or not the switch is selected by default. |
| `required` / `.required`               | `boolean` | Default `false`. Whether or not the switch is required.            |
| `disabled` / `.disabled`               | `boolean` | Default `false`. Disables the switch.                              |
| `@change`                              | `Event`   | Fired when the selected state changes.                             |
| `@input`                               | `Event`   | Fired when the selected state changes.                             |
| `slot="off-icon"`                      |           | An icon to show in the handle when unselected.                     |
| `slot="on-icon"`                       |           | An icon to show in the handle when selected.                       |

## Classes

```js
import switchStylesheet from '@material/web/labs/gb/components/switch/switch.css' with {type: 'css'};
```

| Class              | Type       |
| :----------------- | :--------- |
| `.switch`          | Root       |
| `.switch-icon-off` | Child part |
| `.switch-icon-on`  | Child part |
