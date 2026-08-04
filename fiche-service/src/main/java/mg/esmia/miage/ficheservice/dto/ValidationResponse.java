package mg.esmia.miage.ficheservice.dto;

import mg.esmia.miage.ficheservice.entity.ValidationFiche;

import java.time.Instant;
import java.util.UUID;

public record ValidationResponse(UUID id, UUID ficheId, UUID enseignantId, ValidationFiche.Statut statut,
                                  String commentaire, Instant validatedAt) {
    public static ValidationResponse from(ValidationFiche v) {
        return new ValidationResponse(v.getId(), v.getFicheId(), v.getEnseignantId(), v.getStatut(), v.getCommentaire(), v.getValidatedAt());
    }
}
