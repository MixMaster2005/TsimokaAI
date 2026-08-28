import { queryOptions } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import type { Groupe } from '../types';

export const groupeKeys = {
  bySpace: (spaceId: string) => ['groupes', 'space', spaceId] as const,
};

export const groupesBySpaceQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: groupeKeys.bySpace(spaceId),
    queryFn: () => apiClient.get<Groupe[]>(`/api/v1/spaces/${spaceId}/groupes`),
  });
