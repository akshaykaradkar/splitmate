---
name: gbreeze-energy
description: >-
  Prototypes and code for Material Design web user interfaces using the gBreeze Energy component (`md-gb-energy`) for animated AI surfaces, auroras, and Gemini glows. Use when a UI needs an animated AI-state surface or when the user mentions "energy", "aurora", "gradient", "AI glow/shimmer", "Gemini glow", or an "AI thinking/processing/responding state". Don't use for general gBreeze layout/form components (use the gbreeze skill).
---

# gBreeze Energy

gBreeze Energy (`<md-gb-energy>`) is an animated Material Design surface effect
used to signal AI states (including anticipating, receiving, processing, and
responding). It is Google Material 3 (GM3) / Google-internal only.

## Thinking Path (State Resolution & Rendering Algorithm)

1.  **IF** an invalid state is requested (e.g., `static`): **STOP** and list the
    5 valid states (`idle`, `anticipating`, `receiving`, `processing`,
    `responding`).
2.  **ELSE IF** the prompt is text-only with no state specified (and not asking
    for all/multiple states): **STOP** and ask the user which valid state to
    use.
3.  **ELSE IF** a visual UI mockup/screenshot is provided with no explicit text
    state: **INFER** the valid state immediately from visual cues (do not stop
    to ask).
4.  **ELSE**: **RENDER** a single `<md-gb-energy active state="...">` instance
    (never nest or stack multiple WebGL instances) with concentric host clipping
    and full-bleed backdrop sizing.

## Core Directives & Rationale

*   **What `<md-gb-energy>` is**: It is a custom web element (`<md-gb-energy>`)
    that initializes a WebGL2 canvas rendering real-time fluid/energy shaders.
*   **Validate energy states before generating code**: The only valid energy
    states for interactive UI are `idle`, `anticipating`, `receiving`,
    `processing`, and `responding` (see `EnergyState` in
    `google3/javascript/materialdesign/lib/aurora/energy/types.ts`). `static` is
    not a valid state for interactive UI.
    -   **Specifying all states or multiple states is valid**: If the user asks
        for "all states", "all energy states", or specifies multiple valid
        states (for example, asking for `receiving` and `responding`, or
        requesting a switcher for all states), this is completely valid! In this
        case, do **NOT** prompt the user to pick just one state. Instead,
        proceed with generating the prototype using a single `<md-gb-energy>`
        element and a state switcher or controller to toggle between the
        requested states (or all five states if "all states" was requested).
    -   **If no state is specified (Text-Only Prompts)**: If the user asks in a
        text-only prompt to add energy (or aurora, Gemini glow, etc.) without an
        uploaded UI mockup/screenshot and does **NOT** specify an energy state
        (nor asks for all states or multiple states), do **NOT** guess or
        default to a state, and do **NOT** generate code. Instead, tell the user
        what the valid energy states are (`idle`, `anticipating`, `receiving`,
        `processing`, `responding`) and ask the user which state they would like
        to use.
    -   **Visual Mockup / Screenshot Exception (Infer State Immediately)**: When
        the user provides an uploaded UI mockup or screenshot (e.g., via
        `/genux` or a visual prototyping request) and does not explicitly name
        an energy state in text, **do NOT stop to ask the user**. Instead,
        immediately infer the appropriate valid state from the visual cues in
        the image (`state="receiving"` with simulated voice `dynamicIntensity`
        when a microphone/voice-capture UI is shown; `state="processing"` when a
        `[Processing]` / `[Thinking]` status pill is shown; `state="responding"`
        when streaming AI output is shown; `state="anticipating"` with `1800ms`
        auto-transition to `idle` for entry cues) and proceed directly with
        implementation.
    -   **If an invalid state is requested**: If the user requests an invalid or
        unrecognized energy state (such as "finalizing", "thinking",
        "listening", "generating", etc.), or if they ask for `static`—including
        when multiple states are requested and one or more of them is invalid
        (e.g., 'idle', 'thinking', and 'responding')—do **NOT** generate code
        (do not silently omit the invalid state or guess a replacement).
        Instead, point out the invalid state(s), tell the user what the valid
        energy states are (`idle`, `anticipating`, `receiving`, `processing`,
        `responding`), and ask the user for a valid state.
