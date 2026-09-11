package mg.esmia.miage.analyticsservice.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Événement quiz consommé HISTORIQUEMENT par analytics-service.
 * event: QUIZ_SUBMITTED | QUIZ_CORRECTED
 *
 * <p><b>Contrat actuel (canal unique {@code fiche.events}, CDC §5.4) :</b>
 * les producteurs (fiche-service, record {@code ficheservice.messaging.QuizEvent})
 * publient sur {@code fiche.events} avec {@code score}/{@code total} en Integer
 * et {@code userIdEtu} pour les corrections. Le JSON est découplé (pas de
 * dépendance croisée) : le dispatch tolérant est assuré par
 * {@link FicheEventListener} (parse {@code JsonNode}), qui fait foi côté
 * consommateur. Ce record n'est conservé que pour les constantes de type et
 * la traçabilité du format historique (Double) ; il n'est plus désérialisé.
 *
 * @deprecated Le dispatch passe par {@link FicheEventListener} ; ne plus utiliser
 *             pour désérialiser. Constantes {@code QUIZ_SUBMITTED}/{@code QUIZ_CORRECTED}
 *             encore référencées par le dispatcher.
 */
@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuizEvent(
        String event,
        String quizId,
        String spaceId,
        String userId,
        Double score,
        Double scoreCorrige,
        Double total,
        Instant timestamp
) {
    public static final String QUIZ_SUBMITTED = "QUIZ_SUBMITTED";
    public static final String QUIZ_CORRECTED = "QUIZ_CORRECTED";

    /** Canal historique dédié (SUPPRIMÉ du câblage — conservé pour traçabilité).
     * @deprecated Canal unique : {@code fiche.events} (cf. EventChannels). */
    @Deprecated
    public static final String QUIZ_EVENTS = "quiz.events";
}
