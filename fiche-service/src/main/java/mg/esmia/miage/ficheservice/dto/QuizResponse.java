package mg.esmia.miage.ficheservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import mg.esmia.miage.ficheservice.entity.Quiz;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Réponse quiz SANS les réponses (pour l'étudiant qui consulte).
 */
public record QuizResponse(
        UUID id, UUID spaceId, UUID userId, String title,
        String scope, UUID targetDocumentId, String targetTopic,
        UUID sourceFicheId, List<UUID> sourceDocumentIds,
        String difficulty, int questionCount,
        @JsonProperty("contentJson") String contentJsonSansReponses,
        boolean obsolete, Instant generatedAt, Instant updatedAt
) {
    public static QuizResponse from(Quiz q) {
        return new QuizResponse(
                q.getId(), q.getSpaceId(), q.getUserId(), q.getTitle(),
                q.getScope(), q.getTargetDocumentId(), q.getTargetTopic(),
                q.getSourceFicheId(), List.of(q.getSourceDocumentIds()),
                q.getDifficulty(), q.getQuestionCount(),
                q.getContentJson(),
                q.isObsolete(), q.getGeneratedAt(), q.getUpdatedAt()
        );
    }
}
