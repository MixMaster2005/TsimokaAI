package mg.esmia.miage.ingestionservice.service.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.HostConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.exception.ApiException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.UUID;

/**
 * Orchestration du conteneur docling-worker, spawné à la demande (jamais un service
 * permanent de docker-compose.yml) :
 * <ol>
 *   <li>création d'un conteneur au nom unique {@code docling-worker-<uuid>} sur le réseau
 *       Docker configuré (par défaut {@code apa-net}, le réseau de ingestion-service) ;</li>
 *   <li>démarrage puis attente de disponibilité via {@code GET /health} (timeout configurable) ;</li>
 *   <li>{@code POST /v1/convert} (multipart) via WebClient ;</li>
 *   <li>arrêt + suppression du conteneur <b>toujours</b>, même en cas d'erreur (finally).</li>
 * </ol>
 *
 * Le conteneur étant joint par son nom Docker (DNS du réseau nommé), l'URL d'accès est
 * {@code http://docling-worker-<uuid>:<port>} — atteignable uniquement depuis un conteneur
 * du même réseau (ici ingestion-service).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DockerWorkerClient {

    public static final String ERROR_CODE = "DOCLING_WORKER_ERROR";

    private static final String CONTAINER_NAME_PREFIX = "docling-worker-";

    private final DockerClient dockerClient;
    private final DockerWorkerProperties properties;
    private final WebClient.Builder webClientBuilder;

    /**
     * Convertit un fichier (binaire) via docling-worker.
     *
     * @param fileContent contenu binaire du document
     * @param filename    nom du fichier (extension utilisée par le worker pour choisir le convertisseur)
     * @return le Markdown extrait et ses métadonnées
     * @throws ApiException si le conteneur ne démarre pas, n'est pas prêt à temps ou échoue la conversion
     */
    public DoclingConversionResult convert(byte[] fileContent, String filename) {
        String containerName = CONTAINER_NAME_PREFIX + UUID.randomUUID();
        String containerId = null;
        try {
            containerId = startContainer(containerName);
            String baseUrl = "http://%s:%d".formatted(containerName, properties.getContainerPort());
            waitUntilHealthy(baseUrl);
            return convertFile(baseUrl, fileContent, filename);
        } finally {
            stopAndRemoveContainer(containerId);
        }
    }

    private String startContainer(String containerName) {
        try {
            CreateContainerResponse response = dockerClient.createContainerCmd(properties.getImage())
                    .withName(containerName)
                    .withHostConfig(HostConfig.newHostConfig().withNetworkMode(properties.getNetwork()))
                    .exec();
            String containerId = response.getId();
            dockerClient.startContainerCmd(containerId).exec();
            log.info("docling-worker démarré : {} ({})", containerName, containerId);
            return containerId;
        } catch (DockerException e) {
            throw new ApiException(ERROR_CODE, "Impossible de démarrer le conteneur docling-worker "
                    + "(image '" + properties.getImage() + "'). Vérifiez qu'elle est construite : "
                    + "docker build -t docling-worker:latest ./docling-worker", 500);
        }
    }

    private void waitUntilHealthy(String baseUrl) {
        long deadline = System.currentTimeMillis()
                + (long) properties.getStartupTimeoutSeconds() * 1_000;
        WebClient client = buildWebClient(baseUrl);
        while (System.currentTimeMillis() < deadline) {
            try {
                HealthResponse health = client.get()
                        .uri("/health")
                        .retrieve()
                        .bodyToMono(HealthResponse.class)
                        .block(Duration.ofSeconds(2));
                if (health != null && "ok".equals(health.status())) {
                    return;
                }
            } catch (Exception e) {
                // uvicorn pas encore prêt : on réessaie jusqu'au timeout.
            }
            sleep(500);
        }
        throw new ApiException(ERROR_CODE, "docling-worker non disponible après "
                + properties.getStartupTimeoutSeconds() + "s (" + baseUrl + ")", 500);
    }

    private DoclingConversionResult convertFile(String baseUrl, byte[] fileContent, String filename) {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("file", new ByteArrayResource(fileContent) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        try {
            return buildWebClient(baseUrl)
                    .post()
                    .uri("/v1/convert")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(formData))
                    .retrieve()
                    .bodyToMono(DoclingConversionResult.class)
                    .timeout(Duration.ofSeconds(properties.getConversionTimeoutSeconds()))
                    .block();
        } catch (Exception e) {
            log.error("Échec de la conversion docling-worker pour {}", filename, e);
            throw new ApiException(ERROR_CODE, "Échec de la conversion par docling-worker : "
                    + e.getMessage(), 500);
        }
    }

    private void stopAndRemoveContainer(String containerId) {
        if (containerId == null) {
            return;
        }
        try {
            dockerClient.stopContainerCmd(containerId).exec();
        } catch (Exception e) {
            log.warn("Échec du stop du conteneur docling-worker {}", containerId, e);
        }
        try {
            dockerClient.removeContainerCmd(containerId).exec();
        } catch (Exception e) {
            log.warn("Échec de la suppression du conteneur docling-worker {}", containerId, e);
        }
    }

    private WebClient buildWebClient(String baseUrl) {
        // clone() : ne pas muter le builder partagé de Spring Boot.
        return webClientBuilder.clone().baseUrl(baseUrl).build();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ERROR_CODE, "Interruption pendant l'attente de docling-worker", 500);
        }
    }

    /** Réponse minimale de GET /health. */
    private record HealthResponse(String status) {
    }
}
