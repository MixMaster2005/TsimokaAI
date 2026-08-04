package mg.esmia.miage.ficheservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.IngestionEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.ficheservice.service.FicheService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consomme "ingestion.events". Sur DOCUMENT_READY, marque comme obsolètes les fiches
 * déjà générées pour cet espace (CDC §4.4 : "signalement automatique de l'obsolescence
 * si un nouveau document est ingéré après la génération").
 */
@Component
@Slf4j
public class IngestionEventListener extends AbstractRedisEventListener<IngestionEvent> {

    private final FicheService ficheService;

    public IngestionEventListener(ObjectMapper objectMapper, FicheService ficheService) {
        super(objectMapper, IngestionEvent.class);
        this.ficheService = ficheService;
    }

    @Override
    protected void onEvent(IngestionEvent event) {
        if (IngestionEvent.DOCUMENT_READY.equals(event.event())) {
            log.info("DOCUMENT_READY reçu pour l'espace {} : marquage des fiches existantes comme obsolètes", event.spaceId());
            ficheService.markObsoleteForSpace(UUID.fromString(event.spaceId()));
        }
    }
}
