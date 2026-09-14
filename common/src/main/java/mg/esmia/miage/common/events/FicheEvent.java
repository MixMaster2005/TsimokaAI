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

    /**
     * Événement enrichi : porte l'auteur de la fiche ({@code userId}) et son espace
     * ({@code spaceId}) en plus du validateur ({@code enseignantId}) et du statut.
     * Les consommateurs (analytics, gamification) imputent à {@code userId}.
     */
    public static FicheEvent validated(String ficheId, String spaceId, String userId, String enseignantId, String statut) {
        return new FicheEvent(FICHE_VALIDATED, ficheId, spaceId, userId, enseignantId, statut, Instant.now());
    }

    /**
     * Contrat historique (non enrichi) : {@code spaceId}/{@code userId} à null.
     * Les consommateurs doivent traiter ce cas en no-op loggé.
     *
     * @deprecated Préférer {@link #validated(String, String, String, String, String)}.
     */
    @Deprecated
    public static FicheEvent validated(String ficheId, String enseignantId, String statut) {
        return validated(ficheId, null, null, enseignantId, statut);
    }
}
