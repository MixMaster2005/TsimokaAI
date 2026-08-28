import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { documentKeys } from './keys';
import type { AppDocument } from '../types';

export const documentQueryOptions = (documentId: string) =>
  queryOptions({
    queryKey: documentKeys.byId(documentId),
    queryFn: () => apiClient.get<AppDocument>(`/api/v1/documents/${documentId}`),
  });

export function useDocument(documentId: string) {
  return useQuery(documentQueryOptions(documentId));
}
