package mg.esmia.miage.ficheservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record GenerateQuizRequest(
        @NotNull UUID spaceId,
        String title,
        /** DOCUMENT, SPACE ou TOPIC. */
        @Pattern(regexp = "DOCUMENT|SPACE|TOPIC")
        String scope,
        /** UUID du document ciblé (si scope=DOCUMENT). */
        UUID targetDocumentId,
        /** Nom du topic ciblé (si scope=TOPIC). */
        @Size(min = 3, max = 120)
        String targetTopic,
        /** UUID optionnel d'une fiche source. */
        UUID sourceFicheId,
        /** Documents utilisés pour la génération. */
        List<UUID> documentIds,
        /** FACILE, MOYEN ou DIFFICILE. */
        @Pattern(regexp = "FACILE|MOYEN|DIFFICILE")
        String difficulty,
        /** Nombre de questions (défaut: 10). */
        @Min(1)
        @Max(20)
        Integer questionCount,
        /** BROUILLON ou PUBLIE (optionnel, défaut: BROUILLON). */
        String statut
) {
}
