import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { ficheKeys } from './keys';
import type { Validation } from '../types';

/** État de validation de la fiche — null si jamais soumise (404 NOT_FOUND côté back). */
export const validationQueryOptions = (ficheId: string) =>
  queryOptions({
    queryKey: ficheKeys.validation(ficheId),
    queryFn: async () => {
      try {
        return await apiClient.get<Validation>(`/api/v1/fiches/${ficheId}/validation`);
      } catch (error) {
        if (error instanceof Error && 'status' in error && error.status === 404) return null;
        throw error;
      }
    },
  });

export function useValidation(ficheId: string) {
  return useQuery(validationQueryOptions(ficheId));
}
