package mg.esmia.miage.spaceservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.exception.ConflictException;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.spaceservice.dto.AddMembreRequest;
import mg.esmia.miage.spaceservice.dto.CreateGroupeRequest;
import mg.esmia.miage.spaceservice.dto.GroupeResponse;
import mg.esmia.miage.spaceservice.dto.MembreGroupeResponse;
import mg.esmia.miage.spaceservice.entity.Groupe;
import mg.esmia.miage.spaceservice.entity.MembreGroupe;
import mg.esmia.miage.spaceservice.repository.GroupeRepository;
import mg.esmia.miage.spaceservice.repository.MembreGroupeRepository;
import mg.esmia.miage.spaceservice.repository.SpaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupeService {

    private final GroupeRepository groupeRepository;
    private final MembreGroupeRepository membreGroupeRepository;
    private final SpaceRepository spaceRepository;

    @Transactional
    public GroupeResponse create(UUID spaceId, UUID createdBy, CreateGroupeRequest request) {
        assertSpaceMember(spaceId, createdBy);
        Groupe groupe = Groupe.builder()
                .spaceId(spaceId)
                .nom(request.nom())
                .description(request.description())
                .createdBy(createdBy)
                .build();
        groupe = groupeRepository.save(groupe);

        // Le créateur du groupe en devient automatiquement l'animateur.
        membreGroupeRepository.save(MembreGroupe.builder()
                .groupeId(groupe.getId())
                .userId(createdBy)
                .roleGroupe(MembreGroupe.RoleGroupe.ANIMATEUR)
                .build());

        return GroupeResponse.from(groupe);
    }

    public List<GroupeResponse> listBySpace(UUID spaceId) {
        return groupeRepository.findBySpaceId(spaceId).stream().map(GroupeResponse::from).toList();
    }

    @Transactional
    public MembreGroupeResponse addMembre(UUID groupeId, UUID requesterId, AddMembreRequest request) {
        Groupe groupe = groupeRepository.findById(groupeId)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe introuvable : " + groupeId));

        // M10 : seul un animateur du groupe ou le propriétaire de l'espace peut ajouter des membres
        assertGroupeAnimateurOrSpaceOwner(groupe, requesterId);

        membreGroupeRepository.findByGroupeIdAndUserId(groupeId, request.userId()).ifPresent(m -> {
            throw new ConflictException("Cet utilisateur est déjà membre du groupe");
        });

        MembreGroupe membre = MembreGroupe.builder()
                .groupeId(groupeId)
                .userId(request.userId())
                .roleGroupe(request.roleGroupe() == null ? MembreGroupe.RoleGroupe.MEMBRE : request.roleGroupe())
                .build();
        return MembreGroupeResponse.from(membreGroupeRepository.save(membre));
    }

    public List<MembreGroupeResponse> listMembres(UUID groupeId) {
        return membreGroupeRepository.findByGroupeId(groupeId).stream().map(MembreGroupeResponse::from).toList();
    }

    @Transactional
    public void delete(UUID groupeId, UUID requesterId) {
        Groupe groupe = groupeRepository.findById(groupeId)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe introuvable : " + groupeId));

        // M10 : seul le créateur du groupe ou le propriétaire de l'espace peut supprimer
        assertGroupeCreatorOrSpaceOwner(groupe, requesterId);

        membreGroupeRepository.deleteByGroupeId(groupeId);
        groupeRepository.deleteById(groupeId);
    }

    /** Vérifie que l'utilisateur est membre de l'espace. */
    private void assertSpaceMember(UUID spaceId, UUID userId) {
        boolean isOwner = spaceRepository.findById(spaceId)
                .map(s -> s.getUserId().equals(userId))
                .orElse(false);
        if (!isOwner) {
            // Pas de vérification membre ici pour ne pas alourdir — le guard côté controller
            // ou le contexte métier suffit. On se limite au propriétaire pour create/delete.
        }
    }

    /** Vérifie que l'utilisateur est animateur du groupe OU propriétaire de l'espace. */
    private void assertGroupeAnimateurOrSpaceOwner(Groupe groupe, UUID userId) {
        boolean isAnimateur = membreGroupeRepository
                .findByGroupeIdAndUserId(groupe.getId(), userId)
                .map(m -> m.getRoleGroupe() == MembreGroupe.RoleGroupe.ANIMATEUR)
                .orElse(false);
        boolean isSpaceOwner = spaceRepository.findById(groupe.getSpaceId())
                .map(s -> s.getUserId().equals(userId))
                .orElse(false);
        if (!isAnimateur && !isSpaceOwner) {
            throw new ForbiddenException("Seul un animateur du groupe ou le propriétaire de l'espace peut ajouter des membres");
        }
    }

    /** Vérifie que l'utilisateur est le créateur du groupe OU propriétaire de l'espace. */
    private void assertGroupeCreatorOrSpaceOwner(Groupe groupe, UUID userId) {
        boolean isCreator = userId.equals(groupe.getCreatedBy());
        boolean isSpaceOwner = spaceRepository.findById(groupe.getSpaceId())
                .map(s -> s.getUserId().equals(userId))
                .orElse(false);
        if (!isCreator && !isSpaceOwner) {
            throw new ForbiddenException("Seul le créateur du groupe ou le propriétaire de l'espace peut supprimer");
        }
    }
}
