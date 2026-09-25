# gBreeze Energy Examples

## Standalone Surface

Both `active` and a non-idle `state` (e.g. `state="receiving"`) are required for
the energy surface to visually render.

```html
<md-gb-energy
  active
  state="processing"
  style="--color: var(--md-sys-color-surface, #ffffff); width: 320px; height: 240px; border-radius: 24px;">
  <div class="p-6 text-on-surface">AI Processing Surface</div>
</md-gb-energy>
```

--------------------------------------------------------------------------------

## Attention States & State Transitions

Energy reflects the conversational loop between human and AI. Transition states
semantically and rest/turn off energy when the user is interacting with standard
forms or reading output.

```html
<!-- 1. Idle: resting state / preloaded -->
<md-gb-energy active state="idle"></md-gb-energy>

<!-- 2. Anticipating: entry cues, text selection, proactive suggestions -->
<md-gb-energy active state="anticipating"></md-gb-energy>

<!-- 3. Receiving: active voice/mic listening, live camera/Lens, drawing -->
<md-gb-energy active state="receiving" dynamicintensity="0.6"></md-gb-energy>

<!-- 4. Processing: actively reasoning, querying, synthesizing, or generating -->
<md-gb-energy active state="processing"></md-gb-energy>

<!-- 5. Responding: streaming AI speech or generative content -->
<md-gb-energy active state="responding"></md-gb-energy>
```

### Energy State Switcher Example

When the user explicitly asks for a switcher to test or toggle between energy
states, include all valid interactive states (`idle`, `anticipating`,
`receiving`, `processing`, `responding`) and use strictly their actual names:

```html
<div class="flex flex-wrap gap-2 p-4">
  <md-gb-button color="outlined" size="sm" onclick="setEnergyState('idle')">idle</md-gb-button>
  <md-gb-button color="outlined" size="sm" onclick="setEnergyState('anticipating')">anticipating</md-gb-button>
  <md-gb-button color="outlined" size="sm" onclick="setEnergyState('receiving')">receiving</md-gb-button>
  <md-gb-button color="outlined" size="sm" onclick="setEnergyState('processing')">processing</md-gb-button>
  <md-gb-button color="outlined" size="sm" onclick="setEnergyState('responding')">responding</md-gb-button>
</div>

<script>
  function setEnergyState(state) {
    const energyEl = document.querySelector('md-gb-energy');
    energyEl.state = state;
  }
</script>
```

> [!IMPORTANT] **Only when requested:** If the user asks for a specific energy
> state, do **NOT** generate a state picker or switcher to pick other states.
> Asking for "all states" or multiple valid states is completely valid and
> should produce a single energy element with a state switcher or controller.
>
> [!IMPORTANT] **Actual State Names Only:** Always label each energy state using
> strictly its actual name (`idle`, `anticipating`, `receiving`, `processing`,
> `responding`). Never use alternate names, aliases, or parenthetical
> descriptors (such as "Receiving (Listening)" or "Processing (Thinking)").
>
> [!IMPORTANT] **Invalid States in Multi-State Requests:** If multiple states
> are requested and any of them is invalid (e.g., 'idle', 'thinking', and
> 'responding'), do **NOT** generate code. Point out the invalid state(s) and
> ask the user for clarification with the valid states.

### `state="anticipating"` Auto-Settlement Lifecycle (`1800ms` → `idle`)

`state="anticipating"` is a brief entry cue (when an AI surface mounts, text is
selected, or a prompt bar opens). Always auto-transition `anticipating` to
`idle` after `1800ms` so the entry animation settles rather than looping
forever:

```javascript
// 1. Entry cue on mount or selection: start in 'anticipating'
const energyEl = document.querySelector('#ai-energy');
energyEl.active = true;
energyEl.state = 'anticipating';

// 2. Auto-settle to 'idle' after 1800ms so entry cue does not loop forever
const settleTimer = setTimeout(() => {
  if (energyEl.state === 'anticipating') {
    energyEl.state = 'idle';
  }
}, 1800);

// 3. User submits query: transition to 'processing'
function handleSubmit() {
  clearTimeout(settleTimer);
  energyEl.state = 'processing';
}
```

--------------------------------------------------------------------------------

## Container Containment & Concentric Nested Border Radius

`<md-gb-energy>` renders on a WebGL canvas with padding to allow glow
feathering. Always ensure parent containers declare `border-radius` and
`overflow: hidden` so the canvas is clipped cleanly to rounded shapes without
square corners.

### Concentric Corner Curves Formula

When nesting a container inside another rounded element with padding or a thick
border/inset, calculate the inner border-radius to ensure concentric, natural
curves:

$$\text{inner\_radius} = \text{outer\_radius} - \text{inset\_padding}$$

