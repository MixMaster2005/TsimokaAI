package mg.esmia.miage.spaceservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.exception.ConflictException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.spaceservice.dto.AddMembreRequest;
import mg.esmia.miage.spaceservice.dto.CreateGroupeRequest;
import mg.esmia.miage.spaceservice.dto.GroupeResponse;
import mg.esmia.miage.spaceservice.dto.MembreGroupeResponse;
import mg.esmia.miage.spaceservice.entity.Groupe;
import mg.esmia.miage.spaceservice.entity.MembreGroupe;
import mg.esmia.miage.spaceservice.repository.GroupeRepository;
import mg.esmia.miage.spaceservice.repository.MembreGroupeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupeService {

    private final GroupeRepository groupeRepository;
    private final MembreGroupeRepository membreGroupeRepository;

    @Transactional
    public GroupeResponse create(UUID spaceId, UUID createdBy, CreateGroupeRequest request) {
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
    public MembreGroupeResponse addMembre(UUID groupeId, AddMembreRequest request) {
        groupeRepository.findById(groupeId)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe introuvable : " + groupeId));

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
    public void delete(UUID groupeId) {
        membreGroupeRepository.deleteByGroupeId(groupeId);
        groupeRepository.deleteById(groupeId);
    }
}
