package mg.esmia.miage.gamificationservice.dto;

import jakarta.validation.constraints.NotNull;
import mg.esmia.miage.gamificationservice.entity.ObjectifRevision;

public record UpdateObjectifRequest(@NotNull ObjectifRevision.Statut statut) {
}
