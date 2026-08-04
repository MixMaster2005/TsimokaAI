package mg.esmia.miage.ficheservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.events.FicheEvent;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.ficheservice.dto.ValidateFicheRequest;
import mg.esmia.miage.ficheservice.entity.ValidationFiche;
import mg.esmia.miage.ficheservice.repository.ValidationFicheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final ValidationFicheRepository validationFicheRepository;
    private final RedisEventPublisher eventPublisher;

    @Transactional
    public ValidationFiche validate(UUID ficheId, UUID enseignantId, ValidateFicheRequest request) {
        ValidationFiche validation = validationFicheRepository.findByFicheId(ficheId)
                .orElseGet(() -> ValidationFiche.builder().ficheId(ficheId).build());

        validation.setEnseignantId(enseignantId);
        validation.setStatut(request.statut());
        validation.setCommentaire(request.commentaire());
        validation.setValidatedAt(Instant.now());
        validation = validationFicheRepository.save(validation);

        // Consommé par analytics-service (progression) et gamification-service (badges).
        eventPublisher.publish(EventChannels.FICHE_EVENTS,
                FicheEvent.validated(ficheId.toString(), enseignantId.toString(), request.statut().name()));

        return validation;
    }

    public ValidationFiche getByFiche(UUID ficheId) {
        return validationFicheRepository.findByFicheId(ficheId)
                .orElse(ValidationFiche.builder().ficheId(ficheId).statut(ValidationFiche.Statut.EN_ATTENTE).build());
    }
}
