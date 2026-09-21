<!-- go/g3mark-in-g3doc -->

# gBreeze icon button

## \<md-icon-button\>

### Import

```js
import '@material/web/labs/gb/components/iconbutton/md-icon-button.js';
import '@material/web/labs/gb/styles/icon/md-icon.js';
```

### Usage

```html
<md-icon-button aria-label="More options">
  <md-icon>more_vert</md-icon>
</md-icon-button>
```

```html
<md-icon-button aria-label="More options" color="filled">
  <md-icon>more_vert</md-icon>
</md-icon-button>
<md-icon-button aria-label="More options" color="tonal">
  <md-icon>more_vert</md-icon>
</md-icon-button>
<md-icon-button aria-label="More options" color="outlined">
  <md-icon>more_vert</md-icon>
</md-icon-button>
<md-icon-button aria-label="More options" color="standard">
  <md-icon>more_vert</md-icon>
</md-icon-button>
```

```html
<md-icon-button aria-label="More options" size="xs">
  <md-icon>more_vert</md-icon>
</md-icon-button>
<md-icon-button aria-label="More options" size="sm">
  <md-icon>more_vert</md-icon>
</md-icon-button>
<md-icon-button aria-label="More options" size="md">
  <md-icon>more_vert</md-icon>
</md-icon-button>
<md-icon-button aria-label="More options" size="lg">
  <md-icon>more_vert</md-icon>
</md-icon-button>
<md-icon-button aria-label="More options" size="xl">
  <md-icon>more_vert</md-icon>
</md-icon-button>
```

```html
<md-icon-button aria-label="More options" width="narrow">
  <md-icon>more_vert</md-icon>
</md-icon-button>
<md-icon-button aria-label="More options">
  <md-icon>more_vert</md-icon>
</md-icon-button>
<md-icon-button aria-label="More options" width="wide">
  <md-icon>more_vert</md-icon>
</md-icon-button>
```

### API

`attributes`, `.properties`, `@events`, and `methods()`.

| API                               | Type                                                    | Description                                                                   |
| :-------------------------------- | :------------------------------------------------------ | :---------------------------------------------------------------------------- |
| `color` / `.color`                | `'filled' \| 'tonal' \| 'outlined' \| 'standard'`       | Default `'standard'`. The color of the icon button.                           |
| `size` / `.size`                  | `'xs' \| 'sm' \| 'md' \| 'lg' \| 'xl'`                  | Default `'sm'`. The size of the button.                                       |
| `square` / `.square`              | `boolean`                                               | Default `false`. Changes the shape of the button to be square.                |
| `width` / `.width`                | `'narrow' \| 'wide' \| ''`                              | Default `''`. Changes the width of the button.                                |
| `type` / `.type`                  | `'submit' \| 'reset' \| 'button' \| 'toggle' \| 'link'` | Default `submit`. Behavior of the button.                                     |
| `selected` / `.selected`          | `boolean`                                               | Default `false`. Whether or not the button is selected, when `type="toggle"`. |
| `href` / `.href`                  | `string`                                                | Default `''`. The URL that the `type="link"` points to.                       |
| `target` / `.target`              | `'_blank' \| '_parent' \| '_self' \| '_top' \| ''`      | Default `''`. Where to display the linked `href` URL.                         |
| `disabled` / `.disabled`          | `boolean`                                               | Default `false`. Disables the button.                                         |
| `soft-disabled` / `.softDisabled` | `boolean`                                               | Default `false`. Disables the button but keeps it focusable.                  |
| `@change`                         | `Event`                                                 | Fired when `type="toggle"` buttons change.                                    |

## Classes

```js
import iconButtonStylesheet from '@material/web/labs/gb/components/iconbutton/icon-button.css' with {type: 'css'};
```

| Class                  | Type     |
| :--------------------- | :------- |
| `.icon-btn`            | Root     |
| `.icon-btn-filled`     | Modifier |
| `.icon-btn-tonal`      | Modifier |
| `.icon-btn-outlined`   | Modifier |
| `.icon-btn-standard`   | Modifier |
| `.icon-btn-xs`         | Modifier |
| `.icon-btn-sm`         | Modifier |
| `.icon-btn-md`         | Modifier |
| `.icon-btn-lg`         | Modifier |
| `.icon-btn-xl`         | Modifier |
| `.icon-btn-square`     | Modifier |
| `.icon-btn-narrow`     | Modifier |
| `.icon-btn-wide`       | Modifier |
| `.icon-btn-unselected` | Modifier |
| `.icon-btn-selected`   | Modifier |
