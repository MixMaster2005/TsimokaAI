package mg.esmia.miage.chatservice.rag;

import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.aicommon.ChatProviderResolver;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import mg.esmia.miage.chatservice.client.IngestionClient;
import mg.esmia.miage.common.context.UserContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pipeline RAG « retrieval large + rerank » — équivalent fonctionnel du
 * {@code RetrievalAugmentationAdvisor} du RAG modulaire de Spring AI 2.0 (non disponible en
 * 1.1.x, cf. ARCHITECTURE.md §6.3) :
 *
 * <ol>
 *   <li><b>Réécriture de requête</b> : un appel LLM transforme la question brute en requête de
 *       retrieval autonome — utile en multi-tour où « et pour le second cas ? » n'a aucun sens
 *       comme requête vectorielle isolée.</li>
 *   <li><b>Retrieval large</b> : topK volontairement élevé (40) avec un seuil bas (0.5) pour ne
 *       pas rater de chunk pertinent en amont, filtré sur {@code space_id} en payload.</li>
 *   <li><b>Rerank</b> : réduction au topN restreint (5) avant construction du prompt — le coût
 *       du rerank est largement compensé par la précision du contexte final.</li>
 *   <li><b>Augmentation du prompt système</b> : contexte injecté ; comportement
 *       anti-hallucination assumé si le contexte est vide.</li>
 * </ol>
 *
 * <p>Les documents effectivement utilisés sont exposés dans les métadonnées de la
 * {@link ChatResponse} sous la clé {@link #RETRIEVED_DOCUMENTS} (traçabilité
 * {@code retrieved_chunk_ids} du message ASSISTANT).
 */
@Component
@Slf4j
public class RagPipelineAdvisor implements CallAdvisor {

    /** Clé des documents retrievés dans les métadonnées de la réponse ChatResponse. */
    public static final String RETRIEVED_DOCUMENTS = "rag_retrieved_documents";
    /** Clé du {@code space_id} dans le contexte de l'advisor (set via {@code .advisors(...).param(...)}). */
    public static final String SPACE_ID_CONTEXT = "rag_space_id";

    private static final String SYSTEM_AUGMENT_TEMPLATE = """
            Contexte fourni (extrait des cours de l'espace, trié par pertinence) :
            ---------------------
            %s
            ---------------------
            Réponds en t'appuyant d'abord sur ce contexte. Si tu ne trouves pas la réponse,
            dis-le honnêtement plutôt que d'inventer.
            """;

    private static final String EMPTY_CONTEXT_MESSAGE = """
            Aucun contenu pertinent trouvé dans les cours de cet espace. Réponds honnêtement
            à l'étudiant que tu ne trouves pas cette information dans ses cours (ex. « Je ne
            trouve pas cette information dans vos documents ») — n'invente pas de réponse.
            """;

    private final VectorStore vectorStore;
    private final ChatProviderResolver chatProviderResolver;
    private final DocumentReranker documentReranker;
    private final IngestionClient ingestionClient;

    @Value("${chat.retrieval.top-k:40}")
    private int retrievalTopK;

    @Value("${chat.retrieval.similarity-threshold:0.6}")
    private double retrievalSimilarityThreshold;

    @Value("${chat.max-retrieved-chunks:5}")
    private int topN;

    public RagPipelineAdvisor(VectorStore vectorStore,
                              ChatProviderResolver chatProviderResolver,
                              DocumentReranker documentReranker,
                              IngestionClient ingestionClient) {
        this.vectorStore = vectorStore;
        this.chatProviderResolver = chatProviderResolver;
        this.documentReranker = documentReranker;
        this.ingestionClient = ingestionClient;
    }

    @Override
    public String getName() {
        return "rag-pipeline-advisor";
    }

