import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import type { Badge } from '../types';

export const badgesQueryOptions = queryOptions({
  queryKey: ['gamification', 'badges'] as const,
  queryFn: () => apiClient.get<Badge[]>('/api/v1/badges'),
  staleTime: 5 * 60 * 1000, // la liste de badges bouge rarement
});

export function useBadges() {
  return useQuery(badgesQueryOptions);
}
