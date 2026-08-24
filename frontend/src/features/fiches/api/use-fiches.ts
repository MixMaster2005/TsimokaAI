import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { ficheKeys } from './keys';
import type { Fiche } from '../types';

export const fichesBySpaceQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: ficheKeys.bySpace(spaceId),
    queryFn: () => apiClient.get<Fiche[]>(`/api/v1/fiches?spaceId=${spaceId}`),
  });

/** Vue transverse "Mes fiches" : GET /api/v1/fiches/mine (tous espaces confondus). */
export const fichesMineQueryOptions = queryOptions({
  queryKey: ficheKeys.mine(),
  queryFn: () => apiClient.get<Fiche[]>('/api/v1/fiches/mine'),
});

export function useFiches(spaceId: string) {
  return useQuery(fichesBySpaceQueryOptions(spaceId));
}

export function useFichesMine() {
  return useQuery(fichesMineQueryOptions);
}
