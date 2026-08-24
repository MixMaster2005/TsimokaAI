import { useChalkReveal } from '../lib/use-chalk-reveal';
import { CitationChips } from './CitationChips';
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

  return (
    <div className="mb-6">
      <p className="text-base leading-relaxed text-foreground">{revealed}</p>
      <CitationChips citations={message.citations ?? []} fallbackChunkIds={message.retrievedChunkIds} />
    </div>
  );
}
