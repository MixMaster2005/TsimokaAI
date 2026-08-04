package mg.esmia.miage.common.exception;

/**
 * Codes d'erreur métier partagés. Chaque service peut définir ses propres codes
 * additionnels (enum local ou simples constantes String) tant qu'il respecte le
 * format { code, message, details } de l'enveloppe de réponse.
 */
public final class ErrorCode {
    private ErrorCode() {
    }

    public static final String NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String CONFLICT = "CONFLICT";
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String UPSTREAM_SERVICE_ERROR = "UPSTREAM_SERVICE_ERROR";
}
