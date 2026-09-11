package mg.esmia.miage.ficheservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.events.FicheEvent;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.ficheservice.dto.ValidateFicheRequest;
import mg.esmia.miage.ficheservice.dto.ValidationResponse;
import mg.esmia.miage.ficheservice.entity.Fiche;
import mg.esmia.miage.ficheservice.entity.ValidationFiche;
import mg.esmia.miage.ficheservice.repository.FicheRepository;
import mg.esmia.miage.ficheservice.repository.ValidationFicheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationService {

    private final ValidationFicheRepository validationFicheRepository;
    private final FicheRepository ficheRepository;
    private final RedisEventPublisher eventPublisher;

    @Transactional
    public ValidationResponse validate(UUID ficheId, UUID enseignantId, ValidateFicheRequest request) {
        ValidationFiche validation = validationFicheRepository.findByFicheId(ficheId)
                .orElseGet(() -> ValidationFiche.builder().ficheId(ficheId).build());

        validation.setEnseignantId(enseignantId);
        validation.setStatut(request.statut());
        validation.setCommentaire(request.commentaire());
        validation.setValidatedAt(Instant.now());
        validation = validationFicheRepository.save(validation);

        // Consommé par analytics-service (progression, onFicheValidated enrichi en
        // userId/spaceId) et gamification-service (badges, enseignantId/statut).
        // Champs userId/spaceId renseignés depuis la fiche (record inchangé :
        // nouveaux champs déjà nullables, remplissage rétrocompatible).
        String userId = null;
        String spaceId = null;
        try {
            Fiche fiche = ficheRepository.findById(ficheId).orElse(null);
            if (fiche != null) {
                if (fiche.getUserId() != null) {
                    userId = fiche.getUserId().toString();
                }
                if (fiche.getSpaceId() != null) {
                    spaceId = fiche.getSpaceId().toString();
                }
            } else {
                log.warn("Fiche {} introuvable : FICHE_VALIDATED publié sans userId/spaceId", ficheId);
            }
        } catch (Exception e) {
            log.warn("Lecture fiche {} impossible : FICHE_VALIDATED publié sans userId/spaceId", ficheId, e);
        }
        eventPublisher.publish(EventChannels.FICHE_EVENTS,
                new FicheEvent(FicheEvent.FICHE_VALIDATED, ficheId.toString(), spaceId, userId,
                        enseignantId.toString(), request.statut().name(), Instant.now()));

        return ValidationResponse.from(validation);
    }

    public ValidationResponse getByFiche(UUID ficheId) {
        ValidationFiche validation = validationFicheRepository.findByFicheId(ficheId)
                .orElse(ValidationFiche.builder().ficheId(ficheId).statut(ValidationFiche.Statut.EN_ATTENTE).build());
        return ValidationResponse.from(validation);
    }
}