*   **Only generate a state picker if more than one state is requested**:
    -   If the user asks for one specific energy state, do **NOT** generate a
        picker or switcher to pick other states. Only generate the energy effect
        in the requested state.
    -   If the user asks for more than one energy state or asks for "all
        states", generate a state switcher that allows switching between those
        states. When "all states" is requested, include all valid interactive
        states (`idle`, `anticipating`, `receiving`, `processing`, and
        `responding`). When specific multiple states are requested (e.g.,
        `receiving` and `responding`), include those requested states in the
        switcher.
*   **Do NOT give energy states alternate names**: Only use the actual names of
    the energy states: `idle`, `anticipating`, `receiving`, `processing`, and
    `responding`. Do **NOT** invent, display, or include alternate names,
    aliases, or parenthetical descriptors (such as "Receiving (Listening)",
    "Anticipating (Cue)", "Processing (Thinking)", or "Responding (Speaking)")
    in UI controls (e.g., state switchers), UI labels, or code.
*   **Distinguish Clickable Action Buttons (`<md-gb-button>`) from
    Non-Interactive AI Status Pills / Thinking Banners**:
    -   **IF the element is a clickable user action button or interactive
        card**: ALWAYS use `<md-gb-button>` or `<md-gb-card>` with
        `slot="container"` (e.g., `<md-gb-button color="filled"><md-gb-energy
        slot="container" active
        state="processing"></md-gb-energy>Generate</md-gb-button>`), rather than
        generic HTML `<button>` elements. Import their component modules
        (`@material/web/labs/gb/components/button/md-gb-button.js` and
        `@material/web/labs/gb/components/card/md-gb-card.js`).
    -   **IF the element is a non-interactive AI status indicator, thinking
        banner, or read-only status pill** (e.g., `[🚀 Processing]`, `[✨
        Gathering information]`, or an inline reasoning status bar inside a chat
        response): You **MUST NOT** wrap it in `<md-gb-button>` or `<button>`!
        Wrapping read-only status indicators in `<md-gb-button>` adds false
        button affordances (hover ripples, pointer cursor, button ARIA role).
        Instead, ALWAYS use either `<md-gb-card>` with `slot="container"` OR a
        non-interactive relative pill container (`<div role="status"
        class="relative inline-flex items-center gap-s100 px-s200 py-s100
        rounded-full overflow-hidden bg-surface-container text-on-surface">`)
        containing `<md-gb-energy class="absolute inset-0 pointer-events-none"
        active state="processing" style="--color:
        var(--md-sys-color-surface-container);"></md-gb-energy>` behind a `<span
        class="relative z-10 flex items-center gap-s100">` foreground wrapper.
*   **`state="anticipating"` → `state="idle"` Auto-Transition Lifecycle
    (`1800ms`)**:
    -   `state="anticipating"` is a transient entry/attention cue (e.g., when a
        prompt bar, sheet, or AI card first appears or text is selected) and
        **MUST NOT** loop indefinitely. Unless the user explicitly requests a
        static state switcher to inspect `anticipating`, ALWAYS schedule an
        automatic `setTimeout` transition from `state="anticipating"` to
        `state="idle"` after `1800ms` (`setTimeout(() => { energyEl.state =
        'idle'; }, 1800);`) so the entry cue settles naturally into its resting
        state.
*   **Demonstrating `intensity` Differences (`0` vs `1`) with a Single
    `<md-gb-energy>` Instance**:
    -   In `state="processing"`, the shader maintains a baseline animation floor
        so `intensity="0"` and `intensity="1"` still share baseline movement.
        Furthermore, you **MUST NOT** render two `<md-gb-energy>` elements
        side-by-side to compare `intensity`. To demonstrate or toggle
        `intensity` differences (`0` vs `0.5` vs `1`), ALWAYS use a **single**
        `<md-gb-energy>` instance on the screen and provide `<md-gb-button
        color="outlined" size="sm">` controls that update `energyEl.intensity`
        (and if `intensity === 0` should rest completely, set `energyEl.state =
        'idle'` or toggle `energyEl.intensity = 0` vs `1` on that single
        element).
