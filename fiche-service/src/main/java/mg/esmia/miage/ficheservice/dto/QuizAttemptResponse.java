package mg.esmia.miage.ficheservice.dto;

import mg.esmia.miage.ficheservice.entity.QuizAttempt;

import java.time.Instant;
import java.util.UUID;

public record QuizAttemptResponse(
        UUID id, UUID quizId, UUID userId,
        String answersJson, int score, int totalQuestions,
        Instant attemptedAt
) {
    public static QuizAttemptResponse from(QuizAttempt a) {
        return new QuizAttemptResponse(
                a.getId(), a.getQuizId(), a.getUserId(),
                a.getAnswersJson(), a.getScore(), a.getTotalQuestions(),
                a.getAttemptedAt()
        );
    }
}
