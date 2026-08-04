package mg.esmia.miage.ficheservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.UserEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.ficheservice.service.FicheService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class UserEventListener extends AbstractRedisEventListener<UserEvent> {

    private final FicheService ficheService;

    public UserEventListener(ObjectMapper objectMapper, FicheService ficheService) {
        super(objectMapper, UserEvent.class);
        this.ficheService = ficheService;
    }

    @Override
    protected void onEvent(UserEvent event) {
        if (UserEvent.USER_DELETED.equals(event.event())) {
            log.info("USER_DELETED reçu pour l'utilisateur {} : suppression des fiches associées", event.userId());
            ficheService.deleteAllForUser(UUID.fromString(event.userId()));
        }
    }
}
