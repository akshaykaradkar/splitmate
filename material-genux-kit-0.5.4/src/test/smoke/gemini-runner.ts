/**
 * Gemini CLI orchestration for smoke tests.
 *
 * Follows the same invocation pattern as the eval framework in
 * google3/experimental/users/jameslin/wfe/evals/run_experiments.py:
 *   cat prompt.md | gemini -y -p -
 *
 * Each run operates on a temporary copy of the kit so the real project
 * is never modified.
 */

import { execSync } from 'node:child_process';
import {
  cpSync,
  symlinkSync,
  writeFileSync,
  readFileSync,
  rmSync,
  existsSync,
  mkdtempSync,
  unlinkSync,
} from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';

/** 3 minutes per generation — Gemini can be slow on complex prompts. */
const GEMINI_TIMEOUT = 180_000;
/** 1 minute for tsc + vite build. */
const BUILD_TIMEOUT = 60_000;

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface RunResult {
  /** Map of filepath (relative) -> file contents for all .tsx files Gemini modified or created. */
  modifiedFiles: Record<string, string>;
  /** Whether `npm run build` exited 0. */
  buildPassed: boolean;
  /** Combined build stdout + stderr. */
  buildOutput: string;
  /** Raw text output from the Gemini CLI. */
  geminiOutput: string;
}

// ---------------------------------------------------------------------------
// Git helpers (temp-dir only, never touches the real repo)
// ---------------------------------------------------------------------------

const GIT_ENV = {
  GIT_AUTHOR_NAME: 'smoke-test',
  GIT_AUTHOR_EMAIL: 'smoke@test',
  GIT_COMMITTER_NAME: 'smoke-test',
  GIT_COMMITTER_EMAIL: 'smoke@test',
};

