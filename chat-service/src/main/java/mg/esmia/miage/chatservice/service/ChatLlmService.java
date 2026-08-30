package mg.esmia.miage.chatservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.aicommon.ChatProviderResolver;
import mg.esmia.miage.chatservice.client.IngestionClient;
import mg.esmia.miage.chatservice.dto.Citation;
import mg.esmia.miage.chatservice.dto.StructuredContent;
import mg.esmia.miage.chatservice.entity.Conversation;
import mg.esmia.miage.chatservice.rag.RagPipelineAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Cœur du pipeline RAG+LLM de chat-service, exposé en composant séparé pour que le
 * {@link CircuitBreaker} soit appliqué par proxy Spring (pas d'auto-invocation AOP) —
 * cf. ARCHITECTURE.md §6.3.
 *
 * <ul>
 *   <li><b>@CircuitBreaker(llm-chat)</b> : tout échec d'appel LLM (provider indisponible,
 *       RAG en erreur…) bascule sur {@link #fallbackAssistantReply} — une réponse claire
 *       « assistant temporairement indisponible », jamais une réponse statique trompeuse.</li>
 *   <li>Le persona est résolu en amont par {@link ChatService} (défaillance non bloquante :
 *       bascule sur un persona générique), ce qui n'ouvre pas le circuit quand seul
 *       space-service est down.</li>
 *   <li>{@link MessageChatMemoryAdvisor} + {@link JpaBackedChatMemory} : injection de
 *       l'historique dans le prompt et persistance des messages via le repository unique.</li>
 *   <li>{@link RagPipelineAdvisor} : retrieval large + rerank + augmentation du contexte.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatLlmService {

    private static final String FALLBACK_REPLY =
            "L'assistant est temporairement indisponible. Réessayez dans quelques instants.";

    private final ChatProviderResolver chatProviderResolver;
    private final RagPipelineAdvisor ragPipelineAdvisor;
    private final JpaBackedChatMemory jpaBackedChatMemory;
    private final IngestionClient ingestionClient;
    private final ResponseParser responseParser;

    @CircuitBreaker(name = "llm-chat", fallbackMethod = "fallbackAssistantReply")
    public LlmOutcome generate(Conversation conversation, String question, String systemPrompt) {
        try {
            ChatResponse response = chatProviderResolver.current()
                    .prompt()
                    .system(systemPrompt)
                    .advisors(spec -> spec
                            .param(ChatMemory.CONVERSATION_ID, conversation.getId().toString())
                            .param(RagPipelineAdvisor.SPACE_ID_CONTEXT, conversation.getSpaceId().toString())
                            .advisors(ragPipelineAdvisor,
                                    MessageChatMemoryAdvisor.builder(jpaBackedChatMemory).build()))
                    .user(question)
                    .call()
                    .chatResponse();
            String content = response.getResult().getOutput().getText();
            List<Document> retrieved = extractRetrievedDocuments(response);
            StructuredContent structured = responseParser.parse(content);
            return new LlmOutcome(content, chunkIdsOf(retrieved),
                    chatProviderResolver.activeProvider(),
                    buildCitations(retrieved, conversation.getUserId()),
                    structured);
        } catch (Exception e) {
            log.error("Échec RAG+LLM (provider={}), bascule sur le fallback du circuit breaker",
                    chatProviderResolver.activeProvider(), e);
            throw e instanceof RuntimeException re ? re : new RuntimeException(e);
        }
    }

    /** Fallback du circuit breaker : réponse d'indisponibilité honnête (pas de contenu inventé). */
    public LlmOutcome fallbackAssistantReply(Conversation conversation, String question, String systemPrompt,
                                             Throwable t) {
        log.warn("Fallback llm-chat (provider={}) : {}", chatProviderResolver.activeProvider(), t.getMessage());
        return new LlmOutcome(FALLBACK_REPLY, new UUID[0], chatProviderResolver.activeProvider());
    }

    /**
     * Extraction des chunks réellement utilisés (métadonnée posée par le RagPipelineAdvisor).
     * Payload Qdrant par point : {document_id, space_id, chunk_index, doc_content, content} —
     * cf. ingestion-service/QdrantVectorService.
     */
    private List<Document> extractRetrievedDocuments(ChatResponse response) {
        List<Document> docs = response.getMetadata().get(RagPipelineAdvisor.RETRIEVED_DOCUMENTS);
        return docs == null ? List.of() : docs;
    }

    private UUID[] chunkIdsOf(List<Document> docs) {
        return docs.stream()
                .map(Document::getId)
                .map(id -> {
                    try {
                        return UUID.fromString(id);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toArray(UUID[]::new);
    }

    /**
     * Citations lisibles dérivées des documents retrievés : documentId/chunkIndex/extrait
     * viennent du payload Qdrant ; le nom de fichier est résolu via ingestion-service
     * ({@link IngestionClient}, non bloquant — null => citation sans nom).
     */
    private List<Citation> buildCitations(List<Document> docs, UUID requesterUserId) {
        if (docs.isEmpty()) {
            return List.of();
        }
        // Un nom par documentId : plusieurs chunks d'un même document = une seule résolution REST.
        Map<UUID, String> namesByDocument = new HashMap<>();
        return docs.stream()
                .map(doc -> toCitation(doc, requesterUserId, namesByDocument))
                .filter(Objects::nonNull)
                .toList();
    }

    private Citation toCitation(Document doc, UUID requesterUserId, Map<UUID, String> namesByDocument) {
        try {
            UUID chunkId = UUID.fromString(doc.getId());
            Object documentIdRaw = doc.getMetadata().get("document_id");
            Object chunkIndexRaw = doc.getMetadata().get("chunk_index");
            UUID documentId = documentIdRaw == null ? null : UUID.fromString(documentIdRaw.toString());
            Integer chunkIndex = chunkIndexRaw instanceof Number n ? n.intValue() : null;

            String documentName = null;
            if (documentId != null) {
                documentName = namesByDocument.computeIfAbsent(documentId,
                        id -> ingestionClient.getDocumentName(id, requesterUserId));
            }
            return Citation.of(chunkId, documentId, chunkIndex, doc.getText(), documentName);
        } catch (IllegalArgumentException e) {
            // Chunk sans ID/UUID exploitable : pas de citation, la traçabilité brute est conservée à part.
            log.warn("Chunk retrievé avec identifiant non-UUID ({}), citation ignorée", doc.getId());
            return null;
        }
    }
}