package mg.esmia.miage.gamificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateObjectifRequest(@NotNull UUID spaceId, @NotBlank String titre, String description, LocalDate dateEcheance) {
}
