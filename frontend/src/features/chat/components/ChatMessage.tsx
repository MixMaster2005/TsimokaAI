import { Info } from 'lucide-react';

import { PersonaModal } from '@/features/espaces/components/PersonaModal';
import { useChalkReveal } from './use-chalk-reveal';
import { CitationChips } from './CitationChips';
import { MessageRenderer } from './MessageRenderer';
import type { Message } from '../types';

interface ChatMessageProps {
  message: Message;
  /** Anime l'effet craie uniquement pour le tout dernier message assistant reçu, pas au chargement de l'historique */
  animate?: boolean;
  /** Mode enseignant : pastille discrète ouvrant le PersonaModal sur les messages assistant. */
  showPersonaInfo?: boolean;
  spaceId?: string;
}

/** Pastille discrète sous un message assistant — enseignant + propriétaire uniquement (via showPersonaInfo). */
function PersonaInfoButton({ spaceId, createdAt, personaVersion }: { spaceId: string; createdAt: string; personaVersion?: number | null }) {
  return (
    <span className="mt-1 inline-flex items-center gap-1.5">
      <PersonaModal
        spaceId={spaceId}
        contextMessageCreatedAt={createdAt}
        trigger={
          <button
            type="button"
            title="Voir le persona actif"
            aria-label="Voir le persona actif"
            className="inline-flex items-center gap-1 text-xs text-muted-foreground opacity-60 transition-opacity hover:opacity-100"
          >
            <Info className="size-3.5" />
            <span className="sr-only">Voir le persona actif</span>
          </button>
        }
      />
      {typeof personaVersion === 'number' && (
        <span className="font-mono text-[0.62rem] text-muted-foreground" title={`Généré avec le persona v${personaVersion}`}>
          persona v{personaVersion}
        </span>
      )}
    </span>
  );
}

export function ChatMessage({ message, animate = false, showPersonaInfo = false, spaceId }: ChatMessageProps) {
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
        {showPersonaInfo && spaceId && (
          <PersonaInfoButton spaceId={spaceId} createdAt={message.createdAt} personaVersion={message.personaVersion} />
        )}
      </div>
    );
  }

  // Fallback : rendu texte brut avec animation craie (messages antérieurs)
  return (
    <div className="mb-6">
      <p className="text-base leading-relaxed text-foreground">{revealed}</p>
      <CitationChips citations={message.citations ?? []} fallbackChunkIds={message.retrievedChunkIds} />
      {showPersonaInfo && spaceId && (
        <PersonaInfoButton spaceId={spaceId} createdAt={message.createdAt} personaVersion={message.personaVersion} />
      )}
    </div>
  );
}
