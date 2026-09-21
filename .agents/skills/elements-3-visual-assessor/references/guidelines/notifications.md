# Notifications - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Notifications
(system alerts, push banners, and in-app messaging containers) across any design
system or platform, followed by Elements GM3 specifications for
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

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected
notifications against the following strict standards derived from Elements GM3
guidelines and the Google Notifications Platform (GNP):

### Elements GM3 & GNP Notification Specifications

Elements GM3 and GNP (which unifies Chime push, Gamma email, and GrowthKit
in-app campaigns) establish precise character limits and structural rules:

1.  **Container Geometry & Shapes**:
    -   *Inline Banners & Alerts*: Must use the **None (0dp)** shape style (straight corners) to maintain clean alignment with page margins.
    -   *Component-Level Callouts / Micro-Alerts*: Must use the **Small (8dp)** shape style to visually distinguish secondary notifications.
    -   *In-App Growth Cards / Card Alerts*: Must use the **Medium (12dp)** shape style.
    -   *Modal Alerts & Dialogs*: Must use the **Extra Large (28dp)** shape style (e.g. dialog containers).
2.  **Color Tokens**:
    -   *Informational Alerts*: Utilizes `--cee3-sys-color-extended-info-container` (or `--cee3-sys-color-extended-blue-container`) for background, with `--cee3-sys-color-extended-on-info-container` (or `--cee3-sys-color-extended-on-blue-container`) for text and icons.
    -   *Warning / Caution Alerts*: Utilizes `--cee3-sys-color-extended-warning-container` (or `--cee3-sys-color-extended-caution-container`) for background, with `--cee3-sys-color-extended-on-warning-container` (or `--cee3-sys-color-extended-on-caution-container`) for text and icons.
    -   *Critical / Error Alerts*: Utilizes `--cee3-sys-color-extended-red-container` (or `--cee3-sys-color-extended-error-container`) for background, with `--cee3-sys-color-extended-on-red-container` (or `--cee3-sys-color-extended-on-error-container`) for text and icons.
    -   *Success Alerts*: Utilizes `--cee3-sys-color-extended-success-container` (or `--cee3-sys-color-extended-green-container`) for background, with `--cee3-sys-color-extended-on-success-container` (or `--cee3-sys-color-extended-on-green-container`) for text and icons.
3.  **Character Limits (Mandatory Brevity)**:
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
4.  **Tone, Security, & Risk Communication**:
    -   *Critical Security Alerts*: If an account is compromised, notifications
        must convey risk urgently and tangibly using clear, confident terms
        (e.g., "critical", "risk", "urgent", "unrecognized", "suspicious",
        "immediately", "take action").
    -   *Non-Critical Alerts*: Must be informative and reassuring without
        causing undue alarm. Passive voice is acceptable to maintain legal and
        technical accuracy when attributing activity.
    -   *Tone & Emoji*: Products must avoid jokes, puns, or overdoing "delight".
        Emoji must be used with extreme caution (face/hand gestures perform
        best; negative emoji like frowning or weary faces must **never** be used
        to accentuate bad news).

### Critical Elements GM3 Violations to Flag

-   **Headline Truncation & Dynamic Text**: Exceeding the 29-character title
    limit or inserting dynamic text that clips with ellipses, permanently
    concealing vital alert context.
-   **Incorrect Container Shape**: Utilizing an inconsistent corner radius (e.g. rounded corners on a persistent banner that should have 0dp / None shape style, or straight corners on an in-app dialog that should have 28dp / Extra Large shape style).
-   **Excessive Body Length**: Overcrowding a notification banner with lengthy
    paragraphs (>40 chars collapsed / >80 chars expanded) that fail mobile
    accessibility constraints.
-   **Improper Tone or Negative Emoji**: Using casual jokes, puns, or
    negative/alarmist emoji (frowning faces) in critical or security-related
    notifications.
