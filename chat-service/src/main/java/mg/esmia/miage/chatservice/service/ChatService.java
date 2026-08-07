package mg.esmia.miage.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.chatservice.client.SpaceClient;
import mg.esmia.miage.chatservice.config.ChatProviderResolver;
import mg.esmia.miage.chatservice.dto.MessageResponse;
import mg.esmia.miage.chatservice.dto.SendMessageRequest;
import mg.esmia.miage.chatservice.entity.Conversation;
import mg.esmia.miage.chatservice.entity.Message;
import mg.esmia.miage.chatservice.repository.MessageRepository;
import mg.esmia.miage.common.events.ChatEvent;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Orchestration RAG (cf. CDC §4.3 et "Base de projet") — câblée :
 *
 *   1. Historique récent de la conversation (CHAT_MAX_HISTORY_MESSAGES) injecté dans le
 *      prompt système (l'historique est persisté en base et reconstruit ici — pas de
 *      ChatMemory en mémoire Spring AI, cf. README « Choix techniques »).
 *   2. Persona pédagogique récupéré depuis space-service via {@link SpaceClient}
 *      (défaillance non bloquante : bascule sur un persona générique).
 *   3. Retrieval : {@link QuestionAnswerAdvisor} sur la collection UNIQUE "chunks"
 *      (Option A multi-tenant), filtrée sur {@code space_id} en payload
 *      ({@code filterExpression}), topK = CHAT_MAX_RETRIEVED_CHUNKS.
 *   4. Appel au LLM actif selon ACTIVE_LLM_PROVIDER (groq / gemini / ollama) via
 *      {@link ChatProviderResolver} ; les IDs des chunks réellement utilisés sont extraits
 *      de la métadonnée {@link QuestionAnswerAdvisor#RETRIEVED_DOCUMENTS}.
 *   5. Persistance du message ASSISTANT (retrievedChunkIds + modelUsed) et publication
 *      MESSAGE_CREATED pour les deux messages (USER puis ASSISTANT).
 *
 * Défaillance LLM ou retrieval non bloquante : réponse ASSISTANT dégradée persistée quand
 * même, afin de ne jamais casser la conversation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final MessageRepository messageRepository;
    private final RedisEventPublisher eventPublisher;
    private final ChatProviderResolver chatProviderResolver;
    private final VectorStore vectorStore;
    private final SpaceClient spaceClient;

    @Value("${chat.max-history-messages:10}")
    private int maxHistoryMessages;

    @Value("${chat.max-retrieved-chunks:5}")
    private int maxRetrievedChunks;

    @Value("${chat.similarity-threshold:0.7}")
    private double similarityThreshold;

    private static final String FALLBACK_PERSONA = """
            Tu es un assistant pédagogique (TsimokaAI) aidant un étudiant à comprendre ses
            cours. Réponds de manière claire, structurée et en français, en t'appuyant sur le
            contexte fourni (le persona spécifique de l'espace n'a pas pu être récupéré).
            """;

    @Transactional
    public MessageResponse sendMessage(Conversation conversation, SendMessageRequest request) {
        Message userMessage = Message.builder()
                .conversationId(conversation.getId())
                .role(Message.Role.USER)
                .content(request.content())
                .build();
        userMessage = messageRepository.save(userMessage);
        publishMessageCreated(conversation, userMessage);

        LlmOutcome outcome = generateAssistantReply(conversation, request.content());

        Message assistantMessage = Message.builder()
                .conversationId(conversation.getId())
                .role(Message.Role.ASSISTANT)
                .content(outcome.content())
                .retrievedChunkIds(outcome.chunkIds())
                .modelUsed(outcome.modelUsed())
                .build();
        assistantMessage = messageRepository.save(assistantMessage);
        publishMessageCreated(conversation, assistantMessage);

        return MessageResponse.from(assistantMessage);
    }

    public List<MessageResponse> history(UUID conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(MessageResponse::from).toList();
    }

    private LlmOutcome generateAssistantReply(Conversation conversation, String question) {
        String persona = spaceClient.getAssistantPersona(conversation.getSpaceId(), conversation.getUserId());
        String systemPrompt = buildSystemPrompt(persona, conversation.getId());
        try {
            ChatResponse response = chatProviderResolver.current()
                    .prompt()
                    .system(systemPrompt)
                    .advisors(buildQaAdvisor(conversation.getSpaceId()))
                    .user(question)
                    .call()
                    .chatResponse();
            String content = response.getResult().getOutput().getText();
            return new LlmOutcome(content, extractRetrievedChunkIds(response), chatProviderResolver.activeProvider());
        } catch (Exception e) {
            log.error("Échec de l'appel RAG+LLM (provider={}), réponse dégradée renvoyée",
                    chatProviderResolver.activeProvider(), e);
            return new LlmOutcome("(Réponse dégradée : l'appel au LLM a échoué — " + e.getMessage() + ")",
                    new UUID[0], chatProviderResolver.activeProvider());
        }
    }

    /**
     * Retrieval Qdrant : collection UNIQUE "chunks", points filtrés par {@code space_id} en
     * payload (Option A). Le QuestionAnswerAdvisor fusionne les chunks les plus proches dans
     * le prompt de l'utilisateur.
     */
    private QuestionAnswerAdvisor buildQaAdvisor(UUID spaceId) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .filterExpression("space_id == '" + spaceId + "'")
                        .topK(maxRetrievedChunks)
                        .similarityThreshold(similarityThreshold)
                        .build())
                .build();
    }

    /** Extraction des IDs des chunks réellement utilisés (métadonnée de la réponse LLM). */
    private UUID[] extractRetrievedChunkIds(ChatResponse response) {
        List<Document> docs = response.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
        if (docs == null || docs.isEmpty()) {
            return new UUID[0];
        }
        return docs.stream()
                .map(Document::getId)
                .map(id -> {
                    try {
                        return UUID.fromString(id);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toArray(UUID[]::new);
    }

    /**
     * Prompt système = persona de l'espace + historique récent de la conversation
     * (transcription role:contenu des CHAT_MAX_HISTORY_MESSAGES derniers messages).
     */
    private String buildSystemPrompt(String persona, UUID conversationId) {
        String effectivePersona = (persona == null || persona.isBlank()) ? FALLBACK_PERSONA : persona;
        return effectivePersona + "\n\n" + buildHistoryContext(conversationId);
    }

    private String buildHistoryContext(UUID conversationId) {
        List<Message> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<Message> tail = history.size() > maxHistoryMessages
                ? history.subList(history.size() - maxHistoryMessages, history.size())
                : history;
        StringBuilder sb = new StringBuilder("Historique de la conversation :\n");
        for (Message m : tail) {
            String speaker = m.getRole() == Message.Role.USER ? "Étudiant" : "Assistant";
            sb.append(speaker).append(" : ").append(m.getContent()).append("\n\n");
        }
        return sb.toString().strip();
    }

    private void publishMessageCreated(Conversation conversation, Message message) {
        eventPublisher.publish(EventChannels.CHAT_EVENTS, ChatEvent.messageCreated(
                message.getId().toString(), conversation.getId().toString(), conversation.getSpaceId().toString(),
                conversation.getUserId().toString(), message.getRole().name(), message.getContent()));
    }

    /** Résultat du pipeline RAG+LLM avant persistance du message ASSISTANT. */
    private record LlmOutcome(String content, UUID[] chunkIds, String modelUsed) {
    }
}
