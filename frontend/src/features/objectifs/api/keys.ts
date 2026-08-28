import { queryOptions } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import type { Objectif } from '../types';

export const objectifKeys = {
  bySpace: (spaceId: string) => ['objectifs', 'space', spaceId] as const,
};

export const objectifsBySpaceQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: objectifKeys.bySpace(spaceId),
    queryFn: () => apiClient.get<Objectif[]>(`/api/v1/objectifs?spaceId=${spaceId}`),
  });
