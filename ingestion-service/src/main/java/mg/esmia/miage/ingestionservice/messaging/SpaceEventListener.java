package mg.esmia.miage.ingestionservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.SpaceEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.ingestionservice.service.DocumentService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class SpaceEventListener extends AbstractRedisEventListener<SpaceEvent> {

    private final DocumentService documentService;

    public SpaceEventListener(ObjectMapper objectMapper, DocumentService documentService) {
        super(objectMapper, SpaceEvent.class);
        this.documentService = documentService;
    }

    @Override
    protected void onEvent(SpaceEvent event) {
        if (SpaceEvent.SPACE_DELETED.equals(event.event())) {
            log.info("SPACE_DELETED reçu pour l'espace {} : suppression des documents associés", event.spaceId());
            documentService.deleteAllForSpace(UUID.fromString(event.spaceId()));
        }
    }
}
