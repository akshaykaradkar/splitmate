<!-- go/g3mark-in-g3doc -->

# gBreeze divider

## \<md-divider\>

### Import

```js
import '@material/web/labs/gb/components/divider/md-divider.js';
```

### Usage

```html
<div class="column">
  <div>Vertical</div>
  <md-divider></md-divider>
  <div>Items</div>
</div>

<div class="row">
  <div>Horizontal</div>
  <md-divider vertical></md-divider>
  <div>Items</div>
</div>
```

### API

`attributes`, `.properties`, `@events`, and `methods()`.

| API                      | Type      | Description                                              |
| :----------------------- | :-------- | :------------------------------------------------------- |
| `vertical` / `.vertical` | `boolean` | Default `false`. Whether or not the divider is vertical. |

## Classes

```js
import dividerStylesheet from '@material/web/labs/gb/components/divider/divider.css' with {type: 'css'};
```

| Class               | Type     |
| :------------------ | :------- |
| `.divider`          | Root     |
| `.divider-vertical` | Modifier |
