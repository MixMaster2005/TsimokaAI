package mg.esmia.miage.common.events;

import java.time.Instant;

/**
 * Événements Quiz publiés par fiche-service sur le canal unique {@code fiche.events}
 * (même canal que {@link FicheEvent}, via {@code RedisEventPublisher}).
 * event: QUIZ_SUBMITTED | QUIZ_CORRECTED
 * Consommé par analytics-service (progression) et gamification-service (badges).
 *
 * <p>Contrat JSON découplé : les consommateurs dispatchent sur le champ
 * {@code event} (désérialisation Jackson tolérante) et ignorent les types
 * inconnus. Les champs {@code null} varient selon le type :
 * <ul>
 *   <li>QUIZ_SUBMITTED : {@code attemptId}/{@code userIdEtu}/{@code enseignantId}/
 *   {@code scoreCorrige} à null ; {@code userId} = auteur de la tentative.</li>
 *   <li>QUIZ_CORRECTED : {@code userId} null, {@code userIdEtu} = auteur de la
 *   tentative corrigée ; {@code scoreCorrige} prioritaire.</li>
 * </ul>
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
