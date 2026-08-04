package mg.esmia.miage.gamificationservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.gamificationservice.dto.CreateObjectifRequest;
import mg.esmia.miage.gamificationservice.dto.ObjectifResponse;
import mg.esmia.miage.gamificationservice.dto.UpdateObjectifRequest;
import mg.esmia.miage.gamificationservice.entity.ObjectifRevision;
import mg.esmia.miage.gamificationservice.repository.ObjectifRevisionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObjectifService {

    private final ObjectifRevisionRepository objectifRepository;
    private final GamificationService gamificationService;

    @Transactional
    public ObjectifResponse create(UUID userId, CreateObjectifRequest request) {
        ObjectifRevision objectif = ObjectifRevision.builder()
                .userId(userId)
                .spaceId(request.spaceId())
                .titre(request.titre())
                .description(request.description())
                .dateEcheance(request.dateEcheance())
                .build();
        return ObjectifResponse.from(objectifRepository.save(objectif));
    }

    public List<ObjectifResponse> listMine(UUID userId, UUID spaceId) {
        return objectifRepository.findByUserIdAndSpaceId(userId, spaceId).stream().map(ObjectifResponse::from).toList();
    }

    @Transactional
    public ObjectifResponse updateStatut(UUID id, UUID requesterId, UpdateObjectifRequest request) {
        ObjectifRevision objectif = objectifRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Objectif introuvable : " + id));
        if (!objectif.getUserId().equals(requesterId)) {
            throw new ForbiddenException("Accès refusé à cet objectif");
        }
        objectif.setStatut(request.statut());
        objectif = objectifRepository.save(objectif);

        if (request.statut() == ObjectifRevision.Statut.ATTEINT) {
            gamificationService.onObjectifAtteint(requesterId, objectif.getSpaceId());
        }
        return ObjectifResponse.from(objectif);
    }
}
