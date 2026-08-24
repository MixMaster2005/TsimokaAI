package mg.esmia.miage.spaceservice.dto;

import mg.esmia.miage.spaceservice.entity.Space;

import java.time.Instant;
import java.util.UUID;

/**
 * @param owner true si l'utilisateur à l'origine de la requête est le PROPRIÉTAIRE de
 *              l'espace (écriture réservée) ; false = simple membre (accès
 *              lecture/participation) ou appel interne sans requesterId.
 */
public record SpaceResponse(
        UUID id, UUID userId, String name, String description, String subjectTag,
        String assistantPersona, boolean owner, Instant createdAt, Instant updatedAt
) {
    public static SpaceResponse from(Space space) {
        return from(space, null);
    }

    public static SpaceResponse from(Space space, UUID requesterId) {
        return new SpaceResponse(space.getId(), space.getUserId(), space.getName(), space.getDescription(),
                space.getSubjectTag(), space.getAssistantPersona(),
                requesterId != null && space.getUserId().equals(requesterId),
                space.getCreatedAt(), space.getUpdatedAt());
    }
}
