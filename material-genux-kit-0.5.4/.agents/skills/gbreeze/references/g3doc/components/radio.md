<!-- go/g3mark-in-g3doc -->

# gBreeze radio

## \<md-radio\>

### Import

```js
import '@material/web/labs/gb/components/radio/md-radio.js';
```

### Usage

```html
<md-radio name="group"></md-radio>
<md-radio name="group"></md-radio>
<md-radio name="group"></md-radio>
```

### API

`attributes`, `.properties`, `@events`, and `methods()`.

| API                                  | Type      | Description                                                                                |
| :----------------------------------- | :-------- | :----------------------------------------------------------------------------------------- |
| `checked` / `.checked`               | `boolean` | Default `false`. Whether or not the radio is selected.                                     |
| `defaultchecked` / `.defaultChecked` | `boolean` | Default `false`. Whether or not the radio is selected by default.                          |
| `name` / `.name`                     | `string`  | Default `''`. The name of the form control. Radio's with the same name form a radio group. |
| `required` / `.required`             | `boolean` | Default `false`. Whether or not the radio's group is required.                             |
| `disabled` / `.disabled`             | `boolean` | Default `false`. Disables the radio.                                                       |
| `@change`                            | `Event`   | Fired when the checked state changes.                                                      |
| `@input`                             | `Event`   | Fired when the checked state changes.                                                      |

## Classes

```js
import radioStylesheet from '@material/web/labs/gb/components/radio/radio.css' with {type: 'css'};
```

| Class    | Type |
| :------- | :--- |
| `.radio` | Root |
