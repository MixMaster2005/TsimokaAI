import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { quizKeys } from './keys';
import type { QuizAttempt } from '../types';

export const quizAttemptsQueryOptions = (quizId: string) =>
  queryOptions({
    queryKey: quizKeys.attempts(quizId),
    queryFn: () => apiClient.get<QuizAttempt[]>(`/api/v1/quizzes/${quizId}/attempts/mine`),
  });

export function useQuizAttempts(quizId: string) {
  return useQuery(quizAttemptsQueryOptions(quizId));
}
