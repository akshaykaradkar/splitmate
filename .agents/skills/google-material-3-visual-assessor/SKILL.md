---
name: google-material-3-visual-assessor
description: >-
  Use this skill whenever you need to visually inspect a UI (screenshot, image
  URL, or webpage), inspect UI code (HTML, CSS, web components, or design tokens),
  or perform a hybrid visual + codebase audit to evaluate Google Material 3
  design system compliance or answer specific design inquiries.
---

# Google Material 3 Visual Assessor

This skill is the authoritative standard for deep visual analysis, codebase
styling inspection, and Google Material 3 (M3) adherence audits of UI
screenshots and source code. It is designed to be invoked across any workflow to
perform comprehensive compliance audits or answer targeted design inquiries.

> [!IMPORTANT] **ASSESSMENT & INSPECTION FOCUS:** This skill is focused on
> **assessment, inspection, and compliance reporting**. When evaluating UI
> screenshots or codebases, inspect and report findings or answer the user's
> specific design inquiry. Do not modify or overwrite codebase files during the
> assessment phase unless explicitly requested.

> [!IMPORTANT] **CRITICAL OPERATIONAL REQUIREMENT:** You **MUST NOT** attempt to
> answer any query (whether a targeted design inquiry or a full compliance
> audit) or perform any evaluation without first executing the detection steps
> and reading the corresponding reference files in the `references/` directory:
>
> -   **For Visual Detection:** Read
>     [detection_instruction_v2.md](references/detection_instruction_v2.md) and
>     [valid_components.md](references/valid_components.md).
> -   **For Code & Token Inspection:** Read
>     [code_detection_instruction.md](references/code_detection_instruction.md)
>     and `assets/design-system-bindings.css`.
> -   **For Assessment & Rubrics:** Read
>     [assessment_instruction.md](references/assessment_instruction.md),
>     [rubric.md](references/rubric.md), and ONLY the specific component-level
>     markdown files under `references/rubrics/[component_name].md` for
>     components that were explicitly identified as present in the UI during
>     Step 2 (e.g., if a Button is present, read
>     `references/rubrics/buttons.md`). Do NOT load or read component markdown
>     files for components that are not present.
>
> Skipping these files leads to incorrect formatting and wrong answers. You
> **MUST** load the required reference files in every run (using available file
> or skill resource tools such as `view_file` or `load_skill_resource`).

## Workflow

### Step 1: Access Available Context (Visual, Code, or Both)

Gather and inspect all available UI context provided for the assessment:

1.  **Visual Evidence (UI Screenshot / Webpage)**:
    -   *Attached Image / Screenshot File*: Inspect the provided screenshot file
        or image artifact on disk using `view_file` (or directly from the prompt
        context if already provided).
    -   *Direct Image URL*: Use standard URL fetching tools (e.g.,
        `read_url_content`) to retrieve the image.
    -   *Website URL*: Use browser tools or `browser_subagent` to navigate to
        the webpage and capture a screenshot for assessment.

2.  **Codebase Evidence (Markup, Stylesheets & Tokens)**:
    -   *Markup & Components*: Inspect HTML, JSX, templates, or component source
        files (e.g., standard HTML elements, gBreeze `<md-gb-*>`,
        `@material/web` `<md-*>`, or frontend UI components).
    -   *Stylesheets*: Inspect CSS, SCSS, or style definitions for variables,
        elevation, padding, and layout rules.
    -   *Design Tokens*: Inspect design token specifications or theme
        definitions (e.g., `design_memory.md`, `DESIGN.md`, `tokens.json`, or
        theme CSS).

> [!TIP] **DUAL ASSESSMENT MANDATE:** When **both** visual rendering
> (screenshot/webpage) AND codebase files (HTML/CSS/tokens) are provided or
> accessible, you **MUST inspect and evaluate BOTH**. Correlate visual
> appearance (rendered layout, contrast, colors, geometry) with underlying code
> structure (DOM hierarchy, custom properties like `--md-sys-*`, typography
> scale) for maximum audit accuracy.

### Step 2: Detection & Context Inspection (MANDATORY for All Queries)

Before answering any targeted design question or conducting a full compliance
audit, you **MUST** execute a rigorous detection pass:

