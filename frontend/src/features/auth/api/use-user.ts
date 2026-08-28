import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import type { User } from '../types';

export const userKeys = {
  byId: (userId: string) => ['users', userId] as const,
};

export const userQueryOptions = (userId: string) =>
  queryOptions({
    queryKey: userKeys.byId(userId),
    queryFn: () => apiClient.get<User>(`/api/v1/users/${userId}`),
  });

export function useUser(userId: string) {
  return useQuery(userQueryOptions(userId));
}
