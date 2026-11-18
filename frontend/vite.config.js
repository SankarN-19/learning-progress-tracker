import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

// The dev server proxies /api to Spring Boot, so the browser only ever talks to
// http://127.0.0.1:5173 (same origin): no CORS, and session/CSRF cookies just work.
const apiTarget = process.env.VITE_API_TARGET || 'http://127.0.0.1:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: apiTarget, changeOrigin: false },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js',
  },
});
