package mg.esmia.miage.ficheservice.messaging;

import java.time.Instant;

/**
 * Événements Quiz publiés par fiche-service sur le canal {@code fiche.events}
 * (même canal que {@code FicheEvent}, via {@code RedisEventPublisher}).
 * Les consommateurs existants ignorent les types inconnus (désérialisation
 * Jackson tolérante + filtre sur {@code event}).
 */
public record QuizEvent(
        String event,
        String quizId,
        String attemptId,
        String spaceId,
        String userId,
        String userIdEtu,
        String enseignantId,
        Integer score,
        Integer total,
        Integer scoreCorrige,
        Instant timestamp
) {
    public static final String QUIZ_SUBMITTED = "QUIZ_SUBMITTED";
    public static final String QUIZ_CORRECTED = "QUIZ_CORRECTED";

    public static QuizEvent submitted(String quizId, String spaceId, String userId, int score, int total) {
        return new QuizEvent(QUIZ_SUBMITTED, quizId, null, spaceId, userId, null, null, score, total, null, Instant.now());
    }

    public static QuizEvent corrected(String quizId, String attemptId, String userIdEtu, String spaceId, String enseignantId, Integer scoreCorrige, Integer total) {
        return new QuizEvent(QUIZ_CORRECTED, quizId, attemptId, spaceId, null, userIdEtu, enseignantId, null, total, scoreCorrige, Instant.now());
    }
}
