import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { quizKeys } from './keys';
import type { CreateCorrectionRequest, QuizCorrection } from '../types';

export const quizCorrectionsQueryOptions = (quizId: string) =>
  queryOptions({
    queryKey: quizKeys.corrections(quizId),
    queryFn: () => apiClient.get<QuizCorrection[]>(`/api/v1/quizzes/${quizId}/corrections`),
  });

export function useQuizCorrections(quizId: string) {
  return useQuery(quizCorrectionsQueryOptions(quizId));
}

export function useCreateQuizCorrection(quizId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateCorrectionRequest) =>
      apiClient.post<QuizCorrection>(`/api/v1/quizzes/${quizId}/corrections`, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: quizKeys.corrections(quizId) });
    },
  });
}

export const attemptCorrectionsQueryOptions = (quizId: string, attemptId: string) =>
  queryOptions({
    queryKey: quizKeys.attemptCorrections(attemptId),
    queryFn: () =>
      apiClient.get<QuizCorrection[]>(
        `/api/v1/quizzes/${quizId}/attempts/${attemptId}/corrections`,
      ),
  });

export function useAttemptCorrections(quizId: string, attemptId: string) {
  return useQuery(attemptCorrectionsQueryOptions(quizId, attemptId));
}

export function useCreateAttemptCorrection(quizId: string, attemptId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateCorrectionRequest) =>
      apiClient.post<QuizCorrection>(
        `/api/v1/quizzes/${quizId}/attempts/${attemptId}/corrections`,
        payload,
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: quizKeys.attemptCorrections(attemptId) });
      queryClient.invalidateQueries({ queryKey: quizKeys.corrections(quizId) });
    },
  });
}
