package mg.esmia.miage.spaceservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.UserEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.spaceservice.service.SpaceService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consomme "user.events". Sur USER_DELETED, supprime tous les espaces (et groupes)
 * appartenant à l'utilisateur. Suppression en cascade PAR EVENEMENT, jamais par
 * appel synchrone (contrat de données §5.2).
 */
@Component
@Slf4j
public class UserEventListener extends AbstractRedisEventListener<UserEvent> {

    private final SpaceService spaceService;

    public UserEventListener(ObjectMapper objectMapper, SpaceService spaceService) {
        super(objectMapper, UserEvent.class);
        this.spaceService = spaceService;
    }

    @Override
    protected void onEvent(UserEvent event) {
        if (UserEvent.USER_DELETED.equals(event.event())) {
            log.info("USER_DELETED reçu pour l'utilisateur {} : suppression des espaces associés", event.userId());
            spaceService.deleteAllForUser(UUID.fromString(event.userId()));
        }
    }
}
