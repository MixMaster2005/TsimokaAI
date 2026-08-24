import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { chatKeys } from './keys';
import type { Conversation, CreateConversationPayload } from '../types';

export function useCreateConversation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateConversationPayload) =>
      apiClient.post<Conversation>('/api/v1/conversations', payload),
    onSuccess: (conv) => {
      queryClient.invalidateQueries({ queryKey: chatKeys.conversations(conv.spaceId) });
    },
  });
}
