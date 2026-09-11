import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { quizKeys } from './keys';
import type { Quiz } from '../types';

export const quizzesEspaceQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: quizKeys.espace(spaceId),
    queryFn: () => apiClient.get<Quiz[]>(`/api/v1/quizzes/espace/${spaceId}`),
  });

export function useQuizzesEspace(spaceId: string) {
  return useQuery(quizzesEspaceQueryOptions(spaceId));
}
