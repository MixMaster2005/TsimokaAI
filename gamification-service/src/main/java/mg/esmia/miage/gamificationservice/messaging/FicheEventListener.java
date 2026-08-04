package mg.esmia.miage.gamificationservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.FicheEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.gamificationservice.service.GamificationService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class FicheEventListener extends AbstractRedisEventListener<FicheEvent> {

    private final GamificationService gamificationService;

    public FicheEventListener(ObjectMapper objectMapper, GamificationService gamificationService) {
        super(objectMapper, FicheEvent.class);
        this.gamificationService = gamificationService;
    }

    @Override
    protected void onEvent(FicheEvent event) {
        if (FicheEvent.FICHE_GENERATED.equals(event.event())) {
            gamificationService.onFicheGenerated(UUID.fromString(event.userId()), UUID.fromString(event.spaceId()));
        } else if (FicheEvent.FICHE_VALIDATED.equals(event.event())) {
            gamificationService.onFicheValidated(UUID.fromString(event.enseignantId()), event.statut());
        }
    }
}
