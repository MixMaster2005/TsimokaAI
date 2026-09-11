import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { membresGroupeKeys } from './keys';
import type { MembreGroupe } from '../types';
import type { AddMembrePayload } from '../types';

export function useAddMembreGroupe(groupeId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: AddMembrePayload) =>
      apiClient.post<MembreGroupe>(`/api/v1/groupes/${groupeId}/membres`, payload),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: membresGroupeKeys.byGroupe(groupeId) }),
  });
}
