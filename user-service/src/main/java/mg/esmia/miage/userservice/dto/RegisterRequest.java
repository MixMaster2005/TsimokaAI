package mg.esmia.miage.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requête d'inscription. Le rôle est toujours STUDENT à l'inscription.
 * Le choix Étudiant/Enseignant se fait via l'onboarding (PATCH /me avec role).
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") String password,
        @NotBlank String displayName
) {
}
