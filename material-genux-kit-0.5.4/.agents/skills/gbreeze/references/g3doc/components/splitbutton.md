<!-- go/g3mark-in-g3doc -->

# gBreeze split button

## \<md-split-button\>

### Import

```js
import '@material/web/labs/gb/components/splitbutton/md-split-button.js';
import '@material/web/labs/gb/components/menu/md-menu.js';
import '@material/web/labs/gb/components/menu/md-menu-item.js';
```

### Usage

```html
<md-split-button color="filled">
  <button slot="leading">Filled</button>
  <button slot="trailing" popovertarget="menu"></button>
  <md-menu id="menu">
    <md-menu-item>Option 1</md-menu-item>
    <md-menu-item>Option 2</md-menu-item>
  </md-menu>
</md-split-button>
```

### API

`attributes`, `.properties`, `@events`, and `methods()`.

| API                      | Type                                              | Description                                                      |
| :----------------------- | :------------------------------------------------ | :--------------------------------------------------------------- |
| `color` / `.color`       | `'filled' \| 'elevated' \| 'tonal' \| 'outlined'` | Default `'filled'`. The color of the split button.               |
| `size` / `.size`         | `'xs' \| 'sm' \| 'md' \| 'lg' \| 'xl'`            | Default `'sm'`. The size of the split button.                    |
| `selected` / `.selected` | `boolean`                                         | Default `false`. Whether or not the split button's menu is open. |

## Classes

```js
import splitButtonStylesheet from '@material/web/labs/gb/components/splitbutton/split-button.css' with {type: 'css'};
```

| Class                 | Type     |
| :-------------------- | :------- |
| `.split-btn`          | Root     |
| `.split-btn-selected` | Modifier |

