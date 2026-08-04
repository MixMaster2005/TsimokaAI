package mg.esmia.miage.gamificationservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.SpaceEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.gamificationservice.service.GamificationService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class SpaceEventListener extends AbstractRedisEventListener<SpaceEvent> {

    private final GamificationService gamificationService;

    public SpaceEventListener(ObjectMapper objectMapper, GamificationService gamificationService) {
        super(objectMapper, SpaceEvent.class);
        this.gamificationService = gamificationService;
    }

    @Override
    protected void onEvent(SpaceEvent event) {
        if (SpaceEvent.SPACE_DELETED.equals(event.event())) {
            gamificationService.deleteAllForSpace(UUID.fromString(event.spaceId()));
        }
    }
}
