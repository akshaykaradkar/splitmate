# Card

1.  [ID: card_purpose] Are cards used to display content and actions about a
    single subject in a flexible container? [Metric type: Hygiene] [Weight: 5]

2.  [ID: card_container_rounded_corners] If a Card component (Elevated, Filled,
    or Outlined) is present, does its container comply with the specification:
    corner radius of 12px / 12dp (sys.shape.corner.medium / --md-sys-shape-corner-medium)
    applied to all four corners? [Metric type: Adherence] [Weight: 10]

3.  [ID: card_text_image_scrim] Is text layered over images only if there is a
    scrim or protection (scrim role sys.color.scrim / --md-sys-color-scrim) guaranteeing legibility? [Metric type: Hygiene] [Weight: 5]

4.  [ID: card_elevated_drop_shadow] Do elevated cards use a drop shadow for
    elevation (using shadow color sys.color.shadow / --md-sys-color-shadow and elevation level --md-sys-elevation-level1 or higher)? [Metric type: Hygiene] [Weight: 3]
