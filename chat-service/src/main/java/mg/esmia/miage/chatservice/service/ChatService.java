package mg.esmia.miage.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.ChatEvent;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.chatservice.dto.MessageResponse;
import mg.esmia.miage.chatservice.dto.SendMessageRequest;
import mg.esmia.miage.chatservice.entity.Conversation;
import mg.esmia.miage.chatservice.entity.Message;
import mg.esmia.miage.chatservice.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * ================================ TODO (cœur IA à implémenter) ================================
 * Orchestration RAG (cf. CDC §4.3 et "Base de projet") :
 *
 *   1. Charger l'historique récent de la conversation (CHAT_MAX_HISTORY_MESSAGES, cf.
 *      application.yml) via MessageRepository.
 *   2. Récupérer le persona pédagogique de l'espace (appel REST à space-service
 *      GET /api/v1/spaces/{id}, en interne via la gateway ou un WebClient direct
 *      service-à-service — à trancher et documenter dans le mémoire).
 *   3. Retrieval : embedder la question de l'utilisateur puis chercher les
 *      CHAT_MAX_RETRIEVED_CHUNKS chunks les plus pertinents dans Qdrant
 *      (collection "chunks_{spaceId}", cf. QdrantClient déjà injectable).
 *   4. Construire le prompt : persona en instruction système + chunks retrouvés en
 *      contexte + historique + question.
 *   5. Appeler le LLM actif selon ACTIVE_LLM_PROVIDER (groq / gemini / ollama, cf.
 *      LlmProviderConfig) via Spring AI ChatClient.
 *   6. Persister le message ASSISTANT avec retrievedChunkIds + modelUsed, publier
 *      MESSAGE_CREATED pour les deux messages (USER puis ASSISTANT).
 *
 * Le squelette ci-dessous persiste bien les deux messages et publie les événements attendus
 * par analytics-service, mais renvoie une réponse STATIQUE au lieu d'un vrai appel RAG+LLM.
 * ================================================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final MessageRepository messageRepository;
    private final RedisEventPublisher eventPublisher;

    @Transactional
    public MessageResponse sendMessage(Conversation conversation, SendMessageRequest request) {
        Message userMessage = Message.builder()
                .conversationId(conversation.getId())
                .role(Message.Role.USER)
                .content(request.content())
                .build();
        userMessage = messageRepository.save(userMessage);
        publishMessageCreated(conversation, userMessage);

        // TODO : remplacer ce bloc par le pipeline RAG réel décrit ci-dessus.
        log.warn("ChatService.sendMessage : orchestration RAG non implémentée (TODO), réponse statique renvoyée");
        String answer = "(Réponse générée par un pipeline RAG non encore implémenté — voir ChatService.TODO)";

        Message assistantMessage = Message.builder()
                .conversationId(conversation.getId())
                .role(Message.Role.ASSISTANT)
                .content(answer)
                .retrievedChunkIds(new UUID[0]) // TODO: IDs des chunks réellement utilisés
                .modelUsed("TODO") // TODO: nom réel du modèle utilisé (ex: groq/llama-3.3-70b)
                .build();
        assistantMessage = messageRepository.save(assistantMessage);
        publishMessageCreated(conversation, assistantMessage);

        return MessageResponse.from(assistantMessage);
    }

    public List<MessageResponse> history(UUID conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(MessageResponse::from).toList();
    }

    private void publishMessageCreated(Conversation conversation, Message message) {
        eventPublisher.publish(EventChannels.CHAT_EVENTS, ChatEvent.messageCreated(
                message.getId().toString(), conversation.getId().toString(), conversation.getSpaceId().toString(),
                conversation.getUserId().toString(), message.getRole().name(), message.getContent()));
    }
}
