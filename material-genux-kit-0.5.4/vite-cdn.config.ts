import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'
import { globSync } from 'node:fs';
import { cssSheetPlugin } from './vite-plugins'

const entryPoints = globSync('gbreeze/components/**/!(*.stories|*.test).{ts,tsx}', {
  absolute: true,
});

export default defineConfig({
  base: './',
  plugins: [react(), tailwindcss(), cssSheetPlugin()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@gbreeze': path.resolve(__dirname, './gbreeze'),
    },
  },
  build: {
    outDir: 'dist-cdn',
    rollupOptions: {
      input: entryPoints,
      // Externalize all runtime dependencies for importmap compatibility
      external: [
        'react',
        'react-dom',
        'react-dom/client',
        'lit',
        'lit/decorators.js',
        'lit/directives/class-map.js',
        'lit/directives/style-map.js',
        'lit/directives/ref.js',
        'lit/directives/repeat.js',
        'lit-html',
        '@lit/react',
        /^@material\/web/
      ],
      preserveEntrySignatures: 'strict',
      output: {
        format: 'es',
        preserveModules: true,
        preserveModulesRoot: 'gbreeze/components/',
        // Do NOT cache-bust (add hash to names), we'll setup cache headers
        // and semver outside of this config
        entryFileNames: ({ name }) => `${name}.js`,
        chunkFileNames: '_chunks/[name].js',
        assetFileNames: 'assets/[name].[ext]',
        exports: 'named',
        globals: {
            react: 'React',
            'react-dom': 'ReactDOM',
            'react-dom/client': 'ReactDOMClient',
        },
      },
    },
  },
});
