package mg.esmia.miage.analyticsservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.SpaceEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.analyticsservice.service.AnalyticsService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class SpaceEventListener extends AbstractRedisEventListener<SpaceEvent> {

    private final AnalyticsService analyticsService;

    public SpaceEventListener(ObjectMapper objectMapper, AnalyticsService analyticsService) {
        super(objectMapper, SpaceEvent.class);
        this.analyticsService = analyticsService;
    }

    @Override
    protected void onEvent(SpaceEvent event) {
        if (SpaceEvent.SPACE_DELETED.equals(event.event())) {
            analyticsService.deleteAllForSpace(UUID.fromString(event.spaceId()));
        }
    }
}
