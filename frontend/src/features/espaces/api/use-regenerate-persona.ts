import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import type { Space } from '../types';
import { espaceKeys } from './keys';

/** Régénère le persona pédagogique (recalibre le registre disciplinaire) — propriétaire ou admin. */
export function useRegeneratePersona(espaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => apiClient.post<Space>(`/api/v1/spaces/${espaceId}/persona/regenerate`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: espaceKeys.detail(espaceId) });
    },
  });
}