*   **Do NOT add extra colors to a component with energy**: Do not add extra
    colors (such as custom background colors, colored borders, background tints,
    gradient scrims, or extra colored overlays) to a component or container with
    energy. (Rationale: `<md-gb-energy>` renders real-time WebGL shader colors.
    Adding extra background colors or colorful styling to the host component or
    container clashes with the shader physics, causes harsh color clipping
    artifacts, and washes out or obscures the energy glow). The base container
    color should be controlled exclusively via the `--color` CSS custom property
    (e.g., `--color: var(--md-sys-color-surface)`), leaving the shader to
    produce the fluid color accents or hue rotation.
*   **Do NOT write custom CSS gradients or background-blobs**: Never use
    hand-rolled CSS gradients (`linear-gradient`, `radial-gradient`), blurred
    divs, or CSS keyframe animations when a prompt asks for energy, aurora,
    Gemini glow, or AI gradient states, instead of `md-gb-energy`. (Rationale:
    Hand-rolled gradients cannot replicate the real-time WebGL2 shader physics
    of `<md-gb-energy>`). Also, don't use them in an element that is a
    descendant or ancestor of the `md-gb-energy` element (because they can
    obscure the energy effect).
*   **Simulating Voice for Receiving and Responding States**: If the "Receiving"
    or "Responding" energy state is being used, then always either add simulated
    voice or hook up the prototype to a real microphone, so that the energy
    animates. Simulated voice should be gentle (peaking around 0.4–0.6) with
    natural pauses (~0.5–1.5s), rather than constantly fluctuating at peak
    intensity. See the "Dynamic Intensity, Voice Simulation & Microphone Input"
    section in [Usage Examples](references/energy_usage_examples.md) for example
    code. (Rationale: without simulated or real voice, the "Receiving" and
    "Responding" states look lifeless, but constant peak intensity looks hectic
    and unnatural.)
*   **Do NOT display sliders, numeric values, or meters for dynamic intensity**:
    When modulating `dynamicintensity` (whether via simulated voice or live
    microphone), do **NOT** display a slider (`<input type="range">`), numeric
    or percentage text value (e.g., `0.5` or `50%`), volume meter, or level bar
    in the UI. Dynamic intensity must adjust purely under the covers in code.
    (Rationale: Dynamic intensity is an internal animation parameter driving
    shader physics, not a user-facing setting or telemetry display; exposing
    controls or numbers clutters the UI and distracts from the natural energy
    effect.)
*   **Do NOT use `intensity` to simulate voice input**: Never manipulate or
    animate the `intensity` parameter to simulate voice input or output. Only
    the `dynamicintensity` attribute / `.dynamicIntensity` property should be
    manipulated to mimic voice input (e.g., microphone) or voice output/speech.
    Furthermore, note that `dynamicintensity` only has an effect in the
    `receiving` and `responding` states; it has no effect in other states
    (`idle`, `anticipating`, `processing`).
*   **At most one energy element per page or screen**: Energy should only be
    added to at most one element in a web page or screen. Never render multiple
    `<md-gb-energy>` components simultaneously on the same page or screen.

## Quick Start

Load the self-registering bundle with a plain `<script src>` tag from SCS (never
via importmap or module import):

```html
<script src="https://static.corp.google.com/material-web/gbreeze/latest/components/energy/md-gb-energy.js"></script>
```

Also, link the `gm3.css` style sheet:

```html
<link rel="stylesheet" href="https://static.corp.google.com/material-web/gbreeze/latest/styles/gm3.css">
```

## `md-gb-energy` Component Specification

### Rendering requirements

Energy draws with WebGL2 on a `<canvas>`, so it needs:

*   **The `active` attribute**: toggles the energy effect on or off. Set
    `active` explicitly (`<md-gb-energy active>`) to initialize and preload the
    shader. Without `active`, the element never initializes at all.
*   **A non-idle `state` attribute**: while `active` initializes and preloads
    the shader, the default state is `"idle"`, which rests the animation.
    Therefore, **two attributes are required for energy to visually appear**:
    `active` (to load the shader) and a non-idle `state` (such as
    `state="receiving"`, `state="anticipating"`, or `state="processing"`).
*   **HTTP, not `file://`**: serve through a local web server.
*   **An authenticated corp browser (`gbrowser`)**: the SCS bundle is auth-gated
    (`static.corp.google.com`). In automated sessions, use `gbrowser` or an
    authenticated browser session to avoid 404/auth load failures.
