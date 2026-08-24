import { useState } from 'react';
import { createFileRoute, useParams } from '@tanstack/react-router';

import { conversationsQueryOptions, useConversations } from '@/features/chat/api/use-conversations';
import { useCreateConversation } from '@/features/chat/api/use-create-conversation';
import { ChatThread } from '@/features/chat/components/ChatThread';

export const Route = createFileRoute('/_app/espaces/$spaceId/chat')({
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(conversationsQueryOptions(params.spaceId)),
  component: Chat,
});

/**
 * Rail d'historique : pour l'instant un simple <select> (voir le point
 * ouvert de la cartographie UI — rail permanent desktop vs tiroir à trancher
 * plus tard). Ce composant reste volontairement simple pour ne pas
 * pré-décider d'une réponse coûteuse à construire avant validation du besoin.
 */
function Chat() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId/chat' });
  const { data: conversations } = useConversations(spaceId);
  const createConversation = useCreateConversation();
  const [activeConversationId, setActiveConversationId] = useState<string | null>(
    conversations?.[0]?.id ?? null,
  );

  const activeId = activeConversationId ?? conversations?.[0]?.id ?? null;

  if (!conversations || conversations.length === 0) {
    return (
      <div className="surface-ardoise flex h-full flex-col items-center justify-center gap-3 bg-background text-foreground">
        <p className="text-sm text-muted-foreground">Aucune conversation dans cet espace pour l\u2019instant.</p>
        <button
          onClick={() =>
            createConversation.mutate(
              { spaceId },
              { onSuccess: (conv) => setActiveConversationId(conv.id) },
            )
          }
          className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground"
        >
          Démarrer une conversation
        </button>
      </div>
    );
  }

  if (!activeId) return null;

  return (
    <div className="surface-ardoise flex h-full flex-col bg-background text-foreground">
      {conversations.length > 1 && (
        <div className="border-b border-border px-6 py-2">
          <select
            value={activeId}
            onChange={(e) => setActiveConversationId(e.target.value)}
            className="bg-transparent font-mono text-xs text-muted-foreground"
          >
            {conversations.map((c) => (
              <option key={c.id} value={c.id} className="bg-background text-foreground">
                {c.title ?? 'Sans titre'}
              </option>
            ))}
          </select>
        </div>
      )}
      <div className="min-h-0 flex-1">
        <ChatThread conversationId={activeId} spaceId={spaceId} />
      </div>
    </div>
  );
}
