# Tooltip

1.  [ID: tooltip_usage] Is the tooltip used appropriately for simple descriptions (plain) or detailed feature explanations (rich)? [Metric type: Hygiene] [Weight: 3]

2.  [ID: tooltip_plain_no_interactive] Do plain tooltips remain completely non-interactive, containing only a concise text label and no buttons, links, or other interactive elements? [Metric type: Hygiene] [Weight: 3]

3.  [ID: tooltip_rich_max_buttons] Do rich tooltips contain a maximum of 2 text buttons, placed side-by-side where possible to avoid vertical stacking? [Metric type: Hygiene] [Weight: 3]

4.  [ID: tooltip_obscure_focus_indicators] Do tooltips appear without covering or obscuring the underlying parent UI element or permanently blocking adjacent focus indicators/critical UI? [Metric type: Hygiene] [Weight: 5]

5.  [ID: tooltip_plain_dimensions] If plain tooltips are present, do they comply with standard dimensions: a strict container height of 24dp and 8dp of internal padding on all sides (`--md-sys-measurement-space100`)? [Metric type: Adherence] [Weight: 5]

6.  [ID: tooltip_plain_colors] Do plain tooltips use the high-contrast inverse color mappings, specifically `--md-sys-color-inverse-surface` for container background and `--md-sys-color-inverse-on-surface` for text color? [Metric type: Adherence] [Weight: 5]

7.  [ID: tooltip_rich_corner_radius] If rich tooltips are present, do they utilize rounded corners of `--md-sys-shape-corner-medium` (12px) or larger to signal their status as richer container elements? [Metric type: Adherence] [Weight: 3]

8.  [ID: tooltip_focus_trapping] Do rich tooltips avoid trapping keyboard or screen reader focus, allowing users to navigate linearly past them and move through the rest of the page? [Metric type: Hygiene] [Weight: 5]
