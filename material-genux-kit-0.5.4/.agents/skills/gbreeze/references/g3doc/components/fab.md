<!-- go/g3mark-in-g3doc -->

# gBreeze fab

## \<md-fab\>

### Import

```js
import '@material/web/labs/gb/components/fab/md-fab.js';
import '@material/web/labs/gb/styles/icon/md-icon.js';
```

### Usage

```html
<md-fab aria-label="Add new">
  <md-icon>add</md-icon>
</md-fab>
```

```html
<md-fab>
  <md-icon>add</md-icon>
  Add new
</md-fab>
```

```html
<md-fab aria-label="Add new" color="primary-container">
  <md-icon>add</md-icon>
</md-fab>
<md-fab aria-label="Add new" color="secondary-container">
  <md-icon>add</md-icon>
</md-fab>
<md-fab aria-label="Add new" color="tertiary-container">
  <md-icon>add</md-icon>
</md-fab>
<md-fab aria-label="Add new" color="primary">
  <md-icon>add</md-icon>
</md-fab>
<md-fab aria-label="Add new" color="secondary">
  <md-icon>add</md-icon>
</md-fab>
<md-fab aria-label="Add new" color="tertiary">
  <md-icon>add</md-icon>
</md-fab>
```

```html
<md-fab aria-label="Add new">
  <md-icon>add</md-icon>
</md-fab>
<md-fab aria-label="Add new" size="md">
  <md-icon>add</md-icon>
</md-fab>
<md-fab aria-label="Add new" size="lg">
  <md-icon>add</md-icon>
</md-fab>
```

### API

`attributes`, `.properties`, `@events`, and `methods()`.

| API                | Type                                                                                                             | Description                                          |
| :----------------- | :--------------------------------------------------------------------------------------------------------------- | :--------------------------------------------------- |
| `color` / `.color` | `'primary' \| 'primary-container' \| 'secondary' \| 'secondary-container' \| 'tertiary' \| 'tertiary-container'` | Default `'primary-container'`. The color of the fab. |
| `size` / `.size`   | `'default' \| 'md' \| 'lg'`                                                                                      | Default `'default'`. The size of the fab.            |

## Classes

```js
import fabStylesheet from '@material/web/labs/gb/components/fab/fab.css' with {type: 'css'};
```

| Class                      | Type     |
| :------------------------- | :------- |
| `.fab`                     | Root     |
| `.fab-primary`             | Modifier |
| `.fab-primary-container`   | Modifier |
| `.fab-secondary`           | Modifier |
| `.fab-secondary-container` | Modifier |
| `.fab-tertiary`            | Modifier |
| `.fab-tertiary-container`  | Modifier |
| `.fab-md`                  | Modifier |
| `.fab-lg`                  | Modifier |
