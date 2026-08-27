import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import type { User } from '../types';

/**
 * Session de l'utilisateur courant. Source de vérité pour le guard d'auth
 * (routes/_app/route.tsx, routes/enseignant/route.tsx) et pour l'affichage
 * profil (AppSidebar).
 *
 * Pas de `enabled` conditionnel : si aucun token n'est disponible, la requête
 * échoue en 401, l'interceptor tente un refresh silencieux, puis rejoue.
 * Si le refresh échoue aussi, le guard catch la failure et redirige vers
 * /connexion. Le flow est orchestré par TanStack Query + apiClient, pas par
 * un side-effect externe.
 */
export const sessionQueryOptions = queryOptions({
  queryKey: ['auth', 'session'] as const,
  queryFn: () => apiClient.get<User>('/api/v1/users/me'),
  // Si refresh token absent et pas d'accessToken, inutile de tenter :
  // la requête va directement recevoir 401 → clearTokens → guard redirige.
  retry: false,
  staleTime: 5 * 60 * 1000,
});

/**
 * Utilisé par le loader de routes/_app/route.tsx pour le guard d'auth,
 * ET par AppSidebar pour afficher displayName/avatar — même source, un
 * seul cache, jamais deux endroits qui gardent leur propre copie du user.
 */
export function useSession() {
  return useQuery(sessionQueryOptions);
}
