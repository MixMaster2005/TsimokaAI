package mg.esmia.miage.common.response;

import java.time.Instant;

/**
 * Métadonnées jointes à chaque réponse API, succès ou erreur.
 * Contrat non négociable : chaque réponse porte un request_id traçable de bout en bout.
 */
public record Meta(Instant timestamp, String requestId) {
    public static Meta now(String requestId) {
        return new Meta(Instant.now(), requestId);
    }
}
