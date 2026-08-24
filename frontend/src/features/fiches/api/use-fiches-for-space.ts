import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { ficheKeys } from './keys';
import type { Fiche } from '../types';

/**
 * Vue ENSEIGNANT (ADMIN) : toutes les fiches d'un espace, quel qu'en soit
 * l'auteur — GET /api/v1/fiches/espace/{spaceId} (403 pour un étudiant).
 * C'est la file de travail de validation.
 */
export const fichesForSpaceQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: [...ficheKeys.bySpace(spaceId), 'all'] as const,
    queryFn: () => apiClient.get<Fiche[]>(`/api/v1/fiches/espace/${spaceId}`),
  });

export function useFichesForSpace(spaceId: string) {
  return useQuery(fichesForSpaceQueryOptions(spaceId));
}
