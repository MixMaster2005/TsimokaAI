package mg.esmia.miage.analyticsservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.analyticsservice.service.AnalyticsService;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;

import java.util.UUID;

/**
 * DÉSACTIVÉ — fusionné dans {@link FicheEventListener}.
 *
 * <p>Canal unique {@code fiche.events} (CDC §5.4) : le dispatch
 * FICHE_* + QUIZ_* est assuré par {@code FicheEventListener} (parse
 * {@code JsonNode} tolérant aux deux formes produit/consommé). Ce fichier est
 * conservé pour traçabilité mais n'est plus un bean Spring (pas de
 * {@code @Component}) et n'est abonné à aucun canal dans
 * {@code RedisListenerConfig} : un seul chemin actif.
 */
@Deprecated(forRemoval = false)
@Slf4j
public class QuizEventListener extends AbstractRedisEventListener<QuizEvent> {

    private final AnalyticsService analyticsService;

    public QuizEventListener(ObjectMapper objectMapper, AnalyticsService analyticsService) {
        super(objectMapper, QuizEvent.class);
        this.analyticsService = analyticsService;
    }

    @Override
    protected void onEvent(QuizEvent event) {
        if (QuizEvent.QUIZ_SUBMITTED.equals(event.event())) {
            if (event.spaceId() == null || event.userId() == null || event.score() == null || event.total() == null) {
                log.warn("QUIZ_SUBMITTED incomplet ignoré (quizId={})", event.quizId());
                return;
            }
            analyticsService.onQuizSubmitted(
                    UUID.fromString(event.spaceId()), UUID.fromString(event.userId()),
                    event.score(), event.total());
        } else if (QuizEvent.QUIZ_CORRECTED.equals(event.event())) {
            Double corrige = event.scoreCorrige() != null ? event.scoreCorrige() : event.score();
            if (event.spaceId() == null || event.userId() == null || corrige == null || event.total() == null) {
                log.warn("QUIZ_CORRECTED incomplet ignoré (quizId={})", event.quizId());
                return;
            }
            analyticsService.onQuizCorrected(
                    UUID.fromString(event.spaceId()), UUID.fromString(event.userId()),
                    corrige, event.total());
        }
        // Les autres types (messages chat/fiche présents sur les canaux partagés)
        // sont ignorés silencieusement.
    }
}
