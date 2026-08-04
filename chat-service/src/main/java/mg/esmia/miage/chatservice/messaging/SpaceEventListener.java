package mg.esmia.miage.chatservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.SpaceEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.chatservice.service.ConversationService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class SpaceEventListener extends AbstractRedisEventListener<SpaceEvent> {

    private final ConversationService conversationService;

    public SpaceEventListener(ObjectMapper objectMapper, ConversationService conversationService) {
        super(objectMapper, SpaceEvent.class);
        this.conversationService = conversationService;
    }

    @Override
    protected void onEvent(SpaceEvent event) {
        if (SpaceEvent.SPACE_DELETED.equals(event.event())) {
            log.info("SPACE_DELETED reçu pour l'espace {} : suppression des conversations associées", event.spaceId());
            conversationService.deleteAllForSpace(UUID.fromString(event.spaceId()));
        }
    }
}
