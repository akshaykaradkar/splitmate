---
name: gthink
description: >-
  Verified multi-corpus deep research across Engineering systems (CLs, Bugs,
  Code, Moma) and Google Workspace (Docs, Gmail, Chat, Calendar). Use when
  synthesizing project updates, progress reports, and status summaries (team
  weekly summaries, weekly snippets, executive briefings, highlights and
  lowlights, what a team or engineer shipped or worked on, roadmap/OKR gap
  analysis, stakeholder maps, doc comment triage), auditing engineering health
  and technical deep-dives (architectural evolution across CLs, infra/eval
  blockers, P0/P1 bug root causes, review tech debt, BUILD hygiene), or
  preparing career, GRAD, and knowledge discovery artifacts (quarterly check-in
  notes, peer feedback, peer bonus nominations, impact scorecards, AI fluency,
  Project 101 onboarding guides). Don't use for simple single-corpus lookups or
  counts, direct codebase symbol/file searches, standalone single-tool queries,
  reading/summarizing one named Google Doc (use gdocs), or inspecting one CL's
  diff or comments (use critique).
---

# gthink — Deep Research Skill (GThink Core)

Use this skill when the user asks a corp knowledge question that requires
searching and analyzing information across engineering systems (Bugs, CLs,
Codebase, Internal Knowledge) and their Google Workspace (Drive, Gmail, Chat,
Calendar).

> [!IMPORTANT] **Exclusive Research Pipeline**: Answer with GThink Core itself —
> either by dispatching to a GThink specialist via the Stage 0 handshake, or by
> running the tiered pipeline below. Never hand the work to an external or
> off-platform service (e.g. `EvalTriageA2AService`), and never hand it back to
> the user as a command for them to run. Dispatching to the in-repo `devfolio`
> specialist is the intended behavior, not external delegation.

> [!IMPORTANT] **Experimental Folder Prohibited**: Never use, load, or run
> scripts from any skill under `/google3/experimental/` or any path containing
> `/experimental/`.

## Usage & Flags

```text
/gthink "Prepare my quarterly GRAD check-in notes."
/gthink "Summarize the work of my team last week and call out key risks and next steps." --doc
/gthink "Write constructive GRAD peer feedback for alex based on our collaboration this quarter."
/gthink "What are the root causes of recent failures and open blockers in our infrastructure work?" --no-workspace
```

-   `--doc`: Export the canonical report to a Google Doc for easy sharing.
-   `--email`: Email the report to yourself (or `--email=user@`).
-   `--no-workspace`: Restrict retrieval to Tier 2 Engineering systems (Bugs,
    CLs, Codebase, Moma), skipping Google Workspace data.
-   `--deep`: Force the Deep tier (full 6-stage pipeline) for a request that
    would otherwise run on the fast path.

### Default Behavior: Bare `/gthink` or Help (Interactive Command Guide)

