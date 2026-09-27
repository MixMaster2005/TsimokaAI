import { useState } from 'react';
import { Brain, Plus } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarHeader,
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
import { useSession } from '@/features/auth/api/use-session';
import { useEspace } from '@/features/espaces/api/use-espace';
import { PersonaModal } from '@/features/espaces/components/PersonaModal';
import { getTagColorClass } from '@/features/espaces/lib/get-tag-color';

interface ChatPageProps {
  spaceId: string;
  showSpaceBar?: boolean;
  mode?: 'etudiant' | 'enseignant';
}

export function ChatPage({ spaceId, showSpaceBar = false, mode = 'etudiant' }: ChatPageProps) {
  const { data: conversations, isLoading, isError, refetch } = useConversations(spaceId);
  const { data: space } = useEspace(spaceId);
  const { data: session } = useSession();
  const createConversation = useCreateConversation();
  const [activeConversationId, setActiveConversationId] = useState<string | null>(
    conversations?.[0]?.id ?? null,
  );

  const activeId = activeConversationId ?? conversations?.[0]?.id ?? null;

  const isOwner = space
    ? (space.owner ?? (session?.id !== undefined && space.userId === session.id))
    : false;
  // Persona V1 : déclencheurs visibles en mode enseignant pour le propriétaire uniquement.
  const showPersona = mode === 'enseignant' && isOwner;

  if (isLoading) {
    return (
      <div className={cn(
        'flex h-full flex-col gap-4 p-6 bg-background text-foreground',
        showSpaceBar && 'surface-ardoise',
        )}>
        <div role="status" aria-live="polite" className="flex flex-1 flex-col gap-3" aria-busy="true">
          <Skeleton className="h-10 w-1/3" />
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-16 w-5/6" />
          <span className="sr-only">Chargement des conversations…</span>
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className={cn(
        'flex h-full flex-col bg-background text-foreground',
        showSpaceBar && 'surface-ardoise',
        )}>
        <div className="flex flex-1 flex-col items-start justify-center gap-3 p-6">
          <p role="alert" className="text-sm text-erreur">
            Impossible de charger les conversations. Vérifie ta connexion puis réessaie.
          </p>
          <Button variant="outline" size="sm" onClick={() => refetch()}>
            Réessayer
          </Button>
        </div>
      </div>
    );
  }

  if (conversations !== undefined && conversations.length === 0) {
    return (
      <div className={cn(
        'flex h-full flex-col bg-background text-foreground',
        showSpaceBar && 'surface-ardoise',
        )}>
        {showPersona && (
          <div className="flex items-center justify-end border-b border-border px-4 py-2 sm:px-6">
            <PersonaHeaderButton spaceId={spaceId} version={space?.personaVersion} />
          </div>
        )}
        <div className="flex flex-1 flex-col items-center justify-center gap-3">
        <p className="text-sm text-encre-muted">Aucune conversation dans cet espace pour l'instant.</p>
        {createConversation.isError && (
          <p role="alert" className="text-xs text-erreur">
            La création a échoué — réessaie.
          </p>
        )}
        <Button
          onClick={() =>
            createConversation.mutate(
              { spaceId },
              { onSuccess: (conv) => setActiveConversationId(conv.id) },
            )
          }
          disabled={createConversation.isPending}
        >
          {createConversation.isPending ? 'Création…' : 'Démarrer une conversation'}
        </Button>
        </div>
      </div>
    );
  }

  if (!activeId) {
    return (
      <div className={cn(
        'flex h-full flex-col bg-background text-foreground',
        showSpaceBar && 'surface-ardoise',
        )}>
        <div className="flex flex-1 flex-col items-start justify-center gap-3 p-6">
          <p className="font-display text-base font-semibold text-foreground">
            Sélectionne une conversation
          </p>
          <p className="text-sm text-muted-foreground">
            Choisis un échange dans la liste, ou démarre une nouvelle conversation.
          </p>
          <Button
            onClick={() =>
              createConversation.mutate(
                { spaceId },
                { onSuccess: (conv) => setActiveConversationId(conv.id) },
              )
            }
            disabled={createConversation.isPending}
          >
            {createConversation.isPending ? 'Création…' : 'Nouvelle conversation'}
          </Button>
        </div>
      </div>
    );
  }

  const liste = conversations ?? [];

  // Un seul landmark <main id="contenu"> (celui du layout) : le conteneur
  // interne est une <div> aux mêmes classes, pas un second SidebarInset.
  // Le SidebarProvider est conservé (useSidebar dans ConversationRail).
  return (
    <SidebarProvider defaultOpen className={cn(showSpaceBar && 'surface-ardoise')}>
      <div className="relative flex h-svh w-full flex-1 flex-col overflow-y-auto bg-background text-foreground">
        {showPersona && (
          <div className="flex items-center justify-end border-b border-border px-4 py-2 sm:px-6">
            <PersonaHeaderButton spaceId={spaceId} version={space?.personaVersion} />
          </div>
        )}
        {showSpaceBar && (
          <div className="flex items-center justify-between border-b border-border bg-background px-4 py-2 text-xs sm:px-6">
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
        )}

        <div className="border-b border-border px-4 py-2 md:hidden">
          <label htmlFor="conversation-active" className="sr-only">
            Conversation active…
          </label>
          <select
            id="conversation-active"
            value={activeId}
            onChange={(e) => setActiveConversationId(e.target.value)}
            className="h-11 w-full bg-transparent font-mono text-xs text-muted-foreground"
          >
            {liste.map((c) => (
              <option key={c.id} value={c.id} className="bg-background text-foreground">
                {c.title ?? 'Sans titre'}
              </option>
            ))}
          </select>
        </div>

        <div className="min-h-0 flex-1">
          <ChatThread conversationId={activeId} spaceId={spaceId} showPersonaInfo={showPersona} />
        </div>
      </div>

      <ConversationRail
        conversations={liste}
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

/** Bouton global Persona (Brain + vN) — en-tête, enseignant + propriétaire uniquement. */
function PersonaHeaderButton({ spaceId, version }: { spaceId: string; version?: number | null }) {
  return (
    <PersonaModal
      spaceId={spaceId}
      trigger={
        <Button variant="ghost" size="sm" className="gap-1.5 text-xs text-muted-foreground">
          <Brain className="size-4" aria-hidden="true" />
          Persona
          {typeof version === 'number' && <span className="font-mono">v{version}</span>}
        </Button>
      }
    />
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
    <Sidebar
      side="right"
      collapsible="icon"
    >
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton asChild size="lg" tooltip="Nouvelle conversation">
              <Button onClick={onCreate} disabled={creating} aria-label="Nouvelle conversation">
                <Plus className="size-4" aria-hidden="true" />
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
                      <span className="min-w-0 flex-1 truncate text-sm text-foreground sm:text-base">
                        {conv.title ?? 'Sans titre'}
                      </span>
                      <span className="hidden shrink-0 font-mono text-[0.6rem] tabular-nums text-muted-foreground sm:inline">
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
