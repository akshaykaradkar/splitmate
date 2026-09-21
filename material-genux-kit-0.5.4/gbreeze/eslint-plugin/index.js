/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * eslint-plugin-gbreeze
 *
 * In-repo ESLint plugin enforcing gbreeze conventions. Not published to
 * npm — wired into the app's `eslint.config.js` via a relative import.
 *
 * To add a rule:
 *   1. Create `rules/<rule-id>.js` exporting an ESLint Rule.RuleModule.
 *   2. Import it below and add an entry to `rules`.
 *   3. Add a test in `tests/<rule-id>.test.js` using ESLint's RuleTester.
 *   4. Enable it in `eslint.config.js` under the `gbreeze/` namespace.
 *   5. Document it in `.agents/skills/gbreeze/SKILL.md` (linting section).
 */

import noMaterialSymbolsClass from './rules/no-material-symbols-class.js';

/** @type {import('eslint').ESLint.Plugin} */
const plugin = {
  meta: {
    name: 'eslint-plugin-gbreeze',
    version: '0.1.0',
  },
  rules: {
    'no-material-symbols-class': noMaterialSymbolsClass,
  },
};

export default plugin;
