import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import type { AppDocument } from '../types';

export const documentsKeys = {
  bySpace: (spaceId: string) => ['documents', 'space', spaceId] as const,
};

export const documentsBySpaceQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: documentsKeys.bySpace(spaceId),
    queryFn: () => apiClient.get<AppDocument[]>(`/api/v1/documents?spaceId=${spaceId}`),
    // Le statut d'ingestion change vite (PENDING -> PROCESSING -> READY) —
    // refetch automatique tant qu'un document n'est pas dans un état final.
    refetchInterval: (query) => {
      const docs = query.state.data as AppDocument[] | undefined;
      const hasPending = docs?.some((d) => d.status === 'PENDING' || d.status === 'PROCESSING');
      return hasPending ? 3000 : false;
    },
  });

export function useDocuments(spaceId: string) {
  return useQuery(documentsBySpaceQueryOptions(spaceId));
}
