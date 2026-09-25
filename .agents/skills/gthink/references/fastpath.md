<!-- disableFinding(SNIPPET_EM_DASH) -->

# GThink Fast Path (Simple, Standard and Standard-wide tiers)

One read, one plan, one parallel retrieval batch, complete reads, one draft, one
check. This file is the **only** playbook for these tiers: do not open any other
GThink reference or any other skill's `SKILL.md`. The exact CLI invocations you
need are below.

## 0. Budget: complete data, few turns, one draft

Every survey is **complete for the window** and every artifact it names is
**deep-fetched** — full CL descriptions and, for review asks, every review
thread — by `survey_cls.py` in one parallel call that costs seconds. Nothing is
sampled, capped or truncated: a report built on a prefix of the data is wrong,
not fast. Wall time goes to model round-trips (~20 s each) and to bytes written,
so the caps are on **turns and output**, never on evidence:

| Tier              | Retrieval  | Reads        | Body     | Check            |
| ----------------- | ---------- | ------------ | -------- | ---------------- |
| **Simple**        | ≤ 2 calls, | tool output  | ≤ 1.5 KB | self-check       |
:                   : 1 turn     :              :          :                  :
| **Standard**      | 1 parallel | every survey | ≤ 8 KB   | verify_grounding |
:                   : turn       : file, all    :          :                  :
:                   :            : pages        :          :                  :
| **Standard-wide** | 2 parallel | every survey | ≤ 10 KB  | verify_grounding |
:                   : turns      : file +       :          :                  :
:                   :            : fetched docs :          :                  :