1.  **Read Detection Protocol References:**
    -   Read
        [detection_instruction_v2.md](references/detection_instruction_v2.md),
        [valid_components.md](references/valid_components.md), and
        [code_detection_instruction.md](references/code_detection_instruction.md).
    -   Confirm visual tokens, color roles, and component rules in
        [carbon.md](references/carbon.md) and `assets/design-system-bindings.css`.

2.  **Sequential Visual CoT Scan Loop (Screenshot Analysis):**
    -   Perform a dedicated, sequential visual scan across candidate component
        types from [valid_components.md](references/valid_components.md).
    -   Document each component check explicitly in the `thoughtProcess` array
        (e.g., `" - [App bar]: Scanned top header. Found 1 instance."`, `" -
        [Divider]: Scanned list items. Found 2 instances."`).
    -   **Micro-Element & Structural Parity:** Never omit minor, structural, or
        embedded sub-elements (such as `Divider` lines, `Badge` icons, `Chip`
        elements, or embedded `Search bar` inputs) from the detected components
        list.

3.  **Code Inspection & Cross-Verification Loop (When Code Context Is Present):**
    -   If codebase files (HTML, CSS/SCSS, templates, web components, or tokens)
        are present, perform the same sequential CoT scan across the markup and
        stylesheets.
    -   Use code inspection to explicitly **confirm or challenge/refine** what
        was detected visually (e.g., verifying if a visual container is a
        standard `<md-filled-button>`, confirming DOM hierarchy, or auditing CSS
        variables like `--md-sys-color-primary`).
    -   Reconcile the visual and codebase detections into a unified, verified
        list of present components and extract global theme properties before
        moving on to assessment.

### Step 3: Conduct Assessment or Answer Targeted Inquiry

Depending on the user's inquiry and the provided context, execute the
appropriate evaluation step using your verified detection observations:

#### A. Targeted Design Inquiries (Q&A)

If the user asks a specific design or compliance question (e.g., *"Is there a
FAB on this screen?"*, *"Does this button have sufficient contrast?"*, or *"Are
our CSS variables mapped to M3 color tokens?"*):

1.  **Guideline & Rubric Consultation:** After completing the detection pass in
    Step 2, consult the specific guideline in
    [guidelines/](references/guidelines/) or the relevant section in the
    foundational [rubric.md](references/rubric.md) or component-level rubric
    files corresponding to the inquiry.
2.  **Provide Reasoned Answer:** Provide a direct, clear, and well-reasoned
    answer addressing that specific inquiry using verified observations from the
    detection pass, visual rendering, code, or both.

#### B. Comprehensive Google Material 3 Compliance Audits

If asked for a full compliance audit, grade, or report for a UI, execute the
complete audit process:

1.  **Analysis & Evaluation (MANDATORY Two-Pass Model):**
    -   **Pass 1 (Foundational Screen-Level Evaluation):** Read
        [rubric.md](references/rubric.md) and evaluate all foundational criteria
        across Typography, Color, Layout, Elevation, and Accessibility.
    -   **Pass 2 (Component-Level Evaluation):** For each component identified
        in Step 2, read its specific rubric file under
        `references/rubrics/[component_name].md` and evaluate its criteria. Do
        NOT load component rubrics for absent components.

2.  **Audit & Reporting:**
    -   **Score Calculation & Rubric Completeness Verification:**
        -   **In ADK Environments (Function Calling):** If the
            `calculate_m3_score` tool is provided in your toolset, you **MUST**
            call `calculate_m3_score(rubric={...}, components=[...])` to verify
            rubric completeness and calculate the official compliance scores.
        -   **In CLI/Terminal Environments (e.g. Jetski):** If command execution
            tools are available, execute the bundled scoring script:

            ```bash
            python3 ux/gdp/ai/design_systems/google-material-3/skills/visual-assessor/scripts/calculate_score.py
            ```

            The script dynamically inspects the markdown rubrics and:
            *   Validates that all expected foundational and component criteria have
                been evaluated.
            *   Issues actionable warnings if any questions were missed so you can
                backfill them.
            *   Deterministically computes the UI Hygiene, M3 Adherence,
                subcategory, and final compliance scores. *(Note: In automated
                pipelines, host callers can also execute this calculation
                directly).*
    -   If specific output formatting instructions or structured schemas are
        provided, bypass conversational filler and return the response adhering
        strictly to the requested schema.
