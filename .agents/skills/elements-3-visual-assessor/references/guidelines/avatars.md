# Avatars - Universal AI Detection Guide \& Elements GM3 Specifications

This reference provides visual heuristics for detecting Avatars (representing user profiles, account icons, or people entities) across any user interface, followed by the strict **Elements GM3** specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

An **Avatar** is a high-level visual container specifically dedicated to representing a person, teammate, user account, or organizational entity. Agents must detect avatars based on their structural, geometric, and context-dependent characteristics, regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **Placement \& Contextual Anchoring**:
    -   *App Bars / Search Bars*: Positioned at the far trailing edge (top-right quadrant) as the account profile particle, serving as the gateway to user account management.
    -   *Teammate / People Cards*: Large, prominent central or top-left visual focus within popover hover cards (e.g., standard Google People Cards).
    -   *List Items / Tables*: Placed at the far leading edge (extreme left) of a list row to identify a teammate, sender, or assignee.
    -   *Chips*: Embedded as a miniature leading visual mask (e.g., within People or Input Chips).
-   **Visual Boundaries \& Shape Geometry**: Standard circular masks or highly rounded shapes. They may have a fine outer border/stroke, a solid background (for initials/monograms), or act as a clean clipping frame for photographic portrait images.
-   **Core Anatomy \& Content Mappings**:
    -   *Photographic*: Custom user photo pulled from Momateams/Moma profiles.
    -   *Monogram / Textual*: Large single or double user initials centered over a solid, high-contrast background container.
    -   *Iconic / Fallback*: A generic silhouette symbol or person icon when a photo or name is unavailable.

---

## Part 2: Elements GM3 Specifications \& Compliance Auditing

In the **Elements GM3** design system, avatars follow strict accessibility, layout, and behavioral constraints tailored for corporate enterprise and internal applications. Evaluate detected avatars against these standards:

### 1. App Bar Profile Integration

-   **Visual Placement**: Always positioned at the extreme far-right (trailing edge) of the top app bar.
-   **ARIA Dialog Interaction**:
    -   Clicking the avatar button MUST open a **non-modal ARIA Dialog** (not a standard dropdown menu).
    -   **Keyboard focus must be trapped** within this profile dialog when active.
    -   An explicit close button must be provided in the tab order, which is visually styled to look like the avatar button itself to maintain visual elegance.
-   **Content Limit**: The profile menu must be concise, limiting interactive links to a maximum of **5 links** (such as linking to the user's Momateams profile page).
-   **Action Sequence Constraints**: Avatars must align with the fixed right-to-left utility bar order (`Profile -> Settings -> Support -> Custom Actions`). Excessive trailing controls are prohibited (maximum of 6 total app bar actions).

### 2. People Card Integration

-   **Hover Card Component**: In Elements GM3, hovering or interacting with an avatar or teammate's name triggers a **People Card** (a primitive maintained by the People System team).
-   **Accessible Entry Points**:
    -   *Mouse*: Opened via mouse hover.
    -   *Keyboard*: Opened via the custom shortcut **Alt + Right Arrow**. Since this shortcut is not natively discoverable, products using the People Card must explicitly document this shortcut in help files or shortcut lists.
-   **Keyboard Navigation within Card**: Once the card is open, focus is *not* trapped (unlike the App Bar dialog). Keyboard users navigate items using `Tab`.
-   **Dismissing the Card**: The card is dismissed using `Esc` (which returns focus to the trigger) or by tabbing/shift-tabbing out.

### 3. Chip Integration Specs

-   **Pill \& Input Chip Alignment**: When embedded inside a standard Input Chip or People Chip, the avatar standard size is **24dp** with a fully rounded, circular shape.
-   **Visual Balance**: The avatar must align perfectly with the leading edge of the chip, maintaining a standard margin of 4dp to the chip container.

### 4. Accessibility and Labeling

-   **Moma/Teams Integration**: The avatar must pull from the verified Google profile photo.
-   **No Name Fallbacks**: If the People API cannot pull the user's name quickly, the user menu may exclude the display of the logged-in user's name, but the visual avatar avatar remains active.

---

## Critical Elements GM3 Violations to Flag

1.  **Improper App Bar Profile Menu (Standard Menu Role)**: Implementing the profile dropdown as a standard ARIA menu instead of a focus-trapped, non-modal ARIA Dialog.
2.  **Lack of Keyboard Shortcut Documentation**: Incorporating the People Card (Alt + Right Arrow) without documenting the shortcut for keyboard users.
3.  **Conflating Avatars with General Thumbnails**: Applying circular avatar masking to non-person content (like a file preview, product image, or generic video thumbnail), which violates shape semantics.
4.  **Excessive App Bar Action Trailing Items**: Violating the fixed sequence on the right-hand side or having more than 6 total actions crowding the user avatar.
