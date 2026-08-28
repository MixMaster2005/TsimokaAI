import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { badgeKeys } from './keys';
import type { Badge } from '../types';

export const badgesQueryOptions = queryOptions({
  queryKey: badgeKeys.all,
  queryFn: () => apiClient.get<Badge[]>('/api/v1/badges'),
  staleTime: 5 * 60 * 1000,
});

export function useBadges() {
  return useQuery(badgesQueryOptions);
}
