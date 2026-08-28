package mg.esmia.miage.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.chatservice.client.SpaceClient;
import mg.esmia.miage.chatservice.dto.MessageResponse;
import mg.esmia.miage.chatservice.dto.SendMessageRequest;
import mg.esmia.miage.chatservice.dto.StructuredContent;
import mg.esmia.miage.chatservice.entity.Conversation;
import mg.esmia.miage.chatservice.entity.Message;
import mg.esmia.miage.chatservice.repository.MessageRepository;
import mg.esmia.miage.common.events.ChatEvent;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Orchestration chat (cf. CDC §4.3 et ARCHITECTURE.md §6.3) :
 *
 *   1. Le message USER est persisté immédiatement (événement MESSAGE_CREATED tout de suite,
 *      même si l'appel LLM dure ou échoue).
 *   2. L'appel RAG+LLM est délégué à {@link ChatLlmService} (circuit breaker {@code llm-chat}) :
 *      {@code RagPipelineAdvisor} (rewrite → retrieval topK élevé filtré {@code space_id} →
 *      rerank LLM → contexte) + {@code MessageChatMemoryAdvisor}/{@link JpaBackedChatMemory}
 *      (historique injecté et persisté via l'unique repository).
 *   3. Le {@code MessageChatMemoryAdvisor.after()} persiste le message ASSISTANT (sans
 *      chunkIds/modelUsed) : {@link ChatService} retrouve ce message, l'enrichit
 *      (retrievedChunkIds + modelUsed) et publie l'événement. En cas de fallback du circuit
 *      breaker (l'advisor n'a rien persisté), le message ASSISTANT est persisté ici-même.
 *
 * Persona : récupéré depuis space-service via {@link SpaceClient} (défaillance non bloquante —
 * bascule sur un persona générique, sans ouvrir le circuit breaker).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final MessageRepository messageRepository;
    private final RedisEventPublisher eventPublisher;
    private final ChatLlmService chatLlmService;
    private final SpaceClient spaceClient;
    private final ResponseParser responseParser;

    @Value("classpath:prompts/chat-system.st")
    private Resource chatSystemTemplate;

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

        LlmOutcome outcome = chatLlmService.generate(conversation, request.content(),
                resolvePersona(conversation));

        Message assistantMessage = findOrPersistAssistant(conversation, outcome);
        publishMessageCreated(conversation, assistantMessage);

        // Les blocs structurés ne sont pas stockés en BDD (re-parsés à l'affichage si besoin)
        // mais sont inclus dans la réponse pour le rendu riche côté frontend.
        List<mg.esmia.miage.chatservice.dto.StructuredContent.ContentBlock> blocks =
                blocksFor(assistantMessage, outcome.structuredContent());
        return MessageResponse.from(assistantMessage, blocks);
    }

    public List<MessageResponse> history(UUID conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(message -> MessageResponse.from(message, blocksFor(message, null))).toList();
    }

    /**
     * Persona pédagogique depuis space-service ; défaillance non bloquante (persona générique).
     * Le template system.chat est préfixé pour garantir le formatage structuré des réponses.
     */
    private String resolvePersona(Conversation conversation) {
        String template = readTemplate();
        try {
            String persona = spaceClient.getAssistantPersona(conversation.getSpaceId(), conversation.getUserId());
            String base = (persona == null || persona.isBlank()) ? FALLBACK_PERSONA : persona;
            return template + "\n\n" + base;
        } catch (Exception e) {
            log.warn("Persona indisponible (spaceId={}), bascule sur le persona générique", conversation.getSpaceId(), e);
            return template + "\n\n" + FALLBACK_PERSONA;
        }
    }

    private String readTemplate() {
        try (var in = chatSystemTemplate.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Template chat-system.st illisible, fallback sur le template par défaut", e);
            return FALLBACK_PERSONA;
        }
    }

    /**
     * M5 : Le message ASSISTANT est normalement déjà persisté par {@code MessageChatMemoryAdvisor.after()}
     * (via {@link JpaBackedChatMemory}, contenu identique) : on l'enrichit alors avec
     * retrievedChunkIds + modelUsed. S'il n'existe pas (fallback circuit breaker, l'advisor n'a
     * pas tourné), on le persiste ici-même.
     */
    private Message findOrPersistAssistant(Conversation conversation, LlmOutcome outcome) {
        // M5 : requête ciblée au lieu de charger tout l'historique
        return messageRepository.findLastAssistantByContent(conversation.getId(), outcome.content())
                .map(existing -> {
                    existing.setRetrievedChunkIds(outcome.chunkIds());
                    existing.setModelUsed(outcome.modelUsed());
                    existing.setCitations(outcome.citations());
                    return messageRepository.save(existing);
                })
                .orElseGet(() -> {
                    Message assistantMessage = Message.builder()
                            .conversationId(conversation.getId())
                            .role(Message.Role.ASSISTANT)
                            .content(outcome.content())
                            .retrievedChunkIds(outcome.chunkIds())
                            .modelUsed(outcome.modelUsed())
                            .citations(outcome.citations())
                            .build();
                    return messageRepository.save(assistantMessage);
                });
    }

    private void publishMessageCreated(Conversation conversation, Message message) {
        eventPublisher.publish(EventChannels.CHAT_EVENTS, ChatEvent.messageCreated(
                message.getId().toString(), conversation.getId().toString(), conversation.getSpaceId().toString(),
                conversation.getUserId().toString(), message.getRole().name(), message.getContent()));
    }

    private List<StructuredContent.ContentBlock> blocksFor(Message message, StructuredContent preParsed) {
        if (message.getRole() != Message.Role.ASSISTANT) {
            return null;
        }
        return preParsed != null ? preParsed.blocks() : responseParser.parse(message.getContent()).blocks();
    }
}
