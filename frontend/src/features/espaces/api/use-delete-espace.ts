import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';

import { apiClient } from '@/lib/api-client';
import { espaceKeys } from './keys';

export function useDeleteEspace(spaceId: string) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  return useMutation({
    mutationFn: () => apiClient.delete<void>(`/api/v1/spaces/${spaceId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: espaceKeys.mine() });
      navigate({ to: '/' });
    },
  });
}
