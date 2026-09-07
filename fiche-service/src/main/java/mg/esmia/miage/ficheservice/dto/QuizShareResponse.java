package mg.esmia.miage.ficheservice.dto;

import mg.esmia.miage.ficheservice.entity.QuizShare;

import java.time.Instant;
import java.util.UUID;

public record QuizShareResponse(
        UUID id, UUID quizId, UUID groupeId, UUID destinataireId,
        UUID partagePar, Instant sharedAt
) {
    public static QuizShareResponse from(QuizShare s) {
        return new QuizShareResponse(
                s.getId(), s.getQuizId(), s.getGroupeId(), s.getDestinataireId(),
                s.getPartagePar(), s.getSharedAt()
        );
    }
}
