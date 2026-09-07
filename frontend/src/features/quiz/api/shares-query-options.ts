import { queryOptions } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { quizKeys } from './keys';
import type { QuizShare } from '../types';

export const quizSharesQueryOptions = (quizId: string) =>
  queryOptions({
    queryKey: quizKeys.shares(quizId),
    queryFn: () => apiClient.get<QuizShare[]>(`/api/v1/quizzes/${quizId}/share`),
  });
