---
name: ui-detector
description: >-
  Identifies UI components, buttons, layout regions, and containers in a
  screenshot based on user intent. Utilizes Chain-of-Thought (CoT) iterative
  component-by-component verification to ensure absolute precision and
  exhaustive auditing, providing verified component identification and global
  theme extraction as a structural baseline.
---

# UI Detector

This skill identifies functional and structural UI components within a
screenshot. It accesses a UI (typically via a screenshot) and identifies
components on the page using a meticulous, iterative verification process. The
primary goal of this skill is component identification and global theme
extraction; whether a component is Android Motion adherent is not relevant for this
skill. It serves as the foundational detection step for any Android Motion Design
System audit.

## Workflow

### Step 1: Read Reference Documentation

Before viewing the image, read the core rules and valid components:

1.  Read
    [detection_instruction_v2.md](../references/detection_instruction_v2.md) for
    layout and scanning principles.
2.  Check the valid components list in
    [valid_components.md](../references/valid_components.md).

### Step 2: Access the Image and Determine Intent & Scope

1.  Ensure you have access to the attached screenshot or retrieve it via
    URL/browser tools.
2.  Analyze the user's prompt to determine the required scope of UI detection:
    -   **Exhaustive / Full Audit Intent**: If the user asks a broad or general
        question (e.g., "what components are on the screen?"), your target scope
        comprises ALL valid component strings listed in
        [valid_components.md](../references/valid_components.md).
    -   **Targeted / Specific Component Intent**: If the user asks about
        specific components (e.g., "Is there a FAB?"), your target scope is
        restricted ONLY to those specific component types.

### Step 3: Mandatory Guideline Consultation (CRITICAL)

Now that you have viewed the screenshot and identified candidate component types
you suspect are present:

1.  **List guidelines:** Run a `list_dir` on `../references/guidelines/`.
2.  **Read Guideline Files:** For *every single* component type you suspect is
    present on the screen, you **MUST** call `view_file` to read its
    corresponding guideline file (e.g., `../references/guidelines/fabs.md` if
    you suspect a FAB) to verify its visual heuristics.
3.  Do NOT rely on default model knowledge; the heuristics in these guidelines
    are authoritative for this audit.

### Step 4: Iterative Component-by-Component Verification (CoT Checklist)

When conducting UI detection, you MUST avoid attempting to find all components
at once in a single unorganized scan. Instead, you MUST perform a dedicated,
sequential visual audit for each component type within your determined scope.

In your `thoughtProcess`, BEFORE generating the final `components` JSON array:

1.  **Iterative Checklist Loop:** Iterate through the target component types one
    by one. For each component type, perform an isolated visual scan across the
    entire screenshot specifically looking for instances of that single
    component type, applying the specific heuristics from the guidelines you
    read in Step 3.
2.  **Document Verification Explicitly:** You MUST write out the status of each
    component check explicitly in your `thoughtProcess` array. For example:

    ```json
    "thoughtProcess": [
      "Step 1: Read reference docs...",
      "Step 2: Accessed screenshot and determined intent is Exhaustive...",
      "Step 3: Consulted guideline files for suspected components.",
      "Step 4: Sequential Component Verification Checklist:",
      " - [App bar]: Scanned header region. Found 1 instance.",
      " - [Avatar]: Scanned entire screen. Found 0 instances.",
      " - [Badge]: Scanned icons and tabs. Found 0 instances.",
      " - [Button]: Scanned bottom action area. Found 0 instances.",
      " - [Card]: Scanned main scrollable area. Found 0 instances.",
      " - [Icon button]: Scanned header. Found 3 instances.",
      " - ... (continuing for all valid component types)"
    ]
    ```

3.  **Android Motion Adherence Independence:** Remember that the sole objective of
    this skill is identifying components. Whether a component is Android Motion
    adherent or not is completely irrelevant for this detection skill.

