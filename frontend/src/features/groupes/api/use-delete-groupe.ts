import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { groupeKeys } from './keys';

export function useDeleteGroupe(spaceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (groupeId: string) =>
      apiClient.delete<void>(`/api/v1/groupes/${groupeId}`),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: groupeKeys.bySpace(spaceId) }),
  });
}
