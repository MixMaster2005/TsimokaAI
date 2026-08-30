package mg.esmia.miage.chatservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.chatservice.client.dto.DocumentResponse;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;

/**
 * Client REST service-à-service vers {@code ingestion-service} pour résoudre le nom
 * de fichier lisible d'un document source de citation (GET /api/v1/documents/{id}).
 *
 * <p>Même contrat que {@link SpaceClient} : en appel interne on reproduit les headers
 * {@code X-User-Id} injectés habituellement par la gateway, avec l'identité de
 * l'utilisateur de la conversation — ingestion-service n'authentifie pas de nouveau.
 *
 * <p>Défaillance non bloquante : en cas d'erreur (service injoignable, document supprimé,
 * ou document appartenant à un autre membre de l'espace), la méthode retourne {@code null}
 * et la citation est affichée sans nom de document — jamais de blocage du chat pour ça.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IngestionClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${ingestion-service.url:http://localhost:8083}")
    private String ingestionServiceUrl;

    /**
     * @return le nom de fichier du document, ou {@code null} si non résolvable.
     */
    public String getDocumentName(UUID documentId, UUID requesterUserId) {
        try {
            ApiResponse<DocumentResponse> response = restClientBuilder.build()
                    .get()
                    .uri(ingestionServiceUrl + "/api/v1/documents/{id}", documentId)
                    .header(UserContext.HEADER_USER_ID, requesterUserId.toString())
                    .header(UserContext.HEADER_REQUEST_ID, UUID.randomUUID().toString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<DocumentResponse>>() {});
            if (response == null || !response.success() || response.data() == null) {
                log.warn("Nom de document non récupérable pour {} (réponse {} d'ingestion-service)",
                        documentId, response == null ? "vide" : "success=false");
                return null;
            }
            return response.data().filename();
        } catch (Exception e) {
            log.warn("Appel à ingestion-service échoué pour le document {} (citation sans nom) : {}",
                    documentId, e.getMessage());
            return null;
        }
    }

    public record ResolvedImage(String url, String caption) {}

    private record ResolveRequest(Set<String> imageIds) {}
    private record ResolveResponse(Map<String, ResolvedImage> images) {}

    /**
     * Résout batch un ensemble d'image_ids (metadata Qdrant) en URLs + captions
     * via ingestion-service (POST /api/v1/documents/images/resolve).
     *
     * @return Map imageId → (url, caption), ou Map vide en cas d'erreur.
     */
    public Map<String, ResolvedImage> resolveImageIds(Set<String> imageIds, UUID requesterUserId) {
        if (imageIds == null || imageIds.isEmpty()) {
            return Map.of();
        }
        try {
            ResolveResponse response = restClientBuilder.build()
                    .post()
                    .uri(ingestionServiceUrl + "/api/v1/documents/images/resolve")
                    .header(UserContext.HEADER_USER_ID, requesterUserId.toString())
                    .header(UserContext.HEADER_REQUEST_ID, UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ResolveRequest(imageIds))
                    .retrieve()
                    .body(ResolveResponse.class);
            if (response == null || response.images() == null) {
                return Map.of();
            }
            return response.images();
        } catch (Exception e) {
            log.warn("Résolution image_ids échouée ({} ids) : {}", imageIds.size(), e.getMessage());
            return Map.of();
        }
    }
}
