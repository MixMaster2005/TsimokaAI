import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { chatKeys } from './keys';
import type { Message, MessageRole, SendMessagePayload } from '../types';

/**
 * ⚠️ Hypothèse à vérifier : je n'ai regardé que la signature du controller
 * (`ApiResponse<MessageResponse> send(...)`), pas l'implémentation du
 * service. Je pars du principe que la réponse est le message ASSISTANT
 * généré (le message utilisateur, lui, est connu côté front puisque c'est
 * lui qui vient de l'envoyer). Si le service renvoie autre chose
 * (ex: les deux messages), ajuster onSuccess ci-dessous en conséquence.
 *
 * Pas de streaming détecté côté back (endpoint REST classique, pas de SSE) —
 * la réponse arrive d'un coup. L'effet "craie" du contrat de design
 * (révélation mot par mot) est donc purement client-side, appliqué une fois
 * la réponse complète reçue — voir ChatMessage.tsx.
 */
export function useSendMessage(conversationId: string, spaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: SendMessagePayload) =>
      apiClient.post<Message>(`/api/v1/conversations/${conversationId}/messages`, payload),

    onMutate: async (payload) => {
      // Ajout optimiste du message utilisateur — pas besoin d'attendre le
      // réseau pour l'afficher, on sait déjà exactement ce qu'il contient.
      const optimisticUserMessage: Message = {
        id: `optimistic-${Date.now()}`,
        conversationId,
        role: 'USER' as MessageRole,
        content: payload.content,
        retrievedChunkIds: [],
        modelUsed: null,
        createdAt: new Date().toISOString(),
      };
      queryClient.setQueryData<Message[]>(chatKeys.messages(conversationId), (old) => [
        ...(old ?? []),
        optimisticUserMessage,
      ]);
    },

    onSuccess: (assistantMessage) => {
      queryClient.setQueryData<Message[]>(chatKeys.messages(conversationId), (old) => [
        ...(old ?? []),
        assistantMessage,
      ]);
      // La liste des conversations de l'espace peut avoir changé (updatedAt, titre auto…)
      queryClient.invalidateQueries({ queryKey: chatKeys.conversations(spaceId) });
    },
  });
}
