import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { documentsKeys } from './keys';
import type { AppDocument } from '../types';

export const documentsBySpaceQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: documentsKeys.bySpace(spaceId),
    queryFn: () => apiClient.get<AppDocument[]>(`/api/v1/documents?spaceId=${spaceId}`),
  });

/**
 * Liste les documents d'un espace — hook de données pur.
 *
 * Le SSE (useDocumentSse) n'est PLUS appelé ici pour éviter les connexions
 * en double. Il est appelé au niveau de la page/layout qui a besoin des
 * updates temps réel (documents page, enseignant layout).
 */
export function useDocuments(spaceId: string) {
  return useQuery(documentsBySpaceQueryOptions(spaceId));
}
