import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { espaceKeys } from './keys';
import type { CreateSpacePayload, Space } from '../types';

export function useCreateEspace() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateSpacePayload) => apiClient.post<Space>('/api/v1/spaces', payload),
    onSuccess: () => {
      // Invalide la liste "mine" ( étudiant) ET "allSpaces" (enseignant) —
      // un nouvel espace apparaît des deux côtés.
      queryClient.invalidateQueries({ queryKey: espaceKeys.mine() });
      queryClient.invalidateQueries({ queryKey: espaceKeys.allSpaces() });
    },
  });
}
