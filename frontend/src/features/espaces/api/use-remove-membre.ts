import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { espaceKeys } from './keys';

/** Retrait d'un membre par le propriétaire. */
export function useRemoveMembre(espaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (memberId: string) =>
      apiClient.delete<void>(`/api/v1/spaces/${espaceId}/membres/${memberId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: espaceKeys.membres(espaceId) });
    },
  });
}
