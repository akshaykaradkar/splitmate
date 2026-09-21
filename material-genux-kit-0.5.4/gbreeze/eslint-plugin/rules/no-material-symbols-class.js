/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * `no-material-symbols-class`
 *
 * Flags use of legacy Google Material icon class names (`material-symbols-*`,
 * `material-icons*`) on JSX `className` / `class` attributes. gbreeze ships
 * its own icon system via the `<Icon>` React component (and the `.md-icon`
 * class) wired to the Google Symbols font through `--md-icon-font`. Using
 * the legacy Google Fonts CDN class names bypasses gbreeze's font, size,
 * weight, fill, and grade tokens entirely.
 *
 * Scope: literal string values on JSX attributes only. This includes:
 *   className="..."
 *   className='...'
 *   class="..."
 *   className={"..."}
 *   className={'...'}
 *   className={`...`}              (template literal with no ${} expressions)
 *
 * Out of scope (intentionally — these need separate rules):
 *   - Dynamic className expressions (clsx, ternaries, joined arrays, etc.)
 *   - CSS class selectors in `.css` files (would need Stylelint)
 *   - Raw HTML `class=` attributes in `.html` files (ESLint doesn't parse HTML)
 */

const LEGACY_ICON_CLASS_NAMES = [
  'material-symbols-outlined',
  'material-symbols-rounded',
  'material-symbols-sharp',
  'material-icons-outlined',
  'material-icons-round',
  'material-icons-sharp',
  'material-icons-two-tone',
  'material-icons',
];

const LEGACY_SET = new Set(LEGACY_ICON_CLASS_NAMES);

/**
 * Extract the static string value from a JSX attribute's value node, if it
 * is a literal we can statically inspect. Returns null when the value is
 * dynamic (object expression, ternary, function call, template with
 * substitutions, etc.) — those cases are out of scope.
 *
 * @param {import('estree-jsx').JSXAttribute['value']} valueNode
 * @returns {{text: string, reportNode: import('estree').Node} | null}
 */
function extractLiteralClassString(valueNode) {
  if (!valueNode) return null;

  // className="..."  /  className='...'
  if (valueNode.type === 'Literal' && typeof valueNode.value === 'string') {
    return { text: valueNode.value, reportNode: valueNode };
  }

  // className={ ... }
  if (valueNode.type === 'JSXExpressionContainer') {
    const expr = valueNode.expression;
    if (!expr) return null;

    // className={"..."}  /  className={'...'}
    if (expr.type === 'Literal' && typeof expr.value === 'string') {
      return { text: expr.value, reportNode: expr };
    }

    // className={`...`} — only static templates with zero substitutions
    if (
      expr.type === 'TemplateLiteral' &&
      expr.expressions.length === 0 &&
      expr.quasis.length === 1
    ) {
      return { text: expr.quasis[0].value.cooked ?? '', reportNode: expr };
    }
  }

  return null;
}

/** @type {import('eslint').Rule.RuleModule} */
const rule = {
  meta: {
    type: 'problem',
    docs: {
      description:
        'Disallow legacy Google Material icon class names; use the gbreeze ' +
        '<Icon> component or the .md-icon class instead.',
      recommended: true,
    },
    schema: [],
    messages: {
      legacyClass:
        'Legacy icon class "{{name}}" is not part of gbreeze. ' +
        'Use the <Icon> React component from "@gbreeze/styles/icons/icon" ' +
        '(e.g. <Icon>home</Icon>) or, for raw markup, className="md-icon".',
    },
  },

  create(context) {
    return {
      JSXAttribute(node) {
        // node.name can be a JSXIdentifier ({name: 'className'}) or a
        // JSXNamespacedName (e.g. svg:class). We only care about the
        // identifier form.
        const attrName =
          node.name && node.name.type === 'JSXIdentifier'
            ? node.name.name
            : null;
        if (attrName !== 'className' && attrName !== 'class') return;

        const extracted = extractLiteralClassString(node.value);
        if (extracted == null) return;

        const tokens = extracted.text.split(/\s+/);
        for (const token of tokens) {
          if (token && LEGACY_SET.has(token)) {
            context.report({
              node: extracted.reportNode,
              messageId: 'legacyClass',
              data: { name: token },
            });
          }
        }
      },
    };
  },
};

export default rule;
export { LEGACY_ICON_CLASS_NAMES };
