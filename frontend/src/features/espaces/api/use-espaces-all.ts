import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { espaceKeys } from './keys';
import type { Space } from '../types';

/**
 * Vue de supervision ENSEIGNANT (ADMIN) : tous les espaces de la plateforme.
 * Endpoint GET /api/v1/spaces/all — 403 pour un étudiant, hook donc réservé
 * au layout `enseignant`.
 */
export const espacesAllQueryOptions = queryOptions({
  queryKey: [...espaceKeys.all, 'all'] as const,
  queryFn: () => apiClient.get<Space[]>('/api/v1/spaces/all'),
});

export function useEspacesAll() {
  return useQuery(espacesAllQueryOptions);
}
