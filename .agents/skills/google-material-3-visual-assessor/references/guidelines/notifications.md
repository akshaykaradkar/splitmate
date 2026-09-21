# Notifications - Universal AI Detection Guide & MD3 Specifications

This reference provides universal visual heuristics for detecting Notifications
(system alerts, push banners, and in-app messaging containers) across any design
system or platform, followed by Material Design 3 (MD3) specifications for
compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Notification** is a dedicated messaging container designed to deliver
timely, precise, and actionable information to users (e.g., system alerts, push
notifications, security warnings, or in-app feature discovery). Agents must
detect notifications based on placement anchoring, container lockups, and
collapsible body structures, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Anchoring**: Notifications appear in high-visibility system or
    in-app locations:
    -   *System / Push Banners*: Anchored at the upper leading or top edge of
        the screen, frequently overlaying the OS status bar or dropping down as
        temporary floating banners.
    -   *In-App / Growth Campaigns*: Displayed as prominent inline banners,
        modal survey cards (e.g., HaTS), or floating permission prompts within
        the app layout.
-   **Visual Boundaries & Containment**: Defined by distinct rectangular or
    elevated card-like bounding boxes featuring a solid background fill, rounded
    corners, and subtle drop shadows to separate them from underlying content.
-   **Core Anatomy & Collapsible Structure**: Notifications follow a standard
    multi-level layout hierarchy:
    -   *Header / App Identity*: A leading app icon or brand mark paired with a
        concise app name and timestamp.
    -   *Title / Headline*: Bold, concise text summarizing the alert.
    -   *Body Text*: Supporting descriptive text. Many notifications support
        expandable/collapsible states (displaying a short preview when
        collapsed, expanding to reveal full text).
    -   *Actions (Optional)*: Up to two action buttons (e.g., "Reply",
        "Archive", "Accept") aligned to the bottom trailing edge.

--------------------------------------------------------------------------------

## Part 2: MD3 Specifications & Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected
notifications against the following strict standards derived from Material
guidelines and the Google Notifications Platform (GNP):

### MD3 & GNP Notification Specifications

Material Design 3 and GNP (which unifies Chime push, Gamma email, and GrowthKit
in-app campaigns) establish precise character limits, structural rules, and token bindings:

1.  **Character Limits (Mandatory Brevity)**:
    -   **Title (Headline)**: Strictly **<29 characters**. Critical information
        must be placed at the very beginning of the sentence to prevent
        truncation. Products must avoid dynamic text (user/campaign names) in
        headlines, as it frequently causes truncation when translated.
    -   **Collapsed Body**: Strictly **<40 characters**.
    -   **Expanded Body**: Strictly **<80 characters** (constructed by appending
        details to the collapsed body).
    -   **Buttons**: Maximum of 1 to 2 buttons (1 to 2 words each).
    -   **SMS Notifications**: Strictly <160 characters for Latin languages
        (<134 characters for non-Latin languages) to prevent costly
        multi-message splitting.
2.  **Color, Tone, & Typography Tokens**:
    -   **Container Color**: In-app notifications/banners utilize `--md-sys-color-surface-container-high` (`light-dark(#e9eef6, #282a2c)`) or `--md-sys-color-surface-container` (`light-dark(#f0f4f9, #1e1f20)`).
    -   **Headline Typography**: Matches `var(--md-sys-typescale-emphasized-title-medium)` (700 1rem/1.5rem 'Google Sans Text') or `var(--md-sys-typescale-title-medium)`. Text color must be `--md-sys-color-on-surface` (`light-dark(#1f1f1f, #e3e3e3)`).
    -   **Supporting Body Typography**: Matches `var(--md-sys-typescale-body-medium)` or `var(--md-sys-typescale-body-small)`. Text color must be `--md-sys-color-on-surface-variant` (`light-dark(#444746, #c4c7c5)`).
    -   **Notification Actions**: Action buttons must be low-emphasis text buttons utilizing the `--md-sys-color-primary` (`light-dark(#0b57d0, #a8c7fa)`) role. Typography is `var(--md-sys-typescale-label-large)` (`500 0.9rem/1.3rem 'Google Sans Text'`).
