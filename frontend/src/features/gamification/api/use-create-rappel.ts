import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { rappelKeys } from './keys';
import type { CreateRappelPayload, Rappel } from '../types';

export function useCreateRappel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateRappelPayload) => apiClient.post<Rappel>('/api/v1/rappels', payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: rappelKeys.mine }),
  });
}
