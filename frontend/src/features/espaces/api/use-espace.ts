import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { espaceKeys } from './keys';
import type { Space } from '../types';

export const espaceQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: espaceKeys.detail(spaceId),
    queryFn: () => apiClient.get<Space>(`/api/v1/spaces/${spaceId}`),
  });

export function useEspace(spaceId: string) {
  return useQuery(espaceQueryOptions(spaceId));
}
