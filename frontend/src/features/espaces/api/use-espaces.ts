import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { espaceKeys } from './keys';
import type { Space } from '../types';

export const espacesQueryOptions = queryOptions({
  queryKey: espaceKeys.mine(),
  queryFn: () => apiClient.get<Space[]>('/api/v1/spaces'),
});

/**
 * Exemple d'usage dans un loader de route (préchargement avant affichage) :
 *
 *   loader: ({ context: { queryClient } }) => queryClient.ensureQueryData(espacesQueryOptions)
 *
 * Et dans le composant, ce hook retombe sur le MÊME cache (même query key) :
 * pas de double fetch, TanStack Query réconcilie automatiquement.
 */
export function useEspaces() {
  return useQuery(espacesQueryOptions);
}
