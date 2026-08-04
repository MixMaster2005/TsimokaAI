package mg.esmia.miage.chatservice.dto;

import mg.esmia.miage.chatservice.entity.Conversation;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(UUID id, UUID spaceId, UUID userId, String title, Instant createdAt, Instant updatedAt) {
    public static ConversationResponse from(Conversation c) {
        return new ConversationResponse(c.getId(), c.getSpaceId(), c.getUserId(), c.getTitle(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
