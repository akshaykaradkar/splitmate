# Bottom Sheets - Universal AI Detection Guide & Google Material 3 Specifications

This reference provides universal visual heuristics for detecting Bottom Sheets (anchored bottom containers displaying secondary or supplementary content) across any design system or platform, followed by Google Material 3 (GM3/MD3) specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Bottom sheet** is a dedicated structural container anchored at the bottom of a screen or window, used to present supplementary content, menus, or secondary actions. Agents must detect bottom sheets based on their distinct geometric and structural qualities, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Anchoring**: Anchored flush to the bottom of the screen or window boundary. In compact window sizes (mobile), it typically spans the full width of the screen. In larger window sizes (tablet/desktop), it may be centered or inset with visible side margins.
-   **Visual Boundaries & Containment**: Bottom sheets are distinct containers that rise over the primary page content. They are visually defined by a solid background fill, a top elevation shadow, or a background-dimming overlay (scrim) covering the rest of the screen.
-   **Corner Geometry**: Characterized by distinct rounded top corners (upper-left and upper-right) while the bottom corners remain straight/flush against the bottom edge of the screen.
-   **Core Anatomy & Child Elements**:
    -   *Drag Handle (Optional)*: A small horizontal pill or line centered at the extreme top of the sheet container, indicating the sheet can be dragged or expanded.
    -   *Header / Title*: An optional top bar or title area containing a headline and/or close button.
    -   *Content Area*: Variable layouts containing list items, grid menus, text, cards, or media (thumbnails, images, video) that provide supplementary information or choices.
-   **Mandatory Container Mapping**: Agents MUST bound and classify the outer sheet container as `Bottom sheet`. Agents must not merely map the internal child elements (such as lists, buttons, or cards) while omitting the parent sheet container.

---

## Part 2: Google Material 3 Specifications & Compliance Auditing

When conducting a Google Material 3 (GM3/MD3) adherence audit, evaluate detected bottom sheets against the following standards:

### Google Material 3 Visual & Behavioral Rules

-   **Bottom Sheet Variants**:
    -   **Modal Bottom Sheet**: Appears in front of primary app content, disabling background functionality with a dark overlay scrim mapped to `--md-sys-color-scrim`. Uses elevation `--md-sys-elevation-level1` (1px) and container color `--md-sys-color-surface-container-low` (or standard surface roles).
    -   **Standard Bottom Sheet**: Co-exists with the main UI without a scrim, allowing simultaneous viewing and interaction with both regions (e.g., a persistent audio playback bar).
-   **Corner Radius & Shape**: 
    -   Must use rounded top corners and sharp bottom corners, mapped to `--md-sys-shape-corner-extra-large-top` which evaluates to a **28px top corner radius** (`28px 28px 0 0`) and `0px` bottom corners.
-   **Color & Visual Separation**:
    -   *Modal*: Visual separation is achieved via the scrim overlay (`--md-sys-color-scrim`) and a subtle elevation shadow.
    -   *Standard (No Scrim)*: Must use a higher surface container color role (such as `--md-sys-color-surface-container` or `--md-sys-color-surface-container-high` over the underlying `--md-sys-color-surface-container-low` / `--md-sys-color-surface-dim` background) to ensure clear visual separation without a scrim.
-   **Drag Handle Specs**: 
    -   An optional drag handle must be horizontally centered at the top of the sheet, featuring `--md-sys-measurement-space200` (16px) padding top and bottom. 
    -   The top 48dp portion of the bottom sheet acts as the accessible tap target for the drag handle.
-   **Responsive Layout Measurements**:
    -   *Compact Windows (Mobile, <640dp)*: Spans 100% of the screen width up to a maximum width of 640px. The top margin must be at least `--md-sys-measurement-space900` (72px) from the top of the screen to prevent full-screen blocking at resting height.
    -   *Medium/Expanded Windows (>640dp)*: Bottom sheets must not exceed 640px in width. They adjust to have clearly visible `--md-sys-measurement-space700` (56px) side margins and a `--md-sys-measurement-space700` (56px) top margin, rather than touching screen edges.
-   **Behavior & Dismissal**:
    -   Modal sheets are initially capped at 50% screen height but can be pulled full-screen if content exceeds that height.
    -   Can be dismissed by tapping an internal action, tapping the scrim overlay, swiping down, or selecting a close icon button.
    -   *GAR 2025 Accessibility Requirement*: Bottom sheets must be dismissible or resizable without dragging (a single-pointer alternative is required). On Android, users can select the drag handle (role "button") to cycle through available preset heights, including the dismissed state. Alternatively, a visible close icon button must be provided.

### iOS Platform Specifics (iOS 26 Liquid Glass Update)

-   **Glass Effects (Liquid Glass)**:
    -   *At Half Height*: The sheet is inset from the screen edges and uses a translucent glass effect (Material Glass with semi-transparency and specular highlights), sitting in the *functional layer*.
    -   *At Full Height*: The sheet turns opaque (relying on solid surface color tokens) and extends fully to the screen edges, transitioning to the *content layer*.

### Critical Google Material 3 Violations to Flag

-   **Missing Top Corner Radii**: Presenting a bottom sheet with sharp, 0dp top corners instead of the required `--md-sys-shape-corner-extra-large-top` (28px).
-   **Improper Margins on Large Screens**: Expanding a bottom sheet to full width on screens wider than 640px without applying the mandatory 56px side margins.
-   **Lacking Visual Separation**: Standard bottom sheets that blend entirely into the page background without a distinct container color change (e.g. failing to use a higher surface container role).
