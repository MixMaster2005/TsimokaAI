import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { quizKeys } from './keys';
import type { QuizAttempt } from '../types';

export const quizAttemptsStatsQueryOptions = (quizId: string) =>
  queryOptions({
    queryKey: quizKeys.stats(quizId),
    queryFn: () => apiClient.get<QuizAttempt[]>(`/api/v1/quizzes/${quizId}/attempts/stats`),
  });

export function useQuizAttemptsStats(quizId: string) {
  return useQuery(quizAttemptsStatsQueryOptions(quizId));
}
