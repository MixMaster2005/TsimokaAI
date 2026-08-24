import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import type { CreateObjectifPayload, Objectif, StatutObjectif } from '../types';

const objectifKeys = {
  bySpace: (spaceId: string) => ['objectifs', 'space', spaceId] as const,
};

export const objectifsBySpaceQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: objectifKeys.bySpace(spaceId),
    queryFn: () => apiClient.get<Objectif[]>(`/api/v1/objectifs?spaceId=${spaceId}`),
  });

export function useObjectifs(spaceId: string) {
  return useQuery(objectifsBySpaceQueryOptions(spaceId));
}

export function useCreateObjectif() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateObjectifPayload) => apiClient.post<Objectif>('/api/v1/objectifs', payload),
    onSuccess: (objectif) => {
      queryClient.invalidateQueries({ queryKey: objectifKeys.bySpace(objectif.spaceId) });
    },
  });
}

export function useUpdateObjectifStatut(spaceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, statut }: { id: string; statut: StatutObjectif }) =>
      apiClient.patch<Objectif>(`/api/v1/objectifs/${id}`, { statut }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: objectifKeys.bySpace(spaceId) }),
  });
}
