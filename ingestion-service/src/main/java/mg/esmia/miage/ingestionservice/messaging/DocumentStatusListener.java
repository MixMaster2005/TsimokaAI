package mg.esmia.miage.ingestionservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.IngestionEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.ingestionservice.config.SseEmittersRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Consommateur Redis du canal {@code ingestion.events}.
 * Reçoit les événements DOCUMENT_PROCESSING / DOCUMENT_READY / DOCUMENT_FAILED
 * et les push aux clients SSE connectés via {@link SseEmittersRegistry}.
 */
@Component
@Slf4j
public class DocumentStatusListener extends AbstractRedisEventListener<IngestionEvent> {

    private final SseEmittersRegistry registry;

    public DocumentStatusListener(ObjectMapper objectMapper, SseEmittersRegistry registry) {
        super(objectMapper, IngestionEvent.class);
        this.registry = registry;
    }

    @Override
    protected void onEvent(IngestionEvent event) {
        UUID spaceId = UUID.fromString(event.spaceId());

        Map<String, Object> payload = Map.of(
                "documentId", event.documentId(),
                "status", event.event(),
                "chunkCount", event.chunkCount() != null ? event.chunkCount() : 0,
                "reason", event.reason() != null ? event.reason() : "",
                "timestamp", event.timestamp().toString()
        );

        log.debug("Push SSE document_status vers space {}: {}", spaceId, event.event());
        registry.notify(spaceId, payload);
    }
}
