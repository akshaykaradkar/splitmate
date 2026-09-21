# GenUI Smoke Test

Automated smoke test that validates the genui skill and gBreeze kit produce
correct component and style usage across multiple Gemini CLI generation runs.

## Prerequisites

- **Gemini CLI** installed and authenticated (`gemini --version` should work)
- **Node modules** installed (`npm ci`)
- A valid Gemini API key or Google auth configured for the CLI

## Quick start

```bash
npm run test:smoke
```

This runs 2 prompts x 4 generations each (8 total), validates every run, and
asserts that at least 75% of runs per prompt pass all checks.

## How it works

```
For each prompt:
  1. Copy the kit to a temp directory (/tmp/genui-smoke-XXXX/)
  2. Symlink node_modules (no npm install needed)
  3. Create a GEMINI.md bridge so Gemini CLI discovers AGENTS.md + skills
  4. For each run (1-4):
     a. Write the prompt to .smoke-prompt.md
     b. Run: cat .smoke-prompt.md | gemini -y -m <model>
     c. Collect all modified .tsx files via git diff
     d. Validate the generated code against 9 checks
     e. Run npm run build (typecheck + bundle)
     f. Reset the working tree for the next run
  5. Assert >= 75% of runs pass
  6. Clean up the temp directory
```

## Prompts tested

| Prompt | File origin | Regression it targets |
|--------|------------|----------------------|
| **Travel booking form** — "Design the core booking form for an internal travel tool..." | `prompts/travel_low.md` from James's eval framework | Giant button issue |
| **Gmail + Gemini interface** — "Design a next-generation Gmail interface powered by a deep Gemini integration..." | `prompts/gmail_high.md` from James's eval framework | Font / typography issues |

## Validation checks

Each generated `.tsx` file is checked for:

| # | Check | What it catches | Severity |
|---|-------|----------------|----------|
| 1 | `gbreeze-imports` | No imports from `gbreeze/components/` at all | FAIL |
| 2 | `no-raw-buttons` | Raw `<button>` instead of `<Button>` component | FAIL |
| 3 | `no-raw-inputs` | Raw `<input>` instead of `<TextField>`/`<Checkbox>` | FAIL |
| 4 | `no-giant-buttons` | Oversized button styling (excessive height/padding) | FAIL |
| 5 | `typescale-usage` | Zero `typescale-*` classes in output | FAIL |
| 6 | `no-raw-text-sizes` | `text-sm`, `text-lg`, etc. (suppressed in kit) | FAIL |
| 7 | `no-suppressed-fonts` | `font-sans`, `font-serif`, `font-mono` (suppressed) | FAIL |
| 8 | `no-hardcoded-colors` | `bg-[#...]`, `text-[#...]` arbitrary values | FAIL |
| 9 | `no-typescale-overrides` | `leading-*`/`tracking-*` mixed with `typescale-*` | WARN |

A run **passes** when all FAIL-severity checks pass AND `npm run build` exits 0.

## Configuration

Override defaults with environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `SMOKE_MODEL` | `gemini-3.1-pro-preview` | Gemini model to use |
| `SMOKE_RUNS` | `4` | Number of generations per prompt |
| `SMOKE_THRESHOLD` | `0.75` | Minimum pass rate (0.0 - 1.0) |

### Examples

```bash
# Default: 4 runs, 75% threshold, gemini-3.1-pro-preview
npm run test:smoke

# Use a different model
SMOKE_MODEL=gemini-2.5-pro npm run test:smoke

# Quick 2-run sanity check with lower threshold
SMOKE_RUNS=2 SMOKE_THRESHOLD=0.5 npm run test:smoke

# Single run, always pass (for debugging)
SMOKE_RUNS=1 SMOKE_THRESHOLD=0 npm run test:smoke
```

## Reading the output

Successful run:

```
 ✓ travel-booking-form: >=3/4 generations pass (4m 12s)
     Run 1: PASS (3 gbreeze imports, 5 typescale classes, 0 violations) [build OK, source: files]
     Run 2: PASS (2 gbreeze imports, 4 typescale classes, 0 violations) [build OK, source: files]
     Run 3: FAIL (1 failures: no-raw-buttons) [build OK, source: files]
       - FAIL: no-raw-buttons — Found 1 raw <button> element(s)
     Run 4: PASS (4 gbreeze imports, 6 typescale classes, 0 violations) [build OK, source: files]
     Result: 3/4 passed (need >=3)
```

Key fields in each run line:
- **source: files** — validation ran on files Gemini wrote to disk
- **source: text-output** — fallback: validated code blocks from Gemini's text response
- **build OK / BUILD FAILED** — whether `npm run build` (tsc + vite) passed

## Interaction with normal tests

The smoke tests are **skipped** during normal test runs:

```bash
npm test        # skips smoke tests (SMOKE_TEST not set)
npm run test:run  # skips smoke tests
npm run test:smoke  # runs smoke tests (sets SMOKE_TEST=1)
```

## Adding new prompts

Edit `src/test/smoke/prompts.ts`:

```typescript
export const PROMPTS: SmokePrompt[] = [
  // ... existing prompts ...
  {
    name: 'my-new-prompt',
    text: 'Design a ...',
    expectedComponents: ['Button', 'Card', 'TextField'],
  },
];
```

The test automatically picks up new entries — no other changes needed.

## Adding new checks

Edit `src/test/smoke/validators.ts`:

1. Write a new `check*` function returning `CheckResult`
2. Add it to the `checks` array in `validateGeneratedCode()`

## File structure

```
src/test/smoke/
  prompts.ts           # Prompt definitions and expected components
  validators.ts        # 9 validation checks + code block extraction
  gemini-runner.ts     # Temp kit setup, Gemini CLI invocation, cleanup
  genui-smoke.test.ts  # Vitest test orchestrating runs and assertions
  README.md            # This file
```

## Troubleshooting

### "gemini CLI not found on PATH"

Install Gemini CLI: `npm i -g @google/gemini-cli` or `npx @google/gemini-cli`.

### Rate limit errors (429)

The test adds delays between runs (5s) and between prompts (10s). If you still
hit limits, try a model with higher quotas (`gemini-2.5-flash`) or reduce runs:

```bash
SMOKE_MODEL=gemini-2.5-flash SMOKE_RUNS=2 npm run test:smoke
```

### "no generated code found in files or text output"

The model didn't write any `.tsx` files or output code blocks. The test logs the
first 300 characters of Gemini's output for diagnosis. Common causes:
- API error or rate limit hit early in the generation
- Model spent all turns planning instead of coding (should be rare with the
  prompt wrapper that instructs "start coding immediately")

### Temp directories not cleaned up

If the test crashes, stale temp dirs may remain. Clean them up with:

```bash
rm -rf /tmp/genui-smoke-*
```
