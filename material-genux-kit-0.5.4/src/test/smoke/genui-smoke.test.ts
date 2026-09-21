/**
 * GenUI Smoke Test
 *
 * Validates that the genui skill + gBreeze kit produce correct component
 * and style usage across multiple non-deterministic generation runs.
 *
 * For each prompt:
 *   1. Copies the kit to a temp directory
 *   2. Runs Gemini CLI N times (default 4)
 *   3. Validates each run's generated .tsx files for:
 *      - gBreeze component imports (not raw HTML)
 *      - typescale-* class usage (not raw Tailwind text sizes)
 *      - No hardcoded colors, suppressed fonts, or giant buttons
 *   4. Runs `npm run build` to verify the output compiles
 *   5. Asserts that >= threshold (default 75%) of runs pass
 *
 * Usage:
 *   npm run test:smoke                                        # defaults (4 runs, 75% threshold, gemini-3.1-pro-preview)
 *   SMOKE_MODEL=gemini-2.5-pro npm run test:smoke             # use a different model
 *   SMOKE_RUNS=2 SMOKE_THRESHOLD=0.5 npm run test:smoke  # quick check
 */

import { resolve } from 'node:path';
import { PROMPTS } from './prompts';
import { validateGeneratedCode, extractCodeBlocks } from './validators';
import {
  setupTempKit,
  runGeneration,
  resetForNextRun,
  cleanupTempKit,
  isGeminiAvailable,
} from './gemini-runner';

// ---------------------------------------------------------------------------
// Configuration (overridable via env vars)
// ---------------------------------------------------------------------------

const MODEL = process.env.SMOKE_MODEL ?? 'gemini-3.1-pro-preview';
const RUNS = parseInt(process.env.SMOKE_RUNS ?? '4', 10);
const THRESHOLD = parseFloat(process.env.SMOKE_THRESHOLD ?? '0.75');
const KIT_DIR = resolve(__dirname, '../../..');

/** Delay between runs in ms — helps avoid API rate limits. */
const INTER_RUN_DELAY = 5_000;
/** Delay between prompts in ms — larger gap between different prompt tests. */
const INTER_PROMPT_DELAY = 10_000;

function sleep(ms: number): Promise<void> {
  return new Promise((r) => setTimeout(r, ms));
}

// ---------------------------------------------------------------------------
// Test suite — skipped unless SMOKE_TEST=1 is set (e.g. via npm run test:smoke)
// ---------------------------------------------------------------------------

const smokeEnabled = process.env.SMOKE_TEST === '1';
const geminiReady = isGeminiAvailable();

describe.skipIf(!smokeEnabled || !geminiReady)('GenUI Smoke Test', () => {
  if (!smokeEnabled) {
    it.skip('set SMOKE_TEST=1 to enable (run via: npm run test:smoke)', () => {});
    return;
  }
  if (!geminiReady) {
    it.skip('gemini CLI not found on PATH', () => {});
    return;
  }

  const minRequired = Math.ceil(RUNS * THRESHOLD);

  for (const [promptIdx, prompt] of PROMPTS.entries()) {
    it(
      `${prompt.name}: >=${minRequired}/${RUNS} generations pass`,
      async () => {
        // Delay between prompt tests to let rate limits recover.
        if (promptIdx > 0) {
          console.log(`\n  Waiting ${INTER_PROMPT_DELAY / 1000}s before next prompt...`);
          await sleep(INTER_PROMPT_DELAY);
        }

        const results: Array<{
          run: number;
          passed: boolean;
          buildPassed: boolean;
          summary: string;
        }> = [];

        const tempDir = setupTempKit(KIT_DIR);

        try {
          for (let run = 1; run <= RUNS; run++) {
            // Delay between runs within the same prompt.
            if (run > 1) {
              await sleep(INTER_RUN_DELAY);
            }

            console.log(`\n  [${prompt.name}] Run ${run}/${RUNS} — generating...`);

            const result = runGeneration(tempDir, prompt.text, MODEL);

            // Prefer inspecting files Gemini wrote; fall back to parsing
            // code blocks from the text output if no .tsx files were modified.
            let allCode = Object.values(result.modifiedFiles).join('\n');
            let source = 'files';

            if (!allCode.trim()) {
              const blocks = extractCodeBlocks(result.geminiOutput);
              allCode = blocks.join('\n');
              source = 'text-output';
            }

            if (!allCode.trim()) {
              // Log diagnostic info so users can see WHY no code was generated
              // (e.g. rate limit errors, model unavailable, etc.)
              const snippet = result.geminiOutput.trim().slice(0, 300);
              results.push({
                run,
                passed: false,
                buildPassed: false,
                summary: 'FAIL (no generated code found in files or text output)',
              });
              console.log(`  Run ${run}: FAIL — no code generated (source: ${source})`);
              if (snippet) {
                console.log(`    Gemini output (first 300 chars): ${snippet}`);
              }
              if (run < RUNS) resetForNextRun(tempDir);
              continue;
            }

            const validation = validateGeneratedCode(allCode);
            const passed = validation.passed && result.buildPassed;
            const buildTag = result.buildPassed ? 'build OK' : 'BUILD FAILED';
            const summary = `${validation.summary} [${buildTag}, source: ${source}]`;

            results.push({ run, passed, buildPassed: result.buildPassed, summary });
            console.log(`  Run ${run}: ${summary}`);

            // Log individual check failures for debugging
            for (const check of validation.checks) {
              if (!check.passed) {
                console.log(
                  `    - ${check.severity.toUpperCase()}: ${check.name} — ${check.details}`,
                );
              }
            }

            if (run < RUNS) resetForNextRun(tempDir);
          }
        } finally {
          cleanupTempKit(tempDir);
        }

        const passCount = results.filter((r) => r.passed).length;
        console.log(
          `\n  [${prompt.name}] Result: ${passCount}/${RUNS} passed (need >=${minRequired})`,
        );

        expect(passCount).toBeGreaterThanOrEqual(minRequired);
      },
      // 10 minute timeout per prompt (4 Gemini calls + 4 builds + delays)
      600_000,
    );
  }
});
