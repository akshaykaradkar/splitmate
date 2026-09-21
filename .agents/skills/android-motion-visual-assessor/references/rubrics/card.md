# Card Rubric

1.  [ID: card_purpose] Are cards used to display content and actions about a single subject in a flexible container? [Metric type: Hygiene] [Weight: 5]

2.  [ID: card_container_rounded_corners] If a Card component (Elevated, Filled, or Outlined) is present, does its container comply with the specification: corner radius of 12px (`--droid-sys-shape-corner-medium`) applied to all four corners? [Metric type: Hygiene] [Weight: 10]

3.  [ID: card_text_image_scrim] Is text layered over images only if there is a scrim or protection guaranteeing legibility? [Metric type: Hygiene] [Weight: 5]

4.  [ID: card_elevated_drop_shadow] Do elevated cards use a drop shadow for elevation matching `--droid-sys-elevation-level1` (1px) at resting state? [Metric type: Hygiene] [Weight: 3]

5.  [ID: card_filled_color_token] Do Filled cards utilize the `--droid-sys-color-surface-variant` color token for their container background? [Metric type: Adherence] [Weight: 5]

6.  [ID: card_outlined_border_token] Do Outlined cards utilize the `--droid-sys-color-outline` token for their outer border stroke? [Metric type: Adherence] [Weight: 5]

7.  [ID: card_typography_family] Does the text inside the card conform to the Android Motion typography system, utilizing `'Google Sans'` or `'Google Sans Text'`? [Metric type: Adherence] [Weight: 5]

8.  [ID: card_expansion_motion] If the card is interactive and expands, does its transition duration use `--droid-sys-motion-duration-400` (400ms) or `--droid-sys-motion-duration-500` (500ms) with `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) easing? [Metric type: Adherence] [Weight: 8]
