package mg.esmia.miage.ficheservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import mg.esmia.miage.ficheservice.entity.Quiz;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Vue complète du quiz (enseignant / owner) : contentJson brut avec answer/explanation.
 * Utilisée pour la correction et la consultation propriétaire.
 * Le scoring {@code QuizGenerationService.scoreAttempt} continue de lire le champ
 * {@code answer} en base (jamais depuis cette vue côté client).
 */
public record QuizDetailResponse(
        UUID id, UUID spaceId, UUID userId, String title,
        String scope, UUID targetDocumentId, String targetTopic,
        UUID sourceFicheId, List<UUID> sourceDocumentIds,
        String difficulty, int questionCount,
        @JsonProperty("contentJson") String contentJson,
        String statut,
        boolean obsolete, Instant generatedAt, Instant updatedAt
) implements QuizView {

    public static QuizDetailResponse from(Quiz q) {
        return new QuizDetailResponse(
                q.getId(), q.getSpaceId(), q.getUserId(), q.getTitle(),
                q.getScope(), q.getTargetDocumentId(), q.getTargetTopic(),
                q.getSourceFicheId(),
                q.getSourceDocumentIds() == null ? List.of() : List.of(q.getSourceDocumentIds()),
                q.getDifficulty(), q.getQuestionCount(),
                q.getContentJson(),
                q.getStatut(),
                q.isObsolete(), q.getGeneratedAt(), q.getUpdatedAt()
        );
    }
}
