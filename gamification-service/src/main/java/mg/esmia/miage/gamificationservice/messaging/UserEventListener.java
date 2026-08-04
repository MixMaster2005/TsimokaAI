package mg.esmia.miage.gamificationservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.UserEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.gamificationservice.service.GamificationService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class UserEventListener extends AbstractRedisEventListener<UserEvent> {

    private final GamificationService gamificationService;

    public UserEventListener(ObjectMapper objectMapper, GamificationService gamificationService) {
        super(objectMapper, UserEvent.class);
        this.gamificationService = gamificationService;
    }

    @Override
    protected void onEvent(UserEvent event) {
        if (UserEvent.USER_DELETED.equals(event.event())) {
            gamificationService.deleteAllForUser(UUID.fromString(event.userId()));
        }
    }
}
