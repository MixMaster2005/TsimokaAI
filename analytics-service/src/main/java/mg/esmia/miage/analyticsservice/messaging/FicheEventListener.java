package mg.esmia.miage.analyticsservice.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.FicheEvent;
import mg.esmia.miage.common.events.QuizEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.analyticsservice.service.AnalyticsService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Dispatcher UNIQUE du canal {@code fiche.events} (CDC §5.4).
 *
 * <p>Ce canal transporte deux familles de payloads JSON (découplés, sans
 * dépendance croisée vers les records producteurs de fiche-service) :
 * <ul>
 *   <li>{@code FICHE_GENERATED} / {@code FICHE_VALIDATED} (record
 *   {@code common.events.FicheEvent} côté producteur) ;</li>
 *   <li>{@code QUIZ_SUBMITTED} / {@code QUIZ_CORRECTED} (record
 *   {@code common.events.QuizEvent} côté producteur : {@code score}/{@code total}
 *   en Integer, {@code userIdEtu} au lieu de {@code userId} pour les corrections,
 *   champs {@code null} selon le type — cf. {@code QuizAttemptService} /
 *   {@code QuizCorrectionService}).</li>
 * </ul>
 *
 * <p>Le dispatch se fait sur {@code JsonNode} (champ {@code event}) pour rester
 * tolérant aux deux formes ; chaque branche applique une null-safety stricte
 * (payload incomplet -&gt; warn + ignoré, jamais d'exception).
 * L'ancien {@code QuizEventListener} dédié est désactivé : un seul chemin actif.
 */
@Component
@Slf4j
public class FicheEventListener extends AbstractRedisEventListener<JsonNode> {

    private final AnalyticsService analyticsService;

    public FicheEventListener(ObjectMapper objectMapper, AnalyticsService analyticsService) {
        super(objectMapper, JsonNode.class);
        this.analyticsService = analyticsService;
    }

    @Override
    protected void onEvent(JsonNode node) {
        String type = text(node, "event");
        if (type == null) {
            log.debug("Événement sans champ 'event' ignoré sur fiche.events");
            return;
        }
        switch (type) {
            case FicheEvent.FICHE_GENERATED -> onFicheGenerated(node);
            case FicheEvent.FICHE_VALIDATED -> onFicheValidated(node);
            case QuizEvent.QUIZ_SUBMITTED -> onQuizSubmitted(node);
            case QuizEvent.QUIZ_CORRECTED -> onQuizCorrected(node);
            default -> log.debug("Type d'événement '{}' ignoré sur fiche.events", type);
        }
    }

    private void onFicheGenerated(JsonNode node) {
        UUID userId = uuidOrNull(text(node, "userId"));
        UUID spaceId = uuidOrNull(text(node, "spaceId"));
        if (userId == null || spaceId == null) {
            log.warn("FICHE_GENERATED incomplet ignoré (ficheId={})", text(node, "ficheId"));
            return;
        }
        analyticsService.onFicheGenerated(userId, spaceId);
    }

    private void onFicheValidated(JsonNode node) {
        // Événement enrichi : userId/spaceId de la fiche quand le producteur les
        // renseigne ; nulls (contrat historique) -> no-op loggé dans le service.
        UUID userId = uuidOrNull(text(node, "userId"));
        UUID spaceId = uuidOrNull(text(node, "spaceId"));
        analyticsService.onFicheValidated(userId, spaceId, text(node, "statut"));
    }

    private void onQuizSubmitted(JsonNode node) {
        UUID spaceId = uuidOrNull(text(node, "spaceId"));
        UUID userId = uuidOrNull(text(node, "userId"));
        Double score = doubleOrNull(node, "score");
        Double total = doubleOrNull(node, "total");
        if (spaceId == null || userId == null || score == null || total == null) {
            log.warn("QUIZ_SUBMITTED incomplet ignoré (quizId={})", text(node, "quizId"));
            return;
        }
        analyticsService.onQuizSubmitted(spaceId, userId, score, total);
    }

    private void onQuizCorrected(JsonNode node) {
        UUID spaceId = uuidOrNull(text(node, "spaceId"));
        // Le producteur renseigne userIdEtu (et userId null) pour une correction.
        String rawUser = text(node, "userId") != null ? text(node, "userId") : text(node, "userIdEtu");
        UUID userId = uuidOrNull(rawUser);
        // Le producteur actuel ne joint ni score ni total à la correction :
        // scoreCorrige prioritaire, repli sur score (soumission), sinon ignoré.
        Double score = doubleOrNull(node, "scoreCorrige") != null
                ? doubleOrNull(node, "scoreCorrige")
                : doubleOrNull(node, "score");
        Double total = doubleOrNull(node, "total");
        if (spaceId == null || userId == null || score == null || total == null) {
            log.warn("QUIZ_CORRECTED incomplet ignoré (quizId={} attemptId={}) — "
                    + "en attente d'un payload producteur avec score/total.", text(node, "quizId"),
                    text(node, "attemptId"));
            return;
        }
        analyticsService.onQuizCorrected(spaceId, userId, score, total);
    }

    private static String text(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        String value = child.asText(null);
        return (value == null || value.isBlank()) ? null : value;
    }

    private static UUID uuidOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            log.warn("UUID invalide ignoré : '{}'", raw);
            return null;
        }
    }

    private static Double doubleOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        if (child.isNumber()) {
            return child.asDouble();
        }
        try {
            return Double.parseDouble(child.asText());
        } catch (NumberFormatException e) {
            log.warn("Nombre invalide ignoré pour '{}' : '{}'", field, child.asText());
            return null;
        }
    }
}
