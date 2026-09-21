# Banners Rubric

1.  [ID: banners_basic_square] Is a square basic banner (with straight corners using `--droid-sys-shape-corner-none`) placed at the top of the screen directly below the app bar? [Metric type: Hygiene] [Weight: 3]

2.  [ID: banners_basic_round] Is a round basic banner (featuring a 28px corner radius via `--droid-sys-shape-corner-extra-large`) placed above or inline with body content? [Metric type: Hygiene] [Weight: 5]

3.  [ID: banners_rich_inline] Is a rich banner placed inline within scrolling body content? [Metric type: Hygiene] [Weight: 3]

4.  [ID: banners_dismissal_element] Does the banner contain at most one type of dismissal element, featuring either action text buttons or a close icon button, but not both? [Metric type: Hygiene] [Weight: 5]

5.  [ID: banners_action_placement] If action text buttons are present, is the secondary action placed to the left of the primary confirming action? [Metric type: Hygiene] [Weight: 3]

6.  [ID: banners_rich_image_square] If a leading image is present in a rich banner, does it appear as a square thumbnail (80x80px)? [Metric type: Hygiene] [Weight: 3]

7.  [ID: banners_max_one_visible] Does the screen contain at most one visible banner at any time? [Metric type: Hygiene] [Weight: 3]

8.  [ID: banners_container_colors] Do banners utilize `--droid-sys-color-surface-variant` for container background and `--droid-sys-color-on-surface-variant` for text content? [Metric type: Adherence] [Weight: 5]

9.  [ID: banners_divider] If a bottom border divider is present, does it utilize the `--droid-sys-color-outline` token? [Metric type: Adherence] [Weight: 3]

10. [ID: banners_typography] Does the messaging text use `'Google Sans Text'` and conform to `--droid-sys-typescale-body-large` or `--droid-sys-typescale-title-medium`? [Metric type: Adherence] [Weight: 3]

11. [ID: banners_action_typography] Do action buttons utilize `'Google Sans Text'` matching `--droid-sys-typescale-label-large`? [Metric type: Adherence] [Weight: 3]

12. [ID: banners_entrance_motion] If the banner is dynamically displayed, does its container slide or expand using `--droid-sys-motion-duration-300` (300ms) with `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) easing? [Metric type: Adherence] [Weight: 5]
