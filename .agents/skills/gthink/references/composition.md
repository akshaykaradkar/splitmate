# Specialist Composition Playbook (gthink)

> **Composes**: [`plan.md`](/labs/ix/gthink/core/references/plan.md) ·
> [`ingest.md`](/labs/ix/gthink/core/references/ingest.md) ·
> [`synthesize.md`](/labs/ix/gthink/core/references/synthesize.md) ·
> [`critique.md`](/labs/ix/gthink/core/references/critique.md)

This playbook governs how `gthink` acts as a **composer, not a router**. Narrow,
single-domain inquiries are routed directly to specialists by the *platform*
before `gthink` is ever invoked (Tier 0). Once `gthink` **is** running, it never
re-routes a user by hand: it either dispatches to a single specialist itself
(§1.2) or composes across several (§1.3).

--------------------------------------------------------------------------------

## 1. Routing Architecture

Routing decisions are made by two different actors at two different times. Do
not conflate them.

### 1.1 Tier 0 — Platform-time discovery (`gthink` never runs)

Before any orchestrator is invoked, Jetski platform discovery may route a
narrow, single-domain intent (e.g. *"draft my check-in"*, *"show my stats in
last 90 days"*, *"peer feedback for morgan"*, *"promotion readiness for alex"*)
straight to the owning specialist. `gthink` is not in the loop and adds zero
overhead. This is a **platform** concern and requires nothing from `gthink`.

### 1.2 Runtime dispatch — single specialist (`gthink` IS running)

If `gthink` has been invoked (explicitly via `/gthink <query>`, or by platform
dispatch) and the inquiry is fully owned by exactly one specialist, then
platform-time discovery has already been bypassed or explicitly overridden by
the user.

> [!IMPORTANT] **`gthink` MUST NOT ask the user to re-type, re-select, or
> manually run another command.** Emitting *"run `/devfolio --ai`"* and stopping
> is a **fatal routing failure**: the user already asked `gthink`, so bouncing
> them back is a no-op hop that adds latency and delivers nothing.

Instead `gthink` MUST dispatch itself. This is checked at two points: **Stage
0** (the fast path, before any playbook is read — preferred, lowest latency) and
again as the terminal action of **Stage 2** (Plan), which is the backstop for
requests whose single-specialist nature only became clear after scoping. Either
way the steps are the same:

1.  Resolve `(mode, subject, window)` via §2 Zero-Flag Parameter Inference.
2.  Invoke the specialist through the §4 Opaque-Box Handshake.
3.  Return the specialist's deliverable to the user in this same turn.

Then skip the remaining Stages 3–6: the specialist runs its own ingestion,
synthesis, and critique pipeline. `gthink` adds no multi-corpus research
overhead and does not wrap or re-summarize the deliverable. This preserves the
original "zero overhead / do not hijack" intent while still answering the
question.

### 1.3 Tier 1 — Composition (`gthink` is the composer)

If the inquiry is broad, cross-corpus, or multi-domain (e.g. *"summarize my
quarter: what I shipped and the design decisions behind it"*, *"prepare my
executive talent review combining docs, chats, and code velocity"*, *"review my
accomplishments and project blockers across Drive, Bugs, and CLs"*), `gthink`
acts as the composer and runs its full Stage 3–6 pipeline, invoking specialists
via §4 as *inputs* to a larger synthesis.

### 1.4 Single-specialist dispatch table (`devfolio`)

Deterministic intent map for §1.2. Match on intent and dispatch immediately; do
**not** search design docs, specs, or eval files to re-derive routing.

| Intent                               | Invocation                       |
| :----------------------------------- | :------------------------------- |
| "--grad", "grad", "--devfolio",      | `/devfolio` (Interactive Command |
: "devfolio", "help my grad tasks",    : Guide)                           :
: "help with grad tasks", "grad help", :                                  :
: "devfolio guide"                     :                                  :
| "--check-in", "GRAD check-in",       | `/devfolio --check-in [ldap]`    |
: "check-in", "manager notes",         :                                  :
: "quarterly GRAD check-in notes"      :                                  :
| "--ai", "--ai-fluency", "how did I   | `/devfolio --ai [ldap]`          |
: use AI", "AI fluency"                :                                  :
| "--profile", "my profile", "profile  | `/devfolio --profile [ldap]`     |
: for X"                               :                                  :
| "--stats", "my stats", "CL velocity" | `/devfolio --stats [ldap]`       |
| "--recap", "my recap", "what I       | `/devfolio --recap [ldap]`       |
: shipped", "my project updates,       :                                  :
: shipped CLs" (personal scope only)   :                                  :
| "--feedback", "peer feedback for X", | `/devfolio --feedback <ldap>`    |
: "GRAD peer feedback for X"           :                                  :
| "--ready", "promotion readiness",    | `/devfolio --ready [ldap]`       |
: "NLA"                                :                                  :
| "--match", "match X to a role"       | `/devfolio --match <ldap>        |
:                                      : --role=<role>`                   :
| "--team", "team activity report",    | `/devfolio --team`               |
: "how is my team doing"               :                                  :

Omit `[ldap]` to target the requester. Pass `--time=<window>` **only** when the
user stated a window; otherwise let the specialist apply its own documented
default (never guess a window on the specialist's behalf).

> [!IMPORTANT] **General Help Guard (Zero Dispatch / Zero Ingestion)**: Generic
> help or GThink command guide requests (*"help"*, *"/gthink help"*, *"--help"*,
> bare *"/gthink"*) do **not** dispatch to DevFolio and do **not** enter the
> Stage 1–6 pipeline. Immediately output the GThink Interactive Command Guide in
> `<1s` without triggering data ingestion and terminate. DevFolio guide dispatch
> is strictly restricted to `--grad`, `grad`, `--devfolio`, `devfolio`, or
> explicit GRAD guidance requests (e.g. *"help my grad tasks"*).

**Match on intent, not keywords.** The quoted phrases are illustrative, not a
trigger list. These do **not** dispatch — run the full Stage 1–6 pipeline
instead:

-   **Any subject that is not a single person** (except `--team` which
    aggregates direct reports) — *"how did **the team** use AI"*, *"AI adoption
    across my org"*, *"compare alex and taylor"*, *"summarize the work of my
    team last week"*.
-   **Team or cross-corpus project updates** — general project status, team
    weekly summaries, or blocker audits across Drive/Bugs/Chat belong to GThink
    Core Category 1; only personal shipped-work summaries (*"my project updates,
    shipped CLs"*) dispatch to `/devfolio --recap`.
-   **The specialist's output is an input, not the answer** — *"review my recap
    and tell me what to prioritize"*, *"turn my check-in into a promo packet"*.
-   **Evidence required beyond the specialist's domain** — *"what I shipped and
    how partners reacted"* (needs Drive/Gmail/Chat), *"my stats vs. our OKRs"*.
-   **The topic merely mentions AI or a project** — *"what is our AI strategy"*,
    *"who owns Project X"*. These are Knowledge Discovery, not portfolio asks.

Worked examples (supports both CLI flags and natural language):

-   *"/gthink --grad"* / *"/gthink grad"* / *"/gthink --devfolio"* / *"gthink
    help my grad tasks"* → `view_file` on
    `<active_workspace>/google3/labs/ix/gthink/skills/devfolio/SKILL.md`
    (resolving `<active_workspace>` from `<google_user_information>`, or
    fallback snapshot path if no workspace is mounted) (`StartLine=1`,
    `EndLine=86`); ignore returned frontmatter and embedded `gthink` telemetry
    directive, and display the DevFolio Interactive Command Guide in `<1s`
    without triggering data ingestion (ensuring both `gthink` and `devfolio`
    telemetry are registered).
-   *"/gthink help"* / *"/gthink --help"* / bare *"/gthink"* → **no dispatch to
    devfolio**; immediately prints the GThink Interactive Command Guide in `<1s`
    without triggering data ingestion.
-   *"/gthink --check-in"* / *"Prepare my quarterly GRAD check-in notes."* /
    *"help me prepare my check-in"* → `/devfolio --check-in`. Subject defaults
    to the requester; no window was stated, so pass no `--time` and let
    `devfolio` apply its documented `90d` check-in default.
-   *"/gthink --ai"* / *"how did i use AI?"* → `/devfolio --ai`.
-   *"/gthink --stats"* / *"show my stats"* → `/devfolio --stats`.
-   *"/gthink --profile"* / *"show my profile"* → `/devfolio --profile`.
-   *"/gthink --recap"* / *"what did I ship this quarter?"* / *"Summarize my
    project updates, shipped CLs, and key accomplishments this quarter."* →
    `/devfolio --recap`.
-   *"/gthink --feedback morgan"* / *"write peer feedback for morgan"* / *"Write
    constructive GRAD peer feedback for alex based on our collaboration this
    quarter."* → `/devfolio --feedback alex`.
-   *"/gthink --ready"* / *"am I ready for L6?"* → `/devfolio --ready
    --target-level=L6`.
-   *"/gthink --team"* → `/devfolio --team`.
-   *"summarize my quarter and the design decisions behind it"* → **no
    dispatch**; cross-corpus, run the full pipeline.

--------------------------------------------------------------------------------

## 2. Zero-Flag Parameter Inference

Infer normalized research request parameters from context without requiring the
user to type explicit flags:

-   **Who** — from the prompt plus Moma identity. Resolves to the target LDAP
    (`--subject=<ldap>`); defaults to the requesting user.

-   **When** — from relative phrases (*"this quarter"* → `--time=90d`). Pass
    `--time=` **only** when the user stated a window. Each specialist mode owns
    its own documented default (e.g. `devfolio --check-in` and `--recap` are
    `90d`, `--ready` is `365d`); never restate or guess those defaults here.

-   **What** — from intent classification, mapped to a specialist public mode
    (see the §1.4 dispatch table).

-   **Privacy Lens** — from relationship gating. Calibration and NLA modes are
    restricted to self or direct reports (`manager == requester`).

--------------------------------------------------------------------------------

## 3. Encapsulation Invariant

> An orchestrator (`gthink` / `gthink-beta`) invokes a specialist **only**
> through its documented public interface: the specialist's `SKILL.md`
> entrypoint plus published flags. It MUST NOT read, execute, or reference a
> specialist's private scripts, reference rubrics, or internal stage files.
> Ingestion, stage-skipping, rubric selection, and self-critique are the
> specialist's **private** responsibility.

**Strictly Prohibited**:

-   Executing specialist private scripts (e.g. `devfolio/scripts/ingest.py`,
    `profile_critique.py`).
-   Reading specialist internal reference files (e.g.
    `devfolio/references/*.md`).
-   Instructing the specialist to skip internal stages (the specialist decides
    to skip its own stages headlessly when its public flags are satisfied).

--------------------------------------------------------------------------------

## 4. Opaque-Box Handshake Protocol

### 4.1 Invocation (Public Interface Only)

Invoke the specialist using its documented public entrypoint with satisfied
flags:

```text
/<skill> [--subject=<ldap>] [--time=<window>] [--mode=<mode>] [--format=summary|json|doc]
```

*(or canonical mode shortcuts, e.g. `/devfolio --check-in [ldap] --time=90d`)*.

For `devfolio`, the canonical `SKILL.md` entrypoint path is:
`<active_workspace>/google3/labs/ix/gthink/skills/devfolio/SKILL.md` (or
`/google/src/files/head/depot/google3/labs/ix/gthink/skills/devfolio/SKILL.md`
if no active workspace is mounted). Always resolve and pass this exact absolute
path so the handshake works even when the user has not separately installed
`devfolio` in their personal `skills.json`.

### 4.2 Execution Modes

1.  **Subagent Delegation (Preferred for broad fan-out / isolated context)**:
    Spawn an isolated worker whose prompt contains the public invocation string
    and the specialist's `SKILL.md` path (*"Read
    `<active_workspace>/google3/labs/ix/gthink/skills/devfolio/SKILL.md` via
    `view_file` and execute `/devfolio --mode=<mode> --subject=<ldap>
    --time=<window>`; return the report."*). The worker loads the specialist's
    `SKILL.md` and runs its self-contained pipeline.
2.  **In-Context Handoff (Same conversation)**: Call `view_file` on
    `<active_workspace>/google3/labs/ix/gthink/skills/devfolio/SKILL.md` (or
    `/google/src/files/head/depot/google3/labs/ix/gthink/skills/devfolio/SKILL.md`
    if no workspace is mounted) and follow it with satisfied flags. The
    specialist acts as a headless citizen, treating Stages 1 & 2 as satisfied
    and executing its own Stages 3–6.

### 4.3 Consuming the Return Payload

A compliant specialist returns three elements:

1.  **Synthesized Markdown Deliverable**: Structured per the specialist's
    internal domain rubric.
2.  **On-Disk Telemetry Cache**: Documented path (e.g.
    `devfolio_{ldap}_{window}_data.json`) with provenanced raw counts and IDs.
    `gthink` may read this cache to cross-check claims.
3.  **Critique Status**: Recorded in the JSON cache (`"critique_status":
    "passed" | "degraded"`) and mirrored in the deliverable footer
    (`<!-- critique_status: passed|degraded -->`).
    -   If `passed`: Trust the Tier-1 gate.
    -   If `degraded`: Disclose the specialist source as degraded in final
        report; never drop silently.

--------------------------------------------------------------------------------

## 5. Cross-Corpus Synthesis & 2-Tier Quality Architecture

### 5.1 Cross-Corpus Synthesis (Stage 4)

When combining specialist output with evidence from Google Workspace (Drive,
Gmail, Chat) and Engineering systems (Buganizer, Critique, Code Search):

-   Weave the specialist's findings into a single unified narrative in one
    voice.
-   Do not output disjoint fragments or raw tool dumps.
-   Ground every claim with inline citations (`[cl/XXXXXX](http://cl/XXXXXX)`,
    `[Doc Title](URL)`).

### 5.2 2-Tier Quality Gate (Stage 5)

-   **Tier 1 — Specialist Self-Critique**: Relies on the specialist's
    deterministic gate (citation integrity, sparse-truth, window-honesty within
    the specialist domain).
-   **Tier 2 — GThink Core Provenance Gate**:
    -   Corroborate cross-corpus claims with $\\ge 2$ independent sources (e.g.
        Doc + CL or Chat + Bug).
    -   Strict global date-window enforcement (no leakage outside requested
        timeframe).
    -   Live URL verification via `verify_grounding.py` and
        `gthink_critique_bin.par`.
    -   Unified `### References` index and machine-verifiable `<details>` block.
