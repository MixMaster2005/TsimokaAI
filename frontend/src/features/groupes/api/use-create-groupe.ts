import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { groupeKeys } from './keys';
import type { CreateGroupePayload, Groupe } from '../types';

export function useCreateGroupe(spaceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateGroupePayload) =>
      apiClient.post<Groupe>(`/api/v1/spaces/${spaceId}/groupes`, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: groupeKeys.bySpace(spaceId) });
    },
  });
}
