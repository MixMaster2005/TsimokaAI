package mg.esmia.miage.ficheservice.dto;

import mg.esmia.miage.ficheservice.entity.QuizCorrection;

import java.time.Instant;
import java.util.UUID;

public record QuizCorrectionResponse(
        UUID id, UUID quizId, UUID attemptId, UUID enseignantId,
        String commentaire, Integer scoreCorrige, Instant createdAt
) {
    public static QuizCorrectionResponse from(QuizCorrection c) {
        return new QuizCorrectionResponse(
                c.getId(), c.getQuizId(), c.getAttemptId(), c.getEnseignantId(),
                c.getCommentaire(), c.getScoreCorrige(), c.getCreatedAt()
        );
    }
}
