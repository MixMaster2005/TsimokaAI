package mg.esmia.miage.ficheservice.dto;

import mg.esmia.miage.ficheservice.entity.Fiche;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FicheResponse(
        UUID id, UUID spaceId, UUID userId, String title,
        List<UUID> sourceDocumentIds, String contentJson,
        boolean obsolete, Instant generatedAt, Instant updatedAt
) {
    public static FicheResponse from(Fiche f) {
        return new FicheResponse(f.getId(), f.getSpaceId(), f.getUserId(), f.getTitle(),
                List.of(f.getSourceDocumentIds()), f.getContentJson(), f.isObsolete(), f.getGeneratedAt(), f.getUpdatedAt());
    }
}
