/**
 * Validation functions for generated TSX code.
 *
 * Each check targets a specific regression area from the genui skill's
 * Core Rules (component usage, typography, tokens).
 */

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface CheckResult {
  /** Machine-readable check identifier. */
  name: string;
  passed: boolean;
  /** 'fail' = counts against the run, 'warn' = logged but not scored. */
  severity: 'fail' | 'warn';
  details: string;
}

export interface ValidationResult {
  /** True when every fail-severity check passed. */
  passed: boolean;
  checks: CheckResult[];
  /** Human-readable one-liner for console output. */
  summary: string;
  stats: {
    gbreezeImportCount: number;
    typescaleClassCount: number;
    failCount: number;
    warnCount: number;
  };
}

// ---------------------------------------------------------------------------
// Individual checks
// ---------------------------------------------------------------------------

/**
 * 1. At least one import from gbreeze/components/.
 * Catches: model ignoring the kit entirely and writing vanilla HTML.
 */
function checkGbreezeImports(code: string): CheckResult {
  const importRegex =
    /import\s+(?:(?:type\s+)?{[^}]+}|\w+)\s+from\s+['"]@?gbreeze\/components\//g;
  const matches = code.match(importRegex) ?? [];
  return {
    name: 'gbreeze-imports',
    passed: matches.length > 0,
    severity: 'fail',
    details:
      matches.length > 0
        ? `Found ${matches.length} gbreeze component import(s)`
        : 'No gbreeze component imports found',
  };
}

/**
 * 2. No raw <button> elements — should use <Button> from gbreeze.
 * Catches: Core Rule 1 & 2 violations.
 */
function checkNoRawButtons(code: string): CheckResult {
  // Match <button but not inside comments or strings (simplified heuristic).
  // Exclude lines that are clearly JSX comments {/* ... */}.
  const lines = code.split('\n').filter((l) => !l.trim().startsWith('//') && !l.trim().startsWith('*'));
  const rawButtonCount = lines.filter((l) => /<button[\s>]/i.test(l)).length;
  return {
    name: 'no-raw-buttons',
    passed: rawButtonCount === 0,
    severity: 'fail',
    details:
      rawButtonCount === 0
        ? 'No raw <button> elements found'
        : `Found ${rawButtonCount} raw <button> element(s) — should use <Button> from gbreeze`,
  };
}

/**
 * 3. No raw <input> elements — should use <TextField>/<Checkbox>/<Radio>.
 * Catches: Core Rule 6 violations.
 */
function checkNoRawInputs(code: string): CheckResult {
  const lines = code.split('\n').filter((l) => !l.trim().startsWith('//') && !l.trim().startsWith('*'));
  const rawInputCount = lines.filter((l) => /<input[\s/]/i.test(l)).length;
  return {
    name: 'no-raw-inputs',
    passed: rawInputCount === 0,
    severity: 'fail',
    details:
      rawInputCount === 0
        ? 'No raw <input> elements found'
        : `Found ${rawInputCount} raw <input> element(s) — should use <TextField>/<Checkbox>/<Radio> from gbreeze`,
  };
}

/**
 * 4. No oversized button styling (the "giant button" regression).
 * Catches: buttons with excessive height/padding that break M3 sizing.
 */
function checkNoGiantButtons(code: string): CheckResult {
  const patterns = [
    // Explicit huge height on a button-like context
    /className="[^"]*\b(?:h-(?:1[4-9]|[2-9]\d)\b)[^"]*"/g,
    // Excessive vertical padding
    /className="[^"]*\b(?:py-(?:[6-9]|[1-9]\d)\b)[^"]*"/g,
    // Oversized text on something that looks like a button
    /className="[^"]*\b(?:text-(?:2xl|3xl|4xl|5xl))\b[^"]*"/g,
  ];
  const issues: string[] = [];
  for (const p of patterns) {
    const m = code.match(p);
    if (m) issues.push(...m);
  }
  return {
    name: 'no-giant-buttons',
    passed: issues.length === 0,
    severity: 'fail',
    details:
      issues.length === 0
        ? 'No oversized button styling found'
        : `Found ${issues.length} potential giant-button pattern(s)`,
  };
}

/**
 * 5. At least one typescale-* class is used.
 * Catches: Core Rule 11 — every text element MUST use a typescale utility.
 */
function checkTypescaleUsage(code: string): CheckResult {
  const re = /typescale-(?:display|headline|title|body|label)-(?:sm|md|lg)/g;
  const matches = code.match(re) ?? [];
  return {
    name: 'typescale-usage',
    passed: matches.length > 0,
    severity: 'fail',
    details:
      matches.length > 0
        ? `Found ${matches.length} typescale class usage(s)`
        : 'No typescale-* classes found — all text must use typescale utilities',
  };
}

/**
 * 6. No raw Tailwind text-size utilities (they are suppressed in the kit).
 * Catches: Core Rule 11 anti-patterns.
 */
