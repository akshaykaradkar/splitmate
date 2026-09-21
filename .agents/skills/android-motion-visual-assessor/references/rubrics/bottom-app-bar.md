# Bottom App Bar

1.  [ID: bottom_app_bar_container_shape] Does the bottom app bar container comply with the specification: either stretching full-width with sharp corners (`--droid-sys-shape-corner-none`) or sitting as a floating pill-shaped container with fully rounded corners (`--droid-sys-shape-corner-full`)? [Metric type: Adherence] [Weight: 3]

2.  [ID: bottom_app_bar_button_target_size] Do action buttons inside the bottom app bar appear large enough to touch easily (rendering at 24x24dp inside a minimum `48x48dp` interactive bounding touch target)? [Metric type: Hygiene] [Weight: 3]

3.  [ID: bottom_app_bar_floating_drop_shadow] If a floating bottom app bar is present, does it utilize a prominent drop shadow to elevate it from the background, corresponding to `--droid-sys-elevation-level3` (6px) or `--droid-sys-elevation-level4` (8px)? [Metric type: Adherence] [Weight: 3]

4.  [ID: bottom_app_bar_simultaneous_nav] Are bottom app bars and bottom navigation bars avoided from appearing simultaneously on the same screen viewport? [Metric type: Hygiene] [Weight: 5]

5.  [ID: bottom_app_bar_container_height] Does the bottom app bar container height comply with the standard 64dp specification? [Metric type: Adherence] [Weight: 5]

6.  [ID: bottom_app_bar_container_color] Is the container's background styled with `--droid-sys-color-surface` (light-dark(`#fdfcfb`, `#1f1f1f`)) or `--droid-sys-color-surface-variant` (light-dark(`#e1e3e1`, `#444746`))? [Metric type: Adherence] [Weight: 3]
