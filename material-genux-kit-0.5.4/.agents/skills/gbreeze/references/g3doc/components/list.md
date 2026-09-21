<!-- go/g3mark-in-g3doc -->

# gBreeze list

## \<md-list\> & \<md-list-item\>

### Import

```js
import '@material/web/labs/gb/components/list/md-list.js';
import '@material/web/labs/gb/components/list/md-list-item.js';
```

### Usage

```html
<md-list>
  <md-list-item>Basic Item</md-list-item>
  <md-list-item>
    <md-icon slot="leading">star</md-icon>
    With Leading Icon
  </md-list-item>
  <md-list-item>
    <span slot="avatar">A</span>
    With Avatar & Supporting Text
    <span slot="supporting-text">Supporting text goes here</span>
  </md-list-item>
  <md-list-item style="align-items: start;">
    <md-icon slot="leading">image</md-icon>
    <span slot="overline">Overline text</span>
    Complex Item
    <span slot="supporting-text">
      With overline, support text, and two icons
    </span>
    <span slot="trailing-text">100+</span>
    <md-icon slot="trailing">chevron_right</md-icon>
  </md-list-item>
  <md-list-item checked>
    <md-icon slot="leading">check</md-icon>
    Selected Item
  </md-list-item>
  <md-list-item disabled>
    <md-icon slot="leading">block</md-icon>
    Disabled Item
    <span slot="supporting-text">This item is disabled</span>
  </md-list-item>
</md-list>
```

### API

`attributes`, `.properties`, `@events`, and `methods()`.

#### \<md-list\>

| API                        | Type      | Description                                                       |
| :------------------------- | :-------- | :---------------------------------------------------------------- |
| `segmented` / `.segmented` | `boolean` | Default `false`. Whether to render the list with segmented items. |

#### \<md-list-item\>

| API                          | Type      | Description                                                    |
| :--------------------------- | :-------- | :------------------------------------------------------------- |
| `checked` / `.checked`       | `boolean` | Default `false`. Whether the list item is selected.            |
| `disabled` / `.disabled`     | `boolean` | Default `false`. Whether the list item is disabled.            |
| `static` / `.nonInteractive` | `boolean` | Default `false`. Whether the list item is non-interactive.     |
| `slot="avatar"`              |           | 1-2 characters or an image that appears before the main text.  |
| `slot="leading"`             |           | Content, such as an icon, that appears before the main text.   |
| `slot="overline"`            |           | Text that appears above the main text.                         |
| `slot="supporting-text"`     |           | Secondary text that appears below the main text.               |
| `slot="trailing-text"`       |           | Text or metadata that appears after the main text.             |
| `slot="trailing"`            |           | Content, such as an icon, that appears at the end of the item. |

## Classes

```js
import listStylesheet from '@material/web/labs/gb/components/list/list.css' with {type: 'css'};
```

| Class             | Type        |
| :---------------- | :---------- |
| `.list`           | Root        |
| `.list-segmented` | Modifier    |
| `.list-select`    | Parent part |

| Class                        | Type       |
| :--------------------------- | :--------- |
| `.list-item`                 | Root       |
| `.list-item-static`          | Modifier   |
| `.list-item-content`         | Child part |
| `.list-item-leading`         | Child part |
| `.list-item-trailing`        | Child part |
| `.list-item-overline`        | Child part |
| `.list-item-supporting-text` | Child part |
| `.list-item-trailing-text`   | Child part |
| `.list-item-avatar`          | Child part |
| `.list-item-radio`           | Child part |
| `.list-item-checkbox`        | Child part |
