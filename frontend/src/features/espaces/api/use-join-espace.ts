import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { espaceKeys } from './keys';
import type { Space } from '../types';

/**
 * Rejoint un espace via son code d'invitation (POST /api/v1/spaces/join).
 * Erreurs back explicites : code inconnu (404 NOT_FOUND), propriétaire (409),
 * déjà membre (409) — le composant appelant affiche error.message.
 */
export function useJoinEspace() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (code: string) =>
      apiClient.post<Space>('/api/v1/spaces/join', { code: code.trim().toUpperCase() }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: espaceKeys.mine() });
    },
  });
}