Reads are not retrieval turns: `view_file` the scratch files the survey names
using **exactly the `L<start>-<end>` page ranges its `OUT` lines list** (they
are sized to `view_file`'s 46 KB limit; a bigger range is silently cut), issuing
all pages of one file in the same turn, until every row in scope has been seen.
The body cap is the latency budget for *writing* and is obeyed structurally: a
3-sentence TL;DR, ≤ 5 themes each written as **one short prose paragraph (3–5
sentences, 2–4 citations in total)**, Risks and Next Steps as ≤ 4 one-line
bullets each, and **≤ 25 distinct artifact links in the whole body** (a good
weekly team report carries 15–20 — one per claim, none per CL); `### References`
is a **curated index of ≤ 20 artifacts** — the docs, decision bugs and the CLs
that carry each theme, one line each: for a CL the `refs.txt` line pasted
verbatim (no renumbering, restyling or shortening of the title), for a bug or
doc its `bugs.txt` / `docs.txt` line — closed by one plain-text count for
everything else in scope (`+ 212 further CLs in the window`). A reader who wants
the full list has the survey; a 100-link index is a failure, not thoroughness.
Never escalate to Deep mid-flight; Deep is only for an explicit `--deep` /
"comprehensive" ask.

## 1. Scope (zero tool calls)

-   **who**: requester = the current user (`whoami`). "my team" = the
    requester's Moma **team roster** ∪ their **direct reports** ∪ the requester.
    Resolve it in one short **roster turn** before the parallel batch (it does
    not count against the §0 retrieval calls): run `moma_cli --teams
    --username=<requester>` and `moma_cli --reports=<requester>` in parallel,
    pick the team where `Role: Lead` (else the `Primary Core Work` team with the
    deepest `Path`), then `moma_cli --team_id=<id>` for its members. Name that
    team verbatim in the TL;DR. Only when both lists are empty (an IC with no
    team) does "my team" mean the peers under the same manager (`moma_cli
    --manager=<requester>` → `--reports=<manager>`). A person or team named in
    the ask overrides this default.
-   **when**: use the stated window; defaults: "last week" = 7d, "this month" =
    30d, otherwise 14d. Compute absolute `YYYY-MM-DD` bounds now and never cite
    anything outside them.
-   **what**: Standard uses **exactly 3 source types** from §2 — always CLs,
    plus the two that best fit the ask (bugs, Drive docs, Chat, email,
    calendar). Include one Workspace source unless `--no-workspace` was passed.
    For *how time was spent* / load / bottleneck asks the three are CLs,
    calendar and bugs. For **team-scope** asks (roster ≥ 4) use four: CLs, bugs,
    Drive docs (team sync / status notes) **and Chat** (`list-spaces
    --query=<team name>` then `list-messages` on the team space, plus
    `list-mentions`) — that is where OOO handoffs, deadlines, reviews and
    escalations live, and a team report without them reads as a commit log.
    Because all calls run in parallel, breadth is free. Simple uses the single
    source that owns the entity.
-   **topic**: when the ask names a *topic* rather than a person or team ("our
    evaluation runs", "the migration", "the eval infra"), the scope is that
    topic across whoever worked on it — **not** the team's whole output. Filter
    the surveys by topic keywords (`awk -F'|' 'tolower($6) ~
    /eval|harness|timeout/' /tmp/gthink_survey_<slug>/cls.txt`, the same for bug
    titles) after reading the whole survey; unrelated team work is out of scope
    and padding with it is a wrong answer, not a thorough one. Name the filter
    you used in one line so the reader can widen it.
-   **direction** (triage / action-item asks): "assigned to me" means the
    requester is the assignee, reviewer-in-attention or comment addressee —
    check the `Assignee:` / attention-set / `to:` field, never the topic or the
    reporter. Items the requester *filed or assigned to others* are their
    follow-ups (P2), not their P0s.
-   **ritual noise**: agent-maintained ritual CLs (titles like "Costar Insights
    daily …", "… status brief", "… events log", "… decision log", "weekly
    digest") are counted by the survey's `RITUAL` digest line and reported as
    one `RITUAL` count. They are never cited and never counted as engineering
    output.
-   **shape**: if the ask is **quantitative or enumerative** — how time was
    spent, throughput, bottlenecks, trends, "analyze all / every X", per-person
    load — every number in the report comes from the §2 survey counts (`TOTAL` /
    `WEEK` / `HOUR` lines), never from the rows you happened to read. State the
    total and the window explicitly ("286 authored CLs, 2026-08-20 →
    2026-09-19"). Survey timestamps are UTC: convert the `HOUR` buckets to the
    requester's local zone (location from `moma_cli --person`, e.g. `US-SJC` →
    PDT = UTC−7) before calling anything "after-hours". Workstream shares come
    from the `DIRS` digest line (CLs per directory) over the whole survey, never
    from the rows you happened to read first. Such asks stay on this tier; they
    do not need Deep.

## 1.5 Standard-wide recipes (one per archetype; skip unless the ask matches)

Each recipe names the sources for the §2 batch, the local derivation (awk over
the scratch files — not a retrieval call) and the shape. Everything else in this
file applies unchanged; `references/archetypes.md` supplies the schema when the
ask names it.

-   **Peer bonus / peer feedback / closest collaborators** (window = quarter):
    two surveys, `a:<me>` and `r:<me> -a:<me>`, plus the bug survey for `<me>`.
    Collaborator strength = CLs by X that `<me>` reviewed + CLs by `<me>` that X
    reviewed (second number from the first 30 rows of `critique readonly info
    --cl=` is too slow — use
    `/google/bin/releases/gemini-agents-critique/critique readonly search
    --query="a:<me> r:X from:… is:all" --limit=1` per candidate X in the second
    turn, ≤ 8 candidates) + shared bugs (assignee X on a bug `<me>` filed or
    vice versa). Rank, take the top 3 (peer bonus) or the named person
    (feedback), and cite 2–3 of *their* CLs that `<me>` reviewed per pillar
    (Impact / Partnership / Consistency) — never the requester's own CLs under
    the nominee's name. **Hard rule:** every CL cited under a nominee must list
    `<me>` as a reviewer (or the nominee as a reviewer of `<me>`'s CL) in the
    survey row — a nominee's solo CL, however impressive, is not evidence of
    partnership and is dropped, not cited. If a pillar has no such CL, say so in
    one line rather than padding it. Exclude the requester's manager and reports
    for peer bonus.
-   **Stakeholder map / collaboration directory** (30d): roster turn (team,
    manager, reports) → surveys `a:<me>` and `r:<me> -a:<me>` → collaborators =
    authors of CLs `<me>` reviewed ∪ assignees of shared bugs ∪ people in the
    team Chat space; one `moma_cli --person=a,b,c,…` call gives each person's
    team and title for the tier grouping (Executive sponsors = managers above
    `<me>`; Technical leads = roster leads; Cross-functional partners = authors
    outside the team; Advisory = reviewers who only approve). Cite one CL, bug
    or doc per person, plain text when there is none. Emit the Mermaid `graph
    TD` the archetype requires, then the directory.
-   **Gap / blind-spot analysis** (30d): first turn = team surveys + Drive docs
    per roster member + `moma_search` for `<team> roadmap OKR plan`; pick the
    1–2 planning docs (title contains roadmap / OKR / plan / H2 / Q3) → second
    turn = `retrieve_content.py --docs <id>` for them and the CLs of the largest
    workstreams. Table: each stated goal → shipped evidence (CLs / FIXED bugs)
    or `[No shipped evidence in window]`; then the inverse: the top `WORK`
    buckets with no goal behind them. That inverse list *is* the blind-spot
    section.
-   **Project 101 / onboarding guide / architecture question** ("how does X
    work", "compare X with Y", "why did we build both"; 60d): the subject is the
    **code**, so skip the roster turn and the team survey. `code_search` each
    named system to its directory (`f:<path> README|BUILD|SKILL.md`, or the
    system's own words when no path is known), `view_file` its README and the
    3–5 files that implement it (main lib, config / fixture file, scorer or
    test), run the CL survey with `file:<path>`
    (`/google/bin/releases/gemini-agents-critique/critique readonly search
    --query="file:<path> from:… is:all"`) and put the 2–3 CLs that introduced
    each system into the §3 batch — design rationale lives in CL descriptions
    and the docs they link; `moma_search` the codename. Guide shape: what it is
    (README + 2 design docs) → how it is built (key files as source.corp links)
    → who owns what (top authors from the survey) → what changed recently → how
    to start (commands quoted from README / BUILD). Comparison shape: how each
    works (files linked) → trade-offs (pros / cons per system, from the code you
    read) → why both exist (the rationale the CLs state). An answer on this
    recipe that links no source file and no CL is wrong.
-   **Review feedback / linter friction / tech-debt hotspots** (30d): both
    surveys with `--comments` (`a:<me>` = inbound feedback on the requester's
    CLs; `r:<me> -a:<me>` = the requester's outbound reviews). Numbers come from
    the `COMMENTS` / `THEMES` / `by_commenter` lines over **all** threads; read
    every page of both `comments.txt` files and group by recurring theme with
    the file and CL each pattern hits. Keep the two directions separate: a lint
    blocker on a teammate's CL is *their* friction, never the requester's.
-   **AI usage / agent pairing / workflow friction** (14d): the `AI` digest line
    (`TAG=agy` CLs, `CONV=` sessions) is the measured usage; in `cls_deep.txt`
    group CLs by `CONV=` session to find multi-CL agent trajectories (leverage)
    and read the descriptions of rollbacks, fix-forward chains and `PENDING` CLs
    older than 3 days (friction). Address the requester directly, never as a
    third-person profile; a verdict that agent usage "was not measured" is wrong
    when the digest has the counts.
-   **Quarter retrospective / multi-person comparison**: per-person surveys
    (`a:X` per person, ≤ 6 people, one call each); every number comes from the
    `TOTAL`/`WEEK`/`DIRS` lines; cite each person's key CLs spread across the
    window. Compare shapes (workstream mix, cadence, review load), never raw
    counts alone.

## 2. Retrieve (one turn, all calls in parallel, synchronous)

Issue every command in the **same turn** with `WaitMsBeforeAsync: 60000` — one
tool call per command, never several chained with `&` in one shell: the combined
output truncates from the top and the first command's rows are what vanish.
Never background a retrieval, never poll with `schedule`/`manage_task`, and
never run `csa_cli` (cross-corpus semantic search is Deep-tier only). If a call
times out, record `[No data: <source> timed out]` and continue. Start the clock:
the roster turn's first command is `date +%s > /tmp/gthink_t0_<slug>` (§5 reads
it). Standard-wide may issue one more parallel turn for sources the first turn
named; a third retrieval turn is over budget.

Exact invocations (`alex`, `taylor` are placeholder LDAPs; `<workspace_root>` is
the Piper workspace whose `google3/labs/ix/gthink/skills/gthink/scripts/` exists
— your tool `Cwd` `–` never a copy under `/tmp` or another snapshot: an older
checker there passes drafts the current one rejects):

-   **People / team**

    ```bash
    /google/bin/releases/gemini-agents-moma/moma_cli --teams --username=<ldap>
    /google/bin/releases/gemini-agents-moma/moma_cli --team_id=<id>
    /google/bin/releases/gemini-agents-moma/moma_cli --reports=<ldap>
    /google/bin/releases/gemini-agents-moma/moma_cli --manager=<ldap>
    /google/bin/releases/gemini-agents-moma/moma_cli --person=<ldap1>,<ldap2>
    ```

-   **CLs — complete survey + deep fetch** (one call per scope; `r:` for
    reviews; never `after:`/`before:`; add `--comments` for any ask about code
    reviews, feedback or reviewer load):

    ```bash
    python3 <workspace_root>/google3/labs/ix/gthink/skills/gthink/scripts/survey_cls.py \
      --query="(a:alex OR a:taylor OR r:alex OR r:taylor) from:YYYY-MM-DD to:YYYY-MM-DD is:all" \
      --slug=team
    ```

    The script pages the search to completion, fetches every CL's description
    (and threads) in parallel, and prints the digest every number in the report
    comes from: `TOTAL <n> <author>`, `STATUS`, `WEEK`, `HOUR(UTC)`, `RITUAL`,
    `AI` (`TAG=agy` CLs, `CONV=` sessions), `BUGS`, `DIRS` (CLs per directory =
    workstreams) and, with `--comments`, `COMMENTS` / `THEMES`. Its `OUT` lines
    name the files and the exact `view_file` page ranges — read them all:
    `cls.txt` (every row, `CL n | author | status | +add/-del | YYYY-MM-DD HH:MM
    (UTC) | title`, citable as-is), `cls_deep.txt` (per CL: reviewers,
    `BUG=`/`CONV=`/`TAG=` tags, files, the full description — where design
    rationale, root causes and decisions live), `comments.txt` (every human
    review thread: location, status, author, text, replies), and three
    **paste-ready** files built from the same data: `people.txt` (roster `–`
    `ldap | display name | title | authored=N reviewed=M`, names from moma),
    `refs.txt` (one line per CL: `- [CL] [CL n](http://cl/n) -- real title
    (author x; reviewers: y; STATUS date)`) and `by_author.txt` (the same lines
    grouped `## <ldap> (<name>)`, then `## reviewed by <ldap>`). For
    requester-focused asks run two: `a:<me>` (`--slug=authored`) and `r:<me>
    -a:<me>` (`--slug=reviewed`). For topic asks read everything, then list the
    in-scope subset with `awk -F'|' 'tolower($6) ~ /<topic>/'
    /tmp/gthink_survey_<slug>/cls.txt`; totals stay whole-window.

-   **Bugs — complete survey** (one call, same pattern; `render` a bug only
    through §3). The command prints the counts for the whole result and writes
    **every** row sorted P0 → P1 → P2 (closed-as-noise statuses dropped) to
    `/tmp/gthink_bugs_rows.txt` `–` `view_file` it in full `–` each `priority |
    status | assignee | b/id | title | CLs: …` where `CLs:` is the bug's `Code
    changes:` list, the **authoritative fixing CLs** for that bug. Do not
    re-slice the file with your own parsers:

    ```bash
    /google/bin/releases/issues-cli/issues readonly search \
      --query="(assignee:alex OR assignee:taylor) modified>=YYYY-MM-DD" --limit=500 > /tmp/gthink_bugs.txt \
      && awk '/^Issue ID:/{n++} /^Priority:/{np[$2]++} /^Status:/{ns[$2]++} END {print "TOTAL", n; for (k in np) print "PRIO", k, np[k]; for (k in ns) print "STATUS", k, ns[k]}' /tmp/gthink_bugs.txt \
      && awk '/^Issue ID:/{id=$3; c=""} /^Assignee:/{a=$2; sub(/@google.com/,"",a)} /^Status:/{s=$2} /^Priority:/{p=$2} /^Title:/{t=substr($0,8,90)} /^Code changes:/{c=substr($0,15)} /^---/{if (s !~ /OBSOLETE|DUPLICATE|NOT_REPRODUCIBLE/) print p " | " s " | " a " | b/" id " | " t " | CLs: " c; id=""} END {if (id!="") print p " | " s " | " a " | b/" id " | " t " | CLs: " c}' /tmp/gthink_bugs.txt | sort > /tmp/gthink_bugs_rows.txt && wc -l /tmp/gthink_bugs_rows.txt
    ```

-   **Drive docs** (one call per person; drop `createdBySnippetsMachine: true`):

    ```bash
    /google/bin/releases/gemini-agents-gdrive/gdrive readonly search \
      --owner=<ldap>@google.com --modified-after=YYYY-MM-DDT00:00:00 --max=50 --json \
      > /tmp/gthink_drive_<ldap>.json && wc -c /tmp/gthink_drive_<ldap>.json
    ```

    Workspace listings are redirected to files for the same reason as the
    surveys — tool output truncates from the top at ~8 KB — and read with
    `view_file` in ranges of ≤ 300 lines (a page is cut at 46 KB; JSON lines are
    long).

-   **Email** (always keep the noise exclusions):

    ```bash
    /google/bin/releases/gemini-agents-gmail/gmail readonly search \
      "from:(alex OR taylor) after:YYYY/MM/DD before:YYYY/MM/DD \
       -from:snippets-machine -from:noreply -to:reviewlog -cc:reviewlog \
       -from:gwsq -from:startblock -from:buganizer-system -to:b-system" --max=50 \
       > /tmp/gthink_mail_<slug>.txt && wc -l /tmp/gthink_mail_<slug>.txt
    ```

-   **Chat** (`list-mentions` is primary; discover space `<id>` via
    `list-spaces` if needed):

    ```bash
    /google/bin/releases/gemini-agents-gchat/gchat readonly list-mentions --hours=<24×days> --max=100 > /tmp/gthink_mentions_<slug>.txt && wc -l /tmp/gthink_mentions_<slug>.txt
    /google/bin/releases/gemini-agents-gchat/gchat readonly list-spaces --query="<topic>" --limit=10
    /google/bin/releases/gemini-agents-gchat/gchat readonly list-messages --space=<id> --hours=<24×days> --max=200 > /tmp/gthink_chat_<slug>.txt && wc -l /tmp/gthink_chat_<slug>.txt
    ```

-   **Calendar**

    ```bash
    /google/bin/releases/gemini-agents-gcalendar/gcalendar readonly search "<topic>" \
      --start=<RFC3339> --end=<RFC3339> --max=100
    ```

-   **Internal docs**: built-in `moma_search` tool (one query).

-   **Code**: built-in `code_search` tool.

## 3. Content fetch (docs and bugs; CLs are already deep-fetched)

Fetch the content of every doc and bug the ask or the surveys make relevant —
planning docs, sync notes, and each bug you will explain — spread across the
people and workstreams the survey showed, in **one** batched call. CL
descriptions are already in `cls_deep.txt`; pass `--cls` only for CLs outside
the survey (a fixing CL from a bug's `Code changes:` line, for instance):

```bash
if [ -x /google/bin/releases/gthink/scripts/retrieve_content.par ]; then
  /google/bin/releases/gthink/scripts/retrieve_content.par \
    --docs <id> ... --cls <n> ... --bugs <id> ...
else
  python3 <workspace_root>/google3/labs/ix/gthink/skills/gthink/scripts/retrieve_content.py \
    --docs <id> ... --cls <n> ... --bugs <id> ...
fi
```

The script prints its cache directory (outside the source tree). It writes one
JSON per artifact (`doc_<id>.json`, `cl_<n>.json`, `bug_<id>.json`) plus two
paste-ready digests: `bugs.txt` (`## REFS`: `- [Bug] [b/N](http://b/N) - title
(P?, STATUS, assignee x; modified date)`, then `## DETAILS` with each bug's
owner/reporter/dates, `Code changes:` line and description excerpt) and
`docs.txt` (title, link, created date, headings). `view_file` the two digests
once — never the JSON files one by one; open a single `bug_<id>.json` /
`doc_<id>.json` only for an artifact you quote in depth. Titles, authors, dates
and URLs already present in §2 tool output are grounded as-is. If the cache is
missing or empty, output exactly `[INGESTION_FAILED: Cache file is missing or
empty. Synthesis aborted.]` and stop. Simple tier skips this section and cites
§2 output directly.

## 4. Draft (once, straight to final)

-   **Select before you write.** From the surveys and the fetched content, fix
    the reference set first: the ≤ 20 artifacts that carry the story — the key
    change of each workstream, the fixing CL of each bug you explain, the open
    P0/P1s, the human-stated risks, the docs or chat you used. Everything else
    becomes a count (`+ 212 further CLs`, `40 more P2s`), never a list. `###
    References` is exactly this set (its `refs.txt` lines, in order of first
    citation), so its length is known before you start writing; an inline
    citation outside the set is fine, the index does not have to repeat it.
-   Answer the ask directly and reproduce any structure the prompt requested. If
    the ask names an archetype (comment / action-item **triage**, status report,
    open loops, weekly snippets, decisions log, executive briefing, stakeholder
    map, peer bonus, gap analysis), `view_file`
    `google3/learning/gemini/agents/skills/communication/gthink/references/archetypes.md`
    (prepend your active workspace root) once (the only other reference allowed
    on these tiers) and apply the matching schema only. A triage is always
    tiered: `## P0: Blocking / assigned to you` (the requester is the assignee,
    attention-set reviewer or comment addressee) → `## P1: Mentions / review
    requested` → `## P2: Follow-ups / stale` (items the requester filed or
    assigned to others, and anything untouched > 7d), one linked line per item
    naming who owns it — and an item is listed as open only after its **live
    state** was checked in the same §3 fetch (the doc comment thread is still
    unresolved, the CL still lacks the requester's LGTM, the access request is
    still pending): a notification email is a lead, not evidence, and one
    already-closed item reported as urgent discredits the whole triage.
    Otherwise use the Standard shape: `**TL;DR**` → `## Key Updates &
    Achievements` (≤ 5 themed `###` subsections) → `## Risks & Blockers` → `##
    Next Steps` → `### References`. The TL;DR is the first line of the report
    and is written for a director skimming on a phone: **≤ 3 short sentences (≤
    60 words) of plain prose** — what the period changed for users, the codebase
    or the roadmap; the single biggest live risk; the next step — with **at most
    one number** and nothing else that needs decoding: no inventory of CL / bug
    / tag counts, no `(1) … (5)` theme enumeration (the `###` headings are the
    enumeration), no code paths, IDs, backticked tokens or links. The checker
    rejects a TL;DR over 700 characters or carrying four or more numbers. When
    the ask requests a plan, recommendations or opportunities, that section is a
    first-class theme with the same depth as the diagnosis — ≥ 3 concrete items,
    each with an owner, the tool or mechanism, and the evidence that motivates
    it — never a 4-bullet afterthought. Counts, where one is needed at all, are
    a single line inside a theme or the closing line of `### References`, keep
    **submitted** and **pending** CLs separate (`212 submitted, 7 pending`),
    never one "authored" total — and never appear in the TL;DR.
-   **Impact first: what, why, how. Artifacts prove the story, they are not the
    story.** A theme is one short prose paragraph that answers three questions
    in order — **what** changed (the outcome: what users, the codebase or the
    roadmap can now do, the number that moved, the decision taken), **why** it
    matters (the problem it removes, the risk it retires, the launch it
    unblocks), and **how** (the mechanism or design choice, in one clause) —
    with **2–4 citations for the whole paragraph, chosen by weight** (design
    doc > decision-bearing bug > contract-changing CL > mechanical CL) and
    placed on the sentence each one proves. Volume is not impact: "shipped 23
    CLs", a per-person commit list, a bullet that restates a CL title, or a run
    of links after a sentence say nothing about value and are never written; a
    stack of CLs is the bug that tracks it or one range `[CL a](http://cl/a)–[CL
    b](http://cl/b)`, and the rest lives only in `### References`. Optional ≤ 2
    sub-bullets carry genuinely distinct sub-outcomes, one citation each. Themes
    are **workstreams, never people**: a `### <person>` heading or a per-person
    commit list is the wrong shape for every ask except an explicit per-person
    breakdown; unrelated CLs by the same author do not belong in one theme.
    Every citation must support the exact sentence it sits on — a real link on
    an exaggerated or loosely related claim costs more credibility than no link
    at all.
-   **Risks must be live and sourced.** A risk is a bug the survey shows still
    open (`ASSIGNED`/`ACCEPTED`/`NEW`, modified in-window), a `PENDING` CL (a
    survey row with that status is in-flight work — a blocker or dependency on
    readiness asks, never an achievement), or a concern a human stated in a sync
    doc, chat or email (OOO coverage, an upcoming review or deadline, an
    unassigned P1, a cross-team dependency). A `FIXED` bug is an achievement,
    not a risk; an umbrella bug last touched before the window is not a risk.
    Prefer the human-stated risks: they are what the reader cannot get from a
    query.
-   **Select themes by impact, against the whole survey.** Read the complete
    survey and `cls_deep.txt`, then pick the ≤ 5 workstreams whose *outcomes*
    matter most to the reader — a launch, a migration completed, a decision
    taken, a P0 retired, a contract changed — not the five with the most CLs. A
    workstream with many CLs and no user-visible outcome is one sentence or part
    of the References count, not a theme; a two-CL workstream that unblocked a
    launch is a theme. The biggest risk and the next steps are chosen against
    the whole survey and the open P0/P1s, not against the artifacts you happened
    to fetch, and a team report is not the requester's own week: every
    workstream that carried a real outcome appears whoever did it. In a
    person-scope report the requester's review work is one sentence with one
    citation, not a list.
-   **Absence of evidence is not evidence of absence.** The survey is a window
    on Critique and Buganizer, not on the person. Someone with nothing in scope
    is simply not mentioned — never `[No notable work found for X]`, never "X
    did not ship / had no CLs", never a coverage line written to prove the
    roster was read. A gap is asserted only when a source states it (a doc that
    says the SLO is undefined, a bug that says the owner is unassigned). Survey
    rows are grounded evidence as-is (CL number, author, status, date, title);
    `cls_deep.txt` supplies the rationale (quote the decision, not the title).
    Automated pipeline CLs (the survey's `RITUAL` line) are excluded silently —
    at most one plain-words note (`automated digest CLs excluded`), never the
    field name, `TAG=`/`CONV=` tags, team IDs or other ingest vocabulary — and
    never inflate a total. A pending or excluded CL you mention is cited by
    number.
-   **Paste identities and references, never compose them.** People come from
    `people.txt` (LDAP and display name copied from its columns — never derive
    one from the other or from memory) and every CL line is pasted from
    `refs.txt` / `by_author.txt`, which already carry the real title, author and
    reviewers; bugs and docs take the name their `render` output shows. Write
    `Name (ldap)` at a person's first mention in a theme and the LDAP alone
    afterwards — never the pair on every bullet. A person appears on a line with
    a CL only if that line lists them as its author or reviewer — this holds
    **per line and per table cell**, so split the line or drop the name, and in
    plan / next-step lines that name a collaborator cite the bug, not someone
    else's CL. The TL;DR and any roster / "team led by" sentence carry **no
    citations at all**. Acronyms and project codenames are written in CAPS or
    plain text, never in backticks or parentheses like an LDAP. One wrong
    attribution outweighs ten correct citations.
-   **Use each source for what it proves.** A design doc, sync-doc line or chat
    message is the evidence for a decision, a human-stated risk or a plan; a bug
    is the evidence for a problem and its status; a CL is the evidence for what
    actually changed and how. A root-cause or blocker report that cites only
    bugs has not shown the fix, and a decision attributed to a CL that does not
    state it is a fabrication. A source type that returned nothing material is
    simply not cited. Put the CLs you will cite into the §3 `retrieve_content`
    call.
-   **Explain, don't report status.** When the ask says *root cause*, *why*,
    *what broke* or *how it was fixed*, every item states **cause → fix →
    evidence**: the fixing CL is the one on the bug's `Code changes:` line in
    the survey (or named in its `render` output) — put *that* CL into §3 and
    take cause and fix from its description; never pair a bug with a same-topic
    CL from the survey rows you happened to see. The evidence is the `[b/…]` +
    `[CL …]` pair. "Fixed" / "in progress" / a bug title restated is status, not
    a root cause, and does not satisfy the ask.
-   Every factual bullet ends with an inline citation copied verbatim from tool
    output: `[CL 123](http://cl/123)`, `[b/123](http://b/123)`,
    `[Title](https://docs.google.com/document/d/<ID>/edit)`,
    `[Subject](https://mail.google.com/mail/#all/<ID>)`. No bare IDs, no
    `file:///`, no `/google/src/...`, no cache paths, no invented or reused
    links; when no URL exists, use plain text. In `### References`, prefix each
    entry with its type: `[CL]`, `[Bug]`, `[Doc]`, `[Email]`, `[Chat]`.
-   Only evidence inside the window. Zero results → `[No data found for X]`;
    never pad with empty or "N/A" sections.
-   Exclude bot authors (`snippets-machine`, `copybara-service`, `gwsq`,
    `noreply`, `*-bot`, `*-system`).
-   Body within the §0 cap; `### References` complete.

## 5. Check (Standard: one pass; Simple: skip)

Write the draft **once** with `write_to_file` to `<draft>` =
`/tmp/gthink_draft_<slug>.md` (a short topic slug such as `p1_audit`, so
concurrent runs never clobber each other) — it must already be the final text —
and run, in one call:

```bash
echo ELAPSED $(( $(date +%s) - $(cat /tmp/gthink_t0_<slug> 2>/dev/null || date +%s) )) && \
(if [ -x /google/bin/releases/gthink/scripts/verify_grounding.par ]; then \
  /google/bin/releases/gthink/scripts/verify_grounding.par <draft>; \
else \
  python3 <workspace_root>/google3/labs/ix/gthink/skills/gthink/scripts/verify_grounding.py <draft>; \
fi)
```

The checker reads the survey / `retrieve_content` cache and reports, per line:

-   `Ungrounded CL Citation` — a cited CL that no tool result ever returned (a
    typo or an invented ID). Drop it; only if you copied it verbatim from a tool
    output that was not cached, run `retrieve_content.py --cls <ids>` and drop
    whatever it reports missing.
-   `Attribution Risk` — a person (LDAP or display name) on a line that cites a
    CL they neither authored nor reviewed. Drop the name or move the citation;
    put people in section headings and CLs under their own author.
-   `Unknown LDAP` / `Identity Mismatch` — an LDAP no tool output ever printed,
    or a `Name (ldap)` pair that disagrees with `people.txt`. Copy the LDAP from
    `people.txt` or drop it.
-   `Title Mismatch` — a reference line whose title is not the cited CL's or
    bug's (the message shows the real one): paste the real title, or point the
    line at the ID that title belongs to.
-   `False CL Claim` — a path the cited CL neither touched nor names.
-   `Citation Overload` / `References Overload` — more than three artifact links
    on one body line, or more than 25 in `### References`: keep the 1–2 that
    prove the outcome, collapse a stack to a range or its bug, and let the rest
    be a count.
-   `Status Mismatch` / `Bug Assignee Mismatch` — a PENDING/SUBMITTED word or an
    assignee written next to an artifact disagrees with the cache; copy the
    `refs.txt` / `bugs.txt` line.
-   `Body Citation Budget` — more than 30 distinct artifact links in the body:
    the report has become a list. Keep the one link that proves each claim and
    move the rest to the References count; triage deliverables (`## P0` tiers)
    are exempt.
-   `Absence Claim` — a line that reports someone or something as having done
    nothing (`No notable work found`, `did not ship`, `no CLs`): delete the
    line; absence of evidence is not evidence of absence.
-   `TL;DR Overlong` / `TL;DR Inventory` / `Pipeline Jargon` — the TL;DR runs
    past 700 characters or lists four or more numbers, or a body line carries
    ingest vocabulary (`TAG=`, `CONV=`, `RITUAL`, a team ID, a routing or tier
    sentence): rewrite that line as one plain sentence of outcomes.

**Time-box.** If `ELAPSED` is above 600, deliver the draft as it stands after
dropping only the lines the checker marked `❌` (no other edits, no second
check): a report delivered at 11 minutes with one fewer citation beats a perfect
one that never arrives. Below 600, proceed normally:

Fix `❌` findings only by re-selecting or dropping evidence — never by inventing
links or by scripted edits — with one `replace_file_content` per flagged line
(`TargetContent` = **that line only**), all of them in the same turn however
many there are. A target longer than 5 lines is a rewrite, and a rewrite is
forbidden whichever tool performs it: a second emission of the report costs as
much as the first, and three of them is a timeout. Never edit, trim or redraft
for size after the draft is written — the §0 cap is obeyed while writing, not
enforced afterwards. One verify pass, at most one fix pass, then deliver: `✅
PASS` ends this stage — the next action is §6, with no further commands,
measurements or edits. Do not run `gthink_critique_bin.par` on these tiers.

## 6. Deliver

Apply the Output Contract in `SKILL.md` (TL;DR first, `### References`, footer).
Chat is the only channel unless `--doc` or `--email` was passed:

-   **`--doc`**:

    ```bash
    /google/bin/releases/gemini-agents-gdocs/gdocs mutate import-md \
      --title="<Report Title>" --file=<draft> --pageless
    ```

-   **`--email`** (convert markdown to HTML with inline table styles first):

    ```bash
    /google/bin/releases/gemini-agents-gmail/gmail mutate send \
      --to=<recipient>@google.com --subject="<Report Title>" \
      --body-file=<draft>.html --html
    ```
