import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { RouterProvider, createRouter } from '@tanstack/react-router';
import { QueryClientProvider } from '@tanstack/react-query';

import { routeTree } from './routeTree.gen'; // généré par le plugin Vite depuis src/routes/ — ne pas éditer
import { queryClient } from './lib/query-client';
import './styles/globals.css';

/**
 * Le queryClient est injecté dans le CONTEXTE du router : chaque route peut
 * donc faire `context.queryClient.ensureQueryData(...)` dans son `loader`
 * sans réimporter queryClient partout. C'est ce qui permet à TanStack Router
 * de précharger les données AVANT que le composant de la route ne s'affiche
 * (fini les spinners en cascade route -> puis fetch -> puis re-render).
 */
const router = createRouter({
  routeTree,
  context: { queryClient },
  defaultPreload: 'intent', // précharge les données au survol d'un <Link>, pas juste au clic
  scrollRestoration: true,
  defaultNotFoundComponent: () => <p className="p-8 text-sm text-muted-foreground">Page introuvable.</p>,
});

// Permet à TypeScript de connaître TOUTES les routes de l'app (autocomplete
// sur les <Link to="..."> et les params) — obligatoire avec TanStack Router.
declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}

const rootElement = document.getElementById('root');
if (!rootElement) throw new Error('#root introuvable dans index.html');

createRoot(rootElement).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
);