When invoked with **no research inquiry or topic** (bare `/gthink`, `/gthink
help`, `/gthink --help`, `help`, or general help questions like *"how to use
gthink"*):

> [!IMPORTANT] **Zero Ingestion / Zero Fast-Path Dispatch**: do **not** trigger
> data ingestion and do **not** dispatch to DevFolio. Immediately print the
> GThink Interactive Command Guide below and stop.

### 🧭 Welcome to GThink Deep Research

**gthink** conducts deep corporate research across engineering systems (Bugs,
Critique, Code Search, Moma) and your Google Workspace (Drive, Gmail, Chat,
Calendar), synthesizing verified reports with clickable citations across 3 core
areas:

#### 🔍 1. Strategy, Project Updates & Workspace Intelligence

*   `/gthink "Summarize the work of my team last week and call out key risks and
    next steps."`
*   `/gthink "What are the biggest gaps between our stated roadmap and what we
    actually shipped?" --doc`
*   `/gthink "Build a stakeholder map and collaboration directory for my active
    projects."`
*   `/gthink "Triage all open doc comments, review requests, and action items
    assigned to me."`

#### 🛠️ 2. Engineering Health & Technical Deep-Dives

*   `/gthink "Summarize the core architectural changes and design decisions
    across my recent CLs."`
*   `/gthink "What are the root causes of recent failures and open blockers in
    our infrastructure work?"`
*   `/gthink "What recurring feedback, linter friction, and tech debt hotspots
    show up in my code reviews?"`
*   `/gthink "Audit the BUILD files and dependency hygiene across my active
    project directories."`

#### 🎯 3. Career, GRAD Portfolio (`devfolio`) & Knowledge Discovery

*   `/gthink "Prepare my quarterly GRAD check-in notes."` (or **`--check-in`**)
*   `/gthink "Write constructive GRAD peer feedback for alex based on our
    collaboration this quarter."` (or **`--feedback <ldap>`**)
*   `/gthink "Find the peers I worked with closely last quarter and compose peer
    bonus nominations for the top 3."`
*   `/gthink "Generate my engineering impact profile and activity scorecard for
    the last quarter."` (or **`--profile`** / **`--stats`**)
*   `/gthink "Create a 'Project 101' onboarding guide for a new engineer joining
    our workstream."`
*   **`--grad`** or **`/devfolio`** — Open the DevFolio / GRAD Interactive Guide
    (`--ready` for 365d promotion readiness).

#### ⚙️ Output & Scope Flags

*   **`--doc`**: Export the canonical report to a Google Doc for easy sharing.
*   **`--email`**: Send the report directly to your email (or `--email=user@`).
*   **`--no-workspace`**: Restrict retrieval to Tier 2 Engineering systems
    (Bugs, CLs, Codebase, Moma), skipping Google Workspace data.
*   **`--deep`**: Run the full 6-stage pipeline instead of the fast path.

## Policies

-   **Audience**: inferred from the request; the requesting user by default.
-   **Data scope**: Workspace + Engineering by default; Engineering only with
    `--no-workspace`.
-   **Evidence**: every claim carries an inline citation copied verbatim from
    tool output; evidence is restricted to the requested date window.
-   **Latency is a quality metric, data is never the lever**: the tier chosen in
    Stage 1 fixes the turn and report budgets; the evidence within the asked
    scope is always complete and deep-fetched. Win time with parallel fetches
    and fewer round-trips, never by sampling or truncating data.

## Workflow

### 0. Fast-Path Dispatch (evaluate first, zero tool calls)

If the request plausibly asks for *DevFolio / GRAD guide or help (`--grad`,
`grad`, `--devfolio`), GRAD check-in (`--check-in`), AI fluency (`--ai`),
engineering profile (`--profile`), activity stats (`--stats`), project recap
(`--recap`), peer feedback (`--feedback`), promotion readiness (`--ready`), role
match (`--match`), or team report (`--team`)* **and** the subject is the
requester or one named person, read only
`google3/learning/gemini/agents/skills/communication/gthink/references/composition.md`
(prepend your active workspace root) and apply its §1.4 dispatch table. If the
subject is a team, org, or cohort (e.g. *"my team's work last week"*), condition
(2) below already fails — do not read composition.md; go straight to Stage 1. An
*analysis* of AI usage, agent pairing, workflow friction or time allocation over
a window is a research ask, not the DevFolio `--ai` fluency assessment: it fails
condition (4) — go straight to Stage 1. Otherwise skip to Stage 1.

> [!NOTE] Generic help requests (*"/gthink help"*, *"/gthink --help"*, bare
> *"/gthink"*, *"help"*) are **GThink guide** requests, not DevFolio: print the
> guide above and stop. Only explicit DevFolio/GRAD triggers (`--grad`, `grad`,
> `--devfolio`, *"help my grad tasks"*) dispatch to DevFolio.

Dispatch immediately **only if all four hold**: (1) exactly one specialist owns
the *entire* request; (2) the subject is the requester or one named person — not
a team, org, cohort, or comparison across people; (3) no evidence is needed from
outside that specialist's domain; (4) the user wants the specialist's
deliverable itself, not commentary or a larger document built on it. If any
condition fails, or you are unsure, fall through to Stage 1.

When it fires, dispatch via the composition.md §4 handshake, return the
deliverable, and stop. Never read any other reference, never resolve identity
via `moma`, and never tell the user to run another slash command.

### 1. Tier Gate (zero tool calls; decides everything that follows)

Classify from the prompt alone, then follow **exactly one** path:

-   **Simple** → `references/fastpath.md`. A count, lookup or status of one
    known entity or one corpus (*how many CLs did I submit last week*, *is b/123
    fixed*, *who reviewed cl/N*). Native Jetski answers these without GThink;
    this tier runs only when the user invoked `/gthink` explicitly, and it
    counts from a complete survey without synthesis.
-   **Standard** (default) → `references/fastpath.md`. Summaries, updates,
    triage, or health checks over a window ≤ 30 days for the requester or their
    team, where ≤ 3 source types suffice (*how am I doing on my project*, *what
    changed in my CLs and why*, *what happened in our infra work*). Quantitative
    and enumerative asks in that window (time allocation, throughput,
    bottlenecks, trends, "analyze all X") are Standard too. Standard reads the
    complete survey and every deep-fetched description: the fast path is fewer
    turns, never less data, so these asks never need Deep.
-   **Standard-wide** → `references/fastpath.md` (its §1.5 recipes). The
    archetypes that span 4–5 source types or a quarter: stakeholder maps, peer
    bonus nominations and peer feedback, gap / blind-spot analysis, Project 101
    guides and architecture questions (how a system works, X vs Y trade-offs,
    why it was built), quarter retrospectives, multi-person comparisons. They
    get a wider retrieval budget on the same one-draft discipline; they are
    **not** Deep — the Core pipeline's playbook reads and critique loops exceed
    the latency budget on these asks and deliver nothing.
-   **Deep** → Stages 2–6 below. **Only** when `--deep` is passed or the user
    explicitly asks for a *comprehensive / exhaustive* audit. Never infer Deep
    from the size of the ask.

**Simple / Standard / Standard-wide**: use `view_file` to read
`google3/learning/gemini/agents/skills/communication/gthink/references/fastpath.md`
(prepend your active workspace root) and follow it end to end. Do **not** read
any other GThink reference (except
`google3/learning/gemini/agents/skills/communication/gthink/references/archetypes.md`
(`references/archetypes.md`) when fastpath.md §4 asks for it), any
`core/references/*.md`, or any other skill's `SKILL.md` — fastpath.md already
contains the exact CLI invocations. Then apply the Output Contract.

**Deep**: continue with Stages 2–6. At each stage, use `view_file` to read the
referenced GThink Core playbook (prepend your active workspace root) before
acting.

### 2. Scope & Plan (Deep)

Read `google3/labs/ix/gthink/core/references/scope.md` and
`google3/labs/ix/gthink/core/references/plan.md`.

-   Resolve user context (LDAP, manager, reports, team) via `moma_cli`; weight
    artifacts by interaction goal (Strategic vs. Tactical), not seniority.
-   Resolve all four axes (who / when / where / what). Enforce the global date
    window strictly: never cite artifacts or compute dates outside it.
-   Reproduce any structure the prompt requests; otherwise, if the request names
    an archetype, read
    `google3/learning/gemini/agents/skills/communication/gthink/references/archetypes.md`
    (`references/archetypes.md`) and apply the matching schema only.
-   **Specialist routing (terminal gate)**: when a specialized domain (GRAD
    artifacts, career portfolios, activity metrics) is involved, read
    `google3/learning/gemini/agents/skills/communication/gthink/references/composition.md`
    (`references/composition.md`). Single specialist → dispatch and **stop**.
    Multiple specialists or cross-corpus evidence → continue, invoking
    specialists as inputs via their public entrypoints only.
-   Select ≥ 5 distinct source types; apply the 3-class timeout policy in
    `google3/labs/ix/gthink/core/references/policies.md`.

### 3. Ingest (Deep)

Read `google3/labs/ix/gthink/core/references/ingest.md`.

-   Multi-corpus sweep across Gmail, Drive, Chat, Calendar, Critique, Buganizer
    and Moma (Engineering only with `--no-workspace`), using the full
    `/google/bin/releases/...` CLI paths in `references/fastpath.md` §2 (never
    invoke `gmail`, `gdrive`, `gchat` or `gcalendar` as bare commands on
    `$PATH`); issue independent calls in parallel, synchronously, in the
    foreground. If a call is backgrounded by the runtime, yield with zero text
    until it completes.
-   Cross-corpus semantic search is allowed on this tier only, once, in the
    **foreground** (no trailing `&`, no `schedule`/`manage_task` polling):
    `/google/bin/releases/csa-cli/csa_cli.par --user_prompt="<question>"
    --latency_budget_seconds=20`.
-   Unroll referenced entities (depth ≤ 2) via
    `labs/ix/gthink/core/scripts/references.py`; strip bot noise via
    `labs/ix/gthink/core/scripts/noise_filter.py`; batch CL queries via
    `labs/ix/gthink/core/scripts/cl_stream.py`; use the pre-compiled `.par`
    analyzers in `/google/bin/releases/gthink/scripts/` (`analyze_cls.par`,
    `analyze_transcripts.par`, with graceful fallback to `python3
    <workspace_root>/google3/labs/ix/gthink/skills/gthink/scripts/<script>.py`)
    instead of ad-hoc scripts. Never grep or walk `~/.gemini/jetski/brain/`
    manually.
-   Fetch bodies for every candidate Doc, CL and bug in **one** batched call
    (prefer `/google/bin/releases/gthink/scripts/retrieve_content.par` if
    executable, falling back to `python3
    <workspace_root>/google3/labs/ix/gthink/skills/gthink/scripts/retrieve_content.py
    --docs … --cls … --bugs …`; never `blaze run`), then `view_file` the cache
    files you will cite. If the cache is missing or empty, output exactly
    `[INGESTION_FAILED: Cache file is missing or empty. Synthesis aborted.]` and
    stop — never synthesize from memory.

### 4. Synthesize (Deep)

Read `google3/labs/ix/gthink/core/references/synthesize.md` and
`google3/learning/gemini/agents/skills/communication/gthink/references/grounded_synthesis.md`
(`references/grounded_synthesis.md`). Corroborate cross-corpus claims with ≥ 2
independent sources; weave specialist output into one voice; apply the Output
Contract's citation rules.

### 5. Critique (Deep)

Read `google3/labs/ix/gthink/core/references/critique.md`. Audit the six
dimensions (Groundedness, Relevance, Completeness, Correctness, Coherence,
Safety), save the draft outside the source tree at `/tmp/gthink_draft_<slug>.md`
(a short topic slug, so concurrent runs never clobber each other) and run:

```bash
(if [ -x /google/bin/releases/gthink/scripts/verify_grounding.par ]; then \
  /google/bin/releases/gthink/scripts/verify_grounding.par /tmp/gthink_draft_<slug>.md; \
else \
  python3 <workspace_root>/google3/labs/ix/gthink/skills/gthink/scripts/verify_grounding.py /tmp/gthink_draft_<slug>.md; \
fi) && \
/google/bin/releases/gthink/gthink_critique/gthink_critique_bin.par /tmp/gthink_draft_<slug>.md
```

Critique verifies; it never authors. Missing evidence → re-run
`retrieve_content` for the missing IDs and re-synthesize, or disclose `[Evidence
Gap: No X found]`. Never patch the draft with scripts or in-place edits to force
a pass. Max 3 passes.

### 6. Deliver (Deep)

Read `google3/labs/ix/gthink/core/references/deliver.md` for channels (`--doc`
pageless export, `--email`) and the privacy gate, then apply the Output
Contract.

## Output Contract (all tiers)

-   **Opening**: the first visible line is the `**TL;DR**` (or the ask's own
    first section). No banner, routing statement, tier or workflow name, or
    other pipeline narration precedes it; the AI-generated disclaimer lives in
    the footer.
-   **Citations**: a report is judged by what it says, and a citation exists to
    let the reader check one claim. Every claim a reader could act on or dispute
    (an outcome, a decision, a risk, a number, a person's credit) carries
    **one** link that directly supports it — two at most — selected verbatim
    from tool output — `[CL 123](http://cl/123)`, `[b/123](http://b/123)`,
    `[Title](https://docs.google.com/document/d/<ID>/edit)`,
    `[Subject](https://mail.google.com/mail/#all/<ID>)`,
    `[Event](https://www.google.com/calendar/event?eid=…)`,
    `[Room](https://chat.google.com/room/…)`. Google3 files use
    `https://source.corp.google.com/piper///depot/google3/<path>`. Never emit
    bare IDs, `[[text](http://…)]` double brackets, `file:///`, `/google/src/…`,
    `drive/open?id=` fragments, or cache paths (`.gthink_cache*`); when no
    canonical URL exists, use plain text. A link that only proves activity
    happened (a run of CLs after a sentence, a CL per bullet, a per-person
    commit list) is noise: it lowers readability and the credibility of every
    other citation. A claim with no artifact that supports it is dropped, not
    padded with weaker ones. Connective prose needs no citation.
-   **Honesty**: only evidence inside the window; never fabricate or reuse an
    ID/URL across unrelated items; exclude bot authors; never attribute
    approvals or decisions a cited artifact does not support. When an explicitly
    requested corpus or scope returns nothing, say `[No data found for X]` once.
    Absence of evidence is not evidence of absence: a person or workstream the
    survey shows nothing for is left out, never reported as having done nothing,
    and nothing a source does not say is asserted as a gap.
-   **`### References`**: conclude every deliverable with a curated, numbered
    index of the ≤ 20 artifacts that carry the story (`[CL]`, `[Bug]`, `[Doc]`
    tags), closed by one plain-text count for the rest of the evidence in scope.
    Never a dump of every artifact cited, and never a machine-readable appendix.

-   **Hygiene**: clean markdown only — no thought blocks, raw XML, link logs,
    doubled headers (`### **### …**`), and none of the pipeline's vocabulary:
    VCS tags (`TAG=`, `CONV=`), survey field names (`RITUAL`, `TOTAL`, `DIRS`),
    Moma team or component IDs, tier or workflow names. Say it in plain words
    (`automated digest CLs excluded`, `agent-assisted CLs`). Strip private
    ratings, compensation, and confidential MTM bugs.

-   **Footer** (last lines of chat and artifact):

    ```markdown
    ---
    _Generated via [go/try-gthink](http://go/try-gthink) (Workspace data); AI-generated research — verify citations before acting. Feedback? [go/gthink-chat](http://go/gthink-chat) or [go/gthink-feedback](http://go/gthink-feedback)._
    ```

    With `--no-workspace`, write `(Workspace data excluded)` instead of
    `(Workspace data)`.

## Reporting Issues

Report bugs or improvements for this skill at
[go/gthink-feedback](http://go/gthink-feedback) (component **2261621**, hotlist
[Agent Skill: gthink](http://b/hotlists/8984576)). See the `skill_issue` skill
for instructions on filing and triaging skill bugs.
