# Badge

1.  [ID: badge_usage_attachment] Are summary, count, or feature badges appended
    directly to their associated parent controls or navigation icons rather than
    floating standalone? [Metric type: Adherence] [Weight: 3]

2.  [ID: badge_container_shape] Are badge shapes and heights compliant with their Elements GM3 type:
    - Summary Badge: 6dp circle/dot
    - Information Badge: 20dp height with 4dp corner radius
    - Navigation Feature Badge: 16dp height with 8dp corner radius
    - Count Badge: 16dp height with 8dp corner radius
    - Product Badge: 16dp height with hanging bottom-rounded `0dp, 0dp, 16dp, 16dp` corner radius?
    [Metric type: Hygiene] [Weight: 5]

3.  [ID: badge_text_uppercase_avoided] Is badge text formatted using standard sentence or title case
    instead of all-uppercase letters to ensure readability and correct screen-reader pronunciation?
    [Metric type: Adherence] [Weight: 3]

4.  [ID: badge_count_digit_cap] If a Count Badge exceeds its digit cap, is it formatted
    cleanly using a "+" sign (e.g., "99+") rather than overflowing the container?
    [Metric type: Hygiene] [Weight: 3]

5.  [ID: badge_info_color_semantic] Do Information Badges use standard Elements GM3 semantic colors
    (Grey, Blue, Green, Yellow, Red) paired with clear text labels so color is not the sole indicator of status?
    [Metric type: Hygiene] [Weight: 3]

6.  [ID: badge_interactive_tooltip_specs] If an Information Badge anchors a tooltip, is it configured as
    interactive (keyboard focusable, button role, aria-describedby attribute, and meeting GAR Non-text contrast)?
    [Metric type: Adherence] [Weight: 5]