3.  **Critical Security Alerts & Risk Communication**:
    -   If an account is compromised, notifications must convey risk urgently and tangibly.
    -   **Security Tokens**: Containers use `--md-sys-color-error-container` (`light-dark(#f9dedc, #8c1d18)`) as background, with text and icon colors mapping to `--md-sys-color-on-error-container` (`light-dark(#8c1d18, #f9dedc)`) and critical alert indicators mapped to `--md-sys-color-error` (`light-dark(#b3261e, #f2b8b5)`).
    -   **Tone Rules**: Tone must be clear and confident (using terms like "critical", "risk", "urgent", "unrecognized", "suspicious", "immediately", "take action").
    -   *Non-Critical Alerts*: Must be informative and reassuring without
        causing undue alarm. Passive voice is acceptable to maintain legal and
        technical accuracy when attributing activity.
    -   *Tone & Emoji*: Products must avoid jokes, puns, or overdoing "delight".
        Emoji must be used with extreme caution (face/hand gestures perform
        best; negative emoji like frowning or weary faces must **never** be used
        to accentuate bad news).
4.  **Geometry, Elevation & Spacing**:
    -   **Container Shape**: Corner rounding must use `--md-sys-shape-corner-large` (16px) or `--md-sys-shape-corner-medium` (12px) for overlay/floating banners.
    -   **Elevation**: Floating alerts must use `--md-sys-elevation-level3` (6px) or `--md-sys-elevation-level4` (8px), styled with `--md-sys-color-shadow` shadow filters.
    -   **Margins & Gaps**: Grid padding inside notifications must use `--md-sys-measurement-space200` (16px) for outer boundaries, and `--md-sys-measurement-space100` (8px) for separating sibling elements (e.g., text from actions).

| Element | Typography / Design Token | Color Role | Geometry / Spacing |
| :--- | :--- | :--- | :--- |
| **Notification Container** | — | `var(--md-sys-color-surface-container-high)` | Corner radius: `var(--md-sys-shape-corner-large)` (16px); Elevation: Level 3 (6px) |
| **Headline / Title** | `var(--md-sys-typescale-emphasized-title-medium)` | `var(--md-sys-color-on-surface)` | Max 29 characters; Padding-bottom: `var(--md-sys-measurement-space50)` (4px) |
| **Body Content** | `var(--md-sys-typescale-body-medium)` | `var(--md-sys-color-on-surface-variant)` | Max 40 chars (collapsed) / 80 chars (expanded) |
| **Action Buttons** | `var(--md-sys-typescale-label-large)` | `var(--md-sys-color-primary)` | Max 2 actions; sentence case |
| **Critical Alarm Container** | — | `var(--md-sys-color-error-container)` | Text color: `var(--md-sys-color-on-error-container)` |

### Critical MD3 Violations to Flag

-   **Headline Truncation & Dynamic Text**: Exceeding the 29-character title
    limit or inserting dynamic text that clips with ellipses, permanently
    concealing vital alert context.
-   **Excessive Body Length**: Overcrowding a notification banner with lengthy
    paragraphs (>40 chars collapsed / >80 chars expanded) that fail mobile
    accessibility constraints.
-   **Improper Tone or Negative Emoji**: Using casual jokes, puns, or
    negative/alarmist emoji (frowning faces) in critical or security-related
    notifications.
-   **Incorrect Color Role for Urgency**: Using generic warning or information colors for critical account compromises rather than the official `--md-sys-color-error-container` alert tokens.
-   **Incorrect Outer Geometry**: Using sharp/unrounded corners (0dp) or overly circular pill corners (full) for the notification banner shape rather than standard `var(--md-sys-shape-corner-large)` (16px).
