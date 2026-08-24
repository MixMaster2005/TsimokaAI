import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient, getAccessToken } from '@/lib/api-client';
import type { User } from '../types';

export const sessionQueryOptions = queryOptions({
  queryKey: ['auth', 'session'] as const,
  queryFn: () => apiClient.get<User>('/api/v1/users/me'),
  enabled: Boolean(getAccessToken()),
  staleTime: 5 * 60 * 1000, // le profil change rarement, pas besoin de le refetch souvent
});

/**
 * Utilisé par le loader de routes/_app/route.tsx pour le guard d'auth,
 * ET par AppSidebar pour afficher displayName/avatar — même source, un
 * seul cache, jamais deux endroits qui gardent leur propre copie du user.
 */
export function useSession() {
  return useQuery(sessionQueryOptions);
}
