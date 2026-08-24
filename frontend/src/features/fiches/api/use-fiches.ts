import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { ficheKeys } from './keys';
import type { Fiche } from '../types';

export const fichesBySpaceQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: ficheKeys.bySpace(spaceId),
    queryFn: () => apiClient.get<Fiche[]>(`/api/v1/fiches?spaceId=${spaceId}`),
  });

export function useFiches(spaceId: string) {
  return useQuery(fichesBySpaceQueryOptions(spaceId));
}
