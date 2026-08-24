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

export interface Citation {
  /** ID du point Qdrant (= retrievedChunkIds, traçabilité conservée) */
  chunkId: string;
  documentId: string | null;
  chunkIndex: number | null;
  /** Nom de fichier lisible — null si la résolution a échoué côté back */
  documentName: string | null;
  /** Début du contenu du chunk (tronqué ~180 caractères) */
  excerpt: string | null;
}

export interface Message {
  id: string;
  conversationId: string;
  role: MessageRole;
  content: string;
  /**
   * IDs de chunks (points Qdrant), traçabilité RAG brute. Pour l'affichage,
   * préférer `citations` (document + extrait lisibles, persistés à la
   * génération par chat-service). Les messages antérieurs à la feature
   * n'ont que ces UUID bruts -> CitationChips retombe sur un placeholder.
   */
  retrievedChunkIds: string[];
  citations?: Citation[];
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
