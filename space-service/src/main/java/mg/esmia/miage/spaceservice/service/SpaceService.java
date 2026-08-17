package mg.esmia.miage.spaceservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.events.SpaceEvent;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.spaceservice.dto.CreateSpaceRequest;
import mg.esmia.miage.spaceservice.dto.SpaceResponse;
import mg.esmia.miage.spaceservice.dto.UpdateSpaceRequest;
import mg.esmia.miage.spaceservice.entity.Space;
import mg.esmia.miage.spaceservice.repository.GroupeRepository;
import mg.esmia.miage.spaceservice.repository.SpaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final GroupeRepository groupeRepository;
    private final PersonaService personaService;
    private final RedisEventPublisher eventPublisher;

    @Transactional
    public SpaceResponse create(UUID userId, CreateSpaceRequest request) {
        String persona = personaService.generateInitialPersona(request.name(), request.subjectTag(), request.description());
        Space space = Space.builder()
                .userId(userId)
                .name(request.name())
                .description(request.description())
                .subjectTag(request.subjectTag())
                .assistantPersona(persona)
                .build();
        return SpaceResponse.from(spaceRepository.save(space));
    }

    public SpaceResponse getById(UUID id, UUID requesterId, boolean isAdmin) {
        Space space = findOrThrow(id);
        assertOwnerOrAdmin(space, requesterId, isAdmin);
        return SpaceResponse.from(space);
    }

    public List<SpaceResponse> listMine(UUID userId) {
        return spaceRepository.findByUserId(userId).stream().map(SpaceResponse::from).toList();
    }

    @Transactional
    public SpaceResponse update(UUID id, UUID requesterId, boolean isAdmin, UpdateSpaceRequest request) {
        Space space = findOrThrow(id);
        assertOwnerOrAdmin(space, requesterId, isAdmin);
        space.setName(request.name());
        space.setDescription(request.description());
        space.setSubjectTag(request.subjectTag());
        return SpaceResponse.from(spaceRepository.save(space));
    }

    @Transactional
    public void delete(UUID id, UUID requesterId, boolean isAdmin) {
        Space space = findOrThrow(id);
        assertOwnerOrAdmin(space, requesterId, isAdmin);
        groupeRepository.deleteBySpaceId(id);
        spaceRepository.delete(space);
        // Nettoyage en cascade des autres services (ingestion, chat, fiche, analytics)
        // exclusivement par événement Redis, jamais par appel synchrone direct.
        eventPublisher.publish(EventChannels.SPACE_EVENTS, SpaceEvent.deleted(id.toString(), requesterId.toString()));
    }

    /** Appelé par IngestionEventListener suite à un DOCUMENT_READY. */
    @Transactional
    public void enrichPersonaAfterIngestion(UUID spaceId, String documentId, int chunkCount) {
        spaceRepository.findById(spaceId).ifPresent(space -> {
            String enriched = personaService.enrichPersonaAfterIngestion(
                    space.getAssistantPersona(), spaceId, documentId, chunkCount);
            space.setAssistantPersona(enriched);
            spaceRepository.save(space);
        });
    }

    /** Appelé par UserEventListener suite à un USER_DELETED. */
    @Transactional
    public void deleteAllForUser(UUID userId) {
        spaceRepository.findByUserId(userId).forEach(space -> groupeRepository.deleteBySpaceId(space.getId()));
        spaceRepository.deleteByUserId(userId);
    }

    private Space findOrThrow(UUID id) {
        return spaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Espace introuvable : " + id));
    }

    private void assertOwnerOrAdmin(Space space, UUID requesterId, boolean isAdmin) {
        if (!isAdmin && !space.getUserId().equals(requesterId)) {
            throw new ForbiddenException("Accès refusé à cet espace de cours");
        }
    }
}
