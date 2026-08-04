package mg.esmia.miage.ficheservice.dto;

import jakarta.validation.constraints.NotNull;
import mg.esmia.miage.ficheservice.entity.ValidationFiche;

public record ValidateFicheRequest(@NotNull ValidationFiche.Statut statut, String commentaire) {
}
