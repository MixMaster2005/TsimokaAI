import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { tanstackRouter } from '@tanstack/router-plugin/vite';
import path from 'node:path';

// L'ORDRE DES PLUGINS COMPTE : tanstackRouter DOIT être déclaré avant react().
// Il scanne src/routes/ à chaque changement de fichier et régénère
// src/routeTree.gen.ts — c'est ce fichier généré que le router consomme
// au runtime (voir src/main.tsx). On ne l'édite jamais à la main.
export default defineConfig({
  plugins: [
    tanstackRouter({
      target: 'react',
      routesDirectory: 'src/routes',
      generatedRouteTree: 'src/routeTree.gen.ts',
      autoCodeSplitting: true, // chaque route devient son propre chunk JS, chargé à la demande
    }),
    react(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      // même alias "@/" que ce qu'attend shadcn/ui (components.json) —
      // évite les imports relatifs "../../../" douloureux dans les features
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
});