function gitExec(cmd: string, cwd: string): string {
  return execSync(cmd, {
    cwd,
    encoding: 'utf-8',
    stdio: 'pipe',
    env: { ...process.env, ...GIT_ENV },
  });
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

/**
 * Creates a temporary copy of the kit ready for a Gemini generation run.
 *
 * - Copies the project tree (excluding node_modules, dist, .git)
 * - Symlinks node_modules to the original (skip npm install)
 * - Creates a GEMINI.md bridge so Gemini CLI discovers AGENTS.md
 * - Initialises git to track per-run diffs
 *
 * @returns Absolute path to the temp directory.
 */
export function setupTempKit(kitDir: string): string {
  const tempDir = mkdtempSync(join(tmpdir(), 'genui-smoke-'));

  // Copy kit — exclude heavy / generated directories
  cpSync(kitDir, tempDir, {
    recursive: true,
    filter: (src) => {
      const rel = src.slice(kitDir.length); // e.g. '/node_modules/...' or ''
      if (!rel) return true; // root dir itself
      return (
        !/^\/node_modules(\/|$)/.test(rel) &&
        !/^\/\.git(\/|$)/.test(rel) &&
        !/^\/dist(\/|$)/.test(rel)
      );
    },
  });

  // Symlink node_modules from the real kit (much faster than npm install)
  const nmSrc = join(kitDir, 'node_modules');
  const nmDst = join(tempDir, 'node_modules');
  if (existsSync(nmSrc) && !existsSync(nmDst)) {
    symlinkSync(nmSrc, nmDst);
  }

  // Bridge file: Gemini CLI auto-loads GEMINI.md; our project uses AGENTS.md.
  // The instructions must be explicit enough that the model reads the skills
  // BEFORE generating code — a simple "read AGENTS.md" is often ignored.
  writeFileSync(
    join(tempDir, 'GEMINI.md'),
    [
      '# Project Context',
      '',
      'This is a Material Design project using the **gBreeze** component library (in `gbreeze/`).',
      '',
      '## CRITICAL — Read Before Generating ANY Code',
      '',
      '1. Read `.agents/skills/genui/SKILL.md` — it contains the **Component Registry** of',
      '   all available React components you MUST import and use (Button, TextField, Card, etc.).',
      '   Never recreate these from raw HTML.',
      '2. Read `.agents/skills/gbreeze/SKILL.md` — component APIs and CSS tokens.',
      '3. Read `.agents/skills/design/SKILL.md` — layout and spacing guidelines.',
      '4. Read `.agents/skills/layout-scaffold/SKILL.md` — app shell template.',
      '',
      '## Key Rules (from genui skill)',
      '',
      '- Import components from `gbreeze/components/` — NEVER use raw `<button>`, `<input>`, etc.',
      '- Every text element MUST use a `typescale-*` Tailwind class (e.g. `typescale-body-md`).',
      '- NEVER use raw `text-sm`, `text-lg`, `font-sans` — they are suppressed in this project.',
      '- NEVER hardcode colors (`bg-[#...]`) — use token classes (`bg-primary`, `text-on-surface`).',
      '- Do NOT create a backend. Mock any data that would come from a server.',
      '',
      'Also read `AGENTS.md` for full project guidelines.',
    ].join('\n') + '\n',
  );

  // Git baseline so we can diff after each run
  gitExec('git init', tempDir);
  gitExec('git add -A', tempDir);
  gitExec('git commit -m "baseline" --no-verify --allow-empty', tempDir);

  return tempDir;
}

/**
 * Runs a single Gemini generation inside the temp kit directory.
 *
 * 1. Writes the prompt to a temp file
 * 2. Invokes `cat prompt | gemini -y -m <model>`
 * 3. Collects all modified/new .tsx files via git diff
 * 4. Runs `npm run build` to verify the output compiles
 */
export function runGeneration(
  tempDir: string,
  prompt: string,
  model: string,
): RunResult {
  // Wrap the raw design brief with explicit coding instructions.
  // Without this, the model may output a text description of the design
  // instead of actually writing React code — especially for complex prompts.
  const wrappedPrompt = [
    'Build the following application. Start coding immediately by modifying',
    'src/App.tsx and creating any additional component files you need.',
    'Use the gBreeze component library and follow all project guidelines.',
    'Do NOT describe or plan what you will build — just implement it now.',
    '',
    prompt,
  ].join('\n');

  // Write prompt to file — avoids shell-quoting issues with long text
  const promptFile = join(tempDir, '.smoke-prompt.md');
  writeFileSync(promptFile, wrappedPrompt);

  // Invoke Gemini CLI (same pattern as run_experiments.py)
  let geminiOutput = '';
  try {
    geminiOutput = execSync(`cat .smoke-prompt.md | gemini -y -m ${model}`, {
      cwd: tempDir,
      timeout: GEMINI_TIMEOUT,
      encoding: 'utf-8',
      maxBuffer: 10 * 1024 * 1024, // 10 MB — generations can be verbose
      env: { ...process.env, FORCE_COLOR: '0' },
    });
  } catch (err: any) {
    // Gemini may exit non-zero on rate limits, turn limits, or errors.
    // We still want whatever output it produced.
    geminiOutput = [err.stdout ?? '', err.stderr ?? ''].join('\n');
  }

  // Collect all modified / newly created .tsx files
  const modifiedFiles = collectModifiedTsx(tempDir);

  // Build check — does the generated code actually compile?
  let buildPassed = false;
  let buildOutput = '';
  try {
    buildOutput = execSync('npm run build 2>&1', {
      cwd: tempDir,
      encoding: 'utf-8',
      timeout: BUILD_TIMEOUT,
      env: { ...process.env, FORCE_COLOR: '0' },
    });
    buildPassed = true;
  } catch (err: any) {
    buildOutput = [err.stdout ?? '', err.stderr ?? ''].join('\n');
  }

  return { modifiedFiles, buildPassed, buildOutput, geminiOutput };
}

/**
 * Resets the temp kit to baseline so the next run starts clean.
 */
export function resetForNextRun(tempDir: string): void {
  gitExec('git checkout .', tempDir);
  gitExec('git clean -fd -e node_modules -e .smoke-prompt.md', tempDir);
}

/**
 * Removes the temp directory. Safe to call even if setup partially failed.
 * Carefully removes the node_modules symlink first to avoid deleting the
 * real node_modules directory.
 */
export function cleanupTempKit(tempDir: string): void {
  try {
    const nmLink = join(tempDir, 'node_modules');
    if (existsSync(nmLink)) {
      unlinkSync(nmLink); // remove symlink only, never follows
    }
  } catch { /* best effort */ }
  try {
    rmSync(tempDir, { recursive: true, force: true });
  } catch { /* best effort */ }
}

/**
 * Checks whether the `gemini` CLI binary is available on PATH.
 */
export function isGeminiAvailable(): boolean {
  try {
    execSync('which gemini', { stdio: 'pipe' });
    return true;
  } catch {
    return false;
  }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function collectModifiedTsx(tempDir: string): Record<string, string> {
  let diffOutput = '';
  try {
    diffOutput = gitExec('git diff --name-only', tempDir);
  } catch { /* empty */ }

  let untrackedOutput = '';
  try {
    untrackedOutput = gitExec(
      'git ls-files --others --exclude-standard',
      tempDir,
    );
  } catch { /* empty */ }

  const allChanged = [
    ...diffOutput.trim().split('\n'),
    ...untrackedOutput.trim().split('\n'),
  ].filter((f) => f && f.endsWith('.tsx'));

  const files: Record<string, string> = {};
  for (const file of allChanged) {
    const fullPath = join(tempDir, file);
    if (existsSync(fullPath)) {
      files[file] = readFileSync(fullPath, 'utf-8');
    }
  }
  return files;
}
