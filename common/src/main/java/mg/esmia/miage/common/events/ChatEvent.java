package mg.esmia.miage.common.events;

import java.time.Instant;

/**
 * Publié par chat-service sur le canal "chat.events" à chaque message.
 * event: MESSAGE_CREATED
 * Consommé par analytics-service (statistiques d'usage, détection de notions difficiles).
 */
public record ChatEvent(
        String event,
        String messageId,
        String conversationId,
        String spaceId,
        String userId,
        String role,
        String content,
        Instant timestamp
) {
    public static final String MESSAGE_CREATED = "MESSAGE_CREATED";

    public static ChatEvent messageCreated(String messageId, String conversationId, String spaceId,
                                            String userId, String role, String content) {
        return new ChatEvent(MESSAGE_CREATED, messageId, conversationId, spaceId, userId, role, content, Instant.now());
    }
}
