package mg.esmia.miage.spaceservice.dto;

import jakarta.validation.constraints.NotNull;
import mg.esmia.miage.spaceservice.entity.MembreGroupe;

import java.util.UUID;

public record AddMembreRequest(@NotNull UUID userId, MembreGroupe.RoleGroupe roleGroupe) {
}
