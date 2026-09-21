<!-- go/g3mark-in-g3doc -->

# gBreeze checkbox

## \<md-checkbox\>

### Import

```js
import '@material/web/labs/gb/components/checkbox/md-checkbox.js';
```

### Usage

```html
<md-checkbox></md-checkbox>
<md-checkbox checked></md-checkbox>
<md-checkbox indeterminate></md-checkbox>
<md-checkbox error></md-checkbox>
```

### API

`attributes`, `.properties`, `@events`, and `methods()`.

| API                                  | Type      | Description                                                          |
| :----------------------------------- | :-------- | :------------------------------------------------------------------- |
| `checked` / `.checked`               | `boolean` | Default `false`. Whether or not the checkbox is selected.            |
| `defaultchecked` / `.defaultChecked` | `boolean` | Default `false`. Whether or not the checkbox is selected by default. |
| `indeterminate` / `.indeterminate`   | `boolean` | Default `false`. Whether or not the checkbox is indeterminate.       |
| `disabled` / `.disabled`             | `boolean` | Default `false`. Disables the checkbox.                              |
| `required` / `.required`             | `boolean` | Default `false`. Makes the checkbox required.                        |
| `error` / `.error`                   | `boolean` | Default `false`. Sets the checkbox to an error state.                |
| `@change`                            | `Event`   | Fired when the checkbox checked state changes.                       |
| `@input`                             | `Event`   | Fired when the checkbox checked state changes.                       |

## Classes

```js
import checkboxStylesheet from '@material/web/labs/gb/components/checkbox/checkbox.css' with {type: 'css'};
```

| Class       | Type |
| :---------- | :--- |
| `.checkbox` | Root |
