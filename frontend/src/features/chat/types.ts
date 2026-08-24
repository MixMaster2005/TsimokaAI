/** Calqué sur chat-service/dto/{ConversationResponse,MessageResponse}.java */

export type MessageRole = 'USER' | 'ASSISTANT';

export interface Conversation {
  id: string;
  spaceId: string;
  userId: string;
  title: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Message {
  id: string;
  conversationId: string;
  role: MessageRole;
  content: string;
  /**
   * IDs de chunks (ingestion-service), pas de fiches. ⚠️ Pas d'endpoint de
   * résolution chunk -> (document, segment) identifié côté ingestion-service
   * au moment du scaffolding — CitationChips.tsx affiche donc un placeholder
   * en attendant. À vérifier avant de finaliser la traçabilité CDC §4.3.
   */
  retrievedChunkIds: string[];
  modelUsed: string | null;
  createdAt: string;
}

export interface CreateConversationPayload {
  spaceId: string;
  title?: string;
}

export interface SendMessagePayload {
  content: string;
}
