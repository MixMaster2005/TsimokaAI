package mg.esmia.miage.ingestionservice.service.docker;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du conteneur docling-worker spawné à la demande (cf. DockerWorkerClient).
 * Préfixe : {@code docling-worker} dans application.yml.
 */
@Data
@ConfigurationProperties(prefix = "docling-worker")
public class DockerWorkerProperties {

    /** Socket Docker de l'hôte. Équivaut à un accès root sur l'hôte — voir docker-compose.yml. */
    private String dockerHost = "unix:///var/run/docker.sock";

    /** Image du worker, construite manuellement : docker build -t docling-worker:latest ./docling-worker. */
    private String image = "docling-worker:latest";

    /** Réseau Docker que le conteneur doit rejoindre (même réseau que ingestion-service, nommé "apa-net"). */
    private String network = "apa-net";

    /** Port HTTP exposé par le conteneur docling-worker. */
    private int containerPort = 8090;

    /** Délai max d'attente du GET /health après le démarrage du conteneur. */
    private int startupTimeoutSeconds = 30;

    /** Délai max d'un POST /v1/convert (compté large : légende/transcription Gemini incluses). */
    private int conversionTimeoutSeconds = 300;

    /** Clé API Gemini (vision, OpenAI-compatible) injectée au conteneur via l'env GEMINI_API_KEY. */
    private String geminiApiKey = "";

    /** Modèle Gemini vision injecté au conteneur via l'env GEMINI_MODEL. */
    private String geminiModel = "";

    /**
     * Nombre maximum de conteneurs docling-worker simultanés (sémaphore).
     * Doit être calibré sur la RAM et le CPU disponibles de l'hôte.
     */
    private int maxContainers = 4;
}
