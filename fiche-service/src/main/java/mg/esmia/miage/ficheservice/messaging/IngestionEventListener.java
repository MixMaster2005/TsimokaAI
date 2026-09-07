package mg.esmia.miage.ficheservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.IngestionEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.ficheservice.service.FicheMapCacheService;
import mg.esmia.miage.ficheservice.service.FicheService;
import mg.esmia.miage.ficheservice.service.QuizService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consomme "ingestion.events". Sur DOCUMENT_READY, marque comme obsolètes les fiches
 * et quiz déjà générés pour cet espace, et invalide le cache MAP.
 */
@Component
@Slf4j
public class IngestionEventListener extends AbstractRedisEventListener<IngestionEvent> {

    private final FicheService ficheService;
    private final QuizService quizService;
    private final FicheMapCacheService mapCache;

    public IngestionEventListener(ObjectMapper objectMapper, FicheService ficheService,
                                  QuizService quizService, FicheMapCacheService mapCache) {
        super(objectMapper, IngestionEvent.class);
        this.ficheService = ficheService;
        this.quizService = quizService;
        this.mapCache = mapCache;
    }

    @Override
    protected void onEvent(IngestionEvent event) {
        if (IngestionEvent.DOCUMENT_READY.equals(event.event())) {
            UUID spaceId = UUID.fromString(event.spaceId());
            log.info("DOCUMENT_READY reçu pour l'espace {} : marquage des fiches et quiz comme obsolètes", event.spaceId());
            ficheService.markObsoleteForSpace(spaceId);
            quizService.markObsoleteForSpace(spaceId);
            mapCache.invalidateSpace(spaceId);
        }
    }
}
