import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { rappelKeys } from './keys';
import type { Rappel } from '../types';

export const rappelsQueryOptions = queryOptions({
  queryKey: rappelKeys.mine,
  queryFn: () => apiClient.get<Rappel[]>('/api/v1/rappels'),
});

/** Alimente la cloche de AppSidebar — transverse, tous espaces confondus. */
export function useRappels() {
  return useQuery(rappelsQueryOptions);
}
