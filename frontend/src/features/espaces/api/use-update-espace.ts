import { useMutation, useQueryClient } from '@tanstack/react-query';

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
      queryClient.invalidateQueries({ queryKey: espaceKeys.allSpaces() });
    },
  });
}
