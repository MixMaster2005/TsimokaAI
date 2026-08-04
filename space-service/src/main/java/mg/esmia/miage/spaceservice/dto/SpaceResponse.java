package mg.esmia.miage.spaceservice.dto;

import mg.esmia.miage.spaceservice.entity.Space;

import java.time.Instant;
import java.util.UUID;

public record SpaceResponse(
        UUID id, UUID userId, String name, String description, String subjectTag,
        String assistantPersona, Instant createdAt, Instant updatedAt
) {
    public static SpaceResponse from(Space space) {
        return new SpaceResponse(space.getId(), space.getUserId(), space.getName(), space.getDescription(),
                space.getSubjectTag(), space.getAssistantPersona(), space.getCreatedAt(), space.getUpdatedAt());
    }
}
