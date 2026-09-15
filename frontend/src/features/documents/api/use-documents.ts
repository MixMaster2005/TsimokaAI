import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { documentsKeys } from './keys';
import type { AppDocument } from '../types';

/**
 * Filet de sécurité si le SSE est coupé : refetch toutes les 5s tant qu'au
 * moins un document n'a pas atteint un statut final (READY / FAILED).
 * Dès que tout est final, le polling s'arrête (false).
 */
function pendingPollingInterval(data: AppDocument[] | undefined): number | false {
  return data?.some((d) => d.status === 'PENDING' || d.status === 'PROCESSING') ? 5000 : false;
}

export const documentsBySpaceQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: documentsKeys.bySpace(spaceId),
    queryFn: () => apiClient.get<AppDocument[]>(`/api/v1/documents?spaceId=${spaceId}`),
    refetchInterval: (query) => pendingPollingInterval(query.state.data as AppDocument[] | undefined),
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
