# Date Picker

1.  [ID: date_picker_paired_input] Is the single date picker paired with an outlined text entry field, where the calendar icon acts as the exclusive entry point to open the calendar dropdown? [Metric type: Adherence] [Weight: 3]

2.  [ID: date_picker_touch_targets_48dp] Do all interactive elements inside the calendar view (previous/next arrows, month/year dropdown listboxes, grid dates, and action buttons) meet the minimum 48x48dp interactive target size requirement? [Metric type: Adherence] [Weight: 5]

3.  [ID: date_picker_no_input_mask] Does the text field avoid using an input mask (i.e. does not dynamically add slashes or special characters while the user is typing)? [Metric type: Adherence] [Weight: 3]

4.  [ID: date_picker_range_interim_solution] If the component is used to select a date range, does it use two single date pickers with separate entry points (stacked or side-by-side) as the interim accessible solution instead of a single range picker? [Metric type: Adherence] [Weight: 3]

5.  [ID: date_picker_selected_day_circle] Is the active selected day in the grid represented as a solid filled circle using CEE3 primary color tokens? [Metric type: Hygiene] [Weight: 3]

6.  [ID: date_picker_outside_dates_interactive] Are dates outside the current month visible and interactive to mouse clicks? [Metric type: Hygiene] [Weight: 1]

7.  [ID: date_picker_action_buttons] Does the calendar dropdown include explicit "OK" and "Cancel" buttons aligned to the trailing bottom edge? [Metric type: Hygiene] [Weight: 3]
