import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { quizKeys } from './keys';
import type { Quiz } from '../types';

export function usePublishQuiz(spaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (quizId: string) =>
      apiClient.post<Quiz>(`/api/v1/quizzes/${quizId}/publish`),
    onSuccess: (quiz) => {
      queryClient.invalidateQueries({ queryKey: quizKeys.all });
      queryClient.invalidateQueries({ queryKey: quizKeys.detail(quiz.id) });
      queryClient.invalidateQueries({ queryKey: quizKeys.bySpace(spaceId) });
      queryClient.invalidateQueries({ queryKey: quizKeys.espace(spaceId) });
    },
  });
}