*   **Mobile Device Frame with Inset (e.g. 8px padding/bezel):** Outer frame
    `rounded-[48px]` (48px) − Inset `p-2` (8px) = Inner container
    `rounded-[40px]` (40px).
*   **Flush / Full-Bleed Energy (no inset):** If the energy surface sits
    directly against the outer frame with no inset, use the exact outer radius
    (`rounded-[48px]`).

### Clickable gBreeze Buttons/Cards vs. Non-Interactive AI Status Pills

Distinguish **clickable user action buttons** from **non-interactive AI status
pills or thinking banners**:

*   **Clickable Action Buttons & Cards**: Use `<md-gb-button>` or `<md-gb-card>`
    with `slot="container"`.
*   **Non-Interactive AI Status Pills / Thinking Banners** (`[🚀 Processing]`,
    `[✨ Gathering information]`): Do **NOT** use `<md-gb-button>` for read-only
    status indicators. Use `<md-gb-card>` or a non-interactive `role="status"`
    pill container with `<md-gb-energy class="absolute inset-0
    pointer-events-none">`.

```html
<!-- Import gBreeze button, card, and icon modules -->
<script type="module">
  import '@material/web/labs/gb/components/button/md-gb-button.js';
  import '@material/web/labs/gb/components/card/md-gb-card.js';
  import '@material/web/labs/gb/styles/icon/md-gb-icon.js';
</script>

<!-- 1. Inside md-gb-card slot="container" -->
<md-gb-card color="elevated" class="relative overflow-hidden rounded-3xl">
  <md-gb-energy
    slot="container"
    active
    state="anticipating"
    style="--color: var(--md-sys-color-surface-container-low);">
  </md-gb-energy>
  <div class="p-6">
    <h3 class="typescale-emphasized-title-md text-on-surface">Gemini Suggestions</h3>
    <p class="typescale-body-md text-on-surface-variant">Recommended actions based on your prompt.</p>
  </div>
</md-gb-card>

<!-- 2. Clickable user action button (56px pill): use md-gb-button with size="md" and slot="container" -->
<md-gb-button color="filled" size="md" class="overflow-hidden rounded-full">
  <md-gb-energy slot="container" active state="processing"></md-gb-energy>
  Generate with Gemini
</md-gb-button>

<!-- 3. Non-interactive AI status pill / thinking indicator (NOT a button!) -->
<div
  role="status"
  aria-live="polite"
  class="relative inline-flex items-center gap-s100 px-s200 py-s100 rounded-full overflow-hidden bg-surface-container text-on-surface"
>
  <md-gb-energy
    class="absolute inset-0 pointer-events-none"
    active
    state="processing"
    style="--color: var(--md-sys-color-surface-container);"
  ></md-gb-energy>
  <span class="relative z-10 inline-flex items-center gap-s100 typescale-emphasized-label-lg">
    <md-gb-icon style="--md-icon-size: 18px">rocket_launch</md-gb-icon>
    <span>Processing</span>
  </span>
</div>
```

--------------------------------------------------------------------------------

## Color Theming & Matching Reference Designs

### 1. GM3 Baseline Theme (`type="accents" baseline`)

Use when a GM3 theme is applied and the energy effect is GM3-harmonized. The
`baseline` flag indicates the accents are GM3 and applies GM3-specific
adjustments:

```html
<md-gb-energy
  active
  state="processing"
  type="accents"
  baseline
  style="--color: var(--md-sys-color-surface, #ffffff);">
</md-gb-energy>
```

### 2. Non-Baseline Theme Accents (`type="accents"`)

Use when the energy should harmonize with a different Material theme's tokens
(e.g., an M3 purple theme instead of GM3 baseline):

```html
<md-gb-energy
  active
  state="anticipating"
  type="accents"
  style="--color: var(--md-sys-color-surface); --md-sys-color-primary: #6750a4; --md-sys-color-tertiary: #7d5260;">
</md-gb-energy>
```

### 3. Custom Single-Color Hue (`type="hue"` + `--color`)

Use when the color is not GM3-harmonized and instead harmonizes with a dedicated
single color via hue rotation (e.g., matching a purple/violet AI glow
screenshot):

```html
<!-- Dedicated Purple Hue Rotation -->
<md-gb-energy
  active
  state="receiving"
  type="hue"
  dynamicintensity="0.7"
  style="--color: #7c4dff;">
</md-gb-energy>

<!-- Dedicated Brand Color (e.g. Royal Blue) -->
<md-gb-energy
  active
  state="processing"
  type="hue"
  style="--color: #1a73e8;">
</md-gb-energy>
```

--------------------------------------------------------------------------------

## Dynamic Intensity, Voice Simulation & Microphone Input

In `state="receiving"`, the shader's internal `movementSpeed` is `0.0`. Movement
and volume silhouette are driven entirely by `dynamicintensity`.

