import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { quizKeys } from './keys';
import type { Quiz, GenerateQuizRequest } from '../types';

export function useGenerateQuiz() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: GenerateQuizRequest) => apiClient.post<Quiz>('/api/v1/quizzes/generate', payload),
    onSuccess: (quiz) => {
      queryClient.invalidateQueries({ queryKey: quizKeys.bySpace(quiz.spaceId) });
      queryClient.invalidateQueries({ queryKey: quizKeys.mine() });
    },
  });
}
