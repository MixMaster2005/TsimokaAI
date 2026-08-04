package mg.esmia.miage.gamificationservice.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateRappelRequest(UUID spaceId, @NotBlank String message, @NotNull @FutureOrPresent Instant prevuLe) {
}
