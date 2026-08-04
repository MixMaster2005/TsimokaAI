package mg.esmia.miage.spaceservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.IngestionEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.spaceservice.service.SpaceService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consomme "ingestion.events". Sur DOCUMENT_READY, déclenche l'enrichissement du
 * persona pédagogique de l'espace concerné. Idempotent : appeler deux fois avec le
 * même document_id ne fait que ré-exécuter le même enrichissement (pas d'effet cumulatif
 * problématique côté PersonaService tel que conçu).
 */
@Component
@Slf4j
public class IngestionEventListener extends AbstractRedisEventListener<IngestionEvent> {

    private final SpaceService spaceService;

    public IngestionEventListener(ObjectMapper objectMapper, SpaceService spaceService) {
        super(objectMapper, IngestionEvent.class);
        this.spaceService = spaceService;
    }

    @Override
    protected void onEvent(IngestionEvent event) {
        if (IngestionEvent.DOCUMENT_READY.equals(event.event())) {
            log.info("DOCUMENT_READY reçu pour l'espace {} (document {})", event.spaceId(), event.documentId());
            spaceService.enrichPersonaAfterIngestion(
                    UUID.fromString(event.spaceId()), event.documentId(),
                    event.chunkCount() == null ? 0 : event.chunkCount());
        }
        // DOCUMENT_FAILED : rien à faire côté space-service pour l'instant.
    }
}
