package mg.esmia.miage.ficheservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record GenerateQuizRequest(
        @NotNull UUID spaceId,
        String title,
        /** DOCUMENT, SPACE ou TOPIC. */
        String scope,
        /** UUID du document ciblé (si scope=DOCUMENT). */
        UUID targetDocumentId,
        /** Nom du topic ciblé (si scope=TOPIC). */
        String targetTopic,
        /** UUID optionnel d'une fiche source. */
        UUID sourceFicheId,
        /** Documents utilisés pour la génération. */
        List<UUID> documentIds,
        /** FACILE, MOYEN ou DIFFICILE. */
        String difficulty,
        /** Nombre de questions (défaut: 10). */
        Integer questionCount,
        /** BROUILLON ou PUBLIE (optionnel, défaut: BROUILLON). */
        String statut
) {
}
