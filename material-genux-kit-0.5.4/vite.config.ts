import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'
import { cssSheetPlugin, prependLayerDirectivePlugin } from './vite-plugins';

export default defineConfig({
  base: './',
  plugins: [
    react(),
    tailwindcss(),
    cssSheetPlugin(),
    prependLayerDirectivePlugin(),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@gbreeze': path.resolve(__dirname, './gbreeze'),
      '@material': path.resolve(__dirname, './node_modules/@material'),
    },
  },
  optimizeDeps: {
    exclude: ['@material/web'],
  },
  server: {
    strictPort: true,
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: true,
    include: [
      'src/**/*.{test,spec}.{ts,tsx,js,jsx}',
      'gbreeze/**/*.{test,spec}.{ts,tsx,js,jsx}',
    ],
    server: {
      deps: {
        inline: ['@material/web'],
      },
    },
  },
});
