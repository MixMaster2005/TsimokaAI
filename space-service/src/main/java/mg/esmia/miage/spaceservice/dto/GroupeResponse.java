package mg.esmia.miage.spaceservice.dto;

import mg.esmia.miage.spaceservice.entity.Groupe;

import java.time.Instant;
import java.util.UUID;

public record GroupeResponse(UUID id, UUID spaceId, String nom, String description, UUID createdBy, Instant createdAt) {
    public static GroupeResponse from(Groupe groupe) {
        return new GroupeResponse(groupe.getId(), groupe.getSpaceId(), groupe.getNom(),
                groupe.getDescription(), groupe.getCreatedBy(), groupe.getCreatedAt());
    }
}
