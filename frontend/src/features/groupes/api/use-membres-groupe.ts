import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { membresGroupeKeys } from './keys';
import type { MembreGroupe } from '../types';

export const membresGroupeQueryOptions = (groupeId: string) =>
  queryOptions({
    queryKey: membresGroupeKeys.byGroupe(groupeId),
    queryFn: () => apiClient.get<MembreGroupe[]>(`/api/v1/groupes/${groupeId}/membres`),
  });

export function useMembresGroupe(groupeId: string) {
  return useQuery(membresGroupeQueryOptions(groupeId));
}
