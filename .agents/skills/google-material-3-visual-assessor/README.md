# Google Material 3 Visual Assessor Skill

This directory contains the AI agent skill instructions (`SKILL.md`), rubrics,
and reference guidelines for the **Google Material 3 Visual Assessor (MVA)**—an agent
capability that audits screenshots and user interfaces for compliance with
Google Material 3 (M3) design tokens and system guidelines.

--------------------------------------------------------------------------------

## 1. Official Skill Evaluation (Everest & Plx Dashboard)

Authoritative, end-to-end evaluation and regression scoring of the Google Material 3
Visual Assessor skill is conducted using **Everest** (Google's agent evaluation
platform), not via local standalone `evalin` suites.

*   **Golden Evals Dashboard:** You can monitor official compliance scores,
    accuracy trends, and regression results across our golden screenshot suite
    on the
    **[UX Material UI Assessor Golden Evals Plx Dashboard](https://dashboards.corp.google.com/view/ux_material_ui_assessor_eval_goldens)**.
*   **Full Agent Evals Guide:** For instructions on configuring, triggering, and
    debugging official Everest evaluation runs, see the
    **Running Agent Evals g3doc**.
*   **Evaluation Framework Overview:** See
    **AI Evals for Material Visual Assessor**
    for architecture and design details.

--------------------------------------------------------------------------------

## 2. Local Developer Sample Evals (`sample_evals/`)

The file `sample_evals/EVAL.txtpb` is maintained
**exclusively for developers** making modifications to this skill to perform
quick local sanity checks before mailing or submitting a changelist (CL).

*   **Purpose:** Validates that edits to `SKILL.md` or reference guidelines do
    not break basic component recognition or reasoning formatting.
*   **Not Official Scoring:** These sample tests are lightweight and **do not**
    contribute to official eval scoring or benchmark dashboards.
*   **Running a Gut Check:**

    ```bash
    evalin run ux/gdp/ai/design_systems/google-material-3/skills/visual-assessor/sample_evals/EVAL.txtpb --skills=ux/gdp/ai/design_systems/google-material-3/skills/visual-assessor
    ```

--------------------------------------------------------------------------------

## 3. Directory Structure & Architecture Notes

*   **`SKILL.md`:** Runtime instructions injected into the AI agent's context
    window when the skill is triggered. **Do not** add developer maintenance or
    evaluation workflow notes here, as doing so wastes context tokens and can
    distract the model during live UI audits.
*   **`references/`:** Specialized Google Material 3 component rubrics, guidelines, and
    scoring rules referenced on demand by the agent during Phase 2 component
    evaluation.
*   **`detector/`:** Sub-skill (`ui-detector`) responsible for Phase 1 UI
    component segmentation and bounding-box localization.
*   **`sample_evals/`:** Sample textproto eval suite for local pre-submit
    developer verification.
