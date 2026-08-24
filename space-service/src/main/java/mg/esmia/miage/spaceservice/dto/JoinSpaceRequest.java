package mg.esmia.miage.spaceservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requête "rejoindre un espace" via son code d'invitation. Le code est insensible
 * à la casse (l'étudiant le recopie à la main) et sans tiret ni espace autour.
 */
public record JoinSpaceRequest(
        @NotBlank @Size(max = 10) String code
) {
}