    @Override
    public int getOrder() {
        // IMPORTANT : exécution AVANT le terminal ChatModelCallAdvisor (Ordered.LOWEST_PRECEDENCE).
        // Deux advisors à LOWEST_PRECEDENCE = exécution du terminal en premier, qui appelle le modèle
        // sans chaîner (adviseCall ne fait pas chain.nextCall) => RAG et mémoire silencieusement ignorés.
        return Ordered.LOWEST_PRECEDENCE - 1000;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String rawQuery = request.prompt().getUserMessage() != null
                ? request.prompt().getUserMessage().getText() : null;
        String spaceId = (String) request.context().get(SPACE_ID_CONTEXT);

        String rewrittenQuery = rewriteQuery(rawQuery);
        List<Document> candidates = retrieve(rewrittenQuery, spaceId);
        List<Document> topDocs = documentReranker.rerank(rawQuery, candidates, topN);

        // Résolution batch des image_ids → URLs (non bloquant)
        Set<String> allImageIds = topDocs.stream()
                .map(d -> (String) d.getMetadata().get("image_ids"))
                .filter(Objects::nonNull)
                .flatMap(ids -> Arrays.stream(ids.split(",")))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toSet());
        log.info("[RAG] allImageIds={} from {} topDocs", allImageIds.size(), topDocs.size());
        Map<String, IngestionClient.ResolvedImage> resolvedImages = Map.of();
        if (!allImageIds.isEmpty()) {
            String userId = UserContextHolder.get().userId();
            if (userId != null) {
                resolvedImages = ingestionClient.resolveImageIds(allImageIds, UUID.fromString(userId));
                log.info("[RAG] resolvedImages={} images", resolvedImages.size());
            } else {
                log.warn("[RAG] userId is null, cannot resolve image IDs");
            }
        }

        String contextBlock = buildContextBlock(topDocs, resolvedImages);
        ChatClientRequest augmented = request.mutate()
                .prompt(request.prompt().augmentSystemMessage(contextBlock))
                .context(RETRIEVED_DOCUMENTS, topDocs)
                .build();

        ChatClientResponse response = chain.nextCall(augmented);

        ChatResponse withMetadata = ChatResponse.builder()
                .from(response.chatResponse())
                .metadata(RETRIEVED_DOCUMENTS, topDocs)
                .build();
        return ChatClientResponse.builder()
                .chatResponse(withMetadata)
                .context(response.context())
                .build();
    }

    /** Réécriture de la question en requête de retrieval autonome (échec = question brute). */
    private String rewriteQuery(String rawQuery) {
        try {
            String rewritten = chatProviderResolver.current().prompt()
                    .system("Réécris la question ci-dessous en une requête de recherche autonome, "
                            + "sans référence à un tour de conversation précédent ni à des pronoms "
                            + "résolutifs (lui, elle, ce document...). Réponds uniquement avec la "
                            + "requête réécrite, en français.")
                    .user(rawQuery)
                    .call()
                    .content();
            return (rewritten == null || rewritten.isBlank()) ? rawQuery : rewritten.strip();
        } catch (Exception e) {
            log.warn("Réécriture de requête LLM échouée, question brute utilisée : {}", e.getMessage());
            return rawQuery;
        }
    }

    /** Retrieval large filtré par {@code space_id} (Option A multi-tenant). */
    private List<Document> retrieve(String query, String spaceId) {
        SearchRequest.Builder search = SearchRequest.builder()
                .query(query)
                .topK(retrievalTopK)
                .similarityThreshold(retrievalSimilarityThreshold);
        if (spaceId != null && !spaceId.isBlank()) {
            search.filterExpression("space_id == '" + spaceId + "'");
        }
        List<Document> results = vectorStore.similaritySearch(search.build());
        log.info("[RAG] similaritySearch: {} candidats (topK={}, seuil={})", results.size(), retrievalTopK, retrievalSimilarityThreshold);
        return results;
    }

    private String buildContextBlock(List<Document> docs, Map<String, IngestionClient.ResolvedImage> resolvedImages) {
        if (docs == null || docs.isEmpty()) {
            return EMPTY_CONTEXT_MESSAGE.strip();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            sb.append("[").append(i + 1).append("] ").append(doc.getText()).append("\n\n");

            // Injection des images associées au chunk
            String imageIdsRaw = (String) doc.getMetadata().get("image_ids");
            if (imageIdsRaw != null && !imageIdsRaw.isBlank() && !resolvedImages.isEmpty()) {
                List<String> imageRefs = Arrays.stream(imageIdsRaw.split(","))
                        .map(String::trim)
                        .filter(id -> !id.isEmpty())
                        .map(id -> {
                            IngestionClient.ResolvedImage img = resolvedImages.get(id);
                            if (img == null || img.url() == null) return null;
                            String desc = img.caption() != null ? img.caption() : "Image du document";
                            return "![%s](%s)".formatted(desc, img.url());
                        })
                        .filter(Objects::nonNull)
                        .toList();
                if (!imageRefs.isEmpty()) {
                    sb.append("Images associées :\n");
                    imageRefs.forEach(ref -> sb.append(ref).append("\n"));
                    sb.append("\n");
                }
            }
        }
        return SYSTEM_AUGMENT_TEMPLATE.formatted(sb.toString().strip()).strip();
    }
}