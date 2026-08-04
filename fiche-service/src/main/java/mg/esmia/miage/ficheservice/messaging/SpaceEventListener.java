package mg.esmia.miage.ficheservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.SpaceEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.ficheservice.service.FicheService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class SpaceEventListener extends AbstractRedisEventListener<SpaceEvent> {

    private final FicheService ficheService;

    public SpaceEventListener(ObjectMapper objectMapper, FicheService ficheService) {
        super(objectMapper, SpaceEvent.class);
        this.ficheService = ficheService;
    }

    @Override
    protected void onEvent(SpaceEvent event) {
        if (SpaceEvent.SPACE_DELETED.equals(event.event())) {
            log.info("SPACE_DELETED reçu pour l'espace {} : suppression des fiches associées", event.spaceId());
            ficheService.deleteAllForSpace(UUID.fromString(event.spaceId()));
        }
    }
}
