import { type Plugin } from 'vite';
import { readFileSync, existsSync } from 'node:fs';
import path from 'path';

/** File extensions that may contain `import ... with { type: 'css' }`. */
const CSS_IMPORTER_EXTS = new Set([
  '.ts',
  '.mts',
  '.tsx',
  '.js',
  '.mjs',
  '.jsx',
]);

/**
 * Returns true when the import specifier `source` is imported with
 * `{ type: 'css' }` inside the file at `importerPath`.
 *
 * In dev-server mode Vite may alias-resolve `source` to a full path
 * before our plugin sees it, so the raw specifier no longer appears in
 * the file. When `source` is absolute we fall back to matching on the
 * filename only (e.g. `focus-ring.css`).
 */
function hasCssTypeAssertion(source: string, importerPath: string): boolean {
  try {
    const content = readFileSync(importerPath, 'utf8');
    // Use the raw specifier when available, otherwise the filename.
    // Strip queries for matching.
    const cleanSource = source.split('?')[0].split('#')[0];
    const isSourceAbsolute = path.isAbsolute(cleanSource);
    const needle = isSourceAbsolute ? path.basename(cleanSource) : cleanSource;

    // Static regex to find all CSS imports with type: 'css' or 'assert'
    // Captures the file path/specifier in group 1. Safe from ReDoS.
    const re =
      /['"]([^'"]+\.css)['"]\s*(?:with|assert)\s*\{\s*type:\s*['"]css['"]\s*\}/g;

    let match;
    while ((match = re.exec(content)) !== null) {
      const importSpecifier = match[1];
      const cleanSpecifier = importSpecifier.split('?')[0].split('#')[0];

      if (isSourceAbsolute) {
        if (path.basename(cleanSpecifier) === needle) {
          return true;
        }
      } else {
        if (cleanSpecifier === needle) {
          return true;
        }
      }
    }
    return false;
  } catch {
    return false;
  }
}

/**
 * Handles `import ... from '*.css' with { type: 'css' }` across both
 * local source files and pre-built JS in node_modules.
 *
 * Intercepts `.css` imports from supported file types and resolves them
 * to virtual modules that create a native `CSSStyleSheet` via
 * `new CSSStyleSheet()` + `replaceSync()`. This works in the browser
 * build (Rollup) and the jsdom test environment (Vitest).
 */
export function cssSheetPlugin(): Plugin {
  const PREFIX = '\0css-sheet:';
  const virtualModules = new Map<string, string>();

  return {
    name: 'css-sheet',
    enforce: 'pre',
    resolveId(source, importer, options) {
      // Strip queries for checking extensions and paths
      const [cleanSource] = source.split('?');
      const [cleanImporter] = importer ? importer.split('?') : [''];

      if (
        !cleanSource.endsWith('.css') ||
        !importer ||
        !CSS_IMPORTER_EXTS.has(path.extname(cleanImporter))
      ) {
        return;
      }

      // Only intercept CSS imports with `{ type: 'css' }` assertion.
      // In Rollup build mode, import attributes are available on `options`.
      // In Vitest dev-server mode they may not be, so fall back to reading
      // the importer source and checking for the assertion with a regex.
      const hasAttr = (options as any)?.attributes?.type === 'css';
      if (!hasAttr && !hasCssTypeAssertion(cleanSource, cleanImporter)) {
        return;
      }

      // Resolve the CSS file path.
      // In dev-server mode, aliases may pre-resolve the source to an
      // absolute filesystem path, so handle that case first.
      let cssPath: string;
      if (path.isAbsolute(cleanSource)) {
        cssPath = cleanSource;
      } else if (cleanSource.startsWith('.')) {
        const baseDir = path.dirname(cleanImporter);
        const resolvedPath = path.resolve(baseDir, cleanSource);
        // Path traversal check: ensure resolved path is within project root
        if (!resolvedPath.startsWith(process.cwd())) {
          return;
        }
        cssPath = resolvedPath;
      } else {
        // Bare / package specifier (e.g. @material/web/labs/gb/.../focus-ring.css)
        const nodeModulesDir = path.resolve(__dirname, 'node_modules');
        const resolvedPath = path.resolve(nodeModulesDir, cleanSource);
        // Path traversal check: ensure resolved path is within node_modules
        if (!resolvedPath.startsWith(nodeModulesDir)) {
          return;
        }
        cssPath = resolvedPath;
      }

      if (!existsSync(cssPath)) return;

      // Use .js suffix so other plugins (tailwindcss) don't treat this as CSS
      const virtualId = PREFIX + cssPath + '.js';
      virtualModules.set(virtualId, cssPath);
      return virtualId;
    },
    load(id) {
      const cssPath = virtualModules.get(id);
      if (!cssPath) return;
      const css = readFileSync(cssPath, 'utf8')
        .replace(/\\/g, '\\\\')
        .replace(/`/g, '\\`')
        .replace(/\$/g, '\\$');
      return [
        `const sheet = new CSSStyleSheet();`,
        `try { sheet.replaceSync(\`${css}\`); } catch(e) {}`,
        `export default sheet;`,
      ].join('\n');
    },
  };
}

export function prependLayerDirectivePlugin(): Plugin {
  return {
    name: 'prepend-layer-directive',
    enforce: 'post',
    generateBundle(options, bundle) {
      for (const fileName in bundle) {
        const chunk = bundle[fileName];
        if (chunk.type === 'asset' && fileName.endsWith('.css')) {
          if (
            typeof chunk.source === 'string' &&
            chunk.source.includes('@layer')
          ) {
            chunk.source =
              `@layer theme, base, md, components, utilities;\n` + chunk.source;
          }
        }
      }
    },
  };
}