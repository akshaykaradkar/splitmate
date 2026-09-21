# Badge Rubric

1.  [ID: badge_usage] Are badges appended directly to navigation icons or buttons rather than placed as standalone tags or chips? [Metric type: Adherence] [Weight: 3]

2.  [ID: badge_container_shape] Are small badges shaped as simple solid circles (6px diameter), and large badges shaped as 16px-tall rounded rectangles with an 8px corner radius (`--droid-sys-shape-corner-small`)? [Metric type: Hygiene] [Weight: 5]

3.  [ID: badge_mandatory_elements] Does a large badge contain label text inside its container? [Metric type: Hygiene] [Weight: 3]

4.  [ID: badge_large_label_length] Are large badges limited to 4 characters or fewer (including symbols like "+", e.g., "999+")? [Metric type: Adherence] [Weight: 3]

5.  [ID: badge_error_color_default] Do badges use the `--droid-sys-color-error` container fill color and `--droid-sys-color-on-error` text color by default for alerts? [Metric type: Hygiene] [Weight: 5]

6.  [ID: badge_typography] Does the large badge label text use the `'Google Sans Text'` font family and match the `--droid-sys-typescale-label-small` token? [Metric type: Adherence] [Weight: 3]

7.  [ID: badge_large_padding] Do large badges have 4px internal side padding to avoid text touching the container edges? [Metric type: Hygiene] [Weight: 3]

8.  [ID: badge_entrance_motion] When appearing, does the badge scale up from its anchor point over a duration of `--droid-sys-motion-duration-150` (150ms) using the `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) curve? [Metric type: Adherence] [Weight: 5]
