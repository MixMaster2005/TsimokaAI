import type { Citation } from '../types';
import {
  Accordion,
  AccordionItem,
  AccordionTrigger,
  AccordionContent,
} from '@/components/ui/accordion';

interface CitationChipsProps {
  /** Citations enrichies (document + extrait), persistées par chat-service à la génération. */
  citations: Citation[];
  /**
   * Repli : les messages antérieurs à la feature n'ont que les UUID bruts
   * (retrievedChunkIds) — rendus avec le placeholder d'origine.
   */
  fallbackChunkIds: string[];
}

/**
 * Sources citées sous une réponse de l'assistant.
 *
 * Citations enrichies : chaque source est un item d'accordéon — le nom du
 * document et le numéro de passage en trigger, l'extrait visible au clic.
 * L'accordéon construit la confiance en rendant la traçabilité RAG
 * explicitement accessible (§4.3 du CDC), sans dépendre de tooltips natifs
 * invisibles sur mobile.
 *
 * Sans citation résoluble (fallback circuit breaker, document supprimé,
 * messages antérieurs à la feature) : chip placeholder minimale ; l'UUID brut
 * reste en title afin de conserver la traçabilité RAG.
 */
export function CitationChips({ citations, fallbackChunkIds }: CitationChipsProps) {
  if (citations.length === 0 && fallbackChunkIds.length === 0) return null;

  if (citations.length === 0) {
    return (
      <div className="mt-3 flex flex-wrap gap-2">
        {fallbackChunkIds.map((id, i) => (
          <span
            key={id}
            className="rounded-fiche bg-papier-carte px-2.5 py-1 font-mono text-[0.68rem] text-encre"
            title={id}
          >
            [{i + 1}] source
          </span>
        ))}
      </div>
    );
  }

  return (
    <Accordion type="multiple" className="mt-3">
      {citations.map((c, i) => {
        const label = c.documentName ?? 'Document source';
        return (
          <AccordionItem key={c.chunkId} value={c.chunkId}>
            <AccordionTrigger className="rounded-fiche bg-papier-carte px-2.5 py-2 font-mono text-[0.68rem] text-encre hover:no-underline">
              <span className="flex min-w-0 items-baseline gap-2">
                <span className="shrink-0 text-encre-muted">[{i + 1}]</span>
                <span className="truncate">{label}</span>
                {c.chunkIndex !== null && (
                  <span className="shrink-0 text-encre-muted">p.{c.chunkIndex + 1}</span>
                )}
              </span>
            </AccordionTrigger>
            {c.excerpt && (
              <AccordionContent className="px-2.5 pb-2">
                <p className="font-sans text-xs leading-relaxed text-encre-muted">
                  &laquo;&nbsp;{c.excerpt}&nbsp;&raquo;
                </p>
              </AccordionContent>
            )}
          </AccordionItem>
        );
      })}
    </Accordion>
  );
}
