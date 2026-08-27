import { useState } from 'react';
import { createFileRoute, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { conversationsQueryOptions, useConversations } from '@/features/chat/api/use-conversations';
import { useCreateConversation } from '@/features/chat/api/use-create-conversation';
import { ChatThread } from '@/features/chat/components/ChatThread';
import { cn } from '@/lib/utils';

export const Route = createFileRoute('/_app/espaces/$spaceId/chat')({
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(conversationsQueryOptions(params.spaceId)),
  component: Chat,
});

/**
 * Layout D — Chat (Ardoise) avec rail d'historique (Papier).
 *
 * Pattern NotebookLM : les conversations passées restent visibles dans un
 * rail latéral pendant qu'on discute — renforce la confiance dans les
 * réponses de l'assistant en montrant la continuité.
 *
 * Contrat de design §2 : le rail est en surface Papier, le chat est en
 * surface Ardoise — jamais de mélange sur un même écran.
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
        <p className="text-sm text-muted-foreground">Aucune conversation dans cet espace pour l'instant.</p>
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
    <div className="flex h-full">
      {/* Rail d'historique — surface Papier (contrat de design §2) */}
      <aside className="hidden w-60 flex-none flex-col border-r border-border bg-card md:flex">
        <div className="border-b border-border px-3 py-3">
          <Button
            variant="outline"
            size="sm"
            className="w-full"
            onClick={() =>
              createConversation.mutate(
                { spaceId },
                { onSuccess: (conv) => setActiveConversationId(conv.id) },
              )
            }
            disabled={createConversation.isPending}
          >
            {createConversation.isPending ? 'Création…' : '+ Nouvelle conversation'}
          </Button>
        </div>

        <div className="flex-1 overflow-y-auto px-2 py-2">
          {conversations.map((conv) => (
            <button
              key={conv.id}
              onClick={() => setActiveConversationId(conv.id)}
              className={cn(
                'w-full rounded-md px-3 py-2.5 text-left transition-colors',
                conv.id === activeId
                  ? 'bg-secondary text-foreground'
                  : 'text-muted-foreground hover:bg-secondary/50 hover:text-foreground',
              )}
            >
              <p className="truncate text-sm font-medium">{conv.title ?? 'Sans titre'}</p>
              <p className="font-mono text-[0.62rem] text-muted-foreground">
                {new Date(conv.updatedAt).toLocaleDateString('fr-FR')}
              </p>
            </button>
          ))}
        </div>
      </aside>

      {/* Zone chat — surface Ardoise */}
      <div className="surface-ardoise min-h-0 min-w-0 flex-1 bg-background text-foreground">
        {/* Sélecteur mobile (affiché uniquement < md) */}
        <div className="border-b border-border px-4 py-2 md:hidden">
          <select
            value={activeId}
            onChange={(e) => setActiveConversationId(e.target.value)}
            className="w-full bg-transparent font-mono text-xs text-muted-foreground"
          >
            {conversations.map((c) => (
              <option key={c.id} value={c.id} className="bg-background text-foreground">
                {c.title ?? 'Sans titre'}
              </option>
            ))}
          </select>
        </div>

        <ChatThread conversationId={activeId} spaceId={spaceId} />
      </div>
    </div>
  );
}
