import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import type { MembreGroupe } from '../types';

export const membresGroupeKeys = {
  byGroupe: (groupeId: string) => ['groupes', groupeId, 'membres'] as const,
};

export const membresGroupeQueryOptions = (groupeId: string) =>
  queryOptions({
    queryKey: membresGroupeKeys.byGroupe(groupeId),
    queryFn: () => apiClient.get<MembreGroupe[]>(`/api/v1/groupes/${groupeId}/membres`),
  });

export function useMembresGroupe(groupeId: string) {
  return useQuery(membresGroupeQueryOptions(groupeId));
}
