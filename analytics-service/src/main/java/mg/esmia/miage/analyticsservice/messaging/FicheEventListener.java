package mg.esmia.miage.analyticsservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.FicheEvent;
import mg.esmia.miage.common.messaging.AbstractRedisEventListener;
import mg.esmia.miage.analyticsservice.service.AnalyticsService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class FicheEventListener extends AbstractRedisEventListener<FicheEvent> {

    private final AnalyticsService analyticsService;

    public FicheEventListener(ObjectMapper objectMapper, AnalyticsService analyticsService) {
        super(objectMapper, FicheEvent.class);
        this.analyticsService = analyticsService;
    }

    @Override
    protected void onEvent(FicheEvent event) {
        if (FicheEvent.FICHE_GENERATED.equals(event.event())) {
            analyticsService.onFicheGenerated(UUID.fromString(event.userId()), UUID.fromString(event.spaceId()));
        } else if (FicheEvent.FICHE_VALIDATED.equals(event.event())) {
            analyticsService.onFicheValidated(event.statut());
        }
    }
}
