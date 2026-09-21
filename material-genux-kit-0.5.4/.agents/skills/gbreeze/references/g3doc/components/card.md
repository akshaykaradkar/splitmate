<!-- go/g3mark-in-g3doc -->

# gBreeze card

## \<md-card\>

### Import

```js
import '@material/web/labs/gb/components/card/md-card.js';
```

### Usage

```html
<md-card>
  <div class="content">Card content</div>
</md-card>
```

```html
<md-card color="elevated">
  <div class="content">Card content</div>
</md-card>
<md-card color="filled">
  <div class="content">Card content</div>
</md-card>
<md-card color="outlined">
  <div class="content">Card content</div>
</md-card>
```

```html
<md-card class="layout-card" color="elevated">
  <div class="content">
    <h2>Headline</h2>
    <h3>Subhead</h3>
    <p>
      Explain more about the topic shown in the medium display and subhead
      through supporting text here.
    </p>
    <div class="actions">
      <md-button color="outlined">Action</md-button>
      <md-button color="filled">Action</md-button>
    </div>
  </div>
</md-card>
```

### API

`attributes`, `.properties`, `@events`, and `methods()`.

| API                            | Type                                   | Description                                       |
| :----------------------------- | :------------------------------------- | :------------------------------------------------ |
| `color` / `.color`             | `'filled' \| 'elevated' \| 'outlined'` | Default `'outlined'`. The color of the card.      |
| `disabled` / `.disabled`       | `boolean`                              | Default `false`. Whether the card is disabled.    |
| `interactive` / `.interactive` | `boolean`                              | Default `false`. Whether the card is interactive. |

## Classes

```js
import cardStylesheet from '@material/web/labs/gb/components/card/card.css' with {type: 'css'};
```

| Class            | Type       |
| :--------------- | :--------- |
| `.card`          | Root       |
| `.card-filled`   | Modifier   |
| `.card-elevated` | Modifier   |
| `.card-outlined` | Modifier   |
| `.card-btn`      | Child part |
