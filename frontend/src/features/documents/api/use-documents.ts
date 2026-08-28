import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { documentsKeys } from './keys';
import { useDocumentSse } from './use-document-sse';
import type { AppDocument } from '../types';

export const documentsBySpaceQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: documentsKeys.bySpace(spaceId),
    queryFn: () => apiClient.get<AppDocument[]>(`/api/v1/documents?spaceId=${spaceId}`),
    // Plus de polling — le SSE (useDocumentSse) gère les updates temps réel
  });

export function useDocuments(spaceId: string) {
  // Active la connexion SSE pour ce space — les updates arrivent en push
  useDocumentSse(spaceId);
  return useQuery(documentsBySpaceQueryOptions(spaceId));
}
