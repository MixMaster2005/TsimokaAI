import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient, ApiError } from '@/lib/api-client';

export interface WeeklyTracking {
  semaine: string;
  nbObjectifsAtteints: number;
  tauxProgression: number;
  joursActifs: number;
}

export const weeklyTrackingQueryOptions = (spaceId?: string | null) =>
  queryOptions({
    queryKey: ['objectifs', 'weekly', spaceId],
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
 * TODO (Backend) : GET /api/v1/objectifs/weekly non encore implémenté côté gamification-service.
 * En attendant, ce hook tente l'appel et retourne gracieusement null en cas de 404.
 */
export function useWeeklyTracking(spaceId?: string | null) {
  return useQuery(weeklyTrackingQueryOptions(spaceId));
}
