import { useState } from 'react';
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
import { useConversations } from '@/features/chat/api/use-conversations';
import { useCreateConversation } from '@/features/chat/api/use-create-conversation';
import { ChatThread } from '@/features/chat/components/ChatThread';
import { useEspace } from '@/features/espaces/api/use-espace';
import { getTagColorClass } from '@/features/espaces/lib/get-tag-color';

interface ChatPageProps {
  spaceId: string;
  showSpaceBar?: boolean;
  mode?: 'etudiant' | 'enseignant';
}

export function ChatPage({ spaceId, showSpaceBar = false, mode = 'etudiant' }: ChatPageProps) {
  const { data: conversations } = useConversations(spaceId);
  const { data: space } = useEspace(spaceId);
  const createConversation = useCreateConversation();
  const [activeConversationId, setActiveConversationId] = useState<string | null>(
    conversations?.[0]?.id ?? null,
  );

  const activeId = activeConversationId ?? conversations?.[0]?.id ?? null;

  if (!conversations || conversations.length === 0) {
    return (
      <div className={cn(
        'flex h-full flex-col bg-background text-foreground',
        showSpaceBar && 'surface-ardoise',
      )}>
        {mode === 'enseignant' && (
          <div className="border-b border-papier-border bg-papier-carte/60 px-6 py-2 text-xs text-encre">
            <span className="font-medium">
              Assistant calibré : {space?.subjectTag ?? 'sans tag'} — persona actif
            </span>
            {space?.assistantPersona && (
              <span className="ml-2 max-w-md truncate text-encre-muted" title={space.assistantPersona}>
                🧠 {space.assistantPersona}
              </span>
            )}
          </div>
        )}
        <div className="flex flex-1 flex-col items-center justify-center gap-3">
        <p className="text-sm text-encre-muted">Aucune conversation dans cet espace pour l'instant.</p>
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
      </div>
    );
  }

  if (!activeId) return null;

  return (
    <SidebarProvider defaultOpen>
      <SidebarInset className={cn(
        'h-svh overflow-y-auto bg-background text-foreground',
        showSpaceBar && 'surface-ardoise',
      )}>
        {mode === 'enseignant' && (
          <div className="border-b border-papier-border bg-papier-carte/60 px-6 py-2 text-xs text-encre">
            <span className="font-medium">
              Assistant calibré : {space?.subjectTag ?? 'sans tag'} — persona actif
            </span>
            {space?.assistantPersona && (
              <span className="ml-2 hidden max-w-md truncate text-encre-muted sm:inline" title={space.assistantPersona}>
                🧠 {space.assistantPersona}
              </span>
            )}
          </div>
        )}
        {showSpaceBar && (
          <div className="flex items-center justify-between border-b border-papier-border bg-papier-carte/30 px-6 py-2 text-xs">
            <div className="flex min-w-0 items-center gap-2">
              <span className="truncate font-medium text-encre">{space?.name ?? 'Espace'}</span>
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
              <p className="hidden max-w-md truncate text-[0.72rem] text-encre-muted sm:block" title={space.assistantPersona}>
                🧠 {space.assistantPersona}
              </p>
            )}
          </div>
        )}

        <div className="border-b border-papier-border px-4 py-2 md:hidden">
          <select
            value={activeId}
            onChange={(e) => setActiveConversationId(e.target.value)}
            className="w-full bg-transparent font-mono text-xs text-encre-muted"
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
                      <span className="hidden shrink-0 font-mono text-[0.6rem] text-encre-muted sm:inline">
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
