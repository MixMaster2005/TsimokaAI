import { useState } from 'react';
import { createFileRoute, useParams } from '@tanstack/react-router';
import { Plus } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarRail,
  useSidebar,
} from '@/components/ui/sidebar';
import { conversationsQueryOptions, useConversations } from '@/features/chat/api/use-conversations';
import { useCreateConversation } from '@/features/chat/api/use-create-conversation';
import { ChatThread } from '@/features/chat/components/ChatThread';
import { useEspace } from '@/features/espaces/api/use-espace';
import { getTagColorClass } from '@/features/espaces/lib/get-tag-color';

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
  const { data: space } = useEspace(spaceId);
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
    <SidebarProvider defaultOpen>
      {/* Zone chat — surface Ardoise */}
      <SidebarInset className="surface-ardoise h-svh overflow-y-auto bg-background text-foreground">
        {/* Barre espace + persona reminder */}
        <div className="flex items-center justify-between border-b border-border bg-card/30 px-6 py-2 text-xs">
          <div className="flex min-w-0 items-center gap-2">
            <span className="truncate font-medium text-foreground">{space?.name ?? 'Espace'}</span>
            {space?.subjectTag && (
              <span
                className={cn(
                  'rounded px-1.5 py-0.2 font-mono text-[0.62rem] font-medium uppercase text-white',
                  getTagColorClass(space.subjectTag),
                )}
              >
                {space.subjectTag}
              </span>
            )}
          </div>
          {space?.assistantPersona && (
            <p className="hidden max-w-md truncate text-[0.72rem] text-muted-foreground sm:block" title={space.assistantPersona}>
              🧠 {space.assistantPersona}
            </p>
          )}
        </div>

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

        <div className="min-h-0 flex-1">
          <ChatThread conversationId={activeId} spaceId={spaceId} />
        </div>
      </SidebarInset>

      {/* Rail d'historique — surface Papier (contrat de design §2), rétractable */}
      <ConversationRail
        conversations={conversations}
        activeId={activeId}
        onSelect={setActiveConversationId}
        onCreate={() =>
          createConversation.mutate(
            { spaceId },
            { onSuccess: (conv) => setActiveConversationId(conv.id) },
          )
        }
        creating={createConversation.isPending}
      />
    </SidebarProvider>
  );
}

function ConversationRail({
  conversations,
  activeId,
  onSelect,
  onCreate,
  creating,
}: {
  conversations: { id: string; title: string | null; updatedAt: string }[];
  activeId: string;
  onSelect: (id: string) => void;
  onCreate: () => void;
  creating: boolean;
}) {
  const { state } = useSidebar();

  return (
    <Sidebar side="right" collapsible="icon">
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton asChild size="lg" tooltip="Nouvelle conversation">
              <Button onClick={onCreate} disabled={creating} className="text-craie">
                <Plus className="size-4" />
                {state === 'expanded' && (
                  <span>{creating ? 'Création…' : 'Nouvelle conversation'}</span>
                )}
              </Button>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupContent>
            <SidebarMenu>
              {conversations.map((conv) => (
                <SidebarMenuItem key={conv.id}>
                  <SidebarMenuButton
                    asChild
                    size="lg"
                    isActive={conv.id === activeId}
                    tooltip={state === 'collapsed' ? conv.title ?? 'Sans titre' : undefined}
                    className="h-auto min-h-11 overflow-hidden py-1.5"
                  >
                    <button
                      type="button"
                      onClick={() => onSelect(conv.id)}
                      title={`${conv.title ?? 'Sans titre'} — ${new Date(conv.updatedAt).toLocaleDateString('fr-FR')}`}
                    >
                      <span className="min-w-0 flex-1 truncate text-sm text-encre sm:text-base">
                        {conv.title ?? 'Sans titre'}
                      </span>
                      <span className="hidden shrink-0 font-mono text-[0.6rem] text-muted-foreground sm:inline">
                        {new Date(conv.updatedAt).toLocaleDateString('fr-FR')}
                      </span>
                    </button>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
      <SidebarRail />
    </Sidebar>
  );
}