function checkNoRawTextSizes(code: string): CheckResult {
  // Match Tailwind text-size utilities but NOT MD token classes like text-on-surface.
  // The kit suppresses: text-xs, text-sm, text-base, text-lg, text-xl, text-2xl, etc.
  const re = /\btext-(?:xs|sm|base|lg|xl|[2-9]xl)\b/g;
  const matches = code.match(re) ?? [];
  // Filter out false positives from token color classes (text-on-surface, etc.)
  // text-sm/text-lg etc. are size utilities, not colors, so they should be flagged.
  return {
    name: 'no-raw-text-sizes',
    passed: matches.length === 0,
    severity: 'fail',
    details:
      matches.length === 0
        ? 'No raw Tailwind text-* size utilities found'
        : `Found ${matches.length} raw text size(s): ${[...new Set(matches)].join(', ')} — use typescale-* instead`,
  };
}

/**
 * 7. No suppressed font-family utilities.
 * Catches: Core Rule 11 anti-patterns — use font-brand or font-plain.
 */
function checkNoSuppressedFonts(code: string): CheckResult {
  const re = /\bfont-(?:sans|serif|mono)\b/g;
  const matches = code.match(re) ?? [];
  return {
    name: 'no-suppressed-fonts',
    passed: matches.length === 0,
    severity: 'fail',
    details:
      matches.length === 0
        ? 'No suppressed font-family utilities found'
        : `Found ${matches.length} suppressed font(s): ${[...new Set(matches)].join(', ')} — use font-brand or font-plain`,
  };
}

/**
 * 8. No hardcoded color arbitrary values.
 * Catches: Core Rule 3 — use token-based classes.
 */
function checkNoHardcodedColors(code: string): CheckResult {
  const re = /(?:bg|text|border|ring|shadow|outline|fill|stroke)-\[#[0-9a-fA-F]+\]/g;
  const matches = code.match(re) ?? [];
  return {
    name: 'no-hardcoded-colors',
    passed: matches.length === 0,
    severity: 'fail',
    details:
      matches.length === 0
        ? 'No hardcoded color values found'
        : `Found ${matches.length} hardcoded color(s): ${[...new Set(matches)].join(', ')} — use token-based classes`,
  };
}

/**
 * 9. No leading-* or tracking-* alongside typescale-* (warning only).
 * Catches: typescale already bakes in line-height and letter-spacing.
 */
function checkNoTypescaleOverrides(code: string): CheckResult {
  // Look for lines that have both typescale-* AND leading-* or tracking-*
  const lines = code.split('\n');
  const violations = lines.filter(
    (l) => /typescale-/.test(l) && /\b(?:leading|tracking)-/.test(l),
  );
  return {
    name: 'no-typescale-overrides',
    passed: violations.length === 0,
    severity: 'warn',
    details:
      violations.length === 0
        ? 'No leading-*/tracking-* overrides alongside typescale-*'
        : `Found ${violations.length} line(s) mixing typescale-* with leading-*/tracking-*`,
  };
}

// ---------------------------------------------------------------------------
// Main validator
// ---------------------------------------------------------------------------

/**
 * Runs all validation checks on the combined generated TSX code.
 *
 * @param code - Concatenated contents of all modified .tsx files from a
 *   single generation run.
 * @returns Structured result with per-check details and aggregate stats.
 */
export function validateGeneratedCode(code: string): ValidationResult {
  const checks: CheckResult[] = [
    checkGbreezeImports(code),
    checkNoRawButtons(code),
    checkNoRawInputs(code),
    checkNoGiantButtons(code),
    checkTypescaleUsage(code),
    checkNoRawTextSizes(code),
    checkNoSuppressedFonts(code),
    checkNoHardcodedColors(code),
    checkNoTypescaleOverrides(code),
  ];

  const failCount = checks.filter((c) => !c.passed && c.severity === 'fail').length;
  const warnCount = checks.filter((c) => !c.passed && c.severity === 'warn').length;
  const passed = failCount === 0;

  // Aggregate stats for the summary line.
  const gbreezeImportCount =
    (code.match(/import\s+(?:(?:type\s+)?{[^}]+}|\w+)\s+from\s+['"]@?gbreeze\/components\//g) ?? []).length;
  const typescaleClassCount =
    (code.match(/typescale-(?:display|headline|title|body|label)-(?:sm|md|lg)/g) ?? []).length;

  const failedNames = checks
    .filter((c) => !c.passed && c.severity === 'fail')
    .map((c) => c.name);

  const summary = passed
    ? `PASS (${gbreezeImportCount} gbreeze imports, ${typescaleClassCount} typescale classes, 0 violations)`
    : `FAIL (${failCount} failures: ${failedNames.join(', ')})`;

  return { passed, checks, summary, stats: { gbreezeImportCount, typescaleClassCount, failCount, warnCount } };
}

/**
 * Extracts fenced TSX/JSX/TypeScript code blocks from raw text output.
 *
 * Used as a fallback when Gemini outputs code in its text response rather
 * than writing files.
 */
export function extractCodeBlocks(text: string): string[] {
  const blocks: string[] = [];
  const re = /```(?:tsx|jsx|typescript|ts)\n([\s\S]*?)```/g;
  let match: RegExpExecArray | null;
  while ((match = re.exec(text)) !== null) {
    blocks.push(match[1]);
  }
  return blocks;
}
