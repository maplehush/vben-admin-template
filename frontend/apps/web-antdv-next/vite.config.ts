import { defineConfig } from '@vben/vite-config';

export default defineConfig(async () => {
  return {
    application: {},
    vite: {
      server: {
        proxy: {
          '/api': {
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/api/, ''),
            // Preserve /api for the browser-facing refresh cookie path, strip it upstream.
            target: 'http://localhost:8080',
            ws: true,
          },
        },
      },
    },
  };
});
