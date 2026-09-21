# Loading Indicator

1.  [ID: loading_indicator_continuous_animation] Does the loading indicator animate continuously during active operations? [Metric type: Hygiene] [Weight: 3]

2.  [ID: loading_indicator_no_drop_shadows] Do loading indicators stay flat without drop shadows? [Metric type: Hygiene] [Weight: 3]

3.  [ID: loading_indicator_shape_morph_sequence] Is the active indicator composed of a looping shape-morph sequence cycling through seven unique Google Material 3 geometric shapes? [Metric type: Adherence] [Weight: 10]

4.  [ID: loading_indicator_containment] When the loading indicator is positioned over existing content or used for pull-to-refresh interactions, is a visible container present to provide proper contrast against underlying elements? [Metric type: Adherence] [Weight: 5]

5.  [ID: loading_indicator_color_roles] Does the loading indicator use the correct color tokens:
    -   *Uncontained*: Active shape uses the primary color role (`--md-sys-color-primary`).
    -   *Contained*: Active shape uses the on-primary-container color role (`--md-sys-color-on-primary-container`) on a container fill? [Metric type: Adherence] [Weight: 5]

6.  [ID: loading_indicator_non_gesture_refresh] If the loading indicator is used as part of a pull-to-refresh interaction, is there a clear non-gesture refresh alternative (e.g., a "Refresh" menu item or button) available for accessibility? [Metric type: Adherence] [Weight: 5]
