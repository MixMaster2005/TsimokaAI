import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';

import { apiClient } from '@/lib/api-client';
import { espaceKeys } from './keys';
import type { Space, UpdateSpacePayload } from '../types';

export function useUpdateEspace(spaceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: UpdateSpacePayload) => apiClient.put<Space>(`/api/v1/spaces/${spaceId}`, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: espaceKeys.detail(spaceId) });
      queryClient.invalidateQueries({ queryKey: espaceKeys.mine() });
    },
  });
}

export function useDeleteEspace(spaceId: string) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  return useMutation({
    mutationFn: () => apiClient.delete<void>(`/api/v1/spaces/${spaceId}`),
    onSuccess: () => {
      // SPACE_DELETED déclenche côté back la cascade (documents, fiches, conversations…)
      // — le front n'a qu'à invalider la liste et repartir vers l'étagère.
      queryClient.invalidateQueries({ queryKey: espaceKeys.mine() });
      navigate({ to: '/' });
    },
  });
}
