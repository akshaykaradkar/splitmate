import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import {defineConfig} from 'vite';
import {
  cssSheetPlugin,
  prependLayerDirectivePlugin,
  storybookStaticPlugin,
} from './vite-plugins';

// tslint:disable-next-line:no-default-export Required by Vite configuration.
export default defineConfig({
  base: './',
  plugins: [
    react(),
    tailwindcss(),
    cssSheetPlugin(),
    prependLayerDirectivePlugin(),
    storybookStaticPlugin(),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './'),
      '@material': path.resolve(__dirname, './node_modules/@material'),
    },
    preserveSymlinks: true,
  },
  optimizeDeps: {
    exclude: ['@material/web'],
  },
  server: {
    host: '0.0.0.0',
    allowedHosts: true,
    strictPort: false,
  },
});