*   **A working WebGL2 context**: Remote Linux Cloudtop desktops/VMs often lack
    GPU acceleration, causing `getContext('webgl2')` to return `null` and throw
    `Error: Failed to initialize WebGL context`, which leaves a flat base
    `--color` surface. On local laptops/workstations with native GPU support,
    WebGL2 renders fully. Check console logs for WebGL context failures.

### Attributes and properties

The `md-gb-energy` web component has the following attributes and associated
JavaScript properties. (Below, attributes are in `fixed width` font, while
properties are also in `fixed width` font but prefixed with a period.)

*   `active` / `.active` (boolean)
    -   Default: `false`
    -   Toggles the energy effect on/off. Must be set when rendering an active
        state.
*   `type` / `.type` (`'accents' | 'hue'`)
    -   Default: `'accents'`
    -   `accents` derives energy colors from `--md-sys-color-*` tokens
    -   `hue` uses hue rotation based on the `--color` CSS custom property (see
        below)
    -   `accents` is heavily preferred over `hue`
*   `baseline` / `.baseline` (boolean)
    -   Default: `false`
    -   Whether to use the GM3 baseline color palette
*   `state` / `.state` (`EnergyState` in
    `google3/javascript/materialdesign/lib/aurora/energy/types.ts`)
    -   Default: `'idle'`
    -   The energy state: `'idle'`, `'anticipating'`, `'receiving'`,
        `'processing'`, or `'responding'`. Must be one of these 5 valid states.
        Do not invent alternate states or use alternate names.
    -   Use `idle` to rest the effect.
*   `intensity` / `.intensity` (number from 0 to 1, inclusive)
    -   Default: `0.5`
    -   Intensity of the energy effect. Do NOT manipulate this parameter to
        simulate voice input or output (use `dynamicintensity` instead).
*   `dynamicintensity` / `.dynamicIntensity` (number from 0 to 1, inclusive)
    -   Default: `0`
    -   Added animation on top of the `intensity` of the energy effect. Used to
        represent voice (either real or simulated) in the `receiving` and
        `responding` states. Note that `dynamicintensity` only has an effect in
        the `receiving` and `responding` states.
    -   Do NOT display sliders, numeric/percentage values, or level meters in
        the UI for dynamic intensity; it should always change under the covers.
    -   Keep simulated voice intensity gentle (peaking around 0.4–0.6 with
        natural pauses of ~0.5–1.5s) rather than constantly fluctuating at high
        intensity.
    -   Note that the attribute is all lower case, but the property is camel
        case.

### CSS custom properties

*   `--color`
    -   Default: `var(--md-sys-color-surface)`
    -   The base background color of the energy surface. Defaults to the surface
        color role. When placing an energy effect inside a primary or colored
        container, set `--color: var(--md-sys-color-primary)` (or
        `--md-sys-color-tertiary`, etc.) so the base color matches the container
        role.

### Slots & Host Selection

*   The `md-gb-energy` component has a default slot, which contains the content
    to be displayed on a standalone energy surface.
*   **Clickable Buttons & Cards (`slot="container"`)**: The `md-gb-button` and
    `md-gb-card` components each have a `container` slot (`<md-gb-button
    color="filled"><md-gb-energy slot="container" active
    state="processing"></md-gb-energy>Generate</md-gb-button>`). Use
    `<md-gb-button>` **only** when the element is an interactive user action
    button.
*   **Non-Interactive AI Status Pills / Thinking Banners**: When rendering a
    read-only status indicator (`[🚀 Processing]`, `[✨ Gathering info]`), use
    `<md-gb-card>` with `slot="container"` or a non-interactive `<div
    role="status" class="relative inline-flex items-center gap-s100 px-s200
    py-s100 rounded-full overflow-hidden bg-surface-container">` containing
    `<md-gb-energy class="absolute inset-0 pointer-events-none" active
    state="processing">`. Never use `<md-gb-button>` for read-only status pills.
    See [Usage Examples](references/energy_usage_examples.md).

## Resources

*   [Usage Examples](references/energy_usage_examples.md): standalone surfaces,
    button/card container slots, color types, intensity, and full-page
    backdrops.
