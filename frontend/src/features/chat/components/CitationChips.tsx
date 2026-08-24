/**
 * ⚠️ CHANTIER : Message.retrievedChunkIds ne donne que des UUID de chunks.
 * Aucun endpoint de résolution chunk -> (nom du document, segment) identifié
 * dans ingestion-service au moment du scaffolding. Deux options à trancher :
 *   (a) ingestion-service expose GET /api/v1/chunks?ids=... pour résoudre en batch
 *   (b) chat-service enrichit directement MessageResponse avec ces infos
 * En attendant, on affiche un placeholder minimal plutôt que de bloquer
 * l'affichage du message — la traçabilité existe (les IDs sont là), juste
 * pas encore la présentation lisible.
 */
export function CitationChips({ chunkIds }: { chunkIds: string[] }) {
  if (chunkIds.length === 0) return null;

  return (
    <div className="mt-3 flex flex-wrap gap-2">
      {chunkIds.map((id, i) => (
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
