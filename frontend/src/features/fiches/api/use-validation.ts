import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { ficheKeys } from './keys';
import type { Validation, ValidationStatut } from '../types';

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

/**
 * Valide ou rejette la fiche — PUT réservé aux enseignants côté back
 * (403 sinon). La validation est unique par fiche : un nouveau verdict écrase
 * l'ancien (statut + commentaire + date).
 */
export function useValidateFiche(ficheId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: { statut: ValidationStatut; commentaire?: string }) =>
      apiClient.put<Validation>(`/api/v1/fiches/${ficheId}/validation`, payload),
    onSuccess: (validation) => {
      queryClient.setQueryData(ficheKeys.validation(ficheId), validation);
    },
  });
}
