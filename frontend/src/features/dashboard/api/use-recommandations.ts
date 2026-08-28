import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { recommandationsKeys } from './keys';
import type { Recommandation } from '../types';

export const recommandationsQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: recommandationsKeys.bySpace(spaceId),
    queryFn: () => apiClient.get<Recommandation[]>(`/api/v1/recommandations?spaceId=${spaceId}`),
  });

export function useRecommandations(spaceId: string) {
  return useQuery(recommandationsQueryOptions(spaceId));
}
