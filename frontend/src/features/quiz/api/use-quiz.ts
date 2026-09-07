import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { quizKeys } from './keys';
import type { Quiz } from '../types';

export const quizQueryOptions = (id: string) =>
  queryOptions({
    queryKey: quizKeys.detail(id),
    queryFn: () => apiClient.get<Quiz>(`/api/v1/quizzes/${id}`),
  });

export function useQuiz(id: string) {
  return useQuery(quizQueryOptions(id));
}
