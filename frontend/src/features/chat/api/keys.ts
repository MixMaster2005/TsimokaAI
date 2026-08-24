export const chatKeys = {
  all: ['chat'] as const,
  conversations: (spaceId: string) => [...chatKeys.all, 'conversations', spaceId] as const,
  messages: (conversationId: string) => [...chatKeys.all, 'messages', conversationId] as const,
};
