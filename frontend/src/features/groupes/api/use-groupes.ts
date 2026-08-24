import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import type { CreateGroupePayload, Groupe } from '../types';

const groupeKeys = {
  bySpace: (spaceId: string) => ['groupes', 'space', spaceId] as const,
};

export const groupesBySpaceQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: groupeKeys.bySpace(spaceId),
    queryFn: () => apiClient.get<Groupe[]>(`/api/v1/spaces/${spaceId}/groupes`),
  });

export function useGroupes(spaceId: string) {
  return useQuery(groupesBySpaceQueryOptions(spaceId));
}

export function useCreateGroupe(spaceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateGroupePayload) =>
      apiClient.post<Groupe>(`/api/v1/spaces/${spaceId}/groupes`, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: groupeKeys.bySpace(spaceId) }),
  });
}
