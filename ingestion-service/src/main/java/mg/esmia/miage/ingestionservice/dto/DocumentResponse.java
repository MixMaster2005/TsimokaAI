package mg.esmia.miage.ingestionservice.dto;

import mg.esmia.miage.ingestionservice.entity.Document;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id, UUID spaceId, UUID userId, String filename, String mimeType,
        String storageUrl, Document.Status status, Integer chunkCount,
        String failureReason, Instant createdAt, Instant updatedAt
) {
    public static DocumentResponse from(Document d) {
        return new DocumentResponse(d.getId(), d.getSpaceId(), d.getUserId(), d.getFilename(), d.getMimeType(),
                d.getStorageUrl(), d.getStatus(), d.getChunkCount(), d.getFailureReason(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
