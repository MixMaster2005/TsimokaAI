import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient, ApiError } from '@/lib/api-client';

/** Calqué sur gamification-service/dto/WeeklyTrackingResponse.java. */
export interface WeeklyTracking {
  semaine: string;
  nbObjectifsAtteints: number;
  nbFichesGenerees: number;
  tauxProgression: number; // 0.0 - 1.0
  joursActifs: number;
}

export const weeklyTrackingQueryOptions = (spaceId?: string | null) =>
  queryOptions({
    queryKey: ['objectifs', 'weekly', 'v1', spaceId],
    queryFn: async () => {
      if (!spaceId) return null;
      try {
        return await apiClient.get<WeeklyTracking>(`/api/v1/objectifs/weekly?spaceId=${spaceId}`);
      } catch (error) {
        if (error instanceof ApiError && (error.status === 404 || error.status === 501)) {
          return null;
        }
        throw error;
      }
    },
    enabled: Boolean(spaceId),
  });

/**
 * Récapitulatif hebdomadaire (Lot 2) : GET /api/v1/objectifs/weekly servi par
 * gamification-service depuis suivi_hebdomadaire (zéros si aucune activité,
 * jamais 404). Le repli null ne sert qu'aux backends antérieurs au Lot 2.
 */
export function useWeeklyTracking(spaceId?: string | null) {
  return useQuery(weeklyTrackingQueryOptions(spaceId));
}
