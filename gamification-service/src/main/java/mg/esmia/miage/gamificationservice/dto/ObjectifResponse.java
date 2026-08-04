package mg.esmia.miage.gamificationservice.dto;

import mg.esmia.miage.gamificationservice.entity.ObjectifRevision;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ObjectifResponse(UUID id, UUID spaceId, String titre, String description,
                                LocalDate dateEcheance, ObjectifRevision.Statut statut, Instant createdAt) {
    public static ObjectifResponse from(ObjectifRevision o) {
        return new ObjectifResponse(o.getId(), o.getSpaceId(), o.getTitre(), o.getDescription(),
                o.getDateEcheance(), o.getStatut(), o.getCreatedAt());
    }
}