> [!IMPORTANT] **Do NOT use `intensity` for voice simulation:** Never manipulate
> or animate the `intensity` parameter to simulate voice input or speech volume.
> Only `dynamicintensity` / `.dynamicIntensity` should be modulated for audio or
> voice animation. Furthermore, `dynamicintensity` only has an effect in the
> `receiving` and `responding` states.
>
> [!IMPORTANT] **Change Dynamic Intensity Under the Covers (No Sliders, Numbers,
> or Meters):** When modulating `dynamicintensity` (via simulated voice or live
> microphone), do **NOT** display sliders (`<input type="range">`), numeric or
> percentage text readouts (e.g., "50%"), or audio volume meter/progress bars in
> the UI. Dynamic intensity is an internal shader animation parameter and must
> change purely under the covers in JavaScript.

### 1. Simulated Audio / Voice Input (JavaScript)

Simulate human speech with a natural phrase cadence cycle that alternates
between active speaking phrases (gentle modulation peaking around 0.4–0.6) and
brief pauses (~0.5–1.5s):

```javascript
const energyEl = document.querySelector('#voice-energy');
let voiceAnimId = null;

function startSimulatedVoice() {
  if (voiceAnimId) cancelAnimationFrame(voiceAnimId);
  energyEl.active = true;
  energyEl.state = 'receiving';

  let startTime = null;
  // Natural speech cadence: ~2.5s speaking phrase + ~1.0s pause = ~3.5s cycle
  const phraseDuration = 2500; // ms
  const pauseDuration = 1000;  // ms
  const totalCycle = phraseDuration + pauseDuration;

  function loop(timestamp) {
    if (!startTime) startTime = timestamp;
    const elapsed = (timestamp - startTime) % totalCycle;

    if (elapsed < phraseDuration) {
      // Natural speech modulation with smooth envelope shaping (peaks around 0.4 - 0.6)
      const progress = elapsed / 1000;
      const wave = Math.sin(progress * 4) * 0.15 + Math.sin(progress * 1.5) * 0.15 + 0.3;
      const envelope = Math.sin((elapsed / phraseDuration) * Math.PI);
      energyEl.dynamicIntensity = Math.max(0, Math.min(0.6, wave * envelope));
    } else {
      // Natural silence/pause between phrases
      energyEl.dynamicIntensity = 0;
    }

    voiceAnimId = requestAnimationFrame(loop);
  }
  voiceAnimId = requestAnimationFrame(loop);
}

function stopVoice() {
  if (voiceAnimId) {
    cancelAnimationFrame(voiceAnimId);
    voiceAnimId = null;
  }
  energyEl.dynamicIntensity = 0;
  energyEl.state = 'idle';
}
```

### 2. Live Microphone Input (Web Audio API)

Connect real microphone input to `dynamicIntensity`:

```javascript
async function connectMicrophone(energyEl) {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    const audioContext = new (window.AudioContext || window.webkitAudioContext)();
    const source = audioContext.createMediaStreamSource(stream);
    const analyser = audioContext.createAnalyser();
    analyser.fftSize = 256;
    analyser.smoothingTimeConstant = 0.4;
    source.connect(analyser);

    const dataArray = new Uint8Array(analyser.frequencyBinCount);
    energyEl.active = true;
    energyEl.state = 'receiving';

    function sampleMic() {
      analyser.getByteFrequencyData(dataArray);
      let sum = 0;
      for (let i = 0; i < dataArray.length; i++) {
        sum += dataArray[i];
      }
      const average = sum / dataArray.length;
      // Normalize 0..255 average to 0..1 dynamic intensity
      const normalized = Math.min(1.0, (average / 80));
      energyEl.dynamicIntensity = normalized;

      requestAnimationFrame(sampleMic);
    }
    sampleMic();
  } catch (err) {
    console.warn('Microphone access not available, falling back to simulated input', err);
    startSimulatedVoice();
  }
}
```

--------------------------------------------------------------------------------

## Edge-to-Edge Layouts & Mobile Device Frames

### Inside a Mobile Device Frame (Full-Bleed Backdrop & Concentric Curves)

When `<md-gb-energy>` is used as an ambient screen backdrop inside a mobile
frame (`rounded-[40px]`), its wrapper **MUST** be full-bleed across the entire
screen container (`class="absolute inset-0 w-full h-full pointer-events-none"`)
rather than a fixed-height bottom box (`h-80`), which causes horizontal shader
clipping seams across the viewport.

