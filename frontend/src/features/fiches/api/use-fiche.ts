import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { ficheKeys } from './keys';
import type { Fiche } from '../types';

export const ficheQueryOptions = (id: string) =>
  queryOptions({
    queryKey: ficheKeys.detail(id),
    queryFn: () => apiClient.get<Fiche>(`/api/v1/fiches/${id}`),
  });

export function useFiche(id: string) {
  return useQuery(ficheQueryOptions(id));
}
