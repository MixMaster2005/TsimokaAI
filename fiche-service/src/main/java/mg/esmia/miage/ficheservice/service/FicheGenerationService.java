package mg.esmia.miage.ficheservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.aicommon.ChatProviderResolver;
import mg.esmia.miage.common.exception.ApiException;
import mg.esmia.miage.common.exception.ErrorCode;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Génération de fiches de révision par pattern MAP-REDUCE (cf. CDC §4.4 et ARCHITECTURE.md §6.3) :
 *
 * <ul>
 *   <li><b>MAP</b> : pour chaque document ciblé, les chunks sont lus depuis Qdrant (collection
 *       UNIQUE {@code chunks}, filtre {@code space_id} + {@code document_id} en payload) puis un
 *       résumé intermédiaire structuré est produit par un appel LLM one-shot
 *       ({@code prompts/fiche-map.st}). Si la liste est vide, un MAP unique sur tout le corpus
 *       de l'espace est fait. Résultats mis en cache ({@link FicheMapCacheService}).</li>
 *   <li><b>REDUCE</b> : les résumés intermédiaires sont fusionnés en une {@link FicheContent}
 *       unique et cohérente ({@code prompts/fiche-reduce.st}), garantie par
 *       {@link StructuredOutputValidationAdvisor} (3 tentatives max) + {@code entity(FicheContent.class)}.
 *       Le résultat est sérialisé en JSON dans {@code Fiche.contentJson}.</li>
 * </ul>
 *
 * <p>Résilience : le tout est sous {@link CircuitBreaker} ({@code llm-fiche}). En échec, la
 * génération renvoie une erreur métier ({@link ApiException} 503) plutôt qu'une fiche
 * « placeholder » trompeuse — l'utilisateur relance explicitement la génération.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FicheGenerationService {

    static final int MAX_CHUNK_CHARS = 1500;

    private final ChatProviderResolver chatProviderResolver;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final FicheMapCacheService mapCache;

    @Value("classpath:prompts/fiche-map.st")
    private Resource ficheMapPrompt;

    @Value("classpath:prompts/fiche-reduce.st")
    private Resource ficheReducePrompt;

    @Value("${fiche.max-chunks-per-document:50}")
    private int maxChunksPerDocument;

    @CircuitBreaker(name = "llm-fiche", fallbackMethod = "fallbackGenerateContentJson")
    public String generateContentJson(UUID spaceId, List<UUID> documentIds, String strategy) {
        List<UUID> targets = (documentIds == null || documentIds.isEmpty())
                ? List.of()
                : documentIds;

        if ("single-call".equals(strategy) && !targets.isEmpty()) {
            return generateSingleCall(spaceId, targets);
        }

        // Map-Reduce (stratégie par défaut)
        List<String> summaries = new ArrayList<>();
        if (targets.isEmpty()) {
            summaries.add(mapDocument(spaceId, null));
        } else {
            for (UUID documentId : targets) {
                summaries.add(mapDocument(spaceId, documentId));
            }
        }

        String reducePrompt = readPrompt(ficheReducePrompt)
                .replace("{{SUMMARIES}}", String.join("\n\n", summaries));
        FicheContent content = chatProviderResolver.current()
                .prompt()
                .system("Tu rédiges des fiches de révision. Réponds uniquement en JSON valide, en français.")
                .advisors(StructuredOutputValidationAdvisor.builder()
                        .outputType(FicheContent.class)
                        .maxRepeatAttempts(3)
                        .build())
                .user(reducePrompt)
                .call()
                .entity(FicheContent.class);
        return serialize(content);
    }

    /**
     * Stratégie single-call : tous les chunks de tous les documents sont concaténés
     * en un seul appel LLM. Adaptée quand le corpus tient dans la fenêtre de contexte.
     */
    private String generateSingleCall(UUID spaceId, List<UUID> documentIds) {
        List<Document> allChunks = new ArrayList<>();
        for (UUID docId : documentIds) {
            allChunks.addAll(retrieveChunks(spaceId, docId));
        }
        if (allChunks.isEmpty()) {
            return serialize(FicheContent.empty());
        }
        String content = allChunks.stream()
                .map(Document::getText)
                .map(t -> t == null ? "" : t)
                .map(t -> smartTruncate(t, MAX_CHUNK_CHARS))
                .collect(Collectors.joining("\n\n"));

        String prompt = readPrompt(ficheReducePrompt)
                .replace("{{SUMMARIES}}", "Contenu complet du corpus :\n" + content);

        FicheContent result = chatProviderResolver.current()
                .prompt()
                .system("Tu rédiges des fiches de révision. Réponds uniquement en JSON valide, en français.")
                .advisors(StructuredOutputValidationAdvisor.builder()
                        .outputType(FicheContent.class)
                        .maxRepeatAttempts(3)
                        .build())
                .user(prompt)
                .call()
                .entity(FicheContent.class);
        return serialize(result);
    }

    /**
     * Phase MAP : résumé intermédiaire structuré d'un document (documentId {@code null} = tout l'espace).
     * Résultat mis en cache pour éviter les appels LLM redondants.
     */
    String mapDocument(UUID spaceId, UUID documentId) {
        // 1. Vérifier le cache
        if (documentId != null) {
            Optional<String> cached = mapCache.get(spaceId, documentId);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        // 2. Calcul normal
        List<Document> chunks = retrieveChunks(spaceId, documentId);
        if (chunks.isEmpty()) {
            return "Aucun contenu indexé disponible pour ce document (id=" + documentId + ").";
        }
        String content = chunks.stream()
                .map(Document::getText)
                .map(t -> t == null ? "" : t)
                .map(t -> smartTruncate(t, MAX_CHUNK_CHARS))
                .collect(Collectors.joining("\n\n"));
        String mapPrompt = readPrompt(ficheMapPrompt)
                .replace("{{DOCUMENT_ID}}", documentId == null ? "corpus de l'espace" : documentId.toString())
                .replace("{{CHUNKS}}", content);
        String summary = chatProviderResolver.current()
                .prompt()
                .system("Tu résumes un document de cours. Réponds en français, de façon structurée.")
                .user(mapPrompt)
                .call()
                .content();

        // 3. Mettre en cache
        if (documentId != null && summary != null) {
            mapCache.put(spaceId, documentId, summary);
        }
        return summary;
    }

    /**
     * Lecture des chunks depuis Qdrant : filtre {@code space_id} (+ {@code document_id}) en
     * payload, requête neutre + seuil 0 pour récupérer le contenu sans dépendre de la
     * pertinence sémantique. topK plafonné ({@code fiche.max-chunks-per-document}).
     */
    List<Document> retrieveChunks(UUID spaceId, UUID documentId) {
        SearchRequest.Builder search = SearchRequest.builder()
                .query("résumé du contenu du document")
                .topK(maxChunksPerDocument)
                .similarityThreshold(0.0);
        if (documentId != null) {
            search.filterExpression("space_id == '" + spaceId + "' and document_id == '" + documentId + "'");
        } else {
            search.filterExpression("space_id == '" + spaceId + "'");
        }
        try {
            return vectorStore.similaritySearch(search.build());
        } catch (Exception e) {
            log.warn("Retrieval des chunks indisponible (space={}, doc={}) : {}", spaceId, documentId, e.getMessage());
            return List.of();
        }
    }

    /** Troncature intelligente : cherche la dernière frontière de phrase avant la limite
     *  pour ne pas couper au milieu d'une phrase (F2). */
    static String smartTruncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        int limit = maxLength;
        int lastSentence = Math.max(
                Math.max(text.lastIndexOf(". ", limit), text.lastIndexOf("? ", limit)),
                text.lastIndexOf("! ", limit));
        if (lastSentence > maxLength / 2) {
            limit = lastSentence + 1;
        } else {
            int lastParagraph = text.lastIndexOf("\n\n", limit);
            if (lastParagraph > maxLength / 2) {
                limit = lastParagraph + 2;
            }
        }
        return text.substring(0, limit);
    }

    String serialize(FicheContent content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Sérialisation JSON de la fiche impossible", e);
        }
    }

    /** Fallback du circuit breaker : erreur métier explicite (pas de fiche placeholder). */
    public String fallbackGenerateContentJson(UUID spaceId, List<UUID> documentIds, String strategy, Throwable t) {
        log.error("Fallback llm-fiche (space={}, documents={}, strategy={}) : {}", spaceId, documentIds, strategy, t.getMessage());
        throw new ApiException(ErrorCode.INTERNAL_ERROR,
                "La génération de la fiche a échoué (service LLM indisponible). Réessayez plus tard.", 503);
    }

    String readPrompt(Resource resource) {
        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Prompt illisible : " + resource.getFilename(), e);
        }
    }
}