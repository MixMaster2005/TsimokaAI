import { useChalkReveal } from './use-chalk-reveal';
import { CitationChips } from './CitationChips';
import { MessageRenderer } from './MessageRenderer';
import type { Message } from '../types';

interface ChatMessageProps {
  message: Message;
  /** Anime l'effet craie uniquement pour le tout dernier message assistant reçu, pas au chargement de l'historique */
  animate?: boolean;
}

export function ChatMessage({ message, animate = false }: ChatMessageProps) {
  const revealed = useChalkReveal(message.content, animate && message.role === 'ASSISTANT');

  if (message.role === 'USER') {
    return (
      <div className="mb-6 flex justify-end">
        <div className="max-w-[80%] rounded-md border border-border bg-secondary px-4 py-3 text-sm text-secondary-foreground">
          {message.content}
        </div>
      </div>
    );
  }

  // Si des blocs structurés sont disponibles, utiliser le renderer riche
  if (message.blocks && message.blocks.length > 0) {
    return (
      <div className="mb-6">
        <MessageRenderer blocks={message.blocks} />
        <CitationChips citations={message.citations ?? []} fallbackChunkIds={message.retrievedChunkIds} />
      </div>
    );
  }

  // Fallback : rendu texte brut avec animation craie (messages antérieurs)
  return (
    <div className="mb-6">
      <p className="text-base leading-relaxed text-foreground">{revealed}</p>
      <CitationChips citations={message.citations ?? []} fallbackChunkIds={message.retrievedChunkIds} />
    </div>
  );
}
