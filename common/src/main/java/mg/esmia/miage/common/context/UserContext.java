package mg.esmia.miage.common.context;

/**
 * Contexte utilisateur reconstruit à partir des headers enrichis par la gateway.
 * Contrat de sécurité : l'auth JWT est vérifiée UNIQUEMENT à la gateway.
 * Les services backend font confiance à ces headers et ne rejouent jamais la vérification.
 */
public record UserContext(String userId, String role, String requestId) {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role) || "ENSEIGNANT".equalsIgnoreCase(role);
    }

    public boolean owns(String resourceOwnerId) {
        return userId != null && userId.equals(resourceOwnerId);
    }
}
