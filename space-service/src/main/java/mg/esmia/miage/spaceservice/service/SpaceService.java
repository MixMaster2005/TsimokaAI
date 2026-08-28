package mg.esmia.miage.spaceservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.events.SpaceEvent;
import mg.esmia.miage.common.exception.ConflictException;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.spaceservice.dto.CreateSpaceRequest;
import mg.esmia.miage.spaceservice.dto.InviteCodeResponse;
import mg.esmia.miage.spaceservice.dto.SpaceResponse;
import mg.esmia.miage.spaceservice.dto.UpdateSpaceRequest;
import mg.esmia.miage.spaceservice.entity.MembreSpace;
import mg.esmia.miage.spaceservice.entity.Space;
import mg.esmia.miage.spaceservice.repository.GroupeRepository;
import mg.esmia.miage.spaceservice.repository.MembreSpaceRepository;
import mg.esmia.miage.spaceservice.repository.SpaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpaceService {

    /**
     * Alphabet du code d'invitation : chiffres + lettres majuscules SANS les caractères
     * ambigus à la recopie manuelle (O/0, I/1/L). 8 caractères => ~30^8 combinaisons.
     */
    private static final String CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SpaceRepository spaceRepository;
    private final MembreSpaceRepository membreSpaceRepository;
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
                .inviteCode(generateUniqueCode())
                .build();
        return SpaceResponse.from(spaceRepository.save(space), userId);
    }

    public SpaceResponse getById(UUID id, UUID requesterId, boolean isAdmin) {
        Space space = findOrThrow(id);
        // Lecture ouverte aux MEMBRES (pas seulement au propriétaire) : space-service
        // est le seul point de la plateforme qui vérifie l'appartenance à un espace,
        // les autres services ne revalident pas (cf. README, Règles métier).
        assertCanView(space, requesterId, isAdmin);
        return SpaceResponse.from(space, requesterId);
    }

    /**
     * Espaces possédés + espaces rejoints via code d'invitation. Le propriétaire est
     * dédoublonné en tête (LinkedHashMap) pour garder un ordre stable dans l'étagère.
     */
    public List<SpaceResponse> listMine(UUID userId) {
        Map<UUID, SpaceResponse> result = new LinkedHashMap<>();
        spaceRepository.findByUserId(userId)
                .forEach(s -> result.put(s.getId(), SpaceResponse.from(s, userId)));
        // M3 : batch query au lieu de N× findById
        List<UUID> memberSpaceIds = membreSpaceRepository.findByUserId(userId).stream()
                .map(MembreSpace::getSpaceId)
                .filter(id -> !result.containsKey(id))
                .toList();
        if (!memberSpaceIds.isEmpty()) {
            spaceRepository.findByIdIn(memberSpaceIds)
                    .forEach(s -> result.put(s.getId(), SpaceResponse.from(s, userId)));
        }
        return new ArrayList<>(result.values());
    }

    /**
     * Tous les espaces de la plateforme — réservé aux admins (enseignants) : c'est
     * la vue de supervision qui permet de naviguer vers les fiches à valider.
     */
    public List<SpaceResponse> listAll(boolean isAdmin) {
        if (!isAdmin) {
            throw new ForbiddenException("Réservé aux enseignants");
        }
        // Pas de requesterId => owner=false partout (c'est une vue de supervision,
        // pas une liste d'espaces possédés).
        return spaceRepository.findAll().stream().map(SpaceResponse::from).toList();
    }

    /**
     * Rejoint un espace via son code d'invitation. Erreurs explicites :
     * code inconnu (404), propriétaire (409 — on n'est jamais membre de son propre espace),
     * déjà membre (409).
     */
    @Transactional
    public SpaceResponse joinByCode(UUID userId, String rawCode) {
        String code = rawCode == null ? "" : rawCode.strip().toUpperCase();
        if (code.isEmpty()) {
            throw new ResourceNotFoundException("Code d'invitation invalide");
        }
        Space space = spaceRepository.findByInviteCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun espace pour ce code d'invitation"));

        if (space.getUserId().equals(userId)) {
            throw new ConflictException("Vous êtes déjà le propriétaire de cet espace");
        }
        if (membreSpaceRepository.existsBySpaceIdAndUserId(space.getId(), userId)) {
            throw new ConflictException("Vous êtes déjà membre de cet espace");
        }
        membreSpaceRepository.save(MembreSpace.builder().spaceId(space.getId()).userId(userId).build());
        return SpaceResponse.from(space, userId);
    }

    /** Liste des membres (hors propriétaire) — visible par le propriétaire ET les membres. */
    public List<MembreSpace> listMembres(UUID id, UUID requesterId, boolean isAdmin) {
        Space space = findOrThrow(id);
        assertCanView(space, requesterId, isAdmin);
        return membreSpaceRepository.findBySpaceIdOrderByJoinedAtAsc(id);
    }

    /** Retrait d'un membre par le propriétaire (ou admin) ; le propriétaire lui-même est intouchable. */
    @Transactional
    public void removeMembre(UUID id, UUID memberId, UUID requesterId, boolean isAdmin) {
        Space space = findOrThrow(id);
        assertOwnerOrAdmin(space, requesterId, isAdmin);
        if (space.getUserId().equals(memberId)) {
            throw new ConflictException("Le propriétaire ne peut pas être retiré des membres");
        }
        if (!membreSpaceRepository.existsBySpaceIdAndUserId(id, memberId)) {
            throw new ResourceNotFoundException("Cet utilisateur n'est pas membre de l'espace");
        }
        membreSpaceRepository.deleteBySpaceIdAndUserId(id, memberId);
    }

    /** Un membre quitte l'espace de lui-même (le propriétaire doit passer par la suppression de l'espace). */
    @Transactional
    public void leave(UUID id, UUID userId) {
        Space space = findOrThrow(id);
        if (space.getUserId().equals(userId)) {
            throw new ConflictException(
                    "Le propriétaire ne peut pas quitter son espace — supprimez-le plutôt");
        }
        if (!membreSpaceRepository.existsBySpaceIdAndUserId(id, userId)) {
            throw new ResourceNotFoundException("Vous n'êtes pas membre de cet espace");
        }
        membreSpaceRepository.deleteBySpaceIdAndUserId(id, userId);
    }

    /** Code d'invitation — réservé au PROPRIETAIRE (c'est ce qui protège l'accès). */
    public InviteCodeResponse getInviteCode(UUID id, UUID requesterId, boolean isAdmin) {
        Space space = findOrThrow(id);
        assertOwnerOrAdmin(space, requesterId, isAdmin);
        return new InviteCodeResponse(space.getInviteCode());
    }

    /** Régénération du code (ex. code diffusé trop largement) — réservé au propriétaire. */
    @Transactional
    public InviteCodeResponse regenerateInviteCode(UUID id, UUID requesterId, boolean isAdmin) {
        Space space = findOrThrow(id);
        assertOwnerOrAdmin(space, requesterId, isAdmin);
        space.setInviteCode(generateUniqueCode());
        spaceRepository.save(space);
        return new InviteCodeResponse(space.getInviteCode());
    }

    @Transactional
    public SpaceResponse update(UUID id, UUID requesterId, boolean isAdmin, UpdateSpaceRequest request) {
        Space space = findOrThrow(id);
        assertOwnerOrAdmin(space, requesterId, isAdmin);
        space.setName(request.name());
        space.setDescription(request.description());
        space.setSubjectTag(request.subjectTag());
        return SpaceResponse.from(spaceRepository.save(space), requesterId);
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
        // Les adhésions partent avec les spaces (ON DELETE CASCADE) ; les contenus créés
        // ailleurs (conversations, fiches...) restent purgés par leurs propres listeners.
    }

    private Space findOrThrow(UUID id) {
        return spaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Espace introuvable : " + id));
    }

    /** Lecture/participation : propriétaire, admin, OU membre ayant rejoint par code. */
    private void assertCanView(Space space, UUID requesterId, boolean isAdmin) {
        if (isAdmin || space.getUserId().equals(requesterId)) {
            return;
        }
        if (!membreSpaceRepository.existsBySpaceIdAndUserId(space.getId(), requesterId)) {
            throw new ForbiddenException("Accès refusé à cet espace de cours");
        }
    }

    private void assertOwnerOrAdmin(Space space, UUID requesterId, boolean isAdmin) {
        if (!isAdmin && !space.getUserId().equals(requesterId)) {
            throw new ForbiddenException("Accès refusé à cet espace de cours");
        }
    }

    /** Code unique : boucle sur la contrainte d'unicité (collision statistiquement négligeable). */
    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = randomCode();
            if (spaceRepository.findByInviteCodeIgnoreCase(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Impossible de générer un code d'invitation unique");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
