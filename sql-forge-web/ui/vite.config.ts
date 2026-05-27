import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
import * as path from 'node:path';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  base: './',
  build: {
    outDir: '../target/classes/META-INF/resources/sql/forge/web',
    rollupOptions: {
      input: {
        main: path.resolve(__dirname, 'index.html'),
        home: path.resolve(__dirname, 'home.html'),
        login: path.resolve(__dirname, 'login.html'),
      },
    },
  },
  server: {
    proxy: {
      '/sql/forge': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: [
      {
        find: 'i18n-runtime',
        replacement: path.resolve(__dirname, 'node_modules/i18n-runtime/lib/index.js')
      }
    ]
  },
  css: {
    lightningcss: {
      errorRecovery: true,
    },
  }
});
