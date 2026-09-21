/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import { RuleTester } from 'eslint';
import tsParser from '@typescript-eslint/parser';
import rule from '../rules/no-material-symbols-class.js';

// ESLint's RuleTester expects globally-available `describe`/`it`. Vitest
// provides those when `globals: true` is set in the vite config (it is —
// see vite.config.ts), so RuleTester.run() registers each scenario as its
// own vitest test case.
const ruleTester = new RuleTester({
  languageOptions: {
    parser: tsParser,
    ecmaVersion: 2020,
    sourceType: 'module',
    parserOptions: {
      ecmaFeatures: { jsx: true },
    },
  },
});

ruleTester.run('no-material-symbols-class', rule, {
  valid: [
    // Idiomatic gbreeze usage.
    { code: '<span className="md-icon">home</span>;' },
    { code: '<div className="bg-surface text-on-surface">x</div>;' },
    { code: '<Icon>home</Icon>;' },
    // Class names that merely contain "material" but are not legacy.
    { code: '<span className="my-material-class">x</span>;' },
    // Legacy name appearing inside a non-class string literal — out of scope.
    { code: 'const x = "material-symbols-outlined";' },
    // Dynamic className expression — out of scope (handled by future rule).
    { code: '<span className={cls}>x</span>;' },
    { code: '<span className={cond ? "a" : "b"}>x</span>;' },
    // Template literal WITH substitution — out of scope.
    { code: '<span className={`prefix ${name}`}>x</span>;' },
    // Empty / missing values.
    { code: '<span className="">x</span>;' },
    { code: '<span>x</span>;' },
  ],
  invalid: [
    // Each legacy name in a basic className="..." form.
    ...[
      'material-symbols-outlined',
      'material-symbols-rounded',
      'material-symbols-sharp',
      'material-icons-outlined',
      'material-icons-round',
      'material-icons-sharp',
      'material-icons-two-tone',
      'material-icons',
    ].map((name) => ({
      code: `<span className="${name}">home</span>;`,
      errors: [{ messageId: 'legacyClass', data: { name } }],
    })),

    // Single-quoted attribute value.
    {
      code: "<span className='material-symbols-outlined'>home</span>;",
      errors: [
        {
          messageId: 'legacyClass',
          data: { name: 'material-symbols-outlined' },
        },
      ],
    },

    // Raw HTML-style `class=` attribute (used in some JSX dialects).
    {
      code: '<i class="material-icons">settings</i>;',
      errors: [
        { messageId: 'legacyClass', data: { name: 'material-icons' } },
      ],
    },

    // className={"..."}
    {
      code: '<span className={"material-symbols-rounded text-on-surface"}>star</span>;',
      errors: [
        {
          messageId: 'legacyClass',
          data: { name: 'material-symbols-rounded' },
        },
      ],
    },

    // className={'...'}
    {
      code: "<span className={'material-icons-outlined'}>add</span>;",
      errors: [
        {
          messageId: 'legacyClass',
          data: { name: 'material-icons-outlined' },
        },
      ],
    },

    // className={`...`} — no substitutions.
    {
      code: '<span className={`material-symbols-sharp foo bar`}>menu</span>;',
      errors: [
        {
          messageId: 'legacyClass',
          data: { name: 'material-symbols-sharp' },
        },
      ],
    },

    // Mixed: legacy + valid utility classes — should still flag the legacy one.
    {
      code: '<span className="material-symbols-outlined md-icon">home</span>;',
      errors: [
        {
          messageId: 'legacyClass',
          data: { name: 'material-symbols-outlined' },
        },
      ],
    },

    // Two legacy classes in the same attribute → two reports.
    {
      code: '<span className="material-symbols-outlined material-icons">home</span>;',
      errors: [
        {
          messageId: 'legacyClass',
          data: { name: 'material-symbols-outlined' },
        },
        { messageId: 'legacyClass', data: { name: 'material-icons' } },
      ],
    },
  ],
});
