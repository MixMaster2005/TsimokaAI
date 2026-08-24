import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import type { CreateRappelPayload, Rappel } from '../types';

const rappelKeys = { mine: ['gamification', 'rappels'] as const };

export const rappelsQueryOptions = queryOptions({
  queryKey: rappelKeys.mine,
  queryFn: () => apiClient.get<Rappel[]>('/api/v1/rappels'),
});

/** Alimente la cloche de AppSidebar — transverse, tous espaces confondus. */
export function useRappels() {
  return useQuery(rappelsQueryOptions);
}

export function useCreateRappel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateRappelPayload) => apiClient.post<Rappel>('/api/v1/rappels', payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: rappelKeys.mine }),
  });
}

export function useDeleteRappel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => apiClient.delete<void>(`/api/v1/rappels/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: rappelKeys.mine }),
  });
}
