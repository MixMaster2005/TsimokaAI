import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { espaceKeys } from './keys';

/** Régénère le code (le précédent cesse de fonctionner) — propriétaire uniquement. */
export function useRegenerateInviteCode(espaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () =>
      apiClient.post<{ inviteCode: string }>(`/api/v1/spaces/${espaceId}/invite-code/regenerate`),
    onSuccess: (data) => {
      queryClient.setQueryData(espaceKeys.inviteCode(espaceId), data);
    },
  });
}
