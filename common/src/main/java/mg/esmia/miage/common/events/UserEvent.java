package mg.esmia.miage.common.events;

import java.time.Instant;

/**
 * Publié par user-service sur "user.events".
 * event: USER_DELETED -> déclenche la suppression des données utilisateur dans TOUS
 * les services (par événement, jamais par cascade synchrone inter-BDD).
 */
public record UserEvent(String event, String userId, Instant timestamp) {
    public static final String USER_DELETED = "USER_DELETED";

    public static UserEvent deleted(String userId) {
        return new UserEvent(USER_DELETED, userId, Instant.now());
    }
}
