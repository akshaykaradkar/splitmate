<!-- go/g3mark-in-g3doc -->

# gBreeze button

## \<md-button\>

### Import

```js
import '@material/web/labs/gb/components/button/md-button.js';
```

### Usage

```html
<md-button color="filled">Filled</md-button>
<md-button color="elevated">Elevated</md-button>
<md-button color="outlined">Outlined</md-button>
<md-button color="tonal">Tonal</md-button>
<md-button>Text</md-button>
```

```html
<md-button color="filled" size="xs">Extra small</md-button>
<md-button color="filled">Small</md-button>
<md-button color="filled" size="md">Medium</md-button>
<md-button color="filled" size="lg">Large</md-button>
<md-button color="filled" size="xl">Extra large</md-button>
```

```html
<md-button color="tonal" type="toggle">Unselected</md-button>
<md-button color="tonal" type="toggle" selected>Selected</md-button>
```

### API

`attributes`, `.properties`, `@events`, and `methods()`.

| API                               | Type                                                        | Description                                                                   |
| :-------------------------------- | :---------------------------------------------------------- | :---------------------------------------------------------------------------- |
| `color` / `.color`                | `'filled' \| 'elevated' \| 'tonal' \| 'outlined' \| 'text'` | Default `'text'`. The color of the button.                                    |
| `size` / `.size`                  | `'xs' \| 'sm' \| 'md' \| 'lg' \| 'xl'`                      | Default `'sm'`. The size of the button.                                       |
| `square` / `.square`              | `boolean`                                                   | Default `false`. Changes the shape of the button.                             |
| `type` / `.type`                  | `'submit' \| 'reset' \| 'button' \| 'toggle' \| 'link'`     | Default `submit`. Changes the behavior of the button.                         |
| `selected` / `.selected`          | `boolean`                                                   | Default `false`. Whether or not the button is selected, when `type="toggle"`. |
| `href` / `.href`                  | `string`                                                    | Default `''`. The URL that the `type="link"` points to.                       |
| `target` / `.target`              | `'_blank' \| '_parent' \| '_self' \| '_top' \| ''`          | Default `''`. Where to display the linked `href` URL.                         |
| `disabled` / `.disabled`          | `boolean`                                                   | Default `false`. Disables the button.                                         |
| `soft-disabled` / `.softDisabled` | `boolean`                                                   | Default `false`. Disables the button, but allows focusing for accessibility.  |
| `@change`                         | `Event`                                                     | Fired when `type="toggle"` buttons change.                                    |

## Classes

```js
import buttonStylesheet from '@material/web/labs/gb/components/button/button.css' with {type: 'css'};
```

| Class             | Type     |
| :---------------- | :------- |
| `.btn`            | Root     |
| `.btn-filled`     | Modifier |
| `.btn-elevated`   | Modifier |
| `.btn-tonal`      | Modifier |
| `.btn-outlined`   | Modifier |
| `.btn-text`       | Modifier |
| `.btn-xs`         | Modifier |
| `.btn-sm`         | Modifier |
| `.btn-md`         | Modifier |
| `.btn-lg`         | Modifier |
| `.btn-xl`         | Modifier |
| `.btn-square`     | Modifier |
| `.btn-unselected` | Modifier |
| `.btn-selected`   | Modifier |
