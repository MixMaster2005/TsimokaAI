package mg.esmia.miage.chatservice.dto;

import java.util.UUID;

/**
 * Citation lisible d'un chunk utilisé comme contexte RAG pour une réponse de
 * l'assistant. Remplace le simple UUID brut de {@code retrievedChunkIds} côté
 * affichage : l'étudiant voit D'OÙ vient l'information (document + extrait),
 * pas juste un identifiant interne.
 *
 * <p>Persistée en JSONB sur le message ASSISTANT au moment de la génération
 * (résolution du nom de document via ingestion-service à cet instant unique,
 * jamais au moment de la lecture — pas d'appel réseau par message affiché).
 * Les messages antérieurs à la feature n'ont pas de citations : le front
 * retombe alors sur les chips placeholder basées sur {@code retrievedChunkIds}.
 *
 * @param chunkId      ID du point Qdrant (= {@code retrievedChunkIds}, traçabilité conservée)
 * @param documentId   document source dans ingestion-service
 * @param chunkIndex   position du chunk dans le document
 * @param documentName nom de fichier lisible (null si la résolution a échoué)
 * @param excerpt      début du contenu du chunk (tronqué, suffisant pour repérer le passage)
 */
public record Citation(
        UUID chunkId,
        UUID documentId,
        Integer chunkIndex,
        String documentName,
        String excerpt
) {
    /** Longueur maximale de l'extrait stocké/affiché. */
    private static final int EXCERPT_MAX_LENGTH = 180;

    public static Citation of(UUID chunkId, UUID documentId, Integer chunkIndex,
                              String content, String documentName) {
        String excerpt = null;
        if (content != null && !content.isBlank()) {
            String trimmed = content.strip();
            excerpt = trimmed.length() <= EXCERPT_MAX_LENGTH
                    ? trimmed
                    : trimmed.substring(0, EXCERPT_MAX_LENGTH - 1) + "…";
        }
        return new Citation(chunkId, documentId, chunkIndex, documentName, excerpt);
    }
}
