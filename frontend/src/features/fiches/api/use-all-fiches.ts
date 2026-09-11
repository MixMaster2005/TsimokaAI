import { useQueries } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { ficheKeys } from '@/features/fiches/api/keys';
import type { Space } from '@/features/espaces/types';
import type { Fiche } from '@/features/fiches/types';
import type { Validation } from '@/features/fiches/types';

export interface FicheWithSpace {
  fiche: Fiche;
  space: Space;
  validation: Validation | null;
}

/**
 * Récupère toutes les fiches de tous les espaces pour la vue "Fiches à valider".
 * Parallelise les appels par espace.
 */
export function useAllFiches(espaces: Space[] | undefined) {
  const spaceIds = espaces?.map((s) => s.id) ?? [];

  const fichesResults = useQueries({
    queries: spaceIds.map((id) => ({
      queryKey: ficheKeys.forSpace(id),
      queryFn: () => apiClient.get<Fiche[]>(`/api/v1/fiches/espace/${id}`),
      enabled: spaceIds.length > 0,
    })),
  });

  const validationResults = useQueries({
    queries: fichesResults.flatMap((result) =>
      (result.data ?? []).map((fiche) => ({
        queryKey: ficheKeys.validation(fiche.id),
        queryFn: async () => {
          try {
            return await apiClient.get<Validation>(`/api/v1/fiches/${fiche.id}/validation`);
          } catch (error) {
            if (error instanceof Error && 'status' in error && error.status === 404) return null;
            throw error;
          }
        },
        enabled: result.isSuccess && (result.data?.length ?? 0) > 0,
      }))
    ),
  });

  if (!espaces || spaceIds.length === 0) {
    return { fiches: [] as FicheWithSpace[], isLoading: false };
  }

  const isLoading = fichesResults.some((r) => r.isLoading);

  const fiches: FicheWithSpace[] = [];
  let validationIdx = 0;

  fichesResults.forEach((result, spaceIdx) => {
    const space = espaces[spaceIdx];
    const fichesList = result.data ?? [];

    fichesList.forEach((fiche) => {
      const validation = validationResults[validationIdx]?.data ?? null;
      validationIdx++;
      fiches.push({ fiche, space, validation });
    });
  });

  return { fiches, isLoading };
}
