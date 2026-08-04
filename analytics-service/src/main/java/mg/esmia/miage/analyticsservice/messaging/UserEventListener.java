package mg.esmia.miage.analyticsservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.UserEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.analyticsservice.service.AnalyticsService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class UserEventListener extends AbstractRedisEventListener<UserEvent> {

    private final AnalyticsService analyticsService;

    public UserEventListener(ObjectMapper objectMapper, AnalyticsService analyticsService) {
        super(objectMapper, UserEvent.class);
        this.analyticsService = analyticsService;
    }

    @Override
    protected void onEvent(UserEvent event) {
        if (UserEvent.USER_DELETED.equals(event.event())) {
            analyticsService.deleteAllForUser(UUID.fromString(event.userId()));
        }
    }
}
