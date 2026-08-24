import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { espaceKeys } from './keys';
import type { CreateSpacePayload, Space } from '../types';

export function useCreateEspace() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateSpacePayload) => apiClient.post<Space>('/api/v1/spaces', payload),
    onSuccess: () => {
      // Invalide la liste "mine" pour qu'elle se refetch et inclue le nouvel espace —
      // pattern standard : après une mutation qui change une collection, on invalide
      // la query de collection plutôt que de bidouiller le cache à la main.
      queryClient.invalidateQueries({ queryKey: espaceKeys.mine() });
    },
  });
}
