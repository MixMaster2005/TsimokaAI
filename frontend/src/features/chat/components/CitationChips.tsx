import type { Citation } from '../types';

interface CitationChipsProps {
  /** Citations enrichies (document + extrait), persistées par chat-service à la génération. */
  citations: Citation[];
  /**
   * Repli : les messages antérieurs à la feature n'ont que les UUID bruts
   * (retrievedChunkIds) — on affiche le placeholder d'origine pour eux.
   */
  fallbackChunkIds: string[];
}

/**
 * Sources citées sous une réponse de l'assistant.
 *
 * Avec citations enrichies : nom du document + extrait au survol (title natif,
 * sobre et sans dépendance supplémentaire). Plusieurs chunks du même document =
 * une chip par chunk, distinguées par leur index dans le document.
 *
 * Sans citation résoluble (fallback circuit breaker, document supprimé, anciens
 * messages) : chip placeholder minimale — la traçabilité brute reste visible via
 * l'UUID en title plutôt que de disparaître.
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
    <div className="mt-3 flex flex-col gap-1.5">
      {citations.map((c, i) => {
        const label = c.documentName ?? 'Document source';
        const title = [
          label + (c.chunkIndex !== null ? ` · passage ${c.chunkIndex + 1}` : ''),
          c.excerpt ? `\u00ab ${c.excerpt} \u00bb` : null,
        ]
          .filter(Boolean)
          .join('\n');
        return (
          <span
            key={c.chunkId}
            className="flex max-w-full items-baseline gap-2 rounded-fiche bg-papier-carte px-2.5 py-1 font-mono text-[0.68rem] text-encre"
            title={title}
          >
            <span className="shrink-0 text-encre-muted">[{i + 1}]</span>
            <span className="truncate">{label}</span>
            {c.chunkIndex !== null && <span className="shrink-0 text-encre-muted">p.{c.chunkIndex + 1}</span>}
          </span>
        );
      })}
    </div>
  );
}
