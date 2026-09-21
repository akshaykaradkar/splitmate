# Android Motion Visual Assessor Skill

This directory contains the AI agent skill instructions (`SKILL.md`), rubrics,
and reference guidelines for the **Android Motion Visual Assessor (AMVA)**—an agent
capability that audits screenshots, user interfaces, and source code for compliance with
the Android Motion design system guidelines and specifications.

--------------------------------------------------------------------------------

## 1. Official Skill Evaluation (Everest & Plx Dashboard)

Authoritative, end-to-end evaluation and regression scoring of the Android Motion
Visual Assessor skill is conducted using **Everest** (Google's agent evaluation
platform), not via local standalone `evalin` suites.

*   **Golden Evals Dashboard:** You can monitor official compliance scores,
    accuracy trends, and regression results across our golden screenshot suite
    on the
    **[UX Android Motion UI Assessor Golden Evals Plx Dashboard](https://dashboards.corp.google.com/view/ux_android_motion_ui_assessor_eval_goldens)**.
*   **Full Agent Evals Guide:** For instructions on configuring, triggering, and
    debugging official Everest evaluation runs, see the
    **Running Agent Evals g3doc**.
*   **Evaluation Framework Overview:** See
    **AI Evals for Android Motion Visual Assessor**
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
    evalin run ux/gdp/ai/design_systems/android-motion/skills/visual-assessor/sample_evals/EVAL.txtpb --skills=ux/gdp/ai/design_systems/android-motion/skills/visual-assessor
    ```

--------------------------------------------------------------------------------

## 3. Directory Structure & Architecture Notes

*   **`SKILL.md`:** Runtime instructions injected into the AI agent's context
    window when the skill is triggered. **Do not** add developer maintenance or
    evaluation workflow notes here, as doing so wastes context tokens and can
    distract the model during live UI audits.
*   **`references/`:** Specialized Android Motion component rubrics, guidelines, and
    scoring rules referenced on demand by the agent during Phase 2 component
    evaluation.
*   **`detector/`:** Sub-skill (`ui-detector`) responsible for Phase 1 UI
    component segmentation and bounding-box localization.
