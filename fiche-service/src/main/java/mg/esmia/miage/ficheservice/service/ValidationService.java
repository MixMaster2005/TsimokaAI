package mg.esmia.miage.ficheservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.events.FicheEvent;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.common.exception.BadRequestException;
import mg.esmia.miage.ficheservice.dto.ValidateFicheRequest;
import mg.esmia.miage.ficheservice.dto.ValidationResponse;
import mg.esmia.miage.ficheservice.entity.Fiche;
import mg.esmia.miage.ficheservice.entity.ValidationFiche;
import mg.esmia.miage.ficheservice.repository.FicheRepository;
import mg.esmia.miage.ficheservice.repository.ValidationFicheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
        if (request.statut() == ValidationFiche.Statut.REJETEE && (request.commentaire() == null || request.commentaire().isBlank())) {
            throw new BadRequestException("Commentaire obligatoire pour un rejet");
        }
        ValidationFiche validation = validationFicheRepository.findByFicheId(ficheId)
                .orElseGet(() -> ValidationFiche.builder().ficheId(ficheId).build());

        validation.setEnseignantId(enseignantId);
        validation.setStatut(request.statut());
        validation.setCommentaire(request.commentaire());
        validation.setValidatedAt(Instant.now());
        validation = validationFicheRepository.save(validation);

        // Consommé par analytics-service (progression, onFicheValidated enrichi en
        // userId/spaceId) et gamification-service (badge PREMIERE_FICHE_VALIDEE imputé
        // à userId, pas enseignantId). Champs userId/spaceId renseignés depuis la
        // fiche (record inchangé : nouveaux champs déjà nullables, remplissage
        // rétrocompatible ; ancienne signature validated() 3-args conservée
        // @Deprecated côté common pour les producteurs historiques).
        // Publish-after-commit : l'envoi Redis est différé après le commit (sinon un
        // rollback DB laisserait un événement fantôme). Sans transaction active
        // (tests unitaires), envoi immédiat.
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
        FicheEvent event = FicheEvent.validated(ficheId.toString(), spaceId, userId,
                enseignantId.toString(), request.statut().name());
        publishAfterCommit(EventChannels.FICHE_EVENTS, event);

        return ValidationResponse.from(validation);
    }

    public ValidationResponse getByFiche(UUID ficheId) {
        ValidationFiche validation = validationFicheRepository.findByFicheId(ficheId)
                .orElse(ValidationFiche.builder().ficheId(ficheId).statut(ValidationFiche.Statut.EN_ATTENTE).build());
        return ValidationResponse.from(validation);
    }

    private void publishAfterCommit(String channel, Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publish(channel, event);
                }
            });
        } else {
            eventPublisher.publish(channel, event);
        }
    }
}
