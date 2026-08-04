package mg.esmia.miage.common.exception;

/**
 * A lever quand X-User-Id ne correspond pas au propriétaire de la ressource demandée.
 * Contrat de sécurité : toute tentative d'accès à une ressource d'un autre utilisateur
 * retourne une erreur 403.
 */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message, 403);
    }
}
