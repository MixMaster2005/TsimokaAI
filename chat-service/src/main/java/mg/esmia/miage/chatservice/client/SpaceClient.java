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
     * @return le persona de l'assistant pour cet espace, ou {@code null} si indisponible.
     */
    public String getAssistantPersona(UUID spaceId, UUID ownerUserId) {
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
            return response.data().assistantPersona();
        } catch (Exception e) {
            log.warn("Appel à space-service échoué pour l'espace {} (persona générique utilisé) : {}",
                    spaceId, e.getMessage());
            return null;
        }
    }
}
