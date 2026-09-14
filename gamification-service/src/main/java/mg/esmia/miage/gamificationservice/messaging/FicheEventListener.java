package mg.esmia.miage.gamificationservice.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.FicheEvent;
import mg.esmia.miage.common.events.QuizEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.gamificationservice.service.GamificationService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Dispatcher du canal unique {@code fiche.events} (CDC §5.4) côté gamification.
 *
 * <p>Parse {@code JsonNode} (champ {@code event}) pour rester tolérant aux deux
 * familles de payloads ({@code FicheEvent} / {@link QuizEvent} commun) : les types
 * QUIZ_* sont ignorés ici (aucune règle badge), les types FICHE_* alimentent
 * {@link GamificationService}. Payload incomplet -&gt; warn + ignoré, jamais
 * d'exception (contrat idempotent).
 */
@Component
@Slf4j
public class FicheEventListener extends AbstractRedisEventListener<JsonNode> {

    private final GamificationService gamificationService;

    public FicheEventListener(ObjectMapper objectMapper, GamificationService gamificationService) {
        super(objectMapper, JsonNode.class);
        this.gamificationService = gamificationService;
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
            case QuizEvent.QUIZ_SUBMITTED, QuizEvent.QUIZ_CORRECTED ->
                    log.debug("Événement quiz '{}' ignoré côté gamification (quizId={})",
                            type, text(node, "quizId"));
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
        gamificationService.onFicheGenerated(userId, spaceId);
    }

    private void onFicheValidated(JsonNode node) {
        // Événement enrichi : userId = auteur de la fiche (imputation du badge
        // PREMIERE_FICHE_VALIDEE) ; enseignantId conservé pour traçabilité.
        // Contrat historique (userId null) -> no-op loggé dans le service.
        UUID userId = uuidOrNull(text(node, "userId"));
        UUID enseignantId = uuidOrNull(text(node, "enseignantId"));
        gamificationService.onFicheValidated(userId, enseignantId, text(node, "statut"));
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
}
