# AGENTS.md — material-genux-kit (jetbreeze)

## Project Overview

React 19 + TypeScript 5.9 SPA built with Vite 7. Uses an in-repo Material Design 3
component library called **gbreeze** (`gbreeze/`). Styled with Tailwind CSS v4 mapped
to Material Design tokens. Storybook 10 for component documentation.

## Build / Lint / Test Commands

| Task | Command |
|---|---|
| Install dependencies | `npm ci` |
| Dev server (port 3000) | `npm run dev` |
| Build (typecheck + bundle) | `npm run build` |
| Lint | `npm run lint` |
| Lint with auto-fix | `npm run lint:fix` |
| Run all tests (watch) | `npm test` |
| Run all tests (once) | `npm run test:run` |
| Run a single test file | `npx vitest run src/App.test.tsx` |
| Run tests matching name | `npx vitest run -t "test name"` |
| Storybook dev | `npm run storybook` |
| Storybook build | `npm run storybook:build` |

**CI pipeline** (`.github/workflows/ci.yml`): `lint` -> `test:run` -> `build` ->
`build:cdn`. Any of these failing will fail the PR check.

### Presubmit checks (run before pushing)

After making changes that you intend to commit and push, run **all four** CI
steps locally in this exact order — they are the same checks that run on the
PR. Do not push until all four pass:

```bash
npm run lint        # eslint . — must exit 0
npm run test:run    # vitest run — all tests must pass
npm run build       # tsc && vite build — TypeScript errors fail the build
npm run build:cdn   # vite build -c vite-cdn.config.ts
```

If any step fails:
1. Fix the underlying issue (don't suppress with eslint-disable / `// @ts-ignore`
   unless there's a documented reason).
2. Re-run **from `npm run lint`**, not just the failed step — earlier steps may
   regress when you fix later ones.
3. Only push once all four exit cleanly.

When pushing fixes after a PR has already been opened, run these checks again
before each subsequent push. Don't rely on the remote CI to find what you can
catch in seconds locally.

## Test Setup

- **Framework**: Vitest 4 with jsdom environment
- **Utilities**: @testing-library/react, @testing-library/user-event, @testing-library/jest-dom
- **Globals**: enabled — `describe`, `it`, `expect` are available without import
- **Setup file**: `src/test/setup.ts`
- **Test location**: Co-located with source (`src/**/*.{test,spec}.{ts,tsx}`)
- **CSS in tests**: enabled (`css: true` in vitest config)
- For gbreeze component interaction patterns in tests, load the `gbreeze-testing` skill.

## Path Aliases

| Alias | Maps to |
|---|---|
| `@/*` | `src/*` |
| `@gbreeze/*` | `gbreeze/*` |

Defined in both `tsconfig.json` and `vite.config.ts`.

## Code Style

### File and Directory Naming

- **Files**: `kebab-case` — `icon-button.tsx`, `play-store-rating.tsx`
- **Directories**: `kebab-case` — `play-store-rating/`, `navigation-bar/`
- **Component directories** contain: `index.tsx`, `types.ts`, optional `helpers/`
- **gbreeze components** contain: `component-name.tsx`, `component-name.css`, `component-name.stories.tsx`

### Naming Conventions

- **Components**: PascalCase function names — `export function PlayStoreRating()`
- **Props interfaces**: PascalCase with `Props` suffix — `interface ButtonProps`
- **CSS classes**: BEM-like with component prefix — `btn`, `btn-filled`, `btn-sm`
- **CSS custom properties**: `--kebab-case` — `--container-color`, `--icon-size`

### Imports

Order imports as follows (observed convention):
1. Third-party / framework (`react`, libraries)
2. CSS imports (relative `.css` files)
3. gbreeze library imports (`@gbreeze/...`)
4. Local imports (relative `./` paths)
5. Type-only imports use `import type { ... }` syntax

### TypeScript

- **Strict mode** is enabled
- Prefer `interface` for object shapes and component props
- Use `type` for unions and aliases
- Use `ComponentPropsWithRef<'element'>` as base for props extending native elements
- `@typescript-eslint/no-explicit-any` is **off** — `any` is allowed but discouraged
- Unused variables are a **warning**, not an error

### Components

- **Named exports** throughout (no default exports except `App.tsx`)
- Destructure props in the function signature with defaults
- Use `useImperativeHandle` for forwarding refs
- Compose CSS classes with `.filter(Boolean).join(' ')`
- No external state management — React state only
- Optional chaining for callbacks: `onChange?.(value)`
- Nullish coalescing for defaults: `className ?? ''`

### Exports

- gbreeze components: named exports only
- App root: `export default function App()`
- Storybook: `export default meta` + named story exports

### Copyright Headers (gbreeze files only)

```ts
/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
```

## CSS Architecture

- **Tailwind CSS v4** with Material Design token mapping via `gbreeze/styles/tailwind.css`
- `@layer` cascade: `theme` > `base` > `md` > `components` > `utilities`
- gbreeze component CSS uses `@scope` for encapsulation
- Custom `@utility typescale-*` for Material typography shorthand
- Fonts: Google Sans Flex (brand), Noto Sans (plain), Google Symbols (icons)

## ESLint Configuration

Flat config (`eslint.config.js`):
- Extends: `@eslint/js` recommended + `typescript-eslint` recommended
- Plugins: `react-hooks`, `react-refresh`
- No Prettier or Biome — no dedicated formatter is configured
- Ignored: `dist/`, `node_modules/`, `.zipline/`, `storybook-static/`

## Agent Skills

The `.agents/skills/` directory contains specialized instructions for common tasks.
Load these via the `skill` tool when working on matching tasks:

| Skill | When to load |
|---|---|
| `genui` | Creating or modifying UI with gBreeze components |
| `gbreeze` | Referencing gBreeze component APIs, tokens, or CSS |
| `design` | Layout decisions, responsive breakpoints, elevation |
| `gbreeze-testing` | Writing tests or browser automation for gBreeze components |
| `boilerplate-dev` | Dev server usage, port conventions |
| `semantic-architecture` | Organizing components into modules with types-first approach |
| `react-routing` | Implementing navigation with react-router-dom |

## Workflow: Commits

See `.agents/workflows/commit-and-update-changelog.md` — review changes, update
CHANGELOG.md by date, stage files, and create Conventional Commits.

## Git & PRs

- Use `ggh` (not `gh`) for creating pull requests in corp depot repos (e.g. `depot.code.corp.goog`).

## Key Constraints

- Node.js 22 required
- Dev server runs on port 3000 (strict mode — will error if port is taken)
- `gbreeze/` is NOT a published npm package — it lives in-repo
- `gbreeze/` is not in tsconfig `include`; it is type-checked only via imports from `src/`
- Build = `tsc && vite build` — TypeScript errors will fail the build
- Vite base path is `'./'` (relative) for deployment compatibility
