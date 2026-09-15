package mg.esmia.miage.chatservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.chatservice.client.dto.SpaceResponse;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Client REST service-à-service vers {@code space-service} pour récupérer le persona
 * pédagogique de l'espace (GET /api/v1/spaces/{id}).
 *
 * <p>La gateway vérifie le JWT puis injecte les headers {@code X-User-Id}/X-Role ; en appel
 * interne on reproduit ces headers avec l'identité du propriétaire de la conversation (connue
 * côté chat) — space-service n'authentifie pas de nouveau, il fait confiance aux headers
 * (contrat de sécurité du mémoire).
 *
 * <p>Défaillance non bloquante : en cas d'erreur (space-service injoignable, espace supprimé...),
 * la méthode retourne {@code null} et ChatService bascule sur un persona générique.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpaceClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${space-service.url:http://localhost:8082}")
    private String spaceServiceUrl;

    /**
     * @return l'espace (persona + version) pour cet espace, ou {@code null} si indisponible.
     * Défaillance non bloquante : toute erreur retourne {@code null} (fallback persona
     * générique + {@code personaVersion} null côté appelant).
     */
    public SpaceResponse getSpace(UUID spaceId, UUID ownerUserId) {
        try {
            ApiResponse<SpaceResponse> response = restClientBuilder.build()
                    .get()
                    .uri(spaceServiceUrl + "/api/v1/spaces/{id}", spaceId)
                    .header(UserContext.HEADER_USER_ID, ownerUserId.toString())
                    .header(UserContext.HEADER_REQUEST_ID, UUID.randomUUID().toString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<SpaceResponse>>() {});
            if (response == null || !response.success() || response.data() == null) {
                log.warn("Persona non récupérable pour l'espace {} (réponse {} de space-service)",
                        spaceId, response == null ? "vide" : "success=false");
                return null;
            }
            return response.data();
        } catch (Exception e) {
            log.warn("Appel à space-service échoué pour l'espace {} (persona générique utilisé) : {}",
                    spaceId, e.getMessage());
            return null;
        }
    }

    /**
     * @return le persona de l'assistant pour cet espace, ou {@code null} si indisponible.
     */
    public String getAssistantPersona(UUID spaceId, UUID ownerUserId) {
        SpaceResponse space = getSpace(spaceId, ownerUserId);
        return space == null ? null : space.assistantPersona();
    }

    /**
     * @return la version courante du persona pour cet espace, ou {@code null} si
     * indisponible (space-service injoignable, espace supprimé, ou version absente).
     * Non bloquant : l'appelant persiste alors {@code null} (traçabilité inconnue).
     */
    public Integer getPersonaVersion(UUID spaceId, UUID ownerUserId) {
        SpaceResponse space = getSpace(spaceId, ownerUserId);
        if (space == null) {
            return null;
        }
        if (space.personaVersion() == null) {
            log.warn("personaVersion absente pour l'espace {} (fallback null, traçabilité inconnue)", spaceId);
        }
        return space.personaVersion();
    }
}
