package mg.esmia.miage.common.events;

import java.time.Instant;

/**
 * Publié par ingestion-service sur le canal "ingestion.events".
 * event: DOCUMENT_READY | DOCUMENT_FAILED
 * Consommé par space-service (enrichissement du persona pédagogique).
 */
public record IngestionEvent(
        String event,
        String documentId,
        String spaceId,
        String userId,
        Integer chunkCount,
        String reason,
        Instant timestamp
) {
    public static final String DOCUMENT_READY = "DOCUMENT_READY";
    public static final String DOCUMENT_FAILED = "DOCUMENT_FAILED";

    public static IngestionEvent ready(String documentId, String spaceId, String userId, int chunkCount) {
        return new IngestionEvent(DOCUMENT_READY, documentId, spaceId, userId, chunkCount, null, Instant.now());
    }

    public static IngestionEvent failed(String documentId, String spaceId, String userId, String reason) {
        return new IngestionEvent(DOCUMENT_FAILED, documentId, spaceId, userId, null, reason, Instant.now());
    }
}
