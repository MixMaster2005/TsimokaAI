import { QueryClient } from '@tanstack/react-query';

/**
 * Instance UNIQUE, créée ici et injectée dans le contexte du router (voir main.tsx)
 * ET dans QueryClientProvider — c'est ce qui permet aux routes de faire
 * `context.queryClient.ensureQueryData(...)` dans leurs loaders (voir
 * routes/_app/espaces/$spaceId/fiches/index.tsx pour un exemple).
 *
 * staleTime à 30s par défaut : la donnée reste "fraîche" 30s après fetch,
 * donc naviguer Étagère -> Espace -> retour Étagère ne redéclenche pas
 * un fetch réseau à chaque fois. À ajuster par requête si besoin
 * (ex: les progressions du dashboard peuvent rester "fraîches" plus longtemps
 * que la liste de messages d'une conversation active).
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30 * 1000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});