Also note the gBreeze button height scale (`xs=32px, sm=40px, md=56px, lg=96px,
xl=136px`): always use `size="md"` (`56px`) for mobile bottom action bar buttons
and icon buttons (never `size="xl"` which is `136px` tall or `size="lg"` which
is `96px` tall). Because `<md-gb-icon-button>` only supports `color="filled" |
"tonal" | "outlined" | "standard"` (`color="elevated"` is NOT valid on
`<md-gb-icon-button>`), use `color="tonal"` with a
`--md-sys-color-secondary-container` override when a crisp white surface icon
button is needed on a tinted backdrop:

```html
<!-- Outer device frame: rounded-[48px] with 8px (p-2) inset bezel -->
<div class="relative w-[412px] h-[917px] rounded-[48px] p-2 bg-inverse-surface shadow-xl border-4 border-outline flex flex-col overflow-hidden">
  <!-- Inner screen viewport: 48px - 8px = rounded-[40px] -->
  <div class="relative w-full h-full rounded-[40px] overflow-hidden bg-surface flex flex-col">
    <!-- Full edge-to-edge energy backdrop inside the screen viewport (NEVER use fixed height like h-80) -->
    <md-gb-energy
      class="absolute inset-0 w-full h-full pointer-events-none"
      active
      state="receiving"
      dynamicintensity="0.5"
      type="accents"
      baseline
      style="--color: var(--md-sys-color-surface, #ffffff);">
    </md-gb-energy>

    <!-- Foreground UI content -->
    <header class="relative z-10 p-4 flex justify-between items-center text-on-surface">
      <span class="typescale-emphasized-label-md">9:41</span>
      <div class="flex items-center gap-1">
        <md-gb-icon style="--md-icon-size: 16px">wifi</md-gb-icon>
        <md-gb-icon style="--md-icon-size: 16px">battery_full</md-gb-icon>
      </div>
    </header>

    <main class="relative z-10 flex-1 p-6 flex flex-col justify-between">
      <div class="typescale-headline-sm text-on-surface">
        What date is my flight to Hawaii?
      </div>
      <div class="flex justify-center gap-6 pb-6">
        <!-- 56px mobile action buttons MUST use size="md" (56px), NOT size="xl" (136px) -->
        <md-gb-icon-button color="tonal" size="md" aria-label="Voice input">
          <md-gb-icon>mic</md-gb-icon>
        </md-gb-icon-button>
        <md-gb-icon-button
          color="tonal"
          size="md"
          style="--md-sys-color-secondary-container: var(--md-sys-color-surface-container-lowest); --md-sys-color-on-secondary-container: var(--md-sys-color-on-surface);"
          aria-label="Close">
          <md-gb-icon>close</md-gb-icon>
        </md-gb-icon-button>
      </div>
    </main>
  </div>
</div>
```

--------------------------------------------------------------------------------

## Anti-Patterns: Gradients, Extra Colors, Multiple Elements & Status Pills as Buttons

> [!CAUTION] **CSS Gradients Strictly Forbidden:** CSS gradients
> (`linear-gradient(...)`, `radial-gradient(...)`), gradient scrims, or backdrop
> overlays are strictly forbidden in or on top of energy containers. They wash
> out the WebGL shader and make `anticipating` and `receiving` states invisible.

> [!CAUTION] **Do NOT Add Extra Colors to Components with Energy:** Do not add
> extra colors (such as custom background colors, colored borders, background
> tints, or color overlays) to a component or container with energy. The base
> container color should be controlled exclusively via the `--color` CSS custom
> property (e.g., `--color: var(--md-sys-color-surface)`), leaving the shader to
> produce fluid color accents. Extra colors clash with shader physics and cause
> harsh visual clipping artifacts.

> [!CAUTION] **Do NOT Use `<md-gb-button>` for Read-Only Status Pills, or Raw
> `<button>` for Action Buttons:** Interactive user buttons with energy MUST use
> `<md-gb-button>` with `slot="container"`. Non-interactive AI status pills (`[🚀
> Processing]`, `[✨ Gathering information]`) MUST NOT use `<md-gb-button>`; use
> `<md-gb-card>` or a non-interactive `role="status"` pill container with
> `<md-gb-energy class="absolute inset-0 pointer-events-none">`.

> [!CAUTION] **At Most One Energy Element Per Page/Screen:** Energy should only
> be added to at most one element in a web page or screen. Never render multiple
> `<md-gb-energy>` elements on the same page or screen simultaneously.

> [!CAUTION] **Do NOT Use `intensity` for Voice Input:** Never animate or
> modulate `intensity` to simulate voice or speech. Only `dynamicintensity`
> should be modulated for voice simulation, and only in `receiving` or
> `responding` states.

> [!CAUTION] **Do NOT Display Sliders, Numbers, or Meters for Dynamic
> Intensity:** Never render a slider (`<input type="range">`), numeric or
> percentage text readout, or audio volume level meter in the UI when changing
> `dynamicintensity`. It must change strictly under the covers in code.
