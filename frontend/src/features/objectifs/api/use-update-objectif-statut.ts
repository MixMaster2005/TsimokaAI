import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { objectifKeys } from './keys';
import type { Objectif, StatutObjectif } from '../types';

export function useUpdateObjectifStatut(spaceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, statut }: { id: string; statut: StatutObjectif }) =>
      apiClient.patch<Objectif>(`/api/v1/objectifs/${id}`, { statut }),
    onSuccess: () => {
      if (spaceId) queryClient.invalidateQueries({ queryKey: objectifKeys.bySpace(spaceId) });
    },
  });
}
