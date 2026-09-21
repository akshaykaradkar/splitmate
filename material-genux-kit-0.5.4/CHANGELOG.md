# Changelog

All notable changes to this project will be documented in this file.

## [2026-04-30]
- fix: Removed `shadow-sm` from the landing page panels in favor of M3 tonal elevation (b/507920104).

## [2026-04-29]
- feat: Ported the energy effect library from google3 (`src/energy/`) including the WebGL2 renderer, GLSL shader, energy color utilities, and Material color utilities.
- feat: Added React `energy-hooks` (`useEnergyController`, `useEnergyPointerInteractions`, `useEnergyShapeSync`, `useEnergySurfaceSync`) wrapping the imperative `energizeElement` API.
- feat: Implemented `EnergizedFilledButton` — a reusable wrapper around the gBreeze filled `Button` that slots `BtnEnergy` to apply the energy shader to its host `md-button`. Translates React-idiomatic `onClick` to the lit-react `onclick` event prop and forwards native button attributes.
- feat: Added `EnergyControlPanel` for tuning energy options (state, role, colors, intensity) in Storybook.
- docs: Added Storybook stories `Utils/Energy gBreeze Button` (filled button playground) and `Utils/Energy Util` (energy container orbit demo).

## [2026-04-16]
- fix: Updated `cssSheetPlugin` in `vite-plugins.ts` to handle query parameters in dev mode.
- fix: Excluded `@material/web` from Vite's `optimizeDeps` to allow `cssSheetPlugin` to intercept its imports in dev mode.

## [2026-03-25]
- feat: Integrated Material Web Components (MWC) wrappers for Dialog, Switch, Chips, Tabs, TextField, Radio, IconButton, Divider, Checkbox.
- feat: Added expressive progress indicators (circular, linear) based on GM3-Wiz.
- fix: Updated event handlers to use recommended lowercase.
- fix: Form submitter logic for buttons.
- fix: Ripple fixes and progress bug fixes (circular progress closing at 0/100).
- test: Switched to `shadow-dom-testing-library` and added `element-internals-polyfill` to fix tests.
- chore: Linting, file renames, and organizational cleanup.

## [2026-03-04]
- feat: Added Storybook stories for 21 components in the `gbreeze` library.
- style: Updated Material 3 Expressive styling constraints on the Badge and Banner components.

## [2026-03-02]
- build: Integrated Zipline CLI for deploying build assets.
- build: Set Vite base path to './' for proper relative asset loading on deployment.
- chore: Added npm scripts `zipline` and `deploy` to automate build and upload processes.
- chore: Added VSCode launch configuration for debugging (`.vscode/launch.json`).
- chore: Ignored `.zipline/` configuration directory in `.gitignore`.

## [2026-02-24]
- chore: Configured ESLint with standard plugins for React development.
- build: Set up Vitest and React Testing Library for component testing.
- chore: Added new NPM scripts for testing (`test`, `test:run`, `test:watch`) and linting (`lint`, `lint:fix`).
- chore: Added a new GitHub actions directory for upcoming CI/CD pipelines.
- fix: Addressed multiple linting warnings across the `gBreeze` UI components by removing unused variables in `Carousel`, `DatePicker`, `Progress`, and `SegmentedButton`.
