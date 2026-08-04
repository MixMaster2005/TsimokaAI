package mg.esmia.miage.ingestionservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.UserEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.ingestionservice.service.DocumentService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class UserEventListener extends AbstractRedisEventListener<UserEvent> {

    private final DocumentService documentService;

    public UserEventListener(ObjectMapper objectMapper, DocumentService documentService) {
        super(objectMapper, UserEvent.class);
        this.documentService = documentService;
    }

    @Override
    protected void onEvent(UserEvent event) {
        if (UserEvent.USER_DELETED.equals(event.event())) {
            log.info("USER_DELETED reçu pour l'utilisateur {} : suppression des documents associés", event.userId());
            documentService.deleteAllForUser(UUID.fromString(event.userId()));
        }
    }
}
