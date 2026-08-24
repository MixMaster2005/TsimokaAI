import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { chatKeys } from './keys';
import type { Message } from '../types';

export const messagesQueryOptions = (conversationId: string) =>
  queryOptions({
    queryKey: chatKeys.messages(conversationId),
    queryFn: () => apiClient.get<Message[]>(`/api/v1/conversations/${conversationId}/messages`),
    // Une conversation active change souvent (nouveaux messages) — staleTime
    // plus court que le défaut global de query-client.ts pour cette query précise.
    staleTime: 5 * 1000,
  });

export function useMessages(conversationId: string) {
  return useQuery(messagesQueryOptions(conversationId));
}
