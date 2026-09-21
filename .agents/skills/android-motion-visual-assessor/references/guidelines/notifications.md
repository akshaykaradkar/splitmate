# Notifications - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Notifications
(system alerts, push banners, and in-app messaging containers) across any design
system or platform, followed by Android Motion specifications for
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

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion adherence audit, evaluate detected
notifications against the following strict standards derived from Android Motion guidelines:

### Android Motion Notification Specifications & Token Architecture

Android Motion establishes precise character limits, structural layout, typography scales, and motion parameters for notifications:

1.  **Character Limits (Mandatory Brevity)**:
    -   **Title (Headline)**: Strictly **<29 characters**. Critical information must be placed at the very beginning of the sentence to prevent truncation. Products must avoid dynamic text (user/campaign names) in headlines, as it frequently causes truncation when translated.
    -   **Collapsed Body**: Strictly **<40 characters**.
    -   **Expanded Body**: Strictly **<80 characters** (constructed by appending details to the collapsed body).
    -   **Buttons**: Maximum of 1 to 2 buttons (1 to 2 words each).
    -   **SMS Notifications**: Strictly <160 characters for Latin languages (<134 characters for non-Latin languages) to prevent costly multi-message splitting.
2.  **Color & Token Mapping**:
    -   **Containers**: Use `--droid-sys-color-surface` (light: `#fdfcfb`, dark: `#1f1f1f`) or `--droid-sys-color-surface-variant` (light: `#e1e3e1`, dark: `#444746`).
    -   **Text Content**: Headers and body use `--droid-sys-color-on-surface` (light: `#1f1f1f`, dark: `#e3e3e3`) or `--droid-sys-color-on-surface-variant` (light: `#444746`, dark: `#c4c7c5`) for subtle text.
    -   **Accent Elements / Badges**: Use `--droid-sys-color-primary` (light: `#0b57d0`, dark: `#a8c7fa`) or `--droid-sys-color-error` (light: `#b3261e`, dark: `#f2f2f2`) for critical alerts.
    -   **Shapes**: Notification containers must apply the `--droid-sys-shape-corner-large` (16px) or `--droid-sys-shape-corner-medium` (12px) shape tokens.
3.  **Typography**:
    -   **Title / Headline**: Styled using `--droid-sys-typescale-title-small` (`500 0.9rem/1.3rem 'Google Sans Text'`) or `--droid-sys-typescale-title-medium` (`500 1rem/1.5rem 'Google Sans Text'`).
    -   **Body Text**: Styled using `--droid-sys-typescale-body-medium` (`0.9rem/1.3rem 'Google Sans Text'`).
    -   **Buttons**: Styled using `--droid-sys-typescale-label-large` (`500 0.9rem/1.3rem 'Google Sans Text'`).
4.  **Tone, Security, & Risk Communication**:
    -   *Critical Security Alerts*: If an account is compromised, notifications must convey risk urgently and tangibly using clear, confident terms (e.g., "critical", "risk", "urgent", "unrecognized", "suspicious", "immediately", "take action").
    -   *Non-Critical Alerts*: Must be informative and reassuring without causing undue alarm. Passive voice is acceptable to maintain legal and technical accuracy when attributing activity.
    -   *Tone & Emoji*: Products must avoid jokes, puns, or overdoing "delight". Emoji must be used with extreme caution (face/hand gestures perform best; negative emoji like frowning or weary faces must **never** be used to accentuate bad news).
5.  **Motion & Fluid Transitions**:
    -   **Entrance**: Slid down from the top edge using a duration of `--droid-sys-motion-duration-300` (300ms) or `--droid-sys-motion-duration-350` (350ms) paired with `--droid-sys-motion-easing-emphasized-decelerate` (`cubic-bezier(0.05, 0.7, 0.1, 1.0)`) for a smooth, natural settle.
    -   **Expand / Collapse**: Expanding the notification body to reveal detailed information or actions must morph the container height over `--droid-sys-motion-duration-250` (250ms) with the standard `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) curve, preserving layout flow.

### Critical Android Motion Violations to Flag

-   **Headline Truncation & Dynamic Text**: Exceeding the 29-character title limit or inserting dynamic text that clips with ellipses, permanently concealing vital alert context.
-   **Excessive Body Length**: Overcrowding a notification banner with lengthy paragraphs (>40 chars collapsed / >80 chars expanded) that fail mobile accessibility constraints.
-   **Improper Tone or Negative Emoji**: Using casual jokes, puns, or negative/alarmist emoji (frowning faces) in critical or security-related notifications.
-   **Hardcoded Fonts or Shapes**: Bypassing `--droid-sys-typescale-...` fonts or ignoring `--droid-sys-shape-corner-large` container rounding.
