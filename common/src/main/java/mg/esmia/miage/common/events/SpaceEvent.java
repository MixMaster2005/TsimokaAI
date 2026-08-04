package mg.esmia.miage.common.events;

import java.time.Instant;

/**
 * Publié par space-service sur "space.events".
 * event: SPACE_DELETED -> déclenche le nettoyage en cascade (par événement, jamais
 * par appel synchrone) dans ingestion-service, chat-service, fiche-service, analytics-service.
 */
public record SpaceEvent(String event, String spaceId, String userId, Instant timestamp) {
    public static final String SPACE_DELETED = "SPACE_DELETED";

    public static SpaceEvent deleted(String spaceId, String userId) {
        return new SpaceEvent(SPACE_DELETED, spaceId, userId, Instant.now());
    }
}
