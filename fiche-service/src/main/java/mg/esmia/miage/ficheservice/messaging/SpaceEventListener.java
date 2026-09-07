package mg.esmia.miage.ficheservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.SpaceEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.ficheservice.service.FicheService;
import mg.esmia.miage.ficheservice.service.QuizService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class SpaceEventListener extends AbstractRedisEventListener<SpaceEvent> {

    private final FicheService ficheService;
    private final QuizService quizService;

    public SpaceEventListener(ObjectMapper objectMapper, FicheService ficheService, QuizService quizService) {
        super(objectMapper, SpaceEvent.class);
        this.ficheService = ficheService;
        this.quizService = quizService;
    }

    @Override
    protected void onEvent(SpaceEvent event) {
        if (SpaceEvent.SPACE_DELETED.equals(event.event())) {
            UUID spaceId = UUID.fromString(event.spaceId());
            log.info("SPACE_DELETED reçu pour l'espace {} : suppression des fiches et quiz associés", event.spaceId());
            ficheService.deleteAllForSpace(spaceId);
            quizService.deleteAllForSpace(spaceId);
        }
    }
}
