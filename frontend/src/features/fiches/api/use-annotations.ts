import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { ficheKeys } from './keys';
import type { Annotation } from '../types';

export const annotationsQueryOptions = (ficheId: string) =>
  queryOptions({
    queryKey: ficheKeys.annotations(ficheId),
    queryFn: () => apiClient.get<Annotation[]>(`/api/v1/fiches/${ficheId}/annotations`),
  });

/** Ajoute une annotation sur la fiche (section visée optionnelle). */
export function useAddAnnotation(ficheId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: { contenu: string; sectionRef?: string }) =>
      apiClient.post<Annotation>(`/api/v1/fiches/${ficheId}/annotations`, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ficheKeys.annotations(ficheId) });
    },
  });
}
