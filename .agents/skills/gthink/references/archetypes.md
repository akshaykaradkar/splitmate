# GThink Inquiry Archetype Schemas

Structural contracts for named deliverable types. Read this file only when the
request names one of the archetypes below (Deep tier always reads it). Apply
only the matching section; everything else in this file is irrelevant to the
request.

## Comment & Action-Item Triage

Must synthesize document comments (Docs, Sheets, Slides) and action items across
Gmail and Chat. Strictly structure into requested tiers (e.g. P0: open comments
assigned to me; P1: open comments mentioning me; P2: deleted content cleanup /
stale items).

-   **Strict Window Zero-Substitution**: Do NOT substitute or append comments
    from outside the target activity window (e.g., never list August comments
    when asked for July comments). If zero unresolved comments exist within the
    exact requested target window, report zero unresolved comments for that
    window.

## Strategic Gap Analysis & Blind Spots

Must synthesize signals across Drive docs, Gmail threads, Chat spaces, Calendar
syncs, Buganizer issues, and CLs from the timeframe. Contrast stated roadmap
goals against actual committed CLs and open bugs. Examine retroactive commits to
identify unaddressed technical debt. Structure with Executive Summary, Gap
Taxonomy (Architectural Blind Spots, Technical Debt, Dependency Risks), and
Actionable Mitigations. 100% inline citations.

## Project Status Reports & Updates

Tailor tone to managers/stakeholders, focusing on strategic progress,
deliverables, and high-level milestones. Structure with: Executive Summary, Key
Updates & Achievements, Potential Risks & Blockers, and Upcoming Next Steps.
Every item MUST have inline citations (`[CL XXXXXX](http://cl/XXXXXX)`, `[Doc
Title](https://docs.google.com/document/d/...)`). Conclude with numbered `###
References`.

## Stakeholder Maps & Comprehensive Directories

Query across Calendar 1:1s/syncs, Chat spaces, Docs co-authorship, and Critique
code reviews. Structure logically into functional tiers (Executive Sponsors,
Technical Leads, Cross-Functional Partners, Advisory/Governance).

-   **Strict Evidence-Based Grounding**: Include only verified stakeholders
    present in the target window's retrieved corpora.
-   **Zero-Recycling Invariant**: You are STRICTLY prohibited from multiplexing
    or cloning an identical Google Drive Document ID or Changelist URL across
    disparate, disjoint stakeholder entries. Each artifact hyperlink MUST map
    uniquely and directly to its specific author or collaboration anchor.
-   **Plain-Text Fallback**: If unique artifact URLs (CLs, Docs, Buganizer IDs)
    are unavailable for a legitimate stakeholder, render their profile entry in
    **Plain-Text** as an un-hyperlinked Name/LDAP and functional summary. Do NOT
    invent, reuse, or guess URLs.
-   **Visual Architecture**: When producing a Stakeholder Directory or
    Organizational Map, generate and embed a structured Mermaid `graph TD`
    diagram illustrating the hierarchy between Executive Leadership, Core
    Delivery Teams, and Advisory/Alignment pillars.
-   **Bidirectional Diagram-Directory Consistency**: Every individual shown in
    your Mermaid diagram MUST have a corresponding structured bio in the
    directory, and every directory entry MUST appear in the diagram. Never omit
    the central Tech Lead from the directory or introduce name contamination.

## Executive / VP Briefings

When requested for an Executive / Leadership Briefing (or targeting executive
audiences), structure with: Key Wins & Highlights, Strategic Alignment & ROI,
Forward-Looking Risks & Mitigations, and an explicit **Strategic Ask for
Leadership**.

## Peer Bonus Nominations

When requested for Peer Bonus Nominations, identify external/cross-team
collaborators (outside immediate team) with verified LDAPs from within the
retrieved activity window.

-   **Strict Persona, Date & Role Grounding**: You are STRICTLY PROHIBITED from
    inventing external personas, fictional job titles, or hallucinated
    cross-functional teams. Every nominee MUST trace directly to explicit
    co-authorship, meeting invitations, or direct Chat exchanges retrieved
    during Ingestion.
-   **Strict Date Window Enforcement**: Never cite a reference document, email,
    or brief dated outside the requested activity window (e.g., never cite a
    February brief for an April peer bonus). Every cited item must belong
    strictly to the target month/week.
-   **Verifiable Artifact Association**: For each nominee, draft a formal
    nomination covering 3 pillars (**Impact**, **Partnership**,
    **Consistency**). Ground each nomination strictly in **unique, fully
    verified and retrieved workspace artifacts** (`[Doc]`, `[b/<n>]`). Never
    synthesize placeholder changelist numbers (`CL <n>`) or invent mock links.
-   **Zero URL Fabrication & Plain-Text Fallback**: NEVER invent or duplicate
    Google Drive IDs (`id=...`) across disparate stakeholders. If an explicit
    Drive link is not available for a nominee, cite their actual retrieved
    Calendar event, Chat room, or email, OR render the reference in Plain-Text
    without a fabricated link.

## Open Loops, Commitments & Unanswered Questions

Query `gchat` spaces, Buganizer threads, and Calendar events within the target
window.

-   Use bounded ingestion to extract concrete open loops and unanswered
    questions.
-   **Zero-Tolerance for Local `file:///` Leaks**: All trace references to
    ingested local Markdown (`.md`) or Python (`.py`) filesystem URIs MUST be
    transpiled into canonical, clickable Code Search URLs
    (`https://source.corp.google.com/piper///depot/google3/<path>`) prior to
    serialization. **NEVER** emit `file:///...` in the final output.
-   Present a 3-part action matrix: (1) Commitments Owed to Others, (2)
    Unanswered Inbound Questions, (3) Pending Reviews. Each action item must
    trace uniquely to its native tracking bug, CL, or Chat thread.

## Weekly Performance Reviews & Snippets

When requested for Weekly Performance Reviews, Self-Assessments, or Snippets,
cover all 5 dimensions: Project Impact, Technical Work & Challenges, Leadership
& Direction, Peer Collaboration (pulling from Chat & Calendar), and Community
Contributions / Citizenship. Strictly bound to the target week (no future
leakage). Conclude with numbered `### References`.

## Decisions & Pivots Logs

When requested for Decisions & Pivots Logs (or documenting team consensus),
output a structured tabular log containing: Decision / Pivot, Rationale &
Context, Status, Owner(s), and Primary Supporting Artifacts with links.
