import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { rappelKeys } from './keys';

export function useDeleteRappel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => apiClient.delete<void>(`/api/v1/rappels/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: rappelKeys.mine }),
  });
}
