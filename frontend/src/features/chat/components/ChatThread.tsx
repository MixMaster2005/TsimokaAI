import { useEffect, useRef } from 'react';

import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { ChatInput } from './ChatInput';
import { ChatMessage } from './ChatMessage';
import { useMessages } from '../api/use-messages';
import { useSendMessage } from '../api/use-send-message';

interface ChatThreadProps {
  conversationId: string;
  spaceId: string;
  /** Mode enseignant : pastille persona discrète sur les messages assistant. */
  showPersonaInfo?: boolean;
}

export function ChatThread({ conversationId, spaceId, showPersonaInfo = false }: ChatThreadProps) {
  const { data: messages, isLoading, isError, refetch } = useMessages(conversationId);
  const sendMessage = useSendMessage(conversationId, spaceId);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Défilement borné au fil de messages : scrollIntoView() entraînerait aussi
    // les ancêtres overflow:hidden (colonne chat) et décalerait tout l'écran.
    const reduit = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    scrollRef.current?.scrollTo({
      top: scrollRef.current.scrollHeight,
      behavior: reduit ? 'auto' : 'smooth',
    });
  }, [messages?.length]);

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div ref={scrollRef} className="mx-auto w-full max-w-2xl min-h-0 flex-1 overflow-y-auto px-4 py-6 sm:px-6" aria-busy={isLoading || sendMessage.isPending}>
        {isLoading && (
          <div role="status" aria-live="polite" className="flex flex-col gap-4">
            <Skeleton className="h-16 w-3/4" />
            <Skeleton className="h-10 w-1/2 self-end" />
            <Skeleton className="h-16 w-2/3" />
            <span className="sr-only">Chargement de la conversation…</span>
          </div>
        )}
        {isError && (
          <div className="flex flex-col items-start gap-3">
            <p role="alert" className="text-sm text-erreur">
              Impossible de charger cette conversation. Vérifie ta connexion puis réessaie.
            </p>
            <Button variant="outline" size="sm" onClick={() => refetch()}>
              Réessayer
            </Button>
          </div>
        )}
        {!isLoading && !isError && messages?.length === 0 && (
          <div className="flex flex-col items-start gap-2">
            <p className="font-display text-base font-semibold text-foreground">
              Pose ta première question
            </p>
            <p className="text-sm text-muted-foreground">
              Ex : « Résume le chapitre 2… », « Donne-moi un exemple concret… », « Teste-moi sur les définitions… »
            </p>
          </div>
        )}
        {messages?.map((message, i) => (
          <ChatMessage
            key={message.id}
            message={message}
            // N'anime que le tout dernier message si on vient de l'envoyer (mutation pas encore "settled")
            animate={i === messages.length - 1 && sendMessage.isPending === false && sendMessage.isSuccess}
            showPersonaInfo={showPersonaInfo}
            spaceId={spaceId}
          />
        ))}
        {sendMessage.isPending && (
          <p role="status" aria-live="polite" className="mt-2 text-sm text-muted-foreground">
            Envoi en cours…
          </p>
        )}
      </div>

      {/* Pied fixe : ne défile jamais, padding explicite + filet de séparation. */}
      <div className="mx-auto w-full max-w-2xl shrink-0 border-t border-border bg-background px-4 pb-4 pt-3 sm:px-6">
        <ChatInput
          onSend={(content) => sendMessage.mutate({ content })}
          disabled={sendMessage.isPending}
        />
      </div>
    </div>
  );
}
