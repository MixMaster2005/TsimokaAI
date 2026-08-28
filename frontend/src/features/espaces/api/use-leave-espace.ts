import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { espaceKeys } from './keys';

/** Un membre quitte l'espace de lui-même. */
export function useLeaveEspace() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (espaceId: string) => apiClient.delete<void>(`/api/v1/spaces/${espaceId}/membres/me`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: espaceKeys.all });
    },
  });
}
