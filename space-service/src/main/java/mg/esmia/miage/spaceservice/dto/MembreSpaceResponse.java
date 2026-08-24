package mg.esmia.miage.spaceservice.dto;

import mg.esmia.miage.spaceservice.entity.MembreSpace;

import java.time.Instant;
import java.util.UUID;

/**
 * Membre d'un espace (hors propriétaire, qui est identifié par {@code SpaceResponse.userId}).
 * Pas de nom lisible ici : user-service est la seule source de vérité pour les profils,
 * et il n'existe pas d'endpoint de résolution batch d'utilisateurs — le front complète
 * avec sa session ou affiche l'identifiant (cf. README frontend, limitation connue).
 */
public record MembreSpaceResponse(
        UUID id, UUID spaceId, UUID userId, Instant joinedAt
) {
    public static MembreSpaceResponse from(MembreSpace membre) {
        return new MembreSpaceResponse(membre.getId(), membre.getSpaceId(), membre.getUserId(), membre.getJoinedAt());
    }
}
