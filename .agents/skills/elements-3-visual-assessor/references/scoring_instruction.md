<!-- disableFinding(G3DOC_JINJA) -->

# Scoring Instruction for Elements GM3

This instruction defines how to calculate the overall Elements GM3 Design System
compliance assessment score and subcategory scores using the
**Dual-Metric Governance Model**.

--------------------------------------------------------------------------------

## 1. Dual-Metric Governance Model

To prevent compliance inflation (where generic usability hygiene masks a lack of
system-specific adoption), the rubric divides all evaluated criteria into two
mutually exclusive buckets based on their `[Metric type: ...]` tag:

- **UI Hygiene (Usability & Polish):** Evaluates general visual polish, grid
  alignment, contrast, spacing, legibility, and usability
  (`[Metric type: Hygiene]`).
- **Elements GM3 Adherence (System Specs):** Evaluates strict compliance with
  official Elements GM3 tokens, component geometry, typography families (Google
  Sans, Google Sans Text, Google Symbols), and color roles (`[Metric type: Adherence]`).

--------------------------------------------------------------------------------

## 2. 4-Tier Severity Framework

Criteria carry assigned point weights $w_i$ based on their visual and structural
importance:

- **Tier 0 (Brand Signature Anchors — Weight = 10):** Non-negotiable brand
  identity pillars and system anchor rules (e.g., sentence case capitalization,
  surface color role separation, standard leading icons, active navigation
  pill containers, primary action reservation).
- **Tier 1 (Critical Component & Layout Rules — Weight = 5):** High-impact layout
  constraints, core accessibility, dialog scrims, and visual hierarchy (e.g.,
  pane counts, hardware clipping, whitespace division, text legibility over
  imagery, prominent actions).
- **Tier 2 (Key Elements GM3 Patterns & Component Rules — Weight = 3):** Standard
  component geometry, layout patterns, and usability configurations (e.g.,
  button variant hierarchy, badge shapes, menu elevation, subtitle lines,
  typescale typography, granular color roles).
- **Tier 3 (Visual Polish — Weight = 1):** Minor formatting, casing, and
  alignment polish (e.g., visual alignment, divider stroke thickness,
  icon/label spacing).

--------------------------------------------------------------------------------

## 3. Criteria Filtering & Exclusions

For each criterion in `criteriaStatus`:

- **Scored Criteria:** Only criteria where `answer` is `"Yes"` (or `met: true`)
  or `"No"` (or `met: false`) are scored.
- **Excluded Criteria:** Ignore any criteria where `answer` is `"N/A"` or a HEX
  color value. Excluded criteria contribute 0 to both the numerator (earned
  points) and denominator (possible points).

--------------------------------------------------------------------------------

## 4. The Adherence Multiplier Formula

Rather than enforcing arbitrary binary pass/fail gates, the framework calculates
a single continuous percentage score (0–100%) using the Adherence Multiplier.
The UI Hygiene score serves as the baseline quality metric, scaled directly by
the Elements GM3 Adherence rate.

### Step 1: UI Hygiene Score (%)
$$\text{UI Hygiene Score (\%)} = \left( \frac{\sum_{\text{Passed Hygiene}}
w_i}{\sum_{\text{Evaluated Hygiene}} w_i} \right) \times 100$$

* If no Hygiene criteria are evaluated (denominator = 0), `hygieneScore`
  defaults to 0% (or `"N/A"`).

### Step 2: Elements GM3 Adherence Score (%)
$$\text{Elements GM3 Adherence Score (\%)} = \left(
\frac{\sum_{\text{Passed Adherence}} w_i}{\sum_{\text{Evaluated Adherence}} w_i}
\right) \times 100$$

* If no Adherence criteria are evaluated (denominator = 0), `adherenceScore`
  defaults to 100% (1.0 multiplier), so the Final Compliance Score equals the UI
  Hygiene Score.

### Step 3: Final Compliance Score (%)
$$\text{Final Compliance Score (\%)} = \text{UI Hygiene Score (\%)} \times
\left( \frac{\text{Elements GM3 Adherence Score (\%)}}{100} \right)$$

* Round all percentage scores (`hygieneScore`, `adherenceScore`, `score`) to the
  nearest whole integer.

--------------------------------------------------------------------------------

## 5. Thresholds & Interpretation Ranges

- **85% – 100% (Production Ready / PASSED):** High UI polish combined with
  strict Elements GM3 adoption.
- **50% – 84% (In Transition / NEEDS_REFINEMENT):** Clean layout and good
  hygiene, but contains custom styling, legacy components, or missing brand
  styles requiring remediation.
- **0% – 49% (Non-Elements / Custom App / NEEDS_REFINEMENT):** The application
  operates primarily on custom branding or non-standard components, resulting
  in low system adoption.

--------------------------------------------------------------------------------

## 6. Subcategory Score Calculation

Individual subcategory scores in the JSON compliance report (`appBarScore`,
`colorScore`, `fabScore`, `typographyScore`, `navigationBarScore`,
`layoutScore`, `bottomSheetScore`, `buttonScore`, `auroraScore`) are
calculated using the exact same weighted formula applied exclusively to
criteria matching the respective category prefix:

| Subcategory Score Field | Criteria Prefix Filter | Applicable Sources |
| :--- | :--- | :--- |
| **`typographyScore`** | `[Typography]` | Foundational `rubric.md` |
| **`colorScore`** | `[Color]` | Foundational `rubric.md` |
| **`layoutScore`** | `[Layout]` | Foundational `rubric.md` |
| **`appBarScore`** | `[App bars]` or `[App bar]` | `rubrics/app-bars.md` |
| **`buttonScore`** | `[Buttons]` or `[Button]` | `rubrics/buttons.md` |
| **`fabScore`** | `[FABs]` or `[FAB]` | `rubrics/fabs.md` |
| **`navigationBarScore`** | `[Nav Bars]` | `rubrics/navigation-bars.md` |
| **`bottomSheetScore`** | `[Bottom Sheet]` | `rubrics/bottom-sheet.md` |
| **`auroraScore`** | `[Aurora]` | `rubrics/aurora.md` |

--------------------------------------------------------------------------------

## 7. Zero-Denominator & Absent Component Handling

If a component was **not present** on the screen (or if all evaluated criteria
in that subcategory returned `"N/A"`), the possible points (denominator) for
that subcategory is 0.

- **Required Value:** The corresponding subcategory score field in the JSON
  output **MUST be `"N/A"`** (e.g., `"bottomSheetScore": "N/A"`).
- **Prohibited Value:** Do NOT output `0` or `0%` for absent subcategories, as
  `0` indicates total compliance failure rather than component absence.
