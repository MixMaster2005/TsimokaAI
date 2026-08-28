package mg.esmia.miage.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "L'ancien mot de passe est obligatoire")
        String ancienMotDePasse,

        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 8, message = "Le nouveau mot de passe doit faire au moins 8 caractères")
        String nouveauMotDePasse
) {}
