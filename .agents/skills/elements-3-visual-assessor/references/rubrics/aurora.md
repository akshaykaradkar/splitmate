# Aurora (AI Gradients & Glows)

1.  [ID: aurora_energy_max_one] If background gradient glows are present,
    does the screen contain a maximum of ONE active gradient glow at any given time?
    [Metric type: Adherence] [Weight: 10]

2.  [ID: aurora_energy_subtle] Does the gradient glow or fill treatment appear
    subtle and balanced, without visually overwhelming or dominating the screen
    experience? [Metric type: Hygiene] [Weight: 5]

3.  [ID: aurora_energy_signal_ai] Is the gradient glow or fill effect applied
    exclusively to signal active AI capability and assist the user's
    understanding that AI is present? [Metric type: Adherence] [Weight: 10]

4.  [ID: aurora_glow_ellipse_opacity] Does the background gradient glow ellipse
    use the recommended opacity values (AI analog variant at <= 60% opacity, and
    AI complement variant at <= 45% opacity) with a strong blur effect?
    [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]

5.  [ID: aurora_glow_permitted_backgrounds] Is the background gradient glow applied
    ONLY over supported backgrounds (Surface, Surface Container Lowest, or Surface
    Container Low)? [Metric type: Adherence] [Weight: 5]

6.  [ID: aurora_muted_gradient_fill_order] If a muted gradient fill is applied to a
    button or ghost progress indicator, does it blend tones in the correct order
    (AI main variant -> AI analog variant -> AI complement variant)?
    [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]

7.  [ID: aurora_rich_gradient_text_decorative] If a rich gradient fill is applied to a
    headline, is its application restricted ONLY to decorative greeting headlines
    (e.g., "Good morning") rather than functional labels or body copy?
    [Metric type: Adherence] [Weight: 10]

8.  [ID: aurora_multiple_outputs_no_glow] If the screen contains multiple AI outputs or cards,
    are background gradient glows avoided in favor of standard iconography, content, and
    elevation? [Metric type: Adherence] [Weight: 8]

9.  [ID: aurora_text_icon_contrast] Does text and icon content layered over or
    adjacent to an active gradient surface or glow maintain a minimum 4.5:1 contrast
    legibility without being obscured? [Metric type: Hygiene] [Weight: 5]

10. [ID: aurora_media_content_clear] Does media content (such as photos, videos,
    or illustrations) remain clear and unobscured by overlapping gradient flows?
    [Metric type: Hygiene] [Weight: 5]
