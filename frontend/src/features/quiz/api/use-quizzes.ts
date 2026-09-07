import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { quizKeys } from './keys';
import type { Quiz } from '../types';

export const quizzesBySpaceQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: quizKeys.bySpace(spaceId),
    queryFn: () => apiClient.get<Quiz[]>(`/api/v1/quizzes?spaceId=${spaceId}`),
  });

export const quizzesMineQueryOptions = queryOptions({
  queryKey: quizKeys.mine(),
  queryFn: () => apiClient.get<Quiz[]>('/api/v1/quizzes/mine'),
});

export function useQuizzes(spaceId: string) {
  return useQuery(quizzesBySpaceQueryOptions(spaceId));
}

export function useQuizzesMine() {
  return useQuery(quizzesMineQueryOptions);
}
