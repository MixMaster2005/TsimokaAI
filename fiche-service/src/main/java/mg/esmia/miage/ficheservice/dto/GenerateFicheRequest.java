package mg.esmia.miage.ficheservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record GenerateFicheRequest(
        @NotNull UUID spaceId,
        String title,
        /** Sous-ensemble de documents ciblés ; vide = ensemble du corpus de l'espace. */
        List<UUID> documentIds
) {
}
