import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { quizKeys } from './keys';
import type { QuizAttempt, SubmitQuizAttemptRequest } from '../types';

export function useSubmitQuiz(quizId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: SubmitQuizAttemptRequest) =>
      apiClient.post<QuizAttempt>(`/api/v1/quizzes/${quizId}/attempts`, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: quizKeys.attempts(quizId) });
    },
  });
}
