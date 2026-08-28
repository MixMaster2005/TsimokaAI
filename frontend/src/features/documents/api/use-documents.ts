import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { documentsKeys } from './keys';
import type { AppDocument } from '../types';

export const documentsBySpaceQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: documentsKeys.bySpace(spaceId),
    queryFn: () => apiClient.get<AppDocument[]>(`/api/v1/documents?spaceId=${spaceId}`),
    refetchInterval: (query) => {
      const docs = query.state.data as AppDocument[] | undefined;
      const hasPending = docs?.some((d) => d.status === 'PENDING' || d.status === 'PROCESSING');
      return hasPending ? 3000 : false;
    },
  });

export function useDocuments(spaceId: string) {
  return useQuery(documentsBySpaceQueryOptions(spaceId));
}
