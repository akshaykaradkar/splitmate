# Icon Button

1.  [ID: icon_button_usage] Are icon buttons used for common actions in headers, cards, or toolbars without requiring text? [Metric type: Hygiene] [Weight: 3]

2.  [ID: icon_button_sizes] Do icon buttons comply with Elements GM3 sizing specs: standard icon size of 24dp (minimum 20dp), container size of 40dp for contained styles, and a touch target size of at least 48x48dp? [Metric type: Adherence] [Weight: 5]

3.  [ID: icon_button_no_default_density] Is the default density setting at 0, ensuring that interactive touch targets are NOT shrunk below 48x48dp by default? [Metric type: Adherence] [Weight: 5]

4.  [ID: icon_button_spacing] Do adjacent icon buttons have a spacing of 4dp, or exactly 0dp when nested directly next to each other inside a parent container? [Metric type: Adherence] [Weight: 3]

5.  [ID: icon_button_toggle_style] Do toggle icon buttons represent binary selections by using an outlined icon style in the unselected state and a filled icon style in the selected state? [Metric type: Adherence] [Weight: 5]

6.  [ID: icon_button_toggle_ntc] For toggle icon buttons where the Google Symbols icon exists only as an outline or filled style, does the selection state change the icon weight to semibold (satisfying NTC contrast compliance rather than relying on color change alone)? [Metric type: Adherence] [Weight: 10]

7.  [ID: icon_button_tooltip_hover] Does the icon button display a plain, descriptive tooltip on hover (positioned 4dp above the button, or 4dp below if placed in a top app bar) explaining the action rather than the literal icon name? [Metric type: Hygiene] [Weight: 5]

8.  [ID: icon_button_external_link] If the icon button triggers navigation to an external tab, window, or website, is the accessibility label appended with "opens new window" and is its ARIA role defined as "link"? [Metric type: Adherence] [Weight: 5]
