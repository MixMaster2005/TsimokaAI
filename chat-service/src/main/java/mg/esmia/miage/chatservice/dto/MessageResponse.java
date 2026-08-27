package mg.esmia.miage.chatservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import mg.esmia.miage.chatservice.entity.Message;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageResponse(
        UUID id, UUID conversationId, Message.Role role, String content,
        List<UUID> retrievedChunkIds, List<Citation> citations, String modelUsed,
        List<StructuredContent.ContentBlock> blocks, Instant createdAt
) {
    public static MessageResponse from(Message m) {
        return from(m, null);
    }

    public static MessageResponse from(Message m, List<StructuredContent.ContentBlock> blocks) {
        return new MessageResponse(m.getId(), m.getConversationId(), m.getRole(), m.getContent(),
                List.of(m.getRetrievedChunkIds()),
                m.getCitations() == null ? List.of() : m.getCitations(),
                m.getModelUsed(), blocks, m.getCreatedAt());
    }
}
