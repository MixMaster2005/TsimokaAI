package mg.esmia.miage.chatservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.UserEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.chatservice.service.ConversationService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class UserEventListener extends AbstractRedisEventListener<UserEvent> {

    private final ConversationService conversationService;

    public UserEventListener(ObjectMapper objectMapper, ConversationService conversationService) {
        super(objectMapper, UserEvent.class);
        this.conversationService = conversationService;
    }

    @Override
    protected void onEvent(UserEvent event) {
        if (UserEvent.USER_DELETED.equals(event.event())) {
            log.info("USER_DELETED reçu pour l'utilisateur {} : suppression des conversations associées", event.userId());
            conversationService.deleteAllForUser(UUID.fromString(event.userId()));
        }
    }
}
