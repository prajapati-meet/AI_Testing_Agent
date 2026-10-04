import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  envPrefix: ['VITE_', 'BUG_'],
  server: {
    port: 3000,
    strictPort: true
  }
});
