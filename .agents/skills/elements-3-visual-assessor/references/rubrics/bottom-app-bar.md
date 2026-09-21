# Bottom App Bar

1.  [ID: bottom_app_bar_container_shape] Does the bottom app bar or docked toolbar
    either stretch full-width (docked) or sit as a floating bar with straight or fully rounded corners
    according to layout constraints? [Metric type: Hygiene] [Weight: 3]

2.  [ID: bottom_app_bar_button_target_size] Do action buttons inside the bottom app bar or toolbar
    consistently meet the Elements GM3 **48x48dp** interactive touch target size? [Metric type: Adherence]
    [Weight: 5]

3.  [ID: bottom_app_bar_floating_drop_shadow] If a floating bottom bar or floating pill is used,
    does it use standard Elements GM3 elevation or shadow tokens to cleanly distinguish it from the
    scrolling background content? [Metric type: Hygiene] [Weight: 3]

4.  [ID: bottom_app_bar_simultaneous_nav] Are bottom app bars and bottom navigation/rail layouts
    avoided from appearing simultaneously on the same viewport to prevent visual clutter?
    [Metric type: Hygiene] [Weight: 5]
