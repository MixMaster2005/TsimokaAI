import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { chatKeys } from './keys';
import type { Conversation } from '../types';

export const conversationsQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: chatKeys.conversations(spaceId),
    queryFn: () => apiClient.get<Conversation[]>(`/api/v1/conversations?spaceId=${spaceId}`),
  });

/**
 * Alimente le rail d'historique des conversations (point ouvert de la
 * cartographie UI, Layout Espace) — un simple <select>/liste pour l'instant,
 * à transformer en rail toujours visible sur desktop une fois cette
 * question tranchée avec l'utilisateur.
 */
export function useConversations(spaceId: string) {
  return useQuery(conversationsQueryOptions(spaceId));
}
