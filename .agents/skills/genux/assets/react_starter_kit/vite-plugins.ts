import {cpSync, existsSync, readFileSync} from 'node:fs';
import path from 'node:path';
import sirv from 'sirv';
import {type Plugin, type PreviewServer, type ViteDevServer} from 'vite';

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
 * build (Rollup) and dev-server modes.
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
      // In dev-server mode they may not be, so fall back to reading
      // the importer source and checking for the assertion with a regex.
      const hasAttr =
        (options as {attributes?: {type?: string}} | undefined)?.attributes
          ?.type === 'css';
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

/** Vite plugin that prepends the CSS @layer order directive to bundled CSS assets. */
export function prependLayerDirectivePlugin(): Plugin {
  return {
    name: 'prepend-layer-directive',
    enforce: 'post',
    generateBundle(options, bundle) {
      for (const [fileName, chunk] of Object.entries(bundle)) {
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

/**
 * Serves pre-built Storybook static files under `/storybook/` during dev
 * and preview, and copies them to `dist/storybook` during build.
 */
export function storybookStaticPlugin(): Plugin {
  let rootDir: string;
  let outDir: string;

  const setupMiddleware = (
    server: ViteDevServer | PreviewServer,
    getDir: () => string | null,
  ) => {
    let serveStorybook: ReturnType<typeof sirv> | null = null;
    const initialDir = getDir();
    if (initialDir && existsSync(initialDir)) {
      serveStorybook = sirv(initialDir, {dev: true, single: false});
    }

    server.middlewares.use((req, res, next) => {
      const url = req.url || '';
      const [pathname, search] = url.split('?');
      if (pathname === '/storybook') {
        const target = '/storybook/' + (search ? `?${search}` : '');
        res.statusCode = 301;
        res.setHeader('Location', target);
        res.end();
        return;
      }
      next();
    });

    server.middlewares.use('/storybook', (req, res, next) => {
      if (!serveStorybook) {
        const dir = getDir();
        if (dir && existsSync(dir)) {
          serveStorybook = sirv(dir, {dev: true, single: false});
        }
      }
      if (serveStorybook) {
        return serveStorybook(req, res, next);
      }
      res.statusCode = 404;
      res.setHeader('Content-Type', 'text/html; charset=utf-8');
      res.end(
        '<!DOCTYPE html><html><head><title>Storybook Not Built</title></head>' +
          '<body style="font-family:system-ui,-apple-system,sans-serif;padding:2rem;text-align:center;">' +
          '<h2>Storybook Catalog Not Built</h2>' +
          '<p>The static Storybook catalog was not found.</p>' +
          '<p>Run <code>npm run build:storybook</code> in your terminal to build it.</p>' +
          '</body></html>',
      );
    });
  };

  return {
    name: 'storybook-static',
    configResolved(config) {
      rootDir = config.root;
      outDir = config.build?.outDir
        ? path.resolve(config.root, config.build.outDir)
        : path.resolve(config.root, 'dist');
    },
    configureServer(server) {
      const storybookDir = path.resolve(
        rootDir || process.cwd(),
        'storybook-static',
      );
      setupMiddleware(server, () => storybookDir);
    },
    configurePreviewServer(server) {
      const storybookDir = path.resolve(
        rootDir || process.cwd(),
        'storybook-static',
      );
      const distStorybookDir = path.resolve(
        outDir || path.resolve(process.cwd(), 'dist'),
        'storybook',
      );
      setupMiddleware(server, () => {
        if (existsSync(distStorybookDir)) return distStorybookDir;
        if (existsSync(storybookDir)) return storybookDir;
        return distStorybookDir;
      });
    },
    closeBundle() {
      const dist = outDir
        ? path.resolve(rootDir || process.cwd(), outDir)
        : path.resolve(rootDir || process.cwd(), 'dist');
      const storybookStatic = path.resolve(
        rootDir || process.cwd(),
        'storybook-static',
      );
      if (
        dist !== storybookStatic &&
        existsSync(dist) &&
        existsSync(storybookStatic)
      ) {
        const target = path.resolve(dist, 'storybook');
        cpSync(storybookStatic, target, {recursive: true});
      }
    },
  };
}
