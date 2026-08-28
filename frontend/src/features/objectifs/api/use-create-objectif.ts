import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { objectifKeys } from './keys';
import type { CreateObjectifPayload, Objectif } from '../types';

export function useCreateObjectif() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateObjectifPayload) => apiClient.post<Objectif>('/api/v1/objectifs', payload),
    onSuccess: (objectif) => {
      queryClient.invalidateQueries({ queryKey: objectifKeys.bySpace(objectif.spaceId) });
    },
  });
}
