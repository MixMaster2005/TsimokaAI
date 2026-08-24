package mg.esmia.miage.ficheservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.events.FicheEvent;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.ficheservice.dto.FicheResponse;
import mg.esmia.miage.ficheservice.dto.GenerateFicheRequest;
import mg.esmia.miage.ficheservice.entity.Fiche;
import mg.esmia.miage.ficheservice.repository.AnnotationRepository;
import mg.esmia.miage.ficheservice.repository.FicheRepository;
import mg.esmia.miage.ficheservice.repository.PartageFicheRepository;
import mg.esmia.miage.ficheservice.repository.ValidationFicheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FicheService {

    private final FicheRepository ficheRepository;
    private final PartageFicheRepository partageFicheRepository;
    private final AnnotationRepository annotationRepository;
    private final ValidationFicheRepository validationFicheRepository;
    private final FicheGenerationService generationService;
    private final RedisEventPublisher eventPublisher;

    @Transactional
    public FicheResponse generate(UUID userId, GenerateFicheRequest request) {
        List<UUID> documentIds = request.documentIds() == null ? List.of() : request.documentIds();
        String contentJson = generationService.generateContentJson(request.spaceId(), documentIds);

        Fiche fiche = Fiche.builder()
                .spaceId(request.spaceId())
                .userId(userId)
                .title(request.title() == null || request.title().isBlank() ? "Fiche de révision" : request.title())
                .sourceDocumentIds(documentIds.toArray(new UUID[0]))
                .contentJson(contentJson)
                .generatedAt(Instant.now())
                .obsolete(false)
                .build();
        fiche = ficheRepository.save(fiche);

        eventPublisher.publish(EventChannels.FICHE_EVENTS,
                FicheEvent.generated(fiche.getId().toString(), fiche.getSpaceId().toString(), userId.toString()));

        return FicheResponse.from(fiche);
    }

    public FicheResponse getById(UUID id, UUID requesterId, boolean isAdmin) {
        Fiche fiche = findOrThrow(id);
        assertOwnerOrAdmin(fiche, requesterId, isAdmin);
        return FicheResponse.from(fiche);
    }

    public List<FicheResponse> listMine(UUID spaceId, UUID userId) {
        return ficheRepository.findBySpaceIdAndUserId(spaceId, userId).stream().map(FicheResponse::from).toList();
    }

    /** Vue transverse "Mes fiches" : toutes les fiches de l'utilisateur, tous espaces confondus. */
    public List<FicheResponse> listAllMine(UUID userId) {
        return ficheRepository.findByUserIdOrderByGeneratedAtDesc(userId).stream().map(FicheResponse::from).toList();
    }

    /**
     * Toutes les fiches d'un espace — réservé aux admins (enseignants) : c'est la file
     * de travail qui leur permet de découvrir les fiches à valider.
     */
    public List<FicheResponse> listForSpace(UUID spaceId, boolean isAdmin) {
        if (!isAdmin) {
            throw new ForbiddenException("Réservé aux enseignants");
        }
        return ficheRepository.findBySpaceId(spaceId).stream().map(FicheResponse::from).toList();
    }

    @Transactional
    public void delete(UUID id, UUID requesterId, boolean isAdmin) {
        Fiche fiche = findOrThrow(id);
        assertOwnerOrAdmin(fiche, requesterId, isAdmin);
        partageFicheRepository.deleteByFicheId(id);
        annotationRepository.deleteByFicheId(id);
        validationFicheRepository.deleteByFicheId(id);
        ficheRepository.delete(fiche);
    }

    /** Marque comme obsolète toute fiche générée à partir d'un document ré-ingéré. */
    @Transactional
    public void markObsoleteForSpace(UUID spaceId) {
        ficheRepository.findBySpaceId(spaceId).forEach(f -> {
            f.setObsolete(true);
            ficheRepository.save(f);
        });
    }

    @Transactional
    public void deleteAllForSpace(UUID spaceId) {
        ficheRepository.findBySpaceId(spaceId).forEach(f -> {
            partageFicheRepository.deleteByFicheId(f.getId());
            annotationRepository.deleteByFicheId(f.getId());
            validationFicheRepository.deleteByFicheId(f.getId());
        });
        ficheRepository.deleteBySpaceId(spaceId);
    }

    @Transactional
    public void deleteAllForUser(UUID userId) {
        ficheRepository.deleteByUserId(userId);
    }

    Fiche findOrThrow(UUID id) {
        return ficheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche introuvable : " + id));
    }

    void assertOwnerOrAdmin(Fiche fiche, UUID requesterId, boolean isAdmin) {
        if (!isAdmin && !fiche.getUserId().equals(requesterId)) {
            throw new ForbiddenException("Accès refusé à cette fiche");
        }
    }
}
