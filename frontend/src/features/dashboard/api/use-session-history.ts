import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient, ApiError } from '@/lib/api-client';

export interface SessionRevision {
  id: string;
  spaceId: string;
  titre: string;
  dureeMinutes: number;
  dateSession: string;
  nbFichesRevisees: number;
}

export const sessionHistoryQueryOptions = (spaceId?: string | null) =>
  queryOptions({
    queryKey: ['dashboard', 'sessions', spaceId],
    queryFn: async () => {
      if (!spaceId) return [];
      try {
        return await apiClient.get<SessionRevision[]>(`/api/v1/dashboard/sessions?spaceId=${spaceId}`);
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
 * TODO (Backend) : GET /api/v1/dashboard/sessions non encore disponible côté analytics-service.
 * En attendant, ce hook tente l'appel et retourne gracieusement null/vide en cas de 404.
 */
export function useSessionHistory(spaceId?: string | null) {
  return useQuery(sessionHistoryQueryOptions(spaceId));
}
