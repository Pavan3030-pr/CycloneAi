import { fileURLToPath, URL } from 'node:url';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // The backend is called directly rather than proxied, so the CORS policy on the API is
    // exercised exactly as it is in a deployed environment.
    strictPort: true,
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
});
