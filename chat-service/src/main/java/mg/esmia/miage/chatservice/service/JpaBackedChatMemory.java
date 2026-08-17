package mg.esmia.miage.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.chatservice.entity.Message;
import mg.esmia.miage.chatservice.repository.MessageRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * {@link ChatMemory} custom adossé à la table {@code messages} (PostgreSQL) via
 * {@link MessageRepository} — une seule source de vérité pour l'historique de conversation,
 * PAS de duplication avec un store en mémoire Spring AI (cf. ARCHITECTURE.md §6.3).
 *
 * <ul>
 *   <li>{@code get(conversationId)} : reconstruit la liste de {@code Message} Spring AI depuis
 *       le repository (le message USER et le message ASSISTANT du tour courant sont déjà en base
 *       quand {@code MessageChatMemoryAdvisor.before()} les injecte dans le prompt).</li>
 *   <li>{@code add(conversationId, messages)} : persiste via le même repository. La persistance
 *       du message USER est déjà faite par {@link ChatService} avant l'appel LLM (événement
 *       MESSAGE_CREATED immédiat) : {@code add()} est donc <b>idempotent</b> — un message dont
 *       le contenu correspond au dernier message du même rôle n'est pas ré-inséré.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaBackedChatMemory implements ChatMemory {

    private final MessageRepository messageRepository;

    /**
     * Fenêtre d'historique injectée dans le prompt par {@code MessageChatMemoryAdvisor.before()}
     * (équivalent de {@code CHAT_MAX_HISTORY_MESSAGES} de l'ancien {@code buildHistoryContext}).
     */
    @Value("${chat.max-history-messages:10}")
    private int maxHistoryMessages;

    @Override
    public void add(String conversationId, List<org.springframework.ai.chat.messages.Message> messages) {
        UUID conversationUuid = UUID.fromString(conversationId);
        for (org.springframework.ai.chat.messages.Message message : messages) {
            if (isAlreadyPersisted(conversationUuid, message)) {
                continue;
            }
            messageRepository.save(toEntity(conversationUuid, message));
        }
    }

    @Override
    public List<org.springframework.ai.chat.messages.Message> get(String conversationId) {
        List<Message> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(UUID.fromString(conversationId));
        List<Message> tail = history.size() > maxHistoryMessages
                ? history.subList(history.size() - maxHistoryMessages, history.size())
                : history;
        return tail.stream().map(this::toAiMessage).toList();
    }

    @Override
    public void clear(String conversationId) {
        log.warn("JpaBackedChatMemory.clear({}) : hors périmètre (suppression d'historique non implémentée)", conversationId);
    }

    private boolean isAlreadyPersisted(UUID conversationUuid, org.springframework.ai.chat.messages.Message message) {
        List<Message> all = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationUuid);
        Message.Role role = message.getMessageType() == MessageType.USER ? Message.Role.USER : Message.Role.ASSISTANT;
        for (int i = all.size() - 1; i >= 0; i--) {
            Message entity = all.get(i);
            if (entity.getRole() == role) {
                return entity.getContent().equals(message.getText());
            }
        }
        return false;
    }

    private Message toEntity(UUID conversationUuid, org.springframework.ai.chat.messages.Message message) {
        return Message.builder()
                .conversationId(conversationUuid)
                .role(message.getMessageType() == MessageType.USER ? Message.Role.USER : Message.Role.ASSISTANT)
                .content(message.getText())
                .build();
    }

    private org.springframework.ai.chat.messages.Message toAiMessage(Message entity) {
        if (entity.getRole() == Message.Role.USER) {
            return new UserMessage(entity.getContent());
        }
        return new AssistantMessage(entity.getContent());
    }
}