### Step 5: Global Theme & Motion Token Extraction

In addition to component identification, you MUST extract the overall page-level
theme and motion characteristics to populate the `theme` object in the final JSON:

1.  **Palette:** Use the `color` skill to extract the page-level color scheme from the screenshot:

    ```bash
    blaze run //java/com/google/ux/gdp/ai/design_systems/android_motion/skills/color:color -- \
      --method=extract_and_generate_brand_color_scheme \
      --image_path="<path_to_screenshot>"
    ```

    Use the result's `named_colors` map (converting snake_case keys to camelCase
    keys) to populate the `primaryColor`, `secondaryColor`, `backgroundColor`,
    `surfaceColor`, `onSurface`, and `onSurfaceVariant` fields in the `theme`
    object, referencing `--droid-sys-color-*` specifications.

2.  **Type Scale:** The list of font families and weights used (e.g. "Google Sans-Regular", "Google Sans Text-Medium").

3.  **Motion System (if code/spec is available):** Extract motion easing and duration classes (e.g. `--droid-sys-motion-easing-emphasized`, `--droid-sys-motion-duration-250`).

### Step 6: Compile Components

Consolidate all identified instances from your iterative verification checklist
into a clean JSON object containing the `theme` and `components` list. Ensure
there are no duplicate entries and that each component aligns with the schema
defined below in the Output Format section.

**CRITICAL RULE - MANDATORY MICRO-COMPONENT & STRUCTURAL INCLUSION (ABSOLUTE
PARITY):** Do NOT omit any identified component from your final JSON
`components` array out of a belief that it is merely a minor, decorative, or
structural sub-element of a larger container. In Android Motion design audits, every
single verified structural and visual element—regardless of how small or
seemingly secondary (such as a `Divider` line within a list item, a `Badge` on
an icon, a `Chip`, or an embedded `Search bar`)—is an independent, first-class
entity requiring strict evaluation parity. If your `thoughtProcess` reasoning
confirms the identification of ANY component instance (e.g., stating "Identified
1 vertical divider"), you MUST explicitly output a dedicated JSON component
object for it in the `components` array. Bypassing or filtering out smaller
structural elements after acknowledging them in your thought process causes
critical evaluation failures.

### Step 7: Color, Layout & Motion Intent Placeholder

If the user's question pertains to color, layout, or motion/animation details that are not related to
specific components:

*   **[PLACEHOLDER: Non-Component Color, Layout & Motion Analysis]**
*   *Do nothing for now.* Output an empty or baseline structure indicating that
    non-component color/layout/motion evaluation is pending future implementation.

### Output Format (JSON only)

Return the compiled results as a clean JSON object adhering strictly to the
following schema:

```json
{
  "thoughtProcess": [
    "Step-by-step reasoning documenting the iterative verification checklist..."
  ],
  "theme": {
    "primaryColor": "hex",
    "secondaryColor": "hex",
    "backgroundColor": "hex",
    "surfaceColor": "hex",
    "onSurface": "hex",
    "onSurfaceVariant": "hex",
    "typeScale": ["font1", "font2"]
  },
  "components": [
    {
      "name": "string (semantic name e.g. 'Save Button')",
      "componentType": "standard" | "variation" | "platform" | "custom",
      "mappedComponent":
        "string (Exact match from valid_components list or 'Custom component')",
      "category": "Container" | "Buttons" | "Pickers" | "Loading & Progress" |
        "Navigation" | "Sheets" | "Other" | "Content" |
        "Layout & Organization" | "Menus & Actions" | "Navigation & Search" |
        "Presentation" | "Selection & Input" | "Status" |
        "System Experiences" | "Action Components" |
        "Communication Components" | "Containment Components" |
        "Navigation Components" | "Selection Components" |
        "Text Input Components" | "Aurora Components" | "Aurora" |
        "Primitives" | "Custom" | "Other"
    }
  ]
}
```
