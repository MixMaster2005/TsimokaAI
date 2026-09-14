package mg.esmia.miage.ficheservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mg.esmia.miage.ficheservice.entity.ValidationFiche;

public record ValidateFicheRequest(@NotNull ValidationFiche.Statut statut, @Size(max = 2000) String commentaire) {
}
