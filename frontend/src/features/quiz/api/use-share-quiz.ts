import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { quizKeys } from './keys';
import type { QuizShare, ShareQuizRequest } from '../types';

export function useShareQuiz(quizId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: ShareQuizRequest) =>
      apiClient.post<QuizShare>(`/api/v1/quizzes/${quizId}/share`, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: quizKeys.shares(quizId) });
    },
  });
}
