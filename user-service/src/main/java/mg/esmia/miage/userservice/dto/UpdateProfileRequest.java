package mg.esmia.miage.userservice.dto;

import mg.esmia.miage.userservice.entity.User;

/**
 * Mise à jour partielle du profil. Tous les champs sont optionnels
 * (PATCH sémantique) : seul le(s) champ(s) non nul(s) est/sont mis à jour.
 */
public record UpdateProfileRequest(
        String displayName,
        User.Role role
) {
}
