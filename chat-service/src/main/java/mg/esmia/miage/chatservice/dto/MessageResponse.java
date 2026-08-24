package mg.esmia.miage.chatservice.dto;

import mg.esmia.miage.chatservice.entity.Message;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
        UUID id, UUID conversationId, Message.Role role, String content,
        List<UUID> retrievedChunkIds, List<Citation> citations, String modelUsed, Instant createdAt
) {
    public static MessageResponse from(Message m) {
        return new MessageResponse(m.getId(), m.getConversationId(), m.getRole(), m.getContent(),
                List.of(m.getRetrievedChunkIds()),
                // Messages antérieurs à la feature / fallback circuit breaker : liste vide (pas de null).
                m.getCitations() == null ? List.of() : m.getCitations(),
                m.getModelUsed(), m.getCreatedAt());
    }
}
