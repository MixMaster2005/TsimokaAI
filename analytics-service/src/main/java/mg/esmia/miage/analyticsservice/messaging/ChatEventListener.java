package mg.esmia.miage.analyticsservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.ChatEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.analyticsservice.service.AnalyticsService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class ChatEventListener extends AbstractRedisEventListener<ChatEvent> {

    private final AnalyticsService analyticsService;

    public ChatEventListener(ObjectMapper objectMapper, AnalyticsService analyticsService) {
        super(objectMapper, ChatEvent.class);
        this.analyticsService = analyticsService;
    }

    @Override
    protected void onEvent(ChatEvent event) {
        if (ChatEvent.MESSAGE_CREATED.equals(event.event()) && "USER".equalsIgnoreCase(event.role())) {
            analyticsService.onQuestionAsked(UUID.fromString(event.userId()), UUID.fromString(event.spaceId()), event.content());
        }
    }
}
