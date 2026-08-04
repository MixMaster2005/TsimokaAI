package mg.esmia.miage.common.events;

import java.time.Instant;

/**
 * Publié par fiche-service sur le canal "fiche.events".
 * event: FICHE_GENERATED | FICHE_VALIDATED
 * Consommé par analytics-service (progression) et gamification-service (badges).
 */
public record FicheEvent(
        String event,
        String ficheId,
        String spaceId,
        String userId,
        String enseignantId,
        String statut,
        Instant timestamp
) {
    public static final String FICHE_GENERATED = "FICHE_GENERATED";
    public static final String FICHE_VALIDATED = "FICHE_VALIDATED";

    public static FicheEvent generated(String ficheId, String spaceId, String userId) {
        return new FicheEvent(FICHE_GENERATED, ficheId, spaceId, userId, null, null, Instant.now());
    }

    public static FicheEvent validated(String ficheId, String enseignantId, String statut) {
        return new FicheEvent(FICHE_VALIDATED, ficheId, null, null, enseignantId, statut, Instant.now());
    }
}
