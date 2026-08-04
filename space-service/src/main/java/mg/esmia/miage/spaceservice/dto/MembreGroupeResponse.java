package mg.esmia.miage.spaceservice.dto;

import mg.esmia.miage.spaceservice.entity.MembreGroupe;

import java.time.Instant;
import java.util.UUID;

public record MembreGroupeResponse(UUID id, UUID groupeId, UUID userId, MembreGroupe.RoleGroupe roleGroupe, Instant joinedAt) {
    public static MembreGroupeResponse from(MembreGroupe m) {
        return new MembreGroupeResponse(m.getId(), m.getGroupeId(), m.getUserId(), m.getRoleGroupe(), m.getJoinedAt());
    }
}
